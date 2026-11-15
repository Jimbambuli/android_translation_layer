/*
 * Copyright (C) 2024 The Android Open Source Project
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

package android.window;

import android.annotation.NonNull;
import android.app.Activity;
import android.util.ArrayMap;

import java.lang.ref.WeakReference;

/* AOSP android16 frameworks/base/core/java/android/window/SystemOnBackInvokedCallbacks.java */
public final class SystemOnBackInvokedCallbacks {
	private static final OverrideCallbackFactory<Activity> sMoveTaskToBackFactory = new MoveTaskToBackCallbackFactory();
	private static final OverrideCallbackFactory<Activity> sFinishAndRemoveTaskFactory = new FinishAndRemoveTaskCallbackFactory();

	private SystemOnBackInvokedCallbacks() {
		throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
	}

	@NonNull
	public static OnBackInvokedCallback moveTaskToBackCallback(@NonNull Activity activity) {
		return sMoveTaskToBackFactory.getOverrideCallback(activity);
	}

	@NonNull
	public static OnBackInvokedCallback finishAndRemoveTaskCallback(@NonNull Activity activity) {
		return sFinishAndRemoveTaskFactory.getOverrideCallback(activity);
	}

	private abstract static class OverrideCallbackFactory<TYPE> {
		private final ArrayMap<WeakReference<TYPE>, WeakReference<SystemOverrideOnBackInvokedCallback>> mObjectMap = new ArrayMap<>();

		protected abstract SystemOverrideOnBackInvokedCallback createCallback(@NonNull TYPE context);

		@NonNull
		SystemOverrideOnBackInvokedCallback getOverrideCallback(@NonNull TYPE object) {
			if (object == null) {
				throw new NullPointerException("Input object cannot be null");
			}
			synchronized (mObjectMap) {
				WeakReference<SystemOverrideOnBackInvokedCallback> callback = null;
				for (int i = mObjectMap.size() - 1; i >= 0; --i) {
					final WeakReference<TYPE> next = mObjectMap.keyAt(i);
					if (next.get() == object) {
						callback = mObjectMap.get(next);
						break;
					}
				}
				if (callback != null) {
					return callback.get();
				}
				final SystemOverrideOnBackInvokedCallback contextCallback = createCallback(object);
				if (contextCallback != null) {
					mObjectMap.put(new WeakReference<>(object), new WeakReference<>(contextCallback));
				}
				return contextCallback;
			}
		}
	}

	private static class MoveTaskToBackCallbackFactory extends OverrideCallbackFactory<Activity> {
		@Override
		protected SystemOverrideOnBackInvokedCallback createCallback(Activity activity) {
			final WeakReference<Activity> activityRef = new WeakReference<>(activity);
			return new SystemOverrideOnBackInvokedCallback() {
				@Override
				public void onBackInvoked() {
					if (activityRef.get() != null) {
						activityRef.get().moveTaskToBack(true /* nonRoot */);
					}
				}

				@Override
				public int overrideBehavior() {
					return OVERRIDE_MOVE_TASK_TO_BACK;
				}
			};
		}
	}

	private static class FinishAndRemoveTaskCallbackFactory extends OverrideCallbackFactory<Activity> {
		@Override
		protected SystemOverrideOnBackInvokedCallback createCallback(Activity activity) {
			final WeakReference<Activity> activityRef = new WeakReference<>(activity);
			return new SystemOverrideOnBackInvokedCallback() {
				@Override
				public void onBackInvoked() {
					if (activityRef.get() != null) {
						activityRef.get().finishAndRemoveTask();
					}
				}

				@Override
				public int overrideBehavior() {
					return OVERRIDE_FINISH_AND_REMOVE_TASK;
				}
			};
		}
	}
}
