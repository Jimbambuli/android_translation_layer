package android.media;

public class AudioFormat implements android.os.Parcelable {
	public static final int ENCODING_AC4_L4 = 32;
	public static final int ENCODING_IAMF_SIMPLE_PROFILE_OPUS = 33;
	public static final int ENCODING_IAMF_SIMPLE_PROFILE_AAC = 34;
	public static final int ENCODING_IAMF_SIMPLE_PROFILE_FLAC = 35;
	public static final int ENCODING_IAMF_SIMPLE_PROFILE_PCM = 36;
	public static final int ENCODING_IAMF_BASE_PROFILE_OPUS = 37;
	public static final int ENCODING_IAMF_BASE_PROFILE_AAC = 38;
	public static final int ENCODING_IAMF_BASE_PROFILE_FLAC = 39;
	public static final int ENCODING_IAMF_BASE_PROFILE_PCM = 40;
	public static final int ENCODING_IAMF_BASE_ENHANCED_PROFILE_OPUS = 41;
	public static final int ENCODING_IAMF_BASE_ENHANCED_PROFILE_AAC = 42;
	public static final int ENCODING_IAMF_BASE_ENHANCED_PROFILE_FLAC = 43;
	public static final int ENCODING_IAMF_BASE_ENHANCED_PROFILE_PCM = 44;
	public static final int CHANNEL_CONFIGURATION_INVALID = 0;
	public static final int CHANNEL_CONFIGURATION_DEFAULT = 1;
	public static final int CHANNEL_CONFIGURATION_MONO = 2;
	public static final int CHANNEL_CONFIGURATION_STEREO = 3;
	public static final int CHANNEL_OUT_DEFAULT = 1;
	public static final int CHANNEL_OUT_FRONT_LEFT = 0x4;
	public static final int CHANNEL_OUT_FRONT_RIGHT = 0x8;
	public static final int CHANNEL_OUT_FRONT_CENTER = 0x10;
	public static final int CHANNEL_OUT_LOW_FREQUENCY = 0x20;
	public static final int CHANNEL_OUT_BACK_LEFT = 0x40;
	public static final int CHANNEL_OUT_BACK_RIGHT = 0x80;
	public static final int CHANNEL_OUT_FRONT_LEFT_OF_CENTER = 0x100;
	public static final int CHANNEL_OUT_FRONT_RIGHT_OF_CENTER = 0x200;
	public static final int CHANNEL_OUT_BACK_CENTER = 0x400;
	public static final int CHANNEL_OUT_SIDE_LEFT = 0x800;
	public static final int CHANNEL_OUT_SIDE_RIGHT = 0x1000;
	public static final int CHANNEL_OUT_TOP_CENTER = 0x2000;
	public static final int CHANNEL_OUT_TOP_FRONT_LEFT = 0x4000;
	public static final int CHANNEL_OUT_TOP_FRONT_CENTER = 0x8000;
	public static final int CHANNEL_OUT_TOP_FRONT_RIGHT = 0x10000;
	public static final int CHANNEL_OUT_TOP_BACK_LEFT = 0x20000;
	public static final int CHANNEL_OUT_TOP_BACK_CENTER = 0x40000;
	public static final int CHANNEL_OUT_TOP_BACK_RIGHT = 0x80000;
	public static final int CHANNEL_OUT_TOP_SIDE_LEFT = 0x100000;
	public static final int CHANNEL_OUT_TOP_SIDE_RIGHT = 0x200000;
	public static final int CHANNEL_OUT_BOTTOM_FRONT_LEFT = 0x400000;
	public static final int CHANNEL_OUT_BOTTOM_FRONT_CENTER = 0x800000;
	public static final int CHANNEL_OUT_BOTTOM_FRONT_RIGHT = 0x1000000;
	public static final int CHANNEL_OUT_LOW_FREQUENCY_2 = 0x2000000;
	public static final int CHANNEL_OUT_FRONT_WIDE_LEFT = 0x4000000;
	public static final int CHANNEL_OUT_FRONT_WIDE_RIGHT = 0x8000000;
	public static final int CHANNEL_OUT_MONO = CHANNEL_OUT_FRONT_LEFT;
	public static final int CHANNEL_OUT_STEREO = (CHANNEL_OUT_FRONT_LEFT | CHANNEL_OUT_FRONT_RIGHT);
	public static final int CHANNEL_OUT_QUAD = (CHANNEL_OUT_FRONT_LEFT | CHANNEL_OUT_FRONT_RIGHT | CHANNEL_OUT_BACK_LEFT | CHANNEL_OUT_BACK_RIGHT);
	public static final int CHANNEL_OUT_SURROUND = (CHANNEL_OUT_FRONT_LEFT | CHANNEL_OUT_FRONT_RIGHT | CHANNEL_OUT_FRONT_CENTER | CHANNEL_OUT_BACK_CENTER);
	public static final int CHANNEL_OUT_5POINT1 = (CHANNEL_OUT_FRONT_LEFT | CHANNEL_OUT_FRONT_RIGHT | CHANNEL_OUT_FRONT_CENTER | CHANNEL_OUT_LOW_FREQUENCY | CHANNEL_OUT_BACK_LEFT | CHANNEL_OUT_BACK_RIGHT);
	public static final int CHANNEL_OUT_6POINT1 = (CHANNEL_OUT_5POINT1 | CHANNEL_OUT_BACK_CENTER);
	public static final int CHANNEL_OUT_7POINT1 = (CHANNEL_OUT_FRONT_LEFT | CHANNEL_OUT_FRONT_RIGHT | CHANNEL_OUT_FRONT_CENTER | CHANNEL_OUT_LOW_FREQUENCY | CHANNEL_OUT_BACK_LEFT | CHANNEL_OUT_BACK_RIGHT | CHANNEL_OUT_FRONT_LEFT_OF_CENTER | CHANNEL_OUT_FRONT_RIGHT_OF_CENTER);
	public static final int CHANNEL_OUT_7POINT1_SURROUND = ( CHANNEL_OUT_FRONT_LEFT | CHANNEL_OUT_FRONT_CENTER | CHANNEL_OUT_FRONT_RIGHT | CHANNEL_OUT_SIDE_LEFT | CHANNEL_OUT_SIDE_RIGHT | CHANNEL_OUT_BACK_LEFT | CHANNEL_OUT_BACK_RIGHT | CHANNEL_OUT_LOW_FREQUENCY);
	public static final int CHANNEL_OUT_5POINT1POINT2 = (CHANNEL_OUT_5POINT1 | CHANNEL_OUT_TOP_SIDE_LEFT | CHANNEL_OUT_TOP_SIDE_RIGHT);
	public static final int CHANNEL_OUT_5POINT1POINT4 = (CHANNEL_OUT_5POINT1 | CHANNEL_OUT_TOP_FRONT_LEFT | CHANNEL_OUT_TOP_FRONT_RIGHT | CHANNEL_OUT_TOP_BACK_LEFT | CHANNEL_OUT_TOP_BACK_RIGHT);
	public static final int CHANNEL_OUT_7POINT1POINT2 = (CHANNEL_OUT_7POINT1_SURROUND | CHANNEL_OUT_TOP_SIDE_LEFT | CHANNEL_OUT_TOP_SIDE_RIGHT);
	public static final int CHANNEL_OUT_7POINT1POINT4 = (CHANNEL_OUT_7POINT1_SURROUND | CHANNEL_OUT_TOP_FRONT_LEFT | CHANNEL_OUT_TOP_FRONT_RIGHT | CHANNEL_OUT_TOP_BACK_LEFT | CHANNEL_OUT_TOP_BACK_RIGHT);
	public static final int CHANNEL_OUT_9POINT1POINT4 = (CHANNEL_OUT_7POINT1POINT4 | CHANNEL_OUT_FRONT_WIDE_LEFT | CHANNEL_OUT_FRONT_WIDE_RIGHT);
	public static final int CHANNEL_OUT_9POINT1POINT6 = (CHANNEL_OUT_9POINT1POINT4 | CHANNEL_OUT_TOP_SIDE_LEFT | CHANNEL_OUT_TOP_SIDE_RIGHT);
	public static final int CHANNEL_OUT_13POINT0 = ( CHANNEL_OUT_FRONT_LEFT | CHANNEL_OUT_FRONT_CENTER | CHANNEL_OUT_FRONT_RIGHT | CHANNEL_OUT_SIDE_LEFT | CHANNEL_OUT_SIDE_RIGHT | CHANNEL_OUT_TOP_FRONT_LEFT | CHANNEL_OUT_TOP_FRONT_CENTER | CHANNEL_OUT_TOP_FRONT_RIGHT | CHANNEL_OUT_TOP_BACK_LEFT | CHANNEL_OUT_TOP_BACK_RIGHT | CHANNEL_OUT_BOTTOM_FRONT_LEFT | CHANNEL_OUT_BOTTOM_FRONT_CENTER | CHANNEL_OUT_BOTTOM_FRONT_RIGHT);
	public static final int SAMPLE_RATE_UNSPECIFIED = 0;
	public static final int CHANNEL_IN_DEFAULT = 1;
	public static final int CHANNEL_IN_LEFT = 0x4;
	public static final int CHANNEL_IN_RIGHT = 0x8;
	public static final int CHANNEL_IN_FRONT = 0x10;
	public static final int CHANNEL_IN_BACK = 0x20;
	public static final int CHANNEL_IN_LEFT_PROCESSED = 0x40;
	public static final int CHANNEL_IN_RIGHT_PROCESSED = 0x80;
	public static final int CHANNEL_IN_FRONT_PROCESSED = 0x100;
	public static final int CHANNEL_IN_BACK_PROCESSED = 0x200;
	public static final int CHANNEL_IN_PRESSURE = 0x400;
	public static final int CHANNEL_IN_X_AXIS = 0x800;
	public static final int CHANNEL_IN_Y_AXIS = 0x1000;
	public static final int CHANNEL_IN_Z_AXIS = 0x2000;
	public static final int CHANNEL_IN_VOICE_UPLINK = 0x4000;
	public static final int CHANNEL_IN_VOICE_DNLINK = 0x8000;
	public static final int CHANNEL_IN_MONO = CHANNEL_IN_FRONT;
	public static final int CHANNEL_IN_STEREO = (CHANNEL_IN_LEFT | CHANNEL_IN_RIGHT);

