package android.view.inspector;

import android.view.View;
import android.view.WindowManagerGlobal;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * android-16 frameworks/base core/java/android/view/inspector/WindowInspector.java. The views are the roots
 * of the windows added through WindowManager.addView() (dialogs, popups, toasts); ATL's activity decor
 * views do not go through addView() and are not in the list.
 */
public final class WindowInspector {

	private WindowInspector() {}

	public static List<View> getGlobalWindowViews() {
		return WindowManagerGlobal.getInstance().getWindowViews();
	}

	public static void addGlobalWindowViewsListener(Executor executor, Consumer<List<View>> consumer) {
		WindowManagerGlobal.getInstance().addWindowViewsListener(executor, consumer);
	}

	public static void removeGlobalWindowViewsListener(Consumer<List<View>> consumer) {
		WindowManagerGlobal.getInstance().removeWindowViewsListener(consumer);
	}
}
