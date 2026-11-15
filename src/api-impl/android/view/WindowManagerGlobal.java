package android.view;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

public class WindowManagerGlobal {

	private static final WindowManagerGlobal sDefaultWindowManager = new WindowManagerGlobal();

	private final Object mLock = new Object();
	/* AOSP mViews: the root views of the windows added with WindowManager.addView() */
	private final ArrayList<View> mViews = new ArrayList<>();
	/* AOSP mWindowViewsListenerGroup: listener -> executor */
	private final Map<Consumer<List<View>>, Executor> mWindowViewsListeners = new LinkedHashMap<>();

	public static WindowManagerGlobal getInstance() {
		return sDefaultWindowManager;
	}

	/* AOSP WindowManagerGlobal.getWindowViews() */
	public ArrayList<View> getWindowViews() {
		synchronized (mLock) {
			return new ArrayList<>(mViews);
		}
	}

	/* AOSP addWindowViewsListener(): the current value is delivered at once on the executor; a listener
	 * registered before is left alone */
	public void addWindowViewsListener(Executor executor, Consumer<List<View>> consumer) {
		List<View> current;
		synchronized (mLock) {
			if (mWindowViewsListeners.containsKey(consumer))
				return;
			mWindowViewsListeners.put(consumer, executor);
			current = new ArrayList<>(mViews);
		}
		executor.execute(() -> consumer.accept(current));
	}

	public void removeWindowViewsListener(Consumer<List<View>> consumer) {
		synchronized (mLock) {
			mWindowViewsListeners.remove(consumer);
		}
	}

	void viewAdded(View view) {
		synchronized (mLock) {
			mViews.add(view);
		}
		notifyWindowViewsListeners();
	}

	void viewRemoved(View view) {
		synchronized (mLock) {
			mViews.remove(view);
		}
		notifyWindowViewsListeners();
	}

	private void notifyWindowViewsListeners() {
		Map<Consumer<List<View>>, Executor> listeners;
		List<View> views;
		synchronized (mLock) {
			listeners = new LinkedHashMap<>(mWindowViewsListeners);
			views = new ArrayList<>(mViews);
		}
		for (Map.Entry<Consumer<List<View>>, Executor> e : listeners.entrySet())
			e.getValue().execute(() -> e.getKey().accept(views));
	}
}
