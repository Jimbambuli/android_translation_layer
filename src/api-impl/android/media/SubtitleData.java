package android.media;

/* AOSP media/java/android/media/SubtitleData.java */
import android.annotation.NonNull;
import android.os.Parcel;

public final class SubtitleData
{
	private static final String TAG = "SubtitleData";

	private int mTrackIndex;
	private long mStartTimeUs;
	private long mDurationUs;
	private byte[] mData;

	public SubtitleData(Parcel parcel) {
		if (!parseParcel(parcel)) {
			throw new IllegalArgumentException("parseParcel() fails");
		}
	}

	public SubtitleData(int trackIndex, long startTimeUs, long durationUs, @NonNull byte[] data) {
		if (data == null) {
			throw new IllegalArgumentException("null data is not allowed");
		}
		mTrackIndex = trackIndex;
		mStartTimeUs = startTimeUs;
		mDurationUs = durationUs;
		mData = data;
	}

	public int getTrackIndex() {
		return mTrackIndex;
	}

	public long getStartTimeUs() {
		return mStartTimeUs;
	}

	public long getDurationUs() {
		return mDurationUs;
	}

	public @NonNull byte[] getData() {
		return mData;
	}

	private boolean parseParcel(Parcel parcel) {
		parcel.setDataPosition(0);
		if (parcel.dataAvail() == 0) {
			return false;
		}

		mTrackIndex = parcel.readInt();
		mStartTimeUs = parcel.readLong();
		mDurationUs = parcel.readLong();
		mData = new byte[parcel.readInt()];
		parcel.readByteArray(mData);

		return true;
	}
}
