package android.nfc.cardemulation;

import android.app.Activity;
import android.content.ComponentName;
import android.nfc.NfcAdapter;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.regex.Pattern;

/* AOSP CardEmulation (modules/Nfc). ATL has no NFC hardware and no INfcCardEmulation service: like on a device
 * without FEATURE_NFC_HOST_CARD_EMULATION getInstance() throws UnsupportedOperationException (NullPointerException
 * for a null adapter), so no instance exists and the instance methods are only here for linking. They return what
 * AOSP returns when the service call yields nothing (false, null, empty list). */
public final class CardEmulation {
	private static final Pattern PLPF_PATTERN = Pattern.compile("[0-9A-Fa-f]{2,}[0-9A-Fa-f,\\?,\\*\\.]*");

	@Deprecated
	public static final String ACTION_CHANGE_DEFAULT = "android.nfc.cardemulation.action.ACTION_CHANGE_DEFAULT";
	public static final String EXTRA_CATEGORY = "category";
	public static final String EXTRA_SERVICE_COMPONENT = "component";
	public static final String CATEGORY_PAYMENT = "payment";
	public static final String CATEGORY_OTHER = "other";
	public static final int SELECTION_MODE_PREFER_DEFAULT = 0;
	public static final int SELECTION_MODE_ALWAYS_ASK = 1;
	public static final int SELECTION_MODE_ASK_IF_CONFLICT = 2;
	public static final int PROTOCOL_AND_TECHNOLOGY_ROUTE_DH = 0;
	public static final int PROTOCOL_AND_TECHNOLOGY_ROUTE_ESE = 1;
	public static final int PROTOCOL_AND_TECHNOLOGY_ROUTE_UICC = 2;
	public static final int PROTOCOL_AND_TECHNOLOGY_ROUTE_DEFAULT = 3;
	public static final int PROTOCOL_AND_TECHNOLOGY_ROUTE_NDEF_NFCEE = 4;
	public static final int PROTOCOL_AND_TECHNOLOGY_ROUTE_UNSET = -1;
	public static final String PROPERTY_ALLOW_SHARED_ROLE_PRIORITY = "android.nfc.cardemulation.PROPERTY_ALLOW_SHARED_ROLE_PRIORITY";
	public static final int NFC_INTERNAL_ERROR_UNKNOWN = 0;
	public static final int NFC_INTERNAL_ERROR_NFC_CRASH_RESTART = 1;
	public static final int NFC_INTERNAL_ERROR_NFC_HARDWARE_ERROR = 2;
	public static final int NFC_INTERNAL_ERROR_COMMAND_TIMEOUT = 3;

	/* AOSP CardEmulation.NfcEventCallback */
	public interface NfcEventCallback {
		default void onPreferredServiceChanged(boolean isPreferred) {}

		default void onObserveModeStateChanged(boolean isEnabled) {}

		default void onAidConflictOccurred(String aid) {}

		default void onAidNotRouted(String aid) {}

		default void onNfcStateChanged(int state) {}

		default void onRemoteFieldChanged(boolean isDetected) {}

		default void onInternalErrorReported(int errorType) {}

		default void onOffHostAidSelected(String aid, String offHostSecureElement) {}
	}

	private CardEmulation() {}

	/* AOSP getInstance(): the device has no host card emulation (FEATURE_NFC_HOST_CARD_EMULATION missing) */
	public static synchronized CardEmulation getInstance(NfcAdapter adapter) {
		if (adapter == null)
			throw new NullPointerException("NfcAdapter is null");
		throw new UnsupportedOperationException();
	}

	public boolean isDefaultServiceForCategory(ComponentName service, String category) {
		return false;
	}

	public boolean isDefaultServiceForAid(ComponentName service, String aid) {
		return false;
	}

	/* AOSP: true for every category except payment, which follows a setting (not carried, off by default) */
	public boolean categoryAllowsForegroundPreference(String category) {
		return !CATEGORY_PAYMENT.equals(category);
	}

	/* AOSP getSelectionModeForCategory(): the payment default is never registered here */
	public int getSelectionModeForCategory(String category) {
		if (CATEGORY_PAYMENT.equals(category)) {
			return SELECTION_MODE_ALWAYS_ASK;
		} else {
			return SELECTION_MODE_ASK_IF_CONFLICT;
		}
	}

