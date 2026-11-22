package android.app;

import android.atl.ATLRawDocumentsProvider;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.FileUtils;
import android.provider.DocumentsContract;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/* Intent.ACTION_OPEN_DOCUMENT, GET_CONTENT, CREATE_DOCUMENT and OPEN_DOCUMENT_TREE: on Android, DocumentsUI answers
 * them (packages/apps/DocumentsUI: picker/PickActivity, picker/ActionHandler, base/State). ATL shows the desktop's
 * file dialog instead (Activity.nativeFileChooser(), in a Flatpak the file chooser portal) and answers with the
 * documents of the chosen files (ATLRawDocumentsProvider). */
class DocumentsPicker {

	static final List<String> ACTIONS = Arrays.asList(Intent.ACTION_OPEN_DOCUMENT, Intent.ACTION_GET_CONTENT,
	                                                  Intent.ACTION_CREATE_DOCUMENT, Intent.ACTION_OPEN_DOCUMENT_TREE);

	/* the dialog for the action, a GtkFileChooserAction: 0 open, 1 save, 2 select a folder */
	static int dialogAction(Intent intent) {
		if (Intent.ACTION_CREATE_DOCUMENT.equals(intent.getAction()))
			return 1;
		if (Intent.ACTION_OPEN_DOCUMENT_TREE.equals(intent.getAction()))
			return 2;
		return 0;
	}

	/* PickActivity.includeState() with State.initAcceptMimes(): EXTRA_MIME_TYPES, else the type, else all */
	static String[] acceptMimes(Intent intent) {
		String defaultMimeType = (intent.getType() == null) ? "*/*" : intent.getType();
		if (intent.hasExtra(Intent.EXTRA_MIME_TYPES))
			return intent.getStringArrayExtra(Intent.EXTRA_MIME_TYPES);
		return new String[] {defaultMimeType};
	}

	/* the filter of the dialog: the accepted types, null when all files are (MimeTypes.mimeMatches() of "*\/*") */
	static String[] dialogMimeTypes(Intent intent) {
		String[] acceptMimes = acceptMimes(intent);
		if (acceptMimes == null || acceptMimes.length == 0 || Arrays.asList(acceptMimes).contains("*/*"))
			return null;
		return acceptMimes;
	}

	/* PickActivity.includeState(): several documents only for OPEN_DOCUMENT and GET_CONTENT */
	static boolean allowMultiple(Intent intent) {
		return (Intent.ACTION_OPEN_DOCUMENT.equals(intent.getAction()) || Intent.ACTION_GET_CONTENT.equals(intent.getAction()))
		    && intent.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
	}

	/* CREATE_DOCUMENT: SaveFragment offers EXTRA_TITLE; the provider's createDocument() makes it a valid name with
	 * the extension of the type (FileSystemProvider.createDocument(), FileUtils.buildUniqueFile()). The dialog offers
	 * that name, as the user can change it there. */
	static String initialName(Intent intent) {
		if (!Intent.ACTION_CREATE_DOCUMENT.equals(intent.getAction()))
			return null;
		String title = intent.getStringExtra(Intent.EXTRA_TITLE);
		if (title == null)
			return null;
		String name = FileUtils.buildValidFatFilename(title);
		String[] parts = FileUtils.splitFileName(intent.getType(), name);
		return parts[1].isEmpty() ? parts[0] : parts[0] + "." + parts[1];
	}

	/* ActionHandler.launchToInitialUri(): DocumentsContract.EXTRA_INITIAL_URI, when it is one of ATL's documents,
	 * as the folder to start in (the document's folder, or the document when it is a folder) */
	static String initialFolder(Intent intent) {
		Uri initialUri = (Uri)intent.getParcelableExtra(DocumentsContract.EXTRA_INITIAL_URI);
		if (initialUri == null || !ATLRawDocumentsProvider.AUTHORITY.equals(initialUri.getAuthority()))
			return null;
		String docId;
		try {
			docId = DocumentsContract.isTreeUri(initialUri) && initialUri.getPathSegments().size() < 4
			    ? DocumentsContract.getTreeDocumentId(initialUri) : DocumentsContract.getDocumentId(initialUri);
		} catch (IllegalArgumentException e) {
			return null;
		}
		if (docId == null || !docId.startsWith("raw:/"))
			return null;
		File file = new File(docId.substring(4));
		return file.isDirectory() ? file.getPath() : file.getParent();
	}

	/* the document of a file the dialog chose; for CREATE_DOCUMENT a new file is created like the provider's
	 * createDocument() (an existing one the user chose to replace stays as it is, ActionHandler.saveDocument()) */
	private static Uri documentFor(Intent request, File file) throws IOException {
		if (Intent.ACTION_OPEN_DOCUMENT_TREE.equals(request.getAction()))
			return ATLRawDocumentsProvider.treeUri(file);
		if (Intent.ACTION_CREATE_DOCUMENT.equals(request.getAction()) && !file.exists() && !file.createNewFile())
			throw new FileNotFoundException("Failed to touch " + file);
		return ATLRawDocumentsProvider.documentUri(file);
	}

	/* ActionHandler.onPickFinished(): one document as the data, several as ClipData, with the grants of the action;
	 * null if none could be had */
	static Intent getPickResult(Intent request, String[] fileUris) {
		Uri[] uris = new Uri[fileUris.length];
		for (int i = 0; i < fileUris.length; i++) {
			try {
				uris[i] = documentFor(request, new File(Uri.parse(fileUris[i]).getPath()));
			} catch (IOException e) {
				android.util.Slog.w("DocumentsPicker", "no document for " + fileUris[i] + ": " + e);
				return null;
			}
		}

		final Intent intent = new Intent();
		if (uris.length == 1) {
			intent.setData(uris[0]);
		} else if (uris.length > 1) {
			final ClipData clipData = new ClipData(
			    null, acceptMimes(request), new ClipData.Item(uris[0]));
			for (int i = 1; i < uris.length; i++) {
				clipData.addItem(new ClipData.Item(uris[i]));
			}
			intent.setClipData(clipData);
		}

		if (Intent.ACTION_GET_CONTENT.equals(request.getAction())) {
			intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		} else if (Intent.ACTION_OPEN_DOCUMENT_TREE.equals(request.getAction())) {
			intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
			                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
			                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
			                | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
		} else {
			intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
			                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
			                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
		}
		return intent;
	}
}
