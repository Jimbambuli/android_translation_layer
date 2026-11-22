/*
 * AAudio (NDK libaaudio, interface of Android 16) on ALSA, the backend of ATL's AudioTrack and AudioRecord.
 *
 * The behaviour follows AOSP's non-MMAP path in frameworks/av/media/libaaudio:
 * src/core (AAudioAudio.cpp, AudioStream.cpp, AudioStreamBuilder.cpp, AAudioStreamParameters.cpp)
 * and src/legacy (AudioStreamTrack.cpp, AudioStreamRecord.cpp, AudioStreamLegacy.cpp).
 * There is no MMAP path: streams are always SHARED, AAudioStream_isMMapUsed() is false and an
 * AAUDIO_POLICY_ALWAYS MMAP policy fails like on a device without MMAP. There is no direct or
 * offload output either, so compressed formats and POWER_SAVING_OFFLOADED fail like on a device
 * that has no such output.
 */

#define _GNU_SOURCE
#include <alsa/asoundlib.h>
#include <errno.h>
#include <limits.h>
#include <linux/futex.h>
#include <pthread.h>
#include <stdatomic.h>
#include <stdbool.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <sys/syscall.h>
#include <time.h>
#include <unistd.h>

#include <glib.h>

#define __INTRODUCED_IN(api_level)
#define _Nonnull
#define _Nullable
#define _Null_unspecified
#include "aaudio/AAudio.h"

/* as PCM_DEVICE in api-impl-jni/audio/audio.h */
#define PCM_DEVICE "default"

/* AAudioUtilities.h: AAUDIO_CHANNEL_INDEX_MASK_n = AAUDIO_CHANNEL_BIT_INDEX | ((1 << n) - 1) */
#define AAUDIO_CHANNEL_BIT_INDEX 0x80000000

#define AAUDIO_NANOS_PER_MILLISECOND 1000000LL
#define AAUDIO_NANOS_PER_SECOND 1000000000LL
/* system/media audio.h */
#define SAMPLE_RATE_HZ_MIN_AAUDIO 8000
#define SAMPLE_RATE_HZ_MAX_IEC610937 1600000
#define CHANNEL_COUNT_MIN_AAUDIO 1
#define CHANNEL_COUNT_MAX_AAUDIO 12 /* FCC_LIMIT */
#define AUDIO_CHANNEL_COUNT_MAX 30
/* AudioStreamBuilder.cpp */
#define FRAMES_PER_DATA_CALLBACK_MIN 1
#define FRAMES_PER_DATA_CALLBACK_MAX (1024 * 1024)
/* AudioFlinger would take the rate of the output; ALSA's "default" device has none to offer */
#define DEFAULT_SAMPLE_RATE 48000

enum privacy_sensitive_request {
	PRIVACY_SENSITIVE_DEFAULT,
	PRIVACY_SENSITIVE_DISABLED,
	PRIVACY_SENSITIVE_ENABLED,
};

/*
 * The package name and attribution tag only identify the app towards the permission checks of
 * the audio server, and the presentation end callback only exists for offloaded streams, which
 * can't be opened here. None of them is kept.
 */
struct AAudioStreamBuilderStruct {
	int32_t sample_rate;
	int32_t samples_per_frame;
	aaudio_channel_mask_t channel_mask;
	int32_t device_id;
	aaudio_sharing_mode_t sharing_mode;
	aaudio_format_t format;
	aaudio_direction_t direction;
	aaudio_usage_t usage;
	aaudio_content_type_t content_type;
	aaudio_spatialization_behavior_t spatialization_behavior;
	bool is_content_spatialized;
	aaudio_input_preset_t input_preset;
	int32_t buffer_capacity;
	aaudio_allowed_capture_policy_t allowed_capture_policy;
	aaudio_session_id_t session_id;
	enum privacy_sensitive_request privacy_sensitive_request;
	aaudio_performance_mode_t performance_mode;
	int32_t frames_per_data_callback;
	AAudioStream_dataCallback data_callback;
	void *data_callback_user_data;
	AAudioStream_errorCallback error_callback;
	void *error_callback_user_data;
};

struct AAudioStreamStruct {
	pthread_mutex_t lock;
	_Atomic int32_t state; /* also the futex word for AAudioStream_waitForStateChange() */
	atomic_bool disconnected;
	snd_pcm_t *pcm;
	bool can_pause;

	aaudio_direction_t direction;
	int32_t sample_rate;
	int32_t samples_per_frame;
	aaudio_channel_mask_t channel_mask;
	aaudio_format_t format;
	int32_t bytes_per_frame;
	aaudio_performance_mode_t performance_mode;
	aaudio_usage_t usage;
	aaudio_content_type_t content_type;
	aaudio_spatialization_behavior_t spatialization_behavior;
	bool is_content_spatialized;
	aaudio_input_preset_t input_preset;
	aaudio_allowed_capture_policy_t allowed_capture_policy;
	bool is_privacy_sensitive;
	aaudio_session_id_t session_id;
	int32_t frames_per_burst;
	int32_t buffer_capacity;
	_Atomic int32_t buffer_size;
	_Atomic int32_t xrun_count;
	_Atomic int64_t frames_written;
	_Atomic int64_t frames_read;

	int32_t frames_per_data_callback;
	AAudioStream_dataCallback data_callback;
	void *data_callback_user_data;
	AAudioStream_errorCallback error_callback;
	void *error_callback_user_data;
	void *callback_buffer;
	bool has_thread;
	pthread_t thread;
	atomic_bool callback_enabled;
	_Atomic pid_t data_callback_thread;
	_Atomic pid_t error_callback_thread;
};

static aaudio_policy_t mmap_policy = AAUDIO_UNSPECIFIED;
static _Atomic aaudio_session_id_t next_session_id = 1;

/* In a Flatpak the microphone is PulseAudio's, otherwise only with ATL_UGLY_ENABLE_MICROPHONE (as microphone_allowed() in api-impl-jni/audio/audio.c) */
static bool microphone_allowed(void)
{
	return getenv("ATL_UGLY_ENABLE_MICROPHONE") || g_file_test("/.flatpak-info", G_FILE_TEST_EXISTS);
}

#define AAUDIO_CASE_ENUM(name) \
	case name:             \
		return #name

const char *AAudio_convertResultToText(aaudio_result_t returnCode)
{
	switch (returnCode) {
		AAUDIO_CASE_ENUM(AAUDIO_OK);
		AAUDIO_CASE_ENUM(AAUDIO_ERROR_DISCONNECTED);
		AAUDIO_CASE_ENUM(AAUDIO_ERROR_ILLEGAL_ARGUMENT);
		AAUDIO_CASE_ENUM(AAUDIO_ERROR_INTERNAL);
		AAUDIO_CASE_ENUM(AAUDIO_ERROR_INVALID_STATE);
		AAUDIO_CASE_ENUM(AAUDIO_ERROR_INVALID_HANDLE);
		AAUDIO_CASE_ENUM(AAUDIO_ERROR_UNIMPLEMENTED);
		AAUDIO_CASE_ENUM(AAUDIO_ERROR_UNAVAILABLE);
		AAUDIO_CASE_ENUM(AAUDIO_ERROR_NO_FREE_HANDLES);
		AAUDIO_CASE_ENUM(AAUDIO_ERROR_NO_MEMORY);
		AAUDIO_CASE_ENUM(AAUDIO_ERROR_NULL);
		AAUDIO_CASE_ENUM(AAUDIO_ERROR_TIMEOUT);
		AAUDIO_CASE_ENUM(AAUDIO_ERROR_WOULD_BLOCK);
		AAUDIO_CASE_ENUM(AAUDIO_ERROR_INVALID_FORMAT);
		AAUDIO_CASE_ENUM(AAUDIO_ERROR_OUT_OF_RANGE);
		AAUDIO_CASE_ENUM(AAUDIO_ERROR_NO_SERVICE);
		AAUDIO_CASE_ENUM(AAUDIO_ERROR_INVALID_RATE);
	}
	return "Unrecognized";
}

