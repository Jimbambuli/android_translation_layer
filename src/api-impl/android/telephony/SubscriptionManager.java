package android.telephony;

import com.android.internal.telephony.ModemManagerPhone;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerExecutor;
import android.os.Looper;
import java.util.concurrent.Executor;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import android.annotation.IntDef;
import android.annotation.NonNull;
import java.util.List;

/* AOSP SubscriptionManager (telephony/java/android/telephony/SubscriptionManager.java) with
 * TelephonyManager.getSubscriptionService() == null (no ISub): the lists are empty, a single subscription is null. */
public class SubscriptionManager {
	public static final int INVALID_SUBSCRIPTION_ID = -1;
	public static final int INVALID_SIM_SLOT_INDEX = -1;
	public static final int DEFAULT_SUBSCRIPTION_ID = Integer.MAX_VALUE;

	public static SubscriptionManager from(Context context) {
		return (SubscriptionManager) context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
	}

	public static final int MIN_SUBSCRIPTION_ID_VALUE = 0;
	public static final int MAX_SUBSCRIPTION_ID_VALUE = DEFAULT_SUBSCRIPTION_ID - 1;

	public static boolean isUsableSubscriptionId(int subscriptionId) {
		return isUsableSubIdValue(subscriptionId);
	}

	public static boolean isUsableSubIdValue(int subId) {
		return subId >= MIN_SUBSCRIPTION_ID_VALUE && subId <= MAX_SUBSCRIPTION_ID_VALUE;
	}

	/* AOSP getDefaultSubscriptionId(): the cache's default INVALID_SUBSCRIPTION_ID when ISub is missing */
	public static int getDefaultSubscriptionId() {
		return modemSubId();
	}

	public static boolean isValidSubscriptionId(int subscriptionId) {
		return subscriptionId > INVALID_SUBSCRIPTION_ID;
	}

	public static int getSlotIndex(int subscriptionId) {
		if (subscriptionId == DEFAULT_SUBSCRIPTION_ID)
			subscriptionId = getDefaultSubscriptionId();
		if (subscriptionId == ModemManagerPhone.SUB_ID && modemSubId() == ModemManagerPhone.SUB_ID)
			return ModemManagerPhone.SLOT_INDEX;
		return INVALID_SIM_SLOT_INDEX; // the cache's default when ISub is missing
	}

	/* ATL's phone process: the modem's SIM as subscription SUB_ID once it is loaded (ModemManagerPhone) */
	private static int modemSubId() {
		ModemManagerPhone phone = ModemManagerPhone.get();
		return phone != null && phone.hasSubscription() ? ModemManagerPhone.SUB_ID : INVALID_SUBSCRIPTION_ID;
	}

	private static SubscriptionInfo modemSubscription() {
		ModemManagerPhone phone = ModemManagerPhone.get();
		return phone != null ? phone.getSubscriptionInfo() : null;
	}

	/* AOSP SubscriptionManagerService.getActiveSubscriptionInfoList(): READ_PHONE_STATE (or carrier privileges, else
	 * an empty list); null-free here like the client wraps it */
	public List<SubscriptionInfo> getActiveSubscriptionInfoList() {
		SubscriptionInfo info = modemSubscription();
		if (info != null)
			return new java.util.ArrayList<SubscriptionInfo>(Collections.singletonList(info));
		return Collections.emptyList();
	}

	public SubscriptionInfo getActiveSubscriptionInfo(int subId) {
		SubscriptionInfo info = modemSubscription();
		if (info != null && (subId == ModemManagerPhone.SUB_ID || subId == DEFAULT_SUBSCRIPTION_ID))
			return info;
		return null;
	}

	public List<SubscriptionInfo> getAccessibleSubscriptionInfoList() {
		return Collections.emptyList();
	}

	public List<SubscriptionInfo> getCompleteActiveSubscriptionInfoList() {
		return getActiveSubscriptionInfoList();
	}

	/* AOSP SubscriptionManager.OnSubscriptionsChangedListener: constructor checks and default bodies as in AOSP
	 * (the Compatibility change LAZY_INITIALIZE_SUBSCRIPTIONS_CHANGED_HANDLER is taken as enabled, targetSdk > Q) */
	public static class OnSubscriptionsChangedListener {
		private final android.os.Looper mCreatorLooper;

