package com.android.internal.telephony;

import android.app.ActivityThread;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.telephony.ServiceState;
import android.telephony.SmsMessage;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Slog;

import com.android.internal.telephony.uicc.IccUtils;

import android.atl.annotation.Export;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @hide ATL's phone process: the first modem of ModemManager (org.freedesktop.ModemManager1 on the system bus),
 * as AOSP's phone process (GsmCdmaPhone, UiccController, ServiceStateTracker, InboundSmsHandler) serves
 * TelephonyManager, SubscriptionManager and SmsManager. Without ModemManager or without a modem, get() is null
 * and the managers take the branches AOSP takes when the telephony services are missing.
 *
 * One phone (slot 0) with one subscription (SUB_ID) for the modem's SIM, the state as ModemManager reports it.
 */
public final class ModemManagerPhone {
	private static final String TAG = "ModemManagerPhone";
	private static final String MM = "org.freedesktop.ModemManager1";
	public static final String IFACE_MODEM = MM + ".Modem";
	public static final String IFACE_3GPP = MM + ".Modem.Modem3gpp";
	public static final String IFACE_MESSAGING = MM + ".Modem.Messaging";
	public static final String IFACE_VOICE = MM + ".Modem.Voice";
	public static final String IFACE_SIM = MM + ".Sim";
	public static final String IFACE_SMS = MM + ".Sms";

	/** the subscription id of the modem's SIM (AOSP numbers subscriptions from 1) and its slot */
	public static final int SUB_ID = 1;
	public static final int SLOT_INDEX = 0;

	/* MMModemState, MMModemStateFailedReason, MMModemLock, MMModemCapability, MMModem3gppRegistrationState, MMSmsState */
	private static final int MM_MODEM_STATE_FAILED = -1, MM_MODEM_STATE_UNKNOWN = 0, MM_MODEM_STATE_INITIALIZING = 1,
	    MM_MODEM_STATE_LOCKED = 2, MM_MODEM_STATE_DISABLED = 3, MM_MODEM_STATE_DISABLING = 4, MM_MODEM_STATE_ENABLING = 5,
	    MM_MODEM_STATE_DISCONNECTING = 9, MM_MODEM_STATE_CONNECTING = 10, MM_MODEM_STATE_CONNECTED = 11;
	private static final int MM_MODEM_STATE_FAILED_REASON_SIM_MISSING = 2, MM_MODEM_STATE_FAILED_REASON_SIM_ERROR = 3;
	private static final int MM_MODEM_LOCK_NONE = 1, MM_MODEM_LOCK_SIM_PIN = 2, MM_MODEM_LOCK_SIM_PIN2 = 3,
	    MM_MODEM_LOCK_SIM_PUK = 4, MM_MODEM_LOCK_SIM_PUK2 = 5, MM_MODEM_LOCK_PH_NET_PIN = 8, MM_MODEM_LOCK_PH_NET_PUK = 9;
	private static final int MM_MODEM_CAPABILITY_CDMA_EVDO = 1 << 1, MM_MODEM_CAPABILITY_GSM_UMTS = 1 << 2,
	    MM_MODEM_CAPABILITY_LTE = 1 << 3, MM_MODEM_CAPABILITY_5GNR = 1 << 6;
	private static final int REG_HOME = 1, REG_SEARCHING = 2, REG_DENIED = 3, REG_ROAMING = 5, REG_HOME_SMS_ONLY = 6,
	    REG_ROAMING_SMS_ONLY = 7, REG_EMERGENCY_ONLY = 8, REG_HOME_CSFB_NOT_PREFERRED = 9, REG_ROAMING_CSFB_NOT_PREFERRED = 10;
	private static final int MM_SMS_STATE_RECEIVED = 3;

	private static native boolean nativeConnect();
	private static native String[] nativeGetModems();
	private static native String[] nativeGetAll(String path, String iface);
	private static native String nativeCreateSms(String modem, String number, String text, byte[] data, boolean deliveryReport);
	private static native void nativeSendSms(String path, int token);

