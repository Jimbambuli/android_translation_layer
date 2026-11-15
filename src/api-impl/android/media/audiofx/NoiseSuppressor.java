package android.media.audiofx;

/* AOSP media/java/android/media/audiofx/NoiseSuppressor.java. ATL has no effect engines: isAvailable() finds no
 * EFFECT_TYPE_NS in queryEffects(), the constructor fails like AudioEffect's and create() returns null, as
 * AOSP does on a device that does not implement the effect. */
public class NoiseSuppressor extends AudioEffect {

	public static boolean isAvailable() {
		return AudioEffect.isEffectTypeAvailable(AudioEffect.EFFECT_TYPE_NS);
	}

	public static NoiseSuppressor create(int audioSession) {
		NoiseSuppressor effect = null;
		try {
			effect = new NoiseSuppressor(audioSession);
		} catch (IllegalArgumentException e) {
			// not implemented on this device
		} catch (UnsupportedOperationException e) {
			// not enough resources
		} catch (RuntimeException e) {
			// not enough memory
		}
		return effect;
	}

	private NoiseSuppressor(int audioSession) {
		super(EFFECT_TYPE_NS, EFFECT_TYPE_NULL, 0, audioSession);
	}
}
