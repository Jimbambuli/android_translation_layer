package android.hardware;

import android.os.BadParcelableException;
import android.os.Parcel;
import android.os.Parcelable;

/* AOSP core/java/android/hardware/HardwareBuffer.java (android-16), Java side only. ATL has no gralloc /
 * AHardwareBuffer allocator, so nCreateHardwareBuffer() and nReadHardwareBufferFromParcel() always yield 0
 * and nIsSupported() false: create() and createFromParcel() take AOSP's failure paths, and no live buffer
 * exists (mNativeObject stays 0, every instance reads as closed). */
public final class HardwareBuffer implements Parcelable, AutoCloseable {
	public static final int RGBA_8888 = 1;
	public static final int RGBX_8888 = 2;
	public static final int RGB_888 = 3;
	public static final int RGB_565 = 4;
	public static final int RGBA_FP16 = 0x16;
	public static final int RGBA_1010102 = 0x2b;
	public static final int BLOB = 0x21;
	public static final int YCBCR_420_888 = 0x23;
	public static final int D_16 = 0x30;
	public static final int D_24 = 0x31;
	public static final int DS_24UI8 = 0x32;
	public static final int D_FP32 = 0x33;
	public static final int DS_FP32UI8 = 0x34;
	public static final int S_UI8 = 0x35;
	public static final int YCBCR_P010 = 0x36;
	public static final int YCBCR_P210 = 0x3c;
	public static final int R_8 = 0x38;
	public static final int R_16 = 0x39;
	public static final int RG_1616 = 0x3a;
	public static final int RGBA_10101010 = 0x3b;
	public static final int R_12 = 0x3d;
	public static final int R_14 = 0x3e;
	public static final int RG_1212 = 0x3f;
	public static final int RG_1414 = 0x40;
	public static final int RGBA_12121212 = 0x41;
	public static final int RGBA_14141414 = 0x42;
	public static final int BGRA_1010102 = 0x43;
	public static final int BGRX_1010102 = 0x44;
	public static final long USAGE_CPU_READ_RARELY = 2;
	public static final long USAGE_CPU_READ_OFTEN = 3;
	public static final long USAGE_CPU_WRITE_RARELY = 2 << 4;
	public static final long USAGE_CPU_WRITE_OFTEN = 3 << 4;
	public static final long USAGE_GPU_SAMPLED_IMAGE = 1 << 8;
	public static final long USAGE_GPU_COLOR_OUTPUT = 1 << 9;
	public static final long USAGE_COMPOSER_OVERLAY = 1 << 11;
	public static final long USAGE_PROTECTED_CONTENT = 1 << 14;
	public static final long USAGE_VIDEO_ENCODE = 1 << 16;
	public static final long USAGE_SENSOR_DIRECT_DATA = 1 << 23;
	public static final long USAGE_GPU_DATA_BUFFER = 1 << 24;
	public static final long USAGE_GPU_CUBE_MAP = 1 << 25;
	public static final long USAGE_GPU_MIPMAP_COMPLETE = 1 << 26;
	public static final long USAGE_FRONT_BUFFER = 1L << 32;

	private long mNativeObject;

	private HardwareBuffer() {
	}

	private static void checkArguments(int width, int height, int format, int layers) {
		if (width <= 0) {
			throw new IllegalArgumentException("Invalid width " + width);
		}
		if (height <= 0) {
			throw new IllegalArgumentException("Invalid height " + height);
		}
		if (layers <= 0) {
			throw new IllegalArgumentException("Invalid layer count " + layers);
		}
		if (format == BLOB && height != 1) {
			throw new IllegalArgumentException("Height must be 1 when using the BLOB format");
		}
	}

	public static HardwareBuffer create(int width, int height, int format, int layers, long usage) {
		checkArguments(width, height, format, layers);
		throw new IllegalArgumentException("Unable to create a HardwareBuffer, either the " +
				"dimensions passed were too large, too many image layers were requested, " +
				"or an invalid set of usage flags or invalid format was passed");
	}

	public static boolean isSupported(int width, int height, int format, int layers, long usage) {
		checkArguments(width, height, format, layers);
		return false;
	}

	public int getWidth() {
		checkClosed("width");
		return 0;
	}

	public int getHeight() {
		checkClosed("height");
		return 0;
	}

	public int getFormat() {
		checkClosed("format");
		return 0;
	}

	public int getLayers() {
		checkClosed("layer count");
		return 0;
	}

	public long getUsage() {
		checkClosed("usage");
		return 0;
	}

	public long getId() {
		checkClosed("id");
		return 0;
	}

	private void checkClosed(String name) {
		if (isClosed()) {
			throw new IllegalStateException("This HardwareBuffer has been closed and its "
					+ name + " cannot be obtained.");
		}
	}

	@Override
	public void close() {
		mNativeObject = 0;
	}

	public boolean isClosed() {
		return mNativeObject == 0;
	}

	@Override
	public int describeContents() {
		return Parcelable.CONTENTS_FILE_DESCRIPTOR;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		if (isClosed()) {
			throw new IllegalStateException("This HardwareBuffer has been closed and cannot be "
					+ "written to a parcel.");
		}
	}

	public static final Parcelable.Creator<HardwareBuffer> CREATOR = new Parcelable.Creator<HardwareBuffer>() {
		public HardwareBuffer createFromParcel(Parcel in) {
			if (in == null) {
				throw new NullPointerException("null passed to createFromParcel");
			}
			throw new BadParcelableException("Failed to read hardware buffer");
		}

		public HardwareBuffer[] newArray(int size) {
			return new HardwareBuffer[size];
		}
	};
}
