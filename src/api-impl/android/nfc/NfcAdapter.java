package android.nfc;

import android.content.Context;

public class NfcAdapter {
	/* AOSP public constants (modules/Nfc framework NfcAdapter) */
	public static final String ACTION_NDEF_DISCOVERED = "android.nfc.action.NDEF_DISCOVERED";
	public static final String ACTION_TECH_DISCOVERED = "android.nfc.action.TECH_DISCOVERED";
	public static final String ACTION_TAG_DISCOVERED = "android.nfc.action.TAG_DISCOVERED";
	public static final String ACTION_ADAPTER_STATE_CHANGED = "android.nfc.action.ADAPTER_STATE_CHANGED";
	public static final String EXTRA_TAG = "android.nfc.extra.TAG";
	public static final String EXTRA_NDEF_MESSAGES = "android.nfc.extra.NDEF_MESSAGES";
	public static final String EXTRA_ID = "android.nfc.extra.ID";
	public static final String EXTRA_ADAPTER_STATE = "android.nfc.extra.ADAPTER_STATE";
	public static final int STATE_OFF = 1;
	public static final int STATE_TURNING_ON = 2;
	public static final int STATE_ON = 3;
	public static final int STATE_TURNING_OFF = 4;

	/* the adapter of the app: AOSP NfcAdapter.getNfcAdapter() keeps one per application context; ATL has one
	 * process and one application */
	private static NfcAdapter sAdapter;

	NfcAdapter() {}

	/* AOSP isEnabled() -> NfcService.getState() == STATE_ON: neard's adapter is powered */
	public boolean isEnabled() {
		return com.android.server.nfc.NeardAdapter.isEnabled();
	}

	/* AOSP getNfcAdapter(): UnsupportedOperationException without FEATURE_NFC (and the card emulation features),
	 * which NfcManager and getDefaultAdapter() turn into null */
	static synchronized NfcAdapter getNfcAdapter() {
		if (!com.android.server.nfc.NeardAdapter.isPresent())
			throw new UnsupportedOperationException();
		if (sAdapter == null)
			sAdapter = new NfcAdapter();
		return sAdapter;
	}

	//@UnsupportedAppUsage
	@Deprecated
	public static NfcAdapter getDefaultAdapter() {
		try {
			return getNfcAdapter();
		} catch (UnsupportedOperationException e) {
			return null;
		}
	}

