package android.content.pm;

import android.atl.ATLLoadedApp;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.util.Slog;
import android.util.SparseArray;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

/* AOSP core/java/android/content/pm/PackageInstaller.java (android14-release): the public API an app store uses
 * (sessions, splits, commit with status to an IntentSender, session callbacks).
 *
 * On Android PackageInstallerService keeps the sessions and the system's PackageInstaller app asks the user. ATL
 * keeps the sessions of this process, staged in the app's cache. A commit always needs the user, as on AOSP for an
 * installer without INSTALL_PACKAGES: the receiver gets STATUS_PENDING_USER_ACTION with an ACTION_CONFIRM_INSTALL
 * intent, and when the app starts that intent, ATL hands the staged APKs to the program in ATL_INSTALL_COMMAND
 * (e.g. the launcher that runs ATL), which asks the user and installs:
 *
 *   $ATL_INSTALL_COMMAND APK... (base and split APKs), environment ATL_INSTALLER_PACKAGE / ATL_INSTALLER_LABEL
 *   exit status = PackageInstaller.STATUS_* (0 installed, 3 aborted by the user, 7 incompatible, others failed),
 *   the last line on stderr = EXTRA_STATUS_MESSAGE
 *
 * Without ATL_INSTALL_COMMAND a single APK goes to "android-translation-layer --install" (as ACTION_INSTALL_PACKAGE). */
public class PackageInstaller {
	private static final String TAG = "PackageInstaller";

	public static final String ACTION_SESSION_DETAILS = "android.content.pm.action.SESSION_DETAILS";
	public static final String ACTION_SESSION_COMMITTED = "android.content.pm.action.SESSION_COMMITTED";
	public static final String ACTION_SESSION_UPDATED = "android.content.pm.action.SESSION_UPDATED";
	public static final String ACTION_CONFIRM_INSTALL = "android.content.pm.action.CONFIRM_INSTALL";
	public static final String EXTRA_SESSION_ID = "android.content.pm.extra.SESSION_ID";
	public static final String EXTRA_SESSION = "android.content.pm.extra.SESSION";
	public static final String EXTRA_PACKAGE_NAME = "android.content.pm.extra.PACKAGE_NAME";
	public static final String EXTRA_STATUS = "android.content.pm.extra.STATUS";
	public static final String EXTRA_STATUS_MESSAGE = "android.content.pm.extra.STATUS_MESSAGE";
	public static final String EXTRA_LEGACY_STATUS = "android.content.pm.extra.LEGACY_STATUS";

	public static final int STATUS_PENDING_USER_ACTION = -1;
	public static final int STATUS_SUCCESS = 0;
	public static final int STATUS_FAILURE = 1;
	public static final int STATUS_FAILURE_BLOCKED = 2;
	public static final int STATUS_FAILURE_ABORTED = 3;
	public static final int STATUS_FAILURE_INVALID = 4;
	public static final int STATUS_FAILURE_CONFLICT = 5;
	public static final int STATUS_FAILURE_STORAGE = 6;
	public static final int STATUS_FAILURE_INCOMPATIBLE = 7;
	public static final int STATUS_FAILURE_TIMEOUT = 8;

	public static final int PACKAGE_SOURCE_UNSPECIFIED = 0;
	public static final int PACKAGE_SOURCE_OTHER = 1;
	public static final int PACKAGE_SOURCE_STORE = 2;
	public static final int PACKAGE_SOURCE_LOCAL_FILE = 3;
	public static final int PACKAGE_SOURCE_DOWNLOADED_FILE = 4;

	/* the sessions of this process (AOSP: PackageInstallerService.mSessions) */
	private static final SparseArray<SessionState> sSessions = new SparseArray<>();
	private static final Random sRandom = new Random();
	private static final List<CallbackDelegate> sCallbacks = new ArrayList<>();

	private static final class SessionState {
		final int sessionId;
		final SessionParams params;
		final File stageDir;
		float progress;
		boolean sealed;
		boolean active;
		IntentSender statusReceiver;

		SessionState(int sessionId, SessionParams params, File stageDir) {
			this.sessionId = sessionId;
			this.params = params;
			this.stageDir = stageDir;
		}

