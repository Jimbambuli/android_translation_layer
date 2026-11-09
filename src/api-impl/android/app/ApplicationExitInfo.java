package android.app;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.UserHandle;

import java.io.File;
import java.util.Arrays;
import java.util.Objects;

/**
 * Describes the exit of a process of an app (AOSP ApplicationExitInfo). ATL has no system service that
 * records the exits, ActivityManager.getHistoricalProcessExitReasons() gives an empty list; the class
 * is the value type of AOSP with its getters, its constants and the Parcelable.
 */
public final class ApplicationExitInfo implements Parcelable {
    public static final int REASON_UNKNOWN = 0;
    public static final int REASON_EXIT_SELF = 1;
    public static final int REASON_SIGNALED = 2;
    public static final int REASON_LOW_MEMORY = 3;
    public static final int REASON_CRASH = 4;
    public static final int REASON_CRASH_NATIVE = 5;
    public static final int REASON_ANR = 6;
    public static final int REASON_INITIALIZATION_FAILURE = 7;
    public static final int REASON_PERMISSION_CHANGE = 8;
    public static final int REASON_EXCESSIVE_RESOURCE_USAGE = 9;
    public static final int REASON_USER_REQUESTED = 10;
    public static final int REASON_USER_STOPPED = 11;
    public static final int REASON_DEPENDENCY_DIED = 12;
    public static final int REASON_OTHER = 13;
    public static final int REASON_FREEZER = 14;
    public static final int REASON_PACKAGE_STATE_CHANGE = 15;
    public static final int REASON_PACKAGE_UPDATED = 16;
    public static final int SUBREASON_UNKNOWN = 0;
    public static final int SUBREASON_WAIT_FOR_DEBUGGER = 1;
    public static final int SUBREASON_TOO_MANY_CACHED = 2;
    public static final int SUBREASON_TOO_MANY_EMPTY = 3;
    public static final int SUBREASON_TRIM_EMPTY = 4;
    public static final int SUBREASON_LARGE_CACHED = 5;
    public static final int SUBREASON_MEMORY_PRESSURE = 6;
    public static final int SUBREASON_EXCESSIVE_CPU = 7;
    public static final int SUBREASON_SYSTEM_UPDATE_DONE = 8;
    public static final int SUBREASON_KILL_ALL_FG = 9;
    public static final int SUBREASON_KILL_ALL_BG_EXCEPT = 10;
    public static final int SUBREASON_KILL_UID = 11;
    public static final int SUBREASON_KILL_PID = 12;
    public static final int SUBREASON_INVALID_START = 13;
    public static final int SUBREASON_INVALID_STATE = 14;
    public static final int SUBREASON_IMPERCEPTIBLE = 15;
    public static final int SUBREASON_REMOVE_LRU = 16;
    public static final int SUBREASON_ISOLATED_NOT_NEEDED = 17;
    public static final int SUBREASON_CACHED_IDLE_FORCED_APP_STANDBY = 18;
    public static final int SUBREASON_FREEZER_BINDER_IOCTL = 19;
    public static final int SUBREASON_FREEZER_BINDER_TRANSACTION = 20;
    public static final int SUBREASON_FORCE_STOP = 21;
    public static final int SUBREASON_REMOVE_TASK = 22;
    public static final int SUBREASON_STOP_APP = 23;
    public static final int SUBREASON_KILL_BACKGROUND = 24;
    public static final int SUBREASON_PACKAGE_UPDATE = 25;
    public static final int SUBREASON_UNDELIVERED_BROADCAST = 26;
    public static final int SUBREASON_SDK_SANDBOX_DIED = 27;
    public static final int SUBREASON_SDK_SANDBOX_NOT_NEEDED = 28;
    public static final int SUBREASON_EXCESSIVE_BINDER_OBJECTS = 29;
    public static final int SUBREASON_OOM_KILL = 30;
    public static final int SUBREASON_FREEZER_BINDER_ASYNC_FULL = 31;

    private int mPid;
    private int mRealUid;
    private int mPackageUid;
    private int mDefiningUid;
    private String mProcessName;
    private String mPackageName;
    private int mReason;
    private int mSubReason;
    private int mStatus;
    private int mImportance;
    private long mPss;
    private long mRss;
    private long mTimestamp;
    private String mDescription;
    private byte[] mState;
    private File mTraceFile;

    /** @hide */
    public ApplicationExitInfo() {
    }

    public int getPid() { return mPid; }
    public int getRealUid() { return mRealUid; }
    public int getPackageUid() { return mPackageUid; }
    public int getDefiningUid() { return mDefiningUid; }
    public String getProcessName() { return mProcessName; }
    public int getReason() { return mReason; }
    public int getStatus() { return mStatus; }
    public int getImportance() { return mImportance; }
    public long getPss() { return mPss; }
    public long getRss() { return mRss; }
    public long getTimestamp() { return mTimestamp; }
    public String getDescription() { return mDescription; }
    public UserHandle getUserHandle() { return new UserHandle(UserHandle.getUserId(mRealUid)); }
    public byte[] getProcessStateSummary() { return mState; }
    public java.io.InputStream getTraceInputStream() throws java.io.IOException {
        if (mTraceFile == null || !mTraceFile.exists())
            return null;
        return new java.io.FileInputStream(mTraceFile);
    }

