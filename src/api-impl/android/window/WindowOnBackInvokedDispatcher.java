package android.window;

import java.util.ArrayList;

/**
 * The dispatcher of a window. There is no predictive back gesture, so a back
 * event goes straight to onBackInvoked() of the topmost callback: the one with
 * the highest priority, and among equal priorities the most recently
 * registered one, as in AOSP.
 */
public class WindowOnBackInvokedDispatcher implements OnBackInvokedDispatcher {
	private final ArrayList<OnBackInvokedCallback> callbacks = new ArrayList<>();
	private final ArrayList<Integer> priorities = new ArrayList<>();
	/* PRIORITY_SYSTEM_NAVIGATION_OBSERVER: on API level 36 one callback, which only observes a system back navigation */
	private OnBackInvokedCallback systemNavigationObserverCallback;

	@Override
	public void registerOnBackInvokedCallback(int priority, OnBackInvokedCallback callback) {
		if (priority < 0 && priority != PRIORITY_SYSTEM_NAVIGATION_OBSERVER)
			throw new IllegalArgumentException("Application registered OnBackInvokedCallback cannot have negative priority. Priority: " + priority);
		java.util.Objects.requireNonNull(callback);
		unregisterOnBackInvokedCallback(callback);
		if (priority == PRIORITY_SYSTEM_NAVIGATION_OBSERVER) {
			systemNavigationObserverCallback = callback;
			return;
		}
		callbacks.add(callback);
		priorities.add(priority);
	}

	@Override
	public void unregisterOnBackInvokedCallback(OnBackInvokedCallback callback) {
		if (systemNavigationObserverCallback == callback)
			systemNavigationObserverCallback = null;
		int i = callbacks.indexOf(callback);
		if (i >= 0) {
			callbacks.remove(i);
			priorities.remove(i);
		}
	}

	/** @hide */
	public boolean hasCallbacks() {
		return !callbacks.isEmpty();
	}

	/**
	 * @hide
	 * Invokes the topmost callback. Returns false when there is none, and the
	 * caller falls back to the classic onBackPressed(), the system back navigation,
	 * which the system navigation observer callback observes (AOSP dispatchOnBackInvoked()).
	 */
	public boolean dispatchOnBackInvoked() {
		int top = -1;
		for (int i = 0; i < callbacks.size(); i++) {
			if (top < 0 || priorities.get(i) >= priorities.get(top))
				top = i;
		}
		if (top < 0) {
			if (systemNavigationObserverCallback != null)
				systemNavigationObserverCallback.onBackInvoked();
			return false;
		}
		callbacks.get(top).onBackInvoked();
		return true;
	}
}
