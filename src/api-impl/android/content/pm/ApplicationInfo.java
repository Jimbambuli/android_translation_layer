/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.content.pm;

import android.content.Context;
import android.annotation.UnsupportedAppUsage;
import android.content.pm.PackageManager.NameNotFoundException;
// import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.Printer;
import java.text.Collator;
import java.util.Comparator;

/**
 * Information you can retrieve about a particular application.  This
 * corresponds to information collected from the AndroidManifest.xml's
 * &lt;application&gt; tag.
 */
public class ApplicationInfo extends PackageItemInfo implements android.os.Parcelable {

	/**
	 * Default task affinity of all activities in this application. See
	 * {@link ActivityInfo#taskAffinity} for more information.  This comes
	 * from the "taskAffinity" attribute.
	 */
	public String taskAffinity;

	/**
	 * Optional name of a permission required to be able to access this
	 * application's components.  From the "permission" attribute.
	 */
	public String permission;

	/**
	 * The name of the process this application should run in.  From the
	 * "process" attribute or, if not set, the same as
	 * <var>packageName</var>.
	 */
	public String processName;

	/**
	 * Class implementing the Application object.  From the "class"
	 * attribute.
	 */
	public String className;

	/**
	 * A style resource identifier (in the package's resources) of the
	 * description of an application.  From the "description" attribute
	 * or, if not set, 0.
	 */
	public int descriptionRes;

	/**
	 * A style resource identifier (in the package's resources) of the
	 * default visual theme of the application.  From the "theme" attribute
	 * or, if not set, 0.
	 */
	public int theme;

	/* AOSP ApplicationInfo.localeConfigRes: the android:localeConfig resource of the application element, 0 if none */
	public int localeConfigRes;

	public int getLocaleConfigRes() {
		return localeConfigRes;
	}

	/**
	 * Class implementing the Application's manage space
	 * functionality.  From the "manageSpaceActivity"
	 * attribute. This is an optional attribute and will be null if
	 * applications don't specify it in their manifest
	 */
	public String manageSpaceActivityName;

	/**
	 * Class implementing the Application's backup functionality.  From
	 * the "backupAgent" attribute.  This is an optional attribute and
	 * will be null if the application does not specify it in its manifest.
	 *
	 * <p>If android:allowBackup is set to false, this attribute is ignored.
	 */
	public String backupAgentName;

	/**
	 * The default extra UI options for activities in this application.
	 * Set from the {@link android.R.attr#uiOptions} attribute in the
	 * activity's manifest.
	 */
	public int uiOptions = 0;

	/**
	 * Value for {@link #flags}: if set, this application is installed in the
	 * device's system image.
	 */
	public static final int FLAG_SYSTEM = 1 << 0;

	/**
	 * Value for {@link #flags}: set to true if this application would like to
	 * allow debugging of its
	 * code, even when installed on a non-development system.  Comes
	 * from {@link android.R.styleable#AndroidManifestApplication_debuggable
	 * android:debuggable} of the &lt;application&gt; tag.
	 */
	public static final int FLAG_DEBUGGABLE = 1 << 1;

	/**
	 * Value for {@link #flags}: set to true if this application has code
	 * associated with it.  Comes
	 * from {@link android.R.styleable#AndroidManifestApplication_hasCode
	 * android:hasCode} of the &lt;application&gt; tag.
	 */
	public static final int FLAG_HAS_CODE = 1 << 2;

	/**
	 * Value for {@link #flags}: set to true if this application is persistent.
	 * Comes from {@link android.R.styleable#AndroidManifestApplication_persistent
	 * android:persistent} of the &lt;application&gt; tag.
	 */
	public static final int FLAG_PERSISTENT = 1 << 3;

	/**
	 * Value for {@link #flags}: set to true if this application holds the
	 * {@link android.Manifest.permission#FACTORY_TEST} permission and the
	 * device is running in factory test mode.
	 */
	public static final int FLAG_FACTORY_TEST = 1 << 4;

	/**
	 * Value for {@link #flags}: default value for the corresponding ActivityInfo flag.
	 * Comes from {@link android.R.styleable#AndroidManifestApplication_allowTaskReparenting
	 * android:allowTaskReparenting} of the &lt;application&gt; tag.
	 */
	public static final int FLAG_ALLOW_TASK_REPARENTING = 1 << 5;

	/**
	 * Value for {@link #flags}: default value for the corresponding ActivityInfo flag.
	 * Comes from {@link android.R.styleable#AndroidManifestApplication_allowClearUserData
	 * android:allowClearUserData} of the &lt;application&gt; tag.
	 */
	public static final int FLAG_ALLOW_CLEAR_USER_DATA = 1 << 6;

