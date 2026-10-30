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

package android.graphics;

import android.media.ExifInterface;
import android.util.Size;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * A class for converting encoded images (like {@code PNG}, {@code JPEG},
 * {@code WEBP}, {@code GIF}, or {@code HEIF}) into {@link Bitmap} objects.
 *
 * ATL: AOSP's ImageDecoder is a front end for the native SkAndroidCodec. Here the API is the
 * same for the still image sources (File, byte[]) and decodeBitmap(); decoding goes through
 * BitmapFactory, followed by what SkAndroidCodec adds on top: the EXIF orientation and the
 * scaling to the target size. Drawables, animations, crop, post processors and the other
 * sources are not there.
 */
public final class ImageDecoder implements AutoCloseable {
	/**
	 * Source of encoded image data.
	 */
	public static abstract class Source {
		private Source() {}

		abstract InputStream open() throws IOException;
	}

	private static class ByteArraySource extends Source {
		private final byte[] mData;
		private final int mOffset;
		private final int mLength;

		ByteArraySource(byte[] data, int offset, int length) {
			mData = data;
			mOffset = offset;
			mLength = length;
		}

		@Override
		InputStream open() {
			return new ByteArrayInputStream(mData, mOffset, mLength);
		}
	}

	private static class FileSource extends Source {
		private final File mFile;

		FileSource(File file) {
			mFile = file;
		}

		@Override
		InputStream open() throws IOException {
			return new FileInputStream(mFile);
		}
	}

	/**
	 * Information about an encoded image.
	 */
	public static class ImageInfo {
		private final ImageDecoder mDecoder;

		private ImageInfo(ImageDecoder decoder) {
			mDecoder = decoder;
		}

		/**
		 * Size of the image, without scaling or cropping.
		 */
		public Size getSize() {
			return new Size(mDecoder.mWidth, mDecoder.mHeight);
		}

		/**
		 * The mimeType of the image.
		 */
		public String getMimeType() {
			return mDecoder.mMimeType;
		}

		/**
		 * Whether the image is animated. Only still images are decoded here.
		 */
		public boolean isAnimated() {
			return false;
		}

		/**
		 * The color space of the image.
		 */
		public ColorSpace getColorSpace() {
			return ColorSpace.get(ColorSpace.Named.SRGB);
		}
	}

	/**
	 * Interface for changing the default settings of a decode.
	 */
	public static interface OnHeaderDecodedListener {
		public void onHeaderDecoded(ImageDecoder decoder, ImageInfo info, Source source);
	}

	/**
	 * Information about an interrupted decode.
	 */
	public static final class DecodeException extends IOException {
		public static final int SOURCE_EXCEPTION = 1;
		public static final int SOURCE_INCOMPLETE = 2;
		public static final int SOURCE_MALFORMED_DATA = 3;

		final int mError;
		final Source mSource;

		DecodeException(int error, Throwable cause, Source source) {
			super(errorMessage(error, cause), cause);
			mError = error;
			mSource = source;
		}

		public int getError() {
			return mError;
		}

		public Source getSource() {
			return mSource;
		}

		private static String errorMessage(int error, Throwable cause) {
			switch (error) {
				case SOURCE_EXCEPTION:
					return "Exception in input: " + cause;
				case SOURCE_INCOMPLETE:
					return "Input was incomplete.";
				case SOURCE_MALFORMED_DATA:
					return "Input contained an error.";
				default:
					return "";
			}
		}
	}

	/**
	 * Interface for inspecting a {@link DecodeException DecodeException}.
	 */
	public static interface OnPartialImageListener {
		boolean onPartialImage(DecodeException exception);
	}

	public static final int ALLOCATOR_DEFAULT = 0;
	public static final int ALLOCATOR_SOFTWARE = 1;
	public static final int ALLOCATOR_SHARED_MEMORY = 2;
	public static final int ALLOCATOR_HARDWARE = 3;

	public static final int MEMORY_POLICY_LOW_RAM = 0;
	public static final int MEMORY_POLICY_DEFAULT = 1;

	private final int mWidth;
	private final int mHeight;
	private final String mMimeType;
	private int mDesiredWidth;
	private int mDesiredHeight;
	private int mAllocator = ALLOCATOR_DEFAULT;
	private int mMemoryPolicy = MEMORY_POLICY_DEFAULT;
	private OnPartialImageListener mOnPartialImageListener;

	private ImageDecoder(int width, int height, String mimeType) {
		mWidth = width;
		mHeight = height;
		mMimeType = mimeType;
		mDesiredWidth = width;
		mDesiredHeight = height;
	}

	/**
	 * Create a new {@link Source Source} from a byte array.
	 */
	public static Source createSource(byte[] data, int offset, int length) throws ArrayIndexOutOfBoundsException {
		if (data == null) {
			throw new NullPointerException("null byte[] in createSource!");
		}
		if (offset < 0 || length < 0 || offset >= data.length ||
		    offset + length > data.length) {
			throw new ArrayIndexOutOfBoundsException(
			    "invalid offset/length!");
		}
		return new ByteArraySource(data, offset, length);
	}

	/**
	 * See {@link #createSource(byte[], int, int).
	 */
	public static Source createSource(byte[] data) {
		return createSource(data, 0, data.length);
	}

