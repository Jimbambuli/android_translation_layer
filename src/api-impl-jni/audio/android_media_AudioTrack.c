#include <gtk/gtk.h>

#include <alsa/asoundlib.h>
#include <errno.h>
#include <math.h>
#include <string.h>
#include <poll.h>
#include <stdint.h>
#include <stdio.h>
#include <sys/eventfd.h>
#include <unistd.h>

#include "../defines.h"
#include "../util.h"
#include "audio.h"

#include "../generated_headers/android_media_AudioTrack.h"

/* AOSP audio_bytes_per_sample() of the linear PCM encodings ATL's output takes (android.media.AudioFormat values), else 0 */
static int bytes_per_sample(int encoding)
{
	switch (encoding) {
		case 3: // ENCODING_PCM_8BIT
			return 1;
		case 2: // ENCODING_PCM_16BIT
			return 2;
		case 21: // ENCODING_PCM_24BIT_PACKED
			return 3;
		case 4:  // ENCODING_PCM_FLOAT
		case 22: // ENCODING_PCM_32BIT
			return 4;
		default:
			return 0;
	}
}

/* AOSP: a track plays on the mixer output, which mixes it into the output's format; ATL's output is an ALSA stream of
 * PCM16, mono for a mono track and else stereo, the channel mask of AOSP's primary output. A compressed encoding (AC-3,
 * E-AC-3, DTS, IEC61937...) needs a direct or offloaded output, which ATL does not have: AudioPolicyManager finds no
 * output for it, AudioTrack::set() fails and the track stays STATE_UNINITIALIZED, like on a device without HDMI. */
JNIEXPORT void JNICALL Java_android_media_AudioTrack_native_1constructor(JNIEnv *env, jobject this, jint streamType, jint rate, jint channel_mask, jint channel_count, jint encoding, jint buffer_size, jint mode)
{

	snd_pcm_t *pcm_handle;
	snd_pcm_hw_params_t *params;

	unsigned int channels;
	unsigned int channels_out;
	unsigned int period_time;
	unsigned int buffer_time;

	int ret;

	if (!bytes_per_sample(encoding) || channel_count <= 0) {
		printf("AudioTrack: no output for encoding %d with %d channels\n", encoding, channel_count);
		return; // the track stays uninitialized (pcm_handle field stays NULL)
	}

	/* Open the PCM device in playback mode */
	ret = snd_pcm_open(&pcm_handle, PCM_DEVICE, SND_PCM_STREAM_PLAYBACK, 0);
	if (ret < 0) {
		printf("ERROR: Can't open \"%s\" PCM device. %s\n", PCM_DEVICE, snd_strerror(ret));
		return; // no usable PCM device: leave the track uninitialized (pcm_handle field stays NULL)
	}

	snd_pcm_hw_params_alloca(&params);
	helper_hw_params_init(pcm_handle, params, rate, channel_count == 1 ? 4 /* CHANNEL_OUT_MONO */ : 12 /* CHANNEL_OUT_STEREO */, SND_PCM_FORMAT_S16_LE, &channels);

	/*--↓*/
	snd_pcm_uframes_t buffer_size_as_uframes_t = buffer_size / (channel_count * bytes_per_sample(encoding)); // the track's frames
	snd_pcm_hw_params_set_buffer_size_near(pcm_handle, params, &buffer_size_as_uframes_t);
	/*--↑*/

	/* set the period time to 1/4 of the buffer time */
	ret = snd_pcm_hw_params_get_buffer_time(params, &buffer_time, NULL);
	period_time = buffer_time / 4;
	ret = snd_pcm_hw_params_set_period_time_near(pcm_handle, params, &period_time, NULL);

	/* Write parameters */
	ret = snd_pcm_hw_params(pcm_handle, params);
	if (ret < 0)
		printf("ERROR: Can't set harware parameters. %s\n", snd_strerror(ret));

	//snd_pcm_hw_params_free (hw_params);

	/*--↓*/
	snd_pcm_uframes_t period_size;

	ret = snd_pcm_hw_params_get_period_size(params, &period_size, 0);
	if (ret < 0)
		printf("Error calling snd_pcm_hw_params_get_period_size: %s\n", snd_strerror(ret));

	snd_pcm_sw_params_t *sw_params;

	snd_pcm_sw_params_malloc(&sw_params);
	snd_pcm_sw_params_current(pcm_handle, sw_params);

	snd_pcm_sw_params_set_start_threshold(pcm_handle, sw_params, (buffer_size_as_uframes_t / period_size) * period_size);
	snd_pcm_sw_params_set_avail_min(pcm_handle, sw_params, period_size);

	snd_pcm_sw_params(pcm_handle, sw_params);

	//snd_pcm_sw_params_free (sw_params);
	/*--↑*/

	/* Resume information */
	printf("PCM name: '%s'\n", snd_pcm_name(pcm_handle));

	printf("PCM state: %s\n", snd_pcm_state_name(snd_pcm_state(pcm_handle)));

	snd_pcm_hw_params_get_channels(params, &channels_out);
	printf("channels: %i ", channels_out);

	if (channels_out == 1)
		printf("(mono)\n");
	else if (channels_out == 2)
		printf("(stereo)\n");

	unsigned int tmp;
	snd_pcm_hw_params_get_rate(params, &tmp, 0);
	printf("rate: %d bps\n", tmp);

	snd_pcm_hw_params_get_period_time(params, &period_time, NULL);

	_SET_LONG_FIELD(this, "pcm_handle", _INTPTR(pcm_handle));
	_SET_LONG_FIELD(this, "params", _INTPTR(params));
	_SET_INT_FIELD(this, "channels", channels_out);
	snd_pcm_hw_params_get_buffer_size(params, &buffer_size_as_uframes_t);
	_SET_INT_FIELD(this, "bufferSizeInFrames", buffer_size_as_uframes_t);
	_SET_INT_FIELD(this, "period_time", period_time);
	/* AOSP ClientProxy::interrupt(): pause() and stop() wake a write() that waits for room */
	_SET_INT_FIELD(this, "interrupt_fd", eventfd(0, EFD_NONBLOCK | EFD_CLOEXEC));
}

