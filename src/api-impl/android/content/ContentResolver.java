package android.content;

import android.accounts.Account;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;

public class ContentResolver {
	/** @hide */
	public static final String MIME_TYPE_DEFAULT = "application/octet-stream";
	public static final String SCHEME_CONTENT = "content";
	public static final String SCHEME_ANDROID_RESOURCE = "android.resource";
	public static final String SCHEME_FILE = "file";

	private final Context mContext;

	public ContentResolver(Context context) {
		mContext = context;
	}

	public static final String SYNC_EXTRAS_IGNORE_SETTINGS = "ignore_settings";

	public final void registerContentObserver(Uri uri, boolean notifyForDescendants, ContentObserver observer) {
	}
	public final void unregisterContentObserver(ContentObserver observer) {
	}
	public void notifyChange(Uri uri, ContentObserver observer) {
	}
	public int getUserId() {
		return 0;
	}
	public final void registerContentObserver(Uri uri, boolean notifyForDescendants, ContentObserver observer, int userHandle) {
	}

	public ParcelFileDescriptor openFileDescriptor(Uri uri, String mode) throws FileNotFoundException {
		if ("file".equals(uri.getScheme())) {
			return ParcelFileDescriptor.open(new File(uri.getPath()), ParcelFileDescriptor.parseMode(mode));
		} else {
			ContentProvider provider = ContentProvider.atl_get_content_provider(uri.getAuthority());
			if (provider != null)
				return provider.openFile(uri, mode);
			else
				return null;
		}
	}

	public ParcelFileDescriptor openFileDescriptor(Uri uri, String mode, CancellationSignal signal) throws FileNotFoundException {
		return openFileDescriptor(uri, mode);
	}

	public AssetFileDescriptor openAssetFileDescriptor(Uri uri, String mode) throws FileNotFoundException {
		if ("file".equals(uri.getScheme())) {
			return new AssetFileDescriptor(ParcelFileDescriptor.open(new File(uri.getPath()), ParcelFileDescriptor.parseMode(mode)), 0, AssetFileDescriptor.UNKNOWN_LENGTH);
		} else {
			ContentProvider provider = ContentProvider.atl_get_content_provider(uri.getAuthority());
			if (provider != null)
				return provider.openAssetFile(uri, mode);
			else
				return null;
		}
	}

	public AssetFileDescriptor openAssetFileDescriptor(Uri uri, String mode, CancellationSignal signal) throws FileNotFoundException {
		return openAssetFileDescriptor(uri, mode);
	}

	public final AssetFileDescriptor openTypedAssetFileDescriptor(Uri uri, String mimeType, Bundle opts) throws FileNotFoundException {
		/* FIXME */
		return openAssetFileDescriptor(uri, "r");
	}

	public final AssetFileDescriptor openTypedAssetFileDescriptor(Uri uri, String mimeType, Bundle opts, CancellationSignal cancellationSignal) throws FileNotFoundException {
		return openTypedAssetFileDescriptor(uri, mimeType, opts);
	}

	public InputStream openInputStream(Uri uri) throws FileNotFoundException {
		ParcelFileDescriptor fd = openFileDescriptor(uri, "r");
		return fd != null ? new ParcelFileDescriptor.AutoCloseInputStream(fd) : null;
	}

	public OutputStream openOutputStream(Uri uri) throws FileNotFoundException {
		ParcelFileDescriptor fd = openFileDescriptor(uri, "w");
		return fd != null ? new ParcelFileDescriptor.AutoCloseOutputStream(fd) : null;
	}

