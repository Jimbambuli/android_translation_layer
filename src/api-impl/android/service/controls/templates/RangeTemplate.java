/*
 * Copyright (C) 2019 The Android Open Source Project
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

package android.service.controls.templates;

import android.annotation.NonNull;
import android.annotation.Nullable;

/**
 * A template for a {@link android.service.controls.Control} with inline changing of values in a range.
 * AOSP core/java/android/service/controls/templates/RangeTemplate.java
 */
public final class RangeTemplate extends ControlTemplate {

	private static final @TemplateType int TYPE = TYPE_RANGE;

	private final float mMinValue;
	private final float mMaxValue;
	private final float mCurrentValue;
	private final float mStepValue;
	private final @NonNull CharSequence mFormatString;

	public RangeTemplate(@NonNull String templateId,
	                     float minValue,
	                     float maxValue,
	                     float currentValue,
	                     float stepValue,
	                     @Nullable CharSequence formatString) {
		super(templateId);
		mMinValue = minValue;
		mMaxValue = maxValue;
		mCurrentValue = currentValue;
		mStepValue = stepValue;
		if (formatString != null) {
			mFormatString = formatString;
		} else {
			mFormatString = "%.1f";
		}
		validate();
	}

	public float getMinValue() {
		return mMinValue;
	}

	public float getMaxValue() {
		return mMaxValue;
	}

	public float getCurrentValue() {
		return mCurrentValue;
	}

	public float getStepValue() {
		return mStepValue;
	}

	@NonNull
	public CharSequence getFormatString() {
		return mFormatString;
	}

	@Override
	public int getTemplateType() {
		return TYPE;
	}

	/* AOSP RangeTemplate.validate */
	private void validate() {
		if (Float.compare(mMinValue, mMaxValue) > 0) {
			throw new IllegalArgumentException(
					String.format("minValue=%f > maxValue=%f", mMinValue, mMaxValue));
		}
		if (Float.compare(mMinValue, mCurrentValue) > 0) {
			throw new IllegalArgumentException(
					String.format("minValue=%f > currentValue=%f", mMinValue, mCurrentValue));
		}
		if (Float.compare(mCurrentValue, mMaxValue) > 0) {
			throw new IllegalArgumentException(
					String.format("currentValue=%f > maxValue=%f", mCurrentValue, mMaxValue));
		}
		if (mStepValue <= 0) {
			throw new IllegalArgumentException(String.format("stepValue=%f <= 0", mStepValue));
		}
	}
}