struct jni_callback_data {
	JavaVM *jvm;
	jobject this;
	jclass this_class;
	jint period_time;
};
void periodic_update_callback(snd_async_handler_t *pcm_callback)
{
	struct jni_callback_data *d = snd_async_handler_get_callback_private(pcm_callback);
	int getenv_ret;
	int attach_ret = -1;

	//	printf("periodic_update_callback called!\n");

	JNIEnv *env;
	getenv_ret = (*d->jvm)->GetEnv(d->jvm, (void **)&env, JNI_VERSION_1_6);

	//printf("!!!! GetEnv: %p getenv_ret: %d\n",env, getenv_ret);
	if (getenv_ret == JNI_EDETACHED) {
		printf("!!!! JNI_EDETACHED\n");
		attach_ret = (*d->jvm)->AttachCurrentThread(d->jvm, (void **)&env, NULL);
		// TODO error checking
	}

	/* the listener set now (setPlaybackPositionUpdateListener() may come after play()) */
	jobject listener = _GET_OBJ_FIELD(d->this, "periodic_update_listener", "Landroid/media/AudioTrack$OnPlaybackPositionUpdateListener;");
	if (listener)
		J__AudioTrack__OnPlaybackPositionUpdateListener__onPeriodicNotification(env, listener, d->this);

	if ((*env)->ExceptionCheck(env)) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}

	if (attach_ret == JNI_OK) // if we (succesfully) attached a thread, we should probably detach it now
		(*d->jvm)->DetachCurrentThread(d->jvm);

	// microseconds to milliseconds
	//g_timeout_add (d->period_time / 1000 - 2, G_SOURCE_FUNC(helper_loop), d);
	//return G_SOURCE_REMOVE;
}

