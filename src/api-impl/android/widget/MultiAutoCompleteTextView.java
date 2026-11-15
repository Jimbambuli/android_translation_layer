package android.widget;

import android.content.Context;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.AttributeSet;

/* After AOSP core/java/android/widget/MultiAutoCompleteTextView.java: filtering, completion and validation work
 * on the token around the cursor, as found by the Tokenizer */
public class MultiAutoCompleteTextView extends AutoCompleteTextView {
	private Tokenizer mTokenizer;

	public MultiAutoCompleteTextView(Context context) {
		this(context, null);
	}

	public MultiAutoCompleteTextView(Context context, AttributeSet attrs) {
		this(context, attrs, com.android.internal.R.attr.autoCompleteTextViewStyle);
	}

	public MultiAutoCompleteTextView(Context context, AttributeSet attrs, int defStyleAttr) {
		this(context, attrs, defStyleAttr, 0);
	}

	public MultiAutoCompleteTextView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
	}

	public void setTokenizer(Tokenizer t) {
		mTokenizer = t;
	}

	@Override
	protected void performFiltering(CharSequence text, int keyCode) {
		if (enoughToFilter()) {
			int end = getSelectionEnd();
			int start = mTokenizer.findTokenStart(text, end);
			performFiltering(text, start, end, keyCode);
		} else {
			dismissDropDown();
			Filter f = getFilter();
			if (f != null)
				f.filter(null);
		}
	}

	@Override
	public boolean enoughToFilter() {
		Editable text = getText();
		int end = getSelectionEnd();
		if (end < 0 || mTokenizer == null)
			return false;
		int start = mTokenizer.findTokenStart(text, end);
		return end - start >= getThreshold();
	}

	@Override
	public void performValidation() {
		Validator v = getValidator();
		if (v == null || mTokenizer == null)
			return;
		Editable e = getText();
		boolean changed = false;
		int i = getText().length();
		while (i > 0) {
			int start = mTokenizer.findTokenStart(e, i);
			int end = mTokenizer.findTokenEnd(e, start);
			CharSequence sub = e.subSequence(start, end);
			if (TextUtils.isEmpty(sub)) {
				e.replace(start, i, "");
				changed = true;
			} else if (!v.isValid(sub)) {
				e.replace(start, i, mTokenizer.terminateToken(v.fixText(sub)));
				changed = true;
			}
			i = start;
		}
		/* ATL: the Editable is not bound to the GTK entry, hand the result to it */
		if (changed)
			setText(e);
	}

	protected void performFiltering(CharSequence text, int start, int end, int keyCode) {
		getFilter().filter(text.subSequence(start, end), this);
	}

	/* AOSP replaceText(); ATL has no QwertyKeyListener.markAsReplaced() (undo of the replacement by backspace)
	 * and passes the edited text to the GTK entry, with the cursor behind the inserted token */
	@Override
	protected void replaceText(CharSequence text) {
		clearComposingText();
		int end = getSelectionEnd();
		Editable editable = getText();
		int start = mTokenizer.findTokenStart(editable, end);
		CharSequence token = mTokenizer.terminateToken(text);
		editable.replace(start, end, token);
		setText(editable);
		setSelection(start + token.length());
	}

	@Override
	public CharSequence getAccessibilityClassName() {
		return MultiAutoCompleteTextView.class.getName();
	}

	public static interface Tokenizer {
		public int findTokenStart(CharSequence text, int cursor);

		public int findTokenEnd(CharSequence text, int cursor);

		public CharSequence terminateToken(CharSequence text);
	}

	public static class CommaTokenizer implements Tokenizer {
		public int findTokenStart(CharSequence text, int cursor) {
			int i = cursor;
			while (i > 0 && text.charAt(i - 1) != ',')
				i--;
			while (i < cursor && text.charAt(i) == ' ')
				i++;
			return i;
		}

		public int findTokenEnd(CharSequence text, int cursor) {
			int i = cursor;
			int len = text.length();
			while (i < len) {
				if (text.charAt(i) == ',')
					return i;
				else
					i++;
			}
			return len;
		}

		public CharSequence terminateToken(CharSequence text) {
			int i = text.length();
			while (i > 0 && text.charAt(i - 1) == ' ')
				i--;
			if (i > 0 && text.charAt(i - 1) == ',') {
				return text;
			} else {
				if (text instanceof Spanned) {
					SpannableString sp = new SpannableString(text + ", ");
					TextUtils.copySpansFrom((Spanned)text, 0, text.length(), Object.class, sp, 0);
					return sp;
				} else {
					return text + ", ";
				}
			}
		}
	}
}
