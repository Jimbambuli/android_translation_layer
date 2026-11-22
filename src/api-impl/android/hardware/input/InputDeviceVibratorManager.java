package android.hardware.input;

import android.os.CombinedVibration;
import android.os.NullVibrator;
import android.os.VibrationAttributes;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.util.SparseArray;
import android.view.InputDevice;

/* AOSP InputDeviceVibratorManager (@hide): the vibrators of an input device, one per FF_RUMBLE channel (0 strong,
 * 1 weak); a CombinedVibration plays per channel */
public class InputDeviceVibratorManager extends VibratorManager implements InputManager.InputDeviceListener {
	private final InputManagerGlobal mGlobal;
	private final int mDeviceId;
	private final int mToken;
	private final SparseArray<Vibrator> mVibrators = new SparseArray<>();

	public InputDeviceVibratorManager(int deviceId) {
		super(null);
		mGlobal = InputManagerGlobal.getInstance();
		mDeviceId = deviceId;
		mToken = mGlobal.newVibratorToken();
		initializeVibrators();
	}

	private void initializeVibrators() {
		synchronized (mVibrators) {
			mVibrators.clear();
			for (int vibratorId : mGlobal.getVibratorIds(mDeviceId))
				mVibrators.put(vibratorId, new InputDeviceVibrator(mDeviceId, vibratorId));
		}
	}

	@Override
	public void onInputDeviceAdded(int deviceId) {
	}

	@Override
	public void onInputDeviceRemoved(int deviceId) {
		synchronized (mVibrators) {
			if (deviceId == mDeviceId)
				mVibrators.clear();
		}
	}

	@Override
	public void onInputDeviceChanged(int deviceId) {
		if (deviceId == mDeviceId)
			initializeVibrators();
	}

	@Override
	public int[] getVibratorIds() {
		synchronized (mVibrators) {
			int[] vibratorIds = new int[mVibrators.size()];
			for (int i = 0; i < mVibrators.size(); i++)
				vibratorIds[i] = mVibrators.keyAt(i);
			return vibratorIds;
		}
	}

	@Override
	public Vibrator getVibrator(int vibratorId) {
		synchronized (mVibrators) {
			Vibrator vibrator = mVibrators.get(vibratorId);
			if (vibrator != null)
				return vibrator;
		}
		return new NullVibrator();
	}

	@Override
	public Vibrator getDefaultVibrator() {
		synchronized (mVibrators) {
			if (mVibrators.size() > 0)
				return mVibrators.valueAt(0);
		}
		return new NullVibrator();
	}

	@Override
	public void vibrate(int uid, String opPkg, CombinedVibration effect, String reason, VibrationAttributes attributes) {
		mGlobal.vibrate(mDeviceId, effect, mToken);
	}

	@Override
	public void cancel() {
		mGlobal.cancelVibrate(mDeviceId, mToken);
	}
}
