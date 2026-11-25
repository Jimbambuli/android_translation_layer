package android.media;

public class MediaCodecInfo {
	public static final int SECURITY_MODEL_SANDBOXED = 0;
	public static final int SECURITY_MODEL_MEMORY_SAFE = 1;

	private final String name;
	private final String mime;
	private final String decoder;
	private final boolean hardware;
	private final CodecProfileLevel[] profileLevels;
	private final int maxWidth;
	private final int maxHeight;
	private final boolean encoder;

	/* a codec of MediaCodecList: FFmpeg's decoder, a hardware (VA-API) or software codec, its profiles and levels
	 * and its largest frame */
	MediaCodecInfo(String name, String mime, String decoder, boolean hardware, CodecProfileLevel[] profileLevels,
	               int maxWidth, int maxHeight) {
		this(name, mime, decoder, hardware, profileLevels, maxWidth, maxHeight, false);
	}

	/* an encoder of MediaCodecList: FFmpeg's encoder (getDecoder() names it) */
	MediaCodecInfo(String name, String mime, String decoder, boolean hardware, CodecProfileLevel[] profileLevels,
	               int maxWidth, int maxHeight, boolean encoder) {
		this.encoder = encoder;
		this.name = name;
		this.mime = mime;
		this.decoder = decoder;
		this.hardware = hardware;
		this.profileLevels = profileLevels;
		this.maxWidth = maxWidth;
		this.maxHeight = maxHeight;
	}

	/** @hide FFmpeg's decoder (encoder) of the codec */
	public String getDecoder() {
		return decoder;
	}

	public String getName() {
		return name;
	}

	/* AOSP MediaCodecInfo.getCanonicalName(): the name of the underlying codec; ATL's codecs are no aliases
	 * (isAlias()), so it is the name */
	public final String getCanonicalName() {
		return name;
	}

	public boolean isEncoder() {
		return encoder;
	}

	/* API 29: no codec is listed under a second name */
	public boolean isAlias() {
		return false;
	}

	/* AOSP Codec2InfoBuilder: a software codec of the platform (owner "software") is software only; a vendor codec
	 * without the software-codec attribute is hardware accelerated. The VA-API codecs are such vendor codecs,
	 * FFmpeg's decoders the platform's software codecs. */
	public boolean isHardwareAccelerated() {
		return hardware;
	}

	public boolean isSoftwareOnly() {
		return !hardware;
	}

	public boolean isVendor() {
		return hardware;
	}

	public String[] getSupportedTypes() {
		return new String[] {mime};
	}

	public CodecCapabilities getCapabilitiesForType(String type) {
		/* AOSP: mCaps.get(type) == null -> IllegalArgumentException */
		if (!mime.equals(type))
			throw new IllegalArgumentException("codec does not support type");
		CodecCapabilities caps = new CodecCapabilities();
		caps.mimeType = type;
		caps.profileLevels = new CodecProfileLevel[profileLevels.length];
		for (int i = 0; i < profileLevels.length; i++)
			caps.profileLevels[i] = new CodecProfileLevel(profileLevels[i].profile, profileLevels[i].level);
		caps.videoCaps = new VideoCapabilities(maxWidth, maxHeight);
		/* AOSP's video encoders (C2SoftAvcEnc and the vendors'): input from a Surface or a YUV 4:2:0 ByteBuffer,
		 * with variable and constant bit rate */
		if (encoder && type.startsWith("video/")) {
			caps.colorFormats = new int[] {CodecCapabilities.COLOR_FormatYUV420Flexible, CodecCapabilities.COLOR_FormatSurface,
			                               CodecCapabilities.COLOR_FormatYUV420Planar, CodecCapabilities.COLOR_FormatYUV420SemiPlanar};
			caps.encoderCaps = new EncoderCapabilities((1 << EncoderCapabilities.BITRATE_MODE_VBR) | (1 << EncoderCapabilities.BITRATE_MODE_CBR));
		}
		return caps;
	}

