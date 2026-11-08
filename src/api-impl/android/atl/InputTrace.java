package android.atl;

import android.atl.annotation.Export;
import android.view.View;
import android.widget.TextView;

/* Hooks for tools that follow what the user does (atl-doctor's agent puts a breakpoint on each method). The methods
 * are empty on purpose: ATL calls them where it hands GTK input to the app, and only for input from the user, not
 * for what the app does by itself (callOnClick(), setText(), ...). */
public final class InputTrace {
	/* userTouch() actions */
	public static final int CLICK = 0;
	public static final int LONG_CLICK = 1;

	private InputTrace() {}

	/* the user clicked (or long-clicked) target */
	@Export
	public static void userTouch(View target, int action) {}

	/* the user changed the text of v, which has len characters now */
	@Export
	public static void textChanged(TextView v, int len) {}

	/* a key (KeyEvent code and action) that reached the window of the shown activity, including BACK */
	@Export
	public static void key(int keyCode, int action) {}

	/* the user turned the wheel or the touchpad over v: dx, dy in pixels (a wheel step is 100), positive dy scrolls the content up */
	@Export
	public static void scroll(View v, int dx, int dy) {}
}
