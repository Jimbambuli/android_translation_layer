package android.os;

public interface IBinder {
	/** AOSP IBinder.MAX_IPC_SIZE: the size of a transaction that is safe; ATL has no binder transactions */
	int MAX_IPC_SIZE = 64 * 1024;

	/** AOSP IBinder.getSuggestedMaxIpcSizeBytes(): the largest payload to send in one transaction */
	static int getSuggestedMaxIpcSizeBytes() {
		return MAX_IPC_SIZE;
	}


	int FIRST_CALL_TRANSACTION = 0x00000001;
	int LAST_CALL_TRANSACTION = 0x00ffffff;
	int INTERFACE_TRANSACTION = ('_' << 24) | ('N' << 16) | ('T' << 8) | 'F';
	int FLAG_ONEWAY = 0x00000001;

	/* AOSP IBinder.DeathRecipient; a binder of the app's own process never dies */
	public interface DeathRecipient {
		public void binderDied();

		default void binderDied(IBinder who) {
			binderDied();
		}
	}

	/* abstract in AOSP; here a no-op by default for ATL's own binders (a binder of the process never dies) */
	public default void linkToDeath(DeathRecipient recipient, int flags) throws RemoteException {}

	public default boolean unlinkToDeath(DeathRecipient recipient, int flags) {
		return true;
	}

	public IInterface queryLocalInterface(String descriptor);

	/* the object lives in this process (ATL has no other), so it is alive as AOSP's local Binder */
	public default boolean pingBinder() {
		return true;
	}

	public default boolean isBinderAlive() {
		return true;
	}

	public boolean transact(int code, Parcel data, Parcel reply, int flags) throws RemoteException;

	public String getInterfaceDescriptor();
}
