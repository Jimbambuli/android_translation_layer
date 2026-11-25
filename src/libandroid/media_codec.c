/*
 * NdkMediaCodec (libmediandk) on ATL's android.media.MediaCodec. AOSP's libmediandk drives the native MediaCodec
 * (frameworks/av/media/ndk/NdkMediaCodec.cpp); in ATL the codec is the Java class with its FFmpeg JNI part, so
 * these functions call it like an app would, with AOSP's NDK return values: an exception of the Java codec is
 * AOSP's translate_error() of an error the native codec has no NDK code for, AMEDIA_ERROR_UNKNOWN.
 */

#include <pthread.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>

#include <glib.h>

#include "media_ndk.h"
#include "native_window.h"
#include "ndk_jni.h"

typedef struct AMediaCodec AMediaCodec;
typedef struct AMediaCrypto AMediaCrypto;

typedef struct AMediaCodecBufferInfo {
	int32_t offset;
	int32_t size;
	int64_t presentationTimeUs;
	uint32_t flags;
} AMediaCodecBufferInfo;

enum {
	AMEDIACODEC_INFO_OUTPUT_BUFFERS_CHANGED = -3,
	AMEDIACODEC_INFO_OUTPUT_FORMAT_CHANGED = -2,
	AMEDIACODEC_INFO_TRY_AGAIN_LATER = -1,
};

typedef void (*AMediaCodecOnAsyncInputAvailable)(AMediaCodec *codec, void *userdata, int32_t index);
typedef void (*AMediaCodecOnAsyncOutputAvailable)(AMediaCodec *codec, void *userdata, int32_t index, AMediaCodecBufferInfo *bufferInfo);
typedef void (*AMediaCodecOnAsyncFormatChanged)(AMediaCodec *codec, void *userdata, AMediaFormat *format);
typedef void (*AMediaCodecOnAsyncError)(AMediaCodec *codec, void *userdata, media_status_t error, int32_t actionCode, const char *detail);
typedef struct AMediaCodecOnAsyncNotifyCallback {
	AMediaCodecOnAsyncInputAvailable onAsyncInputAvailable;
	AMediaCodecOnAsyncOutputAvailable onAsyncOutputAvailable;
	AMediaCodecOnAsyncFormatChanged onAsyncFormatChanged;
	AMediaCodecOnAsyncError onAsyncError;
} AMediaCodecOnAsyncNotifyCallback;
typedef void (*AMediaCodecOnFrameRendered)(AMediaCodec *codec, void *userdata, int64_t mediaTimeUs, int64_t systemNano);

#define MAX_OUTPUT_BUFFERS 64

struct AMediaCodec {
	jobject codec; /* android.media.MediaCodec */
	jobject buffer_info;
	bool has_surface;
	/* AOSP gives an output buffer from its data offset on (AMediaCodec_getOutputBuffer()) and reports offset 0 */
	int32_t output_offset[MAX_OUTPUT_BUFFERS];
	int64_t output_time[MAX_OUTPUT_BUFFERS];

	GMutex lock;
	AMediaCodecOnAsyncNotifyCallback async;
	void *async_userdata;
	bool async_set;
	pthread_t async_thread;
	bool async_running;
	AMediaCodecOnFrameRendered rendered;
	void *rendered_userdata;
};

ssize_t AMediaCodec_dequeueOutputBuffer(AMediaCodec *mc, AMediaCodecBufferInfo *info, int64_t timeoutUs);
AMediaFormat *AMediaCodec_getOutputFormat(AMediaCodec *mc);

static struct {
	jclass class;
	jmethodID create_by_name, create_decoder, create_encoder, get_name, configure, start, stop, flush, release;
	jmethodID dequeue_input, get_input_buffer, queue_input, dequeue_output, get_output_buffer, release_output, release_output_at;
	jmethodID get_output_format, get_input_format, get_output_format_at, set_output_surface, set_parameters;
	jfieldID info_offset, info_size, info_time, info_flags;
	jclass buffer_info_class;
	jmethodID buffer_info_ctor;
} m;

