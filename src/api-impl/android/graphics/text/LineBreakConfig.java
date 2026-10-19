package android.graphics.text;

/* API 33. Pango breaks the lines, so the configuration is only kept. */
public final class LineBreakConfig {
	public static final int LINE_BREAK_STYLE_NONE = 0;
	public static final int LINE_BREAK_STYLE_LOOSE = 1;
	public static final int LINE_BREAK_STYLE_NORMAL = 2;
	public static final int LINE_BREAK_STYLE_STRICT = 3;
	public static final int LINE_BREAK_WORD_STYLE_NONE = 0;
	public static final int LINE_BREAK_WORD_STYLE_PHRASE = 1;

	private final int lineBreakStyle;
	private final int lineBreakWordStyle;

	private LineBreakConfig(int lineBreakStyle, int lineBreakWordStyle) {
		this.lineBreakStyle = lineBreakStyle;
		this.lineBreakWordStyle = lineBreakWordStyle;
	}

	public int getLineBreakStyle() {
		return lineBreakStyle;
	}

	public int getLineBreakWordStyle() {
		return lineBreakWordStyle;
	}

	public static final class Builder {
		private int lineBreakStyle = LINE_BREAK_STYLE_NONE;
		private int lineBreakWordStyle = LINE_BREAK_WORD_STYLE_NONE;

		public Builder setLineBreakStyle(int lineBreakStyle) {
			this.lineBreakStyle = lineBreakStyle;
			return this;
		}

		public Builder setLineBreakWordStyle(int lineBreakWordStyle) {
			this.lineBreakWordStyle = lineBreakWordStyle;
			return this;
		}

		public LineBreakConfig build() {
			return new LineBreakConfig(lineBreakStyle, lineBreakWordStyle);
		}
	}
}