	/**
	 * Value for {@link #flags}: this is set if this application has been
	 * install as an update to a built-in system application.
	 */
	public static final int FLAG_UPDATED_SYSTEM_APP = 1 << 7;

	/**
	 * Value for {@link #flags}: this is set of the application has specified
	 * {@link android.R.styleable#AndroidManifestApplication_testOnly
	 * android:testOnly} to be true.
	 */
	public static final int FLAG_TEST_ONLY = 1 << 8;

	/**
	 * Value for {@link #flags}: true when the application's window can be
	 * reduced in size for smaller screens.  Corresponds to
	 * {@link android.R.styleable#AndroidManifestSupportsScreens_smallScreens
	 * android:smallScreens}.
	 */
	public static final int FLAG_SUPPORTS_SMALL_SCREENS = 1 << 9;

	/**
	 * Value for {@link #flags}: true when the application's window can be
	 * displayed on normal screens.  Corresponds to
	 * {@link android.R.styleable#AndroidManifestSupportsScreens_normalScreens
	 * android:normalScreens}.
	 */
	public static final int FLAG_SUPPORTS_NORMAL_SCREENS = 1 << 10;

	/**
	 * Value for {@link #flags}: true when the application's window can be
	 * increased in size for larger screens.  Corresponds to
	 * {@link android.R.styleable#AndroidManifestSupportsScreens_largeScreens
	 * android:largeScreens}.
	 */
	public static final int FLAG_SUPPORTS_LARGE_SCREENS = 1 << 11;

	/**
	 * Value for {@link #flags}: true when the application knows how to adjust
	 * its UI for different screen sizes.  Corresponds to
	 * {@link android.R.styleable#AndroidManifestSupportsScreens_resizeable
	 * android:resizeable}.
	 */
	public static final int FLAG_RESIZEABLE_FOR_SCREENS = 1 << 12;

	/**
	 * Value for {@link #flags}: true when the application knows how to
	 * accomodate different screen densities.  Corresponds to
	 * {@link android.R.styleable#AndroidManifestSupportsScreens_anyDensity
	 * android:anyDensity}.
	 */
	public static final int FLAG_SUPPORTS_SCREEN_DENSITIES = 1 << 13;

	/**
	 * Value for {@link #flags}: set to true if this application would like to
	 * request the VM to operate under the safe mode. Comes from
	 * {@link android.R.styleable#AndroidManifestApplication_vmSafeMode
	 * android:vmSafeMode} of the &lt;application&gt; tag.
	 */
	public static final int FLAG_VM_SAFE_MODE = 1 << 14;

	/**
	 * Value for {@link #flags}: set to <code>false</code> if the application does not wish
	 * to permit any OS-driven backups of its data; <code>true</code> otherwise.
	 *
	 * <p>Comes from the
	 * {@link android.R.styleable#AndroidManifestApplication_allowBackup android:allowBackup}
	 * attribute of the &lt;application&gt; tag.
	 */
	public static final int FLAG_ALLOW_BACKUP = 1 << 15;

	/**
	 * Value for {@link #flags}: set to <code>false</code> if the application must be kept
	 * in memory following a full-system restore operation; <code>true</code> otherwise.
	 * Ordinarily, during a full system restore operation each application is shut down
	 * following execution of its agent's onRestore() method.  Setting this attribute to
	 * <code>false</code> prevents this.  Most applications will not need to set this attribute.
	 *
	 * <p>If
	 * {@link android.R.styleable#AndroidManifestApplication_allowBackup android:allowBackup}
	 * is set to <code>false</code> or no
	 * {@link android.R.styleable#AndroidManifestApplication_backupAgent android:backupAgent}
	 * is specified, this flag will be ignored.
	 *
	 * <p>Comes from the
	 * {@link android.R.styleable#AndroidManifestApplication_killAfterRestore android:killAfterRestore}
	 * attribute of the &lt;application&gt; tag.
	 */
	public static final int FLAG_KILL_AFTER_RESTORE = 1 << 16;

	/**
	 * Value for {@link #flags}: Set to <code>true</code> if the application's backup
	 * agent claims to be able to handle restore data even "from the future,"
	 * i.e. from versions of the application with a versionCode greater than
	 * the one currently installed on the device.  <i>Use with caution!</i>  By default
	 * this attribute is <code>false</code> and the Backup Manager will ensure that data
	 * from "future" versions of the application are never supplied during a restore operation.
	 *
	 * <p>If
	 * {@link android.R.styleable#AndroidManifestApplication_allowBackup android:allowBackup}
	 * is set to <code>false</code> or no
	 * {@link android.R.styleable#AndroidManifestApplication_backupAgent android:backupAgent}
	 * is specified, this flag will be ignored.
	 *
	 * <p>Comes from the
	 * {@link android.R.styleable#AndroidManifestApplication_restoreAnyVersion android:restoreAnyVersion}
	 * attribute of the &lt;application&gt; tag.
	 */
	public static final int FLAG_RESTORE_ANY_VERSION = 1 << 17;

