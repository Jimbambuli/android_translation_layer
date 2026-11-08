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
import android.content.Context;
import android.os.CancellationSignal;

import java.io.PrintWriter;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * The {@link Translator} for translation, defined by a {@link TranslationContext}.
 *
 * ATL: AOSP's Translator sends the requests to the translation service. ATL has none and
 * TranslationManager creates no Translator, so an instance only exists for tests: it answers every
 * request with TRANSLATION_STATUS_UNKNOWN_ERROR and has AOSP's destroy() and isDestroyed().
 */
public class Translator {
	private final Object mLock = new Object();

	private final int mId;
	@NonNull private final Context mContext;
	@NonNull private final TranslationContext mTranslationContext;
	@NonNull private final TranslationManager mManager;
	private boolean mDestroyed;

	/**
	 * Name of the extra containing the binder to the service.
	 * @hide
	 */
	public static final String EXTRA_SERVICE_BINDER = "binder";

	/**
	 * Name of the extra containing the session id.
	 * @hide
	 */
	public static final String EXTRA_SESSION_ID = "sessionId";

	/**
	 * @hide
	 */
	public Translator(@NonNull Context context, @NonNull TranslationContext translationContext, int sessionId,
	                  @NonNull TranslationManager translationManager) {
		mContext = context;
		mTranslationContext = translationContext;
		mId = sessionId;
		mManager = translationManager;
	}

	/** @hide */
	public TranslationContext getTranslationContext() {
		return mTranslationContext;
	}

	/** @hide */
	public int getTranslatorId() {
		return mId;
	}

	/** @hide */
	public void dump(@NonNull String prefix, @NonNull PrintWriter pw) {
		pw.print(prefix);
		pw.print("translationContext: ");
		pw.println(mTranslationContext);
	}

	/**
	 * Requests a translation for the provided {@link TranslationRequest} using the Translator's
	 * source spec and destination spec.
	 *
	 * @param request {@link TranslationRequest} request to translate.
	 *
	 * @throws IllegalStateException if this Translator session was destroyed when called.
	 *
	 * @removed use {@link #translate(TranslationRequest, CancellationSignal, Executor, Consumer)}
	 * instead.
	 */
	@Deprecated
	@Nullable
	public void translate(@NonNull TranslationRequest request,
	                      @NonNull @CallbackExecutor Executor executor, @NonNull Consumer<TranslationResponse> callback) {
		translate(request, null, executor, callback);
	}

	/**
	 * Requests a translation for the provided {@link TranslationRequest} using the Translator's
	 * source spec and destination spec.
	 *
	 * @param request {@link TranslationRequest} request to translate.
	 * @param cancellationSignal signal to cancel the operation in progress.
	 * @param executor Executor to run callback operations
	 * @param callback {@link Consumer} to receive the translation response. Multiple responses may
	 *                                 be received if {@link TranslationRequest#FLAG_PARTIAL_RESPONSES} is set.
	 *
	 * @throws IllegalStateException if this Translator session was destroyed when called.
	 */
	@Nullable
	public void translate(@NonNull TranslationRequest request,
	                      @Nullable CancellationSignal cancellationSignal,
	                      @NonNull @CallbackExecutor Executor executor,
	                      @NonNull Consumer<TranslationResponse> callback) {
		Objects.requireNonNull(request, "Translation request cannot be null");
		Objects.requireNonNull(executor, "Executor cannot be null");
		Objects.requireNonNull(callback, "Callback cannot be null");

		if (isDestroyed()) {
			throw new IllegalStateException("This translator has been destroyed");
		}

		TranslationResponse response =
		    new TranslationResponse.Builder(TranslationResponse.TRANSLATION_STATUS_UNKNOWN_ERROR).build();
		executor.execute(() -> callback.accept(response));
	}

	/**
	 * Destroy this Translator.
	 */
	public void destroy() {
		synchronized (mLock) {
			if (mDestroyed) {
				return;
			}
			mDestroyed = true;
			mManager.removeTranslator(mId);
		}
	}

	/**
	 * Returns whether or not this Translator has been destroyed.
	 *
	 * @see #destroy()
	 */
	public boolean isDestroyed() {
		synchronized (mLock) {
			return mDestroyed;
		}
	}
}
