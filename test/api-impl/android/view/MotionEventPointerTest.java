package android.view;

import junit.framework.TestCase;

public class MotionEventPointerTest extends TestCase {
	public void testFindPointerIndexAndEdgeFlags() {
		MotionEvent e = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 5f, 6f, 0);
		assertEquals(0, e.findPointerIndex(0));
		assertEquals(-1, e.findPointerIndex(7));
		assertEquals(0, e.getEdgeFlags());
		e.setEdgeFlags(MotionEvent.EDGE_LEFT);
		assertEquals(MotionEvent.EDGE_LEFT, e.getEdgeFlags());
		assertEquals(MotionEvent.EDGE_LEFT, MotionEvent.obtain(e).getEdgeFlags());
	}
}
