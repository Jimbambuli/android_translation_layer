package android.drm;

import android.annotation.NonNull;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/* API 11. Client of the (deprecated) DRM framework, after AOSP drm/java/android/drm/DrmManagerClient.java.
 * ATL has no drmserver and no DRM engine plugins, so the private native methods (_xxx) are replaced by the
 * answers of a DrmManager that has no plugin loaded (DrmManager.cpp, the "no plugin supports this" branch of
 * each call). The native sources are not part of aosp-src, so these results are from the C++ default values,
 * not re-read: canHandle false, getDrmObjectType UNKNOWN, checkRightsStatus RIGHTS_INVALID, getOriginalMimeType "",
 * openConvertSession INVALID_SESSION, removeRights/saveRights/removeAllRights ERROR_UNKNOWN, null for
 * constraints, metadata, DrmInfo, converted status and process status, no support info. The Java part
 * (argument checks, the event/info handler threads, event types) is AOSP's. CloseGuard is left out (it only
 * logs a leaked client). A content: Uri is not resolved through MediaStore (ATL has no MediaColumns.DATA):
 * with no engine the path cannot change any answer. Most apps only reference release(). */
@Deprecated
public class DrmManagerClient implements AutoCloseable {
	/** Indicates that a request was successful or that no error occurred. */
	public static final int ERROR_NONE = 0;
	/** Indicates that an error occurred and the reason is not known. */
	public static final int ERROR_UNKNOWN = -2000;

	/** @hide */
	public static final int INVALID_SESSION = -1;

	HandlerThread mInfoThread;
	HandlerThread mEventThread;
	private static final String TAG = "DrmManagerClient";

	private final AtomicBoolean mClosed = new AtomicBoolean();

	/** Interface definition for a callback that receives status messages and warnings during registration and
	 * rights acquisition. */
	public interface OnInfoListener {
		public void onInfo(DrmManagerClient client, DrmInfoEvent event);
	}

	/** Interface definition for a callback that receives information about DRM processing events. */
	public interface OnEventListener {
		public void onEvent(DrmManagerClient client, DrmEvent event);
	}

	/** Interface definition for a callback that receives information about DRM framework errors. */
	public interface OnErrorListener {
		public void onError(DrmManagerClient client, DrmErrorEvent event);
	}

	private static final int ACTION_REMOVE_ALL_RIGHTS = 1001;
	private static final int ACTION_PROCESS_DRM_INFO = 1002;

	private int mUniqueId;
	private Context mContext;
	private InfoHandler mInfoHandler;
	private EventHandler mEventHandler;
	private OnInfoListener mOnInfoListener;
	private OnEventListener mOnEventListener;
	private OnErrorListener mOnErrorListener;

	private class EventHandler extends Handler {

		public EventHandler(Looper looper) {
			super(looper);
		}

		public void handleMessage(Message msg) {
			DrmEvent event = null;
			DrmErrorEvent error = null;
			HashMap<String, Object> attributes = new HashMap<String, Object>();

			switch (msg.what) {
				case ACTION_PROCESS_DRM_INFO: {
					final DrmInfo drmInfo = (DrmInfo) msg.obj;
					DrmInfoStatus status = _processDrmInfo(mUniqueId, drmInfo);

					attributes.put(DrmEvent.DRM_INFO_STATUS_OBJECT, status);
					attributes.put(DrmEvent.DRM_INFO_OBJECT, drmInfo);

					if (null != status && DrmInfoStatus.STATUS_OK == status.statusCode) {
						event = new DrmEvent(mUniqueId, getEventType(status.infoType), null, attributes);
					} else {
						int infoType = (null != status) ? status.infoType : drmInfo.getInfoType();
						error = new DrmErrorEvent(mUniqueId, getErrorType(infoType), null, attributes);
					}
					break;
				}
				case ACTION_REMOVE_ALL_RIGHTS: {
					if (ERROR_NONE == _removeAllRights(mUniqueId)) {
						event = new DrmEvent(mUniqueId, DrmEvent.TYPE_ALL_RIGHTS_REMOVED, null);
					} else {
						error = new DrmErrorEvent(mUniqueId, DrmErrorEvent.TYPE_REMOVE_ALL_RIGHTS_FAILED, null);
					}
					break;
				}
				default:
					Log.e(TAG, "Unknown message type " + msg.what);
					return;
			}
			if (null != mOnEventListener && null != event) {
				mOnEventListener.onEvent(DrmManagerClient.this, event);
			}
			if (null != mOnErrorListener && null != error) {
				mOnErrorListener.onError(DrmManagerClient.this, error);
			}
		}
	}

