package android.content;

import android.atl.ATLLoadedApp;
import android.atl.ATLLoadedAppManager;
import android.atl.ATLMediaContentProvider;
import android.atl.ATLProvider;
import android.atl.annotation.Export;
import android.content.pm.PackageParser;
import android.content.pm.ProviderInfo;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

public abstract class ContentProvider {

	static final HashMap<String, ATLProvider> atl_providers = new HashMap<>();

	@Export
	static void createContentProviders() {
		atl_providers.put("media", new ATLProvider(new ATLMediaContentProvider()));
		ATLLoadedApp primary = ATLLoadedApp.getPrimaryApplication();
		for (PackageParser.Provider provider_parsed : primary.pkg.providers) {
			String process_name = provider_parsed.info.processName;
			if (process_name != null && process_name.contains(":")) {
				/* NOTE: even if it doesn't contain `:`, if it's not null we probably
				 * need to check what it's requesting; `:` means it wants us to spawn
				 * a new process, which we currently don't support */
				System.out.println("not creating provider " + provider_parsed.className + ", it wants to be started in a new process (" + process_name + ")");
				continue;
			}
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

	static ContentProvider atl_get_content_provider(String authority) {
		ATLProvider atlProvider;
		synchronized (atl_providers) {
			atlProvider = atl_providers.get(authority);
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

	public abstract String getType(Uri uri);

	public abstract ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException;

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
}
