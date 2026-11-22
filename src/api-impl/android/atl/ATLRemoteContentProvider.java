package android.atl;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.DeadObjectException;
import android.os.IBinder;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Slog;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

/**
 * A content provider of the app that runs in another of its processes (android:process), as
 * ContentProviderProxy: each call goes over the Binder of the provider's Transport to that process, which runs
 * the provider. A cursor comes back as a copy of its rows (AOSP shares a CursorWindow instead).
 */
public final class ATLRemoteContentProvider extends ContentProvider {
	private static final String TAG = "ATLRemoteContentProvider";

	private static final int QUERY = IBinder.FIRST_CALL_TRANSACTION;
	private static final int GET_TYPE = IBinder.FIRST_CALL_TRANSACTION + 1;
	private static final int INSERT = IBinder.FIRST_CALL_TRANSACTION + 2;
	private static final int DELETE = IBinder.FIRST_CALL_TRANSACTION + 3;
	private static final int UPDATE = IBinder.FIRST_CALL_TRANSACTION + 4;
	private static final int CALL = IBinder.FIRST_CALL_TRANSACTION + 5;
	private static final int OPEN_ASSET_FILE = IBinder.FIRST_CALL_TRANSACTION + 6;

	private final String processName;
	private final String authority;
	private IBinder transport;

	public ATLRemoteContentProvider(String processName, String authority) {
		this.processName = processName;
		this.authority = authority;
	}

	/* ContentProvider.onCreate(): nothing to set up, the provider is ready */
	@Override
	public boolean onCreate() {
		return true;
	}

	private abstract static class Call {
		abstract void write(Parcel data);
	}

	private synchronized IBinder transport(boolean fresh) throws RemoteException {
		if (fresh || transport == null)
			transport = ATLProcess.acquireProvider(processName, authority);
		if (transport == null)
			throw new RemoteException("no provider " + authority + " in process " + processName);
		return transport;
	}

	/* the reply of the call; like an unstable provider, a provider whose process died is acquired again once */
	private Parcel transact(int code, Call call) throws RemoteException {
		for (int attempt = 0;; attempt++) {
			Parcel data = Parcel.obtain();
			Parcel reply = Parcel.obtain();
			call.write(data);
			try {
				transport(attempt > 0).transact(code, data, reply, 0);
				reply.readException();
				return reply;
			} catch (DeadObjectException e) {
				if (attempt > 0)
					throw e;
				Slog.w(TAG, "process " + processName + " of provider " + authority + " died, acquiring it again");
			}
		}
	}

