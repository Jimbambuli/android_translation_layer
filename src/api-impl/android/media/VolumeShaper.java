package android.media;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Arrays;
import java.util.Objects;

/* AOSP media/java/android/media/VolumeShaper.java. A VolumeShaper comes from AudioTrack/MediaPlayer
 * createVolumeShaper() (PlayerBase), which ATL's players do not have, so no shaper is ever bound to a player:
 * every operation takes AOSP's path for a shaper without player (IllegalStateException "uninitialized shaper").
 * Configuration, Operation and State are AOSP's. */
public final class VolumeShaper implements AutoCloseable {

	private VolumeShaper() {}

	public void apply(Operation operation) {
		throw new IllegalStateException("uninitialized shaper");
	}

	public void replace(Configuration configuration, Operation operation, boolean join) {
		throw new IllegalStateException("uninitialized shaper");
	}

	public float getVolume() {
		throw new IllegalStateException("uninitialized shaper");
	}

	/* AOSP close(): the terminate operation's IllegalStateException is swallowed */
	@Override
	public void close() {}

	/* AOSP finalize(): close() */
	@Override
	protected void finalize() {
		close();
	}

	public static final class Configuration implements Parcelable {
		private static final int MAXIMUM_CURVE_POINTS = 16;

		public static int getMaximumCurvePoints() {
			return MAXIMUM_CURVE_POINTS;
		}
		 static final int TYPE_ID = 0;

		 static final int TYPE_SCALE = 1;
		public static final int INTERPOLATOR_TYPE_STEP = 0;

		public static final int INTERPOLATOR_TYPE_LINEAR = 1;

		public static final int INTERPOLATOR_TYPE_CUBIC = 2;

		public static final int INTERPOLATOR_TYPE_CUBIC_MONOTONIC = 3;
		public static final int OPTION_FLAG_VOLUME_IN_DBFS = (1 << 0);

		public static final int OPTION_FLAG_CLOCK_TIME = (1 << 1);

		private static final int OPTION_FLAG_PUBLIC_ALL =
				OPTION_FLAG_VOLUME_IN_DBFS | OPTION_FLAG_CLOCK_TIME;

		public static final Configuration LINEAR_RAMP = new VolumeShaper.Configuration.Builder()
				.setInterpolatorType(INTERPOLATOR_TYPE_LINEAR)
				.setCurve(new float[] {0.f, 1.f} ,
						new float[] {0.f, 1.f} )
				.setDuration(1000)
				.build();

		public static final Configuration CUBIC_RAMP = new VolumeShaper.Configuration.Builder()
				.setInterpolatorType(INTERPOLATOR_TYPE_CUBIC)
				.setCurve(new float[] {0.f, 1.f} ,
						new float[] {0.f, 1.f}  )
				.setDuration(1000)
				.build();

		public static final Configuration SINE_RAMP;

		public static final Configuration SCURVE_RAMP;

		static {
			final int POINTS = MAXIMUM_CURVE_POINTS;
			final float times[] = new float[POINTS];
			final float sines[] = new float[POINTS];
			final float scurve[] = new float[POINTS];
			for (int i = 0; i < POINTS; ++i) {
				times[i] = (float)i / (POINTS - 1);
				final float sine = (float)Math.sin(times[i] * Math.PI / 2.);
				sines[i] = sine;
				scurve[i] = sine * sine;
			}
			SINE_RAMP = new VolumeShaper.Configuration.Builder()
				.setInterpolatorType(INTERPOLATOR_TYPE_CUBIC)
				.setCurve(times, sines)
				.setDuration(1000)
				.build();
			SCURVE_RAMP = new VolumeShaper.Configuration.Builder()
				.setInterpolatorType(INTERPOLATOR_TYPE_CUBIC)
				.setCurve(times, scurve)
				.setDuration(1000)
				.build();
		}
		private final int mType;
		private final int mId;
		private final int mOptionFlags;
		private final double mDurationMs;
		private final int mInterpolatorType;
		private final float[] mTimes;
		private final float[] mVolumes;

		@Override
		public String toString() {
			return "VolumeShaper.Configuration{"
					+ "mType = " + mType
					+ ", mId = " + mId
					+ (mType == TYPE_ID
						? "}"
						: ", mOptionFlags = 0x" + Integer.toHexString(mOptionFlags).toUpperCase()
						+ ", mDurationMs = " + mDurationMs
						+ ", mInterpolatorType = " + mInterpolatorType
						+ ", mTimes[] = " + Arrays.toString(mTimes)
						+ ", mVolumes[] = " + Arrays.toString(mVolumes)
						+ "}");
		}

