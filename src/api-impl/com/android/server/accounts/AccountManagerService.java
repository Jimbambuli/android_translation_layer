package com.android.server.accounts;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorDescription;
import android.accounts.IAccountAuthenticator;
import android.accounts.IAccountAuthenticatorResponse;
import android.accounts.IAccountManagerResponse;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Slog;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * The part of AOSP's AccountManagerService (services/core/java/com/android/server/accounts, android-16.0.0_r4)
 * that talks to authenticators: a Session binds the authenticator service of the account type (as
 * bindToAuthenticator()), calls it and hands its answer to the caller's IAccountManagerResponse. ATL runs it in
 * the app's process; the services of other packages (microG's com.google authenticator) run there too, as all of
 * Play Services does. Accounts, passwords, user data and auth tokens stay in AccountManager's keyring.
 * Left out, as ATL has no other users or apps running beside the app: users, account visibility per package,
 * notifications, the session timeout and the permission grant dialog (GrantCredentialsPermissionActivity) - the
 * calling app counts as granted.
 * @hide
 */
public final class AccountManagerService {
	private static final String TAG = "AccountManagerService";
	private static AccountManagerService sInstance;

	private final Context mContext;
	private final AccountAuthenticatorCache mAuthenticatorCache;
	/* TokenCache for authenticators with customTokens: account, type and caller package -> token, expiry */
	private final HashMap<List<String>, Object[]> mTokenCache = new HashMap<>();

	private AccountManagerService(Context context) {
		mContext = context;
		mAuthenticatorCache = new AccountAuthenticatorCache(context);
	}

	public static synchronized AccountManagerService get(Context context) {
		if (sInstance == null)
			sInstance = new AccountManagerService(context.getApplicationContext() != null ? context.getApplicationContext() : context);
		return sInstance;
	}

	private AccountManager accounts() {
		return AccountManager.get(mContext);
	}

	public AuthenticatorDescription[] getAuthenticatorTypes() {
		List<AccountAuthenticatorCache.ServiceInfo> services = mAuthenticatorCache.getAllServices();
		AuthenticatorDescription[] types = new AuthenticatorDescription[services.size()];
		for (int i = 0; i < types.length; i++)
			types[i] = services.get(i).type;
		return types;
	}

	private static void onResult(IAccountManagerResponse response, Bundle result) {
		try {
			response.onResult(result);
		} catch (RemoteException e) {
			Slog.w(TAG, "failure while notifying response", e);
		}
	}

	private static void onError(IAccountManagerResponse response, int errorCode, String errorMessage) {
		try {
			response.onError(errorCode, errorMessage);
		} catch (RemoteException e) {
			Slog.w(TAG, "failure while notifying response", e);
		}
	}