static JNIEnv *env_init(void)
{
	JNIEnv *env = ndk_jni_env();
	if (!env || m.class)
		return env;
	jclass class = ndk_jni_class(env, "android/media/MediaCodec");
	m.create_by_name = (*env)->GetStaticMethodID(env, class, "createByCodecName", "(Ljava/lang/String;)Landroid/media/MediaCodec;");
	m.create_decoder = (*env)->GetStaticMethodID(env, class, "createDecoderByType", "(Ljava/lang/String;)Landroid/media/MediaCodec;");
	m.create_encoder = (*env)->GetStaticMethodID(env, class, "createEncoderByType", "(Ljava/lang/String;)Landroid/media/MediaCodec;");
	m.get_name = (*env)->GetMethodID(env, class, "getName", "()Ljava/lang/String;");
	m.configure = (*env)->GetMethodID(env, class, "configure", "(Landroid/media/MediaFormat;Landroid/view/Surface;Landroid/media/MediaCrypto;I)V");
	m.start = (*env)->GetMethodID(env, class, "start", "()V");
	m.stop = (*env)->GetMethodID(env, class, "stop", "()V");
	m.flush = (*env)->GetMethodID(env, class, "flush", "()V");
	m.release = (*env)->GetMethodID(env, class, "release", "()V");
	m.dequeue_input = (*env)->GetMethodID(env, class, "dequeueInputBuffer", "(J)I");
	m.get_input_buffer = (*env)->GetMethodID(env, class, "getInputBuffer", "(I)Ljava/nio/ByteBuffer;");
	m.queue_input = (*env)->GetMethodID(env, class, "queueInputBuffer", "(IIIJI)V");
	m.dequeue_output = (*env)->GetMethodID(env, class, "dequeueOutputBuffer", "(Landroid/media/MediaCodec$BufferInfo;J)I");
	m.get_output_buffer = (*env)->GetMethodID(env, class, "getOutputBuffer", "(I)Ljava/nio/ByteBuffer;");
	m.release_output = (*env)->GetMethodID(env, class, "releaseOutputBuffer", "(IZ)V");
	m.release_output_at = (*env)->GetMethodID(env, class, "releaseOutputBuffer", "(IJ)V");
	m.get_output_format = (*env)->GetMethodID(env, class, "getOutputFormat", "()Landroid/media/MediaFormat;");
	m.get_input_format = (*env)->GetMethodID(env, class, "getInputFormat", "()Landroid/media/MediaFormat;");
	m.get_output_format_at = (*env)->GetMethodID(env, class, "getOutputFormat", "(I)Landroid/media/MediaFormat;");
	m.set_output_surface = (*env)->GetMethodID(env, class, "setOutputSurface", "(Landroid/view/Surface;)V");
	m.set_parameters = (*env)->GetMethodID(env, class, "setParameters", "(Landroid/os/Bundle;)V");
	m.buffer_info_class = ndk_jni_class(env, "android/media/MediaCodec$BufferInfo");
	m.buffer_info_ctor = (*env)->GetMethodID(env, m.buffer_info_class, "<init>", "()V");
	m.info_offset = (*env)->GetFieldID(env, m.buffer_info_class, "offset", "I");
	m.info_size = (*env)->GetFieldID(env, m.buffer_info_class, "size", "I");
	m.info_time = (*env)->GetFieldID(env, m.buffer_info_class, "presentationTimeUs", "J");
	m.info_flags = (*env)->GetFieldID(env, m.buffer_info_class, "flags", "I");
	m.class = class;
	return env;
}

static media_status_t status(JNIEnv *env, const char *what)
{
	return ndk_jni_threw(env, what) ? AMEDIA_ERROR_UNKNOWN : AMEDIA_OK;
}

static AMediaCodec *create(jmethodID factory, const char *name)
{
	JNIEnv *env = env_init();
	if (!env || !name)
		return NULL;
	jstring jname = (*env)->NewStringUTF(env, name);
	jobject codec = (*env)->CallStaticObjectMethod(env, m.class, factory, jname);
	(*env)->DeleteLocalRef(env, jname);
	if (ndk_jni_threw(env, name) || !codec)
		return NULL;
	AMediaCodec *mc = calloc(1, sizeof(AMediaCodec));
	mc->codec = (*env)->NewGlobalRef(env, codec);
	jobject info = (*env)->NewObject(env, m.buffer_info_class, m.buffer_info_ctor);
	mc->buffer_info = (*env)->NewGlobalRef(env, info);
	(*env)->DeleteLocalRef(env, info);
	(*env)->DeleteLocalRef(env, codec);
	g_mutex_init(&mc->lock);
	return mc;
}

