package android.provider;

import android.content.ContentResolver;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.util.Size;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/* the URIs and helpers of AOSP 14's MediaStore; ATLMediaContentProvider answers them */
public class MediaStore {

	public static final String AUTHORITY = "media";
	public static final Uri AUTHORITY_URI = Uri.parse("content://" + AUTHORITY);

	/** @hide AOSP MediaStore call() method names and extras of the permission requests */
	public static final String CREATE_WRITE_REQUEST_CALL = "create_write_request";
	/** @hide */
	public static final String CREATE_TRASH_REQUEST_CALL = "create_trash_request";
	public static final String CREATE_FAVORITE_REQUEST_CALL = "create_favorite_request";
	public static final String GET_VERSION_CALL = "get_version";
	public static final String GET_GENERATION_CALL = "get_generation";
	public static final String GET_DOCUMENT_URI_CALL = "get_document_uri";
	public static final String GET_MEDIA_URI_CALL = "get_media_uri";
	public static final String EXTRA_URI = "uri";
	public static final String EXTRA_URI_PERMISSIONS = "uriPermissions";
	public static final String CREATE_DELETE_REQUEST_CALL = "create_delete_request";
	/** @hide */
	public static final String EXTRA_CLIP_DATA = "clip_data";
	/** @hide */
	public static final String EXTRA_CONTENT_VALUES = "content_values";
	/** @hide */
	public static final String EXTRA_RESULT = "result";

	public static final String VOLUME_INTERNAL = "internal";
	public static final String VOLUME_EXTERNAL = "external";
	public static final String VOLUME_EXTERNAL_PRIMARY = "external_primary";

	public static final String PARAM_INCLUDE_PENDING = "includePending";
	public static final String PARAM_LIMIT = "limit";
	public static final String PARAM_REQUIRE_ORIGINAL = "requireOriginal";

	public static final String QUERY_ARG_MATCH_PENDING = "android:query-arg-match-pending";
	public static final String QUERY_ARG_MATCH_TRASHED = "android:query-arg-match-trashed";
	public static final String QUERY_ARG_MATCH_FAVORITE = "android:query-arg-match-favorite";
	public static final int MATCH_DEFAULT = 0;
	public static final int MATCH_INCLUDE = 1;
	public static final int MATCH_EXCLUDE = 2;
	public static final int MATCH_ONLY = 3;

	public static final String UNKNOWN_STRING = "<unknown>";

	public static boolean getIncludePending(Uri uri) {
		return uri.getBooleanQueryParameter(PARAM_INCLUDE_PENDING, false);
	}

	/* AOSP MediaStore.setRequireOriginal()/getRequireOriginal(): ask for the file without location redaction;
	 * ATLMediaContentProvider never redacts, so every file is the original */
	public static Uri setRequireOriginal(Uri uri) {
		return uri.buildUpon().appendQueryParameter(PARAM_REQUIRE_ORIGINAL, "1").build();
	}

	public static boolean getRequireOriginal(Uri uri) {
		return uri.getBooleanQueryParameter(PARAM_REQUIRE_ORIGINAL, false);
	}

	public static String getVolumeName(Uri uri) {
		final List<String> segments = uri.getPathSegments();
		if (AUTHORITY.equals(uri.getAuthority()) && segments != null && segments.size() > 0) {
			return segments.get(0);
		}
		throw new IllegalArgumentException("Missing volume name: " + uri);
	}

	/* AOSP: the app op of MANAGE_MEDIA, which ATL does not have: its default, the permission */
	public static boolean canManageMedia(Context context) {
		return context.checkPermission("android.permission.MANAGE_MEDIA", android.os.Process.myPid(),
		                               context.getApplicationInfo().uid)
		    == android.content.pm.PackageManager.PERMISSION_GRANTED;
	}

	/* ATL has one external volume, the user's folders (ATLMediaContentProvider) */
	public static Set<String> getExternalVolumeNames(Context context) {
		return Collections.singleton(VOLUME_EXTERNAL_PRIMARY);
	}

	/** @hide */
	public static class ThumbnailConstants {
		public static final int MINI_KIND = 1;
		public static final int FULL_SCREEN_KIND = 2;
		public static final int MICRO_KIND = 3;

		public static final Size MINI_SIZE = new Size(512, 384);
		public static final Size FULL_SCREEN_SIZE = new Size(1024, 786);
		public static final Size MICRO_SIZE = new Size(96, 96);

