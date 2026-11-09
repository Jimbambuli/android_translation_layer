package android.text;

public class StaticLayout extends Layout {

	public StaticLayout(CharSequence source, int bufstart, int bufend,
	                    TextPaint paint, int outerwidth,
	                    Alignment align, TextDirectionHeuristic textDir,
	                    float spacingmult, float spacingadd,
	                    boolean includepad,
	                    TextUtils.TruncateAt ellipsize, int ellipsizedWidth, int maxLines) {
		super(source, paint, outerwidth, align, spacingmult, spacingadd);
	}

	public StaticLayout(CharSequence source, TextPaint paint, int outerwidth, Alignment align, float spacingmult, float spacingadd, boolean includepad) {
		super(source, paint, outerwidth, align, spacingmult, spacingadd);
	}

	public StaticLayout(CharSequence source, int start, int end, TextPaint paint, int outerwidth, Alignment align, float spacingmult, float spacingadd, boolean includepad, TextUtils.TruncateAt ellipsize, int ellipsizedWidth) {
		super(source, paint, outerwidth, align, spacingmult, spacingadd);
		if (ellipsize != null)
			native_set_ellipsize(layout, ellipsize == null ? 0 : ellipsize.ordinal() + 1, ellipsizedWidth);
	}

	public StaticLayout(CharSequence source, int start, int end, TextPaint paint, int outerwidth, Alignment align, float spacingmult, float spacingadd, boolean includepad) {
		super(source, paint, outerwidth, align, spacingmult, spacingadd);
	}

	public static class Builder {
		private StaticLayout layout;
		private int maxLines = Integer.MAX_VALUE;
		private TextUtils.TruncateAt ellipsize;
		private float spacingAdd = 0.0f;
		private float spacingMult = 1.0f;
		private int justificationMode = 0; // JUSTIFICATION_MODE_NONE
		private int[] leftIndents;
		private int hyphenationFrequency = -1; // not set: Pango's own default
		private int breakStrategy = 0; // BREAK_STRATEGY_SIMPLE
		private boolean includePad = true;
		private boolean useBoundsForWidth = false;
		private TextDirectionHeuristic textDir;

		public static Builder obtain(CharSequence source, int bufstart, int bufend, TextPaint paint, int outerwidth) {
			Builder builder = new Builder();
			builder.layout = new StaticLayout(source, bufstart, bufend, paint, outerwidth, null, null, 1.0f, 0, false, null, 0, 0);
			return builder;
		}

		/* AOSP: the heuristic that finds the paragraph direction. Pango finds it itself from the first
		 * strong character (auto direction, = FIRSTSTRONG_LTR) and has a base direction only per
		 * context, which every layout of the app shares, so the heuristic is kept and not applied */
		public Builder setTextDirection(TextDirectionHeuristic textDir) {
			this.textDir = textDir;
			return this;
		}

		public Builder setAlignment(Alignment align) {
			layout.setAlignment(align);
			return this;
		}

		public Builder setMaxLines(int maxLines) {
			this.maxLines = maxLines;
			return this;
		}

		public Builder setEllipsize(TextUtils.TruncateAt ellipsize) {
			this.ellipsize = ellipsize;
			return this;
		}

		/* Pango ellipsizes at the layout width */
		public Builder setEllipsizedWidth(int ellipsizedWidth) { return this; }

		public Builder setLineSpacing(float add, float mult) {
			spacingAdd = add;
			spacingMult = mult;
			return this;
		}

		/* AOSP adds the space between the font's top/bottom and its ascent/descent above the first and below
		 * the last line; Pango's lines are the logical extents of the font, with that space (the paddings
		 * Layout.getTopPadding()/getBottomPadding() answer are fixed), so the value is kept and not applied */
		public Builder setIncludePad(boolean includepad) {
			this.includePad = includepad;
			return this;
		}

		/* Pango has a single (greedy) line breaker = BREAK_STRATEGY_SIMPLE; balanced and high quality break
		 * the same way, the value is kept */
		public Builder setBreakStrategy(int strategy) {
			this.breakStrategy = strategy;
			return this;
		}

		/* HYPHENATION_FREQUENCY_NONE: no hyphen where a word is broken (Layout.setHyphenation()); Pango has no
		 * hyphenation dictionary, normal and full leave Pango's hyphens */
		public Builder setHyphenationFrequency(int hyphenationFrequency) {
			this.hyphenationFrequency = hyphenationFrequency;
			return this;
		}

		/* only the left indents: Pango indents the first line or all others (hanging) */
		public Builder setIndents(int[] indents, int[] widths) {
			leftIndents = indents;
			return this;
		}

		public Builder setJustificationMode(int mode) {
			justificationMode = mode;
			return this;
		}

		public Builder setUseLineSpacingFromFallbacks(boolean useLineSpacingFromFallbacks) { return this; }

		/* Android 15 (a @FlaggedApi in android-14.0.0_r75, which has it): the width of the layout from the bounding
		 * boxes of the glyphs instead of their advances. The flag is kept and reported by Layout.getUseBoundsForWidth();
		 * Pango's lines are still measured by advance. */
		public Builder setUseBoundsForWidth(boolean useBoundsForWidth) {
			this.useBoundsForWidth = useBoundsForWidth;
			return this;
		}

		public boolean getUseBoundsForWidth() {
			return useBoundsForWidth;
		}

		public Builder setLineBreakConfig(android.graphics.text.LineBreakConfig lineBreakConfig) { return this; }

		/* the layout exists since obtain(); apply what the setters collected, in the order AOSP's
		 * StaticLayout lays out: spacing, justification and indents first, the line limit last */
		public StaticLayout build() {
			int first = 0, rest = 0;
			if (leftIndents != null && leftIndents.length > 0) {
				first = leftIndents[0];
				rest = leftIndents.length > 1 ? leftIndents[1] : first;
			}
			layout.setParagraphStyle(spacingMult, spacingAdd, justificationMode == 1, first, rest);
			if (hyphenationFrequency >= 0)
				layout.setHyphenation(hyphenationFrequency != 0);
			layout.setMaxLines(maxLines, ellipsize);
			layout.mUseBoundsForWidth = useBoundsForWidth;
			return layout;
		}
	}

	/* API 33 */
	public boolean isFallbackLineSpacingEnabled() {
		return false;
	}
}