AMediaCodec *AMediaCodec_createCodecByName(const char *name)
{
	return create(env_init() ? m.create_by_name : NULL, name);
}

AMediaCodec *AMediaCodec_createDecoderByType(const char *mime_type)
{
	return create(env_init() ? m.create_decoder : NULL, mime_type);
}

AMediaCodec *AMediaCodec_createEncoderByType(const char *mime_type)
{
	return create(env_init() ? m.create_encoder : NULL, mime_type);
}

static void async_stop(AMediaCodec *mc)
{
	g_mutex_lock(&mc->lock);
	bool running = mc->async_running;
	mc->async_running = false;
	g_mutex_unlock(&mc->lock);
	if (running && !pthread_equal(pthread_self(), mc->async_thread))
		pthread_join(mc->async_thread, NULL);
}

media_status_t AMediaCodec_delete(AMediaCodec *mc)
{
	if (!mc)
		return AMEDIA_OK;
	JNIEnv *env = env_init();
	async_stop(mc);
	(*env)->CallVoidMethod(env, mc->codec, m.release);
	ndk_jni_threw(env, "AMediaCodec_delete");
	(*env)->DeleteGlobalRef(env, mc->codec);
	(*env)->DeleteGlobalRef(env, mc->buffer_info);
	g_mutex_clear(&mc->lock);
	free(mc);
	return AMEDIA_OK;
}

media_status_t AMediaCodec_getName(AMediaCodec *mc, char **out_name)
{
	if (!out_name)
		return AMEDIA_ERROR_INVALID_PARAMETER;
	JNIEnv *env = env_init();
	jstring name = (*env)->CallObjectMethod(env, mc->codec, m.get_name);
	if (ndk_jni_threw(env, "AMediaCodec_getName") || !name)
		return AMEDIA_ERROR_UNKNOWN;
	const char *s = (*env)->GetStringUTFChars(env, name, NULL);
	*out_name = strdup(s);
	(*env)->ReleaseStringUTFChars(env, name, s);
	(*env)->DeleteLocalRef(env, name);
	return AMEDIA_OK;
}

void AMediaCodec_releaseName(AMediaCodec *mc, char *name)
{
	free(name);
}

media_status_t AMediaCodec_configure(AMediaCodec *mc, const AMediaFormat *format, struct ANativeWindow *window, AMediaCrypto *crypto, uint32_t flags)
{
	JNIEnv *env = env_init();
	/* ATL's AMediaCrypto_new() never creates a crypto object (no DRM scheme), so crypto is always NULL here */
	jobject jformat = AMediaFormat_toJava(env, format);
	mc->has_surface = window && window->surface;
	(*env)->CallVoidMethod(env, mc->codec, m.configure, jformat, window ? window->surface : NULL, NULL, (jint)flags);
	(*env)->DeleteLocalRef(env, jformat);
	return status(env, "AMediaCodec_configure");
}

/* AOSP: the async notifications come from the codec's looper; here a thread of the codec takes the buffers as
 * they become free or ready (MediaCodec hands an index out once) and calls the callbacks, from start() until
 * stop(), flush() or the deletion */
