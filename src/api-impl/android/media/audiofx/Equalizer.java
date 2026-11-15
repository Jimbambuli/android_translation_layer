package android.media.audiofx;

/* The constructor is an upstream ATL stub (Fossify Music Player): unlike the other effects it does not fail,
 * although ATL has no equalizer engine. Such an instance is an uninitialized AudioEffect, so the getters and
 * setters below take AOSP's path for one (media/java/android/media/audiofx/Equalizer.java: getParameter()/
 * setParameter() -> checkState() -> IllegalStateException); getPresetName() reads the preset names the AOSP
 * constructor fetches, of which there are none. */
public class Equalizer extends AudioEffect {
	private short mNumBands = 0;
	private int mNumPresets;
	private String[] mPresetNames;

	public Equalizer(int priority, int audioSession) {}

	public void usePreset(short preset) {}

	public short getNumberOfBands() {
		if (mNumBands != 0) {
			return mNumBands;
		}
		checkState("getParameter()");
		return mNumBands;
	}

	public short[] getBandLevelRange() {
		checkState("getParameter()");
		return new short[2];
	}

	public void setBandLevel(short band, short level) {
		checkState("setParameter()");
	}

	public short getBandLevel(short band) {
		checkState("getParameter()");
		return 0;
	}

	public int getCenterFreq(short band) {
		checkState("getParameter()");
		return 0;
	}

	public int[] getBandFreqRange(short band) {
		checkState("getParameter()");
		return new int[2];
	}

	public short getCurrentPreset() {
		checkState("getParameter()");
		return 0;
	}

	public short getNumberOfPresets() {
		checkState("getParameter()");
		return 0;
	}

	public String getPresetName(short preset) {
		if (preset >= 0 && preset < mNumPresets) {
			return mPresetNames[preset];
		} else {
			return "";
		}
	}
}
