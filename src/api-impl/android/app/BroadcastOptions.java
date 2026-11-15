package android.app;

import android.os.Bundle;

/* AOSP BroadcastOptions (android-16), the subset apps use: makeBasic(), the share identity flag
 * and toBundle() with AOSP's key. ATL's broadcasts do not read the options. */
public class BroadcastOptions {
	private static final String KEY_FLAGS = "android:broadcast.flags";
	private static final int FLAG_SHARE_IDENTITY = 1 << 4;

	private static final String KEY_DELIVERY_GROUP_POLICY = "android:broadcast.deliveryGroupPolicy";
	private static final String KEY_DELIVERY_GROUP_NAMESPACE = "android:broadcast.deliveryGroupMatchingNamespace";
	private static final String KEY_DELIVERY_GROUP_KEY = "android:broadcast.deliveryGroupMatchingKey";
	private static final String KEY_DEFERRAL_POLICY = "android:broadcast.deferralPolicy";

	public static final int DELIVERY_GROUP_POLICY_ALL = 0;
	public static final int DELIVERY_GROUP_POLICY_MOST_RECENT = 1;
	public static final int DELIVERY_GROUP_POLICY_MERGED = 2;
	public static final int DEFERRAL_POLICY_DEFAULT = 0;
	public static final int DEFERRAL_POLICY_NONE = 1;
	public static final int DEFERRAL_POLICY_UNTIL_ACTIVE = 2;

	private int mFlags;
	private int mDeliveryGroupPolicy = DELIVERY_GROUP_POLICY_ALL;
	private String mDeliveryGroupMatchingNamespaceFragment;
	private String mDeliveryGroupMatchingKeyFragment;
	private int mDeferralPolicy = DEFERRAL_POLICY_DEFAULT;

	public static BroadcastOptions makeBasic() {
		return new BroadcastOptions();
	}

	public BroadcastOptions() {}

	public BroadcastOptions setShareIdentityEnabled(boolean shareIdentityEnabled) {
		if (shareIdentityEnabled) {
			mFlags |= FLAG_SHARE_IDENTITY;
		} else {
			mFlags &= ~FLAG_SHARE_IDENTITY;
		}
		return this;
	}

	public boolean isShareIdentityEnabled() {
		return (mFlags & FLAG_SHARE_IDENTITY) != 0;
	}

	/* AOSP BroadcastOptions(Bundle): the fields above read back from toBundle() */
	public static BroadcastOptions fromBundle(Bundle options) {
		BroadcastOptions opts = new BroadcastOptions();
		opts.mFlags = options.getInt(KEY_FLAGS, 0);
		opts.mDeliveryGroupPolicy = options.getInt(KEY_DELIVERY_GROUP_POLICY, DELIVERY_GROUP_POLICY_ALL);
		opts.mDeliveryGroupMatchingNamespaceFragment = options.getString(KEY_DELIVERY_GROUP_NAMESPACE);
		opts.mDeliveryGroupMatchingKeyFragment = options.getString(KEY_DELIVERY_GROUP_KEY);
		opts.mDeferralPolicy = options.getInt(KEY_DEFERRAL_POLICY, DEFERRAL_POLICY_DEFAULT);
		return opts;
	}

	public BroadcastOptions setDeferralPolicy(int deferralPolicy) {
		mDeferralPolicy = deferralPolicy;
		return this;
	}

	public int getDeferralPolicy() {
		return mDeferralPolicy;
	}

	public void clearDeferralPolicy() {
		mDeferralPolicy = DEFERRAL_POLICY_DEFAULT;
	}

	public BroadcastOptions setDeliveryGroupPolicy(int policy) {
		mDeliveryGroupPolicy = policy;
		return this;
	}

	public int getDeliveryGroupPolicy() {
		return mDeliveryGroupPolicy;
	}

	public void clearDeliveryGroupPolicy() {
		mDeliveryGroupPolicy = DELIVERY_GROUP_POLICY_ALL;
	}

	public BroadcastOptions setDeliveryGroupMatchingKey(String namespace, String key) {
		mDeliveryGroupMatchingNamespaceFragment = java.util.Objects.requireNonNull(namespace);
		mDeliveryGroupMatchingKeyFragment = java.util.Objects.requireNonNull(key);
		return this;
	}

	public String getDeliveryGroupMatchingKey() {
		if (mDeliveryGroupMatchingNamespaceFragment == null || mDeliveryGroupMatchingKeyFragment == null)
			return null;
		return String.join(":", mDeliveryGroupMatchingNamespaceFragment, mDeliveryGroupMatchingKeyFragment);
	}

	public void clearDeliveryGroupMatchingKey() {
		mDeliveryGroupMatchingNamespaceFragment = null;
		mDeliveryGroupMatchingKeyFragment = null;
	}

	public Bundle toBundle() {
		Bundle b = new Bundle();
		if (mFlags != 0) {
			b.putInt(KEY_FLAGS, mFlags);
		}
		if (mDeliveryGroupPolicy != DELIVERY_GROUP_POLICY_ALL)
			b.putInt(KEY_DELIVERY_GROUP_POLICY, mDeliveryGroupPolicy);
		if (mDeliveryGroupMatchingNamespaceFragment != null)
			b.putString(KEY_DELIVERY_GROUP_NAMESPACE, mDeliveryGroupMatchingNamespaceFragment);
		if (mDeliveryGroupMatchingKeyFragment != null)
			b.putString(KEY_DELIVERY_GROUP_KEY, mDeliveryGroupMatchingKeyFragment);
		if (mDeferralPolicy != DEFERRAL_POLICY_DEFAULT)
			b.putInt(KEY_DEFERRAL_POLICY, mDeferralPolicy);
		return b;
	}
}
