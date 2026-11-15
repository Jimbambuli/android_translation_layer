package android.app;

import android.annotation.UnsupportedAppUsage;

public class StatusBarManager {
	/**
	 * While not being part of the official Android API, some apps use this method to open the notifications panel.
	 */
	@UnsupportedAppUsage
	public void expandNotificationsPanel() {}

	/**
	 * While not being part of the official Android API, some apps use this method to open the settings panel.
	 */
	@UnsupportedAppUsage
	public void expandSettingsPanel() {}

	/**
	 * While not being part of the official Android API, some apps use this method to hide the notification/settings panel.
	 */
	@UnsupportedAppUsage
	public void collapsePanels() {}

	/* AOSP: asks the status bar service to bind the TileService for an update; ATL has no quick settings */
	public void requestTileServiceListeningState(android.content.ComponentName componentName) {
		java.util.Objects.requireNonNull(componentName);
	}
}
