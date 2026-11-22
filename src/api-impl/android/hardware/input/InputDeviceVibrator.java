package android.hardware.input;

import android.os.VibrationEffect;
import android.os.Vibrator;

/* AOSP InputDeviceVibrator (core/java/android/hardware/input/InputDeviceVibrator.java): the vibrator of an input
 * device (FF_RUMBLE of a game controller) with amplitude control. Like AOSP, a vibration plays on all motors of the
 * device, whatever the vibrator id. */
final class InputDeviceVibrator extends Vibrator {
	private final int mDeviceId;
	private final int mVibratorId;
	private final InputManagerGlobal mGlobal;
	/* the token of AOSP's Binder: cancel() stops only this vibrator's own vibration */
	private final int mToken;

	InputDeviceVibrator(int deviceId, int vibratorId) {
		super(null);
		mGlobal = InputManagerGlobal.getInstance();
		mDeviceId = deviceId;
		mVibratorId = vibratorId;
		mToken = mGlobal.newVibratorToken();
	}

	@Override
	public boolean hasVibrator() {
		return true;
	}

	/* AOSP isVibrating() (@SystemApi) */
	public boolean isVibrating() {
		return mGlobal.isVibrating(mDeviceId);
	}

	@Override
	public boolean hasAmplitudeControl() {
		return true;
	}

	@Override
	public int getId() {
		return mVibratorId;
	}

	/* AOSP Vibrator.vibrate(long), vibrate(long[], int): a one-shot or waveform effect */
	@Override
	public void vibrate(long milliseconds) {
		vibrate(VibrationEffect.createOneShot(milliseconds, VibrationEffect.DEFAULT_AMPLITUDE));
	}

	@Override
	public void vibrate(long[] pattern, int repeat) {
		if (repeat < -1 || repeat >= pattern.length)
			throw new ArrayIndexOutOfBoundsException();
		vibrate(VibrationEffect.createWaveform(pattern, repeat));
	}

	@Override
	public void vibrate(VibrationEffect effect) {
		mGlobal.vibrate(mDeviceId, effect, mToken);
	}

	@Override
	public void cancel() {
		mGlobal.cancelVibrate(mDeviceId, mToken);
	}
}
