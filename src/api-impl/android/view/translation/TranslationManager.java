/*
 * Copyright (C) 2020 The Android Open Source Project
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

package android.view.translation;

import android.annotation.CallbackExecutor;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.PendingIntent;
import android.content.Context;
import android.util.ArrayMap;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * The {@link TranslationManager} class provides ways for apps to integrate and use the
 * translation framework.
 *
 * ATL: AOSP's manager talks to the system's translation service (ITranslationManager), a binder
 * service that ATL does not have. It behaves as AOSP does on a device whose translation service has no
 * translation available: no translator can be created (the callback gets null), there are no capabilities,
 * the capability listeners are kept but never called and there is no settings activity. Context
 * returns no TranslationManager as a device without the service does, the constructor is for tests.
 */
public final class TranslationManager {
	private static final String TAG = "TranslationManager";

	/**
	 * Timeout for calls to system_server.
	 */
	static final int SYNC_CALLS_TIMEOUT_MS = 60_000;
	/**
	 * The result code from result receiver success.
	 * @hide
	 */
	public static final int STATUS_SYNC_CALL_SUCCESS = 1;
	/**
	 * The result code from result receiver fail.
	 * @hide
	 */
	public static final int STATUS_SYNC_CALL_FAIL = 2;

	/**
	 * Name of the extra used to pass the translation capabilities.
	 * @hide
	 */
	public static final String EXTRA_CAPABILITIES = "translation_capabilities";

	private final Object mLock = new Object();

	@NonNull private final Context mContext;

	private final Map<Consumer<TranslationCapability>, Executor> mCapabilityCallbacks = new ArrayMap<>();

	/**
	 * @hide
	 */
	public TranslationManager(@NonNull Context context) {
		mContext = Objects.requireNonNull(context, "context cannot be null");
	}

	/**
	 * Create a Translator for translation.
	 *
	 * @param translationContext {@link TranslationContext} containing the specs for creating the
	 *                                                     Translator.
	 * @param executor Executor to run callback operations
	 * @param callback {@link Consumer} to receive the translator. A {@code null} value is returned
	 *                                 if the service could not create the translator.
	 */
	public void createOnDeviceTranslator(@NonNull TranslationContext translationContext,
	                                     @NonNull @CallbackExecutor Executor executor, @NonNull Consumer<Translator> callback) {
		Objects.requireNonNull(translationContext, "translationContext cannot be null");
		Objects.requireNonNull(executor, "executor cannot be null");
		Objects.requireNonNull(callback, "callback cannot be null");

		executor.execute(() -> callback.accept(null));
	}

	/**
	 * Creates an on-device Translator for natural language translation.
	 *
	 * <p><strong>NOTE: </strong>Call on a worker thread.
	 *
	 * @removed use {@link #createOnDeviceTranslator(TranslationContext, Executor, Consumer)}
	 * instead.
	 *
	 * @param translationContext {@link TranslationContext} containing the specs for creating the
	 *                                                     Translator.
	 */
	@Deprecated
	@Nullable
	public Translator createOnDeviceTranslator(@NonNull TranslationContext translationContext) {
		Objects.requireNonNull(translationContext, "translationContext cannot be null");
		return null;
	}

	/**
	 * @removed Use {@link #createOnDeviceTranslator(TranslationContext)} instead.
	 */
	@Deprecated
	@Nullable
	public Translator createTranslator(@NonNull TranslationContext translationContext) {
		return createOnDeviceTranslator(translationContext);
	}

	/**
	 * Returns a set of {@link TranslationCapability}s describing the capabilities for
	 * {@link Translator}s.
	 *
	 * <p>These translation capabilities contains a source and target {@link TranslationSpec}
	 * representing the data expected for both ends of translation process. The capabilities
	 * provides the information and limitations for generating a {@link TranslationContext}.
	 * The {@link TranslationContext} will be used by
	 * {@link #createOnDeviceTranslator(TranslationContext, Executor, Consumer)} to create a
	 * {@link Translator} that can offer the translation service.</p>
	 *
	 * <p><strong>NOTE: </strong>Call on a worker thread.
	 *
	 * @param sourceFormat data format for the input data to be translated.
	 * @param targetFormat data format for the expected translated output data.
	 * @return A set of {@link TranslationCapability}s.
	 */
	@NonNull
	public Set<TranslationCapability> getOnDeviceTranslationCapabilities(
	    @TranslationSpec.DataFormat int sourceFormat,
	    @TranslationSpec.DataFormat int targetFormat) {
		return Collections.emptySet();
	}

