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

	/* AOSP DisplayManager.getDisplays(): all logical displays; ATL has the default display only */
	public Display[] getDisplays() {
		return new Display[] {getDisplay(Display.DEFAULT_DISPLAY)};
	}
}
