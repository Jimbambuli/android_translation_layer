package android.hardware.camera2.impl;

import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.InputConfiguration;
import android.hardware.camera2.params.OutputConfiguration;
import android.hardware.camera2.params.SessionConfiguration;
import android.os.Handler;
import android.os.Looper;
import android.util.Range;
import android.util.Size;
import android.view.Surface;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;

/** @hide AOSP core/java/android/hardware/camera2/impl/CameraDeviceImpl.java over ATL's camera HAL
 * (ExternalCameraDevice) in place of the camera service's ICameraDeviceUser: the same checks, sessions and
 * callbacks, with the HAL's frames driving the requests (one request per frame, single captures before the
 * repeating request, as AOSP's Camera3Device request thread does).
 *
 * The HAL gives one stream: its size is the nearest supported size to the largest SurfaceTexture output of the
 * session (AOSP's camera service rounds the size of a preview Surface to the nearest supported one), else the
 * largest supported preview size up to 1920x1080 (an ATL SurfaceView keeps no buffer size). Frames go to the
 * SurfaceView/SurfaceTexture targets of the request; an ImageReader target gets the frame as an image of its size and
 * format (YUV_420_888, JPEG, PRIVATE), as the external HAL scales and encodes its outputs from the frame.
 *
 * Locking: the HAL's stream thread holds the native loop's lock while it calls onFrame(), which takes
 * mInterfaceLock; so the HAL is only called outside mInterfaceLock, serialized by mHalLock. */
public class CameraDeviceImpl extends CameraDevice implements ExternalCameraDevice.FrameListener {
	/* AOSP CameraDeviceImpl.REQUEST_ID_NONE */
	private static final int REQUEST_ID_NONE = -1;

	private final Object mInterfaceLock = new Object();
	private final Object mHalLock = new Object();
	private final String mCameraId;
	private final StateCallback mDeviceCallback;
	private final Executor mDeviceExecutor;
	private final CameraCharacteristics mCharacteristics;
	private final ExternalCameraDevice mHal;

	private boolean mClosing;
	private boolean mInError;
	private boolean mStreaming;
	private int mNextSessionId;
	private CameraCaptureSessionImpl mCurrentSession;
	private List<Surface> mConfiguredOutputs = new ArrayList<Surface>();

	/* the requests: AOSP's request ids are the sequence ids */
	private static final class RequestHolder {
		final List<CaptureRequest> requests;
		final CameraCaptureSession.CaptureCallback callback;
		final Executor executor;
		final int sequenceId;
		final boolean repeating;
		int next;
		long lastFrameNumber = CameraCaptureSession.CaptureCallback.NO_FRAMES_CAPTURED;

		RequestHolder(List<CaptureRequest> requests, CameraCaptureSession.CaptureCallback callback, Executor executor, int sequenceId, boolean repeating) {
			this.requests = requests;
			this.callback = callback;
			this.executor = executor;
			this.sequenceId = sequenceId;
			this.repeating = repeating;
		}
	}

	private int mRequestId;
	private long mFrameNumber;
	private RequestHolder mRepeating;
	private final ArrayDeque<RequestHolder> mPending = new ArrayDeque<RequestHolder>();
	private boolean mIdle = true;

	public CameraDeviceImpl(String cameraId, StateCallback callback, Executor executor, CameraCharacteristics characteristics, ExternalCameraDevice hal) {
		if (cameraId == null || callback == null || executor == null || characteristics == null) {
			throw new IllegalArgumentException("Null argument given");
		}
		mCameraId = cameraId;
		mDeviceCallback = callback;
		mDeviceExecutor = executor;
		mCharacteristics = characteristics;
		mHal = hal;
	}

	/* AOSP setRemoteDevice(): connected, onOpened; setRemoteFailure(): onError */
	public void connect() {
		if (mHal.open(this)) {
			mDeviceExecutor.execute(() -> mDeviceCallback.onOpened(this));
		} else {
			synchronized (mInterfaceLock) {
				mInError = true;
			}
			mDeviceExecutor.execute(() -> mDeviceCallback.onError(this, StateCallback.ERROR_CAMERA_DEVICE));
		}
	}

