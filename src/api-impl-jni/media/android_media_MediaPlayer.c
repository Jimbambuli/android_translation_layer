#include <alsa/asoundlib.h>
#include <math.h>
#include <sys/stat.h>

#include <gtk/gtk.h>
#include <libavcodec/avcodec.h>
#include <libavfilter/avfilter.h>
#include <libavfilter/buffersink.h>
#include <libavfilter/buffersrc.h>
#include <libavformat/avformat.h>
#include <libavutil/display.h>
#include <libswresample/swresample.h>
#include <libswscale/swscale.h>

#include "../defines.h"
#include "../util.h"
#include "../audio/audio.h"
#include "../widgets/android_view_SurfaceView.h"
#include "av_input.h"

#include "../generated_headers/android_media_MediaPlayer.h"

/* MediaPlayer like AOSP's NuPlayer: the demuxer (libavformat) feeds the decoders (libavcodec). The
 * audio samples go to the same ALSA device as AudioTrack's, and the position of what the device has
 * played is the clock (NuPlayer's MediaClock, anchored by the audio renderer) that the video frames
 * wait for. Audio and video have a demuxer each (the file is opened twice), so that neither waits
 * for packets of the other. The Java side calls the audio part from its playback thread and the
 * video part from its video thread, open and release from the others. */

#define OUT_CHANNELS   2
#define OUT_LATENCY_US 200000

/* native_step() at the end of the stream */
#define STEP_END   (-1)
#define STEP_ERROR (-2)
/* native_videoDecode() */
#define VIDEO_END   (-1)
#define VIDEO_ERROR (-2)
/* the clock runs on from the last audio write for at most this long */
#define CLOCK_MAX_EXTRAPOLATION_MS 250

struct video {
	struct fd_region region;
	AVIOContext *io;
	AVFormatContext *format; /* NULL without a video stream */
	AVCodecContext *codec;
	struct SwsContext *sws;
	AVPacket *packet;
	AVFrame *frame; /* the frame decoded last */
	int stream_index;
	int64_t skip_until; /* ms: after a seek, the frames that end before it are dropped */
	int rotation;       /* degrees clockwise the frames are to be turned for display (the container's display matrix) */
	/* of the frames given to GTK as YUV, made for the first one */
	GdkColorState *color_state;
};

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
	/* the position is base + (written - what the device still holds) * speed, in frames */
	int64_t base;
	int64_t written;
	/* setPlaybackParams(): speed and pitch (requested ones under clock_lock, the playback thread applies them), and
	 * the libavfilter graph that changes the output samples, built when needed (NULL at speed and pitch 1) */
	float speed;
	float pitch;
	float requested_speed;
	float requested_pitch;
	AVFilterGraph *graph;
	AVFilterContext *graph_in;
	AVFilterContext *graph_out;
	AVFrame *filtered;
	int64_t graph_pts;
	gboolean graph_flushed;
	int64_t skip_until; /* after a seek, the decoded frames before this are dropped */
	struct video video;
	/* the clock: the position at the last write to the device, running on in real time while playing;
	 * loops counts the passes of setLooping() */
	GMutex clock_lock;
	int clock_ms;
	gint64 clock_time;
	gboolean clock_running;
	int loops;
};

static void video_free(struct video *v)
{
	if (v->color_state)
		gdk_color_state_unref(v->color_state);
	sws_freeContext(v->sws);
	avcodec_free_context(&v->codec);
	av_packet_free(&v->packet);
	av_frame_free(&v->frame);
	if (v->format || v->io)
		close_input(v->format, v->io);
	*v = (struct video){};
}

/* the video stream of the source, opened a second time (path, or the same fd region): none for
 * audio files, also not their cover picture */
