package android.media;

import android.atl.annotation.Export;
import java.nio.ByteBuffer;

public class AudioTrack implements AudioRouting {
	public interface OnPlaybackPositionUpdateListener {
		void onMarkerReached(AudioTrack track);

		@Export
		void onPeriodicNotification(AudioTrack track);
	}

	public static final int SUCCESS = 0;
	public static final int ERROR = -1;
	public static final int ERROR_BAD_VALUE = -2; // basically EINVAL
	public static final int ERROR_INVALID_OPERATION = -3;
	public static final int ERROR_DEAD_OBJECT = -6;

	/** AOSP: write() returns when all of the data is written; ATL's output blocks, so this is also what WRITE_NON_BLOCKING does */
	public static final int WRITE_BLOCKING = 0;
	public static final int WRITE_NON_BLOCKING = 1;

	public static final int STATE_UNINITIALIZED = 0;
	public static final int STATE_INITIALIZED = 1;
	public static final int STATE_NO_STATIC_DATA = 2;

	public static final int MODE_STATIC = 0;
	public static final int MODE_STREAM = 1;

	public static final int ENCAPSULATION_MODE_NONE = 0;
	public static final int ENCAPSULATION_MODE_ELEMENTARY_STREAM = 1;
	public static final int ENCAPSULATION_MODE_HANDLE = 2;

	public static final int ENCAPSULATION_METADATA_TYPE_NONE = 0;
	public static final int ENCAPSULATION_METADATA_TYPE_FRAMEWORK_TUNER = 1;
	public static final int ENCAPSULATION_METADATA_TYPE_DVB_AD_DESCRIPTOR = 2;
	public static final int ENCAPSULATION_METADATA_TYPE_SUPPLEMENTARY_AUDIO_PLACEMENT = 3;

	public static final int SUPPLEMENTARY_AUDIO_PLACEMENT_NORMAL = 0;
	public static final int SUPPLEMENTARY_AUDIO_PLACEMENT_LEFT = 1;
	public static final int SUPPLEMENTARY_AUDIO_PLACEMENT_RIGHT = 2;

	public static final int DUAL_MONO_MODE_OFF = 0;
	public static final int DUAL_MONO_MODE_LR = 1;
	public static final int DUAL_MONO_MODE_LL = 2;
	public static final int DUAL_MONO_MODE_RR = 3;

	public static final int PERFORMANCE_MODE_NONE = 0;
	public static final int PERFORMANCE_MODE_LOW_LATENCY = 1;
	public static final int PERFORMANCE_MODE_POWER_SAVING = 2;

	public static final int PLAYSTATE_STOPPED = 1;
	public static final int PLAYSTATE_PAUSED = 2;
	public static final int PLAYSTATE_PLAYING = 3;

	int streamType;
	int sampleRateInHz;
	int channelConfig;
	int audioFormat;
	int bufferSizeInBytes;
	int mode;
	private int sessionId;
	private int playbackState = PLAYSTATE_STOPPED;
	private int playbackHeadPosition = 0;
	private float volume = 1.f;
	private AudioAttributes mConfiguredAudioAttributes;
	private android.media.metrics.LogSessionId mLogSessionId = android.media.metrics.LogSessionId.LOG_SESSION_ID_NONE;
	private int mMarkerPosition;
	private int mPlaybackRate;
	private int mStateOverride = -1; // setState()
	private final java.util.concurrent.ConcurrentHashMap<OnCodecFormatChangedListener, java.util.concurrent.Executor> mCodecFormatChangedListeners =
	    new java.util.concurrent.ConcurrentHashMap<>();

	/* AOSP mChannelCount, mChannelMask (a position mask, the legacy configurations mapped) and mAudioFormat */
	int channelCount;
	int channelMask;
	int encoding;

	// for native code's use
	long pcm_handle;
	long params;
	int channels; // of the ALSA stream: mono, else stereo (the mixer output, see native_constructor)
	int period_time;
	int bufferSizeInFrames; // the ALSA buffer
	int underrunCount;      // ALSA underruns (EPIPE) the writes recovered from
	long async_handler;     // the periodic callback, set up by the first play(), removed by release()
	int interrupt_fd;       // wakes a blocking write() on pause(), stop() and release()
	private int mPositionNotificationPeriod; // AudioTrack::setPositionUpdatePeriod()
	OnPlaybackPositionUpdateListener periodic_update_listener;

	native void native_constructor(int streamType, int sampleRateInHz, int channelMask, int channelCount, int encoding, int bufferSizeInBytes, int mode);
	public AudioTrack(int streamType, int sampleRateInHz, int channelConfig, int audioFormat, int bufferSizeInBytes, int mode) {
		/* AOSP audioParamCheck(): SAMPLE_RATE_UNSPECIFIED plays at the output's rate */
		if (sampleRateInHz == AudioFormat.SAMPLE_RATE_UNSPECIFIED)
			sampleRateInHz = getNativeOutputSampleRate(streamType);
		this.streamType = streamType;
		this.sampleRateInHz = sampleRateInHz;
		this.channelConfig = channelConfig;
		this.audioFormat = audioFormat;
		this.bufferSizeInBytes = bufferSizeInBytes;
		this.mode = mode;
		/* AOSP audioParamCheck(): the legacy configurations stand for mono and stereo, any other mask for itself */
		switch (channelConfig) {
			case AudioFormat.CHANNEL_OUT_DEFAULT:
			case AudioFormat.CHANNEL_OUT_MONO:
			case AudioFormat.CHANNEL_CONFIGURATION_MONO:
				channelCount = 1;
				channelMask = AudioFormat.CHANNEL_OUT_MONO;
				break;
			case AudioFormat.CHANNEL_OUT_STEREO:
			case AudioFormat.CHANNEL_CONFIGURATION_STEREO:
				channelCount = 2;
				channelMask = AudioFormat.CHANNEL_OUT_STEREO;
				break;
			default:
				channelCount = AudioFormat.channelCountFromOutChannelMask(channelConfig);
				channelMask = channelConfig;
		}
		encoding = getAudioFormat();
		sessionId = AudioManager.newAudioSessionId(); // AOSP: AUDIO_SESSION_ALLOCATE

		/* AOSP AudioTrack(int, ...): the attributes of the legacy stream type */
		mConfiguredAudioAttributes = new AudioAttributes.Builder().setLegacyStreamType(streamType).build();
		mPlaybackRate = sampleRateInHz;
		System.out.println("\n\n\nAudioTrack(" + streamType + ", " + sampleRateInHz + ", " + channelConfig + ", " + audioFormat + ", " + bufferSizeInBytes + ", " + mode + "); called\n\n\n\n");
		native_constructor(streamType, sampleRateInHz, channelMask, channelCount, encoding, bufferSizeInBytes, mode);
	}

