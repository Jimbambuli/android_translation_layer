package android.view.contentcapture;

import android.os.ParcelFileDescriptor;

/* AOSP DataShareWriteAdapter: the app's side of ContentCaptureManager.shareData() */
public interface DataShareWriteAdapter {
	void onWrite(ParcelFileDescriptor destination);

	void onRejected();

	/* AOSP: does nothing by default */
	default void onError(int errorCode) {}
}
