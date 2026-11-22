package com.android.server.wifi;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.NetworkCapabilities;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

/*
 * The parts of AOSP's Wi-Fi service an app reaches through WifiManager, in the app's process (the pattern of
 * LocationManagerService): WifiServiceImpl (startScan, getScanResults, getConnectionInfo, getWifiEnabledState,
 * registerScanResultsCallback), ScanRequestProxy (throttling, results cache, SCAN_RESULTS_AVAILABLE_ACTION) and
 * WifiPermissionsUtil.enforceCanAccessScanResults(). The hardware below is NetworkManager (NetworkManagerWifi).
 *
 * Left out, because ATL has none of it: app ops (OPSTR_WIFI_SCAN, FINE_LOCATION are allowed when the permission is),
 * users and profiles, idle mode, background apps (the app's window counts as foreground), the settings and setup
 * wizard permissions, hidden network scans, scan-only mode (isScanAlwaysAvailable() stays false: NetworkManager does
 * not scan with the radio off).
 */
public final class WifiServiceImpl {
	private static final String TAG = "WifiService";

	/* ScanRequestProxy */
	public static final int SCAN_REQUEST_THROTTLE_TIME_WINDOW_FG_APPS_MS = 120 * 1000;
	public static final int SCAN_REQUEST_THROTTLE_MAX_IN_TIME_WINDOW_FG_APPS = 4;

	private static WifiServiceImpl sInstance;

	private final Handler mHandler = new Handler(Looper.getMainLooper()); // AOSP's WifiThreadRunner
	private final Map<String, LinkedList<Long>> mLastScanTimestampsForFgApps = new HashMap<>();
	private final Map<String, ScanResult> mFullScanCache = new HashMap<>();
	private boolean mCacheFilled;
	private final List<Object[]> mScanResultsCallbacks = new ArrayList<>(); // {Executor, ScanResultsCallback}
	private int mWifiState = -1;
	/* WifiSettingsConfigStore.WIFI_SCAN_THROTTLE_ENABLED: true unless the user turns it off in the developer
	 * options, which ATL does not have */
	private boolean mScanThrottleEnabled = true;

	public static synchronized WifiServiceImpl get() {
		if (sInstance == null)
			sInstance = new WifiServiceImpl();
		return sInstance;
	}

	private WifiServiceImpl() {}

	/* ---- permissions (WifiPermissionsUtil, Context.enforceCallingOrSelfPermission) ---- */

	private static boolean granted(Context context, String permission) {
		return context.getPackageManager().checkPermission(permission, context.getOpPackageName()) == PackageManager.PERMISSION_GRANTED;
	}

	private static void enforceAccessPermission(Context context) {
		if (!granted(context, Manifest.permission.ACCESS_WIFI_STATE))
			throw new SecurityException(context.getOpPackageName() + " was not granted " + Manifest.permission.ACCESS_WIFI_STATE + ": WifiService");
	}

	private static void enforceChangePermission(Context context) {
		if (!granted(context, Manifest.permission.CHANGE_WIFI_STATE))
			throw new SecurityException(context.getOpPackageName() + " was not granted " + Manifest.permission.CHANGE_WIFI_STATE + ": WifiService");
	}

	/* WifiPermissionsUtil.enforceCanAccessScanResults(): location on, FINE (COARSE before Q) location permission */
	private static void enforceCanAccessScanResults(Context context) {
		LocationManager locationManager = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
		if (locationManager == null || !locationManager.isLocationEnabled())
			throw new SecurityException("Location mode is disabled for the device");
		boolean targetSdkLessThanQ = context.getApplicationInfo().targetSdkVersion < Build.VERSION_CODES.Q;
		boolean allowed = granted(context, Manifest.permission.ACCESS_FINE_LOCATION)
		                  || (targetSdkLessThanQ && granted(context, Manifest.permission.ACCESS_COARSE_LOCATION));
		if (!allowed)
			throw new SecurityException("UID " + android.os.Process.myUid() + " has no location permission");
	}

	private static boolean checkLocalMacAddressPermission(Context context) {
		// a signature|privileged permission (@SystemApi), no app holds it in ATL
		return granted(context, "android.permission.LOCAL_MAC_ADDRESS");
	}

	/* ---- state ---- */

	/* ActiveModeWarden.getWifiState(): the radio switch of NetworkManager and a device it can use */
	private static int currentWifiState(int[] device) {
		return device[NetworkManagerWifi.DEVICE_WIRELESS_ENABLED] != 0 && device[NetworkManagerWifi.DEVICE_STATE] >= NetworkManagerWifi.NM_DEVICE_STATE_DISCONNECTED
		    ? WifiManager.WIFI_STATE_ENABLED
		    : WifiManager.WIFI_STATE_DISABLED;
	}

	public int getWifiEnabledState(Context context) {
		enforceAccessPermission(context);
		return currentWifiState(NetworkManagerWifi.getDevice());
	}

