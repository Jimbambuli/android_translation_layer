package android.print;

import android.annotation.NonNull;
import android.content.ComponentName;
import android.graphics.drawable.Icon;
import android.util.Log;

import java.util.Collections;
import java.util.List;

/* AOSP core/java/android/print/PrintManager.java for a device without the feature android.software.print:
 * ATL has no print spooler service, so mService == null in every method and each takes the AOSP branch
 * "Feature android.software.print not available" (print() returns null, getPrintJobs() is empty,
 * isPrintServiceEnabled() is false). */
public class PrintManager {
	private static final String LOG_TAG = "PrintManager";

	/* AOSP PrintManager.print(): ATL has no print spooler service, so this is the branch for mService == null,
	 * as on a device without the feature android.software.print */
	public PrintJob print(String printJobName, PrintDocumentAdapter documentAdapter, PrintAttributes attributes) {
		Log.w(LOG_TAG, "Feature android.software.print not available");
		return null;
	}

	/**
	 * Gets the print jobs for this application.
	 *
	 * @return The print job list.
	 * @see PrintJob
	 */
	public @NonNull List<PrintJob> getPrintJobs() {
		Log.w(LOG_TAG, "Feature android.software.print not available");
		return Collections.emptyList();
	}

	/**
	 * Gets whether a print service is enabled.
	 *
	 * @param service The service component.
	 * @return Whether the service is enabled.
	 */
	public boolean isPrintServiceEnabled(@NonNull ComponentName service) {
		Log.w(LOG_TAG, "Feature android.software.print not available");
		return false;
	}

	/** @hide */
	public PrintJob getPrintJob(PrintJobId printJobId) {
		Log.w(LOG_TAG, "Feature android.software.print not available");
		return null;
	}

	/** @hide */
	public Icon getCustomPrinterIcon(PrinterId printerId) {
		Log.w(LOG_TAG, "Feature android.software.print not available");
		return null;
	}

	PrintJobInfo getPrintJobInfo(PrintJobId printJobId) {
		Log.w(LOG_TAG, "Feature android.software.print not available");
		return null;
	}

	void cancelPrintJob(PrintJobId printJobId) {
		Log.w(LOG_TAG, "Feature android.software.print not available");
	}

	void restartPrintJob(PrintJobId printJobId) {
		Log.w(LOG_TAG, "Feature android.software.print not available");
	}
}