	@Override
	public String getId() {
		return mCameraId;
	}

	CameraCharacteristics getCharacteristics() {
		return mCharacteristics;
	}

	/* AOSP checkIfCameraClosedOrInError() */
	private void checkIfCameraClosedOrInError() throws CameraAccessException {
		if (mClosing || !mHal.isOpenBy(this)) {
			throw new IllegalStateException("CameraDevice was already closed");
		}
		if (mInError) {
			throw new CameraAccessException(CameraAccessException.CAMERA_ERROR, "The camera device has encountered a serious error");
		}
	}

	/* AOSP CameraDeviceImpl.checkHandler()/checkAndWrapHandler() */
	static Handler checkHandler(Handler handler) {
		if (handler == null) {
			Looper looper = Looper.myLooper();
			if (looper == null) {
				throw new IllegalArgumentException("No handler given, and current thread has no looper!");
			}
			handler = new Handler(looper);
		}
		return handler;
	}

	static <T> Handler checkHandler(Handler handler, T callback) {
		if (callback != null) {
			return checkHandler(handler);
		}
		return handler;
	}

	static Executor checkAndWrapHandler(Handler handler) {
		Handler h = checkHandler(handler);
		return h::post;
	}

	static Executor checkExecutor(Executor executor) {
		return (executor == null) ? checkAndWrapHandler(null) : executor;
	}

	static <T> Executor checkExecutor(Executor executor, T callback) {
		return (callback != null) ? checkExecutor(executor) : executor;
	}

	/* --- sessions --- */

	@Override
	@Deprecated
	public void createCaptureSession(List<Surface> outputs, CameraCaptureSession.StateCallback callback, Handler handler) throws CameraAccessException {
		List<OutputConfiguration> outConfigurations = new ArrayList<>(outputs.size());
		for (Surface surface : outputs) {
			outConfigurations.add(new OutputConfiguration(surface));
		}
		createCaptureSessionInternal(null, outConfigurations, callback, checkAndWrapHandler(handler), SessionConfiguration.SESSION_REGULAR);
	}

	@Override
	@Deprecated
	public void createCaptureSessionByOutputConfigurations(List<OutputConfiguration> outputConfigurations, CameraCaptureSession.StateCallback callback, Handler handler) throws CameraAccessException {
		List<OutputConfiguration> currentOutputs = new ArrayList<>(outputConfigurations);
		createCaptureSessionInternal(null, currentOutputs, callback, checkAndWrapHandler(handler), SessionConfiguration.SESSION_REGULAR);
	}

	@Override
	@Deprecated
	public void createReprocessableCaptureSession(InputConfiguration inputConfig, List<Surface> outputs, CameraCaptureSession.StateCallback callback, Handler handler) throws CameraAccessException {
		if (inputConfig == null) {
			throw new IllegalArgumentException("inputConfig cannot be null when creating a reprocessable capture session");
		}
		List<OutputConfiguration> outConfigurations = new ArrayList<>(outputs.size());
		for (Surface surface : outputs) {
			outConfigurations.add(new OutputConfiguration(surface));
		}
		createCaptureSessionInternal(inputConfig, outConfigurations, callback, checkAndWrapHandler(handler), SessionConfiguration.SESSION_REGULAR);
	}

	@Override
	@Deprecated
	public void createReprocessableCaptureSessionByConfigurations(InputConfiguration inputConfig, List<OutputConfiguration> outputs, CameraCaptureSession.StateCallback callback, Handler handler) throws CameraAccessException {
		if (inputConfig == null) {
			throw new IllegalArgumentException("inputConfig cannot be null when creating a reprocessable capture session");
		}
		if (outputs == null) {
			throw new IllegalArgumentException("Output configurations cannot be null when creating a reprocessable capture session");
		}
		createCaptureSessionInternal(inputConfig, new ArrayList<OutputConfiguration>(outputs), callback, checkAndWrapHandler(handler), SessionConfiguration.SESSION_REGULAR);
	}

