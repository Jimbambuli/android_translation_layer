package android.media;

public class MediaCodecInfo {

	private String name;
	private String mime;

	public MediaCodecInfo(String name, String mime) {
		this.name = name;
		this.mime = mime;
	}

	public String getName() {
		return name;
	}

	public boolean isEncoder() {
		return false;
	}

	/* API 29: the codecs are FFmpeg's software codecs */
	public boolean isAlias() {
		return false;
	}

	public boolean isHardwareAccelerated() {
		return false;
	}

	public boolean isSoftwareOnly() {
		return true;
	}

	public boolean isVendor() {
		return false;
	}

	public String[] getSupportedTypes() {
		return new String[] {mime};
	}

	public CodecCapabilities getCapabilitiesForType(String type) {
		CodecCapabilities caps = new CodecCapabilities();
		caps.mimeType = type;
		if ("video/avc".equals(type)) {
			caps.profileLevels = new CodecProfileLevel[] {
				new CodecProfileLevel(CodecProfileLevel.AVCProfileBaseline, CodecProfileLevel.AVCLevel61),
				new CodecProfileLevel(CodecProfileLevel.AVCProfileMain, CodecProfileLevel.AVCLevel61),
			};
		}
		return caps;
	}

	public static class CodecCapabilities {

		/* AOSP: never null, empty for a codec without profiles */
		public CodecProfileLevel[] profileLevels = new CodecProfileLevel[0];

		private String mimeType;

		public String getMimeType() {
			return mimeType;
		}

		/* Public modifiable field, as in AOSP; ATL advertises no specific color formats. */
		public int[] colorFormats = new int[0];

		public boolean isFeatureSupported(String feature) {
			System.out.println("CodecCapabilities.isFeatureSupported(" + feature + ")");
			return false;
		}

		public boolean isFeatureRequired(String feature) {
			System.out.println("CodecCapabilities.isFeatureRequired(" + feature + ")");
			return false;
		}

		public AudioCapabilities getAudioCapabilities() {
			return new AudioCapabilities();
		}

		/* AOSP: mEncoderCaps, null for a decoder; all ATL codecs are decoders */
		public EncoderCapabilities getEncoderCapabilities() {
			return null;
		}

		public VideoCapabilities getVideoCapabilities() {
			return new VideoCapabilities();
		}

		public int getMaxSupportedInstances() {
			return 1;
		}
	}

	public static final class CodecProfileLevel {

		public int profile;
		public int level;

		public CodecProfileLevel() {}

		public CodecProfileLevel(int profile, int level) {
			this.profile = profile;
			this.level = level;
		}

		// AVC profiles
		public static final int AVCProfileBaseline = 0x01;
		public static final int AVCProfileMain = 0x02;
		public static final int AVCProfileExtended = 0x04;
		public static final int AVCProfileHigh = 0x08;
		public static final int AVCProfileHigh10 = 0x10;
		public static final int AVCProfileHigh422 = 0x20;
		public static final int AVCProfileHigh444 = 0x40;

		// AVC levels
		public static final int AVCLevel1 = 0x01;
		public static final int AVCLevel1b = 0x02;
		public static final int AVCLevel11 = 0x04;
		public static final int AVCLevel12 = 0x08;
		public static final int AVCLevel13 = 0x10;
		public static final int AVCLevel2 = 0x20;
		public static final int AVCLevel21 = 0x40;
		public static final int AVCLevel22 = 0x80;
		public static final int AVCLevel3 = 0x100;
		public static final int AVCLevel31 = 0x200;
		public static final int AVCLevel32 = 0x400;
		public static final int AVCLevel4 = 0x800;
		public static final int AVCLevel41 = 0x1000;
		public static final int AVCLevel42 = 0x2000;
		public static final int AVCLevel5 = 0x4000;
		public static final int AVCLevel51 = 0x8000;
		public static final int AVCLevel52 = 0x10000;
		public static final int AVCLevel6 = 0x20000;
		public static final int AVCLevel61 = 0x40000;
		public static final int AVCLevel62 = 0x80000;