const char *AAudio_convertStreamStateToText(aaudio_stream_state_t state)
{
	switch (state) {
		AAUDIO_CASE_ENUM(AAUDIO_STREAM_STATE_UNINITIALIZED);
		AAUDIO_CASE_ENUM(AAUDIO_STREAM_STATE_UNKNOWN);
		AAUDIO_CASE_ENUM(AAUDIO_STREAM_STATE_OPEN);
		AAUDIO_CASE_ENUM(AAUDIO_STREAM_STATE_STARTING);
		AAUDIO_CASE_ENUM(AAUDIO_STREAM_STATE_STARTED);
		AAUDIO_CASE_ENUM(AAUDIO_STREAM_STATE_PAUSING);
		AAUDIO_CASE_ENUM(AAUDIO_STREAM_STATE_PAUSED);
		AAUDIO_CASE_ENUM(AAUDIO_STREAM_STATE_FLUSHING);
		AAUDIO_CASE_ENUM(AAUDIO_STREAM_STATE_FLUSHED);
		AAUDIO_CASE_ENUM(AAUDIO_STREAM_STATE_STOPPING);
		AAUDIO_CASE_ENUM(AAUDIO_STREAM_STATE_STOPPED);
		AAUDIO_CASE_ENUM(AAUDIO_STREAM_STATE_CLOSING);
		AAUDIO_CASE_ENUM(AAUDIO_STREAM_STATE_CLOSED);
		AAUDIO_CASE_ENUM(AAUDIO_STREAM_STATE_DISCONNECTED);
	}
	return "Unrecognized";
}

/* ---- MMAP policy: ATL has no MMAP path ---- */

aaudio_policy_t AAudio_getMMapPolicy(void)
{
	return mmap_policy;
}

aaudio_result_t AAudio_setMMapPolicy(aaudio_policy_t policy)
{
	switch (policy) {
		case AAUDIO_UNSPECIFIED:
		case AAUDIO_POLICY_NEVER:
		case AAUDIO_POLICY_AUTO:
		case AAUDIO_POLICY_ALWAYS:
			mmap_policy = policy;
			return AAUDIO_OK;
		default:
			return AAUDIO_ERROR_ILLEGAL_ARGUMENT;
	}
}

static aaudio_policy_t platform_mmap_policy(AAudio_DeviceType device, aaudio_direction_t direction)
{
	if (direction != AAUDIO_DIRECTION_INPUT && direction != AAUDIO_DIRECTION_OUTPUT)
		return AAUDIO_ERROR_ILLEGAL_ARGUMENT;
	if (device < AAUDIO_DEVICE_BUILTIN_EARPIECE || device > AAUDIO_DEVICE_DOCK_ANALOG)
		return AAUDIO_ERROR_ILLEGAL_ARGUMENT;
	return AAUDIO_POLICY_NEVER;
}

aaudio_policy_t AAudio_getPlatformMMapPolicy(AAudio_DeviceType device, aaudio_direction_t direction)
{
	return platform_mmap_policy(device, direction);
}

aaudio_policy_t AAudio_getPlatformMMapExclusivePolicy(AAudio_DeviceType device, aaudio_direction_t direction)
{
	return platform_mmap_policy(device, direction);
}

/* ---- builder ---- */

aaudio_result_t AAudio_createStreamBuilder(AAudioStreamBuilder **builder)
{
	AAudioStreamBuilder *b = calloc(1, sizeof(*b));
	if (!b)
		return AAUDIO_ERROR_NO_MEMORY;
	/* defaults of AAudioStreamParameters and AudioStreamBuilder; everything else is AAUDIO_UNSPECIFIED (0) */
	b->sharing_mode = AAUDIO_SHARING_MODE_SHARED;
	b->direction = AAUDIO_DIRECTION_OUTPUT;
	b->session_id = AAUDIO_SESSION_ID_NONE;
	b->privacy_sensitive_request = PRIVACY_SENSITIVE_DEFAULT;
	b->performance_mode = AAUDIO_PERFORMANCE_MODE_NONE;
	*builder = b;
	return AAUDIO_OK;
}

aaudio_result_t AAudioStreamBuilder_delete(AAudioStreamBuilder *builder)
{
	if (!builder)
		return AAUDIO_ERROR_NULL;
	free(builder);
	return AAUDIO_OK;
}

void AAudioStreamBuilder_setDeviceId(AAudioStreamBuilder *builder, int32_t deviceId)
{
	builder->device_id = deviceId;
}

void AAudioStreamBuilder_setPackageName(AAudioStreamBuilder *builder, const char *packageName) {}

void AAudioStreamBuilder_setAttributionTag(AAudioStreamBuilder *builder, const char *attributionTag) {}

void AAudioStreamBuilder_setSampleRate(AAudioStreamBuilder *builder, int32_t sampleRate)
{
	builder->sample_rate = sampleRate;
}

void AAudioStreamBuilder_setChannelMask(AAudioStreamBuilder *builder, aaudio_channel_mask_t channelMask)
{
	builder->channel_mask = channelMask;
	builder->samples_per_frame = __builtin_popcount(channelMask & ~AAUDIO_CHANNEL_BIT_INDEX);
}

/* AAudioConvert_channelCountToMask(): a channel count always becomes an index mask */
void AAudioStreamBuilder_setSamplesPerFrame(AAudioStreamBuilder *builder, int32_t samplesPerFrame)
{
	aaudio_channel_mask_t mask;
	if (samplesPerFrame < 0 || samplesPerFrame > AUDIO_CHANNEL_COUNT_MAX)
		mask = AAUDIO_CHANNEL_INVALID;
	else if (samplesPerFrame == 0)
		mask = AAUDIO_UNSPECIFIED;
	else
		mask = AAUDIO_CHANNEL_BIT_INDEX | ((1 << samplesPerFrame) - 1);
	AAudioStreamBuilder_setChannelMask(builder, mask);
}

void AAudioStreamBuilder_setChannelCount(AAudioStreamBuilder *builder, int32_t channelCount)
{
	AAudioStreamBuilder_setSamplesPerFrame(builder, channelCount);
}

void AAudioStreamBuilder_setDirection(AAudioStreamBuilder *builder, aaudio_direction_t direction)
{
	builder->direction = direction;
}

void AAudioStreamBuilder_setFormat(AAudioStreamBuilder *builder, aaudio_format_t format)
{
	builder->format = format;
}

void AAudioStreamBuilder_setSharingMode(AAudioStreamBuilder *builder, aaudio_sharing_mode_t sharingMode)
{
	builder->sharing_mode = sharingMode;
}

void AAudioStreamBuilder_setUsage(AAudioStreamBuilder *builder, aaudio_usage_t usage)
{
	builder->usage = usage;
}

void AAudioStreamBuilder_setContentType(AAudioStreamBuilder *builder, aaudio_content_type_t contentType)
{
	builder->content_type = contentType;
}

void AAudioStreamBuilder_setSpatializationBehavior(AAudioStreamBuilder *builder, aaudio_spatialization_behavior_t spatializationBehavior)
{
	builder->spatialization_behavior = spatializationBehavior;
}

void AAudioStreamBuilder_setIsContentSpatialized(AAudioStreamBuilder *builder, bool isSpatialized)
{
	builder->is_content_spatialized = isSpatialized;
}

void AAudioStreamBuilder_setInputPreset(AAudioStreamBuilder *builder, aaudio_input_preset_t inputPreset)
{
	builder->input_preset = inputPreset;
}

void AAudioStreamBuilder_setPrivacySensitive(AAudioStreamBuilder *builder, bool privacySensitive)
{
	builder->privacy_sensitive_request = privacySensitive ? PRIVACY_SENSITIVE_ENABLED : PRIVACY_SENSITIVE_DISABLED;
}

void AAudioStreamBuilder_setBufferCapacityInFrames(AAudioStreamBuilder *builder, int32_t numFrames)
{
	builder->buffer_capacity = numFrames;
}

void AAudioStreamBuilder_setAllowedCapturePolicy(AAudioStreamBuilder *builder, aaudio_allowed_capture_policy_t capturePolicy)
{
	builder->allowed_capture_policy = capturePolicy;
}

void AAudioStreamBuilder_setSessionId(AAudioStreamBuilder *builder, aaudio_session_id_t sessionId)
{
	builder->session_id = sessionId;
}

void AAudioStreamBuilder_setPerformanceMode(AAudioStreamBuilder *builder, aaudio_performance_mode_t mode)
{
	builder->performance_mode = mode;
}

void AAudioStreamBuilder_setDataCallback(AAudioStreamBuilder *builder, AAudioStream_dataCallback callback, void *userData)
{
	builder->data_callback = callback;
	builder->data_callback_user_data = userData;
}

void AAudioStreamBuilder_setErrorCallback(AAudioStreamBuilder *builder, AAudioStream_errorCallback callback, void *userData)
{
	builder->error_callback = callback;
	builder->error_callback_user_data = userData;
}

void AAudioStreamBuilder_setPresentationEndCallback(AAudioStreamBuilder *builder, AAudioStream_presentationEndCallback callback, void *userData) {}

