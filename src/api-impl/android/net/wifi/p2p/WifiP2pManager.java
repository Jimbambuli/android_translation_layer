package android.net.wifi.p2p;

import android.content.Context;
import android.net.MacAddress;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.p2p.nsd.WifiP2pServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pServiceRequest;
import android.net.wifi.p2p.nsd.WifiP2pUsdBasedServiceResponse;
import android.os.Looper;
import android.os.OutcomeReceiver;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/* AOSP packages/modules/Wifi/framework/java/android/net/wifi/p2p/WifiP2pManager.java: ATL has no Wi-Fi P2P service.
 * initialize() gets no messenger from it and returns null (initializeChannel: "if (messenger == null) return null"),
 * and every other call first runs checkChannel(), which throws for that null channel. A Channel therefore never
 * exists, and no call gets past checkChannel(). Without the service getSupportedFeatures() has no feature bit,
 * so every isXSupported() is false and the calls AOSP gates on it throw UnsupportedOperationException before
 * checkChannel(). registerWifiP2pListener() has nothing to register with and no event ever comes, so it
 * does nothing, unregisterWifiP2pListener() is the AOSP "unknown listener" no-op. */
public class WifiP2pManager {
	public static final String WIFI_P2P_STATE_CHANGED_ACTION = "STATE_CHANGED";
	public static final String EXTRA_WIFI_STATE = "wifi_p2p_state";
	public static final int WIFI_P2P_STATE_DISABLED = 1;
	public static final int WIFI_P2P_STATE_ENABLED = 2;
	public static final String WIFI_P2P_DISCOVERY_CHANGED_ACTION = "DISCOVERY_STATE_CHANGE";
	public static final String EXTRA_DISCOVERY_STATE = "discoveryState";
	public static final int WIFI_P2P_DISCOVERY_STOPPED = 1;
	public static final int WIFI_P2P_DISCOVERY_STARTED = 2;
	public static final String ACTION_WIFI_P2P_LISTEN_STATE_CHANGED = "action.WIFI_P2P_LISTEN_STATE_CHANGED";
	public static final String EXTRA_LISTEN_STATE = "extra.LISTEN_STATE";
	public static final int WIFI_P2P_LISTEN_STOPPED = 1;
	public static final int WIFI_P2P_LISTEN_STARTED = 2;
	public static final String ACTION_WIFI_P2P_REQUEST_RESPONSE_CHANGED = "action.WIFI_P2P_REQUEST_RESPONSE_CHANGED";
	public static final String EXTRA_REQUEST_CONFIG = "extra.REQUEST_CONFIG";
	public static final String EXTRA_REQUEST_RESPONSE = "extra.REQUEST_RESPONSE";
	public static final String WIFI_P2P_PEERS_CHANGED_ACTION = "PEERS_CHANGED";
	public static final String EXTRA_P2P_DEVICE_LIST = "wifiP2pDeviceList";
	public static final String WIFI_P2P_CONNECTION_CHANGED_ACTION = "CONNECTION_STATE_CHANGE";
	public static final String EXTRA_WIFI_P2P_INFO = "wifiP2pInfo";
	public static final String EXTRA_NETWORK_INFO = "networkInfo";
	public static final String EXTRA_WIFI_P2P_GROUP = "p2pGroupInfo";
	public static final String WIFI_P2P_THIS_DEVICE_CHANGED_ACTION = "THIS_DEVICE_CHANGED";
	public static final String EXTRA_WIFI_P2P_DEVICE = "wifiP2pDevice";

	public static final int ERROR = 0;
	public static final int P2P_UNSUPPORTED = 1;
	public static final int BUSY = 2;
	public static final int NO_SERVICE_REQUESTS = 3;
	public static final int NO_PERMISSION = 4;

	public static final int WIFI_P2P_SCAN_FULL = 0;
	public static final int WIFI_P2P_SCAN_SOCIAL = 1;
	public static final int WIFI_P2P_SCAN_SINGLE_FREQ = 2;

