package android.os;

import android.util.SparseArray;
import java.util.Objects;

/* AOSP CombinedVibration (frameworks/base/core/java/android/os): effects for several vibrators at once. The public
 * API (createParallel(), startParallel()) with the Mono and Stereo forms it produces. Not carried: the @TestApi
 * Sequential form and the duration/transform/adapt methods, which need VibrationEffect.validate()/getDuration() and
 * VibratorInfo that ATL's VibrationEffect does not have. The validate() checks are those AOSP makes before
 * VibrationEffect.validate(): effect not null, at least one effect. */
public abstract class CombinedVibration implements Parcelable {
	private static final int PARCEL_TOKEN_MONO = 1;
	private static final int PARCEL_TOKEN_STEREO = 2;
	private static final int PARCEL_TOKEN_SEQUENTIAL = 3;

	CombinedVibration() {}

	public static CombinedVibration createParallel(VibrationEffect effect) {
		CombinedVibration combined = new Mono(effect);
		combined.validate();
		return combined;
	}

	public static ParallelCombination startParallel() {
		return new ParallelCombination();
	}

	@Override
	public int describeContents() {
		return 0;
	}

	public abstract void validate();

	public abstract boolean hasVibrator(int vibratorId);

	/* AOSP CombinedVibration.ParallelCombination */
	public static final class ParallelCombination {
		private final SparseArray<VibrationEffect> mEffects = new SparseArray<>();

		ParallelCombination() {}

		public ParallelCombination addVibrator(int vibratorId, VibrationEffect effect) {
			mEffects.put(vibratorId, effect);
			return this;
		}

		public CombinedVibration combine() {
			if (mEffects.size() == 0) {
				throw new IllegalStateException("Combination must have at least one element to combine.");
			}
			CombinedVibration combined = new Stereo(mEffects);
			combined.validate();
			return combined;
		}
	}

	/* AOSP CombinedVibration.Mono: one effect for all vibrators */
	public static final class Mono extends CombinedVibration {
		private final VibrationEffect mEffect;

		Mono(Parcel in) {
			mEffect = VibrationEffect.CREATOR.createFromParcel(in);
		}

		Mono(VibrationEffect effect) {
			mEffect = effect;
		}

		public VibrationEffect getEffect() {
			return mEffect;
		}

		@Override
		public void validate() {
			Objects.requireNonNull(mEffect);
		}

		@Override
		public boolean hasVibrator(int vibratorId) {
			return true;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (!(o instanceof Mono)) {
				return false;
			}
			return mEffect.equals(((Mono)o).mEffect);
		}

		@Override
		public int hashCode() {
			return Objects.hash(mEffect);
		}

		@Override
		public String toString() {
			return "Mono{mEffect=" + mEffect + '}';
		}

		@Override
		public void writeToParcel(Parcel out, int flags) {
			out.writeInt(PARCEL_TOKEN_MONO);
			mEffect.writeToParcel(out, flags);
		}

		public static final Parcelable.Creator<Mono> CREATOR = new Parcelable.Creator<Mono>() {
			@Override
			public Mono createFromParcel(Parcel in) {
				in.readInt();
				return new Mono(in);
			}

			@Override
			public Mono[] newArray(int size) {
				return new Mono[size];
			}
		};
	}

	/* AOSP CombinedVibration.Stereo: an effect per vibrator id */
	public static final class Stereo extends CombinedVibration {
		private final SparseArray<VibrationEffect> mEffects;

		Stereo(Parcel in) {
			int size = in.readInt();
			mEffects = new SparseArray<>(size);
			for (int i = 0; i < size; i++) {
				int vibratorId = in.readInt();
				mEffects.put(vibratorId, VibrationEffect.CREATOR.createFromParcel(in));
			}
		}

		Stereo(SparseArray<VibrationEffect> effects) {
			mEffects = new SparseArray<>(effects.size());
			for (int i = 0; i < effects.size(); i++) {
				mEffects.put(effects.keyAt(i), effects.valueAt(i));
			}
		}

		public SparseArray<VibrationEffect> getEffects() {
			return mEffects;
		}

		@Override
		public void validate() {
			if (mEffects.size() <= 0) {
				throw new IllegalArgumentException("There should be at least one effect set for a combined effect");
			}
			for (int i = 0; i < mEffects.size(); i++) {
				Objects.requireNonNull(mEffects.valueAt(i));
			}
		}

		@Override
		public boolean hasVibrator(int vibratorId) {
			return mEffects.indexOfKey(vibratorId) >= 0;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (!(o instanceof Stereo)) {
				return false;
			}
			Stereo other = (Stereo)o;
			if (mEffects.size() != other.mEffects.size()) {
				return false;
			}
			for (int i = 0; i < mEffects.size(); i++) {
				if (!mEffects.valueAt(i).equals(other.mEffects.get(mEffects.keyAt(i)))) {
					return false;
				}
			}
			return true;
		}

		@Override
		public int hashCode() {
			return mEffects.contentHashCode();
		}

		@Override
		public String toString() {
			return "Stereo{mEffects=" + mEffects + '}';
		}

		@Override
		public void writeToParcel(Parcel out, int flags) {
			out.writeInt(PARCEL_TOKEN_STEREO);
			out.writeInt(mEffects.size());
			for (int i = 0; i < mEffects.size(); i++) {
				out.writeInt(mEffects.keyAt(i));
				mEffects.valueAt(i).writeToParcel(out, flags);
			}
		}

		public static final Parcelable.Creator<Stereo> CREATOR = new Parcelable.Creator<Stereo>() {
			@Override
			public Stereo createFromParcel(Parcel in) {
				in.readInt();
				return new Stereo(in);
			}

			@Override
			public Stereo[] newArray(int size) {
				return new Stereo[size];
			}
		};
	}

	public static final Parcelable.Creator<CombinedVibration> CREATOR = new Parcelable.Creator<CombinedVibration>() {
		@Override
		public CombinedVibration createFromParcel(Parcel in) {
			int token = in.readInt();
			if (token == PARCEL_TOKEN_MONO) {
				return new Mono(in);
			} else if (token == PARCEL_TOKEN_STEREO) {
				return new Stereo(in);
			} else {
				/* PARCEL_TOKEN_SEQUENTIAL (the Sequential form) is not carried */
				throw new IllegalStateException("Unexpected combined vibration event type token in parcel.");
			}
		}

		@Override
		public CombinedVibration[] newArray(int size) {
			return new CombinedVibration[size];
		}
	};
}
