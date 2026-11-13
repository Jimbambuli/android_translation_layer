package android.media;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class MediaFormat {

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
