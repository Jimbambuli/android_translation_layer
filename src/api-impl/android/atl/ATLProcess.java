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
			ProcessRecord existing = processes.get(processName);
			if (existing == null) {
				existing = new ProcessRecord(processName);
				processes.put(processName, existing);
			}
			r = existing;
			r.bindings.put(conn, intent);
		}
		r.worker.execute(new Runnable() {
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

	private static IBinder bindInProcess(ProcessRecord r, Intent intent) throws RemoteException {
		if (r.connection == null)
			startProcess(r);
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
		r.connection = new ATLBinderConnection(sv[0], pid, null, r);
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
			/* the process ends by itself once its last service is gone; a new binding starts a new one */
			if (r.bindings.isEmpty())
				processes.remove(r.name);
		}
		if (last) {
			r.worker.execute(new Runnable() {
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
		if (!isSecondary())
			return;
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
