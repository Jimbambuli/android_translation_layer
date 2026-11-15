package android.content;

import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Objects;

public class ContentProviderClient implements AutoCloseable {
	private final ContentProvider contentProvider;
	private final String mAuthority;

	ContentProviderClient(ContentProvider contentProvider, String authority) {
		this.contentProvider = contentProvider;
		this.mAuthority = authority;
	}

	public ContentProvider getLocalContentProvider() {
		return contentProvider;
	}

	public Cursor query(Uri url, String[] projection, String selection, String[] selectionArgs, String sortOrder) throws RemoteException {
		return contentProvider.query(url, projection, selection, selectionArgs, sortOrder);
	}

	public Cursor query(Uri url, String[] projection, String selection, String[] selectionArgs, String sortOrder, CancellationSignal cancellationSignal) throws RemoteException {
		return contentProvider.query(url, projection, selection, selectionArgs, sortOrder, cancellationSignal);
	}

	public Bundle call(String method, String arg, Bundle extras) throws RemoteException {
		return contentProvider.call(method, arg, extras);
	}

	public Bundle call(String authority, String method, String arg, Bundle extras) throws RemoteException {
		return contentProvider.call(authority, method, arg, extras);
	}

	public String getType(Uri url) throws RemoteException {
		return contentProvider.getType(url);
	}

	/* AOSP ContentProviderClient: insert/update/delete use the provider's Bundle forms (as
	 * ContentResolver does), bulkInsert and applyBatch go straight to the provider */
	public Uri insert(Uri url, ContentValues initialValues) throws RemoteException {
		return insert(url, initialValues, null);
	}

	public Uri insert(Uri url, ContentValues initialValues, Bundle extras) throws RemoteException {
		Objects.requireNonNull(url, "url");
		return contentProvider.insert(url, initialValues, extras);
	}

	public int bulkInsert(Uri url, ContentValues[] initialValues) throws RemoteException {
		Objects.requireNonNull(url, "url");
		Objects.requireNonNull(initialValues, "initialValues");
		return contentProvider.bulkInsert(url, initialValues);
	}

	public int delete(Uri url, String selection, String[] selectionArgs) throws RemoteException {
		return delete(url, ContentResolver.createSqlQueryBundle(selection, selectionArgs));
	}

	public int delete(Uri url, Bundle extras) throws RemoteException {
		Objects.requireNonNull(url, "url");
		return contentProvider.delete(url, extras);
	}

	public int update(Uri url, ContentValues values, String selection, String[] selectionArgs) throws RemoteException {
		return update(url, values, ContentResolver.createSqlQueryBundle(selection, selectionArgs));
	}

	public int update(Uri url, ContentValues values, Bundle extras) throws RemoteException {
		Objects.requireNonNull(url, "url");
		return contentProvider.update(url, values, extras);
	}

	public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> operations) throws RemoteException, OperationApplicationException {
		return applyBatch(mAuthority, operations);
	}

	public ContentProviderResult[] applyBatch(String authority, ArrayList<ContentProviderOperation> operations) throws RemoteException, OperationApplicationException {
		Objects.requireNonNull(operations, "operations");
		return contentProvider.applyBatch(authority, operations);
	}

	public ParcelFileDescriptor openFile(Uri url, String mode) throws RemoteException, FileNotFoundException {
		return contentProvider.openFile(url, mode);
	}

	public AssetFileDescriptor openAssetFile(Uri url, String mode) throws RemoteException, FileNotFoundException {
		return contentProvider.openAssetFile(url, mode);
	}

	public InputStream openInputStream(Uri url) throws FileNotFoundException, RemoteException {
		ParcelFileDescriptor fd = openFile(url, "r");
		return fd != null ? new ParcelFileDescriptor.AutoCloseInputStream(fd) : null;
	}

	public OutputStream openOutputStream(Uri url) throws FileNotFoundException, RemoteException {
		ParcelFileDescriptor fd = openFile(url, "w");
		return fd != null ? new ParcelFileDescriptor.AutoCloseOutputStream(fd) : null;
	}

	public boolean release() {
		return true;
	}

	@Override
	public void close() {
		release();
	}
}
