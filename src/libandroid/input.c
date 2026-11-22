#include <assert.h>
#include <fcntl.h>
#include <poll.h>
#include <stdatomic.h>
#include <stdbool.h>
#include <stddef.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#include <gtk/gtk.h>

#include "looper.h"
#include "ndk_jni.h"

enum {
	AINPUT_EVENT_TYPE_KEY = 1,
	AINPUT_EVENT_TYPE_MOTION = 2,
	AINPUT_EVENT_TYPE_FOCUS = 3,
	AINPUT_EVENT_TYPE_CAPTURE = 4,
	AINPUT_EVENT_TYPE_DRAG = 5,
	AINPUT_EVENT_TYPE_TOUCH_MODE = 6
};

enum {
	AINPUT_SOURCE_CLASS_MASK = 0x000000ff,
	AINPUT_SOURCE_CLASS_NONE = 0x00000000,
	AINPUT_SOURCE_CLASS_BUTTON = 0x00000001,
	AINPUT_SOURCE_CLASS_POINTER = 0x00000002,
	AINPUT_SOURCE_CLASS_NAVIGATION = 0x00000004,
	AINPUT_SOURCE_CLASS_POSITION = 0x00000008,
	AINPUT_SOURCE_CLASS_JOYSTICK = 0x00000010
};

enum {
	AINPUT_SOURCE_UNKNOWN = 0x00000000,
	AINPUT_SOURCE_KEYBOARD = 0x00000100 | AINPUT_SOURCE_CLASS_BUTTON,
	AINPUT_SOURCE_DPAD = 0x00000200 | AINPUT_SOURCE_CLASS_BUTTON,
	AINPUT_SOURCE_GAMEPAD = 0x00000400 | AINPUT_SOURCE_CLASS_BUTTON,
	AINPUT_SOURCE_TOUCHSCREEN = 0x00001000 | AINPUT_SOURCE_CLASS_POINTER,
	AINPUT_SOURCE_MOUSE = 0x00002000 | AINPUT_SOURCE_CLASS_POINTER,
	AINPUT_SOURCE_STYLUS = 0x00004000 | AINPUT_SOURCE_CLASS_POINTER,
	AINPUT_SOURCE_BLUETOOTH_STYLUS = 0x00008000 | AINPUT_SOURCE_STYLUS,
	AINPUT_SOURCE_TRACKBALL = 0x00010000 | AINPUT_SOURCE_CLASS_NAVIGATION,
	AINPUT_SOURCE_MOUSE_RELATIVE = 0x00020000 | AINPUT_SOURCE_CLASS_NAVIGATION,
	AINPUT_SOURCE_TOUCHPAD = 0x00100000 | AINPUT_SOURCE_CLASS_POSITION,
	AINPUT_SOURCE_TOUCH_NAVIGATION = 0x00200000 | AINPUT_SOURCE_CLASS_NONE,
	AINPUT_SOURCE_JOYSTICK = 0x01000000 | AINPUT_SOURCE_CLASS_JOYSTICK,
	AINPUT_SOURCE_HDMI = 0x02000000 | AINPUT_SOURCE_CLASS_BUTTON,
	AINPUT_SOURCE_SENSOR = 0x04000000 | AINPUT_SOURCE_CLASS_NONE,
	AINPUT_SOURCE_ROTARY_ENCODER = 0x00400000 | AINPUT_SOURCE_CLASS_NONE,
	AINPUT_SOURCE_ANY = 0xffffff00
};

enum {
	AMOTION_EVENT_ACTION_MASK = 0xff,
	AMOTION_EVENT_ACTION_POINTER_INDEX_MASK = 0xff00,
	AMOTION_EVENT_ACTION_DOWN = 0,
	AMOTION_EVENT_ACTION_UP = 1,
	AMOTION_EVENT_ACTION_MOVE = 2,
	AMOTION_EVENT_ACTION_CANCEL = 3,
	AMOTION_EVENT_ACTION_OUTSIDE = 4,
	AMOTION_EVENT_ACTION_POINTER_DOWN = 5,
	AMOTION_EVENT_ACTION_POINTER_UP = 6,
	AMOTION_EVENT_ACTION_HOVER_MOVE = 7,
	AMOTION_EVENT_ACTION_SCROLL = 8,
	AMOTION_EVENT_ACTION_HOVER_ENTER = 9,
	AMOTION_EVENT_ACTION_HOVER_EXIT = 10,
	AMOTION_EVENT_ACTION_BUTTON_PRESS = 11,
	AMOTION_EVENT_ACTION_BUTTON_RELEASE = 12
};

enum {
	AMETA_SHIFT_ON = 0x01,
	AMETA_ALT_ON = 0x02,
	AMETA_CTRL_ON = 0x1000,
	AMETA_META_ON = 0x10000,
	AMETA_CAPS_LOCK_ON = 0x100000,
	AMOTION_EVENT_AXIS_X = 0,
	AMOTION_EVENT_AXIS_Y = 1,
	AMOTION_EVENT_AXIS_PRESSURE = 2,
	AMOTION_EVENT_AXIS_SIZE = 3,
	AMOTION_EVENT_AXIS_TOUCH_MAJOR = 4,
	AMOTION_EVENT_AXIS_TOUCH_MINOR = 5,
	AMOTION_EVENT_AXIS_TOOL_MAJOR = 6,
	AMOTION_EVENT_AXIS_TOOL_MINOR = 7,
	AMOTION_EVENT_AXIS_ORIENTATION = 8,
	AMOTION_EVENT_MAXIMUM_VALID_AXIS_VALUE = 53,
	AMOTION_EVENT_TOOL_TYPE_FINGER = 1,
};

