package android.telephony;

import java.util.ArrayList;
import java.util.List;

public class TelephonyManager {
	/* AOSP TelephonyManager.NETWORK_TYPE_* and getNetworkTypeName() */
	public static final int NETWORK_TYPE_UNKNOWN = 0;
	public static final int NETWORK_TYPE_GPRS = 1;
	public static final int NETWORK_TYPE_EDGE = 2;
	public static final int NETWORK_TYPE_UMTS = 3;
	public static final int NETWORK_TYPE_CDMA = 4;
	public static final int NETWORK_TYPE_EVDO_0 = 5;
	public static final int NETWORK_TYPE_EVDO_A = 6;
	public static final int NETWORK_TYPE_1xRTT = 7;
	public static final int NETWORK_TYPE_HSDPA = 8;
	public static final int NETWORK_TYPE_HSUPA = 9;
	public static final int NETWORK_TYPE_HSPA = 10;
	public static final int NETWORK_TYPE_IDEN = 11;
	public static final int NETWORK_TYPE_EVDO_B = 12;
	public static final int NETWORK_TYPE_LTE = 13;
	public static final int NETWORK_TYPE_EHRPD = 14;
	public static final int NETWORK_TYPE_HSPAP = 15;
	public static final int NETWORK_TYPE_GSM = 16;
	public static final int NETWORK_TYPE_TD_SCDMA = 17;
	public static final int NETWORK_TYPE_IWLAN = 18;
	public static final int NETWORK_TYPE_LTE_CA = 19;
	public static final int NETWORK_TYPE_NR = 20;

	public static String getNetworkTypeName(int type) {
		switch (type) {
			case NETWORK_TYPE_GPRS:
				return "GPRS";
			case NETWORK_TYPE_EDGE:
				return "EDGE";
			case NETWORK_TYPE_UMTS:
				return "UMTS";
			case NETWORK_TYPE_HSDPA:
				return "HSDPA";
			case NETWORK_TYPE_HSUPA:
				return "HSUPA";
			case NETWORK_TYPE_HSPA:
				return "HSPA";
			case NETWORK_TYPE_CDMA:
				return "CDMA";
			case NETWORK_TYPE_EVDO_0:
				return "CDMA - EvDo rev. 0";
			case NETWORK_TYPE_EVDO_A:
				return "CDMA - EvDo rev. A";
			case NETWORK_TYPE_EVDO_B:
				return "CDMA - EvDo rev. B";
			case NETWORK_TYPE_1xRTT:
				return "CDMA - 1xRTT";
			case NETWORK_TYPE_LTE:
				return "LTE";
			case NETWORK_TYPE_EHRPD:
				return "CDMA - eHRPD";
			case NETWORK_TYPE_IDEN:
				return "iDEN";
			case NETWORK_TYPE_HSPAP:
				return "HSPA+";
			case NETWORK_TYPE_GSM:
				return "GSM";
			case NETWORK_TYPE_TD_SCDMA:
				return "TD_SCDMA";
			case NETWORK_TYPE_IWLAN:
				return "IWLAN";
			case NETWORK_TYPE_LTE_CA:
				return "LTE_CA";
			case NETWORK_TYPE_NR:
				return "NR";
			case NETWORK_TYPE_UNKNOWN:
				return "UNKNOWN";
			default:
				return "UNKNOWN(" + type + ")";
		}
	}

	/* AOSP: PhoneConstants.STATE_KEY = "state", PhoneConstants.State.X.toString() */
	public static final String EXTRA_STATE = "state";
	public static final String EXTRA_STATE_IDLE = "IDLE";
	public static final String EXTRA_STATE_RINGING = "RINGING";
	public static final String EXTRA_STATE_OFFHOOK = "OFFHOOK";

	public static TelephonyManager getDefault() {
		return new TelephonyManager();
	}

	/* AOSP TelephonyManager with getITelephony() == null (a device without telephony service) */
	private int mSubId = SubscriptionManager.DEFAULT_SUBSCRIPTION_ID;

