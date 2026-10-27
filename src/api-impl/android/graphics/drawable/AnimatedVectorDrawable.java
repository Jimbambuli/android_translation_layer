package android.graphics.drawable;

public class AnimatedVectorDrawable extends Drawable implements Animatable2 {

	public void registerAnimationCallback(Animatable2.AnimationCallback callback) {}

	public boolean unregisterAnimationCallback(Animatable2.AnimationCallback callback) {
		return false;
	}

	public void start() {}

	public void stop() {}

	/**
	 * Reverses ongoing animations or starts pending animations in reverse.
	 * <p>
	 * NOTE: Only works if all animations support reverse. Otherwise, this will
	 * do nothing.
	 * @hide
	 */
	public void reverse() {}

	/**
	 * @hide
	 */
	public boolean canReverse() {
		return false;
	}
}
