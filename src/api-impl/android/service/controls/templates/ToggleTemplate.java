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
 * A template for a {@link android.service.controls.Control} with successive states.
 * AOSP core/java/android/service/controls/templates/ToggleTemplate.java
 */
public final class ToggleTemplate extends ControlTemplate {

	private static final @TemplateType int TYPE = TYPE_TOGGLE;
	private final @NonNull ControlButton mButton;

	public ToggleTemplate(@NonNull String templateId, @NonNull ControlButton button) {
		super(templateId);
		Preconditions.checkNotNull(button);
		mButton = button;
	}

	public boolean isChecked() {
		return mButton.isChecked();
	}

	@NonNull
	public CharSequence getContentDescription() {
		return mButton.getActionDescription();
	}

	@Override
	public int getTemplateType() {
		return TYPE;
	}
}
