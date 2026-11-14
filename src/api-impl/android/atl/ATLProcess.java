package android.atl;

import android.app.Activity;
import android.atl.annotation.Export;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageParser;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Parcel;
import android.os.Process;
import android.os.RemoteException;
import android.util.Slog;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * The processes of the app (android:process). The main process starts a process for a service whose process
 * name differs from its own, like ActivityManagerService's startProcessLocked(); the new process is this
 * executable again (ATL_PROCESS_NAME, see main.c), connected to the main process by an ATLBinderConnection.
 * Its context object (AppThread, like IApplicationThread) creates and binds the services there.
 */
public final class ATLProcess {
	private static final String TAG = "ATLProcess";

	/* set in a process that the main process started */
	private static final String process_name = System.getenv("ATL_PROCESS_NAME");
	private static final boolean isolated = System.getenv("ATL_PROCESS_ISOLATED") != null;

	/* AppThread transactions */
	private static final int BIND_SERVICE = IBinder.FIRST_CALL_TRANSACTION;
	private static final int UNBIND_SERVICE = IBinder.FIRST_CALL_TRANSACTION + 1;
	private static final int START_SERVICE = IBinder.FIRST_CALL_TRANSACTION + 2;
	private static final int STOP_SERVICE = IBinder.FIRST_CALL_TRANSACTION + 3;
	/* to the main process: this process has no components left and would end */
	private static final int MAY_EXIT = IBinder.FIRST_CALL_TRANSACTION;
	/* both ways: the Transport of a content provider of the called process */
	private static final int ACQUIRE_PROVIDER = IBinder.FIRST_CALL_TRANSACTION + 10;
	/* both ways: start an activity of the called process in its window */
	private static final int START_ACTIVITY = IBinder.FIRST_CALL_TRANSACTION + 11;
	/* to the main process: bind and unbind one of its services, or one of a third process, for another process */
	private static final int BIND_MAIN_SERVICE = IBinder.FIRST_CALL_TRANSACTION + 12;
	private static final int UNBIND_MAIN_SERVICE = IBinder.FIRST_CALL_TRANSACTION + 13;
	/* to the main process: Process.killProcess() of its pid from another process */
	private static final int RESTART_MAIN = IBinder.FIRST_CALL_TRANSACTION + 14;
	/* to the caller of START_ACTIVITY: the activity finished, with its result */
	private static final int ACTIVITY_FINISHED = IBinder.FIRST_CALL_TRANSACTION;
	/* to the caller of START_ACTIVITY: the activity left its task for the pinned task (picture-in-picture), or
	 * came back on top of it */
	private static final int ACTIVITY_COVERS = IBinder.FIRST_CALL_TRANSACTION + 1;

	/** whether this is a process the main process started, not the main process */
	public static boolean isSecondary() {
		return process_name != null;
	}

	/** Process.isIsolated(): a process of an isolated service (android:isolatedProcess) */
	public static boolean isIsolated() {
		return isolated;
	}

	/** Application.getProcessName() */
	public static String myProcessName() {
		return process_name != null ? process_name : mainProcessName();
	}

	public static String mainProcessName() {
		ApplicationInfo info = ATLLoadedApp.getPrimaryApplication().pkg.applicationInfo;
		return info.processName != null ? info.processName : info.packageName;
	}

	/* ---------------------------------------------------------------- main process */

	private static final class ProcessRecord implements ATLBinderConnection.DeathListener {
		final String name;
		/* the process in the table: an isolated service gets a process of its own (per instance name) */
		final String key;
		final boolean isolated;
		/* starting the process and the transactions to it, in the order of the calls */
		final ExecutorService worker = Executors.newSingleThreadExecutor();
		ATLBinderConnection connection;
		IBinder appThread;
		final HashMap<ServiceConnection, Intent> bindings = new HashMap<>();
		/* the binder each bound service returned (onBind runs once per service, like AOSP per intent) */
		final HashMap<String, IBinder> service_binders = new HashMap<>();
		/* calls queued for the worker; while there are any, the process must not end */
		int pending;
		/* the activities started there that did not finish yet */
		final ArrayList<ResultTarget> activities = new ArrayList<>();
		/* the services of the main process it bound, with the connection that binds them here */
		final HashMap<String, ServiceConnection> bound_by_it = new HashMap<>();

