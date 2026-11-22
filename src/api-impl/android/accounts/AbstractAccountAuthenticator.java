/* AOSP android-16.0.0_r4 frameworks/base core/java/android/accounts/AbstractAccountAuthenticator.java */
package android.accounts;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import java.util.Arrays;

public abstract class AbstractAccountAuthenticator {
    private static final String TAG = "AccountAuthenticator";

    public static final String KEY_CUSTOM_TOKEN_EXPIRY = "android.accounts.expiry";

    private static final String KEY_AUTH_TOKEN_TYPE =
        "android.accounts.AbstractAccountAuthenticato.KEY_AUTH_TOKEN_TYPE";
    private static final String KEY_REQUIRED_FEATURES =
        "android.accounts.AbstractAccountAuthenticator.KEY_REQUIRED_FEATURES";
    private static final String KEY_OPTIONS =
        "android.accounts.AbstractAccountAuthenticator.KEY_OPTIONS";
    private static final String KEY_ACCOUNT =
        "android.accounts.AbstractAccountAuthenticator.KEY_ACCOUNT";

    public AbstractAccountAuthenticator(Context context) {
    }

    /* AOSP enforces ACCOUNT_MANAGER on every call (@EnforcePermission); ATL's only caller is the account manager
     * service in the app's process (com.android.server.accounts), which holds it as system_server does */
    private class Transport extends IAccountAuthenticator.Stub {
        @Override
        public void addAccount(IAccountAuthenticatorResponse response, String accountType,
                String authTokenType, String[] features, Bundle options)
                throws RemoteException {
            try {
                final Bundle result = AbstractAccountAuthenticator.this.addAccount(
                    new AccountAuthenticatorResponse(response),
                        accountType, authTokenType, features, options);
                if (result != null) {
                    response.onResult(result);
                } else {
                    response.onError(AccountManager.ERROR_CODE_INVALID_RESPONSE,
                            "null bundle returned");
                }
            } catch (Exception e) {
                handleException(response, "addAccount", accountType, e);
            }
        }

        @Override
        public void confirmCredentials(IAccountAuthenticatorResponse response,
                Account account, Bundle options) throws RemoteException {
            try {
                final Bundle result = AbstractAccountAuthenticator.this.confirmCredentials(
                    new AccountAuthenticatorResponse(response), account, options);
                if (result != null) {
                    response.onResult(result);
                }
            } catch (Exception e) {
                handleException(response, "confirmCredentials", account.toString(), e);
            }
        }

        @Override
        public void getAuthTokenLabel(IAccountAuthenticatorResponse response,
                String authTokenType)
                throws RemoteException {
            try {
                Bundle result = new Bundle();
                result.putString(AccountManager.KEY_AUTH_TOKEN_LABEL,
                        AbstractAccountAuthenticator.this.getAuthTokenLabel(authTokenType));
                response.onResult(result);
            } catch (Exception e) {
                handleException(response, "getAuthTokenLabel", authTokenType, e);
            }
        }

        @Override
        public void getAuthToken(IAccountAuthenticatorResponse response,
                Account account, String authTokenType, Bundle loginOptions)
                throws RemoteException {
            try {
                final Bundle result = AbstractAccountAuthenticator.this.getAuthToken(
                        new AccountAuthenticatorResponse(response), account,
                        authTokenType, loginOptions);
                if (result != null) {
                    response.onResult(result);
                }
            } catch (Exception e) {
                handleException(response, "getAuthToken",
                        account.toString() + "," + authTokenType, e);
            }
        }

        @Override
        public void updateCredentials(IAccountAuthenticatorResponse response, Account account,
                String authTokenType, Bundle loginOptions) throws RemoteException {
            try {
                final Bundle result = AbstractAccountAuthenticator.this.updateCredentials(
                    new AccountAuthenticatorResponse(response), account,
                        authTokenType, loginOptions);
                if (result != null) {
                    response.onResult(result);
                }
            } catch (Exception e) {
                handleException(response, "updateCredentials",
                        account.toString() + "," + authTokenType, e);
            }
        }

