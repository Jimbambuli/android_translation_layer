package android.telephony;

import android.app.PendingIntent;
import android.text.TextUtils;

/* AOSP SmsManager on a device without FEATURE_TELEPHONY_MESSAGING: TelephonyManager.getSmsService() is null, so
 * every use of ISms goes through getISmsServiceOrThrow() = UnsupportedOperationException("Sms is not supported").
 * Argument checks that come before that are kept. */
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

	/* AOSP SmsManager MMS entry points: argument checks, then Context.getSystemService(MMS_SERVICE) ("mms"); ATL has
	 * no MmsManager, so the service is null and nothing is sent or downloaded. */
	public void sendMultimediaMessage(android.content.Context context, android.net.Uri contentUri, String locationUrl,
	                                  android.os.Bundle configOverrides, PendingIntent sentIntent) {
		sendMultimediaMessage(context, contentUri, locationUrl, configOverrides, sentIntent, 0L);
	}

	public void sendMultimediaMessage(android.content.Context context, android.net.Uri contentUri, String locationUrl,
	                                  android.os.Bundle configOverrides, PendingIntent sentIntent, long messageId) {
		if (contentUri == null) {
			throw new IllegalArgumentException("Uri contentUri null");
		}
	}

	public void downloadMultimediaMessage(android.content.Context context, String locationUrl, android.net.Uri contentUri,
	                                      android.os.Bundle configOverrides, PendingIntent downloadedIntent) {
		downloadMultimediaMessage(context, locationUrl, contentUri, configOverrides, downloadedIntent, 0L);
	}

	public void downloadMultimediaMessage(android.content.Context context, String locationUrl, android.net.Uri contentUri,
	                                      android.os.Bundle configOverrides, PendingIntent downloadedIntent, long messageId) {
		if (TextUtils.isEmpty(locationUrl)) {
			throw new IllegalArgumentException("Empty MMS location URL");
		}
		if (contentUri == null) {
			throw new IllegalArgumentException("Uri contentUri null");
		}
	}

	/* AOSP SmsManager.divideMessage(): null check, then SmsMessage.fragmentText(text, getSubscriptionId()). For the
	 * default instance getSubscriptionId() already throws "Sms is not supported" (no ISms); for an explicit
	 * subscription the GSM/CDMA fragmenting is not provided and fails the same way. */
	public java.util.ArrayList<String> divideMessage(String text) {
		if (null == text)
			throw new IllegalArgumentException("text is null");
		getSubscriptionId();
		throw new UnsupportedOperationException("Sms is not supported");
	}

	/* getISmsService() == null */
	public android.os.Bundle getCarrierConfigValues() {
		return new android.os.Bundle();
	}
}