	/**
	 * Value for {@link #flags}: Set to true if the application is
	 * currently installed on external/removable/unprotected storage.  Such
	 * applications may not be available if their storage is not currently
	 * mounted.  When the storage it is on is not available, it will look like
	 * the application has been uninstalled (its .apk is no longer available)
	 * but its persistent data is not removed.
	 */
	public static final int FLAG_EXTERNAL_STORAGE = 1 << 18;

	/**
	 * Value for {@link #flags}: true when the application's window can be
	 * increased in size for extra large screens.  Corresponds to
	 * {@link android.R.styleable#AndroidManifestSupportsScreens_xlargeScreens
	 * android:xlargeScreens}.
	 */
	public static final int FLAG_SUPPORTS_XLARGE_SCREENS = 1 << 19;

	/**
	 * Value for {@link #flags}: true when the application has requested a
	 * large heap for its processes.  Corresponds to
	 * {@link android.R.styleable#AndroidManifestApplication_largeHeap
	 * android:largeHeap}.
	 */
	public static final int FLAG_LARGE_HEAP = 1 << 20;

	/**
	 * Value for {@link #flags}: true if this application's package is in
	 * the stopped state.
	 */
	public static final int FLAG_STOPPED = 1 << 21;

	/**
	 * Value for {@link #flags}: true  when the application is willing to support
	 * RTL (right to left). All activities will inherit this value.
	 *
	 * Set from the {@link android.R.attr#supportsRtl} attribute in the
	 * activity's manifest.
	 *
	 * Default value is false (no support for RTL).
	 */
	public static final int FLAG_SUPPORTS_RTL = 1 << 22;

	/**
	 * Value for {@link #flags}: true if the application is currently
	 * installed for the calling user.
	 */
	public static final int FLAG_INSTALLED = 1 << 23;

	/**
	 * Value for {@link #flags}: true if the application only has its
	 * data installed; the application package itself does not currently
	 * exist on the device.
	 */
	public static final int FLAG_IS_DATA_ONLY = 1 << 24;

	/* AOSP ApplicationInfo (Android 16): flags bits 25..31. ATL's old hidden flags FLAG_PRIVILEGED (1<<30),
	 * FLAG_FORWARD_LOCK (1<<29), FLAG_CANT_SAVE_STATE (1<<28) and FLAG_BLOCKED (1<<27) took these bits; Android
	 * moved them to privateFlags (PRIVATE_FLAG_PRIVILEGED, PRIVATE_FLAG_CANT_SAVE_STATE, PRIVATE_FLAG_HIDDEN)
	 * and dropped forward locking. */

	/** Value for {@link #flags}: the application is a game (deprecated: use {@link #category}). */
	@Deprecated
	public static final int FLAG_IS_GAME = 1 << 25;

	/** Value for {@link #flags}: android:fullBackupOnly of the &lt;application&gt; tag. */
	public static final int FLAG_FULL_BACKUP_ONLY = 1 << 26;

	/** Value for {@link #flags}: android:usesCleartextTraffic of the &lt;application&gt; tag. */
	public static final int FLAG_USES_CLEARTEXT_TRAFFIC = 1 << 27;

	/** Value for {@link #flags}: android:extractNativeLibs of the &lt;application&gt; tag. */
	public static final int FLAG_EXTRACT_NATIVE_LIBS = 1 << 28;

	/** Value for {@link #flags}: android:hardwareAccelerated of the &lt;application&gt; tag. */
	public static final int FLAG_HARDWARE_ACCELERATED = 1 << 29;

	/** Value for {@link #flags}: the application is suspended. */
	public static final int FLAG_SUSPENDED = 1 << 30;

	/** Value for {@link #flags}: android:multiArch of the &lt;application&gt; tag. */
	public static final int FLAG_MULTIARCH = 1 << 31;

