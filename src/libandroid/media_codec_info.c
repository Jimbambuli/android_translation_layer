/*
 * NdkMediaCodecInfo and NdkMediaCodecStore (libmediandk, API 36) on ATL's android.media.MediaCodecList and
 * MediaCodecInfo. AOSP builds one AMediaCodecInfo per (codec, media type) of its native MediaCodecList on first use
 * (frameworks/av/media/ndk/NdkMediaCodecStore.cpp) and keeps them, their names and the list of media types for the
 * life of the process: nothing is ever freed, the pointers handed out stay valid. Here the list is the Java one, the
 * capabilities are read from the Java classes with AOSP's NDK results (optional empty -> AMEDIA_ERROR_UNSUPPORTED,
 * NULL object -> INVALID_PARAMETER or -1).
 */

#include <stdarg.h>
#include <stdlib.h>
#include <string.h>

#include <glib.h>

#include "media_ndk.h"
#include "ndk_jni.h"

typedef struct AIntRange {
	int32_t mLower;
	int32_t mUpper;
} AIntRange;

typedef struct ADoubleRange {
	double mLower;
	double mUpper;
} ADoubleRange;

typedef enum AMediaCodecKind {
	AMediaCodecKind_INVALID = 0,
	AMediaCodecKind_DECODER = 1,
	AMediaCodecKind_ENCODER = 2,
} AMediaCodecKind;

typedef enum AMediaCodecType {
	AMediaCodecType_INVALID_CODEC_INFO = 0,
	AMediaCodecType_SOFTWARE_ONLY = 1,
	AMediaCodecType_HARDWARE_ACCELERATED = 2,
	AMediaCodecType_SOFTWARE_WITH_DEVICE_ACCESS = 3,
} AMediaCodecType;

typedef enum ABitrateMode {
	ABITRATE_MODE_CQ = 0,
	ABITRATE_MODE_VBR = 1,
	ABITRATE_MODE_CBR = 2,
	ABITRATE_MODE_CBR_FD = 3
} ABitrateMode;

/* AMediaCodecSupportedMediaType::Mode, a C++ enum inside the struct */
enum { FLAG_DECODER = 1 << 0, FLAG_ENCODER = 1 << 1 };

typedef struct AMediaCodecSupportedMediaType {
	const char *mMediaType;
	uint32_t mMode;
} AMediaCodecSupportedMediaType;

typedef struct ACodecAudioCapabilities {
	jobject caps; /* android.media.MediaCodecInfo.AudioCapabilities */
	int *sample_rates;
	size_t n_sample_rates;
	AIntRange *sample_rate_ranges;
	size_t n_sample_rate_ranges;
	AIntRange *channel_ranges;
	size_t n_channel_ranges;
} ACodecAudioCapabilities;

typedef struct ACodecPerformancePoint {
	jobject point; /* android.media.MediaCodecInfo.VideoCapabilities.PerformancePoint */
	int32_t width, height, frame_rate;
} ACodecPerformancePoint;

/* the Java VideoCapabilities.getSupportedPerformancePoints() of ATL is always empty: no point to list */
typedef struct ACodecVideoCapabilities {
	jobject caps; /* android.media.MediaCodecInfo.VideoCapabilities */
} ACodecVideoCapabilities;

typedef struct ACodecEncoderCapabilities {
	jobject caps; /* android.media.MediaCodecInfo.EncoderCapabilities */
} ACodecEncoderCapabilities;

typedef struct AMediaCodecInfo {
	char *name; /* the name of the store, a media type suffix and ".N" make it unique */
	char *canonical_name;
	char *media_type;
	bool encoder, vendor;
	AMediaCodecType type;
	jobject caps; /* android.media.MediaCodecInfo.CodecCapabilities */
	ACodecAudioCapabilities *audio;
	ACodecVideoCapabilities *video;
	ACodecEncoderCapabilities *encoder_caps;
} AMediaCodecInfo;

static struct {
	jclass list_class, info_class, caps_class, audio_class, video_class, encoder_class, point_class, number_class, range_class;
	jmethodID get_count, get_at;
	jmethodID get_name, is_encoder, is_vendor, is_hardware, is_software, get_types, caps_for_type;
	jmethodID feature_supported, feature_required, max_instances, format_supported, get_audio, get_video, get_encoder;
	jmethodID a_bitrate, a_rates, a_rate_ranges, a_max_channels, a_min_channels, a_channel_ranges, a_rate_supported;
	jmethodID v_bitrate, v_widths, v_heights, v_width_align, v_height_align, v_frame_rates, v_widths_for, v_heights_for;
	jmethodID v_frame_rates_for, v_achievable, v_size_rate, v_size;
	jmethodID e_quality, e_complexity, e_bitrate_mode;
	jmethodID p_ctor, p_covers_format, p_covers;
	jmethodID int_value, double_value, get_lower, get_upper;
} m;

