#include <math.h>
#include <sys/stat.h>

#include <glib.h>
#include <libavcodec/avcodec.h>
#include <libswresample/swresample.h>

#include "../defines.h"
#include "../media/av_input.h"
#include "audio.h"

#include "../generated_headers/android_media_SoundPool.h"

/* SoundPool like AOSP's (frameworks/av/media/jni/soundpool): a sample is decoded completely when it is
 * loaded (16 bit PCM, at most 1 MiB like AOSP's kDefaultHeapSize), and played by up to maxStreams streams.
 * AOSP gives every stream an AudioTrack and AudioFlinger mixes them; here the pool mixes its streams itself
 * into one ALSA stream on the device of AudioTrack: a stream opened per play() would cost its start-up time
 * (with PulseAudio 0.3-1 s measured for MediaPlayer) on every sound, and a device without a mixer (plain hw)
 * can be opened only once. */

#define OUT_RATE       48000
#define OUT_CHANNELS   2
#define OUT_LATENCY_US 50000
#define PERIOD_FRAMES  (OUT_RATE / 100)
#define MAX_PCM_BYTES  (1024 * 1024)

struct sample {
	int refs; /* the pool's table and every stream playing it; guarded by the pool's lock */
	int16_t *data;
	int frames;
	int channels; /* 1 or 2 */
	int rate;
};

enum stream_state { STREAM_IDLE,
	            STREAM_PLAYING,
	            STREAM_PAUSED };

struct stream {
	enum stream_state state;
	gboolean auto_paused;
	int id;
	int priority;
	uint64_t order; /* when it started: among the same priority, the oldest is replaced first */
	struct sample *sample;
	double position; /* in frames of the sample */
	float rate;
	float left, right;
	int loop; /* the passes still to come after this one, -1 forever */
};

struct pool {
	GMutex lock;
	GCond cond;
	GHashTable *samples; /* id -> struct sample */
	struct stream *streams;
	int max_streams;
	int next_stream_id;
	uint64_t next_order;
	gboolean quit;
	GThread *thread;
};

static void sample_unref(struct sample *sample)
{
	if (--sample->refs == 0) {
		g_free(sample->data);
		g_free(sample);
	}
}

static void stream_free(struct stream *stream)
{
	if (stream->sample)
		sample_unref(stream->sample);
	stream->sample = NULL;
	stream->state = STREAM_IDLE;
	stream->priority = -1;
}

static struct stream *find_stream(struct pool *pool, int id)
{
	for (int i = 0; i < pool->max_streams; i++)
		if (pool->streams[i].state != STREAM_IDLE && pool->streams[i].id == id)
			return &pool->streams[i];
	return NULL;
}

/* mixes PERIOD_FRAMES frames of the playing streams; FALSE if none plays */
static gboolean mix(struct pool *pool, float *acc)
{
	gboolean any = FALSE;
	memset(acc, 0, PERIOD_FRAMES * OUT_CHANNELS * sizeof(float));
	for (int i = 0; i < pool->max_streams; i++) {
		struct stream *s = &pool->streams[i];
		if (s->state != STREAM_PLAYING)
			continue;
		any = TRUE;
		struct sample *sample = s->sample;
		double step = (double)sample->rate * s->rate / OUT_RATE;
		for (int f = 0; f < PERIOD_FRAMES; f++) {
			int at = (int)s->position;
			float frac = s->position - at;
			/* linear interpolation; the frame after the last is the first again when looping */
			int next = at + 1 < sample->frames ? at + 1 : (s->loop != 0 ? 0 : -1);
			for (int c = 0; c < OUT_CHANNELS; c++) {
				int sc = sample->channels == 1 ? 0 : c;
				float v0 = sample->data[at * sample->channels + sc];
				float v1 = next >= 0 ? sample->data[next * sample->channels + sc] : 0.f;
				acc[f * OUT_CHANNELS + c] += (v0 + (v1 - v0) * frac) * (c == 0 ? s->left : s->right);
			}
			s->position += step;
			while (s->position >= sample->frames && s->loop != 0) {
				if (s->loop > 0)
					s->loop--;
				s->position -= sample->frames;
			}
			if (s->position >= sample->frames) {
				stream_free(s);
				break;
			}
		}
	}
	return any;
}

