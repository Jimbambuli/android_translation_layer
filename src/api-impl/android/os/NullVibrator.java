package android.os;

/* AOSP android.os.NullVibrator (@hide): a vibrator that does not exist, e.g. that of an input device without one */
public class NullVibrator extends Vibrator {

	public NullVibrator() {
		super(-1);
	}

	@Override
	public boolean hasVibrator() {
		return false;
	}

	@Override
	public void vibrate(long millis) {}

	@Override
	public void vibrate(long[] pattern, int repeat) {}

	@Override
	public void vibrate(VibrationEffect effect) {}
}