JNIEXPORT void JNICALL Java_android_media_AudioTrack_native_1play(JNIEnv *env, jobject this)
{
	snd_pcm_t *pcm_handle = _PTR(_GET_LONG_FIELD(this, "pcm_handle"));
	if (!pcm_handle)
		return; // uninitialized track (no audio device): nothing to start

	/* one callback for the life of the track, like AOSP's AudioTrackThread (created with the track, ended by
	 * release()); the first play() sets it up */
	if (!_GET_LONG_FIELD(this, "async_handler")) {
		JavaVM *jvm;
		(*env)->GetJavaVM(env, &jvm);

		struct jni_callback_data *callback_data = malloc(sizeof(struct jni_callback_data));
		callback_data->jvm = jvm;
		callback_data->this = _REF(this);
		callback_data->this_class = _REF(_CLASS(this));
		callback_data->period_time = _GET_INT_FIELD(this, "period_time");

		snd_async_handler_t *pcm_callback;
		if (snd_async_add_pcm_handler(&pcm_callback, pcm_handle, periodic_update_callback, callback_data) == 0) {
			_SET_LONG_FIELD(this, "async_handler", _INTPTR(pcm_callback));
		} else {
			_UNREF(callback_data->this);
			_UNREF(callback_data->this_class);
			free(callback_data);
		}
	}

	/*--↓*/
	snd_pcm_start(pcm_handle);
	snd_pcm_pause(pcm_handle, FALSE);
	/*--↑*/
}

/* AOSP ClientProxy::interrupt(): wakes a blocking write() (pause(), stop(), release()) */
static void interrupt_write(JNIEnv *env, jobject this)
{
	int fd = _GET_INT_FIELD(this, "interrupt_fd");
	if (fd > 0)
		eventfd_write(fd, 1);
}

/* waits until the PCM has room; FALSE when interrupt_write() came first */
static gboolean wait_for_room(snd_pcm_t *pcm_handle, int interrupt_fd)
{
	int count = snd_pcm_poll_descriptors_count(pcm_handle);
	if (count <= 0)
		return FALSE;
	struct pollfd fds[count + 1];
	snd_pcm_poll_descriptors(pcm_handle, fds, count);
	fds[count] = (struct pollfd){.fd = interrupt_fd, .events = POLLIN};
	while (poll(fds, count + 1, -1) < 0) {
		if (errno != EINTR)
			return FALSE;
	}
	if (fds[count].revents & POLLIN) {
		eventfd_t value;
		eventfd_read(interrupt_fd, &value);
		return FALSE;
	}
	unsigned short revents;
	snd_pcm_poll_descriptors_revents(pcm_handle, fds, count, &revents);
	return TRUE;
}

/* AOSP audio_utils ChannelMix fillChannelMatrix() for AUDIO_CHANNEL_OUT_STEREO: the weights of a channel (one bit of a
 * position mask, android.media.AudioFormat values) in the left and the right output */
static void stereo_weights(int channel, int mask, float *left, float *right)
{
	const float COEF_25 = 0.2508909536f;
	const float COEF_35 = 0.3543928915f;
	const float COEF_36 = 0.3552343859f;
	const float COEF_61 = 0.6057043428f;
	const float MINUS_3_DB_IN_FLOAT = M_SQRT1_2;
	*left = *right = 0.f;
	switch (channel) {
		case 0x4:       // FRONT_LEFT
		case 0x4000:    // TOP_FRONT_LEFT
		case 0x400000:  // BOTTOM_FRONT_LEFT
			*left = 1.f;
			break;
		case 0x800:     // SIDE_LEFT
		case 0x40:      // BACK_LEFT
		case 0x20000:   // TOP_BACK_LEFT
		case 0x4000000: // FRONT_WIDE_LEFT
			*left = MINUS_3_DB_IN_FLOAT;
			break;
		case 0x8:       // FRONT_RIGHT
		case 0x10000:   // TOP_FRONT_RIGHT
		case 0x1000000: // BOTTOM_FRONT_RIGHT
			*right = 1.f;
			break;
		case 0x1000:    // SIDE_RIGHT
		case 0x80:      // BACK_RIGHT
		case 0x80000:   // TOP_BACK_RIGHT
		case 0x8000000: // FRONT_WIDE_RIGHT
			*right = MINUS_3_DB_IN_FLOAT;
			break;
		case 0x10:      // FRONT_CENTER
		case 0x8000:    // TOP_FRONT_CENTER
		case 0x800000:  // BOTTOM_FRONT_CENTER
			*left = *right = MINUS_3_DB_IN_FLOAT;
			break;
		case 0x100000:  // TOP_SIDE_LEFT
			*left = COEF_61;
			break;
		case 0x200000:  // TOP_SIDE_RIGHT
			*right = COEF_61;
			break;
		case 0x100:     // FRONT_LEFT_OF_CENTER
			*left = COEF_61;
			*right = COEF_25;
			break;
		case 0x200:     // FRONT_RIGHT_OF_CENTER
			*left = COEF_25;
			*right = COEF_61;
			break;
		case 0x2000:    // TOP_CENTER
			*left = *right = COEF_36;
			break;
		case 0x40000:   // TOP_BACK_CENTER
			*left = *right = COEF_35;
			break;
		case 0x2000000: // LOW_FREQUENCY_2
			*right = MINUS_3_DB_IN_FLOAT;
			break;
		case 0x20:      // LOW_FREQUENCY
			if (mask & 0x2000000) {
				*left = MINUS_3_DB_IN_FLOAT;
				break;
			}
			/* fall through */
		case 0x400:     // BACK_CENTER
			*left = *right = 0.5f;
			break;
	}
}

