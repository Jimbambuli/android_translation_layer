/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.internal.content.storage;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MatrixCursor.RowBuilder;
import android.graphics.Point;
import android.net.Uri;
import android.os.CancellationSignal;
import android.os.FileObserver;
import android.os.FileUtils;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.DocumentsContract.Document;
import android.provider.DocumentsProvider;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.android.internal.util.ArrayUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A helper class for {@link android.provider.DocumentsProvider} to perform file operations on local
 * files.
 *
 * AOSP 16's FileSystemProvider without what needs MediaProvider: there is no MediaStore.scanFile() (ATL's
 * media index brings itself up to date, ATLMediaContentProvider), no transcoding for the calling uid (a file
 * is opened for reading as it is, like FileUtils.convertToModernFd() has no modern fd), no MetadataReader
 * (so no FLAG_SUPPORTS_METADATA and no getDocumentMetadata()), and the trash API of the flags
 * enableDocumentsTrashApi/enableTrashAndRestoreByFilePathApi is left out; querySearchDocuments() of the
 * subclasses that search (ExternalStorageProvider) too.
 */
public abstract class FileSystemProvider extends DocumentsProvider {

	private static final String TAG = "FileSystemProvider";

	private static final boolean LOG_INOTIFY = false;

	private String[] mDefaultProjection;

	private final ArrayMap<File, DirectoryObserver> mObservers = new ArrayMap<>();

	private Handler mHandler;

	protected abstract File getFileForDocId(String docId, boolean visible)
	    throws FileNotFoundException;

	protected abstract String getDocIdForFile(File file) throws FileNotFoundException;

	protected abstract Uri buildNotificationUri(String docId);

	/**
	 * Callback indicating that the given document has been modified. This gives
	 * the provider a hook to invalidate cached data, such as {@code sdcardfs}.
	 */
	protected void onDocIdChanged(String docId) {
		// Default is no-op
	}

	/**
	 * Callback indicating that the given document has been deleted or moved. This gives
	 * the provider a hook to revoke the uri permissions.
	 */
	protected void onDocIdDeleted(String docId, boolean shouldRevokeUriPermission) {
		// Default is no-op
	}

	@Override
	public boolean onCreate() {
		throw new UnsupportedOperationException(
		    "Subclass should override this and call onCreate(defaultDocumentProjection)");
	}

	protected void onCreate(String[] defaultProjection) {
		mHandler = new Handler();
		mDefaultProjection = defaultProjection;
	}

	@Override
	public boolean isChildDocument(String parentDocId, String docId) {
		try {
			final File parent = getFileForDocId(parentDocId).getCanonicalFile();
			final File doc = getFileForDocId(docId).getCanonicalFile();
			return FileUtils.contains(parent, doc);
		} catch (IOException e) {
			throw new IllegalArgumentException(
			    "Failed to determine if " + docId + " is child of " + parentDocId + ": " + e);
		}
	}

	protected final List<String> findDocumentPath(File parent, File doc)
	    throws FileNotFoundException {

		if (!doc.exists()) {
			throw new FileNotFoundException(doc + " is not found.");
		}

		if (!FileUtils.contains(parent, doc)) {
			throw new FileNotFoundException(doc + " is not found under " + parent);
		}

		LinkedList<String> path = new LinkedList<>();
		while (doc != null && FileUtils.contains(parent, doc)) {
			path.addFirst(getDocIdForFile(doc));

			doc = doc.getParentFile();
		}

		return path;
	}

	@Override
	public String createDocument(String docId, String mimeType, String displayName)
	    throws FileNotFoundException {
		displayName = FileUtils.buildValidFatFilename(displayName);

		final File parent = getFileForDocId(docId);
		if (!parent.isDirectory()) {
			throw new IllegalArgumentException("Parent document isn't a directory");
		}

		final File file = FileUtils.buildUniqueFile(parent, mimeType, displayName);
		final String childId;
		if (Document.MIME_TYPE_DIR.equals(mimeType)) {
			if (!file.mkdir()) {
				throw new IllegalStateException("Failed to mkdir " + file);
			}
			childId = getDocIdForFile(file);
			onDocIdChanged(childId);
		} else {
			try {
				if (!file.createNewFile()) {
					throw new IllegalStateException("Failed to touch " + file);
				}
				childId = getDocIdForFile(file);
				onDocIdChanged(childId);
			} catch (IOException e) {
				throw new IllegalStateException("Failed to touch " + file + ": " + e);
			}
		}
		return childId;
	}

