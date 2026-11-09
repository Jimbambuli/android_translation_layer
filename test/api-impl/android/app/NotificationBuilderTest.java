package android.app;

import android.os.Bundle;
import junit.framework.TestCase;

public class NotificationBuilderTest extends TestCase {
	public void testFlagsAndFields() {
		PendingIntent none = null;
		Notification n = new Notification.Builder(null)
			.setAutoCancel(true)
			.setOngoing(true)
			.setOnlyAlertOnce(true)
			.setLocalOnly(true)
			.setGroupSummary(true)
			.setNumber(7)
			.setColor(0xFF112233)
			.setCategory("msg")
			.setGroup("g")
			.setSortKey("k")
			.setGroupAlertBehavior(Notification.GROUP_ALERT_CHILDREN)
			.setBadgeIconType(Notification.BADGE_ICON_LARGE)
			.setVisibility(Notification.VISIBILITY_SECRET)
			.setDefaults(Notification.DEFAULT_LIGHTS)
			.setFullScreenIntent(none, true)
			.setLights(0xFF00FF00, 100, 200)
			.build();
		int flags = Notification.FLAG_AUTO_CANCEL | Notification.FLAG_ONGOING_EVENT | Notification.FLAG_ONLY_ALERT_ONCE
			| Notification.FLAG_LOCAL_ONLY | Notification.FLAG_GROUP_SUMMARY | Notification.FLAG_HIGH_PRIORITY | Notification.FLAG_SHOW_LIGHTS;
		assertEquals(flags, n.flags);
		assertEquals(7, n.number);
		assertEquals(0xFF112233, n.color);
		assertEquals("g", n.getGroup());
		assertEquals("k", n.getSortKey());
		assertTrue(n.isGroupSummary());
		assertEquals(Notification.GROUP_ALERT_CHILDREN, n.getGroupAlertBehavior());
		assertEquals(Notification.BADGE_ICON_LARGE, n.getBadgeIconType());
		assertEquals(Notification.VISIBILITY_SECRET, n.visibility);
		assertEquals(0xFF00FF00, n.ledARGB);
		assertEquals(100, n.ledOnMS);
		assertEquals(200, n.ledOffMS);
	}

	public void testFlagsOff() {
		Notification n = new Notification.Builder(null).setAutoCancel(true).setAutoCancel(false).build();
		assertEquals(0, n.flags & Notification.FLAG_AUTO_CANCEL);
	}

	public void testExtras() {
		Bundle user = new Bundle();
		user.putString("a", "b");
		Notification n = new Notification.Builder(null)
			.setContentInfo("info")
			.setSubText("sub")
			.setProgress(10, 3, false)
			.addPerson("tel:1")
			.addPerson("tel:2")
			.setExtras(user)
			.build();
		assertEquals("info", n.extras.getCharSequence(Notification.EXTRA_INFO_TEXT));
		assertEquals("sub", n.extras.getCharSequence(Notification.EXTRA_SUB_TEXT));
		assertEquals(10, n.extras.getInt(Notification.EXTRA_PROGRESS_MAX));
		assertEquals(3, n.extras.getInt(Notification.EXTRA_PROGRESS));
		assertEquals(2, n.extras.getStringArrayList(Notification.EXTRA_PEOPLE).size());
		assertEquals("b", n.extras.getString("a"));
	}
}
