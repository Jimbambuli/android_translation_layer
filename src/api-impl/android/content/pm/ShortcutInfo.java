package android.content.pm;

import android.app.Person;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.LocusId;
import android.graphics.drawable.Icon;
import android.os.Parcelable;
import android.os.PersistableBundle;
import java.util.Set;

public class ShortcutInfo implements Parcelable {
	public static final Creator<ShortcutInfo> CREATOR = null;

	private String mId;
	private Set<String> mCategories;
	private int mRank;

	public String getId() {
		return mId;
	}

	public Set<String> getCategories() {
		return mCategories;
	}

	public int getRank() {
		return mRank;
	}

	public static class Builder {
		private final String mId;
		private Set<String> mCategories;
		private int mRank;

		public Builder(Context context, String id) {
			mId = id;
		}

		public ShortcutInfo build() {
			ShortcutInfo info = new ShortcutInfo();
			info.mId = mId;
			info.mCategories = mCategories;
			info.mRank = mRank;
			return info;
		}

		public Builder setActivity(ComponentName activity) {
			return this;
		}

		public Builder setCategories(Set<String> categories) {
			mCategories = categories;
			return this;
		}

		public Builder setDisabledMessage(CharSequence disabledMessage) {
			return this;
		}

		public Builder setExcludedFromSurfaces(int surfaces) {
			return this;
		}

		public Builder setExtras(PersistableBundle extras) {
			return this;
		}

		public Builder setIcon(Icon icon) {
			return this;
		}

		public Builder setIntent(Intent intent) {
			return this;
		}

		public Builder setIntents(Intent[] intents) {
			return this;
		}

		public Builder setLocusId(LocusId locusId) {
			return this;
		}

		public Builder setLongLabel(CharSequence longLabel) {
			return this;
		}

		public Builder setLongLived(boolean longLived) {
			return this;
		}

		public Builder setPersons(Person[] persons) {
			return this;
		}

		public Builder setRank(int rank) {
			mRank = rank;
			return this;
		}

		public Builder setShortLabel(CharSequence shortLabel) {
			return this;
		}
	}
}
