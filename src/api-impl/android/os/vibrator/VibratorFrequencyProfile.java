package android.os.vibrator;

import android.util.Range;
import android.util.SparseArray;

/* AOSP android.os.vibrator.VibratorFrequencyProfile: the frequency response of a vibrator with frequency control.
 * ATL's vibrator has none (Vibrator.getFrequencyProfile() returns null), so an instance is never handed out: AOSP's
 * constructor from VibratorInfo.FrequencyProfile (no VibratorInfo in ATL) is left out and the getters give the
 * values of AOSP's empty frequency profile (NaN, no range). */
public final class VibratorFrequencyProfile {

	VibratorFrequencyProfile() {}

	public SparseArray<Float> getFrequenciesOutputAcceleration() {
		return new SparseArray<>(0);
	}

	public float getMaxOutputAccelerationGs() {
		return Float.NaN;
	}

	public Range<Float> getFrequencyRange(float minOutputAccelerationGs) {
		return null;
	}

	public float getOutputAccelerationGs(float frequencyHz) {
		return Float.NaN;
	}

	public float getMinFrequencyHz() {
		return Float.NaN;
	}

	public float getMaxFrequencyHz() {
		return Float.NaN;
	}
}
