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

package android.os;

import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import dalvik.system.VMRuntime;
import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.DirectByteBuffer;
import java.nio.NioUtils;
import sun.misc.Cleaner;

/**
 * SharedMemory enables the creation, mapping, and protection control over anonymous shared memory.
 * (AOSP android-14 SharedMemory; the natives follow the host ashmem of libcutils, see below)
 */
public final class SharedMemory implements Parcelable, Closeable {

	private final FileDescriptor mFileDescriptor;
	private final int mSize;
	private final MemoryRegistration mMemoryRegistration;
	private Cleaner mCleaner;

	private SharedMemory(FileDescriptor fd) {
		// This constructor is only used internally so it should be impossible to hit any of the
		// exceptions unless something goes horribly wrong.
		if (fd == null) {
			throw new IllegalArgumentException("Unable to create SharedMemory from a null FileDescriptor");
		}
		if (!fd.valid()) {
			throw new IllegalArgumentException("Unable to create SharedMemory from closed FileDescriptor");
		}
		mFileDescriptor = fd;
		mSize = nGetSize(mFileDescriptor);
		if (mSize <= 0) {
			throw new IllegalArgumentException("FileDescriptor is not a valid ashmem fd");
		}

		mMemoryRegistration = new MemoryRegistration(mSize);
		mCleaner = Cleaner.create(mFileDescriptor, new Closer(mFileDescriptor.getInt$(), mMemoryRegistration));
	}

	public static SharedMemory create(String name, int size) throws ErrnoException {
		if (size <= 0) {
			throw new IllegalArgumentException("Size must be greater than zero");
		}
		return new SharedMemory(nCreate(name, size));
	}

	private void checkOpen() {
		if (!mFileDescriptor.valid()) {
			throw new IllegalStateException("SharedMemory is closed");
		}
	}

	public static SharedMemory fromFileDescriptor(ParcelFileDescriptor fd) {
		FileDescriptor f = new FileDescriptor();
		f.setInt$(fd.detachFd());
		return new SharedMemory(f);
	}

	private static final int PROT_MASK = OsConstants.PROT_READ | OsConstants.PROT_WRITE | OsConstants.PROT_EXEC | OsConstants.PROT_NONE;

	private static void validateProt(int prot) {
		if ((prot & ~PROT_MASK) != 0) {
			throw new IllegalArgumentException("Invalid prot value");
		}
	}

	public boolean setProtect(int prot) {
		checkOpen();
		validateProt(prot);
		int errno = nSetProt(mFileDescriptor, prot);
		return errno == 0;
	}

	public FileDescriptor getFileDescriptor() {
		return mFileDescriptor;
	}

	public int getFd() {
		return mFileDescriptor.getInt$();
	}

	public int getSize() {
		checkOpen();
		return mSize;
	}

	public ByteBuffer mapReadWrite() throws ErrnoException {
		return map(OsConstants.PROT_READ | OsConstants.PROT_WRITE, 0, mSize);
	}

	public ByteBuffer mapReadOnly() throws ErrnoException {
		return map(OsConstants.PROT_READ, 0, mSize);
	}

	public ByteBuffer map(int prot, int offset, int length) throws ErrnoException {
		checkOpen();
		validateProt(prot);
		if (offset < 0) {
			throw new IllegalArgumentException("Offset must be >= 0");
		}
		if (length <= 0) {
			throw new IllegalArgumentException("Length must be > 0");
		}
		if (offset + length > mSize) {
			throw new IllegalArgumentException("offset + length must not exceed getSize()");
		}
		long address = Os.mmap(0, length, prot, OsConstants.MAP_SHARED, mFileDescriptor, offset);
		boolean readOnly = (prot & OsConstants.PROT_WRITE) == 0;
		Runnable unmapper = new Unmapper(address, length, mMemoryRegistration.acquire());
		return new DirectByteBuffer(length, address, mFileDescriptor, unmapper, readOnly);
	}

	public static void unmap(ByteBuffer buffer) {
		if (buffer instanceof DirectByteBuffer) {
			NioUtils.freeDirectBuffer(buffer);
		} else {
			throw new IllegalArgumentException("ByteBuffer wasn't created by #map(int, int, int); can't unmap");
		}
	}

	@Override
	public void close() {
		mFileDescriptor.setInt$(-1);
		if (mCleaner != null) {
			mCleaner.clean();
			mCleaner = null;
		}
	}

	@Override
	public int describeContents() {
		return CONTENTS_FILE_DESCRIPTOR;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		checkOpen();
		dest.writeFileDescriptor(mFileDescriptor);
	}

