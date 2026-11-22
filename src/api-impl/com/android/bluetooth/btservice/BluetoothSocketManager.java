package com.android.bluetooth.btservice;

import android.os.ParcelFileDescriptor;
import android.util.Log;
import com.android.bluetooth.bluez.Bluez;
import com.android.bluetooth.bluez.BluezBus;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.UUID;

/* ATL: the RFCOMM part of AOSP's BluetoothSocketManagerBinder (connectSocket/createSocketChannel) on BlueZ: a socket
 * is an org.bluez.Profile1 the app registers with ProfileManager1 for the service UUID. BlueZ publishes the SDP
 * record and listens (server), or looks the channel up by SDP and connects (client, Device1.ConnectProfile), and
 * hands the connected RFCOMM socket over in NewConnection. No AF_BLUETOOTH socket is opened by the app. */
public final class BluetoothSocketManager {
	private static final String TAG = "BluetoothSocketManager";

	private static final String XML = "<node><interface name='org.bluez.Profile1'>"
	                                  + "<method name='Release'/>"
	                                  + "<method name='NewConnection'><arg name='device' type='o' direction='in'/>"
	                                  + "<arg name='fd' type='h' direction='in'/><arg name='fd_properties' type='a{sv}' direction='in'/></method>"
	                                  + "<method name='RequestDisconnection'><arg name='device' type='o' direction='in'/></method>"
	                                  + "</interface></node>";

	private static int next = 1;

	/* one connection handed over by BlueZ */
	public static final class Connection {
		public final String address;
		public final ParcelFileDescriptor fd;
		/* a client socket keeps its profile registered while it is open (BlueZ tracks the connection with it) */
		public Profile owner;

		Connection(String address, ParcelFileDescriptor fd) {
			this.address = address;
			this.fd = fd;
		}
	}

	/* a registered profile; connections queue up until taken */
	public static final class Profile implements BluezBus.ExportedObject {
		final String path;
		final UUID uuid;
		private final ArrayDeque<Connection> connections = new ArrayDeque<>();
		private String error; /* why the profile can no longer give connections */
		private boolean closed;

		Profile(UUID uuid) {
			synchronized (BluetoothSocketManager.class) {
				path = "/org/atl/bluetooth/profile" + next++;
			}
			this.uuid = uuid;
		}

		@Override
		public void onMethodCall(long invocation, String method, Object[] args) {
			if ("NewConnection".equals(method)) {
				String device = (String)args[0];
				int fd = (Integer)args[1];
				Object address = Bluez.get().property(device, Bluez.DEVICE, "Address");
				BluezBus.returnValue(invocation, null);
				if (fd < 0)
					return;
				synchronized (this) {
					if (closed) {
						try {
							ParcelFileDescriptor.adoptFd(fd).close();
						} catch (IOException e) {
						}
						return;
					}
					connections.add(new Connection(address instanceof String ? (String)address : "00:00:00:00:00:00", ParcelFileDescriptor.adoptFd(fd)));
					notifyAll();
				}
			} else if ("Release".equals(method)) {
				BluezBus.returnValue(invocation, null);
				fail("bt socket is not in listen state");
			} else {
				/* RequestDisconnection: the socket's owner closes it */
				BluezBus.returnValue(invocation, null);
			}
		}

		@Override
		public Object getProperty(String name) {
			return null;
		}

		synchronized void fail(String why) {
			if (error == null)
				error = why;
			notifyAll();
		}

		/* waits for a connection; timeoutMs <= 0: no limit */
		public synchronized Connection take(int timeoutMs) throws IOException {
			long end = timeoutMs > 0 ? System.currentTimeMillis() + timeoutMs : 0;
			while (connections.isEmpty()) {
				if (closed)
					throw new IOException("bt socket closed");
				if (error != null)
					throw new IOException(error);
				long wait = end == 0 ? 0 : end - System.currentTimeMillis();
				if (end != 0 && wait <= 0)
					throw new IOException("Try again");
				try {
					wait(wait);
				} catch (InterruptedException e) {
					throw new java.io.InterruptedIOException();
				}
			}
			return connections.poll();
		}

		public void close() {
			synchronized (this) {
				if (closed)
					return;
				closed = true;
				for (Connection c : connections)
					try {
						c.fd.close();
					} catch (IOException e) {
					}
				connections.clear();
				notifyAll();
			}
			BluezBus.call("/org/bluez", Bluez.PROFILE_MANAGER, "UnregisterProfile", "(o)", new Object[] {path}, new BluezBus.Reply() {
				public void onReply(Object[] result, String err, String message) {
					BluezBus.unexport(path, XML);
				}
			});
		}
	}

	/* ProfileManager1.RegisterProfile(); waits for BlueZ's answer. secure: authentication and encryption (AOSP's
	 * secure sockets), insecure: neither */
	public static Profile register(UUID uuid, String role, String name, boolean secure) throws IOException {
		final Profile p = new Profile(uuid);
		if (!BluezBus.export(p.path, XML, p))
			throw new IOException("Error: -1");
		java.util.ArrayList<Object> options = new java.util.ArrayList<>();
		options.add("Role");
		options.add(Bluez.variant("s", role));
		options.add("RequireAuthentication");
		options.add(Bluez.variant("b", secure));
		options.add("RequireAuthorization");
		options.add(Bluez.variant("b", false));
		options.add("AutoConnect");
		options.add(Bluez.variant("b", false));
		if (name != null) {
			options.add("Name");
			options.add(Bluez.variant("s", name));
		}
		Object r = BluezBus.callSync("/org/bluez", Bluez.PROFILE_MANAGER, "RegisterProfile", "(osa{sv})", new Object[] {p.path, uuid.toString(), options.toArray()});
		if (r instanceof String) {
			Log.e(TAG, "RegisterProfile: " + r);
			BluezBus.unexport(p.path, XML);
			throw new IOException("Error: -1");
		}
		return p;
	}

	/* client: BlueZ looks up the RFCOMM channel of uuid by SDP and connects; the socket arrives in NewConnection */
	public static Connection connect(String address, UUID uuid, boolean secure) throws IOException {
		final Profile p = register(uuid, "client", null, secure);
		String path = AdapterService.get().devicePath(address);
		if (path == null || !Bluez.get().has(path, Bluez.DEVICE)) {
			p.close();
			throw new IOException("read failed, socket might closed or timeout, read ret: -1");
		}
		BluezBus.call(path, Bluez.DEVICE, "ConnectProfile", "(s)", new Object[] {uuid.toString()}, 60000, new BluezBus.Reply() {
			public void onReply(Object[] result, String error, String message) {
				if (error != null) {
					Log.i(TAG, "ConnectProfile: " + error + " " + message);
					p.fail("read failed, socket might closed or timeout, read ret: -1");
				}
			}
		});
		Connection c;
		try {
			c = p.take(0);
		} catch (IOException e) {
			p.close();
			throw e;
		}
		c.owner = p;
		return c;
	}
}