		public android.os.Looper getCreatorLooper() {
			return mCreatorLooper;
		}

		public OnSubscriptionsChangedListener() {
			mCreatorLooper = android.os.Looper.myLooper();
		}

		public OnSubscriptionsChangedListener(android.os.Looper looper) {
			java.util.Objects.requireNonNull(looper);
			mCreatorLooper = looper;
		}

		public void onSubscriptionsChanged() {}

		public void onAddListenerFailed() {}
	}

	/* AOSP: the cache query returns the default INVALID_SUBSCRIPTION_ID when ISub is not available */
	public static int getDefaultDataSubscriptionId() {
		return modemSubId();
	}

	public static int getActiveDataSubscriptionId() {
		return modemSubId();
	}

	/* AOSP: throws when getSubscriptionService() is null */
	public String getPhoneNumber(int subscriptionId) {
		if (ModemManagerPhone.get() != null) {
			if (subscriptionId == DEFAULT_SUBSCRIPTION_ID)
				subscriptionId = getDefaultSubscriptionId();
			if (subscriptionId != ModemManagerPhone.SUB_ID || modemSubId() != ModemManagerPhone.SUB_ID)
				return "";
			/* SubscriptionManagerService: TelephonyPermissions.checkReadPhoneNumber() */
			android.telephony.TelephonyManager tm = new android.telephony.TelephonyManager();
			String number = tm.getLine1Number();
			return number != null ? number : "";
		}
		throw new IllegalStateException("subscription service unavailable.");
	}

	public void addOnSubscriptionsChangedListener(OnSubscriptionsChangedListener listener) {
		if (listener == null) {
			return;
		}
		Looper looper = listener.getCreatorLooper();
		if (looper == null) {
			throw new RuntimeException("Can't create handler inside thread " + Thread.currentThread() + " that has not called Looper.prepare()");
		}
		addOnSubscriptionsChangedListener(new HandlerExecutor(new Handler(looper)), listener);
	}

	/* AOSP: without TELEPHONY_REGISTRY_SERVICE the caller is told on its listener that registering failed */
	public void addOnSubscriptionsChangedListener(final Executor executor, final OnSubscriptionsChangedListener listener) {
		if (ModemManagerPhone.get() == null) {
			executor.execute(() -> listener.onAddListenerFailed());
			return;
		}
		removeOnSubscriptionsChangedListener(listener);
		/* TelephonyRegistry: once right away, then whenever the subscriptions change */
		Runnable notify = new Runnable() {
			private String last;

			public void run() {
				SubscriptionInfo info = modemSubscription();
				String now = String.valueOf(info);
				if (now.equals(last))
					return;
				last = now;
				executor.execute(() -> listener.onSubscriptionsChanged());
			}
		};
		sListeners.put(listener, notify);
		ModemManagerPhone.addListener(notify);
		notify.run();
	}

	private static final java.util.Map<OnSubscriptionsChangedListener, Runnable> sListeners = new java.util.concurrent.ConcurrentHashMap<>();

	public void removeOnSubscriptionsChangedListener(OnSubscriptionsChangedListener listener) {
		Runnable old = listener != null ? sListeners.remove(listener) : null;
		if (old != null)
			ModemManagerPhone.removeListener(old);
	}

	/* AOSP SubscriptionManager.isNetworkRoaming(int): getPhoneId(subId) is INVALID_PHONE_INDEX without ISub, so false */
	public boolean isNetworkRoaming(int subId) {
		ModemManagerPhone phone = ModemManagerPhone.get();
		if (phone != null && (subId == ModemManagerPhone.SUB_ID || subId == DEFAULT_SUBSCRIPTION_ID) && modemSubId() == ModemManagerPhone.SUB_ID)
			return phone.isRoaming();
		return false;
	}

	/* AOSP SubscriptionManager members with TelephonyManager.getSubscriptionService() == null (no ISub on the desktop):
	 * the null branch of each AOSP method (count/default value, IllegalStateException, or nothing) */
	public interface OnOpportunisticSubscriptionsChangedListener {
		void onOpportunisticSubscriptionsChanged();
	}
	public static final int PHONE_NUMBER_SOURCE_UICC = 1;
	public static final int PHONE_NUMBER_SOURCE_CARRIER = 2;
	public static final int PHONE_NUMBER_SOURCE_IMS = 3;