	@Override
	public String renameDocument(String docId, String displayName) throws FileNotFoundException {
		// Since this provider treats renames as generating a completely new
		// docId, we're okay with letting the MIME type change.
		displayName = FileUtils.buildValidFatFilename(displayName);

		final File before = getFileForDocId(docId);
		final File after = FileUtils.buildUniqueFile(before.getParentFile(), displayName);
		if (!before.renameTo(after)) {
			throw new IllegalStateException("Failed to rename to " + after);
		}

		final String afterDocId = getDocIdForFile(after);
		onDocIdChanged(docId);
		onDocIdChanged(afterDocId);

		if (!TextUtils.equals(docId, afterDocId)) {
			// DocumentsProvider handles the revoking / granting uri permission for the docId and
			// the afterDocId in the renameDocument case. Don't need to call revokeUriPermission
			// for the docId here.
			onDocIdDeleted(docId, /* shouldRevokeUriPermission */ false);
			return afterDocId;
		} else {
			return null;
		}
	}

	@Override
	public String moveDocument(String sourceDocumentId, String sourceParentDocumentId,
	                           String targetParentDocumentId)
	    throws FileNotFoundException {
		final File before = getFileForDocId(sourceDocumentId);
		final File after = new File(getFileForDocId(targetParentDocumentId), before.getName());

		if (after.exists()) {
			throw new IllegalStateException("Already exists " + after);
		}
		if (!before.renameTo(after)) {
			throw new IllegalStateException("Failed to move to " + after);
		}

		final String docId = getDocIdForFile(after);
		onDocIdChanged(sourceDocumentId);
		onDocIdDeleted(sourceDocumentId, /* shouldRevokeUriPermission */ true);
		onDocIdChanged(docId);
		return docId;
	}

	@Override
	public void deleteDocument(String docId) throws FileNotFoundException {
		final File file = getFileForDocId(docId);

		final boolean isDirectory = file.isDirectory();
		if (isDirectory) {
			FileUtils.deleteContents(file);
		}
		// We could be deleting pending media which doesn't have any content yet, so only throw
		// if the file exists and we fail to delete it.
		if (file.exists() && !file.delete()) {
			throw new IllegalStateException("Failed to delete " + file);
		}

		onDocIdChanged(docId);
		onDocIdDeleted(docId, /* shouldRevokeUriPermission */ true);
	}

	@Override
	public Cursor queryDocument(String documentId, String[] projection)
	    throws FileNotFoundException {
		final MatrixCursor result = new MatrixCursor(resolveProjection(projection));
		includeFile(result, documentId, null);
		return result;
	}

	/**
	 * WARNING: this method should really be {@code final}, but for the backward compatibility it's
	 * not; new classes that extend {@link FileSystemProvider} should override
	 * {@link #queryChildDocuments(String, String[], String, boolean)}, not this method.
	 */
	@Override
	public Cursor queryChildDocuments(String documentId, String[] projection, String sortOrder)
	    throws FileNotFoundException {
		return queryChildDocuments(documentId, projection, sortOrder, /* includeHidden */ false);
	}

	@Override
	public final Cursor queryChildDocumentsForManage(String documentId, String[] projection,
	                                                 String sortOrder) throws FileNotFoundException {
		return queryChildDocuments(documentId, projection, sortOrder, /* includeHidden */ true);
	}

	protected Cursor queryChildDocuments(String documentId, String[] projection, String sortOrder,
	                                     boolean includeHidden) throws FileNotFoundException {
		final File parent = getFileForDocId(documentId);
		final MatrixCursor result = new DirectoryCursor(
		    resolveProjection(projection), documentId, parent);

		if (!parent.isDirectory()) {
			Log.w(TAG, '"' + documentId + "\" is not a directory");
			return result;
		}

		if (!includeHidden && shouldHideDocument(documentId)) {
			Log.w(TAG, "Queried directory \"" + documentId + "\" is hidden");
			return result;
		}

		for (File file : FileUtils.listFilesOrEmpty(parent)) {
			if (!includeHidden && shouldHideDocument(file))
				continue;

			includeFile(result, null, file);
		}

		return result;
	}

	@Override
	public String getDocumentType(String documentId) throws FileNotFoundException {
		return getDocumentType(documentId, getFileForDocId(documentId));
	}

