package android.hardware.display;

import android.os.Handler;
import android.os.Looper;
import android.view.Display;

public final class DisplayManager {
	/* AOSP DisplayManager.DisplayListener */
	public interface DisplayListener {
		void onDisplayAdded(int displayId);

		void onDisplayRemoved(int displayId);

		void onDisplayChanged(int displayId);
	}

	public Display getDisplay(int dummy) {
		return new Display();
	}

	/* AOSP DisplayManagerGlobal.mDisplayListeners: the listeners of all DisplayManagers with the looper or
	 * executor their events go to (DisplayListenerDelegate); ATL has one display that is never added or
	 * removed, it only changes when the compositor rotates it */
	private static final class Delegate {
		final DisplayListener listener;
		final Handler handler;
		final java.util.concurrent.Executor executor;
		final long eventFilter;

		Delegate(DisplayListener listener, Handler handler, java.util.concurrent.Executor executor, long eventFilter) {
			this.listener = listener;
			this.handler = handler;
			this.executor = executor;
			this.eventFilter = eventFilter;
		}
	}

	/* AOSP DisplayManager.EVENT_TYPE_* */
	public static final long EVENT_TYPE_DISPLAY_ADDED = 1L << 0;
	public static final long EVENT_TYPE_DISPLAY_REMOVED = 1L << 1;
	public static final long EVENT_TYPE_DISPLAY_CHANGED = 1L << 2;
	public static final long EVENT_TYPE_DISPLAY_REFRESH_RATE = 1L << 3;
	public static final long EVENT_TYPE_DISPLAY_STATE = 1L << 4;
	public static final long EVENT_TYPE_DISPLAY_BRIGHTNESS = 1L << 5;

	private static final java.util.ArrayList<Delegate> sDisplayListeners = new java.util.ArrayList<>();

	private static int findDisplayListenerLocked(DisplayListener listener) {
		for (int i = 0; i < sDisplayListeners.size(); i++) {
			if (sDisplayListeners.get(i).listener == listener)
				return i;
		}
		return -1;
	}

	/* AOSP DisplayManager.registerDisplayListener(DisplayListener, Handler): the events go to the handler's
	 * looper, without one to the looper of the calling thread (DisplayManagerGlobal.getLooperForHandler()) */
	public void registerDisplayListener(DisplayListener listener, Handler handler) {
		if (listener == null) {
			throw new IllegalArgumentException("listener must not be null");
		}
		Looper looper = handler != null ? handler.getLooper() : Looper.myLooper();
		if (looper == null)
			looper = Looper.getMainLooper();
		synchronized (sDisplayListeners) {
			if (findDisplayListenerLocked(listener) < 0)
				sDisplayListeners.add(new Delegate(listener, new Handler(looper), null, EVENT_TYPE_DISPLAY_ADDED | EVENT_TYPE_DISPLAY_CHANGED | EVENT_TYPE_DISPLAY_REMOVED));
		}
	}

	/* AOSP DisplayManagerGlobal.unregisterDisplayListener() */
	public void unregisterDisplayListener(DisplayListener listener) {
		if (listener == null) {
			throw new IllegalArgumentException("listener must not be null");
		}
		synchronized (sDisplayListeners) {
			int index = findDisplayListenerLocked(listener);
			if (index >= 0)
				sDisplayListeners.remove(index);
		}
	}

	/* AOSP DisplayManager.registerDisplayListener(Executor, long, DisplayListener): the events of the filter go
	 * to the executor */
	public void registerDisplayListener(java.util.concurrent.Executor executor, long eventFilter, DisplayListener listener) {
		if (listener == null) {
			throw new IllegalArgumentException("listener must not be null");
		}
		if (executor == null) {
			throw new IllegalArgumentException("executor must not be null");
		}
		synchronized (sDisplayListeners) {
			if (findDisplayListenerLocked(listener) < 0)
				sDisplayListeners.add(new Delegate(listener, null, executor, eventFilter));
		}
	}

