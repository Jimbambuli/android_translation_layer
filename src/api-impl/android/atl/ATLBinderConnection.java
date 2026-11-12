package android.atl;

import android.os.Binder;
import android.os.BinderProxy;
import android.os.DeadObjectException;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Process;
import android.os.RemoteException;
import android.system.ErrnoException;
import android.system.Os;
import android.util.Slog;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.ByteArrayInputStream;
import java.io.FileDescriptor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Binder transactions between the app's main process and one of its other processes (android:process), what the
 * binder driver does on Android: a transaction to a BinderProxy is sent with the binders and file descriptors of
 * its Parcel and executed by the Binder object of the other side, which sends back the reply.
 *
 * Binders cross as nodes: a Binder of this process is sent as its node id, the other side gets a BinderProxy
 * for it; a BinderProxy of the other side goes back as that id and arrives as the original Binder. Handle 0 is
 * the context object of the connection.
 *
 * Like the driver, a thread waiting for its reply executes the transactions the other side makes while serving
 * it (a callback in the middle of a call), all others run on a pool of binder threads; one-way transactions run
 * in order.
 */
public final class ATLBinderConnection {
	private static final String TAG = "ATLBinderConnection";

	private static final int TXN = 1;
	private static final int REPLY = 2;
	private static final int DEAD_REPLY = 3; // the target of a transaction is not known
	/* object table entries: a node of the sender, or a node of the receiver that the sender holds a proxy for */
	private static final byte SENDER_NODE = 1;
	private static final byte RECEIVER_NODE = 2;
	private static final int MAX_FDS = 253;

	/** what the other side's end means for this process */
	public interface DeathListener {
		void onConnectionDied(ATLBinderConnection connection);
	}

	private final int fd;
	public final int peerPid;
	private final DeathListener deathListener;
	private volatile boolean dead;
	private final AtomicInteger nextTxnId = new AtomicInteger(1);
	private final HashMap<Integer, Waiter> waiters = new HashMap<>();
	private final IdentityHashMap<IBinder, Integer> nodeIds = new IdentityHashMap<>();
	private final HashMap<Integer, IBinder> nodes = new HashMap<>();
	private final HashMap<Integer, BinderProxy> proxies = new HashMap<>();
	private int nextNodeId = 1;
	private final ExecutorService oneway = Executors.newSingleThreadExecutor(new BinderThreadFactory("oneway"));

	private static final AtomicInteger binderThreads = new AtomicInteger();
	/* the binder thread pool, like ProcessState's (at most 15 threads) */
	private static final ThreadPoolExecutor pool = new ThreadPoolExecutor(1, 15, 60, TimeUnit.SECONDS,
	                                                                       new LinkedBlockingQueue<Runnable>(), new BinderThreadFactory(null));
	static {
		pool.allowCoreThreadTimeOut(true);
	}

	private static final class BinderThreadFactory implements ThreadFactory {
		private final String name;

		BinderThreadFactory(String name) {
			this.name = name;
		}