		// VP9 profiles
		public static final int VP9Profile0 = 0x01;
		public static final int VP9Profile1 = 0x02;
		public static final int VP9Profile2 = 0x04;
		public static final int VP9Profile3 = 0x08;
		public static final int VP9Profile2HDR10Plus = 0x1000;
		public static final int VP9Profile3HDR10Plus = 0x2000;

		// VP9 levels
		public static final int VP9Level1 = 0x1;
		public static final int VP9Level11 = 0x2;
		public static final int VP9Level2 = 0x4;
		public static final int VP9Level21 = 0x8;
		public static final int VP9Level3 = 0x10;
		public static final int VP9Level31 = 0x20;
		public static final int VP9Level4 = 0x40;
		public static final int VP9Level41 = 0x80;
		public static final int VP9Level5 = 0x100;
		public static final int VP9Level51 = 0x200;
		public static final int VP9Level52 = 0x400;
		public static final int VP9Level6 = 0x800;
		public static final int VP9Level61 = 0x1000;
		public static final int VP9Level62 = 0x2000;

		// VP8 profile/level
		public static final int VP8ProfileMain = 0x01;
		public static final int VP8LevelMain = 0x01;

		// HEVC profiles
		public static final int HEVCProfileMain = 0x01;
		public static final int HEVCProfileMain10 = 0x02;
		public static final int HEVCProfileMain10HDR10 = 0x1000;
		public static final int HEVCProfileMainStill = 0x04;

		// HEVC levels
		public static final int HEVCMainTierLevel1 = 0x1;
		public static final int HEVCMainTierLevel2 = 0x2;
		public static final int HEVCMainTierLevel21 = 0x4;
		public static final int HEVCMainTierLevel3 = 0x8;
		public static final int HEVCMainTierLevel31 = 0x10;
		public static final int HEVCMainTierLevel4 = 0x20;
		public static final int HEVCMainTierLevel41 = 0x40;
		public static final int HEVCMainTierLevel5 = 0x80;
		public static final int HEVCMainTierLevel51 = 0x100;
		public static final int HEVCMainTierLevel52 = 0x200;
		public static final int HEVCMainTierLevel6 = 0x400;
		public static final int HEVCMainTierLevel61 = 0x800;
		public static final int HEVCMainTierLevel62 = 0x1000;
		public static final int HEVCHighTierLevel1 = 0x1;
		public static final int HEVCHighTierLevel2 = 0x2;
		public static final int HEVCHighTierLevel21 = 0x4;
		public static final int HEVCHighTierLevel3 = 0x8;
		public static final int HEVCHighTierLevel31 = 0x10;
		public static final int HEVCHighTierLevel4 = 0x20;
		public static final int HEVCHighTierLevel41 = 0x40;
		public static final int HEVCHighTierLevel5 = 0x80;
		public static final int HEVCHighTierLevel51 = 0x100;
		public static final int HEVCHighTierLevel52 = 0x200;
		public static final int HEVCHighTierLevel6 = 0x400;
		public static final int HEVCHighTierLevel61 = 0x800;
		public static final int HEVCHighTierLevel62 = 0x1000;

		// AAC profiles
		public static final int AACObjectMain = 1;
		public static final int AACObjectLC = 2;
		public static final int AACObjectSSR = 3;
		public static final int AACObjectLTP = 4;
		public static final int AACObjectHE = 5;
		public static final int AACObjectScalable = 6;
		public static final int AACObjectERLC = 17;
		public static final int AACObjectERScalable = 20;
		public static final int AACObjectLD = 23;
		public static final int AACObjectHE_PS = 29;
		public static final int AACObjectELD = 39;
		public static final int AACObjectXHE = 42;
	}