		ProcessRecord(String key, String name, boolean isolated) {
			this.key = key;
			this.name = name;
			this.isolated = isolated;
		}

		/* the process ended (or crashed): its bindings are lost, like ActiveServices.killServicesLocked() */
		@Override
		public void onConnectionDied(ATLBinderConnection connection) {
			/* a process from the app's zygote is the zygote's child, which reaps it and logs its status */
			int status = ATLBinderConnection.nativeWaitPid(connection.peerPid);
			Slog.i(TAG, "process " + name + " (pid " + connection.peerPid + ") has died" + (status >= 0 ? ", status " + status : ""));
			final ArrayList<Map.Entry<ServiceConnection, Intent>> lost;
			final ArrayList<ResultTarget> lost_activities;
			synchronized (processes) {
				if (processes.get(key) == this)
					processes.remove(key);
				lost = new ArrayList<>(bindings.entrySet());
				bindings.clear();
				lost_activities = new ArrayList<>(activities);
				activities.clear();
			}
			worker.shutdown();
			/* like ActivityRecord.finishIfPossible() for the activities of a dead process: RESULT_CANCELED */
			for (ResultTarget target : lost_activities)
				target.finished(Activity.RESULT_CANCELED, null);
			/* its bindings of services here go with it (ActiveServices.killServicesLocked()) */
			final ArrayList<ServiceConnection> unbind;
			synchronized (bound_by_it) {
				unbind = new ArrayList<>(bound_by_it.values());
				bound_by_it.clear();
			}
			for (ServiceConnection conn : unbind)
				unbindConnection(conn);
			new Handler(Looper.getMainLooper()).post(new Runnable() {
				@Override
				public void run() {
					for (Map.Entry<ServiceConnection, Intent> binding : lost)
						binding.getKey().onServiceDisconnected(binding.getValue().getComponent());
				}
			});
		}
	}

	private static final HashMap<String, ProcessRecord> processes = new HashMap<>();

	/** the pids and names of the app's processes, for ActivityManager.getRunningAppProcesses() */
	public static Map<Integer, String> runningProcesses() {
		HashMap<Integer, String> ret = new HashMap<>();
		ret.put(Process.myPid(), myProcessName());
		synchronized (processes) {
			for (ProcessRecord r : processes.values()) {
				if (r.connection != null && !r.connection.isDead())
					ret.put(r.connection.peerPid, r.name);
			}
		}
		return ret;
	}

	/** ActiveServices.bindServiceLocked() for a service in another process of the app */
	public static boolean bindService(final Intent intent, final ServiceConnection conn, String processName, String instanceName) {
		if (isSecondary()) {
			/* a process the main process started does not start processes itself: the main process binds the
			 * service (and starts its process) for it */
			if (instanceName == null)
				return bindMainService(intent, conn, processName);
			Slog.w(TAG, "bindService: " + intent.getComponent() + " (instance " + instanceName + ") runs in process " + processName + ", not supported from process " + process_name);
			return false;
		}
		final ProcessRecord r;
		synchronized (processes) {
			r = getProcessRecord(processName, intent, instanceName);
			r.bindings.put(conn, intent);
		}
		submit(r, new Runnable() {
			@Override
			public void run() {
				final ComponentName component = intent.getComponent();
				final IBinder binder;
				try {
					binder = bindInProcess(r, intent);
				} catch (RemoteException e) {
					Slog.w(TAG, "bindService: " + component + " in process " + r.name + " failed: " + e);
					return;
				}
				new Handler(Looper.getMainLooper()).post(new Runnable() {
					@Override
					public void run() {
						synchronized (processes) {
							if (r.bindings.get(conn) != intent)
								return; // unbound or the process died meanwhile
						}
						if (binder != null)
							conn.onServiceConnected(component, binder);
						else
							conn.onNullBinding(component);
					}
				});
			}
		});
		return true;
	}