		public static Size getKindSize(int kind) {
			if (kind == ThumbnailConstants.MICRO_KIND) {
				return ThumbnailConstants.MICRO_SIZE;
			} else if (kind == ThumbnailConstants.FULL_SCREEN_KIND) {
				return ThumbnailConstants.FULL_SCREEN_SIZE;
			} else if (kind == ThumbnailConstants.MINI_KIND) {
				return ThumbnailConstants.MINI_SIZE;
			} else {
				throw new IllegalArgumentException("Unsupported kind: " + kind);
			}
		}
	}

	/* AOSP MediaStore.InternalThumbnails (packages/providers/MediaProvider) */
	private static class InternalThumbnails {
		private static final java.util.HashMap<Uri, CancellationSignal> sPending = new java.util.HashMap<>();

		static Bitmap getThumbnail(ContentResolver cr, Uri uri, int kind, BitmapFactory.Options opts) {
			final android.util.Size size = ThumbnailConstants.getKindSize(kind);

			CancellationSignal signal = null;
			synchronized (sPending) {
				signal = sPending.get(uri);
				if (signal == null) {
					signal = new CancellationSignal();
					sPending.put(uri, signal);
				}
			}

			try {
				return cr.loadThumbnail(uri, size, signal);
			} catch (IOException e) {
				android.util.Log.w("MediaStore", "Failed to obtain thumbnail for " + uri, e);
				return null;
			} finally {
				synchronized (sPending) {
					sPending.remove(uri);
				}
			}
		}
	}

	public static class Images {

		public static class Media {

			public static final Bitmap getBitmap(ContentResolver cr, Uri url) throws FileNotFoundException, IOException {
				InputStream input = cr.openInputStream(url);
				Bitmap bitmap = BitmapFactory.decodeStream(input);
				input.close();
				return bitmap;
			}

			public static Uri getContentUri(String volumeName) {
				return AUTHORITY_URI.buildUpon().appendPath(volumeName).appendPath("images").appendPath("media").build();
			}

			public static Uri getContentUri(String volumeName, long id) {
				return ContentUris.withAppendedId(getContentUri(volumeName), id);
			}

			public static final Uri EXTERNAL_CONTENT_URI = getContentUri("external");
			public static final Uri INTERNAL_CONTENT_URI = getContentUri("internal");
			public static final String CONTENT_TYPE = "vnd.android.cursor.dir/image";
		}

		public static class Thumbnails {

			public static Uri getContentUri(String volumeName) {
				return AUTHORITY_URI.buildUpon().appendPath(volumeName).appendPath("images").appendPath("thumbnails").build();
			}

			public static final Uri EXTERNAL_CONTENT_URI = getContentUri("external");
			public static final Uri INTERNAL_CONTENT_URI = getContentUri("internal");

			public static final int MINI_KIND = ThumbnailConstants.MINI_KIND;
			public static final int FULL_SCREEN_KIND = ThumbnailConstants.FULL_SCREEN_KIND;
			public static final int MICRO_KIND = ThumbnailConstants.MICRO_KIND;

			public static Cursor queryMiniThumbnail(ContentResolver contentResolver, long id, int kind, String[] projection) {
				return null;
			}

			/* AOSP MediaStore.Images.Thumbnails.getThumbnail: both forms go to InternalThumbnails.getThumbnail
			 * (ContentResolver.loadThumbnail at the kind's size; null when that fails) */
			public static Bitmap getThumbnail(ContentResolver cr, long origId, int kind, BitmapFactory.Options options) {
				final Uri uri = ContentUris.withAppendedId(Media.EXTERNAL_CONTENT_URI, origId);
				return InternalThumbnails.getThumbnail(cr, uri, kind, options);
			}

			public static Bitmap getThumbnail(ContentResolver cr, long origId, long groupId, int kind, BitmapFactory.Options options) {
				return getThumbnail(cr, origId, kind, options);
			}
		}
	}

	public static class Video {

		public static class Media {

			public static Uri getContentUri(String volumeName) {
				return AUTHORITY_URI.buildUpon().appendPath(volumeName).appendPath("video").appendPath("media").build();
			}

			public static Uri getContentUri(String volumeName, long id) {
				return ContentUris.withAppendedId(getContentUri(volumeName), id);
			}