		@Override
		public int hashCode() {
			return mType == TYPE_ID
					? Objects.hash(mType, mId)
					: Objects.hash(mType, mId,
							mOptionFlags, mDurationMs, mInterpolatorType,
							Arrays.hashCode(mTimes), Arrays.hashCode(mVolumes));
		}

		@Override
		public boolean equals(Object o) {
			if (!(o instanceof Configuration)) return false;
			if (o == this) return true;
			final Configuration other = (Configuration) o;
			return mType == other.mType
					&& mId == other.mId
					&& (mType == TYPE_ID
						||  (mOptionFlags == other.mOptionFlags
							&& mDurationMs == other.mDurationMs
							&& mInterpolatorType == other.mInterpolatorType
							&& Arrays.equals(mTimes, other.mTimes)
							&& Arrays.equals(mVolumes, other.mVolumes)));
		}

		@Override
		public int describeContents() {
			return 0;
		}

		/* AOSP writes the AIDL parcelable VolumeShaperConfiguration (generated, not in ATL); the same values are
		 * written directly: type, id, and for TYPE_SCALE the option flags, duration, interpolator and curve */
		public void writeToParcel(Parcel dest, int flags) {
			dest.writeInt(mType);
			dest.writeInt(mId);
			if (mType != TYPE_ID) {
				dest.writeInt(mOptionFlags);
				dest.writeDouble(mDurationMs);
				dest.writeInt(mInterpolatorType);
				dest.writeFloatArray(mTimes);
				dest.writeFloatArray(mVolumes);
			}
		}

		public static final Parcelable.Creator<VolumeShaper.Configuration> CREATOR
				= new Parcelable.Creator<VolumeShaper.Configuration>() {
			@Override
			public VolumeShaper.Configuration createFromParcel(Parcel p) {
				final int type = p.readInt();
				final int id = p.readInt();
				if (type == TYPE_ID) {
					return new VolumeShaper.Configuration(id);
				}
				final int optionFlags = p.readInt();
				final double durationMs = p.readDouble();
				final int interpolatorType = p.readInt();
				final float[] times = p.createFloatArray();
				final float[] volumes = p.createFloatArray();
				return new VolumeShaper.Configuration(type, id, optionFlags, durationMs, interpolatorType,
						times, volumes);
			}

			@Override
			public VolumeShaper.Configuration[] newArray(int size) {
				return new VolumeShaper.Configuration[size];
			}
		};

		public Configuration(int id) {
			if (id < 0) {
				throw new IllegalArgumentException("negative id " + id);
			}
			mType = TYPE_ID;
			mId = id;
			mInterpolatorType = 0;
			mOptionFlags = 0;
			mDurationMs = 0;
			mTimes = null;
			mVolumes = null;
		}

		private Configuration(int type,
				int id,
				int optionFlags,
				double durationMs,
				int interpolatorType,
				float[] times,
				float[] volumes) {
			mType = type;
			mId = id;
			mOptionFlags = optionFlags;
			mDurationMs = durationMs;
			mInterpolatorType = interpolatorType;
			mTimes = times;
			mVolumes = volumes;
		}

		public int getType() {
			return mType;
		}

		public int getId() {
			return mId;
		}

		public int getInterpolatorType() {
			return mInterpolatorType;
		}

		public int getOptionFlags() {
			return mOptionFlags & OPTION_FLAG_PUBLIC_ALL;
		}

		 int getAllOptionFlags() {
			return mOptionFlags;
		}

		public long getDuration() {
			return (long) mDurationMs;
		}

		public float[] getTimes() {
			return mTimes;
		}

		public float[] getVolumes() {
			return mVolumes;
		}

		private static String checkCurveForErrors(
				float[] times, float[] volumes, boolean log) {
			if (times == null) {
				return "times array must be non-null";
			} else if (volumes == null) {
				return "volumes array must be non-null";
			} else if (times.length != volumes.length) {
				return "array length must match";
			} else if (times.length < 2) {
				return "array length must be at least 2";
			} else if (times.length > MAXIMUM_CURVE_POINTS) {
				return "array length must be no larger than " + MAXIMUM_CURVE_POINTS;
			} else if (times[0] != 0.f) {
				return "times must start at 0.f";
			} else if (times[times.length - 1] != 1.f) {
				return "times must end at 1.f";
			}
			for (int i = 1; i < times.length; ++i) {
				if (!(times[i] > times[i - 1]) ) {
					return "times not monotonic increasing, check index " + i;
				}
			}
			if (log) {
				for (int i = 0; i < volumes.length; ++i) {
					if (!(volumes[i] <= 0.f) ) {
						return "volumes for log scale cannot be positive, "
								+ "check index " + i;
					}
				}
			} else {
				for (int i = 0; i < volumes.length; ++i) {
					if (!(volumes[i] >= 0.f) || !(volumes[i] <= 1.f) ) {
						return "volumes for linear scale must be between 0.f and 1.f, "
								+ "check index " + i;
					}
				}
			}
			return null; // no errors
		}

