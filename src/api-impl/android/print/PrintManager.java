package android.print;

import android.util.Log;

public class PrintManager {
	private static final String LOG_TAG = "PrintManager";

	/* AOSP PrintManager.print(): ATL has no print spooler service, so this is the branch for mService == null,
	 * as on a device without the feature android.software.print */
	public PrintJob print(String printJobName, PrintDocumentAdapter documentAdapter, PrintAttributes attributes) {
		Log.w(LOG_TAG, "Feature android.software.print not available");
		return null;
	}
}
