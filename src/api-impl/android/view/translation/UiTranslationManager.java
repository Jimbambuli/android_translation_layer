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
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.app.assist.ActivityId;
import android.content.Context;
import android.util.ArrayMap;
import android.view.autofill.AutofillId;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * <p>The {@link UiTranslationManager} class provides ways for apps to use the ui translation
 * function in framework.
 *
 * ATL: AOSP's manager passes the calls to the system's translation service (ITranslationManager), a
 * binder service that ATL does not have and whose methods need the permission MANAGE_UI_TRANSLATION.
 * Without a service nothing is translated: the calls check their arguments as AOSP does and change
 * nothing, the state callbacks are kept but never called. Context returns no UiTranslationManager as
 * a device without the service does, the constructor is for tests.
 */
public final class UiTranslationManager {
	private static final String TAG = "UiTranslationManager";

	/**
	 * The tag which uses for enabling debug log dump. To enable it, we can use command "adb shell
	 * setprop log.tag.UiTranslation DEBUG".
	 *
	 * @hide
	 */
	public static final String LOG_TAG = "UiTranslation";

	/**
	 * The state caller request to disable utranslation,it is no longer need to ui translation.
	 *
	 * @hide
	 */
	public static final int STATE_UI_TRANSLATION_STARTED = 0;
	/**
	 * The state caller request to pause ui translation, it will switch back to the original text.
	 *
	 * @hide
	 */
	public static final int STATE_UI_TRANSLATION_PAUSED = 1;
	/**
	 * The state caller request to resume the paused ui translation, it will show the translated
	 * text again.
	 *
	 * @hide
	 */
	public static final int STATE_UI_TRANSLATION_RESUMED = 2;
	/**
	 * The state the caller requests to enable ui translation.
	 *
	 * @hide
	 */
	public static final int STATE_UI_TRANSLATION_FINISHED = 3;

	/**
	 * @hide
	 */
	@IntDef(prefix = {"STATE__TRANSLATION"},
	        value = {STATE_UI_TRANSLATION_STARTED, STATE_UI_TRANSLATION_PAUSED, STATE_UI_TRANSLATION_RESUMED, STATE_UI_TRANSLATION_FINISHED})
	@Retention(RetentionPolicy.SOURCE)
	public @interface UiTranslationState {}

	// Keys for the data transmitted in the internal UI Translation state callback.
	/** @hide */
	public static final String EXTRA_STATE = "state";
	/** @hide */
	public static final String EXTRA_SOURCE_LOCALE = "source_locale";
	/** @hide */
	public static final String EXTRA_TARGET_LOCALE = "target_locale";
	/** @hide */
	public static final String EXTRA_PACKAGE_NAME = "package_name";

	@NonNull private final Context mContext;

	private final Map<UiTranslationStateCallback, Executor> mCallbacks = new ArrayMap<>();

	/**
	 * @hide
	 */
	public UiTranslationManager(@NonNull Context context) {
		mContext = Objects.requireNonNull(context);
	}

	/**
	 * Request ui translation for a given Views.
	 *
	 * @param sourceSpec {@link TranslationSpec} for the data to be translated.
	 * @param targetSpec {@link TranslationSpec} for the translated data.
	 * @param viewIds A list of the {@link View}'s {@link AutofillId} which needs to be translated
	 * @param activityId the identifier for the Activity which needs ui translation
	 * @throws IllegalArgumentException if the no {@link View}'s {@link AutofillId} in the list
	 * @hide
	 * @removed
	 */
	@Deprecated
	public void startTranslation(@NonNull TranslationSpec sourceSpec, @NonNull TranslationSpec targetSpec,
	                             @NonNull List<AutofillId> viewIds, @NonNull ActivityId activityId) {
		startTranslation(sourceSpec, targetSpec, viewIds, activityId,
		                 new UiTranslationSpec.Builder().setShouldPadContentForCompat(true).build());
	}

	/**
	 * Request ui translation for a given Views.
	 *
	 * @param sourceSpec {@link TranslationSpec} for the data to be translated.
	 * @param targetSpec {@link TranslationSpec} for the translated data.
	 * @param viewIds A list of the {@link View}'s {@link AutofillId} which needs to be translated
	 * @param activityId the identifier for the Activity which needs ui translation
	 * @param uiTranslationSpec configuration for translation of the specified views
	 * @throws IllegalArgumentException if the no {@link View}'s {@link AutofillId} in the list
	 * @hide
	 */
	public void startTranslation(@NonNull TranslationSpec sourceSpec, @NonNull TranslationSpec targetSpec,
	                             @NonNull List<AutofillId> viewIds, @NonNull ActivityId activityId,
	                             @NonNull UiTranslationSpec uiTranslationSpec) {
		Objects.requireNonNull(sourceSpec);
		Objects.requireNonNull(targetSpec);
		Objects.requireNonNull(viewIds);
		Objects.requireNonNull(activityId);
		Objects.requireNonNull(activityId.getToken());
		Objects.requireNonNull(uiTranslationSpec);
		if (viewIds.size() == 0) {
			throw new IllegalArgumentException("Invalid empty views: " + viewIds);
		}
	}

	/**
	 * Request to disable the ui translation. It will destroy all the {@link Translator}s and no
	 * longer to show the translated text.
	 *
	 * @param activityId the identifier for the Activity which needs ui translation
	 * @hide
	 */
	public void finishTranslation(@NonNull ActivityId activityId) {
		Objects.requireNonNull(activityId);
		Objects.requireNonNull(activityId.getToken());
	}

	/**
	 * Request to pause the current ui translation's {@link Translator} which will switch back to
	 * the original language.
	 *
	 * @param activityId the identifier for the Activity which needs ui translation
	 * @hide
	 */
	public void pauseTranslation(@NonNull ActivityId activityId) {
		Objects.requireNonNull(activityId);
		Objects.requireNonNull(activityId.getToken());
	}

	/**
	 * Request to resume the paused ui translation's {@link Translator} which will switch to the
	 * translated language if the text had been translated.
	 *
	 * @param activityId the identifier for the Activity which needs ui translation
	 * @hide
	 */
	public void resumeTranslation(@NonNull ActivityId activityId) {
		Objects.requireNonNull(activityId);
		Objects.requireNonNull(activityId.getToken());
	}

	/**
	 * Registers a callback for the translation state of the UI.
	 *
	 * @param executor the executor the callback runs on
	 * @param callback the callback to register
	 */
	public void registerUiTranslationStateCallback(@NonNull @CallbackExecutor Executor executor,
	                                               @NonNull UiTranslationStateCallback callback) {
		Objects.requireNonNull(executor);
		Objects.requireNonNull(callback);
		synchronized (mCallbacks) {
			if (mCallbacks.containsKey(callback)) {
				return;
			}
			mCallbacks.put(callback, executor);
		}
	}

	/**
	 * Unregisters a callback of the translation state of the UI.
	 *
	 * @param callback the callback to unregister
	 */
	public void unregisterUiTranslationStateCallback(@NonNull UiTranslationStateCallback callback) {
		Objects.requireNonNull(callback);
		synchronized (mCallbacks) {
			mCallbacks.remove(callback);
		}
	}

	/**
	 * Notify apps the translation is finished because {@link android.app.Activity} is finished.
	 *
	 * @param activityDestroyed if the ui translation is finished because of activity destroyed.
	 * @param activityId the identifier for the Activity which needs ui translation
	 * @param componentName the ui translated Activity componentName.
	 *
	 * @hide
	 */
	public void onTranslationFinished(boolean activityDestroyed, ActivityId activityId,
	                                  android.content.ComponentName componentName) {}
}
