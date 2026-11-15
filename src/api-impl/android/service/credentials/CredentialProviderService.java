/*
 * Copyright (C) 2022 The Android Open Source Project
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

package android.service.credentials;

import android.annotation.CallSuper;
import android.annotation.NonNull;
import android.annotation.SdkConstant;
import android.app.Service;
import android.content.Intent;
import android.credentials.ClearCredentialStateException;
import android.credentials.CreateCredentialException;
import android.credentials.GetCredentialException;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.OutcomeReceiver;

/**
 * Service to be extended by credential providers (AOSP CredentialProviderService).
 *
 * <p>ATL has no ICredentialProviderService stub: the system credential manager never binds
 * to the provider, so only the lifecycle, the constants and the abstract hooks are ported.
 */
public abstract class CredentialProviderService extends Service {
	/** AOSP CredentialProviderService.EXTRA_CREATE_CREDENTIAL_REQUEST */
	public static final String EXTRA_CREATE_CREDENTIAL_REQUEST =
			"android.service.credentials.extra.CREATE_CREDENTIAL_REQUEST";

	/** AOSP CredentialProviderService.EXTRA_GET_CREDENTIAL_REQUEST */
	public static final String EXTRA_GET_CREDENTIAL_REQUEST =
			"android.service.credentials.extra.GET_CREDENTIAL_REQUEST";

	/** AOSP CredentialProviderService.EXTRA_CREATE_CREDENTIAL_RESPONSE */
	public static final String EXTRA_CREATE_CREDENTIAL_RESPONSE =
			"android.service.credentials.extra.CREATE_CREDENTIAL_RESPONSE";

	/** AOSP CredentialProviderService.EXTRA_GET_CREDENTIAL_RESPONSE */
	public static final String EXTRA_GET_CREDENTIAL_RESPONSE =
			"android.service.credentials.extra.GET_CREDENTIAL_RESPONSE";

	/** AOSP CredentialProviderService.EXTRA_BEGIN_GET_CREDENTIAL_RESPONSE */
	public static final String EXTRA_BEGIN_GET_CREDENTIAL_RESPONSE =
			"android.service.credentials.extra.BEGIN_GET_CREDENTIAL_RESPONSE";

	/** AOSP CredentialProviderService.EXTRA_GET_CREDENTIAL_EXCEPTION */
	public static final String EXTRA_GET_CREDENTIAL_EXCEPTION =
			"android.service.credentials.extra.GET_CREDENTIAL_EXCEPTION";

	/** AOSP CredentialProviderService.EXTRA_CREATE_CREDENTIAL_EXCEPTION */
	public static final String EXTRA_CREATE_CREDENTIAL_EXCEPTION =
			"android.service.credentials.extra.CREATE_CREDENTIAL_EXCEPTION";

	/** AOSP CredentialProviderService.EXTRA_BEGIN_GET_CREDENTIAL_REQUEST */
	public static final String EXTRA_BEGIN_GET_CREDENTIAL_REQUEST =
			"android.service.credentials.extra.BEGIN_GET_CREDENTIAL_REQUEST";

	/** AOSP CredentialProviderService.EXTRA_AUTOFILL_ID */
	public static final String EXTRA_AUTOFILL_ID =
			"android.service.credentials.extra.AUTOFILL_ID";

	/** AOSP CredentialProviderService.SERVICE_META_DATA */
	public static final String SERVICE_META_DATA = "android.credentials.provider";

	/** AOSP CredentialProviderService.TEST_SYSTEM_PROVIDER_META_DATA_KEY */
	public static final String TEST_SYSTEM_PROVIDER_META_DATA_KEY =
			"android.credentials.testsystemprovider";

	private Handler mHandler;

	/** AOSP CredentialProviderService.SERVICE_INTERFACE */
	@SdkConstant(SdkConstant.SdkConstantType.SERVICE_ACTION)
	public static final String SERVICE_INTERFACE =
			"android.service.credentials.CredentialProviderService";

	/** AOSP CredentialProviderService.SYSTEM_SERVICE_INTERFACE */
	@SdkConstant(SdkConstant.SdkConstantType.SERVICE_ACTION)
	public static final String SYSTEM_SERVICE_INTERFACE =
			"android.service.credentials.system.CredentialProviderService";

	@CallSuper
	@Override
	public void onCreate() {
		super.onCreate();
		mHandler = new Handler(Looper.getMainLooper(), null, true);
	}

	/**
	 * AOSP CredentialProviderService.onBind returns the ICredentialProviderService binder for
	 * SERVICE_INTERFACE; no ICredentialProviderService stub, nobody binds.
	 */
	@Override
	public final IBinder onBind(@NonNull Intent intent) {
		return null;
	}

	/** AOSP CredentialProviderService.onBeginGetCredential */
	public abstract void onBeginGetCredential(@NonNull BeginGetCredentialRequest request,
			@NonNull CancellationSignal cancellationSignal,
			@NonNull OutcomeReceiver<
					BeginGetCredentialResponse, GetCredentialException> callback);

	/** AOSP CredentialProviderService.onBeginCreateCredential */
	public abstract void onBeginCreateCredential(@NonNull BeginCreateCredentialRequest request,
			@NonNull CancellationSignal cancellationSignal,
			@NonNull OutcomeReceiver<BeginCreateCredentialResponse,
					CreateCredentialException> callback);

	/** AOSP CredentialProviderService.onClearCredentialState */
	public abstract void onClearCredentialState(@NonNull ClearCredentialStateRequest request,
			@NonNull CancellationSignal cancellationSignal,
			@NonNull OutcomeReceiver<Void,
					ClearCredentialStateException> callback);
}