	/** Invalid audio channel mask */
	public static final int CHANNEL_INVALID = 0;

	/* AOSP AudioFormat encodings (android-14) */
	public static final int ENCODING_INVALID = 0;
	public static final int ENCODING_DEFAULT = 1;
	public static final int ENCODING_PCM_16BIT = 2;
	public static final int ENCODING_PCM_8BIT = 3;
	public static final int ENCODING_PCM_FLOAT = 4;
	public static final int ENCODING_AC3 = 5;
	public static final int ENCODING_E_AC3 = 6;
	public static final int ENCODING_DTS = 7;
	public static final int ENCODING_DTS_HD = 8;
	public static final int ENCODING_MP3 = 9;
	public static final int ENCODING_AAC_LC = 10;
	public static final int ENCODING_AAC_HE_V1 = 11;
	public static final int ENCODING_AAC_HE_V2 = 12;
	public static final int ENCODING_IEC61937 = 13;
	public static final int ENCODING_DOLBY_TRUEHD = 14;
	public static final int ENCODING_AAC_ELD = 15;
	public static final int ENCODING_AAC_XHE = 16;
	public static final int ENCODING_AC4 = 17;
	public static final int ENCODING_E_AC3_JOC = 18;
	public static final int ENCODING_DOLBY_MAT = 19;
	public static final int ENCODING_OPUS = 20;
	public static final int ENCODING_LEGACY_SHORT_ARRAY_THRESHOLD = ENCODING_OPUS;
	public static final int ENCODING_PCM_24BIT_PACKED = 21;
	public static final int ENCODING_PCM_32BIT = 22;
	public static final int ENCODING_MPEGH_BL_L3 = 23;
	public static final int ENCODING_MPEGH_BL_L4 = 24;
	public static final int ENCODING_MPEGH_LC_L3 = 25;
	public static final int ENCODING_MPEGH_LC_L4 = 26;
	@Deprecated public static final int ENCODING_DTS_UHD = 27;
	public static final int ENCODING_DRA = 28;
	public static final int ENCODING_DTS_HD_MA = 29;
	public static final int ENCODING_DTS_UHD_P1 = 27;
	public static final int ENCODING_DTS_UHD_P2 = 30;
	public static final int ENCODING_DSD = 31;

