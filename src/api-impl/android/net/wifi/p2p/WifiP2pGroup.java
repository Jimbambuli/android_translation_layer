package android.net.wifi.p2p;

import android.net.MacAddress;
import android.os.Parcel;
import android.os.Parcelable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/* AOSP packages/modules/Wifi/framework/java/android/net/wifi/p2p/WifiP2pGroup.java: a group as built by the
 * no-argument constructor, with the plain getters and setters, the copy constructor and the Parcelable code
 * (without the vendor data, a system API). The supplicant-event parser is not ported. */
public class WifiP2pGroup implements Parcelable {
	public static final int NETWORK_ID_PERSISTENT = -2;
	public static final int NETWORK_ID_TEMPORARY = -1;

	public static final int SECURITY_TYPE_UNKNOWN = -1;
	public static final int SECURITY_TYPE_WPA2_PSK = 0;
	public static final int SECURITY_TYPE_WPA3_COMPATIBILITY = 1;
	public static final int SECURITY_TYPE_WPA3_SAE = 2;

	public byte[] interfaceAddress;

	private List<WifiP2pDevice> mClients = new ArrayList<WifiP2pDevice>();
	private WifiP2pDevice mOwner;
	private boolean mIsGroupOwner;
	private String mNetworkName;
	private String mPassphrase;
	private String mInterface;
	private int mSecurityType;
	private int mNetId;
	private int mFrequency;

	public WifiP2pGroup() {}

	public WifiP2pGroup(WifiP2pGroup source) {
		if (source != null) {
			mNetworkName = source.getNetworkName();
			mOwner = new WifiP2pDevice(source.getOwner());
			mIsGroupOwner = source.mIsGroupOwner;
			for (WifiP2pDevice d : source.getClientList()) {
				mClients.add(d);
			}
			mPassphrase = source.getPassphrase();
			mInterface = source.getInterface();
			mNetId = source.getNetworkId();
			mSecurityType = source.getSecurityType();
			mFrequency = source.getFrequency();
		}
	}

	public void setNetworkName(String networkName) {
		mNetworkName = networkName;
	}

	public String getNetworkName() {
		return mNetworkName;
	}

	public void setIsGroupOwner(boolean isGo) {
		mIsGroupOwner = isGo;
	}

	public boolean isGroupOwner() {
		return mIsGroupOwner;
	}

	public void setOwner(WifiP2pDevice device) {
		mOwner = device;
	}

	public WifiP2pDevice getOwner() {
		return mOwner;
	}

	public Collection<WifiP2pDevice> getClientList() {
		return Collections.unmodifiableCollection(mClients);
	}

	public void setPassphrase(String passphrase) {
		mPassphrase = passphrase;
	}

	public String getPassphrase() {
		return mPassphrase;
	}

	public void setInterface(String intf) {
		mInterface = intf;
	}

	public String getInterface() {
		return mInterface;
	}

	public int getNetworkId() {
		return mNetId;
	}

	public void setNetworkId(int netId) {
		mNetId = netId;
	}

	public int getFrequency() {
		return mFrequency;
	}

	public void setFrequency(int freq) {
		mFrequency = freq;
	}

	public int getSecurityType() {
		return mSecurityType;
	}

	/* getGroupOwnerBssid() */
	public MacAddress getGroupOwnerBssid() {
		if (isGroupOwner() && getSecurityType() == SECURITY_TYPE_WPA3_SAE && interfaceAddress != null) {
			return MacAddress.fromBytes(interfaceAddress);
		}
		return null;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(mNetworkName);
		dest.writeParcelable(mOwner, flags);
		dest.writeByte(mIsGroupOwner ? (byte) 1 : (byte) 0);
		dest.writeInt(mClients.size());
		for (WifiP2pDevice client : mClients) {
			dest.writeParcelable(client, flags);
		}
		dest.writeString(mPassphrase);
		dest.writeString(mInterface);
		dest.writeInt(mNetId);
		dest.writeInt(mSecurityType);
		dest.writeInt(mFrequency);
	}

	public static final Creator<WifiP2pGroup> CREATOR = new Creator<WifiP2pGroup>() {
		public WifiP2pGroup createFromParcel(Parcel in) {
			WifiP2pGroup group = new WifiP2pGroup();
			group.setNetworkName(in.readString());
			group.setOwner((WifiP2pDevice) in.readParcelable(WifiP2pDevice.class.getClassLoader()));
			group.setIsGroupOwner(in.readByte() == (byte) 1);
			int clientCount = in.readInt();
			for (int i = 0; i < clientCount; i++) {
				WifiP2pDevice client = (WifiP2pDevice) in.readParcelable(WifiP2pDevice.class.getClassLoader());
				/* AOSP addClient(WifiP2pDevice): no duplicates */
				if (!group.mClients.contains(client)) {
					group.mClients.add(new WifiP2pDevice(client));
				}
			}
			group.setPassphrase(in.readString());
			group.setInterface(in.readString());
			group.setNetworkId(in.readInt());
			group.mSecurityType = in.readInt();
			group.setFrequency(in.readInt());
			return group;
		}

		public WifiP2pGroup[] newArray(int size) {
			return new WifiP2pGroup[size];
		}
	};
}
