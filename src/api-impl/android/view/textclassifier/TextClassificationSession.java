/*
 * Copyright (C) 2018 The Android Open Source Project
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

package android.view.textclassifier;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Session-aware TextClassifier (AOSP android-16.0.0_r4 TextClassificationSession).
 *
 * ATL: the delegate is never AOSP's SystemTextClassifier (no remote session to open) and ATL has no
 * SelectionEvent/TextClassifierEvent, so the event helper and the Cleaner that reports the session end are left
 * out; selection and classification pass to the delegate until the session is destroyed, as in AOSP.
 */
final class TextClassificationSession implements TextClassifier {
	private final TextClassifier mDelegate;
	private final TextClassificationContext mClassificationContext;
	private final Object mLock = new Object();
	private boolean mDestroyed;

	TextClassificationSession(TextClassificationContext context, TextClassifier delegate) {
		mClassificationContext = Objects.requireNonNull(context);
		mDelegate = Objects.requireNonNull(delegate);
	}

	@Override
	public TextSelection suggestSelection(TextSelection.Request request) {
		return checkDestroyedAndRun(() -> mDelegate.suggestSelection(request));
	}

	@Override
	public TextClassification classifyText(TextClassification.Request request) {
		return checkDestroyedAndRun(() -> mDelegate.classifyText(request));
	}

	@Override
	public int getMaxGenerateLinksTextLength() {
		return checkDestroyedAndRun(mDelegate::getMaxGenerateLinksTextLength);
	}

	@Override
	public void destroy() {
		synchronized (mLock) {
			mDestroyed = true;
		}
	}

	@Override
	public boolean isDestroyed() {
		synchronized (mLock) {
			return mDestroyed;
		}
	}

	private <T> T checkDestroyedAndRun(Supplier<T> responseSupplier) {
		if (!isDestroyed()) {
			T response = responseSupplier.get();
			synchronized (mLock) {
				if (!mDestroyed) {
					return response;
				}
			}
		}
		throw new IllegalStateException("This TextClassification session has been destroyed");
	}
}