	int sampleRate = 44100;
	int channelMask;
	int encoding;
	int channelIndexMask;
	int propertySetMask; // AUDIO_FORMAT_HAS_PROPERTY_*, as set by the Builder

	/* AOSP AudioFormat getters; ATL's Builder sets no channel index mask */
	public int getEncoding() {
		return encoding;
	}

	public int getSampleRate() {
		return sampleRate;
	}

	public int getChannelMask() {
		return channelMask;
	}

	public int getChannelIndexMask() {
		return channelIndexMask;
	}

	/* AOSP AudioFormat.getFrameSizeInBytes(): sample size times channel count for PCM, else (also for an
	 * invalid encoding or a channel count of 0) 1 */
	public int getFrameSizeInBytes() {
		int frameSizeInBytes = 1;
		switch (encoding) { // AudioFormat.getBytesPerSample(); other encodings throw there and give 1
			case ENCODING_PCM_8BIT:
				frameSizeInBytes = getChannelCount();
				break;
			case ENCODING_PCM_16BIT:
			case ENCODING_IEC61937:
			case ENCODING_DEFAULT:
				frameSizeInBytes = 2 * getChannelCount();
				break;
			case ENCODING_PCM_24BIT_PACKED:
				frameSizeInBytes = 3 * getChannelCount();
				break;
			case ENCODING_PCM_FLOAT:
			case ENCODING_PCM_32BIT:
				frameSizeInBytes = 4 * getChannelCount();
				break;
			default:
				break;
		}
		return frameSizeInBytes != 0 ? frameSizeInBytes : 1;
	}

