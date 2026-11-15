package android.app;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Parcel;

/* AOSP frameworks/base core/java/android/app/TaskInfo.java (Android 16): the common base of
 * ActivityManager.RecentTaskInfo and RunningTaskInfo. ATL has the public fields plus the hidden userId,
 * taskId, isRunning and isVisible; the other hidden fields (configuration, window token, PiP, app compat
 * info, ...) have no ATL counterpart. readTaskFromParcel()/writeTaskToParcel() keep the AOSP order of
 * these fields; ATL has no Parcelable TaskDescription, so taskDescription is not parcelled. */
public class TaskInfo {
	/** @hide */
	public int userId;

	public int taskId;

	public boolean isRunning;

	public Intent baseIntent;

	public ComponentName baseActivity;

	public ComponentName topActivity;

	public ComponentName origActivity;

	public int numActivities;

	public ActivityManager.TaskDescription taskDescription;

	/** @hide */
	public boolean isVisible;

	TaskInfo() {
		// Do nothing
	}

	public boolean isVisible() {
		return isVisible;
	}

	/** @hide */
	void readTaskFromParcel(Parcel source) {
		userId = source.readInt();
		taskId = source.readInt();
		isRunning = source.readBoolean();
		baseIntent = source.readTypedObject(Intent.CREATOR);
		baseActivity = ComponentName.readFromParcel(source);
		topActivity = ComponentName.readFromParcel(source);
		origActivity = ComponentName.readFromParcel(source);
		numActivities = source.readInt();
		isVisible = source.readBoolean();
	}

	/** @hide */
	public void writeTaskToParcel(Parcel dest, int flags) {
		dest.writeInt(userId);
		dest.writeInt(taskId);
		dest.writeBoolean(isRunning);
		dest.writeTypedObject(baseIntent, 0);
		ComponentName.writeToParcel(baseActivity, dest);
		ComponentName.writeToParcel(topActivity, dest);
		ComponentName.writeToParcel(origActivity, dest);
		dest.writeInt(numActivities);
		dest.writeBoolean(isVisible);
	}
}