	/**
	 * Flags associated with the application.  Any combination of
	 * {@link #FLAG_SYSTEM}, {@link #FLAG_DEBUGGABLE}, {@link #FLAG_HAS_CODE},
	 * {@link #FLAG_PERSISTENT}, {@link #FLAG_FACTORY_TEST}, and
	 * {@link #FLAG_ALLOW_TASK_REPARENTING}
	 * {@link #FLAG_ALLOW_CLEAR_USER_DATA}, {@link #FLAG_UPDATED_SYSTEM_APP},
	 * {@link #FLAG_TEST_ONLY}, {@link #FLAG_SUPPORTS_SMALL_SCREENS},
	 * {@link #FLAG_SUPPORTS_NORMAL_SCREENS},
	 * {@link #FLAG_SUPPORTS_LARGE_SCREENS}, {@link #FLAG_SUPPORTS_XLARGE_SCREENS},
	 * {@link #FLAG_RESIZEABLE_FOR_SCREENS},
	 * {@link #FLAG_SUPPORTS_SCREEN_DENSITIES}, {@link #FLAG_VM_SAFE_MODE},
	 * {@link #FLAG_INSTALLED}.
	 */
	public int flags = 0;

	/**
	 * Value for {@link #privateFlags}: whether this app is a resource overlay.
	 * @hide
	 */
	public static final int PRIVATE_FLAG_IS_RESOURCE_OVERLAY = 1 << 28;

	/** Value for {@link #privateFlags}: the application is hidden via restrictions. @hide */
	public static final int PRIVATE_FLAG_HIDDEN = 1 << 0;

	/** Value for {@link #privateFlags}: the application is heavy-weight (android:cantSaveState). @hide */
	public static final int PRIVATE_FLAG_CANT_SAVE_STATE = 1 << 1;

	/** Value for {@link #privateFlags}: the application may hold privileged permissions. @hide */
	public static final int PRIVATE_FLAG_PRIVILEGED = 1 << 3;

	/**
	 * Private/hidden flags. See {@code PRIVATE_FLAG_...} constants.
	 * @hide
	 */
	public int privateFlags;

	/**
	 * The required smallest screen width the application can run on.  If 0,
	 * nothing has been specified.  Comes from
	 * {@link android.R.styleable#AndroidManifestSupportsScreens_requiresSmallestWidthDp
	 * android:requiresSmallestWidthDp} attribute of the &lt;supports-screens&gt; tag.
	 */
	public int requiresSmallestWidthDp = 0;

	/**
	 * The maximum smallest screen width the application is designed for.  If 0,
	 * nothing has been specified.  Comes from
	 * {@link android.R.styleable#AndroidManifestSupportsScreens_compatibleWidthLimitDp
	 * android:compatibleWidthLimitDp} attribute of the &lt;supports-screens&gt; tag.
	 */
	public int compatibleWidthLimitDp = 0;

	/**
	 * The maximum smallest screen width the application will work on.  If 0,
	 * nothing has been specified.  Comes from
	 * {@link android.R.styleable#AndroidManifestSupportsScreens_largestWidthLimitDp
	 * android:largestWidthLimitDp} attribute of the &lt;supports-screens&gt; tag.
	 */
	public int largestWidthLimitDp = 0;

	/**
	 * Full path to the location of this package.
	 */
	public String sourceDir = "/tmp/FIXME/FIXME.apk"; // FIXME

	/**
	 * Full path to the location of the publicly available parts of this
	 * package (i.e. the primary resource package and manifest).  For
	 * non-forward-locked apps this will be the same as {@link #sourceDir).
	 */
	public String publicSourceDir;

	/**
	 * Full paths to the locations of extra resource packages this application
	 * uses. This field is only used if there are extra resource packages,
	 * otherwise it is null.
	 *
	 * {@hide}
	 */
	public String[] resourceDirs;

	/**
	 * String retrieved from the seinfo tag found in selinux policy. This value
	 * is useful in setting an SELinux security context on the process as well
	 * as its data directory.
	 *
	 * {@hide}
	 */
	public String seinfo;

	/**
	 * Paths to all shared libraries this application is linked against.  This
	 * field is only set if the {@link PackageManager#GET_SHARED_LIBRARY_FILES
	 * PackageManager.GET_SHARED_LIBRARY_FILES} flag was used when retrieving
	 * the structure.
	 */
	public String[] sharedLibraryFiles;

	/**
	 * Full path to a directory assigned to the package for its persistent
	 * data.
	 */
	public String dataDir;

	/* AOSP: the device-protected (direct boot) counterpart of dataDir; ATL has the one storage */
	public String deviceProtectedDataDir;

	/**
	 * Full path to the directory where native JNI libraries are stored.
	 */
	public String nativeLibraryDir;

	/**
	 * While not being part of the official Android API, WhatsApp uses this field to check for cpu ABI.
	 */
	@UnsupportedAppUsage
	public String primaryCpuAbi;

	/**
	 * While not being part of the official Android API, WhatsApp uses this field to check for cpu ABI.
	 */
	@UnsupportedAppUsage
	public String secondaryCpuAbi;

	/**
	 * The kernel user-ID that has been assigned to this application;
	 * currently this is not a unique ID (multiple applications can have
	 * the same uid).
	 */
	public int uid;