	public static class CodecCapabilities {
		public static final int COLOR_FormatMonochrome = 1;
		public static final int COLOR_Format8bitRGB332 = 2;
		public static final int COLOR_Format12bitRGB444 = 3;
		public static final int COLOR_Format16bitARGB4444 = 4;
		public static final int COLOR_Format16bitARGB1555 = 5;
		public static final int COLOR_Format16bitRGB565 = 6;
		public static final int COLOR_Format16bitBGR565 = 7;
		public static final int COLOR_Format18bitRGB666 = 8;
		public static final int COLOR_Format18bitARGB1665 = 9;
		public static final int COLOR_Format19bitARGB1666 = 10;
		public static final int COLOR_Format24bitRGB888 = 11;
		public static final int COLOR_Format24bitBGR888 = 12;
		public static final int COLOR_Format24bitARGB1887 = 13;
		public static final int COLOR_Format25bitARGB1888 = 14;
		public static final int COLOR_Format32bitBGRA8888 = 15;
		public static final int COLOR_Format32bitARGB8888 = 16;
		public static final int COLOR_FormatYUV411Planar = 17;
		public static final int COLOR_FormatYUV411PackedPlanar = 18;
		public static final int COLOR_FormatYUV420Planar = 19;
		public static final int COLOR_FormatYUV420PackedPlanar = 20;
		public static final int COLOR_FormatYUV420SemiPlanar = 21;
		public static final int COLOR_FormatYUV422Planar = 22;
		public static final int COLOR_FormatYUV422PackedPlanar = 23;
		public static final int COLOR_FormatYUV422SemiPlanar = 24;
		public static final int COLOR_FormatYCbYCr = 25;
		public static final int COLOR_FormatYCrYCb = 26;
		public static final int COLOR_FormatCbYCrY = 27;
		public static final int COLOR_FormatCrYCbY = 28;
		public static final int COLOR_FormatYUV444Interleaved = 29;
		public static final int COLOR_FormatRawBayer8bit = 30;
		public static final int COLOR_FormatRawBayer10bit = 31;
		public static final int COLOR_FormatRawBayer8bitcompressed = 32;
		public static final int COLOR_FormatL2 = 33;
		public static final int COLOR_FormatL4 = 34;
		public static final int COLOR_FormatL8 = 35;
		public static final int COLOR_FormatL16 = 36;
		public static final int COLOR_FormatL24 = 37;
		public static final int COLOR_FormatL32 = 38;
		public static final int COLOR_FormatYUV420PackedSemiPlanar = 39;
		public static final int COLOR_FormatYUV422PackedSemiPlanar = 40;
		public static final int COLOR_Format18BitBGR666 = 41;
		public static final int COLOR_Format24BitARGB6666 = 42;
		public static final int COLOR_Format24BitABGR6666 = 43;
		public static final int COLOR_FormatYUVP010 = 54;
		public static final int COLOR_FormatYUVP210 = 60;
		public static final int COLOR_TI_FormatYUV420PackedSemiPlanar = 0x7f000100;
		public static final int COLOR_FormatSurface = 0x7F000789;
		public static final int COLOR_Format64bitABGRFloat = 0x7F000F16;
		public static final int COLOR_Format32bitABGR8888 = 0x7F00A000;
		public static final int COLOR_Format32bitABGR2101010 = 0x7F00AAA2;
		public static final int COLOR_FormatYUV420Flexible = 0x7F420888;
		public static final int COLOR_FormatYUV422Flexible = 0x7F422888;
		public static final int COLOR_FormatYUV444Flexible = 0x7F444888;
		public static final int COLOR_FormatRGBFlexible = 0x7F36B888;
		public static final int COLOR_FormatRGBAFlexible = 0x7F36A888;
		public static final int COLOR_QCOM_FormatYUV420SemiPlanar = 0x7fa30c00;
		public static final String FEATURE_HlgEditing = "hlg-editing";
		public static final String FEATURE_DynamicColorAspects = "dynamic-color-aspects";
		public static final String FEATURE_Roi = "region-of-interest";
		public static final String FEATURE_DetachedSurface = "detached-surface";

		/* AOSP CodecCapabilities FEATURE_* */
		public static final String FEATURE_AdaptivePlayback = "adaptive-playback";
		public static final String FEATURE_SecurePlayback = "secure-playback";
		public static final String FEATURE_TunneledPlayback = "tunneled-playback";
		public static final String FEATURE_DynamicTimestamp = "dynamic-timestamp";
		public static final String FEATURE_FrameParsing = "frame-parsing";
		public static final String FEATURE_MultipleFrames = "multiple-frames";
		public static final String FEATURE_PartialFrame = "partial-frame";
		public static final String FEATURE_IntraRefresh = "intra-refresh";
		public static final String FEATURE_LowLatency = "low-latency";
		public static final String FEATURE_QpBounds = "qp-bounds";
		public static final String FEATURE_EncodingStatistics = "encoding-statistics";
		public static final String FEATURE_HdrEditing = "hdr-editing";

		/* AOSP: never null, empty for a codec without profiles */
		public CodecProfileLevel[] profileLevels = new CodecProfileLevel[0];

		private String mimeType;
		private VideoCapabilities videoCaps = new VideoCapabilities(VideoCapabilities.MAX_SIZE, VideoCapabilities.MAX_SIZE);

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

		private EncoderCapabilities encoderCaps;

		/* AOSP: mEncoderCaps, null for a decoder */
		public EncoderCapabilities getEncoderCapabilities() {
			return encoderCaps;
		}

		public VideoCapabilities getVideoCapabilities() {
			return videoCaps;
		}