	public AudioTrack(int streamType, int sampleRateInHz, int channelConfig, int audioFormat, int bufferSizeInBytes, int mode, int sessionId) {
		this(streamType, sampleRateInHz, channelConfig, audioFormat, bufferSizeInBytes, mode);
		if (sessionId != 0) // AOSP AUDIO_SESSION_ALLOCATE (0): the track gets a new session (the 6-argument constructor's)
			this.sessionId = sessionId;
	}

	public AudioTrack(AudioAttributes attributes, AudioFormat format, int bufferSizeInBytes, int mode, int sessionId) {
		this(AudioAttributes.toLegacyStreamType(attributes), format.sampleRate, format.channelMask, format.encoding, bufferSizeInBytes, mode, sessionId);
		mConfiguredAudioAttributes = attributes;
	}

	/* AOSP AudioTrack.getMinBufferSize() */
	static public int getMinBufferSize(int sampleRateInHz, int channelConfig, int audioFormat) {
		int channelCount = 0;
		switch (channelConfig) {
			case AudioFormat.CHANNEL_OUT_MONO:
			case AudioFormat.CHANNEL_CONFIGURATION_MONO:
				channelCount = 1;
				break;
			case AudioFormat.CHANNEL_OUT_STEREO:
			case AudioFormat.CHANNEL_CONFIGURATION_STEREO:
				channelCount = 2;
				break;
			default:
				if (!isMultichannelConfigSupported(channelConfig, audioFormat)) {
					System.err.println("AudioTrack: getMinBufferSize(): Invalid channel configuration.");
					return ERROR_BAD_VALUE;
				} else {
					channelCount = AudioFormat.channelCountFromOutChannelMask(channelConfig);
				}
		}

		if (!AudioFormat.isPublicEncoding(audioFormat)) {
			System.err.println("AudioTrack: getMinBufferSize(): Invalid audio format.");
			return ERROR_BAD_VALUE;
		}

		// Note: AudioFormat.SAMPLE_RATE_UNSPECIFIED is not allowed
		if ((sampleRateInHz < AudioFormat.SAMPLE_RATE_HZ_MIN) || (sampleRateInHz > AudioFormat.SAMPLE_RATE_HZ_MAX)) {
			System.err.println("AudioTrack: getMinBufferSize(): " + sampleRateInHz + " Hz is not a supported sample rate.");
			return ERROR_BAD_VALUE;
		}

		int size = native_get_min_buff_size(sampleRateInHz, channelCount, audioFormat);
		if (size <= 0) {
			System.err.println("AudioTrack: getMinBufferSize(): error querying hardware");
			return ERROR;
		} else {
			return size;
		}
	}

	/* AOSP android_media_AudioTrack_get_min_buff_size(): AudioTrack::getMinFrameCount() frames of the track's frame
	 * size, or the frame count itself for a compressed encoding. getMinFrameCount() is
	 * AudioSystem::calculateMinFrameCount() over the output's latency, frame count and rate, which ATL's output reports
	 * as AudioManager.getOutputLatency() and the properties OUTPUT_FRAMES_PER_BUFFER and OUTPUT_SAMPLE_RATE. */
	private static int native_get_min_buff_size(int sampleRateInHz, int channelCount, int audioFormat) {
		int afLatencyMs = AudioManager.OUTPUT_LATENCY_MS;
		int afFrameCount = Integer.parseInt(AudioManager.OUTPUT_FRAMES_PER_BUFFER);
		int afSampleRate = Integer.parseInt(AudioManager.OUTPUT_SAMPLE_RATE);
		int minBufCount = afLatencyMs / ((1000 * afFrameCount) / afSampleRate);
		if (minBufCount < 2)
			minBufCount = 2;
		/* AudioResamplerPublic.h sourceFramesNeededWithTimestretch() at speed 1.0 */
		long required = sampleRateInHz == afSampleRate ? afFrameCount : (long)afFrameCount * sampleRateInHz / afSampleRate + 1 + 1;
		long frameCount = minBufCount * (required + 1 + 1);
		int bytesPerSample = bytesPerSample(audioFormat);
		return (int)(bytesPerSample > 0 ? frameCount * channelCount * bytesPerSample : frameCount);
	}

	/* AOSP audio_bytes_per_sample() for the encodings with proportional frames (linear PCM), else 0 */
	private static int bytesPerSample(int encoding) {
		switch (encoding) {
			case AudioFormat.ENCODING_PCM_8BIT:
				return 1;
			case AudioFormat.ENCODING_PCM_16BIT:
			case AudioFormat.ENCODING_IEC61937:
				return 2;
			case AudioFormat.ENCODING_PCM_24BIT_PACKED:
				return 3;
			case AudioFormat.ENCODING_PCM_FLOAT:
			case AudioFormat.ENCODING_PCM_32BIT:
				return 4;
			default:
				return 0;
		}
	}

