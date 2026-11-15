package android.hardware.camera2.params;




import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/* AOSP core/java/android/hardware/camera2/params/DynamicRangeProfiles.java (comments and annotations
 * removed, CameraMetadata values inlined) */
public final class DynamicRangeProfiles {
	
	public static final long STANDARD =
			0x1 /*CameraMetadata.REQUEST_AVAILABLE_DYNAMIC_RANGE_PROFILES_MAP_STANDARD*/;

	public static final long HLG10  =
			0x2 /*CameraMetadata.REQUEST_AVAILABLE_DYNAMIC_RANGE_PROFILES_MAP_HLG10*/;

	public static final long HDR10  =
			0x4 /*CameraMetadata.REQUEST_AVAILABLE_DYNAMIC_RANGE_PROFILES_MAP_HDR10*/;

	public static final long HDR10_PLUS =
			0x8 /*CameraMetadata.REQUEST_AVAILABLE_DYNAMIC_RANGE_PROFILES_MAP_HDR10_PLUS*/;

	public static final long DOLBY_VISION_10B_HDR_REF =
			0x10 /*CameraMetadata.REQUEST_AVAILABLE_DYNAMIC_RANGE_PROFILES_MAP_DOLBY_VISION_10B_HDR_REF*/;

	public static final long DOLBY_VISION_10B_HDR_REF_PO =
			0x20 /*CameraMetadata.REQUEST_AVAILABLE_DYNAMIC_RANGE_PROFILES_MAP_DOLBY_VISION_10B_HDR_REF_PO*/;

	public static final long DOLBY_VISION_10B_HDR_OEM =
			0x40 /*CameraMetadata.REQUEST_AVAILABLE_DYNAMIC_RANGE_PROFILES_MAP_DOLBY_VISION_10B_HDR_OEM*/;

	public static final long DOLBY_VISION_10B_HDR_OEM_PO =
			0x80 /*CameraMetadata.REQUEST_AVAILABLE_DYNAMIC_RANGE_PROFILES_MAP_DOLBY_VISION_10B_HDR_OEM_PO*/;

	public static final long DOLBY_VISION_8B_HDR_REF =
			0x100 /*CameraMetadata.REQUEST_AVAILABLE_DYNAMIC_RANGE_PROFILES_MAP_DOLBY_VISION_8B_HDR_REF*/;

	public static final long DOLBY_VISION_8B_HDR_REF_PO =
			0x200 /*CameraMetadata.REQUEST_AVAILABLE_DYNAMIC_RANGE_PROFILES_MAP_DOLBY_VISION_8B_HDR_REF_PO*/;

	public static final long DOLBY_VISION_8B_HDR_OEM =
			0x400 /*CameraMetadata.REQUEST_AVAILABLE_DYNAMIC_RANGE_PROFILES_MAP_DOLBY_VISION_8B_HDR_OEM*/;

	public static final long DOLBY_VISION_8B_HDR_OEM_PO =
			0x800 /*CameraMetadata.REQUEST_AVAILABLE_DYNAMIC_RANGE_PROFILES_MAP_DOLBY_VISION_8B_HDR_OEM_PO*/;

	public static final long PUBLIC_MAX =
			0x1000 /*CameraMetadata.REQUEST_AVAILABLE_DYNAMIC_RANGE_PROFILES_MAP_MAX*/;


	private final HashMap<Long, Set<Long>> mProfileMap = new HashMap<>();
	private final HashMap<Long, Boolean> mLookahedLatencyMap = new HashMap<>();

	public DynamicRangeProfiles(final long[] elements) {
		if ((elements.length % 3) != 0) {
			throw new IllegalArgumentException("Dynamic range profile map length " +
					elements.length + " is not even!");
		}

		for (int i = 0; i < elements.length; i += 3) {
			checkProfileValue(elements[i]);
			if (elements[i] == STANDARD) {
				throw new IllegalArgumentException("Dynamic range profile map must not include a"
						+ " STANDARD profile entry!");
			}
			HashSet<Long> profiles = new HashSet<>();

			if (elements[i+1] != 0) {
				for (long profile = STANDARD; profile < PUBLIC_MAX; profile <<= 1) {
					if ((elements[i+1] & profile) != 0) {
						profiles.add(profile);
					}
				}
			}

			mProfileMap.put(elements[i], profiles);
			mLookahedLatencyMap.put(elements[i], elements[i+2] != 0L);
		}
		HashSet<Long> standardConstraints = new HashSet<>();
		standardConstraints.add(STANDARD);
		for(Long profile : mProfileMap.keySet()) {
			if (mProfileMap.get(profile).isEmpty() || mProfileMap.get(profile).contains(STANDARD)) {
				standardConstraints.add(profile);
			}
		}

		mProfileMap.put(STANDARD, standardConstraints);
		mLookahedLatencyMap.put(STANDARD, false);
	}

	public static void checkProfileValue(long profile) {
		if (profile == STANDARD || profile == HLG10 || profile == HDR10 || profile == HDR10_PLUS
				|| profile == DOLBY_VISION_10B_HDR_REF || profile == DOLBY_VISION_10B_HDR_REF_PO
				|| profile == DOLBY_VISION_10B_HDR_OEM || profile == DOLBY_VISION_10B_HDR_OEM_PO
				|| profile == DOLBY_VISION_8B_HDR_REF || profile == DOLBY_VISION_8B_HDR_REF_PO
				|| profile == DOLBY_VISION_8B_HDR_OEM
				|| profile == DOLBY_VISION_8B_HDR_OEM_PO) {//No-op
		} else {
			throw new IllegalArgumentException("Unknown profile " + profile);
		}
	}

	 public Set<Long> getSupportedProfiles() {
		 return Collections.unmodifiableSet(mProfileMap.keySet());
	 }

	 public Set<Long> getProfileCaptureRequestConstraints(long profile) {
		 Set<Long> ret = mProfileMap.get(profile);
		 if (ret == null) {
			 throw new IllegalArgumentException("Unsupported profile!");
		 }

		 return Collections.unmodifiableSet(ret);
	 }

	public boolean isExtraLatencyPresent(long profile) {
		Boolean ret = mLookahedLatencyMap.get(profile);
		if (ret == null) {
			throw new IllegalArgumentException("Unsupported profile!");
		}

		return ret;
	}
}