static JNIEnv *env_init(void)
{
	JNIEnv *env = ndk_jni_env();
	if (!env || m.list_class)
		return env;
	jclass list = ndk_jni_class(env, "android/media/MediaCodecList");
	m.info_class = ndk_jni_class(env, "android/media/MediaCodecInfo");
	m.caps_class = ndk_jni_class(env, "android/media/MediaCodecInfo$CodecCapabilities");
	m.audio_class = ndk_jni_class(env, "android/media/MediaCodecInfo$AudioCapabilities");
	m.video_class = ndk_jni_class(env, "android/media/MediaCodecInfo$VideoCapabilities");
	m.encoder_class = ndk_jni_class(env, "android/media/MediaCodecInfo$EncoderCapabilities");
	m.point_class = ndk_jni_class(env, "android/media/MediaCodecInfo$VideoCapabilities$PerformancePoint");
	m.number_class = ndk_jni_class(env, "java/lang/Number");
	m.range_class = ndk_jni_class(env, "android/util/Range");
	m.get_count = (*env)->GetStaticMethodID(env, list, "getCodecCount", "()I");
	m.get_at = (*env)->GetStaticMethodID(env, list, "getCodecInfoAt", "(I)Landroid/media/MediaCodecInfo;");
	m.get_name = (*env)->GetMethodID(env, m.info_class, "getName", "()Ljava/lang/String;");
	m.is_encoder = (*env)->GetMethodID(env, m.info_class, "isEncoder", "()Z");
	m.is_vendor = (*env)->GetMethodID(env, m.info_class, "isVendor", "()Z");
	m.is_hardware = (*env)->GetMethodID(env, m.info_class, "isHardwareAccelerated", "()Z");
	m.is_software = (*env)->GetMethodID(env, m.info_class, "isSoftwareOnly", "()Z");
	m.get_types = (*env)->GetMethodID(env, m.info_class, "getSupportedTypes", "()[Ljava/lang/String;");
	m.caps_for_type = (*env)->GetMethodID(env, m.info_class, "getCapabilitiesForType", "(Ljava/lang/String;)Landroid/media/MediaCodecInfo$CodecCapabilities;");
	m.feature_supported = (*env)->GetMethodID(env, m.caps_class, "isFeatureSupported", "(Ljava/lang/String;)Z");
	m.feature_required = (*env)->GetMethodID(env, m.caps_class, "isFeatureRequired", "(Ljava/lang/String;)Z");
	m.max_instances = (*env)->GetMethodID(env, m.caps_class, "getMaxSupportedInstances", "()I");
	m.format_supported = (*env)->GetMethodID(env, m.caps_class, "isFormatSupported", "(Landroid/media/MediaFormat;)Z");
	m.get_audio = (*env)->GetMethodID(env, m.caps_class, "getAudioCapabilities", "()Landroid/media/MediaCodecInfo$AudioCapabilities;");
	m.get_video = (*env)->GetMethodID(env, m.caps_class, "getVideoCapabilities", "()Landroid/media/MediaCodecInfo$VideoCapabilities;");
	m.get_encoder = (*env)->GetMethodID(env, m.caps_class, "getEncoderCapabilities", "()Landroid/media/MediaCodecInfo$EncoderCapabilities;");
	m.a_bitrate = (*env)->GetMethodID(env, m.audio_class, "getBitrateRange", "()Landroid/util/Range;");
	m.a_rates = (*env)->GetMethodID(env, m.audio_class, "getSupportedSampleRates", "()[I");
	m.a_rate_ranges = (*env)->GetMethodID(env, m.audio_class, "getSupportedSampleRateRanges", "()[Landroid/util/Range;");
	m.a_max_channels = (*env)->GetMethodID(env, m.audio_class, "getMaxInputChannelCount", "()I");
	m.a_min_channels = (*env)->GetMethodID(env, m.audio_class, "getMinInputChannelCount", "()I");
	m.a_channel_ranges = (*env)->GetMethodID(env, m.audio_class, "getInputChannelCountRanges", "()[Landroid/util/Range;");
	m.a_rate_supported = (*env)->GetMethodID(env, m.audio_class, "isSampleRateSupported", "(I)Z");
	m.v_bitrate = (*env)->GetMethodID(env, m.video_class, "getBitrateRange", "()Landroid/util/Range;");
	m.v_widths = (*env)->GetMethodID(env, m.video_class, "getSupportedWidths", "()Landroid/util/Range;");
	m.v_heights = (*env)->GetMethodID(env, m.video_class, "getSupportedHeights", "()Landroid/util/Range;");
	m.v_width_align = (*env)->GetMethodID(env, m.video_class, "getWidthAlignment", "()I");
	m.v_height_align = (*env)->GetMethodID(env, m.video_class, "getHeightAlignment", "()I");
	m.v_frame_rates = (*env)->GetMethodID(env, m.video_class, "getSupportedFrameRates", "()Landroid/util/Range;");
	m.v_widths_for = (*env)->GetMethodID(env, m.video_class, "getSupportedWidthsFor", "(I)Landroid/util/Range;");
	m.v_heights_for = (*env)->GetMethodID(env, m.video_class, "getSupportedHeightsFor", "(I)Landroid/util/Range;");
	m.v_frame_rates_for = (*env)->GetMethodID(env, m.video_class, "getSupportedFrameRatesFor", "(II)Landroid/util/Range;");
	m.v_achievable = (*env)->GetMethodID(env, m.video_class, "getAchievableFrameRatesFor", "(II)Landroid/util/Range;");
	m.v_size_rate = (*env)->GetMethodID(env, m.video_class, "areSizeAndRateSupported", "(IID)Z");
	m.v_size = (*env)->GetMethodID(env, m.video_class, "isSizeSupported", "(II)Z");
	m.e_quality = (*env)->GetMethodID(env, m.encoder_class, "getQualityRange", "()Landroid/util/Range;");
	m.e_complexity = (*env)->GetMethodID(env, m.encoder_class, "getComplexityRange", "()Landroid/util/Range;");
	m.e_bitrate_mode = (*env)->GetMethodID(env, m.encoder_class, "isBitrateModeSupported", "(I)Z");
	m.p_ctor = (*env)->GetMethodID(env, m.point_class, "<init>", "(III)V");
	m.p_covers_format = (*env)->GetMethodID(env, m.point_class, "covers", "(Landroid/media/MediaFormat;)Z");
	m.p_covers = (*env)->GetMethodID(env, m.point_class, "covers", "(Landroid/media/MediaCodecInfo$VideoCapabilities$PerformancePoint;)Z");
	m.int_value = (*env)->GetMethodID(env, m.number_class, "intValue", "()I");
	m.double_value = (*env)->GetMethodID(env, m.number_class, "doubleValue", "()D");
	m.get_lower = (*env)->GetMethodID(env, m.range_class, "getLower", "()Ljava/lang/Comparable;");
	m.get_upper = (*env)->GetMethodID(env, m.range_class, "getUpper", "()Ljava/lang/Comparable;");
	m.list_class = list;
	return env;
}

