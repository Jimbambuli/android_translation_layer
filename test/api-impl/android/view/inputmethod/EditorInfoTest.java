/*
 * Host test for the initial surrounding text and handwriting gestures of AOSP's EditorInfo (API 30/34), which
 * Compose fills in onCreateInputConnection(). Run like test/api-impl/android/app/job/JobSchedulerTest.java.
 */
package android.view.inputmethod;

import java.util.Arrays;
import java.util.List;
import junit.framework.TestCase;

public class EditorInfoTest extends TestCase {
	public void testInitialSurroundingSubText() {
		EditorInfo info = new EditorInfo();
		info.initialSelStart = 14;
		info.initialSelEnd = 16;
		/* the field holds "0123456789test@example.org", the app passes the part from offset 10 */
		info.setInitialSurroundingSubText("test@example.org", 10);
		assertEquals("test", info.getInitialTextBeforeCursor(10, 0).toString());
		assertEquals("@e", info.getInitialSelectedText(0).toString());
		assertEquals("xample.org", info.getInitialTextAfterCursor(20, 0).toString());
		SurroundingText text = info.getInitialSurroundingText(2, 3, 0);
		assertEquals("st@exam", text.getText().toString());
		assertEquals(2, text.getSelectionStart());
		assertEquals(4, text.getSelectionEnd());
		assertEquals(12, text.getOffset());
	}

	public void testPasswordHasNoSurroundingText() {
		EditorInfo info = new EditorInfo();
		info.inputType = EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_VARIATION_PASSWORD;
		info.initialSelStart = 3;
		info.initialSelEnd = 3;
		info.setInitialSurroundingText("secret");
		assertNull(info.getInitialTextBeforeCursor(10, 0));
	}

	public void testHandwritingGesturesRoundTrip() {
		EditorInfo info = new EditorInfo();
		List<Class<? extends HandwritingGesture>> gestures = Arrays.asList(SelectGesture.class, DeleteGesture.class, JoinOrSplitGesture.class);
		info.setSupportedHandwritingGestures(gestures);
		assertEquals(gestures, info.getSupportedHandwritingGestures());
		info.setSupportedHandwritingGesturePreviews(new java.util.HashSet<Class<? extends PreviewableHandwritingGesture>>(Arrays.asList(SelectGesture.class)));
		assertTrue(info.getSupportedHandwritingGesturePreviews().contains(SelectGesture.class));
	}
}
