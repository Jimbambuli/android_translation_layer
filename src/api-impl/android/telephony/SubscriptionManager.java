package android.telephony;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerExecutor;
import android.os.Looper;
import java.util.concurrent.Executor;
import java.util.Collections;
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
		return INVALID_SUBSCRIPTION_ID;
	}

	public static boolean isValidSubscriptionId(int subscriptionId) {
		return subscriptionId > INVALID_SUBSCRIPTION_ID;
	}

	public static int getSlotIndex(int subscriptionId) {
		return INVALID_SIM_SLOT_INDEX; // the cache's default when ISub is missing
	}

	public List<SubscriptionInfo> getActiveSubscriptionInfoList() {
		return Collections.emptyList();
	}

	public SubscriptionInfo getActiveSubscriptionInfo(int subId) {
		return null;
	}

	public List<SubscriptionInfo> getAccessibleSubscriptionInfoList() {
		return Collections.emptyList();
	}

	public List<SubscriptionInfo> getCompleteActiveSubscriptionInfoList() {
		return Collections.emptyList();
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
		return INVALID_SUBSCRIPTION_ID;
	}

	public static int getActiveDataSubscriptionId() {
		return INVALID_SUBSCRIPTION_ID;
	}

	/* AOSP: throws when getSubscriptionService() is null */
	public String getPhoneNumber(int subscriptionId) {
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
	public void addOnSubscriptionsChangedListener(Executor executor, OnSubscriptionsChangedListener listener) {
		executor.execute(() -> listener.onAddListenerFailed());
	}

	public void removeOnSubscriptionsChangedListener(OnSubscriptionsChangedListener listener) {}

	/* AOSP SubscriptionManager.isNetworkRoaming(int): getPhoneId(subId) is INVALID_PHONE_INDEX without ISub, so false */
	public boolean isNetworkRoaming(int subId) {
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
	/* AOSP isSubscriptionAssociatedWithUser(): needs SubscriptionManagerService; ATL has no SIM, so no subscription exists */
	public boolean isSubscriptionAssociatedWithUser(int subscriptionId) { return false; }
	public int getActiveSubscriptionInfoCount() { return 0; }
	public int getActiveSubscriptionInfoCountMax() { return 0; }
	public SubscriptionInfo getActiveSubscriptionInfoForSimSlotIndex(int slotIndex) { return null; }
	public java.util.List<SubscriptionInfo> getAllSubscriptionInfoList() { return java.util.Collections.emptyList(); }
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
	public static int getDefaultSmsSubscriptionId() { return INVALID_SUBSCRIPTION_ID; }
	public static int getDefaultVoiceSubscriptionId() { return INVALID_SUBSCRIPTION_ID; }
	public static int getSubscriptionId(int slotIndex) {
		if (!isValidSlotIndex(slotIndex))
			return INVALID_SUBSCRIPTION_ID;
		return INVALID_SUBSCRIPTION_ID;
	}
	public int[] getSubscriptionIds(int slotIndex) {
		if (!isValidSlotIndex(slotIndex))
			return null;
		return new int[] {getSubscriptionId(slotIndex)};
	}
	public boolean isActiveSubscriptionId(int subscriptionId) { return false; }
	public boolean isSubscriptionAssociatedWithUser(int subscriptionId, android.os.UserHandle userHandle) {
		if (!isValidSubscriptionId(subscriptionId))
			throw new IllegalArgumentException("[isSubscriptionAssociatedWithUser]: Invalid subscriptionId: " + subscriptionId);
		return false;
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
}
