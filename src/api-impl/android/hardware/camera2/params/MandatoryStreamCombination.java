/* AOSP MandatoryStreamCombination.java (Android 16), including the hidden Builder.
 * Difference: Builder's isHiddenPhysicalCamera is false (ATL has no camera service); @StreamUseCase annotation omitted. */


package android.hardware.camera2.params;

import static android.hardware.camera2.params.StreamConfigurationMap.checkArgumentFormat;

import android.graphics.ImageFormat;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.params.OutputConfiguration;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.hardware.camera2.utils.HashCodeHelpers;
import android.media.CamcorderProfile;
import android.util.Log;
import android.util.Pair;
import android.util.Size;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public final class MandatoryStreamCombination {
	private static final String TAG = "MandatoryStreamCombination";

	public static final class MandatoryStreamInformation {
		private final int mFormat;
		private final ArrayList<Size> mAvailableSizes = new ArrayList<Size> ();
		private final boolean mIsInput;
		private final boolean mIsUltraHighResolution;
		private final boolean mIsMaximumSize;
		private final boolean mIs10BitCapable;
		private final long mStreamUseCase;

		public MandatoryStreamInformation(List<Size> availableSizes, int format,
				boolean isMaximumSize) {
			this(availableSizes, format, isMaximumSize, false,
					false);
		}

		public MandatoryStreamInformation(List<Size> availableSizes, int format,
				boolean isMaximumSize, boolean isInput) {
			this(availableSizes, format, isMaximumSize, isInput,
					 false);
		}

		public MandatoryStreamInformation(List<Size> availableSizes, int format,
				boolean isMaximumSize, boolean isInput, boolean isUltraHighResolution) {
			this(availableSizes, format, isMaximumSize, isInput, isUltraHighResolution,
					 false);
		}

		public MandatoryStreamInformation(List<Size> availableSizes, int format,
				boolean isMaximumSize, boolean isInput, boolean isUltraHighResolution,
				boolean is10BitCapable) {
			this(availableSizes, format, isMaximumSize, isInput, isUltraHighResolution,
					is10BitCapable, CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_DEFAULT);
		}

		public MandatoryStreamInformation(List<Size> availableSizes, int format,
				boolean isMaximumSize, boolean isInput, boolean isUltraHighResolution,
				boolean is10BitCapable, long streamUseCase) {
			if (availableSizes.isEmpty()) {
				throw new IllegalArgumentException("No available sizes");
			}
			mAvailableSizes.addAll(availableSizes);
			mFormat = checkArgumentFormat(format);
			mIsMaximumSize = isMaximumSize;
			mIsInput = isInput;
			mIsUltraHighResolution = isUltraHighResolution;
			mIs10BitCapable = is10BitCapable;
			mStreamUseCase = streamUseCase;
		}

		public boolean isInput() {
			return mIsInput;
		}

		public boolean isUltraHighResolution() {
			return mIsUltraHighResolution;
		}

		public boolean isMaximumSize() {
			return mIsMaximumSize;
		}

		public boolean is10BitCapable() {
			return mIs10BitCapable;
		}

		public List<Size> getAvailableSizes() {
			return Collections.unmodifiableList(mAvailableSizes);
		}

		public int getFormat() {
			// P010 YUV streams must be supported along with SDR 8-bit YUV streams
			if ((mIs10BitCapable)  && (mFormat == ImageFormat.YCBCR_P010)) {
				return ImageFormat.YUV_420_888;
			}
			return mFormat;
		}

		public int get10BitFormat() {
			if (!mIs10BitCapable) {
				throw new UnsupportedOperationException("10-bit output is not supported!");
			}
			return mFormat;
		}

		public long getStreamUseCase() {
			return mStreamUseCase;
		}

		@Override
		public boolean equals(final Object obj) {
			if (obj == null) {
				return false;
			}
			if (this == obj) {
				return true;
			}
			if (obj instanceof MandatoryStreamInformation) {
				final MandatoryStreamInformation other = (MandatoryStreamInformation) obj;
				if ((mFormat != other.mFormat) || (mIsInput != other.mIsInput) ||
						(mIsUltraHighResolution != other.mIsUltraHighResolution) ||
						(mStreamUseCase != other.mStreamUseCase) ||
						(mAvailableSizes.size() != other.mAvailableSizes.size())) {
					return false;
				}

				return mAvailableSizes.equals(other.mAvailableSizes);
			}

			return false;
		}

		@Override
		public int hashCode() {
			return HashCodeHelpers.hashCode(mFormat, Boolean.hashCode(mIsInput),
					Boolean.hashCode(mIsUltraHighResolution), mAvailableSizes.hashCode(),
					mStreamUseCase);
		}
	}

	private final String mDescription;
	private final boolean mIsReprocessable;
	private final ArrayList<MandatoryStreamInformation> mStreamsInformation =
			new ArrayList<MandatoryStreamInformation>();

	private static final long STREAM_USE_CASE_PREVIEW =
			CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW;
	private static final long STREAM_USE_CASE_STILL_CAPTURE =
			CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_STILL_CAPTURE;
	private static final long STREAM_USE_CASE_RECORD =
			CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_VIDEO_RECORD;
	private static final long STREAM_USE_CASE_PREVIEW_VIDEO_STILL =
			CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW_VIDEO_STILL;
	private static final long STREAM_USE_CASE_VIDEO_CALL =
			CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_VIDEO_CALL;
	private static final long STREAM_USE_CASE_CROPPED_RAW =
			CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_CROPPED_RAW;

	public MandatoryStreamCombination(List<MandatoryStreamInformation> streamsInformation,
			String description, boolean isReprocessable) {
		if (streamsInformation.isEmpty()) {
			throw new IllegalArgumentException("Empty stream information");
		}
		mStreamsInformation.addAll(streamsInformation);
		mDescription = description;
		mIsReprocessable = isReprocessable;
	}

	public CharSequence getDescription() {
		return mDescription;
	}

	public boolean isReprocessable() {
		return mIsReprocessable;
	}

	public List<MandatoryStreamInformation> getStreamsInformation() {
		return Collections.unmodifiableList(mStreamsInformation);
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj == null) {
			return false;
		}
		if (this == obj) {
			return true;
		}
		if (obj instanceof MandatoryStreamCombination) {
			final MandatoryStreamCombination other = (MandatoryStreamCombination) obj;
			if ((mDescription != other.mDescription) ||
					(mIsReprocessable != other.mIsReprocessable) ||
					(mStreamsInformation.size() != other.mStreamsInformation.size())) {
				return false;
			}

			return mStreamsInformation.equals(other.mStreamsInformation);
		}

		return false;
	}

	@Override
	public int hashCode() {
		return HashCodeHelpers.hashCode(Boolean.hashCode(mIsReprocessable), mDescription.hashCode(),
				mStreamsInformation.hashCode());
	}

	private static enum SizeThreshold { VGA, PREVIEW, RECORD, MAXIMUM, s720p, s1440p, FULL_RES }
	private static enum ReprocessType { NONE, PRIVATE, YUV, REMOSAIC }
	private static final class StreamTemplate {
		public int mFormat;
		public SizeThreshold mSizeThreshold;
		public long mStreamUseCase;
		public StreamTemplate(int format, SizeThreshold sizeThreshold) {
			this(format, sizeThreshold, CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_DEFAULT);
		}
		public StreamTemplate(int format, SizeThreshold sizeThreshold,
				long streamUseCase) {
			mFormat = format;
			mSizeThreshold = sizeThreshold;
			mStreamUseCase = streamUseCase;
		}
	}

	private static final class StreamCombinationTemplate {
		public StreamTemplate[] mStreamTemplates;
		public String mDescription;
		public ReprocessType mReprocessType;
		// Substitute MAXIMUM size YUV output stream with JPEG / RAW_SENSOR.
		public boolean mSubstituteYUV = false;

		public StreamCombinationTemplate(StreamTemplate[] streamTemplates,
				String description) {
			this(streamTemplates, description, ReprocessType.NONE);
		}

		public StreamCombinationTemplate(StreamTemplate[] streamTemplates,
				String description, ReprocessType reprocessType) {
			this(streamTemplates, description, reprocessType,  false);
		}

		public StreamCombinationTemplate(StreamTemplate[] streamTemplates,
				String description, boolean substituteYUV) {
			this(streamTemplates, description,  ReprocessType.NONE,
					substituteYUV);
		}

		public StreamCombinationTemplate(StreamTemplate[] streamTemplates,
				String description, ReprocessType reprocessType, boolean substituteYUV) {
			mStreamTemplates = streamTemplates;
			mReprocessType = reprocessType;
			mDescription = description;
			mSubstituteYUV = substituteYUV;
		}
	}

	private static StreamCombinationTemplate sLegacyCombinations[] = {
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.MAXIMUM) },
				"Simple preview, GPU video processing, or no-preview video recording"),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.JPEG, SizeThreshold.MAXIMUM) },
				"No-viewfinder still image capture"),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.MAXIMUM) },
				"In-application video/image processing"),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.PREVIEW),
				new StreamTemplate(ImageFormat.JPEG, SizeThreshold.MAXIMUM) },
				"Standard still imaging"),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.PREVIEW),
				new StreamTemplate(ImageFormat.JPEG, SizeThreshold.MAXIMUM) },
				"In-app processing plus still capture"),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.PREVIEW),
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.PREVIEW) },
				"Standard recording"),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.PREVIEW),
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.PREVIEW) },
				"Preview plus in-app processing"),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.PREVIEW),
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.PREVIEW),
				new StreamTemplate(ImageFormat.JPEG, SizeThreshold.MAXIMUM) },
				"Still capture plus in-app processing")
	};

	private static StreamCombinationTemplate sLimitedCombinations[] = {
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.PREVIEW),
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.RECORD)},
				"High-resolution video recording with preview"),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.PREVIEW),
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.RECORD)},
				"High-resolution in-app video processing with preview"),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.PREVIEW),
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.RECORD) },
				"Two-input in-app video processing"),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.PREVIEW),
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.RECORD),
				new StreamTemplate(ImageFormat.JPEG, SizeThreshold.RECORD) },
				"High-resolution recording with video snapshot"),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.PREVIEW),
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.RECORD),
				new StreamTemplate(ImageFormat.JPEG, SizeThreshold.RECORD) },
				"High-resolution in-app processing with video snapshot"),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.PREVIEW),
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.PREVIEW),
				new StreamTemplate(ImageFormat.JPEG, SizeThreshold.MAXIMUM) },
				"Two-input in-app processing with still capture")
	};

	private static StreamCombinationTemplate sBurstCombinations[] = {
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.PREVIEW),
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.MAXIMUM) },
				"Maximum-resolution GPU processing with preview"),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.PREVIEW),
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.MAXIMUM) },
				"Maximum-resolution in-app processing with preview"),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.PREVIEW),
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.MAXIMUM) },
				"Maximum-resolution two-input in-app processsing")
	};

	private static StreamCombinationTemplate sFullCombinations[] = {
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.MAXIMUM),
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.MAXIMUM) },
				"Maximum-resolution GPU processing with preview"),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.MAXIMUM),
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.MAXIMUM) },
				"Maximum-resolution in-app processing with preview"),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.MAXIMUM),
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.MAXIMUM) },
				"Maximum-resolution two-input in-app processing"),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.PREVIEW),
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.PREVIEW),
				new StreamTemplate(ImageFormat.JPEG, SizeThreshold.MAXIMUM) },
				"Video recording with maximum-size video snapshot"),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.VGA),
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.PREVIEW),
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.MAXIMUM) },
				"Standard video recording plus maximum-resolution in-app processing"),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.VGA),
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.PREVIEW),
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.MAXIMUM) },
				"Preview plus two-input maximum-resolution in-app processing")
	};

	private static StreamCombinationTemplate sRawCombinations[] = {
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.RAW_SENSOR,  SizeThreshold.MAXIMUM) },
				"No-preview DNG capture"),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.PREVIEW),
				new StreamTemplate(ImageFormat.RAW_SENSOR, SizeThreshold.MAXIMUM) },
				"Standard DNG capture"),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.PREVIEW),
				new StreamTemplate(ImageFormat.RAW_SENSOR, SizeThreshold.MAXIMUM) },
				"In-app processing plus DNG capture"),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.PREVIEW),
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.PREVIEW),
				new StreamTemplate(ImageFormat.RAW_SENSOR, SizeThreshold.MAXIMUM) },
				"Video recording with DNG capture"),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.PREVIEW),
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.PREVIEW),
				new StreamTemplate(ImageFormat.RAW_SENSOR, SizeThreshold.MAXIMUM) },
				"Preview with in-app processing and DNG capture"),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.PREVIEW),
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.PREVIEW),
				new StreamTemplate(ImageFormat.RAW_SENSOR, SizeThreshold.MAXIMUM) },
				"Two-input in-app processing plus DNG capture"),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.PREVIEW),
				new StreamTemplate(ImageFormat.JPEG, SizeThreshold.MAXIMUM),
				new StreamTemplate(ImageFormat.RAW_SENSOR, SizeThreshold.MAXIMUM) },
				"Still capture with simultaneous JPEG and DNG"),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.PREVIEW),
				new StreamTemplate(ImageFormat.JPEG, SizeThreshold.MAXIMUM),
				new StreamTemplate(ImageFormat.RAW_SENSOR, SizeThreshold.MAXIMUM) },
				"In-app processing with simultaneous JPEG and DNG")
	};

	private static StreamCombinationTemplate sLevel3Combinations[] = {
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.PREVIEW),
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.VGA),
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.MAXIMUM),
				new StreamTemplate(ImageFormat.RAW_SENSOR, SizeThreshold.MAXIMUM) },
				"In-app viewfinder analysis with dynamic selection of output format"),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.PREVIEW),
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.VGA),
				new StreamTemplate(ImageFormat.JPEG, SizeThreshold.MAXIMUM),
				new StreamTemplate(ImageFormat.RAW_SENSOR, SizeThreshold.MAXIMUM) },
				"In-app viewfinder analysis with dynamic selection of output format")
	};

	private static StreamCombinationTemplate sLimitedPrivateReprocCombinations[] = {
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.JPEG, SizeThreshold.MAXIMUM) },
				"No-viewfinder still image reprocessing",
				 ReprocessType.PRIVATE),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.PREVIEW),
				new StreamTemplate(ImageFormat.JPEG, SizeThreshold.MAXIMUM) },
				"ZSL(Zero-Shutter-Lag) still imaging",
				 ReprocessType.PRIVATE),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.PREVIEW),
				new StreamTemplate(ImageFormat.JPEG, SizeThreshold.MAXIMUM) },
				"ZSL still and in-app processing imaging",
				 ReprocessType.PRIVATE),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.PREVIEW),
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.PREVIEW),
				new StreamTemplate(ImageFormat.JPEG, SizeThreshold.MAXIMUM) },
				"ZSL in-app processing with still capture",
				 ReprocessType.PRIVATE),
	};

	private static StreamCombinationTemplate sLimitedYUVReprocCombinations[] = {
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.JPEG, SizeThreshold.MAXIMUM) },
				"No-viewfinder still image reprocessing",
				 ReprocessType.YUV),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.PREVIEW),
				new StreamTemplate(ImageFormat.JPEG, SizeThreshold.MAXIMUM) },
				"ZSL(Zero-Shutter-Lag) still imaging",
				 ReprocessType.YUV),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.PREVIEW),
				new StreamTemplate(ImageFormat.JPEG, SizeThreshold.MAXIMUM) },
				"ZSL still and in-app processing imaging",
				 ReprocessType.YUV),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.PREVIEW),
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.PREVIEW),
				new StreamTemplate(ImageFormat.JPEG, SizeThreshold.MAXIMUM) },
				"ZSL in-app processing with still capture",
				 ReprocessType.YUV),
	};

	private static StreamCombinationTemplate sFullPrivateReprocCombinations[] = {
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.PREVIEW),
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.RECORD) },
				"High-resolution ZSL in-app video processing with regular preview",
				 ReprocessType.PRIVATE),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.PREVIEW),
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.MAXIMUM) },
				"Maximum-resolution ZSL in-app processing with regular preview",
				 ReprocessType.PRIVATE),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.PREVIEW),
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.MAXIMUM) },
				"Maximum-resolution two-input ZSL in-app processing",
				 ReprocessType.PRIVATE),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.PREVIEW),
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.PREVIEW),
				new StreamTemplate(ImageFormat.JPEG, SizeThreshold.MAXIMUM) },
				"ZSL still capture and in-app processing",
				 ReprocessType.PRIVATE),
	};

	private static StreamCombinationTemplate sFullYUVReprocCombinations[] = {
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.PREVIEW) },
				"Maximum-resolution multi-frame image fusion in-app processing with regular "
				+ "preview",
				 ReprocessType.YUV),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.PREVIEW) },
				"Maximum-resolution multi-frame image fusion two-input in-app processing",
				 ReprocessType.YUV),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.PREVIEW),
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.RECORD) },
				"High-resolution ZSL in-app video processing with regular preview",
				 ReprocessType.YUV),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.PREVIEW),
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.PREVIEW),
				new StreamTemplate(ImageFormat.JPEG, SizeThreshold.MAXIMUM) },
				"ZSL still capture and in-app processing",
				 ReprocessType.YUV),
	};

	private static StreamCombinationTemplate sRAWPrivateReprocCombinations[] = {
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.PREVIEW),
				new StreamTemplate(ImageFormat.RAW_SENSOR, SizeThreshold.MAXIMUM) },
				"Mutually exclusive ZSL in-app processing and DNG capture",
				 ReprocessType.PRIVATE),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.PREVIEW),
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.PREVIEW),
				new StreamTemplate(ImageFormat.RAW_SENSOR, SizeThreshold.MAXIMUM) },
				"Mutually exclusive ZSL in-app processing and preview with DNG capture",
				 ReprocessType.PRIVATE),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.PREVIEW),
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.PREVIEW),
				new StreamTemplate(ImageFormat.RAW_SENSOR, SizeThreshold.MAXIMUM) },
				"Mutually exclusive ZSL two-input in-app processing and DNG capture",
				 ReprocessType.PRIVATE),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.PREVIEW),
				new StreamTemplate(ImageFormat.JPEG, SizeThreshold.MAXIMUM),
				new StreamTemplate(ImageFormat.RAW_SENSOR, SizeThreshold.MAXIMUM) },
				"Mutually exclusive ZSL still capture and preview with DNG capture",
				 ReprocessType.PRIVATE),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.PREVIEW),
				new StreamTemplate(ImageFormat.JPEG, SizeThreshold.MAXIMUM),
				new StreamTemplate(ImageFormat.RAW_SENSOR, SizeThreshold.MAXIMUM) },
				"Mutually exclusive ZSL in-app processing with still capture and DNG capture",
				 ReprocessType.PRIVATE),
	};

	private static StreamCombinationTemplate sRAWYUVReprocCombinations[] = {
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.PREVIEW),
				new StreamTemplate(ImageFormat.RAW_SENSOR, SizeThreshold.MAXIMUM) },
				"Mutually exclusive ZSL in-app processing and DNG capture",
				 ReprocessType.YUV),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.PREVIEW),
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.PREVIEW),
				new StreamTemplate(ImageFormat.RAW_SENSOR, SizeThreshold.MAXIMUM) },
				"Mutually exclusive ZSL in-app processing and preview with DNG capture",
				 ReprocessType.YUV),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.PREVIEW),
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.PREVIEW),
				new StreamTemplate(ImageFormat.RAW_SENSOR, SizeThreshold.MAXIMUM) },
				"Mutually exclusive ZSL two-input in-app processing and DNG capture",
				 ReprocessType.YUV),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.PREVIEW),
				new StreamTemplate(ImageFormat.JPEG, SizeThreshold.MAXIMUM),
				new StreamTemplate(ImageFormat.RAW_SENSOR, SizeThreshold.MAXIMUM) },
				"Mutually exclusive ZSL still capture and preview with DNG capture",
				 ReprocessType.YUV),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.PREVIEW),
				new StreamTemplate(ImageFormat.JPEG, SizeThreshold.MAXIMUM),
				new StreamTemplate(ImageFormat.RAW_SENSOR, SizeThreshold.MAXIMUM) },
				"Mutually exclusive ZSL in-app processing with still capture and DNG capture",
				 ReprocessType.YUV),
	};

	private static StreamCombinationTemplate sLevel3PrivateReprocCombinations[] = {
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.PREVIEW),
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.VGA),
				new StreamTemplate(ImageFormat.RAW_SENSOR, SizeThreshold.MAXIMUM),
				new StreamTemplate(ImageFormat.JPEG, SizeThreshold.MAXIMUM) },
				"In-app viewfinder analysis with ZSL, RAW, and JPEG reprocessing output",
				 ReprocessType.PRIVATE),
	};

	private static StreamCombinationTemplate sLevel3YUVReprocCombinations[] = {
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.PREVIEW),
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.VGA),
				new StreamTemplate(ImageFormat.RAW_SENSOR, SizeThreshold.MAXIMUM) },
				"In-app viewfinder analysis with ZSL and RAW",
				 ReprocessType.YUV),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.PREVIEW),
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.VGA),
				new StreamTemplate(ImageFormat.RAW_SENSOR, SizeThreshold.MAXIMUM),
				new StreamTemplate(ImageFormat.JPEG, SizeThreshold.MAXIMUM) },
				"In-app viewfinder analysis with ZSL, RAW, and JPEG reprocessing output",
				 ReprocessType.YUV),
	};

	private static StreamCombinationTemplate sConcurrentStreamCombinations[] = {
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.s1440p) },
				"In-app video / image processing"),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.s1440p) },
				"preview / preview to GPU"),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.JPEG, SizeThreshold.s1440p) },
				"No view-finder still image capture"),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.s720p),
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.s1440p)},
				"Two-input in app video / image processing"),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.s720p),
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.s1440p)},
				"High resolution video recording with preview"),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.s720p),
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.s1440p)},
				"In-app video / image processing with preview"),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.s720p),
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.s1440p)},
				"In-app video / image processing with preview"),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.s720p),
				new StreamTemplate(ImageFormat.JPEG, SizeThreshold.s1440p)},
				"Standard still image capture"),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.s720p),
				new StreamTemplate(ImageFormat.JPEG, SizeThreshold.s1440p)},
				"Standard still image capture"),
	};

	private static StreamCombinationTemplate sConcurrentDepthOnlyStreamCombinations[] = {
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.DEPTH16, SizeThreshold.VGA) },
				"Depth capture for mesh based object rendering"),
	};

	private static StreamCombinationTemplate sUltraHighResolutionStreamCombinations[] = {
		// UH res YUV / RAW / JPEG + PRIV preview size stream
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.FULL_RES),
				 new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.PREVIEW)},
				"Ultra high resolution YUV image capture with preview"),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.RAW_SENSOR, SizeThreshold.FULL_RES),
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.PREVIEW)},
				"Ultra high resolution RAW_SENSOR image capture with preview"),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.JPEG, SizeThreshold.FULL_RES),
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.PREVIEW)},
				"Ultra high resolution JPEG image capture with preview"),

		// UH res YUV / RAW / JPEG + YUV preview size stream
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.FULL_RES),
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.PREVIEW)},
				"No-viewfinder Ultra high resolution YUV image capture with image analysis"),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.RAW_SENSOR, SizeThreshold.FULL_RES),
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.PREVIEW)},
				"No-viewfinder Ultra high resolution RAW_SENSOR image capture with image analysis"),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.JPEG, SizeThreshold.FULL_RES),
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.PREVIEW)},
				"No-viewfinder Ultra high resolution JPEG image capture with image analysis"),

		// UH res YUV / RAW / JPEG + PRIV preview + PRIV RECORD stream
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.FULL_RES),
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.PREVIEW),
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.RECORD)},
				"Ultra high resolution YUV image capture with preview + app-based image analysis"),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.RAW_SENSOR, SizeThreshold.FULL_RES),
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.PREVIEW),
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.RECORD)},
				"Ultra high resolution RAW image capture with preview + app-based image analysis"),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.JPEG, SizeThreshold.FULL_RES),
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.PREVIEW),
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.RECORD)},
				"Ultra high resolution JPEG image capture with preview + app-based image analysis"),

		// UH res YUV / RAW / JPEG + PRIV preview + YUV RECORD stream
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.FULL_RES),
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.PREVIEW),
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.RECORD)},
				"Ultra high resolution YUV image capture with preview + app-based image analysis"),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.RAW_SENSOR, SizeThreshold.FULL_RES),
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.PREVIEW),
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.RECORD)},
				"Ultra high resolution RAW image capture with preview + app-based image analysis"),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.JPEG, SizeThreshold.FULL_RES),
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.PREVIEW),
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.RECORD)},
				"Ultra high resolution JPEG image capture with preview + app-based image analysis"),

		// UH RES YUV / RAW / JPEG + PRIV preview + YUV / RAW / JPEG Maximum stream
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.FULL_RES),
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.PREVIEW),
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.MAXIMUM)},
				"Ultra high resolution YUV image capture with preview + default",
				 true),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.RAW_SENSOR, SizeThreshold.FULL_RES),
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.PREVIEW),
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.MAXIMUM)},
				"Ultra high resolution RAW image capture with preview + default",
				 true),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.JPEG, SizeThreshold.FULL_RES),
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.PREVIEW),
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.MAXIMUM)},
				"Ultra high resolution JPEG capture with preview + default",
				 true),
	};

	private static StreamCombinationTemplate sUltraHighResolutionReprocStreamCombinations[] = {
		// RAW_SENSOR -> RAW_SENSOR + preview size PRIV / YUV
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.PREVIEW)},
				"In-app RAW remosaic reprocessing with separate preview",
				 ReprocessType.REMOSAIC),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.PREVIEW)},
				"In-app RAW remosaic reprocessing with in-app image analysis",
				 ReprocessType.REMOSAIC),

		// RAW -> JPEG / YUV reprocessing + YUV / PRIV preview size stream
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.JPEG, SizeThreshold.FULL_RES),
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.PREVIEW)},
				"In-app RAW -> JPEG reprocessing with separate preview",
				 ReprocessType.REMOSAIC),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.FULL_RES),
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.PREVIEW)},
				"In-app RAW -> YUV reprocessing with separate preview",
				 ReprocessType.REMOSAIC),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.JPEG, SizeThreshold.FULL_RES),
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.PREVIEW)},
				"In-app RAW -> JPEG reprocessing with in-app image analysis",
				 ReprocessType.REMOSAIC),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.FULL_RES),
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.PREVIEW)},
				"In-app RAW -> YUV reprocessing with in-app image analysis",
				 ReprocessType.REMOSAIC),
	};

	private static StreamCombinationTemplate sUltraHighResolutionYUVReprocStreamCombinations[] = {
		// YUV -> JPEG reprocess + PRIV / YUV preview size stream
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.JPEG, SizeThreshold.FULL_RES),
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.PREVIEW)},
				"Ultra high resolution YUV -> JPEG reprocessing with separate preview",
				 ReprocessType.YUV),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.JPEG, SizeThreshold.FULL_RES),
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.PREVIEW)},
				"Ultra high resolution YUV -> JPEG reprocessing with in-app image analysis",
				 ReprocessType.YUV),

		// YUV -> YUV reprocess + PRIV / YUV preview size stream
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.FULL_RES),
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.PREVIEW)},
				"Ultra high resolution YUV -> YUV reprocessing with separate preview",
				 ReprocessType.YUV),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.FULL_RES),
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.PREVIEW)},
				"Ultra high resolution YUV -> YUV reprocessing with in-app image analysis",
				 ReprocessType.YUV),
	};

	private static StreamCombinationTemplate sUltraHighResolutionPRIVReprocStreamCombinations[] = {
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.JPEG, SizeThreshold.FULL_RES),
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.PREVIEW)},
				"Ultra high resolution PRIVATE -> JPEG reprocessing with separate preview",
				 ReprocessType.PRIVATE),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.JPEG, SizeThreshold.FULL_RES),
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.PREVIEW)},
				"Ultra high resolution PRIVATE -> JPEG reprocessing with in-app image analysis",
				 ReprocessType.PRIVATE),
	};

	private static StreamCombinationTemplate s10BitOutputStreamCombinations[] = {
			new StreamCombinationTemplate(new StreamTemplate [] {
					new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.MAXIMUM)},
					"Simple preview, GPU video processing, or no-preview video recording"),
			new StreamCombinationTemplate(new StreamTemplate [] {
					new StreamTemplate(ImageFormat.YCBCR_P010, SizeThreshold.MAXIMUM)},
					"In-application video/image processing"),
			new StreamCombinationTemplate(new StreamTemplate [] {
					new StreamTemplate(ImageFormat.JPEG, SizeThreshold.MAXIMUM),
					new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.PREVIEW)},
					"Standard still imaging"),
			new StreamCombinationTemplate(new StreamTemplate [] {
					new StreamTemplate(ImageFormat.YCBCR_P010, SizeThreshold.MAXIMUM),
					new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.PREVIEW)},
					"Maximum-resolution in-app processing with preview"),
			new StreamCombinationTemplate(new StreamTemplate [] {
					new StreamTemplate(ImageFormat.YCBCR_P010, SizeThreshold.MAXIMUM),
					new StreamTemplate(ImageFormat.YCBCR_P010, SizeThreshold.PREVIEW)},
					"Maximum-resolution two-input in-app processing"),
			new StreamCombinationTemplate(new StreamTemplate [] {
					new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.RECORD),
					new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.PREVIEW)},
					"High-resolution video recording with preview"),
			new StreamCombinationTemplate(new StreamTemplate [] {
					new StreamTemplate(ImageFormat.YCBCR_P010, SizeThreshold.RECORD),
					new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.RECORD),
					new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.PREVIEW)},
					"High-resolution recording with in-app snapshot"),
			new StreamCombinationTemplate(new StreamTemplate [] {
					new StreamTemplate(ImageFormat.JPEG, SizeThreshold.RECORD),
					new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.RECORD),
					new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.PREVIEW)},
					"High-resolution recording with video snapshot"),
	};

	private static StreamCombinationTemplate sStreamUseCaseCombinations[] = {
		// Single stream combinations
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.PREVIEW,
						STREAM_USE_CASE_PREVIEW) },
				"Simple preview"),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.PREVIEW,
						STREAM_USE_CASE_PREVIEW) },
				"Simple in-application image processing"),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.RECORD,
						STREAM_USE_CASE_RECORD) },
				"Simple video recording"),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.RECORD,
						STREAM_USE_CASE_RECORD) },
				"Simple in-application video processing"),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.JPEG, SizeThreshold.MAXIMUM,
						STREAM_USE_CASE_STILL_CAPTURE) },
				"Simple JPEG still capture"),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.MAXIMUM,
						STREAM_USE_CASE_STILL_CAPTURE) },
				"Simple YUV still capture"),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.s1440p,
						STREAM_USE_CASE_PREVIEW_VIDEO_STILL) },
				"Multi-purpose stream for preview, video and still capture"),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.s1440p,
						STREAM_USE_CASE_PREVIEW_VIDEO_STILL) },
				"Multi-purpose YUV stream for preview, video and still capture"),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.s1440p,
						STREAM_USE_CASE_VIDEO_CALL) },
				"Simple video call"),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.s1440p,
						STREAM_USE_CASE_VIDEO_CALL) },
				"Simple YUV video call"),

		// 2-stream combinations
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.PREVIEW,
						STREAM_USE_CASE_PREVIEW),
				new StreamTemplate(ImageFormat.JPEG, SizeThreshold.MAXIMUM,
						STREAM_USE_CASE_STILL_CAPTURE)},
				"Preview with JPEG still image capture"),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.PREVIEW,
						STREAM_USE_CASE_PREVIEW),
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.MAXIMUM,
						STREAM_USE_CASE_STILL_CAPTURE)},
				"Preview with YUV still image capture"),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.PREVIEW,
						STREAM_USE_CASE_PREVIEW),
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.RECORD,
						STREAM_USE_CASE_RECORD)},
				"Preview with video recording"),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.PREVIEW,
						STREAM_USE_CASE_PREVIEW),
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.RECORD,
						STREAM_USE_CASE_RECORD)},
				"Preview with in-application video processing"),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.PREVIEW,
						STREAM_USE_CASE_PREVIEW),
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.PREVIEW,
						STREAM_USE_CASE_PREVIEW)},
				"Preview with in-application image processing"),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.PREVIEW,
						STREAM_USE_CASE_PREVIEW),
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.s1440p,
						STREAM_USE_CASE_VIDEO_CALL)},
				"Preview with video call"),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.PREVIEW,
						STREAM_USE_CASE_PREVIEW),
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.s1440p,
						STREAM_USE_CASE_VIDEO_CALL)},
				"Preview with YUV video call"),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.s1440p,
						STREAM_USE_CASE_PREVIEW_VIDEO_STILL),
				new StreamTemplate(ImageFormat.JPEG, SizeThreshold.MAXIMUM,
						STREAM_USE_CASE_STILL_CAPTURE)},
				"Multi-purpose stream with JPEG still capture"),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.s1440p,
						STREAM_USE_CASE_PREVIEW_VIDEO_STILL),
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.MAXIMUM,
						STREAM_USE_CASE_STILL_CAPTURE)},
				"Multi-purpose stream with YUV still capture"),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.s1440p,
						STREAM_USE_CASE_PREVIEW_VIDEO_STILL),
				new StreamTemplate(ImageFormat.JPEG, SizeThreshold.MAXIMUM,
						STREAM_USE_CASE_STILL_CAPTURE)},
				"Multi-purpose YUV stream with JPEG still capture"),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.s1440p,
						STREAM_USE_CASE_PREVIEW_VIDEO_STILL),
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.MAXIMUM,
						STREAM_USE_CASE_STILL_CAPTURE)},
				"Multi-purpose YUV stream with YUV still capture"),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.PREVIEW,
						STREAM_USE_CASE_STILL_CAPTURE),
				new StreamTemplate(ImageFormat.JPEG, SizeThreshold.MAXIMUM,
						STREAM_USE_CASE_STILL_CAPTURE)},
				"YUV and JPEG concurrent still image capture (for testing)"),

		// 3-stream combinations
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.PREVIEW,
						STREAM_USE_CASE_PREVIEW),
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.RECORD,
						STREAM_USE_CASE_RECORD),
				new StreamTemplate(ImageFormat.JPEG, SizeThreshold.RECORD,
						STREAM_USE_CASE_STILL_CAPTURE)},
				"Preview, video record and JPEG video snapshot"),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.PREVIEW,
						STREAM_USE_CASE_PREVIEW),
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.RECORD,
						STREAM_USE_CASE_RECORD),
				new StreamTemplate(ImageFormat.JPEG, SizeThreshold.RECORD,
						STREAM_USE_CASE_STILL_CAPTURE)},
				"Preview, in-application video processing and JPEG video snapshot"),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.PREVIEW,
						STREAM_USE_CASE_PREVIEW),
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.PREVIEW,
						STREAM_USE_CASE_PREVIEW),
				new StreamTemplate(ImageFormat.JPEG, SizeThreshold.MAXIMUM,
						STREAM_USE_CASE_STILL_CAPTURE)},
				"Preview, in-application image processing, and JPEG still image capture"),
	};

	private static StreamCombinationTemplate sCroppedRawStreamUseCaseCombinations[] = {
		// Single stream combination
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.RAW_SENSOR, SizeThreshold.MAXIMUM,
						STREAM_USE_CASE_CROPPED_RAW)},
				"Cropped RAW still image capture without preview"),

		// 2 Stream combinations
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.PREVIEW,
						STREAM_USE_CASE_PREVIEW),
				new StreamTemplate(ImageFormat.RAW_SENSOR, SizeThreshold.MAXIMUM,
						STREAM_USE_CASE_CROPPED_RAW)},
				"Cropped RAW still image capture with preview"),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.PREVIEW,
						STREAM_USE_CASE_PREVIEW),
				new StreamTemplate(ImageFormat.RAW_SENSOR, SizeThreshold.MAXIMUM,
						STREAM_USE_CASE_CROPPED_RAW)},
				"In-app image processing with cropped RAW still image capture"),

		// 3 stream combinations
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.PREVIEW,
						STREAM_USE_CASE_PREVIEW),
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.MAXIMUM,
						STREAM_USE_CASE_STILL_CAPTURE),
				new StreamTemplate(ImageFormat.RAW_SENSOR, SizeThreshold.MAXIMUM,
						STREAM_USE_CASE_CROPPED_RAW)},
				"Preview with YUV and RAW still image capture"),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.PREVIEW,
						STREAM_USE_CASE_PREVIEW),
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.MAXIMUM,
						STREAM_USE_CASE_STILL_CAPTURE),
				new StreamTemplate(ImageFormat.RAW_SENSOR, SizeThreshold.MAXIMUM,
						STREAM_USE_CASE_CROPPED_RAW)},
				"In-app image processing with YUV and RAW still image capture"),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.PREVIEW,
						STREAM_USE_CASE_PREVIEW),
				new StreamTemplate(ImageFormat.JPEG, SizeThreshold.MAXIMUM,
						STREAM_USE_CASE_STILL_CAPTURE),
				new StreamTemplate(ImageFormat.RAW_SENSOR, SizeThreshold.MAXIMUM,
						STREAM_USE_CASE_CROPPED_RAW)},
				"Preview with JPEG and RAW still image capture"),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.PREVIEW,
						STREAM_USE_CASE_PREVIEW),
				new StreamTemplate(ImageFormat.JPEG, SizeThreshold.MAXIMUM,
						STREAM_USE_CASE_STILL_CAPTURE),
				new StreamTemplate(ImageFormat.RAW_SENSOR, SizeThreshold.MAXIMUM,
						STREAM_USE_CASE_CROPPED_RAW)},
				"In-app image processing with JPEG and RAW still image capture"),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.PREVIEW,
						STREAM_USE_CASE_PREVIEW),
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.PREVIEW,
						STREAM_USE_CASE_RECORD),
				new StreamTemplate(ImageFormat.RAW_SENSOR, SizeThreshold.MAXIMUM,
						STREAM_USE_CASE_CROPPED_RAW)},
				"Preview with video recording and RAW snapshot"),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.PREVIEW,
						STREAM_USE_CASE_PREVIEW),
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.PREVIEW,
						STREAM_USE_CASE_PREVIEW),
				new StreamTemplate(ImageFormat.RAW_SENSOR, SizeThreshold.MAXIMUM,
						STREAM_USE_CASE_CROPPED_RAW)},
				"Preview with in-app image processing and RAW still image capture"),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.PREVIEW,
						STREAM_USE_CASE_PREVIEW),
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.PREVIEW,
						STREAM_USE_CASE_PREVIEW),
				new StreamTemplate(ImageFormat.RAW_SENSOR, SizeThreshold.MAXIMUM,
						STREAM_USE_CASE_CROPPED_RAW)},
				"Two input in-app processing and RAW still image capture"),
	};

	private static StreamCombinationTemplate sPreviewStabilizedStreamCombinations[] = {
		// 1 stream combinations
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.s1440p)},
				"Stabilized preview, GPU video processing, or no-preview stabilized recording"),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.s1440p)},
				"Stabilized preview, GPU video processing, or no-preview stabilized recording"),
		//2 stream combinations
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.JPEG, SizeThreshold.MAXIMUM),
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.s1440p)},
				"Standard JPEG still imaging with stabilized preview"),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.MAXIMUM),
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.s1440p)},
				"Standard YUV still imaging with stabilized preview"),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.MAXIMUM),
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.s1440p)},
				"Standard YUV still imaging with stabilized in-app image processing stream"),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.JPEG, SizeThreshold.MAXIMUM),
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.s1440p)},
				"Standard JPEG still imaging with stabilized in-app image processing stream"),

		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.s1440p),
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.PREVIEW)},
				"High-resolution video recording with preview both streams stabilized"),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.s1440p),
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.PREVIEW)},
				"High-resolution video recording with preview both streams stabilized"),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.s1440p),
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.PREVIEW)},
				"High-resolution video recording with preview both streams stabilized"),
		new StreamCombinationTemplate(new StreamTemplate [] {
				new StreamTemplate(ImageFormat.YUV_420_888, SizeThreshold.s1440p),
				new StreamTemplate(ImageFormat.PRIVATE, SizeThreshold.PREVIEW)},
				"High-resolution video recording with preview both streams stabilized"),
	};

	public static final class Builder {
		private Size mDisplaySize;
		private List<Integer> mCapabilities;
		private int mHwLevel, mCameraId;
		private StreamConfigurationMap mStreamConfigMap;
		private StreamConfigurationMap mStreamConfigMapMaximumResolution;
		private boolean mIsHiddenPhysicalCamera;
		private boolean mIsPreviewStabilizationSupported = false;
		private boolean mIsCroppedRawSupported = false;

		private final Size kPreviewSizeBound = new Size(1920, 1088);

		public Builder(int cameraId, int hwLevel, Size displaySize,
				List<Integer> capabilities, StreamConfigurationMap sm,
				StreamConfigurationMap smMaxResolution, boolean previewStabilization,
				boolean isCroppedRawSupported) {
			mCameraId = cameraId;
			mDisplaySize = displaySize;
			mCapabilities = capabilities;
			mStreamConfigMap = sm;
			mStreamConfigMapMaximumResolution = smMaxResolution;
			mHwLevel = hwLevel;
			// AOSP asks ICameraService (CameraManager.isHiddenPhysicalCamera); ATL has no camera service
			// and no hidden physical cameras, so the answer is always false.
			mIsHiddenPhysicalCamera = false;
			mIsPreviewStabilizationSupported = previewStabilization;
			mIsCroppedRawSupported = isCroppedRawSupported;
		}

		private List<MandatoryStreamCombination>
		getAvailableMandatoryStreamCombinationsInternal(
				StreamCombinationTemplate []chosenStreamCombinations, boolean s10Bit) {

			HashMap<Pair<SizeThreshold, Integer>, List<Size>> availableSizes =
					enumerateAvailableSizes();
			if (availableSizes == null) {
				Log.e(TAG, "Available size enumeration failed!");
				return null;
			}

			ArrayList<MandatoryStreamCombination> availableStreamCombinations = new ArrayList<>();
			availableStreamCombinations.ensureCapacity(chosenStreamCombinations.length);
			for (StreamCombinationTemplate combTemplate : chosenStreamCombinations) {
				ArrayList<MandatoryStreamInformation> streamsInfo = new ArrayList<>();
				streamsInfo.ensureCapacity(combTemplate.mStreamTemplates.length);
				for (StreamTemplate template : combTemplate.mStreamTemplates) {
					List<Size> sizes = null;
					Pair<SizeThreshold, Integer> pair;
					pair = new Pair<>(template.mSizeThreshold, new Integer(template.mFormat));
					sizes = availableSizes.get(pair);
					if (s10Bit && template.mFormat == ImageFormat.YCBCR_P010) {
						// Make sure that exactly the same 10 and 8-bit YUV streams sizes are
						// supported
						pair = new Pair<>(template.mSizeThreshold,
								new Integer(ImageFormat.YUV_420_888));
						HashSet<Size> sdrYuvSizes = new HashSet<>(availableSizes.get(pair));
						if (!sdrYuvSizes.equals(new HashSet<>(sizes))) {
							Log.e(TAG, "The supported 10-bit YUV sizes are different from the"
									+ " supported 8-bit YUV sizes!");
							return null;
						}
					}

					MandatoryStreamInformation streamInfo;
					boolean isMaximumSize =
							(template.mSizeThreshold == SizeThreshold.MAXIMUM);
					try {
						streamInfo = new MandatoryStreamInformation(sizes, template.mFormat,
								isMaximumSize,  false,
								 false,
								 s10Bit ? template.mFormat != ImageFormat.JPEG :
										false);
					} catch (IllegalArgumentException e) {
						Log.e(TAG, "No available sizes found for format: " + template.mFormat +
								" size threshold: " + template.mSizeThreshold + " combination: " +
								combTemplate.mDescription);
						return null;
					}
					streamsInfo.add(streamInfo);
				}

				MandatoryStreamCombination streamCombination;
				try {
					streamCombination = new MandatoryStreamCombination(streamsInfo,
							combTemplate.mDescription,  false);
				} catch (IllegalArgumentException e) {
					Log.e(TAG, "No stream information for mandatory combination: "
							+ combTemplate.mDescription);
					return null;
				}

				availableStreamCombinations.add(streamCombination);
			}

			return Collections.unmodifiableList(availableStreamCombinations);
		}

		public List<MandatoryStreamCombination>
		getAvailableMandatoryPreviewStabilizedStreamCombinations() {
			// Since preview stabilization support is optional, we mandate these stream
			// combinations regardless of camera device capabilities.

			StreamCombinationTemplate []chosenStreamCombinations =
					sPreviewStabilizedStreamCombinations;

			if (!mIsPreviewStabilizationSupported) {
				Log.v(TAG, "Device does not support preview stabilization");
				 return null;
			 }

			return getAvailableMandatoryStreamCombinationsInternal(chosenStreamCombinations,
					false);
		}

		public List<MandatoryStreamCombination>
		getAvailableMandatory10BitStreamCombinations() {
			// Since 10-bit streaming support is optional, we mandate these stream
			// combinations regardless of camera device capabilities.

			StreamCombinationTemplate []chosenStreamCombinations = s10BitOutputStreamCombinations;
			if (!is10BitOutputSupported()) {
				Log.v(TAG, "Device is not able to output 10-bit!");
				return null;
			}
			return getAvailableMandatoryStreamCombinationsInternal(chosenStreamCombinations,
					true);
		}

		public List<MandatoryStreamCombination>
				getAvailableMandatoryStreamUseCaseCombinations() {
			if (!isCapabilitySupported(
					CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_STREAM_USE_CASE)) {
				return null;
			}

			HashMap<Pair<SizeThreshold, Integer>, List<Size>> availableSizes =
					enumerateAvailableSizes();
			if (availableSizes == null) {
				Log.e(TAG, "Available size enumeration failed!");
				return null;
			}
			ArrayList<StreamCombinationTemplate> availableTemplates =
					new ArrayList<StreamCombinationTemplate> ();
			availableTemplates.addAll(Arrays.asList(sStreamUseCaseCombinations));

			ArrayList<MandatoryStreamCombination> availableStreamCombinations = new ArrayList<>();
			int capacity = sStreamUseCaseCombinations.length;
			if (mIsCroppedRawSupported) {
				capacity += sCroppedRawStreamUseCaseCombinations.length;
				availableStreamCombinations.ensureCapacity(capacity);
				availableTemplates.addAll(Arrays.asList(sCroppedRawStreamUseCaseCombinations));
			}
			 else {
				availableStreamCombinations.ensureCapacity(capacity);
			 }

			for (StreamCombinationTemplate combTemplate : availableTemplates) {
				ArrayList<MandatoryStreamInformation> streamsInfo =
						new ArrayList<MandatoryStreamInformation>();
				streamsInfo.ensureCapacity(combTemplate.mStreamTemplates.length);

				for (StreamTemplate template : combTemplate.mStreamTemplates) {
					List<Size> sizes = null;
					Pair<SizeThreshold, Integer> pair;
					pair = new Pair<SizeThreshold, Integer>(template.mSizeThreshold,
							new Integer(template.mFormat));
					sizes = availableSizes.get(pair);

					MandatoryStreamInformation streamInfo;
					boolean isMaximumSize =
							(template.mSizeThreshold == SizeThreshold.MAXIMUM);
					try {
						streamInfo = new MandatoryStreamInformation(sizes, template.mFormat,
								isMaximumSize, false, false,
								 false, template.mStreamUseCase);
					} catch (IllegalArgumentException e) {
						Log.e(TAG, "No available sizes found for format: " + template.mFormat +
								" size threshold: " + template.mSizeThreshold + " combination: " +
								combTemplate.mDescription);
						return null;
					}
					streamsInfo.add(streamInfo);
				}

				MandatoryStreamCombination streamCombination;
				try {
					streamCombination = new MandatoryStreamCombination(streamsInfo,
							combTemplate.mDescription,  false);
				} catch (IllegalArgumentException e) {
					Log.e(TAG, "No stream information for mandatory combination: "
							+ combTemplate.mDescription);
					return null;
				}

				availableStreamCombinations.add(streamCombination);
			}

			return Collections.unmodifiableList(availableStreamCombinations);
		}

		public List<MandatoryStreamCombination>
				getAvailableMandatoryConcurrentStreamCombinations() {
			// Since concurrent streaming support is optional, we mandate these stream
			// combinations regardless of camera device capabilities.

			StreamCombinationTemplate []chosenStreamCombinations = sConcurrentStreamCombinations;
			if (!isColorOutputSupported()) {
				Log.v(TAG, "Device is not backward compatible, depth streams are mandatory!");
				chosenStreamCombinations = sConcurrentDepthOnlyStreamCombinations;
			}
			Size sizeVGAp = new Size(640, 480);
			Size size720p = new Size(1280, 720);
			Size size1440p = new Size(1920, 1440);

			ArrayList<MandatoryStreamCombination> availableConcurrentStreamCombinations =
					new ArrayList<MandatoryStreamCombination>();
			availableConcurrentStreamCombinations.ensureCapacity(
					chosenStreamCombinations.length);
			for (StreamCombinationTemplate combTemplate : chosenStreamCombinations) {
				ArrayList<MandatoryStreamInformation> streamsInfo =
						new ArrayList<MandatoryStreamInformation>();
				streamsInfo.ensureCapacity(combTemplate.mStreamTemplates.length);
				for (StreamTemplate template : combTemplate.mStreamTemplates) {
					MandatoryStreamInformation streamInfo;
					List<Size> sizes = new ArrayList<Size>();
					Size formatSize = null;
					switch (template.mSizeThreshold) {
						case s1440p:
							formatSize = size1440p;
							break;
						case VGA:
							formatSize = sizeVGAp;
							break;
						default:
							formatSize = size720p;
					}
					Size sizeChosen =
							getMinSize(formatSize,
									getMaxSize(mStreamConfigMap.getOutputSizes(template.mFormat)));
					sizes.add(sizeChosen);
					try {
						streamInfo = new MandatoryStreamInformation(sizes, template.mFormat,
							false);
					} catch (IllegalArgumentException e) {
						String cause = "No available sizes found for format: " + template.mFormat
								+ " size threshold: " + template.mSizeThreshold + " combination: "
								+ combTemplate.mDescription;
						throw new RuntimeException(cause, e);
					}
					streamsInfo.add(streamInfo);
				}

				MandatoryStreamCombination streamCombination;
				try {
					streamCombination = new MandatoryStreamCombination(streamsInfo,
							combTemplate.mDescription, false);
				} catch (IllegalArgumentException e) {
					String cause =  "No stream information for mandatory combination: "
							+ combTemplate.mDescription;
					throw new RuntimeException(cause, e);
				}
				availableConcurrentStreamCombinations.add(streamCombination);
			}
			return Collections.unmodifiableList(availableConcurrentStreamCombinations);
		}

		public List<MandatoryStreamCombination>
				getAvailableMandatoryMaximumResolutionStreamCombinations() {

			if (!isColorOutputSupported()) {
				Log.v(TAG, "Device is not backward compatible!, no mandatory maximum res streams");
				return null;
			}

			ArrayList<StreamCombinationTemplate> chosenStreamCombinationTemplates =
					new ArrayList<StreamCombinationTemplate>();

			chosenStreamCombinationTemplates.addAll(
					Arrays.asList(sUltraHighResolutionStreamCombinations));

			ArrayList<MandatoryStreamCombination> availableStreamCombinations =
					new ArrayList<MandatoryStreamCombination>();
			boolean addRemosaicReprocessing = isRemosaicReprocessingSupported();

			int remosaicSize = 0;
			Size [] maxResYUVInputSizes =
					mStreamConfigMapMaximumResolution.getInputSizes(ImageFormat.YUV_420_888);
			Size [] maxResPRIVInputSizes =
					mStreamConfigMapMaximumResolution.getInputSizes(ImageFormat.PRIVATE);

			if (addRemosaicReprocessing) {
				remosaicSize += sUltraHighResolutionReprocStreamCombinations.length;
				chosenStreamCombinationTemplates.addAll(
						Arrays.asList(sUltraHighResolutionReprocStreamCombinations));
			}

			if (maxResYUVInputSizes != null && maxResYUVInputSizes.length != 0) {
				remosaicSize += sUltraHighResolutionYUVReprocStreamCombinations.length;
				chosenStreamCombinationTemplates.addAll(
						Arrays.asList(sUltraHighResolutionYUVReprocStreamCombinations));
			}

			if (maxResPRIVInputSizes != null && maxResPRIVInputSizes.length != 0) {
				remosaicSize += sUltraHighResolutionPRIVReprocStreamCombinations.length;
				chosenStreamCombinationTemplates.addAll(
						Arrays.asList(sUltraHighResolutionPRIVReprocStreamCombinations));

			}
			availableStreamCombinations.ensureCapacity(
					chosenStreamCombinationTemplates.size() + remosaicSize);
			fillUHMandatoryStreamCombinations(availableStreamCombinations,
					chosenStreamCombinationTemplates);

			return Collections.unmodifiableList(availableStreamCombinations);
		}

		private MandatoryStreamCombination createUHSensorMandatoryStreamCombination(
				StreamCombinationTemplate combTemplate, int substitutedFormat) {
			ArrayList<MandatoryStreamInformation> streamsInfo =
					new ArrayList<MandatoryStreamInformation>();
			streamsInfo.ensureCapacity(combTemplate.mStreamTemplates.length);
			boolean isReprocess = combTemplate.mReprocessType != ReprocessType.NONE;
			if (isReprocess) {
				int format = -1;
				ArrayList<Size> inputSize = new ArrayList<Size>();
				if (combTemplate.mReprocessType == ReprocessType.PRIVATE) {
					inputSize.add(
							getMaxSize(mStreamConfigMapMaximumResolution.getInputSizes(
									ImageFormat.PRIVATE)));
					format = ImageFormat.PRIVATE;
				} else if (combTemplate.mReprocessType == ReprocessType.REMOSAIC) {
					inputSize.add(
							getMaxSize(mStreamConfigMapMaximumResolution.getInputSizes(
									ImageFormat.RAW_SENSOR)));
					format = ImageFormat.RAW_SENSOR;
				} else {
					inputSize.add(
							getMaxSize(mStreamConfigMapMaximumResolution.getInputSizes(
									ImageFormat.YUV_420_888)));
					format = ImageFormat.YUV_420_888;
				}
				streamsInfo.add(new MandatoryStreamInformation(inputSize, format,
						false, true,
						 true));
				streamsInfo.add(new MandatoryStreamInformation(inputSize, format,
						false,  false,
						true));
			}
			HashMap<Pair<SizeThreshold, Integer>, List<Size>> availableDefaultNonRawSizes =
					enumerateAvailableSizes();
			if (availableDefaultNonRawSizes == null) {
				Log.e(TAG, "Available size enumeration failed");
				return null;
			}
			Size[] defaultRawSizes =
					mStreamConfigMap.getOutputSizes(ImageFormat.RAW_SENSOR);
			ArrayList<Size> availableDefaultRawSizes = new ArrayList<>();
			if (defaultRawSizes != null) {
				availableDefaultRawSizes.ensureCapacity(defaultRawSizes.length);
				availableDefaultRawSizes.addAll(Arrays.asList(defaultRawSizes));
			}
			for (StreamTemplate template : combTemplate.mStreamTemplates) {
				MandatoryStreamInformation streamInfo;
				List<Size> sizes = new ArrayList<Size>();
				int formatChosen = template.mFormat;
				boolean isUltraHighResolution =
						(template.mSizeThreshold == SizeThreshold.FULL_RES);
				StreamConfigurationMap sm =
						isUltraHighResolution ?
								mStreamConfigMapMaximumResolution : mStreamConfigMap;
				boolean isMaximumSize = (template.mSizeThreshold == SizeThreshold.MAXIMUM);

				if (substitutedFormat != ImageFormat.UNKNOWN && isMaximumSize) {
					formatChosen = substitutedFormat;
				}

				if (isUltraHighResolution) {
					Size [] outputSizes = sm.getOutputSizes(formatChosen);
					Size [] highResolutionOutputSizes =
							sm.getHighResolutionOutputSizes(formatChosen);
					Size maxBurstSize = getMaxSizeOrNull(outputSizes);
					Size maxHighResolutionSize = getMaxSizeOrNull(highResolutionOutputSizes);
					Size chosenMaxSize =
							maxBurstSize != null ? maxBurstSize : maxHighResolutionSize;
					if (maxBurstSize != null && maxHighResolutionSize != null) {
						chosenMaxSize = getMaxSize(maxBurstSize, maxHighResolutionSize);
					}
					sizes.add(chosenMaxSize);
				} else {
					if (formatChosen == ImageFormat.RAW_SENSOR) {
						// RAW_SENSOR always has MAXIMUM threshold.
						sizes = availableDefaultRawSizes;
					} else {
						Pair<SizeThreshold, Integer> pair =
							new Pair<SizeThreshold, Integer>(template.mSizeThreshold,
									new Integer(formatChosen));
						sizes = availableDefaultNonRawSizes.get(pair);
					}
				}

				try {
					streamInfo = new MandatoryStreamInformation(sizes, formatChosen,
							isMaximumSize,  false, isUltraHighResolution);
				} catch (IllegalArgumentException e) {
					String cause = "No available sizes found for format: " + template.mFormat
							+ " size threshold: " + template.mSizeThreshold + " combination: "
							+ combTemplate.mDescription;
					throw new RuntimeException(cause, e);
				}
				streamsInfo.add(streamInfo);
			}

			String formatString = null;
			switch (substitutedFormat) {
				case ImageFormat.RAW_SENSOR :
					formatString = "RAW_SENSOR";
					break;
				case ImageFormat.JPEG :
					formatString = "JPEG";
					break;
				default:
					formatString = "YUV";
			}

			MandatoryStreamCombination streamCombination;
			try {
				streamCombination = new MandatoryStreamCombination(streamsInfo,
						combTemplate.mDescription + " " + formatString + " still-capture",
						isReprocess);
			} catch (IllegalArgumentException e) {
				String cause =  "No stream information for mandatory combination: "
						+ combTemplate.mDescription;
				throw new RuntimeException(cause, e);
			}
			return streamCombination;
		}

		private void fillUHMandatoryStreamCombinations(
				ArrayList<MandatoryStreamCombination> availableStreamCombinations,
				ArrayList<StreamCombinationTemplate> chosenTemplates) {

			for (StreamCombinationTemplate combTemplate : chosenTemplates) {
				MandatoryStreamCombination streamCombination =
						createUHSensorMandatoryStreamCombination(combTemplate,
								  ImageFormat.UNKNOWN);
				availableStreamCombinations.add(streamCombination);
				if (combTemplate.mSubstituteYUV) {
					 streamCombination =
							createUHSensorMandatoryStreamCombination(combTemplate,
									ImageFormat.RAW_SENSOR);
					availableStreamCombinations.add(streamCombination);
					streamCombination =
							createUHSensorMandatoryStreamCombination(combTemplate,
									ImageFormat.JPEG);
					availableStreamCombinations.add(streamCombination);
				}
			}
		}

		public List<MandatoryStreamCombination>
			getAvailableMandatoryStreamCombinations() {
			if (!isColorOutputSupported()) {
				Log.v(TAG, "Device is not backward compatible!");
				return null;
			}

			if ((mCameraId < 0) && !isExternalCamera()) {
				Log.i(TAG, "Invalid camera id");
				return null;
			}

			ArrayList<StreamCombinationTemplate> availableTemplates =
					new ArrayList<StreamCombinationTemplate> ();
			if (isHardwareLevelAtLeastLegacy()) {
				availableTemplates.addAll(Arrays.asList(sLegacyCombinations));
			}

			// External devices are identical to limited devices w.r.t. stream combinations.
			if (isHardwareLevelAtLeastLimited() || isExternalCamera()) {
				availableTemplates.addAll(Arrays.asList(sLimitedCombinations));

				if (isPrivateReprocessingSupported()) {
					availableTemplates.addAll(Arrays.asList(sLimitedPrivateReprocCombinations));
				}

				if (isYUVReprocessingSupported()) {
					availableTemplates.addAll(Arrays.asList(sLimitedYUVReprocCombinations));
				}

			}

			if (isCapabilitySupported(
					CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BURST_CAPTURE)) {
				availableTemplates.addAll(Arrays.asList(sBurstCombinations));
			}

			if (isHardwareLevelAtLeastFull()) {
				availableTemplates.addAll(Arrays.asList(sFullCombinations));

				if (isPrivateReprocessingSupported()) {
					availableTemplates.addAll(Arrays.asList(sFullPrivateReprocCombinations));
				}

				if (isYUVReprocessingSupported()) {
					availableTemplates.addAll(Arrays.asList(sFullYUVReprocCombinations));
				}

			}

			if (isCapabilitySupported(
					CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW)) {
				availableTemplates.addAll(Arrays.asList(sRawCombinations));

				if (isPrivateReprocessingSupported()) {
					availableTemplates.addAll(Arrays.asList(sRAWPrivateReprocCombinations));
				}

				if (isYUVReprocessingSupported()) {
					availableTemplates.addAll(Arrays.asList(sRAWYUVReprocCombinations));
				}

			}

			if (isHardwareLevelAtLeastLevel3()) {
				availableTemplates.addAll(Arrays.asList(sLevel3Combinations));

				if (isPrivateReprocessingSupported()) {
					availableTemplates.addAll(Arrays.asList(sLevel3PrivateReprocCombinations));
				}

				if (isYUVReprocessingSupported()) {
					availableTemplates.addAll(Arrays.asList(sLevel3YUVReprocCombinations));
				}

			}

			return generateAvailableCombinations(availableTemplates);
		}

		private List<MandatoryStreamCombination> generateAvailableCombinations(
				ArrayList<StreamCombinationTemplate> availableTemplates) {
			if (availableTemplates.isEmpty()) {
				Log.e(TAG, "No available stream templates!");
				return null;
			}

			HashMap<Pair<SizeThreshold, Integer>, List<Size>> availableSizes =
				enumerateAvailableSizes();
			if (availableSizes == null) {
				Log.e(TAG, "Available size enumeration failed!");
				return null;
			}

			// RAW only uses MAXIMUM size threshold
			Size[] rawSizes = mStreamConfigMap.getOutputSizes(ImageFormat.RAW_SENSOR);
			ArrayList<Size> availableRawSizes = new ArrayList<Size>();
			if (rawSizes != null) {
				availableRawSizes.ensureCapacity(rawSizes.length);
				availableRawSizes.addAll(Arrays.asList(rawSizes));
			}

			Size maxPrivateInputSize = new Size(0, 0);
			if (isPrivateReprocessingSupported()) {
				maxPrivateInputSize = getMaxSize(mStreamConfigMap.getInputSizes(
							ImageFormat.PRIVATE));
			}

			Size maxYUVInputSize = new Size(0, 0);
			if (isYUVReprocessingSupported()) {
				maxYUVInputSize = getMaxSize(mStreamConfigMap.getInputSizes(
							ImageFormat.YUV_420_888));
			}

			// Generate the available mandatory stream combinations given the supported templates
			// and size ranges.
			ArrayList<MandatoryStreamCombination> availableStreamCombinations =
					new ArrayList<MandatoryStreamCombination>();
			availableStreamCombinations.ensureCapacity(availableTemplates.size());
			for (StreamCombinationTemplate combTemplate : availableTemplates) {
				ArrayList<MandatoryStreamInformation> streamsInfo =
						new ArrayList<MandatoryStreamInformation>();
				streamsInfo.ensureCapacity(combTemplate.mStreamTemplates.length);
				boolean isReprocessable = combTemplate.mReprocessType != ReprocessType.NONE;
				if (isReprocessable) {
					// The first and second streams in a reprocessable combination have the
					// same size and format. The first is the input and the second is the output
					// used for generating the subsequent input buffers.
					ArrayList<Size> inputSize = new ArrayList<Size>();
					int format;
					if (combTemplate.mReprocessType == ReprocessType.PRIVATE) {
						inputSize.add(maxPrivateInputSize);
						format = ImageFormat.PRIVATE;
					} else {
						// Default mandatory streams only have PRIVATE / YUV reprocessing.
						inputSize.add(maxYUVInputSize);
						format = ImageFormat.YUV_420_888;
					}
					streamsInfo.add(new MandatoryStreamInformation(inputSize, format,
								true, true));
					streamsInfo.add(new MandatoryStreamInformation(inputSize, format,
							true));
				}

				for (StreamTemplate template : combTemplate.mStreamTemplates) {
					List<Size> sizes = null;
					if (template.mFormat == ImageFormat.RAW_SENSOR) {
						sizes = availableRawSizes;
					} else {
						Pair<SizeThreshold, Integer> pair;
						pair = new Pair<SizeThreshold, Integer>(template.mSizeThreshold,
								new Integer(template.mFormat));
						sizes = availableSizes.get(pair);
					}

					MandatoryStreamInformation streamInfo;
					boolean isMaximumSize =
							(template.mSizeThreshold == SizeThreshold.MAXIMUM);
					try {
						streamInfo = new MandatoryStreamInformation(sizes, template.mFormat,
								isMaximumSize);
					} catch (IllegalArgumentException e) {
						Log.e(TAG, "No available sizes found for format: " + template.mFormat +
								" size threshold: " + template.mSizeThreshold + " combination: " +
								combTemplate.mDescription);
						return null;
					}
					streamsInfo.add(streamInfo);
				}

				MandatoryStreamCombination streamCombination;
				try {
					streamCombination = new MandatoryStreamCombination(streamsInfo,
							combTemplate.mDescription, isReprocessable);
				} catch (IllegalArgumentException e) {
					Log.e(TAG, "No stream information for mandatory combination: "
							+ combTemplate.mDescription);
					return null;
				}

				availableStreamCombinations.add(streamCombination);
			}

			return Collections.unmodifiableList(availableStreamCombinations);
		}

		private HashMap<Pair<SizeThreshold, Integer>, List<Size>>
			enumerateAvailableSizes() {
			final int[] formats = {
				ImageFormat.RAW_SENSOR,
				ImageFormat.PRIVATE,
				ImageFormat.YUV_420_888,
				ImageFormat.JPEG,
				ImageFormat.YCBCR_P010
			};
			Size recordingMaxSize = new Size(0, 0);
			Size previewMaxSize = new Size(0, 0);
			Size vgaSize = new Size(640, 480);
			Size s720pSize = new Size(1280, 720);
			Size s1440pSize = new Size(1920, 1440);
			// For external camera, or hidden physical camera, CamcorderProfile may not be
			// available, so get maximum recording size using stream configuration map.
			if (isExternalCamera() || mIsHiddenPhysicalCamera) {
				recordingMaxSize = getMaxCameraRecordingSize();
			} else {
				recordingMaxSize = getMaxRecordingSize();
			}
			if (recordingMaxSize == null) {
				Log.e(TAG, "Failed to find maximum recording size!");
				return null;
			}

			HashMap<Integer, Size[]> allSizes = new HashMap<Integer, Size[]>();
			for (int format : formats) {
				Integer intFormat = new Integer(format);
				Size[] sizes = mStreamConfigMap.getOutputSizes(format);
				if (sizes == null) {
					sizes = new Size[0];
				}
				allSizes.put(intFormat, sizes);
			}

			List<Size> previewSizes = getSizesWithinBound(
					allSizes.get(new Integer(ImageFormat.PRIVATE)), kPreviewSizeBound);
			if ((previewSizes == null) || (previewSizes.isEmpty())) {
				Log.e(TAG, "No preview sizes within preview size bound!");
				return null;
			}
			List<Size> orderedPreviewSizes = getAscendingOrderSizes(previewSizes,
					false);
			previewMaxSize = getMaxPreviewSize(orderedPreviewSizes);

			HashMap<Pair<SizeThreshold, Integer>, List<Size>> availableSizes =
					new HashMap<Pair<SizeThreshold, Integer>, List<Size>>();

			for (int format : formats) {
				Integer intFormat = new Integer(format);
				Size[] sizes = allSizes.get(intFormat);
				Pair<SizeThreshold, Integer> pair = new Pair<SizeThreshold, Integer>(
						SizeThreshold.VGA, intFormat);
				availableSizes.put(pair, getSizesWithinBound(sizes, vgaSize));

				pair = new Pair<SizeThreshold, Integer>(SizeThreshold.PREVIEW, intFormat);
				availableSizes.put(pair, getSizesWithinBound(sizes, previewMaxSize));

				pair = new Pair<SizeThreshold, Integer>(SizeThreshold.RECORD, intFormat);
				availableSizes.put(pair, getSizesWithinBound(sizes, recordingMaxSize));

				pair = new Pair<SizeThreshold, Integer>(SizeThreshold.MAXIMUM, intFormat);
				availableSizes.put(pair, Arrays.asList(sizes));

				pair = new Pair<SizeThreshold, Integer>(SizeThreshold.s720p, intFormat);
				availableSizes.put(pair, getSizesWithinBound(sizes, s720pSize));

				pair = new Pair<SizeThreshold, Integer>(SizeThreshold.s1440p, intFormat);
				availableSizes.put(pair, getSizesWithinBound(sizes, s1440pSize));
			}

			return availableSizes;
		}

		private static List<Size> getSizesWithinBound(Size[] sizes,
				Size bound) {
			ArrayList<Size> ret = new ArrayList<Size>();
			for (Size size : sizes) {
				if (size.getWidth() <= bound.getWidth() && size.getHeight() <= bound.getHeight()) {
					ret.add(size);
				}
			}

			return ret;
		}

		public static Size getMinSize(Size a, Size b) {
			if (a == null || b == null) {
				throw new IllegalArgumentException("sizes was empty");
			}
			if (a.getWidth() * a.getHeight() < b.getHeight() * b.getWidth()) {
				return a;
			}
			return b;
		}

		public static Size getMaxSize(Size... sizes) {
			if (sizes == null || sizes.length == 0) {
				throw new IllegalArgumentException("sizes was empty");
			}

			Size sz = sizes[0];
			for (Size size : sizes) {
				if (size.getWidth() * size.getHeight() > sz.getWidth() * sz.getHeight()) {
					sz = size;
				}
			}

			return sz;
		}

		public static Size getMaxSizeOrNull(Size... sizes) {
			if (sizes == null || sizes.length == 0) {
				return null;
			}

			return getMaxSize(sizes);
		}

		private boolean isHardwareLevelAtLeast(int level) {
			final int[] sortedHwLevels = {
				CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY,
				CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL,
				CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
				CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
				CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3
			};
			if (level == mHwLevel) {
				return true;
			}

			for (int sortedlevel : sortedHwLevels) {
				if (sortedlevel == level) {
					return true;
				} else if (sortedlevel == mHwLevel) {
					return false;
				}
			}

			return false;
		}

		private boolean isExternalCamera() {
			return mHwLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL;
		}

		private boolean isHardwareLevelAtLeastLegacy() {
			return isHardwareLevelAtLeast(CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY);
		}

		private boolean isHardwareLevelAtLeastLimited() {
			return isHardwareLevelAtLeast(CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED);
		}

		private boolean isHardwareLevelAtLeastFull() {
			return isHardwareLevelAtLeast(CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_FULL);
		}

		private boolean isHardwareLevelAtLeastLevel3() {
			return isHardwareLevelAtLeast(CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_3);
		}

		private boolean isCapabilitySupported(int capability) {
			return mCapabilities.contains(capability);
		}

		private boolean isColorOutputSupported() {
			return isCapabilitySupported(
					CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE);
		}

		private boolean is10BitOutputSupported() {
			return isCapabilitySupported(
					CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_DYNAMIC_RANGE_TEN_BIT);
		}

		private boolean isPrivateReprocessingSupported() {
			return isCapabilitySupported(
					CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_PRIVATE_REPROCESSING);
		}

		private boolean isYUVReprocessingSupported() {
			return isCapabilitySupported(
					CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_YUV_REPROCESSING);
		}

		private boolean isRemosaicReprocessingSupported() {
			return isCapabilitySupported(
					CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_REMOSAIC_REPROCESSING);
		}

		private Size getMaxRecordingSize() {
			int quality =
					CamcorderProfile.hasProfile(mCameraId, CamcorderProfile.QUALITY_2160P) ?
						CamcorderProfile.QUALITY_2160P :
					CamcorderProfile.hasProfile(mCameraId, CamcorderProfile.QUALITY_1080P) ?
						CamcorderProfile.QUALITY_1080P :
					CamcorderProfile.hasProfile(mCameraId, CamcorderProfile.QUALITY_720P) ?
						CamcorderProfile.QUALITY_720P :
					CamcorderProfile.hasProfile(mCameraId, CamcorderProfile.QUALITY_480P) ?
						CamcorderProfile.QUALITY_480P :
					CamcorderProfile.hasProfile(mCameraId, CamcorderProfile.QUALITY_QVGA) ?
						CamcorderProfile.QUALITY_QVGA :
					CamcorderProfile.hasProfile(mCameraId, CamcorderProfile.QUALITY_CIF) ?
						CamcorderProfile.QUALITY_CIF :
					CamcorderProfile.hasProfile(mCameraId, CamcorderProfile.QUALITY_QCIF) ?
						CamcorderProfile.QUALITY_QCIF :
						-1;

			if (quality < 0) {
				return null;
			}

			CamcorderProfile maxProfile = CamcorderProfile.get(mCameraId, quality);
			return new Size(maxProfile.videoFrameWidth, maxProfile.videoFrameHeight);
		}

		private Size getMaxCameraRecordingSize() {
			final Size FULLHD = new Size(1920, 1080);

			Size[] videoSizeArr = mStreamConfigMap.getOutputSizes(
					android.media.MediaRecorder.class);
			List<Size> sizes = new ArrayList<Size>();
			for (Size sz: videoSizeArr) {
				if (sz.getWidth() <= FULLHD.getWidth() && sz.getHeight() <= FULLHD.getHeight()) {
					sizes.add(sz);
				}
			}
			List<Size> videoSizes = getAscendingOrderSizes(sizes, false);
			for (Size sz : videoSizes) {
				long minFrameDuration = mStreamConfigMap.getOutputMinFrameDuration(
						android.media.MediaRecorder.class, sz);
				// Give some margin for rounding error
				if (minFrameDuration < (1e9 / 29.9)) {
					Log.i(TAG, "External camera " + mCameraId + " has max video size:" + sz);
					return sz;
				}
			}
			Log.w(TAG, "Camera " + mCameraId + " does not support any 30fps video output");
			return FULLHD; // doesn't matter what size is returned here
		}

		private Size getMaxPreviewSize(List<Size> orderedPreviewSizes) {
			if (orderedPreviewSizes != null) {
				for (Size size : orderedPreviewSizes) {
					if ((mDisplaySize.getWidth() >= size.getWidth()) &&
							(mDisplaySize.getHeight() >= size.getHeight())) {
						return size;
					}
				}
			}

			Log.w(TAG,"Camera " + mCameraId + " maximum preview size search failed with "
					+ "display size " + mDisplaySize);
			return kPreviewSizeBound;
		}

		public static class SizeComparator implements Comparator<Size> {
			@Override
			public int compare(Size lhs, Size rhs) {
				return StreamConfigurationMap.compareSizes(lhs.getWidth(), lhs.getHeight(),
						rhs.getWidth(), rhs.getHeight());
			}
		}

		private static List<Size> getAscendingOrderSizes(
				final List<Size> sizeList, boolean ascending) {
			Comparator<Size> comparator = new SizeComparator();
			List<Size> sortedSizes = new ArrayList<Size>();
			sortedSizes.addAll(sizeList);
			Collections.sort(sortedSizes, comparator);
			if (!ascending) {
				Collections.reverse(sortedSizes);
			}

			return sortedSizes;
		}
	}
}