#define MAX_POINTERS 16 /* AOSP include/input/Input.h */
#define AXIS_COUNT (AMOTION_EVENT_MAXIMUM_VALID_AXIS_VALUE + 1)

struct pointer_coords {
	float axis[AXIS_COUNT]; /* raw coordinates, as AOSP's PointerCoords */
};

/* AOSP libinput KeyEvent/MotionEvent in one struct (the key fields are 0 for a motion event and the other way
 * round). The events of the input queue go through a pipe and use the inline storage for their single sample. */
struct AInputEvent {
	int32_t type;
	int32_t device_id;
	int32_t source;
	int32_t action;
	int32_t flags;
	int32_t meta_state;
	int64_t down_time; /* ns */
	int32_t key_code;
	int32_t scan_code;
	int32_t repeat_count;
	int64_t key_event_time; /* ns */
	int32_t button_state;
	int32_t action_button;
	int32_t edge_flags;
	int32_t classification;
	float x_offset; /* window minus raw coordinates (AOSP MotionEvent::getRawXOffset()) */
	float y_offset;
	float x_precision;
	float y_precision;
	size_t pointer_count;
	size_t history_size;
	int32_t pointer_id[MAX_POINTERS];
	int32_t tool_type[MAX_POINTERS];
	int64_t *sample_time;		/* history_size + 1 entries, the current sample last (ns) */
	struct pointer_coords *coords;	/* (history_size + 1) * pointer_count entries */
	int64_t inline_time;
	struct pointer_coords inline_coords;
};

typedef void AInputQueue;

struct ALooper;
typedef int (*Looper_callbackFunc)(int fd, int events, void *data);

/* AOSP MotionEvent::getHistoricalAxisValue(): X and Y get the offset of the window, the other axes are as stored */
static float axis_value(const struct AInputEvent *e, int32_t axis, size_t pointer_index, size_t history_index, bool raw)
{
	if (axis < 0 || axis >= AXIS_COUNT)
		return 0;
	float value = e->coords[history_index * e->pointer_count + pointer_index].axis[axis];
	if (!raw && axis == AMOTION_EVENT_AXIS_X)
		value += e->x_offset;
	if (!raw && axis == AMOTION_EVENT_AXIS_Y)
		value += e->y_offset;
	return value;
}

float AMotionEvent_getAxisValue(const struct AInputEvent *motion_event, int32_t axis, size_t pointer_index)
{
	return axis_value(motion_event, axis, pointer_index, motion_event->history_size, false);
}

size_t AMotionEvent_getPointerCount(const struct AInputEvent *motion_event)
{
	return motion_event->pointer_count;
}

int32_t AInputEvent_getType(const struct AInputEvent *event)
{
	if (event) {
		return event->type;
	} else {
		return -1;
	}
}

int32_t AInputEvent_getSource(const struct AInputEvent *event)
{
	if (event) {
		return event->source;
	} else {
		return -1;
	}
}

int32_t AInputEvent_getDeviceId(const struct AInputEvent *event)
{
	return event->device_id;
}

int32_t AMotionEvent_getAction(const struct AInputEvent *motion_event)
{
	if (motion_event) {
		return motion_event->action;
	} else {
		return -1;
	}
}

int32_t AMotionEvent_getPointerId(const struct AInputEvent *motion_event, size_t pointer_index)
{
	if (motion_event) {
		return motion_event->pointer_id[pointer_index];
	} else {
		return -1; // 0?
	}
}

float AMotionEvent_getX(const struct AInputEvent *motion_event, size_t pointer_index)
{
	if (motion_event) {
		return AMotionEvent_getAxisValue(motion_event, AMOTION_EVENT_AXIS_X, pointer_index);
	} else {
		return -1;
	}
}

float AMotionEvent_getY(const struct AInputEvent *motion_event, size_t pointer_index)
{
	if (motion_event) {
		return AMotionEvent_getAxisValue(motion_event, AMOTION_EVENT_AXIS_Y, pointer_index);
	} else {
		return -1;
	}
}

