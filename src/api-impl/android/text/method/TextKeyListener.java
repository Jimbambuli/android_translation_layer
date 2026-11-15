package android.text.method;

import android.content.Context;
import android.text.Editable;
import android.text.NoCopySpan;
import android.text.Selection;
import android.text.Spannable;
import android.text.SpanWatcher;
import android.text.TextUtils;

/* AOSP's TextKeyListener hands each key to QwertyKeyListener or MultiTapKeyListener, which insert the typed
 * characters into the Editable. ATL has neither (its text fields are GTK widgets): this one only does what
 * BaseKeyListener does (delete keys, meta keys) and leaves a printable key unhandled (false), so the caller
 * passes it on itself (GeckoView then sends it to Gecko). */
public class TextKeyListener extends BaseKeyListener implements SpanWatcher {

	/* AOSP TextKeyListener: spans and preference bits shared with QwertyKeyListener */
	static final Object ACTIVE = new NoCopySpan.Concrete();
	static final Object CAPPED = new NoCopySpan.Concrete();
	static final Object INHIBIT_REPLACEMENT = new NoCopySpan.Concrete();
	static final Object LAST_TYPED = new NoCopySpan.Concrete();

	static final int AUTO_CAP = 1;
	static final int AUTO_TEXT = 2;
	static final int AUTO_PERIOD = 4;
	static final int SHOW_PASSWORD = 8;

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

	/* AOSP TextKeyListener.getPrefs(): the bits come from Settings.System text_auto_caps, text_auto_replace,
	 * text_auto_punctuate and show_password, each 1 (on) when unset. ATL has none of these settings (no
	 * Settings.System.put*), so a fresh device's defaults apply. */
	public int getPrefs(Context context) {
		return AUTO_CAP | AUTO_TEXT | AUTO_PERIOD | SHOW_PASSWORD;
	}

	/* AOSP TextKeyListener.shouldCap() */
	public static boolean shouldCap(Capitalize cap, CharSequence cs, int off) {
		if (cap == Capitalize.NONE)
			return false;
		if (cap == Capitalize.CHARACTERS)
			return true;
		return TextUtils.getCapsMode(cs, off, cap == Capitalize.WORDS ? TextUtils.CAP_MODE_WORDS : TextUtils.CAP_MODE_SENTENCES) != 0;
	}

	/* AOSP TextKeyListener.clear() */
	public static void clear(Editable e) {
		e.clear();
		e.removeSpan(ACTIVE);
		e.removeSpan(CAPPED);
		e.removeSpan(INHIBIT_REPLACEMENT);
		e.removeSpan(LAST_TYPED);

		QwertyKeyListener.Replaced[] repl = e.getSpans(0, e.length(), QwertyKeyListener.Replaced.class);
		for (int i = 0; i < repl.length; i++)
			e.removeSpan(repl[i]);
	}

	@Override
	public void onSpanAdded(Spannable s, Object what, int start, int end) {}

	@Override
	public void onSpanRemoved(Spannable s, Object what, int start, int end) {}

	@Override
	public void onSpanChanged(Spannable s, Object what, int start, int end, int st, int en) {
		if (what == Selection.SELECTION_END)
			s.removeSpan(ACTIVE);
	}

	public enum Capitalize {
		NONE,
		SENTENCES,
		WORDS,
		CHARACTERS,
	}
}