	public ParcelFileDescriptor getFdDup() throws IOException {
		return ParcelFileDescriptor.dup(mFileDescriptor);
	}

	public static final Parcelable.Creator<SharedMemory> CREATOR = new Parcelable.Creator<SharedMemory>() {
		@Override
		public SharedMemory createFromParcel(Parcel source) {
			ParcelFileDescriptor pfd = source.readFileDescriptor();
			return pfd != null ? fromFileDescriptor(pfd) : null;
		}

		@Override
		public SharedMemory[] newArray(int size) {
			return new SharedMemory[size];
		}
	};

	/**
	 * Cleaner that closes the FD
	 */
	private static final class Closer implements Runnable {
		private int mFd;
		private MemoryRegistration mMemoryReference;

		private Closer(int fd, MemoryRegistration memoryReference) {
			mFd = fd;
			mMemoryReference = memoryReference;
		}

		@Override
		public void run() {
			try {
				FileDescriptor fd = new FileDescriptor();
				fd.setInt$(mFd);
				Os.close(fd);
			} catch (ErrnoException e) { /* swallow error */
			}
			mMemoryReference.release();
			mMemoryReference = null;
		}
	}

	/**
	 * Cleaner that munmap regions
	 */
	private static final class Unmapper implements Runnable {
		private long mAddress;
		private int mSize;
		private MemoryRegistration mMemoryReference;

		private Unmapper(long address, int size, MemoryRegistration memoryReference) {
			mAddress = address;
			mSize = size;
			mMemoryReference = memoryReference;
		}

		@Override
		public void run() {
			try {
				Os.munmap(mAddress, mSize);
			} catch (ErrnoException e) { /* swallow exception */
			}
			mMemoryReference.release();
			mMemoryReference = null;
		}
	}

	/**
	 * Helper class that ensures that the native allocation pressure against the VM heap stays
	 * active until the FD is closed as well as all mappings from that FD are closed.
	 */
	private static final class MemoryRegistration {
		private int mSize;
		private int mReferenceCount;

		private MemoryRegistration(int size) {
			mSize = size;
			mReferenceCount = 1;
			VMRuntime.getRuntime().registerNativeAllocation(mSize);
		}

		public synchronized MemoryRegistration acquire() {
			mReferenceCount++;
			return this;
		}

		public synchronized void release() {
			mReferenceCount--;
			if (mReferenceCount == 0) {
				VMRuntime.getRuntime().registerNativeFree(mSize);
			}
		}
	}

	/* The natives of AOSP (core/jni/android_os_SharedMemory.cpp) call ashmem_create_region,
	 * ashmem_get_size_region and ashmem_set_prot_region of libcutils. ATL runs on the host variant
	 * (art_standalone system/core/libcutils/ashmem-host.c), done here in Java with the same steps. */

	/* ashmem-host.c ashmem_create_region: an unlinked temporary file of the requested size */
	private static FileDescriptor nCreate(String name, int size) throws ErrnoException {
		String template = "/tmp/android-ashmem-" + Os.getpid() + "-";
		java.io.File file;
		try {
			file = java.io.File.createTempFile(template.substring(5), null, new java.io.File("/tmp"));
		} catch (IOException e) {
			throw new ErrnoException("SharedMemory_create", OsConstants.EIO);
		}
		FileDescriptor fd = null;
		try {
			fd = Os.open(file.getPath(), OsConstants.O_RDWR | OsConstants.O_CLOEXEC, 0600);
			Os.ftruncate(fd, size);
			Os.remove(file.getPath());
			return fd;
		} catch (ErrnoException e) {
			if (fd != null) {
				try {
					Os.close(fd);
				} catch (ErrnoException e2) {
				}
			}
			file.delete();
			throw new ErrnoException("SharedMemory_create", e.errno);
		}
	}

	/* ashmem-host.c ashmem_get_size_region: only an unlinked regular file counts */
	private static int nGetSize(FileDescriptor fd) {
		try {
			android.system.StructStat st = Os.fstat(fd);
			if (!(st.st_nlink == 0 && OsConstants.S_ISREG(st.st_mode)))
				return -1;
			return (int)st.st_size;
		} catch (ErrnoException e) {
			return -1;
		}
	}

	/* ashmem-host.c ashmem_set_prot_region: no protection on the host, always succeeds */
	private static int nSetProt(FileDescriptor fd, int prot) {
		return 0;
	}
}
