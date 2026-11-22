/* AOSP frameworks/base/core/java/android/hardware/camera2/DngCreator.java (Android 16)
 * Differences: javadoc removed. The native part (android_hardware_camera2_DngCreator.cpp) is ported only as far as it can
 * be reached with ATL's HAL: nativeInit/nativeDestroy/setters and the checks in DngCreator_setup() with the same
 * exceptions; the writing of the TIFF/DNG file is not ported (see nativeSetup). ATL's camera metadata
 * has no CameraMetadataNative, nativeInit takes the CameraCharacteristics and CaptureResult directly. ATL's HAL
 * has no RAW capability and no SENSOR_INFO_COLOR_FILTER_ARRANGEMENT, so every write ends in the exception AOSP
 * throws for the missing CFAPattern (or the size AssertionError) before a TIFF writer is needed. */
package android.hardware.camera2;

import android.annotation.IntRange;
import android.annotation.NonNull;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.location.Location;
import android.media.ExifInterface;
import android.media.Image;
import android.os.SystemClock;
import android.util.Log;
import android.util.Size;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public final class DngCreator implements AutoCloseable {

	private static final String TAG = "DngCreator";
		public DngCreator(@NonNull CameraCharacteristics characteristics,
			@NonNull CaptureResult metadata) {
		if (characteristics == null || metadata == null) {
			throw new IllegalArgumentException("Null argument to DngCreator constructor");
		}

		// Find current time in milliseconds since 1970
		long currentTime = System.currentTimeMillis();
		// Assume that sensor timestamp has that timebase to start
		long timeOffset = 0;

		int timestampSource = characteristics.get(
				CameraCharacteristics.SENSOR_INFO_TIMESTAMP_SOURCE);

		if (timestampSource == CameraCharacteristics.SENSOR_INFO_TIMESTAMP_SOURCE_REALTIME) {
			// This means the same timebase as SystemClock.elapsedRealtime(),
			// which is CLOCK_BOOTTIME
			timeOffset = currentTime - SystemClock.elapsedRealtime();
		} else if (timestampSource == CameraCharacteristics.SENSOR_INFO_TIMESTAMP_SOURCE_UNKNOWN) {
			// This means the same timebase as System.currentTimeMillis(),
			// which is CLOCK_MONOTONIC
			timeOffset = currentTime - SystemClock.uptimeMillis();
		} else {
			// Unexpected time source - treat as CLOCK_MONOTONIC
			Log.w(TAG, "Sensor timestamp source is unexpected: " + timestampSource);
			timeOffset = currentTime - SystemClock.uptimeMillis();
		}

		// Find capture time (nanos since boot)
		Long timestamp = metadata.get(CaptureResult.SENSOR_TIMESTAMP);
		long captureTime = currentTime;
		if (timestamp != null) {
			captureTime = timestamp / 1000000 + timeOffset;
		}

		// Create this fresh each time since the time zone may change while a long-running application
		// is active.
		final DateFormat dateTimeStampFormat =
				new SimpleDateFormat(TIFF_DATETIME_FORMAT, Locale.US);
		dateTimeStampFormat.setTimeZone(TimeZone.getDefault());

		// Format for metadata
		String formattedCaptureTime = dateTimeStampFormat.format(captureTime);

		nativeInit(characteristics, metadata, formattedCaptureTime);
	}

		@NonNull
	public DngCreator setOrientation(int orientation) {
		if (orientation < ExifInterface.ORIENTATION_UNDEFINED ||
				orientation > ExifInterface.ORIENTATION_ROTATE_270) {
			throw new IllegalArgumentException("Orientation " + orientation +
					" is not a valid EXIF orientation value");
		}
		// ExifInterface and TIFF/EP spec differ on definition of
		// "Unknown" orientation; other values map directly
		if (orientation == ExifInterface.ORIENTATION_UNDEFINED) {
			orientation = TAG_ORIENTATION_UNKNOWN;
		}
		nativeSetOrientation(orientation);
		return this;
	}

		@NonNull
	public DngCreator setThumbnail(@NonNull Bitmap pixels) {
		if (pixels == null) {
			throw new IllegalArgumentException("Null argument to setThumbnail");
		}

		int width = pixels.getWidth();
		int height = pixels.getHeight();

		if (width > MAX_THUMBNAIL_DIMENSION || height > MAX_THUMBNAIL_DIMENSION) {
			throw new IllegalArgumentException("Thumbnail dimensions width,height (" + width +
					"," + height + ") too large, dimensions must be smaller than " +
					MAX_THUMBNAIL_DIMENSION);
		}

		ByteBuffer rgbBuffer = convertToRGB(pixels);
		nativeSetThumbnail(rgbBuffer, width, height);

		return this;
	}

		@NonNull
	public DngCreator setThumbnail(@NonNull Image pixels) {
		if (pixels == null) {
			throw new IllegalArgumentException("Null argument to setThumbnail");
		}

		int format = pixels.getFormat();
		if (format != ImageFormat.YUV_420_888) {
			throw new IllegalArgumentException("Unsupported Image format " + format);
		}

		int width = pixels.getWidth();
		int height = pixels.getHeight();

		if (width > MAX_THUMBNAIL_DIMENSION || height > MAX_THUMBNAIL_DIMENSION) {
			throw new IllegalArgumentException("Thumbnail dimensions width,height (" + width +
					"," + height + ") too large, dimensions must be smaller than " +
					MAX_THUMBNAIL_DIMENSION);
		}

		ByteBuffer rgbBuffer = convertToRGB(pixels);
		nativeSetThumbnail(rgbBuffer, width, height);

		return this;
	}

		@NonNull
	public DngCreator setLocation(@NonNull Location location) {
		if (location == null) {
			throw new IllegalArgumentException("Null location passed to setLocation");
		}
		double latitude = location.getLatitude();
		double longitude = location.getLongitude();
		long time = location.getTime();

		int[] latTag = toExifLatLong(latitude);
		int[] longTag = toExifLatLong(longitude);
		String latRef = latitude >= 0 ? GPS_LAT_REF_NORTH : GPS_LAT_REF_SOUTH;
		String longRef = longitude >= 0 ? GPS_LONG_REF_EAST : GPS_LONG_REF_WEST;

		String dateTag = sExifGPSDateStamp.format(time);
		mGPSTimeStampCalendar.setTimeInMillis(time);
		int[] timeTag = new int[] { mGPSTimeStampCalendar.get(Calendar.HOUR_OF_DAY), 1,
				mGPSTimeStampCalendar.get(Calendar.MINUTE), 1,
				mGPSTimeStampCalendar.get(Calendar.SECOND), 1 };
		nativeSetGpsTags(latTag, latRef, longTag, longRef, dateTag, timeTag);
		return this;
	}

		@NonNull
	public DngCreator setDescription(@NonNull String description) {
		if (description == null) {
			throw new IllegalArgumentException("Null description passed to setDescription.");
		}
		nativeSetDescription(description);
		return this;
	}

		public void writeInputStream(@NonNull OutputStream dngOutput, @NonNull Size size,
			@NonNull InputStream pixels, @IntRange(from=0) long offset) throws IOException {
		if (dngOutput == null) {
			throw new IllegalArgumentException("Null dngOutput passed to writeInputStream");
		} else if (size == null) {
			throw new IllegalArgumentException("Null size passed to writeInputStream");
		} else if (pixels == null) {
			throw new IllegalArgumentException("Null pixels passed to writeInputStream");
		} else if (offset < 0) {
			throw new IllegalArgumentException("Negative offset passed to writeInputStream");
		}

		int width = size.getWidth();
		int height = size.getHeight();
		if (width <= 0 || height <= 0) {
			throw new IllegalArgumentException("Size with invalid width, height: (" + width + "," +
					height + ") passed to writeInputStream");
		}
		nativeWriteInputStream(dngOutput, pixels, width, height, offset);
	}

		public void writeByteBuffer(@NonNull OutputStream dngOutput, @NonNull Size size,
			@NonNull ByteBuffer pixels, @IntRange(from=0) long offset)
			throws IOException {
		if (dngOutput == null) {
			throw new IllegalArgumentException("Null dngOutput passed to writeByteBuffer");
		} else if (size == null) {
			throw new IllegalArgumentException("Null size passed to writeByteBuffer");
		} else if (pixels == null) {
			throw new IllegalArgumentException("Null pixels passed to writeByteBuffer");
		} else if (offset < 0) {
			throw new IllegalArgumentException("Negative offset passed to writeByteBuffer");
		}

		int width = size.getWidth();
		int height = size.getHeight();

		writeByteBuffer(width, height, pixels, dngOutput, DEFAULT_PIXEL_STRIDE,
				width * DEFAULT_PIXEL_STRIDE, offset);
	}

		public void writeImage(@NonNull OutputStream dngOutput, @NonNull Image pixels)
			throws IOException {
		if (dngOutput == null) {
			throw new IllegalArgumentException("Null dngOutput to writeImage");
		} else if (pixels == null) {
			throw new IllegalArgumentException("Null pixels to writeImage");
		}

		int format = pixels.getFormat();
		if (format != ImageFormat.RAW_SENSOR) {
			throw new IllegalArgumentException("Unsupported image format " + format);
		}

		Image.Plane[] planes = pixels.getPlanes();
		if (planes == null || planes.length <= 0) {
			throw new IllegalArgumentException("Image with no planes passed to writeImage");
		}

		ByteBuffer buf = planes[0].getBuffer();
		writeByteBuffer(pixels.getWidth(), pixels.getHeight(), buf, dngOutput,
				planes[0].getPixelStride(), planes[0].getRowStride(), 0);
	}

	@Override
	public void close() {
		nativeDestroy();
	}

		public static final int MAX_THUMBNAIL_DIMENSION = 256; // max pixel dimension for TIFF/EP

	@Override
	protected void finalize() throws Throwable {
		try {
			close();
		} finally {
			super.finalize();
		}
	}

	private static final String GPS_LAT_REF_NORTH = "N";
	private static final String GPS_LAT_REF_SOUTH = "S";
	private static final String GPS_LONG_REF_EAST = "E";
	private static final String GPS_LONG_REF_WEST = "W";

	private static final String GPS_DATE_FORMAT_STR = "yyyy:MM:dd";
	private static final String TIFF_DATETIME_FORMAT = "yyyy:MM:dd HH:mm:ss";
	private static final DateFormat sExifGPSDateStamp =
			new SimpleDateFormat(GPS_DATE_FORMAT_STR, Locale.US);
	private final Calendar mGPSTimeStampCalendar = Calendar
			.getInstance(TimeZone.getTimeZone("UTC"));

	static {
		sExifGPSDateStamp.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	private static final int DEFAULT_PIXEL_STRIDE = 2; // bytes per sample
	private static final int BYTES_PER_RGB_PIX = 3; // bytes per pixel

	// TIFF tag values needed to map between public API and TIFF spec
	private static final int TAG_ORIENTATION_UNKNOWN = 9;

		private void writeByteBuffer(int width, int height, ByteBuffer pixels, OutputStream dngOutput,
								 int pixelStride, int rowStride, long offset)  throws IOException {
		if (width <= 0 || height <= 0) {
			throw new IllegalArgumentException("Image with invalid width, height: (" + width + "," +
					height + ") passed to write");
		}
		long capacity = pixels.capacity();
		long totalSize = ((long) rowStride) * height + offset;
		if (capacity < totalSize) {
			throw new IllegalArgumentException("Image size " + capacity +
					" is too small (must be larger than " + totalSize + ")");
		}
		int minRowStride = pixelStride * width;
		if (minRowStride > rowStride) {
			throw new IllegalArgumentException("Invalid image pixel stride, row byte width " +
					minRowStride + " is too large, expecting " + rowStride);
		}
		pixels.clear(); // Reset mark and limit
		nativeWriteImage(dngOutput, width, height, pixels, rowStride, pixelStride, offset,
				pixels.isDirect());
		pixels.clear();
	}

		private static void yuvToRgb(byte[] yuvData, int outOffset, /*out*/byte[] rgbOut) {
		final int COLOR_MAX = 255;

		float y = yuvData[0] & 0xFF;  // Y channel
		float cb = yuvData[1] & 0xFF; // U channel
		float cr = yuvData[2] & 0xFF; // V channel

		// convert YUV -> RGB (from JFIF's "Conversion to and from RGB" section)
		float r = y + 1.402f * (cr - 128);
		float g = y - 0.34414f * (cb - 128) - 0.71414f * (cr - 128);
		float b = y + 1.772f * (cb - 128);

		// clamp to [0,255]
		rgbOut[outOffset] = (byte) Math.max(0, Math.min(COLOR_MAX, r));
		rgbOut[outOffset + 1] = (byte) Math.max(0, Math.min(COLOR_MAX, g));
		rgbOut[outOffset + 2] = (byte) Math.max(0, Math.min(COLOR_MAX, b));
	}

		private static void colorToRgb(int color, int outOffset, /*out*/byte[] rgbOut) {
		rgbOut[outOffset] = (byte) Color.red(color);
		rgbOut[outOffset + 1] = (byte) Color.green(color);
		rgbOut[outOffset + 2] = (byte) Color.blue(color);
		// Discards Alpha
	}

		private static ByteBuffer convertToRGB(Image yuvImage) {
		// TODO: Optimize this with renderscript intrinsic.
		int width = yuvImage.getWidth();
		int height = yuvImage.getHeight();
		ByteBuffer buf = ByteBuffer.allocateDirect(BYTES_PER_RGB_PIX * width * height);

		Image.Plane yPlane = yuvImage.getPlanes()[0];
		Image.Plane uPlane = yuvImage.getPlanes()[1];
		Image.Plane vPlane = yuvImage.getPlanes()[2];

		ByteBuffer yBuf = yPlane.getBuffer();
		ByteBuffer uBuf = uPlane.getBuffer();
		ByteBuffer vBuf = vPlane.getBuffer();

		yBuf.rewind();
		uBuf.rewind();
		vBuf.rewind();

		int yRowStride = yPlane.getRowStride();
		int vRowStride = vPlane.getRowStride();
		int uRowStride = uPlane.getRowStride();

		int yPixStride = yPlane.getPixelStride();
		int vPixStride = vPlane.getPixelStride();
		int uPixStride = uPlane.getPixelStride();

		byte[] yuvPixel = { 0, 0, 0 };
		byte[] yFullRow = new byte[yPixStride * (width - 1) + 1];
		byte[] uFullRow = new byte[uPixStride * (width / 2 - 1) + 1];
		byte[] vFullRow = new byte[vPixStride * (width / 2 - 1) + 1];
		byte[] finalRow = new byte[BYTES_PER_RGB_PIX * width];
		for (int i = 0; i < height; i++) {
			int halfH = i / 2;
			yBuf.position(yRowStride * i);
			yBuf.get(yFullRow);
			uBuf.position(uRowStride * halfH);
			uBuf.get(uFullRow);
			vBuf.position(vRowStride * halfH);
			vBuf.get(vFullRow);
			for (int j = 0; j < width; j++) {
				int halfW = j / 2;
				yuvPixel[0] = yFullRow[yPixStride * j];
				yuvPixel[1] = uFullRow[uPixStride * halfW];
				yuvPixel[2] = vFullRow[vPixStride * halfW];
				yuvToRgb(yuvPixel, j * BYTES_PER_RGB_PIX, /*out*/finalRow);
			}
			buf.put(finalRow);
		}

		yBuf.rewind();
		uBuf.rewind();
		vBuf.rewind();
		buf.rewind();
		return buf;
	}

		private static ByteBuffer convertToRGB(Bitmap argbBitmap) {
		// TODO: Optimize this.
		int width = argbBitmap.getWidth();
		int height = argbBitmap.getHeight();
		ByteBuffer buf = ByteBuffer.allocateDirect(BYTES_PER_RGB_PIX * width * height);

		int[] pixelRow = new int[width];
		byte[] finalRow = new byte[BYTES_PER_RGB_PIX * width];
		for (int i = 0; i < height; i++) {
			argbBitmap.getPixels(pixelRow, /*offset*/0, /*stride*/width, /*x*/0, /*y*/i,
					/*width*/width, /*height*/1);
			for (int j = 0; j < width; j++) {
				colorToRgb(pixelRow[j], j * BYTES_PER_RGB_PIX, /*out*/finalRow);
			}
			buf.put(finalRow);
		}

		buf.rewind();
		return buf;
	}

		private static int[] toExifLatLong(double value) {
		// convert to the format dd/1 mm/1 ssss/100
		value = Math.abs(value);
		int degrees = (int) value;
		value = (value - degrees) * 60;
		int minutes = (int) value;
		value = (value - minutes) * 6000;
		int seconds = (int) value;
		return new int[] { degrees, 1, minutes, 1, seconds, 100 };
	}

		/* The JNI part (android_hardware_camera2_DngCreator.cpp) is not available in ATL. Its NativeContext is
	 * mCharacteristics/mResult (null after nativeDestroy, like a cleared mNativeContext); the checks of nativeInit,
	 * the setters and the write functions up to DngCreator_setup() are ported. DngCreator_setup() throws with ATL's
	 * HAL before any TIFF writer is needed (see nativeSetup), so the TIFF/DNG writing itself (TiffWriter, strip
	 * sources, thumbnail IFD) is not ported. */
	private CameraCharacteristics mCharacteristics;
	private CaptureResult mResult;

	private synchronized void nativeInit(CameraCharacteristics characteristics, CaptureResult result,
										 String captureTime) {
		if (captureTime.length() + 1 != 20) {
			throw new IllegalArgumentException(
					"Formatted capture time string length is not required 20 characters");
		}
		mCharacteristics = characteristics;
		mResult = result;
	}

	private synchronized void nativeDestroy() {
		mCharacteristics = null;
		mResult = null;
	}

	private synchronized void nativeSetOrientation(int orientation) {
		if (mCharacteristics == null) {
			throw new AssertionError("setOrientation called with uninitialized DngCreator");
		}
	}

	private synchronized void nativeSetDescription(String description) {
		if (mCharacteristics == null) {
			throw new AssertionError("setDescription called with uninitialized DngCreator");
		}
	}

	private synchronized void nativeSetGpsTags(int[] latTag, String latRef, int[] longTag,
											   String longRef, String dateTag, int[] timeTag) {
		if (mCharacteristics == null) {
			throw new AssertionError("setGpsTags called with uninitialized DngCreator");
		}
		final int GPS_VALUE_LENGTH = 6;
		if (latTag.length != GPS_VALUE_LENGTH) {
			throw new IllegalArgumentException("invalid latitude tag length");
		} else if (longTag.length != GPS_VALUE_LENGTH) {
			throw new IllegalArgumentException("invalid longitude tag length");
		} else if (timeTag.length != GPS_VALUE_LENGTH) {
			throw new IllegalArgumentException("invalid time tag length");
		}
	}

	private synchronized void nativeSetThumbnail(ByteBuffer buffer, int width, int height) {
		if (mCharacteristics == null) {
			throw new AssertionError("setThumbnail called with uninitialized DngCreator");
		}
		int fullSize = width * height * BYTES_PER_RGB_PIX;
		if (buffer.capacity() != fullSize) {
			throw new AssertionError("Invalid size " + buffer.capacity()
					+ " for thumbnail, expected size was " + fullSize);
		}
	}

	private synchronized void nativeWriteImage(OutputStream out, int width, int height,
											   ByteBuffer rawBuffer, int rowStride,
											   int pixStride, long offset, boolean isDirect)
											   throws IOException {
		if (mCharacteristics == null) {
			throw new AssertionError("Write called with uninitialized DngCreator");
		}
		nativeSetup(width, height);
	}

	private synchronized void nativeWriteInputStream(OutputStream out, InputStream rawStream,
													 int width, int height, long offset)
													 throws IOException {
		if (mCharacteristics == null) {
			throw new AssertionError("Write called with uninitialized DngCreator");
		}
		nativeSetup(width, height);
	}

	private static String missing(String tag, int id) {
		return "Missing metadata fields for tag " + tag + " (" + Integer.toHexString(id) + ")";
	}

	/* DngCreator_setup() up to the first tag that ATL's HAL does not report: dimension check against
	 * the (maximum resolution) pre-correction active array / pixel array, MONOCHROME / MONO / NIR consistency and
	 * the required CFAPattern. ATL's HAL (ExternalCameraDevice) reports no SENSOR_INFO_COLOR_FILTER_ARRANGEMENT,
	 * so with its metadata this always ends in the IllegalArgumentException for TAG_CFAPATTERN (or in the
	 * AssertionError for a size that is neither the active array nor the pixel array); the TiffWriter code that
	 * follows in AOSP cannot be reached and is not ported. */
	private void nativeSetup(int width, int height) {
		final int TAG_IMAGEWIDTH = 0x0100;
		final int TAG_CFAPATTERN = 0x828E;
		long imageWidth = width & 0xFFFFFFFFL;
		long imageHeight = height & 0xFFFFFFFFL;
		CameraCharacteristics characteristics = mCharacteristics;

		int[] caps = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);
		boolean isMaximumResolutionMode = false;
		if (caps != null) {
			for (int cap : caps) {
				if (cap == CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_ULTRA_HIGH_RESOLUTION_SENSOR) {
					isMaximumResolutionMode = true;
				}
			}
		}
		if (isMaximumResolutionMode) {
			// isMaximumResolutionModeImage()
			android.graphics.Rect pre = characteristics.get(
					CameraCharacteristics.SENSOR_INFO_PRE_CORRECTION_ACTIVE_ARRAY_SIZE_MAXIMUM_RESOLUTION);
			if (pre == null) {
				throw new IllegalArgumentException(missing("ImageWidth", TAG_IMAGEWIDTH));
			}
			Size pix = characteristics.get(
					CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE_MAXIMUM_RESOLUTION);
			if (pix == null) {
				throw new IllegalArgumentException(missing("ImageWidth", TAG_IMAGEWIDTH));
			}
			isMaximumResolutionMode = (imageWidth == pre.width() && imageHeight == pre.height())
					|| (imageWidth == pix.getWidth() && imageHeight == pix.getHeight());
		}

		android.graphics.Rect pre = characteristics.get(isMaximumResolutionMode
				? CameraCharacteristics.SENSOR_INFO_PRE_CORRECTION_ACTIVE_ARRAY_SIZE_MAXIMUM_RESOLUTION
				: CameraCharacteristics.SENSOR_INFO_PRE_CORRECTION_ACTIVE_ARRAY_SIZE);
		if (pre == null) {
			throw new IllegalArgumentException(missing("ImageWidth", TAG_IMAGEWIDTH));
		}
		Size pix = characteristics.get(isMaximumResolutionMode
				? CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE_MAXIMUM_RESOLUTION
				: CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE);
		if (pix == null) {
			throw new IllegalArgumentException(missing("ImageWidth", TAG_IMAGEWIDTH));
		}
		if (!((imageWidth == pre.width() && imageHeight == pre.height())
				|| (imageWidth == pix.getWidth() && imageHeight == pix.getHeight()))) {
			throw new AssertionError("Height and width of image buffer did not match height and width of"
					+ " either the preCorrectionActiveArraySize or the pixelArraySize.");
		}

		// AOSP reads the color filter arrangement without checking that it exists
		Integer colorFilter = characteristics.get(CameraCharacteristics.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT);
		boolean monochrome = false;
		if (caps != null) {
			for (int cap : caps) {
				if (cap == CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_MONOCHROME) {
					monochrome = true;
				}
			}
		}
		if (!monochrome && colorFilter != null
				&& (colorFilter == CameraMetadata.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT_MONO
				|| colorFilter == CameraMetadata.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT_NIR)) {
			throw new AssertionError(
					"A camera device with MONO/NIR color filter must have MONOCHROME capability.");
		}

		if (colorFilter == null) {
			throw new IllegalArgumentException(missing("CFAPattern", TAG_CFAPATTERN));
		}
		throw new UnsupportedOperationException("TIFF writer is not available in ATL");
	}
}
