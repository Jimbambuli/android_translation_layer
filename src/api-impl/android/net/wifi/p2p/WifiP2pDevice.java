package android.net.wifi.p2p;

import android.net.wifi.ScanResult;
import android.os.Parcel;
import android.os.Parcelable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/* AOSP packages/modules/Wifi/framework/java/android/net/wifi/p2p/WifiP2pDevice.java: the public fields and
 * getters, the copy constructor and the Parcelable code. Not ported: the supplicant-event parser, the interface
 * MAC address, the vendor data and dirInfo (system API). The vendor elements are parcelled by hand, because
 * ATL's ScanResult.InformationElement is not Parcelable. */
public class WifiP2pDevice implements Parcelable {
	public String deviceName = "";
	public String deviceAddress = "";
	private InetAddress mIpAddress;
	public String primaryDeviceType;
	public String secondaryDeviceType;

	private static final int WPS_CONFIG_DISPLAY = 0x0008;
	private static final int WPS_CONFIG_PUSHBUTTON = 0x0080;
	private static final int WPS_CONFIG_KEYPAD = 0x0100;

	private static final int DEVICE_CAPAB_SERVICE_DISCOVERY = 1;
	private static final int GROUP_CAPAB_GROUP_OWNER = 1;

	public int wpsConfigMethodsSupported;
	public int deviceCapability;
	public int groupCapability;
	public WifiP2pWfdInfo wfdInfo;

	public static final int CONNECTED = 0;
	public static final int INVITED = 1;
	public static final int FAILED = 2;
	public static final int AVAILABLE = 3;
	public static final int UNAVAILABLE = 4;

	public int status = UNAVAILABLE;

	private List<ScanResult.InformationElement> mVendorElements;
	private int mPairingBootstrappingMethods;

	public WifiP2pDevice() {}

	public WifiP2pDevice(WifiP2pDevice source) {
		if (source != null) {
			deviceName = source.deviceName;
			deviceAddress = source.deviceAddress;
			mIpAddress = source.mIpAddress;
			primaryDeviceType = source.primaryDeviceType;
			secondaryDeviceType = source.secondaryDeviceType;
			wpsConfigMethodsSupported = source.wpsConfigMethodsSupported;
			deviceCapability = source.deviceCapability;
			groupCapability = source.groupCapability;
			status = source.status;
			if (source.wfdInfo != null) {
				wfdInfo = new WifiP2pWfdInfo(source.wfdInfo);
			}
			if (null != source.mVendorElements) {
				mVendorElements = new ArrayList<>(source.mVendorElements);
			}
			mPairingBootstrappingMethods = source.mPairingBootstrappingMethods;
		}
	}

	public WifiP2pWfdInfo getWfdInfo() {
		return wfdInfo;
	}

	public boolean wpsPbcSupported() {
		return (wpsConfigMethodsSupported & WPS_CONFIG_PUSHBUTTON) != 0;
	}

	public boolean wpsKeypadSupported() {
		return (wpsConfigMethodsSupported & WPS_CONFIG_KEYPAD) != 0;
	}

	public boolean wpsDisplaySupported() {
		return (wpsConfigMethodsSupported & WPS_CONFIG_DISPLAY) != 0;
	}

	public boolean isServiceDiscoveryCapable() {
		return (deviceCapability & DEVICE_CAPAB_SERVICE_DISCOVERY) != 0;
	}

	public boolean isGroupOwner() {
		return (groupCapability & GROUP_CAPAB_GROUP_OWNER) != 0;
	}

	public void update(WifiP2pDevice device) {
		updateSupplicantDetails(device);
		status = device.status;
	}

	public void updateSupplicantDetails(WifiP2pDevice device) {
		if (device == null) {
			throw new IllegalArgumentException("device is null");
		}
		if (device.deviceAddress == null) {
			throw new IllegalArgumentException("deviceAddress is null");
		}
		if (!deviceAddress.equals(device.deviceAddress)) {
			throw new IllegalArgumentException("deviceAddress does not match");
		}
		deviceName = device.deviceName;
		primaryDeviceType = device.primaryDeviceType;
		secondaryDeviceType = device.secondaryDeviceType;
		wpsConfigMethodsSupported = device.wpsConfigMethodsSupported;
		deviceCapability = device.deviceCapability;
		groupCapability = device.groupCapability;
		wfdInfo = device.wfdInfo;
	}

	public List<ScanResult.InformationElement> getVendorElements() {
		if (mVendorElements == null) {
			return Collections.emptyList();
		}
		return new ArrayList<>(mVendorElements);
	}

	public InetAddress getIpAddress() {
		return mIpAddress;
	}

	public boolean isOpportunisticBootstrappingMethodSupported() {
		return (mPairingBootstrappingMethods & WifiP2pPairingBootstrappingConfig.PAIRING_BOOTSTRAPPING_METHOD_OPPORTUNISTIC) != 0;
	}

