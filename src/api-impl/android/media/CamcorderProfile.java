package android.media;

import android.hardware.Camera;

/* AOSP media/java/android/media/CamcorderProfile.java. The profiles are ATL's (a device's media_profiles.xml): for each
 * camera the standard qualities whose size the camera gives, recorded as MPEG-4 with H.264 and AAC (the encoders ATL
 * has, MediaRecorder); QUALITY_LOW and QUALITY_HIGH are the lowest and the highest of them, as in AOSP's
 * MediaProfiles. Without an H.264 encoder or a camera there is no profile: hasProfile() is false, get(int) finds no
 * back-facing camera and returns null, get(int, int) throws RuntimeException("Error retrieving camcorder profile
 * params") as media/jni/android_media_MediaProfiles.cpp does when the parameters are missing, and getAll() returns null.
 * Time lapse and high speed qualities have no profile. The quality checks are AOSP's. */
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
		CamcorderProfile profile = profile(cameraId, quality);
		if (profile == null)
			throw new RuntimeException("Error retrieving camcorder profile params");
		return profile;
	}

	/* the qualities of ATL's profiles by their frame size, lowest first */
	private static final int[][] QUALITY_SIZES = {
		{QUALITY_QCIF, 176, 144}, {QUALITY_QVGA, 320, 240}, {QUALITY_CIF, 352, 288}, {QUALITY_VGA, 640, 480},
		{QUALITY_480P, 720, 480}, {QUALITY_720P, 1280, 720}, {QUALITY_1080P, 1920, 1080}, {QUALITY_2160P, 3840, 2160},
	};

	private static CamcorderProfile profile(int cameraId, int quality) {
		java.util.List<android.hardware.camera2.impl.ExternalCameraDevice> devices = android.hardware.camera2.impl.ExternalCameraDevice.getDevices(false);
		if (cameraId < 0 || cameraId >= devices.size() || quality >= QUALITY_TIME_LAPSE_LIST_START)
			return null;
		if (new MediaCodecList(MediaCodecList.REGULAR_CODECS).findEncoderForFormat(MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, 176, 144)) == null)
			return null;
		android.hardware.camera2.params.StreamConfigurationMap map = devices.get(cameraId).getCharacteristics().get(android.hardware.camera2.CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
		android.util.Size[] sizes = map != null ? map.getOutputSizes(android.graphics.ImageFormat.YUV_420_888) : null;
		java.util.ArrayList<int[]> available = new java.util.ArrayList<>();
		for (int[] q : QUALITY_SIZES)
			for (int i = 0; sizes != null && i < sizes.length; i++)
				if (sizes[i].getWidth() == q[1] && sizes[i].getHeight() == q[2]) {
					available.add(q);
					break;
				}
		if (available.isEmpty())
			return null;
		int[] chosen = null;
		if (quality == QUALITY_LOW)
			chosen = available.get(0);
		else if (quality == QUALITY_HIGH)
			chosen = available.get(available.size() - 1);
		for (int[] q : available)
			if (q[0] == quality)
				chosen = q;
		if (chosen == null)
			return null;
		CamcorderProfile profile = new CamcorderProfile();
		profile.duration = 60;
		profile.quality = quality;
		profile.fileFormat = MediaRecorder.OutputFormat.MPEG_4;
		profile.videoCodec = MediaRecorder.VideoEncoder.H264;
		profile.videoFrameWidth = chosen[1];
		profile.videoFrameHeight = chosen[2];
		profile.videoFrameRate = 30;
		profile.videoBitRate = Math.max(192000, chosen[1] * chosen[2] * 6); /* about 12 Mbit/s for 1080p */
		profile.audioCodec = MediaRecorder.AudioEncoder.AAC;
		profile.audioBitRate = 96000;
		profile.audioSampleRate = 48000;
		profile.audioChannels = 1;
		return profile;
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
		return profile(cameraId, quality) != null;
	}
}
