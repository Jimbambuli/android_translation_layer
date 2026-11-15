package android.hardware.camera2;


import java.util.List;

/* AOSP core/java/android/hardware/camera2/CameraConstrainedHighSpeedCaptureSession.java */
public abstract class CameraConstrainedHighSpeedCaptureSession extends CameraCaptureSession {

	public abstract List<CaptureRequest> createHighSpeedRequestList(
			CaptureRequest request) throws CameraAccessException;

}