		SessionInfo info() {
			SessionInfo info = new SessionInfo();
			info.sessionId = sessionId;
			info.installerPackageName = params.installerPackageName != null ? params.installerPackageName : ownPackage();
			info.progress = progress;
			info.sealed = sealed;
			info.active = active;
			info.mode = params.mode;
			info.sizeBytes = params.sizeBytes;
			info.appPackageName = params.appPackageName;
			info.appIcon = params.appIcon;
			info.appLabel = params.appLabel;
			info.installLocation = params.installLocation;
			info.originatingUri = params.originatingUri;
			info.originatingUid = params.originatingUid;
			info.referrerUri = params.referrerUri;
			info.installReason = params.installReason;
			info.packageSource = params.packageSource;
			info.requireUserAction = params.requireUserAction;
			return info;
		}
	}

	private static final class CallbackDelegate {
		final SessionCallback callback;
		final Handler handler;

		CallbackDelegate(SessionCallback callback, Handler handler) {
			this.callback = callback;
			this.handler = handler;
		}
	}

	private static String ownPackage() {
		return ATLLoadedApp.getPrimaryApplication().pkg.packageName;
	}

	private static Context appContext() {
		return ATLLoadedApp.getPrimaryApplication().getApplication();
	}

	private interface CallbackCall {
		void call(SessionCallback callback);
	}

	private static void notifyCallbacks(final CallbackCall call) {
		List<CallbackDelegate> delegates;
		synchronized (sCallbacks) {
			delegates = new ArrayList<>(sCallbacks);
		}
		for (final CallbackDelegate d : delegates) {
			d.handler.post(new Runnable() {
				@Override
				public void run() {
					call.call(d.callback);
				}
			});
		}
	}

	private static SessionState session(int sessionId) {
		synchronized (sSessions) {
			return sSessions.get(sessionId);
		}
	}

	public int createSession(SessionParams params) throws IOException {
		if (params.mode != SessionParams.MODE_FULL_INSTALL && params.mode != SessionParams.MODE_INHERIT_EXISTING)
			throw new IllegalArgumentException("Invalid install mode: " + params.mode);
		final int sessionId;
		SessionState state;
		synchronized (sSessions) {
			/* AOSP PackageInstallerService.allocateSessionIdLocked(): random positive ids */
			int id;
			do {
				id = sRandom.nextInt(Integer.MAX_VALUE - 1) + 1;
			} while (sSessions.get(id) != null);
			sessionId = id;
			File stageDir = new File(new File(appContext().getCacheDir(), "atl_install_sessions"), "vmdl" + sessionId + ".tmp");
			if (!stageDir.mkdirs() && !stageDir.isDirectory())
				throw new IOException("Failed to create session directory " + stageDir);
			state = new SessionState(sessionId, params.copy(), stageDir);
			sSessions.put(sessionId, state);
		}
		notifyCallbacks(new CallbackCall() {
			public void call(SessionCallback cb) { cb.onCreated(sessionId); }
		});
		return sessionId;
	}

	public Session openSession(int sessionId) throws IOException {
		SessionState state = session(sessionId);
		if (state == null)
			throw new SecurityException("Caller has no access to session " + sessionId);
		if (state.sealed)
			throw new SecurityException("Session " + sessionId + " is sealed");
		if (!state.active) {
			state.active = true;
			notifyCallbacks(new CallbackCall() {
				public void call(SessionCallback cb) { cb.onActiveChanged(sessionId, true); }
			});
		}
		return new Session(state);
	}

	public SessionInfo getSessionInfo(int sessionId) {
		SessionState state = session(sessionId);
		return state != null ? state.info() : null;
	}

	public List<SessionInfo> getAllSessions() {
		List<SessionInfo> list = new ArrayList<>();
		synchronized (sSessions) {
			for (int i = 0; i < sSessions.size(); i++)
				list.add(sSessions.valueAt(i).info());
		}
		return list;
	}

	public List<SessionInfo> getMySessions() {
		return getAllSessions();
	}

	public void abandonSession(int sessionId) {
		SessionState state = session(sessionId);
		if (state == null)
			throw new SecurityException("Caller has no access to session " + sessionId);
		destroy(state, false);
	}

	public void updateSessionAppIcon(int sessionId, Bitmap appIcon) {
		SessionState state = session(sessionId);
		if (state != null)
			state.params.appIcon = appIcon;
	}

