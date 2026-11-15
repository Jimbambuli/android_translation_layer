package android.service.autofill;

import android.os.Bundle;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/* AOSP SaveRequest; Parcelable left out */
public final class SaveRequest {

	private final ArrayList<FillContext> mFillContexts;
	private final Bundle mClientState;
	private final ArrayList<String> mDatasetIds;

	public SaveRequest(ArrayList<FillContext> fillContexts, Bundle clientState, ArrayList<String> datasetIds) {
		mFillContexts = Objects.requireNonNull(fillContexts, "fillContexts");
		mClientState = clientState;
		mDatasetIds = datasetIds;
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
}
