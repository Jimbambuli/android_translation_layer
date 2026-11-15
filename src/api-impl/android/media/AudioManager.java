package android.media;

import android.os.Handler;

public class AudioManager {
	public static final String PROPERTY_OUTPUT_FRAMES_PER_BUFFER = "android.media.property.OUTPUT_FRAMES_PER_BUFFER";
	public static final String PROPERTY_OUTPUT_SAMPLE_RATE = "android.media.property.OUTPUT_SAMPLE_RATE";

	public static final int STREAM_MUSIC = 0x3;

	public static final String ACTION_HDMI_AUDIO_PLUG = "android.media.action.HDMI_AUDIO_PLUG";
	public static final String EXTRA_AUDIO_PLUG_STATE = "android.media.extra.AUDIO_PLUG_STATE";

	/* AOSP AudioService sends ACTION_HDMI_AUDIO_PLUG as a sticky broadcast at boot; ATL has no HDMI audio
	 * output, so a receiver that registers for it gets the sticky reporting it unplugged (state 0), as on a
	 * device without HDMI. */
	public static android.content.Intent hdmiAudioPlugStickyIntent() {
		android.content.Intent intent = new android.content.Intent(ACTION_HDMI_AUDIO_PLUG);
		intent.putExtra(EXTRA_AUDIO_PLUG_STATE, 0);
		return intent;
	}

	private native void nativeSetStreamVolume(int volume);
	private native int nativeGetStreamVolume();
	private native boolean nativeIsStreamMute();

	public boolean isBluetoothA2dpOn() {
		return false;
	}

	public String getProperty(String name) {
		switch (name) {
			case PROPERTY_OUTPUT_FRAMES_PER_BUFFER:
				return "256"; // FIXME arbitrary
			case PROPERTY_OUTPUT_SAMPLE_RATE:
				return "44100"; // FIXME arbitrary
			default:
				System.out.println("AudioManager.getProperty: >" + name + "< not handled");
				return "";
		}
	}

	/* hidden in AOSP (@UnsupportedAppUsage), used by cubeb (Firefox): the output latency in ms; FIXME arbitrary like
	 * the properties above (a few buffers of PROPERTY_OUTPUT_FRAMES_PER_BUFFER) */
	public int getOutputLatency(int streamType) {
		return 20;
	}

	/* AOSP AudioManager.OnAudioFocusChangeListener */
	public interface OnAudioFocusChangeListener {
		public void onAudioFocusChange(int focusChange);
	}

	public static final int AUDIOFOCUS_NONE = 0;
	public static final int AUDIOFOCUS_GAIN = 1;
	public static final int AUDIOFOCUS_GAIN_TRANSIENT = 2;
	public static final int AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK = 3;
	public static final int AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE = 4;

	public static final int AUDIOFOCUS_LOSS = -1 * AUDIOFOCUS_GAIN;
	public static final int AUDIOFOCUS_LOSS_TRANSIENT = -1 * AUDIOFOCUS_GAIN_TRANSIENT;
	public static final int AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK = -1 * AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK;

	public static final int AUDIOFOCUS_FLAG_DELAY_OK = 1 << 0;
	public static final int AUDIOFOCUS_FLAG_PAUSES_ON_DUCKABLE_LOSS = 1 << 1;
	public static final int AUDIOFOCUS_FLAG_LOCK = 1 << 2;

	public static final int AUDIOFOCUS_REQUEST_FAILED = 0;
	public static final int AUDIOFOCUS_REQUEST_GRANTED = 1;
	public static final int AUDIOFOCUS_REQUEST_DELAYED = 2;

	public static final int ADJUST_LOWER = -1;
	public static final int ADJUST_SAME = 0;
	public static final int ADJUST_RAISE = 1;
	public static final int ADJUST_MUTE = -100;
	public static final int ADJUST_UNMUTE = 100;
	public static final int ADJUST_TOGGLE_MUTE = 101;