			public static final Uri EXTERNAL_CONTENT_URI = getContentUri("external");
			public static final Uri INTERNAL_CONTENT_URI = getContentUri("internal");
			public static final String CONTENT_TYPE = "vnd.android.cursor.dir/video";
		}

		public static class Thumbnails {

			public static Uri getContentUri(String volumeName) {
				return AUTHORITY_URI.buildUpon().appendPath(volumeName).appendPath("video").appendPath("thumbnails").build();
			}

			public static final Uri EXTERNAL_CONTENT_URI = getContentUri("external");
			public static final Uri INTERNAL_CONTENT_URI = getContentUri("internal");

			public static final int MINI_KIND = ThumbnailConstants.MINI_KIND;
			public static final int FULL_SCREEN_KIND = ThumbnailConstants.FULL_SCREEN_KIND;
			public static final int MICRO_KIND = ThumbnailConstants.MICRO_KIND;

			public static Bitmap getThumbnail(ContentResolver contentResolver, long videoId, int kind, BitmapFactory.Options options) throws IOException {
				Uri uri = Media.EXTERNAL_CONTENT_URI.buildUpon().appendPath(String.valueOf(videoId)).build();
				try (ParcelFileDescriptor fd = contentResolver.openFileDescriptor(uri, "r")) {
					return BitmapFactory.decodeFileDescriptor(fd.getFileDescriptor(), null, options);
				}
			}
		}
	}

	public static class Audio {

		public static class Media {

			public static Uri getContentUri(String volumeName) {
				return AUTHORITY_URI.buildUpon().appendPath(volumeName).appendPath("audio").appendPath("media").build();
			}

			public static Uri getContentUri(String volumeName, long id) {
				return ContentUris.withAppendedId(getContentUri(volumeName), id);
			}

			public static final Uri EXTERNAL_CONTENT_URI = getContentUri("external");
			public static final Uri INTERNAL_CONTENT_URI = getContentUri("internal");
			public static final String CONTENT_TYPE = "vnd.android.cursor.dir/audio";
			public static final String ENTRY_CONTENT_TYPE = "vnd.android.cursor.item/audio";
		}

		public static class Artists {

			public static Uri getContentUri(String volumeName) {
				return AUTHORITY_URI.buildUpon().appendPath(volumeName).appendPath("audio").appendPath("artists").build();
			}

			public static final Uri EXTERNAL_CONTENT_URI = getContentUri("external");
			public static final Uri INTERNAL_CONTENT_URI = getContentUri("internal");
			public static final String CONTENT_TYPE = "vnd.android.cursor.dir/artists";
			public static final String ENTRY_CONTENT_TYPE = "vnd.android.cursor.item/artist";
		}

		public static class Albums {

			public static Uri getContentUri(String volumeName) {
				return AUTHORITY_URI.buildUpon().appendPath(volumeName).appendPath("audio").appendPath("albums").build();
			}

			public static final Uri EXTERNAL_CONTENT_URI = getContentUri("external");
			public static final Uri INTERNAL_CONTENT_URI = getContentUri("internal");
			public static final String CONTENT_TYPE = "vnd.android.cursor.dir/albums";
			public static final String ENTRY_CONTENT_TYPE = "vnd.android.cursor.item/album";
		}

		public static class Genres {

			public static Uri getContentUri(String volumeName) {
				return AUTHORITY_URI.buildUpon().appendPath(volumeName).appendPath("audio").appendPath("genres").build();
			}

			public static final Uri EXTERNAL_CONTENT_URI = getContentUri("external");
			public static final Uri INTERNAL_CONTENT_URI = getContentUri("internal");
			public static final String CONTENT_TYPE = "vnd.android.cursor.dir/genre";
			public static final String ENTRY_CONTENT_TYPE = "vnd.android.cursor.item/genre";
		}

		public static class Playlists {

			public static Uri getContentUri(String volumeName) {
				return AUTHORITY_URI.buildUpon().appendPath(volumeName).appendPath("audio").appendPath("playlists").build();
			}

			public static final Uri EXTERNAL_CONTENT_URI = getContentUri("external");
			public static final Uri INTERNAL_CONTENT_URI = getContentUri("internal");
			public static final String CONTENT_TYPE = "vnd.android.cursor.dir/playlist";
			public static final String ENTRY_CONTENT_TYPE = "vnd.android.cursor.item/playlist";
		}
	}

