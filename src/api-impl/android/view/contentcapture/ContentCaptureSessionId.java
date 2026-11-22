package android.view.contentcapture;

import android.os.Parcel;
import android.os.Parcelable;

/* AOSP ContentCaptureSessionId: the id of a ContentCaptureSession */
public final class ContentCaptureSessionId implements Parcelable {
	private final int mValue;

	/** @hide */
	public ContentCaptureSessionId(int value) {
		mValue = value;
	}

	/** @hide */
	public int getValue() {
		return mValue;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + mValue;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final ContentCaptureSessionId other = (ContentCaptureSessionId)obj;
		return mValue == other.mValue;
	}

	@Override
	public String toString() {
		return Integer.toString(mValue);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel parcel, int flags) {
		parcel.writeInt(mValue);
	}

	public static final Parcelable.Creator<ContentCaptureSessionId> CREATOR = new Parcelable.Creator<ContentCaptureSessionId>() {
		@Override
		public ContentCaptureSessionId createFromParcel(Parcel parcel) {
			return new ContentCaptureSessionId(parcel.readInt());
		}

		@Override
		public ContentCaptureSessionId[] newArray(int size) {
			return new ContentCaptureSessionId[size];
		}
	};
}