	public static final int GET_DEVICES_INPUTS = 0x0001;
	public static final int GET_DEVICES_OUTPUTS = 0x0002;
	public static final int GET_DEVICES_ALL = GET_DEVICES_OUTPUTS | GET_DEVICES_INPUTS;

	public int getRingerMode() {
		return 0;
	}

	/* AOSP AudioManager.getStreamVolume(): the volume index of the stream. ATL has the one volume of the
	 * ALSA «Master» control (0 to getStreamMaxVolume() = 100) for every stream; without that control it is the
	 * last value setStreamVolume() was given, 0 before */
	public int getStreamVolume(int streamType) {
		int volume = nativeGetStreamVolume();
		return volume >= 0 ? volume : mLastStreamVolume;
	}

	public int getStreamMaxVolume(int streamType) {
		return 100;
	}

	public int requestAudioFocus(OnAudioFocusChangeListener listener, int streamType, int durationHint) {
		return /*AUDIOFOCUS_REQUEST_GRANTED*/ 1;
	}

	public int abandonAudioFocus(OnAudioFocusChangeListener listener) {
		return /*AUDIOFOCUS_REQUEST_GRANTED*/ 1;
	}

	public int requestAudioFocus(AudioFocusRequest focusRequest) {
		if (focusRequest == null) {
			throw new NullPointerException("Illegal null AudioFocusRequest");
		}
		return AUDIOFOCUS_REQUEST_GRANTED;
	}

	public int abandonAudioFocusRequest(AudioFocusRequest focusRequest) {
		if (focusRequest == null) {
			throw new IllegalArgumentException("Illegal null AudioFocusRequest");
		}
		return AUDIOFOCUS_REQUEST_GRANTED;
	}

	/**
	 * Sets a variable number of parameter values to audio hardware.
	 *
	 * @param keyValuePairs list of parameters key value pairs in the form:
	 *    key1=value1;key2=value2;...
	 *
	 * AOSP hands them to the audio HAL (AudioSystem.setParameters()); PulseAudio has none of them
	 */
	public void setParameters(String keyValuePairs) {}

	public boolean isWiredHeadsetOn() {
		return false;
	}

	private int mLastStreamVolume;

	public void setStreamVolume(int streamType, int index, int flags) {
		mLastStreamVolume = Math.max(0, Math.min(index, getStreamMaxVolume(streamType)));
		nativeSetStreamVolume(index);
	}

	/* AOSP: AudioService.isVolumeFixed() is config_useFixedVolume, false; ATL sets the volume (nativeSetStreamVolume) */
	public boolean isVolumeFixed() {
		return false;
	}

	/* AOSP AudioManager.isStreamMute(): whether the stream is muted. ATL has the mute switch of the ALSA
	 * «Master» control for every stream; false without it */
	public boolean isStreamMute(int streamType) {
		return mStreamMuted || nativeIsStreamMute();
	}

	private boolean mStreamMuted;

	/* AOSP AudioManager.adjustStreamVolume(): moves the volume index of the stream by one step in the given
	 * direction, or mutes/unmutes it. ATL has one volume for every stream, so this goes through
	 * setStreamVolume() and the mute is only remembered (isStreamMute()); no hardware mute is switched */
	public void adjustStreamVolume(int streamType, int direction, int flags) {
		switch (direction) {
		case ADJUST_RAISE:
			setStreamVolume(streamType, getStreamVolume(streamType) + 1, flags);
			break;
		case ADJUST_LOWER:
			setStreamVolume(streamType, getStreamVolume(streamType) - 1, flags);
			break;
		case ADJUST_MUTE:
			mStreamMuted = true;
			break;
		case ADJUST_UNMUTE:
			mStreamMuted = false;
			break;
		case ADJUST_TOGGLE_MUTE:
			mStreamMuted = !mStreamMuted;
			break;
		default:
			break;
		}
	}

	/* AOSP AudioManager.setStreamMute() (deprecated): remembered only, see adjustStreamVolume() */
	public void setStreamMute(int streamType, boolean state) {
		mStreamMuted = state;
	}

	public boolean isMusicActive() {
		return false;
	}

