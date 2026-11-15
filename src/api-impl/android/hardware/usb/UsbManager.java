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
}
