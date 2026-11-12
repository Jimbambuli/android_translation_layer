package android.atl;

import android.atl.annotation.Export;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
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

	/* AppThread transactions */
	private static final int BIND_SERVICE = IBinder.FIRST_CALL_TRANSACTION;
	private static final int UNBIND_SERVICE = IBinder.FIRST_CALL_TRANSACTION + 1;
	private static final int START_SERVICE = IBinder.FIRST_CALL_TRANSACTION + 2;
	private static final int STOP_SERVICE = IBinder.FIRST_CALL_TRANSACTION + 3;
	/* to the main process: this process has no components left and would end */
	private static final int MAY_EXIT = IBinder.FIRST_CALL_TRANSACTION;
	/* both ways: the Transport of a content provider of the called process */
	private static final int ACQUIRE_PROVIDER = IBinder.FIRST_CALL_TRANSACTION + 10;

	/** whether this is a process the main process started, not the main process */
	public static boolean isSecondary() {
		return process_name != null;
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
		/* starting the process and the transactions to it, in the order of the calls */
		final ExecutorService worker = Executors.newSingleThreadExecutor();
		ATLBinderConnection connection;
		IBinder appThread;
		final HashMap<ServiceConnection, Intent> bindings = new HashMap<>();
		/* the binder each bound service returned (onBind runs once per service, like AOSP per intent) */
		final HashMap<String, IBinder> service_binders = new HashMap<>();
		/* calls queued for the worker; while there are any, the process must not end */
		int pending;

		ProcessRecord(String name) {
			this.name = name;
		}

		/* the process ended (or crashed): its bindings are lost, like ActiveServices.killServicesLocked() */
		@Override
		public void onConnectionDied(ATLBinderConnection connection) {
			int status = ATLBinderConnection.nativeWaitPid(connection.peerPid);
			Slog.i(TAG, "process " + name + " (pid " + connection.peerPid + ") has died, status " + status);
			final ArrayList<Map.Entry<ServiceConnection, Intent>> lost;
			synchronized (processes) {
				if (processes.get(name) == this)
					processes.remove(name);
				lost = new ArrayList<>(bindings.entrySet());
				bindings.clear();
			}
			worker.shutdown();
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
	public static boolean bindService(final Intent intent, final ServiceConnection conn, String processName) {
		if (isSecondary()) {
			/* a process the main process started does not start processes itself (yet) */
			Slog.w(TAG, "bindService: " + intent.getComponent() + " runs in process " + processName + ", not supported from process " + process_name);
			return false;
		}
		final ProcessRecord r;
		synchronized (processes) {
			r = getProcessRecord(processName);
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
	private static ProcessRecord getProcessRecord(String processName) {
		ProcessRecord r = processes.get(processName);
		/* a process that died but whose end is not handled yet is not used again */
		if (r == null || (r.connection != null && r.connection.isDead())) {
			r = new ProcessRecord(processName);
			processes.put(processName, r);
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
			r = getProcessRecord(processName);
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
			r = processes.get(processName);
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
				r = getProcessRecord(processName);
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
		int pid = ATLBinderConnection.nativeSpawn(sv[1], r.name);
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
				/* the process has no components left: it ends unless calls to it are on the way; a later
				 * call starts a new process */
				boolean exit;
				synchronized (processes) {
					exit = r.pending == 0 && r.bindings.isEmpty();
					if (exit && processes.get(r.name) == r)
						processes.remove(r.name);
				}
				reply.writeInt(exit ? 1 : 0);
				return true;
			}
			if (code == ACQUIRE_PROVIDER) {
				reply.writeStrongBinder(ATLRemoteContentProvider.transportFor(data.readString()));
				return true;
			}
			return super.onTransact(code, data, reply, flags);
		}
	}

	/* ---------------------------------------------------------------- secondary process */

	private static ATLBinderConnection main_connection;

	/* the services the main process bound here, with the connection that binds them in this process */
	private static final HashMap<String, ServiceConnection> bound_here = new HashMap<>();

	/* IApplicationThread of this process: the main process binds and unbinds the services here */
	private static final class AppThread extends Binder {
		@Override
		protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
			switch (code) {
				case BIND_SERVICE: {
					Intent intent = Intent.CREATOR.createFromParcel(data);
					reply.writeStrongBinder(bindHere(intent));
					return true;
				}
				case UNBIND_SERVICE: {
					Intent intent = Intent.CREATOR.createFromParcel(data);
					ServiceConnection conn;
					synchronized (bound_here) {
						conn = bound_here.remove(intent.getComponent().getClassName());
					}
					if (conn != null)
						ATLLoadedApp.getPrimaryApplication().unbindService(conn);
					return true;
				}
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

	/* ActivityThread.handleBindService(): create the service if needed and return what its onBind() returns */
	private static IBinder bindHere(Intent intent) {
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
		if (ATLLoadedApp.getPrimaryApplication().startOrBindService(intent, conn, Context.BIND_AUTO_CREATE) == null)
			return null;
		synchronized (bound_here) {
			bound_here.put(intent.getComponent().getClassName(), conn);
		}
		try {
			if (!done.await(20, TimeUnit.SECONDS))
				Slog.w(TAG, "service " + intent.getComponent() + " did not bind within 20 s");
		} catch (InterruptedException e) {
		}
		return result[0];
	}

	/** the last service of this process is gone: it ends, like a process without components that AOSP kills */
	public static void onServicesGone() {
		/* a process whose provider another process uses stays (AOSP keeps it while the connection lasts) */
		if (!isSecondary() || ATLRemoteContentProvider.hasTransports())
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
					if (!ATLLoadedApp.getPrimaryApplication().hasRunningServices())
						onServicesGone();
				}
			}, 1000);
			return;
		}
		Slog.i(TAG, "process " + process_name + " has no services left, exiting");
		Runtime.getRuntime().halt(0);
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
