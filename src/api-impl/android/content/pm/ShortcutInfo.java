package android.content.pm;

import android.app.Person;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.LocusId;
import android.graphics.drawable.Icon;
import android.os.Parcelable;
import android.os.PersistableBundle;
import android.os.Process;
import android.os.UserHandle;
import android.text.TextUtils;
import java.util.Objects;
import java.util.Set;

/* AOSP ShortcutInfo: what the builder was given; ShortcutManager marks the dynamic ones */
public class ShortcutInfo implements Parcelable {
	public static final Creator<ShortcutInfo> CREATOR = null;

	public static final int DISABLED_REASON_NOT_DISABLED = 0;

	private String mId;
	private String mPackageName;
	private ComponentName mActivity;
	private CharSequence mTitle;
	private CharSequence mText;
	private CharSequence mDisabledMessage;
	private Intent[] mIntents;
	private Set<String> mCategories;
	private PersistableBundle mExtras;
	private LocusId mLocusId;
	private int mRank;
	private int mExcludedSurfaces;
	private long mLastChangedTimestamp;
	private Icon mIcon;
	private Person[] mPersons;
	private boolean mLongLived;
	boolean mDynamic;

	/* AOSP ShortcutInfo.validateIcon (tint check left out: ATL Icon has no hasTint) */
	public static Icon validateIcon(Icon icon) {
		switch (icon.getType()) {
			case Icon.TYPE_RESOURCE:
			case Icon.TYPE_BITMAP:
			case Icon.TYPE_ADAPTIVE_BITMAP:
			case Icon.TYPE_URI:
			case Icon.TYPE_URI_ADAPTIVE_BITMAP:
				break;
			default:
				throw new IllegalArgumentException("Unsupported icon type:"
				    + " only the bitmap and resource types are supported");
		}
		return icon;
	}

	/* @hide in AOSP; kept for the builder fields */
	public Icon getIcon() {
		return mIcon;
	}

	public Person[] getPersons() {
		return mPersons == null ? null : mPersons.clone();
	}

	public boolean isLongLived() {
		return mLongLived;
	}

	public String getId() {
		return mId;
	}

	public String getPackage() {
		return mPackageName;
	}

	public ComponentName getActivity() {
		return mActivity;
	}

	public CharSequence getShortLabel() {
		return mTitle;
	}

	public CharSequence getLongLabel() {
		return mText;
	}

	public CharSequence getDisabledMessage() {
		return mDisabledMessage;
	}

	public int getDisabledReason() {
		return DISABLED_REASON_NOT_DISABLED;
	}

	public Set<String> getCategories() {
		return mCategories;
	}

	/* AOSP: a copy of the last intent */
	public Intent getIntent() {
		if (mIntents == null || mIntents.length == 0) {
			return null;
		}
		return new Intent(mIntents[mIntents.length - 1]);
	}

	public Intent[] getIntents() {
		if (mIntents == null) {
			return null;
		}
		Intent[] ret = new Intent[mIntents.length];
		for (int i = 0; i < ret.length; i++) {
			ret[i] = new Intent(mIntents[i]);
		}
		return ret;
	}

	public PersistableBundle getExtras() {
		return mExtras;
	}

	public LocusId getLocusId() {
		return mLocusId;
	}

	public int getRank() {
		return mRank;
	}

	public UserHandle getUserHandle() {
		return Process.myUserHandle();
	}

	public long getLastChangedTimestamp() {
		return mLastChangedTimestamp;
	}

	public boolean isDynamic() {
		return mDynamic;
	}

	/* no launcher pins shortcuts, none comes from the manifest or a cache */
	public boolean isPinned() {
		return false;
	}

	public boolean isDeclaredInManifest() {
		return false;
	}

	public boolean isCached() {
		return false;
	}

	public boolean isImmutable() {
		return false;
	}

	public boolean isEnabled() {
		return true;
	}

	public boolean hasKeyFieldsOnly() {
		return false;
	}

	public boolean isExcludedFromSurfaces(int surface) {
		return (mExcludedSurfaces & surface) != 0;
	}

	public int getExcludedFromSurfaces() {
		return mExcludedSurfaces;
	}

