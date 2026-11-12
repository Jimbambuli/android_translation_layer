/*
 * Host test for the notification styles, persons and RemoteInput results as AOSP keeps them in the extras, and
 * for the text of a style that the desktop notification shows. Run like test/api-impl/android/os/SystemPropertiesTest.java.
 */
package android.app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import junit.framework.TestCase;

public class NotificationStyleTest extends TestCase {
	public void testBigText() {
		Notification.Builder b = new Notification.Builder(null, null).setContentTitle("T").setContentText("kurz");
		new Notification.BigTextStyle(b).setBigContentTitle("Gross").setSummaryText("S").bigText("lang und ausfuehrlich");
		Notification n = b.build();
		assertEquals("lang und ausfuehrlich", n.extras.getCharSequence(Notification.EXTRA_BIG_TEXT));
		assertEquals("Gross", n.extras.getCharSequence(Notification.EXTRA_TITLE_BIG));
		assertEquals("S", n.extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT));
		assertEquals("T", n.extras.getCharSequence(Notification.EXTRA_TITLE));
		assertEquals("kurz", n.extras.getCharSequence(Notification.EXTRA_TEXT));
		assertEquals(Notification.BigTextStyle.class.getName(), n.extras.getString(Notification.EXTRA_TEMPLATE));
		assertEquals("lang und ausfuehrlich", n.style.desktopText());
	}

	public void testInbox() {
		Notification n = new Notification.Builder(null, null)
			.setStyle(new Notification.InboxStyle().addLine("a").addLine("b")).build();
		CharSequence[] lines = n.extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
		assertEquals(2, lines.length);
		assertEquals("a\nb", n.style.desktopText().toString());
	}

	public void testMessaging() {
		Person ich = new Person.Builder().setName("Ich").build();
		Notification.MessagingStyle st = new Notification.MessagingStyle(ich).setConversationTitle("Gruppe").setGroupConversation(true);
		st.addMessage("Hallo", 1, "Anna");
		st.addMessage(new Notification.MessagingStyle.Message("Tschuess", 2, new Person.Builder().setName("Beat").build()));
		Notification n = new Notification.Builder(null, null).setStyle(st).build();
		Parcelable[] msgs = n.extras.getParcelableArray(Notification.EXTRA_MESSAGES);
		assertEquals(2, msgs.length);
		assertEquals("Anna", ((Bundle)msgs[0]).getCharSequence("sender"));
		assertEquals(2, ((Bundle)msgs[1]).getLong("time"));
		assertEquals("Gruppe", n.extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE));
		assertTrue(n.extras.getBoolean(Notification.EXTRA_IS_GROUP_CONVERSATION));
		assertEquals("Ich", n.extras.getCharSequence(Notification.EXTRA_SELF_DISPLAY_NAME));
		assertEquals("Beat: Tschuess", n.style.desktopText().toString());
		st.setGroupConversation(false);
		assertEquals("Tschuess", n.style.desktopText().toString());
		for (int i = 0; i < 30; i++)
			st.addMessage("m" + i, i, (CharSequence)null);
		assertEquals(Notification.MessagingStyle.MAXIMUM_RETAINED_MESSAGES, st.getMessages().size());
	}

	public void testPersonsAndActions() {
		Notification leer = new Notification.Builder(null, null).build();
		assertNull(leer.actions);
		PendingIntent pi = PendingIntent.getBroadcast(null, 1, new Intent("X"), 0);
		Notification n = new Notification.Builder(null, null)
			.addPerson(new Person.Builder().setName("Anna").build())
			.addAction(new Notification.Action.Builder(0, "Ok", pi).build())
			.setContentIntent(pi).build();
		assertEquals(1, n.actions.length);
		assertSame(pi, n.contentIntent);
		assertEquals(1, n.extras.getParcelableArrayList(Notification.EXTRA_PEOPLE_LIST).size());
	}

	public void testCallStyleNeedsAName() {
		PendingIntent pi = PendingIntent.getBroadcast(null, 1, new Intent("X"), 0);
		try {
			Notification.CallStyle.forOngoingCall(new Person.Builder().build(), pi);
			fail();
		} catch (IllegalArgumentException e) {
		}
		Notification n = new Notification.Builder(null, null)
			.setStyle(Notification.CallStyle.forOngoingCall(new Person.Builder().setName("Anna").build(), pi)).build();
		assertEquals(Notification.CallStyle.CALL_TYPE_ONGOING, n.extras.getInt(Notification.EXTRA_CALL_TYPE));
	}

	public void testRemoteInputResults() {
		RemoteInput ri = new RemoteInput.Builder("antwort").setAllowDataType("image/png", true).build();
		Intent intent = new Intent("REPLY");
		assertNull(RemoteInput.getResultsFromIntent(intent));
		Bundle results = new Bundle();
		results.putCharSequence("antwort", "Gruezi");
		RemoteInput.addResultsToIntent(new RemoteInput[] {ri}, intent, results);
		assertEquals("Gruezi", RemoteInput.getResultsFromIntent(intent).getCharSequence("antwort"));
		RemoteInput.setResultsSource(intent, RemoteInput.SOURCE_CHOICE);
		assertEquals(RemoteInput.SOURCE_CHOICE, RemoteInput.getResultsSource(intent));
		assertTrue(ri.getAllowedDataTypes().contains("image/png"));
	}
}