	public void updateSessionAppLabel(int sessionId, CharSequence appLabel) {
		SessionState state = session(sessionId);
		if (state != null)
			state.params.appLabel = appLabel != null ? appLabel.toString() : null;
	}

	public void registerSessionCallback(SessionCallback callback) {
		registerSessionCallback(callback, new Handler(Looper.getMainLooper()));
	}

	public void registerSessionCallback(SessionCallback callback, Handler handler) {
		synchronized (sCallbacks) {
			sCallbacks.add(new CallbackDelegate(callback, handler != null ? handler : new Handler(Looper.getMainLooper())));
		}
	}

	public void unregisterSessionCallback(SessionCallback callback) {
		synchronized (sCallbacks) {
			for (int i = sCallbacks.size() - 1; i >= 0; i--)
				if (sCallbacks.get(i).callback == callback)
					sCallbacks.remove(i);
		}
	}

	private static void deleteRecursively(File f) {
		File[] children = f.listFiles();
		if (children != null)
			for (File c : children)
				deleteRecursively(c);
		f.delete();
	}

	private static void destroy(SessionState state, final boolean success) {
		synchronized (sSessions) {
			sSessions.remove(state.sessionId);
		}
		deleteRecursively(state.stageDir);
		final int sessionId = state.sessionId;
		notifyCallbacks(new CallbackCall() {
			public void call(SessionCallback cb) { cb.onFinished(sessionId, success); }
		});
	}

	/* AOSP PackageInstallerSession.sendOnPackageInstalled()/sendPendingUserActionIntent(): the fill-in intent
	 * for the IntentSender given to commit() */
	private static void sendStatus(SessionState state, int status, String message, Intent confirm) {
		Intent fillIn = new Intent();
		fillIn.putExtra(EXTRA_SESSION_ID, state.sessionId);
		fillIn.putExtra(EXTRA_STATUS, status);
		if (state.params.appPackageName != null)
			fillIn.putExtra(EXTRA_PACKAGE_NAME, state.params.appPackageName);
		if (message != null)
			fillIn.putExtra(EXTRA_STATUS_MESSAGE, message);
		if (confirm != null)
			fillIn.putExtra(Intent.EXTRA_INTENT, confirm);
		Slog.i(TAG, "session " + state.sessionId + " (" + state.params.appPackageName + "): status " + status);
		try {
			state.statusReceiver.sendIntent(appContext(), 0, fillIn, null, null);
		} catch (IntentSender.SendIntentException e) {
			Slog.w(TAG, "status for session " + state.sessionId + " not delivered: " + e);
		}
	}

	/* Called by startActivity() for ACTION_CONFIRM_INSTALL, which on AOSP opens the system's
	 * PackageInstallerActivity. Returns false for other intents. */
	public static boolean handleConfirmIntent(Intent intent) {
		if (intent == null || !ACTION_CONFIRM_INSTALL.equals(intent.getAction()))
			return false;
		final SessionState state = session(intent.getIntExtra(EXTRA_SESSION_ID, -1));
		if (state == null || !state.sealed) {
			Slog.w(TAG, "confirm intent for an unknown or open session: " + intent);
			return true;
		}
		new Thread(new Runnable() {
			@Override
			public void run() {
				install(state);
			}
		}, "PackageInstaller-" + state.sessionId).start();
		return true;
	}

	/* the base APK first, then the splits in name order (AOSP sorts nothing, but a launcher needs the base) */
	private static List<String> stagedApks(SessionState state) {
		File[] files = state.stageDir.listFiles();
		List<String> apks = new ArrayList<>();
		if (files == null)
			return apks;
		Arrays.sort(files);
		for (File f : files)
			if (f.isFile())
				apks.add(f.getAbsolutePath());
		return apks;
	}

