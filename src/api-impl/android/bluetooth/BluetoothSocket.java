package android.bluetooth;

import android.annotation.IntDef;
import android.os.ParcelUuid;
import android.util.Log;
import com.android.bluetooth.btservice.AdapterService;
import com.android.bluetooth.btservice.BluetoothSocketManager;
import java.io.Closeable;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/* AOSP BluetoothSocket (packages/modules/Bluetooth framework, android-16.0.0_r4). An RFCOMM socket to a service UUID
 * connects through ATL's BluetoothSocketManager (BlueZ ProfileManager1/Profile1: SDP lookup and connection by BlueZ,
 * the connected socket handed to the app); its streams read and write that socket like AOSP's
 * BluetoothInputStream/BluetoothOutputStream. RFCOMM to a fixed channel and L2CAP need the app's own AF_BLUETOOTH
 * socket, which ATL does not open: connect() fails for them. */
public final class BluetoothSocket implements Closeable {
	private static final String TAG = "BluetoothSocket";

	public static final int MAX_RFCOMM_CHANNEL = 30;
	public static final int TYPE_RFCOMM = 1;
	public static final int TYPE_SCO = 2;
	public static final int TYPE_L2CAP = 3;
	public static final int TYPE_L2CAP_BREDR = TYPE_L2CAP;
	public static final int TYPE_L2CAP_LE = 4;
	public static final int TYPE_LE = 4;

	/** @hide */
	@IntDef(prefix = {"BluetoothSocket.TYPE_"}, value = {BluetoothSocket.TYPE_RFCOMM, BluetoothSocket.TYPE_SCO, BluetoothSocket.TYPE_L2CAP, BluetoothSocket.TYPE_LE})
	@Retention(RetentionPolicy.SOURCE)
	public @interface SocketType {}

	/*package*/ static final String DEFAULT_SOCKET_NAME = "default_name";

	private enum SocketState { INIT, CONNECTED, LISTENING, CLOSED }

	private final BluetoothDevice mDevice;
	private final int mType;
	private final boolean mAuth;
	private final boolean mEncrypt;
	private final int mPort;
	private final ParcelUuid mUuid;
	private volatile SocketState mSocketState = SocketState.INIT;
	private BluetoothSocketManager.Connection mConnection;
	private FileInputStream mSocketIS;
	private FileOutputStream mSocketOS;
	private final InputStream mInputStream;
	private final OutputStream mOutputStream;

	/*package*/ BluetoothSocket(BluetoothDevice device, int type, boolean auth, boolean encrypt, int port, ParcelUuid uuid) {
		if (type == TYPE_RFCOMM && uuid == null && (port < 1 || port > MAX_RFCOMM_CHANNEL)) {
			throw new IllegalArgumentException("Invalid RFCOMM channel: " + port);
		}
		mDevice = device;
		mType = type;
		mAuth = auth;
		mEncrypt = encrypt;
		mPort = port;
		mUuid = uuid;
		mInputStream = new InputStream() {
			@Override
			public int available() throws IOException {
				return BluetoothSocket.this.available();
			}

			@Override
			public void close() throws IOException {
				BluetoothSocket.this.close();
			}

			@Override
			public int read() throws IOException {
				byte[] b = new byte[1];
				int ret = BluetoothSocket.this.read(b, 0, 1);
				return ret == 1 ? (int)b[0] & 0xff : -1;
			}

			@Override
			public int read(byte[] b, int offset, int length) throws IOException {
				if (b == null) {
					throw new NullPointerException("byte array is null");
				}
				if ((offset | length) < 0 || length > b.length - offset) {
					throw new ArrayIndexOutOfBoundsException("invalid offset or length");
				}
				return BluetoothSocket.this.read(b, offset, length);
			}
		};
		mOutputStream = new OutputStream() {
			@Override
			public void close() throws IOException {
				BluetoothSocket.this.close();
			}

			@Override
			public void write(int oneByte) throws IOException {
				BluetoothSocket.this.write(new byte[] {(byte)oneByte}, 0, 1);
			}

			@Override
			public void write(byte[] b, int offset, int count) throws IOException {
				if (b == null) {
					throw new NullPointerException("buffer is null");
				}
				if ((offset | count) < 0 || count > b.length - offset) {
					throw new IndexOutOfBoundsException("invalid offset or length");
				}
				BluetoothSocket.this.write(b, offset, count);
			}

			@Override
			public void flush() throws IOException {}
		};
	}

