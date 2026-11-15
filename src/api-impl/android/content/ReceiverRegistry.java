package android.content;

import android.os.Handler;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/* AOSP ActivityManagerService.mRegisteredReceivers / ReceiverList and LoadedApk.mReceivers: the BroadcastReceiver
 * object is the key and owns every filter it was registered with. One receiver with two filters (or twice with
 * the same one) is several entries, two receivers with equal filters are two entries, and removeAll() drops all
 * entries of one receiver. scheduler is the Handler the receiver was registered with (null: none given). */
class ReceiverRegistry implements Iterable<ReceiverRegistry.Registration> {
	static class Registration {
		final BroadcastReceiver receiver;
		final IntentFilter filter;
		final Handler scheduler;

		Registration(BroadcastReceiver receiver, IntentFilter filter, Handler scheduler) {
			this.receiver = receiver;
			this.filter = filter;
			this.scheduler = scheduler;
		}
	}

	private final List<Registration> registrations = new CopyOnWriteArrayList<>();

	void add(BroadcastReceiver receiver, IntentFilter filter, Handler scheduler) {
		registrations.add(new Registration(receiver, filter, scheduler));
	}

	/* true when the receiver was registered at all (identity, null never is) */
	boolean removeAll(BroadcastReceiver receiver) {
		if (receiver == null)
			return false;
		boolean removed = false;
		for (Registration registration : registrations) {
			if (registration.receiver == receiver && registrations.remove(registration))
				removed = true;
		}
		return removed;
	}

	public Iterator<Registration> iterator() {
		return registrations.iterator();
	}
}