	public static class Builder {
		private final String mId;
		private final String mPackageName;
		private ComponentName mActivity;
		private CharSequence mTitle;
		private CharSequence mText;
		private CharSequence mDisabledMessage;
		private Intent[] mIntents;
		private Set<String> mCategories;
		private PersistableBundle mExtras;
		private LocusId mLocusId;
		private int mRank;
		private int mExcludedSurfaces;
		private Icon mIcon;
		private Person[] mPersons;
		private boolean mIsLongLived;

		public Builder(Context context, String id) {
			if (TextUtils.isEmpty(id)) {
				throw new IllegalArgumentException("id cannot be empty");
			}
			mId = id;
			mPackageName = context != null ? context.getPackageName() : null;
		}

		public ShortcutInfo build() {
			ShortcutInfo info = new ShortcutInfo();
			info.mId = mId;
			info.mPackageName = mPackageName;
			info.mActivity = mActivity;
			info.mTitle = mTitle;
			info.mText = mText;
			info.mDisabledMessage = mDisabledMessage;
			info.mIntents = mIntents;
			info.mCategories = mCategories;
			info.mExtras = mExtras;
			info.mLocusId = mLocusId;
			info.mRank = mRank;
			info.mExcludedSurfaces = mExcludedSurfaces;
			info.mLastChangedTimestamp = System.currentTimeMillis();
			info.mIcon = mIcon;
			info.mPersons = mPersons == null ? null : mPersons.clone();
			info.mLongLived = mIsLongLived;
			return info;
		}

		public Builder setActivity(ComponentName activity) {
			mActivity = Objects.requireNonNull(activity, "activity cannot be null");
			return this;
		}

		public Builder setCategories(Set<String> categories) {
			mCategories = categories;
			return this;
		}

		public Builder setDisabledMessage(CharSequence disabledMessage) {
			mDisabledMessage = disabledMessage;
			return this;
		}

		public Builder setExcludedFromSurfaces(int surfaces) {
			mExcludedSurfaces = surfaces;
			return this;
		}

		public Builder setExtras(PersistableBundle extras) {
			mExtras = extras;
			return this;
		}

		public Builder setIcon(Icon icon) {
			mIcon = validateIcon(icon);
			return this;
		}

		public Builder setIntent(Intent intent) {
			return setIntents(new Intent[] {intent});
		}

		/* AOSP: every intent needs an action, the builder keeps copies */
		public Builder setIntents(Intent[] intents) {
			Objects.requireNonNull(intents, "intents cannot be null");
			if (intents.length == 0) {
				throw new IllegalArgumentException("intents cannot be empty");
			}
			Intent[] copies = new Intent[intents.length];
			for (int i = 0; i < intents.length; i++) {
				Objects.requireNonNull(intents[i], "intents cannot contain null");
				Objects.requireNonNull(intents[i].getAction(), "intent's action must be set");
				copies[i] = new Intent(intents[i]);
			}
			mIntents = copies;
			return this;
		}

		public Builder setLocusId(LocusId locusId) {
			mLocusId = Objects.requireNonNull(locusId, "locusId cannot be null");
			return this;
		}

		public Builder setLongLabel(CharSequence longLabel) {
			if (TextUtils.isEmpty(longLabel)) {
				throw new IllegalArgumentException("longLabel cannot be empty");
			}
			mText = longLabel;
			return this;
		}

		public Builder setLongLived(boolean longLived) {
			mIsLongLived = longLived;
			return this;
		}

		public Builder setPerson(Person person) {
			return setPersons(new Person[] {person});
		}

		/* AOSP Builder.setPersons; clonePersons drops the icons via Person.toBuilder,
		   which ATL's Person lacks, so the array is copied as is */
		public Builder setPersons(Person[] persons) {
			Objects.requireNonNull(persons, "persons cannot be null");
			if (persons.length == 0) {
				throw new IllegalArgumentException("persons cannot be empty");
			}
			for (Person person : persons) {
				Objects.requireNonNull(person, "persons cannot contain null");
			}
			mPersons = persons.clone();
			return this;
		}

		public Builder setRank(int rank) {
			if (rank < 0) {
				throw new IllegalArgumentException("Rank cannot be negative");
			}
			mRank = rank;
			return this;
		}

		public Builder setShortLabel(CharSequence shortLabel) {
			if (TextUtils.isEmpty(shortLabel)) {
				throw new IllegalArgumentException("shortLabel cannot be empty");
			}
			mTitle = shortLabel;
			return this;
		}
	}
}
