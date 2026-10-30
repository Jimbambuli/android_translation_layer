/*
 * Copyright (C) 2006 The Android Open Source Project
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

package android.os;

import java.io.FileDescriptor;

/**
 * Utility methods useful for working with files.
 *
 * Only the part of AOSP's FileUtils that other AOSP classes taken over here use.
 */
public final class FileUtils {
	private FileUtils() {
	}

	/**
	 * AOSP asks MediaStore for the original (not transcoded) media file behind fd and returns
	 * null when there is none or on any error. There is no media transcoding here, so there is
	 * never a "modern" fd to switch to.
	 *
	 * {@hide}
	 */
	public static ParcelFileDescriptor convertToModernFd(FileDescriptor fd) {
		return null;
	}
}
