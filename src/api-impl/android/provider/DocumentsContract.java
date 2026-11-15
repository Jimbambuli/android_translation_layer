/*
 * Copyright (C) 2013 The Android Open Source Project
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

package android.provider;

import static com.android.internal.util.Preconditions.checkCollectionElementsNotNull;
import static com.android.internal.util.Preconditions.checkCollectionNotEmpty;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.ParcelableException;
import android.os.RemoteException;
import android.util.Log;
import dalvik.system.VMRuntime;
import java.io.FileNotFoundException;
import com.android.internal.util.Preconditions;
import java.util.List;
import java.util.Objects;

public final class DocumentsContract {
	public static final String EXTRA_SHOW_ADVANCED = "android.provider.extra.SHOW_ADVANCED";
	public static final String EXTRA_INITIAL_URI = "android.provider.extra.INITIAL_URI";
	public static final String EXTERNAL_STORAGE_PROVIDER_AUTHORITY = "com.android.externalstorage.documents";
	public static final String EXTERNAL_STORAGE_PRIMARY_EMULATED_ROOT_ID = "primary";
	public static final String EXTRA_LOADING = "loading";
	public static final String EXTRA_INFO = "info";
	public static final String EXTRA_ERROR = "error";
	public static final String EXTRA_RESULT = "result";
	public static final String EXTRA_ORIENTATION = "android.provider.extra.ORIENTATION";
	public static final String METHOD_CREATE_DOCUMENT = "android:createDocument";
	public static final String METHOD_RENAME_DOCUMENT = "android:renameDocument";
	public static final String METHOD_DELETE_DOCUMENT = "android:deleteDocument";
	public static final String METHOD_COPY_DOCUMENT = "android:copyDocument";
	public static final String METHOD_MOVE_DOCUMENT = "android:moveDocument";
	public static final String METHOD_IS_CHILD_DOCUMENT = "android:isChildDocument";
	public static final String METHOD_REMOVE_DOCUMENT = "android:removeDocument";
	public static final String METHOD_EJECT_ROOT = "android:ejectRoot";
	public static final String METHOD_FIND_DOCUMENT_PATH = "android:findDocumentPath";
	public static final String METHOD_CREATE_WEB_LINK_INTENT = "android:createWebLinkIntent";
	public static final String METHOD_GET_DOCUMENT_METADATA = "android:getDocumentMetadata";
	public static final String EXTRA_PARENT_URI = "parentUri";
	public static final String EXTRA_URI = "uri";
	public static final String EXTRA_URI_PERMISSIONS = "uriPermissions";
	public static final String EXTRA_OPTIONS = "options";
	/* seems these are not supposed to be ABI but some apps disagree */
	private static final String PATH_ROOT = "root";
	private static final String PATH_RECENT = "recent";
	private static final String PATH_DOCUMENT = "document";
	private static final String PATH_CHILDREN = "children";
	private static final String TAG = "DocumentsContract";
	private static final String PATH_SEARCH = "search";
	private static final String PATH_TREE = "tree";
	private static final String PARAM_QUERY = "query";
	private static final String PARAM_MANAGE = "manage";

	public static String getDocumentId(Uri documentUri) {
		final List<String> paths = documentUri.getPathSegments();
		if (paths.size() >= 2 && PATH_DOCUMENT.equals(paths.get(0))) {
			return paths.get(1);
		}
		if (paths.size() >= 4 && PATH_TREE.equals(paths.get(0))
		    && PATH_DOCUMENT.equals(paths.get(2))) {
			return paths.get(3);
		}
		throw new IllegalArgumentException("Invalid URI: " + documentUri);
	}

	public static String getTreeDocumentId(Uri documentUri) {
		final List<String> paths = documentUri.getPathSegments();
		if (paths.size() >= 2 && PATH_TREE.equals(paths.get(0))) {
			return paths.get(1);
		}
		throw new IllegalArgumentException("Invalid URI: " + documentUri);
	}

	public static boolean isContentUri(Uri uri) {
		return uri != null && ContentResolver.SCHEME_CONTENT.equals(uri.getScheme());
	}

	private static boolean isDocumentsProvider(Context context, String authority) {
		return false;
	}

	public static boolean isDocumentUri(Context context, Uri uri) {
		if (isContentUri(uri) && isDocumentsProvider(context, uri.getAuthority())) {
			final List<String> paths = uri.getPathSegments();
			if (paths.size() == 2) {
				return PATH_DOCUMENT.equals(paths.get(0));
			} else if (paths.size() == 4) {
				return PATH_TREE.equals(paths.get(0)) && PATH_DOCUMENT.equals(paths.get(2));
			}
		}
		return false;
	}

	public static boolean isTreeUri(Uri uri) {
		final List<String> paths = uri.getPathSegments();
		return (paths.size() >= 2 && PATH_TREE.equals(paths.get(0)));
	}

	public static Uri buildChildDocumentsUriUsingTree(Uri treeUri, String parentDocumentId) {
		return new Uri.Builder()
		    .scheme(ContentResolver.SCHEME_CONTENT)
		    .authority(treeUri.getAuthority())
		    .appendPath(PATH_TREE)
		    .appendPath(getTreeDocumentId(treeUri))
		    .appendPath(PATH_DOCUMENT)
		    .appendPath(parentDocumentId)
		    .appendPath(PATH_CHILDREN)
		    .build();
	}

	public static Uri buildDocumentUriUsingTree(Uri treeUri, String documentId) {
		return new Uri.Builder()
		    .scheme(ContentResolver.SCHEME_CONTENT)
		    .authority(treeUri.getAuthority())
		    .appendPath(PATH_TREE)
		    .appendPath(getTreeDocumentId(treeUri))
		    .appendPath(PATH_DOCUMENT)
		    .appendPath(documentId)
		    .build();
	}

	/* AOSP DocumentsContract.copyDocument .. removeDocument, isChildDocument: the request goes to the
	 * DocumentsProvider through ContentResolver.call(); failures are logged and rethrown by rethrowIfNecessary */
	public static Uri createDocument(ContentResolver content, Uri parentDocumentUri, String mimeType, String displayName) throws FileNotFoundException {
		try {
			final Bundle in = new Bundle();
			in.putParcelable(DocumentsContract.EXTRA_URI, parentDocumentUri);
			in.putString(Document.COLUMN_MIME_TYPE, mimeType);
			in.putString(Document.COLUMN_DISPLAY_NAME, displayName);

			final Bundle out = content.call(parentDocumentUri.getAuthority(), METHOD_CREATE_DOCUMENT, null, in);
			return out.getParcelable(DocumentsContract.EXTRA_URI, android.net.Uri.class);
		} catch (Exception e) {
			Log.w(TAG, "Failed to create document", e);
			rethrowIfNecessary(e);
			return null;
		}
	}

	public static boolean isChildDocument(ContentResolver content, Uri parentDocumentUri, Uri childDocumentUri) throws FileNotFoundException {
		Preconditions.checkNotNull(content, "content can not be null");
		Preconditions.checkNotNull(parentDocumentUri, "parentDocumentUri can not be null");
		Preconditions.checkNotNull(childDocumentUri, "childDocumentUri can not be null");
		try {
			final Bundle in = new Bundle();
			in.putParcelable(DocumentsContract.EXTRA_URI, parentDocumentUri);
			in.putParcelable(DocumentsContract.EXTRA_TARGET_URI, childDocumentUri);

			final Bundle out = content.call(parentDocumentUri.getAuthority(), METHOD_IS_CHILD_DOCUMENT, null, in);
			if (out == null) {
				throw new RemoteException("Failed to get a response from isChildDocument query.");
			}
			if (!out.containsKey(DocumentsContract.EXTRA_RESULT)) {
				throw new RemoteException("Response did not include result field..");
			}
			return out.getBoolean(DocumentsContract.EXTRA_RESULT);
		} catch (Exception e) {
			Log.w(TAG, "Failed to create document", e);
			rethrowIfNecessary(e);
			return false;
		}
	}

	public static Uri renameDocument(ContentResolver content, Uri documentUri, String displayName) throws FileNotFoundException {
		try {
			final Bundle in = new Bundle();
			in.putParcelable(DocumentsContract.EXTRA_URI, documentUri);
			in.putString(Document.COLUMN_DISPLAY_NAME, displayName);

			final Bundle out = content.call(documentUri.getAuthority(), METHOD_RENAME_DOCUMENT, null, in);
			final Uri outUri = out.getParcelable(DocumentsContract.EXTRA_URI, android.net.Uri.class);
			return (outUri != null) ? outUri : documentUri;
		} catch (Exception e) {
			Log.w(TAG, "Failed to rename document", e);
			rethrowIfNecessary(e);
			return null;
		}
	}

	public static boolean deleteDocument(ContentResolver content, Uri documentUri) throws FileNotFoundException {
		try {
			final Bundle in = new Bundle();
			in.putParcelable(DocumentsContract.EXTRA_URI, documentUri);

			content.call(documentUri.getAuthority(), METHOD_DELETE_DOCUMENT, null, in);
			return true;
		} catch (Exception e) {
			Log.w(TAG, "Failed to delete document", e);
			rethrowIfNecessary(e);
			return false;
		}
	}

	public static Uri copyDocument(ContentResolver content, Uri sourceDocumentUri, Uri targetParentDocumentUri) throws FileNotFoundException {
		try {
			final Bundle in = new Bundle();
			in.putParcelable(DocumentsContract.EXTRA_URI, sourceDocumentUri);
			in.putParcelable(DocumentsContract.EXTRA_TARGET_URI, targetParentDocumentUri);

			final Bundle out = content.call(sourceDocumentUri.getAuthority(), METHOD_COPY_DOCUMENT, null, in);
			return out.getParcelable(DocumentsContract.EXTRA_URI, android.net.Uri.class);
		} catch (Exception e) {
			Log.w(TAG, "Failed to copy document", e);
			rethrowIfNecessary(e);
			return null;
		}
	}

	public static Uri moveDocument(ContentResolver content, Uri sourceDocumentUri, Uri sourceParentDocumentUri, Uri targetParentDocumentUri) throws FileNotFoundException {
		try {
			final Bundle in = new Bundle();
			in.putParcelable(DocumentsContract.EXTRA_URI, sourceDocumentUri);
			in.putParcelable(DocumentsContract.EXTRA_PARENT_URI, sourceParentDocumentUri);
			in.putParcelable(DocumentsContract.EXTRA_TARGET_URI, targetParentDocumentUri);

			final Bundle out = content.call(sourceDocumentUri.getAuthority(), METHOD_MOVE_DOCUMENT, null, in);
			return out.getParcelable(DocumentsContract.EXTRA_URI, android.net.Uri.class);
		} catch (Exception e) {
			Log.w(TAG, "Failed to move document", e);
			rethrowIfNecessary(e);
			return null;
		}
	}

	public static boolean removeDocument(ContentResolver content, Uri documentUri, Uri parentDocumentUri) throws FileNotFoundException {
		try {
			final Bundle in = new Bundle();
			in.putParcelable(DocumentsContract.EXTRA_URI, documentUri);
			in.putParcelable(DocumentsContract.EXTRA_PARENT_URI, parentDocumentUri);

			content.call(documentUri.getAuthority(), METHOD_REMOVE_DOCUMENT, null, in);
			return true;
		} catch (Exception e) {
			Log.w(TAG, "Failed to remove document", e);
			rethrowIfNecessary(e);
			return false;
		}
	}

	/* AOSP rethrowIfNecessary; a provider in ATL runs in the same process, so its FileNotFoundException
	 * arrives as such instead of wrapped in a ParcelableException */
	private static void rethrowIfNecessary(Exception e) throws FileNotFoundException {
		if (VMRuntime.getRuntime().getTargetSdkVersion() >= Build.VERSION_CODES.O) {
			if (e instanceof ParcelableException) {
				((ParcelableException) e).maybeRethrow(FileNotFoundException.class);
			} else if (e instanceof FileNotFoundException) {
				throw (FileNotFoundException) e;
			} else if (e instanceof RemoteException) {
				((RemoteException) e).rethrowAsRuntimeException();
			} else if (e instanceof RuntimeException) {
				throw (RuntimeException) e;
			}
		}
	}

	/* AOSP buildSearchDocumentsUri() */
	public static Uri buildSearchDocumentsUri(String authority, String rootId, String query) {
		return new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT).authority(authority)
		    .appendPath(PATH_ROOT).appendPath(rootId).appendPath(PATH_SEARCH)
		    .appendQueryParameter(PARAM_QUERY, query).build();
	}

	/* AOSP getDocumentThumbnail(): ContentResolver.loadThumbnail() with the size of the point; errors are logged,
	 * and rethrown for apps targeting O and above (rethrowIfNecessary) */
	public static android.graphics.Bitmap getDocumentThumbnail(ContentResolver content, Uri documentUri,
	                                                           android.graphics.Point size,
	                                                           android.os.CancellationSignal signal)
	    throws FileNotFoundException {
		try {
			return content.loadThumbnail(documentUri, new android.util.Size(size.x, size.y), signal);
		} catch (Exception e) {
			if (!(e instanceof android.os.OperationCanceledException))
				Log.w("DocumentsContract", "Failed to load thumbnail for " + documentUri + ": " + e);
			rethrowIfNecessary(e);
			return null;
		}
	}

	public static Uri buildChildDocumentsUri(String authority, String parentDocumentId) {
		return new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT).authority(authority).appendPath(PATH_DOCUMENT).appendPath(parentDocumentId).appendPath(PATH_CHILDREN).build();
	}

	public static Uri buildDocumentUri(String authority, String documentId) {
		return new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT).authority(authority).appendPath(PATH_DOCUMENT).appendPath(documentId).build();
	}

	public final static class Document {
		public static final String COLUMN_DOCUMENT_ID = "document_id";
		public static final String COLUMN_MIME_TYPE = "mime_type";
		public static final String COLUMN_DISPLAY_NAME = "_display_name"; //OpenableColumns.DISPLAY_NAME;
		public static final String COLUMN_SUMMARY = "summary";
		public static final String COLUMN_LAST_MODIFIED = "last_modified";
		public static final String COLUMN_ICON = "icon";
		public static final String COLUMN_FLAGS = "flags";
		public static final String COLUMN_SIZE = "_size"; //OpenableColumns.SIZE;
		public static final String MIME_TYPE_DIR = "vnd.android.document/directory";

		public static final int FLAG_SUPPORTS_THUMBNAIL = 1;
		public static final int FLAG_SUPPORTS_WRITE = 1 << 1;
		public static final int FLAG_SUPPORTS_DELETE = 1 << 2;
		public static final int FLAG_DIR_SUPPORTS_CREATE = 1 << 3;
		public static final int FLAG_DIR_PREFERS_GRID = 1 << 4;
		public static final int FLAG_DIR_PREFERS_LAST_MODIFIED = 1 << 5;
		public static final int FLAG_SUPPORTS_RENAME = 1 << 6;
		public static final int FLAG_SUPPORTS_COPY = 1 << 7;
		public static final int FLAG_SUPPORTS_MOVE = 1 << 8;
		public static final int FLAG_VIRTUAL_DOCUMENT = 1 << 9;
		public static final int FLAG_SUPPORTS_REMOVE = 1 << 10;
		public static final int FLAG_SUPPORTS_SETTINGS = 1 << 11;
		public static final int FLAG_WEB_LINKABLE = 1 << 12;
		public static final int FLAG_PARTIAL = 1 << 13;
		public static final int FLAG_SUPPORTS_METADATA = 1 << 14;
		public static final int FLAG_DIR_BLOCKS_OPEN_DOCUMENT_TREE = 1 << 15;
	}

	/* from AOSP android-14 for DocumentsProvider */
	/** {@hide} */
	public static final String EXTRA_TARGET_URI = "android.content.extra.TARGET_URI";

	/**
	 * Key for {@link DocumentsProvider} to query display name is matched.
	 * The match of display name is partial matching and case-insensitive.
	 * Ex: The value is "o", the display name of the results will contain
	 * both "foo" and "Open".
	 *
	 * @see DocumentsProvider#querySearchDocuments(String, String[],
	 *      Bundle)
	 */
	public static final String QUERY_ARG_DISPLAY_NAME = "android:query-arg-display-name";

	/**
	 * Key for {@link DocumentsProvider} to query mime types is matched.
	 * The value is a string array, it can support different mime types.
	 * Each items will be treated as "OR" condition. Ex: {"image/*" ,
	 * "video/*"}. The mime types of the results will contain both image
	 * type and video type.
	 *
	 * @see DocumentsProvider#querySearchDocuments(String, String[],
	 *      Bundle)
	 */
	public static final String QUERY_ARG_MIME_TYPES = "android:query-arg-mime-types";

	/**
	 * Key for {@link DocumentsProvider} to query the file size in bytes is
	 * larger than the value.
	 *
	 * @see DocumentsProvider#querySearchDocuments(String, String[],
	 *      Bundle)
	 */
	public static final String QUERY_ARG_FILE_SIZE_OVER = "android:query-arg-file-size-over";

	/**
	 * Key for {@link DocumentsProvider} to query the last modified time
	 * is newer than the value. The unit is in milliseconds since
	 * January 1, 1970 00:00:00.0 UTC.
	 *
	 * @see DocumentsProvider#querySearchDocuments(String, String[],
	 *      Bundle)
	 * @see Document#COLUMN_LAST_MODIFIED
	 */
	public static final String QUERY_ARG_LAST_MODIFIED_AFTER =
			"android:query-arg-last-modified-after";

	/**
	 * Key for {@link DocumentsProvider} to decide whether the files that
	 * have been added to MediaStore should be excluded. If the value is
	 * true, exclude them. Otherwise, include them.
	 *
	 * @see DocumentsProvider#querySearchDocuments(String, String[],
	 *      Bundle)
	 */
	public static final String QUERY_ARG_EXCLUDE_MEDIA = "android:query-arg-exclude-media";

	/**
	 * Constants related to a root of documents, including {@link Cursor} column
	 * names and flags. A root is the start of a tree of documents, such as a
	 * physical storage device, or an account. Each root starts at the directory
	 * referenced by {@link Root#COLUMN_DOCUMENT_ID}, which can recursively
	 * contain both documents and directories.
	 * <p>
	 * All columns are <em>read-only</em> to client applications.
	 */
	public final static class Root {
		private Root() {
		}

		/**
		 * Unique ID of a root. This ID is both provided by and interpreted by a
		 * {@link DocumentsProvider}, and should be treated as an opaque value
		 * by client applications. This column is required.
		 * <p>
		 * Type: STRING
		 */
		public static final String COLUMN_ROOT_ID = "root_id";

		/**
		 * Flags that apply to a root. This column is required.
		 * <p>
		 * Type: INTEGER (int)
		 *
		 * @see #FLAG_LOCAL_ONLY
		 * @see #FLAG_SUPPORTS_CREATE
		 * @see #FLAG_SUPPORTS_RECENTS
		 * @see #FLAG_SUPPORTS_SEARCH
		 */
		public static final String COLUMN_FLAGS = "flags";

		/**
		 * Icon resource ID for a root. This column is required.
		 * <p>
		 * Type: INTEGER (int)
		 */
		public static final String COLUMN_ICON = "icon";

		/**
		 * Title for a root, which will be shown to a user. This column is
		 * required. For a single storage service surfacing multiple accounts as
		 * different roots, this title should be the name of the service.
		 * <p>
		 * Type: STRING
		 */
		public static final String COLUMN_TITLE = "title";

		/**
		 * Summary for this root, which may be shown to a user. This column is
		 * optional, and may be {@code null}. For a single storage service
		 * surfacing multiple accounts as different roots, this summary should
		 * be the name of the account.
		 * <p>
		 * Type: STRING
		 */
		public static final String COLUMN_SUMMARY = "summary";

		/**
		 * Document which is a directory that represents the top directory of
		 * this root. This column is required.
		 * <p>
		 * Type: STRING
		 *
		 * @see Document#COLUMN_DOCUMENT_ID
		 */
		public static final String COLUMN_DOCUMENT_ID = "document_id";

		/**
		 * Number of bytes available in this root. This column is optional, and
		 * may be {@code null} if unknown or unbounded.
		 * <p>
		 * Type: INTEGER (long)
		 */
		public static final String COLUMN_AVAILABLE_BYTES = "available_bytes";

		/**
		 * Capacity of a root in bytes. This column is optional, and may be
		 * {@code null} if unknown or unbounded.
		 * <p>
		 * Type: INTEGER (long)
		 */
		public static final String COLUMN_CAPACITY_BYTES = "capacity_bytes";

		/**
		 * MIME types supported by this root. This column is optional, and if
		 * {@code null} the root is assumed to support all MIME types. Multiple
		 * MIME types can be separated by a newline. For example, a root
		 * supporting audio might return "audio/*\napplication/x-flac".
		 * <p>
		 * Type: STRING
		 */
		public static final String COLUMN_MIME_TYPES = "mime_types";

		/**
		 * Query arguments supported by this root. This column is optional
		 * and related to {@link #COLUMN_FLAGS} and {@link #FLAG_SUPPORTS_SEARCH}.
		 * If the flags include {@link #FLAG_SUPPORTS_SEARCH}, and the column is
		 * {@code null}, the root is assumed to support {@link #QUERY_ARG_DISPLAY_NAME}
		 * search of {@link Document#COLUMN_DISPLAY_NAME}. Multiple query arguments
		 * can be separated by a newline. For example, a root supporting
		 * {@link #QUERY_ARG_MIME_TYPES} and {@link #QUERY_ARG_DISPLAY_NAME} might
		 * return "android:query-arg-mime-types\nandroid:query-arg-display-name".
		 * <p>
		 * Type: STRING
		 * @see #COLUMN_FLAGS
		 * @see #FLAG_SUPPORTS_SEARCH
		 * @see #QUERY_ARG_DISPLAY_NAME
		 * @see #QUERY_ARG_FILE_SIZE_OVER
		 * @see #QUERY_ARG_LAST_MODIFIED_AFTER
		 * @see #QUERY_ARG_MIME_TYPES
		 * @see DocumentsProvider#querySearchDocuments(String, String[],
		 *      Bundle)
		 */
		public static final String COLUMN_QUERY_ARGS = "query_args";

		/**
		 * MIME type for a root.
		 */
		public static final String MIME_TYPE_ITEM = "vnd.android.document/root";

		/**
		 * Flag indicating that at least one directory under this root supports
		 * creating content. Roots with this flag will be shown when an
		 * application interacts with {@link Intent#ACTION_CREATE_DOCUMENT}.
		 *
		 * @see #COLUMN_FLAGS
		 */
		public static final int FLAG_SUPPORTS_CREATE = 1;

		/**
		 * Flag indicating that this root offers content that is strictly local
		 * on the device. That is, no network requests are made for the content.
		 *
		 * @see #COLUMN_FLAGS
		 * @see Intent#EXTRA_LOCAL_ONLY
		 */
		public static final int FLAG_LOCAL_ONLY = 1 << 1;

		/**
		 * Flag indicating that this root can be queried to provide recently
		 * modified documents.
		 *
		 * @see #COLUMN_FLAGS
		 * @see DocumentsContract#buildRecentDocumentsUri(String, String)
		 * @see DocumentsProvider#queryRecentDocuments(String, String[])
		 */
		public static final int FLAG_SUPPORTS_RECENTS = 1 << 2;

		/**
		 * Flag indicating that this root supports search.
		 *
		 * @see #COLUMN_FLAGS
		 * @see DocumentsContract#buildSearchDocumentsUri(String, String,
		 *      String)
		 * @see DocumentsProvider#querySearchDocuments(String, String,
		 *      String[])
		 * @see DocumentsProvider#querySearchDocuments(String, String[],
		 *      Bundle)
		 */
		public static final int FLAG_SUPPORTS_SEARCH = 1 << 3;

		/**
		 * Flag indicating that this root supports testing parent child
		 * relationships.
		 *
		 * @see #COLUMN_FLAGS
		 * @see DocumentsProvider#isChildDocument(String, String)
		 */
		public static final int FLAG_SUPPORTS_IS_CHILD = 1 << 4;

		/**
		 * Flag indicating that this root can be ejected.
		 *
		 * @see #COLUMN_FLAGS
		 * @see DocumentsContract#ejectRoot(ContentResolver, Uri)
		 * @see DocumentsProvider#ejectRoot(String)
		 */
		public static final int FLAG_SUPPORTS_EJECT = 1 << 5;

		/**
		 * Flag indicating that this root is currently empty. This may be used
		 * to hide the root when opening documents, but the root will still be
		 * shown when creating documents and {@link #FLAG_SUPPORTS_CREATE} is
		 * also set. If the value of this flag changes, such as when a root
		 * becomes non-empty, you must send a content changed notification for
		 * {@link DocumentsContract#buildRootsUri(String)}.
		 *
		 * @see #COLUMN_FLAGS
		 * @see ContentResolver#notifyChange(Uri,
		 *      android.database.ContentObserver, boolean)
		 */
		public static final int FLAG_EMPTY = 1 << 6;

		/**
		 * Flag indicating that this root should only be visible to advanced
		 * users.
		 *
		 * @see #COLUMN_FLAGS
		 * {@hide}
		 */
		@SystemApi
		public static final int FLAG_ADVANCED = 1 << 16;

		/**
		 * Flag indicating that this root has settings.
		 *
		 * @see #COLUMN_FLAGS
		 * @see DocumentsContract#ACTION_DOCUMENT_ROOT_SETTINGS
		 * {@hide}
		 */
		@SystemApi
		public static final int FLAG_HAS_SETTINGS = 1 << 17;

		/**
		 * Flag indicating that this root is on removable SD card storage.
		 *
		 * @see #COLUMN_FLAGS
		 * {@hide}
		 */
		@SystemApi
		public static final int FLAG_REMOVABLE_SD = 1 << 18;

		/**
		 * Flag indicating that this root is on removable USB storage.
		 *
		 * @see #COLUMN_FLAGS
		 * {@hide}
		 */
		@SystemApi
		public static final int FLAG_REMOVABLE_USB = 1 << 19;
	}

	/**
	 * Build URI representing the roots of a document provider. When queried, a
	 * provider will return one or more rows with columns defined by
	 * {@link Root}.
	 *
	 * @see DocumentsProvider#queryRoots(String[])
	 */
	public static Uri buildRootsUri(String authority) {
		return new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT)
				.authority(authority).appendPath(PATH_ROOT).build();
	}

	/**
	 * Build URI representing the given {@link Root#COLUMN_ROOT_ID} in a
	 * document provider.
	 *
	 * @see #getRootId(Uri)
	 */
	public static Uri buildRootUri(String authority, String rootId) {
		return new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT)
				.authority(authority).appendPath(PATH_ROOT).appendPath(rootId).build();
	}

	/**
	 * Build URI representing access to descendant documents of the given
	 * {@link Document#COLUMN_DOCUMENT_ID}.
	 *
	 * @see #getTreeDocumentId(Uri)
	 */
	public static Uri buildTreeDocumentUri(String authority, String documentId) {
		return new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT).authority(authority)
				.appendPath(PATH_TREE).appendPath(documentId).build();
	}

	/** {@hide} */
	public static Uri buildDocumentUriMaybeUsingTree(Uri baseUri, String documentId) {
		if (isTreeUri(baseUri)) {
			return buildDocumentUriUsingTree(baseUri, documentId);
		} else {
			return buildDocumentUri(baseUri.getAuthority(), documentId);
		}
	}

	/**
	 * Extract the {@link Root#COLUMN_ROOT_ID} from the given URI.
	 */
	public static String getRootId(Uri rootUri) {
		final List<String> paths = rootUri.getPathSegments();
		if (paths.size() >= 2 && PATH_ROOT.equals(paths.get(0))) {
			return paths.get(1);
		}
		throw new IllegalArgumentException("Invalid URI: " + rootUri);
	}

	/**
	 * Extract the search query from a URI built by
	 * {@link #buildSearchDocumentsUri(String, String, String)}.
	 */
	public static String getSearchDocumentsQuery(Uri searchDocumentsUri) {
		return searchDocumentsUri.getQueryParameter(PARAM_QUERY);
	}

	/**
	 * Extract the search query from a Bundle
	 * {@link #QUERY_ARG_DISPLAY_NAME}.
	 * {@hide}
	 */
	public static String getSearchDocumentsQuery(@NonNull Bundle bundle) {
		Preconditions.checkNotNull(bundle, "bundle can not be null");
		return bundle.getString(QUERY_ARG_DISPLAY_NAME, "" /* defaultValue */);
	}

	/**
	 * Build URI that append the query parameter {@link PARAM_MANAGE} to
	 * enable the manage mode.
	 * @see DocumentsProvider#queryChildDocumentsForManage(String parentDocId, String[], String)
	 * {@hide}
	 */
	@SystemApi
	public static @NonNull Uri setManageMode(@NonNull Uri uri) {
		Preconditions.checkNotNull(uri, "uri can not be null");
		return uri.buildUpon().appendQueryParameter(PARAM_MANAGE, "true").build();
	}

	/**
	 * Extract the manage mode from a URI built by
	 * {@link #setManageMode(Uri)}.
	 * {@hide}
	 */
	@SystemApi
	public static boolean isManageMode(@NonNull Uri uri) {
		Preconditions.checkNotNull(uri, "uri can not be null");
		return uri.getBooleanQueryParameter(PARAM_MANAGE, false);
	}

	/**
	 * Holds a path from a document to a particular document under it. It
	 * may also contains the root ID where the path resides.
	 */
	public static final class Path implements Parcelable {

		private final @Nullable String mRootId;
		private final List<String> mPath;

		/**
		 * Creates a Path.
		 *
		 * @param rootId the ID of the root. May be null.
		 * @param path the list of document ID from the parent document at
		 *          position 0 to the child document.
		 */
		public Path(@Nullable String rootId, List<String> path) {
			checkCollectionNotEmpty(path, "path");
			checkCollectionElementsNotNull(path, "path");

			mRootId = rootId;
			mPath = path;
		}

		/**
		 * Returns the root id or null if the calling package doesn't have
		 * permission to access root information.
		 */
		public @Nullable String getRootId() {
			return mRootId;
		}

		/**
		 * Returns the path. The path is trimmed to the top of tree if
		 * calling package doesn't have permission to access those
		 * documents.
		 */
		public List<String> getPath() {
			return mPath;
		}

		@Override
		public boolean equals(@Nullable Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || !(o instanceof Path)) {
				return false;
			}
			Path path = (Path) o;
			return Objects.equals(mRootId, path.mRootId) &&
					Objects.equals(mPath, path.mPath);
		}

		@Override
		public int hashCode() {
			return Objects.hash(mRootId, mPath);
		}

		@Override
		public String toString() {
			return new StringBuilder()
					.append("DocumentsContract.Path{")
					.append("rootId=")
					.append(mRootId)
					.append(", path=")
					.append(mPath)
					.append("}")
					.toString();
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			dest.writeString(mRootId);
			dest.writeStringList(mPath);
		}

		@Override
		public int describeContents() {
			return 0;
		}

		public static final @android.annotation.NonNull Creator<Path> CREATOR = new Creator<Path>() {
			@Override
			public Path createFromParcel(Parcel in) {
				final String rootId = in.readString();
				final List<String> path = in.createStringArrayList();
				return new Path(rootId, path);
			}

			@Override
			public Path[] newArray(int size) {
				return new Path[size];
			}
		};
	}
}