static void *async_loop(void *data)
{
	AMediaCodec *mc = data;
	JNIEnv *env = env_init();
	while (true) {
		g_mutex_lock(&mc->lock);
		bool running = mc->async_running;
		AMediaCodecOnAsyncNotifyCallback cb = mc->async;
		void *userdata = mc->async_userdata;
		g_mutex_unlock(&mc->lock);
		if (!running)
			break;
		jint index = (*env)->CallIntMethod(env, mc->codec, m.dequeue_input, (jlong)0);
		if (ndk_jni_threw(env, "async dequeueInputBuffer")) {
			if (cb.onAsyncError)
				cb.onAsyncError(mc, userdata, AMEDIA_ERROR_UNKNOWN, 0, "dequeueInputBuffer failed");
			break;
		}
		if (index >= 0 && cb.onAsyncInputAvailable) {
			cb.onAsyncInputAvailable(mc, userdata, index);
			continue;
		}
		AMediaCodecBufferInfo info;
		ssize_t out = AMediaCodec_dequeueOutputBuffer(mc, &info, 10000);
		if (out >= 0 && cb.onAsyncOutputAvailable) {
			cb.onAsyncOutputAvailable(mc, userdata, out, &info);
		} else if (out == AMEDIACODEC_INFO_OUTPUT_FORMAT_CHANGED && cb.onAsyncFormatChanged) {
			AMediaFormat *format = AMediaCodec_getOutputFormat(mc);
			cb.onAsyncFormatChanged(mc, userdata, format);
			AMediaFormat_delete(format);
		} else if (out < 0 && out != AMEDIACODEC_INFO_TRY_AGAIN_LATER && out != AMEDIACODEC_INFO_OUTPUT_BUFFERS_CHANGED) {
			if (cb.onAsyncError)
				cb.onAsyncError(mc, userdata, out, 0, "dequeueOutputBuffer failed");
			break;
		}
	}
	return NULL;
}

static void async_start(AMediaCodec *mc)
{
	g_mutex_lock(&mc->lock);
	bool start = mc->async_set && !mc->async_running;
	if (start)
		mc->async_running = true;
	g_mutex_unlock(&mc->lock);
	if (start)
		pthread_create(&mc->async_thread, NULL, async_loop, mc);
}

media_status_t AMediaCodec_setAsyncNotifyCallback(AMediaCodec *mc, AMediaCodecOnAsyncNotifyCallback callback, void *userdata)
{
	g_mutex_lock(&mc->lock);
	mc->async = callback;
	mc->async_userdata = userdata;
	mc->async_set = true;
	g_mutex_unlock(&mc->lock);
	return AMEDIA_OK;
}

/* stored like ATL's MediaCodec.setOnFrameRenderedListener(); ATL's codec does not report rendered frames */
media_status_t AMediaCodec_setOnFrameRenderedCallback(AMediaCodec *mc, AMediaCodecOnFrameRendered callback, void *userdata)
{
	g_mutex_lock(&mc->lock);
	mc->rendered = callback;
	mc->rendered_userdata = userdata;
	g_mutex_unlock(&mc->lock);
	return AMEDIA_OK;
}

/* AOSP MediaCodec::onReleaseCrypto(): INVALID_OPERATION without a crypto object, which ATL never has */
media_status_t AMediaCodec_releaseCrypto(AMediaCodec *mc)
{
	return AMEDIA_ERROR_UNKNOWN;
}

media_status_t AMediaCodec_start(AMediaCodec *mc)
{
	JNIEnv *env = env_init();
	(*env)->CallVoidMethod(env, mc->codec, m.start);
	media_status_t ret = status(env, "AMediaCodec_start");
	if (ret == AMEDIA_OK)
		async_start(mc);
	return ret;
}

media_status_t AMediaCodec_stop(AMediaCodec *mc)
{
	JNIEnv *env = env_init();
	async_stop(mc);
	(*env)->CallVoidMethod(env, mc->codec, m.stop);
	return status(env, "AMediaCodec_stop");
}

/* AOSP: in asynchronous mode the codec waits for start() after a flush */
media_status_t AMediaCodec_flush(AMediaCodec *mc)
{
	JNIEnv *env = env_init();
	async_stop(mc);
	(*env)->CallVoidMethod(env, mc->codec, m.flush);
	return status(env, "AMediaCodec_flush");
}

ssize_t AMediaCodec_dequeueInputBuffer(AMediaCodec *mc, int64_t timeoutUs)
{
	JNIEnv *env = env_init();
	jint index = (*env)->CallIntMethod(env, mc->codec, m.dequeue_input, (jlong)timeoutUs);
	if (ndk_jni_threw(env, "AMediaCodec_dequeueInputBuffer"))
		return AMEDIA_ERROR_UNKNOWN;
	return index;
}

