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

	/* NfcAdapter.callService()/callServiceReturn(): sService is null and getServiceInterface() finds no NFC service,
	 * so attemptDeadServiceRecovery() rethrows the RemoteException("NFC Service is null") as RuntimeException. */
	private static RuntimeException noService() {
		return new RuntimeException(new android.os.RemoteException("NFC Service is null"));
	}

	/* NfcAdapter.enableReaderMode()/disableReaderMode(): UnsupportedOperationException without FEATURE_NFC */
	public void enableReaderMode(android.app.Activity activity, ReaderCallback callback, int flags,
	                             android.os.Bundle extras) {
		throw new UnsupportedOperationException();
	}

	public void disableReaderMode(android.app.Activity activity) {
		throw new UnsupportedOperationException();
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
