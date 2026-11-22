package android.content;

import android.atl.ATLLoadedApp;
import android.atl.ATLLoadedAppManager;
import android.atl.ATLMediaContentProvider;
import android.atl.ATLProcess;
import android.atl.ATLProvider;
import android.atl.ATLRemoteContentProvider;
import android.atl.annotation.Export;
import android.content.pm.PackageManager;
import android.content.pm.PackageParser;
import android.content.pm.ProviderInfo;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

public abstract class ContentProvider implements ComponentCallbacks2 {
	/* AOSP ContentProvider (ComponentCallbacks2): the callbacks do nothing unless the provider overrides them */
	public void onConfigurationChanged(android.content.res.Configuration newConfig) {}

	public void onLowMemory() {}

	public void onTrimMemory(int level) {}


	static final HashMap<String, ATLProvider> atl_providers = new HashMap<>();
	/* the providers createContentProviders() installed (also those that failed to load) */
	private static final java.util.HashSet<PackageParser.Provider> installed_at_start = new java.util.HashSet<>();

	@Export
	static void createContentProviders() {
		atl_providers.put("media", new ATLProvider(new ATLMediaContentProvider()));
		ATLLoadedApp primary = ATLLoadedApp.getPrimaryApplication();
		for (PackageParser.Provider provider_parsed : primary.pkg.providers) {
			/* like ComputerEngine.queryContentProviders() for AMS.generateApplicationProvidersLocked(): a disabled
			 * provider is not installed (android:enabled="false" in the manifest, unless the app enabled it) */
			if (!PackageManager.atlIsComponentEnabled(provider_parsed.info, 0))
				continue;
			String process_name = provider_parsed.info.processName;
			if (ATLProcess.isSecondary()) {
				/* like AMS.generateApplicationProvidersLocked(): a process gets the providers that run in it */
				if (!ATLProcess.myProcessName().equals(process_name))
					continue;
			} else if (process_name != null && !process_name.equals(ATLProcess.myProcessName())) {
				/* it runs in its own process, which atl_get_content_provider() starts on first use */
				continue;
			}
			installed_at_start.add(provider_parsed);
			atl_providers.put(provider_parsed.info.authority,
			                  new ATLProvider(primary, provider_parsed));
		}
		// getContentProvider() initializes the content providers,
		// we also opportunistically remove the ones that failed to load
		atl_providers.values().removeIf(new Predicate<ATLProvider>() {
			@Override
			public boolean test(ATLProvider atlProvider) {
				return atlProvider.getContentProvider() == null;
			}
		});
	}

	/** @hide a provider that runs in this process, for the app's other processes */
	public static ContentProvider atl_get_local_content_provider(String authority) {
		ATLProvider atlProvider;
		synchronized (atl_providers) {
			atlProvider = atl_providers.get(authority);
		}
		return atlProvider == null ? null : atlProvider.getContentProvider();
	}

	/* an enabled provider of the app with this authority that runs in this process */
	private static PackageParser.Provider ownEnabledProvider(String authority) {
		for (PackageParser.Provider p : ATLLoadedApp.getPrimaryApplication().pkg.providers) {
			if (p.info.authority == null || !java.util.Arrays.asList(p.info.authority.split(";")).contains(authority))
				continue;
			String process_name = p.info.processName != null ? p.info.processName : ATLProcess.mainProcessName();
			if (process_name.equals(ATLProcess.myProcessName()) && !installed_at_start.contains(p)
			    && PackageManager.atlIsComponentEnabled(p.info, 0))
				return p;
		}
		return null;
	}

	/* the process an own provider of the app runs in, if that is another process */
	private static String otherProviderProcess(String authority) {
		for (PackageParser.Provider p : ATLLoadedApp.getPrimaryApplication().pkg.providers) {
			if (p.info.authority == null || !java.util.Arrays.asList(p.info.authority.split(";")).contains(authority))
				continue;
			String process_name = p.info.processName != null ? p.info.processName : ATLProcess.mainProcessName();
			return process_name.equals(ATLProcess.myProcessName()) ? null : process_name;
		}
		return null;
	}

