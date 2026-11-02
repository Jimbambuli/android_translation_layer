package android.content.pm;

import java.util.Collections;
import java.util.List;

public class PackageInstaller {
	public static final int PACKAGE_SOURCE_UNSPECIFIED = 0;
	public static final int PACKAGE_SOURCE_OTHER = 1;
	public static final int PACKAGE_SOURCE_STORE = 2;
	public static final int PACKAGE_SOURCE_LOCAL_FILE = 3;
	public static final int PACKAGE_SOURCE_DOWNLOADED_FILE = 4;


	public List getMySessions() {
		return Collections.emptyList();
	}

	public void registerSessionCallback(SessionCallback callback) {}

	public abstract static class SessionCallback {
		public abstract void onActiveChanged(int sessionId, boolean active);

		public abstract void onBadgingChanged(int sessionId);

		public abstract void onCreated(int sessionId);

		public abstract void onFinished(int sessionId, boolean success);

		public abstract void onProgressChanged(int sessionId, float progress);
	}
}