void AAudioStreamBuilder_setFramesPerDataCallback(AAudioStreamBuilder *builder, int32_t numFrames)
{
	builder->frames_per_data_callback = numFrames;
}

static bool is_compressed_format(aaudio_format_t format)
{
	switch (format) {
		case AAUDIO_FORMAT_IEC61937:
		case AAUDIO_FORMAT_MP3:
		case AAUDIO_FORMAT_AAC_LC:
		case AAUDIO_FORMAT_AAC_HE_V1:
		case AAUDIO_FORMAT_AAC_HE_V2:
		case AAUDIO_FORMAT_AAC_ELD:
		case AAUDIO_FORMAT_AAC_XHE:
		case AAUDIO_FORMAT_OPUS:
			return true;
		default:
			return false;
	}
}

/* AAudioStreamParameters::validateChannelMask() */
static aaudio_result_t validate_channel_mask(aaudio_channel_mask_t mask, aaudio_direction_t direction)
{
	if (mask == AAUDIO_UNSPECIFIED)
		return AAUDIO_OK;
	if (mask & AAUDIO_CHANNEL_BIT_INDEX) {
		uint32_t bits = mask & ~AAUDIO_CHANNEL_BIT_INDEX; /* AAUDIO_CHANNEL_INDEX_MASK_1 .. _24 */
		return (bits != 0 && bits < (1u << 24) && (bits & (bits + 1)) == 0) ? AAUDIO_OK : AAUDIO_ERROR_ILLEGAL_ARGUMENT;
	}
	if (direction == AAUDIO_DIRECTION_INPUT) {
		switch (mask) {
			case AAUDIO_CHANNEL_MONO:
			case AAUDIO_CHANNEL_STEREO:
			case AAUDIO_CHANNEL_FRONT_BACK:
			case AAUDIO_CHANNEL_2POINT0POINT2:
			case AAUDIO_CHANNEL_2POINT1POINT2:
			case AAUDIO_CHANNEL_3POINT0POINT2:
			case AAUDIO_CHANNEL_3POINT1POINT2:
			case AAUDIO_CHANNEL_5POINT1:
				return AAUDIO_OK;
			default:
				return AAUDIO_ERROR_ILLEGAL_ARGUMENT;
		}
	}
	switch (mask) {
		case AAUDIO_CHANNEL_MONO:
		case AAUDIO_CHANNEL_STEREO:
		case AAUDIO_CHANNEL_2POINT1:
		case AAUDIO_CHANNEL_TRI:
		case AAUDIO_CHANNEL_TRI_BACK:
		case AAUDIO_CHANNEL_3POINT1:
		case AAUDIO_CHANNEL_2POINT0POINT2:
		case AAUDIO_CHANNEL_2POINT1POINT2:
		case AAUDIO_CHANNEL_3POINT0POINT2:
		case AAUDIO_CHANNEL_3POINT1POINT2:
		case AAUDIO_CHANNEL_QUAD:
		case AAUDIO_CHANNEL_QUAD_SIDE:
		case AAUDIO_CHANNEL_SURROUND:
		case AAUDIO_CHANNEL_PENTA:
		case AAUDIO_CHANNEL_5POINT1:
		case AAUDIO_CHANNEL_5POINT1_SIDE:
		case AAUDIO_CHANNEL_5POINT1POINT2:
		case AAUDIO_CHANNEL_5POINT1POINT4:
		case AAUDIO_CHANNEL_6POINT1:
		case AAUDIO_CHANNEL_7POINT1:
		case AAUDIO_CHANNEL_7POINT1POINT2:
		case AAUDIO_CHANNEL_7POINT1POINT4:
		case AAUDIO_CHANNEL_9POINT1POINT4:
		case AAUDIO_CHANNEL_9POINT1POINT6:
			return AAUDIO_OK;
		default:
			return AAUDIO_ERROR_ILLEGAL_ARGUMENT;
	}
}

/* AAudioStreamParameters::validate() and AudioStreamBuilder::validate() */
static aaudio_result_t validate_builder(const AAudioStreamBuilder *b)
{
	if (b->samples_per_frame != AAUDIO_UNSPECIFIED && (b->samples_per_frame < CHANNEL_COUNT_MIN_AAUDIO || b->samples_per_frame > CHANNEL_COUNT_MAX_AAUDIO))
		return AAUDIO_ERROR_OUT_OF_RANGE;
	if (b->device_id < 0)
		return AAUDIO_ERROR_OUT_OF_RANGE;
	if (b->sharing_mode != AAUDIO_SHARING_MODE_EXCLUSIVE && b->sharing_mode != AAUDIO_SHARING_MODE_SHARED)
		return AAUDIO_ERROR_ILLEGAL_ARGUMENT;
	switch (b->format) {
		case AAUDIO_FORMAT_UNSPECIFIED:
		case AAUDIO_FORMAT_PCM_I16:
		case AAUDIO_FORMAT_PCM_FLOAT:
		case AAUDIO_FORMAT_PCM_I24_PACKED:
		case AAUDIO_FORMAT_PCM_I32:
			break;
		default:
			if (!is_compressed_format(b->format))
				return AAUDIO_ERROR_INVALID_FORMAT;
	}
	if (b->sample_rate != AAUDIO_UNSPECIFIED && (b->sample_rate < SAMPLE_RATE_HZ_MIN_AAUDIO || b->sample_rate > SAMPLE_RATE_HZ_MAX_IEC610937))
		return AAUDIO_ERROR_INVALID_RATE;
	if (b->buffer_capacity < 0)
		return AAUDIO_ERROR_OUT_OF_RANGE;
	if (b->direction != AAUDIO_DIRECTION_INPUT && b->direction != AAUDIO_DIRECTION_OUTPUT)
		return AAUDIO_ERROR_ILLEGAL_ARGUMENT;
	switch (b->usage) {
		case AAUDIO_UNSPECIFIED:
		case AAUDIO_USAGE_MEDIA:
		case AAUDIO_USAGE_VOICE_COMMUNICATION:
		case AAUDIO_USAGE_VOICE_COMMUNICATION_SIGNALLING:
		case AAUDIO_USAGE_ALARM:
		case AAUDIO_USAGE_NOTIFICATION:
		case AAUDIO_USAGE_NOTIFICATION_RINGTONE:
		case AAUDIO_USAGE_NOTIFICATION_EVENT:
		case AAUDIO_USAGE_ASSISTANCE_ACCESSIBILITY:
		case AAUDIO_USAGE_ASSISTANCE_NAVIGATION_GUIDANCE:
		case AAUDIO_USAGE_ASSISTANCE_SONIFICATION:
		case AAUDIO_USAGE_GAME:
		case AAUDIO_USAGE_ASSISTANT:
		case AAUDIO_SYSTEM_USAGE_EMERGENCY:
		case AAUDIO_SYSTEM_USAGE_SAFETY:
		case AAUDIO_SYSTEM_USAGE_VEHICLE_STATUS:
		case AAUDIO_SYSTEM_USAGE_ANNOUNCEMENT:
			break;
		default:
			return AAUDIO_ERROR_ILLEGAL_ARGUMENT;
	}
	switch (b->content_type) {
		case AAUDIO_UNSPECIFIED:
		case AAUDIO_CONTENT_TYPE_MUSIC:
		case AAUDIO_CONTENT_TYPE_MOVIE:
		case AAUDIO_CONTENT_TYPE_SONIFICATION:
		case AAUDIO_CONTENT_TYPE_SPEECH:
			break;
		default:
			return AAUDIO_ERROR_ILLEGAL_ARGUMENT;
	}
	switch (b->spatialization_behavior) {
		case AAUDIO_UNSPECIFIED:
		case AAUDIO_SPATIALIZATION_BEHAVIOR_AUTO:
		case AAUDIO_SPATIALIZATION_BEHAVIOR_NEVER:
			break;
		default:
			return AAUDIO_ERROR_ILLEGAL_ARGUMENT;
	}
	switch (b->input_preset) {
		case AAUDIO_UNSPECIFIED:
		case AAUDIO_INPUT_PRESET_GENERIC:
		case AAUDIO_INPUT_PRESET_CAMCORDER:
		case AAUDIO_INPUT_PRESET_VOICE_COMMUNICATION:
		case AAUDIO_INPUT_PRESET_VOICE_RECOGNITION:
		case AAUDIO_INPUT_PRESET_UNPROCESSED:
		case AAUDIO_INPUT_PRESET_VOICE_PERFORMANCE:
		case AAUDIO_INPUT_PRESET_SYSTEM_ECHO_REFERENCE:
		case AAUDIO_INPUT_PRESET_SYSTEM_HOTWORD:
			break;
		default:
			return AAUDIO_ERROR_ILLEGAL_ARGUMENT;
	}
	switch (b->allowed_capture_policy) {
		case AAUDIO_UNSPECIFIED:
		case AAUDIO_ALLOW_CAPTURE_BY_ALL:
		case AAUDIO_ALLOW_CAPTURE_BY_SYSTEM:
		case AAUDIO_ALLOW_CAPTURE_BY_NONE:
			break;
		default:
			return AAUDIO_ERROR_ILLEGAL_ARGUMENT;
	}
	aaudio_result_t result = validate_channel_mask(b->channel_mask, b->direction);
	if (result != AAUDIO_OK)
		return result;

	switch (b->performance_mode) {
		case AAUDIO_PERFORMANCE_MODE_NONE:
		case AAUDIO_PERFORMANCE_MODE_POWER_SAVING:
		case AAUDIO_PERFORMANCE_MODE_LOW_LATENCY:
			break;
		case AAUDIO_PERFORMANCE_MODE_POWER_SAVING_OFFLOADED:
			if (b->direction != AAUDIO_DIRECTION_OUTPUT || b->format == AAUDIO_FORMAT_UNSPECIFIED || b->sample_rate == 0 || b->channel_mask == AAUDIO_UNSPECIFIED)
				return AAUDIO_ERROR_ILLEGAL_ARGUMENT;
			break;
		default:
			return AAUDIO_ERROR_ILLEGAL_ARGUMENT;
	}
	if (b->frames_per_data_callback != AAUDIO_UNSPECIFIED && (b->frames_per_data_callback < FRAMES_PER_DATA_CALLBACK_MIN || b->frames_per_data_callback > FRAMES_PER_DATA_CALLBACK_MAX))
		return AAUDIO_ERROR_OUT_OF_RANGE;
	return AAUDIO_OK;
}

