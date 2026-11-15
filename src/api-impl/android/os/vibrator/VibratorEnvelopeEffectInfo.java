package android.os.vibrator;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

/* AOSP android.os.vibrator.VibratorEnvelopeEffectInfo: the limits of a vibrator for envelope effects */
public final class VibratorEnvelopeEffectInfo implements Parcelable {
	private final int mMaxSize;
	private final long mMinControlPointDurationMillis;
	private final long mMaxControlPointDurationMillis;

	VibratorEnvelopeEffectInfo(Parcel in) {
		mMaxSize = in.readInt();
		mMinControlPointDurationMillis = in.readLong();
		mMaxControlPointDurationMillis = in.readLong();
	}

	public VibratorEnvelopeEffectInfo(int maxSize, long minControlPointDurationMillis, long maxControlPointDurationMillis) {
		mMaxSize = maxSize;
		mMinControlPointDurationMillis = minControlPointDurationMillis;
		mMaxControlPointDurationMillis = maxControlPointDurationMillis;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(mMaxSize);
		dest.writeLong(mMinControlPointDurationMillis);
		dest.writeLong(mMaxControlPointDurationMillis);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof VibratorEnvelopeEffectInfo)) {
			return false;
		}
		VibratorEnvelopeEffectInfo other = (VibratorEnvelopeEffectInfo)o;
		return mMaxSize == other.mMaxSize && mMinControlPointDurationMillis == other.mMinControlPointDurationMillis
		    && mMaxControlPointDurationMillis == other.mMaxControlPointDurationMillis;
	}

	@Override
	public int hashCode() {
		return Objects.hash(mMaxSize, mMinControlPointDurationMillis, mMaxControlPointDurationMillis);
	}

	@Override
	public String toString() {
		return "VibratorEnvelopeEffectInfo{"
		    + ", mMaxSize=" + mMaxSize
		    + ", mMinControlPointDurationMillis=" + mMinControlPointDurationMillis
		    + ", mMaxControlPointDurationMillis=" + mMaxControlPointDurationMillis + '}';
	}

	public static final Creator<VibratorEnvelopeEffectInfo> CREATOR = new Creator<VibratorEnvelopeEffectInfo>() {
		@Override
		public VibratorEnvelopeEffectInfo createFromParcel(Parcel in) {
			return new VibratorEnvelopeEffectInfo(in);
		}

		@Override
		public VibratorEnvelopeEffectInfo[] newArray(int size) {
			return new VibratorEnvelopeEffectInfo[size];
		}
	};

	public long getMaxDurationMillis() {
		return mMaxSize * mMaxControlPointDurationMillis;
	}

	public int getMaxSize() {
		return mMaxSize;
	}

	public long getMinControlPointDurationMillis() {
		return mMinControlPointDurationMillis;
	}

	public long getMaxControlPointDurationMillis() {
		return mMaxControlPointDurationMillis;
	}
}
