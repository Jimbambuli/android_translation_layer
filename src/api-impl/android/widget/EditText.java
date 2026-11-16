package android.widget;

import android.atl.InputTrace;
import android.atl.annotation.Export;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.InputFilter;
import android.text.SpannableStringBuilder;
import android.text.SpannedString;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import java.util.ArrayList;

public class EditText extends TextView {
	public EditText(Context context) {
		super(context);
	}

	public EditText(Context context, AttributeSet attrs) {
		this(context, attrs, com.android.internal.R.attr.editTextStyle);
	}

	public EditText(Context context, AttributeSet attrs, int defStyleAttr) {
		this(context, attrs, defStyleAttr, 0);
	}

	public EditText(Context context, AttributeSet attrs, int defStyle, int defStyleRes) {
		super(context, attrs, defStyle, defStyleRes);
	}

	@Override
	protected native long native_constructor(Context context, AttributeSet attrs);
	protected native String native_getText(long widget);
	protected native void native_addTextChangedListener(long widget, TextWatcher watcher);
	protected native void native_setOnEditorActionListener(long widget, OnEditorActionListener l);
	protected native void native_setText(long widget, String text);
	protected native void native_setHint(long widget, CharSequence s);
	protected native CharSequence native_getHint(long widget); // gtk_entry_set_placeholder_text

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return native_gtkEventPropagation();
	}

	/* As on AOSP the EditText has one Editable, made by the editable factory when the text is set; typing changes
	 * it in place (sendTextChanged()), getText() returns it and the TextWatchers get it. An app that sets its own
	 * factory (Notally: the factory hands out the note's body) so sees the typed text in its own buffer. */
	private Editable mEditable;

	/** AOSP TextView.onCheckIsTextEditor() for an EditText: it always has an Editor, with TYPE_CLASS_TEXT unless the
	 * app sets another type; ATL cannot tell an explicit TYPE_NULL from "not set" (0), so only that case differs */
	@Override
	public boolean onCheckIsTextEditor() {
		return true;
	}

	public Editable getText() {
		if (mEditable == null)
			mEditable = getEditableFactory().newEditable(native_getText(widget));
		return mEditable;
	}

	public Editable getEditableText() {
		return getText();
	}

	@Override
	public void setText(CharSequence text) {
		text = applyFilters(text);
		if (text == null)
			text = "";
		mEditable = getEditableFactory().newEditable(text);
		programmatic_text = true;
		native_setText(widget, mEditable.toString());
		programmatic_text = false;
	}

	/* the typed change into the Editable: only the changed part is replaced, so spans elsewhere stay */
	private void syncEditable(String text) {
		Editable e = getText();
		String old = e.toString();
		if (old.equals(text))
			return;
		int start = 0, max = Math.min(old.length(), text.length());
		while (start < max && old.charAt(start) == text.charAt(start))
			start++;
		int oldEnd = old.length(), newEnd = text.length();
		while (oldEnd > start && newEnd > start && old.charAt(oldEnd - 1) == text.charAt(newEnd - 1)) {
			oldEnd--;
			newEnd--;
		}
		e.replace(start, oldEnd, text, start, newEnd);
	}

	/* as AOSP's Editor: typed or pasted text goes through the filters before it is inserted */
	@Override
	public void setFilters(InputFilter[] filters) {
		super.setFilters(filters);
		native_setFiltered(widget, filters.length > 0);
	}
	private native void native_setFiltered(long widget, boolean filtered);

	/* called from native code before source is inserted at position (in characters): the filtered
	 * text, or null to insert it unchanged */
	@Export
	private String filterInsertion(String source, int position) {
		String text = native_getText(widget);
		int offset = text.offsetByCodePoints(0, Math.min(position, text.codePointCount(0, text.length())));
		SpannedString dest = new SpannedString(text);
		CharSequence result = source;
		boolean filtered = false;
		for (InputFilter filter : getFilters()) {
			CharSequence out = filter.filter(result, 0, result.length(), dest, offset, offset);
			if (out != null) {
				result = out;
				filtered = true;
			}
		}
		return filtered ? result.toString() : null;
	}
	@Override
	protected void applyNativeTextSize(float size) {
		native_setTextSize(widget, size);
		applyHintColor();
	}
	private native void native_setTextSize(long widget, float size);

	/* like on AOSP, the view keeps its listeners, the GtkText calls back the view (see native code) */
	private ArrayList<TextWatcher> listeners;
	private OnEditorActionListener editor_action_listener;

	@Override
	public void removeTextChangedListener(TextWatcher watcher) {
		if (listeners != null)
			listeners.remove(watcher);
	}

	@Override
	public void addTextChangedListener(TextWatcher watcher) {
		if (listeners == null)
			listeners = new ArrayList<TextWatcher>();
		listeners.add(watcher);
		native_addTextChangedListener(widget, watcher);
	}

	@Override
	public void setOnEditorActionListener(OnEditorActionListener l) {
		editor_action_listener = l;
		native_setOnEditorActionListener(widget, l);
	}

	/* the text is being set by the app, not typed (InputTrace) */
	private boolean programmatic_text;

	/* called from native code when the text changed */
	@Export
	private void sendTextChanged(String text) {
		if (!programmatic_text)
			InputTrace.textChanged(this, text.length());
		String previous = mPreviousText;
		mPreviousText = text;
		syncEditable(text);
		Editable s = getText();
		/* AOSP TextView.sendBeforeTextChanged(), sendOnTextChanged(), onTextChanged(), sendAfterTextChanged(): the
		 * GtkText has changed already, so the "before" call gets the text from before with the new length */
		ArrayList<TextWatcher> watchers = listeners == null ? new ArrayList<TextWatcher>() : new ArrayList<TextWatcher>(listeners);
		SpannableStringBuilder old = new SpannableStringBuilder(previous);
		for (TextWatcher watcher : watchers)
			watcher.beforeTextChanged(old, 0, old.length(), s.length());
		for (TextWatcher watcher : watchers)
			watcher.onTextChanged(s, 0, old.length(), s.length());
		onTextChanged(s, 0, old.length(), s.length());
		for (TextWatcher watcher : watchers)
			watcher.afterTextChanged(s);
		if (mSelectionChangePending) {
			mSelectionChangePending = false;
			onSelectionChanged(getSelectionStart(), getSelectionEnd());
		}
	}

	private String mPreviousText = "";

	/* called from native code when Enter was pressed */
	@Export
	private void sendEditorAction(KeyEvent event) {
		/* like AOSP, where the editor action comes from TextView.onKeyDown(): a subclass's onKeyDown()
		 * sees Enter first (AutoCompleteTextView completes the item selected in its drop-down) */
		if (onKeyDown(KeyEvent.KEYCODE_ENTER, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER)))
			return;
		if (editor_action_listener != null)
			editor_action_listener.onEditorAction(this, 0, event);
	}

	/* the GtkText has no room for compound drawables: they are kept (getCompoundDrawablesRelative() is how
	 * TextInputLayout finds its icons) but not drawn */
	@Override
	protected void updateNativeCompoundDrawables() {}

	@Override
	public void setHint(CharSequence s) {
		native_setHint(widget, s == null ? "" : s.toString());
		applyHintColor();
	}

	@Override
	public void setHintTextColor(ColorStateList colors) {
		super.setHintTextColor(colors);
		applyHintColor();
	}

	private void applyHintColor() {
		ColorStateList colors = getHintTextColors();
		if (colors != null)
			native_setHintColor(widget, colors.getColorForState(getDrawableState(), colors.getDefaultColor()));
	}
	private native void native_setHintColor(long widget, int color);

	@Override
	public CharSequence getHint() {
		return native_getHint(widget);
	}

	/* ---- selection: the GtkText's. GTK counts characters, Android UTF-16 units. ---- */

	private native int[] native_getSelectionBounds(long widget);
	private native void native_setSelectionBounds(long widget, int start, int end);

	private int toUtf16(String s, int codePoints) {
		return s.offsetByCodePoints(0, Math.max(0, Math.min(codePoints, s.codePointCount(0, s.length()))));
	}

	@Override
	public int getSelectionStart() {
		String s = native_getText(widget);
		return toUtf16(s, native_getSelectionBounds(widget)[0]);
	}

	@Override
	public int getSelectionEnd() {
		String s = native_getText(widget);
		return toUtf16(s, native_getSelectionBounds(widget)[1]);
	}

	/* AOSP EditText.setSelection(): Selection.setSelection() on the text, which throws if the range is not in it */
	@Override
	public void setSelection(int start, int stop) {
		String s = native_getText(widget);
		int length = s.length();
		if (start < 0 || stop < 0 || start > length || stop > length) {
			throw new IndexOutOfBoundsException("setSpan (" + start + " ... " + stop + ") ends beyond length " + length);
		}
		native_setSelectionBounds(widget, s.codePointCount(0, start), s.codePointCount(0, stop));
	}

	@Override
	public void setSelection(int index) {
		setSelection(index, index);
	}

	public void extendSelection(int index) {
		setSelection(getSelectionStart(), index);
	}

	public void selectAll() {
		setSelection(0, native_getText(widget).length());
	}

	/* the cursor moved with a text change that sendTextChanged() has not passed on yet */
	private boolean mSelectionChangePending;

	/* called from native code when the cursor or the selection moved. GtkText moves the cursor before it reports
	 * the change of its text; AOSP's SpannableStringBuilder.replace() calls the TextWatchers first and the span
	 * watchers (TextView.spanChange() -> onSelectionChanged()) after them, with the Editable already changed. So
	 * while the typed text is not in the Editable yet, onSelectionChanged() waits for sendTextChanged(). */
	@Export
	private void sendSelectionChanged() {
		if (!native_getText(widget).equals(mPreviousText)) {
			mSelectionChangePending = true;
			return;
		}
		onSelectionChanged(getSelectionStart(), getSelectionEnd());
	}

	@Override
	boolean textChangesComeFromNative() {
		return true;
	}

	/* the paste, cut and typed replacement of the range [min, max) by new text: the cursor is after it */
	@Override
	void replaceText_internal(int min, int max, CharSequence newText) {
		StringBuilder sb = new StringBuilder(native_getText(widget));
		sb.replace(min, max, newText.toString());
		setText(sb);
		setSelection(min + newText.length());
	}

	@Override
	public CharSequence getAccessibilityClassName() {
		return EditText.class.getName();
	}

	@Override
	public void onInitializeAccessibilityNodeInfoInternal(android.view.accessibility.AccessibilityNodeInfo info) {
		super.onInitializeAccessibilityNodeInfoInternal(info);
		info.setClassName(EditText.class.getName());
		info.setEditable(isEnabled());
	}
}
