package android.net.wifi.aware;

import android.annotation.NonNull;
import android.annotation.Nullable;
import java.util.List;

/* AOSP packages/modules/Wifi/framework/java/android/net/wifi/aware/ServiceDiscoveryInfo.java */
public final class ServiceDiscoveryInfo {
	private final byte[] mServiceSpecificInfo;
	private final List<byte[]> mMatchFilters;
	private final int mPeerCipherSuite;
	private final byte[] mScid;
	private final PeerHandle mPeerHandle;
	private final String mPairingAlias;
	private final AwarePairingConfig mPairingConfig;

	/* AOSP additionally takes OuiKeyedData[] vendorData (SystemApi, not in this implementation) */
	public ServiceDiscoveryInfo(PeerHandle peerHandle, int peerCipherSuite,
			@Nullable byte[] serviceSpecificInfo, @NonNull List<byte[]> matchFilter,
			@Nullable byte[] scid, String pairingAlias, AwarePairingConfig pairingConfig) {
		mServiceSpecificInfo = serviceSpecificInfo;
		mMatchFilters = matchFilter;
		mPeerCipherSuite = peerCipherSuite;
		mScid = scid;
		mPeerHandle = peerHandle;
		mPairingAlias = pairingAlias;
		mPairingConfig = pairingConfig;
	}

	@NonNull
	public PeerHandle getPeerHandle() {
		return mPeerHandle;
	}

	@NonNull
	public List<byte[]> getMatchFilters() {
		return mMatchFilters;
	}

	@Nullable
	public byte[] getServiceSpecificInfo() {
		return mServiceSpecificInfo;
	}

	@Nullable
	public byte[] getScid() {
		return mScid;
	}

	public int getPeerCipherSuite() {
		return mPeerCipherSuite;
	}

	@Nullable
	public String getPairedAlias() {
		return mPairingAlias;
	}

	@Nullable
	public AwarePairingConfig getPairingConfig() {
		return mPairingConfig;
	}
}
