package android.hardware.camera2;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/* AOSP core/java/android/hardware/camera2/TotalCaptureResult.java, reduced like CaptureResult: ATL has no
 * camera service, so no result is ever produced. */
public final class TotalCaptureResult extends CaptureResult {
	private final List<CaptureResult> mPartialResults;
	private final int mSessionId;
	private final HashMap<String, TotalCaptureResult> mPhysicalCaptureResults = new HashMap<>();

	TotalCaptureResult(CaptureRequest parent, int sequenceId, long frameNumber, List<CaptureResult> partials, int sessionId) {
		super(parent, sequenceId, frameNumber);
		mPartialResults = partials;
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
