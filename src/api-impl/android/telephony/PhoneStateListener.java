package android.telephony;

/* The callbacks of AOSP PhoneStateListener whose argument types ATL has; every default body is empty there
 * ("default implementation empty"). ATL has no modem, so listen() never calls them. */
public class PhoneStateListener {

	public void onMessageWaitingIndicatorChanged(boolean mwi) {}

	public void onCallForwardingIndicatorChanged(boolean cfi) {}

	public void onCellLocationChanged(CellLocation location) {}

	@Deprecated
	public void onCallStateChanged(int state, String phoneNumber) {}

	public void onDataConnectionStateChanged(int state) {}

	public void onDataConnectionStateChanged(int state, int networkType) {}

	public void onDataActivity(int direction) {}

	@Deprecated
	public void onSignalStrengthChanged(int asu) {}

	public void onUserMobileDataStateChanged(boolean enabled) {}

	public void onActiveDataSubscriptionIdChanged(int subId) {}
}
