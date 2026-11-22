package android.os;

import android.util.Slog;

public class Vibrator {
	int fd; // vibrator /dev/input/eventX

	public Vibrator() {
		fd = native_constructor();
	}

	/* for NullVibrator: no device behind it */
	Vibrator(int fd) {
		this.fd = fd;
	}

	/* AOSP @hide: a vibrator that is not the device's own (InputDeviceVibrator of a game controller) */
	protected Vibrator(android.content.Context context) {
		this(-1);
	}

	public boolean hasVibrator() {
		return true;
	}

	/**
	 * Check whether the vibrator supports all of the given primitives.
	 *
	 * <p>A vibrator without support for any primitive, like this one, returns false.
	 */
	public boolean areAllPrimitivesSupported(int... primitiveIds) {
		return false;
	}

	/* the vibration motor is only switched on and off (native_vibrate), its strength cannot be set */
	public boolean hasAmplitudeControl() {
		return false;
	}

	public void vibrate(long millis) {
		if (fd != -1)
			native_vibrate(fd, millis);
		else
			Slog.v("Vibrator", "vibration motor go burrrr for " + millis + "ms");
	}

	private Thread vibrateThread;

	public void vibrate(final long[] pattern, int repeat) {
		if (repeat < -1 || repeat >= pattern.length)
			throw new ArrayIndexOutOfBoundsException();
		vibrateThread = new Thread(new Runnable() {
			public void run() {
				for (int i = 0; i < pattern.length; i++) {
					if (i % 2 == 0)
						try {
							Thread.sleep(pattern[i]);
						} catch (InterruptedException e) {
							return;
						}
					else
						vibrate(pattern[i]);
				}
			}
		});
		vibrateThread.start();
	}

	public void vibrate(VibrationEffect effect) {
		vibrate(effect.timings, effect.repeat);
	}

	public void vibrate(VibrationEffect effect, VibrationAttributes attributes) {
		vibrate(effect);
	}

	/* AOSP turns the AudioAttributes into VibrationAttributes, which ATL does not use */
	public void vibrate(VibrationEffect effect, android.media.AudioAttributes attributes) {
		vibrate(effect);
	}

	public static final int VIBRATION_EFFECT_SUPPORT_UNKNOWN = 0;
	public static final int VIBRATION_EFFECT_SUPPORT_YES = 1;
	public static final int VIBRATION_EFFECT_SUPPORT_NO = 2;

	/* AOSP areEffectsSupported() asks VibratorInfo.isEffectSupported(); the vibrator of ATL (a motor that is only
	 * switched on and off) reports no predefined effect, like areAllPrimitivesSupported() no primitive */
	public int[] areEffectsSupported(int... effectIds) {
		int[] supported = new int[effectIds.length];
		java.util.Arrays.fill(supported, VIBRATION_EFFECT_SUPPORT_NO);
		return supported;
	}

	/* AOSP areAllEffectsSupported(): NO if one effect is, UNKNOWN if one is unknown, else YES */
	public final int areAllEffectsSupported(int... effectIds) {
		int allSupported = VIBRATION_EFFECT_SUPPORT_YES;
		for (int support : areEffectsSupported(effectIds)) {
			switch (support) {
				case VIBRATION_EFFECT_SUPPORT_NO:
					return VIBRATION_EFFECT_SUPPORT_NO;
				case VIBRATION_EFFECT_SUPPORT_YES:
					continue;
				default:
					allSupported = VIBRATION_EFFECT_SUPPORT_UNKNOWN;
					break;
			}
		}
		return allSupported;
	}

	/* AOSP arePrimitivesSupported(): VibratorInfo.isPrimitiveSupported(), false for all as in areAllPrimitivesSupported() */
	public boolean[] arePrimitivesSupported(int... primitiveIds) {
		return new boolean[primitiveIds.length];
	}

	/* AOSP getPrimitiveDurations(): VibratorInfo.getPrimitiveDuration() is 0 for a primitive that is not supported */
	public int[] getPrimitiveDurations(int... primitiveIds) {
		return new int[primitiveIds.length];
	}

	/* AOSP areEnvelopeEffectsSupported(): the motor has no envelope (PWLE) control */
	public boolean areEnvelopeEffectsSupported() {
		return false;
	}

	/* AOSP getEnvelopeEffectInfo(): the limits of VibratorInfo without envelope support are all 0 */
	public android.os.vibrator.VibratorEnvelopeEffectInfo getEnvelopeEffectInfo() {
		return new android.os.vibrator.VibratorEnvelopeEffectInfo(0, 0, 0);
	}

	/* AOSP getFrequencyProfile(): null "if the vibrator does not have frequency control" */
	public android.os.vibrator.VibratorFrequencyProfile getFrequencyProfile() {
		return null;
	}

	/* AOSP getId(): the id of the vibrator behind this object, as VibratorManager.getVibratorIds() lists it */
	public int getId() {
		return 0;
	}

	/* AOSP getResonantFrequency()/getQFactor(): NaN "if it's unknown, not applicable" */
	public float getResonantFrequency() {
		return Float.NaN;
	}

	public float getQFactor() {
		return Float.NaN;
	}

	/* AOSP vibrate(long, AudioAttributes): errors of the one-shot effect are only logged (pre-O behaviour) */
	public void vibrate(long milliseconds, android.media.AudioAttributes attributes) {
		try {
			vibrate(VibrationEffect.createOneShot(milliseconds, VibrationEffect.DEFAULT_AMPLITUDE), attributes);
		} catch (IllegalArgumentException iae) {
			Slog.e("Vibrator", "Failed to create VibrationEffect", iae);
		}
	}

	/* AOSP vibrate(long[], int, AudioAttributes) */
	public void vibrate(long[] pattern, int repeat, android.media.AudioAttributes attributes) {
		if (repeat < -1 || repeat >= pattern.length)
			throw new ArrayIndexOutOfBoundsException();
		try {
			vibrate(VibrationEffect.createWaveform(pattern, repeat), attributes);
		} catch (IllegalArgumentException iae) {
			Slog.e("Vibrator", "Failed to create VibrationEffect", iae);
		}
	}

	public void cancel() {
		if (vibrateThread != null) {
			vibrateThread.interrupt();
			vibrateThread = null;
		}
	}

	private native void native_vibrate(int fd, long millis);
	private native int native_constructor();
}