/* ---- stream state ---- */

static aaudio_stream_state_t get_state(AAudioStream *stream)
{
	return atomic_load(&stream->state);
}

/* AudioStream::setState() */
static void set_state(AAudioStream *stream, aaudio_stream_state_t state)
{
	aaudio_stream_state_t old_state = atomic_load(&stream->state);
	if (state == old_state || old_state == AAUDIO_STREAM_STATE_CLOSED)
		return;
	if (old_state == AAUDIO_STREAM_STATE_CLOSING && state != AAUDIO_STREAM_STATE_CLOSED)
		return;
	atomic_store(&stream->state, state);
	syscall(SYS_futex, &stream->state, FUTEX_WAKE_PRIVATE, INT_MAX, NULL, NULL, 0);
}

static bool is_active(AAudioStream *stream)
{
	aaudio_stream_state_t state = get_state(stream);
	return state == AAUDIO_STREAM_STATE_STARTING || state == AAUDIO_STREAM_STATE_STARTED;
}

static bool is_data_callback_active(AAudioStream *stream)
{
	return stream->data_callback && is_active(stream);
}

static bool collides_with_callback(AAudioStream *stream)
{
	pid_t tid = gettid();
	return atomic_load(&stream->data_callback_thread) == tid || atomic_load(&stream->error_callback_thread) == tid;
}

static void maybe_call_error_callback(AAudioStream *stream, aaudio_result_t result)
{
	if (!stream->error_callback)
		return;
	pid_t expected = 0;
	if (atomic_compare_exchange_strong(&stream->error_callback_thread, &expected, gettid())) {
		stream->error_callback(stream, stream->error_callback_user_data, result);
		atomic_store(&stream->error_callback_thread, 0);
	}
}

/* AudioStreamLegacy::forceDisconnect() */
static void force_disconnect(AAudioStream *stream, bool error_callback_enabled)
{
	aaudio_stream_state_t state = get_state(stream);
	if (atomic_load(&stream->disconnected) || state == AAUDIO_STREAM_STATE_CLOSING || state == AAUDIO_STREAM_STATE_CLOSED)
		return;
	atomic_store(&stream->disconnected, true);
	syscall(SYS_futex, &stream->state, FUTEX_WAKE_PRIVATE, INT_MAX, NULL, NULL, 0);
	if (error_callback_enabled)
		maybe_call_error_callback(stream, AAUDIO_ERROR_DISCONNECTED);
}

/* ---- ALSA transfer ---- */

static snd_pcm_format_t alsa_format(aaudio_format_t format, int32_t *bytes_per_sample)
{
	switch (format) {
		case AAUDIO_FORMAT_PCM_I16:
			*bytes_per_sample = 2;
			return SND_PCM_FORMAT_S16_LE;
		case AAUDIO_FORMAT_PCM_I24_PACKED:
			*bytes_per_sample = 3;
			return SND_PCM_FORMAT_S24_3LE;
		case AAUDIO_FORMAT_PCM_I32:
			*bytes_per_sample = 4;
			return SND_PCM_FORMAT_S32_LE;
		case AAUDIO_FORMAT_PCM_FLOAT:
		default:
			*bytes_per_sample = 4;
			return SND_PCM_FORMAT_FLOAT_LE;
	}
}

/* frames handed to the device but not yet played (output) or captured but not yet read (input) */
static int64_t pcm_delay(AAudioStream *stream)
{
	snd_pcm_sframes_t delay;
	if (snd_pcm_delay(stream->pcm, &delay) < 0 || delay < 0)
		return 0;
	return delay;
}

/*
 * Playback starts by hand (start_threshold is the boundary), like AudioFlinger's Track::isReady():
 * an active track is mixed once min(start threshold = frame count, buffer size) frames are ready,
 * so the device starts with a primed buffer. ALSA can't take more than fits, so a chunk that no
 * longer fits starts it as well. So does a chunk that would go beyond the buffer size: AudioTrack's
 * client fills the buffer up to its size with part of a chunk, which reaches the threshold; here
 * the callback thread waits for room for a whole chunk, which a device that is not started never makes.
 */
static void start_if_primed(AAudioStream *stream, int32_t next_frames)
{
	if (stream->direction != AAUDIO_DIRECTION_OUTPUT || !is_active(stream) || snd_pcm_state(stream->pcm) != SND_PCM_STATE_PREPARED)
		return;
	snd_pcm_sframes_t avail = snd_pcm_avail_update(stream->pcm);
	int32_t queued = stream->buffer_capacity - (avail < 0 ? 0 : avail);
	int32_t threshold = atomic_load(&stream->buffer_size);
	if (queued >= threshold || (queued > 0 && (avail < next_frames || queued + next_frames > threshold)))
		snd_pcm_start(stream->pcm);
}

/* returns the frames transferred, or AAUDIO_ERROR_DISCONNECTED when the device is gone */
static aaudio_result_t pcm_transfer(AAudioStream *stream, void *buffer, int32_t num_frames, bool blocking)
{
	bool output = stream->direction == AAUDIO_DIRECTION_OUTPUT;
	int32_t done = 0;

	snd_pcm_nonblock(stream->pcm, blocking ? 0 : 1);
	while (done < num_frames) {
		char *data = (char *)buffer + (size_t)done * stream->bytes_per_frame;
		snd_pcm_uframes_t count = num_frames - done;
		if (output && snd_pcm_state(stream->pcm) == SND_PCM_STATE_PREPARED) {
			/* a not yet started device never frees space: write only what fits, then prime */
			start_if_primed(stream, count);
			snd_pcm_sframes_t avail = snd_pcm_avail_update(stream->pcm);
			if (snd_pcm_state(stream->pcm) == SND_PCM_STATE_PREPARED && avail >= 0 && (snd_pcm_uframes_t)avail < count) {
				if (avail == 0)
					break;
				count = avail;
			}
		}
		snd_pcm_sframes_t n = output ? snd_pcm_writei(stream->pcm, data, count)
		                             : snd_pcm_readi(stream->pcm, data, count);
		if (n == -EAGAIN || n == 0)
			break;
		if (n == -EPIPE || n == -ESTRPIPE || n == -EINTR) {
			if (n == -EPIPE && output)
				atomic_fetch_add(&stream->xrun_count, 1); /* AudioRecord counts no overruns */
			if (snd_pcm_recover(stream->pcm, n, 1) < 0)
				return AAUDIO_ERROR_DISCONNECTED;
			if (!output && is_active(stream))
				snd_pcm_start(stream->pcm);
			continue;
		}
		if (n < 0)
			return AAUDIO_ERROR_DISCONNECTED;
		done += n;
	}
	if (output)
		start_if_primed(stream, stream->frames_per_burst);
	if (output)
		atomic_fetch_add(&stream->frames_written, done);
	else
		atomic_fetch_add(&stream->frames_read, done);
	return done;
}

