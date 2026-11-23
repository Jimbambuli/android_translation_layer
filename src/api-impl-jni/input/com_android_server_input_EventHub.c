/* The kernel side of AOSP's EventHub (frameworks/native/services/inputflinger/reader/EventHub.cpp) for the
 * input devices that ATL reads itself, the game controllers: /dev/input is watched with inotify, a device is
 * opened and asked for its identifier, its event bits and its axes, its events are read on the main loop, and
 * FF_RUMBLE plays its vibrations. Which devices ATL keeps, and what their events mean, is decided in Java
 * (com.android.server.input.EventHub, InputReader), as InputReader does on Android. Keyboards, mice and touch
 * screens stay with GTK. */

#include <dirent.h>
#include <errno.h>
#include <fcntl.h>
#include <string.h>
#include <sys/inotify.h>
#include <sys/ioctl.h>
#include <time.h>
#include <unistd.h>

#include <linux/input.h>

#include <glib-unix.h>
#include <gtk/gtk.h>

#include "../defines.h"
#include "../util.h"
#include "../generated_headers/com_android_server_input_EventHub.h"

#define DEVICE_INPUT_PATH "/dev/input"

static jclass event_hub_class;
static jmethodID on_device_path_method;
static jmethodID on_events_method;
static jmethodID on_device_gone_method;

/* the devices ATL reads, by their Java id */
struct device {
	int id;
	int fd;
	guint source;
	int ff_effect_id;
	gboolean ff_effect_playing;
};
static GHashTable *devices; /* id -> struct device */

static void describe_exception(JNIEnv *env)
{
	if ((*env)->ExceptionCheck(env)) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}
}

/* EventHub::readNotifyLocked(): a node of /dev/input came or went. AOSP opens a node when it is created;
 * on a desktop udev gives the node its owner and ACL only after that (IN_ATTRIB), so a change of the
 * attributes counts as a new node too (Java ignores a path it has open already) */
static gboolean on_inotify(gint fd, GIOCondition condition, gpointer user_data)
{
	char buffer[4096] __attribute__((aligned(__alignof__(struct inotify_event))));
	ssize_t len = read(fd, buffer, sizeof(buffer));
	if (len <= 0)
		return G_SOURCE_CONTINUE;
	JNIEnv *env = get_jni_env();
	for (char *p = buffer; p < buffer + len;) {
		struct inotify_event *event = (struct inotify_event *)p;
		if (event->len && !strncmp(event->name, "event", 5)) {
			char *path = g_strdup_printf(DEVICE_INPUT_PATH "/%s", event->name);
			jstring path_jstr = (*env)->NewStringUTF(env, path);
			jboolean added = !(event->mask & IN_DELETE);
			(*env)->CallStaticVoidMethod(env, event_hub_class, on_device_path_method, path_jstr, added);
			describe_exception(env);
			(*env)->DeleteLocalRef(env, path_jstr);
			g_free(path);
		}
		p += sizeof(struct inotify_event) + event->len;
	}
	return G_SOURCE_CONTINUE;
}

/* EventHub(): the inotify watch on /dev/input; returns the paths of the event nodes there now
 * (scanDevicesLocked()) */
