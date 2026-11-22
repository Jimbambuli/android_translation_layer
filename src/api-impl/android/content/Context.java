package android.content;

import android.R;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.AppOpsManager;
import android.app.Application;
import android.app.ContextImpl;
import android.app.KeyguardManager;
import android.app.NotificationManager;
import android.app.Service;
import android.app.SharedPreferencesImpl;
import android.app.StatusBarManager;
import android.app.UiModeManager;
import android.app.job.JobScheduler;
import android.atl.ATLLoadedApp;
import android.atl.ATLLoadedAppManager;
import android.atl.annotation.Export;
import android.bluetooth.BluetoothManager;
import android.content.pm.ApplicationInfo;
import android.os.UserHandle;
import android.content.pm.PackageManager;
import android.content.pm.PackageParser;
import android.content.pm.ShortcutManager;
import android.content.res.AssetManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import android.hardware.SensorManager;
import android.hardware.display.ColorDisplayManager;
import android.hardware.display.DisplayManager;
import android.hardware.input.InputManager;
import android.hardware.usb.UsbManager;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.MediaRouter;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerExecutor;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.ParcelFileDescriptor;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.UserManager;
import android.os.Vibrator;
import android.telephony.TelephonyManager;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Slog;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.WindowManagerImpl;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.CaptioningManager;
import android.view.inputmethod.InputMethodManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.Provider;
import java.security.Security;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

public abstract class Context {
	private final static String TAG = "Context";

	public static final int MODE_PRIVATE = 0;
	public static final int MODE_APPEND = 0x8000;
	public static final String LAYOUT_INFLATER_SERVICE = "layout_inflater";
	public static final String ACTIVITY_SERVICE = "activity";
	public static final String AUDIO_SERVICE = "audio";
	public static final String CLIPBOARD_SERVICE = "clipboard";
	public static final String DISPLAY_SERVICE = "display";
	public static final String INPUT_METHOD_SERVICE = "input_method";
	public static final String LOCATION_SERVICE = "location";
	public static final String MEDIA_ROUTER_SERVICE = "media_router";
	public static final String POWER_SERVICE = "power";
	public static final String VIBRATOR_SERVICE = "vibrator";
	public static final String VIBRATOR_MANAGER_SERVICE = "vibrator_manager";
	public static final String WINDOW_SERVICE = "window";
	public static final String SENSOR_SERVICE = "sensor";
	public static final String CONNECTIVITY_SERVICE = "connectivity";
	public static final String KEYGUARD_SERVICE = "keyguard";
	public static final String TELEPHONY_SERVICE = "phone";
	public static final String USB_SERVICE = "usb";
	public static final String NOTIFICATION_SERVICE = "notification";
	public static final String ALARM_SERVICE = "alarm";
	public static final String INPUT_SERVICE = "input";
	public static final String UI_MODE_SERVICE = "uimode";
	public static final String ROLE_SERVICE = "role";
	public static final String ACCESSIBILITY_SERVICE = "accessibility";
	public static final String WIFI_SERVICE = "wifi";
	public static final String BLUETOOTH_SERVICE = "bluetooth";
	public static final String JOB_SCHEDULER_SERVICE = "jobscheduler";
	public static final String APP_OPS_SERVICE = "appops";
	public static final String USER_SERVICE = "user";
	public static final String CROSS_PROFILE_APPS_SERVICE = "crossprofileapps";
	public static final String LAUNCHER_APPS_SERVICE = "launcherapps";
	public static final String CAPTIONING_SERVICE = "captioning";
	public static final String STATUS_BAR_SERVICE = "statusbar";
	public static final String CAMERA_SERVICE = "camera";
	public static final String COLOR_DISPLAY_SERVICE = "color_display";
	public static final String SEARCH_SERVICE = "search";
	public static final String STORAGE_SERVICE = "storage";
	public static final String BATTERY_SERVICE = "batterymanager";
	public static final String DOWNLOAD_SERVICE = "download";
	public static final String ACCOUNT_SERVICE = "account";
	public static final String DEVICE_POLICY_SERVICE = "device_policy";
	public static final String DROPBOX_SERVICE = "dropbox";
	public static final String TEXT_SERVICES_MANAGER_SERVICE = "textservices";
	public static final String TELECOM_SERVICE = "telecom";
	public static final String WALLPAPER_SERVICE = "wallpaper";
	public static final String RESTRICTIONS_SERVICE = "restrictions";
	public static final String PRINT_SERVICE = "print";
	public static final String CONSUMER_IR_SERVICE = "consumer_ir";
	public static final String FINGERPRINT_SERVICE = "fingerprint";
	public static final String TV_INPUT_SERVICE = "tv_input";
	public static final String USAGE_STATS_SERVICE = "usagestats";
	public static final String STORAGE_STATS_SERVICE = "storagestats";
	public static final String MEDIA_PROJECTION_SERVICE = "media_projection";
	public static final String APPWIDGET_SERVICE = "appwidget";
	public static final String SHORTCUT_SERVICE = "shortcut";
	public static final String AUTOFILL_MANAGER_SERVICE = "autofill";
	public static final String TEXT_CLASSIFICATION_SERVICE = "textclassification";
	public static final String LOCALE_SERVICE = "locale";
	public static final String GRAMMATICAL_INFLECTION_SERVICE = "grammatical_inflection";
	public static final String TRANSLATION_MANAGER_SERVICE = "translation";
	public static final String UI_TRANSLATION_SERVICE = "ui_translation";
	public static final String TELEPHONY_SUBSCRIPTION_SERVICE = "telephony_subscription_service";
	public static final String NSD_SERVICE = "servicediscovery";
	public static final String WIFI_P2P_SERVICE = "wifip2p";
	public static final String NFC_SERVICE = "nfc";
	public static final String MEDIA_SESSION_SERVICE = "media_session";
	public static final String MEDIA_METRICS_SERVICE = "media_metrics";
	public static final String DOMAIN_VERIFICATION_SERVICE = "domain_verification";


	public static PackageManager package_manager;
	public static Configuration sys_config;

	// TODO: Migrate to ATLLoadedApp, cannot remove yet due to being called by native
	// The current replacement is ATLLoadedApp.getPrimaryApplication().getApplication()
	@Deprecated
	private static Application this_application;

	File data_dir = null;
	File prefs_dir = null;
	File files_dir = null;
	File obb_dir = null;
	File cache_dir = null;
	File nobackup_dir = null;
	File code_cache_dir = null;

	private static final ReceiverRegistry receiverRegistrations = new ReceiverRegistry();

