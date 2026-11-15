package android.hardware.camera2;

import android.hardware.camera2.utils.HashCodeHelpers;

import java.util.concurrent.Executor;

/* AOSP core/java/android/hardware/camera2/CameraExtensionSession.java (comments and annotations
 * removed). ATL has no camera service, so no extension session is ever created. */
public abstract class CameraExtensionSession implements AutoCloseable {
	 
	public CameraExtensionSession () {}

	public static abstract class ExtensionCaptureCallback {

		public void onCaptureStarted(CameraExtensionSession session,
				CaptureRequest request, long timestamp) {
		}

		public void onCaptureProcessStarted(CameraExtensionSession session,
				CaptureRequest request) {
		}

		public void onCaptureFailed(CameraExtensionSession session,
				CaptureRequest request) {
		}

		public void onCaptureFailed(CameraExtensionSession session,
				CaptureRequest request, int failure) {
		}

		public void onCaptureSequenceCompleted(CameraExtensionSession session,
				int sequenceId) {
		}

		public void onCaptureSequenceAborted(CameraExtensionSession session,
				int sequenceId) {
		}

		public void onCaptureResultAvailable(CameraExtensionSession session,
				CaptureRequest request, TotalCaptureResult result) {
		}

		public void onCaptureProcessProgressed(CameraExtensionSession session,
				CaptureRequest request, int progress) {
		}
	}

	public static abstract class StateCallback {

		public abstract void onConfigured(CameraExtensionSession session);

		public abstract void onConfigureFailed(CameraExtensionSession session);

		public void onClosed(CameraExtensionSession session) {
		}
	}

	public android.hardware.camera2.CameraDevice getDevice() {
		throw new UnsupportedOperationException("Subclasses must override this method");
	}

	public int capture(CaptureRequest request,
			Executor executor,
			ExtensionCaptureCallback listener) throws CameraAccessException {
		throw new UnsupportedOperationException("Subclasses must override this method");
	}

	public int setRepeatingRequest(CaptureRequest request,
			Executor executor,
			ExtensionCaptureCallback listener) throws CameraAccessException {
		throw new UnsupportedOperationException("Subclasses must override this method");
	}

	public void stopRepeating() throws CameraAccessException {
		throw new UnsupportedOperationException("Subclasses must override this method");
	}

	public final static class StillCaptureLatency {
		private final long mCaptureLatency, mProcessingLatency;

		public StillCaptureLatency(long captureLatency, long processingLatency) {
			mCaptureLatency = captureLatency;
			mProcessingLatency = processingLatency;
		}
		
		public long getCaptureLatency() {
			return mCaptureLatency;
		}

		public long getProcessingLatency() {
			return mProcessingLatency;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			StillCaptureLatency latency = (StillCaptureLatency) o;

			if (mCaptureLatency != latency.mCaptureLatency) return false;
			if (mProcessingLatency != latency.mProcessingLatency) return false;

			return true;
		}

		@Override
		public int hashCode() {
			return HashCodeHelpers.hashCode(mCaptureLatency, mProcessingLatency);
		}

		@Override
		public String toString() {
			return "StillCaptureLatency(processingLatency:" + mProcessingLatency +
					", captureLatency: " + mCaptureLatency + ")";
		}
	}

	public StillCaptureLatency getRealtimeStillCaptureLatency() throws CameraAccessException {
		throw new UnsupportedOperationException("Subclasses must override this method");
	}

	public void close() throws CameraAccessException {
		throw new UnsupportedOperationException("Subclasses must override this method");
	}
}
