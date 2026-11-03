/*
 * Host test for the desktop notification of an Android notification: the importance of its channel (or, without
 * one, its priority as AOSP maps it) becomes the GNotification priority, a switched-off channel shows nothing,
 * and a reply action (RemoteInput) opens the app, as the desktop portals have no text field. Run like
 * test/api-impl/android/os/SystemPropertiesTest.java.
 */
package android.app;

import junit.framework.TestCase;

public class NotificationDesktopTest extends TestCase {
	private static final int G_LOW = 1, G_NORMAL = 0, G_HIGH = 2, G_URGENT = 3;

	private static Notification on(String channel, int priority) {
		Notification n = new Notification.Builder(null, channel).setPriority(priority).build();
		return n;
	}

	public void testChannelImportance() {
		NotificationManager nm = new NotificationManager();
		nm.createNotificationChannel(new NotificationChannel("leise", "Leise", NotificationManager.IMPORTANCE_LOW));
		nm.createNotificationChannel(new NotificationChannel("normal", "Normal", NotificationManager.IMPORTANCE_DEFAULT));
		nm.createNotificationChannel(new NotificationChannel("laut", "Laut", NotificationManager.IMPORTANCE_HIGH));
		nm.createNotificationChannel(new NotificationChannel("aus", "Aus", NotificationManager.IMPORTANCE_NONE));
		assertEquals(G_LOW, NotificationManager.desktopPriority(on("leise", Notification.PRIORITY_MAX)));
		assertEquals(G_NORMAL, NotificationManager.desktopPriority(on("normal", Notification.PRIORITY_MIN)));
		assertEquals(G_HIGH, NotificationManager.desktopPriority(on("laut", Notification.PRIORITY_DEFAULT)));
		assertEquals(-1, NotificationManager.desktopPriority(on("aus", Notification.PRIORITY_HIGH)));
	}

	public void testPriorityWithoutChannel() {
		assertEquals(G_LOW, NotificationManager.desktopPriority(on(null, Notification.PRIORITY_MIN)));
		assertEquals(G_NORMAL, NotificationManager.desktopPriority(on("unbekannt", Notification.PRIORITY_DEFAULT)));
		assertEquals(G_HIGH, NotificationManager.desktopPriority(on(null, Notification.PRIORITY_HIGH)));
		assertEquals(G_URGENT, NotificationManager.desktopPriority(on(null, Notification.PRIORITY_MAX)));
	}

	public void testReplyActionOpensTheApp() {
		PendingIntent content = PendingIntent.getActivity(null, 1, new android.content.Intent("OPEN"), 0);
		PendingIntent reply = PendingIntent.getBroadcast(null, 2, new android.content.Intent("REPLY"), 0);
		PendingIntent read = PendingIntent.getBroadcast(null, 3, new android.content.Intent("READ"), 0);
		Notification.Action answer = new Notification.Action.Builder(0, "Antworten", reply)
		                                 .addRemoteInput(new RemoteInput.Builder("text").build()).build();
		Notification.Action mark = new Notification.Action.Builder(0, "Gelesen", read).build();
		Notification n = new Notification.Builder(null, null).setContentIntent(content).addAction(answer).addAction(mark).build();
		assertSame(content, NotificationManager.buttonIntent(n, answer));
		assertSame(read, NotificationManager.buttonIntent(n, mark));
		assertEquals(1, answer.getRemoteInputs().length);
	}
}
