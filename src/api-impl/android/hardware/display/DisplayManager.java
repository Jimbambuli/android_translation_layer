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

	public void registerDisplayListener(DisplayListener listener, Handler handler) {
	}

	public void unregisterDisplayListener(DisplayListener listener) {}

	public Display[] getDisplays() {
		return new Display[0];
	}
}
