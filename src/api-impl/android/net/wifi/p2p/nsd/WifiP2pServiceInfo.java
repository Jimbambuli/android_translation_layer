package android.net.wifi.p2p.nsd;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/* AOSP packages/modules/Wifi/framework/java/android/net/wifi/p2p/nsd/WifiP2pServiceInfo.java: common base of the
 * service descriptions, with the query list, the USD-based service config, equals/hashCode and the Parcelable
 * code. Not ported: the hidden USD session id and getSupplicantQueryList(). */
public class WifiP2pServiceInfo implements Parcelable {
	public static final int SERVICE_TYPE_ALL = 0;
	public static final int SERVICE_TYPE_BONJOUR = 1;
	public static final int SERVICE_TYPE_UPNP = 2;
	public static final int SERVICE_TYPE_VENDOR_SPECIFIC = 255;

	private List<String> mQueryList;
	private WifiP2pUsdBasedServiceConfig mUsdServiceConfig;

	protected WifiP2pServiceInfo(List<String> queryList) {
		if (queryList == null) {
			throw new IllegalArgumentException("query list cannot be null");
		}
		mQueryList = queryList;
	}

	private WifiP2pServiceInfo(List<String> queryList, WifiP2pUsdBasedServiceConfig usdConfig) {
		mQueryList = queryList;
		mUsdServiceConfig = usdConfig;
	}

	public WifiP2pServiceInfo(WifiP2pUsdBasedServiceConfig usdConfig) {
		Objects.requireNonNull(usdConfig, "usd based service config cannot be null");
		mUsdServiceConfig = usdConfig;
	}

	public WifiP2pUsdBasedServiceConfig getWifiP2pUsdBasedServiceConfig() {
		return mUsdServiceConfig;
	}

	static String bin2HexStr(byte[] data) {
		StringBuffer sb = new StringBuffer();
		for (byte b : data) {
			String s = Integer.toHexString(b & 0xff);
			if (s.length() == 1) {
				sb.append('0');
			}
			sb.append(s);
		}
		return sb.toString();
	}

	public boolean equals(Object o) {
		if (o == this) {
			return true;
		}
		if (!(o instanceof WifiP2pServiceInfo)) {
			return false;
		}
		WifiP2pServiceInfo servInfo = (WifiP2pServiceInfo) o;
		return Objects.equals(mQueryList, servInfo.mQueryList) && Objects.equals(mUsdServiceConfig, servInfo.mUsdServiceConfig);
	}

	public int hashCode() {
		int result = 17;
		result = 31 * result + (mQueryList == null ? 0 : mQueryList.hashCode());
		result = 31 * result + (mUsdServiceConfig == null ? 0 : mUsdServiceConfig.hashCode());
		return result;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeStringList(mQueryList);
		dest.writeParcelable(mUsdServiceConfig, flags);
		/* AOSP also writes the hidden USD session id (0 unless set) */
		dest.writeInt(0);
	}

	public static final Creator<WifiP2pServiceInfo> CREATOR = new Creator<WifiP2pServiceInfo>() {
		public WifiP2pServiceInfo createFromParcel(Parcel in) {
			List<String> data = new ArrayList<String>();
			in.readStringList(data);
			WifiP2pUsdBasedServiceConfig config = (WifiP2pUsdBasedServiceConfig) in.readParcelable(WifiP2pUsdBasedServiceConfig.class.getClassLoader());
			in.readInt();
			return new WifiP2pServiceInfo(data, config);
		}

		public WifiP2pServiceInfo[] newArray(int size) {
			return new WifiP2pServiceInfo[size];
		}
	};
}
