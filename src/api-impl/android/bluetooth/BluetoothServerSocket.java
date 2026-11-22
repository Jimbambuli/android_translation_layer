package android.bluetooth;

import com.android.bluetooth.btservice.AdapterService;
import com.android.bluetooth.btservice.BluetoothSocketManager;
import java.io.Closeable;
import java.io.IOException;
import java.util.UUID;

/* AOSP BluetoothServerSocket (packages/modules/Bluetooth framework, android-16.0.0_r4). An RFCOMM server for a
 * service UUID is a BlueZ Profile1 in the server role (BluetoothSocketManager): BlueZ publishes the SDP record with the
 * name, picks the channel and hands each incoming connection over; accept() takes them in order. */
public final class BluetoothServerSocket implements Closeable {
	private static final String TAG = "BluetoothServerSocket";

	private final BluetoothSocketManager.Profile mProfile;
	private final int mType;
	private volatile boolean mClosed;

	private BluetoothServerSocket(BluetoothSocketManager.Profile profile, int type) {
		mProfile = profile;
		mType = type;
	}

	/* BluetoothAdapter.createNewRfcommSocketAndRecord() */
	/*package*/ static BluetoothServerSocket listenRfcomm(String name, UUID uuid, boolean secure) throws IOException {
		BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
		if (adapter == null || !adapter.isEnabled())
			throw new IOException("Error: -1");
		AdapterService.checkConnectPermission(TAG, "listenUsingRfcommWithServiceRecord");
		return new BluetoothServerSocket(BluetoothSocketManager.register(uuid, "server", name, secure), BluetoothSocket.TYPE_RFCOMM);
	}

	public BluetoothSocket accept() throws IOException {
		return accept(-1);
	}

	public BluetoothSocket accept(int timeout) throws IOException {
		if (mClosed)
			throw new IOException("bt socket is not in listen state");
		BluetoothSocketManager.Connection c = mProfile.take(timeout);
		BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
		return new BluetoothSocket(adapter.getRemoteDevice(c.address), mType, c);
	}

	public void close() throws IOException {
		mClosed = true;
		mProfile.close();
	}

	/** @hide BlueZ picks the channel and does not tell it */
	public int getChannel() {
		return -1;
	}

	/* RFCOMM servers have no PSM: AOSP returns -1 for them */
	public int getPsm() {
		return -1;
	}

	@Override
	public String toString() {
		return "ServerSocket: Type: TYPE_RFCOMM";
	}
}