bool AMediaFormat_getString(AMediaFormat *format, const char *name, const char **out);

static char *utf(JNIEnv *env, jstring s)
{
	const char *chars = (*env)->GetStringUTFChars(env, s, NULL);
	char *copy = g_strdup(chars);
	(*env)->ReleaseStringUTFChars(env, s, chars);
	return copy;
}

/* the bounds of a Java Range of Integers or Doubles */
static void range_bounds(JNIEnv *env, jobject range, bool is_double, double *lower, double *upper)
{
	jobject l = (*env)->CallObjectMethod(env, range, m.get_lower);
	jobject u = (*env)->CallObjectMethod(env, range, m.get_upper);
	*lower = is_double ? (*env)->CallDoubleMethod(env, l, m.double_value) : (*env)->CallIntMethod(env, l, m.int_value);
	*upper = is_double ? (*env)->CallDoubleMethod(env, u, m.double_value) : (*env)->CallIntMethod(env, u, m.int_value);
	(*env)->DeleteLocalRef(env, l);
	(*env)->DeleteLocalRef(env, u);
}

/* AOSP's optional<Range>: a Java exception (IllegalArgumentException of an unsupported size) or null is "no value",
 * AMEDIA_ERROR_UNSUPPORTED; the range method is called with the int arguments that follow */
static media_status_t get_range(jobject obj, jmethodID method, bool is_double, void *out, ...)
{
	JNIEnv *env = env_init();
	va_list args;
	va_start(args, out);
	jobject range = (*env)->CallObjectMethodV(env, obj, method, args);
	va_end(args);
	if (ndk_jni_threw_class(env, "java/lang/IllegalArgumentException", "range") || !range)
		return AMEDIA_ERROR_UNSUPPORTED;
	double lower, upper;
	range_bounds(env, range, is_double, &lower, &upper);
	(*env)->DeleteLocalRef(env, range);
	if (is_double) {
		((ADoubleRange *)out)->mLower = lower;
		((ADoubleRange *)out)->mUpper = upper;
	} else {
		((AIntRange *)out)->mLower = lower;
		((AIntRange *)out)->mUpper = upper;
	}
	return AMEDIA_OK;
}

static AIntRange *int_ranges(JNIEnv *env, jobjectArray array, size_t *count)
{
	jsize n = array ? (*env)->GetArrayLength(env, array) : 0;
	AIntRange *ranges = g_new0(AIntRange, n);
	for (jsize i = 0; i < n; i++) {
		jobject range = (*env)->GetObjectArrayElement(env, array, i);
		double lower, upper;
		range_bounds(env, range, false, &lower, &upper);
		ranges[i] = (AIntRange){lower, upper};
		(*env)->DeleteLocalRef(env, range);
	}
	*count = n;
	return ranges;
}

static ACodecAudioCapabilities *audio_caps_new(JNIEnv *env, jobject caps)
{
	ACodecAudioCapabilities *audio = g_new0(ACodecAudioCapabilities, 1);
	audio->caps = (*env)->NewGlobalRef(env, caps);
	jintArray rates = (*env)->CallObjectMethod(env, caps, m.a_rates);
	if (rates) { /* null: the rates are given as ranges only */
		audio->n_sample_rates = (*env)->GetArrayLength(env, rates);
		audio->sample_rates = g_new(int, audio->n_sample_rates);
		(*env)->GetIntArrayRegion(env, rates, 0, audio->n_sample_rates, audio->sample_rates);
		(*env)->DeleteLocalRef(env, rates);
	}
	jobjectArray rate_ranges = (*env)->CallObjectMethod(env, caps, m.a_rate_ranges);
	audio->sample_rate_ranges = int_ranges(env, rate_ranges, &audio->n_sample_rate_ranges);
	jobjectArray channel_ranges = (*env)->CallObjectMethod(env, caps, m.a_channel_ranges);
	audio->channel_ranges = int_ranges(env, channel_ranges, &audio->n_channel_ranges);
	return audio;
}