        @Override
        public void editProperties(IAccountAuthenticatorResponse response,
                String accountType) throws RemoteException {
            try {
                final Bundle result = AbstractAccountAuthenticator.this.editProperties(
                    new AccountAuthenticatorResponse(response), accountType);
                if (result != null) {
                    response.onResult(result);
                }
            } catch (Exception e) {
                handleException(response, "editProperties", accountType, e);
            }
        }

        @Override
        public void hasFeatures(IAccountAuthenticatorResponse response,
                Account account, String[] features) throws RemoteException {
            try {
                final Bundle result = AbstractAccountAuthenticator.this.hasFeatures(
                    new AccountAuthenticatorResponse(response), account, features);
                if (result != null) {
                    response.onResult(result);
                }
            } catch (Exception e) {
                handleException(response, "hasFeatures", account.toString(), e);
            }
        }

        @Override
        public void getAccountRemovalAllowed(IAccountAuthenticatorResponse response,
                Account account) throws RemoteException {
            try {
                final Bundle result = AbstractAccountAuthenticator.this.getAccountRemovalAllowed(
                    new AccountAuthenticatorResponse(response), account);
                if (result != null) {
                    response.onResult(result);
                }
            } catch (Exception e) {
                handleException(response, "getAccountRemovalAllowed", account.toString(), e);
            }
        }

        @Override
        public void getAccountCredentialsForCloning(IAccountAuthenticatorResponse response,
                Account account) throws RemoteException {
            try {
                final Bundle result =
                        AbstractAccountAuthenticator.this.getAccountCredentialsForCloning(
                                new AccountAuthenticatorResponse(response), account);
                if (result != null) {
                    response.onResult(result);
                }
            } catch (Exception e) {
                handleException(response, "getAccountCredentialsForCloning", account.toString(), e);
            }
        }

        @Override
        public void addAccountFromCredentials(IAccountAuthenticatorResponse response,
                Account account,
                Bundle accountCredentials) throws RemoteException {
            try {
                final Bundle result =
                        AbstractAccountAuthenticator.this.addAccountFromCredentials(
                                new AccountAuthenticatorResponse(response), account,
                                accountCredentials);
                if (result != null) {
                    response.onResult(result);
                }
            } catch (Exception e) {
                handleException(response, "addAccountFromCredentials", account.toString(), e);
            }
        }

        @Override
        public void startAddAccountSession(IAccountAuthenticatorResponse response,
                String accountType, String authTokenType, String[] features, Bundle options)
                throws RemoteException {
            try {
                final Bundle result = AbstractAccountAuthenticator.this.startAddAccountSession(
                        new AccountAuthenticatorResponse(response), accountType, authTokenType,
                        features, options);
                if (result != null) {
                    response.onResult(result);
                }
            } catch (Exception e) {
                handleException(response, "startAddAccountSession", accountType, e);
            }
        }

        @Override
        public void startUpdateCredentialsSession(
                IAccountAuthenticatorResponse response,
                Account account,
                String authTokenType,
                Bundle loginOptions) throws RemoteException {
            try {
                final Bundle result = AbstractAccountAuthenticator.this
                        .startUpdateCredentialsSession(
                                new AccountAuthenticatorResponse(response),
                                account,
                                authTokenType,
                                loginOptions);
                if (result != null) {
                    response.onResult(result);
                }
            } catch (Exception e) {
                handleException(response, "startUpdateCredentialsSession",
                        account.toString() + "," + authTokenType, e);

            }
        }

        @Override
        public void finishSession(
                IAccountAuthenticatorResponse response,
                String accountType,
                Bundle sessionBundle) throws RemoteException {
            try {
                final Bundle result = AbstractAccountAuthenticator.this.finishSession(
                        new AccountAuthenticatorResponse(response), accountType, sessionBundle);
                if (result != null) {
                    result.keySet(); // force it to be unparcelled
                }
                if (result != null) {
                    response.onResult(result);
                }
            } catch (Exception e) {
                handleException(response, "finishSession", accountType, e);

            }
        }

