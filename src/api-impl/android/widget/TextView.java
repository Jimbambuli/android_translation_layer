package android.widget;

import android.atl.annotation.Export;
import android.annotation.UnsupportedAppUsage;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.BlendMode;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.Canvas;
import android.graphics.Paint;
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
import android.text.StaticLayout;
import android.text.TextDirectionHeuristic;
import android.text.TextDirectionHeuristics;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.KeyListener;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.text.method.PasswordTransformationMethod;
import android.text.method.SingleLineTransformationMethod;
import android.text.method.TransformationMethod;
import android.text.style.AlignmentSpan;
import android.text.style.CharacterStyle;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.BackgroundColorSpan;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.LeadingMarginSpan;
import android.text.style.LineBackgroundSpan;
import android.text.style.LineHeightSpan;
import android.text.style.MetricAffectingSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.ReplacementSpan;
import android.text.style.SubscriptSpan;
import android.text.style.SuperscriptSpan;
import android.text.style.TextAppearanceSpan;
import android.text.style.TypefaceSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.URLSpan;
import android.text.style.UnderlineSpan;
import android.text.util.Linkify;
import android.util.SparseIntArray;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.PointerIcon;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.autofill.AutofillValue;
import android.view.inputmethod.EditorInfo;
import android.view.textclassifier.TextClassificationManager;
import android.view.textclassifier.TextClassifier;
import android.text.Selection;
import android.util.Log;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.TreeSet;

