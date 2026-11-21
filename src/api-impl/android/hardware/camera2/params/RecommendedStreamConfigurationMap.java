/* AOSP RecommendedStreamConfigurationMap.java (Android 16). Difference: the @RecommendedUsecase annotation type is omitted.
 * ATL's CameraManager lists no camera, so CameraCharacteristics never produces one; apps may construct it. */
package android.hardware.camera2.params;

import android.util.ArraySet;
import android.util.Range;
import android.util.Size;
import android.view.Surface;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

public final class RecommendedStreamConfigurationMap {
	private static final String TAG = "RecommendedStreamConfigurationMap";

	private int mUsecase;
	private boolean mSupportsPrivate;
	private StreamConfigurationMap mRecommendedMap;

	public static final int MAX_USECASE_COUNT = 32;
	public static final int USECASE_PREVIEW = 0x0;
	public static final int USECASE_RECORD = 0x1;
	public static final int USECASE_VIDEO_SNAPSHOT = 0x2;
	public static final int USECASE_SNAPSHOT = 0x3;
	public static final int USECASE_ZSL = 0x4;
	public static final int USECASE_RAW = 0x5;
	public static final int USECASE_LOW_LATENCY_SNAPSHOT = 0x6;
	public static final int USECASE_10BIT_OUTPUT = 0x8;
	public static final int USECASE_VENDOR_START = 0x18;

	public RecommendedStreamConfigurationMap(StreamConfigurationMap recommendedMap, int usecase, boolean supportsPrivate) {
		mRecommendedMap = recommendedMap;
		mUsecase = usecase;
		mSupportsPrivate = supportsPrivate;
	}

	public int getRecommendedUseCase() {
		return mUsecase;
	}

	private Set<Integer> getUnmodifiableIntegerSet(int[] intArray) {
		if ((intArray != null) && (intArray.length > 0)) {
			ArraySet<Integer> integerSet = new ArraySet<Integer>();
			integerSet.ensureCapacity(intArray.length);
			for (int intEntry : intArray) {
				integerSet.add(intEntry);
			}

			return Collections.unmodifiableSet(integerSet);
		}

		return null;
	}

	public Set<Integer> getOutputFormats() {
		return getUnmodifiableIntegerSet(mRecommendedMap.getOutputFormats());
	}

	public Set<Integer> getValidOutputFormatsForInput(int inputFormat) {
		return getUnmodifiableIntegerSet(mRecommendedMap.getValidOutputFormatsForInput(inputFormat));
	}

	public Set<Integer> getInputFormats() {
		return getUnmodifiableIntegerSet(mRecommendedMap.getInputFormats());
	}

	private Set<Size> getUnmodifiableSizeSet(Size[] sizeArray) {
		if ((sizeArray != null) && (sizeArray.length > 0)) {
			ArraySet<Size> sizeSet = new ArraySet<Size>();
			sizeSet.addAll(Arrays.asList(sizeArray));
			return Collections.unmodifiableSet(sizeSet);
		}

		return null;
	}

	public Set<Size> getInputSizes(int format) {
		return getUnmodifiableSizeSet(mRecommendedMap.getInputSizes(format));
	}

	public boolean isOutputSupportedFor(int format) {
		return mRecommendedMap.isOutputSupportedFor(format);
	}

	public Set<Size> getOutputSizes(int format) {
		return getUnmodifiableSizeSet(mRecommendedMap.getOutputSizes(format));
	}

	public Set<Size> getHighSpeedVideoSizes() {
		return getUnmodifiableSizeSet(mRecommendedMap.getHighSpeedVideoSizes());
	}

	private Set<Range<Integer>> getUnmodifiableRangeSet(Range<Integer>[] rangeArray) {
		if ((rangeArray != null) && (rangeArray.length > 0)) {
			ArraySet<Range<Integer>> rangeSet = new ArraySet<Range<Integer>>();
			rangeSet.addAll(Arrays.asList(rangeArray));
			return Collections.unmodifiableSet(rangeSet);
		}

		return null;
	}

	public Set<Range<Integer>> getHighSpeedVideoFpsRangesFor(Size size) {
		return getUnmodifiableRangeSet(mRecommendedMap.getHighSpeedVideoFpsRangesFor(size));
	}

	public Set<Range<Integer>> getHighSpeedVideoFpsRanges() {
		return getUnmodifiableRangeSet(mRecommendedMap.getHighSpeedVideoFpsRanges());
	}

	public Set<Size> getHighSpeedVideoSizesFor(Range<Integer> fpsRange) {
		return getUnmodifiableSizeSet(mRecommendedMap.getHighSpeedVideoSizesFor(fpsRange));
	}

	public Set<Size> getHighResolutionOutputSizes(int format) {
		return getUnmodifiableSizeSet(mRecommendedMap.getHighResolutionOutputSizes(format));
	}

	public long getOutputMinFrameDuration(int format, Size size) {
		return mRecommendedMap.getOutputMinFrameDuration(format, size);
	}

	public long getOutputStallDuration(int format, Size size) {
		return mRecommendedMap.getOutputStallDuration(format, size);
	}

	public <T> Set<Size> getOutputSizes(Class<T> klass) {
		if (mSupportsPrivate) {
			return getUnmodifiableSizeSet(mRecommendedMap.getOutputSizes(klass));
		}

		return null;
	}

	public <T> long getOutputMinFrameDuration(final Class<T> klass, final Size size) {
		if (mSupportsPrivate) {
			return mRecommendedMap.getOutputMinFrameDuration(klass, size);
		}

		return 0;
	}

	public <T> long getOutputStallDuration(final Class<T> klass, final Size size) {
		if (mSupportsPrivate) {
			return mRecommendedMap.getOutputStallDuration(klass, size);
		}

		return 0;
	}

	public boolean isOutputSupportedFor(Surface surface) {
		return mRecommendedMap.isOutputSupportedFor(surface);
	}
}
