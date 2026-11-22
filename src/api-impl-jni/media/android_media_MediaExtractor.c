/*
 * android.media.MediaExtractor on FFmpeg's libavformat.
 *
 * AOSP: NuMediaExtractor (frameworks/av/media/libstagefright/NuMediaExtractor.cpp) over the extractor plugins
 * (frameworks/av/media/module/extractors), the formats converted by convertMetaDataToMessage()
 * (frameworks/av/media/libstagefright/Utils.cpp). Here one demuxer reads all tracks; the samples of each selected
 * track are queued, and the current sample is the earliest queued one (NuMediaExtractor::fetchAllTrackSamples()).
 * Samples and codec specific data have AOSP's layout: H.264/HEVC in Annex-B, AAC without ADTS header, a Vorbis
 * sample followed by its int32 number of page samples (-1: not known).
 */

#include <errno.h>
#include <fcntl.h>
#include <math.h>
#include <stdbool.h>
#include <string.h>
#include <sys/stat.h>
#include <unistd.h>

#include <glib.h>
#include <libavformat/avformat.h>
#include <libavutil/display.h>

#include "../defines.h"
#include "../util.h"
#include "av_input.h"
#include "../generated_headers/android_media_MediaExtractor.h"

struct sample {
	struct sample *next;
	uint8_t *data;
	int size;
	int64_t time_us;
	int flags; /* SAMPLE_FLAG_SYNC */
};

struct track {
	int stream;      /* index in format->streams */
	const char *mime;
	int nal_length;  /* bytes of the NAL size prefix of an H.264/HEVC sample (avcC/hvcC), 0: already Annex-B */
	bool adts;       /* AAC in ADTS frames: the header is cut off */
	bool vorbis;
	bool selected;
	unsigned order;  /* selection order: the first selected wins among samples of the same time */
	bool eos;
	struct sample *head, *tail;
	int64_t skip_until; /* after seekTo(): samples before the sync sample of this time (index time base) are dropped */
	bool need_key;   /* after seekTo() without an index: up to the first sync sample */
};

struct extractor {
	AVFormatContext *format;
	AVIOContext *io;
	struct fd_region region;
	int nb_tracks;
	struct track *tracks;
	int *track_of_stream; /* -1: no track */
	unsigned order;
	AVPacket *packet;
};

#define SAMPLE_FLAG_SYNC 1

/* MediaDefs.cpp; NULL: no track (AOSP's extractors do not expose such a stream either) */
static const char *mime_of(const AVCodecParameters *par)
{
	switch (par->codec_id) {
		case AV_CODEC_ID_H264: return "video/avc";
		case AV_CODEC_ID_HEVC: return "video/hevc";
		case AV_CODEC_ID_VP8: return "video/x-vnd.on2.vp8";
		case AV_CODEC_ID_VP9: return "video/x-vnd.on2.vp9";
		case AV_CODEC_ID_AV1: return "video/av01";
		case AV_CODEC_ID_MPEG4: return "video/mp4v-es";
		case AV_CODEC_ID_H263: return "video/3gpp";
		case AV_CODEC_ID_MPEG2VIDEO: return "video/mpeg2";
		case AV_CODEC_ID_MJPEG: return "video/x-motion-jpeg";
		case AV_CODEC_ID_AAC: return "audio/mp4a-latm";
		case AV_CODEC_ID_MP3: return "audio/mpeg";
		case AV_CODEC_ID_MP2: return "audio/mpeg-L2";
		case AV_CODEC_ID_MP1: return "audio/mpeg-L1";
		case AV_CODEC_ID_OPUS: return "audio/opus";
		case AV_CODEC_ID_VORBIS: return "audio/vorbis";
		case AV_CODEC_ID_FLAC: return "audio/flac";
		case AV_CODEC_ID_AMR_NB: return "audio/3gpp";
		case AV_CODEC_ID_AMR_WB: return "audio/amr-wb";
		case AV_CODEC_ID_AC3: return "audio/ac3";
		case AV_CODEC_ID_EAC3: return "audio/eac3";
		case AV_CODEC_ID_ALAC: return "audio/alac";
		case AV_CODEC_ID_DTS: return "audio/vnd.dts";
		case AV_CODEC_ID_PCM_ALAW: return "audio/g711-alaw";
		case AV_CODEC_ID_PCM_MULAW: return "audio/g711-mlaw";
		case AV_CODEC_ID_PCM_S16LE:
		case AV_CODEC_ID_PCM_U8:
		case AV_CODEC_ID_PCM_F32LE:
		case AV_CODEC_ID_PCM_S32LE: return "audio/raw";
		case AV_CODEC_ID_MOV_TEXT: return "text/3gpp-tt";
		case AV_CODEC_ID_WEBVTT: return "text/vtt";
		case AV_CODEC_ID_SUBRIP: return "application/x-subrip";
		default: return NULL;
	}
}

static bool is_mp4(AVFormatContext *format)
{
	return strstr(format->iformat->name, "mp4") != NULL;
}

/* ---- the Java map (AOSP ConvertMessageToMap(): Integer, Long, String, ByteBuffer.wrap(byte[])) ---- */

