package android.media;

public class AudioRecord implements AudioRouting, MicrophoneDirection, AudioRecordingMonitor {

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
	private boolean mPrivacySensitive;
	private android.media.metrics.LogSessionId mLogSessionId = android.media.metrics.LogSessionId.LOG_SESSION_ID_NONE;
	private final android.os.Looper mInitializationLooper = android.os.Looper.myLooper() != null ? android.os.Looper.myLooper()
	                                                                                              : android.os.Looper.getMainLooper();
	private final java.util.HashMap<AudioRouting.OnRoutingChangedListener, android.os.Handler> mRoutingChangeListeners = new java.util.HashMap<>();
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

	/* AOSP AudioRecord.getChannelConfiguration(): mChannelMask */
	public int getChannelConfiguration() {
		return mChannelMask;
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

	/* AOSP AudioRecord.startRecording(MediaSyncEvent): the state check and syncEvent.getType() as in AOSP, then the
	 * recording starts. ATL has no audio session to wait for, so the synchronization event is not awaited */
	public void startRecording(MediaSyncEvent syncEvent) throws IllegalStateException {
		if (pcm_handle == 0)
			throw new IllegalStateException("startRecording() called on an uninitialized AudioRecord.");
		syncEvent.getType();
		syncEvent.getAudioSessionId();
		startRecording();
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

	/* AOSP AudioRecord.getRoutedDevices(): the devices of native_getRoutedDeviceIds(), for ATL the one routed input */
	@Override
	public java.util.List<AudioDeviceInfo> getRoutedDevices() {
		java.util.List<AudioDeviceInfo> audioDeviceInfos = new java.util.ArrayList<AudioDeviceInfo>();
		AudioDeviceInfo device = getRoutedDevice();
		if (device != null)
			audioDeviceInfos.add(device);
		return audioDeviceInfos;
	}

	/* AOSP AudioRecord.addOnRoutingChangedListener(): ATL never reroutes a record (one input), so the listeners
	 * are kept and never called */
	@Override
	public void addOnRoutingChangedListener(AudioRouting.OnRoutingChangedListener listener, android.os.Handler handler) {
		synchronized (mRoutingChangeListeners) {
			if (listener != null && !mRoutingChangeListeners.containsKey(listener)) {
				mRoutingChangeListeners.put(listener, handler != null ? handler : new android.os.Handler(mInitializationLooper));
			}
		}
	}

	@Override
	public void removeOnRoutingChangedListener(AudioRouting.OnRoutingChangedListener listener) {
		synchronized (mRoutingChangeListeners) {
			mRoutingChangeListeners.remove(listener);
		}
	}

	/* AOSP AudioRecord.OnRoutingChangedListener (deprecated) and its add/remove variants */
	@Deprecated
	public interface OnRoutingChangedListener extends AudioRouting.OnRoutingChangedListener {
		public void onRoutingChanged(AudioRecord audioRecord);

		@Override
		default public void onRoutingChanged(AudioRouting router) {
			if (router instanceof AudioRecord) {
				onRoutingChanged((AudioRecord) router);
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

	/* AOSP AudioRecord.getActiveMicrophones(): the HAL (native_get_active_microphones()) reports none in ATL, which
	 * takes AOSP's fallback: the routed device's MicrophoneInfo with a direct mapping of every channel; none unless
	 * the record is active */
	public java.util.List<MicrophoneInfo> getActiveMicrophones() throws java.io.IOException {
		java.util.ArrayList<MicrophoneInfo> activeMicrophones = new java.util.ArrayList<>();
		AudioDeviceInfo device = getRoutedDevice();
		if (device != null) {
			for (MicrophoneInfo microphone : new AudioManager().getMicrophones()) {
				if (microphone.getId() != device.getId())
					continue;
				java.util.ArrayList<android.util.Pair<Integer, Integer>> channelMapping = new java.util.ArrayList<>();
				for (int i = 0; i < getChannelCount(); i++) {
					channelMapping.add(new android.util.Pair<Integer, Integer>(i, MicrophoneInfo.CHANNEL_MAPPING_DIRECT));
				}
				microphone.setChannelMapping(channelMapping);
				activeMicrophones.add(microphone);
				break;
			}
		}
		return activeMicrophones;
	}

	/* AOSP AudioRecord.setPreferredMicrophoneDirection(): native_set_preferred_microphone_direction() is
	 * AudioRecord::setPreferredMicrophoneDirection() through the audio HAL, which ATL does not have: not applied (false) */
	@Override
	public boolean setPreferredMicrophoneDirection(int direction) {
		return false;
	}

	/* AOSP AudioRecord.setPreferredMicrophoneFieldDimension(): argument check as AOSP, then as above not applied */
	@Override
	public boolean setPreferredMicrophoneFieldDimension(float zoom) {
		if (!(zoom >= -1 && zoom <= 1))
			throw new IllegalArgumentException("Argument must fall between -1 & 1 (inclusive)");
		return false;
	}

	/* AOSP AudioRecord.isPrivacySensitive(): AudioAttributes.FLAG_CAPTURE_PRIVATE, set by Builder.setPrivacySensitive() */
	public boolean isPrivacySensitive() {
		return mPrivacySensitive;
	}

	/* AOSP AudioRecord.setLogSessionId()/getLogSessionId() */
	public void setLogSessionId(android.media.metrics.LogSessionId logSessionId) {
		java.util.Objects.requireNonNull(logSessionId);
		if (pcm_handle == 0) {
			throw new IllegalStateException("AudioRecord not initialized");
		}
		mLogSessionId = logSessionId;
	}

	public android.media.metrics.LogSessionId getLogSessionId() {
		return mLogSessionId;
	}

	/* AOSP AudioRecord.getMetrics(): native_getMetrics() reads the record's item from the mediametrics service,
	 * which ATL does not have: an empty bundle */
	public android.os.PersistableBundle getMetrics() {
		return new android.os.PersistableBundle();
	}

	/* AOSP AudioRecord.MetricsConstants */
	public static final class MetricsConstants {
		private MetricsConstants() {}

		private static final String MM_PREFIX = "android.media.audiorecord.";

		public static final String ENCODING = MM_PREFIX + "encoding";
		public static final String SOURCE = MM_PREFIX + "source";
		@Deprecated
		public static final String LATENCY = MM_PREFIX + "latency";
		public static final String SAMPLERATE = MM_PREFIX + "samplerate";
		public static final String CHANNELS = MM_PREFIX + "channels";
		public static final String CHANNEL_MASK = MM_PREFIX + "channelMask";
		public static final String PORT_ID = MM_PREFIX + "portId";
		public static final String FRAME_COUNT = MM_PREFIX + "frameCount";
		public static final String ATTRIBUTES = MM_PREFIX + "attributes";
		public static final String DURATION_MS = MM_PREFIX + "durationMs";
		public static final String START_COUNT = MM_PREFIX + "startCount";
	}

	/* AOSP AudioRecord.getBufferSizeInFrames(): native_setup() sizes the buffer as bufferSizeInBytes / frame size */
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

	public static final int READ_BLOCKING = 0;
	public static final int READ_NON_BLOCKING = 1;

	/* AOSP AudioRecord.read(byte[], int, int) -> read(..., READ_BLOCKING) */
	public int read(byte[] audioData, int offsetInBytes, int sizeInBytes) {
		return read(audioData, offsetInBytes, sizeInBytes, READ_BLOCKING);
	}

	/* AOSP AudioRecord.read(byte[], int, int, int): the checks as in AOSP, then native_read_in_byte_array(). ATL
	 * captures 16-bit samples (S16_LE): PCM_16BIT is handed over in native (little) endian, PCM_8BIT as unsigned
	 * 8-bit like the AOSP native conversion. ATL's capture always blocks. */
	public int read(byte[] audioData, int offsetInBytes, int sizeInBytes, int readMode) {
		if (pcm_handle == 0 || mAudioFormat == AudioFormat.ENCODING_PCM_FLOAT) {
			return ERROR_INVALID_OPERATION;
		}
		if ((readMode != READ_BLOCKING) && (readMode != READ_NON_BLOCKING)) {
			android.util.Log.e("AudioRecord", "AudioRecord.read() called with invalid blocking mode");
			return ERROR_BAD_VALUE;
		}
		if ((audioData == null) || (offsetInBytes < 0) || (sizeInBytes < 0)
		    || (offsetInBytes + sizeInBytes < 0) // detect integer overflow
		    || (offsetInBytes + sizeInBytes > audioData.length)) {
			return ERROR_BAD_VALUE;
		}
		return readBytes(audioData, offsetInBytes, sizeInBytes);
	}

	/* AOSP AudioRecord.read(ByteBuffer, int) -> read(..., READ_BLOCKING) */
	public int read(java.nio.ByteBuffer audioBuffer, int sizeInBytes) {
		return read(audioBuffer, sizeInBytes, READ_BLOCKING);
	}

	/* AOSP AudioRecord.read(ByteBuffer, int, int): native_read_in_direct_buffer() writes to the beginning of a
	 * direct buffer, ignoring position and limit (which stay unchanged); a buffer that is not direct gets 0 */
	public int read(java.nio.ByteBuffer audioBuffer, int sizeInBytes, int readMode) {
		if (pcm_handle == 0) {
			return ERROR_INVALID_OPERATION;
		}
		if ((readMode != READ_BLOCKING) && (readMode != READ_NON_BLOCKING)) {
			android.util.Log.e("AudioRecord", "AudioRecord.read() called with invalid blocking mode");
			return ERROR_BAD_VALUE;
		}
		if ((audioBuffer == null) || (sizeInBytes < 0)) {
			return ERROR_BAD_VALUE;
		}
		if (!audioBuffer.isDirect())
			return 0;
		byte[] bytes = new byte[Math.min(sizeInBytes, audioBuffer.capacity())];
		int read = readBytes(bytes, 0, bytes.length);
		for (int i = 0; i < read; i++)
			audioBuffer.put(i, bytes[i]);
		return read;
	}

	/* reads whole frames of 16-bit samples and stores them in the record's encoding */
	private int readBytes(byte[] audioData, int offsetInBytes, int sizeInBytes) {
		final boolean eightBit = mAudioFormat == AudioFormat.ENCODING_PCM_8BIT;
		final int bytesPerSample = eightBit ? 1 : 2;
		final int samples = sizeInBytes / (bytesPerSample * channels) * channels;
		short[] buffer = new short[samples];
		int read = read(buffer, 0, samples);
		if (read <= 0)
			return read;
		for (int i = 0; i < read; i++) {
			if (eightBit) {
				audioData[offsetInBytes + i] = (byte)((buffer[i] >> 8) + 128);
			} else {
				audioData[offsetInBytes + 2 * i] = (byte)buffer[i];
				audioData[offsetInBytes + 2 * i + 1] = (byte)(buffer[i] >> 8);
			}
		}
		return read * bytesPerSample;
	}

	/* AOSP AudioRecord.read(short[], int, int) -> read(..., READ_BLOCKING) */
	public int read(short[] audioData, int offsetInShorts, int sizeInShorts) {
		return read(audioData, offsetInShorts, sizeInShorts, READ_BLOCKING);
	}

	/* AOSP AudioRecord.read(short[], int, int, int): the checks as in AOSP, then native_read_in_short_array() */
	public int read(short[] audioData, int offsetInShorts, int sizeInShorts, int readMode) {
		if (pcm_handle == 0
		    || mAudioFormat == AudioFormat.ENCODING_PCM_FLOAT
		    // use ByteBuffer instead for later encodings
		    || mAudioFormat > AudioFormat.ENCODING_LEGACY_SHORT_ARRAY_THRESHOLD) {
			return ERROR_INVALID_OPERATION;
		}
		if ((readMode != READ_BLOCKING) && (readMode != READ_NON_BLOCKING)) {
			android.util.Log.e("AudioRecord", "AudioRecord.read() called with invalid blocking mode");
			return ERROR_BAD_VALUE;
		}
		if ((audioData == null) || (offsetInShorts < 0) || (sizeInShorts < 0)
		    || (offsetInShorts + sizeInShorts < 0) // detect integer overflow
		    || (offsetInShorts + sizeInShorts > audioData.length)) {
			return ERROR_BAD_VALUE;
		}
		return readShorts(audioData, offsetInShorts, sizeInShorts);
	}

	/* AOSP AudioRecord.read(float[], int, int, int): ENCODING_PCM_FLOAT only. ATL captures 16-bit samples, which are
	 * converted to the range -1.0 to 1.0 */
	public int read(float[] audioData, int offsetInFloats, int sizeInFloats, int readMode) {
		if (pcm_handle == 0) {
			android.util.Log.e("AudioRecord", "AudioRecord.read() called in invalid state STATE_UNINITIALIZED");
			return ERROR_INVALID_OPERATION;
		}
		if (mAudioFormat != AudioFormat.ENCODING_PCM_FLOAT) {
			android.util.Log.e("AudioRecord", "AudioRecord.read(float[] ...) requires format ENCODING_PCM_FLOAT");
			return ERROR_INVALID_OPERATION;
		}
		if ((readMode != READ_BLOCKING) && (readMode != READ_NON_BLOCKING)) {
			android.util.Log.e("AudioRecord", "AudioRecord.read() called with invalid blocking mode");
			return ERROR_BAD_VALUE;
		}
		if ((audioData == null) || (offsetInFloats < 0) || (sizeInFloats < 0)
		    || (offsetInFloats + sizeInFloats < 0) // detect integer overflow
		    || (offsetInFloats + sizeInFloats > audioData.length)) {
			return ERROR_BAD_VALUE;
		}
		short[] buffer = new short[sizeInFloats];
		int read = readShorts(buffer, 0, sizeInFloats);
		for (int i = 0; i < read; i++)
			audioData[offsetInFloats + i] = buffer[i] / 32768f;
		return read;
	}

	/* reads whole frames of 16-bit samples from the capture device */
	private int readShorts(short[] audioData, int offsetInShorts, int sizeInShorts) {
		if (pcm_handle == 0)
			return ERROR_INVALID_OPERATION;
		int read = native_read(pcm_handle, audioData, offsetInShorts, sizeInShorts / channels) * channels;
		if (read > 0)
			framesRead(read / channels);
		return read;
	}

	/* AOSP AudioRecord marker and periodic position notification (native AudioRecord::processAudioBuffer(),
	 * whose source is not part of the AOSP tree used here): ATL counts the frames handed to the app and calls
	 * the OnRecordPositionUpdateListener on the thread of its Handler (default: the looper of the creating thread) */
	private final Object mPositionListenerLock = new Object();
	private OnRecordPositionUpdateListener mPositionListener;
	private android.os.Handler mEventHandler;
	private int mMarkerPosition;
	private boolean mMarkerReached;
	private int mUpdatePeriod;
	private long mNewPosition;
	private long mFramePosition;

	private void framesRead(int frames) {
		final OnRecordPositionUpdateListener listener;
		final android.os.Handler handler;
		int markerEvents = 0;
		int periodEvents = 0;
		synchronized (mPositionListenerLock) {
			mFramePosition += frames;
			if (mMarkerPosition > 0 && !mMarkerReached && mFramePosition >= mMarkerPosition) {
				mMarkerReached = true;
				markerEvents = 1;
			}
			if (mUpdatePeriod > 0) {
				while (mNewPosition <= mFramePosition) {
					periodEvents++;
					mNewPosition += mUpdatePeriod;
				}
			}
			listener = mPositionListener;
			handler = mEventHandler;
		}
		if (listener == null || handler == null)
			return;
		for (int i = 0; i < markerEvents; i++) {
			handler.post(new Runnable() {
				@Override
				public void run() {
					listener.onMarkerReached(AudioRecord.this);
				}
			});
		}
		for (int i = 0; i < periodEvents; i++) {
			handler.post(new Runnable() {
				@Override
				public void run() {
					listener.onPeriodicNotification(AudioRecord.this);
				}
			});
		}
	}

	/* AOSP AudioRecord.OnRecordPositionUpdateListener */
	public interface OnRecordPositionUpdateListener {
		void onMarkerReached(AudioRecord recorder);

		void onPeriodicNotification(AudioRecord recorder);
	}

	/* AOSP AudioRecord.setRecordPositionUpdateListener() */
	public void setRecordPositionUpdateListener(OnRecordPositionUpdateListener listener) {
		setRecordPositionUpdateListener(listener, null);
	}

	public void setRecordPositionUpdateListener(OnRecordPositionUpdateListener listener, android.os.Handler handler) {
		synchronized (mPositionListenerLock) {
			mPositionListener = listener;
			if (listener != null) {
				mEventHandler = new android.os.Handler(handler != null ? handler.getLooper() : mInitializationLooper);
			} else {
				mEventHandler = null;
			}
		}
	}

	/* AOSP AudioRecord.getNotificationMarkerPosition(): native_get_marker_pos(), 0 until one is set */
	public int getNotificationMarkerPosition() {
		synchronized (mPositionListenerLock) {
			return mMarkerPosition;
		}
	}

	/* AOSP AudioRecord.getPositionNotificationPeriod(): native_get_pos_update_period(), 0 until one is set */
	public int getPositionNotificationPeriod() {
		synchronized (mPositionListenerLock) {
			return mUpdatePeriod;
		}
	}

	/* AOSP AudioRecord.setNotificationMarkerPosition() */
	public int setNotificationMarkerPosition(int markerInFrames) {
		if (pcm_handle == 0)
			return ERROR_INVALID_OPERATION;
		synchronized (mPositionListenerLock) {
			mMarkerPosition = markerInFrames;
			mMarkerReached = false;
		}
		return SUCCESS;
	}

	/* AOSP AudioRecord.setPositionNotificationPeriod() */
	public int setPositionNotificationPeriod(int periodInFrames) {
		if (pcm_handle == 0)
			return ERROR_INVALID_OPERATION;
		synchronized (mPositionListenerLock) {
			mUpdatePeriod = periodInFrames;
			mNewPosition = mFramePosition + periodInFrames;
		}
		return SUCCESS;
	}

	public static final int SUCCESS = 0;
	public static final int ERROR = -1;

	public void stop() {
		if (pcm_handle != 0)
			native_stop(pcm_handle);
		recordingState = RECORDSTATE_STOPPED;
		synchronized (sActiveRecords) {
			sActiveRecords.remove(this);
		}
	}

	/* AOSP AudioRecord.finalize(): release() */
	@Override
	protected void finalize() {
		release();
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

		/* AOSP AudioRecord.Builder.setAudioPlaybackCaptureConfig() */
		private AudioPlaybackCaptureConfiguration mAudioPlaybackCaptureConfiguration;

		public Builder setAudioPlaybackCaptureConfig(AudioPlaybackCaptureConfiguration config) {
			mAudioPlaybackCaptureConfiguration = java.util.Objects.requireNonNull(config, "Illegal null AudioPlaybackCaptureConfiguration argument");
			return this;
		}

		private int mPrivacySensitive = -1; // AOSP PRIVACY_SENSITIVE_DEFAULT

		/* AOSP AudioRecord.Builder.setPrivacySensitive(): build() puts it into the attributes (FLAG_CAPTURE_PRIVATE) */
		public Builder setPrivacySensitive(boolean privacySensitive) {
			mPrivacySensitive = privacySensitive ? 1 : 0;
			return this;
		}

		public AudioRecord build() {
			if (mPrivacySensitive != -1) {
				if (audioSource == 8 /* REMOTE_SUBMIX */ || audioSource == 1998 /* RADIO_TUNER */
				    || audioSource == 3 /* VOICE_DOWNLINK */ || audioSource == 2 /* VOICE_UPLINK */
				    || audioSource == 4 /* VOICE_CALL */ || audioSource == 1997 /* ECHO_REFERENCE */) {
					throw new UnsupportedOperationException("Cannot request private capture with source: " + audioSource);
				}
			}
			if (mAudioPlaybackCaptureConfiguration != null) // AOSP buildAudioPlaybackCaptureRecord(): an audio policy of the projection; none exists on ATL
				throw new UnsupportedOperationException("audio playback capture is not provided");
			AudioRecord record = new AudioRecord(audioSource, audioFormat.sampleRate, audioFormat.channelMask, audioFormat.encoding,
			                                     mBufferSizeInBytes != 0 ? mBufferSizeInBytes : 32768);
			record.mPrivacySensitive = mPrivacySensitive == 1;
			return record;
		}
	}
}
