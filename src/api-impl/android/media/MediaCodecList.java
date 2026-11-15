package android.media;

public class MediaCodecList {

	public MediaCodecList(int kind) {}

	public static int getCodecCount() {
		return 6;
	}

	public static MediaCodecInfo getCodecInfoAt(int index) {
		switch (index) {
			case 0:
				return new MediaCodecInfo("aac", "audio/mp4a-latm");
			case 1:
				return new MediaCodecInfo("h264", "video/avc");
			case 2:
				return new MediaCodecInfo("mp3", "audio/mpeg");
			case 3:
				return new MediaCodecInfo("opus", "audio/opus");
			case 4:
				return new MediaCodecInfo("vp8", "video/x-vnd.on2.vp8");
			case 5:
				return new MediaCodecInfo("vp9", "video/x-vnd.on2.vp9");
			default:
				return null;
		}
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
		MediaCodecInfo[] infos = new MediaCodecInfo[getCodecCount()];
		for (int i = 0; i < infos.length; i++)
			infos[i] = getCodecInfoAt(i);
		return infos;
	}
}