static void *caps_holder(JNIEnv *env, jobject caps, size_t size)
{
	jobject *holder = g_malloc0(size);
	*holder = (*env)->NewGlobalRef(env, caps); /* the Java object is the first member of both holders */
	return holder;
}

static GMutex store_lock;
static bool store_ready;
static GPtrArray *infos; /* AMediaCodecInfo *, in the order of the Java list */
static GHashTable *name_to_info;
static AMediaCodecSupportedMediaType *media_types;
static size_t n_media_types;

static void add_media_type(const char *type, bool encoder)
{
	for (size_t i = 0; i < n_media_types; i++) {
		if (!strcmp(media_types[i].mMediaType, type)) {
			media_types[i].mMode |= encoder ? FLAG_ENCODER : FLAG_DECODER;
			return;
		}
	}
	media_types = g_renew(AMediaCodecSupportedMediaType, media_types, n_media_types + 1);
	media_types[n_media_types++] = (AMediaCodecSupportedMediaType){g_strdup(type), encoder ? FLAG_ENCODER : FLAG_DECODER};
}

/* AMediaCodecInfo of one media type of a codec: AOSP's AMediaCodecInfo constructor */
static AMediaCodecInfo *info_new(JNIEnv *env, jobject jinfo, const char *ndk_name, const char *type, jobject caps)
{
	AMediaCodecInfo *info = g_new0(AMediaCodecInfo, 1);
	info->name = g_strdup(ndk_name);
	jstring jname = (*env)->CallObjectMethod(env, jinfo, m.get_name);
	info->canonical_name = utf(env, jname);
	(*env)->DeleteLocalRef(env, jname);
	info->media_type = g_strdup(type);
	info->encoder = (*env)->CallBooleanMethod(env, jinfo, m.is_encoder);
	info->vendor = (*env)->CallBooleanMethod(env, jinfo, m.is_vendor);
	/* AMediaCodecInfo_getMediaCodecInfoType(): software only, else hardware accelerated, else software with device access */
	info->type = (*env)->CallBooleanMethod(env, jinfo, m.is_software) ? AMediaCodecType_SOFTWARE_ONLY :
	             (*env)->CallBooleanMethod(env, jinfo, m.is_hardware) ? AMediaCodecType_HARDWARE_ACCELERATED :
	                                                                    AMediaCodecType_SOFTWARE_WITH_DEVICE_ACCESS;
	info->caps = (*env)->NewGlobalRef(env, caps);
	/* ATL's Java CodecCapabilities hands out audio and video capabilities for every type; AOSP's only for the matching one */
	jobject sub;
	if (g_str_has_prefix(type, "audio/") && (sub = (*env)->CallObjectMethod(env, caps, m.get_audio))) {
		info->audio = audio_caps_new(env, sub);
		(*env)->DeleteLocalRef(env, sub);
	}
	if (g_str_has_prefix(type, "video/") && (sub = (*env)->CallObjectMethod(env, caps, m.get_video))) {
		info->video = caps_holder(env, sub, sizeof(ACodecVideoCapabilities));
		(*env)->DeleteLocalRef(env, sub);
	}
	if ((sub = (*env)->CallObjectMethod(env, caps, m.get_encoder))) {
		info->encoder_caps = caps_holder(env, sub, sizeof(ACodecEncoderCapabilities));
		(*env)->DeleteLocalRef(env, sub);
	}
	return info;
}

/* initMediaTypes() and initCodecInfoMap() of AOSP in one pass over the Java list. AOSP skips codecs with
 * FEATURE_SpecialCodec; ATL has none. */
