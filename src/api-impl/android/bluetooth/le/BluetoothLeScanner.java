package android.bluetooth.le;

import android.app.PendingIntent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.util.List;

/* BluetoothLeScanner is only handed out by BluetoothAdapter when getLeAccess() holds (adapter on or BLE-only). ATL's adapter stays
 * off, so getBluetoothLe*() returns null and no instance exists; the methods are here for linking and follow AOSP
 * BluetoothLeScanner up to BluetoothLeUtils.checkAdapterStateOn(), which is false for an adapter that is off. */
public final class BluetoothLeScanner {
	private static final String TAG = "BluetoothLeScanner";

	BluetoothLeScanner() {}

	public void startScan(final ScanCallback callback) {
		startScan(null, new ScanSettings.Builder().build(), callback);
	}

	public void startScan(List<ScanFilter> filters, ScanSettings settings, final ScanCallback callback) {
		startScan(filters, settings, callback, null);
	}

	public int startScan(List<ScanFilter> filters, ScanSettings settings, final PendingIntent callbackIntent) {
		return startScan(filters, settings != null ? settings : new ScanSettings.Builder().build(), null, callbackIntent);
	}

	private int startScan(List<ScanFilter> filters, ScanSettings settings, final ScanCallback callback,
	                      final PendingIntent callbackIntent) {
		if (callback == null && callbackIntent == null) {
			throw new IllegalArgumentException("callback is null");
		}
		if (settings == null) {
			throw new IllegalArgumentException("settings is null");
		}
		Log.w(TAG, "BLE is not available");
		return postCallbackErrorOrReturn(callback, ScanCallback.SCAN_FAILED_INTERNAL_ERROR);
	}

	private int postCallbackErrorOrReturn(final ScanCallback callback, final int error) {
		if (callback == null) {
			return error;
		}
		new Handler(Looper.getMainLooper()).post(new Runnable() {
			@Override
			public void run() {
				callback.onScanFailed(error);
			}
		});
		return ScanCallback.NO_ERROR;
	}

	public void stopScan(ScanCallback callback) {
		Log.w(TAG, "BLE is not available");
	}

	public void stopScan(PendingIntent callbackIntent) {
		Log.w(TAG, "BLE is not available");
	}

	public void flushPendingScanResults(ScanCallback callback) {
		Log.w(TAG, "BLE is not available");
		if (callback == null) {
			throw new IllegalArgumentException("callback cannot be null!");
		}
	}
}
