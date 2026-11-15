package android.accounts;

import android.atl.ATLSecrets;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/*
 * Accounts are stored in the Secret Service keyring and shared between all apps,
 * the same way they are shared on Android.
 */
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

	/**
	 * Account is always visible to given application and only authenticator can revoke visibility.
	 */
	public static final int VISIBILITY_VISIBLE = 1;

	/**
	 * Account is visible to given application, but user can revoke visibility.
	 */
	public static final int VISIBILITY_USER_MANAGED_VISIBLE = 2;

	/**
	 * Account is not visible to given application and only authenticator can grant visibility.
	 */
	public static final int VISIBILITY_NOT_VISIBLE = 3;

	private static final AccountManager instance = new AccountManager();

	public static AccountManager get(Context context) {
		return instance;
	}

	public Account[] getAccounts() {
		return getAccountsByType(null);
	}

	public Account[] getAccountsByType(String type) {
		ArrayList<Account> accounts = new ArrayList<>();
		String[] list = (ATLSecrets.mayHoldSecrets() ? nativeListAccounts() : "").split("\n");
		for (int i = 0; i + 1 < list.length; i += 2) {
			if (type == null || type.equals(list[i]))
				accounts.add(new Account(list[i + 1], list[i]));
		}
		return accounts.toArray(new Account[0]);
	}

	/* AOSP getAccountsByTypeAndFeatures(): the accounts of the type whose authenticator has all the features. ATL's
	 * accounts come from the keyring, their authenticators (microG) are not asked: no feature is known, so a
	 * request with features finds none */
	public AccountManagerFuture<Account[]> getAccountsByTypeAndFeatures(final String type, final String[] features,
	                                                                  AccountManagerCallback<Account[]> callback, Handler handler) {
		if (type == null)
			throw new IllegalArgumentException("type is null");
		return runAsync(new Callable<Account[]>() {
			@Override
			public Account[] call() {
				if (features != null && features.length > 0)
					return new Account[0];
				return getAccountsByType(type);
			}
		}, callback, handler);
	}

	public Account[] getAccountsByTypeForPackage(String type, String packageName) {
		return getAccountsByType(type);
	}

	public int getAccountVisibility(Account account, String packageName) {
		return VISIBILITY_VISIBLE;
	}

	public boolean addAccountExplicitly(Account account, String password, Bundle extras) {
		if (keyringGet(account.type, account.name, "account") != null)
			return false;
		if (!nativeSet(account.type, account.name, "account", ""))
			return false;
		setPassword(account, password);
		if (extras != null) {
			for (String key : extras.keySet())
				setUserData(account, key, extras.getString(key));
		}
		return true;
	}

	/* AOSP: AccountManagerService binds the authenticator of the account type, which adds the account (usually with
	 * its own activity). ATL knows no authenticators (see getAccountsByTypeAndFeatures), so the bind fails as for a
	 * type without one: onError(ERROR_CODE_REMOTE_EXCEPTION, "bind failure") -> AuthenticatorException */
	public AccountManagerFuture<Bundle> addAccount(final String accountType, final String authTokenType,
	                                               final String[] requiredFeatures, final Bundle addAccountOptions,
	                                               final Activity activity, AccountManagerCallback<Bundle> callback, Handler handler) {
		if (accountType == null)
			throw new IllegalArgumentException("accountType is null");
		return runAsync(new Callable<Bundle>() {
			@Override
			public Bundle call() throws Exception {
				throw new AuthenticatorException("bind failure");
			}
		}, callback, handler);
	}

	/* AOSP getAuthToken() (all three overloads): AccountManagerService asks the authenticator of the account type
	 * for the token. ATL knows no authenticators (see addAccount), so the bind fails as for a type without one:
	 * AuthenticatorException "bind failure" from the future. The argument checks are AOSP's. */
	public AccountManagerFuture<Bundle> getAuthToken(final Account account, final String authTokenType, final Bundle options,
	                                                 final Activity activity, AccountManagerCallback<Bundle> callback, Handler handler) {
		if (account == null)
			throw new IllegalArgumentException("account is null");
		if (authTokenType == null)
			throw new IllegalArgumentException("authTokenType is null");
		return runAsync(new Callable<Bundle>() {
			@Override
			public Bundle call() throws Exception {
				throw new AuthenticatorException("bind failure");
			}
		}, callback, handler);
	}

	public AccountManagerFuture<Bundle> getAuthToken(final Account account, final String authTokenType,
	                                                 final boolean notifyAuthFailure, AccountManagerCallback<Bundle> callback, Handler handler) {
		return getAuthToken(account, authTokenType, null, notifyAuthFailure, callback, handler);
	}

	public AccountManagerFuture<Bundle> getAuthToken(final Account account, final String authTokenType, final Bundle options,
	                                                 final boolean notifyAuthFailure, AccountManagerCallback<Bundle> callback, Handler handler) {
		return getAuthToken(account, authTokenType, options, (Activity)null, callback, handler);
	}

	/* AOSP blockingGetAuthToken() */
	public String blockingGetAuthToken(Account account, String authTokenType, boolean notifyAuthFailure)
	    throws OperationCanceledException, IOException, AuthenticatorException {
		if (account == null)
			throw new IllegalArgumentException("account is null");
		if (authTokenType == null)
			throw new IllegalArgumentException("authTokenType is null");
		Bundle bundle = getAuthToken(account, authTokenType, notifyAuthFailure, null, null).getResult();
		if (bundle == null)
			return null;
		return bundle.getString(KEY_AUTHTOKEN);
	}

	/**
	 * Removes an account from the AccountManager.  Does nothing if the account
	 * does not exist.  Does not delete the account from the server.
	 * The authenticator may have its own policies preventing account
	 * deletion, in which case the account will not be deleted.
	 *
	 * <p>This method requires the caller to have a signature match with the
	 * authenticator that manages the specified account.
	 *
	 * <p><b>NOTE:</b> If targeting your app to work on API level 22 and before,
	 * MANAGE_ACCOUNTS permission is needed for those platforms. See docs for
	 * this function in API level 22.
	 *
	 * @param account The {@link Account} to remove
	 * @param callback Callback to invoke when the request completes,
	 *     null for no callback
	 * @param handler {@link Handler} identifying the callback thread,
	 *     null for the main thread
	 * @return An {@link AccountManagerFuture} which resolves to a Boolean,
	 *     true if the account has been successfully removed
	 */
	public AccountManagerFuture<Boolean> removeAccount(Account account, AccountManagerCallback<Boolean> callback, Handler handler) {
		// no lambda here, dx can't handle invokedynamic for our min sdk version
		return runAsync(new Callable<Boolean>() {
			@Override
			public Boolean call() {
				return ATLSecrets.mayHoldSecrets() && nativeRemove(account.type, account.name);
			}
		}, callback, handler);
	}

	/*
	 * Runs work off the calling thread like AOSP does, so native keyring calls don't block the UI.
	 * The callback is invoked on the handler's thread (main thread if null) once the work is done.
	 */
	private static <V> AccountManagerFuture<V> runAsync(Callable<V> work, final AccountManagerCallback<V> callback, final Handler handler) {
		class Future extends FutureTask<V> implements AccountManagerFuture<V> {
			Future() {
				super(work);
			}

			@Override
			protected void done() {
				if (callback == null)
					return;
				(handler != null ? handler : new Handler(Looper.getMainLooper())).post(new Runnable() {
					@Override
					public void run() {
						callback.run(Future.this);
					}
				});
			}

			public V getResult() throws OperationCanceledException, IOException, AuthenticatorException {
				return internalGetResult(null, null);
			}

			public V getResult(long timeout, TimeUnit unit) throws OperationCanceledException, IOException, AuthenticatorException {
				return internalGetResult(timeout, unit);
			}

			/* AOSP AmsTask.internalGetResult: the work's exception as the caller expects it */
			private V internalGetResult(Long timeout, TimeUnit unit) throws OperationCanceledException, IOException, AuthenticatorException {
				try {
					if (timeout == null) {
						return get();
					} else {
						return get(timeout, unit);
					}
				} catch (InterruptedException | TimeoutException | CancellationException e) {
					// fall through and cancel
				} catch (ExecutionException e) {
					final Throwable cause = e.getCause();
					if (cause instanceof IOException) {
						throw (IOException) cause;
					} else if (cause instanceof UnsupportedOperationException) {
						throw new AuthenticatorException(cause);
					} else if (cause instanceof AuthenticatorException) {
						throw (AuthenticatorException) cause;
					} else if (cause instanceof RuntimeException) {
						throw (RuntimeException) cause;
					} else if (cause instanceof Error) {
						throw (Error) cause;
					} else {
						throw new IllegalStateException(cause);
					}
				} finally {
					cancel(true /* interrupt if running */);
				}
				throw new OperationCanceledException();
			}
		}
		Future future = new Future();
		AsyncTask.THREAD_POOL_EXECUTOR.execute(future);
		return future;
	}

	/* AOSP removeAccountExplicitly(): the service removes the account without asking its authenticator */
	public boolean removeAccountExplicitly(Account account) {
		if (account == null)
			throw new IllegalArgumentException("account is null");
		return ATLSecrets.mayHoldSecrets() && nativeRemove(account.type, account.name);
	}

	/* AOSP clearPassword(): the service sets the password to null (nativeSet clears the entry for null);
	 * without a keyring there is nothing to clear */
	public void clearPassword(Account account) {
		if (account == null)
			throw new IllegalArgumentException("account is null");
		if (ATLSecrets.mayHoldSecrets())
			setPassword(account, null);
	}

	/* AOSP getAuthenticatorTypes(): the authenticators the AccountManagerService knows; ATL registers none
	 * (see getAccountsByTypeAndFeatures) */
	public AuthenticatorDescription[] getAuthenticatorTypes() {
		return new AuthenticatorDescription[0];
	}

	/* AOSP removeAccount(Account, Activity, ...): AccountManagerService binds the authenticator of the account type to
	 * remove the account (RemoveAccountSession); ATL knows no authenticators (see addAccount), so the bind fails */
	public AccountManagerFuture<Bundle> removeAccount(final Account account, final Activity activity,
	                                                  AccountManagerCallback<Bundle> callback, Handler handler) {
		if (account == null)
			throw new IllegalArgumentException("account is null");
		return runAsync(new Callable<Bundle>() {
			@Override
			public Bundle call() throws Exception {
				throw new AuthenticatorException("bind failure");
			}
		}, callback, handler);
	}

	/* AOSP updateCredentials(): asks the authenticator of the account type; none is bound here (see addAccount) */
	public AccountManagerFuture<Bundle> updateCredentials(final Account account, final String authTokenType, final Bundle options,
	                                                      final Activity activity, AccountManagerCallback<Bundle> callback, Handler handler) {
		if (account == null)
			throw new IllegalArgumentException("account is null");
		return runAsync(new Callable<Bundle>() {
			@Override
			public Bundle call() throws Exception {
				throw new AuthenticatorException("bind failure");
			}
		}, callback, handler);
	}

	public String getPassword(Account account) {
		return keyringGet(account.type, account.name, "password");
	}

	public void setPassword(Account account, String password) {
		nativeSet(account.type, account.name, "password", password);
	}

	public String getUserData(Account account, String key) {
		return keyringGet(account.type, account.name, "userdata:" + key);
	}

	public void setUserData(Account account, String key, String value) {
		nativeSet(account.type, account.name, "userdata:" + key, value);
	}

	public String peekAuthToken(Account account, String authTokenType) {
		return keyringGet(account.type, account.name, "authtoken:" + authTokenType);
	}

	public void setAuthToken(Account account, String authTokenType, String authToken) {
		nativeSet(account.type, account.name, "authtoken:" + authTokenType, authToken);
		/* index of the token types set for the account: the keyring cannot be enumerated by key, but
		 * invalidateAuthToken() has to find the entries by token value */
		String types = keyringGet(account.type, account.name, "authtokentypes");
		if (authToken != null && (types == null || !("\n" + types + "\n").contains("\n" + authTokenType + "\n")))
			nativeSet(account.type, account.name, "authtokentypes", types == null || types.isEmpty() ? authTokenType : types + "\n" + authTokenType);
	}

	/* AOSP AccountManager.invalidateAuthToken(): the argument checks; the cache removal happens in
	 * AccountManagerService (source not available): the cached token is removed for the accounts of the type */
	public void invalidateAuthToken(final String accountType, final String authToken) {
		if (accountType == null)
			throw new IllegalArgumentException("accountType is null");
		if (authToken == null)
			return;
		for (Account account : getAccountsByType(accountType)) {
			String types = keyringGet(account.type, account.name, "authtokentypes");
			if (types == null)
				continue;
			for (String authTokenType : types.split("\n")) {
				if (authToken.equals(keyringGet(account.type, account.name, "authtoken:" + authTokenType)))
					nativeSet(account.type, account.name, "authtoken:" + authTokenType, null);
			}
		}
	}

	/* The extras of ChooseTypeAndAccountActivity (frameworks/base accounts/ChooseTypeAndAccountActivity.java) */
	private static final String EXTRA_ALLOWABLE_ACCOUNTS_ARRAYLIST = "allowableAccounts";
	private static final String EXTRA_ALLOWABLE_ACCOUNT_TYPES_STRING_ARRAY = "allowableAccountTypes";
	private static final String EXTRA_ADD_ACCOUNT_OPTIONS_BUNDLE = "addAccountOptions";
	private static final String EXTRA_ADD_ACCOUNT_REQUIRED_FEATURES_STRING_ARRAY = "addAccountRequiredFeatures";
	private static final String EXTRA_ADD_ACCOUNT_AUTH_TOKEN_TYPE_STRING = "authTokenType";
	private static final String EXTRA_SELECTED_ACCOUNT = "selectedAccount";
	private static final String EXTRA_DESCRIPTION_TEXT_OVERRIDE = "descriptionTextOverride";

	/** AOSP: the deprecated overload drops alwaysPromptForAccount. */
	@Deprecated
	public static Intent newChooseAccountIntent(Account selectedAccount, ArrayList<Account> allowableAccounts,
	                                            String[] allowableAccountTypes, boolean alwaysPromptForAccount,
	                                            String descriptionOverrideText, String addAccountAuthTokenType,
	                                            String[] addAccountRequiredFeatures, Bundle addAccountOptions) {
		return newChooseAccountIntent(selectedAccount, allowableAccounts, allowableAccountTypes, descriptionOverrideText,
		                              addAccountAuthTokenType, addAccountRequiredFeatures, addAccountOptions);
	}

	/* AOSP: only builds the Intent for the system activity named by config_chooseTypeAndAccountActivity; ATL has no
	 * such activity, so starting it fails like on a device without it */
	public static Intent newChooseAccountIntent(Account selectedAccount, List<Account> allowableAccounts,
	                                            String[] allowableAccountTypes, String descriptionOverrideText,
	                                            String addAccountAuthTokenType, String[] addAccountRequiredFeatures,
	                                            Bundle addAccountOptions) {
		Intent intent = new Intent();
		ComponentName componentName = ComponentName.unflattenFromString(
		        Resources.getSystem().getString(com.android.internal.R.string.config_chooseTypeAndAccountActivity));
		intent.setClassName(componentName.getPackageName(), componentName.getClassName());
		intent.putExtra(EXTRA_ALLOWABLE_ACCOUNTS_ARRAYLIST,
		                allowableAccounts == null ? null : new ArrayList<Account>(allowableAccounts));
		intent.putExtra(EXTRA_ALLOWABLE_ACCOUNT_TYPES_STRING_ARRAY, allowableAccountTypes);
		intent.putExtra(EXTRA_ADD_ACCOUNT_OPTIONS_BUNDLE, addAccountOptions);
		intent.putExtra(EXTRA_SELECTED_ACCOUNT, selectedAccount);
		intent.putExtra(EXTRA_DESCRIPTION_TEXT_OVERRIDE, descriptionOverrideText);
		intent.putExtra(EXTRA_ADD_ACCOUNT_AUTH_TOKEN_TYPE_STRING, addAccountAuthTokenType);
		intent.putExtra(EXTRA_ADD_ACCOUNT_REQUIRED_FEATURES_STRING_ARRAY, addAccountRequiredFeatures);
		return intent;
	}

	public boolean setAccountVisibility(Account account, String packageName, int visibility) {
		return true;
	}

	public void addOnAccountsUpdatedListener(OnAccountsUpdateListener listener, Handler handler, boolean updateImmediately) {
		addOnAccountsUpdatedListener(listener, handler, updateImmediately, null);
	}

	/* changes are not reported yet; as AOSP, updateImmediately posts the current accounts to the handler */
	public void addOnAccountsUpdatedListener(final OnAccountsUpdateListener listener,
	                                         Handler handler, boolean updateImmediately, String[] accountTypes) {
		if (listener == null)
			throw new IllegalArgumentException("the listener is null");
		if (!updateImmediately)
			return;
		Account[] all = getAccounts();
		ArrayList<Account> wanted = new ArrayList<Account>();
		for (Account account : all) {
			if (accountTypes == null || java.util.Arrays.asList(accountTypes).contains(account.type))
				wanted.add(account);
		}
		final Account[] accounts = wanted.toArray(new Account[0]);
		(handler != null ? handler : new Handler(Looper.getMainLooper())).post(new Runnable() {
			@Override
			public void run() {
				listener.onAccountsUpdated(accounts);
			}
		});
	}

	public void removeOnAccountsUpdatedListener(OnAccountsUpdateListener listener) {
	}

	public Map<Account, Integer> getAccountsAndVisibilityForPackage(String packageName,
	                                                                String accountType) {
		HashMap<Account, Integer> result = new HashMap<Account, Integer>();
		Account[] accounts = getAccounts();
		for (Account acc : accounts) {
			result.put(acc, VISIBILITY_VISIBLE);
		}
		return result;
	}

	public AccountManagerFuture<Boolean> hasFeatures(Account account, String[] features, AccountManagerCallback<Boolean> callback, Handler handler) {
		return runAsync(new Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {
				return false;
			}
		}, callback, handler);
	}

	/* nothing to read before the keyring file exists (ATLSecrets), and asking would show the desktop's dialog */
	private static String keyringGet(String type, String name, String key) {
		return ATLSecrets.mayHoldSecrets() ? nativeGet(type, name, key) : null;
	}

	private static native String nativeGet(String type, String name, String key);
	// value == null removes the entry, returns false on keyring errors
	private static native boolean nativeSet(String type, String name, String key, String value);
	// returns false if the account didn't exist or on keyring errors
	private static native boolean nativeRemove(String type, String name);
	// returns "type\nname\n" for every account
	private static native String nativeListAccounts();
}
