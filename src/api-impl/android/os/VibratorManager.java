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

	/* AOSP vibrate(CombinedVibration), vibrate(CombinedVibration, VibrationAttributes) */
	public final void vibrate(CombinedVibration effect) {
		vibrate(effect, null);
	}

	public final void vibrate(CombinedVibration effect, VibrationAttributes attributes) {
		vibrate(Process.myUid(), mContext != null ? mContext.getPackageName() : null, effect, null, attributes);
	}

	/* AOSP @hide (SystemVibratorManager for the device's vibrators): ATL's one vibrator (id 0) plays its effect of
	 * the combination, the effect of a vibrator that does not exist is not played */
	public void vibrate(int uid, String opPkg, CombinedVibration effect, String reason, VibrationAttributes attributes) {
		effect.validate();
		VibrationEffect vibratorEffect = null;
		if (effect instanceof CombinedVibration.Mono)
			vibratorEffect = ((CombinedVibration.Mono)effect).getEffect();
		else if (effect instanceof CombinedVibration.Stereo)
			vibratorEffect = ((CombinedVibration.Stereo)effect).getEffects().get(0);
		if (vibratorEffect != null)
			getDefaultVibrator().vibrate(vibratorEffect);
	}
}