	/** @hide */
	public static void notify(Object thisReference, int uniqueId, int infoType, String message) {
		DrmManagerClient instance = (DrmManagerClient) ((WeakReference) thisReference).get();

		if (null != instance && null != instance.mInfoHandler) {
			Message m = instance.mInfoHandler.obtainMessage(InfoHandler.INFO_EVENT_TYPE, uniqueId, infoType, message);
			instance.mInfoHandler.sendMessage(m);
		}
	}

	private class InfoHandler extends Handler {
		public static final int INFO_EVENT_TYPE = 1;

		public InfoHandler(Looper looper) {
			super(looper);
		}

		public void handleMessage(Message msg) {
			DrmInfoEvent info = null;
			DrmErrorEvent error = null;

			switch (msg.what) {
				case InfoHandler.INFO_EVENT_TYPE:
					int uniqueId = msg.arg1;
					int infoType = msg.arg2;
					String message = msg.obj.toString();

					switch (infoType) {
						case DrmInfoEvent.TYPE_REMOVE_RIGHTS: {
							try {
								DrmUtils.removeFile(message);
							} catch (IOException e) {
								e.printStackTrace();
							}
							info = new DrmInfoEvent(uniqueId, infoType, message);
							break;
						}
						case DrmInfoEvent.TYPE_ALREADY_REGISTERED_BY_ANOTHER_ACCOUNT:
						case DrmInfoEvent.TYPE_RIGHTS_INSTALLED:
						case DrmInfoEvent.TYPE_WAIT_FOR_RIGHTS:
						case DrmInfoEvent.TYPE_ACCOUNT_ALREADY_REGISTERED:
						case DrmInfoEvent.TYPE_RIGHTS_REMOVED: {
							info = new DrmInfoEvent(uniqueId, infoType, message);
							break;
						}
						default:
							error = new DrmErrorEvent(uniqueId, infoType, message);
							break;
					}

					if (null != mOnInfoListener && null != info) {
						mOnInfoListener.onInfo(DrmManagerClient.this, info);
					}
					if (null != mOnErrorListener && null != error) {
						mOnErrorListener.onError(DrmManagerClient.this, error);
					}
					return;
				default:
					Log.e(TAG, "Unknown message type " + msg.what);
					return;
			}
		}
	}

	/** Creates a <code>DrmManagerClient</code>. */
	public DrmManagerClient(Context context) {
		mContext = context;
		createEventThreads();

		// save the unique id
		mUniqueId = _initialize();
	}

	@Override
	protected void finalize() throws Throwable {
		try {
			close();
		} finally {
			super.finalize();
		}
	}

	@Override
	public void close() {
		if (mClosed.compareAndSet(false, true)) {
			if (mEventHandler != null) {
				mEventThread.quit();
				mEventThread = null;
			}
			if (mInfoHandler != null) {
				mInfoThread.quit();
				mInfoThread = null;
			}
			mEventHandler = null;
			mInfoHandler = null;
			mOnEventListener = null;
			mOnInfoListener = null;
			mOnErrorListener = null;
			_release(mUniqueId);
		}
	}

	@Deprecated
	public void release() {
		close();
	}

