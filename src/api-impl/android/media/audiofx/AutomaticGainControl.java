package android.media.audiofx;

/* AOSP media/java/android/media/audiofx/AutomaticGainControl.java. ATL has no effect engines: isAvailable() finds no
 * EFFECT_TYPE_AGC in queryEffects(), the constructor fails like AudioEffect's and create() returns null, as
 * AOSP does on a device that does not implement the effect. */
public class AutomaticGainControl extends AudioEffect {

	public static boolean isAvailable() {
		return AudioEffect.isEffectTypeAvailable(AudioEffect.EFFECT_TYPE_AGC);
	}

	public static AutomaticGainControl create(int audioSession) {
		AutomaticGainControl effect = null;
		try {
			effect = new AutomaticGainControl(audioSession);
		} catch (IllegalArgumentException e) {
			// not implemented on this device
		} catch (UnsupportedOperationException e) {
			// not enough resources
		} catch (RuntimeException e) {
			// not enough memory
		}
		return effect;
	}

	private AutomaticGainControl(int audioSession) {
		super(EFFECT_TYPE_AGC, EFFECT_TYPE_NULL, 0, audioSession);
	}
}