static void video_open(struct player *p, const char *path)
{
	int index = av_find_best_stream(p->format, AVMEDIA_TYPE_VIDEO, -1, -1, NULL, 0);
	if (index < 0 || (p->format->streams[index]->disposition & AV_DISPOSITION_ATTACHED_PIC))
		return;
	struct video *v = &p->video;
	v->region = (struct fd_region){.fd = p->region.fd, .offset = p->region.offset, .length = p->region.length};
	v->format = open_input(path, &v->region, &v->io);
	if (!v->format)
		goto fail;
	const AVCodec *decoder = NULL;
	v->stream_index = av_find_best_stream(v->format, AVMEDIA_TYPE_VIDEO, -1, -1, &decoder, 0);
	if (v->stream_index < 0 || !decoder)
		goto fail;
	v->codec = avcodec_alloc_context3(decoder);
	v->codec->thread_count = 0; /* as many as libavcodec finds useful */
	if (avcodec_parameters_to_context(v->codec, v->format->streams[v->stream_index]->codecpar) < 0 || avcodec_open2(v->codec, decoder, NULL) < 0)
		goto fail;
	for (unsigned i = 0; i < v->format->nb_streams; i++)
		if (i != v->stream_index)
			v->format->streams[i]->discard = AVDISCARD_ALL;
	v->packet = av_packet_alloc();
	v->frame = av_frame_alloc();
	/* like NuPlayer's rotation-degrees: the display matrix turns counterclockwise, Android counts clockwise */
	AVCodecParameters *par = v->format->streams[v->stream_index]->codecpar;
	const AVPacketSideData *matrix = av_packet_side_data_get(par->coded_side_data, par->nb_coded_side_data, AV_PKT_DATA_DISPLAYMATRIX);
	if (matrix && matrix->size >= 9 * sizeof(int32_t)) {
		double degrees = -av_display_rotation_get((const int32_t *)matrix->data);
		if (!isnan(degrees))
			v->rotation = ((int)lround(degrees / 90) * 90 % 360 + 360) % 360;
	}
	return;
fail:
	printf("MediaPlayer: can't decode the video stream\n");
	video_free(v);
}

static void clock_set(struct player *p, int ms, gboolean running)
{
	g_mutex_lock(&p->clock_lock);
	p->clock_ms = ms;
	p->clock_time = g_get_monotonic_time();
	p->clock_running = running;
	g_mutex_unlock(&p->clock_lock);
}

static void graph_free(struct player *p)
{
	avfilter_graph_free(&p->graph);
	p->graph_flushed = FALSE;
}

