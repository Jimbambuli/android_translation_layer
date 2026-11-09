package android.text;

import android.os.Parcel;
import junit.framework.TestCase;

public class AnnotationTest extends TestCase {
	public void testKeyValueAndParcel() {
		Annotation a = new Annotation("font", "title");
		assertEquals("font", a.getKey());
		assertEquals("title", a.getValue());
		assertEquals(TextUtils.ANNOTATION, a.getSpanTypeId());
		Parcel p = Parcel.obtain();
		a.writeToParcel(p, 0);
		p.setDataPosition(0);
		Annotation b = new Annotation(p);
		p.recycle();
		assertEquals("font", b.getKey());
		assertEquals("title", b.getValue());
	}
}
