package android.hardware.camera2.params;

import static com.android.internal.util.Preconditions.*;

import android.graphics.SurfaceTexture;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/* AOSP core/java/android/hardware/camera2/params/OutputConfiguration.java, reduced: no Parcelable,
 * multi-resolution or color-space part. ATL cannot query a Surface's size, format and dataspace
 * (SurfaceUtils), so a surface-backed configuration records no configured size and addSurface() skips
 * the format and dataspace match; the configuration is only consumed by a CameraDevice, which ATL never
 * opens. */
public final class OutputConfiguration {
	public static final int ROTATION_0 = 0;

	public static final int ROTATION_90 = 1;

	public static final int ROTATION_180 = 2;

	public static final int ROTATION_270 = 3;

	public static final int SURFACE_GROUP_ID_NONE = -1;

	public static final int TIMESTAMP_BASE_DEFAULT = 0;

	public static final int TIMESTAMP_BASE_SENSOR = 1;

	public static final int TIMESTAMP_BASE_MONOTONIC = 2;

	public static final int TIMESTAMP_BASE_REALTIME = 3;

	public static final int TIMESTAMP_BASE_CHOREOGRAPHER_SYNCED = 4;

	public static final int TIMESTAMP_BASE_READOUT_SENSOR = 5;

	public static final int MIRROR_MODE_AUTO = 0;
	public static final int MIRROR_MODE_NONE = 1;
	public static final int MIRROR_MODE_H = 2;
	public static final int MIRROR_MODE_V = 3;

	private static final int MAX_SURFACES_COUNT = 4;

	private ArrayList<Surface> mSurfaces;
	private final int mRotation;
	private final int mSurfaceGroupId;
	private final Size mConfiguredSize;
	private final boolean mIsDeferredConfig;
	private boolean mIsShared;
	private String mPhysicalCameraId;
	private boolean mIsMultiResolution;
	private ArrayList<Integer> mSensorPixelModesUsed = new ArrayList<Integer>();
	private long mDynamicRangeProfile = 1 /*DynamicRangeProfiles.STANDARD*/;
	private long mStreamUseCase = 0 /*CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_DEFAULT*/;
	private int mTimestampBase = TIMESTAMP_BASE_DEFAULT;
	private int mMirrorMode = MIRROR_MODE_AUTO;
	private boolean mReadoutTimestampEnabled;
	private boolean mIsReadoutSensorTimestampBase;

	public OutputConfiguration(Surface surface) {
		this(SURFACE_GROUP_ID_NONE, surface, ROTATION_0);
	}

	public OutputConfiguration(int surfaceGroupId, Surface surface) {
		this(surfaceGroupId, surface, ROTATION_0);
	}

	public OutputConfiguration(Surface surface, int rotation) {
		this(SURFACE_GROUP_ID_NONE, surface, rotation);
	}

	public OutputConfiguration(int surfaceGroupId, Surface surface, int rotation) {
		checkNotNull(surface, "Surface must not be null");
		checkArgumentInRange(rotation, ROTATION_0, ROTATION_270, "Rotation constant");
		mSurfaceGroupId = surfaceGroupId;
		mSurfaces = new ArrayList<Surface>();
		mSurfaces.add(surface);
		mRotation = rotation;
		mConfiguredSize = null;
		mIsDeferredConfig = false;
	}

	public <T> OutputConfiguration(Size surfaceSize, Class<T> klass) {
		checkNotNull(klass, "surfaceSize must not be null");
		checkNotNull(klass, "klass must not be null");
		if (klass != SurfaceHolder.class && klass != SurfaceTexture.class)
			throw new IllegalArgumentException("Unknown surface source class type");
		if (surfaceSize.getWidth() == 0 || surfaceSize.getHeight() == 0)
			throw new IllegalArgumentException("Surface size needs to be non-zero");
		mSurfaceGroupId = SURFACE_GROUP_ID_NONE;
		mSurfaces = new ArrayList<Surface>();
		mRotation = ROTATION_0;
		mConfiguredSize = surfaceSize;
		mIsDeferredConfig = true;
	}

	public void setDynamicRangeProfile(long profile) {
		mDynamicRangeProfile = profile;
	}

	public long getDynamicRangeProfile() {
		return mDynamicRangeProfile;
	}

	public void addSurface(Surface surface) {
		checkNotNull(surface, "Surface must not be null");
		if (mSurfaces.contains(surface)) {
			throw new IllegalStateException("Surface is already added!");
		}
		if (mSurfaces.size() == 1 && !mIsShared) {
			throw new IllegalStateException("Cannot have 2 surfaces for a non-sharing configuration");
		}
		if (mSurfaces.size() + 1 > MAX_SURFACES_COUNT) {
			throw new IllegalArgumentException("Exceeds maximum number of surfaces");
		}
		mSurfaces.add(surface);
	}

	public void enableSurfaceSharing() {
		if (mIsMultiResolution) {
			throw new IllegalStateException("Cannot enable surface sharing on "
					+ "multi-resolution output configurations");
		}
		mIsShared = true;
	}

	public void setPhysicalCameraId(String physicalCameraId) {
		mPhysicalCameraId = physicalCameraId;
	}

	public void addSensorPixelModeUsed(int sensorPixelModeUsed) {
		if (sensorPixelModeUsed != 0 /*CameraMetadata.SENSOR_PIXEL_MODE_DEFAULT*/ &&
				sensorPixelModeUsed != 1 /*CameraMetadata.SENSOR_PIXEL_MODE_MAXIMUM_RESOLUTION*/) {
			throw new IllegalArgumentException("Not a valid sensor pixel mode " +
					sensorPixelModeUsed);
		}

		if (mSensorPixelModesUsed.contains(sensorPixelModeUsed)) {
			return;
		}
		mSensorPixelModesUsed.add(sensorPixelModeUsed);
	}