	private static final int SUPPORTED_OUT_CHANNELS =
	    AudioFormat.CHANNEL_OUT_FRONT_LEFT |
	    AudioFormat.CHANNEL_OUT_FRONT_RIGHT |
	    AudioFormat.CHANNEL_OUT_FRONT_CENTER |
	    AudioFormat.CHANNEL_OUT_LOW_FREQUENCY |
	    AudioFormat.CHANNEL_OUT_BACK_LEFT |
	    AudioFormat.CHANNEL_OUT_BACK_RIGHT |
	    AudioFormat.CHANNEL_OUT_FRONT_LEFT_OF_CENTER |
	    AudioFormat.CHANNEL_OUT_FRONT_RIGHT_OF_CENTER |
	    AudioFormat.CHANNEL_OUT_BACK_CENTER |
	    AudioFormat.CHANNEL_OUT_SIDE_LEFT |
	    AudioFormat.CHANNEL_OUT_SIDE_RIGHT |
	    AudioFormat.CHANNEL_OUT_TOP_CENTER |
	    AudioFormat.CHANNEL_OUT_TOP_FRONT_LEFT |
	    AudioFormat.CHANNEL_OUT_TOP_FRONT_CENTER |
	    AudioFormat.CHANNEL_OUT_TOP_FRONT_RIGHT |
	    AudioFormat.CHANNEL_OUT_TOP_BACK_LEFT |
	    AudioFormat.CHANNEL_OUT_TOP_BACK_CENTER |
	    AudioFormat.CHANNEL_OUT_TOP_BACK_RIGHT |
	    AudioFormat.CHANNEL_OUT_TOP_SIDE_LEFT |
	    AudioFormat.CHANNEL_OUT_TOP_SIDE_RIGHT |
	    AudioFormat.CHANNEL_OUT_BOTTOM_FRONT_LEFT |
	    AudioFormat.CHANNEL_OUT_BOTTOM_FRONT_CENTER |
	    AudioFormat.CHANNEL_OUT_BOTTOM_FRONT_RIGHT |
	    AudioFormat.CHANNEL_OUT_LOW_FREQUENCY_2 |
	    AudioFormat.CHANNEL_OUT_FRONT_WIDE_LEFT |
	    AudioFormat.CHANNEL_OUT_FRONT_WIDE_RIGHT;

	/* AOSP AudioTrack.CHANNEL_PAIR_MAP: the channels that only come in pairs */
	private static final int[] CHANNEL_PAIRS = {
	    AudioFormat.CHANNEL_OUT_FRONT_LEFT | AudioFormat.CHANNEL_OUT_FRONT_RIGHT,
	    AudioFormat.CHANNEL_OUT_BACK_LEFT | AudioFormat.CHANNEL_OUT_BACK_RIGHT,
	    AudioFormat.CHANNEL_OUT_FRONT_LEFT_OF_CENTER | AudioFormat.CHANNEL_OUT_FRONT_RIGHT_OF_CENTER,
	    AudioFormat.CHANNEL_OUT_SIDE_LEFT | AudioFormat.CHANNEL_OUT_SIDE_RIGHT,
	    AudioFormat.CHANNEL_OUT_TOP_FRONT_LEFT | AudioFormat.CHANNEL_OUT_TOP_FRONT_RIGHT,
	    AudioFormat.CHANNEL_OUT_TOP_BACK_LEFT | AudioFormat.CHANNEL_OUT_TOP_BACK_RIGHT,
	    AudioFormat.CHANNEL_OUT_TOP_SIDE_LEFT | AudioFormat.CHANNEL_OUT_TOP_SIDE_RIGHT,
	    AudioFormat.CHANNEL_OUT_BOTTOM_FRONT_LEFT | AudioFormat.CHANNEL_OUT_BOTTOM_FRONT_RIGHT,
	    AudioFormat.CHANNEL_OUT_FRONT_WIDE_LEFT | AudioFormat.CHANNEL_OUT_FRONT_WIDE_RIGHT};

	/* AOSP AudioSystem.OUT_CHANNEL_COUNT_MAX (native_getMaxChannelCount(): FCC_LIMIT, FCC_12 in system/audio.h) and FCC_24 */
	private static final int OUT_CHANNEL_COUNT_MAX = 12;
	private static final int FCC_24 = 24;

	/* AOSP AudioTrack.isMultichannelConfigSupported() */
	private static boolean isMultichannelConfigSupported(int channelConfig, int encoding) {
		if ((channelConfig & SUPPORTED_OUT_CHANNELS) != channelConfig)
			return false;
		final int channelCount = AudioFormat.channelCountFromOutChannelMask(channelConfig);
		final int channelCountLimit;
		try {
			channelCountLimit = AudioFormat.isEncodingLinearFrames(encoding) ? OUT_CHANNEL_COUNT_MAX : FCC_24;
		} catch (IllegalArgumentException iae) {
			return false;
		}
		if (channelCount > channelCountLimit)
			return false;
		final int frontPair = AudioFormat.CHANNEL_OUT_FRONT_LEFT | AudioFormat.CHANNEL_OUT_FRONT_RIGHT;
		if ((channelConfig & frontPair) != frontPair)
			return false;
		for (int positionPair : CHANNEL_PAIRS) {
			if ((channelConfig & positionPair) != 0 && (channelConfig & positionPair) != positionPair)
				return false;
		}
		return true;
	}

	public void setPlaybackPositionUpdateListener(OnPlaybackPositionUpdateListener listener) {
		this.periodic_update_listener = listener;
	}

	/* AOSP AudioTrack.setPlaybackPositionUpdateListener(listener, handler): ATL calls the listener from the
	 * playback thread in either case, as for the variant without a handler */
	public void setPlaybackPositionUpdateListener(OnPlaybackPositionUpdateListener listener, android.os.Handler handler) {
		setPlaybackPositionUpdateListener(listener);
	}

	/* AOSP AudioTrack.setPositionNotificationPeriod()/getPositionNotificationPeriod(): the period is kept; ATL calls
	 * onPeriodicNotification() once per period of the ALSA stream */
	public int setPositionNotificationPeriod(int periodInFrames) {
		if (getState() == STATE_UNINITIALIZED) {
			return ERROR_INVALID_OPERATION;
		}
		mPositionNotificationPeriod = periodInFrames;
		return SUCCESS;
	}

	/* AOSP AudioTrack.getNotificationMarkerPosition()/setNotificationMarkerPosition(): the marker is kept as
	 * AudioTrack::setMarkerPosition() does; ATL's output has no thread that reports the marker as reached
	 * (only the periodic notification of the native write path), so onMarkerReached() is not called */
	public int getNotificationMarkerPosition() {
		return mMarkerPosition;
	}

	public int setNotificationMarkerPosition(int markerInFrames) {
		if (getState() == STATE_UNINITIALIZED) {
			return ERROR_INVALID_OPERATION;
		}
		mMarkerPosition = markerInFrames;
		return SUCCESS;
	}

	public int getPositionNotificationPeriod() {
		return mPositionNotificationPeriod;
	}

	public void play() {
		System.out.println("calling AudioTrack.play()\n");
		playbackState = PLAYSTATE_PLAYING;
		native_play();
	}

