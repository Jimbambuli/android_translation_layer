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

		/* AOSP RunningAppProcessInfo(): a foreground process of unknown reason */
		public RunningAppProcessInfo() {
			importance = IMPORTANCE_FOREGROUND;
			importanceReasonCode = REASON_UNKNOWN;
			processState = 6; /* PROCESS_STATE_IMPORTANT_FOREGROUND */
		}

		public RunningAppProcessInfo(String processName, int pid, String[] pkgList) {
			this.processName = processName;
			this.pid = pid;
			this.pkgList = pkgList;
		}
	}

	/* the entry of the task in the list of recent apps; ATL has no such list, the window has its own title */
	public static class TaskDescription {
		private String mLabel;
		private android.graphics.drawable.Icon mIcon;
		private int mColorPrimary;
		private int mColorBackground;
		private int mStatusBarColor;
		private int mNavigationBarColor;

		/* AOSP ActivityManager.TaskDescription.Builder */
		public static final class Builder {
			private String mLabel = null;
			private int mIconRes = 0;
			private int mPrimaryColor = 0;
			private int mBackgroundColor = 0;
			private int mStatusBarColor = 0;
			private int mNavigationBarColor = 0;

			public Builder setLabel(String label) {
				mLabel = label;
				return this;
			}

			public Builder setIcon(int iconRes) {
				mIconRes = iconRes;
				return this;
			}

			public Builder setPrimaryColor(int color) {
				mPrimaryColor = color;
				return this;
			}

			public Builder setBackgroundColor(int color) {
				mBackgroundColor = color;
				return this;
			}

			public Builder setStatusBarColor(int color) {
				mStatusBarColor = color;
				return this;
			}

			public Builder setNavigationBarColor(int color) {
				mNavigationBarColor = color;
				return this;
			}

			public TaskDescription build() {
				final android.graphics.drawable.Icon icon = mIconRes == 0 ? null : android.graphics.drawable.Icon.createWithResource(ActivityThread.currentPackageName(), mIconRes);
				return new TaskDescription(mLabel, icon, mPrimaryColor, mBackgroundColor, mStatusBarColor, mNavigationBarColor);
			}
		}

		/* AOSP: the label, icon and colors are kept for the system's recents list; ATL has none (the desktop
		 * window has its own title), so nothing reads them but the getters */
		public TaskDescription(String label, android.graphics.drawable.Icon icon, int colorPrimary, int colorBackground, int statusBarColor, int navigationBarColor) {
			mLabel = label;
			mIcon = icon;
			mColorPrimary = colorPrimary;
			mColorBackground = colorBackground;
			mStatusBarColor = statusBarColor;
			mNavigationBarColor = navigationBarColor;
		}

		public TaskDescription() {
			this(null, null, 0, 0, 0, 0);
		}

		public TaskDescription(String label) {
			this(label, null, 0, 0, 0, 0);
		}

		public TaskDescription(String label, Bitmap icon) {
			this(label, icon != null ? android.graphics.drawable.Icon.createWithBitmap(icon) : null, 0, 0, 0, 0);
		}

		public TaskDescription(String label, Bitmap icon, int colorPrimary) {
			this(label, icon != null ? android.graphics.drawable.Icon.createWithBitmap(icon) : null, colorPrimary, 0, 0, 0);
			if ((colorPrimary != 0) && (android.graphics.Color.alpha(colorPrimary) != 255))
				throw new RuntimeException("A TaskDescription's primary color should be opaque");
		}

		public TaskDescription(String label, int iconRes) {
			this(label, android.graphics.drawable.Icon.createWithResource(ActivityThread.currentPackageName(), iconRes), 0, 0, 0, 0);
		}

		public TaskDescription(String label, int iconRes, int colorPrimary) {
			this(label, android.graphics.drawable.Icon.createWithResource(ActivityThread.currentPackageName(), iconRes), colorPrimary, 0, 0, 0);
			if ((colorPrimary != 0) && (android.graphics.Color.alpha(colorPrimary) != 255))
				throw new RuntimeException("A TaskDescription's primary color should be opaque");
		}

		public TaskDescription(TaskDescription td) {
			this(td.mLabel, td.mIcon, td.mColorPrimary, td.mColorBackground, td.mStatusBarColor, td.mNavigationBarColor);
		}

		public String getLabel() { return mLabel; }

		public int getPrimaryColor() { return mColorPrimary; }

		public int getBackgroundColor() { return mColorBackground; }

		public int getStatusBarColor() { return mStatusBarColor; }

		public int getNavigationBarColor() { return mNavigationBarColor; }

		public void setLabel(String label) { mLabel = label; }

		public void setPrimaryColor(int primaryColor) {
			if ((primaryColor != 0) && (android.graphics.Color.alpha(primaryColor) != 255))
				throw new RuntimeException("A TaskDescription's primary color should be opaque");
			mColorPrimary = primaryColor;
		}

		public void setBackgroundColor(int backgroundColor) {
			if ((backgroundColor != 0) && (android.graphics.Color.alpha(backgroundColor) != 255))
				throw new RuntimeException("A TaskDescription's background color should be opaque");
			mColorBackground = backgroundColor;
		}

		public void setStatusBarColor(int statusBarColor) { mStatusBarColor = statusBarColor; }

		public void setNavigationBarColor(int navigationBarColor) { mNavigationBarColor = navigationBarColor; }

		public void setIcon(android.graphics.drawable.Icon icon) { mIcon = icon; }
	}

	/* the app's processes (android:process, see ATLProcess); a process started by the main process knows itself */
	public List<RunningAppProcessInfo> getRunningAppProcesses() {
		ArrayList<RunningAppProcessInfo> ret = new ArrayList<>();
		for (java.util.Map.Entry<Integer, String> p : android.atl.ATLProcess.runningProcesses().entrySet())
			ret.add(new RunningAppProcessInfo(p.getValue(), p.getKey(), new String[] {ATLLoadedApp.getPrimaryApplication().pkg.packageName}));
		return ret;
	}

	public boolean isLowRamDevice() { return false; }

	/* AOSP ActivityManager.isLowRamDeviceStatic(): ro.config.low_ram, not set on a desktop */
	public static boolean isLowRamDeviceStatic() {
		return false;
	}
	/* AOSP ActivityManager.LOCK_TASK_MODE_* and getLockTaskModeState(): no task is ever locked */
	public static final int LOCK_TASK_MODE_NONE = 0;
	public static final int LOCK_TASK_MODE_LOCKED = 1;
	public static final int LOCK_TASK_MODE_PINNED = 2;

	public int getLockTaskModeState() { return LOCK_TASK_MODE_NONE; }

	public static boolean isLowMemoryKillReportSupported() {
		return android.os.SystemProperties.getBoolean("persist.sys.lmk.reportkills", false);
	}


	/* AOSP ActivityManager.isBackgroundRestricted() asks the service whether the user restricted the
	 * app in the background (app op RUN_ANY_IN_BACKGROUND); ATL has no such setting and its
	 * AppOpsManager allows every op, so the app is never restricted */
	public boolean isBackgroundRestricted() { return false; }

	/* AOSP ActivityManager.getLauncherLargeIconSizeInner: app_icon_size, one density step larger from
	 * smallestScreenWidthDp 600 on (the framework dimen is the same in the app's and the system resources) */
	public int getLauncherLargeIconSize() {
		final android.content.res.Resources res = android.content.res.Resources.getSystem();
		final int size = res.getDimensionPixelSize(android.R.dimen.app_icon_size);
		final int sw = res.getConfiguration().smallestScreenWidthDp;
		if (sw < 600)
			return size;
		switch (res.getDisplayMetrics().densityDpi) {
			case android.util.DisplayMetrics.DENSITY_LOW:
				return (size * android.util.DisplayMetrics.DENSITY_MEDIUM) / android.util.DisplayMetrics.DENSITY_LOW;
			case android.util.DisplayMetrics.DENSITY_MEDIUM:
				return (size * android.util.DisplayMetrics.DENSITY_HIGH) / android.util.DisplayMetrics.DENSITY_MEDIUM;
			case android.util.DisplayMetrics.DENSITY_TV:
				return (size * android.util.DisplayMetrics.DENSITY_XHIGH) / android.util.DisplayMetrics.DENSITY_HIGH;
			case android.util.DisplayMetrics.DENSITY_HIGH:
				return (size * android.util.DisplayMetrics.DENSITY_XHIGH) / android.util.DisplayMetrics.DENSITY_HIGH;
			case android.util.DisplayMetrics.DENSITY_XHIGH:
				return (size * android.util.DisplayMetrics.DENSITY_XXHIGH) / android.util.DisplayMetrics.DENSITY_XHIGH;
			case android.util.DisplayMetrics.DENSITY_XXHIGH:
				return (size * android.util.DisplayMetrics.DENSITY_XHIGH * 2) / android.util.DisplayMetrics.DENSITY_XXHIGH;
			default:
				return (int)((size * 1.5f) + .5f);
		}
	}

	public static class MemoryInfo implements Parcelable {
		public long availMem;

		public long totalMem;

		public long advertisedMem;

		public long threshold;

		public boolean lowMemory;

		public long hiddenAppThreshold;

		public long secondaryServerThreshold;

		public long visibleAppThreshold;

		public long foregroundAppThreshold;

		public int describeContents() {
			return 0;
		}

		public void writeToParcel(Parcel dest, int flags) {
			dest.writeLong(availMem);
			dest.writeLong(totalMem);
			dest.writeLong(advertisedMem);
			dest.writeLong(threshold);
			dest.writeInt(lowMemory ? 1 : 0);
			dest.writeLong(hiddenAppThreshold);
			dest.writeLong(secondaryServerThreshold);
			dest.writeLong(visibleAppThreshold);
			dest.writeLong(foregroundAppThreshold);
		}

		public void readFromParcel(Parcel source) {
			availMem = source.readLong();
			totalMem = source.readLong();
			advertisedMem = source.readLong();
			threshold = source.readLong();
			lowMemory = source.readInt() != 0;
			hiddenAppThreshold = source.readLong();
			secondaryServerThreshold = source.readLong();
			visibleAppThreshold = source.readLong();
			foregroundAppThreshold = source.readLong();
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

	/* AOSP ProcessList.getMemoryInfo(): availMem = MemFree + Cached and totalMem = MemTotal of /proc/meminfo
	 * (android_util_Process.cpp), the thresholds the low memory killer levels of ProcessList.updateOomLevels()
	 * (max scale, 64 bit: foreground 73728 KB, visible 92160 KB, cached/home/service 221184 KB, which for a
	 * device with less than 700 MB would need the display size too: this takes the memory scale only);
	 * advertisedMem is totalMem here (not looked up) */
	public void getMemoryInfo(MemoryInfo outInfo) {
		long free = 0, cached = 0, total = 0;
		try (java.io.BufferedReader r = new java.io.BufferedReader(new java.io.FileReader("/proc/meminfo"))) {
			String line;
			while ((line = r.readLine()) != null) {
				String[] f = line.split("\\s+");
				if (f.length < 2)
					continue;
				if (f[0].equals("MemFree:"))
					free = Long.parseLong(f[1]) * 1024;
				else if (f[0].equals("Cached:"))
					cached = Long.parseLong(f[1]) * 1024;
				else if (f[0].equals("MemTotal:"))
					total = Long.parseLong(f[1]) * 1024;
			}
		} catch (java.io.IOException | NumberFormatException e) {
			/* total 0, as the native code when the file can't be read */
		}
		final float scaleMem = ((float)(total / (1024 * 1024) - 350)) / (700 - 350);
		final float scale = scaleMem < 0 ? 0 : scaleMem > 1 ? 1 : scaleMem;
		final int[] low = {12288, 18432, 24576, 36864, 43008, 49152};
		final int[] high = {73728, 92160, 110592, 129024, 147456 * 3 / 2, 184320 * 7 / 4};
		final long[] level = new long[6]; // foreground, visible, perceptible, perceptible low, cached min, cached lmk first
		for (int i = 0; i < 6; i++)
			level[i] = (long)(int)(low[i] + ((high[i] - low[i]) * scale)) * 1024;
		final long homeAppMem = level[4]; // getMemLevel(HOME_APP_ADJ 600): the first level with adj >= 600 is 900
		final long cachedAppMem = level[4];
		outInfo.advertisedMem = total;
		outInfo.availMem = free + cached;
		outInfo.totalMem = total;
		outInfo.threshold = homeAppMem;
		outInfo.lowMemory = outInfo.availMem < (homeAppMem + ((cachedAppMem - homeAppMem) / 2));
		outInfo.hiddenAppThreshold = cachedAppMem;
		outInfo.secondaryServerThreshold = level[4];
		outInfo.visibleAppThreshold = level[1];
		outInfo.foregroundAppThreshold = level[0];
	}

	public ConfigurationInfo getDeviceConfigurationInfo() {
		return new ConfigurationInfo();
	}

	public int getMemoryClass() { return 20; }      // suggested heap size in MB
	public int getLargeMemoryClass() { return 60; } // value chosen arbitrarily

	/* AOSP ActivityManagerService.fillInProcMemInfoLocked(): the process's own ids and its importance; ATL
	 * does not track whether the app is visible, it counts as the top app (IMPORTANCE_FOREGROUND, state TOP) */
	public static void getMyMemoryState(RunningAppProcessInfo outInfo) {
		outInfo.pid = android.os.Process.myPid();
		outInfo.uid = android.os.Process.myUid();
		String pkg = ATLLoadedApp.getPrimaryApplication().pkg.packageName;
		outInfo.processName = pkg;
		outInfo.pkgList = new String[] {pkg};
		outInfo.lastTrimLevel = 0;
		outInfo.importance = RunningAppProcessInfo.IMPORTANCE_FOREGROUND;
		outInfo.importanceReasonCode = RunningAppProcessInfo.REASON_UNKNOWN;
		outInfo.processState = 2; // PROCESS_STATE_TOP
	}

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

		/* AOSP AppTask.finishAndRemoveTask()/setExcludeFromRecents(): calls into the task of the
		 * ActivityTaskManager. getAppTasks() here is always empty, so no AppTask exists to receive it. */
		public void finishAndRemoveTask() {}

		public void setExcludeFromRecents(boolean exclude) {}
	}
	public List<ActivityManager.AppTask> getAppTasks() {
		return new ArrayList<>();
	}

	/* AOSP ActivityManager.RecentTaskInfo/RunningTaskInfo (Android 16): extend TaskInfo, which holds
	 * baseIntent, baseActivity, topActivity, origActivity, numActivities and userId; parcelled like AOSP
	 * readFromParcel()/writeToParcel() (own fields, then TaskInfo.read/writeTaskToParcel()) */
	public static class RecentTaskInfo extends TaskInfo implements Parcelable {
		@Deprecated
		public int id;

		@Deprecated
		public int persistentId;

		@Deprecated
		public CharSequence description;

		@Deprecated
		public int affiliatedTaskId;

		public RecentTaskInfo() {
		}

		private RecentTaskInfo(Parcel source) {
			readFromParcel(source);
		}

		@Override
		public int describeContents() {
			return 0;
		}

		public void readFromParcel(Parcel source) {
			id = source.readInt();
			persistentId = source.readInt();
			super.readTaskFromParcel(source);
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			dest.writeInt(id);
			dest.writeInt(persistentId);
			super.writeTaskToParcel(dest, flags);
		}

		public static final Parcelable.Creator<RecentTaskInfo> CREATOR = new Parcelable.Creator<RecentTaskInfo>() {
			public RecentTaskInfo createFromParcel(Parcel source) {
				return new RecentTaskInfo(source);
			}

			public RecentTaskInfo[] newArray(int size) {
				return new RecentTaskInfo[size];
			}
		};
	}

	public static class RunningTaskInfo extends TaskInfo implements Parcelable {
		@Deprecated
		public int id;

		@Deprecated
		public android.graphics.Bitmap thumbnail;

		@Deprecated
		public CharSequence description;

		@Deprecated
		public int numRunning;

		public RunningTaskInfo() {
		}

		private RunningTaskInfo(Parcel source) {
			readFromParcel(source);
		}

		@Override
		public int describeContents() {
			return 0;
		}

		public void readFromParcel(Parcel source) {
			id = source.readInt();
			super.readTaskFromParcel(source);
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			dest.writeInt(id);
			super.writeTaskToParcel(dest, flags);
		}

		public static final Parcelable.Creator<RunningTaskInfo> CREATOR = new Parcelable.Creator<RunningTaskInfo>() {
			public RunningTaskInfo createFromParcel(Parcel source) {
				return new RunningTaskInfo(source);
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

	/* AOSP (API 35) returns the records of the app's last starts; ATL keeps no start records, so the list is
	 * always empty */
	public List<ApplicationStartInfo> getHistoricalProcessStartReasons(int maxNum) {
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

	/* AOSP hands the listener the ApplicationStartInfo of this process; ATL has no start records, so it is never called */
	public void addApplicationStartInfoCompletionListener(java.util.concurrent.Executor executor, java.util.function.Consumer<ApplicationStartInfo> listener) {
		if (executor == null || listener == null) {
			throw new NullPointerException("executor and listener must not be null");
		}
	}

	public void removeApplicationStartInfoCompletionListener(java.util.function.Consumer<ApplicationStartInfo> listener) {
		if (listener == null) {
			throw new NullPointerException("listener must not be null");
		}
	}

	/* AOSP ActivityManager.moveTaskToFront(int, int): moveTaskToFront(taskId, flags, null) */
	public void moveTaskToFront(int taskId, int flags) {
		moveTaskToFront(taskId, flags, null);
	}

	public void moveTaskToFront(int taskId, int flags, Bundle options) {
	}

	/* AOSP ActivityManager members that call ActivityManagerService/ActivityTaskManagerService: without them there is no
	 * lock task, no other process to kill (killBackgroundProcesses() only kills background processes of other apps),
	 * no control panel of a service, no heap watch, no app-not-responding report */
	public boolean isInLockTaskMode() { return getLockTaskModeState() != LOCK_TASK_MODE_NONE; }
	public void killBackgroundProcesses(String packageName) {}
	public void restartPackage(String packageName) { killBackgroundProcesses(packageName); }
	public PendingIntent getRunningServiceControlPanel(android.content.ComponentName service) { return null; }
	public void appNotResponding(String reason) {}
	public void clearWatchHeapLimit() {}
	public void setWatchHeapLimit(long pssSize) {}
	public static void setVrThread(int tid) {}
	public void dumpPackageState(java.io.FileDescriptor fd, String packageName) {}
}
