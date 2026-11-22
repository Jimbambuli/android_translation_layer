package android.view.contentcapture;

import android.content.ComponentName;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * AOSP ContentCaptureManager. ATL has no content capture service: AOSP's SystemServiceRegistry then returns no
 * manager at all (getSystemService(ContentCaptureManager.class) is null) and the constructor requires the service,
 * so no instance is ever created here. The methods answer as AOSP's manager does while content capture is disabled.
 */
public final class ContentCaptureManager {
	public static final int DATA_SHARE_ERROR_UNKNOWN = 1;
	public static final int DATA_SHARE_ERROR_CONCURRENT_REQUEST = 2;
	public static final int DATA_SHARE_ERROR_TIMEOUT_INTERRUPTED = 3;

	private ContentCaptureManager() {}

	/* AOSP: null while content capture is disabled */
	public ComponentName getServiceComponentName() {
		return null;
	}

	/* AOSP: false once the main session is disabled; there is no service to enable it */
	public boolean isContentCaptureEnabled() {
		return false;
	}

	/* AOSP: null while content capture is disabled */
	public Set<ContentCaptureCondition> getContentCaptureConditions() {
		return null;
	}

	/* AOSP: only sets the flags of the main session, which does not exist without the service */
	public void setContentCaptureEnabled(boolean enabled) {}

	/* AOSP: hands the request to the service; there is none */
	public void removeData(DataRemovalRequest request) {
		Objects.requireNonNull(request);
	}

	/* AOSP: hands the request to the service; there is none */
	public void shareData(DataShareRequest request, Executor executor, DataShareWriteAdapter dataShareWriteAdapter) {
		Objects.requireNonNull(request);
		Objects.requireNonNull(dataShareWriteAdapter);
		Objects.requireNonNull(executor);
	}
}
