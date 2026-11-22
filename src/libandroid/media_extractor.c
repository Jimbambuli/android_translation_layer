/*
 * NdkMediaExtractor and NdkMediaDataSource (libmediandk) on ATL's android.media.MediaExtractor (FFmpeg's libavformat).
 * AOSP's extractor is NuMediaExtractor (frameworks/av/media/ndk/NdkMediaExtractor.cpp); a source no demuxer opens
 * fails like NuMediaExtractor does when no plugin sniffs the content (initMediaExtractor(): ERROR_UNSUPPORTED).
 */

#define _GNU_SOURCE
#include <stdlib.h>
#include <string.h>

#include "media_ndk.h"
#include "ndk_jni.h"

typedef struct AMediaCodecCryptoInfo AMediaCodecCryptoInfo;
typedef struct AMediaDataSource AMediaDataSource;
typedef ssize_t (*AMediaDataSourceReadAt)(void *userdata, off64_t offset, void *buffer, size_t size);
typedef ssize_t (*AMediaDataSourceGetSize)(void *userdata);
typedef void (*AMediaDataSourceClose)(void *userdata);
typedef ssize_t (*AMediaDataSourceGetAvailableSize)(void *userdata, off64_t offset);
typedef uint8_t AMediaUUID[16];

/* NdkMediaDataSource.cpp */
struct AMediaDataSource {
	void *userdata;
	AMediaDataSourceReadAt readAt;
	AMediaDataSourceGetSize getSize;
	AMediaDataSourceClose close;
	AMediaDataSourceGetAvailableSize getAvailableSize;
};

typedef enum {
	AMEDIAEXTRACTOR_SEEK_PREVIOUS_SYNC,
	AMEDIAEXTRACTOR_SEEK_NEXT_SYNC,
	AMEDIAEXTRACTOR_SEEK_CLOSEST_SYNC
} SeekMode;

typedef struct PsshEntry {
	AMediaUUID uuid;
	size_t datalen;
	void *data;
} PsshEntry;

typedef struct PsshInfo {
	size_t numentries;
	PsshEntry entries[0];
} PsshInfo;

typedef struct AMediaExtractor {
	jobject extractor; /* android.media.MediaExtractor */
	AMediaDataSource *source; /* NuMediaExtractor's mDataSource of setDataSourceCustom(), closed on delete */
} AMediaExtractor;

AMediaFormat *AMediaFormat_new(void);
media_status_t AMediaFormat_delete(AMediaFormat *format);
void AMediaFormat_clear(AMediaFormat *format);
void AMediaFormat_setInt32(AMediaFormat *format, const char *name, int32_t value);
void AMediaFormat_setString(AMediaFormat *format, const char *name, const char *value);
bool AMediaFormat_getString(AMediaFormat *format, const char *name, const char **out);

static struct {
	jclass class;
	jmethodID ctor, release, set_path, set_fd, get_track_count, get_track_format, select_track, unselect_track;
	jmethodID seek_to, advance, read_sample, get_track_index, get_time, get_size, get_flags, get_cached, get_file_format;
	jmethodID set_custom;
	jclass fd_class, source_class;
	jmethodID fd_ctor, fd_set, source_ctor;
} m;

static void register_data_source(JNIEnv *env);