	/* AOSP AudioFormat.describeContents(), writeToParcel(), AudioFormat(Parcel) and CREATOR */
	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(android.os.Parcel dest, int flags) {
		dest.writeInt(propertySetMask);
		dest.writeInt(encoding);
		dest.writeInt(sampleRate);
		dest.writeInt(channelMask);
		dest.writeInt(channelIndexMask);
	}

	private AudioFormat(android.os.Parcel in) {
		propertySetMask = in.readInt();
		encoding = in.readInt();
		sampleRate = in.readInt();
		channelMask = in.readInt();
		channelIndexMask = in.readInt();
	}

	public AudioFormat() {}

	public static final android.os.Parcelable.Creator<AudioFormat> CREATOR = new android.os.Parcelable.Creator<AudioFormat>() {
		public AudioFormat createFromParcel(android.os.Parcel p) {
			return new AudioFormat(p);
		}

		public AudioFormat[] newArray(int size) {
			return new AudioFormat[size];
		}
	};

	/* AOSP: computed in the constructor from the position and index masks */
	public int getChannelCount() {
		final int channelIndexCount = Integer.bitCount(getChannelIndexMask());
		int channelCount = channelCountFromOutChannelMask(getChannelMask());
		if (channelCount == 0) {
			channelCount = channelIndexCount;
		} else if (channelCount != channelIndexCount && channelIndexCount != 0) {
			channelCount = 0; // position and index channel count mismatch
		}
		return channelCount;
	}

	/* AOSP AudioFormat.SAMPLE_RATE_HZ_MIN/MAX: AudioSystem's, SAMPLE_RATE_HZ_MIN/MAX of system/audio.h */
	public static final int SAMPLE_RATE_HZ_MIN = 4000;
	public static final int SAMPLE_RATE_HZ_MAX = 192000;

	/** @hide AOSP AudioFormat.isPublicEncoding() */
	public static boolean isPublicEncoding(int audioFormat) {
		switch (audioFormat) {
			case ENCODING_PCM_16BIT:
			case ENCODING_PCM_8BIT:
			case ENCODING_PCM_FLOAT:
			case ENCODING_AC3:
			case ENCODING_E_AC3:
			case ENCODING_DTS:
			case ENCODING_DTS_HD:
			case ENCODING_MP3:
			case ENCODING_AAC_LC:
			case ENCODING_AAC_HE_V1:
			case ENCODING_AAC_HE_V2:
			case ENCODING_IEC61937:
			case ENCODING_DOLBY_TRUEHD:
			case ENCODING_AAC_ELD:
			case ENCODING_AAC_XHE:
			case ENCODING_AC4:
			case ENCODING_AC4_L4:
			case ENCODING_E_AC3_JOC:
			case ENCODING_DOLBY_MAT:
			case ENCODING_OPUS:
			case ENCODING_PCM_24BIT_PACKED:
			case ENCODING_PCM_32BIT:
			case ENCODING_MPEGH_BL_L3:
			case ENCODING_MPEGH_BL_L4:
			case ENCODING_MPEGH_LC_L3:
			case ENCODING_MPEGH_LC_L4:
			case ENCODING_DTS_UHD_P1:
			case ENCODING_DRA:
			case ENCODING_DTS_HD_MA:
			case ENCODING_DTS_UHD_P2:
			case ENCODING_DSD:
			case ENCODING_IAMF_BASE_ENHANCED_PROFILE_AAC:
			case ENCODING_IAMF_BASE_ENHANCED_PROFILE_FLAC:
			case ENCODING_IAMF_BASE_ENHANCED_PROFILE_OPUS:
			case ENCODING_IAMF_BASE_ENHANCED_PROFILE_PCM:
			case ENCODING_IAMF_BASE_PROFILE_AAC:
			case ENCODING_IAMF_BASE_PROFILE_FLAC:
			case ENCODING_IAMF_BASE_PROFILE_OPUS:
			case ENCODING_IAMF_BASE_PROFILE_PCM:
			case ENCODING_IAMF_SIMPLE_PROFILE_AAC:
			case ENCODING_IAMF_SIMPLE_PROFILE_FLAC:
			case ENCODING_IAMF_SIMPLE_PROFILE_OPUS:
			case ENCODING_IAMF_SIMPLE_PROFILE_PCM:
				return true;
			default:
				return false;
		}
	}

