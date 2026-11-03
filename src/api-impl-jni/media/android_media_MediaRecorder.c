/* MediaRecorder for sound: the microphone through ALSA (in the Flatpak PulseAudio behind it), encoded and
   muxed with FFmpeg into the file the app gives. Video (camera) is not recorded yet. */
#include <alsa/asoundlib.h>
#include <glib.h>
#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <unistd.h>

#include "../defines.h"
#include "../audio/audio.h"

#include "../generated_headers/android_media_MediaRecorder.h"

struct recorder {
	snd_pcm_t *pcm;
	AVCodecContext *enc;
	AVFormatContext *oc;
	AVStream *st;
	AVFrame *frame;
	int16_t *pcm_buf;
	int frame_size;
	int fd; /* own copy of the app's descriptor, -1 for a path */
	int64_t next_pts;
	GThread *thread;
	gint running;
	gint paused;
	gint max_amplitude;
};

static const char *muxer_name(int output_format)
{
	switch (output_format) {
		case 1: /* THREE_GPP */
			return "3gp";
		case 3: /* AMR_NB */
		case 4: /* AMR_WB */
			return "amr";
		case 5: /* AAC_ADIF: FFmpeg writes no ADIF, ADTS is the same AAC with frame headers */
		case 6: /* AAC_ADTS */
			return "adts";
		case 8:
			return "mpegts";
		case 9:
			return "webm";
		case 11:
			return "ogg";
		default: /* DEFAULT, MPEG_4 */
			return "mp4";
	}
}

static const AVCodec *find_encoder(int audio_encoder)
{
	const char *names[3] = {0};
	switch (audio_encoder) {
		case 1: /* AMR_NB */
			names[0] = "libopencore_amrnb";
			break;
		case 2: /* AMR_WB */
			names[0] = "libvo_amrwbenc";
			break;
		case 3: /* AAC; HE_AAC and AAC_ELD need encoders FFmpeg builds rarely have: AAC-LC */
		case 4:
		case 5:
			names[0] = "aac";
			break;
		case 6:
			names[0] = "libvorbis";
			names[1] = "vorbis";
			break;
		case 7:
			names[0] = "libopus";
			names[1] = "opus";
			break;
		default: /* DEFAULT is AMR-NB on Android; without that encoder AAC */
			names[0] = "libopencore_amrnb";
			names[1] = "aac";
	}
	for (int i = 0; names[i]; i++) {
		const AVCodec *codec = avcodec_find_encoder_by_name(names[i]);
		if (codec)
			return codec;
	}
	return NULL;
}

static int fd_write(void *opaque, const uint8_t *buf, int size)
{
	struct recorder *r = opaque;
	int done = 0;
	while (done < size) {
		ssize_t n = write(r->fd, buf + done, size - done);
		if (n < 0)
			return AVERROR(errno);
		done += n;
	}
	return done;
}

static int64_t fd_seek(void *opaque, int64_t offset, int whence)
{
	struct recorder *r = opaque;
	if (whence == AVSEEK_SIZE)
		return -1;
	return lseek(r->fd, offset, whence & ~AVSEEK_FORCE);
}

static void write_packets(struct recorder *r)
{
	AVPacket *pkt = av_packet_alloc();
	while (avcodec_receive_packet(r->enc, pkt) == 0) {
		av_packet_rescale_ts(pkt, r->enc->time_base, r->st->time_base);
		pkt->stream_index = r->st->index;
		av_interleaved_write_frame(r->oc, pkt);
	}
	av_packet_free(&pkt);
}

/* interleaved 16 bit from ALSA into the encoder's sample format */
static void fill_frame(struct recorder *r, int n)
{
	AVFrame *f = r->frame;
	int ch = r->enc->ch_layout.nb_channels;
	av_frame_make_writable(f);
	f->nb_samples = n;
	for (int i = 0; i < n; i++) {
		for (int c = 0; c < ch; c++) {
			int16_t s = r->pcm_buf[i * ch + c];
			switch (r->enc->sample_fmt) {
				case AV_SAMPLE_FMT_FLTP:
					((float *)f->data[c])[i] = s / 32768.0f;
					break;
				case AV_SAMPLE_FMT_FLT:
					((float *)f->data[0])[i * ch + c] = s / 32768.0f;
					break;
				case AV_SAMPLE_FMT_S16P:
					((int16_t *)f->data[c])[i] = s;
					break;
				default: /* AV_SAMPLE_FMT_S16 */
					((int16_t *)f->data[0])[i * ch + c] = s;
			}
		}
	}
}

