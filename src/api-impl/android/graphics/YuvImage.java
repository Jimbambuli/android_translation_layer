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

import java.io.OutputStream;

/**
 * After AOSP android-16.0.0_r4 graphics/java/android/graphics/YuvImage.java (constructors,
 * argument checks, strides/offsets, adjustRectangle). AOSP's nativeCompressToJpeg
 * (libs/hwui/jni/YuvToJpegEncoder.cpp) feeds the YCbCr samples to libjpeg; ATL converts them
 * to RGB with the JFIF (full range BT.601) equations and encodes with Bitmap.compress(JPEG),
 * whose encoder converts back to JFIF YCbCr. compressToJpegR (Ultra HDR) is not provided.
 */
public class YuvImage {
	private int mFormat;
	private byte[] mData;
	private int[] mStrides;
	private int mWidth;
	private int mHeight;
	private ColorSpace mColorSpace;

	private final static String[] sSupportedFormats =
	    {"NV21", "YUY2", "YCBCR_P010", "YUV_420_888"};

	private static String printSupportedFormats() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < sSupportedFormats.length; ++i) {
			sb.append(sSupportedFormats[i]);
			if (i != sSupportedFormats.length - 1) {
				sb.append(", ");
			}
		}
		return sb.toString();
	}

	public YuvImage(byte[] yuv, int format, int width, int height, int[] strides) {
		this(yuv, format, width, height, strides, ColorSpace.get(ColorSpace.Named.SRGB));
	}

	public YuvImage(byte[] yuv, int format, int width, int height,
			int[] strides, ColorSpace colorSpace) {
		if (format != ImageFormat.NV21 &&
		    format != ImageFormat.YUY2 &&
		    format != ImageFormat.YCBCR_P010 &&
		    format != ImageFormat.YUV_420_888) {
			throw new IllegalArgumentException(
			    "only supports the following ImageFormat:" + printSupportedFormats());
		}
		if (width <= 0 || height <= 0) {
			throw new IllegalArgumentException(
			    "width and height must large than 0");
		}
		if (yuv == null) {
			throw new IllegalArgumentException("yuv cannot be null");
		}
		if (colorSpace == null) {
			throw new IllegalArgumentException("ColorSpace cannot be null");
		}
		if (strides == null) {
			mStrides = calculateStrides(width, format);
		} else {
			mStrides = strides;
		}
		mData = yuv;
		mFormat = format;
		mWidth = width;
		mHeight = height;
		mColorSpace = colorSpace;
	}

	public boolean compressToJpeg(Rect rectangle, int quality, OutputStream stream) {
		if (mFormat != ImageFormat.NV21 && mFormat != ImageFormat.YUY2) {
			throw new IllegalArgumentException(
			    "Only ImageFormat.NV21 and ImageFormat.YUY2 are supported.");
		}
		if (mColorSpace.getId() != ColorSpace.Named.SRGB.ordinal()) {
			throw new IllegalArgumentException("Only SRGB color space is supported.");
		}
		Rect wholeImage = new Rect(0, 0, mWidth, mHeight);
		if (!wholeImage.contains(rectangle)) {
			throw new IllegalArgumentException(
			    "rectangle is not inside the image");
		}
		if (quality < 0 || quality > 100) {
			throw new IllegalArgumentException("quality must be 0..100");
		}
		if (stream == null) {
			throw new IllegalArgumentException("stream cannot be null");
		}
		adjustRectangle(rectangle);
		int[] offsets = calculateOffsets(rectangle.left, rectangle.top);
		int width = rectangle.width();
		int height = rectangle.height();
		if (width <= 0 || height <= 0)
			return false;
		int[] argb = new int[width * height];
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int luma, cb, cr;
				if (mFormat == ImageFormat.NV21) {
					luma = mData[offsets[0] + y * mStrides[0] + x] & 0xff;
					int c = offsets[1] + (y / 2) * mStrides[1] + (x / 2) * 2;
					cr = mData[c] & 0xff;
					cb = mData[c + 1] & 0xff;
				} else { /* YUY2: Y0 U Y1 V */
					int p = offsets[0] + y * mStrides[0] + (x / 2) * 4;
					luma = mData[p + (x & 1) * 2] & 0xff;
					cb = mData[p + 1] & 0xff;
					cr = mData[p + 3] & 0xff;
				}
				argb[y * width + x] = jfifToArgb(luma, cb, cr);
			}
		}
		Bitmap bitmap = Bitmap.createBitmap(argb, width, height, Bitmap.Config.ARGB_8888);
		boolean ret = bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream);
		bitmap.recycle();
		return ret;
	}

	private static int clampByte(double v) {
		return v < 0 ? 0 : v > 255 ? 255 : (int)Math.round(v);
	}

	private static int jfifToArgb(int luma, int cb, int cr) {
		int r = clampByte(luma + 1.402 * (cr - 128));
		int g = clampByte(luma - 0.344136 * (cb - 128) - 0.714136 * (cr - 128));
		int b = clampByte(luma + 1.772 * (cb - 128));
		return 0xff000000 | (r << 16) | (g << 8) | b;
	}

	public byte[] getYuvData() {
		return mData;
	}

	public int getYuvFormat() {
		return mFormat;
	}

	public int[] getStrides() {
		return mStrides;
	}

	public int getWidth() {
		return mWidth;
	}

	public int getHeight() {
		return mHeight;
	}

	public ColorSpace getColorSpace() {
		return mColorSpace;
	}

	int[] calculateOffsets(int left, int top) {
		int[] offsets = null;
		if (mFormat == ImageFormat.NV21) {
			offsets = new int[] {top * mStrides[0] + left,
					     mHeight * mStrides[0] + top / 2 * mStrides[1] + left / 2 * 2};
			return offsets;
		}
		if (mFormat == ImageFormat.YUY2) {
			offsets = new int[] {top * mStrides[0] + left / 2 * 4};
			return offsets;
		}
		return offsets;
	}

	private int[] calculateStrides(int width, int format) {
		int[] strides = null;
		switch (format) {
			case ImageFormat.NV21:
				strides = new int[] {width, width};
				return strides;
			case ImageFormat.YCBCR_P010:
				strides = new int[] {width * 2, width * 2};
				return strides;
			case ImageFormat.YUV_420_888:
				strides = new int[] {width, (width + 1) / 2, (width + 1) / 2};
				return strides;
			case ImageFormat.YUY2:
				strides = new int[] {width * 2};
				return strides;
			default:
				throw new IllegalArgumentException(
				    "only supports the following ImageFormat:" + printSupportedFormats());
		}
	}

	private void adjustRectangle(Rect rect) {
		int width = rect.width();
		int height = rect.height();
		if (mFormat == ImageFormat.NV21) {
			width &= ~1;
			height &= ~1;
			rect.left &= ~1;
			rect.top &= ~1;
			rect.right = rect.left + width;
			rect.bottom = rect.top + height;
		}
		if (mFormat == ImageFormat.YUY2) {
			width &= ~1;
			rect.left &= ~1;
			rect.right = rect.left + width;
		}
	}
}
