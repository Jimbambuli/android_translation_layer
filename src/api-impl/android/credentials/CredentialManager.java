/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.credentials;

/**
 * Manages user authentication flows.
 *
 * ATL has no credential manager service: like on a device without one,
 * Context.getSystemService(Context.CREDENTIAL_SERVICE) returns null and androidx.credentials
 * reports the framework API as unavailable. The class exists for apps that refer to it.
 */
public final class CredentialManager {
	private CredentialManager() {}

	/* The methods of AOSP CredentialManager up to the call of ICredentialManager (mService). No instance exists (the
	 * service is missing, getSystemService(CREDENTIAL_SERVICE) is null), so the service call is not reachable. */
	private static final String TAG = "CredentialManager";

	public void getCredential(android.content.Context context, GetCredentialRequest request,
	                          android.os.CancellationSignal cancellationSignal, java.util.concurrent.Executor executor,
	                          android.os.OutcomeReceiver<GetCredentialResponse, GetCredentialException> callback) {
		java.util.Objects.requireNonNull(request, "request must not be null");
		java.util.Objects.requireNonNull(context, "context must not be null");
		java.util.Objects.requireNonNull(executor, "executor must not be null");
		java.util.Objects.requireNonNull(callback, "callback must not be null");
		if (cancellationSignal != null && cancellationSignal.isCanceled()) {
			android.util.Log.w(TAG, "getCredential already canceled");
			return;
		}
		throw new IllegalStateException("no credential manager service");
	}

	public void createCredential(android.content.Context context, CreateCredentialRequest request,
	                             android.os.CancellationSignal cancellationSignal,
	                             java.util.concurrent.Executor executor,
	                             android.os.OutcomeReceiver<CreateCredentialResponse, CreateCredentialException> callback) {
		java.util.Objects.requireNonNull(request, "request must not be null");
		java.util.Objects.requireNonNull(context, "context must not be null");
		java.util.Objects.requireNonNull(executor, "executor must not be null");
		java.util.Objects.requireNonNull(callback, "callback must not be null");
		if (cancellationSignal != null && cancellationSignal.isCanceled()) {
			android.util.Log.w(TAG, "createCredential already canceled");
			return;
		}
		throw new IllegalStateException("no credential manager service");
	}

	public void clearCredentialState(ClearCredentialStateRequest request, android.os.CancellationSignal cancellationSignal,
	                                 java.util.concurrent.Executor executor,
	                                 android.os.OutcomeReceiver<Void, ClearCredentialStateException> callback) {
		java.util.Objects.requireNonNull(request, "request must not be null");
		java.util.Objects.requireNonNull(executor, "executor must not be null");
		java.util.Objects.requireNonNull(callback, "callback must not be null");
		if (cancellationSignal != null && cancellationSignal.isCanceled()) {
			android.util.Log.w(TAG, "clearCredentialState already canceled");
			return;
		}
		throw new IllegalStateException("no credential manager service");
	}
}
