package android.nfc.cardemulation;

import android.app.Activity;
import android.content.ComponentName;
import android.nfc.NfcAdapter;

/* AOSP NfcFCardEmulation (modules/Nfc). Without NFC-F card emulation (FEATURE_NFC_HOST_CARD_EMULATION_NFCF missing)
 * getInstance() throws UnsupportedOperationException (NullPointerException for a null adapter); no instance exists,
 * the instance methods follow AOSP's argument checks and then report failure. */
public final class NfcFCardEmulation {
	private NfcFCardEmulation() {}

	public static synchronized NfcFCardEmulation getInstance(NfcAdapter adapter) {
		if (adapter == null)
			throw new NullPointerException("NfcAdapter is null");
		throw new UnsupportedOperationException();
	}

	public String getSystemCodeForService(ComponentName service) throws RuntimeException {
		if (service == null) {
			throw new NullPointerException("service is null");
		}
		return null;
	}

	public boolean registerSystemCodeForService(ComponentName service, String systemCode) throws RuntimeException {
		if (service == null || systemCode == null) {
			throw new NullPointerException("service or systemCode is null");
		}
		return false;
	}

	public boolean unregisterSystemCodeForService(ComponentName service) throws RuntimeException {
		if (service == null) {
			throw new NullPointerException("service is null");
		}
		return false;
	}

	public String getNfcid2ForService(ComponentName service) throws RuntimeException {
		if (service == null) {
			throw new NullPointerException("service is null");
		}
		return null;
	}

	public boolean setNfcid2ForService(ComponentName service, String nfcid2) throws RuntimeException {
		if (service == null || nfcid2 == null) {
			throw new NullPointerException("service or nfcid2 is null");
		}
		return false;
	}

	public boolean enableService(Activity activity, ComponentName service) throws RuntimeException {
		if (activity == null || service == null) {
			throw new NullPointerException("activity or service is null");
		}
		if (!activity.isResumed()) {
			throw new IllegalArgumentException("Activity must be resumed.");
		}
		return false;
	}

	public boolean disableService(Activity activity) throws RuntimeException {
		if (activity == null) {
			throw new NullPointerException("activity is null");
		}
		if (!activity.isResumed()) {
			throw new IllegalArgumentException("Activity must be resumed.");
		}
		return false;
	}
}
