/*
 * Host test for the content URIs of files the desktop hands to an app (dropped or pasted files), which name the
 * file by its path like AOSP's DownloadsProvider ("raw:" document IDs). Run like
 * test/api-impl/android/os/SystemPropertiesTest.java. Reading through the URI needs ATL's natives (an app run).
 */
package android.content;

import android.net.Uri;
import junit.framework.TestCase;

public class ContentResolverFileUriTest extends TestCase {
	public void testUriNamesTheFileByPath() {
		Uri uri = ContentResolver.atlUriForFile("/tmp/a b/c.png");
		assertEquals("content://com.android.providers.downloads.documents/document/raw%3A%2Ftmp%2Fa%20b%2Fc.png", uri.toString());
	}
}
