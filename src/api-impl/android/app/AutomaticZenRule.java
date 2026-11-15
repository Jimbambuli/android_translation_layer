package android.app;

import android.content.ComponentName;
import android.net.Uri;

/* AOSP AutomaticZenRule (android-16.0.0_r4, AutomaticZenRule.java), the fields that do not need ZenPolicy or
 * ZenDeviceEffects (ATL has neither): a rule's name, owner, condition, interruption filter and state. Parcelable
 * without the ZenPolicy and ZenDeviceEffects fields; without Builder. */
public final class AutomaticZenRule implements android.os.Parcelable {
	public static final int TYPE_UNKNOWN = -1;
	public static final int TYPE_OTHER = 0;
	public static final int TYPE_SCHEDULE_TIME = 1;
	public static final int TYPE_SCHEDULE_CALENDAR = 2;
	public static final int TYPE_BEDTIME = 3;
	public static final int TYPE_DRIVING = 4;
	public static final int TYPE_IMMERSIVE = 5;
	public static final int TYPE_THEATER = 6;
	public static final int TYPE_MANAGED = 7;

	public static final int FIELD_NAME = 1 << 0;
	public static final int FIELD_INTERRUPTION_FILTER = 1 << 1;
	public static final int FIELD_ICON = 1 << 2;

	public static final int MAX_STRING_LENGTH = 500;
	public static final int MAX_DESC_LENGTH = 150;

	private boolean enabled;
	private String name;
	private int interruptionFilter;
	private Uri conditionId;
	private ComponentName owner;
	private ComponentName configurationActivity;
	private long creationTime;
	private String mPkg;
	private int mType = TYPE_UNKNOWN;
	private int mIconResId;
	private String mTriggerDescription;
	private boolean mAllowManualInvocation;

	public AutomaticZenRule(String name, ComponentName owner, Uri conditionId, int interruptionFilter, boolean enabled) {
		this(name, owner, null, conditionId, interruptionFilter, enabled);
	}

	public AutomaticZenRule(String name, ComponentName owner, ComponentName configurationActivity, Uri conditionId,
	                        int interruptionFilter, boolean enabled) {
		this.name = getTrimmedString(name);
		this.owner = getTrimmedComponentName(owner);
		this.configurationActivity = getTrimmedComponentName(configurationActivity);
		this.conditionId = getTrimmedUri(conditionId);
		this.interruptionFilter = interruptionFilter;
		this.enabled = enabled;
	}

