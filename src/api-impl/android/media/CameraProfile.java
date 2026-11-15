package android.media;

/* AOSP media/java/android/media/CameraProfile.java. ATL has no camera (Camera.getNumberOfCameras() is 0), so the
 * no-id overload finds no back camera and returns 0 as in AOSP. The levels of an id are those of libmedia's
 * MediaProfiles defaults (the native side of AOSP, not in aosp-src): 50, 60, 70 for low, medium, high. */
public class CameraProfile {
	public static final int QUALITY_LOW = 0;
	public static final int QUALITY_MEDIUM = 1;
	public static final int QUALITY_HIGH = 2;

	private static final int[] LEVELS = {50, 60, 70};

	public static int getJpegEncodingQualityParameter(int quality) {
		int numberOfCameras = android.hardware.Camera.getNumberOfCameras();
		android.hardware.Camera.CameraInfo cameraInfo = new android.hardware.Camera.CameraInfo();
		for (int i = 0; i < numberOfCameras; i++) {
			android.hardware.Camera.getCameraInfo(i, cameraInfo);
			if (cameraInfo.facing == android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK)
				return getJpegEncodingQualityParameter(i, quality);
		}
		return 0;
	}

	public static int getJpegEncodingQualityParameter(int cameraId, int quality) {
		if (quality < QUALITY_LOW || quality > QUALITY_HIGH)
			throw new IllegalArgumentException("Unsupported quality level: " + quality);
		return LEVELS[quality];
	}
}
