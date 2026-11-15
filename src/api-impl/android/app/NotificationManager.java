package android.app;

import android.app.Notification.MediaStyle;
import android.atl.ATLLoadedApp;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.service.notification.StatusBarNotification;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

public class NotificationManager {

	public static final int IMPORTANCE_UNSPECIFIED = -1000;
	public static final int IMPORTANCE_NONE = 0;
	public static final int IMPORTANCE_MIN = 1;
	public static final int IMPORTANCE_LOW = 2;
	public static final int IMPORTANCE_DEFAULT = 3;
	public static final int IMPORTANCE_HIGH = 4;
	public static final int IMPORTANCE_MAX = 5;

	/* GNotificationPriority */
	private static final int G_NORMAL = 0, G_LOW = 1, G_HIGH = 2, G_URGENT = 3;

	/**
	 * The GNotificationPriority for the importance of the notification's channel or, without one, for its
	 * priority mapped to an importance as AOSP does for apps before channels; -1 when the channel is switched off.
	 */
	static int desktopPriority(Notification notification) {
		NotificationChannel channel;
		synchronized (channels) {
			channel = notification.channelId == null ? null : channels.get(notification.channelId);
		}
		int importance = channel == null ? IMPORTANCE_UNSPECIFIED : channel.getImportance();
		if (importance == IMPORTANCE_UNSPECIFIED) {
			int p = notification.priority;
			importance = p <= Notification.PRIORITY_MIN ? IMPORTANCE_MIN : p == Notification.PRIORITY_LOW ? IMPORTANCE_LOW : p == Notification.PRIORITY_DEFAULT ? IMPORTANCE_DEFAULT : p == Notification.PRIORITY_HIGH ? IMPORTANCE_HIGH : IMPORTANCE_MAX;
		}
		if (importance == IMPORTANCE_NONE)
			return -1;
		if (importance <= IMPORTANCE_LOW)
			return G_LOW;
		if (importance == IMPORTANCE_DEFAULT)
			return G_NORMAL;
		return importance == IMPORTANCE_HIGH ? G_HIGH : G_URGENT;
	}

	/* The desktop portals have no text field for a reply (RemoteInput): that button opens the app instead. */
	static PendingIntent buttonIntent(Notification notification, Notification.Action action) {
		return action.getRemoteInputs() == null ? action.actionIntent : notification.contentIntent;
	}

	private static int mpris_notification_id = -1;

	public void cancelAll() {
		Integer[] ids;
		synchronized (active) {
			ids = active.toArray(new Integer[0]);
		}
		for (int id : ids)
			cancel(id);
	}

	/* AOSP keeps the delegate per package in the NotificationManagerService; ATL runs one app */
	private static String notification_delegate;

	public void setNotificationDelegate(String delegate) {
		notification_delegate = delegate;
	}

	public String getNotificationDelegate() {
		return notification_delegate;
	}

	public void notify(String tag, int id, Notification notification) {
		/* Android 13: without POST_NOTIFICATIONS the system drops the app's notifications */
		if (android.content.pm.PackageManager.deniedByUser("android.permission.POST_NOTIFICATIONS")
		    && !(notification.style instanceof MediaStyle))
			return;
		if (notification.style instanceof MediaStyle) { // MPRIS content is handled by MediaSession implementation
			if (mpris_notification_id == -1) {
				Application application = ATLLoadedApp.getPrimaryApplication().getApplication();
				nativeShowMPRIS(application.getPackageName(), application.get_app_label());
				mpris_notification_id = id;
			}
			return;
		}

		System.out.println("notify(" + tag + ", " + id + ", " + notification + ") called");
		int priority = desktopPriority(notification);
		if (priority < 0)
			return;
		long builder = nativeInitBuilder();
		for (Notification.Action action : notification.actions != null ? notification.actions : new Notification.Action[0]) {
			int intentType = -1;
			Intent intent = null;
			PendingIntent pendingIntent = buttonIntent(notification, action);
			if (pendingIntent != null) {
				intentType = pendingIntent.type;
				intent = pendingIntent.intent;
			}
			nativeAddAction(builder, String.valueOf(action.title), intentType, intent);
		}
		int intentType = -1;
		Intent intent = null;
		if (notification.contentIntent != null) {
			intentType = notification.contentIntent.type;
			intent = notification.contentIntent.intent;
		}
		String iconPath = notification.iconPath == null || notification.iconPath.endsWith(".xml") ? null : notification.iconPath;
		/* the desktop notification has no expanded form: the text of a style (big text, lines, latest message) */
		CharSequence styleText = notification.style != null ? notification.style.desktopText() : null;
		String text = styleText != null ? styleText.toString() : notification.text;
		nativeShowNotification(builder, id, notification.title, text, iconPath, notification.ongoing, priority, intentType, intent);
		synchronized (active) {
			active.add(id);
		}
	}

	private static final Set<Integer> active = new HashSet<>();

	public void notify(int id, Notification notification) {
		notify(null, id, notification);
	}

