package android.app;

public class AppOpsManager {

	/* AOSP's AppOpsManager modes. ATL has no AppOpsService and grants what an app asks for, as it
	 * grants the permissions: every op is in mode MODE_ALLOWED. */
	/* AOSP @hide: the process states that decide an op's mode (App Manager reads the fields) */
	public static final int UID_STATE_PERSISTENT = 100;
	public static final int UID_STATE_TOP = 200;
	public static final int UID_STATE_FOREGROUND_SERVICE_LOCATION = 300;
	public static final int UID_STATE_FOREGROUND_SERVICE = 400;
	public static final int UID_STATE_FOREGROUND = 500;
	public static final int UID_STATE_MAX_LAST_NON_RESTRICTED = UID_STATE_FOREGROUND;
	public static final int UID_STATE_BACKGROUND = 600;
	public static final int UID_STATE_CACHED = 700;
	public static final int MAX_PRIORITY_UID_STATE = UID_STATE_PERSISTENT;
	public static final int MIN_PRIORITY_UID_STATE = UID_STATE_CACHED;

	public static final int MODE_ALLOWED = 0;
	public static final int MODE_IGNORED = 1;
	public static final int MODE_ERRORED = 2;
	public static final int MODE_DEFAULT = 3;
	public static final int MODE_FOREGROUND = 4;

	public static final String OPSTR_PICTURE_IN_PICTURE = "android:picture_in_picture";

	public static String permissionToOp(String permission) {
		return permission;
	}

	@Deprecated
	public int noteProxyOpNoThrow(String op, String proxiedPackageName) {
		return noteProxyOpNoThrow(op, proxiedPackageName, android.os.Binder.getCallingUid(), null, null);
	}

	@Deprecated
	public int noteProxyOpNoThrow(String op, String proxiedPackageName, int proxiedUid) {
		return noteProxyOpNoThrow(op, proxiedPackageName, proxiedUid, null, null);
	}

	/* the op is noted and the mode answered: ATL has no AppOpsService to note it with, every op is allowed */
	public int noteProxyOpNoThrow(String op, String proxiedPackageName, int proxiedUid, String proxiedAttributionTag, String message) {
		return MODE_ALLOWED;
	}

	/* AOSP AppOpsManager.noteOp/noteOpNoThrow/noteProxyOp (string forms): the op is noted with the AppOpsService and
	 * its mode answered, noteOp/noteProxyOp throw SecurityException on MODE_ERRORED. Every op is MODE_ALLOWED in ATL,
	 * so they answer that and never throw. */
	@Deprecated
	public int noteOp(String op, int uid, String packageName) {
		return noteOp(op, uid, packageName, null, null);
	}

	public int noteOp(String op, int uid, String packageName, String attributionTag, String message) {
		return MODE_ALLOWED;
	}

	@Deprecated
	public int noteOpNoThrow(String op, int uid, String packageName) {
		return noteOpNoThrow(op, uid, packageName, null, null);
	}

	public int noteOpNoThrow(String op, int uid, String packageName, String attributionTag, String message) {
		return MODE_ALLOWED;
	}

	@Deprecated
	public int noteProxyOp(String op, String proxiedPackageName) {
		return noteProxyOp(op, proxiedPackageName, android.os.Binder.getCallingUid(), null, null);
	}

	public int noteProxyOp(String op, String proxiedPackageName, int proxiedUid, String proxiedAttributionTag, String message) {
		return MODE_ALLOWED;
	}

	@Deprecated
	public int checkOpNoThrow(String op, int uid, String packageName) {
		return unsafeCheckOpNoThrow(op, uid, packageName);
	}

	public int unsafeCheckOpNoThrow(String op, int uid, String packageName) {
		return MODE_ALLOWED;
	}

	@Deprecated
	public int checkOp(String op, int uid, String packageName) {
		return unsafeCheckOp(op, uid, packageName);
	}

	public int unsafeCheckOp(String op, int uid, String packageName) {
		return MODE_ALLOWED;
	}
}