		@Override
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r, "binder:" + Process.myPid() + "_" + (name != null ? name : Integer.toString(binderThreads.incrementAndGet())));
			t.setDaemon(true);
			return t;
		}
	}

	private static final class Message {
		int type;
		int txnId;
		int handle;
		int code;
		int flags;
		int callingPid;
		int nestedIn;
		byte[] objKinds;
		int[] objIds;
		byte[] data;
		FileDescriptor[] fds;
	}

	/* a thread waiting for the reply to its transaction, and the transactions it executes meanwhile */
	private static final class Waiter {
		final LinkedBlockingQueue<Message> inbox = new LinkedBlockingQueue<>();
	}

	/* the transaction a thread is executing for which connection, innermost last */
	private static final class Serving {
		final ATLBinderConnection connection;
		final int txnId;
		final int callingPid;

		Serving(ATLBinderConnection connection, int txnId, int callingPid) {
			this.connection = connection;
			this.txnId = txnId;
			this.callingPid = callingPid;
		}
	}

	private static final ThreadLocal<ArrayDeque<Serving>> serving = new ThreadLocal<ArrayDeque<Serving>>() {
		@Override
		protected ArrayDeque<Serving> initialValue() {
			return new ArrayDeque<>();
		}
	};

	/** Binder.getCallingPid(): the process whose transaction this thread executes, else this one */
	public static int getCallingPid() {
		Serving s = serving.get().peekLast();
		return s != null ? s.callingPid : Process.myPid();
	}

	public ATLBinderConnection(int fd, int peerPid, IBinder contextObject, DeathListener deathListener) {
		this.fd = fd;
		this.peerPid = peerPid;
		this.deathListener = deathListener;
		if (contextObject != null) {
			nodes.put(0, contextObject);
			nodeIds.put(contextObject, 0);
		}
		Thread reader = new Thread(new Runnable() {
			@Override
			public void run() {
				readLoop();
			}
		}, "binder:" + Process.myPid() + "_reader");
		reader.setDaemon(true);
		reader.start();
	}

	/** the context object of the other side */
	public IBinder getContextObject() {
		return proxyFor(0);
	}

	public boolean isDead() {
		return dead;
	}

	private synchronized BinderProxy proxyFor(int handle) {
		BinderProxy proxy = proxies.get(handle);
		if (proxy == null) {
			proxy = new BinderProxy(this, handle);
			if (dead)
				proxy.atl_markDead();
			proxies.put(handle, proxy);
		}
		return proxy;
	}

	private synchronized int nodeIdFor(IBinder binder) {
		Integer id = nodeIds.get(binder);
		if (id == null) {
			/* nodes stay for the life of the connection (no reference counting yet) */
			id = nextNodeId++;
			nodeIds.put(binder, id);
			nodes.put(id, binder);
		}
		return id;
	}

	private synchronized IBinder node(int id) {
		return nodes.get(id);
	}

	/** BinderProxy.transact(): send the transaction and, unless it is one-way, wait for the reply */
	public boolean transact(int handle, int code, Parcel data, Parcel reply, int flags) throws RemoteException {
		if (dead)
			throw new DeadObjectException();
		Message m = new Message();
		m.type = TXN;
		m.txnId = nextTxnId.getAndIncrement();
		m.handle = handle;
		m.code = code;
		m.flags = flags;
		Serving s = innermostServing(this);
		m.nestedIn = s != null ? s.txnId : 0;
		boolean oneway = (flags & IBinder.FLAG_ONEWAY) != 0;
		Waiter waiter = null;
		if (!oneway) {
			waiter = new Waiter();
			synchronized (waiters) {
				waiters.put(m.txnId, waiter);
			}
		}
		try {
			send(m, data);
			if (oneway)
				return true;
			while (true) {
				Message in;
				try {
					in = waiter.inbox.take();
				} catch (InterruptedException e) {
					throw new RemoteException("interrupted");
				}
				if (in.type == TXN) {
					execute(in);
					continue;
				}
				if (in.type == DEAD_REPLY || in.data == null)
					throw new DeadObjectException();
				if (reply != null)
					toParcel(in, reply);
				return true;
			}
		} finally {
			if (waiter != null) {
				synchronized (waiters) {
					waiters.remove(m.txnId);
				}
			}
		}
	}

	private static Serving innermostServing(ATLBinderConnection connection) {
		ArrayDeque<Serving> stack = serving.get();
		java.util.Iterator<Serving> it = stack.descendingIterator();
		while (it.hasNext()) {
			Serving s = it.next();
			if (s.connection == connection)
				return s;
		}
		return null;
	}

	private void send(Message m, Parcel parcel) throws RemoteException {
		ArrayList<IBinder> binders = parcel != null ? parcel.atl_getBinders() : null;
		ArrayList<FileDescriptor> fds = parcel != null ? parcel.atl_getFileDescriptors() : null;
		byte[] data = parcel != null ? parcel.marshall() : new byte[0];
		ByteArrayOutputStream bytes = new ByteArrayOutputStream(48 + data.length);
		DataOutputStream out = new DataOutputStream(bytes);
		int[] fdInts = null;
		try {
			out.writeInt(m.type);
			out.writeInt(m.txnId);
			out.writeInt(m.handle);
			out.writeInt(m.code);
			out.writeInt(m.flags);
			out.writeInt(Process.myPid());
			out.writeInt(m.nestedIn);
			int n = binders != null ? binders.size() : 0;
			out.writeInt(n);
			for (int i = 0; i < n; i++) {
				IBinder b = binders.get(i);
				if (b instanceof BinderProxy && ((BinderProxy)b).atl_getConnection() == this) {
					out.writeByte(RECEIVER_NODE);
					out.writeInt(((BinderProxy)b).atl_getHandle());
				} else if (b instanceof BinderProxy) {
					/* a binder of a third process: not passed on yet */
					throw new RemoteException("cannot pass a binder of another process on");
				} else {
					out.writeByte(SENDER_NODE);
					out.writeInt(nodeIdFor(b));
				}
			}
			out.write(data);
			out.flush();
			if (fds != null && !fds.isEmpty()) {
				if (fds.size() > MAX_FDS)
					throw new RemoteException("too many file descriptors: " + fds.size());
				fdInts = new int[fds.size()];
				for (int i = 0; i < fdInts.length; i++)
					fdInts[i] = fds.get(i).getInt$();
			}
		} catch (IOException e) {
			throw new RemoteException(e.toString());
		}
		int ret = nativeSend(fd, bytes.toByteArray(), fdInts);
		ArrayList<FileDescriptor> close = parcel != null ? parcel.atl_fileDescriptorsToClose() : null;
		for (int i = 0; close != null && i < close.size(); i++) {
			try {
				Os.close(close.get(i));
			} catch (ErrnoException ignored) {
			}
		}
		if (ret < 0) {
			if (dead)
				throw new DeadObjectException();
			throw new RemoteException("sendmsg failed: errno " + (-ret));
		}
	}

	private void readLoop() {
		int[] fdsOut = new int[MAX_FDS + 1];
		while (true) {
			byte[] bytes = nativeReceive(fd, fdsOut);
			if (bytes == null)
				break;
			Message m = new Message();
			try {
				DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));
				m.type = in.readInt();
				m.txnId = in.readInt();
				m.handle = in.readInt();
				m.code = in.readInt();
				m.flags = in.readInt();
				m.callingPid = in.readInt();
				m.nestedIn = in.readInt();
				int n = in.readInt();
				m.objKinds = new byte[n];
				m.objIds = new int[n];
				for (int i = 0; i < n; i++) {
					m.objKinds[i] = in.readByte();
					m.objIds[i] = in.readInt();
				}
				m.data = new byte[in.available()];
				in.readFully(m.data);
			} catch (IOException e) {
				Slog.e(TAG, "malformed message", e);
				break;
			}
			m.fds = new FileDescriptor[fdsOut[0]];
			for (int i = 0; i < m.fds.length; i++) {
				m.fds[i] = new FileDescriptor();
				m.fds[i].setInt$(fdsOut[1 + i]);
			}
			dispatch(m);
		}
		die();
	}

	private void dispatch(final Message m) {
		if (m.type == REPLY || m.type == DEAD_REPLY) {
			Waiter w;
			synchronized (waiters) {
				w = waiters.get(m.txnId);
			}
			if (w != null)
				w.inbox.add(m);
			else
				Slog.w(TAG, "reply to unknown transaction " + m.txnId);
			return;
		}
		if (m.nestedIn != 0) {
			Waiter w;
			synchronized (waiters) {
				w = waiters.get(m.nestedIn);
			}
			if (w != null) {
				w.inbox.add(m);
				return;
			}
		}
		Runnable r = new Runnable() {
			@Override
			public void run() {
				execute(m);
			}
		};
		if ((m.flags & IBinder.FLAG_ONEWAY) != 0)
			oneway.execute(r);
		else
			pool.execute(r);
	}

	/* Binder.execTransact(): run the transaction on its target and send the reply */
	private void execute(Message m) {
		IBinder target = node(m.handle);
		boolean oneway = (m.flags & IBinder.FLAG_ONEWAY) != 0;
		Parcel data = Parcel.obtain();
		toParcel(m, data);
		Parcel reply = Parcel.obtain();
		ArrayDeque<Serving> stack = serving.get();
		stack.addLast(new Serving(this, m.txnId, m.callingPid));
		try {
			if (target == null) {
				Slog.w(TAG, "transaction to unknown node " + m.handle);
			} else {
				try {
					target.transact(m.code, data, reply, m.flags);
				} catch (RemoteException | RuntimeException e) {
					if (oneway) {
						Slog.w(TAG, "uncaught exception in one-way transaction " + m.code, e);
					} else {
						reply.setDataPosition(0);
						reply.setDataSize(0);
						try {
							reply.writeException(e);
						} catch (Exception unwritable) {
							Slog.e(TAG, "uncaught exception in transaction " + m.code, e);
							reply.setDataSize(0);
						}
					}
				}
			}
		} finally {
			stack.removeLast();
			/* like Parcel.recycle() of the driver's buffer: the received descriptors nobody took belong to the
			 * transaction */
			for (FileDescriptor f : data.atl_untakenFileDescriptors()) {
				try {
					Os.close(f);
				} catch (ErrnoException ignored) {
				}
			}
		}
		if (oneway)
			return;
		Message r = new Message();
		r.type = target == null ? DEAD_REPLY : REPLY;
		r.txnId = m.txnId;
		try {
			send(r, reply);
		} catch (RemoteException e) {
			Slog.w(TAG, "cannot send the reply to transaction " + m.txnId + ": " + e);
		}
	}

	private void toParcel(Message m, Parcel parcel) {
		ArrayList<IBinder> binders = new ArrayList<>(m.objIds.length);
		for (int i = 0; i < m.objIds.length; i++)
			binders.add(m.objKinds[i] == RECEIVER_NODE ? node(m.objIds[i]) : proxyFor(m.objIds[i]));
		ArrayList<FileDescriptor> fds = new ArrayList<>(m.fds.length);
		for (FileDescriptor f : m.fds)
			fds.add(f);
		parcel.unmarshall(m.data, 0, m.data.length);
		parcel.atl_setObjects(binders, fds);
		parcel.setDataPosition(0);
	}

	private void die() {
		dead = true;
		try {
			Os.close(fdObject());
		} catch (ErrnoException ignored) {
		}
		ArrayList<Waiter> pending;
		synchronized (waiters) {
			pending = new ArrayList<>(waiters.values());
		}
		for (Waiter w : pending) {
			Message m = new Message();
			m.type = DEAD_REPLY;
			w.inbox.add(m);
		}
		ArrayList<BinderProxy> dying;
		synchronized (this) {
			dying = new ArrayList<>(proxies.values());
		}
		for (BinderProxy p : dying)
			p.atl_markDead();
		for (BinderProxy p : dying)
			p.atl_sendObituary();
		if (deathListener != null)
			deathListener.onConnectionDied(this);
	}

	private FileDescriptor fdObject() {
		FileDescriptor f = new FileDescriptor();
		f.setInt$(fd);
		return f;
	}

	static native int[] nativeSocketPair();
	static native int nativeSpawn(int childFd, String processName);
	private static native int nativeSend(int fd, byte[] data, int[] fds);
	private static native byte[] nativeReceive(int fd, int[] fdsOut);
	static native int nativeWaitPid(int pid);
}
