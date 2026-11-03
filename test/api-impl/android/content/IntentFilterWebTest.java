/*
 * Host test for the data authorities of IntentFilter and handlesWebUris(), which the domain verification state
 * is collected from. Run like test/api-impl/android/os/SystemPropertiesTest.java.
 */
package android.content;

import junit.framework.TestCase;

public class IntentFilterWebTest extends TestCase {
	private static IntentFilter browsable(String... schemes) {
		IntentFilter f = new IntentFilter(Intent.ACTION_VIEW);
		f.addCategory(Intent.CATEGORY_DEFAULT);
		f.addCategory(Intent.CATEGORY_BROWSABLE);
		for (String s : schemes)
			f.addDataScheme(s);
		return f;
	}

	public void testAuthoritiesAreKept() {
		IntentFilter f = browsable("http", "https");
		f.addDataAuthority("play.google.com", null);
		f.addDataAuthority("*.example.org", "8080");
		assertEquals(2, f.countDataAuthorities());
		assertEquals("play.google.com", f.getDataAuthority(0).getHost());
		assertEquals(-1, f.getDataAuthority(0).getPort());
		assertEquals("*.example.org", f.getDataAuthority(1).getHost());
		assertEquals(8080, f.getDataAuthority(1).getPort());
		assertTrue(f.authoritiesIterator().hasNext());
	}

	public void testHandlesWebUris() {
		assertTrue(browsable("http", "https").handlesWebUris(false));
		assertTrue(browsable("https").handlesWebUris(true));
		assertTrue(browsable("market", "https").handlesWebUris(false));
		assertFalse(browsable("market", "https").handlesWebUris(true));
		assertFalse(browsable("market").handlesWebUris(false));
		assertFalse(browsable().handlesWebUris(false));
		IntentFilter notBrowsable = new IntentFilter(Intent.ACTION_VIEW);
		notBrowsable.addDataScheme("https");
		assertFalse(notBrowsable.handlesWebUris(false));
	}

	private static int match(IntentFilter f, String uri, String type, String... categories) {
		android.net.Uri data = uri != null ? android.net.Uri.parse(uri) : null;
		java.util.Set<String> cats = new java.util.HashSet<>(java.util.Arrays.asList(categories));
		return f.match(Intent.ACTION_VIEW, type, data != null ? data.getScheme() : null, data, cats, "test");
	}

	public void testMatchHostAndPathPrefix() { // JW Library: <data android:scheme="https" android:host="www.jw.org" android:pathPrefix="/finder"/>
		IntentFilter f = browsable("https");
		f.addDataAuthority("www.jw.org", null);
		f.addDataPath("/finder", android.os.PatternMatcher.PATTERN_PREFIX);
		assertEquals(IntentFilter.MATCH_CATEGORY_PATH + IntentFilter.MATCH_ADJUSTMENT_NORMAL,
		             match(f, "https://www.jw.org/finder?wtlocale=X&docid=1", null, Intent.CATEGORY_BROWSABLE));
		assertEquals(IntentFilter.NO_MATCH_DATA, match(f, "https://www.jw.org/de/", null));
		assertEquals(IntentFilter.NO_MATCH_DATA, match(f, "https://jw.org/finder", null));
		assertEquals(IntentFilter.NO_MATCH_DATA, match(f, "http://www.jw.org/finder", null));
	}

	public void testMatchWildcardHostAndCategories() {
		IntentFilter f = browsable("https");
		f.addDataAuthority("*.youtube.com", null);
		assertEquals(IntentFilter.MATCH_CATEGORY_HOST + IntentFilter.MATCH_ADJUSTMENT_NORMAL,
		             match(f, "https://m.youtube.com/watch?v=x", null, Intent.CATEGORY_BROWSABLE, Intent.CATEGORY_DEFAULT));
		assertEquals(IntentFilter.NO_MATCH_CATEGORY, match(f, "https://m.youtube.com/watch?v=x", null, "android.intent.category.TEST"));
		assertEquals(IntentFilter.NO_MATCH_ACTION, f.match(Intent.ACTION_SEND, null, "https", android.net.Uri.parse("https://m.youtube.com/"), null, "test"));
	}

	public void testMatchOwnSchemeAndType() throws Exception {
		assertEquals(IntentFilter.MATCH_CATEGORY_SCHEME + IntentFilter.MATCH_ADJUSTMENT_NORMAL,
		             match(browsable("jwlibrary"), "jwlibrary:///finder?bible=1", null));
		IntentFilter typed = browsable("content", "file");
		typed.addDataType("video/*");
		assertEquals(IntentFilter.MATCH_CATEGORY_TYPE + IntentFilter.MATCH_ADJUSTMENT_NORMAL, match(typed, "file:///a.mp4", "video/mp4"));
		assertEquals(IntentFilter.NO_MATCH_TYPE, match(typed, "file:///a.mp4", null));
		assertEquals(IntentFilter.NO_MATCH_TYPE, match(browsable("file"), "file:///a.mp4", "video/mp4"));
	}

	public void testAutoVerify() {
		IntentFilter f = browsable("https");
		assertFalse(f.getAutoVerify());
		f.setAutoVerify(true);
		assertTrue(f.getAutoVerify());
	}
}