	static {
		sys_config = new Configuration();
		native_updateConfig(sys_config);
		/* GTK draws the logical pixels at the monitor's scale: resources are chosen for the density
		 * of the device pixels (see Resources.updateConfiguration), the app works at DENSITY_DEVICE */
		DisplayMetrics.DENSITY_DEVICE_PIXELS = (int)(DisplayMetrics.DENSITY_DEVICE * native_getMonitorScale() + 0.5);
		sys_config.setLayoutDirection(sys_config.locale); // like AOSP, from the locale
		android.atl.ATLThemeOverlay.apply(); // the accent color of the desktop (Material You)
		ATLLoadedApp primary_application = ATLLoadedApp.getPrimaryApplication();

		ApplicationInfo application_info = primary_application.pkg.applicationInfo;
		application_info.dataDir = Environment.getExternalStorageDirectory().getAbsolutePath();
		application_info.nativeLibraryDir = (new File(Environment.getExternalStorageDirectory(), "lib")).getAbsolutePath();
		// like ActivityThread.handleBindApplication
		if ((application_info.flags & ApplicationInfo.FLAG_LARGE_HEAP) != 0)
			dalvik.system.VMRuntime.getRuntime().clearGrowthLimit();
		package_manager = new PackageManager();

		Provider provider = new Provider("AndroidKeyStore", 1.0, "Android KeyStore provider") {};
		provider.put("KeyStore.AndroidKeyStore", "android.security.keystore.AndroidKeyStore");
		provider.put("KeyStore.AndroidCAStore", "android.security.keystore.AndroidCAStore");
		provider.put("KeyGenerator.AES", "android.security.keystore.KeyGenerator$AES");
		provider.put("KeyGenerator.HmacSHA512", "android.security.keystore.KeyGenerator$HmacSHA512");
		provider.put("KeyPairGenerator.RSA", "android.security.keystore.AndroidKeyStoreKeyPairGeneratorSpi$RSA");
		Security.addProvider(provider);
		// like AndroidKeyStoreProvider.install(): ciphers for AndroidKeyStore keys come from their own provider,
		// which takes only those keys (SupportedKeyClasses). AOSP puts it after Bouncy Castle, which cannot use
		// keystore2's key handles; ATL's keys are encodable, so it goes first to keep Bouncy Castle off them.
		Provider workaround = new Provider("AndroidKeyStoreBCWorkaround", 1.0, "Android KeyStore security provider to work around Bouncy Castle") {};
		workaround.put("Cipher.AES/GCM/NoPadding", "android.security.keystore.AndroidKeyStoreAuthenticatedAESCipherSpi$GCM");
		workaround.put("Cipher.AES/GCM/NoPadding SupportedKeyClasses", "android.security.keystore.AndroidKeyStoreSecretKey");
		// AndroidKeyStoreBCWorkaroundProvider's RSA ciphers, for AndroidKeyStore private keys
		String rsa = "android.security.keystore.AndroidKeyStoreRSACipherSpi$";
		String[][] rsaCiphers = {
		    {"NoPadding", "NoPadding"},
		    {"PKCS1Padding", "PKCS1Padding"},
		    {"OAEPPadding", "OAEPWithSHA1AndMGF1Padding"},
		    {"OAEPWithSHA-1AndMGF1Padding", "OAEPWithSHA1AndMGF1Padding"},
		    {"OAEPWithSHA-224AndMGF1Padding", "OAEPWithSHA224AndMGF1Padding"},
		    {"OAEPWithSHA-256AndMGF1Padding", "OAEPWithSHA256AndMGF1Padding"},
		    {"OAEPWithSHA-384AndMGF1Padding", "OAEPWithSHA384AndMGF1Padding"},
		    {"OAEPWithSHA-512AndMGF1Padding", "OAEPWithSHA512AndMGF1Padding"}};
		for (String[] cipher : rsaCiphers) {
			workaround.put("Cipher.RSA/ECB/" + cipher[0], rsa + cipher[1]);
			workaround.put("Cipher.RSA/ECB/" + cipher[0] + " SupportedKeyClasses", "android.security.keystore.AndroidKeyStorePrivateKey");
			// AOSP maps RSA/None/OAEPWithSHA-224AndMGF1Padding to the SHA-256 one
			workaround.put("Alg.Alias.Cipher.RSA/None/" + cipher[0], "RSA/ECB/" + (cipher[0].contains("SHA-224") ? "OAEPWithSHA-256AndMGF1Padding" : cipher[0]));
		}
		Security.insertProviderAt(workaround, 1);

		for (PackageParser.Activity receiver : primary_application.pkg.receivers) {
			if (receiver.intents == null)
				continue;
			for (PackageParser.ActivityIntentInfo intent : receiver.intents) {
				if (intent.matchAction("org.unifiedpush.android.connector.MESSAGE")) {
					nativeExportUnifiedPush(application_info.packageName);
					break;
				}
			}
		}
	}

	protected static native void native_updateConfig(Configuration config);
	private static native double native_getMonitorScale();
	protected static native void native_updateConfigFromWindow(long window, Configuration config);
	protected static native void nativeOpenFile(int fd);
	/* appLabels/appIcons: other installed apps that take what is shared (ATLLoadedAppManager.shareTargets()); when
	 * the user picks one, the sheet calls runShareAppTarget() with its index */
	protected static native void nativeShareFile(String text, int fd, String[] labels, String[] appLabels, String[] appIcons);

	/** @hide set by ContextImpl.share() */
	public static Runnable[] shareAppTargets;

	@Export
	static void runShareAppTarget(int index) {
		Runnable[] targets = shareAppTargets;
		shareAppTargets = null;
		if (targets != null && index >= 0 && index < targets.length)
			targets[index].run();
	}
	/* the desktop's choice of applications (OpenURI portal with «ask») for a file (fd) or a link; false without
	 * the portal */
	protected static native boolean nativeOpenWith(int fd, String uri);
	private static native void nativeExportUnifiedPush(String packageName);
	private static native void nativeRegisterUnifiedPush(String token, String application);
	protected static native void nativeStartExternalService(Intent service);

	@Export
	static Application createApplication(long native_window) throws Exception {
		Application application = ATLLoadedApp.getPrimaryApplication().getApplication();
		application.native_window = native_window;
		this_application = application;
		/* AOSP's ClipboardService knows the clipboard before an app asks (TextView.canPaste() for the floating
		 * toolbar); ATL's ClipboardManager reads the desktop's clipboard asynchronously, so it starts with the app */
		application.getSystemService(CLIPBOARD_SERVICE);
		return application;
	}

	public Context() {
		if (Log.isLoggable(TAG, Log.VERBOSE))
			Slog.v(TAG, "new Context! this one is: " + this);
	}

	public int checkPermission(String permission, int pid, int uid) {
		return getPackageManager().checkPermission(permission, getPackageName());
	}

	public abstract Resources.Theme getTheme();

	public abstract ApplicationInfo getApplicationInfo();

	/* the application of the context's own package, like AOSP's LoadedApk.getApplication() */
	public Context getApplicationContext() {
		ATLLoadedApp app = get_atl_loaded_app();
		if (app == null || "android".equals(app.pkg.packageName))
			app = ATLLoadedApp.getPrimaryApplication();
		return app.getApplication();
	}

	public ContentResolver getContentResolver() {
		return new ContentResolver(this);
	}

	public abstract Object getSystemService(String name);

	/* AOSP Context.getSystemService(Class): the name comes from SystemServiceRegistry (getSystemServiceName),
	 * a class that is no system service gives null */
	@SuppressWarnings("unchecked")
	public final <T> T getSystemService(Class<T> serviceClass) {
		String serviceName = getSystemServiceName(serviceClass);
		return serviceName != null ? (T)getSystemService(serviceName) : null;
	}

	public abstract String getSystemServiceName(Class<?> serviceClass);

	/* the sticky broadcasts apps sent with sendStickyBroadcast(), by action */
	private static final Map<String, Intent> stickyIntents = new ConcurrentHashMap<>();

	/* AOSP ActivityManagerService.registerReceiver(): every sticky broadcast the filter matches, an app's
	 * own over the system's of the same action; the system's are ACTION_BATTERY_CHANGED,
	 * CONNECTIVITY_ACTION and ACTION_HDMI_AUDIO_PLUG, built from the current state */
	private static java.util.List<Intent> stickyIntentsFor(IntentFilter filter) {
		java.util.ArrayList<Intent> found = new java.util.ArrayList<>();
		if (filter == null)
			return found;
		for (int i = 0; i < filter.countActions(); i++) {
			String action = filter.getAction(i);
			Intent sticky = stickyIntents.get(action);
			if (sticky == null && Intent.ACTION_BATTERY_CHANGED.equals(action))
				sticky = android.os.BatteryManager.batteryChangedIntent();
			if (sticky == null && android.net.ConnectivityManager.CONNECTIVITY_ACTION.equals(action))
				sticky = android.net.ConnectivityManager.stickyIntentFor(filter);
			if (sticky == null && android.media.AudioManager.ACTION_HDMI_AUDIO_PLUG.equals(action))
				sticky = android.media.AudioManager.hdmiAudioPlugStickyIntent();
			if (sticky != null)
				found.add(new Intent(sticky));
		}
		return found;
	}

	/* AOSP: with a receiver the sticky broadcasts are delivered at once (on the main thread); the first of
	 * them is returned. Without a receiver (the state query registerReceiver(null, filter)) nothing is
	 * registered; the result is null when no sticky broadcast matches */
	public Intent registerReceiver(final BroadcastReceiver receiver, IntentFilter filter) {
		return registerReceiverInternal(receiver, filter, null);
	}

