package android.net.wifi.p2p.nsd;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.Arrays;

/* AOSP packages/modules/Wifi/framework/java/android/net/wifi/p2p/nsd/WifiP2pUsdBasedServiceResponse.java */
public final class WifiP2pUsdBasedServiceResponse implements Parcelable {
	private int mServiceProtocolType = -1;
	private byte[] mServiceSpecificInfo;

	/* hidden in AOSP, public there as well */
	public WifiP2pUsdBasedServiceResponse(int serviceProtocolType, byte[] serviceSpecificInfo) {
		mServiceProtocolType = serviceProtocolType;
		mServiceSpecificInfo = serviceSpecificInfo;
	}

	public int getServiceProtocolType() {
		return mServiceProtocolType;
	}

	public byte[] getServiceSpecificInfo() {
		return mServiceSpecificInfo;
	}

	public String toString() {
		StringBuilder sbuf = new StringBuilder("WifiP2pUsdBasedServiceResponse:");
		sbuf.append("\n Protocol type: ").append(mServiceProtocolType);
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
		dest.writeByteArray(mServiceSpecificInfo);
	}

	public static final Creator<WifiP2pUsdBasedServiceResponse> CREATOR = new Creator<WifiP2pUsdBasedServiceResponse>() {
		public WifiP2pUsdBasedServiceResponse createFromParcel(Parcel in) {
			int serviceProtocolType = in.readInt();
			byte[] ssi = in.createByteArray();
			return new WifiP2pUsdBasedServiceResponse(serviceProtocolType, ssi);
		}

		public WifiP2pUsdBasedServiceResponse[] newArray(int size) {
			return new WifiP2pUsdBasedServiceResponse[size];
		}
	};
}
