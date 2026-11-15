package android.media.audiofx;

import android.media.AudioManager;

/* AOSP media/java/android/media/audiofx/HapticGenerator.java. ATL has no effect engines (see AudioEffect), so
 * create() fails like AOSP's constructor does when native_setup() finds no haptic generator effect. */
public class HapticGenerator extends AudioEffect implements AutoCloseable {
	/* AOSP isAvailable(): haptic playback supported and the effect type listed; ATL has neither */
	public static boolean isAvailable() {
		return AudioManager.isHapticPlaybackSupported() && AudioEffect.isEffectTypeAvailable(AudioEffect.EFFECT_TYPE_HAPTIC_GENERATOR);
	}

	/* AOSP create(): new HapticGenerator(audioSession); the super constructor throws in ATL */
	public static HapticGenerator create(int audioSession) {
		return new HapticGenerator(audioSession);
	}

	private HapticGenerator(int audioSession) {
		super(EFFECT_TYPE_HAPTIC_GENERATOR, EFFECT_TYPE_NULL, 0, audioSession);
	}

	/* AOSP close(): release() */
	@Override
	public void close() {
		release();
	}
}
