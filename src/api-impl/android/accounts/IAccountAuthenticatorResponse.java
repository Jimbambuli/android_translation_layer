package android.accounts;

import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

/* AOSP IAccountAuthenticatorResponse.aidl as the Java interface aidl generates, without the Proxy and onTransact(): the account manager
 * service and the authenticators run in the app's process, so Stub.asInterface() returns the implementation
 * itself */
public interface IAccountAuthenticatorResponse extends android.os.IInterface {
	public static abstract class Stub extends android.os.Binder implements IAccountAuthenticatorResponse {
		private static final String DESCRIPTOR = "android.accounts.IAccountAuthenticatorResponse";

		public Stub() {
			attachInterface(this, DESCRIPTOR);
		}

		public static IAccountAuthenticatorResponse asInterface(IBinder obj) {
			if (obj == null)
				return null;
			return (IAccountAuthenticatorResponse)obj.queryLocalInterface(DESCRIPTOR);
		}

		@Override
		public IBinder asBinder() {
			return this;
		}
	}

	void onResult(Bundle value) throws RemoteException;
	void onRequestContinued() throws RemoteException;
	void onError(int errorCode, String errorMessage) throws RemoteException;
}