	public TelephonyManager createForSubscriptionId(int subId) {
		TelephonyManager tm = new TelephonyManager();
		tm.mSubId = subId;
		return tm;
	}

	/* AOSP getSubscriptionId() / getSubId() */
	public int getSubscriptionId() {
		if (SubscriptionManager.isUsableSubIdValue(mSubId)) {
			return mSubId;
		}
		return SubscriptionManager.getDefaultSubscriptionId();
	}

	public static final int DATA_ACTIVITY_NONE = 0x00000000;
	public static final int DATA_ACTIVITY_IN = 0x00000001;
	public static final int DATA_ACTIVITY_OUT = 0x00000002;
	public static final int DATA_ACTIVITY_INOUT = 0x00000003;
	public static final int DATA_ACTIVITY_DORMANT = 0x00000004;

	/* getITelephony() == null: DATA_ACTIVITY_NONE */
	public int getDataActivity() {
		return DATA_ACTIVITY_NONE;
	}

	/* getITelephony() == null: returns without doing anything */
	public void sendDialerSpecialCode(String inputCode) {}

	/* getImei(getSlotIndex()): no ITelephony, null */
	public String getImei() {
		return null;
	}

	public int getCallStateForSubscription() {
		return 0; // getCallState(subId): CALL_STATE_IDLE
	}

	public int getDataState() {
		return 0; // DATA_DISCONNECTED
	}

	/* AOSP with getITelephony() == null */
	public SignalStrength getSignalStrength() {
		return null;
	}

	public List<CellInfo> getAllCellInfo() {
		return null;
	}

	public abstract static class CellInfoCallback {
		public static final int ERROR_TIMEOUT = 1;
		public static final int ERROR_MODEM_ERROR = 2;

		public abstract void onCellInfo(List<CellInfo> cellInfo);

		public void onError(int errorCode, Throwable detail) {}
	}

	/* AOSP: with telephony null, ISE for apps targeting after R (NULL_TELEPHONY_THROW_NO_CB), silent return
	 * before. ATL does not keep the caller's target SDK here, so the modern behaviour is used. */
	public void requestCellInfoUpdate(java.util.concurrent.Executor executor, CellInfoCallback callback) {
		throw new IllegalStateException("Telephony is null");
	}

	public String getMmsUserAgent() {
		return null;
	}

	public String getMmsUAProfUrl() {
		return null;
	}

	// FIXME: can we return null instead of ""?
	public String getNetworkOperator() {
		return "";
	}

	public String getNetworkOperatorName() {
		return "";
	}

	public String getSimOperator() {
		return "";
	}

	public String getSubscriberId() {
		return "";
	}

	/* TelephonyManager.java: PhoneConstants values; getCurrentPhoneType() is always GSM on Android 16 */
	public static final int PHONE_TYPE_NONE = 0;
	public static final int PHONE_TYPE_GSM = 1;
	public static final int PHONE_TYPE_CDMA = 2;
	public static final int PHONE_TYPE_SIP = 3;
	public static final int PHONE_TYPE_IMS = 4;

	public int getCurrentPhoneType() {
		return PHONE_TYPE_GSM;
	}

	public int getCurrentPhoneType(int subId) {
		return PHONE_TYPE_GSM;
	}

	public int getPhoneType() {
		return 0; // PHONE_TYPE_NONE
	}

	public String getNetworkCountryIso() { return ""; }
	public String getSimCountryIso() { return ""; }

	public List getNeighboringCellInfo() {
		return new ArrayList(0);
	}

	public CellLocation getCellLocation() {
		return null; // getITelephony() == null: AOSP returns null
	}

	public boolean isNetworkRoaming() {
		return false;
	}

	public void listen(PhoneStateListener listener, int events) {}