static JNIEnv *env_init(void)
{
	JNIEnv *env = ndk_jni_env();
	if (!env || m.class)
		return env;
	jclass class = ndk_jni_class(env, "android/media/MediaExtractor");
	m.ctor = (*env)->GetMethodID(env, class, "<init>", "()V");
	m.release = (*env)->GetMethodID(env, class, "release", "()V");
	m.set_path = (*env)->GetMethodID(env, class, "setDataSource", "(Ljava/lang/String;)V");
	m.set_fd = (*env)->GetMethodID(env, class, "setDataSource", "(Ljava/io/FileDescriptor;JJ)V");
	m.set_custom = (*env)->GetMethodID(env, class, "setDataSource", "(Landroid/media/MediaDataSource;)V");
	m.get_track_count = (*env)->GetMethodID(env, class, "getTrackCount", "()I");
	m.get_track_format = (*env)->GetMethodID(env, class, "getTrackFormat", "(I)Landroid/media/MediaFormat;");
	m.select_track = (*env)->GetMethodID(env, class, "selectTrack", "(I)V");
	m.unselect_track = (*env)->GetMethodID(env, class, "unselectTrack", "(I)V");
	m.seek_to = (*env)->GetMethodID(env, class, "seekTo", "(JI)V");
	m.advance = (*env)->GetMethodID(env, class, "advance", "()Z");
	m.read_sample = (*env)->GetMethodID(env, class, "readSampleData", "(Ljava/nio/ByteBuffer;I)I");
	m.get_track_index = (*env)->GetMethodID(env, class, "getSampleTrackIndex", "()I");
	m.get_time = (*env)->GetMethodID(env, class, "getSampleTime", "()J");
	m.get_size = (*env)->GetMethodID(env, class, "getSampleSize", "()J");
	m.get_flags = (*env)->GetMethodID(env, class, "getSampleFlags", "()I");
	m.get_cached = (*env)->GetMethodID(env, class, "getCachedDuration", "()J");
	m.get_file_format = (*env)->GetMethodID(env, class, "getFileFormat", "()Landroid/media/MediaFormat;");
	m.fd_class = ndk_jni_class(env, "java/io/FileDescriptor");
	m.fd_ctor = (*env)->GetMethodID(env, m.fd_class, "<init>", "()V");
	/* libnativehelper's AFileDescriptor_setFd(): FileDescriptor.setInt$() (libcore's field is "descriptor") */
	m.fd_set = (*env)->GetMethodID(env, m.fd_class, "setInt$", "(I)V");
	register_data_source(env);
	m.class = class;
	return env;
}

AMediaExtractor *AMediaExtractor_new(void)
{
	JNIEnv *env = env_init();
	if (!env)
		return NULL;
	jobject extractor = (*env)->NewObject(env, m.class, m.ctor);
	if (ndk_jni_threw(env, "AMediaExtractor_new") || !extractor)
		return NULL;
	AMediaExtractor *ex = calloc(1, sizeof(AMediaExtractor));
	ex->extractor = (*env)->NewGlobalRef(env, extractor);
	(*env)->DeleteLocalRef(env, extractor);
	return ex;
}

/* AOSP: `delete mData`, which accepts NULL */
media_status_t AMediaExtractor_delete(AMediaExtractor *ex)
{
	if (!ex)
		return AMEDIA_OK;
	JNIEnv *env = env_init();
	(*env)->CallVoidMethod(env, ex->extractor, m.release);
	ndk_jni_threw(env, "AMediaExtractor_delete");
	(*env)->DeleteGlobalRef(env, ex->extractor);
	if (ex->source) { /* ~NuMediaExtractor(): mDataSource->close(), then ~NdkDataSource() deletes the copy */
		if (ex->source->close && ex->source->userdata)
			ex->source->close(ex->source->userdata);
		free(ex->source);
	}
	free(ex);
	return AMEDIA_OK;
}

/* an IOException is NuMediaExtractor's ERROR_UNSUPPORTED (no demuxer accepts the source) */
static media_status_t set_failed(JNIEnv *env, const char *what)
{
	return ndk_jni_threw_class(env, "java/io/IOException", what) ? AMEDIA_ERROR_UNSUPPORTED : AMEDIA_OK;
}

media_status_t AMediaExtractor_setDataSourceFd(AMediaExtractor *ex, int fd, off64_t offset, off64_t length)
{
	JNIEnv *env = env_init();
	/* the descriptor stays the caller's: a plain java.io.FileDescriptor does not close it */
	jobject jfd = (*env)->NewObject(env, m.fd_class, m.fd_ctor);
	(*env)->CallVoidMethod(env, jfd, m.fd_set, fd);
	(*env)->CallVoidMethod(env, ex->extractor, m.set_fd, jfd, (jlong)offset, (jlong)length);
	(*env)->DeleteLocalRef(env, jfd);
	return set_failed(env, "AMediaExtractor_setDataSourceFd");
}

