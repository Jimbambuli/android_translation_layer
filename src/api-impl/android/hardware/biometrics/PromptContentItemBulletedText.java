package android.hardware.biometrics;

import android.os.Parcel;
import android.os.Parcelable;

public final class PromptContentItemBulletedText implements PromptContentItemParcelable {
	private final String mText;

	public PromptContentItemBulletedText(String text) {
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

	public static final Creator<PromptContentItemBulletedText> CREATOR = new Creator<>() {
		@Override
		public PromptContentItemBulletedText createFromParcel(Parcel in) {
			return new PromptContentItemBulletedText(in.readString());
		}

		@Override
		public PromptContentItemBulletedText[] newArray(int size) {
			return new PromptContentItemBulletedText[size];
		}
	};
}