	@Override
	public Cursor query(final Uri uri, final String[] projection, final Bundle queryArgs, CancellationSignal cancellationSignal) {
		try {
			Parcel reply = transact(QUERY, new Call() {
				@Override
				void write(Parcel data) {
					Uri.writeToParcel(data, uri);
					data.writeStringArray(projection);
					data.writeBundle(queryArgs);
				}
			});
			return readCursor(reply);
		} catch (RemoteException e) {
			Slog.w(TAG, "query " + uri + ": " + e);
			return null;
		}
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		/* ContentResolver.createSqlQueryBundle(): the transport only knows the Bundle form */
		Bundle queryArgs = new Bundle();
		if (selection != null)
			queryArgs.putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection);
		if (selectionArgs != null)
			queryArgs.putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs);
		if (sortOrder != null)
			queryArgs.putString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER, sortOrder);
		return query(uri, projection, queryArgs, null);
	}

	@Override
	public String getType(final Uri uri) {
		try {
			return transact(GET_TYPE, new Call() {
				@Override
				void write(Parcel data) {
					Uri.writeToParcel(data, uri);
				}
			}).readString();
		} catch (RemoteException e) {
			Slog.w(TAG, "getType " + uri + ": " + e);
			return null;
		}
	}

	@Override
	public Uri insert(final Uri uri, final ContentValues values) {
		try {
			return Uri.CREATOR.createFromParcel(transact(INSERT, new Call() {
				@Override
				void write(Parcel data) {
					Uri.writeToParcel(data, uri);
					writeValues(data, values);
				}
			}));
		} catch (RemoteException e) {
			Slog.w(TAG, "insert " + uri + ": " + e);
			return null;
		}
	}

	@Override
	public int delete(final Uri uri, final String selection, final String[] selectionArgs) {
		try {
			return transact(DELETE, new Call() {
				@Override
				void write(Parcel data) {
					Uri.writeToParcel(data, uri);
					data.writeString(selection);
					data.writeStringArray(selectionArgs);
				}
			}).readInt();
		} catch (RemoteException e) {
			Slog.w(TAG, "delete " + uri + ": " + e);
			return 0;
		}
	}

	@Override
	public int update(final Uri uri, final ContentValues values, final String selection, final String[] selectionArgs) {
		try {
			return transact(UPDATE, new Call() {
				@Override
				void write(Parcel data) {
					Uri.writeToParcel(data, uri);
					writeValues(data, values);
					data.writeString(selection);
					data.writeStringArray(selectionArgs);
				}
			}).readInt();
		} catch (RemoteException e) {
			Slog.w(TAG, "update " + uri + ": " + e);
			return 0;
		}
	}

	@Override
	public Bundle call(final String authority, final String method, final String arg, final Bundle extras) {
		try {
			return transact(CALL, new Call() {
				@Override
				void write(Parcel data) {
					data.writeString(authority);
					data.writeString(method);
					data.writeString(arg);
					data.writeBundle(extras);
				}
			}).readBundle();
		} catch (RemoteException e) {
			Slog.w(TAG, "call " + method + ": " + e);
			return null;
		}
	}

	@Override
	public Bundle call(String method, String arg, Bundle extras) {
		return call(authority, method, arg, extras);
	}

	@Override
	public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
		AssetFileDescriptor afd = openAssetFile(uri, mode);
		return afd != null ? afd.getParcelFileDescriptor() : null;
	}

	@Override
	public AssetFileDescriptor openAssetFile(final Uri uri, final String mode) throws FileNotFoundException {
		Parcel reply;
		try {
			reply = transact(OPEN_ASSET_FILE, new Call() {
				@Override
				void write(Parcel data) {
					Uri.writeToParcel(data, uri);
					data.writeString(mode);
				}
			});
		} catch (RemoteException e) {
			throw new FileNotFoundException("provider " + authority + " in process " + processName + ": " + e);
		}
		if (reply.readInt() == 0)
			throw new FileNotFoundException(reply.readString());
		if (reply.readInt() == 0)
			return null;
		ParcelFileDescriptor fd = reply.readFileDescriptor();
		long start = reply.readLong();
		long length = reply.readLong();
		return new AssetFileDescriptor(fd, start, length);
	}

	private static void writeValues(Parcel data, ContentValues values) {
		if (values == null) {
			data.writeInt(-1);
			return;
		}
		data.writeInt(values.size());
		for (Map.Entry<String, Object> e : values.valueSet()) {
			data.writeString(e.getKey());
			data.writeValue(e.getValue());
		}
	}

	private static ContentValues readValues(Parcel data) throws ReflectiveOperationException {
		int n = data.readInt();
		if (n < 0)
			return null;
		ContentValues values = new ContentValues(n);
		for (int i = 0; i < n; i++)
			values.putObject(data.readString(), data.readValue(null));
		return values;
	}

	/* the cursor as columns and rows of values (CursorWindow types: null, long, double, String, byte[]) */
	private static void writeCursor(Parcel reply, Cursor cursor) {
		if (cursor == null) {
			reply.writeInt(0);
			return;
		}
		reply.writeInt(1);
		try {
			String[] columns = cursor.getColumnNames();
			reply.writeStringArray(columns);
			reply.writeInt(cursor.getCount());
			cursor.moveToPosition(-1);
			while (cursor.moveToNext()) {
				for (int i = 0; i < columns.length; i++) {
					switch (cursor.getType(i)) {
						case Cursor.FIELD_TYPE_INTEGER:
							reply.writeValue(cursor.getLong(i));
							break;
						case Cursor.FIELD_TYPE_FLOAT:
							reply.writeValue(cursor.getDouble(i));
							break;
						case Cursor.FIELD_TYPE_STRING:
							reply.writeValue(cursor.getString(i));
							break;
						case Cursor.FIELD_TYPE_BLOB:
							reply.writeValue(cursor.getBlob(i));
							break;
						default:
							reply.writeValue(null);
					}
				}
			}
		} finally {
			cursor.close();
		}
	}

	private static Cursor readCursor(Parcel reply) throws RemoteException {
		if (reply.readInt() == 0)
			return null;
		String[] columns = reply.createStringArray();
		int count = reply.readInt();
		MatrixCursor cursor = new MatrixCursor(columns, count);
		try {
			for (int r = 0; r < count; r++) {
				Object[] row = new Object[columns.length];
				for (int i = 0; i < columns.length; i++)
					row[i] = reply.readValue(null);
				cursor.addRow(row);
			}
		} catch (ReflectiveOperationException e) {
			throw new RemoteException(e.toString());
		}
		return cursor;
	}

	/* ContentProvider.Transport: the provider of this process for the app's other processes */
	private static final HashMap<String, IBinder> transports = new HashMap<>();

	/** the Transport of a provider of this process, null if this process has none for the authority */
	static IBinder transportFor(String authority) {
		final ContentProvider provider = ContentProvider.atl_get_local_content_provider(authority);
		if (provider == null)
			return null;
		synchronized (transports) {
			IBinder transport = transports.get(authority);
			if (transport == null) {
				transport = new Binder() {
					@Override
					protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
						try {
							return execute(provider, code, data, reply) || super.onTransact(code, data, reply, flags);
						} catch (ReflectiveOperationException e) {
							throw new RemoteException(e.toString());
						}
					}
				};
				transports.put(authority, transport);
			}
			return transport;
		}
	}

	static boolean hasTransports() {
		synchronized (transports) {
			return !transports.isEmpty();
		}
	}

	private static boolean execute(ContentProvider provider, int code, Parcel data, Parcel reply) throws ReflectiveOperationException {
		switch (code) {
			case QUERY: {
				Uri uri = Uri.CREATOR.createFromParcel(data);
				String[] projection = data.createStringArray();
				Bundle queryArgs = data.readBundle();
				Cursor cursor = provider.query(uri, projection, queryArgs, null);
				reply.writeNoException();
				writeCursor(reply, cursor);
				return true;
			}
			case GET_TYPE: {
				String type = provider.getType(Uri.CREATOR.createFromParcel(data));
				reply.writeNoException();
				reply.writeString(type);
				return true;
			}
			case INSERT: {
				Uri uri = Uri.CREATOR.createFromParcel(data);
				Uri ret = provider.insert(uri, readValues(data));
				reply.writeNoException();
				Uri.writeToParcel(reply, ret);
				return true;
			}
			case DELETE: {
				Uri uri = Uri.CREATOR.createFromParcel(data);
				int n = provider.delete(uri, data.readString(), data.createStringArray());
				reply.writeNoException();
				reply.writeInt(n);
				return true;
			}
			case UPDATE: {
				Uri uri = Uri.CREATOR.createFromParcel(data);
				ContentValues values = readValues(data);
				int n = provider.update(uri, values, data.readString(), data.createStringArray());
				reply.writeNoException();
				reply.writeInt(n);
				return true;
			}
			case CALL: {
				String authority = data.readString();
				String method = data.readString();
				String arg = data.readString();
				Bundle ret = provider.call(authority, method, arg, data.readBundle());
				reply.writeNoException();
				reply.writeBundle(ret);
				return true;
			}
			case OPEN_ASSET_FILE: {
				Uri uri = Uri.CREATOR.createFromParcel(data);
				String mode = data.readString();
				AssetFileDescriptor afd;
				try {
					afd = provider.openAssetFile(uri, mode);
				} catch (FileNotFoundException e) {
					reply.writeNoException();
					reply.writeInt(0);
					reply.writeString(e.getMessage());
					return true;
				}
				java.io.FileDescriptor fd = null;
				if (afd != null) {
					/* like AssetFileDescriptor.writeToParcel(PARCELABLE_WRITE_RETURN_VALUE): the descriptor goes to
					 * the caller and is closed here (its duplicate once it is sent) */
					try {
						fd = android.system.Os.dup(afd.getParcelFileDescriptor().getFileDescriptor());
						afd.close();
					} catch (android.system.ErrnoException | java.io.IOException e) {
						throw new IllegalStateException(e);
					}
				}
				reply.writeNoException();
				reply.writeInt(1);
				reply.writeInt(afd != null ? 1 : 0);
				if (afd != null) {
					reply.atl_writeOwnedFileDescriptor(fd);
					reply.writeLong(afd.getStartOffset());
					reply.writeLong(afd.getDeclaredLength());
				}
				return true;
			}
		}
		return false;
	}
}
