package android.app;

import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.os.Parcel;
import junit.framework.TestCase;

public class NotificationActionTest extends TestCase {
	public void testBuilderKeepsWhatTheSettersGet() {
		Bundle extras = new Bundle();
		extras.putString("k", "v");
		Notification.Action a = new Notification.Action.Builder((Icon)null, "Reply", null)
			.setSemanticAction(Notification.Action.SEMANTIC_ACTION_REPLY)
			.setContextual(true)
			.setAllowGeneratedReplies(false)
			.setAuthenticationRequired(true)
			.addExtras(extras)
			.build();
		assertEquals("Reply", a.title);
		assertEquals(Notification.Action.SEMANTIC_ACTION_REPLY, a.getSemanticAction());
		assertTrue(a.isContextual());
		assertFalse(a.getAllowGeneratedReplies());
		assertTrue(a.isAuthenticationRequired());
		assertEquals("v", a.getExtras().getString("k"));
		assertNull(a.getRemoteInputs());
	}

	public void testDefaultsAndCopy() {
		Notification.Action a = new Notification.Action.Builder((Icon)null, "T", null).build();
		assertEquals(Notification.Action.SEMANTIC_ACTION_NONE, a.getSemanticAction());
		assertFalse(a.isContextual());
		assertTrue(a.getAllowGeneratedReplies());
		Notification.Action b = new Notification.Action.Builder(a).setSemanticAction(Notification.Action.SEMANTIC_ACTION_DELETE).build();
		assertEquals(Notification.Action.SEMANTIC_ACTION_DELETE, b.getSemanticAction());
		assertEquals(Notification.Action.SEMANTIC_ACTION_NONE, a.getSemanticAction());
		assertEquals("T", a.clone().title);
	}

	public void testRemoteInputsAndExtend() {
		RemoteInput in = new RemoteInput.Builder("key").setLabel("l").build();
		Notification.Action a = new Notification.Action.Builder((Icon)null, "T", null)
			.addRemoteInput(in)
			.extend(builder -> builder.setSemanticAction(Notification.Action.SEMANTIC_ACTION_MUTE))
			.build();
		assertEquals(1, a.getRemoteInputs().length);
		assertEquals("key", a.getRemoteInputs()[0].getResultKey());
		assertEquals(Notification.Action.SEMANTIC_ACTION_MUTE, a.getSemanticAction());
	}
}
