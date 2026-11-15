package android.media.metrics;


import android.hardware.DataSpace;
import android.media.MediaCodec;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Size;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/* AOSP media/java/android/media/metrics/MediaItemInfo.java (android-16) */
public final class MediaItemInfo implements Parcelable {
	public static final int SOURCE_TYPE_UNSPECIFIED = 0;

	public static final int SOURCE_TYPE_GALLERY = 1;

	public static final int SOURCE_TYPE_CAMERA = 2;

	public static final int SOURCE_TYPE_EDITING_SESSION = 3;

	public static final int SOURCE_TYPE_LOCAL_FILE = 4;

	public static final int SOURCE_TYPE_REMOTE_FILE = 5;

	public static final int SOURCE_TYPE_REMOTE_LIVE_STREAM = 6;

	public static final int SOURCE_TYPE_GENERATED = 7;
	public static final long DATA_TYPE_IMAGE = 1L;

	public static final long DATA_TYPE_VIDEO = 1L << 1;

	public static final long DATA_TYPE_AUDIO = 1L << 2;

	public static final long DATA_TYPE_METADATA = 1L << 3;

	public static final long DATA_TYPE_DEPTH = 1L << 4;

	public static final long DATA_TYPE_GAIN_MAP = 1L << 5;

	public static final long DATA_TYPE_HIGH_FRAME_RATE = 1L << 6;

	public static final long DATA_TYPE_SPEED_SETTING_CUE_POINTS = 1L << 7;

	public static final long DATA_TYPE_GAPLESS = 1L << 8;

	public static final long DATA_TYPE_SPATIAL_AUDIO = 1L << 9;

	public static final long DATA_TYPE_HIGH_DYNAMIC_RANGE_VIDEO = 1L << 10;

	public static final int VALUE_UNSPECIFIED = -1;

	private final int mSourceType;
	private final long mDataTypes;
	private final long mDurationMillis;
	private final long mClipDurationMillis;
	private final String mContainerMimeType;
	private final List<String> mSampleMimeTypes;
	private final List<String> mCodecNames;
	private final int mAudioSampleRateHz;
	private final int mAudioChannelCount;
	private final long mAudioSampleCount;
	private final Size mVideoSize;
	private final int mVideoDataSpace;
	private final float mVideoFrameRate;
	private final long mVideoSampleCount;

	private MediaItemInfo(
			int sourceType,
			long dataTypes,
			long durationMillis,
			long clipDurationMillis,
			String containerMimeType,
			List<String> sampleMimeTypes,
			List<String> codecNames,
			int audioSampleRateHz,
			int audioChannelCount,
			long audioSampleCount,
			Size videoSize,
			int videoDataSpace,
			float videoFrameRate,
			long videoSampleCount) {
		mSourceType = sourceType;
		mDataTypes = dataTypes;
		mDurationMillis = durationMillis;
		mClipDurationMillis = clipDurationMillis;
		mContainerMimeType = containerMimeType;
		mSampleMimeTypes = sampleMimeTypes;
		mCodecNames = codecNames;
		mAudioSampleRateHz = audioSampleRateHz;
		mAudioChannelCount = audioChannelCount;
		mAudioSampleCount = audioSampleCount;
		mVideoSize = videoSize;
		mVideoDataSpace = videoDataSpace;
		mVideoFrameRate = videoFrameRate;
		mVideoSampleCount = videoSampleCount;
	}

	public int getSourceType() {
		return mSourceType;
	}

	public long getDataTypes() {
		return mDataTypes;
	}

	public long getDurationMillis() {
		return mDurationMillis;
	}

	public long getClipDurationMillis() {
		return mClipDurationMillis;
	}

	public String getContainerMimeType() {
		return mContainerMimeType;
	}

	public List<String> getSampleMimeTypes() {
		return new ArrayList<>(mSampleMimeTypes);
	}

	public List<String> getCodecNames() {
		return new ArrayList<>(mCodecNames);
	}

	public int getAudioSampleRateHz() {
		return mAudioSampleRateHz;
	}

	public int getAudioChannelCount() {
		return mAudioChannelCount;
	}

