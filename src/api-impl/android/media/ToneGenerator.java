package android.media;

public class ToneGenerator {

	/** Default value for an unknown or unspecified tone. */
	public static final int TONE_UNKNOWN = -1;

	public ToneGenerator(int streamType, int volume) {}

	/* AOSP: true when the tone could be started. ATL's ToneGenerator produces no sound (stopTone() and release() are
	 * empty as well), so no tone is ever started. */
	public boolean startTone(int toneType, int durationMs) {
		return false;
	}

	public void stopTone() {}

	public void release() {}
}
