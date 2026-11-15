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

	public Drawable loadIcon(PackageManager pm) {
		Drawable icon = activityInfo.loadIcon(pm);
		if (icon == null) {
			icon = new Drawable();
		}
		return icon;
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