	/** @hide AOSP AudioFormat.isEncodingLinearFrames() */
	public static boolean isEncodingLinearFrames(int audioFormat) {
		switch (audioFormat) {
			case ENCODING_PCM_16BIT:
			case ENCODING_PCM_8BIT:
			case ENCODING_PCM_FLOAT:
			case ENCODING_IEC61937: // same size as stereo PCM
			case ENCODING_PCM_24BIT_PACKED:
			case ENCODING_PCM_32BIT:
			case ENCODING_DEFAULT:
				return true;
			case ENCODING_AC3:
			case ENCODING_E_AC3:
			case ENCODING_DTS:
			case ENCODING_DTS_HD:
			case ENCODING_MP3:
			case ENCODING_AAC_LC:
			case ENCODING_AAC_HE_V1:
			case ENCODING_AAC_HE_V2:
			case ENCODING_DOLBY_TRUEHD:
			case ENCODING_AAC_ELD:
			case ENCODING_AAC_XHE:
			case ENCODING_AC4:
			case ENCODING_AC4_L4:
			case ENCODING_E_AC3_JOC:
			case ENCODING_DOLBY_MAT:
			case ENCODING_OPUS:
			case ENCODING_MPEGH_BL_L3:
			case ENCODING_MPEGH_BL_L4:
			case ENCODING_MPEGH_LC_L3:
			case ENCODING_MPEGH_LC_L4:
			case ENCODING_DTS_UHD_P1:
			case ENCODING_DRA:
			case ENCODING_DTS_HD_MA:
			case ENCODING_DTS_UHD_P2:
			case ENCODING_IAMF_BASE_ENHANCED_PROFILE_AAC:
			case ENCODING_IAMF_BASE_ENHANCED_PROFILE_FLAC:
			case ENCODING_IAMF_BASE_ENHANCED_PROFILE_OPUS:
			case ENCODING_IAMF_BASE_ENHANCED_PROFILE_PCM:
			case ENCODING_IAMF_BASE_PROFILE_AAC:
			case ENCODING_IAMF_BASE_PROFILE_FLAC:
			case ENCODING_IAMF_BASE_PROFILE_OPUS:
			case ENCODING_IAMF_BASE_PROFILE_PCM:
			case ENCODING_IAMF_SIMPLE_PROFILE_AAC:
			case ENCODING_IAMF_SIMPLE_PROFILE_FLAC:
			case ENCODING_IAMF_SIMPLE_PROFILE_OPUS:
			case ENCODING_IAMF_SIMPLE_PROFILE_PCM:
				return false;
			case ENCODING_INVALID:
			default:
				throw new IllegalArgumentException("Bad audio format " + audioFormat);
		}
	}

	/** @hide */
	public static int channelCountFromInChannelMask(int mask) {
		return Integer.bitCount(mask);
	}

	/** @hide */
	public static int channelCountFromOutChannelMask(int mask) {
		return Integer.bitCount(mask);
	}