	public void setSpeakerphoneOn(boolean on) {}

	public boolean isSpeakerphoneOn() {
		return false;
	}

	public void setBluetoothScoOn(boolean on) {}

	public boolean isBluetoothScoOn() {
		return false;
	}

	/* AOSP AudioManager.startBluetoothSco(): ATL has no Bluetooth, the audio service finds no SCO device and the
	 * connection is not made (isBluetoothScoOn() stays false) */
	public void startBluetoothSco() {}

	public void stopBluetoothSco() {}

	/* AOSP AudioManager.isBluetoothScoAvailableOffCall(): the resource config_bluetooth_sco_off_call, true in AOSP's
	 * core/res/res/values/config.xml */
	public boolean isBluetoothScoAvailableOffCall() {
		return true;
	}

	/* AOSP AudioManager.dispatchMediaKeyEvent() -> MediaSessionLegacyHelper.sendMediaButtonEvent(keyEvent, false)
	 * -> MediaSessionManager.dispatchMediaKeyEvent(keyEvent, false): to the media button session */
	public void dispatchMediaKeyEvent(android.view.KeyEvent keyEvent) {
		if (keyEvent == null) {
			android.util.Log.w("MediaSessionHelper", "Tried to send a null key event. Ignoring.");
			return;
		}
		new android.media.session.MediaSessionManager().dispatchMediaKeyEvent(keyEvent, false);
	}

	public void setMode(int mode) {}

	public int getMode() {
		return /*MODE_NORMAL*/ 0;
	}

	public boolean isMicrophoneMute() {
		return false;
	}

	public void setMicrophoneMute(boolean on) {
		System.out.println("AudioManager.setMicrophoneMute(" + on + ")");
	}
	public void unloadSoundEffects() {}

	/* AOSP AudioManager.generateAudioSessionId(): a new session id, > 0 (AudioSystem.newAudioSessionId()) */
	private static final java.util.concurrent.atomic.AtomicInteger sNextAudioSessionId = new java.util.concurrent.atomic.AtomicInteger(1);

	public int generateAudioSessionId() {
		return sNextAudioSessionId.getAndIncrement();
	}

	/* AOSP AudioManager.getDevices(): the audio devices of the requested direction. ATL has no audio policy, the
	 * devices are the default ALSA output (speaker) and input (microphone) the app plays on and records from */
	public AudioDeviceInfo[] getDevices(int flags) {
		java.util.ArrayList<AudioDeviceInfo> devices = new java.util.ArrayList<>();
		if ((flags & GET_DEVICES_OUTPUTS) != 0) {
			devices.add(new AudioDeviceInfo(1, AudioDeviceInfo.TYPE_BUILTIN_SPEAKER, "default", true));
		}
		if ((flags & GET_DEVICES_INPUTS) != 0) {
			devices.add(new AudioDeviceInfo(2, AudioDeviceInfo.TYPE_BUILTIN_MIC, "default", false));
		}
		return devices.toArray(new AudioDeviceInfo[0]);
	}

	/* AOSP AudioManager.getParameters(): the audio HAL's values of the keys ("key=value;..."). ATL has no HAL,
	 * which answers as one that knows none of the keys: an empty string */
	public String getParameters(String keys) {
		return "";
	}

	/* AOSP AudioManager.getAudioDevicesForAttributes() (API 33): the output devices the audio policy routes the
	 * attributes to; ATL plays everything on the one default output */
	public java.util.List<AudioDeviceInfo> getAudioDevicesForAttributes(AudioAttributes attributes) {
		java.util.Objects.requireNonNull(attributes);
		return new java.util.ArrayList<>(java.util.Arrays.asList(getDevices(GET_DEVICES_OUTPUTS)));
	}