	private Intent registerReceiverInternal(final BroadcastReceiver receiver, IntentFilter filter, final Handler registeredWith) {
		final Handler scheduler = registeredWith != null ? registeredWith : receiver != null ? new Handler(Looper.getMainLooper()) : null;
		/* AOSP ContextImpl.registerReceiverInternal(): the ReceiverDispatcher keeps getOuterContext(), and
		 * LoadedApk.ReceiverDispatcher.Args.getRunnable() calls onReceive(mContext, intent) with it */
		final Context context = receiverOwner(this);
		if (receiver != null)
			receiverRegistrations.add(context, receiver, filter, registeredWith);
		java.util.List<Intent> stickies = stickyIntentsFor(filter);
		if (receiver != null) {
			for (final Intent sticky : stickies) {
				scheduler.post(new Runnable() {
					public void run() {
						receiver.onReceive(context, new Intent(sticky));
					}
				});
			}
		}
		return stickies.isEmpty() ? null : stickies.get(0);
	}

	public Looper getMainLooper() {
		return Looper.getMainLooper();
	}

	/* AOSP Context.getMainExecutor() */
	public Executor getMainExecutor() {
		return new HandlerExecutor(new Handler(getMainLooper()));
	}

	@Export
	public String getPackageName() {
		return getApplicationInfo().packageName;
	}

	/* AOSP ContextImpl.getOpPackageName(): the package the app ops are noted for, the app's own */
	public String getOpPackageName() {
		return getPackageName();
	}

	public String getPackageCodePath() {
		return getApplicationInfo().sourceDir;
	}

	public int getColor(int resId) {
		return this.getResources().getColor(resId, getTheme());
	}

	public final String getString(int resId) {
		return this.getResources().getString(resId);
	}

	public final String getString(int resId, Object... formatArgs) {
		return this.getResources().getString(resId, formatArgs);
	}

	public PackageManager getPackageManager() {
		return package_manager;
	}

	public abstract Resources getResources();

	/** The default device ID, which is of the physical device on which the app is running (Android 14). */
	public static final int DEVICE_ID_DEFAULT = 0;
	/** Invalid device ID. */
	public static final int DEVICE_ID_INVALID = -1;

	/* AOSP Context.getDeviceId(): the id of the device the context is associated with; the one device of ATL has the
	 * default id (there are no virtual devices) */
	public int getDeviceId() {
		return DEVICE_ID_DEFAULT;
	}

	/* the device id never changes: there is one device, so no listener is ever called */
	public void registerDeviceIdChangeListener(java.util.concurrent.Executor executor, java.util.function.IntConsumer listener) {
		if (executor == null || listener == null)
			throw new NullPointerException("executor and listener must not be null");
	}

	public void unregisterDeviceIdChangeListener(java.util.function.IntConsumer listener) {
		if (listener == null)
			throw new NullPointerException("listener must not be null");
	}

	public AssetManager getAssets() {
		return getResources().getAssets();
	}

	/* ContextImpl.makeFilename(), without BlockGuard */
	private File makeFilename(File base, String name) {
		if (name.indexOf(File.separatorChar) < 0) {
			return new File(base, name);
		}
		throw new IllegalArgumentException(
		    "File " + name + " contains a path separator");
	}

	/* ContextImpl.ensurePrivateDirExists(). ATL: mkdirs() instead of Os.mkdir() with mode 0771, since
	 * the parents of some of the directories (cache, external) do not exist yet */
	private static File ensurePrivateDirExists(File file) {
		if (!file.exists()) {
			if (!file.mkdirs() && !file.exists())
				Slog.w(TAG, "Failed to ensure " + file);
			// (if it exists now, we raced with someone; that's okay)
		}
		return file;
	}

	private final Object mSync = new Object();

	/* like AOSP's LoadedApk.getDataDirFile(): the directory of the context's own package, so a
	 * package loaded into the app's process (Play Services) keeps its own data */
	private File getDataDirFile() {
		if (data_dir == null) {
			ApplicationInfo info = getApplicationInfo();
			data_dir = info != null && info.dataDir != null ? new File(info.dataDir) : android.os.Environment.getExternalStorageDirectory();
		}
		return data_dir;
	}

	public File getDataDir() {
		return getDataDirFile();
	}

	public File getFilesDir() {
		synchronized (mSync) {
			if (files_dir == null) {
				files_dir = new File(getDataDirFile(), "files");
			}
			return ensurePrivateDirExists(files_dir);
		}
	}

	/* ContextImpl.getCodeCacheDir(); ATL has no code cache of its own, it is only a directory */
	public File getCodeCacheDir() {
		synchronized (mSync) {
			if (code_cache_dir == null) {
				code_cache_dir = new File(getDataDirFile(), "code_cache");
			}
			return ensurePrivateDirExists(code_cache_dir);
		}
	}

	public File getExternalFilesDir(String type) {
		// Operates on primary external storage
		final File[] dirs = getExternalFilesDirs(type);
		return (dirs != null && dirs.length > 0) ? dirs[0] : null;
	}

	public File[] getExternalFilesDirs(String type) {
		synchronized (mSync) {
			/* Environment.buildExternalStorageAppFilesDirs() and buildPaths() */
			File[] dirs = {new File(getDataDirFile(), "Android/data/" + getPackageName() + "/files")};
			if (type != null) {
				dirs = new File[] {new File(dirs[0], type)};
			}
			return ensureExternalDirsExistOrFilter(dirs);
		}
	}

	/* ContextImpl.ensureExternalDirsExistOrFilter(), without asking StorageManager (vold) to create what
	 * this process cannot */
	private File[] ensureExternalDirsExistOrFilter(File[] dirs) {
		final File[] result = new File[dirs.length];
		for (int i = 0; i < dirs.length; i++) {
			File dir = dirs[i];
			if (!dir.exists()) {
				try {
					dir.mkdirs();
				} catch (Exception e) {
					Slog.w(TAG, "Failed to ensure " + dir + ": " + e);
					dir = null;
				}
			}
			if (dir != null && !dir.exists()) {
				Slog.w(TAG, "Failed to ensure " + dir);
				dir = null;
			}
			result[i] = dir;
		}
		return result;
	}

	public File getObbDir() {
		// Operates on primary external storage
		final File[] dirs = getObbDirs();
		return (dirs != null && dirs.length > 0) ? dirs[0] : null;
	}

	public File[] getObbDirs() {
		synchronized (mSync) {
			if (obb_dir == null) {
				obb_dir = new File(getDataDirFile(), "Android/obb/" + getPackageName());
			}
			return ensureExternalDirsExistOrFilter(new File[] {obb_dir});
		}
	}

	/* ATL: in /tmp/atl_cache (AOSP: <data dir>/cache), what the system may clear at any time */
	public File getCacheDir() {
		synchronized (mSync) {
			if (cache_dir == null) {
				cache_dir = new File("/tmp/atl_cache/" + getPackageName());
			}
			return ensurePrivateDirExists(cache_dir);
		}
	}

	public File getExternalCacheDir() {
		// Operates on primary external storage
		final File[] dirs = getExternalCacheDirs();
		return (dirs != null && dirs.length > 0) ? dirs[0] : null;
	}

	public File[] getExternalCacheDirs() {
		synchronized (mSync) {
			/* Environment.buildExternalStorageAppCacheDirs() */
			return ensureExternalDirsExistOrFilter(
			    new File[] {new File(getDataDirFile(), "Android/data/" + getPackageName() + "/cache")});
		}
	}

	/* the directory is <data dir>/no_backup/<package> here, not <data dir>/no_backup as in AOSP: an
	 * app (WorkManager) that kept its database there would not find it again */
	public File getNoBackupFilesDir() {
		synchronized (mSync) {
			if (nobackup_dir == null) {
				nobackup_dir = new File(getDataDirFile(), "no_backup/" + getPackageName());
			}
			return ensurePrivateDirExists(nobackup_dir);
		}
	}

	private File getPreferencesDir() {
		synchronized (mSync) {
			if (prefs_dir == null) {
				prefs_dir = new File(getDataDirFile(), "shared_prefs");
			}
			return ensurePrivateDirExists(prefs_dir);
		}
	}

