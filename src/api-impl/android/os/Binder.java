package android.os;

import android.atl.ATLLoadedApp;
import android.content.Context;

public class Binder implements IBinder {

	private String mDescriptor;
	private IInterface mOwner;

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
	public static int getCallingPid() { return android.atl.ATLBinderConnection.getCallingPid(); }

	@Override
	public boolean equals(Object obj) {
		return obj instanceof Binder;
	}
}