	public boolean setShouldDefaultToObserveModeForService(ComponentName service, boolean enable) {
		return false;
	}

	public void setRequireDeviceScreenOnForService(ComponentName service, boolean enable) {}

	public boolean isDeviceScreenOnRequiredForService(ComponentName service) {
		return false;
	}

	public void setRequireDeviceUnlockForService(ComponentName service, boolean enable) {}

	public boolean isDeviceUnlockRequiredForService(ComponentName service) {
		return false;
	}

	public boolean registerPollingLoopFilterForService(ComponentName service, String pollingLoopFilter,
	                                                   boolean autoTransact) {
		validatePollingLoopFilter(pollingLoopFilter);
		return false;
	}

	public boolean removePollingLoopFilterForService(ComponentName service, String pollingLoopFilter) {
		validatePollingLoopFilter(pollingLoopFilter);
		return false;
	}

	public List<String> getPollingLoopFiltersForService(ComponentName service) {
		return List.of();
	}

	public boolean registerPollingLoopPatternFilterForService(ComponentName service, String pollingLoopPatternFilter,
	                                                          boolean autoTransact) {
		validatePollingLoopPatternFilter(pollingLoopPatternFilter);
		return false;
	}

	public boolean removePollingLoopPatternFilterForService(ComponentName service, String pollingLoopPatternFilter) {
		validatePollingLoopPatternFilter(pollingLoopPatternFilter);
		return false;
	}

	public List<String> getPollingLoopPatternFiltersForService(ComponentName service) {
		return List.of();
	}

	public boolean registerAidsForService(ComponentName service, String category, List<String> aids) {
		return false;
	}

	public boolean unsetOffHostForService(ComponentName service) {
		return false;
	}

	/* AOSP setOffHostForService(): false without an NfcAdapter */
	public boolean setOffHostForService(ComponentName service, String offHostSecureElement) {
		return false;
	}

	public List<String> getAidsForService(ComponentName service, String category) {
		return null;
	}

	public boolean removeAidsForService(ComponentName service, String category) {
		return false;
	}

	public boolean setPreferredService(Activity activity, ComponentName service) {
		if (activity == null || service == null) {
			throw new NullPointerException("activity or service or category is null");
		}
		return false;
	}

	public boolean unsetPreferredService(Activity activity) {
		if (activity == null) {
			throw new NullPointerException("activity is null");
		}
		return false;
	}

	public boolean supportsAidPrefixRegistration() {
		return false;
	}

	public List<String> getAidsForPreferredPaymentService() {
		return null;
	}

	public String getRouteDestinationForPreferredPaymentService() {
		return null;
	}

	public CharSequence getDescriptionForPreferredPaymentService() {
		return null;
	}

	/* AOSP validatePollingLoopFilter() */
	static String validatePollingLoopFilter(String pollingLoopFilter) {
		byte[] plfBytes = java.util.HexFormat.of().parseHex(pollingLoopFilter);
		if (plfBytes.length == 0) {
			throw new IllegalArgumentException("Polling loop filter must contain at least one byte.");
		}
		return java.util.HexFormat.of().withUpperCase().formatHex(plfBytes);
	}

	/* AOSP validatePollingLoopPatternFilter() */
	static String validatePollingLoopPatternFilter(String pollingLoopPatternFilter) {
		if (!PLPF_PATTERN.matcher(pollingLoopPatternFilter).matches()) {
			throw new IllegalArgumentException("Polling loop pattern filters may only contain hexadecimal numbers, ?s and *s");
		}
		return Pattern.compile(pollingLoopPatternFilter.toUpperCase(Locale.ROOT)).toString();
	}

	public boolean isEuiccSupported() {
		return false;
	}

	/* AOSP getDefaultNfcSubscriptionId(): SubscriptionManager.INVALID_SUBSCRIPTION_ID */
	public int getDefaultNfcSubscriptionId() {
		return -1;
	}

	public void registerNfcEventCallback(Executor executor, NfcEventCallback callback) {}

	public void unregisterNfcEventCallback(NfcEventCallback callback) {}
}
