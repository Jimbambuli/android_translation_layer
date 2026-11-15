package android.media.audiofx;

/* AOSP media/java/android/media/audiofx/LoudnessEnhancer.java. ATL has no effect engines, so the
 * constructor fails like AudioEffect's and no instance exists; the getters/setters are AOSP's
 * checkState() answer for an uninitialized effect. */
public class LoudnessEnhancer extends AudioEffect {
	public static final int PARAM_TARGET_GAIN_MB = 0;

	public LoudnessEnhancer(int audioSession) {
		super(EFFECT_TYPE_LOUDNESS_ENHANCER, EFFECT_TYPE_NULL, 0, audioSession);
	}

	public void setTargetGain(int gainmB) {
		throw new IllegalStateException("setParameter() called on uninitialized AudioEffect.");
	}

	public float getTargetGain() {
		throw new IllegalStateException("getParameter() called on uninitialized AudioEffect.");
	}
}
