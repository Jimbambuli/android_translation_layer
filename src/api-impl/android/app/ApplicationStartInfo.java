package android.app;

import android.content.Intent;
import java.util.HashMap;
import java.util.Map;

/* AOSP ApplicationStartInfo (android-16.0.0_r4, ApplicationStartInfo.java): the record of one start of the process,
 * without Parcelable and the system-side setters. ATL keeps no start records, so it has only what an app can read. */
public final class ApplicationStartInfo {
	public static final int STARTUP_STATE_STARTED = 0;
	public static final int STARTUP_STATE_ERROR = 1;
	public static final int STARTUP_STATE_FIRST_FRAME_DRAWN = 2;
	public static final int START_REASON_ALARM = 0;
	public static final int START_REASON_BACKUP = 1;
	public static final int START_REASON_BOOT_COMPLETE = 2;
	public static final int START_REASON_BROADCAST = 3;
	public static final int START_REASON_CONTENT_PROVIDER = 4;
	public static final int START_REASON_JOB = 5;
	public static final int START_REASON_LAUNCHER = 6;
	public static final int START_REASON_LAUNCHER_RECENTS = 7;
	public static final int START_REASON_OTHER = 8;
	public static final int START_REASON_PUSH = 9;
	public static final int START_REASON_SERVICE = 10;
	public static final int START_REASON_START_ACTIVITY = 11;
	public static final int START_TYPE_UNSET = 0;
	public static final int START_TYPE_COLD = 1;
	public static final int START_TYPE_WARM = 2;
	public static final int START_TYPE_HOT = 3;
	public static final int LAUNCH_MODE_STANDARD = 0;
	public static final int LAUNCH_MODE_SINGLE_TOP = 1;
	public static final int LAUNCH_MODE_SINGLE_INSTANCE = 2;
	public static final int LAUNCH_MODE_SINGLE_TASK = 3;
	public static final int LAUNCH_MODE_SINGLE_INSTANCE_PER_TASK = 4;
	public static final int START_TIMESTAMP_RESERVED_RANGE_SYSTEM = 20;
	public static final int START_TIMESTAMP_RESERVED_RANGE_DEVELOPER_START = START_TIMESTAMP_RESERVED_RANGE_SYSTEM + 1;
	public static final int START_TIMESTAMP_RESERVED_RANGE_DEVELOPER = 30;
	public static final int START_TIMESTAMP_LAUNCH = 0;
	public static final int START_TIMESTAMP_FORK = 1;
	public static final int START_TIMESTAMP_APPLICATION_ONCREATE = 2;
	public static final int START_TIMESTAMP_BIND_APPLICATION = 3;
	public static final int START_TIMESTAMP_FIRST_FRAME = 4;
	public static final int START_TIMESTAMP_FULLY_DRAWN = 5;
	public static final int START_TIMESTAMP_INITIAL_RENDERTHREAD_FRAME = 6;
	public static final int START_TIMESTAMP_SURFACEFLINGER_COMPOSITION_COMPLETE = 7;
	public static final int START_COMPONENT_ACTIVITY = 1;
	public static final int START_COMPONENT_BROADCAST = 2;
	public static final int START_COMPONENT_CONTENT_PROVIDER = 3;
	public static final int START_COMPONENT_SERVICE = 4;
	public static final int START_COMPONENT_OTHER = 5;

	private long mMonotonicCreationTimeMs;
	private int mStartupState;
	private int mPid;
	private int mRealUid;
	private int mPackageUid;
	private int mDefiningUid;
	private String mPackageName;
	private String mProcessName;
	private int mReason;
	private Map<Integer, Long> mStartupTimestampsNs;
	private int mStartType;
	private Intent mStartIntent;
	private int mLaunchMode;
	private boolean mWasForceStopped;
	private int mStartComponent;

	public int getStartupState() {
		return mStartupState;
	}

	public long getMonotonicCreationTimeMs() {
		return mMonotonicCreationTimeMs;
	}

	public int getPid() {
		return mPid;
	}

	public int getRealUid() {
		return mRealUid;
	}

	public int getPackageUid() {
		return mPackageUid;
	}

	public int getDefiningUid() {
		return mDefiningUid;
	}

	public String getPackageName() {
		return mPackageName;
	}

	public String getProcessName() {
		return mProcessName;
	}

	public int getReason() {
		return mReason;
	}

	public int getStartType() {
		return mStartType;
	}

	public Intent getIntent() {
		return mStartIntent;
	}

	public int getLaunchMode() {
		return mLaunchMode;
	}

	public boolean wasForceStopped() {
		return mWasForceStopped;
	}

	public int getStartComponent() {
		return mStartComponent;
	}

	public Map<Integer, Long> getStartupTimestamps() {
		if (mStartupTimestampsNs == null) {
			mStartupTimestampsNs = new HashMap<>();
		}
		return mStartupTimestampsNs;
	}
}
