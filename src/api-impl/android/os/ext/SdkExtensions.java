package android.os.ext;

public class SdkExtensions {
	public static int getExtensionVersion(int extension) {
		return 0;
	}

	/* consistent with getExtensionVersion() above: no extension is installed. The SdkExtensions module source is
	 * not in the AOSP tree, so this is not checked against it. */
	public static java.util.Map<Integer, Integer> getAllExtensionVersions() {
		return java.util.Collections.emptyMap();
	}
}