	@Override
	@Deprecated
	public void createConstrainedHighSpeedCaptureSession(List<Surface> outputs, CameraCaptureSession.StateCallback callback, Handler handler) throws CameraAccessException {
		if (outputs == null || outputs.size() == 0 || outputs.size() > 2) {
			throw new IllegalArgumentException("Output surface list must not be null and the size must be no more than 2");
		}
		List<OutputConfiguration> outConfigurations = new ArrayList<>(outputs.size());
		for (Surface surface : outputs) {
			outConfigurations.add(new OutputConfiguration(surface));
		}
		createCaptureSessionInternal(null, outConfigurations, callback, checkAndWrapHandler(handler), SessionConfiguration.SESSION_HIGH_SPEED);
	}

	@Override
	public void createCaptureSession(SessionConfiguration config) throws CameraAccessException {
		if (config == null) {
			throw new IllegalArgumentException("Invalid session configuration");
		}
		List<OutputConfiguration> outputConfigs = config.getOutputConfigurations();
		if (outputConfigs == null) {
			throw new IllegalArgumentException("Invalid output configurations");
		}
		if (config.getExecutor() == null) {
			throw new IllegalArgumentException("Invalid executor");
		}
		createCaptureSessionInternal(config.getInputConfiguration(), outputConfigs, config.getStateCallback(), config.getExecutor(), config.getSessionType());
	}

	/* AOSP createCaptureSessionInternal() -> configureStreamsChecked(): the HAL has no input streams (no reprocessing
	 * capability) and no high speed configurations, so both fail with the service's IllegalArgumentException; the
	 * outputs are the session's Surfaces */
	private void createCaptureSessionInternal(InputConfiguration inputConfig, List<OutputConfiguration> outputConfigurations, CameraCaptureSession.StateCallback callback, Executor executor, int operatingMode) throws CameraAccessException {
		synchronized (mHalLock) {
			try {
				createCaptureSessionLocked(inputConfig, outputConfigurations, callback, executor, operatingMode);
			} finally {
				stopStreamIfStopped();
			}
		}
	}

	private void createCaptureSessionLocked(InputConfiguration inputConfig, List<OutputConfiguration> outputConfigurations, CameraCaptureSession.StateCallback callback, Executor executor, int operatingMode) throws CameraAccessException {
		synchronized (mInterfaceLock) {
			checkIfCameraClosedOrInError();
			if (operatingMode == SessionConfiguration.SESSION_HIGH_SPEED && inputConfig != null) {
				throw new IllegalArgumentException("Constrained high speed session doesn't support input configuration yet.");
			}
			if (mCurrentSession != null) {
				mCurrentSession.replaceSessionClose();
			}
			clearRequestsLocked();
			if (inputConfig != null) {
				throw new IllegalArgumentException("Input format " + inputConfig.getFormat() + " is not valid for this camera device");
			}
			if (operatingMode == SessionConfiguration.SESSION_HIGH_SPEED) {
				throw new IllegalArgumentException("Camera device " + mCameraId + " does not support high speed video recording");
			}
			List<Surface> outputs = new ArrayList<Surface>();
			for (OutputConfiguration config : outputConfigurations) {
				for (Surface surface : config.getSurfaces()) {
					if (surface != null)
						outputs.add(surface);
				}
			}
			boolean configureSuccess = !outputs.isEmpty();
			mConfiguredOutputs = configureSuccess ? outputs : new ArrayList<Surface>();
			mCurrentSession = new CameraCaptureSessionImpl(mNextSessionId++, callback, executor, this, mDeviceExecutor, configureSuccess);
		}
	}

	@Override
	public boolean isSessionConfigurationSupported(SessionConfiguration sessionConfig) throws CameraAccessException {
		synchronized (mInterfaceLock) {
			checkIfCameraClosedOrInError();
			return sessionConfig.getInputConfiguration() == null && sessionConfig.getSessionType() == SessionConfiguration.SESSION_REGULAR && !sessionConfig.getOutputConfigurations().isEmpty();
		}
	}

	/* --- requests --- */

