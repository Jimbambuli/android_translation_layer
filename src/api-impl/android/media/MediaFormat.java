package android.media;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

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
	public static final String KEY_SAMPLE_RATE = "sample-rate";
	public static final String KEY_CHANNEL_COUNT = "channel-count";
	public static final String KEY_WIDTH = "width";
	public static final String KEY_HEIGHT = "height";

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

	public boolean containsKey(String name) {
		return map.containsKey(name);
	}

	public String toString() {
		return map.toString();
	}

	public String getString(String name) {
		return (String)map.get(name);
	}

	public long getLong(String name) {
		return (long)map.get(name);
	}
}
