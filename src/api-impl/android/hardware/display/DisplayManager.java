package android.hardware.display;

import android.os.Handler;
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

	/* AOSP DisplayManagerGlobal.registerDisplayListener(): the listener is kept until it is unregistered; ATL has
	 * one display that is never added or removed, so no event is sent */
	private final java.util.ArrayList<DisplayListener> mDisplayListeners = new java.util.ArrayList<>();

	public void registerDisplayListener(DisplayListener listener, Handler handler) {
		if (listener == null) {
			throw new IllegalArgumentException("listener must not be null");
		}
		synchronized (mDisplayListeners) {
			if (!mDisplayListeners.contains(listener))
				mDisplayListeners.add(listener);
		}
	}

	/* AOSP DisplayManagerGlobal.unregisterDisplayListener() */
	public void unregisterDisplayListener(DisplayListener listener) {
		if (listener == null) {
			throw new IllegalArgumentException("listener must not be null");
		}
		synchronized (mDisplayListeners) {
			mDisplayListeners.remove(listener);
		}
	}

	/* AOSP DisplayManager.registerDisplayListener(Executor, long, DisplayListener): kept like the Handler variant;
	 * ATL sends no display events, so the event filter and the executor are not needed */
	public void registerDisplayListener(java.util.concurrent.Executor executor, long eventFilter, DisplayListener listener) {
		registerDisplayListener(listener, null);
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
}