static void put(JNIEnv *env, jobject map, const char *key, jobject value)
{
	jstring k = _JSTRING(key);
	jobject old = (*env)->CallObjectMethod(env, map, _METHOD(_CLASS(map), "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"), k, value);
	(*env)->DeleteLocalRef(env, old);
	(*env)->DeleteLocalRef(env, k);
	(*env)->DeleteLocalRef(env, value);
}

static void put_int(JNIEnv *env, jobject map, const char *key, int32_t value)
{
	jclass c = (*env)->FindClass(env, "java/lang/Integer");
	put(env, map, key, (*env)->CallStaticObjectMethod(env, c, _STATIC_METHOD(c, "valueOf", "(I)Ljava/lang/Integer;"), value));
}

static void put_long(JNIEnv *env, jobject map, const char *key, int64_t value)
{
	jclass c = (*env)->FindClass(env, "java/lang/Long");
	put(env, map, key, (*env)->CallStaticObjectMethod(env, c, _STATIC_METHOD(c, "valueOf", "(J)Ljava/lang/Long;"), value));
}

static void put_string(JNIEnv *env, jobject map, const char *key, const char *value)
{
	put(env, map, key, _JSTRING(value));
}

static void put_buffer(JNIEnv *env, jobject map, const char *key, const uint8_t *data, int size)
{
	jbyteArray array = (*env)->NewByteArray(env, size);
	(*env)->SetByteArrayRegion(env, array, 0, size, (const jbyte *)data);
	jclass c = (*env)->FindClass(env, "java/nio/ByteBuffer");
	put(env, map, key, (*env)->CallStaticObjectMethod(env, c, _STATIC_METHOD(c, "wrap", "([B)Ljava/nio/ByteBuffer;"), array));
	(*env)->DeleteLocalRef(env, array);
}

static void put_ns(JNIEnv *env, jobject map, const char *key, int64_t ns)
{
	put_buffer(env, map, key, (const uint8_t *)&ns, sizeof(ns)); /* int64_t in host order, as AOSP */
}

/* ---- codec specific data (Utils.cpp convertMetaDataToMessage() and the extractors) ---- */

/* copyNALUToABuffer(): a 4 byte start code before each NAL unit */
static void append_nal(GByteArray *out, const uint8_t *nal, int size)
{
	g_byte_array_append(out, (const uint8_t[]){0, 0, 0, 1}, 4);
	g_byte_array_append(out, nal, size);
}

static bool is_annexb(const uint8_t *p, int size)
{
	return size >= 4 && p[0] == 0 && p[1] == 0 && (p[2] == 1 || (p[2] == 0 && p[3] == 1));
}

/* calls fn for each NAL unit of Annex-B data */
static void annexb_nals(const uint8_t *p, int size, void (*fn)(const uint8_t *nal, int size, void *data), void *data)
{
	int i = 0, start = -1;
	while (i + 3 <= size) {
		if (p[i] == 0 && p[i + 1] == 0 && p[i + 2] == 1) {
			if (start >= 0) {
				int end = i;
				while (end > start && p[end - 1] == 0)
					end--;
				fn(p + start, end - start, data);
			}
			i += 3;
			start = i;
		} else {
			i++;
		}
	}
	if (start >= 0 && start < size)
		fn(p + start, size - start, data);
}

/* parseAvcProfileLevelFromAvcc(): profile_idc, constraint flags, level_idc */
static void avc_profile_level(JNIEnv *env, jobject map, uint8_t profile, uint8_t constraints, uint8_t level)
{
	static const struct { uint8_t idc; int32_t value; } profiles[] = {
		{66, 0x01}, {77, 0x02}, {88, 0x04}, {100, 0x08}, {110, 0x10}, {122, 0x20}, {244, 0x40}};
	/* AVCLevel1 .. AVCLevel62 are 1 << n in this order (AVCLevel1b is 1 << 1) */
	static const uint8_t levels[] = {10, 0, 11, 12, 13, 20, 21, 22, 30, 31, 32, 40, 41, 42, 50, 51, 52, 60, 61, 62};
	int32_t codec_profile = -1;
	for (size_t i = 0; i < G_N_ELEMENTS(profiles); i++)
		if (profiles[i].idc == profile)
			codec_profile = profiles[i].value;
	if (codec_profile < 0)
		return;
	if (profile == 66 && (constraints & 0x40))
		codec_profile = 0x10000; /* AVCProfileConstrainedBaseline */
	else if (profile == 100 && (constraints & 0x0C) == 0x0C)
		codec_profile = 0x80000; /* AVCProfileConstrainedHigh */
	put_int(env, map, "profile", codec_profile);
	int32_t codec_level = -1;
	if (level == 9)
		codec_level = 0x02;
	for (size_t i = 0; i < G_N_ELEMENTS(levels); i++)
		if (levels[i] && levels[i] == level)
			codec_level = 1 << i;
	if (level == 11 && (profile == 66 || profile == 77 || profile == 88))
		codec_level = (constraints & 0x10) ? 0x02 : 0x04;
	if (codec_level >= 0)
		put_int(env, map, "level", codec_level);
}