    /** @hide */ public int getSubReason() { return mSubReason; }
    /** @hide */ public String getPackageName() { return mPackageName; }
    /** @hide */ public void setPid(int pid) { mPid = pid; }
    /** @hide */ public void setRealUid(int uid) { mRealUid = uid; }
    /** @hide */ public void setPackageUid(int uid) { mPackageUid = uid; }
    /** @hide */ public void setDefiningUid(int uid) { mDefiningUid = uid; }
    /** @hide */ public void setProcessName(String processName) { mProcessName = processName; }
    /** @hide */ public void setPackageName(String packageName) { mPackageName = packageName; }
    /** @hide */ public void setReason(int reason) { mReason = reason; }
    /** @hide */ public void setSubReason(int subReason) { mSubReason = subReason; }
    /** @hide */ public void setStatus(int status) { mStatus = status; }
    /** @hide */ public void setImportance(int importance) { mImportance = importance; }
    /** @hide */ public void setPss(long pss) { mPss = pss; }
    /** @hide */ public void setRss(long rss) { mRss = rss; }
    /** @hide */ public void setTimestamp(long timestamp) { mTimestamp = timestamp; }
    /** @hide */ public void setDescription(String description) { mDescription = description; }
    /** @hide */ public void setProcessStateSummary(byte[] state) { mState = state; }
    /** @hide */ public void setTraceFile(File traceFile) { mTraceFile = traceFile; }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mPid);
        dest.writeInt(mRealUid);
        dest.writeInt(mPackageUid);
        dest.writeInt(mDefiningUid);
        dest.writeString(mProcessName);
        dest.writeString(mPackageName);
        dest.writeInt(mReason);
        dest.writeInt(mSubReason);
        dest.writeInt(mStatus);
        dest.writeInt(mImportance);
        dest.writeLong(mPss);
        dest.writeLong(mRss);
        dest.writeLong(mTimestamp);
        dest.writeString(mDescription);
        dest.writeByteArray(mState);
        dest.writeString(mTraceFile != null ? mTraceFile.getPath() : null);
    }

    private ApplicationExitInfo(Parcel in) {
        mPid = in.readInt();
        mRealUid = in.readInt();
        mPackageUid = in.readInt();
        mDefiningUid = in.readInt();
        mProcessName = in.readString();
        mPackageName = in.readString();
        mReason = in.readInt();
        mSubReason = in.readInt();
        mStatus = in.readInt();
        mImportance = in.readInt();
        mPss = in.readLong();
        mRss = in.readLong();
        mTimestamp = in.readLong();
        mDescription = in.readString();
        mState = in.createByteArray();
        String trace = in.readString();
        mTraceFile = trace != null ? new File(trace) : null;
    }

    public static final Creator<ApplicationExitInfo> CREATOR = new Creator<ApplicationExitInfo>() {
        @Override
        public ApplicationExitInfo createFromParcel(Parcel in) {
            return new ApplicationExitInfo(in);
        }

        @Override
        public ApplicationExitInfo[] newArray(int size) {
            return new ApplicationExitInfo[size];
        }
    };

    @Override
    public boolean equals(Object other) {
        if (other == this)
            return true;
        if (!(other instanceof ApplicationExitInfo))
            return false;
        ApplicationExitInfo o = (ApplicationExitInfo)other;
        return mPid == o.mPid && mRealUid == o.mRealUid && mPackageUid == o.mPackageUid && mDefiningUid == o.mDefiningUid
            && mReason == o.mReason && mSubReason == o.mSubReason && mImportance == o.mImportance && mStatus == o.mStatus
            && mTimestamp == o.mTimestamp && mPss == o.mPss && mRss == o.mRss
            && Objects.equals(mProcessName, o.mProcessName) && Objects.equals(mDescription, o.mDescription)
            && Arrays.equals(mState, o.mState);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mPid, mRealUid, mPackageUid, mDefiningUid, mProcessName, mReason, mSubReason, mStatus, mImportance, mPss, mRss, mTimestamp, mDescription);
    }

    @Override
    public String toString() {
        return "ApplicationExitInfo{pid=" + mPid + " realUid=" + mRealUid + " packageUid=" + mPackageUid + " definingUid=" + mDefiningUid
            + " user=" + UserHandle.getUserId(mRealUid) + " process=" + mProcessName + " reason=" + mReason + " subreason=" + mSubReason
            + " status=" + mStatus + " importance=" + mImportance + " pss=" + mPss + " rss=" + mRss + " time=" + mTimestamp
            + " desc=" + mDescription + "}";
    }
}
