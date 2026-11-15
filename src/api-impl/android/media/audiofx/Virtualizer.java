package android.media.audiofx;

/* AOSP media/java/android/media/audiofx/Virtualizer.java. ATL has no effect engines, so the constructor fails like
 * AudioEffect's and no instance exists; the members are AOSP's checkState() answer for an uninitialized
 * effect. */
public class Virtualizer extends AudioEffect {

	public Virtualizer(int priority, int audioSession) {
		super(EFFECT_TYPE_VIRTUALIZER, EFFECT_TYPE_NULL, priority, audioSession);
	}

	public void setStrength(short strength) {
		throw new IllegalStateException("setParameter() called on uninitialized AudioEffect.");
	}

	public short getRoundedStrength() {
		throw new IllegalStateException("getParameter() called on uninitialized AudioEffect.");
	}
}
