package android.media;

import android.graphics.Rect;
import java.nio.ByteBuffer;
import java.util.AbstractSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class MediaFormat {

	/* AOSP MediaFormat MIMETYPE_* (used by EncoderProfiles) */
	public static final String MIMETYPE_VIDEO_VP8 = "video/x-vnd.on2.vp8";
	public static final String MIMETYPE_VIDEO_VP9 = "video/x-vnd.on2.vp9";
	public static final String MIMETYPE_VIDEO_AV1 = "video/av01";
	public static final String MIMETYPE_VIDEO_AVC = "video/avc";
	public static final String MIMETYPE_VIDEO_HEVC = "video/hevc";
	public static final String MIMETYPE_VIDEO_MPEG4 = "video/mp4v-es";
	public static final String MIMETYPE_VIDEO_H263 = "video/3gpp";
	public static final String MIMETYPE_VIDEO_DOLBY_VISION = "video/dolby-vision";
	public static final String MIMETYPE_AUDIO_AMR_NB = "audio/3gpp";
	public static final String MIMETYPE_AUDIO_AMR_WB = "audio/amr-wb";
	public static final String MIMETYPE_AUDIO_AAC = "audio/mp4a-latm";
	public static final String MIMETYPE_AUDIO_VORBIS = "audio/vorbis";
	public static final String MIMETYPE_AUDIO_OPUS = "audio/opus";
	public static final String KEY_MIME = "mime";
	public static final String KEY_PROFILE = "profile";
	public static final String KEY_LEVEL = "level";
	public static final String KEY_SAMPLE_RATE = "sample-rate";
	public static final String KEY_CHANNEL_COUNT = "channel-count";
	public static final String KEY_WIDTH = "width";
	public static final String KEY_HEIGHT = "height";
	public static final String KEY_MAX_WIDTH = "max-width";
	public static final String KEY_MAX_HEIGHT = "max-height";

	private Map<String, Object> map = new HashMap<>();

	public static final MediaFormat createAudioFormat(String mime, int sampleRate, int channelCount) {
		MediaFormat format = new MediaFormat();
		format.setString(KEY_MIME, mime);
		format.setInteger(KEY_SAMPLE_RATE, sampleRate);
		format.setInteger(KEY_CHANNEL_COUNT, channelCount);
		return format;
	}

	public static final MediaFormat createVideoFormat(String mime, int width, int height) {
		MediaFormat format = new MediaFormat();
		format.setString(KEY_MIME, mime);
		format.setInteger(KEY_WIDTH, width);
		format.setInteger(KEY_HEIGHT, height);
		return format;
	}

	public MediaFormat() {
	}

	/* AOSP MediaFormat(MediaFormat other): a copy of the key/value map */
	public MediaFormat(MediaFormat other) {
		map.putAll(other.map);
	}

	public void setString(String key, String value) {
		map.put(key, value);
	}

	public void setInteger(String key, int value) {
		map.put(key, value);
	}

	public void setByteBuffer(String key, ByteBuffer value) {
		map.put(key, value);
	}

	/* AOSP MediaFormat.setLong() */
	public final void setLong(String name, long value) {
		map.put(name, value);
	}

	/* AOSP MediaFormat value types and getValueTypeForKey() (API 29) */
	public static final int TYPE_NULL = 0;
	public static final int TYPE_INTEGER = 1;
	public static final int TYPE_LONG = 2;
	public static final int TYPE_FLOAT = 3;
	public static final int TYPE_STRING = 4;
	public static final int TYPE_BYTE_BUFFER = 5;

	public final int getValueTypeForKey(String name) {
		Object value = map.get(name);
		if (value == null) {
			return TYPE_NULL;
		} else if (value instanceof Integer) {
			return TYPE_INTEGER;
		} else if (value instanceof Long) {
			return TYPE_LONG;
		} else if (value instanceof Float) {
			return TYPE_FLOAT;
		} else if (value instanceof String) {
			return TYPE_STRING;
		} else if (value instanceof ByteBuffer) {
			return TYPE_BYTE_BUFFER;
		}
		throw new RuntimeException("invalid value for key");
	}

	public void setFloat(String key, float value) {
		map.put(key, value);
	}

	public ByteBuffer getByteBuffer(String name) {
		return (ByteBuffer)map.get(name);
	}

	public int getInteger(String name) {
		return (int)map.get(name);
	}

	public float getFloat(String name) {
		return (Float)map.get(name);
	}

	public float getFloat(String name, float defaultValue) {
		Object value = map.get(name);
		return value instanceof Float ? (Float)value : defaultValue;
	}

	public int getInteger(String name, int defaultValue) {
		Object value = map.get(name);
		return value instanceof Integer ? (Integer)value : defaultValue;
	}

	public long getLong(String name, long defaultValue) {
		Object value = map.get(name);
		return value instanceof Long ? (Long)value : defaultValue;
	}

	public void setFeatureEnabled(String feature, boolean enabled) {
		map.put(KEY_FEATURE_PREFIX + feature, enabled ? 1 : 0);
	}

	private static final String KEY_FEATURE_PREFIX = "feature-";

	/* AOSP MediaFormat.getFeatureEnabled()/removeKey() */
	public boolean getFeatureEnabled(String feature) {
		Integer enabled = (Integer)map.get(KEY_FEATURE_PREFIX + feature);
		if (enabled == null) {
			throw new IllegalArgumentException("feature is not specified");
		}
		return enabled != 0;
	}

	public final void removeKey(String name) {
		// exclude feature mappings
		if (!name.startsWith(KEY_FEATURE_PREFIX)) {
			map.remove(name);
		}
	}

	public boolean containsKey(String name) {
		return map.containsKey(name);
	}

	public String toString() {
		return map.toString();
	}

	public String getString(String name) {
		return (String)map.get(name);
	}

	public String getString(String name, String defaultValue) {
		String ret = getString(name);
		return ret == null ? defaultValue : ret;
	}

	public ByteBuffer getByteBuffer(String name, ByteBuffer defaultValue) {
		ByteBuffer ret = getByteBuffer(name);
		return ret == null ? defaultValue : ret;
	}

	public long getLong(String name) {
		return (long)map.get(name);
	}

	/* AOSP MediaFormat (frameworks/base/media/java/android/media/MediaFormat.java): feature and key views, getNumber,
	 * createSubtitleFormat, QpOffsetRect */
	public final boolean containsFeature(String name) {
		return map.containsKey(KEY_FEATURE_PREFIX + name);
	}

	public final void removeFeature(String name) {
		map.remove(KEY_FEATURE_PREFIX + name);
	}

	public final Number getNumber(String name) {
		return (Number) map.get(name);
	}

	public final Number getNumber(String name, Number defaultValue) {
		Number ret = getNumber(name);
		return ret == null ? defaultValue : ret;
	}

	public final Set<String> getKeys() {
		return new UnprefixedKeySet(KEY_FEATURE_PREFIX);
	}

	public final Set<String> getFeatures() {
		return new PrefixedKeySetWithPrefixRemoved(KEY_FEATURE_PREFIX);
	}

	public static final MediaFormat createSubtitleFormat(String mime, String language) {
		MediaFormat format = new MediaFormat();
		format.setString(KEY_MIME, mime);
		format.setString("language", language);
		return format;
	}

	private abstract class FilteredMappedKeySet extends AbstractSet<String> {
		private Set<String> mKeys;

		abstract protected boolean keepKey(String key);

		abstract protected String mapKeyToItem(String key);

		abstract protected String mapItemToKey(String item);

		public FilteredMappedKeySet() {
			mKeys = map.keySet();
		}

		@Override
		public boolean contains(Object o) {
			if (o instanceof String) {
				String key = mapItemToKey((String) o);
				return keepKey(key) && mKeys.contains(key);
			}
			return false;
		}

		@Override
		public boolean remove(Object o) {
			if (o instanceof String) {
				String key = mapItemToKey((String) o);
				if (keepKey(key) && mKeys.remove(key)) {
					map.remove(key);
					return true;
				}
			}
			return false;
		}

		private class KeyIterator implements Iterator<String> {
			Iterator<String> mIterator;
			String mLast;

			public KeyIterator() {
				mIterator = mKeys.stream().filter(k -> keepKey(k)).collect(Collectors.toList()).iterator();
			}

			@Override
			public boolean hasNext() {
				return mIterator.hasNext();
			}

			@Override
			public String next() {
				mLast = mIterator.next();
				return mapKeyToItem(mLast);
			}

			@Override
			public void remove() {
				mIterator.remove();
				map.remove(mLast);
			}
		}

		@Override
		public Iterator<String> iterator() {
			return new KeyIterator();
		}

		@Override
		public int size() {
			return (int) mKeys.stream().filter(this::keepKey).count();
		}
	}

	private class UnprefixedKeySet extends FilteredMappedKeySet {
		private String mPrefix;

		public UnprefixedKeySet(String prefix) {
			super();
			mPrefix = prefix;
		}

		protected boolean keepKey(String key) {
			return !key.startsWith(mPrefix);
		}

		protected String mapKeyToItem(String key) {
			return key;
		}

		protected String mapItemToKey(String item) {
			return item;
		}
	}

	private class PrefixedKeySetWithPrefixRemoved extends FilteredMappedKeySet {
		private String mPrefix;
		private int mPrefixLength;

		public PrefixedKeySetWithPrefixRemoved(String prefix) {
			super();
			mPrefix = prefix;
			mPrefixLength = prefix.length();
		}

		protected boolean keepKey(String key) {
			return key.startsWith(mPrefix);
		}

		protected String mapKeyToItem(String key) {
			return key.substring(mPrefixLength);
		}

		protected String mapItemToKey(String item) {
			return mPrefix + item;
		}
	}

	public static final class QpOffsetRect {
		private Rect mContour;
		private int mQpOffset;

		public QpOffsetRect(Rect contour, int qpOffset) {
			mContour = contour;
			mQpOffset = qpOffset;
		}

		public void set(Rect contour, int qpOffset) {
			mContour = contour;
			mQpOffset = qpOffset;
		}

		public String flattenToString() {
			return mContour.top + "," + mContour.left + "-" + mContour.bottom + "," + mContour.right + "=" + mQpOffset + ";";
		}

		public static String flattenToString(List<QpOffsetRect> qpOffsetRects) {
			StringBuilder builder = new StringBuilder();
			for (QpOffsetRect qpOffsetRect : qpOffsetRects) {
				builder.append(qpOffsetRect.flattenToString());
			}
			return builder.toString();
		}
	}
}
