/*
 * Host test for WindowManager.LayoutParams (AOSP constants, constructors, copyFrom() change mask, Parcel round trip) and the
 * Parcel round trip of KeyEvent and AccessibilityEvent. Pure Java. Run like test/api-impl/android/app/job/JobSchedulerTest.java.
 */
package android.view;

import android.os.Parcel;
import android.view.accessibility.AccessibilityEvent;
import junit.framework.TestCase;

public class WindowLayoutParamsTest extends TestCase {

	public void testConstantsAndConstructors() {
		assertEquals(0x80, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		assertEquals(0x00080000, WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
		WindowManager.LayoutParams p = new WindowManager.LayoutParams(100, 200, 5, 6, WindowManager.LayoutParams.TYPE_APPLICATION, 0x10, -3);
		assertEquals(100, p.width);
		assertEquals(200, p.height);
		assertEquals(5, p.x);
		assertEquals(6, p.y);
		assertEquals(WindowManager.LayoutParams.TYPE_APPLICATION, p.type);
		assertEquals(0x10, p.flags);
		assertEquals(-3, p.format);
		assertEquals(1.0f, new WindowManager.LayoutParams().alpha);
	}

	public void testCopyFromChangeMask() {
		WindowManager.LayoutParams a = new WindowManager.LayoutParams();
		WindowManager.LayoutParams b = new WindowManager.LayoutParams();
		assertEquals(0, a.copyFrom(b));
		b.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND;
		b.preferredDisplayModeId = 3;
		b.setBlurBehindRadius(7);
		b.setTitle("title");
		int changes = a.copyFrom(b);
		assertTrue((changes & WindowManager.LayoutParams.LAYOUT_CHANGED) != 0 || (changes & WindowManager.LayoutParams.TITLE_CHANGED) != 0);
		assertEquals(WindowManager.LayoutParams.FLAG_DIM_BEHIND, a.flags);
		assertEquals(3, a.preferredDisplayModeId);
		assertEquals(7, a.getBlurBehindRadius());
		assertEquals("title", a.getTitle().toString());
	}

	public void testParcel() {
		WindowManager.LayoutParams a = new WindowManager.LayoutParams(300, 400, WindowManager.LayoutParams.TYPE_APPLICATION, 0x20, -1);
		a.setTitle("t");
		a.setFitInsetsSides(0);
		Parcel parcel = Parcel.obtain();
		a.writeToParcel(parcel, 0);
		parcel.setDataPosition(0);
		WindowManager.LayoutParams b = WindowManager.LayoutParams.CREATOR.createFromParcel(parcel);
		assertEquals("parcel must be fully read", parcel.dataSize(), parcel.dataPosition());
		parcel.recycle();
		assertEquals(300, b.width);
		assertEquals(400, b.height);
		assertEquals(0x20, b.flags);
		assertEquals("t", b.getTitle().toString());
		assertEquals(0, b.getFitInsetsSides());
	}

	public void testKeyEventParcel() {
		KeyEvent e = new KeyEvent(10, 20, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_A, 2, KeyEvent.META_SHIFT_ON);
		Parcel parcel = Parcel.obtain();
		e.writeToParcel(parcel, 0);
		parcel.setDataPosition(0);
		KeyEvent r = KeyEvent.CREATOR.createFromParcel(parcel);
		assertEquals("parcel must be fully read", parcel.dataSize(), parcel.dataPosition());
		parcel.recycle();
		assertEquals(KeyEvent.KEYCODE_A, r.getKeyCode());
		assertEquals(KeyEvent.ACTION_DOWN, r.getAction());
		assertEquals(2, r.getRepeatCount());
		assertEquals(KeyEvent.META_SHIFT_ON, r.getMetaState());
		assertEquals(10, r.getDownTime());
		assertEquals(20, r.getEventTime());
	}

	public void testAccessibilityEventParcel() {
		AccessibilityEvent e = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_VIEW_CLICKED);
		e.setClassName("a.B");
		e.setPackageName("a");
		e.getText().add("hello");
		e.setItemCount(3);
		e.setCurrentItemIndex(1);
		e.setEnabled(true);
		Parcel parcel = Parcel.obtain();
		e.writeToParcel(parcel, 0);
		parcel.setDataPosition(0);
		AccessibilityEvent r = AccessibilityEvent.CREATOR.createFromParcel(parcel);
		assertEquals("parcel must be fully read", parcel.dataSize(), parcel.dataPosition());
		parcel.recycle();
		assertEquals(AccessibilityEvent.TYPE_VIEW_CLICKED, r.getEventType());
		assertEquals("a.B", r.getClassName().toString());
		assertEquals("hello", r.getText().get(0).toString());
		assertEquals(3, r.getItemCount());
		assertEquals(1, r.getCurrentItemIndex());
		assertTrue(r.isEnabled());
	}
}
