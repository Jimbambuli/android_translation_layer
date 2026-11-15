package android.net.wifi.p2p;

import android.net.MacAddress;
import android.os.Parcel;
import android.os.Parcelable;
import java.util.Arrays;

/* AOSP packages/modules/Wifi/framework/java/android/net/wifi/p2p/WifiP2pDirInfo.java */
public final class WifiP2pDirInfo implements Parcelable {
	private MacAddress mMacAddress;
	private byte[] mNonce;
	private byte[] mDirTag;

	public MacAddress getMacAddress() {
		return mMacAddress;
	}

	public byte[] getNonce() {
		return mNonce;
	}

	public byte[] getDirTag() {
		return mDirTag;
	}

	public WifiP2pDirInfo(MacAddress macAddress, byte[] nonce, byte[] dirTag) {
		mMacAddress = macAddress;
		mNonce = nonce;
		mDirTag = dirTag;
	}

	public String toString() {
		StringBuilder sbuf = new StringBuilder("WifiP2pDirInfo:");
		sbuf.append("\n Mac Address: ").append(mMacAddress);
		sbuf.append("\n Nonce : ").append((mNonce == null) ? "<null>" : Arrays.toString(mNonce));
		sbuf.append("\n DIR Tag : ").append((mDirTag == null) ? "<null>" : Arrays.toString(mDirTag));
		return sbuf.toString();
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		mMacAddress.writeToParcel(dest, flags);
		dest.writeByteArray(mNonce);
		dest.writeByteArray(mDirTag);
	}

	public static final Creator<WifiP2pDirInfo> CREATOR = new Creator<WifiP2pDirInfo>() {
		public WifiP2pDirInfo createFromParcel(Parcel in) {
			return new WifiP2pDirInfo(MacAddress.CREATOR.createFromParcel(in), in.createByteArray(), in.createByteArray());
		}

		public WifiP2pDirInfo[] newArray(int size) {
			return new WifiP2pDirInfo[size];
		}
	};
}
