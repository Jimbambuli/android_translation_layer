package android.media;

/* AOSP media/java/android/media/TimedMetaData.java */
import android.annotation.NonNull;
import android.os.Parcel;

public final class TimedMetaData {
	private static final String TAG = "TimedMetaData";

	private long mTimestampUs;
	private byte[] mMetaData;

	static TimedMetaData createTimedMetaDataFromParcel(Parcel parcel) {
		return new TimedMetaData(parcel);
	}

	private TimedMetaData(Parcel parcel) {
		if (!parseParcel(parcel)) {
			throw new IllegalArgumentException("parseParcel() fails");
		}
	}

	public TimedMetaData(long timestampUs, @NonNull byte[] metaData) {
		if (metaData == null) {
			throw new IllegalArgumentException("null metaData is not allowed");
		}
		mTimestampUs = timestampUs;
		mMetaData = metaData;
	}

	public long getTimestamp() {
		return mTimestampUs;
	}

	public byte[] getMetaData() {
		return mMetaData;
	}

	private boolean parseParcel(Parcel parcel) {
		parcel.setDataPosition(0);
		if (parcel.dataAvail() == 0) {
			return false;
		}

		mTimestampUs = parcel.readLong();
		mMetaData = new byte[parcel.readInt()];
		parcel.readByteArray(mMetaData);

		return true;
	}
}
