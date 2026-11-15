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
import android.content.Context;
import com.android.internal.util.Preconditions;

/**
 * An abstract input template for a {@link android.service.controls.Control}.
 * AOSP core/java/android/service/controls/templates/ControlTemplate.java. The Bundle (Binder transport)
 * parts getDataBundle()/createTemplateFromBundle() are left out: nothing crosses a process boundary in ATL.
 */
public abstract class ControlTemplate {

	public static final @NonNull ControlTemplate NO_TEMPLATE = new ControlTemplate("") {
		@Override
		public int getTemplateType() {
			return TYPE_NO_TEMPLATE;
		}
	};

	private static final @NonNull ControlTemplate ERROR_TEMPLATE = new ControlTemplate("") {
		@Override
		public int getTemplateType() {
			return TYPE_ERROR;
		}
	};

	public @interface TemplateType {}

	public static final @TemplateType int TYPE_ERROR = -1;
	public static final @TemplateType int TYPE_NO_TEMPLATE = 0;
	public static final @TemplateType int TYPE_TOGGLE = 1;
	public static final @TemplateType int TYPE_RANGE = 2;
	public static final @TemplateType int TYPE_THUMBNAIL = 3;
	public static final @TemplateType int TYPE_TOGGLE_RANGE = 6;
	public static final @TemplateType int TYPE_TEMPERATURE = 7;
	public static final @TemplateType int TYPE_STATELESS = 8;

	private @NonNull final String mTemplateId;

	@NonNull
	public String getTemplateId() {
		return mTemplateId;
	}

	public abstract @TemplateType int getTemplateType();

	ControlTemplate(@NonNull String templateId) {
		Preconditions.checkNotNull(templateId);
		mTemplateId = templateId;
	}

	/* AOSP: hook for subclasses that hold Uri-backed data before the template is sent over Binder */
	public void prepareTemplateForBinder(@NonNull Context context) {}

	@NonNull
	public static ControlTemplate getErrorTemplate() {
		return ERROR_TEMPLATE;
	}

	@NonNull
	public static ControlTemplate getNoTemplateObject() {
		return NO_TEMPLATE;
	}
}
