package android.text.method;

import android.text.InputType;
import android.text.SpannableStringBuilder;
import junit.framework.TestCase;

public class KeyListenersTest extends TestCase {
	public void testDigitsInputType() {
		assertEquals(InputType.TYPE_CLASS_NUMBER, DigitsKeyListener.getInstance(false, false).getInputType());
		assertEquals(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED, DigitsKeyListener.getInstance(true, false).getInputType());
		assertEquals(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL, DigitsKeyListener.getInstance(false, true).getInputType());
	}

	public void testDigitsFilter() {
		DigitsKeyListener l = DigitsKeyListener.getInstance("0123456789");
		assertNull(l.filter("123", 0, 3, new SpannableStringBuilder(), 0, 0));
		assertEquals("12", l.filter("1a2", 0, 3, new SpannableStringBuilder(), 0, 0).toString());
		assertEquals("", l.filter("abc", 0, 3, new SpannableStringBuilder(), 0, 0).toString());
	}
}
