package android.media;

import android.hardware.Camera;

/* AOSP media/java/android/media/CamcorderProfile.java. ATL has no camera and no media profiles for one:
 * hasProfile() is false (MediaProfiles::hasCamcorderProfile finds none), get(int) finds no back-facing
 * camera and returns null, get(int, int) throws RuntimeException("Error retrieving camcorder profile
 * params") as media/jni/android_media_MediaProfiles.cpp does when the parameters are missing, and
 * getAll() returns null (no profile). The quality checks are AOSP's. */
public class CamcorderProfile {
	public static final int QUALITY_LOW  = 0;
	public static final int QUALITY_HIGH = 1;
	public static final int QUALITY_QCIF = 2;
	public static final int QUALITY_CIF = 3;
	public static final int QUALITY_480P = 4;
	public static final int QUALITY_720P = 5;
	public static final int QUALITY_1080P = 6;
	public static final int QUALITY_QVGA = 7;
	public static final int QUALITY_2160P = 8;
	public static final int QUALITY_VGA = 9;
	public static final int QUALITY_4KDCI = 10;
	public static final int QUALITY_QHD = 11;
	public static final int QUALITY_2K = 12;
	public static final int QUALITY_8KUHD = 13;
	private static final int QUALITY_LIST_START = QUALITY_LOW;
	private static final int QUALITY_LIST_END = QUALITY_8KUHD;
	public static final int QUALITY_TIME_LAPSE_LOW  = 1000;
	public static final int QUALITY_TIME_LAPSE_HIGH = 1001;
	public static final int QUALITY_TIME_LAPSE_QCIF = 1002;
	public static final int QUALITY_TIME_LAPSE_CIF = 1003;
	public static final int QUALITY_TIME_LAPSE_480P = 1004;
	public static final int QUALITY_TIME_LAPSE_720P = 1005;
	public static final int QUALITY_TIME_LAPSE_1080P = 1006;
	public static final int QUALITY_TIME_LAPSE_QVGA = 1007;
	public static final int QUALITY_TIME_LAPSE_2160P = 1008;
	public static final int QUALITY_TIME_LAPSE_VGA = 1009;
	public static final int QUALITY_TIME_LAPSE_4KDCI = 1010;
	public static final int QUALITY_TIME_LAPSE_QHD = 1011;
	public static final int QUALITY_TIME_LAPSE_2K = 1012;
	public static final int QUALITY_TIME_LAPSE_8KUHD = 1013;
	private static final int QUALITY_TIME_LAPSE_LIST_START = QUALITY_TIME_LAPSE_LOW;
	private static final int QUALITY_TIME_LAPSE_LIST_END = QUALITY_TIME_LAPSE_8KUHD;
	public static final int QUALITY_HIGH_SPEED_LOW = 2000;
	public static final int QUALITY_HIGH_SPEED_HIGH = 2001;
	public static final int QUALITY_HIGH_SPEED_480P = 2002;
	public static final int QUALITY_HIGH_SPEED_720P = 2003;
	public static final int QUALITY_HIGH_SPEED_1080P = 2004;
	public static final int QUALITY_HIGH_SPEED_2160P = 2005;
	public static final int QUALITY_HIGH_SPEED_CIF = 2006;
	public static final int QUALITY_HIGH_SPEED_VGA = 2007;
	public static final int QUALITY_HIGH_SPEED_4KDCI = 2008;
	private static final int QUALITY_HIGH_SPEED_LIST_START = QUALITY_HIGH_SPEED_LOW;
	private static final int QUALITY_HIGH_SPEED_LIST_END = QUALITY_HIGH_SPEED_4KDCI;

	public int duration;
	public int quality;
	public int fileFormat;
	public int videoCodec;
	public int videoBitRate;
	public int videoFrameRate;
	public int videoFrameWidth;
	public int videoFrameHeight;
	public int audioCodec;
	public int audioBitRate;
	public int audioSampleRate;
	public int audioChannels;

	private CamcorderProfile() {
	}

	private static boolean isQualityInRange(int quality) {
		return (quality >= QUALITY_LIST_START && quality <= QUALITY_LIST_END) ||
		       (quality >= QUALITY_TIME_LAPSE_LIST_START && quality <= QUALITY_TIME_LAPSE_LIST_END) ||
		       (quality >= QUALITY_HIGH_SPEED_LIST_START && quality <= QUALITY_HIGH_SPEED_LIST_END);
	}

	public static CamcorderProfile get(int quality) {
		int numberOfCameras = Camera.getNumberOfCameras();
		Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
		for (int i = 0; i < numberOfCameras; i++) {
			Camera.getCameraInfo(i, cameraInfo);
			if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
				return get(i, quality);
			}
		}
		return null;
	}

	public static CamcorderProfile get(int cameraId, int quality) {
		if (!isQualityInRange(quality)) {
			String errMessage = "Unsupported quality level: " + quality;
			throw new IllegalArgumentException(errMessage);
		}
		throw new RuntimeException("Error retrieving camcorder profile params");
	}

	public static EncoderProfiles getAll(String cameraId, int quality) {
		if (!isQualityInRange(quality)) {
			String errMessage = "Unsupported quality level: " + quality;
			throw new IllegalArgumentException(errMessage);
		}
		return null;
	}

	public static boolean hasProfile(int quality) {
		int numberOfCameras = Camera.getNumberOfCameras();
		Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
		for (int i = 0; i < numberOfCameras; i++) {
			Camera.getCameraInfo(i, cameraInfo);
			if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
				return hasProfile(i, quality);
			}
		}
		return false;
	}

	public static boolean hasProfile(int cameraId, int quality) {
		return false;
	}
}
