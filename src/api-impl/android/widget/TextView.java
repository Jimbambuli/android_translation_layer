package android.widget;

import android.annotation.UnsupportedAppUsage;
import android.atl.annotation.Export;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.BoringLayout;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.TextDirectionHeuristic;
import android.text.TextDirectionHeuristics;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.BaseMovementMethod;
import android.text.method.KeyListener;
import android.text.method.MovementMethod;
import android.text.method.TransformationMethod;
import android.text.style.URLSpan;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import java.util.Locale;

public class TextView extends View {
	private ColorStateList colors = new ColorStateList(new int[][] {new int[0]}, new int[1]);
	private CharSequence text = "";
	private TextPaint paint = new TextPaint();
	private Typeface typeface;
	private boolean include_padding = false;
	private int break_strategy = 0 /*BREAK_STRATEGY_SIMPLE*/;
	private int hyphenation_frequency = 0 /*HYPHENATION_FREQUENCY_NONE*/;
	private int gravity = Gravity.TOP | Gravity.START;
	private int max_lines = Integer.MAX_VALUE;
	private TextUtils.TruncateAt ellipsize = null;

	public TextView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public TextView(Context context) {
		this(context, null);
	}

	public TextView(Context context, AttributeSet attrs, int defStyleAttr) {
		this(context, attrs, defStyleAttr, 0);
	}