	/* like AOSP for streaming tracks: what was written before is still played out */
	public void stop() {
		playbackState = PLAYSTATE_STOPPED;
		native_stop();
	}

	/**
	 * Discards the data that was written but not played yet. Only when stopped or paused, like
	 * AOSP; the playback head position starts again at 0.
	 */
	public void flush() {
		if (playbackState == PLAYSTATE_PLAYING)
			return;
		native_flush();
		playbackHeadPosition = 0;
	}

	public void release() {
		System.out.println("calling AudioTrack.release()\n");
		native_release();
		pcm_handle = 0; // AOSP release(): the track is STATE_UNINITIALIZED afterwards, a second release does nothing
		mStateOverride = -1;
		playbackState = PLAYSTATE_STOPPED;
	}

	/* AOSP AudioTrack.finalize(): closes the PCM device if the app did not */
	@Override
	protected void finalize() {
		if (pcm_handle != 0) {
			native_release();
			pcm_handle = 0;
		}
	}

	public int getState() {
		if (mStateOverride >= 0)
			return mStateOverride; // AOSP setState()
		// the native constructor leaves pcm_handle at 0 when no PCM device could be opened
		return pcm_handle != 0 ? STATE_INITIALIZED : STATE_UNINITIALIZED;
	}

	/* AOSP AudioTrack.setState(): deprecated, sets the state as is */
	@Deprecated
	protected void setState(int state) {
		mStateOverride = state;
	}

	public int write(byte[] audioData, int offsetInBytes, int sizeInBytes) {
		return write(audioData, offsetInBytes, sizeInBytes, WRITE_BLOCKING);
	}

	/* AOSP: WRITE_BLOCKING waits until all data is queued, WRITE_NON_BLOCKING queues what fits and returns at once
	 * (0 when the buffer is full). A track that is stopped or paused on entry returns a short count instead of
	 * waiting (AOSP write(byte[], int, int)), here like a non-blocking write. */
	private boolean blocking(int writeMode) {
		return writeMode == WRITE_BLOCKING && playbackState == PLAYSTATE_PLAYING;
	}

	/* AOSP: byte[] data is not for a float track (write(ByteBuffer, ...) is) */
	public int write(byte[] audioData, int offsetInBytes, int sizeInBytes, int writeMode) {
		if (getState() == STATE_UNINITIALIZED || encoding == AudioFormat.ENCODING_PCM_FLOAT) {
			return ERROR_INVALID_OPERATION;
		}
		return writeBytes(audioData, offsetInBytes, sizeInBytes, writeMode);
	}

	/* the track's frame size: channel count times the size of a sample of its encoding */
	private int frameSize() {
		return channelCount * bytesPerSample(encoding);
	}

	/* for native code's use: the enabled Dynamics Processing of the track's session (AudioFlinger applies a session's
	 * effect chain to the mix of its tracks), see updateEffect() */
	float[] effect;
	float[] effectState = new float[2]; // the limiter's envelope per output channel

	private static final java.util.UUID DYNAMICS_PROCESSING = android.media.audiofx.AudioEffect.EFFECT_TYPE_DYNAMICS_PROCESSING;

	/* the parameters of the enabled Dynamics Processing effect on the track's session, as its engine uses them
	 * (EffectDynamicsProcessing.cpp): the preferred frame duration of the engine architecture, then for each output
	 * channel the input gain (dB) and the limiter (applies: 1/0, attack ms, release ms, ratio, threshold dB, post gain
	 * dB); null without such an effect */
	private void updateEffect() {
		float[] params = null;
		for (android.media.audiofx.AudioEffect e : android.media.audiofx.AudioEffect.enabledEffects(sessionId)) {
			if (!DYNAMICS_PROCESSING.equals(e.typeOf()))
				continue;
			java.nio.ByteBuffer arch = value(e, 0x30);
			params = new float[1 + 2 * 7];
			boolean limiterInUse = arch != null && arch.capacity() >= 36 && arch.getInt(32) != 0;
			params[0] = arch != null && arch.capacity() >= 8 ? arch.getFloat(4) : 10.f;
			for (int ch = 0; ch < 2; ch++) {
				java.nio.ByteBuffer gain = value(e, 0x20, ch);
				java.nio.ByteBuffer lim = value(e, 0x70, ch);
				int o = 1 + ch * 7;
				params[o] = gain != null && gain.capacity() >= 4 ? gain.getFloat(0) : 0.f;
				if (limiterInUse && lim != null && lim.capacity() >= 32 && lim.getInt(0) != 0 && lim.getInt(4) != 0) {
					params[o + 1] = 1.f;
					params[o + 2] = lim.getFloat(12);
					params[o + 3] = lim.getFloat(16);
					params[o + 4] = lim.getFloat(20);
					params[o + 5] = lim.getFloat(24);
					params[o + 6] = lim.getFloat(28);
				}
			}
			break;
		}
		effect = params;
	}

	private static java.nio.ByteBuffer value(android.media.audiofx.AudioEffect e, int... param) {
		java.nio.ByteBuffer p = java.nio.ByteBuffer.allocate(param.length * 4).order(java.nio.ByteOrder.nativeOrder());
		for (int v : param)
			p.putInt(v);
		byte[] v = e.parameter(p.array());
		return v == null ? null : java.nio.ByteBuffer.wrap(v).order(java.nio.ByteOrder.nativeOrder());
	}

	private int writeBytes(byte[] audioData, int offsetInBytes, int sizeInBytes, int writeMode) {
		if ((writeMode != WRITE_BLOCKING) && (writeMode != WRITE_NON_BLOCKING)) {
			return ERROR_BAD_VALUE;
		}
		/* sanity check the parameters before calling native_write */
		if ((audioData == null)
		    || (offsetInBytes < 0) || (sizeInBytes < 0)
		    || (offsetInBytes + sizeInBytes < 0)
		    || (offsetInBytes + sizeInBytes > audioData.length)) {
			return ERROR_BAD_VALUE;
		}

		int framesToWrite = sizeInBytes / frameSize();
		updateEffect();
		int ret = native_write(audioData, offsetInBytes, framesToWrite, volume, blocking(writeMode));
		if (ret > 0) {
			playbackHeadPosition += ret;
		}
		return ret < 0 ? ret : ret * frameSize();
	}