static JNIEnv *store_init(void)
{
	JNIEnv *env = env_init();
	g_mutex_lock(&store_lock);
	if (!store_ready) {
		infos = g_ptr_array_new();
		name_to_info = g_hash_table_new(g_str_hash, g_str_equal);
		jint count = (*env)->CallStaticIntMethod(env, m.list_class, m.get_count);
		for (jint idx = 0; idx < count; idx++) {
			jobject jinfo = (*env)->CallStaticObjectMethod(env, m.list_class, m.get_at, idx);
			if (ndk_jni_threw(env, "MediaCodecList.getCodecInfoAt") || !jinfo)
				continue;
			jstring jname = (*env)->CallObjectMethod(env, jinfo, m.get_name);
			char *codec_name = utf(env, jname);
			(*env)->DeleteLocalRef(env, jname);
			jobjectArray jtypes = (*env)->CallObjectMethod(env, jinfo, m.get_types);
			jsize n_types = (*env)->GetArrayLength(env, jtypes);
			for (jsize t = 0; t < n_types; t++) {
				jstring jtype = (*env)->GetObjectArrayElement(env, jtypes, t);
				char *type = utf(env, jtype);
				jobject caps = (*env)->CallObjectMethod(env, jinfo, m.caps_for_type, jtype);
				if (!ndk_jni_threw(env, type) && caps) {
					/* the name after the slash of the type is added when the codec has several types, ".1", ".2" ... when the name is taken */
					const char *slash = strrchr(type, '/');
					char *base = n_types > 1 ? g_strconcat(codec_name, ".", slash ? slash + 1 : type, NULL) : g_strdup(codec_name);
					char *ndk_name = g_strdup(base);
					for (int copy = 1; g_hash_table_contains(name_to_info, ndk_name); copy++) {
						g_free(ndk_name);
						ndk_name = g_strdup_printf("%s.%d", base, copy);
					}
					AMediaCodecInfo *info = info_new(env, jinfo, ndk_name, type, caps);
					g_ptr_array_add(infos, info);
					g_hash_table_insert(name_to_info, info->name, info);
					add_media_type(type, info->encoder);
					g_free(base);
					g_free(ndk_name);
				}
				(*env)->DeleteLocalRef(env, caps);
				(*env)->DeleteLocalRef(env, jtype);
				g_free(type);
			}
			(*env)->DeleteLocalRef(env, jtypes);
			(*env)->DeleteLocalRef(env, jinfo);
			g_free(codec_name);
		}
		store_ready = true;
	}
	g_mutex_unlock(&store_lock);
	return env;
}

/* NdkMediaCodecStore.cpp */
media_status_t AMediaCodecStore_getSupportedMediaTypes(const AMediaCodecSupportedMediaType **outMediaTypes, size_t *outCount)
{
	if (!outMediaTypes || !outCount)
		return AMEDIA_ERROR_INVALID_PARAMETER;
	store_init();
	*outCount = n_media_types;
	*outMediaTypes = media_types;
	return AMEDIA_OK;
}

static media_status_t find_next_codec(const AMediaFormat *format, bool encoder, const AMediaCodecInfo **outCodecInfo)
{
	if (!outCodecInfo)
		return AMEDIA_ERROR_INVALID_PARAMETER;
	JNIEnv *env = store_init();
	const char *mime = NULL;
	jobject jformat = NULL;
	if (format) {
		if (!AMediaFormat_getString((AMediaFormat *)format, "mime", &mime))
			return AMEDIA_ERROR_INVALID_PARAMETER;
		bool known = false;
		for (guint i = 0; i < infos->len && !known; i++)
			known = !strcmp(((AMediaCodecInfo *)infos->pdata[i])->media_type, mime);
		if (!known)
			return AMEDIA_ERROR_UNSUPPORTED;
		jformat = AMediaFormat_toJava(env, format);
	}
	/* the codec after *outCodecInfo, the first one for NULL */
	bool found = *outCodecInfo == NULL;
	for (guint i = 0; i < infos->len; i++) {
		AMediaCodecInfo *info = infos->pdata[i];
		if (mime && strcmp(info->media_type, mime))
			continue;
		if (found && info->encoder == encoder && (!jformat || (*env)->CallBooleanMethod(env, info->caps, m.format_supported, jformat))) {
			*outCodecInfo = info;
			if (jformat)
				(*env)->DeleteLocalRef(env, jformat);
			return AMEDIA_OK;
		}
		if (*outCodecInfo == info)
			found = true;
	}
	if (jformat)
		(*env)->DeleteLocalRef(env, jformat);
	*outCodecInfo = NULL;
	return AMEDIA_ERROR_UNSUPPORTED;
}

media_status_t AMediaCodecStore_findNextDecoderForFormat(const AMediaFormat *format, const AMediaCodecInfo **outCodecInfo)
{
	return find_next_codec(format, false, outCodecInfo);
}

media_status_t AMediaCodecStore_findNextEncoderForFormat(const AMediaFormat *format, const AMediaCodecInfo **outCodecInfo)
{
	return find_next_codec(format, true, outCodecInfo);
}

media_status_t AMediaCodecStore_getCodecInfo(const char *name, const AMediaCodecInfo **outCodecInfo)
{
	if (!outCodecInfo || !name)
		return AMEDIA_ERROR_INVALID_PARAMETER;
	store_init();
	*outCodecInfo = g_hash_table_lookup(name_to_info, name);
	return *outCodecInfo ? AMEDIA_OK : AMEDIA_ERROR_UNSUPPORTED;
}

/* NdkMediaCodecInfo.cpp: AMediaCodecInfo */
const char *AMediaCodecInfo_getCanonicalName(const AMediaCodecInfo *info)
{
	return info ? info->canonical_name : NULL;
}

AMediaCodecKind AMediaCodecInfo_getKind(const AMediaCodecInfo *info)
{
	if (!info)
		return AMediaCodecKind_INVALID;
	return info->encoder ? AMediaCodecKind_ENCODER : AMediaCodecKind_DECODER;
}

int32_t AMediaCodecInfo_isVendor(const AMediaCodecInfo *info)
{
	return info ? info->vendor : -1;
}

AMediaCodecType AMediaCodecInfo_getMediaCodecInfoType(const AMediaCodecInfo *info)
{
	return info ? info->type : AMediaCodecType_INVALID_CODEC_INFO;
}

