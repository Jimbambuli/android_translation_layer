package android.atl;

import android.content.Context;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.provider.DocumentsContract.Document;
import android.provider.DocumentsContract.Root;
import com.android.internal.content.storage.FileSystemProvider;
import java.io.File;
import java.io.FileNotFoundException;

/* The documents of the files the desktop hands to an app: the file dialog of ACTION_OPEN_DOCUMENT, GET_CONTENT,
 * CREATE_DOCUMENT and OPEN_DOCUMENT_TREE (Activity), dropped and pasted files. They are named by their path like
 * the "raw:" documents of AOSP's DownloadStorageProvider (packages/providers/DownloadProvider, RawDocumentsHelper),
 * which the FileSystemProvider it extends answers; this is that part of it, under its authority. The Downloads
 * root and its numbered documents come from DownloadManager's database, which ATL does not have. */
public class ATLRawDocumentsProvider extends FileSystemProvider {
	/* DownloadProvider's Constants.STORAGE_AUTHORITY */
	public static final String AUTHORITY = "com.android.providers.downloads.documents";

	/* RawDocumentsHelper.RAW_PREFIX */
	private static final String RAW_PREFIX = "raw:";

	private static final String[] DEFAULT_ROOT_PROJECTION = new String[] {
	    Root.COLUMN_ROOT_ID, Root.COLUMN_FLAGS, Root.COLUMN_ICON,
	    Root.COLUMN_TITLE, Root.COLUMN_DOCUMENT_ID, Root.COLUMN_QUERY_ARGS};

	private static final String[] DEFAULT_DOCUMENT_PROJECTION = new String[] {
	    Document.COLUMN_DOCUMENT_ID, Document.COLUMN_MIME_TYPE, Document.COLUMN_DISPLAY_NAME,
	    Document.COLUMN_SUMMARY, Document.COLUMN_LAST_MODIFIED, Document.COLUMN_FLAGS,
	    Document.COLUMN_SIZE,
	};

	/* the provider as DownloadProvider's manifest declares it (DocumentsProvider.attachInfo() checks this) */
	public static ProviderInfo providerInfo() {
		ProviderInfo info = new ProviderInfo();
		info.authority = AUTHORITY;
		info.packageName = "com.android.providers.downloads";
		info.name = "com.android.providers.downloads.DownloadStorageProvider";
		info.exported = true;
		info.grantUriPermissions = true;
		info.readPermission = android.Manifest.permission.MANAGE_DOCUMENTS;
		info.writePermission = android.Manifest.permission.MANAGE_DOCUMENTS;
		return info;
	}

	public static ATLRawDocumentsProvider create(Context context) {
		ATLRawDocumentsProvider provider = new ATLRawDocumentsProvider();
		provider.attachInfo(context, providerInfo());
		provider.onCreate();
		return provider;
	}

	/* the document of a file; DocumentsContract.buildDocumentUri(AUTHORITY, RawDocumentsHelper.getDocIdForFile()) */
	public static Uri documentUri(File file) {
		return DocumentsContract.buildDocumentUri(AUTHORITY, RAW_PREFIX + file.getAbsolutePath());
	}

	/* the tree of a directory, as DocumentsUI hands one out for OPEN_DOCUMENT_TREE */
	public static Uri treeUri(File dir) {
		return DocumentsContract.buildTreeDocumentUri(AUTHORITY, RAW_PREFIX + dir.getAbsolutePath());
	}

	@Override
	public boolean onCreate() {
		super.onCreate(DEFAULT_DOCUMENT_PROJECTION);
		return true;
	}

	@Override
	public Cursor queryRoots(String[] projection) throws FileNotFoundException {
		/* DownloadStorageProvider's only root is the Downloads root of DownloadManager's database */
		return new MatrixCursor(projection != null ? projection : DEFAULT_ROOT_PROJECTION);
	}

	@Override
	public DocumentsContract.Path findDocumentPath(String parentDocId, String docId) throws FileNotFoundException {
		/* DownloadStorageProvider.findDocumentPath() for raw: documents, which name no root */
		if (parentDocId == null)
			throw new FileNotFoundException(docId + " is not found under a root");
		return new DocumentsContract.Path(null, findDocumentPath(getFileForDocId(parentDocId), getFileForDocId(docId)));
	}

	@Override
	protected File getFileForDocId(String docId, boolean visible) throws FileNotFoundException {
		if (docId != null && docId.startsWith(RAW_PREFIX))
			return new File(docId.substring(RAW_PREFIX.length()));
		throw new FileNotFoundException("No such document " + docId);
	}

	@Override
	protected String getDocIdForFile(File file) throws FileNotFoundException {
		return RAW_PREFIX + file.getAbsolutePath();
	}

	@Override
	protected Uri buildNotificationUri(String docId) {
		return DocumentsContract.buildChildDocumentsUri(AUTHORITY, docId);
	}
}
