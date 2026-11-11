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

	// for native code's use
	long pcm_handle;
	long params;
	int channels;
	int period_time;
	int bufferSizeInFrames; // the ALSA buffer
	int underrunCount;      // ALSA underruns (EPIPE) the writes recovered from
	// mostly
	static int frames;
	OnPlaybackPositionUpdateListener periodic_update_listener;

	native void native_constructor(int streamType, int sampleRateInHz, int num_channels, int audioFormat, int bufferSizeInBytes, int mode);
	public AudioTrack(int streamType, int sampleRateInHz, int channelConfig, int audioFormat, int bufferSizeInBytes, int mode) {
		this.streamType = streamType;
		this.sampleRateInHz = sampleRateInHz;
		this.channelConfig = channelConfig;
		this.audioFormat = audioFormat;
		this.bufferSizeInBytes = bufferSizeInBytes;
		this.mode = mode;

		System.out.println("\n\n\nAudioTrack(" + streamType + ", " + sampleRateInHz + ", " + channelConfig + ", " + audioFormat + ", " + bufferSizeInBytes + ", " + mode + "); called\n\n\n\n");
		native_constructor(streamType, sampleRateInHz, channelConfig, audioFormat, bufferSizeInBytes, mode);
	}

	public AudioTrack(int streamType, int sampleRateInHz, int channelConfig, int audioFormat, int bufferSizeInBytes, int mode, int sessionId) {
		this(streamType, sampleRateInHz, channelConfig, audioFormat, bufferSizeInBytes, mode);
		this.sessionId = sessionId;
	}

	public AudioTrack(AudioAttributes attributes, AudioFormat format, int bufferSizeInBytes, int mode, int sessionId) {
		this(AudioAttributes.toLegacyStreamType(attributes), format.sampleRate, format.channelMask, format.encoding, bufferSizeInBytes, mode, sessionId);
	}

	public static native int getMinBufferSize(int sampleRateInHz, int channelConfig, int audioFormat);

	public void setPlaybackPositionUpdateListener(OnPlaybackPositionUpdateListener listener) {
		this.periodic_update_listener = listener;
	}

	public int setPositionNotificationPeriod(int periodInFrames) {
		System.out.println("\n\nAudioTrack.nsetPositionNotificationPeriod(" + periodInFrames + "); called\n\n\n\n");
		return 0; // SUCCESS
	}

	public int getPositionNotificationPeriod() {
		return this.frames;
	}

	public void play() {
		System.out.println("calling AudioTrack.play()\n");
		playbackState = PLAYSTATE_PLAYING;
		native_play();
	}

	/* like AOSP for streaming tracks: what was written before is still played out */
	public void stop() {
		playbackState = PLAYSTATE_STOPPED;
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
	}

	public int getState() {
		// the native constructor leaves pcm_handle at 0 when no PCM device could be opened
		return pcm_handle != 0 ? STATE_INITIALIZED : STATE_UNINITIALIZED;
	}

	public int write(byte[] audioData, int offsetInBytes, int sizeInBytes) {
		/* sanity check the parameters before calling native_write */
		if ((audioData == null)
		    || (offsetInBytes < 0) || (sizeInBytes < 0)
		    || (offsetInBytes + sizeInBytes < 0)
		    || (offsetInBytes + sizeInBytes > audioData.length)) {
			return ERROR_BAD_VALUE;
		}

		int framesToWrite = sizeInBytes / channels / 2; // 2 means PCM16
		int ret = native_write(audioData, offsetInBytes, framesToWrite, volume);
		if (ret > 0) {
			playbackHeadPosition += ret;
		}
		return ret * channels * 2; // 2 means PCM16
	}

	/* AOSP AudioTrack.write(ByteBuffer, int, int): the bytes from the position, a direct buffer too */
	public int write(ByteBuffer audioData, int sizeInBytes, int writeMode) {
		if (audioData == null || sizeInBytes < 0 || sizeInBytes > audioData.remaining()) {
			return ERROR_BAD_VALUE;
		}
		int ret;
		if (audioData.hasArray()) {
			ret = write(audioData.array(), audioData.arrayOffset() + audioData.position(), sizeInBytes);
		} else {
			byte[] copy = new byte[sizeInBytes];
			audioData.duplicate().get(copy);
			ret = write(copy, 0, sizeInBytes);
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

	public int write(byte[] audioData, int offsetInBytes, int sizeInBytes, int writeMode) {
		return write(audioData, offsetInBytes, sizeInBytes);
	}

	public int write(short[] audioData, int offsetInShorts, int sizeInShorts, int writeMode) {
		return write(audioData, offsetInShorts, sizeInShorts);
	}

	/* AOSP AudioTrack.write(float[], ...): ENCODING_PCM_FLOAT data. ATL's output is PCM16 (native_constructor), the
	 * samples are converted (-1.0 to 1.0 to 16 bit, clipped). Returns the number of floats written. */
	public int write(float[] audioData, int offsetInFloats, int sizeInFloats, int writeMode) {
		if (audioData == null || offsetInFloats < 0 || sizeInFloats < 0
		    || offsetInFloats + sizeInFloats < 0 || offsetInFloats + sizeInFloats > audioData.length) {
			return ERROR_BAD_VALUE;
		}
		short[] pcm = new short[sizeInFloats];
		for (int i = 0; i < sizeInFloats; i++) {
			float sample = audioData[offsetInFloats + i];
			pcm[i] = (short)Math.max(-32768, Math.min(32767, Math.round(sample * 32768f)));
		}
		return write(pcm, 0, sizeInFloats);
	}

	public int write(short audioData[], int offsetInShorts, int sizeInShorts) {
		/* sanity check the parameters before calling native_write */
		if ((audioData == null)
		    || (offsetInShorts < 0) || (sizeInShorts < 0)
		    || (offsetInShorts + sizeInShorts < 0)
		    || (offsetInShorts + sizeInShorts > audioData.length)) {
			return ERROR_BAD_VALUE;
		}

		int framesToWrite = sizeInShorts / channels;
		int ret = native_write(audioData, offsetInShorts, framesToWrite, volume);
		if (ret > 0) {
			playbackHeadPosition += ret;
		}
		return ret * channels;
	}

	public int getAudioSessionId() {
		return sessionId;
	}

	public int getSampleRate() {
		return sampleRateInHz;
	}

	/** @hide AOSP: native_get_latency(), i.e. AudioTrack::mLatency = afLatency + 1000 * mFrameCount / mSampleRate.
	 * ExoPlayer looks it up via reflection. ATL has no AudioFlinger: the output (afLatency) part is
	 * already accounted for by the ALSA delay in getPlaybackHeadPosition(), so only the buffer term
	 * (PCM16, as used by native_constructor) is reported. */
	public int getLatency() {
		if (channels <= 0 || sampleRateInHz <= 0)
			return 0;
		return (int)((long)bufferSizeInBytes / (channels * 2) * 1000 / sampleRateInHz);
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

	public int getPlaybackHeadPosition() {
		return playbackHeadPosition - native_getPlaybackHeadPosition();
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

	private native int native_getPlaybackHeadPosition();
	public native void native_play();
	public native void native_pause();
	private native void native_flush();
	private native int native_write(byte[] audioData, int offsetInBytes, int framesToWrite, float volume);
	private native int native_write(short[] audioData, int offsetInShorts, int framesToWrite, float volume);
	public native void native_release();

	public static int getNativeOutputSampleRate(int i) {
		return -1;
	}

	// nested classes
	public static class Builder {
		private AudioAttributes mAttributes;
		private AudioFormat mFormat;
		private int mBufferSizeInBytes;
		private int mTransferMode;
		private int mSessionId;
		private boolean mOffload;

		public Builder() {}

		public AudioTrack build() {
			/* AOSP Builder.build(): offload only for a format the output can take compressed, which none is here */
			if (mOffload) {
				throw new UnsupportedOperationException("Cannot create AudioTrack, offload format / attributes not supported");
			}
			if (mAttributes == null) {
				mAttributes = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build();
			}
			return new AudioTrack(mAttributes, mFormat, mBufferSizeInBytes, mTransferMode, mSessionId);
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

	public void setOffloadDelayPadding(int delayInFrames, int paddingInFrames) {}

	public void setOffloadEndOfStream() {}

	public interface StreamEventCallback {}

	public void registerStreamEventCallback(java.util.concurrent.Executor executor, StreamEventCallback eventCallback) {}

	public void unregisterStreamEventCallback(StreamEventCallback eventCallback) {}

	public void setLogSessionId(android.media.metrics.LogSessionId logSessionId) {}
}
