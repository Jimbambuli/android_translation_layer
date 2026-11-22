package com.android.internal.telephony;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityThread;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Telephony.Sms;
import android.telephony.ServiceState;
import android.telephony.SmsManager;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import android.util.Slog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @hide AOSP SmsController / IccSmsInterfaceManager / SMSDispatcher (frameworks/opt/telephony) for ModemManager: the
 * permission checks of SmsController and SmsPermissions, ServiceState checks of SMSDispatcher (RESULT_ERROR_RADIO_OFF,
 * RESULT_ERROR_NO_SERVICE), the sent intents with their extras and the "sent" provider entry of a non-default SMS app
 * (SMSDispatcher.SmsTracker.persistSentMessageIfRequired). ModemManager splits a long text itself, so all parts go
 * as one SMS object and every part's sent intent gets its answer.
 *
 * Not available from ModemManager, so as AOSP without the feature: delivery reports (the delivery intents are never
 * sent), SMS on the SIM (empty list), cell broadcast, data SMS to a port (no port header in ModemManager's API: the
 * sent intent gets RESULT_ERROR_GENERIC_FAILURE), app specific SMS tokens.
 */
public final class SmsController extends ISms.Stub {
	private static final String TAG = "SmsController";
	private static final int NO_ERROR_CODE = -1; /* SmsResponse.NO_ERROR_CODE */
	private static final String SEND_NEXT_MSG_EXTRA = "SendNextMsg";
	private static final String MESSAGE_ID_EXTRA = "MessageId";

	private static SmsController sInstance;
	private static int sNextToken;
	private static final Map<Integer, Tracker> sTrackers = new HashMap<>();

	static synchronized SmsController get() {
		if (sInstance == null)
			sInstance = new SmsController();
		return sInstance;
	}

	private static Context context() {
		return ActivityThread.currentApplication();
	}

	/* SMSDispatcher.SmsTracker for one SMS object */
	private static final class Tracker {
		String callingPkg;
		String destAddr;
		String text;
		List<PendingIntent> sentIntents;
		boolean persist;
		long messageId;
		boolean multipart;
	}

	/* the service runs in the app's process: the calling app is the app itself (AOSP checks the binder caller,
	 * Context.enforceCallingPermission(), which in ATL never has a caller) */
	private static void enforce(String permission, String message) {
		if (context().checkCallingOrSelfPermission(permission) != PackageManager.PERMISSION_GRANTED)
			throw new SecurityException(message + ": Neither user " + android.os.Process.myUid() + " nor current process has " + permission + ".");
	}

	/* SmsPermissions.checkCallingCanSendText(): without persisting the caller must be IMS/carrier app or have
	 * MODIFY_PHONE_STATE; SEND_SMS always */
	private static void checkCallingCanSendText(boolean persistMessageForNonDefaultSmsApp, String message) {
		if (!persistMessageForNonDefaultSmsApp)
			enforce(Manifest.permission.MODIFY_PHONE_STATE, message);
		enforce(Manifest.permission.SEND_SMS, message);
	}

	private static void sendErrorInPendingIntents(List<PendingIntent> intents, int errorCode) {
		if (intents == null)
			return;
		for (PendingIntent intent : intents) {
			if (intent == null)
				continue;
			try {
				intent.send(errorCode);
			} catch (PendingIntent.CanceledException e) {
			}
		}
	}

	private void send(String callingPkg, String destAddr, String text, List<PendingIntent> sentIntents,
	                  boolean persist, long messageId, boolean multipart) {
		ModemManagerPhone phone = ModemManagerPhone.get();
		Tracker t = new Tracker();
		t.callingPkg = callingPkg;
		t.destAddr = destAddr;
		t.text = text;
		t.sentIntents = sentIntents != null ? sentIntents : Collections.<PendingIntent>emptyList();
		t.persist = persist;
		t.messageId = messageId;
		t.multipart = multipart;
		/* SMSDispatcher.sendRawPdu() -> handleNotInService(): no SMS without the radio or a network */
		if (phone == null || phone.isRadioOff()) {
			finishLater(t, SmsManager.RESULT_ERROR_RADIO_OFF);
			return;
		}
		if (!phone.isRegistered()) {
			finishLater(t, SmsManager.RESULT_ERROR_NO_SERVICE);
			return;
		}
		String path = phone.createSms(destAddr, text, null, false);
		if (path.startsWith("!")) {
			Slog.w(TAG, "ModemManager does not create the SMS: " + path.substring(1));
			finishLater(t, SmsManager.RESULT_ERROR_GENERIC_FAILURE);
			return;
		}
		int token;
		synchronized (sTrackers) {
			token = ++sNextToken;
			sTrackers.put(token, t);
		}
		ModemManagerPhone.sendSms(path, token);
	}