	@Override
	public CaptureRequest.Builder createCaptureRequest(int templateType, Set<String> physicalCameraIdSet) throws CameraAccessException {
		synchronized (mInterfaceLock) {
			checkIfCameraClosedOrInError();
			for (String physicalId : physicalCameraIdSet) {
				if (Objects.equals(physicalId, getId())) {
					throw new IllegalStateException("Physical id matches the logical id!");
				}
			}
			return new CaptureRequest.Builder(defaultRequest(templateType), false, CameraCaptureSession.SESSION_ID_NONE, getId(), physicalCameraIdSet);
		}
	}

	@Override
	public CaptureRequest.Builder createCaptureRequest(int templateType) throws CameraAccessException {
		synchronized (mInterfaceLock) {
			checkIfCameraClosedOrInError();
			return new CaptureRequest.Builder(defaultRequest(templateType), false, CameraCaptureSession.SESSION_ID_NONE, getId(), null);
		}
	}

	/* the HAL's ILLEGAL_ARGUMENT for MANUAL/ZERO_SHUTTER_LAG becomes the service's IllegalArgumentException */
	private java.util.Map<CaptureRequest.Key<?>, Object> defaultRequest(int templateType) {
		java.util.Map<CaptureRequest.Key<?>, Object> template = mHal.getDefaultRequest(templateType);
		if (template == null) {
			throw new IllegalArgumentException("Template " + templateType + " is not supported by camera " + mCameraId);
		}
		return template;
	}

	/* AOSP: the input stream of a reprocessable session; there is none */
	@Override
	public CaptureRequest.Builder createReprocessCaptureRequest(TotalCaptureResult inputResult) throws CameraAccessException {
		synchronized (mInterfaceLock) {
			checkIfCameraClosedOrInError();
			throw new IllegalArgumentException("Camera " + mCameraId + " has no reprocessable session");
		}
	}

	private int mAudioRestriction = AUDIO_RESTRICTION_NONE;

	@Override
	public void setCameraAudioRestriction(int mode) throws CameraAccessException {
		synchronized (mInterfaceLock) {
			checkIfCameraClosedOrInError();
			mAudioRestriction = mode;
		}
	}

	@Override
	public int getCameraAudioRestriction() throws CameraAccessException {
		synchronized (mInterfaceLock) {
			checkIfCameraClosedOrInError();
			return mAudioRestriction;
		}
	}

	/* AOSP submitCaptureRequest(): the targets must be Surfaces of the session */
	int submitCaptureRequest(List<CaptureRequest> requestList, CameraCaptureSession.CaptureCallback callback, Executor executor, boolean repeating) throws CameraAccessException {
		executor = checkExecutor(executor, callback);
		int sequenceId;
		Set<Surface> targets;
		Size streamSize = null;
		int maxFps = 30;
		synchronized (mHalLock) {
			synchronized (mInterfaceLock) {
				sequenceId = submitLocked(requestList, callback, executor, repeating);
				targets = targetsLocked();
				if (!mStreaming) {
					streamSize = streamSizeLocked();
					Range<Integer> fpsRange = requestList.get(0).get(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE);
					if (fpsRange != null)
						maxFps = fpsRange.getUpper();
				}
			}
			mHal.setTargets(this, targets);
			if (streamSize != null) {
				if (!mHal.startStream(this, streamSize.getWidth(), streamSize.getHeight(), maxFps)) {
					throw new CameraAccessException(CameraAccessException.CAMERA_ERROR, "The camera device cannot stream " + streamSize);
				}
				synchronized (mInterfaceLock) {
					mStreaming = true;
				}
			}
		}
		return sequenceId;
	}

	private int submitLocked(List<CaptureRequest> requestList, CameraCaptureSession.CaptureCallback callback, Executor executor, boolean repeating) throws CameraAccessException {
		checkIfCameraClosedOrInError();
		for (CaptureRequest request : requestList) {
			/* the service: settings for physical cameras need a logical multi-camera, which the HAL is not */
			if (request.getPhysicalCameraSettings().size() > 1) {
				throw new IllegalArgumentException("Camera " + mCameraId + " has no physical cameras");
			}
			if (request.getTargets().isEmpty()) {
				throw new IllegalArgumentException("Each request must have at least one Surface target");
			}
			for (Surface surface : request.getTargets()) {
				if (surface == null) {
					throw new IllegalArgumentException("Null Surface targets are not allowed");
				}
				if (!mConfiguredOutputs.contains(surface)) {
					throw new IllegalArgumentException("Request targets Surface that is not part of current capture session");
				}
			}
		}
		if (repeating) {
			stopRepeatingLocked();
		}
		RequestHolder holder = new RequestHolder(new ArrayList<CaptureRequest>(requestList), callback, executor, mRequestId++, repeating);
		if (repeating)
			mRepeating = holder;
		else
			mPending.add(holder);
		if (mIdle) {
			mIdle = false;
			if (mCurrentSession != null)
				mCurrentSession.onActive();
		}
		return holder.sequenceId;
	}

