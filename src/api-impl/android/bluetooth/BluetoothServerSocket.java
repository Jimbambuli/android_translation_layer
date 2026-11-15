package android.bluetooth;

import java.io.Closeable;
import java.io.IOException;

/* AOSP BluetoothServerSocket: BluetoothAdapter.listenUsing*RfcommWithServiceRecord() throws IOException("Error: -1")
 * in ATL (bindListen() with the Bluetooth service off), so apps get no instance; the class exists for the type. */
public final class BluetoothServerSocket implements Closeable {

	BluetoothServerSocket() {}

	public BluetoothSocket accept() throws IOException {
		throw new IOException("Bluetooth is off");
	}

	public BluetoothSocket accept(int timeout) throws IOException {
		throw new IOException("Bluetooth is off");
	}

	public void close() throws IOException {}

	public int getChannel() {
		return -1;
	}

	public int getPsm() {
		return -1;
	}
}
