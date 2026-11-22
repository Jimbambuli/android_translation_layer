package android.telephony;

import com.android.internal.telephony.ModemManagerPhone;

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
		ModemManagerPhone phone = ModemManagerPhone.get();
		if (phone != null)
			return phone.getDataState();
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
		ModemManagerPhone phone = ModemManagerPhone.get();
		if (phone != null)
			return phone.getNetworkOperatorNumeric(); /* the operator_numeric property ServiceStateTracker sets */
		return "";
	}

	public String getNetworkOperatorName() {
		ModemManagerPhone phone = ModemManagerPhone.get();
		if (phone != null)
			return phone.getNetworkOperatorName(); /* operator_alpha */
		return "";
	}

	public String getSimOperator() {
		return getSimOperatorNumeric();
	}

	/* AOSP getSimOperatorNumeric(): the property icc_operator_numeric of the phone, "" without a SIM */
	public String getSimOperatorNumeric() {
		ModemManagerPhone phone = ModemManagerPhone.get();
		if (phone != null)
			return phone.getSimOperatorNumeric();
		return "";
	}

	public String getSubscriberId() {
		ModemManagerPhone phone = ModemManagerPhone.get();
		if (phone != null)
			return checkReadDeviceIdentifiers("getSubscriberId") ? phone.getImsi() : null;
		return "";
	}

	/* TelephonyManager.java: PhoneConstants values; getCurrentPhoneType() is always GSM on Android 16 */
	public static final int PHONE_TYPE_NONE = 0;
	public static final int PHONE_TYPE_GSM = 1;
	public static final int PHONE_TYPE_CDMA = 2;
	public static final int PHONE_TYPE_SIP = 3;
	public static final int PHONE_TYPE_IMS = 4;

	public int getCurrentPhoneType() {
		ModemManagerPhone phone = ModemManagerPhone.get();
		if (phone != null)
			return phone.isCdma() && !phone.isGsm() ? PHONE_TYPE_CDMA : PHONE_TYPE_GSM;
		return PHONE_TYPE_GSM;
	}

	public int getCurrentPhoneType(int subId) {
		return getCurrentPhoneType();
	}

	/* AOSP: PHONE_TYPE_NONE unless the device can call or has mobile data */
	public int getPhoneType() {
		if (!isDeviceVoiceCapable() && !isDataCapable())
			return PHONE_TYPE_NONE;
		return getCurrentPhoneType();
	}

	public String getNetworkCountryIso() {
		ModemManagerPhone phone = ModemManagerPhone.get();
		return phone != null ? phone.getNetworkCountryIso() : "";
	}
	/* AOSP: icc_operator_iso_country, from the SIM's MCC (MccTable) */
	public String getSimCountryIso() {
		ModemManagerPhone phone = ModemManagerPhone.get();
		return phone != null ? phone.getSimCountryIso() : "";
	}

	public List getNeighboringCellInfo() {
		return new ArrayList(0);
	}

	public CellLocation getCellLocation() {
		return null; // getITelephony() == null: AOSP returns null
	}

	public boolean isNetworkRoaming() {
		ModemManagerPhone phone = ModemManagerPhone.get();
		if (phone != null)
			return phone.isRoaming();
		return false;
	}

	/* AOSP listen() -> TelephonyRegistry: the current state right away, then each change, on the listener's
	 * executor; ATL reports service state, call state (always idle: calls go to the desktop's dialer) and data
	 * connection state of ModemManager's modem */
	public void listen(final PhoneStateListener listener, final int events) {
		if (listener == null)
			return;
		Runnable old = sPhoneStateListeners.remove(listener);
		if (old != null)
			ModemManagerPhone.removeListener(old);
		if (events == PhoneStateListener.LISTEN_NONE || ModemManagerPhone.get() == null || listener.mExecutor == null)
			return;
		Runnable notify = new Runnable() {
			private ServiceState lastService;
			private int lastData = -1;
			private int lastType = -1;
			private boolean first = true;

			public void run() {
				ModemManagerPhone phone = ModemManagerPhone.get();
				final ServiceState ss = phone != null ? phone.getServiceState() : new ServiceState();
				final int data = phone != null ? phone.getDataState() : DATA_DISCONNECTED;
				final int type = phone != null ? phone.getNetworkType() : NETWORK_TYPE_UNKNOWN;
				if ((events & PhoneStateListener.LISTEN_SERVICE_STATE) != 0 && (first || !ss.equals(lastService)))
					listener.mExecutor.execute(() -> listener.onServiceStateChanged(ss));
				if ((events & PhoneStateListener.LISTEN_DATA_CONNECTION_STATE) != 0 && (first || data != lastData || type != lastType))
					listener.mExecutor.execute(() -> {
						listener.onDataConnectionStateChanged(data);
						listener.onDataConnectionStateChanged(data, type);
					});
				if ((events & PhoneStateListener.LISTEN_CALL_STATE) != 0 && first)
					listener.mExecutor.execute(() -> listener.onCallStateChanged(CALL_STATE_IDLE, ""));
				lastService = ss;
				lastData = data;
				lastType = type;
				first = false;
			}
		};
		sPhoneStateListeners.put(listener, notify);
		ModemManagerPhone.addListener(notify);
		notify.run();
	}

	private static final java.util.Map<Object, Runnable> sPhoneStateListeners = new java.util.concurrent.ConcurrentHashMap<>();

	/* AOSP TelephonyManager.registerTelephonyCallback(): ATL has no modem, so as on a device without telephony
	 * the callback never hears of a change */
	public void registerTelephonyCallback(final java.util.concurrent.Executor executor, final TelephonyCallback callback) {
		if (executor == null || callback == null) {
			throw new IllegalArgumentException("TelephonyCallback and executor must be non-null");
		}
		Runnable old = sPhoneStateListeners.remove(callback);
		if (old != null)
			ModemManagerPhone.removeListener(old);
		if (ModemManagerPhone.get() == null)
			return;
		/* AOSP TelephonyRegistry: the current state right away, then each change */
		Runnable notify = new Runnable() {
			private ServiceState lastService;
			private int lastData = -1;
			private int lastType = -1;
			private boolean first = true;

			public void run() {
				ModemManagerPhone phone = ModemManagerPhone.get();
				final ServiceState ss = phone != null ? phone.getServiceState() : new ServiceState();
				final int data = phone != null ? phone.getDataState() : DATA_DISCONNECTED;
				final int type = phone != null ? phone.getNetworkType() : NETWORK_TYPE_UNKNOWN;
				if (callback instanceof TelephonyCallback.ServiceStateListener && (first || !ss.equals(lastService)))
					executor.execute(() -> ((TelephonyCallback.ServiceStateListener)callback).onServiceStateChanged(ss));
				if (callback instanceof TelephonyCallback.DataConnectionStateListener && (first || data != lastData || type != lastType))
					executor.execute(() -> ((TelephonyCallback.DataConnectionStateListener)callback).onDataConnectionStateChanged(data, type));
				if (callback instanceof TelephonyCallback.CallStateListener && first)
					executor.execute(() -> ((TelephonyCallback.CallStateListener)callback).onCallStateChanged(CALL_STATE_IDLE));
				lastService = ss;
				lastData = data;
				lastType = type;
				first = false;
			}
		};
		sPhoneStateListeners.put(callback, notify);
		ModemManagerPhone.addListener(notify);
		notify.run();
	}

	/* AOSP TelephonyManager.registerTelephonyCallback(int, Executor, TelephonyCallback) (API 33): same as above,
	 * includeLocationData only selects which location fields a modem would report */
	public void registerTelephonyCallback(int includeLocationData, java.util.concurrent.Executor executor, TelephonyCallback callback) {
		registerTelephonyCallback(executor, callback);
	}

	public void unregisterTelephonyCallback(TelephonyCallback callback) {
		Runnable old = callback != null ? sPhoneStateListeners.remove(callback) : null;
		if (old != null)
			ModemManagerPhone.removeListener(old);
	}

	public int getNetworkType() {
		ModemManagerPhone phone = ModemManagerPhone.get();
		if (phone != null)
			return phone.getNetworkType();
		return 0; // NETWORK_TYPE_UNKNOWN
	}

	/* AOSP TelephonyManager.getDataNetworkType(): no mobile data network */
	public int getDataNetworkType() {
		ModemManagerPhone phone = ModemManagerPhone.get();
		if (phone != null)
			return phone.getNetworkType();
		return 0; // NETWORK_TYPE_UNKNOWN
	}

	/* AOSP: TelephonyPermissions.checkReadPhoneNumber() (READ_PHONE_NUMBERS or READ_SMS; READ_PHONE_STATE before R),
	 * else SecurityException; the number the SIM/modem knows (ModemManager OwnNumbers) */
	public String getLine1Number() {
		ModemManagerPhone phone = ModemManagerPhone.get();
		if (phone != null) {
			android.content.Context context = android.app.ActivityThread.currentApplication();
			boolean legacy = context.getApplicationInfo().targetSdkVersion < android.os.Build.VERSION_CODES.R
			    && context.checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE) == android.content.pm.PackageManager.PERMISSION_GRANTED;
			if (!legacy && context.checkSelfPermission(android.Manifest.permission.READ_PHONE_NUMBERS) != android.content.pm.PackageManager.PERMISSION_GRANTED
			    && context.checkSelfPermission(android.Manifest.permission.READ_SMS) != android.content.pm.PackageManager.PERMISSION_GRANTED)
				throw new SecurityException("getLine1NumberForDisplay: Neither user " + android.os.Process.myUid()
				                            + " nor current process has android.permission.READ_PHONE_STATE, android.permission.READ_SMS, or android.permission.READ_PHONE_NUMBERS");
			return phone.getLine1Number();
		}
		return null;
	}

	/* AOSP getSimStateIncludingLoaded(): without a SIM slot (getSlotIndex() < 0, getPhoneCount() 0 on a device
	 * that is neither voice, SMS nor data capable) all SIMs count as absent */
	public int getSimState() {
		ModemManagerPhone phone = ModemManagerPhone.get();
		if (phone != null) {
			int simState = phone.getSimState();
			return simState == SIM_STATE_LOADED ? SIM_STATE_READY : simState;
		}
		return 1; // SIM_STATE_ABSENT
	}

	/* AOSP TelephonyManager.getSimState(int) -> getSimStateForSlotIndex(int): SIM_STATE_UNKNOWN when
	 * getITelephony() is null, as on ATL */
	public int getSimState(int slotIndex) {
		ModemManagerPhone phone = ModemManagerPhone.get();
		if (phone != null && slotIndex == ModemManagerPhone.SLOT_INDEX) {
			int simState = phone.getSimState();
			return simState == SIM_STATE_LOADED ? SIM_STATE_READY : simState;
		}
		return 0; // SIM_STATE_UNKNOWN
	}

	/* AOSP TelephonyManager.getPhoneCount() -> getActiveModemCount(): multisim config UNKNOWN and neither voice,
	 * SMS nor data capable, so 0 */
	public int getPhoneCount() {
		if (ModemManagerPhone.get() != null)
			return 1;
		return 0;
	}

	/* AOSP getSimOperatorNameForPhone(): the property icc_operator_alpha, "" when not set (no SIM) */
	public String getSimOperatorName() {
		ModemManagerPhone phone = ModemManagerPhone.get();
		if (phone != null)
			return phone.getSimOperatorName();
		return "";
	}

	public int getCallState() {
		return 0; // CALL_STATE_IDLE
	}

	/* AOSP TelephonyManager.getDeviceId(): null when there is no ITelephony service */
	public String getDeviceId() {
		ModemManagerPhone phone = ModemManagerPhone.get();
		if (phone != null)
			return checkReadDeviceIdentifiers("getDeviceId") ? phone.getImei() : null;
		return null;
	}

	/* AOSP getServiceStateForSlot(): null when getITelephony() is null */
	public ServiceState getServiceState() {
		ModemManagerPhone phone = ModemManagerPhone.get();
		if (phone != null)
			return phone.getServiceState();
		return null;
	}

	/* AOSP getSimCarrierIdName(): null without ITelephony */
	public CharSequence getSimCarrierIdName() {
		return null;
	}

	/* AOSP getSimSerialNumber(): null without IPhoneSubInfo */
	public String getSimSerialNumber() {
		ModemManagerPhone phone = ModemManagerPhone.get();
		if (phone != null)
			return checkReadDeviceIdentifiers("getSimSerialNumber") ? phone.getIccId() : null;
		return null;
	}

	/* AOSP isSmsCapable() -> hasCapability(): no FEATURE_TELEPHONY_MESSAGING on ATL, so a device without SMS */
	public boolean isSmsCapable() {
		ModemManagerPhone phone = ModemManagerPhone.get();
		return phone != null && phone.hasMessaging();
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
		ModemManagerPhone phone = ModemManagerPhone.get();
		return phone != null ? phone.getNetworkCountryIso() : "";
	}
	public String getDeviceId(int slotIndex) {
		if (ModemManagerPhone.get() != null && slotIndex == ModemManagerPhone.SLOT_INDEX)
			return getDeviceId();
		return null;
	}
	public String getImei(int slotIndex) {
		ModemManagerPhone phone = ModemManagerPhone.get();
		if (phone != null && slotIndex == ModemManagerPhone.SLOT_INDEX)
			return checkReadDeviceIdentifiers("getImeiForSlot") ? phone.getImei() : null;
		return null;
	}

	/* AOSP TelephonyPermissions.checkCallingOrSelfReadDeviceIdentifiers(): only privileged apps (READ_PRIVILEGED_PHONE_STATE,
	 * carrier privileges); a normal app gets a SecurityException from Q on, before Q null with READ_PHONE_STATE */
	private static boolean checkReadDeviceIdentifiers(String message) {
		android.content.Context context = android.app.ActivityThread.currentApplication();
		if (context.getApplicationInfo().targetSdkVersion < android.os.Build.VERSION_CODES.Q
		    && context.checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE) == android.content.pm.PackageManager.PERMISSION_GRANTED)
			return false;
		throw new SecurityException(message + ": The uid " + android.os.Process.myUid() + " does not meet the requirements to access device identifiers.");
	}
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
	public boolean isModemEnabledForSlot(int slotIndex) {
		ModemManagerPhone phone = ModemManagerPhone.get();
		return phone != null && slotIndex == ModemManagerPhone.SLOT_INDEX && !phone.isRadioOff();
	}
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
	/* AOSP: READ_PHONE_STATE; the numbers of each phone's subscription (EmergencyNumberTracker) */
	public java.util.Map<Integer, java.util.List<android.telephony.emergency.EmergencyNumber>> getEmergencyNumberList() {
		ModemManagerPhone phone = ModemManagerPhone.get();
		if (phone != null) {
			java.util.Map<Integer, java.util.List<android.telephony.emergency.EmergencyNumber>> map = new java.util.HashMap<>();
			int subId = SubscriptionManager.getSubscriptionId(ModemManagerPhone.SLOT_INDEX);
			map.put(SubscriptionManager.isValidSubscriptionId(subId) ? subId : SubscriptionManager.INVALID_SUBSCRIPTION_ID, phone.getEmergencyNumbers());
			return map;
		}
		throw new IllegalStateException("telephony service is null.");
	}
	/* AOSP getEmergencyNumberList(int): no telephony service, as for the no-argument variant */
	public java.util.Map<Integer, java.util.List<android.telephony.emergency.EmergencyNumber>> getEmergencyNumberList(int categories) {
		if (ModemManagerPhone.get() != null) {
			/* AOSP: the numbers in one of the categories, or with no category (UNSPECIFIED counts for all) */
			java.util.Map<Integer, java.util.List<android.telephony.emergency.EmergencyNumber>> map = getEmergencyNumberList();
			for (java.util.List<android.telephony.emergency.EmergencyNumber> list : map.values())
				list.removeIf(n -> !(n.isInEmergencyServiceCategories(categories)));
			return map;
		}
		throw new IllegalStateException("telephony service is null.");
	}
	/* AOSP: ITelephony.isEmergencyNumber(number, exactMatch true): one of the emergency numbers */
	public boolean isEmergencyNumber(String number) {
		ModemManagerPhone phone = ModemManagerPhone.get();
		if (phone == null)
			throw new IllegalStateException("telephony service is null.");
		if (number == null)
			throw new IllegalArgumentException("Number should be non-null");
		String n = PhoneNumberUtils.extractNetworkPortion(number);
		for (android.telephony.emergency.EmergencyNumber e : phone.getEmergencyNumbers())
			if (e.getNumber().equals(n))
				return true;
		return false;
	}
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
	public int getVoiceNetworkType() {
		ModemManagerPhone phone = ModemManagerPhone.get();
		if (phone != null && phone.getVoiceRegState() == ServiceState.STATE_IN_SERVICE)
			return phone.getNetworkType();
		return NETWORK_TYPE_UNKNOWN;
	}
	public android.net.Uri getVoicemailRingtoneUri(android.telecom.PhoneAccountHandle accountHandle) { return null; }
	public void setVoicemailRingtoneUri(android.telecom.PhoneAccountHandle phoneAccountHandle, android.net.Uri uri) {}
	public boolean isVoicemailVibrationEnabled(android.telecom.PhoneAccountHandle accountHandle) { return false; }
	public void setVoicemailVibrationEnabled(android.telecom.PhoneAccountHandle phoneAccountHandle, boolean enabled) {}
	public boolean hasCarrierPrivileges() { return false; }
	public boolean hasIccCard() {
		ModemManagerPhone phone = ModemManagerPhone.get();
		return phone != null && phone.hasSim();
	}
	public IccOpenLogicalChannelResponse iccOpenLogicalChannel(String AID) { return null; }
	public IccOpenLogicalChannelResponse iccOpenLogicalChannel(String AID, int p2) { return null; }
	public boolean iccCloseLogicalChannel(int channel) { return false; }
	public String iccTransmitApduLogicalChannel(int channel, int cla, int instruction, int p1, int p2, int p3, String data) { return ""; }
	public String iccTransmitApduBasicChannel(int cla, int instruction, int p1, int p2, int p3, String data) { return ""; }
	public byte[] iccExchangeSimIO(int fileID, int command, int p1, int p2, int p3, String filePath) { return null; }
	public String sendEnvelopeWithStatus(String content) { return ""; }
	public boolean isConcurrentVoiceAndDataSupported() { return false; }
	public boolean isDataCapable() {
		ModemManagerPhone phone = ModemManagerPhone.get();
		return phone != null && phone.isDataCapable();
	}
	public boolean isDataConnectionAllowed() { return false; }
	public boolean isDataEnabled() { return isDataEnabledForReason(DATA_ENABLED_REASON_USER); }
	public boolean isDataEnabledForReason(int reason) { return false; }
	public void setDataEnabled(boolean enable) {}
	public void setDataEnabledForReason(int reason, boolean enable) {}
	public boolean isDataRoamingEnabled() { return false; }
	public boolean isDeviceSmsCapable() { return isSmsCapable(); }
	/* AOSP: FEATURE_TELEPHONY_CALLING (ModemManager's Voice interface) */
	public boolean isDeviceVoiceCapable() {
		ModemManagerPhone phone = ModemManagerPhone.get();
		return phone != null && phone.hasVoice();
	}
	public boolean isVoiceCapable() { return isDeviceVoiceCapable(); }
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

	/* ATL: the remaining constants of AOSP 16 TelephonyManager (api/current.txt) */
	/** @hide AOSP SIM_STATE_LOADED, SIM_STATE_PRESENT (SystemApi) */
	public static final int SIM_STATE_LOADED = 10;
	public static final int SIM_STATE_PRESENT = 11;
	public static final String ACTION_CARRIER_MESSAGING_CLIENT_SERVICE = "android.telephony.action.CARRIER_MESSAGING_CLIENT_SERVICE";
	public static final String ACTION_CARRIER_SIGNAL_DEFAULT_NETWORK_AVAILABLE = "android.telephony.action.CARRIER_SIGNAL_DEFAULT_NETWORK_AVAILABLE";
	public static final String ACTION_CARRIER_SIGNAL_PCO_VALUE = "android.telephony.action.CARRIER_SIGNAL_PCO_VALUE";
	public static final String ACTION_CARRIER_SIGNAL_REDIRECTED = "android.telephony.action.CARRIER_SIGNAL_REDIRECTED";
	public static final String ACTION_CARRIER_SIGNAL_REQUEST_NETWORK_FAILED = "android.telephony.action.CARRIER_SIGNAL_REQUEST_NETWORK_FAILED";
	public static final String ACTION_CARRIER_SIGNAL_RESET = "android.telephony.action.CARRIER_SIGNAL_RESET";
	public static final String ACTION_CONFIGURE_VOICEMAIL = "android.telephony.action.CONFIGURE_VOICEMAIL";
	public static final String ACTION_MULTI_SIM_CONFIG_CHANGED = "android.telephony.action.MULTI_SIM_CONFIG_CHANGED";
	public static final String ACTION_NETWORK_COUNTRY_CHANGED = "android.telephony.action.NETWORK_COUNTRY_CHANGED";
	public static final String ACTION_RESET_MOBILE_NETWORK_SETTINGS = "android.telephony.action.RESET_MOBILE_NETWORK_SETTINGS";
	public static final String ACTION_RESPOND_VIA_MESSAGE = "android.intent.action.RESPOND_VIA_MESSAGE";
	public static final String ACTION_SECRET_CODE = "android.telephony.action.SECRET_CODE";
	public static final String ACTION_SHOW_VOICEMAIL_NOTIFICATION = "android.telephony.action.SHOW_VOICEMAIL_NOTIFICATION";
	public static final String ACTION_SUBSCRIPTION_CARRIER_IDENTITY_CHANGED = "android.telephony.action.SUBSCRIPTION_CARRIER_IDENTITY_CHANGED";
	public static final String ACTION_SUBSCRIPTION_SPECIFIC_CARRIER_IDENTITY_CHANGED = "android.telephony.action.SUBSCRIPTION_SPECIFIC_CARRIER_IDENTITY_CHANGED";
	public static final int APPTYPE_CSIM = 4;
	public static final int APPTYPE_ISIM = 5;
	public static final int APPTYPE_RUIM = 3;
	public static final int APPTYPE_SIM = 1;
	public static final int APPTYPE_UNKNOWN = 0;
	public static final int APPTYPE_USIM = 2;
	public static final int AUTHTYPE_EAP_AKA = 129;
	public static final int AUTHTYPE_EAP_SIM = 128;
	public static final int AUTHTYPE_GBA_BOOTSTRAP = 132;
	public static final int AUTHTYPE_GBA_NAF_KEY_EXTERNAL = 133;
	public static final int CALL_COMPOSER_STATUS_ON = 1;
	public static final int CALL_STATE_IDLE = 0;
	public static final int CALL_STATE_OFFHOOK = 2;
	public static final int CALL_STATE_RINGING = 1;
	public static final String CAPABILITY_SLICING_CONFIG_SUPPORTED = "CAPABILITY_SLICING_CONFIG_SUPPORTED";
	public static final int CARRIER_RESTRICTION_STATUS_NOT_RESTRICTED = 1;
	public static final int CARRIER_RESTRICTION_STATUS_RESTRICTED = 2;
	public static final int CARRIER_RESTRICTION_STATUS_RESTRICTED_TO_CALLER = 3;
	public static final int CARRIER_RESTRICTION_STATUS_UNKNOWN = 0;
	public static final int DATA_CONNECTED = 2;
	public static final int DATA_CONNECTING = 1;
	public static final int DATA_DISCONNECTED = 0;
	public static final int DATA_DISCONNECTING = 4;
	public static final int DATA_ENABLED_REASON_CARRIER = 2;
	public static final int DATA_ENABLED_REASON_OVERRIDE = 4;
	public static final int DATA_ENABLED_REASON_POLICY = 1;
	public static final int DATA_ENABLED_REASON_THERMAL = 3;
	public static final int DATA_ENABLED_REASON_UNKNOWN = -1;
	public static final int DATA_HANDOVER_IN_PROGRESS = 5;
	public static final int DATA_SUSPENDED = 3;
	public static final int DATA_UNKNOWN = -1;
	public static final int DEFAULT_PORT_INDEX = 0;
	public static final String EVENT_DISPLAY_EMERGENCY_MESSAGE = "android.telephony.event.DISPLAY_EMERGENCY_MESSAGE";
	public static final String EXTRA_ACTIVE_SIM_SUPPORTED_COUNT = "android.telephony.extra.ACTIVE_SIM_SUPPORTED_COUNT";
	public static final String EXTRA_APN_PROTOCOL = "android.telephony.extra.APN_PROTOCOL";
	public static final String EXTRA_APN_TYPE = "android.telephony.extra.APN_TYPE";
	public static final String EXTRA_CALL_VOICEMAIL_INTENT = "android.telephony.extra.CALL_VOICEMAIL_INTENT";
	public static final String EXTRA_CARRIER_ID = "android.telephony.extra.CARRIER_ID";
	public static final String EXTRA_CARRIER_NAME = "android.telephony.extra.CARRIER_NAME";
	public static final String EXTRA_DATA_FAIL_CAUSE = "android.telephony.extra.DATA_FAIL_CAUSE";
	public static final String EXTRA_DEFAULT_NETWORK_AVAILABLE = "android.telephony.extra.DEFAULT_NETWORK_AVAILABLE";
	public static final String EXTRA_EMERGENCY_CALL_TO_SATELLITE_HANDOVER_TYPE = "android.telephony.extra.EMERGENCY_CALL_TO_SATELLITE_HANDOVER_TYPE";
	public static final String EXTRA_EMERGENCY_CALL_TO_SATELLITE_LAUNCH_INTENT = "android.telephony.extra.EMERGENCY_CALL_TO_SATELLITE_LAUNCH_INTENT";
	public static final String EXTRA_HIDE_PUBLIC_SETTINGS = "android.telephony.extra.HIDE_PUBLIC_SETTINGS";
	public static final String EXTRA_IS_REFRESH = "android.telephony.extra.IS_REFRESH";
	public static final String EXTRA_LAST_KNOWN_NETWORK_COUNTRY = "android.telephony.extra.LAST_KNOWN_NETWORK_COUNTRY";
	public static final String EXTRA_LAUNCH_VOICEMAIL_SETTINGS_INTENT = "android.telephony.extra.LAUNCH_VOICEMAIL_SETTINGS_INTENT";
	public static final String EXTRA_NETWORK_COUNTRY = "android.telephony.extra.NETWORK_COUNTRY";
	public static final String EXTRA_NOTIFICATION_COUNT = "android.telephony.extra.NOTIFICATION_COUNT";
	public static final String EXTRA_PCO_ID = "android.telephony.extra.PCO_ID";
	public static final String EXTRA_PCO_VALUE = "android.telephony.extra.PCO_VALUE";
	public static final String EXTRA_PHONE_ACCOUNT_HANDLE = "android.telephony.extra.PHONE_ACCOUNT_HANDLE";
	public static final String EXTRA_REDIRECTION_URL = "android.telephony.extra.REDIRECTION_URL";
	public static final String EXTRA_SPECIFIC_CARRIER_ID = "android.telephony.extra.SPECIFIC_CARRIER_ID";
	public static final String EXTRA_SPECIFIC_CARRIER_NAME = "android.telephony.extra.SPECIFIC_CARRIER_NAME";
	public static final String EXTRA_SUBSCRIPTION_ID = "android.telephony.extra.SUBSCRIPTION_ID";
	public static final String EXTRA_VOICEMAIL_NUMBER = "android.telephony.extra.VOICEMAIL_NUMBER";
	public static final int INCLUDE_LOCATION_DATA_COARSE = 1;
	public static final int INCLUDE_LOCATION_DATA_FINE = 2;
	public static final int INCLUDE_LOCATION_DATA_NONE = 0;
	public static final String METADATA_HIDE_VOICEMAIL_SETTINGS_MENU = "android.telephony.HIDE_VOICEMAIL_SETTINGS_MENU";
	public static final int MULTISIM_ALLOWED = 0;
	public static final int MULTISIM_NOT_SUPPORTED_BY_CARRIER = 2;
	public static final long NETWORK_TYPE_BITMASK_1xRTT = 64L;
	public static final long NETWORK_TYPE_BITMASK_EDGE = 2L;
	public static final long NETWORK_TYPE_BITMASK_EVDO_0 = 16L;
	public static final long NETWORK_TYPE_BITMASK_EVDO_A = 32L;
	public static final long NETWORK_TYPE_BITMASK_EVDO_B = 2048L;
	public static final long NETWORK_TYPE_BITMASK_GPRS = 1L;
	public static final long NETWORK_TYPE_BITMASK_GSM = 32768L;
	public static final long NETWORK_TYPE_BITMASK_HSDPA = 128L;
	public static final long NETWORK_TYPE_BITMASK_HSPA = 512L;
	public static final long NETWORK_TYPE_BITMASK_HSPAP = 16384L;
	public static final long NETWORK_TYPE_BITMASK_HSUPA = 256L;
	public static final long NETWORK_TYPE_BITMASK_IWLAN = 131072L;
	public static final long NETWORK_TYPE_BITMASK_LTE = 4096L;
	public static final long NETWORK_TYPE_BITMASK_NR = 524288L;
	public static final long NETWORK_TYPE_BITMASK_TD_SCDMA = 65536L;
	public static final long NETWORK_TYPE_BITMASK_UMTS = 4L;
	public static final int PREMIUM_CAPABILITY_PRIORITIZE_LATENCY = 34;
	public static final int PURCHASE_PREMIUM_CAPABILITY_RESULT_ALREADY_IN_PROGRESS = 4;
	public static final int PURCHASE_PREMIUM_CAPABILITY_RESULT_ALREADY_PURCHASED = 3;
	public static final int PURCHASE_PREMIUM_CAPABILITY_RESULT_CARRIER_DISABLED = 7;
	public static final int PURCHASE_PREMIUM_CAPABILITY_RESULT_CARRIER_ERROR = 8;
	public static final int PURCHASE_PREMIUM_CAPABILITY_RESULT_ENTITLEMENT_CHECK_FAILED = 13;
	public static final int PURCHASE_PREMIUM_CAPABILITY_RESULT_FEATURE_NOT_SUPPORTED = 10;
	public static final int PURCHASE_PREMIUM_CAPABILITY_RESULT_NETWORK_NOT_AVAILABLE = 12;
	public static final int PURCHASE_PREMIUM_CAPABILITY_RESULT_NOT_DEFAULT_DATA_SUBSCRIPTION = 14;
	public static final int PURCHASE_PREMIUM_CAPABILITY_RESULT_NOT_FOREGROUND = 5;
	public static final int PURCHASE_PREMIUM_CAPABILITY_RESULT_PENDING_NETWORK_SETUP = 15;
	public static final int PURCHASE_PREMIUM_CAPABILITY_RESULT_REQUEST_FAILED = 11;
	public static final int PURCHASE_PREMIUM_CAPABILITY_RESULT_SUCCESS = 1;
	public static final int PURCHASE_PREMIUM_CAPABILITY_RESULT_THROTTLED = 2;
	public static final int PURCHASE_PREMIUM_CAPABILITY_RESULT_TIMEOUT = 9;
	public static final int PURCHASE_PREMIUM_CAPABILITY_RESULT_USER_CANCELED = 6;
	public static final int SET_OPPORTUNISTIC_SUB_INACTIVE_SUBSCRIPTION = 2;
	public static final int SET_OPPORTUNISTIC_SUB_NO_OPPORTUNISTIC_SUB_AVAILABLE = 3;
	public static final int SET_OPPORTUNISTIC_SUB_REMOTE_SERVICE_EXCEPTION = 4;
	public static final int SET_OPPORTUNISTIC_SUB_SUCCESS = 0;
	public static final int SET_OPPORTUNISTIC_SUB_VALIDATION_FAILED = 1;
	public static final int SIM_STATE_ABSENT = 1;
	public static final int SIM_STATE_CARD_IO_ERROR = 8;
	public static final int SIM_STATE_CARD_RESTRICTED = 9;
	public static final int SIM_STATE_NETWORK_LOCKED = 4;
	public static final int SIM_STATE_NOT_READY = 6;
	public static final int SIM_STATE_PERM_DISABLED = 7;
	public static final int SIM_STATE_PIN_REQUIRED = 2;
	public static final int SIM_STATE_PUK_REQUIRED = 3;
	public static final int SIM_STATE_READY = 5;
	public static final int SIM_STATE_UNKNOWN = 0;
	public static final int UNSUPPORTED_CARD_ID = -1;
	public static final int UPDATE_AVAILABLE_NETWORKS_ABORTED = 2;
	public static final int UPDATE_AVAILABLE_NETWORKS_DISABLE_MODEM_FAIL = 5;
	public static final int UPDATE_AVAILABLE_NETWORKS_ENABLE_MODEM_FAIL = 6;
	public static final int UPDATE_AVAILABLE_NETWORKS_INVALID_ARGUMENTS = 3;
	public static final int UPDATE_AVAILABLE_NETWORKS_MULTIPLE_NETWORKS_NOT_SUPPORTED = 7;
	public static final int UPDATE_AVAILABLE_NETWORKS_NO_CARRIER_PRIVILEGE = 4;
	public static final int UPDATE_AVAILABLE_NETWORKS_NO_OPPORTUNISTIC_SUB_AVAILABLE = 8;
	public static final int UPDATE_AVAILABLE_NETWORKS_REMOTE_SERVICE_EXCEPTION = 9;
	public static final int UPDATE_AVAILABLE_NETWORKS_SERVICE_IS_DISABLED = 10;
	public static final int UPDATE_AVAILABLE_NETWORKS_SUCCESS = 0;
	public static final int UPDATE_AVAILABLE_NETWORKS_UNKNOWN_FAILURE = 1;
	public static final int USSD_ERROR_SERVICE_UNAVAIL = -2;
	public static final int USSD_RETURN_FAILURE = -1;
	public static final String VVM_TYPE_CVVM = "vvm_type_cvvm";
	public static final String VVM_TYPE_OMTP = "vvm_type_omtp";
}
