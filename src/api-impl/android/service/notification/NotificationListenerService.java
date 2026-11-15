package android.service.notification;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.os.IBinder;

/* AOSP NotificationListenerService as far as it can be used without the NotificationManagerService, which
 * binds a listener only after the user granted it notification access in the settings (ATL has no such
 * setting: NotificationManager.isNotificationListenerAccessGranted() is false). Nobody binds the service,
 * so every call takes the path AOSP has for a listener that is not bound (isBound() false): nothing is
 * done and null/0 comes back */
public abstract class NotificationListenerService extends Service {
	public static final String SERVICE_INTERFACE = "android.service.notification.NotificationListenerService";
	public static final int REASON_CLICK = 1;
	public static final int REASON_CANCEL = 2;
	public static final int REASON_CANCEL_ALL = 3;
	public static final int REASON_ERROR = 4;
	public static final int REASON_PACKAGE_CHANGED = 5;
	public static final int REASON_USER_STOPPED = 6;
	public static final int REASON_PACKAGE_BANNED = 7;
	public static final int REASON_APP_CANCEL = 8;
	public static final int REASON_APP_CANCEL_ALL = 9;
	public static final int REASON_LISTENER_CANCEL = 10;
	public static final int REASON_LISTENER_CANCEL_ALL = 11;
	public static final int REASON_GROUP_SUMMARY_CANCELED = 12;
	public static final int REASON_GROUP_OPTIMIZATION = 13;
	public static final int REASON_PACKAGE_SUSPENDED = 14;
	public static final int REASON_PROFILE_TURNED_OFF = 15;
	public static final int REASON_UNAUTOBUNDLED = 16;
	public static final int REASON_CHANNEL_BANNED = 17;
	public static final int REASON_SNOOZED = 18;
	public static final int REASON_TIMEOUT = 19;
	public static final int REASON_CHANNEL_REMOVED = 20;
	public static final int REASON_CLEAR_DATA = 21;
	public static final int HINT_HOST_DISABLE_EFFECTS = 1;
	public static final int HINT_HOST_DISABLE_NOTIFICATION_EFFECTS = 1 << 1;
	public static final int HINT_HOST_DISABLE_CALL_EFFECTS = 1 << 2;
	public static final int INTERRUPTION_FILTER_ALL = 1;
	public static final int INTERRUPTION_FILTER_PRIORITY = 2;
	public static final int INTERRUPTION_FILTER_NONE = 3;
	public static final int INTERRUPTION_FILTER_ALARMS = 4;
	public static final int INTERRUPTION_FILTER_UNKNOWN = 0;
	public static final int FLAG_FILTER_TYPE_CONVERSATIONS = 1;
	public static final int FLAG_FILTER_TYPE_ALERTING = 2;
	public static final int FLAG_FILTER_TYPE_SILENT = 4;
	public static final int FLAG_FILTER_TYPE_ONGOING = 8;
	public static final int NOTIFICATION_CHANNEL_OR_GROUP_ADDED = 1;
	public static final int NOTIFICATION_CHANNEL_OR_GROUP_UPDATED = 2;
	public static final int NOTIFICATION_CHANNEL_OR_GROUP_DELETED = 3;
	public static final String META_DATA_DISABLED_FILTER_TYPES = "android.service.notification.disabled_filter_types";
	public static final String META_DATA_DEFAULT_FILTER_TYPES = "android.service.notification.default_filter_types";

	/* AOSP NotificationListenerService.RankingMap: the rankings of the active notifications, none here */
	public static class RankingMap {
		public String[] getOrderedKeys() {
			return new String[0];
		}

		public boolean getRanking(String key, Ranking outRanking) {
			return false;
		}
	}

	public static class Ranking {
		public static final int USER_SENTIMENT_NEGATIVE = -1;
		public static final int USER_SENTIMENT_NEUTRAL = 0;
		public static final int USER_SENTIMENT_POSITIVE = 1;
		public static final int VISIBILITY_NO_OVERRIDE = -1000;

		public Ranking() {}
	}

	public void onNotificationPosted(StatusBarNotification sbn) {
		// optional
	}

	public void onNotificationPosted(StatusBarNotification sbn, RankingMap rankingMap) {
		onNotificationPosted(sbn);
	}

	public void onNotificationRemoved(StatusBarNotification sbn) {
		// optional
	}

	public void onNotificationRemoved(StatusBarNotification sbn, RankingMap rankingMap) {
		onNotificationRemoved(sbn);
	}

	public void onNotificationRemoved(StatusBarNotification sbn, RankingMap rankingMap, int reason) {
		onNotificationRemoved(sbn, rankingMap);
	}

	public void onListenerConnected() {
		// optional
	}

	public void onListenerDisconnected() {
		// optional
	}

	public void onNotificationRankingUpdate(RankingMap rankingMap) {
		// optional
	}

	public void onListenerHintsChanged(int hints) {
		// optional
	}

	public void onInterruptionFilterChanged(int interruptionFilter) {
		// optional
	}

	public final void cancelNotification(String pkg, String tag, int id) {}

	public final void cancelNotification(String key) {}

	public final void cancelAllNotifications() {
		cancelNotifications(null);
	}

	public final void cancelNotifications(String[] keys) {}

	public final void snoozeNotification(String key, long durationMs) {}

	public final void setNotificationsShown(String[] keys) {}

	public final void setOnNotificationPostedTrim(int trim) {}

	public StatusBarNotification[] getActiveNotifications() {
		return getActiveNotifications(null, 0);
	}

	public StatusBarNotification[] getActiveNotifications(String[] keys) {
		return getActiveNotifications(keys, 0);
	}

	public StatusBarNotification[] getActiveNotifications(int trim) {
		return getActiveNotifications(null, trim);
	}

	public StatusBarNotification[] getActiveNotifications(String[] keys, int trim) {
		return null; /* AOSP: asks the service only if isBound() */
	}

	public final StatusBarNotification[] getSnoozedNotifications() {
		return null;
	}

	public final int getCurrentListenerHints() {
		return 0;
	}

	public final int getCurrentInterruptionFilter() {
		return INTERRUPTION_FILTER_UNKNOWN;
	}

	public final void clearRequestedListenerHints() {}

	public final void requestListenerHints(int hints) {}

	public final void requestInterruptionFilter(int interruptionFilter) {}

	public RankingMap getCurrentRanking() {
		return null;
	}

	/* AOSP returns its binder wrapper; ATL has no INotificationListener to implement */
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	protected boolean isBound() {
		return false;
	}

	/* AOSP asks the service to bind the listener again; a component that is not enabled in the settings is
	 * not bound */
	public static void requestRebind(ComponentName componentName) {}

	public static void requestUnbind(ComponentName componentName) {}

	public final void requestUnbind() {}
}