	/**
	 * The minimum SDK version this application can run on. It will not run
	 * on earlier versions.
	 */
	public int minSdkVersion;

	/**
	 * The minimum SDK version this application targets.  It may run on earlier
	 * versions, but it knows how to work with any new behavior added at this
	 * version.  Will be {@link android.os.Build.VERSION_CODES#CUR_DEVELOPMENT}
	 * if this is a development build and the app is targeting that.  You should
	 * compare that this number is >= the SDK version number at which your
	 * behavior was introduced.
	 */
	public int targetSdkVersion;

	/**
	 * When false, indicates that all components within this application are
	 * considered disabled, regardless of their individually set enabled status.
	 */
	public boolean enabled = true;

	/**
	 * For convenient access to the current enabled setting of this app.
	 * @hide
	 */
	public int enabledSetting = PackageManager.COMPONENT_ENABLED_STATE_DEFAULT;

	/**
	 * For convenient access to package's install location.
	 * @hide
	 */
	public int installLocation = PackageInfo.INSTALL_LOCATION_UNSPECIFIED;

	public String[] splitSourceDirs;

	public void dump(Printer pw, String prefix) {
		super.dumpFront(pw, prefix);
		if (className != null) {
			pw.println(prefix + "className=" + className);
		}
		if (permission != null) {
			pw.println(prefix + "permission=" + permission);
		}
		pw.println(prefix + "processName=" + processName);
		pw.println(prefix + "taskAffinity=" + taskAffinity);
		pw.println(prefix + "uid=" + uid + " flags=0x" + Integer.toHexString(flags) + " theme=0x" + Integer.toHexString(theme));
		pw.println(prefix + "requiresSmallestWidthDp=" + requiresSmallestWidthDp + " compatibleWidthLimitDp=" + compatibleWidthLimitDp + " largestWidthLimitDp=" + largestWidthLimitDp);
		pw.println(prefix + "sourceDir=" + sourceDir);
		if (sourceDir == null) {
			if (publicSourceDir != null) {
				pw.println(prefix + "publicSourceDir=" + publicSourceDir);
			}
		} else if (!sourceDir.equals(publicSourceDir)) {
			pw.println(prefix + "publicSourceDir=" + publicSourceDir);
		}
		if (resourceDirs != null) {
			pw.println(prefix + "resourceDirs=" + resourceDirs);
		}
		if (seinfo != null) {
			pw.println(prefix + "seinfo=" + seinfo);
		}
		pw.println(prefix + "dataDir=" + dataDir);
		if (sharedLibraryFiles != null) {
			pw.println(prefix + "sharedLibraryFiles=" + sharedLibraryFiles);
		}
		pw.println(prefix + "enabled=" + enabled + " targetSdkVersion=" + targetSdkVersion);
		if (manageSpaceActivityName != null) {
			pw.println(prefix + "manageSpaceActivityName=" + manageSpaceActivityName);
		}
		if (descriptionRes != 0) {
			pw.println(prefix + "description=0x" + Integer.toHexString(descriptionRes));
		}
		if (uiOptions != 0) {
			pw.println(prefix + "uiOptions=0x" + Integer.toHexString(uiOptions));
		}
		pw.println(prefix + "supportsRtl=" + (hasRtlSupport() ? "true" : "false"));
		super.dumpBack(pw, prefix);
	}

	/**
	 * @return true if "supportsRtl" has been set to true in the AndroidManifest
	 * @hide
	 */
	/**
	 * Returns true if the package has declared in its manifest that it is a
	 * runtime resource overlay.
	 */
	public boolean isResourceOverlay() {
		return (privateFlags & ApplicationInfo.PRIVATE_FLAG_IS_RESOURCE_OVERLAY) != 0;
	}

	public boolean hasRtlSupport() {
		return (flags & FLAG_SUPPORTS_RTL) == FLAG_SUPPORTS_RTL;
	}

	public static class DisplayNameComparator
	    implements Comparator<ApplicationInfo> {
		public DisplayNameComparator(PackageManager pm) {
			mPM = pm;
		}

		public final int compare(ApplicationInfo aa, ApplicationInfo ab) {
			CharSequence sa = mPM.getApplicationLabel(aa);
			if (sa == null) {
				sa = aa.packageName;
			}
			CharSequence sb = mPM.getApplicationLabel(ab);
			if (sb == null) {
				sb = ab.packageName;
			}

			return sCollator.compare(sa.toString(), sb.toString());
		}

		private final Collator sCollator = Collator.getInstance();
		private PackageManager mPM;
	}

