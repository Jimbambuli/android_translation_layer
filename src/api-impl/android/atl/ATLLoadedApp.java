package android.atl;

import android.annotation.NonNull;
import android.app.Activity;
import android.app.Application;
import android.app.ContextImpl;
import android.app.Service;
import android.atl.annotation.Export;
import android.content.*;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageParser;
import android.content.pm.ServiceInfo;
import android.graphics.Compatibility;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Slog;
import dalvik.system.BaseDexClassLoader;
import dalvik.system.PathClassLoader;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import org.xmlpull.v1.XmlPullParserException;

/**
 * Hold ATL Specific application data to load apps
 */
public final class ATLLoadedApp {
	private final static String TAG = "ATLLoadedApp";
	public static final Set<String> play_services = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
	    "com.android.vending", "com.google.android.gms", "com.google.android.gsf")));
	private static ATLLoadedApp system_application;
	private static ATLLoadedApp primary_application;
	public final Resources default_resources;
	public final ClassLoader class_loader;
	public final PackageParser.Package pkg;
	public final int effective_sdk_compat;
	private final Object init_lock;
	private final HashMap<String, ServiceRecord> running_services;
	/* connections bound without BIND_AUTO_CREATE to a service that is not running (ActiveServices keeps
	 * their bindings and connects them once the service is brought up by someone else) */
	private final HashMap<String, HashMap<ServiceConnection, Integer>> pending_bindings = new HashMap<>();
	private Application application;
	private Resources.Theme default_theme;
	/* the AssetManagers for the override configurations, see createResources() */
	private final HashMap<Configuration, AssetManager> override_assets = new HashMap<>();

	/* ResourcesManager.appendLibAssetsLocked() for Resources.registerResourcePaths(): the paths go into the assets of
	 * every Resources of this app; later override assets copy them from default_resources */
	public void atl_addResourcePaths(java.util.List<String> paths) {
		AssetManager base = default_resources.getAssets();
		for (String path : paths)
			base.addAssetPath(path);
		synchronized (override_assets) {
			for (AssetManager assets : override_assets.values())
				for (String path : paths)
					assets.addAssetPath(path);
		}
	}
	/* ResourcesManager.mResourceReferences: the Resources made here, with their override configuration
	 * (null for none), which follow a change of the system configuration */
	private static final WeakHashMap<Resources, Configuration> resource_references = new WeakHashMap<>();

	private static Resources track(Resources resources, Configuration overrideConfiguration) {
		synchronized (resource_references) {
			resource_references.put(resources, overrideConfiguration);
		}
		return resources;
	}

	/* ResourcesManager.applyConfigurationToResources(): every Resources takes the new system configuration,
	 * with its override configuration on top (ResourcesManager.generateConfig()) */
	public static void applyConfigurationToResources(Configuration config) {
		Resources.updateSystemConfiguration(config, null, null);
		Map.Entry<Resources, Configuration>[] entries;
		synchronized (resource_references) {
			entries = resource_references.entrySet().toArray(new Map.Entry[0]);
		}
		for (Map.Entry<Resources, Configuration> entry : entries) {
			Resources resources = entry.getKey();
			if (resources == null)
				continue;
			Configuration resourcesConfig = config;
			if (entry.getValue() != null) {
				resourcesConfig = new Configuration(config);
				resourcesConfig.updateFrom(entry.getValue());
			}
			resources.updateConfiguration(resourcesConfig, null);
		}
	}

	ATLLoadedApp(Resources resources, ClassLoader classLoader,
	             PackageParser.Package pkg) {
		this.default_resources = resources;
		this.class_loader = classLoader;
		this.pkg = pkg;
		this.effective_sdk_compat = Math.min(pkg.applicationInfo.targetSdkVersion, Build.VERSION.SDK_INT);
		/* AOSP sets it in ActivityThread.handleBindApplication(); AnimatedVectorDrawable reads it */
		Compatibility.setTargetSdkVersion(pkg.applicationInfo.targetSdkVersion);
		this.init_lock = new Object();
		this.running_services = new HashMap<>();
	}

	@NonNull
	public static ATLLoadedApp getSystemApplication() {
		if (system_application != null) {
			return system_application;
		}
		PackageParser packageParser = new PackageParser(null);
		Resources resources = Resources.getSystem();
		String[] outError = new String[1];
		PackageParser.Package pkg;
		try {
			pkg = packageParser.parsePackage(resources,
			                                 resources.getAssets().openXmlResourceParser(1, "AndroidManifest.xml"),
			                                 PackageParser.PARSE_IS_SYSTEM, outError);
		} catch (XmlPullParserException | IOException e) {
			throw new RuntimeException(outError[0], e);
		}
		/* no collectCertificates(): the framework resources come from no archive (the parser has no path), so it
		 * always failed with a NullPointerException in JarFile, logged a warning and left an unclosed JarFile for
		 * the finalizer; the package has no signatures either way */
		return system_application = new ATLLoadedApp(resources,
		                                             ATLLoadedApp.class.getClassLoader(), pkg);
	}

	@NonNull
	public static synchronized ATLLoadedApp getPrimaryApplication() {
		if (primary_application != null) {
			return primary_application;
		}
		final String classLoaderPath = System.getProperty("atl.app.class.path");
		final String nativePath = System.getProperty("atl.app.library.path");
		if (classLoaderPath == null && nativePath == null) {
			// Allow ATL test runner to not fail when no primary application is present
			Slog.w(TAG, "No primary application defined, using framework-res.apk as fallback");
			return primary_application = getSystemApplication();
		}
		final int pathsIndex = classLoaderPath.indexOf(':');
		try {
			return primary_application = loadFromPath(
				   pathsIndex == -1 ? classLoaderPath : classLoaderPath.substring(0, pathsIndex),
				   nativePath, classLoaderPath);
		} catch (IOException e) {
			// Failing to get the primary application should always be an error
			throw new Error("Failed to parse primary application", e);
		}
	}

	@NonNull
	public static ATLLoadedApp loadFromPath(String mainApk, String nativePath, String classLoaderPath) throws IOException {
		if (ATLToggles.ATL_GHAX) {
			// Always load ghax.jar as the first element to override application library classes
			classLoaderPath = ATLPaths.ghax_jar.getPath() + File.pathSeparator + classLoaderPath;
		}
		BaseDexClassLoader classLoader = new PathClassLoader(
		    classLoaderPath, nativePath, ATLLoadedApp.class.getClassLoader());
		AssetManager assetManager = new AssetManager(classLoaderPath + ":" + System.getProperty("java.class.path"));
		PackageParser packageParser = new PackageParser(mainApk);
		Resources resources = new Resources(classLoader, assetManager, new DisplayMetrics(), Context.sys_config);
		String[] outError = new String[1];
		PackageParser.Package pkg;
		try {
			pkg = packageParser.parsePackage(resources,
			                                 assetManager.openXmlResourceParser(1, "AndroidManifest.xml"), 0, outError);
		} catch (XmlPullParserException e) {
			throw new IOException(outError[0], e);
		}
		if (packageParser.getParseError() != PackageManager.INSTALL_SUCCEEDED) {
			throw new IOException(outError[0]);
		}
		packageParser.collectCertificates(pkg, 0);
		// Support for MicroG and other custom GMS implementations.
		if (play_services.contains(pkg.packageName)) {
			ATLSigHelper.addGMSSignatures(pkg);
		}
		pkg.applicationInfo.sourceDir = mainApk;
		return new ATLLoadedApp(track(resources, null), classLoader, pkg);
	}

	public Resources createDefaultResources() {
		return this.createResources(this.default_resources.getDisplayMetrics(), null);
	}

	/* Like ResourcesManager.getResources(): Resources with the same override configuration share the
	 * AssetManager (in AOSP the one of their ResourcesImpl), whose native configuration is theirs; those
	 * of another override configuration get an AssetManager of their own with the same ApkAssets. */
	public Resources createResources(DisplayMetrics displayMetrics, Configuration overrideConfiguration) {
		if (displayMetrics == null)
			displayMetrics = this.default_resources.getDisplayMetrics();
		AssetManager assets = this.default_resources.getAssets();
		Configuration config = Context.sys_config;
		/* ResourcesKey.hasOverrideConfiguration() */
		if (overrideConfiguration != null && !Configuration.EMPTY.equals(overrideConfiguration)) {
			/* ResourcesManager.findOrCreateResourcesImplForKeyLocked(), createAssetManager() */
			synchronized (this.override_assets) {
				assets = this.override_assets.get(overrideConfiguration);
				if (assets == null) {
					assets = new AssetManager(this.default_resources.getAssets());
					this.override_assets.put(new Configuration(overrideConfiguration), assets);
				}
			}
			/* ResourcesManager.generateConfig() */
			config = new Configuration(Context.sys_config);
			config.updateFrom(overrideConfiguration);
		}
		return track(new Resources(this.class_loader, assets, displayMetrics, config),
		             config == Context.sys_config ? null : new Configuration(overrideConfiguration));
	}

	public ContextImpl createContext(DisplayMetrics displayMetrics, Configuration overrideConfiguration, int theme) {
		return new ContextImpl(this.createResources(displayMetrics, overrideConfiguration), this, theme);
	}

	public ContextImpl createContext(DisplayMetrics displayMetrics, Configuration overrideConfiguration, Resources.Theme theme) {
		return new ContextImpl(this.createResources(displayMetrics, overrideConfiguration), this, theme);
	}

	public Resources.Theme getDefaultTheme() {
		if (this.default_theme != null)
			return this.default_theme;
		synchronized (this.init_lock) {
			if (this.default_theme != null)
				return this.default_theme;
			this.default_theme = this.default_resources.newTheme();
			this.default_theme.applyStyle(this.pkg.applicationInfo.theme, true);
		}
		return this.default_theme;
	}

	public Application getApplicationUnsafe() throws ReflectiveOperationException {
		if (this.application != null)
			return application;
		final String applicationClassName = pkg.applicationInfo.className;
		synchronized (this.init_lock) {
			if (application != null)
				return application;
			if (applicationClassName != null) {
				Class<? extends Application> cls =
				    this.loadClass(pkg.applicationInfo.className)
					.asSubclass(Application.class);
				Constructor<? extends Application> constructor = cls.getConstructor();
				application = constructor.newInstance();
			} else {
				application = new Application();
			}
			application.atl_attach_base_context(new ContextImpl(
			    this.createDefaultResources(), this, pkg.applicationInfo.theme));
			// HACK: Set WhatsApp's custom logging mechanism to verbose for easier debugging.
			// Should be removed again once WhatsApp is fully supported
			try {
				this.loadClass("com.whatsapp.util.Log").getField("level").setInt(null, 5);
			} catch (Exception ignore) {
			}
		}
		return application;
	}

	public Class<?> loadClass(String name) throws ClassNotFoundException {
		return Class.forName(name, false, class_loader);
	}

	public Application getApplication() {
		try {
			return this.getApplicationUnsafe();
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	public Activity createActivity(String className, Intent intent)
	    throws ReflectiveOperationException {
		int theme_res = 0;
		ActivityInfo info = null;
		int soft_input_mode = 0;
		String activityClass = className;
		for (PackageParser.Activity activity : pkg.activities) {
			if (className.equals(activity.className)) {
				soft_input_mode = activity.info.softInputMode;
				info = activity.info;
				theme_res = activity.info.getThemeResource();
				// an <activity-alias> (e.g. the launch intent of an app with switchable icons) runs its target,
				// the activity keeps the alias as its component like in AOSP
				if (activity.info.targetActivity != null)
					activityClass = activity.info.targetActivity;
				break;
			}
		}

		theme_res = Resources.selectDefaultTheme(theme_res, this.effective_sdk_compat);

		Class<? extends Activity> cls = this.loadClass(activityClass).asSubclass(Activity.class);
		Constructor<? extends Activity> constructor = cls.getConstructor();
		Activity activity = constructor.newInstance();
		activity.component = new ComponentName(pkg.packageName, className);
		intent.setComponent(activity.component);
		activity.intent = intent;
		Resources r = this.createDefaultResources();
		// Like ActivityThread.performLaunchActivity(): the base context only carries the default
		// theme, the activity theme is applied after attachBaseContext(). An activity may replace
		// its resources there (AppCompat does so for night mode), and the theme has to be resolved
		// against those resources, not against the ones of the base context.
		activity.atl_attach_base_context(ContextImpl.createActivityContext(r, this, Resources.selectDefaultTheme(0, this.effective_sdk_compat)));
		// like Activity.attach(), with the activity as the Window.Callback: the window takes the
		// manifest's soft input mode, the color mode and the post-processing preference (ATL reads
		// neither android:colorMode nor android:preferMinimalPostProcessing, so these are the defaults)
		if (soft_input_mode != 0)
			activity.getWindow().setSoftInputMode(soft_input_mode);
		activity.getWindow().setColorMode(0 /* ActivityInfo.COLOR_MODE_DEFAULT */);
		activity.getWindow().setPreferMinimalPostProcessing(false);
		activity.setTheme(theme_res);
		// ActivityThread.performLaunchActivity(): the title is r.activityInfo.loadLabel(), i.e. the activity's label,
		// else the application's, a literal android:label (nonLocalizedLabel) as well as a string resource
		// Activity.attach() sets it without onTitleChanged() (an activity's override may need its views, which
		// onCreate() makes; microG's AssistantActivity does)
		activity.atl_attach_title(info != null ? info.loadLabel(activity.getPackageManager()) : pkg.applicationInfo.loadLabel(activity.getPackageManager()));
		return activity;
	}

	/* ActiveServices' ServiceRecord: a running service of the app and what keeps it running */
	private static final class ServiceRecord {
		final Service service;
		final int flags; // ServiceInfo.flags
		boolean startRequested;
		int lastStartId;
		/* the bound connections with their bind flags (ConnectionRecord) and the intent of the binding */
		final HashMap<ServiceConnection, Integer> connections = new HashMap<>();
		Intent bindIntent;

		ServiceRecord(Service service, int flags) {
			this.service = service;
			this.flags = flags;
		}
	}

	private ServiceRecord getServiceRecord(String className) {
		synchronized (running_services) {
			return running_services.get(className);
		}
	}

	public boolean hasRunningServices() {
		synchronized (running_services) {
			return !running_services.isEmpty();
		}
	}

	/* android:isolatedProcess of a service of the app */
	public boolean isIsolatedService(String className) {
		for (PackageParser.Service s : pkg.services) {
			if (className.equals(s.className))
				return (s.info.flags & ServiceInfo.FLAG_ISOLATED_PROCESS) != 0;
		}
		return false;
	}

	/* the process a service of the app runs in (android:process); null if the app has no such service */
	public String serviceProcessName(String className) {
		for (PackageParser.Service s : pkg.services) {
			if (className.equals(s.className))
				return s.info.processName;
		}
		return null;
	}

	public ComponentName startOrBindService(final Intent intent, final ServiceConnection serviceConnection) {
		return startOrBindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
	}

	public ComponentName startOrBindService(final Intent intent, final ServiceConnection serviceConnection, final int flags) {
		ComponentName component = intent.getComponent();
		if (component == null) {
			int priority = Integer.MIN_VALUE;
			for (PackageParser.Service service : pkg.services) {
				if (service.intents == null)
					continue;
				for (PackageParser.IntentInfo intentInfo : service.intents) {
					if (intentInfo.matchAction(intent.getAction()) && intentInfo.priority > priority) {
						component = new ComponentName(pkg.packageName, service.className);
						priority = intentInfo.priority;
						break;
					}
				}
			}
		}
		if (component == null) {
			Slog.w(TAG, "startService: no matching service found for intent: " + intent);
			return null;
		}
		final ComponentName name = component;
		final String className = component.getClassName();

		new Handler(Looper.getMainLooper()).post(new Runnable() {
			@Override
			public void run() {
				try {
					ServiceRecord r = getServiceRecord(className);
					if (r == null && serviceConnection != null && (flags & Context.BIND_AUTO_CREATE) == 0) {
						synchronized (running_services) {
							HashMap<ServiceConnection, Integer> pending = pending_bindings.get(className);
							if (pending == null)
								pending_bindings.put(className, pending = new HashMap<>());
							pending.put(serviceConnection, flags);
						}
						return;
					}
					HashMap<ServiceConnection, Integer> pending = null;
					if (r == null) {
						Class<? extends Service> cls = ATLLoadedApp.this.loadClass(className).asSubclass(Service.class);
						Service service = cls.getConstructor().newInstance();
						service.attachBaseContext(new ContextImpl(
						    ATLLoadedApp.this.createDefaultResources(), ATLLoadedApp.this,
						    pkg.applicationInfo.theme));
						service.onCreate();
						int serviceFlags = 0;
						for (PackageParser.Service s : pkg.services) {
							if (className.equals(s.className))
								serviceFlags = s.info.flags;
						}
						r = new ServiceRecord(service, serviceFlags);
						synchronized (running_services) {
							running_services.put(className, r);
							pending = pending_bindings.remove(className);
							if (pending != null) {
								pending.remove(serviceConnection);
								r.connections.putAll(pending);
							}
						}
					}
					/* ActiveServices.realStartServiceLocked(): the bindings that waited for the service, before onStartCommand() */
					if (pending != null && !pending.isEmpty()) {
						Intent bindIntent = new Intent(intent);
						bindIntent.setComponent(new ComponentName(pkg.packageName, className));
						if (r.bindIntent == null)
							r.bindIntent = bindIntent;
						android.os.IBinder binder = r.service.onBind(r.bindIntent);
						for (ServiceConnection connection : pending.keySet()) {
							if (binder != null)
								connection.onServiceConnected(bindIntent.getComponent(), binder);
							else
								connection.onNullBinding(bindIntent.getComponent());
						}
					}

					if (serviceConnection != null) {
						synchronized (running_services) {
							r.connections.put(serviceConnection, flags);
						}
						if (r.bindIntent == null)
							r.bindIntent = intent;
						// AOSP delivers onNullBinding when the service's onBind returns null, never
						// onServiceConnected with a null binder (which crashes callers that dereference it).
						android.os.IBinder binder = r.service.onBind(intent);
						if (binder != null) {
							serviceConnection.onServiceConnected(name, binder);
						} else {
							serviceConnection.onNullBinding(name);
						}
					} else {
						int startId;
						synchronized (running_services) {
							r.startRequested = true;
							startId = ++r.lastStartId;
						}
						r.service.onStartCommand(intent, 0, startId);
					}
				} catch (ReflectiveOperationException e) {
					Slog.e(TAG, "startService: failed to start service " + className, e);
				}
			}
		});

		return component;
	}

	/* ActiveServices.bringDownServiceIfNeededLocked(): a service runs as long as it is started or bound with
	 * BIND_AUTO_CREATE (isServiceNeededLocked()); otherwise it is destroyed, on the main thread */
	private void bringDownServiceIfNeeded(final String className, final ServiceRecord r) {
		new Handler(Looper.getMainLooper()).post(new Runnable() {
			@Override
			public void run() {
				synchronized (running_services) {
					if (running_services.get(className) != r || r.startRequested)
						return;
					for (int bindFlags : r.connections.values()) {
						if ((bindFlags & Context.BIND_AUTO_CREATE) != 0)
							return;
					}
					running_services.remove(className);
				}
				Slog.i(TAG, "destroying service " + className);
				r.service.atl_destroy();
				boolean none;
				synchronized (running_services) {
					none = running_services.isEmpty();
				}
				if (none)
					ATLProcess.onServicesGone();
			}
		});
	}

	public boolean stopService(Intent intent) {
		if (intent.getComponent() == null)
			return false;
		String className = intent.getComponent().getClassName();
		ServiceRecord r = getServiceRecord(className);
		if (r == null)
			return false;
		synchronized (running_services) {
			r.startRequested = false;
		}
		bringDownServiceIfNeeded(className, r);
		return true;
	}

	/* ActiveServices.stopServiceTokenLocked(): Service.stopSelf(startId) stops the service only when startId is
	 * the last start (or negative, stopSelf()) */
	public boolean stopServiceToken(Service service, int startId) {
		String className = service.getClass().getName();
		ServiceRecord r = getServiceRecord(className);
		if (r == null || r.service != service)
			return false;
		synchronized (running_services) {
			if (startId >= 0 && r.lastStartId != startId)
				return false;
			r.startRequested = false;
		}
		bringDownServiceIfNeeded(className, r);
		return true;
	}

	/* ActiveServices.unbindServiceLocked()/removeConnectionLocked(): when the last connection of the service goes,
	 * it is unbound (onUnbind()); a connection with BIND_AUTO_CREATE no longer keeps it running */
	public void unbindService(final ServiceConnection serviceConnection) {
		new Handler(Looper.getMainLooper()).post(new Runnable() {
			@Override
			public void run() {
				String className = null;
				ServiceRecord r = null;
				Integer bindFlags = null;
				synchronized (running_services) {
					for (HashMap<ServiceConnection, Integer> pending : pending_bindings.values())
						pending.remove(serviceConnection);
					for (Map.Entry<String, ServiceRecord> entry : running_services.entrySet()) {
						bindFlags = entry.getValue().connections.remove(serviceConnection);
						if (bindFlags != null) {
							className = entry.getKey();
							r = entry.getValue();
							break;
						}
					}
				}
				if (r == null)
					return;
				if (r.connections.isEmpty())
					r.service.onUnbind(r.bindIntent);
				if ((bindFlags & Context.BIND_AUTO_CREATE) != 0)
					bringDownServiceIfNeeded(className, r);
			}
		});
	}

	/* Task.intent: the intent that started the app's task, kept apart from the root activity's getIntent(),
	 * which the app may replace (setIntent()) */
	private static Intent task_base_intent;

	public static synchronized void setTaskBaseIntent(Intent intent) {
		if (task_base_intent == null)
			task_base_intent = new Intent(intent);
	}

	/* The app's task was removed (its window closed): ActiveServices.cleanUpServices() stops the started services
	 * with android:stopWithTask and tells the others with Service.onTaskRemoved() */
	@Export
	private static void cleanUpServices() {
		final ATLLoadedApp app = getPrimaryApplication();
		final Intent baseIntent;
		synchronized (ATLLoadedApp.class) {
			baseIntent = task_base_intent;
		}
		ArrayList<Map.Entry<String, ServiceRecord>> services;
		synchronized (app.running_services) {
			services = new ArrayList<>(app.running_services.entrySet());
		}
		for (Map.Entry<String, ServiceRecord> entry : services) {
			final ServiceRecord r = entry.getValue();
			if (!r.startRequested)
				continue;
			if ((r.flags & ServiceInfo.FLAG_STOP_WITH_TASK) != 0) {
				Slog.i(TAG, "Stopping service " + entry.getKey() + ": remove task");
				synchronized (app.running_services) {
					r.startRequested = false;
				}
				app.bringDownServiceIfNeeded(entry.getKey(), r);
			} else {
				final Intent rootIntent = new Intent(baseIntent);
				Slog.i(TAG, "remove task: onTaskRemoved() for service " + entry.getKey());
				new Handler(Looper.getMainLooper()).post(new Runnable() {
					@Override
					public void run() {
						r.service.onTaskRemoved(rootIntent);
					}
				});
			}
		}
	}

	public void receiveBroadcast(Context context, Intent intent) {
		receiveBroadcast(context, intent, -1 /* Activity.RESULT_OK */);
	}

	/* resultCode: what the receivers' getResultCode() gives (Context.sendBroadcastWithResultCode()) */
	public void receiveBroadcast(Context context, Intent intent, int resultCode) {
		ComponentName component = intent.getComponent();
		for (PackageParser.Activity receiver : pkg.receivers) {
			// an explicit broadcast goes only to its receiver, whatever its intent filters
			if (component != null) {
				if (component.getPackageName().equals(pkg.packageName) && component.getClassName().equals(receiver.className))
					deliverBroadcast(receiver, context, intent, resultCode);
				continue;
			}
			if (receiver.intents == null)
				continue;
			for (PackageParser.IntentInfo intentInfo : receiver.intents) {
				if (intentInfo.matchAction(intent.getAction())) {
					deliverBroadcast(receiver, context, intent, resultCode);
				}
			}
		}
	}

	private void deliverBroadcast(PackageParser.Activity receiver, Context context, Intent intent, int resultCode) {
		try {
			Class<? extends BroadcastReceiver> cls = this.loadClass(receiver.className).asSubclass(BroadcastReceiver.class);
			BroadcastReceiver receiverInstance = cls.newInstance();
			receiverInstance.setResultCode(resultCode);
			// a receiver of another package runs with that package's context, as in its own process
			receiverInstance.onReceive(pkg.packageName.equals(context.getPackageName()) ? context : getApplication(), intent);
		} catch (ReflectiveOperationException e) {
			Slog.e(TAG, "startService: failed to receive broadcast " + receiver.className, e);
		}
	}

	public long getNativeWindow() {
		return this.application == null ? 0 : this.application.native_window;
	}
}
