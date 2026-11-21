package android.hardware.camera2.impl;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.OutputConfiguration;
import android.os.Handler;
import android.view.Surface;
import java.util.List;
import java.util.concurrent.Executor;

/** @hide AOSP core/java/android/hardware/camera2/impl/CameraCaptureSessionImpl.java: the argument and state checks
 * and the session's state callbacks (onConfigured/onConfigureFailed, onActive/onReady as the device gets
 * requests and runs out of them, onClosed), the requests going to CameraDeviceImpl. AOSP's sequence drainer is
 * the device's idle state here: a closed session reports onClosed once its requests are done. AOSP's hidden
 * CameraCaptureSessionCore interface (shared with the high speed session, which this device has none of) is
 * left out. */
public class CameraCaptureSessionImpl extends CameraCaptureSession {
	private final int mId;
	private final StateCallback mStateCallback;
	private final Executor mStateExecutor;
	private final CameraDeviceImpl mDeviceImpl;
	private final Executor mDeviceExecutor;
	private final boolean mConfigureSuccess;

	private volatile boolean mClosed;
	/* set from the device's stream thread too, which must not wait for this session's monitor */
	private final java.util.concurrent.atomic.AtomicBoolean mClosedReported = new java.util.concurrent.atomic.AtomicBoolean();

	CameraCaptureSessionImpl(int id, StateCallback callback, Executor stateExecutor, CameraDeviceImpl deviceImpl, Executor deviceStateExecutor, boolean configureSuccess) {
		if (callback == null) {
			throw new IllegalArgumentException("callback must not be null");
		}
		mId = id;
		mStateCallback = callback;
		mStateExecutor = stateExecutor;
		mDeviceImpl = deviceImpl;
		mDeviceExecutor = deviceStateExecutor;
		mConfigureSuccess = configureSuccess;
		if (configureSuccess) {
			mStateExecutor.execute(() -> mStateCallback.onConfigured(this));
			/* the device is idle after configuring */
			mStateExecutor.execute(() -> mStateCallback.onReady(this));
		} else {
			mStateExecutor.execute(() -> mStateCallback.onConfigureFailed(this));
			mClosed = true;
		}
	}

	int getSessionId() {
		return mId;
	}

	@Override
	public CameraDevice getDevice() {
		return mDeviceImpl;
	}

	@Override
	public void prepare(Surface surface) throws CameraAccessException {
		checkNotClosed();
		if (surface == null) {
			throw new IllegalArgumentException("Surface must not be null");
		}
		/* nothing to allocate ahead of time: the Surface is ready */
		mStateExecutor.execute(() -> mStateCallback.onSurfacePrepared(this, surface));
	}

	@Override
	public void finalizeOutputConfigurations(List<OutputConfiguration> outputConfigs) throws CameraAccessException {
		checkNotClosed();
		if (outputConfigs == null) {
			throw new IllegalArgumentException("deferredOutputConfigs must not be null");
		}
		throw new IllegalArgumentException("The session has no deferred output configurations");
	}

	@Override
	public int capture(CaptureRequest request, CaptureCallback callback, Handler handler) throws CameraAccessException {
		checkCaptureRequest(request);
		synchronized (this) {
			checkNotClosed();
			handler = CameraDeviceImpl.checkHandler(handler, callback);
			return mDeviceImpl.capture(request, callback, handler != null ? handler::post : null);
		}
	}

	@Override
	public int captureSingleRequest(CaptureRequest request, Executor executor, CaptureCallback callback) throws CameraAccessException {
		if (executor == null) {
			throw new IllegalArgumentException("executor must not be null");
		} else if (callback == null) {
			throw new IllegalArgumentException("callback must not be null");
		}
		checkCaptureRequest(request);
		synchronized (this) {
			checkNotClosed();
			return mDeviceImpl.capture(request, callback, executor);
		}
	}

	private void checkCaptureRequest(CaptureRequest request) {
		if (request == null) {
			throw new IllegalArgumentException("request must not be null");
		} else if (request.isReprocess()) {
			throw new IllegalArgumentException("this capture session cannot handle reprocess requests");
		}
	}

	@Override
	public int captureBurst(List<CaptureRequest> requests, CaptureCallback callback, Handler handler) throws CameraAccessException {
		checkCaptureRequests(requests);
		synchronized (this) {
			checkNotClosed();
			handler = CameraDeviceImpl.checkHandler(handler, callback);
			return mDeviceImpl.captureBurst(requests, callback, handler != null ? handler::post : null);
		}
	}

	@Override
	public int captureBurstRequests(List<CaptureRequest> requests, Executor executor, CaptureCallback callback) throws CameraAccessException {
		if (executor == null) {
			throw new IllegalArgumentException("executor must not be null");
		} else if (callback == null) {
			throw new IllegalArgumentException("callback must not be null");
		}
		checkCaptureRequests(requests);
		synchronized (this) {
			checkNotClosed();
			return mDeviceImpl.captureBurst(requests, callback, executor);
		}
	}