	public void cancel(String tag, final int id) {
		// remove_notification doesn't work reliably when sent directly after add_notification in GNOME session.
		// So we give some extra delay here.
		new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
			@Override
			public void run() {
				if (mpris_notification_id == id) {
					mpris_notification_id = -1;
					nativeCancelMPRIS();
				} else {
					nativeCancel(id);
				}
				synchronized (active) {
					active.remove(id);
				}
			}
		}, 100);
	}

	public void cancel(int id) {
		cancel(null, id);
	}

	/* AOSP keeps the channels per package in the NotificationManagerService; ATL runs one app */
	private static final LinkedHashMap<String, NotificationChannel> channels = new LinkedHashMap<>();

	/**
	 * Creates a notification channel that notifications can be posted to. Creating an existing
	 * channel with its original values performs no operation; only its name, description and
	 * group are updated, as in AOSP.
	 */
	public void createNotificationChannel(NotificationChannel channel) {
		synchronized (channels) {
			NotificationChannel existing = channels.get(channel.getId());
			if (existing == null) {
				channels.put(channel.getId(), channel);
				return;
			}
			existing.setName(channel.getName());
			existing.setDescription(channel.getDescription());
			if (channel.getGroup() != null)
				existing.setGroup(channel.getGroup());
		}
	}

	protected native long nativeInitBuilder();
	protected native void nativeAddAction(long builder, String title, int intentType, Intent intent);
	protected native void nativeShowNotification(long builder, int id, String title, String text, String iconPath, boolean ongoing, int priority, int intentType, Intent intent);
	protected native void nativeShowMPRIS(String packageName, String identiy);
	protected native void nativeCancel(int id);
	protected native void nativeCancelMPRIS();

	/* AOSP keeps the groups with the channels in the NotificationManagerService */
	private static final LinkedHashMap<String, NotificationChannelGroup> groups = new LinkedHashMap<>();

	public void createNotificationChannelGroup(NotificationChannelGroup group) {
		synchronized (channels) {
			groups.put(group.getId(), group.copyWith(new ArrayList<NotificationChannel>()));
		}
	}

	/* AOSP: the group with the channels that belong to it */
	public NotificationChannelGroup getNotificationChannelGroup(String channelGroupId) {
		synchronized (channels) {
			NotificationChannelGroup group = groups.get(channelGroupId);
			if (group == null)
				return null;
			List<NotificationChannel> in = new ArrayList<>();
			for (NotificationChannel channel : channels.values())
				if (channelGroupId.equals(channel.getGroup()))
					in.add(channel);
			return group.copyWith(in);
		}
	}

	/* AOSP: deleting a group deletes its channels */
	public void deleteNotificationChannelGroup(String groupId) {
		synchronized (channels) {
			if (groups.remove(groupId) == null)
				return;
			channels.values().removeIf(channel -> groupId.equals(channel.getGroup()));
		}
	}

	/* AOSP: the conversation's own channel, or its parent channel when it has none; ATL keeps no conversation channels */
	public NotificationChannel getNotificationChannel(String channelId, String conversationId) {
		return getNotificationChannel(channelId);
	}

	public List<NotificationChannel> getNotificationChannels() {
		synchronized (channels) {
			return new ArrayList<>(channels.values());
		}
	}

	public List<NotificationChannelGroup> getNotificationChannelGroups() {
		synchronized (channels) {
			List<NotificationChannelGroup> list = new ArrayList<>();
			for (String id : groups.keySet())
				list.add(getNotificationChannelGroup(id));
			return list;
		}
	}

	/* AOSP getImportance asks NotificationManagerService.getPackageImportance: IMPORTANCE_DEFAULT while the app
	 * holds POST_NOTIFICATIONS, else IMPORTANCE_NONE (same test as areNotificationsEnabled here) */
	public int getImportance() {
		return areNotificationsEnabled() ? IMPORTANCE_DEFAULT : IMPORTANCE_NONE;
	}

	public static final int INTERRUPTION_FILTER_UNKNOWN = 0;
	public static final int INTERRUPTION_FILTER_ALL = 1;
	public static final int INTERRUPTION_FILTER_PRIORITY = 2;
	public static final int INTERRUPTION_FILTER_NONE = 3;
	public static final int INTERRUPTION_FILTER_ALARMS = 4;

	/* AOSP: zenModeToInterruptionFilter(getZenMode()); ATL has no Do Not Disturb, the zen mode is off */
	public final int getCurrentInterruptionFilter() {
		return INTERRUPTION_FILTER_ALL;
	}

	/* AOSP: Do Not Disturb access is granted by the user in the settings, which ATL does not have */
	public boolean isNotificationPolicyAccessGranted() {
		return false;
	}

	/* AOSP: the service allows full screen intents below target SDK 34, above with USE_FULL_SCREEN_INTENT,
	 * which ATL grants like every permission */
	public boolean canUseFullScreenIntent() {
		return true;
	}

	public boolean areNotificationsEnabled() {
		return !android.content.pm.PackageManager.deniedByUser("android.permission.POST_NOTIFICATIONS");
	}

	public void deleteNotificationChannel(String channelId) {
		synchronized (channels) {
			channels.remove(channelId);
		}
	}

	public void createNotificationChannelGroups(List<NotificationChannelGroup> groups) {
		for (NotificationChannelGroup group : groups)
			createNotificationChannelGroup(group);
	}

	public void createNotificationChannels(List<NotificationChannel> channels) {
		for (NotificationChannel channel : channels)
			createNotificationChannel(channel);
	}

	public StatusBarNotification[] getActiveNotifications() {
		return new StatusBarNotification[0];
	}

	public NotificationChannel getNotificationChannel(String channelId) {
		synchronized (channels) {
			return channels.get(channelId);
		}
	}
}