static aaudio_result_t system_stop_internal(AAudioStream *stream);

static aaudio_data_callback_result_t call_data_callback(AAudioStream *stream, void *data, int32_t num_frames)
{
	aaudio_data_callback_result_t result = AAUDIO_CALLBACK_RESULT_STOP;
	pid_t expected = 0;
	if (atomic_compare_exchange_strong(&stream->data_callback_thread, &expected, gettid())) {
		result = stream->data_callback(stream, stream->data_callback_user_data, data, num_frames);
		atomic_store(&stream->data_callback_thread, 0);
	}
	return result;
}

/* the callback thread of AudioTrack/AudioRecord together with AudioStreamLegacy::onMoreData() */
static void *callback_thread(void *arg)
{
	AAudioStream *stream = arg;
	bool output = stream->direction == AAUDIO_DIRECTION_OUTPUT;
	int32_t num_frames = stream->frames_per_data_callback > 0 ? stream->frames_per_data_callback : stream->frames_per_burst;

	while (atomic_load(&stream->callback_enabled)) {
		if (output) {
			/* keep at most getBufferSizeInFrames() frames queued */
			snd_pcm_sframes_t avail = snd_pcm_avail_update(stream->pcm);
			if (avail < 0) {
				if (snd_pcm_recover(stream->pcm, avail, 1) < 0) {
					force_disconnect(stream, true);
					break;
				}
				if (avail == -EPIPE)
					atomic_fetch_add(&stream->xrun_count, 1);
				continue;
			}
			if (avail < stream->buffer_capacity && stream->buffer_capacity - avail + num_frames > atomic_load(&stream->buffer_size)) {
				start_if_primed(stream, num_frames);
				snd_pcm_wait(stream->pcm, 100);
				continue;
			}
		} else {
			aaudio_result_t result = pcm_transfer(stream, stream->callback_buffer, num_frames, true);
			if (result < 0) {
				force_disconnect(stream, true);
				break;
			}
			if (result < num_frames)
				continue;
		}
		aaudio_data_callback_result_t result = call_data_callback(stream, stream->callback_buffer, num_frames);
		if (result != AAUDIO_CALLBACK_RESULT_CONTINUE) {
			system_stop_internal(stream);
			atomic_store(&stream->callback_enabled, false);
			break;
		}
		if (output && pcm_transfer(stream, stream->callback_buffer, num_frames, true) < 0) {
			force_disconnect(stream, true);
			break;
		}
	}
	return NULL;
}

/* stops the callback thread; the thread itself only leaves its loop (AudioStream::joinThread_l()) */
static void stop_callback_thread_l(AAudioStream *stream)
{
	atomic_store(&stream->callback_enabled, false);
	if (stream->has_thread && !pthread_equal(pthread_self(), stream->thread)) {
		pthread_mutex_unlock(&stream->lock);
		pthread_join(stream->thread, NULL);
		pthread_mutex_lock(&stream->lock);
		stream->has_thread = false;
	}
}

/* ---- requests (AudioStreamTrack / AudioStreamRecord) ---- */

static aaudio_result_t request_start_l(AAudioStream *stream)
{
	if (!stream->pcm)
		return AAUDIO_ERROR_INVALID_STATE;
	stop_callback_thread_l(stream); /* a thread that stopped itself from its callback */

	snd_pcm_state_t pcm_state = snd_pcm_state(stream->pcm);
	if (pcm_state == SND_PCM_STATE_PAUSED) {
		snd_pcm_pause(stream->pcm, 0);
	} else if (pcm_state != SND_PCM_STATE_PREPARED && pcm_state != SND_PCM_STATE_RUNNING) {
		if (snd_pcm_prepare(stream->pcm) < 0)
			return AAUDIO_ERROR_INTERNAL;
	}

	aaudio_stream_state_t original_state = get_state(stream);
	set_state(stream, AAUDIO_STREAM_STATE_STARTING);
	if (stream->direction == AAUDIO_DIRECTION_INPUT) {
		if (snd_pcm_state(stream->pcm) == SND_PCM_STATE_PREPARED && snd_pcm_start(stream->pcm) < 0) {
			set_state(stream, original_state);
			return AAUDIO_ERROR_INTERNAL;
		}
	} else {
		start_if_primed(stream, stream->frames_per_burst); /* data written before the start */
	}
	if (stream->data_callback) {
		atomic_store(&stream->callback_enabled, true);
		if (pthread_create(&stream->thread, NULL, callback_thread, stream) != 0) {
			atomic_store(&stream->callback_enabled, false);
			snd_pcm_drop(stream->pcm);
			set_state(stream, original_state);
			return AAUDIO_ERROR_NO_FREE_HANDLES;
		}
		pthread_setname_np(stream->thread, "AAudio");
		stream->has_thread = true;
	}
	set_state(stream, AAUDIO_STREAM_STATE_STARTED);
	return AAUDIO_OK;
}

/* only for output, like AudioStreamTrack */
static aaudio_result_t request_pause_l(AAudioStream *stream)
{
	if (!stream->pcm)
		return AAUDIO_ERROR_INVALID_STATE;
	set_state(stream, AAUDIO_STREAM_STATE_PAUSING);
	stop_callback_thread_l(stream);
	if (snd_pcm_state(stream->pcm) == SND_PCM_STATE_RUNNING) {
		if (!stream->can_pause || snd_pcm_pause(stream->pcm, 1) < 0) {
			/* a device without pause loses the queued frames */
			snd_pcm_drop(stream->pcm);
			snd_pcm_prepare(stream->pcm);
			atomic_store(&stream->frames_read, atomic_load(&stream->frames_written));
		}
	}
	set_state(stream, AAUDIO_STREAM_STATE_PAUSED);
	return AAUDIO_OK;
}

/* only for output, like AudioStreamTrack */
static aaudio_result_t request_flush_l(AAudioStream *stream)
{
	if (!stream->pcm)
		return AAUDIO_ERROR_INVALID_STATE;
	set_state(stream, AAUDIO_STREAM_STATE_FLUSHING);
	snd_pcm_drop(stream->pcm);
	snd_pcm_prepare(stream->pcm);
	atomic_store(&stream->frames_read, atomic_load(&stream->frames_written));
	set_state(stream, AAUDIO_STREAM_STATE_FLUSHED);
	return AAUDIO_OK;
}

static aaudio_result_t request_stop_l(AAudioStream *stream)
{
	if (!stream->pcm)
		return AAUDIO_ERROR_INVALID_STATE;
	set_state(stream, AAUDIO_STREAM_STATE_STOPPING);
	stop_callback_thread_l(stream);
	if (stream->direction == AAUDIO_DIRECTION_OUTPUT) {
		/* the frames already written are still played; snd_pcm_drain() of ALSA's pulse plugin can hang, so wait at most one buffer */
		int64_t wait_us = (int64_t)stream->buffer_capacity * 1000000 / stream->sample_rate + 20000;
		/* Track::isReady(): a stopping track plays whatever is ready, even if the buffer never filled */
		if (snd_pcm_state(stream->pcm) == SND_PCM_STATE_PREPARED && pcm_delay(stream) > 0)
			snd_pcm_start(stream->pcm);
		while (wait_us > 0 && snd_pcm_state(stream->pcm) == SND_PCM_STATE_RUNNING && pcm_delay(stream) > 0) {
			usleep(5000);
			wait_us -= 5000;
		}
		snd_pcm_drop(stream->pcm);
		atomic_store(&stream->frames_read, atomic_load(&stream->frames_written));
	} else {
		snd_pcm_drop(stream->pcm);
		atomic_store(&stream->frames_written, atomic_load(&stream->frames_read));
	}
	snd_pcm_prepare(stream->pcm);
	set_state(stream, AAUDIO_STREAM_STATE_STOPPED);
	return AAUDIO_OK;
}

/* AudioStream::safeStop_l() */
static aaudio_result_t safe_stop_l(AAudioStream *stream)
{
	switch (get_state(stream)) {
		case AAUDIO_STREAM_STATE_STARTING:
		case AAUDIO_STREAM_STATE_STARTED:
		case AAUDIO_STREAM_STATE_DISCONNECTED:
			break;
		case AAUDIO_STREAM_STATE_OPEN:
		case AAUDIO_STREAM_STATE_PAUSED:
		case AAUDIO_STREAM_STATE_FLUSHED:
			set_state(stream, AAUDIO_STREAM_STATE_STOPPED);
			return AAUDIO_OK;
		case AAUDIO_STREAM_STATE_STOPPING:
		case AAUDIO_STREAM_STATE_STOPPED:
			return AAUDIO_OK;
		default:
			return AAUDIO_ERROR_INVALID_STATE;
	}
	return request_stop_l(stream);
}

