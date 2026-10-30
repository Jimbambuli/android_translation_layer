package android.app;

import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;

/* MediaStore.ACTION_PICK_IMAGES: on Android, MediaProvider's photo picker answers it. ATL shows the GTK
 * file dialog instead (Activity.nativePickImages()); checking the request and building the result are
 * taken from the android-14 picker (packages/providers/MediaProvider, photopicker/). */
class PhotoPicker {

	/* photopicker/util/MimeFilterUtils.getMimeTypeFilters(); null means all images and videos */
	static String[] getMimeTypeFilters(Intent intent) throws IllegalArgumentException {
		// EXTRA_MIME_TYPES has higher priority over getType() filter.
		if (intent.hasExtra(Intent.EXTRA_MIME_TYPES)) {
			final String[] extraMimeTypes = intent.getStringArrayExtra(Intent.EXTRA_MIME_TYPES);

			if (requiresUnsupportedFilters(extraMimeTypes)) {
				throw new IllegalArgumentException("Invalid EXTRA_MIME_TYPES value, only media "
				                                   + "mime type filters are accepted");
			}

			return extraMimeTypes;
		}

		final String mimeType = intent.getType();
		if (isMimeTypeMedia(mimeType)) {
			return new String[] {mimeType};
		}

		return null;
	}

	/* MimeFilterUtils.isMimeTypeMedia() with util/MimeUtils.isImageMimeType()/isVideoMimeType() */
	private static boolean isMimeTypeMedia(String mimeType) {
		return startsWithIgnoreCase(mimeType, "image/") || startsWithIgnoreCase(mimeType, "video/");
	}

	/* util/MimeUtils.startsWithIgnoreCase() */
	private static boolean startsWithIgnoreCase(String target, String other) {
		if (target == null || other == null)
			return false;
		if (other.length() > target.length())
			return false;
		return target.regionMatches(true, 0, other, 0, other.length());
	}

	/* MimeFilterUtils.requiresUnsupportedFilters() */
	private static boolean requiresUnsupportedFilters(String[] mimeTypeFilters) {
		// no filters imply that we should show non-media files as well
		if (mimeTypeFilters == null || mimeTypeFilters.length == 0) {
			return true;
		}

		for (String mimeTypeFilter : mimeTypeFilters) {
			if (!isMimeTypeMedia(mimeTypeFilter)) {
				return true;
			}
		}

		return false;
	}

	/* photopicker/data/Selection.parseSelectionValuesFromIntent() for ACTION_PICK_IMAGES: how many items
	 * may be picked, 1 is single selection */
	static int getMaxSelectionLimit(Intent intent) throws IllegalArgumentException {
		final Bundle extras = intent.getExtras();
		final boolean isExtraPickImagesMaxSet =
		    extras != null && extras.containsKey(MediaStore.EXTRA_PICK_IMAGES_MAX);

		// Check EXTRA_PICK_IMAGES_MAX value only if the flag is set.
		if (isExtraPickImagesMaxSet) {
			final int extraMax =
			    intent.getIntExtra(MediaStore.EXTRA_PICK_IMAGES_MAX,
			                       /* defaultValue */ -1);
			// Multi selection max limit should always be greater than 1 and less than or equal
			// to PICK_IMAGES_MAX_LIMIT.
			if (extraMax <= 1 || extraMax > MediaStore.getPickImagesMaxLimit()) {
				throw new IllegalArgumentException("Invalid EXTRA_PICK_IMAGES_MAX value");
			}
			return extraMax;
		}
		return 1;
	}

	/* photopicker/data/PickerResult.getPickerResponseIntent(); like the picker, which lets the user
	 * select no more than the limit, only the first maxItems uris count */
	static Intent getPickerResponseIntent(int maxItems, String[] selectedUris) {
		final boolean canSelectMultiple = maxItems > 1;
		Intent intent = new Intent();
		final int size = Math.min(selectedUris.length, maxItems);
		if (size < 1) {
			return intent;
		}
		if (!canSelectMultiple) {
			intent.setData(Uri.parse(selectedUris[0]));
		}
		// TODO (b/169737761): use correct mime types
		String[] mimeTypes = new String[] {"image/*", "video/*"};
		final ClipData clipData = new ClipData(null /* label */, mimeTypes,
		                                       new ClipData.Item(Uri.parse(selectedUris[0])));
		for (int i = 1; i < size; i++) {
			clipData.addItem(new ClipData.Item(Uri.parse(selectedUris[i])));
		}
		intent.setClipData(clipData);
		intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);

		return intent;
	}
}
