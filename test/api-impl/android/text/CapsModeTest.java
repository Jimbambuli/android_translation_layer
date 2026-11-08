package android.text;

import junit.framework.TestCase;

public class CapsModeTest extends TestCase {
	public void testCapsMode() {
		assertEquals(TextUtils.CAP_MODE_SENTENCES, TextUtils.getCapsMode("Ein Satz. ", 10, TextUtils.CAP_MODE_SENTENCES) & TextUtils.CAP_MODE_SENTENCES);
		assertEquals(0, TextUtils.getCapsMode("mitten im Satz", 5, TextUtils.CAP_MODE_SENTENCES));
		assertEquals(TextUtils.CAP_MODE_WORDS, TextUtils.getCapsMode("a b", 2, TextUtils.CAP_MODE_WORDS) & TextUtils.CAP_MODE_WORDS);
	}
}
