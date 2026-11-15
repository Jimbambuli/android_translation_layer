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

	/* AOSP StatusBarManager: result codes of requestAddTileService() */
	public static final int TILE_ADD_REQUEST_RESULT_TILE_NOT_ADDED = 0;
	public static final int TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED = 1;
	public static final int TILE_ADD_REQUEST_RESULT_TILE_ADDED = 2;
	public static final int TILE_ADD_REQUEST_RESULT_DIALOG_DISMISSED = 3;
	private static final int TILE_ADD_REQUEST_FIRST_ERROR_CODE = 1000;
	public static final int TILE_ADD_REQUEST_ERROR_MISMATCHED_PACKAGE = TILE_ADD_REQUEST_FIRST_ERROR_CODE;
	public static final int TILE_ADD_REQUEST_ERROR_REQUEST_IN_PROGRESS = TILE_ADD_REQUEST_FIRST_ERROR_CODE + 1;
	public static final int TILE_ADD_REQUEST_ERROR_BAD_COMPONENT = TILE_ADD_REQUEST_FIRST_ERROR_CODE + 2;
	public static final int TILE_ADD_REQUEST_ERROR_NOT_CURRENT_USER = TILE_ADD_REQUEST_FIRST_ERROR_CODE + 3;
	public static final int TILE_ADD_REQUEST_ERROR_APP_NOT_IN_FOREGROUND = TILE_ADD_REQUEST_FIRST_ERROR_CODE + 4;
	public static final int TILE_ADD_REQUEST_ERROR_NO_STATUS_BAR_SERVICE = TILE_ADD_REQUEST_FIRST_ERROR_CODE + 5;

	private final android.content.Context mContext;

	public StatusBarManager() {
		this(null);
	}

	public StatusBarManager(android.content.Context context) {
		mContext = context;
	}

	/* AOSP requestAddTileService(): the arguments are checked, a component of another package is answered with
	 * MISMATCHED_PACKAGE; otherwise the status bar service decides. ATL has none (no quick settings), which AOSP
	 * names TILE_ADD_REQUEST_ERROR_NO_STATUS_BAR_SERVICE. */
	public void requestAddTileService(android.content.ComponentName tileServiceComponentName, CharSequence tileLabel,
	                                  android.graphics.drawable.Icon icon, java.util.concurrent.Executor resultExecutor,
	                                  java.util.function.Consumer<Integer> resultCallback) {
		java.util.Objects.requireNonNull(tileServiceComponentName);
		java.util.Objects.requireNonNull(tileLabel);
		java.util.Objects.requireNonNull(icon);
		java.util.Objects.requireNonNull(resultExecutor);
		java.util.Objects.requireNonNull(resultCallback);
		int result = mContext != null && !tileServiceComponentName.getPackageName().equals(mContext.getPackageName())
		    ? TILE_ADD_REQUEST_ERROR_MISMATCHED_PACKAGE
		    : TILE_ADD_REQUEST_ERROR_NO_STATUS_BAR_SERVICE;
		resultExecutor.execute(() -> resultCallback.accept(result));
	}
}