		public int getMaxSupportedInstances() {
			return 1;
		}

		/* AOSP mDefaultFormat: holds the default values of the audio/video capabilities; ATL publishes none,
		 * so only the mime type is set, as AOSP does before adding the capability defaults */
		public MediaFormat getDefaultFormat() {
			MediaFormat format = new MediaFormat();
			format.setString(MediaFormat.KEY_MIME, mimeType);
			return format;
		}

		/* AOSP CodecCapabilities.isFormatSupported(): the mime type must match, requested decoder features
		 * must be supported (ATL supports and requires none) and a requested profile must be listed at a
		 * high enough level. ATL's audio/video capabilities accept every size and rate, so their
		 * supportsFormat() checks always pass. */
		private static final String[] DECODER_FEATURES = {
			FEATURE_AdaptivePlayback, FEATURE_SecurePlayback, FEATURE_TunneledPlayback, FEATURE_PartialFrame,
			FEATURE_FrameParsing, FEATURE_MultipleFrames, FEATURE_DynamicTimestamp, FEATURE_LowLatency,
		};

		public final boolean isFormatSupported(MediaFormat format) {
			String mime = format.getString(MediaFormat.KEY_MIME);
			if (mime != null && !mimeType.equalsIgnoreCase(mime))
				return false;
			/* AOSP VideoCapabilities.supportsFormat(): the size must be supported */
			if (mimeType.startsWith("video/") && format.containsKey(MediaFormat.KEY_WIDTH) && format.containsKey(MediaFormat.KEY_HEIGHT)
			    && !videoCaps.isSizeSupported(format.getInteger(MediaFormat.KEY_WIDTH), format.getInteger(MediaFormat.KEY_HEIGHT)))
				return false;
			for (String feature : DECODER_FEATURES) {
				if (!format.containsKey("feature-" + feature))
					continue;
				int yesNo = format.getInteger("feature-" + feature);
				if ((yesNo == 1 && !isFeatureSupported(feature)) || (yesNo == 0 && isFeatureRequired(feature)))
					return false;
			}
			if (format.containsKey(MediaFormat.KEY_PROFILE)) {
				int profile = format.getInteger(MediaFormat.KEY_PROFILE);
				Integer level = format.containsKey(MediaFormat.KEY_LEVEL) ? format.getInteger(MediaFormat.KEY_LEVEL) : null;
				boolean found = false;
				for (CodecProfileLevel pl : profileLevels) {
					if (pl.profile == profile && (level == null || "audio/mp4a-latm".equalsIgnoreCase(mimeType) || pl.level >= level)) {
						found = true;
						break;
					}
				}
				if (!found)
					return false;
			}
			return true;
		}
	
    /* AOSP 16 API; no backing hardware/service in ATL */
    public static android.media.MediaCodecInfo.CodecCapabilities createFromProfileLevel(String arg0, int arg1, int arg2) { return null; }
}

