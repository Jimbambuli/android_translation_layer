package android.hardware.camera2;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/* AOSP core/java/android/hardware/camera2/TotalCaptureResult.java, reduced like CaptureResult. */
public final class TotalCaptureResult extends CaptureResult {
	private final List<CaptureResult> mPartialResults;
	private final int mSessionId;
	private final HashMap<String, TotalCaptureResult> mPhysicalCaptureResults = new HashMap<>();

	/** @hide AOSP TotalCaptureResult(logicalCameraId, CameraMetadataNative results, parent, extras, partials,
	 * sessionId, physicalResults); the extras are the sequence id and the frame number */
	public TotalCaptureResult(String logicalCameraId, Map<CaptureResult.Key<?>, Object> results, CaptureRequest parent, int sequenceId, long frameNumber, List<CaptureResult> partials, int sessionId) {
		super(logicalCameraId, results, parent, sequenceId, frameNumber);
		mPartialResults = partials != null ? partials : new java.util.ArrayList<CaptureResult>();
		mSessionId = sessionId;
	}

	public List<CaptureResult> getPartialResults() {
		return Collections.unmodifiableList(mPartialResults);
	}

	public int getSessionId() {
		return mSessionId;
	}

	@Deprecated
	public Map<String, CaptureResult> getPhysicalCameraResults() {
		return Collections.unmodifiableMap(new HashMap<String, CaptureResult>(mPhysicalCaptureResults));
	}

	public Map<String, TotalCaptureResult> getPhysicalCameraTotalResults() {
		return Collections.unmodifiableMap(mPhysicalCaptureResults);
	}
}