	public void removeSensorPixelModeUsed(int sensorPixelModeUsed) {
	  if (!mSensorPixelModesUsed.remove(Integer.valueOf(sensorPixelModeUsed))) {
			throw new IllegalArgumentException("sensorPixelMode " + sensorPixelModeUsed +
					"is not part of this output configuration");
	  }
	}

	public boolean isForPhysicalCamera() {
		return (mPhysicalCameraId != null);
	}

	public boolean isDeferredConfiguration() {
		return mIsDeferredConfig;
	}

	public void removeSurface(Surface surface) {
		if (getSurface() == surface) {
			throw new IllegalArgumentException(
					"Cannot remove surface associated with this output configuration");
		}
		if (!mSurfaces.remove(surface)) {
			throw new IllegalArgumentException("Surface is not part of this output configuration");
		}
	}

	public void setStreamUseCase(long streamUseCase) {
		long maxUseCaseValue = 0x6 /*CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_CROPPED_RAW*/;
		if (streamUseCase > maxUseCaseValue &&
				streamUseCase < 0x10000 /*CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_VENDOR_START*/) {
			throw new IllegalArgumentException("Not a valid stream use case value " +
					streamUseCase);
		}

		mStreamUseCase = streamUseCase;
	}

	public long getStreamUseCase() {
		return mStreamUseCase;
	}

	public void setTimestampBase(int timestampBase) {
		if (timestampBase < TIMESTAMP_BASE_DEFAULT ||
				timestampBase > TIMESTAMP_BASE_READOUT_SENSOR) {
			throw new IllegalArgumentException("Not a valid timestamp base value " +
					timestampBase);
		}

		if (timestampBase == TIMESTAMP_BASE_READOUT_SENSOR) {
			mTimestampBase = TIMESTAMP_BASE_SENSOR;
			mReadoutTimestampEnabled = true;
			mIsReadoutSensorTimestampBase = true;
		} else {
			mTimestampBase = timestampBase;
			mIsReadoutSensorTimestampBase = false;
		}
	}

	public int getTimestampBase() {
		if (mIsReadoutSensorTimestampBase) {
			return TIMESTAMP_BASE_READOUT_SENSOR;
		} else {
			return mTimestampBase;
		}
	}

	public void setMirrorMode(int mirrorMode) {
		if (mirrorMode < MIRROR_MODE_AUTO ||
				mirrorMode > MIRROR_MODE_V) {
			throw new IllegalArgumentException("Not a valid mirror mode " + mirrorMode);
		}
		mMirrorMode = mirrorMode;
	}

	public int getMirrorMode() {
		return mMirrorMode;
	}

	/* android-16 OutputConfiguration color space (the constructors start at UNSPECIFIED) */
	private int mColorSpace = ColorSpaceProfiles.UNSPECIFIED;

	public void setColorSpace(android.graphics.ColorSpace.Named colorSpace) {
		mColorSpace = colorSpace.ordinal();
	}

	public void clearColorSpace() {
		mColorSpace = ColorSpaceProfiles.UNSPECIFIED;
	}

	public android.graphics.ColorSpace getColorSpace() {
		if (mColorSpace != ColorSpaceProfiles.UNSPECIFIED) {
			return android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.values()[mColorSpace]);
		} else {
			return null;
		}
	}

	public void setReadoutTimestampEnabled(boolean on) {
		mReadoutTimestampEnabled = on;
	}

	public boolean isReadoutTimestampEnabled() {
		return mReadoutTimestampEnabled;
	}

	public int getMaxSharedSurfaceCount() {
		return MAX_SURFACES_COUNT;
	}

	public Surface getSurface() {
		if (mSurfaces.size() == 0) {
			return null;
		}

		return mSurfaces.get(0);
	}

	public List<Surface> getSurfaces() {
		return Collections.unmodifiableList(mSurfaces);
	}

	public int getRotation() {
		return mRotation;
	}

	public int getSurfaceGroupId() {
		return mSurfaceGroupId;
	}
	/* AOSP equals() over the fields ATL keeps */
	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (this == obj)
			return true;
		if (!(obj instanceof OutputConfiguration))
			return false;
		final OutputConfiguration other = (OutputConfiguration)obj;
		if (mRotation != other.mRotation || !java.util.Objects.equals(mConfiguredSize, other.mConfiguredSize) ||
		    mSurfaceGroupId != other.mSurfaceGroupId || mIsDeferredConfig != other.mIsDeferredConfig ||
		    mIsShared != other.mIsShared || !java.util.Objects.equals(mPhysicalCameraId, other.mPhysicalCameraId) ||
		    mIsMultiResolution != other.mIsMultiResolution || mStreamUseCase != other.mStreamUseCase ||
		    mTimestampBase != other.mTimestampBase || mMirrorMode != other.mMirrorMode ||
		    mReadoutTimestampEnabled != other.mReadoutTimestampEnabled ||
		    !mSensorPixelModesUsed.equals(other.mSensorPixelModesUsed) ||
		    mDynamicRangeProfile != other.mDynamicRangeProfile)
			return false;
		/* a deferred configuration compares without its surfaces */
		return mIsDeferredConfig || mSurfaces.equals(other.mSurfaces);
	}

	@Override
	public int hashCode() {
		return java.util.Objects.hash(mRotation, mIsDeferredConfig ? null : mSurfaces, mConfiguredSize, mSurfaceGroupId,
		                              mIsShared, mPhysicalCameraId, mIsMultiResolution, mSensorPixelModesUsed,
		                              mDynamicRangeProfile, mStreamUseCase, mTimestampBase, mMirrorMode,
		                              mReadoutTimestampEnabled);
	}
}
