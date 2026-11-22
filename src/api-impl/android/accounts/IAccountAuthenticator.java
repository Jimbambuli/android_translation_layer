package android.accounts;

import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

/* AOSP IAccountAuthenticator.aidl as the Java interface aidl generates, without the Proxy and onTransact(): the account manager
 * service and the authenticators run in the app's process, so Stub.asInterface() returns the implementation
 * itself */
public interface IAccountAuthenticator extends android.os.IInterface {
	public static abstract class Stub extends android.os.Binder implements IAccountAuthenticator {
		private static final String DESCRIPTOR = "android.accounts.IAccountAuthenticator";

		public Stub() {
			attachInterface(this, DESCRIPTOR);
		}

		public static IAccountAuthenticator asInterface(IBinder obj) {
			if (obj == null)
				return null;
			return (IAccountAuthenticator)obj.queryLocalInterface(DESCRIPTOR);
		}

		@Override
		public IBinder asBinder() {
			return this;
		}
	}

	void addAccount(IAccountAuthenticatorResponse response, String accountType, String authTokenType,
	                String[] requiredFeatures, Bundle options) throws RemoteException;
	void confirmCredentials(IAccountAuthenticatorResponse response, Account account, Bundle options) throws RemoteException;
	void getAuthToken(IAccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options)
	    throws RemoteException;
	void getAuthTokenLabel(IAccountAuthenticatorResponse response, String authTokenType) throws RemoteException;
	void updateCredentials(IAccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options)
	    throws RemoteException;
	void editProperties(IAccountAuthenticatorResponse response, String accountType) throws RemoteException;
	void hasFeatures(IAccountAuthenticatorResponse response, Account account, String[] features) throws RemoteException;
	void getAccountRemovalAllowed(IAccountAuthenticatorResponse response, Account account) throws RemoteException;
	void getAccountCredentialsForCloning(IAccountAuthenticatorResponse response, Account account) throws RemoteException;
	void addAccountFromCredentials(IAccountAuthenticatorResponse response, Account account, Bundle accountCredentials)
	    throws RemoteException;
	void startAddAccountSession(IAccountAuthenticatorResponse response, String accountType, String authTokenType,
	                            String[] requiredFeatures, Bundle options) throws RemoteException;
	void startUpdateCredentialsSession(IAccountAuthenticatorResponse response, Account account, String authTokenType,
	                                   Bundle options) throws RemoteException;
	void finishSession(IAccountAuthenticatorResponse response, String accountType, Bundle sessionBundle)
	    throws RemoteException;
	void isCredentialsUpdateSuggested(IAccountAuthenticatorResponse response, Account account, String statusToken)
	    throws RemoteException;
}
