/*
 * Host test for the receiver bookkeeping behind Context.registerReceiver() / unregisterReceiver(), which follows
 * AOSP LoadedApk.forgetReceiverDispatcher (the receiver object is the key). Pure Java. Run like
 * test/api-impl/android/os/PersistableBundleTest.java with the class android.content.ReceiverRegistryTest.
 */
package android.content;

import junit.framework.TestCase;

public class ReceiverRegistryTest extends TestCase {
	private static BroadcastReceiver receiver() {
		return new BroadcastReceiver() {
			public void onReceive(Context context, Intent intent) {}
		};
	}

	private static int count(ReceiverRegistry registry, BroadcastReceiver receiver) {
		int n = 0;
		for (ReceiverRegistry.Registration r : registry)
			if (r.receiver == receiver)
				n++;
		return n;
	}

	public void testTwoReceiversSameFilterFirstUnregistered() {
		ReceiverRegistry registry = new ReceiverRegistry();
		IntentFilter filter = new IntentFilter("a.b.ACTION");
		BroadcastReceiver first = receiver(), second = receiver();
		registry.add(first, filter, null);
		registry.add(second, filter, null);
		assertTrue(registry.removeAll(first));
		assertEquals(0, count(registry, first));
		assertEquals(1, count(registry, second));
		assertTrue(registry.removeAll(second));
	}

	public void testSameReceiverTwiceOneUnregisterRemovesBoth() {
		ReceiverRegistry registry = new ReceiverRegistry();
		BroadcastReceiver r = receiver();
		registry.add(r, new IntentFilter("a.b.ONE"), null);
		registry.add(r, new IntentFilter("a.b.TWO"), null);
		assertEquals(2, count(registry, r));
		assertTrue(registry.removeAll(r));
		assertEquals(0, count(registry, r));
		assertFalse("second unregister: not registered any more", registry.removeAll(r));
	}

	public void testNotRegisteredAndNull() {
		ReceiverRegistry registry = new ReceiverRegistry();
		assertFalse(registry.removeAll(receiver()));
		assertFalse(registry.removeAll(null));
	}
}
