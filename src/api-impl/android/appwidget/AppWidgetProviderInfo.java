package android.appwidget;

import android.content.ComponentName;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Process;
import android.os.UserHandle;

/* AOSP core/java/android/appwidget/AppWidgetProviderInfo.java (android-16). The system's widget service fills it
 * from the provider's <appwidget-provider> metadata; ATL has no widget host, so AppWidgetManager hands none out.
 * providerInfo is not parcelled (ATL's ActivityInfo has no CREATOR). */
public class AppWidgetProviderInfo implements Parcelable {
	public static final int RESIZE_NONE = 0;
	public static final int RESIZE_HORIZONTAL = 1;
	public static final int RESIZE_VERTICAL = 2;
	public static final int RESIZE_BOTH = RESIZE_HORIZONTAL | RESIZE_VERTICAL;

	public static final int WIDGET_CATEGORY_UNKNOWN = -1;
	public static final int WIDGET_CATEGORY_HOME_SCREEN = 1;
	public static final int WIDGET_CATEGORY_KEYGUARD = 2;
	public static final int WIDGET_CATEGORY_SEARCHBOX = 4;

	public static final int WIDGET_FEATURE_RECONFIGURABLE = 1;
	public static final int WIDGET_FEATURE_HIDE_FROM_PICKER = 2;
	public static final int WIDGET_FEATURE_CONFIGURATION_OPTIONAL = 4;

	public ComponentName provider;
	public int minWidth;
	public int minHeight;
	public int minResizeWidth;
	public int minResizeHeight;
	public int maxResizeWidth;
	public int maxResizeHeight;
	public int targetCellWidth;
	public int targetCellHeight;
	public int updatePeriodMillis;
	public int initialLayout;
	public int initialKeyguardLayout;
	public ComponentName configure;
	@Deprecated
	public String label;
	@Deprecated
	public int icon;
	public int autoAdvanceViewId;
	@Deprecated
	public int previewImage;
	public int previewLayout;
	public int resizeMode;
	public int widgetCategory;
	public int descriptionRes;
	public int widgetFeatures;
	public ActivityInfo providerInfo;
	public boolean isExtendedFromAppWidgetProvider;

	public AppWidgetProviderInfo() {
	}

	public AppWidgetProviderInfo(Parcel in) {
		this.provider = in.readTypedObject(ComponentName.CREATOR);
		this.minWidth = in.readInt();
		this.minHeight = in.readInt();
		this.minResizeWidth = in.readInt();
		this.minResizeHeight = in.readInt();
		this.maxResizeWidth = in.readInt();
		this.maxResizeHeight = in.readInt();
		this.targetCellWidth = in.readInt();
		this.targetCellHeight = in.readInt();
		this.updatePeriodMillis = in.readInt();
		this.initialLayout = in.readInt();
		this.initialKeyguardLayout = in.readInt();
		this.configure = in.readTypedObject(ComponentName.CREATOR);
		this.label = in.readString();
		this.icon = in.readInt();
		this.previewImage = in.readInt();
		this.previewLayout = in.readInt();
		this.autoAdvanceViewId = in.readInt();
		this.resizeMode = in.readInt();
		this.widgetCategory = in.readInt();
		this.widgetFeatures = in.readInt();
		this.descriptionRes = in.readInt();
		this.isExtendedFromAppWidgetProvider = in.readBoolean();
	}

	public final String loadLabel(PackageManager packageManager) {
		CharSequence label = providerInfo.loadLabel(packageManager);
		if (label != null) {
			return label.toString().trim();
		}
		return null;
	}

	/* AOSP: the user of providerInfo's uid; ATL runs one user */
	public final UserHandle getProfile() {
		return Process.myUserHandle();
	}

	public ActivityInfo getActivityInfo() {
		return providerInfo;
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeTypedObject(this.provider, flags);
		out.writeInt(this.minWidth);
		out.writeInt(this.minHeight);
		out.writeInt(this.minResizeWidth);
		out.writeInt(this.minResizeHeight);
		out.writeInt(this.maxResizeWidth);
		out.writeInt(this.maxResizeHeight);
		out.writeInt(this.targetCellWidth);
		out.writeInt(this.targetCellHeight);
		out.writeInt(this.updatePeriodMillis);
		out.writeInt(this.initialLayout);
		out.writeInt(this.initialKeyguardLayout);
		out.writeTypedObject(this.configure, flags);
		out.writeString(this.label);
		out.writeInt(this.icon);
		out.writeInt(this.previewImage);
		out.writeInt(this.previewLayout);
		out.writeInt(this.autoAdvanceViewId);
		out.writeInt(this.resizeMode);
		out.writeInt(this.widgetCategory);
		out.writeInt(this.widgetFeatures);
		out.writeInt(this.descriptionRes);
		out.writeBoolean(this.isExtendedFromAppWidgetProvider);
	}

	@Override
	public AppWidgetProviderInfo clone() {
		AppWidgetProviderInfo that = new AppWidgetProviderInfo();
		that.provider = this.provider == null ? null : this.provider.clone();
		that.minWidth = this.minWidth;
		that.minHeight = this.minHeight;
		that.minResizeWidth = this.minResizeWidth;
		that.minResizeHeight = this.minResizeHeight;
		that.maxResizeWidth = this.maxResizeWidth;
		that.maxResizeHeight = this.maxResizeHeight;
		that.targetCellWidth = this.targetCellWidth;
		that.targetCellHeight = this.targetCellHeight;
		that.updatePeriodMillis = this.updatePeriodMillis;
		that.initialLayout = this.initialLayout;
		that.initialKeyguardLayout = this.initialKeyguardLayout;
		that.configure = this.configure == null ? null : this.configure.clone();
		that.label = this.label;
		that.icon = this.icon;
		that.previewImage = this.previewImage;
		that.previewLayout = this.previewLayout;
		that.autoAdvanceViewId = this.autoAdvanceViewId;
		that.resizeMode = this.resizeMode;
		that.widgetCategory = this.widgetCategory;
		that.providerInfo = this.providerInfo;
		that.widgetFeatures = this.widgetFeatures;
		that.descriptionRes = this.descriptionRes;
		that.isExtendedFromAppWidgetProvider = this.isExtendedFromAppWidgetProvider;
		return that;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	public static final Parcelable.Creator<AppWidgetProviderInfo> CREATOR = new Parcelable.Creator<AppWidgetProviderInfo>() {
		@Override
		public AppWidgetProviderInfo createFromParcel(Parcel parcel) {
			return new AppWidgetProviderInfo(parcel);
		}

		@Override
		public AppWidgetProviderInfo[] newArray(int size) {
			return new AppWidgetProviderInfo[size];
		}
	};

	public String toString() {
		return "AppWidgetProviderInfo(" + getProfile() + '/' + provider + ')';
	}
}