/* parseHevcProfileLevelFromHvcc() (the HDR profiles need the VUI, which is not parsed: Main10 stays Main10) */
static void hevc_profile_level(JNIEnv *env, jobject map, const uint8_t *hvcc, int size)
{
	if (size < 13 || hvcc[0] != 1)
		return;
	uint8_t profile = hvcc[1] & 0x1f, tier = (hvcc[1] & 0x20) >> 5, level = hvcc[12];
	int32_t codec_profile;
	if (profile == 1 || profile == 3 || (profile != 2 && (hvcc[2] & 0x40)))
		codec_profile = 0x01; /* HEVCProfileMain */
	else if (profile == 2 || (hvcc[2] & 0x20))
		codec_profile = 0x02; /* HEVCProfileMain10 */
	else
		return;
	put_int(env, map, "profile", codec_profile);
	/* HEVCMainTierLevel1 .. 62: 1 << 2n, the high tier 1 << (2n + 1) */
	static const uint8_t levels[] = {30, 60, 63, 90, 93, 120, 123, 150, 153, 156, 180, 183, 186};
	for (size_t i = 0; i < G_N_ELEMENTS(levels); i++)
		if (levels[i] == level)
			put_int(env, map, "level", 1 << (2 * i + tier));
}

/* parseAacProfileFromCsd(): the audio object type, which the AACObject* constants are */
static void aac_profile(JNIEnv *env, jobject map, const uint8_t *csd, int size)
{
	if (size < 2)
		return;
	uint16_t aot = csd[0] << 8 | csd[1];
	aot = (aot & 0xf800) == 0xf800 ? 32 + ((aot >> 5) & 0x3f) : aot >> 11;
	static const uint16_t known[] = {1, 2, 3, 4, 5, 6, 17, 23, 29, 39, 42};
	for (size_t i = 0; i < G_N_ELEMENTS(known); i++)
		if (known[i] == aot)
			put_int(env, map, "profile", aot);
}

struct avc_sets {
	GByteArray *sps, *pps;
	const uint8_t *first_sps;
	int first_sps_size;
};

static void avc_sort_nal(const uint8_t *nal, int size, void *data)
{
	struct avc_sets *sets = data;
	if (size < 1)
		return;
	if ((nal[0] & 0x1f) == 7) {
		append_nal(sets->sps, nal, size);
		if (!sets->first_sps) {
			sets->first_sps = nal;
			sets->first_sps_size = size;
		}
	} else if ((nal[0] & 0x1f) == 8) {
		append_nal(sets->pps, nal, size);
	}
}

static void hevc_copy_nal(const uint8_t *nal, int size, void *data)
{
	append_nal(data, nal, size);
}

/* csd-0 SPS and csd-1 PPS, each NAL unit with a start code; the NAL size length of the samples */
static int avc_csd(JNIEnv *env, jobject map, const uint8_t *p, int size)
{
	struct avc_sets sets = {g_byte_array_new(), g_byte_array_new()};
	int nal_length = 0;
	if (size >= 7 && p[0] == 1) {
		avc_profile_level(env, map, p[1], p[2], p[3]);
		nal_length = (p[4] & 3) + 1;
		int n = p[5] & 31, o = 6;
		for (int i = 0; i < n && o + 2 <= size; i++) {
			int l = p[o] << 8 | p[o + 1];
			if (o + 2 + l > size)
				break;
			append_nal(sets.sps, p + o + 2, l);
			o += 2 + l;
		}
		n = o < size ? p[o++] : 0;
		for (int i = 0; i < n && o + 2 <= size; i++) {
			int l = p[o] << 8 | p[o + 1];
			if (o + 2 + l > size)
				break;
			append_nal(sets.pps, p + o + 2, l);
			o += 2 + l;
		}
	} else if (is_annexb(p, size)) {
		annexb_nals(p, size, avc_sort_nal, &sets);
		if (sets.first_sps_size >= 4)
			avc_profile_level(env, map, sets.first_sps[1], sets.first_sps[2], sets.first_sps[3]);
	}
	if (sets.sps->len)
		put_buffer(env, map, "csd-0", sets.sps->data, sets.sps->len);
	if (sets.pps->len)
		put_buffer(env, map, "csd-1", sets.pps->data, sets.pps->len);
	g_byte_array_free(sets.sps, TRUE);
	g_byte_array_free(sets.pps, TRUE);
	return nal_length;
}

/* csd-0: the NAL units of all arrays of the hvcC (VPS, SPS, PPS, SEI), each with a start code */
static int hevc_csd(JNIEnv *env, jobject map, const uint8_t *p, int size)
{
	GByteArray *csd = g_byte_array_new();
	int nal_length = 0;
	if (size >= 23 && !is_annexb(p, size) && (p[0] == 1 || p[0] == 0)) {
		nal_length = (p[21] & 3) + 1;
		int arrays = p[22], o = 23;
		for (int a = 0; a < arrays && o + 3 <= size; a++) {
			int n = p[o + 1] << 8 | p[o + 2];
			o += 3;
			for (int i = 0; i < n && o + 2 <= size; i++) {
				int l = p[o] << 8 | p[o + 1];
				if (o + 2 + l > size)
					break;
				append_nal(csd, p + o + 2, l);
				o += 2 + l;
			}
		}
		hevc_profile_level(env, map, p, size);
	} else if (is_annexb(p, size)) {
		annexb_nals(p, size, hevc_copy_nal, csd);
	}
	if (csd->len)
		put_buffer(env, map, "csd-0", csd->data, csd->len);
	g_byte_array_free(csd, TRUE);
	return nal_length;
}