	public File[] getExternalMediaDirs() {
		synchronized (mSync) {
			/* Environment.buildExternalStorageAppMediaDirs() */
			return ensureExternalDirsExistOrFilter(
			    new File[] {new File(getDataDirFile(), "Android/media/" + getPackageName())});
		}
	}

	/* ContextImpl.getDir(): <data dir>/app_<name>, without the file mode (there is one user) */
	public File getDir(String name, int mode) {
		name = "app_" + name;
		File file = makeFilename(getDataDirFile(), name);
		if (!file.exists()) {
			file.mkdir();
		}
		return file;
	}

	public File getFileStreamPath(String name) {
		return makeFilename(getFilesDir(), name);
	}

	public boolean deleteFile(String name) {
		File f = makeFilename(getFilesDir(), name);
		return f.delete();
	}

	public File getSharedPreferencesPath(String name) {
		return makeFilename(getPreferencesDir(), name + ".xml");
	}

	public File getSharedPrefsFile(String name) {
		return getSharedPreferencesPath(name);
	}

	private static Map<String, SharedPreferences> sharedPrefs = new HashMap<String, SharedPreferences>();

	public SharedPreferences getSharedPreferences(String name, int mode) {
		if (Log.isLoggable(TAG, Log.VERBOSE))
			Slog.v(TAG, "\n\n...> getSharedPreferences(" + name + ")\n\n");
		/* by file as AOSP's sSharedPrefsCache: packages in the same process have their own */
		File prefsFile = getSharedPrefsFile(name);
		synchronized (sharedPrefs) {
			SharedPreferences prefs = sharedPrefs.get(prefsFile.getPath());
			if (prefs == null) {
				prefs = new SharedPreferencesImpl(prefsFile, mode);
				sharedPrefs.put(prefsFile.getPath(), prefs);
			}
			return prefs;
		}
	}

	@Export
	public abstract ClassLoader getClassLoader();

	@Export
	public abstract ComponentName startService(Intent intent);

	/* there are no background execution limits, so this is the same as startService() */
	public ComponentName startForegroundService(Intent service) {
		return startService(service);
	}

	public FileInputStream openFileInput(String name) throws FileNotFoundException {
		File f = makeFilename(getFilesDir(), name);
		return new FileInputStream(f);
	}

	/* ContextImpl.openFileOutput(), without the file mode */
	public FileOutputStream openFileOutput(String name, int mode) throws FileNotFoundException {
		final boolean append = (mode & MODE_APPEND) != 0;
		File f = makeFilename(getFilesDir(), name);
		try {
			return new FileOutputStream(f, append);
		} catch (FileNotFoundException e) {
		}

		File parent = f.getParentFile();
		parent.mkdir();
		return new FileOutputStream(f, append);
	}

	public int checkCallingOrSelfPermission(String permission) {
		return getPackageManager().checkPermission(permission, getPackageName());
	}

	public int checkSelfPermission(String permission) {
		return checkCallingOrSelfPermission(permission);
	}

	/* ATL runs one app and no IPC: like AOSP outside of a binder call, there is no calling process that could
	 * hold a permission, and the app itself may access its own URIs */
	public int checkCallingPermission(String permission) {
		return PackageManager.PERMISSION_DENIED;
	}

	public void enforceCallingPermission(String permission, String message) {
		if (checkCallingPermission(permission) != PackageManager.PERMISSION_GRANTED)
			throw new SecurityException((message != null ? message + ": " : "") + "Neither user nor current process has " + permission + ".");
	}

	public int checkCallingUriPermission(Uri uri, int modeFlags) {
		return PackageManager.PERMISSION_DENIED;
	}

	public int checkCallingOrSelfUriPermission(Uri uri, int modeFlags) {
		return PackageManager.PERMISSION_GRANTED;
	}

	/* AOSP ContextImpl.checkUriPermission asks the ActivityManager for the grants of pid/uid;
	 * checkCallingOrSelfUriPermission is this call with the own ids outside of a binder call. ATL keeps
	 * no grants for other uids, so: the own uid like checkCallingOrSelfUriPermission, every other denied */
	public int checkUriPermission(Uri uri, int pid, int uid, int modeFlags) {
		return uid == android.os.Process.myUid() ? checkCallingOrSelfUriPermission(uri, modeFlags) : PackageManager.PERMISSION_DENIED;
	}

	/* AOSP ContextImpl.checkUriPermissions(): checkUriPermission() of each Uri, in order */
	public int[] checkUriPermissions(java.util.List<Uri> uris, int pid, int uid, int modeFlags) {
		int[] res = new int[uris.size()];
		for (int i = 0; i < res.length; i++)
			res[i] = checkUriPermission(uris.get(i), pid, uid, modeFlags);
		return res;
	}

	/* AOSP ContextImpl.registerComponentCallbacks(): the callbacks belong to the application, which calls them
	 * (Application.onConfigurationChanged(), onLowMemory(), onTrimMemory()) */
	public void registerComponentCallbacks(ComponentCallbacks callbacks) {
		Context app = getApplicationContext();
		if (app != null && app != this)
			app.registerComponentCallbacks(callbacks);
	}

	public void unregisterComponentCallbacks(ComponentCallbacks callbacks) {
		Context app = getApplicationContext();
		if (app != null && app != this)
			app.unregisterComponentCallbacks(callbacks);
	}

	public abstract boolean bindService(final Intent intent, final ServiceConnection serviceConnection, int flags);

	/* AOSP ContextImpl.bindServiceAsUser: ATL has the one user only */
	public boolean bindServiceAsUser(Intent service, ServiceConnection conn, int flags, UserHandle user) {
		return bindService(service, conn, flags);
	}

	/* the dispatchers of the connections bound with an executor (LoadedApk.mServices), which unbindService()
	 * unbinds in place of the app's connection */
	private static final java.util.WeakHashMap<ServiceConnection, ServiceConnection> executor_dispatchers = new java.util.WeakHashMap<>();

	/** @hide */
	public static ServiceConnection atl_takeExecutorDispatcher(ServiceConnection conn) {
		synchronized (executor_dispatchers) {
			return executor_dispatchers.remove(conn);
		}
	}

	/* AOSP Context.bindService(Intent, int, Executor, ServiceConnection) (API 29): like ContextImpl's
	 * ServiceDispatcher with an executor, the connection's callbacks run on the executor */
	public boolean bindService(Intent service, int flags, java.util.concurrent.Executor executor, ServiceConnection conn) {
		return bindServiceInstance(service, flags, null, executor, conn);
	}

	/* AOSP Context.bindIsolatedService() (API 29): a service with android:isolatedProcess, in a process of its own
	 * for each instance name */
	public boolean bindIsolatedService(Intent service, int flags, String instanceName, java.util.concurrent.Executor executor, ServiceConnection conn) {
		if (instanceName == null)
			throw new NullPointerException("null instanceName");
		return bindServiceInstance(service, flags, instanceName, executor, conn);
	}

	/** @hide bindService() of the instance of an isolated service (ContextImpl) */
	public boolean atl_bindService(Intent service, ServiceConnection conn, int flags, String instanceName) {
		return bindService(service, conn, flags);
	}

	private boolean bindServiceInstance(Intent service, int flags, String instanceName, final java.util.concurrent.Executor executor, final ServiceConnection conn) {
		if (executor == null)
			throw new IllegalArgumentException("executor must not be null");
		if (conn == null)
			throw new IllegalArgumentException("connection is null");
		ServiceConnection dispatcher = new ServiceConnection() {
			@Override
			public void onServiceConnected(final ComponentName name, final android.os.IBinder binder) {
				executor.execute(new Runnable() {
					@Override
					public void run() {
						conn.onServiceConnected(name, binder);
					}
				});
			}

			@Override
			public void onServiceDisconnected(final ComponentName name) {
				executor.execute(new Runnable() {
					@Override
					public void run() {
						conn.onServiceDisconnected(name);
					}
				});
			}
		};
		synchronized (executor_dispatchers) {
			executor_dispatchers.put(conn, dispatcher);
		}
		return atl_bindService(service, dispatcher, flags, instanceName);
	}