		private static void checkCurveForErrorsAndThrowException(
				float[] times, float[] volumes, boolean log, boolean ise) {
			final String error = checkCurveForErrors(times, volumes, log);
			if (error != null) {
				if (ise) {
					throw new IllegalStateException(error);
				} else {
					throw new IllegalArgumentException(error);
				}
			}
		}

		private static void checkValidVolumeAndThrowException(float volume, boolean log) {
			if (log) {
				if (!(volume <= 0.f) ) {
					throw new IllegalArgumentException("dbfs volume must be 0.f or less");
				}
			} else {
				if (!(volume >= 0.f) || !(volume <= 1.f) ) {
					throw new IllegalArgumentException("volume must be >= 0.f and <= 1.f");
				}
			}
		}

		private static void clampVolume(float[] volumes, boolean log) {
			if (log) {
				for (int i = 0; i < volumes.length; ++i) {
					if (!(volumes[i] <= 0.f) ) {
						volumes[i] = 0.f;
					}
				}
			} else {
				for (int i = 0; i < volumes.length; ++i) {
					if (!(volumes[i] >= 0.f) ) {
						volumes[i] = 0.f;
					} else if (!(volumes[i] <= 1.f)) {
						volumes[i] = 1.f;
					}
				}
			}
		}

		public static final class Builder {
			private int mType = TYPE_SCALE;
			private int mId = -1; // invalid
			private int mInterpolatorType = INTERPOLATOR_TYPE_CUBIC;
			private int mOptionFlags = OPTION_FLAG_CLOCK_TIME;
			private double mDurationMs = 1000.;
			private float[] mTimes = null;
			private float[] mVolumes = null;

			public Builder() {
			}

			public Builder(Configuration configuration) {
				mType = configuration.getType();
				mId = configuration.getId();
				mOptionFlags = configuration.getAllOptionFlags();
				mInterpolatorType = configuration.getInterpolatorType();
				mDurationMs = configuration.getDuration();
				mTimes = configuration.getTimes().clone();
				mVolumes = configuration.getVolumes().clone();
			}

			public Builder setId(int id) {
				if (id < -1) {
					throw new IllegalArgumentException("invalid id: " + id);
				}
				mId = id;
				return this;
			}

			public Builder setInterpolatorType(int interpolatorType) {
				switch (interpolatorType) {
					case INTERPOLATOR_TYPE_STEP:
					case INTERPOLATOR_TYPE_LINEAR:
					case INTERPOLATOR_TYPE_CUBIC:
					case INTERPOLATOR_TYPE_CUBIC_MONOTONIC:
						mInterpolatorType = interpolatorType;
						break;
					default:
						throw new IllegalArgumentException("invalid interpolatorType: "
								+ interpolatorType);
				}
				return this;
			}

			public Builder setOptionFlags(int optionFlags) {
				if ((optionFlags & ~OPTION_FLAG_PUBLIC_ALL) != 0) {
					throw new IllegalArgumentException("invalid bits in flag: " + optionFlags);
				}
				mOptionFlags = mOptionFlags & ~OPTION_FLAG_PUBLIC_ALL | optionFlags;
				return this;
			}

			public Builder setDuration(long durationMillis) {
				if (durationMillis <= 0) {
					throw new IllegalArgumentException(
							"duration: " + durationMillis + " not positive");
				}
				mDurationMs = (double) durationMillis;
				return this;
			}

			public Builder setCurve(float[] times, float[] volumes) {
				final boolean log = (mOptionFlags & OPTION_FLAG_VOLUME_IN_DBFS) != 0;
				checkCurveForErrorsAndThrowException(times, volumes, log, false );
				mTimes = times.clone();
				mVolumes = volumes.clone();
				return this;
			}

			public Builder reflectTimes() {
				final boolean log = (mOptionFlags & OPTION_FLAG_VOLUME_IN_DBFS) != 0;
				checkCurveForErrorsAndThrowException(mTimes, mVolumes, log, true );
				int i;
				for (i = 0; i < mTimes.length / 2; ++i) {
					float temp = mTimes[i];
					mTimes[i] = 1.f - mTimes[mTimes.length - 1 - i];
					mTimes[mTimes.length - 1 - i] = 1.f - temp;
					temp = mVolumes[i];
					mVolumes[i] = mVolumes[mVolumes.length - 1 - i];
					mVolumes[mVolumes.length - 1 - i] = temp;
				}
				if ((mTimes.length & 1) != 0) {
					mTimes[i] = 1.f - mTimes[i];
				}
				return this;
			}

