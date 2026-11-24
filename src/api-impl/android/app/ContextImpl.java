package android.app;

import android.accounts.AccountManager;
import android.annotation.Nullable;
import android.annotation.UnsupportedAppUsage;
import android.app.SearchManager;
import android.app.job.JobScheduler;
import android.atl.ATLLoadedApp;
import android.atl.ATLLoadedAppManager;
import android.atl.ATLProcess;
import android.bluetooth.BluetoothManager;
import android.content.*;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageParser;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraManager;
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
import android.os.*;
import android.os.storage.StorageManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.util.Slog;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.view.WindowManagerImpl;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.CaptioningManager;
import android.view.inputmethod.InputMethodManager;
import com.android.internal.policy.PhoneLayoutInflater;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public final class ContextImpl extends Context {
	private static final int ATL_FLAG_NO_CODE = 0x00000001;
	private static final int ATL_FLAG_RESTRICTED = 0x00000002;
	private static final int ATL_FLAG_PROTECTED_STORAGE = 0x00000004;
	private final static String TAG = "ContextImpl";
	private final Resources resources;
	private final ATLLoadedApp atl_loaded_app;
	/* LoadedApk.mServices: the service connections bound with this context, and the app of their service */
	private final java.util.HashMap<ServiceConnection, ATLLoadedApp> bound_services = new java.util.HashMap<>();
	/**
	 * While not being part of the official Android API, some applications use it to reset/reload the theme in the context.
	 */
	@UnsupportedAppUsage
	private Resources.Theme mTheme;
	/**
	 * While not being part of the official Android API, some application use it to get the theme resource ID.
	 */
	@UnsupportedAppUsage
	private int mThemeResource;

	/* like AOSP: the services of this context that SystemServiceRegistry's CachedServiceFetcher keeps */
	static final int STATE_UNINITIALIZED = 0;
	static final int STATE_INITIALIZING = 1;
	static final int STATE_READY = 2;
	static final int STATE_NOT_FOUND = 3;
	final Object[] mServiceCache = SystemServiceRegistry.createServiceCache();
	final int[] mServiceInitializationStateArray = new int[mServiceCache.length];

	private final int atl_flags;
	/* like AOSP's mContextType: activity contexts (and their configuration contexts) are UI contexts */
	private boolean mIsUiContext;

	public ContextImpl(Resources resources, ATLLoadedApp loadedApp, Resources.Theme theme) {
		this(resources, loadedApp, 0, 0);
		getTheme().setTo(theme);
	}

	private ContextImpl(Resources resources, ATLLoadedApp loadedApp, Resources.Theme theme, int flags) {
		this(resources, loadedApp, 0, flags);
		getTheme().setTo(theme);
	}

	public ContextImpl(Resources resources, ATLLoadedApp loadedApp, int themeResource) {
		this(resources, loadedApp, themeResource, 0);
	}

	private ContextImpl(Resources resources, ATLLoadedApp loadedApp, int themeResource, int flags) {
		this.resources = resources;
		this.atl_loaded_app = loadedApp;
		mThemeResource = themeResource;
		this.atl_flags = flags;
	}

	/* like AOSP's ContextImpl.createActivityContext(): the base context of an activity, a UI context */
	public static ContextImpl createActivityContext(Resources resources, ATLLoadedApp loadedApp, int themeResource) {
		ContextImpl context = new ContextImpl(resources, loadedApp, themeResource);
		context.mIsUiContext = true;
		return context;
	}

	@Nullable
	private ATLLoadedApp atl_get_intent_target(Intent intent) {
		ATLLoadedApp targetApp;
		String targetPackage = intent.getComponent() != null ? intent.getComponent().getPackageName() : null;
		if (targetPackage != null || (targetPackage = intent.getPackage()) != null) {
			if (targetPackage.equals(this.atl_loaded_app.pkg.packageName)) {
				targetApp = this.atl_loaded_app;
			} else {
				targetApp = ATLLoadedAppManager.getAppFromPackageName(targetPackage);
			}
		} else {
			targetApp = this.atl_loaded_app;
		}
		return targetApp;
	}

	/* Cloud Messaging goes to Play Services (microG) when it is installed, it keeps its own
	 * connection; otherwise to the gcm_service over DBus */
	private static boolean atl_use_gcm_service(Intent intent) {
		return intent.getAction() != null && intent.getAction().startsWith("com.google.android.c2dm") &&
		    !ATLLoadedAppManager.isInstalled("com.google.android.gms");
	}

	@Override
	public ApplicationInfo getApplicationInfo() {
		return atl_loaded_app.pkg.applicationInfo;
	}

	@Override
	public void setTheme(int resId) {
		mThemeResource = resId;
		if (mTheme != null) {
			mTheme.applyStyle(resId, true);
		}
	}

	@Override
	public boolean isRestricted() {
		return (this.atl_flags & ATL_FLAG_RESTRICTED) != 0;
	}

	@Override
	public boolean stopService(Intent intent) {
		ATLLoadedApp targetApp = this.atl_get_intent_target(intent);
		if (targetApp == null) {
			return false;
		}
		String processName = otherServiceProcess(targetApp, intent);
		if (processName != null)
			return ATLProcess.stopService(intent, processName);
		return targetApp.stopService(intent);
	}

	/* the process of the app a service with its own android:process runs in, null if it runs in this one; an
	 * isolated service always runs in a process of its own */
	private static String otherServiceProcess(ATLLoadedApp targetApp, Intent intent) {
		if (targetApp != ATLLoadedApp.getPrimaryApplication() || intent.getComponent() == null)
			return null;
		String className = intent.getComponent().getClassName();
		String processName = targetApp.serviceProcessName(className);
		if (targetApp.isIsolatedService(className))
			return processName != null ? processName : ATLProcess.mainProcessName();
		return processName != null && !processName.equals(ATLProcess.myProcessName()) ? processName : null;
	}

	/* ContextImpl.bindIsolatedService(): the instance of an isolated service runs in a process of its own */
	@Override
	public boolean atl_bindService(Intent intent, ServiceConnection serviceConnection, int flags, String instanceName) {
		if (instanceName == null)
			return bindService(intent, serviceConnection, flags);
		ATLLoadedApp targetApp = this.atl_get_intent_target(intent);
		if (targetApp == null || intent.getComponent() == null)
			return false;
		if (!targetApp.isIsolatedService(intent.getComponent().getClassName()))
			throw new IllegalArgumentException("Can't use instance name '" + instanceName + "' with non-isolated service '" + intent.getComponent().getClassName() + "'");
		return ATLProcess.bindService(intent, serviceConnection, otherServiceProcess(targetApp, intent), instanceName);
	}

	@Override
	public Resources.Theme getTheme() {
		if (mTheme == null) {
			mTheme = resources.newTheme();
			if (mThemeResource != 0) {
				mTheme.applyStyle(mThemeResource, true);
			}
		}
		return mTheme;
	}

	@Override
	public Object getSystemService(String name) {
		return SystemServiceRegistry.getSystemService(this, name);
	}

	@Override
	public String getSystemServiceName(Class<?> serviceClass) {
		return SystemServiceRegistry.getSystemServiceName(serviceClass);
	}

	@Override
	public Resources getResources() {
		return resources;
	}

	@Override
	public ClassLoader getClassLoader() {
		if ((this.atl_flags & ATL_FLAG_NO_CODE) != 0) {
			return null;
		}
		return this.atl_loaded_app.class_loader;
	}

	public ComponentName startService(Intent intent) {
		// One way Messenger interface for GCM. Request per intent and response per Messenger.
		// Wrap the response part in a BroadcastReceiver for compatibility with our gcm_service
		if (atl_use_gcm_service(intent) && "com.google.android.c2dm.intent.REGISTER".equals(intent.getAction()) && intent.getParcelableExtra("google.messenger") instanceof Messenger) {
			final Messenger messenger = (Messenger)intent.getParcelableExtra("google.messenger");
			this.registerReceiver(new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent resultIntent) {
					try {
						messenger.send(Message.obtain(null, 0, resultIntent));
					} catch (RemoteException e) {
						e.printStackTrace();
					}
				}
			}, new IntentFilter("com.google.android.c2dm.intent.REGISTRATION"));
		}
		ATLLoadedApp targetApp = this.atl_get_intent_target(intent);
		// Without Play Services, Cloud Messaging (c2dm) intents are started using DBus. Delivering them to the in process gstub is pointless
		if (targetApp == null || atl_use_gcm_service(intent)) {
			// External package. Try to start using DBus Action
			if (intent.getPackage() != null)
				nativeStartExternalService(intent);
			return null;
		}
		String processName = otherServiceProcess(targetApp, intent);
		if (processName != null)
			return ATLProcess.startService(intent, processName);
		return targetApp.startOrBindService(intent, null);
	}

	/* AOSP: each context has its ApplicationPackageManager, whose getPackageInstaller() passes the context's package
	 * as installer. ATL shares one package manager; a context of another loaded package (Play Services, e.g.
	 * microG Companion installing for a store) gets its own, so that its sessions have that package as installer */
	@Override
	public android.content.pm.PackageManager getPackageManager() {
		if (atl_loaded_app == null || atl_loaded_app == ATLLoadedApp.getPrimaryApplication()
		    || atl_loaded_app == ATLLoadedApp.getSystemApplication())
			return super.getPackageManager();
		return atl_loaded_app.getPackageManager();
	}

	@Override
	public boolean bindService(Intent intent, ServiceConnection serviceConnection, int flags) {
		// Two way Messenger interface for GCM. Request and response are send over Messenger.
		// Wrap it on top of the old intent interface for compatibility with our gcm_service
		if (atl_use_gcm_service(intent) && "com.google.android.c2dm.intent.REGISTER".equals(intent.getAction())) {
			Messenger requestMessenger = new Messenger(new Handler(Looper.getMainLooper()) {
				@Override
				public void handleMessage(Message requestMessage) {
					int request_id = requestMessage.arg1;
					Messenger responseMessenger = requestMessage.replyTo;
					registerReceiver(new BroadcastReceiver() {
						@Override
						public void onReceive(Context context, Intent responseIntent) {
							try {
								// map response Intent to Message
								Message responseMessage = Message.obtain(null, 0, request_id, 0);
								Bundle data = new Bundle();
								data.putBundle("data", responseIntent.getExtras());
								responseMessage.setData(data);
								responseMessenger.send(responseMessage);
							} catch (RemoteException e) {
								e.printStackTrace();
							}
						}
					}, new IntentFilter("com.google.android.c2dm.intent.REGISTRATION"));
					// map request Message to Intent
					Intent requestIntent = new Intent("com.google.android.c2dm.intent.REGISTER");
					requestIntent.setPackage("com.google.android.gms");
					requestIntent.putExtras(requestMessage.getData().getBundle("data"));
					nativeStartExternalService(requestIntent);
				}
			});
			serviceConnection.onServiceConnected(intent.getComponent(), requestMessenger.getBinder());
			return true;
		}
		ATLLoadedApp targetApp = this.atl_get_intent_target(intent);
		if (targetApp == null) {
			return false;
		}
		String processName = otherServiceProcess(targetApp, intent);
		if (processName != null)
			return ATLProcess.bindService(intent, serviceConnection, processName, null);
		if (targetApp.startOrBindService(intent, serviceConnection, flags, atl_loaded_app) == null)
			return false;
		synchronized (bound_services) {
			bound_services.put(serviceConnection, targetApp);
		}
		return true;
	}

	/* LoadedApk.forgetServiceDispatcher(): a connection this context did not bind (or a Cloud Messaging one,
	 * which has no service behind it) is ignored instead of AOSP's IllegalArgumentException */
	@Override
	public void unbindService(ServiceConnection serviceConnection) {
		ServiceConnection dispatcher = Context.atl_takeExecutorDispatcher(serviceConnection);
		if (dispatcher != null)
			serviceConnection = dispatcher;
		if (ATLProcess.unbindService(serviceConnection))
			return;
		ATLLoadedApp targetApp;
		synchronized (bound_services) {
			targetApp = bound_services.remove(serviceConnection);
		}
		if (targetApp != null)
			targetApp.unbindService(serviceConnection);
	}

	/* AOSP resolves an implicit intent with data against all packages with MATCH_DEFAULT_ONLY: the app's own
	 * activity takes it if one of its filters in CATEGORY_DEFAULT matches. A web link only if the filter is
	 * verified (android:autoVerify, Android 12) or the intent names the package (a link the desktop hands to
	 * this app); otherwise the desktop opens it. Files (content:, file:) stay with the desktop, where Android would
	 * offer a chooser. */
	private String ownActivityForData(Intent intent) {
		Uri data = intent.getData();
		String scheme = data.getScheme();
		if (scheme == null || "content".equals(scheme) || "file".equals(scheme))
			return null;
		boolean web = "http".equals(scheme) || "https".equals(scheme);
		boolean forUs = this.atl_loaded_app.pkg.packageName.equals(intent.getPackage());
		String className = null;
		int best = 0;
		for (PackageParser.Activity activity : this.atl_loaded_app.pkg.activities) {
			if (!activity.info.enabled)
				continue;
			for (PackageParser.IntentInfo filter : activity.intents) {
				if (!filter.hasCategory(Intent.CATEGORY_DEFAULT) || (web && !forUs && !filter.getAutoVerify()))
					continue;
				int score = filter.match(intent.getAction(), intent.getType(), scheme, data, intent.getCategories(), TAG);
				if (score > best) {
					className = activity.info.targetActivity != null ? activity.info.targetActivity : activity.className;
					best = score;
				}
			}
		}
		return className;
	}

	/* ACTION_SEND / SEND_MULTIPLE without a target (AOSP: the chooser). A link goes to the desktop's own choice of
	 * applications (OpenURI portal, «ask»); a file or text, or without the portal, gets ATL's share sheet (open
	 * with, copy, mail, save). SEND_MULTIPLE hands over its first file. */
	private void share(Intent intent, CharSequence chooser_title) {
		Slog.i(TAG, "share: " + intent);
		final String text = intent.getStringExtra(Intent.EXTRA_TEXT);
		Uri stream = null;
		Object extra = intent.getExtras() != null ? intent.getExtras().get(Intent.EXTRA_STREAM) : null;
		if (extra instanceof Uri)
			stream = (Uri)extra;
		else if (extra instanceof java.util.List && !((java.util.List<?>)extra).isEmpty() && ((java.util.List<?>)extra).get(0) instanceof Uri)
			stream = (Uri)((java.util.List<?>)extra).get(0);
		ParcelFileDescriptor fd = null;
		if (stream != null) {
			try {
				fd = getContentResolver().openFileDescriptor(stream, "r");
			} catch (FileNotFoundException | SecurityException e) {
				e.printStackTrace();
			}
		}
		final String link = fd == null && text != null && isLink(text.trim()) ? text.trim() : null;
		final ParcelFileDescriptor fd_final = fd;
		final String[] labels = {chooser_title != null ? chooser_title.toString() : getString(com.android.internal.R.string.share),
		                         getString(com.android.internal.R.string.copy),
		                         getString(com.android.internal.R.string.autofill_save_yes),
		                         getString(com.android.internal.R.string.whichViewApplication),
		                         getString(com.android.internal.R.string.atl_share_email)}; // the Email portal's target in a Flatpak
		/* the other installed apps that take this (the starter writes the types each one takes shared next to
		 * its APK); a pick starts the app with the text as a data: URI or the file as a file: URI (--uri) */
		final Uri stream_final = stream;
		final String type = intent.getType() != null ? intent.getType() : stream != null ? "application/octet-stream" : "text/plain";
		final java.util.List<String[]> apps = (stream != null && fd != null) || (stream == null && text != null)
		    ? ATLLoadedAppManager.shareTargets(type, getPackageName()) : new java.util.ArrayList<String[]>();
		final String[] appLabels = new String[apps.size()];
		final String[] appIcons = new String[apps.size()];
		Runnable[] appTargets = new Runnable[apps.size()];
		for (int i = 0; i < apps.size(); i++) {
			final String pkg = apps.get(i)[0];
			appLabels[i] = apps.get(i)[1];
			appIcons[i] = apps.get(i)[2];
			appTargets[i] = new Runnable() {
				@Override
				public void run() {
					String uri = stream_final != null ? sharedFileUri(stream_final) : "data:text/plain;charset=utf-8," + Uri.encode(text);
					if (uri != null)
						ATLLoadedAppManager.startInstalledApp(getPackageName(), pkg, uri);
				}
			};
		}
		Context.shareAppTargets = appTargets;
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				/* a link: the desktop's choice of applications; a file: the sheet, with «Open with» (the desktop's
				 * choice) and «Save» (the file chooser portal writes where the user chooses, e.g. an export) */
				boolean asked = link != null && nativeOpenWith(-1, link);
				if (!asked)
					nativeShareFile(text, fd_final != null ? fd_final.getFd() : -1, labels, appLabels, appIcons);
				if (fd_final != null) {
					try {
						fd_final.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		};
		if (Looper.myLooper() == Looper.getMainLooper())
			runnable.run();
		else
			new Handler(Looper.getMainLooper()).post(runnable);
	}

	/* a file: URI another app can open: the file itself when the URI names one (file:, ATL's raw: documents), else
	 * a copy in the cache, which every app of the starter reads (one data folder); null if it cannot be read */
	private String sharedFileUri(Uri stream) {
		if ("file".equals(stream.getScheme()))
			return stream.toString();
		java.util.List<String> segments = stream.getPathSegments();
		if (segments.size() == 2 && segments.get(1).startsWith("raw:/"))
			return Uri.fromFile(new java.io.File(segments.get(1).substring(4))).toString();
		String name = stream.getLastPathSegment();
		try (android.database.Cursor c = getContentResolver().query(stream, new String[] {"_display_name"}, null, null, null)) {
			if (c != null && c.moveToFirst() && c.getString(0) != null)
				name = c.getString(0);
		} catch (RuntimeException e) {
			Slog.w(TAG, "share: no name for " + stream + ": " + e);
		}
		name = new java.io.File(name != null && !name.isEmpty() ? name : "shared").getName();
		java.io.File dir = new java.io.File(getCacheDir(), "shared");
		dir.mkdirs();
		java.io.File copy = new java.io.File(dir, name);
		try (java.io.InputStream in = getContentResolver().openInputStream(stream);
		     java.io.OutputStream out = new java.io.FileOutputStream(copy)) {
			byte[] buf = new byte[65536];
			for (int n; (n = in.read(buf)) > 0;)
				out.write(buf, 0, n);
		} catch (IOException | RuntimeException e) {
			Slog.w(TAG, "share: cannot copy " + stream + ": " + e);
			return null;
		}
		return Uri.fromFile(copy).toString();
	}

	/* one URI with a scheme and nothing else (what NewPipe or a browser shares), not prose with a link in it */
	private static boolean isLink(String text) {
		if (text.isEmpty() || text.length() > 8192 || text.matches("(?s).*\\s.*"))
			return false;
		Uri uri = Uri.parse(text);
		return uri.getScheme() != null && uri.getScheme().matches("[a-zA-Z][a-zA-Z0-9+.-]*")
		    && !"file".equalsIgnoreCase(uri.getScheme()) && !"content".equalsIgnoreCase(uri.getScheme());
	}

	@Override
	public void startActivity(Intent intent) {
		Slog.i(TAG, "startActivity(" + intent + ") called");
		/* AOSP: the system's PackageInstallerActivity confirms a PackageInstaller session */
		if (android.content.pm.PackageInstaller.handleConfirmIntent(intent))
			return;
		CharSequence chooser_title = null;
		if (intent.getAction() != null && intent.getAction().equals("android.intent.action.CHOOSER")) {
			chooser_title = intent.getCharSequenceExtra(Intent.EXTRA_TITLE);
			intent = (Intent)intent.getExtras().get("android.intent.extra.INTENT");
		}
		if (android.content.pm.PackageInstaller.handlePackageIntent(this, intent, null))
			return;

		/* AOSP ActivityStarter/ActivityTaskManagerService: ACTION_CALL is for apps with CALL_PHONE (Telecom's
		 * UserCallActivity then places the call; ATL hands the tel: link to the desktop's dialer below) */
		if (Intent.ACTION_CALL.equals(intent.getAction()) && checkSelfPermission(android.Manifest.permission.CALL_PHONE) != android.content.pm.PackageManager.PERMISSION_GRANTED)
			throw new SecurityException("Permission Denial: starting " + intent + " from " + getPackageName()
			                            + " requires android.permission.CALL_PHONE");
		String className = null;
		if (intent.getComponent() != null) {
			if (ATLLoadedAppManager.startInstalledApp(this.atl_loaded_app.pkg.packageName, intent.getComponent().getPackageName()))
				return;
			className = intent.getComponent().getClassName();
		} else {
			if (Intent.ACTION_SEND.equals(intent.getAction()) || Intent.ACTION_SEND_MULTIPLE.equals(intent.getAction())) {
				share(intent, chooser_title);
				return;
			} else if (intent.getData() != null && ("file".equals(intent.getData().getScheme()) || "data".equals(intent.getData().getScheme()))
			           && this.atl_loaded_app.pkg.packageName.equals(intent.getPackage())
			           && Activity.desktopFileIntent(intent.getData()) != null) {
				/* a file the desktop hands to the running app (main.c: --uri while it runs) */
				intent = Activity.desktopFileIntent(intent.getData());
				className = intent.getComponent().getClassName();
				Slog.i(TAG, "startActivity: " + className + " takes the file from the desktop: " + intent);
			} else if (intent.getData() != null && (className = ownActivityForData(intent)) != null) {
				Slog.i(TAG, "startActivity: " + className + " of the app takes " + intent.getData());
			} else if (intent.getData() != null) {
				Slog.i(TAG, "starting extern activity with intent: " + intent);
				if (intent.getData().getScheme().equals("content")) {
					try (ParcelFileDescriptor fd = getContentResolver().openFileDescriptor(intent.getData(), "r")) {
						if (fd != null) {
							nativeOpenFile(fd.getFd());
							return;
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				Activity.nativeOpenURI(desktopUri(intent.getData()));
				return;
			}
			for (PackageParser.Activity activity : this.atl_loaded_app.pkg.activities) {
				if (className != null)
					break;
				for (PackageParser.IntentInfo intentInfo : activity.intents) {
					if (intentInfo.matchAction(intent.getAction())) {
						className = activity.className;
						break;
					}
				}
			}
		}
		if (className == null) {
			Slog.w(TAG, "startActivity: intent could not be handled.");
			return;
		}
		String process = this.atl_loaded_app == ATLLoadedApp.getPrimaryApplication() ? ATLProcess.activityProcess(className) : null;
		if (process != null) {
			Activity.startInOtherProcess(null, new Intent(intent).setComponent(new ComponentName(this.atl_loaded_app.pkg.packageName, className)), -1, process);
			return;
		}
		final String className_ = className;
		final Intent intent_ = intent;
		/* like AOSP (ActivityStarter), the launch modes singleTask and singleInstance reuse the activity's instance
		 * in the back stack as FLAG_ACTIVITY_CLEAR_TOP does: the ones above it finish and it gets onNewIntent() */
		int launchMode = ActivityInfo.LAUNCH_MULTIPLE;
		String activityClass = className; // an <activity-alias> runs its target activity (ActivityInfo.targetActivity)
		for (PackageParser.Activity activity : this.atl_loaded_app.pkg.activities) {
			if (activity.className.equals(className)) {
				launchMode = activity.info.launchMode;
				if (activity.info.targetActivity != null)
					activityClass = activity.info.targetActivity;
			}
		}
		final String activityClass_ = activityClass;
		/* FLAG_ACTIVITY_NEW_TASK with FLAG_ACTIVITY_CLEAR_TASK: the app's one task is cleared, the activity starts
		 * anew as its root; the ones that were there finish after the start and what it started itself
		 * (Activity.nativeClearTaskMark()) */
		final boolean clear_task = (intent.getFlags() & (Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK))
		    == (Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		final boolean reuse = !clear_task && ((intent.getFlags() & Intent.FLAG_ACTIVITY_CLEAR_TOP) != 0
		    || launchMode == ActivityInfo.LAUNCH_SINGLE_TASK || launchMode == ActivityInfo.LAUNCH_SINGLE_INSTANCE);
		new Handler(Looper.getMainLooper()).post(new Runnable() {
			@Override
			public void run() {
				try {
					if (clear_task)
						Activity.nativeClearTaskMark();
					if (reuse) {
						boolean found = Activity.nativeResumeActivity(
						    ContextImpl.this.atl_loaded_app.loadClass(activityClass_).asSubclass(Activity.class),
						    intent_);
						if (found)
							return;
					}
					/* like AOSP, the activity that is left is paused before the new one is created */
					Activity.nativePauseCurrentActivity(intent_);
					Activity activity = Activity.internalCreateActivity(
					    className_, ContextImpl.this.atl_loaded_app.getNativeWindow(), intent_);
					Activity.nativeStartActivity(activity);
					if (clear_task) {
						new Handler(Looper.getMainLooper()).post(new Runnable() {
							@Override
							public void run() {
								Activity.nativeClearTaskFinish(true);
							}
						});
					}
				} catch (Exception e) {
					e.printStackTrace();
					if (clear_task)
						Activity.nativeClearTaskFinish(false); // nothing started: the task stays
					Activity.nativeResumeCurrentActivity();
				}
			}
		});
	}

	@Override
	public Context createPackageContext(String packageName, int flags) {
		if (packageName.equals(atl_loaded_app.pkg.packageName)) {
			return this;
		}
		ATLLoadedApp primaryApplication = ATLLoadedApp.getPrimaryApplication();
		if (primaryApplication.pkg.packageName.equals(packageName)) {
			return primaryApplication.getApplication();
		}
		if (packageName.equals("android")) {
			// Shortcut for system application
			ATLLoadedApp system = ATLLoadedApp.getSystemApplication();
			return new ContextImpl(Resources.getSystem(), system,
			                       Resources.selectDefaultTheme(0, Build.VERSION.SDK_INT));
		}
		ATLLoadedApp loadedApplication = ATLLoadedAppManager.getAppFromPackageName(packageName);
		if (loadedApplication != null) {
			int atl_flags = 0;
			if ((flags & Context.CONTEXT_INCLUDE_CODE) == 0) {
				atl_flags |= ATL_FLAG_NO_CODE;
			}
			if ((flags & Context.CONTEXT_RESTRICTED) != 0) {
				atl_flags |= ATL_FLAG_RESTRICTED;
			}
			return new ContextImpl(loadedApplication.createDefaultResources(),
			                       loadedApplication, loadedApplication.pkg.applicationInfo.theme,
			                       atl_flags);
		}
		// Return the application context as a fallback
		Log.e(TAG, "!!!!!!! createPackageContext: case >" + packageName + "< is not implemented yet");
		return primaryApplication.getApplication();
	}

	@Override
	public Context createConfigurationContext(Configuration overrideConfiguration) {
		if (overrideConfiguration == null) {
			throw new IllegalArgumentException("overrideConfiguration must not be null");
		}
		ContextImpl context = this.atl_loaded_app.createContext(null, overrideConfiguration, getTheme());
		context.mIsUiContext = mIsUiContext;
		return context;
	}

	/* AOSP ContextImpl.getDisplay() throws for a context without a display (application or service);
	 * ATL does not tell the contexts apart and has the one display, which every context is on */
	@Override
	public Display getDisplay() {
		return getSystemService(DisplayManager.class).getDisplay(Display.DEFAULT_DISPLAY);
	}

	@Override
	public Context createDisplayContext(Display display) {
		return new ContextImpl(getResources(), this.atl_loaded_app, getTheme(), this.atl_flags);
	}

	/* AOSP: a WindowContext, a UI context for windows of the given type (toasts, overlays); ATL has one display */
	@Override
	public Context createWindowContext(int type, Bundle options) {
		return createWindowContext(null, type, options);
	}

	@Override
	public Context createWindowContext(Display display, int type, Bundle options) {
		ContextImpl context = new ContextImpl(getResources(), this.atl_loaded_app, getTheme(), this.atl_flags);
		context.mIsUiContext = true;
		return context;
	}

	@Override
	public boolean isUiContext() {
		return mIsUiContext;
	}

	@Override
	public boolean isDeviceProtectedStorage() {
		return (this.atl_flags & ATL_FLAG_PROTECTED_STORAGE) != 0;
	}

	@Override
	public Context createDeviceProtectedStorageContext() {
		return new ContextImpl(getResources(), this.atl_loaded_app, getTheme(),
		                       this.atl_flags | ATL_FLAG_PROTECTED_STORAGE);
	}

	/* AOSP ContextImpl.createCredentialProtectedStorageContext(): the device-protected flag is cleared */
	@Override
	public Context createCredentialProtectedStorageContext() {
		return new ContextImpl(getResources(), this.atl_loaded_app, getTheme(),
		                       this.atl_flags & ~ATL_FLAG_PROTECTED_STORAGE);
	}

	@Override
	public int getThemeResId() {
		return mThemeResource;
	}

	@Override
	public ATLLoadedApp get_atl_loaded_app() {
		return atl_loaded_app;
	}

	/** @hide a link for the desktop: a tel: number as the dialer reads it (RFC 3966, Android apps percent-encode the
	 * «+» as Uri.fromParts() does; Telecom decodes it with PhoneNumberUtils.getNumberFromIntent()) */
	public static String desktopUri(Uri uri) {
		if (uri != null && "tel".equalsIgnoreCase(uri.getScheme()) && uri.getSchemeSpecificPart() != null)
			return "tel:" + uri.getSchemeSpecificPart();
		return String.valueOf(uri);
	}
}
