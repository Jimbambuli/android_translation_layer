package android.media.audiofx;

/* AOSP media/java/android/media/audiofx/AcousticEchoCanceler.java. ATL has no effect engines: isAvailable() finds no
 * EFFECT_TYPE_AEC in queryEffects(), the constructor fails like AudioEffect's and create() returns null, as
 * AOSP does on a device that does not implement the effect. */
public class AcousticEchoCanceler extends AudioEffect {

	public static boolean isAvailable() {
		return AudioEffect.isEffectTypeAvailable(AudioEffect.EFFECT_TYPE_AEC);
	}

	public static AcousticEchoCanceler create(int audioSession) {
		AcousticEchoCanceler effect = null;
		try {
			effect = new AcousticEchoCanceler(audioSession);
		} catch (IllegalArgumentException e) {
			// not implemented on this device
		} catch (UnsupportedOperationException e) {
			// not enough resources
		} catch (RuntimeException e) {
			// not enough memory
		}
		return effect;
	}

	private AcousticEchoCanceler(int audioSession) {
		super(EFFECT_TYPE_AEC, EFFECT_TYPE_NULL, 0, audioSession);
	}
}
