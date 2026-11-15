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
import com.android.internal.util.Preconditions;

/**
 * A template for a {@link android.service.controls.Control} supporting toggling and a range.
 * AOSP core/java/android/service/controls/templates/ToggleRangeTemplate.java
 */
public final class ToggleRangeTemplate extends ControlTemplate {

	private static final @TemplateType int TYPE = TYPE_TOGGLE_RANGE;

	private @NonNull final ControlButton mControlButton;
	private @NonNull final RangeTemplate mRangeTemplate;

	public ToggleRangeTemplate(@NonNull String templateId,
	                           @NonNull ControlButton button,
	                           @NonNull RangeTemplate range) {
		super(templateId);
		Preconditions.checkNotNull(button);
		Preconditions.checkNotNull(range);
		mControlButton = button;
		mRangeTemplate = range;
	}

	public ToggleRangeTemplate(@NonNull String templateId,
	                           boolean checked,
	                           @NonNull CharSequence actionDescription,
	                           @NonNull RangeTemplate range) {
		this(templateId, new ControlButton(checked, actionDescription), range);
	}

	@NonNull
	public RangeTemplate getRange() {
		return mRangeTemplate;
	}

	public boolean isChecked() {
		return mControlButton.isChecked();
	}

	@NonNull
	public CharSequence getActionDescription() {
		return mControlButton.getActionDescription();
	}

	@Override
	public int getTemplateType() {
		return TYPE;
	}
}