static aaudio_result_t system_stop_internal(AAudioStream *stream)
{
	pthread_mutex_lock(&stream->lock);
	aaudio_result_t result = safe_stop_l(stream);
	pthread_mutex_unlock(&stream->lock);
	return result;
}

/* AudioStream::release_l() */
static void release_l(AAudioStream *stream)
{
	if (get_state(stream) == AAUDIO_STREAM_STATE_CLOSING)
		return;
	stop_callback_thread_l(stream);
	set_state(stream, AAUDIO_STREAM_STATE_CLOSING);
}

/* ---- open / close ---- */

static aaudio_result_t open_pcm(AAudioStream *stream, int32_t requested_capacity)
{
	snd_pcm_hw_params_t *hw;
	snd_pcm_sw_params_t *sw;
	int32_t bytes_per_sample;
	snd_pcm_format_t format = alsa_format(stream->format, &bytes_per_sample);
	unsigned int rate = stream->sample_rate;
	/* period = burst: shorter for LOW_LATENCY, longer for POWER_SAVING, like AudioFlinger's FAST and DEEP_BUFFER outputs */
	unsigned int period_us = stream->performance_mode == AAUDIO_PERFORMANCE_MODE_LOW_LATENCY ? 5000 : stream->performance_mode == AAUDIO_PERFORMANCE_MODE_POWER_SAVING ? 20000
	                                                                                                                                                             : 10000;
	snd_pcm_uframes_t period_size, buffer_size, boundary;
	int ret;

	ret = snd_pcm_open(&stream->pcm, PCM_DEVICE, stream->direction == AAUDIO_DIRECTION_INPUT ? SND_PCM_STREAM_CAPTURE : SND_PCM_STREAM_PLAYBACK, 0);
	if (ret < 0) {
		stream->pcm = NULL;
		return AAUDIO_ERROR_INTERNAL;
	}

	snd_pcm_hw_params_alloca(&hw);
	snd_pcm_hw_params_any(stream->pcm, hw);
	ret = snd_pcm_hw_params_set_access(stream->pcm, hw, SND_PCM_ACCESS_RW_INTERLEAVED);
	if (ret >= 0)
		ret = snd_pcm_hw_params_set_format(stream->pcm, hw, format);
	if (ret >= 0)
		ret = snd_pcm_hw_params_set_channels(stream->pcm, hw, stream->samples_per_frame);
	if (ret >= 0)
		ret = snd_pcm_hw_params_set_rate_near(stream->pcm, hw, &rate, NULL);
	if (ret >= 0 && requested_capacity > 0) {
		buffer_size = requested_capacity;
		ret = snd_pcm_hw_params_set_buffer_size_near(stream->pcm, hw, &buffer_size);
		if (ret >= 0)
			snd_pcm_hw_params_set_period_time_near(stream->pcm, hw, &period_us, NULL);
	} else if (ret >= 0) {
		unsigned int periods = 4;
		ret = snd_pcm_hw_params_set_period_time_near(stream->pcm, hw, &period_us, NULL);
		if (ret >= 0)
			snd_pcm_hw_params_set_periods_near(stream->pcm, hw, &periods, NULL);
	}
	if (ret >= 0)
		ret = snd_pcm_hw_params(stream->pcm, hw);
	if (ret < 0) {
		snd_pcm_close(stream->pcm);
		stream->pcm = NULL;
		return AAUDIO_ERROR_INTERNAL;
	}
	snd_pcm_hw_params_get_period_size(hw, &period_size, NULL);
	snd_pcm_hw_params_get_buffer_size(hw, &buffer_size);
	stream->can_pause = snd_pcm_hw_params_can_pause(hw);
	stream->sample_rate = rate;
	stream->bytes_per_frame = bytes_per_sample * stream->samples_per_frame;
	stream->frames_per_burst = period_size;
	stream->buffer_capacity = buffer_size;
	stream->buffer_size = buffer_size;

	snd_pcm_sw_params_alloca(&sw);
	snd_pcm_sw_params_current(stream->pcm, sw);
	snd_pcm_sw_params_get_boundary(sw, &boundary);
	snd_pcm_sw_params_set_start_threshold(stream->pcm, sw, boundary);
	snd_pcm_sw_params_set_avail_min(stream->pcm, sw, period_size);
	snd_pcm_sw_params(stream->pcm, sw);
	return AAUDIO_OK;
}

aaudio_result_t AAudioStreamBuilder_openStream(AAudioStreamBuilder *builder, AAudioStream **streamPtr)
{
	if (!streamPtr)
		return AAUDIO_ERROR_NULL;
	*streamPtr = NULL;
	aaudio_result_t result = validate_builder(builder);
	if (result != AAUDIO_OK)
		return result;
	/* AudioStreamBuilder::build(): MMAP is required, but the device has none */
	if (mmap_policy == AAUDIO_POLICY_ALWAYS)
		return AAUDIO_ERROR_NO_SERVICE;
	/* AudioStreamTrack::open(): getDirectPlaybackSupport() finds no offload output; AudioTrack forces a direct output for compressed formats */
	if (builder->performance_mode == AAUDIO_PERFORMANCE_MODE_POWER_SAVING_OFFLOADED || is_compressed_format(builder->format))
		return AAUDIO_ERROR_ILLEGAL_ARGUMENT;

	AAudioStream *stream = calloc(1, sizeof(*stream));
	if (!stream)
		return AAUDIO_ERROR_NO_MEMORY;
	pthread_mutex_init(&stream->lock, NULL);
	stream->state = AAUDIO_STREAM_STATE_UNINITIALIZED;

	/* AudioStream::open() */
	stream->direction = builder->direction;
	stream->sample_rate = builder->sample_rate ?: DEFAULT_SAMPLE_RATE;
	/* AAudio_getChannelMaskForOpen(): stereo when unspecified */
	stream->channel_mask = builder->channel_mask ?: AAUDIO_CHANNEL_STEREO;
	stream->samples_per_frame = __builtin_popcount(stream->channel_mask & ~AAUDIO_CHANNEL_BIT_INDEX);
	/* AudioStreamTrack/AudioStreamRecord::open(): float when unspecified */
	stream->format = builder->format ?: AAUDIO_FORMAT_PCM_FLOAT;
	stream->performance_mode = builder->performance_mode;
	stream->usage = builder->usage ?: AAUDIO_USAGE_MEDIA;
	stream->content_type = builder->content_type ?: AAUDIO_CONTENT_TYPE_MUSIC;
	stream->spatialization_behavior = builder->spatialization_behavior ?: AAUDIO_SPATIALIZATION_BEHAVIOR_AUTO;
	stream->is_content_spatialized = builder->is_content_spatialized;
	stream->input_preset = builder->input_preset ?: AAUDIO_INPUT_PRESET_VOICE_RECOGNITION;
	stream->allowed_capture_policy = builder->allowed_capture_policy ?: AAUDIO_ALLOW_CAPTURE_BY_ALL;
	/* AudioStreamBuilder::build() */
	if (builder->privacy_sensitive_request == PRIVACY_SENSITIVE_DEFAULT)
		stream->is_privacy_sensitive = stream->input_preset == AAUDIO_INPUT_PRESET_CAMCORDER || stream->input_preset == AAUDIO_INPUT_PRESET_VOICE_COMMUNICATION;
	else
		stream->is_privacy_sensitive = builder->privacy_sensitive_request == PRIVACY_SENSITIVE_ENABLED;
	/* the track allocates a session for AAUDIO_SESSION_ID_ALLOCATE and keeps a given one */
	if (builder->session_id == AAUDIO_SESSION_ID_NONE)
		stream->session_id = AAUDIO_SESSION_ID_NONE;
	else if (builder->session_id == AAUDIO_SESSION_ID_ALLOCATE)
		stream->session_id = atomic_fetch_add(&next_session_id, 1);
	else
		stream->session_id = builder->session_id;
	stream->frames_per_data_callback = builder->frames_per_data_callback;
	stream->data_callback = builder->data_callback;
	stream->data_callback_user_data = builder->data_callback_user_data;
	stream->error_callback = builder->error_callback;
	stream->error_callback_user_data = builder->error_callback_user_data;

	if (stream->direction == AAUDIO_DIRECTION_INPUT && !microphone_allowed())
		result = AAUDIO_ERROR_INTERNAL; /* AudioRecord fails with PERMISSION_DENIED, which AAudio reports as internal error */
	else
		result = open_pcm(stream, builder->buffer_capacity);
	if (result == AAUDIO_OK && stream->data_callback) {
		int32_t frames = stream->frames_per_data_callback > 0 ? stream->frames_per_data_callback : stream->frames_per_burst;
		stream->callback_buffer = calloc(frames, stream->bytes_per_frame);
		if (!stream->callback_buffer)
			result = AAUDIO_ERROR_NO_MEMORY;
	}
	if (result != AAUDIO_OK) {
		if (stream->pcm)
			snd_pcm_close(stream->pcm);
		pthread_mutex_destroy(&stream->lock);
		free(stream);
		return result;
	}
	set_state(stream, AAUDIO_STREAM_STATE_OPEN);
	*streamPtr = stream;
	return AAUDIO_OK;
}

