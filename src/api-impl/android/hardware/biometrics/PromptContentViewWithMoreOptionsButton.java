package android.hardware.biometrics;


import android.content.DialogInterface;
import android.hardware.biometrics.BiometricPrompt.ButtonInfo;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;


import java.util.concurrent.Executor;

public final class PromptContentViewWithMoreOptionsButton implements PromptContentViewParcelable {
	private static final String TAG = "PromptContentViewWithMoreOptionsButton";

	static final int MAX_DESCRIPTION_CHARACTER_NUMBER = 225;

	private final String mDescription;
	private DialogInterface.OnClickListener mListener;
	private ButtonInfo mButtonInfo;

	private PromptContentViewWithMoreOptionsButton(
			String description, Executor executor,
			DialogInterface.OnClickListener listener) {
		mDescription = description;
		mListener = listener;
		mButtonInfo = new ButtonInfo(executor, listener);
	}

	private PromptContentViewWithMoreOptionsButton(Parcel in) {
		mDescription = in.readString();
	}

	public String getDescription() {
		return mDescription;
	}

	public DialogInterface.OnClickListener getMoreOptionsButtonListener() {
		return mListener;
	}

	ButtonInfo getButtonInfo() {
		return mButtonInfo;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(mDescription);
	}

	public static final Creator<PromptContentViewWithMoreOptionsButton> CREATOR = new Creator<PromptContentViewWithMoreOptionsButton>() {
		@Override
		public PromptContentViewWithMoreOptionsButton createFromParcel(Parcel in) {
			return new PromptContentViewWithMoreOptionsButton(in);
		}

		@Override
		public PromptContentViewWithMoreOptionsButton[] newArray(int size) {
			return new PromptContentViewWithMoreOptionsButton[size];
		}
	};

	public static final class Builder {
		private String mDescription;
		private Executor mExecutor;
		private DialogInterface.OnClickListener mListener;

		public Builder setDescription(String description) {
			if (description.length() > MAX_DESCRIPTION_CHARACTER_NUMBER) {
				Log.w(TAG, "The character number of description exceeds "
						+ MAX_DESCRIPTION_CHARACTER_NUMBER);
			}
			mDescription = description;
			return this;
		}

		public Builder setMoreOptionsButtonListener(Executor executor,
				DialogInterface.OnClickListener listener) {
			mExecutor = executor;
			mListener = listener;
			return this;
		}

		public PromptContentViewWithMoreOptionsButton build() {
			if (mExecutor == null) {
				throw new IllegalArgumentException(
						"The executor for the listener of more options button on prompt content "
								+ "must be set and non-null if "
								+ "PromptContentViewWithMoreOptionsButton is used.");
			}
			if (mListener == null) {
				throw new IllegalArgumentException(
						"The listener of more options button on prompt content must be set and "
								+ "non-null if PromptContentViewWithMoreOptionsButton is used.");
			}
			return new PromptContentViewWithMoreOptionsButton(mDescription, mExecutor, mListener);
		}
	}
}
