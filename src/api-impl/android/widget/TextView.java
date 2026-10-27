package android.widget;

import android.annotation.UnsupportedAppUsage;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.BlendMode;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.fonts.FontStyle;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.LocaleList;
import android.text.BoringLayout;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.Spannable;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.TextDirectionHeuristic;
import android.text.TextDirectionHeuristics;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.KeyListener;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.text.method.SingleLineTransformationMethod;
import android.text.method.TransformationMethod;
import android.text.style.CharacterStyle;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.URLSpan;
import android.text.style.UnderlineSpan;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import java.util.Arrays;
import java.util.Locale;
import java.util.TreeSet;

public class TextView extends View {
	/* AOSP: opaque black until a color is set; the color for the current drawable state is mCurTextColor */
	private ColorStateList colors = ColorStateList.valueOf(0xFF000000);
	private int mCurTextColor = 0xFF000000;
	private CharSequence text = "";
	private TextPaint paint = new TextPaint();
	private Typeface typeface;
	private boolean include_padding = true; // AOSP's default
	private int break_strategy = 0 /*BREAK_STRATEGY_SIMPLE*/;
	private int hyphenation_frequency = 0 /*HYPHENATION_FREQUENCY_NONE*/;
	private int gravity = Gravity.TOP | Gravity.START;
	/* AOSP: the height limits in lines (the pixel and ems modes are not applied here) */
	private static final int LINES = 1;
	private static final int EMS = LINES;
	private static final int PIXELS = 2;
	private int mMaximum = Integer.MAX_VALUE;
	private int mMaxMode = LINES;
	private int mMinimum = 0;
	private int mMinMode = LINES;
	private boolean mSingleLine;
	private boolean mHorizontallyScrolling;
	private TextUtils.TruncateAt mEllipsize;
	private InputFilter.LengthFilter mSingleLineLengthFilter = null;
	private static final int MAX_LENGTH_FOR_SINGLE_LINE_EDIT_TEXT = 5000;
	private TransformationMethod mTransformation;
	private CharSequence mTransformed = "";
	private BufferType mBufferType = BufferType.NORMAL;
	private static final int ELLIPSIZE_NOT_SET = -1;
	private static final int ELLIPSIZE_NONE = 0;
	private static final int ELLIPSIZE_START = 1;
	private static final int ELLIPSIZE_MIDDLE = 2;
	private static final int ELLIPSIZE_END = 3;
	private static final int ELLIPSIZE_MARQUEE = 4;

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
			if (a.hasValue(com.android.internal.R.styleable.TextView_textColorHint))
				setHintTextColor(a.getColorStateList(com.android.internal.R.styleable.TextView_textColorHint));
			if (a.hasValue(com.android.internal.R.styleable.TextView_textSize)) {
				// as AOSP: the pixel size and its unit are kept, the size is set with the layout request
				mTextSizeUnit = a.peekValue(com.android.internal.R.styleable.TextView_textSize).getComplexUnit();
				setRawTextSize(a.getDimensionPixelSize(com.android.internal.R.styleable.TextView_textSize, 10), true);
			}

			if (a.hasValue(com.android.internal.R.styleable.TextView_textStyle)) {
				int textStyle = a.getInt(com.android.internal.R.styleable.TextView_textStyle, 0);
				setTypeface(getTypeface(), textStyle);
			}

			include_padding = a.getBoolean(com.android.internal.R.styleable.TextView_includeFontPadding, true);

			if (a.hasValue(com.android.internal.R.styleable.TextView_gravity))
				setGravity(a.getInt(com.android.internal.R.styleable.TextView_gravity, Gravity.TOP | Gravity.START));

			int ellipsize = a.getInt(com.android.internal.R.styleable.TextView_ellipsize, ELLIPSIZE_NOT_SET);
			boolean singleLine = a.getBoolean(com.android.internal.R.styleable.TextView_singleLine, false);
			if (a.hasValue(com.android.internal.R.styleable.TextView_lines))
				setLines(a.getInt(com.android.internal.R.styleable.TextView_lines, -1));
			if (a.hasValue(com.android.internal.R.styleable.TextView_minLines))
				setMinLines(a.getInt(com.android.internal.R.styleable.TextView_minLines, -1));
			if (a.hasValue(com.android.internal.R.styleable.TextView_maxLines))
				setMaxLines(a.getInt(com.android.internal.R.styleable.TextView_maxLines, -1));
			int max_length = a.getInt(com.android.internal.R.styleable.TextView_maxLength, -1);
			if (max_length >= 0)
				setFilters(new InputFilter[] {new InputFilter.LengthFilter(max_length)});

			Drawable drawableLeft = a.getDrawable(com.android.internal.R.styleable.TextView_drawableLeft);
			Drawable drawableTop = a.getDrawable(com.android.internal.R.styleable.TextView_drawableTop);
			Drawable drawableRight = a.getDrawable(com.android.internal.R.styleable.TextView_drawableRight);
			Drawable drawableBottom = a.getDrawable(com.android.internal.R.styleable.TextView_drawableBottom);
			Drawable drawableStart = a.getDrawable(com.android.internal.R.styleable.TextView_drawableStart);
			Drawable drawableEnd = a.getDrawable(com.android.internal.R.styleable.TextView_drawableEnd);
			ColorStateList drawableTint = a.getColorStateList(com.android.internal.R.styleable.TextView_drawableTint);
			BlendMode drawableTintMode = Drawable.parseBlendMode(a.getInt(com.android.internal.R.styleable.TextView_drawableTintMode, -1), null);
			int drawablePadding = a.getDimensionPixelSize(com.android.internal.R.styleable.TextView_drawablePadding, 0);

			// Set up the tint (if needed) before setting the drawables so that it
			// gets applied correctly.
			if (drawableTint != null || drawableTintMode != null) {
				if (mDrawables == null) {
					mDrawables = new Drawables(context);
				}
				if (drawableTint != null) {
					mDrawables.mTintList = drawableTint;
					mDrawables.mHasTint = true;
				}
				if (drawableTintMode != null) {
					mDrawables.mBlendMode = drawableTintMode;
					mDrawables.mHasTintMode = true;
				}
			}

			// This call will save the initial left/right drawables
			setCompoundDrawablesWithIntrinsicBounds(drawableLeft, drawableTop, drawableRight, drawableBottom);
			setRelativeDrawablesIfNeeded(drawableStart, drawableEnd);
			setCompoundDrawablePadding(drawablePadding);

			// Same as setSingleLine(), but make sure the transformation method and the maximum number
			// of lines of height are unchanged for multi-line TextViews.
			setInputTypeSingleLine(singleLine);
			applySingleLine(singleLine, singleLine, singleLine,
			                // Does not apply automated max length filter since length filter will be resolved
			                // later in this function.
			                false);

			if (singleLine && getKeyListener() == null && ellipsize == ELLIPSIZE_NOT_SET) {
				ellipsize = ELLIPSIZE_END;
			}

