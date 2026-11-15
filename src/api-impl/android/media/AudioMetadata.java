package android.media;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.util.Pair;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/* AOSP media/java/android/media/AudioMetadata.java: Key, Format, createKey(), createMap() and BaseMap. The
 * parcel/ByteBuffer packing (unpackMetadataMap/dataFromByteBuffer) is @hide and only talks to the native audio
 * framework, which ATL does not have. */
public final class AudioMetadata {
	private AudioMetadata() {}

	public interface Key<T> {
		@NonNull
		String getName();

		@NonNull
		Class<T> getValueClass();
	}

	@NonNull
	public static AudioMetadataMap createMap() {
		return new BaseMap();
	}

	public static class Format {
		@NonNull public static final Key<Integer> KEY_BIT_RATE = createKey("bitrate", Integer.class);
		@NonNull public static final Key<Integer> KEY_CHANNEL_MASK = createKey("channel-mask", Integer.class);
		@NonNull public static final Key<String> KEY_MIME = createKey("mime", String.class);
		@NonNull public static final Key<Integer> KEY_SAMPLE_RATE = createKey("sample-rate", Integer.class);
		@NonNull public static final Key<Integer> KEY_BIT_WIDTH = createKey("bit-width", Integer.class);
		@NonNull public static final Key<Boolean> KEY_ATMOS_PRESENT = createKey("atmos-present", Boolean.class);
		@NonNull public static final Key<Integer> KEY_HAS_ATMOS = createKey("has-atmos", Integer.class);
		@NonNull public static final Key<Integer> KEY_AUDIO_ENCODING = createKey("audio-encoding", Integer.class);
		@NonNull public static final Key<Integer> KEY_PRESENTATION_ID = createKey("presentation-id", Integer.class);
		@NonNull public static final Key<Integer> KEY_PROGRAM_ID = createKey("program-id", Integer.class);
		@NonNull public static final Key<Integer> KEY_PRESENTATION_CONTENT_CLASSIFIER =
				createKey("presentation-content-classifier", Integer.class);
		@NonNull public static final Key<String> KEY_PRESENTATION_LANGUAGE =
				createKey("presentation-language", String.class);

		private Format() {}
	}

	@NonNull
	public static <T> Key<T> createKey(@NonNull String name, @NonNull Class<T> type) {
		return new Key<T>() {
			private final String mName = name;
			private final Class<T> mType = type;

			@Override
			@NonNull
			public String getName() {
				return mName;
			}

			@Override
			@NonNull
			public Class<T> getValueClass() {
				return mType;
			}

			@Override
			public boolean equals(Object obj) {
				if (obj == this) {
					return true;
				}
				if (!(obj instanceof Key)) {
					return false;
				}
				Key<?> other = (Key<?>) obj;
				return mName.equals(other.getName()) && mType.equals(other.getValueClass());
			}

			@Override
			public int hashCode() {
				return Objects.hash(mName, mType);
			}
		};
	}

	public static class BaseMap implements AudioMetadataMap {
		@Override
		public <T> boolean containsKey(@NonNull Key<T> key) {
			Pair<Key<?>, Object> valuePair = mHashMap.get(pairFromKey(key));
			return valuePair != null;
		}

		@Override
		@NonNull
		public AudioMetadataMap dup() {
			BaseMap map = new BaseMap();
			map.mHashMap.putAll(this.mHashMap);
			return map;
		}

		@Override
		@Nullable
		@SuppressWarnings("unchecked")
		public <T> T get(@NonNull Key<T> key) {
			Pair<Key<?>, Object> valuePair = mHashMap.get(pairFromKey(key));
			return (T) getValueFromValuePair(valuePair);
		}

		@Override
		@NonNull
		public Set<Key<?>> keySet() {
			HashSet<Key<?>> set = new HashSet<>();
			for (Pair<Key<?>, Object> pair : mHashMap.values()) {
				set.add(pair.first);
			}
			return set;
		}

		@Override
		@Nullable
		@SuppressWarnings("unchecked")
		public <T> T remove(@NonNull Key<T> key) {
			Pair<Key<?>, Object> valuePair = mHashMap.remove(pairFromKey(key));
			return (T) getValueFromValuePair(valuePair);
		}

		@Override
		@Nullable
		@SuppressWarnings("unchecked")
		public <T> T set(@NonNull Key<T> key, @NonNull T value) {
			Objects.requireNonNull(value);
			Pair<Key<?>, Object> valuePair = mHashMap.put(pairFromKey(key), new Pair<Key<?>, Object>(key, value));
			return (T) getValueFromValuePair(valuePair);
		}

		@Override
		public int size() {
			return mHashMap.size();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this) {
				return true;
			}
			if (!(obj instanceof BaseMap)) {
				return false;
			}
			BaseMap other = (BaseMap) obj;
			return mHashMap.equals(other.mHashMap);
		}

		@Override
		public int hashCode() {
			return Objects.hash(mHashMap);
		}

		@NonNull
		private static <T> Pair<String, Class<?>> pairFromKey(@NonNull Key<T> key) {
			Objects.requireNonNull(key);
			return new Pair<String, Class<?>>(key.getName(), key.getValueClass());
		}

		@Nullable
		private static Object getValueFromValuePair(@Nullable Pair<Key<?>, Object> valuePair) {
			if (valuePair == null) {
				return null;
			}
			return valuePair.second;
		}

		private final HashMap<Pair<String, Class<?>>, Pair<Key<?>, Object>> mHashMap = new HashMap<>();
	}
}
