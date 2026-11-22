package android.media;

public class MediaCodecList {
	public static final int REGULAR_CODECS = 0;
	public static final int ALL_CODECS = 1;

	public MediaCodecList(int kind) {}

	/* the software codecs: FFmpeg's decoders (name, type, FFmpeg's decoder) */
	private static final String[][] SOFTWARE_CODECS = {
		{"aac", "audio/mp4a-latm", "aac"},
		{"h264", "video/avc", "h264"},
		{"mp3", "audio/mpeg", "mp3"},
		{"opus", "audio/opus", "opus"},
		{"vp8", "video/x-vnd.on2.vp8", "vp8"},
		{"vp9", "video/x-vnd.on2.vp9", "vp9"},
		{"hevc", "video/hevc", "hevc"},
		{"av1", "video/av01", "libdav1d"},
	};

	/* the hardware codecs: FFmpeg's decoders with VA-API, in the order of the types of
	 * MediaCodec.native_hardwareProfiles() */
	private static final String[][] HARDWARE_CODECS = {
		{"c2.vaapi.avc.decoder", "video/avc", "h264"},
		{"c2.vaapi.hevc.decoder", "video/hevc", "hevc"},
		{"c2.vaapi.vp8.decoder", "video/x-vnd.on2.vp8", "vp8"},
		{"c2.vaapi.vp9.decoder", "video/x-vnd.on2.vp9", "vp9"},
		{"c2.vaapi.av1.decoder", "video/av01", "av1"},
	};

	private static MediaCodecInfo[] codecs;

	/* AOSP lists the vendor (hardware) codecs before the platform's software codecs. A hardware codec is listed for
	 * a type the VA-API device decodes, with the profiles it decodes; without a VA-API device the list has only the
	 * software codecs. */
	private static synchronized MediaCodecInfo[] getCodecs() {
		if (codecs != null)
			return codecs;
		java.util.ArrayList<MediaCodecInfo> list = new java.util.ArrayList<>();
		int[] hardware = MediaCodec.native_hardwareProfiles();
		for (int type = 0; type < HARDWARE_CODECS.length; type++) {
			java.util.ArrayList<MediaCodecInfo.CodecProfileLevel> profiles = new java.util.ArrayList<>();
			int maxWidth = 0, maxHeight = 0;
			for (int i = 0; i + 3 < hardware.length; i += 4) {
				if (hardware[i] != type)
					continue;
				profiles.add(new MediaCodecInfo.CodecProfileLevel(hardware[i + 1], levelForSize(HARDWARE_CODECS[type][1], hardware[i + 2], hardware[i + 3])));
				maxWidth = Math.max(maxWidth, hardware[i + 2]);
				maxHeight = Math.max(maxHeight, hardware[i + 3]);
			}
			if (!profiles.isEmpty())
				list.add(new MediaCodecInfo(HARDWARE_CODECS[type][0], HARDWARE_CODECS[type][1], HARDWARE_CODECS[type][2], true,
				                            profiles.toArray(new MediaCodecInfo.CodecProfileLevel[0]), maxWidth, maxHeight));
		}
		for (String[] codec : SOFTWARE_CODECS)
			list.add(new MediaCodecInfo(codec[0], codec[1], codec[2], false, softwareProfileLevels(codec[1]),
			                            MediaCodecInfo.VideoCapabilities.MAX_SIZE, MediaCodecInfo.VideoCapabilities.MAX_SIZE));
		codecs = list.toArray(new MediaCodecInfo[0]);
		return codecs;
	}

	/* the profiles FFmpeg's software decoders decode (as the types' profiles of AOSP's software codecs, plus the
	 * ones FFmpeg decodes beyond them), at the highest level: FFmpeg sets no limit of its own */
	private static MediaCodecInfo.CodecProfileLevel[] softwareProfileLevels(String mime) {
		int[] profiles;
		int level;
		switch (mime) {
			case "video/avc":
				profiles = new int[] {MediaCodecInfo.CodecProfileLevel.AVCProfileConstrainedBaseline, MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline,
				                      MediaCodecInfo.CodecProfileLevel.AVCProfileMain, MediaCodecInfo.CodecProfileLevel.AVCProfileConstrainedHigh,
				                      MediaCodecInfo.CodecProfileLevel.AVCProfileHigh, MediaCodecInfo.CodecProfileLevel.AVCProfileHigh10,
				                      MediaCodecInfo.CodecProfileLevel.AVCProfileHigh422, MediaCodecInfo.CodecProfileLevel.AVCProfileHigh444};
				level = MediaCodecInfo.CodecProfileLevel.AVCLevel62;
				break;
			case "video/hevc":
				profiles = new int[] {MediaCodecInfo.CodecProfileLevel.HEVCProfileMain, MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10,
				                      MediaCodecInfo.CodecProfileLevel.HEVCProfileMainStill};
				level = MediaCodecInfo.CodecProfileLevel.HEVCMainTierLevel62;
				break;
			case "video/x-vnd.on2.vp8":
				profiles = new int[] {MediaCodecInfo.CodecProfileLevel.VP8ProfileMain};
				level = MediaCodecInfo.CodecProfileLevel.VP8Level_Version0;
				break;
			case "video/x-vnd.on2.vp9":
				profiles = new int[] {MediaCodecInfo.CodecProfileLevel.VP9Profile0, MediaCodecInfo.CodecProfileLevel.VP9Profile1,
				                      MediaCodecInfo.CodecProfileLevel.VP9Profile2, MediaCodecInfo.CodecProfileLevel.VP9Profile3};
				level = MediaCodecInfo.CodecProfileLevel.VP9Level62;
				break;
			case "video/av01":
				profiles = new int[] {MediaCodecInfo.CodecProfileLevel.AV1ProfileMain8, MediaCodecInfo.CodecProfileLevel.AV1ProfileMain10};
				level = MediaCodecInfo.CodecProfileLevel.AV1Level73;
				break;
			default:
				return new MediaCodecInfo.CodecProfileLevel[0];
		}
		MediaCodecInfo.CodecProfileLevel[] levels = new MediaCodecInfo.CodecProfileLevel[profiles.length];
		for (int i = 0; i < profiles.length; i++)
			levels[i] = new MediaCodecInfo.CodecProfileLevel(profiles[i], level);
		return levels;
	}

