/*
 * Copyright (C) 2021 The Android Open Source Project
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

package android.system;

import java.lang.ref.Cleaner;

/**
 * SystemCleaner provides a shared {@link Cleaner} for the process (AOSP SystemCleaner: the
 * runtime's CleanerFactory; ATL runs on the JVM, so one shared Cleaner is created here).
 */
public final class SystemCleaner {
	private static final Cleaner CLEANER = Cleaner.create();

	private SystemCleaner() {}

	/** Returns the shared {@link Cleaner}. */
	public static Cleaner cleaner() {
		return CLEANER;
	}
}
