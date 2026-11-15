/*
 * Host test for the receiver bookkeeping behind Context.registerReceiver() / unregisterReceiver(), which follows
 * AOSP LoadedApk.forgetReceiverDispatcher (the receiver object is the key). Pure Java. Run like
 * test/api-impl/android/os/PersistableBundleTest.java with the class android.content.ReceiverRegistryTest.
 */
package android.content;

import junit.framework.TestCase;

public class ReceiverRegistryTest extends TestCase {
	private static final Object OWNER = new Object();

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
		registry.add(OWNER, first, filter, null);
		registry.add(OWNER, second, filter, null);
		assertTrue(registry.removeAll(OWNER, first));
		assertEquals(0, count(registry, first));
		assertEquals(1, count(registry, second));
		assertTrue(registry.removeAll(OWNER, second));
	}

	public void testSameReceiverTwiceOneUnregisterRemovesBoth() {
		ReceiverRegistry registry = new ReceiverRegistry();
		BroadcastReceiver r = receiver();
		registry.add(OWNER, r, new IntentFilter("a.b.ONE"), null);
		registry.add(OWNER, r, new IntentFilter("a.b.TWO"), null);
		assertEquals(2, count(registry, r));
		assertTrue(registry.removeAll(OWNER, r));
		assertEquals(0, count(registry, r));
		assertFalse("second unregister: not registered any more", registry.removeAll(OWNER, r));
	}

	public void testNotRegisteredAndNull() {
		ReceiverRegistry registry = new ReceiverRegistry();
		assertFalse(registry.removeAll(OWNER, receiver()));
		assertFalse(registry.removeAll(OWNER, null));
	}

	/* LoadedApk.mReceivers is per Context: unregistering with the other Context is not registered there */
	public void testSameReceiverTwoContextsUnregisterOnlyOne() {
		ReceiverRegistry registry = new ReceiverRegistry();
		Object activity = new Object(), application = new Object();
		IntentFilter filter = new IntentFilter("a.b.ACTION");
		BroadcastReceiver r = receiver();
		registry.add(activity, r, filter, null);
		registry.add(application, r, filter, null);
		assertEquals("both registrations get broadcasts", 2, count(registry, r));
		assertTrue(registry.removeAll(activity, r));
		assertEquals(1, count(registry, r));
		assertFalse("not registered with the activity any more", registry.removeAll(activity, r));
		assertTrue(registry.removeAll(application, r));
		assertEquals(0, count(registry, r));
	}

	public void testOtherContextNotRegistered() {
		ReceiverRegistry registry = new ReceiverRegistry();
		BroadcastReceiver r = receiver();
		registry.add(new Object(), r, new IntentFilter("a.b.ACTION"), null);
		assertFalse(registry.removeAll(new Object(), r));
		assertEquals(1, count(registry, r));
	}

	/* LoadedApk.ReceiverDispatcher.mContext: each registration carries the Context it was registered with,
	 * which onReceive() gets, whoever sends the broadcast */
	public void testEachRegistrationCarriesItsOwnContext() {
		ReceiverRegistry registry = new ReceiverRegistry();
		Object activity = new Object(), application = new Object();
		BroadcastReceiver r = receiver();
		registry.add(activity, r, new IntentFilter("a.b.ACTION"), null);
		registry.add(application, r, new IntentFilter("a.b.ACTION"), null);
		int seenActivity = 0, seenApplication = 0;
		for (ReceiverRegistry.Registration reg : registry) {
			if (reg.owner == activity)
				seenActivity++;
			else if (reg.owner == application)
				seenApplication++;
		}
		assertEquals(1, seenActivity);
		assertEquals(1, seenApplication);
		assertTrue(registry.removeAll(activity, r));
		for (ReceiverRegistry.Registration reg : registry)
			assertSame("the remaining registration keeps its own Context", application, reg.owner);
	}
}
