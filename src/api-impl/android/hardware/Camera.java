package android.hardware;

public class Camera {

	public interface PreviewCallback {}

	public interface AutoFocusCallback {}

	public interface ErrorCallback {}

	public static class CameraInfo {
		public static final int CAMERA_FACING_BACK = 0;
		public static final int CAMERA_FACING_FRONT = 1;
		public int facing;
		public int orientation;
		public boolean canDisableShutterSound;
	}

	public static int getNumberOfCameras() { return 0; }

	/* ATL has no camera, so this fails like AOSP's native _getCameraInfo() for an unknown camera id */
	public static void getCameraInfo(int cameraId, CameraInfo cameraInfo) {
		throw new RuntimeException("Fail to get camera info");
	}

	public static Camera open() {
		return null;
	}

	public static Camera open(int cameraId) {
		return null;
	}

	public void setErrorCallback(ErrorCallback callback) {}
}