static uint8_t *buffer_address(JNIEnv *env, jobject buffer, size_t offset, size_t *out_size)
{
	if (!buffer) {
		if (out_size)
			*out_size = 0;
		return NULL;
	}
	uint8_t *data = (*env)->GetDirectBufferAddress(env, buffer);
	jlong capacity = (*env)->GetDirectBufferCapacity(env, buffer);
	(*env)->DeleteLocalRef(env, buffer);
	if (!data)
		return NULL;
	if (out_size)
		*out_size = capacity - offset;
	return data + offset;
}

uint8_t *AMediaCodec_getInputBuffer(AMediaCodec *mc, size_t idx, size_t *out_size)
{
	JNIEnv *env = env_init();
	jobject buffer = (*env)->CallObjectMethod(env, mc->codec, m.get_input_buffer, (jint)idx);
	if (ndk_jni_threw(env, "AMediaCodec_getInputBuffer"))
		return NULL;
	return buffer_address(env, buffer, 0, out_size);
}

/* AOSP MediaCodec.getOutputBuffer(): no buffer for a codec configured with an output surface */
uint8_t *AMediaCodec_getOutputBuffer(AMediaCodec *mc, size_t idx, size_t *out_size)
{
	if (mc->has_surface) {
		if (out_size)
			*out_size = 0;
		return NULL;
	}
	JNIEnv *env = env_init();
	jobject buffer = (*env)->CallObjectMethod(env, mc->codec, m.get_output_buffer, (jint)idx);
	if (ndk_jni_threw(env, "AMediaCodec_getOutputBuffer"))
		return NULL;
	return buffer_address(env, buffer, idx < MAX_OUTPUT_BUFFERS ? mc->output_offset[idx] : 0, out_size);
}

media_status_t AMediaCodec_queueInputBuffer(AMediaCodec *mc, size_t idx, off_t offset, size_t size, uint64_t time, uint32_t flags)
{
	JNIEnv *env = env_init();
	(*env)->CallVoidMethod(env, mc->codec, m.queue_input, (jint)idx, (jint)offset, (jint)size, (jlong)time, (jint)flags);
	return status(env, "AMediaCodec_queueInputBuffer");
}

ssize_t AMediaCodec_dequeueOutputBuffer(AMediaCodec *mc, AMediaCodecBufferInfo *info, int64_t timeoutUs)
{
	JNIEnv *env = env_init();
	jint index = (*env)->CallIntMethod(env, mc->codec, m.dequeue_output, mc->buffer_info, (jlong)timeoutUs);
	if (ndk_jni_threw(env, "AMediaCodec_dequeueOutputBuffer"))
		return AMEDIA_ERROR_UNKNOWN;
	if (index >= 0) {
		int32_t offset = (*env)->GetIntField(env, mc->buffer_info, m.info_offset);
		if (index < MAX_OUTPUT_BUFFERS) {
			mc->output_offset[index] = offset;
			mc->output_time[index] = (*env)->GetLongField(env, mc->buffer_info, m.info_time);
		}
		info->offset = 0;
		info->size = (*env)->GetIntField(env, mc->buffer_info, m.info_size);
		info->presentationTimeUs = (*env)->GetLongField(env, mc->buffer_info, m.info_time);
		info->flags = (*env)->GetIntField(env, mc->buffer_info, m.info_flags);
	}
	return index;
}

static AMediaFormat *format_from(JNIEnv *env, jobject jformat, const char *what)
{
	if (ndk_jni_threw(env, what) || !jformat)
		return NULL;
	AMediaFormat *format = AMediaFormat_fromJava(env, jformat);
	(*env)->DeleteLocalRef(env, jformat);
	return format;
}

AMediaFormat *AMediaCodec_getOutputFormat(AMediaCodec *mc)
{
	JNIEnv *env = env_init();
	return format_from(env, (*env)->CallObjectMethod(env, mc->codec, m.get_output_format), "AMediaCodec_getOutputFormat");
}

AMediaFormat *AMediaCodec_getInputFormat(AMediaCodec *mc)
{
	JNIEnv *env = env_init();
	return format_from(env, (*env)->CallObjectMethod(env, mc->codec, m.get_input_format), "AMediaCodec_getInputFormat");
}