media_status_t AMediaExtractor_setDataSource(AMediaExtractor *ex, const char *location)
{
	JNIEnv *env = env_init();
	jstring path = (*env)->NewStringUTF(env, location);
	(*env)->CallVoidMethod(env, ex->extractor, m.set_path, path);
	(*env)->DeleteLocalRef(env, path);
	return set_failed(env, "AMediaExtractor_setDataSource");
}

/* NuMediaExtractor::setDataSource(new NdkDataSource(src)): NdkDataSource reads a copy of src's callbacks, here through
 * android.media.NdkMediaDataSource, whose methods are the natives below */
media_status_t AMediaExtractor_setDataSourceCustom(AMediaExtractor *ex, AMediaDataSource *src)
{
	JNIEnv *env = env_init();
	AMediaDataSource *copy = malloc(sizeof(AMediaDataSource));
	*copy = *src;
	jobject source = (*env)->NewObject(env, m.source_class, m.source_ctor, (jlong)(intptr_t)copy);
	(*env)->CallVoidMethod(env, ex->extractor, m.set_custom, source);
	(*env)->DeleteLocalRef(env, source);
	media_status_t status = set_failed(env, "AMediaExtractor_setDataSourceCustom");
	if (status == AMEDIA_OK && !ex->source)
		ex->source = copy;
	else
		free(copy);
	return status;
}

size_t AMediaExtractor_getTrackCount(AMediaExtractor *ex)
{
	JNIEnv *env = env_init();
	jint count = (*env)->CallIntMethod(env, ex->extractor, m.get_track_count);
	return ndk_jni_threw(env, "AMediaExtractor_getTrackCount") ? 0 : count;
}

/* AOSP returns the format of a failed getTrackFormat() as it is: empty */
AMediaFormat *AMediaExtractor_getTrackFormat(AMediaExtractor *ex, size_t idx)
{
	JNIEnv *env = env_init();
	jobject jformat = (*env)->CallObjectMethod(env, ex->extractor, m.get_track_format, (jint)idx);
	if (ndk_jni_threw(env, "AMediaExtractor_getTrackFormat") || !jformat)
		return AMediaFormat_new();
	AMediaFormat *format = AMediaFormat_fromJava(env, jformat);
	(*env)->DeleteLocalRef(env, jformat);
	return format;
}

/* NuMediaExtractor returns -EINVAL/-ERANGE, which AOSP's translate_error() has no NDK code for: AMEDIA_ERROR_UNKNOWN */
media_status_t AMediaExtractor_selectTrack(AMediaExtractor *ex, size_t idx)
{
	JNIEnv *env = env_init();
	(*env)->CallVoidMethod(env, ex->extractor, m.select_track, (jint)idx);
	return ndk_jni_threw_class(env, "java/lang/IllegalArgumentException", "AMediaExtractor_selectTrack") ? AMEDIA_ERROR_UNKNOWN : AMEDIA_OK;
}

media_status_t AMediaExtractor_unselectTrack(AMediaExtractor *ex, size_t idx)
{
	JNIEnv *env = env_init();
	(*env)->CallVoidMethod(env, ex->extractor, m.unselect_track, (jint)idx);
	return ndk_jni_threw_class(env, "java/lang/IllegalArgumentException", "AMediaExtractor_unselectTrack") ? AMEDIA_ERROR_UNKNOWN : AMEDIA_OK;
}

/* the buffer is the caller's: AOSP wraps it in an ABuffer, here in a direct ByteBuffer; -1 at the end of the stream */
ssize_t AMediaExtractor_readSampleData(AMediaExtractor *ex, uint8_t *buffer, size_t capacity)
{
	JNIEnv *env = env_init();
	jobject bb = (*env)->NewDirectByteBuffer(env, buffer, capacity);
	jint size = (*env)->CallIntMethod(env, ex->extractor, m.read_sample, bb, 0);
	(*env)->DeleteLocalRef(env, bb);
	return ndk_jni_threw(env, "AMediaExtractor_readSampleData") ? -1 : size;
}

/* -1 (the whole mask) when there is no sample, as AOSP when getSampleMeta() fails */
uint32_t AMediaExtractor_getSampleFlags(AMediaExtractor *ex)
{
	JNIEnv *env = env_init();
	jint flags = (*env)->CallIntMethod(env, ex->extractor, m.get_flags);
	return ndk_jni_threw(env, "AMediaExtractor_getSampleFlags") ? (uint32_t)-1 : (uint32_t)flags;
}