static void player_free(struct player *p)
{
	graph_free(p);
	av_frame_free(&p->filtered);
	video_free(&p->video);
	g_mutex_clear(&p->clock_lock);
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
	g_mutex_init(&p->clock_lock);
	p->speed = p->pitch = p->requested_speed = p->requested_pitch = 1.f;

	if (path_jstr) {
		path = (*env)->GetStringUTFChars(env, path_jstr, NULL);
	} else {
		/* the region ends at the end of the file at the latest */
		struct stat st;
		int64_t size = fstat(fd, &st) == 0 && S_ISREG(st.st_mode) ? st.st_size - offset : INT64_MAX;
		p->region = (struct fd_region){.fd = fd, .offset = offset, .length = (length <= 0 || length > size) ? size : length};
	}
	p->format = open_input(path, &p->region, &p->io);
	if (p->format)
		video_open(p, path);
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
	for (unsigned i = 0; i < p->format->nb_streams; i++)
		if (i != p->stream_index)
			p->format->streams[i]->discard = AVDISCARD_ALL;
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

/* the media frames played (or being played) so far */
static int64_t media_frames(struct player *p)
{
	return p->base + llround(p->written * (double)p->speed);
}

/* the graph for the speed and pitch: asetrate and aresample shift the pitch (and the speed with it), atempo sets the
 * speed keeping the pitch (one atempo takes 0.5 to 100), aformat gives the output format back; FALSE on failure */
static gboolean graph_build(struct player *p)
{
	char args[128], number[G_ASCII_DTOSTR_BUF_SIZE];
	GString *chain = g_string_new(NULL);
	if (p->pitch != 1.f)
		g_string_append_printf(chain, "asetrate=%ld,aresample=%d,", lround(p->rate * (double)p->pitch), p->rate);
	double tempo = p->speed / (double)p->pitch;
	for (; tempo < 0.5; tempo /= 0.5)
		g_string_append(chain, "atempo=0.5,");
	if (tempo != 1.)
		g_string_append_printf(chain, "atempo=%s,", g_ascii_formatd(number, sizeof(number), "%.6f", tempo));
	g_string_append_printf(chain, "aformat=sample_fmts=s16:channel_layouts=stereo:sample_rates=%d", p->rate);

	p->graph = avfilter_graph_alloc();
	snprintf(args, sizeof(args), "sample_rate=%d:sample_fmt=s16:channel_layout=stereo:time_base=1/%d", p->rate, p->rate);
	AVFilterInOut *outputs = avfilter_inout_alloc();
	AVFilterInOut *inputs = avfilter_inout_alloc();
	gboolean ok = avfilter_graph_create_filter(&p->graph_in, avfilter_get_by_name("abuffer"), "in", args, NULL, p->graph) >= 0;
	if (ok)
		ok = avfilter_graph_create_filter(&p->graph_out, avfilter_get_by_name("abuffersink"), "out", NULL, NULL, p->graph) >= 0;
	if (ok) {
		outputs->name = av_strdup("in");
		outputs->filter_ctx = p->graph_in;
		inputs->name = av_strdup("out");
		inputs->filter_ctx = p->graph_out;
		ok = avfilter_graph_parse_ptr(p->graph, chain->str, &inputs, &outputs, NULL) >= 0 && avfilter_graph_config(p->graph, NULL) >= 0;
	}
	avfilter_inout_free(&inputs);
	avfilter_inout_free(&outputs);
	if (!ok) {
		printf("MediaPlayer: can't set up the playback speed and pitch (%s)\n", chain->str);
		graph_free(p);
	}
	g_string_free(chain, TRUE);
	if (!p->filtered)
		p->filtered = av_frame_alloc();
	return ok;
}

/* the frames frames in p->buffer through the graph (none: the end of the stream): the samples it gives into
 * p->buffer, their number */
static int graph_filter(struct player *p, int frames)
{
	if (!p->graph && !graph_build(p))
		return frames;
	if (frames > 0) {
		AVFrame *in = av_frame_alloc();
		in->format = AV_SAMPLE_FMT_S16;
		av_channel_layout_copy(&in->ch_layout, &(AVChannelLayout)AV_CHANNEL_LAYOUT_STEREO);
		in->sample_rate = p->rate;
		in->nb_samples = frames;
		in->pts = p->graph_pts;
		p->graph_pts += frames;
		if (av_frame_get_buffer(in, 0) == 0) {
			memcpy(in->data[0], p->buffer, frames * OUT_CHANNELS * sizeof(int16_t));
			av_buffersrc_add_frame(p->graph_in, in);
		}
		av_frame_free(&in);
	} else {
		av_buffersrc_add_frame(p->graph_in, NULL);
		p->graph_flushed = TRUE;
	}
	int out = 0;
	while (av_buffersink_get_frame(p->graph_out, p->filtered) >= 0) {
		int n = p->filtered->nb_samples;
		if (out + n > p->buffer_frames) {
			p->buffer = g_renew(int16_t, p->buffer, (out + n) * OUT_CHANNELS);
			p->buffer_frames = out + n;
		}
		memcpy(p->buffer + out * OUT_CHANNELS, p->filtered->data[0], n * OUT_CHANNELS * sizeof(int16_t));
		out += n;
		av_frame_unref(p->filtered);
	}
	return out;
}

/* the next samples to play into p->buffer (decoded, at the speed and pitch): their number, 0 at the end, < 0 on
 * error */
static int decode(struct player *p)
{
	if (!p->codec) {
		int64_t left = (p->format->duration / (AV_TIME_BASE / 1000) - media_frames(p)) / p->speed;
		return MAX(MIN(left, 20), 0);
	}
	AVStream *stream = p->format->streams[p->stream_index];
	while (TRUE) {
		int ret = avcodec_receive_frame(p->codec, p->frame);
		if (ret == AVERROR_EOF) {
			if (p->graph && !p->graph_flushed) {
				int frames = graph_filter(p, 0); /* what the graph still holds */
				if (frames > 0)
					return frames;
			}
			/* the audio ends before the video: silence until the end of the file */
			int64_t left = 0;
			if (p->video.format && p->format->duration != AV_NOPTS_VALUE)
				left = (av_rescale(p->format->duration, p->rate, AV_TIME_BASE) - media_frames(p)) / p->speed;
			if (left <= 0)
				return 0;
			int frames = MIN(left, p->rate / 50);
			if (frames > p->buffer_frames) {
				p->buffer = g_renew(int16_t, p->buffer, frames * OUT_CHANNELS);
				p->buffer_frames = frames;
			}
			memset(p->buffer, 0, frames * OUT_CHANNELS * sizeof(int16_t));
			return frames;
		}
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
		if (p->speed != 1.f || p->pitch != 1.f) {
			frames = graph_filter(p, frames);
			if (frames == 0)
				continue;
		}
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
	p->base = p->skip_until - llround(p->written * (double)p->speed);
	graph_free(p); /* its samples were from before */
}

static jint position_ms(struct player *p)
{
	snd_pcm_sframes_t delay = 0;
	if (p->pcm && snd_pcm_delay(p->pcm, &delay) < 0)
		delay = 0;
	int64_t frames = p->base + llround((p->written - MIN(MAX(delay, 0), p->written)) * (double)p->speed);
	return MAX(frames, 0) * 1000 / p->rate;
}

/* plays the next decoded samples (blocks while the device is full): the position in milliseconds,
 * STEP_END once everything is played, STEP_ERROR if the stream cannot be decoded; looping, the
 * end is followed by the start without a gap */
JNIEXPORT jint JNICALL Java_android_media_MediaPlayer_native_1step(JNIEnv *env, jclass class, jlong ptr, jfloat left, jfloat right, jboolean looping)
{
	struct player *p = _PTR(ptr);

	/* a new speed or pitch: the position goes on from where it is (the samples in the device play at the old speed) */
	g_mutex_lock(&p->clock_lock);
	float speed = p->requested_speed, pitch = p->requested_pitch;
	g_mutex_unlock(&p->clock_lock);
	if (speed != p->speed || pitch != p->pitch) {
		p->base += llround(p->written * (double)(p->speed - speed));
		g_mutex_lock(&p->clock_lock);
		p->speed = speed;
		g_mutex_unlock(&p->clock_lock);
		p->pitch = pitch;
		graph_free(p);
	}

	int frames = decode(p);
	if (frames == 0 && looping) {
		seek(p, 0, FALSE);
		g_mutex_lock(&p->clock_lock);
		p->loops++;
		g_mutex_unlock(&p->clock_lock);
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
		int ms = position_ms(p);
		clock_set(p, ms, TRUE);
		return ms;
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
	int ms = position_ms(p);
	clock_set(p, ms, TRUE);
	return ms;
}

/* setPlaybackParams(): the playback thread applies them with its next samples */
JNIEXPORT void JNICALL Java_android_media_MediaPlayer_native_1setPlaybackRate(JNIEnv *env, jclass class, jlong ptr, jfloat speed, jfloat pitch)
{
	struct player *p = _PTR(ptr);
	g_mutex_lock(&p->clock_lock);
	p->requested_speed = speed;
	p->requested_pitch = pitch;
	g_mutex_unlock(&p->clock_lock);
}

/* drops what the device still holds; the Java side seeks to the paused position on resume */
JNIEXPORT void JNICALL Java_android_media_MediaPlayer_native_1pause(JNIEnv *env, jclass class, jlong ptr)
{
	struct player *p = _PTR(ptr);
	if (p->pcm) {
		snd_pcm_drop(p->pcm);
		snd_pcm_prepare(p->pcm);
	}
	g_mutex_lock(&p->clock_lock);
	p->clock_running = FALSE;
	g_mutex_unlock(&p->clock_lock);
}

/* user: a seekTo() (the passes of looping count anew), not the resume after a pause */
JNIEXPORT void JNICALL Java_android_media_MediaPlayer_native_1seek(JNIEnv *env, jclass class, jlong ptr, jint msec, jboolean user)
{
	struct player *p = _PTR(ptr);
	seek(p, msec, TRUE);
	clock_set(p, msec, FALSE);
	if (user) {
		g_mutex_lock(&p->clock_lock);
		p->loops = 0;
		g_mutex_unlock(&p->clock_lock);
	}
}

/* the media time the video waits for: the passes of looping in the upper, the ms in the lower half */
JNIEXPORT jlong JNICALL Java_android_media_MediaPlayer_native_1clock(JNIEnv *env, jclass class, jlong ptr)
{
	struct player *p = _PTR(ptr);
	g_mutex_lock(&p->clock_lock);
	int64_t ms = p->clock_ms;
	if (p->clock_running)
		ms += MIN((g_get_monotonic_time() - p->clock_time) / 1000, CLOCK_MAX_EXTRAPOLATION_MS) * p->speed;
	jlong clock = ((jlong)p->loops << 32) | (uint32_t)ms;
	g_mutex_unlock(&p->clock_lock);
	return clock;
}

/* the display size like NuPlayer's (the sample aspect ratio applied to the width): width << 16 | height,
 * 0 without video */
JNIEXPORT jint JNICALL Java_android_media_MediaPlayer_native_1getVideoSize(JNIEnv *env, jclass class, jlong ptr)
{
	struct video *v = &((struct player *)_PTR(ptr))->video;
	if (!v->format)
		return 0;
	AVStream *stream = v->format->streams[v->stream_index];
	int width = stream->codecpar->width;
	int height = stream->codecpar->height;
	AVRational sar = av_guess_sample_aspect_ratio(v->format, stream, NULL);
	if (sar.num > 0 && sar.den > 0)
		width = av_rescale(width, sar.num, sar.den);
	if (v->rotation == 90 || v->rotation == 270) { /* NuPlayer reports the size as displayed */
		int w = width;
		width = height;
		height = w;
	}
	return (MIN(width, 0xffff) << 16) | MIN(height, 0xffff);
}

/* decodes the next video frame: its time in ms, VIDEO_END after the last one, VIDEO_ERROR */
JNIEXPORT jint JNICALL Java_android_media_MediaPlayer_native_1videoDecode(JNIEnv *env, jclass class, jlong ptr)
{
	struct video *v = &((struct player *)_PTR(ptr))->video;
	AVStream *stream = v->format->streams[v->stream_index];
	while (TRUE) {
		int ret = avcodec_receive_frame(v->codec, v->frame);
		if (ret == AVERROR_EOF)
			return VIDEO_END;
		if (ret == AVERROR(EAGAIN)) {
			if (av_read_frame(v->format, v->packet) < 0) {
				avcodec_send_packet(v->codec, NULL);
				continue;
			}
			if (v->packet->stream_index == v->stream_index)
				avcodec_send_packet(v->codec, v->packet);
			av_packet_unref(v->packet);
			continue;
		}
		if (ret < 0)
			return VIDEO_ERROR;
		int64_t pts = v->frame->best_effort_timestamp;
		if (pts == AV_NOPTS_VALUE)
			pts = 0;
		if (stream->start_time != AV_NOPTS_VALUE)
			pts -= stream->start_time;
		int64_t ms = av_rescale_q(pts, stream->time_base, (AVRational){1, 1000});
		int64_t end = ms + MAX(av_rescale_q(v->frame->duration, stream->time_base, (AVRational){1, 1000}), 1);
		if (v->skip_until > 0 && end <= v->skip_until)
			continue;
		v->skip_until = 0;
		return MAX(ms, 0);
	}
}

JNIEXPORT void JNICALL Java_android_media_MediaPlayer_native_1videoSeek(JNIEnv *env, jclass class, jlong ptr, jint msec)
{
	struct video *v = &((struct player *)_PTR(ptr))->video;
	AVStream *stream = v->format->streams[v->stream_index];
	int64_t target = av_rescale_q(msec, (AVRational){1, 1000}, stream->time_base);
	if (stream->start_time != AV_NOPTS_VALUE)
		target += stream->start_time;
	av_seek_frame(v->format, v->stream_index, target, AVSEEK_FLAG_BACKWARD);
	avcodec_flush_buffers(v->codec);
	v->skip_until = msec;
}

/* a plane of width x height pixels of bpp bytes, turned clockwise by degrees, into dst (the size turned, packed); in
 * tiles of 32x32 pixels, so that reading and writing stay in the cache */
static void rotate_plane(guchar *dst, const uint8_t *src, int src_stride, int width, int height, int bpp, int degrees)
{
	int dst_width = (degrees == 90 || degrees == 270) ? height : width;
	for (int ty = 0; ty < height; ty += 32)
		for (int tx = 0; tx < width; tx += 32)
			for (int y = ty; y < MIN(ty + 32, height); y++) {
				const uint8_t *row = src + (size_t)y * src_stride;
				for (int x = tx; x < MIN(tx + 32, width); x++) {
					int dx, dy;
					switch (degrees) {
						case 90:
							dx = height - 1 - y;
							dy = x;
							break;
						case 180:
							dx = width - 1 - x;
							dy = height - 1 - y;
							break;
						case 270:
							dx = y;
							dy = width - 1 - x;
							break;
						default:
							dx = x;
							dy = y;
							break;
					}
					guchar *d = dst + ((size_t)dy * dst_width + dx) * bpp;
					if (bpp == 1)
						*d = row[x];
					else
						memcpy(d, row + (size_t)x * bpp, bpp);
				}
			}
}

/* a plane into dst, packed and turned */
static void copy_plane(guchar *dst, const uint8_t *src, int src_stride, int width, int height, int bpp, int degrees)
{
	if (degrees) {
		rotate_plane(dst, src, src_stride, width, height, bpp, degrees);
		return;
	}
	for (int y = 0; y < height; y++)
		memcpy(dst + (size_t)y * width * bpp, src + (size_t)y * src_stride, (size_t)width * bpp);
}

#if GTK_CHECK_VERSION(4, 20, 0)
/* 8-bit YUV 4:2:0 as it is to GTK, which converts it when drawing (no conversion per frame here) */
static GdkTexture *yuv_texture(struct video *v, AVFrame *frame)
{
	if ((frame->format != AV_PIX_FMT_YUV420P && frame->format != AV_PIX_FMT_YUVJ420P) || frame->width % 2 || frame->height % 2)
		return NULL;
	if (!v->color_state) {
		/* the matrix of the frame's color space (unspecified: BT.601, as libswscale's default), sRGB otherwise as
		 * the RGB frames */
		GdkCicpParams *params = gdk_cicp_params_new();
		gdk_cicp_params_set_color_primaries(params, 1);
		gdk_cicp_params_set_transfer_function(params, 13);
		guint matrix = 6;
		if (frame->colorspace == AVCOL_SPC_BT709)
			matrix = 1;
		else if (frame->colorspace == AVCOL_SPC_BT2020_NCL)
			matrix = 9;
		gdk_cicp_params_set_matrix_coefficients(params, matrix);
		gdk_cicp_params_set_range(params, frame->color_range == AVCOL_RANGE_JPEG || frame->format == AV_PIX_FMT_YUVJ420P ? GDK_CICP_RANGE_FULL : GDK_CICP_RANGE_NARROW);
		v->color_state = gdk_cicp_params_build_color_state(params, NULL);
		g_object_unref(params);
		if (!v->color_state)
			return NULL;
	}
	int width = frame->width, height = frame->height;
	gsize luma = (gsize)width * height, chroma = luma / 4;
	guchar *data = g_malloc(luma + 2 * chroma);
	copy_plane(data, frame->data[0], frame->linesize[0], width, height, 1, v->rotation);
	copy_plane(data + luma, frame->data[1], frame->linesize[1], width / 2, height / 2, 1, v->rotation);
	copy_plane(data + luma + chroma, frame->data[2], frame->linesize[2], width / 2, height / 2, 1, v->rotation);
	if (v->rotation == 90 || v->rotation == 270) {
		width = frame->height;
		height = frame->width;
	}
	GBytes *bytes = g_bytes_new_take(data, luma + 2 * chroma);
	GdkMemoryTextureBuilder *builder = gdk_memory_texture_builder_new();
	gdk_memory_texture_builder_set_bytes(builder, bytes);
	gdk_memory_texture_builder_set_width(builder, width);
	gdk_memory_texture_builder_set_height(builder, height);
	gdk_memory_texture_builder_set_format(builder, GDK_MEMORY_G8_B8_R8_420);
	gdk_memory_texture_builder_set_color_state(builder, v->color_state);
	gdk_memory_texture_builder_set_stride_for_plane(builder, 0, width);
	gdk_memory_texture_builder_set_stride_for_plane(builder, 1, width / 2);
	gdk_memory_texture_builder_set_stride_for_plane(builder, 2, width / 2);
	gdk_memory_texture_builder_set_offset(builder, 0, 0);
	gdk_memory_texture_builder_set_offset(builder, 1, luma);
	gdk_memory_texture_builder_set_offset(builder, 2, luma + chroma);
	GdkTexture *texture = gdk_memory_texture_builder_build(builder);
	g_object_unref(builder);
	g_bytes_unref(bytes);
	return texture;
}
#endif

/* the frame decoded last as a new texture, turned for display: 8-bit YUV 4:2:0 as it is (GTK 4.20), other formats
 * converted to RGB in the colors of their color space */
JNIEXPORT jlong JNICALL Java_android_media_MediaPlayer_native_1videoTexture(JNIEnv *env, jclass class, jlong ptr)
{
	struct video *v = &((struct player *)_PTR(ptr))->video;
	AVFrame *frame = v->frame;
#if GTK_CHECK_VERSION(4, 20, 0)
	GdkTexture *yuv = yuv_texture(v, frame);
	if (yuv)
		return _INTPTR(yuv);
#endif
	v->sws = sws_getCachedContext(v->sws, frame->width, frame->height, frame->format, frame->width, frame->height, AV_PIX_FMT_RGB24, SWS_BILINEAR, NULL, NULL, NULL);
	if (!v->sws)
		return 0;
	sws_setColorspaceDetails(v->sws, sws_getCoefficients(frame->colorspace), frame->color_range == AVCOL_RANGE_JPEG, sws_getCoefficients(SWS_CS_DEFAULT), 1, 0, 1 << 16, 1 << 16);
	int width = frame->width, height = frame->height;
	int stride = width * 3;
	guchar *rgb = g_malloc(stride * height);
	sws_scale(v->sws, (const uint8_t *const *)frame->data, frame->linesize, 0, height, (uint8_t *[1]){rgb}, (int[1]){stride});
	if (v->rotation) {
		guchar *turned = g_malloc(stride * height);
		rotate_plane(turned, rgb, stride, width, height, 3, v->rotation);
		g_free(rgb);
		rgb = turned;
		if (v->rotation != 180) {
			width = frame->height;
			height = frame->width;
			stride = width * 3;
		}
	}
	GBytes *bytes = g_bytes_new_take(rgb, stride * height);
	GdkTexture *texture = gdk_memory_texture_new(width, height, GDK_MEMORY_R8G8B8, bytes, stride);
	g_bytes_unref(bytes);
	return _INTPTR(texture);
}

struct show_texture {
	GtkWidget *wrapper;
	GdkTexture *texture;
	gboolean crop;
};

static gboolean show_texture(gpointer data)
{
	struct show_texture *d = data;
	GtkWidget *child = gtk_widget_get_first_child(d->wrapper);
	if (child && SURFACE_VIEW_IS_WIDGET(child)) {
		SURFACE_VIEW_WIDGET(child)->crop = d->crop;
		surface_view_widget_set_texture(SURFACE_VIEW_WIDGET(child), d->texture, FALSE);
	} else {
		g_object_unref(d->texture);
	}
	g_object_unref(d->wrapper);
	g_free(d);
	return G_SOURCE_REMOVE;
}

/* the texture (taken over) into the SurfaceView whose Surface is on the widget, on the main thread; crop: the scaling
 * mode SCALE_TO_FIT_WITH_CROPPING, which the compositor applies in AOSP */
JNIEXPORT void JNICALL Java_android_media_MediaPlayer_native_1showTexture(JNIEnv *env, jclass class, jlong widget, jlong texture, jboolean crop)
{
	if (!texture)
		return;
	struct show_texture *d = g_new(struct show_texture, 1);
	d->wrapper = g_object_ref(_PTR(widget));
	d->texture = _PTR(texture);
	d->crop = crop;
	g_idle_add_full(G_PRIORITY_HIGH_IDLE, show_texture, d, NULL);
}

JNIEXPORT void JNICALL Java_android_media_MediaPlayer_native_1release(JNIEnv *env, jclass class, jlong ptr)
{
	player_free(_PTR(ptr));
}
