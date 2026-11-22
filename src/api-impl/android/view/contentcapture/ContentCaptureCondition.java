package android.view.contentcapture;

import android.content.LocusId;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.DebugUtils;
import java.util.Objects;

/* AOSP ContentCaptureCondition: a condition (locus id and flags) for when content capture is allowed */
public final class ContentCaptureCondition implements Parcelable {
	public static final int FLAG_IS_REGEX = 0x2;

	private final LocusId mLocusId;
	private final int mFlags;

	public ContentCaptureCondition(LocusId locusId, int flags) {
		this.mLocusId = Objects.requireNonNull(locusId);
		this.mFlags = flags;
	}

	public LocusId getLocusId() {
		return mLocusId;
	}

	public int getFlags() {
		return mFlags;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + mFlags;
		result = prime * result + ((mLocusId == null) ? 0 : mLocusId.hashCode());
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
		final ContentCaptureCondition other = (ContentCaptureCondition)obj;
		if (mFlags != other.mFlags)
			return false;
		if (mLocusId == null) {
			if (other.mLocusId != null)
				return false;
		} else {
			if (!mLocusId.equals(other.mLocusId))
				return false;
		}
		return true;
	}

	@Override
	public String toString() {
		final StringBuilder string = new StringBuilder(mLocusId.toString());
		if (mFlags != 0) {
			string.append(" (")
			    .append(DebugUtils.flagsToString(ContentCaptureCondition.class, "FLAG_", mFlags))
			    .append(')');
		}
		return string.toString();
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel parcel, int flags) {
		parcel.writeParcelable(mLocusId, flags);
		parcel.writeInt(mFlags);
	}

	public static final Parcelable.Creator<ContentCaptureCondition> CREATOR = new Parcelable.Creator<ContentCaptureCondition>() {
		@Override
		public ContentCaptureCondition createFromParcel(Parcel parcel) {
			return new ContentCaptureCondition(parcel.readParcelable(null, LocusId.class), parcel.readInt());
		}

		@Override
		public ContentCaptureCondition[] newArray(int size) {
			return new ContentCaptureCondition[size];
		}
	};
}
