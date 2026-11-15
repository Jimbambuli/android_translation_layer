package android.os;

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
}
