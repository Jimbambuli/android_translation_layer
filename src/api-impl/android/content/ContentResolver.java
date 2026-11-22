package android.content;

import android.accounts.Account;
import android.atl.ATLLoadedApp;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.graphics.Matrix;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import java.util.Objects;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import android.os.CancellationSignal;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;
import android.util.Log;
import android.util.Size;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ContentResolver {
	/** @hide */
	public static final String MIME_TYPE_DEFAULT = "application/octet-stream";
	public static final String SCHEME_CONTENT = "content";
	public static final String SCHEME_ANDROID_RESOURCE = "android.resource";
	public static final String SCHEME_FILE = "file";

	public static final String EXTRA_SIZE = "android.content.extra.SIZE";
	public static final String EXTRA_HONORED_ARGS = "android.content.extra.HONORED_ARGS";
	public static final String QUERY_ARG_SQL_SELECTION = "android:query-arg-sql-selection";
	public static final String QUERY_ARG_SQL_SELECTION_ARGS = "android:query-arg-sql-selection-args";
	public static final String QUERY_ARG_SQL_SORT_ORDER = "android:query-arg-sql-sort-order";
	public static final String QUERY_ARG_SORT_COLUMNS = "android:query-arg-sort-columns";
	public static final String QUERY_ARG_SORT_DIRECTION = "android:query-arg-sort-direction";
	public static final String QUERY_ARG_SORT_COLLATION = "android:query-arg-sort-collation";
	public static final String QUERY_ARG_SQL_GROUP_BY = "android:query-arg-sql-group-by";
	public static final String QUERY_ARG_SQL_HAVING = "android:query-arg-sql-having";
	public static final String QUERY_ARG_SQL_LIMIT = "android:query-arg-sql-limit";
	public static final String QUERY_ARG_GROUP_COLUMNS = "android:query-arg-group-columns";
	public static final String QUERY_ARG_LIMIT = "android:query-arg-limit";
	public static final String QUERY_ARG_OFFSET = "android:query-arg-offset";
	public static final int QUERY_SORT_DIRECTION_ASCENDING = 0;
	public static final int QUERY_SORT_DIRECTION_DESCENDING = 1;

	/**
	 * Returns structured sort args formatted as an SQL sort clause.
	 *
	 * @hide
	 */
	public static String createSqlSortClause(Bundle queryArgs) {
		String[] columns = queryArgs.getStringArray(QUERY_ARG_SORT_COLUMNS);
		if (columns == null || columns.length == 0) {
			throw new IllegalArgumentException("Can't create sort clause without columns.");
		}

		String query = TextUtils.join(", ", columns);

		// Interpret PRIMARY and SECONDARY collation strength as no-case collation based
		// on their javadoc descriptions.
		int collation = queryArgs.getInt(
		    ContentResolver.QUERY_ARG_SORT_COLLATION, java.text.Collator.IDENTICAL);
		if (collation == java.text.Collator.PRIMARY || collation == java.text.Collator.SECONDARY) {
			query += " COLLATE NOCASE";
		}

		int sortDir = queryArgs.getInt(QUERY_ARG_SORT_DIRECTION, Integer.MIN_VALUE);
		if (sortDir != Integer.MIN_VALUE) {
			switch (sortDir) {
				case QUERY_SORT_DIRECTION_ASCENDING:
					query += " ASC";
					break;
				case QUERY_SORT_DIRECTION_DESCENDING:
					query += " DESC";
					break;
				default:
					throw new IllegalArgumentException("Unsupported sort direction value."
					                                   + " See ContentResolver documentation for details.");
			}
		}
		return query;
	}

	private final Context mContext;

	public ContentResolver(Context context) {
		mContext = context;
	}

	public static final String SYNC_EXTRAS_IGNORE_SETTINGS = "ignore_settings";

	/* AOSP ContentService's observers, for the process: an observer hears of changes of its URI, of the URI's
	 * ancestors, and with notifyForDescendants of the URI's descendants (ObserverNode.collectObserversLocked()) */
	private static final class Observation {
		final Uri uri;
		final boolean notifyForDescendants;
		final ContentObserver observer;

		Observation(Uri uri, boolean notifyForDescendants, ContentObserver observer) {
			this.uri = uri;
			this.notifyForDescendants = notifyForDescendants;
			this.observer = observer;
		}
	}
	private static final ArrayList<Observation> sObservations = new ArrayList<>();

	public final void registerContentObserver(Uri uri, boolean notifyForDescendants, ContentObserver observer) {
		if (uri == null || observer == null)
			throw new NullPointerException("uri and observer must not be null");
		synchronized (sObservations) {
			sObservations.add(new Observation(uri, notifyForDescendants, observer));
		}
	}
	public final void unregisterContentObserver(ContentObserver observer) {
		if (observer == null)
			throw new NullPointerException("observer must not be null");
		synchronized (sObservations) {
			sObservations.removeIf(o -> o.observer == observer);
		}
	}

	/* the path of a URI with its authority first, as ObserverNode walks it */
	private static List<String> observedPath(Uri uri) {
		ArrayList<String> path = new ArrayList<>();
		path.add(uri.getAuthority());
		path.addAll(uri.getPathSegments());
		return path;
	}

	public static final int NOTIFY_SYNC_TO_NETWORK = 1 << 0;
	public static final int NOTIFY_SKIP_NOTIFY_FOR_DESCENDANTS = 1 << 1;
	public static final int NOTIFY_INSERT = 1 << 2;
	public static final int NOTIFY_UPDATE = 1 << 3;
	public static final int NOTIFY_DELETE = 1 << 4;
	public static final int NOTIFY_NO_DELAY = 1 << 15;

	public void notifyChange(Uri uri, ContentObserver observer) {
		notifyChange(uri, observer, NOTIFY_SYNC_TO_NETWORK);
	}
	public void notifyChange(Uri uri, ContentObserver observer, boolean syncToNetwork) {
		notifyChange(uri, observer, syncToNetwork ? NOTIFY_SYNC_TO_NETWORK : 0);
	}
	public void notifyChange(Uri uri, ContentObserver observer, int flags) {
		Objects.requireNonNull(uri, "uri");
		for (ContentObserver o : matchingObservers(uri, observer, flags))
			o.dispatchChange(o == observer, uri, flags);
	}
	public void notifyChange(Collection<Uri> uris, ContentObserver observer, int flags) {
		Objects.requireNonNull(uris, "uris");
		// one notification per observer, carrying all the URIs it matched (as AOSP delivers them)
		LinkedHashMap<ContentObserver, ArrayList<Uri>> byObserver = new LinkedHashMap<>();
		for (Uri uri : uris)
			for (ContentObserver o : matchingObservers(uri, observer, flags))
				byObserver.computeIfAbsent(o, k -> new ArrayList<>()).add(uri);
		for (Map.Entry<ContentObserver, ArrayList<Uri>> e : byObserver.entrySet())
			e.getKey().dispatchChange(e.getKey() == observer, e.getValue(), flags);
	}
	private List<ContentObserver> matchingObservers(Uri uri, ContentObserver observer, int flags) {
		List<String> changed = observedPath(uri);
		ArrayList<ContentObserver> notify = new ArrayList<>();
		synchronized (sObservations) {
			for (Observation o : sObservations) {
				List<String> observed = observedPath(o.uri);
				boolean match;
				if (observed.size() <= changed.size())
					// the change is at the observed URI or below it
					match = changed.subList(0, observed.size()).equals(observed) && (observed.size() == changed.size() || o.notifyForDescendants);
				else
					// the change is at an ancestor of the observed URI
					match = observed.subList(0, changed.size()).equals(changed);
				// NOTIFY_SKIP_NOTIFY_FOR_DESCENDANTS: skip the root URI of observers that notify for descendants
				if (match && (flags & NOTIFY_SKIP_NOTIFY_FOR_DESCENDANTS) != 0 && o.notifyForDescendants && observed.size() == changed.size())
					match = false;
				if (match && (o.observer != observer || observer.deliverSelfNotifications()) && !notify.contains(o.observer))
					notify.add(o.observer);
			}
		}
		return notify;
	}
	public int getUserId() {
		return 0;
	}
	public final void registerContentObserver(Uri uri, boolean notifyForDescendants, ContentObserver observer, int userHandle) {
		registerContentObserver(uri, notifyForDescendants, observer);
	}

	/* ATL: the content URI of a file the desktop handed to the app (file dialog, dropped or pasted), a "raw:"
	 * document of the documents provider ATL has like AOSP's DownloadStorageProvider (ATLRawDocumentsProvider) */
	/** @hide */
	public static Uri atlUriForFile(String path) {
		return android.atl.ATLRawDocumentsProvider.documentUri(new File(path));
	}

	/* the path of a file:// URI, else null */
	private static String filePath(Uri uri) {
		if (SCHEME_FILE.equals(uri.getScheme()))
			return uri.getPath();
		return null;
	}

	public ParcelFileDescriptor openFileDescriptor(Uri uri, String mode) throws FileNotFoundException {
		String path = filePath(uri);
		if (path != null) {
			return ParcelFileDescriptor.open(new File(path), ParcelFileDescriptor.parseMode(mode));
		} else {
			ContentProvider provider = ContentProvider.atl_get_content_provider(uri.getAuthority());
			if (provider != null)
				return provider.openFile(uri, mode);
			else
				throw new FileNotFoundException("No content provider: " + uri);
		}
	}

	public ParcelFileDescriptor openFileDescriptor(Uri uri, String mode, CancellationSignal signal) throws FileNotFoundException {
		return openFileDescriptor(uri, mode);
	}

	public AssetFileDescriptor openAssetFileDescriptor(Uri uri, String mode) throws FileNotFoundException {
		String path = filePath(uri);
		if (path != null) {
			return new AssetFileDescriptor(ParcelFileDescriptor.open(new File(path), ParcelFileDescriptor.parseMode(mode)), 0, AssetFileDescriptor.UNKNOWN_LENGTH);
		} else {
			ContentProvider provider = ContentProvider.atl_get_content_provider(uri.getAuthority());
			if (provider != null)
				return provider.openAssetFile(uri, mode);
			else
				throw new FileNotFoundException("No content provider: " + uri);
		}
	}

	public AssetFileDescriptor openAssetFileDescriptor(Uri uri, String mode, CancellationSignal signal) throws FileNotFoundException {
		return openAssetFileDescriptor(uri, mode);
	}

	/** @hide AOSP ContentResolver.openTypedAssetFile(): the same as openTypedAssetFileDescriptor() */
	public final AssetFileDescriptor openTypedAssetFile(Uri uri, String mimeTypeFilter, Bundle opts, CancellationSignal signal) throws FileNotFoundException {
		return openTypedAssetFileDescriptor(uri, mimeTypeFilter, opts, signal);
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
		return openOutputStream(uri, "w");
	}

	public OutputStream openOutputStream(Uri uri, String mode) throws FileNotFoundException {
		ParcelFileDescriptor fd = openFileDescriptor(uri, mode);
		return fd != null ? new ParcelFileDescriptor.AutoCloseOutputStream(fd) : null;
	}

	public Cursor query(Uri uri, String[] projection, Bundle queryArgs, CancellationSignal cancellationSignal) {
		String path = filePath(uri);
		ContentProvider provider = path == null ? ContentProvider.atl_get_content_provider(uri.getAuthority()) : null;
		if (provider != null) {
			// as AOSP: the provider gets the arguments, so one that knows them (MediaStore) honours limits
			return provider.query(uri, projection, queryArgs, cancellationSignal);
		} else if (path != null) {
			if (projection == null)
				projection = new String[] {"_display_name", "_size", "mime_type"};
			MatrixCursor cursor = new MatrixCursor(projection);
			Object[] row = new Object[projection.length];
			native_query_file_info(path, projection, row);
			cursor.addRow(row);
			return cursor;
		} else {
			return null;
		}
	}

	/* AOSP ContentResolver: the SQL-style forms go through the Bundle form, so a provider that only knows that one
	 * (DocumentsProvider: "Pre-Android-O query format not supported.") answers them */
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		return query(uri, projection, selection, selectionArgs, sortOrder, null);
	}

	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder, CancellationSignal cancellationSignal) {
		Bundle queryArgs = createSqlQueryBundle(selection, selectionArgs, sortOrder);
		return query(uri, projection, queryArgs, cancellationSignal);
	}

	/* AOSP ContentResolver: the SQL-style calls go through the Bundle forms, which reach the provider's
	 * Bundle methods (their defaults fall back to the SQL-style ones). Without a provider ATL keeps its
	 * answer (0 / null) instead of AOSP's IllegalArgumentException("Unknown URL"). */
	public final int delete(Uri uri, String selection, String[] selectionArgs) {
		return delete(uri, createSqlQueryBundle(selection, selectionArgs));
	}

	public final int delete(Uri uri, Bundle extras) {
		Objects.requireNonNull(uri, "url");
		ContentProvider provider = ContentProvider.atl_get_content_provider(uri.getAuthority());
		if (provider != null)
			return provider.delete(uri, extras);
		else
			return 0;
	}

	public final Uri insert(Uri uri, ContentValues values) {
		return insert(uri, values, null);
	}

	public final Uri insert(Uri uri, ContentValues values, Bundle extras) {
		Objects.requireNonNull(uri, "url");
		ContentProvider provider = ContentProvider.atl_get_content_provider(uri.getAuthority());
		if (provider != null)
			return provider.insert(uri, values, extras);
		else
			return null;
	}

	public final int bulkInsert(Uri uri, ContentValues[] values) {
		Objects.requireNonNull(uri, "url");
		Objects.requireNonNull(values, "values");
		ContentProvider provider = ContentProvider.atl_get_content_provider(uri.getAuthority());
		if (provider != null)
			return provider.bulkInsert(uri, values);
		else
			return 0;
	}

	/* AOSP ContentResolver.loadThumbnail(Uri, Size, CancellationSignal) and the static
	 * loadThumbnail(ContentInterface, ...) it calls (ContentInterface is this resolver here): the provider's
	 * typed "image/*" file for EXTRA_SIZE, decoded and sampled down, then turned by EXTRA_ORIENTATION */
	public Bitmap loadThumbnail(Uri uri, Size size, CancellationSignal signal) throws IOException {
		Objects.requireNonNull(uri);
		Objects.requireNonNull(size);

		final Bundle opts = new Bundle();
		opts.putParcelable(EXTRA_SIZE, new Point(size.getWidth(), size.getHeight()));
		final int[] orientation = new int[1];

		Bitmap bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(() -> {
			final AssetFileDescriptor afd = openTypedAssetFile(uri, "image/*", opts, signal);
			final Bundle extras = afd.getExtras();
			orientation[0] = (extras != null) ? extras.getInt(android.provider.DocumentsContract.EXTRA_ORIENTATION, 0) : 0;
			return afd;
		}), (ImageDecoder decoder, ImageDecoder.ImageInfo info, ImageDecoder.Source source) -> {
			decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);

			// One last-ditch check to see if we've been canceled.
			if (signal != null) signal.throwIfCanceled();

			// We requested a rough thumbnail size, but the remote size may have
			// returned something giant, so defensively scale down as needed.
			final int widthSample = info.getSize().getWidth() / size.getWidth();
			final int heightSample = info.getSize().getHeight() / size.getHeight();
			final int sample = Math.max(widthSample, heightSample);
			if (sample > 1) {
				decoder.setTargetSampleSize(sample);
			}
		});

		if (orientation[0] != 0) {
			final int width = bitmap.getWidth();
			final int height = bitmap.getHeight();

			final Matrix m = new Matrix();
			m.setRotate(orientation[0], width / 2, height / 2);
			bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, m, false);
		}

		return bitmap;
	}

	/* AOSP ContentResolver.createSqlQueryBundle */
	public static Bundle createSqlQueryBundle(String selection, String[] selectionArgs) {
		return createSqlQueryBundle(selection, selectionArgs, null);
	}

	public static Bundle createSqlQueryBundle(String selection, String[] selectionArgs, String sortOrder) {
		if (selection == null && selectionArgs == null && sortOrder == null) {
			return null;
		}

		Bundle queryArgs = new Bundle();
		if (selection != null) {
			queryArgs.putString(QUERY_ARG_SQL_SELECTION, selection);
		}
		if (selectionArgs != null) {
			queryArgs.putStringArray(QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs);
		}
		if (sortOrder != null) {
			queryArgs.putString(QUERY_ARG_SQL_SORT_ORDER, sortOrder);
		}
		return queryArgs;
	}

	/* AOSP ContentResolver.getStreamTypes(): the provider's answer, null without a provider */
	public String[] getStreamTypes(Uri url, String mimeTypeFilter) {
		Objects.requireNonNull(url, "url");
		Objects.requireNonNull(mimeTypeFilter, "mimeTypeFilter");
		ContentProvider provider = ContentProvider.atl_get_content_provider(url.getAuthority());
		if (provider == null)
			return null;
		return provider.getStreamTypes(url, mimeTypeFilter);
	}

	public String getType(Uri uri) {
		ContentProvider provider = ContentProvider.atl_get_content_provider(uri.getAuthority());
		if (provider != null)
			return provider.getType(uri);
		else
			return null;
	}

	public final int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		return update(uri, values, createSqlQueryBundle(selection, selectionArgs));
	}

	public final int update(Uri uri, ContentValues values, Bundle extras) {
		Objects.requireNonNull(uri, "uri");
		ContentProvider provider = ContentProvider.atl_get_content_provider(uri.getAuthority());
		if (provider != null)
			return provider.update(uri, values, extras);
		else
			return 0;
	}

	public final Bundle call(Uri uri, String method, String arg, Bundle extras) {
		return call(uri.getAuthority(), method, arg, extras);
	}

	public final Bundle call(String authority, String method, String arg, Bundle extras) {
		ContentProvider provider = ContentProvider.atl_get_content_provider(authority);
		if (provider == null)
			throw new IllegalArgumentException("Unknown authority " + authority);
		return provider.call(authority, method, arg, extras);
	}

	public ContentProviderResult[] applyBatch(String authority, ArrayList<ContentProviderOperation> operations) throws OperationApplicationException {
		ContentProvider provider = ContentProvider.atl_get_content_provider(authority);
		if (provider == null)
			throw new IllegalArgumentException("Unknown authority " + authority);
		return provider.applyBatch(authority, operations);
	}

	/* AOSP ContentResolver.take/releasePersistableUriPermission(), getPersistedUriPermissions(): the grants an app keeps
	 * across restarts (UriGrantsManagerService writes them to urigrants.xml). There is no grant to check here (the file
	 * chooser and the intents that bring a Uri are the grants of an ATL app), so a call takes what the flags say;
	 * they are kept in the shared preferences "atl_uri_permissions" of the package: uri -> "flags,time". */
	private static final int PERSISTABLE_MODE_FLAGS = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;

	private android.content.SharedPreferences persistedUriPermissions() {
		return mContext.getSharedPreferences("atl_uri_permissions", Context.MODE_PRIVATE);
	}

	public void takePersistableUriPermission(Uri uri, int modeFlags) {
		if (uri == null)
			throw new NullPointerException("uri");
		takePersistableUriPermission(uri.toString(), modeFlags);
	}

	private synchronized void takePersistableUriPermission(String uri, int modeFlags) {
		modeFlags &= PERSISTABLE_MODE_FLAGS;
		if (modeFlags == 0)
			throw new IllegalArgumentException("Must specify a read or write mode flag");
		android.content.SharedPreferences prefs = persistedUriPermissions();
		String old = prefs.getString(uri, null);
		int flags = old != null ? Integer.parseInt(old.substring(0, old.indexOf(','))) : 0;
		long time = old != null ? Long.parseLong(old.substring(old.indexOf(',') + 1)) : System.currentTimeMillis();
		prefs.edit().putString(uri, (flags | modeFlags) + "," + time).apply();
	}

	public synchronized void releasePersistableUriPermission(Uri uri, int modeFlags) {
		if (uri == null)
			throw new NullPointerException("uri");
		modeFlags &= PERSISTABLE_MODE_FLAGS;
		android.content.SharedPreferences prefs = persistedUriPermissions();
		String old = prefs.getString(uri.toString(), null);
		if (old == null)
			return;
		int flags = Integer.parseInt(old.substring(0, old.indexOf(','))) & ~modeFlags;
		android.content.SharedPreferences.Editor edit = prefs.edit();
		if (flags == 0)
			edit.remove(uri.toString());
		else
			edit.putString(uri.toString(), flags + old.substring(old.indexOf(',')));
		edit.apply();
	}

	public synchronized List<UriPermission> getPersistedUriPermissions() {
		List<UriPermission> result = new ArrayList<>();
		for (java.util.Map.Entry<String, ?> e : persistedUriPermissions().getAll().entrySet()) {
			String v = String.valueOf(e.getValue());
			result.add(new UriPermission(Uri.parse(e.getKey()), Integer.parseInt(v.substring(0, v.indexOf(','))), Long.parseLong(v.substring(v.indexOf(',') + 1))));
		}
		return result;
	}

	public final ContentProviderClient acquireContentProviderClient(Uri uri) {
		return acquireContentProviderClient(uri.getAuthority());
	}

	public final ContentProviderClient acquireContentProviderClient(String name) {
		ContentProvider provider = ContentProvider.atl_get_content_provider(name);
		return provider != null ? new ContentProviderClient(provider, name) : null;
	}

	public final ContentProviderClient acquireUnstableContentProviderClient(Uri uri) {
		return acquireContentProviderClient(uri);
	}

	public final ContentProviderClient acquireUnstableContentProviderClient(String name) {
		return acquireContentProviderClient(name);
	}

	public static void requestSync(final Account account, final String authority, final Bundle extras) {
		// bind the app's sync adapter service and run the sync on a separate thread, like the SyncManager would
		ATLLoadedApp app = ATLLoadedApp.getPrimaryApplication();
		Intent intent = new Intent("android.content.SyncAdapter");
		intent.setPackage(app.pkg.packageName);
		ComponentName component = app.startOrBindService(intent, new ServiceConnection() {
			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
				if (!(service instanceof AbstractThreadedSyncAdapter.SyncAdapterBinder)) {
					Log.w("ContentResolver", "requestSync: sync adapter service returned " + service);
					return;
				}
				final AbstractThreadedSyncAdapter adapter = ((AbstractThreadedSyncAdapter.SyncAdapterBinder)service).getSyncAdapter();
				new Thread(new Runnable() {
					@Override
					public void run() {
						ContentProvider provider = ContentProvider.atl_get_content_provider(authority);
						SyncResult syncResult = new SyncResult();
						try {
							adapter.onPerformSync(account, extras != null ? extras : new Bundle(), authority,
							                      provider != null ? new ContentProviderClient(provider, authority) : null, syncResult);
							Log.i("ContentResolver", "requestSync: sync for " + authority + " finished: " + syncResult);
						} catch (Throwable t) {
							Log.e("ContentResolver", "requestSync: sync for " + authority + " failed", t);
						}
					}
				}, "SyncAdapterThread").start();
			}
		});
		if (component == null)
			Log.w("ContentResolver", "requestSync: no sync adapter service found in " + app.pkg.packageName);
	}

	public static void cancelSync(Account account, String authority) {
	}

	/* AOSP ContentService.cancelRequest(): a periodic request is removed, a one-off request cancels the sync of the
	 * account/authority */
	public static void cancelSync(SyncRequest request) {
		if (request == null)
			throw new IllegalArgumentException("request cannot be null");
		if (request.isPeriodic())
			removePeriodicSync(request.getAccount(), request.getProvider(), request.getBundle());
		else
			cancelSync(request.getAccount(), request.getProvider());
	}

	/* AOSP ContentService.sync(SyncRequest): a periodic request is added as periodic sync, others are requested once */
	public static void requestSync(SyncRequest request) {
		if (request.isPeriodic())
			addPeriodicSync(request.getAccount(), request.getProvider(), request.getBundle(), request.getSyncRunTime());
		else
			requestSync(request.getAccount(), request.getProvider(), request.getBundle());
	}

	public void cancelSync(Uri uri) {
		cancelSync(null /* all accounts */, uri != null ? uri.getAuthority() : null);
	}

	public static void setMasterSyncAutomatically(boolean sync) {
	}

	public static boolean getMasterSyncAutomatically() {
		return true;
	}

	public static int getIsSyncable(Account account, String authority) {
		return 1;
	}

	public static boolean getSyncAutomatically(Account account, String authority) {
		return false;
	}

	public static void setSyncAutomatically(Account account, String authority, boolean sync) {
	}

	public static void addPeriodicSync(Account account, String authority, Bundle extras, long pollFrequency) {
	}

	public static void setIsSyncable(Account account, String authority, int syncable) {
	}

	public static boolean isSyncActive(Account account, String authority) {
		return false;
	}

	/* AOSP ContentResolver.validateSyncExtrasBundle() */
	public static void validateSyncExtrasBundle(Bundle extras) {
		try {
			for (String key : extras.keySet()) {
				Object value = extras.get(key);
				if (value == null)
					continue;
				if (value instanceof Long)
					continue;
				if (value instanceof Integer)
					continue;
				if (value instanceof Boolean)
					continue;
				if (value instanceof Float)
					continue;
				if (value instanceof Double)
					continue;
				if (value instanceof String)
					continue;
				if (value instanceof Account)
					continue;
				throw new IllegalArgumentException("unexpected value type: " + value.getClass().getName());
			}
		} catch (IllegalArgumentException e) {
			throw e;
		} catch (RuntimeException exc) {
			throw new IllegalArgumentException("error unparceling Bundle", exc);
		}
	}

	/* AOSP ContentResolver.removePeriodicSync(): the ContentService has none (addPeriodicSync above keeps nothing) */
	public static void removePeriodicSync(Account account, String authority, Bundle extras) {
		validateSyncExtrasBundle(extras);
	}

	/* AOSP getPeriodicSyncs(), getCurrentSyncs(), getSyncAdapterTypes(), isSyncPending(): answers of a ContentService
	 * that runs no sync: nothing periodic, nothing running, no adapter registered with it, nothing pending */
	public static List<PeriodicSync> getPeriodicSyncs(Account account, String authority) {
		return new ArrayList<PeriodicSync>();
	}

	public static List<SyncInfo> getCurrentSyncs() {
		return new ArrayList<SyncInfo>();
	}

	public static SyncAdapterType[] getSyncAdapterTypes() {
		return new SyncAdapterType[0];
	}

	public static boolean isSyncPending(Account account, String authority) {
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

	/* AOSP ContentResolver sync extras and the checks SyncRequest.Builder uses (@hide helpers) */
	public static final String SYNC_EXTRAS_FORCE = "force";
	public static final String SYNC_EXTRAS_MANUAL = "force";
	public static final String SYNC_EXTRAS_INITIALIZE = "initialize";
	public static final String SYNC_EXTRAS_EXPEDITED = "expedited";
	public static final String SYNC_EXTRAS_REQUIRE_CHARGING = "require_charging";
	public static final String SYNC_EXTRAS_SCHEDULE_AS_EXPEDITED_JOB = "schedule_as_expedited_job";
	public static final String SYNC_EXTRAS_IGNORE_BACKOFF = "ignore_backoff";
	public static final String SYNC_EXTRAS_DO_NOT_RETRY = "do_not_retry";
	public static final String SYNC_EXTRAS_DISALLOW_METERED = "allow_metered";

	public static boolean hasInvalidScheduleAsEjExtras(Bundle extras) {
		return extras.getBoolean(ContentResolver.SYNC_EXTRAS_REQUIRE_CHARGING)
			|| extras.getBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED);
	}

	public static boolean invalidPeriodicExtras(Bundle extras) {
		return extras.getBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, false)
			|| extras.getBoolean(ContentResolver.SYNC_EXTRAS_DO_NOT_RETRY, false)
			|| extras.getBoolean(ContentResolver.SYNC_EXTRAS_IGNORE_BACKOFF, false)
			|| extras.getBoolean(ContentResolver.SYNC_EXTRAS_IGNORE_SETTINGS, false)
			|| extras.getBoolean(ContentResolver.SYNC_EXTRAS_INITIALIZE, false)
			|| extras.getBoolean(ContentResolver.SYNC_EXTRAS_FORCE, false)
			|| extras.getBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, false)
			|| extras.getBoolean(ContentResolver.SYNC_EXTRAS_SCHEDULE_AS_EXPEDITED_JOB, false);
	}
}
