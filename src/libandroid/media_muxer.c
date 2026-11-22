/*
 * NdkMediaMuxer (libmediandk) on ATL's android.media.MediaMuxer. AOSP's AMediaMuxer wraps libstagefright's MediaMuxer
 * (frameworks/av/media/ndk/NdkMediaMuxer.cpp); ATL has no container writer, so the Java constructor throws
 * IOException and AMediaMuxer_new() returns NULL, as AOSP when MediaMuxer::create() fails. The other functions are
 * the Java calls with AOSP's NDK results: an error of the Java muxer (wrong state, bad argument) is a status_t that
 * AOSP's translate_error() has no NDK code for, AMEDIA_ERROR_UNKNOWN.
 */

#include <stdlib.h>

#include "media_ndk.h"
#include "ndk_jni.h"

typedef enum {
	AMEDIAMUXER_OUTPUT_FORMAT_MPEG_4 = 0,
	AMEDIAMUXER_OUTPUT_FORMAT_WEBM = 1,
	AMEDIAMUXER_OUTPUT_FORMAT_THREE_GPP = 2,
	AMEDIAMUXER_OUTPUT_FORMAT_HEIF = 3,
	AMEDIAMUXER_OUTPUT_FORMAT_OGG = 4,
} OutputFormat;

typedef enum {
	AMEDIAMUXER_APPEND_IGNORE_LAST_VIDEO_GOP = 0,
	AMEDIAMUXER_APPEND_TO_EXISTING_DATA = 1,
} AppendMode;

typedef struct AMediaCodecBufferInfo {
	int32_t offset;
	int32_t size;
	int64_t presentationTimeUs;
	uint32_t flags;
} AMediaCodecBufferInfo;

typedef struct AMediaMuxer {
	jobject muxer; /* android.media.MediaMuxer */
	size_t track_count;
	jobject *track_formats; /* global references of the MediaFormats given to addTrack() */
} AMediaMuxer;

static struct {
	jclass class;
	jmethodID ctor, set_location, set_orientation, add_track, start, stop, release, write;
	jclass fd_class, info_class;
	jmethodID fd_ctor, info_ctor, info_set;
	jfieldID fd_fd;
} m;

static JNIEnv *env_init(void)
{
	JNIEnv *env = ndk_jni_env();
	if (!env || m.class)
		return env;
	jclass class = ndk_jni_class(env, "android/media/MediaMuxer");
	m.ctor = (*env)->GetMethodID(env, class, "<init>", "(Ljava/io/FileDescriptor;I)V");
	m.set_location = (*env)->GetMethodID(env, class, "setLocation", "(FF)V");
	m.set_orientation = (*env)->GetMethodID(env, class, "setOrientationHint", "(I)V");
	m.add_track = (*env)->GetMethodID(env, class, "addTrack", "(Landroid/media/MediaFormat;)I");
	m.start = (*env)->GetMethodID(env, class, "start", "()V");
	m.stop = (*env)->GetMethodID(env, class, "stop", "()V");
	m.release = (*env)->GetMethodID(env, class, "release", "()V");
	m.write = (*env)->GetMethodID(env, class, "writeSampleData", "(ILjava/nio/ByteBuffer;Landroid/media/MediaCodec$BufferInfo;)V");
	m.fd_class = ndk_jni_class(env, "java/io/FileDescriptor");
	m.fd_ctor = (*env)->GetMethodID(env, m.fd_class, "<init>", "()V");
	m.fd_fd = (*env)->GetFieldID(env, m.fd_class, "fd", "I");
	m.info_class = ndk_jni_class(env, "android/media/MediaCodec$BufferInfo");
	m.info_ctor = (*env)->GetMethodID(env, m.info_class, "<init>", "()V");
	m.info_set = (*env)->GetMethodID(env, m.info_class, "set", "(IIJI)V");
	m.class = class;
	return env;
}

static media_status_t status(JNIEnv *env, const char *what)
{
	return ndk_jni_threw(env, what) ? AMEDIA_ERROR_UNKNOWN : AMEDIA_OK;
}

/* the descriptor stays the caller's: a plain java.io.FileDescriptor does not close it */
AMediaMuxer *AMediaMuxer_new(int fd, OutputFormat format)
{
	JNIEnv *env = env_init();
	if (!env)
		return NULL;
	jobject jfd = (*env)->NewObject(env, m.fd_class, m.fd_ctor);
	(*env)->SetIntField(env, jfd, m.fd_fd, fd);
	jobject muxer = (*env)->NewObject(env, m.class, m.ctor, jfd, (jint)format);
	(*env)->DeleteLocalRef(env, jfd);
	if (ndk_jni_threw_class(env, "java/io/IOException", "AMediaMuxer_new") || !muxer)
		return NULL;
	AMediaMuxer *mux = calloc(1, sizeof(AMediaMuxer));
	mux->muxer = (*env)->NewGlobalRef(env, muxer);
	(*env)->DeleteLocalRef(env, muxer);
	return mux;
}

