/*
 * Copyright (C) 2015 The Android Open Source Project
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

package android.view;

import android.os.IBinder;

/**
 * {@link DragAndDropPermissions} controls the access permissions for the content URIs associated
 * with a {@link DragEvent}.
 *
 * ATL: the app reads a dropped file through its path (the desktop gave it to the app), there are no URI
 * grants to take or release; the API is AOSP's.
 */
public final class DragAndDropPermissions {
	private final Object mDragAndDropPermissions;

	/** @hide null if the drag carries no content URIs */
	public static DragAndDropPermissions obtain(DragEvent dragEvent) {
		if (dragEvent.getDragAndDropPermissions() == null) {
			return null;
		}
		return new DragAndDropPermissions(dragEvent.getDragAndDropPermissions());
	}

	private DragAndDropPermissions(Object dragAndDropPermissions) {
		mDragAndDropPermissions = dragAndDropPermissions;
	}

	/** @hide take permissions, binding their lifetime to the activity */
	public boolean take(IBinder activityToken) {
		return true;
	}

	/** @hide take permissions transiently */
	public boolean takeTransient() {
		return true;
	}

	/**
	 * Revoke permissions explicitly.
	 */
	public void release() {
	}
}
