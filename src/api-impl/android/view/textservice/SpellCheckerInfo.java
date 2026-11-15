package android.view.textservice;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.graphics.drawable.Drawable;

/**
 * AOSP frameworks/base core/java/android/view/textservice/SpellCheckerInfo.java, reduced.
 *
 * Describes a spell checker service. ATL has no spell checker services, so
 * TextServicesManager never hands one out. The service's spell checker meta-data
 * (label, settings activity, subtypes) is not parsed and it is not Parcelable
 * (ATL's ResolveInfo is not).
 */
public final class SpellCheckerInfo {

	private final ResolveInfo mService;
	private final String mId;
	private final int mLabel;
	private final String mSettingsActivityName;

	/* AOSP SpellCheckerInfo(Context, ResolveInfo) without the meta-data parsing */
	public SpellCheckerInfo(Context context, ResolveInfo service) {
		mService = service;
		ServiceInfo si = service.serviceInfo;
		mId = new ComponentName(si.packageName, si.name).flattenToShortString();
		mLabel = 0;
		mSettingsActivityName = null;
	}

	public String getId() {
		return mId;
	}

	public ComponentName getComponent() {
		return new ComponentName(
		    mService.serviceInfo.packageName, mService.serviceInfo.name);
	}

	public String getPackageName() {
		return mService.serviceInfo.packageName;
	}

	public CharSequence loadLabel(PackageManager pm) {
		if (mLabel == 0 || pm == null)
			return "";
		return pm.getText(getPackageName(), mLabel, mService.serviceInfo.applicationInfo);
	}

	public Drawable loadIcon(PackageManager pm) {
		return mService.loadIcon(pm);
	}

	public ServiceInfo getServiceInfo() {
		return mService.serviceInfo;
	}

	public String getSettingsActivity() {
		return mSettingsActivityName;
	}

	/* no subtypes, see above */
	public int getSubtypeCount() {
		return 0;
	}
}
