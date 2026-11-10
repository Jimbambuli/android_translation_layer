package android.view;

import android.atl.annotation.Export;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.DisplayMetrics;

@Export
public final class Display {

	/* AOSP Display.DEFAULT_DISPLAY */
	public static final int DEFAULT_DISPLAY = 0;
	/* AOSP Display.INVALID_DISPLAY */
	public static final int INVALID_DISPLAY = -1;

	public static int window_width = 960;
	public static int window_height = 540;

	// FIXME: what do we return here?
	// we don't want to hardcode this stuff, but at the same time the apps can cache it
	public void getMetrics(DisplayMetrics outMetrics) {
		outMetrics.widthPixels = this.window_width;
		outMetrics.heightPixels = this.window_height;
	}

	public void getRealMetrics(DisplayMetrics outMetrics) {
		getMetrics(outMetrics); // probably?
	}

	/* ATL has no display flags (no secure/private/presentation displays) */
	public int getFlags() {
		return 0;
	}

	public int getWidth() {
		return window_width;
	}

	public int getHeight() {
		return window_height;
	}

	public int getRawWidth() {
		return window_width; // what's the difference?
	}

	public int getRawHeight() {
		return window_height; // what's the difference?
	}

	public int getRotation() {
		return 0 /*ROTATION_0*/;
	}

	public float getRefreshRate() {
		return 60; // FIXME
	}

	public long getAppVsyncOffsetNanos() {
		return 0; // what else would we return here?
	}

	/* AOSP 14 Display: the desktop display has no HDR capabilities (DisplayInfo.hdrCapabilities null),
	 * no HDR/SDR ratio and no rounded corners */
	public HdrCapabilities getHdrCapabilities() {
		return null;
	}

	public boolean isHdr() {
		HdrCapabilities hdrCapabilities = getHdrCapabilities();
		if (hdrCapabilities == null) {
			return false;
		}
		return !(hdrCapabilities.getSupportedHdrTypes().length == 0);
	}

	public float getHdrSdrRatio() {
		return 1.0f;
	}

	public RoundedCorner getRoundedCorner(int position) {
		return null;
	}

	/* AOSP 14 Display.HdrCapabilities without the Parcelable part */
	public static final class HdrCapabilities {
		public static final float INVALID_LUMINANCE = -1;
		public static final int HDR_TYPE_INVALID = -1;
		public static final int HDR_TYPE_DOLBY_VISION = 1;
		public static final int HDR_TYPE_HDR10 = 2;
		public static final int HDR_TYPE_HLG = 3;
		public static final int HDR_TYPE_HDR10_PLUS = 4;

		private int[] mSupportedHdrTypes = new int[0];
		private float mMaxLuminance = INVALID_LUMINANCE;
		private float mMaxAverageLuminance = INVALID_LUMINANCE;
		private float mMinLuminance = INVALID_LUMINANCE;

		public HdrCapabilities() {
		}

		public HdrCapabilities(int[] supportedHdrTypes, float maxLuminance, float maxAverageLuminance, float minLuminance) {
			mSupportedHdrTypes = supportedHdrTypes;
			java.util.Arrays.sort(mSupportedHdrTypes);
			mMaxLuminance = maxLuminance;
			mMaxAverageLuminance = maxAverageLuminance;
			mMinLuminance = minLuminance;
		}

		public int[] getSupportedHdrTypes() {
			return java.util.Arrays.copyOf(mSupportedHdrTypes, mSupportedHdrTypes.length);
		}

		public float getDesiredMaxLuminance() {
			return mMaxLuminance;
		}

		public float getDesiredMaxAverageLuminance() {
			return mMaxAverageLuminance;
		}

		public float getDesiredMinLuminance() {
			return mMinLuminance;
		}
	}

	/* AOSP 14: deprecated, always RGBA_8888 */
	public int getPixelFormat() {
		return android.graphics.PixelFormat.RGBA_8888;
	}

	public int getDisplayId() {
		return 0;
	}

	public long getPresentationDeadlineNanos() {
		return 0; // what else...
	}

	public void getSize(Point size) {
		size.set(getWidth(), getHeight());
	}

	public void getRealSize(Point size) {
		getSize(size);
	}

	public void getRectSize(Rect rect) {
		rect.set(0, 0, getWidth(), getHeight());
	}

	public DisplayCutout getCutout() {
		return DisplayCutout.NO_CUTOUT;
	}

	public Display.Mode getMode() {
		return new Display.Mode();
	}

	public static final class Mode {

		public int getPhysicalWidth() {
			return window_width;
		}

		public int getPhysicalHeight() {
			return window_height;
		}
	}

	public Mode[] getSupportedModes() {
		return new Mode[] {getMode()};
	}
}