	/* For use from native code */
	@Export
	static Activity resolveActivityInternal(Intent intent) throws ReflectiveOperationException {
		String className = null;
		ATLLoadedApp primary = ATLLoadedApp.getPrimaryApplication();
		if (intent.getComponent() != null) {
			className = intent.getComponent().getClassName();
		} else {
			int best_score = -5;
			for (PackageParser.Activity activity : primary.pkg.activities) {
				for (PackageParser.IntentInfo intentInfo : activity.intents) {
					int score = intentInfo.match(intent.getAction(), intent.getType(), intent.getScheme(), intent.getData(), intent.getCategories(), "Context");
					if (score > best_score && score > 0) {
						className = activity.className;
						best_score = score;
					}
				}
			}
		}
		if (className != null) {
			return Activity.internalCreateActivity(className, primary.getNativeWindow(), intent);
		} else {
			return null;
		}
	}

	@Export
	public abstract void startActivity(Intent intent);

	/* AOSP: starts all of them, the last one on top of the others */
	public void startActivities(Intent[] intents) {
		startActivities(intents, null);
	}

	public void startActivities(Intent[] intents, Bundle options) {
		for (Intent intent : intents)
			startActivity(intent, options);
	}

	public void startActivity(Intent intent, Bundle options) {
		startActivity(intent);
	}

	public final TypedArray obtainStyledAttributes(AttributeSet set, int[] attrs) {
		return getTheme().obtainStyledAttributes(set, attrs, 0, 0);
	}
	public final TypedArray obtainStyledAttributes(AttributeSet set, int[] attrs, int defStyleAttr, int defStyleRes) {
		return getTheme().obtainStyledAttributes(set, attrs, defStyleAttr, defStyleRes);
	}
	public final TypedArray obtainStyledAttributes(int resid, int[] attrs) {
		return getTheme().obtainStyledAttributes(resid, attrs);
	}
	public final TypedArray obtainStyledAttributes(int[] attrs) {
		return getTheme().obtainStyledAttributes(attrs);
	}

	public abstract void setTheme(int resId);

	public final CharSequence getText(int resId) {
		return getResources().getText(resId);
	}

	public final ColorStateList getColorStateList(int id) {
		return getResources().getColorStateList(id, getTheme());
	}

	public final Drawable getDrawable(int resId) {
		return getResources().getDrawable(resId, getTheme());
	}

	public abstract boolean isRestricted();

	/**
	 * Returns {@code true} if the context is a UI context which can access UI components such as
	 * {@link WindowManager}, {@link android.view.LayoutInflater LayoutInflater} or
	 * {@link android.app.WallpaperManager WallpaperManager}. Accessing UI components from non-UI
	 * contexts throws {@link android.os.strictmode.Violation} if
	 * {@link android.os.StrictMode.VmPolicy.Builder#detectIncorrectContextUse()} is enabled.
	 * <p>
	 * Examples of UI contexts are
	 * an {@link android.app.Activity Activity}, a context created from
	 * {@link #createWindowContext(int, Bundle)} or
	 * {@link android.inputmethodservice.InputMethodService InputMethodService}
	 * </p>
	 * <p>
	 * Note that even if it is allowed programmatically, it is not suggested to override this
	 * method to bypass {@link android.os.strictmode.IncorrectContextUseViolation} verification.
	 * </p>
	 *
	 * @see #getDisplay()
	 * @see #getSystemService(String)
	 * @see android.os.StrictMode.VmPolicy.Builder#detectIncorrectContextUse()
	 */
	public boolean isUiContext() {
		throw new RuntimeException("Not implemented. Must override in a subclass.");
	}

	/* ContextImpl.getDatabasePath() */
	public File getDatabasePath(String name) {
		File dir;
		File f;

		if (name.charAt(0) == File.separatorChar) {
			String dirPath = name.substring(0, name.lastIndexOf(File.separatorChar));
			dir = new File(dirPath);
			name = name.substring(name.lastIndexOf(File.separatorChar));
			f = new File(dir, name);

			if (!dir.isDirectory())
				dir.mkdir();
		} else {
			dir = getDatabasesDir();
			f = makeFilename(dir, name);
		}

		return f;
	}

	private File getDatabasesDir() {
		synchronized (mSync) {
			return ensurePrivateDirExists(new File(getDataDirFile(), "databases"));
		}
	}

	@Export
	public void sendBroadcast(Intent intent) {
		if ("org.unifiedpush.android.distributor.REGISTER".equals(intent.getAction())) {
			nativeRegisterUnifiedPush(intent.getStringExtra("token"), intent.getStringExtra("application"));
		}
		for (ReceiverRegistry.Registration registration : receiverRegistrations) {
			if (registration.filter.matchAction(intent.getAction())) {
				/* the Context it was registered with (LoadedApk.ReceiverDispatcher.mContext), not the sender's */
				BroadcastReceiver receiver = registration.receiver;
				Context context = (Context)registration.owner;
				if (registration.scheduler != null)
					registration.scheduler.post(() -> receiver.onReceive(context, intent));
				else
					receiver.onReceive(context, intent);
			}
		}
		// a broadcast for another package goes to that package's receivers (loaded into this process)
		String targetPackage = intent.getComponent() != null ? intent.getComponent().getPackageName() : intent.getPackage();
		ATLLoadedApp target = targetPackage != null ? ATLLoadedAppManager.getAppFromPackageName(targetPackage) : ATLLoadedApp.getPrimaryApplication();
		if (target != null)
			target.receiveBroadcast(this, intent);
	}

	/* a broadcast of the system: to the receivers registered at run time, on the main thread */
	public static void sendBroadcastToReceivers(final Intent intent) {
		for (final ReceiverRegistry.Registration e : receiverRegistrations) {
			if (e.filter.matchAction(intent.getAction())) {
				(e.scheduler != null ? e.scheduler : new Handler(Looper.getMainLooper())).post(new Runnable() {
					public void run() {
						e.receiver.onReceive((Context)e.owner, intent);
					}
				});
			}
		}
	}

	public void sendBroadcast(Intent intent, String receiverPermission) {
		sendBroadcast(intent);
	}

	/* AOSP ContextImpl.sendBroadcast(Intent, String, Bundle): the options (BroadcastOptions) only steer
	 * the system's delivery, which ATL does not have */
	public void sendBroadcast(Intent intent, String receiverPermission, Bundle options) {
		sendBroadcast(intent, receiverPermission);
	}

	/* AOSP ContextImpl.sendBroadcastAsUser: ATL has the one user only */
	public void sendBroadcastAsUser(Intent intent, android.os.UserHandle user) {
		sendBroadcast(intent);
	}

	public void sendBroadcastAsUser(Intent intent, android.os.UserHandle user, String receiverPermission) {
		sendBroadcast(intent, receiverPermission);
	}

	/* AOSP (deprecated): the broadcast stays and goes to every receiver that registers for it later */
	public void sendStickyBroadcast(Intent intent) {
		if (intent.getAction() != null)
			stickyIntents.put(intent.getAction(), new Intent(intent));
		sendBroadcast(intent);
	}

	public void sendStickyBroadcast(Intent intent, Bundle options) {
		sendStickyBroadcast(intent);
	}

	public void sendStickyOrderedBroadcast(Intent intent, BroadcastReceiver resultReceiver, Handler scheduler, int initialCode, String initialData, Bundle initialExtras) {
		sendStickyBroadcast(intent);
	}

	public void removeStickyBroadcast(Intent intent) {
		if (intent.getAction() != null)
			stickyIntents.remove(intent.getAction());
	}

	public void sendStickyBroadcastAsUser(Intent intent, UserHandle user) {
		sendStickyBroadcast(intent);
	}

	public void removeStickyBroadcastAsUser(Intent intent, UserHandle user) {
		removeStickyBroadcast(intent);
	}

	/* the receivers get the broadcast one after the other already, so it is ordered (by registration
	 * instead of priority, nobody aborts it) */
	public void sendOrderedBroadcast(Intent intent, String receiverPermission) {
		sendBroadcast(intent, receiverPermission);
	}

