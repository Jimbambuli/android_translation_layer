#include <alsa/asoundlib.h>
#include <sys/stat.h>

#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libswresample/swresample.h>

#include "../defines.h"
#include "../util.h"
#include "../audio/audio.h"
#include "av_input.h"

#include "../generated_headers/android_media_MediaPlayer.h"

/* MediaPlayer's audio, like AOSP's NuPlayer: the demuxer (libavformat) feeds the decoder
 * (libavcodec), whose samples go to the same ALSA device as AudioTrack's. The Java side calls all
 * of this from its playback thread, except open and release. */

#define OUT_CHANNELS   2
#define OUT_LATENCY_US 200000

/* native_step() at the end of the stream */
#define STEP_END   (-1)
#define STEP_ERROR (-2)

struct player {
	struct fd_region region;
	AVIOContext *io;
	AVFormatContext *format;
	AVCodecContext *codec;
	SwrContext *swr;
	AVPacket *packet;
	AVFrame *frame;
	int stream_index;
	int rate;
	snd_pcm_t *pcm; /* NULL without an audio device: then the step only waits for the samples' time */
	int16_t *buffer;
	int buffer_frames;
	/* the position is base + written - (what the device still holds), in output frames */
	int64_t base;
	int64_t written;
	int64_t skip_until; /* after a seek, the decoded frames before this are dropped */
};

static void player_free(struct player *p)
{
	if (p->pcm)
		snd_pcm_close(p->pcm);
	swr_free(&p->swr);
	avcodec_free_context(&p->codec);
	av_packet_free(&p->packet);
	av_frame_free(&p->frame);
	close_input(p->format, p->io);
	g_free(p->buffer);
	g_free(p);
}

JNIEXPORT jlong JNICALL Java_android_media_MediaPlayer_native_1open(JNIEnv *env, jclass class, jstring path_jstr, jint fd, jlong offset, jlong length)
{
	struct player *p = g_new0(struct player, 1);
	const char *path = NULL;

	if (path_jstr) {
		path = (*env)->GetStringUTFChars(env, path_jstr, NULL);
	} else {
		/* the region ends at the end of the file at the latest */
		struct stat st;
		int64_t size = fstat(fd, &st) == 0 && S_ISREG(st.st_mode) ? st.st_size - offset : INT64_MAX;
		p->region = (struct fd_region){.fd = fd, .offset = offset, .length = (length <= 0 || length > size) ? size : length};
	}
	p->format = open_input(path, &p->region, &p->io);
	if (path)
		(*env)->ReleaseStringUTFChars(env, path_jstr, path);
	if (!p->format)
		goto fail;

	const AVCodec *decoder = NULL;
	p->stream_index = av_find_best_stream(p->format, AVMEDIA_TYPE_AUDIO, -1, -1, &decoder, 0);
	if (p->stream_index < 0) {
		/* no audio (a video without sound): the playback only takes its time, in ms "frames" */
		if (p->format->duration == AV_NOPTS_VALUE)
			goto fail;
		p->rate = 1000;
		return _INTPTR(p);
	}
	p->codec = avcodec_alloc_context3(decoder);
	if (avcodec_parameters_to_context(p->codec, p->format->streams[p->stream_index]->codecpar) < 0 || avcodec_open2(p->codec, decoder, NULL) < 0)
		goto fail;
	p->rate = p->codec->sample_rate;
	if (p->rate <= 0)
		goto fail;

	AVChannelLayout out_layout = AV_CHANNEL_LAYOUT_STEREO;
	if (p->codec->ch_layout.order == AV_CHANNEL_ORDER_UNSPEC)
		av_channel_layout_default(&p->codec->ch_layout, p->codec->ch_layout.nb_channels);
	if (swr_alloc_set_opts2(&p->swr, &out_layout, AV_SAMPLE_FMT_S16, p->rate, &p->codec->ch_layout, p->codec->sample_fmt, p->rate, 0, NULL) < 0 || swr_init(p->swr) < 0)
		goto fail;
	p->packet = av_packet_alloc();
	p->frame = av_frame_alloc();

	int ret = snd_pcm_open(&p->pcm, PCM_DEVICE, SND_PCM_STREAM_PLAYBACK, 0);
	if (ret == 0)
		ret = snd_pcm_set_params(p->pcm, SND_PCM_FORMAT_S16_LE, SND_PCM_ACCESS_RW_INTERLEAVED, OUT_CHANNELS, p->rate, 1, OUT_LATENCY_US);
	if (ret < 0) {
		printf("MediaPlayer: no audio output (\"%s\": %s)\n", PCM_DEVICE, snd_strerror(ret));
		if (p->pcm)
			snd_pcm_close(p->pcm);
		p->pcm = NULL;
	}
	return _INTPTR(p);

fail:
	player_free(p);
	return 0;
}

/* in milliseconds, -1 if unknown */
JNIEXPORT jint JNICALL Java_android_media_MediaPlayer_native_1getDuration(JNIEnv *env, jclass class, jlong ptr)
{
	struct player *p = _PTR(ptr);
	if (p->format->duration == AV_NOPTS_VALUE)
		return -1;
	return p->format->duration / (AV_TIME_BASE / 1000);
}