AMediaFormat *AMediaCodec_getBufferFormat(AMediaCodec *mc, size_t index)
{
	JNIEnv *env = env_init();
	return format_from(env, (*env)->CallObjectMethod(env, mc->codec, m.get_output_format_at, (jint)index), "AMediaCodec_getBufferFormat");
}

media_status_t AMediaCodec_releaseOutputBuffer(AMediaCodec *mc, size_t idx, bool render)
{
	JNIEnv *env = env_init();
	(*env)->CallVoidMethod(env, mc->codec, m.release_output, (jint)idx, (jboolean)render);
	return status(env, "AMediaCodec_releaseOutputBuffer");
}

media_status_t AMediaCodec_releaseOutputBufferAtTime(AMediaCodec *mc, size_t idx, int64_t timestampNs)
{
	JNIEnv *env = env_init();
	(*env)->CallVoidMethod(env, mc->codec, m.release_output_at, (jint)idx, (jlong)timestampNs);
	return status(env, "AMediaCodec_releaseOutputBufferAtTime");
}

media_status_t AMediaCodec_setOutputSurface(AMediaCodec *mc, struct ANativeWindow *window)
{
	JNIEnv *env = env_init();
	(*env)->CallVoidMethod(env, mc->codec, m.set_output_surface, window ? window->surface : NULL);
	media_status_t ret = status(env, "AMediaCodec_setOutputSurface");
	if (ret == AMEDIA_OK)
		mc->has_surface = window && window->surface;
	return ret;
}

/* AOSP NdkMediaCodec: the Surface of MediaCodec.createInputSurface() as an ANativeWindow; INVALID_OPERATION (here
 * the IllegalStateException) for a codec that is not a video encoder configured for surface input */
media_status_t AMediaCodec_createInputSurface(AMediaCodec *mc, struct ANativeWindow **surface)
{
	if (!surface || !mc)
		return AMEDIA_ERROR_INVALID_PARAMETER;
	*surface = NULL;
	JNIEnv *env = env_init();
	jobject java_surface = (*env)->CallObjectMethod(env, mc->codec, (*env)->GetMethodID(env, m.class, "createInputSurface", "()Landroid/view/Surface;"));
	if (ndk_jni_threw(env, "AMediaCodec_createInputSurface") || !java_surface)
		return AMEDIA_ERROR_INVALID_OPERATION;
	*surface = ANativeWindow_fromConsumerSurface(env, java_surface);
	(*env)->DeleteLocalRef(env, java_surface);
	return AMEDIA_OK;
}

/* AOSP: a PersistentSurface (MediaCodec.createPersistentInputSurface()) as an ANativeWindow */
media_status_t AMediaCodec_createPersistentInputSurface(struct ANativeWindow **surface)
{
	if (!surface)
		return AMEDIA_ERROR_INVALID_PARAMETER;
	*surface = NULL;
	JNIEnv *env = env_init();
	jobject java_surface = (*env)->CallStaticObjectMethod(env, m.class, (*env)->GetStaticMethodID(env, m.class, "createPersistentInputSurface", "()Landroid/view/Surface;"));
	if (ndk_jni_threw(env, "AMediaCodec_createPersistentInputSurface") || !java_surface)
		return AMEDIA_ERROR_UNKNOWN;
	*surface = ANativeWindow_fromConsumerSurface(env, java_surface);
	(*env)->DeleteLocalRef(env, java_surface);
	return AMEDIA_OK;
}

/* AOSP: only a persistent surface (from AMediaCodec_createPersistentInputSurface()) is accepted; the window gets the
 * encoder as its consumer */
media_status_t AMediaCodec_setInputSurface(AMediaCodec *mc, struct ANativeWindow *surface)
{
	if (!mc || !surface || !surface->surface)
		return AMEDIA_ERROR_INVALID_PARAMETER;
	JNIEnv *env = env_init();
	(*env)->CallVoidMethod(env, mc->codec, (*env)->GetMethodID(env, m.class, "setInputSurface", "(Landroid/view/Surface;)V"), surface->surface);
	if (ndk_jni_threw(env, "AMediaCodec_setInputSurface"))
		return AMEDIA_ERROR_INVALID_PARAMETER;
	jobject reader = (*env)->GetObjectField(env, surface->surface, (*env)->GetFieldID(env, ndk_jni_class(env, "android/view/Surface"), "atl_imageReader", "Landroid/media/ImageReader;"));
	ANativeWindow_setImageReader(surface, env, reader);
	(*env)->DeleteLocalRef(env, reader);
	return AMEDIA_OK;
}