	/* the record of a process of the app, a new one (the process starts with its first call) if it does not run */
	private static ProcessRecord getProcessRecord(String processName, Intent intent, String instanceName) {
		boolean isolated = intent != null && ATLLoadedApp.getPrimaryApplication().isIsolatedService(intent.getComponent().getClassName());
		String key = isolated ? processName + "#" + intent.getComponent().getClassName() + "#" + (instanceName != null ? instanceName : "") : processName;
		ProcessRecord r = processes.get(key);
		/* a process that died but whose end is not handled yet is not used again */
		if (r == null || (r.connection != null && r.connection.isDead())) {
			r = new ProcessRecord(key, processName, isolated);
			processes.put(key, r);
		}
		return r;
	}

	/* runs a call to the process on its worker; the process stays while calls are pending (see MAY_EXIT) */
	private static void submit(final ProcessRecord r, final Runnable call) {
		synchronized (processes) {
			r.pending++;
		}
		r.worker.execute(new Runnable() {
			@Override
			public void run() {
				try {
					call.run();
				} finally {
					synchronized (processes) {
						r.pending--;
					}
				}
			}
		});
	}

	/** ActiveServices.startServiceLocked() for a service in another process of the app */
	public static ComponentName startService(final Intent intent, String processName) {
		if (isSecondary()) {
			Slog.w(TAG, "startService: " + intent.getComponent() + " runs in process " + processName + ", not supported from process " + process_name);
			return null;
		}
		final ProcessRecord r;
		synchronized (processes) {
			r = getProcessRecord(processName, intent, null);
		}
		submit(r, new Runnable() {
			@Override
			public void run() {
				try {
					ensureStarted(r);
					Parcel data = Parcel.obtain();
					intent.writeToParcel(data, 0);
					r.appThread.transact(START_SERVICE, data, Parcel.obtain(), 0);
				} catch (RemoteException e) {
					Slog.w(TAG, "startService: " + intent.getComponent() + " in process " + r.name + " failed: " + e);
				}
			}
		});
		return intent.getComponent();
	}

	/** stopService() of a service in another process of the app: false if that process does not run */
	public static boolean stopService(final Intent intent, String processName) {
		final ProcessRecord r;
		synchronized (processes) {
			ProcessRecord found = null;
			for (ProcessRecord p : processes.values()) {
				if (p.name.equals(processName) && (!p.isolated || p.key.startsWith(processName + "#" + intent.getComponent().getClassName() + "#")))
					found = p;
			}
			r = found;
		}
		if (r == null)
			return false;
		submit(r, new Runnable() {
			@Override
			public void run() {
				if (r.appThread == null)
					return;
				Parcel data = Parcel.obtain();
				intent.writeToParcel(data, 0);
				try {
					r.appThread.transact(STOP_SERVICE, data, Parcel.obtain(), 0);
				} catch (RemoteException e) {
					Slog.w(TAG, "stopService: " + intent.getComponent() + ": " + e);
				}
			}
		});
		return true;
	}

	/** AMS.getContentProviderImpl(): the Transport of a provider of the app in another process, which is started
	 * if needed; null if that process has no such provider */
	public static IBinder acquireProvider(String processName, String authority) throws RemoteException {
		IBinder target;
		ProcessRecord r = null;
		if (isSecondary()) {
			if (!processName.equals(mainProcessName())) {
				Slog.w(TAG, "provider " + authority + " runs in process " + processName + ", not supported from process " + process_name);
				return null;
			}
			target = main_connection.getContextObject();
		} else {
			synchronized (processes) {
				r = getProcessRecord(processName, null, null);
				r.pending++;
			}
			try {
				ensureStarted(r);
			} catch (RemoteException e) {
				synchronized (processes) {
					r.pending--;
				}
				throw e;
			}
			target = r.appThread;
		}
		try {
			Parcel data = Parcel.obtain();
			Parcel reply = Parcel.obtain();
			data.writeString(authority);
			target.transact(ACQUIRE_PROVIDER, data, reply, 0);
			return reply.readStrongBinder();
		} finally {
			if (r != null) {
				synchronized (processes) {
					r.pending--;
				}
			}
		}
	}