	/* the level of a VA-API profile: VA-API names the largest frame, not a level or a frame rate. The level is the
	 * highest of the size class (the spec's largest picture: AVC MaxFS, HEVC/VP9/AV1 MaxLumaPs/MaxPicSize) that frame
	 * reaches; within the class the highest sub-level, as the decoder's frame rate is not known. */
	private static int levelForSize(String mime, int width, int height) {
		long pixels = (long)width * height;
		long macroblocks = (long)((width + 15) / 16) * ((height + 15) / 16);
		switch (mime) {
			case "video/avc":
				return macroblocks >= 139264 ? MediaCodecInfo.CodecProfileLevel.AVCLevel62 : macroblocks >= 36864 ? MediaCodecInfo.CodecProfileLevel.AVCLevel52 : macroblocks >= 8704 ? MediaCodecInfo.CodecProfileLevel.AVCLevel42 : MediaCodecInfo.CodecProfileLevel.AVCLevel32;
			case "video/hevc":
				return pixels >= 35651584 ? MediaCodecInfo.CodecProfileLevel.HEVCMainTierLevel62 : pixels >= 8912896 ? MediaCodecInfo.CodecProfileLevel.HEVCMainTierLevel52 : pixels >= 2228224 ? MediaCodecInfo.CodecProfileLevel.HEVCMainTierLevel41 : MediaCodecInfo.CodecProfileLevel.HEVCMainTierLevel31;
			case "video/x-vnd.on2.vp9":
				return pixels >= 35651584 ? MediaCodecInfo.CodecProfileLevel.VP9Level62 : pixels >= 8912896 ? MediaCodecInfo.CodecProfileLevel.VP9Level52 : pixels >= 2228224 ? MediaCodecInfo.CodecProfileLevel.VP9Level41 : MediaCodecInfo.CodecProfileLevel.VP9Level31;
			case "video/av01":
				return pixels >= 35651584 ? MediaCodecInfo.CodecProfileLevel.AV1Level63 : pixels >= 8912896 ? MediaCodecInfo.CodecProfileLevel.AV1Level53 : pixels >= 2228224 ? MediaCodecInfo.CodecProfileLevel.AV1Level43 : MediaCodecInfo.CodecProfileLevel.AV1Level33;
			default:
				return MediaCodecInfo.CodecProfileLevel.VP8Level_Version0;
		}
	}

	public static int getCodecCount() {
		return getCodecs().length;
	}

	/* AOSP: IllegalArgumentException for an index below 0 or above the count (the count itself is out of the array) */
	public static MediaCodecInfo getCodecInfoAt(int index) {
		MediaCodecInfo[] infos = getCodecs();
		if (index < 0 || index > infos.length)
			throw new IllegalArgumentException();
		return infos[index];
	}

	/** @hide the codec of that name, or null */
	static MediaCodecInfo getInfoByName(String name) {
		for (MediaCodecInfo info : getCodecs())
			if (info.getName().equals(name))
				return info;
		return null;
	}

	/* AOSP findEncoderForFormat()/findCodecForFormat(): the first codec of the requested kind whose
	 * capabilities support the format; ATL lists only decoders, so no encoder is ever found */
	public final String findDecoderForFormat(MediaFormat format) {
		return findCodecForFormat(false, format);
	}

	public final String findEncoderForFormat(MediaFormat format) {
		return findCodecForFormat(true, format);
	}

	private String findCodecForFormat(boolean encoder, MediaFormat format) {
		String mime = format.getString(MediaFormat.KEY_MIME);
		for (MediaCodecInfo info : getCodecInfos()) {
			if (info.isEncoder() != encoder)
				continue;
			try {
				MediaCodecInfo.CodecCapabilities caps = info.getCapabilitiesForType(mime);
				if (caps != null && caps.isFormatSupported(format))
					return info.getName();
			} catch (IllegalArgumentException e) {
				// type is not supported
			}
		}
		return null;
	}

	public MediaCodecInfo[] getCodecInfos() {
		return getCodecs().clone();
	}
}
