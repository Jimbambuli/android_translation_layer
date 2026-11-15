package android.net.wifi.p2p;

import android.os.Parcel;
import android.os.Parcelable;

/* AOSP packages/modules/Wifi/framework/java/android/net/wifi/p2p/WifiP2pWfdInfo.java (public API, with
 * setR2DeviceType: ATL reports Android 16, SDK level S or higher) */
public final class WifiP2pWfdInfo implements Parcelable {
	private boolean mEnabled;
	private int mDeviceInfo;
	private int mR2DeviceInfo = -1;

	public static final int DEVICE_TYPE_WFD_SOURCE = 0;
	public static final int DEVICE_TYPE_PRIMARY_SINK = 1;
	public static final int DEVICE_TYPE_SECONDARY_SINK = 2;
	public static final int DEVICE_TYPE_SOURCE_OR_PRIMARY_SINK = 3;

	public static final int PREFERRED_CONNECTIVITY_P2P = 0;
	public static final int PREFERRED_CONNECTIVITY_TDLS = 1;

	public static final int DEVICE_INFO_DEVICE_TYPE_MASK = 1 << 1 | 1 << 0;
	public static final int DEVICE_INFO_COUPLED_SINK_SUPPORT_AT_SOURCE = 1 << 2;
	public static final int DEVICE_INFO_COUPLED_SINK_SUPPORT_AT_SINK = 1 << 3;
	private static final int SESSION_AVAILABLE_BIT1 = 1 << 4;
	private static final int SESSION_AVAILABLE_BIT2 = 1 << 5;
	public static final int DEVICE_INFO_SESSION_AVAILABLE_MASK = SESSION_AVAILABLE_BIT2 | SESSION_AVAILABLE_BIT1;
	public static final int DEVICE_INFO_WFD_SERVICE_DISCOVERY_SUPPORT = 1 << 6;
	public static final int DEVICE_INFO_PREFERRED_CONNECTIVITY_MASK = 1 << 7;
	public static final int DEVICE_INFO_CONTENT_PROTECTION_SUPPORT = 1 << 8;
	public static final int DEVICE_INFO_TIME_SYNCHRONIZATION_SUPPORT = 1 << 9;
	public static final int DEVICE_INFO_AUDIO_UNSUPPORTED_AT_PRIMARY_SINK = 1 << 10;
	public static final int DEVICE_INFO_AUDIO_ONLY_SUPPORT_AT_SOURCE = 1 << 11;
	public static final int DEVICE_INFO_TDLS_PERSISTENT_GROUP = 1 << 12;
	public static final int DEVICE_INFO_TDLS_PERSISTENT_GROUP_REINVOKE = 1 << 13;

	private int mCtrlPort;
	private int mMaxThroughput;

	public WifiP2pWfdInfo() {}

	public WifiP2pWfdInfo(WifiP2pWfdInfo source) {
		if (source != null) {
			mEnabled = source.mEnabled;
			mDeviceInfo = source.mDeviceInfo;
			mCtrlPort = source.mCtrlPort;
			mMaxThroughput = source.mMaxThroughput;
			mR2DeviceInfo = source.mR2DeviceInfo;
		}
	}

	public int getDeviceInfo() {
		return mDeviceInfo;
	}

	public int getR2DeviceInfo() {
		return mR2DeviceInfo;
	}

	public boolean isEnabled() {
		return mEnabled;
	}

	public boolean isR2Supported() {
		return mR2DeviceInfo >= 0;
	}

	public void setEnabled(boolean enabled) {
		mEnabled = enabled;
	}

	public boolean setR2DeviceType(int deviceType) {
		if (DEVICE_TYPE_WFD_SOURCE != deviceType && DEVICE_TYPE_PRIMARY_SINK != deviceType && DEVICE_TYPE_SOURCE_OR_PRIMARY_SINK != deviceType) {
			return false;
		}
		if (!isR2Supported()) {
			mR2DeviceInfo = 0;
		}
		mR2DeviceInfo &= ~DEVICE_INFO_DEVICE_TYPE_MASK;
		mR2DeviceInfo |= deviceType;
		return true;
	}

	public int getDeviceType() {
		return mDeviceInfo & DEVICE_INFO_DEVICE_TYPE_MASK;
	}

	public int getR2DeviceType() {
		return mR2DeviceInfo & DEVICE_INFO_DEVICE_TYPE_MASK;
	}

