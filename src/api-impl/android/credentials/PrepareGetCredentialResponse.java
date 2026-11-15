package android.credentials;

import android.content.Context;
import android.os.CancellationSignal;
import android.os.OutcomeReceiver;
import java.util.concurrent.Executor;

/* AOSP android.credentials.PrepareGetCredentialResponse. An instance only comes from the credential manager service
 * (CredentialManager.prepareGetCredential()), which ATL does not have, so none exists. The class is here for the
 * type; its answers are those of a response without results. */
public final class PrepareGetCredentialResponse {
	public static final class PendingGetCredentialHandle {
		PendingGetCredentialHandle() {}

		/* AOSP PendingGetCredentialHandle.show(): without a PendingIntent the result is TYPE_NO_CREDENTIAL */
		void show(Context context, CancellationSignal cancellationSignal, Executor executor,
		          OutcomeReceiver<GetCredentialResponse, GetCredentialException> callback) {
			executor.execute(() -> callback.onError(new GetCredentialException(GetCredentialException.TYPE_NO_CREDENTIAL)));
		}
	}

	private final PendingGetCredentialHandle mPendingGetCredentialHandle = new PendingGetCredentialHandle();

	private PrepareGetCredentialResponse() {}

	public boolean hasCredentialResults(String credentialType) {
		return false;
	}

	public boolean hasAuthenticationResults() {
		return false;
	}

	public boolean hasRemoteResults() {
		return false;
	}

	public PendingGetCredentialHandle getPendingGetCredentialHandle() {
		return mPendingGetCredentialHandle;
	}
}
