package android.os;

import junit.framework.TestCase;

public class ParcelMapTest extends TestCase {
	public void testParcelMapAndSparseArray() {
		Parcel p = Parcel.obtain();
		java.util.HashMap<String, Object> m = new java.util.HashMap<>();
		m.put("a", "b");
		p.writeMap(m);
		android.util.SparseArray<String> sa = new android.util.SparseArray<>();
		sa.put(3, "x");
		p.writeSparseArray(sa);
		p.setDataPosition(0);
		java.util.HashMap<String, Object> m2 = new java.util.HashMap<>();
		p.readMap(m2, null);
		assertEquals("b", m2.get("a"));
		android.util.SparseArray<String> sa2 = p.readSparseArray(null);
		assertEquals("x", sa2.get(3));
	}
}
