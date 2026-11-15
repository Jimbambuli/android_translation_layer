package android.media;

public class AudioFormat implements android.os.Parcelable {

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
