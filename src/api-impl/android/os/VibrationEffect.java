package android.os;

public class VibrationEffect {

	/* AOSP keeps a list of segments; ATL's vibrator only switches on and off, so the timings are enough:
	 * alternating off and on durations in milliseconds (starting with off) and the index to repeat from */
	final long[] timings;
	final int repeat;

	private VibrationEffect(long[] timings, int repeat) {
		this.timings = timings;
		this.repeat = repeat;
	}

	public static VibrationEffect createOneShot(long milliseconds, int amplitude) {
		return new VibrationEffect(new long[] {0, milliseconds}, -1);
	}

	public static final int DEFAULT_AMPLITUDE = -1;
	public static final int MAX_AMPLITUDE = 255;

	public static VibrationEffect createWaveform(long[] pattern, int repeat) {
		return new VibrationEffect(pattern.clone(), repeat);
	}

	/* AOSP VibrationEffect.createWaveform(long[], int[], int): one step per timing, amplitude 0 is off.
	 * ATL's off/on list gets a zero-length step where two steps of the same kind follow each other,
	 * so the repeat index still points at the same step. The strength itself is dropped (no amplitude
	 * control, see Vibrator.hasAmplitudeControl). */
	public static VibrationEffect createWaveform(long[] timings, int[] amplitudes, int repeat) {
		if (timings.length != amplitudes.length) {
			throw new IllegalArgumentException(
			    "timing and amplitude arrays must be of equal length"
			    + " (timings.length=" + timings.length
			    + ", amplitudes.length=" + amplitudes.length + ")");
		}
		if (repeat < -1 || repeat >= timings.length) {
			throw new IllegalArgumentException("repeat index must be within the bounds of the segments"
			    + " (segments.length=" + timings.length + ", index=" + repeat + ")");
		}
		boolean hasNonZeroDuration = false;
		for (long timing : timings)
			hasNonZeroDuration |= timing != 0;
		if (!hasNonZeroDuration)
			throw new IllegalArgumentException("at least one timing must be non-zero");
		long[] steps = new long[2 * timings.length + 1];
		int n = 0;
		int newRepeat = -1;
		for (int i = 0; i < timings.length; i++) {
			if (timings[i] < 0)
				throw new IllegalArgumentException("timings must all be >= 0");
			if (amplitudes[i] < DEFAULT_AMPLITUDE || amplitudes[i] > MAX_AMPLITUDE)
				throw new IllegalArgumentException("amplitudes must all be DEFAULT_AMPLITUDE or between 0 and 255");
			boolean on = amplitudes[i] != 0;
			if ((n % 2 == 1) != on)
				steps[n++] = 0; // even slots are off, odd slots are on
			if (i == repeat)
				newRepeat = n;
			steps[n++] = timings[i];
		}
		return new VibrationEffect(java.util.Arrays.copyOf(steps, n), newRepeat);
	}
}
