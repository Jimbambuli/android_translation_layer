package android.atl;

import android.annotation.Nullable;
import android.content.pm.PackageParser;
import android.util.DisplayMetrics;
import android.util.Slog;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public final class ATLLoadedAppManager {
	private static final String TAG = "ATLLoadedAppManager";
	private static final HashMap<String, ATLLoadedApp> loaded_apps = new HashMap<>();
	private static final HashMap<String, PackageParser.Package> loaded_packages = new HashMap<>();
	private static final Object load_lock = new Object();
	private static final HashSet<String> always_visible_packages = new HashSet<>();
	private static final HashSet<String> always_allow_query_all_packages = new HashSet<>();

	static {
		// Always allow MicroG to be visible for application compatibility.
		always_visible_packages.addAll(ATLLoadedApp.play_services);
		// Always allow MicroG to see all applications for compatibility.
		always_allow_query_all_packages.addAll(ATLLoadedApp.play_services);
	}

	private ATLLoadedAppManager() {}

	@Nullable
	public static PackageParser.Package getPackageFromPackageName(String packageName) {
		ATLLoadedApp app = ATLLoadedApp.getPrimaryApplication();
		if (app.pkg.packageName.equals(packageName)) {
			return app.pkg;
		}
		if ("android".equals(packageName) || "atl".equals(packageName)) {
			return ATLLoadedApp.getSystemApplication().pkg;
		}
		app = loaded_apps.get(packageName);
		if (app != null) {
			return app.pkg;
		}
		PackageParser.Package pkg = loaded_packages.get(packageName);
		if (pkg != null) {
			return pkg;
		}
		return loadPackage(packageName);
	}

	@Nullable
	public static ATLLoadedApp peekAppFromPackageName(String packageName) {
		ATLLoadedApp app = ATLLoadedApp.getPrimaryApplication();
		if (app.pkg.packageName.equals(packageName)) {
			return app;
		}
		if ("android".equals(packageName) || "atl".equals(packageName)) {
			return ATLLoadedApp.getSystemApplication();
		}
		return loaded_apps.get(packageName);
	}

	@Nullable
	public static ATLLoadedApp getAppFromPackageName(String packageName) {
		ATLLoadedApp app = peekAppFromPackageName(packageName);
		if (app != null) {
			return app;
		}
		return loadApplication(packageName);
	}

	private static PackageParser.Package loadPackage(String packageName) {
		if (!isPackageVisible(ATLLoadedApp.getPrimaryApplication().pkg.packageName, packageName)) {
			// No right to view this package.
			return null;
		}
		PackageParser.Package pkg;
		synchronized (load_lock) {
			ATLLoadedApp app = loaded_apps.get(packageName);
			if (app != null) {
				return app.pkg;
			}
			pkg = loaded_packages.get(packageName);
			if (pkg != null) {
				return pkg;
			}
			File installed_apk_location = new File(ATLPaths.installed_apks_dir, packageName + ".apk");
			if (!installed_apk_location.isFile()) {
				if (ATLLoadedApp.play_services.contains(packageName)) {
					return loadGStub(packageName).pkg;
				}
				return null;
			}
			PackageParser packageParser = new PackageParser(installed_apk_location.getPath());
			pkg = packageParser.parsePackage(installed_apk_location,
			                                 installed_apk_location.getPath(), new DisplayMetrics(), 0);
			if (pkg != null) {
				setInstalledPaths(pkg);
				packageParser.collectCertificates(pkg, 0);
				if (ATLLoadedApp.play_services.contains(packageName)) {
					ATLSigHelper.addGMSSignatures(pkg);
				}
				loaded_packages.put(packageName, pkg);
			} else {
				Slog.e(TAG, "Failed to load package " + packageName);
			}
		}
		return pkg;
	}

	private static ATLLoadedApp loadApplication(String packageName) {
		if (!isPackageVisible(ATLLoadedApp.getPrimaryApplication().pkg.packageName, packageName)) {
			// No right to view this package.
			return null;
		}
		ATLLoadedApp app;
		synchronized (load_lock) {
			app = loaded_apps.get(packageName);
			if (app != null) {
				return app;
			}
			Slog.i(TAG, "Attempting to load " + packageName + " into " + ATLLoadedApp.getPrimaryApplication().pkg.packageName);
			File installed_apk_location = new File(ATLPaths.installed_apks_dir, packageName + ".apk");
			File installed_lib_location = new File(getDataDir(packageName), "lib");
			if (!installed_apk_location.isFile()) {
				if (ATLLoadedApp.play_services.contains(packageName)) {
					return loadGStub(packageName);
				}
				return null;
			}
			String apk_path = installed_apk_location.getPath();
			String lib_path = installed_lib_location.isDirectory() ? installed_lib_location.getPath() : null;
			try {
				app = ATLLoadedApp.loadFromPath(apk_path, lib_path, apk_path);
				setInstalledPaths(app.pkg);
				app.pkg.applicationInfo.nativeLibraryDir = lib_path;
				Slog.i(TAG, "loadApplication(\"" + packageName + "\")");
				loaded_packages.put(packageName, app.pkg);
				loaded_apps.put(packageName, app);
			} catch (IOException e) {
				Slog.e(TAG, "Failed to load " + packageName, e);
				return null;
			}
		}
		return app;
	}

	/**
	 * The data directory of an installed package: the one main.c gives the package when it is
	 * started from its file in _installed_apks_, so the package keeps its data whether it runs as
	 * the app or is loaded into another app's process.
	 */
	public static File getDataDir(String packageName) {
		return new File(ATLPaths.app_data_dir_base, packageName + ".apk_");
	}

	public static boolean isInstalled(String packageName) {
		return new File(ATLPaths.installed_apks_dir, packageName + ".apk").isFile();
	}

	/**
	 * Start an activity of another installed package, as Android starts it in that package's own
	 * process: ATL hands the package to the program in ATL_LAUNCH_COMMAND (an app starter), or
	 * starts its file in _installed_apks_ with android-translation-layer. False when the package is
	 * the caller's own, runs in the app's process (Play Services) or is not installed.
	 */
	public static boolean startInstalledApp(String callerPackageName, String packageName) {
		return startInstalledApp(callerPackageName, packageName, null);
	}

	/* uri: a link for the app, as from its menu entry (--uri) */
	public static boolean startInstalledApp(String callerPackageName, String packageName, String uri) {
		if (packageName == null || packageName.equals(callerPackageName) ||
		    always_visible_packages.contains(packageName) || !isInstalled(packageName))
			return false;
		List<String> command = new ArrayList<>();
		String launchCommand = System.getenv("ATL_LAUNCH_COMMAND");
		if (launchCommand != null && !launchCommand.trim().isEmpty()) {
			command.addAll(Arrays.asList(launchCommand.trim().split("\\s+")));
			if (uri != null)
				command.addAll(Arrays.asList("--uri", uri));
			command.add(packageName);
		} else {
			command.addAll(Arrays.asList("/usr/bin/env", "android-translation-layer",
			                             new File(ATLPaths.installed_apks_dir, packageName + ".apk").getPath()));
			if (uri != null)
				command.add("--uri=" + uri);
		}
		Slog.i(TAG, "starting installed app " + packageName + ": " + command);
		try {
			new ProcessBuilder(command).inheritIO().start();
			return true;
		} catch (IOException e) {
			Slog.e(TAG, "cannot start " + packageName, e);
			return false;
		}
	}

	private static void setInstalledPaths(PackageParser.Package pkg) {
		File apk = new File(ATLPaths.installed_apks_dir, pkg.packageName + ".apk");
		pkg.applicationInfo.sourceDir = apk.getPath();
		pkg.applicationInfo.publicSourceDir = apk.getPath();
		pkg.applicationInfo.dataDir = getDataDir(pkg.packageName).getPath();
		pkg.applicationInfo.deviceProtectedDataDir = pkg.applicationInfo.dataDir;
		pkg.applicationInfo.nativeLibraryDir = new File(getDataDir(pkg.packageName), "lib").getPath();
	}

	/**
	 * The packages the app sees, as AOSP's getInstalledPackages() with package visibility: the app
	 * itself and the packages in _installed_apks_ that it may see.
	 */
	public static List<String> getInstalledPackageNames() {
		String primaryPackageName = ATLLoadedApp.getPrimaryApplication().pkg.packageName;
		List<String> names = new ArrayList<>();
		names.add(primaryPackageName);
		String[] files = ATLPaths.installed_apks_dir.list();
		if (files != null) {
			Arrays.sort(files);
			for (String file : files) {
				if (!file.endsWith(".apk"))
					continue;
				String packageName = file.substring(0, file.length() - ".apk".length());
				if (!packageName.equals(primaryPackageName) && isPackageVisible(primaryPackageName, packageName))
					names.add(packageName);
			}
		}
		return names;
	}

	/**
	 * The package that installed `packageName` (AOSP InstallSource.mInstallerPackageName): the starter keeps it as
	 * "installer" in the PACKAGE.json next to the APK when an app store installed the app through ATL's
	 * PackageInstaller (ATL_INSTALLER_PACKAGE of ATL_INSTALL_COMMAND); null if it did not record one.
	 */
	public static String recordedInstaller(String packageName) {
		File meta = new File(ATLPaths.installed_apks_dir, packageName + ".json");
		if (!meta.isFile())
			return null;
		try {
			org.json.JSONObject json = new org.json.JSONObject(new String(java.nio.file.Files.readAllBytes(meta.toPath()),
			                                                              java.nio.charset.StandardCharsets.UTF_8));
			String installer = json.optString("installer", "");
			return installer.isEmpty() ? null : installer;
		} catch (IOException | org.json.JSONException e) {
			Slog.w(TAG, "cannot read " + meta + ": " + e);
			return null;
		}
	}

	/**
	 * The other installed apps that take `type` shared (SEND/SEND_MULTIPLE), for the share sheet, which is system UI
	 * and sees every app: {package, label, icon file}. The starter keeps the types each app takes in "share_types"
	 * of the PACKAGE.json next to its APK (from the app's filters in CATEGORY_DEFAULT); apps installed before it
	 * did lack them and are not offered.
	 */
	public static List<String[]> shareTargets(String type, String ownPackageName) {
		List<String[]> targets = new ArrayList<>();
		String[] files = ATLPaths.installed_apks_dir.list();
		if (files == null || type == null)
			return targets;
		Arrays.sort(files);
		String wanted = type.toLowerCase(java.util.Locale.ROOT);
		for (String file : files) {
			if (!file.endsWith(".json"))
				continue;
			String packageName = file.substring(0, file.length() - ".json".length());
			if (packageName.equals(ownPackageName) || always_visible_packages.contains(packageName)
			    || !new File(ATLPaths.installed_apks_dir, packageName + ".apk").isFile())
				continue;
			try {
				org.json.JSONObject meta = new org.json.JSONObject(new String(java.nio.file.Files.readAllBytes(
				    new File(ATLPaths.installed_apks_dir, file).toPath()), java.nio.charset.StandardCharsets.UTF_8));
				org.json.JSONArray types = meta.optJSONArray("share_types");
				if (types == null)
					continue;
				for (int i = 0; i < types.length(); i++) {
					if (mimeMatches(types.getString(i).toLowerCase(java.util.Locale.ROOT), wanted)) {
						File icon = new File(ATLPaths.installed_apks_dir, packageName + ".png");
						targets.add(new String[] {packageName, meta.optString("label", packageName),
						                          icon.isFile() ? icon.getPath() : null});
						break;
					}
				}
			} catch (IOException | org.json.JSONException e) {
				Slog.w(TAG, "shareTargets: cannot read " + file + ": " + e);
			}
		}
		return targets;
	}

	/* a filter's type against what is shared, both possibly with a * subtype (as Intent.normalizeMimeType/IntentFilter) */
	private static boolean mimeMatches(String filter, String type) {
		if (filter.equals("*/*") || filter.equals("*") || filter.equals(type))
			return true;
		int f = filter.indexOf('/'), t = type.indexOf('/');
		if (f < 0 || t < 0)
			return false;
		String fMain = filter.substring(0, f), tMain = type.substring(0, t);
		return fMain.equals(tMain) && (filter.endsWith("/*") || type.endsWith("/*"));
	}

	/**
	 * Find an exported content provider of another package, as AOSP's provider lookup over all
	 * packages; ATL looks only at the packages every app may see (Play Services), the provider
	 * then runs in the app's process like the rest of the package.
	 */
	@Nullable
	public static PackageParser.Provider findForeignProvider(String authority) {
		String primaryPackageName = ATLLoadedApp.getPrimaryApplication().pkg.packageName;
		for (String packageName : always_visible_packages) {
			if (packageName.equals(primaryPackageName)) {
				continue;
			}
			PackageParser.Package pkg = getPackageFromPackageName(packageName);
			if (pkg == null) {
				continue;
			}
			for (PackageParser.Provider provider : pkg.providers) {
				if (provider.info.exported && authority.equals(provider.info.authority)) {
					return provider;
				}
			}
		}
		return null;
	}

	/* AOSP AppsFilterImpl.shouldFilterApplication(): an app holding QUERY_ALL_PACKAGES, or one that targets
	 * SDK 29 or lower, sees every package (the <queries> element is not looked at yet) */
	private static boolean isPackageVisible(String sourcePackageName, String targetPackageName) {
		if (always_visible_packages.contains(targetPackageName) || always_allow_query_all_packages.contains(sourcePackageName))
			return true;
		ATLLoadedApp primary = ATLLoadedApp.getPrimaryApplication();
		if (!primary.pkg.packageName.equals(sourcePackageName))
			return false;
		return primary.pkg.applicationInfo.targetSdkVersion <= 29
		    || primary.pkg.requestedPermissions.contains("android.permission.QUERY_ALL_PACKAGES");
	}

	private static ATLLoadedApp loadGStub(String packageName) {
		ATLLoadedApp app = ATLGStub.loadFakePlayServices(packageName);
		Slog.i(TAG, "loadGStub(\"" + packageName + "\")");
		loaded_packages.put(packageName, app.pkg);
		loaded_apps.put(packageName, app);
		return app;
	}
}