	private static boolean sConnected;
	private static volatile ModemManagerPhone sPhone;
	private static final CopyOnWriteArrayList<Runnable> sListeners = new CopyOnWriteArrayList<>();

	/* the state of one modem: interface -> property -> value (see value_string() in the C part) */
	public final String modemPath;
	private final Map<String, Map<String, String>> mProps;
	private final Map<String, String> mSim;

	private ModemManagerPhone(String modemPath, Map<String, Map<String, String>> props, Map<String, String> sim) {
		this.modemPath = modemPath;
		mProps = props;
		mSim = sim;
	}

	/** the phone of the first modem, null when ModemManager has none (a device without telephony) */
	public static ModemManagerPhone get() {
		synchronized (ModemManagerPhone.class) {
			if (!sConnected) {
				sConnected = true;
				if (nativeConnect())
					refresh();
			}
		}
		return sPhone;
	}

	/** the SMS service (AOSP SmsController) of the modem, null without one that can send SMS */
	public static ISms getSmsService() {
		ModemManagerPhone phone = get();
		return phone != null && phone.hasMessaging() ? SmsController.get() : null;
	}

	/** run when the state changes, on the main thread */
	public static void addListener(Runnable listener) {
		get();
		sListeners.addIfAbsent(listener);
	}

	public static void removeListener(Runnable listener) {
		sListeners.remove(listener);
	}

	private static Map<String, Map<String, Map<String, String>>> rows(String[] rows) {
		Map<String, Map<String, Map<String, String>>> objects = new HashMap<>();
		for (int i = 0; i + 3 < rows.length; i += 4)
			objects.computeIfAbsent(rows[i], k -> new HashMap<>()).computeIfAbsent(rows[i + 1], k -> new HashMap<>()).put(rows[i + 2], rows[i + 3]);
		return objects;
	}

	private static Map<String, String> getAll(String path, String iface) {
		Map<String, Map<String, String>> object = rows(nativeGetAll(path, iface)).get(path);
		return object != null && object.get(iface) != null ? object.get(iface) : new HashMap<String, String>();
	}

	private static synchronized void refresh() {
		String modemPath = null;
		Map<String, Map<String, String>> props = null;
		for (Map.Entry<String, Map<String, Map<String, String>>> e : rows(nativeGetModems()).entrySet()) {
			if (e.getValue().containsKey(IFACE_MODEM) && (modemPath == null || e.getKey().compareTo(modemPath) < 0)) {
				modemPath = e.getKey();
				props = e.getValue();
			}
		}
		if (modemPath == null) {
			if (sPhone != null)
				Slog.i(TAG, "the modem is gone");
			sPhone = null;
			return;
		}
		String simPath = props.get(IFACE_MODEM).get("Sim");
		Map<String, String> sim = simPath == null || simPath.equals("/") ? new HashMap<String, String>() : getAll(simPath, IFACE_SIM);
		if (sPhone == null)
			Slog.i(TAG, "modem " + modemPath + ": " + props.get(IFACE_MODEM).get("Manufacturer") + " " + props.get(IFACE_MODEM).get("Model"));
		sPhone = new ModemManagerPhone(modemPath, props, sim);
	}

	/* ModemManager reports a change: read the state again, tell the listeners, deliver SMS that arrived in full */
	@Export
	static void changed() {
		refresh();
		for (Runnable listener : sListeners)
			listener.run();
		InboundSms.checkPending();
	}

	@Export
	static void smsAdded(String path, boolean received) {
		if (received)
			InboundSms.added(path);
	}

	@Export
	static void smsSent(int token, String error) {
		SmsController.sent(token, error);
	}

	/* ---- the modem's state ---- */

	private String prop(String iface, String name) {
		Map<String, String> p = mProps.get(iface);
		String value = p != null ? p.get(name) : null;
		return value != null ? value : "";
	}