int AMediaExtractor_getSampleTrackIndex(AMediaExtractor *ex)
{
	JNIEnv *env = env_init();
	jint idx = (*env)->CallIntMethod(env, ex->extractor, m.get_track_index);
	return ndk_jni_threw(env, "AMediaExtractor_getSampleTrackIndex") ? -1 : idx;
}

int64_t AMediaExtractor_getSampleTime(AMediaExtractor *ex)
{
	JNIEnv *env = env_init();
	jlong time = (*env)->CallLongMethod(env, ex->extractor, m.get_time);
	return ndk_jni_threw(env, "AMediaExtractor_getSampleTime") ? -1 : time;
}

/* ERROR_END_OF_STREAM and every other error are false */
bool AMediaExtractor_advance(AMediaExtractor *ex)
{
	JNIEnv *env = env_init();
	jboolean ok = (*env)->CallBooleanMethod(env, ex->extractor, m.advance);
	return !ndk_jni_threw(env, "AMediaExtractor_advance") && ok;
}

/* NuMediaExtractor::seekTo(): ERROR_END_OF_STREAM when no selected track has a sample, which the Java class ignores */
media_status_t AMediaExtractor_seekTo(AMediaExtractor *ex, int64_t seekPosUs, SeekMode mode)
{
	JNIEnv *env = env_init();
	/* AOSP maps anything but PREVIOUS_SYNC and CLOSEST_SYNC to SEEK_NEXT_SYNC, as the Java constants do */
	jint java_mode = mode == AMEDIAEXTRACTOR_SEEK_PREVIOUS_SYNC ? 0 : mode == AMEDIAEXTRACTOR_SEEK_CLOSEST_SYNC ? 2 : 1;
	(*env)->CallVoidMethod(env, ex->extractor, m.seek_to, (jlong)seekPosUs, java_mode);
	if (ndk_jni_threw(env, "AMediaExtractor_seekTo"))
		return AMEDIA_ERROR_UNKNOWN;
	jint idx = (*env)->CallIntMethod(env, ex->extractor, m.get_track_index);
	return ndk_jni_threw(env, "AMediaExtractor_seekTo") || idx < 0 ? AMEDIA_ERROR_END_OF_STREAM : AMEDIA_OK;
}

/* the container's "mime"; empty when NuMediaExtractor::getFileFormat() fails (-EINVAL, which AOSP ignores) */
AMediaFormat *AMediaExtractor_getFileFormat(AMediaExtractor *ex)
{
	JNIEnv *env = env_init();
	jobject jformat = (*env)->CallObjectMethod(env, ex->extractor, m.get_file_format);
	if (ndk_jni_threw(env, "AMediaExtractor_getFileFormat") || !jformat)
		return AMediaFormat_new();
	AMediaFormat *format = AMediaFormat_fromJava(env, jformat);
	(*env)->DeleteLocalRef(env, jformat);
	return format;
}

ssize_t AMediaExtractor_getSampleSize(AMediaExtractor *ex)
{
	JNIEnv *env = env_init();
	jlong size = (*env)->CallLongMethod(env, ex->extractor, m.get_size);
	return ndk_jni_threw(env, "AMediaExtractor_getSampleSize") ? -1 : size;
}

/* no demuxed file carries a "pssh" entry (no DRM): NULL, as AOSP */
PsshInfo *AMediaExtractor_getPsshInfo(AMediaExtractor *ex)
{
	return NULL;
}

/* no sample carries encryption data (kKeyEncryptedSizes): NULL, as AOSP */
AMediaCodecCryptoInfo *AMediaExtractor_getSampleCryptoInfo(AMediaExtractor *ex)
{
	return NULL;
}

int64_t AMediaExtractor_getCachedDuration(AMediaExtractor *ex)
{
	JNIEnv *env = env_init();
	jlong duration = (*env)->CallLongMethod(env, ex->extractor, m.get_cached);
	return ndk_jni_threw(env, "AMediaExtractor_getCachedDuration") ? -1 : duration;
}