			public Builder invertVolumes() {
				final boolean log = (mOptionFlags & OPTION_FLAG_VOLUME_IN_DBFS) != 0;
				checkCurveForErrorsAndThrowException(mTimes, mVolumes, log, true );
				float min = mVolumes[0];
				float max = mVolumes[0];
				for (int i = 1; i < mVolumes.length; ++i) {
					if (mVolumes[i] < min) {
						min = mVolumes[i];
					} else if (mVolumes[i] > max) {
						max = mVolumes[i];
					}
				}

				final float maxmin = max + min;
				for (int i = 0; i < mVolumes.length; ++i) {
					mVolumes[i] = maxmin - mVolumes[i];
				}
				return this;
			}

			public Builder scaleToEndVolume(float volume) {
				final boolean log = (mOptionFlags & OPTION_FLAG_VOLUME_IN_DBFS) != 0;
				checkCurveForErrorsAndThrowException(mTimes, mVolumes, log, true );
				checkValidVolumeAndThrowException(volume, log);
				final float startVolume = mVolumes[0];
				final float endVolume = mVolumes[mVolumes.length - 1];
				if (endVolume == startVolume) {
					final float offset = volume - startVolume;
					for (int i = 0; i < mVolumes.length; ++i) {
						mVolumes[i] = mVolumes[i] + offset * mTimes[i];
					}
				} else {
					final float scale = (volume - startVolume) / (endVolume - startVolume);
					for (int i = 0; i < mVolumes.length; ++i) {
						mVolumes[i] = scale * (mVolumes[i] - startVolume) + startVolume;
					}
				}
				clampVolume(mVolumes, log);
				return this;
			}

			public Builder scaleToStartVolume(float volume) {
				final boolean log = (mOptionFlags & OPTION_FLAG_VOLUME_IN_DBFS) != 0;
				checkCurveForErrorsAndThrowException(mTimes, mVolumes, log, true );
				checkValidVolumeAndThrowException(volume, log);
				final float startVolume = mVolumes[0];
				final float endVolume = mVolumes[mVolumes.length - 1];
				if (endVolume == startVolume) {
					final float offset = volume - startVolume;
					for (int i = 0; i < mVolumes.length; ++i) {
						mVolumes[i] = mVolumes[i] + offset * (1.f - mTimes[i]);
					}
				} else {
					final float scale = (volume - endVolume) / (startVolume - endVolume);
					for (int i = 0; i < mVolumes.length; ++i) {
						mVolumes[i] = scale * (mVolumes[i] - endVolume) + endVolume;
					}
				}
				clampVolume(mVolumes, log);
				return this;
			}

			public Configuration build() {
				final boolean log = (mOptionFlags & OPTION_FLAG_VOLUME_IN_DBFS) != 0;
				checkCurveForErrorsAndThrowException(mTimes, mVolumes, log, true );
				return new Configuration(mType, mId, mOptionFlags, mDurationMs,
						mInterpolatorType, mTimes, mVolumes);
			}
		} // Configuration.Builder
	} // Configuration

	public static final class Operation implements Parcelable {
		
		public static final Operation PLAY =
				new VolumeShaper.Operation.Builder()
					.build();

		public static final Operation REVERSE =
				new VolumeShaper.Operation.Builder()
					.reverse()
					.build();
		private static final int FLAG_NONE = 0;

		private static final int FLAG_REVERSE = 1 << 0;

		private static final int FLAG_TERMINATE = 1 << 1;

		private static final int FLAG_JOIN = 1 << 2;

		private static final int FLAG_DEFER = 1 << 3;

		private static final int FLAG_CREATE_IF_NEEDED = 1 << 4;

		private static final int FLAG_PUBLIC_ALL = FLAG_REVERSE | FLAG_TERMINATE;

		private final int mFlags;
		private final int mReplaceId;
		private final float mXOffset;

		@Override
		public String toString() {
			return "VolumeShaper.Operation{"
					+ "mFlags = 0x" + Integer.toHexString(mFlags).toUpperCase()
					+ ", mReplaceId = " + mReplaceId
					+ ", mXOffset = " + mXOffset
					+ "}";
		}

		@Override
		public int hashCode() {
			return Objects.hash(mFlags, mReplaceId, mXOffset);
		}