        @Override
        public void isCredentialsUpdateSuggested(
                IAccountAuthenticatorResponse response,
                Account account,
                String statusToken) throws RemoteException {
            try {
                final Bundle result = AbstractAccountAuthenticator.this
                        .isCredentialsUpdateSuggested(
                                new AccountAuthenticatorResponse(response), account, statusToken);
                if (result != null) {
                    response.onResult(result);
                }
            } catch (Exception e) {
                handleException(response, "isCredentialsUpdateSuggested", account.toString(), e);
            }
        }
    }

    private void handleException(IAccountAuthenticatorResponse response, String method,
            String data, Exception e) throws RemoteException {
        if (e instanceof NetworkErrorException) {
            response.onError(AccountManager.ERROR_CODE_NETWORK_ERROR, e.getMessage());
        } else if (e instanceof UnsupportedOperationException) {
            response.onError(AccountManager.ERROR_CODE_UNSUPPORTED_OPERATION,
                    method + " not supported");
        } else if (e instanceof IllegalArgumentException) {
            response.onError(AccountManager.ERROR_CODE_BAD_ARGUMENTS,
                    method + " not supported");
        } else {
            Log.w(TAG, method + "(" + data + ")", e);
            response.onError(AccountManager.ERROR_CODE_REMOTE_EXCEPTION,
                    method + " failed");
        }
    }

    private Transport mTransport = new Transport();

        public final IBinder getIBinder() {
        return mTransport.asBinder();
    }

        public abstract Bundle editProperties(AccountAuthenticatorResponse response,
            String accountType);

        public abstract Bundle addAccount(AccountAuthenticatorResponse response, String accountType,
            String authTokenType, String[] requiredFeatures, Bundle options)
            throws NetworkErrorException;

        public abstract Bundle confirmCredentials(AccountAuthenticatorResponse response,
            Account account, Bundle options)
            throws NetworkErrorException;

        public abstract Bundle getAuthToken(AccountAuthenticatorResponse response,
            Account account, String authTokenType, Bundle options)
            throws NetworkErrorException;

        public abstract String getAuthTokenLabel(String authTokenType);

        public abstract Bundle updateCredentials(AccountAuthenticatorResponse response,
            Account account, String authTokenType, Bundle options) throws NetworkErrorException;

        public abstract Bundle hasFeatures(AccountAuthenticatorResponse response,
            Account account, String[] features) throws NetworkErrorException;

