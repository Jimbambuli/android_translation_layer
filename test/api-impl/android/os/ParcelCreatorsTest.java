/*
 * Host test for the Parcel contract of ComponentName and ColorStateList (CREATOR, writeToParcel, readFromParcel)
 * and for Parcel.readSerializable(ClassLoader, Class) (android-14). Pure Java, no native code; run like
 * PersistableBundleTest.
 */
package android.os;

import android.content.ComponentName;
import android.content.res.ColorStateList;
import junit.framework.TestCase;

public class ParcelCreatorsTest extends TestCase {

	public void testComponentName() {
		ComponentName c = new ComponentName("a.b", "a.b.C");
		Parcel p = Parcel.obtain();
		c.writeToParcel(p, 0);
		ComponentName.writeToParcel(null, p);
		ComponentName.writeToParcel(c, p);
		p.writeParcelable(c, 0);
		p.setDataPosition(0);
		assertEquals(c, ComponentName.CREATOR.createFromParcel(p));
		assertNull(ComponentName.readFromParcel(p));
		assertEquals(c, ComponentName.readFromParcel(p));
		assertEquals(c, p.readParcelable(ClassLoader.getSystemClassLoader()));
		assertEquals(0, p.dataAvail());
		assertEquals(2, ComponentName.CREATOR.newArray(2).length);
	}

	public void testComponentNameNull() {
		Parcel p = Parcel.obtain();
		p.writeString("x");
		p.writeString(null);
		p.setDataPosition(0);
		try {
			new ComponentName(p);
			fail();
		} catch (NullPointerException e) {
		}
	}

	public void testColorStateList() {
		ColorStateList c = new ColorStateList(new int[][] {{1, -2}, {}}, new int[] {0xff112233, 0xff445566});
		Parcel p = Parcel.obtain();
		c.writeToParcel(p, 0);
		p.setDataPosition(0);
		ColorStateList r = ColorStateList.CREATOR.createFromParcel(p);
		assertEquals(c.getColors().length, r.getColors().length);
		assertEquals(c.getDefaultColor(), r.getDefaultColor());
		assertEquals(c.getColorForState(new int[] {1}, 0), r.getColorForState(new int[] {1}, 0));
		assertEquals(0, p.dataAvail());
	}

	public void testReadSerializableClass() throws Exception {
		Parcel p = Parcel.obtain();
		p.writeSerializable("text");
		p.writeSerializable(null);
		p.writeSerializable("text");
		p.setDataPosition(0);
		assertEquals("text", p.readSerializable(null, String.class));
		assertNull(p.readSerializable(null, String.class));
		try {
			p.readSerializable(null, Integer.class);
			fail();
		} catch (BadParcelableException e) {
		}
	}
}
