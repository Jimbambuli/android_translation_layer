package android.view.inputmethod;

import android.content.ComponentName;

/**
 * This class is used to specify meta information of an input method (AOSP InputMethodInfo). ATL has no input method
 * services (the desktop's input method is GTK's): InputMethodManager's lists are empty, an InputMethodInfo
 * is only what an app builds itself with the constructor AOSP offers for tests.
 */
public final class InputMethodInfo {
	private final String mPackageName;
	private final String mServiceName;
	private final CharSequence mLabel;
	private final String mSettingsActivity;

	public InputMethodInfo(String packageName, String className, CharSequence label, String settingsActivity) {
		mPackageName = packageName;
		mServiceName = className;
		mLabel = label;
		mSettingsActivity = settingsActivity;
	}

	/* AOSP parses the service's meta-data for the settings activity and the subtypes; ATL has no input method services,
	 * so the info is the service's package, name and label, without settings activity and subtypes */
	public InputMethodInfo(android.content.Context context, android.content.pm.ResolveInfo service) throws org.xmlpull.v1.XmlPullParserException, java.io.IOException {
		this(service.serviceInfo.packageName, service.serviceInfo.name, service.loadLabel(context.getPackageManager()), null);
	}

	/* AOSP InputMethodInfo.loadLabel(): the service's label; the test constructor gives it as the
	 * ResolveInfo's nonLocalizedLabel */
	public CharSequence loadLabel(android.content.pm.PackageManager pm) {
		return mLabel;
	}

	/* AOSP InputMethodInfo.getServiceInfo(): the ServiceInfo that buildFakeResolveInfo() makes for the test
	 * constructor (enabled, exported, labelled; its ApplicationInfo only knows the package) */
	public android.content.pm.ServiceInfo getServiceInfo() {
		android.content.pm.ServiceInfo si = new android.content.pm.ServiceInfo();
		android.content.pm.ApplicationInfo ai = new android.content.pm.ApplicationInfo();
		ai.packageName = mPackageName;
		ai.enabled = true;
		si.applicationInfo = ai;
		si.enabled = true;
		si.packageName = mPackageName;
		si.name = mServiceName;
		si.exported = true;
		si.nonLocalizedLabel = mLabel;
		return si;
	}

	/** The unique id of the input method: the flattened component name of its service */
	public String getId() {
		return new ComponentName(mPackageName, mServiceName).flattenToShortString();
	}

	public String getPackageName() {
		return mPackageName;
	}

	public String getServiceName() {
		return mServiceName;
	}

	public ComponentName getComponent() {
		return new ComponentName(mPackageName, mServiceName);
	}

	public String getSettingsActivity() {
		return mSettingsActivity;
	}

	/** An input method of this kind has no subtypes */
	public int getSubtypeCount() {
		return 0;
	}

	public InputMethodSubtype getSubtypeAt(int index) {
		throw new ArrayIndexOutOfBoundsException(index);
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		}
		if (!(o instanceof InputMethodInfo)) {
			return false;
		}
		return getId().equals(((InputMethodInfo)o).getId());
	}

	@Override
	public int hashCode() {
		return getId().hashCode();
	}

	@Override
	public String toString() {
		return "InputMethodInfo{" + getId() + ", settings: " + mSettingsActivity + "}";
	}
}
