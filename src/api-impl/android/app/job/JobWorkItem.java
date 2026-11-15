package android.app.job;

import android.content.Intent;

final public class JobWorkItem implements android.os.Parcelable {
	private final Intent mIntent;
	int mDeliveryCount;
	int mWorkId;

	private final long mNetworkDownloadBytes;
	private final long mNetworkUploadBytes;
	private final long mMinimumChunkBytes;

	public JobWorkItem(Intent intent) {
		this(intent, JobInfo.NETWORK_BYTES_UNKNOWN, JobInfo.NETWORK_BYTES_UNKNOWN);
	}

	public JobWorkItem(Intent intent, long downloadBytes, long uploadBytes) {
		this(intent, downloadBytes, uploadBytes, JobInfo.NETWORK_BYTES_UNKNOWN);
	}

	/* AOSP JobWorkItem(Intent, long, long, long) and enforceValidity(): the checks that hold regardless of the
	 * app's target SDK (the one for negative estimates is behind a compat change) */
	public JobWorkItem(Intent intent, long downloadBytes, long uploadBytes, long minimumChunkBytes) {
		mIntent = intent;
		mNetworkDownloadBytes = downloadBytes;
		mNetworkUploadBytes = uploadBytes;
		mMinimumChunkBytes = minimumChunkBytes;
		long estimatedTransfer = uploadBytes == JobInfo.NETWORK_BYTES_UNKNOWN ? downloadBytes
		    : uploadBytes + (downloadBytes == JobInfo.NETWORK_BYTES_UNKNOWN ? 0 : downloadBytes);
		if (minimumChunkBytes != JobInfo.NETWORK_BYTES_UNKNOWN && estimatedTransfer != JobInfo.NETWORK_BYTES_UNKNOWN
		    && minimumChunkBytes > estimatedTransfer)
			throw new IllegalArgumentException("Minimum chunk size can't be greater than estimated network usage");
		if (minimumChunkBytes != JobInfo.NETWORK_BYTES_UNKNOWN && minimumChunkBytes <= 0)
			throw new IllegalArgumentException("Minimum chunk size must be positive");
	}

	public long getEstimatedNetworkDownloadBytes() {
		return mNetworkDownloadBytes;
	}

	public long getEstimatedNetworkUploadBytes() {
		return mNetworkUploadBytes;
	}

	public long getMinimumNetworkChunkBytes() {
		return mMinimumChunkBytes;
	}

	public Intent getIntent() {
		return mIntent;
	}

	/* AOSP JobWorkItem.getDeliveryCount(): how often the item was handed to dequeueWork() */
	public int getDeliveryCount() {
		return mDeliveryCount;
	}

	/* AOSP JobWorkItem(Parcel)/writeToParcel()/describeContents()/CREATOR (apex/jobscheduler, android-16.0.0_r4).
	 * ATL keeps no extras (AOSP's are set through a hidden builder): written as an empty bundle, skipped
	 * when read */
	JobWorkItem(android.os.Parcel in) {
		if (in.readInt() != 0) {
			mIntent = Intent.CREATOR.createFromParcel(in);
		} else {
			mIntent = null;
		}
		in.readPersistableBundle(); // extras
		mNetworkDownloadBytes = in.readLong();
		mNetworkUploadBytes = in.readLong();
		mMinimumChunkBytes = in.readLong();
		mDeliveryCount = in.readInt();
		mWorkId = in.readInt();
	}

	public int describeContents() {
		return 0;
	}

	public void writeToParcel(android.os.Parcel out, int flags) {
		if (mIntent != null) {
			out.writeInt(1);
			mIntent.writeToParcel(out, 0);
		} else {
			out.writeInt(0);
		}
		out.writePersistableBundle(android.os.PersistableBundle.EMPTY);
		out.writeLong(mNetworkDownloadBytes);
		out.writeLong(mNetworkUploadBytes);
		out.writeLong(mMinimumChunkBytes);
		out.writeInt(mDeliveryCount);
		out.writeInt(mWorkId);
	}

	public static final android.os.Parcelable.Creator<JobWorkItem> CREATOR = new android.os.Parcelable.Creator<JobWorkItem>() {
		public JobWorkItem createFromParcel(android.os.Parcel in) {
			return new JobWorkItem(in);
		}

		public JobWorkItem[] newArray(int size) {
			return new JobWorkItem[size];
		}
	};
}
