package android.os;

import android.atl.ATLLoadedApp;
import android.content.Context;
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

	@Override
	public boolean equals(Object obj) {
		return obj instanceof Binder;
	}
}
