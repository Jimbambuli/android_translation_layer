package android.app;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.util.Slog;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/* AOSP DownloadManager. AOSP hands the request to DownloadProvider (a system service with its own database and
 * notifications); ATL has no such service, so the downloads are made here: one thread per request, the rows kept in
 * memory for this run, HTTP(S) with the request's headers. Left out: resuming, the retry/backoff of
 * DownloadThread, the Downloads UI and the download notification (setNotificationVisibility is kept only),
 * network type and roaming restrictions (the desktop has one link) */
public class DownloadManager {
	private static final String TAG = "DownloadManager";

	public static final String COLUMN_ID = "_id";
	public static final String COLUMN_TITLE = "title";
	public static final String COLUMN_DESCRIPTION = "description";
	public static final String COLUMN_URI = "uri";
	public static final String COLUMN_MEDIA_TYPE = "media_type";
	public static final String COLUMN_TOTAL_SIZE_BYTES = "total_size";
	public static final String COLUMN_LOCAL_URI = "local_uri";
	public static final String COLUMN_LOCAL_FILENAME = "local_filename";
	public static final String COLUMN_STATUS = "status";
	public static final String COLUMN_REASON = "reason";
	public static final String COLUMN_BYTES_DOWNLOADED_SO_FAR = "bytes_so_far";
	public static final String COLUMN_LAST_MODIFIED_TIMESTAMP = "last_modified_timestamp";
	public static final String COLUMN_MEDIAPROVIDER_URI = "mediaprovider_uri";

	public static final int STATUS_PENDING = 1 << 0;
	public static final int STATUS_RUNNING = 1 << 1;
	public static final int STATUS_PAUSED = 1 << 2;
	public static final int STATUS_SUCCESSFUL = 1 << 3;
	public static final int STATUS_FAILED = 1 << 4;

	public static final int ERROR_UNKNOWN = 1000;
	public static final int ERROR_FILE_ERROR = 1001;
	public static final int ERROR_UNHANDLED_HTTP_CODE = 1002;
	public static final int ERROR_HTTP_DATA_ERROR = 1004;
	public static final int ERROR_TOO_MANY_REDIRECTS = 1005;
	public static final int ERROR_INSUFFICIENT_SPACE = 1006;
	public static final int ERROR_DEVICE_NOT_FOUND = 1007;
	public static final int ERROR_CANNOT_RESUME = 1008;
	public static final int ERROR_FILE_ALREADY_EXISTS = 1009;

	public static final int PAUSED_WAITING_TO_RETRY = 1;
	public static final int PAUSED_WAITING_FOR_NETWORK = 2;
	public static final int PAUSED_QUEUED_FOR_WIFI = 3;
	public static final int PAUSED_UNKNOWN = 4;

	public static final String ACTION_DOWNLOAD_COMPLETE = "android.intent.action.DOWNLOAD_COMPLETE";
	public static final String ACTION_NOTIFICATION_CLICKED = "android.intent.action.DOWNLOAD_NOTIFICATION_CLICKED";
	public static final String ACTION_VIEW_DOWNLOADS = "android.intent.action.VIEW_DOWNLOADS";
	public static final String EXTRA_DOWNLOAD_ID = "extra_download_id";
	public static final String EXTRA_NOTIFICATION_CLICK_DOWNLOAD_IDS = "extra_click_download_ids";
	public static final String INTENT_EXTRAS_SORT_BY_SIZE = "android.app.DownloadManager.extra_sortBySize";

	public static final int NETWORK_MOBILE = 1 << 0;
	public static final int NETWORK_WIFI = 1 << 1;
	public static final int NETWORK_BLUETOOTH = 1 << 2;

	public static final int VISIBILITY_VISIBLE = 0;
	public static final int VISIBILITY_VISIBLE_NOTIFY_COMPLETED = 1;
	public static final int VISIBILITY_HIDDEN = 2;
	public static final int VISIBILITY_VISIBLE_NOTIFY_ONLY_COMPLETION = 3;

	public static class Request {
		private final Uri mUri;
		private Uri mDestinationUri;
		private final List<Map.Entry<String, String>> mRequestHeaders = new ArrayList<>();
		private CharSequence mTitle;
		private CharSequence mDescription;
		private String mMimeType;
		private int mNotificationVisibility = VISIBILITY_VISIBLE;
		private boolean mIsVisibleInDownloadsUi = true;
		private boolean mScannable = false;

		public Request(Uri uri) {
			if (uri == null)
				throw new NullPointerException();
			String scheme = uri.getScheme();
			if (scheme == null || !(scheme.equals("http") || scheme.equals("https")))
				throw new IllegalArgumentException("Can only download HTTP/HTTPS URIs: " + uri);
			mUri = uri;
		}

