/*
 * Copyright 2018 The Android Open Source Project
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

package android.view.inspector;

/**
 * An interface for companion objects used to inspect views (AOSP android-16.0.0_r4 InspectionCompanion).
 */
public interface InspectionCompanion<T> {
	void mapProperties(PropertyMapper propertyMapper);

	void readProperties(T inspectable, PropertyReader propertyReader);

	/** Thrown by readProperties(Object, PropertyReader) if called before mapProperties(PropertyMapper). */
	class UninitializedPropertyMapException extends RuntimeException {
		public UninitializedPropertyMapException() {
			super("Unable to read properties of an inspectable before mapping their IDs.");
		}
	}
}