	public synchronized void setOnInfoListener(OnInfoListener infoListener) {
		mOnInfoListener = infoListener;
		if (null != infoListener) {
			createListeners();
		}
	}

	public synchronized void setOnEventListener(OnEventListener eventListener) {
		mOnEventListener = eventListener;
		if (null != eventListener) {
			createListeners();
		}
	}

	public synchronized void setOnErrorListener(OnErrorListener errorListener) {
		mOnErrorListener = errorListener;
		if (null != errorListener) {
			createListeners();
		}
	}

	public String[] getAvailableDrmEngines() {
		DrmSupportInfo[] supportInfos = _getAllSupportInfo(mUniqueId);
		ArrayList<String> descriptions = new ArrayList<String>();

		for (int i = 0; i < supportInfos.length; i++) {
			descriptions.add(supportInfos[i].getDescriprition());
		}

		String[] drmEngines = new String[descriptions.size()];
		return descriptions.toArray(drmEngines);
	}

	public @NonNull Collection<DrmSupportInfo> getAvailableDrmSupportInfo() {
		return Arrays.asList(_getAllSupportInfo(mUniqueId));
	}

	public ContentValues getConstraints(String path, int action) {
		if (null == path || path.equals("") || !DrmStore.Action.isValid(action)) {
			throw new IllegalArgumentException("Given usage or path is invalid/null");
		}
		return _getConstraints(mUniqueId, path, action);
	}

	public ContentValues getMetadata(String path) {
		if (null == path || path.equals("")) {
			throw new IllegalArgumentException("Given path is invalid/null");
		}
		return _getMetadata(mUniqueId, path);
	}

	public ContentValues getConstraints(Uri uri, int action) {
		if (null == uri || Uri.EMPTY == uri) {
			throw new IllegalArgumentException("Uri should be non null");
		}
		return getConstraints(convertUriToPath(uri), action);
	}

	public ContentValues getMetadata(Uri uri) {
		if (null == uri || Uri.EMPTY == uri) {
			throw new IllegalArgumentException("Uri should be non null");
		}
		return getMetadata(convertUriToPath(uri));
	}

	public int saveRights(DrmRights drmRights, String rightsPath, String contentPath) throws IOException {
		if (null == drmRights || !drmRights.isValid()) {
			throw new IllegalArgumentException("Given drmRights or contentPath is not valid");
		}
		if (null != rightsPath && !rightsPath.equals("")) {
			DrmUtils.writeToFile(rightsPath, drmRights.getData());
		}
		return _saveRights(mUniqueId, drmRights, rightsPath, contentPath);
	}

	public boolean canHandle(String path, String mimeType) {
		if ((null == path || path.equals("")) && (null == mimeType || mimeType.equals(""))) {
			throw new IllegalArgumentException("Path or the mimetype should be non null");
		}
		return _canHandle(mUniqueId, path, mimeType);
	}

	public boolean canHandle(Uri uri, String mimeType) {
		if ((null == uri || Uri.EMPTY == uri) && (null == mimeType || mimeType.equals(""))) {
			throw new IllegalArgumentException("Uri or the mimetype should be non null");
		}
		return canHandle(convertUriToPath(uri), mimeType);
	}

	public int processDrmInfo(DrmInfo drmInfo) {
		if (null == drmInfo || !drmInfo.isValid()) {
			throw new IllegalArgumentException("Given drmInfo is invalid/null");
		}
		int result = ERROR_UNKNOWN;
		if (null != mEventHandler) {
			Message msg = mEventHandler.obtainMessage(ACTION_PROCESS_DRM_INFO, drmInfo);
			result = (mEventHandler.sendMessage(msg)) ? ERROR_NONE : result;
		}
		return result;
	}

	public DrmInfo acquireDrmInfo(DrmInfoRequest drmInfoRequest) {
		if (null == drmInfoRequest || !drmInfoRequest.isValid()) {
			throw new IllegalArgumentException("Given drmInfoRequest is invalid/null");
		}
		return _acquireDrmInfo(mUniqueId, drmInfoRequest);
	}