	public static final class CodecProfileLevel {
		public static final int H263ProfileBaseline = 0x01;
		public static final int H263ProfileH320Coding = 0x02;
		public static final int H263ProfileBackwardCompatible = 0x04;
		public static final int H263ProfileISWV2 = 0x08;
		public static final int H263ProfileISWV3 = 0x10;
		public static final int H263ProfileHighCompression = 0x20;
		public static final int H263ProfileInternet = 0x40;
		public static final int H263ProfileInterlace = 0x80;
		public static final int H263ProfileHighLatency = 0x100;
		public static final int H263Level10 = 0x01;
		public static final int H263Level20 = 0x02;
		public static final int H263Level30 = 0x04;
		public static final int H263Level40 = 0x08;
		public static final int H263Level45 = 0x10;
		public static final int H263Level50 = 0x20;
		public static final int H263Level60 = 0x40;
		public static final int H263Level70 = 0x80;
		public static final int MPEG4ProfileSimple = 0x01;
		public static final int MPEG4ProfileSimpleScalable = 0x02;
		public static final int MPEG4ProfileCore = 0x04;
		public static final int MPEG4ProfileMain = 0x08;
		public static final int MPEG4ProfileNbit = 0x10;
		public static final int MPEG4ProfileScalableTexture = 0x20;
		public static final int MPEG4ProfileSimpleFace = 0x40;
		public static final int MPEG4ProfileSimpleFBA = 0x80;
		public static final int MPEG4ProfileBasicAnimated = 0x100;
		public static final int MPEG4ProfileHybrid = 0x200;
		public static final int MPEG4ProfileAdvancedRealTime = 0x400;
		public static final int MPEG4ProfileCoreScalable = 0x800;
		public static final int MPEG4ProfileAdvancedCoding = 0x1000;
		public static final int MPEG4ProfileAdvancedCore = 0x2000;
		public static final int MPEG4ProfileAdvancedScalable = 0x4000;
		public static final int MPEG4ProfileAdvancedSimple = 0x8000;
		public static final int MPEG4Level0 = 0x01;
		public static final int MPEG4Level0b = 0x02;
		public static final int MPEG4Level1 = 0x04;
		public static final int MPEG4Level2 = 0x08;
		public static final int MPEG4Level3 = 0x10;
		public static final int MPEG4Level3b = 0x18;
		public static final int MPEG4Level4 = 0x20;
		public static final int MPEG4Level4a = 0x40;
		public static final int MPEG4Level5 = 0x80;
		public static final int MPEG4Level6 = 0x100;
		public static final int MPEG2ProfileSimple = 0x00;
		public static final int MPEG2ProfileMain = 0x01;
		public static final int MPEG2Profile422 = 0x02;
		public static final int MPEG2ProfileSNR = 0x03;
		public static final int MPEG2ProfileSpatial = 0x04;
		public static final int MPEG2ProfileHigh = 0x05;
		public static final int MPEG2LevelLL = 0x00;
		public static final int MPEG2LevelML = 0x01;
		public static final int MPEG2LevelH14 = 0x02;
		public static final int MPEG2LevelHL = 0x03;
		public static final int MPEG2LevelHP = 0x04;
		public static final int DolbyVisionProfileDvavPer = 0x1;
		public static final int DolbyVisionProfileDvavPen = 0x2;
		public static final int DolbyVisionProfileDvheDer = 0x4;
		public static final int DolbyVisionProfileDvheDen = 0x8;
		public static final int DolbyVisionProfileDvheDtr = 0x10;
		public static final int DolbyVisionProfileDvheStn = 0x20;
		public static final int DolbyVisionProfileDvheDth = 0x40;
		public static final int DolbyVisionProfileDvheDtb = 0x80;
		public static final int DolbyVisionProfileDvheSt = 0x100;
		public static final int DolbyVisionProfileDvavSe = 0x200;
		public static final int DolbyVisionProfileDvav110 = 0x400;
		public static final int DolbyVisionLevelHd24 = 0x1;
		public static final int DolbyVisionLevelHd30 = 0x2;
		public static final int DolbyVisionLevelFhd24 = 0x4;
		public static final int DolbyVisionLevelFhd30 = 0x8;
		public static final int DolbyVisionLevelFhd60 = 0x10;
		public static final int DolbyVisionLevelUhd24 = 0x20;
		public static final int DolbyVisionLevelUhd30 = 0x40;
		public static final int DolbyVisionLevelUhd48 = 0x80;
		public static final int DolbyVisionLevelUhd60 = 0x100;
		public static final int DolbyVisionLevelUhd120 = 0x200;
		public static final int DolbyVisionLevel8k30 = 0x400;
		public static final int DolbyVisionLevel8k60 = 0x800;
		public static final int DTS_HDProfileHRA = 0x1;
		public static final int DTS_HDProfileLBR = 0x2;
		public static final int DTS_HDProfileMA = 0x4;
		public static final int DTS_UHDProfileP1 = 0x1;
		public static final int DTS_UHDProfileP2 = 0x2;
		private static final int AC4BitstreamVersion0 = 0x01;
		private static final int AC4BitstreamVersion1 = 0x02;
		private static final int AC4BitstreamVersion2 = 0x04;
		private static final int AC4PresentationVersion0 = 0x01;
		private static final int AC4PresentationVersion1 = 0x02;
		private static final int AC4PresentationVersion2 = 0x04;
		public static final int AC4Profile00 = AC4BitstreamVersion0 << 8 | AC4PresentationVersion0;
		public static final int AC4Profile10 = AC4BitstreamVersion1 << 8 | AC4PresentationVersion0;
		public static final int AC4Profile11 = AC4BitstreamVersion1 << 8 | AC4PresentationVersion1;
		public static final int AC4Profile21 = AC4BitstreamVersion2 << 8 | AC4PresentationVersion1;
		public static final int AC4Profile22 = AC4BitstreamVersion2 << 8 | AC4PresentationVersion2;
		public static final int AC4Level0 = 0x01;
		public static final int AC4Level1 = 0x02;
		public static final int AC4Level2 = 0x04;
		public static final int AC4Level3 = 0x08;
		public static final int AC4Level4 = 0x10;
		public static final int APVProfile422_10 = 0x01;
		public static final int APVProfile422_10HDR10 = 0x1000;
		public static final int APVProfile422_10HDR10Plus = 0x2000;
		public static final int APVLevel1Band0 = 0x101;
		public static final int APVLevel1Band1 = 0x102;
		public static final int APVLevel1Band2 = 0x104;
		public static final int APVLevel1Band3 = 0x108;
		public static final int APVLevel11Band0 = 0x201;
		public static final int APVLevel11Band1 = 0x202;
		public static final int APVLevel11Band2 = 0x204;
		public static final int APVLevel11Band3 = 0x208;
		public static final int APVLevel2Band0 = 0x401;
		public static final int APVLevel2Band1 = 0x402;
		public static final int APVLevel2Band2 = 0x404;
		public static final int APVLevel2Band3 = 0x408;
		public static final int APVLevel21Band0 = 0x801;
		public static final int APVLevel21Band1 = 0x802;
		public static final int APVLevel21Band2 = 0x804;
		public static final int APVLevel21Band3 = 0x808;
		public static final int APVLevel3Band0 = 0x1001;
		public static final int APVLevel3Band1 = 0x1002;
		public static final int APVLevel3Band2 = 0x1004;
		public static final int APVLevel3Band3 = 0x1008;
		public static final int APVLevel31Band0 = 0x2001;
		public static final int APVLevel31Band1 = 0x2002;
		public static final int APVLevel31Band2 = 0x2004;
		public static final int APVLevel31Band3 = 0x2008;
		public static final int APVLevel4Band0 = 0x4001;
		public static final int APVLevel4Band1 = 0x4002;
		public static final int APVLevel4Band2 = 0x4004;
		public static final int APVLevel4Band3 = 0x4008;
		public static final int APVLevel41Band0 = 0x8001;
		public static final int APVLevel41Band1 = 0x8002;
		public static final int APVLevel41Band2 = 0x8004;
		public static final int APVLevel41Band3 = 0x8008;
		public static final int APVLevel5Band0 = 0x10001;
		public static final int APVLevel5Band1 = 0x10002;
		public static final int APVLevel5Band2 = 0x10004;
		public static final int APVLevel5Band3 = 0x10008;
		public static final int APVLevel51Band0 = 0x20001;
		public static final int APVLevel51Band1 = 0x20002;
		public static final int APVLevel51Band2 = 0x20004;
		public static final int APVLevel51Band3 = 0x20008;
		public static final int APVLevel6Band0 = 0x40001;
		public static final int APVLevel6Band1 = 0x40002;
		public static final int APVLevel6Band2 = 0x40004;
		public static final int APVLevel6Band3 = 0x40008;
		public static final int APVLevel61Band0 = 0x80001;
		public static final int APVLevel61Band1 = 0x80002;
		public static final int APVLevel61Band2 = 0x80004;
		public static final int APVLevel61Band3 = 0x80008;
		public static final int APVLevel7Band0 = 0x100001;
		public static final int APVLevel7Band1 = 0x100002;
		public static final int APVLevel7Band2 = 0x100004;
		public static final int APVLevel7Band3 = 0x100008;
		public static final int APVLevel71Band0 = 0x200001;
		public static final int APVLevel71Band1 = 0x200002;
		public static final int APVLevel71Band2 = 0x200004;
		public static final int APVLevel71Band3 = 0x200008;
		private static final int IAMF_CODEC_OPUS = 0x1;
		private static final int IAMF_CODEC_AAC = 0x1 << 1;
		private static final int IAMF_CODEC_FLAC = 0x1 << 2;
		private static final int IAMF_CODEC_PCM = 0x1 << 3;
		private static final int IAMF_PROFILE_SIMPLE = 0x1 << 16;
		private static final int IAMF_PROFILE_BASE = 0x1 << 17;
		private static final int IAMF_PROFILE_BASE_ENHANCED = 0x1 << 18;
		private static final int IAMF_v1 = 0x1 << 24;
		public static final int IAMFProfileSimpleOpus = IAMF_v1 + IAMF_PROFILE_SIMPLE + IAMF_CODEC_OPUS;
		public static final int IAMFProfileSimpleAac = IAMF_v1 + IAMF_PROFILE_SIMPLE + IAMF_CODEC_AAC;
		public static final int IAMFProfileSimpleFlac = IAMF_v1 + IAMF_PROFILE_SIMPLE + IAMF_CODEC_FLAC;
		public static final int IAMFProfileSimplePcm = IAMF_v1 + IAMF_PROFILE_SIMPLE + IAMF_CODEC_PCM;
		public static final int IAMFProfileBaseOpus = IAMF_v1 + IAMF_PROFILE_BASE + IAMF_CODEC_OPUS;
		public static final int IAMFProfileBaseAac = IAMF_v1 + IAMF_PROFILE_BASE + IAMF_CODEC_AAC;
		public static final int IAMFProfileBaseFlac = IAMF_v1 + IAMF_PROFILE_BASE + IAMF_CODEC_FLAC;
		public static final int IAMFProfileBasePcm = IAMF_v1 + IAMF_PROFILE_BASE + IAMF_CODEC_PCM;
		public static final int IAMFProfileBaseEnhancedOpus = IAMF_v1 + IAMF_PROFILE_BASE_ENHANCED + IAMF_CODEC_OPUS;
		public static final int IAMFProfileBaseEnhancedAac = IAMF_v1 + IAMF_PROFILE_BASE_ENHANCED + IAMF_CODEC_AAC;
		public static final int IAMFProfileBaseEnhancedFlac = IAMF_v1 + IAMF_PROFILE_BASE_ENHANCED + IAMF_CODEC_FLAC;
		public static final int IAMFProfileBaseEnhancedPcm = IAMF_v1 + IAMF_PROFILE_BASE_ENHANCED + IAMF_CODEC_PCM;

