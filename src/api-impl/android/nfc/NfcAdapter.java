package android.nfc;

import android.content.Context;

public class NfcAdapter {

	//@UnsupportedAppUsage
	public static NfcAdapter getDefaultAdapter() {
		return null;
	}

	public static NfcAdapter getDefaultAdapter(Context context) {
		return null;
	}

	/* AOSP NfcAdapter: sHasNfcFeature is false without FEATURE_NFC, ATL declares none (getDefaultAdapter() is null,
	 * so an app normally never gets here) */
	public void enableForegroundDispatch(android.app.Activity activity, android.app.PendingIntent intent, android.content.IntentFilter[] filters, String[][] techLists) {
		throw new UnsupportedOperationException();
	}

	public void disableForegroundDispatch(android.app.Activity activity) {
		throw new UnsupportedOperationException();
	}
}