	/* AOSP TelephonyManager.registerTelephonyCallback(): ATL has no modem, so as on a device without telephony
	 * the callback never hears of a change */
	public void registerTelephonyCallback(java.util.concurrent.Executor executor, TelephonyCallback callback) {
		if (executor == null || callback == null) {
			throw new IllegalArgumentException("TelephonyCallback and executor must be non-null");
		}
	}

	/* AOSP TelephonyManager.registerTelephonyCallback(int, Executor, TelephonyCallback) (API 33): same as above,
	 * includeLocationData only selects which location fields a modem would report */
	public void registerTelephonyCallback(int includeLocationData, java.util.concurrent.Executor executor, TelephonyCallback callback) {
		registerTelephonyCallback(executor, callback);
	}

	public void unregisterTelephonyCallback(TelephonyCallback callback) {}

	public int getNetworkType() {
		return 0; // NETWORK_TYPE_UNKNOWN
	}

	/* AOSP TelephonyManager.getDataNetworkType(): no mobile data network */
	public int getDataNetworkType() {
		return 0; // NETWORK_TYPE_UNKNOWN
	}

	public String getLine1Number() {
		return null;
	}

	/* AOSP getSimStateIncludingLoaded(): without a SIM slot (getSlotIndex() < 0, getPhoneCount() 0 on a device
	 * that is neither voice, SMS nor data capable) all SIMs count as absent */
	public int getSimState() {
		return 1; // SIM_STATE_ABSENT
	}

	/* AOSP TelephonyManager.getSimState(int) -> getSimStateForSlotIndex(int): SIM_STATE_UNKNOWN when
	 * getITelephony() is null, as on ATL */
	public int getSimState(int slotIndex) {
		return 0; // SIM_STATE_UNKNOWN
	}

	/* AOSP TelephonyManager.getPhoneCount() -> getActiveModemCount(): multisim config UNKNOWN and neither voice,
	 * SMS nor data capable, so 0 */
	public int getPhoneCount() {
		return 0;
	}

	/* AOSP getSimOperatorNameForPhone(): the property icc_operator_alpha, "" when not set (no SIM) */
	public String getSimOperatorName() {
		return "";
	}

	public int getCallState() {
		return 0; // CALL_STATE_IDLE
	}

	/* AOSP TelephonyManager.getDeviceId(): null when there is no ITelephony service */
	public String getDeviceId() {
		return null;
	}

	/* AOSP getServiceStateForSlot(): null when getITelephony() is null */
	public ServiceState getServiceState() {
		return null;
	}

	/* AOSP getSimCarrierIdName(): null without ITelephony */
	public CharSequence getSimCarrierIdName() {
		return null;
	}

	/* AOSP getSimSerialNumber(): null without IPhoneSubInfo */
	public String getSimSerialNumber() {
		return null;
	}

	/* AOSP isSmsCapable() -> hasCapability(): no FEATURE_TELEPHONY_MESSAGING on ATL, so a device without SMS */
	public boolean isSmsCapable() {
		return false;
	}

	/* AOSP TelephonyManager members with getITelephony() == null / getIOns() == null (no telephony service on the
	 * desktop): each follows the null branch of the AOSP method (default value, IllegalStateException or no-op) */
	public static final int NETWORK_SELECTION_MODE_UNKNOWN = 0;
	public static final int NETWORK_SELECTION_MODE_AUTO = 1;
	public static final int NETWORK_SELECTION_MODE_MANUAL = 2;
	public static final int UNINITIALIZED_CARD_ID = -2;
	public static final int UNKNOWN_CARRIER_ID = -1;
	public static final int ALLOWED_NETWORK_TYPES_REASON_USER = 0;
	public static final int ALLOWED_NETWORK_TYPES_REASON_POWER = 1;
	public static final int ALLOWED_NETWORK_TYPES_REASON_CARRIER = 2;
	public static final int ALLOWED_NETWORK_TYPES_REASON_TEST = 4;
	public static final int CALL_COMPOSER_STATUS_OFF = 0;
	public static final int DATA_ENABLED_REASON_USER = 0;
	public static final long NETWORK_TYPE_BITMASK_UNKNOWN = 0L;
	public static final int MULTISIM_NOT_SUPPORTED_BY_HARDWARE = 1;