	private static void install(final SessionState state) {
		List<String> apks = stagedApks(state);
		int status;
		String message = null;
		if (apks.isEmpty()) {
			status = STATUS_FAILURE_INVALID;
			message = "No APKs in session";
		} else {
			List<String> command = new ArrayList<>();
			String installCommand = System.getenv("ATL_INSTALL_COMMAND");
			if (installCommand != null && !installCommand.trim().isEmpty()) {
				command.addAll(Arrays.asList(installCommand.trim().split("\\s+")));
				command.addAll(apks);
			} else if (apks.size() == 1) {
				command.addAll(Arrays.asList("/usr/bin/env", "android-translation-layer", "--install", apks.get(0)));
			} else {
				command = null;
			}
			if (command == null) {
				status = STATUS_FAILURE_INCOMPATIBLE;
				message = "Split APKs need ATL_INSTALL_COMMAND";
			} else {
				String[] result = runLauncher(command);
				status = Integer.parseInt(result[0]);
				if (status != STATUS_SUCCESS)
					message = result[1];
			}
		}
		if (message != null)
			Slog.i(TAG, "session " + state.sessionId + ": " + message);
		/* the receiver runs on the main thread, as broadcasts do */
		final int finalStatus = status;
		final String finalMessage = message;
		new Handler(Looper.getMainLooper()).post(new Runnable() {
			@Override
			public void run() {
				sendStatus(state, finalStatus, finalMessage, null);
				destroy(state, finalStatus == STATUS_SUCCESS);
			}
		});
	}

	/* Runs the launcher's command (ATL_INSTALL_COMMAND, ATL_UNINSTALL_COMMAND) with the caller as installer:
	 * { status (exit status as PackageInstaller.STATUS_*), last line on stderr or the exception } */
	private static String[] runLauncher(List<String> command) {
		try {
			ProcessBuilder pb = new ProcessBuilder(command);
			Map<String, String> env = pb.environment();
			env.put("ATL_INSTALLER_PACKAGE", ownPackage());
			CharSequence label = appContext().getApplicationInfo().loadLabel(appContext().getPackageManager());
			env.put("ATL_INSTALLER_LABEL", label != null ? label.toString() : ownPackage());
			pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
			java.lang.Process p = pb.start();
			String lastLine = null;
			try (BufferedReader err = new BufferedReader(new InputStreamReader(p.getErrorStream()))) {
				String line;
				while ((line = err.readLine()) != null) {
					System.err.println(line);
					if (!line.trim().isEmpty())
						lastLine = line.trim();
				}
			}
			int exit = p.waitFor();
			int status = (exit >= STATUS_SUCCESS && exit <= STATUS_FAILURE_TIMEOUT) ? exit : STATUS_FAILURE;
			return new String[] {String.valueOf(status), lastLine};
		} catch (IOException | InterruptedException e) {
			return new String[] {String.valueOf(STATUS_FAILURE), e.toString()};
		}
	}

	private static List<String> launcherCommand(String variable, String packageName) {
		String command = System.getenv(variable);
		if (command == null || command.trim().isEmpty())
			return null;
		List<String> out = new ArrayList<>(Arrays.asList(command.trim().split("\\s+")));
		out.add(packageName);
		return out;
	}

