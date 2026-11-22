package com.android.bluetooth.bluez;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.SparseArray;

/* ATL: the system bus connection to BlueZ (org.bluez), in place of the binder connection of AOSP's framework to the
 * Bluetooth app. Values cross as Java objects in both directions:
 *   b Boolean, y n q i h Integer, u x t Long, d Double, s o g String, ay byte[], a{..} java.util.Map (to the bus: an
 *   Object[] of key, value, key, value...), other arrays and tuples Object[], v unwrapped (to the bus: Object[]{sig, value}).
 * A received fd (h) is a dup the receiver owns. Replies, signals and calls to exported objects arrive on the main
 * thread (the main loop of the GLib default context). */
public final class BluezBus {
	private static final String TAG = "BluezBus";

	public interface Reply {
		/* result: Object[] of the out values, or null with error set ("org.bluez.Error.Failed" ...) */
		void onReply(Object[] result, String error, String message);
	}

	public interface Listener {
		void onSignal(String path, String iface, String member, Object[] args);
		void onOwnerChanged(boolean present);
	}

	public interface ExportedObject {
		/* answer with returnValue()/returnError() on the invocation, now or later */
		void onMethodCall(long invocation, String method, Object[] args);
		Object getProperty(String name);
	}

	private static boolean initialized;
	private static boolean connected;
	private static Listener listener;
	private static final SparseArray<Reply> replies = new SparseArray<>();
	private static int nextToken = 1;
	private static final SparseArray<ExportedObject> exported = new SparseArray<>();
	private static final java.util.HashMap<String, Integer> exportedIds = new java.util.HashMap<>();

	private BluezBus() {}

	/* connects to the system bus once; false if there is none (or the sandbox gives none) */
	public static synchronized boolean init(Listener l) {
		listener = l;
		if (!initialized) {
			initialized = true;
			connected = nativeInit();
		}
		return connected;
	}

	public static boolean isConnected() {
		return connected;
	}

	/* blocking call, for state the app reads synchronously; result as in Reply, or a String error "name: message" */
	public static Object callSync(String path, String iface, String method, String sig, Object[] args) {
		if (!connected)
			return "org.freedesktop.DBus.Error.Disconnected: no system bus";
		return nativeCallSync(path, iface, method, sig, args, 25000);
	}

	public static void call(String path, String iface, String method, String sig, Object[] args, Reply reply) {
		call(path, iface, method, sig, args, -1, reply);
	}

	/* timeoutMs -1: the D-Bus default (25 s) */
	public static void call(String path, String iface, String method, String sig, Object[] args, int timeoutMs, Reply reply) {
		if (!connected) {
			if (reply != null)
				deliverLater(reply, "org.freedesktop.DBus.Error.Disconnected", "no system bus");
			return;
		}
		int token = 0;
		if (reply != null) {
			synchronized (replies) {
				token = nextToken++;
				if (nextToken == Integer.MAX_VALUE)
					nextToken = 1;
				replies.put(token, reply);
			}
		}
		nativeCall(token, path, iface, method, sig, args, timeoutMs);
	}

	private static void deliverLater(final Reply reply, final String error, final String message) {
		new Handler(Looper.getMainLooper()).post(new Runnable() {
			public void run() {
				reply.onReply(null, error, message);
			}
		});
	}

	public static void setProperty(String path, String iface, String name, String sig, Object value, Reply reply) {
		call(path, "org.freedesktop.DBus.Properties", "Set", "(ssv)", new Object[] {iface, name, new Object[] {sig, value}}, reply);
	}

	/* xml: introspection data of one interface; false if the path already has it */
	public static synchronized boolean export(String path, String xml, ExportedObject object) {
		if (!connected)
			return false;
		int id = nativeExport(path, xml);
		if (id <= 0)
			return false;
		exported.put(id, object);
		exportedIds.put(path + "|" + xml.hashCode(), id);
		return true;
	}

	public static synchronized void unexport(String path, String xml) {
		Integer id = exportedIds.remove(path + "|" + xml.hashCode());
		if (id != null) {
			exported.remove(id);
			nativeUnexport(id);
		}
	}

	public static void returnValue(long invocation, Object[] values) {
		nativeReturn(invocation, values);
	}

	public static void returnError(long invocation, String name, String message) {
		nativeReturnError(invocation, name, message);
	}

	/* ---- from the native side (main thread) ---- */

	private static void onReply(int token, Object[] result, String error, String message) {
		Reply reply;
		synchronized (replies) {
			reply = replies.get(token);
			replies.remove(token);
		}
		if (reply == null)
			return;
		try {
			reply.onReply(result, error, message);
		} catch (RuntimeException e) {
			Log.e(TAG, "reply handler failed", e);
			throw e;
		}
	}

	private static void onSignal(String path, String iface, String member, Object[] args) {
		if (listener != null)
			listener.onSignal(path, iface, member, args);
	}

	private static void onOwner(boolean present) {
		if (listener != null)
			listener.onOwnerChanged(present);
	}

	private static void onMethodCall(int id, long invocation, String method, Object[] args) {
		ExportedObject object;
		synchronized (BluezBus.class) {
			object = exported.get(id);
		}
		if (object == null) {
			nativeReturnError(invocation, "org.freedesktop.DBus.Error.UnknownObject", "gone");
			return;
		}
		object.onMethodCall(invocation, method, args);
	}

	private static Object onGetProperty(int id, String name) {
		ExportedObject object;
		synchronized (BluezBus.class) {
			object = exported.get(id);
		}
		return object == null ? null : object.getProperty(name);
	}

	private static native boolean nativeInit();
	private static native Object nativeCallSync(String path, String iface, String method, String sig, Object[] args, int timeoutMs);
	private static native void nativeCall(int token, String path, String iface, String method, String sig, Object[] args, int timeoutMs);
	private static native int nativeExport(String path, String xml);
	private static native void nativeUnexport(int id);
	private static native void nativeReturn(long invocation, Object[] values);
	private static native void nativeReturnError(long invocation, String name, String message);
}