			switch (ellipsize) {
				case ELLIPSIZE_START:
					setEllipsize(TextUtils.TruncateAt.START);
					break;
				case ELLIPSIZE_MIDDLE:
					setEllipsize(TextUtils.TruncateAt.MIDDLE);
					break;
				case ELLIPSIZE_END:
					setEllipsize(TextUtils.TruncateAt.END);
					break;
				case ELLIPSIZE_MARQUEE:
					// AOSP also fades the edge (setHorizontalFadingEdgeEnabled()); there is no scrolling text
					setEllipsize(TextUtils.TruncateAt.MARQUEE);
					break;
				case ELLIPSIZE_NONE:
				default:
					break;
			}

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
		setText(text, mBufferType);
	}

	public void setText(CharSequence text, BufferType type) {
		mBufferType = type;
		if (text == null)
			text = "";
		text = applyFilters(text);
		/* as AOSP's setText(): the buffer type (or a movement method, which needs spans) decides what getText()
		 * returns; the Editor, PrecomputedText and autoLink parts of AOSP do not apply here */
		if (type == BufferType.EDITABLE) {
			text = Editable.Factory.getInstance().newEditable(text);
		} else if (type == BufferType.SPANNABLE || mMovement != null) {
			text = Spannable.Factory.getInstance().newSpannable(text);
		} else {
			text = TextUtils.stringOrSpannedString(text);
		}
		this.text = text;
		/* as AOSP's setText(): the transformation decides what is shown (getText() stays the text that was set) */
		mTransformed = mTransformation == null ? this.text : mTransformation.getTransformation(this.text, this);
		updateNativeText();
		if (!isLayoutRequested())
			requestLayout();
	}

	private void updateNativeText() {
		if (mTransformed instanceof Spanned)
			native_setMarkup(toPangoMarkup((Spanned)mTransformed));
		else
			native_setText(mTransformed.toString());
	}

	private static final ClickableSpan[] NO_LINKS = new ClickableSpan[0];
	private ClickableSpan[] linkSpans = NO_LINKS;
	private ColorStateList linkTextColor;

	/* the character spans as Pango markup; a ClickableSpan becomes a link whose URI is its index in linkSpans */
	private String toPangoMarkup(Spanned text) {
		CharacterStyle[] spans = text.getSpans(0, text.length(), CharacterStyle.class);
		linkSpans = text.getSpans(0, text.length(), ClickableSpan.class);
		TreeSet<Integer> bounds = new TreeSet<>();
		bounds.add(0);
		bounds.add(text.length());
		for (CharacterStyle span : spans) {
			bounds.add(Math.max(0, Math.min(text.getSpanStart(span), text.length())));
			bounds.add(Math.max(0, Math.min(text.getSpanEnd(span), text.length())));
		}
		int linkColor = getLinkTextColors().getColorForState(getDrawableState(), getLinkTextColors().getDefaultColor());
		StringBuilder markup = new StringBuilder();
		Integer start = bounds.first();
		for (Integer end : bounds.tailSet(start, false)) {
			StringBuilder open = new StringBuilder();
			StringBuilder close = new StringBuilder();
			for (CharacterStyle span : spans) {
				if (text.getSpanStart(span) > start || text.getSpanEnd(span) < end)
					continue;
				String tag = null;
				String attrs = null;
				if (span instanceof ClickableSpan) {
					tag = "a";
					attrs = String.format("href=\"%d\"><span %s underline=\"single\"", Arrays.asList(linkSpans).indexOf(span), pangoColor(linkColor));
				} else if (span instanceof StyleSpan && (((StyleSpan)span).getStyle() & Typeface.BOLD) != 0) {
					tag = "b";
				} else if (span instanceof StyleSpan && (((StyleSpan)span).getStyle() & Typeface.ITALIC) != 0) {
					tag = "i";
				} else if (span instanceof UnderlineSpan) {
					tag = "u";
				} else if (span instanceof StrikethroughSpan) {
					tag = "s";
				} else if (span instanceof ForegroundColorSpan) {
					tag = "span";
					attrs = pangoColor(((ForegroundColorSpan)span).getForegroundColor());
				}
				if (tag == null)
					continue;
				open.append('<').append(tag).append(attrs != null ? " " + attrs : "").append('>');
				close.insert(0, "</" + tag + ">");
				if (span instanceof ClickableSpan)
					close.insert(0, "</span>");
			}
			markup.append(open);
			for (int i = start; i < end; i++) {
				char c = text.charAt(i);
				markup.append(c == '&' ? "&amp;" : c == '<' ? "&lt;" : c == '>' ? "&gt;" : String.valueOf(c));
			}
			markup.append(close);
			start = end;
		}
		return markup.toString();
	}

	/* with its own alpha, otherwise GTK draws the span with the alpha of the text color */
	private static String pangoColor(int color) {
		return String.format("foreground=\"#%06x\" fgalpha=\"%d\"", color & 0xffffff, Math.max(1, (color >>> 24) * 65535 / 255));
	}

	/* called when a link of the markup is activated; like LinkMovementMethod, only then do links react */
	private boolean onLinkActivated(String uri) {
		if (!(mMovement instanceof LinkMovementMethod))
			return true;
		try {
			linkSpans[Integer.parseInt(uri)].onClick(this);
		} catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
		}
		return true;
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

	private native final void native_setMarkup(String markup);

	public native final void native_setText(String text);

	/* android-14 TextView.setTextSize(), setTextSizeInternal(), setRawTextSize() and getTextSize(): the size is
	 * converted to pixels with the display metrics, the paint keeps it (getTextSize(), getPaint().getTextSize()) */
	public void setTextSize(float size) {
		setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
	}

	public void setTextSize(int unit, float size) {
		setTextSizeInternal(unit, size, true);
	}

	private void setTextSizeInternal(int unit, float size, boolean shouldRequestLayout) {
		mTextSizeUnit = unit;
		Context c = getContext();
		Resources r;

		if (c == null) {
			r = Resources.getSystem();
		} else {
			r = c.getResources();
		}

		setRawTextSize(TypedValue.applyDimension(unit, size, r.getDisplayMetrics()), shouldRequestLayout);
	}

	/* AOSP skips this when the paint already has the size; here the label's size is independent of the
	 * paint's initial one, so it is always passed on */
	private void setRawTextSize(float size, boolean shouldRequestLayout) {
		getPaint().setTextSize(size);
		applyNativeTextSize(size);

		if (shouldRequestLayout) {
			requestLayout();
			invalidate();
		}
	}

	private int mTextSizeUnit = -1;

	/* the GtkLabel takes the size as an absolute Pango size in pixels; Button, CheckBox, CompoundButton and
	 * RadioButton (native widgets with their own text size) ignore it, EditText sets it on its GtkText */
	protected void applyNativeTextSize(float size) {
		native_setTextSize(size);
	}
	private native void native_setTextSize(float size);

	public native final void native_setTextColor(int color);
	public void setTextColor(int color) {
		setTextColor(ColorStateList.valueOf(color));
	}
	public void setTextColor(ColorStateList colors) {
		if (colors != null) {
			this.colors = colors;
			updateTextColors();
		}
	}

	/* AOSP updateTextColors(): the color for the current drawable state, which the label is given as well */
	private void updateTextColors() {
		mCurTextColor = colors.getColorForState(getDrawableState(), colors.getDefaultColor());
		native_setTextColor(mCurTextColor);
	}

	@Override
	protected void drawableStateChanged() {
		super.drawableStateChanged();
		if (colors.isStateful())
			updateTextColors();

		/* AOSP TextView.drawableStateChanged() */
		if (mDrawables != null) {
			final int[] state = getDrawableState();
			for (Drawable dr : mDrawables.mShowing) {
				if (dr != null && dr.isStateful() && dr.setState(state)) {
					invalidateDrawable(dr);
				}
			}
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


	/* API 29 */
	public final boolean isHorizontallyScrollable() {
		return mHorizontallyScrolling;
	}

	public void setInputType(int type) {}
	private static final Spanned EMPTY_SPANNED = new SpannedString("");
	private static final InputFilter[] NO_FILTERS = new InputFilter[0];
	private InputFilter[] filters = NO_FILTERS;
	public void setFilters(InputFilter[] filters) {
		if (filters == null)
			throw new IllegalArgumentException();
		this.filters = filters;
	}

	/* as AOSP's setText(): the whole new text goes through the filters, replacing an empty text */
	protected CharSequence applyFilters(CharSequence text) {
		if (text == null)
			return null;
		for (InputFilter filter : filters) {
			CharSequence out = filter.filter(text, 0, text.length(), EMPTY_SPANNED, 0, 0);
			if (out != null)
				text = out;
		}
		return text;
	}
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

	// the hint is not shown, but the colors are kept for getHintTextColors()
	private ColorStateList hint_colors = null;
	public void setHintTextColor(ColorStateList colorStateList) {
		hint_colors = colorStateList;
	}
	public void setHintTextColor(int i) {
		setHintTextColor(ColorStateList.valueOf(i));
	}
	public final ColorStateList getHintTextColors() {
		return hint_colors;
	}
	public void setLinkTextColor(ColorStateList colorStateList) {
		linkTextColor = colorStateList;
		if (mTransformed instanceof Spanned)
			native_setMarkup(toPangoMarkup((Spanned)mTransformed));
	}

	public final ColorStateList getLinkTextColors() {
		if (linkTextColor == null) {
			TypedArray a = getContext().obtainStyledAttributes(new int[] {android.R.attr.textColorLink});
			linkTextColor = a.getColorStateList(0);
			a.recycle();
			if (linkTextColor == null)
				linkTextColor = ColorStateList.valueOf(0xff0000ee);
		}
		return linkTextColor;
	}

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

	/* AOSP: values of the typeface attribute */
	private static final int SANS = 1;
	private static final int SERIF = 2;
	private static final int MONOSPACE = 3;
	private static final int DEFAULT_TYPEFACE = -1;


	private static class TextAppearanceAttributes {
		int mTextColorHighlight = 0;
		int mSearchResultHighlightColor = 0;
		int mFocusedSearchResultHighlightColor = 0;
		ColorStateList mTextColor = null;
		ColorStateList mTextColorHint = null;
		ColorStateList mTextColorLink = null;
		int mTextSize = -1;
		int mTextSizeUnit = -1;
		LocaleList mTextLocales = null;
		String mFontFamily = null;
		Typeface mFontTypeface = null;
		boolean mFontFamilyExplicit = false;
		int mTypefaceIndex = -1;
		int mTextStyle = 0;
		int mFontWeight = FontStyle.FONT_WEIGHT_UNSPECIFIED;
		boolean mAllCaps = false;
		int mShadowColor = 0;
		float mShadowDx = 0, mShadowDy = 0, mShadowRadius = 0;
		boolean mHasElegant = false;
		boolean mElegant = false;
		boolean mHasFallbackLineSpacing = false;
		boolean mFallbackLineSpacing = false;
		boolean mHasLetterSpacing = false;
		float mLetterSpacing = 0;
		String mFontFeatureSettings = null;
		String mFontVariationSettings = null;

	}

	private void readTextAppearance(Context context, TypedArray appearance,
			TextAppearanceAttributes attributes) {
		final int n = appearance.getIndexCount();
		for (int i = 0; i < n; i++) {
			final int attr = appearance.getIndex(i);
			int index = attr;
			switch (index) {
				case com.android.internal.R.styleable.TextAppearance_textColorHighlight:
					attributes.mTextColorHighlight =
							appearance.getColor(attr, attributes.mTextColorHighlight);
					break;
				case com.android.internal.R.styleable.TextAppearance_searchResultHighlightColor:
					attributes.mSearchResultHighlightColor =
							appearance.getColor(attr, attributes.mSearchResultHighlightColor);
					break;
				case com.android.internal.R.styleable
						.TextAppearance_focusedSearchResultHighlightColor:
					attributes.mFocusedSearchResultHighlightColor =
							appearance.getColor(attr,
									attributes.mFocusedSearchResultHighlightColor);
					break;
				case com.android.internal.R.styleable.TextAppearance_textColor:
					attributes.mTextColor = appearance.getColorStateList(attr);
					break;
				case com.android.internal.R.styleable.TextAppearance_textColorHint:
					attributes.mTextColorHint = appearance.getColorStateList(attr);
					break;
				case com.android.internal.R.styleable.TextAppearance_textColorLink:
					attributes.mTextColorLink = appearance.getColorStateList(attr);
					break;
				case com.android.internal.R.styleable.TextAppearance_textSize:
					attributes.mTextSize =
							appearance.getDimensionPixelSize(attr, attributes.mTextSize);
					attributes.mTextSizeUnit = appearance.peekValue(attr).getComplexUnit();
					break;
				case com.android.internal.R.styleable.TextAppearance_textLocale:
					final String localeString = appearance.getString(attr);
					if (localeString != null) {
						final LocaleList localeList = LocaleList.forLanguageTags(localeString);
						if (!localeList.isEmpty()) {
							attributes.mTextLocales = localeList;
						}
					}
					break;
				case com.android.internal.R.styleable.TextAppearance_typeface:
					attributes.mTypefaceIndex = appearance.getInt(attr, attributes.mTypefaceIndex);
					if (attributes.mTypefaceIndex != -1 && !attributes.mFontFamilyExplicit) {
						attributes.mFontFamily = null;
					}
					break;
				case com.android.internal.R.styleable.TextAppearance_fontFamily:
					if (attributes.mFontTypeface == null) {
						attributes.mFontFamily = appearance.getString(attr);
					}
					attributes.mFontFamilyExplicit = true;
					break;
				case com.android.internal.R.styleable.TextAppearance_textStyle:
					attributes.mTextStyle = appearance.getInt(attr, attributes.mTextStyle);
					break;
				case com.android.internal.R.styleable.TextAppearance_textFontWeight:
					attributes.mFontWeight = appearance.getInt(attr, attributes.mFontWeight);
					break;
				case com.android.internal.R.styleable.TextAppearance_textAllCaps:
					attributes.mAllCaps = appearance.getBoolean(attr, attributes.mAllCaps);
					break;
				case com.android.internal.R.styleable.TextAppearance_shadowColor:
					attributes.mShadowColor = appearance.getInt(attr, attributes.mShadowColor);
					break;
				case com.android.internal.R.styleable.TextAppearance_shadowDx:
					attributes.mShadowDx = appearance.getFloat(attr, attributes.mShadowDx);
					break;
				case com.android.internal.R.styleable.TextAppearance_shadowDy:
					attributes.mShadowDy = appearance.getFloat(attr, attributes.mShadowDy);
					break;
				case com.android.internal.R.styleable.TextAppearance_shadowRadius:
					attributes.mShadowRadius = appearance.getFloat(attr, attributes.mShadowRadius);
					break;
				case com.android.internal.R.styleable.TextAppearance_elegantTextHeight:
					attributes.mHasElegant = true;
					attributes.mElegant = appearance.getBoolean(attr, attributes.mElegant);
					break;
				case com.android.internal.R.styleable.TextAppearance_fallbackLineSpacing:
					attributes.mHasFallbackLineSpacing = true;
					attributes.mFallbackLineSpacing = appearance.getBoolean(attr,
							attributes.mFallbackLineSpacing);
					break;
				case com.android.internal.R.styleable.TextAppearance_letterSpacing:
					attributes.mHasLetterSpacing = true;
					attributes.mLetterSpacing =
							appearance.getFloat(attr, attributes.mLetterSpacing);
					break;
				case com.android.internal.R.styleable.TextAppearance_fontFeatureSettings:
					attributes.mFontFeatureSettings = appearance.getString(attr);
					break;
				case com.android.internal.R.styleable.TextAppearance_fontVariationSettings:
					attributes.mFontVariationSettings = appearance.getString(attr);
					break;
				default:
			}
		}
	}

	private void applyTextAppearance(TextAppearanceAttributes attributes) {
		if (attributes.mTextColor != null) {
			setTextColor(attributes.mTextColor);
		}

		if (attributes.mTextColorHint != null) {
			setHintTextColor(attributes.mTextColorHint);
		}

		if (attributes.mTextColorLink != null) {
			setLinkTextColor(attributes.mTextColorLink);
		}

		if (attributes.mTextColorHighlight != 0) {
			setHighlightColor(attributes.mTextColorHighlight);
		}

		if (attributes.mTextSize != -1) {
			mTextSizeUnit = attributes.mTextSizeUnit;
			setRawTextSize(attributes.mTextSize, true /* shouldRequestLayout */);
		}

		if (attributes.mTypefaceIndex != -1 && !attributes.mFontFamilyExplicit) {
			attributes.mFontFamily = null;
		}
		// AOSP always calls this; with nothing specified it sets the default typeface again, which here
		// would only cost a native call for every styled TextView
		if (attributes.mFontTypeface != null || attributes.mFontFamily != null || attributes.mTypefaceIndex != -1
				|| attributes.mTextStyle != 0 || attributes.mFontWeight != FontStyle.FONT_WEIGHT_UNSPECIFIED) {
			setTypefaceFromAttrs(attributes.mFontTypeface, attributes.mFontFamily,
					attributes.mTypefaceIndex, attributes.mTextStyle, attributes.mFontWeight);
		}

		if (attributes.mShadowColor != 0) {
			setShadowLayer(attributes.mShadowRadius, attributes.mShadowDx, attributes.mShadowDy,
					attributes.mShadowColor);
		}

		if (attributes.mAllCaps) {
			setAllCaps(true); // a CSS class of the label instead of AOSP's AllCapsTransformationMethod
		}



		if (attributes.mHasLetterSpacing) {
			setLetterSpacing(attributes.mLetterSpacing);
		}



	}

	private void setTypefaceFromAttrs(Typeface typeface, String familyName,
			int typefaceIndex, int style,
					int weight) {
		if (typeface == null && familyName != null) {
			// Lookup normal Typeface from system font map.
			final Typeface normalTypeface = Typeface.create(familyName, Typeface.NORMAL);
			resolveStyleAndSetTypeface(normalTypeface, style, weight);
		} else if (typeface != null) {
			resolveStyleAndSetTypeface(typeface, style, weight);
		} else {  // both typeface and familyName is null.
			switch (typefaceIndex) {
				case SANS:
					resolveStyleAndSetTypeface(Typeface.SANS_SERIF, style, weight);
					break;
				case SERIF:
					resolveStyleAndSetTypeface(Typeface.SERIF, style, weight);
					break;
				case MONOSPACE:
					resolveStyleAndSetTypeface(Typeface.MONOSPACE, style, weight);
					break;
				case DEFAULT_TYPEFACE:
				default:
					resolveStyleAndSetTypeface(null, style, weight);
					break;
			}
		}
	}

	private void resolveStyleAndSetTypeface(Typeface typeface, int style,
					int weight) {
		if (weight >= 0) {
			weight = Math.min(FontStyle.FONT_WEIGHT_MAX, weight);
			final boolean italic = (style & Typeface.ITALIC) != 0;
			setTypeface(Typeface.create(typeface, weight, italic));
		} else {
			setTypeface(typeface, style);
		}
	}

	public void setTextAppearance(Context context, int resId) {
		final TypedArray ta = context.obtainStyledAttributes(resId, com.android.internal.R.styleable.TextAppearance);
		final TextAppearanceAttributes attributes = new TextAppearanceAttributes();
		readTextAppearance(context, ta, attributes);
		ta.recycle();
		applyTextAppearance(attributes);
	}

	/* GtkLabel can only limit the number of lines when ellipsizing, so text cut off
	 * after maxLines always gets an ellipsis, also when no ellipsize mode is set.
	 * There is no minimum number of lines (minLines/lines only set the AOSP state). */
	private void updateLines() {
		int mode = 3; // PANGO_ELLIPSIZE_END
		if (mEllipsize == TextUtils.TruncateAt.START)
			mode = 1; // PANGO_ELLIPSIZE_START
		else if (mEllipsize == TextUtils.TruncateAt.MIDDLE)
			mode = 2; // PANGO_ELLIPSIZE_MIDDLE
		boolean limited = mMaxMode == LINES && mMaximum > 0 && mMaximum != Integer.MAX_VALUE;
		native_setLines(limited ? mMaximum : -1, limited ? mode : 0);
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

	public float getTextSize() {
		return getPaint().getTextSize();
	}

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

	/* like AOSP: the baseline of the first line, below the top padding and moved by the vertical
	 * gravity (BaselineLayout, baseline-aligned LinearLayouts and others place the text by it) */
	@Override
	public int getBaseline() {
		int baseline = native_getBaseline(widget, getMeasuredWidth() - getPaddingLeft() - getPaddingRight(),
		                                  getMeasuredHeight() - getPaddingTop() - getPaddingBottom());
		return baseline < 0 ? super.getBaseline() : getPaddingTop() + baseline;
	}
	private native int native_getBaseline(long widget, int width, int height);

	public int getGravity() {
		return gravity;
	}

	public CharSequence getText() {
		return text;
	};

	private native void native_setCompoundDrawablePadding(long widget, int pad);
	private native void native_setTypeface(long widget, Typeface tf);

	protected native void native_setCompoundDrawables(long widget, long left, long top, long right, long bottom);

	public void setAllCaps(boolean allCaps) {
		String[] classesToRemove = {"ATL-text-uppercase"};
		native_removeClasses(widget, classesToRemove);

		if (allCaps) {
			native_addClass(widget, "ATL-text-uppercase");
		}
	}

	public void setSaveEnabled(boolean enabled) {}


	public void setEditableFactory(Editable.Factory factory) {}

	public KeyListener getKeyListener() { return null; }

	public int getInputType() { return 0; }

	public InputFilter[] getFilters() { return filters; }

	public int getLineCount() { return 1; }

	public URLSpan[] getUrls() { return new URLSpan[0]; }

	/* as AOSP: the movement method, null unless set (or the default of a subclass) */
	private MovementMethod mMovement;

	protected MovementMethod getDefaultMovementMethod() {
		return null;
	}

	public final void setMovementMethod(MovementMethod movement) {
		if (mMovement != movement) {
			mMovement = movement;

			if (movement != null && !(getText() instanceof Spannable)) {
				setText(getText());
			}

			// (AOSP also makes the view focusable and clickable here and tells the Editor: not needed, the
			// GtkLabel handles its links itself)
		}
	}

	public void setTextIsSelectable(boolean selectable) {}

	public final MovementMethod getMovementMethod() {
		return mMovement;
	}


	public int getMinHeight() { return 0; }
	public int getMinWidth() { return 0; }
	public void setMinHeight(int minHeight) {}
	public void setMaxHeight(int maxHeight) {}


	public static interface OnEditorActionListener {
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
		return mCurTextColor;
	}

	public void setHint(int resId) {
		setHint(getContext().getResources().getText(resId));
	}

	public float getLetterSpacing() { return 0.f; }


	public boolean isTextSelectable() { return true; }


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

	public void setLinkTextColor(int color) {
		setLinkTextColor(ColorStateList.valueOf(color));
	}

	public void setCustomSelectionActionModeCallback(ActionMode.Callback actionModeCallback) {}

	public void setRawInputType(int type) {}

	public void setSelectAllOnFocus(boolean selectAllOnFocus) {}

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

	public int getHyphenationFrequency() {
		return hyphenation_frequency;
	}

	public void setHyphenationFrequency(int hyphenationFrequency) {
		hyphenation_frequency = hyphenationFrequency;
	}

	public boolean getIncludeFontPadding() { return include_padding; }

	public void setIncludeFontPadding(boolean includePadding) {
		include_padding = includePadding;
		requestLayout();
	}

	/* the font padding depends on the font, which may have changed since the last measure */
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		native_setFontPadding(widget, include_padding);
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}
	private native void native_setFontPadding(long widget, boolean include);

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


	/* ATL: AOSP's onDraw() paints the compound drawables itself; here the GtkBox of the label does, so every
	 * change of the drawables that are shown (mShowing, already resolved for the layout direction) is passed
	 * to it. Subclasses whose native widget is no GtkBox (Button, EditText) or that add a drawable
	 * (CheckedTextView) override this. */
	protected void updateNativeCompoundDrawables() {
		final Drawable[] d = getCompoundDrawables();
		boolean any = d[Drawables.LEFT] != null || d[Drawables.TOP] != null || d[Drawables.RIGHT] != null || d[Drawables.BOTTOM] != null;
		if (!any && !nativeHasDrawables)
			return; // the constructor sets "no drawables": nothing to tell the native side
		nativeHasDrawables = any;
		native_setCompoundDrawables(widget, d[Drawables.LEFT] != null ? d[Drawables.LEFT].paintable : 0,
				d[Drawables.TOP] != null ? d[Drawables.TOP].paintable : 0,
				d[Drawables.RIGHT] != null ? d[Drawables.RIGHT].paintable : 0,
				d[Drawables.BOTTOM] != null ? d[Drawables.BOTTOM].paintable : 0);
	}

	/* resolveDrawables() and the reset on a direction change are View's, as in AOSP */

	/* no text layout here (GtkLabel), so the text never needs the scrolling space of the extended padding
	 * and has no vertical offset: the AOSP values for a text that fits */
	public int getExtendedPaddingTop() {
		return getCompoundPaddingTop();
	}

	public int getExtendedPaddingBottom() {
		return getCompoundPaddingBottom();
	}

	public int getTotalPaddingTop() {
		return getExtendedPaddingTop();
	}

	public int getTotalPaddingBottom() {
		return getExtendedPaddingBottom();
	}

	Drawables mDrawables;
	private boolean nativeHasDrawables;
	private int mLastLayoutDirection = -1;

	static class Drawables {
		static final int LEFT = 0;
		static final int TOP = 1;
		static final int RIGHT = 2;
		static final int BOTTOM = 3;

		static final int DRAWABLE_NONE = -1;
		static final int DRAWABLE_RIGHT = 0;
		static final int DRAWABLE_LEFT = 1;

		final Rect mCompoundRect = new Rect();

		final Drawable[] mShowing = new Drawable[4];

		ColorStateList mTintList;
		BlendMode mBlendMode;
		boolean mHasTint;
		boolean mHasTintMode;

		Drawable mDrawableStart, mDrawableEnd, mDrawableError, mDrawableTemp;
		Drawable mDrawableLeftInitial, mDrawableRightInitial;

		boolean mIsRtlCompatibilityMode;
		boolean mOverride;

		int mDrawableSizeTop, mDrawableSizeBottom, mDrawableSizeLeft, mDrawableSizeRight,
				mDrawableSizeStart, mDrawableSizeEnd, mDrawableSizeError, mDrawableSizeTemp;

		int mDrawableWidthTop, mDrawableWidthBottom, mDrawableHeightLeft, mDrawableHeightRight,
				mDrawableHeightStart, mDrawableHeightEnd, mDrawableHeightError, mDrawableHeightTemp;

		int mDrawablePadding;

		int mDrawableSaved = DRAWABLE_NONE;

		public Drawables(Context context) {
			final int targetSdkVersion = context.getApplicationInfo().targetSdkVersion;
			mIsRtlCompatibilityMode = targetSdkVersion < VERSION_CODES.JELLY_BEAN_MR1
					|| !context.getApplicationInfo().hasRtlSupport();
			mOverride = false;
		}

		/**
		 * @return {@code true} if this object contains metadata that needs to
		 *         be retained, {@code false} otherwise
		 */
		public boolean hasMetadata() {
			return mDrawablePadding != 0 || mHasTintMode || mHasTint;
		}

		/**
		 * Updates the list of displayed drawables to account for the current
		 * layout direction.
		 *
		 * @param layoutDirection the current layout direction
		 * @return {@code true} if the displayed drawables changed
		 */
		public boolean resolveWithLayoutDirection(int layoutDirection) {
			final Drawable previousLeft = mShowing[Drawables.LEFT];
			final Drawable previousRight = mShowing[Drawables.RIGHT];

			// First reset "left" and "right" drawables to their initial values
			mShowing[Drawables.LEFT] = mDrawableLeftInitial;
			mShowing[Drawables.RIGHT] = mDrawableRightInitial;

			if (mIsRtlCompatibilityMode) {
				// Use "start" drawable as "left" drawable if the "left" drawable was not defined
				if (mDrawableStart != null && mShowing[Drawables.LEFT] == null) {
					mShowing[Drawables.LEFT] = mDrawableStart;
					mDrawableSizeLeft = mDrawableSizeStart;
					mDrawableHeightLeft = mDrawableHeightStart;
				}
				// Use "end" drawable as "right" drawable if the "right" drawable was not defined
				if (mDrawableEnd != null && mShowing[Drawables.RIGHT] == null) {
					mShowing[Drawables.RIGHT] = mDrawableEnd;
					mDrawableSizeRight = mDrawableSizeEnd;
					mDrawableHeightRight = mDrawableHeightEnd;
				}
			} else {
				// JB-MR1+ normal case: "start" / "end" drawables are overriding "left" / "right"
				// drawable if and only if they have been defined
				switch(layoutDirection) {
					case LAYOUT_DIRECTION_RTL:
						if (mOverride) {
							mShowing[Drawables.RIGHT] = mDrawableStart;
							mDrawableSizeRight = mDrawableSizeStart;
							mDrawableHeightRight = mDrawableHeightStart;

							mShowing[Drawables.LEFT] = mDrawableEnd;
							mDrawableSizeLeft = mDrawableSizeEnd;
							mDrawableHeightLeft = mDrawableHeightEnd;
						}
						break;

					case LAYOUT_DIRECTION_LTR:
					default:
						if (mOverride) {
							mShowing[Drawables.LEFT] = mDrawableStart;
							mDrawableSizeLeft = mDrawableSizeStart;
							mDrawableHeightLeft = mDrawableHeightStart;

							mShowing[Drawables.RIGHT] = mDrawableEnd;
							mDrawableSizeRight = mDrawableSizeEnd;
							mDrawableHeightRight = mDrawableHeightEnd;
						}
						break;
				}
			}

			applyErrorDrawableIfNeeded(layoutDirection);

			return mShowing[Drawables.LEFT] != previousLeft
					|| mShowing[Drawables.RIGHT] != previousRight;
		}

		public void setErrorDrawable(Drawable dr, TextView tv) {
			if (mDrawableError != dr && mDrawableError != null) {
				mDrawableError.setCallback(null);
			}
			mDrawableError = dr;

			if (mDrawableError != null) {
				final Rect compoundRect = mCompoundRect;
				final int[] state = tv.getDrawableState();

				mDrawableError.setState(state);
				mDrawableError.copyBounds(compoundRect);
				mDrawableError.setCallback(tv);
				mDrawableSizeError = compoundRect.width();
				mDrawableHeightError = compoundRect.height();
			} else {
				mDrawableSizeError = mDrawableHeightError = 0;
			}
		}

		private void applyErrorDrawableIfNeeded(int layoutDirection) {
			// first restore the initial state if needed
			switch (mDrawableSaved) {
				case DRAWABLE_LEFT:
					mShowing[Drawables.LEFT] = mDrawableTemp;
					mDrawableSizeLeft = mDrawableSizeTemp;
					mDrawableHeightLeft = mDrawableHeightTemp;
					break;
				case DRAWABLE_RIGHT:
					mShowing[Drawables.RIGHT] = mDrawableTemp;
					mDrawableSizeRight = mDrawableSizeTemp;
					mDrawableHeightRight = mDrawableHeightTemp;
					break;
				case DRAWABLE_NONE:
				default:
			}
			// then, if needed, assign the Error drawable to the correct location
			if (mDrawableError != null) {
				switch(layoutDirection) {
					case LAYOUT_DIRECTION_RTL:
						mDrawableSaved = DRAWABLE_LEFT;

						mDrawableTemp = mShowing[Drawables.LEFT];
						mDrawableSizeTemp = mDrawableSizeLeft;
						mDrawableHeightTemp = mDrawableHeightLeft;

						mShowing[Drawables.LEFT] = mDrawableError;
						mDrawableSizeLeft = mDrawableSizeError;
						mDrawableHeightLeft = mDrawableHeightError;
						break;
					case LAYOUT_DIRECTION_LTR:
					default:
						mDrawableSaved = DRAWABLE_RIGHT;

						mDrawableTemp = mShowing[Drawables.RIGHT];
						mDrawableSizeTemp = mDrawableSizeRight;
						mDrawableHeightTemp = mDrawableHeightRight;

						mShowing[Drawables.RIGHT] = mDrawableError;
						mDrawableSizeRight = mDrawableSizeError;
						mDrawableHeightRight = mDrawableHeightError;
						break;
				}
			}
		}
	}

	public void setCompoundDrawables(Drawable left, Drawable top,
			Drawable right, Drawable bottom) {
		Drawables dr = mDrawables;

		// We're switching to absolute, discard relative.
		if (dr != null) {
			if (dr.mDrawableStart != null) dr.mDrawableStart.setCallback(null);
			dr.mDrawableStart = null;
			if (dr.mDrawableEnd != null) dr.mDrawableEnd.setCallback(null);
			dr.mDrawableEnd = null;
			dr.mDrawableSizeStart = dr.mDrawableHeightStart = 0;
			dr.mDrawableSizeEnd = dr.mDrawableHeightEnd = 0;
		}

		final boolean drawables = left != null || top != null || right != null || bottom != null;
		if (!drawables) {
			// Clearing drawables...  can we free the data structure?
			if (dr != null) {
				if (!dr.hasMetadata()) {
					mDrawables = null;
				} else {
					// We need to retain the last set padding, so just clear
					// out all of the fields in the existing structure.
					for (int i = dr.mShowing.length - 1; i >= 0; i--) {
						if (dr.mShowing[i] != null) {
							dr.mShowing[i].setCallback(null);
						}
						dr.mShowing[i] = null;
					}
					dr.mDrawableSizeLeft = dr.mDrawableHeightLeft = 0;
					dr.mDrawableSizeRight = dr.mDrawableHeightRight = 0;
					dr.mDrawableSizeTop = dr.mDrawableWidthTop = 0;
					dr.mDrawableSizeBottom = dr.mDrawableWidthBottom = 0;
				}
			}
		} else {
			if (dr == null) {
				mDrawables = dr = new Drawables(getContext());
			}

			mDrawables.mOverride = false;

			if (dr.mShowing[Drawables.LEFT] != left && dr.mShowing[Drawables.LEFT] != null) {
				dr.mShowing[Drawables.LEFT].setCallback(null);
			}
			dr.mShowing[Drawables.LEFT] = left;

			if (dr.mShowing[Drawables.TOP] != top && dr.mShowing[Drawables.TOP] != null) {
				dr.mShowing[Drawables.TOP].setCallback(null);
			}
			dr.mShowing[Drawables.TOP] = top;

			if (dr.mShowing[Drawables.RIGHT] != right && dr.mShowing[Drawables.RIGHT] != null) {
				dr.mShowing[Drawables.RIGHT].setCallback(null);
			}
			dr.mShowing[Drawables.RIGHT] = right;

			if (dr.mShowing[Drawables.BOTTOM] != bottom && dr.mShowing[Drawables.BOTTOM] != null) {
				dr.mShowing[Drawables.BOTTOM].setCallback(null);
			}
			dr.mShowing[Drawables.BOTTOM] = bottom;

			final Rect compoundRect = dr.mCompoundRect;
			int[] state;

			state = getDrawableState();

			if (left != null) {
				left.setState(state);
				left.copyBounds(compoundRect);
				left.setCallback(this);
				dr.mDrawableSizeLeft = compoundRect.width();
				dr.mDrawableHeightLeft = compoundRect.height();
			} else {
				dr.mDrawableSizeLeft = dr.mDrawableHeightLeft = 0;
			}

			if (right != null) {
				right.setState(state);
				right.copyBounds(compoundRect);
				right.setCallback(this);
				dr.mDrawableSizeRight = compoundRect.width();
				dr.mDrawableHeightRight = compoundRect.height();
			} else {
				dr.mDrawableSizeRight = dr.mDrawableHeightRight = 0;
			}

			if (top != null) {
				top.setState(state);
				top.copyBounds(compoundRect);
				top.setCallback(this);
				dr.mDrawableSizeTop = compoundRect.height();
				dr.mDrawableWidthTop = compoundRect.width();
			} else {
				dr.mDrawableSizeTop = dr.mDrawableWidthTop = 0;
			}

			if (bottom != null) {
				bottom.setState(state);
				bottom.copyBounds(compoundRect);
				bottom.setCallback(this);
				dr.mDrawableSizeBottom = compoundRect.height();
				dr.mDrawableWidthBottom = compoundRect.width();
			} else {
				dr.mDrawableSizeBottom = dr.mDrawableWidthBottom = 0;
			}
		}

		// Save initial left/right drawables
		if (dr != null) {
			dr.mDrawableLeftInitial = left;
			dr.mDrawableRightInitial = right;
		}

		resetResolvedDrawables();
		resolveDrawables();
		applyCompoundDrawableTint();
		updateNativeCompoundDrawables();
		invalidate();
		requestLayout();
	}

	public void setCompoundDrawablesWithIntrinsicBounds(int left,
			int top, int right, int bottom) {
		final Context context = getContext();
		setCompoundDrawablesWithIntrinsicBounds(left != 0 ? context.getDrawable(left) : null,
				top != 0 ? context.getDrawable(top) : null,
				right != 0 ? context.getDrawable(right) : null,
				bottom != 0 ? context.getDrawable(bottom) : null);
	}

	public void setCompoundDrawablesWithIntrinsicBounds(Drawable left,
			Drawable top, Drawable right, Drawable bottom) {

		if (left != null) {
			left.setBounds(0, 0, left.getIntrinsicWidth(), left.getIntrinsicHeight());
		}
		if (right != null) {
			right.setBounds(0, 0, right.getIntrinsicWidth(), right.getIntrinsicHeight());
		}
		if (top != null) {
			top.setBounds(0, 0, top.getIntrinsicWidth(), top.getIntrinsicHeight());
		}
		if (bottom != null) {
			bottom.setBounds(0, 0, bottom.getIntrinsicWidth(), bottom.getIntrinsicHeight());
		}
		setCompoundDrawables(left, top, right, bottom);
	}

	public void setCompoundDrawablesRelative(Drawable start, Drawable top,
			Drawable end, Drawable bottom) {
		Drawables dr = mDrawables;

		// We're switching to relative, discard absolute.
		if (dr != null) {
			if (dr.mShowing[Drawables.LEFT] != null) {
				dr.mShowing[Drawables.LEFT].setCallback(null);
			}
			dr.mShowing[Drawables.LEFT] = dr.mDrawableLeftInitial = null;
			if (dr.mShowing[Drawables.RIGHT] != null) {
				dr.mShowing[Drawables.RIGHT].setCallback(null);
			}
			dr.mShowing[Drawables.RIGHT] = dr.mDrawableRightInitial = null;
			dr.mDrawableSizeLeft = dr.mDrawableHeightLeft = 0;
			dr.mDrawableSizeRight = dr.mDrawableHeightRight = 0;
		}

		final boolean drawables = start != null || top != null
				|| end != null || bottom != null;

		if (!drawables) {
			// Clearing drawables...  can we free the data structure?
			if (dr != null) {
				if (!dr.hasMetadata()) {
					mDrawables = null;
				} else {
					// We need to retain the last set padding, so just clear
					// out all of the fields in the existing structure.
					if (dr.mDrawableStart != null) dr.mDrawableStart.setCallback(null);
					dr.mDrawableStart = null;
					if (dr.mShowing[Drawables.TOP] != null) {
						dr.mShowing[Drawables.TOP].setCallback(null);
					}
					dr.mShowing[Drawables.TOP] = null;
					if (dr.mDrawableEnd != null) {
						dr.mDrawableEnd.setCallback(null);
					}
					dr.mDrawableEnd = null;
					if (dr.mShowing[Drawables.BOTTOM] != null) {
						dr.mShowing[Drawables.BOTTOM].setCallback(null);
					}
					dr.mShowing[Drawables.BOTTOM] = null;
					dr.mDrawableSizeStart = dr.mDrawableHeightStart = 0;
					dr.mDrawableSizeEnd = dr.mDrawableHeightEnd = 0;
					dr.mDrawableSizeTop = dr.mDrawableWidthTop = 0;
					dr.mDrawableSizeBottom = dr.mDrawableWidthBottom = 0;
				}
			}
		} else {
			if (dr == null) {
				mDrawables = dr = new Drawables(getContext());
			}

			mDrawables.mOverride = true;

			if (dr.mDrawableStart != start && dr.mDrawableStart != null) {
				dr.mDrawableStart.setCallback(null);
			}
			dr.mDrawableStart = start;

			if (dr.mShowing[Drawables.TOP] != top && dr.mShowing[Drawables.TOP] != null) {
				dr.mShowing[Drawables.TOP].setCallback(null);
			}
			dr.mShowing[Drawables.TOP] = top;

			if (dr.mDrawableEnd != end && dr.mDrawableEnd != null) {
				dr.mDrawableEnd.setCallback(null);
			}
			dr.mDrawableEnd = end;

			if (dr.mShowing[Drawables.BOTTOM] != bottom && dr.mShowing[Drawables.BOTTOM] != null) {
				dr.mShowing[Drawables.BOTTOM].setCallback(null);
			}
			dr.mShowing[Drawables.BOTTOM] = bottom;

			final Rect compoundRect = dr.mCompoundRect;
			int[] state;

			state = getDrawableState();

			if (start != null) {
				start.setState(state);
				start.copyBounds(compoundRect);
				start.setCallback(this);
				dr.mDrawableSizeStart = compoundRect.width();
				dr.mDrawableHeightStart = compoundRect.height();
			} else {
				dr.mDrawableSizeStart = dr.mDrawableHeightStart = 0;
			}

			if (end != null) {
				end.setState(state);
				end.copyBounds(compoundRect);
				end.setCallback(this);
				dr.mDrawableSizeEnd = compoundRect.width();
				dr.mDrawableHeightEnd = compoundRect.height();
			} else {
				dr.mDrawableSizeEnd = dr.mDrawableHeightEnd = 0;
			}

			if (top != null) {
				top.setState(state);
				top.copyBounds(compoundRect);
				top.setCallback(this);
				dr.mDrawableSizeTop = compoundRect.height();
				dr.mDrawableWidthTop = compoundRect.width();
			} else {
				dr.mDrawableSizeTop = dr.mDrawableWidthTop = 0;
			}

			if (bottom != null) {
				bottom.setState(state);
				bottom.copyBounds(compoundRect);
				bottom.setCallback(this);
				dr.mDrawableSizeBottom = compoundRect.height();
				dr.mDrawableWidthBottom = compoundRect.width();
			} else {
				dr.mDrawableSizeBottom = dr.mDrawableWidthBottom = 0;
			}
		}

		resetResolvedDrawables();
		resolveDrawables();
		updateNativeCompoundDrawables();
		invalidate();
		requestLayout();
	}

	public void setCompoundDrawablesRelativeWithIntrinsicBounds(int start,
			int top, int end, int bottom) {
		final Context context = getContext();
		setCompoundDrawablesRelativeWithIntrinsicBounds(
				start != 0 ? context.getDrawable(start) : null,
				top != 0 ? context.getDrawable(top) : null,
				end != 0 ? context.getDrawable(end) : null,
				bottom != 0 ? context.getDrawable(bottom) : null);
	}

	public void setCompoundDrawablesRelativeWithIntrinsicBounds(Drawable start,
			Drawable top, Drawable end, Drawable bottom) {

		if (start != null) {
			start.setBounds(0, 0, start.getIntrinsicWidth(), start.getIntrinsicHeight());
		}
		if (end != null) {
			end.setBounds(0, 0, end.getIntrinsicWidth(), end.getIntrinsicHeight());
		}
		if (top != null) {
			top.setBounds(0, 0, top.getIntrinsicWidth(), top.getIntrinsicHeight());
		}
		if (bottom != null) {
			bottom.setBounds(0, 0, bottom.getIntrinsicWidth(), bottom.getIntrinsicHeight());
		}
		setCompoundDrawablesRelative(start, top, end, bottom);
	}

	public Drawable[] getCompoundDrawables() {
		final Drawables dr = mDrawables;
		if (dr != null) {
			return dr.mShowing.clone();
		} else {
			return new Drawable[] { null, null, null, null };
		}
	}

	public Drawable[] getCompoundDrawablesRelative() {
		final Drawables dr = mDrawables;
		if (dr != null) {
			return new Drawable[] {
				dr.mDrawableStart, dr.mShowing[Drawables.TOP],
				dr.mDrawableEnd, dr.mShowing[Drawables.BOTTOM]
			};
		} else {
			return new Drawable[] { null, null, null, null };
		}
	}

	public void setCompoundDrawablePadding(int pad) {
		Drawables dr = mDrawables;
		if (pad == 0) {
			if (dr != null) {
				dr.mDrawablePadding = pad;
			}
		} else {
			if (dr == null) {
				mDrawables = dr = new Drawables(getContext());
			}
			dr.mDrawablePadding = pad;
		}

		native_setCompoundDrawablePadding(widget, pad);
		invalidate();
		requestLayout();
	}

	public int getCompoundDrawablePadding() {
		final Drawables dr = mDrawables;
		return dr != null ? dr.mDrawablePadding : 0;
	}

	public void setCompoundDrawableTintList(ColorStateList tint) {
		if (mDrawables == null) {
			mDrawables = new Drawables(getContext());
		}
		mDrawables.mTintList = tint;
		mDrawables.mHasTint = true;

		applyCompoundDrawableTint();
	}

	public ColorStateList getCompoundDrawableTintList() {
		return mDrawables != null ? mDrawables.mTintList : null;
	}

	public void setCompoundDrawableTintMode(PorterDuff.Mode tintMode) {
		setCompoundDrawableTintBlendMode(tintMode != null
				? BlendMode.fromValue(tintMode.nativeInt) : null);
	}

	public void setCompoundDrawableTintBlendMode(BlendMode blendMode) {
		if (mDrawables == null) {
			mDrawables = new Drawables(getContext());
		}
		mDrawables.mBlendMode = blendMode;
		mDrawables.mHasTintMode = true;

		applyCompoundDrawableTint();
	}

	public PorterDuff.Mode getCompoundDrawableTintMode() {
		BlendMode mode = getCompoundDrawableTintBlendMode();
		return mode != null ? BlendMode.blendModeToPorterDuffMode(mode) : null;
	}

	public BlendMode getCompoundDrawableTintBlendMode() {
		return mDrawables != null ? mDrawables.mBlendMode : null;
	}

	private void applyCompoundDrawableTint() {
		if (mDrawables == null) {
			return;
		}

		if (mDrawables.mHasTint || mDrawables.mHasTintMode) {
			final ColorStateList tintList = mDrawables.mTintList;
			final BlendMode blendMode = mDrawables.mBlendMode;
			final boolean hasTint = mDrawables.mHasTint;
			final boolean hasTintMode = mDrawables.mHasTintMode;
			final int[] state = getDrawableState();

			for (Drawable dr : mDrawables.mShowing) {
				if (dr == null) {
					continue;
				}

				if (dr == mDrawables.mDrawableError) {
					// From a developer's perspective, the error drawable isn't
					// a compound drawable. Don't apply the generic compound
					// drawable tint to it.
					continue;
				}

				dr.mutate();

				if (hasTint) {
					dr.setTintList(tintList);
				}

				if (hasTintMode) {
					dr.setTintBlendMode(blendMode);
				}

				// The drawable (or one of its children) may not have been
				// stateful before applying the tint, so let's try again.
				if (dr.isStateful()) {
					dr.setState(state);
				}
			}
		}
	}

	private void setRelativeDrawablesIfNeeded(Drawable start, Drawable end) {
		boolean hasRelativeDrawables = (start != null) || (end != null);
		if (hasRelativeDrawables) {
			Drawables dr = mDrawables;
			if (dr == null) {
				mDrawables = dr = new Drawables(getContext());
			}
			mDrawables.mOverride = true;
			final Rect compoundRect = dr.mCompoundRect;
			int[] state = getDrawableState();
			if (start != null) {
				start.setBounds(0, 0, start.getIntrinsicWidth(), start.getIntrinsicHeight());
				start.setState(state);
				start.copyBounds(compoundRect);
				start.setCallback(this);

				dr.mDrawableStart = start;
				dr.mDrawableSizeStart = compoundRect.width();
				dr.mDrawableHeightStart = compoundRect.height();
			} else {
				dr.mDrawableSizeStart = dr.mDrawableHeightStart = 0;
			}
			if (end != null) {
				end.setBounds(0, 0, end.getIntrinsicWidth(), end.getIntrinsicHeight());
				end.setState(state);
				end.copyBounds(compoundRect);
				end.setCallback(this);

				dr.mDrawableEnd = end;
				dr.mDrawableSizeEnd = compoundRect.width();
				dr.mDrawableHeightEnd = compoundRect.height();
			} else {
				dr.mDrawableSizeEnd = dr.mDrawableHeightEnd = 0;
			}
			resetResolvedDrawables();
			resolveDrawables();
			applyCompoundDrawableTint();
			updateNativeCompoundDrawables();
		}
	}

	public int getCompoundPaddingTop() {
		final Drawables dr = mDrawables;
		if (dr == null || dr.mShowing[Drawables.TOP] == null) {
			return getPaddingTop();
		} else {
			return getPaddingTop() + dr.mDrawablePadding + dr.mDrawableSizeTop;
		}
	}

	public int getCompoundPaddingBottom() {
		final Drawables dr = mDrawables;
		if (dr == null || dr.mShowing[Drawables.BOTTOM] == null) {
			return getPaddingBottom();
		} else {
			return getPaddingBottom() + dr.mDrawablePadding + dr.mDrawableSizeBottom;
		}
	}

	public int getCompoundPaddingLeft() {
		final Drawables dr = mDrawables;
		if (dr == null || dr.mShowing[Drawables.LEFT] == null) {
			return getPaddingLeft();
		} else {
			return getPaddingLeft() + dr.mDrawablePadding + dr.mDrawableSizeLeft;
		}
	}

	public int getCompoundPaddingRight() {
		final Drawables dr = mDrawables;
		if (dr == null || dr.mShowing[Drawables.RIGHT] == null) {
			return getPaddingRight();
		} else {
			return getPaddingRight() + dr.mDrawablePadding + dr.mDrawableSizeRight;
		}
	}

	public int getTotalPaddingLeft() {
		return getCompoundPaddingLeft();
	}

	public int getTotalPaddingRight() {
		return getCompoundPaddingRight();
	}

	@Override
	protected boolean verifyDrawable(Drawable who) {
		final boolean verified = super.verifyDrawable(who);
		if (!verified && mDrawables != null) {
			for (Drawable dr : mDrawables.mShowing) {
				if (who == dr) {
					return true;
				}
			}
		}
		return verified;
	}

	@Override
	public void jumpDrawablesToCurrentState() {
		super.jumpDrawablesToCurrentState();
		if (mDrawables != null) {
			for (Drawable dr : mDrawables.mShowing) {
				if (dr != null) {
					dr.jumpToCurrentState();
				}
			}
		}
	}

	@Override
	public void onResolveDrawables(int layoutDirection) {
		// No need to resolve twice
		if (mLastLayoutDirection == layoutDirection) {
			return;
		}
		mLastLayoutDirection = layoutDirection;

		// Resolve drawables
		if (mDrawables != null) {
			if (mDrawables.resolveWithLayoutDirection(layoutDirection)) {
				prepareDrawableForDisplay(mDrawables.mShowing[Drawables.LEFT]);
				prepareDrawableForDisplay(mDrawables.mShowing[Drawables.RIGHT]);
				applyCompoundDrawableTint();
				updateNativeCompoundDrawables();
			}
		}
	}

	private void prepareDrawableForDisplay(Drawable dr) {
		if (dr == null) {
			return;
		}

		dr.setLayoutDirection(getLayoutDirection());

		if (dr.isStateful()) {
			dr.setState(getDrawableState());
			dr.jumpToCurrentState();
		}
	}

	@Override
	protected void resetResolvedDrawables() {
		super.resetResolvedDrawables();
		mLastLayoutDirection = -1;
	}

	public void setMinLines(int minLines) {
		mMinimum = minLines;
		mMinMode = LINES;

		requestLayout();
		invalidate();
	}

	public void setMaxLines(int maxLines) {
		mMaximum = maxLines;
		mMaxMode = LINES;

		updateLines();
		requestLayout();
		invalidate();
	}

	public void setLines(int lines) {
		mMaximum = mMinimum = lines;
		mMaxMode = mMinMode = LINES;

		updateLines();
		requestLayout();
		invalidate();
	}

	public int getMaxLines() {
		return mMaxMode == LINES ? mMaximum : -1;
	}

	public int getMinLines() {
		return mMinMode == LINES ? mMinimum : -1;
	}

	public void setHorizontallyScrolling(boolean whether) {
		if (mHorizontallyScrolling != whether) {
			mHorizontallyScrolling = whether;

			requestLayout();
			invalidate();
		}
	}

	public boolean getHorizontallyScrolling() {
		return mHorizontallyScrolling;
	}

	public void setEllipsize(TextUtils.TruncateAt where) {
		// TruncateAt is an enum. != comparison is ok between these singleton objects.
		if (mEllipsize != where) {
			mEllipsize = where;

			updateLines();
			requestLayout();
			invalidate();
		}
	}

	public TextUtils.TruncateAt getEllipsize() {
		return mEllipsize;
	}

	public void setSingleLine(boolean singleLine) {
		// Could be used, but may break backward compatibility.
		// if (mSingleLine == singleLine) return;
		setInputTypeSingleLine(singleLine);
		applySingleLine(singleLine, true, true, true);
	}

	private void applySingleLine(boolean singleLine, boolean applyTransformation,
			boolean changeMaxLines, boolean changeMaxLength) {
		mSingleLine = singleLine;

		if (singleLine) {
			setLines(1);
			setHorizontallyScrolling(true);
			if (applyTransformation) {
				setTransformationMethod(SingleLineTransformationMethod.getInstance());
			}

			if (!changeMaxLength) return;

			// Single line length filter is only applicable editable text.
			if (mBufferType != BufferType.EDITABLE) return;

			final InputFilter[] prevFilters = getFilters();
			for (InputFilter filter: getFilters()) {
				// We don't add LengthFilter if already there.
				if (filter instanceof InputFilter.LengthFilter) return;
			}

			if (mSingleLineLengthFilter == null) {
				mSingleLineLengthFilter = new InputFilter.LengthFilter(
					MAX_LENGTH_FOR_SINGLE_LINE_EDIT_TEXT);
			}

			final InputFilter[] newFilters = new InputFilter[prevFilters.length + 1];
			System.arraycopy(prevFilters, 0, newFilters, 0, prevFilters.length);
			newFilters[prevFilters.length] = mSingleLineLengthFilter;

			setFilters(newFilters);

			// Since filter doesn't apply to existing text, trigger filter by setting text.
			setText(getText());
		} else {
			if (changeMaxLines) {
				setMaxLines(Integer.MAX_VALUE);
			}
			setHorizontallyScrolling(false);
			if (applyTransformation) {
				setTransformationMethod(null);
			}

			if (!changeMaxLength) return;

			// Single line length filter is only applicable editable text.
			if (mBufferType != BufferType.EDITABLE) return;

			final InputFilter[] prevFilters = getFilters();
			if (prevFilters.length == 0) return;

			// Short Circuit: if mSingleLineLengthFilter is not allocated, nobody sets automated
			// single line char limit filter.
			if (mSingleLineLengthFilter == null) return;

			// If we need to remove mSingleLineLengthFilter, we need to allocate another array.
			// Since filter list is expected to be small and want to avoid unnecessary array
			// allocation, check if there is mSingleLengthFilter first.
			int targetIndex = -1;
			for (int i = 0; i < prevFilters.length; ++i) {
				if (prevFilters[i] == mSingleLineLengthFilter) {
					targetIndex = i;
					break;
				}
			}
			if (targetIndex == -1) return;  // not found. Do nothing.

			if (prevFilters.length == 1) {
				setFilters(NO_FILTERS);
				return;
			}

			// Create new array which doesn't include mSingleLengthFilter.
			final InputFilter[] newFilters = new InputFilter[prevFilters.length - 1];
			System.arraycopy(prevFilters, 0, newFilters, 0, targetIndex);
			System.arraycopy(
					prevFilters,
					targetIndex + 1,
					newFilters,
					targetIndex,
					prevFilters.length - targetIndex - 1);
			setFilters(newFilters);
			mSingleLineLengthFilter = null;
		}
	}

	private void setInputTypeSingleLine(boolean singleLine) {
		// no Editor: the input type of the GtkText is not kept by the TextView
	}

	public final TransformationMethod getTransformationMethod() {
		return mTransformation;
	}

	public final void setTransformationMethod(TransformationMethod method) {
		setTransformationMethodInternal(method);
	}

	void setTransformationMethodInternal(TransformationMethod method) {
		if (method == mTransformation) {
			// Avoid the setText() below if the transformation is
			// the same.
			return;
		}

		mTransformation = method;

		// (AOSP also tells a TransformationMethod2 whether the length may change and keeps the method as a
		// span of a Spannable text: neither is needed here, the text is transformed on every setText())
		setText(getText()); // not the field: Button keeps its text in the native widget
	}

	/* as AOSP; Linkify is a stub here, so the mask is kept (getAutoLinkMask()) but no links are added to the text */
	private int mAutoLinkMask;
	private boolean mLinksClickable = true;

	public final void setAutoLinkMask(int mask) {
		mAutoLinkMask = mask;
	}

	public final void setLinksClickable(boolean whether) {
		mLinksClickable = whether;
	}

	public final boolean getLinksClickable() {
		return mLinksClickable;
	}

	public final int getAutoLinkMask() {
		return mAutoLinkMask;
	}

	/* as AOSP: the hint is kept for getHint(); a GtkLabel has no place to show it while the text is empty
	 * (EditText overrides this with the placeholder of its GtkText) */
	private CharSequence mHint;

	public void setHint(CharSequence hint) {
		mHint = TextUtils.stringOrSpannedString(hint);
	}

	public CharSequence getHint() {
		return mHint;
	}
}
