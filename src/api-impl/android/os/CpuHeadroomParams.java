package android.os;

import java.util.Arrays;

/* AOSP CpuHeadroomParams (frameworks/base/core/java/android/os), with the fields of CpuHeadroomParamsInternal (AIDL
 * defaults: window 1000 ms, calculation type MIN, no tids) held directly */
public final class CpuHeadroomParams {
	public static final int CPU_HEADROOM_CALCULATION_TYPE_MIN = 0;
	public static final int CPU_HEADROOM_CALCULATION_TYPE_AVERAGE = 1;

	private int mCalculationType = CPU_HEADROOM_CALCULATION_TYPE_MIN;
	private int mCalculationWindowMillis = 1000;
	private int[] mTids;

	private CpuHeadroomParams() {}

	public static final class Builder {
		private int mCalculationType = -1;
		private int mCalculationWindowMillis = -1;
		private int[] mTids = null;

		public Builder() {}

		public Builder(CpuHeadroomParams params) {
			if (params.mCalculationType >= 0) {
				mCalculationType = params.mCalculationType;
			}
			if (params.mCalculationWindowMillis >= 0) {
				mCalculationWindowMillis = params.mCalculationWindowMillis;
			}
			if (params.mTids != null) {
				mTids = Arrays.copyOf(params.mTids, params.mTids.length);
			}
		}

		public Builder setCalculationType(int calculationType) {
			switch (calculationType) {
				case CPU_HEADROOM_CALCULATION_TYPE_MIN:
				case CPU_HEADROOM_CALCULATION_TYPE_AVERAGE: {
					mCalculationType = calculationType;
					return this;
				}
			}
			throw new IllegalArgumentException("Invalid calculation type: " + calculationType);
		}

		public Builder setCalculationWindowMillis(int windowMillis) {
			if (windowMillis <= 0) {
				throw new IllegalArgumentException("Invalid calculation window: " + windowMillis);
			}
			mCalculationWindowMillis = windowMillis;
			return this;
		}

		public Builder setTids(int... tids) {
			for (int tid : tids) {
				if (tid <= 0) {
					throw new IllegalArgumentException("Invalid TID: " + tid);
				}
			}
			mTids = tids;
			return this;
		}

		public CpuHeadroomParams build() {
			CpuHeadroomParams params = new CpuHeadroomParams();
			if (mCalculationType >= 0) {
				params.mCalculationType = mCalculationType;
			}
			if (mCalculationWindowMillis >= 0) {
				params.mCalculationWindowMillis = mCalculationWindowMillis;
			}
			if (mTids != null) {
				params.mTids = mTids;
			}
			return params;
		}
	}

	public Builder toBuilder() {
		return new Builder(this);
	}

	public int getCalculationType() {
		switch (mCalculationType) {
			case CPU_HEADROOM_CALCULATION_TYPE_MIN:
			case CPU_HEADROOM_CALCULATION_TYPE_AVERAGE:
				return mCalculationType;
			default:
				return CPU_HEADROOM_CALCULATION_TYPE_MIN;
		}
	}

	public long getCalculationWindowMillis() {
		return mCalculationWindowMillis;
	}

	public int[] getTids() {
		return mTids == null ? null : Arrays.copyOf(mTids, mTids.length);
	}

	@Override
	public String toString() {
		return "CpuHeadroomParams{calculationType=" + mCalculationType + ", calculationWindowMillis=" +
		       mCalculationWindowMillis + ", tids=" + Arrays.toString(mTids) + '}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		CpuHeadroomParams that = (CpuHeadroomParams)o;
		return mCalculationType == that.mCalculationType && mCalculationWindowMillis == that.mCalculationWindowMillis &&
		       Arrays.equals(mTids, that.mTids);
	}

	@Override
	public int hashCode() {
		return 31 * (31 * mCalculationType + mCalculationWindowMillis) + Arrays.hashCode(mTids);
	}
}
