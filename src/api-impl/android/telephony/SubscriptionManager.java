package android.telephony;

import android.content.Context;
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

	/* AOSP: with the TelephonyRegistry service (ATL has none) the listener is registered; without it the caller is
	 * told through onAddListenerFailed() on its executor. */
	public void addOnSubscriptionsChangedListener(OnSubscriptionsChangedListener listener) {
		if (listener == null)
			return;
		android.os.Looper looper = listener.getCreatorLooper();
		if (looper == null)
			throw new RuntimeException("Can't create handler inside thread " + Thread.currentThread()
			                           + " that has not called Looper.prepare()");
		android.os.Handler handler = new android.os.Handler(looper);
		addOnSubscriptionsChangedListener(handler::post, listener);
	}

	public void addOnSubscriptionsChangedListener(java.util.concurrent.Executor executor,
	                                              OnSubscriptionsChangedListener listener) {
		executor.execute(() -> listener.onAddListenerFailed());
	}

	public void removeOnSubscriptionsChangedListener(OnSubscriptionsChangedListener listener) {}
}
