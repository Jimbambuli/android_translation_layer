package android.net.wifi.p2p.nsd;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import java.util.Arrays;
import java.util.Objects;

/* AOSP packages/modules/Wifi/framework/java/android/net/wifi/p2p/nsd/WifiP2pUsdBasedServiceConfig.java */
public final class WifiP2pUsdBasedServiceConfig implements Parcelable {
	private static final int SERVICE_SPECIFIC_INFO_MAXIMUM_LENGTH = 1024;

	public static final int SERVICE_PROTOCOL_TYPE_BONJOUR = 1;
	public static final int SERVICE_PROTOCOL_TYPE_GENERIC = 2;

	private int mServiceProtocolType;
	private String mServiceName;
	private byte[] mServiceSpecificInfo;

	public WifiP2pUsdBasedServiceConfig() {}

	public int getServiceProtocolType() {
		return mServiceProtocolType;
	}

	public String getServiceName() {
		return mServiceName;
	}

	public byte[] getServiceSpecificInfo() {
		return mServiceSpecificInfo;
	}

	public static int getMaxAllowedServiceSpecificInfoLength() {
		return SERVICE_SPECIFIC_INFO_MAXIMUM_LENGTH;
	}

	public String toString() {
		StringBuilder sbuf = new StringBuilder("WifiP2pUsdBasedServiceConfig:");
		sbuf.append("\n Protocol type: ").append(mServiceProtocolType);
		sbuf.append("\n Service name : ").append(mServiceName);
		sbuf.append("\n Service specific info : ").append((mServiceSpecificInfo == null) ? "<null>" : Arrays.toString(mServiceSpecificInfo));
		return sbuf.toString();
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(mServiceProtocolType);
		dest.writeString(mServiceName);
		dest.writeByteArray(mServiceSpecificInfo);
	}

	public static final Creator<WifiP2pUsdBasedServiceConfig> CREATOR = new Creator<WifiP2pUsdBasedServiceConfig>() {
		public WifiP2pUsdBasedServiceConfig createFromParcel(Parcel in) {
			WifiP2pUsdBasedServiceConfig config = new WifiP2pUsdBasedServiceConfig();
			config.mServiceProtocolType = in.readInt();
			config.mServiceName = in.readString();
			config.mServiceSpecificInfo = in.createByteArray();
			return config;
		}

		public WifiP2pUsdBasedServiceConfig[] newArray(int size) {
			return new WifiP2pUsdBasedServiceConfig[size];
		}
	};

	public static final class Builder {
		private static final int SERVICE_NAME_MAXIMUM_LENGTH = 100;

		private int mServiceProtocolType = SERVICE_PROTOCOL_TYPE_GENERIC;
		private String mServiceName;
		byte[] mServiceSpecificInfo;

		public Builder(String serviceName) {
			Objects.requireNonNull(serviceName, "Service name cannot be null");
			if (TextUtils.isEmpty(serviceName)) {
				throw new IllegalArgumentException("Service name cannot be empty!");
			}
			if (serviceName.length() > SERVICE_NAME_MAXIMUM_LENGTH) {
				throw new IllegalArgumentException("Service name length: " + serviceName.length() + " must be less than " + SERVICE_NAME_MAXIMUM_LENGTH);
			}
			mServiceName = serviceName;
		}

		public Builder setServiceProtocolType(int serviceProtocolType) {
			if (serviceProtocolType < 0 || serviceProtocolType > 255) {
				throw new IllegalArgumentException("serviceProtocolType must be between 0-255 (inclusive)");
			}
			mServiceProtocolType = serviceProtocolType;
			return this;
		}

		public Builder setServiceSpecificInfo(byte[] serviceSpecificInfo) {
			if (serviceSpecificInfo != null && serviceSpecificInfo.length > getMaxAllowedServiceSpecificInfoLength()) {
				throw new IllegalArgumentException("Service specific info length: " + serviceSpecificInfo.length + " must be less than " + getMaxAllowedServiceSpecificInfoLength());
			}
			mServiceSpecificInfo = serviceSpecificInfo;
			return this;
		}

		public WifiP2pUsdBasedServiceConfig build() {
			if (TextUtils.isEmpty(mServiceName)) {
				throw new IllegalStateException("Service name must be non-empty");
			}
			WifiP2pUsdBasedServiceConfig config = new WifiP2pUsdBasedServiceConfig();
			config.mServiceName = mServiceName;
			config.mServiceProtocolType = mServiceProtocolType;
			config.mServiceSpecificInfo = mServiceSpecificInfo;
			return config;
		}
	}
}