	/* AOSP AudioManager.registerAudioDeviceCallback(): a new callback hears of the devices that are there
	 * (onAudioDevicesAdded() on the handler's thread, else the main thread); ATL's devices never change, so
	 * nothing follows */
	public void registerAudioDeviceCallback(final AudioDeviceCallback callback, Handler handler) {
		if (callback == null)
			return;
		final AudioDeviceInfo[] devices = getDevices(GET_DEVICES_ALL);
		(handler != null ? handler : new Handler(android.os.Looper.getMainLooper())).post(new Runnable() {
			@Override
			public void run() {
				callback.onAudioDevicesAdded(devices);
			}
		});
	}

	public void unregisterAudioDeviceCallback(AudioDeviceCallback callback) {}

	/* AOSP AudioManager.AudioRecordingCallback */
	public static abstract class AudioRecordingCallback {
		public void onRecordingConfigChanged(java.util.List<AudioRecordingConfiguration> configs) {}
	}

	/* AOSP AudioManager.getActiveRecordingConfigurations(): AudioService's list of active recordings as an
	 * unprivileged app sees it; ATL knows the app's own AudioRecords */
	public java.util.List<AudioRecordingConfiguration> getActiveRecordingConfigurations() {
		return AudioRecord.activeRecordingConfigurations();
	}

	/* AOSP AudioManager.getAvailableCommunicationDevices()/getCommunicationDevice()/setCommunicationDevice():
	 * the devices usable for calls among the audio service's outputs (ATL: the speaker), the one in use (the
	 * speaker until another is chosen) and its change. The OnCommunicationDeviceChangedListeners are told of a
	 * change on their executor. */
	public interface OnCommunicationDeviceChangedListener {
		void onCommunicationDeviceChanged(AudioDeviceInfo device);
	}

	private AudioDeviceInfo mCommunicationDevice;
	private final java.util.Map<OnCommunicationDeviceChangedListener, java.util.concurrent.Executor> mCommunicationListeners = new java.util.LinkedHashMap<>();

	public java.util.List<AudioDeviceInfo> getAvailableCommunicationDevices() {
		return new java.util.ArrayList<>(java.util.Arrays.asList(getDevices(GET_DEVICES_OUTPUTS)));
	}

	public synchronized AudioDeviceInfo getCommunicationDevice() {
		return mCommunicationDevice != null ? mCommunicationDevice : getAvailableCommunicationDevices().get(0);
	}

	public boolean setCommunicationDevice(AudioDeviceInfo device) {
		java.util.Objects.requireNonNull(device);
		if (device.getId() == 0) {
			return false;
		}
		boolean available = false;
		for (AudioDeviceInfo d : getAvailableCommunicationDevices()) {
			available |= d.getId() == device.getId();
		}
		if (!available) {
			return false;
		}
		final AudioDeviceInfo chosen = device;
		java.util.Map<OnCommunicationDeviceChangedListener, java.util.concurrent.Executor> listeners;
		synchronized (this) {
			if (getCommunicationDevice().getId() == chosen.getId()) {
				mCommunicationDevice = chosen;
				return true;
			}
			mCommunicationDevice = chosen;
			listeners = new java.util.LinkedHashMap<>(mCommunicationListeners);
		}
		for (final java.util.Map.Entry<OnCommunicationDeviceChangedListener, java.util.concurrent.Executor> e : listeners.entrySet()) {
			e.getValue().execute(() -> e.getKey().onCommunicationDeviceChanged(chosen));
		}
		return true;
	}

	public void clearCommunicationDevice() {
		synchronized (this) {
			mCommunicationDevice = null;
		}
	}

	public synchronized void addOnCommunicationDeviceChangedListener(java.util.concurrent.Executor executor, OnCommunicationDeviceChangedListener listener) {
		java.util.Objects.requireNonNull(executor);
		java.util.Objects.requireNonNull(listener);
		if (mCommunicationListeners.containsKey(listener)) {
			throw new IllegalArgumentException("attempt to call addOnCommunicationDeviceChangedListener() on a previously registered listener");
		}
		mCommunicationListeners.put(listener, executor);
	}

	public synchronized void removeOnCommunicationDeviceChangedListener(OnCommunicationDeviceChangedListener listener) {
		java.util.Objects.requireNonNull(listener);
		mCommunicationListeners.remove(listener);
	}

