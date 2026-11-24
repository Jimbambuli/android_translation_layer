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
import android.text.method.PasswordTransformationMethod;
import android.text.method.TransformationMethod;
import android.util.AttributeSet;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import java.util.ArrayList;

public class EditText extends TextView {
	public EditText(Context context) {
		super(context);
		nativeSetOnLongClickListener(widget);
	}

	public EditText(Context context, AttributeSet attrs) {
		this(context, attrs, com.android.internal.R.attr.editTextStyle);
	}

	public EditText(Context context, AttributeSet attrs, int defStyleAttr) {
		this(context, attrs, defStyleAttr, 0);
	}

	public EditText(Context context, AttributeSet attrs, int defStyle, int defStyleRes) {
		super(context, attrs, defStyle, defStyleRes);
		/* the long press of the GtkText reaches performLongClick(), which selects a word like AOSP's Editor */
		nativeSetOnLongClickListener(widget);
	}

	@Override
	protected native long native_constructor(Context context, AttributeSet attrs);
	protected native String native_getText(long widget);
	protected native void native_addTextChangedListener(long widget, TextWatcher watcher);
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

	@Override
	protected boolean getDefaultEditable() {
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

	/* AOSP EditText.setText(CharSequence, BufferType): the text always goes into the editable buffer (here the
	 * GtkText); TextView.setText(CharSequence) and its other variants end up here */
	@Override
	public void setText(CharSequence text, BufferType type) {
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

	/* the text is being set by the app, not typed (InputTrace) */
	private boolean programmatic_text;

	/* called from native code when the text changed */
	@Export
	private void sendTextChanged(String text) {
		if (!programmatic_text)
			InputTrace.textChanged(this, text.length());
		String previous = mPreviousText != null ? mPreviousText : "";
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
		/* AOSP Editor.sendOnTextChanged(): a change of the text ends the text action mode */
		if (mParagraphSelection != null)
			mParagraphSelection.onTextChanged();
	}

	/* AOSP TextView.performLongClick() with Editor.performLongClick(): unless a long click listener takes the
	 * press, the word at the touch is selected and the floating toolbar of the selection starts */
	@Override
	public boolean performLongClick() {
		if (super.performLongClick())
			return true;
		if (!editTextSelection().performLongClickAtCursor())
			return false;
		performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
		return true;
	}

	/* no initializer: TextView's constructor sets the first text before EditText's field initializers run,
	 * so null is the empty text from before it */
	private String mPreviousText;

	/* called from native code when the GtkText took Enter: like a hardware Enter on AOSP, the key goes down and up
	 * through dispatchKeyEvent(), to the OnKeyListener and to onKeyDown()/onKeyUp() (a subclass's first:
	 * AutoCompleteTextView completes the item selected in its drop-down) */
	@Export
	private void sendEnterKey(int metaState) {
		long now = SystemClock.uptimeMillis();
		dispatchKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER, 0, metaState,
		                              KeyCharacterMap.VIRTUAL_KEYBOARD, 0, KeyEvent.FLAG_FROM_SYSTEM, InputDevice.SOURCE_KEYBOARD));
		dispatchKeyEvent(new KeyEvent(now, SystemClock.uptimeMillis(), KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER, 0, metaState,
		                              KeyCharacterMap.VIRTUAL_KEYBOARD, 0, KeyEvent.FLAG_FROM_SYSTEM, InputDevice.SOURCE_KEYBOARD));
	}

	/* AOSP InputContentType.enterDown: the editor action listener took Enter down */
	private boolean mEnterDown;

	/* AOSP TextView.getActionIdForEnterEvent() */
	private int getActionIdForEnterEvent() {
		if (!isSingleLine())
			return EditorInfo.IME_NULL;
		return getImeOptions() & EditorInfo.IME_MASK_ACTION;
	}

