package android.content.pm;

import android.content.IntentFilter;
import android.graphics.drawable.Drawable;

public class ResolveInfo {
	public ActivityInfo activityInfo = new ActivityInfo();
	public ServiceInfo serviceInfo = new ServiceInfo();
	public IntentFilter filter = new IntentFilter();
	public int priority = -500;
	/* AOSP ResolveInfo fields that an app reads when it picks among the activities for an intent */
	public ProviderInfo providerInfo;
	public int preferredOrder;
	public int match;
	public int specificIndex = -1;
	public boolean isDefault;
	public int labelRes;
	public CharSequence nonLocalizedLabel;
	public int icon;
	public String resolvePackageName;
	public boolean system;

	/* AOSP ResolveInfo.noResourceId / iconResourceId / getIconResource(): the icon resource of the resolved component;
	 * noResourceId is only set for managed-profile entries, ATL has none */
	public boolean noResourceId;
	public int iconResourceId;

	public final int getIconResource() {
		if (noResourceId)
			return 0;
		return getIconResourceInternal();
	}

	final int getIconResourceInternal() {
		if (iconResourceId != 0)
			return iconResourceId;
		final ComponentInfo ci = getComponentInfo();
		if (ci != null) {
			return ci.getIconResource();
		}
		return 0;
	}

	/* AOSP ResolveInfo.loadIcon(); getUserBadgedIcon() of the only user returns the icon itself */
	public Drawable loadIcon(PackageManager pm) {
		Drawable dr = null;
		if (resolvePackageName != null && iconResourceId != 0) {
			dr = pm.getDrawable(resolvePackageName, iconResourceId, null);
		}
		ComponentInfo ci = getComponentInfo();
		if (dr == null && iconResourceId != 0) {
			ApplicationInfo ai = ci.applicationInfo;
			dr = pm.getDrawable(ci.packageName, iconResourceId, ai);
		}
		if (dr != null) {
			return dr;
		}
		return ci.loadIcon(pm);
	}

	/* AOSP ResolveInfo.getComponentInfo. ATL always allocates activityInfo and serviceInfo, so the
	 * one that was filled in (has a name) is taken, in AOSP's order. */
	public ComponentInfo getComponentInfo() {
		if (activityInfo != null && activityInfo.name != null)
			return activityInfo;
		if (serviceInfo != null && serviceInfo.name != null)
			return serviceInfo;
		if (providerInfo != null)
			return providerInfo;
		if (activityInfo != null)
			return activityInfo;
		throw new IllegalStateException("Missing ComponentInfo!");
	}

	/* AOSP ResolveInfo.loadLabel */
	public CharSequence loadLabel(PackageManager pm) {
		if (nonLocalizedLabel != null) {
			return nonLocalizedLabel;
		}
		CharSequence label;
		if (resolvePackageName != null && labelRes != 0) {
			label = pm.getText(resolvePackageName, labelRes, null);
			if (label != null) {
				return label.toString().trim();
			}
		}
		ComponentInfo ci = getComponentInfo();
		ApplicationInfo ai = ci.applicationInfo;
		if (labelRes != 0) {
			label = pm.getText(ci.packageName, labelRes, ai);
			if (label != null) {
				return label.toString().trim();
			}
		}

		CharSequence data = ci.loadLabel(pm);
		// Make the data safe
		if (data != null)
			data = data.toString().trim();
		return data;
	}

	public static class DisplayNameComparator {

		public DisplayNameComparator(PackageManager pm) {}
	}
}