/* sample i of the track's data as float (AOSP memcpy_to_float_from_*) */
static float sample_to_float(const void *data, int encoding, int i)
{
	switch (encoding) {
		case 3: // ENCODING_PCM_8BIT
			return (((const uint8_t *)data)[i] - 128) / 128.f;
		case 21: { // ENCODING_PCM_24BIT_PACKED
			const uint8_t *p = (const uint8_t *)data + i * 3;
			return (int32_t)((uint32_t)p[0] << 8 | (uint32_t)p[1] << 16 | (uint32_t)p[2] << 24) / 2147483648.f;
		}
		case 22: // ENCODING_PCM_32BIT
			return ((const int32_t *)data)[i] / 2147483648.f;
		case 4: // ENCODING_PCM_FLOAT
			return ((const float *)data)[i];
		default: // ENCODING_PCM_16BIT
			return ((const int16_t *)data)[i] / 32768.f;
	}
}

/* AOSP AudioMixer: the track's samples in float, a multichannel track downmixed to the stereo output by the matrix of
 * ChannelMix (clamped to +3 dB like ChannelMix's clamp()), the volume applied, then PCM16 of the output (clamp16_from_float). A mono or stereo track keeps its channels (the ALSA stream has
 * as many). */
static void mix_to_output(jshort *out, int out_channels, const void *data, int encoding, int track_channels, int mask, int frames, float volume)
{
	float *mixed = malloc(frames * out_channels * sizeof(float));
	if (!mixed) {
		memset(out, 0, frames * out_channels * sizeof(jshort));
		return;
	}
	float weights[32][2];
	gboolean downmix = track_channels != out_channels;
	if (downmix) {
		int index = 0;
		for (unsigned tmp = mask; tmp != 0 && index < 32; ++index) {
			unsigned lowest = tmp & -tmp;
			stereo_weights(lowest, mask, &weights[index][0], &weights[index][1]);
			tmp ^= lowest;
		}
		for (; index < 32; index++)
			weights[index][0] = weights[index][1] = 0.f;
	}
	for (int f = 0; f < frames; f++) {
		for (int c = 0; c < out_channels; c++) {
			float v;
			if (downmix) {
				v = 0.f;
				for (int t = 0; t < track_channels && t < 32; t++)
					v += sample_to_float(data, encoding, f * track_channels + t) * weights[t][c];
				v = fminf(fmaxf(v, -M_SQRT2), M_SQRT2);
			} else {
				v = sample_to_float(data, encoding, f * track_channels + c);
			}
			mixed[f * out_channels + c] = v * volume;
		}
	}
	for (int i = 0; i < frames * out_channels; i++) {
		float v = mixed[i];
		out[i] = v >= 1.f ? 32767 : v <= -1.f ? -32768 : (jshort)lrintf(v * 32768.f);
	}
	free(mixed);
}