	public void hasFeatures(IAccountManagerResponse response, final Account account, final String[] features) {
		if (account == null || features == null) {
			onError(response, AccountManager.ERROR_CODE_BAD_ARGUMENTS, account == null ? "account is null" : "features is null");
			return;
		}
		new Session(response, account.type, false, true, account.name) {
			@Override
			public void run() throws RemoteException {
				mAuthenticator.hasFeatures(this, account, features);
			}

			@Override
			public void onResult(Bundle result) {
				IAccountManagerResponse response = getResponseAndClose();
				if (response == null)
					return;
				if (result == null) {
					AccountManagerService.onError(response, AccountManager.ERROR_CODE_INVALID_RESPONSE, "null bundle");
					return;
				}
				Bundle newResult = new Bundle();
				newResult.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, result.getBoolean(AccountManager.KEY_BOOLEAN_RESULT, false));
				AccountManagerService.onResult(response, newResult);
			}
		}.bind();
	}

	/* GetAccountsByTypeAndFeatureSession: asks the authenticator account by account */
	public void getAccountsByFeatures(IAccountManagerResponse response, String type, final String[] features) {
		final Account[] candidates = accounts().getAccountsByType(type);
		if (features == null || features.length == 0 || candidates.length == 0) {
			Bundle result = new Bundle();
			result.putParcelableArray(AccountManager.KEY_ACCOUNTS, features == null || features.length == 0 ? candidates : new Account[0]);
			onResult(response, result);
			return;
		}
		new Session(response, type, false, true, null) {
			private int mCurrentAccount = 0;
			private final ArrayList<Account> mAccountsWithFeatures = new ArrayList<>();

			@Override
			public void run() throws RemoteException {
				checkAccount();
			}

			private void checkAccount() {
				if (mCurrentAccount >= candidates.length) {
					sendResult();
					return;
				}
				IAccountAuthenticator accountAuthenticator = mAuthenticator;
				if (accountAuthenticator == null)
					return;
				try {
					accountAuthenticator.hasFeatures(this, candidates[mCurrentAccount], features);
				} catch (RemoteException e) {
					onError(AccountManager.ERROR_CODE_REMOTE_EXCEPTION, "remote exception");
				}
			}

			@Override
			public void onResult(Bundle result) {
				if (result == null) {
					onError(AccountManager.ERROR_CODE_INVALID_RESPONSE, "null bundle");
					return;
				}
				if (result.getBoolean(AccountManager.KEY_BOOLEAN_RESULT, false))
					mAccountsWithFeatures.add(candidates[mCurrentAccount]);
				mCurrentAccount++;
				checkAccount();
			}

			private void sendResult() {
				IAccountManagerResponse response = getResponseAndClose();
				if (response == null)
					return;
				Bundle result = new Bundle();
				result.putParcelableArray(AccountManager.KEY_ACCOUNTS, mAccountsWithFeatures.toArray(new Account[0]));
				AccountManagerService.onResult(response, result);
			}
		}.bind();
	}

	/* removeAccountAsUser(): the authenticator decides with getAccountRemovalAllowed() */
	public void removeAccount(IAccountManagerResponse response, final Account account, boolean expectActivityLaunch) {
		if (account == null) {
			onError(response, AccountManager.ERROR_CODE_BAD_ARGUMENTS, "account is null");
			return;
		}
		new Session(response, account.type, expectActivityLaunch, true, account.name) {
			@Override
			public void run() throws RemoteException {
				mAuthenticator.getAccountRemovalAllowed(this, account);
			}

			@Override
			public void onResult(Bundle result) {
				if (result != null && result.containsKey(AccountManager.KEY_BOOLEAN_RESULT) && !result.containsKey(AccountManager.KEY_INTENT)) {
					final boolean removalAllowed = result.getBoolean(AccountManager.KEY_BOOLEAN_RESULT);
					if (removalAllowed)
						accounts().removeAccountExplicitly(account);
					IAccountManagerResponse response = getResponseAndClose();
					if (response != null) {
						Bundle result2 = new Bundle();
						result2.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, removalAllowed);
						AccountManagerService.onResult(response, result2);
					}
				}
				super.onResult(result);
			}
		}.bind();
	}

	public void getAuthToken(IAccountManagerResponse response, final Account account, final String authTokenType,
	                         final boolean notifyOnAuthFailure, boolean expectActivityLaunch, final Bundle loginOptions) {
		if (account == null) {
			onError(response, AccountManager.ERROR_CODE_BAD_ARGUMENTS, "account is null");
			return;
		}
		if (authTokenType == null) {
			onError(response, AccountManager.ERROR_CODE_BAD_ARGUMENTS, "authTokenType is null");
			return;
		}
		AccountAuthenticatorCache.ServiceInfo authenticatorInfo = mAuthenticatorCache.getServiceInfo(account.type);
		final boolean customTokens = authenticatorInfo != null && authenticatorInfo.type.customTokens;
		final String callerPkg = loginOptions.getString(AccountManager.KEY_ANDROID_PACKAGE_NAME);
		loginOptions.putInt(AccountManager.KEY_CALLER_UID, Binder.getCallingUid());
		loginOptions.putInt(AccountManager.KEY_CALLER_PID, Binder.getCallingPid());
		if (notifyOnAuthFailure)
			loginOptions.putBoolean(AccountManager.KEY_NOTIFY_ON_FAILURE, true);
		if (!customTokens) {
			String authToken = accounts().peekAuthToken(account, authTokenType);
			if (authToken != null) {
				Bundle result = new Bundle();
				result.putString(AccountManager.KEY_AUTHTOKEN, authToken);
				result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
				result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
				onResult(response, result);
				return;
			}
		} else {
			Object[] cached = readCachedToken(account, authTokenType, callerPkg);
			if (cached != null) {
				Bundle result = new Bundle();
				result.putString(AccountManager.KEY_AUTHTOKEN, (String)cached[0]);
				result.putLong(AbstractAccountAuthenticator.KEY_CUSTOM_TOKEN_EXPIRY, (Long)cached[1]);
				result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
				result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
				onResult(response, result);
				return;
			}
		}
		new Session(response, account.type, expectActivityLaunch, false, account.name) {
			@Override
			public void run() throws RemoteException {
				mAuthenticator.getAuthToken(this, account, authTokenType, loginOptions);
			}

			@Override
			public void onResult(Bundle result) {
				if (result != null) {
					String authToken = result.getString(AccountManager.KEY_AUTHTOKEN);
					if (authToken != null) {
						String name = result.getString(AccountManager.KEY_ACCOUNT_NAME);
						String type = result.getString(AccountManager.KEY_ACCOUNT_TYPE);
						if (TextUtils.isEmpty(type) || TextUtils.isEmpty(name)) {
							onError(AccountManager.ERROR_CODE_INVALID_RESPONSE, "the type and name should not be empty");
							return;
						}
						if (!type.equals(mAccountType)) {
							onError(AccountManager.ERROR_CODE_INVALID_RESPONSE, "incorrect account type");
							return;
						}
						Account resultAccount = new Account(name, type);
						if (!customTokens) {
							accounts().setAuthToken(resultAccount, authTokenType, authToken);
						}
						long expiryMillis = result.getLong(AbstractAccountAuthenticator.KEY_CUSTOM_TOKEN_EXPIRY, 0L);
						if (customTokens && expiryMillis > System.currentTimeMillis())
							saveCachedToken(resultAccount, authTokenType, callerPkg, authToken, expiryMillis);
					}
				}
				super.onResult(result);
			}
		}.bind();
	}

	private synchronized Object[] readCachedToken(Account account, String tokenType, String callerPkg) {
		List<String> key = java.util.Arrays.asList(account.type, account.name, tokenType, callerPkg);
		Object[] value = mTokenCache.get(key);
		if (value != null && (Long)value[1] <= System.currentTimeMillis()) {
			mTokenCache.remove(key);
			return null;
		}
		return value;
	}

	private synchronized void saveCachedToken(Account account, String tokenType, String callerPkg, String token, long expiryMillis) {
		mTokenCache.put(java.util.Arrays.asList(account.type, account.name, tokenType, callerPkg), new Object[] {token, expiryMillis});
	}

	/* invalidateAuthToken() removes the token from the TokenCache too */
	public synchronized void invalidateCachedToken(String accountType, String authToken) {
		if (authToken == null)
			return;
		for (java.util.Iterator<Object[]> it = mTokenCache.values().iterator(); it.hasNext();)
			if (authToken.equals(it.next()[0]))
				it.remove();
	}

	public void addAccount(IAccountManagerResponse response, final String accountType, final String authTokenType,
	                       final String[] requiredFeatures, boolean expectActivityLaunch, Bundle optionsIn) {
		if (accountType == null) {
			onError(response, AccountManager.ERROR_CODE_BAD_ARGUMENTS, "accountType is null");
			return;
		}
		final Bundle options = (optionsIn == null) ? new Bundle() : optionsIn;
		options.putInt(AccountManager.KEY_CALLER_UID, Binder.getCallingUid());
		options.putInt(AccountManager.KEY_CALLER_PID, Binder.getCallingPid());
		new Session(response, accountType, expectActivityLaunch, true, null) {
			@Override
			public void run() throws RemoteException {
				mAuthenticator.addAccount(this, mAccountType, authTokenType, requiredFeatures, options);
			}
		}.bind();
	}

	public void confirmCredentials(IAccountManagerResponse response, final Account account, final Bundle options,
	                               boolean expectActivityLaunch) {
		if (account == null) {
			onError(response, AccountManager.ERROR_CODE_BAD_ARGUMENTS, "account is null");
			return;
		}
		new Session(response, account.type, expectActivityLaunch, true, account.name) {
			@Override
			public void run() throws RemoteException {
				mAuthenticator.confirmCredentials(this, account, options);
			}
		}.bind();
	}

	public void updateCredentials(IAccountManagerResponse response, final Account account, final String authTokenType,
	                              boolean expectActivityLaunch, final Bundle loginOptions) {
		if (account == null) {
			onError(response, AccountManager.ERROR_CODE_BAD_ARGUMENTS, "account is null");
			return;
		}
		new Session(response, account.type, expectActivityLaunch, true, account.name) {
			@Override
			public void run() throws RemoteException {
				mAuthenticator.updateCredentials(this, account, authTokenType, loginOptions);
			}
		}.bind();
	}

	public void editProperties(IAccountManagerResponse response, final String accountType, boolean expectActivityLaunch) {
		if (accountType == null) {
			onError(response, AccountManager.ERROR_CODE_BAD_ARGUMENTS, "accountType is null");
			return;
		}
		new Session(response, accountType, expectActivityLaunch, true, null) {
			@Override
			public void run() throws RemoteException {
				mAuthenticator.editProperties(this, mAccountType);
			}
		}.bind();
	}

	/* Session: binds the authenticator, runs the request and answers the caller */
	private abstract class Session extends IAccountAuthenticatorResponse.Stub implements ServiceConnection {
		private final Object mSessionLock = new Object();
		IAccountManagerResponse mResponse;
		final String mAccountType;
		final boolean mExpectActivityLaunch;
		final String mAccountName;
		private final boolean mStripAuthTokenFromResult;
		IAccountAuthenticator mAuthenticator = null;
		private String mAuthenticatorPackage;
		private boolean mBound;

		Session(IAccountManagerResponse response, String accountType, boolean expectActivityLaunch,
		        boolean stripAuthTokenFromResult, String accountName) {
			if (accountType == null)
				throw new IllegalArgumentException("accountType is null");
			mStripAuthTokenFromResult = stripAuthTokenFromResult;
			mResponse = response;
			mAccountType = accountType;
			mExpectActivityLaunch = expectActivityLaunch;
			mAccountName = accountName;
		}

		IAccountManagerResponse getResponseAndClose() {
			IAccountManagerResponse response;
			synchronized (mSessionLock) {
				response = mResponse;
				mResponse = null;
			}
			if (response == null)
				return null;
			close();
			return response;
		}

		private void close() {
			synchronized (mSessionLock) {
				if (mBound) {
					mBound = false;
					mContext.unbindService(this);
				}
				mAuthenticator = null;
			}
		}

		void bind() {
			if (!bindToAuthenticator(mAccountType)) {
				Slog.d(TAG, "bind attempt failed for " + mAccountType);
				onError(AccountManager.ERROR_CODE_REMOTE_EXCEPTION, "bind failure");
			}
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			synchronized (mSessionLock) {
				mAuthenticator = IAccountAuthenticator.Stub.asInterface(service);
				try {
					run();
				} catch (RemoteException e) {
					onError(AccountManager.ERROR_CODE_REMOTE_EXCEPTION, "remote exception");
				}
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			mAuthenticator = null;
			IAccountManagerResponse response = getResponseAndClose();
			if (response != null)
				AccountManagerService.onError(response, AccountManager.ERROR_CODE_REMOTE_EXCEPTION, "disconnected");
		}

		@Override
		public void onNullBinding(ComponentName name) {
			IAccountManagerResponse response = getResponseAndClose();
			if (response != null)
				AccountManagerService.onError(response, AccountManager.ERROR_CODE_REMOTE_EXCEPTION, "disconnected");
		}

		public abstract void run() throws RemoteException;

		/* checkKeyIntent(): an intent in the result must start an activity of the authenticator's package (AOSP: of
		 * a package with the authenticator's signature) or an exported activity of the system */
		private boolean checkKeyIntent(Bundle bundle) {
			Intent intent = bundle.getParcelable(AccountManager.KEY_INTENT, Intent.class);
			if (intent == null)
				return true;
			/* AOSP resolveActivityAsUser(); ATL's resolveActivity() looks only at the app's own activities, so an
			 * explicit intent is resolved by its component */
			ActivityInfo targetActivityInfo;
			if (intent.getComponent() != null) {
				try {
					targetActivityInfo = mContext.getPackageManager().getActivityInfo(intent.getComponent(), 0);
				} catch (android.content.pm.PackageManager.NameNotFoundException e) {
					return false;
				}
			} else {
				ResolveInfo resolveInfo = mContext.getPackageManager().resolveActivity(intent, 0);
				if (resolveInfo == null || resolveInfo.activityInfo == null)
					return false;
				targetActivityInfo = resolveInfo.activityInfo;
			}
			if ("android".equals(targetActivityInfo.packageName) && targetActivityInfo.exported)
				return true;
			if (!targetActivityInfo.packageName.equals(mAuthenticatorPackage)) {
				Slog.e(TAG, "KEY_INTENT resolved to an Activity (" + targetActivityInfo.name + ") in a package ("
				                + targetActivityInfo.packageName + ") that does not belong to the authenticator");
				return false;
			}
			return true;
		}

		@Override
		public void onResult(Bundle result) {
			if (result != null && !checkKeyIntent(result)) {
				onError(AccountManager.ERROR_CODE_INVALID_RESPONSE, "invalid intent in bundle returned");
				return;
			}
			IAccountManagerResponse response;
			if (mExpectActivityLaunch && result != null && result.containsKey(AccountManager.KEY_INTENT)) {
				response = mResponse;
			} else {
				response = getResponseAndClose();
			}
			if (response == null)
				return;
			if (result == null) {
				AccountManagerService.onError(response, AccountManager.ERROR_CODE_INVALID_RESPONSE, "null bundle returned");
				return;
			}
			if (mStripAuthTokenFromResult)
				result.remove(AccountManager.KEY_AUTHTOKEN);
			if (result.getInt(AccountManager.KEY_ERROR_CODE, -1) > 0) {
				AccountManagerService.onError(response, result.getInt(AccountManager.KEY_ERROR_CODE),
				                              result.getString(AccountManager.KEY_ERROR_MESSAGE));
			} else {
				AccountManagerService.onResult(response, result);
			}
		}

		@Override
		public void onRequestContinued() {}

		@Override
		public void onError(int errorCode, String errorMessage) {
			IAccountManagerResponse response = getResponseAndClose();
			if (response != null)
				AccountManagerService.onError(response, errorCode, errorMessage);
		}

		private boolean bindToAuthenticator(String authenticatorType) {
			AccountAuthenticatorCache.ServiceInfo authenticatorInfo = mAuthenticatorCache.getServiceInfo(authenticatorType);
			if (authenticatorInfo == null) {
				Slog.w(TAG, "there is no authenticator for " + authenticatorType + ", bailing out");
				return false;
			}
			Intent intent = new Intent();
			intent.setAction(AccountManager.ACTION_AUTHENTICATOR_INTENT);
			intent.setComponent(authenticatorInfo.componentName);
			mAuthenticatorPackage = authenticatorInfo.componentName.getPackageName();
			synchronized (mSessionLock) {
				mBound = true;
			}
			if (!mContext.bindService(intent, this, Context.BIND_AUTO_CREATE)) {
				Slog.w(TAG, "bindService to " + authenticatorInfo.componentName + " failed");
				close();
				return false;
			}
			return true;
		}
	}
}
