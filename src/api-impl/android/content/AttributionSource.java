package android.content;

import android.os.Parcel;
import android.os.Parcelable;

/* AOSP android.content.AttributionSource: who accesses data (uid, package, attribution tag). ATL has no permission
 * attribution chain, so only the identity of one source is kept; the parcel holds these three values in place of
 * AOSP's AttributionSourceState (ATL parcels stay in the process). */
public final class AttributionSource implements Parcelable {
	private final int mUid;
	private final String mPackageName;
	private final String mAttributionTag;

	AttributionSource(int uid, String packageName, String attributionTag) {
		mUid = uid;
		mPackageName = packageName;
		mAttributionTag = attributionTag;
	}

	public int getUid() {
		return mUid;
	}

	public String getPackageName() {
		return mPackageName;
	}

	public String getAttributionTag() {
		return mAttributionTag;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(mUid);
		dest.writeString(mPackageName);
		dest.writeString(mAttributionTag);
	}

	public static final Parcelable.Creator<AttributionSource> CREATOR = new Parcelable.Creator<AttributionSource>() {
		@Override
		public AttributionSource createFromParcel(Parcel in) {
			return new AttributionSource(in.readInt(), in.readString(), in.readString());
		}

		@Override
		public AttributionSource[] newArray(int size) {
			return new AttributionSource[size];
		}
	};

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof AttributionSource))
			return false;
		AttributionSource that = (AttributionSource)o;
		return mUid == that.mUid && java.util.Objects.equals(mPackageName, that.mPackageName)
		    && java.util.Objects.equals(mAttributionTag, that.mAttributionTag);
	}

	@Override
	public int hashCode() {
		return java.util.Objects.hash(mUid, mPackageName, mAttributionTag);
	}
}
