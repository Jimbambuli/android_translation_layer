package android.net.wifi.p2p;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

/* AOSP packages/modules/Wifi/framework/java/android/net/wifi/p2p/WifiP2pDeviceList.java (public API plus the
 * helpers the Parcelable code needs; dirInfo and pairing methods are not copied, see WifiP2pDevice) */
public class WifiP2pDeviceList implements Parcelable {
	private final HashMap<String, WifiP2pDevice> mDevices = new HashMap<String, WifiP2pDevice>();

	public WifiP2pDeviceList() {}

	public WifiP2pDeviceList(WifiP2pDeviceList source) {
		if (source != null) {
			for (WifiP2pDevice d : source.getDeviceList()) {
				mDevices.put(d.deviceAddress, new WifiP2pDevice(d));
			}
		}
	}

	private void validateDevice(WifiP2pDevice device) {
		if (device == null) {
			throw new IllegalArgumentException("Null device");
		}
		if (TextUtils.isEmpty(device.deviceAddress)) {
			throw new IllegalArgumentException("Empty deviceAddress");
		}
	}

	private void validateDeviceAddress(String deviceAddress) {
		if (TextUtils.isEmpty(deviceAddress)) {
			throw new IllegalArgumentException("Empty deviceAddress");
		}
	}

	private void update(WifiP2pDevice device) {
		updateSupplicantDetails(device);
		mDevices.get(device.deviceAddress).status = device.status;
	}

	private void updateSupplicantDetails(WifiP2pDevice device) {
		validateDevice(device);
		WifiP2pDevice d = mDevices.get(device.deviceAddress);
		if (d != null) {
			d.deviceName = device.deviceName;
			d.primaryDeviceType = device.primaryDeviceType;
			d.secondaryDeviceType = device.secondaryDeviceType;
			d.wpsConfigMethodsSupported = device.wpsConfigMethodsSupported;
			d.deviceCapability = device.deviceCapability;
			d.groupCapability = device.groupCapability;
			d.wfdInfo = device.wfdInfo;
			return;
		}
		mDevices.put(device.deviceAddress, device);
	}

	public WifiP2pDevice get(String deviceAddress) {
		validateDeviceAddress(deviceAddress);
		return mDevices.get(deviceAddress);
	}

	public Collection<WifiP2pDevice> getDeviceList() {
		return Collections.unmodifiableCollection(mDevices.values());
	}

	public String toString() {
		StringBuffer sbuf = new StringBuffer();
		for (WifiP2pDevice device : mDevices.values()) {
			sbuf.append("\n").append(device);
		}
		return sbuf.toString();
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(mDevices.size());
		for (WifiP2pDevice device : mDevices.values()) {
			dest.writeParcelable(device, flags);
		}
	}

	public static final Creator<WifiP2pDeviceList> CREATOR = new Creator<WifiP2pDeviceList>() {
		public WifiP2pDeviceList createFromParcel(Parcel in) {
			WifiP2pDeviceList deviceList = new WifiP2pDeviceList();
			int deviceCount = in.readInt();
			for (int i = 0; i < deviceCount; i++) {
				deviceList.update((WifiP2pDevice) in.readParcelable(null));
			}
			return deviceList;
		}

		public WifiP2pDeviceList[] newArray(int size) {
			return new WifiP2pDeviceList[size];
		}
	};
}
