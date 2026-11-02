package android.accounts;

import android.content.Context;
import android.os.Bundle;
import android.os.IBinder;

/* ATL: no account manager service binds to authenticators, so only the app-facing part (as in AOSP) */
public abstract class AbstractAccountAuthenticator {
	public static final String KEY_CUSTOM_TOKEN_EXPIRY = "android.accounts.expiry";

	public AbstractAccountAuthenticator(Context context) {}

	public final IBinder getIBinder() {
		return null;
	}

	public abstract Bundle editProperties(AccountAuthenticatorResponse response, String accountType);

	public abstract Bundle addAccount(AccountAuthenticatorResponse response, String accountType, String authTokenType,
	                                  String[] requiredFeatures, Bundle options) throws NetworkErrorException;

	public abstract Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account, Bundle options)
	    throws NetworkErrorException;

	public abstract Bundle getAuthToken(AccountAuthenticatorResponse response, Account account, String authTokenType,
	                                    Bundle options) throws NetworkErrorException;

	public abstract String getAuthTokenLabel(String authTokenType);

	public abstract Bundle updateCredentials(AccountAuthenticatorResponse response, Account account, String authTokenType,
	                                         Bundle options) throws NetworkErrorException;

	public abstract Bundle hasFeatures(AccountAuthenticatorResponse response, Account account, String[] features)
	    throws NetworkErrorException;

	public Bundle getAccountRemovalAllowed(AccountAuthenticatorResponse response, Account account) throws NetworkErrorException {
		Bundle result = new Bundle();
		result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, true);
		return result;
	}

	public Bundle getAccountCredentialsForCloning(AccountAuthenticatorResponse response, Account account) throws NetworkErrorException {
		Bundle result = new Bundle();
		result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false);
		return result;
	}

	public Bundle addAccountFromCredentials(AccountAuthenticatorResponse response, Account account, Bundle accountCredentials) throws NetworkErrorException {
		Bundle result = new Bundle();
		result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false);
		return result;
	}
}
