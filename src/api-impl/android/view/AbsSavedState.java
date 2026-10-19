package android.view;

import android.os.Parcel;
import android.os.Parcelable;

/* as in AOSP: the base of the states that views save, holding the state of their superclass */
public abstract class AbsSavedState implements Parcelable {
	public static final AbsSavedState EMPTY_STATE = new AbsSavedState() {};

	private final Parcelable mSuperState;

	private AbsSavedState() {
		mSuperState = null;
	}

	protected AbsSavedState(Parcelable superState) {
		if (superState == null)
			throw new IllegalArgumentException("superState must not be null");
		mSuperState = superState != EMPTY_STATE ? superState : null;
	}

	protected AbsSavedState(Parcel source) {
		this(source, null);
	}

	protected AbsSavedState(Parcel source, ClassLoader loader) {
		Parcelable superState = source.readParcelable(loader != null ? loader : AbsSavedState.class.getClassLoader());
		mSuperState = superState != null ? superState : EMPTY_STATE;
	}

	public final Parcelable getSuperState() {
		return mSuperState;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeParcelable(mSuperState, flags);
	}

	public static final Parcelable.Creator<AbsSavedState> CREATOR = new Parcelable.Creator<AbsSavedState>() {
		@Override
		public AbsSavedState createFromParcel(Parcel in) {
			Parcelable superState = in.readParcelable(AbsSavedState.class.getClassLoader());
			if (superState != null)
				throw new IllegalStateException("superState must be null");
			return EMPTY_STATE;
		}

		@Override
		public AbsSavedState[] newArray(int size) {
			return new AbsSavedState[size];
		}
	};
}