/* AOSP frameworks/base/native/android/input.cpp: plain accessors of the event */
int32_t AMotionEvent_getFlags(const struct AInputEvent *e) { return e->flags; }
int32_t AMotionEvent_getMetaState(const struct AInputEvent *e) { return e->meta_state; }
int32_t AMotionEvent_getButtonState(const struct AInputEvent *e) { return e->button_state; }
int32_t AMotionEvent_getActionButton(const struct AInputEvent *e) { return e->action_button; }
int32_t AMotionEvent_getEdgeFlags(const struct AInputEvent *e) { return e->edge_flags; }
int32_t AMotionEvent_getClassification(const struct AInputEvent *e) { return e->classification; }
int64_t AMotionEvent_getDownTime(const struct AInputEvent *e) { return e->down_time; }
int64_t AMotionEvent_getEventTime(const struct AInputEvent *e) { return e->sample_time[e->history_size]; }
float AMotionEvent_getXOffset(const struct AInputEvent *e) { return e->x_offset; }
float AMotionEvent_getYOffset(const struct AInputEvent *e) { return e->y_offset; }
float AMotionEvent_getXPrecision(const struct AInputEvent *e) { return e->x_precision; }
float AMotionEvent_getYPrecision(const struct AInputEvent *e) { return e->y_precision; }
int32_t AMotionEvent_getToolType(const struct AInputEvent *e, size_t i) { return e->tool_type[i]; }
size_t AMotionEvent_getHistorySize(const struct AInputEvent *e) { return e->history_size; }
int64_t AMotionEvent_getHistoricalEventTime(const struct AInputEvent *e, size_t h) { return e->sample_time[h]; }

#define CURRENT_AXIS(name, axis) \
	float AMotionEvent_get##name(const struct AInputEvent *e, size_t i) { return axis_value(e, axis, i, e->history_size, false); } \
	float AMotionEvent_getHistorical##name(const struct AInputEvent *e, size_t i, size_t h) { return axis_value(e, axis, i, h, false); }
CURRENT_AXIS(Pressure, AMOTION_EVENT_AXIS_PRESSURE)
CURRENT_AXIS(Size, AMOTION_EVENT_AXIS_SIZE)
CURRENT_AXIS(TouchMajor, AMOTION_EVENT_AXIS_TOUCH_MAJOR)
CURRENT_AXIS(TouchMinor, AMOTION_EVENT_AXIS_TOUCH_MINOR)
CURRENT_AXIS(ToolMajor, AMOTION_EVENT_AXIS_TOOL_MAJOR)
CURRENT_AXIS(ToolMinor, AMOTION_EVENT_AXIS_TOOL_MINOR)
CURRENT_AXIS(Orientation, AMOTION_EVENT_AXIS_ORIENTATION)

float AMotionEvent_getRawX(const struct AInputEvent *e, size_t i) { return axis_value(e, AMOTION_EVENT_AXIS_X, i, e->history_size, true); }
float AMotionEvent_getRawY(const struct AInputEvent *e, size_t i) { return axis_value(e, AMOTION_EVENT_AXIS_Y, i, e->history_size, true); }
float AMotionEvent_getHistoricalRawX(const struct AInputEvent *e, size_t i, size_t h) { return axis_value(e, AMOTION_EVENT_AXIS_X, i, h, true); }
float AMotionEvent_getHistoricalRawY(const struct AInputEvent *e, size_t i, size_t h) { return axis_value(e, AMOTION_EVENT_AXIS_Y, i, h, true); }
float AMotionEvent_getHistoricalX(const struct AInputEvent *e, size_t i, size_t h) { return axis_value(e, AMOTION_EVENT_AXIS_X, i, h, false); }
float AMotionEvent_getHistoricalY(const struct AInputEvent *e, size_t i, size_t h) { return axis_value(e, AMOTION_EVENT_AXIS_Y, i, h, false); }
float AMotionEvent_getHistoricalAxisValue(const struct AInputEvent *e, int32_t axis, size_t i, size_t h) { return axis_value(e, axis, i, h, false); }

int32_t AKeyEvent_getAction(const struct AInputEvent *e) { return e->action; }
int32_t AKeyEvent_getFlags(const struct AInputEvent *e) { return e->flags; }
int32_t AKeyEvent_getScanCode(const struct AInputEvent *e) { return e->scan_code; }
int32_t AKeyEvent_getMetaState(const struct AInputEvent *e) { return e->meta_state; }
int32_t AKeyEvent_getRepeatCount(const struct AInputEvent *e) { return e->repeat_count; }
int64_t AKeyEvent_getDownTime(const struct AInputEvent *e) { return e->down_time; }
int64_t AKeyEvent_getEventTime(const struct AInputEvent *e) { return e->key_event_time; }

/* AOSP input.cpp AInputEvent_release(): only for the events of AKeyEvent_fromJava() and AMotionEvent_fromJava() */
void AInputEvent_release(const struct AInputEvent *event)
{
	struct AInputEvent *e = (struct AInputEvent *)event;
	if (e->coords != &e->inline_coords) {
		free(e->coords);
		free(e->sample_time);
	}
	free(e);
}

static jint call_int(JNIEnv *env, jobject obj, jclass class, const char *name)
{
	return (*env)->CallIntMethod(env, obj, (*env)->GetMethodID(env, class, name, "()I"));
}

static jlong call_long(JNIEnv *env, jobject obj, jclass class, const char *name)
{
	return (*env)->CallLongMethod(env, obj, (*env)->GetMethodID(env, class, name, "()J"));
}

/* AOSP android_view_KeyEvent_obtainAsCopy(JNIEnv*, jobject); ATL's KeyEvent keeps its times in ms, AOSP's native
 * KeyEvent in ns */