	public static final int CONNECTION_REQUEST_ACCEPT = 0;
	public static final int CONNECTION_REQUEST_REJECT = 1;
	public static final int CONNECTION_REQUEST_DEFER_TO_SERVICE = 2;
	public static final int CONNECTION_REQUEST_DEFER_SHOW_PIN_TO_SERVICE = 3;
	public static final int CONNECTION_REQUEST_DEFER_SHOW_PASSWORD_TO_SERVICE = 4;

	public static final int GROUP_CREATION_FAILURE_REASON_CONNECTION_CANCELLED = 0;
	public static final int GROUP_CREATION_FAILURE_REASON_TIMED_OUT = 1;
	public static final int GROUP_CREATION_FAILURE_REASON_USER_REJECTED = 2;
	public static final int GROUP_CREATION_FAILURE_REASON_PROVISION_DISCOVERY_FAILED = 3;
	public static final int GROUP_CREATION_FAILURE_REASON_GROUP_REMOVED = 4;
	public static final int GROUP_CREATION_FAILURE_REASON_INVITATION_FAILED = 5;

	private static final int WIFI_P2P_VENDOR_ELEMENTS_MAXIMUM_LENGTH = 512;

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

	public interface DeviceInfoListener {
		public void onDeviceInfoAvailable(WifiP2pDevice wifiP2pDevice);
	}

	public interface DiscoveryStateListener {
		public void onDiscoveryStateAvailable(int state);
	}

	public interface NetworkInfoListener {
		public void onNetworkInfoAvailable(NetworkInfo networkInfo);
	}

	public interface P2pStateListener {
		public void onP2pStateAvailable(int state);
	}

	public interface PeerListListener {
		public void onPeersAvailable(WifiP2pDeviceList peers);
	}

	public interface ServiceResponseListener {
		public void onServiceAvailable(int protocolType, byte[] responseData, WifiP2pDevice srcDevice);

		public default void onUsdBasedServiceAvailable(WifiP2pDevice srcDevice, WifiP2pUsdBasedServiceResponse usdResponseData) {}
	}

	public interface UpnpServiceResponseListener {
		public void onUpnpServiceAvailable(List<String> uniqueServiceNames, WifiP2pDevice srcDevice);
	}

	public interface ExternalApproverRequestListener {
		public static final int REQUEST_TYPE_NEGOTIATION = 0;
		public static final int REQUEST_TYPE_INVITATION = 1;
		public static final int REQUEST_TYPE_JOIN = 2;

		public static final int APPROVER_DETACH_REASON_REMOVE = 0;
		public static final int APPROVER_DETACH_REASON_FAILURE = 1;
		public static final int APPROVER_DETACH_REASON_REPLACE = 2;
		public static final int APPROVER_DETACH_REASON_CLOSE = 3;

		public void onAttached(MacAddress deviceAddress);

		public void onDetached(MacAddress deviceAddress, int reason);

		public void onConnectionRequested(int requestType, WifiP2pConfig config, WifiP2pDevice device);

		public void onPinGenerated(MacAddress deviceAddress, String pin);

		public default void onPasswordGenerated(MacAddress deviceAddress, String password) {}
	}

	public interface WifiP2pListener {
		public default void onP2pStateChanged(int state) {}

		public default void onDiscoveryStateChanged(int state) {}

		public default void onListenStateChanged(int state) {}

		public default void onDeviceConfigurationChanged(WifiP2pDevice p2pDevice) {}

		public default void onPeerListChanged(WifiP2pDeviceList peers) {}

		public default void onGroupCreating() {}

		public default void onGroupNegotiationRejectedByUser() {}

		public default void onGroupCreationFailed(int reason) {}

		public default void onGroupCreated(WifiP2pInfo info, WifiP2pGroup group) {}

		public default void onPeerClientJoined(WifiP2pInfo info, WifiP2pGroup group) {}

