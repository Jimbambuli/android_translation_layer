package android.app;

import android.atl.ATLLoadedApp;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Debug;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Process;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ActivityManager {

	public static class RunningAppProcessInfo {
		public int importance;
		public int pid;
		public int uid;
		public String processName;
		public String[] pkgList;
		public String[] pkgDeps;
		public int lru;
		public int lastTrimLevel;
		public int importanceReasonCode;
		public int importanceReasonPid;
		public ComponentName importanceReasonComponent;
		public int importanceReasonImportance;
		public int processState;

		public static final int IMPORTANCE_FOREGROUND = 100;
		public static final int IMPORTANCE_FOREGROUND_SERVICE = 125;
		public static final int IMPORTANCE_TOP_SLEEPING = 150;
		public static final int IMPORTANCE_VISIBLE = 200;
		public static final int IMPORTANCE_PERCEPTIBLE = 230;
		public static final int IMPORTANCE_CANT_SAVE_STATE = 350;
		public static final int IMPORTANCE_SERVICE = 300;
		public static final int IMPORTANCE_CACHED = 400;
		public static final int IMPORTANCE_GONE = 1000;
		public static final int REASON_UNKNOWN = 0;
		public static final int REASON_PROVIDER_IN_USE = 1;
		public static final int REASON_SERVICE_IN_USE = 2;

		public RunningAppProcessInfo(String processName, int pid, String[] pkgList) {
			this.processName = processName;
			this.pid = pid;
			this.pkgList = pkgList;
		}

		private RunningAppProcessInfo(int pid, String processName) {
			this.pid = pid;
			this.processName = processName;
			this.pkgList = new String[] {processName};
		}
	}

	public static class TaskDescription {
		public TaskDescription(String name) {}
		public TaskDescription(String name, Bitmap icon, int color) {}
	}

	public List<RunningAppProcessInfo> getRunningAppProcesses() {
		return Arrays.asList(new RunningAppProcessInfo(Process.myPid(),
		                                               ATLLoadedApp.getPrimaryApplication().pkg.packageName));
	}

	public boolean isLowRamDevice() { return false; }

	public static class MemoryInfo implements Parcelable {
		/* For now, just always report there's 10GB free RAM */
		public long availMem = 10000;

		public long totalMem = 10000;

		public long advertisedMem = 10000;

		public long threshold = 200;

		public boolean lowMemory = false;

		public int describeContents() {
			return 0;
		}

		public void writeToParcel(Parcel dest, int flags) {
			dest.writeLong(availMem);
			dest.writeLong(totalMem);
			dest.writeLong(advertisedMem);
			dest.writeLong(threshold);
			dest.writeInt(lowMemory ? 1 : 0);
		}

		public void readFromParcel(Parcel source) {
			availMem = source.readLong();
			totalMem = source.readLong();
			advertisedMem = source.readLong();
			threshold = source.readLong();
			lowMemory = source.readInt() != 0;
		}

		public static final Parcelable.Creator<MemoryInfo> CREATOR = new Parcelable.Creator<MemoryInfo>() {
			public MemoryInfo createFromParcel(Parcel in) {
				MemoryInfo info = new MemoryInfo();
				info.readFromParcel(in);
				return info;
			}

			public MemoryInfo[] newArray(int size) {
				return new MemoryInfo[size];
			}
		};
	}

	public void getMemoryInfo(MemoryInfo outInfo) {
		outInfo = new MemoryInfo();
	}

	public ConfigurationInfo getDeviceConfigurationInfo() {
		return new ConfigurationInfo();
	}

	public int getMemoryClass() { return 20; }      // suggested heap size in MB
	public int getLargeMemoryClass() { return 60; } // value chosen arbitrarily

	public static void getMyMemoryState(RunningAppProcessInfo outInfo) {}

	/* AOSP ActivityManager.getProcessMemoryInfo(): ActivityManagerService fills each MemoryInfo with
	 * Debug.getMemoryInfo() of the process */
	public Debug.MemoryInfo[] getProcessMemoryInfo(int[] pids) {
		Debug.MemoryInfo[] infos = new Debug.MemoryInfo[pids.length];
		for (int i = 0; i < pids.length; i++) {
			infos[i] = new Debug.MemoryInfo();
			Debug.getMemoryInfo(infos[i]);
		}
		return infos;
	}

	public boolean clearApplicationUserData() { return false; }

	public static class ProcessErrorStateInfo implements Parcelable {
		public int condition;
		public String processName;
		public int pid;
		public int uid;
		public String tag;
		public String shortMsg;
		public String longMsg;
		public byte[] crashData;
		public int uid_mm;
		public int etype;
		public int euid;
		public int epid;
		public String eproc;
		public String edata;
		public Map<String, Object> extras;

		public static final int CRASHED = 1;
		public static final int NOT_RESPONDING = 2;
		public static final int NO_ERROR = 0;

		public int describeContents() {
			return 0;
		}

		public void writeToParcel(Parcel dest, int flags) {
			dest.writeInt(condition);
			dest.writeString(processName);
		}

		public void readFromParcel(Parcel source) {
			condition = source.readInt();
			processName = source.readString();
		}

		public static final Parcelable.Creator<ProcessErrorStateInfo> CREATOR = new Parcelable.Creator<ProcessErrorStateInfo>() {
			public ProcessErrorStateInfo createFromParcel(Parcel in) {
				ProcessErrorStateInfo info = new ProcessErrorStateInfo();
				info.readFromParcel(in);
				return info;
			}

			public ProcessErrorStateInfo[] newArray(int size) {
				return new ProcessErrorStateInfo[size];
			}
		};
	}

	public static class AppTask {
		public RecentTaskInfo getTaskInfo() {
			return new RecentTaskInfo();
		}
	}
	public List<ActivityManager.AppTask> getAppTasks() {
		return new ArrayList<>();
	}

	public static class RecentTaskInfo implements Parcelable {
		public int id;
		public int persistentId;
		public android.content.Intent baseIntent;
		public android.content.ComponentName origActivity;
		public CharSequence description;
		public int userId;

		public int describeContents() {
			return 0;
		}

		public void writeToParcel(Parcel dest, int flags) {
			dest.writeInt(id);
			dest.writeLong(persistentId);
		}

		public void readFromParcel(Parcel source) {
			id = source.readInt();
			persistentId = (int)source.readLong();
		}

		public static final Parcelable.Creator<RecentTaskInfo> CREATOR = new Parcelable.Creator<RecentTaskInfo>() {
			public RecentTaskInfo createFromParcel(Parcel in) {
				RecentTaskInfo info = new RecentTaskInfo();
				info.readFromParcel(in);
				return info;
			}

			public RecentTaskInfo[] newArray(int size) {
				return new RecentTaskInfo[size];
			}
		};
	}

	public static class RunningTaskInfo implements Parcelable {
		public int id;
		public android.content.ComponentName baseActivity;
		public android.content.ComponentName topActivity;
		public int numActivities;
		public int numRunning;
		public android.graphics.Bitmap thumbnail;
		public CharSequence description;

		public int describeContents() {
			return 0;
		}

		public void writeToParcel(Parcel dest, int flags) {
			dest.writeInt(id);
			dest.writeInt(numActivities);
			dest.writeInt(numRunning);
		}

		public void readFromParcel(Parcel source) {
			id = source.readInt();
			numActivities = source.readInt();
			numRunning = source.readInt();
		}

		public static final Parcelable.Creator<RunningTaskInfo> CREATOR = new Parcelable.Creator<RunningTaskInfo>() {
			public RunningTaskInfo createFromParcel(Parcel in) {
				RunningTaskInfo info = new RunningTaskInfo();
				info.readFromParcel(in);
				return info;
			}

			public RunningTaskInfo[] newArray(int size) {
				return new RunningTaskInfo[size];
			}
		};
	}

	public List<RecentTaskInfo> getRecentTasks(int maxNum, int flags) {
		return new ArrayList<>();
	}

	public List<RunningTaskInfo> getRunningTasks(int maxNum) {
		return new ArrayList<>();
	}

	public List<ActivityManager.ProcessErrorStateInfo> getProcessesInErrorState() {
		return new ArrayList<>();
	}

	public static class RunningServiceInfo implements Parcelable {
		public android.content.ComponentName service;
		public int pid;
		public int uid;
		public String process;
		public boolean foreground;
		public long activeSince;
		public boolean started;
		public int clientCount;
		public int clientPackageCount;
		public int crashCount;
		public long restarting;
		public int flags;
		public String clientPackage;

		public RunningServiceInfo() {
		}

		public int describeContents() {
			return 0;
		}

		public void writeToParcel(Parcel dest, int flags) {
			return;
		}

		public void readFromParcel(Parcel source) {
			return;
		}
	}

	public List<RunningServiceInfo> getRunningServices(int maxNum)
	    throws SecurityException {
		return new ArrayList<>();
	}

	public List<ApplicationExitInfo> getHistoricalProcessExitReasons(String pkgname, int pid, int maxNum) {
		return Collections.emptyList();
	}

	/**
	 * Set custom state data for this process. It will be included in the record of
	 * {@link ApplicationExitInfo} on the death of the current calling process; ATL keeps
	 * no such records (getHistoricalProcessExitReasons() is empty).
	 */
	public void setProcessStateSummary(byte[] state) {
		if (state != null && state.length > 128) {
			throw new IllegalArgumentException("Data size is too large");
		}
	}

	public static boolean isUserAMonkey() { return false; }

	/* AOSP ActivityManager.isRunningInTestHarness()/isRunningInUserTestHarness(): never a test harness */
	public static boolean isRunningInTestHarness() { return false; }
	public static boolean isRunningInUserTestHarness() { return false; }

	public void moveTaskToFront(int taskId, int flags, Bundle options) {
	}
}