	/* WifiServiceImpl.is5GHzBandSupported() etc.: the bands the device can use (ActiveModeWarden.isBandSupportedForSta) */
	public boolean isBandSupported(int nmCapability) {
		int caps = NetworkManagerWifi.getDevice()[NetworkManagerWifi.DEVICE_CAPABILITIES];
		return (caps & NetworkManagerWifi.NM_WIFI_DEVICE_CAP_FREQ_VALID) != 0 && (caps & nmCapability) != 0;
	}

	public boolean isScanThrottleEnabled() {
		return mScanThrottleEnabled;
	}

	/* WifiServiceImpl.setWifiEnabled(): CHANGE_WIFI_STATE, refused (false) for apps targeting Q or later, otherwise
	 * the radio is switched (NetworkManager's WirelessEnabled, polkit decides) and the answer is true. Without a
	 * Wi-Fi device nothing can be switched and the state stays WIFI_STATE_DISABLED. The confirmation dialog for
	 * third-party apps (config_showConfirmationDialogForThirdPartyAppsEnablingWifi) is not shown (its default in
	 * AOSP not checked). */
	public boolean setWifiEnabled(Context context, boolean enable) {
		enforceChangePermission(context);
		if (context.getApplicationInfo().targetSdkVersion >= Build.VERSION_CODES.Q) {
			Log.i(TAG, "setWifiEnabled not allowed for uid=" + android.os.Process.myUid());
			return false;
		}
		Log.i(TAG, "setWifiEnabled package=" + context.getOpPackageName() + " enable=" + enable);
		if (NetworkManagerWifi.isPresent())
			NetworkManagerWifi.setWirelessEnabled(enable);
		return true;
	}

	/* ---- scans (WifiServiceImpl.startScan, ScanRequestProxy.startScan) ---- */

	public boolean startScan(Context context) {
		enforceChangePermission(context);
		String packageName = context.getOpPackageName();
		Log.i(TAG, "startScan uid=" + android.os.Process.myUid() + " packageName=" + packageName); // mLog.info("startScan uid=%")
		try {
			enforceCanAccessScanResults(context);
		} catch (SecurityException e) {
			Log.w(TAG, "Permission violation - startScan not allowed for packageName=" + packageName + ", reason=" + e);
			return false;
		}
		if (currentWifiState(NetworkManagerWifi.getDevice()) != WifiManager.WIFI_STATE_ENABLED) { // !mScanningEnabled
			Log.e(TAG, "Failed to retrieve wifiscanner");
			sendScanResultFailureBroadcastToPackage(packageName);
			return false;
		}
		if (isScanThrottleEnabled() && shouldScanRequestBeThrottledForForegroundApp(packageName)) {
			Log.i(TAG, "Scan request from " + packageName + " throttled");
			sendScanResultFailureBroadcastToPackage(packageName);
			return false;
		}
		NetworkManagerWifi.requestScan();
		return true;
	}

	private boolean shouldScanRequestBeThrottledForForegroundApp(String packageName) {
		synchronized (mLastScanTimestampsForFgApps) {
			LinkedList<Long> scanRequestTimestamps = mLastScanTimestampsForFgApps.get(packageName);
			if (scanRequestTimestamps == null) {
				scanRequestTimestamps = new LinkedList<>();
				mLastScanTimestampsForFgApps.put(packageName, scanRequestTimestamps);
			}
			long currentTimeMillis = SystemClock.elapsedRealtime();
			while (!scanRequestTimestamps.isEmpty()
			       && currentTimeMillis - scanRequestTimestamps.getFirst() > SCAN_REQUEST_THROTTLE_TIME_WINDOW_FG_APPS_MS)
				scanRequestTimestamps.removeFirst();
			if (scanRequestTimestamps.size() >= SCAN_REQUEST_THROTTLE_MAX_IN_TIME_WINDOW_FG_APPS)
				return true;
			scanRequestTimestamps.addLast(currentTimeMillis);
			return false;
		}
	}