		public int profile;
		public int level;

		public CodecProfileLevel() {}

		public CodecProfileLevel(int profile, int level) {
			this.profile = profile;
			this.level = level;
		}

		/* AOSP 16 CodecProfileLevel constants of the video codecs */
		// AVC profiles
		public static final int AVCProfileBaseline = 0x01;
		public static final int AVCProfileMain = 0x02;
		public static final int AVCProfileExtended = 0x04;
		public static final int AVCProfileHigh = 0x08;
		public static final int AVCProfileHigh10 = 0x10;
		public static final int AVCProfileHigh422 = 0x20;
		public static final int AVCProfileHigh444 = 0x40;
		public static final int AVCProfileConstrainedBaseline = 0x10000;
		public static final int AVCProfileConstrainedHigh = 0x80000;

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

		// VP8 profiles and levels
		public static final int VP8Level_Version0 = 0x01;
		public static final int VP8Level_Version1 = 0x02;
		public static final int VP8Level_Version2 = 0x04;
		public static final int VP8Level_Version3 = 0x08;
		public static final int VP8ProfileMain = 0x01;

		// VP9 profiles
		public static final int VP9Profile0 = 0x01;
		public static final int VP9Profile1 = 0x02;
		public static final int VP9Profile2 = 0x04;
		public static final int VP9Profile3 = 0x08;
		public static final int VP9Profile2HDR = 0x1000;
		public static final int VP9Profile3HDR = 0x2000;
		public static final int VP9Profile2HDR10Plus = 0x4000;
		public static final int VP9Profile3HDR10Plus = 0x8000;

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