	static ContentProvider atl_get_content_provider(String authority) {
		ATLProvider atlProvider;
		synchronized (atl_providers) {
			atlProvider = atl_providers.get(authority);
			String process_name = atlProvider == null && authority != null ? otherProviderProcess(authority) : null;
			if (process_name != null) {
				// a provider of the app in another of its processes
				atlProvider = new ATLProvider(new ATLRemoteContentProvider(process_name, authority));
				atl_providers.put(authority, atlProvider);
			}
			if (atlProvider == null && authority != null) {
				// an own provider of this process that was disabled at start and has been enabled since:
				// AMS.getContentProviderImpl() resolves it now, and the client installs it (canRunHere)
				PackageParser.Provider own = ownEnabledProvider(authority);
				if (own != null) {
					atlProvider = new ATLProvider(ATLLoadedApp.getPrimaryApplication(), own);
					atl_providers.put(authority, atlProvider);
				}
			}
			if (atlProvider == null && authority != null) {
				// a provider of another package (Play Services), loaded into this process on first use
				PackageParser.Provider provider = ATLLoadedAppManager.findForeignProvider(authority);
				ATLLoadedApp app = provider == null ? null : ATLLoadedAppManager.getAppFromPackageName(provider.owner.packageName);
				if (app == null) {
					return null;
				}
				atlProvider = new ATLProvider(app, provider);
				atl_providers.put(authority, atlProvider);
			}
		}
		return atlProvider == null ? null : atlProvider.getContentProvider();
	}

	public boolean onCreate() { return false; }

	public Bundle call(String method, String arg, Bundle extras) {
		return null;
	}

	public Bundle call(String authority, String method, String arg, Bundle extras) {
		return call(method, arg, extras);
	}

	private Context mContext;

	/* as AOSP, the context given to attachInfo(); ATL's own providers are not attached and run as the app */
	public Context getContext() {
		return mContext != null ? mContext : ATLLoadedApp.getPrimaryApplication().getApplication();
	}

	public ContentProviderResult[] applyBatch(String authority, ArrayList<ContentProviderOperation> operations) throws OperationApplicationException {
		return applyBatch(operations);
	}

