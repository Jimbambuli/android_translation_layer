/* AOSP frameworks/base/media/java/android/media/MediaCasStateException.java
 * ATL has no CAS plugin service, as on a device without one: no CA system is supported and every
 * constructor throws UnsupportedCasException ("Unsupported casSystemId N"). */
package android.media;

public class MediaCasStateException extends IllegalStateException {
	private final int mErrorCode;
	private final String mDiagnosticInfo;

	MediaCasStateException(int err, String msg, String diagnosticInfo) {
		super(msg);
		mErrorCode = err;
		mDiagnosticInfo = diagnosticInfo;
	}

	public String getDiagnosticInfo() {
		return mDiagnosticInfo;
	}
}