	public Cursor query(Uri uri, String[] projection, Bundle queryArgs, CancellationSignal cancellationSignal) {
		if ("file".equals(uri.getScheme())) {
			MatrixCursor cursor = new MatrixCursor(projection);
			Object[] row = new Object[projection.length];
			native_query_file_info(uri.getPath(), projection, row);
			cursor.addRow(row);
			return cursor;
		} else {
			return null;
		}
	}

	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		ContentProvider provider = ContentProvider.atl_get_content_provider(uri.getAuthority());
		if (provider != null) {
			return provider.query(uri, projection, selection, selectionArgs, sortOrder);
		} else if ("file".equals(uri.getScheme())) {
			if (projection == null)
				projection = new String[] {"_display_name", "mime_type"};
			MatrixCursor cursor = new MatrixCursor(projection);
			Object[] row = new Object[projection.length];
			native_query_file_info(uri.getPath(), projection, row);
			cursor.addRow(row);
			return cursor;
		} else {
			return null;
		}
	}

	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder, CancellationSignal cancellationSignal) {
		return query(uri, projection, selection, selectionArgs, sortOrder);
	}

	public int delete(Uri uri, String selection, String[] selectionArgs) {
		ContentProvider provider = ContentProvider.atl_get_content_provider(uri.getAuthority());
		if (provider != null)
			return provider.delete(uri, selection, selectionArgs);
		else
			return 0;
	}

	public Uri insert(Uri uri, ContentValues values) {
		ContentProvider provider = ContentProvider.atl_get_content_provider(uri.getAuthority());
		if (provider != null)
			return provider.insert(uri, values);
		else
			return null;
	}

	public String getType(Uri uri) {
		ContentProvider provider = ContentProvider.atl_get_content_provider(uri.getAuthority());
		if (provider != null)
			return provider.getType(uri);
		else
			return null;
	}

	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		ContentProvider provider = ContentProvider.atl_get_content_provider(uri.getAuthority());
		if (provider != null)
			return provider.update(uri, values, selection, selectionArgs);
		else
			return 0;
	}

	public List getPersistedUriPermissions() {
		return Collections.emptyList();
	}

	public final ContentProviderClient acquireContentProviderClient(Uri uri) {
		return acquireContentProviderClient(uri.getAuthority());
	}

	public final ContentProviderClient acquireContentProviderClient(String name) {
		ContentProvider provider = ContentProvider.atl_get_content_provider(name);
		return provider != null ? new ContentProviderClient(provider) : null;
	}

	public final ContentProviderClient acquireUnstableContentProviderClient(Uri uri) {
		return acquireContentProviderClient(uri);
	}

	public final ContentProviderClient acquireUnstableContentProviderClient(String name) {
		return acquireContentProviderClient(name);
	}

	public static void requestSync(Account account, String authority, Bundle extras) {
	}

	public static void cancelSync(Account account, String authority) {
	}

	public static void setMasterSyncAutomatically(boolean sync) {
	}

	public static boolean isSyncActive(Account account, String authority) {
		return false;
	}

	public static final int SYNC_OBSERVER_TYPE_SETTINGS = 1 << 0;
	public static final int SYNC_OBSERVER_TYPE_PENDING = 1 << 1;
	public static final int SYNC_OBSERVER_TYPE_ACTIVE = 1 << 2;

	/**
	 * Request notifications when the different aspects of the SyncManager change. The
	 * different items that can be requested are:
	 * <ul>
	 * <li> {@link #SYNC_OBSERVER_TYPE_PENDING}
	 * <li> {@link #SYNC_OBSERVER_TYPE_ACTIVE}
	 * <li> {@link #SYNC_OBSERVER_TYPE_SETTINGS}
	 * </ul>
	 * The caller can set one or more of the status types in the mask for any
	 * given listener registration.
	 * @param mask the status change types that will cause the callback to be invoked
	 * @param callback observer to be invoked when the status changes
	 * @return a handle that can be used to remove the listener at a later time
	 */
	public static Object addStatusChangeListener(int mask, final SyncStatusObserver callback) {
		if (callback == null) {
			throw new IllegalArgumentException("you passed in a null callback");
		}
		/* there is no SyncManager, so the sync status never changes */
		return new Object();
	}

	/**
	 * Remove a previously registered status change listener.
	 * @param handle the handle that was returned by {@link #addStatusChangeListener}
	 */
	public static void removeStatusChangeListener(Object handle) {
		if (handle == null) {
			throw new IllegalArgumentException("you passed in a null handle");
		}
	}

	private static native void native_query_file_info(String path, String[] attributes, Object[] result);

	/* AOSP ContentResolver.OpenResourceIdResult and getResourceId() (@hide): a resource identified by the {@link Resources} that contains it, and a resource id.
	 *
	 * @hide
	 */
	public class OpenResourceIdResult {
		public Resources r;
		public int id;
	}

	/**
	 * Resolves an android.resource URI to a {@link Resources} and a resource id.
	 *
	 * @hide
	 */
	public OpenResourceIdResult getResourceId(Uri uri) throws FileNotFoundException {
		String authority = uri.getAuthority();
		Resources r;
		if (TextUtils.isEmpty(authority)) {
			throw new FileNotFoundException("No authority: " + uri);
		} else {
			try {
				r = mContext.getPackageManager().getResourcesForApplication(authority);
			} catch (NameNotFoundException ex) {
				throw new FileNotFoundException("No package found for authority: " + uri);
			}
		}
		List<String> path = uri.getPathSegments();
		if (path == null) {
			throw new FileNotFoundException("No path: " + uri);
		}
		int len = path.size();
		int id;
		if (len == 1) {
			try {
				id = Integer.parseInt(path.get(0));
			} catch (NumberFormatException e) {
				throw new FileNotFoundException("Single path segment is not a resource ID: " + uri);
			}
		} else if (len == 2) {
			id = r.getIdentifier(path.get(1), path.get(0), authority);
		} else {
			throw new FileNotFoundException("More than two path segments: " + uri);
		}
		if (id == 0) {
			throw new FileNotFoundException("No resource found for: " + uri);
		}
		OpenResourceIdResult res = new OpenResourceIdResult();
		res.r = r;
		res.id = id;
		return res;
	}
}
