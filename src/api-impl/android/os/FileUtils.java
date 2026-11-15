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

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Executor;

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

	/* AOSP FileUtils.contains: file lies in dir or is dir; both resolved by the caller */
	public static boolean contains(File dir, File file) {
		if (dir == null || file == null) return false;
		return contains(dir.getAbsolutePath(), file.getAbsolutePath());
	}

	public static boolean contains(String dirPath, String filePath) {
		if (dirPath.equals(filePath)) {
			return true;
		}
		if (!dirPath.endsWith("/")) {
			dirPath += "/";
		}
		return filePath.startsWith(dirPath);
	}

	/* AOSP FileUtils.ProgressListener */
	public interface ProgressListener {
		void onProgress(long progress);
	}

	private static final long COPY_CHECKPOINT_BYTES = 524288;

	public static long copy(InputStream in, OutputStream out) throws IOException {
		return copy(in, out, null, null, null);
	}

	/* AOSP FileUtils.copy(...) -> copyInternalUserspace(): the kernel shortcuts (copy_file_range, sendfile) are
	 * only speed-ups, the result is the same */
	public static long copy(InputStream in, OutputStream out, CancellationSignal signal, Executor executor,
	                        ProgressListener listener) throws IOException {
		long progress = 0;
		long checkpoint = 0;
		byte[] buffer = new byte[8192];
		int t;
		while ((t = in.read(buffer)) != -1) {
			out.write(buffer, 0, t);
			progress += t;
			checkpoint += t;
			if (checkpoint >= COPY_CHECKPOINT_BYTES) {
				if (signal != null)
					signal.throwIfCanceled();
				if (executor != null && listener != null) {
					final long progressSnapshot = progress;
					executor.execute(() -> listener.onProgress(progressSnapshot));
				}
				checkpoint = 0;
			}
		}
		if (executor != null && listener != null) {
			final long progressSnapshot = progress;
			executor.execute(() -> listener.onProgress(progressSnapshot));
		}
		return progress;
	}
}
