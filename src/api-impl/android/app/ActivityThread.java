package android.app;

import android.app.Application;
import android.atl.ATLLoadedApp;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.content.res.Configuration;
import android.atl.annotation.Export;
import java.util.List;

public class ActivityThread {
	public static ActivityThread currentActivityThread() {
		return new ActivityThread();
	}
	public static String currentPackageName() {
		return ATLLoadedApp.getPrimaryApplication().pkg.packageName;
	}
	public static String currentProcessName() {
		return Application.getProcessName();
	}
	public static Application currentApplication() {
		return ATLLoadedApp.getPrimaryApplication().getApplication();
	}
	public Application getApplication() {
		return ATLLoadedApp.getPrimaryApplication().getApplication();
	}

	public Configuration getConfiguration() {
		return Context.sys_config;
	}

	/* The desktop switched between light and dark (the color-scheme of the XDG settings portal): like
	 * UiModeManagerService.updateConfigurationLocked(), the night bits of the global configuration
	 * change, which goes to the app as in ActivityThread.handleConfigurationChanged() */
	@Export
	static void setNightMode(int uiModeNight, float contrast) {
		UiModeManager.sContrast = contrast;
		Configuration config = new Configuration(Context.sys_config);
		config.uiMode = (config.uiMode & ~Configuration.UI_MODE_NIGHT_MASK) | uiModeNight;
		handleConfigurationChanged(config);
	}

	/* ActivityThread.handleConfigurationChanged(): the Resources take the new configuration
	 * (ResourcesManager.applyConfigurationToResources()) and the application gets onConfigurationChanged().
	 * The activities are not told here: like ActivityRecord.ensureActivityConfiguration(), the shown one is
	 * relaunched or told right away, the others when they come back (Activity.ensureActivityConfiguration()). */
	public static void handleConfigurationChanged(Configuration config) {
		int diff = Context.sys_config.diff(config);
		if (diff == 0)
			return;
		Context.sys_config.setTo(config);
		ATLLoadedApp.applyConfigurationToResources(Context.sys_config);
		Application application = ATLLoadedApp.getPrimaryApplication().getApplication();
		if (application != null)
			application.onConfigurationChanged(new Configuration(Context.sys_config));
	}
}