/* blocking: AOSP WRITE_BLOCKING on a playing track; otherwise what fits now, 0 when the buffer is full */
static int write_frames(JNIEnv *env, jobject this, snd_pcm_t *pcm_handle, const void *buffer, int frames_to_write, float volume, jboolean blocking)
{
	if (!pcm_handle)
		return -1; // uninitialized track (no audio device): report a write error
	gboolean playing = _GET_INT_FIELD(this, "playbackState") == 3 /* PLAYSTATE_PLAYING */;
	/* AOSP: what is written before play() (or after flush() while paused) waits for play(). A PCM that was not
	 * started since it was prepared starts by itself once enough frames are queued (ALSA: start_threshold, the
	 * pulse plugin: its prebuf, buffer size less one period), so until play() it takes at most two periods less
	 * than the buffer (a short count, as AOSP's write() returns for a stopped or paused track) */
	if (!playing && snd_pcm_state(pcm_handle) == SND_PCM_STATE_PREPARED) {
		snd_pcm_uframes_t buffer_size, period_size;
		snd_pcm_sframes_t avail = snd_pcm_avail(pcm_handle);
		if (snd_pcm_get_params(pcm_handle, &buffer_size, &period_size) < 0 || avail < 0)
			return 0;
		snd_pcm_sframes_t room = (snd_pcm_sframes_t)(buffer_size - 2 * period_size) - (snd_pcm_sframes_t)(buffer_size - avail);
		if (room <= 0)
			return 0;
		if (frames_to_write > room)
			frames_to_write = room;
	}
	/* the conversion goes into a copy: the app's data stays as it is (after a short write it comes again) */
	jshort *scaled = NULL;
	int encoding = _GET_INT_FIELD(this, "encoding");
	int track_channels = _GET_INT_FIELD(this, "channelCount");
	int out_channels = _GET_INT_FIELD(this, "channels");
	if (volume != 1.f || encoding != 2 /* ENCODING_PCM_16BIT */ || track_channels != out_channels) {
		scaled = malloc(frames_to_write * out_channels * sizeof(jshort));
		if (!scaled)
			return 0;
		mix_to_output(scaled, out_channels, buffer, encoding, track_channels, _GET_INT_FIELD(this, "channelMask"), frames_to_write, volume);
		buffer = scaled;
	}
	/* AOSP AudioTrack::write(): a blocking write waits for room until all is queued, but pause(), stop() or
	 * release() from another thread interrupt the wait; it returns what was queued so far (0 if nothing). An
	 * interrupt that came before the write does not count (ClientProxy::obtainBuffer(): ignoreInitialPendingInterrupt) */
	int interrupt_fd = _GET_INT_FIELD(this, "interrupt_fd");
	if (blocking && interrupt_fd > 0) {
		eventfd_t value;
		eventfd_read(interrupt_fd, &value);
	}
	int frame_size = _GET_INT_FIELD(this, "channels") * 2; // 2 means PCM16
	snd_pcm_sframes_t frames_written = 0;
	snd_pcm_nonblock(pcm_handle, 1);
	while (frames_written < frames_to_write) {
		snd_pcm_sframes_t ret = snd_pcm_writei(pcm_handle, (const char *)buffer + frames_written * frame_size, frames_to_write - frames_written);
		if (ret == -EPIPE) {
			printf("XRUN.\n");
			_SET_INT_FIELD(this, "underrunCount", _GET_INT_FIELD(this, "underrunCount") + 1); // AudioTrack.getUnderrunCount()
			snd_pcm_recover(pcm_handle, ret, 0);
			ret = snd_pcm_writei(pcm_handle, (const char *)buffer + frames_written * frame_size, frames_to_write - frames_written);
			if (playing)
				snd_pcm_start(pcm_handle); // a paused or stopped track waits for play()
		}
		if (ret > 0) {
			frames_written += ret;
		} else if (ret == -EAGAIN || ret == 0) {
			// the buffer is full: non-blocking returns, blocking waits unless interrupted
			if (!blocking || interrupt_fd <= 0 || !wait_for_room(pcm_handle, interrupt_fd))
				break;
		} else {
			printf("ERROR. Can't write to PCM device. %s\n", snd_strerror(ret));
			break;
		}
	}
	snd_pcm_nonblock(pcm_handle, 0);
	free(scaled);
	return frames_written;
}

JNIEXPORT jint JNICALL Java_android_media_AudioTrack_native_1write___3BIIFZ(JNIEnv *env, jobject this, jbyteArray audio_data, jint offset_in_bytes, jint frames_to_write, jfloat volume, jboolean blocking)
{
	snd_pcm_t *pcm_handle = _PTR(_GET_LONG_FIELD(this, "pcm_handle"));

	jbyte *buffer = _GET_BYTE_ARRAY_ELEMENTS(audio_data);
	snd_pcm_sframes_t frames_written = write_frames(env, this, pcm_handle, buffer + offset_in_bytes, frames_to_write, volume, blocking);
	_RELEASE_BYTE_ARRAY_ELEMENTS(audio_data, buffer);

	return frames_written;
}

