package com.android.providers.telephony;

import android.atl.ATLLoadedAppManager;
import android.content.Context;
import android.app.SharedPreferencesImpl;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import java.io.File;
import java.util.HashMap;

/**
 * ATL: the Context of the telephony providers. In AOSP they live in the package
 * com.android.providers.telephony, whose data (databases/mmssms.db, app_parts) all apps share. ATL runs
 * them in the app's process, so this wrapper moves the data directory to that package's, the same for
 * every app: <code>ATLLoadedAppManager.getDataDir("com.android.providers.telephony")</code>.
 * Storage is neither device nor credential protected as far as the providers can tell, so the "encrypted
 * storage" contexts are this context itself.
 */
public class TelephonyProviderContext extends ContextWrapper {
	private static final String PACKAGE = "com.android.providers.telephony";

	private final File dataDir = ATLLoadedAppManager.getDataDir(PACKAGE);

	public TelephonyProviderContext(Context base) {
		super(base);
	}

	private static File dir(File dir) {
		dir.mkdirs();
		return dir;
	}

	@Override
	public File getDataDir() {
		return dir(dataDir);
	}

	@Override
	public File getFilesDir() {
		return dir(new File(dataDir, "files"));
	}

	/* as Context.getDatabasePath(): an absolute name is kept, a relative one is in <data dir>/databases */
	@Override
	public File getDatabasePath(String name) {
		if (name.charAt(0) == File.separatorChar) {
			return super.getDatabasePath(name);
		}
		return new File(dir(new File(dataDir, "databases")), name);
	}

	/* as ContextImpl.getDir(): <data dir>/app_<name> */
	@Override
	public File getDir(String name, int mode) {
		return dir(new File(dataDir, "app_" + name));
	}

	/* the preferences (initial_create_done) belong to the shared database, so they are shared too */
	private static final HashMap<String, SharedPreferences> prefs = new HashMap<>();

	@Override
	public SharedPreferences getSharedPreferences(String name, int mode) {
		File file = new File(dir(new File(dataDir, "shared_prefs")), name + ".xml");
		synchronized (prefs) {
			SharedPreferences p = prefs.get(file.getPath());
			if (p == null) {
				p = new SharedPreferencesImpl(file, mode);
				prefs.put(file.getPath(), p);
			}
			return p;
		}
	}

	@Override
	public String getPackageName() {
		return PACKAGE;
	}

	@Override
	public boolean isDeviceProtectedStorage() {
		return true;
	}

	@Override
	public boolean isCredentialProtectedStorage() {
		return false;
	}

	@Override
	public Context createDeviceProtectedStorageContext() {
		return this;
	}

	@Override
	public Context createCredentialProtectedStorageContext() {
		return this;
	}
}
