package android.media.projection;

import android.hardware.display.VirtualDisplay;
import android.os.Handler;
import android.view.Surface;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/* AOSP media/java/android/media/projection/MediaProjection.java. ATL has no media projection service, so
 * MediaProjectionManager.getMediaProjection() never returns an instance; the callbacks are kept as in
 * AOSP and no virtual display can be created. */
public final class MediaProjection {
	private final Map<Callback, Handler> mCallbacks = new ConcurrentHashMap<>();

	MediaProjection() {
	}

	public void registerCallback(Callback callback, Handler handler) {
		final Callback c = Objects.requireNonNull(callback);
		if (handler == null)
			handler = new Handler();
		mCallbacks.put(c, handler);
	}

	public void unregisterCallback(Callback callback) {
		final Callback c = Objects.requireNonNull(callback);
		mCallbacks.remove(c);
	}

	/* AOSP: DisplayManager.createVirtualDisplay() returns null when the display cannot be created */
	public VirtualDisplay createVirtualDisplay(String name, int width, int height, int dpi, int flags,
	                                           Surface surface, VirtualDisplay.Callback callback, Handler handler) {
		return null;
	}

	public void stop() {
	}

	/* AOSP MediaProjection.Callback */
	public abstract static class Callback {
		public void onStop() {}

		public void onCapturedContentResize(int width, int height) {}

		public void onCapturedContentVisibilityChanged(boolean isVisible) {}
	}
}
