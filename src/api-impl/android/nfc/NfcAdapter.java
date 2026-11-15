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

	/* NfcAdapter.java (modules/Nfc): reader mode flags and extras */
	public static final int FLAG_READER_NFC_A = 0x1;
	public static final int FLAG_READER_NFC_B = 0x2;
	public static final int FLAG_READER_NFC_F = 0x4;
	public static final int FLAG_READER_NFC_V = 0x8;
	public static final int FLAG_READER_NFC_BARCODE = 0x10;
	public static final int FLAG_READER_SKIP_NDEF_CHECK = 0x80;
	public static final int FLAG_READER_NO_PLATFORM_SOUNDS = 0x100;
	public static final String EXTRA_READER_PRESENCE_CHECK_DELAY = "presence";

	/* NfcAdapter.enableForegroundDispatch()/disableForegroundDispatch(): without FEATURE_NFC (sHasNfcFeature false)
	 * both throw UnsupportedOperationException, before any argument check. No instance exists here
	 * (getDefaultAdapter() is null); enable/disableReaderMode need android.nfc.Tag and are not implemented. */
	public void enableForegroundDispatch(android.app.Activity activity, android.app.PendingIntent intent,
	                                     android.content.IntentFilter[] filters, String[][] techLists) {
		throw new UnsupportedOperationException();
	}

	public void disableForegroundDispatch(android.app.Activity activity) {
		throw new UnsupportedOperationException();
	}
}