		@Override
		public boolean equals(Object o) {
			if (!(o instanceof Operation)) return false;
			if (o == this) return true;
			final Operation other = (Operation) o;

			return mFlags == other.mFlags
					&& mReplaceId == other.mReplaceId
					&& Float.compare(mXOffset, other.mXOffset) == 0;
		}

		@Override
		public int describeContents() {
			return 0;
		}

		/* AOSP writes the AIDL parcelable VolumeShaperOperation (not in ATL); the same values directly */
		public void writeToParcel(Parcel dest, int flags) {
			dest.writeInt(mFlags);
			dest.writeInt(mReplaceId);
			dest.writeFloat(mXOffset);
		}

		public static final Parcelable.Creator<VolumeShaper.Operation> CREATOR
				= new Parcelable.Creator<VolumeShaper.Operation>() {
			@Override
			public VolumeShaper.Operation createFromParcel(Parcel p) {
				return new VolumeShaper.Operation(p.readInt(), p.readInt(), p.readFloat());
			}

			@Override
			public VolumeShaper.Operation[] newArray(int size) {
				return new VolumeShaper.Operation[size];
			}
		};

		private Operation(int flags, int replaceId, float xOffset) {
			mFlags = flags;
			mReplaceId = replaceId;
			mXOffset = xOffset;
		}

		public static final class Builder {
			int mFlags;
			int mReplaceId;
			float mXOffset;

			public Builder() {
				mFlags = 0;
				mReplaceId = -1;
				mXOffset = Float.NaN;
			}

			public Builder(VolumeShaper.Operation operation) {
				mReplaceId = operation.mReplaceId;
				mFlags = operation.mFlags;
				mXOffset = operation.mXOffset;
			}

			public Builder replace(int id, boolean join) {
				mReplaceId = id;
				if (join) {
					mFlags |= FLAG_JOIN;
				} else {
					mFlags &= ~FLAG_JOIN;
				}
				return this;
			}

			public Builder defer() {
				mFlags |= FLAG_DEFER;
				return this;
			}

			public Builder terminate() {
				mFlags |= FLAG_TERMINATE;
				return this;
			}

			public Builder reverse() {
				mFlags ^= FLAG_REVERSE;
				return this;
			}

			public Builder createIfNeeded() {
				mFlags |= FLAG_CREATE_IF_NEEDED;
				return this;
			}

			public Builder setXOffset(float xOffset) {
				if (xOffset < -0.f) {
					throw new IllegalArgumentException("Negative xOffset not allowed");
				} else if (xOffset > 1.f) {
					throw new IllegalArgumentException("xOffset > 1.f not allowed");
				}
				mXOffset = xOffset;
				return this;
			}

			private Builder setFlags(int flags) {
				if ((flags & ~FLAG_PUBLIC_ALL) != 0) {
					throw new IllegalArgumentException("flag has unknown bits set: " + flags);
				}
				mFlags = mFlags & ~FLAG_PUBLIC_ALL | flags;
				return this;
			}

			public Operation build() {
				return new Operation(mFlags, mReplaceId, mXOffset);
			}
		} // Operation.Builder
	} // Operation

	public static final class State implements Parcelable {
		private float mVolume;
		private float mXOffset;

		@Override
		public String toString() {
			return "VolumeShaper.State{"
					+ "mVolume = " + mVolume
					+ ", mXOffset = " + mXOffset
					+ "}";
		}

		@Override
		public int hashCode() {
			return Objects.hash(mVolume, mXOffset);
		}

		@Override
		public boolean equals(Object o) {
			if (!(o instanceof State)) return false;
			if (o == this) return true;
			final State other = (State) o;
			return mVolume == other.mVolume
					&& mXOffset == other.mXOffset;
		}

		@Override
		public int describeContents() {
			return 0;
		}

		/* AOSP writes the AIDL parcelable VolumeShaperState (not in ATL); the same values directly */
		public void writeToParcel(Parcel dest, int flags) {
			dest.writeFloat(mVolume);
			dest.writeFloat(mXOffset);
		}

		public static final Parcelable.Creator<VolumeShaper.State> CREATOR
				= new Parcelable.Creator<VolumeShaper.State>() {
			@Override
			public VolumeShaper.State createFromParcel(Parcel p) {
				return new VolumeShaper.State(p.readFloat(), p.readFloat());
			}

			@Override
			public VolumeShaper.State[] newArray(int size) {
				return new VolumeShaper.State[size];
			}
		};

		State(float volume, float xOffset) {
			mVolume = volume;
			mXOffset = xOffset;
		}

		public float getVolume() {
			return mVolume;
		}

		public float getXOffset() {
			return mXOffset;
		}
	} // State
}
