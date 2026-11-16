package android.app.job;

import android.content.ClipData;
import android.content.ComponentName;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.Uri;
import android.os.Bundle;
import android.os.PersistableBundle;

public class JobInfo implements android.os.Parcelable {
	public static final int NETWORK_TYPE_NONE = 0;
	public static final int NETWORK_TYPE_ANY = 1;

	private ComponentName service;
	long initialBackoffMillis = DEFAULT_INITIAL_BACKOFF_MILLIS;
	int backoffPolicy;
	private PersistableBundle extras;
	long periodicMillis;
	private int id;
	boolean running;
	JobParameters workParams; // the running job of enqueue(), which takes further work
	long minLatencyMillis;
	int networkType = NETWORK_TYPE_NONE;
	private String traceTag;

	/* AOSP JobInfo fields: what the Builder was given. ATL's JobScheduler runs a job once after minLatencyMillis
	 * (periodic: again after periodicMillis) and evaluates none of the other conditions */
	public static final int NETWORK_TYPE_UNMETERED = 2;
	public static final int NETWORK_TYPE_NOT_ROAMING = 3;
	public static final int NETWORK_TYPE_CELLULAR = 4;
	public static final int NETWORK_TYPE_METERED = NETWORK_TYPE_CELLULAR;
	public static final long DEFAULT_INITIAL_BACKOFF_MILLIS = 30000L;
	public static final int BACKOFF_POLICY_LINEAR = 0;
	public static final int BACKOFF_POLICY_EXPONENTIAL = 1;
	public static final int PRIORITY_MIN = 100;
	public static final int PRIORITY_LOW = 200;
	public static final int PRIORITY_DEFAULT = 300;
	public static final int PRIORITY_HIGH = 400;
	public static final int PRIORITY_MAX = 500;
	public static final long NETWORK_BYTES_UNKNOWN = -1L;
	private static final int FLAG_PREFETCH = 1 << 2;
	private static final int FLAG_EXPEDITED = 1 << 4;
	private static final int FLAG_USER_INITIATED = 1 << 5;
	private static final int FLAG_IMPORTANT_WHILE_FOREGROUND = 1 << 1;
	private boolean isPersisted;
	private boolean requireCharging;
	private boolean requireDeviceIdle;
	private boolean requireBatteryNotLow;
	private boolean requireStorageNotLow;
	private TriggerContentUri[] triggerContentUris;
	private long triggerContentUpdateDelay = -1L;
	private long triggerContentMaxDelay = -1L;
	private long maxExecutionDelayMillis;
	private long flexMillis;
	private boolean hasEarlyConstraint;
	private boolean hasLateConstraint;
	private Bundle transientExtras = Bundle.EMPTY;
	private ClipData clipData;
	private int clipGrantFlags;
	private NetworkRequest networkRequest;
	private long networkDownloadBytes = NETWORK_BYTES_UNKNOWN;
	private long networkUploadBytes = NETWORK_BYTES_UNKNOWN;
	private long minimumNetworkChunkBytes = NETWORK_BYTES_UNKNOWN;
	private int flags;
	private int priority = PRIORITY_DEFAULT;

	public JobInfo() {}

