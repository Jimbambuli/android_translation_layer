package android.bluetooth;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/* AOSP BluetoothSocket (packages/modules/Bluetooth, android16-release) as far as a socket can exist without a
 * Bluetooth stack: BluetoothDevice.createRfcommSocketToServiceRecord() throws "Bluetooth is not enabled" in ATL, so
 * apps get no instance. connect() is AOSP's "bluetoothProxy == null" branch, which throws a BluetoothSocketException
 * (BLUETOOTH_OFF_FAILURE); that subclass of IOException is not in ATL, so a plain IOException stands in for it. */
public final class BluetoothSocket implements Closeable {
	public static final int MAX_RFCOMM_CHANNEL = 30;
	public static final int TYPE_RFCOMM = 1;
	public static final int TYPE_SCO = 2;
	public static final int TYPE_L2CAP = 3;
	public static final int TYPE_L2CAP_BREDR = TYPE_L2CAP;
	public static final int TYPE_L2CAP_LE = 4;
	public static final int TYPE_LE = 4;

	private final BluetoothDevice mDevice;
	private final int mType;

	BluetoothSocket(BluetoothDevice device, int type) {
		mDevice = device;
		mType = type;
	}

	public BluetoothDevice getRemoteDevice() {
		return mDevice;
	}

	public InputStream getInputStream() throws IOException {
		return null; // mInputStream is only set by connect()
	}

	public OutputStream getOutputStream() throws IOException {
		return null; // mOutputStream is only set by connect()
	}

	public boolean isConnected() {
		return false;
	}

	public void connect() throws IOException {
		throw new IOException("Bluetooth is off");
	}

	public void close() throws IOException {
	}

	public int getConnectionType() {
		return mType;
	}
}
