package android.view.contentcapture;

import android.annotation.NonNull;
import android.app.ActivityThread;
import android.content.LocusId;
import android.os.Parcel;
import android.os.Parcelable;
import com.android.internal.util.AnnotationValidations;
import java.util.Objects;

/* AOSP DataShareRequest: what an app shares with the content capture service (package, locus id, MIME type) */
public final class DataShareRequest implements Parcelable {
	private final String mPackageName;
	private final LocusId mLocusId;
	private final String mMimeType;

	public DataShareRequest(LocusId locusId, String mimeType) {
		Objects.requireNonNull(mimeType);
		mPackageName = ActivityThread.currentActivityThread().getApplication().getPackageName();
		mLocusId = locusId;
		mMimeType = mimeType;
	}

	public String getPackageName() {
		return mPackageName;
	}

	public LocusId getLocusId() {
		return mLocusId;
	}

	public String getMimeType() {
		return mMimeType;
	}

	@Override
	public String toString() {
		return "DataShareRequest { " +
		    "packageName = " + mPackageName + ", " +
		    "locusId = " + mLocusId + ", " +
		    "mimeType = " + mMimeType +
		    " }";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		DataShareRequest that = (DataShareRequest)o;
		return Objects.equals(mPackageName, that.mPackageName)
		    && Objects.equals(mLocusId, that.mLocusId)
		    && Objects.equals(mMimeType, that.mMimeType);
	}

	@Override
	public int hashCode() {
		int _hash = 1;
		_hash = 31 * _hash + Objects.hashCode(mPackageName);
		_hash = 31 * _hash + Objects.hashCode(mLocusId);
		_hash = 31 * _hash + Objects.hashCode(mMimeType);
		return _hash;
	}

	/* AOSP (codegen): a presence byte for the locus id, then the fields */
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		byte flg = 0;
		if (mLocusId != null)
			flg |= 0x2;
		dest.writeByte(flg);
		dest.writeString(mPackageName);
		if (mLocusId != null)
			dest.writeTypedObject(mLocusId, flags);
		dest.writeString(mMimeType);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	/* package-private */ DataShareRequest(Parcel in) {
		byte flg = in.readByte();
		String packageName = in.readString();
		LocusId locusId = (flg & 0x2) == 0 ? null : (LocusId)in.readTypedObject(LocusId.CREATOR);
		String mimeType = in.readString();
		this.mPackageName = packageName;
		AnnotationValidations.validate(NonNull.class, null, mPackageName);
		this.mLocusId = locusId;
		this.mMimeType = mimeType;
		AnnotationValidations.validate(NonNull.class, null, mMimeType);
	}

	public static final Parcelable.Creator<DataShareRequest> CREATOR = new Parcelable.Creator<DataShareRequest>() {
		@Override
		public DataShareRequest[] newArray(int size) {
			return new DataShareRequest[size];
		}

		@Override
		public DataShareRequest createFromParcel(Parcel in) {
			return new DataShareRequest(in);
		}
	};
}
