package android.telephony;

import android.app.PendingIntent;
import android.text.TextUtils;

/* AOSP SmsManager on a device without FEATURE_TELEPHONY_MESSAGING: TelephonyManager.getSmsService() is null, so
 * every use of ISms goes through getISmsServiceOrThrow() = UnsupportedOperationException("Sms is not supported").
 * Argument checks that come before that are kept. divideMessage() (SmsMessage.fragmentText, GSM alphabet tables)
 * is not provided. */
public final class SmsManager {
	private static final SmsManager DEFAULT_INSTANCE = new SmsManager(SubscriptionManager.DEFAULT_SUBSCRIPTION_ID);

	private final int mSubId;

	private SmsManager(int subId) {
		mSubId = subId;
	}

	public static SmsManager getDefault() {
		return DEFAULT_INSTANCE;
	}

	public static SmsManager getSmsManagerForSubscriptionId(int subId) {
		return new SmsManager(subId);
	}

	public SmsManager createForSubscriptionId(int subId) {
		return new SmsManager(subId);
	}

	public static int getDefaultSmsSubscriptionId() {
		return SubscriptionManager.INVALID_SUBSCRIPTION_ID; // getISmsService() == null
	}

	public int getSubscriptionId() {
		if (mSubId == SubscriptionManager.DEFAULT_SUBSCRIPTION_ID)
			throw new UnsupportedOperationException("Sms is not supported");
		return mSubId;
	}

	public void sendTextMessage(String destinationAddress, String scAddress, String text, PendingIntent sentIntent, PendingIntent deliveryIntent) {
		if (TextUtils.isEmpty(destinationAddress))
			throw new IllegalArgumentException("Invalid destinationAddress");
		if (TextUtils.isEmpty(text))
			throw new IllegalArgumentException("Invalid message body");
		throw new UnsupportedOperationException("Sms is not supported");
	}

	public void sendMultipartTextMessage(String destinationAddress, String scAddress, java.util.ArrayList<String> parts, java.util.ArrayList<PendingIntent> sentIntents, java.util.ArrayList<PendingIntent> deliveryIntents) {
		if (TextUtils.isEmpty(destinationAddress))
			throw new IllegalArgumentException("Invalid destinationAddress");
		if (parts == null || parts.size() < 1)
			throw new IllegalArgumentException("Invalid message body");
		throw new UnsupportedOperationException("Sms is not supported");
	}
}