	/* AOSP AutomaticZenRule(Parcel): the fields in the order of writeToParcel(), without the ZenPolicy and the
	 * ZenDeviceEffects (not in ATL) */
	public AutomaticZenRule(android.os.Parcel source) {
		enabled = source.readInt() == 1;
		if (source.readInt() == 1)
			name = getTrimmedString(source.readString());
		interruptionFilter = source.readInt();
		conditionId = getTrimmedUri((Uri) source.readParcelable(null));
		owner = getTrimmedComponentName((ComponentName) source.readParcelable(null));
		configurationActivity = getTrimmedComponentName((ComponentName) source.readParcelable(null));
		creationTime = source.readLong();
		mPkg = source.readString();
		mAllowManualInvocation = source.readInt() != 0;
		mIconResId = source.readInt();
		mTriggerDescription = getTrimmedString(source.readString(), MAX_DESC_LENGTH);
		mType = source.readInt();
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(android.os.Parcel dest, int flags) {
		dest.writeInt(enabled ? 1 : 0);
		if (name != null) {
			dest.writeInt(1);
			dest.writeString(name);
		} else {
			dest.writeInt(0);
		}
		dest.writeInt(interruptionFilter);
		dest.writeParcelable(conditionId, 0);
		dest.writeParcelable(owner, 0);
		dest.writeParcelable(configurationActivity, 0);
		dest.writeLong(creationTime);
		dest.writeString(mPkg);
		dest.writeInt(mAllowManualInvocation ? 1 : 0);
		dest.writeInt(mIconResId);
		dest.writeString(mTriggerDescription);
		dest.writeInt(mType);
	}

	public static final android.os.Parcelable.Creator<AutomaticZenRule> CREATOR = new android.os.Parcelable.Creator<AutomaticZenRule>() {
		@Override
		public AutomaticZenRule createFromParcel(android.os.Parcel source) {
			return new AutomaticZenRule(source);
		}

		@Override
		public AutomaticZenRule[] newArray(int size) {
			return new AutomaticZenRule[size];
		}
	};

	public ComponentName getOwner() {
		return owner;
	}

	public ComponentName getConfigurationActivity() {
		return configurationActivity;
	}

	public Uri getConditionId() {
		return conditionId;
	}

	public int getInterruptionFilter() {
		return interruptionFilter;
	}

	public String getName() {
		return name;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public long getCreationTime() {
		return creationTime;
	}

	public void setConditionId(Uri conditionId) {
		this.conditionId = getTrimmedUri(conditionId);
	}

	public void setInterruptionFilter(int interruptionFilter) {
		this.interruptionFilter = interruptionFilter;
	}

	public void setName(String name) {
		this.name = getTrimmedString(name);
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public void setOwner(ComponentName owner) {
		this.owner = owner;
	}

	public void setConfigurationActivity(ComponentName componentName) {
		this.configurationActivity = getTrimmedComponentName(componentName);
	}

	public void setPackageName(String pkgName) {
		mPkg = pkgName;
	}

	public String getPackageName() {
		return mPkg;
	}

	public int getType() {
		return mType;
	}

	public void setType(int type) {
		mType = checkValidType(type);
	}

	public String getTriggerDescription() {
		return mTriggerDescription;
	}

	public void setTriggerDescription(String triggerDescription) {
		mTriggerDescription = getTrimmedString(triggerDescription, MAX_DESC_LENGTH);
	}

	public int getIconResId() {
		return mIconResId;
	}

	public void setIconResId(int iconResId) {
		mIconResId = iconResId;
	}

	public boolean isManualInvocationAllowed() {
		return mAllowManualInvocation;
	}

	public void setManualInvocationAllowed(boolean allowManualInvocation) {
		mAllowManualInvocation = allowManualInvocation;
	}

	private static int checkValidType(int type) {
		if (type < TYPE_UNKNOWN || type > TYPE_MANAGED) {
			throw new IllegalArgumentException("Rule type must be one of TYPE_UNKNOWN, TYPE_OTHER, TYPE_SCHEDULE_TIME, "
			    + "TYPE_SCHEDULE_CALENDAR, TYPE_BEDTIME, TYPE_DRIVING, TYPE_IMMERSIVE, TYPE_THEATER, or TYPE_MANAGED");
		}
		return type;
	}

	private static ComponentName getTrimmedComponentName(ComponentName cn) {
		if (cn == null) return null;
		return new ComponentName(getTrimmedString(cn.getPackageName()), getTrimmedString(cn.getClassName()));
	}

	private static String getTrimmedString(String input) {
		return getTrimmedString(input, MAX_STRING_LENGTH);
	}

	private static String getTrimmedString(String input, int length) {
		if (input != null && input.length() > length) {
			return input.substring(0, length);
		}
		return input;
	}

	private static Uri getTrimmedUri(Uri input) {
		if (input != null && input.toString().length() > MAX_STRING_LENGTH) {
			return Uri.parse(getTrimmedString(input.toString()));
		}
		return input;
	}

	/* AOSP AutomaticZenRule(Parcel)/writeToParcel()/describeContents()/CREATOR (Android 16). ATL has no
	 * ZenPolicy or ZenDeviceEffects: written as null, skipped when read. ENABLED is 1, DISABLED 0 */
	public AutomaticZenRule(android.os.Parcel source) {
		enabled = source.readInt() == 1;
		if (source.readInt() == 1) {
			name = getTrimmedString(source.readString8());
		}
		interruptionFilter = source.readInt();
		conditionId = getTrimmedUri(source.readParcelable(null, Uri.class));
		owner = getTrimmedComponentName(source.readParcelable(null, ComponentName.class));
		configurationActivity = getTrimmedComponentName(source.readParcelable(null, ComponentName.class));
		creationTime = source.readLong();
		source.readParcelable(null); // zen policy
		mPkg = source.readString8();
		source.readParcelable(null); // device effects
		mAllowManualInvocation = source.readBoolean();
		mIconResId = source.readInt();
		mTriggerDescription = getTrimmedString(source.readString8(), MAX_DESC_LENGTH);
		mType = source.readInt();
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(android.os.Parcel dest, int flags) {
		dest.writeInt(enabled ? 1 : 0);
		if (name != null) {
			dest.writeInt(1);
			dest.writeString8(name);
		} else {
			dest.writeInt(0);
		}
		dest.writeInt(interruptionFilter);
		dest.writeParcelable(conditionId, 0);
		dest.writeParcelable(owner, 0);
		dest.writeParcelable(configurationActivity, 0);
		dest.writeLong(creationTime);
		dest.writeParcelable(null, 0); // mZenPolicy
		dest.writeString8(mPkg);
		dest.writeParcelable(null, 0); // mDeviceEffects
		dest.writeBoolean(mAllowManualInvocation);
		dest.writeInt(mIconResId);
		dest.writeString8(mTriggerDescription);
		dest.writeInt(mType);
	}

	public static final android.os.Parcelable.Creator<AutomaticZenRule> CREATOR = new android.os.Parcelable.Creator<AutomaticZenRule>() {
		@Override
		public AutomaticZenRule createFromParcel(android.os.Parcel source) {
			return new AutomaticZenRule(source);
		}

		@Override
		public AutomaticZenRule[] newArray(int size) {
			return new AutomaticZenRule[size];
		}
	};
}