const char *AMediaCodecInfo_getMediaType(const AMediaCodecInfo *info)
{
	return info ? info->media_type : NULL;
}

int32_t AMediaCodecInfo_getMaxSupportedInstances(const AMediaCodecInfo *info)
{
	if (!info)
		return -1;
	JNIEnv *env = env_init();
	return (*env)->CallIntMethod(env, info->caps, m.max_instances);
}

static int32_t feature(const AMediaCodecInfo *info, const char *name, jmethodID method)
{
	if (!info || !name)
		return -1;
	JNIEnv *env = env_init();
	jstring jname = (*env)->NewStringUTF(env, name);
	jboolean result = (*env)->CallBooleanMethod(env, info->caps, method, jname);
	(*env)->DeleteLocalRef(env, jname);
	return result;
}

int32_t AMediaCodecInfo_isFeatureSupported(const AMediaCodecInfo *info, const char *featureName)
{
	return feature(info, featureName, m.feature_supported);
}

int32_t AMediaCodecInfo_isFeatureRequired(const AMediaCodecInfo *info, const char *featureName)
{
	return feature(info, featureName, m.feature_required);
}

int32_t AMediaCodecInfo_isFormatSupported(const AMediaCodecInfo *info, const AMediaFormat *format)
{
	if (!info || !format)
		return -1;
	JNIEnv *env = env_init();
	jobject jformat = AMediaFormat_toJava(env, format);
	jboolean result = (*env)->CallBooleanMethod(env, info->caps, m.format_supported, jformat);
	(*env)->DeleteLocalRef(env, jformat);
	return result;
}

media_status_t AMediaCodecInfo_getAudioCapabilities(const AMediaCodecInfo *info, const ACodecAudioCapabilities **outAudioCaps)
{
	if (!info || !outAudioCaps)
		return AMEDIA_ERROR_INVALID_PARAMETER;
	*outAudioCaps = info->audio;
	return *outAudioCaps ? AMEDIA_OK : AMEDIA_ERROR_UNSUPPORTED;
}

media_status_t AMediaCodecInfo_getVideoCapabilities(const AMediaCodecInfo *info, const ACodecVideoCapabilities **outVideoCaps)
{
	if (!info || !outVideoCaps)
		return AMEDIA_ERROR_INVALID_PARAMETER;
	*outVideoCaps = info->video;
	return *outVideoCaps ? AMEDIA_OK : AMEDIA_ERROR_UNSUPPORTED;
}

media_status_t AMediaCodecInfo_getEncoderCapabilities(const AMediaCodecInfo *info, const ACodecEncoderCapabilities **outEncoderCaps)
{
	if (!info || !outEncoderCaps)
		return AMEDIA_ERROR_INVALID_PARAMETER;
	*outEncoderCaps = info->encoder_caps;
	return *outEncoderCaps ? AMEDIA_OK : AMEDIA_ERROR_UNSUPPORTED;
}

/* ACodecAudioCapabilities */
media_status_t ACodecAudioCapabilities_getBitrateRange(const ACodecAudioCapabilities *audioCaps, AIntRange *outRange)
{
	if (!audioCaps || !outRange)
		return AMEDIA_ERROR_INVALID_PARAMETER;
	return get_range(audioCaps->caps, m.a_bitrate, false, outRange);
}

media_status_t ACodecAudioCapabilities_getSupportedSampleRates(const ACodecAudioCapabilities *audioCaps, const int **outArrayPtr, size_t *outCount)
{
	if (!audioCaps || !outArrayPtr || !outCount)
		return AMEDIA_ERROR_INVALID_PARAMETER;
	if (!audioCaps->n_sample_rates)
		return AMEDIA_ERROR_UNSUPPORTED;
	*outArrayPtr = audioCaps->sample_rates;
	*outCount = audioCaps->n_sample_rates;
	return AMEDIA_OK;
}

media_status_t ACodecAudioCapabilities_getSupportedSampleRateRanges(const ACodecAudioCapabilities *audioCaps, const AIntRange **outArrayPtr, size_t *outCount)
{
	if (!audioCaps || !outArrayPtr || !outCount)
		return AMEDIA_ERROR_INVALID_PARAMETER;
	*outArrayPtr = audioCaps->sample_rate_ranges;
	*outCount = audioCaps->n_sample_rate_ranges;
	return AMEDIA_OK;
}

int32_t ACodecAudioCapabilities_getMaxInputChannelCount(const ACodecAudioCapabilities *audioCaps)
{
	if (!audioCaps)
		return -1;
	JNIEnv *env = env_init();
	return (*env)->CallIntMethod(env, audioCaps->caps, m.a_max_channels);
}

int32_t ACodecAudioCapabilities_getMinInputChannelCount(const ACodecAudioCapabilities *audioCaps)
{
	if (!audioCaps)
		return -1;
	JNIEnv *env = env_init();
	return (*env)->CallIntMethod(env, audioCaps->caps, m.a_min_channels);
}

