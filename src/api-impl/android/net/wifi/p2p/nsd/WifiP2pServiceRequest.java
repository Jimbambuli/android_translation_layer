package android.net.wifi.p2p.nsd;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.Locale;
import java.util.Objects;

/* AOSP packages/modules/Wifi/framework/java/android/net/wifi/p2p/nsd/WifiP2pServiceRequest.java: common base of
 * the service requests, with the query validation, the USD-based service config, equals/hashCode and the
 * Parcelable code. Not ported: the hidden transaction id, USD session id and getSupplicantQuery(). */
public class WifiP2pServiceRequest implements Parcelable {
	private int mProtocolType;
	private int mLength;
	private int mTransId;
	private String mQuery;
	private WifiP2pUsdBasedServiceConfig mUsdServiceConfig;

	protected WifiP2pServiceRequest(int protocolType, String query) {
		validateQuery(query);
		mProtocolType = protocolType;
		mQuery = query;
		if (query != null) {
			mLength = query.length() / 2 + 2;
		} else {
			mLength = 2;
		}
	}

	private WifiP2pServiceRequest(int serviceType, int length, int transId, String query, WifiP2pUsdBasedServiceConfig usdConfig) {
		mProtocolType = serviceType;
		mLength = length;
		mTransId = transId;
		mQuery = query;
		mUsdServiceConfig = usdConfig;
	}

	public WifiP2pServiceRequest(WifiP2pUsdBasedServiceConfig usdConfig) {
		Objects.requireNonNull(usdConfig, "usdConfig cannot be null");
		mUsdServiceConfig = usdConfig;
	}

	public WifiP2pUsdBasedServiceConfig getWifiP2pUsdBasedServiceConfig() {
		return mUsdServiceConfig;
	}

	private void validateQuery(String query) {
		if (query == null) {
			return;
		}
		int UNSIGNED_SHORT_MAX = 0xffff;
		if (query.length() % 2 == 1) {
			throw new IllegalArgumentException("query size is invalid. query=" + query);
		}
		if (query.length() / 2 > UNSIGNED_SHORT_MAX) {
			throw new IllegalArgumentException("query size is too large. len=" + query.length());
		}
		query = query.toLowerCase(Locale.ROOT);
		char[] chars = query.toCharArray();
		for (char c : chars) {
			if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f'))) {
				throw new IllegalArgumentException("query should be hex string. query=" + query);
			}
		}
	}

	public static WifiP2pServiceRequest newInstance(int protocolType, String queryData) {
		return new WifiP2pServiceRequest(protocolType, queryData);
	}

	public static WifiP2pServiceRequest newInstance(int protocolType) {
		return new WifiP2pServiceRequest(protocolType, null);
	}

	public boolean equals(Object o) {
		if (o == this) {
			return true;
		}
		if (!(o instanceof WifiP2pServiceRequest)) {
			return false;
		}
		WifiP2pServiceRequest req = (WifiP2pServiceRequest) o;
		return mProtocolType == req.mProtocolType && mLength == req.mLength && Objects.equals(mQuery, req.mQuery) && Objects.equals(mUsdServiceConfig, req.mUsdServiceConfig);
	}

	public int hashCode() {
		int result = 17;
		result = 31 * result + mProtocolType;
		result = 31 * result + mLength;
		result = 31 * result + (mQuery == null ? 0 : mQuery.hashCode());
		result = 31 * result + (mUsdServiceConfig == null ? 0 : mUsdServiceConfig.hashCode());
		return result;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(mProtocolType);
		dest.writeInt(mLength);
		dest.writeInt(mTransId);
		dest.writeString(mQuery);
		dest.writeParcelable(mUsdServiceConfig, flags);
		/* AOSP also writes the hidden USD session id (0 unless set) */
		dest.writeInt(0);
	}

	public static final Creator<WifiP2pServiceRequest> CREATOR = new Creator<WifiP2pServiceRequest>() {
		public WifiP2pServiceRequest createFromParcel(Parcel in) {
			int servType = in.readInt();
			int length = in.readInt();
			int transId = in.readInt();
			String query = in.readString();
			WifiP2pUsdBasedServiceConfig config = (WifiP2pUsdBasedServiceConfig) in.readParcelable(WifiP2pUsdBasedServiceConfig.class.getClassLoader());
			in.readInt();
			return new WifiP2pServiceRequest(servType, length, transId, query, config);
		}

		public WifiP2pServiceRequest[] newArray(int size) {
			return new WifiP2pServiceRequest[size];
		}
	};
}
