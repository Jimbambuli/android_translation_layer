package android.hardware.usb;

import java.util.concurrent.TimeoutException;

/* AOSP core/java/android/hardware/usb/UsbDeviceConnection.java. UsbManager.openDevice() never returns a connection
 * in ATL (no USB service), so a connection is never open: the native_* calls behave as
 * android_hardware_UsbDeviceConnection.cpp does for a closed device (false / -1 / null). */
public class UsbDeviceConnection {
	private final UsbDevice mDevice;

	public UsbDeviceConnection(UsbDevice device) {
		mDevice = device;
	}

	boolean isOpen() {
		return false;
	}

	public void close() {}

	public int getFileDescriptor() {
		return -1;
	}

	public byte[] getRawDescriptors() {
		return null;
	}

	public boolean claimInterface(UsbInterface intf, boolean force) {
		return false;
	}

	public boolean releaseInterface(UsbInterface intf) {
		return false;
	}

	public boolean setInterface(UsbInterface intf) {
		return false;
	}

	public boolean setConfiguration(UsbConfiguration configuration) {
		return false;
	}

	public int controlTransfer(int requestType, int request, int value, int index, byte[] buffer, int length, int timeout) {
		return controlTransfer(requestType, request, value, index, buffer, 0, length, timeout);
	}

	public int controlTransfer(int requestType, int request, int value, int index, byte[] buffer, int offset, int length,
	                           int timeout) {
		checkBounds(buffer, offset, length);
		return -1;
	}

	public int bulkTransfer(UsbEndpoint endpoint, byte[] buffer, int length, int timeout) {
		return bulkTransfer(endpoint, buffer, 0, length, timeout);
	}

	public int bulkTransfer(UsbEndpoint endpoint, byte[] buffer, int offset, int length, int timeout) {
		checkBounds(buffer, offset, length);
		return -1;
	}

	public UsbRequest requestWait() {
		return null;
	}

	public UsbRequest requestWait(long timeout) throws TimeoutException {
		if (timeout < 0) {
			throw new IllegalArgumentException("timeout must not be negative");
		}
		return null;
	}

	public String getSerial() {
		return null;
	}

	boolean cancelRequest(UsbRequest request) {
		return false;
	}

	private static void checkBounds(byte[] buffer, int start, int length) {
		final int bufferLength = (buffer != null ? buffer.length : 0);
		if (length < 0 || start < 0 || start + length > bufferLength) {
			throw new IllegalArgumentException("Buffer start or length out of bounds.");
		}
	}
}