        public Bundle getAccountRemovalAllowed(AccountAuthenticatorResponse response,
            Account account) throws NetworkErrorException {
        final Bundle result = new Bundle();
        result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, true);
        return result;
    }

        public Bundle getAccountCredentialsForCloning(final AccountAuthenticatorResponse response,
            final Account account) throws NetworkErrorException {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Bundle result = new Bundle();
                result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false);
                response.onResult(result);
            }
        }).start();
        return null;
    }

        public Bundle addAccountFromCredentials(final AccountAuthenticatorResponse response,
            Account account,
            Bundle accountCredentials) throws NetworkErrorException {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Bundle result = new Bundle();
                result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false);
                response.onResult(result);
            }
        }).start();
        return null;
    }

        public Bundle startAddAccountSession(
            final AccountAuthenticatorResponse response,
            final String accountType,
            final String authTokenType,
            final String[] requiredFeatures,
            final Bundle options)
            throws NetworkErrorException {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Bundle sessionBundle = new Bundle();
                sessionBundle.putString(KEY_AUTH_TOKEN_TYPE, authTokenType);
                sessionBundle.putStringArray(KEY_REQUIRED_FEATURES, requiredFeatures);
                sessionBundle.putBundle(KEY_OPTIONS, options);
                Bundle result = new Bundle();
                result.putBundle(AccountManager.KEY_ACCOUNT_SESSION_BUNDLE, sessionBundle);
                response.onResult(result);
            }

        }).start();
        return null;
    }

        public Bundle startUpdateCredentialsSession(
            final AccountAuthenticatorResponse response,
            final Account account,
            final String authTokenType,
            final Bundle options) throws NetworkErrorException {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Bundle sessionBundle = new Bundle();
                sessionBundle.putString(KEY_AUTH_TOKEN_TYPE, authTokenType);
                sessionBundle.putParcelable(KEY_ACCOUNT, account);
                sessionBundle.putBundle(KEY_OPTIONS, options);
                Bundle result = new Bundle();
                result.putBundle(AccountManager.KEY_ACCOUNT_SESSION_BUNDLE, sessionBundle);
                response.onResult(result);
            }

        }).start();
        return null;
    }

        public Bundle finishSession(
            final AccountAuthenticatorResponse response,
            final String accountType,
            final Bundle sessionBundle) throws NetworkErrorException {
        if (TextUtils.isEmpty(accountType)) {
            Log.e(TAG, "Account type cannot be empty.");
            Bundle result = new Bundle();
            result.putInt(AccountManager.KEY_ERROR_CODE, AccountManager.ERROR_CODE_BAD_ARGUMENTS);
            result.putString(AccountManager.KEY_ERROR_MESSAGE,
                    "accountType cannot be empty.");
            return result;
        }

        if (sessionBundle == null) {
            Log.e(TAG, "Session bundle cannot be null.");
            Bundle result = new Bundle();
            result.putInt(AccountManager.KEY_ERROR_CODE, AccountManager.ERROR_CODE_BAD_ARGUMENTS);
            result.putString(AccountManager.KEY_ERROR_MESSAGE,
                    "sessionBundle cannot be null.");
            return result;
        }

        if (!sessionBundle.containsKey(KEY_AUTH_TOKEN_TYPE)) {
            // We cannot handle Session bundle not created by default startAddAccountSession(...)
            // nor startUpdateCredentialsSession(...) implementation. Return error.
            Bundle result = new Bundle();
            result.putInt(AccountManager.KEY_ERROR_CODE,
                    AccountManager.ERROR_CODE_UNSUPPORTED_OPERATION);
            result.putString(AccountManager.KEY_ERROR_MESSAGE,
                    "Authenticator must override finishSession if startAddAccountSession"
                            + " or startUpdateCredentialsSession is overridden.");
            response.onResult(result);
            return result;
        }
        String authTokenType = sessionBundle.getString(KEY_AUTH_TOKEN_TYPE);
        Bundle options = sessionBundle.getBundle(KEY_OPTIONS);
        String[] requiredFeatures = sessionBundle.getStringArray(KEY_REQUIRED_FEATURES);
        Account account = sessionBundle.getParcelable(KEY_ACCOUNT, android.accounts.Account.class);
        boolean containsKeyAccount = sessionBundle.containsKey(KEY_ACCOUNT);

        // Actual options passed to add account or update credentials flow.
        Bundle sessionOptions = new Bundle(sessionBundle);
        // Remove redundant extras in session bundle before passing it to addAccount(...) or
        // updateCredentials(...).
        sessionOptions.remove(KEY_AUTH_TOKEN_TYPE);
        sessionOptions.remove(KEY_REQUIRED_FEATURES);
        sessionOptions.remove(KEY_OPTIONS);
        sessionOptions.remove(KEY_ACCOUNT);

        if (options != null) {
            // options may contains old system info such as
            // AccountManager.KEY_ANDROID_PACKAGE_NAME required by the add account flow or update
            // credentials flow, we should replace with the new values of the current call added
            // to sessionBundle by AccountManager or AccountManagerService.
            options.putAll(sessionOptions);
            sessionOptions = options;
        }

        // Session bundle created by startUpdateCredentialsSession default implementation should
        // contain KEY_ACCOUNT.
        if (containsKeyAccount) {
            return updateCredentials(response, account, authTokenType, options);
        }
        // Otherwise, session bundle was created by startAddAccountSession default implementation.
        return addAccount(response, accountType, authTokenType, requiredFeatures, sessionOptions);
    }

        public Bundle isCredentialsUpdateSuggested(
            final AccountAuthenticatorResponse response,
            Account account,
            String statusToken) throws NetworkErrorException {
        Bundle result = new Bundle();
        result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false);
        return result;
    }
}
