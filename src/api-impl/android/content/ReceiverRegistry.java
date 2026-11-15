package android.content;

import android.os.Handler;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/* AOSP ActivityManagerService.mRegisteredReceivers / ReceiverList and LoadedApk.mReceivers: the BroadcastReceiver
 * object is the key and owns every filter it was registered with. One receiver with two filters (or twice with
 * the same one) is several entries, two receivers with equal filters are two entries, and removeAll() drops all
 * entries of one receiver. As LoadedApk.mReceivers the bookkeeping is per Context (owner, compared by identity):
 * the same receiver registered with two Contexts is two dispatchers, and unregistering it with one of them leaves
 * the other registered. Delivery goes to every registration, whatever its owner (ActivityManagerService is
 * system wide); onReceive() gets the owner, as LoadedApk.ReceiverDispatcher keeps the registering Context
 * (mContext) and not the sender's. scheduler is the Handler the receiver was registered with (null: none given). */
class ReceiverRegistry implements Iterable<ReceiverRegistry.Registration> {
	static class Registration {
		final Object owner;
		final BroadcastReceiver receiver;
		final IntentFilter filter;
		final Handler scheduler;

		Registration(Object owner, BroadcastReceiver receiver, IntentFilter filter, Handler scheduler) {
			this.owner = owner;
			this.receiver = receiver;
			this.filter = filter;
			this.scheduler = scheduler;
		}
	}

	private final List<Registration> registrations = new CopyOnWriteArrayList<>();

	void add(Object owner, BroadcastReceiver receiver, IntentFilter filter, Handler scheduler) {
		registrations.add(new Registration(owner, receiver, filter, scheduler));
	}

	/* true when the receiver was registered with this owner (identity, null never is) */
	boolean removeAll(Object owner, BroadcastReceiver receiver) {
		if (receiver == null)
			return false;
		boolean removed = false;
		for (Registration registration : registrations) {
			if (registration.owner == owner && registration.receiver == receiver && registrations.remove(registration))
				removed = true;
		}
		return removed;
	}

	public Iterator<Registration> iterator() {
		return registrations.iterator();
	}
}
