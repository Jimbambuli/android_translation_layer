package android.media;

import android.annotation.IntRange;
import android.annotation.NonNull;
import android.annotation.Nullable;
import java.util.Set;

/* AOSP media/java/android/media/AudioMetadataReadMap.java */
public interface AudioMetadataReadMap {
	<T> boolean containsKey(@NonNull AudioMetadata.Key<T> key);

	@NonNull
	AudioMetadataMap dup();

	@Nullable
	<T> T get(@NonNull AudioMetadata.Key<T> key);

	@NonNull
	Set<AudioMetadata.Key<?>> keySet();

	@IntRange(from = 0)
	int size();
}
