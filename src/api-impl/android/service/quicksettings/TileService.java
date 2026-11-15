package android.service.quicksettings;

import android.app.Dialog;
import android.app.PendingIntent;
import android.app.Service;
import android.app.StatusBarManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.os.IBinder;
import android.util.Log;
import java.util.Objects;

/* AOSP core/java/android/service/quicksettings/TileService.java (android-16). On Android the quick settings host
 * (SystemUI) binds the service with its IQSService and the tile token in the intent extras; ATL has no such
 * host, so the service is never bound by the system and stays as AOSP's is before onBind(): no tile
 * (getQsTile() null), no service (mService null). The callbacks are empty in AOSP too. */
public class TileService extends Service {
	private static final String TAG = "TileService";

	public static final String ACTION_QS_TILE_PREFERENCES = "android.service.quicksettings.action.QS_TILE_PREFERENCES";
	public static final String ACTION_QS_TILE = "android.service.quicksettings.action.QS_TILE";
	public static final String META_DATA_ACTIVE_TILE = "android.service.quicksettings.ACTIVE_TILE";
	public static final String META_DATA_TOGGLEABLE_TILE = "android.service.quicksettings.TOGGLEABLE_TILE";
	public static final String EXTRA_SERVICE = "service";
	public static final String EXTRA_TOKEN = "token";
	public static final String EXTRA_STATE = "state";

	/* AOSP ChangeId START_ACTIVITY_NEEDS_PENDING_INTENT: enabled from target SDK 34 (UPSIDE_DOWN_CAKE) */
	public static final long START_ACTIVITY_NEEDS_PENDING_INTENT = 241766793L;

	private Tile mTile;

	public void onTileAdded() {
	}

	public void onTileRemoved() {
	}

	public void onStartListening() {
	}

	public void onStopListening() {
	}

	public void onClick() {
	}

	/* AOSP: reported to the IQSService only when bound (mService != null) */
	public final void setStatusIcon(Icon icon, String contentDescription) {
	}

	/* AOSP shows the dialog as TYPE_QS_DIALOG over the panel and tells the service; here it is just shown */
	public final void showDialog(Dialog dialog) {
		dialog.show();
	}

	/* AOSP: the service unlocks the device first and then calls the runnable; there is no lock screen
	 * (KeyguardManager.isKeyguardLocked() false), so it runs at once */
	public final void unlockAndRun(Runnable runnable) {
		runnable.run();
	}

	/* AOSP: the QS service answers the keyguard state; without a lock screen the device is neither secure
	 * (KeyguardManager.isDeviceSecure()) nor locked */
	public final boolean isSecure() {
		return false;
	}

	public final boolean isLocked() {
		return false;
	}

	@Deprecated
	public final void startActivityAndCollapse(Intent intent) {
		if (getApplicationInfo().targetSdkVersion >= 34)
			throw new UnsupportedOperationException("startActivityAndCollapse: Starting activity from TileService using an Intent is not allowed.");
		startActivity(intent);
	}

	/* AOSP: the service starts the PendingIntent's activity and collapses the panel */
	public final void startActivityAndCollapse(PendingIntent pendingIntent) {
		Objects.requireNonNull(pendingIntent);
		pendingIntent.send();
	}

	public final Tile getQsTile() {
		return mTile;
	}

	/* AOSP returns the IQSTileService stub after asking the IQSService for the tile; there is no host to bind it */
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	/* AOSP: StatusBarManager.requestTileServiceListeningState(component) */
	public static final void requestListeningState(Context context, ComponentName component) {
		StatusBarManager sbm = (StatusBarManager)context.getSystemService(Context.STATUS_BAR_SERVICE);
		if (sbm == null) {
			Log.e(TAG, "No StatusBarManager service found");
			return;
		}
		sbm.requestTileServiceListeningState(component);
	}
}
