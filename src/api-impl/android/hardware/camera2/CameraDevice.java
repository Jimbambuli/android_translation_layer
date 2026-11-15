package android.hardware.camera2;

import android.hardware.camera2.params.InputConfiguration;
import android.hardware.camera2.params.OutputConfiguration;
import android.hardware.camera2.params.SessionConfiguration;
import android.os.Handler;
import android.view.Surface;
import java.util.List;
import java.util.Set;

/* AOSP core/java/android/hardware/camera2/CameraDevice.java. ATL has no camera service, so
 * CameraManager.openCamera() never hands out an instance; the abstract class and its StateCallback
 * exist so apps can link and subclass them. The members are
 * declared from AOSP (the hidden createCustomCaptureSession() is left out). */
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

	public static final int SESSION_OPERATION_MODE_NORMAL = 0;
	public static final int SESSION_OPERATION_MODE_CONSTRAINED_HIGH_SPEED = 1;
	public static final int SESSION_OPERATION_MODE_VENDOR_START = 0x8000;

	@Deprecated
	public abstract void createCaptureSession(List<Surface> outputs,
			CameraCaptureSession.StateCallback callback, Handler handler)
			throws CameraAccessException;

	@Deprecated
	public abstract void createCaptureSessionByOutputConfigurations(
			List<OutputConfiguration> outputConfigurations,
			CameraCaptureSession.StateCallback callback, Handler handler)
			throws CameraAccessException;
	
	@Deprecated
	public abstract void createReprocessableCaptureSession(InputConfiguration inputConfig,
			List<Surface> outputs, CameraCaptureSession.StateCallback callback,
			Handler handler)
			throws CameraAccessException;

	@Deprecated
	public abstract void createReprocessableCaptureSessionByConfigurations(
			InputConfiguration inputConfig,
			List<OutputConfiguration> outputs,
			CameraCaptureSession.StateCallback callback,
			Handler handler)
			throws CameraAccessException;

	@Deprecated
	public abstract void createConstrainedHighSpeedCaptureSession(List<Surface> outputs,
			CameraCaptureSession.StateCallback callback,
			Handler handler)
			throws CameraAccessException;

	public void createExtensionSession(
			android.hardware.camera2.params.ExtensionSessionConfiguration extensionConfiguration)
			throws CameraAccessException {
		throw new UnsupportedOperationException("No default implementation");
	}

	public void createCaptureSession(
			SessionConfiguration config) throws CameraAccessException {
		throw new UnsupportedOperationException("No default implementation");
	}

	public abstract CaptureRequest.Builder createCaptureRequest(int templateType)
			throws CameraAccessException;

	public CaptureRequest.Builder createCaptureRequest(int templateType,
			Set<String> physicalCameraIdSet) throws CameraAccessException {
		throw new UnsupportedOperationException("Subclasses must override this method");
	}

	public abstract CaptureRequest.Builder createReprocessCaptureRequest(
			TotalCaptureResult inputResult) throws CameraAccessException;

	public boolean isSessionConfigurationSupported(
			SessionConfiguration sessionConfig) throws CameraAccessException {
		throw new UnsupportedOperationException("Subclasses must override this method");
	}

	public void setCameraAudioRestriction(
			int mode) throws CameraAccessException {
		throw new UnsupportedOperationException("Subclasses must override this method");
	}

	public int getCameraAudioRestriction() throws CameraAccessException {
		throw new UnsupportedOperationException("Subclasses must override this method");
	}


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