		// HEVC profiles
		public static final int HEVCProfileMain = 0x01;
		public static final int HEVCProfileMain10 = 0x02;
		public static final int HEVCProfileMainStill = 0x04;
		public static final int HEVCProfileMain10HDR10 = 0x1000;
		public static final int HEVCProfileMain10HDR10Plus = 0x2000;

		// HEVC levels
		public static final int HEVCMainTierLevel1 = 0x1;
		public static final int HEVCHighTierLevel1 = 0x2;
		public static final int HEVCMainTierLevel2 = 0x4;
		public static final int HEVCHighTierLevel2 = 0x8;
		public static final int HEVCMainTierLevel21 = 0x10;
		public static final int HEVCHighTierLevel21 = 0x20;
		public static final int HEVCMainTierLevel3 = 0x40;
		public static final int HEVCHighTierLevel3 = 0x80;
		public static final int HEVCMainTierLevel31 = 0x100;
		public static final int HEVCHighTierLevel31 = 0x200;
		public static final int HEVCMainTierLevel4 = 0x400;
		public static final int HEVCHighTierLevel4 = 0x800;
		public static final int HEVCMainTierLevel41 = 0x1000;
		public static final int HEVCHighTierLevel41 = 0x2000;
		public static final int HEVCMainTierLevel5 = 0x4000;
		public static final int HEVCHighTierLevel5 = 0x8000;
		public static final int HEVCMainTierLevel51 = 0x10000;
		public static final int HEVCHighTierLevel51 = 0x20000;
		public static final int HEVCMainTierLevel52 = 0x40000;
		public static final int HEVCHighTierLevel52 = 0x80000;
		public static final int HEVCMainTierLevel6 = 0x100000;
		public static final int HEVCHighTierLevel6 = 0x200000;
		public static final int HEVCMainTierLevel61 = 0x400000;
		public static final int HEVCHighTierLevel61 = 0x800000;
		public static final int HEVCMainTierLevel62 = 0x1000000;
		public static final int HEVCHighTierLevel62 = 0x2000000;

		// AV1 profiles
		public static final int AV1ProfileMain8 = 0x1;
		public static final int AV1ProfileMain10 = 0x2;
		public static final int AV1ProfileMain10HDR10 = 0x1000;
		public static final int AV1ProfileMain10HDR10Plus = 0x2000;