	private String getDocumentType(final String documentId, final File file)
	    throws FileNotFoundException {
		if (file.isDirectory()) {
			return Document.MIME_TYPE_DIR;
		} else {
			final int lastDot = documentId.lastIndexOf('.');
			if (lastDot >= 0) {
				final String extension = documentId.substring(lastDot + 1).toLowerCase();
				final String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
				if (mime != null) {
					return mime;
				}
			}
			return ContentResolver.MIME_TYPE_DEFAULT;
		}
	}

	@Override
	public ParcelFileDescriptor openDocument(
	    String documentId, String mode, CancellationSignal signal)
	    throws FileNotFoundException {
		final File file = getFileForDocId(documentId);
		final File visibleFile = getFileForDocId(documentId, true);

		final int pfdMode = ParcelFileDescriptor.parseMode(mode);
		if (visibleFile == null || pfdMode == ParcelFileDescriptor.MODE_READ_ONLY) {
			/* AOSP opens a visible file for reading through MediaStore (transcoding for the calling uid) */
			return ParcelFileDescriptor.open(file, pfdMode);
		} else {
			try {
				// When finished writing, kick off media scanner
				return ParcelFileDescriptor.open(
				    file, pfdMode, mHandler, (IOException e) -> {
					    onDocIdChanged(documentId);
					    scanFile(visibleFile);
				    });
			} catch (IOException e) {
				throw new FileNotFoundException("Failed to open for writing: " + e);
			}
		}
	}

	private void scanFile(File visibleFile) {
		final Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
		intent.setData(Uri.fromFile(visibleFile));
		getContext().sendBroadcast(intent);
	}

	@Override
	public AssetFileDescriptor openDocumentThumbnail(
	    String documentId, Point sizeHint, CancellationSignal signal)
	    throws FileNotFoundException {
		final File file = getFileForDocId(documentId);
		return DocumentsContract.openImageThumbnail(file);
	}

	protected RowBuilder includeFile(final MatrixCursor result, String docId, File file)
	    throws FileNotFoundException {
		final String[] columns = result.getColumnNames();
		final RowBuilder row = result.newRow();

		if (docId == null) {
			docId = getDocIdForFile(file);
		} else {
			file = getFileForDocId(docId);
		}

		final String mimeType = getDocumentType(docId, file);
		row.add(Document.COLUMN_DOCUMENT_ID, docId);
		row.add(Document.COLUMN_MIME_TYPE, mimeType);

		final int flagIndex = ArrayUtils.indexOf(columns, Document.COLUMN_FLAGS);
		if (flagIndex != -1) {
			final boolean isDir = mimeType.equals(Document.MIME_TYPE_DIR);
			int flags = 0;
			if (file.canWrite()) {
				flags |= Document.FLAG_SUPPORTS_DELETE;
				flags |= Document.FLAG_SUPPORTS_RENAME;
				flags |= Document.FLAG_SUPPORTS_MOVE;

				if (isDir) {
					flags |= Document.FLAG_DIR_SUPPORTS_CREATE;
				} else {
					flags |= Document.FLAG_SUPPORTS_WRITE;
				}
			}

			if (isDir && shouldBlockDirectoryFromTree(docId)) {
				flags |= Document.FLAG_DIR_BLOCKS_OPEN_DOCUMENT_TREE;
			}

			if (mimeType.startsWith("image/")) {
				flags |= Document.FLAG_SUPPORTS_THUMBNAIL;
			}

			if (typeSupportsMetadata(mimeType)) {
				flags |= Document.FLAG_SUPPORTS_METADATA;
			}
			row.add(flagIndex, flags);
		}

		final int displayNameIndex = ArrayUtils.indexOf(columns, Document.COLUMN_DISPLAY_NAME);
		if (displayNameIndex != -1) {
			row.add(displayNameIndex, file.getName());
		}

		final int lastModifiedIndex = ArrayUtils.indexOf(columns, Document.COLUMN_LAST_MODIFIED);
		if (lastModifiedIndex != -1) {
			final long lastModified = file.lastModified();
			// Only publish dates reasonably after epoch
			if (lastModified > 31536000000L) {
				row.add(lastModifiedIndex, lastModified);
			}
		}
		final int sizeIndex = ArrayUtils.indexOf(columns, Document.COLUMN_SIZE);
		if (sizeIndex != -1) {
			row.add(sizeIndex, file.length());
		}

		// Return the row builder just in case any subclass want to add more stuff to it.
		return row;
	}