/* AOSP's AACExtractor / ESQueue: the 2 byte AudioSpecificConfig of an ADTS stream (MakeAACCodecSpecificData()) */
static void adts_csd(JNIEnv *env, jobject map, const AVCodecParameters *par)
{
	static const int rates[] = {96000, 88200, 64000, 48000, 44100, 32000, 24000, 22050, 16000, 12000, 11025, 8000, 7350};
	int index = 15;
	for (size_t i = 0; i < G_N_ELEMENTS(rates); i++)
		if (rates[i] == par->sample_rate)
			index = i;
	int aot = par->profile >= 0 ? par->profile + 1 : 2; /* FF_PROFILE_AAC_* is the object type - 1 */
	uint8_t csd[2] = {aot << 3 | index >> 1, (index & 1) << 7 | par->ch_layout.nb_channels << 3};
	put_buffer(env, map, "csd-0", csd, 2);
	aac_profile(env, map, csd, 2);
}

/* Xiph lacing (FFmpeg's extradata of Vorbis): csd-0 the identification header, csd-1 the setup header */
static void vorbis_csd(JNIEnv *env, jobject map, const uint8_t *p, int size)
{
	if (size < 3 || p[0] != 2)
		return;
	int o = 1, len[2] = {0, 0};
	for (int i = 0; i < 2; i++) {
		while (o < size && p[o] == 0xff)
			len[i] += p[o++];
		if (o >= size)
			return;
		len[i] += p[o++];
	}
	if (o + len[0] + len[1] >= size || p[o] != 1 || p[o + len[0] + len[1]] != 5)
		return;
	put_buffer(env, map, "csd-0", p + o, len[0]);
	put_buffer(env, map, "csd-1", p + o + len[0] + len[1], size - o - len[0] - len[1]);
}

/* csd-0 OpusHead, csd-1 codec delay and csd-2 seek pre-roll in ns (MPEG4/Ogg: pre-skip and 80 ms; Matroska: the
 * CodecDelay and SeekPreRoll elements, which FFmpeg gives in 48 kHz samples) */
static void opus_csd(JNIEnv *env, jobject map, AVFormatContext *format, const AVCodecParameters *par)
{
	if (par->extradata_size < 19)
		return;
	put_buffer(env, map, "csd-0", par->extradata, par->extradata_size);
	bool mkv = strstr(format->iformat->name, "matroska") != NULL;
	int64_t delay = mkv ? par->initial_padding : (par->extradata[10] | par->extradata[11] << 8);
	put_ns(env, map, "csd-1", delay * 1000000000LL / 48000);
	put_ns(env, map, "csd-2", mkv && par->seek_preroll > 0 ? par->seek_preroll * 1000000000LL / 48000 : 80000000LL);
}

/* the largest sample (+ 10 * 2 for the start codes) of the MPEG4 sample table; without one AOSP's estimate */
static int32_t mp4_max_input_size(AVStream *st, const char *mime)
{
	int64_t max = 0;
	int n = avformat_index_get_entries_count(st);
	for (int i = 0; i < n; i++)
		max = MAX(max, avformat_index_get_entry(st, i)->size);
	if (max > 0)
		return max + 10 * 2;
	int64_t w = st->codecpar->width ? st->codecpar->width : 1920, h = st->codecpar->height ? st->codecpar->height : 1080;
	if (!strncmp(mime, "audio/", 6))
		max = 1024 * 128;
	else if (!strcmp(mime, "video/avc") || !strcmp(mime, "video/hevc"))
		max = ((w + 15) / 16) * ((h + 15) / 16) * 192;
	else
		max = w * h * 3 / 2;
	return max + max / 10;
}

static int64_t duration_us(AVFormatContext *format, AVStream *st)
{
	if (st->duration != AV_NOPTS_VALUE && st->duration > 0)
		return av_rescale_q(st->duration, st->time_base, AV_TIME_BASE_Q);
	return format->duration != AV_NOPTS_VALUE ? format->duration : -1;
}