	/* AOSP AudioManager.AudioPlaybackCallback/AudioRecordingCallback and their (un)registration: the callbacks hear
	 * of changes in the audio service's playback/recording configurations, ATL has no such service and never
	 * calls them (a registration alone triggers nothing in AOSP either) */
	public static abstract class AudioPlaybackCallback {
		public void onPlaybackConfigChanged(java.util.List<?> configs) {}
	}

	public void registerAudioPlaybackCallback(AudioPlaybackCallback cb, Handler handler) {
		if (cb == null) {
			throw new IllegalArgumentException("Illegal null AudioPlaybackCallback argument");
		}
	}

	public void unregisterAudioPlaybackCallback(AudioPlaybackCallback cb) {
		if (cb == null) {
			throw new IllegalArgumentException("Illegal null AudioPlaybackCallback argument");
		}
	}

	public void registerAudioRecordingCallback(AudioRecordingCallback cb, Handler handler) {
		if (cb == null) {
			throw new IllegalArgumentException("Illegal null AudioRecordingCallback argument");
		}
	}

	public void unregisterAudioRecordingCallback(AudioRecordingCallback cb) {
		if (cb == null) {
			throw new IllegalArgumentException("Illegal null AudioRecordingCallback argument");
		}
	}

	/* AOSP AudioManager.FX_KEY_CLICK: the standard key click sound */
	public static final int FX_KEY_CLICK = 0;

	public void playSoundEffect(int effectType) {}

	/* AOSP AudioManager.playSoundEffect(int, float): like playSoundEffect(int), with a volume */
	public void playSoundEffect(int effectType, float volume) {
		playSoundEffect(effectType);
	}

	/* API 32 */
	public Spatializer getSpatializer() {
		return new Spatializer();
	}

	/* API 29/31: no compressed offload, all playback is decoded */
	public static boolean isOffloadedPlaybackSupported(AudioFormat format, AudioAttributes attributes) {
		return false;
	}

	public static final int PLAYBACK_OFFLOAD_NOT_SUPPORTED = 0;

	public static int getPlaybackOffloadSupport(AudioFormat format, AudioAttributes attributes) {
		return PLAYBACK_OFFLOAD_NOT_SUPPORTED;
	}

	/* API 33: no output plays the format directly (no compressed passthrough or offload), as in
	 * isOffloadedPlaybackSupported() */
	public static final int DIRECT_PLAYBACK_NOT_SUPPORTED = 0;

	public static int getDirectPlaybackSupport(AudioFormat format, AudioAttributes attributes) {
		java.util.Objects.requireNonNull(format);
		java.util.Objects.requireNonNull(attributes);
		return DIRECT_PLAYBACK_NOT_SUPPORTED;
	}

	/* AOSP AudioManager.getDirectProfilesForAttributes() (API 33): AudioSystem lists the direct output profiles of the
	 * audio policy; ATL's PulseAudio output has none (see getDirectPlaybackSupport()), so the list is empty */
	public @android.annotation.NonNull java.util.List<AudioProfile> getDirectProfilesForAttributes(@android.annotation.NonNull AudioAttributes attributes) {
		java.util.Objects.requireNonNull(attributes);
		return new java.util.ArrayList<>();
	}

	/* API 28 */
	public int getStreamMinVolume(int streamType) {
		return 0;
	}

	public static final int RINGER_MODE_SILENT = 0;
	public static final int RINGER_MODE_VIBRATE = 1;
	public static final int RINGER_MODE_NORMAL = 2;
	private static final int RINGER_MODE_MAX = RINGER_MODE_NORMAL;

	/* AOSP AudioManager.setRingerMode(): ignores invalid modes (isValidRingerMode(): range check, then
	 * AudioService), else AudioService.setRingerModeExternal(). ATL has no ringer, getRingerMode() stays */
	public void setRingerMode(int ringerMode) {
		if (ringerMode < 0 || ringerMode > RINGER_MODE_MAX) {
			return;
		}
	}

