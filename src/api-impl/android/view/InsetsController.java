package android.view;

import android.view.WindowInsets.Type.InsetsType;

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

	public void show(@InsetsType int types) {
		window.setInsetsVisible(types, true);
	}

	public void hide(@InsetsType int types) {
		window.setInsetsVisible(types, false);
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