static gpointer mixer_thread(gpointer data)
{
	struct pool *pool = data;
	float acc[PERIOD_FRAMES * OUT_CHANNELS];
	int16_t out[PERIOD_FRAMES * OUT_CHANNELS];
	snd_pcm_t *pcm = NULL;
	gboolean pcm_failed = FALSE;
	gboolean running = FALSE; /* the device has our samples */

	g_mutex_lock(&pool->lock);
	while (!pool->quit) {
		if (!mix(pool, acc)) {
			if (running) { /* let the device play out what it has, then wait for the next play() */
				g_mutex_unlock(&pool->lock);
				snd_pcm_drain(pcm);
				snd_pcm_prepare(pcm);
				g_mutex_lock(&pool->lock);
				running = FALSE;
				continue;
			}
			g_cond_wait(&pool->cond, &pool->lock);
			continue;
		}
		g_mutex_unlock(&pool->lock);

		for (int i = 0; i < PERIOD_FRAMES * OUT_CHANNELS; i++)
			out[i] = CLAMP(lrintf(acc[i]), -32768, 32767);
		if (!pcm && !pcm_failed) {
			int ret = snd_pcm_open(&pcm, PCM_DEVICE, SND_PCM_STREAM_PLAYBACK, 0);
			if (ret == 0)
				ret = snd_pcm_set_params(pcm, SND_PCM_FORMAT_S16_LE, SND_PCM_ACCESS_RW_INTERLEAVED, OUT_CHANNELS, OUT_RATE, 1, OUT_LATENCY_US);
			if (ret < 0) {
				printf("SoundPool: no audio output (\"%s\": %s)\n", PCM_DEVICE, snd_strerror(ret));
				if (pcm)
					snd_pcm_close(pcm);
				pcm = NULL;
				pcm_failed = TRUE;
			}
		}
		if (pcm) {
			int16_t *at = out;
			int frames = PERIOD_FRAMES;
			while (frames > 0) {
				snd_pcm_sframes_t ret = snd_pcm_writei(pcm, at, frames);
				if (ret < 0)
					ret = snd_pcm_recover(pcm, ret, 1);
				if (ret < 0) {
					printf("SoundPool: can't write to the audio device: %s\n", snd_strerror(ret));
					break;
				}
				at += ret * OUT_CHANNELS;
				frames -= ret;
			}
			running = TRUE;
		} else { /* without a device the sounds still take their time */
			g_usleep(PERIOD_FRAMES * G_USEC_PER_SEC / OUT_RATE);
		}
		g_mutex_lock(&pool->lock);
	}
	g_mutex_unlock(&pool->lock);
	if (pcm) {
		snd_pcm_drop(pcm);
		snd_pcm_close(pcm);
	}
	return NULL;
}

JNIEXPORT jlong JNICALL Java_android_media_SoundPool_native_1setup(JNIEnv *env, jclass class, jint max_streams)
{
	struct pool *pool = g_new0(struct pool, 1);
	g_mutex_init(&pool->lock);
	g_cond_init(&pool->cond);
	pool->samples = g_hash_table_new_full(NULL, NULL, NULL, (GDestroyNotify)sample_unref);
	pool->max_streams = MAX(max_streams, 1);
	pool->streams = g_new0(struct stream, pool->max_streams);
	for (int i = 0; i < pool->max_streams; i++)
		pool->streams[i].priority = -1;
	pool->thread = g_thread_new("SoundPool", mixer_thread, pool);
	return _INTPTR(pool);
}

