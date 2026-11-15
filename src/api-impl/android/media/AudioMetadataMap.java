package android.media;

import android.annotation.NonNull;
import android.annotation.Nullable;

/* AOSP media/java/android/media/AudioMetadataMap.java */
public interface AudioMetadataMap extends AudioMetadataReadMap {
	@Nullable
	<T> T remove(@NonNull AudioMetadata.Key<T> key);

	@Nullable
	<T> T set(@NonNull AudioMetadata.Key<T> key, @NonNull T value);
}
