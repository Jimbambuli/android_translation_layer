/*
 * Copyright (C) 2017 The Android Open Source Project
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

package android.hardware;

import java.nio.channels.Channel;
import java.util.concurrent.atomic.AtomicBoolean;

/* AOSP SensorDirectChannel; created by SensorManager.createDirectChannel() */
public final class SensorDirectChannel implements Channel {
	// shared memory types
	public static final int TYPE_MEMORY_FILE = 1;
	public static final int TYPE_HARDWARE_BUFFER = 2;

	// sensor rate levels
	public static final int RATE_STOP = 0;
	public static final int RATE_NORMAL = 1; // 50Hz
	public static final int RATE_FAST = 2; // ~200Hz
	public static final int RATE_VERY_FAST = 3; // ~800Hz

	@Override
	public boolean isOpen() {
		return !mClosed.get();
	}

	/** @hide */
	@Deprecated
	public boolean isValid() {
		return isOpen();
	}

	@Override
	public void close() {
		if (mClosed.compareAndSet(false, true)) {
			mManager.destroyDirectChannel(this);
		}
	}

	public int configure(Sensor sensor, int rateLevel) {
		return mManager.configureDirectChannelImpl(this, sensor, rateLevel);
	}

	SensorDirectChannel(SensorManager manager, int id, int type, long size) {
		mManager = manager;
		mNativeHandle = id;
		mType = type;
		mSize = size;
	}

	int getNativeHandle() {
		return mNativeHandle;
	}

	@Override
	protected void finalize() throws Throwable {
		try {
			close();
		} finally {
			super.finalize();
		}
	}

	private final AtomicBoolean mClosed = new AtomicBoolean();
	private final SensorManager mManager;
	private final int mNativeHandle;
	private final long mSize;
	private final int mType;
}
