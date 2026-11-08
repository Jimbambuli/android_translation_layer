package android.app;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.ArraySet;

public class RemoteInput implements Parcelable {

	private static final int FLAG_ALLOW_FREE_FORM_INPUT = 0x1;

	private final String mResultKey;
	private final CharSequence mLabel;
	private final CharSequence[] mChoices;
	private final int mFlags;
	private final Bundle mExtras;

	private RemoteInput(String resultKey, CharSequence label, CharSequence[] choices, int flags, Bundle extras) {
		mResultKey = resultKey;
		mLabel = label;
		mChoices = choices;
		mFlags = flags;
		mExtras = extras;
	}

	public String getResultKey() {
		return mResultKey;
	}

	public CharSequence getLabel() {
		return mLabel;
	}

	public CharSequence[] getChoices() {
		return mChoices;
	}

	public boolean getAllowFreeFormInput() {
		return (mFlags & FLAG_ALLOW_FREE_FORM_INPUT) != 0;
	}

	public Bundle getExtras() {
		return mExtras;
	}

	public static class Builder {
		private final String mResultKey;
		private final Bundle mExtras = new Bundle();
		private CharSequence mLabel;
		private CharSequence[] mChoices;
		private int mFlags = FLAG_ALLOW_FREE_FORM_INPUT;

		public Builder(String resultKey) {
			if (resultKey == null) {
				throw new IllegalArgumentException("Result key can't be null");
			}
			mResultKey = resultKey;
		}

		public Builder setLabel(CharSequence label) {
			mLabel = label;
			return this;
		}

		public Builder setChoices(CharSequence[] choices) {
			mChoices = choices != null ? choices.clone() : null;
			return this;
		}

		public Builder setAllowFreeFormInput(boolean allowFreeFormInput) {
			if (allowFreeFormInput) {
				mFlags |= FLAG_ALLOW_FREE_FORM_INPUT;
			} else {
				mFlags &= ~FLAG_ALLOW_FREE_FORM_INPUT;
			}
			return this;
		}

		public Builder addExtras(Bundle extras) {
			if (extras != null) {
				mExtras.putAll(extras);
			}
			return this;
		}

		public RemoteInput build() {
			return new RemoteInput(mResultKey, mLabel, mChoices, mFlags, mExtras);
		}
	}

	/* AOSP RemoteInput(Parcel) / writeToParcel() / CREATOR, wire format of android-14; ATL has no data types or
	 * choice editing, so the edit-choices value is written as EDIT_CHOICES_BEFORE_SENDING_AUTO (0) and the
	 * allowed data types as an empty set. */
	private RemoteInput(Parcel in) {
		mResultKey = in.readString();
		mLabel = in.readCharSequence();
		mChoices = in.readCharSequenceArray();
		mFlags = in.readInt();
		in.readInt(); /* edit choices before sending */
		mExtras = in.readBundle();
		in.readArraySet(null); /* allowed data types */
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeString(mResultKey);
		out.writeCharSequence(mLabel);
		out.writeCharSequenceArray(mChoices);
		out.writeInt(mFlags);
		out.writeInt(0);
		out.writeBundle(mExtras);
		out.writeArraySet(new ArraySet<String>());
	}

	public static final Creator<RemoteInput> CREATOR = new Creator<RemoteInput>() {
		@Override
		public RemoteInput createFromParcel(Parcel in) {
			return new RemoteInput(in);
		}

		@Override
		public RemoteInput[] newArray(int size) {
			return new RemoteInput[size];
		}
	};
}
