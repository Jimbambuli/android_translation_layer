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

	/* AOSP SubscriptionInfo fields before any value is read from ISub */
	private CharSequence mCarrierName;
	private String mIccId;

	public CharSequence getCarrierName() {
		return mCarrierName;
	}

	public String getIccId() {
		return mIccId;
	}

	/* AOSP SubscriptionInfo.getCountryIso(): mCountryIso, not set before ISub fills it */
	private String mCountryIso;

	public String getCountryIso() {
		return mCountryIso;
	}

	/* AOSP SubscriptionInfo.getPrintableId() (@hide); ATL is no debuggable build (TelephonyUtils.IS_DEBUGGABLE false) */
	public static String getPrintableId(String id) {
		String idToPrint = null;
		if (id != null) {
			if (id.length() > 9) {
				idToPrint = id.substring(0, 9) + Rlog.pii(false, id.substring(9));
			} else {
				idToPrint = id;
			}
		}
		return idToPrint;
	}
}