	public boolean setDeviceType(int deviceType) {
		if (DEVICE_TYPE_WFD_SOURCE <= deviceType && deviceType <= DEVICE_TYPE_SOURCE_OR_PRIMARY_SINK) {
			mDeviceInfo &= ~DEVICE_INFO_DEVICE_TYPE_MASK;
			mDeviceInfo |= deviceType;
			return true;
		}
		return false;
	}

	public boolean isSessionAvailable() {
		return (mDeviceInfo & DEVICE_INFO_SESSION_AVAILABLE_MASK) != 0;
	}

	public void setSessionAvailable(boolean enabled) {
		if (enabled) {
			mDeviceInfo |= SESSION_AVAILABLE_BIT1;
			mDeviceInfo &= ~SESSION_AVAILABLE_BIT2;
		} else {
			mDeviceInfo &= ~DEVICE_INFO_SESSION_AVAILABLE_MASK;
		}
	}

	public boolean isContentProtectionSupported() {
		return (mDeviceInfo & DEVICE_INFO_CONTENT_PROTECTION_SUPPORT) != 0;
	}

	public void setContentProtectionSupported(boolean enabled) {
		if (enabled) {
			mDeviceInfo |= DEVICE_INFO_CONTENT_PROTECTION_SUPPORT;
		} else {
			mDeviceInfo &= ~DEVICE_INFO_CONTENT_PROTECTION_SUPPORT;
		}
	}

	public boolean isCoupledSinkSupportedAtSource() {
		return (mDeviceInfo & DEVICE_INFO_COUPLED_SINK_SUPPORT_AT_SOURCE) != 0;
	}

	public void setCoupledSinkSupportAtSource(boolean enabled) {
		if (enabled) {
			mDeviceInfo |= DEVICE_INFO_COUPLED_SINK_SUPPORT_AT_SOURCE;
		} else {
			mDeviceInfo &= ~DEVICE_INFO_COUPLED_SINK_SUPPORT_AT_SOURCE;
		}
	}

	public boolean isCoupledSinkSupportedAtSink() {
		return (mDeviceInfo & DEVICE_INFO_COUPLED_SINK_SUPPORT_AT_SINK) != 0;
	}

	public void setCoupledSinkSupportAtSink(boolean enabled) {
		if (enabled) {
			mDeviceInfo |= DEVICE_INFO_COUPLED_SINK_SUPPORT_AT_SINK;
		} else {
			mDeviceInfo &= ~DEVICE_INFO_COUPLED_SINK_SUPPORT_AT_SINK;
		}
	}

	public int getControlPort() {
		return mCtrlPort;
	}

	public void setControlPort(int port) {
		mCtrlPort = port;
	}

	public void setMaxThroughput(int maxThroughput) {
		mMaxThroughput = maxThroughput;
	}

	public int getMaxThroughput() {
		return mMaxThroughput;
	}

	@Override
	public String toString() {
		StringBuffer sbuf = new StringBuffer();
		sbuf.append("WFD enabled: ").append(mEnabled);
		sbuf.append("\n WFD DeviceInfo: ").append(mDeviceInfo);
		sbuf.append("\n WFD CtrlPort: ").append(mCtrlPort);
		sbuf.append("\n WFD MaxThroughput: ").append(mMaxThroughput);
		sbuf.append("\n WFD R2 DeviceInfo: ").append(mR2DeviceInfo);
		return sbuf.toString();
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(mEnabled ? 1 : 0);
		dest.writeInt(mDeviceInfo);
		dest.writeInt(mCtrlPort);
		dest.writeInt(mMaxThroughput);
		dest.writeInt(mR2DeviceInfo);
	}

	private void readFromParcel(Parcel in) {
		mEnabled = (in.readInt() == 1);
		mDeviceInfo = in.readInt();
		mCtrlPort = in.readInt();
		mMaxThroughput = in.readInt();
		mR2DeviceInfo = in.readInt();
	}

	public static final Creator<WifiP2pWfdInfo> CREATOR = new Creator<WifiP2pWfdInfo>() {
		public WifiP2pWfdInfo createFromParcel(Parcel in) {
			WifiP2pWfdInfo device = new WifiP2pWfdInfo();
			device.readFromParcel(in);
			return device;
		}

		public WifiP2pWfdInfo[] newArray(int size) {
			return new WifiP2pWfdInfo[size];
		}
	};
}
