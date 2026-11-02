package android.app;

public class AppOpsManager {

	/* AOSP's AppOpsManager modes. ATL has no AppOpsService and grants what an app asks for, as it
	 * grants the permissions: every op is in mode MODE_ALLOWED. */
	public static final int MODE_ALLOWED = 0;
	public static final int MODE_IGNORED = 1;
	public static final int MODE_ERRORED = 2;
	public static final int MODE_DEFAULT = 3;
	public static final int MODE_FOREGROUND = 4;

	public static final String OPSTR_PICTURE_IN_PICTURE = "android:picture_in_picture";

	public static String permissionToOp(String permission) {
		return permission;
	}

	public int noteProxyOpNoThrow(String op, String pkg) {
		return 0;
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
