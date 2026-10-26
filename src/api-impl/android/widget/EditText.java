package android.widget;

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

	public Editable getText() {
		return new SpannableStringBuilder(native_getText(widget));
	}

	public Editable getEditableText() {
		return new SpannableStringBuilder(native_getText(widget));
	}

	@Override
	public void setText(CharSequence text) {
		text = applyFilters(text);
		native_setText(widget, text == null ? "" : text.toString());
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
	public void setTextSize(float size) {
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

	/* called from native code when the text changed */
	@Export
	private void sendTextChanged(String text) {
		if (listeners == null)
			return;
		SpannableStringBuilder s = new SpannableStringBuilder(text);
		for (TextWatcher watcher : new ArrayList<TextWatcher>(listeners)) {
			watcher.onTextChanged(s, 0, 0, s.length());
			watcher.afterTextChanged(s);
		}
	}

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

	@Override
	public void setCompoundDrawables(Drawable left, Drawable top, Drawable right, Drawable bottom) {}

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

	public void selectAll() {}
}