static jobject track_format(JNIEnv *env, struct extractor *ex, struct track *t)
{
	jclass hash_map = (*env)->FindClass(env, "java/util/HashMap");
	jobject map = (*env)->NewObject(env, hash_map, _METHOD(hash_map, "<init>", "()V"));
	AVFormatContext *format = ex->format;
	AVStream *st = format->streams[t->stream];
	AVCodecParameters *par = st->codecpar;
	bool mp4 = is_mp4(format);

	put_string(env, map, "mime", t->mime);
	put_int(env, map, "track-id", mp4 && st->id > 0 ? st->id : (int)(t - ex->tracks) + 1);
	int64_t duration = duration_us(format, st);
	if (duration >= 0)
		put_long(env, map, "durationUs", duration);
	if (par->bit_rate > 0 && par->bit_rate <= INT32_MAX)
		put_int(env, map, "bitrate", par->bit_rate);
	AVDictionaryEntry *language = av_dict_get(st->metadata, "language", NULL, 0);
	if (language)
		put_string(env, map, "language", language->value);

	if (par->codec_type == AVMEDIA_TYPE_VIDEO) {
		put_int(env, map, "width", par->width);
		put_int(env, map, "height", par->height);
		/* the display matrix of the track header: AOSP's degrees are clockwise, FFmpeg's counterclockwise */
		const AVPacketSideData *sd = av_packet_side_data_get(par->coded_side_data, par->nb_coded_side_data, AV_PKT_DATA_DISPLAYMATRIX);
		if (sd && sd->size >= 9 * 4) {
			double angle = av_display_rotation_get((const int32_t *)sd->data);
			int rotation = ((int)lround(-angle) % 360 + 360) % 360;
			if (rotation == 90 || rotation == 180 || rotation == 270)
				put_int(env, map, "rotation-degrees", rotation);
		}
		if (mp4 && st->nb_frames > 0 && duration > 0) {
			put_int(env, map, "frame-rate", (st->nb_frames * 1000000LL + (duration >> 1)) / duration);
			put_int(env, map, "frame-count", st->nb_frames);
		}
	} else if (par->codec_type == AVMEDIA_TYPE_AUDIO) {
		put_int(env, map, "channel-count", par->ch_layout.nb_channels);
		put_int(env, map, "sample-rate", par->sample_rate);
		if (par->codec_id == AV_CODEC_ID_PCM_U8)
			put_int(env, map, "pcm-encoding", 3); /* AudioFormat.ENCODING_PCM_8BIT */
		else if (par->codec_id == AV_CODEC_ID_PCM_F32LE)
			put_int(env, map, "pcm-encoding", 4); /* ENCODING_PCM_FLOAT */
		else if (par->codec_id == AV_CODEC_ID_PCM_S32LE)
			put_int(env, map, "pcm-encoding", 22); /* ENCODING_PCM_32BIT */
	}
	if (mp4)
		put_int(env, map, "max-input-size", mp4_max_input_size(st, t->mime));

	const uint8_t *extra = par->extradata;
	int extra_size = par->extradata_size;
	switch (par->codec_id) {
		case AV_CODEC_ID_H264:
			avc_csd(env, map, extra, extra_size);
			break;
		case AV_CODEC_ID_HEVC:
			hevc_csd(env, map, extra, extra_size);
			break;
		case AV_CODEC_ID_AAC:
			if (extra_size >= 2) {
				put_buffer(env, map, "csd-0", extra, extra_size);
				aac_profile(env, map, extra, extra_size);
			} else {
				adts_csd(env, map, par);
			}
			break;
		case AV_CODEC_ID_OPUS:
			opus_csd(env, map, format, par);
			break;
		case AV_CODEC_ID_VORBIS:
			vorbis_csd(env, map, extra, extra_size);
			break;
		case AV_CODEC_ID_FLAC:
			/* Matroska's CodecPrivate: "fLaC" and the STREAMINFO block, which FFmpeg's extradata is */
			if (extra_size == 34) {
				uint8_t csd[42] = {'f', 'L', 'a', 'C', 0x80, 0, 0, 34};
				memcpy(csd + 8, extra, 34);
				put_buffer(env, map, "csd-0", csd, sizeof(csd));
			}
			break;
		case AV_CODEC_ID_AV1:
		case AV_CODEC_ID_MPEG4:
		case AV_CODEC_ID_ALAC:
			/* av1C / esds DecoderSpecificInfo / ALAC magic cookie as stored */
			if (extra_size > 0)
				put_buffer(env, map, "csd-0", extra, extra_size);
			break;
		default:
			break;
	}
	return map;
}

/* ---- samples ---- */

static void free_samples(struct track *t)
{
	while (t->head) {
		struct sample *s = t->head;
		t->head = s->next;
		g_free(s->data);
		g_free(s);
	}
	t->tail = NULL;
}

/* MPEG4Extractor/MatroskaExtractor: the length prefixes of the NAL units become 4 byte start codes */
static uint8_t *to_annexb(const uint8_t *p, int size, int len, int *out_size)
{
	GByteArray *out = g_byte_array_sized_new(size + 16);
	int o = 0;
	while (o + len <= size) {
		uint32_t n = 0;
		for (int i = 0; i < len; i++)
			n = n << 8 | p[o + i];
		o += len;
		if (n > (uint32_t)(size - o))
			break; /* malformed: the rest is dropped */
		append_nal(out, p + o, n);
		o += n;
	}
	*out_size = out->len;
	return g_byte_array_free(out, FALSE);
}

static void queue_packet(struct extractor *ex, struct track *t, AVPacket *pkt)
{
	AVStream *st = ex->format->streams[t->stream];
	const uint8_t *data = pkt->data;
	int size = pkt->size;
	struct sample *s = g_new0(struct sample, 1);

	if (t->adts && size >= 7 && data[0] == 0xff && (data[1] & 0xf6) == 0xf0) {
		int header = (data[1] & 1) ? 7 : 9; /* protection_absent */
		data += MIN(header, size);
		size -= MIN(header, size);
	}
	if (t->nal_length) {
		s->data = to_annexb(data, size, t->nal_length, &s->size);
	} else {
		s->size = size + (t->vorbis ? 4 : 0);
		s->data = g_malloc(MAX(s->size, 1));
		memcpy(s->data, data, size);
		if (t->vorbis) /* NuMediaExtractor::appendVorbisNumPageSamples() without kKeyValidSamples */
			memcpy(s->data + size, &(int32_t){-1}, 4);
	}
	int64_t ts = pkt->pts != AV_NOPTS_VALUE ? pkt->pts : pkt->dts;
	s->time_us = ts != AV_NOPTS_VALUE ? av_rescale_q(ts, st->time_base, AV_TIME_BASE_Q) : 0;
	s->flags = (pkt->flags & AV_PKT_FLAG_KEY) ? SAMPLE_FLAG_SYNC : 0;
	if (t->tail)
		t->tail->next = s;
	else
		t->head = s;
	t->tail = s;
}