	public TextView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);

		TypedArray a = context.obtainStyledAttributes(attrs, com.android.internal.R.styleable.TextView, defStyleAttr, 0);
		try {
			if (a.hasValue(com.android.internal.R.styleable.TextView_text)) {
				setText(a.getText(com.android.internal.R.styleable.TextView_text));
			}
			if (a.hasValue(com.android.internal.R.styleable.TextView_hint)) {
				setHint(a.getText(com.android.internal.R.styleable.TextView_hint));
			}

			int ap = a.getResourceId(com.android.internal.R.styleable.TextView_textAppearance, -1);
			if (ap != -1)
				setTextAppearance(context, ap);
			if (a.hasValue(com.android.internal.R.styleable.TextView_textColor)) {
				setTextColor(a.getColorStateList(com.android.internal.R.styleable.TextView_textColor));
			}
			if (a.hasValue(com.android.internal.R.styleable.TextView_textSize)) {
				setTextSize(a.getDimensionPixelSize(com.android.internal.R.styleable.TextView_textSize, 10));
			}

			if (a.hasValue(com.android.internal.R.styleable.TextView_textStyle)) {
				int textStyle = a.getInt(com.android.internal.R.styleable.TextView_textStyle, 0);
				setTypeface(getTypeface(), textStyle);
			}

			if (a.hasValue(com.android.internal.R.styleable.TextView_gravity))
				setGravity(a.getInt(com.android.internal.R.styleable.TextView_gravity, Gravity.TOP | Gravity.START));

			int ellipsize_attr = a.getInt(com.android.internal.R.styleable.TextView_ellipsize, -1);
			if (ellipsize_attr >= 1 && ellipsize_attr <= 4)
				setEllipsize(TextUtils.TruncateAt.values()[ellipsize_attr - 1]);
			if (a.getBoolean(com.android.internal.R.styleable.TextView_singleLine, false))
				setSingleLine(true);
			if (a.hasValue(com.android.internal.R.styleable.TextView_lines))
				setLines(a.getInt(com.android.internal.R.styleable.TextView_lines, -1));
			if (a.hasValue(com.android.internal.R.styleable.TextView_maxLines))
				setMaxLines(a.getInt(com.android.internal.R.styleable.TextView_maxLines, -1));

			Drawable left = a.getDrawable(com.android.internal.R.styleable.TextView_drawableLeft);
			Drawable top = a.getDrawable(com.android.internal.R.styleable.TextView_drawableTop);
			Drawable right = a.getDrawable(com.android.internal.R.styleable.TextView_drawableRight);
			Drawable bottom = a.getDrawable(com.android.internal.R.styleable.TextView_drawableBottom);
			Drawable start = a.getDrawable(com.android.internal.R.styleable.TextView_drawableStart);
			Drawable end = a.getDrawable(com.android.internal.R.styleable.TextView_drawableEnd);
			// layout direction is always LTR, so start/end override left/right like in AOSP
			if (start != null)
				left = start;
			if (end != null)
				right = end;
			if (left != null || top != null || right != null || bottom != null)
				setCompoundDrawablesWithIntrinsicBounds(left, top, right, bottom);
			if (a.hasValue(com.android.internal.R.styleable.TextView_drawablePadding))
				setCompoundDrawablePadding(a.getDimensionPixelSize(com.android.internal.R.styleable.TextView_drawablePadding, 0));

			if (a.hasValue(com.android.internal.R.styleable.TextView_textAllCaps)) {
				boolean allCaps = a.getBoolean(com.android.internal.R.styleable.TextView_textAllCaps, false);
				setAllCaps(allCaps);
			}
		} catch (java.lang.Exception e) {
			System.out.println("exception while inflating TextView:");
			e.printStackTrace();
		}

		a.recycle();
		haveCustomMeasure = false;
	}

	@Override
	protected native long native_constructor(Context context, AttributeSet attrs);

	public void setText(int resId) {
		setText(getContext().getResources().getText(resId));
	}

	public void setText(CharSequence text) {
		setText(text, BufferType.NORMAL);
	}

	public void setText(CharSequence text, BufferType type) {
		this.text = text == null ? "" : text;
		native_setText(text != null ? text.toString() : null);

		if (text instanceof android.text.Spanned)
			native_set_markup(1);
		if (!isLayoutRequested())
			requestLayout();
	}

	public void setText(char[] text, int start, int len) {
		setText(new String(text, start, len));
	}

	public final void append(CharSequence text) {
		append(text, 0, text.length());
	}

	public void append(CharSequence text, int start, int end) {
		setText(SpannableStringBuilder.valueOf(this.text).append(text, start, end));
	}

	private native final void native_set_markup(int bool);

	public native final void native_setText(String text);

	public void setTextSize(int unit, float size) {
		if (unit != TypedValue.COMPLEX_UNIT_SP)
			System.out.println("setTextSize called with non-SP unit (" + unit + "), we don't currently handle that");
		setTextSize(size);
	}
	public native void setTextSize(float size);

	public native final void native_setTextColor(int color);
	public void setTextColor(int color) {
		colors = ColorStateList.valueOf(color);
		native_setTextColor(color);
	}
	public void setTextColor(ColorStateList colors) {
		if (colors != null) {
			this.colors = colors;
			native_setTextColor(colors.getColorForState(getDrawableState(), colors.getDefaultColor()));
		}
	}

	@Override
	protected void drawableStateChanged() {
		super.drawableStateChanged();
		if (colors.isStateful())
			native_setTextColor(colors.getColorForState(getDrawableState(), colors.getDefaultColor()));
		for (Drawable d : new Drawable[] {drawableLeft, drawableTop, drawableRight, drawableBottom}) {
			if (d != null && d.isStateful())
				d.setState(getDrawableState());
		}
	}
	/* as in AOSP: the style is applied by deriving a typeface from tf */
	public void setTypeface(Typeface tf, int style) {
		if (style > 0)
			tf = (tf == null) ? Typeface.defaultFromStyle(style) : Typeface.create(tf, style);
		setTypeface(tf);
	}

	public void setTypeface(Typeface tf) {
		typeface = tf;
		paint.setTypeface(tf);
		native_setTypeface(widget, tf);
	}
	public void setLineSpacing(float add, float mult) {}

	/* API 28, as in AOSP */
	public void setLineHeight(int lineHeight) {
		int fontHeight = getPaint().getFontMetricsInt(null);
		if (lineHeight != fontHeight)
			setLineSpacing(lineHeight - fontHeight, 1f);
	}

	/* API 34 */
	public void setLineHeight(int unit, float lineHeight) {
		setLineHeight((int)android.util.TypedValue.applyDimension(unit, lineHeight, getResources().getDisplayMetrics()));
	}

	/* API 28, as in AOSP: moves the first/last baseline by adjusting the padding */
	public void setFirstBaselineToTopHeight(int firstBaselineToTopHeight) {
		android.graphics.Paint.FontMetricsInt fm = getPaint().getFontMetricsInt();
		int fontMetricsTop = getIncludeFontPadding() ? fm.top : fm.ascent;
		if (firstBaselineToTopHeight > Math.abs(fontMetricsTop))
			setPadding(getPaddingLeft(), firstBaselineToTopHeight - (-fontMetricsTop), getPaddingRight(), getPaddingBottom());
	}

	public void setLastBaselineToBottomHeight(int lastBaselineToBottomHeight) {
		android.graphics.Paint.FontMetricsInt fm = getPaint().getFontMetricsInt();
		int fontMetricsBottom = getIncludeFontPadding() ? fm.bottom : fm.descent;
		if (lastBaselineToBottomHeight > Math.abs(fontMetricsBottom))
			setPadding(getPaddingLeft(), getPaddingTop(), getPaddingRight(), lastBaselineToBottomHeight - fontMetricsBottom);
	}

	private boolean horizontallyScrollable;

	/* API 29 */
	public final boolean isHorizontallyScrollable() {
		return horizontallyScrollable;
	}
	public final void setLinksClickable(boolean whether) {}

	public void setInputType(int type) {}
	public void setFilters(InputFilter[] filters) {}
	public void setCursorVisible(boolean visible) {}
	public void setImeOptions(int imeOptions) {}

	public final ColorStateList getTextColors() { return colors; }
	public static ColorStateList getTextColors(Context context, TypedArray attrs) { return new ColorStateList(new int[][] {new int[0]}, new int[1]); }

	public TextPaint getPaint() {
		return paint;
	}

	public void addTextChangedListener(TextWatcher watcher) {}
	public void removeTextChangedListener(TextWatcher watcher) {}
	public void setOnEditorActionListener(TextView.OnEditorActionListener l) {}

	public TransformationMethod getTransformationMethod() {
		return null;
	}

	// the hint is not shown, but the colors are kept for getHintTextColors()
	private ColorStateList hint_colors = null;
	public void setHintTextColor(ColorStateList colorStateList) {
		hint_colors = colorStateList;
	}
	public void setHintTextColor(int i) {
		hint_colors = ColorStateList.valueOf(i);
	}
	public final ColorStateList getHintTextColors() {
		return hint_colors;
	}
	public void setLinkTextColor(ColorStateList colorStateList) {}

	public void setSingleLine() {
		setSingleLine(true);
	}
	public void setSelection(int i) {}
	public void setSelection(int i, int j) {}

	public int getSelectionStart() {
		return 0;
	}

	public int getSelectionEnd() {
		return 0;
	}

	public void setEllipsize(TextUtils.TruncateAt truncateAt) {
		ellipsize = truncateAt;
		updateLines();
	}

	/**
	 * Sets the text color, size, style, hint color and all caps from a TextAppearance style.
	 */
	public void setTextAppearance(Context context, int resId) {
		TypedArray a = context.obtainStyledAttributes(resId, com.android.internal.R.styleable.TextAppearance);
		if (a.hasValue(com.android.internal.R.styleable.TextAppearance_textColor)) {
			ColorStateList textColor = a.getColorStateList(com.android.internal.R.styleable.TextAppearance_textColor);
			if (textColor != null)
				setTextColor(textColor);
		}
		if (a.hasValue(com.android.internal.R.styleable.TextAppearance_textSize))
			setTextSize(a.getDimensionPixelSize(com.android.internal.R.styleable.TextAppearance_textSize, 10));
		if (a.hasValue(com.android.internal.R.styleable.TextAppearance_textColorHint))
			setHintTextColor(a.getColorStateList(com.android.internal.R.styleable.TextAppearance_textColorHint));
		if (a.hasValue(com.android.internal.R.styleable.TextAppearance_textStyle))
			setTypeface(getTypeface(), a.getInt(com.android.internal.R.styleable.TextAppearance_textStyle, 0));
		if (a.hasValue(com.android.internal.R.styleable.TextAppearance_textAllCaps))
			setAllCaps(a.getBoolean(com.android.internal.R.styleable.TextAppearance_textAllCaps, false));
		a.recycle();
	}

	public void setMaxLines(int maxLines) {
		max_lines = maxLines > 0 ? maxLines : Integer.MAX_VALUE;
		updateLines();
	}

	/* GtkLabel can only limit the number of lines when ellipsizing, so text cut off
	 * after maxLines always gets an ellipsis, also when no ellipsize mode is set. */
	private void updateLines() {
		int mode = 3; // PANGO_ELLIPSIZE_END
		if (ellipsize == TextUtils.TruncateAt.START)
			mode = 1; // PANGO_ELLIPSIZE_START
		else if (ellipsize == TextUtils.TruncateAt.MIDDLE)
			mode = 2; // PANGO_ELLIPSIZE_MIDDLE
		native_setLines(max_lines == Integer.MAX_VALUE ? -1 : max_lines, max_lines == Integer.MAX_VALUE ? 0 : mode);
	}
	private native void native_setLines(int lines, int ellipsize_mode);

	public void setMinWidth(int minWidth) {}

	public void setMinEms(int minEms) {}

	public void setMaxEms(int maxEms) {}

	public Locale getTextLocale() {
		return Locale.getDefault();
	}

	// there is no input method connection to batch or configure
	public void beginBatchEdit() {}

	public void endBatchEdit() {}

	public void setPrivateImeOptions(String type) {}
	public void setMaxWidth(int maxWidth) {}

	public Typeface getTypeface() { return typeface; }

	public float getTextSize() { return 10; }

	/* GtkLabel mirrors the alignment in right-to-left layouts, so LEFT and RIGHT are
	 * treated like START and END */
	@Override
	public void setGravity(int gravity) {
		this.gravity = gravity;
		float xalign = 0.f;
		int horizontal = gravity & Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK;
		if (horizontal == Gravity.CENTER_HORIZONTAL)
			xalign = 0.5f;
		else if (horizontal == Gravity.RIGHT || horizontal == Gravity.END)
			xalign = 1.f;
		float yalign = 0.f;
		int vertical = gravity & Gravity.VERTICAL_GRAVITY_MASK;
		if (vertical == Gravity.CENTER_VERTICAL)
			yalign = 0.5f;
		else if (vertical == Gravity.BOTTOM)
			yalign = 1.f;
		native_setAlignment(xalign, yalign);
	}
	private native void native_setAlignment(float xalign, float yalign);

	public int getGravity() {
		return gravity;
	}

	public int getCompoundPaddingTop() { return 0; }
	public int getCompoundPaddingBottom() { return 0; }

	public CharSequence getText() {
		return text;
	};

	private int drawable_padding = 0;

	public void setCompoundDrawablePadding(int pad) {
		drawable_padding = pad;
		native_setCompoundDrawablePadding(widget, pad);
	}

	private native void native_setCompoundDrawablePadding(long widget, int pad);
	private native void native_setTypeface(long widget, Typeface tf);

	protected native void native_setCompoundDrawables(long widget, long left, long top, long right, long bottom);

	// just to prevent garbage collection while native side uses it
	private Drawable drawableLeft = null;
	private Drawable drawableTop = null;
	private Drawable drawableRight = null;
	private Drawable drawableBottom = null;

	public void setCompoundDrawables(Drawable left, Drawable top, Drawable right, Drawable bottom) {
		for (Drawable d : new Drawable[] {left, top, right, bottom}) {
			if (d != null) {
				d.setCallback(this);
				if (d.isStateful())
					d.setState(getDrawableState());
			}
		}
		native_setCompoundDrawables(widget,
		                            left != null ? left.paintable : 0,
		                            top != null ? top.paintable : 0,
		                            right != null ? right.paintable : 0,
		                            bottom != null ? bottom.paintable : 0);
		drawableLeft = left;
		drawableTop = top;
		drawableRight = right;
		drawableBottom = bottom;
	}

	public void setAllCaps(boolean allCaps) {
		String[] classesToRemove = {"ATL-text-uppercase"};
		native_removeClasses(widget, classesToRemove);

		if (allCaps) {
			native_addClass(widget, "ATL-text-uppercase");
		}
	}

	public void setSaveEnabled(boolean enabled) {}

	public final void setAutoLinkMask(int mask) {}

	public void setEditableFactory(Editable.Factory factory) {}

	public KeyListener getKeyListener() { return null; }

	public int getInputType() { return 0; }

	public final void setTransformationMethod(TransformationMethod method) {}

	public InputFilter[] getFilters() { return new InputFilter[0]; }

	public int getMaxLines() { return max_lines == Integer.MAX_VALUE ? -1 : max_lines; }

	public void setCompoundDrawablesRelative(Drawable start, Drawable top, Drawable end, Drawable bottom) {
		setCompoundDrawables(start, top, end, bottom);
	}

	public int getLineCount() { return 1; }

	public URLSpan[] getUrls() { return new URLSpan[0]; }

	public void setMovementMethod(MovementMethod method) {}

	public void setTextIsSelectable(boolean selectable) {}

	public MovementMethod getMovementMethod() {
		return new BaseMovementMethod();
	}

	public CharSequence getHint() { return "HINT"; }

	public int getMinHeight() { return 0; }
	public int getMinWidth() { return 0; }
	public void setMinHeight(int minHeight) {}
	public void setMaxHeight(int maxHeight) {}

	public void setHorizontallyScrolling(boolean whether) {
		horizontallyScrollable = whether;
	}
	public boolean getHorizontallyScrolling() {
		return false;
	}

	public static interface OnEditorActionListener {
		@Export
		public abstract boolean onEditorAction(TextView v, int actionId, KeyEvent event);
	}

	public static enum BufferType {
		EDITABLE,
		NORMAL,
		SPANNABLE,
	}

	public Layout getLayout() {
		return new BoringLayout(getText(), getPaint(), getWidth(), Layout.Alignment.ALIGN_NORMAL, 1, 0, new BoringLayout.Metrics(), false);
	}

	public int getCurrentTextColor() {
		return Color.CYAN;
	}

	public void setSingleLine(boolean singleLine) {
		setMaxLines(singleLine ? 1 : Integer.MAX_VALUE);
	}

	public int getCompoundPaddingLeft() { return 0; }

	public int getCompoundPaddingRight() { return 0; }

	public void setHint(int resId) {
		setHint(getContext().getResources().getText(resId));
	}

	public float getLetterSpacing() { return 0.f; }

	public void setCompoundDrawablesRelativeWithIntrinsicBounds(int start, int top, int end, int bottom) {
		setCompoundDrawablesWithIntrinsicBounds(start, top, end, bottom);
	}

	public void setCompoundDrawablesRelativeWithIntrinsicBounds(Drawable start, Drawable top, Drawable end, Drawable bottom) {
		setCompoundDrawablesWithIntrinsicBounds(start, top, end, bottom);
	}

	public boolean getLinksClickable() { return true; }

	public boolean isTextSelectable() { return true; }

	public void setCompoundDrawablesWithIntrinsicBounds(int left, int top, int right, int bottom) {
		final Context context = getContext();
		setCompoundDrawablesWithIntrinsicBounds(left != 0 ? context.getDrawable(left) : null,
		                                        top != 0 ? context.getDrawable(top) : null,
		                                        right != 0 ? context.getDrawable(right) : null,
		                                        bottom != 0 ? context.getDrawable(bottom) : null);
	}

	public void setCompoundDrawablesWithIntrinsicBounds(Drawable left, Drawable top, Drawable right, Drawable bottom) {
		for (Drawable d : new Drawable[] {left, top, right, bottom}) {
			if (d != null)
				d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
		}
		setCompoundDrawables(left, top, right, bottom);
	}

	public void setHint(CharSequence s) {}

	public Drawable[] getCompoundDrawablesRelative() { return getCompoundDrawables(); }

	public Drawable[] getCompoundDrawables() {
		return new Drawable[] {drawableLeft, drawableTop, drawableRight, drawableBottom};
	}

	public void setTextAppearance(int resId) {
		setTextAppearance(getContext(), resId);
	}

	public int length() {
		return getText().length();
	}

	public void setHighlightColor(int color) {}

	public Editable getEditableText() {
		return new SpannableStringBuilder(getText());
	}

	public int getMaxWidth() { return 1000; }

	public void nullLayouts() {}

	public void setLinkTextColor(int color) {}

	public void setCustomSelectionActionModeCallback(ActionMode.Callback actionModeCallback) {}

	public int getExtendedPaddingTop() { return 0; }

	public void setRawInputType(int type) {}

	public TextUtils.TruncateAt getEllipsize() { return ellipsize; }

	/* only the maximum is applied, the view is not made taller for shorter text */
	public void setLines(int lines) {
		setMaxLines(lines);
	}

	public int getMinLines() {
		return -1;
	}

	public void setMinLines(int lines) {}

	public void setSelectAllOnFocus(boolean selectAllOnFocus) {}

	public int getCompoundDrawablePadding() { return drawable_padding; }

	public int getPaintFlags() { return 0; }

	public void setPaintFlags(int flags) {}

	public int getLineHeight() {
		return 10; // FIXME
	}

	public int getMaxHeight() {
		return -1;
	}

	public boolean isAllCaps() { return false; }

	public int getAutoSizeStepGranularity() {
		return -1;
	}

	public void setAutoSizeTextTypeUniformWithPresetSizes(int[] presetSizes, int unit) {}

	public void setCompoundDrawableTintList(ColorStateList tint) {}

	public void setCompoundDrawableTintMode(PorterDuff.Mode tintMode) {}

	public int getHyphenationFrequency() {
		return hyphenation_frequency;
	}

	public void setHyphenationFrequency(int hyphenationFrequency) {
		hyphenation_frequency = hyphenationFrequency;
	}

	public boolean getIncludeFontPadding() { return include_padding; }

	public void setIncludeFontPadding(boolean includePadding) {
		include_padding = includePadding;
	}

	public float getLineSpacingExtra() { return 0.f; }

	public float getLineSpacingMultiplier() { return 1.f; }

	public TextDirectionHeuristic getTextDirectionHeuristic() {
		return TextDirectionHeuristics.LTR;
	}

	public Bundle getInputExtras(boolean key) {
		return new Bundle();
	}

	public void setError(CharSequence error) {
		System.out.println("ERROR: " + error);
	}

	public int getTotalPaddingLeft() { return 0; }

	public int getTotalPaddingTop() { return 0; }

	public int getTotalPaddingRight() { return 0; }

	public int getTotalPaddingBottom() { return 0; }

	public int getImeOptions() { return 0; }

	public void setShadowLayer(float radius, float dx, float dy, int color) {}

	public int getBreakStrategy() {
		return break_strategy;
	}

	public void setBreakStrategy(int strategy) {
		break_strategy = strategy;
	}

	public void clearComposingText() {}

	public void setKeyListener(KeyListener keyListener) {}

	public int getAutoLinkMask() { return 0; }

	public void setWidth(int width) {}
	public void setHeight(int height) {}

	public void setFreezesText(boolean freezesText) {}

	public void setLetterSpacing(float letterSpacing) {}

	public void setMarqueeRepeatLimit(int marqueeLimit) {}

	public void setShowSoftInputOnFocus(boolean showSoftInputOnFocus) {}

	public void setCustomInsertionActionModeCallback(ActionMode.Callback actionModeCallback) {}

	public int getJustificationMode() {
		return 0; // JUSTIFICATION_MODE_NONE
	}

	public void setImeActionLabel(CharSequence label, int actionId) {}

	public int getExtendedPaddingBottom() { return 0; }

	public boolean hasSelection() { return false; }

	@UnsupportedAppUsage /* androidx ACTVAutoSizeHelper seems to love this */
	/* Copyright (C) 2006 The Android Open Source Project */
	private Layout.Alignment getLayoutAlignment() {
		Layout.Alignment alignment;
		switch (getTextAlignment()) {
			case TEXT_ALIGNMENT_GRAVITY:
				switch (gravity & Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK) {
					case Gravity.START:
						alignment = Layout.Alignment.ALIGN_NORMAL;
						break;
					case Gravity.END:
						alignment = Layout.Alignment.ALIGN_OPPOSITE;
						break;
					case Gravity.LEFT:
						alignment = Layout.Alignment.ALIGN_LEFT;
						break;
					case Gravity.RIGHT:
						alignment = Layout.Alignment.ALIGN_RIGHT;
						break;
					case Gravity.CENTER_HORIZONTAL:
						alignment = Layout.Alignment.ALIGN_CENTER;
						break;
					default:
						alignment = Layout.Alignment.ALIGN_NORMAL;
						break;
				}
				break;
			case TEXT_ALIGNMENT_TEXT_START:
				alignment = Layout.Alignment.ALIGN_NORMAL;
				break;
			case TEXT_ALIGNMENT_TEXT_END:
				alignment = Layout.Alignment.ALIGN_OPPOSITE;
				break;
			case TEXT_ALIGNMENT_CENTER:
				alignment = Layout.Alignment.ALIGN_CENTER;
				break;
			case TEXT_ALIGNMENT_VIEW_START:
				alignment = (getLayoutDirection() == LAYOUT_DIRECTION_RTL) ? Layout.Alignment.ALIGN_RIGHT : Layout.Alignment.ALIGN_LEFT;
				break;
			case TEXT_ALIGNMENT_VIEW_END:
				alignment = (getLayoutDirection() == LAYOUT_DIRECTION_RTL) ? Layout.Alignment.ALIGN_LEFT : Layout.Alignment.ALIGN_RIGHT;
				break;
			case TEXT_ALIGNMENT_INHERIT:
				// This should never happen as we have already resolved the text alignment
				// but better safe than sorry so we just fall through
			default:
				alignment = Layout.Alignment.ALIGN_NORMAL;
				break;
		}
		return alignment;
	}
}