	int capture(CaptureRequest request, CameraCaptureSession.CaptureCallback callback, Executor executor) throws CameraAccessException {
		return submitCaptureRequest(Collections.singletonList(request), callback, executor, false);
	}

	int captureBurst(List<CaptureRequest> requests, CameraCaptureSession.CaptureCallback callback, Executor executor) throws CameraAccessException {
		if (requests == null || requests.isEmpty()) {
			throw new IllegalArgumentException("At least one request must be given");
		}
		return submitCaptureRequest(requests, callback, executor, false);
	}

	int setRepeatingRequest(CaptureRequest request, CameraCaptureSession.CaptureCallback callback, Executor executor) throws CameraAccessException {
		return submitCaptureRequest(Collections.singletonList(request), callback, executor, true);
	}

	int setRepeatingBurst(List<CaptureRequest> requests, CameraCaptureSession.CaptureCallback callback, Executor executor) throws CameraAccessException {
		if (requests == null || requests.isEmpty()) {
			throw new IllegalArgumentException("At least one request must be given");
		}
		return submitCaptureRequest(requests, callback, executor, true);
	}

	/* AOSP stopRepeating(): the repeating request's sequence completes with its last frame */
	void stopRepeating() throws CameraAccessException {
		Set<Surface> targets;
		synchronized (mHalLock) {
			synchronized (mInterfaceLock) {
				checkIfCameraClosedOrInError();
				stopRepeatingLocked();
				checkIdleLocked();
				targets = targetsLocked();
			}
			mHal.setTargets(this, targets);
		}
	}

	private void stopRepeatingLocked() {
		RequestHolder holder = mRepeating;
		mRepeating = null;
		if (holder != null && holder.callback != null) {
			CameraCaptureSessionImpl session = mCurrentSession;
			if (holder.lastFrameNumber == CameraCaptureSession.CaptureCallback.NO_FRAMES_CAPTURED)
				holder.executor.execute(() -> holder.callback.onCaptureSequenceAborted(session, holder.sequenceId));
			else
				holder.executor.execute(() -> holder.callback.onCaptureSequenceCompleted(session, holder.sequenceId, holder.lastFrameNumber));
		}
	}

	/* AOSP switchToOffline(): the checks up to the offline support of each stream, which the HAL has for none */
	android.hardware.camera2.CameraOfflineSession switchToOffline(java.util.Collection<Surface> offlineOutputs) throws CameraAccessException {
		if (offlineOutputs.isEmpty()) {
			throw new IllegalArgumentException("Invalid offline surfaces!");
		}
		synchronized (mInterfaceLock) {
			checkIfCameraClosedOrInError();
			for (Surface surface : offlineOutputs) {
				if (!mConfiguredOutputs.contains(surface)) {
					throw new IllegalArgumentException("Offline surface is not part of this session");
				}
				throw new IllegalArgumentException("Surface: " + surface + " does not  support offline mode");
			}
		}
		throw new IllegalArgumentException("Invalid offline surfaces!");
	}

	/* AOSP flush(): the pending captures fail as flushed, the repeating request ends */
	void flush() throws CameraAccessException {
		synchronized (mHalLock) {
			synchronized (mInterfaceLock) {
				flushLocked();
			}
			mHal.setTargets(this, Collections.<Surface>emptyList());
		}
	}

