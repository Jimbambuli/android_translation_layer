package android.drm;

import android.content.Context;
import android.net.Uri;

import java.util.concurrent.atomic.AtomicBoolean;

/* API 11. Client of the (deprecated) DRM framework. ATL has no drmserver and no DRM engine plugins,
 * so this is the client of a device whose DrmManager has no plugins loaded: getAvailableDrmEngines()
 * is empty and canHandle() is false (DrmManager::canHandle finds no plugin). close()/release() follow
 * AOSP drm/java/android/drm/DrmManagerClient.java (idempotent through mClosed). The argument checks of
 * canHandle() are AOSP's. Most apps only reference release() (63 of 64 in the r9 corpus). */
public class DrmManagerClient implements AutoCloseable {
	public static final int ERROR_NONE = 0;
	public static final int ERROR_UNKNOWN = -2000;

	public static final int INVALID_SESSION = -1;

	private final AtomicBoolean mClosed = new AtomicBoolean();
	private Context mContext;

	public DrmManagerClient(Context context) {
		mContext = context;
	}

	@Override
	public void close() {
		mClosed.compareAndSet(false, true);
	}

	@Deprecated
	public void release() {
		close();
	}

	public String[] getAvailableDrmEngines() {
		return new String[0];
	}

	public boolean canHandle(String path, String mimeType) {
		if ((null == path || path.equals("")) && (null == mimeType || mimeType.equals(""))) {
			throw new IllegalArgumentException("Path or the mimetype should be non null");
		}
		return false;
	}

	public boolean canHandle(Uri uri, String mimeType) {
		if ((null == uri || Uri.EMPTY == uri) && (null == mimeType || mimeType.equals(""))) {
			throw new IllegalArgumentException("Uri or the mimetype should be non null");
		}
		return canHandle(convertUriToPath(uri), mimeType);
	}

	/* AOSP convertUriToPath(), except that a content: Uri is not resolved through MediaStore: with no
	 * engine the path cannot change the answer of canHandle(). */
	private String convertUriToPath(Uri uri) {
		String path = null;
		if (null != uri) {
			String scheme = uri.getScheme();
			if (null == scheme || scheme.equals("") || scheme.equals("file")) {
				path = uri.getPath();
			} else if (scheme.equals("http") || scheme.equals("https") || scheme.equals("content")) {
				path = uri.toString();
			} else {
				throw new IllegalArgumentException("Given Uri scheme is not supported");
			}
		}
		return path;
	}
}