	/* AOSP ApplicationInfo (android-14): fields that the package parser fills; ATL leaves the values the parser
	 * leaves for a package without them (null, 0, CATEGORY_UNDEFINED) */
	public java.util.UUID storageUuid;
	public String[] splitNames;
	public String seInfo;
	public String seInfoUser;
	public int compileSdkVersion;
	public String compileSdkVersionCodename;
	public String zygotePreloadName;

	/**
	 * The factory of this package, as specified by the &lt;manifest&gt;
	 * tag's {@link android.R.styleable#AndroidManifestApplication_appComponentFactory}
	 * attribute.
	 */
	public String appComponentFactory;

	/* AOSP ApplicationInfo (Android 16): values of android:gwpAsanMode, android:memtagMode,
	 * android:nativeHeapZeroInitialized and android:requestRawExternalStorageAccess */
	public static final int GWP_ASAN_DEFAULT = -1;
	public static final int GWP_ASAN_NEVER = 0;
	public static final int GWP_ASAN_ALWAYS = 1;
	public static final int MEMTAG_DEFAULT = -1;
	public static final int MEMTAG_OFF = 0;
	public static final int MEMTAG_ASYNC = 1;
	public static final int MEMTAG_SYNC = 2;
	public static final int ZEROINIT_DEFAULT = -1;
	public static final int ZEROINIT_DISABLED = 0;
	public static final int ZEROINIT_ENABLED = 1;
	public static final int RAW_EXTERNAL_STORAGE_ACCESS_DEFAULT = 0;
	public static final int RAW_EXTERNAL_STORAGE_ACCESS_REQUESTED = 1;
	public static final int RAW_EXTERNAL_STORAGE_ACCESS_NOT_REQUESTED = 2;

	public static final int CATEGORY_UNDEFINED = -1;
	public static final int CATEGORY_GAME = 0;
	public static final int CATEGORY_AUDIO = 1;
	public static final int CATEGORY_VIDEO = 2;
	public static final int CATEGORY_IMAGE = 3;
	public static final int CATEGORY_SOCIAL = 4;
	public static final int CATEGORY_NEWS = 5;
	public static final int CATEGORY_MAPS = 6;
	public static final int CATEGORY_PRODUCTIVITY = 7;
	public static final int CATEGORY_ACCESSIBILITY = 8;
	public int category = CATEGORY_UNDEFINED;

	public static final int HIDDEN_API_ENFORCEMENT_DEFAULT = -1;
	public static final int HIDDEN_API_ENFORCEMENT_DISABLED = 0;
	public static final int HIDDEN_API_ENFORCEMENT_ENABLED = 2;
	private int mHiddenApiPolicy = HIDDEN_API_ENFORCEMENT_DEFAULT;

	/* AOSP getHiddenApiEnforcementPolicy(): an app that is neither signed with the platform key nor a system app
	 * is never allowed to use hidden APIs, so the policy is the one set, else ENABLED */
	public int getHiddenApiEnforcementPolicy() {
		if (mHiddenApiPolicy != HIDDEN_API_ENFORCEMENT_DEFAULT)
			return mHiddenApiPolicy;
		return HIDDEN_API_ENFORCEMENT_ENABLED;
	}

	public ApplicationInfo() {
		if (System.getProperty("os.arch").equals("aarch64")) {
			primaryCpuAbi = "arm64-v8a";
		} else if (System.getProperty("os.arch").equals("x86_64")) {
			primaryCpuAbi = "x86_64";
		}
	}

	public ApplicationInfo(ApplicationInfo orig) {
		super(orig);
		taskAffinity = orig.taskAffinity;
		permission = orig.permission;
		processName = orig.processName;
		className = orig.className;
		theme = orig.theme;
		localeConfigRes = orig.localeConfigRes;
		flags = orig.flags;
		privateFlags = orig.privateFlags;
		requiresSmallestWidthDp = orig.requiresSmallestWidthDp;
		compatibleWidthLimitDp = orig.compatibleWidthLimitDp;
		largestWidthLimitDp = orig.largestWidthLimitDp;
		sourceDir = orig.sourceDir;
		publicSourceDir = orig.publicSourceDir;
		nativeLibraryDir = orig.nativeLibraryDir;
		resourceDirs = orig.resourceDirs;
		seinfo = orig.seinfo;
		sharedLibraryFiles = orig.sharedLibraryFiles;
		dataDir = orig.dataDir;
		deviceProtectedDataDir = orig.deviceProtectedDataDir;
		primaryCpuAbi = orig.primaryCpuAbi;
		secondaryCpuAbi = orig.secondaryCpuAbi;
		uid = orig.uid;
		minSdkVersion = orig.minSdkVersion;
		targetSdkVersion = orig.targetSdkVersion;
		enabled = orig.enabled;
		enabledSetting = orig.enabledSetting;
		installLocation = orig.installLocation;
		manageSpaceActivityName = orig.manageSpaceActivityName;
		descriptionRes = orig.descriptionRes;
		uiOptions = orig.uiOptions;
		backupAgentName = orig.backupAgentName;
		storageUuid = orig.storageUuid;
		splitNames = orig.splitNames;
		seInfo = orig.seInfo;
		seInfoUser = orig.seInfoUser;
		deviceProtectedDataDir = orig.deviceProtectedDataDir;
		compileSdkVersion = orig.compileSdkVersion;
		compileSdkVersionCodename = orig.compileSdkVersionCodename;
		zygotePreloadName = orig.zygotePreloadName;
		appComponentFactory = orig.appComponentFactory;
		category = orig.category;
		mHiddenApiPolicy = orig.mHiddenApiPolicy;
	}