	/**
	 * Create a new {@link Source Source} from a {@link java.io.File}.
	 */
	public static Source createSource(File file) {
		return new FileSource(file);
	}

	/**
	 * Specify the size of the output {@link Bitmap}.
	 */
	public void setTargetSize(int width, int height) {
		if (width <= 0 || height <= 0) {
			throw new IllegalArgumentException("Dimensions must be positive! "
			                                   + "provided (" + width + ", " + height + ")");
		}

		mDesiredWidth = width;
		mDesiredHeight = height;
	}

	/**
	 * Set the target size with a sampleSize. As SkAndroidCodec's generic sampling, each
	 * dimension is divided by sampleSize, but never below 1.
	 */
	public void setTargetSampleSize(int sampleSize) {
		if (sampleSize <= 0) {
			throw new IllegalArgumentException("sampleSize must be positive! "
			                                   + "provided " + sampleSize);
		}
		this.setTargetSize(Math.max(1, mWidth / sampleSize), Math.max(1, mHeight / sampleSize));
	}

	/**
	 * Choose the backing for the pixel memory. All Bitmaps are in software memory here.
	 */
	public void setAllocator(int allocator) {
		if (allocator < ALLOCATOR_DEFAULT || allocator > ALLOCATOR_HARDWARE) {
			throw new IllegalArgumentException("invalid allocator " + allocator);
		}
		mAllocator = allocator;
	}

	public int getAllocator() {
		return mAllocator;
	}

	/**
	 * Set (replace) the {@link OnPartialImageListener} on this object. BitmapFactory either
	 * decodes an image or fails, so it is never called.
	 */
	public void setOnPartialImageListener(OnPartialImageListener listener) {
		mOnPartialImageListener = listener;
	}

	public void setMemorySizePolicy(int policy) {
		mMemoryPolicy = policy;
	}

	public int getMemorySizePolicy() {
		return mMemoryPolicy;
	}

	/**
	 * Specify the desired {@link ColorSpace} for the output. Images are decoded to sRGB.
	 */
	public void setTargetColorSpace(ColorSpace colorSpace) {
	}

	@Override
	public void close() {
	}

	/**
	 * Create a {@link Bitmap} from a {@code Source}.
	 */
	public static Bitmap decodeBitmap(Source src, OnHeaderDecodedListener listener) throws IOException {
		if (listener == null) {
			throw new IllegalArgumentException("listener cannot be null! "
			                                   + "Use decodeBitmap(Source) to not have a listener");
		}
		return decodeBitmapImpl(src, listener);
	}

	/**
	 * See {@link #decodeBitmap(Source, OnHeaderDecodedListener)}.
	 */
	public static Bitmap decodeBitmap(Source src) throws IOException {
		return decodeBitmapImpl(src, null);
	}

	/* the linear part of Skia's SkEncodedOriginToMatrix(); createBitmap() moves the result to
	 * the origin. null for ORIENTATION_NORMAL or an unknown value */
	private static Matrix orientationMatrix(int exifOrientation) {
		Matrix m = new Matrix();
		switch (exifOrientation) {
			case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
				m.postScale(-1, 1);
				break;
			case ExifInterface.ORIENTATION_ROTATE_180:
				m.postRotate(180);
				break;
			case ExifInterface.ORIENTATION_FLIP_VERTICAL:
				m.postScale(1, -1);
				break;
			case ExifInterface.ORIENTATION_TRANSPOSE:
				m.postRotate(90);
				m.postScale(-1, 1);
				break;
			case ExifInterface.ORIENTATION_ROTATE_90:
				m.postRotate(90);
				break;
			case ExifInterface.ORIENTATION_TRANSVERSE:
				m.postRotate(90);
				m.postScale(1, -1);
				break;
			case ExifInterface.ORIENTATION_ROTATE_270:
				m.postRotate(270);
				break;
			default:
				return null;
		}
		return m;
	}

	private static Bitmap decodeBitmapImpl(Source src, OnHeaderDecodedListener listener) throws IOException {
		BitmapFactory.Options opts = new BitmapFactory.Options();
		Bitmap decoded;
		try (InputStream is = src.open()) {
			decoded = BitmapFactory.decodeStream(is, null, opts);
		}
		if (decoded == null) {
			throw new DecodeException(DecodeException.SOURCE_MALFORMED_DATA, null, src);
		}

		/* SkAndroidCodec applies the EXIF orientation, so the header reports the oriented size */
		Matrix orientation = null;
		try (InputStream is = src.open()) {
			orientation = orientationMatrix(new ExifInterface(is).getAttributeInt(
			    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL));
		} catch (IOException | RuntimeException e) {
			/* not an image with EXIF data */
		}
		if (orientation != null) {
			decoded = Bitmap.createBitmap(decoded, 0, 0, decoded.getWidth(), decoded.getHeight(), orientation, true);
		}

		ImageDecoder decoder = new ImageDecoder(decoded.getWidth(), decoded.getHeight(), opts.outMimeType);
		if (listener != null) {
			listener.onHeaderDecoded(decoder, new ImageInfo(decoder), src);
		}
		if (decoder.mDesiredWidth != decoder.mWidth || decoder.mDesiredHeight != decoder.mHeight) {
			decoded = Bitmap.createScaledBitmap(decoded, decoder.mDesiredWidth, decoder.mDesiredHeight, true);
		}
		return decoded;
	}
}