	/* AOSP AudioTrack.write(ByteBuffer, int, int): the bytes from the position, a direct buffer too */
	public int write(ByteBuffer audioData, int sizeInBytes, int writeMode) {
		if ((writeMode != WRITE_BLOCKING) && (writeMode != WRITE_NON_BLOCKING)) {
			return ERROR_BAD_VALUE;
		}
		if (audioData == null || sizeInBytes < 0 || sizeInBytes > audioData.remaining()) {
			return ERROR_BAD_VALUE;
		}
		if (getState() == STATE_UNINITIALIZED) {
			return ERROR_INVALID_OPERATION;
		}
		int ret;
		if (audioData.hasArray()) {
			ret = writeBytes(audioData.array(), audioData.arrayOffset() + audioData.position(), sizeInBytes, writeMode);
		} else {
			byte[] copy = new byte[sizeInBytes];
			audioData.duplicate().get(copy);
			ret = writeBytes(copy, 0, sizeInBytes, writeMode);
		}
		if (ret > 0) {
			audioData.position(audioData.position() + ret);
		}
		return ret;
	}

	/* AOSP AudioTrack.write(ByteBuffer, int, int, long): the timestamp is for tunneled A/V sync playback (HW_AV_SYNC), which
	 * ATL does not have */
	public int write(ByteBuffer audioData, int sizeInBytes, int writeMode, long timestamp) {
		return write(audioData, sizeInBytes, writeMode);
	}

	public int write(short[] audioData, int offsetInShorts, int sizeInShorts) {
		return write(audioData, offsetInShorts, sizeInShorts, WRITE_BLOCKING);
	}

	/* AOSP AudioTrack.write(float[], ...): only for an ENCODING_PCM_FLOAT track; returns the number of floats written */
	public int write(float[] audioData, int offsetInFloats, int sizeInFloats, int writeMode) {
		if (getState() == STATE_UNINITIALIZED || encoding != AudioFormat.ENCODING_PCM_FLOAT) {
			return ERROR_INVALID_OPERATION;
		}
		if ((writeMode != WRITE_BLOCKING) && (writeMode != WRITE_NON_BLOCKING)) {
			return ERROR_BAD_VALUE;
		}
		if (audioData == null || offsetInFloats < 0 || sizeInFloats < 0
		    || offsetInFloats + sizeInFloats < 0 || offsetInFloats + sizeInFloats > audioData.length) {
			return ERROR_BAD_VALUE;
		}
		int framesToWrite = sizeInFloats / channelCount;
		updateEffect();
		int ret = native_write(audioData, offsetInFloats, framesToWrite, volume, blocking(writeMode));
		if (ret > 0) {
			playbackHeadPosition += ret;
		}
		return ret < 0 ? ret : ret * channelCount;
	}

	/* AOSP: not for a float track or an encoding above ENCODING_LEGACY_SHORT_ARRAY_THRESHOLD; the shorts are the track's
	 * data as it is (returns the number of shorts written) */
	public int write(short audioData[], int offsetInShorts, int sizeInShorts, int writeMode) {
		if (getState() == STATE_UNINITIALIZED || encoding == AudioFormat.ENCODING_PCM_FLOAT
		    || encoding > AudioFormat.ENCODING_LEGACY_SHORT_ARRAY_THRESHOLD) {
			return ERROR_INVALID_OPERATION;
		}
		if ((writeMode != WRITE_BLOCKING) && (writeMode != WRITE_NON_BLOCKING)) {
			return ERROR_BAD_VALUE;
		}
		/* sanity check the parameters before calling native_write */
		if ((audioData == null)
		    || (offsetInShorts < 0) || (sizeInShorts < 0)
		    || (offsetInShorts + sizeInShorts < 0)
		    || (offsetInShorts + sizeInShorts > audioData.length)) {
			return ERROR_BAD_VALUE;
		}

		int framesToWrite = sizeInShorts * 2 / frameSize();
		updateEffect();
		int ret = native_write(audioData, offsetInShorts, framesToWrite, volume, blocking(writeMode));
		if (ret > 0) {
			playbackHeadPosition += ret;
		}
		return ret < 0 ? ret : ret * frameSize() / 2;
	}

	public int getAudioSessionId() {
		return sessionId;
	}

	public int getSampleRate() {
		return sampleRateInHz;
	}

	/** @hide AOSP: native_get_latency(), i.e. AudioTrack::mLatency = afLatency + 1000 * mFrameCount / mSampleRate.
	 * ExoPlayer looks it up via reflection. ATL has no AudioFlinger: the output (afLatency) part is
	 * already accounted for by the ALSA delay in getPlaybackHeadPosition(), so only the buffer term is reported. */
	public int getLatency() {
		if (frameSize() <= 0 || sampleRateInHz <= 0)
			return 0;
		return (int)((long)bufferSizeInBytes / frameSize() * 1000 / sampleRateInHz);
	}

	public int setStereoVolume(float leftVolume, float rightVolume) {
		this.volume = (leftVolume + rightVolume) / 2;
		return 0;
	}

	public int getPlayState() {
		return playbackState;
	}

	public void pause() {
		System.out.println("calling AudioTrack.pause()\n");
		playbackState = PLAYSTATE_PAUSED;
		native_pause();
	}

	/* the frames written less those still queued (ALSA's delay, which also counts the latency of the output and
	 * can exceed what was written at first); AOSP's position starts at 0 */
	public int getPlaybackHeadPosition() {
		int position = playbackHeadPosition - native_getPlaybackHeadPosition();
		return (position < 0 && playbackHeadPosition >= 0) ? 0 : position;
	}

	public int setVolume(float volume) {
		this.volume = volume;
		return 0;
	}

	/* AOSP AudioTrack.getTimestamp(): the frame that was presented at the time (CLOCK_MONOTONIC) of nanoTime; only
	 * while the track plays. ATL knows the position from the ALSA delay (getPlaybackHeadPosition()) and takes
	 * the time of the call. */
	public boolean getTimestamp(AudioTimestamp timestamp) {
		if (timestamp == null) {
			throw new IllegalArgumentException();
		}
		if (playbackState != PLAYSTATE_PLAYING || getState() != STATE_INITIALIZED) {
			return false;
		}
		timestamp.framePosition = getPlaybackHeadPosition() & 0xFFFFFFFFL;
		timestamp.nanoTime = System.nanoTime();
		return true;
	}