static gpointer record_thread(gpointer data)
{
	struct recorder *r = data;
	int ch = r->enc->ch_layout.nb_channels;
	snd_pcm_start(r->pcm);
	while (g_atomic_int_get(&r->running)) {
		snd_pcm_sframes_t n = snd_pcm_readi(r->pcm, r->pcm_buf, r->frame_size);
		if (n == -EPIPE) {
			snd_pcm_recover(r->pcm, n, 1);
			snd_pcm_start(r->pcm);
			continue;
		}
		if (n <= 0)
			continue;
		int peak = 0;
		for (int i = 0; i < n * ch; i++)
			peak = MAX(peak, ABS(r->pcm_buf[i]));
		if (peak > g_atomic_int_get(&r->max_amplitude))
			g_atomic_int_set(&r->max_amplitude, MIN(peak, 32767));
		/* like AOSP the pause leaves no gap in the file; a short read only fits an encoder of any frame size */
		if (g_atomic_int_get(&r->paused) || (n < r->frame_size && !(r->enc->codec->capabilities & AV_CODEC_CAP_VARIABLE_FRAME_SIZE)))
			continue;
		fill_frame(r, n);
		r->frame->pts = r->next_pts;
		r->next_pts += n;
		if (avcodec_send_frame(r->enc, r->frame) == 0)
			write_packets(r);
	}
	return NULL;
}

static void recorder_free(struct recorder *r)
{
	if (r->pcm)
		snd_pcm_close(r->pcm);
	if (r->oc && r->oc->pb) {
		if (r->fd >= 0) {
			av_freep(&r->oc->pb->buffer);
			avio_context_free(&r->oc->pb);
		} else {
			avio_closep(&r->oc->pb);
		}
	}
	if (r->oc)
		avformat_free_context(r->oc);
	if (r->fd >= 0)
		close(r->fd);
	avcodec_free_context(&r->enc);
	av_frame_free(&r->frame);
	g_free(r->pcm_buf);
	g_free(r);
}