	public String toString() {
		return "ApplicationInfo{" + Integer.toHexString(System.identityHashCode(this)) + " " + packageName + "}";
	}

	/**
	 * Retrieve the textual description of the application.  This
	 * will call back on the given PackageManager to load the description from
	 * the application.
	 *
	 * @param pm A PackageManager from which the label can be loaded; usually
	 * the PackageManager from which you originally retrieved this item.
	 *
	 * @return Returns a CharSequence containing the application's description.
	 * If there is no description, null is returned.
	 */
	public CharSequence loadDescription(PackageManager pm) {
		if (descriptionRes != 0) {
			CharSequence label = pm.getText(packageName, descriptionRes, this);
			if (label != null) {
				return label;
			}
		}
		return null;
	}

	/**
	 * Disable compatibility mode
	 *
	 * @hide
	 */
	public void disableCompatibilityMode() {
		flags |= (FLAG_SUPPORTS_LARGE_SCREENS | FLAG_SUPPORTS_NORMAL_SCREENS | FLAG_SUPPORTS_SMALL_SCREENS | FLAG_RESIZEABLE_FOR_SCREENS | FLAG_SUPPORTS_SCREEN_DENSITIES | FLAG_SUPPORTS_XLARGE_SCREENS);
	}

	/**
	 * @hide
	 */
	@Override
	protected Drawable loadDefaultIcon(PackageManager pm) {
		/* AOSP ApplicationInfo.loadDefaultIcon(): the sd-card branch (FLAG_EXTERNAL_STORAGE) never applies,
		 * ATL installs no package there */
		return pm.getDefaultActivityIcon();
	}

	private boolean isPackageUnavailable(PackageManager pm) {
		try {
			return pm.getPackageInfo(packageName, 0) == null;
		} catch (NameNotFoundException ex) {
			return true;
		}
	}

	/**
	 * @hide
	 */
	@Override
	protected ApplicationInfo getApplicationInfo() {
		return this;
	}

	public String[] splitPublicSourceDirs = new String[0];

	/* AOSP ApplicationInfo.writeToParcel()/ApplicationInfo(Parcel)/CREATOR (Android 16): the fields ATL has, in AOSP order.
	 * Not parcelled (no such field or helper in ATL): if (dest.maybeWriteSquashed(this)) { ..., privateFlagsExt, if (storageUuid != null) { dest.write..., scanSourceDir, scanPublicSourceDir, SparseArray, secondaryNativeLibraryDir, nativeLibraryRootDir, nativeLibraryRootRequiresIsa, overlayPaths, sharedLibraryInfos, optionalSharedLibraryInfos, credentialProtectedDataDir, longVersionCode, fullBackupContent, dataExtractionRulesRes, crossProfile, networkSecurityConfigRes, targetSandboxVersion, classLoaderName, splitClassLoaderNames, appComponentFactory (ATL parcels no fields after targetSandboxVersion), iconRes, roundIconRes, hiddenUntilInstalled, gwpAsanMode, memtagMode, nativeHeapZeroInitialized, sForBoolean.parcel(requestRawExternal..., createTimestamp, if (mAppClassNamesByProcess == null) ..., allowCrossUidActivitySwitchFromBelow, mPageSizeAppCompatFlags, unalignedNativeLibraries, sForStringSet.parcel(mKnownActivityEm... */
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(android.os.Parcel dest, int parcelableFlags) {
		super.writeToParcel(dest, parcelableFlags);
		dest.writeString8(taskAffinity);
		dest.writeString8(permission);
		dest.writeString8(processName);
		dest.writeString8(className);
		dest.writeInt(theme);
		dest.writeInt(flags);
		dest.writeInt(privateFlags);
		dest.writeInt(requiresSmallestWidthDp);
		dest.writeInt(compatibleWidthLimitDp);
		dest.writeInt(largestWidthLimitDp);
		dest.writeString8(sourceDir);
		dest.writeString8(publicSourceDir);
		dest.writeString8Array(splitNames);
		dest.writeString8Array(splitSourceDirs);
		dest.writeString8Array(splitPublicSourceDirs);
		dest.writeString8(nativeLibraryDir);
		dest.writeString8(primaryCpuAbi);
		dest.writeString8(secondaryCpuAbi);
		dest.writeString8Array(resourceDirs);
		dest.writeString8(seInfo);
		dest.writeString8(seInfoUser);
		dest.writeString8Array(sharedLibraryFiles);
		dest.writeString8(dataDir);
		dest.writeString8(deviceProtectedDataDir);
		dest.writeInt(uid);
		dest.writeInt(minSdkVersion);
		dest.writeInt(targetSdkVersion);
		dest.writeInt(enabled ? 1 : 0);
		dest.writeInt(enabledSetting);
		dest.writeInt(installLocation);
		dest.writeString8(manageSpaceActivityName);
		dest.writeString8(backupAgentName);
		dest.writeInt(descriptionRes);
		dest.writeInt(uiOptions);
		dest.writeInt(category);
		dest.writeInt(compileSdkVersion);
		dest.writeString8(compileSdkVersionCodename);
		dest.writeInt(mHiddenApiPolicy);
		dest.writeString8(zygotePreloadName);
		dest.writeInt(localeConfigRes);
	}

