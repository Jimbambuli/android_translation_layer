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

	public static final int STATE_UNKNOWN = 0;
	public static final int STATE_OFF = 1;
	public static final int STATE_ON = 2;

	/* ATL has the one display and it is always on and valid */
	public boolean isValid() {
		return true;
	}

	public int getState() {
		return STATE_ON;
	}

	/* AOSP: smallest/largest size the application can see after rotation; ATL does not rotate */
	public void getCurrentSizeRange(Point outSmallestSize, Point outLargestSize) {
		int smallest = Math.min(window_width, window_height);
		int largest = Math.max(window_width, window_height);
		outSmallestSize.x = smallest;
		outSmallestSize.y = smallest;
		outLargestSize.x = largest;
		outLargestSize.y = largest;
	}

	public int getRotation() {
		return 0 /*ROTATION_0*/;
	}

	public float getRefreshRate() {
		return 60; // FIXME
	}

	/* AOSP Display.getName() returns DisplayInfo.name; the built-in display is named by
	 * LocalDisplayAdapter from R.string.display_manager_built_in_display_name */
	public String getName() {
		return "Built-in Screen";
	}

	/* AOSP Display.getShape(): DisplayInfo.displayShape, which is
	 * DisplayShape.createDefaultDisplayShape() for a device without a shape spec */
	public DisplayShape getShape() {
		return DisplayShape.createDefaultDisplayShape(getWidth(), getHeight(), false);
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

	/* AOSP Display.isHdrSdrRatioAvailable(): DisplayInfo.hdrSdrRatio is NaN for a display without HDR */
	public boolean isHdrSdrRatioAvailable() {
		return false;
	}

	/* AOSP 16 Display.getHighestHdrSdrRatio(): 1 when isHdrSdrRatioAvailable() is false */
	public float getHighestHdrSdrRatio() {
		return 1.0f;
	}

	/* AOSP 16 Display.hasArrSupport(): DisplayInfo.hasArrSupport, false for a display without adaptive refresh
	 * rate (ATL's one mode) */
	public boolean hasArrSupport() {
		return false;
	}

	/* AOSP Display.getOrientation() (deprecated) */
	public int getOrientation() {
		return getRotation();
	}

	/* AOSP Display.getSupportedRefreshRates(): the rates of the modes of the default resolution; ATL has one mode */
	public float[] getSupportedRefreshRates() {
		return new float[] {getRefreshRate()};
	}

	/* AOSP Display.isWideColorGamut()/getPreferredWideGamutColorSpace(): ATL draws in sRGB, so the display has no
	 * wide colour gamut mode and the preferred wide gamut colour space is null */
	public boolean isWideColorGamut() {
		return false;
	}

	public android.graphics.ColorSpace getPreferredWideGamutColorSpace() {
		return null;
	}

	/* AOSP Display.isMinimalPostProcessingSupported(): DisplayInfo.minimalPostProcessingSupported, false unless the
	 * display (an HDMI sink with ALLM/game content type) says so */
	public boolean isMinimalPostProcessingSupported() {
		return false;
	}

	public void registerHdrSdrRatioChangedListener(java.util.concurrent.Executor executor, java.util.function.Consumer<Display> listener) {
		if (!isHdrSdrRatioAvailable()) {
			throw new IllegalStateException("HDR/SDR ratio changed not available");
		}
	}

	public void unregisterHdrSdrRatioChangedListener(java.util.function.Consumer<Display> listener) {
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

		/* AOSP Display.Mode.getModeId(); ATL has a single valid mode (AOSP INVALID_MODE_ID is -1) */
		public int getModeId() {
			return 1;
		}

		/* AOSP Display.Mode.getRefreshRate(); the same rate Display.getRefreshRate() reports */
		public float getRefreshRate() {
			return 60;
		}

		public int getPhysicalWidth() {
			return window_width;
		}

		public int getPhysicalHeight() {
			return window_height;
		}

		/* AOSP Display.Mode.getAlternativeRefreshRates(): the other rates of the same resolution; ATL has one mode */
		public float[] getAlternativeRefreshRates() {
			return new float[0];
		}

		/* AOSP 16 Display.Mode.getSupportedHdrTypes(): a copy of mSupportedHdrTypes; ATL's display has no
		 * HDR (getHdrCapabilities() is null, isHdr() false), so the mode supports none */
		public int[] getSupportedHdrTypes() {
			return new int[0];
		}
	}

	public Mode[] getSupportedModes() {
		return new Mode[] {getMode()};
	}
}
