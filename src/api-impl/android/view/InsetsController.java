package android.view;

import android.os.CancellationSignal;
import android.view.WindowInsets.Type.InsetsType;
import android.view.animation.Interpolator;

/* there are no system bars to hide on the desktop; the controller keeps their requested visibility
 * so that the window's insets report it, and dispatches the insets again when it changes, as AOSP does */
public class InsetsController implements WindowInsetsController {
	private final Window window;
	private int behavior = BEHAVIOR_DEFAULT;
	private int appearance;

	public InsetsController(Window window) {
		this.window = window;
	}

	public void setSystemBarsAppearance(int appearance, int mask) {
		this.appearance = (this.appearance & ~mask) | (appearance & mask);
	}

	public int getSystemBarsAppearance() {
		return appearance;
	}

	public void setSystemBarsBehavior(int behavior) {
		this.behavior = behavior;
	}

	public int getSystemBarsBehavior() {
		return behavior;
	}

	/* AOSP InsetsController.show()/hide(): the IME is requested from InputMethodManager for the focused editor
	 * of the window (ImeInsetsSourceConsumer.requestShow()) or hidden for the window; its visibility in the
	 * insets is what the keyboard does (Window.getWindowInsets()) */
	public void show(@InsetsType int types) {
		if ((types & WindowInsets.Type.ime()) != 0) {
			View focus = window.getDecorView().findFocus();
			if (focus != null && focus.onCheckIsTextEditor())
				getImm().showSoftInput(focus, 0);
		}
		window.setInsetsVisible(types & ~WindowInsets.Type.ime(), true);
	}

	public void hide(@InsetsType int types) {
		if ((types & WindowInsets.Type.ime()) != 0)
			getImm().hideSoftInputFromWindow(window.getDecorView().getWindowToken(), 0);
		window.setInsetsVisible(types & ~WindowInsets.Type.ime(), false);
	}

	private android.view.inputmethod.InputMethodManager getImm() {
		return (android.view.inputmethod.InputMethodManager)window.getContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
	}

	/* AOSP InsetsController.controlWindowInsetsAnimation(): uncontrollable types (and no types) cancel the request
	 * with a null controller; on the desktop no type is controllable (see below) */
	@Override
	public void controlWindowInsetsAnimation(@InsetsType int types, long durationMillis, Interpolator interpolator,
	                                         CancellationSignal cancellationSignal, WindowInsetsAnimationControlListener listener) {
		listener.onCancelled(null);
	}

	/* like AOSP, the listener learns the controllable types at once; a desktop window has no
	 * system bars or IME insets an app could animate, so there are none and they never change */
	@Override
	public void addOnControllableInsetsChangedListener(OnControllableInsetsChangedListener listener) {
		listener.onControllableInsetsChanged(this, 0);
	}

	@Override
	public void removeOnControllableInsetsChangedListener(OnControllableInsetsChangedListener listener) {}
}