aaudio_result_t AAudioStream_release(AAudioStream *stream)
{
	if (!stream)
		return AAUDIO_ERROR_NULL;
	if (collides_with_callback(stream))
		return AAUDIO_ERROR_INVALID_STATE;
	pthread_mutex_lock(&stream->lock);
	release_l(stream);
	pthread_mutex_unlock(&stream->lock);
	return AAUDIO_OK;
}

aaudio_result_t AAudioStream_close(AAudioStream *stream)
{
	if (!stream)
		return AAUDIO_ERROR_NULL;
	if (collides_with_callback(stream))
		return AAUDIO_ERROR_INVALID_STATE;
	pthread_mutex_lock(&stream->lock);
	release_l(stream);
	stop_callback_thread_l(stream);
	snd_pcm_close(stream->pcm);
	stream->pcm = NULL;
	set_state(stream, AAUDIO_STREAM_STATE_CLOSED);
	pthread_mutex_unlock(&stream->lock);
	pthread_mutex_destroy(&stream->lock);
	free(stream->callback_buffer);
	free(stream);
	return AAUDIO_OK;
}

/* ---- control (AudioStream::systemStart() etc.) ---- */

aaudio_result_t AAudioStream_requestStart(AAudioStream *stream)
{
	if (collides_with_callback(stream))
		return AAUDIO_ERROR_INVALID_STATE;
	pthread_mutex_lock(&stream->lock);
	aaudio_result_t result;
	if (atomic_load(&stream->disconnected)) {
		result = AAUDIO_ERROR_INVALID_STATE;
	} else {
		switch (get_state(stream)) {
			case AAUDIO_STREAM_STATE_OPEN:
			case AAUDIO_STREAM_STATE_PAUSING:
			case AAUDIO_STREAM_STATE_PAUSED:
			case AAUDIO_STREAM_STATE_STOPPING:
			case AAUDIO_STREAM_STATE_STOPPED:
			case AAUDIO_STREAM_STATE_FLUSHING:
			case AAUDIO_STREAM_STATE_FLUSHED:
				result = request_start_l(stream);
				break;
			case AAUDIO_STREAM_STATE_DISCONNECTED:
				result = AAUDIO_ERROR_INTERNAL;
				break;
			default:
				result = AAUDIO_ERROR_INVALID_STATE;
				break;
		}
	}
	pthread_mutex_unlock(&stream->lock);
	return result;
}

aaudio_result_t AAudioStream_requestPause(AAudioStream *stream)
{
	if (stream->direction != AAUDIO_DIRECTION_OUTPUT)
		return AAUDIO_ERROR_UNIMPLEMENTED;
	if (collides_with_callback(stream))
		return AAUDIO_ERROR_INVALID_STATE;
	pthread_mutex_lock(&stream->lock);
	aaudio_result_t result;
	switch (get_state(stream)) {
		case AAUDIO_STREAM_STATE_STARTING:
		case AAUDIO_STREAM_STATE_STARTED:
		case AAUDIO_STREAM_STATE_DISCONNECTED:
			result = request_pause_l(stream);
			break;
		case AAUDIO_STREAM_STATE_OPEN:
		case AAUDIO_STREAM_STATE_STOPPED:
		case AAUDIO_STREAM_STATE_FLUSHED:
			set_state(stream, AAUDIO_STREAM_STATE_PAUSED);
			result = AAUDIO_OK;
			break;
		case AAUDIO_STREAM_STATE_PAUSING:
		case AAUDIO_STREAM_STATE_PAUSED:
			result = AAUDIO_OK;
			break;
		default:
			result = AAUDIO_ERROR_INVALID_STATE;
			break;
	}
	pthread_mutex_unlock(&stream->lock);
	return result;
}

aaudio_result_t AAudioStream_requestFlush(AAudioStream *stream)
{
	if (stream->direction != AAUDIO_DIRECTION_OUTPUT)
		return AAUDIO_ERROR_UNIMPLEMENTED;
	if (collides_with_callback(stream))
		return AAUDIO_ERROR_INVALID_STATE;
	pthread_mutex_lock(&stream->lock);
	aaudio_result_t result;
	switch (get_state(stream)) { /* AAudio_isFlushAllowed() */
		case AAUDIO_STREAM_STATE_OPEN:
		case AAUDIO_STREAM_STATE_PAUSED:
		case AAUDIO_STREAM_STATE_STOPPED:
		case AAUDIO_STREAM_STATE_FLUSHED:
			result = request_flush_l(stream);
			break;
		default:
			result = AAUDIO_ERROR_INVALID_STATE;
			break;
	}
	pthread_mutex_unlock(&stream->lock);
	return result;
}

aaudio_result_t AAudioStream_requestStop(AAudioStream *stream)
{
	if (collides_with_callback(stream))
		return AAUDIO_ERROR_INVALID_STATE;
	return system_stop_internal(stream);
}

aaudio_stream_state_t AAudioStream_getState(AAudioStream *stream)
{
	return atomic_load(&stream->disconnected) ? AAUDIO_STREAM_STATE_DISCONNECTED : get_state(stream);
}

/* AudioStream::waitForStateChange(); the requests above finish synchronously, so there is no state machine to advance */
aaudio_result_t AAudioStream_waitForStateChange(AAudioStream *stream, aaudio_stream_state_t inputState, aaudio_stream_state_t *nextState, int64_t timeoutNanoseconds)
{
	int64_t duration = 20 * AAUDIO_NANOS_PER_MILLISECOND;
	aaudio_stream_state_t state = AAudioStream_getState(stream);
	while (state == inputState && timeoutNanoseconds > 0) {
		if (duration > timeoutNanoseconds)
			duration = timeoutNanoseconds;
		struct timespec time = {
			.tv_sec = duration / AAUDIO_NANOS_PER_SECOND,
			.tv_nsec = duration % AAUDIO_NANOS_PER_SECOND,
		};
		syscall(SYS_futex, &stream->state, FUTEX_WAIT_PRIVATE, inputState, &time, NULL, 0);
		timeoutNanoseconds -= duration;
		state = AAudioStream_getState(stream);
	}
	if (nextState)
		*nextState = state;
	return state == inputState ? AAUDIO_ERROR_TIMEOUT : AAUDIO_OK;
}

/* ---- blocking transfer (AudioStreamTrack::write(), AudioStreamRecord::read()) ---- */

static aaudio_result_t transfer(AAudioStream *stream, void *buffer, int32_t numFrames, int64_t timeoutNanoseconds)
{
	if (atomic_load(&stream->disconnected))
		return AAUDIO_ERROR_DISCONNECTED;
	if (!stream->pcm)
		return AAUDIO_ERROR_INVALID_STATE;
	/* blocking waits for the device, which only moves while the stream runs */
	aaudio_result_t result = pcm_transfer(stream, buffer, numFrames, timeoutNanoseconds > 0 && is_active(stream));
	if (result < 0)
		force_disconnect(stream, false);
	return result;
}

aaudio_result_t AAudioStream_read(AAudioStream *stream, void *buffer, int32_t numFrames, int64_t timeoutNanoseconds)
{
	if (!buffer)
		return AAUDIO_ERROR_NULL;
	if (numFrames < 0)
		return AAUDIO_ERROR_ILLEGAL_ARGUMENT;
	if (numFrames == 0)
		return 0;
	if (stream->direction != AAUDIO_DIRECTION_INPUT)
		return AAUDIO_ERROR_UNIMPLEMENTED; /* AudioStream::read() of an output stream */
	return transfer(stream, buffer, numFrames, timeoutNanoseconds);
}

