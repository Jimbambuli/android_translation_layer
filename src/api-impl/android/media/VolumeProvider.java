package android.media;

/* AOSP android.media.VolumeProvider (without the hidden callback to the session; ATL has no remote playback) */
public abstract class VolumeProvider {
	public static final int VOLUME_CONTROL_FIXED = 0;
	public static final int VOLUME_CONTROL_RELATIVE = 1;
	public static final int VOLUME_CONTROL_ABSOLUTE = 2;

	private final int mControlType;
	private final int mMaxVolume;
	private final String mControlId;
	private int mCurrentVolume;

	public VolumeProvider(int volumeControl, int maxVolume, int currentVolume) {
		this(volumeControl, maxVolume, currentVolume, null);
	}

	public VolumeProvider(int volumeControl, int maxVolume, int currentVolume, String volumeControlId) {
		mControlType = volumeControl;
		mMaxVolume = maxVolume;
		mCurrentVolume = currentVolume;
		mControlId = volumeControlId;
	}

	public final int getVolumeControl() {
		return mControlType;
	}

	public final int getMaxVolume() {
		return mMaxVolume;
	}

	public final int getCurrentVolume() {
		return mCurrentVolume;
	}

	public final void setCurrentVolume(int currentVolume) {
		mCurrentVolume = currentVolume;
	}

	public final String getVolumeControlId() {
		return mControlId;
	}

	public void onSetVolumeTo(int volume) {
	}

	public void onAdjustVolume(int direction) {
	}
}