	private PlaybackParams mPlaybackParams = new PlaybackParams();
	private float mAuxEffectSendLevel;
	private AudioDeviceInfo mPreferredDevice;

	/* AOSP AudioTrack.setPlaybackParams(): ATL's ALSA output plays at the sample rate it was opened with, the
	 * speed and pitch are kept (getPlaybackParams()) and not applied */
	public void setPlaybackParams(PlaybackParams params) {
		if (params == null) {
			throw new IllegalArgumentException("params is null");
		}
		mPlaybackParams = params;
	}

	public PlaybackParams getPlaybackParams() {
		return mPlaybackParams;
	}

	/* AOSP AudioTrack.attachAuxEffect(): ATL has no audio effects. 0 detaches (always possible); an effect id
	 * names an effect the audio framework cannot find, AOSP's answer is ERROR_BAD_VALUE. */
	public int attachAuxEffect(int effectId) {
		if (getState() == STATE_UNINITIALIZED) {
			return ERROR_INVALID_OPERATION;
		}
		return effectId == 0 ? SUCCESS : ERROR_BAD_VALUE;
	}

	/* AOSP AudioTrack.setAuxEffectSendLevel(): kept for the attached effect, which there is none to apply it to */
	public int setAuxEffectSendLevel(float level) {
		if (getState() == STATE_UNINITIALIZED) {
			return ERROR_INVALID_OPERATION;
		}
		mAuxEffectSendLevel = Math.max(0f, Math.min(level, 1f));
		return SUCCESS;
	}

	/* AOSP AudioTrack.setPreferredDevice(): an output device, or null for none; ATL has the one output
	 * (AudioManager.getDevices()), so the preference is always met */
	public boolean setPreferredDevice(AudioDeviceInfo deviceInfo) {
		if (deviceInfo != null && !deviceInfo.isSink()) {
			return false;
		}
		mPreferredDevice = deviceInfo;
		return true;
	}

	public AudioDeviceInfo getPreferredDevice() {
		return mPreferredDevice;
	}

	/* AOSP AudioTrack.getBufferSizeInFrames(): the frames the buffer holds, here the ALSA buffer's */
	public int getBufferSizeInFrames() {
		return bufferSizeInFrames;
	}

	/* AOSP AudioTrack.setBufferSizeInFrames(): AudioTrack::setBufferSizeInFrames() answers with the size
	 * actually in use; ATL cannot resize the opened ALSA buffer, so that stays the ALSA buffer's */
	public int setBufferSizeInFrames(int bufferSizeInFrames) {
		if (mode == /*MODE_STATIC*/ 0 || getState() == STATE_UNINITIALIZED)
			return ERROR_INVALID_OPERATION;
		if (bufferSizeInFrames < 0)
			return ERROR_BAD_VALUE;
		return this.bufferSizeInFrames;
	}

	/* AOSP AudioTrack.getPlaybackRate(): the rate set by setPlaybackRate(), the sample rate at first */
	public int getPlaybackRate() {
		return mPlaybackRate;
	}

	/* AOSP AudioTrack.setPlaybackRate(): the checks as AOSP; ATL's ALSA output keeps the rate it was opened with
	 * (like setPlaybackParams()), the new rate is kept for getPlaybackRate() */
	public int setPlaybackRate(int sampleRateInHz) {
		if (getState() != STATE_INITIALIZED) {
			return ERROR_INVALID_OPERATION;
		}
		if (sampleRateInHz <= 0) {
			return ERROR_BAD_VALUE;
		}
		mPlaybackRate = sampleRateInHz;
		return SUCCESS;
	}

	/* AOSP AudioTrack.getAudioAttributes() */
	public AudioAttributes getAudioAttributes() {
		if (getState() == STATE_UNINITIALIZED || mConfiguredAudioAttributes == null) {
			throw new IllegalStateException("track not initialized");
		}
		return mConfiguredAudioAttributes;
	}

	/* AOSP AudioTrack.getStreamType() */
	public int getStreamType() {
		return streamType;
	}

	/* AOSP AudioTrack.getChannelConfiguration(): audioParamCheck() maps the legacy configurations to the masks */
	public int getChannelConfiguration() {
		switch (channelConfig) {
			case 1: // CHANNEL_OUT_DEFAULT
			case 4: // CHANNEL_OUT_MONO
			case 2: // CHANNEL_CONFIGURATION_MONO
				return 4; // CHANNEL_OUT_MONO
			case 12: // CHANNEL_OUT_STEREO
			case 3: // CHANNEL_CONFIGURATION_STEREO
				return 12; // CHANNEL_OUT_STEREO
			default:
				return channelConfig;
		}
	}

	/* AOSP AudioTrack.getFormat() */
	public AudioFormat getFormat() {
		AudioFormat.Builder builder = new AudioFormat.Builder()
		    .setSampleRate(sampleRateInHz)
		    .setEncoding(getAudioFormat());
		if (getChannelConfiguration() != AudioFormat.CHANNEL_INVALID) {
			builder.setChannelMask(getChannelConfiguration());
		}
		return builder.build();
	}

	/* AOSP AudioTrack.getNativeFrameCount() (deprecated): the buffer capacity, the ALSA buffer */
	@Deprecated
	protected int getNativeFrameCount() {
		return bufferSizeInFrames;
	}

	/* AOSP AudioTrack.getMinVolume(): GAIN_MIN */
	static public float getMinVolume() {
		return 0.0f;
	}

	/* AOSP AudioTrack.getPerformanceMode(): derived from the output flags; ATL's ALSA output has neither
	 * the FAST nor the DEEP_BUFFER flag */
	public int getPerformanceMode() {
		return PERFORMANCE_MODE_NONE;
	}

	/* AOSP AudioTrack.getRoutedDevices(): the device the track plays on, the one output */
	@Override
	public java.util.List<AudioDeviceInfo> getRoutedDevices() {
		java.util.List<AudioDeviceInfo> audioDeviceInfos = new java.util.ArrayList<AudioDeviceInfo>();
		audioDeviceInfos.add(getRoutedDevice());
		return audioDeviceInfos;
	}

