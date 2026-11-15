package android.hardware.usb;

import java.nio.ByteBuffer;
import java.util.Objects;

/* AOSP core/java/android/hardware/usb/UsbRequest.java. The connection is never open in ATL (see
 * UsbDeviceConnection), so native_init fails and queue() finds no open connection. */
public class UsbRequest {
	private UsbEndpoint mEndpoint;
	private Object mClientData;
	private UsbDeviceConnection mConnection;

	public UsbRequest() {}

	public boolean initialize(UsbDeviceConnection connection, UsbEndpoint endpoint) {
		mEndpoint = endpoint;
		mConnection = Objects.requireNonNull(connection, "connection");
		return false;
	}

	public void close() {}

	public UsbEndpoint getEndpoint() {
		return mEndpoint;
	}

	public Object getClientData() {
		return mClientData;
	}

	public void setClientData(Object data) {
		mClientData = data;
	}

	@Deprecated
	public boolean queue(ByteBuffer buffer, int length) {
		if (mConnection == null || !mConnection.isOpen()) {
			throw new NullPointerException("invalid connection");
		}
		return false;
	}

	public boolean queue(ByteBuffer buffer) {
		if (mConnection == null || !mConnection.isOpen()) {
			throw new IllegalStateException("invalid connection");
		}
		return false;
	}

	public boolean cancel() {
		if (mConnection == null) {
			return false;
		}
		return mConnection.cancelRequest(this);
	}
}