	private void checkCaptureRequests(List<CaptureRequest> requests) {
		if (requests == null) {
			throw new IllegalArgumentException("Requests must not be null");
		} else if (requests.isEmpty()) {
			throw new IllegalArgumentException("Requests must have at least one element");
		}
		for (CaptureRequest request : requests) {
			if (request.isReprocess()) {
				throw new IllegalArgumentException("This capture session cannot handle reprocess requests");
			}
		}
	}

	@Override
	public int setRepeatingRequest(CaptureRequest request, CaptureCallback callback, Handler handler) throws CameraAccessException {
		checkRepeatingRequest(request);
		synchronized (this) {
			checkNotClosed();
			handler = CameraDeviceImpl.checkHandler(handler, callback);
			return mDeviceImpl.setRepeatingRequest(request, callback, handler != null ? handler::post : null);
		}
	}

	@Override
	public int setSingleRepeatingRequest(CaptureRequest request, Executor executor, CaptureCallback callback) throws CameraAccessException {
		if (executor == null) {
			throw new IllegalArgumentException("executor must not be null");
		} else if (callback == null) {
			throw new IllegalArgumentException("callback must not be null");
		}
		checkRepeatingRequest(request);
		synchronized (this) {
			checkNotClosed();
			return mDeviceImpl.setRepeatingRequest(request, callback, executor);
		}
	}

	private void checkRepeatingRequest(CaptureRequest request) {
		if (request == null) {
			throw new IllegalArgumentException("request must not be null");
		} else if (request.isReprocess()) {
			throw new IllegalArgumentException("repeating reprocess requests are not supported");
		}
	}

	@Override
	public int setRepeatingBurst(List<CaptureRequest> requests, CaptureCallback callback, Handler handler) throws CameraAccessException {
		checkRepeatingRequests(requests);
		synchronized (this) {
			checkNotClosed();
			handler = CameraDeviceImpl.checkHandler(handler, callback);
			return mDeviceImpl.setRepeatingBurst(requests, callback, handler != null ? handler::post : null);
		}
	}

	@Override
	public int setRepeatingBurstRequests(List<CaptureRequest> requests, Executor executor, CaptureCallback callback) throws CameraAccessException {
		if (executor == null) {
			throw new IllegalArgumentException("executor must not be null");
		} else if (callback == null) {
			throw new IllegalArgumentException("callback must not be null");
		}
		checkRepeatingRequests(requests);
		synchronized (this) {
			checkNotClosed();
			return mDeviceImpl.setRepeatingBurst(requests, callback, executor);
		}
	}

	private void checkRepeatingRequests(List<CaptureRequest> requests) {
		if (requests == null) {
			throw new IllegalArgumentException("requests must not be null");
		} else if (requests.isEmpty()) {
			throw new IllegalArgumentException("requests must have at least one element");
		}
		for (CaptureRequest r : requests) {
			if (r.isReprocess()) {
				throw new IllegalArgumentException("repeating reprocess burst requests are not supported");
			}
		}
	}

	@Override
	public void stopRepeating() throws CameraAccessException {
		synchronized (this) {
			checkNotClosed();
		}
		mDeviceImpl.stopRepeating();
	}

	@Override
	public void abortCaptures() throws CameraAccessException {
		synchronized (this) {
			checkNotClosed();
		}
		mDeviceImpl.flush();
	}

	@Override
	public boolean isReprocessable() {
		return false;
	}

	@Override
	public Surface getInputSurface() {
		return null;
	}

	@Override
	public void updateOutputConfiguration(OutputConfiguration config) throws CameraAccessException {
		synchronized (this) {
			checkNotClosed();
		}
		throw new IllegalArgumentException("The output configuration is not part of the session's deferred or shared outputs");
	}

	/* AOSP: the camera service answers whether the HAL can keep processing a stream offline; the external HAL
	 * cannot */
	@Override
	public boolean supportsOfflineProcessing(Surface surface) {
		if (surface == null) {
			throw new IllegalArgumentException("Surface must not be null");
		}
		return false;
	}

	/* AOSP close(): stopRepeating(), then onClosed once the device has drained the requests */
	@Override
	public void close() {
		synchronized (this) {
			if (mClosed)
				return;
			mClosed = true;
		}
		try {
			mDeviceImpl.stopRepeating();
		} catch (IllegalStateException | CameraAccessException e) {
			/* the device is closed or failed: the session is done */
		}
		if (mDeviceImpl.isIdle())
			reportClosed();
	}

	/* AOSP replaceSessionClose(): a new session or the device's close ends this one */
	void replaceSessionClose() {
		mClosed = true;
		reportClosed();
	}

	/* the device starts on a request */
	void onActive() {
		if (!mClosed)
			mStateExecutor.execute(() -> mStateCallback.onActive(this));
	}

	/* the device has no request left */
	void onIdle() {
		if (mClosed) {
			reportClosed();
		} else {
			mStateExecutor.execute(() -> {
				mStateCallback.onCaptureQueueEmpty(this);
				mStateCallback.onReady(this);
			});
		}
	}

	private void reportClosed() {
		if (!mConfigureSuccess || mClosedReported.getAndSet(true))
			return;
		mStateExecutor.execute(() -> mStateCallback.onClosed(this));
	}

	private void checkNotClosed() {
		if (mClosed) {
			throw new IllegalStateException("Session has been closed; further changes are illegal.");
		}
	}
}