	/** @hide DisplayManagerGlobal.handleDisplayEvent(displayId, EVENT_DISPLAY_CHANGED) */
	public static void notifyDisplayChanged(final int displayId) {
		Delegate[] delegates;
		synchronized (sDisplayListeners) {
			delegates = sDisplayListeners.toArray(new Delegate[0]);
		}
		for (final Delegate d : delegates) {
			if ((d.eventFilter & EVENT_TYPE_DISPLAY_CHANGED) == 0)
				continue;
			Runnable r = new Runnable() {
				@Override
				public void run() {
					d.listener.onDisplayChanged(displayId);
				}
			};
			if (d.executor != null)
				d.executor.execute(r);
			else
				d.handler.post(r);
		}
	}

	/* AOSP DisplayManager.getDisplays(String): the categories select from the displays; ATL's only display is
	 * the default one and has no presentation or rear role */
	public Display[] getDisplays(String category) {
		if (category == null || "android.hardware.display.category.ALL_INCLUDING_DISABLED".equals(category)
				|| "android.hardware.display.category.BUILT_IN_DISPLAYS".equals(category)) {
			return getDisplays();
		}
		return new Display[0];
	}

	/* AOSP DisplayManager.getDisplays(): all logical displays; ATL has the default display only */
	public Display[] getDisplays() {
		return new Display[] {getDisplay(Display.DEFAULT_DISPLAY)};
	}

	/* AOSP DisplayManager.createVirtualDisplay(): VirtualDisplayConfig.Builder checks the arguments, then
	 * DisplayManagerGlobal.createVirtualDisplayWrapper() returns null when the display service gives no display
	 * id ("Could not create virtual display"); ATL has no display service that creates virtual displays (as
	 * MediaProjection.createVirtualDisplay()) */
	public VirtualDisplay createVirtualDisplay(String name, int width, int height, int densityDpi,
	                                           android.view.Surface surface, int flags) {
		return createVirtualDisplay(name, width, height, densityDpi, surface, flags, null, null);
	}

	public VirtualDisplay createVirtualDisplay(String name, int width, int height, int densityDpi,
	                                           android.view.Surface surface, int flags,
	                                           VirtualDisplay.Callback callback, android.os.Handler handler) {
		if (name == null) {
			throw new IllegalArgumentException("Virtual display name is required");
		}
		if (width <= 0) {
			throw new IllegalArgumentException("Virtual display width must be positive");
		}
		if (height <= 0) {
			throw new IllegalArgumentException("Virtual display height must be positive");
		}
		if (densityDpi <= 0) {
			throw new IllegalArgumentException("Virtual display density must be positive");
		}
		android.util.Log.e("DisplayManager", "Could not create virtual display: " + name);
		return null;
	}

	/* AOSP DisplayManager.createVirtualDisplay(VirtualDisplayConfig, Handler, Callback): same outcome as the
	 * argument variant (the config was already checked by its Builder), no display service -> null */
	public VirtualDisplay createVirtualDisplay(VirtualDisplayConfig config) {
		return createVirtualDisplay(config, null, null);
	}

	public VirtualDisplay createVirtualDisplay(VirtualDisplayConfig config, Handler handler, VirtualDisplay.Callback callback) {
		if (config == null) {
			throw new IllegalArgumentException("config must not be null");
		}
		android.util.Log.e("DisplayManager", "Could not create virtual display: " + config.getName());
		return null;
	}

	/* AOSP DisplayManager.MATCH_CONTENT_FRAMERATE_* */
	public static final int MATCH_CONTENT_FRAMERATE_UNKNOWN = -1;
	public static final int MATCH_CONTENT_FRAMERATE_NEVER = 0;
	public static final int MATCH_CONTENT_FRAMERATE_SEAMLESSS_ONLY = 1;
	public static final int MATCH_CONTENT_FRAMERATE_ALWAYS = 2;

	/* AOSP getMatchContentFrameRateUserPreference(): DisplayManagerService's default switching type is
	 * SWITCHING_TYPE_WITHIN_GROUPS, which toMatchContentFrameRateSetting() maps to SEAMLESSS_ONLY */
	public int getMatchContentFrameRateUserPreference() {
		return MATCH_CONTENT_FRAMERATE_SEAMLESSS_ONLY;
	}
}