	private static void ensureStarted(ProcessRecord r) throws RemoteException {
		synchronized (r) {
			if (r.connection == null)
				startProcess(r);
		}
	}

	private static IBinder bindInProcess(ProcessRecord r, Intent intent) throws RemoteException {
		ensureStarted(r);
		String className = intent.getComponent().getClassName();
		synchronized (processes) {
			if (r.service_binders.containsKey(className))
				return r.service_binders.get(className);
		}
		Parcel data = Parcel.obtain();
		Parcel reply = Parcel.obtain();
		intent.writeToParcel(data, 0);
		r.appThread.transact(BIND_SERVICE, data, reply, 0);
		IBinder binder = reply.readStrongBinder();
		synchronized (processes) {
			r.service_binders.put(className, binder);
		}
		return binder;
	}

	private static void startProcess(ProcessRecord r) throws RemoteException {
		int[] sv = ATLBinderConnection.nativeSocketPair();
		if (sv == null)
			throw new RemoteException("socketpair failed");
		Slog.i(TAG, "starting process " + r.name);
		int pid = ATLBinderConnection.nativeSpawn(sv[1], r.name, r.isolated);
		closeFd(sv[1]);
		if (pid < 0) {
			closeFd(sv[0]);
			throw new RemoteException("cannot start process " + r.name + ": errno " + (-pid));
		}
		Slog.i(TAG, "Start proc " + pid + ":" + r.name);
		r.connection = new ATLBinderConnection(sv[0], pid, new MainThread(r), r);
		r.appThread = r.connection.getContextObject();
	}

	private static void closeFd(int fd) {
		java.io.FileDescriptor f = new java.io.FileDescriptor();
		f.setInt$(fd);
		try {
			android.system.Os.close(f);
		} catch (android.system.ErrnoException ignored) {
		}
	}

	/** unbindService() of a connection to a service in another process; false if it is none */
	public static boolean unbindService(ServiceConnection conn) {
		if (isSecondary())
			return unbindMainService(conn);
		final ProcessRecord r;
		final Intent intent;
		final boolean last;
		synchronized (processes) {
			ProcessRecord found = null;
			for (ProcessRecord p : processes.values()) {
				if (p.bindings.containsKey(conn)) {
					found = p;
					break;
				}
			}
			if (found == null)
				return false;
			r = found;
			intent = r.bindings.remove(conn);
			String className = intent.getComponent().getClassName();
			boolean others = false;
			for (Intent i : r.bindings.values())
				others |= className.equals(i.getComponent().getClassName());
			last = !others;
			if (last)
				r.service_binders.remove(className);
		}
		if (last) {
			submit(r, new Runnable() {
				@Override
				public void run() {
					if (r.appThread == null)
						return;
					Parcel data = Parcel.obtain();
					intent.writeToParcel(data, 0);
					try {
						r.appThread.transact(UNBIND_SERVICE, data, Parcel.obtain(), 0);
					} catch (RemoteException e) {
						Slog.w(TAG, "unbindService: " + intent.getComponent() + ": " + e);
					}
				}
			});
		}
		return true;
	}

	/* the context object of the main process for one of the other processes */
	private static final class MainThread extends Binder {
		private final ProcessRecord r;

		MainThread(ProcessRecord r) {
			this.r = r;
		}

