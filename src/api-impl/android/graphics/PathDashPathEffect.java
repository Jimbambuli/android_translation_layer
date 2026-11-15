package android.graphics;

/* AOSP graphics/java/android/graphics/PathDashPathEffect.java creates a native SkPathEffect;
 * ATL's Paint applies only DashPathEffect, other effects are kept but not drawn
 * (as with CornerPathEffect). */
public class PathDashPathEffect extends PathEffect {

	public enum Style {
		TRANSLATE(0), //!< translate the shape to each position
		ROTATE(1),    //!< rotate the shape about its center
		MORPH(2);     //!< transform each point, and turn lines into curves

		Style(int value) {
			native_style = value;
		}
		int native_style;
	}

	/**
	 * Dash the drawn path by stamping it with the specified shape. This only
	 * applies to drawings when the paint's style is STROKE or STROKE_AND_FILL.
	 */
	public PathDashPathEffect(Path shape, float advance, float phase, Style style) {
		super();
	}
}
