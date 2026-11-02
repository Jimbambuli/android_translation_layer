package android.app;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.IBinder;

public abstract class Service extends ContextWrapper {

	private int notification_id;

	public Service() {
		super(null);
	}

	public void onCreate() {
		System.out.println("Service.onCreate() called");
	}

	public void onDestroy() {
		System.out.println("Service.onDestroy() called");
	}

	public abstract IBinder onBind(Intent intent);

	public int onStartCommand(Intent intent, int flags, int startId) {
		System.out.println("Service.onStartCommand(" + intent + ", " + flags + ", " + startId + ") called");
		return 0;
	}

	public static final int STOP_FOREGROUND_LEGACY = 0;
	public static final int STOP_FOREGROUND_REMOVE = 1 << 0;
	public static final int STOP_FOREGROUND_DETACH = 1 << 1;

	/* the foreground services of the app (ActiveServices); while there are any, the app keeps running when
	 * its window is closed, as an Android app's process keeps running for its foreground service */
	private static int foreground_services = 0;
	private boolean foreground = false;

	/* AOSP Service.startForeground(): ActiveServices.setServiceForegroundInnerLocked() posts the notification
	 * (ServiceRecord.postNotification()); a new id replaces the notification of the old one */
	public final void startForeground(int id, Notification notification) {
		System.out.println("startForeground(" + id + ", " + notification + ") called");
		if (notification == null)
			throw new IllegalArgumentException("null notification");
		NotificationManager notificationManager = new NotificationManager();
		if (foreground && notification_id != id)
			notificationManager.cancel(notification_id);
		this.notification_id = id;
		notificationManager.notify(id, notification);
		setForeground(true);
	}

	public final void startForeground(int id, Notification notification, int foregroundServiceType) {
		startForeground(id, notification);
	}

	@Deprecated
	public final void stopForeground(boolean removeNotification) {
		stopForeground(removeNotification ? STOP_FOREGROUND_REMOVE : STOP_FOREGROUND_LEGACY);
	}

	/* AOSP Service.stopForeground(int): the notification stays unless STOP_FOREGROUND_REMOVE */
	public final void stopForeground(int notificationBehavior) {
		System.out.println("stopForeground(" + notificationBehavior + ") called");
		if ((notificationBehavior & STOP_FOREGROUND_REMOVE) != 0)
			new NotificationManager().cancel(notification_id);
		setForeground(false);
	}

	public final int getForegroundServiceType() {
		return 0;
	}

	private void setForeground(boolean foreground) {
		if (this.foreground == foreground)
			return;
		this.foreground = foreground;
		synchronized (Service.class) {
			foreground_services += foreground ? 1 : -1;
			nativeSetForegroundServices(foreground_services);
		}
	}

	private static native void nativeSetForegroundServices(int count);

	public Application getApplication() {
		return this.get_atl_loaded_app().getApplication();
	}

	public void stopSelf(int startId) {
		System.out.println("Service.stopSelf(" + startId + ") called");
	}

	public void stopSelf() {
		System.out.println("Service.stopSelf() called");
	}

	public boolean stopSelfResult(int startId) {
		System.out.println("Service.stopSelfResult(" + startId + ") called");
		return true;
	}

	public void attachBaseContext(Context newBase) {
		super.attachBaseContext(newBase);
		System.out.println("Service.attachBaseContext(" + newBase + ") called");
	}
}