	public static boolean isValidSlotIndex(int slotIndex) {
		return slotIndex >= 0 && slotIndex < TelephonyManager.getDefault().getActiveModemCount();
	}
	public SubscriptionManager createForAllUserProfiles() { return this; }
	public void addOnOpportunisticSubscriptionsChangedListener(java.util.concurrent.Executor executor, OnOpportunisticSubscriptionsChangedListener listener) {}
	public void removeOnOpportunisticSubscriptionsChangedListener(OnOpportunisticSubscriptionsChangedListener listener) {
		if (listener == null)
			throw new NullPointerException("listener cannot be null");
	}
	/* AOSP SubscriptionManagerService.isSubscriptionAssociatedWithUser(): a subscription without a user association
	 * belongs to every user (ATL has one); without the modem's SIM no subscription exists */
	public boolean isSubscriptionAssociatedWithUser(int subscriptionId) {
		return isActiveSubscriptionId(subscriptionId);
	}
	public int getActiveSubscriptionInfoCount() { return getActiveSubscriptionInfoList().size(); }
	/* AOSP: the number of SIM slots (TelephonyManager.getSimCount()) */
	public int getActiveSubscriptionInfoCountMax() { return ModemManagerPhone.get() != null ? 1 : 0; }
	public SubscriptionInfo getActiveSubscriptionInfoForSimSlotIndex(int slotIndex) {
		return slotIndex == ModemManagerPhone.SLOT_INDEX ? modemSubscription() : null;
	}
	public java.util.List<SubscriptionInfo> getAllSubscriptionInfoList() { return getActiveSubscriptionInfoList(); }
	/** @hide AOSP: the subscriptions of the user (ATL has one) */
	public java.util.List<SubscriptionInfo> getSubscriptionInfoListAssociatedWithUser(android.os.UserHandle userHandle) {
		/* AOSP builds a new ArrayList the caller may change (ProviderUtil.getSelectionBySubIds() adds to it) */
		return new java.util.ArrayList<SubscriptionInfo>(getActiveSubscriptionInfoList());
	}
	/** @hide AOSP: the user a subscription belongs to; null when none is set */
	public android.os.UserHandle getSubscriptionUserHandle(int subId) {
		return null;
	}
	public java.util.List<SubscriptionInfo> getOpportunisticSubscriptions() { return new java.util.ArrayList<>(); }
	public java.util.List<SubscriptionInfo> getSubscriptionsInGroup(android.os.ParcelUuid groupUuid) {
		if (groupUuid == null)
			throw new NullPointerException("groupUuid can't be null");
		throw new IllegalStateException("telephony service is null.");
	}
	public android.os.ParcelUuid createSubscriptionGroup(java.util.List<Integer> subIdList) {
		if (subIdList == null)
			throw new NullPointerException("can't create group for null subId list");
		throw new IllegalStateException("telephony service is null.");
	}
	public void addSubscriptionsIntoGroup(java.util.List<Integer> subIdList, android.os.ParcelUuid groupUuid) {
		if (subIdList == null)
			throw new NullPointerException("subIdList can't be null.");
		if (groupUuid == null)
			throw new NullPointerException("groupUuid can't be null.");
		throw new IllegalStateException("telephony service is null.");
	}
	public void removeSubscriptionsFromGroup(java.util.List<Integer> subIdList, android.os.ParcelUuid groupUuid) {
		if (subIdList == null)
			throw new NullPointerException("subIdList can't be null.");
		if (groupUuid == null)
			throw new NullPointerException("groupUuid can't be null.");
		throw new IllegalStateException("telephony service is null.");
	}
	public static int getDefaultSmsSubscriptionId() { return modemSubId(); }
	public static int getDefaultVoiceSubscriptionId() { return modemSubId(); }
	public static int getSubscriptionId(int slotIndex) {
		if (!isValidSlotIndex(slotIndex))
			return INVALID_SUBSCRIPTION_ID;
		return slotIndex == ModemManagerPhone.SLOT_INDEX ? modemSubId() : INVALID_SUBSCRIPTION_ID;
	}

	/** AOSP: extra of the subscription and of the slot in telephony broadcasts */
	public static final String EXTRA_SUBSCRIPTION_INDEX = "android.telephony.extra.SUBSCRIPTION_INDEX";
	public static final String EXTRA_SLOT_INDEX = "android.telephony.extra.SLOT_INDEX";