/* AOSP: the window of the persistent surface goes */
media_status_t AMediaCodec_releasePersistentInputSurface(struct ANativeWindow *surface)
{
	if (!surface)
		return AMEDIA_ERROR_INVALID_PARAMETER;
	ANativeWindow_release(surface);
	return AMEDIA_OK;
}

/* AOSP hands the parameters to the native codec. ATL's MediaCodec.setParameters() applies none (its FFmpeg
 * decoders have no such parameters), so it gets an empty Bundle */
media_status_t AMediaCodec_setParameters(AMediaCodec *mc, const AMediaFormat *params)
{
	if (!params || !mc)
		return AMEDIA_ERROR_INVALID_PARAMETER;
	JNIEnv *env = env_init();
	jclass bundle_class = ndk_jni_class(env, "android/os/Bundle");
	jobject bundle = (*env)->NewObject(env, bundle_class, (*env)->GetMethodID(env, bundle_class, "<init>", "()V"));
	(*env)->CallVoidMethod(env, mc->codec, m.set_parameters, bundle);
	(*env)->DeleteLocalRef(env, bundle);
	return status(env, "AMediaCodec_setParameters");
}

/* AOSP: MediaCodec::signalEndOfInputStream() is INVALID_OPERATION but for an encoder with an input surface */
media_status_t AMediaCodec_signalEndOfInputStream(AMediaCodec *mc)
{
	if (!mc)
		return AMEDIA_ERROR_INVALID_PARAMETER;
	JNIEnv *env = env_init();
	(*env)->CallVoidMethod(env, mc->codec, (*env)->GetMethodID(env, m.class, "signalEndOfInputStream", "()V"));
	return ndk_jni_threw(env, "AMediaCodec_signalEndOfInputStream") ? AMEDIA_ERROR_INVALID_OPERATION : AMEDIA_OK;
}

typedef enum {
	AMEDIACODECRYPTOINFO_MODE_CLEAR = 0,
	AMEDIACODECRYPTOINFO_MODE_AES_CTR = 1,
	AMEDIACODECRYPTOINFO_MODE_AES_WV = 2,
	AMEDIACODECRYPTOINFO_MODE_AES_CBC = 3
} cryptoinfo_mode_t;

typedef struct {
	int32_t encryptBlocks;
	int32_t skipBlocks;
} cryptoinfo_pattern_t;

typedef struct AMediaCodecCryptoInfo {
	int numsubsamples;
	uint8_t key[16];
	uint8_t iv[16];
	cryptoinfo_mode_t mode;
	cryptoinfo_pattern_t pattern;
	size_t *clearbytes;
	size_t *encryptedbytes;
} AMediaCodecCryptoInfo;

/* AOSP: a secure buffer needs the crypto object of configure(); ATL never has one (see MediaCodec.queueSecureInputBuffer) */
media_status_t AMediaCodec_queueSecureInputBuffer(AMediaCodec *mc, size_t idx, off_t offset, AMediaCodecCryptoInfo *crypto, uint64_t time, uint32_t flags)
{
	return AMEDIA_ERROR_UNKNOWN;
}

/* MediaCodec.CodecException ACTION_TRANSIENT, ACTION_RECOVERABLE */
bool AMediaCodecActionCode_isRecoverable(int32_t actionCode)
{
	return actionCode == 2;
}

bool AMediaCodecActionCode_isTransient(int32_t actionCode)
{
	return actionCode == 1;
}

/* AMediaCodecCryptoInfo: plain data, as in AOSP */
void AMediaCodecCryptoInfo_setPattern(AMediaCodecCryptoInfo *info, cryptoinfo_pattern_t *pattern)
{
	info->pattern.encryptBlocks = pattern->encryptBlocks;
	info->pattern.skipBlocks = pattern->skipBlocks;
}

