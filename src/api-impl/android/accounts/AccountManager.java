package android.accounts;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;

public class AccountManager {
	public static final String KEY_ACCOUNT_NAME = "authAccount";
	public static final String KEY_ACCOUNT_TYPE = "accountType";
	public static final String KEY_AUTHTOKEN = "authtoken";
	public static final String KEY_INTENT = "intent";
	public static final String KEY_PASSWORD = "password";
	public static final String KEY_USERDATA = "userdata";
	public static final String KEY_BOOLEAN_RESULT = "booleanResult";
	public static final String KEY_ERROR_CODE = "errorCode";
	public static final String KEY_ERROR_MESSAGE = "errorMessage";
	public static final String KEY_ACCOUNT_AUTHENTICATOR_RESPONSE = "accountAuthenticatorResponse";
	public static final String KEY_ACCOUNT_MANAGER_RESPONSE = "accountManagerResponse";
	public static final String KEY_AUTH_FAILED_MESSAGE = "authFailedMessage";
	public static final String KEY_AUTH_TOKEN_LABEL = "authTokenLabelKey";
	public static final String KEY_CALLER_UID = "callerUid";
	public static final String KEY_CALLER_PID = "callerPid";
	public static final String KEY_ANDROID_PACKAGE_NAME = "androidPackageName";
	public static final String KEY_NOTIFY_ON_FAILURE = "notifyOnAuthFailure";
	public static final int ERROR_CODE_REMOTE_EXCEPTION = 1;
	public static final int ERROR_CODE_NETWORK_ERROR = 3;
	public static final int ERROR_CODE_CANCELED = 4;
	public static final int ERROR_CODE_INVALID_RESPONSE = 5;
	public static final int ERROR_CODE_UNSUPPORTED_OPERATION = 6;
	public static final int ERROR_CODE_BAD_ARGUMENTS = 7;
	public static final int ERROR_CODE_BAD_REQUEST = 8;
	public static final int ERROR_CODE_BAD_AUTHENTICATION = 9;
	public static final String ACTION_AUTHENTICATOR_INTENT = "android.accounts.AccountAuthenticator";
	public static final String AUTHENTICATOR_META_DATA_NAME = "android.accounts.AccountAuthenticator";
	public static final String AUTHENTICATOR_ATTRIBUTES_NAME = "account-authenticator";
	public static final String LOGIN_ACCOUNTS_CHANGED_ACTION = "android.accounts.LOGIN_ACCOUNTS_CHANGED";

	public static AccountManager get(Context context) {
		return new AccountManager();
	}

	public Account[] getAccountsByType(String type) {
		return new Account[0];
	}

	public Account[] getAccounts() {
		return new Account[0];
	}

	public boolean addAccountExplicitly(Account account, String password, Bundle extras) {
		System.out.println("addAccountExplicitly(" + account + ", " + password + ", " + extras + ") called");
		return false;
	}

	/* there are no accounts, so they never change */
	public void addOnAccountsUpdatedListener(OnAccountsUpdateListener listener, Handler handler, boolean updateImmediately) {
		if (updateImmediately && listener != null)
			listener.onAccountsUpdated(getAccounts());
	}

	public void addOnAccountsUpdatedListener(OnAccountsUpdateListener listener, Handler handler, boolean updateImmediately, String[] accountTypes) {
		addOnAccountsUpdatedListener(listener, handler, updateImmediately);
	}

	public void removeOnAccountsUpdatedListener(OnAccountsUpdateListener listener) {}
}
