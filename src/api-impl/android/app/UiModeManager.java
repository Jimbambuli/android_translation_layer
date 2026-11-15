package android.app;

import android.content.Context;
import android.content.res.Configuration;
import android.util.ArrayMap;
import java.util.Objects;
import java.util.concurrent.Executor;

public class UiModeManager {

	public int getCurrentModeType() {
		return Context.sys_config.uiMode & Configuration.UI_MODE_TYPE_MASK;
	}

	public int getNightMode() {
		return Context.sys_config.uiMode & Configuration.UI_MODE_NIGHT_MASK;
	}

	public static final int MODE_NIGHT_AUTO = 0;
	public static final int MODE_NIGHT_NO = 1;
	public static final int MODE_NIGHT_YES = 2;
	public static final int MODE_NIGHT_CUSTOM = 3;

	/* AOSP UiModeManagerService DEFAULT_CUSTOM_NIGHT_START_TIME/END_TIME, what the custom night mode (MODE_NIGHT_CUSTOM)
	 * switches at; ATL follows the desktop's night mode and never sets another */
	public java.time.LocalTime getCustomNightModeStart() {
		return java.time.LocalTime.of(22, 0);
	}

	public java.time.LocalTime getCustomNightModeEnd() {
		return java.time.LocalTime.of(6, 0);
	}

	/* AOSP asks the system service, which needs MODIFY_DAY_NIGHT_MODE for the whole device; ATL follows
	 * the night mode of the desktop and has nothing to change */
	public void setNightMode(int mode) {}

	/* AOSP stores a night mode for this app only and restarts its activities with it; ATL keeps following
	 * the desktop (the app's own theme switch then has no effect, but it does not crash) */
	public void setApplicationNightMode(int mode) {}

	/* the contrast of the desktop (the contrast setting of the XDG settings portal): 0 for the
	 * default, 1 for high contrast, which on Android is the highest level of the contrast setting */
	static float sContrast = 0;

	/**
	 * Returns the color contrast for the user, from -1 (lowest) to 1 (highest), 0 being the default.
	 */
	public float getContrast() {
		return sContrast;
	}

	/* AOSP UiModeManager.ContrastChangeListener */
	public interface ContrastChangeListener {
		void onContrastChanged(float contrast);
	}

	/* AOSP keeps the listeners per UiModeManager and the service calls each of them back; ATL has no
	 * service, the desktop contrast arrives in ActivityThread.setNightMode() */
	private static final ArrayMap<ContrastChangeListener, Executor> sContrastChangeListeners = new ArrayMap<>();

	public void addContrastChangeListener(Executor executor, ContrastChangeListener listener) {
		Objects.requireNonNull(executor);
		Objects.requireNonNull(listener);
		synchronized (sContrastChangeListeners) {
			sContrastChangeListeners.put(listener, executor);
		}
	}

	public void removeContrastChangeListener(ContrastChangeListener listener) {
		Objects.requireNonNull(listener);
		synchronized (sContrastChangeListeners) {
			sContrastChangeListeners.remove(listener);
		}
	}

	/* AOSP mCallback.notifyContrastChanged() */
	static void notifyContrastChanged(final float contrast) {
		final ArrayMap<ContrastChangeListener, Executor> listeners;
		synchronized (sContrastChangeListeners) {
			if (Math.abs(sContrast - contrast) < 1e-10)
				return;
			sContrast = contrast;
			listeners = new ArrayMap<>(sContrastChangeListeners);
		}
		for (int i = 0; i < listeners.size(); i++) {
			final ContrastChangeListener listener = listeners.keyAt(i);
			listeners.valueAt(i).execute(new Runnable() {
				@Override
				public void run() {
					listener.onContrastChanged(contrast);
				}
			});
		}
	}
}
