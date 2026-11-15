package android.net.wifi.p2p;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/* AOSP packages/modules/Wifi/framework/java/android/net/wifi/p2p/WifiP2pGroup.java: a group as built by the
 * no-argument constructor, with the plain getters and setters. The supplicant-event parser is not ported. */
public class WifiP2pGroup {
	public static final int NETWORK_ID_PERSISTENT = -2;
	public static final int NETWORK_ID_TEMPORARY = -1;

	private List<WifiP2pDevice> mClients = new ArrayList<WifiP2pDevice>();
	private WifiP2pDevice mOwner;
	private boolean mIsGroupOwner;
	private String mNetworkName;
	private String mPassphrase;
	private String mInterface;
	private int mNetId;
	private int mFrequency;

	public WifiP2pGroup() {}

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
}