		public default void onPeerClientDisconnected(WifiP2pInfo info, WifiP2pGroup group) {}

		public default void onFrequencyChanged(WifiP2pInfo info, WifiP2pGroup group) {}

		public default void onGroupRemoved() {}
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

	/* getSupportedFeatures() is 0 without the service, see the class comment */
	public boolean isSetVendorElementsSupported() {
		return false;
	}

	public boolean isChannelConstrainedDiscoverySupported() {
		return false;
	}

	public boolean isGroupClientRemovalSupported() {
		return false;
	}

	public boolean isGroupOwnerIPv6LinkLocalAddressProvided() {
		return false;
	}

	public boolean isWiFiDirectR2Supported() {
		return false;
	}

	public boolean isPccModeSupported() {
		return false;
	}

	public static int getP2pMaxAllowedVendorElementsLengthBytes() {
		return WIFI_P2P_VENDOR_ELEMENTS_MAXIMUM_LENGTH;
	}

	public void registerWifiP2pListener(Executor executor, WifiP2pListener listener) {}

	public void unregisterWifiP2pListener(WifiP2pListener listener) {}

	public void discoverPeersOnSocialChannels(Channel channel, ActionListener listener) {
		if (!isChannelConstrainedDiscoverySupported()) {
			throw new UnsupportedOperationException();
		}
		checkChannel(channel);
	}

	public void discoverPeersOnSpecificFrequency(Channel channel, int frequencyMhz, ActionListener listener) {
		if (!isChannelConstrainedDiscoverySupported()) {
			throw new UnsupportedOperationException();
		}
		checkChannel(channel);
		if (frequencyMhz <= 0) {
			throw new IllegalArgumentException("This frequency must be a positive value.");
		}
	}

	public void startPeerDiscovery(Channel channel, WifiP2pDiscoveryConfig config, ActionListener listener) {
		if (!isChannelConstrainedDiscoverySupported()) {
			throw new UnsupportedOperationException();
		}
		checkChannel(channel);
		Objects.requireNonNull(config);
	}

	public void startListening(Channel channel, ActionListener listener) {
		checkChannel(channel);
	}

	public void stopListening(Channel channel, ActionListener listener) {
		checkChannel(channel);
	}

	public void getListenState(Channel c, Executor executor, Consumer<Integer> resultsCallback) {
		Objects.requireNonNull(c, "channel cannot be null and needs to be initialized)");
		Objects.requireNonNull(executor, "executor cannot be null");
		Objects.requireNonNull(resultsCallback, "resultsCallback cannot be null");
	}

	public void startUsdBasedLocalServiceAdvertisement(Channel channel, WifiP2pServiceInfo servInfo, WifiP2pUsdBasedLocalServiceAdvertisementConfig config, ActionListener listener) {
		if (!isWiFiDirectR2Supported()) {
			throw new UnsupportedOperationException();
		}
		checkChannel(channel);
		Objects.requireNonNull(servInfo, "service info cannot be null");
		Objects.requireNonNull(config, "Advertisement config cannot be null");
	}

	public void discoverUsdBasedServices(Channel channel, WifiP2pUsdBasedServiceDiscoveryConfig config, ActionListener listener) {
		if (!isWiFiDirectR2Supported()) {
			throw new UnsupportedOperationException();
		}
		checkChannel(channel);
		Objects.requireNonNull(config, "Service discovery config cannot be null");
	}

	public void removeLocalService(Channel channel, WifiP2pServiceInfo servInfo, ActionListener listener) {
		checkChannel(channel);
		checkServiceInfo(servInfo);
	}

	public void setServiceResponseListener(Channel channel, ServiceResponseListener listener) {
		checkChannel(channel);
	}

	public void setUpnpServiceResponseListener(Channel channel, UpnpServiceResponseListener listener) {
		checkChannel(channel);
	}