	public abstract boolean stopService(Intent intent);

	public void unbindService(ServiceConnection serviceConnection) {}

	/* AOSP Context.updateServiceGroup() (API 29) groups the processes of bound services for the low memory
	 * killer; ATL has no process importance, so nothing changes */
	public void updateServiceGroup(ServiceConnection conn, int group, int importance) {}

	/* AOSP LoadedApk.forgetReceiverDispatcher(): the receiver object is the key (ArrayMap.get(r), null allowed),
	 * the dispatcher holds every filter the receiver was registered with and ActivityManagerService
	 * .unregisterReceiver() removes the whole ReceiverList; a receiver that is not registered, null included,
	 * is an IllegalArgumentException("Receiver not registered: " + r) that apps catch */
	/* AOSP keys LoadedApk.mReceivers on ContextImpl.getOuterContext(): a ContextWrapper hands registerReceiver()
	 * down to its base, so the key is the innermost wrapper (the Activity, Service or Application that holds
	 * the ContextImpl), not a wrapper an app put around it */
	private static Context receiverOwner(Context context) {
		while (context instanceof ContextWrapper && ((ContextWrapper)context).getBaseContext() instanceof ContextWrapper)
			context = ((ContextWrapper)context).getBaseContext();
		return context;
	}

	public void unregisterReceiver(BroadcastReceiver receiver) {
		boolean registered = receiverRegistrations.removeAll(receiverOwner(this), receiver);
		if (!registered) {
			Slog.w(TAG, "unregisterReceiver: receiver not registered: " + receiver);
			throw new IllegalArgumentException("Receiver not registered: " + receiver);
		}
	}

	public static final int CONTEXT_INCLUDE_CODE = 0x00000001;
	public static final int CONTEXT_IGNORE_SECURITY = 0x00000002;
	public static final int CONTEXT_RESTRICTED = 0x00000004;

	/* AOSP Context.bindService() flags */
	public static final int BIND_AUTO_CREATE = 0x0001;
	public static final int BIND_INCLUDE_CAPABILITIES = 0x000001000;

	/* AOSP Context.BindServiceFlags : the bind flags as a 64 bit value */
	public final static class BindServiceFlags {
		private final long mValue;

		private BindServiceFlags(long value) {
			mValue = value;
		}

		public long getValue() {
			return mValue;
		}

		public static BindServiceFlags of(long value) {
			if ((value & Integer.toUnsignedLong(0x80000000)) != 0) {
				throw new IllegalArgumentException("BIND_EXTERNAL_SERVICE is deprecated. Use BIND_EXTERNAL_SERVICE_LONG instead");
			}
			return new BindServiceFlags(value);
		}
	}

	/* AOSP Context.UpdateBindingParams: one update (unbind or rebind) of updateServiceBindings() */
	public static final class UpdateBindingParams {
		private ServiceConnection mConnection;
		private boolean mUnbind;
		private BindServiceFlags mFlags;

		private UpdateBindingParams(Builder builder) {
			mConnection = builder.mConnection;
			mUnbind = builder.mUnbind;
			mFlags = builder.mFlags;
		}

		public void setUnbind() {
			mUnbind = true;
			mFlags = null;
		}

		public void setRebind(BindServiceFlags flags) {
			mUnbind = false;
			mFlags = java.util.Objects.requireNonNull(flags);
		}

		public ServiceConnection getConnection() {
			return mConnection;
		}

		public boolean isUnbind() {
			return mUnbind;
		}

		public boolean isRebind() {
			return !mUnbind;
		}

		public BindServiceFlags getFlags() {
			return mFlags;
		}

		public static final class Builder {
			private final ServiceConnection mConnection;
			private boolean mUnbind;
			private BindServiceFlags mFlags;

			public Builder(ServiceConnection connection) {
				mConnection = java.util.Objects.requireNonNull(connection);
				mUnbind = true;
				mFlags = null;
			}

			public Builder(ServiceConnection connection, BindServiceFlags flags) {
				mConnection = java.util.Objects.requireNonNull(connection);
				mUnbind = false;
				mFlags = java.util.Objects.requireNonNull(flags);
			}

			public UpdateBindingParams build() {
				return new UpdateBindingParams(this);
			}
		}
	}

	/* AOSP Context.getUpdateableFlags(): BIND_UPDATEABLE_FLAGS = BIND_NOT_FOREGROUND | BIND_ALLOW_OOM_MANAGEMENT
	 * | BIND_WAIVE_PRIORITY | BIND_IMPORTANT | BIND_ADJUST_WITH_ACTIVITY | BIND_NOT_PERCEPTIBLE */
	public BindServiceFlags getUpdateableFlags() {
		return BindServiceFlags.of(0x4 | 0x10 | 0x20 | 0x40 | 0x80 | 0x100);
	}

	/* AOSP ContextImpl.bindService(Intent, ServiceConnection, BindServiceFlags): bindServiceCommon() with the
	 * flags as long; ATL's int flags carry the bits the process scheduler of a desktop does not use anyway */
	public boolean bindService(Intent service, ServiceConnection conn, BindServiceFlags flags) {
		return bindService(service, conn, (int)flags.getValue());
	}

	public boolean bindService(Intent service, BindServiceFlags flags, java.util.concurrent.Executor executor, ServiceConnection conn) {
		return bindService(service, (int)flags.getValue(), executor, conn);
	}

	public boolean bindIsolatedService(Intent service, BindServiceFlags flags, String instanceName, java.util.concurrent.Executor executor, ServiceConnection conn) {
		return bindIsolatedService(service, (int)flags.getValue(), instanceName, executor, conn);
	}

	public boolean bindServiceAsUser(Intent service, ServiceConnection conn, BindServiceFlags flags, UserHandle user) {
		return bindServiceAsUser(service, conn, (int)flags.getValue(), user);
	}

	/* AOSP ContextImpl.updateServiceBindings(): unbinds or rebinds the given connections. A rebind changes
	 * the scheduling flags of the binding (BIND_NOT_FOREGROUND etc.), which ATL does not have */
	public void updateServiceBindings(java.util.List<UpdateBindingParams> params) {
		for (UpdateBindingParams p : params) {
			if (p.isUnbind())
				unbindService(p.getConnection());
		}
	}

	public void rebindService(ServiceConnection conn, BindServiceFlags flags) {
		java.util.Objects.requireNonNull(conn);
		java.util.Objects.requireNonNull(flags);
	}