const struct AInputEvent *AKeyEvent_fromJava(JNIEnv *env, jobject keyEvent)
{
	jclass class = (*env)->GetObjectClass(env, keyEvent);
	struct AInputEvent *e = calloc(1, sizeof(*e));
	e->type = AINPUT_EVENT_TYPE_KEY;
	e->device_id = call_int(env, keyEvent, class, "getDeviceId");
	e->source = call_int(env, keyEvent, class, "getSource");
	e->meta_state = call_int(env, keyEvent, class, "getMetaState");
	e->action = call_int(env, keyEvent, class, "getAction");
	e->key_code = call_int(env, keyEvent, class, "getKeyCode");
	e->scan_code = call_int(env, keyEvent, class, "getScanCode");
	e->repeat_count = call_int(env, keyEvent, class, "getRepeatCount");
	e->flags = call_int(env, keyEvent, class, "getFlags");
	e->down_time = call_long(env, keyEvent, class, "getDownTime") * 1000000;
	e->key_event_time = call_long(env, keyEvent, class, "getEventTime") * 1000000;
	e->coords = &e->inline_coords;
	e->sample_time = &e->inline_time;
	(*env)->DeleteLocalRef(env, class);
	return e;
}

/* AOSP input.cpp AMotionEvent_fromJava(): a copy with the history, read through the public getters of ATL's
 * MotionEvent (which has no native event); the offset is that of the first pointer, as ATL only translates */
const struct AInputEvent *AMotionEvent_fromJava(JNIEnv *env, jobject motionEvent)
{
	if (!motionEvent)
		return NULL;
	jclass class = (*env)->GetObjectClass(env, motionEvent);
	jmethodID get_axis = (*env)->GetMethodID(env, class, "getAxisValue", "(II)F");
	jmethodID get_hist_axis = (*env)->GetMethodID(env, class, "getHistoricalAxisValue", "(III)F");
	jmethodID get_hist_time = (*env)->GetMethodID(env, class, "getHistoricalEventTimeNanos", "(I)J");
	jmethodID get_id = (*env)->GetMethodID(env, class, "getPointerId", "(I)I");
	jmethodID get_tool = (*env)->GetMethodID(env, class, "getToolType", "(I)I");
	jmethodID get_raw[2] = {(*env)->GetMethodID(env, class, "getRawX", "(I)F"), (*env)->GetMethodID(env, class, "getRawY", "(I)F")};
	/* ATL's getAxisValue() does not cover these, the getters of the axes do */
	static const struct { int32_t axis; const char *name; } named[] = {
		{AMOTION_EVENT_AXIS_PRESSURE, "getPressure"}, {AMOTION_EVENT_AXIS_SIZE, "getSize"},
		{AMOTION_EVENT_AXIS_TOUCH_MAJOR, "getTouchMajor"}, {AMOTION_EVENT_AXIS_TOUCH_MINOR, "getTouchMinor"},
		{AMOTION_EVENT_AXIS_TOOL_MAJOR, "getToolMajor"}, {AMOTION_EVENT_AXIS_TOOL_MINOR, "getToolMinor"},
		{AMOTION_EVENT_AXIS_ORIENTATION, "getOrientation"},
	};
	struct AInputEvent *e = calloc(1, sizeof(*e));
	e->type = AINPUT_EVENT_TYPE_MOTION;
	e->device_id = call_int(env, motionEvent, class, "getDeviceId");
	e->source = call_int(env, motionEvent, class, "getSource");
	e->action = call_int(env, motionEvent, class, "getAction");
	e->flags = call_int(env, motionEvent, class, "getFlags");
	e->meta_state = call_int(env, motionEvent, class, "getMetaState");
	e->button_state = call_int(env, motionEvent, class, "getButtonState");
	e->action_button = call_int(env, motionEvent, class, "getActionButton");
	e->edge_flags = call_int(env, motionEvent, class, "getEdgeFlags");
	e->classification = call_int(env, motionEvent, class, "getClassification");
	e->down_time = call_long(env, motionEvent, class, "getDownTime") * 1000000;
	e->x_precision = (*env)->CallFloatMethod(env, motionEvent, (*env)->GetMethodID(env, class, "getXPrecision", "()F"));
	e->y_precision = (*env)->CallFloatMethod(env, motionEvent, (*env)->GetMethodID(env, class, "getYPrecision", "()F"));
	e->pointer_count = call_int(env, motionEvent, class, "getPointerCount");
	e->history_size = call_int(env, motionEvent, class, "getHistorySize");
	e->sample_time = calloc(e->history_size + 1, sizeof(*e->sample_time));
	e->coords = calloc((e->history_size + 1) * e->pointer_count, sizeof(*e->coords));
	if (e->pointer_count) {
		e->x_offset = (*env)->CallFloatMethod(env, motionEvent, get_axis, AMOTION_EVENT_AXIS_X, 0) - (*env)->CallFloatMethod(env, motionEvent, get_raw[0], 0);
		e->y_offset = (*env)->CallFloatMethod(env, motionEvent, get_axis, AMOTION_EVENT_AXIS_Y, 0) - (*env)->CallFloatMethod(env, motionEvent, get_raw[1], 0);
	}
	for (size_t i = 0; i < e->pointer_count; i++) {
		e->pointer_id[i] = (*env)->CallIntMethod(env, motionEvent, get_id, (jint)i);
		e->tool_type[i] = (*env)->CallIntMethod(env, motionEvent, get_tool, (jint)i);
	}
	for (size_t h = 0; h < e->history_size; h++) {
		e->sample_time[h] = (*env)->CallLongMethod(env, motionEvent, get_hist_time, (jint)h);
		for (size_t i = 0; i < e->pointer_count; i++) {
			struct pointer_coords *c = &e->coords[h * e->pointer_count + i];
			for (int32_t axis = 0; axis < AXIS_COUNT; axis++)
				c->axis[axis] = (*env)->CallFloatMethod(env, motionEvent, get_hist_axis, axis, (jint)i, (jint)h);
			c->axis[AMOTION_EVENT_AXIS_X] -= e->x_offset;
			c->axis[AMOTION_EVENT_AXIS_Y] -= e->y_offset;
		}
	}
	e->sample_time[e->history_size] = call_long(env, motionEvent, class, "getEventTime") * 1000000;
	for (size_t i = 0; i < e->pointer_count; i++) {
		struct pointer_coords *c = &e->coords[e->history_size * e->pointer_count + i];
		for (int32_t axis = 0; axis < AXIS_COUNT; axis++)
			c->axis[axis] = (*env)->CallFloatMethod(env, motionEvent, get_axis, axis, (jint)i);
		for (size_t n = 0; n < sizeof(named) / sizeof(named[0]); n++)
			c->axis[named[n].axis] = (*env)->CallFloatMethod(env, motionEvent, (*env)->GetMethodID(env, class, named[n].name, "(I)F"), (jint)i);
		c->axis[AMOTION_EVENT_AXIS_X] = (*env)->CallFloatMethod(env, motionEvent, get_raw[0], (jint)i);
		c->axis[AMOTION_EVENT_AXIS_Y] = (*env)->CallFloatMethod(env, motionEvent, get_raw[1], (jint)i);
	}
	(*env)->DeleteLocalRef(env, class);
	return e;
}

