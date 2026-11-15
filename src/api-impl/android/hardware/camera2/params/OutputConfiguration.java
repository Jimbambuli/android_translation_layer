package android.hardware.camera2.params;

import static com.android.internal.util.Preconditions.*;

import android.graphics.SurfaceTexture;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/* AOSP core/java/android/hardware/camera2/params/OutputConfiguration.java, reduced: the Parcel carries
 * ATL's fields only and there is no MultiResolutionImageReader (ATL has no ImageReader). ATL cannot query a Surface's size, format and dataspace
 * (SurfaceUtils), so a surface-backed configuration records no configured size and addSurface() skips
 * the format and dataspace match; the configuration is only consumed by a CameraDevice, which ATL never
 * opens. */
public final class OutputConfiguration implements android.os.Parcelable {
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
	private ArrayList<Integer> mMirrorModeForSurfaces = new ArrayList<Integer>();
	private long mUsage;
	private boolean mReadoutTimestampEnabled;
	private boolean mIsReadoutSensorTimestampBase;
	/* AOSP mPublicFormat: ImageFormat.PRIVATE for surface and deferred configurations (SurfaceUtils.getSurfaceFormat()
	 * of a display surface is HAL_PIXEL_FORMAT_IMPLEMENTATION_DEFINED), the given format otherwise */
	private int mConfiguredFormat = 0x22 /*ImageFormat.PRIVATE*/;

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
		mMirrorModeForSurfaces.add(mMirrorMode);
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

	/* AOSP OutputConfiguration(int format, Size surfaceSize[, long usage]) for an ImageReader surface to be
	 * added later with addSurface() (the format is not recorded, see the class comment) */
	public OutputConfiguration(int format, Size surfaceSize) {
		this(format, surfaceSize, format == 0x22 /*ImageFormat.PRIVATE*/ ? 0 : 3 /*HardwareBuffer.USAGE_CPU_READ_OFTEN*/);
	}

	public OutputConfiguration(int surfaceGroupId, int format, Size surfaceSize) {
		this(surfaceGroupId, format, surfaceSize,
				format == 0x22 /*ImageFormat.PRIVATE*/ ? 0 : 3 /*HardwareBuffer.USAGE_CPU_READ_OFTEN*/);
	}

	public OutputConfiguration(int format, Size surfaceSize, long usage) {
		this(SURFACE_GROUP_ID_NONE, format, surfaceSize, usage);
	}

	public OutputConfiguration(int surfaceGroupId, int format, Size surfaceSize, long usage) {
		checkNotNull(surfaceSize, "surfaceSize must not be null");
		if (surfaceSize.getWidth() == 0 || surfaceSize.getHeight() == 0)
			throw new IllegalArgumentException("Surface size needs to be non-zero");
		mSurfaceGroupId = surfaceGroupId;
		mSurfaces = new ArrayList<Surface>();
		mRotation = ROTATION_0;
		mConfiguredSize = surfaceSize;
		mConfiguredFormat = format;
		mIsDeferredConfig = false;
		mUsage = usage;
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
		mMirrorModeForSurfaces.add(mMirrorMode);
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
		int surfaceIndex = mSurfaces.indexOf(surface);
		if (surfaceIndex == -1) {
			throw new IllegalArgumentException("Surface is not part of this output configuration");
		}
		mSurfaces.remove(surfaceIndex);
		mMirrorModeForSurfaces.remove(surfaceIndex);
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
		for (int j = 0; j < mMirrorModeForSurfaces.size(); j++) {
			mMirrorModeForSurfaces.set(j, mirrorMode);
		}
	}

	public int getMirrorMode() {
		return mMirrorMode;
	}

	public void setMirrorMode(Surface surface, int mirrorMode) {
		checkNotNull(surface, "Surface must not be null");
		if (mirrorMode < MIRROR_MODE_AUTO || mirrorMode > MIRROR_MODE_V) {
			throw new IllegalArgumentException("Not a valid mirror mode " + mirrorMode);
		}
		int surfaceIndex = mSurfaces.indexOf(surface);
		if (surfaceIndex == -1) {
			throw new IllegalArgumentException("Surface not part of the OutputConfiguration");
		}
		mMirrorModeForSurfaces.set(surfaceIndex, mirrorMode);
	}

