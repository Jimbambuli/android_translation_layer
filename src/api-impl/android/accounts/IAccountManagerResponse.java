package android.accounts;

import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

/* AOSP IAccountManagerResponse.aidl as the Java interface aidl generates, without the Proxy and onTransact(): the account manager
 * service and the authenticators run in the app's process, so Stub.asInterface() returns the implementation
 * itself */
public interface IAccountManagerResponse extends android.os.IInterface {
	public static abstract class Stub extends android.os.Binder implements IAccountManagerResponse {
		private static final String DESCRIPTOR = "android.accounts.IAccountManagerResponse";

		public Stub() {
			attachInterface(this, DESCRIPTOR);
		}

		public static IAccountManagerResponse asInterface(IBinder obj) {
			if (obj == null)
				return null;
			return (IAccountManagerResponse)obj.queryLocalInterface(DESCRIPTOR);
		}

		@Override
		public IBinder asBinder() {
			return this;
		}
	}

	void onResult(Bundle value) throws RemoteException;
	void onError(int errorCode, String errorMessage) throws RemoteException;
}
