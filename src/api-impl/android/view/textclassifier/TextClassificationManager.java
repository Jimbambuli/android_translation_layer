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
package android.view.textclassifier;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.Context;

import java.util.Objects;

/**
 * Interface to the text classification service.
 *
 * ATL: AOSP's manager hands out the system's or the on-device TextClassifier. ATL has neither, so,
 * as on a device without a classification service, the manager answers TextClassifier.NO_OP until
 * an app sets its own with setTextClassifier(). Sessions are AOSP's (android-16.0.0_r4) default
 * factory: a TextClassificationSession around getTextClassifier().
 */
public final class TextClassificationManager {
	private final Object mLock = new Object();

	private final Context mContext;
	private TextClassifier mCustomTextClassifier;

	/** AOSP: a session-aware classifier around the current one */
	public TextClassifier createTextClassificationSession(TextClassificationContext classificationContext) {
		Objects.requireNonNull(classificationContext);
		return new TextClassificationSession(classificationContext, getTextClassifier());
	}

	/** @hide */
	public TextClassifier createTextClassificationSession(TextClassificationContext classificationContext,
	                                                      TextClassifier textClassifier) {
		Objects.requireNonNull(classificationContext);
		Objects.requireNonNull(textClassifier);
		return new TextClassificationSession(classificationContext, textClassifier);
	}

	/** @hide */
	public TextClassificationManager(Context context) {
		mContext = Objects.requireNonNull(context);
	}

	/**
	 * Returns the text classifier that was set via {@link #setTextClassifier(TextClassifier)}.
	 * If a customized text classifier was not set, the default text classifier is returned.
	 */
	@NonNull
	public TextClassifier getTextClassifier() {
		synchronized (mLock) {
			if (mCustomTextClassifier != null) {
				return mCustomTextClassifier;
			}
			return TextClassifier.NO_OP;
		}
	}

	/* ATL: AOSP getTextClassifier(int type); ATL has one classifier whatever the type */
	@NonNull
	public TextClassifier getTextClassifier(int type) {
		return getTextClassifier();
	}

	/**
	 * Sets a preferred text classifier.
	 * This overrides the default text classifier that would be returned by
	 * {@link #getTextClassifier()}.
	 *
	 * @param textClassifier the preferred text classifier. Set to {@code null} to reset to the
	 *      default text classifier.
	 */
	public void setTextClassifier(@Nullable TextClassifier textClassifier) {
		synchronized (mLock) {
			mCustomTextClassifier = textClassifier;
		}
	}
}
