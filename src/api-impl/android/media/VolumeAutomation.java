package android.media;

import android.annotation.NonNull;

/* AOSP media/java/android/media/VolumeAutomation.java */
public interface VolumeAutomation {
	@NonNull
	VolumeShaper createVolumeShaper(@NonNull VolumeShaper.Configuration configuration);
}
