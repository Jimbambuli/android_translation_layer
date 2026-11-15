package android.hardware.camera2;

/* AOSP core/java/android/hardware/camera2/CameraDevice.java. ATL has no camera service, so
 * CameraManager.openCamera() never hands out an instance; the abstract class and its StateCallback
 * exist so apps can link and subclass them. Only the members that need no further camera2 classes are
 * declared; the capture-session factories are left out until CaptureRequest and friends exist. */
public abstract class CameraDevice implements AutoCloseable {

	public static final int TEMPLATE_PREVIEW = 1;
	public static final int TEMPLATE_STILL_CAPTURE = 2;
	public static final int TEMPLATE_RECORD = 3;
	public static final int TEMPLATE_VIDEO_SNAPSHOT = 4;
	public static final int TEMPLATE_ZERO_SHUTTER_LAG = 5;
	public static final int TEMPLATE_MANUAL = 6;

	public static final int AUDIO_RESTRICTION_NONE = 0;
	public static final int AUDIO_RESTRICTION_VIBRATION = 1;
	public static final int AUDIO_RESTRICTION_VIBRATION_SOUND = 3;

	public abstract String getId();

	@Override
	public abstract void close();

	public static abstract class StateCallback {
		public static final int ERROR_CAMERA_IN_USE = 1;
		public static final int ERROR_MAX_CAMERAS_IN_USE = 2;
		public static final int ERROR_CAMERA_DISABLED = 3;
		public static final int ERROR_CAMERA_DEVICE = 4;
		public static final int ERROR_CAMERA_SERVICE = 5;

		public abstract void onOpened(CameraDevice camera); // Must implement

		public void onClosed(CameraDevice camera) {
			// Default empty implementation
		}

		public abstract void onDisconnected(CameraDevice camera); // Must implement

		public abstract void onError(CameraDevice camera, int error); // Must implement
	}
}
