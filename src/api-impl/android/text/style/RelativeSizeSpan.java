package android.text.style;

public class RelativeSizeSpan extends MetricAffectingSpan {
	private final float proportion;

	public RelativeSizeSpan(float proportion) {
		this.proportion = proportion;
	}

	public float getSizeChange() {
		return proportion;
	}
}
