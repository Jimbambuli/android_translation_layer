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

	private static void setInstalledPaths(PackageParser.Package pkg) {
		File apk = new File(ATLPaths.installed_apks_dir, pkg.packageName + ".apk");
		pkg.applicationInfo.sourceDir = apk.getPath();
		pkg.applicationInfo.publicSourceDir = apk.getPath();
		pkg.applicationInfo.dataDir = getDataDir(pkg.packageName).getPath();
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

	private static boolean isPackageVisible(String sourcePackageName, String targetPackageName) {
		return always_visible_packages.contains(targetPackageName) || always_allow_query_all_packages.contains(sourcePackageName);
	}

	private static ATLLoadedApp loadGStub(String packageName) {
		ATLLoadedApp app = ATLGStub.loadFakePlayServices(packageName);
		Slog.i(TAG, "loadGStub(\"" + packageName + "\")");
		loaded_packages.put(packageName, app.pkg);
		loaded_apps.put(packageName, app);
		return app;
	}
}