/* reads until every selected track has a sample or is at its end */
static void fill(struct extractor *ex)
{
	for (;;) {
		bool need = false;
		for (int i = 0; i < ex->nb_tracks; i++) {
			struct track *t = &ex->tracks[i];
			if (t->selected && !t->head && !t->eos)
				need = true;
		}
		if (!need)
			return;
		if (av_read_frame(ex->format, ex->packet) < 0) {
			for (int i = 0; i < ex->nb_tracks; i++)
				ex->tracks[i].eos = true;
			return;
		}
		AVPacket *pkt = ex->packet;
		int index = pkt->stream_index < (int)ex->format->nb_streams ? ex->track_of_stream[pkt->stream_index] : -1;
		struct track *t = index >= 0 ? &ex->tracks[index] : NULL;
		if (t && t->selected && !t->eos) {
			int64_t ts = pkt->dts != AV_NOPTS_VALUE ? pkt->dts : pkt->pts;
			bool key = pkt->flags & AV_PKT_FLAG_KEY;
			bool drop = (t->need_key && !key) ||
			            (t->skip_until != AV_NOPTS_VALUE && (ts == AV_NOPTS_VALUE || ts < t->skip_until || !key));
			if (!drop) {
				t->need_key = false;
				t->skip_until = AV_NOPTS_VALUE;
				queue_packet(ex, t, pkt);
			}
		}
		av_packet_unref(pkt);
	}
}

/* the track of the current sample: the earliest one, the first selected among equal times */
static struct track *current(struct extractor *ex)
{
	fill(ex);
	struct track *min = NULL;
	for (int i = 0; i < ex->nb_tracks; i++) {
		struct track *t = &ex->tracks[i];
		if (!t->selected || !t->head)
			continue;
		if (!min || t->head->time_us < min->head->time_us || (t->head->time_us == min->head->time_us && t->order < min->order))
			min = t;
	}
	return min;
}

/* ---- JNI ---- */

JNIEXPORT jlong JNICALL Java_android_media_MediaExtractor_native_1open(JNIEnv *env, jclass class, jstring path_jstr, jobjectArray keys, jobjectArray values, jint fd, jlong offset, jlong length, jobject data_source)
{
	struct extractor *ex = g_new0(struct extractor, 1);
	const char *path = NULL;
	char *url = NULL;
	AVDictionary *options = NULL;

	ex->region.fd = -1;
	if (path_jstr) {
		/* DataSourceFactory::CreateFromURI(): http(s) through the network, file:// and everything else a file */
		path = _CSTRING(path_jstr);
		if (!g_ascii_strncasecmp(path, "http://", 7) || !g_ascii_strncasecmp(path, "https://", 8)) {
			url = g_strdup(path);
			GString *headers = g_string_new(NULL);
			for (jsize i = 0; keys && values && i < (*env)->GetArrayLength(env, keys); i++) {
				jstring k = (*env)->GetObjectArrayElement(env, keys, i), v = (*env)->GetObjectArrayElement(env, values, i);
				const char *ks = _CSTRING(k), *vs = _CSTRING(v);
				g_string_append_printf(headers, "%s: %s\r\n", ks, vs);
				(*env)->ReleaseStringUTFChars(env, k, ks);
				(*env)->ReleaseStringUTFChars(env, v, vs);
			}
			if (headers->len)
				av_dict_set(&options, "headers", headers->str, 0);
			g_string_free(headers, TRUE);
		} else {
			url = g_strconcat("file:", g_ascii_strncasecmp(path, "file://", 7) ? path : path + 7, NULL);
		}
		(*env)->ReleaseStringUTFChars(env, path_jstr, path);
	} else if (data_source) {
		/* the size MediaDataSource.getSize() tells, or unknown (-1) */
		jlong size = (*env)->CallLongMethod(env, data_source, _METHOD(_CLASS(data_source), "getSize", "()J"));
		if ((*env)->ExceptionCheck(env)) {
			(*env)->ExceptionClear(env);
			size = -1;
		}
		ex->region = (struct fd_region){.fd = -1, .length = size >= 0 ? size : INT64_MAX, .data_source = _REF(data_source)};
	} else {
		/* NuMediaExtractor: FileSource(dup(fd), offset, length), the region ends at the end of the file at the latest */
		struct stat st;
		int own = fcntl(fd, F_DUPFD_CLOEXEC, 0);
		int64_t size = own >= 0 && fstat(own, &st) == 0 && S_ISREG(st.st_mode) ? st.st_size - offset : INT64_MAX;
		ex->region = (struct fd_region){.fd = own, .offset = offset, .length = (length <= 0 || length > size) ? size : length};
	}
	if (url || ex->region.fd >= 0 || ex->region.data_source)
		ex->format = open_input_options(url, &ex->region, &ex->io, &options);
	av_dict_free(&options);
	g_free(url);
	if (!ex->format) {
		Java_android_media_MediaExtractor_native_1release(env, class, _INTPTR(ex));
		return 0;
	}

	ex->packet = av_packet_alloc();
	ex->tracks = g_new0(struct track, ex->format->nb_streams);
	ex->track_of_stream = g_new(int, ex->format->nb_streams);
	for (unsigned int i = 0; i < ex->format->nb_streams; i++) {
		AVStream *st = ex->format->streams[i];
		const char *mime = mime_of(st->codecpar);
		ex->track_of_stream[i] = -1;
		st->discard = AVDISCARD_ALL;
		if (!mime || (st->disposition & AV_DISPOSITION_ATTACHED_PIC))
			continue;
		struct track *t = &ex->tracks[ex->nb_tracks];
		*t = (struct track){.stream = i, .mime = mime, .skip_until = AV_NOPTS_VALUE};
		const uint8_t *e = st->codecpar->extradata;
		int n = st->codecpar->extradata_size;
		if (st->codecpar->codec_id == AV_CODEC_ID_H264 && n >= 7 && e[0] == 1)
			t->nal_length = (e[4] & 3) + 1;
		else if (st->codecpar->codec_id == AV_CODEC_ID_HEVC && n >= 23 && !is_annexb(e, n) && e[0] <= 1)
			t->nal_length = (e[21] & 3) + 1;
		t->adts = st->codecpar->codec_id == AV_CODEC_ID_AAC && n < 2;
		t->vorbis = st->codecpar->codec_id == AV_CODEC_ID_VORBIS;
		ex->track_of_stream[i] = ex->nb_tracks++;
	}
	return _INTPTR(ex);
}

