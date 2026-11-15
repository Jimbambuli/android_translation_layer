/*
 * Copyright (C) 2010 The Android Open Source Project
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

package android.graphics;

import android.os.ParcelFileDescriptor;
import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Public API and checks after AOSP android-16.0.0_r4
 * graphics/java/android/graphics/BitmapRegionDecoder.java. AOSP keeps a Skia tile index
 * (libs/hwui/jni/BitmapRegionDecoder.cpp); ATL keeps the encoded bytes and decodes the whole
 * image with BitmapFactory for each region, then copies the region out. The result follows
 * SkBitmapRegionCodec: the bitmap has the requested size divided by inSampleSize, parts of the
 * rectangle outside the image stay transparent. options.inBitmap is not reused.
 */
public final class BitmapRegionDecoder {
	private final byte[] mData;
	private final int mWidth;
	private final int mHeight;
	private boolean mRecycled;
	private final Object mNativeLock = new Object();

	private BitmapRegionDecoder(byte[] data, int width, int height) {
		mData = data;
		mWidth = width;
		mHeight = height;
	}

	private static BitmapRegionDecoder create(byte[] data) throws IOException {
		BitmapFactory.Options opts = new BitmapFactory.Options();
		opts.inJustDecodeBounds = true;
		BitmapFactory.decodeByteArray(data, 0, data.length, opts);
		if (opts.outWidth <= 0 || opts.outHeight <= 0)
			throw new IOException("Image format not supported");
		return new BitmapRegionDecoder(data, opts.outWidth, opts.outHeight);
	}

	private static byte[] readAll(InputStream is) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buf = new byte[16 * 1024];
		int n;
		while ((n = is.read(buf)) > 0)
			out.write(buf, 0, n);
		return out.toByteArray();
	}

	public static BitmapRegionDecoder newInstance(byte[] data, int offset, int length, boolean isShareable) throws IOException {
		return newInstance(data, offset, length);
	}

	public static BitmapRegionDecoder newInstance(byte[] data, int offset, int length) throws IOException {
		if ((offset | length) < 0 || data.length < offset + length) {
			throw new ArrayIndexOutOfBoundsException();
		}
		byte[] copy = new byte[length];
		System.arraycopy(data, offset, copy, 0, length);
		return create(copy);
	}

	public static BitmapRegionDecoder newInstance(FileDescriptor fd, boolean isShareable) throws IOException {
		try (FileInputStream is = new FileInputStream(fd)) {
			return create(readAll(is));
		}
	}

	public static BitmapRegionDecoder newInstance(ParcelFileDescriptor pfd) throws IOException {
		return newInstance(pfd.getFileDescriptor(), false);
	}

	public static BitmapRegionDecoder newInstance(InputStream is, boolean isShareable) throws IOException {
		return newInstance(is);
	}

	/* AOSP returns null for a null stream */
	public static BitmapRegionDecoder newInstance(InputStream is) throws IOException {
		if (is == null)
			return null;
		return create(readAll(is));
	}

	public static BitmapRegionDecoder newInstance(String pathName, boolean isShareable) throws IOException {
		return newInstance(pathName);
	}

	public static BitmapRegionDecoder newInstance(String pathName) throws IOException {
		try (InputStream stream = new FileInputStream(pathName)) {
			return create(readAll(stream));
		}
	}

	public Bitmap decodeRegion(Rect rect, BitmapFactory.Options options) {
		synchronized (mNativeLock) {
			checkRecycled("decodeRegion called on recycled region decoder");
			if (rect.right <= 0 || rect.bottom <= 0 || rect.left >= getWidth()
			    || rect.top >= getHeight())
				throw new IllegalArgumentException("rectangle is outside the image");
			int sampleSize = 1;
			Bitmap.Config config = Bitmap.Config.ARGB_8888;
			if (options != null) {
				sampleSize = Math.max(options.inSampleSize, 1);
				if (options.inPreferredConfig != null)
					config = options.inPreferredConfig;
				options.outWidth = -1;
				options.outHeight = -1;
				options.outMimeType = null;
				options.outConfig = null;
			}
			BitmapFactory.Options decodeOpts = new BitmapFactory.Options();
			decodeOpts.inSampleSize = sampleSize;
			decodeOpts.inPreferredConfig = config;
			Bitmap full = BitmapFactory.decodeByteArray(mData, 0, mData.length, decodeOpts);
			if (full == null)
				return null;
			/* SkBitmapRegionCodec: output size is the subset scaled by the sample size */
			int outW = Math.max(rect.width() / sampleSize, 1);
			int outH = Math.max(rect.height() / sampleSize, 1);
			/* the part of the subset inside the image, in sampled coordinates */
			int srcX = Math.max(rect.left, 0) / sampleSize;
			int srcY = Math.max(rect.top, 0) / sampleSize;
			int dstX = (Math.max(rect.left, 0) - rect.left) / sampleSize;
			int dstY = (Math.max(rect.top, 0) - rect.top) / sampleSize;
			int w = Math.min(Math.min(rect.right, mWidth) / sampleSize, full.getWidth()) - srcX;
			int h = Math.min(Math.min(rect.bottom, mHeight) / sampleSize, full.getHeight()) - srcY;
			w = Math.min(w, outW - dstX);
			h = Math.min(h, outH - dstY);
			Bitmap out;
			if (dstX == 0 && dstY == 0 && w == outW && h == outH) {
				out = Bitmap.createBitmap(full, srcX, srcY, outW, outH);
			} else {
				out = Bitmap.createBitmap(outW, outH, config);
				if (w > 0 && h > 0) {
					int[] pixels = new int[w * h];
					full.getPixels(pixels, 0, w, srcX, srcY, w, h);
					out.setPixels(pixels, 0, w, dstX, dstY, w, h);
				}
			}
			if (out != full)
				full.recycle();
			if (options != null) {
				options.outWidth = out.getWidth();
				options.outHeight = out.getHeight();
				options.outConfig = out.getConfig();
			}
			return out;
		}
	}

	public int getWidth() {
		synchronized (mNativeLock) {
			checkRecycled("getWidth called on recycled region decoder");
			return mWidth;
		}
	}

	public int getHeight() {
		synchronized (mNativeLock) {
			checkRecycled("getHeight called on recycled region decoder");
			return mHeight;
		}
	}

	public void recycle() {
		synchronized (mNativeLock) {
			mRecycled = true;
		}
	}

	public final boolean isRecycled() {
		return mRecycled;
	}

	private void checkRecycled(String errorMessage) {
		if (mRecycled) {
			throw new IllegalStateException(errorMessage);
		}
	}
}