	/* ScanRequestProxy's broadcasts: to the receivers registered at run time (since O an implicit broadcast does not
	 * reach manifest receivers) */
	private static void sendScanResultBroadcast(boolean scanSucceeded) {
		Intent intent = new Intent(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
		intent.putExtra(WifiManager.EXTRA_RESULTS_UPDATED, scanSucceeded);
		Context.sendBroadcastToReceivers(intent);
	}

	private static void sendScanResultFailureBroadcastToPackage(String packageName) {
		Intent intent = new Intent(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
		intent.putExtra(WifiManager.EXTRA_RESULTS_UPDATED, false);
		intent.setPackage(packageName);
		Context.sendBroadcastToReceivers(intent);
	}

	/* ScanRequestProxy.GlobalScanListener.onResults() for every full scan, also the ones NetworkManager does by
	 * itself (AOSP's connectivity scans reach the listener the same way); ScanRequestProxyScanListener.onFailure() */
	void onScanDone(boolean success) {
		if (success) {
			synchronized (mFullScanCache) {
				fillCache();
			}
			sendScanResultBroadcast(true);
			sendScanResultsAvailableToCallbacks();
		} else {
			Log.e(TAG, "Scan failure received.");
			sendScanResultBroadcast(false);
		}
	}

	private void fillCache() {
		mFullScanCache.clear();
		for (ScanResult s : NetworkManagerWifi.getScanResults()) {
			ScanResult scanResult = mFullScanCache.get(s.BSSID);
			if (scanResult == null || android.text.TextUtils.isEmpty(scanResult.SSID) || !android.text.TextUtils.isEmpty(s.SSID))
				mFullScanCache.put(s.BSSID, s);
		}
		mCacheFilled = true;
	}

	public List<ScanResult> getScanResults(Context context) {
		enforceAccessPermission(context);
		try {
			enforceCanAccessScanResults(context);
		} catch (SecurityException e) {
			Log.w(TAG, "Permission violation - getScanResults not allowed for packageName=" + context.getOpPackageName() + ", reason=" + e);
			return null;
		}
		if (currentWifiState(NetworkManagerWifi.getDevice()) != WifiManager.WIFI_STATE_ENABLED)
			return new ArrayList<>(); // ScanRequestProxy.clearScanResults() when scanning is disabled
		synchronized (mFullScanCache) {
			/* the system's scans before the app started: the results NetworkManager holds */
			if (!mCacheFilled)
				fillCache();
			List<ScanResult> results = new ArrayList<>();
			for (ScanResult s : mFullScanCache.values())
				results.add(new ScanResult(s));
			return results;
		}
	}

	public void registerScanResultsCallback(Context context, Executor executor, WifiManager.ScanResultsCallback callback) {
		enforceAccessPermission(context);
		synchronized (mScanResultsCallbacks) {
			mScanResultsCallbacks.add(new Object[] {executor, callback});
		}
	}

	public void unregisterScanResultsCallback(WifiManager.ScanResultsCallback callback) {
		synchronized (mScanResultsCallbacks) {
			for (int i = mScanResultsCallbacks.size() - 1; i >= 0; i--)
				if (mScanResultsCallbacks.get(i)[1] == callback)
					mScanResultsCallbacks.remove(i);
		}
	}

	private void sendScanResultsAvailableToCallbacks() {
		List<Object[]> callbacks;
		synchronized (mScanResultsCallbacks) {
			callbacks = new ArrayList<>(mScanResultsCallbacks);
		}
		for (Object[] entry : callbacks) {
			final WifiManager.ScanResultsCallback callback = (WifiManager.ScanResultsCallback)entry[1];
			((Executor)entry[0]).execute(callback::onScanResultsAvailable);
		}
	}

	/* ---- connection (WifiServiceImpl.getConnectionInfo) ---- */

	public WifiInfo getConnectionInfo(Context context) {
		enforceAccessPermission(context);
		int[] device = NetworkManagerWifi.getDevice();
		if (currentWifiState(device) != WifiManager.WIFI_STATE_ENABLED)
			return new WifiInfo();
		WifiInfo wifiInfo = NetworkManagerWifi.getConnectionInfo(device);
		long redactions = wifiInfo.getApplicableRedactions();
		if (checkLocalMacAddressPermission(context))
			redactions &= ~NetworkCapabilities.REDACT_FOR_LOCAL_MAC_ADDRESS;
		try {
			enforceCanAccessScanResults(context);
			redactions &= ~NetworkCapabilities.REDACT_FOR_ACCESS_FINE_LOCATION;
		} catch (SecurityException ignored) {
		}
		return wifiInfo.makeCopy(redactions);
	}

	/* ---- WIFI_STATE_CHANGED_ACTION (ActiveModeWarden: a sticky broadcast) ---- */

	void onStateChanged() {
		int state = currentWifiState(NetworkManagerWifi.getDevice());
		int previous;
		synchronized (this) {
			previous = mWifiState;
			mWifiState = state;
		}
		if (previous == -1 || previous == state)
			return;
		sendWifiStateChanged(state, previous, false);
		if (state != WifiManager.WIFI_STATE_ENABLED) {
			synchronized (mFullScanCache) {
				mFullScanCache.clear();
				mCacheFilled = false;
			}
		}
	}

	/* ActiveModeWarden.setWifiStateForApiCalls() -> WifiController: EXTRA_WIFI_STATE, EXTRA_PREVIOUS_WIFI_STATE */
	private static void sendWifiStateChanged(int state, int previous, boolean initialOnly) {
		Intent intent = new Intent(WifiManager.WIFI_STATE_CHANGED_ACTION);
		intent.putExtra(WifiManager.EXTRA_WIFI_STATE, state);
		intent.putExtra(WifiManager.EXTRA_PREVIOUS_WIFI_STATE, previous);
		Context.sendStickyBroadcastToReceivers(intent, initialOnly);
	}

	/** called when the WifiManager comes up: the sticky state the system had already sent (ENABLING -> ENABLED or
	 * DISABLING -> DISABLED), the state the next change is compared with */
	public synchronized void start() {
		if (mWifiState != -1)
			return;
		mWifiState = currentWifiState(NetworkManagerWifi.getDevice());
		sendWifiStateChanged(mWifiState, mWifiState == WifiManager.WIFI_STATE_ENABLED ? WifiManager.WIFI_STATE_ENABLING : WifiManager.WIFI_STATE_DISABLING, true);
	}
}
