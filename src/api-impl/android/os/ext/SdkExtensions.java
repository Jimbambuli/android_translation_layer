package android.os.ext;

public class SdkExtensions {
	public static int getExtensionVersion(int extension) {
		return 0;
	}

	/* ATL reports no extension (getExtensionVersion() is 0 for all), so the map of the versions is empty
	 * (SdkExtensions source not available locally) */
	public static java.util.Map<Integer, Integer> getAllExtensionVersions() {
		return java.util.Collections.emptyMap();
	}
}