	/** @hide AOSP putPhoneIdAndSubIdExtra() with PhoneConstants.PHONE_KEY "phone" and SUBSCRIPTION_KEY "subscription" */
	public static void putPhoneIdAndSubIdExtra(android.content.Intent intent, int phoneId, int subId) {
		intent.putExtra(EXTRA_SLOT_INDEX, phoneId);
		intent.putExtra("phone", phoneId);
		putSubscriptionIdExtra(intent, subId);
	}

	/** @hide */
	public static void putSubscriptionIdExtra(android.content.Intent intent, int subId) {
		intent.putExtra(EXTRA_SUBSCRIPTION_INDEX, subId);
		intent.putExtra("subscription", subId);
	}
	public int[] getSubscriptionIds(int slotIndex) {
		if (!isValidSlotIndex(slotIndex))
			return null;
		return new int[] {getSubscriptionId(slotIndex)};
	}
	public boolean isActiveSubscriptionId(int subscriptionId) {
		return subscriptionId == ModemManagerPhone.SUB_ID && modemSubId() == ModemManagerPhone.SUB_ID;
	}
	public boolean isSubscriptionAssociatedWithUser(int subscriptionId, android.os.UserHandle userHandle) {
		if (!isValidSubscriptionId(subscriptionId))
			throw new IllegalArgumentException("[isSubscriptionAssociatedWithUser]: Invalid subscriptionId: " + subscriptionId);
		return isActiveSubscriptionId(subscriptionId);
	}
	public String getPhoneNumber(int subscriptionId, int source) {
		if (source != PHONE_NUMBER_SOURCE_UICC && source != PHONE_NUMBER_SOURCE_CARRIER && source != PHONE_NUMBER_SOURCE_IMS)
			throw new IllegalArgumentException("invalid source " + source);
		throw new IllegalStateException("subscription service unavailable.");
	}
	public void setCarrierPhoneNumber(int subscriptionId, String number) {
		if (number == null)
			throw new NullPointerException("invalid number null");
		throw new IllegalStateException("subscription service unavailable.");
	}
	public java.util.List<android.net.Uri> getDeviceToDeviceStatusSharingContacts(int subscriptionId) { return new java.util.ArrayList<>(); }

	/* ATL: constants, IntDef annotations and helpers that the full SubscriptionInfo needs; AOSP values (SimInfo.* resolved:
	 * android.provider.Telephony.SimInfo of android-16.0.0_r4). */
	@Retention(RetentionPolicy.SOURCE)
	@IntDef(prefix = {"SUBSCRIPTION_TYPE_"}, value = {SUBSCRIPTION_TYPE_LOCAL_SIM, SUBSCRIPTION_TYPE_REMOTE_SIM})
	public @interface SubscriptionType {}
	public static final int SUBSCRIPTION_TYPE_LOCAL_SIM = 0;
	public static final int SUBSCRIPTION_TYPE_REMOTE_SIM = 1;

	public static final int NAME_SOURCE_UNKNOWN = -1;
	public static final int NAME_SOURCE_CARRIER_ID = 0;
	public static final int NAME_SOURCE_SIM_SPN = 1;
	public static final int NAME_SOURCE_USER_INPUT = 2;
	public static final int NAME_SOURCE_CARRIER = 3;
	public static final int NAME_SOURCE_SIM_PNN = 4;
	@Retention(RetentionPolicy.SOURCE)
	@IntDef(prefix = {"NAME_SOURCE_"}, value = {NAME_SOURCE_UNKNOWN, NAME_SOURCE_CARRIER_ID, NAME_SOURCE_SIM_SPN,
			NAME_SOURCE_USER_INPUT, NAME_SOURCE_CARRIER, NAME_SOURCE_SIM_PNN})
	public @interface SimDisplayNameSource {}

	public static final int DATA_ROAMING_DISABLE = 0;

	public static final int PROFILE_CLASS_TESTING = 0;
	public static final int PROFILE_CLASS_PROVISIONING = 1;
	public static final int PROFILE_CLASS_OPERATIONAL = 2;
	public static final int PROFILE_CLASS_UNSET = -1;
	@Retention(RetentionPolicy.SOURCE)
	@IntDef(prefix = {"PROFILE_CLASS_"}, value = {PROFILE_CLASS_TESTING, PROFILE_CLASS_PROVISIONING,
			PROFILE_CLASS_OPERATIONAL, PROFILE_CLASS_UNSET})
	public @interface ProfileClass {}