	/**
	 * Some providers may want to restrict access to certain directories and files,
	 * e.g. <i>"Android/data"</i> and <i>"Android/obb"</i> on the shared storage for
	 * privacy reasons.
	 * Such providers should override this method.
	 */
	protected boolean shouldHideDocument(String documentId)
	    throws FileNotFoundException {
		return false;
	}

	/**
	 * A variant of the {@link #shouldHideDocument(String)} that takes a {@link File} instead of
	 * a {@link String} {@code documentId}.
	 *
	 * @see #shouldHideDocument(String)
	 */
	protected final boolean shouldHideDocument(File document)
	    throws FileNotFoundException {
		return shouldHideDocument(getDocIdForFile(document));
	}

	/**
	 * @return if the directory that should be blocked from being selected when the user launches
	 * an {@link Intent#ACTION_OPEN_DOCUMENT_TREE} intent.
	 *
	 * @see Document#FLAG_DIR_BLOCKS_OPEN_DOCUMENT_TREE
	 */
	protected boolean shouldBlockDirectoryFromTree(String documentId)
	    throws FileNotFoundException {
		return false;
	}

	/* AOSP: MetadataReader.isSupportedMimeType(mimeType) or a directory; there is no MetadataReader here */
	protected boolean typeSupportsMetadata(String mimeType) {
		return false;
	}

	protected final File getFileForDocId(String docId) throws FileNotFoundException {
		return getFileForDocId(docId, false);
	}

	private String[] resolveProjection(String[] projection) {
		return projection == null ? mDefaultProjection : projection;
	}

	private void startObserving(File file, Uri notifyUri, DirectoryCursor cursor) {
		synchronized (mObservers) {
			DirectoryObserver observer = mObservers.get(file);
			if (observer == null) {
				observer =
				    new DirectoryObserver(file, getContext().getContentResolver(), notifyUri);
				observer.startWatching();
				mObservers.put(file, observer);
			}
			observer.mCursors.add(cursor);

			if (LOG_INOTIFY)
				Log.d(TAG, "after start: " + observer);
		}
	}

	private void stopObserving(File file, DirectoryCursor cursor) {
		synchronized (mObservers) {
			DirectoryObserver observer = mObservers.get(file);
			if (observer == null)
				return;

			observer.mCursors.remove(cursor);
			if (observer.mCursors.size() == 0) {
				mObservers.remove(file);
				observer.stopWatching();
			}

			if (LOG_INOTIFY)
				Log.d(TAG, "after stop: " + observer);
		}
	}

	private static class DirectoryObserver extends FileObserver {
		private static final int NOTIFY_EVENTS = ATTRIB | CLOSE_WRITE | MOVED_FROM | MOVED_TO | CREATE | DELETE | DELETE_SELF | MOVE_SELF;

		private final File mFile;
		private final ContentResolver mResolver;
		private final Uri mNotifyUri;
		private final CopyOnWriteArrayList<DirectoryCursor> mCursors;

		DirectoryObserver(File file, ContentResolver resolver, Uri notifyUri) {
			super(file.getAbsolutePath(), NOTIFY_EVENTS);
			mFile = file;
			mResolver = resolver;
			mNotifyUri = notifyUri;
			mCursors = new CopyOnWriteArrayList<>();
		}

		@Override
		public void onEvent(int event, String path) {
			if ((event & NOTIFY_EVENTS) != 0) {
				if (LOG_INOTIFY)
					Log.d(TAG, "onEvent() " + event + " at " + path);
				for (DirectoryCursor cursor : mCursors) {
					cursor.notifyChanged();
				}
				mResolver.notifyChange(mNotifyUri, null, false);
			}
		}

		@Override
		public String toString() {
			String filePath = mFile.getAbsolutePath();
			return "DirectoryObserver{file=" + filePath + ", ref=" + mCursors.size() + "}";
		}
	}

	private class DirectoryCursor extends MatrixCursor {
		private final File mFile;

		public DirectoryCursor(String[] columnNames, String docId, File file) {
			super(columnNames);

			final Uri notifyUri = buildNotificationUri(docId);
			boolean registerSelfObserver = false; // Our FileObserver sees all relevant changes.
			setNotificationUris(getContext().getContentResolver(), Arrays.asList(notifyUri),
			                    getContext().getContentResolver().getUserId(), registerSelfObserver);

			mFile = file;
			startObserving(mFile, notifyUri, this);
		}

		public void notifyChanged() {
			onChange(false);
		}

		@Override
		public void close() {
			super.close();
			stopObserving(mFile, this);
		}
	}
}
