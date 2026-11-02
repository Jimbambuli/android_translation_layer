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
}
