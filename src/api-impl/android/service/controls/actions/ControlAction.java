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
import com.android.internal.util.Preconditions;

/**
 * An abstract action indicating a user interaction with a {@link android.service.controls.Control}.
 * AOSP core/java/android/service/controls/actions/ControlAction.java. The Bundle (Binder transport) parts
 * are left out; ModeAction and CommandAction are not ported (not requested).
 */
public abstract class ControlAction {

	public @interface ActionType {};

	public static final @NonNull ControlAction ERROR_ACTION = new ControlAction() {
		@Override
		public int getActionType() {
			return TYPE_ERROR;
		}
	};

	public static final @ActionType int TYPE_ERROR = -1;
	public static final @ActionType int TYPE_BOOLEAN = 1;
	public static final @ActionType int TYPE_FLOAT = 2;
	public static final @ActionType int TYPE_MODE = 4;
	public static final @ActionType int TYPE_COMMAND = 5;

	/* AOSP ControlAction.isValidResponse */
	public static final boolean isValidResponse(@ResponseResult int response) {
		return (response >= 0 && response < NUM_RESPONSE_TYPES);
	}

	private static final int NUM_RESPONSE_TYPES = 6;

	public @interface ResponseResult {};

	public static final @ResponseResult int RESPONSE_UNKNOWN = 0;
	public static final @ResponseResult int RESPONSE_OK = 1;
	public static final @ResponseResult int RESPONSE_FAIL = 2;
	public static final @ResponseResult int RESPONSE_CHALLENGE_ACK = 3;
	public static final @ResponseResult int RESPONSE_CHALLENGE_PIN = 4;
	public static final @ResponseResult int RESPONSE_CHALLENGE_PASSPHRASE = 5;

	public abstract @ActionType int getActionType();

	private final @NonNull String mTemplateId;
	private final @Nullable String mChallengeValue;

	private ControlAction() {
		mTemplateId = "";
		mChallengeValue = null;
	}

	ControlAction(@NonNull String templateId, @Nullable String challengeValue) {
		Preconditions.checkNotNull(templateId);
		mTemplateId = templateId;
		mChallengeValue = challengeValue;
	}

	@NonNull
	public String getTemplateId() {
		return mTemplateId;
	}

	@Nullable
	public String getChallengeValue() {
		return mChallengeValue;
	}

	@NonNull
	public static ControlAction getErrorAction() {
		return ERROR_ACTION;
	}
}