	/* AOSP JobInfo(Parcel)/writeToParcel()/describeContents()/CREATOR (frameworks/base apex/jobscheduler
	 * framework/java/android/app/job/JobInfo.java, android-16.0.0_r4). ATL keeps the constraints as
	 * booleans (AOSP constraintFlags: CHARGING 1, BATTERY_NOT_LOW 2, DEVICE_IDLE 4, STORAGE_NOT_LOW 8),
	 * the interval as periodicMillis (AOSP isPeriodic is intervalMillis != 0 here) and the network type
	 * apart from the request (derived from the request when read, as AOSP getNetworkType()). ATL has no
	 * bias (written as BIAS_DEFAULT 0) and no debug tags (written as none). */
	private JobInfo(android.os.Parcel in) {
		id = in.readInt();
		final PersistableBundle persistableExtras = in.readPersistableBundle();
		extras = persistableExtras != null ? persistableExtras : PersistableBundle.EMPTY;
		transientExtras = in.readBundle();
		if (in.readInt() != 0) {
			clipData = ClipData.CREATOR.createFromParcel(in);
			clipGrantFlags = in.readInt();
		} else {
			clipData = null;
			clipGrantFlags = 0;
		}
		service = (ComponentName)in.readParcelable(null);
		final int constraintFlags = in.readInt();
		requireCharging = (constraintFlags & 1) != 0;
		requireBatteryNotLow = (constraintFlags & 2) != 0;
		requireDeviceIdle = (constraintFlags & 4) != 0;
		requireStorageNotLow = (constraintFlags & 8) != 0;
		triggerContentUris = in.createTypedArray(TriggerContentUri.CREATOR);
		triggerContentUpdateDelay = in.readLong();
		triggerContentMaxDelay = in.readLong();
		if (in.readInt() != 0) {
			networkRequest = NetworkRequest.CREATOR.createFromParcel(in);
		} else {
			networkRequest = null;
		}
		if (networkRequest == null) {
			networkType = NETWORK_TYPE_NONE;
		} else if (networkRequest.networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)) {
			networkType = NETWORK_TYPE_UNMETERED;
		} else if (networkRequest.networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING)) {
			networkType = NETWORK_TYPE_NOT_ROAMING;
		} else if (networkRequest.networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
			networkType = NETWORK_TYPE_CELLULAR;
		} else {
			networkType = NETWORK_TYPE_ANY;
		}
		networkDownloadBytes = in.readLong();
		networkUploadBytes = in.readLong();
		minimumNetworkChunkBytes = in.readLong();
		minLatencyMillis = in.readLong();
		maxExecutionDelayMillis = in.readLong();
		in.readInt(); // isPeriodic: periodicMillis != 0
		isPersisted = in.readInt() == 1;
		periodicMillis = in.readLong();
		flexMillis = in.readLong();
		initialBackoffMillis = in.readLong();
		backoffPolicy = in.readInt();
		hasEarlyConstraint = in.readInt() == 1;
		hasLateConstraint = in.readInt() == 1;
		in.readInt(); // bias
		priority = in.readInt();
		flags = in.readInt();
		final int numDebugTags = in.readInt();
		for (int i = 0; i < numDebugTags; ++i) {
			if (in.readString() == null) {
				throw new IllegalStateException("malformed parcel");
			}
		}
		final String traceTag = in.readString();
		this.traceTag = traceTag == null ? null : traceTag.intern();
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(android.os.Parcel out, int flags) {
		out.writeInt(id);
		out.writePersistableBundle(extras);
		out.writeBundle(transientExtras);
		if (clipData != null) {
			out.writeInt(1);
			clipData.writeToParcel(out, flags);
			out.writeInt(clipGrantFlags);
		} else {
			out.writeInt(0);
		}
		out.writeParcelable(service, flags);
		out.writeInt((requireCharging ? 1 : 0) | (requireBatteryNotLow ? 2 : 0)
			     | (requireDeviceIdle ? 4 : 0) | (requireStorageNotLow ? 8 : 0));
		out.writeTypedArray(triggerContentUris, flags);
		out.writeLong(triggerContentUpdateDelay);
		out.writeLong(triggerContentMaxDelay);
		if (networkRequest != null) {
			out.writeInt(1);
			networkRequest.writeToParcel(out, flags);
		} else {
			out.writeInt(0);
		}
		out.writeLong(networkDownloadBytes);
		out.writeLong(networkUploadBytes);
		out.writeLong(minimumNetworkChunkBytes);
		out.writeLong(minLatencyMillis);
		out.writeLong(maxExecutionDelayMillis);
		out.writeInt(isPeriodic() ? 1 : 0);
		out.writeInt(isPersisted ? 1 : 0);
		out.writeLong(periodicMillis);
		out.writeLong(flexMillis);
		out.writeLong(initialBackoffMillis);
		out.writeInt(backoffPolicy);
		out.writeInt(hasEarlyConstraint ? 1 : 0);
		out.writeInt(hasLateConstraint ? 1 : 0);
		out.writeInt(0); // mBias: BIAS_DEFAULT
		out.writeInt(priority);
		out.writeInt(this.flags);
		out.writeInt(0); // no debug tags
		out.writeString(traceTag);
	}

	public static final android.os.Parcelable.Creator<JobInfo> CREATOR = new android.os.Parcelable.Creator<JobInfo>() {
		@Override
		public JobInfo createFromParcel(android.os.Parcel in) {
			return new JobInfo(in);
		}

		@Override
		public JobInfo[] newArray(int size) {
			return new JobInfo[size];
		}
	};

	public long getMinLatencyMillis() {
		return minLatencyMillis;
	}

	public long getMaxExecutionDelayMillis() {
		return maxExecutionDelayMillis;
	}

	public boolean isPeriodic() {
		return periodicMillis != 0;
	}

	public long getIntervalMillis() {
		return periodicMillis;
	}

	public long getFlexMillis() {
		return flexMillis;
	}

	public boolean hasEarlyConstraint() {
		return hasEarlyConstraint;
	}

	public boolean hasLateConstraint() {
		return hasLateConstraint;
	}

	public long getInitialBackoffMillis() {
		return initialBackoffMillis;
	}

	public int getBackoffPolicy() {
		return backoffPolicy;
	}

	public boolean isPersisted() {
		return isPersisted;
	}

	public boolean isRequireCharging() {
		return requireCharging;
	}

	public boolean isRequireDeviceIdle() {
		return requireDeviceIdle;
	}

	public boolean isRequireBatteryNotLow() {
		return requireBatteryNotLow;
	}

	public boolean isRequireStorageNotLow() {
		return requireStorageNotLow;
	}

	public TriggerContentUri[] getTriggerContentUris() {
		return triggerContentUris;
	}

	public long getTriggerContentUpdateDelay() {
		return triggerContentUpdateDelay;
	}

	public long getTriggerContentMaxDelay() {
		return triggerContentMaxDelay;
	}

	public Bundle getTransientExtras() {
		return transientExtras.isEmpty() ? new Bundle() : new Bundle(transientExtras);
	}

	public ClipData getClipData() {
		return clipData;
	}

	public int getClipGrantFlags() {
		return clipGrantFlags;
	}

	public NetworkRequest getRequiredNetwork() {
		return networkRequest;
	}

	public long getEstimatedNetworkDownloadBytes() {
		return networkDownloadBytes;
	}

	public long getEstimatedNetworkUploadBytes() {
		return networkUploadBytes;
	}

	public long getMinimumNetworkChunkBytes() {
		return minimumNetworkChunkBytes;
	}

	public boolean isImportantWhileForeground() {
		return (flags & FLAG_IMPORTANT_WHILE_FOREGROUND) != 0;
	}

	public boolean isPrefetch() {
		return (flags & FLAG_PREFETCH) != 0;
	}

	public boolean isExpedited() {
		return (flags & FLAG_EXPEDITED) != 0;
	}

	public boolean isUserInitiated() {
		return (flags & FLAG_USER_INITIATED) != 0;
	}

	public int getPriority() {
		return priority;
	}

	public static final long getMinFlexMillis() {
		return 5 * 60 * 1000L;
	}

	public static final long getMinBackoffMillis() {
		return 10 * 1000L;
	}

	public ComponentName getService() {
		return service;
	}

	public PersistableBundle getExtras() {
		return extras;
	}

	public int getNetworkType() {
		return networkType;
	}

	public int getId() {
		return id;
	}

	public String getTraceTag() {
		return traceTag;
	}

	public static long getMinPeriodMillis() {
		return 1000;
	}

	public String toString() {
		return "JobInfo{"
		     + "jobService=" + service
		     + ", initialBackoffMillis=" + initialBackoffMillis
		     + ", backoffPolicy=" + backoffPolicy
		     + ", extras=" + extras
		     + ", periodicMillis=" + periodicMillis
		     + ", id=" + id
		     + '}';
	}

	public static final class Builder {

		private JobInfo jobInfo;
		private boolean backoffPolicySet;

		public Builder(int jobId, ComponentName jobService) {
			jobInfo = new JobInfo();
			jobInfo.id = jobId;
			jobInfo.service = jobService;
			jobInfo.extras = PersistableBundle.EMPTY; // AOSP Builder.mExtras
		}

		/* AOSP: a backoff below getMinBackoffMillis() is raised to it */
		public Builder setBackoffCriteria(long initialBackoffMillis, int backoffPolicy) {
			if (initialBackoffMillis < getMinBackoffMillis())
				initialBackoffMillis = getMinBackoffMillis();
			backoffPolicySet = true;
			jobInfo.initialBackoffMillis = initialBackoffMillis;
			jobInfo.backoffPolicy = backoffPolicy;
			return this;
		}

		public Builder setExtras(PersistableBundle extras) {
			jobInfo.extras = extras;
			return this;
		}

		public Builder setMinimumLatency(long minLatencyMillis) {
			jobInfo.minLatencyMillis = minLatencyMillis;
			jobInfo.hasEarlyConstraint = true;
			return this;
		}

		public Builder setOverrideDeadline(long maxExecutionDelayMillis) {
			jobInfo.maxExecutionDelayMillis = maxExecutionDelayMillis;
			jobInfo.hasLateConstraint = true;
			return this;
		}

		public Builder setPeriodic(long intervalMillis) {
			return setPeriodic(intervalMillis, intervalMillis);
		}

		/* AOSP: the flex is raised to getMinFlexMillis() or 5 percent of the interval, whichever is higher
		 * (the interval to getMinPeriodMillis(), which ATL keeps at its own value) */
		public Builder setPeriodic(long intervalMillis, long flexMillis) {
			if (intervalMillis < getMinPeriodMillis())
				intervalMillis = getMinPeriodMillis();
			long minFlex = Math.max(5 * intervalMillis / 100, getMinFlexMillis());
			if (flexMillis < minFlex)
				flexMillis = minFlex;
			jobInfo.periodicMillis = intervalMillis;
			jobInfo.flexMillis = flexMillis;
			jobInfo.hasEarlyConstraint = jobInfo.hasLateConstraint = true;
			return this;
		}

		public Builder setPersisted(boolean persisted) {
			jobInfo.isPersisted = persisted;
			return this;
		}

		public Builder setRequiredNetworkType(int networkType) {
			jobInfo.networkType = networkType;
			if (networkType == NETWORK_TYPE_NONE) {
				jobInfo.networkRequest = null;
				return this;
			}
			NetworkRequest.Builder builder = new NetworkRequest.Builder();
			builder.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
			builder.addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
			builder.removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN);
			builder.removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED);
			if (networkType == NETWORK_TYPE_UNMETERED)
				builder.addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED);
			else if (networkType == NETWORK_TYPE_NOT_ROAMING)
				builder.addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING);
			else if (networkType == NETWORK_TYPE_CELLULAR)
				builder.addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR);
			jobInfo.networkRequest = builder.build();
			return this;
		}

		public Builder setRequiresCharging(boolean requiresCharging) {
			jobInfo.requireCharging = requiresCharging;
			return this;
		}

		public Builder setRequiresDeviceIdle(boolean requiresDeviceIdle) {
			jobInfo.requireDeviceIdle = requiresDeviceIdle;
			return this;
		}

		public Builder setRequiresBatteryNotLow(boolean batteryNotLow) {
			jobInfo.requireBatteryNotLow = batteryNotLow;
			return this;
		}

		public Builder setRequiresStorageNotLow(boolean storageNotLow) {
			jobInfo.requireStorageNotLow = storageNotLow;
			return this;
		}

		public Builder addTriggerContentUri(TriggerContentUri uri) {
			java.util.ArrayList<TriggerContentUri> list = new java.util.ArrayList<>();
			if (jobInfo.triggerContentUris != null)
				list.addAll(java.util.Arrays.asList(jobInfo.triggerContentUris));
			list.add(uri);
			jobInfo.triggerContentUris = list.toArray(new TriggerContentUri[0]);
			return this;
		}

		public Builder setTriggerContentUpdateDelay(long durationMs) {
			jobInfo.triggerContentUpdateDelay = durationMs;
			return this;
		}

		public Builder setTriggerContentMaxDelay(long durationMs) {
			jobInfo.triggerContentMaxDelay = durationMs;
			return this;
		}

		public Builder setTransientExtras(Bundle extras) {
			jobInfo.transientExtras = extras.isEmpty() ? Bundle.EMPTY : new Bundle(extras);
			return this;
		}

		public Builder setClipData(ClipData clip, int grantFlags) {
			jobInfo.clipData = clip;
			jobInfo.clipGrantFlags = grantFlags;
			return this;
		}

		public Builder setRequiredNetwork(NetworkRequest networkRequest) {
			jobInfo.networkRequest = networkRequest;
			jobInfo.networkType = networkRequest != null ? NETWORK_TYPE_ANY : NETWORK_TYPE_NONE;
			return this;
		}

		public Builder setEstimatedNetworkBytes(long downloadBytes, long uploadBytes) {
			jobInfo.networkDownloadBytes = downloadBytes;
			jobInfo.networkUploadBytes = uploadBytes;
			return this;
		}

		public Builder setMinimumNetworkChunkBytes(long chunkSizeBytes) {
			if (chunkSizeBytes != NETWORK_BYTES_UNKNOWN && chunkSizeBytes <= 0)
				throw new IllegalArgumentException("Minimum chunk size must be positive");
			jobInfo.minimumNetworkChunkBytes = chunkSizeBytes;
			return this;
		}

		private Builder setFlag(int flag, boolean on) {
			jobInfo.flags = on ? jobInfo.flags | flag : jobInfo.flags & ~flag;
			return this;
		}

		public Builder setImportantWhileForeground(boolean importantWhileForeground) {
			return setFlag(FLAG_IMPORTANT_WHILE_FOREGROUND, importantWhileForeground);
		}

		public Builder setPrefetch(boolean prefetch) {
			return setFlag(FLAG_PREFETCH, prefetch);
		}

		public Builder setExpedited(boolean expedited) {
			return setFlag(FLAG_EXPEDITED, expedited);
		}

		public Builder setUserInitiated(boolean userInitiated) {
			return setFlag(FLAG_USER_INITIATED, userInitiated);
		}

		public Builder setPriority(int priority) {
			jobInfo.priority = priority;
			return this;
		}

		/* the name of the job in system traces (API 35); kept for getTraceTag() */
		public Builder setTraceTag(String traceTag) {
			jobInfo.traceTag = traceTag;
			return this;
		}

		/* AOSP Builder.build()/JobInfo.enforceValidity(): the checks that hold regardless of the app's target
		 * SDK (those behind compat changes, and the time window one, are left out) */
		public JobInfo build() {
			JobInfo j = jobInfo;
			if (backoffPolicySet && j.requireDeviceIdle)
				throw new IllegalArgumentException("An idle mode job will not respect any back-off policy, so calling setBackoffCriteria with setRequiresDeviceIdle is an error.");
			if ((j.networkDownloadBytes > 0 || j.networkUploadBytes > 0 || j.minimumNetworkChunkBytes > 0) && j.networkRequest == null)
				throw new IllegalArgumentException("Can't provide estimated network usage without requiring a network");
			long estimatedTransfer = j.networkUploadBytes == NETWORK_BYTES_UNKNOWN ? j.networkDownloadBytes
			    : j.networkUploadBytes + (j.networkDownloadBytes == NETWORK_BYTES_UNKNOWN ? 0 : j.networkDownloadBytes);
			if (j.minimumNetworkChunkBytes != NETWORK_BYTES_UNKNOWN && estimatedTransfer != NETWORK_BYTES_UNKNOWN
			    && j.minimumNetworkChunkBytes > estimatedTransfer)
				throw new IllegalArgumentException("Minimum chunk size can't be greater than estimated network usage");
			boolean hasDeadline = j.maxExecutionDelayMillis != 0L;
			if (j.isPeriodic()) {
				if (hasDeadline)
					throw new IllegalArgumentException("Can't call setOverrideDeadline() on a periodic job.");
				if (j.minLatencyMillis != 0L)
					throw new IllegalArgumentException("Can't call setMinimumLatency() on a periodic job");
				if (j.triggerContentUris != null)
					throw new IllegalArgumentException("Can't call addTriggerContentUri() on a periodic job");
			}
			if (j.isPersisted) {
				if (j.triggerContentUris != null)
					throw new IllegalArgumentException("Can't call addTriggerContentUri() on a persisted job");
				if (!j.transientExtras.isEmpty())
					throw new IllegalArgumentException("Can't call setTransientExtras() on a persisted job");
				if (j.clipData != null)
					throw new IllegalArgumentException("Can't call setClipData() on a persisted job");
			}
			if (j.isImportantWhileForeground()) {
				if (j.hasEarlyConstraint)
					throw new IllegalArgumentException("An important while foreground job cannot have a time delay");
				if (j.priority != PRIORITY_HIGH && j.priority != PRIORITY_DEFAULT)
					throw new IllegalArgumentException("An important while foreground job must be high or default priority. Don't mark unimportant tasks as important while foreground.");
			}
			switch (j.priority) {
			case PRIORITY_MAX:
				if (!(j.isExpedited() || j.isUserInitiated()))
					throw new IllegalArgumentException("Only expedited or user-initiated jobs can have max priority");
				break;
			case PRIORITY_HIGH:
				if (j.isPrefetch())
					throw new IllegalArgumentException("Prefetch jobs cannot be high priority");
				if (j.isPeriodic())
					throw new IllegalArgumentException("Periodic jobs cannot be high priority");
				break;
			case PRIORITY_DEFAULT:
			case PRIORITY_LOW:
			case PRIORITY_MIN:
				break;
			default:
				throw new IllegalArgumentException("Invalid priority level provided: " + j.priority);
			}
			j.extras = j.extras.deepCopy(); // AOSP JobInfo(Builder)
			return j;
		}
	}

	/* AOSP JobInfo.TriggerContentUri */
	public static final class TriggerContentUri implements android.os.Parcelable {
		public static final int FLAG_NOTIFY_FOR_DESCENDANTS = 1 << 0;
		private final Uri mUri;
		private final int mFlags;

		public TriggerContentUri(Uri uri, int flags) {
			mUri = java.util.Objects.requireNonNull(uri);
			mFlags = flags;
		}

		public Uri getUri() {
			return mUri;
		}

		public int getFlags() {
			return mFlags;
		}

		/* AOSP JobInfo.TriggerContentUri(Parcel)/describeContents()/writeToParcel()/CREATOR */
		private TriggerContentUri(android.os.Parcel in) {
			mUri = Uri.CREATOR.createFromParcel(in);
			mFlags = in.readInt();
		}

		@Override
		public int describeContents() {
			return 0;
		}

		@Override
		public void writeToParcel(android.os.Parcel out, int flags) {
			mUri.writeToParcel(out, flags);
			out.writeInt(mFlags);
		}

		public static final android.os.Parcelable.Creator<TriggerContentUri> CREATOR = new android.os.Parcelable.Creator<TriggerContentUri>() {
			@Override
			public TriggerContentUri createFromParcel(android.os.Parcel in) {
				return new TriggerContentUri(in);
			}

			@Override
			public TriggerContentUri[] newArray(int size) {
				return new TriggerContentUri[size];
			}
		};

		@Override
		public boolean equals(Object o) {
			if (!(o instanceof TriggerContentUri))
				return false;
			TriggerContentUri t = (TriggerContentUri)o;
			return java.util.Objects.equals(t.mUri, mUri) && t.mFlags == mFlags;
		}

		@Override
		public int hashCode() {
			return (mUri == null ? 0 : mUri.hashCode()) ^ mFlags;
		}
	}
}