		public Request setDestinationUri(Uri uri) {
			mDestinationUri = uri;
			return this;
		}

		public Request setDestinationInExternalFilesDir(Context context, String dirType, String subPath) {
			final File file = context.getExternalFilesDir(dirType);
			if (file == null)
				throw new IllegalStateException("Failed to get external storage files directory");
			else if (file.exists()) {
				if (!file.isDirectory())
					throw new IllegalStateException(file.getAbsolutePath() + " already exists and is not a directory");
			} else if (!file.mkdirs()) {
				throw new IllegalStateException("Unable to create directory: " + file.getAbsolutePath());
			}
			setDestinationFromBase(file, subPath);
			return this;
		}

		/* AOSP (target SDK Q and later) has the DownloadProvider create the directory; here it is made directly */
		public Request setDestinationInExternalPublicDir(String dirType, String subPath) {
			File file = Environment.getExternalStoragePublicDirectory(dirType);
			if (file == null)
				throw new IllegalStateException("Failed to get external storage public directory");
			if (file.exists()) {
				if (!file.isDirectory())
					throw new IllegalStateException(file.getAbsolutePath() + " already exists and is not a directory");
			} else if (!file.mkdirs()) {
				throw new IllegalStateException("Unable to create directory: " + file.getAbsolutePath());
			}
			setDestinationFromBase(file, subPath);
			return this;
		}

		private void setDestinationFromBase(File base, String subPath) {
			if (subPath == null)
				throw new NullPointerException("subPath cannot be null");
			mDestinationUri = Uri.withAppendedPath(Uri.fromFile(base), subPath);
		}

		/* AOSP: deprecated, ignored from Q on; only kept */
		@Deprecated
		public void allowScanningByMediaScanner() {
			mScannable = true;
		}

		public Request addRequestHeader(String header, String value) {
			if (header == null)
				throw new NullPointerException("header cannot be null");
			if (header.contains(":"))
				throw new IllegalArgumentException("header may not contain ':'");
			if (value == null)
				value = "";
			mRequestHeaders.add(new java.util.AbstractMap.SimpleImmutableEntry<String, String>(header, value));
			return this;
		}

		public Request setTitle(CharSequence title) {
			mTitle = title;
			return this;
		}

		public Request setDescription(CharSequence description) {
			mDescription = description;
			return this;
		}

		public Request setMimeType(String mimeType) {
			mMimeType = mimeType;
			return this;
		}

		public Request setAllowedNetworkTypes(int flags) { return this; }

		public Request setAllowedOverRoaming(boolean allowed) { return this; }

		public Request setAllowedOverMetered(boolean allow) { return this; }

		public Request setRequiresCharging(boolean requiresCharging) { return this; }

		public Request setRequiresDeviceIdle(boolean requiresDeviceIdle) { return this; }

		public Request setVisibleInDownloadsUi(boolean isVisible) {
			mIsVisibleInDownloadsUi = isVisible;
			return this;
		}

		public Request setNotificationVisibility(int visibility) {
			mNotificationVisibility = visibility;
			return this;
		}
	}

	public static class Query {
		public static final int ORDER_ASCENDING = 1;
		public static final int ORDER_DESCENDING = 2;

		private long[] mIds = null;
		private Integer mStatusFlags = null;
		private String mOrderByColumn = COLUMN_LAST_MODIFIED_TIMESTAMP;
		private int mOrderDirection = ORDER_DESCENDING;
		private boolean mOnlyIncludeVisibleInDownloadsUi = false;

		public Query setFilterById(long... ids) {
			mIds = ids;
			return this;
		}

		public Query setFilterByStatus(int flags) {
			mStatusFlags = flags;
			return this;
		}

		public Query setOnlyIncludeVisibleInDownloadsUi(boolean value) {
			mOnlyIncludeVisibleInDownloadsUi = value;
			return this;
		}

		public Query orderBy(String column, int direction) {
			if (direction != ORDER_ASCENDING && direction != ORDER_DESCENDING)
				throw new IllegalArgumentException("Invalid direction: " + direction);
			if (column.equals(COLUMN_LAST_MODIFIED_TIMESTAMP) || column.equals(COLUMN_TOTAL_SIZE_BYTES)) {
				mOrderByColumn = column;
				mOrderDirection = direction;
			} else {
				throw new IllegalArgumentException("Cannot order by " + column);
			}
			return this;
		}
	}

	/* a download of this run (AOSP: a row of the DownloadProvider's database) */
	private static final class Row {
		long id;
		Uri uri;
		String title;
		String description;
		String mimeType;
		int status = STATUS_PENDING;
		int reason;
		long totalBytes = -1;
		long bytesSoFar;
		long lastModified = System.currentTimeMillis();
		File file;
		boolean visibleInUi;
		volatile boolean canceled;
	}

