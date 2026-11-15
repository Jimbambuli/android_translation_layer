package android.media;

public class AudioRecord {

	public static final int RECORDSTATE_STOPPED = 1;
	public static final int RECORDSTATE_RECORDING = 3;

	public static final int ERROR_BAD_VALUE = -2;
	public static final int ERROR_INVALID_OPERATION = -3;

	private long pcm_handle;
	private int channels; // set by native constructor
	private int recordingState = RECORDSTATE_STOPPED;
	private int sampleRateInHz;

	private native long native_constructor(int streamType, int sampleRateInHz, int num_channels, int audioFormat, int bufferSizeInBytes);
	private native void native_record(long pcm_handle);
	private native void native_stop(long pcm_handle);
	private native int native_read(long pcm_handle, short[] audioData, int offsetInShorts, int framesToWrite);
	private native void native_release(long pcm_handle);

	/** in a Flatpak the microphone is PulseAudio's (--socket=pulseaudio); outside only with ATL_UGLY_ENABLE_MICROPHONE */
	public static boolean microphoneAvailable() {
		if (android.content.pm.PackageManager.deniedByUser("android.permission.RECORD_AUDIO"))
			return false;                   // switched off for this app
		return System.getenv("ATL_UGLY_ENABLE_MICROPHONE") != null || new java.io.File("/.flatpak-info").exists();
	}

	/* AOSP AudioRecord fields set by audioParamCheck()/getChannelMaskFromLegacyConfig() and native_setup() */
	private int mRecordSource;
	private int mAudioFormat;
	private int mChannelMask;
	private final int mSessionId = new AudioManager().generateAudioSessionId();

	private int mBufferSizeInBytes;
	private AudioDeviceInfo mPreferredDevice;
	private final java.util.LinkedList<RecordingCallbackInfo> mRecordCallbackList = new java.util.LinkedList<>();

	/* AudioRecords that are recording: ATL's stand-in for AudioService's recording monitor */
	private static final java.util.ArrayList<AudioRecord> sActiveRecords = new java.util.ArrayList<>();

	public AudioRecord(int streamType, int sampleRateInHz, int channelConfig, int audioFormat, int bufferSizeInBytes) {
		this.sampleRateInHz = sampleRateInHz;
		mBufferSizeInBytes = bufferSizeInBytes;
		mRecordSource = streamType;
		mAudioFormat = audioFormat == AudioFormat.ENCODING_DEFAULT ? AudioFormat.ENCODING_PCM_16BIT : audioFormat;
		switch (channelConfig) {
			case 1: // AudioFormat.CHANNEL_IN_DEFAULT
			case 0x10: // AudioFormat.CHANNEL_IN_MONO
			case 2: // AudioFormat.CHANNEL_CONFIGURATION_MONO
				mChannelMask = 0x10;
				break;
			case 0xc: // AudioFormat.CHANNEL_IN_STEREO
			case 3: // AudioFormat.CHANNEL_CONFIGURATION_STEREO
				mChannelMask = 0xc;
				break;
			default:
				mChannelMask = channelConfig;
				break;
		}
		/* AOSP: without RECORD_AUDIO the native setup fails and the AudioRecord stays uninitialized */
		if (!android.content.pm.PackageManager.deniedByUser("android.permission.RECORD_AUDIO"))
			pcm_handle = native_constructor(streamType, sampleRateInHz, channelConfig, audioFormat, bufferSizeInBytes);
	}

	public static native int getMinBufferSize(int sampleRateInHz, int channelConfig, int audioFormat);

	public int getSampleRate() {
		return sampleRateInHz;
	}

	/* AOSP AudioRecord.getAudioSource() */
	public int getAudioSource() {
		return mRecordSource;
	}

	/* AOSP AudioRecord.getAudioFormat(): the encoding */
	public int getAudioFormat() {
		return mAudioFormat;
	}

	/* AOSP AudioRecord.getChannelCount(): AudioFormat.channelCountFromInChannelMask(mChannelMask) */
	public int getChannelCount() {
		return AudioFormat.channelCountFromInChannelMask(mChannelMask);
	}

	/* AOSP AudioRecord.getFormat() */
	public AudioFormat getFormat() {
		AudioFormat.Builder builder = new AudioFormat.Builder()
		    .setSampleRate(sampleRateInHz)
		    .setEncoding(mAudioFormat);
		if (mChannelMask != AudioFormat.CHANNEL_INVALID) {
			builder.setChannelMask(mChannelMask);
		}
		return builder.build();
	}

