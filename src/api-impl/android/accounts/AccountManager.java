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
import android.os.RemoteException;
import com.android.server.accounts.AccountManagerService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
	public static final int ERROR_CODE_USER_RESTRICTED = 100;
	public static final int ERROR_CODE_MANAGEMENT_DISABLED_FOR_ACCOUNT_TYPE = 101;
	public static final String KEY_ACCOUNTS = "accounts";
	public static final String KEY_ACCOUNT_SESSION_BUNDLE = "accountSessionBundle";
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
	public static final int VISIBILITY_USER_MANAGED_NOT_VISIBLE = 4;

	/* the caller's context, as AOSP's (SystemServiceRegistry makes one AccountManager per context): its package
	 * is the caller (KEY_ANDROID_PACKAGE_NAME) */
	private final Context mContext;

	/** @hide */
	public AccountManager(Context context) {
		mContext = context;
	}

	public static AccountManager get(Context context) {
		if (context == null)
			throw new IllegalArgumentException("context is null");
		return (AccountManager)context.getSystemService(Context.ACCOUNT_SERVICE);
	}

	private AccountManagerService service() {
		return AccountManagerService.get(mContext);
	}

	/** @hide AOSP getAccountsForPackage(): the accounts the package may see, all of them here (visibility is not kept) */
	public Account[] getAccountsForPackage(String packageName, int uid) {
		return getAccounts();
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

	/* AOSP getAccountsByTypeAndFeatures(): the accounts of the type whose authenticator says it has all the
	 * features (GetAccountsByTypeAndFeatureSession) */
	public AccountManagerFuture<Account[]> getAccountsByTypeAndFeatures(final String type, final String[] features,
	                                                                  AccountManagerCallback<Account[]> callback, Handler handler) {
		if (type == null)
			throw new IllegalArgumentException("type is null");
		return new Future2Task<Account[]>(handler, callback) {
			@Override
			public void doWork() throws RemoteException {
				service().getAccountsByFeatures(mResponse, type, features);
			}

			@Override
			public Account[] bundleToResult(Bundle bundle) throws AuthenticatorException {
				if (!bundle.containsKey(KEY_ACCOUNTS))
					throw new AuthenticatorException("no result in response");
				final android.os.Parcelable[] parcelables = bundle.getParcelableArray(KEY_ACCOUNTS);
				Account[] descs = new Account[parcelables.length];
				for (int i = 0; i < parcelables.length; i++)
					descs[i] = (Account)parcelables[i];
				return descs;
			}
		}.start();
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

	/* AOSP: adds the account, then sets the visibility of every entry of the map */
	public boolean addAccountExplicitly(Account account, String password, Bundle extras, Map<String, Integer> visibility) {
		if (account == null)
			throw new IllegalArgumentException("account is null");
		if (!addAccountExplicitly(account, password, extras))
			return false;
		if (visibility != null) {
			for (Map.Entry<String, Integer> entry : visibility.entrySet())
				setAccountVisibility(account, entry.getKey(), entry.getValue());
		}
		return true;
	}

	/* AOSP addAccount(): AccountManagerService binds the authenticator of the account type, which adds the account
	 * (usually with its own activity, returned as KEY_INTENT or started with the given activity) */
	public AccountManagerFuture<Bundle> addAccount(final String accountType, final String authTokenType,
	                                               final String[] requiredFeatures, final Bundle addAccountOptions,
	                                               final Activity activity, AccountManagerCallback<Bundle> callback, Handler handler) {
		if (accountType == null)
			throw new IllegalArgumentException("accountType is null");
		final Bundle optionsIn = new Bundle();
		if (addAccountOptions != null)
			optionsIn.putAll(addAccountOptions);
		optionsIn.putString(KEY_ANDROID_PACKAGE_NAME, mContext.getPackageName());
		return new AmsTask(activity, handler, callback) {
			@Override
			public void doWork() throws RemoteException {
				service().addAccount(mResponse, accountType, authTokenType, requiredFeatures, activity != null, optionsIn);
			}
		}.start();
	}

	/* AOSP getAuthToken(): AccountManagerService asks the authenticator of the account type for the token */
	public AccountManagerFuture<Bundle> getAuthToken(final Account account, final String authTokenType, final Bundle options,
	                                                 final Activity activity, AccountManagerCallback<Bundle> callback, Handler handler) {
		if (account == null)
			throw new IllegalArgumentException("account is null");
		if (authTokenType == null)
			throw new IllegalArgumentException("authTokenType is null");
		final Bundle optionsIn = new Bundle();
		if (options != null)
			optionsIn.putAll(options);
		optionsIn.putString(KEY_ANDROID_PACKAGE_NAME, mContext.getPackageName());
		return new AmsTask(activity, handler, callback) {
			@Override
			public void doWork() throws RemoteException {
				service().getAuthToken(mResponse, account, authTokenType, false /* notifyOnAuthFailure */, true /* expectActivityLaunch */, optionsIn);
			}
		}.start();
	}

	public AccountManagerFuture<Bundle> getAuthToken(final Account account, final String authTokenType,
	                                                 final boolean notifyAuthFailure, AccountManagerCallback<Bundle> callback, Handler handler) {
		return getAuthToken(account, authTokenType, null, notifyAuthFailure, callback, handler);
	}

	public AccountManagerFuture<Bundle> getAuthToken(final Account account, final String authTokenType, final Bundle options,
	                                                 final boolean notifyAuthFailure, AccountManagerCallback<Bundle> callback, Handler handler) {
		if (account == null)
			throw new IllegalArgumentException("account is null");
		if (authTokenType == null)
			throw new IllegalArgumentException("authTokenType is null");
		final Bundle optionsIn = new Bundle();
		if (options != null)
			optionsIn.putAll(options);
		optionsIn.putString(KEY_ANDROID_PACKAGE_NAME, mContext.getPackageName());
		return new AmsTask(null, handler, callback) {
			@Override
			public void doWork() throws RemoteException {
				service().getAuthToken(mResponse, account, authTokenType, notifyAuthFailure, false /* expectActivityLaunch */, optionsIn);
			}
		}.start();
	}

	/* AOSP confirmCredentials() */
	public AccountManagerFuture<Bundle> confirmCredentials(final Account account, final Bundle options, final Activity activity,
	                                                       final AccountManagerCallback<Bundle> callback, final Handler handler) {
		if (account == null)
			throw new IllegalArgumentException("account is null");
		return new AmsTask(activity, handler, callback) {
			@Override
			public void doWork() throws RemoteException {
				service().confirmCredentials(mResponse, account, options, activity != null);
			}
		}.start();
	}

	/* AOSP editProperties() */
	public AccountManagerFuture<Bundle> editProperties(final String accountType, final Activity activity,
	                                                   final AccountManagerCallback<Bundle> callback, final Handler handler) {
		if (accountType == null)
			throw new IllegalArgumentException("accountType is null");
		return new AmsTask(activity, handler, callback) {
			@Override
			public void doWork() throws RemoteException {
				service().editProperties(mResponse, accountType, activity != null);
			}
		}.start();
	}

	/* AOSP GetAuthTokenByTypeAndFeaturesTask: the accounts of the type with the features; none -> addAccount(), one ->
	 * getAuthToken() of it, several -> a result with the intent of the account chooser (KEY_INTENT) */
	public AccountManagerFuture<Bundle> getAuthTokenByFeatures(final String accountType, final String authTokenType,
	                                                           final String[] features, final Activity activity,
	                                                           final Bundle addAccountOptions, final Bundle getAuthTokenOptions,
	                                                           AccountManagerCallback<Bundle> callback, Handler handler) {
		if (accountType == null)
			throw new IllegalArgumentException("account type is null");
		if (authTokenType == null)
			throw new IllegalArgumentException("authTokenType is null");
		return runAsync(new Callable<Bundle>() {
			@Override
			public Bundle call() throws Exception {
				Account[] accounts = getAccountsByTypeAndFeatures(accountType, features, null, null).getResult();
				if (accounts.length == 0)
					return addAccount(accountType, authTokenType, features, addAccountOptions, activity, null, null).getResult();
				if (accounts.length == 1)
					return getAuthToken(accounts[0], authTokenType, getAuthTokenOptions, activity, null, null).getResult();
				Bundle result = new Bundle();
				result.putParcelable(KEY_INTENT, newChooseAccountIntent(null, new ArrayList<Account>(Arrays.asList(accounts)), null, null,
				                                                        null, null, null));
				return result;
			}
		}, callback, handler);
	}

	/* AOSP AccountManager.notifyAccountAuthenticated() (deprecated): the service records the time of the last
	 * authentication and returns whether the account exists */
	public boolean notifyAccountAuthenticated(Account account) {
		if (account == null)
			throw new IllegalArgumentException("account is null");
		return keyringGet(account.type, account.name, "account") != null;
	}

	/* AOSP renameAccount(): AccountManagerService moves the account (password, user data, auth tokens) to the new name
	 * of the same type; the result is the new Account, the old name stays when the new one is taken. User data and
	 * auth tokens are found by the key indexes setUserData()/setAuthToken() keep, as the keyring cannot be enumerated */
	public AccountManagerFuture<Account> renameAccount(final Account account, final String newName,
	                                                   AccountManagerCallback<Account> callback, Handler handler) {
		if (account == null)
			throw new IllegalArgumentException("account is null");
		if (newName == null || newName.isEmpty())
			throw new IllegalArgumentException("newName is empty or null");
		return runAsync(new Callable<Account>() {
			@Override
			public Account call() {
				if (!ATLSecrets.mayHoldSecrets() || keyringGet(account.type, account.name, "account") == null)
					return account;
				Account renamed = new Account(newName, account.type);
				if (newName.equals(account.name) || keyringGet(renamed.type, renamed.name, "account") != null)
					return account;
				if (!nativeSet(renamed.type, renamed.name, "account", ""))
					return account;
				setPassword(renamed, getPassword(account));
				String keys = keyringGet(account.type, account.name, "userdatakeys");
				if (keys != null && !keys.isEmpty())
					for (String key : keys.split("\n"))
						setUserData(renamed, key, getUserData(account, key));
				String types = keyringGet(account.type, account.name, "authtokentypes");
				if (types != null && !types.isEmpty())
					for (String type : types.split("\n"))
						setAuthToken(renamed, type, peekAuthToken(account, type));
				nativeRemove(account.type, account.name);
				return renamed;
			}
		}, callback, handler);
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

	/* AOSP ensureNotOnMainThread(): waiting for a result on the main thread would deadlock */
	private void ensureNotOnMainThread() {
		final Looper looper = Looper.myLooper();
		if (looper != null && looper == mContext.getMainLooper()) {
			final IllegalStateException exception = new IllegalStateException("calling this from your main thread can lead to deadlock");
			android.util.Log.e("AccountManager", "calling this from your main thread can lead to deadlock and/or ANRs", exception);
			if (mContext.getApplicationInfo().targetSdkVersion >= android.os.Build.VERSION_CODES.FROYO)
				throw exception;
		}
	}

	private static <T> void postToHandler(Handler handler, final AccountManagerCallback<T> callback, final AccountManagerFuture<T> future) {
		(handler != null ? handler : new Handler(Looper.getMainLooper())).post(new Runnable() {
			@Override
			public void run() {
				callback.run(future);
			}
		});
	}

	/* AOSP convertErrorToException() */
	private static Exception convertErrorToException(int code, String message) {
		if (code == ERROR_CODE_NETWORK_ERROR)
			return new IOException(message);
		if (code == ERROR_CODE_UNSUPPORTED_OPERATION)
			return new UnsupportedOperationException(message);
		if (code == ERROR_CODE_INVALID_RESPONSE)
			return new AuthenticatorException(message);
		if (code == ERROR_CODE_BAD_ARGUMENTS)
			return new IllegalArgumentException(message);
		return new AuthenticatorException(message);
	}

	/* AOSP AmsTask: a request to the account manager service whose answer is a Bundle; an intent in the answer is
	 * started with the caller's activity, if it gave one, and the task waits for the real answer */
	private abstract class AmsTask extends FutureTask<Bundle> implements AccountManagerFuture<Bundle> {
		final IAccountManagerResponse mResponse;
		final Handler mHandler;
		final AccountManagerCallback<Bundle> mCallback;
		final Activity mActivity;

		public AmsTask(Activity activity, Handler handler, AccountManagerCallback<Bundle> callback) {
			super(new Callable<Bundle>() {
				@Override
				public Bundle call() throws Exception {
					throw new IllegalStateException("this should never be called");
				}
			});
			mHandler = handler;
			mCallback = callback;
			mActivity = activity;
			mResponse = new Response();
		}

		public final AccountManagerFuture<Bundle> start() {
			try {
				doWork();
			} catch (RemoteException e) {
				setException(e);
			}
			return this;
		}

		@Override
		protected void set(Bundle bundle) {
			if (bundle == null)
				android.util.Log.e("AccountManager", "the bundle must not be null", new Exception());
			super.set(bundle);
		}

		public abstract void doWork() throws RemoteException;

		private Bundle internalGetResult(Long timeout, TimeUnit unit) throws OperationCanceledException, IOException, AuthenticatorException {
			if (!isDone())
				ensureNotOnMainThread();
			try {
				if (timeout == null)
					return get();
				else
					return get(timeout, unit);
			} catch (CancellationException | TimeoutException | InterruptedException e) {
				throw new OperationCanceledException(e);
			} catch (ExecutionException e) {
				final Throwable cause = e.getCause();
				if (cause instanceof IOException)
					throw (IOException)cause;
				else if (cause instanceof UnsupportedOperationException)
					throw new AuthenticatorException(cause);
				else if (cause instanceof AuthenticatorException)
					throw (AuthenticatorException)cause;
				else if (cause instanceof RuntimeException)
					throw (RuntimeException)cause;
				else if (cause instanceof Error)
					throw (Error)cause;
				else
					throw new IllegalStateException(cause);
			} finally {
				cancel(true /* interrupt if running */);
			}
		}

		@Override
		public Bundle getResult() throws OperationCanceledException, IOException, AuthenticatorException {
			return internalGetResult(null, null);
		}

		@Override
		public Bundle getResult(long timeout, TimeUnit unit) throws OperationCanceledException, IOException, AuthenticatorException {
			return internalGetResult(timeout, unit);
		}

		@Override
		protected void done() {
			if (mCallback != null)
				postToHandler(mHandler, mCallback, this);
		}

		/* Handles the responses from the AccountManager */
		private class Response extends IAccountManagerResponse.Stub {
			@Override
			public void onResult(Bundle bundle) {
				if (bundle == null) {
					onError(ERROR_CODE_INVALID_RESPONSE, "null bundle returned");
					return;
				}
				Intent intent = bundle.getParcelable(KEY_INTENT, Intent.class);
				if (intent != null && mActivity != null) {
					// since the user provided an Activity we will silently start intents that we see
					mActivity.startActivity(intent);
					// leave the Future running to wait for the real response to this request
				} else if (bundle.getBoolean("retry")) {
					try {
						doWork();
					} catch (RemoteException e) {
						throw new RuntimeException(e);
					}
				} else {
					set(bundle);
				}
			}

			@Override
			public void onError(int code, String message) {
				if (code == ERROR_CODE_CANCELED || code == ERROR_CODE_USER_RESTRICTED || code == ERROR_CODE_MANAGEMENT_DISABLED_FOR_ACCOUNT_TYPE) {
					// the authenticator indicated that this request was canceled or we were forbidden to fulfill;
					// cancel now
					cancel(true /* mayInterruptIfRunning */);
					return;
				}
				setException(convertErrorToException(code, message));
			}
		}
	}

	/* AOSP BaseFutureTask/Future2Task: a request whose answer Bundle becomes a result of type T */
	private abstract class Future2Task<T> extends FutureTask<T> implements AccountManagerFuture<T> {
		final IAccountManagerResponse mResponse;
		final Handler mHandler;
		final AccountManagerCallback<T> mCallback;

		public Future2Task(Handler handler, AccountManagerCallback<T> callback) {
			super(new Callable<T>() {
				@Override
				public T call() throws Exception {
					throw new IllegalStateException("this should never be called");
				}
			});
			mHandler = handler;
			mCallback = callback;
			mResponse = new Response();
		}

		public abstract void doWork() throws RemoteException;

		public abstract T bundleToResult(Bundle bundle) throws AuthenticatorException;

		@Override
		protected void done() {
			if (mCallback != null)
				postToHandler(mHandler, mCallback, this);
		}

		public Future2Task<T> start() {
			try {
				doWork();
			} catch (RemoteException e) {
				setException(e);
			}
			return this;
		}

		private T internalGetResult(Long timeout, TimeUnit unit) throws OperationCanceledException, IOException, AuthenticatorException {
			if (!isDone())
				ensureNotOnMainThread();
			try {
				if (timeout == null)
					return get();
				else
					return get(timeout, unit);
			} catch (InterruptedException | TimeoutException | CancellationException e) {
				// fall through and cancel
			} catch (ExecutionException e) {
				final Throwable cause = e.getCause();
				if (cause instanceof IOException)
					throw (IOException)cause;
				else if (cause instanceof UnsupportedOperationException)
					throw new AuthenticatorException(cause);
				else if (cause instanceof AuthenticatorException)
					throw (AuthenticatorException)cause;
				else if (cause instanceof RuntimeException)
					throw (RuntimeException)cause;
				else if (cause instanceof Error)
					throw (Error)cause;
				else
					throw new IllegalStateException(cause);
			} finally {
				cancel(true /* interrupt if running */);
			}
			throw new OperationCanceledException();
		}

		@Override
		public T getResult() throws OperationCanceledException, IOException, AuthenticatorException {
			return internalGetResult(null, null);
		}

		@Override
		public T getResult(long timeout, TimeUnit unit) throws OperationCanceledException, IOException, AuthenticatorException {
			return internalGetResult(timeout, unit);
		}

		private class Response extends IAccountManagerResponse.Stub {
			@Override
			public void onResult(Bundle bundle) {
				try {
					T result = bundleToResult(bundle);
					if (result == null)
						return;
					set(result);
					return;
				} catch (ClassCastException e) {
					// we will set the exception below
				} catch (AuthenticatorException e) {
					// we will set the exception below
				}
				onError(ERROR_CODE_INVALID_RESPONSE, "no result in response");
			}

			@Override
			public void onError(int code, String message) {
				if (code == ERROR_CODE_CANCELED || code == ERROR_CODE_USER_RESTRICTED || code == ERROR_CODE_MANAGEMENT_DISABLED_FOR_ACCOUNT_TYPE) {
					// the authenticator indicated that this request was canceled or we were forbidden to fulfill;
					// cancel now
					cancel(true /* mayInterruptIfRunning */);
					return;
				}
				setException(convertErrorToException(code, message));
			}
		}
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

	/* AOSP getAuthenticatorTypes(): the authenticators the AccountManagerService knows (AccountAuthenticatorCache) */
	public AuthenticatorDescription[] getAuthenticatorTypes() {
		return service().getAuthenticatorTypes();
	}

	/* AOSP removeAccount(Account, Activity, ...): AccountManagerService asks the authenticator of the account type
	 * (getAccountRemovalAllowed) and removes the account when it allows it */
	public AccountManagerFuture<Bundle> removeAccount(final Account account, final Activity activity,
	                                                  AccountManagerCallback<Bundle> callback, Handler handler) {
		if (account == null)
			throw new IllegalArgumentException("account is null");
		return new AmsTask(activity, handler, callback) {
			@Override
			public void doWork() throws RemoteException {
				service().removeAccount(mResponse, account, activity != null);
			}
		}.start();
	}

	/* AOSP updateCredentials(): asks the authenticator of the account type */
	public AccountManagerFuture<Bundle> updateCredentials(final Account account, final String authTokenType, final Bundle options,
	                                                      final Activity activity, AccountManagerCallback<Bundle> callback, Handler handler) {
		if (account == null)
			throw new IllegalArgumentException("account is null");
		return new AmsTask(activity, handler, callback) {
			@Override
			public void doWork() throws RemoteException {
				service().updateCredentials(mResponse, account, authTokenType, activity != null, options);
			}
		}.start();
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
		/* index of the user data keys, for renameAccount() */
		String keys = keyringGet(account.type, account.name, "userdatakeys");
		if (value != null && (keys == null || !("\n" + keys + "\n").contains("\n" + key + "\n")))
			nativeSet(account.type, account.name, "userdatakeys", keys == null || keys.isEmpty() ? key : keys + "\n" + key);
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
		service().invalidateCachedToken(accountType, authToken);
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

	/* AOSP hasFeatures(): asks the authenticator of the account type */
	public AccountManagerFuture<Boolean> hasFeatures(final Account account, final String[] features,
	                                                 AccountManagerCallback<Boolean> callback, Handler handler) {
		if (account == null)
			throw new IllegalArgumentException("account is null");
		if (features == null)
			throw new IllegalArgumentException("features is null");
		return new Future2Task<Boolean>(handler, callback) {
			@Override
			public void doWork() throws RemoteException {
				service().hasFeatures(mResponse, account, features);
			}

			@Override
			public Boolean bundleToResult(Bundle bundle) throws AuthenticatorException {
				if (!bundle.containsKey(KEY_BOOLEAN_RESULT))
					throw new AuthenticatorException("no result in response");
				return bundle.getBoolean(KEY_BOOLEAN_RESULT);
			}
		}.start();
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