aaudio_result_t AAudioStream_write(AAudioStream *stream, const void *buffer, int32_t numFrames, int64_t timeoutNanoseconds)
{
	if (!buffer)
		return AAUDIO_ERROR_NULL;
	if (is_data_callback_active(stream))
		return AAUDIO_ERROR_INVALID_STATE;
	if (numFrames < 0)
		return AAUDIO_ERROR_ILLEGAL_ARGUMENT;
	if (numFrames == 0)
		return 0;
	if (stream->direction != AAUDIO_DIRECTION_OUTPUT)
		return AAUDIO_ERROR_UNIMPLEMENTED; /* AudioStream::write() of an input stream */
	return transfer(stream, (void *)buffer, numFrames, timeoutNanoseconds);
}

/* ---- buffer ---- */

aaudio_result_t AAudioStream_setBufferSizeInFrames(AAudioStream *stream, int32_t numFrames)
{
	if (stream->direction == AAUDIO_DIRECTION_INPUT)
		return stream->buffer_capacity; /* AudioStreamRecord: fixed */
	/* AudioTrack::setBufferSizeInFrames() keeps it between one burst and the capacity */
	if (numFrames < stream->frames_per_burst)
		numFrames = stream->frames_per_burst;
	if (numFrames > stream->buffer_capacity)
		numFrames = stream->buffer_capacity;
	atomic_store(&stream->buffer_size, numFrames);
	return numFrames;
}

int32_t AAudioStream_getBufferSizeInFrames(AAudioStream *stream)
{
	return atomic_load(&stream->buffer_size);
}

int32_t AAudioStream_getFramesPerBurst(AAudioStream *stream)
{
	return stream->frames_per_burst;
}

int32_t AAudioStream_getBufferCapacityInFrames(AAudioStream *stream)
{
	return stream->buffer_capacity;
}

int32_t AAudioStream_getFramesPerDataCallback(AAudioStream *stream)
{
	return stream->frames_per_data_callback;
}

int32_t AAudioStream_getXRunCount(AAudioStream *stream)
{
	return atomic_load(&stream->xrun_count);
}

/* ---- stream properties ---- */

int32_t AAudioStream_getSampleRate(AAudioStream *stream)
{
	return stream->sample_rate;
}

/* ALSA's "default" device is the sound server, which resamples and mixes: what reaches the hardware is not known */
int32_t AAudioStream_getHardwareSampleRate(AAudioStream *stream)
{
	return stream->sample_rate;
}

int32_t AAudioStream_getChannelCount(AAudioStream *stream)
{
	return stream->samples_per_frame;
}

int32_t AAudioStream_getHardwareChannelCount(AAudioStream *stream)
{
	return stream->samples_per_frame;
}

int32_t AAudioStream_getSamplesPerFrame(AAudioStream *stream)
{
	return AAudioStream_getChannelCount(stream);
}

aaudio_format_t AAudioStream_getFormat(AAudioStream *stream)
{
	return stream->format;
}

aaudio_format_t AAudioStream_getHardwareFormat(AAudioStream *stream)
{
	return stream->format;
}

aaudio_channel_mask_t AAudioStream_getChannelMask(AAudioStream *stream)
{
	return (stream->channel_mask & AAUDIO_CHANNEL_BIT_INDEX) ? AAUDIO_UNSPECIFIED : stream->channel_mask;
}

aaudio_direction_t AAudioStream_getDirection(AAudioStream *stream)
{
	return stream->direction;
}

aaudio_sharing_mode_t AAudioStream_getSharingMode(AAudioStream *stream)
{
	return AAUDIO_SHARING_MODE_SHARED; /* EXCLUSIVE mode not supported in legacy */
}

aaudio_performance_mode_t AAudioStream_getPerformanceMode(AAudioStream *stream)
{
	return stream->performance_mode;
}

/* no routed devices to report */
int32_t AAudioStream_getDeviceId(AAudioStream *stream)
{
	return AAUDIO_UNSPECIFIED;
}

aaudio_result_t AAudioStream_getDeviceIds(AAudioStream *stream, int32_t *ids, int32_t *numIds)
{
	if (!numIds || !ids)
		return AAUDIO_ERROR_ILLEGAL_ARGUMENT;
	*numIds = 0;
	return AAUDIO_OK;
}

aaudio_usage_t AAudioStream_getUsage(AAudioStream *stream)
{
	return stream->usage;
}

aaudio_content_type_t AAudioStream_getContentType(AAudioStream *stream)
{
	return stream->content_type;
}

aaudio_spatialization_behavior_t AAudioStream_getSpatializationBehavior(AAudioStream *stream)
{
	return stream->spatialization_behavior;
}

bool AAudioStream_isContentSpatialized(AAudioStream *stream)
{
	return stream->is_content_spatialized;
}

aaudio_input_preset_t AAudioStream_getInputPreset(AAudioStream *stream)
{
	return stream->input_preset;
}

aaudio_allowed_capture_policy_t AAudioStream_getAllowedCapturePolicy(AAudioStream *stream)
{
	return stream->allowed_capture_policy;
}

aaudio_session_id_t AAudioStream_getSessionId(AAudioStream *stream)
{
	return stream->session_id;
}

bool AAudioStream_isMMapUsed(AAudioStream *stream)
{
	return false;
}

bool AAudioStream_isPrivacySensitive(AAudioStream *stream)
{
	return stream->is_privacy_sensitive;
}

/* ---- position ---- */

static bool position_moves(AAudioStream *stream)
{
	switch (get_state(stream)) {
		case AAUDIO_STREAM_STATE_STARTING:
		case AAUDIO_STREAM_STATE_STARTED:
		case AAUDIO_STREAM_STATE_STOPPING:
		case AAUDIO_STREAM_STATE_PAUSING:
		case AAUDIO_STREAM_STATE_PAUSED:
			return stream->pcm != NULL;
		default:
			return false;
	}
}

int64_t AAudioStream_getFramesWritten(AAudioStream *stream)
{
	if (stream->direction == AAUDIO_DIRECTION_INPUT && position_moves(stream)) {
		int64_t position = atomic_load(&stream->frames_read) + pcm_delay(stream);
		if (position > atomic_load(&stream->frames_written))
			atomic_store(&stream->frames_written, position);
	}
	return atomic_load(&stream->frames_written);
}

int64_t AAudioStream_getFramesRead(AAudioStream *stream)
{
	if (stream->direction == AAUDIO_DIRECTION_OUTPUT && position_moves(stream)) {
		int64_t position = atomic_load(&stream->frames_written) - pcm_delay(stream);
		if (position > atomic_load(&stream->frames_read))
			atomic_store(&stream->frames_read, position);
	}
	return atomic_load(&stream->frames_read);
}

aaudio_result_t AAudioStream_getTimestamp(AAudioStream *stream, clockid_t clockid, int64_t *framePosition, int64_t *timeNanoseconds)
{
	if (!framePosition || !timeNanoseconds)
		return AAUDIO_ERROR_NULL;
	if (clockid != CLOCK_MONOTONIC && clockid != CLOCK_BOOTTIME)
		return AAUDIO_ERROR_ILLEGAL_ARGUMENT;
	/* AudioTrack/AudioRecord have no timestamp before the device runs (WOULD_BLOCK or INVALID_OPERATION) */
	if (get_state(stream) != AAUDIO_STREAM_STATE_STARTED || !stream->pcm || snd_pcm_state(stream->pcm) != SND_PCM_STATE_RUNNING)
		return AAUDIO_ERROR_INVALID_STATE;
	struct timespec now;
	int64_t delay = pcm_delay(stream);
	clock_gettime(clockid, &now);
	if (stream->direction == AAUDIO_DIRECTION_OUTPUT)
		*framePosition = atomic_load(&stream->frames_written) - delay;
	else
		*framePosition = atomic_load(&stream->frames_read) + delay;
	*timeNanoseconds = now.tv_sec * AAUDIO_NANOS_PER_SECOND + now.tv_nsec;
	return AAUDIO_OK;
}

/* ---- offload: only for POWER_SAVING_OFFLOADED streams, which can't be opened (AudioStreamTrack) ---- */

aaudio_result_t AAudioStream_setOffloadDelayPadding(AAudioStream *stream, int32_t delayInFrames, int32_t paddingInFrames)
{
	if (delayInFrames < 0 || paddingInFrames < 0)
		return AAUDIO_ERROR_ILLEGAL_ARGUMENT;
	return AAUDIO_ERROR_UNIMPLEMENTED;
}

int32_t AAudioStream_getOffloadDelay(AAudioStream *stream)
{
	return AAUDIO_ERROR_UNIMPLEMENTED;
}

int32_t AAudioStream_getOffloadPadding(AAudioStream *stream)
{
	return AAUDIO_ERROR_UNIMPLEMENTED;
}

aaudio_result_t AAudioStream_setOffloadEndOfStream(AAudioStream *stream)
{
	return AAUDIO_ERROR_UNIMPLEMENTED;
}
