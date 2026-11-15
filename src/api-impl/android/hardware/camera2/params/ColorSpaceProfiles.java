package android.hardware.camera2.params;

import android.graphics.ColorSpace;
import android.graphics.ImageFormat;
import android.util.ArrayMap;
import android.util.ArraySet;

import java.util.Map;
import java.util.Set;

/* AOSP core/java/android/hardware/camera2/params/ColorSpaceProfiles.java (android-16) */
public final class ColorSpaceProfiles {
	
	public static final int UNSPECIFIED =
			-1 /* CameraMetadata.REQUEST_AVAILABLE_COLOR_SPACE_PROFILES_MAP_UNSPECIFIED */;

	private final Map<ColorSpace.Named, Map<Integer, Set<Long>>> mProfileMap = new ArrayMap<>();

	public ColorSpaceProfiles(final long[] elements) {
		if ((elements.length % 3) != 0) {
			throw new IllegalArgumentException("Color space profile map length "
					+ elements.length + " is not divisible by 3!");
		}

		for (int i = 0; i < elements.length; i += 3) {
			int colorSpace = (int) elements[i];
			checkProfileValue(colorSpace);
			ColorSpace.Named namedColorSpace = ColorSpace.Named.values()[colorSpace];
			int imageFormat = (int) elements[i + 1];
			long dynamicRangeProfileBitmap = elements[i + 2];

			if (!mProfileMap.containsKey(namedColorSpace)) {
				ArrayMap<Integer, Set<Long>> imageFormatMap = new ArrayMap<>();
				mProfileMap.put(namedColorSpace, imageFormatMap);
			}

			if (!mProfileMap.get(namedColorSpace).containsKey(imageFormat)) {
				ArraySet<Long> dynamicRangeProfiles = new ArraySet<>();
				mProfileMap.get(namedColorSpace).put(imageFormat, dynamicRangeProfiles);
			}

			if (dynamicRangeProfileBitmap != 0) {
				for (long dynamicRangeProfile = DynamicRangeProfiles.STANDARD;
						dynamicRangeProfile < DynamicRangeProfiles.PUBLIC_MAX;
						dynamicRangeProfile <<= 1) {
					if ((dynamicRangeProfileBitmap & dynamicRangeProfile) != 0) {
						mProfileMap.get(namedColorSpace).get(imageFormat).add(dynamicRangeProfile);
					}
				}
			}
		}
	}

	public static void checkProfileValue(int colorSpace) {
		boolean found = false;
		for (ColorSpace.Named value : ColorSpace.Named.values()) {
			if (colorSpace == value.ordinal()) {
				found = true;
				break;
			}
		}

		if (!found) {
			throw new IllegalArgumentException("Unknown ColorSpace " + colorSpace);
		}
	}

	public Map<ColorSpace.Named, Map<Integer, Set<Long>>> getProfileMap() {
		return mProfileMap;
	}

	public Set<ColorSpace.Named> getSupportedColorSpaces(
			int imageFormat) {
		ArraySet<ColorSpace.Named> supportedColorSpaceProfiles = new ArraySet<>();
		for (ColorSpace.Named colorSpace : mProfileMap.keySet()) {
			if (imageFormat == ImageFormat.UNKNOWN) {
				supportedColorSpaceProfiles.add(colorSpace);
			} else {
				Map<Integer, Set<Long>> imageFormatMap = mProfileMap.get(colorSpace);
				if (imageFormatMap.containsKey(imageFormat)) {
					supportedColorSpaceProfiles.add(colorSpace);
				}
			}
		}
		return supportedColorSpaceProfiles;
	}

	public Set<Integer> getSupportedImageFormatsForColorSpace(
			ColorSpace.Named colorSpace) {
		Map<Integer, Set<Long>> imageFormatMap = mProfileMap.get(colorSpace);
		if (imageFormatMap == null) {
			return new ArraySet<Integer>();
		}

		return imageFormatMap.keySet();
	}

	public Set<Long> getSupportedDynamicRangeProfiles(ColorSpace.Named colorSpace,
			int imageFormat) {
		Map<Integer, Set<Long>> imageFormatMap = mProfileMap.get(colorSpace);
		if (imageFormatMap == null) {
			return new ArraySet<Long>();
		}

		Set<Long> dynamicRangeProfiles = null;
		if (imageFormat == ImageFormat.UNKNOWN) {
			dynamicRangeProfiles = new ArraySet<>();
			for (int supportedImageFormat : imageFormatMap.keySet()) {
				Set<Long> supportedDynamicRangeProfiles = imageFormatMap.get(
						supportedImageFormat);
				for (Long supportedDynamicRangeProfile : supportedDynamicRangeProfiles) {
					dynamicRangeProfiles.add(supportedDynamicRangeProfile);
				}
			}
		} else {
			dynamicRangeProfiles = imageFormatMap.get(imageFormat);
			if (dynamicRangeProfiles == null) {
				return new ArraySet<>();
			}
		}

		return dynamicRangeProfiles;
	}

	public Set<ColorSpace.Named> getSupportedColorSpacesForDynamicRange(
			int imageFormat,
			long dynamicRangeProfile) {
		ArraySet<ColorSpace.Named> supportedColorSpaceProfiles = new ArraySet<>();
		for (ColorSpace.Named colorSpace : mProfileMap.keySet()) {
			Map<Integer, Set<Long>> imageFormatMap = mProfileMap.get(colorSpace);
			if (imageFormat == ImageFormat.UNKNOWN) {
				for (int supportedImageFormat : imageFormatMap.keySet()) {
					Set<Long> dynamicRangeProfiles = imageFormatMap.get(supportedImageFormat);
					if (dynamicRangeProfiles.contains(dynamicRangeProfile)) {
						supportedColorSpaceProfiles.add(colorSpace);
					}
				}
			} else if (imageFormatMap.containsKey(imageFormat)) {
				Set<Long> dynamicRangeProfiles = imageFormatMap.get(imageFormat);
				if (dynamicRangeProfiles.contains(dynamicRangeProfile)) {
					supportedColorSpaceProfiles.add(colorSpace);
				}
			}
		}
		return supportedColorSpaceProfiles;
	}
}
