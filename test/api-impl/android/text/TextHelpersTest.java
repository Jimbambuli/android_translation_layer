/*
 * Host test for TextDirectionHeuristics (strong characters decide), TextUtils.expandTemplate() and CaptioningManager.CaptionStyle.
 * Pure Java. Run like test/api-impl/android/app/job/JobSchedulerTest.java.
 */
package android.text;

import android.view.accessibility.CaptioningManager;
import junit.framework.TestCase;

public class TextHelpersTest extends TestCase {

	public void testFirstStrong() {
		String hebrew = "אב";
		String latin = "ab";
		assertTrue(TextDirectionHeuristics.FIRSTSTRONG_LTR.isRtl(hebrew, 0, hebrew.length()));
		assertFalse(TextDirectionHeuristics.FIRSTSTRONG_LTR.isRtl(latin, 0, latin.length()));
		assertFalse(TextDirectionHeuristics.FIRSTSTRONG_LTR.isRtl("123", 0, 3));
		assertTrue(TextDirectionHeuristics.FIRSTSTRONG_RTL.isRtl("123", 0, 3));
		assertTrue(TextDirectionHeuristics.ANYRTL_LTR.isRtl("ab" + hebrew, 0, 4));
		assertFalse(TextDirectionHeuristics.LTR.isRtl(hebrew, 0, 2));
		assertTrue(TextDirectionHeuristics.RTL.isRtl(latin, 0, 2));
	}

	public void testExpandTemplate() {
		assertEquals("a-X-b-Y", TextUtils.expandTemplate("a-^1-b-^2", "X", "Y").toString());
		assertEquals("^1", TextUtils.expandTemplate("^^1", "X").toString());
		try {
			TextUtils.expandTemplate("^3", "X");
			fail("a value that is not given");
		} catch (IllegalArgumentException expected) {
		}
	}

	public void testCaptionStyle() {
		CaptioningManager.CaptionStyle d = CaptioningManager.CaptionStyle.DEFAULT;
		assertEquals(0xFFFFFFFF, d.foregroundColor);
		assertEquals(0xFF000000, d.backgroundColor);
		assertEquals(CaptioningManager.CaptionStyle.EDGE_TYPE_NONE, d.edgeType);
		CaptioningManager.CaptionStyle unspecified = CaptioningManager.CaptionStyle.PRESETS[4];
		assertFalse(unspecified.hasForegroundColor());
		assertEquals(d.foregroundColor, d.applyStyle(unspecified).foregroundColor);
	}
}
