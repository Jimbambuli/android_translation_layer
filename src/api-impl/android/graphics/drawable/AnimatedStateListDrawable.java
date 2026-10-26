package android.graphics.drawable;

import android.content.res.Resources;

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
		this(null, null);
	}

	public void addState(int[] stateSet, Drawable drawable, int id) {
		addState(stateSet, drawable);
	}

	public <T extends Drawable & Animatable> void addTransition(int fromId, int toId, T transition, boolean reversible) {}

	static class AnimatedStateListState extends StateListState {
		AnimatedStateListState(AnimatedStateListState orig, AnimatedStateListDrawable owner, Resources res) {
			super(orig, owner, res);
		}

		@Override
		public Drawable newDrawable() {
			return new AnimatedStateListDrawable(this, null);
		}

		@Override
		public Drawable newDrawable(Resources res) {
			return new AnimatedStateListDrawable(this, res);
		}
	}

	private AnimatedStateListDrawable(AnimatedStateListState state, Resources res) {
		super(null);

		// Every animated state list drawable has its own constant state.
		final AnimatedStateListState newState = new AnimatedStateListState(state, this, res);
		setConstantState(newState);
		onStateChange(getState());
		jumpToCurrentState();
	}
}
