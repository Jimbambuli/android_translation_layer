package android.hardware.camera2;

import android.hardware.camera2.impl.CameraDeviceImpl;
import android.hardware.camera2.impl.ExternalCameraDevice;
import android.os.Handler;
import android.os.Looper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

/* AOSP core/java/android/hardware/camera2/CameraManager.java over ATL's camera HAL (impl.ExternalCameraDevice,
 * the cameras of PipeWire) in place of the camera service: the cameras the HAL reports, their characteristics,
 * and openCamera() connecting a CameraDeviceImpl to the HAL. A camera that appears or goes away while the app
 * runs is not reported (the list is read once). Errors the service would report keep AOSP's exceptions:
 * IllegalArgumentException for an unknown camera id. */
public final class CameraManager {

	private final Map<AvailabilityCallback, Executor> mCallbackMap = new HashMap<AvailabilityCallback, Executor>();

	/* without asking the user (inside a Flatpak the portal's IsCameraPresent stands in, see ExternalCameraDevice) */
	public String[] getCameraIdList() throws CameraAccessException {
		return ExternalCameraDevice.getCameraIds();
	}

	private static boolean isKnownId(String cameraId) {
		return java.util.Arrays.asList(ExternalCameraDevice.getCameraIds()).contains(cameraId);
	}

	/* AOSP: the HAL lists no concurrent camera combinations */
	public Set<Set<String>> getConcurrentCameraIds() throws CameraAccessException {
		return new HashSet<Set<String>>();
	}

	/* the service's ERROR_ILLEGAL_ARGUMENT for an unknown camera; inside a Flatpak the camera portal asks the user
	 * here if needed (the camera's data comes only through it) */
	private static ExternalCameraDevice getDevice(String cameraId) {
		if (cameraId == null) {
			throw new IllegalArgumentException("cameraId was null");
		}
		ExternalCameraDevice device = ExternalCameraDevice.get(cameraId);
		if (device == null) {
			throw new IllegalArgumentException("Camera ID '" + cameraId + "' not available on device.");
		}
		return device;
	}

	/* AOSP getCameraCharacteristics() -> prepareCameraCharacteristics(): the camera id and the display size go into
	 * the metadata for the mandatory stream combinations */
	public CameraCharacteristics getCameraCharacteristics(String cameraId) throws CameraAccessException {
		CameraCharacteristics characteristics = getDevice(cameraId).getCharacteristics();
		try {
			characteristics.setCameraId(Integer.parseInt(cameraId));
		} catch (NumberFormatException e) {
		}
		characteristics.setDisplaySize(getDisplaySize());
		return characteristics;
	}

	/* AOSP getDisplaySize(): the default display's real size, landscape */
	private static android.util.Size getDisplaySize() {
		android.graphics.Point sz = new android.graphics.Point();
		new android.hardware.display.DisplayManager().getDisplay(android.view.Display.DEFAULT_DISPLAY).getRealSize(sz);
		int width = sz.x;
		int height = sz.y;
		if (height > width) {
			height = width;
			width = sz.y;
		}
		return new android.util.Size(width, height);
	}

	/* AOSP isCameraDeviceSetupSupported(): the external HAL has no CameraDeviceSetup (AIDL device version
	 * below 1.3) */
	public boolean isCameraDeviceSetupSupported(String cameraId) throws CameraAccessException {
		if (cameraId == null) {
			throw new IllegalArgumentException("Camera ID was null");
		}
		if (!isKnownId(cameraId)) {
			throw new IllegalArgumentException("Camera ID '" + cameraId + "' not available on device.");
		}
		return false;
	}

	public CameraDevice.CameraDeviceSetup getCameraDeviceSetup(String cameraId) throws CameraAccessException {
		if (!isCameraDeviceSetupSupported(cameraId))
			throw new UnsupportedOperationException("CameraDeviceSetup is not supported for Camera ID: " + cameraId);
		return null;
	}

	public void openCamera(String cameraId, CameraDevice.StateCallback callback, Handler handler) throws CameraAccessException {
		openCameraImpl(cameraId, callback, checkAndWrapHandler(handler));
	}

	public void openCamera(String cameraId, Executor executor, CameraDevice.StateCallback callback) throws CameraAccessException {
		if (executor == null) {
			throw new IllegalArgumentException("executor was null");
		}
		openCameraImpl(cameraId, callback, executor);
	}

