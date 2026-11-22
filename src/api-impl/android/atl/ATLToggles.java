package android.atl;

public final class ATLToggles {
	/* ghax.jar in front of the app's code replaces GMS client library classes of the app. On Android the app's
	 * classes always come from the app (its PathClassLoader only has the boot class path as parent), and an app
	 * shrunk by R8 needs its own copy (Booking.com: NoSuchFieldError zzb in AdvertisingIdClient$Info), so it is off
	 * unless asked for with ATL_GHAX=1 */
	public static final boolean ATL_GHAX = queryBooleanToggle("ATL_GHAX", false);

	private ATLToggles() {}

	private static boolean queryBooleanToggle(String name, boolean def) {
		String env = System.getenv(name);
		return env == null || env.isEmpty() ? def : "1".equals(env) || "true".equals(env);
	}
}