		// AV1 levels
		public static final int AV1Level2 = 0x1;
		public static final int AV1Level21 = 0x2;
		public static final int AV1Level22 = 0x4;
		public static final int AV1Level23 = 0x8;
		public static final int AV1Level3 = 0x10;
		public static final int AV1Level31 = 0x20;
		public static final int AV1Level32 = 0x40;
		public static final int AV1Level33 = 0x80;
		public static final int AV1Level4 = 0x100;
		public static final int AV1Level41 = 0x200;
		public static final int AV1Level42 = 0x400;
		public static final int AV1Level43 = 0x800;
		public static final int AV1Level5 = 0x1000;
		public static final int AV1Level51 = 0x2000;
		public static final int AV1Level52 = 0x4000;
		public static final int AV1Level53 = 0x8000;
		public static final int AV1Level6 = 0x10000;
		public static final int AV1Level61 = 0x20000;
		public static final int AV1Level62 = 0x40000;
		public static final int AV1Level63 = 0x80000;
		public static final int AV1Level7 = 0x100000;
		public static final int AV1Level71 = 0x200000;
		public static final int AV1Level72 = 0x400000;
		public static final int AV1Level73 = 0x800000;
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

	/* AOSP MediaCodecInfo.EncoderCapabilities of a video encoder: the bit rate modes it supports */
	public static final class EncoderCapabilities {
		public static final int BITRATE_MODE_CQ = 0;
		public static final int BITRATE_MODE_VBR = 1;
		public static final int BITRATE_MODE_CBR = 2;
		public static final int BITRATE_MODE_CBR_FD = 3;

		/* AOSP defaults of an EncoderCapabilities without published quality and complexity limits */
		private final android.util.Range<Integer> mQualityRange = new android.util.Range<Integer>(0, 0);
		private final android.util.Range<Integer> mComplexityRange = new android.util.Range<Integer>(0, 0);
		private int mBitControl = 0;

		private EncoderCapabilities(int bitControl) {
			mBitControl = bitControl;
		}

		public android.util.Range<Integer> getQualityRange() {
			return mQualityRange;
		}

		public android.util.Range<Integer> getComplexityRange() {
			return mComplexityRange;
		}

		public boolean isBitrateModeSupported(int mode) {
			if (mode < BITRATE_MODE_CQ || mode > BITRATE_MODE_CBR_FD)
				return false;
			return (mBitControl & (1 << mode)) != 0;
		}
	}

	public static class VideoCapabilities {
		/* AOSP initWithPlatformLimits(): LazyHolder.SIZE_RANGE of a 64-bit process; a software codec publishes no
		 * smaller limit, a VA-API codec its largest frame */
		static final int MAX_SIZE = 32768;
		private final android.util.Range<Integer> widths;
		private final android.util.Range<Integer> heights;

		VideoCapabilities(int maxWidth, int maxHeight) {
			widths = new android.util.Range<Integer>(1, maxWidth);
			heights = new android.util.Range<Integer>(1, maxHeight);
		}

		public android.util.Range<Integer> getSupportedWidths() {
			return widths;
		}

		public android.util.Range<Integer> getSupportedHeights() {
			return heights;
		}

		/* AOSP getSupportedWidthsFor(): with the platform limits' full aspect-ratio and block ranges and
		 * this alignment of 1 the result is the plain size range */
		public android.util.Range<Integer> getSupportedWidthsFor(int height) {
			if (!heights.contains(height) || (height % getHeightAlignment()) != 0)
				throw new IllegalArgumentException("unsupported height");
			return widths;
		}

		public android.util.Range<Integer> getSupportedHeightsFor(int width) {
			if (!widths.contains(width) || (width % getWidthAlignment()) != 0)
				throw new IllegalArgumentException("unsupported width");
			return heights;
		}

		/* AOSP FRAME_RATE_RANGE of initWithPlatformLimits() */
		public android.util.Range<Integer> getSupportedFrameRates() {
			return new android.util.Range<Integer>(0, 960);
		}

		/* AOSP getSupportedFrameRatesFor() with the platform limits: 2x2 blocks and an unbounded
		 * blocks-per-second range */
		public android.util.Range<Double> getSupportedFrameRatesFor(int width, int height) {
			if (!isSizeSupported(width, height))
				throw new IllegalArgumentException("unsupported size");
			long blockCount = (long)((width + 1) / 2) * ((height + 1) / 2);
			return new android.util.Range<Double>(Math.max(1.0 / blockCount, 0.0), Math.min(Long.MAX_VALUE / (double)blockCount, 960.0));
		}

		public int getWidthAlignment() {
			return 1;
		}

		public int getHeightAlignment() {
			return 1;
		}

		public boolean areSizeAndRateSupported(int width, int height, double frameRate) {
			return isSizeSupported(width, height) && frameRate > 0;
		}