	/* AOSP AudioRecord.getAudioSessionId(): native_setup() allocates a new session (AUDIO_SESSION_ALLOCATE) */
	public int getAudioSessionId() {
		return mSessionId;
	}

	public int getState() {
		return pcm_handle != 0 ? /*STATE_INITIALIZED*/ 1 : /*STATE_UNINITIALIZED*/ 0;
	}

	public int getRecordingState() {
		return recordingState;
	}

	public void startRecording() {
		if (pcm_handle == 0)
			throw new IllegalStateException("startRecording() called on an uninitialized AudioRecord.");
		native_record(pcm_handle);
		recordingState = RECORDSTATE_RECORDING;
		synchronized (sActiveRecords) {
			if (!sActiveRecords.contains(this))
				sActiveRecords.add(this);
		}
		dispatchRecordingConfigChange();
	}

	/* AOSP AudioRecordingMonitorImpl, MSG_RECORDING_CONFIG_CHANGE: the callbacks hear of this record's
	 * configuration when AudioService reports one for its session, i.e. once the recording is active */
	private void dispatchRecordingConfigChange() {
		final AudioRecordingConfiguration config = getActiveRecordingConfiguration();
		if (config == null)
			return;
		final java.util.List<AudioRecordingConfiguration> configList = new java.util.ArrayList<>(1);
		configList.add(config);
		synchronized (mRecordCallbackList) {
			for (final RecordingCallbackInfo arci : mRecordCallbackList) {
				arci.mExecutor.execute(new Runnable() {
					@Override
					public void run() {
						arci.mCb.onRecordingConfigChanged(configList);
					}
				});
			}
		}
	}

	private static class RecordingCallbackInfo {
		final java.util.concurrent.Executor mExecutor;
		final AudioManager.AudioRecordingCallback mCb;

		RecordingCallbackInfo(java.util.concurrent.Executor e, AudioManager.AudioRecordingCallback cb) {
			mExecutor = e;
			mCb = cb;
		}
	}

	/* AOSP AudioRecord.registerAudioRecordingCallback() -> AudioRecordingMonitorImpl */
	public void registerAudioRecordingCallback(java.util.concurrent.Executor executor, AudioManager.AudioRecordingCallback cb) {
		if (cb == null)
			throw new IllegalArgumentException("Illegal null AudioRecordingCallback");
		if (executor == null)
			throw new IllegalArgumentException("Illegal null Executor");
		synchronized (mRecordCallbackList) {
			for (RecordingCallbackInfo arci : mRecordCallbackList) {
				if (arci.mCb == cb)
					throw new IllegalArgumentException("AudioRecordingCallback already registered");
			}
			mRecordCallbackList.add(new RecordingCallbackInfo(executor, cb));
		}
	}

	public void unregisterAudioRecordingCallback(AudioManager.AudioRecordingCallback cb) {
		if (cb == null)
			throw new IllegalArgumentException("Illegal null AudioRecordingCallback argument");
		synchronized (mRecordCallbackList) {
			for (RecordingCallbackInfo arci : mRecordCallbackList) {
				if (arci.mCb == cb) {
					mRecordCallbackList.remove(arci);
					return;
				}
			}
			throw new IllegalArgumentException("AudioRecordingCallback was not registered");
		}
	}

	/* AOSP AudioRecordingMonitorImpl.getActiveRecordingConfiguration(): the configuration AudioService
	 * reports for this record's session (anonymized as for an unprivileged app), null when not recording */
	public AudioRecordingConfiguration getActiveRecordingConfiguration() {
		synchronized (sActiveRecords) {
			if (!sActiveRecords.contains(this))
				return null;
		}
		AudioFormat format = getFormat();
		return new AudioRecordingConfiguration(-1, mSessionId, mRecordSource, format, format, 0, "", 0, false,
		                                       mRecordSource, new android.media.audiofx.AudioEffect.Descriptor[0],
		                                       new android.media.audiofx.AudioEffect.Descriptor[0]);
	}

	/* AudioManager.getActiveRecordingConfigurations(): those of all active records of the app */
	static java.util.List<AudioRecordingConfiguration> activeRecordingConfigurations() {
		java.util.ArrayList<AudioRecord> records;
		synchronized (sActiveRecords) {
			records = new java.util.ArrayList<>(sActiveRecords);
		}
		java.util.ArrayList<AudioRecordingConfiguration> configs = new java.util.ArrayList<>();
		for (AudioRecord record : records) {
			AudioRecordingConfiguration config = record.getActiveRecordingConfiguration();
			if (config != null)
				configs.add(config);
		}
		return configs;
	}