	public long getAudioSampleCount() {
		return mAudioSampleCount;
	}

	public Size getVideoSize() {
		return mVideoSize;
	}

	// Packed integer for an android.hardware.DataSpace.
	public int getVideoDataSpace() {
		return mVideoDataSpace;
	}

	public float getVideoFrameRate() {
		return mVideoFrameRate;
	}

	public long getVideoSampleCount() {
		return mVideoSampleCount;
	}

	public static final class Builder {

		private int mSourceType;
		private long mDataTypes;
		private long mDurationMillis;
		private long mClipDurationMillis;
		private String mContainerMimeType;
		private final ArrayList<String> mSampleMimeTypes;
		private final ArrayList<String> mCodecNames;
		private int mAudioSampleRateHz;
		private int mAudioChannelCount;
		private long mAudioSampleCount;
		private Size mVideoSize;
		private int mVideoDataSpace;
		private float mVideoFrameRate;
		private long mVideoSampleCount;

		public Builder() {
			mSourceType = SOURCE_TYPE_UNSPECIFIED;
			mDurationMillis = VALUE_UNSPECIFIED;
			mClipDurationMillis = VALUE_UNSPECIFIED;
			mSampleMimeTypes = new ArrayList<>();
			mCodecNames = new ArrayList<>();
			mAudioSampleRateHz = VALUE_UNSPECIFIED;
			mAudioChannelCount = VALUE_UNSPECIFIED;
			mAudioSampleCount = VALUE_UNSPECIFIED;
			mVideoSize = new Size(VALUE_UNSPECIFIED, VALUE_UNSPECIFIED);
			mVideoFrameRate = VALUE_UNSPECIFIED;
			mVideoSampleCount = VALUE_UNSPECIFIED;
		}

		public Builder setSourceType(int sourceType) {
			mSourceType = sourceType;
			return this;
		}

		public Builder addDataType(long dataType) {
			mDataTypes |= dataType;
			return this;
		}

		public Builder setDurationMillis(long durationMillis) {
			mDurationMillis = durationMillis;
			return this;
		}

		public Builder setClipDurationMillis(long clipDurationMillis) {
			mClipDurationMillis = clipDurationMillis;
			return this;
		}

		public Builder setContainerMimeType(String containerMimeType) {
			mContainerMimeType = Objects.requireNonNull(containerMimeType);
			return this;
		}

		public Builder addSampleMimeType(String mimeType) {
			mSampleMimeTypes.add(Objects.requireNonNull(mimeType));
			return this;
		}

		public Builder addCodecName(String codecName) {
			mCodecNames.add(Objects.requireNonNull(codecName));
			return this;
		}

		public Builder setAudioSampleRateHz(int audioSampleRateHz) {
			mAudioSampleRateHz = audioSampleRateHz;
			return this;
		}

		public Builder setAudioChannelCount(int audioChannelCount) {
			mAudioChannelCount = audioChannelCount;
			return this;
		}

		public Builder setAudioSampleCount(long audioSampleCount) {
			mAudioSampleCount = audioSampleCount;
			return this;
		}

		public Builder setVideoSize(Size videoSize) {
			mVideoSize = Objects.requireNonNull(videoSize);
			return this;
		}

		public Builder setVideoDataSpace(int videoDataSpace) {
			mVideoDataSpace = videoDataSpace;
			return this;
		}

		public Builder setVideoFrameRate(float videoFrameRate) {
			mVideoFrameRate = videoFrameRate;
			return this;
		}

		public Builder setVideoSampleCount(long videoSampleCount) {
			mVideoSampleCount = videoSampleCount;
			return this;
		}

		public MediaItemInfo build() {
			return new MediaItemInfo(
					mSourceType,
					mDataTypes,
					mDurationMillis,
					mClipDurationMillis,
					mContainerMimeType,
					mSampleMimeTypes,
					mCodecNames,
					mAudioSampleRateHz,
					mAudioChannelCount,
					mAudioSampleCount,
					mVideoSize,
					mVideoDataSpace,
					mVideoFrameRate,
					mVideoSampleCount);
		}
	}

