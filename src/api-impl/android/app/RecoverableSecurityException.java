package android.app;

import java.util.Objects;

/* AOSP RecoverableSecurityException (android-16), the exception with the user's message and the
 * action that resolves it. Without parceling (ATL's RemoteAction is not Parcelable) and without
 * showAsNotification()/showAsDialog(). */
public final class RecoverableSecurityException extends SecurityException {
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
}
