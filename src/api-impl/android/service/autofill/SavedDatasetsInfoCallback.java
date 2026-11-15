package android.service.autofill;

import java.util.Set;

/* AOSP SavedDatasetsInfoCallback */
public interface SavedDatasetsInfoCallback {

	int ERROR_OTHER = 0;
	int ERROR_UNSUPPORTED = 1;
	int ERROR_NEEDS_USER_ACTION = 2;

	void onSuccess(Set<SavedDatasetsInfo> results);

	void onError(int error);
}