	private ApplicationInfo(android.os.Parcel source) {
		super(source);
		taskAffinity = source.readString8();
		permission = source.readString8();
		processName = source.readString8();
		className = source.readString8();
		theme = source.readInt();
		flags = source.readInt();
		privateFlags = source.readInt();
		requiresSmallestWidthDp = source.readInt();
		compatibleWidthLimitDp = source.readInt();
		largestWidthLimitDp = source.readInt();
		sourceDir = source.readString8();
		publicSourceDir = source.readString8();
		splitNames = source.createString8Array();
		splitSourceDirs = source.createString8Array();
		splitPublicSourceDirs = source.createString8Array();
		nativeLibraryDir = source.readString8();
		primaryCpuAbi = source.readString8();
		secondaryCpuAbi = source.readString8();
		resourceDirs = source.createString8Array();
		seInfo = source.readString8();
		seInfoUser = source.readString8();
		sharedLibraryFiles = source.createString8Array();
		dataDir = source.readString8();
		deviceProtectedDataDir = source.readString8();
		uid = source.readInt();
		minSdkVersion = source.readInt();
		targetSdkVersion = source.readInt();
		enabled = source.readInt() != 0;
		enabledSetting = source.readInt();
		installLocation = source.readInt();
		manageSpaceActivityName = source.readString8();
		backupAgentName = source.readString8();
		descriptionRes = source.readInt();
		uiOptions = source.readInt();
		category = source.readInt();
		compileSdkVersion = source.readInt();
		compileSdkVersionCodename = source.readString8();
		mHiddenApiPolicy = source.readInt();
		zygotePreloadName = source.readString8();
		localeConfigRes = source.readInt();
	}

	public static final android.os.Parcelable.Creator<ApplicationInfo> CREATOR = new android.os.Parcelable.Creator<ApplicationInfo>() {
		public ApplicationInfo createFromParcel(android.os.Parcel source) {
			return new ApplicationInfo(source);
		}

		public ApplicationInfo[] newArray(int size) {
			return new ApplicationInfo[size];
		}
	};
	/* AOSP ApplicationInfo.getCategoryTitle(): the localized name of the category, null for an unknown one */
	public static CharSequence getCategoryTitle(Context context, int category) {
		switch (category) {
		case CATEGORY_GAME: return context.getText(com.android.internal.R.string.app_category_game);
		case CATEGORY_AUDIO: return context.getText(com.android.internal.R.string.app_category_audio);
		case CATEGORY_VIDEO: return context.getText(com.android.internal.R.string.app_category_video);
		case CATEGORY_IMAGE: return context.getText(com.android.internal.R.string.app_category_image);
		case CATEGORY_SOCIAL: return context.getText(com.android.internal.R.string.app_category_social);
		case CATEGORY_NEWS: return context.getText(com.android.internal.R.string.app_category_news);
		case CATEGORY_MAPS: return context.getText(com.android.internal.R.string.app_category_maps);
		case CATEGORY_PRODUCTIVITY: return context.getText(com.android.internal.R.string.app_category_productivity);
		case CATEGORY_ACCESSIBILITY: return context.getText(com.android.internal.R.string.app_category_accessibility);
		default: return null;
		}
	}
}
