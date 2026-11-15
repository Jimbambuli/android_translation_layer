package android.app.usage;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.ArraySet;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/* AOSP UsageEventsQuery (android-16.0.0_r4): the filter of UsageStatsManager.queryEvents(UsageEventsQuery) */
public final class UsageEventsQuery implements Parcelable {
	private final long mBeginTimeMillis;
	private final long mEndTimeMillis;
	private final int[] mEventTypes;
	private final int mUserId;
	private final String[] mPackageNames;

	private UsageEventsQuery(Builder builder) {
		mBeginTimeMillis = builder.mBeginTimeMillis;
		mEndTimeMillis = builder.mEndTimeMillis;
		mEventTypes = new int[builder.mEventTypes.size()];
		for (int i = 0; i < mEventTypes.length; i++)
			mEventTypes[i] = builder.mEventTypes.valueAt(i);
		mUserId = builder.mUserId;
		mPackageNames = builder.mPackageNames.toArray(new String[builder.mPackageNames.size()]);
	}

	private UsageEventsQuery(Parcel in) {
		mBeginTimeMillis = in.readLong();
		mEndTimeMillis = in.readLong();
		int eventTypesLength = in.readInt();
		mEventTypes = new int[eventTypesLength];
		in.readIntArray(mEventTypes);
		mUserId = in.readInt();
		int packageNamesLength = in.readInt();
		mPackageNames = new String[packageNamesLength];
		in.readStringArray(mPackageNames);
	}

	public long getBeginTimeMillis() {
		return mBeginTimeMillis;
	}

	public long getEndTimeMillis() {
		return mEndTimeMillis;
	}

	public int[] getEventTypes() {
		return Arrays.copyOf(mEventTypes, mEventTypes.length);
	}

	public int getUserId() {
		return mUserId;
	}

	public Set<String> getPackageNames() {
		if (mPackageNames == null || mPackageNames.length == 0)
			return Collections.emptySet();
		HashSet<String> pkgNameSet = new HashSet<>();
		for (String pkgName : mPackageNames)
			pkgNameSet.add(pkgName);
		return pkgNameSet;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeLong(mBeginTimeMillis);
		dest.writeLong(mEndTimeMillis);
		dest.writeInt(mEventTypes.length);
		dest.writeIntArray(mEventTypes);
		dest.writeInt(mUserId);
		dest.writeInt(mPackageNames.length);
		dest.writeStringArray(mPackageNames);
	}

	public static final Creator<UsageEventsQuery> CREATOR = new Creator<UsageEventsQuery>() {
		@Override
		public UsageEventsQuery createFromParcel(Parcel in) {
			return new UsageEventsQuery(in);
		}

		@Override
		public UsageEventsQuery[] newArray(int size) {
			return new UsageEventsQuery[size];
		}
	};

	public static final class Builder {
		private final long mBeginTimeMillis;
		private final long mEndTimeMillis;
		private final ArraySet<Integer> mEventTypes = new ArraySet<>();
		private int mUserId = UserHandle.USER_NULL;
		private final ArraySet<String> mPackageNames = new ArraySet<>();

		public Builder(long beginTimeMillis, long endTimeMillis) {
			if (beginTimeMillis < 0 || endTimeMillis < beginTimeMillis)
				throw new IllegalArgumentException("Invalid period");
			mBeginTimeMillis = beginTimeMillis;
			mEndTimeMillis = endTimeMillis;
		}

		public UsageEventsQuery build() {
			return new UsageEventsQuery(this);
		}

		public Builder setEventTypes(int... eventTypes) {
			if (eventTypes == null || eventTypes.length == 0)
				throw new NullPointerException("eventTypes is null or empty");
			mEventTypes.clear();
			for (int i = 0; i < eventTypes.length; i++) {
				final int eventType = eventTypes[i];
				if (eventType < UsageEvents.Event.NONE || eventType > UsageEvents.Event.MAX_EVENT_TYPE)
					throw new IllegalArgumentException("Invalid usage event type: " + eventType);
				mEventTypes.add(eventType);
			}
			return this;
		}

		public Builder setUserId(int userId) {
			mUserId = userId;
			return this;
		}

		public Builder setPackageNames(String... pkgNames) {
			if (pkgNames == null || pkgNames.length == 0)
				throw new NullPointerException("pkgNames is null or empty");
			mPackageNames.clear();
			for (int i = 0; i < pkgNames.length; i++) {
				if (!TextUtils.isEmpty(pkgNames[i]))
					mPackageNames.add(pkgNames[i]);
			}
			return this;
		}
	}
}
