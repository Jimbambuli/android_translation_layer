package android.app;

import android.app.Notification.MediaStyle;
import android.atl.ATLLoadedApp;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.service.notification.StatusBarNotification;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

public class NotificationManager {

	private static int mpris_notification_id = -1;

	public void cancelAll() {}

	/* AOSP keeps the delegate per package in the NotificationManagerService; ATL runs one app */
	private static String notification_delegate;

	public void setNotificationDelegate(String delegate) {
		notification_delegate = delegate;
	}

	public String getNotificationDelegate() {
		return notification_delegate;
	}

	public void notify(String tag, int id, Notification notification) {
		if (notification.style instanceof MediaStyle) { // MPRIS content is handled by MediaSession implementation
			if (mpris_notification_id == -1) {
				Application application = ATLLoadedApp.getPrimaryApplication().getApplication();
				nativeShowMPRIS(application.getPackageName(), application.get_app_label());
				mpris_notification_id = id;
			}
			return;
		}

		System.out.println("notify(" + tag + ", " + id + ", " + notification + ") called");
		long builder = nativeInitBuilder();
		for (Notification.Action action : notification.actions) {
			int intentType = -1;
			Intent intent = null;
			if (action.intent != null) {
				intentType = action.intent.type;
				intent = action.intent.intent;
			}
			nativeAddAction(builder, action.title, intentType, intent);
		}
		int intentType = -1;
		Intent intent = null;
		if (notification.intent != null) {
			intentType = notification.intent.type;
			intent = notification.intent.intent;
		}
		String iconPath = notification.iconPath == null || notification.iconPath.endsWith(".xml") ? null : notification.iconPath;
		nativeShowNotification(builder, id, notification.title, notification.text, iconPath, notification.ongoing, intentType, intent);
	}

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
	protected native void nativeShowNotification(long builder, int id, String title, String text, String iconPath, boolean ongoing, int intentType, Intent intent);
	protected native void nativeShowMPRIS(String packageName, String identiy);
	protected native void nativeCancel(int id);
	protected native void nativeCancelMPRIS();

	public void createNotificationChannelGroup(NotificationChannelGroup v) {}

	public List<NotificationChannel> getNotificationChannels() {
		synchronized (channels) {
			return new ArrayList<>(channels.values());
		}
	}

	public List<NotificationChannelGroup> getNotificationChannelGroups() {
		return Collections.emptyList();
	}

	public boolean areNotificationsEnabled() {
		return true;
	}

	public void deleteNotificationChannel(String channelId) {
		synchronized (channels) {
			channels.remove(channelId);
		}
	}

	public void createNotificationChannelGroups(List<NotificationChannelGroup> groups) {}

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
