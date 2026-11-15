package android.net.wifi.p2p;

import java.net.InetAddress;

/* AOSP packages/modules/Wifi/framework/java/android/net/wifi/p2p/WifiP2pInfo.java: the public fields */
public class WifiP2pInfo implements android.os.Parcelable {
	public boolean groupFormed;
	public boolean isGroupOwner;
	public InetAddress groupOwnerAddress;

	public WifiP2pInfo() {}

	/* AOSP WifiP2pInfo(WifiP2pInfo) copy constructor, describeContents()/writeToParcel()/CREATOR */
	public WifiP2pInfo(WifiP2pInfo source) {
		if (source != null) {
			groupFormed = source.groupFormed;
			isGroupOwner = source.isGroupOwner;
			groupOwnerAddress = source.groupOwnerAddress;
		}
	}

	public int describeContents() {
		return 0;
	}

	public void writeToParcel(android.os.Parcel dest, int flags) {
		dest.writeByte(groupFormed ? (byte)1 : (byte)0);
		dest.writeByte(isGroupOwner ? (byte)1 : (byte)0);
		if (groupOwnerAddress != null) {
			dest.writeByte((byte)1);
			dest.writeByteArray(groupOwnerAddress.getAddress());
		} else {
			dest.writeByte((byte)0);
		}
	}

	public static final android.os.Parcelable.Creator<WifiP2pInfo> CREATOR = new android.os.Parcelable.Creator<WifiP2pInfo>() {
		public WifiP2pInfo createFromParcel(android.os.Parcel in) {
			WifiP2pInfo info = new WifiP2pInfo();
			info.groupFormed = (in.readByte() == 1);
			info.isGroupOwner = (in.readByte() == 1);
			if (in.readByte() == 1) {
				try {
					info.groupOwnerAddress = InetAddress.getByAddress(in.createByteArray());
				} catch (java.net.UnknownHostException e) {}
			}
			return info;
		}

		public WifiP2pInfo[] newArray(int size) {
			return new WifiP2pInfo[size];
		}
	};
}