	private int intProp(String iface, String name, int def) {
		try {
			return Integer.parseInt(prop(iface, name));
		} catch (NumberFormatException e) {
			return def;
		}
	}

	private long uintProp(String iface, String name) {
		try {
			return Long.parseLong(prop(iface, name));
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	public boolean hasMessaging() {
		return mProps.containsKey(IFACE_MESSAGING);
	}

	public boolean hasVoice() {
		return mProps.containsKey(IFACE_VOICE);
	}

	/** 3GPP (GSM/UMTS/LTE/NR): ModemManager's capabilities */
	public boolean isGsm() {
		return (uintProp(IFACE_MODEM, "CurrentCapabilities") & (MM_MODEM_CAPABILITY_GSM_UMTS | MM_MODEM_CAPABILITY_LTE | MM_MODEM_CAPABILITY_5GNR)) != 0
		    || mProps.containsKey(IFACE_3GPP);
	}

	public boolean isCdma() {
		return (uintProp(IFACE_MODEM, "CurrentCapabilities") & MM_MODEM_CAPABILITY_CDMA_EVDO) != 0;
	}

	/** a modem that can carry mobile data (3GPP packet data or CDMA EVDO) */
	public boolean isDataCapable() {
		return isGsm() || isCdma();
	}

	private int modemState() {
		return intProp(IFACE_MODEM, "State", MM_MODEM_STATE_UNKNOWN);
	}

	public boolean hasSim() {
		return !mSim.isEmpty();
	}

	/** TelephonyManager.SIM_STATE_* as UiccController reports it; LOADED once the SIM can be used */
	public int getSimState() {
		int state = modemState();
		if (state == MM_MODEM_STATE_FAILED) {
			int reason = intProp(IFACE_MODEM, "StateFailedReason", 0);
			if (reason == MM_MODEM_STATE_FAILED_REASON_SIM_MISSING)
				return TelephonyManager.SIM_STATE_ABSENT;
			if (reason == MM_MODEM_STATE_FAILED_REASON_SIM_ERROR)
				return TelephonyManager.SIM_STATE_CARD_IO_ERROR;
			return hasSim() ? TelephonyManager.SIM_STATE_NOT_READY : TelephonyManager.SIM_STATE_ABSENT;
		}
		if (state == MM_MODEM_STATE_LOCKED) {
			switch (intProp(IFACE_MODEM, "UnlockRequired", MM_MODEM_LOCK_NONE)) {
				case MM_MODEM_LOCK_SIM_PIN:
					return TelephonyManager.SIM_STATE_PIN_REQUIRED;
				case MM_MODEM_LOCK_SIM_PUK:
					return TelephonyManager.SIM_STATE_PUK_REQUIRED;
				case MM_MODEM_LOCK_PH_NET_PIN:
				case MM_MODEM_LOCK_PH_NET_PUK:
					return TelephonyManager.SIM_STATE_NETWORK_LOCKED;
				default:
					return TelephonyManager.SIM_STATE_NOT_READY;
			}
		}
		if (!hasSim())
			return state == MM_MODEM_STATE_UNKNOWN || state == MM_MODEM_STATE_INITIALIZING ? TelephonyManager.SIM_STATE_UNKNOWN
			                                                                                : TelephonyManager.SIM_STATE_ABSENT;
		if (state == MM_MODEM_STATE_UNKNOWN || state == MM_MODEM_STATE_INITIALIZING)
			return TelephonyManager.SIM_STATE_NOT_READY;
		return TelephonyManager.SIM_STATE_LOADED;
	}

	/** an active subscription: a SIM whose records are loaded */
	public boolean hasSubscription() {
		return getSimState() == TelephonyManager.SIM_STATE_LOADED;
	}

	public String getSimOperatorNumeric() {
		return hasSubscription() ? mSim.getOrDefault("OperatorIdentifier", "") : "";
	}

	/** the SPN (Sim.OperatorName) */
	public String getSimOperatorName() {
		return hasSubscription() ? mSim.getOrDefault("OperatorName", "") : "";
	}

	public String getSimCountryIso() {
		String numeric = getSimOperatorNumeric();
		return numeric.length() >= 3 ? MccTable.countryCodeForMcc(numeric.substring(0, 3)) : "";
	}

	/** the SubscriptionInfo of the SIM (SubscriptionManagerService builds it from the SIM records), null without one */
	public android.telephony.SubscriptionInfo getSubscriptionInfo() {
		if (!hasSubscription())
			return null;
		String numeric = getSimOperatorNumeric();
		String name = getSimOperatorName();
		if (name.isEmpty())
			name = getNetworkOperatorName();
		String number = getLine1Number();
		return new android.telephony.SubscriptionInfo.Builder()
		    .setId(SUB_ID)
		    .setIccId(getIccId())
		    .setSimSlotIndex(SLOT_INDEX)
		    .setDisplayName(name)
		    .setCarrierName(name)
		    .setDisplayNameSource(SubscriptionManager.NAME_SOURCE_SIM_SPN)
		    .setNumber(number != null ? number : "")
		    .setMcc(numeric.length() >= 5 ? numeric.substring(0, 3) : null)
		    .setMnc(numeric.length() >= 5 ? numeric.substring(3) : null)
		    .setCountryIso(getSimCountryIso())
		    .build();
	}

	public String getIccId() {
		return hasSim() ? mSim.getOrDefault("SimIdentifier", "") : "";
	}

	public String getImsi() {
		return hasSubscription() ? mSim.getOrDefault("Imsi", "") : "";
	}

	public String getImei() {
		String imei = prop(IFACE_3GPP, "Imei");
		return !imei.isEmpty() ? imei : prop(IFACE_MODEM, "EquipmentIdentifier");
	}

	public String getLine1Number() {
		String numbers = prop(IFACE_MODEM, "OwnNumbers");
		return numbers.isEmpty() ? null : numbers.split("\n")[0];
	}

	public String getManufacturer() {
		return prop(IFACE_MODEM, "Manufacturer");
	}

	public String getRevision() {
		return prop(IFACE_MODEM, "Revision");
	}

	/** EmergencyNumberTracker: the SIM's emergency numbers and the defaults (112, 911 with a SIM, more without) */
	public List<android.telephony.emergency.EmergencyNumber> getEmergencyNumbers() {
		List<android.telephony.emergency.EmergencyNumber> list = new ArrayList<>();
		Set<String> seen = new HashSet<>();
		String country = getNetworkCountryIso().isEmpty() ? getSimCountryIso() : getNetworkCountryIso();
		for (String n : mSim.getOrDefault("EmergencyNumbers", "").split("\n")) {
			if (!n.isEmpty() && seen.add(n))
				list.add(new android.telephony.emergency.EmergencyNumber(n, country, "", android.telephony.emergency.EmergencyNumber.EMERGENCY_SERVICE_CATEGORY_UNSPECIFIED,
				                                                       new ArrayList<String>(), android.telephony.emergency.EmergencyNumber.EMERGENCY_NUMBER_SOURCE_SIM,
				                                                       android.telephony.emergency.EmergencyNumber.EMERGENCY_CALL_ROUTING_UNKNOWN));
		}
		String defaults = getSimState() == TelephonyManager.SIM_STATE_ABSENT ? "112,911,000,08,110,118,119,999" : "112,911";
		for (String n : defaults.split(",")) {
			if (seen.add(n))
				list.add(new android.telephony.emergency.EmergencyNumber(n, "", "", android.telephony.emergency.EmergencyNumber.EMERGENCY_SERVICE_CATEGORY_UNSPECIFIED,
				                                                       new ArrayList<String>(), android.telephony.emergency.EmergencyNumber.EMERGENCY_NUMBER_SOURCE_DEFAULT,
				                                                       android.telephony.emergency.EmergencyNumber.EMERGENCY_CALL_ROUTING_UNKNOWN));
		}
		return list;
	}

	private int registration() {
		return intProp(IFACE_3GPP, "RegistrationState", 0);
	}

	/** the radio is off: ModemManager's modem is disabled (or locked, before it can be enabled) */
	public boolean isRadioOff() {
		int state = modemState();
		return state == MM_MODEM_STATE_DISABLED || state == MM_MODEM_STATE_DISABLING || state == MM_MODEM_STATE_LOCKED
		    || state == MM_MODEM_STATE_FAILED || state == MM_MODEM_STATE_ENABLING;
	}

	/** ServiceState.STATE_* of voice (ServiceStateTracker): in service when registered for voice */
	public int getVoiceRegState() {
		if (isRadioOff())
			return ServiceState.STATE_POWER_OFF;
		switch (registration()) {
			case REG_HOME:
			case REG_ROAMING:
			case REG_HOME_CSFB_NOT_PREFERRED:
			case REG_ROAMING_CSFB_NOT_PREFERRED:
				return ServiceState.STATE_IN_SERVICE;
			case REG_EMERGENCY_ONLY:
			case REG_DENIED:
				return ServiceState.STATE_EMERGENCY_ONLY;
			default:
				return ServiceState.STATE_OUT_OF_SERVICE;
		}
	}

	/** registered at all (also SMS only): the network's operator is known */
	public boolean isRegistered() {
		if (isRadioOff())
			return false;
		int reg = registration();
		return reg == REG_HOME || reg == REG_ROAMING || reg == REG_HOME_SMS_ONLY || reg == REG_ROAMING_SMS_ONLY
		    || reg == REG_HOME_CSFB_NOT_PREFERRED || reg == REG_ROAMING_CSFB_NOT_PREFERRED;
	}

	public boolean isRoaming() {
		int reg = registration();
		return !isRadioOff() && (reg == REG_ROAMING || reg == REG_ROAMING_SMS_ONLY || reg == REG_ROAMING_CSFB_NOT_PREFERRED);
	}

	public String getNetworkOperatorNumeric() {
		return isRegistered() ? prop(IFACE_3GPP, "OperatorCode") : "";
	}

	public String getNetworkOperatorName() {
		return isRegistered() ? prop(IFACE_3GPP, "OperatorName") : "";
	}

	public String getNetworkCountryIso() {
		String numeric = getNetworkOperatorNumeric();
		return numeric.length() >= 3 ? MccTable.countryCodeForMcc(numeric.substring(0, 3)) : "";
	}

	/** TelephonyManager.NETWORK_TYPE_* of the best access technology ModemManager reports */
	public int getNetworkType() {
		if (!isRegistered())
			return TelephonyManager.NETWORK_TYPE_UNKNOWN;
		long tech = uintProp(IFACE_MODEM, "AccessTechnologies");
		/* MMModemAccessTechnology bits; LTE with 5G NR is NR NSA, which AOSP reports as LTE */
		if ((tech & (1 << 14 | 1 << 16 | 1 << 17)) != 0)
			return TelephonyManager.NETWORK_TYPE_LTE;
		if ((tech & (1 << 15)) != 0)
			return TelephonyManager.NETWORK_TYPE_NR;
		if ((tech & (1 << 9)) != 0)
			return TelephonyManager.NETWORK_TYPE_HSPAP;
		if ((tech & (1 << 8)) != 0)
			return TelephonyManager.NETWORK_TYPE_HSPA;
		if ((tech & (1 << 7)) != 0)
			return TelephonyManager.NETWORK_TYPE_HSUPA;
		if ((tech & (1 << 6)) != 0)
			return TelephonyManager.NETWORK_TYPE_HSDPA;
		if ((tech & (1 << 5)) != 0)
			return TelephonyManager.NETWORK_TYPE_UMTS;
		if ((tech & (1 << 13)) != 0)
			return TelephonyManager.NETWORK_TYPE_EVDO_B;
		if ((tech & (1 << 12)) != 0)
			return TelephonyManager.NETWORK_TYPE_EVDO_A;
		if ((tech & (1 << 11)) != 0)
			return TelephonyManager.NETWORK_TYPE_EVDO_0;
		if ((tech & (1 << 10)) != 0)
			return TelephonyManager.NETWORK_TYPE_1xRTT;
		if ((tech & (1 << 4)) != 0)
			return TelephonyManager.NETWORK_TYPE_EDGE;
		if ((tech & (1 << 3)) != 0)
			return TelephonyManager.NETWORK_TYPE_GPRS;
		if ((tech & (1 << 1 | 1 << 2)) != 0)
			return TelephonyManager.NETWORK_TYPE_GSM;
		return TelephonyManager.NETWORK_TYPE_UNKNOWN;
	}

	/** TelephonyManager.DATA_* from the modem's state (ModemManager: CONNECTED while a bearer is connected) */
	public int getDataState() {
		switch (modemState()) {
			case MM_MODEM_STATE_CONNECTED:
				return TelephonyManager.DATA_CONNECTED;
			case MM_MODEM_STATE_CONNECTING:
				return TelephonyManager.DATA_CONNECTING;
			case MM_MODEM_STATE_DISCONNECTING:
				return TelephonyManager.DATA_DISCONNECTING;
			default:
				return TelephonyManager.DATA_DISCONNECTED;
		}
	}

	/** SignalQuality in percent, -1 when unknown */
	public int getSignalQuality() {
		return isRadioOff() ? -1 : intProp(IFACE_MODEM, "SignalQuality", -1);
	}

	/** the ServiceState ServiceStateTracker would report */
	public ServiceState getServiceState() {
		ServiceState ss = new ServiceState();
		int voice = getVoiceRegState();
		ss.setVoiceRegState(voice);
		ss.setDataRegState(isRegistered() ? ServiceState.STATE_IN_SERVICE : voice == ServiceState.STATE_POWER_OFF ? ServiceState.STATE_POWER_OFF : ServiceState.STATE_OUT_OF_SERVICE);
		ss.setRoaming(isRoaming());
		if (isRegistered())
			ss.setOperatorName(getNetworkOperatorName(), getNetworkOperatorName(), getNetworkOperatorNumeric());
		ss.setIsManualSelection(false);
		return ss;
	}

	/* ---- SMS ---- */

	/** Messaging.Create and Sms.Send: the answer comes through callback(token, error or null) */
	String createSms(String number, String text, byte[] data, boolean deliveryReport) {
		return nativeCreateSms(modemPath, number, text, data, deliveryReport);
	}

	static void sendSms(String path, int token) {
		nativeSendSms(path, token);
	}

	static Map<String, String> getSms(String path) {
		return getAll(path, IFACE_SMS);
	}

	/**
	 * AOSP InboundSmsHandler for ModemManager: a received SMS (ModemManager has put the parts together) goes as
	 * SMS_DELIVER_ACTION to the default SMS app, then as SMS_RECEIVED_ACTION to every receiver, with the 3GPP
	 * SMS-DELIVER PDUs ATL builds from the SMS's number, text and time. ModemManager keeps received SMS until someone
	 * deletes them (on postmarketOS Chatty does); ATL delivers each SMS object once and leaves it there.
	 */
	static final class InboundSms {
		private static final Set<String> sDone = new HashSet<>();
		private static final Set<String> sPending = new HashSet<>();

		static synchronized void added(String path) {
			if (sDone.contains(path))
				return;
			sPending.add(path);
			checkPending();
		}

		static synchronized void checkPending() {
			for (String path : new ArrayList<>(sPending)) {
				Map<String, String> sms = getSms(path);
				if (sms.isEmpty()) {
					sPending.remove(path);
					continue;
				}
				if (!String.valueOf(MM_SMS_STATE_RECEIVED).equals(sms.get("State")))
					continue; /* still receiving parts: PropertiesChanged brings us back */
				sPending.remove(path);
				sDone.add(path);
				dispatch(sms);
			}
		}

		private static void dispatch(Map<String, String> sms) {
			Context context = ActivityThread.currentApplication();
			byte[][] pdus;
			try {
				pdus = DeliverPdu.build(sms.get("SMSC"), sms.getOrDefault("Number", ""), sms.get("Text"),
				                        parseData(sms.get("Data")), sms.get("Timestamp"));
			} catch (Exception e) {
				Slog.e(TAG, "cannot build the PDUs of a received SMS", e);
				return;
			}
			Intent intent = new Intent(Telephony.Sms.Intents.SMS_DELIVER_ACTION);
			intent.putExtra("pdus", pdus);
			intent.putExtra("format", SmsConstants.FORMAT_3GPP);
			SubscriptionManager.putPhoneIdAndSubIdExtra(intent, SLOT_INDEX, SUB_ID);
			intent.addFlags(Intent.FLAG_RECEIVER_NO_ABORT);
			ComponentName component = SmsApplication.getDefaultSmsApplication(context, true);
			Slog.i(TAG, "SMS received" + (component != null ? ", delivering to " + component.flattenToShortString() : ", no default SMS app"));
			if (component != null) {
				/* AOSP: only the default SMS app gets SMS_DELIVER; then everyone SMS_RECEIVED (the default SMS app included) */
				intent.setComponent(component);
				context.sendOrderedBroadcast(intent, android.Manifest.permission.RECEIVE_SMS);
			}
			Intent received = new Intent(intent);
			received.setAction(Telephony.Sms.Intents.SMS_RECEIVED_ACTION);
			received.setComponent(null);
			context.sendOrderedBroadcast(received, android.Manifest.permission.RECEIVE_SMS);
		}

		/* the Data property, in hex (value_string() in the C part) */
		private static byte[] parseData(String data) {
			return data == null || data.isEmpty() ? null : IccUtils.hexStringToBytes(data);
		}

	}

	/**
	 * 3GPP TS 23.040 SMS-DELIVER PDUs (with the SMSC address in front, as RIL hands them to InboundSmsHandler) for a
	 * text or data SMS: GSM 7 bit when the text fits the default alphabet, else UCS-2, split like a sender splits it
	 * (SmsMessage.fragmentText) with a concatenation header.
	 */
	static final class DeliverPdu {
		private static int sRef;

		static byte[][] build(String smsc, String number, String text, byte[] data, String timestamp) throws EncodeException {
			byte[] scts = scts(timestamp);
			if (text == null || text.isEmpty() && data != null) {
				byte[] octets = data != null ? data : new byte[0];
				return new byte[][] {pdu(smsc, number, 0x04, scts, false, null, octets, -1)};
			}
			int[] ted = SmsMessage.calculateLength(text, false);
			boolean sevenBit = ted[3] == SmsConstants.ENCODING_7BIT;
			List<String> parts = ted[0] > 1 ? SmsMessage.fragmentText(text) : java.util.Collections.singletonList(text);
			int ref = (++sRef) & 0xff;
			byte[][] pdus = new byte[parts.size()][];
			for (int i = 0; i < parts.size(); i++) {
				byte[] header = null;
				if (parts.size() > 1) {
					SmsHeader.ConcatRef concat = new SmsHeader.ConcatRef();
					concat.refNumber = ref;
					concat.seqNumber = i + 1;
					concat.msgCount = parts.size();
					concat.isEightBits = true;
					SmsHeader smsHeader = new SmsHeader();
					smsHeader.concatRef = concat;
					header = SmsHeader.toByteArray(smsHeader);
				}
				if (sevenBit) {
					/* first byte: the user data length in septets, header included */
					byte[] ud = GsmAlphabet.stringToGsm7BitPackedWithHeader(parts.get(i), header, 0, 0);
					pdus[i] = pdu(smsc, number, 0x00, scts, header != null, null, java.util.Arrays.copyOfRange(ud, 1, ud.length), ud[0] & 0xff);
				} else {
					byte[] ucs2 = parts.get(i).getBytes(StandardCharsets.UTF_16BE);
					pdus[i] = pdu(smsc, number, 0x08, scts, header != null, header, ucs2, -1);
				}
			}
			return pdus;
		}

		/* udl >= 0: ud is packed 7 bit with the header in it and udl septets; udl < 0: octets, the header (with its
		 * length byte) goes in front of ud */
		private static byte[] pdu(String smsc, String number, int dcs, byte[] scts, boolean udhi, byte[] header, byte[] ud, int udl) {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			byte[] sc = TextUtils.isEmpty(smsc) ? null : android.telephony.PhoneNumberUtils.networkPortionToCalledPartyBCDWithLength(smsc);
			if (sc == null)
				out.write(0);
			else
				out.write(sc, 0, sc.length);
			out.write(0x04 | (udhi ? 0x40 : 0)); /* SMS-DELIVER, TP-MMS: no more messages */
			writeAddress(out, number);
			out.write(0x00); /* TP-PID */
			out.write(dcs);
			out.write(scts, 0, scts.length);
			if (udl < 0) {
				int len = (header != null ? header.length + 1 : 0) + ud.length;
				out.write(len);
				if (header != null) {
					out.write(header.length);
					out.write(header, 0, header.length);
				}
			} else {
				out.write(udl);
			}
			out.write(ud, 0, ud.length);
			return out.toByteArray();
		}

		/* TP-OA: a number (international with +) or an alphanumeric sender in GSM 7 bit */
		private static void writeAddress(ByteArrayOutputStream out, String number) {
			if (number.matches("\\+?[0-9*#]+")) {
				boolean international = number.startsWith("+");
				String digits = international ? number.substring(1) : number;
				out.write(digits.length());
				out.write(international ? 0x91 : 0x81);
				byte[] bcd = android.telephony.PhoneNumberUtils.numberToCalledPartyBCD(digits);
				/* numberToCalledPartyBCD puts the TOA first */
				out.write(bcd, 1, bcd.length - 1);
				return;
			}
			try {
				byte[] packed = GsmAlphabet.stringToGsm7BitPacked(number, 0, true, 0, 0);
				int septets = packed[0] & 0xff;
				out.write((septets * 7 + 3) / 4); /* semi-octets used */
				out.write(0xd0); /* alphanumeric */
				out.write(packed, 1, packed.length - 1);
			} catch (EncodeException e) {
				out.write(0);
				out.write(0x81);
			}
		}

		/* TP-SCTS: semi-octet swapped YYMMDDhhmmss and the zone in quarter hours */
		private static byte[] scts(String timestamp) {
			OffsetDateTime t;
			try {
				t = OffsetDateTime.parse(normalize(timestamp));
			} catch (DateTimeParseException | NullPointerException e) {
				t = OffsetDateTime.now();
			}
			int[] v = {t.getYear() % 100, t.getMonthValue(), t.getDayOfMonth(), t.getHour(), t.getMinute(), t.getSecond()};
			byte[] out = new byte[7];
			for (int i = 0; i < 6; i++)
				out[i] = (byte)((v[i] % 10) << 4 | v[i] / 10);
			int quarters = t.getOffset().getTotalSeconds() / 900;
			int q = Math.abs(quarters);
			out[6] = (byte)((q % 10) << 4 | q / 10 | (quarters < 0 ? 0x08 : 0));
			return out;
		}

		/* ModemManager writes ISO 8601 with the zone as +HH or +HH:MM (mm_new_iso8601_time) */
		private static String normalize(String timestamp) {
			if (timestamp.matches(".*[+-]\\d\\d$"))
				return timestamp + ":00";
			return timestamp;
		}
	}
}
