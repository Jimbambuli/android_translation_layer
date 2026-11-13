package android.os;

import android.content.Context;

/* AOSP android.os.VibratorManager (API 31): access to all vibrators of the device. ATL has the one vibrator of
 * Context.VIBRATOR_SERVICE; AOSP's VibratorManager is abstract and the system hands out SystemVibratorManager */
public class VibratorManager {
	private final Context mContext;

	/* AOSP @hide constructor */
	public VibratorManager(Context context) {
		mContext = context;
	}

	public int[] getVibratorIds() {
		return new int[] {0};
	}

	public Vibrator getVibrator(int vibratorId) {
		return vibratorId == 0 ? getDefaultVibrator() : new NullVibrator();
	}

	public Vibrator getDefaultVibrator() {
		return (Vibrator)mContext.getSystemService(Context.VIBRATOR_SERVICE);
	}

	public void cancel() {
		getDefaultVibrator().cancel();
	}
}