	/* an accepted socket (BluetoothServerSocket) */
	/*package*/ BluetoothSocket(BluetoothDevice device, int type, BluetoothSocketManager.Connection connection) {
		this(device, type, true, true, -1, null);
		attach(connection);
	}

	private void attach(BluetoothSocketManager.Connection connection) {
		mConnection = connection;
		mSocketIS = new FileInputStream(connection.fd.getFileDescriptor());
		mSocketOS = new FileOutputStream(connection.fd.getFileDescriptor());
		mSocketState = SocketState.CONNECTED;
	}

	public BluetoothDevice getRemoteDevice() {
		return mDevice;
	}

	public InputStream getInputStream() throws IOException {
		return mInputStream;
	}

	public OutputStream getOutputStream() throws IOException {
		return mOutputStream;
	}

	public boolean isConnected() {
		return mSocketState == SocketState.CONNECTED;
	}

	public void connect() throws IOException {
		if (mDevice == null) throw new IOException("Connect is called on null device");
		if (mSocketState == SocketState.CLOSED) throw new IOException("socket closed");
		BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
		if (adapter == null || !adapter.isEnabled()) {
			throw new BluetoothSocketException(BluetoothSocketException.BLUETOOTH_OFF_FAILURE);
		}
		AdapterService.checkConnectPermission(TAG, "connect");
		if (mType != TYPE_RFCOMM || mUuid == null) {
			throw new BluetoothSocketException(BluetoothSocketException.SOCKET_CONNECTION_FAILURE, "ATL cannot open " + (mType == TYPE_RFCOMM ? "RFCOMM channel sockets" : "L2CAP sockets"));
		}
		BluetoothSocketManager.Connection c = BluetoothSocketManager.connect(mDevice.getAddress(), mUuid.getUuid(), mAuth || mEncrypt);
		synchronized (this) {
			if (mSocketState == SocketState.CLOSED) {
				c.fd.close();
				if (c.owner != null)
					c.owner.close();
				throw new IOException("bt socket closed");
			}
			attach(c);
		}
	}

	/*package*/ int available() throws IOException {
		if (mSocketIS == null) throw new IOException("socket not created");
		return mSocketIS.available();
	}

	/*package*/ int read(byte[] b, int offset, int length) throws IOException {
		if (mSocketIS == null) throw new IOException("bt socket is not connected");
		int ret = mSocketIS.read(b, offset, length);
		if (ret < 0) {
			throw new IOException("bt socket closed, read return: " + ret);
		}
		return ret;
	}

	/*package*/ int write(byte[] b, int offset, int length) throws IOException {
		if (mSocketOS == null) throw new IOException("bt socket is not connected");
		mSocketOS.write(b, offset, length);
		return length;
	}

	public void close() throws IOException {
		BluetoothSocketManager.Connection c;
		synchronized (this) {
			if (mSocketState == SocketState.CLOSED)
				return;
			mSocketState = SocketState.CLOSED;
			c = mConnection;
			mConnection = null;
		}
		if (c != null) {
			c.fd.close();
			if (c.owner != null)
				c.owner.close();
		}
	}

	/* BlueZ does not report the RFCOMM frame size: AOSP's value before the stack reports it */
	public int getMaxTransmitPacketSize() {
		return 0;
	}

	public int getMaxReceivePacketSize() {
		return 0;
	}

	public int getConnectionType() {
		if (mType == TYPE_L2CAP_LE) {
			return TYPE_L2CAP;
		}
		return mType;
	}
}