JNIEXPORT jobjectArray JNICALL Java_com_android_server_input_EventHub_nativeInit(JNIEnv *env, jclass class)
{
	event_hub_class = _REF(class);
	on_device_path_method = _STATIC_METHOD(class, "onDevicePath", "(Ljava/lang/String;Z)V");
	on_events_method = _STATIC_METHOD(class, "onEvents", "(I[J[I)V");
	on_device_gone_method = _STATIC_METHOD(class, "onDeviceGone", "(I)V");
	devices = g_hash_table_new(g_direct_hash, g_direct_equal);

	int inotify_fd = inotify_init1(IN_NONBLOCK | IN_CLOEXEC);
	if (inotify_fd < 0 || inotify_add_watch(inotify_fd, DEVICE_INPUT_PATH, IN_CREATE | IN_DELETE | IN_ATTRIB) < 0) {
		g_log(NULL, G_LOG_LEVEL_INFO, "EventHub: cannot watch " DEVICE_INPUT_PATH ": %m");
		if (inotify_fd >= 0)
			close(inotify_fd);
	} else {
		g_unix_fd_add(inotify_fd, G_IO_IN, on_inotify, NULL);
	}

	GPtrArray *paths = g_ptr_array_new_with_free_func(g_free);
	DIR *dir = opendir(DEVICE_INPUT_PATH);
	if (dir) {
		struct dirent *entry;
		while ((entry = readdir(dir)))
			if (!strncmp(entry->d_name, "event", 5))
				g_ptr_array_add(paths, g_strdup_printf(DEVICE_INPUT_PATH "/%s", entry->d_name));
		closedir(dir);
	}
	jobjectArray array = (*env)->NewObjectArray(env, paths->len, (*env)->FindClass(env, "java/lang/String"), NULL);
	for (guint i = 0; i < paths->len; i++) {
		jstring path = (*env)->NewStringUTF(env, paths->pdata[i]);
		(*env)->SetObjectArrayElement(env, array, i, path);
		(*env)->DeleteLocalRef(env, path);
	}
	g_ptr_array_free(paths, TRUE);
	return array;
}

/* EventHub::openDeviceLocked(): O_RDWR for the force feedback, read only if that is not allowed */
JNIEXPORT jint JNICALL Java_com_android_server_input_EventHub_nativeOpen(JNIEnv *env, jclass class, jstring path_jstr)
{
	const char *path = (*env)->GetStringUTFChars(env, path_jstr, NULL);
	int fd = open(path, O_RDWR | O_CLOEXEC | O_NONBLOCK);
	if (fd < 0)
		fd = open(path, O_RDONLY | O_CLOEXEC | O_NONBLOCK);
	(*env)->ReleaseStringUTFChars(env, path_jstr, path);
	return fd;
}

static void set_bits(JNIEnv *env, jobjectArray bits, int index, int fd, unsigned long request, size_t size)
{
	jbyte buffer[size];
	memset(buffer, 0, size);
	if (ioctl(fd, request, buffer) < 0)
		memset(buffer, 0, size);
	jbyteArray array = (*env)->NewByteArray(env, size);
	(*env)->SetByteArrayRegion(env, array, 0, size, buffer);
	(*env)->SetObjectArrayElement(env, bits, index, array);
	(*env)->DeleteLocalRef(env, array);
}

static jstring get_string(JNIEnv *env, int fd, unsigned long request)
{
	char buffer[256] = {0};
	if (ioctl(fd, request, buffer) < 1)
		return NULL;
	buffer[sizeof(buffer) - 1] = '\0';
	return (*env)->NewStringUTF(env, buffer);
}

/* The identifier, the event bits and the absolute axes of a device, as EventHub::openDeviceLocked() reads
 * them: strings {name, location, uniqueId}, ids {bus, vendor, product, version, driverVersion}, bits
 * {key, abs, rel, sw, led, ff, msc, prop}, abs_info [ABS_CNT][6] {present, min, max, flat, fuzz, resolution} */