	/* like the phone process, the answer comes after sendTextMessage() has returned (the app writes its outbox entry
	 * first and then takes the result) */
	private static void finishLater(final Tracker t, final int result) {
		new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> finish(t, result, NO_ERROR_CODE));
	}

	/* the answer of Sms.Send */
	static void sent(int token, String error) {
		Tracker t;
		synchronized (sTrackers) {
			t = sTrackers.remove(token);
		}
		if (t == null)
			return;
		if (error != null)
			Slog.w(TAG, "the SMS was not sent: " + error);
		finish(t, error == null ? Activity.RESULT_OK : SmsManager.RESULT_ERROR_GENERIC_FAILURE, NO_ERROR_CODE);
	}

	/* SmsTracker.onSent()/onFailed() for every part: the provider entry with the last part, the sent intents */
	private static void finish(Tracker t, int result, int errorCode) {
		Context context = context();
		Uri uri = null;
		if (t.text != null && t.persist && !SmsApplication.isDefaultSmsApplication(context, t.callingPkg))
			uri = persistSentMessage(context, t, result == Activity.RESULT_OK ? Sms.MESSAGE_TYPE_SENT : Sms.MESSAGE_TYPE_FAILED, errorCode);
		for (int i = 0; i < t.sentIntents.size(); i++) {
			PendingIntent sentIntent = t.sentIntents.get(i);
			if (sentIntent == null)
				continue;
			Intent fillIn = new Intent();
			if (uri != null)
				fillIn.putExtra("uri", uri.toString());
			if (errorCode != NO_ERROR_CODE)
				fillIn.putExtra("errorCode", errorCode);
			if (t.multipart && i == t.sentIntents.size() - 1)
				fillIn.putExtra(SEND_NEXT_MSG_EXTRA, true);
			if (t.messageId != 0L)
				fillIn.putExtra(MESSAGE_ID_EXTRA, t.messageId);
			fillIn.putExtra("format", SmsConstants.FORMAT_3GPP);
			fillIn.putExtra("ims", false);
			try {
				sentIntent.send(context, result, fillIn);
			} catch (PendingIntent.CanceledException e) {
				Slog.e(TAG, "the sent intent was canceled");
			}
		}
	}

	/* SMSDispatcher.SmsTracker.persistSentMessageIfRequired() */
	private static Uri persistSentMessage(Context context, Tracker t, int messageType, int errorCode) {
		ContentValues values = new ContentValues();
		values.put(Sms.SUBSCRIPTION_ID, ModemManagerPhone.SUB_ID);
		values.put(Sms.ADDRESS, t.destAddr);
		values.put(Sms.BODY, t.text);
		values.put(Sms.DATE, System.currentTimeMillis());
		values.put(Sms.SEEN, 1);
		values.put(Sms.READ, 1);
		if (!TextUtils.isEmpty(t.callingPkg))
			values.put(Sms.CREATOR, t.callingPkg);
		if (errorCode != NO_ERROR_CODE)
			values.put(Sms.ERROR_CODE, errorCode);
		ContentResolver resolver = context.getContentResolver();
		try {
			Uri uri = resolver.insert(Sms.Sent.CONTENT_URI, values);
			if (uri != null && messageType == Sms.MESSAGE_TYPE_FAILED) {
				ContentValues updateValues = new ContentValues(1);
				updateValues.put(Sms.TYPE, Sms.MESSAGE_TYPE_FAILED);
				resolver.update(uri, updateValues, null, null);
			}
			return uri;
		} catch (Exception e) {
			Slog.e(TAG, "cannot persist the sent message", e);
			return null;
		}
	}

	private static String pkg(String callingPkg) {
		return callingPkg != null ? callingPkg : context().getPackageName();
	}

	/* ---- ISms ---- */

	@Override
	public void sendTextForSubscriber(int subId, String callingPkg, String callingAttributionTag, String destAddr,
	                                  String scAddr, String text, PendingIntent sentIntent, PendingIntent deliveryIntent,
	                                  boolean persistMessageForNonDefaultSmsApp, long messageId) {
		checkCallingCanSendText(persistMessageForNonDefaultSmsApp, "Sending SMS message");
		send(pkg(callingPkg), destAddr, text, Collections.singletonList(sentIntent), persistMessageForNonDefaultSmsApp, messageId, false);
	}

	@Override
	public void sendTextForSubscriberWithOptions(int subId, String callingPkg, String callingAttributionTag, String destAddr,
	                                             String scAddr, String text, PendingIntent sentIntent, PendingIntent deliveryIntent,
	                                             boolean persistMessageForNonDefaultSmsApp, int priority, boolean expectMore,
	                                             int validityPeriod) {
		sendTextForSubscriber(subId, callingPkg, callingAttributionTag, destAddr, scAddr, text, sentIntent, deliveryIntent,
		                      persistMessageForNonDefaultSmsApp, 0L);
	}

	@Override
	public void sendMultipartTextForSubscriber(int subId, String callingPkg, String callingAttributionTag,
	                                           String destinationAddress, String scAddress, List<String> parts,
	                                           List<PendingIntent> sentIntents, List<PendingIntent> deliveryIntents,
	                                           boolean persistMessageForNonDefaultSmsApp, long messageId) {
		checkCallingCanSendText(persistMessageForNonDefaultSmsApp, "Sending SMS message");
		StringBuilder text = new StringBuilder();
		for (String part : parts)
			text.append(part);
		List<PendingIntent> intents = new ArrayList<>();
		for (int i = 0; i < parts.size(); i++)
			intents.add(sentIntents != null && i < sentIntents.size() ? sentIntents.get(i) : null);
		send(pkg(callingPkg), destinationAddress, text.toString(), intents, persistMessageForNonDefaultSmsApp, messageId, true);
	}

	@Override
	public void sendMultipartTextForSubscriberWithOptions(int subId, String callingPkg, String callingAttributionTag,
	                                                      String destinationAddress, String scAddress, List<String> parts,
	                                                      List<PendingIntent> sentIntents, List<PendingIntent> deliveryIntents,
	                                                      boolean persistMessageForNonDefaultSmsApp, int priority,
	                                                      boolean expectMore, int validityPeriod) {
		sendMultipartTextForSubscriber(subId, callingPkg, callingAttributionTag, destinationAddress, scAddress, parts,
		                               sentIntents, deliveryIntents, persistMessageForNonDefaultSmsApp, 0L);
	}

	@Override
	public void sendDataForSubscriber(int subId, String callingPkg, String callingattributionTag, String destAddr,
	                                  String scAddr, int destPort, byte[] data, PendingIntent sentIntent,
	                                  PendingIntent deliveryIntent) {
		/* SmsController: SmsPermissions.checkCallingCanSendSms() */
		enforce(Manifest.permission.SEND_SMS, "Sending SMS message");
		Slog.w(TAG, "data SMS to port " + destPort + ": ModemManager cannot address a port");
		sendErrorInPendingIntents(Collections.singletonList(sentIntent), SmsManager.RESULT_ERROR_GENERIC_FAILURE);
	}

	/* IccSmsInterfaceManager.sendStoredText(): the message of the provider, as the default SMS app wrote it */
	@Override
	public void sendStoredText(int subId, String callingPkg, String callingAttributionTag, Uri messageUri, String scAddress,
	                           PendingIntent sentIntent, PendingIntent deliveryIntent) {
		enforce(Manifest.permission.SEND_SMS, "Sending SMS message");
		String[] stored = loadStored(messageUri);
		if (stored == null) {
			sendErrorInPendingIntents(Collections.singletonList(sentIntent), SmsManager.RESULT_ERROR_GENERIC_FAILURE);
			return;
		}
		send(pkg(callingPkg), stored[0], stored[1], Collections.singletonList(sentIntent), false, 0L, false);
	}

	@Override
	public void sendStoredMultipartText(int subId, String callingPkg, String callingAttributeTag, Uri messageUri,
	                                    String scAddress, List<PendingIntent> sentIntents, List<PendingIntent> deliveryIntents) {
		enforce(Manifest.permission.SEND_SMS, "Sending SMS message");
		String[] stored = loadStored(messageUri);
		if (stored == null) {
			sendErrorInPendingIntents(sentIntents, SmsManager.RESULT_ERROR_GENERIC_FAILURE);
			return;
		}
		send(pkg(callingPkg), stored[0], stored[1], sentIntents, false, 0L, true);
	}

	private static String[] loadStored(Uri messageUri) {
		try (Cursor c = context().getContentResolver().query(messageUri, new String[] {Sms.ADDRESS, Sms.BODY}, null, null, null)) {
			if (c != null && c.moveToFirst() && !TextUtils.isEmpty(c.getString(0)) && !TextUtils.isEmpty(c.getString(1)))
				return new String[] {c.getString(0), c.getString(1)};
		} catch (Exception e) {
			Slog.e(TAG, "sendStored: cannot read " + messageUri, e);
		}
		return null;
	}

	@Override
	public void injectSmsPduForSubscriber(int subId, byte[] pdu, String format, PendingIntent receivedIntent) {
		enforce(Manifest.permission.MODIFY_PHONE_STATE, "injectSmsPdu");
	}

	@Override
	public List<SmsRawData> getAllMessagesFromIccEfForSubscriber(int subId, String callingPkg) {
		return new ArrayList<SmsRawData>();
	}

	@Override
	public boolean updateMessageOnIccEfForSubscriber(int subId, String callingPkg, int messageIndex, int newStatus, byte[] pdu) {
		return false;
	}

	@Override
	public boolean copyMessageToIccEfForSubscriber(int subId, String callingPkg, int status, byte[] pdu, byte[] smsc) {
		return false;
	}

	@Override
	public boolean enableCellBroadcastForSubscriber(int subId, int messageIdentifier, int ranType) {
		return false;
	}

	@Override
	public boolean disableCellBroadcastForSubscriber(int subId, int messageIdentifier, int ranType) {
		return false;
	}

	@Override
	public boolean enableCellBroadcastRangeForSubscriber(int subId, int startMessageId, int endMessageId, int ranType) {
		return false;
	}

	@Override
	public boolean disableCellBroadcastRangeForSubscriber(int subId, int startMessageId, int endMessageId, int ranType) {
		return false;
	}

	@Override
	public boolean resetAllCellBroadcastRanges(int subId) {
		return false;
	}

	@Override
	public int getPremiumSmsPermission(String packageName) {
		return SmsManager.PREMIUM_SMS_CONSENT_UNKNOWN;
	}

	@Override
	public int getPremiumSmsPermissionForSubscriber(int subId, String packageName) {
		return SmsManager.PREMIUM_SMS_CONSENT_UNKNOWN;
	}

	@Override
	public void setPremiumSmsPermission(String packageName, int permission) {
		enforce(Manifest.permission.MODIFY_PHONE_STATE, "setPremiumSmsPermission");
	}

	@Override
	public void setPremiumSmsPermissionForSubscriber(int subId, String packageName, int permission) {
		setPremiumSmsPermission(packageName, permission);
	}

	@Override
	public boolean isImsSmsSupportedForSubscriber(int subId) {
		return false;
	}

	@Override
	public boolean isSmsSimPickActivityNeeded(int subId) {
		return false;
	}

	@Override
	public int getPreferredSmsSubscription() {
		ModemManagerPhone phone = ModemManagerPhone.get();
		return phone != null && phone.hasSubscription() ? ModemManagerPhone.SUB_ID : SubscriptionManager.INVALID_SUBSCRIPTION_ID;
	}

	/* IccSmsInterfaceManager.getImsSmsFormat() without IMS */
	@Override
	public String getImsSmsFormatForSubscriber(int subId) {
		return SmsConstants.FORMAT_UNKNOWN;
	}

	@Override
	public boolean isSMSPromptEnabled() {
		return false;
	}

	@Override
	public Bundle getCarrierConfigValuesForSubscriber(int subId) {
		return new Bundle();
	}

	@Override
	public String createAppSpecificSmsToken(int subId, String callingPkg, PendingIntent intent) {
		return null;
	}

	@Override
	public String createAppSpecificSmsTokenWithPackageInfo(int subId, String callingPkg, String prefixes, PendingIntent intent) {
		return null;
	}

	@Override
	public void setStorageMonitorMemoryStatusOverride(int subId, boolean isStorageAvailable) {
		enforce(Manifest.permission.MODIFY_PHONE_STATE, "setStorageMonitorMemoryStatusOverride");
	}

	@Override
	public void clearStorageMonitorMemoryStatusOverride(int subId) {
		enforce(Manifest.permission.MODIFY_PHONE_STATE, "clearStorageMonitorMemoryStatusOverride");
	}

	@Override
	public int checkSmsShortCodeDestination(int subId, String callingApk, String callingFeatureId, String destAddress, String countryIso) {
		return SmsManager.SMS_CATEGORY_NOT_SHORT_CODE;
	}

	@Override
	public String getSmscAddressFromIccEfForSubscriber(int subId, String callingPackage) {
		return null;
	}

	@Override
	public boolean setSmscAddressOnIccEfForSubscriber(String smsc, int subId, String callingPackage) {
		return false;
	}

	@Override
	public int getSmsCapacityOnIccForSubscriber(int subId) {
		return 0;
	}

	@Override
	public long getWapMessageSize(String locationUrl) {
		return 0;
	}
}
