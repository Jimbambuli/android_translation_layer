package android.hardware.biometrics;

import android.os.Parcel;
import android.os.Parcelable;

public final class PromptContentItemPlainText implements PromptContentItemParcelable {
	private final String mText;

	public PromptContentItemPlainText(String text) {
		mText = text;
	}

	public String getText() {
		return mText;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(mText);
	}

	public static final Creator<PromptContentItemPlainText> CREATOR = new Creator<>() {
		@Override
		public PromptContentItemPlainText createFromParcel(Parcel in) {
			return new PromptContentItemPlainText(in.readString());
		}

		@Override
		public PromptContentItemPlainText[] newArray(int size) {
			return new PromptContentItemPlainText[size];
		}
	};
}