	public static class Downloads {

		public static Uri getContentUri(String volumeName) {
			return AUTHORITY_URI.buildUpon().appendPath(volumeName).appendPath("downloads").build();
		}

		public static Uri getContentUri(String volumeName, long id) {
			return ContentUris.withAppendedId(getContentUri(volumeName), id);
		}

		public static final Uri EXTERNAL_CONTENT_URI = getContentUri("external");
		public static final Uri INTERNAL_CONTENT_URI = getContentUri("internal");
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/download";
	}

	public static class Files {

		public static Uri getContentUri(String volumeName) {
			return AUTHORITY_URI.buildUpon().appendPath(volumeName).appendPath("file").build();
		}

		public static Uri getContentUri(String volumeName, long rowId) {
			return ContentUris.withAppendedId(getContentUri(volumeName), rowId);
		}

		public static class FileColumns {
			public static final int MEDIA_TYPE_NONE = 0;
			public static final int MEDIA_TYPE_IMAGE = 1;
			public static final int MEDIA_TYPE_AUDIO = 2;
			public static final int MEDIA_TYPE_VIDEO = 3;
			public static final int MEDIA_TYPE_PLAYLIST = 4;
			public static final int MEDIA_TYPE_SUBTITLE = 5;
			public static final int MEDIA_TYPE_DOCUMENT = 6;
		}
	}

	private static final int PICK_IMAGES_MAX_LIMIT = 100;

	/**
	 * Activity Action: Allow the user to select images or videos provided by
	 * system and return it. This is different than {@link Intent#ACTION_PICK}
	 * and {@link Intent#ACTION_GET_CONTENT} in that
	 * <ul>
	 * <li> the data for this action is provided by the system
	 * <li> this action is only used for picking images and videos
	 * <li> caller gets read access to user picked items even without storage
	 * permissions
	 * </ul>
	 * <p>
	 * Callers can optionally specify MIME type (such as {@code image/*} or
	 * {@code video/*}), resulting in a range of content selection that the
	 * caller is interested in. The optional MIME type can be requested with
	 * {@link Intent#setType(String)}.
	 * <p>
	 * If the caller needs multiple returned items (or caller wants to allow
	 * multiple selection), then it can specify
	 * {@link MediaStore#EXTRA_PICK_IMAGES_MAX} to indicate this.
	 * <p>
	 * When the caller requests multiple selection, the value of
	 * {@link MediaStore#EXTRA_PICK_IMAGES_MAX} must be a positive integer
	 * greater than 1 and less than or equal to
	 * {@link MediaStore#getPickImagesMaxLimit}, otherwise
	 * {@link Activity#RESULT_CANCELED} is returned.
	 * <p>
	 * Output: MediaStore content URI(s) of the item(s) that was picked.
	 */
	public static final String ACTION_PICK_IMAGES = "android.provider.action.PICK_IMAGES";

	/**
	 * Activity Action: Launch settings controlling images or videos selection with
	 * {@link #ACTION_PICK_IMAGES}.
	 */
	public static final String ACTION_PICK_IMAGES_SETTINGS =
	    "android.provider.action.PICK_IMAGES_SETTINGS";

	/**
	 * The name of an optional intent-extra used to allow multiple selection of
	 * items and constrain maximum number of items that can be returned by
	 * {@link MediaStore#ACTION_PICK_IMAGES}, action may still return nothing
	 * (0 items) if the user chooses to cancel.
	 * <p>
	 * The value of this intent-extra should be a positive integer greater
	 * than 1 and less than or equal to
	 * {@link MediaStore#getPickImagesMaxLimit}, otherwise
	 * {@link Activity#RESULT_CANCELED} is returned.
	 */
	public final static String EXTRA_PICK_IMAGES_MAX = "android.provider.extra.PICK_IMAGES_MAX";

	/**
	 * The maximum limit for the number of items that can be selected using
	 * {@link MediaStore#ACTION_PICK_IMAGES} when launched in multiple selection mode.
	 * This can be used as a constant value for {@link MediaStore#EXTRA_PICK_IMAGES_MAX}.
	 */
	public static int getPickImagesMaxLimit() {
		return PICK_IMAGES_MAX_LIMIT;
	}

