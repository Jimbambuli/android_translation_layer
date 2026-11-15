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
 * (label, settings activity, subtypes) is not parsed; the Parcel is AOSP's, with
 * the subtype list always empty (ATL has no SpellCheckerSubtype).
 */
public final class SpellCheckerInfo implements android.os.Parcelable {

	private final ResolveInfo mService;
	private final String mId;
	private final int mLabel;
	private final String mSettingsActivityName;

	/* AOSP SpellCheckerInfo(Parcel) */
	public SpellCheckerInfo(android.os.Parcel source) {
		mLabel = source.readInt();
		mId = source.readString();
		mSettingsActivityName = source.readString();
		mService = ResolveInfo.CREATOR.createFromParcel(source);
		source.readInt(); // the subtypes: none in ATL, an AOSP parcel of a spell checker with subtypes is not read
	}

	@Override
	public void writeToParcel(android.os.Parcel dest, int flags) {
		dest.writeInt(mLabel);
		dest.writeString(mId);
		dest.writeString(mSettingsActivityName);
		mService.writeToParcel(dest, flags);
		dest.writeInt(0); // writeTypedList(no subtypes)
	}

	public static final android.os.Parcelable.Creator<SpellCheckerInfo> CREATOR = new android.os.Parcelable.Creator<SpellCheckerInfo>() {
		@Override
		public SpellCheckerInfo createFromParcel(android.os.Parcel source) {
			return new SpellCheckerInfo(source);
		}

		@Override
		public SpellCheckerInfo[] newArray(int size) {
			return new SpellCheckerInfo[size];
		}
	};

	@Override
	public int describeContents() {
		return 0;
	}

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