/* AOSP: the sample's track index and the mime of that track (the sample's own metadata, SEI etc., does not exist) */
media_status_t AMediaExtractor_getSampleFormat(AMediaExtractor *ex, AMediaFormat *fmt)
{
	if (!fmt)
		return AMEDIA_ERROR_INVALID_PARAMETER;
	JNIEnv *env = env_init();
	jint idx = (*env)->CallIntMethod(env, ex->extractor, m.get_track_index);
	if (ndk_jni_threw(env, "AMediaExtractor_getSampleFormat") || idx < 0)
		return AMEDIA_ERROR_END_OF_STREAM; /* getSampleMeta() = ERROR_END_OF_STREAM */
	AMediaFormat_clear(fmt);
	AMediaFormat_setInt32(fmt, "track-index", idx);
	AMediaFormat *track = AMediaExtractor_getTrackFormat(ex, idx);
	const char *mime;
	if (AMediaFormat_getString(track, "mime", &mime))
		AMediaFormat_setString(fmt, "mime", mime);
	AMediaFormat_delete(track);
	return AMEDIA_OK;
}

/* NdkDataSource::readAt(): -1 without a callback or userdata, else the callback's result (-1 at the end) */
static jint data_source_read_at(JNIEnv *env, jclass class, jlong ptr, jlong position, jbyteArray buffer, jint offset, jint size)
{
	AMediaDataSource *src = (AMediaDataSource *)(intptr_t)ptr;
	if (!src->readAt || !src->userdata)
		return -1;
	jbyte *bytes = (*env)->GetByteArrayElements(env, buffer, NULL);
	ssize_t ret = src->readAt(src->userdata, position, bytes + offset, size);
	(*env)->ReleaseByteArrayElements(env, buffer, bytes, 0);
	return ret;
}

/* NdkDataSource::getSize(): -1 (unknown) without a callback or userdata */
static jlong data_source_get_size(JNIEnv *env, jclass class, jlong ptr)
{
	AMediaDataSource *src = (AMediaDataSource *)(intptr_t)ptr;
	return src->getSize && src->userdata ? src->getSize(src->userdata) : -1;
}

static void register_data_source(JNIEnv *env)
{
	static const JNINativeMethod methods[] = {
		{"native_readAt", "(JJ[BII)I", data_source_read_at},
		{"native_getSize", "(J)J", data_source_get_size},
	};
	m.source_class = ndk_jni_class(env, "android/media/NdkMediaDataSource");
	m.source_ctor = (*env)->GetMethodID(env, m.source_class, "<init>", "(J)V");
	(*env)->RegisterNatives(env, m.source_class, methods, 2);
	ndk_jni_threw(env, "NdkMediaDataSource");
}

AMediaDataSource *AMediaDataSource_new(void)
{
	return calloc(1, sizeof(AMediaDataSource));
}

/* AOSP: DataSourceFactory::CreateFromURI() on the HTTP service of the Java VM; ATL has neither, which is AOSP's
 * "source is null" */
AMediaDataSource *AMediaDataSource_newUri(const char *uri, int numheaders, const char *const *key_values)
{
	return NULL;
}

void AMediaDataSource_delete(AMediaDataSource *src)
{
	free(src);
}

void AMediaDataSource_setUserdata(AMediaDataSource *src, void *userdata)
{
	src->userdata = userdata;
}

void AMediaDataSource_setReadAt(AMediaDataSource *src, AMediaDataSourceReadAt readAt)
{
	src->readAt = readAt;
}

void AMediaDataSource_setGetSize(AMediaDataSource *src, AMediaDataSourceGetSize getSize)
{
	src->getSize = getSize;
}

void AMediaDataSource_setClose(AMediaDataSource *src, AMediaDataSourceClose close)
{
	src->close = close;
}

/* AOSP calls close without a check */
void AMediaDataSource_close(AMediaDataSource *src)
{
	src->close(src->userdata);
}

void AMediaDataSource_setGetAvailableSize(AMediaDataSource *src, AMediaDataSourceGetAvailableSize getAvailableSize)
{
	src->getAvailableSize = getAvailableSize;
}