	/* AOSP TextView.shouldAdvanceFocusOnEnter(); an EditText always has its key listener here */
	private boolean shouldAdvanceFocusOnEnter() {
		if (isSingleLine())
			return true;
		int type = getInputType();
		if ((type & EditorInfo.TYPE_MASK_CLASS) == EditorInfo.TYPE_CLASS_TEXT) {
			int variation = type & EditorInfo.TYPE_MASK_VARIATION;
			if (variation == EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS || variation == EditorInfo.TYPE_TEXT_VARIATION_EMAIL_SUBJECT)
				return true;
		}
		return false;
	}

	private static boolean isEnter(int keyCode) {
		return keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER;
	}

	/* AOSP TextView.onKeyDown() -> doKeyDown() for Enter: the editor action listener may take it; a single line
	 * editor does not take it as text. Otherwise AOSP's key listener types a new line, which the single line of the
	 * GtkText cannot show: Enter is taken all the same. */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (!isEnter(keyCode) || !isEnabled())
			return super.onKeyDown(keyCode, event);
		if (event.hasNoModifiers()) {
			if (mOnEditorActionListener != null && mOnEditorActionListener.onEditorAction(this, getActionIdForEnterEvent(), event)) {
				mEnterDown = true;
				return true;
			}
			if ((event.getFlags() & KeyEvent.FLAG_EDITOR_ACTION) != 0 || shouldAdvanceFocusOnEnter()) {
				if (hasOnClickListeners())
					return super.onKeyDown(keyCode, event);
				return true;
			}
		}
		return true;
	}

	/* AOSP TextView.onKeyUp() for Enter: the editor action listener that took Enter down gets it up as well; a
	 * single line editor moves the focus down */
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (!isEnter(keyCode) || !isEnabled() || !event.hasNoModifiers())
			return super.onKeyUp(keyCode, event);
		if (mOnEditorActionListener != null && mEnterDown) {
			mEnterDown = false;
			if (mOnEditorActionListener.onEditorAction(this, getActionIdForEnterEvent(), event))
				return true;
		}
		if ((event.getFlags() & KeyEvent.FLAG_EDITOR_ACTION) != 0 || shouldAdvanceFocusOnEnter()) {
			if (!hasOnClickListeners()) {
				View v = focusSearch(FOCUS_DOWN);
				if (v != null) {
					if (!v.requestFocus(FOCUS_DOWN))
						throw new IllegalStateException("focus search returned a view that wasn't able to take focus!");
					super.onKeyUp(keyCode, event);
					return true;
				}
			}
		}
		return super.onKeyUp(keyCode, event);
	}

	/* the GtkText shows the text itself, without the transformation: the password transformation is its invisible
	 * mode (and the password purpose for the input method) */
	@Override
	void setTransformationMethodInternal(TransformationMethod method) {
		super.setTransformationMethodInternal(method);
		native_setPassword(widget, method instanceof PasswordTransformationMethod);
	}
	private native void native_setPassword(long widget, boolean password);

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
		if (!native_getText(widget).equals(mPreviousText != null ? mPreviousText : "")) {
			mSelectionChangePending = true;
			return;
		}
		onSelectionChanged(getSelectionStart(), getSelectionEnd());
		if (mParagraphSelection != null)
			mParagraphSelection.onEditTextSelectionChanged();
	}

	@Override
	boolean textChangesComeFromNative() {
		return true;
	}

	/* the paste, cut and typed replacement of the range [min, max) by new text: the cursor is after it. Like
	 * AOSP's Editable.replace() it is an edit of the text (the GtkText's, with its filters and the change
	 * reported to the TextWatchers as typing is), not a new text */
	@Override
	void replaceText_internal(int min, int max, CharSequence newText) {
		String s = native_getText(widget);
		native_replaceText(widget, s.codePointCount(0, min), s.codePointCount(0, max), newText.toString());
	}
	private native void native_replaceText(long widget, int start, int end, String text);

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
