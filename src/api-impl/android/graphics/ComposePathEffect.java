package android.graphics;

/* AOSP graphics/java/android/graphics/ComposePathEffect.java creates a native SkPathEffect;
 * ATL's Paint applies only DashPathEffect, other effects are kept but not drawn
 * (as with CornerPathEffect). */
public class ComposePathEffect extends PathEffect {
	public ComposePathEffect(PathEffect outerpe, PathEffect innerpe) {
		super();
	}
}