	/* AOSP MediaStore.createRequest(): the ClipData of the Uris and the ContentValues go to the media provider's
	 * call(); ATL has no media provider process, so ContentResolver.call() answers like on a device without it
	 * (IllegalArgumentException "Unknown authority") unless a provider is registered for "media" */
	private static PendingIntent createRequest(ContentResolver resolver, String method, Collection<Uri> uris, ContentValues values) {
		Objects.requireNonNull(resolver);
		Objects.requireNonNull(uris);

		final Iterator<Uri> it = uris.iterator();
		final ClipData clipData = ClipData.newRawUri(null, it.next());
		while (it.hasNext()) {
			clipData.addItem(new ClipData.Item(it.next()));
		}

		final Bundle extras = new Bundle();
		extras.putParcelable(EXTRA_CLIP_DATA, clipData);
		extras.putParcelable(EXTRA_CONTENT_VALUES, values);
		return (PendingIntent)resolver.call(AUTHORITY, method, null, extras).getParcelable(EXTRA_RESULT);
	}

	/* AOSP MediaStore.createWriteRequest() */
	public static PendingIntent createWriteRequest(ContentResolver resolver, Collection<Uri> uris) {
		return createRequest(resolver, CREATE_WRITE_REQUEST_CALL, uris, null);
	}

	/* AOSP MediaStore.createDeleteRequest() */
	public static PendingIntent createDeleteRequest(ContentResolver resolver, Collection<Uri> uris) {
		return createRequest(resolver, CREATE_DELETE_REQUEST_CALL, uris, null);
	}

	/* AOSP MediaStore.createTrashRequest() / createFavoriteRequest(): MediaColumns.IS_TRASHED / IS_FAVORITE are
	 * "is_trashed" / "is_favorite" */
	public static PendingIntent createTrashRequest(ContentResolver resolver, Collection<Uri> uris, boolean value) {
		final ContentValues values = new ContentValues();
		values.put("is_trashed", value ? 1 : 0);
		return createRequest(resolver, CREATE_TRASH_REQUEST_CALL, uris, values);
	}

	public static PendingIntent createFavoriteRequest(ContentResolver resolver, Collection<Uri> uris, boolean value) {
		final ContentValues values = new ContentValues();
		values.put("is_favorite", value ? 1 : 0);
		return createRequest(resolver, CREATE_FAVORITE_REQUEST_CALL, uris, values);
	}

	/* AOSP MediaStore.getVersion(): the media provider is reached through call() (acquireContentProviderClient
	 * + client.call() in AOSP, which ContentResolver.call() wraps) */
	public static String getVersion(Context context) {
		return getVersion(context, VOLUME_EXTERNAL_PRIMARY);
	}

	public static String getVersion(Context context, String volumeName) {
		final Bundle in = new Bundle();
		in.putString(android.content.Intent.EXTRA_TEXT, volumeName);
		final Bundle out = context.getContentResolver().call(AUTHORITY, GET_VERSION_CALL, null, in);
		return out.getString(android.content.Intent.EXTRA_TEXT);
	}

	public static long getGeneration(Context context, String volumeName) {
		return getGeneration(context.getContentResolver(), volumeName);
	}

	public static long getGeneration(ContentResolver resolver, String volumeName) {
		final Bundle in = new Bundle();
		in.putString(android.content.Intent.EXTRA_TEXT, volumeName);
		final Bundle out = resolver.call(AUTHORITY, GET_GENERATION_CALL, null, in);
		return out.getLong(android.content.Intent.EXTRA_INDEX);
	}

	private static Uri convertUri(Context context, String method, Uri uri) {
		final ContentResolver resolver = context.getContentResolver();
		final Bundle in = new Bundle();
		in.putParcelable(EXTRA_URI, uri);
		in.putParcelableArrayList(EXTRA_URI_PERMISSIONS, new ArrayList<>(resolver.getPersistedUriPermissions()));
		return (Uri)resolver.call(AUTHORITY, method, null, in).getParcelable(EXTRA_URI);
	}

	/* AOSP MediaStore.getDocumentUri() / getMediaUri() */
	public static Uri getDocumentUri(Context context, Uri mediaUri) {
		return convertUri(context, GET_DOCUMENT_URI_CALL, mediaUri);
	}

	public static Uri getMediaUri(Context context, Uri documentUri) {
		return convertUri(context, GET_MEDIA_URI_CALL, documentUri);
	}
}
