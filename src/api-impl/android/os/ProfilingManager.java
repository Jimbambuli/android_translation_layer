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

package android.os;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.Context;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * Profiling requests (Profiling module, framework/java/android/os/ProfilingManager.java).
 *
 * ATL has no ProfilingService: AOSP's getOrCreateIProfilingServiceLocked() returns null
 * when the service is not registered, so every request is answered with ERROR_UNKNOWN
 * "ProfilingService is not available" through the listener. The trigger methods
 * (system triggered profiling) are not taken.
 */
public final class ProfilingManager {
	public static final int PROFILING_TYPE_JAVA_HEAP_DUMP = 1;
	public static final int PROFILING_TYPE_HEAP_PROFILE = 2;
	public static final int PROFILING_TYPE_STACK_SAMPLING = 3;
	public static final int PROFILING_TYPE_SYSTEM_TRACE = 4;

	/** @hide */
	public static final String KEY_DURATION_MS = "KEY_DURATION_MS";
	/** @hide */
	public static final String KEY_SAMPLING_INTERVAL_BYTES = "KEY_SAMPLING_INTERVAL_BYTES";
	/** @hide */
	public static final String KEY_TRACK_JAVA_ALLOCATIONS = "KEY_TRACK_JAVA_ALLOCATIONS";
	/** @hide */
	public static final String KEY_FREQUENCY_HZ = "KEY_FREQUENCY_HZ";
	/** @hide */
	public static final String KEY_SIZE_KB = "KEY_SIZE_KB";
	/** @hide */
	public static final String KEY_BUFFER_FILL_POLICY = "KEY_BUFFER_FILL_POLICY";
	/** @hide */
	public static final int VALUE_BUFFER_FILL_POLICY_DISCARD = 1;
	/** @hide */
	public static final int VALUE_BUFFER_FILL_POLICY_RING_BUFFER = 2;

	@IntDef(prefix = {"PROFILING_TYPE_"}, value = {
	        PROFILING_TYPE_JAVA_HEAP_DUMP,
	        PROFILING_TYPE_HEAP_PROFILE,
	        PROFILING_TYPE_STACK_SAMPLING,
	        PROFILING_TYPE_SYSTEM_TRACE,
	})
	@Retention(RetentionPolicy.SOURCE)
	public @interface ProfilingType {}

	private static final int TRIGGER_TYPE_NONE = 0; // ProfilingTrigger.TRIGGER_TYPE_NONE

	private final Object mLock = new Object();
	private final Context mContext;
	// AOSP mCallbacks: a request listener has a key, a general listener (registerForAllProfilingResults) has none
	private final ArrayList<Wrapper> mCallbacks = new ArrayList<>();

	private static final class Wrapper {
		final Consumer<ProfilingResult> mListener;
		final boolean mHasKey;

		Wrapper(Consumer<ProfilingResult> listener, boolean hasKey) {
			mListener = listener;
			mHasKey = hasKey;
		}
	}

	/** @hide */
	public ProfilingManager(Context context) {
		mContext = context;
	}

	private static void deliverNoService(Executor executor, Consumer<ProfilingResult> listener, String tag) {
		executor.execute(() -> listener.accept(new ProfilingResult(ProfilingResult.ERROR_UNKNOWN, null, tag,
		        "ProfilingService is not available", TRIGGER_TYPE_NONE)));
	}

	public void requestProfiling(@ProfilingType int profilingType, @Nullable Bundle parameters,
	                             @Nullable String tag, @Nullable CancellationSignal cancellationSignal,
	                             @Nullable Executor executor, @Nullable Consumer<ProfilingResult> listener) {
		synchronized (mLock) {
			if (executor != null && listener != null) {
				mCallbacks.add(new Wrapper(listener, true));
			} else if (mCallbacks.isEmpty()) {
				throw new IllegalArgumentException("No listeners have been registered. Request has been discarded.");
			}
			// no ProfilingService: AOSP calls executor.execute() here (NullPointerException for a null executor)
			deliverNoService(executor, listener, tag);
		}
	}

	public void registerForAllProfilingResults(@NonNull Executor executor, @NonNull Consumer<ProfilingResult> listener) {
		synchronized (mLock) {
			executor.execute(() -> listener.accept(new ProfilingResult(ProfilingResult.ERROR_UNKNOWN, null, null,
			        "Binder exception processing request", TRIGGER_TYPE_NONE)));
		}
	}

	public void unregisterForAllProfilingResults(@Nullable Consumer<ProfilingResult> listener) {
		synchronized (mLock) {
			// AOSP: registerForAllProfilingResults without a service adds no wrapper, but requests did
			if (mCallbacks.isEmpty())
				return;
			if (listener == null) {
				for (int i = mCallbacks.size() - 1; i >= 0; i--) {
					if (!mCallbacks.get(i).mHasKey)
						mCallbacks.remove(i);
				}
			} else {
				for (int i = 0; i < mCallbacks.size(); i++) {
					if (listener.equals(mCallbacks.get(i).mListener)) {
						mCallbacks.remove(i);
						return;
					}
				}
			}
		}
	}
}
