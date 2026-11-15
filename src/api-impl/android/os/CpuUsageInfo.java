package android.os;

/* AOSP CpuUsageInfo (frameworks/base/core/java/android/os) */
public final class CpuUsageInfo implements Parcelable {
	private long mActive;
	private long mTotal;

	public static final Parcelable.Creator<CpuUsageInfo> CREATOR = new Parcelable.Creator<CpuUsageInfo>() {
		public CpuUsageInfo createFromParcel(Parcel in) {
			return new CpuUsageInfo(in);
		}

		public CpuUsageInfo[] newArray(int size) {
			return new CpuUsageInfo[size];
		}
	};

	/* AOSP @hide constructor */
	public CpuUsageInfo(long activeTime, long totalTime) {
		mActive = activeTime;
		mTotal = totalTime;
	}

	private CpuUsageInfo(Parcel in) {
		mActive = in.readLong();
		mTotal = in.readLong();
	}

	public long getActive() {
		return mActive;
	}

	public long getTotal() {
		return mTotal;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeLong(mActive);
		out.writeLong(mTotal);
	}
}
