package android.media.metrics;


import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/* AOSP media/java/android/media/metrics/EditingEndedEvent.java (android-16) */
public final class EditingEndedEvent extends Event implements Parcelable {

	public static final int FINAL_STATE_SUCCEEDED = 1;

	public static final int FINAL_STATE_CANCELED = 2;

	public static final int FINAL_STATE_ERROR = 3;
	private final int mFinalState;

	private final float mFinalProgressPercent;

	public static final int ERROR_CODE_NONE = 1;

	public static final int ERROR_CODE_FAILED_RUNTIME_CHECK = 2;

	public static final int ERROR_CODE_IO_UNSPECIFIED = 3;

	public static final int ERROR_CODE_IO_NETWORK_CONNECTION_FAILED = 4;

	public static final int ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT = 5;

	public static final int ERROR_CODE_IO_BAD_HTTP_STATUS = 6;

	public static final int ERROR_CODE_IO_FILE_NOT_FOUND = 7;

	public static final int ERROR_CODE_IO_NO_PERMISSION = 8;

	public static final int ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED = 9;

	public static final int ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE = 10;

	public static final int ERROR_CODE_DECODER_INIT_FAILED = 11;

	public static final int ERROR_CODE_DECODING_FAILED = 12;

	public static final int ERROR_CODE_DECODING_FORMAT_UNSUPPORTED = 13;

	public static final int ERROR_CODE_ENCODER_INIT_FAILED = 14;

	public static final int ERROR_CODE_ENCODING_FAILED = 15;

	public static final int ERROR_CODE_ENCODING_FORMAT_UNSUPPORTED = 16;

	public static final int ERROR_CODE_VIDEO_FRAME_PROCESSING_FAILED = 17;

	public static final int ERROR_CODE_AUDIO_PROCESSING_FAILED = 18;

	public static final int ERROR_CODE_MUXING_FAILED = 19;
	public static final int TIME_SINCE_CREATED_UNKNOWN = -1;

	public static final int PROGRESS_PERCENT_UNKNOWN = -1;

	private final int mErrorCode;
	@SuppressWarnings("HidingField") // Hiding field from superclass as for playback events.
	private final long mTimeSinceCreatedMillis;

	private final String mExporterName;
	private final String mMuxerName;
	private final ArrayList<MediaItemInfo> mInputMediaItemInfos;
	private final MediaItemInfo mOutputMediaItemInfo;
	public static final long OPERATION_TYPE_VIDEO_TRANSCODE = 1;

	public static final long OPERATION_TYPE_AUDIO_TRANSCODE = 1L << 1;

	public static final long OPERATION_TYPE_VIDEO_EDIT = 1L << 2;

	public static final long OPERATION_TYPE_AUDIO_EDIT = 1L << 3;

	public static final long OPERATION_TYPE_VIDEO_TRANSMUX = 1L << 4;

	public static final long OPERATION_TYPE_AUDIO_TRANSMUX = 1L << 5;

	public static final long OPERATION_TYPE_PAUSED = 1L << 6;

	public static final long OPERATION_TYPE_RESUMED = 1L << 7;

	private final long mOperationTypes;

	private EditingEndedEvent(
			int finalState,
			float finalProgressPercent,
			int errorCode,
			long timeSinceCreatedMillis,
			String exporterName,
			String muxerName,
			ArrayList<MediaItemInfo> inputMediaItemInfos,
			MediaItemInfo outputMediaItemInfo,
			long operationTypes,
			Bundle extras) {
		mFinalState = finalState;
		mFinalProgressPercent = finalProgressPercent;
		mErrorCode = errorCode;
		mTimeSinceCreatedMillis = timeSinceCreatedMillis;
		mExporterName = exporterName;
		mMuxerName = muxerName;
		mInputMediaItemInfos = inputMediaItemInfos;
		mOutputMediaItemInfo = outputMediaItemInfo;
		mOperationTypes = operationTypes;
		mMetricsBundle = new Bundle(extras); /* ATL Bundle has no deepCopy() */
	}

	public int getFinalState() {
		return mFinalState;
	}

	public float getFinalProgressPercent() {
		return mFinalProgressPercent;
	}

	public int getErrorCode() {
		return mErrorCode;
	}

	@Override
	public long getTimeSinceCreatedMillis() {
		return mTimeSinceCreatedMillis;
	}

	public String getExporterName() {
		return mExporterName;
	}

	public String getMuxerName() {
		return mMuxerName;
	}

	public List<MediaItemInfo> getInputMediaItemInfos() {
		return new ArrayList<>(mInputMediaItemInfos);
	}

	public MediaItemInfo getOutputMediaItemInfo() {
		return mOutputMediaItemInfo;
	}

	public long getOperationTypes() {
		return mOperationTypes;
	}

	@Override
	public Bundle getMetricsBundle() {
		return mMetricsBundle;
	}

