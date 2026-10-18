package android.graphics.drawable;

/**
 * A StateListDrawable that may animate between its states.
 *
 * Transitions are not supported: they are accepted, but the drawable switches
 * to the item for the new state right away, like a plain StateListDrawable.
 * This is what an &lt;animated-selector&gt; (e.g. the material radio button and
 * check box) is inflated to.
 */
public class AnimatedStateListDrawable extends StateListDrawable {

	public AnimatedStateListDrawable() {
		super();
	}

	public void addState(int[] stateSet, Drawable drawable, int id) {
		addState(stateSet, drawable);
	}

	public <T extends Drawable & Animatable> void addTransition(int fromId, int toId, T transition, boolean reversible) {}
}