/* the PointerCoords[] of one sample, with the raw coordinates */
static jobjectArray java_coords(JNIEnv *env, const struct AInputEvent *e, size_t h)
{
	jclass class = ndk_jni_class(env, "android/view/MotionEvent$PointerCoords");
	jmethodID ctor = (*env)->GetMethodID(env, class, "<init>", "()V");
	jmethodID set_axis = (*env)->GetMethodID(env, class, "setAxisValue", "(IF)V");
	jobjectArray array = (*env)->NewObjectArray(env, e->pointer_count, class, NULL);
	for (size_t i = 0; i < e->pointer_count; i++) {
		jobject coords = (*env)->NewObject(env, class, ctor);
		for (int32_t axis = 0; axis < AXIS_COUNT; axis++)
			(*env)->CallVoidMethod(env, coords, set_axis, axis, axis_value(e, axis, i, h, true));
		(*env)->SetObjectArrayElement(env, array, i, coords);
		(*env)->DeleteLocalRef(env, coords);
	}
	return array;
}

/* AOSP input.cpp AInputEvent_toJava() with android_view_KeyEvent_obtainAsCopy() and
 * android_view_MotionEvent_obtainAsCopy(): ATL's Java events get the values through obtain(), the history through
 * addBatch() and the offset of the window through offsetLocation() */
