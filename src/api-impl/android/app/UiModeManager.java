package android.app;

import android.content.Context;
import android.content.res.Configuration;

public class UiModeManager {

	public int getCurrentModeType() {
		return Context.sys_config.uiMode & Configuration.UI_MODE_TYPE_MASK;
	}

	public int getNightMode() {
		return Context.sys_config.uiMode & Configuration.UI_MODE_NIGHT_MASK;
	}

	/* the contrast of the desktop (the contrast setting of the XDG settings portal): 0 for the
	 * default, 1 for high contrast, which on Android is the highest level of the contrast setting */
	static float sContrast = 0;

	/**
	 * Returns the color contrast for the user, from -1 (lowest) to 1 (highest), 0 being the default.
	 */
	public float getContrast() {
		return sContrast;
	}
}
