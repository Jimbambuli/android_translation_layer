package android.atl;

import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Slog;
import java.io.FileDescriptor;
import java.util.ArrayDeque;
import java.util.WeakHashMap;

/**
 * The binder of a service of another app (microG, its Companion) as the binding app gets it. On Android that service
 * runs in the other app's process, and a oneway transaction (IBinder.FLAG_ONEWAY) "returns immediately, without
 * waiting for a result from the callee"; the other process executes the oneway calls to one IBinder on its binder
 * threads one at a time, in call order. ATL loads the other app into this process, so without this the caller's
 * thread, often the main thread, would execute the call and wait for it (Companion's checkLicense() blocks in
 * runBlocking until an AccountManager callback runs on the main thread). Everything else goes to the binder itself.
 */
final class ATLOtherAppBinder implements IBinder {
	private static final String TAG = "ATLOtherAppBinder";
	/* one per binder, like the BinderProxy of a handle: the order holds for all connections to the service */
	private static final WeakHashMap<IBinder, ATLOtherAppBinder> wrappers = new WeakHashMap<>();

	private final IBinder binder;
	private final ArrayDeque<Runnable> oneway = new ArrayDeque<>();
	private boolean dispatching;

	private ATLOtherAppBinder(IBinder binder) {
		this.binder = binder;
	}

	static IBinder of(IBinder binder) {
		if (binder == null)
			return null;
		synchronized (wrappers) {
			ATLOtherAppBinder w = wrappers.get(binder);
			if (w == null)
				wrappers.put(binder, w = new ATLOtherAppBinder(binder));
			return w;
		}
	}

	@Override
	public boolean transact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
		if ((flags & FLAG_ONEWAY) == 0)
			return binder.transact(code, data, reply, flags);
		synchronized (oneway) {
			oneway.add(() -> {
				try {
					binder.transact(code, data, null, flags);
				} catch (RemoteException | RuntimeException e) {
					// AOSP Binder.execTransactInternal(): an exception of a oneway call only goes to the log
					Slog.w(TAG, "Caught an exception from the binder stub implementation.", e);
				}
			});
			if (!dispatching) {
				dispatching = true;
				ATLBinderConnection.executeOnBinderThread(this::dispatch);
			}
		}
		return true;
	}

	private void dispatch() {
		while (true) {
			Runnable call;
			synchronized (oneway) {
				call = oneway.poll();
				if (call == null) {
					dispatching = false;
					return;
				}
			}
			call.run();
		}
	}

	@Override
	public IInterface queryLocalInterface(String descriptor) {
		return binder.queryLocalInterface(descriptor);
	}

	@Override
	public String getInterfaceDescriptor() {
		return binder.getInterfaceDescriptor();
	}

	@Override
	public void linkToDeath(DeathRecipient recipient, int flags) throws RemoteException {
		binder.linkToDeath(recipient, flags);
	}

	@Override
	public boolean unlinkToDeath(DeathRecipient recipient, int flags) {
		return binder.unlinkToDeath(recipient, flags);
	}

	@Override
	public boolean pingBinder() {
		return binder.pingBinder();
	}

	@Override
	public boolean isBinderAlive() {
		return binder.isBinderAlive();
	}

	@Override
	public void dump(FileDescriptor fd, String[] args) throws RemoteException {
		binder.dump(fd, args);
	}

	@Override
	public void dumpAsync(FileDescriptor fd, String[] args) throws RemoteException {
		binder.dumpAsync(fd, args);
	}
}
