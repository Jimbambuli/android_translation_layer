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
}