	public static final int USAGE_SETTING_UNKNOWN = -1;
	public static final int USAGE_SETTING_DEFAULT = 0;
	public static final int USAGE_SETTING_VOICE_CENTRIC = 1;
	public static final int USAGE_SETTING_DATA_CENTRIC = 2;
	@Retention(RetentionPolicy.SOURCE)
	@IntDef(prefix = {"USAGE_SETTING_"}, value = {USAGE_SETTING_UNKNOWN, USAGE_SETTING_DEFAULT,
			USAGE_SETTING_VOICE_CENTRIC, USAGE_SETTING_DATA_CENTRIC})
	public @interface UsageSetting {}

	public static final int SERVICE_CAPABILITY_VOICE = 1;
	public static final int SERVICE_CAPABILITY_SMS = 2;
	public static final int SERVICE_CAPABILITY_DATA = 3;
	public static final int SERVICE_CAPABILITY_MAX = SERVICE_CAPABILITY_DATA;
	@Retention(RetentionPolicy.SOURCE)
	@IntDef(prefix = {"SERVICE_CAPABILITY"}, value = {SERVICE_CAPABILITY_VOICE, SERVICE_CAPABILITY_SMS,
			SERVICE_CAPABILITY_DATA})
	public @interface ServiceCapability {}

	public static final int TRANSFER_STATUS_NONE = 0;
	public static final int TRANSFER_STATUS_TRANSFERRED_OUT = 1;
	public static final int TRANSFER_STATUS_CONVERTED = 2;
	@Retention(RetentionPolicy.SOURCE)
	@IntDef(prefix = {"TRANSFER_STATUS"}, value = {TRANSFER_STATUS_NONE, TRANSFER_STATUS_TRANSFERRED_OUT,
			TRANSFER_STATUS_CONVERTED})
	public @interface TransferStatus {}

	@NonNull
	public static String displayNameSourceToString(@SimDisplayNameSource int source) {
		switch (source) {
			case NAME_SOURCE_UNKNOWN: return "UNKNOWN";
			case NAME_SOURCE_CARRIER_ID: return "CARRIER_ID";
			case NAME_SOURCE_SIM_SPN: return "SIM_SPN";
			case NAME_SOURCE_USER_INPUT: return "USER_INPUT";
			case NAME_SOURCE_CARRIER: return "CARRIER";
			case NAME_SOURCE_SIM_PNN: return "SIM_PNN";
			default:
				return "UNKNOWN(" + source + ")";
		}
	}

	@NonNull
	public static String subscriptionTypeToString(@SubscriptionType int type) {
		switch (type) {
			case SUBSCRIPTION_TYPE_LOCAL_SIM: return "LOCAL_SIM";
			case SUBSCRIPTION_TYPE_REMOTE_SIM: return "REMOTE_SIM";
			default:
				return "UNKNOWN(" + type + ")";
		}
	}

	@NonNull
	public static String usageSettingToString(@UsageSetting int usageSetting) {
		switch (usageSetting) {
			case USAGE_SETTING_UNKNOWN: return "UNKNOWN";
			case USAGE_SETTING_DEFAULT: return "DEFAULT";
			case USAGE_SETTING_VOICE_CENTRIC: return "VOICE_CENTRIC";
			case USAGE_SETTING_DATA_CENTRIC: return "DATA_CENTRIC";
			default:
				return "UNKNOWN(" + usageSetting + ")";
		}
	}

	@NonNull
	@ServiceCapability
	public static Set<Integer> getServiceCapabilitiesSet(int combinedServiceCapabilities) {
		Set<Integer> capabilities = new HashSet<>();
		for (int i = SERVICE_CAPABILITY_VOICE; i <= SERVICE_CAPABILITY_MAX; i++) {
			final int capabilityBitmask = serviceCapabilityToBitmask(i);
			if ((combinedServiceCapabilities & capabilityBitmask) == capabilityBitmask) {
				capabilities.add(i);
			}
		}
		return Collections.unmodifiableSet(capabilities);
	}

	public static int serviceCapabilityToBitmask(@ServiceCapability int capability) {
		return 1 << (capability - 1);
	}
}
