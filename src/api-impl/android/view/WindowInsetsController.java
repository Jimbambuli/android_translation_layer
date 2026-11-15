package android.view;

import android.os.CancellationSignal;
import android.view.WindowInsets.Type.InsetsType;
import android.view.animation.Interpolator;

public interface WindowInsetsController {
	public static final int BEHAVIOR_DEFAULT = 1;
	public static final int BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE = 2;

	void setSystemBarsAppearance(int appearance, int mask);
	int getSystemBarsAppearance();
	void setSystemBarsBehavior(int behavior);
	int getSystemBarsBehavior();
	void show(@InsetsType int types);
	void hide(@InsetsType int types);

	/**
	 * Adds a {@link OnControllableInsetsChangedListener} to the window insets controller.
	 */
	/** AOSP: lets the app animate the given insets types itself */
	void controlWindowInsetsAnimation(@InsetsType int types, long durationMillis, Interpolator interpolator,
	                                  CancellationSignal cancellationSignal, WindowInsetsAnimationControlListener listener);

	void addOnControllableInsetsChangedListener(OnControllableInsetsChangedListener listener);

	/**
	 * Removes a {@link OnControllableInsetsChangedListener} from the window insets controller.
	 */
	void removeOnControllableInsetsChangedListener(OnControllableInsetsChangedListener listener);

	/**
	 * Listener to be notified when the set of controllable {@link WindowInsets.Type} controlled by
	 * a {@link WindowInsetsController} changes.
	 */
	interface OnControllableInsetsChangedListener {
		/**
		 * Called when the set of controllable {@link WindowInsets.Type} changes.
		 *
		 * @param controller The controller for which the set of controllable
		 *                   {@link WindowInsets.Type}s are changing.
		 * @param typeMask   Bitwise type-mask of the {@link WindowInsets.Type}s the controller is
		 *                   currently able to control.
		 */
		void onControllableInsetsChanged(WindowInsetsController controller, @InsetsType int typeMask);
	}
}
