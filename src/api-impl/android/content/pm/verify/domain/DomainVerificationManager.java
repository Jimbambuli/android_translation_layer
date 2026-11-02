package android.content.pm.verify.domain;

import android.atl.ATLLoadedApp;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PackageParser;
import android.os.Process;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Patterns;
import java.util.Map;
import java.util.UUID;

/* AOSP core/java/android/content/pm/verify/domain/DomainVerificationManager.java (android14-release), the public
 * part. On Android the state comes from DomainVerificationService; ATL runs one app and verifies no domains, so
 * the app's own state lists the web domains it declares (as DomainVerificationCollector.collectAllWebDomains()
 * collects them) all in DOMAIN_STATE_NONE, link handling allowed. Other packages are unknown, as AOSP reports a
 * package it does not have. */
public final class DomainVerificationManager {
	public static final int STATUS_OK = 0;
	public static final int ERROR_DOMAIN_SET_ID_INVALID = 1;
	public static final int ERROR_UNKNOWN_DOMAIN = 2;
	public static final int ERROR_UNABLE_TO_APPROVE = 3;

	private final Context mContext;
	private final UUID mIdentifier = UUID.randomUUID();

	public DomainVerificationManager(Context context) {
		mContext = context;
	}

	public DomainVerificationUserState getDomainVerificationUserState(String packageName) throws NameNotFoundException {
		if (!mContext.getPackageName().equals(packageName))
			throw new NameNotFoundException("Package " + packageName + " is not installed");
		Map<String, Integer> hostToStateMap = new ArrayMap<>();
		for (String host : collectAllWebDomains(ATLLoadedApp.getPrimaryApplication().pkg))
			hostToStateMap.put(host, DomainVerificationUserState.DOMAIN_STATE_NONE);
		return new DomainVerificationUserState(mIdentifier, packageName, Process.myUserHandle(), true, hostToStateMap);
	}

	/* AOSP DomainVerificationCollector.collectDomainsInternal(pkg, checkAutoVerify=false, valid=true), without
	 * the size limit */
	private static java.util.Set<String> collectAllWebDomains(PackageParser.Package pkg) {
		java.util.Set<String> domains = new android.util.ArraySet<>();
		for (PackageParser.Activity activity : pkg.activities) {
			for (IntentFilter intentFilter : activity.intents) {
				if (!intentFilter.hasCategory(Intent.CATEGORY_DEFAULT) || !intentFilter.handlesWebUris(false))
					continue;
				int authorityCount = intentFilter.countDataAuthorities();
				for (int index = 0; index < authorityCount; index++) {
					String host = intentFilter.getDataAuthority(index).getHost();
					if (!TextUtils.isEmpty(host) && Patterns.DOMAIN_NAME.matcher(host).matches())
						domains.add(host);
				}
			}
		}
		return domains;
	}
}
