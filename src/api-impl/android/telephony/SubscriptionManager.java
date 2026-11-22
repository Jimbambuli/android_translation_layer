package android.telephony;

import android.Manifest;
import android.annotation.DurationMillisLong;
import android.annotation.RequiresFeature;
import android.annotation.RequiresPermission;
import android.annotation.SdkConstant;
import android.annotation.SdkConstant.SdkConstantType;
import android.annotation.SuppressAutoDoc;
import android.annotation.SystemApi;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.ParcelUuid;
import android.os.UserHandle;
import android.provider.Telephony.SimInfo;
import android.telephony.euicc.EuiccManager;
import com.android.internal.util.Preconditions;
import android.telephony.Rlog;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
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

    /**
     * Device status is not shared to a remote party.
     */
    public static final int D2D_SHARING_DISABLED = 0;

    /**
     * Device status is shared with all numbers in the user's contacts.
     */
    public static final int D2D_SHARING_ALL_CONTACTS = 1;

    /**
     * Device status is shared with all selected contacts.
     */
    public static final int D2D_SHARING_SELECTED_CONTACTS = 2;

    /**
     * Device status is shared whenever possible.
     */
    public static final int D2D_SHARING_ALL = 3;

    /**
     * TelephonyProvider column name for device to device sharing status.
     * <P>Type: INTEGER (int)</P>
     */
    public static final String D2D_STATUS_SHARING = SimInfo.COLUMN_D2D_STATUS_SHARING;

    /**
     * TelephonyProvider column name for contacts information that allow device to device sharing.
     * <P>Type: TEXT (String)</P>
     */
    public static final String D2D_STATUS_SHARING_SELECTED_CONTACTS =
            SimInfo.COLUMN_D2D_STATUS_SHARING_SELECTED_CONTACTS;

    /** Indicates that data roaming is enabled for a subscription */
    public static final int DATA_ROAMING_ENABLE = SimInfo.DATA_ROAMING_ENABLE;

    /**
     * Broadcast Action: The default subscription has changed.  This has the following
     * extra values:</p>
     * The {@link #EXTRA_SUBSCRIPTION_INDEX} extra indicates the current default subscription index
     */
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_DEFAULT_SUBSCRIPTION_CHANGED
            = "android.telephony.action.DEFAULT_SUBSCRIPTION_CHANGED";

    /**
     * Broadcast Action: The default sms subscription has changed.  This has the following
     * extra values:</p>
     * {@link #EXTRA_SUBSCRIPTION_INDEX} extra indicates the current default sms
     * subscription index
     */
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_DEFAULT_SMS_SUBSCRIPTION_CHANGED
            = "android.telephony.action.DEFAULT_SMS_SUBSCRIPTION_CHANGED";

    /**
     * Activity Action: Display UI for managing the billing relationship plans
     * between a carrier and a specific subscriber.
     * <p>
     * Carrier apps are encouraged to implement this activity, and the OS will
     * provide an affordance to quickly enter this activity, typically via
     * Settings. This affordance will only be shown when the carrier app is
     * actively providing subscription plan information via
     * {@link #setSubscriptionPlans(int, List)}.
     * <p>
     * Contains {@link #EXTRA_SUBSCRIPTION_INDEX} to indicate which subscription
     * the user is interested in.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_MANAGE_SUBSCRIPTION_PLANS
            = "android.telephony.action.MANAGE_SUBSCRIPTION_PLANS";

    /**
     * Broadcast Action: Request a refresh of the billing relationship plans
     * between a carrier and a specific subscriber.
     * <p>
     * Carrier apps are encouraged to implement this receiver, and the OS will
     * provide an affordance to request a refresh. This affordance will only be
     * shown when the carrier app is actively providing subscription plan
     * information via {@link #setSubscriptionPlans(int, List)}.
     * <p>
     * Contains {@link #EXTRA_SUBSCRIPTION_INDEX} to indicate which subscription
     * the user is interested in.
     * <p>
     * Receivers should protect themselves by checking that the sender holds the
     * {@code android.permission.MANAGE_SUBSCRIPTION_PLANS} permission.
     */
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_REFRESH_SUBSCRIPTION_PLANS
            = "android.telephony.action.REFRESH_SUBSCRIPTION_PLANS";

    /**
     * Get the description of the billing relationship plan between a carrier
     * and a specific subscriber.
     * <p>
     * This method is only accessible to the following narrow set of apps:
     * <ul>
     * <li>The carrier app for this subscriberId, as determined by
     * {@link TelephonyManager#hasCarrierPrivileges()}.
     * <li>The carrier app explicitly delegated access through
     * {@link CarrierConfigManager#KEY_CONFIG_PLANS_PACKAGE_OVERRIDE_STRING}.
     * </ul>
     *
     * @param subId the subscriber this relationship applies to
     * @throws SecurityException if the caller doesn't meet the requirements
     *             outlined above.
     */
    public @NonNull List<SubscriptionPlan> getSubscriptionPlans(int subId) {
        // ATL: NetworkPolicyManagerService stores the plans an owner app set with setSubscriptionPlans();
        // nothing sets them in ATL, AOSP then returns no plans
        return Collections.emptyList();
    }

    /**
     * Set the description of the billing relationship plan between a carrier
     * and a specific subscriber.
     * <p>
     * This method is only accessible to the following narrow set of apps:
     * <ul>
     * <li>The carrier app for this subscriberId, as determined by
     * {@link TelephonyManager#hasCarrierPrivileges()}.
     * <li>The carrier app explicitly delegated access through
     * {@link CarrierConfigManager#KEY_CONFIG_PLANS_PACKAGE_OVERRIDE_STRING}.
     * </ul>
     *
     * @param subId the subscriber this relationship applies to. An empty list
     *            may be sent to clear any existing plans.
     * @param plans the list of plans. The first plan is always the primary and
     *            most important plan. Any additional plans are secondary and
     *            may not be displayed or used by decision making logic.
     * @throws SecurityException if the caller doesn't meet the requirements
     *             outlined above.
     * @throws IllegalArgumentException if plans don't meet the requirements
     *             defined in {@link SubscriptionPlan}.
     * @deprecated use {@link #setSubscriptionPlans(int, List, long)} instead.
     */
    @Deprecated
    public void setSubscriptionPlans(int subId, @NonNull List<SubscriptionPlan> plans) {
        setSubscriptionPlans(subId, plans, 0);
    }

    /**
     * Set the description of the billing relationship plan between a carrier
     * and a specific subscriber.
     * <p>
     * This method is only accessible to the following narrow set of apps:
     * <ul>
     * <li>The carrier app for this subscriberId, as determined by
     * {@link TelephonyManager#hasCarrierPrivileges()}.
     * <li>The carrier app explicitly delegated access through
     * {@link CarrierConfigManager#KEY_CONFIG_PLANS_PACKAGE_OVERRIDE_STRING}.
     * </ul>
     *
     * @param subId the subscriber this relationship applies to. An empty list
     *            may be sent to clear any existing plans.
     * @param plans the list of plans. The first plan is always the primary and
     *            most important plan. Any additional plans are secondary and
     *            may not be displayed or used by decision making logic.
     * @param expirationDurationMillis the duration after which the subscription plans
     *            will be automatically cleared, or {@code 0} to leave the plans until
     *            explicitly cleared, or the next reboot, whichever happens first.
     * @throws SecurityException if the caller doesn't meet the requirements
     *             outlined above.
     * @throws IllegalArgumentException if plans don't meet the requirements
     *             defined in {@link SubscriptionPlan}.
     */
    public void setSubscriptionPlans(int subId, @NonNull List<SubscriptionPlan> plans,
            @DurationMillisLong long expirationDurationMillis) {
        Preconditions.checkNotNull(plans, "plans cannot be null");
        throw new SecurityException("Caller " + currentPackageName()
                + " is not the owner of subscription plans for " + subId
                + " (requires MANAGE_SUBSCRIPTION_PLANS or carrier privileges)");
    }

    /**
     * Temporarily override the billing relationship plan between a carrier and
     * a specific subscriber to be considered unmetered. This will be reflected
     * to apps via {@link NetworkCapabilities#NET_CAPABILITY_NOT_METERED}.
     * <p>
     * This method is only accessible to the following narrow set of apps:
     * <ul>
     * <li>The carrier app for this subscriberId, as determined by
     * {@link TelephonyManager#hasCarrierPrivileges()}.
     * <li>The carrier app explicitly delegated access through
     * {@link CarrierConfigManager#KEY_CONFIG_PLANS_PACKAGE_OVERRIDE_STRING}.
     * </ul>
     *
     * @param subId the subscriber this override applies to.
     * @param overrideUnmetered set if the billing relationship should be
     *            considered unmetered.
     * @param expirationDurationMillis the duration after which the requested override
     *            will be automatically cleared, or {@code 0} to leave in the
     *            requested state until explicitly cleared, or the next reboot,
     *            whichever happens first.
     * @throws SecurityException if the caller doesn't meet the requirements
     *            outlined above.
     */
    public void setSubscriptionOverrideUnmetered(int subId, boolean overrideUnmetered,
            @DurationMillisLong long expirationDurationMillis) {
        setSubscriptionOverrideUnmetered(subId, overrideUnmetered,
                TelephonyManager.getAllNetworkTypes(), expirationDurationMillis);
    }

    /**
     * Temporarily override the billing relationship plan between a carrier and
     * a specific subscriber to be considered unmetered. This will be reflected
     * to apps via {@link NetworkCapabilities#NET_CAPABILITY_NOT_METERED}.
     * <p>
     * This method is only accessible to the following narrow set of apps:
     * <ul>
     * <li>The carrier app for this subscriberId, as determined by
     * {@link TelephonyManager#hasCarrierPrivileges()}.
     * <li>The carrier app explicitly delegated access through
     * {@link CarrierConfigManager#KEY_CONFIG_PLANS_PACKAGE_OVERRIDE_STRING}.
     * </ul>
     *
     * @param subId the subscriber this override applies to.
     * @param overrideUnmetered set if the billing relationship should be
     *            considered unmetered.
     * @param networkTypes the network types this override applies to. If no
     *            network types are specified, override values will be ignored.
     * @param expirationDurationMillis the duration after which the requested override
     *            will be automatically cleared, or {@code 0} to leave in the
     *            requested state until explicitly cleared, or the next reboot,
     *            whichever happens first.
     * @throws SecurityException if the caller doesn't meet the requirements
     *            outlined above.
     */
    public void setSubscriptionOverrideUnmetered(int subId, boolean overrideUnmetered,
            @NonNull @Annotation.NetworkType int[] networkTypes,
            @DurationMillisLong long expirationDurationMillis) {
        throw new SecurityException("Caller " + currentPackageName()
                + " is not the owner of subscription plans for " + subId
                + " (requires MANAGE_SUBSCRIPTION_PLANS or carrier privileges)");
    }

    /**
     * Temporarily override the billing relationship plan between a carrier and
     * a specific subscriber to be considered congested. This will cause the
     * device to delay certain network requests when possible, such as developer
     * jobs that are willing to run in a flexible time window.
     * <p>
     * This method is only accessible to the following narrow set of apps:
     * <ul>
     * <li>The carrier app for this subscriberId, as determined by
     * {@link TelephonyManager#hasCarrierPrivileges()}.
     * <li>The carrier app explicitly delegated access through
     * {@link CarrierConfigManager#KEY_CONFIG_PLANS_PACKAGE_OVERRIDE_STRING}.
     * </ul>
     *
     * @param subId the subscriber this override applies to.
     * @param overrideCongested set if the subscription should be considered
     *            congested.
     * @param expirationDurationMillis the duration after which the requested override
     *            will be automatically cleared, or {@code 0} to leave in the
     *            requested state until explicitly cleared, or the next reboot,
     *            whichever happens first.
     * @throws SecurityException if the caller doesn't meet the requirements
     *            outlined above.
     */
    public void setSubscriptionOverrideCongested(int subId, boolean overrideCongested,
            @DurationMillisLong long expirationDurationMillis) {
        setSubscriptionOverrideCongested(subId, overrideCongested,
                TelephonyManager.getAllNetworkTypes(), expirationDurationMillis);
    }

    /**
     * Temporarily override the billing relationship plan between a carrier and
     * a specific subscriber to be considered congested. This will cause the
     * device to delay certain network requests when possible, such as developer
     * jobs that are willing to run in a flexible time window.
     * <p>
     * This method is only accessible to the following narrow set of apps:
     * <ul>
     * <li>The carrier app for this subscriberId, as determined by
     * {@link TelephonyManager#hasCarrierPrivileges()}.
     * <li>The carrier app explicitly delegated access through
     * {@link CarrierConfigManager#KEY_CONFIG_PLANS_PACKAGE_OVERRIDE_STRING}.
     * </ul>
     *
     * @param subId the subscriber this override applies to.
     * @param overrideCongested set if the subscription should be considered congested.
     * @param networkTypes the network types this override applies to. If no network types are
     * specified, override values will be ignored.
     * @param expirationDurationMillis the duration after which the requested override
     * will be automatically cleared, or {@code 0} to leave in the requested state until explicitly
     * cleared, or the next reboot, whichever happens first.
     *
     * @throws SecurityException if the caller doesn't meet the requirements outlined above.
     */
    public void setSubscriptionOverrideCongested(int subId, boolean overrideCongested,
            @NonNull @Annotation.NetworkType int[] networkTypes,
            @DurationMillisLong long expirationDurationMillis) {
        throw new SecurityException("Caller " + currentPackageName()
                + " is not the owner of subscription plans for " + subId
                + " (requires MANAGE_SUBSCRIPTION_PLANS or carrier privileges)");
    }

    /**
     * Checks whether the app with the given context is authorized to manage the given subscription
     * according to its metadata.
     *
     * Only supported for embedded subscriptions (if {@link SubscriptionInfo#isEmbedded} returns
     * true). To check for permissions for non-embedded subscription as well,
     * see {@link android.telephony.TelephonyManager#hasCarrierPrivileges}.
     *
     * @param info The subscription to check.
     * @return whether the app is authorized to manage this subscription per its metadata.
     * @see android.telephony.TelephonyManager#hasCarrierPrivileges
     */
    public boolean canManageSubscription(SubscriptionInfo info) {
        return canManageSubscription(info, currentPackageName());
    }

    /**
     * Checks whether the given app is authorized to manage the given subscription. An app can only
     * be authorized if it is included in the {@link android.telephony.UiccAccessRule} of the
     * {@link android.telephony.SubscriptionInfo} with the access status.
     *
     * Only supported for embedded subscriptions (if {@link SubscriptionInfo#isEmbedded} returns
     * true). To check for permissions for non-embedded subscription as well,
     * see {@link android.telephony.TelephonyManager#hasCarrierPrivileges}.
     *
     * @param info The subscription to check.
     * @param packageName Package name of the app to check.
     *
     * @return whether the app is authorized to manage this subscription per its access rules.
     * @see android.telephony.TelephonyManager#hasCarrierPrivileges
     * @hide
     */
    @SystemApi
    public boolean canManageSubscription(@NonNull SubscriptionInfo info,
            @NonNull String packageName) {
        if (info == null || packageName == null) {
            return false;
        }
        return info.canManageSubscription(android.app.ActivityThread.currentApplication(), packageName);
    }

    /**
     * Switch to a certain subscription
     *
     *  @param subId sub id
     *  @param callbackIntent pending intent that will be sent after operation is done.
     *
     *  @deprecated this API is a duplicate of {@link EuiccManager#switchToSubscription(int,
     *  PendingIntent)} and does not support Multiple Enabled Profile(MEP). Apps should use
     *  {@link EuiccManager#switchToSubscription(int, PendingIntent)} or
     *  {@link EuiccManager#switchToSubscription(int, int, PendingIntent)} instead.
     *
     * @throws UnsupportedOperationException If the device does not have
     *          {@link PackageManager#FEATURE_TELEPHONY_EUICC}.
     */
    @RequiresPermission(android.Manifest.permission.WRITE_EMBEDDED_SUBSCRIPTIONS)
    @RequiresFeature(PackageManager.FEATURE_TELEPHONY_EUICC)
    @Deprecated
    public void switchToSubscription(int subId, @NonNull PendingIntent callbackIntent) {
        Preconditions.checkNotNull(callbackIntent, "callbackIntent cannot be null");
        EuiccManager euiccManager = new EuiccManager(android.app.ActivityThread.currentApplication());
        euiccManager.switchToSubscription(subId, callbackIntent);
    }

    /**
     * Set whether a subscription is opportunistic, that is, whether the network it connects
     * to has limited coverage. For example, CBRS. Setting a subscription opportunistic has
     * following impacts:
     *  1) Even if it's active, it will be dormant most of the time. The modem will not try
     *     to scan or camp until it knows an available network is nearby to save power.
     *  2) Telephony relies on system app or carrier input to notify nearby available networks.
     *     See {@link TelephonyManager#updateAvailableNetworks(List, Executor, Consumer)}
     *     for more information.
     *  3) In multi-SIM devices, when the network is nearby and camped, system may automatically
     *     switch internet data between it and default data subscription, based on carrier
     *     recommendation and its signal strength and metered-ness, etc.
     *
     *
     * Caller will either have {@link android.Manifest.permission#MODIFY_PHONE_STATE} or carrier
     * privilege permission of the subscription.
     *
     * @param opportunistic whether it's an opportunistic subscription.
     * @param subId the unique SubscriptionInfo index in database
     * @return {@code true} if the operation is succeed, {@code false} otherwise.
     *
     * @throws UnsupportedOperationException If the device does not have
     *          {@link PackageManager#FEATURE_TELEPHONY_SUBSCRIPTION}.
     */
    @SuppressAutoDoc // Blocked by b/72967236 - no support for carrier privileges
    @RequiresPermission(android.Manifest.permission.MODIFY_PHONE_STATE)
    public boolean setOpportunistic(boolean opportunistic, int subId) {
        if (VDBG) logd("[setOpportunistic]+ opportunistic:" + opportunistic + " subId:" + subId);
        // ATL: SubscriptionController enforces MODIFY_PHONE_STATE or carrier privileges, an app has neither
        throw new SecurityException("setOpportunistic: requires MODIFY_PHONE_STATE permission or carrier privileges");
    }

    /**
     * Set the device to device status sharing user preference for a subscription id. The setting
     * app uses this method to indicate with whom they wish to share device to device status
     * information.
     *
     * @param subscriptionId The subscription id.
     * @param sharing The status sharing preference.
     *
     * @throws SecurityException if the caller doesn't have permissions required.
     * @throws UnsupportedOperationException If the device does not have
     *          {@link PackageManager#FEATURE_TELEPHONY_SUBSCRIPTION}.
     */
    @RequiresPermission(Manifest.permission.MODIFY_PHONE_STATE)
    public void setDeviceToDeviceStatusSharingPreference(int subscriptionId,
            @DeviceToDeviceStatusSharingPreference int sharing) {
        if (VDBG) {
            logd("[setDeviceToDeviceStatusSharing] + sharing: " + sharing + " subId: "
                    + subscriptionId);
        }
        throw new SecurityException("setDeviceToDeviceStatusSharing: requires MODIFY_PHONE_STATE permission");
    }

    /**
     * Returns the user-chosen device to device status sharing preference
     * @param subscriptionId Subscription id of subscription
     * @return The device to device status sharing preference
     *
     * @throws SecurityException if the caller doesn't have permissions required.
     * @throws UnsupportedOperationException If the device does not have
     *          {@link PackageManager#FEATURE_TELEPHONY_SUBSCRIPTION}.
     */
    public @DeviceToDeviceStatusSharingPreference int getDeviceToDeviceStatusSharingPreference(
            int subscriptionId) {
        if (VDBG) {
            logd("[getDeviceToDeviceStatusSharing] + subId: " + subscriptionId);
        }
        // the subscription's stored value is the column default
        return D2D_SHARING_DISABLED;
    }

    /**
     * Set the list of contacts that allow device to device status sharing for a subscription id.
     * The setting app uses this method to indicate with whom they wish to share device to device
     * status information.
     *
     * @param subscriptionId The subscription id.
     * @param contacts The list of contacts that allow device to device status sharing.
     *
     * @throws SecurityException if the caller doesn't have permissions required.
     * @throws UnsupportedOperationException If the device does not have
     *          {@link PackageManager#FEATURE_TELEPHONY_SUBSCRIPTION}.
     */
    @RequiresPermission(Manifest.permission.MODIFY_PHONE_STATE)
    public void setDeviceToDeviceStatusSharingContacts(int subscriptionId,
            @NonNull List<Uri> contacts) {
        if (VDBG) {
            logd("[setDeviceToDeviceStatusSharingContacts] + subId: " + subscriptionId);
        }
        throw new SecurityException("setDeviceToDeviceStatusSharingContacts: requires MODIFY_PHONE_STATE permission");
    }

    /**
     * A listener class for monitoring changes to {@link SubscriptionInfo} records of opportunistic
     * subscriptions.
     * <p>
     * Override the onOpportunisticSubscriptionsChanged method in the object that extends this
     * or {@link #addOnOpportunisticSubscriptionsChangedListener(
     * Executor, OnOpportunisticSubscriptionsChangedListener)}
     * to register your listener and to unregister invoke
     * {@link #removeOnOpportunisticSubscriptionsChangedListener(
     * OnOpportunisticSubscriptionsChangedListener)}
     * <p>
     * Permissions android.Manifest.permission.READ_PHONE_STATE is required
     * for #onOpportunisticSubscriptionsChanged to be invoked.
     */
    public static class OnOpportunisticSubscriptionsChangedListener {
        /**
         * Callback invoked when there is any change to any SubscriptionInfo. Typically
         * this method would invoke {@link #getActiveSubscriptionInfoList}
         */
        public void onOpportunisticSubscriptionsChanged() {
            if (DBG) log("onOpportunisticSubscriptionsChanged: NOT OVERRIDDEN");
        }

        private void log(String s) {
            Rlog.d(LOG_TAG, s);
        }
    }

    private static String currentPackageName() {
        android.app.Application app = android.app.ActivityThread.currentApplication();
        return app != null ? app.getPackageName() : null;
    }

    private static final String LOG_TAG = "SubscriptionManager";

    private static final boolean DBG = false;

    private static final boolean VDBG = false;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(prefix = {"D2D_SHARING_"},
            value = {
                    D2D_SHARING_DISABLED,
                    D2D_SHARING_ALL_CONTACTS,
                    D2D_SHARING_SELECTED_CONTACTS,
                    D2D_SHARING_ALL
            })
    public @interface DeviceToDeviceStatusSharingPreference {}

    private static void logd(String msg) {
        Rlog.d(LOG_TAG, msg);
    }
}
