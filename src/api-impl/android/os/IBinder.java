package android.os;

public interface IBinder {

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

	public boolean transact(int code, Parcel data, Parcel reply, int flags);

	public String getInterfaceDescriptor();
}
