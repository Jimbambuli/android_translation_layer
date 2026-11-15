package android.telephony;

import android.os.Handler;
import android.os.HandlerExecutor;
import android.os.Looper;
import java.util.concurrent.Executor;

/* AOSP frameworks/base/telephony/java/android/telephony/SubscriptionManager.java: ATL has no ISub service and no
 * TelephonyRegistryManager, so the answers are those of a device without them */
public class SubscriptionManager {
	public static final int INVALID_SUBSCRIPTION_ID = -1;
	public static final int DEFAULT_SUBSCRIPTION_ID = Integer.MAX_VALUE;

	public static class OnSubscriptionsChangedListener {
		private final Looper mCreatorLooper;

		public OnSubscriptionsChangedListener() {
			mCreatorLooper = Looper.myLooper();
		}

		public OnSubscriptionsChangedListener(Looper looper) {
			if (looper == null) {
				throw new NullPointerException();
			}
			mCreatorLooper = looper;
		}

		public Looper getCreatorLooper() {
			return mCreatorLooper;
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
}