	@Override
	public String toString() {
		return "MediaItemInfo { "
				+ "sourceType = "
				+ mSourceType
				+ ", "
				+ "dataTypes = "
				+ mDataTypes
				+ ", "
				+ "durationMillis = "
				+ mDurationMillis
				+ ", "
				+ "clipDurationMillis = "
				+ mClipDurationMillis
				+ ", "
				+ "containerMimeType = "
				+ mContainerMimeType
				+ ", "
				+ "sampleMimeTypes = "
				+ mSampleMimeTypes
				+ ", "
				+ "codecNames = "
				+ mCodecNames
				+ ", "
				+ "audioSampleRateHz = "
				+ mAudioSampleRateHz
				+ ", "
				+ "audioChannelCount = "
				+ mAudioChannelCount
				+ ", "
				+ "audioSampleCount = "
				+ mAudioSampleCount
				+ ", "
				+ "videoSize = "
				+ mVideoSize
				+ ", "
				+ "videoDataSpace = "
				+ mVideoDataSpace
				+ ", "
				+ "videoFrameRate = "
				+ mVideoFrameRate
				+ ", "
				+ "videoSampleCount = "
				+ mVideoSampleCount
				+ " }";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		MediaItemInfo that = (MediaItemInfo) o;
		return mSourceType == that.mSourceType
				&& mDataTypes == that.mDataTypes
				&& mDurationMillis == that.mDurationMillis
				&& mClipDurationMillis == that.mClipDurationMillis
				&& Objects.equals(mContainerMimeType, that.mContainerMimeType)
				&& mSampleMimeTypes.equals(that.mSampleMimeTypes)
				&& mCodecNames.equals(that.mCodecNames)
				&& mAudioSampleRateHz == that.mAudioSampleRateHz
				&& mAudioChannelCount == that.mAudioChannelCount
				&& mAudioSampleCount == that.mAudioSampleCount
				&& Objects.equals(mVideoSize, that.mVideoSize)
				&& Objects.equals(mVideoDataSpace, that.mVideoDataSpace)
				&& mVideoFrameRate == that.mVideoFrameRate
				&& mVideoSampleCount == that.mVideoSampleCount;
	}

	@Override
	public int hashCode() {
		return Objects.hash(mSourceType, mDataTypes);
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(mSourceType);
		dest.writeLong(mDataTypes);
		dest.writeLong(mDurationMillis);
		dest.writeLong(mClipDurationMillis);
		dest.writeString(mContainerMimeType);
		dest.writeStringList(mSampleMimeTypes);
		dest.writeStringList(mCodecNames);
		dest.writeInt(mAudioSampleRateHz);
		dest.writeInt(mAudioChannelCount);
		dest.writeLong(mAudioSampleCount);
		dest.writeInt(mVideoSize.getWidth());
		dest.writeInt(mVideoSize.getHeight());
		dest.writeInt(mVideoDataSpace);
		dest.writeFloat(mVideoFrameRate);
		dest.writeLong(mVideoSampleCount);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	private MediaItemInfo(Parcel in) {
		mSourceType = in.readInt();
		mDataTypes = in.readLong();
		mDurationMillis = in.readLong();
		mClipDurationMillis = in.readLong();
		mContainerMimeType = in.readString();
		mSampleMimeTypes = new ArrayList<>();
		in.readStringList(mSampleMimeTypes);
		mCodecNames = new ArrayList<>();
		in.readStringList(mCodecNames);
		mAudioSampleRateHz = in.readInt();
		mAudioChannelCount = in.readInt();
		mAudioSampleCount = in.readLong();
		int videoSizeWidth = in.readInt();
		int videoSizeHeight = in.readInt();
		mVideoSize = new Size(videoSizeWidth, videoSizeHeight);
		mVideoDataSpace = in.readInt();
		mVideoFrameRate = in.readFloat();
		mVideoSampleCount = in.readLong();
	}

	public static final Creator<MediaItemInfo> CREATOR =
			new Creator<>() {
				@Override
				public MediaItemInfo[] newArray(int size) {
					return new MediaItemInfo[size];
				}

				@Override
				public MediaItemInfo createFromParcel(Parcel in) {
					return new MediaItemInfo(in);
				}
			};
}
