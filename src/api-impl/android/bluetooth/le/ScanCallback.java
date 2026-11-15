package android.bluetooth.le;

/* AOSP ScanCallback without onScanResult/onBatchScanResults (ScanResult is not in ATL). */
public abstract class ScanCallback {
	public static final int SCAN_FAILED_ALREADY_STARTED = 1;
	public static final int SCAN_FAILED_APPLICATION_REGISTRATION_FAILED = 2;
	public static final int SCAN_FAILED_INTERNAL_ERROR = 3;
	public static final int SCAN_FAILED_FEATURE_UNSUPPORTED = 4;
	public static final int SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES = 5;
	public static final int SCAN_FAILED_SCANNING_TOO_FREQUENTLY = 6;

	static final int NO_ERROR = 0;

	public void onScanFailed(int errorCode) {}
}
