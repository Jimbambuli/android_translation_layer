package android.hardware.camera2;

import android.os.Handler;
import android.os.Looper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

/* AOSP core/java/android/hardware/camera2/CameraManager.java on a device whose camera service is
 * disabled (CameraManagerGlobal.sCameraServiceDisabled, config.disable_cameraservice): ATL has no
 * camera service. The id list is empty, getCameraCharacteristics()/openCamera()/setTorchMode() throw
 * IllegalArgumentException("No cameras available on device") after AOSP's argument checks, and
 * availability callbacks are kept but never called, as there is no device to report. */
public class CameraManager {

	private final Map<AvailabilityCallback, Executor> mCallbackMap = new HashMap<AvailabilityCallback, Executor>();

	public String[] getCameraIdList() throws CameraAccessException {
		return new String[] {};
	}

	public Set<Set<String>> getConcurrentCameraIds() throws CameraAccessException {
		return new HashSet<Set<String>>();
	}

	public CameraCharacteristics getCameraCharacteristics(String cameraId) throws CameraAccessException {
		throw new IllegalArgumentException("No cameras available on device");
	}

	/* AOSP isCameraDeviceSetupSupported()/getCameraDeviceSetup(): the id is checked against the (empty)
	 * camera id list first */
	public boolean isCameraDeviceSetupSupported(String cameraId) throws CameraAccessException {
		if (cameraId == null) {
			throw new IllegalArgumentException("Camera ID was null");
		}
		throw new IllegalArgumentException("Camera ID '" + cameraId + "' not available on device.");
	}

	public CameraDevice.CameraDeviceSetup getCameraDeviceSetup(String cameraId) throws CameraAccessException {
		isCameraDeviceSetupSupported(cameraId); /* throws, see above */
		throw new UnsupportedOperationException("CameraDeviceSetup is not supported for Camera ID: " + cameraId);
	}

	public void openCamera(String cameraId, CameraDevice.StateCallback callback, Handler handler) throws CameraAccessException {
		openCameraForUid(cameraId, callback);
	}

	public void openCamera(String cameraId, Executor executor, CameraDevice.StateCallback callback) throws CameraAccessException {
		if (executor == null) {
			throw new IllegalArgumentException("executor was null");
		}
		openCameraForUid(cameraId, callback);
	}

	private void openCameraForUid(String cameraId, CameraDevice.StateCallback callback) {
		if (cameraId == null) {
			throw new IllegalArgumentException("cameraId was null");
		} else if (callback == null) {
			throw new IllegalArgumentException("callback was null");
		}
		throw new IllegalArgumentException("No cameras available on device");
	}

	/* AOSP CameraManagerGlobal.registerTorchCallback(): stored; with the service disabled there is no torch
	 * status to report */
	private final Map<TorchCallback, Executor> mTorchCallbackMap = new HashMap<TorchCallback, Executor>();

	public void registerTorchCallback(TorchCallback callback, Handler handler) {
		synchronized (mTorchCallbackMap) {
			mTorchCallbackMap.put(callback, checkAndWrapHandler(handler));
		}
	}

	public void registerTorchCallback(Executor executor, TorchCallback callback) {
		if (executor == null)
			throw new IllegalArgumentException("executor was null");
		synchronized (mTorchCallbackMap) {
			mTorchCallbackMap.put(callback, executor);
		}
	}

	public void unregisterTorchCallback(TorchCallback callback) {
		synchronized (mTorchCallbackMap) {
			mTorchCallbackMap.remove(callback);
		}
	}

	/* AOSP: sCameraServiceDisabled -> IllegalArgumentException (AOSP's wording here is "No camera available") */
	public void turnOnTorchWithStrengthLevel(String cameraId, int torchStrength) throws CameraAccessException {
		throw new IllegalArgumentException("No camera available on device");
	}

	/* AOSP getCameraExtensionCharacteristics(): starts with getCameraCharacteristics(), which fails */
	public CameraExtensionCharacteristics getCameraExtensionCharacteristics(String cameraId) throws CameraAccessException {
		getCameraCharacteristics(cameraId);
		return null;
	}

	/* AOSP CameraManager.TorchCallback */
	public static abstract class TorchCallback {
		public void onTorchModeUnavailable(String cameraId) {}

		public void onTorchModeChanged(String cameraId, boolean enabled) {}

		public void onTorchStrengthLevelChanged(String cameraId, int newStrengthLevel) {}
	}

	public void setTorchMode(String cameraId, boolean enabled) throws CameraAccessException {
		throw new IllegalArgumentException("No cameras available on device");
	}

	public void registerAvailabilityCallback(AvailabilityCallback callback, Handler handler) {
		synchronized (mCallbackMap) {
			mCallbackMap.put(callback, checkAndWrapHandler(handler));
		}
	}

	public void registerAvailabilityCallback(Executor executor, AvailabilityCallback callback) {
		if (executor == null) {
			throw new IllegalArgumentException("executor was null");
		}
		synchronized (mCallbackMap) {
			mCallbackMap.put(callback, executor);
		}
	}

	public void unregisterAvailabilityCallback(AvailabilityCallback callback) {
		synchronized (mCallbackMap) {
			mCallbackMap.remove(callback);
		}
	}

	/* AOSP CameraDeviceImpl.checkAndWrapHandler()/checkHandler() */
	private static Executor checkAndWrapHandler(Handler handler) {
		if (handler == null) {
			Looper looper = Looper.myLooper();
			if (looper == null) {
				throw new IllegalArgumentException("No handler given, and current thread has no looper!");
			}
			handler = new Handler(looper);
		}
		return handler::post;
	}

	public static abstract class AvailabilityCallback {
		public void onCameraAvailable(String cameraId) {
			// default empty implementation
		}

		public void onCameraUnavailable(String cameraId) {
			// default empty implementation
		}

		public void onCameraAccessPrioritiesChanged() {
			// default empty implementation
		}

		public void onPhysicalCameraAvailable(String cameraId, String physicalCameraId) {
			// default empty implementation
		}

		public void onPhysicalCameraUnavailable(String cameraId, String physicalCameraId) {
			// default empty implementation
		}

		public void onCameraOpened(String cameraId, String packageId) {
			// default empty implementation
		}

		public void onCameraClosed(String cameraId) {
			// default empty implementation
		}
	}
}