		public boolean isSizeSupported(int width, int height) {
			return widths.contains(width) && heights.contains(height);
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
			public static final PerformancePoint SD_24 = new PerformancePoint(720, 480, 24);
			public static final PerformancePoint SD_25 = new PerformancePoint(720, 576, 25);
			public static final PerformancePoint SD_30 = new PerformancePoint(720, 480, 30);
			public static final PerformancePoint SD_48 = new PerformancePoint(720, 480, 48);
			public static final PerformancePoint SD_50 = new PerformancePoint(720, 576, 50);
			public static final PerformancePoint SD_60 = new PerformancePoint(720, 480, 60);
			public static final PerformancePoint HD_24 = new PerformancePoint(1280, 720, 24);
			public static final PerformancePoint HD_25 = new PerformancePoint(1280, 720, 25);
			public static final PerformancePoint HD_30 = new PerformancePoint(1280, 720, 30);
			public static final PerformancePoint HD_50 = new PerformancePoint(1280, 720, 50);
			public static final PerformancePoint HD_60 = new PerformancePoint(1280, 720, 60);
			public static final PerformancePoint HD_100 = new PerformancePoint(1280, 720, 100);
			public static final PerformancePoint HD_120 = new PerformancePoint(1280, 720, 120);
			public static final PerformancePoint HD_200 = new PerformancePoint(1280, 720, 200);
			public static final PerformancePoint HD_240 = new PerformancePoint(1280, 720, 240);
			public static final PerformancePoint FHD_24 = new PerformancePoint(1920, 1080, 24);
			public static final PerformancePoint FHD_25 = new PerformancePoint(1920, 1080, 25);
			public static final PerformancePoint FHD_30 = new PerformancePoint(1920, 1080, 30);
			public static final PerformancePoint FHD_50 = new PerformancePoint(1920, 1080, 50);
			public static final PerformancePoint FHD_60 = new PerformancePoint(1920, 1080, 60);
			public static final PerformancePoint FHD_100 = new PerformancePoint(1920, 1080, 100);
			public static final PerformancePoint FHD_120 = new PerformancePoint(1920, 1080, 120);
			public static final PerformancePoint FHD_200 = new PerformancePoint(1920, 1080, 200);
			public static final PerformancePoint FHD_240 = new PerformancePoint(1920, 1080, 240);
			public static final PerformancePoint UHD_24 = new PerformancePoint(3840, 2160, 24);
			public static final PerformancePoint UHD_25 = new PerformancePoint(3840, 2160, 25);
			public static final PerformancePoint UHD_30 = new PerformancePoint(3840, 2160, 30);
			public static final PerformancePoint UHD_50 = new PerformancePoint(3840, 2160, 50);
			public static final PerformancePoint UHD_60 = new PerformancePoint(3840, 2160, 60);
			public static final PerformancePoint UHD_100 = new PerformancePoint(3840, 2160, 100);
			public static final PerformancePoint UHD_120 = new PerformancePoint(3840, 2160, 120);
			public static final PerformancePoint UHD_200 = new PerformancePoint(3840, 2160, 200);
			public static final PerformancePoint UHD_240 = new PerformancePoint(3840, 2160, 240);

			private final int width, height, frameRate;

			public PerformancePoint(int width, int height, int frameRate) {
				this.width = width;
				this.height = height;
				this.frameRate = frameRate;
			}

			/* AOSP covers(MediaFormat): the format's size and frame rate (rounded up to a whole number) as a point */
			public boolean covers(MediaFormat format) {
				int frameRate = 0;
				switch (format.getValueTypeForKey("frame-rate")) {
					case MediaFormat.TYPE_INTEGER:
						frameRate = format.getInteger("frame-rate", 0);
						break;
					case MediaFormat.TYPE_LONG:
						frameRate = (int) Math.round((float) Math.ceil(format.getLong("frame-rate", 0)));
						break;
					case MediaFormat.TYPE_FLOAT:
						frameRate = Math.round((float) Math.ceil(format.getFloat("frame-rate", 0)));
						break;
				}
				return covers(new PerformancePoint(format.getInteger(MediaFormat.KEY_WIDTH, 0),
						format.getInteger(MediaFormat.KEY_HEIGHT, 0), frameRate));
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

		/* AOSP: the lowest bound of getInputChannelCountRanges() */
		public int getMinInputChannelCount() {
			return 1;
		}

		/* AOSP mInputChannelRanges: a single range [1..getMaxInputChannelCount()] */
		public android.util.Range<Integer>[] getInputChannelCountRanges() {
			@SuppressWarnings("unchecked")
			android.util.Range<Integer>[] ranges = new android.util.Range[] {new android.util.Range<Integer>(1, getMaxInputChannelCount())};
			return ranges;
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
    /* AOSP 16 API; no backing hardware/service in ATL */
    public int getSecurityModel() { return 0; }
}