public class TextView extends View implements android.view.ViewTreeObserver.OnPreDrawListener {
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
	/* AOSP: the height limits in lines or pixels (lines via GTK in updateLines(), pixels in onMeasure()) */
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
		this(context, attrs, com.android.internal.R.attr.textViewStyle);
	}

	public TextView(Context context) {
		this(context, null);
	}

	public TextView(Context context, AttributeSet attrs, int defStyleAttr) {
		this(context, attrs, defStyleAttr, 0);
	}

	public TextView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);

		TypedArray a = context.obtainStyledAttributes(attrs, com.android.internal.R.styleable.TextView, defStyleAttr, defStyleRes);
		try {
			/* AOSP's attribute loop reads these before the text is set at its end */
			mAutoLinkMask = a.getInt(com.android.internal.R.styleable.TextView_autoLink, 0);
			mLinksClickable = a.getBoolean(com.android.internal.R.styleable.TextView_linksClickable, true);
			if (a.hasValue(com.android.internal.R.styleable.TextView_text)) {
				setText(a.getText(com.android.internal.R.styleable.TextView_text));
			}
			if (a.hasValue(com.android.internal.R.styleable.TextView_hint)) {
				setHint(a.getText(com.android.internal.R.styleable.TextView_hint));
			}

			/* as AOSP: the text appearance, then the appearance attributes of the view itself (fontFamily,
			 * typeface, textFontWeight, ... included) are read into one set, which is applied once */
			final TextAppearanceAttributes attributes = new TextAppearanceAttributes();
			int ap = a.getResourceId(com.android.internal.R.styleable.TextView_textAppearance, -1);
			if (ap != -1) {
				final TypedArray appearance = context.obtainStyledAttributes(ap, com.android.internal.R.styleable.TextAppearance);
				readTextAppearance(context, appearance, attributes, false /* styleArray */);
				appearance.recycle();
			}
			readTextAppearance(context, a, attributes, true /* styleArray */);
			applyTextAppearance(attributes);

			include_padding = a.getBoolean(com.android.internal.R.styleable.TextView_includeFontPadding, true);

			if (a.hasValue(com.android.internal.R.styleable.TextView_gravity))
				setGravity(a.getInt(com.android.internal.R.styleable.TextView_gravity, Gravity.TOP | Gravity.START));

			int ellipsize = a.getInt(com.android.internal.R.styleable.TextView_ellipsize, ELLIPSIZE_NOT_SET);
			boolean singleLine = a.getBoolean(com.android.internal.R.styleable.TextView_singleLine, false);
			/* AOSP: an input type makes the Editor (here mInputType) and overrides the deprecated singleLine; without
			 * one an editable view (EditText) gets TYPE_CLASS_TEXT. The IME options go with the Editor's
			 * InputContentType (getImeOptions(), the action of Enter). ATL leaves out the key listeners of
			 * inputMethod, digits, phoneNumber, numeric, autoText and capitalize. */
			int inputType = a.getInt(com.android.internal.R.styleable.TextView_inputType, EditorInfo.TYPE_NULL);
			if (inputType != EditorInfo.TYPE_NULL) {
				setRawInputType(inputType);
				singleLine = !isMultilineInputType(inputType);
			} else if (a.getBoolean(com.android.internal.R.styleable.TextView_editable, getDefaultEditable())) {
				setRawInputType(EditorInfo.TYPE_CLASS_TEXT);
			}
			setImeOptions(a.getInt(com.android.internal.R.styleable.TextView_imeOptions, mImeOptions));
			if (a.hasValue(com.android.internal.R.styleable.TextView_imeActionLabel) || a.hasValue(com.android.internal.R.styleable.TextView_imeActionId))
				setImeActionLabel(a.getText(com.android.internal.R.styleable.TextView_imeActionLabel),
				                  a.getInt(com.android.internal.R.styleable.TextView_imeActionId, mImeActionId));
			/* in the order of the AOSP attribute loop (styleable index): maxHeight, minHeight, maxLines, lines,
			 * height, minLines */
			if (a.hasValue(com.android.internal.R.styleable.TextView_maxHeight))
				setMaxHeight(a.getDimensionPixelSize(com.android.internal.R.styleable.TextView_maxHeight, -1));
			if (a.hasValue(com.android.internal.R.styleable.TextView_minHeight))
				setMinHeight(a.getDimensionPixelSize(com.android.internal.R.styleable.TextView_minHeight, -1));
			if (a.hasValue(com.android.internal.R.styleable.TextView_maxLines))
				setMaxLines(a.getInt(com.android.internal.R.styleable.TextView_maxLines, -1));
			if (a.hasValue(com.android.internal.R.styleable.TextView_lines))
				setLines(a.getInt(com.android.internal.R.styleable.TextView_lines, -1));
			if (a.hasValue(com.android.internal.R.styleable.TextView_height))
				setHeight(a.getDimensionPixelSize(com.android.internal.R.styleable.TextView_height, -1));
			if (a.hasValue(com.android.internal.R.styleable.TextView_minLines))
				setMinLines(a.getInt(com.android.internal.R.styleable.TextView_minLines, -1));
			if (a.hasValue(com.android.internal.R.styleable.TextView_lineSpacingExtra) || a.hasValue(com.android.internal.R.styleable.TextView_lineSpacingMultiplier))
				setLineSpacing(a.getDimensionPixelSize(com.android.internal.R.styleable.TextView_lineSpacingExtra, 0),
				               a.getFloat(com.android.internal.R.styleable.TextView_lineSpacingMultiplier, 1.0f));
			if (a.hasValue(com.android.internal.R.styleable.TextView_maxEms))
				setMaxEms(a.getInt(com.android.internal.R.styleable.TextView_maxEms, -1));
			if (a.hasValue(com.android.internal.R.styleable.TextView_maxWidth))
				setMaxWidth(a.getDimensionPixelSize(com.android.internal.R.styleable.TextView_maxWidth, -1));
			if (a.hasValue(com.android.internal.R.styleable.TextView_ems))
				setEms(a.getInt(com.android.internal.R.styleable.TextView_ems, -1));
			if (a.hasValue(com.android.internal.R.styleable.TextView_width))
				setWidth(a.getDimensionPixelSize(com.android.internal.R.styleable.TextView_width, -1));
			if (a.hasValue(com.android.internal.R.styleable.TextView_minEms))
				setMinEms(a.getInt(com.android.internal.R.styleable.TextView_minEms, -1));
			if (a.hasValue(com.android.internal.R.styleable.TextView_minWidth))
				setMinWidth(a.getDimensionPixelSize(com.android.internal.R.styleable.TextView_minWidth, -1));
			mCursorDrawableRes = a.getResourceId(com.android.internal.R.styleable.TextView_textCursorDrawable, 0);
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
			if (a.getBoolean(com.android.internal.R.styleable.TextView_password, false) || isPasswordInputType(inputType))
				setTransformationMethod(PasswordTransformationMethod.getInstance());

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

			// AOSP attribute loop: the selection handles, loaded when first asked for
			mTextSelectHandleLeftRes = a.getResourceId(com.android.internal.R.styleable.TextView_textSelectHandleLeft, 0);
			mTextSelectHandleRightRes = a.getResourceId(com.android.internal.R.styleable.TextView_textSelectHandleRight, 0);
			mTextSelectHandleRes = a.getResourceId(com.android.internal.R.styleable.TextView_textSelectHandle, 0);
			// AOSP attribute loop: TextView_textIsSelectable -> setTextIsSelectable()
			if (a.getBoolean(com.android.internal.R.styleable.TextView_textIsSelectable, false))
				setTextIsSelectable(true);
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
		final CharSequence oldText = this.text;
		final int oldLength = oldText == null ? 0 : oldText.length();
		final boolean notify = !textChangesComeFromNative();
		final boolean needEditableForNotification = notify && mWatchers != null && mWatchers.size() != 0;
		if (notify)
			sendBeforeTextChanged(oldText, 0, oldLength, text.length());
		/* as AOSP's setText(): the links of the autoLink mask, LinkMovementMethod for them unless the text can be
		 * selected (ATL's selectable text has no movement method, the label selects) */
		if (mAutoLinkMask != 0) {
			Spannable s2 = (type == BufferType.EDITABLE || text instanceof Spannable) ? (Spannable)text : mSpannableFactory.newSpannable(text);
			if (Linkify.addLinks(s2, mAutoLinkMask)) {
				text = s2;
				type = (type == BufferType.EDITABLE) ? BufferType.EDITABLE : BufferType.SPANNABLE;
				mBufferType = type;
				this.text = text;
				if (mLinksClickable && !(mTextIsSelectable && isEnabled()))
					setMovementMethod(LinkMovementMethod.getInstance());
			}
		}
		/* as AOSP's setText(): the buffer type (or a movement method, which needs spans) decides what getText()
		 * returns; the Editor and PrecomputedText parts of AOSP do not apply here */
		if (type == BufferType.EDITABLE || needEditableForNotification) {
			text = mEditableFactory.newEditable(text);
		} else if (type == BufferType.SPANNABLE || mMovement != null) {
			text = mSpannableFactory.newSpannable(text);
		} else {
			text = TextUtils.stringOrSpannedString(text);
		}
		this.text = text;
		/* as AOSP's setText(): the transformation decides what is shown (getText() stays the text that was set) */
		mTransformed = mTransformation == null ? this.text : mTransformation.getTransformation(this.text, this);
		updateNativeText();
		if (!isLayoutRequested())
			requestLayout();
		if (notify) {
			handleTextChanged(this.text, 0, oldLength, this.text.length());
			if (needEditableForNotification)
				sendAfterTextChanged((Editable)this.text);
		}
	}

	private void updateNativeText() {
		if (mParagraphSelection != null)
			mParagraphSelection.onTextChanged();
		/* (Button and EditText have widgets of their own, not the label) */
		if (mTransformed instanceof Spanned && !(this instanceof Button || this instanceof EditText) && hasParagraphSpans((Spanned)mTransformed)) {
			showParagraphLayout(true);
			return;
		}
		showParagraphLayout(false);
		if (mTransformed instanceof Spanned)
			setPangoMarkup((Spanned)mTransformed);
		else
			native_setText(mTransformed.toString());
	}

	/* --- ParagraphStyle spans ---
	 * AOSP's TextView lays out and draws its text with a StaticLayout. The GtkLabel of ATL's TextView knows no
	 * paragraph spans (leading margins and what they draw, line heights, line backgrounds, the alignment of a
	 * paragraph), so a text with such spans is laid out and drawn like AOSP with android.text.StaticLayout (which
	 * handles them, see Layout) on the canvas of the view, and the label stays empty. Touches on such a text go to
	 * the movement method like in AOSP (onTouchEvent()), so its links are clickable; it cannot be selected (AOSP's
	 * Editor does that, ATL's label does it for other texts). */
	private Layout mParagraphLayout;
	private ParagraphLayoutDrawable mParagraphOverlay;
	private boolean mParagraphText;

	private static boolean hasParagraphSpans(Spanned text) {
		return text.getSpans(0, text.length(), LeadingMarginSpan.class).length > 0 || text.getSpans(0, text.length(), LineHeightSpan.class).length > 0
		    || text.getSpans(0, text.length(), LineBackgroundSpan.class).length > 0 || text.getSpans(0, text.length(), AlignmentSpan.class).length > 0;
	}

	private void showParagraphLayout(boolean show) {
		mParagraphLayout = null;
		if (show == mParagraphText) {
			if (show)
				invalidate();
			return;
		}
		mParagraphText = show;
		if (show) {
			native_setText("");
			replacementSpans.clear();
			if (mReplacementOverlay != null)
				getOverlay().remove(mReplacementOverlay);
			linkSpans = NO_LINKS;
			if (mParagraphOverlay == null)
				mParagraphOverlay = new ParagraphLayoutDrawable();
			getOverlay().add(mParagraphOverlay);
			nativeSetOnTouchListener(widget); // the touches for onTouchEvent(), also inside a GTK scroll view
			invalidate();
		} else if (mParagraphOverlay != null) {
			getOverlay().remove(mParagraphOverlay);
		}
	}

	/* AOSP TextView.makeNewLayout() for the text at the width (without the paddings) */
	private Layout makeParagraphLayout(int width) {
		/* AOSP updateTextColors() keeps the link colour in the paint; the layout takes the spans' look when built */
		getPaint().linkColor = getLinkTextColors().getColorForState(getDrawableState(), getLinkTextColors().getDefaultColor());
		mParagraphLayout = StaticLayout.Builder.obtain(mTransformed, 0, mTransformed.length(), getPaint(), Math.max(0, width))
		    .setAlignment(getLayoutAlignment())
		    .setLineSpacing(mSpacingAdd, mSpacingMult)
		    .setIncludePad(include_padding)
		    .setBreakStrategy(getBreakStrategy())
		    .setJustificationMode(mJustificationMode)
		    .setMaxLines(mMaxMode == LINES ? mMaximum : Integer.MAX_VALUE)
		    .setEllipsize(mEllipsize)
		    .build();
		return mParagraphLayout;
	}

	/* AOSP TextView.onMeasure(): the width from the desired width of the text (unless exact), limited like there,
	 * the height from the layout at that width */
	private void measureParagraphs(int widthMeasureSpec, int heightMeasureSpec) {
		int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		int widthSize = MeasureSpec.getSize(widthMeasureSpec);
		int heightMode = MeasureSpec.getMode(heightMeasureSpec);
		int heightSize = MeasureSpec.getSize(heightMeasureSpec);
		int pad = getCompoundPaddingLeft() + getCompoundPaddingRight();
		int width;
		if (widthMode == MeasureSpec.EXACTLY) {
			width = widthSize;
		} else {
			width = (int)Math.ceil(Layout.getDesiredWidth(mTransformed, getPaint())) + pad;
			if (mMaxWidthMode == EMS)
				width = Math.min(width, mMaxWidth * getLineHeight());
			else
				width = Math.min(width, mMaxWidth);
			if (mMinWidthMode == EMS)
				width = Math.max(width, mMinWidth * getLineHeight());
			else
				width = Math.max(width, mMinWidth);
			width = Math.max(width, getSuggestedMinimumWidth());
			if (widthMode == MeasureSpec.AT_MOST)
				width = Math.min(widthSize, width);
		}
		Layout layout = makeParagraphLayout(width - pad);
		int height;
		if (heightMode == MeasureSpec.EXACTLY) {
			height = heightSize;
		} else {
			height = layout.getHeight() + getCompoundPaddingTop() + getCompoundPaddingBottom();
			height = Math.max(height, getSuggestedMinimumHeight());
			if (heightMode == MeasureSpec.AT_MOST)
				height = Math.min(height, heightSize);
		}
		setMeasuredDimension(width, height);
		applyHeightLimits(heightMeasureSpec);
	}

	/* AOSP TextView.getVerticalOffset(): the text moved down by the vertical gravity when it is lower than the view */
	private int paragraphVerticalOffset(Layout layout) {
		int gravity = this.gravity & Gravity.VERTICAL_GRAVITY_MASK;
		if (gravity == Gravity.TOP)
			return 0;
		int boxht = getHeight() - getExtendedPaddingTop() - getExtendedPaddingBottom();
		int textht = layout.getHeight();
		if (textht >= boxht)
			return 0;
		return gravity == Gravity.BOTTOM ? boxht - textht : (boxht - textht) >> 1;
	}

	private Layout paragraphLayout() {
		int width = getWidth() - getCompoundPaddingLeft() - getCompoundPaddingRight();
		return mParagraphLayout != null && mParagraphLayout.getWidth() == Math.max(0, width) ? mParagraphLayout : makeParagraphLayout(width);
	}

	/* AOSP TextView.onDraw(): the paint gets the current colours, the layout is drawn inside the paddings */
	private final Paint mHighlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

	private final class ParagraphLayoutDrawable extends Drawable {
		@Override
		public void draw(Canvas canvas) {
			Layout layout = paragraphLayout();
			TextPaint textPaint = getPaint();
			textPaint.setColor(getCurrentTextColor());
			textPaint.linkColor = getLinkTextColors().getColorForState(getDrawableState(), getLinkTextColors().getDefaultColor());
			textPaint.drawableState = getDrawableState();
			canvas.save();
			canvas.translate(getCompoundPaddingLeft(), getExtendedPaddingTop() + paragraphVerticalOffset(layout));
			ParagraphTextSelection selection = paragraphSelection();
			if (selection != null && mMovement != null)
				selection.drawHighlight(canvas, layout, mHighlightPaint);
			layout.draw(canvas);
			canvas.restore();
		}
	}

	private static final ClickableSpan[] NO_LINKS = new ClickableSpan[0];
	private ClickableSpan[] linkSpans = NO_LINKS;
	private ColorStateList linkTextColor;

	/* the character spans as Pango markup; a ClickableSpan becomes a link whose URI is its index in linkSpans */
	/* the ReplacementSpans in the markup: UTF-16 index of the placeholder in the markup's text and the
	 * size and vertical offset (relative to the baseline) the placeholder takes in the line */
	private final ArrayList<int[]> replacementPlaces = new ArrayList<>();

	private void setPangoMarkup(Spanned text) {
		String markup = toPangoMarkup(text);
		int[] places = new int[replacementPlaces.size() * 4];
		for (int i = 0; i < replacementPlaces.size(); i++)
			System.arraycopy(replacementPlaces.get(i), 0, places, i * 4, 4);
		native_setMarkup(markup, places);
		if (!replacementSpans.isEmpty()) {
			if (mReplacementOverlay == null)
				mReplacementOverlay = new ReplacementSpansDrawable();
			getOverlay().add(mReplacementOverlay);
			mReplacementOverlay.invalidateSelf();
		} else if (mReplacementOverlay != null) {
			getOverlay().remove(mReplacementOverlay);
		}
	}

	/* the ReplacementSpans: the label leaves a gap of their size (PangoAttrShape) and the overlay of the view
	 * calls their draw() there, with the line Pango has put the placeholder on (AOSP TextLine.handleReplacement()) */
	private ReplacementSpansDrawable mReplacementOverlay;
	private final ArrayList<ReplacementSpan> replacementSpans = new ArrayList<>();
	private Spanned replacementText;

	private final ArrayList<int[]> replacementRuns = new ArrayList<>(); // start and end of the run of each placeholder

	private final class ReplacementSpansDrawable extends Drawable {
		@Override
		public void draw(Canvas canvas) {
			float[] place = new float[4];
			for (int i = 0; i < replacementSpans.size(); i++) {
				ReplacementSpan span = replacementSpans.get(i);
				int[] run = replacementRuns.get(i);
				if (!native_getPlaceholderPosition(widget, i, place))
					continue;
				span.draw(canvas, replacementText, run[0], run[1], place[0], Math.round(place[1]), Math.round(place[2]),
				          Math.round(place[3]), replacementPaint(replacementText, run[0], run[1], true));
			}
		}
	}

	/* AOSP: the paint of the text with the other MetricAffectingSpans of the replaced run applied, their measure state
	 * for getSize() (MeasuredParagraph.applyMetricsAffectingSpan()), their draw state for draw() (TextLine.handleRun());
	 * for draw() it has the colours TextView.onDraw() gives mTextPaint */
	private TextPaint replacementPaint(Spanned text, int start, int end, boolean draw) {
		TextPaint wp = new TextPaint();
		wp.set(getPaint());
		if (draw) {
			wp.setColor(getCurrentTextColor());
			wp.linkColor = getLinkTextColors().getColorForState(getDrawableState(), getLinkTextColors().getDefaultColor());
			wp.drawableState = getDrawableState();
		}
		for (MetricAffectingSpan span : text.getSpans(start, end, MetricAffectingSpan.class)) {
			if (span instanceof ReplacementSpan || text.getSpanStart(span) == text.getSpanEnd(span))
				continue;
			if (draw)
				span.updateDrawState(wp);
			else
				span.updateMeasureState(wp);
		}
		return wp;
	}

	/* x (left edge), top, baseline and bottom of the line of the placeholder, in the view; false when there is
	 * none (the text has no such place, or an ellipsis replaces it) */
	private native boolean native_getPlaceholderPosition(long widget, int index, float[] place);

	private String toPangoMarkup(Spanned text) {
		replacementPlaces.clear();
		replacementSpans.clear();
		replacementRuns.clear();
		replacementText = text;
		int plain = 0;
		CharacterStyle[] spans = text.getSpans(0, text.length(), CharacterStyle.class);
		linkSpans = text.getSpans(0, text.length(), ClickableSpan.class);
		/* AOSP MeasuredParagraph/TextLine: a paragraph is split at the transitions of its MetricAffectingSpans; the
		 * last ReplacementSpan of a run replaces it, getSize() and draw() are called per run */
		ReplacementSpan[] runSpan = new ReplacementSpan[text.length()];
		int[] runEnd = new int[text.length()];
		if (text.getSpans(0, text.length(), ReplacementSpan.class).length > 0) {
			for (int p = 0, pend; p < text.length(); p = pend) {
				pend = TextUtils.indexOf(text, '\n', p, text.length());
				pend = pend < 0 ? text.length() : pend + 1;
				for (int s = p, next; s < pend; s = next) {
					next = text.nextSpanTransition(s, pend, MetricAffectingSpan.class);
					for (MetricAffectingSpan span : text.getSpans(s, next, MetricAffectingSpan.class)) {
						if (span instanceof ReplacementSpan && text.getSpanStart(span) != text.getSpanEnd(span))
							runSpan[s] = (ReplacementSpan)span;
					}
					runEnd[s] = next;
				}
			}
		}
		TreeSet<Integer> bounds = new TreeSet<>();
		bounds.add(0);
		bounds.add(text.length());
		for (CharacterStyle span : spans) {
			bounds.add(Math.max(0, Math.min(text.getSpanStart(span), text.length())));
			bounds.add(Math.max(0, Math.min(text.getSpanEnd(span), text.length())));
		}
		int linkColor = getLinkTextColors().getColorForState(getDrawableState(), getLinkTextColors().getDefaultColor());
		StringBuilder markup = new StringBuilder();
		int replacedUntil = 0;
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
				} else if (span instanceof BackgroundColorSpan) {
					int color = ((BackgroundColorSpan)span).getBackgroundColor();
					tag = "span";
					attrs = String.format("background=\"#%06x\" bgalpha=\"%d\"", color & 0xffffff, Math.max(1, (color >>> 24) * 65535 / 255));
				} else if (span instanceof RelativeSizeSpan) {
					tag = "span";
					attrs = String.format(Locale.ROOT, "size=\"%d%%\"", Math.max(1, Math.round(((RelativeSizeSpan)span).getSizeChange() * 100)));
				} else if (span instanceof AbsoluteSizeSpan && getTextSize() > 0) {
					AbsoluteSizeSpan abs = (AbsoluteSizeSpan)span;
					float px = abs.getDip() ? abs.getSize() * getResources().getDisplayMetrics().density : abs.getSize();
					tag = "span";
					attrs = String.format(Locale.ROOT, "size=\"%d%%\"", Math.max(1, Math.round(px * 100 / getTextSize())));
				} else if (span instanceof SuperscriptSpan) {
					tag = "sup";
				} else if (span instanceof SubscriptSpan) {
					tag = "sub";
				} else if (span instanceof TextAppearanceSpan) {
					/* AOSP TextAppearanceSpan.updateDrawState(): colour, size, family and style of the appearance */
					TextAppearanceSpan appearance = (TextAppearanceSpan)span;
					StringBuilder sb = new StringBuilder();
					if (appearance.getTextColor() != null)
						sb.append(pangoColor(appearance.getTextColor().getColorForState(getDrawableState(), appearance.getTextColor().getDefaultColor()))).append(' ');
					if (appearance.getTextSize() > 0 && getTextSize() > 0)
						sb.append(String.format(Locale.ROOT, "size=\"%d%%\" ", Math.max(1, Math.round(appearance.getTextSize() * 100 / getTextSize()))));
					if (appearance.getFamily() != null)
						sb.append("font_family=\"").append(appearance.getFamily().replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;")).append("\" ");
					if ((appearance.getTextStyle() & Typeface.BOLD) != 0)
						sb.append("font_weight=\"bold\" ");
					if ((appearance.getTextStyle() & Typeface.ITALIC) != 0)
						sb.append("font_style=\"italic\" ");
					if (sb.length() > 0) {
						tag = "span";
						attrs = sb.toString().trim();
					}
				} else if (span instanceof TypefaceSpan && ((TypefaceSpan)span).getFamily() != null) {
					tag = "span";
					attrs = "font_family=\"" + ((TypefaceSpan)span).getFamily().replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;") + "\"";
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
				/* AOSP draws a ReplacementSpan instead of the text of a run; the characters of the run give way to one
				 * placeholder of the width and font metrics getSize() gives for the run (a line break stays) */
				if (i < replacedUntil) {
					if (text.charAt(i) == '\n') {
						markup.append('\n');
						plain++;
					}
					continue;
				}
				if (runSpan[i] != null) {
					TextPaint wp = replacementPaint(text, i, runEnd[i], false);
					Paint.FontMetricsInt fm = wp.getFontMetricsInt();
					int width = runSpan[i].getSize(wp, text, i, runEnd[i], fm);
					replacementPlaces.add(new int[] {plain, width, fm.descent - fm.ascent, fm.ascent});
					replacementSpans.add(runSpan[i]);
					replacementRuns.add(new int[] {i, runEnd[i]});
					markup.append('\uFFFC');
					plain++;
					replacedUntil = runEnd[i];
					continue;
				}
				char c = text.charAt(i);
				markup.append(c == '&' ? "&amp;" : c == '<' ? "&lt;" : c == '>' ? "&gt;" : String.valueOf(c));
				plain++;
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

	/* called when a link of the markup is activated; like LinkMovementMethod, only then do links react, and like
	 * AOSP's onTouchEvent() the autoLink links of a selectable text */
	private boolean onLinkActivated(String uri) {
		if (!(mMovement instanceof LinkMovementMethod) && !(mLinksClickable && mAutoLinkMask != 0 && mTextIsSelectable))
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

	private native final void native_setMarkup(String markup, int[] places);

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
		if (getPaint().getLetterSpacing() != 0)
			applyNativeLetterSpacing(); // in pixels, so it follows the size

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
	private native void native_setPaintFlags(int flags);

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

	/* AOSP updateTextColors(): the color for the current drawable state (0 if no state matches), which the
	 * label is given as well; the text that is drawn in Java is redrawn when it changed */
	private void updateTextColors() {
		final int color = colors.getColorForState(getDrawableState(), 0);
		final boolean changed = color != mCurTextColor;
		mCurTextColor = color;
		native_setTextColor(color);
		if (changed)
			invalidate();
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
	/* AOSP keeps the spacing for its layouts and getLineHeight(); the GtkLabel does not space its lines by it */
	private float mSpacingMult = 1.0f;
	private float mSpacingAdd = 0.0f;

	public void setLineSpacing(float add, float mult) {
		if (mSpacingAdd != add || mSpacingMult != mult) {
			mSpacingAdd = add;
			mSpacingMult = mult;
			requestLayout();
			invalidate();
		}
	}

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

	/* AOSP keeps the input type in Editor.mInputType, which only exists once an input type is set (mEditor != null). ATL has no
	 * Editor: the type is kept here, 0 (TYPE_NULL) stands for "no Editor". The key listener, IME hints and password handling
	 * of TextView.setInputType(type, direct) are not reproduced. */
	private int mInputType;

	/* AOSP TextView.setInputType(): a password type shows the text through the password transformation, and the
	 * type decides whether the view has a single line (without AOSP's monospace typeface for passwords) */
	public void setInputType(int type) {
		final boolean wasPassword = isPasswordInputType(getInputType());
		final boolean wasVisiblePassword = isVisiblePasswordInputType(getInputType());
		setRawInputType(type);
		final boolean isPassword = isPasswordInputType(type);
		boolean forceUpdate = false;
		if (isPassword) {
			setTransformationMethod(PasswordTransformationMethod.getInstance());
		} else if (isVisiblePasswordInputType(type) || wasPassword || wasVisiblePassword) {
			if (mTransformation == PasswordTransformationMethod.getInstance())
				forceUpdate = true;
		}
		boolean singleLine = !isMultilineInputType(type);
		if (mSingleLine != singleLine || forceUpdate)
			applySingleLine(singleLine, !isPassword, true, true);
	}

	/* AOSP TextView.isPasswordInputType(), isVisiblePasswordInputType() */
	static boolean isPasswordInputType(int inputType) {
		final int variation = inputType & (EditorInfo.TYPE_MASK_CLASS | EditorInfo.TYPE_MASK_VARIATION);
		return variation == (EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_VARIATION_PASSWORD)
		    || variation == (EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_VARIATION_WEB_PASSWORD)
		    || variation == (EditorInfo.TYPE_CLASS_NUMBER | EditorInfo.TYPE_NUMBER_VARIATION_PASSWORD);
	}

	private static boolean isVisiblePasswordInputType(int inputType) {
		final int variation = inputType & (EditorInfo.TYPE_MASK_CLASS | EditorInfo.TYPE_MASK_VARIATION);
		return variation == (EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
	}
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

	public final ColorStateList getTextColors() { return colors; }
	public static ColorStateList getTextColors(Context context, TypedArray attrs) { return new ColorStateList(new int[][] {new int[0]}, new int[1]); }

	public TextPaint getPaint() {
		return paint;
	}


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
			setPangoMarkup((Spanned)mTransformed);
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

	private static final SparseIntArray sAppearanceValues = new SparseIntArray();
	static {
		sAppearanceValues.put(com.android.internal.R.styleable.TextView_textColorHighlight,
				com.android.internal.R.styleable.TextAppearance_textColorHighlight);
		sAppearanceValues.put(com.android.internal.R.styleable.TextView_searchResultHighlightColor,
				com.android.internal.R.styleable.TextAppearance_searchResultHighlightColor);
		sAppearanceValues.put(
				com.android.internal.R.styleable.TextView_focusedSearchResultHighlightColor,
				com.android.internal.R.styleable.TextAppearance_focusedSearchResultHighlightColor);
		sAppearanceValues.put(com.android.internal.R.styleable.TextView_textColor,
				com.android.internal.R.styleable.TextAppearance_textColor);
		sAppearanceValues.put(com.android.internal.R.styleable.TextView_textColorHint,
				com.android.internal.R.styleable.TextAppearance_textColorHint);
		sAppearanceValues.put(com.android.internal.R.styleable.TextView_textColorLink,
				com.android.internal.R.styleable.TextAppearance_textColorLink);
		sAppearanceValues.put(com.android.internal.R.styleable.TextView_textSize,
				com.android.internal.R.styleable.TextAppearance_textSize);
		sAppearanceValues.put(com.android.internal.R.styleable.TextView_textLocale,
				com.android.internal.R.styleable.TextAppearance_textLocale);
		sAppearanceValues.put(com.android.internal.R.styleable.TextView_typeface,
				com.android.internal.R.styleable.TextAppearance_typeface);
		sAppearanceValues.put(com.android.internal.R.styleable.TextView_fontFamily,
				com.android.internal.R.styleable.TextAppearance_fontFamily);
		sAppearanceValues.put(com.android.internal.R.styleable.TextView_textStyle,
				com.android.internal.R.styleable.TextAppearance_textStyle);
		sAppearanceValues.put(com.android.internal.R.styleable.TextView_textFontWeight,
				com.android.internal.R.styleable.TextAppearance_textFontWeight);
		sAppearanceValues.put(com.android.internal.R.styleable.TextView_textAllCaps,
				com.android.internal.R.styleable.TextAppearance_textAllCaps);
		sAppearanceValues.put(com.android.internal.R.styleable.TextView_shadowColor,
				com.android.internal.R.styleable.TextAppearance_shadowColor);
		sAppearanceValues.put(com.android.internal.R.styleable.TextView_shadowDx,
				com.android.internal.R.styleable.TextAppearance_shadowDx);
		sAppearanceValues.put(com.android.internal.R.styleable.TextView_shadowDy,
				com.android.internal.R.styleable.TextAppearance_shadowDy);
		sAppearanceValues.put(com.android.internal.R.styleable.TextView_shadowRadius,
				com.android.internal.R.styleable.TextAppearance_shadowRadius);
		sAppearanceValues.put(com.android.internal.R.styleable.TextView_elegantTextHeight,
				com.android.internal.R.styleable.TextAppearance_elegantTextHeight);
		sAppearanceValues.put(com.android.internal.R.styleable.TextView_fallbackLineSpacing,
				com.android.internal.R.styleable.TextAppearance_fallbackLineSpacing);
		sAppearanceValues.put(com.android.internal.R.styleable.TextView_letterSpacing,
				com.android.internal.R.styleable.TextAppearance_letterSpacing);
		sAppearanceValues.put(com.android.internal.R.styleable.TextView_fontFeatureSettings,
				com.android.internal.R.styleable.TextAppearance_fontFeatureSettings);
		sAppearanceValues.put(com.android.internal.R.styleable.TextView_fontVariationSettings,
				com.android.internal.R.styleable.TextAppearance_fontVariationSettings);
		sAppearanceValues.put(com.android.internal.R.styleable.TextView_lineBreakStyle,
				com.android.internal.R.styleable.TextAppearance_lineBreakStyle);
		sAppearanceValues.put(com.android.internal.R.styleable.TextView_lineBreakWordStyle,
				com.android.internal.R.styleable.TextAppearance_lineBreakWordStyle);
	}

	/* AOSP readTextAppearance(): styleArray tells whether the TypedArray is a TextView style (its indexes are
	 * translated with sAppearanceValues) or a TextAppearance */
	private void readTextAppearance(Context context, TypedArray appearance,
			TextAppearanceAttributes attributes, boolean styleArray) {
		final int n = appearance.getIndexCount();
		for (int i = 0; i < n; i++) {
			final int attr = appearance.getIndex(i);
			int index = attr;
			// Translate style array index ids to TextAppearance ids.
			if (styleArray) {
				index = sAppearanceValues.get(attr, -1);
				if (index == -1) {
					// This value is not part of a Text Appearance and should be ignored.
					continue;
				}
			}
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
					try {
						attributes.mFontTypeface = appearance.getFont(attr);
					} catch (UnsupportedOperationException | Resources.NotFoundException e) {
						// Expected if it is not a font resource.
					}
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
		readTextAppearance(context, ta, attributes, false /* styleArray */);
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
	private native void native_setWrap(boolean wrap);

	// there is no input method connection to batch or configure
	public void beginBatchEdit() {}

	public void endBatchEdit() {}

	/* AOSP keeps it in Editor.mInputContentType and hands it to the IME in onCreateInputConnection(); ATL keeps it
	 * for the getter (the GTK input method gets no private options) */
	private String mPrivateImeOptions;

	public void setPrivateImeOptions(String type) {
		mPrivateImeOptions = type;
	}

	public String getPrivateImeOptions() {
		return mPrivateImeOptions;
	}

	/** AOSP onCheckIsTextEditor(): mEditor != null && mEditor.mInputType != TYPE_NULL; in ATL an input type
	 * other than 0 stands for the Editor (see mInputType) */
	@Override
	public boolean onCheckIsTextEditor() {
		return mInputType != 0;
	}

	public Typeface getTypeface() { return typeface; }

	public float getTextSize() {
		return getPaint().getTextSize();
	}

	/* GtkLabel mirrors the alignment in right-to-left layouts, so LEFT and RIGHT are
	 * treated like START and END */
	@Override
	public void setGravity(int gravity) {
		this.gravity = gravity;
		applyAlignment();
	}

	/* the horizontal alignment is the one of the resolved text alignment (the gravity for the
	 * default), the vertical one is the gravity's */
	private void applyAlignment() {
		float xalign = 0.f;
		switch (getLayoutAlignment()) {
			case ALIGN_CENTER:
				xalign = 0.5f;
				break;
			case ALIGN_OPPOSITE:
			case ALIGN_RIGHT:
				xalign = 1.f;
				break;
		}
		float yalign = 0.f;
		int vertical = gravity & Gravity.VERTICAL_GRAVITY_MASK;
		if (vertical == Gravity.CENTER_VERTICAL)
			yalign = 0.5f;
		else if (vertical == Gravity.BOTTOM)
			yalign = 1.f;
		native_setAlignment(xalign, yalign);
	}

	@Override
	public void onRtlPropertiesChanged(int layoutDirection) {
		super.onRtlPropertiesChanged(layoutDirection);
		applyAlignment();
	}
	private native void native_setAlignment(float xalign, float yalign);

	/* like AOSP: the baseline of the first line, below the top padding and moved by the vertical
	 * gravity (BaselineLayout, baseline-aligned LinearLayouts and others place the text by it) */
	@Override
	public int getBaseline() {
		if (mParagraphText) {
			Layout layout = paragraphLayout();
			return getExtendedPaddingTop() + paragraphVerticalOffset(layout) + layout.getLineBaseline(0);
		}
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

	protected native void native_setCompoundDrawables(long widget, Drawable left, Drawable top, Drawable right, Drawable bottom);

	public void setAllCaps(boolean allCaps) {
		mAllCaps = allCaps;
		String[] classesToRemove = {"ATL-text-uppercase"};
		native_removeClasses(widget, classesToRemove);

		if (allCaps) {
			native_addClass(widget, "ATL-text-uppercase");
		}
	}

	public void setSaveEnabled(boolean enabled) {}


	/* AOSP TextView.setEditableFactory()/setSpannableFactory(): the factories setText() makes its buffers with */
	private Editable.Factory mEditableFactory = Editable.Factory.getInstance();
	private Spannable.Factory mSpannableFactory = Spannable.Factory.getInstance();

	/* for EditText, which makes its buffer with it too */
	Editable.Factory getEditableFactory() {
		return mEditableFactory;
	}

	public void setEditableFactory(Editable.Factory factory) {
		mEditableFactory = factory;
		setText(text);
	}

	public void setSpannableFactory(Spannable.Factory factory) {
		mSpannableFactory = factory;
		setText(text);
	}

	public int getInputType() { return mInputType; }

	public InputFilter[] getFilters() { return filters; }

	public int getLineCount() {
		return mParagraphText ? paragraphLayout().getLineCount() : 1;
	}

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

	private boolean mTextIsSelectable;

	/** AOSP TextView.setTextIsSelectable(): focusable in touch mode, clickable and long-clickable as selectable,
	 * ArrowKeyMovementMethod and a spannable text; the selection of a paragraph text is made like AOSP's Editor
	 * (ParagraphTextSelection), that of other texts by the GtkLabel */
	public void setTextIsSelectable(boolean selectable) {
		if (mTextIsSelectable == selectable)
			return;
		mTextIsSelectable = selectable;
		setFocusableInTouchMode(selectable);
		setFocusable(FOCUSABLE_AUTO);
		setClickable(selectable);
		setLongClickable(selectable);
		native_setSelectable(widget, selectable);
		setMovementMethod(selectable ? android.text.method.ArrowKeyMovementMethod.getInstance() : null);
		setText(getText(), selectable ? BufferType.SPANNABLE : BufferType.NORMAL);
	}

	/* the parts of AOSP's Editor that select a paragraph text (or the text of an EditText), made when first needed */
	ParagraphTextSelection mParagraphSelection;

	private ParagraphTextSelection paragraphSelection() {
		if (mParagraphText && mTextIsSelectable && text instanceof Spannable) {
			if (mParagraphSelection == null)
				mParagraphSelection = new ParagraphTextSelection(this);
			return mParagraphSelection;
		}
		return null;
	}

	/* the Editor parts for an EditText: its GtkText edits, places the cursor and selects; the word selection of
	 * a long press and the floating toolbar of the selection are the Editor's */
	ParagraphTextSelection editTextSelection() {
		if (mParagraphSelection == null)
			mParagraphSelection = new ParagraphTextSelection(this);
		return mParagraphSelection;
	}
	private native void native_setSelectable(long widget, boolean selectable);

	/* AOSP setTextSelectHandle*()/getTextSelectHandle*(): kept for the getters; ATL draws no selection handles
	 * (GTK draws its own), so AOSP's mEditor.loadHandleDrawables() has nothing to update */
	private Drawable mTextSelectHandle;
	private Drawable mTextSelectHandleLeft;
	private Drawable mTextSelectHandleRight;

	public void setTextSelectHandle(Drawable textSelectHandle) {
		java.util.Objects.requireNonNull(textSelectHandle, "The text select handle should not be null.");
		mTextSelectHandle = textSelectHandle;
	}

	public void setTextSelectHandle(int textSelectHandle) {
		if (textSelectHandle == 0)
			throw new IllegalArgumentException("The text select handle should be a valid drawable resource id.");
		setTextSelectHandle(getContext().getDrawable(textSelectHandle));
	}

	/* AOSP: the drawable of the theme's attribute until one is set */
	private int mTextSelectHandleRes;
	private int mTextSelectHandleLeftRes;
	private int mTextSelectHandleRightRes;

	public Drawable getTextSelectHandle() {
		if (mTextSelectHandle == null && mTextSelectHandleRes != 0)
			mTextSelectHandle = getContext().getDrawable(mTextSelectHandleRes);
		return mTextSelectHandle;
	}

	public void setTextSelectHandleLeft(Drawable textSelectHandleLeft) {
		java.util.Objects.requireNonNull(textSelectHandleLeft, "The left text select handle should not be null.");
		mTextSelectHandleLeft = textSelectHandleLeft;
	}

	public void setTextSelectHandleLeft(int textSelectHandleLeft) {
		if (textSelectHandleLeft == 0)
			throw new IllegalArgumentException("The text select left handle should be a valid drawable resource id.");
		setTextSelectHandleLeft(getContext().getDrawable(textSelectHandleLeft));
	}

	public Drawable getTextSelectHandleLeft() {
		if (mTextSelectHandleLeft == null && mTextSelectHandleLeftRes != 0)
			mTextSelectHandleLeft = getContext().getDrawable(mTextSelectHandleLeftRes);
		return mTextSelectHandleLeft;
	}

	public void setTextSelectHandleRight(Drawable textSelectHandleRight) {
		java.util.Objects.requireNonNull(textSelectHandleRight, "The right text select handle should not be null.");
		mTextSelectHandleRight = textSelectHandleRight;
	}

	public void setTextSelectHandleRight(int textSelectHandleRight) {
		if (textSelectHandleRight == 0)
			throw new IllegalArgumentException("The text select right handle should be a valid drawable resource id.");
		setTextSelectHandleRight(getContext().getDrawable(textSelectHandleRight));
	}

	public Drawable getTextSelectHandleRight() {
		if (mTextSelectHandleRight == null && mTextSelectHandleRightRes != 0)
			mTextSelectHandleRight = getContext().getDrawable(mTextSelectHandleRightRes);
		return mTextSelectHandleRight;
	}

	public final MovementMethod getMovementMethod() {
		return mMovement;
	}




	public static interface OnEditorActionListener {
		public abstract boolean onEditorAction(TextView v, int actionId, KeyEvent event);
	}

	public static enum BufferType {
		EDITABLE,
		NORMAL,
		SPANNABLE,
	}

	public Layout getLayout() {
		if (mParagraphText)
			return paragraphLayout();
		return new BoringLayout(getText(), getPaint(), getWidth(), Layout.Alignment.ALIGN_NORMAL, 1, 0, new BoringLayout.Metrics(), false);
	}

	public int getCurrentTextColor() {
		return mCurTextColor;
	}

	public void setHint(int resId) {
		setHint(getContext().getResources().getText(resId));
	}

	public float getLetterSpacing() {
		return getPaint().getLetterSpacing();
	}


	/** AOSP isTextSelectable(): false unless set (mEditor == null or mTextIsSelectable false) */
	public boolean isTextSelectable() { return mTextIsSelectable; }


	public void setTextAppearance(int resId) {
		setTextAppearance(getContext(), resId);
	}

	public int length() {
		return getText().length();
	}

	/* AOSP TextView.setHighlightColor / getHighlightColor (mHighlightColor, default 0x6633B5E5); the colour is kept, ATL's
	 * text widgets draw their selection through GTK, which does not use it. */
	private int mHighlightColor = 0x6633B5E5;

	public void setHighlightColor(int color) {
		if (mHighlightColor != color) {
			mHighlightColor = color;
			invalidate();
		}
	}

	public int getHighlightColor() {
		return mHighlightColor;
	}

	/* AOSP TextView.setTextCursorDrawable / getTextCursorDrawable (textCursorDrawable attribute); the drawable is
	 * kept for the app, ATL's text widgets draw the cursor through GTK, which does not use it. */
	private int mCursorDrawableRes;
	private Drawable mCursorDrawable;

	public void setTextCursorDrawable(Drawable textCursorDrawable) {
		mCursorDrawable = textCursorDrawable;
		mCursorDrawableRes = 0;
	}

	public void setTextCursorDrawable(int textCursorDrawable) {
		setTextCursorDrawable(textCursorDrawable != 0 ? getContext().getDrawable(textCursorDrawable) : null);
	}

	public Drawable getTextCursorDrawable() {
		if (mCursorDrawable == null && mCursorDrawableRes != 0)
			mCursorDrawable = getContext().getDrawable(mCursorDrawableRes);
		return mCursorDrawable;
	}

	public Editable getEditableText() {
		return new SpannableStringBuilder(getText());
	}


	public void nullLayouts() {}

	public void setLinkTextColor(int color) {
		setLinkTextColor(ColorStateList.valueOf(color));
	}


	public void setRawInputType(int type) {
		mInputType = type;
	}

	public void setSelectAllOnFocus(boolean selectAllOnFocus) {}

	/* AOSP: the font's line spacing scaled and extended by the line spacing */
	public int getLineHeight() {
		return Math.round(getPaint().getFontMetricsInt(null) * mSpacingMult + mSpacingAdd);
	}

	/* The configuration of uniform auto-sizing as AOSP keeps it. ATL's TextView does not resize
	 * the text itself; what an app sets is what the getters report. */
	public static final int AUTO_SIZE_TEXT_TYPE_NONE = 0;
	public static final int AUTO_SIZE_TEXT_TYPE_UNIFORM = 1;
	private int mAutoSizeTextType = AUTO_SIZE_TEXT_TYPE_NONE;
	private float mAutoSizeMinTextSizeInPx = -1;
	private float mAutoSizeMaxTextSizeInPx = -1;
	private float mAutoSizeStepGranularityInPx = -1;
	private int[] mAutoSizeTextSizesInPx = new int[0];

	private float autoSizeToPx(int unit, float size) {
		return android.util.TypedValue.applyDimension(unit, size, getContext().getResources().getDisplayMetrics());
	}

	public int getAutoSizeTextType() {
		return mAutoSizeTextType;
	}

	public int getAutoSizeStepGranularity() {
		return Math.round(mAutoSizeStepGranularityInPx);
	}

	public int getAutoSizeMinTextSize() {
		return Math.round(mAutoSizeMinTextSizeInPx);
	}

	public int getAutoSizeMaxTextSize() {
		return Math.round(mAutoSizeMaxTextSizeInPx);
	}

	public int[] getAutoSizeTextAvailableSizes() {
		return mAutoSizeTextSizesInPx.clone();
	}

	public void setAutoSizeTextTypeWithDefaults(int autoSizeTextType) {
		switch (autoSizeTextType) {
			case AUTO_SIZE_TEXT_TYPE_NONE:
				mAutoSizeTextType = AUTO_SIZE_TEXT_TYPE_NONE;
				mAutoSizeMinTextSizeInPx = mAutoSizeMaxTextSizeInPx = mAutoSizeStepGranularityInPx = -1;
				mAutoSizeTextSizesInPx = new int[0];
				break;
			case AUTO_SIZE_TEXT_TYPE_UNIFORM:
				setAutoSizeTextTypeUniformWithConfiguration(12, 112, 1, android.util.TypedValue.COMPLEX_UNIT_SP);
				break;
			default:
				throw new IllegalArgumentException("Unknown auto-size text type: " + autoSizeTextType);
		}
	}

	public void setAutoSizeTextTypeUniformWithConfiguration(int autoSizeMinTextSize, int autoSizeMaxTextSize, int autoSizeStepGranularity, int unit) {
		float min = autoSizeToPx(unit, autoSizeMinTextSize);
		float max = autoSizeToPx(unit, autoSizeMaxTextSize);
		float step = autoSizeToPx(unit, autoSizeStepGranularity);
		if (min <= 0)
			throw new IllegalArgumentException("Minimum auto-size text size (" + min + "px) is less or equal to (0px)");
		if (max <= min)
			throw new IllegalArgumentException("Maximum auto-size text size (" + max + "px) is less or equal to minimum auto-size text size (" + min + "px)");
		if (step <= 0)
			throw new IllegalArgumentException("The auto-size step granularity (" + step + "px) is less or equal to (0px)");
		mAutoSizeTextType = AUTO_SIZE_TEXT_TYPE_UNIFORM;
		mAutoSizeMinTextSizeInPx = min;
		mAutoSizeMaxTextSizeInPx = max;
		mAutoSizeStepGranularityInPx = step;
		int count = (int)Math.floor((max - min) / step) + 1;
		int[] sizes = new int[count];
		for (int i = 0; i < count; i++)
			sizes[i] = Math.round(min + (i * step));
		mAutoSizeTextSizesInPx = cleanupAutoSizePresetSizes(sizes);
	}

	public void setAutoSizeTextTypeUniformWithPresetSizes(int[] presetSizes, int unit) {
		int[] sizes = new int[presetSizes.length];
		for (int i = 0; i < sizes.length; i++)
			sizes[i] = Math.round(autoSizeToPx(unit, presetSizes[i]));
		sizes = cleanupAutoSizePresetSizes(sizes);
		if (sizes.length <= 0)
			throw new IllegalArgumentException("None of the preset sizes is valid: " + java.util.Arrays.toString(presetSizes));
		mAutoSizeTextType = AUTO_SIZE_TEXT_TYPE_UNIFORM;
		mAutoSizeTextSizesInPx = sizes;
		mAutoSizeMinTextSizeInPx = sizes[0];
		mAutoSizeMaxTextSizeInPx = sizes[sizes.length - 1];
		mAutoSizeStepGranularityInPx = -1;
	}

	/* as AOSP: only positive values, sorted, without duplicates */
	private static int[] cleanupAutoSizePresetSizes(int[] presetValues) {
		java.util.TreeSet<Integer> set = new java.util.TreeSet<>();
		for (int v : presetValues)
			if (v > 0)
				set.add(v);
		int[] ret = new int[set.size()];
		int i = 0;
		for (int v : set)
			ret[i++] = v;
		return ret;
	}

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
		if (mParagraphText) {
			measureParagraphs(widthMeasureSpec, heightMeasureSpec);
			return;
		}
		native_setFontPadding(widget, include_padding);
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		/* AOSP TextView.onMeasure(): unless the width is exact, it is limited by maxWidth/maxEms and minWidth/minEms
		 * (setWidth(), android:width ...); the height then follows from the text in that width */
		int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		if (widthMode == MeasureSpec.EXACTLY) {
			applyHeightLimits(heightMeasureSpec);
			return;
		}
		int width = getMeasuredWidth();
		/* the label's natural width leaves out the letter spacing after the last letter, like AOSP's text width
		 * (minikin's adjustAdvanceLetterSpacingEdge()); Pango's line breaking counts it, so the label would
		 * ellipsize or wrap its text at that width */
		float letterSpacing = getPaint().getLetterSpacing() * getTextSize();
		if (letterSpacing > 0)
			width += (int)Math.ceil(letterSpacing);
		if (mMaxWidthMode == EMS) {
			width = Math.min(width, mMaxWidth * getLineHeight());
		} else {
			width = Math.min(width, mMaxWidth);
		}
		if (mMinWidthMode == EMS) {
			width = Math.max(width, mMinWidth * getLineHeight());
		} else {
			width = Math.max(width, mMinWidth);
		}
		width = Math.max(width, getSuggestedMinimumWidth());
		if (widthMode == MeasureSpec.AT_MOST) {
			width = Math.min(MeasureSpec.getSize(widthMeasureSpec), width);
		}
		if (width != getMeasuredWidth())
			super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), heightMeasureSpec);
		applyHeightLimits(heightMeasureSpec);
	}

	/* AOSP TextView.getDesiredHeight() with onMeasure(): unless the height is exact, a maximum in pixels caps
	 * it and a minimum in pixels raises it (the limits in lines are applied by GTK, see updateLines()) */
	private void applyHeightLimits(int heightMeasureSpec) {
		int heightMode = MeasureSpec.getMode(heightMeasureSpec);
		if (heightMode == MeasureSpec.EXACTLY)
			return;
		int height = getMeasuredHeight();
		if (mMaxMode == PIXELS)
			height = Math.min(height, mMaximum);
		if (mMinMode == PIXELS)
			height = Math.max(height, mMinimum);
		height = Math.max(height, getSuggestedMinimumHeight());
		if (heightMode == MeasureSpec.AT_MOST)
			height = Math.min(height, MeasureSpec.getSize(heightMeasureSpec));
		if (height != getMeasuredHeight())
			setMeasuredDimension(getMeasuredWidth(), height);
	}
	private native void native_setFontPadding(long widget, boolean include);

	public float getLineSpacingExtra() {
		return mSpacingAdd;
	}

	public float getLineSpacingMultiplier() {
		return mSpacingMult;
	}

	/* AOSP TextView.getTextMetricsParams()/setTextMetricsParams(): how this view measures its text */
	public android.text.PrecomputedText.Params getTextMetricsParams() {
		return new android.text.PrecomputedText.Params.Builder(getPaint()).setBreakStrategy(getBreakStrategy())
		    .setHyphenationFrequency(getHyphenationFrequency()).setTextDirection(getTextDirectionHeuristic()).build();
	}

	public void setTextMetricsParams(android.text.PrecomputedText.Params params) {
		getPaint().set(params.getTextPaint());
		setBreakStrategy(params.getBreakStrategy());
		setHyphenationFrequency(params.getHyphenationFrequency());
	}

	public TextDirectionHeuristic getTextDirectionHeuristic() {
		return TextDirectionHeuristics.LTR;
	}

	public Bundle getInputExtras(boolean key) {
		return new Bundle();
	}

	/* AOSP Editor.mError: ATL shows no error popup or icon, setError() only remembers the error (a Spanned is kept as such,
	 * TextUtils.stringOrSpannedString()) so getError() returns it */
	private CharSequence mError;

	public void setError(CharSequence error) {
		mError = error == null ? null : TextUtils.stringOrSpannedString(error);
	}

	/** AOSP setError(CharSequence, Drawable): the icon is not shown (see above), the error is kept as in setError(CharSequence) */
	public void setError(CharSequence error, Drawable icon) {
		setError(error);
	}

	/** AOSP getError(): mEditor == null ? null : mEditor.mError (in ATL an input type stands for the Editor, see mInputType) */
	public CharSequence getError() {
		return mInputType == 0 ? null : mError;
	}

	public int getBreakStrategy() {
		return break_strategy;
	}

	public void setBreakStrategy(int strategy) {
		break_strategy = strategy;
	}

	/** AOSP clearComposingText(): removes the composing spans of a Spannable text */
	public void clearComposingText() {
		if (getText() instanceof Spannable) {
			android.view.inputmethod.BaseInputConnection.removeComposingSpans((Spannable)getText());
		}
	}

	/* AOSP TextView.setWidth(): exactly this wide (as minimum and maximum width) */
	public void setWidth(int pixels) {
		mMaxWidth = mMinWidth = pixels;
		mMaxWidthMode = mMinWidthMode = PIXELS;
		requestLayout();
		invalidate();
	}

	/** AOSP TextView.setHeight(): minimum and maximum height in pixels (replaces lines/minLines/maxLines) */
	public void setHeight(int pixels) {
		mMaximum = mMinimum = pixels;
		mMaxMode = mMinMode = PIXELS;
		updateLines();
		requestLayout();
		invalidate();
	}

	/* AOSP setFreezesText()/getFreezesText(): whether the text is part of the saved state; ATL does not save view state,
	 * so the flag is only kept */
	private boolean mFreezesText;

	public void setFreezesText(boolean freezesText) {
		mFreezesText = freezesText;
	}

	public boolean getFreezesText() {
		return mFreezesText;
	}

	/* AOSP: in ems, kept by the paint; the label gets it as a Pango letter spacing in pixels */
	public void setLetterSpacing(float letterSpacing) {
		if (letterSpacing != getPaint().getLetterSpacing()) {
			getPaint().setLetterSpacing(letterSpacing);
			applyNativeLetterSpacing();
			requestLayout();
			invalidate();
		}
	}

	private void applyNativeLetterSpacing() {
		native_setLetterSpacing(widget, getPaint().getLetterSpacing() * getTextSize());
	}
	private native void native_setLetterSpacing(long widget, float spacing);

	/* AOSP mMarqueeRepeatLimit (default 3); ATL does not scroll marquee text (ellipsizes it), so only kept */
	private int mMarqueeRepeatLimit = 3;

	public void setMarqueeRepeatLimit(int marqueeLimit) {
		mMarqueeRepeatLimit = marqueeLimit;
	}

	public int getMarqueeRepeatLimit() {
		return mMarqueeRepeatLimit;
	}

	private int mJustificationMode = 0; // Layout.JUSTIFICATION_MODE_NONE

	public int getJustificationMode() {
		return mJustificationMode;
	}

	/** AOSP setJustificationMode(): the mode is kept; the layout of ATL does not justify */
	public void setJustificationMode(int justificationMode) {
		mJustificationMode = justificationMode;
		requestLayout();
		invalidate();
	}

	/** AOSP setElegantTextHeight() */
	public void setElegantTextHeight(boolean elegant) {
		if (elegant != getPaint().isElegantTextHeight()) {
			getPaint().setElegantTextHeight(elegant);
			requestLayout();
			invalidate();
		}
	}

	private boolean mFallbackLineSpacing;

	/** AOSP setFallbackLineSpacing(): kept, requests a new layout like AOSP */
	public void setFallbackLineSpacing(boolean enabled) {
		if (mFallbackLineSpacing != enabled) {
			mFallbackLineSpacing = enabled;
			requestLayout();
			invalidate();
		}
	}

	/** AOSP getFontFeatureSettings() */
	public String getFontFeatureSettings() {
		return getPaint().getFontFeatureSettings();
	}

	/** AOSP setFontFeatureSettings() */
	public void setFontFeatureSettings(String fontFeatureSettings) {
		if (fontFeatureSettings != getPaint().getFontFeatureSettings() && (fontFeatureSettings == null || !fontFeatureSettings.equals(getPaint().getFontFeatureSettings()))) {
			getPaint().setFontFeatureSettings(fontFeatureSettings);
			requestLayout();
			invalidate();
		}
	}

	/** AOSP isSuggestionsEnabled(): false without Editor (see mInputType), else only for a text class input without
	 * TYPE_TEXT_FLAG_NO_SUGGESTIONS and with a variation that allows suggestions */
	public boolean isSuggestionsEnabled() {
		if (mInputType == 0)
			return false;
		if ((mInputType & android.text.InputType.TYPE_MASK_CLASS) != android.text.InputType.TYPE_CLASS_TEXT) {
			return false;
		}
		if ((mInputType & android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS) > 0)
			return false;

		final int variation = mInputType & android.text.InputType.TYPE_MASK_VARIATION;
		return (variation == android.text.InputType.TYPE_TEXT_VARIATION_NORMAL ||
		        variation == android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_SUBJECT ||
		        variation == android.text.InputType.TYPE_TEXT_VARIATION_LONG_MESSAGE ||
		        variation == android.text.InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE ||
		        variation == android.text.InputType.TYPE_TEXT_VARIATION_WEB_EDIT_TEXT);
	}

	/** AOSP setTextKeepState(): sets the text and keeps the selection, clamped to the new length */
	public final void setTextKeepState(CharSequence text) {
		setTextKeepState(text, mBufferType);
	}

	public final void setTextKeepState(CharSequence text, BufferType type) {
		int start = getSelectionStart();
		int end = getSelectionEnd();
		int len = text.length();

		setText(text, type);

		if (start >= 0 || end >= 0) {
			if (getText() instanceof Spannable) {
				Selection.setSelection((Spannable)getText(), Math.max(0, Math.min(start, len)), Math.max(0, Math.min(end, len)));
			}
		}
	}

	/** AOSP onPreDraw() (ViewTreeObserver.OnPreDrawListener): lets the drawing go on. Not reproduced: the scrolling of the
	 * cursor into view that AOSP does for a movement method, ATL has no Editor cursor controller. */
	public boolean onPreDraw() {
		return true;
	}

	/** AOSP extractText() = Editor.extractText(request, outText) = extractTextInternal() for the whole text (offsets
	 * EXTRACT_UNKNOWN): false without Editor (see mInputType), request or outText */
	public boolean extractText(android.view.inputmethod.ExtractedTextRequest request, android.view.inputmethod.ExtractedText outText) {
		if (mInputType == 0 || request == null || outText == null) {
			return false;
		}

		final CharSequence content = getText();
		if (content == null) {
			return false;
		}

		final int N = content.length();
		outText.partialStartOffset = outText.partialEndOffset = -1;
		if ((request.flags & android.view.inputmethod.InputConnection.GET_TEXT_WITH_STYLES) != 0) {
			outText.text = content.subSequence(0, N);
		} else {
			outText.text = TextUtils.substring(content, 0, N);
		}
		outText.flags = 0;
		if (android.text.method.MetaKeyKeyListener.getMetaState(content, android.text.method.MetaKeyKeyListener.META_SELECTING) != 0) {
			outText.flags |= android.view.inputmethod.ExtractedText.FLAG_SELECTING;
		}
		if (isSingleLine()) {
			outText.flags |= android.view.inputmethod.ExtractedText.FLAG_SINGLE_LINE;
		}
		outText.startOffset = 0;
		outText.selectionStart = getSelectionStart();
		outText.selectionEnd = getSelectionEnd();
		outText.hint = getHint();
		return true;
	}

	/** AOSP setLocalePreferredLineHeightForMinimumUsed(): kept, requests a new layout like AOSP */
	private boolean mUseLocalePreferredLineHeightForMinimum;

	public void setLocalePreferredLineHeightForMinimumUsed(boolean flag) {
		if (mUseLocalePreferredLineHeightForMinimum != flag) {
			mUseLocalePreferredLineHeightForMinimum = flag;
			requestLayout();
			invalidate();
		}
	}

	public boolean isLocalePreferredLineHeightForMinimumUsed() {
		return mUseLocalePreferredLineHeightForMinimum;
	}

	/** AOSP setScroller(): the Scroller is kept (ATL scrolls through GTK, it is not driven) */
	private Scroller mScroller;

	public void setScroller(Scroller s) {
		mScroller = s;
	}

	/** AOSP getCompoundPaddingStart()/getCompoundPaddingEnd() */
	public int getCompoundPaddingStart() {
		switch (getLayoutDirection()) {
		default:
		case LAYOUT_DIRECTION_LTR:
			return getCompoundPaddingLeft();
		case LAYOUT_DIRECTION_RTL:
			return getCompoundPaddingRight();
		}
	}

	public int getCompoundPaddingEnd() {
		switch (getLayoutDirection()) {
		default:
		case LAYOUT_DIRECTION_LTR:
			return getCompoundPaddingRight();
		case LAYOUT_DIRECTION_RTL:
			return getCompoundPaddingLeft();
		}
	}

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


	/* ATL: AOSP's onDraw() paints the compound drawables itself; here the GtkBox of the label does (in their
	 * bounds, see native_setCompoundDrawables), so every change of the drawables that are shown (mShowing,
	 * already resolved for the layout direction) is passed to it. A Button has the box in its GtkButton.
	 * Subclasses whose native widget has no such box (EditText) or that add a drawable (CheckedTextView)
	 * override this. */
	protected void updateNativeCompoundDrawables() {
		final Drawable[] d = getCompoundDrawables();
		boolean any = d[Drawables.LEFT] != null || d[Drawables.TOP] != null || d[Drawables.RIGHT] != null || d[Drawables.BOTTOM] != null;
		if (!any && !nativeHasDrawables)
			return; // the constructor sets "no drawables": nothing to tell the native side
		nativeHasDrawables = any;
		native_setCompoundDrawables(widget, d[Drawables.LEFT], d[Drawables.TOP], d[Drawables.RIGHT], d[Drawables.BOTTOM]);
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

	/* AOSP: with getVerticalOffset(true), which only the StaticLayout of a paragraph text has here */
	public int getTotalPaddingTop() {
		return getExtendedPaddingTop() + (mParagraphText ? paragraphVerticalOffset(paragraphLayout()) : 0);
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
			/* AOSP lays out a horizontally scrolling text (singleLine) in one line as wide as the text: the label
			 * does not wrap, so that it measures that width */
			native_setWrap(!whether);

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
		// AOSP TextView.setInputTypeSingleLine(): TYPE_MASK_CLASS 0xf, TYPE_CLASS_TEXT 0x1, TYPE_TEXT_FLAG_MULTI_LINE 0x20000
		if (mInputType != 0 && (mInputType & 0x0000000f) == 0x00000001) {
			if (singleLine) {
				mInputType &= ~0x00020000;
			} else {
				mInputType |= 0x00020000;
			}
		}
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

	/* as AOSP; setText() adds the links of the mask */
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

	/* ---- AOSP TextView members that apps call and override and that ATL had no counterpart for. ATL's TextView is
	 * a GtkLabel, an EditText a GtkText: what the label cannot show or the app cannot reach (a floating selection
	 * toolbar, the shadow, text locales for the shaper, the IME's hints) is kept and returned, as AOSP does, and said
	 * so where it matters. ---- */

	/* AOSP TextView.getCurrentHintTextColor(): the hint color for the drawable state; the text color while no
	 * hint color is set */
	public final int getCurrentHintTextColor() {
		return hint_colors != null ? hint_colors.getColorForState(getDrawableState(), hint_colors.getDefaultColor()) : mCurTextColor;
	}

	/* AOSP TextView.bringPointIntoView(): scrolls the text so that the offset is visible. The label does not scroll
	 * its text inside the view (a single line label is cut or ellipsized), so nothing moves and AOSP's answer for
	 * a text that did not scroll is false */
	public boolean bringPointIntoView(int offset) {
		return bringPointIntoView(offset, false);
	}

	/** AOSP bringPointIntoView(int, boolean): nothing scrolls here, see above */
	public boolean bringPointIntoView(int offset, boolean requestRectWithoutFocus) {
		return false;
	}

	private ActionMode.Callback mCustomSelectionActionModeCallback;
	private ActionMode.Callback mCustomInsertionActionModeCallback;

	/* the callbacks are kept; ATL starts no selection or insertion action mode, so they are never called */
	public void setCustomSelectionActionModeCallback(ActionMode.Callback actionModeCallback) {
		mCustomSelectionActionModeCallback = actionModeCallback;
	}

	public ActionMode.Callback getCustomSelectionActionModeCallback() {
		return mCustomSelectionActionModeCallback;
	}

	public void setCustomInsertionActionModeCallback(ActionMode.Callback actionModeCallback) {
		mCustomInsertionActionModeCallback = actionModeCallback;
	}

	public ActionMode.Callback getCustomInsertionActionModeCallback() {
		return mCustomInsertionActionModeCallback;
	}

	private int mImeActionId;
	private CharSequence mImeActionLabel;

	public void setImeActionLabel(CharSequence label, int actionId) {
		mImeActionLabel = label;
		mImeActionId = actionId;
	}

	public CharSequence getImeActionLabel() {
		return mImeActionLabel;
	}

	public int getImeActionId() {
		return mImeActionId;
	}

	private int mImeOptions = 0; // EditorInfo.IME_NULL

	/* AOSP TextView.isMultilineInputType() */
	private static boolean isMultilineInputType(int type) {
		return (type & (EditorInfo.TYPE_MASK_CLASS | EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE)) == (EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE);
	}

	/* AOSP: whether the view is editable without the editable attribute (EditText: yes) */
	protected boolean getDefaultEditable() {
		return false;
	}

	public void setImeOptions(int imeOptions) {
		mImeOptions = imeOptions;
	}

	public int getImeOptions() {
		return mImeOptions;
	}

	private LocaleList mImeHintLocales;

	public void setImeHintLocales(LocaleList hintLocales) {
		mImeHintLocales = hintLocales;
	}

	public LocaleList getImeHintLocales() {
		return mImeHintLocales;
	}

	private boolean mShowSoftInputOnFocus = true;

	public final void setShowSoftInputOnFocus(boolean show) {
		mShowSoftInputOnFocus = show;
	}

	public final boolean getShowSoftInputOnFocus() {
		return mShowSoftInputOnFocus;
	}

	private boolean mCursorVisible = true;

	public void setCursorVisible(boolean visible) {
		mCursorVisible = visible;
	}

	public boolean isCursorVisible() {
		return mCursorVisible;
	}

	private boolean mRevealOnFocusHint = true;

	public final void setRevealOnFocusHint(boolean revealOnFocus) {
		mRevealOnFocusHint = revealOnFocus;
	}

	public final boolean getRevealOnFocusHint() {
		return mRevealOnFocusHint;
	}

	private boolean mUseBoundsForWidth;

	public void setUseBoundsForWidth(boolean useBoundsForWidth) {
		mUseBoundsForWidth = useBoundsForWidth;
	}

	public boolean getUseBoundsForWidth() {
		return mUseBoundsForWidth;
	}

	/* AOSP TextView.setShadowLayer(): the shadow of the text (a Paint's); a GtkLabel draws none, the values are kept
	 * and the paint holds them like AOSP's */
	private float mShadowRadius;
	private float mShadowDx;
	private float mShadowDy;
	private int mShadowColor;

	public void setShadowLayer(float radius, float dx, float dy, int color) {
		mShadowRadius = radius;
		mShadowDx = dx;
		mShadowDy = dy;
		mShadowColor = color;
		getPaint().setShadowLayer(radius, dx, dy, color);
	}

	public float getShadowRadius() {
		return mShadowRadius;
	}

	public float getShadowDx() {
		return mShadowDx;
	}

	public float getShadowDy() {
		return mShadowDy;
	}

	public int getShadowColor() {
		return mShadowColor;
	}

	/* AOSP TextView.setTextScaleX(): the horizontal scale of the glyphs, the paint's */
	public void setTextScaleX(float size) {
		if (size != getPaint().getTextScaleX()) {
			getPaint().setTextScaleX(size);
			requestLayout();
			invalidate();
		}
	}

	public float getTextScaleX() {
		return getPaint().getTextScaleX();
	}

	/* AOSP TextView.setFontVariationSettings(): true if the settings are valid and the paint took them */
	public boolean setFontVariationSettings(String fontVariationSettings) {
		final String existingSettings = getPaint().getFontVariationSettings();
		if (fontVariationSettings == existingSettings || (fontVariationSettings != null && fontVariationSettings.equals(existingSettings))) {
			return true;
		}
		boolean effective = getPaint().setFontVariationSettings(fontVariationSettings);
		if (effective) {
			requestLayout();
			invalidate();
		}
		return effective;
	}

	public String getFontVariationSettings() {
		return getPaint().getFontVariationSettings();
	}

	private LocaleList mTextLocales;

	/* AOSP TextView.setTextLocales(): the locales the shaper picks the glyphs by (Han unification, hyphenation);
	 * the paint keeps them, Pango picks by the label's language */
	public void setTextLocales(LocaleList locales) {
		mTextLocales = locales;
		getPaint().setTextLocales(locales);
		requestLayout();
		invalidate();
	}

	public LocaleList getTextLocales() {
		return mTextLocales != null ? mTextLocales : LocaleList.getDefault();
	}

	public void setTextLocale(Locale locale) {
		setTextLocales(new LocaleList(locale));
	}

	public Locale getTextLocale() {
		return getTextLocales().get(0);
	}

	private TextClassifier mTextClassifier;

	public void setTextClassifier(TextClassifier textClassifier) {
		mTextClassifier = textClassifier;
	}

	/* AOSP TextView.requestActionMode(TextLinkSpan) (@hide): starts the selection action mode around the link.
	 * ATL's TextView has no selection action mode, so none is started (AOSP's answer when it cannot start one) */
	public boolean requestActionMode(android.view.textclassifier.TextLinks.TextLinkSpan clickedSpan) {
		java.util.Objects.requireNonNull(clickedSpan);
		return false;
	}

	/* AOSP TextView.handleClick(TextLinkSpan) (@hide): classifies the link text off the UI thread and sends the
	 * first action's intent. AOSP gives up after 1 s (completeOnTimeout); that timeout is not carried over */
	public boolean handleClick(android.view.textclassifier.TextLinks.TextLinkSpan clickedSpan) {
		java.util.Objects.requireNonNull(clickedSpan);
		final CharSequence text = getText();
		if (text instanceof android.text.Spanned) {
			final android.text.Spanned spanned = (android.text.Spanned) text;
			final int start = spanned.getSpanStart(clickedSpan);
			final int end = spanned.getSpanEnd(clickedSpan);
			if (start >= 0 && end <= text.length() && start < end) {
				final android.view.textclassifier.TextClassification.Request request =
				    new android.view.textclassifier.TextClassification.Request.Builder(text, start, end)
				        .setDefaultLocales(getTextLocales())
				        .build();
				final TextClassifier classifier = getTextClassifier();
				new Thread(() -> {
					android.view.textclassifier.TextClassification classification = classifier.classifyText(request);
					if (classification != null && !classification.getActions().isEmpty()) {
						// AOSP catches PendingIntent.CanceledException here; ATL's send() does not throw it
						classification.getActions().get(0).getActionIntent().send();
					} else {
						Log.d("TextView", "No link action to perform");
					}
				}).start();
				return true;
			}
		}
		return false;
	}

	/* AOSP TextView.getTextClassifier(): the one set, else the one of the TextClassificationManager */
	public TextClassifier getTextClassifier() {
		if (mTextClassifier == null) {
			TextClassificationManager manager = (TextClassificationManager)getContext().getSystemService(Context.TEXT_CLASSIFICATION_SERVICE);
			if (manager != null) {
				return manager.getTextClassifier();
			}
			return TextClassifier.NO_OP;
		}
		return mTextClassifier;
	}

	/* AOSP TextView.onPrivateIMECommand(): a command an input method sent to this editor; none does here */
	public boolean onPrivateIMECommand(String action, Bundle data) {
		return false;
	}

	/* AOSP TextView.onTextChanged(), onSelectionChanged(): the hooks a subclass overrides */
	protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
	}

	protected void onSelectionChanged(int selStart, int selEnd) {
		sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED);
	}

	/* an EditText's text comes from the GtkText, which calls back; for a label setText() calls the listeners */
	boolean textChangesComeFromNative() {
		return false;
	}

	private ArrayList<TextWatcher> mWatchers;

	public void addTextChangedListener(TextWatcher watcher) {
		if (mWatchers == null) {
			mWatchers = new ArrayList<TextWatcher>();
		}
		mWatchers.add(watcher);
	}

	public void removeTextChangedListener(TextWatcher watcher) {
		if (mWatchers != null) {
			int i = mWatchers.indexOf(watcher);
			if (i >= 0) {
				mWatchers.remove(i);
			}
		}
	}

	/* AOSP TextView.sendBeforeTextChanged(), sendOnTextChanged(), sendAfterTextChanged(): the watchers hear the
	 * change, in this order, then the view's own onTextChanged() */
	void sendBeforeTextChanged(CharSequence text, int start, int before, int after) {
		if (mWatchers != null) {
			for (TextWatcher w : new ArrayList<TextWatcher>(mWatchers)) {
				w.beforeTextChanged(text, start, before, after);
			}
		}
	}

	void handleTextChanged(CharSequence buffer, int start, int before, int after) {
		if (mWatchers != null) {
			for (TextWatcher w : new ArrayList<TextWatcher>(mWatchers)) {
				w.onTextChanged(buffer, start, before, after);
			}
		}
		onTextChanged(buffer, start, before, after);
	}

	void sendAfterTextChanged(Editable text) {
		if (mWatchers != null) {
			for (TextWatcher w : new ArrayList<TextWatcher>(mWatchers)) {
				w.afterTextChanged(text);
			}
		}
	}

	public void setText(int resid, BufferType type) {
		setText(getContext().getResources().getText(resid), type);
	}

	/* AOSP TextView.setEms(), setMinEms(), setMaxEms(), setMinWidth()...: the size limits in ems or pixels, as set */
	private int mMaxWidth = Integer.MAX_VALUE;
	private int mMaxWidthMode = PIXELS;
	private int mMinWidth = 0;
	private int mMinWidthMode = PIXELS;

	public void setEms(int ems) {
		mMaxWidth = mMinWidth = ems;
		mMaxWidthMode = mMinWidthMode = EMS;
		requestLayout();
		invalidate();
	}

	public void setMinEms(int minEms) {
		mMinWidth = minEms;
		mMinWidthMode = EMS;
		requestLayout();
		invalidate();
	}

	public void setMaxEms(int maxEms) {
		mMaxWidth = maxEms;
		mMaxWidthMode = EMS;
		requestLayout();
		invalidate();
	}

	public int getMinEms() {
		return mMinWidthMode == EMS ? mMinWidth : -1;
	}

	public int getMaxEms() {
		return mMaxWidthMode == EMS ? mMaxWidth : -1;
	}

	public void setMinWidth(int minPixels) {
		mMinWidth = minPixels;
		mMinWidthMode = PIXELS;
		requestLayout();
		invalidate();
	}

	public int getMinWidth() {
		return mMinWidthMode == PIXELS ? mMinWidth : -1;
	}

	public void setMaxWidth(int maxPixels) {
		mMaxWidth = maxPixels;
		mMaxWidthMode = PIXELS;
		requestLayout();
		invalidate();
	}

	public int getMaxWidth() {
		return mMaxWidthMode == PIXELS ? mMaxWidth : -1;
	}

	/* AOSP TextView.setMinHeight()/setMaxHeight(): the height limits in pixels share mMinimum/mMaximum with the
	 * limits in lines; the getters return -1 while the limit is in lines */
	public void setMinHeight(int minPixels) {
		mMinimum = minPixels;
		mMinMode = PIXELS;
		requestLayout();
		invalidate();
	}

	public int getMinHeight() {
		return mMinMode == PIXELS ? mMinimum : -1;
	}

	public void setMaxHeight(int maxPixels) {
		mMaximum = maxPixels;
		mMaxMode = PIXELS;
		updateLines();
		requestLayout();
		invalidate();
	}

	public int getMaxHeight() {
		return mMaxMode == PIXELS ? mMaximum : -1;
	}

	private KeyListener mKeyListener;

	public void setKeyListener(KeyListener input) {
		mKeyListener = input;
	}

	public KeyListener getKeyListener() {
		return mKeyListener;
	}

	public void setPaintFlags(int flags) {
		if (getPaint().getFlags() != flags) {
			getPaint().setFlags(flags);
			native_setPaintFlags(flags);
			requestLayout();
			invalidate();
		}
	}

	public int getPaintFlags() {
		return getPaint().getFlags();
	}

	private boolean mAllCaps;

	public boolean isAllCaps() {
		return mAllCaps;
	}

	/* AOSP TextView.getUrls(): the URLSpans of the text */
	public URLSpan[] getUrls() {
		if (text instanceof Spanned) {
			return ((Spanned)text).getSpans(0, text.length(), URLSpan.class);
		} else {
			return new URLSpan[0];
		}
	}

	public boolean isSingleLine() {
		return mSingleLine;
	}

	/* AOSP TextView.getLineBounds(): the bounds of the line in the view's coordinates, and its baseline */
	public int getLineBounds(int line, Rect bounds) {
		Layout layout = getLayout();
		if (layout == null) {
			if (bounds != null) {
				bounds.set(0, 0, 0, 0);
			}
			return 0;
		} else {
			int baseline = layout.getLineBounds(line, bounds);
			int voffset = getExtendedPaddingTop();
			if (bounds != null) {
				bounds.offset(getCompoundPaddingLeft(), voffset);
			}
			return baseline + voffset;
		}
	}

	/* AOSP TextView.onTouchEvent() for a paragraph text, which this view lays out itself: the movement method gets
	 * the touch (LinkMovementMethod clicks its links). Other texts are the label's (its links, its selection), as
	 * are the Editor parts of AOSP. */
	@Export
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		final int action = event.getActionMasked();
		final ParagraphTextSelection selection = paragraphSelection();
		if (selection != null) {
			selection.onTouchEvent(event);
		}
		final boolean superResult = super.onTouchEvent(event);
		/* AOSP: the up after a long press or double tap belongs to the selection */
		if (selection != null && selection.mDiscardNextActionUp && action == MotionEvent.ACTION_UP) {
			selection.mDiscardNextActionUp = false;
			return superResult;
		}
		final boolean touchIsFinished = (action == MotionEvent.ACTION_UP) && isFocused();
		if (mParagraphText && mMovement != null && isEnabled() && text instanceof Spannable) {
			boolean handled = mMovement.onTouchEvent(this, (Spannable)text, event);
			if (touchIsFinished && selection != null) {
				selection.onTouchUpEvent(event);
				handled = true;
			}
			if (handled)
				return true;
		}
		return superResult;
	}

	/* AOSP TextView.performLongClick(): the Editor selects the word of a selectable paragraph text unless a long
	 * click listener took the click; the up that ends the press is discarded */
	@Override
	public boolean performLongClick() {
		boolean handled = false;
		boolean performedHapticFeedback = false;
		final ParagraphTextSelection selection = paragraphSelection();
		if (selection != null)
			selection.mIsBeingLongClicked = true;
		if (super.performLongClick()) {
			handled = true;
			performedHapticFeedback = true;
		}
		if (selection != null) {
			handled |= selection.performLongClick(handled);
			selection.mIsBeingLongClicked = false;
		}
		if (handled) {
			if (!performedHapticFeedback)
				performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
			if (selection != null)
				selection.mDiscardNextActionUp = true;
		}
		return handled;
	}

	/* AOSP TextView.getLineAtCoordinate() */
	int getLineAtCoordinate(float y) {
		y -= getTotalPaddingTop();
		// Clamp the position to inside of the view.
		y = Math.max(0.0f, y);
		y = Math.min(getHeight() - getTotalPaddingBottom() - 1, y);
		y += getScrollY();
		return getLayout().getLineForVertical((int)y);
	}

	/* AOSP TextView.convertToLocalHorizontalCoordinate() */
	float convertToLocalHorizontalCoordinate(float x) {
		x -= getTotalPaddingLeft();
		// Clamp the position to inside of the view.
		x = Math.max(0.0f, x);
		x = Math.min(getWidth() - getTotalPaddingRight() - 1, x);
		x += getScrollX();
		return x;
	}

	/* AOSP TextView.canCopy()/canShare()/canSelectAllText()/canSelectText(); ATL's mEditor is the selection of a
	 * paragraph text (or an EditText's GtkText), there are no password transformations of a label */
	boolean canCopy() {
		if (hasPasswordInputType())
			return false;
		return getText().length() > 0 && hasSelection() && (this instanceof EditText || paragraphSelection() != null);
	}

	/* AOSP TextView.canCut(): an editable text (here an EditText, whose GtkText takes the keys) */
	boolean canCut() {
		if (hasPasswordInputType())
			return false;
		return getText().length() > 0 && hasSelection() && this instanceof EditText;
	}

	/* AOSP TextView.canPaste() */
	boolean canPaste() {
		if (!(this instanceof EditText) || getSelectionStart() < 0 || getSelectionEnd() < 0)
			return false;
		android.content.ClipboardManager clipboard = (android.content.ClipboardManager)getContext().getSystemService(Context.CLIPBOARD_SERVICE);
		return clipboard != null && clipboard.hasPrimaryClip();
	}

	/* AOSP hasPasswordTransformationMethod(): ATL has the password transformation by the input type only */
	private boolean hasPasswordInputType() {
		final int variation = mInputType & 0x0fff; // TYPE_MASK_CLASS | TYPE_MASK_VARIATION
		return variation == 0x81 || variation == 0x91 || variation == 0xe1 || variation == 0x12;
	}

	boolean canShare() {
		if (!getContext().getResources().getBoolean(com.android.internal.R.bool.config_textShareSupported))
			return false;
		return canCopy();
	}

	boolean canSelectText() {
		return getText().length() != 0 && (this instanceof EditText || paragraphSelection() != null);
	}

	boolean canSelectAllText() {
		return canSelectText() && !(getSelectionStart() == 0 && getSelectionEnd() == getText().length());
	}

	/* AOSP TextView.shareSelectedText() */
	private void shareSelectedText() {
		final int start = Math.max(0, Math.min(getSelectionStart(), getSelectionEnd()));
		final int end = Math.max(0, Math.max(getSelectionStart(), getSelectionEnd()));
		String selectedText = start == end ? null : String.valueOf(getText().subSequence(start, end));
		if (selectedText != null && !selectedText.isEmpty()) {
			android.content.Intent sharingIntent = new android.content.Intent(android.content.Intent.ACTION_SEND);
			sharingIntent.setType("text/plain");
			sharingIntent.removeExtra(android.content.Intent.EXTRA_TEXT);
			selectedText = (String)TextUtils.trimToParcelableSize(selectedText);
			sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, selectedText);
			getContext().startActivity(android.content.Intent.createChooser(sharingIntent, null));
			setSelection(getSelectionEnd());
		}
	}

	/* AOSP TextView.getOffsetForPosition(): the character offset closest to the point, -1 without a layout */
	public int getOffsetForPosition(float x, float y) {
		Layout layout = getLayout();
		if (layout == null) {
			return -1;
		}
		float xInLayout = Math.max(0, Math.min(x - getTotalPaddingLeft() + getScrollX(), getWidth() - getTotalPaddingRight() + getScrollX() - 1));
		final int line = layout.getLineForVertical((int)(y - getTotalPaddingTop() + getScrollY()));
		return layout.getOffsetForHorizontal(line, xInLayout);
	}

	@Override
	public CharSequence getAccessibilityClassName() {
		return TextView.class.getName();
	}

	/* AOSP TextView accessibility: the event and the node get the class, whether it is a password and the text
	 * (a password's text is left out) */
	@Override
	public void onInitializeAccessibilityEventInternal(AccessibilityEvent event) {
		super.onInitializeAccessibilityEventInternal(event);
		event.setClassName(getAccessibilityClassName());
		final boolean isPassword = (mInputType & 0x0fff) == 0x81 || (mInputType & 0x0fff) == 0x91 || (mInputType & 0x0fff) == 0xe1 || (mInputType & 0x0fff) == 0x12;
		event.setPassword(isPassword);
		if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED) {
			event.setFromIndex(getSelectionStart());
			event.setToIndex(getSelectionEnd());
			event.setItemCount(text.length());
		}
	}

	@Override
	public void onPopulateAccessibilityEventInternal(AccessibilityEvent event) {
		super.onPopulateAccessibilityEventInternal(event);
		CharSequence t = getText();
		if (!TextUtils.isEmpty(t)) {
			event.getText().add(t);
		}
	}

	@Override
	public void onInitializeAccessibilityNodeInfoInternal(AccessibilityNodeInfo info) {
		super.onInitializeAccessibilityNodeInfoInternal(info);
		info.setClassName(getAccessibilityClassName());
		final boolean isPassword = (mInputType & 0x0fff) == 0x81 || (mInputType & 0x0fff) == 0x91 || (mInputType & 0x0fff) == 0xe1 || (mInputType & 0x0fff) == 0x12;
		info.setPassword(isPassword);
		if (!isPassword) {
			info.setText(getText());
		}
	}

	@Override
	public PointerIcon onResolvePointerIcon(MotionEvent event, int pointerIndex) {
		if (this instanceof EditText) {
			return PointerIcon.getSystemIcon(getContext(), PointerIcon.TYPE_TEXT);
		}
		return super.onResolvePointerIcon(event, pointerIndex);
	}

	/* AOSP TextView autofill: an editable text takes a text value and gives its text */
	public int getAutofillType() {
		return this instanceof EditText ? AUTOFILL_TYPE_TEXT : AUTOFILL_TYPE_NONE;
	}

	public void autofill(AutofillValue value) {
		if (!(this instanceof EditText)) {
			return;
		}
		if (!value.isText()) {
			Log.w("TextView", value + " could not be autofilled into " + this);
			return;
		}
		setText(value.getTextValue(), mBufferType);
		// Move cursor to the end of the text
		final CharSequence editableText = getText();
		if (editableText != null) {
			setSelection(editableText.length());
		}
	}

	public AutofillValue getAutofillValue() {
		return this instanceof EditText ? AutofillValue.forText(getText()) : null;
	}

	/* AOSP TextView.onEditorAction(): the OnEditorActionListener first; without one or if it did not take it, the
	 * default of the action: NEXT/PREVIOUS move the focus (IME_ACTION_DONE has nothing to hide here: ATL shows no
	 * soft keyboard) */
	public void onEditorAction(int actionCode) {
		if (mOnEditorActionListener != null && mOnEditorActionListener.onEditorAction(this, actionCode, null)) {
			return;
		}
		if (actionCode == 5 /* IME_ACTION_NEXT */) {
			View v = focusSearch(View.FOCUS_FORWARD);
			if (v != null && !v.requestFocus(View.FOCUS_FORWARD)) {
				throw new IllegalStateException("focus search returned a view that wasn't able to take focus!");
			}
		} else if (actionCode == 7 /* IME_ACTION_PREVIOUS */) {
			View v = focusSearch(View.FOCUS_BACKWARD);
			if (v != null && !v.requestFocus(View.FOCUS_BACKWARD)) {
				throw new IllegalStateException("focus search returned a view that wasn't able to take focus!");
			}
		}
	}

	OnEditorActionListener mOnEditorActionListener;

	public void setOnEditorActionListener(TextView.OnEditorActionListener l) {
		mOnEditorActionListener = l;
	}

	/* AOSP TextView.onTextContextMenuItem(): copy, cut, paste and select all of the text and the selection, which
	 * the (here absent) context menu and the keyboard shortcuts use */
	public boolean onTextContextMenuItem(int id) {
		int min = 0;
		int max = getText().length();
		if (isFocused()) {
			final int selStart = getSelectionStart();
			final int selEnd = getSelectionEnd();
			min = Math.max(0, Math.min(selStart, selEnd));
			max = Math.max(0, Math.max(selStart, selEnd));
		}
		android.content.ClipboardManager clipboard = (android.content.ClipboardManager)getContext().getSystemService(Context.CLIPBOARD_SERVICE);
		switch (id) {
		case android.R.id.selectAll:
			if (this instanceof EditText) {
				setSelection(0, getText().length());
				if (mParagraphSelection != null)
					mParagraphSelection.onSelectionChangedByMenu();
				return true;
			}
			if (paragraphSelection() != null) {
				/* AOSP selectAllText(), the Editor's action mode follows */
				setSelection(0, getText().length());
				mParagraphSelection.onSelectionChangedByMenu();
				return true;
			}
			return false;
		case com.android.internal.R.id.shareText:
			if (paragraphSelection() != null || this instanceof EditText) {
				shareSelectedText();
				return true;
			}
			return false;
		case android.R.id.cut:
			if (this instanceof EditText && clipboard != null) {
				clipboard.setPrimaryClip(android.content.ClipData.newPlainText(null, getTransformedText(min, max)));
				deleteText_internal(min, max);
				/* AOSP: the cut ends the text action mode */
				if (mParagraphSelection != null)
					mParagraphSelection.stopTextActionMode();
				return true;
			}
			return false;
		case android.R.id.copy:
			if (clipboard != null && max > min) {
				clipboard.setPrimaryClip(android.content.ClipData.newPlainText(null, getTransformedText(min, max)));
				/* AOSP: the copy ends the text action mode */
				if (mParagraphSelection != null)
					mParagraphSelection.stopTextActionMode();
				return true;
			}
			return false;
		case android.R.id.paste:
			if (this instanceof EditText && clipboard != null && clipboard.hasPrimaryClip()) {
				android.content.ClipData clip = clipboard.getPrimaryClip();
				StringBuilder paste = new StringBuilder();
				for (int i = 0; i < clip.getItemCount(); i++) {
					paste.append(clip.getItemAt(i).coerceToText(getContext()));
				}
				replaceText_internal(min, max, paste);
				/* AOSP paste(): the text action mode ends */
				if (mParagraphSelection != null)
					mParagraphSelection.stopTextActionMode();
				return true;
			}
			return false;
		default:
			return false;
		}
	}

	CharSequence getTransformedText(int start, int end) {
		return getText().subSequence(Math.max(0, start), Math.min(getText().length(), Math.max(0, end)));
	}

	/* replace the characters [min, max) of the text by the new ones and put the cursor after them; a label has no
	 * editing, an EditText overrides this with the GtkText's */
	void replaceText_internal(int min, int max, CharSequence newText) {
	}

	void deleteText_internal(int min, int max) {
		replaceText_internal(min, max, "");
	}

	/* AOSP TextView selection: the Selection span of a Spannable text; getSelectionStart() is -1 without one */
	public void setSelection(int start, int stop) {
		if (text instanceof Spannable) {
			Selection.setSelection((Spannable)text, start, stop);
			onSelectionChanged(start, stop);
		}
	}

	public void setSelection(int index) {
		setSelection(index, index);
	}

	public int getSelectionStart() {
		return text instanceof Spannable ? Selection.getSelectionStart(text) : -1;
	}

	public int getSelectionEnd() {
		return text instanceof Spannable ? Selection.getSelectionEnd(text) : -1;
	}

	public boolean hasSelection() {
		final int selectionStart = getSelectionStart();
		final int selectionEnd = getSelectionEnd();
		return selectionStart >= 0 && selectionStart != selectionEnd;
	}

	/* AOSP: TextView.getWordIterator(), for the key listeners' word-wise deletion */
	private android.text.method.WordIterator mWordIterator;

	/** @hide */
	public android.text.method.WordIterator getWordIterator() {
		if (mWordIterator == null) {
			mWordIterator = new android.text.method.WordIterator(getTextLocale());
		}
		return mWordIterator;
	}

	/* ATL has no OffsetMapping (TransformationMethod2 / InsertModeTransformationMethod) */
	public boolean isOffsetMappingAvailable() {
		return false;
	}
}