	/* AOSP AudioTrack.setDualMonoMode()/getDualMonoMode(): AudioTrack::setDualMonoMode() fails on an output
	 * that cannot do it (ATL's has no mixer thread), which is false, and getDualMonoMode() answers OFF */
	public boolean setDualMonoMode(int dualMonoMode) {
		if (!isValidDualMonoMode(dualMonoMode)) {
			throw new IllegalArgumentException("Invalid Dual Mono mode " + dualMonoMode);
		}
		return false;
	}

	public int getDualMonoMode() {
		return DUAL_MONO_MODE_OFF;
	}

	private static boolean isValidDualMonoMode(int dualMonoMode) {
		switch (dualMonoMode) {
			case DUAL_MONO_MODE_OFF:
			case DUAL_MONO_MODE_LR:
			case DUAL_MONO_MODE_LL:
			case DUAL_MONO_MODE_RR:
				return true;
			default:
				return false;
		}
	}

	/* AOSP AudioTrack.setAudioDescriptionMixLeveldB()/getAudioDescriptionMixLeveldB(): as above, the output
	 * cannot apply it (false), and the level is the default NEGATIVE_INFINITY */
	public boolean setAudioDescriptionMixLeveldB(float level) {
		if (Float.isNaN(level) || level > 48.f) { // MAX_AUDIO_DESCRIPTION_MIX_LEVEL
			throw new IllegalArgumentException("level is out of range" + level);
		}
		return false;
	}

	public float getAudioDescriptionMixLeveldB() {
		return Float.NEGATIVE_INFINITY;
	}

	/* AOSP AudioTrack.getOffloadDelay()/getOffloadPadding(): ATL has no offloaded track (build() refuses it) */
	public int getOffloadDelay() {
		throw new IllegalStateException("Illegal query of delay on non-offloaded track");
	}

	public int getOffloadPadding() {
		throw new IllegalStateException("Illegal query of padding on non-offloaded track");
	}

	/* AOSP AudioTrack.getMetrics(): native_getMetrics() reads the track's item from the mediametrics service,
	 * which ATL does not have: an empty bundle */
	public android.os.PersistableBundle getMetrics() {
		return new android.os.PersistableBundle();
	}

	/* AOSP AudioTrack.setLogSessionId()/getLogSessionId() */
	public void setLogSessionId(android.media.metrics.LogSessionId logSessionId) {
		java.util.Objects.requireNonNull(logSessionId);
		if (getState() == STATE_UNINITIALIZED) {
			throw new IllegalStateException("track not initialized");
		}
		mLogSessionId = logSessionId;
	}

	public android.media.metrics.LogSessionId getLogSessionId() {
		return mLogSessionId;
	}

	/* AOSP AudioTrack.OnCodecFormatChangedListener and its add/remove: the codec format only changes on
	 * offloaded and passthrough tracks, which ATL does not have, so the listeners are kept and never called */
	public interface OnCodecFormatChangedListener {
		void onCodecFormatChanged(AudioTrack audioTrack, AudioMetadataReadMap info);
	}

	public void addOnCodecFormatChangedListener(java.util.concurrent.Executor executor, OnCodecFormatChangedListener listener) {
		mCodecFormatChangedListeners.putIfAbsent(java.util.Objects.requireNonNull(listener), java.util.Objects.requireNonNull(executor));
	}

	public void removeOnCodecFormatChangedListener(OnCodecFormatChangedListener listener) {
		mCodecFormatChangedListeners.remove(java.util.Objects.requireNonNull(listener));
	}

	/* AOSP AudioTrack.MetricsConstants */
	public static final class MetricsConstants {
		private MetricsConstants() {}

		private static final String MM_PREFIX = "android.media.audiotrack.";

		public static final String STREAMTYPE = MM_PREFIX + "streamtype";
		public static final String CONTENTTYPE = MM_PREFIX + "type";
		public static final String USAGE = MM_PREFIX + "usage";
		public static final String SAMPLERATE = "android.media.audiorecord.samplerate";
		public static final String CHANNELMASK = "android.media.audiorecord.channelmask";
		public static final String SAMPLE_RATE = MM_PREFIX + "sampleRate";
		public static final String CHANNEL_MASK = MM_PREFIX + "channelMask";
		public static final String ENCODING = MM_PREFIX + "encoding";
		public static final String PORT_ID = MM_PREFIX + "portId";
		public static final String FRAME_COUNT = MM_PREFIX + "frameCount";
		public static final String ATTRIBUTES = MM_PREFIX + "attributes";
	}

	/* AOSP AudioTrack.getBufferCapacityInFrames(): the largest buffer the track can use, the ALSA buffer */
	public int getBufferCapacityInFrames() {
		return bufferSizeInFrames;
	}

	/* AOSP AudioTrack.getChannelCount() */
	public int getChannelCount() {
		return channelCount;
	}

	/* AOSP AudioTrack.getMaxVolume(): GAIN_MAX */
	static public float getMaxVolume() {
		return 1.0f;
	}

	/* AOSP AudioTrack.getUnderrunCount(): how often the buffer ran empty while playing */
	public int getUnderrunCount() {
		return underrunCount;
	}

	/* AOSP AudioTrack.getRoutedDevice(): the device the track plays on, the one output */
	@Override
	public AudioDeviceInfo getRoutedDevice() {
		return new AudioDeviceInfo(1, AudioDeviceInfo.TYPE_BUILTIN_SPEAKER, "default", true);
	}

	/* AOSP AudioTrack.addOnRoutingChangedListener(): ATL never reroutes a track (one output), so the
	 * listeners are never called */
	@Override
	public void addOnRoutingChangedListener(AudioRouting.OnRoutingChangedListener listener, android.os.Handler handler) {}

	@Override
	public void removeOnRoutingChangedListener(AudioRouting.OnRoutingChangedListener listener) {}

	/* AOSP AudioTrack.OnRoutingChangedListener (deprecated) and its add/remove variants */
	@Deprecated
	public interface OnRoutingChangedListener extends AudioRouting.OnRoutingChangedListener {
		public void onRoutingChanged(AudioTrack audioTrack);

		@Override
		default public void onRoutingChanged(AudioRouting router) {
			if (router instanceof AudioTrack) {
				onRoutingChanged((AudioTrack) router);
			}
		}
	}

	@Deprecated
	public void addOnRoutingChangedListener(OnRoutingChangedListener listener, android.os.Handler handler) {
		addOnRoutingChangedListener((AudioRouting.OnRoutingChangedListener) listener, handler);
	}

