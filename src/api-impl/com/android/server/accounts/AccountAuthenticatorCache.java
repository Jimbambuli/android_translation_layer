package com.android.server.accounts;

import android.accounts.AccountManager;
import android.accounts.AuthenticatorDescription;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Slog;
import android.util.Xml;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.xmlpull.v1.XmlPullParser;

/**
 * AOSP AccountAuthenticatorCache (services/core/java/com/android/server/accounts) with the parts of
 * RegisteredServicesCache (core/java/android/content/pm) it uses: the services of the installed packages that
 * handle android.accounts.AccountAuthenticator, described by their android.accounts.AccountAuthenticator
 * meta-data. AOSP keeps the cache in system_server and updates it on package changes; ATL's packages do not
 * change while an app runs, so it is read once per process.
 * @hide
 */
final class AccountAuthenticatorCache {
	private static final String TAG = "Account";

	/* RegisteredServicesCache.ServiceInfo */
	static final class ServiceInfo {
		final AuthenticatorDescription type;
		final ComponentName componentName;
		final int uid;

		ServiceInfo(AuthenticatorDescription type, ComponentName componentName, int uid) {
			this.type = type;
			this.componentName = componentName;
			this.uid = uid;
		}
	}

	private final Context mContext;
	private List<ServiceInfo> mServices;

	AccountAuthenticatorCache(Context context) {
		mContext = context;
	}

	/* RegisteredServicesCache.getServiceInfo(): the authenticator of the type, null if there is none */
	ServiceInfo getServiceInfo(String accountType) {
		for (ServiceInfo info : getAllServices())
			if (info.type.type.equals(accountType))
				return info;
		return null;
	}

	/* RegisteredServicesCache.getAllServices() / generateServicesMap(): the first service of a type wins */
	synchronized List<ServiceInfo> getAllServices() {
		if (mServices != null)
			return mServices;
		List<ServiceInfo> services = new ArrayList<>();
		PackageManager pm = mContext.getPackageManager();
		List<ResolveInfo> resolveInfos = pm.queryIntentServices(new Intent(AccountManager.ACTION_AUTHENTICATOR_INTENT),
		                                                         PackageManager.GET_META_DATA);
		for (ResolveInfo resolveInfo : resolveInfos) {
			ServiceInfo info = parseServiceInfo(pm, resolveInfo.serviceInfo);
			if (info == null || getType(services, info.type.type) != null)
				continue;
			services.add(info);
		}
		mServices = Collections.unmodifiableList(services);
		return mServices;
	}

	private static ServiceInfo getType(List<ServiceInfo> services, String type) {
		for (ServiceInfo info : services)
			if (info.type.type.equals(type))
				return info;
		return null;
	}

	/* RegisteredServicesCache.parseServiceInfo(); AOSP's package manager leaves disabled components out of
	 * queryIntentServices() (ComponentResolver), ATL's does not, so they are skipped here */
	private ServiceInfo parseServiceInfo(PackageManager pm, android.content.pm.ServiceInfo si) {
		if (!si.enabled || (si.applicationInfo != null && !si.applicationInfo.enabled))
			return null;
		ComponentName componentName = new ComponentName(si.packageName, si.name);
		try (XmlResourceParser parser = si.loadXmlMetaData(pm, AccountManager.AUTHENTICATOR_META_DATA_NAME)) {
			if (parser == null) {
				Slog.w(TAG, "No " + AccountManager.AUTHENTICATOR_META_DATA_NAME + " meta-data for " + componentName);
				return null;
			}
			int type;
			while ((type = parser.next()) != XmlPullParser.END_DOCUMENT && type != XmlPullParser.START_TAG) {
			}
			if (!AccountManager.AUTHENTICATOR_ATTRIBUTES_NAME.equals(parser.getName())) {
				Slog.w(TAG, "Meta-data does not start with " + AccountManager.AUTHENTICATOR_ATTRIBUTES_NAME + " tag: " + componentName);
				return null;
			}
			Resources res = pm.getResourcesForApplication(si.applicationInfo);
			AuthenticatorDescription description = parseServiceAttributes(res, si.packageName, Xml.asAttributeSet(parser));
			if (description == null)
				return null;
			return new ServiceInfo(description, componentName, si.applicationInfo.uid);
		} catch (Exception e) {
			Slog.w(TAG, "Unable to load authenticator " + componentName, e);
			return null;
		}
	}

	/* AccountAuthenticatorCache.parseServiceAttributes() */
	private static AuthenticatorDescription parseServiceAttributes(Resources res, String packageName, AttributeSet attrs) {
		TypedArray sa = res.obtainAttributes(attrs, com.android.internal.R.styleable.AccountAuthenticator);
		try {
			final String accountType = sa.getString(com.android.internal.R.styleable.AccountAuthenticator_accountType);
			final int labelId = sa.getResourceId(com.android.internal.R.styleable.AccountAuthenticator_label, 0);
			final int iconId = sa.getResourceId(com.android.internal.R.styleable.AccountAuthenticator_icon, 0);
			final int smallIconId = sa.getResourceId(com.android.internal.R.styleable.AccountAuthenticator_smallIcon, 0);
			final int prefId = sa.getResourceId(com.android.internal.R.styleable.AccountAuthenticator_accountPreferences, 0);
			final boolean customTokens = sa.getBoolean(com.android.internal.R.styleable.AccountAuthenticator_customTokens, false);
			if (TextUtils.isEmpty(accountType))
				return null;
			return new AuthenticatorDescription(accountType, packageName, labelId, iconId, smallIconId, prefId, customTokens);
		} finally {
			sa.recycle();
		}
	}
}
