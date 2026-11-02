/*
 * Host test for KeyEvent.getUnicodeChar(): with Ctrl, Alt or Meta a key types no character, like AOSP's
 * KeyCharacterMap with Generic.kcm. Run like test/api-impl/android/app/job/JobSchedulerTest.java.
 */
package android.view;

import junit.framework.TestCase;

public class KeyEventTest extends TestCase {
	private static KeyEvent key(int code, int meta, int unicode) {
		KeyEvent event = new KeyEvent(0, 0, KeyEvent.ACTION_DOWN, code, 0, meta);
		event.unicodeValue = unicode; /* set by the native code from the GDK key */
		return event;
	}

	public void testPlainKeyTypesItsCharacter() {
		assertEquals('a', key(KeyEvent.KEYCODE_A, 0, 'a').getUnicodeChar());
		assertEquals('A', key(KeyEvent.KEYCODE_A, KeyEvent.META_SHIFT_ON, 'A').getUnicodeChar());
	}

	public void testShortcutTypesNoCharacter() {
		assertEquals(0, key(KeyEvent.KEYCODE_A, KeyEvent.META_CTRL_ON, 'a').getUnicodeChar());
		assertEquals(0, key(KeyEvent.KEYCODE_V, KeyEvent.META_CTRL_ON | KeyEvent.META_SHIFT_ON, 'V').getUnicodeChar());
		assertEquals(0, key(KeyEvent.KEYCODE_A, KeyEvent.META_ALT_ON, 'a').getUnicodeChar());
		assertEquals(0, key(KeyEvent.KEYCODE_A, KeyEvent.META_META_ON, 'a').getUnicodeChar());
	}
}
