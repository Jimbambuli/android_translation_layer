package android.media;

import java.util.concurrent.Executor;

/* API 32. There is no spatial audio, so the spatializer is never available nor enabled. */
public class Spatializer {
	public static final int SPATIALIZER_IMMERSIVE_LEVEL_OTHER = -1;
	public static final int SPATIALIZER_IMMERSIVE_LEVEL_NONE = 0;
	public static final int SPATIALIZER_IMMERSIVE_LEVEL_MULTICHANNEL = 1;

	public interface OnSpatializerStateChangedListener {
		void onSpatializerEnabledChanged(Spatializer spat, boolean enabled);
		void onSpatializerAvailableChanged(Spatializer spat, boolean available);
	}

	Spatializer() {}

	public int getImmersiveAudioLevel() {
		return SPATIALIZER_IMMERSIVE_LEVEL_NONE;
	}

	public boolean isAvailable() {
		return false;
	}

	public boolean isEnabled() {
		return false;
	}

	public boolean canBeSpatialized(AudioAttributes attributes, AudioFormat format) {
		return false;
	}

	public void addOnSpatializerStateChangedListener(Executor executor, OnSpatializerStateChangedListener listener) {}

	public void removeOnSpatializerStateChangedListener(OnSpatializerStateChangedListener listener) {}
}