jobject AInputEvent_toJava(JNIEnv *env, const struct AInputEvent *e)
{
	if (e->type == AINPUT_EVENT_TYPE_KEY) {
		jclass class = ndk_jni_class(env, "android/view/KeyEvent");
		jobject obj = (*env)->CallStaticObjectMethod(env, class, (*env)->GetStaticMethodID(env, class, "obtain", "(JJIIIIIIIILjava/lang/String;)Landroid/view/KeyEvent;"),
		                                             (jlong)(e->down_time / 1000000), (jlong)(e->key_event_time / 1000000), e->action, e->key_code, e->repeat_count,
		                                             e->meta_state, e->device_id, e->scan_code, e->flags, e->source, NULL);
		return ndk_jni_threw(env, "KeyEvent.obtain") ? NULL : obj;
	}
	if (e->type != AINPUT_EVENT_TYPE_MOTION) {
		fprintf(stderr, "Unexpected event type %d in AInputEvent_toJava.\n", e->type);
		abort();
	}
	jclass class = ndk_jni_class(env, "android/view/MotionEvent");
	jclass props_class = ndk_jni_class(env, "android/view/MotionEvent$PointerProperties");
	jmethodID props_ctor = (*env)->GetMethodID(env, props_class, "<init>", "()V");
	jfieldID props_id = (*env)->GetFieldID(env, props_class, "id", "I");
	jfieldID props_tool = (*env)->GetFieldID(env, props_class, "toolType", "I");
	jobjectArray props = (*env)->NewObjectArray(env, e->pointer_count, props_class, NULL);
	for (size_t i = 0; i < e->pointer_count; i++) {
		jobject p = (*env)->NewObject(env, props_class, props_ctor);
		(*env)->SetIntField(env, p, props_id, e->pointer_id[i]);
		(*env)->SetIntField(env, p, props_tool, e->tool_type[i]);
		(*env)->SetObjectArrayElement(env, props, i, p);
		(*env)->DeleteLocalRef(env, p);
	}
	jobjectArray coords = java_coords(env, e, 0);
	jobject obj = (*env)->CallStaticObjectMethod(env, class, (*env)->GetStaticMethodID(env, class, "obtain", "(JJII[Landroid/view/MotionEvent$PointerProperties;[Landroid/view/MotionEvent$PointerCoords;IIFFIIII)Landroid/view/MotionEvent;"),
	                                             (jlong)(e->down_time / 1000000), (jlong)(e->sample_time[0] / 1000000), e->action, (jint)e->pointer_count, props, coords,
	                                             e->meta_state, e->button_state, e->x_precision, e->y_precision, e->device_id, e->edge_flags, e->source, e->flags);
	(*env)->DeleteLocalRef(env, props);
	(*env)->DeleteLocalRef(env, coords);
	if (ndk_jni_threw(env, "MotionEvent.obtain"))
		return NULL;
	(*env)->CallVoidMethod(env, obj, (*env)->GetMethodID(env, class, "setEdgeFlags", "(I)V"), e->edge_flags);
	for (size_t h = 1; h <= e->history_size; h++) {
		coords = java_coords(env, e, h);
		(*env)->CallVoidMethod(env, obj, (*env)->GetMethodID(env, class, "addBatch", "(J[Landroid/view/MotionEvent$PointerCoords;I)V"), (jlong)(e->sample_time[h] / 1000000), coords, e->meta_state);
		(*env)->DeleteLocalRef(env, coords);
		if (ndk_jni_threw(env, "MotionEvent.addBatch"))
			break;
	}
	(*env)->CallVoidMethod(env, obj, (*env)->GetMethodID(env, class, "offsetLocation", "(FF)V"), e->x_offset, e->y_offset);
	return obj;
}

void AInputQueue_detachLooper(AInputQueue *queue)
{
	return;
}

struct android_poll_source {
	// The identifier of this source.  May be LOOPER_ID_MAIN or
	// LOOPER_ID_INPUT.
	int32_t id;

	// The android_app this ident is associated with.
	struct android_app *app;

	// Function to call to perform the standard processing of data from
	// this source.
	void (*process)(struct android_app *app, struct android_poll_source *source);
};

// TODO: malloc on getEvent and free on finishEvent? malloc isn't very fast though, and events can in principle be pretty frequent
struct AInputEvent fixme_ugly_current_event;

static int64_t touch_down_time; /* of the last ACTION_DOWN, for AMotionEvent_getDownTime() */

/* the modifier keys of a GDK event as AMETA_* (as getMetaState() of ATL's MotionEvent in views/android_view_View.c) */
static int32_t meta_state(GdkModifierType state)
{
	return (state & GDK_SHIFT_MASK ? AMETA_SHIFT_ON : 0) | (state & GDK_ALT_MASK ? AMETA_ALT_ON : 0) | (state & GDK_CONTROL_MASK ? AMETA_CTRL_ON : 0) |
	       (state & GDK_SUPER_MASK ? AMETA_META_ON : 0) | (state & GDK_LOCK_MASK ? AMETA_CAPS_LOCK_ON : 0);
}

static inline void make_touch_event(GdkEvent *event, GtkEventControllerLegacy *event_controller, struct AInputEvent *ainput_event)
{
	GtkWidget *window = gtk_event_controller_get_widget(GTK_EVENT_CONTROLLER(event_controller));
	GtkWidget *child;
	double x, y, pressure;

	memset(ainput_event, 0, sizeof(*ainput_event));
	ainput_event->type = AINPUT_EVENT_TYPE_MOTION;
	ainput_event->device_id = 1; /* as the MotionEvents of ATL for the pointer events of GTK */
	ainput_event->source = AINPUT_SOURCE_TOUCHSCREEN;
	ainput_event->meta_state = meta_state(gdk_event_get_modifier_state(event));
	ainput_event->x_precision = 1;
	ainput_event->y_precision = 1;
	ainput_event->pointer_count = 1;
	ainput_event->tool_type[0] = AMOTION_EVENT_TOOL_TYPE_FINGER;
	ainput_event->inline_time = (int64_t)gdk_event_get_time(event) * 1000000; /* ms, as ATL's MotionEvent */
	/* AOSP's InputReader for devices without a pressure axis: a pointer that touches has pressure 1 */
	ainput_event->inline_coords.axis[AMOTION_EVENT_AXIS_PRESSURE] = gdk_event_get_axis(event, GDK_AXIS_PRESSURE, &pressure) ? pressure : 1;

	gdk_event_get_position(event, &x, &y);
	ainput_event->inline_coords.axis[AMOTION_EVENT_AXIS_X] = x;
	ainput_event->inline_coords.axis[AMOTION_EVENT_AXIS_Y] = y;

	// the window's coordinate system starts at the top left of the header bar, which is not ideal
	// apps expect it to start at the top left of the area where child widgets get placed, so that
	// the top left of the window is the same as the top left of a single widget filling the entire window
	// while it's quite hacky, the following should realistically work for most if not all cases
	// (the raw coordinates stay those of the window, the difference is the offset of the event)
	if ((child = gtk_window_get_child(GTK_WINDOW(window)))) {
		int ret;
		graphene_point_t p;
		ret = gtk_widget_compute_point(window, child, &GRAPHENE_POINT_INIT(x, y), &p);
		assert(ret);

		ainput_event->x_offset = p.x - x;
		ainput_event->y_offset = p.y - y;
	}

	switch (gdk_event_get_event_type(event)) {
		case GDK_BUTTON_PRESS:
		case GDK_TOUCH_BEGIN:
			ainput_event->action = AMOTION_EVENT_ACTION_DOWN;
			touch_down_time = ainput_event->inline_time;
			break;
		case GDK_BUTTON_RELEASE:
		case GDK_TOUCH_END:
			ainput_event->action = AMOTION_EVENT_ACTION_UP;
			break;
		case GDK_MOTION_NOTIFY:
		case GDK_TOUCH_UPDATE:
			ainput_event->action = AMOTION_EVENT_ACTION_MOVE;
			break;
		default:
			fprintf(stderr, "%s: %s: passed in GdkEvent is not a touch event or equivalent\n", __FILE__, __func__);
			break;
	}
	ainput_event->down_time = touch_down_time;
}

