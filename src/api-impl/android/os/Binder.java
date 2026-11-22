package android.os;

import android.atl.ATLLoadedApp;
import android.annotation.NonNull;
import android.content.Context;
import android.util.ExceptionUtils;
import com.android.internal.util.FunctionalUtils.ThrowingRunnable;
import com.android.internal.util.FunctionalUtils.ThrowingSupplier;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintWriter;

public class Binder implements IBinder {

	private String mDescriptor;
	private IInterface mOwner;

	public Binder() {}

	/* AOSP Binder(String): the descriptor identifies the binder for debugging */
	public Binder(String descriptor) {
		mDescriptor = descriptor;
	}

	public void attachInterface(IInterface owner, String descriptor) {
		mOwner = owner;
		mDescriptor = descriptor;
	}

	public String getInterfaceDescriptor() {
		return mDescriptor;
	}

	public IInterface getOwner() {
		return mOwner;
	}

	public static void flushPendingCommands() {}

	public static long clearCallingIdentity() { return 0; }

	public static void restoreCallingIdentity(long identityToken) {}

	/* AOSP Binder.queryLocalInterface(): in the process that implements the interface, Stub.asInterface()
	 * gets the implementation itself instead of a proxy */
	@Override
	public IInterface queryLocalInterface(String descriptor) {
		if (mDescriptor != null && mDescriptor.equals(descriptor)) {
			return mOwner;
		}
		return null;
	}

	/* AOSP Binder.transact(): the call stays in the process, onTransact() handles it */
	@Override
	public final boolean transact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
		if (data != null) {
			data.setDataPosition(0);
		}
		boolean r = onTransact(code, data, reply, flags);
		if (reply != null) {
			reply.setDataPosition(0);
		}
		return r;
	}

	/* AOSP Binder.dump(FileDescriptor, String[]): calls the more convenient dump(FileDescriptor, PrintWriter, String[])
	 * (PrintWriter instead of FastPrintWriter, same output); AOSP's doDump() also prints exceptions into the dump */
	public void dump(FileDescriptor fd, String[] args) {
		FileOutputStream fout = new FileOutputStream(fd);
		PrintWriter pw = new PrintWriter(fout);
		try {
			try {
				dump(fd, pw, args);
			} catch (SecurityException e) {
				pw.println("Security exception: " + e.getMessage());
				throw e;
			} catch (Throwable e) {
				pw.println();
				pw.println("Exception occurred while dumping:");
				e.printStackTrace(pw);
			}
		} finally {
			pw.flush();
		}
	}

	/* AOSP Binder.dumpAsync(): like dump(), but on its own thread */
	public void dumpAsync(final FileDescriptor fd, final String[] args) {
		final FileOutputStream fout = new FileOutputStream(fd);
		final PrintWriter pw = new PrintWriter(fout);
		Thread thr = new Thread("Binder.dumpAsync") {
			public void run() {
				try {
					dump(fd, pw, args);
				} finally {
					pw.flush();
				}
			}
		};
		thr.start();
	}

	protected void dump(FileDescriptor fd, PrintWriter fout, String[] args) {
	}

	/* AOSP Binder.onTransact() without dump, shell command and sysprops transactions */
	protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
		if (code == INTERFACE_TRANSACTION) {
			reply.writeString(getInterfaceDescriptor());
			return true;
		}
		return false;
	}

	public static int getCallingUid() { return ATLLoadedApp.getPrimaryApplication().pkg.applicationInfo.uid; }

	/* the process of the transaction this thread executes (ATLBinderConnection), else this one like AOSP */
	/* AOSP Binder.getCallingUidOrThrow(): ATL does not track transactions, every caller counts as handling one */
	public static final int getCallingUidOrThrow() { return getCallingUid(); }

	public static int getCallingPid() { return android.atl.ATLBinderConnection.getCallingPid(); }

	/* AOSP Binder.getCallingUserHandle(): the user of the calling uid */
	public static final UserHandle getCallingUserHandle() {
		return UserHandle.getUserHandleForUid(getCallingUid());
	}

	/* AOSP Binder.setCallingWorkSourceUid() and friends are native (IPCThreadState: work source uid and a propagate
	 * flag per thread, the token packs both). ATL keeps the two values per thread; nothing propagates them, as
	 * transactions stay in the process. */
	private static final int UNSET_WORKSOURCE = -1;
	private static final ThreadLocal<int[]> sWorkSource = ThreadLocal.withInitial(() -> new int[] {UNSET_WORKSOURCE, 0});

	public static final long setCallingWorkSourceUid(int workSource) {
		int[] state = sWorkSource.get();
		long token = ((long)state[1] << 32) | (state[0] & 0xffffffffL);
		state[0] = workSource;
		state[1] = 1;
		return token;
	}

	public static final int getCallingWorkSourceUid() {
		return sWorkSource.get()[0];
	}

	public static final long clearCallingWorkSource() {
		return setCallingWorkSourceUid(UNSET_WORKSOURCE);
	}

	public static final void restoreCallingWorkSource(long token) {
		int[] state = sWorkSource.get();
		state[0] = (int)token;
		state[1] = (int)(token >> 32);
	}

	/* AOSP Binder.joinThreadPool(): BinderInternal.joinThreadPool() never returns; ATL has no binder threads to serve,
	 * so the thread just stays here (until it is interrupted) */
	public static final void joinThreadPool() {
		try {
			while (true) {
				Thread.sleep(Long.MAX_VALUE);
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	/* AOSP Binder.pingBinder()/isBinderAlive(): a local Binder is alive; linkToDeath() does nothing and
	 * unlinkToDeath() returns true (it never dies) */
	@Override
	public boolean pingBinder() {
		return true;
	}

	@Override
	public boolean isBinderAlive() {
		return true;
	}

	@Override
	public void linkToDeath(DeathRecipient recipient, int flags) {}

	@Override
	public boolean unlinkToDeath(DeathRecipient recipient, int flags) {
		return true;
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof Binder;
	}

    /**
     * Convenience method for running the provided action enclosed in
     * {@link #clearCallingIdentity}/{@link #restoreCallingIdentity}.
     *
     * <p>Any exception thrown by the given action will be caught and
     * rethrown after the call to {@link #restoreCallingIdentity}.
     *
     * @hide
     */
    public static final void withCleanCallingIdentity(@NonNull ThrowingRunnable action) {
        Throwable throwableToPropagate = null;
        final long callingIdentity = clearCallingIdentity();
        try {
            action.runOrThrow();
        } catch (Throwable throwable) {
            throwableToPropagate = throwable;
        } finally {
            restoreCallingIdentity(callingIdentity);
            if (throwableToPropagate != null) {
                throw ExceptionUtils.propagate(throwableToPropagate);
            }
        }
    }

    /**
     * Convenience method for running the provided action enclosed in
     * {@link #clearCallingIdentity}/{@link #restoreCallingIdentity} returning the result.
     *
     * <p>Any exception thrown by the given action will be caught and rethrown after
     * the call to {@link #restoreCallingIdentity}.
     *
     * @hide
     */
    public static final <T> T withCleanCallingIdentity(@NonNull ThrowingSupplier<T> action) {
        Throwable throwableToPropagate = null;
        final long callingIdentity = clearCallingIdentity();
        try {
            return action.getOrThrow();
        } catch (Throwable throwable) {
            throwableToPropagate = throwable;
            return null; // overridden by throwing in finally block
        } finally {
            restoreCallingIdentity(callingIdentity);
            if (throwableToPropagate != null) {
                throw ExceptionUtils.propagate(throwableToPropagate);
            }
        }
    }
}