	private void flushLocked() throws CameraAccessException {
		checkIfCameraClosedOrInError();
		CameraCaptureSessionImpl session = mCurrentSession;
		for (RequestHolder holder : mPending) {
			if (holder.callback == null)
				continue;
			for (int i = holder.next; i < holder.requests.size(); i++) {
				CaptureRequest request = holder.requests.get(i);
				CaptureFailure failure = new CaptureFailure(request, CaptureFailure.REASON_FLUSHED, false, holder.sequenceId, mFrameNumber++, null);
				holder.executor.execute(() -> holder.callback.onCaptureFailed(session, request, failure));
			}
			holder.executor.execute(() -> holder.callback.onCaptureSequenceAborted(session, holder.sequenceId));
		}
		mPending.clear();
		stopRepeatingLocked();
		checkIdleLocked();
	}

	private void checkIdleLocked() {
		if (!mIdle && mRepeating == null && mPending.isEmpty()) {
			mIdle = true;
			if (mCurrentSession != null)
				mCurrentSession.onIdle();
		}
	}

	boolean isIdle() {
		synchronized (mInterfaceLock) {
			return mIdle;
		}
	}

	/* the frames go to the display Surfaces of the requests in flight */
	private Set<Surface> targetsLocked() {
		Set<Surface> targets = new LinkedHashSet<Surface>();
		if (mRepeating != null)
			for (CaptureRequest request : mRepeating.requests)
				targets.addAll(request.getTargets());
		for (RequestHolder holder : mPending)
			for (CaptureRequest request : holder.requests)
				targets.addAll(request.getTargets());
		return targets;
	}

	/* the stream size, see the class comment */
	private Size streamSizeLocked() {
		Size[] sizes = mCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(SurfaceTexture.class);
		Size wanted = null;
		for (Surface surface : mConfiguredOutputs) {
			Size size = null;
			if (surface.surfaceTexture != null)
				size = new Size(surface.surfaceTexture.getDefaultBufferWidth(), surface.surfaceTexture.getDefaultBufferHeight());
			else if (surface.atl_imageReader != null)
				size = new Size(surface.atl_imageReader.getWidth(), surface.atl_imageReader.getHeight());
			if (size != null && (wanted == null || size.getWidth() * size.getHeight() > wanted.getWidth() * wanted.getHeight()))
				wanted = size;
		}
		Size best = null;
		for (Size size : sizes) {
			if (wanted != null) {
				if (best == null || distance(size, wanted) < distance(best, wanted))
					best = size;
			} else if (size.getWidth() <= 1920 && size.getHeight() <= 1080 && (best == null || size.getWidth() * size.getHeight() > best.getWidth() * best.getHeight())) {
				best = size;
			}
		}
		return best != null ? best : sizes[0];
	}

	private static long distance(Size a, Size b) {
		long dw = a.getWidth() - b.getWidth(), dh = a.getHeight() - b.getHeight();
		return dw * dw + dh * dh;
	}

	/* a new session or a closed device: the requests end without callbacks (AOSP: the old session is replaced) */
	private void clearRequestsLocked() {
		mRepeating = null;
		mPending.clear();
		mIdle = true;
		mStreaming = false;
	}

	/* with mHalLock: the stream ends when the requests were cleared */
	private void stopStreamIfStopped() {
		boolean stop;
		synchronized (mInterfaceLock) {
			stop = !mStreaming;
		}
		if (stop) {
			mHal.setTargets(this, Collections.<Surface>emptyList());
			mHal.stopStream(this);
		}
	}