media_status_t ACodecAudioCapabilities_getInputChannelCountRanges(const ACodecAudioCapabilities *audioCaps, const AIntRange **outArrayPtr, size_t *outCount)
{
	if (!audioCaps || !outArrayPtr || !outCount)
		return AMEDIA_ERROR_INVALID_PARAMETER;
	*outArrayPtr = audioCaps->channel_ranges;
	*outCount = audioCaps->n_channel_ranges;
	return AMEDIA_OK;
}

int32_t ACodecAudioCapabilities_isSampleRateSupported(const ACodecAudioCapabilities *audioCaps, int32_t sampleRate)
{
	if (!audioCaps)
		return -1;
	JNIEnv *env = env_init();
	return (*env)->CallBooleanMethod(env, audioCaps->caps, m.a_rate_supported, sampleRate);
}

/* ACodecPerformancePoint: AOSP's C++ PerformancePoint is the Java one; equals() of AOSP compares the macroblock
 * size (16x16 blocks here) and the frame rate, which the C struct keeps as it was created */
ACodecPerformancePoint *ACodecPerformancePoint_create(int32_t width, int32_t height, int32_t frameRate)
{
	JNIEnv *env = env_init();
	ACodecPerformancePoint *pp = g_new0(ACodecPerformancePoint, 1);
	jobject point = (*env)->NewObject(env, m.point_class, m.p_ctor, width, height, frameRate);
	pp->point = (*env)->NewGlobalRef(env, point);
	(*env)->DeleteLocalRef(env, point);
	pp->width = width;
	pp->height = height;
	pp->frame_rate = frameRate;
	return pp;
}

void ACodecPerformancePoint_destroy(ACodecPerformancePoint *performancePoint)
{
	if (!performancePoint)
		return;
	JNIEnv *env = env_init();
	(*env)->DeleteGlobalRef(env, performancePoint->point);
	g_free(performancePoint);
}

int32_t ACodecPerformancePoint_coversFormat(const ACodecPerformancePoint *performancePoint, const AMediaFormat *format)
{
	if (!performancePoint || !format)
		return -1;
	JNIEnv *env = env_init();
	jobject jformat = AMediaFormat_toJava(env, format);
	jboolean result = (*env)->CallBooleanMethod(env, performancePoint->point, m.p_covers_format, jformat);
	(*env)->DeleteLocalRef(env, jformat);
	return result;
}

int32_t ACodecPerformancePoint_covers(const ACodecPerformancePoint *one, const ACodecPerformancePoint *another)
{
	if (!one || !another)
		return -1;
	JNIEnv *env = env_init();
	return (*env)->CallBooleanMethod(env, one->point, m.p_covers, another->point);
}

int32_t ACodecPerformancePoint_equals(const ACodecPerformancePoint *one, const ACodecPerformancePoint *another)
{
	if (!one || !another)
		return -1;
	return (one->width + 15) / 16 == (another->width + 15) / 16 && (one->height + 15) / 16 == (another->height + 15) / 16 &&
	       MAX(1, one->frame_rate) == MAX(1, another->frame_rate);
}

/* ACodecVideoCapabilities */
media_status_t ACodecVideoCapabilities_getBitrateRange(const ACodecVideoCapabilities *videoCaps, AIntRange *outRange)
{
	if (!videoCaps || !outRange)
		return AMEDIA_ERROR_INVALID_PARAMETER;
	return get_range(videoCaps->caps, m.v_bitrate, false, outRange);
}

media_status_t ACodecVideoCapabilities_getSupportedWidths(const ACodecVideoCapabilities *videoCaps, AIntRange *outRange)
{
	if (!videoCaps || !outRange)
		return AMEDIA_ERROR_INVALID_PARAMETER;
	return get_range(videoCaps->caps, m.v_widths, false, outRange);
}

media_status_t ACodecVideoCapabilities_getSupportedHeights(const ACodecVideoCapabilities *videoCaps, AIntRange *outRange)
{
	if (!videoCaps || !outRange)
		return AMEDIA_ERROR_INVALID_PARAMETER;
	return get_range(videoCaps->caps, m.v_heights, false, outRange);
}

int32_t ACodecVideoCapabilities_getWidthAlignment(const ACodecVideoCapabilities *videoCaps)
{
	if (!videoCaps)
		return -1;
	JNIEnv *env = env_init();
	return (*env)->CallIntMethod(env, videoCaps->caps, m.v_width_align);
}

int32_t ACodecVideoCapabilities_getHeightAlignment(const ACodecVideoCapabilities *videoCaps)
{
	if (!videoCaps)
		return -1;
	JNIEnv *env = env_init();
	return (*env)->CallIntMethod(env, videoCaps->caps, m.v_height_align);
}

media_status_t ACodecVideoCapabilities_getSupportedFrameRates(const ACodecVideoCapabilities *videoCaps, AIntRange *outRange)
{
	if (!videoCaps || !outRange)
		return AMEDIA_ERROR_INVALID_PARAMETER;
	return get_range(videoCaps->caps, m.v_frame_rates, false, outRange);
}

media_status_t ACodecVideoCapabilities_getSupportedWidthsFor(const ACodecVideoCapabilities *videoCaps, int32_t height, AIntRange *outRange)
{
	if (!videoCaps || !outRange)
		return AMEDIA_ERROR_INVALID_PARAMETER;
	return get_range(videoCaps->caps, m.v_widths_for, false, outRange, height);
}

