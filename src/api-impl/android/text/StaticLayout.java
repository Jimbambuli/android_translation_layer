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

		public static Builder obtain(CharSequence source, int bufstart, int bufend, TextPaint paint, int outerwidth) {
			Builder builder = new Builder();
			builder.layout = new StaticLayout(source, bufstart, bufend, paint, outerwidth, null, null, 1.0f, 0, false, null, 0, 0);
			return builder;
		}

		public Builder setTextDirection(TextDirectionHeuristic textDir) { return this; }

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

		public Builder setIncludePad(boolean includepad) { return this; }

		/* Pango has a single (greedy) line breaker */
		public Builder setBreakStrategy(int strategy) { return this; }

		/* Pango has no hyphenation dictionary */
		public Builder setHyphenationFrequency(int hyphenationFrequency) { return this; }

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
			layout.setMaxLines(maxLines, ellipsize);
			return layout;
		}
	}

	/* API 33 */
	public boolean isFallbackLineSpacingEnabled() {
		return false;
	}
}