/* the whole file (fd region) as 16 bit PCM in its own rate, mono or stereo: 0, or an error */
static int decode(struct fd_region *region, struct sample *sample)
{
	AVIOContext *io;
	AVFormatContext *format = open_input(NULL, region, &io);
	AVCodecContext *codec = NULL;
	SwrContext *swr = NULL;
	AVPacket *packet = av_packet_alloc();
	AVFrame *frame = av_frame_alloc();
	GByteArray *pcm = g_byte_array_new();
	int ret = -1;

	if (!format)
		goto out;
	const AVCodec *decoder = NULL;
	int index = av_find_best_stream(format, AVMEDIA_TYPE_AUDIO, -1, -1, &decoder, 0);
	if (index < 0)
		goto out;
	codec = avcodec_alloc_context3(decoder);
	if (avcodec_parameters_to_context(codec, format->streams[index]->codecpar) < 0 || avcodec_open2(codec, decoder, NULL) < 0)
		goto out;
	if (codec->sample_rate <= 0 || codec->ch_layout.nb_channels <= 0)
		goto out;
	if (codec->ch_layout.order == AV_CHANNEL_ORDER_UNSPEC)
		av_channel_layout_default(&codec->ch_layout, codec->ch_layout.nb_channels);
	sample->rate = codec->sample_rate;
	sample->channels = codec->ch_layout.nb_channels == 1 ? 1 : 2;
	AVChannelLayout out_layout;
	av_channel_layout_default(&out_layout, sample->channels);
	if (swr_alloc_set_opts2(&swr, &out_layout, AV_SAMPLE_FMT_S16, sample->rate, &codec->ch_layout, codec->sample_fmt, sample->rate, 0, NULL) < 0 || swr_init(swr) < 0)
		goto out;

	int frame_bytes = sample->channels * sizeof(int16_t);
	gboolean eof = FALSE;
	while (pcm->len < MAX_PCM_BYTES) {
		int r = avcodec_receive_frame(codec, frame);
		if (r == AVERROR_EOF)
			break;
		if (r == AVERROR(EAGAIN)) {
			if (eof)
				break;
			if (av_read_frame(format, packet) < 0) {
				avcodec_send_packet(codec, NULL);
				eof = TRUE;
				continue;
			}
			if (packet->stream_index == index)
				avcodec_send_packet(codec, packet);
			av_packet_unref(packet);
			continue;
		}
		if (r < 0)
			break;
		int frames = swr_get_out_samples(swr, frame->nb_samples);
		guint at = pcm->len;
		g_byte_array_set_size(pcm, at + frames * frame_bytes);
		uint8_t *dst = pcm->data + at;
		frames = swr_convert(swr, &dst, frames, (const uint8_t **)frame->extended_data, frame->nb_samples);
		g_byte_array_set_size(pcm, at + MAX(frames, 0) * frame_bytes);
		av_frame_unref(frame);
	}
	/* like AOSP: what does not fit into the heap is cut off */
	if (pcm->len > MAX_PCM_BYTES)
		g_byte_array_set_size(pcm, MAX_PCM_BYTES - MAX_PCM_BYTES % frame_bytes);
	sample->frames = pcm->len / frame_bytes;
	if (sample->frames > 0) {
		sample->data = (int16_t *)g_byte_array_free(pcm, FALSE);
		pcm = NULL;
		ret = 0;
	}

out:
	if (pcm)
		g_byte_array_free(pcm, TRUE);
	av_frame_free(&frame);
	av_packet_free(&packet);
	swr_free(&swr);
	avcodec_free_context(&codec);
	close_input(format, io);
	return ret;
}

/* decodes the sample (on the Java side's loader thread) and adds it to the pool: 0, or an error */
JNIEXPORT jint JNICALL Java_android_media_SoundPool_native_1decode(JNIEnv *env, jclass class, jlong pool_ptr, jint sample_id, jint fd, jlong offset, jlong length)
{
	struct pool *pool = _PTR(pool_ptr);
	struct stat st;
	int64_t size = fstat(fd, &st) == 0 && S_ISREG(st.st_mode) ? st.st_size - offset : INT64_MAX;
	struct fd_region region = {.fd = fd, .offset = offset, .length = (length <= 0 || length > size) ? size : length};
	struct sample *sample = g_new0(struct sample, 1);
	sample->refs = 1;

	if (decode(&region, sample) != 0) {
		g_free(sample);
		return -1;
	}
	g_mutex_lock(&pool->lock);
	g_hash_table_insert(pool->samples, GINT_TO_POINTER(sample_id), sample);
	g_mutex_unlock(&pool->lock);
	return 0;
}

/* AOSP: the streams that play it keep the sample until they end */
JNIEXPORT jboolean JNICALL Java_android_media_SoundPool_native_1unload(JNIEnv *env, jclass class, jlong pool_ptr, jint sample_id)
{
	struct pool *pool = _PTR(pool_ptr);
	g_mutex_lock(&pool->lock);
	gboolean found = g_hash_table_remove(pool->samples, GINT_TO_POINTER(sample_id));
	g_mutex_unlock(&pool->lock);
	return found;
}

/* AOSP StreamManager: a free stream, or else the one of the lowest priority (the oldest among equal ones)
 * if that is not higher than the new one's; otherwise the sound is not played (0) */