	/* AOSP ContextImpl.checkUriPermission(Uri, String, String, int, int, int) */
	public int checkUriPermission(Uri uri, String readPermission, String writePermission, int pid, int uid, int modeFlags) {
		if ((modeFlags & Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0) {
			if (readPermission == null || checkPermission(readPermission, pid, uid) == PackageManager.PERMISSION_GRANTED)
				return PackageManager.PERMISSION_GRANTED;
		}
		if ((modeFlags & Intent.FLAG_GRANT_WRITE_URI_PERMISSION) != 0) {
			if (writePermission == null || checkPermission(writePermission, pid, uid) == PackageManager.PERMISSION_GRANTED)
				return PackageManager.PERMISSION_GRANTED;
		}
		return uri != null ? checkUriPermission(uri, pid, uid, modeFlags) : PackageManager.PERMISSION_DENIED;
	}

	/* AOSP ContextImpl.checkCallingUriPermissions(): outside of a binder call there is no calling process, all denied */
	public int[] checkCallingUriPermissions(java.util.List<Uri> uris, int modeFlags) {
		int[] res = new int[uris.size()];
		java.util.Arrays.fill(res, PackageManager.PERMISSION_DENIED);
		return res;
	}

	/* AOSP ContextImpl.checkCallingOrSelfUriPermissions() */
	public int[] checkCallingOrSelfUriPermissions(java.util.List<Uri> uris, int modeFlags) {
		return checkUriPermissions(uris, android.os.Process.myPid(), android.os.Process.myUid(), modeFlags);
	}

	/* AOSP ContextImpl.checkContentUriPermissionFull(): the ActivityManager's grants of pid/uid, as checkUriPermission() */
	public int checkContentUriPermissionFull(Uri uri, int pid, int uid, int modeFlags) {
		return checkUriPermission(uri, pid, uid, modeFlags);
	}

	/* AOSP ContextImpl.enforce() */
	private void atl_enforce(String permission, int resultOfCheck, boolean selfToo, int uid, String message) {
		if (resultOfCheck != PackageManager.PERMISSION_GRANTED) {
			throw new SecurityException((message != null ? (message + ": ") : "") + (selfToo ? "Neither user " + uid + " nor current process has " : "uid " + uid + " does not have ") + permission + ".");
		}
	}

	public void enforcePermission(String permission, int pid, int uid, String message) {
		atl_enforce(permission, checkPermission(permission, pid, uid), false, uid, message);
	}

	public void enforceCallingOrSelfPermission(String permission, String message) {
		atl_enforce(permission, checkCallingOrSelfPermission(permission), true, android.os.Process.myUid(), message);
	}

	/* AOSP ContextImpl.uriModeFlagToString() / enforceForUri() */
	private void atl_enforceForUri(int modeFlags, int resultOfCheck, boolean selfToo, int uid, Uri uri, String message) {
		if (resultOfCheck != PackageManager.PERMISSION_GRANTED) {
			StringBuilder builder = new StringBuilder();
			if ((modeFlags & Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0)
				builder.append("read and ");
			if ((modeFlags & Intent.FLAG_GRANT_WRITE_URI_PERMISSION) != 0)
				builder.append("write and ");
			if ((modeFlags & Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION) != 0)
				builder.append("persistable and ");
			if ((modeFlags & Intent.FLAG_GRANT_PREFIX_URI_PERMISSION) != 0)
				builder.append("prefix and ");
			if (builder.length() <= 5)
				throw new IllegalArgumentException("Unknown permission mode flags: " + modeFlags);
			builder.setLength(builder.length() - 5);
			throw new SecurityException((message != null ? (message + ": ") : "") + (selfToo ? "Neither user " + uid + " nor current process has " : "User " + uid + " does not have ") + builder + " permission on " + uri + ".");
		}
	}

	public void enforceUriPermission(Uri uri, int pid, int uid, int modeFlags, String message) {
		atl_enforceForUri(modeFlags, checkUriPermission(uri, pid, uid, modeFlags), false, uid, uri, message);
	}

	public void enforceCallingUriPermission(Uri uri, int modeFlags, String message) {
		atl_enforceForUri(modeFlags, checkCallingUriPermission(uri, modeFlags), false, android.os.Process.myUid(), uri, message);
	}

	public void enforceCallingOrSelfUriPermission(Uri uri, int modeFlags, String message) {
		atl_enforceForUri(modeFlags, checkCallingOrSelfUriPermission(uri, modeFlags), true, android.os.Process.myUid(), uri, message);
	}

	public void enforceUriPermission(Uri uri, String readPermission, String writePermission, int pid, int uid, int modeFlags, String message) {
		atl_enforceForUri(modeFlags, checkUriPermission(uri, readPermission, writePermission, pid, uid, modeFlags), false, uid, uri, message);
	}

	/* AOSP ContextImpl.getWallpaperManager() */
	private android.app.WallpaperManager atl_wallpaperManager() {
		return android.app.WallpaperManager.getInstance(this);
	}

	/* AOSP ContextImpl.peekWallpaper(): WallpaperManager.peekDrawable(), null when no wallpaper is set (ATL has none) */
	@Deprecated
	public Drawable peekWallpaper() {
		return null;
	}

	@Deprecated
	public void setWallpaper(android.graphics.Bitmap bitmap) throws java.io.IOException {
		atl_wallpaperManager().setBitmap(bitmap);
	}

	@Deprecated
	public void setWallpaper(java.io.InputStream data) throws java.io.IOException {
		atl_wallpaperManager().setStream(data);
	}

	@Deprecated
	public void clearWallpaper() throws java.io.IOException {
		atl_wallpaperManager().clear();
	}

	/* AOSP ContextImpl.moveDatabaseFrom() -> moveFiles(): the database file and its -journal/-wal/-shm companions
	 * (all files of the databases directory that start with the name) move to this context's databases directory */
	public boolean moveDatabaseFrom(Context sourceContext, String name) {
		synchronized (Context.class) {
			File source = sourceContext.getDatabasePath(name);
			File target = getDatabasePath(name);
			File sourceDir = source.getParentFile();
			File targetDir = target.getParentFile();
			File[] files = sourceDir.listFiles((dir, n) -> n.startsWith(source.getName()));
			int res = 0;
			if (files != null) {
				for (File f : files) {
					File to = new File(targetDir, f.getName());
					targetDir.mkdirs();
					if (f.renameTo(to))
						res++;
					else {
						res = -1;
						break;
					}
				}
			}
			return res != -1;
		}
	}

	/* AOSP ContextImpl.startInstrumentation(): ActivityManager.startInstrumentation() finds no <instrumentation>
	 * of the class on a device that has none and returns false */
	public boolean startInstrumentation(ComponentName className, String profileFile, Bundle arguments) {
		return false;
	}

	/* AOSP ContextImpl.startIntentSender(): sends the IntentSender (the activity it stands for is started) */
	public void startIntentSender(IntentSender intent, Intent fillInIntent, int flagsMask, int flagsValues, int extraFlags) throws IntentSender.SendIntentException {
		startIntentSender(intent, fillInIntent, flagsMask, flagsValues, extraFlags, null);
	}

	public void startIntentSender(IntentSender intent, Intent fillInIntent, int flagsMask, int flagsValues, int extraFlags, Bundle options) throws IntentSender.SendIntentException {
		if (intent == null)
			throw new IntentSender.SendIntentException("null IntentSender");
		intent.sendIntent(this, 0, fillInIntent, null, null);
	}

	/* AOSP ContextImpl.sendBroadcastWithMultiplePermissions(): the permissions restrict the receivers, ATL has no other app */
	public void sendBroadcastWithMultiplePermissions(Intent intent, String[] receiverPermissions) {
		sendBroadcast(intent);
	}

	/* AOSP ContextImpl.sendOrderedBroadcastAsUser(): ATL has the one user only */
	public void sendOrderedBroadcastAsUser(Intent intent, UserHandle user, String receiverPermission, BroadcastReceiver resultReceiver, Handler scheduler, int initialCode, String initialData, Bundle initialExtras) {
		sendOrderedBroadcast(intent, receiverPermission, resultReceiver, scheduler, initialCode, initialData, initialExtras);
	}

	public void sendStickyOrderedBroadcastAsUser(Intent intent, UserHandle user, BroadcastReceiver resultReceiver, Handler scheduler, int initialCode, String initialData, Bundle initialExtras) {
		sendStickyOrderedBroadcast(intent, resultReceiver, scheduler, initialCode, initialData, initialExtras);
	}

	/* AOSP Context.getParams(): null unless the context was created by createContext(ContextParams) */
	public ContextParams getParams() {
		return null;
	}

	public abstract Context createPackageContext(String packageName, int flags);

	public void grantUriPermission(String dummy, Uri dummy2, int dummy3) {
		System.out.println("grantUriPermission(" + dummy + ", " + dummy2 + ", " + dummy3 + ") called");
	}

	public static final int MODE_ENABLE_WRITE_AHEAD_LOGGING = 0x0008;
	public static final int MODE_NO_LOCALIZED_COLLATORS = 0x0010;

	public SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory) {
		return openOrCreateDatabase(name, mode, factory, null);
	}

	public SQLiteDatabase openOrCreateDatabase(String filename, int mode, SQLiteDatabase.CursorFactory factory, DatabaseErrorHandler errorHandler) {
		int flags = SQLiteDatabase.CREATE_IF_NECESSARY;
		if ((mode & MODE_ENABLE_WRITE_AHEAD_LOGGING) != 0) {
			flags |= SQLiteDatabase.ENABLE_WRITE_AHEAD_LOGGING;
		}
		if ((mode & MODE_NO_LOCALIZED_COLLATORS) != 0) {
			flags |= SQLiteDatabase.NO_LOCALIZED_COLLATORS;
		}
		SQLiteDatabase db = SQLiteDatabase.openDatabase(filename, factory, flags, errorHandler);
		return db;
	}

	/* ContextImpl.deleteDatabase(): the journal and the other files of the database go with it */
	public boolean deleteDatabase(String name) {
		try {
			File f = getDatabasePath(name);
			return SQLiteDatabase.deleteDatabase(f);
		} catch (Exception e) {
		}
		return false;
	}

	public abstract Context createConfigurationContext(Configuration configuration);

	/* AOSP ContextImpl.sendOrderedBroadcast(Intent, String, BroadcastReceiver, Handler, int, String, Bundle): the
	 * receivers get the broadcast, then resultReceiver gets it last with the result, on scheduler or, when that is
	 * null, on the main thread. ATL's receivers do not pass a result on, so the result receiver sees the initial
	 * code, data and extras. */
	public void sendOrderedBroadcast(Intent intent, String receiverPermission, BroadcastReceiver resultReceiver, Handler scheduler, int initialCode, String initialData, Bundle initialExtras) {
		sendOrderedBroadcast(intent, receiverPermission);
		if (resultReceiver == null)
			return;
		Context context = this;
		(scheduler != null ? scheduler : new Handler(Looper.getMainLooper())).post(() -> {
			resultReceiver.setResult(initialCode, initialData, initialExtras);
			resultReceiver.onReceive(context, intent);
		});
	}

	/* AOSP Context.sendOrderedBroadcast(Intent, String, Bundle) (API 31): the options (BroadcastOptions) only steer
	 * the system's delivery */
	public void sendOrderedBroadcast(Intent intent, String receiverPermission, Bundle options) {
		sendOrderedBroadcast(intent, receiverPermission);
	}

	/* AOSP Context.sendOrderedBroadcast(Intent, String, Bundle, BroadcastReceiver, Handler, int, String, Bundle)
	 * (API 31): as the variant without options */
	public void sendOrderedBroadcast(Intent intent, String receiverPermission, Bundle options, BroadcastReceiver resultReceiver, Handler scheduler, int initialCode, String initialData, Bundle initialExtras) {
		sendOrderedBroadcast(intent, receiverPermission, resultReceiver, scheduler, initialCode, initialData, initialExtras);
	}

	/* AOSP Context.sendOrderedBroadcast(Intent, String, String, BroadcastReceiver, Handler, int, String, Bundle)
	 * (API 30): receiverAppOp only limits the receivers to those allowed the app op; ATL has no app ops */
	public void sendOrderedBroadcast(Intent intent, String receiverPermission, String receiverAppOp, BroadcastReceiver resultReceiver, Handler scheduler, int initialCode, String initialData, Bundle initialExtras) {
		sendOrderedBroadcast(intent, receiverPermission, resultReceiver, scheduler, initialCode, initialData, initialExtras);
	}

	/* AOSP Context.sendOrderedBroadcast(Intent, int, String, String, BroadcastReceiver, Handler, String, Bundle,
	 * Bundle) (API 30) */
	public void sendOrderedBroadcast(Intent intent, int initialCode, String receiverPermission, String receiverAppOp, BroadcastReceiver resultReceiver, Handler scheduler, String initialData, Bundle initialExtras, Bundle options) {
		sendOrderedBroadcast(intent, receiverPermission, resultReceiver, scheduler, initialCode, initialData, initialExtras);
	}

	public abstract Context createDisplayContext(Display display);

	/* AOSP Context.getDisplay() (API 30) */
	public Display getDisplay() {
		throw new RuntimeException("Not implemented. Must override in a subclass.");
	}

	/**
	 * Creates a Context for a non-activity window.
	 */
	public Context createWindowContext(int type, Bundle options) {
		throw new RuntimeException("Not implemented. Must override in a subclass.");
	}

	/**
	 * Creates a Context for a non-activity window on the given Display.
	 */
	public Context createWindowContext(Display display, int type, Bundle options) {
		throw new RuntimeException("Not implemented. Must override in a subclass.");
	}

	/* ATL has a single Context; an attribution tag does not change it */
	public Context createAttributionContext(String attributionTag) {
		return this;
	}

	/* ATL has a single Context; attribution parameters do not change it (like createAttributionContext) */
	public Context createContext(ContextParams contextParams) {
		return this;
	}

	/* ATL has a single (default) device; a device context is the same Context */
	public Context createDeviceContext(int deviceId) {
		return this;
	}

	public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter, String broadcastPermission, Handler scheduler) {
		return registerReceiverInternal(receiver, filter, scheduler);
	}