JNIEXPORT jint JNICALL Java_android_media_AudioTrack_native_1write___3SIIFZ(JNIEnv *env, jobject this, jshortArray audio_data, jint offset_in_shorts, jint frames_to_write, jfloat volume, jboolean blocking)
{
	snd_pcm_t *pcm_handle = _PTR(_GET_LONG_FIELD(this, "pcm_handle"));

	jshort *buffer = (*env)->GetShortArrayElements(env, audio_data, NULL);
	snd_pcm_sframes_t frames_written = write_frames(env, this, pcm_handle, buffer + offset_in_shorts, frames_to_write, volume, blocking);
	(*env)->ReleaseShortArrayElements(env, audio_data, buffer, 0);

	return frames_written;
}

JNIEXPORT jint JNICALL Java_android_media_AudioTrack_native_1write___3FIIFZ(JNIEnv *env, jobject this, jfloatArray audio_data, jint offset_in_floats, jint frames_to_write, jfloat volume, jboolean blocking)
{
	snd_pcm_t *pcm_handle = _PTR(_GET_LONG_FIELD(this, "pcm_handle"));

	jfloat *buffer = (*env)->GetFloatArrayElements(env, audio_data, NULL);
	snd_pcm_sframes_t frames_written = write_frames(env, this, pcm_handle, buffer + offset_in_floats, frames_to_write, volume, blocking);
	(*env)->ReleaseFloatArrayElements(env, audio_data, buffer, JNI_ABORT);

	return frames_written;
}

JNIEXPORT void JNICALL Java_android_media_AudioTrack_native_1pause(JNIEnv *env, jobject this)
{
	snd_pcm_t *pcm_handle = _PTR(_GET_LONG_FIELD(this, "pcm_handle"));
	if (!pcm_handle)
		return; // uninitialized track: nothing to pause
	snd_pcm_pause(pcm_handle, TRUE);
	interrupt_write(env, this);
}

/* AOSP AudioTrack::stop(): interrupts a blocking write(); what was written still plays out (ATL's stop()) */
JNIEXPORT void JNICALL Java_android_media_AudioTrack_native_1stop(JNIEnv *env, jobject this)
{
	if (_GET_LONG_FIELD(this, "pcm_handle"))
		interrupt_write(env, this);
}

JNIEXPORT void JNICALL Java_android_media_AudioTrack_native_1flush(JNIEnv *env, jobject this)
{
	snd_pcm_t *pcm_handle = _PTR(_GET_LONG_FIELD(this, "pcm_handle"));
	if (!pcm_handle)
		return; // uninitialized track: nothing to flush
	snd_pcm_drop(pcm_handle);
	snd_pcm_prepare(pcm_handle);
}

JNIEXPORT void JNICALL Java_android_media_AudioTrack_native_1release(JNIEnv *env, jobject this)
{
	snd_pcm_t *pcm_handle = _PTR(_GET_LONG_FIELD(this, "pcm_handle"));
	if (!pcm_handle)
		return; // uninitialized track: nothing to release
	interrupt_write(env, this);
	snd_async_handler_t *pcm_callback = _PTR(_GET_LONG_FIELD(this, "async_handler"));
	if (pcm_callback) {
		struct jni_callback_data *callback_data = snd_async_handler_get_callback_private(pcm_callback);
		snd_async_del_handler(pcm_callback);
		_UNREF(callback_data->this);
		_UNREF(callback_data->this_class);
		free(callback_data);
		_SET_LONG_FIELD(this, "async_handler", 0);
	}
	snd_pcm_close(pcm_handle);
	close(_GET_INT_FIELD(this, "interrupt_fd"));
	_SET_INT_FIELD(this, "interrupt_fd", -1);
}

JNIEXPORT jint JNICALL Java_android_media_AudioTrack_native_1getPlaybackHeadPosition(JNIEnv *env, jobject this)
{
	snd_pcm_t *pcm_handle = _PTR(_GET_LONG_FIELD(this, "pcm_handle"));
	if (!pcm_handle)
		return 0; // uninitialized track: no playback position
	snd_pcm_sframes_t delay;
	if (snd_pcm_delay(pcm_handle, &delay) < 0)
		return 0; // e.g. right after flush(): nothing queued

	return delay;
}
