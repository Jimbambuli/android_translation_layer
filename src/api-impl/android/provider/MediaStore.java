package android.provider;

import android.content.ContentResolver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Size;
import java.io.IOException;

public class MediaStore {

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

	public static class Images {

		public static class Media {

			public static final Uri EXTERNAL_CONTENT_URI = Uri.parse("content://media/external/images/media");
			public static final Uri INTERNAL_CONTENT_URI = Uri.parse("content://media/internal/images/media");
		}

		public static class Thumbnails {

			public static final Uri EXTERNAL_CONTENT_URI = Uri.parse("content://media/external/images/media");

			public static final int MINI_KIND = ThumbnailConstants.MINI_KIND;
			public static final int FULL_SCREEN_KIND = ThumbnailConstants.FULL_SCREEN_KIND;
			public static final int MICRO_KIND = ThumbnailConstants.MICRO_KIND;

			public static Cursor queryMiniThumbnail(ContentResolver contentResolver, long id, int kind, String[] projection) {
				return null;
			}

			public static Bitmap getThumbnail(ContentResolver contentResolver, long imageId, long groupId, int kind, BitmapFactory.Options options) throws IOException {
				Uri uri = Media.EXTERNAL_CONTENT_URI.buildUpon().appendPath(String.valueOf(imageId)).build();
				try (ParcelFileDescriptor fd = contentResolver.openFileDescriptor(uri, "r")) {
					return BitmapFactory.decodeFileDescriptor(fd.getFileDescriptor(), null, options);
				}
			}
		}
	}

	public static class Video {

		public static class Media {

			public static final Uri EXTERNAL_CONTENT_URI = Uri.parse("content://media/external/video/media");
			public static final Uri INTERNAL_CONTENT_URI = Uri.parse("content://media/internal/video/media");
		}

		public static class Thumbnails {

			public static final Uri EXTERNAL_CONTENT_URI = Uri.parse("content://media/external/video/media");

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

			public static final Uri EXTERNAL_CONTENT_URI = Uri.parse("content://media/external/audio/media");
		}

		public static class Artists {
			public static final Uri EXTERNAL_CONTENT_URI = Uri.parse("content://media/external/audio/artists");
		}

		public static class Albums {
			public static final Uri EXTERNAL_CONTENT_URI = Uri.parse("content://media/external/audio/albums");
		}

		public static class Genres {
			public static final Uri EXTERNAL_CONTENT_URI = Uri.parse("content://media/external/audio/genres");
		}
	}

	public static class Files {

		public static Uri getContentUri(String type) {
			return Uri.parse("content://media/files/" + type);
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
}
