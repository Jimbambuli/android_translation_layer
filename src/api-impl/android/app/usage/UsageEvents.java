package android.app.usage;

import android.content.res.Configuration;
import java.util.ArrayList;
import java.util.List;

/* AOSP UsageEvents without the Parcelable part. UsageStatsService, which records the events, is not part
 * of ATL: UsageStatsManager.queryEvents() answers with the empty result AOSP gives when the service has
 * none (no events for the app, or no PACKAGE_USAGE_STATS) */
public final class UsageEvents {
	public static final int SHOW_ALL_EVENT_DATA = 0x00000000;
	public static final int OBFUSCATE_INSTANT_APPS = 0x00000001;
	public static final int HIDE_SHORTCUT_EVENTS = 0x00000002;
	public static final int OBFUSCATE_NOTIFICATION_EVENTS = 0x00000004;
	public static final int HIDE_LOCUS_EVENTS = 0x00000008;

	public static final class Event {
		public static final int NONE = 0;
		public static final int MOVE_TO_FOREGROUND = 1;
		public static final int ACTIVITY_RESUMED = 1;
		public static final int MOVE_TO_BACKGROUND = 2;
		public static final int ACTIVITY_PAUSED = 2;
		public static final int END_OF_DAY = 3;
		public static final int CONTINUE_PREVIOUS_DAY = 4;
		public static final int CONFIGURATION_CHANGE = 5;
		public static final int SYSTEM_INTERACTION = 6;
		public static final int USER_INTERACTION = 7;
		public static final int SHORTCUT_INVOCATION = 8;
		public static final int CHOOSER_ACTION = 9;
		public static final int NOTIFICATION_SEEN = 10;
		public static final int STANDBY_BUCKET_CHANGED = 11;
		public static final int NOTIFICATION_INTERRUPTION = 12;
		public static final int SLICE_PINNED_PRIV = 13;
		public static final int SLICE_PINNED = 14;
		public static final int SCREEN_INTERACTIVE = 15;
		public static final int SCREEN_NON_INTERACTIVE = 16;
		public static final int KEYGUARD_SHOWN = 17;
		public static final int KEYGUARD_HIDDEN = 18;
		public static final int FOREGROUND_SERVICE_START = 19;
		public static final int FOREGROUND_SERVICE_STOP = 20;
		public static final int CONTINUING_FOREGROUND_SERVICE = 21;
		public static final int ROLLOVER_FOREGROUND_SERVICE = 22;
		public static final int ACTIVITY_STOPPED = 23;
		public static final int ACTIVITY_DESTROYED = 24;
		public static final int FLUSH_TO_DISK = 25;
		public static final int DEVICE_SHUTDOWN = 26;
		public static final int DEVICE_STARTUP = 27;
		public static final int USER_UNLOCKED = 28;
		public static final int USER_STOPPED = 29;
		public static final int LOCUS_ID_SET = 30;
		public static final int APP_COMPONENT_USED = 31;
		public static final int MAX_EVENT_TYPE = 31;
		public static final int FLAG_IS_PACKAGE_INSTANT_APP = 1 << 0;
		public static final int VALID_FLAG_BITS = FLAG_IS_PACKAGE_INSTANT_APP;

		public String mPackage;
		public String mClass;
		public int mInstanceId;
		public String mTaskRootPackage;
		public String mTaskRootClass;
		public long mTimeStamp;
		public int mEventType;
		public Configuration mConfiguration;
		public String mShortcutId;
		public int mBucketAndReason;
		public String mNotificationChannelId;
		public int mFlags;

		public Event() {}

		public Event(int type, long timeStamp) {
			mEventType = type;
			mTimeStamp = timeStamp;
		}

		public Event(Event orig) {
			copyFrom(orig);
		}

		void copyFrom(Event orig) {
			mPackage = orig.mPackage;
			mClass = orig.mClass;
			mInstanceId = orig.mInstanceId;
			mTaskRootPackage = orig.mTaskRootPackage;
			mTaskRootClass = orig.mTaskRootClass;
			mTimeStamp = orig.mTimeStamp;
			mEventType = orig.mEventType;
			mConfiguration = orig.mConfiguration;
			mShortcutId = orig.mShortcutId;
			mBucketAndReason = orig.mBucketAndReason;
			mNotificationChannelId = orig.mNotificationChannelId;
			mFlags = orig.mFlags;
		}

		public String getPackageName() { return mPackage; }
		public boolean isInstantApp() { return (mFlags & FLAG_IS_PACKAGE_INSTANT_APP) != 0; }
		public String getClassName() { return mClass; }
		public int getInstanceId() { return mInstanceId; }
		public String getTaskRootPackageName() { return mTaskRootPackage; }
		public String getTaskRootClassName() { return mTaskRootClass; }
		public long getTimeStamp() { return mTimeStamp; }
		public int getEventType() { return mEventType; }
		public Configuration getConfiguration() { return mConfiguration; }
		public String getShortcutId() { return mShortcutId; }
		public int getAppStandbyBucket() { return (mBucketAndReason & 0xFFFF0000) >>> 16; }
		public int getStandbyReason() { return mBucketAndReason & 0x0000FFFF; }
		public String getNotificationChannelId() { return mNotificationChannelId; }
	}

	private final List<Event> mEvents;
	private int mIndex;

	/* AOSP UsageStatsManager.sEmptyResults: new UsageEvents() */
	public UsageEvents() {
		mEvents = new ArrayList<>();
	}

	public UsageEvents(List<Event> events, String[] stringPool) {
		mEvents = new ArrayList<>(events);
	}

	public boolean hasNextEvent() {
		return mIndex < mEvents.size();
	}

	public boolean getNextEvent(Event eventOut) {
		if (eventOut == null)
			throw new IllegalArgumentException("Given eventOut must not be null");
		if (mIndex >= mEvents.size())
			return false;
		eventOut.copyFrom(mEvents.get(mIndex));
		mIndex++;
		return true;
	}

	public void resetToStart() {
		mIndex = 0;
	}
}
