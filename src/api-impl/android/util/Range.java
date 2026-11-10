package android.util;

import java.util.Objects;

/* AOSP android.util.Range, adapted: null checks via java.util.Objects, no framework-only annotations. */
public final class Range<T extends Comparable<? super T>> {

	private final T mLower;
	private final T mUpper;

	public Range(final T lower, final T upper) {
		mLower = Objects.requireNonNull(lower, "lower must not be null");
		mUpper = Objects.requireNonNull(upper, "upper must not be null");

		if (lower.compareTo(upper) > 0) {
			throw new IllegalArgumentException("lower must be less than or equal to upper");
		}
	}

	public static <T extends Comparable<? super T>> Range<T> create(final T lower, final T upper) {
		return new Range<T>(lower, upper);
	}

	public T getLower() {
		return mLower;
	}

	public T getUpper() {
		return mUpper;
	}

	public boolean contains(T value) {
		Objects.requireNonNull(value, "value must not be null");
		return value.compareTo(mLower) >= 0 && value.compareTo(mUpper) <= 0;
	}

	public boolean contains(Range<T> range) {
		Objects.requireNonNull(range, "value must not be null");
		return range.mLower.compareTo(mLower) >= 0 && range.mUpper.compareTo(mUpper) <= 0;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		} else if (this == obj) {
			return true;
		} else if (obj instanceof Range) {
			@SuppressWarnings("rawtypes")
			Range other = (Range) obj;
			return mLower.equals(other.mLower) && mUpper.equals(other.mUpper);
		}
		return false;
	}

	public T clamp(T value) {
		Objects.requireNonNull(value, "value must not be null");
		if (value.compareTo(mLower) < 0) {
			return mLower;
		} else if (value.compareTo(mUpper) > 0) {
			return mUpper;
		} else {
			return value;
		}
	}

	public Range<T> intersect(Range<T> range) {
		Objects.requireNonNull(range, "range must not be null");
		int cmpLower = range.mLower.compareTo(mLower);
		int cmpUpper = range.mUpper.compareTo(mUpper);

		if (cmpLower <= 0 && cmpUpper >= 0) {
			return this;
		} else if (cmpLower >= 0 && cmpUpper <= 0) {
			return range;
		} else {
			return Range.create(
					cmpLower <= 0 ? mLower : range.mLower,
					cmpUpper >= 0 ? mUpper : range.mUpper);
		}
	}

	public Range<T> intersect(T lower, T upper) {
		Objects.requireNonNull(lower, "lower must not be null");
		Objects.requireNonNull(upper, "upper must not be null");
		int cmpLower = lower.compareTo(mLower);
		int cmpUpper = upper.compareTo(mUpper);

		if (cmpLower <= 0 && cmpUpper >= 0) {
			return this;
		} else {
			return Range.create(
					cmpLower <= 0 ? mLower : lower,
					cmpUpper >= 0 ? mUpper : upper);
		}
	}

	public Range<T> extend(Range<T> range) {
		Objects.requireNonNull(range, "range must not be null");
		int cmpLower = range.mLower.compareTo(mLower);
		int cmpUpper = range.mUpper.compareTo(mUpper);

		if (cmpLower <= 0 && cmpUpper >= 0) {
			return range;
		} else if (cmpLower >= 0 && cmpUpper <= 0) {
			return this;
		} else {
			return Range.create(
					cmpLower >= 0 ? mLower : range.mLower,
					cmpUpper <= 0 ? mUpper : range.mUpper);
		}
	}

	public Range<T> extend(T lower, T upper) {
		Objects.requireNonNull(lower, "lower must not be null");
		Objects.requireNonNull(upper, "upper must not be null");
		int cmpLower = lower.compareTo(mLower);
		int cmpUpper = upper.compareTo(mUpper);

		if (cmpLower >= 0 && cmpUpper <= 0) {
			return this;
		} else {
			return Range.create(
					cmpLower >= 0 ? mLower : lower,
					cmpUpper <= 0 ? mUpper : upper);
		}
	}

	public Range<T> extend(T value) {
		Objects.requireNonNull(value, "value must not be null");
		return extend(value, value);
	}

	@Override
	public String toString() {
		return String.format("[%s, %s]", mLower, mUpper);
	}

	@Override
	public int hashCode() {
		return Objects.hash(mLower, mUpper);
	}
}
