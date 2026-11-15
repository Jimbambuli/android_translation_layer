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

package android.service.controls.actions;

import android.annotation.NonNull;
import android.annotation.Nullable;

/**
 * Action sent by a {@link android.service.controls.templates.ToggleTemplate}.
 * AOSP core/java/android/service/controls/actions/BooleanAction.java
 */
public final class BooleanAction extends ControlAction {

	private static final @ActionType int TYPE = TYPE_BOOLEAN;
	private final boolean mNewState;

	public BooleanAction(@NonNull String templateId, boolean newState) {
		this(templateId, newState, null);
	}

	public BooleanAction(@NonNull String templateId, boolean newState,
	                     @Nullable String challengeValue) {
		super(templateId, challengeValue);
		mNewState = newState;
	}

	public boolean getNewState() {
		return mNewState;
	}

	@Override
	public int getActionType() {
		return TYPE;
	}
}