	public int acquireRights(DrmInfoRequest drmInfoRequest) {
		DrmInfo drmInfo = acquireDrmInfo(drmInfoRequest);
		if (null == drmInfo) {
			return ERROR_UNKNOWN;
		}
		return processDrmInfo(drmInfo);
	}

	public int getDrmObjectType(String path, String mimeType) {
		if ((null == path || path.equals("")) && (null == mimeType || mimeType.equals(""))) {
			throw new IllegalArgumentException("Path or the mimetype should be non null");
		}
		return _getDrmObjectType(mUniqueId, path, mimeType);
	}

	public int getDrmObjectType(Uri uri, String mimeType) {
		if ((null == uri || Uri.EMPTY == uri) && (null == mimeType || mimeType.equals(""))) {
			throw new IllegalArgumentException("Uri or the mimetype should be non null");
		}
		String path = "";
		try {
			path = convertUriToPath(uri);
		} catch (Exception e) {
			// Even uri is invalid the mimetype shall be valid, so allow to proceed further.
			Log.w(TAG, "Given Uri could not be found in media store");
		}
		return getDrmObjectType(path, mimeType);
	}

	public String getOriginalMimeType(String path) {
		if (null == path || path.equals("")) {
			throw new IllegalArgumentException("Given path should be non null");
		}

		/* AOSP opens the file and hands the descriptor to the native call, which ignores it with no engine;
		 * the open can only fail silently (IOException is swallowed), so it is left out */
		return _getOriginalMimeType(mUniqueId, path, new File(path).exists());
	}

	public String getOriginalMimeType(Uri uri) {
		if (null == uri || Uri.EMPTY == uri) {
			throw new IllegalArgumentException("Given uri is not valid");
		}
		return getOriginalMimeType(convertUriToPath(uri));
	}

	public int checkRightsStatus(String path) {
		return checkRightsStatus(path, DrmStore.Action.DEFAULT);
	}

	public int checkRightsStatus(Uri uri) {
		if (null == uri || Uri.EMPTY == uri) {
			throw new IllegalArgumentException("Given uri is not valid");
		}
		return checkRightsStatus(convertUriToPath(uri));
	}

	public int checkRightsStatus(String path, int action) {
		if (null == path || path.equals("") || !DrmStore.Action.isValid(action)) {
			throw new IllegalArgumentException("Given path or action is not valid");
		}
		return _checkRightsStatus(mUniqueId, path, action);
	}

	public int checkRightsStatus(Uri uri, int action) {
		if (null == uri || Uri.EMPTY == uri) {
			throw new IllegalArgumentException("Given uri is not valid");
		}
		return checkRightsStatus(convertUriToPath(uri), action);
	}

	public int removeRights(String path) {
		if (null == path || path.equals("")) {
			throw new IllegalArgumentException("Given path should be non null");
		}
		return _removeRights(mUniqueId, path);
	}

	public int removeRights(Uri uri) {
		if (null == uri || Uri.EMPTY == uri) {
			throw new IllegalArgumentException("Given uri is not valid");
		}
		return removeRights(convertUriToPath(uri));
	}

	public int removeAllRights() {
		int result = ERROR_UNKNOWN;
		if (null != mEventHandler) {
			Message msg = mEventHandler.obtainMessage(ACTION_REMOVE_ALL_RIGHTS);
			result = (mEventHandler.sendMessage(msg)) ? ERROR_NONE : result;
		}
		return result;
	}

	public int openConvertSession(String mimeType) {
		if (null == mimeType || mimeType.equals("")) {
			throw new IllegalArgumentException("Path or the mimeType should be non null");
		}
		return _openConvertSession(mUniqueId, mimeType);
	}