	public boolean isPinCodeDisplayBootstrappingMethodSupported() {
		return (mPairingBootstrappingMethods & WifiP2pPairingBootstrappingConfig.PAIRING_BOOTSTRAPPING_METHOD_DISPLAY_PINCODE) != 0;
	}

	public boolean isPassphraseDisplayBootstrappingMethodSupported() {
		return (mPairingBootstrappingMethods & WifiP2pPairingBootstrappingConfig.PAIRING_BOOTSTRAPPING_METHOD_DISPLAY_PASSPHRASE) != 0;
	}

	public boolean isPinCodeKeypadBootstrappingMethodSupported() {
		return (mPairingBootstrappingMethods & WifiP2pPairingBootstrappingConfig.PAIRING_BOOTSTRAPPING_METHOD_KEYPAD_PINCODE) != 0;
	}

	public boolean isPassphraseKeypadBootstrappingMethodSupported() {
		return (mPairingBootstrappingMethods & WifiP2pPairingBootstrappingConfig.PAIRING_BOOTSTRAPPING_METHOD_KEYPAD_PASSPHRASE) != 0;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof WifiP2pDevice)) {
			return false;
		}
		WifiP2pDevice other = (WifiP2pDevice) obj;
		if (other.deviceAddress == null) {
			return (deviceAddress == null);
		}
		return other.deviceAddress.equals(deviceAddress);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(deviceAddress);
	}

	@Override
	public String toString() {
		StringBuffer sbuf = new StringBuffer();
		sbuf.append("Device: ").append(deviceName);
		sbuf.append("\n deviceAddress: ").append(deviceAddress);
		sbuf.append("\n ipAddress: ").append(mIpAddress == null ? "none" : mIpAddress.getHostAddress());
		sbuf.append("\n primary type: ").append(primaryDeviceType);
		sbuf.append("\n secondary type: ").append(secondaryDeviceType);
		sbuf.append("\n wps: ").append(wpsConfigMethodsSupported);
		sbuf.append("\n grpcapab: ").append(groupCapability);
		sbuf.append("\n devcapab: ").append(deviceCapability);
		sbuf.append("\n status: ").append(status);
		sbuf.append("\n wfdInfo: ").append(wfdInfo);
		sbuf.append("\n Pairing Bootstrapping Methods: ").append(mPairingBootstrappingMethods);
		return sbuf.toString();
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(deviceName);
		dest.writeString(deviceAddress);
		if (mIpAddress != null) {
			dest.writeByte((byte) 1);
			dest.writeByteArray(mIpAddress.getAddress());
		} else {
			dest.writeByte((byte) 0);
		}
		dest.writeString(primaryDeviceType);
		dest.writeString(secondaryDeviceType);
		dest.writeInt(wpsConfigMethodsSupported);
		dest.writeInt(deviceCapability);
		dest.writeInt(groupCapability);
		dest.writeInt(status);
		if (wfdInfo != null) {
			dest.writeInt(1);
			wfdInfo.writeToParcel(dest, flags);
		} else {
			dest.writeInt(0);
		}
		if (mVendorElements == null) {
			dest.writeInt(-1);
		} else {
			dest.writeInt(mVendorElements.size());
			for (ScanResult.InformationElement ie : mVendorElements) {
				dest.writeInt(ie.id);
				dest.writeInt(ie.idExt);
				dest.writeByteArray(ie.bytes);
			}
		}
		dest.writeInt(mPairingBootstrappingMethods);
	}

	public static final Creator<WifiP2pDevice> CREATOR = new Creator<WifiP2pDevice>() {
		@Override
		public WifiP2pDevice createFromParcel(Parcel in) {
			WifiP2pDevice device = new WifiP2pDevice();
			device.deviceName = in.readString();
			device.deviceAddress = in.readString();
			if (in.readByte() == 1) {
				try {
					device.mIpAddress = InetAddress.getByAddress(in.createByteArray());
				} catch (UnknownHostException e) {
					e.printStackTrace();
					return new WifiP2pDevice();
				}
			}
			device.primaryDeviceType = in.readString();
			device.secondaryDeviceType = in.readString();
			device.wpsConfigMethodsSupported = in.readInt();
			device.deviceCapability = in.readInt();
			device.groupCapability = in.readInt();
			device.status = in.readInt();
			if (in.readInt() == 1) {
				device.wfdInfo = WifiP2pWfdInfo.CREATOR.createFromParcel(in);
			}
			int count = in.readInt();
			if (count >= 0) {
				device.mVendorElements = new ArrayList<>(count);
				for (int i = 0; i < count; i++) {
					ScanResult.InformationElement ie = new ScanResult.InformationElement();
					ie.id = in.readInt();
					ie.idExt = in.readInt();
					ie.bytes = in.createByteArray();
					device.mVendorElements.add(ie);
				}
			}
			device.mPairingBootstrappingMethods = in.readInt();
			return device;
		}

		@Override
		public WifiP2pDevice[] newArray(int size) {
			return new WifiP2pDevice[size];
		}
	};
}
