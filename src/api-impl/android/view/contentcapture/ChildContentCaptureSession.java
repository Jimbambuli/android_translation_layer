package android.view.contentcapture;

/* AOSP ChildContentCaptureSession: a session created by the app with
 * ContentCaptureSession.createContentCaptureSession(); like its parent it is disabled in ATL */
final class ChildContentCaptureSession extends ContentCaptureSession {
	ChildContentCaptureSession(ContentCaptureContext clientContext) {
		super(clientContext);
	}
}
