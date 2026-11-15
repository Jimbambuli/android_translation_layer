package android.media;

/* AOSP media/java/android/media/AudioPlaybackConfiguration.java. ATL has no AudioService, so no instance is ever
 * handed out (AudioManager.getActivePlaybackConfigurations() is not implemented and AudioPlaybackCallback never
 * fires); the type only carries the player's AudioAttributes as in AOSP (mPlayerAttr). */
public final class AudioPlaybackConfiguration {
	private final AudioAttributes mPlayerAttr;

	AudioPlaybackConfiguration(AudioAttributes attributes) {
		mPlayerAttr = attributes;
	}

	/* AOSP getAudioAttributes(): returns mPlayerAttr */
	public AudioAttributes getAudioAttributes() {
		return mPlayerAttr;
	}
}
