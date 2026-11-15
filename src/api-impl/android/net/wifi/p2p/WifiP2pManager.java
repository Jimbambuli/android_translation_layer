package android.net.wifi.p2p;

import android.content.Context;
import android.net.wifi.p2p.nsd.WifiP2pServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pServiceRequest;
import android.os.Looper;
import java.util.Map;

/* AOSP packages/modules/Wifi/framework/java/android/net/wifi/p2p/WifiP2pManager.java: ATL has no Wi-Fi P2P service.
 * initialize() gets no messenger from it and returns null (initializeChannel: "if (messenger == null) return null"),
 * and every other call first runs checkChannel(), which throws for that null channel. A Channel therefore never
 * exists, and no call gets past checkChannel(). */
public class WifiP2pManager {
	public static final int ERROR = 0;
	public static final int P2P_UNSUPPORTED = 1;
	public static final int BUSY = 2;
	public static final int NO_SERVICE_REQUESTS = 3;

	public interface ChannelListener {
		public void onChannelDisconnected();
	}

	public interface ActionListener {
		public void onSuccess();

		public void onFailure(int reason);
	}

	public interface ConnectionInfoListener {
		public void onConnectionInfoAvailable(WifiP2pInfo info);
	}

	public interface GroupInfoListener {
		public void onGroupInfoAvailable(WifiP2pGroup group);
	}

	public interface DnsSdServiceResponseListener {
		public void onDnsSdServiceAvailable(String instanceName, String registrationType, WifiP2pDevice srcDevice);
	}

	public interface DnsSdTxtRecordListener {
		public void onDnsSdTxtRecordAvailable(String fullDomainName, Map<String, String> txtRecordMap, WifiP2pDevice srcDevice);
	}

	public static final class Channel implements AutoCloseable {
		private Channel() {}

		@Override
		public void close() {}
	}

	private static void checkChannel(Channel c) {
		if (c == null) {
			throw new IllegalArgumentException("Channel needs to be initialized");
		}
	}

	private static void checkServiceInfo(WifiP2pServiceInfo info) {
		if (info == null) {
			throw new IllegalArgumentException("service info is null");
		}
	}

	private static void checkServiceRequest(WifiP2pServiceRequest req) {
		if (req == null) {
			throw new IllegalArgumentException("service request is null");
		}
	}

	public Channel initialize(Context srcContext, Looper srcLooper, ChannelListener listener) {
		return null;
	}

	public void discoverPeers(Channel channel, ActionListener listener) {
		checkChannel(channel);
	}

	public void stopPeerDiscovery(Channel channel, ActionListener listener) {
		checkChannel(channel);
	}

	public void connect(Channel channel, WifiP2pConfig config, ActionListener listener) {
		checkChannel(channel);
		if (config == null) {
			throw new IllegalArgumentException("config cannot be null");
		}
		if (config.deviceAddress == null || config.deviceAddress.isEmpty()) {
			throw new IllegalArgumentException("deviceAddress cannot be empty");
		}
	}

	public void cancelConnect(Channel channel, ActionListener listener) {
		checkChannel(channel);
	}

	public void createGroup(Channel channel, ActionListener listener) {
		checkChannel(channel);
	}

	public void createGroup(Channel channel, WifiP2pConfig config, ActionListener listener) {
		checkChannel(channel);
	}

	public void removeGroup(Channel channel, ActionListener listener) {
		checkChannel(channel);
	}

	public void addLocalService(Channel channel, WifiP2pServiceInfo servInfo, ActionListener listener) {
		checkChannel(channel);
		checkServiceInfo(servInfo);
	}

	public void clearLocalServices(Channel channel, ActionListener listener) {
		checkChannel(channel);
	}

	public void setDnsSdResponseListeners(Channel channel, DnsSdServiceResponseListener servListener, DnsSdTxtRecordListener txtListener) {
		checkChannel(channel);
	}

	public void discoverServices(Channel channel, ActionListener listener) {
		checkChannel(channel);
	}

	public void addServiceRequest(Channel channel, WifiP2pServiceRequest req, ActionListener listener) {
		checkChannel(channel);
		checkServiceRequest(req);
	}

	public void removeServiceRequest(Channel channel, WifiP2pServiceRequest req, ActionListener listener) {
		checkChannel(channel);
		checkServiceRequest(req);
	}

	public void clearServiceRequests(Channel channel, ActionListener listener) {
		checkChannel(channel);
	}

	public void requestConnectionInfo(Channel channel, ConnectionInfoListener listener) {
		checkChannel(channel);
	}

	public void requestGroupInfo(Channel channel, GroupInfoListener listener) {
		checkChannel(channel);
	}
}