media_status_t ACodecVideoCapabilities_getSupportedHeightsFor(const ACodecVideoCapabilities *videoCaps, int32_t width, AIntRange *outRange)
{
	if (!videoCaps || !outRange)
		return AMEDIA_ERROR_INVALID_PARAMETER;
	return get_range(videoCaps->caps, m.v_heights_for, false, outRange, width);
}

media_status_t ACodecVideoCapabilities_getSupportedFrameRatesFor(const ACodecVideoCapabilities *videoCaps, int32_t width, int32_t height, ADoubleRange *outRange)
{
	if (!videoCaps || !outRange)
		return AMEDIA_ERROR_INVALID_PARAMETER;
	return get_range(videoCaps->caps, m.v_frame_rates_for, true, outRange, width, height);
}

media_status_t ACodecVideoCapabilities_getAchievableFrameRatesFor(const ACodecVideoCapabilities *videoCaps, int32_t width, int32_t height, ADoubleRange *outRange)
{
	if (!videoCaps || !outRange)
		return AMEDIA_ERROR_INVALID_PARAMETER;
	return get_range(videoCaps->caps, m.v_achievable, true, outRange, width, height);
}

/* ATL's Java class publishes no performance points, so the list is empty */
media_status_t ACodecVideoCapabilities_getNextSupportedPerformancePoint(const ACodecVideoCapabilities *videoCaps, const ACodecPerformancePoint **outPerformancePoint)
{
	if (!videoCaps || !outPerformancePoint)
		return AMEDIA_ERROR_INVALID_PARAMETER;
	*outPerformancePoint = NULL;
	return AMEDIA_ERROR_UNSUPPORTED;
}

int32_t ACodecVideoCapabilities_areSizeAndRateSupported(const ACodecVideoCapabilities *videoCaps, int32_t width, int32_t height, double frameRate)
{
	if (!videoCaps)
		return -1;
	JNIEnv *env = env_init();
	return (*env)->CallBooleanMethod(env, videoCaps->caps, m.v_size_rate, width, height, frameRate);
}

int32_t ACodecVideoCapabilities_isSizeSupported(const ACodecVideoCapabilities *videoCaps, int32_t width, int32_t height)
{
	if (!videoCaps)
		return -1;
	JNIEnv *env = env_init();
	return (*env)->CallBooleanMethod(env, videoCaps->caps, m.v_size, width, height);
}

/* ACodecEncoderCapabilities */
media_status_t ACodecEncoderCapabilities_getQualityRange(const ACodecEncoderCapabilities *encoderCaps, AIntRange *outRange)
{
	if (!encoderCaps || !outRange)
		return AMEDIA_ERROR_INVALID_PARAMETER;
	return get_range(encoderCaps->caps, m.e_quality, false, outRange);
}

media_status_t ACodecEncoderCapabilities_getComplexityRange(const ACodecEncoderCapabilities *encoderCaps, AIntRange *outRange)
{
	if (!encoderCaps || !outRange)
		return AMEDIA_ERROR_INVALID_PARAMETER;
	return get_range(encoderCaps->caps, m.e_complexity, false, outRange);
}

int32_t ACodecEncoderCapabilities_isBitrateModeSupported(const ACodecEncoderCapabilities *encoderCaps, ABitrateMode mode)
{
	if (!encoderCaps)
		return -1;
	JNIEnv *env = env_init();
	return (*env)->CallBooleanMethod(env, encoderCaps->caps, m.e_bitrate_mode, (jint)mode);
}

/* feature names */
const char *AMediaCodecInfo_FEATURE_AdaptivePlayback = "adaptive-playback";
const char *AMediaCodecInfo_FEATURE_SecurePlayback = "secure-playback";
const char *AMediaCodecInfo_FEATURE_TunneledPlayback = "tunneled-playback";
const char *AMediaCodecInfo_FEATURE_DynamicTimestamp = "dynamic-timestamp";
const char *AMediaCodecInfo_FEATURE_FrameParsing = "frame-parsing";
const char *AMediaCodecInfo_FEATURE_MultipleFrames = "multiple-frames";
const char *AMediaCodecInfo_FEATURE_PartialFrame = "partial-frame";
const char *AMediaCodecInfo_FEATURE_IntraRefresh = "intra-refresh";
const char *AMediaCodecInfo_FEATURE_LowLatency = "low-latency";
const char *AMediaCodecInfo_FEATURE_QpBounds = "qp-bounds";
const char *AMediaCodecInfo_FEATURE_EncodingStatistics = "encoding-statistics";
const char *AMediaCodecInfo_FEATURE_HdrEditing = "hdr-editing";
const char *AMediaCodecInfo_FEATURE_HlgEditing = "hlg-editing";
const char *AMediaCodecInfo_FEATURE_DynamicColorAspects = "dynamic-color-aspects";
const char *AMediaCodecInfo_FEATURE_Roi = "region-of-interest";
const char *AMediaCodecInfo_FEATURE_DetachedSurface = "detached-surface";
