package android.net.rtp;

import java.util.Arrays;

/* android.net.rtp.AudioCodec (framework-current.txt); the codec table and getCodec() as in the AOSP class */
@Deprecated
public class AudioCodec {
	public static final AudioCodec PCMU = new AudioCodec(0, "PCMU/8000", null);
	public static final AudioCodec PCMA = new AudioCodec(8, "PCMA/8000", null);
	public static final AudioCodec GSM = new AudioCodec(3, "GSM/8000", null);
	public static final AudioCodec GSM_EFR = new AudioCodec(96, "GSM-EFR/8000", null);
	public static final AudioCodec AMR = new AudioCodec(96, "AMR/8000", null);

	private static final AudioCodec[] sCodecs = {GSM_EFR, AMR, GSM, PCMU, PCMA};

	private AudioCodec(int type, String rtpmap, String fmtp) {
		this.type = type;
		this.rtpmap = rtpmap;
		this.fmtp = fmtp;
	}

	public final int type;
	public final String rtpmap;
	public final String fmtp;

	public static AudioCodec[] getCodecs() {
		return Arrays.copyOf(sCodecs, sCodecs.length);
	}

	public static AudioCodec getCodec(int type, String rtpmap, String fmtp) {
		if (type < 0 || type > 127) {
			return null;
		}

		AudioCodec hint = null;
		if (rtpmap != null) {
			rtpmap = rtpmap.trim().toUpperCase();
			for (AudioCodec codec : sCodecs) {
				if (rtpmap.startsWith(codec.rtpmap)) {
					String s = rtpmap.substring(codec.rtpmap.length());
					if (s.length() == 0 || s.equals("/1")) {
						hint = codec;
					}
					break;
				}
			}
		} else if (type < 96) {
			for (AudioCodec codec : sCodecs) {
				if (type == codec.type) {
					hint = codec;
					rtpmap = codec.rtpmap;
					break;
				}
			}
		}

		if (hint == null) {
			return null;
		}

		if (hint == AMR && fmtp != null) {
			String s = fmtp.toLowerCase();
			if (s.contains("crc=1") || s.contains("robust-sorting=1") || s.contains("interleaving=")) {
				return null;
			}
		}
		return new AudioCodec(type, rtpmap, fmtp);
	}
}