static gboolean on_event(GtkEventControllerLegacy *self, GdkEvent *event, int input_queue_pipe_fd)
{
	struct AInputEvent ainput_event;

	// TODO: this doesn't work for multitouch
	switch (gdk_event_get_event_type(event)) {
		// mouse click/move (currently we convert these to touch events)
		case GDK_BUTTON_PRESS:
		case GDK_BUTTON_RELEASE:
		case GDK_MOTION_NOTIFY:
			/* ATL_MOUSE_AS_TOUCH (as android_view_View.c): only the primary button is the finger, and a finger
			 * that does not touch sends nothing (no ACTION_MOVE without ACTION_DOWN) */
			if (getenv("ATL_MOUSE_AS_TOUCH") &&
			    !(gdk_event_get_event_type(event) == GDK_MOTION_NOTIFY ? gdk_event_get_modifier_state(event) & GDK_BUTTON1_MASK : gdk_button_event_get_button(event) == GDK_BUTTON_PRIMARY))
				return false;
		// touchscreen
		case GDK_TOUCH_BEGIN:
		case GDK_TOUCH_END:
		case GDK_TOUCH_UPDATE:
			make_touch_event(event, self, &ainput_event);
			write(input_queue_pipe_fd, &ainput_event, sizeof(struct AInputEvent));
			break;
		default:
			return false;
			break;
	}

	return true;
}

// FIXME put this in a header file
struct input_queue {
	int fd;
	GtkEventController *controller;
};

void AInputQueue_attachLooper(struct input_queue *queue, struct ALooper *looper, int ident, Looper_callbackFunc callback, void *data)
{
	struct android_poll_source *poll_source = (struct android_poll_source *)data;
	//printf("AInputQueue_attachLooper called: queue: %p, looper: %p, ident: %d, callback %p, data: %p, process_func: %p\n", queue, looper, ident, callback, poll_source, poll_source ? poll_source->process : 0);
	if (poll_source == NULL)
		return;
	int input_queue_pipe[2];
	if (pipe(input_queue_pipe)) {
		fprintf(stderr, "could not create pipe: %s", strerror(errno));
		return;
	}
	fcntl(input_queue_pipe[0], F_SETFL, O_NONBLOCK);
	ALooper_addFd(looper, input_queue_pipe[0], ident, (1 << 0) /*? ALOOPER_EVENT_INPUT*/, callback, data);
	g_signal_connect(queue->controller, "event", G_CALLBACK(on_event), GINT_TO_POINTER(input_queue_pipe[1]));
	queue->fd = input_queue_pipe[0];
}

int32_t AInputQueue_getEvent(struct input_queue *queue, struct AInputEvent **outEvent)
{
	if (read(queue->fd, &fixme_ugly_current_event, sizeof(struct AInputEvent)) == sizeof(struct AInputEvent)) {
		fixme_ugly_current_event.sample_time = &fixme_ugly_current_event.inline_time;
		fixme_ugly_current_event.coords = &fixme_ugly_current_event.inline_coords;
		*outEvent = &fixme_ugly_current_event;
		return 0;
	} else {
		return -1; // no events or error
	}
}

int32_t AInputQueue_preDispatchEvent(AInputQueue *queue, struct AInputEvent *event)
{
	return 0; // we don't want to claim the event for ourselves, let the app process it
}

void AInputQueue_finishEvent(AInputQueue *queue, struct AInputEvent *event, int handled)
{
	// should we do something here?
}

int32_t AKeyEvent_getKeyCode(struct AInputEvent *event)
{
	/* 0 for a motion event, which Minecraft PE passes here */
	return event->key_code;
}

/* AOSP input.h: 1 with events, 0 without, negative on an error (poll() of the single pipe gives just that) */
int32_t AInputQueue_hasEvents(struct input_queue *queue)
{
	return poll(&(struct pollfd){.fd = queue->fd, .events = POLLIN}, 1, 0);
}