	/* AOSP: PackageInstallerService.uninstall() through the system's UninstallerActivity, which asks the user.
	 * ATL hands the package to ATL_UNINSTALL_COMMAND (the launcher asks and uninstalls; exit status as for
	 * installs) and, like the system, tells the app with ACTION_PACKAGE_REMOVED. `done` gets the status. */
	private static void uninstallPackage(final String packageName, final java.util.function.IntConsumer done) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				List<String> command = launcherCommand("ATL_UNINSTALL_COMMAND", packageName);
				final int status;
				if (command == null || !android.atl.ATLLoadedAppManager.isInstalled(packageName)) {
					Slog.w(TAG, "cannot uninstall " + packageName + (command == null ? ": no ATL_UNINSTALL_COMMAND" : ": not installed"));
					status = STATUS_FAILURE;
				} else {
					status = Integer.parseInt(runLauncher(command)[0]);
				}
				Slog.i(TAG, "uninstall " + packageName + ": status " + status);
				new Handler(Looper.getMainLooper()).post(new Runnable() {
					@Override
					public void run() {
						if (status == STATUS_SUCCESS) {
							Intent removed = new Intent(Intent.ACTION_PACKAGE_REMOVED, Uri.fromParts("package", packageName, null));
							removed.putExtra(Intent.EXTRA_REPLACING, false);
							appContext().sendBroadcast(removed);
						}
						done.accept(status);
					}
				});
			}
		}, "PackageInstaller-uninstall").start();
	}

	public void uninstall(final String packageName, final IntentSender statusReceiver) {
		uninstallPackage(packageName, new java.util.function.IntConsumer() {
			@Override
			public void accept(int status) {
				if (statusReceiver == null)
					return;
				Intent fillIn = new Intent();
				fillIn.putExtra(EXTRA_PACKAGE_NAME, packageName);
				fillIn.putExtra(EXTRA_STATUS, status);
				try {
					statusReceiver.sendIntent(appContext(), 0, fillIn, null, null);
				} catch (IntentSender.SendIntentException e) {
					Slog.w(TAG, "uninstall status not delivered: " + e);
				}
			}
		});
	}

	/* package:<name> and market: intents (AOSP: the system's UninstallerActivity and Settings' app info; the store
	 * app that has a filter for market:). None of them goes to the desktop's «Open with». Returns false for other
	 * intents; `result` (may be null) gets an activity result: RESULT_OK, RESULT_CANCELED, or 1
	 * (RESULT_FIRST_USER, failed) like UninstallerActivity with EXTRA_RETURN_RESULT. */
	public static boolean handlePackageIntent(Context context, Intent intent, final java.util.function.IntConsumer result) {
		Uri data = intent != null ? intent.getData() : null;
		if (data == null)
			return false;
		String action = intent.getAction();
		if ("package".equals(data.getScheme())) {
			String packageName = data.getSchemeSpecificPart();
			if (Intent.ACTION_DELETE.equals(action) || Intent.ACTION_UNINSTALL_PACKAGE.equals(action)) {
				uninstallPackage(packageName, new java.util.function.IntConsumer() {
					@Override
					public void accept(int status) {
						if (result != null)
							result.accept(status == STATUS_SUCCESS ? -1 : status == STATUS_FAILURE_ABORTED ? 0 : 1);
					}
				});
			} else if ("android.settings.APPLICATION_DETAILS_SETTINGS".equals(action)) {
				List<String> command = launcherCommand("ATL_APP_DETAILS_COMMAND", packageName);
				try {
					if (command != null)
						new ProcessBuilder(command).inheritIO().start();
				} catch (IOException e) {
					Slog.w(TAG, "cannot show the details of " + packageName + ": " + e);
				}
			} else {
				Slog.w(TAG, "not handled: " + intent);
			}
			return true;
		}
		if ("market".equals(data.getScheme())) {
			String store = marketApp(intent);
			if (store == null)
				throw new android.content.ActivityNotFoundException("No Activity found to handle " + intent);
			android.atl.ATLLoadedAppManager.startInstalledApp(context.getPackageName(), store, data.toString());
			return true;
		}
		return false;
	}

	/* the installed app (not the caller) with an activity whose filter takes the market: link, e.g. Aurora Store */
	private static String marketApp(Intent intent) {
		String[] files = android.atl.ATLPaths.installed_apks_dir.list();
		if (files == null)
			return null;
		Arrays.sort(files);
		for (String file : files) {
			if (!file.endsWith(".apk"))
				continue;
			String name = file.substring(0, file.length() - 4);
			if (name.equals(ownPackage()))
				continue;
			PackageParser.Package pkg = android.atl.ATLLoadedAppManager.getPackageFromPackageName(name);
			if (pkg == null)
				continue;
			for (PackageParser.Activity activity : pkg.activities)
				for (PackageParser.IntentInfo filter : activity.intents)
					if (filter.hasCategory(Intent.CATEGORY_DEFAULT) &&
					    filter.match(intent.getAction(), null, "market", intent.getData(), intent.getCategories(), TAG) > 0)
						return name;
		}
		return null;
	}

	public static class Session implements java.io.Closeable {
		private final SessionState mState;

		private Session(SessionState state) {
			mState = state;
		}

		private File file(String name) {
			/* AOSP PackageInstallerSession.doWriteInternal(): only a plain file name */
			if (name == null || name.isEmpty() || name.contains("/") || name.equals(".") || name.equals(".."))
				throw new IllegalArgumentException("Invalid name: " + name);
			return new File(mState.stageDir, name);
		}

		private void assertOpen() {
			if (mState.sealed)
				throw new SecurityException("Session " + mState.sessionId + " is sealed");
			if (session(mState.sessionId) == null)
				throw new SecurityException("Session " + mState.sessionId + " was abandoned");
		}

		public void setStagingProgress(float progress) {
			mState.progress = progress;
			final int sessionId = mState.sessionId;
			final float p = progress;
			notifyCallbacks(new CallbackCall() {
				public void call(SessionCallback cb) { cb.onProgressChanged(sessionId, p); }
			});
		}

		public OutputStream openWrite(String name, long offsetBytes, long lengthBytes) throws IOException {
			assertOpen();
			FileOutputStream out = new FileOutputStream(file(name), offsetBytes > 0);
			if (offsetBytes > 0)
				out.getChannel().position(offsetBytes);
			return out;
		}

		public void fsync(OutputStream out) throws IOException {
			if (out instanceof FileOutputStream)
				((FileOutputStream)out).getFD().sync();
			else
				throw new IllegalArgumentException("Unrecognized stream");
		}

		public InputStream openRead(String name) throws IOException {
			return new FileInputStream(file(name));
		}

		public String[] getNames() throws IOException {
			String[] names = mState.stageDir.list();
			if (names == null)
				return new String[0];
			Arrays.sort(names);
			return names;
		}

		/* AOSP: only for MODE_INHERIT_EXISTING; ATL installs whole packages */
		public void removeSplit(String splitName) throws IOException {
			assertOpen();
			throw new IOException("Removing splits is not supported");
		}

		public void commit(IntentSender statusReceiver) {
			assertOpen();
			mState.sealed = true;
			mState.statusReceiver = statusReceiver;
			final SessionState state = mState;
			final Intent confirm = new Intent(ACTION_CONFIRM_INSTALL);
			confirm.putExtra(EXTRA_SESSION_ID, state.sessionId);
			/* as from the system server, the status arrives after commit() has returned */
			new Handler(Looper.getMainLooper()).post(new Runnable() {
				@Override
				public void run() {
					sendStatus(state, STATUS_PENDING_USER_ACTION, null, confirm);
				}
			});
		}

		public void abandon() {
			if (session(mState.sessionId) != null)
				destroy(mState, false);
		}

		@Override
		public void close() {
			if (mState.active) {
				mState.active = false;
				final int sessionId = mState.sessionId;
				notifyCallbacks(new CallbackCall() {
					public void call(SessionCallback cb) { cb.onActiveChanged(sessionId, false); }
				});
			}
		}
	}

	public static class SessionParams {
		public static final int MODE_INVALID = -1;
		public static final int MODE_FULL_INSTALL = 1;
		public static final int MODE_INHERIT_EXISTING = 2;
		public static final int UID_UNKNOWN = -1;
		public static final int USER_ACTION_UNSPECIFIED = 0;
		public static final int USER_ACTION_REQUIRED = 1;
		public static final int USER_ACTION_NOT_REQUIRED = 2;

		public int mode = MODE_INVALID;
		public int installLocation = PackageInfo.INSTALL_LOCATION_INTERNAL_ONLY;
		public int installReason = PackageManager.INSTALL_REASON_UNKNOWN;
		public long sizeBytes = -1;
		public String appPackageName;
		public Bitmap appIcon;
		public String appLabel;
		public Uri originatingUri;
		public int originatingUid = UID_UNKNOWN;
		public Uri referrerUri;
		public String installerPackageName;
		public int packageSource = PACKAGE_SOURCE_UNSPECIFIED;
		public int requireUserAction = USER_ACTION_UNSPECIFIED;
		public boolean requestUpdateOwnership;
		public boolean applicationEnabledSettingPersistent;

		public SessionParams(int mode) {
			this.mode = mode;
		}

		public SessionParams copy() {
			SessionParams p = new SessionParams(mode);
			p.installLocation = installLocation;
			p.installReason = installReason;
			p.sizeBytes = sizeBytes;
			p.appPackageName = appPackageName;
			p.appIcon = appIcon;
			p.appLabel = appLabel;
			p.originatingUri = originatingUri;
			p.originatingUid = originatingUid;
			p.referrerUri = referrerUri;
			p.installerPackageName = installerPackageName;
			p.packageSource = packageSource;
			p.requireUserAction = requireUserAction;
			p.requestUpdateOwnership = requestUpdateOwnership;
			p.applicationEnabledSettingPersistent = applicationEnabledSettingPersistent;
			return p;
		}

		public void setInstallLocation(int installLocation) { this.installLocation = installLocation; }
		public void setSize(long sizeBytes) { this.sizeBytes = sizeBytes; }
		public void setAppPackageName(String appPackageName) { this.appPackageName = appPackageName; }
		public void setAppIcon(Bitmap appIcon) { this.appIcon = appIcon; }
		public void setAppLabel(CharSequence appLabel) { this.appLabel = appLabel != null ? appLabel.toString() : null; }
		public void setOriginatingUri(Uri originatingUri) { this.originatingUri = originatingUri; }
		public void setOriginatingUid(int originatingUid) { this.originatingUid = originatingUid; }
		public void setReferrerUri(Uri referrerUri) { this.referrerUri = referrerUri; }
		public void setInstallReason(int installReason) { this.installReason = installReason; }
		public void setInstallerPackageName(String installerPackageName) { this.installerPackageName = installerPackageName; }
		public void setPackageSource(int packageSource) { this.packageSource = packageSource; }
		public void setRequireUserAction(int requireUserAction) {
			if (requireUserAction != USER_ACTION_UNSPECIFIED && requireUserAction != USER_ACTION_REQUIRED
			    && requireUserAction != USER_ACTION_NOT_REQUIRED)
				throw new IllegalArgumentException("requireUserAction set as invalid value of " + requireUserAction);
			this.requireUserAction = requireUserAction;
		}
		public void setRequestUpdateOwnership(boolean enable) { this.requestUpdateOwnership = enable; }
		public void setApplicationEnabledSettingPersistent() { this.applicationEnabledSettingPersistent = true; }
		public void setInstallScenario(int installScenario) {}
		public void setDontKillApp(boolean dontKillApp) {}
		public void setGrantedRuntimePermissions(String[] permissions) {}
		public void setWhitelistedRestrictedPermissions(java.util.Set<String> permissions) {}
		public void setAutoRevokePermissionsMode(boolean shouldAutoRevoke) {}
		public void setMultiPackage() {
			throw new UnsupportedOperationException("multi-package sessions are not supported");
		}
	}

	public static class SessionInfo {
		public static final int INVALID_ID = -1;

		public int sessionId;
		public String installerPackageName;
		public float progress;
		public boolean sealed;
		public boolean active;
		public int mode;
		public long sizeBytes;
		public String appPackageName;
		public Bitmap appIcon;
		public CharSequence appLabel;
		public int installLocation;
		public Uri originatingUri;
		public int originatingUid;
		public Uri referrerUri;
		public int installReason;
		public int packageSource;
		public int requireUserAction;

		public int getSessionId() { return sessionId; }
		public String getInstallerPackageName() { return installerPackageName; }
		public float getProgress() { return progress; }
		public boolean isActive() { return active; }
		public boolean isSealed() { return sealed; }
		public int getMode() { return mode; }
		public long getSize() { return sizeBytes; }
		public String getAppPackageName() { return appPackageName; }
		public Bitmap getAppIcon() { return appIcon; }
		public CharSequence getAppLabel() { return appLabel; }
		public int getInstallLocation() { return installLocation; }
		public Uri getOriginatingUri() { return originatingUri; }
		public int getOriginatingUid() { return originatingUid; }
		public Uri getReferrerUri() { return referrerUri; }
		public int getInstallReason() { return installReason; }
		public boolean isMultiPackage() { return false; }
		public boolean isStaged() { return false; }
		public boolean isCommitted() { return sealed; }
		public boolean hasParentSessionId() { return false; }
		public int getParentSessionId() { return INVALID_ID; }
		public int[] getChildSessionIds() { return new int[0]; }
		public int getRequireUserAction() { return requireUserAction; }
		public int getUserActionRequired() { return requireUserAction; }
		public android.os.UserHandle getUser() { return Process.myUserHandle(); }
		public Intent createDetailsIntent() {
			return new Intent(ACTION_SESSION_DETAILS).putExtra(EXTRA_SESSION_ID, sessionId);
		}
	}

	public abstract static class SessionCallback {
		public abstract void onCreated(int sessionId);

		public abstract void onBadgingChanged(int sessionId);

		public abstract void onActiveChanged(int sessionId, boolean active);

		public abstract void onProgressChanged(int sessionId, float progress);

		public abstract void onFinished(int sessionId, boolean success);
	}
}
