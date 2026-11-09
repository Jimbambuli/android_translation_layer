package android.database;

import android.os.Parcel;
import junit.framework.TestCase;

public class CursorWindowTest extends TestCase {
	public void testParcelRoundTrip() {
		CursorWindow w = new CursorWindow("test", 1024L);
		w.setNumColumns(5);
		w.setStartPosition(3);
		w.allocRow();
		w.putLong(42L, 3, 0);
		w.putDouble(1.5, 3, 1);
		w.putString("Grüezi 😀", 3, 2);
		w.putBlob(new byte[] {1, 2, 3}, 3, 3);
		w.putNull(3, 4);
		w.allocRow();
		w.putString("zwei", 4, 2);
		Parcel p = Parcel.obtain();
		w.writeToParcel(p, 0);
		p.setDataPosition(0);
		CursorWindow r = CursorWindow.CREATOR.createFromParcel(p);
		assertEquals("test", r.getName());
		assertEquals(3, r.getStartPosition());
		assertEquals(2, r.getNumRows());
		assertEquals(42L, r.getLong(3, 0));
		assertEquals(1.5, r.getDouble(3, 1), 0.0);
		assertEquals("Grüezi 😀", r.getString(3, 2));
		assertEquals(3, r.getBlob(3, 3).length);
		assertEquals(Cursor.FIELD_TYPE_NULL, r.getType(3, 4));
		assertEquals("zwei", r.getString(4, 2));
		assertEquals(0, CursorWindow.CREATOR.newArray(0).length);
		p.recycle();
	}
}
