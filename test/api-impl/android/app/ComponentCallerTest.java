package android.app;

import android.content.pm.PackageManager;
import android.net.Uri;
import junit.framework.TestCase;

public class ComponentCallerTest extends TestCase {
	public void testUnknownCaller() {
		ComponentCaller caller = new ComponentCaller(null, null);
		assertEquals(-1, caller.getUid());
		assertNull(caller.getPackage());
		assertEquals(PackageManager.PERMISSION_DENIED, caller.checkContentUriPermission(Uri.parse("content://x/y"), 1));
	}

	public void testEquality() {
		ComponentCaller a = new ComponentCaller(null, null);
		ComponentCaller b = new ComponentCaller(null, null);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertFalse(a.equals(null));
		assertFalse(a.equals("x"));
	}
}
