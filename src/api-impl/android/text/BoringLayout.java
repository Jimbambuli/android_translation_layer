package android.text;

public class BoringLayout extends Layout {

	public BoringLayout(CharSequence source, TextPaint paint, int outerwidth, Layout.Alignment align, float spacingMult, float spacingAdd, BoringLayout.Metrics metrics, boolean includePad) {
		super(source, paint, outerwidth, align, spacingMult, spacingAdd);
	}

	/* API 33 */
	public BoringLayout(CharSequence source, TextPaint paint, int outerWidth, Layout.Alignment align, float spacingMult, float spacingAdd, BoringLayout.Metrics metrics, boolean includePad, TextUtils.TruncateAt ellipsize, int ellipsizedWidth, boolean useFallbackLineSpacing) {
		this(source, paint, outerWidth, align, spacingMult, spacingAdd, metrics, includePad);
	}

	public boolean isFallbackLineSpacingEnabled() {
		return false;
	}

	public static class Metrics {};

	public static Metrics isBoring(CharSequence source, TextPaint paint, Metrics metrics) {
		return metrics;
	}

	/* API 33 */
	public static Metrics isBoring(CharSequence source, TextPaint paint, TextDirectionHeuristic textDir, boolean useFallbackLineSpacing, Metrics metrics) {
		return isBoring(source, paint, metrics);
	}

	public static Metrics isBoring(CharSequence source, TextPaint paint) {
		return null;
	}
}