	/**
	 * @removed Use {@link #getOnDeviceTranslationCapabilities(int, int)} instead.
	 */
	@Deprecated
	@NonNull
	public Set<TranslationCapability> getTranslationCapabilities(
	    @TranslationSpec.DataFormat int sourceFormat,
	    @TranslationSpec.DataFormat int targetFormat) {
		return getOnDeviceTranslationCapabilities(sourceFormat, targetFormat);
	}

	/**
	 * Registers a {@link Consumer} to listen for updates to {@link TranslationCapability}s on
	 * the device.
	 *
	 * @param executor the {@link Executor} to execute the callback operations
	 * @param capabilityListener a {@link Consumer} to receive the updated
	 *                           {@link TranslationCapability} from the on-device translation service
	 */
	public void addOnDeviceTranslationCapabilityUpdateListener(
	    @NonNull @CallbackExecutor Executor executor,
	    @NonNull Consumer<TranslationCapability> capabilityListener) {
		Objects.requireNonNull(executor, "executor should not be null");
		Objects.requireNonNull(capabilityListener, "capability listener should not be null");

		synchronized (mLock) {
			if (mCapabilityCallbacks.containsKey(capabilityListener)) {
				return;
			}
			mCapabilityCallbacks.put(capabilityListener, executor);
		}
	}

	/**
	 * @removed Use {@link #addOnDeviceTranslationCapabilityUpdateListener(Executor, Consumer)}
	 * instead.
	 */
	@Deprecated
	public void addOnDeviceTranslationCapabilityUpdateListener(
	    @TranslationSpec.DataFormat int sourceFormat,
	    @TranslationSpec.DataFormat int targetFormat,
	    @NonNull PendingIntent pendingIntent) {
		Objects.requireNonNull(pendingIntent, "pending intent should not be null");
	}

	/**
	 * @removed Use {@link #addOnDeviceTranslationCapabilityUpdateListener(Executor, Consumer)}
	 * instead.
	 */
	@Deprecated
	public void addTranslationCapabilityUpdateListener(
	    @TranslationSpec.DataFormat int sourceFormat,
	    @TranslationSpec.DataFormat int targetFormat,
	    @NonNull PendingIntent pendingIntent) {
		addOnDeviceTranslationCapabilityUpdateListener(sourceFormat, targetFormat, pendingIntent);
	}

	/**
	 * Unregisters a {@link Consumer} to listen for updates to {@link TranslationCapability}s on
	 * the device.
	 *
	 * @param capabilityListener the {@link Consumer} to unregister
	 */
	public void removeOnDeviceTranslationCapabilityUpdateListener(
	    @NonNull Consumer<TranslationCapability> capabilityListener) {
		Objects.requireNonNull(capabilityListener, "capability callback should not be null");

		synchronized (mLock) {
			mCapabilityCallbacks.remove(capabilityListener);
		}
	}

	/**
	 * @removed Use {@link #removeOnDeviceTranslationCapabilityUpdateListener(Consumer)} instead.
	 */
	@Deprecated
	public void removeOnDeviceTranslationCapabilityUpdateListener(
	    @TranslationSpec.DataFormat int sourceFormat,
	    @TranslationSpec.DataFormat int targetFormat,
	    @NonNull PendingIntent pendingIntent) {
		Objects.requireNonNull(pendingIntent, "pending intent should not be null");
	}

	/**
	 * @removed Use {@link #removeOnDeviceTranslationCapabilityUpdateListener(Consumer)} instead.
	 */
	@Deprecated
	public void removeTranslationCapabilityUpdateListener(
	    @TranslationSpec.DataFormat int sourceFormat,
	    @TranslationSpec.DataFormat int targetFormat,
	    @NonNull PendingIntent pendingIntent) {
		removeOnDeviceTranslationCapabilityUpdateListener(sourceFormat, targetFormat, pendingIntent);
	}

	/**
	 * Returns an immutable PendingIntent which can be used to launch an activity to view/edit
	 * on-device translation settings.
	 *
	 * @return An immutable PendingIntent or {@code null} if one of the reason met:
	 *         <ul>
	 *           <li>Device manufacturer (OEM) does not provide TranslationService.</li>
	 *           <li>The TranslationService doesn't provide the Settings.</li>
	 *         </ul>
	 **/
	@Nullable
	public PendingIntent getOnDeviceTranslationSettingsActivityIntent() {
		return null;
	}

	/**
	 * @removed Use {@link #getOnDeviceTranslationSettingsActivityIntent()} instead.
	 */
	@Deprecated
	@Nullable
	public PendingIntent getTranslationSettingsActivityIntent() {
		return getOnDeviceTranslationSettingsActivityIntent();
	}

	/**
	 * @hide
	 */
	void removeTranslator(int id) {}
}