	@Override
	public String toString() {
		return "EditingEndedEvent { "
				+ "finalState = "
				+ mFinalState
				+ ", "
				+ "finalProgressPercent = "
				+ mFinalProgressPercent
				+ ", "
				+ "errorCode = "
				+ mErrorCode
				+ ", "
				+ "timeSinceCreatedMillis = "
				+ mTimeSinceCreatedMillis
				+ ", "
				+ "exporterName = "
				+ mExporterName
				+ ", "
				+ "muxerName = "
				+ mMuxerName
				+ ", "
				+ "inputMediaItemInfos = "
				+ mInputMediaItemInfos
				+ ", "
				+ "outputMediaItemInfo = "
				+ mOutputMediaItemInfo
				+ ", "
				+ "operationTypes = "
				+ mOperationTypes
				+ " }";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		EditingEndedEvent that = (EditingEndedEvent) o;
		return mFinalState == that.mFinalState
				&& mFinalProgressPercent == that.mFinalProgressPercent
				&& mErrorCode == that.mErrorCode
				&& Objects.equals(mInputMediaItemInfos, that.mInputMediaItemInfos)
				&& Objects.equals(mOutputMediaItemInfo, that.mOutputMediaItemInfo)
				&& mOperationTypes == that.mOperationTypes
				&& mTimeSinceCreatedMillis == that.mTimeSinceCreatedMillis
				&& Objects.equals(mExporterName, that.mExporterName)
				&& Objects.equals(mMuxerName, that.mMuxerName);
	}

	@Override
	public int hashCode() {
		return Objects.hash(
				mFinalState,
				mFinalProgressPercent,
				mErrorCode,
				mInputMediaItemInfos,
				mOutputMediaItemInfo,
				mOperationTypes,
				mTimeSinceCreatedMillis,
				mExporterName,
				mMuxerName);
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(mFinalState);
		dest.writeFloat(mFinalProgressPercent);
		dest.writeInt(mErrorCode);
		dest.writeLong(mTimeSinceCreatedMillis);
		dest.writeString(mExporterName);
		dest.writeString(mMuxerName);
		dest.writeTypedList(mInputMediaItemInfos);
		dest.writeTypedObject(mOutputMediaItemInfo,  0);
		dest.writeLong(mOperationTypes);
		dest.writeBundle(mMetricsBundle);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	private EditingEndedEvent(Parcel in) {
		mFinalState = in.readInt();
		mFinalProgressPercent = in.readFloat();
		mErrorCode = in.readInt();
		mTimeSinceCreatedMillis = in.readLong();
		mExporterName = in.readString();
		mMuxerName = in.readString();
		mInputMediaItemInfos = new ArrayList<>();
		in.readTypedList(mInputMediaItemInfos, MediaItemInfo.CREATOR);
		mOutputMediaItemInfo = in.readTypedObject(MediaItemInfo.CREATOR);
		mOperationTypes = in.readLong();
		mMetricsBundle = in.readBundle();
	}

	public static final Creator<EditingEndedEvent> CREATOR =
			new Creator<>() {
				@Override
				public EditingEndedEvent[] newArray(int size) {
					return new EditingEndedEvent[size];
				}

				@Override
				public EditingEndedEvent createFromParcel(Parcel in) {
					return new EditingEndedEvent(in);
				}
			};

	public static final class Builder {
		private final int mFinalState;
		private final ArrayList<MediaItemInfo> mInputMediaItemInfos;
		private float mFinalProgressPercent;
		private int mErrorCode;
		private long mTimeSinceCreatedMillis;
		private String mExporterName;
		private String mMuxerName;
		private MediaItemInfo mOutputMediaItemInfo;
		private long mOperationTypes;
		private Bundle mMetricsBundle;

		public Builder(int finalState) {
			mFinalState = finalState;
			mFinalProgressPercent = PROGRESS_PERCENT_UNKNOWN;
			mErrorCode = ERROR_CODE_NONE;
			mTimeSinceCreatedMillis = TIME_SINCE_CREATED_UNKNOWN;
			mInputMediaItemInfos = new ArrayList<>();
			mMetricsBundle = new Bundle();
		}

		public Builder setFinalProgressPercent(
				float finalProgressPercent) {
			mFinalProgressPercent = finalProgressPercent;
			return this;
		}

		public Builder setTimeSinceCreatedMillis(
				long timeSinceCreatedMillis) {
			mTimeSinceCreatedMillis = timeSinceCreatedMillis;
			return this;
		}

		public Builder setExporterName(String exporterName) {
			mExporterName = Objects.requireNonNull(exporterName);
			return this;
		}

		public Builder setMuxerName(String muxerName) {
			mMuxerName = Objects.requireNonNull(muxerName);
			return this;
		}

		public Builder setErrorCode(int value) {
			mErrorCode = value;
			return this;
		}

		public Builder addInputMediaItemInfo(MediaItemInfo mediaItemInfo) {
			mInputMediaItemInfos.add(Objects.requireNonNull(mediaItemInfo));
			return this;
		}

		public Builder setOutputMediaItemInfo(MediaItemInfo mediaItemInfo) {
			mOutputMediaItemInfo = Objects.requireNonNull(mediaItemInfo);
			return this;
		}

		public Builder addOperationType(long operationType) {
			mOperationTypes |= operationType;
			return this;
		}

		public Builder setMetricsBundle(Bundle metricsBundle) {
			mMetricsBundle = Objects.requireNonNull(metricsBundle);
			return this;
		}

		public EditingEndedEvent build() {
			return new EditingEndedEvent(
					mFinalState,
					mFinalProgressPercent,
					mErrorCode,
					mTimeSinceCreatedMillis,
					mExporterName,
					mMuxerName,
					mInputMediaItemInfos,
					mOutputMediaItemInfo,
					mOperationTypes,
					mMetricsBundle);
		}
	}

}
