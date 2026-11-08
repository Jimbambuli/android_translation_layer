/*
 * Host test for the Parcel contract of ComponentName and ColorStateList (CREATOR, writeToParcel, readFromParcel)
 * and for Parcel.readSerializable(ClassLoader, Class) (android-14). Pure Java, no native code; run like
 * PersistableBundleTest.
 */
package android.os;

import android.content.ComponentName;
import android.content.res.ColorStateList;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import java.util.ArrayList;
import java.util.Arrays;
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

	public void testGeometry() {
		Parcel p = Parcel.obtain();
		new Point(1, -2).writeToParcel(p, 0);
		new PointF(1.5f, -2.5f).writeToParcel(p, 0);
		new Rect(1, 2, 3, 4).writeToParcel(p, 0);
		new RectF(1.5f, 2.5f, 3.5f, 4.5f).writeToParcel(p, 0);
		p.writeTypedObject(new Rect(5, 6, 7, 8), 0);
		p.setDataPosition(0);
		assertEquals(new Point(1, -2), Point.CREATOR.createFromParcel(p));
		assertEquals(new PointF(1.5f, -2.5f), PointF.CREATOR.createFromParcel(p));
		assertEquals(new Rect(1, 2, 3, 4), Rect.CREATOR.createFromParcel(p));
		assertEquals(new RectF(1.5f, 2.5f, 3.5f, 4.5f), RectF.CREATOR.createFromParcel(p));
		assertEquals(new Rect(5, 6, 7, 8), p.readTypedObject(Rect.CREATOR));
		assertEquals(0, p.dataAvail());
	}

	public void testParcelableList() {
		Parcel p = Parcel.obtain();
		p.writeParcelableList(Arrays.asList(new Rect(1, 1, 2, 2), null), 0);
		p.writeParcelableList(null, 0);
		p.writeString8Array(new String[] {"a", "b"});
		p.setDataPosition(0);
		ArrayList<Rect> list = new ArrayList<>(Arrays.asList(new Rect(), new Rect(), new Rect()));
		ClassLoader cl = ClassLoader.getSystemClassLoader();
		assertSame(list, p.readParcelableList(list, cl, Rect.class));
		assertEquals(2, list.size());
		assertEquals(new Rect(1, 1, 2, 2), list.get(0));
		assertNull(list.get(1));
		p.readParcelableList(list, cl, Rect.class);
		assertEquals(0, list.size());
		assertTrue(Arrays.equals(new String[] {"a", "b"}, p.createString8Array()));
	}
}