	/* AOSP AudioManager.adjustVolume()/adjustSuggestedStreamVolume(): MediaSessionLegacyHelper.sendAdjustVolumeBy()
	 * adjusts the stream the media session service picks. ATL has one volume for every stream
	 * (adjustStreamVolume()), so the stream choice makes no difference */
	public void adjustVolume(int direction, int flags) {
		adjustStreamVolume(STREAM_MUSIC, direction, flags);
	}

	public void adjustSuggestedStreamVolume(int direction, int suggestedStreamType, int flags) {
		adjustStreamVolume(suggestedStreamType, direction, flags);
	}

	/* AOSP AudioManager.setWiredHeadsetOn(): deprecated, empty body */
	@Deprecated
	public void setWiredHeadsetOn(boolean on) {}

	/* AOSP AudioManager.loadSoundEffects() -> AudioService.loadSoundEffects(); ATL plays no sound effects
	 * (playSoundEffect()), so there is nothing to load, like unloadSoundEffects() */
	public void loadSoundEffects() {}

	/* AOSP AudioManager.isHapticPlaybackSupported() -> AudioSystem: whether the audio HAL has an output with
	 * haptic channels. A desktop has none */
	public static boolean isHapticPlaybackSupported() {
		return false;
	}

	/* AOSP AudioManager.setAllowedCapturePolicy()/getAllowedCapturePolicy(): AudioService keeps the policy of the
	 * app's uid, ALLOW_CAPTURE_BY_ALL until it is set */
	private int mAllowedCapturePolicy = AudioAttributes.ALLOW_CAPTURE_BY_ALL;

	public void setAllowedCapturePolicy(int capturePolicy) {
		mAllowedCapturePolicy = capturePolicy;
	}

	public int getAllowedCapturePolicy() {
		return mAllowedCapturePolicy;
	}

	/* AOSP AudioManager.OnModeChangedListener, add/removeOnModeChangedListener() (CallbackUtil.addListener()/
	 * removeListener()). ATL's mode is always MODE_NORMAL (setMode() changes nothing), so no listener is called */
	public interface OnModeChangedListener {
		void onModeChanged(int mode);
	}

	private final java.util.ArrayList<OnModeChangedListener> mModeChangedListeners = new java.util.ArrayList<>();

	public void addOnModeChangedListener(java.util.concurrent.Executor executor, OnModeChangedListener listener) {
		java.util.Objects.requireNonNull(executor);
		java.util.Objects.requireNonNull(listener);
		synchronized (mModeChangedListeners) {
			if (mModeChangedListeners.contains(listener)) {
				throw new IllegalArgumentException("attempt to call addOnModeChangedListener"
				                                   + "on a previously registered listener");
			}
			mModeChangedListeners.add(listener);
		}
	}

	public void removeOnModeChangedListener(OnModeChangedListener listener) {
		java.util.Objects.requireNonNull(listener);
		synchronized (mModeChangedListeners) {
			if (!mModeChangedListeners.remove(listener)) {
				throw new IllegalArgumentException("attempt to call removeOnModeChangedListener"
				                                   + " on an unregistered listener");
			}
		}
	}

