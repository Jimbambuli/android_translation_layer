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
}
