package android.hardware.display;

import android.view.Display;
import android.view.Surface;

/* AOSP core/java/android/hardware/display/VirtualDisplay.java. ATL's display service creates no virtual
 * displays (MediaProjection never exists, see MediaProjectionManager.getMediaProjection()), so no instance
 * is handed out; the members keep AOSP's local state without DisplayManagerGlobal. */
public final class VirtualDisplay {
	private final Display mDisplay;
	private Object mToken;
	private Surface mSurface;

	VirtualDisplay(Display display, Object token, Surface surface) {
		mDisplay = display;
		mToken = token;
		mSurface = surface;
	}

	public Display getDisplay() {
		return mDisplay;
	}

	public Surface getSurface() {
		return mSurface;
	}

	public void setSurface(Surface surface) {
		if (mSurface != surface)
			mSurface = surface;
	}

	public void resize(int width, int height, int densityDpi) {
	}

	public void release() {
		mToken = null;
	}

	@Override
	public String toString() {
		return "VirtualDisplay{display=" + mDisplay + ", token=" + mToken + ", surface=" + mSurface + "}";
	}

	/* AOSP VirtualDisplay.Callback */
	public static abstract class Callback {
		public void onPaused() {}

		public void onResumed() {}

		public void onStopped() {}
	}
}