JNIEXPORT jlong JNICALL Java_android_media_MediaRecorder_nativePrepare(JNIEnv *env, jclass clazz, jstring path_jstr, jint fd, jint output_format, jint audio_encoder, jint sample_rate, jint channels, jint bit_rate)
{
	const AVCodec *codec = find_encoder(audio_encoder);
	if (!codec) {
		printf("MediaRecorder: no encoder for AudioEncoder %d\n", audio_encoder);
		return 0;
	}
	struct recorder *r = g_new0(struct recorder, 1);
	r->fd = -1;
	r->enc = avcodec_alloc_context3(codec);
	/* the requested rate when the encoder takes it, else 48 kHz or the encoder's first */
	int rate = sample_rate > 0 ? sample_rate : 8000;
	const int *rates = NULL;
	int n_rates = 0;
	avcodec_get_supported_config(r->enc, codec, AV_CODEC_CONFIG_SAMPLE_RATE, 0, (const void **)&rates, &n_rates);
	if (rates) {
		int ok = 0, has48 = 0;
		for (int i = 0; i < n_rates; i++) {
			ok |= rates[i] == rate;
			has48 |= rates[i] == 48000;
		}
		if (!ok)
			rate = has48 ? 48000 : rates[0];
	}
	unsigned int alsa_channels;
	int ret = snd_pcm_open(&r->pcm, PCM_DEVICE, SND_PCM_STREAM_CAPTURE, 0);
	if (ret < 0) {
		printf("MediaRecorder: no microphone: %s\n", snd_strerror(ret));
		r->pcm = NULL;
		goto fail;
	}
	snd_pcm_hw_params_t *params;
	snd_pcm_hw_params_alloca(&params);
	helper_hw_params_init(r->pcm, params, rate, channels == 2 ? 12 : 16, SND_PCM_FORMAT_S16_LE, &alsa_channels);
	if (snd_pcm_hw_params(r->pcm, params) < 0)
		goto fail;
	unsigned int got_rate = rate;
	snd_pcm_hw_params_get_rate(params, &got_rate, NULL);

	enum AVSampleFormat fmt = AV_SAMPLE_FMT_S16;
	const enum AVSampleFormat *fmts = NULL;
	int n_fmts = 0;
	avcodec_get_supported_config(r->enc, codec, AV_CODEC_CONFIG_SAMPLE_FORMAT, 0, (const void **)&fmts, &n_fmts);
	if (fmts) {
		fmt = fmts[0];
		for (int i = 0; i < n_fmts; i++)
			if (fmts[i] == AV_SAMPLE_FMT_S16)
				fmt = AV_SAMPLE_FMT_S16;
	}
	if (fmt != AV_SAMPLE_FMT_S16 && fmt != AV_SAMPLE_FMT_S16P && fmt != AV_SAMPLE_FMT_FLT && fmt != AV_SAMPLE_FMT_FLTP)
		goto fail;
	r->enc->sample_fmt = fmt;
	r->enc->sample_rate = got_rate;
	av_channel_layout_default(&r->enc->ch_layout, alsa_channels);
	r->enc->bit_rate = bit_rate > 0 ? bit_rate : 0;
	r->enc->time_base = (AVRational){1, got_rate};
	r->enc->strict_std_compliance = FF_COMPLIANCE_EXPERIMENTAL; /* FFmpeg's own Opus encoder */

	if (avformat_alloc_output_context2(&r->oc, NULL, muxer_name(output_format), NULL) < 0)
		goto fail;
	if (r->oc->oformat->flags & AVFMT_GLOBALHEADER)
		r->enc->flags |= AV_CODEC_FLAG_GLOBAL_HEADER;
	if (avcodec_open2(r->enc, codec, NULL) < 0)
		goto fail;
	r->st = avformat_new_stream(r->oc, NULL);
	r->st->time_base = r->enc->time_base;
	avcodec_parameters_from_context(r->st->codecpar, r->enc);

	if (path_jstr) {
		const char *path = (*env)->GetStringUTFChars(env, path_jstr, NULL);
		ret = avio_open(&r->oc->pb, path, AVIO_FLAG_WRITE);
		(*env)->ReleaseStringUTFChars(env, path_jstr, path);
		if (ret < 0)
			goto fail;
	} else {
		r->fd = dup(fd);
		if (r->fd < 0)
			goto fail;
		r->oc->pb = avio_alloc_context(av_malloc(32768), 32768, 1, r, NULL, fd_write, fd_seek);
	}
	if (avformat_write_header(r->oc, NULL) < 0)
		goto fail;

	r->frame = av_frame_alloc();
	r->frame->format = fmt;
	r->frame->sample_rate = got_rate;
	av_channel_layout_copy(&r->frame->ch_layout, &r->enc->ch_layout);
	r->frame_size = r->enc->frame_size ?: 1024;
	r->frame->nb_samples = r->frame_size;
	if (av_frame_get_buffer(r->frame, 0) < 0)
		goto fail;
	r->pcm_buf = g_new(int16_t, r->frame_size * alsa_channels);
	return _INTPTR(r);

fail:
	printf("MediaRecorder: prepare failed\n");
	recorder_free(r);
	return 0;
}

JNIEXPORT void JNICALL Java_android_media_MediaRecorder_nativeStart(JNIEnv *env, jclass clazz, jlong ptr)
{
	struct recorder *r = _PTR(ptr);
	g_atomic_int_set(&r->running, 1);
	r->thread = g_thread_new("MediaRecorder", record_thread, r);
}

JNIEXPORT void JNICALL Java_android_media_MediaRecorder_nativePause(JNIEnv *env, jclass clazz, jlong ptr, jboolean paused)
{
	struct recorder *r = _PTR(ptr);
	g_atomic_int_set(&r->paused, paused);
}

JNIEXPORT jint JNICALL Java_android_media_MediaRecorder_nativeGetMaxAmplitude(JNIEnv *env, jclass clazz, jlong ptr)
{
	struct recorder *r = _PTR(ptr);
	return g_atomic_int_exchange(&r->max_amplitude, 0);
}

/* stops a started recording and completes the file; also frees a recorder that was only prepared */
JNIEXPORT void JNICALL Java_android_media_MediaRecorder_nativeStop(JNIEnv *env, jclass clazz, jlong ptr)
{
	struct recorder *r = _PTR(ptr);
	if (r->thread) {
		g_atomic_int_set(&r->running, 0);
		g_thread_join(r->thread);
		if (avcodec_send_frame(r->enc, NULL) == 0)
			write_packets(r);
	}
	av_write_trailer(r->oc);
	recorder_free(r);
}