JNIEXPORT void JNICALL Java_android_media_MediaExtractor_native_1release(JNIEnv *env, jclass class, jlong ptr)
{
	struct extractor *ex = _PTR(ptr);
	for (int i = 0; i < ex->nb_tracks; i++)
		free_samples(&ex->tracks[i]);
	if (ex->format || ex->io)
		close_input(ex->format, ex->io);
	if (ex->region.fd >= 0)
		close(ex->region.fd);
	if (ex->region.data_source)
		_UNREF(ex->region.data_source);
	av_packet_free(&ex->packet);
	g_free(ex->tracks);
	g_free(ex->track_of_stream);
	g_free(ex);
}

JNIEXPORT jint JNICALL Java_android_media_MediaExtractor_native_1getTrackCount(JNIEnv *env, jclass class, jlong ptr)
{
	return ((struct extractor *)_PTR(ptr))->nb_tracks;
}

/* NULL: no such track (AOSP -ERANGE) */
JNIEXPORT jobject JNICALL Java_android_media_MediaExtractor_native_1getTrackFormat(JNIEnv *env, jclass class, jlong ptr, jint index)
{
	struct extractor *ex = _PTR(ptr);
	if (index < 0 || index >= ex->nb_tracks)
		return NULL;
	return track_format(env, ex, &ex->tracks[index]);
}

/* MatroskaExtractor tells WebM by the DocType, which FFmpeg does not keep: here by WebM's codecs */
static bool webm(struct extractor *ex)
{
	for (unsigned int i = 0; i < ex->format->nb_streams; i++) {
		enum AVCodecID id = ex->format->streams[i]->codecpar->codec_id;
		if (id != AV_CODEC_ID_VP8 && id != AV_CODEC_ID_VP9 && id != AV_CODEC_ID_AV1 && id != AV_CODEC_ID_OPUS &&
		    id != AV_CODEC_ID_VORBIS && id != AV_CODEC_ID_WEBVTT)
			return false;
	}
	return true;
}

/* the "mime" of the container as the extractors set it; NULL if AOSP's extractor sets none */
JNIEXPORT jobject JNICALL Java_android_media_MediaExtractor_native_1getFileFormat(JNIEnv *env, jclass class, jlong ptr)
{
	struct extractor *ex = _PTR(ptr);
	const char *name = ex->format->iformat->name, *mime = NULL;
	bool video = false;
	for (int i = 0; i < ex->nb_tracks; i++)
		video |= !strncmp(ex->tracks[i].mime, "video/", 6);
	if (is_mp4(ex->format))
		mime = video ? "video/mp4" : "audio/mp4";
	else if (strstr(name, "matroska"))
		mime = webm(ex) ? "video/webm" : "video/x-matroska";
	else if (!strcmp(name, "ogg"))
		mime = "audio/ogg";
	else if (!strcmp(name, "mp3"))
		mime = "audio/mpeg";
	else if (!strcmp(name, "aac"))
		mime = "audio/aac-adts";
	else if (!strcmp(name, "flac"))
		mime = "audio/flac";
	else if (!strcmp(name, "wav"))
		mime = "audio/x-wav";
	else if (!strcmp(name, "mpegts"))
		mime = "video/mp2ts";
	else if (!strcmp(name, "amr"))
		mime = "audio/3gpp";
	if (!mime)
		return NULL;
	jclass hash_map = (*env)->FindClass(env, "java/util/HashMap");
	jobject map = (*env)->NewObject(env, hash_map, _METHOD(hash_map, "<init>", "()V"));
	put_string(env, map, "mime", mime);
	return map;
}