	/* AOSP MediaCodecInfo.EncoderCapabilities; never handed out, as ATL has no encoders */
	public static final class EncoderCapabilities {
		public static final int BITRATE_MODE_CQ = 0;
		public static final int BITRATE_MODE_VBR = 1;
		public static final int BITRATE_MODE_CBR = 2;
		public static final int BITRATE_MODE_CBR_FD = 3;

		private EncoderCapabilities() {}
	}

	public static class VideoCapabilities {
		/* AOSP initWithPlatformLimits(): LazyHolder.SIZE_RANGE of a 64-bit process, as no limits are published */
		private static final android.util.Range<Integer> SIZE_RANGE = new android.util.Range<Integer>(1, 32768);

		public android.util.Range<Integer> getSupportedWidths() {
			return SIZE_RANGE;
		}

		public android.util.Range<Integer> getSupportedHeights() {
			return SIZE_RANGE;
		}

		/* AOSP getSupportedWidthsFor(): with the platform limits' full aspect-ratio and block ranges and
		 * this alignment of 1 the result is the plain size range */
		public android.util.Range<Integer> getSupportedWidthsFor(int height) {
			if (!SIZE_RANGE.contains(height) || (height % getHeightAlignment()) != 0)
				throw new IllegalArgumentException("unsupported height");
			return SIZE_RANGE;
		}

		public android.util.Range<Integer> getSupportedHeightsFor(int width) {
			if (!SIZE_RANGE.contains(width) || (width % getWidthAlignment()) != 0)
				throw new IllegalArgumentException("unsupported width");
			return SIZE_RANGE;
		}

		public int getWidthAlignment() {
			return 1;
		}

		public int getHeightAlignment() {
			return 1;
		}

		public boolean areSizeAndRateSupported(int width, int height, double frameRate) {
			return width > 0 && height > 0 && frameRate > 0;
		}

		public boolean isSizeSupported(int width, int height) {
			return width > 0 && height > 0;
		}

		/* API 23: null, as AOSP's for a codec that published no measured frame rates (FFmpeg's here) */
		public android.util.Range<Double> getAchievableFrameRatesFor(int width, int height) {
			if (!isSizeSupported(width, height))
				throw new IllegalArgumentException("unsupported size");
			return null;
		}

		/* No codec-specific limits are published; report the full non-negative range. */
		public android.util.Range<Integer> getBitrateRange() {
			return new android.util.Range<Integer>(0, Integer.MAX_VALUE);
		}

		/* API 29: an empty list means the codec does not publish performance points */
		public java.util.List<PerformancePoint> getSupportedPerformancePoints() {
			return java.util.Collections.emptyList();
		}

		public static final class PerformancePoint {
			private final int width, height, frameRate;

			public PerformancePoint(int width, int height, int frameRate) {
				this.width = width;
				this.height = height;
				this.frameRate = frameRate;
			}

			public boolean covers(PerformancePoint other) {
				return width * height >= other.width * other.height && frameRate >= other.frameRate;
			}
		}
	}

	public static class AudioCapabilities {

		/* AOSP: null when the rates are given as ranges (mSampleRates == null), as here */
		public int[] getSupportedSampleRates() {
			return null;
		}

		public boolean isSampleRateSupported(int sampleRate) {
			return true;
		}

		public int getMaxInputChannelCount() {
			return 2;
		}

		/* No codec-specific limits are published; report the full non-negative range. */
		public android.util.Range<Integer> getBitrateRange() {
			return new android.util.Range<Integer>(0, Integer.MAX_VALUE);
		}

		/* every rate is accepted (see isSampleRateSupported); the range reaches up to FFmpeg's highest audio rate */
		public android.util.Range<Integer>[] getSupportedSampleRateRanges() {
			@SuppressWarnings("unchecked")
			android.util.Range<Integer>[] ranges = new android.util.Range[] {new android.util.Range<Integer>(8000, 192000)};
			return ranges;
		}
	}
}