	private static final Map<Long, Row> sRows = new LinkedHashMap<>();
	private static long sNextId = 1;

	private final Context mContext;

	public DownloadManager() {
		this(null);
	}

	/** @hide */
	public DownloadManager(Context context) {
		mContext = context;
	}

	/* AOSP enqueue(): the id of the new download, whose result is announced with ACTION_DOWNLOAD_COMPLETE */
	public long enqueue(Request request) {
		final Row row = new Row();
		row.uri = request.mUri;
		row.title = request.mTitle != null ? request.mTitle.toString() : "";
		row.description = request.mDescription != null ? request.mDescription.toString() : "";
		row.mimeType = request.mMimeType;
		row.visibleInUi = request.mIsVisibleInDownloadsUi;
		final Uri destination = request.mDestinationUri;
		synchronized (sRows) {
			row.id = sNextId++;
			sRows.put(row.id, row);
		}
		if (destination != null && "file".equals(destination.getScheme()))
			row.file = new File(destination.getPath());
		final List<Map.Entry<String, String>> headers = new ArrayList<>(request.mRequestHeaders);
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				download(row, headers);
			}
		}, "DownloadManager-" + row.id);
		t.setDaemon(true);
		t.start();
		return row.id;
	}

	private void download(Row row, List<Map.Entry<String, String>> headers) {
		File partial = null;
		try {
			synchronized (sRows) {
				row.status = STATUS_RUNNING;
				row.lastModified = System.currentTimeMillis();
			}
			HttpURLConnection conn = (HttpURLConnection)new URL(row.uri.toString()).openConnection();
			conn.setInstanceFollowRedirects(true);
			for (Map.Entry<String, String> h : headers)
				conn.addRequestProperty(h.getKey(), h.getValue());
			int code = conn.getResponseCode();
			if (code != 200) {
				fail(row, code >= 400 && code < 600 ? code : ERROR_UNHANDLED_HTTP_CODE);
				return;
			}
			if (row.mimeType == null) {
				String type = conn.getContentType();
				if (type != null)
					row.mimeType = type.split(";")[0].trim();
			}
			long length = conn.getContentLengthLong();
			synchronized (sRows) {
				row.totalBytes = length;
			}
			File dest = row.file;
			if (dest == null) {
				/* AOSP: no destination = the DownloadProvider's cache, named after the URL */
				File dir = new File(mContext != null ? mContext.getCacheDir() : new File(System.getProperty("java.io.tmpdir")), "DownloadManager");
				dir.mkdirs();
				String name = row.uri.getLastPathSegment();
				dest = new File(dir, (name != null && !name.isEmpty() ? name : "downloadfile") + "-" + row.id);
			} else if (dest.exists()) {
				fail(row, ERROR_FILE_ALREADY_EXISTS);
				return;
			} else if (dest.getParentFile() != null) {
				dest.getParentFile().mkdirs();
			}
			partial = dest;
			try (InputStream in = conn.getInputStream(); OutputStream out = new FileOutputStream(dest)) {
				byte[] buf = new byte[16384];
				int n;
				while ((n = in.read(buf)) != -1) {
					if (row.canceled)
						return;
					out.write(buf, 0, n);
					synchronized (sRows) {
						row.bytesSoFar += n;
						row.lastModified = System.currentTimeMillis();
					}
				}
			}
			synchronized (sRows) {
				row.file = dest;
				if (row.totalBytes < 0)
					row.totalBytes = row.bytesSoFar;
				row.status = STATUS_SUCCESSFUL;
				row.lastModified = System.currentTimeMillis();
			}
			partial = null;
			complete(row);
		} catch (java.io.FileNotFoundException e) {
			Slog.w(TAG, "download " + row.id + " failed", e);
			fail(row, ERROR_FILE_ERROR);
		} catch (IOException e) {
			Slog.w(TAG, "download " + row.id + " failed", e);
			fail(row, ERROR_HTTP_DATA_ERROR);
		} catch (RuntimeException e) {
			Slog.w(TAG, "download " + row.id + " failed", e);
			fail(row, ERROR_UNKNOWN);
		} finally {
			if (partial != null && !row.canceled)
				partial.delete();
		}
	}

	private void fail(Row row, int reason) {
		synchronized (sRows) {
			row.status = STATUS_FAILED;
			row.reason = reason;
			row.lastModified = System.currentTimeMillis();
		}
		complete(row);
	}

	/* AOSP DownloadNotifier/DownloadReceiver: a finished download, successful or not, is announced to the
	 * app that enqueued it */
	private void complete(Row row) {
		if (mContext == null || row.canceled)
			return;
		Intent intent = new Intent(ACTION_DOWNLOAD_COMPLETE);
		intent.putExtra(EXTRA_DOWNLOAD_ID, row.id);
		intent.setPackage(mContext.getPackageName());
		mContext.sendBroadcast(intent);
	}

	public Cursor query(Query query) {
		final String[] columns = {COLUMN_ID, COLUMN_LOCAL_FILENAME, COLUMN_MEDIAPROVIDER_URI, COLUMN_TITLE, COLUMN_DESCRIPTION, COLUMN_URI, COLUMN_STATUS, COLUMN_MEDIA_TYPE, COLUMN_TOTAL_SIZE_BYTES, COLUMN_LAST_MODIFIED_TIMESTAMP, COLUMN_BYTES_DOWNLOADED_SO_FAR, COLUMN_LOCAL_URI, COLUMN_REASON};
		List<Row> matches = new ArrayList<>();
		synchronized (sRows) {
			for (Row r : sRows.values()) {
				if (query.mIds != null) {
					boolean found = false;
					for (long id : query.mIds)
						found |= id == r.id;
					if (!found)
						continue;
				}
				if (query.mStatusFlags != null && (query.mStatusFlags & r.status) == 0)
					continue;
				if (query.mOnlyIncludeVisibleInDownloadsUi && !r.visibleInUi)
					continue;
				matches.add(r);
			}
			final boolean bySize = query.mOrderByColumn.equals(COLUMN_TOTAL_SIZE_BYTES);
			final int dir = query.mOrderDirection == Query.ORDER_ASCENDING ? 1 : -1;
			Collections.sort(matches, new Comparator<Row>() {
				@Override
				public int compare(Row a, Row b) {
					return dir * Long.compare(bySize ? a.totalBytes : a.lastModified, bySize ? b.totalBytes : b.lastModified);
				}
			});
			MatrixCursor cursor = new MatrixCursor(columns);
			for (Row r : matches) {
				boolean done = r.status == STATUS_SUCCESSFUL;
				cursor.addRow(new Object[] {r.id, done && r.file != null ? r.file.getAbsolutePath() : null, null, r.title, r.description, r.uri.toString(), r.status, r.mimeType, r.totalBytes, r.lastModified, r.bytesSoFar, done && r.file != null ? Uri.fromFile(r.file).toString() : null, r.reason});
			}
			return cursor;
		}
	}

	/* AOSP: the download is marked deleted and its file removed */
	public int remove(long... ids) {
		if (ids == null || ids.length == 0)
			throw new IllegalArgumentException("input param 'ids' can't be null");
		int count = 0;
		synchronized (sRows) {
			for (long id : ids) {
				Row r = sRows.remove(id);
				if (r == null)
					continue;
				r.canceled = true;
				if (r.file != null)
					r.file.delete();
				count++;
			}
		}
		return count;
	}

	/* AOSP returns a content://downloads URI of the DownloadProvider for a successful download; ATL has no such
	 * provider, the file's own Uri takes its place */
	public Uri getUriForDownloadedFile(long id) {
		synchronized (sRows) {
			Row r = sRows.get(id);
			if (r != null && r.status == STATUS_SUCCESSFUL && r.file != null)
				return Uri.fromFile(r.file);
		}
		return null;
	}

	public String getMimeTypeForDownloadedFile(long id) {
		synchronized (sRows) {
			Row r = sRows.get(id);
			return r != null && r.status == STATUS_SUCCESSFUL ? r.mimeType : null;
		}
	}

	public ParcelFileDescriptor openDownloadedFile(long id) throws FileNotFoundException {
		synchronized (sRows) {
			Row r = sRows.get(id);
			if (r == null || r.status != STATUS_SUCCESSFUL || r.file == null)
				throw new FileNotFoundException("Download " + id + " does not exist or is not complete");
			return ParcelFileDescriptor.open(r.file, ParcelFileDescriptor.MODE_READ_ONLY);
		}
	}

	/* AOSP: a finished file is registered as a completed download of its own */
	public long addCompletedDownload(String title, String description, boolean isMediaScannerScannable, String mimeType, String path, long length, boolean showNotification) {
		if (path == null || !new File(path).isAbsolute())
			throw new IllegalArgumentException("Not a valid path: " + path);
		Row row = new Row();
		row.uri = Uri.fromFile(new File(path));
		row.title = title;
		row.description = description;
		row.mimeType = mimeType;
		row.file = new File(path);
		row.status = STATUS_SUCCESSFUL;
		row.totalBytes = row.bytesSoFar = length;
		row.visibleInUi = true;
		synchronized (sRows) {
			row.id = sNextId++;
			sRows.put(row.id, row);
		}
		return row.id;
	}

	public static Long getMaxBytesOverMobile(Context context) {
		return null;
	}
}
