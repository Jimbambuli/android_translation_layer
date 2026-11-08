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
