/*
 * Copyright (C) 2012 The Android Open Source Project
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

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;

/* AOSP os/SELinux.java on a device whose kernel has no SELinux: android_os_SELinux.cpp answers every call
 * with false / null when SELinux is disabled (isSELinuxDisabled) */
public class SELinux {
	public static final String fileSelabelLookup(String path) {
		return null;
	}

	public static final boolean isSELinuxEnabled() {
		return false;
	}

	public static final boolean isSELinuxEnforced() {
		return false;
	}

	public static final boolean setFSCreateContext(String context) {
		return false;
	}

	public static final boolean setFileContext(String path, String context) {
		return false;
	}

	public static final String getFileContext(String path) {
		return null;
	}

	public static final String getPeerContext(FileDescriptor fd) {
		return null;
	}

	public static final String getFileContext(FileDescriptor fd) {
		return null;
	}

	public static final String getContext() {
		return null;
	}

	public static final String getPidContext(int pid) {
		return null;
	}

	public static final boolean checkSELinuxAccess(String scon, String tcon, String tclass, String perm) {
		return false;
	}

	public static boolean restorecon(String pathname) throws NullPointerException {
		if (pathname == null) {
			throw new NullPointerException();
		}
		return false;
	}

	public static boolean restorecon(File file) throws NullPointerException {
		try {
			file.getCanonicalPath();
			return false;
		} catch (IOException e) {
			return false;
		}
	}

	public static boolean restoreconRecursive(File file) {
		return false;
	}
}
