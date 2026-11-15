package android.service.autofill;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/* AOSP SaveRequest */
public final class SaveRequest implements Parcelable {

	private final ArrayList<FillContext> mFillContexts;
	private final Bundle mClientState;
	private final ArrayList<String> mDatasetIds;

	public SaveRequest(ArrayList<FillContext> fillContexts, Bundle clientState, ArrayList<String> datasetIds) {
		mFillContexts = Objects.requireNonNull(fillContexts, "fillContexts");
		mClientState = clientState;
		mDatasetIds = datasetIds;
	}

	private SaveRequest(Parcel parcel) {
		this(parcel.createTypedArrayList(FillContext.CREATOR), parcel.readBundle(), parcel.createStringArrayList());
	}

	public List<FillContext> getFillContexts() {
		return mFillContexts;
	}

	public Bundle getClientState() {
		return mClientState;
	}

	public List<String> getDatasetIds() {
		return mDatasetIds;
	}

	/* AOSP SaveRequest.describeContents */
	@Override
	public int describeContents() {
		return 0;
	}

	/* AOSP SaveRequest.writeToParcel */
	@Override
	public void writeToParcel(Parcel parcel, int flags) {
		parcel.writeTypedList(mFillContexts, flags);
		parcel.writeBundle(mClientState);
		parcel.writeStringList(mDatasetIds);
	}

	/* AOSP SaveRequest.CREATOR */
	public static final Creator<SaveRequest> CREATOR = new Creator<SaveRequest>() {
		@Override
		public SaveRequest createFromParcel(Parcel parcel) {
			return new SaveRequest(parcel);
		}

		@Override
		public SaveRequest[] newArray(int size) {
			return new SaveRequest[size];
		}
	};
}
