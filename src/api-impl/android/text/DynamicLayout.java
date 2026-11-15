package android.text;

import android.text.style.UpdateLayout;

/* AOSP core/java/android/text/DynamicLayout.java: a layout of a text that may change; it follows the changes of the
 * text and of its spans. AOSP reflows only the paragraphs around the change (StaticLayout per block, blocks kept in
 * mObjects/mInts); ATL's Pango layout has no incremental update, so the whole text is laid out again on each
 * change (Layout.reflow()). The block-index methods (@hide) are not here. */
public class DynamicLayout extends Layout {

	private CharSequence mBase;
	private CharSequence mDisplay;
	private ChangeWatcher mWatcher;
	private int mEllipsizedWidth;
	private TextUtils.TruncateAt mEllipsize;
	private boolean mIncludePad;

	public DynamicLayout(CharSequence base, TextPaint paint, int width, Alignment align,
	                     float spacingmult, float spacingadd, boolean includepad) {
		this(base, base, paint, width, align, spacingmult, spacingadd, includepad);
	}

	public DynamicLayout(CharSequence base, CharSequence display, TextPaint paint, int width, Alignment align,
	                     float spacingmult, float spacingadd, boolean includepad) {
		this(base, display, paint, width, align, spacingmult, spacingadd, includepad, null, 0);
	}

	public DynamicLayout(CharSequence base, CharSequence display, TextPaint paint, int width, Alignment align,
	                     float spacingmult, float spacingadd, boolean includepad,
	                     TextUtils.TruncateAt ellipsize, int ellipsizedWidth) {
		this(base, display, paint, width, align, TextDirectionHeuristics.FIRSTSTRONG_LTR,
		     spacingmult, spacingadd, includepad, 0, 0, 0, Integer.MAX_VALUE, ellipsize, ellipsizedWidth);
	}

	public DynamicLayout(CharSequence base, CharSequence display, TextPaint paint, int width, Alignment align,
	                     TextDirectionHeuristic textDir, float spacingmult, float spacingadd, boolean includepad,
	                     int breakStrategy, int hyphenationFrequency, int justificationMode, int maxLines,
	                     TextUtils.TruncateAt ellipsize, int ellipsizedWidth) {
		super(display, paint, width, align, spacingmult, spacingadd);
		mBase = base;
		mDisplay = display;
		mIncludePad = includepad;
		mEllipsize = ellipsize;
		mEllipsizedWidth = ellipsize == null ? width : ellipsizedWidth;
		if (ellipsize != null)
			native_set_ellipsize(layout, ellipsize.ordinal() + 1, ellipsizedWidth);
		if (justificationMode == 1)
			setParagraphStyle(spacingmult, spacingadd, true, 0, 0);
		if (hyphenationFrequency == 0)
			setHyphenation(false);
		if (maxLines != Integer.MAX_VALUE)
			setMaxLines(maxLines, ellipsize);

		/* AOSP: the watcher is a span of the base text; a display text that is not the base is a transformed copy
		 * that the caller keeps up to date itself */
		if (base instanceof Spannable) {
			mWatcher = new ChangeWatcher();
			((Spannable)base).setSpan(mWatcher, 0, base.length(),
			                          Spannable.SPAN_INCLUSIVE_INCLUSIVE | (PRIORITY << Spannable.SPAN_PRIORITY_SHIFT));
		}
	}

	private static final int PRIORITY = 128;

	@Override
	public CharSequence getText() {
		return mDisplay;
	}

	@Override
	public int getEllipsizedWidth() {
		return mEllipsizedWidth;
	}

	private void reflow() {
		reflow(mDisplay);
		if (mEllipsize != null)
			native_set_ellipsize(layout, mEllipsize.ordinal() + 1, mEllipsizedWidth);
	}

	private class ChangeWatcher implements TextWatcher, SpanWatcher {
		@Override
		public void beforeTextChanged(CharSequence s, int where, int before, int after) {
		}

		@Override
		public void onTextChanged(CharSequence s, int where, int before, int after) {
			reflow();
		}

		@Override
		public void afterTextChanged(Editable s) {
		}

		@Override
		public void onSpanAdded(Spannable s, Object o, int start, int end) {
			if (o instanceof UpdateLayout)
				reflow();
		}

		@Override
		public void onSpanRemoved(Spannable s, Object o, int start, int end) {
			if (o instanceof UpdateLayout)
				reflow();
		}

		@Override
		public void onSpanChanged(Spannable s, Object o, int start, int end, int nstart, int nend) {
			if (o instanceof UpdateLayout)
				reflow();
		}
	}

	/* AOSP DynamicLayout.Builder */
	public static final class Builder {
		private CharSequence mBase;
		private CharSequence mDisplay;
		private TextPaint mPaint;
		private int mWidth;
		private Alignment mAlignment = Alignment.ALIGN_NORMAL;
		private TextDirectionHeuristic mTextDir = TextDirectionHeuristics.FIRSTSTRONG_LTR;
		private float mSpacingMult = 1.0f;
		private float mSpacingAdd = 0.0f;
		private boolean mIncludePad = true;
		private int mBreakStrategy = 0;
		private int mHyphenationFrequency = -1;
		private int mJustificationMode = 0;
		private TextUtils.TruncateAt mEllipsize;
		private int mEllipsizedWidth;

		private Builder(CharSequence base, TextPaint paint, int width) {
			mBase = base;
			mDisplay = base;
			mPaint = paint;
			mWidth = width;
			mEllipsizedWidth = width;
		}

		public static Builder obtain(CharSequence base, TextPaint paint, int width) {
			return new Builder(base, paint, width);
		}

		public Builder setDisplayText(CharSequence display) {
			mDisplay = display;
			return this;
		}

		public Builder setAlignment(Alignment alignment) {
			mAlignment = alignment;
			return this;
		}

		public Builder setTextDirection(TextDirectionHeuristic textDir) {
			mTextDir = textDir;
			return this;
		}

		public Builder setLineSpacing(float spacingAdd, float spacingMult) {
			mSpacingAdd = spacingAdd;
			mSpacingMult = spacingMult;
			return this;
		}

		public Builder setIncludePad(boolean includePad) {
			mIncludePad = includePad;
			return this;
		}

		public Builder setBreakStrategy(int breakStrategy) {
			mBreakStrategy = breakStrategy;
			return this;
		}

		public Builder setHyphenationFrequency(int hyphenationFrequency) {
			mHyphenationFrequency = hyphenationFrequency;
			return this;
		}

		public Builder setJustificationMode(int justificationMode) {
			mJustificationMode = justificationMode;
			return this;
		}

		public Builder setEllipsize(TextUtils.TruncateAt ellipsize) {
			mEllipsize = ellipsize;
			return this;
		}

		public Builder setEllipsizedWidth(int ellipsizedWidth) {
			mEllipsizedWidth = ellipsizedWidth;
			return this;
		}

		public DynamicLayout build() {
			return new DynamicLayout(mBase, mDisplay, mPaint, mWidth, mAlignment, mTextDir, mSpacingMult,
			                         mSpacingAdd, mIncludePad, mBreakStrategy, mHyphenationFrequency,
			                         mJustificationMode, Integer.MAX_VALUE, mEllipsize, mEllipsizedWidth);
		}
	}
}
