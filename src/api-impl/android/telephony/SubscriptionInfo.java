package android.telephony;

/* An instance only comes from ISub, which ATL does not have: SubscriptionManager's lists are empty and
 * getActiveSubscriptionInfo() is null, so no object exists. The class is here for the type. */
public class SubscriptionInfo {
	SubscriptionInfo() {}

	/* Fields as in AOSP SubscriptionInfo (mId, mMcc, mMnc) before any value is read from ISub. */
	private int mId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
	private int mMcc;
	private int mMnc;
	private int mSimSlotIndex = SubscriptionManager.INVALID_SIM_SLOT_INDEX;
	private CharSequence mDisplayName;
	private String mNumber;
	private String mMcc_String;
	private String mMnc_String;

	public int getSubscriptionId() {
		return mId;
	}

	@Deprecated
	public int getMcc() {
		return mMcc;
	}

	@Deprecated
	public int getMnc() {
		return mMnc;
	}

	public int getSimSlotIndex() {
		return mSimSlotIndex;
	}

	public CharSequence getDisplayName() {
		return mDisplayName;
	}

	@Deprecated
	public String getNumber() {
		return mNumber;
	}

	public String getMccString() {
		return mMcc_String;
	}

	public String getMncString() {
		return mMnc_String;
	}
}
