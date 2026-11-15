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

	/* AOSP Display.getDeviceProductInfo(): DisplayInfo.deviceProductInfo, set only for a display whose EDID the
	 * display service read; the answer for a display without product information is null */
	public android.hardware.display.DeviceProductInfo getDeviceProductInfo() {
		return null;
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

	/* AOSP Display.HdrCapabilities */
	public static final class HdrCapabilities implements android.os.Parcelable {
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

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			if (!(other instanceof HdrCapabilities)) {
				return false;
			}
			HdrCapabilities that = (HdrCapabilities)other;
			return java.util.Arrays.equals(mSupportedHdrTypes, that.mSupportedHdrTypes) && mMaxLuminance == that.mMaxLuminance
			    && mMaxAverageLuminance == that.mMaxAverageLuminance && mMinLuminance == that.mMinLuminance;
		}

		@Override
		public int hashCode() {
			int hash = 23;
			hash = hash * 17 + java.util.Arrays.hashCode(mSupportedHdrTypes);
			hash = hash * 17 + Float.floatToIntBits(mMaxLuminance);
			hash = hash * 17 + Float.floatToIntBits(mMaxAverageLuminance);
			hash = hash * 17 + Float.floatToIntBits(mMinLuminance);
			return hash;
		}

		public static final android.os.Parcelable.Creator<HdrCapabilities> CREATOR = new android.os.Parcelable.Creator<HdrCapabilities>() {
			@Override
			public HdrCapabilities createFromParcel(android.os.Parcel source) {
				return new HdrCapabilities(source);
			}

			@Override
			public HdrCapabilities[] newArray(int size) {
				return new HdrCapabilities[size];
			}
		};

		private HdrCapabilities(android.os.Parcel source) {
			int types = source.readInt();
			mSupportedHdrTypes = new int[types];
			for (int i = 0; i < types; ++i) {
				mSupportedHdrTypes[i] = source.readInt();
			}
			mMaxLuminance = source.readFloat();
			mMaxAverageLuminance = source.readFloat();
			mMinLuminance = source.readFloat();
		}

		@Override
		public int describeContents() {
			return 0;
		}

		@Override
		public void writeToParcel(android.os.Parcel dest, int flags) {
			dest.writeInt(mSupportedHdrTypes.length);
			for (int i = 0; i < mSupportedHdrTypes.length; ++i) {
				dest.writeInt(mSupportedHdrTypes[i]);
			}
			dest.writeFloat(mMaxLuminance);
			dest.writeFloat(mMaxAverageLuminance);
			dest.writeFloat(mMinLuminance);
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

	public static final class Mode implements android.os.Parcelable {

		/* AOSP keeps these as fields; ATL's own mode reads the window live, a mode un-parceled from AOSP's format is a snapshot */
		private boolean mFixed;
		private int mModeId;
		private int mParentModeId = -1; // AOSP INVALID_MODE_ID
		private int mFlags;
		private int mWidth;
		private int mHeight;
		private float mPeakRefreshRate;
		private float mVsyncRate;
		private float[] mAlternativeRefreshRates = new float[0];
		private int[] mSupportedHdrTypes = new int[0];

		Mode() {}

		private Mode(int modeId, int parentModeId, int flags, int width, int height, float refreshRate, float vsyncRate,
		             float[] alternativeRefreshRates, int[] supportedHdrTypes) {
			mFixed = true;
			mModeId = modeId;
			mParentModeId = parentModeId;
			mFlags = flags;
			mWidth = width;
			mHeight = height;
			mPeakRefreshRate = refreshRate;
			mVsyncRate = vsyncRate;
			mAlternativeRefreshRates = java.util.Arrays.copyOf(alternativeRefreshRates, alternativeRefreshRates.length);
			java.util.Arrays.sort(mAlternativeRefreshRates);
			mSupportedHdrTypes = java.util.Arrays.copyOf(supportedHdrTypes, supportedHdrTypes.length);
			java.util.Arrays.sort(mSupportedHdrTypes);
		}

		private Mode(android.os.Parcel in) {
			this(in.readInt(), in.readInt(), in.readInt(), in.readInt(), in.readInt(), in.readFloat(), in.readFloat(),
			     in.createFloatArray(), in.createIntArray());
		}

		@Override
		public int describeContents() {
			return 0;
		}

		/* AOSP Display.Mode.writeToParcel() */
		@Override
		public void writeToParcel(android.os.Parcel out, int parcelableFlags) {
			out.writeInt(getModeId());
			out.writeInt(mParentModeId);
			out.writeInt(mFlags);
			out.writeInt(getPhysicalWidth());
			out.writeInt(getPhysicalHeight());
			out.writeFloat(getRefreshRate());
			out.writeFloat(mFixed ? mVsyncRate : getRefreshRate());
			out.writeFloatArray(getAlternativeRefreshRates());
			out.writeIntArray(getSupportedHdrTypes());
		}

		public static final android.os.Parcelable.Creator<Mode> CREATOR = new android.os.Parcelable.Creator<Mode>() {
			@Override
			public Mode createFromParcel(android.os.Parcel in) {
				return new Mode(in);
			}

			@Override
			public Mode[] newArray(int size) {
				return new Mode[size];
			}
		};

		/* AOSP Display.Mode.getModeId(); ATL has a single valid mode (AOSP INVALID_MODE_ID is -1) */
		public int getModeId() {
			if (mFixed) return mModeId;
			return 1;
		}

		/* AOSP Display.Mode.getRefreshRate(); the same rate Display.getRefreshRate() reports */
		public float getRefreshRate() {
			if (mFixed) return mPeakRefreshRate;
			return 60;
		}

		public int getPhysicalWidth() {
			if (mFixed) return mWidth;
			return window_width;
		}

		public int getPhysicalHeight() {
			if (mFixed) return mHeight;
			return window_height;
		}

		/* AOSP Display.Mode.getAlternativeRefreshRates(): the other rates of the same resolution; ATL has one mode */
		public float[] getAlternativeRefreshRates() {
			if (mFixed) return java.util.Arrays.copyOf(mAlternativeRefreshRates, mAlternativeRefreshRates.length);
			return new float[0];
		}

		/* AOSP 16 Display.Mode.getSupportedHdrTypes(): a copy of mSupportedHdrTypes; ATL's display has no
		 * HDR (getHdrCapabilities() is null, isHdr() false), so the mode supports none */
		public int[] getSupportedHdrTypes() {
			if (mFixed) return java.util.Arrays.copyOf(mSupportedHdrTypes, mSupportedHdrTypes.length);
			return new int[0];
		}
	}

	public Mode[] getSupportedModes() {
		return new Mode[] {getMode()};
	}
}
