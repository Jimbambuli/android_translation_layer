package android.telephony;

/* An instance only comes from ISub, which ATL does not have: SubscriptionManager's lists are empty and
 * getActiveSubscriptionInfo() is null, so no object exists. The class is here for the type. */
public class SubscriptionInfo {
	SubscriptionInfo() {}
}