JNIEXPORT jint JNICALL Java_android_media_SoundPool_native_1play(JNIEnv *env, jclass class, jlong pool_ptr, jint sample_id, jfloat left, jfloat right, jint priority, jint loop, jfloat rate)
{
	struct pool *pool = _PTR(pool_ptr);
	int id = 0;
	g_mutex_lock(&pool->lock);
	struct sample *sample = g_hash_table_lookup(pool->samples, GINT_TO_POINTER(sample_id));
	if (!sample)
		goto out;
	struct stream *use = NULL;
	for (int i = 0; i < pool->max_streams; i++) {
		struct stream *s = &pool->streams[i];
		if (s->state == STREAM_IDLE) {
			use = s;
			break;
		}
		if (!use || s->priority < use->priority || (s->priority == use->priority && s->order < use->order))
			use = s;
	}
	if (use->state != STREAM_IDLE) {
		if (use->priority > priority)
			goto out;
		stream_free(use);
	}
	if (++pool->next_stream_id <= 0)
		pool->next_stream_id = 1;
	id = pool->next_stream_id;
	sample->refs++;
	*use = (struct stream){
		.state = STREAM_PLAYING,
		.id = id,
		.priority = priority,
		.order = pool->next_order++,
		.sample = sample,
		.rate = rate,
		.left = left,
		.right = right,
		.loop = loop,
	};
	g_cond_signal(&pool->cond);
out:
	g_mutex_unlock(&pool->lock);
	return id;
}

enum { CMD_PAUSE,
       CMD_RESUME,
       CMD_STOP,
       CMD_VOLUME,
       CMD_PRIORITY,
       CMD_LOOP,
       CMD_RATE };

/* a command for the stream streamID; a stream that has ended (or never was) ignores it, as in AOSP */
JNIEXPORT void JNICALL Java_android_media_SoundPool_native_1command(JNIEnv *env, jclass class, jlong pool_ptr, jint stream_id, jint cmd, jint i, jfloat a, jfloat b)
{
	struct pool *pool = _PTR(pool_ptr);
	g_mutex_lock(&pool->lock);
	struct stream *s = find_stream(pool, stream_id);
	if (s) {
		switch (cmd) {
			case CMD_PAUSE:
				if (s->state == STREAM_PLAYING)
					s->state = STREAM_PAUSED;
				break;
			case CMD_RESUME:
				if (s->state == STREAM_PAUSED)
					s->state = STREAM_PLAYING;
				s->auto_paused = FALSE;
				break;
			case CMD_STOP:
				stream_free(s);
				break;
			case CMD_VOLUME:
				s->left = a;
				s->right = b;
				break;
			case CMD_PRIORITY:
				s->priority = i;
				break;
			case CMD_LOOP:
				s->loop = i;
				break;
			case CMD_RATE:
				s->rate = a;
				break;
		}
		g_cond_signal(&pool->cond);
	}
	g_mutex_unlock(&pool->lock);
}

/* AOSP: autoPause() pauses the playing streams, autoResume() resumes those it paused */
JNIEXPORT void JNICALL Java_android_media_SoundPool_native_1autoPause(JNIEnv *env, jclass class, jlong pool_ptr, jboolean pause)
{
	struct pool *pool = _PTR(pool_ptr);
	g_mutex_lock(&pool->lock);
	for (int i = 0; i < pool->max_streams; i++) {
		struct stream *s = &pool->streams[i];
		if (pause && s->state == STREAM_PLAYING) {
			s->state = STREAM_PAUSED;
			s->auto_paused = TRUE;
		} else if (!pause && s->state == STREAM_PAUSED && s->auto_paused) {
			s->state = STREAM_PLAYING;
			s->auto_paused = FALSE;
		}
	}
	g_cond_signal(&pool->cond);
	g_mutex_unlock(&pool->lock);
}

JNIEXPORT void JNICALL Java_android_media_SoundPool_native_1release(JNIEnv *env, jclass class, jlong pool_ptr)
{
	struct pool *pool = _PTR(pool_ptr);
	g_mutex_lock(&pool->lock);
	pool->quit = TRUE;
	g_cond_signal(&pool->cond);
	g_mutex_unlock(&pool->lock);
	g_thread_join(pool->thread);
	for (int i = 0; i < pool->max_streams; i++)
		if (pool->streams[i].state != STREAM_IDLE)
			stream_free(&pool->streams[i]);
	g_hash_table_destroy(pool->samples);
	g_free(pool->streams);
	g_mutex_clear(&pool->lock);
	g_cond_clear(&pool->cond);
	g_free(pool);
}
