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
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.service.controls.actions.ControlAction;
import java.util.List;
import java.util.function.Consumer;

/**
 * Service implementation allowing applications to contribute controls to the System UI.
 * AOSP core/java/android/service/controls/ControlsProviderService.java. AOSP onBind() wraps the four
 * abstract methods in an IControlsProvider.Stub for System UI; ATL has no such client, so nothing
 * binds. requestAddControl() needs the config_controlsPackage broadcast and is left out.
 */
public abstract class ControlsProviderService extends Service {

	public static final String SERVICE_CONTROLS = "android.service.controls.ControlsProviderService";

	public static final String META_DATA_PANEL_ACTIVITY = "android.service.controls.META_DATA_PANEL_ACTIVITY";

	public static final String EXTRA_LOCKSCREEN_ALLOW_TRIVIAL_CONTROLS =
			"android.service.controls.extra.LOCKSCREEN_ALLOW_TRIVIAL_CONTROLS";

	/* java.util.concurrent.Flow is not in the ART core library: minimal stand-in so the signatures compile */
	public interface Publisher<T> {
	}

	public @interface ControlsSurface {
	}

	public static final int CONTROLS_SURFACE_ACTIVITY_PANEL = 0;
	public static final int CONTROLS_SURFACE_DREAM = 1;

	public static final String EXTRA_CONTROLS_SURFACE = "android.service.controls.extra.CONTROLS_SURFACE";

	public static final String ACTION_ADD_CONTROL = "android.service.controls.action.ADD_CONTROL";

	public static final String EXTRA_CONTROL = "android.service.controls.extra.CONTROL";

	public static final String CALLBACK_BUNDLE = "CALLBACK_BUNDLE";
	public static final String CALLBACK_TOKEN = "CALLBACK_TOKEN";

	public static final @NonNull String TAG = "ControlsProviderService";

	@NonNull
	public abstract Publisher<Control> createPublisherForAllAvailable();

	@Nullable
	public Publisher<Control> createPublisherForSuggested() {
		return null;
	}

	@NonNull
	public abstract Publisher<Control> createPublisherFor(@NonNull List<String> controlIds);

	public abstract void performControlAction(@NonNull String controlId,
	                                          @NonNull ControlAction action, @NonNull Consumer<Integer> consumer);

	@Override
	public final IBinder onBind(@NonNull Intent intent) {
		/* kein IControlsProvider-Stub, niemand bindet */
		return null;
	}

	@Override
	public final boolean onUnbind(@NonNull Intent intent) {
		return true;
	}
}