	/* AOSP AudioManager.getMicrophones(): AudioSystem.getMicrophones() asks the audio HAL for microphone
	 * characteristics. ATL has no HAL, which takes AOSP's fallback (addMicrophonesFromAudioDeviceInfo() with
	 * microphoneInfoFromAudioDeviceInfo()): one MicrophoneInfo with unknown characteristics per input device of
	 * getDevices() except TYPE_TELEPHONY. The port name is the device's product name; the port type is the
	 * internal device of TYPE_BUILTIN_MIC (AUDIO_DEVICE_IN_BUILTIN_MIC), ATL's only input */
	public java.util.List<MicrophoneInfo> getMicrophones() throws java.io.IOException {
		java.util.ArrayList<MicrophoneInfo> microphones = new java.util.ArrayList<MicrophoneInfo>();
		for (AudioDeviceInfo deviceInfo : getDevices(GET_DEVICES_INPUTS)) {
			int deviceType = deviceInfo.getType();
			if (deviceType == AudioDeviceInfo.TYPE_TELEPHONY) {
				continue;
			}
			int micLocation = (deviceType == AudioDeviceInfo.TYPE_BUILTIN_MIC) ? MicrophoneInfo.LOCATION_MAINBODY
			                  : deviceType == AudioDeviceInfo.TYPE_UNKNOWN ? MicrophoneInfo.LOCATION_UNKNOWN
			                  : MicrophoneInfo.LOCATION_PERIPHERAL;
			MicrophoneInfo microphone = new MicrophoneInfo(
			    deviceInfo.getProductName().toString() + deviceInfo.getId(),
			    0x80000004 /* AUDIO_DEVICE_IN_BUILTIN_MIC */, deviceInfo.getAddress(), micLocation,
			    MicrophoneInfo.GROUP_UNKNOWN, MicrophoneInfo.INDEX_IN_THE_GROUP_UNKNOWN,
			    MicrophoneInfo.POSITION_UNKNOWN, MicrophoneInfo.ORIENTATION_UNKNOWN,
			    new java.util.ArrayList<android.util.Pair<Float, Float>>(),
			    new java.util.ArrayList<android.util.Pair<Integer, Integer>>(),
			    MicrophoneInfo.SENSITIVITY_UNKNOWN, MicrophoneInfo.SPL_UNKNOWN,
			    MicrophoneInfo.SPL_UNKNOWN, MicrophoneInfo.DIRECTIONALITY_UNKNOWN);
			microphone.setId(deviceInfo.getId());
			microphones.add(microphone);
		}
		return microphones;
	}

	/* AOSP AudioManager.registerMediaButtonEventReceiver()/unregisterMediaButtonEventReceiver() (deprecated):
	 * through MediaSessionLegacyHelper, which keeps an active MediaSession per PendingIntent that forwards the
	 * media buttons to it. The context is the application's (AOSP: the AudioManager's context) */
	@Deprecated
	public void registerMediaButtonEventReceiver(android.content.ComponentName eventReceiver) {
		if (eventReceiver == null) {
			return;
		}
		android.content.Context context = android.app.ActivityThread.currentApplication();
		if (!eventReceiver.getPackageName().equals(context.getPackageName())) {
			android.util.Log.e("AudioManager", "registerMediaButtonEventReceiver() error: "
			                                   + "receiver and context package names don't match");
			return;
		}
		android.content.Intent mediaButtonIntent = new android.content.Intent(android.content.Intent.ACTION_MEDIA_BUTTON);
		mediaButtonIntent.setComponent(eventReceiver);
		android.app.PendingIntent pi = android.app.PendingIntent.getBroadcast(context, 0, mediaButtonIntent,
		                                                                      android.app.PendingIntent.FLAG_IMMUTABLE);
		registerMediaButtonIntent(pi, eventReceiver);
	}

	@Deprecated
	public void registerMediaButtonEventReceiver(android.app.PendingIntent eventReceiver) {
		if (eventReceiver == null) {
			return;
		}
		registerMediaButtonIntent(eventReceiver, null);
	}

	public void registerMediaButtonIntent(android.app.PendingIntent pi, android.content.ComponentName eventReceiver) {
		if (pi == null) {
			android.util.Log.e("AudioManager", "Cannot call registerMediaButtonIntent() with a null parameter");
			return;
		}
		android.content.Context context = android.app.ActivityThread.currentApplication();
		android.media.session.MediaSessionLegacyHelper helper = android.media.session.MediaSessionLegacyHelper.getHelper(context);
		helper.addMediaButtonListener(pi, eventReceiver, context);
	}

	@Deprecated
	public void unregisterMediaButtonEventReceiver(android.content.ComponentName eventReceiver) {
		if (eventReceiver == null) {
			return;
		}
		android.content.Context context = android.app.ActivityThread.currentApplication();
		android.content.Intent mediaButtonIntent = new android.content.Intent(android.content.Intent.ACTION_MEDIA_BUTTON);
		mediaButtonIntent.setComponent(eventReceiver);
		android.app.PendingIntent pi = android.app.PendingIntent.getBroadcast(context, 0, mediaButtonIntent,
		                                                                      android.app.PendingIntent.FLAG_IMMUTABLE);
		unregisterMediaButtonIntent(pi);
	}