		@Override
		protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
			if (code == MAY_EXIT) {
				/* the process has no components left: it ends unless calls or activities to it are on the way; a later
				 * call starts a new process */
				boolean exit;
				synchronized (processes) {
					exit = r.pending == 0 && r.bindings.isEmpty() && r.activities.isEmpty();
					if (exit && processes.get(r.key) == r)
						processes.remove(r.key);
				}
				reply.writeInt(exit ? 1 : 0);
				return true;
			}
			if (code == ACQUIRE_PROVIDER) {
				reply.writeStrongBinder(ATLRemoteContentProvider.transportFor(data.readString()));
				return true;
			}
			if (code == START_ACTIVITY) {
				startHere(data);
				return true;
			}
			if (code == RESTART_MAIN) {
				boolean me = data.readInt() == Process.myPid();
				reply.writeInt(me ? 1 : 0);
				if (me) {
					Slog.i(TAG, "process " + r.name + " kills the main process: the app restarts (exit " + Process.EXIT_RESTART + ")");
					new Handler(Looper.getMainLooper()).post(new Runnable() {
						@Override
						public void run() {
							System.exit(Process.EXIT_RESTART);
						}
					});
				}
				return true;
			}
			if (code == BIND_MAIN_SERVICE) {
				Intent intent = Intent.CREATOR.createFromParcel(data);
				String processName = data.readString();
				/* a service of a third process: the binder returned from there is forwarded through this process */
				reply.writeStrongBinder(bindHere(intent, processName.equals(mainProcessName()) ? null : processName, r.bound_by_it));
				return true;
			}
			if (code == UNBIND_MAIN_SERVICE) {
				unbindHere(Intent.CREATOR.createFromParcel(data), r.bound_by_it);
				return true;
			}
			return super.onTransact(code, data, reply, flags);
		}
	}

	/* ---------------------------------------------------------------- activities */

	/** the process an activity of the app runs in (android:process) when it is another one than this, else
	 * null. ProcessPhoenix's activity stays in this process: it restarts the app by killing the process that
	 * started it, which ATL does with exit code EXIT_RESTART (Process.killProcess()). */
	public static String activityProcess(String className) {
		if (className.startsWith("com.jakewharton.processphoenix."))
			return null;
		for (PackageParser.Activity a : ATLLoadedApp.getPrimaryApplication().pkg.activities) {
			if (a.className.equals(className)) {
				String p = a.info.processName;
				return p != null && !p.equals(myProcessName()) ? p : null;
			}
		}
		return null;
	}

	/** what the starter of an activity in another process gets, on the main thread, once it finished (with its
	 * result) or its process ended (RESULT_CANCELED) */
	public interface ActivityFinished {
		void onActivityFinished(int resultCode, Intent data);

		/** the activity left the starter's task for the pinned task (false), or came back on top of it (true) */
		void onCovering(boolean covers);
	}

	/* the starter's end of an activity in another process (AOSP: the ActivityRecord's resultTo) */
	private static final class ResultTarget extends Binder {
		private ActivityFinished callback;
		private final ProcessRecord record;

		ResultTarget(ActivityFinished callback, ProcessRecord record) {
			this.callback = callback;
			this.record = record;
		}

		void finished(final int resultCode, final Intent data) {
			final ActivityFinished cb;
			synchronized (this) {
				cb = callback;
				callback = null;
			}
			if (cb == null)
				return;
			if (record != null) {
				synchronized (processes) {
					record.activities.remove(this);
				}
			}
			new Handler(Looper.getMainLooper()).post(new Runnable() {
				@Override
				public void run() {
					cb.onActivityFinished(resultCode, data);
				}
			});
		}

		void covers(final boolean covers) {
			final ActivityFinished cb;
			synchronized (this) {
				cb = callback;
			}
			if (cb == null)
				return;
			new Handler(Looper.getMainLooper()).post(new Runnable() {
				@Override
				public void run() {
					synchronized (ResultTarget.this) {
						if (callback == null)
							return; // finished meanwhile
					}
					cb.onCovering(covers);
				}
			});
		}

		@Override
		protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
			if (code == ACTIVITY_COVERS) {
				covers(data.readInt() != 0);
				return true;
			}
			if (code != ACTIVITY_FINISHED)
				return super.onTransact(code, data, reply, flags);
			int resultCode = data.readInt();
			finished(resultCode, data.readInt() != 0 ? Intent.CREATOR.createFromParcel(data) : null);
			return true;
		}
	}

	/** ActivityStarter for an activity of the app in another process: it comes up in that process's window,
	 * which opens over the window of this one (transient for it); `wantsResult` like a request code >= 0 */
	public static void startActivity(final Intent intent, String processName, final boolean wantsResult, ActivityFinished finished) {
		/* the handle of this process's window for the other one; it comes from the compositor */
		Activity.nativeExportWindow();
		if (isSecondary()) {
			final ResultTarget target = new ResultTarget(finished, null);
			if (!processName.equals(mainProcessName())) {
				Slog.w(TAG, "startActivity: " + intent.getComponent() + " runs in process " + processName + ", not supported from process " + process_name);
				target.finished(Activity.RESULT_CANCELED, null);
				return;
			}
			worker_here.execute(new Runnable() {
				@Override
				public void run() {
					try {
						sendStart(main_connection.getContextObject(), intent, wantsResult, target);
					} catch (RemoteException e) {
						target.finished(Activity.RESULT_CANCELED, null);
					}
				}
			});
			return;
		}
		final ProcessRecord r;
		final ResultTarget target;
		synchronized (processes) {
			r = getProcessRecord(processName, null, null);
			target = new ResultTarget(finished, r);
			r.activities.add(target);
		}
		submit(r, new Runnable() {
			@Override
			public void run() {
				try {
					ensureStarted(r);
					sendStart(r.appThread, intent, wantsResult, target);
				} catch (RemoteException e) {
					Slog.w(TAG, "startActivity: " + intent.getComponent() + " in process " + r.name + " failed: " + e);
					target.finished(Activity.RESULT_CANCELED, null);
				}
			}
		});
	}

	private static void sendStart(IBinder to, Intent intent, boolean wantsResult, ResultTarget target) throws RemoteException {
		Parcel data = Parcel.obtain();
		intent.writeToParcel(data, 0);
		data.writeInt(wantsResult ? 1 : 0);
		data.writeStrongBinder(target);
		data.writeString(Activity.nativeWaitWindowHandle(2000));
		to.transact(START_ACTIVITY, data, Parcel.obtain(), 0);
	}

	/** the end of an activity that another process started here: its result goes back there */
	public static final class RemoteCaller {
		private IBinder resultTo;
		public final boolean wantsResult;

		RemoteCaller(IBinder resultTo, boolean wantsResult) {
			this.resultTo = resultTo;
			this.wantsResult = wantsResult;
		}

		/** the activity entered picture-in-picture as the only one of this process's window, so it no longer
		 * covers the starter (false), or it came back from there (true) */
		public void covers(boolean covers) {
			IBinder to;
			synchronized (this) {
				to = resultTo;
			}
			if (to == null)
				return;
			Parcel p = Parcel.obtain();
			p.writeInt(covers ? 1 : 0);
			try {
				to.transact(ACTIVITY_COVERS, p, Parcel.obtain(), 0);
			} catch (RemoteException e) {
				Slog.w(TAG, "the starter of an activity is gone: " + e);
			}
		}

		/** Activity.finish(), or its end without one (RESULT_CANCELED); only the first call counts */
		public void finished(int resultCode, Intent data) {
			IBinder to;
			synchronized (this) {
				to = resultTo;
				resultTo = null;
			}
			if (to == null)
				return;
			Parcel p = Parcel.obtain();
			p.writeInt(wantsResult ? resultCode : Activity.RESULT_CANCELED);
			if (wantsResult && data != null) {
				p.writeInt(1);
				data.writeToParcel(p, 0);
			} else {
				p.writeInt(0);
			}
			try {
				/* not one-way: the starter has it before this process may end (MAY_EXIT) */
				to.transact(ACTIVITY_FINISHED, p, Parcel.obtain(), 0);
			} catch (RemoteException e) {
				Slog.w(TAG, "the starter of an activity is gone: " + e);
			}
		}
	}

	/* ActivityThread.handleLaunchActivity() for an activity another process started: it comes up in the window
	 * of this process, which opens over the starter's */
	private static void startHere(Parcel data) {
		final Intent intent = Intent.CREATOR.createFromParcel(data);
		boolean wantsResult = data.readInt() != 0;
		final RemoteCaller caller = new RemoteCaller(data.readStrongBinder(), wantsResult);
		final String parent = data.readString();
		new Handler(Looper.getMainLooper()).post(new Runnable() {
			@Override
			public void run() {
				Activity.nativeShowWindowFor(parent, ATLLoadedApp.getPrimaryApplication().pkg.packageName);
				try {
					Activity.startFromOtherProcess(intent, caller);
				} catch (ReflectiveOperationException e) {
					Slog.w(TAG, "cannot start " + intent.getComponent() + ": " + e);
					caller.finished(Activity.RESULT_CANCELED, null);
				}
			}
		});
	}

	/* ---------------------------------------------------------------- secondary process */

	private static ATLBinderConnection main_connection;

	/** Process.killProcess(pid) in this process: true if pid is the main process, which then ends with
	 * EXIT_RESTART (on Android a restart activity kills it this way and starts the app anew) */
	public static boolean restartMainProcess(int pid) {
		Parcel data = Parcel.obtain();
		Parcel reply = Parcel.obtain();
		data.writeInt(pid);
		try {
			main_connection.getContextObject().transact(RESTART_MAIN, data, reply, 0);
		} catch (RemoteException e) {
			return false;
		}
		return reply.readInt() == 1;
	}

	/* the calls of this process to the main process that do not wait for their answer */
	private static final ExecutorService worker_here = Executors.newSingleThreadExecutor();

	/* the services of the main process this process bound */
	private static final HashMap<ServiceConnection, Intent> main_bindings = new HashMap<>();

	/* bindService() of a service of the main process or of a third process: the main process binds it for this one */
	private static boolean bindMainService(final Intent intent, final ServiceConnection conn, final String processName) {
		synchronized (main_bindings) {
			main_bindings.put(conn, intent);
		}
		worker_here.execute(new Runnable() {
			@Override
			public void run() {
				final IBinder binder;
				try {
					Parcel data = Parcel.obtain();
					Parcel reply = Parcel.obtain();
					intent.writeToParcel(data, 0);
					data.writeString(processName);
					main_connection.getContextObject().transact(BIND_MAIN_SERVICE, data, reply, 0);
					binder = reply.readStrongBinder();
				} catch (RemoteException e) {
					Slog.w(TAG, "bindService: " + intent.getComponent() + " in process " + processName + " failed: " + e);
					return;
				}
				new Handler(Looper.getMainLooper()).post(new Runnable() {
					@Override
					public void run() {
						synchronized (main_bindings) {
							if (main_bindings.get(conn) != intent)
								return; // unbound meanwhile
						}
						if (binder != null)
							conn.onServiceConnected(intent.getComponent(), binder);
						else
							conn.onNullBinding(intent.getComponent());
					}
				});
			}
		});
		return true;
	}

	private static boolean unbindMainService(ServiceConnection conn) {
		final Intent intent;
		synchronized (main_bindings) {
			intent = main_bindings.remove(conn);
		}
		if (intent == null)
			return false;
		worker_here.execute(new Runnable() {
			@Override
			public void run() {
				Parcel data = Parcel.obtain();
				intent.writeToParcel(data, 0);
				try {
					main_connection.getContextObject().transact(UNBIND_MAIN_SERVICE, data, Parcel.obtain(), 0);
				} catch (RemoteException e) {
					Slog.w(TAG, "unbindService: " + intent.getComponent() + ": " + e);
				}
			}
		});
		return true;
	}

	/* the services the main process bound here, with the connection that binds them in this process */
	private static final HashMap<String, ServiceConnection> bound_here = new HashMap<>();

	/* IApplicationThread of this process: the main process binds and unbinds the services here */
	private static final class AppThread extends Binder {
		@Override
		protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
			switch (code) {
				case BIND_SERVICE:
					reply.writeStrongBinder(bindHere(Intent.CREATOR.createFromParcel(data), null, bound_here));
					return true;
				case UNBIND_SERVICE:
					unbindHere(Intent.CREATOR.createFromParcel(data), bound_here);
					return true;
				case START_ACTIVITY:
					startHere(data);
					return true;
				case START_SERVICE:
					ATLLoadedApp.getPrimaryApplication().startOrBindService(Intent.CREATOR.createFromParcel(data), null);
					return true;
				case STOP_SERVICE:
					ATLLoadedApp.getPrimaryApplication().stopService(Intent.CREATOR.createFromParcel(data));
					return true;
				case ACQUIRE_PROVIDER:
					reply.writeStrongBinder(ATLRemoteContentProvider.transportFor(data.readString()));
					return true;
			}
			return super.onTransact(code, data, reply, flags);
		}
	}

	/* ActivityThread.handleBindService(): create the service if needed and return what its onBind() returns;
	 * `bound` keeps the connection that binds it here. With processName (main process only), the service runs in
	 * that process and is bound there. */
	private static IBinder bindHere(Intent intent, String processName, HashMap<String, ServiceConnection> bound) {
		final IBinder[] result = new IBinder[1];
		final CountDownLatch done = new CountDownLatch(1);
		ServiceConnection conn = new ServiceConnection() {
			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
				result[0] = service;
				done.countDown();
			}

			@Override
			public void onNullBinding(ComponentName name) {
				done.countDown();
			}
		};
		if (processName != null ? !bindService(intent, conn, processName, null)
		                        : ATLLoadedApp.getPrimaryApplication().startOrBindService(intent, conn, Context.BIND_AUTO_CREATE) == null)
			return null;
		synchronized (bound) {
			bound.put(intent.getComponent().getClassName(), conn);
		}
		try {
			if (!done.await(20, TimeUnit.SECONDS))
				Slog.w(TAG, "service " + intent.getComponent() + " did not bind within 20 s");
		} catch (InterruptedException e) {
		}
		return result[0];
	}

	private static void unbindHere(Intent intent, HashMap<String, ServiceConnection> bound) {
		ServiceConnection conn;
		synchronized (bound) {
			conn = bound.remove(intent.getComponent().getClassName());
		}
		if (conn != null)
			unbindConnection(conn);
	}

	/* a connection bindHere() made: to a service of another process or of this one */
	private static void unbindConnection(ServiceConnection conn) {
		if (!unbindService(conn))
			ATLLoadedApp.getPrimaryApplication().unbindService(conn);
	}

	/** the last service of this process is gone: it ends, like a process without components that AOSP kills */
	public static void onServicesGone() {
		/* a process whose provider another process uses stays (AOSP keeps it while the connection lasts), and one
		 * that shows activities */
		if (!isSecondary() || ATLRemoteContentProvider.hasTransports() || Activity.nativeActivityCount() > 0)
			return;
		Parcel reply = Parcel.obtain();
		try {
			main_connection.getContextObject().transact(MAY_EXIT, Parcel.obtain(), reply, 0);
		} catch (RemoteException e) {
			return; // the main process is gone, this one ends with the connection
		}
		if (reply.readInt() == 0) {
			/* calls from the main process are on the way: ask again once they are done */
			new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
				@Override
				public void run() {
					if (!ATLLoadedApp.getPrimaryApplication().hasRunningServices() && Activity.nativeActivityCount() == 0)
						onServicesGone();
				}
			}, 1000);
			return;
		}
		Slog.i(TAG, "process " + process_name + " has no services left, exiting");
		Runtime.getRuntime().halt(0);
	}

	/* main.c: the last activity of this process is gone (its window hid instead of closing): the process ends
	 * like one without components, unless it has services */
	@Export
	private static void onActivitiesGone() {
		if (!ATLLoadedApp.getPrimaryApplication().hasRunningServices())
			onServicesGone();
	}

	/* main.c, in a process the main process started: after Application.onCreate(), serve the main process */
	@Export
	private static void attachToMainProcess() {
		int fd = Integer.parseInt(System.getenv("ATL_IPC_FD"));
		Slog.i(TAG, "process " + process_name + " (pid " + Process.myPid() + ") attached to the main process");
		main_connection = new ATLBinderConnection(fd, 0, new AppThread(), new ATLBinderConnection.DeathListener() {
			@Override
			public void onConnectionDied(ATLBinderConnection connection) {
				Slog.i(TAG, "the main process has ended, process " + process_name + " ends too");
				Runtime.getRuntime().halt(0);
			}
		});
	}
}