/* the next decoded samples into p->buffer: their number, 0 at the end, < 0 on error */
static int decode(struct player *p)
{
	if (!p->codec) {
		int64_t left = p->format->duration / (AV_TIME_BASE / 1000) - (p->base + p->written);
		return MAX(MIN(left, 20), 0);
	}
	AVStream *stream = p->format->streams[p->stream_index];
	while (TRUE) {
		int ret = avcodec_receive_frame(p->codec, p->frame);
		if (ret == AVERROR_EOF)
			return 0;
		if (ret == AVERROR(EAGAIN)) {
			if (av_read_frame(p->format, p->packet) < 0) {
				avcodec_send_packet(p->codec, NULL);
				continue;
			}
			if (p->packet->stream_index == p->stream_index)
				avcodec_send_packet(p->codec, p->packet);
			av_packet_unref(p->packet);
			continue;
		}
		if (ret < 0)
			return ret;

		int frames = swr_get_out_samples(p->swr, p->frame->nb_samples);
		if (frames > p->buffer_frames) {
			p->buffer = g_renew(int16_t, p->buffer, frames * OUT_CHANNELS);
			p->buffer_frames = frames;
		}
		uint8_t *out = (uint8_t *)p->buffer;
		frames = swr_convert(p->swr, &out, frames, (const uint8_t **)p->frame->extended_data, p->frame->nb_samples);
		int64_t pts = p->frame->best_effort_timestamp;
		av_frame_unref(p->frame);
		if (frames <= 0)
			continue;

		/* after a seek: drop what lies before the target */
		if (p->skip_until > 0 && pts != AV_NOPTS_VALUE) {
			if (stream->start_time != AV_NOPTS_VALUE)
				pts -= stream->start_time;
			int64_t start = av_rescale_q(pts, stream->time_base, (AVRational){1, p->rate});
			if (start + frames <= p->skip_until)
				continue;
			if (start < p->skip_until) {
				int skip = p->skip_until - start;
				memmove(p->buffer, p->buffer + skip * OUT_CHANNELS, (frames - skip) * OUT_CHANNELS * sizeof(int16_t));
				frames -= skip;
			}
		}
		p->skip_until = 0;
		return frames;
	}
}

/* the decoder continues at msec; drop: the device's samples go away too (otherwise they are
 * played before the ones from msec, and the position counts from msec once they are through) */
static void seek(struct player *p, int msec, gboolean drop)
{
	if (p->codec) {
		AVStream *stream = p->format->streams[p->stream_index];
		int64_t target = av_rescale_q(msec, (AVRational){1, 1000}, stream->time_base);
		if (stream->start_time != AV_NOPTS_VALUE)
			target += stream->start_time;
		av_seek_frame(p->format, p->stream_index, target, AVSEEK_FLAG_BACKWARD);
		avcodec_flush_buffers(p->codec);
		swr_init(p->swr); /* drops the resampler's buffered samples */
	}
	p->skip_until = (int64_t)msec * p->rate / 1000;
	if (drop) {
		if (p->pcm) {
			snd_pcm_drop(p->pcm);
			snd_pcm_prepare(p->pcm);
		}
		p->written = 0;
	}
	p->base = p->skip_until - p->written;
}

static jint position_ms(struct player *p)
{
	snd_pcm_sframes_t delay = 0;
	if (p->pcm && snd_pcm_delay(p->pcm, &delay) < 0)
		delay = 0;
	int64_t frames = p->base + p->written - MIN(MAX(delay, 0), p->written);
	return MAX(frames, 0) * 1000 / p->rate;
}

/* plays the next decoded samples (blocks while the device is full): the position in milliseconds,
 * STEP_END once everything is played, STEP_ERROR if the stream cannot be decoded; looping, the
 * end is followed by the start without a gap */
JNIEXPORT jint JNICALL Java_android_media_MediaPlayer_native_1step(JNIEnv *env, jclass class, jlong ptr, jfloat left, jfloat right, jboolean looping)
{
	struct player *p = _PTR(ptr);

	int frames = decode(p);
	if (frames == 0 && looping) {
		seek(p, 0, FALSE);
		frames = decode(p);
		if (frames == 0)
			return STEP_ERROR; /* nothing to play at all */
	}
	if (frames < 0)
		return STEP_ERROR;
	if (frames == 0) {
		if (p->pcm)
			snd_pcm_drain(p->pcm);
		return STEP_END;
	}
	if (p->codec && (left != 1.f || right != 1.f)) {
		for (int i = 0; i < frames; i++) {
			p->buffer[i * 2] *= left;
			p->buffer[i * 2 + 1] *= right;
		}
	}
	if (!p->pcm) {
		g_usleep(frames * G_USEC_PER_SEC / p->rate);
		p->written += frames;
		return position_ms(p);
	}
	int16_t *data = p->buffer;
	while (frames > 0) {
		snd_pcm_sframes_t ret = snd_pcm_writei(p->pcm, data, frames);
		if (ret < 0)
			ret = snd_pcm_recover(p->pcm, ret, 1);
		if (ret < 0) {
			printf("MediaPlayer: can't write to the audio device: %s\n", snd_strerror(ret));
			return STEP_ERROR;
		}
		data += ret * OUT_CHANNELS;
		frames -= ret;
		p->written += ret;
	}
	return position_ms(p);
}

/* drops what the device still holds; the Java side seeks to the paused position on resume */
JNIEXPORT void JNICALL Java_android_media_MediaPlayer_native_1pause(JNIEnv *env, jclass class, jlong ptr)
{
	struct player *p = _PTR(ptr);
	if (p->pcm) {
		snd_pcm_drop(p->pcm);
		snd_pcm_prepare(p->pcm);
	}
}

JNIEXPORT void JNICALL Java_android_media_MediaPlayer_native_1seek(JNIEnv *env, jclass class, jlong ptr, jint msec)
{
	seek(_PTR(ptr), msec, TRUE);
}

JNIEXPORT void JNICALL Java_android_media_MediaPlayer_native_1release(JNIEnv *env, jclass class, jlong ptr)
{
	player_free(_PTR(ptr));
}
