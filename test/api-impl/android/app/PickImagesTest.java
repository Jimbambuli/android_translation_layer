/*
 * Host test for MediaStore.ACTION_PICK_IMAGES: the MediaStore API the androidx photo picker
 * contracts call (PickVisualMedia / PickMultipleVisualMedia) and how Activity checks the request
 * (PhotoPicker) and builds the result, as MediaProvider's photo picker does (android-14).
 * Pure Java, no native code. From the build dir:
 *   javac -source 8 -target 8 -bootclasspath $PREFIX/lib64/java/core-all_classes.jar \
 *     -cp src/api-impl/hax.jar:$PREFIX/lib64/java/core-junit_classes.jar -d t ../test/api-impl/android/app/PickImagesTest.java
 *   dx --dex --output=t.jar t
 *   dalvikvm -Xbootclasspath/a:t.jar:api-impl.jar junit.textui.TestRunner android.app.PickImagesTest
 * (junit is on the boot class path, so the test and api-impl.jar go there too)
 */
package android.app;

import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;

import java.util.Arrays;

import junit.framework.TestCase;

public class PickImagesTest extends TestCase {

	public void testMediaStoreApi() {
		assertEquals("android.provider.action.PICK_IMAGES", MediaStore.ACTION_PICK_IMAGES);
		assertEquals("android.provider.extra.PICK_IMAGES_MAX", MediaStore.EXTRA_PICK_IMAGES_MAX);
		assertEquals(100, MediaStore.getPickImagesMaxLimit());
	}

	private static Intent pick() {
		return new Intent(MediaStore.ACTION_PICK_IMAGES);
	}

	/* MimeFilterUtils.getMimeTypeFilters(): no or non-media type = all images and videos */
	public void testMimeTypes() {
		assertNull(PhotoPicker.getMimeTypeFilters(pick()));
		assertNull(PhotoPicker.getMimeTypeFilters(pick().setType("*/*")));
		assertTrue(Arrays.equals(new String[] {"image/*"}, PhotoPicker.getMimeTypeFilters(pick().setType("image/*"))));
		assertTrue(Arrays.equals(new String[] {"video/mp4"}, PhotoPicker.getMimeTypeFilters(pick().setType("video/mp4"))));
		String[] both = {"image/*", "video/*"};
		assertTrue(Arrays.equals(both, PhotoPicker.getMimeTypeFilters(pick().setType("*/*").putExtra(Intent.EXTRA_MIME_TYPES, both))));
		try {
			PhotoPicker.getMimeTypeFilters(pick().putExtra(Intent.EXTRA_MIME_TYPES, new String[] {"image/*", "application/pdf"}));
			fail("non-media EXTRA_MIME_TYPES accepted");
		} catch (IllegalArgumentException expected) {
		}
	}

	/* Selection.parseSelectionValuesFromIntent(): 1 = single selection, otherwise 2..getPickImagesMaxLimit() */
	public void testMaxItems() {
		assertEquals(1, PhotoPicker.getMaxSelectionLimit(pick()));
		assertEquals(5, PhotoPicker.getMaxSelectionLimit(pick().putExtra(MediaStore.EXTRA_PICK_IMAGES_MAX, 5)));
		assertEquals(100, PhotoPicker.getMaxSelectionLimit(pick().putExtra(MediaStore.EXTRA_PICK_IMAGES_MAX, MediaStore.getPickImagesMaxLimit())));
		for (int bad : new int[] {1, 0, -1, 101}) {
			try {
				PhotoPicker.getMaxSelectionLimit(pick().putExtra(MediaStore.EXTRA_PICK_IMAGES_MAX, bad));
				fail("EXTRA_PICK_IMAGES_MAX " + bad + " accepted");
			} catch (IllegalArgumentException expected) {
			}
		}
	}

	/* PickerResult.getPickerResponseIntent() */
	public void testSingleResult() {
		Intent r = PhotoPicker.getPickerResponseIntent(1, new String[] {"file:///tmp/a.jpg"});
		assertEquals(Uri.parse("file:///tmp/a.jpg"), r.getData());
		ClipData clip = r.getClipData();
		assertNotNull(clip);
		assertEquals(1, clip.getItemCount());
		assertEquals(Uri.parse("file:///tmp/a.jpg"), clip.getItemAt(0).getUri());
		assertEquals(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION, r.getFlags());
	}

	public void testMultipleResult() {
		Intent r = PhotoPicker.getPickerResponseIntent(2, new String[] {"file:///tmp/a.jpg", "file:///tmp/b.mp4", "file:///tmp/c.png"});
		assertNull(r.getData());
		ClipData clip = r.getClipData();
		assertEquals(2, clip.getItemCount()); // the picker lets the user pick no more than the limit
		assertEquals(Uri.parse("file:///tmp/a.jpg"), clip.getItemAt(0).getUri());
		assertEquals(Uri.parse("file:///tmp/b.mp4"), clip.getItemAt(1).getUri());
	}
}