/* false: no such track */
JNIEXPORT jboolean JNICALL Java_android_media_MediaExtractor_native_1selectTrack(JNIEnv *env, jclass class, jlong ptr, jint index, jboolean select)
{
	struct extractor *ex = _PTR(ptr);
	if (index < 0 || index >= ex->nb_tracks)
		return JNI_FALSE;
	struct track *t = &ex->tracks[index];
	if (t->selected == !!select)
		return JNI_TRUE;
	free_samples(t);
	t->selected = select;
	t->order = ++ex->order;
	t->eos = false;
	t->skip_until = AV_NOPTS_VALUE;
	t->need_key = false;
	ex->format->streams[t->stream]->discard = select ? AVDISCARD_DEFAULT : AVDISCARD_ALL;
	return JNI_TRUE;
}

/* index entry of the sync sample for the mode (SEEK_TO_PREVIOUS_SYNC 0, NEXT_SYNC 1, CLOSEST_SYNC 2); -1: none */
static int sync_entry(AVStream *st, int64_t ts, int mode)
{
	int prev = av_index_search_timestamp(st, ts, AVSEEK_FLAG_BACKWARD);
	int next = av_index_search_timestamp(st, ts, 0);
	if (mode == 1)
		return next;
	if (mode == 0)
		return prev >= 0 ? prev : next;
	if (prev < 0 || next < 0)
		return prev >= 0 ? prev : next;
	int64_t a = ts - avformat_index_get_entry(st, prev)->timestamp, b = avformat_index_get_entry(st, next)->timestamp - ts;
	return b < a ? next : prev;
}

/* each selected track goes to its sync sample of timeUs (MediaSource::ReadOptions::setSeekTo()); the demuxer
 * to the earliest of them */
JNIEXPORT void JNICALL Java_android_media_MediaExtractor_native_1seekTo(JNIEnv *env, jclass class, jlong ptr, jlong time_us, jint mode)
{
	struct extractor *ex = _PTR(ptr);
	int64_t start = INT64_MAX;
	bool any = false;
	for (int i = 0; i < ex->nb_tracks; i++) {
		struct track *t = &ex->tracks[i];
		if (!t->selected)
			continue;
		any = true;
		AVStream *st = ex->format->streams[t->stream];
		free_samples(t);
		t->eos = false;
		t->need_key = false;
		t->skip_until = AV_NOPTS_VALUE;
		int64_t ts = av_rescale_q(time_us, AV_TIME_BASE_Q, st->time_base);
		if (avformat_index_get_entries_count(st) > 0) {
			int e = sync_entry(st, ts, mode);
			if (e < 0) { /* SEEK_TO_NEXT_SYNC after the last sync sample: the track has ended */
				t->eos = true;
				continue;
			}
			t->skip_until = avformat_index_get_entry(st, e)->timestamp;
			start = MIN(start, av_rescale_q(t->skip_until, st->time_base, AV_TIME_BASE_Q));
		} else {
			t->need_key = true;
			start = MIN(start, time_us);
		}
	}
	if (!any)
		return;
	if (start == INT64_MAX)
		start = time_us;
	av_seek_frame(ex->format, -1, start, AVSEEK_FLAG_BACKWARD);
}

JNIEXPORT jboolean JNICALL Java_android_media_MediaExtractor_native_1advance(JNIEnv *env, jclass class, jlong ptr)
{
	struct extractor *ex = _PTR(ptr);
	struct track *t = current(ex);
	if (!t)
		return JNI_FALSE;
	struct sample *s = t->head;
	t->head = s->next;
	if (!t->head)
		t->tail = NULL;
	g_free(s->data);
	g_free(s);
	return current(ex) != NULL;
}

/* the size; -1 at the end; -2 when the buffer is too small (JNI: IllegalArgumentException) */
JNIEXPORT jint JNICALL Java_android_media_MediaExtractor_native_1readSampleData(JNIEnv *env, jclass class, jlong ptr, jobject buffer, jint offset)
{
	struct extractor *ex = _PTR(ptr);
	uint8_t *dst = (*env)->GetDirectBufferAddress(env, buffer);
	jbyteArray array = NULL;
	jlong capacity;
	if (dst) {
		capacity = (*env)->GetDirectBufferCapacity(env, buffer);
	} else {
		/* AOSP: the whole backing array, from its start */
		array = (*env)->CallObjectMethod(env, buffer, _METHOD((*env)->FindClass(env, "java/nio/ByteBuffer"), "array", "()[B"));
		if ((*env)->ExceptionCheck(env) || !array)
			return -2;
		capacity = (*env)->GetArrayLength(env, array);
	}
	if (capacity < offset)
		return -2;
	struct track *t = current(ex);
	if (!t)
		return -1;
	if (t->head->size > capacity - offset)
		return -2;
	if (dst)
		memcpy(dst + offset, t->head->data, t->head->size);
	else
		(*env)->SetByteArrayRegion(env, array, offset, t->head->size, (const jbyte *)t->head->data);
	return t->head->size;
}

/* what: 0 track index, 1 time, 2 size, 3 flags of the current sample; Long.MIN_VALUE at the end */
JNIEXPORT jlong JNICALL Java_android_media_MediaExtractor_native_1getSample(JNIEnv *env, jclass class, jlong ptr, jint what)
{
	struct extractor *ex = _PTR(ptr);
	struct track *t = current(ex);
	if (!t)
		return INT64_MIN;
	switch (what) {
		case 0: return t - ex->tracks;
		case 1: return t->head->time_us;
		case 2: return t->head->size;
		default: return t->head->flags;
	}
}
