package android.nfc;

import android.content.Context;

public class NfcAdapter {

	NfcAdapter() {}

	/* getDefaultAdapter() is null (device without NFC), so no instance exists; the method is here for linking. */
	public boolean isEnabled() {
		return false;
	}

	//@UnsupportedAppUsage
	public static NfcAdapter getDefaultAdapter() {
		return null;
	}

	public static NfcAdapter getDefaultAdapter(Context context) {
		return null;
	}
}