/* AOSP android_view_InputQueue_getNativePtr() */
AInputQueue *AInputQueue_fromJava(JNIEnv *env, jobject inputQueue)
{
	jlong ptr = (*env)->CallLongMethod(env, inputQueue, (*env)->GetMethodID(env, (*env)->GetObjectClass(env, inputQueue), "getNativePtr", "()J"));
	return (AInputQueue *)(intptr_t)ptr;
}

/* AOSP frameworks/base/native/android/input_transfer_token.cpp: ATL's android.window.InputTransferToken is a plain
 * Java object, the native token holds it with a reference count */
struct AInputTransferToken {
	jobject token;
	atomic_int refs;
};

static struct AInputTransferToken *transfer_token_new(JNIEnv *env, jobject token)
{
	struct AInputTransferToken *t = malloc(sizeof(*t));
	t->token = (*env)->NewGlobalRef(env, token);
	atomic_init(&t->refs, 1);
	return t;
}

struct AInputTransferToken *AInputTransferToken_fromJava(JNIEnv *env, jobject inputTransferTokenObj)
{
	return transfer_token_new(env, inputTransferTokenObj);
}

jobject AInputTransferToken_toJava(JNIEnv *env, const struct AInputTransferToken *aInputTransferToken)
{
	return (*env)->NewLocalRef(env, aInputTransferToken->token);
}

void AInputTransferToken_release(struct AInputTransferToken *aInputTransferToken)
{
	if (!aInputTransferToken || atomic_fetch_sub(&aInputTransferToken->refs, 1) != 1)
		return;
	JNIEnv *env = ndk_jni_env();
	(*env)->DeleteGlobalRef(env, aInputTransferToken->token);
	free(aInputTransferToken);
}

/* AOSP frameworks/base/native/android/surface_control_input_receiver.cpp */
typedef bool (*AInputReceiver_onMotionEvent)(void *context, struct AInputEvent *motionEvent);
typedef bool (*AInputReceiver_onKeyEvent)(void *context, struct AInputEvent *keyEvent);

typedef struct AInputReceiverCallbacks {
	void *context;
	AInputReceiver_onMotionEvent onMotionEvent;
	AInputReceiver_onKeyEvent onKeyEvent;
} AInputReceiverCallbacks;

AInputReceiverCallbacks *AInputReceiverCallbacks_create(void *context)
{
	AInputReceiverCallbacks *callbacks = calloc(1, sizeof(*callbacks));
	callbacks->context = context;
	return callbacks;
}

void AInputReceiverCallbacks_release(AInputReceiverCallbacks *callbacks)
{
	free(callbacks);
}

void AInputReceiverCallbacks_setMotionEventCallback(AInputReceiverCallbacks *callbacks, AInputReceiver_onMotionEvent onMotionEvent)
{
	callbacks->onMotionEvent = onMotionEvent;
}

void AInputReceiverCallbacks_setKeyEventCallback(AInputReceiverCallbacks *callbacks, AInputReceiver_onKeyEvent onKeyEvent)
{
	callbacks->onKeyEvent = onKeyEvent;
}

/* AOSP creates the client's InputTransferToken and asks the window manager for an input channel of the
 * SurfaceControl. ATL has no SurfaceControl layers and so no input channel for them: the receiver gets its token
 * but no event is ever delivered to the callbacks. */
typedef struct AInputReceiver {
	AInputReceiverCallbacks *callbacks;
	struct AInputTransferToken *token;
} AInputReceiver;

static AInputReceiver *input_receiver_new(AInputReceiverCallbacks *callbacks)
{
	JNIEnv *env = ndk_jni_env();
	jclass class = ndk_jni_class(env, "android/window/InputTransferToken");
	jobject token = (*env)->NewObject(env, class, (*env)->GetMethodID(env, class, "<init>", "()V"));
	AInputReceiver *receiver = malloc(sizeof(*receiver));
	receiver->callbacks = callbacks;
	receiver->token = transfer_token_new(env, token);
	(*env)->DeleteLocalRef(env, token);
	return receiver;
}

AInputReceiver *AInputReceiver_createBatchedInputReceiver(void *aChoreographer, const struct AInputTransferToken *hostToken, const void *aSurfaceControl, AInputReceiverCallbacks *callbacks)
{
	return input_receiver_new(callbacks);
}

AInputReceiver *AInputReceiver_createUnbatchedInputReceiver(struct ALooper *aLooper, const struct AInputTransferToken *hostToken, const void *aSurfaceControl, AInputReceiverCallbacks *callbacks)
{
	return input_receiver_new(callbacks);
}

/* AOSP InputReceiver::getInputTransferToken(): the caller gets a reference of its own */
const struct AInputTransferToken *AInputReceiver_getInputTransferToken(AInputReceiver *aInputReceiver)
{
	atomic_fetch_add(&aInputReceiver->token->refs, 1);
	return aInputReceiver->token;
}

void AInputReceiver_release(AInputReceiver *aInputReceiver)
{
	if (!aInputReceiver)
		return;
	AInputTransferToken_release(aInputReceiver->token);
	free(aInputReceiver);
}