	/* there is only one app, so the export flags (RECEIVER_EXPORTED etc.) make no difference */
	public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter, int flags) {
		return registerReceiver(receiver, filter);
	}

	public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter, String broadcastPermission, Handler scheduler, int flags) {
		return registerReceiver(receiver, filter, broadcastPermission, scheduler);
	}

	public String[] fileList() {
		return listOrEmpty(getFilesDir());
	}

	/* FileUtils.listOrEmpty() */
	private static String[] listOrEmpty(File dir) {
		String[] list = dir != null ? dir.list() : null;
		return list != null ? list : new String[0];
	}

	public void revokeUriPermission(Uri uri, int mode) {
		System.out.println("revokeUriPermission(" + uri + ", " + mode + ") called");
	}

	/* AOSP ContextImpl.revokeUriPermission(String, Uri, int): asks ActivityManager to revoke what the package was
	 * granted; ATL grants no URI permissions, so like the other overload there is nothing to revoke */
	public void revokeUriPermission(String toPackage, Uri uri, int modeFlags) {
		System.out.println("revokeUriPermission(" + toPackage + ", " + uri + ", " + modeFlags + ") called");
	}

	/* AOSP ContextImpl.createContextForSplit(): an app that does not request isolated split loading has all its
	 * splits loaded, so the context itself serves (ATL installs no isolated splits) */
	public Context createContextForSplit(String splitName) throws android.content.pm.PackageManager.NameNotFoundException {
		return this;
	}

	public String getAttributionTag() {
		return null;
	}
	public abstract boolean isDeviceProtectedStorage();

	public Drawable getWallpaper() {
		return null;
	}

	/* AOSP ContextImpl: WallpaperManager.getDesiredMinimumWidth() asks the wallpaper service, which ATL does not have;
	 * "if the returned value is <= 0, the caller should use the width of the default display" */
	@Deprecated
	public int getWallpaperDesiredMinimumWidth() {
		return 0;
	}

	@Deprecated
	public int getWallpaperDesiredMinimumHeight() {
		return 0;
	}

	/* AOSP ContextImpl: PermissionControllerManager.revokeSelfPermissionsOnKill(); nothing is granted by a permission
	 * controller in ATL ("a permission that is not currently granted is ignored") */
	public void revokeSelfPermissionsOnKill(java.util.Collection<String> permissions) {
	}

	public void revokeSelfPermissionOnKill(String permName) {
		revokeSelfPermissionsOnKill(java.util.Collections.singletonList(permName));
	}

	public String[] databaseList() {
		return listOrEmpty(getDatabasesDir());
	}

	public abstract Context createDeviceProtectedStorageContext();

	/* AOSP Context.createCredentialProtectedStorageContext() (@SystemApi) */
	public abstract Context createCredentialProtectedStorageContext();

	public AttributionSource getAttributionSource() {
		return null;
	}

	public boolean moveSharedPreferencesFrom(Context sourceContext, String name) {
		File sourceFile = sourceContext.getSharedPrefsFile(name);
		if (!sourceFile.exists()) {
			return true;
		}
		deleteSharedPreferences(name);
		return sourceFile.renameTo(getSharedPrefsFile(name));
	}

	public boolean deleteSharedPreferences(String name) {
		File prefsFile = getSharedPrefsFile(name);
		prefsFile.delete();
		synchronized (sharedPrefs) {
			sharedPrefs.remove(prefsFile.getPath());
		}
		return true;
	}

	public String getPackageResourcePath() {
		return get_atl_loaded_app().pkg.applicationInfo.sourceDir;
	}

	public abstract int getThemeResId();

	public abstract ATLLoadedApp get_atl_loaded_app();
}
