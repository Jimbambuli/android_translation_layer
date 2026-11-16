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

package android.hardware;

import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;
import java.io.FileDescriptor;
import java.io.IOException;
import java.time.Duration;
import libcore.util.NativeAllocationRegistry;

/* AOSP core/java/android/hardware/SyncFence.java. The native side (android_hardware_SyncFence.c) is AOSP's
 * android_hardware_SyncFence.cpp with ui/Fence's sync_file calls in place of the Fence class. */
public final class SyncFence implements AutoCloseable, Parcelable {

	public static final long SIGNAL_TIME_INVALID = -1;

	public static final long SIGNAL_TIME_PENDING = Long.MAX_VALUE;

	/* AOSP: NativeAllocationRegistry.createNonmalloced(); ART's libcore in ATL only has the constructor */
	private static final NativeAllocationRegistry sRegistry =
		new NativeAllocationRegistry(SyncFence.class.getClassLoader(), nGetDestructor(), 4);

	private long mNativePtr;

	// The destructor for this object
	// This is also used as our internal lock object. Although SyncFence doesn't claim to be
	// thread-safe, the cost of doing so to avoid issues around double-close or similar issues
	// is well worth making.
	private final Runnable mCloser;

	private SyncFence(int fileDescriptor) {
		mNativePtr = nCreate(fileDescriptor);
		mCloser = sRegistry.registerNativeAllocation(this, mNativePtr);
	}

	/* AOSP reads the descriptor with Parcel.readRawFileDescriptor(); ATL's Parcel has readFileDescriptor(),
	 * whose ParcelFileDescriptor hands its descriptor over with detachFd() */
	private SyncFence(Parcel parcel) {
		boolean valid = parcel.readBoolean();
		ParcelFileDescriptor fileDescriptor = null;
		if (valid) {
			fileDescriptor = parcel.readFileDescriptor();
		}
		if (fileDescriptor != null) {
			mNativePtr = nCreate(fileDescriptor.detachFd());
			mCloser = sRegistry.registerNativeAllocation(this, mNativePtr);
		} else {
			mCloser = () -> {};
		}
	}

	/** @hide */
	public SyncFence(long nativeFencePtr) {
		mNativePtr = nativeFencePtr;
		if (nativeFencePtr != 0) {
			mCloser = sRegistry.registerNativeAllocation(this, mNativePtr);
		} else {
			mCloser = () -> {};
		}
	}

	public SyncFence(SyncFence other) {
		this(other.mNativePtr);
		if (mNativePtr != 0) {
			nIncRef(mNativePtr);
		}
	}

	private SyncFence() {
		mCloser = () -> {};
	}

	/** @hide */
	public static SyncFence createEmpty() {
		return new SyncFence();
	}

	/** @hide */
	public static SyncFence create(ParcelFileDescriptor wrapped) {
		return new SyncFence(wrapped.detachFd());
	}

	/** @hide */
	public static SyncFence adopt(int fileDescriptor) {
		return new SyncFence(fileDescriptor);
	}

	/** @hide */
	public ParcelFileDescriptor getFdDup() throws IOException {
		synchronized (mCloser) {
			final int fd = mNativePtr != 0 ? nGetFd(mNativePtr) : -1;
			if (fd == -1) {
				throw new IllegalStateException("Cannot dup the FD of an invalid SyncFence");
			}
			return ParcelFileDescriptor.fromFd(fd);
		}
	}

	public boolean isValid() {
		synchronized (mCloser) {
			return mNativePtr != 0 && nIsValid(mNativePtr);
		}
	}

	public boolean await(Duration timeout) {
		final long timeoutNanos;
		if (timeout.isNegative()) {
			timeoutNanos = -1;
		} else {
			timeoutNanos = timeout.toNanos();
		}
		return await(timeoutNanos);
	}

	public boolean awaitForever() {
		return await(-1);
	}

	private boolean await(long timeoutNanos) {
		synchronized (mCloser) {
			return mNativePtr != 0 && nWait(mNativePtr, timeoutNanos);
		}
	}

	public long getSignalTime() {
		synchronized (mCloser) {
			return mNativePtr != 0 ? nGetSignalTime(mNativePtr) : SIGNAL_TIME_INVALID;
		}
	}

	@Override
	public void close() {
		synchronized (mCloser) {
			if (mNativePtr == 0) {
				return;
			}
			mNativePtr = 0;
			mCloser.run();
		}
	}

	@Override
	public int describeContents() {
		return CONTENTS_FILE_DESCRIPTOR;
	}

	/** @hide */
	public Object getLock() {
		return mCloser;
	}

	/** @hide */
	public long getNativeFence() {
		return mNativePtr;
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		synchronized (mCloser) {
			final int fd = mNativePtr != 0 ? nGetFd(mNativePtr) : -1;
			if (fd == -1) {
				out.writeBoolean(false);
			} else {
				out.writeBoolean(true);
				FileDescriptor temp = new FileDescriptor();
				temp.setInt$(fd);
				out.writeFileDescriptor(temp);
			}
		}
	}

	public static final Parcelable.Creator<SyncFence> CREATOR =
		new Parcelable.Creator<SyncFence>() {
			@Override
			public SyncFence createFromParcel(Parcel in) {
				return new SyncFence(in);
			}

			@Override
			public SyncFence[] newArray(int size) {
				return new SyncFence[size];
			}
		};

	private static native long nGetDestructor();
	private static native long nCreate(int fd);
	private static native boolean nIsValid(long nPtr);
	private static native int nGetFd(long nPtr);
	private static native boolean nWait(long nPtr, long timeout);
	private static native long nGetSignalTime(long nPtr);
	private static native void nIncRef(long nPtr);
}