JNIEXPORT jboolean JNICALL Java_com_android_server_input_EventHub_nativeGetDeviceInfo(JNIEnv *env, jclass class, jint fd, jobjectArray strings, jintArray ids, jobjectArray bits, jintArray abs_info)
{
	int driver_version;
	struct input_id input_id;
	if (ioctl(fd, EVIOCGVERSION, &driver_version) || ioctl(fd, EVIOCGID, &input_id))
		return JNI_FALSE;
	jstring name = get_string(env, fd, EVIOCGNAME(255));
	(*env)->SetObjectArrayElement(env, strings, 0, name);
	(*env)->SetObjectArrayElement(env, strings, 1, get_string(env, fd, EVIOCGPHYS(255)));
	(*env)->SetObjectArrayElement(env, strings, 2, get_string(env, fd, EVIOCGUNIQ(255)));
	jint id_values[] = {input_id.bustype, input_id.vendor, input_id.product, input_id.version, driver_version};
	(*env)->SetIntArrayRegion(env, ids, 0, 5, id_values);

	set_bits(env, bits, 0, fd, EVIOCGBIT(EV_KEY, (KEY_MAX + 8) / 8), (KEY_MAX + 8) / 8);
	set_bits(env, bits, 1, fd, EVIOCGBIT(EV_ABS, (ABS_MAX + 8) / 8), (ABS_MAX + 8) / 8);
	set_bits(env, bits, 2, fd, EVIOCGBIT(EV_REL, (REL_MAX + 8) / 8), (REL_MAX + 8) / 8);
	set_bits(env, bits, 3, fd, EVIOCGBIT(EV_SW, (SW_MAX + 8) / 8), (SW_MAX + 8) / 8);
	set_bits(env, bits, 4, fd, EVIOCGBIT(EV_LED, (LED_MAX + 8) / 8), (LED_MAX + 8) / 8);
	set_bits(env, bits, 5, fd, EVIOCGBIT(EV_FF, (FF_MAX + 8) / 8), (FF_MAX + 8) / 8);
	set_bits(env, bits, 6, fd, EVIOCGBIT(EV_MSC, (MSC_MAX + 8) / 8), (MSC_MAX + 8) / 8);
	set_bits(env, bits, 7, fd, EVIOCGPROP((INPUT_PROP_MAX + 8) / 8), (INPUT_PROP_MAX + 8) / 8);

	/* EventHub::getAbsoluteAxisInfo() */
	jbyteArray abs_bits = (*env)->GetObjectArrayElement(env, bits, 1);
	jbyte *abs = (*env)->GetByteArrayElements(env, abs_bits, NULL);
	jint info[ABS_CNT * 6];
	memset(info, 0, sizeof(info));
	for (int axis = 0; axis < ABS_CNT; axis++) {
		struct input_absinfo absinfo;
		if (!(abs[axis / 8] & (1 << (axis % 8))) || ioctl(fd, EVIOCGABS(axis), &absinfo))
			continue;
		if (absinfo.minimum == absinfo.maximum) /* AOSP: not a valid axis */
			continue;
		jint *row = info + axis * 6;
		row[0] = 1;
		row[1] = absinfo.minimum;
		row[2] = absinfo.maximum;
		row[3] = absinfo.flat;
		row[4] = absinfo.fuzz;
		row[5] = absinfo.resolution;
	}
	(*env)->ReleaseByteArrayElements(env, abs_bits, abs, JNI_ABORT);
	(*env)->SetIntArrayRegion(env, abs_info, 0, ABS_CNT * 6, info);
	return name ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL Java_com_android_server_input_EventHub_nativeClose(JNIEnv *env, jclass class, jint fd)
{
	close(fd);
}

static gboolean on_device_readable(gint fd, GIOCondition condition, gpointer user_data)
{
	struct device *device = user_data;
	JNIEnv *env = get_jni_env();
	struct input_event events[64];
	ssize_t len = read(fd, events, sizeof(events));
	if (len < 0 && (errno == EAGAIN || errno == EINTR))
		return G_SOURCE_CONTINUE;
	if (len <= 0 || (condition & (G_IO_ERR | G_IO_HUP))) {
		/* EventHub::readDeviceLocked(): ENODEV, the device went away */
		device->source = 0;
		(*env)->CallStaticVoidMethod(env, event_hub_class, on_device_gone_method, device->id);
		describe_exception(env);
		return G_SOURCE_REMOVE;
	}
	int count = len / sizeof(struct input_event);
	jlong when[count];
	jint values[count * 3];
	for (int i = 0; i < count; i++) {
		when[i] = (jlong)events[i].input_event_sec * 1000000000LL + (jlong)events[i].input_event_usec * 1000LL;
		values[i * 3] = events[i].type;
		values[i * 3 + 1] = events[i].code;
		values[i * 3 + 2] = events[i].value;
	}
	jlongArray when_array = (*env)->NewLongArray(env, count);
	(*env)->SetLongArrayRegion(env, when_array, 0, count, when);
	jintArray values_array = (*env)->NewIntArray(env, count * 3);
	(*env)->SetIntArrayRegion(env, values_array, 0, count * 3, values);
	(*env)->CallStaticVoidMethod(env, event_hub_class, on_events_method, device->id, when_array, values_array);
	describe_exception(env);
	(*env)->DeleteLocalRef(env, when_array);
	(*env)->DeleteLocalRef(env, values_array);
	return G_SOURCE_CONTINUE;
}

/* EventHub::Device::configureFd() and registerDeviceForEpollLocked(): the kernel's time stamps in
 * CLOCK_MONOTONIC (SystemClock.uptimeMillis() of ATL), the events read on the main loop. The kernel's key
 * repeat is not turned off (EVIOCSREP): the device is shared with the desktop, and the repeats (value 2)
 * are dropped like on AOSP (KeyboardInputMapper::process()) */
JNIEXPORT void JNICALL Java_com_android_server_input_EventHub_nativeStartReading(JNIEnv *env, jclass class, jint id, jint fd)
{
	int clock_id = CLOCK_MONOTONIC;
	ioctl(fd, EVIOCSCLOCKID, &clock_id);
	struct device *device = g_new0(struct device, 1);
	device->id = id;
	device->fd = fd;
	device->ff_effect_id = -1;
	device->source = g_unix_fd_add(fd, G_IO_IN | G_IO_ERR | G_IO_HUP, on_device_readable, device);
	g_hash_table_insert(devices, GINT_TO_POINTER(id), device);
}

/* EventHub::closeDeviceLocked() */
JNIEXPORT void JNICALL Java_com_android_server_input_EventHub_nativeStopReading(JNIEnv *env, jclass class, jint id)
{
	struct device *device = g_hash_table_lookup(devices, GINT_TO_POINTER(id));
	if (!device)
		return;
	g_hash_table_remove(devices, GINT_TO_POINTER(id));
	if (device->source)
		g_source_remove(device->source);
	if (device->ff_effect_id != -1)
		ioctl(device->fd, EVIOCRMFF, device->ff_effect_id);
	close(device->fd);
	g_free(device);
}

/* EventHub::vibrate(): one FF_RUMBLE effect per device, uploaded again for every element */
JNIEXPORT void JNICALL Java_com_android_server_input_EventHub_nativeVibrate(JNIEnv *env, jclass class, jint id, jint strong_magnitude, jint weak_magnitude, jint duration)
{
	struct device *device = g_hash_table_lookup(devices, GINT_TO_POINTER(id));
	if (!device)
		return;
	struct ff_effect effect = {
		.type = FF_RUMBLE,
		.id = device->ff_effect_id,
		.u.rumble.strong_magnitude = strong_magnitude,
		.u.rumble.weak_magnitude = weak_magnitude,
		.replay.length = duration,
		.replay.delay = 0,
	};
	if (ioctl(device->fd, EVIOCSFF, &effect)) {
		g_log(NULL, G_LOG_LEVEL_WARNING, "EventHub: could not upload force feedback effect to device %d: %m", id);
		return;
	}
	device->ff_effect_id = effect.id;
	struct input_event event = {.type = EV_FF, .code = device->ff_effect_id, .value = 1};
	if (write(device->fd, &event, sizeof(event)) != sizeof(event)) {
		g_log(NULL, G_LOG_LEVEL_WARNING, "EventHub: could not start force feedback effect on device %d: %m", id);
		return;
	}
	device->ff_effect_playing = TRUE;
}

/* EventHub::cancelVibrate() */
JNIEXPORT void JNICALL Java_com_android_server_input_EventHub_nativeCancelVibrate(JNIEnv *env, jclass class, jint id)
{
	struct device *device = g_hash_table_lookup(devices, GINT_TO_POINTER(id));
	if (!device || !device->ff_effect_playing)
		return;
	device->ff_effect_playing = FALSE;
	struct input_event event = {.type = EV_FF, .code = device->ff_effect_id, .value = 0};
	if (write(device->fd, &event, sizeof(event)) != sizeof(event))
		g_log(NULL, G_LOG_LEVEL_WARNING, "EventHub: could not stop force feedback effect on device %d: %m", id);
}

/* the directory of AOSP's /system/usr (keylayout, keychars, idc) in ATL's installation */
JNIEXPORT jstring JNICALL Java_com_android_server_input_EventHub_nativeGetSystemUsrDir(JNIEnv *env, jclass class)
{
	char *dir = atl_install_path(ATL_DATADIR "/atl/system/usr/");
	jstring ret = (*env)->NewStringUTF(env, dir);
	g_free(dir);
	return ret;
}
