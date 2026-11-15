package android.hardware.usb;

import java.util.HashMap;

public class UsbManager {
	public HashMap getDeviceList() {
		return new HashMap();
	}

	/* AOSP hasPermission(UsbDevice): false without the USB service (mService == null) */
	public boolean hasPermission(UsbDevice device) {
		return false;
	}

	/* AOSP hasPermission(UsbAccessory): false without the USB service (mService == null) */
	public boolean hasPermission(UsbAccessory accessory) {
		return false;
	}

	/* AOSP openDevice(): the USB service gives no file descriptor for a device it does not list -> null */
	public UsbDeviceConnection openDevice(UsbDevice device) {
		device.getDeviceName();
		return null;
	}

	/* AOSP requestPermission(): the USB service ignores devices/accessories it does not know and sends no result */
	public void requestPermission(UsbDevice device, android.app.PendingIntent pi) {}

	public void requestPermission(UsbAccessory accessory, android.app.PendingIntent pi) {}
}
