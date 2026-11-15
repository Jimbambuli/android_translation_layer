package android.content.pm;

import android.content.IntentFilter;
import android.graphics.drawable.Drawable;

public class ResolveInfo implements android.os.Parcelable {
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

	public ResolveInfo() {
	}

	/* AOSP ResolveInfo(ResolveInfo): copies the fields AOSP copies (ATL has none of its hidden user fields) */
	public ResolveInfo(ResolveInfo orig) {
		activityInfo = orig.activityInfo;
		serviceInfo = orig.serviceInfo;
		providerInfo = orig.providerInfo;
		filter = orig.filter;
		priority = orig.priority;
		preferredOrder = orig.preferredOrder;
		match = orig.match;
		specificIndex = orig.specificIndex;
		labelRes = orig.labelRes;
		nonLocalizedLabel = orig.nonLocalizedLabel;
		icon = orig.icon;
		resolvePackageName = orig.resolvePackageName;
		noResourceId = orig.noResourceId;
		iconResourceId = orig.iconResourceId;
		system = orig.system;
	}

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

	/* AOSP ResolveInfo.DisplayNameComparator: orders by label with a PRIMARY-strength Collator. ATL has
	 * no targetUserId (one user, AOSP's default USER_CURRENT), so AOSP's moving of other users' entries
	 * to the end never applies */
	public static class DisplayNameComparator implements java.util.Comparator<ResolveInfo> {
		public DisplayNameComparator(PackageManager pm) {
			mPM = pm;
			mCollator.setStrength(java.text.Collator.PRIMARY);
		}

		public final int compare(ResolveInfo a, ResolveInfo b) {
			CharSequence sa = a.loadLabel(mPM);
			if (sa == null) sa = a.activityInfo.name;
			CharSequence sb = b.loadLabel(mPM);
			if (sb == null) sb = b.activityInfo.name;

			return mCollator.compare(sa.toString(), sb.toString());
		}

		private final java.text.Collator mCollator = java.text.Collator.getInstance();
		private final PackageManager mPM;
	}

	/* AOSP ResolveInfo.writeToParcel()/ResolveInfo(Parcel)/CREATOR (Android 16). ATL has no targetUserId,
	 * handleAllWebDataURI, mAutoResolutionAllowed, isInstantAppAvailable or userHandle: their slots are
	 * written with the AOSP defaults (UserHandle.USER_CURRENT = -2, false) and skipped when read */
	public int describeContents() {
		return 0;
	}

	public void writeToParcel(android.os.Parcel dest, int parcelableFlags) {
		if (activityInfo != null) {
			dest.writeInt(1);
			activityInfo.writeToParcel(dest, parcelableFlags);
		} else if (serviceInfo != null) {
			dest.writeInt(2);
			serviceInfo.writeToParcel(dest, parcelableFlags);
		} else if (providerInfo != null) {
			dest.writeInt(3);
			providerInfo.writeToParcel(dest, parcelableFlags);
		} else {
			dest.writeInt(0);
		}
		if (filter != null) {
			dest.writeInt(1);
			filter.writeToParcel(dest, parcelableFlags);
		} else {
			dest.writeInt(0);
		}
		dest.writeInt(priority);
		dest.writeInt(preferredOrder);
		dest.writeInt(match);
		dest.writeInt(specificIndex);
		dest.writeInt(labelRes);
		android.text.TextUtils.writeToParcel(nonLocalizedLabel, dest, parcelableFlags);
		dest.writeInt(icon);
		dest.writeString8(resolvePackageName);
		dest.writeInt(-2 /* targetUserId: UserHandle.USER_CURRENT */);
		dest.writeInt(system ? 1 : 0);
		dest.writeInt(noResourceId ? 1 : 0);
		dest.writeInt(iconResourceId);
		dest.writeInt(0 /* handleAllWebDataURI */);
		dest.writeInt(0 /* mAutoResolutionAllowed */);
		dest.writeInt(0 /* isInstantAppAvailable */);
		dest.writeInt(-2 /* userHandle: UserHandle.USER_CURRENT */);
	}

	private ResolveInfo(android.os.Parcel source) {
		activityInfo = null;
		serviceInfo = null;
		providerInfo = null;
		switch (source.readInt()) {
			case 1:
				activityInfo = ActivityInfo.CREATOR.createFromParcel(source);
				break;
			case 2:
				serviceInfo = ServiceInfo.CREATOR.createFromParcel(source);
				break;
			case 3:
				providerInfo = ProviderInfo.CREATOR.createFromParcel(source);
				break;
			default:
				android.util.Slog.w("ResolveInfo", "Missing ComponentInfo!");
				break;
		}
		filter = null;
		if (source.readInt() != 0) {
			filter = IntentFilter.CREATOR.createFromParcel(source);
		}
		priority = source.readInt();
		preferredOrder = source.readInt();
		match = source.readInt();
		specificIndex = source.readInt();
		labelRes = source.readInt();
		nonLocalizedLabel = android.text.TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(source);
		icon = source.readInt();
		resolvePackageName = source.readString8();
		source.readInt(); // targetUserId
		system = source.readInt() != 0;
		noResourceId = source.readInt() != 0;
		iconResourceId = source.readInt();
		source.readInt(); // handleAllWebDataURI
		source.readInt(); // mAutoResolutionAllowed
		source.readInt(); // isInstantAppAvailable
		source.readInt(); // userHandle
	}

	public static final android.os.Parcelable.Creator<ResolveInfo> CREATOR = new android.os.Parcelable.Creator<ResolveInfo>() {
		public ResolveInfo createFromParcel(android.os.Parcel source) {
			return new ResolveInfo(source);
		}

		public ResolveInfo[] newArray(int size) {
			return new ResolveInfo[size];
		}
	};
}
