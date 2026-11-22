package com.android.internal.telephony;

import android.Manifest.permission;
import android.atl.ATLLoadedApp;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.PackageParser;
import android.provider.Telephony;
import android.telephony.TelephonyManager;

import java.util.List;

/**
 * @hide The default SMS application, after AOSP 16 telephony/common/.../SmsApplication.java.
 *
 * <p>On Android the user gives the SMS role (RoleManager.ROLE_SMS) to one complete SMS application
 * (SmsApplicationData.isComplete(): SMS_DELIVER receiver with BROADCAST_SMS, WAP_PUSH_DELIVER receiver
 * with BROADCAST_WAP_PUSH, RESPOND_VIA_MESSAGE service with SEND_RESPOND_VIA_MESSAGE, SENDTO smsto: activity).
 * ATL runs one app and grants it what its manifest asks for: the running app holds the role when it is complete
 * and the device can send SMS (AOSP: the role is not available without FEATURE_TELEPHONY_MESSAGING).
 */
public final class SmsApplication {
	private static final String SCHEME_SMSTO = "smsto";

	/** AOSP SmsApplication.SmsApplicationData: the classes of a complete SMS app */
	public static class SmsApplicationData {
		public final String mPackageName;
		public String mSmsReceiverClass;
		public String mMmsReceiverClass;
		public String mRespondViaMessageClass;
		public String mSendToClass;
		public String mSmsAppChangedReceiverClass;
		public String mProviderChangedReceiverClass;
		public String mSimFullReceiverClass;

		SmsApplicationData(String packageName) {
			mPackageName = packageName;
		}

		public boolean isComplete() {
			return (mSmsReceiverClass != null && mMmsReceiverClass != null
			        && mRespondViaMessageClass != null && mSendToClass != null);
		}
	}

	private static boolean matches(PackageParser.Component<? extends PackageParser.IntentInfo> c, String action, String scheme) {
		for (IntentFilter filter : c.intents) {
			if (filter.matchAction(action) && (scheme == null || filter.hasDataScheme(scheme)))
				return true;
		}
		return false;
	}

	/* AOSP getApplicationCollectionInternal() for one package */
	private static SmsApplicationData getApplicationData(PackageParser.Package pkg) {
		SmsApplicationData data = new SmsApplicationData(pkg.packageName);
		for (PackageParser.Activity r : pkg.receivers) {
			if (data.mSmsReceiverClass == null && permission.BROADCAST_SMS.equals(r.info.permission)
			    && matches(r, Telephony.Sms.Intents.SMS_DELIVER_ACTION, null))
				data.mSmsReceiverClass = r.className;
			if (data.mMmsReceiverClass == null && permission.BROADCAST_WAP_PUSH.equals(r.info.permission)
			    && matches(r, Telephony.Sms.Intents.WAP_PUSH_DELIVER_ACTION, null))
				data.mMmsReceiverClass = r.className;
			if (data.mSmsAppChangedReceiverClass == null && matches(r, Telephony.Sms.Intents.ACTION_DEFAULT_SMS_PACKAGE_CHANGED, null))
				data.mSmsAppChangedReceiverClass = r.className;
			if (data.mProviderChangedReceiverClass == null && matches(r, Telephony.Sms.Intents.ACTION_EXTERNAL_PROVIDER_CHANGE, null))
				data.mProviderChangedReceiverClass = r.className;
			if (data.mSimFullReceiverClass == null && matches(r, Telephony.Sms.Intents.SIM_FULL_ACTION, null))
				data.mSimFullReceiverClass = r.className;
		}
		for (PackageParser.Service s : pkg.services) {
			if (data.mRespondViaMessageClass == null && permission.SEND_RESPOND_VIA_MESSAGE.equals(s.info.permission)
			    && matches(s, TelephonyManager.ACTION_RESPOND_VIA_MESSAGE, SCHEME_SMSTO))
				data.mRespondViaMessageClass = s.className;
		}
		for (PackageParser.Activity a : pkg.activities) {
			if (data.mSendToClass == null && matches(a, Intent.ACTION_SENDTO, SCHEME_SMSTO))
				data.mSendToClass = a.className;
		}
		return data;
	}

	/** @hide the role holder's data, null when there is none */
	public static SmsApplicationData getSmsApplicationData(Context context) {
		if (context == null || !context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY_MESSAGING))
			return null;
		ATLLoadedApp app = ATLLoadedApp.getPrimaryApplication();
		if (app == null)
			return null;
		SmsApplicationData data = getApplicationData(app.pkg);
		return data.isComplete() ? data : null;
	}

	/** AOSP: the component of the default SMS app's SMS_DELIVER receiver, null without one */
	public static ComponentName getDefaultSmsApplication(Context context, boolean updateIfNeeded) {
		SmsApplicationData data = getSmsApplicationData(context);
		return data == null ? null : new ComponentName(data.mPackageName, data.mSmsReceiverClass);
	}

	/** AOSP getDefaultSmsApplicationAsUser(): ATL has one user */
	public static ComponentName getDefaultSmsApplicationAsUser(Context context, boolean updateIfNeeded, android.os.UserHandle userHandle) {
		return getDefaultSmsApplication(context, updateIfNeeded);
	}

	/** AOSP: the default SMS app's ACTION_EXTERNAL_PROVIDER_CHANGE receiver */
	public static ComponentName getDefaultExternalTelephonyProviderChangedApplication(Context context, boolean updateIfNeeded) {
		SmsApplicationData data = getSmsApplicationData(context);
		return data == null || data.mProviderChangedReceiverClass == null ? null
		                                                                   : new ComponentName(data.mPackageName, data.mProviderChangedReceiverClass);
	}

	/** AOSP: the default SMS app's ACTION_DEFAULT_SMS_PACKAGE_CHANGED receiver */
	public static ComponentName getDefaultSmsApplicationChangedReceiver(Context context, boolean updateIfNeeded) {
		SmsApplicationData data = getSmsApplicationData(context);
		return data == null || data.mSmsAppChangedReceiverClass == null ? null
		                                                                  : new ComponentName(data.mPackageName, data.mSmsAppChangedReceiverClass);
	}

	/** AOSP: the default SMS app's SIM_FULL receiver */
	public static ComponentName getDefaultSimFullApplication(Context context, boolean updateIfNeeded) {
		SmsApplicationData data = getSmsApplicationData(context);
		return data == null || data.mSimFullReceiverClass == null ? null
		                                                            : new ComponentName(data.mPackageName, data.mSimFullReceiverClass);
	}

	/** AOSP: the framework writes sent messages to the provider for every app but the default SMS app */
	public static boolean shouldWriteMessageForPackage(String packageName, Context context) {
		return !isDefaultSmsApplication(context, packageName);
	}

	public static boolean isDefaultSmsApplication(Context context, String packageName) {
		if (packageName == null)
			return false;
		ComponentName component = getDefaultSmsApplication(context, false);
		return component != null && packageName.equals(component.getPackageName());
	}

	/** AOSP: the complete SMS apps; ATL knows the running one */
	public static java.util.Collection<SmsApplicationData> getApplicationCollection(Context context) {
		SmsApplicationData data = getSmsApplicationData(context);
		return data == null ? java.util.Collections.<SmsApplicationData>emptyList() : java.util.Collections.singletonList(data);
	}
}