	private int getSubId() {
		return getSubscriptionId();
	}

	private int getSlotIndex() {
		return SubscriptionManager.getSlotIndex(getSubId());
	}

	public String getNetworkCountryIso(int slotIndex) {
		/* AOSP: the slot must be valid (or DEFAULT_SIM_SLOT_INDEX = Integer.MAX_VALUE), else IllegalArgumentException;
		 * without ITelephony "" */
		if (slotIndex != Integer.MAX_VALUE && !(slotIndex >= 0 && slotIndex < getPhoneCount()))
			throw new IllegalArgumentException("invalid slot index " + slotIndex);
		return "";
	}
	public String getDeviceId(int slotIndex) { return null; }
	public String getImei(int slotIndex) { return null; }
	public String getMeid() { return null; }
	public String getMeid(int slotIndex) { return null; }
	public String getManufacturerCode() { return null; }
	public String getManufacturerCode(int slotIndex) { return null; }
	public String getTypeAllocationCode() { return getTypeAllocationCode(getSlotIndex()); }
	public String getTypeAllocationCode(int slotIndex) { return null; }
	public String getDeviceSoftwareVersion() { return getDeviceSoftwareVersion(getSlotIndex()); }
	public String getDeviceSoftwareVersion(int slotIndex) { return null; }
	public String getPrimaryImei() { throw new IllegalStateException("Telephony service not available."); }
	public String getNai() { return null; }
	public String getGroupIdLevel1() { return null; }
	public String getNetworkSpecifier() { return String.valueOf(getSubId()); }
	public int getSubscriptionId(android.telecom.PhoneAccountHandle phoneAccountHandle) {
		/* AOSP: ITelephony null -> INVALID_SUBSCRIPTION_ID */
		return SubscriptionManager.INVALID_SUBSCRIPTION_ID;
	}
	public android.telecom.PhoneAccountHandle getPhoneAccountHandle() { return null; }
	public TelephonyManager createForPhoneAccountHandle(android.telecom.PhoneAccountHandle phoneAccountHandle) {
		int subId = getSubscriptionId(phoneAccountHandle);
		if (!SubscriptionManager.isValidSubscriptionId(subId))
			return null;
		return createForSubscriptionId(subId);
	}
	public ServiceState getServiceState(int includeLocationData) { return null; }
	public boolean canChangeDtmfToneLength() { return false; }
	public void clearSignalStrengthUpdateRequest(SignalStrengthUpdateRequest request) {
		java.util.Objects.requireNonNull(request, "request must not be null");
	}
	public void setSignalStrengthUpdateRequest(SignalStrengthUpdateRequest request) {
		java.util.Objects.requireNonNull(request, "request must not be null");
	}
	public boolean doesSwitchMultiSimConfigTriggerReboot() { return false; }
	public void switchMultiSimConfig(int numOfSims) {}
	public int getActiveModemCount() { return getPhoneCount(); }
	public int getSupportedModemCount() { return getActiveModemCount(); }
	public int isMultiSimSupported() { return MULTISIM_NOT_SUPPORTED_BY_HARDWARE; }
	public boolean isModemEnabledForSlot(int slotIndex) { return false; }
	public void rebootModem() { throw new IllegalStateException("telephony service is null."); }
	private static boolean isValidAllowedNetworkTypesReason(int reason) {
		return reason >= ALLOWED_NETWORK_TYPES_REASON_USER && reason <= ALLOWED_NETWORK_TYPES_REASON_TEST;
	}
	public long getAllowedNetworkTypesForReason(int reason) {
		if (!isValidAllowedNetworkTypesReason(reason))
			throw new IllegalArgumentException("invalid AllowedNetworkTypesReason.");
		throw new IllegalStateException("telephony service is null.");
	}
	public void setAllowedNetworkTypesForReason(int reason, long allowedNetworkTypes) {
		if (!isValidAllowedNetworkTypesReason(reason))
			throw new IllegalArgumentException("invalid AllowedNetworkTypesReason.");
		throw new IllegalStateException("telephony service is null.");
	}
	public long getSupportedRadioAccessFamily() { return NETWORK_TYPE_BITMASK_UNKNOWN; }
	public int getCallComposerStatus() { return CALL_COMPOSER_STATUS_OFF; }
	public void setCallComposerStatus(int status) {
		if (status > 2 || status < CALL_COMPOSER_STATUS_OFF)
			throw new IllegalArgumentException("requested status is invalid");
	}
	public static long getMaximumCallComposerPictureSize() { return 80000; }
	public int getCardIdForDefaultEuicc() { return UNINITIALIZED_CARD_ID; }
	public java.util.List<UiccCardInfo> getUiccCardsInfo() { return new java.util.ArrayList<UiccCardInfo>(); }
	public int getCarrierIdFromSimMccMnc() { return UNKNOWN_CARRIER_ID; }
	public int getSimCarrierId() { return UNKNOWN_CARRIER_ID; }
	public int getSimSpecificCarrierId() { return UNKNOWN_CARRIER_ID; }
	public CharSequence getSimSpecificCarrierIdName() { return null; }
	public void getCarrierRestrictionStatus(java.util.concurrent.Executor executor, java.util.function.Consumer<Integer> resultListener) {
		java.util.Objects.requireNonNull(executor);
		java.util.Objects.requireNonNull(resultListener);
	}
	public java.util.Map<Integer, java.util.List<android.telephony.emergency.EmergencyNumber>> getEmergencyNumberList() {
		throw new IllegalStateException("telephony service is null.");
	}
	public boolean isEmergencyNumber(String number) { throw new IllegalStateException("telephony service is null."); }
	public java.util.List<String> getEquivalentHomePlmns() { return java.util.Collections.emptyList(); }
	public String[] getForbiddenPlmns() { return null; }
	public int setForbiddenPlmns(java.util.List<String> fplmns) { return -1; }
	public String getIccAuthentication(int appType, int authType, String data) { return null; }
	public String getManualNetworkSelectionPlmn() { return ""; }
	public int getNetworkSelectionMode() { return NETWORK_SELECTION_MODE_UNKNOWN; }
	public boolean isManualNetworkSelectionAllowed() { return true; }
	public void setNetworkSelectionModeAutomatic() {}
	public boolean setNetworkSelectionModeManual(String operatorNumeric, boolean persistSelection) { return false; }
	public boolean setNetworkSelectionModeManual(String operatorAlphaLong, boolean persistSelection, int ran) { return false; }
	public int getPreferredOpportunisticDataSubscription() { return SubscriptionManager.INVALID_SUBSCRIPTION_ID; }
	public String getVisualVoicemailPackageName() { return null; }
	public String getVoiceMailAlphaTag() { return null; }
	public String getVoiceMailNumber() { return null; }
	public boolean setVoiceMailNumber(String alphaTag, String number) { return false; }
	public int getVoiceNetworkType() { return NETWORK_TYPE_UNKNOWN; }
	public android.net.Uri getVoicemailRingtoneUri(android.telecom.PhoneAccountHandle accountHandle) { return null; }
	public void setVoicemailRingtoneUri(android.telecom.PhoneAccountHandle phoneAccountHandle, android.net.Uri uri) {}
	public boolean isVoicemailVibrationEnabled(android.telecom.PhoneAccountHandle accountHandle) { return false; }
	public void setVoicemailVibrationEnabled(android.telecom.PhoneAccountHandle phoneAccountHandle, boolean enabled) {}
	public boolean hasCarrierPrivileges() { return false; }
	public boolean hasIccCard() { return false; }
	public IccOpenLogicalChannelResponse iccOpenLogicalChannel(String AID) { return null; }
	public IccOpenLogicalChannelResponse iccOpenLogicalChannel(String AID, int p2) { return null; }
	public boolean iccCloseLogicalChannel(int channel) { return false; }
	public String iccTransmitApduLogicalChannel(int channel, int cla, int instruction, int p1, int p2, int p3, String data) { return ""; }
	public String iccTransmitApduBasicChannel(int cla, int instruction, int p1, int p2, int p3, String data) { return ""; }
	public byte[] iccExchangeSimIO(int fileID, int command, int p1, int p2, int p3, String filePath) { return null; }
	public String sendEnvelopeWithStatus(String content) { return ""; }
	public boolean isConcurrentVoiceAndDataSupported() { return false; }
	public boolean isDataCapable() { return false; }
	public boolean isDataConnectionAllowed() { return false; }
	public boolean isDataEnabled() { return isDataEnabledForReason(DATA_ENABLED_REASON_USER); }
	public boolean isDataEnabledForReason(int reason) { return false; }
	public void setDataEnabled(boolean enable) {}
	public void setDataEnabledForReason(int reason, boolean enable) {}
	public boolean isDataRoamingEnabled() { return false; }
	public boolean isDeviceSmsCapable() { return isSmsCapable(); }
	public boolean isDeviceVoiceCapable() { return false; }
	public boolean isVoiceCapable() { return false; }
	public boolean isHearingAidCompatibilitySupported() { return false; }
	public boolean isRttSupported() { return false; }
	public boolean isTtyModeSupported() { return false; }
	public boolean isWorldPhone() { return false; }
	public boolean isRadioInterfaceCapabilitySupported(String capability) {
		if (capability == null)
			return false;
		throw new IllegalStateException("telephony service is null.");
	}
	public boolean isPremiumCapabilityAvailableForPurchase(int capability) {
		throw new IllegalStateException("telephony service is null.");
	}
	public void purchasePremiumCapability(int capability, java.util.concurrent.Executor executor, java.util.function.Consumer<Integer> callback) {
		java.util.Objects.requireNonNull(executor);
		java.util.Objects.requireNonNull(callback);
		callback.accept(11 /* PURCHASE_PREMIUM_CAPABILITY_RESULT_REQUEST_FAILED */);
	}
	public boolean setLine1NumberForDisplay(String alphaTag, String number) { return false; }
	public boolean setOperatorBrandOverride(String brand) { return false; }
	public boolean setPreferredNetworkTypeToGlobal() { return false; }
	public void setVisualVoicemailSmsFilterSettings(VisualVoicemailSmsFilterSettings settings) {}
	public void sendVisualVoicemailSms(String number, int port, String text, android.app.PendingIntent sentIntent) {}
	public NetworkScan requestNetworkScan(NetworkScanRequest request, java.util.concurrent.Executor executor, TelephonyScanManager.NetworkScanCallback callback) {
		return requestNetworkScan(2 /* INCLUDE_LOCATION_DATA_FINE */, request, executor, callback);
	}
	public NetworkScan requestNetworkScan(int includeLocationData, NetworkScanRequest request, java.util.concurrent.Executor executor, TelephonyScanManager.NetworkScanCallback callback) {
		java.util.Objects.requireNonNull(request, "Request was null");
		java.util.Objects.requireNonNull(callback, "Callback was null");
		java.util.Objects.requireNonNull(executor, "Executor was null");
		return null;
	}

	/* AOSP TelephonyManager.getSubIdForPhoneAccount() (@hide) */
	public int getSubIdForPhoneAccount(android.telecom.PhoneAccount phoneAccount) {
		int retval = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
		if (phoneAccount != null && phoneAccount.hasCapabilities(android.telecom.PhoneAccount.CAPABILITY_SIM_SUBSCRIPTION)) {
			retval = getSubscriptionId(phoneAccount.getAccountHandle());
		}
		return retval;
	}
}
