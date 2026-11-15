package android.hardware;

import android.util.Log;

/* AOSP core/java/android/hardware/ConsumerIrManager.java on a device without an IR emitter: there is no
 * consumer IR service (mService == null), so AOSP's methods take their "no consumer ir service" paths. */
public final class ConsumerIrManager {
	private static final String TAG = "ConsumerIr";

	public boolean hasIrEmitter() {
		Log.w(TAG, "no consumer ir service.");
		return false;
	}

	public void transmit(int carrierFrequency, int[] pattern) {
		Log.w(TAG, "failed to transmit; no consumer ir service.");
	}

	public final class CarrierFrequencyRange {
		private final int mMinFrequency;
		private final int mMaxFrequency;

		public CarrierFrequencyRange(int min, int max) {
			mMinFrequency = min;
			mMaxFrequency = max;
		}

		public int getMinFrequency() {
			return mMinFrequency;
		}

		public int getMaxFrequency() {
			return mMaxFrequency;
		}
	}

	public CarrierFrequencyRange[] getCarrierFrequencies() {
		Log.w(TAG, "no consumer ir service.");
		return null;
	}
}
