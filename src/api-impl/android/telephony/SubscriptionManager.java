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
}
