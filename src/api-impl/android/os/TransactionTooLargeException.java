package android.os;

/* AOSP: a Binder transaction that was too large; DeadObjectException's sibling (RemoteException) */
public class TransactionTooLargeException extends RemoteException {
	public TransactionTooLargeException() {
		super();
	}

	public TransactionTooLargeException(String msg) {
		super(msg);
	}
}
