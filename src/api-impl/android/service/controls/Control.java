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

package android.service.controls;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.PendingIntent;
import android.content.res.ColorStateList;
import android.graphics.drawable.Icon;
import android.service.controls.templates.ControlTemplate;
import android.util.Log;
import com.android.internal.util.Preconditions;

/**
 * Represents a physical object that can be represented by a {@link ControlTemplate} and whose
 * properties may be modified through a {@link android.service.controls.actions.ControlAction}.
 * AOSP core/java/android/service/controls/Control.java. Parcelable (Binder transport to System UI) is left out.
 */
public final class Control {

	private static final String TAG = "Control";
	private static final int NUM_STATUS = 5;
	public @interface Status {};
	public static final int STATUS_UNKNOWN = 0;
	public static final int STATUS_OK = 1;
	public static final int STATUS_NOT_FOUND = 2;
	public static final int STATUS_ERROR = 3;
	public static final int STATUS_DISABLED = 4;
	private final @NonNull String mControlId;
	private final @DeviceTypes.DeviceType int mDeviceType;
	private final @NonNull CharSequence mTitle;
	private final @NonNull CharSequence mSubtitle;
	private final @Nullable CharSequence mStructure;
	private final @Nullable CharSequence mZone;
	private final @NonNull PendingIntent mAppIntent;
	private final @Nullable Icon mCustomIcon;
	private final @Nullable ColorStateList mCustomColor;
	private final @Status int mStatus;
	private final @NonNull ControlTemplate mControlTemplate;
	private final @NonNull CharSequence mStatusText;
	private final boolean mAuthRequired;
	Control(@NonNull String controlId,
			@DeviceTypes.DeviceType int deviceType,
			@NonNull CharSequence title,
			@NonNull CharSequence subtitle,
			@Nullable CharSequence structure,
			@Nullable CharSequence zone,
			@NonNull PendingIntent appIntent,
			@Nullable Icon customIcon,
			@Nullable ColorStateList customColor,
			@Status int status,
			@NonNull ControlTemplate controlTemplate,
			@NonNull CharSequence statusText,
			boolean authRequired) {
		Preconditions.checkNotNull(controlId);
		Preconditions.checkNotNull(title);
		Preconditions.checkNotNull(subtitle);
		Preconditions.checkNotNull(appIntent);
		Preconditions.checkNotNull(controlTemplate);
		Preconditions.checkNotNull(statusText);
		mControlId = controlId;
		if (!DeviceTypes.validDeviceType(deviceType)) {
			Log.e(TAG, "Invalid device type:" + deviceType);
			mDeviceType = DeviceTypes.TYPE_UNKNOWN;
		} else {
			mDeviceType = deviceType;
		}
		mTitle = title;
		mSubtitle = subtitle;
		mStructure = structure;
		mZone = zone;
		mAppIntent = appIntent;
		mCustomColor = customColor;
		mCustomIcon = customIcon;
		if (status < 0 || status >= NUM_STATUS) {
			mStatus = STATUS_UNKNOWN;
			Log.e(TAG, "Status unknown:" + status);
		} else {
			mStatus = status;
		}
		mControlTemplate = controlTemplate;
		mStatusText = statusText;
		mAuthRequired = authRequired;
	}
	@NonNull
	public String getControlId() {
		return mControlId;
	}
	@DeviceTypes.DeviceType
	public int getDeviceType() {
		return mDeviceType;
	}
	@NonNull
	public CharSequence getTitle() {
		return mTitle;
	}
	@NonNull
	public CharSequence getSubtitle() {
		return mSubtitle;
	}
	@Nullable
	public CharSequence getStructure() {
		return mStructure;
	}
	@Nullable
	public CharSequence getZone() {
		return mZone;
	}
	@NonNull
	public PendingIntent getAppIntent() {
		return mAppIntent;
	}
	@Nullable
	public Icon getCustomIcon() {
		return mCustomIcon;
	}
	@Nullable
	public ColorStateList getCustomColor() {
		return mCustomColor;
	}
	@Status
	public int getStatus() {
		return mStatus;
	}
	@NonNull
	public ControlTemplate getControlTemplate() {
		return mControlTemplate;
	}
	@NonNull
	public CharSequence getStatusText() {
		return mStatusText;
	}
	public boolean isAuthRequired() {
		return mAuthRequired;
	}
	public static final class StatelessBuilder {
		private static final String TAG = "StatelessBuilder";
		private @NonNull String mControlId;
		private @DeviceTypes.DeviceType int mDeviceType = DeviceTypes.TYPE_UNKNOWN;
		private @NonNull CharSequence mTitle = "";
		private @NonNull CharSequence mSubtitle = "";
		private @Nullable CharSequence mStructure;
		private @Nullable CharSequence mZone;
		private @NonNull PendingIntent mAppIntent;
		private @Nullable Icon mCustomIcon;
		private @Nullable ColorStateList mCustomColor;
		public StatelessBuilder(@NonNull String controlId,
				@NonNull PendingIntent appIntent) {
			Preconditions.checkNotNull(controlId);
			Preconditions.checkNotNull(appIntent);
			mControlId = controlId;
			mAppIntent = appIntent;
		}
		public StatelessBuilder(@NonNull Control control) {
			Preconditions.checkNotNull(control);
			mControlId = control.mControlId;
			mDeviceType = control.mDeviceType;
			mTitle = control.mTitle;
			mSubtitle = control.mSubtitle;
			mStructure = control.mStructure;
			mZone = control.mZone;
			mAppIntent = control.mAppIntent;
			mCustomIcon = control.mCustomIcon;
			mCustomColor = control.mCustomColor;
		}
		@NonNull
		public StatelessBuilder setControlId(@NonNull String controlId) {
			Preconditions.checkNotNull(controlId);
			mControlId = controlId;
			return this;
		}
		@NonNull
		public StatelessBuilder setDeviceType(@DeviceTypes.DeviceType int deviceType) {
			if (!DeviceTypes.validDeviceType(deviceType)) {
				Log.e(TAG, "Invalid device type:" + deviceType);
				mDeviceType = DeviceTypes.TYPE_UNKNOWN;
			} else {
				mDeviceType = deviceType;
			}
			return this;
		}
		@NonNull
		public StatelessBuilder setTitle(@NonNull CharSequence title) {
			Preconditions.checkNotNull(title);
			mTitle = title;
			return this;
		}
		@NonNull
		public StatelessBuilder setSubtitle(@NonNull CharSequence subtitle) {
			Preconditions.checkNotNull(subtitle);
			mSubtitle = subtitle;
			return this;
		}
		@NonNull
		public StatelessBuilder setStructure(@Nullable CharSequence structure) {
			mStructure = structure;
			return this;
		}
		@NonNull
		public StatelessBuilder setZone(@Nullable CharSequence zone) {
			mZone = zone;
			return this;
		}
		@NonNull
		public StatelessBuilder setAppIntent(@NonNull PendingIntent appIntent) {
			Preconditions.checkNotNull(appIntent);
			mAppIntent = appIntent;
			return this;
		}
		@NonNull
		public StatelessBuilder setCustomIcon(@Nullable Icon customIcon) {
			mCustomIcon = customIcon;
			return this;
		}
		@NonNull
		public StatelessBuilder setCustomColor(@Nullable ColorStateList customColor) {
			mCustomColor = customColor;
			return this;
		}
		@NonNull
		public Control build() {
			return new Control(mControlId,
					mDeviceType,
					mTitle,
					mSubtitle,
					mStructure,
					mZone,
					mAppIntent,
					mCustomIcon,
					mCustomColor,
					STATUS_UNKNOWN,
					ControlTemplate.NO_TEMPLATE,
					"",
					true );
		}
	}
	public static final class StatefulBuilder {
		private static final String TAG = "StatefulBuilder";
		private @NonNull String mControlId;
		private @DeviceTypes.DeviceType int mDeviceType = DeviceTypes.TYPE_UNKNOWN;
		private @NonNull CharSequence mTitle = "";
		private @NonNull CharSequence mSubtitle = "";
		private @Nullable CharSequence mStructure;
		private @Nullable CharSequence mZone;
		private @NonNull PendingIntent mAppIntent;
		private @Nullable Icon mCustomIcon;
		private @Nullable ColorStateList mCustomColor;
		private @Status int mStatus = STATUS_UNKNOWN;
		private @NonNull ControlTemplate mControlTemplate = ControlTemplate.NO_TEMPLATE;
		private @NonNull CharSequence mStatusText = "";
		private boolean mAuthRequired = true;
		public StatefulBuilder(@NonNull String controlId,
				@NonNull PendingIntent appIntent) {
			Preconditions.checkNotNull(controlId);
			Preconditions.checkNotNull(appIntent);
			mControlId = controlId;
			mAppIntent = appIntent;
		}
		public StatefulBuilder(@NonNull Control control) {
			Preconditions.checkNotNull(control);
			mControlId = control.mControlId;
			mDeviceType = control.mDeviceType;
			mTitle = control.mTitle;
			mSubtitle = control.mSubtitle;
			mStructure = control.mStructure;
			mZone = control.mZone;
			mAppIntent = control.mAppIntent;
			mCustomIcon = control.mCustomIcon;
			mCustomColor = control.mCustomColor;
			mStatus = control.mStatus;
			mControlTemplate = control.mControlTemplate;
			mStatusText = control.mStatusText;
			mAuthRequired = control.mAuthRequired;
		}
		@NonNull
		public StatefulBuilder setControlId(@NonNull String controlId) {
			Preconditions.checkNotNull(controlId);
			mControlId = controlId;
			return this;
		}
		@NonNull
		public StatefulBuilder setDeviceType(@DeviceTypes.DeviceType int deviceType) {
			if (!DeviceTypes.validDeviceType(deviceType)) {
				Log.e(TAG, "Invalid device type:" + deviceType);
				mDeviceType = DeviceTypes.TYPE_UNKNOWN;
			} else {
				mDeviceType = deviceType;
			}
			return this;
		}
		@NonNull
		public StatefulBuilder setTitle(@NonNull CharSequence title) {
			Preconditions.checkNotNull(title);
			mTitle = title;
			return this;
		}
		@NonNull
		public StatefulBuilder setSubtitle(@NonNull CharSequence subtitle) {
			Preconditions.checkNotNull(subtitle);
			mSubtitle = subtitle;
			return this;
		}
		@NonNull
		public StatefulBuilder setStructure(@Nullable CharSequence structure) {
			mStructure = structure;
			return this;
		}
		@NonNull
		public StatefulBuilder setZone(@Nullable CharSequence zone) {
			mZone = zone;
			return this;
		}
		@NonNull
		public StatefulBuilder setAppIntent(@NonNull PendingIntent appIntent) {
			Preconditions.checkNotNull(appIntent);
			mAppIntent = appIntent;
			return this;
		}
		@NonNull
		public StatefulBuilder setCustomIcon(@Nullable Icon customIcon) {
			mCustomIcon = customIcon;
			return this;
		}
		@NonNull
		public StatefulBuilder setCustomColor(@Nullable ColorStateList customColor) {
			mCustomColor = customColor;
			return this;
		}
		@NonNull
		public StatefulBuilder setStatus(@Status int status) {
			if (status < 0 || status >= NUM_STATUS) {
				mStatus = STATUS_UNKNOWN;
				Log.e(TAG, "Status unknown:" + status);
			} else {
				mStatus = status;
			}
			return this;
		}
		@NonNull
		public StatefulBuilder setControlTemplate(@NonNull ControlTemplate controlTemplate) {
			Preconditions.checkNotNull(controlTemplate);
			mControlTemplate = controlTemplate;
			return this;
		}
		@NonNull
		public StatefulBuilder setStatusText(@NonNull CharSequence statusText) {
			Preconditions.checkNotNull(statusText);
			mStatusText = statusText;
			return this;
		}
		@NonNull
		public StatefulBuilder setAuthRequired(boolean authRequired) {
			mAuthRequired = authRequired;
			return this;
		}
		@NonNull
		public Control build() {
			return new Control(mControlId,
					mDeviceType,
					mTitle,
					mSubtitle,
					mStructure,
					mZone,
					mAppIntent,
					mCustomIcon,
					mCustomColor,
					mStatus,
					mControlTemplate,
					mStatusText,
					mAuthRequired);
		}
	}
}
