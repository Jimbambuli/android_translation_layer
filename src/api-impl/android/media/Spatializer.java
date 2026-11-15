package android.media;

import java.util.Collections;
import java.util.List;
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

	/* AOSP Spatializer.getSpatializedChannelMasks() asks AudioService; without a spatializer effect
	 * nothing is spatialized (AudioService side not in the local sources) */
	public List<Integer> getSpatializedChannelMasks() {
		return Collections.emptyList();
	}

	public void addOnSpatializerStateChangedListener(Executor executor, OnSpatializerStateChangedListener listener) {}

	public void removeOnSpatializerStateChangedListener(OnSpatializerStateChangedListener listener) {}
    /* AOSP 16 API; no backing hardware/service in ATL */
    public void addOnHeadTrackerAvailableListener(java.util.concurrent.Executor arg0, android.media.Spatializer.OnHeadTrackerAvailableListener arg1) {  }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public boolean isHeadTrackerAvailable() { return false; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public void removeOnHeadTrackerAvailableListener(android.media.Spatializer.OnHeadTrackerAvailableListener arg0) {  }

    /* AOSP 16 API; no backing hardware/service in ATL */
    public static interface OnHeadTrackerAvailableListener {
        void onHeadTrackerAvailableChanged(android.media.Spatializer arg0, boolean arg1);
    }
}
