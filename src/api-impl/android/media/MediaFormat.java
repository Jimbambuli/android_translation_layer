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
	public static final String MIMETYPE_VIDEO_APV = "video/apv";
	public static final String MIMETYPE_VIDEO_MPEG2 = "video/mpeg2";
	public static final String MIMETYPE_VIDEO_RAW = "video/raw";
	public static final String MIMETYPE_VIDEO_SCRAMBLED = "video/scrambled";
	public static final String MIMETYPE_AUDIO_MPEG = "audio/mpeg";
	public static final String MIMETYPE_AUDIO_QCELP = "audio/qcelp";
	public static final String MIMETYPE_AUDIO_G711_ALAW = "audio/g711-alaw";
	public static final String MIMETYPE_AUDIO_G711_MLAW = "audio/g711-mlaw";
	public static final String MIMETYPE_AUDIO_RAW = "audio/raw";
	public static final String MIMETYPE_AUDIO_FLAC = "audio/flac";
	public static final String MIMETYPE_AUDIO_MSGSM = "audio/gsm";
	public static final String MIMETYPE_AUDIO_AC3 = "audio/ac3";
	public static final String MIMETYPE_AUDIO_EAC3 = "audio/eac3";
	public static final String MIMETYPE_AUDIO_EAC3_JOC = "audio/eac3-joc";
	public static final String MIMETYPE_AUDIO_AC4 = "audio/ac4";
	public static final String MIMETYPE_AUDIO_SCRAMBLED = "audio/scrambled";
	public static final String MIMETYPE_AUDIO_MPEGH_MHA1 = "audio/mha1";
	public static final String MIMETYPE_AUDIO_MPEGH_MHM1 = "audio/mhm1";
	public static final String MIMETYPE_AUDIO_DTS = "audio/vnd.dts";
	public static final String MIMETYPE_AUDIO_DTS_HD = "audio/vnd.dts.hd";
	public static final String MIMETYPE_AUDIO_DTS_UHD = "audio/vnd.dts.uhd";
	public static final String MIMETYPE_AUDIO_DRA = "audio/vnd.dra";
	public static final String MIMETYPE_AUDIO_DOLBY_MAT = "audio/vnd.dolby.mat";
	public static final String MIMETYPE_AUDIO_DOLBY_TRUEHD = "audio/vnd.dolby.mlp";
	public static final String MIMETYPE_AUDIO_AAC_LC = "audio/mp4a.40.02";
	public static final String MIMETYPE_AUDIO_AAC_HE_V1 = "audio/mp4a.40.05";
	public static final String MIMETYPE_AUDIO_AAC_HE_V2 = "audio/mp4a.40.29";
	public static final String MIMETYPE_AUDIO_AAC_ELD = "audio/mp4a.40.39";
	public static final String MIMETYPE_AUDIO_AAC_XHE = "audio/mp4a.40.42";
	public static final String MIMETYPE_AUDIO_MPEGH_BL_L3 = "audio/mhm1.03";
	public static final String MIMETYPE_AUDIO_MPEGH_BL_L4 = "audio/mhm1.04";
	public static final String MIMETYPE_AUDIO_MPEGH_LC_L3 = "audio/mhm1.0d";
	public static final String MIMETYPE_AUDIO_MPEGH_LC_L4 = "audio/mhm1.0e";
	public static final String MIMETYPE_AUDIO_IEC61937 = "audio/x-iec61937";
	public static final String MIMETYPE_AUDIO_IAMF = "audio/iamf";
	public static final String MIMETYPE_IMAGE_ANDROID_HEIC = "image/vnd.android.heic";
	public static final String MIMETYPE_IMAGE_AVIF = "image/avif";
	public static final String MIMETYPE_TEXT_VTT = "text/vtt";
	public static final String MIMETYPE_TEXT_SUBRIP = "application/x-subrip";
	public static final String MIMETYPE_TEXT_CEA_608 = "text/cea-608";
	public static final String MIMETYPE_TEXT_CEA_708 = "text/cea-708";
	public static final String LOG_SESSION_ID = "log-session-id";
	public static final String KEY_CODECS_STRING = "codecs-string";
	public static final String KEY_LOW_LATENCY = "low-latency";
	public static final String KEY_LANGUAGE = "language";
	public static final String KEY_CAPTION_SERVICE_NUMBER = "caption-service-number";
	public static final String KEY_CROP_BOTTOM = "crop-bottom";
	public static final String KEY_CROP_LEFT = "crop-left";
	public static final String KEY_CROP_RIGHT = "crop-right";
	public static final String KEY_CROP_TOP = "crop-top";
	public static final String KEY_MAX_INPUT_SIZE = "max-input-size";
	public static final String KEY_BUFFER_BATCH_MAX_OUTPUT_SIZE = "buffer-batch-max-output-size";
	public static final String KEY_BUFFER_BATCH_THRESHOLD_OUTPUT_SIZE = "buffer-batch-threshold-output-size";
	public static final String KEY_PIXEL_ASPECT_RATIO_WIDTH = "sar-width";
	public static final String KEY_PIXEL_ASPECT_RATIO_HEIGHT = "sar-height";
	public static final String KEY_BIT_RATE = "bitrate";
	public static final String KEY_HARDWARE_AV_SYNC_ID = "hw-av-sync-id";
	public static final String KEY_COLOR_FORMAT = "color-format";
	public static final String KEY_FRAME_RATE = "frame-rate";
	public static final String KEY_TILE_WIDTH = "tile-width";
	public static final String KEY_TILE_HEIGHT = "tile-height";
	public static final String KEY_GRID_ROWS = "grid-rows";
	public static final String KEY_GRID_COLUMNS = "grid-cols";
	public static final String KEY_PCM_ENCODING = "pcm-encoding";
	public static final String KEY_CAPTURE_RATE = "capture-rate";
	public static final String KEY_SLOW_MOTION_MARKERS = "slow-motion-markers";
	public static final String KEY_I_FRAME_INTERVAL = "i-frame-interval";
	public static final String KEY_INTRA_REFRESH_PERIOD = "intra-refresh-period";
	public static final String KEY_PREPEND_HEADER_TO_SYNC_FRAMES = "prepend-sps-pps-to-idr-frames";
	public static final String KEY_TEMPORAL_LAYERING = "ts-schema";
	public static final String KEY_STRIDE = "stride";
	public static final String KEY_SLICE_HEIGHT = "slice-height";
	public static final String KEY_REPEAT_PREVIOUS_FRAME_AFTER = "repeat-previous-frame-after";
	public static final String KEY_MAX_FPS_TO_ENCODER = "max-fps-to-encoder";
	public static final String KEY_MAX_PTS_GAP_TO_ENCODER = "max-pts-gap-to-encoder";
	public static final String KEY_CREATE_INPUT_SURFACE_SUSPENDED = "create-input-buffers-suspended";
	public static final String KEY_PUSH_BLANK_BUFFERS_ON_STOP = "push-blank-buffers-on-shutdown";
	public static final String KEY_DURATION = "durationUs";
	public static final String KEY_IS_ADTS = "is-adts";
	public static final String KEY_CHANNEL_MASK = "channel-mask";
	public static final String KEY_MAX_OUTPUT_CHANNEL_COUNT = "max-output-channel-count";
	public static final String KEY_ENCODER_DELAY = "encoder-delay";
	public static final String KEY_ENCODER_PADDING = "encoder-padding";
	public static final String KEY_AAC_PROFILE = "aac-profile";
	public static final String KEY_AAC_SBR_MODE = "aac-sbr-mode";
	public static final String KEY_AAC_MAX_OUTPUT_CHANNEL_COUNT = "aac-max-output-channel_count";
	public static final String KEY_AAC_DRC_TARGET_REFERENCE_LEVEL = "aac-target-ref-level";
	public static final String KEY_AAC_DRC_EFFECT_TYPE = "aac-drc-effect-type";
	public static final String KEY_AAC_ENCODED_TARGET_LEVEL = "aac-encoded-target-level";
	public static final String KEY_AAC_DRC_BOOST_FACTOR = "aac-drc-boost-level";
	public static final String KEY_AAC_DRC_ATTENUATION_FACTOR = "aac-drc-cut-level";
	public static final String KEY_AAC_DRC_HEAVY_COMPRESSION = "aac-drc-heavy-compression";
	public static final String KEY_AAC_DRC_OUTPUT_LOUDNESS = "aac-drc-output-loudness";
	public static final String KEY_AAC_DRC_ALBUM_MODE = "aac-drc-album-mode";
	public static final String KEY_FLAC_COMPRESSION_LEVEL = "flac-compression-level";
	public static final String KEY_MPEGH_PROFILE_LEVEL_INDICATION = "mpegh-profile-level-indication";
	public static final String KEY_MPEGH_COMPATIBLE_SETS = "mpegh-compatible-sets";
	public static final String KEY_MPEGH_REFERENCE_CHANNEL_LAYOUT = "mpegh-reference-channel-layout";
	public static final String KEY_COMPLEXITY = "complexity";
	public static final String KEY_QUALITY = "quality";
	public static final String KEY_PRIORITY = "priority";
	public static final String KEY_OPERATING_RATE = "operating-rate";
	public static final String KEY_LATENCY = "latency";
	public static final String KEY_OUTPUT_REORDER_DEPTH = "output-reorder-depth";
	public static final String KEY_ROTATION = "rotation-degrees";
	public static final String KEY_BITRATE_MODE = "bitrate-mode";
	public static final String KEY_VIDEO_QP_MAX = "video-qp-max";
	public static final String KEY_VIDEO_QP_MIN = "video-qp-min";
	public static final String KEY_VIDEO_QP_I_MAX = "video-qp-i-max";
	public static final String KEY_VIDEO_QP_I_MIN = "video-qp-i-min";
	public static final String KEY_VIDEO_QP_P_MAX = "video-qp-p-max";
	public static final String KEY_VIDEO_QP_P_MIN = "video-qp-p-min";
	public static final String KEY_VIDEO_QP_B_MAX = "video-qp-b-max";
	public static final String KEY_VIDEO_QP_B_MIN = "video-qp-b-min";
	public static final String KEY_VIDEO_ENCODING_STATISTICS_LEVEL = "video-encoding-statistics-level";
	public static final int VIDEO_ENCODING_STATISTICS_LEVEL_NONE = 0;
	public static final int VIDEO_ENCODING_STATISTICS_LEVEL_1 = 1;
	public static final String KEY_VIDEO_QP_AVERAGE = "video-qp-average";
	public static final String KEY_PICTURE_TYPE = "picture-type";
	public static final int PICTURE_TYPE_UNKNOWN = 0;
	public static final int PICTURE_TYPE_I = 1;
	public static final int PICTURE_TYPE_P = 2;
	public static final int PICTURE_TYPE_B = 3;
	public static final String KEY_AUDIO_SESSION_ID = "audio-session-id";
	public static final String KEY_IS_AUTOSELECT = "is-autoselect";
	public static final String KEY_IS_DEFAULT = "is-default";
	public static final String KEY_IS_FORCED_SUBTITLE = "is-forced-subtitle";
	public static final String KEY_HAPTIC_CHANNEL_COUNT = "haptic-channel-count";
	public static final String KEY_COLOR_STANDARD = "color-standard";
	public static final int COLOR_STANDARD_BT709 = 1;
	public static final int COLOR_STANDARD_BT601_PAL = 2;
	public static final int COLOR_STANDARD_BT601_NTSC = 4;
	public static final int COLOR_STANDARD_BT2020 = 6;
	public static final String KEY_COLOR_TRANSFER = "color-transfer";
	public static final int COLOR_TRANSFER_LINEAR = 1;
	public static final int COLOR_TRANSFER_SDR_VIDEO = 3;
	public static final int COLOR_TRANSFER_ST2084 = 6;
	public static final int COLOR_TRANSFER_HLG = 7;
	public static final String KEY_COLOR_RANGE = "color-range";
	public static final int COLOR_RANGE_LIMITED = 2;
	public static final int COLOR_RANGE_FULL = 1;
	public static final String KEY_HDR_STATIC_INFO = "hdr-static-info";
	public static final String KEY_HDR10_PLUS_INFO = "hdr10-plus-info";
	public static final String KEY_COLOR_TRANSFER_REQUEST = "color-transfer-request";
	public static final String KEY_TRACK_ID = "track-id";
	public static final String KEY_MAX_B_FRAMES = "max-bframes";
	public static final String KEY_ALLOW_FRAME_DROP = "allow-frame-drop";
	public static final String KEY_IMPORTANCE = "importance";
	public static final int FLAG_SECURITY_MODEL_SANDBOXED = (1 << MediaCodecInfo.SECURITY_MODEL_SANDBOXED);
	public static final int FLAG_SECURITY_MODEL_MEMORY_SAFE = (1 << MediaCodecInfo.SECURITY_MODEL_MEMORY_SAFE);
	public static final String KEY_SECURITY_MODEL = "security-model";
	public static final String KEY_NUM_SLOTS = "num-slots";
	public static final String KEY_PICTURE_PROFILE_ID = "picture-profile-id";
	public static final String KEY_PICTURE_PROFILE_INSTANCE = "picture-profile-instance";
	public static final String KEY_AUDIO_PRESENTATION_ID = "audio-presentation-id";

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

	/* AOSP MediaFormat(Map) (hidden): the map of MediaExtractor's formats becomes the format's own */
	MediaFormat(Map<String, Object> map) {
		this.map = map;
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