	/* ExternalCameraDevice.FrameListener, on the stream's thread: the next request takes the frame */
	@Override
	public void onFrame(long timestampNs) {
		RequestHolder holder;
		CaptureRequest request;
		long frameNumber;
		boolean last = false;
		CameraCaptureSessionImpl session;
		synchronized (mInterfaceLock) {
			holder = mPending.peek();
			if (holder != null) {
				request = holder.requests.get(holder.next++);
				if (holder.next == holder.requests.size()) {
					mPending.poll();
					last = true;
				}
			} else if (mRepeating != null) {
				holder = mRepeating;
				request = holder.requests.get(holder.next++ % holder.requests.size());
			} else {
				return;
			}
			frameNumber = mFrameNumber++;
			holder.lastFrameNumber = frameNumber;
			session = mCurrentSession;
		}
		for (Surface target : request.getTargets()) {
			if (target.atl_imageReader != null && !queueImage(target.atl_imageReader, request, timestampNs) && holder.callback != null) {
				final RequestHolder h = holder;
				h.executor.execute(() -> h.callback.onCaptureBufferLost(session, request, target, frameNumber));
			}
		}
		if (holder.callback != null) {
			TotalCaptureResult result = new TotalCaptureResult(mCameraId, mHal.fillCaptureResult(request, timestampNs), request, holder.sequenceId, frameNumber, new ArrayList<>(), session != null ? session.getSessionId() : CameraCaptureSession.SESSION_ID_NONE);
			final RequestHolder h = holder;
			final boolean sequenceDone = last;
			h.executor.execute(() -> {
				h.callback.onCaptureStarted(session, request, timestampNs, frameNumber);
				h.callback.onCaptureCompleted(session, request, result);
				if (sequenceDone)
					h.callback.onCaptureSequenceCompleted(session, h.sequenceId, frameNumber);
			});
		}
		if (last) {
			Set<Surface> targets;
			synchronized (mInterfaceLock) {
				checkIdleLocked();
				targets = targetsLocked();
			}
			mHal.setTargets(this, targets); /* on the stream's thread, which holds the loop's lock already */
		}
	}

	/* the frame for an ImageReader output, on the stream's thread: false if the reader takes no image now */
	private boolean queueImage(android.media.ImageReader reader, CaptureRequest request, long timestampNs) {
		int w = reader.getWidth(), h = reader.getHeight();
		int format = reader.getImageFormat();
		/* an encoder's input surface (MediaRecorder.getSurface(), MediaCodec.createInputSurface()) takes the camera's
		 * YUV frames (AOSP: the camera's buffers with video encoder usage) */
		if (format == android.graphics.ImageFormat.PRIVATE && (reader.getUsage() & android.hardware.HardwareBuffer.USAGE_VIDEO_ENCODE) != 0)
			format = android.graphics.ImageFormat.YUV_420_888;
		switch (format) {
			case android.graphics.ImageFormat.YUV_420_888:
				return mHal.queueYuvImage(reader, timestampNs);
			case android.graphics.ImageFormat.JPEG: {
				Byte quality = request.get(CaptureRequest.JPEG_QUALITY);
				byte[] jpeg = mHal.encodeJpeg(w, h, quality != null ? quality & 0xff : 90);
				if (jpeg == null)
					return false;
				java.nio.ByteBuffer buffer = java.nio.ByteBuffer.allocateDirect(jpeg.length).put(jpeg);
				buffer.rewind();
				return reader.queueImage(timestampNs, new java.nio.ByteBuffer[] {buffer}, new int[] {0}, new int[] {0});
			}
			case android.graphics.ImageFormat.PRIVATE:
				return reader.queueImage(timestampNs, new java.nio.ByteBuffer[0], new int[0], new int[0]);
			default:
				return false;
		}
	}

	/* the stream failed or the camera is gone: AOSP onDeviceError(ERROR_CAMERA_DISCONNECTED) -> onDisconnected */
	@Override
	public void onStreamError(String message) {
		synchronized (mInterfaceLock) {
			if (mClosing)
				return;
			mInError = true;
		}
		mDeviceExecutor.execute(() -> mDeviceCallback.onDisconnected(this));
	}

	/* another client opened the camera: AOSP's camera service evicts this one (onDisconnected) */
	@Override
	public void onEvicted() {
		onStreamError("evicted");
	}

	/* AOSP close(): the session ends, then onClosed */
	@Override
	public void close() {
		synchronized (mHalLock) {
			synchronized (mInterfaceLock) {
				if (mClosing)
					return;
				mClosing = true;
				clearRequestsLocked();
				if (mCurrentSession != null) {
					mCurrentSession.replaceSessionClose();
					mCurrentSession = null;
				}
			}
			mHal.close(this);
		}
		mDeviceExecutor.execute(() -> mDeviceCallback.onClosed(this));
	}
}