	public int getMirrorMode(Surface surface) {
		checkNotNull(surface, "Surface must not be null");
		int surfaceIndex = mSurfaces.indexOf(surface);
		if (surfaceIndex == -1) {
			throw new IllegalArgumentException("Surface not part of the OutputConfiguration");
		}
		return mMirrorModeForSurfaces.get(surfaceIndex);
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
	/* AOSP OutputConfiguration.getConfiguredSize(): null for a surface-backed configuration here, because ATL cannot
	 * query a Surface's size (see the class comment) */
	public Size getConfiguredSize() {
		return mConfiguredSize;
	}

	/* AOSP getConfiguredFormat() = mPublicFormat */
	public int getConfiguredFormat() {
		return mConfiguredFormat;
	}

	private static final java.util.concurrent.atomic.AtomicInteger sNextMultiResolutionGroupId =
	    new java.util.concurrent.atomic.AtomicInteger(0);

	/* AOSP createInstancesForMultiResolutionOutput(Collection<MultiResolutionStreamInfo>, int) */
	public static List<OutputConfiguration> createInstancesForMultiResolutionOutput(
	    java.util.Collection<MultiResolutionStreamInfo> streams, int format) {
		if (streams == null || streams.size() <= 1) {
			throw new IllegalArgumentException("The streams list must contain at least 2 entries");
		}
		if (format == 0x11 /*ImageFormat.NV21*/) {
			throw new IllegalArgumentException("NV21 format is not supported");
		}
		int groupId = sNextMultiResolutionGroupId.getAndIncrement();
		ArrayList<OutputConfiguration> configs = new ArrayList<OutputConfiguration>();
		for (MultiResolutionStreamInfo stream : streams) {
			Size surfaceSize = new Size(stream.getWidth(), stream.getHeight());
			OutputConfiguration config = new OutputConfiguration(groupId, format, surfaceSize);
			config.setPhysicalCameraId(stream.getPhysicalCameraId());
			config.mIsMultiResolution = true;
			configs.add(config);
		}
		return configs;
	}

	public int describeContents() {
		return 0;
	}

	public void writeToParcel(android.os.Parcel dest, int flags) {
		dest.writeInt(mRotation);
		dest.writeInt(mSurfaceGroupId);
		dest.writeInt(mConfiguredSize != null ? mConfiguredSize.getWidth() : 0);
		dest.writeInt(mConfiguredSize != null ? mConfiguredSize.getHeight() : 0);
		dest.writeInt(mConfiguredFormat);
		dest.writeInt(mIsDeferredConfig ? 1 : 0);
		dest.writeInt(mIsShared ? 1 : 0);
		dest.writeParcelableArray(mSurfaces.toArray(new Surface[0]), flags);
		dest.writeString(mPhysicalCameraId);
		dest.writeInt(mIsMultiResolution ? 1 : 0);
		dest.writeInt(mSensorPixelModesUsed.size());
		for (int mode : mSensorPixelModesUsed)
			dest.writeInt(mode);
		dest.writeLong(mDynamicRangeProfile);
		dest.writeInt(mColorSpace);
		dest.writeLong(mStreamUseCase);
		dest.writeInt(mTimestampBase);
		dest.writeInt(mMirrorMode);
		dest.writeInt(mMirrorModeForSurfaces.size());
		for (int mode : mMirrorModeForSurfaces)
			dest.writeInt(mode);
		dest.writeInt(mReadoutTimestampEnabled ? 1 : 0);
		dest.writeLong(mUsage);
	}

	private OutputConfiguration(android.os.Parcel source) {
		mRotation = source.readInt();
		mSurfaceGroupId = source.readInt();
		int width = source.readInt();
		int height = source.readInt();
		mConfiguredSize = (width != 0 && height != 0) ? new Size(width, height) : null;
		mConfiguredFormat = source.readInt();
		mIsDeferredConfig = source.readInt() == 1;
		mIsShared = source.readInt() == 1;
		mSurfaces = new ArrayList<Surface>();
		try {
			for (android.os.Parcelable surface : source.readParcelableArray(Surface.class.getClassLoader()))
				mSurfaces.add((Surface)surface);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
		mPhysicalCameraId = source.readString();
		mIsMultiResolution = source.readInt() == 1;
		for (int n = source.readInt(); n > 0; n--)
			mSensorPixelModesUsed.add(source.readInt());
		mDynamicRangeProfile = source.readLong();
		mColorSpace = source.readInt();
		mStreamUseCase = source.readLong();
		mTimestampBase = source.readInt();
		mMirrorMode = source.readInt();
		for (int n = source.readInt(); n > 0; n--)
			mMirrorModeForSurfaces.add(source.readInt());
		mReadoutTimestampEnabled = source.readInt() == 1;
		mUsage = source.readLong();
	}

	public static final android.os.Parcelable.Creator<OutputConfiguration> CREATOR =
	    new android.os.Parcelable.Creator<OutputConfiguration>() {
		    public OutputConfiguration createFromParcel(android.os.Parcel source) {
			    return new OutputConfiguration(source);
		    }

		    public OutputConfiguration[] newArray(int size) {
			    return new OutputConfiguration[size];
		    }
	    };

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
		    mReadoutTimestampEnabled != other.mReadoutTimestampEnabled || mUsage != other.mUsage ||
		    !mMirrorModeForSurfaces.equals(other.mMirrorModeForSurfaces) ||
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