AMediaCodecCryptoInfo *AMediaCodecCryptoInfo_new(int numsubsamples, uint8_t key[16], uint8_t iv[16], cryptoinfo_mode_t mode, size_t *clearbytes, size_t *encryptedbytes)
{
	size_t cryptosize;
	if (__builtin_mul_overflow(sizeof(size_t) * 2, numsubsamples, &cryptosize) ||
	    __builtin_add_overflow(cryptosize, sizeof(AMediaCodecCryptoInfo), &cryptosize))
		return NULL;
	AMediaCodecCryptoInfo *ret = malloc(cryptosize);
	if (!ret)
		return NULL;
	ret->numsubsamples = numsubsamples;
	memcpy(ret->key, key, 16);
	memcpy(ret->iv, iv, 16);
	ret->mode = mode;
	ret->pattern.encryptBlocks = 0;
	ret->pattern.skipBlocks = 0;
	ret->clearbytes = (size_t *)(ret + 1);
	ret->encryptedbytes = ret->clearbytes + numsubsamples;
	memcpy(ret->clearbytes, clearbytes, numsubsamples * sizeof(size_t));
	memcpy(ret->encryptedbytes, encryptedbytes, numsubsamples * sizeof(size_t));
	return ret;
}

media_status_t AMediaCodecCryptoInfo_delete(AMediaCodecCryptoInfo *info)
{
	free(info);
	return AMEDIA_OK;
}

size_t AMediaCodecCryptoInfo_getNumSubSamples(AMediaCodecCryptoInfo *ci)
{
	return ci->numsubsamples;
}

media_status_t AMediaCodecCryptoInfo_getKey(AMediaCodecCryptoInfo *ci, uint8_t *dst)
{
	if (!ci)
		return AMEDIA_ERROR_INVALID_OBJECT;
	if (!dst)
		return AMEDIA_ERROR_INVALID_PARAMETER;
	memcpy(dst, ci->key, 16);
	return AMEDIA_OK;
}

media_status_t AMediaCodecCryptoInfo_getIV(AMediaCodecCryptoInfo *ci, uint8_t *dst)
{
	if (!ci)
		return AMEDIA_ERROR_INVALID_OBJECT;
	if (!dst)
		return AMEDIA_ERROR_INVALID_PARAMETER;
	memcpy(dst, ci->iv, 16);
	return AMEDIA_OK;
}

cryptoinfo_mode_t AMediaCodecCryptoInfo_getMode(AMediaCodecCryptoInfo *ci)
{
	if (!ci)
		return (cryptoinfo_mode_t)AMEDIA_ERROR_INVALID_OBJECT;
	return ci->mode;
}

media_status_t AMediaCodecCryptoInfo_getClearBytes(AMediaCodecCryptoInfo *ci, size_t *dst)
{
	if (!ci)
		return AMEDIA_ERROR_INVALID_OBJECT;
	if (!dst)
		return AMEDIA_ERROR_INVALID_PARAMETER;
	memcpy(dst, ci->clearbytes, sizeof(size_t) * ci->numsubsamples);
	return AMEDIA_OK;
}

media_status_t AMediaCodecCryptoInfo_getEncryptedBytes(AMediaCodecCryptoInfo *ci, size_t *dst)
{
	if (!ci)
		return AMEDIA_ERROR_INVALID_OBJECT;
	if (!dst)
		return AMEDIA_ERROR_INVALID_PARAMETER;
	memcpy(dst, ci->encryptedbytes, sizeof(size_t) * ci->numsubsamples);
	return AMEDIA_OK;
}

const char *AMEDIACODEC_KEY_HDR10_PLUS_INFO = "hdr10-plus-info";
const char *AMEDIACODEC_KEY_LOW_LATENCY = "low-latency";
const char *AMEDIACODEC_KEY_OFFSET_TIME = "time-offset-us";
const char *AMEDIACODEC_KEY_REQUEST_SYNC_FRAME = "request-sync";
const char *AMEDIACODEC_KEY_SUSPEND = "drop-input-frames";
const char *AMEDIACODEC_KEY_SUSPEND_TIME = "drop-start-time-us";
const char *AMEDIACODEC_KEY_VIDEO_BITRATE = "video-bitrate";
