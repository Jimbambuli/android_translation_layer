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

	public static VibrationEffect createWaveform(long[] pattern, int repeat) {
		return new VibrationEffect(pattern.clone(), repeat);
	}
}