	/** @hide */
public static String toLogFriendlyEncoding(int enc) {
	switch(enc) {
		case ENCODING_INVALID:
			return "ENCODING_INVALID";
		case ENCODING_PCM_16BIT:
			return "ENCODING_PCM_16BIT";
		case ENCODING_PCM_8BIT:
			return "ENCODING_PCM_8BIT";
		case ENCODING_PCM_FLOAT:
			return "ENCODING_PCM_FLOAT";
		case ENCODING_AC3:
			return "ENCODING_AC3";
		case ENCODING_E_AC3:
			return "ENCODING_E_AC3";
		case ENCODING_DTS:
			return "ENCODING_DTS";
		case ENCODING_DTS_HD:
			return "ENCODING_DTS_HD";
		case ENCODING_MP3:
			return "ENCODING_MP3";
		case ENCODING_AAC_LC:
			return "ENCODING_AAC_LC";
		case ENCODING_AAC_HE_V1:
			return "ENCODING_AAC_HE_V1";
		case ENCODING_AAC_HE_V2:
			return "ENCODING_AAC_HE_V2";
		case ENCODING_IEC61937:
			return "ENCODING_IEC61937";
		case ENCODING_DOLBY_TRUEHD:
			return "ENCODING_DOLBY_TRUEHD";
		case ENCODING_AAC_ELD:
			return "ENCODING_AAC_ELD";
		case ENCODING_AAC_XHE:
			return "ENCODING_AAC_XHE";
		case ENCODING_AC4:
			return "ENCODING_AC4";
		case ENCODING_E_AC3_JOC:
			return "ENCODING_E_AC3_JOC";
		case ENCODING_DOLBY_MAT:
			return "ENCODING_DOLBY_MAT";
		case ENCODING_OPUS:
			return "ENCODING_OPUS";
		case ENCODING_PCM_24BIT_PACKED:
			return "ENCODING_PCM_24BIT_PACKED";
		case ENCODING_PCM_32BIT:
			return "ENCODING_PCM_32BIT";
		case ENCODING_MPEGH_BL_L3:
			return "ENCODING_MPEGH_BL_L3";
		case ENCODING_MPEGH_BL_L4:
			return "ENCODING_MPEGH_BL_L4";
		case ENCODING_MPEGH_LC_L3:
			return "ENCODING_MPEGH_LC_L3";
		case ENCODING_MPEGH_LC_L4:
			return "ENCODING_MPEGH_LC_L4";
		case ENCODING_DTS_UHD_P1:
			return "ENCODING_DTS_UHD_P1";
		case ENCODING_DRA:
			return "ENCODING_DRA";
		case ENCODING_DTS_HD_MA:
			return "ENCODING_DTS_HD_MA";
		case ENCODING_DTS_UHD_P2:
			return "ENCODING_DTS_UHD_P2";
		case ENCODING_DSD:
			return "ENCODING_DSD";
		default :
			return "invalid encoding " + enc;
	}
}

	public static class Builder {

		private AudioFormat audioFormat = new AudioFormat();

		public Builder() {
		}

		/* AOSP AudioFormat.Builder(AudioFormat af): reuse the data of af */
		public Builder(AudioFormat af) {
			audioFormat.encoding = af.encoding;
			audioFormat.sampleRate = af.sampleRate;
			audioFormat.channelMask = af.channelMask;
			audioFormat.channelIndexMask = af.channelIndexMask;
			audioFormat.propertySetMask = af.propertySetMask;
		}

		public Builder setSampleRate(int sampleRate) {
			audioFormat.sampleRate = sampleRate;
			audioFormat.propertySetMask |= 0x2; // AUDIO_FORMAT_HAS_PROPERTY_SAMPLE_RATE
			return this;
		}

		public Builder setChannelMask(int channelMask) {
			audioFormat.channelMask = channelMask;
			audioFormat.propertySetMask |= 0x4; // AUDIO_FORMAT_HAS_PROPERTY_CHANNEL_MASK
			return this;
		}

		/* AOSP AudioFormat.Builder.setChannelIndexMask() */
		public Builder setChannelIndexMask(int channelIndexMask) {
			if (channelIndexMask == 0) {
				throw new IllegalArgumentException("Invalid zero channel index mask");
			} else if (audioFormat.channelMask != 0 &&
			           Integer.bitCount(channelIndexMask) != Integer.bitCount(audioFormat.channelMask)) {
				throw new IllegalArgumentException("Mismatched channel count for index mask " +
				                                   Integer.toHexString(channelIndexMask).toUpperCase());
			}
			audioFormat.channelIndexMask = channelIndexMask;
			audioFormat.propertySetMask |= 0x8; // AUDIO_FORMAT_HAS_PROPERTY_CHANNEL_INDEX_MASK
			return this;
		}

		public Builder setEncoding(int encoding) {
			audioFormat.encoding = encoding;
			audioFormat.propertySetMask |= 0x1; // AUDIO_FORMAT_HAS_PROPERTY_ENCODING
			return this;
		}

		public AudioFormat build() {
			return audioFormat;
		}
	}
}