	public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> operations) throws OperationApplicationException {
		final int numOperations = operations.size();
		final ContentProviderResult[] results = new ContentProviderResult[numOperations];
		for (int i = 0; i < numOperations; i++) {
			results[i] = operations.get(i).apply(this, results, i);
		}
		return results;
	}

	public abstract Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder);

	/* as AOSP: providers that can cancel a query override this one */
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder, CancellationSignal cancellationSignal) {
		return query(uri, projection, selection, selectionArgs, sortOrder);
	}

	public abstract Uri insert(Uri uri, ContentValues values);

	public abstract int update(Uri uri, ContentValues values, String selection, String[] selectionArgs);

	public abstract int delete(Uri uri, String selection, String[] selectionArgs);

	/* AOSP ContentProvider.insert(Uri, ContentValues, Bundle), bulkInsert, delete(Uri, Bundle) and
	 * update(Uri, ContentValues, Bundle): the defaults that fall back to the SQL-style methods */
	public Uri insert(Uri uri, ContentValues values, Bundle extras) {
		return insert(uri, values);
	}

	public int bulkInsert(Uri uri, ContentValues[] values) {
		int numValues = values.length;
		for (int i = 0; i < numValues; i++) {
			insert(uri, values[i]);
		}
		return numValues;
	}

	public int delete(Uri uri, Bundle extras) {
		extras = (extras != null) ? extras : Bundle.EMPTY;
		return delete(uri,
		    extras.getString(ContentResolver.QUERY_ARG_SQL_SELECTION),
		    extras.getStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS));
	}

	public int update(Uri uri, ContentValues values, Bundle extras) {
		extras = (extras != null) ? extras : Bundle.EMPTY;
		return update(uri, values,
		    extras.getString(ContentResolver.QUERY_ARG_SQL_SELECTION),
		    extras.getStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS));
	}

	public abstract String getType(Uri uri);

	/* AOSP ContentProvider.openFile(Uri, String): not abstract, a provider without files throws */
	public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
		throw new FileNotFoundException("No files supported by provider at " + uri);
	}

	public AssetFileDescriptor openAssetFile(Uri uri, String mode) throws FileNotFoundException {
		ParcelFileDescriptor fd = openFile(uri, mode);
		return fd != null ? new AssetFileDescriptor(fd, 0, -1) : null;
	}

	public void attachInfo(Context context, ProviderInfo provider) {
		// only allow it to be set once, as AOSP
		if (mContext == null) {
			mContext = context;
		}
	}

	/* the newer forms of query(), openFile() and openAssetFile() (API 16-26) fall back to the older ones, as in AOSP */
	public Cursor query(Uri uri, String[] projection, Bundle queryArgs, CancellationSignal cancellationSignal) {
		queryArgs = queryArgs != null ? queryArgs : Bundle.EMPTY;
		String sortClause = queryArgs.getString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER);
		if (sortClause == null && queryArgs.containsKey(ContentResolver.QUERY_ARG_SORT_COLUMNS)) {
			sortClause = ContentResolver.createSqlSortClause(queryArgs);
		}
		return query(uri, projection, queryArgs.getString(ContentResolver.QUERY_ARG_SQL_SELECTION),
		             queryArgs.getStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS), sortClause, cancellationSignal);
	}

	public ParcelFileDescriptor openFile(Uri uri, String mode, CancellationSignal signal) throws FileNotFoundException {
		return openFile(uri, mode);
	}

	public AssetFileDescriptor openAssetFile(Uri uri, String mode, CancellationSignal signal) throws FileNotFoundException {
		return openAssetFile(uri, mode);
	}

	public String[] getStreamTypes(Uri uri, String mimeTypeFilter) {
		return null;
	}

	/* AOSP ContentProvider.isTemporary(), shutdown(), openFileHelper() */
	protected boolean isTemporary() {
		return false;
	}

	public void shutdown() {
		Log.w("ContentProvider", "implement ContentProvider shutdown() to make sure all database connections are gracefully shutdown");
	}

	protected final ParcelFileDescriptor openFileHelper(Uri uri, String mode) throws FileNotFoundException {
		Cursor c = query(uri, new String[] {"_data"}, null, null, null);
		int count = (c != null) ? c.getCount() : 0;
		if (count != 1) {
			// If there is not exactly one result, throw an appropriate exception.
			if (c != null) {
				c.close();
			}
			if (count == 0) {
				throw new FileNotFoundException("No entry for " + uri);
			}
			throw new FileNotFoundException("Multiple items at " + uri);
		}

		c.moveToFirst();
		int i = c.getColumnIndex("_data");
		String path = (i >= 0 ? c.getString(i) : null);
		c.close();
		if (path == null) {
			throw new FileNotFoundException("Column _data not found.");
		}

		int modeBits = ParcelFileDescriptor.parseMode(mode);
		return ParcelFileDescriptor.open(new java.io.File(path), modeBits);
	}

	public AssetFileDescriptor openTypedAssetFile(Uri uri, String mimeTypeFilter, Bundle opts) throws FileNotFoundException {
		if ("*/*".equals(mimeTypeFilter)) {
			// If they can take anything, the untyped open call is good enough.
			return openAssetFile(uri, "r");
		}
		String baseType = getType(uri);
		if (baseType != null && ClipDescription.compareMimeTypes(baseType, mimeTypeFilter)) {
			// Use old untyped open call if this provider has a type for this
			// URI and it matches the request.
			return openAssetFile(uri, "r");
		}
		throw new FileNotFoundException("Can't open " + uri + " as type " + mimeTypeFilter);
	}

	public AssetFileDescriptor openTypedAssetFile(Uri uri, String mimeTypeFilter, Bundle opts, CancellationSignal signal) throws FileNotFoundException {
		return openTypedAssetFile(uri, mimeTypeFilter, opts);
	}

	/* API 34: getType() for callers without read permission */
	public String getTypeAnonymous(Uri uri) {
		return getType(uri);
	}

	public Uri canonicalize(Uri url) {
		return null;
	}

	public Uri uncanonicalize(Uri url) {
		return url;
	}

	/**
	 * ATL: the app is the only caller and has one user, so an incoming URI stays as it is.
	 * @hide
	 */
	public Uri validateIncomingUri(Uri uri) throws SecurityException {
		return uri;
	}

	/**
	 * ATL: the permission checks of a call from the app itself, which may use its own provider
	 * (AOSP passes the AttributionSource of the binder caller).
	 * @hide
	 */
	protected int enforceReadPermissionInner(Uri uri) throws SecurityException {
		return 0; // AppOpsManager.MODE_ALLOWED
	}

	/** @hide */
	protected int enforceWritePermissionInner(Uri uri) throws SecurityException {
		return 0; // AppOpsManager.MODE_ALLOWED
	}

	public String getCallingPackage() {
		return ATLLoadedApp.getPrimaryApplication().pkg.packageName;
	}

	/* AOSP ContentProvider.PipeDataWriter and openPipeHelper() */
	public interface PipeDataWriter<T> {
		public void writeDataToPipe(ParcelFileDescriptor output, Uri uri, String mimeType, Bundle opts, T args);
	}

	public <T> ParcelFileDescriptor openPipeHelper(final Uri uri, final String mimeType, final Bundle opts, final T args,
	                                               final PipeDataWriter<T> func) throws FileNotFoundException {
		try {
			final ParcelFileDescriptor[] fds = ParcelFileDescriptor.createPipe();

			AsyncTask<Object, Object, Object> task = new AsyncTask<Object, Object, Object>() {
				@Override
				protected Object doInBackground(Object... params) {
					func.writeDataToPipe(fds[1], uri, mimeType, opts, args);
					try {
						fds[1].close();
					} catch (IOException e) {
						Log.w("ContentProvider", "Failure closing pipe", e);
					}
					return null;
				}
			};
			task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Object[])null);

			return fds[0];
		} catch (IOException e) {
			throw new FileNotFoundException("failure making pipe");
		}
	}
}