	/* AOSP openCameraImpl() -> openCameraDeviceUserAsync(): the device reports onOpened (or onError) on the
	 * executor once it is connected */
	private void openCameraImpl(String cameraId, CameraDevice.StateCallback callback, Executor executor) throws CameraAccessException {
		if (cameraId == null) {
			throw new IllegalArgumentException("cameraId was null");
		} else if (callback == null) {
			throw new IllegalArgumentException("callback was null");
		}
		ExternalCameraDevice hal = ExternalCameraDevice.get(cameraId);
		if (hal == null && ExternalCameraDevice.accessDenied()) {
			/* the service's ERROR_PERMISSION_DENIED: the user refused the camera (portal) */
			throw new SecurityException("Caller cannot open camera \"" + cameraId + "\" without camera permission");
		}
		if (hal == null) {
			throw new IllegalArgumentException("Camera ID '" + cameraId + "' not available on device.");
		}
		CameraDeviceImpl device = new CameraDeviceImpl(cameraId, callback, executor, hal.getCharacteristics(), hal);
		device.connect();
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

	/* CameraService::setTorchMode()/turnOnTorchWithStrengthLevel(): an unknown id, then no flash unit, are its
	 * ERROR_ILLEGAL_ARGUMENT */
	private static void checkTorchCamera(String cameraId) {
		if (cameraId == null)
			throw new IllegalArgumentException("cameraId was null");
		if (!isKnownId(cameraId))
			throw new IllegalArgumentException("Camera ID \"" + cameraId + "\" is a not valid camera ID");
		throw new IllegalArgumentException("Camera \"" + cameraId + "\" does not have a flash unit");
	}

	/* CameraService::getTorchStrengthLevel(): the external HAL cannot report one, which the service gives as
	 * ERROR_INVALID_OPERATION, i.e. CameraAccessException(CAMERA_ERROR) */
	public int getTorchStrengthLevel(String cameraId) throws CameraAccessException {
		if (cameraId == null)
			throw new IllegalArgumentException("cameraId was null");
		if (!isKnownId(cameraId))
			throw new IllegalArgumentException("Camera ID \"" + cameraId + "\" is a not valid camera ID");
		throw new CameraAccessException(CameraAccessException.CAMERA_ERROR, "Unable to retrieve torch strength level for device " + cameraId);
	}

	/* AOSP CameraManagerGlobal.isConcurrentSessionConfigurationSupported(): the ids must form one of the concurrent
	 * combinations, of which the HAL has none */
	public boolean isConcurrentSessionConfigurationSupported(java.util.Map<String, android.hardware.camera2.params.SessionConfiguration> cameraIdAndSessionConfig) throws CameraAccessException {
		if (cameraIdAndSessionConfig == null) {
			throw new IllegalArgumentException("cameraIdsAndSessionConfigurations was null");
		}
		if (cameraIdAndSessionConfig.size() == 0) {
			throw new IllegalArgumentException("camera id and session combination is empty");
		}
		boolean subsetFound = false;
		for (Set<String> combination : getConcurrentCameraIds()) {
			if (combination.containsAll(cameraIdAndSessionConfig.keySet()))
				subsetFound = true;
		}
		return subsetFound;
	}

	/* AOSP: the service's ERROR_ILLEGAL_ARGUMENT for a camera without a flash unit (FLASH_INFO_AVAILABLE false) */
	public void turnOnTorchWithStrengthLevel(String cameraId, int torchStrength) throws CameraAccessException {
		checkTorchCamera(cameraId);
	}

	public CameraExtensionCharacteristics getCameraExtensionCharacteristics(String cameraId) throws CameraAccessException {
		getCameraCharacteristics(cameraId);
		return new CameraExtensionCharacteristics();
	}

	/* AOSP CameraManager.TorchCallback */
	public static abstract class TorchCallback {
		public void onTorchModeUnavailable(String cameraId) {}

		public void onTorchModeChanged(String cameraId, boolean enabled) {}

		public void onTorchStrengthLevelChanged(String cameraId, int newStrengthLevel) {}
	}

	public void setTorchMode(String cameraId, boolean enabled) throws CameraAccessException {
		checkTorchCamera(cameraId);
	}

	public void registerAvailabilityCallback(AvailabilityCallback callback, Handler handler) {
		registerAvailabilityCallbackImpl(callback, checkAndWrapHandler(handler));
	}

	public void registerAvailabilityCallback(Executor executor, AvailabilityCallback callback) {
		if (executor == null) {
			throw new IllegalArgumentException("executor was null");
		}
		registerAvailabilityCallbackImpl(callback, executor);
	}

	/* AOSP CameraManagerGlobal.registerAvailabilityCallback() -> updateCallbackLocked(): the new callback hears
	 * of each present camera */
	private void registerAvailabilityCallbackImpl(AvailabilityCallback callback, Executor executor) {
		synchronized (mCallbackMap) {
			if (mCallbackMap.put(callback, executor) != null)
				return;
		}
		for (String id : ExternalCameraDevice.getCameraIds())
			executor.execute(() -> callback.onCameraAvailable(id));
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