	@Deprecated
	public void removeOnRoutingChangedListener(OnRoutingChangedListener listener) {
		removeOnRoutingChangedListener((AudioRouting.OnRoutingChangedListener) listener);
	}

	private native int native_getPlaybackHeadPosition();
	public native void native_play();
	public native void native_pause();
	private native void native_stop();
	private native void native_flush();
	private native int native_write(byte[] audioData, int offsetInBytes, int framesToWrite, float volume, boolean blocking);
	private native int native_write(short[] audioData, int offsetInShorts, int framesToWrite, float volume, boolean blocking);
	private native int native_write(float[] audioData, int offsetInFloats, int framesToWrite, float volume, boolean blocking);
	public native void native_release();

	/* AOSP: the sampling rate of the output the stream type plays on (AudioSystem::getOutputSamplingRate()), the
	 * rate AudioManager reports as PROPERTY_OUTPUT_SAMPLE_RATE */
	public static int getNativeOutputSampleRate(int streamType) {
		return Integer.parseInt(AudioManager.OUTPUT_SAMPLE_RATE);
	}

	// nested classes
	public static class Builder {
		private AudioAttributes mAttributes;
		private AudioFormat mFormat;
		private int mBufferSizeInBytes;
		private int mTransferMode;
		private int mSessionId;
		private boolean mOffload;
		private int mEncapsulationMode = ENCAPSULATION_MODE_NONE;
		private android.content.Context mContext;

		public Builder() {}

		public AudioTrack build() {
			/* AOSP Builder.build(): offload only for a format the output can take compressed, which none is here */
			if (mOffload) {
				throw new UnsupportedOperationException("Cannot create AudioTrack, offload format / attributes not supported");
			}
			if (mEncapsulationMode != ENCAPSULATION_MODE_NONE) {
				throw new UnsupportedOperationException("Cannot create AudioTrack"); // AOSP: native_setup() rejects it
			}
			if (mAttributes == null) {
				mAttributes = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build();
			}
			final AudioTrack track = new AudioTrack(mAttributes, mFormat, mBufferSizeInBytes, mTransferMode, mSessionId);
			if (track.getState() == STATE_UNINITIALIZED) {
				// release is not necessary
				throw new UnsupportedOperationException("Cannot create AudioTrack");
			}
			return track;
		}

		/* API 31, AOSP AudioTrack.Builder.setContext(): the context only supplies attribution data */
		public Builder setContext(android.content.Context context) {
			mContext = java.util.Objects.requireNonNull(context);
			return this;
		}

		public Builder setSessionId(int sessionId) {
			mSessionId = sessionId;
			return this;
		}

		public Builder setAudioAttributes(AudioAttributes attributes) {
			mAttributes = attributes;
			return this;
		}

		public Builder setAudioFormat(AudioFormat format) {
			mFormat = format;
			return this;
		}

		public Builder setBufferSizeInBytes(int bufferSizeInBytes) {
			mBufferSizeInBytes = bufferSizeInBytes;
			return this;
		}

		/* AOSP AudioTrack.Builder.setEncapsulationMode(): build() fails for a mode other than NONE, no output supports it */
		public Builder setEncapsulationMode(int encapsulationMode) {
			switch (encapsulationMode) {
				case ENCAPSULATION_MODE_NONE:
				case ENCAPSULATION_MODE_ELEMENTARY_STREAM:
				case ENCAPSULATION_MODE_HANDLE:
					mEncapsulationMode = encapsulationMode;
					break;
				default:
					throw new IllegalArgumentException("Invalid encapsulation mode " + encapsulationMode);
			}
			return this;
		}

		public Builder setTransferMode(int mode) {
			mTransferMode = mode;
			return this;
		}

		public Builder setPerformanceMode(int performanceMode) {
			return this;
		}

		/* API 29: offload is never supported (see AudioManager), so build() refuses it as AOSP does */
		public Builder setOffloadedPlayback(boolean offload) {
			mOffload = offload;
			return this;
		}
	}

	/* API 29/31 */
	public static boolean isDirectPlaybackSupported(AudioFormat format, AudioAttributes attributes) {
		return false;
	}

	public boolean isOffloadedPlayback() {
		return false;
	}

	/* AOSP AudioTrack.setOffloadDelayPadding()/setOffloadEndOfStream(): the checks; the track is never offloaded */
	public void setOffloadDelayPadding(int delayInFrames, int paddingInFrames) {
		if (paddingInFrames < 0) {
			throw new IllegalArgumentException("Illegal negative padding");
		}
		if (delayInFrames < 0) {
			throw new IllegalArgumentException("Illegal negative delay");
		}
		throw new IllegalStateException("Illegal use of delay/padding on non-offloaded track");
	}

	public void setOffloadEndOfStream() {
		throw new IllegalStateException("EOS not supported on non-offloaded track");
	}

	/* AOSP AudioTrack.StreamEventCallback */
	public abstract static class StreamEventCallback {
		public void onTearDown(AudioTrack track) {}

		public void onPresentationEnded(AudioTrack track) {}

		public void onDataRequest(AudioTrack track, int sizeInFrames) {}
	}

	public void registerStreamEventCallback(java.util.concurrent.Executor executor, StreamEventCallback eventCallback) {}

	public void unregisterStreamEventCallback(StreamEventCallback eventCallback) {}

	/* AOSP AudioTrack.getAudioFormat(): mAudioFormat, where audioParamCheck() turned ENCODING_DEFAULT into
	 * ENCODING_PCM_16BIT */
	public int getAudioFormat() {
		return audioFormat == AudioFormat.ENCODING_DEFAULT ? AudioFormat.ENCODING_PCM_16BIT : audioFormat;
	}
    /* AOSP 16 API; no backing hardware/service in ATL */
    public int setLoopPoints(int arg0, int arg1, int arg2) { return 0; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public android.media.VolumeShaper createVolumeShaper(android.media.VolumeShaper.Configuration arg0) { return null; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public int getStartThresholdInFrames() { return 0; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public int reloadStaticData() { return 0; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public int setPlaybackHeadPosition(int arg0) { return 0; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public int setPresentation(android.media.AudioPresentation arg0) { return 0; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public int setStartThresholdInFrames(int arg0) { return 0; }
}
