package android.nfc;

/* AOSP NfcManager: the adapter of the application, null without FEATURE_NFC */
public final class NfcManager {
	private final NfcAdapter mAdapter;

	/** @hide */
	public NfcManager() {
		NfcAdapter adapter;
		try {
			adapter = NfcAdapter.getNfcAdapter();
		} catch (UnsupportedOperationException e) {
			adapter = null;
		}
		mAdapter = adapter;
	}

	public NfcAdapter getDefaultAdapter() {
		return mAdapter;
	}
}