/* AOSP: `delete muxer`, which accepts NULL; the Java muxer is stopped and released like MediaMuxer's destructor does */
media_status_t AMediaMuxer_delete(AMediaMuxer *mux)
{
	if (!mux)
		return AMEDIA_OK;
	JNIEnv *env = env_init();
	(*env)->CallVoidMethod(env, mux->muxer, m.release);
	ndk_jni_threw(env, "AMediaMuxer_delete");
	(*env)->DeleteGlobalRef(env, mux->muxer);
	for (size_t i = 0; i < mux->track_count; i++)
		(*env)->DeleteGlobalRef(env, mux->track_formats[i]);
	free(mux->track_formats);
	free(mux);
	return AMEDIA_OK;
}

media_status_t AMediaMuxer_setLocation(AMediaMuxer *mux, float latitude, float longitude)
{
	JNIEnv *env = env_init();
	(*env)->CallVoidMethod(env, mux->muxer, m.set_location, latitude, longitude);
	return status(env, "AMediaMuxer_setLocation");
}

media_status_t AMediaMuxer_setOrientationHint(AMediaMuxer *mux, int degrees)
{
	JNIEnv *env = env_init();
	(*env)->CallVoidMethod(env, mux->muxer, m.set_orientation, (jint)degrees);
	return status(env, "AMediaMuxer_setOrientationHint");
}

ssize_t AMediaMuxer_addTrack(AMediaMuxer *mux, const AMediaFormat *format)
{
	JNIEnv *env = env_init();
	jobject jformat = AMediaFormat_toJava(env, format);
	jint index = (*env)->CallIntMethod(env, mux->muxer, m.add_track, jformat);
	if (ndk_jni_threw(env, "AMediaMuxer_addTrack")) {
		(*env)->DeleteLocalRef(env, jformat);
		return AMEDIA_ERROR_UNKNOWN;
	}
	mux->track_formats = realloc(mux->track_formats, (mux->track_count + 1) * sizeof(jobject));
	mux->track_formats[mux->track_count++] = (*env)->NewGlobalRef(env, jformat);
	(*env)->DeleteLocalRef(env, jformat);
	return index;
}

media_status_t AMediaMuxer_start(AMediaMuxer *mux)
{
	JNIEnv *env = env_init();
	(*env)->CallVoidMethod(env, mux->muxer, m.start);
	return status(env, "AMediaMuxer_start");
}

media_status_t AMediaMuxer_stop(AMediaMuxer *mux)
{
	JNIEnv *env = env_init();
	(*env)->CallVoidMethod(env, mux->muxer, m.stop);
	return status(env, "AMediaMuxer_stop");
}

/* AOSP wraps data + info->offset, info->size in an ABuffer: the same range as a direct buffer, offset 0 */
media_status_t AMediaMuxer_writeSampleData(AMediaMuxer *mux, size_t trackIdx, const uint8_t *data, const AMediaCodecBufferInfo *info)
{
	JNIEnv *env = env_init();
	jobject buffer = (*env)->NewDirectByteBuffer(env, (void *)(data + info->offset), info->size);
	jobject jinfo = (*env)->NewObject(env, m.info_class, m.info_ctor);
	(*env)->CallVoidMethod(env, jinfo, m.info_set, 0, info->size, (jlong)info->presentationTimeUs, (jint)info->flags);
	(*env)->CallVoidMethod(env, mux->muxer, m.write, (jint)trackIdx, buffer, jinfo);
	(*env)->DeleteLocalRef(env, jinfo);
	(*env)->DeleteLocalRef(env, buffer);
	return status(env, "AMediaMuxer_writeSampleData");
}

/* MediaAppender::create() needs the demuxer and muxer of libstagefright, neither exists in ATL: it fails, NULL */
AMediaMuxer *AMediaMuxer_append(int fd, AppendMode mode)
{
	return NULL;
}

ssize_t AMediaMuxer_getTrackCount(AMediaMuxer *mux)
{
	return mux->track_count;
}

/* NULL for an index without a track, as AOSP */
AMediaFormat *AMediaMuxer_getTrackFormat(AMediaMuxer *mux, size_t idx)
{
	if (idx >= mux->track_count)
		return NULL;
	return AMediaFormat_fromJava(env_init(), mux->track_formats[idx]);
}
