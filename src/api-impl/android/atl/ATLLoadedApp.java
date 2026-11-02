package android.atl;

import android.annotation.NonNull;
import android.app.Activity;
import android.app.Application;
import android.app.ContextImpl;
import android.app.Service;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.pm.PackageParser;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.WeakHashMap;
import org.xmlpull.v1.XmlPullParserException;

/**
 * Hold ATL Specific application data to load apps
 */
public final class ATLLoadedApp {
	private final static String TAG = "ATLLoadedApp";
	static final HashSet<String> play_services = new HashSet<>(Arrays.asList(
	    "com.android.vending", "com.google.android.gms", "com.google.android.gsf"));
	private static ATLLoadedApp system_application;
	private static ATLLoadedApp primary_application;
	public final Resources default_resources;
	public final ClassLoader class_loader;
	public final PackageParser.Package pkg;
	public final int effective_sdk_compat;
	private final Object init_lock;
	private final HashMap<String, Service> running_services;
	private Application application;
	private Resources.Theme default_theme;
	/* the AssetManagers for the override configurations, see createResources() */
	private final HashMap<Configuration, AssetManager> override_assets = new HashMap<>();
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
		packageParser.collectCertificates(pkg, PackageParser.PARSE_IS_SYSTEM);
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
		Resources resources = new Resources(assetManager, new DisplayMetrics(), Context.sys_config);
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
		return track(new Resources(assets, displayMetrics, config),
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
		int label_res = 0;
		int soft_input_mode = 0;
		for (PackageParser.Activity activity : pkg.activities) {
			if (className.equals(activity.className)) {
				soft_input_mode = activity.info.softInputMode;
				label_res = activity.info.labelRes;
				theme_res = activity.info.getThemeResource();
				break;
			}
		}

		theme_res = Resources.selectDefaultTheme(theme_res, this.effective_sdk_compat);

		Class<? extends Activity> cls = this.loadClass(className).asSubclass(Activity.class);
		Constructor<? extends Activity> constructor = cls.getConstructor();
		Activity activity = constructor.newInstance();
		intent.setComponent(new ComponentName(pkg.packageName, className));
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
		if (label_res != 0) {
			activity.setTitle(r.getText(label_res));
		} else if (pkg.applicationInfo.labelRes != 0) {
			activity.setTitle(r.getText(pkg.applicationInfo.labelRes));
		}
		return activity;
	}

	public ComponentName startOrBindService(final Intent intent, final ServiceConnection serviceConnection) {
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
		final String className = component.getClassName();

		new Handler(Looper.getMainLooper()).post(new Runnable() {
			@Override
			public void run() {
				try {
					Service service = running_services.get(className);
					if (service == null) {
						Class<? extends Service> cls = ATLLoadedApp.this.loadClass(className).asSubclass(Service.class);
						service = cls.getConstructor().newInstance();
						service.attachBaseContext(new ContextImpl(
						    ATLLoadedApp.this.createDefaultResources(), ATLLoadedApp.this,
						    pkg.applicationInfo.theme));
						service.onCreate();
						running_services.put(className, service);
					}

					if (serviceConnection != null) {
						serviceConnection.onServiceConnected(intent.getComponent(), service.onBind(intent));
					} else {
						service.onStartCommand(intent, 0, 0);
					}
				} catch (ReflectiveOperationException e) {
					Slog.e(TAG, "startService: failed to start service " + className, e);
				}
			}
		});

		return component;
	}

	public boolean stopService(Intent intent) {
		if (intent.getComponent() == null)
			return false;
		String className = intent.getComponent().getClassName();
		Service service = running_services.get(className);
		if (service == null)
			return false;
		service.onDestroy();
		running_services.remove(className);
		return true;
	}

	public void receiveBroadcast(Context context, Intent intent) {
		ComponentName component = intent.getComponent();
		for (PackageParser.Activity receiver : pkg.receivers) {
			// an explicit broadcast goes only to its receiver, whatever its intent filters
			if (component != null) {
				if (component.getPackageName().equals(pkg.packageName) && component.getClassName().equals(receiver.className))
					deliverBroadcast(receiver, context, intent);
				continue;
			}
			if (receiver.intents == null)
				continue;
			for (PackageParser.IntentInfo intentInfo : receiver.intents) {
				if (intentInfo.matchAction(intent.getAction())) {
					deliverBroadcast(receiver, context, intent);
				}
			}
		}
	}

	private void deliverBroadcast(PackageParser.Activity receiver, Context context, Intent intent) {
		try {
			Class<? extends BroadcastReceiver> cls = this.loadClass(receiver.className).asSubclass(BroadcastReceiver.class);
			BroadcastReceiver receiverInstance = cls.newInstance();
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
