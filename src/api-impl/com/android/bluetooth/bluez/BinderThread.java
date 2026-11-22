package com.android.bluetooth.bluez;

import android.os.Handler;
import android.os.HandlerThread;

/* ATL: in AOSP the Bluetooth app's oneway callbacks reach the app on a binder thread, not on its main thread; the
 * framework waits for some of them on the calling thread (BluetoothLeScanner.BleScanCallbackWrapper waits for
 * onScannerRegistered, also when startScan() runs on the main thread). Such callbacks run on this thread. */
public final class BinderThread {
	private static Handler handler;

	private BinderThread() {}

	public static synchronized void post(Runnable r) {
		if (handler == null) {
			HandlerThread thread = new HandlerThread("BluetoothBinder");
			thread.start();
			handler = new Handler(thread.getLooper());
		}
		handler.post(r);
	}
}