	public void setWfdInfo(Channel channel, WifiP2pWfdInfo wfdInfo, ActionListener listener) {
		checkChannel(channel);
	}

	public void removeClient(Channel channel, MacAddress peerAddress, ActionListener listener) {
		if (!isGroupClientRemovalSupported()) {
			throw new UnsupportedOperationException();
		}
		checkChannel(channel);
	}

	public void requestPeers(Channel channel, PeerListListener listener) {
		checkChannel(channel);
	}

	public void requestP2pState(Channel c, P2pStateListener listener) {
		checkChannel(c);
		if (listener == null) {
			throw new IllegalArgumentException("This listener cannot be null.");
		}
	}

	public void requestDiscoveryState(Channel c, DiscoveryStateListener listener) {
		checkChannel(c);
		if (listener == null) {
			throw new IllegalArgumentException("This listener cannot be null.");
		}
	}

	public void requestNetworkInfo(Channel c, NetworkInfoListener listener) {
		checkChannel(c);
		if (listener == null) {
			throw new IllegalArgumentException("This listener cannot be null.");
		}
	}

	public void requestDeviceInfo(Channel c, DeviceInfoListener listener) {
		checkChannel(c);
		if (listener == null) {
			throw new IllegalArgumentException("This listener cannot be null.");
		}
	}

	public void addExternalApprover(Channel c, MacAddress deviceAddress, ExternalApproverRequestListener listener) {
		checkChannel(c);
		if (listener == null) {
			throw new IllegalArgumentException("This listener cannot be null.");
		}
		if (null == deviceAddress) {
			throw new IllegalArgumentException("deviceAddress cannot be empty");
		}
	}

	public void removeExternalApprover(Channel c, MacAddress deviceAddress, ActionListener listener) {
		checkChannel(c);
		if (null == deviceAddress) {
			throw new IllegalArgumentException("deviceAddress cannot be empty");
		}
	}

	public void setConnectionRequestResult(Channel c, MacAddress deviceAddress, int result, ActionListener listener) {
		checkChannel(c);
		if (null == deviceAddress) {
			throw new IllegalArgumentException("deviceAddress cannot be empty");
		}
	}

	public void setConnectionRequestResult(Channel c, MacAddress deviceAddress, int result, String pinOrPassword, ActionListener listener) {
		checkChannel(c);
		if (null == deviceAddress) {
			throw new IllegalArgumentException("deviceAddress cannot be empty");
		}
		if (result == CONNECTION_REQUEST_ACCEPT && (pinOrPassword == null || pinOrPassword.isEmpty())) {
			throw new IllegalArgumentException("PIN cannot be empty for accepting a request");
		}
	}

	public void setVendorElements(Channel c, List<ScanResult.InformationElement> vendorElements, ActionListener listener) {
		if (!isSetVendorElementsSupported()) {
			throw new UnsupportedOperationException();
		}
		checkChannel(c);
	}

	public void requestDirInfo(Channel c, Executor executor, OutcomeReceiver<WifiP2pDirInfo, Exception> callback) {
		if (!isWiFiDirectR2Supported()) {
			throw new UnsupportedOperationException();
		}
		Objects.requireNonNull(c, "channel cannot be null and needs to be initialized)");
		Objects.requireNonNull(executor, "executor cannot be null");
		Objects.requireNonNull(callback, "callback cannot be null");
	}

	public void validateDirInfo(Channel c, WifiP2pDirInfo dirInfo, Executor executor, OutcomeReceiver<Boolean, Exception> callback) {
		if (!isWiFiDirectR2Supported()) {
			throw new UnsupportedOperationException();
		}
		Objects.requireNonNull(c, "channel cannot be null and needs to be initialized)");
		Objects.requireNonNull(dirInfo, "dirInfo cannot be null");
		Objects.requireNonNull(executor, "executor cannot be null");
		Objects.requireNonNull(callback, "resultsCallback cannot be null");
	}
}
