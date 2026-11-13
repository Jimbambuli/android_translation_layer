package android.text.method;

import android.text.Editable;

/* AOSP's TextKeyListener hands each key to QwertyKeyListener or MultiTapKeyListener, which insert the typed
 * characters into the Editable. ATL has neither (its text fields are GTK widgets): this one only does what
 * BaseKeyListener does (delete keys, meta keys) and leaves a printable key unhandled (false), so the caller
 * passes it on itself (GeckoView then sends it to Gecko). */
public class TextKeyListener extends BaseKeyListener {

	private static TextKeyListener[] instances = new TextKeyListener[Capitalize.values().length * 2];

	private final Capitalize autoCap;
	private final boolean autoText;

	public TextKeyListener(Capitalize cap, boolean autotext) {
		autoCap = cap;
		autoText = autotext;
	}

	public static TextKeyListener getInstance(boolean autotext, Capitalize cap) {
		int off = cap.ordinal() * 2 + (autotext ? 1 : 0);
		if (instances[off] == null)
			instances[off] = new TextKeyListener(cap, autotext);
		return instances[off];
	}

	public static TextKeyListener getInstance() {
		return getInstance(false, Capitalize.NONE);
	}

	@Override
	public int getInputType() {
		return makeTextContentType(autoCap, autoText);
	}

	public static void clear(Editable content) {}

	public enum Capitalize {
		NONE,
		SENTENCES,
		WORDS,
		CHARACTERS,
	}
}