	public DrmConvertedStatus convertData(int convertId, byte[] inputData) {
		if (null == inputData || 0 >= inputData.length) {
			throw new IllegalArgumentException("Given inputData should be non null");
		}
		return _convertData(mUniqueId, convertId, inputData);
	}

	public DrmConvertedStatus closeConvertSession(int convertId) {
		return _closeConvertSession(mUniqueId, convertId);
	}

	private int getEventType(int infoType) {
		int eventType = -1;

		switch (infoType) {
			case DrmInfoRequest.TYPE_REGISTRATION_INFO:
			case DrmInfoRequest.TYPE_UNREGISTRATION_INFO:
			case DrmInfoRequest.TYPE_RIGHTS_ACQUISITION_INFO:
				eventType = DrmEvent.TYPE_DRM_INFO_PROCESSED;
				break;
		}
		return eventType;
	}

	private int getErrorType(int infoType) {
		int error = -1;

		switch (infoType) {
			case DrmInfoRequest.TYPE_REGISTRATION_INFO:
			case DrmInfoRequest.TYPE_UNREGISTRATION_INFO:
			case DrmInfoRequest.TYPE_RIGHTS_ACQUISITION_INFO:
				error = DrmErrorEvent.TYPE_PROCESS_DRM_INFO_FAILED;
				break;
		}
		return error;
	}

	/* AOSP convertUriToPath(), except that a content: Uri is not resolved through MediaStore */
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

	/* The native interfaces of AOSP with a DrmManager that has no plugin (see the class comment) */
	private int _initialize() {
		return 0;
	}

	private void _setListeners(int uniqueId, Object weak_this) {}

	private void _release(int uniqueId) {}

	private ContentValues _getConstraints(int uniqueId, String path, int usage) {
		return null;
	}

	private ContentValues _getMetadata(int uniqueId, String path) {
		return null;
	}

	private boolean _canHandle(int uniqueId, String path, String mimeType) {
		return false;
	}

	private DrmInfoStatus _processDrmInfo(int uniqueId, DrmInfo drmInfo) {
		return null;
	}

	private DrmInfo _acquireDrmInfo(int uniqueId, DrmInfoRequest drmInfoRequest) {
		return null;
	}

	private int _saveRights(int uniqueId, DrmRights drmRights, String rightsPath, String contentPath) {
		return ERROR_UNKNOWN;
	}

	private int _getDrmObjectType(int uniqueId, String path, String mimeType) {
		return DrmStore.DrmObjectType.UNKNOWN;
	}

	private String _getOriginalMimeType(int uniqueId, String path, boolean fileExists) {
		return "";
	}

	private int _checkRightsStatus(int uniqueId, String path, int action) {
		return DrmStore.RightsStatus.RIGHTS_INVALID;
	}

	private int _removeRights(int uniqueId, String path) {
		return ERROR_UNKNOWN;
	}

	private int _removeAllRights(int uniqueId) {
		return ERROR_UNKNOWN;
	}

	private int _openConvertSession(int uniqueId, String mimeType) {
		return INVALID_SESSION;
	}

	private DrmConvertedStatus _convertData(int uniqueId, int convertId, byte[] inputData) {
		return null;
	}

	private DrmConvertedStatus _closeConvertSession(int uniqueId, int convertId) {
		return null;
	}

	private DrmSupportInfo[] _getAllSupportInfo(int uniqueId) {
		return new DrmSupportInfo[0];
	}

	private void createEventThreads() {
		if (mEventHandler == null && mInfoHandler == null) {
			mInfoThread = new HandlerThread("DrmManagerClient.InfoHandler");
			mInfoThread.start();
			mInfoHandler = new InfoHandler(mInfoThread.getLooper());

			mEventThread = new HandlerThread("DrmManagerClient.EventHandler");
			mEventThread.start();
			mEventHandler = new EventHandler(mEventThread.getLooper());
		}
	}

	private void createListeners() {
		_setListeners(mUniqueId, new WeakReference<DrmManagerClient>(this));
	}
}
