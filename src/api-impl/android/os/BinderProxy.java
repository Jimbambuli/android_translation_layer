package android.os;

import android.atl.ATLBinderConnection;
import android.util.Slog;
import java.util.ArrayList;

/**
 * AOSP BinderProxy: a Binder object of another process of the app. Its transactions go over the
 * ATLBinderConnection to that process, where the object executes them.
 */
public final class BinderProxy implements IBinder {
	private final ATLBinderConnection connection;
	private final int handle;
	private volatile boolean dead;
	private final ArrayList<DeathRecipient> recipients = new ArrayList<>();

	/** @hide */
	public BinderProxy(ATLBinderConnection connection, int handle) {
		this.connection = connection;
		this.handle = handle;
	}

	/** @hide */
	public ATLBinderConnection atl_getConnection() {
		return connection;
	}

	/** @hide */
	public int atl_getHandle() {
		return handle;
	}

	@Override
	public IInterface queryLocalInterface(String descriptor) {
		return null;
	}

	@Override
	public boolean transact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
		return connection.transact(handle, code, data, reply, flags);
	}

	@Override
	public String getInterfaceDescriptor() {
		Parcel data = Parcel.obtain();
		Parcel reply = Parcel.obtain();
		try {
			transact(INTERFACE_TRANSACTION, data, reply, 0);
		} catch (RemoteException e) {
			return null;
		}
		return reply.readString();
	}

	@Override
	public boolean pingBinder() {
		return !dead;
	}

	@Override
	public boolean isBinderAlive() {
		return !dead;
	}

	@Override
	public void linkToDeath(DeathRecipient recipient, int flags) throws RemoteException {
		synchronized (recipients) {
			if (dead)
				throw new DeadObjectException();
			recipients.add(recipient);
		}
	}

	@Override
	public boolean unlinkToDeath(DeathRecipient recipient, int flags) {
		synchronized (recipients) {
			if (dead)
				throw new java.util.NoSuchElementException("death link does not exist");
			return recipients.remove(recipient);
		}
	}

	/** @hide the process of the object is gone */
	public void atl_markDead() {
		dead = true;
	}

	/** @hide BinderProxy.sendDeathNotice() */
	public void atl_sendObituary() {
		ArrayList<DeathRecipient> list;
		synchronized (recipients) {
			list = new ArrayList<>(recipients);
			recipients.clear();
		}
		for (DeathRecipient r : list) {
			try {
				r.binderDied(this);
			} catch (RuntimeException e) {
				Slog.w("BinderProxy", "uncaught exception in binderDied()", e);
			}
		}
	}
}