	/* AOSP getDefaultAdapter(Context): through the NfcManager of the application context */
	public static NfcAdapter getDefaultAdapter(Context context) {
		if (context == null) {
			throw new IllegalArgumentException("context cannot be null");
		}
		context = context.getApplicationContext();
		if (context == null) {
			throw new IllegalArgumentException("context not associated with any application (using a mock context?)");
		}
		NfcManager manager = (NfcManager)context.getSystemService(Context.NFC_SERVICE);
		if (manager == null) {
			return null;
		}
		return manager.getDefaultAdapter();
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
	public static final String EXTRA_READER_TECH_A_POLLING_LOOP_ANNOTATION = "android.nfc.extra.READER_TECH_A_POLLING_LOOP_ANNOTATION";
	public static final int FLAG_READER_DISABLE = 0x0;
	public static final int FLAG_READER_KEEP = 0x80000000;

	/* NfcAdapter.java (modules/Nfc): listen flags for setDiscoveryTechnology() */
	public static final int FLAG_LISTEN_NFC_PASSIVE_A = 0x1;
	public static final int FLAG_LISTEN_NFC_PASSIVE_B = 1 << 1;
	public static final int FLAG_LISTEN_NFC_PASSIVE_F = 1 << 2;
	public static final int FLAG_LISTEN_DISABLE = 0x0;
	public static final int FLAG_LISTEN_KEEP = 0x80000000;

	/* NfcAdapter.java (modules/Nfc): secure element transactions, preferred payment service, tag intent preference */
	public static final String ACTION_TRANSACTION_DETECTED = "android.nfc.action.TRANSACTION_DETECTED";
	public static final String ACTION_PREFERRED_PAYMENT_CHANGED = "android.nfc.action.PREFERRED_PAYMENT_CHANGED";
	public static final String ACTION_CHANGE_TAG_INTENT_PREFERENCE = "android.nfc.action.CHANGE_TAG_INTENT_PREFERENCE";
	public static final String EXTRA_AID = "android.nfc.extra.AID";
	public static final String EXTRA_DATA = "android.nfc.extra.DATA";
	public static final String EXTRA_SECURE_ELEMENT_NAME = "android.nfc.extra.SECURE_ELEMENT_NAME";
	public static final String EXTRA_PREFERRED_PAYMENT_CHANGED_REASON = "android.nfc.extra.PREFERRED_PAYMENT_CHANGED_REASON";
	public static final int PREFERRED_PAYMENT_LOADED = 1;
	public static final int PREFERRED_PAYMENT_CHANGED = 2;
	public static final int PREFERRED_PAYMENT_UPDATED = 3;

	/* AOSP enableForegroundDispatch()/disableForegroundDispatch(): NfcService.setForegroundDispatch(); an instance
	 * exists only with FEATURE_NFC */
	public void enableForegroundDispatch(android.app.Activity activity, android.app.PendingIntent intent,
	                                     android.content.IntentFilter[] filters, String[][] techLists) {
		if (activity == null || intent == null) {
			throw new NullPointerException();
		}
		com.android.server.nfc.NeardAdapter.setForegroundDispatch(intent, filters,
		                                                          techLists != null && techLists.length > 0 ? techLists : null);
	}

	public void disableForegroundDispatch(android.app.Activity activity) {
		com.android.server.nfc.NeardAdapter.setForegroundDispatch(null, null, null);
	}

	/* NfcAdapter.callService()/callServiceReturn(): sService is null and getServiceInterface() finds no NFC service,
	 * so attemptDeadServiceRecovery() rethrows the RemoteException("NFC Service is null") as RuntimeException. */
	private static RuntimeException noService() {
		return new RuntimeException(new android.os.RemoteException("NFC Service is null"));
	}

	/* AOSP enableReaderMode()/disableReaderMode(): NfcActivityManager, reader mode while the activity is resumed */
	public void enableReaderMode(android.app.Activity activity, ReaderCallback callback, int flags,
	                             android.os.Bundle extras) {
		com.android.server.nfc.NeardAdapter.setReaderMode(activity, callback, flags);
	}

	public void disableReaderMode(android.app.Activity activity) {
		com.android.server.nfc.NeardAdapter.setReaderMode(activity, null, 0);
	}

	public boolean disable() {
		throw noService();
	}

	public boolean enable() {
		throw noService();
	}

	/* getNfcAntennaInfo(): UnsupportedOperationException without FEATURE_NFC and FEATURE_NFC_HOST_CARD_EMULATION */
	public NfcAntennaInfo getNfcAntennaInfo() {
		throw new UnsupportedOperationException();
	}

	/* getWlcListenerDeviceInfo()/isWlcEnabled(): UnsupportedOperationException without FEATURE_NFC_CHARGING */
	public WlcListenerDeviceInfo getWlcListenerDeviceInfo() {
		throw new UnsupportedOperationException();
	}

	public boolean isWlcEnabled() {
		throw new UnsupportedOperationException();
	}

	public boolean ignore(Tag tag, int debounceMs, OnTagRemovedListener tagRemovedListener, android.os.Handler handler) {
		throw noService();
	}

	public boolean isExitFramesSupported() {
		throw noService();
	}

	public boolean isObserveModeEnabled() {
		throw noService();
	}

	public boolean isObserveModeSupported() {
		throw noService();
	}

	public boolean setObserveModeEnabled(boolean enabled) {
		throw noService();
	}

	public boolean isPowerSavingModeEnabled() {
		throw noService();
	}

	public boolean isPowerSavingModeSupported() {
		throw noService();
	}

	public void setPowerSavingMode(boolean enabled) {
		throw noService();
	}

	public boolean isReaderModeAnnotationSupported() {
		throw noService();
	}

	public boolean isReaderOptionEnabled() {
		throw new UnsupportedOperationException();
	}

	public boolean isReaderOptionSupported() {
		throw new UnsupportedOperationException();
	}

	public boolean isSecureNfcEnabled() {
		throw new UnsupportedOperationException();
	}

	public boolean isSecureNfcSupported() {
		throw new UnsupportedOperationException();
	}

	public boolean isTagIntentAllowed() {
		throw new UnsupportedOperationException();
	}

	public boolean isTagIntentAppPreferenceSupported() {
		throw new UnsupportedOperationException();
	}

	/* resetDiscoveryTechnology(): activity == null or a resumed activity reach callService() */
	public void resetDiscoveryTechnology(android.app.Activity activity) {
		throw noService();
	}

	/* setDiscoveryTechnology(): UnsupportedOperationException without FEATURE_NFC and FEATURE_NFC_HOST_CARD_EMULATION */
	public void setDiscoveryTechnology(android.app.Activity activity, int pollTechnology, int listenTechnology) {
		throw new UnsupportedOperationException();
	}

	/* NfcAdapter.java: nested callback interfaces */
	@Deprecated
	public static interface CreateBeamUrisCallback {
		public android.net.Uri[] createBeamUris(NfcEvent event);
	}

	@Deprecated
	public static interface CreateNdefMessageCallback {
		public NdefMessage createNdefMessage(NfcEvent event);
	}

	@Deprecated
	public static interface OnNdefPushCompleteCallback {
		public void onNdefPushComplete(NfcEvent event);
	}

	public static interface OnTagRemovedListener {
		void onTagRemoved();
	}

	public interface ReaderCallback {
		public void onTagDiscovered(Tag tag);
	}
}