	@Deprecated
	public void unregisterMediaButtonEventReceiver(android.app.PendingIntent eventReceiver) {
		if (eventReceiver == null) {
			return;
		}
		unregisterMediaButtonIntent(eventReceiver);
	}

	public void unregisterMediaButtonIntent(android.app.PendingIntent pi) {
		android.media.session.MediaSessionLegacyHelper helper =
		    android.media.session.MediaSessionLegacyHelper.getHelper(android.app.ActivityThread.currentApplication());
		helper.removeMediaButtonListener(pi);
	}
    /* AOSP 16 API; no backing hardware/service in ATL */
    public void registerRemoteControlClient(android.media.RemoteControlClient arg0) {  }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public void unregisterRemoteControlClient(android.media.RemoteControlClient arg0) {  }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public java.util.List<android.media.AudioPlaybackConfiguration> getActivePlaybackConfigurations() { return new java.util.ArrayList<>(); }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public float getStreamVolumeDb(int arg0, int arg1, int arg2) { return 0f; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public void addOnPreferredMixerAttributesChangedListener(java.util.concurrent.Executor arg0, android.media.AudioManager.OnPreferredMixerAttributesChangedListener arg1) {  }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public void adjustVolumeGroupVolume(int arg0, int arg1, int arg2) {  }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public boolean clearPreferredMixerAttributes(android.media.AudioAttributes arg0, android.media.AudioDeviceInfo arg1) { return false; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public int getAudioHwSyncForSession(int arg0) { return 0; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public int getEncodedSurroundMode() { return 0; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public android.media.AudioMixerAttributes getPreferredMixerAttributes(android.media.AudioAttributes arg0, android.media.AudioDeviceInfo arg1) { return null; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public int getRouting(int arg0) { return 0; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public java.util.Set<java.lang.Integer> getSupportedDeviceTypes(int arg0) { return new java.util.HashSet<>(); }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public java.util.List<android.media.AudioMixerAttributes> getSupportedMixerAttributes(android.media.AudioDeviceInfo arg0) { return new java.util.ArrayList<>(); }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public int getVibrateSetting(int arg0) { return 0; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public int getVolumeGroupIdForAttributes(android.media.AudioAttributes arg0) { return 0; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public boolean isCallScreeningModeSupported() { return false; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public boolean isRampingRingerEnabled() { return false; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public boolean isSurroundFormatEnabled(int arg0) { return false; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public boolean isVolumeGroupMuted(int arg0) { return false; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public boolean registerRemoteController(android.media.RemoteController arg0) { return false; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public void removeOnPreferredMixerAttributesChangedListener(android.media.AudioManager.OnPreferredMixerAttributesChangedListener arg0) {  }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public void setBluetoothA2dpOn(boolean arg0) {  }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public boolean setEncodedSurroundMode(int arg0) { return false; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public boolean setPreferredMixerAttributes(android.media.AudioAttributes arg0, android.media.AudioDeviceInfo arg1, android.media.AudioMixerAttributes arg2) { return false; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public void setRouting(int arg0, int arg1, int arg2) {  }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public void setStreamSolo(int arg0, boolean arg1) {  }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public boolean setSurroundFormatEnabled(int arg0, boolean arg1) { return false; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public void setVibrateSetting(int arg0, int arg1) {  }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public boolean shouldVibrate(int arg0) { return false; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public void unregisterRemoteController(android.media.RemoteController arg0) {  }

    /* AOSP 16 API; no backing hardware/service in ATL */
    public static interface OnPreferredMixerAttributesChangedListener {
        void onPreferredMixerAttributesChanged(android.media.AudioAttributes arg0, android.media.AudioDeviceInfo arg1, android.media.AudioMixerAttributes arg2);
    }
}
