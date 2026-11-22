package android.os;

import android.content.Intent;

import java.util.ArrayList;
import java.util.List;

/*
 * A desktop session is one user: the owner of the device, no managed profile, no guest, not a demo
 * device, nothing restricted (AOSP UserManager's answers for a single-user device).
 */
public class UserManager {
	public static final String DISALLOW_MODIFY_ACCOUNTS = "no_modify_accounts";
	public static final String DISALLOW_INSTALL_APPS = "no_install_apps";
	public static final String DISALLOW_INSTALL_UNKNOWN_SOURCES = "no_install_unknown_sources";
	public static final String DISALLOW_CONFIG_WIFI = "no_config_wifi";
	public static final String DISALLOW_SHARE_LOCATION = "no_share_location";
	public static final String DISALLOW_OUTGOING_CALLS = "no_outgoing_calls";
	public static final String DISALLOW_SMS = "no_sms";

	/* AOSP UserManager.get(Context) */
	public static UserManager get(android.content.Context context) {
		return (UserManager) context.getSystemService(android.content.Context.USER_SERVICE);
	}

	public static final int QUIET_MODE_DISABLE_ONLY_IF_CREDENTIAL_NOT_REQUIRED = 0x1;

	/* AOSP UserManager: extras of the ACTION_CREATE_USER intent */
	private static final String ACTION_CREATE_USER = "android.os.action.CREATE_USER";
	public static final String EXTRA_USER_NAME = "android.os.extra.USER_NAME";
	public static final String EXTRA_USER_ACCOUNT_NAME = "android.os.extra.USER_ACCOUNT_NAME";
	public static final String EXTRA_USER_ACCOUNT_TYPE = "android.os.extra.USER_ACCOUNT_TYPE";
	public static final String EXTRA_USER_ACCOUNT_OPTIONS = "android.os.extra.USER_ACCOUNT_OPTIONS";

	/* AOSP UserManager.UserOperationException: the failure of a user operation (the system's add/remove user calls) */
	public static class UserOperationException extends RuntimeException {
		private final int mUserOperationResult;

		public UserOperationException(String message, int userOperationResult) {
			super(message);
			mUserOperationResult = userOperationResult;
		}

		public int getUserOperationResult() {
			return mUserOperationResult;
		}
	}

	public boolean isUserUnlocked() {
		return true;
	}
	public boolean isUserUnlocked(UserHandle user) {
		return true;
	}
	public boolean isUserRunning(UserHandle user) {
		return true;
	}
	public boolean isUserRunningOrStopping(UserHandle user) {
		return true;
	}
	public static boolean supportsMultipleUsers() {
		return false;
	}

	public long getSerialNumberForUser(UserHandle user) {
		return user.getIdentifier();
	}

	public UserHandle getUserForSerialNumber(long serialNumber) {
		return serialNumber == 0 ? Process.myUserHandle() : null;
	}

	public boolean isDemoUser() { return false; }
	public static boolean isDeviceInDemoMode(android.content.Context context) { return false; }
	public boolean isGuestUser() { return false; }
	public boolean isSystemUser() { return true; }
	public boolean isAdminUser() { return true; }
	public boolean isUserAGoat() { return false; }
	public boolean isManagedProfile() { return false; }
	public boolean isManagedProfile(int userId) { return false; }
	public boolean isProfile() { return false; }
	public boolean isRestrictedProfile() { return false; }
	public boolean isLinkedUser() { return false; }
	public boolean isQuietModeEnabled(UserHandle userHandle) { return false; }
	public boolean isUserForeground() { return true; }
	public boolean hasUserRestriction(String restrictionKey) { return false; }
	public Bundle getUserRestrictions() { return new Bundle(); }
	public Bundle getUserRestrictions(UserHandle userHandle) { return getUserRestrictions(); }
	public String getUserName() { return "Owner"; }
	public int getUserCount() { return 1; }
	public List<UserHandle> getUserProfiles() {
		List<UserHandle> profiles = new ArrayList<>();
		profiles.add(Process.myUserHandle());
		return profiles;
	}

	/* AOSP UserManager.createUserCreationIntent() */
	public static Intent createUserCreationIntent(String userName, String accountName, String accountType,
			PersistableBundle accountOptions) {
		Intent intent = new Intent(ACTION_CREATE_USER);
		if (userName != null) {
			intent.putExtra(EXTRA_USER_NAME, userName);
		}
		if (accountName != null && accountType == null) {
			throw new IllegalArgumentException("accountType must be specified if accountName is specified");
		}
		if (accountName != null) {
			intent.putExtra(EXTRA_USER_ACCOUNT_NAME, accountName);
		}
		if (accountType != null) {
			intent.putExtra(EXTRA_USER_ACCOUNT_TYPE, accountType);
		}
		if (accountOptions != null) {
			intent.putExtra(EXTRA_USER_ACCOUNT_OPTIONS, accountOptions);
		}
		return intent;
	}

	/* AOSP getApplicationRestrictions(): "an empty Bundle if there are no saved restrictions"; nobody saves any here */
	public Bundle getApplicationRestrictions(String packageName) {
		return new Bundle();
	}

	/* ATL does not record when the user was created; 0 as UserInfo.creationTime of a user without a time */
	public long getUserCreationTime(UserHandle userHandle) {
		return 0;
	}

	public static boolean isHeadlessSystemUserMode() {
		return false;
	}

	public boolean isCommunalProfile() {
		return false;
	}

	/* the one user is in the foreground and an admin (isUserForeground(), isAdminUser()) */
	public boolean isForegroundUserAdmin() {
		return true;
	}

	/* AOSP: an app without MANAGE_USERS/MODIFY_QUIET_MODE that is not the default launcher gets a SecurityException;
	 * ATL has no managed profile to switch either */
	public boolean requestQuietModeEnabled(boolean enableQuietMode, UserHandle userHandle) {
		throw new SecurityException("Can't modify quiet mode, caller is neither foreground default launcher nor has"
				+ " MANAGE_USERS/MODIFY_QUIET_MODE permission");
	}

	public boolean requestQuietModeEnabled(boolean enableQuietMode, UserHandle userHandle, int flags) {
		return requestQuietModeEnabled(enableQuietMode, userHandle);
	}

	/* AOSP: "The restrictions PIN functionality is no longer provided by the system ... always returns false" */
	@Deprecated
	public boolean setRestrictionsChallenge(String newPin) {
		return false;
	}

	/* AOSP: the system sets restrictions only for apps with MANAGE_USERS */
	@Deprecated
	public void setUserRestriction(String key, boolean value) {
		throw new SecurityException("You need MANAGE_USERS permission to: change user restriction");
	}

	/* AOSP: "This method is no longer supported" */
	@Deprecated
	public void setUserRestrictions(Bundle restrictions) {
		throw new UnsupportedOperationException("This method is no longer supported");
	}

	@Deprecated
	public void setUserRestrictions(Bundle restrictions, UserHandle userHandle) {
		throw new UnsupportedOperationException("This method is no longer supported");
	}
}
