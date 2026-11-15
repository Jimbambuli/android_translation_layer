package android.app;

import java.util.Objects;

/* AOSP RecoverableSecurityException (android-16), the exception with the user's message and the
 * action that resolves it. Without parceling (ATL's RemoteAction is not Parcelable) and without
 * showAsNotification()/showAsDialog(). */
public final class RecoverableSecurityException extends SecurityException implements android.os.Parcelable {
	private final CharSequence mUserMessage;
	private final RemoteAction mUserAction;

	public RecoverableSecurityException(Throwable cause, CharSequence userMessage, RemoteAction userAction) {
		super(cause.getMessage());
		mUserMessage = Objects.requireNonNull(userMessage);
		mUserAction = Objects.requireNonNull(userAction);
	}

	public CharSequence getUserMessage() {
		return mUserMessage;
	}

	public RemoteAction getUserAction() {
		return mUserAction;
	}

	/* AOSP RecoverableSecurityException(Parcel)/writeToParcel()/describeContents()/CREATOR */
	public RecoverableSecurityException(android.os.Parcel in) {
		this(new SecurityException(in.readString()), in.readCharSequence(), RemoteAction.CREATOR.createFromParcel(in));
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(android.os.Parcel dest, int flags) {
		dest.writeString(getMessage());
		dest.writeCharSequence(mUserMessage);
		mUserAction.writeToParcel(dest, flags);
	}

	public static final android.os.Parcelable.Creator<RecoverableSecurityException> CREATOR = new android.os.Parcelable.Creator<RecoverableSecurityException>() {
		@Override
		public RecoverableSecurityException createFromParcel(android.os.Parcel source) {
			return new RecoverableSecurityException(source);
		}

		@Override
		public RecoverableSecurityException[] newArray(int size) {
			return new RecoverableSecurityException[size];
		}
	};
}
