package android.hardware.biometrics;

import android.annotation.NonNull;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Contains the information for a fallback option to be displayed within Biometric Prompt (AOSP android-16).
 */
public final class FallbackOption implements Parcelable {
	@NonNull private final CharSequence mText;
	private final int mIconType;

	public @NonNull CharSequence getText() {
		return mText;
	}

	public int getIconType() {
		return mIconType;
	}

	public FallbackOption(@NonNull CharSequence text, int iconType) {
		this.mText = text;
		this.mIconType = iconType;
	}

	private FallbackOption(Parcel in) {
		mText = in.readCharSequence();
		mIconType = in.readInt();
	}

	@NonNull public static final Creator<FallbackOption> CREATOR = new Creator<FallbackOption>() {
		@Override
		public FallbackOption createFromParcel(Parcel in) {
			return new FallbackOption(in);
		}

		@Override
		public FallbackOption[] newArray(int size) {
			return new FallbackOption[size];
		}
	};

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(@NonNull Parcel dest, int flags) {
		dest.writeCharSequence(mText);
		dest.writeInt(mIconType);
	}
}