	/* AOSP setPreferredDevice(): an input device or null; native_setInputDevice() succeeds, ATL records
	 * from its one input either way */
	public boolean setPreferredDevice(AudioDeviceInfo deviceInfo) {
		if (deviceInfo != null && !deviceInfo.isSource())
			return false;
		synchronized (this) {
			mPreferredDevice = deviceInfo;
		}
		return true;
	}

	public AudioDeviceInfo getPreferredDevice() {
		synchronized (this) {
			return mPreferredDevice;
		}
	}

	/* AOSP getRoutedDevice(): native_getRoutedDeviceId() is 0 (null) unless the record is active, then
	 * the input it records from, ATL's one input */
	public AudioDeviceInfo getRoutedDevice() {
		if (recordingState != RECORDSTATE_RECORDING)
			return null;
		AudioDeviceInfo[] inputs = new AudioManager().getDevices(AudioManager.GET_DEVICES_INPUTS);
		return inputs.length > 0 ? inputs[0] : null;
	}

	/* AOSP getBufferSizeInFrames(): native_setup() sizes the buffer as bufferSizeInBytes / frame size */
	public int getBufferSizeInFrames() {
		int bytesPerSample;
		switch (mAudioFormat) {
			case AudioFormat.ENCODING_PCM_8BIT: bytesPerSample = 1; break;
			case AudioFormat.ENCODING_PCM_FLOAT: bytesPerSample = 4; break;
			default: bytesPerSample = 2; break;
		}
		return mBufferSizeInBytes / (getChannelCount() * bytesPerSample);
	}

	/* API 24, AOSP AudioRecord.getTimestamp(): argument check as AOSP; ATL's capture path reports no
	 * timestamps, which AOSP's native_get_timestamp() returns as ERROR_INVALID_OPERATION */
	public int getTimestamp(AudioTimestamp outTimestamp, int timebase) {
		if (outTimestamp == null ||
		    (timebase != AudioTimestamp.TIMEBASE_BOOTTIME
		     && timebase != AudioTimestamp.TIMEBASE_MONOTONIC)) {
			throw new IllegalArgumentException();
		}
		return ERROR_INVALID_OPERATION;
	}

	public int read(short[] audioData, int offsetInShorts, int sizeInShorts) {
		/* sanity check the parameters before calling native_write */
		if ((audioData == null)
		    || (offsetInShorts < 0) || (sizeInShorts < 0)
		    || (offsetInShorts + sizeInShorts < 0)
		    || (offsetInShorts + sizeInShorts > audioData.length)) {
			return ERROR_BAD_VALUE;
		}
		if (pcm_handle == 0)
			return /*ERROR_INVALID_OPERATION*/ -3;

		return native_read(pcm_handle, audioData, offsetInShorts, sizeInShorts / channels) * channels;
	}

	public void stop() {
		if (pcm_handle != 0)
			native_stop(pcm_handle);
		recordingState = RECORDSTATE_STOPPED;
		synchronized (sActiveRecords) {
			sActiveRecords.remove(this);
		}
	}

	public void release() {
		if (pcm_handle != 0)
			native_release(pcm_handle);
		pcm_handle = 0;
		synchronized (sActiveRecords) {
			sActiveRecords.remove(this);
		}
	}

	public static class Builder {

		private int audioSource;
		private AudioFormat audioFormat;
		private int mBufferSizeInBytes;
		private android.content.Context mContext;

		/* AOSP AudioRecord.Builder.setBufferSizeInBytes() */
		public Builder setBufferSizeInBytes(int bufferSizeInBytes) throws IllegalArgumentException {
			if (bufferSizeInBytes <= 0) {
				throw new IllegalArgumentException("Invalid buffer size " + bufferSizeInBytes);
			}
			mBufferSizeInBytes = bufferSizeInBytes;
			return this;
		}

		/* API 31, AOSP AudioRecord.Builder.setContext(): the context only supplies attribution data */
		public Builder setContext(android.content.Context context) {
			mContext = java.util.Objects.requireNonNull(context);
			return this;
		}

		public Builder setAudioSource(int audioSource) {
			this.audioSource = audioSource;
			return this;
		}

		public Builder setAudioFormat(AudioFormat audioFormat) {
			this.audioFormat = audioFormat;
			return this;
		}

		public AudioRecord build() {
			return new AudioRecord(audioSource, audioFormat.sampleRate, audioFormat.channelMask, audioFormat.encoding,
			                       mBufferSizeInBytes != 0 ? mBufferSizeInBytes : 32768);
		}
	}
}
