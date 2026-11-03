/*
 * Host test for ClipData.newUri(): like AOSP, the description has the type the ContentResolver gives for a
 * content: URI (an image copied in an app goes to the desktop as an image), else text/uri-list. Run like
 * test/api-impl/android/os/SystemPropertiesTest.java.
 */
package android.content;

import android.net.Uri;
import junit.framework.TestCase;

public class ClipDataUriTest extends TestCase {
	private static class Types extends ContentResolver {
		Types() {
			super(null);
		}

		@Override
		public String getType(Uri uri) {
			return uri.getPath().endsWith(".png") ? "image/png" : null;
		}
	}

	public void testContentUriTakesTheResolversType() {
		ClipData clip = ClipData.newUri(new Types(), "Bild", Uri.parse("content://org.example.files/a.png"));
		assertEquals(1, clip.getDescription().getMimeTypeCount());
		assertEquals("image/png", clip.getDescription().getMimeType(0));
		assertTrue(clip.getDescription().hasMimeType("image/*"));
	}

	public void testUnknownTypeIsUriList() {
		ClipData clip = ClipData.newUri(new Types(), null, Uri.parse("content://org.example.files/a.bin"));
		assertEquals(ClipDescription.MIMETYPE_TEXT_URILIST, clip.getDescription().getMimeType(0));
		clip = ClipData.newUri(new Types(), null, Uri.parse("https://example.org/a.png"));
		assertEquals(ClipDescription.MIMETYPE_TEXT_URILIST, clip.getDescription().getMimeType(0));
	}
}
