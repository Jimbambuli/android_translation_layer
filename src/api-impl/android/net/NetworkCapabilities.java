package android.net;

public final class NetworkCapabilities implements android.os.Parcelable {
	public static final int NET_CAPABILITY_MMS = 0;
	public static final int NET_CAPABILITY_SUPL = 1;
	public static final int NET_CAPABILITY_DUN = 2;
	public static final int NET_CAPABILITY_FOTA = 3;
	public static final int NET_CAPABILITY_IMS = 4;
	public static final int NET_CAPABILITY_CBS = 5;
	public static final int NET_CAPABILITY_WIFI_P2P = 6;
	public static final int NET_CAPABILITY_IA = 7;
	public static final int NET_CAPABILITY_RCS = 8;
	public static final int NET_CAPABILITY_XCAP = 9;
	public static final int NET_CAPABILITY_EIMS = 10;
	public static final int NET_CAPABILITY_NOT_METERED = 11;
	public static final int NET_CAPABILITY_INTERNET = 12;
	public static final int NET_CAPABILITY_NOT_RESTRICTED = 13;
	public static final int NET_CAPABILITY_TRUSTED = 14;
	public static final int NET_CAPABILITY_NOT_VPN = 15;
	public static final int NET_CAPABILITY_VALIDATED = 16;
	public static final int NET_CAPABILITY_CAPTIVE_PORTAL = 17;
	public static final int NET_CAPABILITY_NOT_ROAMING = 18;
	public static final int NET_CAPABILITY_FOREGROUND = 19;
	public static final int NET_CAPABILITY_NOT_CONGESTED = 20;
	public static final int NET_CAPABILITY_NOT_SUSPENDED = 21;
	public static final int NET_CAPABILITY_OEM_PAID = 22;
	public static final int NET_CAPABILITY_MCX = 23;
	public static final int NET_CAPABILITY_PARTIAL_CONNECTIVITY = 24;
	public static final int NET_CAPABILITY_TEMPORARILY_NOT_METERED = 25;
	public static final int NET_CAPABILITY_OEM_PRIVATE = 26;
	public static final int NET_CAPABILITY_VEHICLE_INTERNAL = 27;
	public static final int NET_CAPABILITY_NOT_VCN_MANAGED = 28;
	public static final int NET_CAPABILITY_ENTERPRISE = 29;
	public static final int NET_CAPABILITY_VSIM = 30;
	public static final int NET_CAPABILITY_BIP = 31;
	public static final int NET_CAPABILITY_HEAD_UNIT = 32;
	public static final int NET_CAPABILITY_MMTEL = 33;
	public static final int NET_CAPABILITY_PRIORITIZE_LATENCY = 34;
	public static final int NET_CAPABILITY_PRIORITIZE_BANDWIDTH = 35;

	public static final int TRANSPORT_CELLULAR = 0;
	public static final int TRANSPORT_WIFI = 1;
	public static final int TRANSPORT_BLUETOOTH = 2;
	public static final int TRANSPORT_ETHERNET = 3;
	public static final int TRANSPORT_VPN = 4;
	public static final int TRANSPORT_WIFI_AWARE = 5;
	public static final int TRANSPORT_LOWPAN = 6;
	public static final int TRANSPORT_TEST = 7;
	public static final int TRANSPORT_USB = 8;
	public static final int TRANSPORT_THREAD = 9;
	public static final int TRANSPORT_SATELLITE = 10;

	private static final long NOT_RESTRICTED = 1L << NET_CAPABILITY_NOT_RESTRICTED;
	private static final long TRUSTED = 1L << NET_CAPABILITY_TRUSTED;
	private static final long NOT_VPN = 1L << NET_CAPABILITY_NOT_VPN;

	private static final int MIN_NET_CAPABILITY = NET_CAPABILITY_MMS;
	private static final int MAX_NET_CAPABILITY = NET_CAPABILITY_PRIORITIZE_BANDWIDTH;

	private static final int MIN_TRANSPORT = TRANSPORT_CELLULAR;
	private static final int MAX_TRANSPORT = TRANSPORT_SATELLITE;

	// AOSP sets these on every freshly constructed instance
	private static final long DEFAULT_CAPABILITIES = NOT_RESTRICTED | TRUSTED | NOT_VPN;

	private long mNetworkCapabilities = DEFAULT_CAPABILITIES;
	private long mTransportTypes;
	private int mLinkUpBandwidthKbps;
	private int mLinkDownBandwidthKbps;
	private int mSignalStrength = SIGNAL_STRENGTH_UNSPECIFIED;
	private NetworkSpecifier mNetworkSpecifier;

	public static final int SIGNAL_STRENGTH_UNSPECIFIED = Integer.MIN_VALUE;

	public NetworkCapabilities() {
	}

	public NetworkCapabilities(NetworkCapabilities nc) {
		if (nc != null) {
			mNetworkCapabilities = nc.mNetworkCapabilities;
			mTransportTypes = nc.mTransportTypes;
			mLinkUpBandwidthKbps = nc.mLinkUpBandwidthKbps;
			mLinkDownBandwidthKbps = nc.mLinkDownBandwidthKbps;
			mSignalStrength = nc.mSignalStrength;
			mNetworkSpecifier = nc.mNetworkSpecifier;
		}
	}

	/** @hide AOSP NetworkCapabilities.clearAll() */
	public void clearAll() {
		mNetworkCapabilities = 0;
		mTransportTypes = 0;
		mLinkUpBandwidthKbps = 0;
		mLinkDownBandwidthKbps = 0;
		mSignalStrength = SIGNAL_STRENGTH_UNSPECIFIED;
		mNetworkSpecifier = null;
	}

	/** @hide AOSP NetworkCapabilities.setNetworkSpecifier(): a specifier needs a single non-test transport */
	public NetworkCapabilities setNetworkSpecifier(NetworkSpecifier networkSpecifier) {
		if (networkSpecifier != null
		    && mTransportTypes != (1L << TRANSPORT_TEST)
		    && Long.bitCount(mTransportTypes & ~(1L << TRANSPORT_TEST)) != 1
		    && (mTransportTypes & ~(1L << TRANSPORT_TEST)) != (1 << TRANSPORT_CELLULAR | 1 << TRANSPORT_SATELLITE)) {
			throw new IllegalStateException("Must have a single non-test transport specified to use setNetworkSpecifier");
		}
		mNetworkSpecifier = networkSpecifier;
		return this;
	}

	public NetworkSpecifier getNetworkSpecifier() {
		return mNetworkSpecifier;
	}

	/* AOSP BitUtils.unpackBits() */
	private static int[] unpackBits(long val) {
		int size = Long.bitCount(val);
		int[] result = new int[size];
		int index = 0;
		int bitPos = 0;
		while (val != 0) {
			if ((val & 1) == 1)
				result[index++] = bitPos;
			val = val >>> 1;
			bitPos++;
		}
		return result;
	}

	public int[] getCapabilities() {
		return unpackBits(mNetworkCapabilities);
	}

	public int[] getTransportTypes() {
		return unpackBits(mTransportTypes);
	}

	/** @hide AOSP satisfiedByNetCapabilities() and satisfiedByTransportTypes(), the parts kept here */
	public boolean satisfiedByNetworkCapabilities(NetworkCapabilities nc) {
		return nc != null
		    && (nc.mNetworkCapabilities & mNetworkCapabilities) == mNetworkCapabilities
		    && (mTransportTypes == 0 || (mTransportTypes & nc.mTransportTypes) != 0)
		    && (mNetworkSpecifier == null || mNetworkSpecifier.canBeSatisfiedBy(nc.mNetworkSpecifier));
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof NetworkCapabilities))
			return false;
		NetworkCapabilities that = (NetworkCapabilities)obj;
		return mNetworkCapabilities == that.mNetworkCapabilities && mTransportTypes == that.mTransportTypes
		    && mLinkUpBandwidthKbps == that.mLinkUpBandwidthKbps && mLinkDownBandwidthKbps == that.mLinkDownBandwidthKbps
		    && mSignalStrength == that.mSignalStrength && java.util.Objects.equals(mNetworkSpecifier, that.mNetworkSpecifier);
	}

	@Override
	public int hashCode() {
		return (int)(mNetworkCapabilities ^ (mNetworkCapabilities >>> 32)) * 31 + (int)mTransportTypes;
	}

	@Override
	public String toString() {
		return "NetworkCapabilities: Capabilities: " + java.util.Arrays.toString(getCapabilities())
		    + " Transports: " + java.util.Arrays.toString(getTransportTypes());
	}

	public boolean hasCapability(int capability) {
		if (!isValidCapability(capability))
			return false;
		return (mNetworkCapabilities & (1L << capability)) != 0;
	}

	public boolean hasTransport(int transportType) {
		return isValidTransport(transportType) && ((mTransportTypes & (1L << transportType)) != 0);
	}

	public NetworkCapabilities setLinkUpstreamBandwidthKbps(int upKbps) {
		mLinkUpBandwidthKbps = upKbps;
		return this;
	}

	public int getLinkUpstreamBandwidthKbps() {
		return mLinkUpBandwidthKbps;
	}

	public NetworkCapabilities setLinkDownstreamBandwidthKbps(int downKbps) {
		mLinkDownBandwidthKbps = downKbps;
		return this;
	}

	public int getLinkDownstreamBandwidthKbps() {
		return mLinkDownBandwidthKbps;
	}

	public int getSignalStrength() {
		return mSignalStrength;
	}

	public NetworkCapabilities addCapability(int capability) {
		checkValidCapability(capability);
		mNetworkCapabilities |= 1L << capability;
		return this;
	}

	public NetworkCapabilities removeCapability(int capability) {
		checkValidCapability(capability);
		mNetworkCapabilities &= ~(1L << capability);
		return this;
	}

	public NetworkCapabilities addTransportType(int transportType) {
		checkValidTransportType(transportType);
		mTransportTypes |= 1L << transportType;
		return this;
	}

	public NetworkCapabilities removeTransportType(int transportType) {
		checkValidTransportType(transportType);
		mTransportTypes &= ~(1L << transportType);
		return this;
	}

	private static boolean isValidCapability(int capability) {
		return capability >= MIN_NET_CAPABILITY && capability <= MAX_NET_CAPABILITY;
	}

	private static void checkValidCapability(int capability) {
		if (!isValidCapability(capability))
			throw new IllegalArgumentException("NetworkCapability " + capability + " out of range");
	}

	private static boolean isValidTransport(int transportType) {
		return transportType >= MIN_TRANSPORT && transportType <= MAX_TRANSPORT;
	}

	private static void checkValidTransportType(int transportType) {
		if (!isValidTransport(transportType))
			throw new IllegalArgumentException("Invalid TransportType " + transportType);
	}

	/* mTransportInfo / mOwnerUid at their AOSP defaults: ATL's networks carry no transport info and no owner
	 * (Process.INVALID_UID is -1) */
	public TransportInfo getTransportInfo() {
		return null;
	}

	public int getOwnerUid() {
		return -1;
	}

	/* AOSP NetworkCapabilities.describeContents()/writeToParcel()/CREATOR (Connectivity framework), layout of
	 * Android 16. ATL keeps capabilities, transports, bandwidths, signal strength and the specifier; the
	 * rest is written as on an AOSP NetworkCapabilities that has none of it (no forbidden capabilities,
	 * transport info, uids, SSID, admin uids, subscription ids or underlying networks, owner/requestor
	 * INVALID_UID, enterprise id 0, RES_ID_UNSET) and skipped when read. AOSP masks with
	 * ALL_VALID_CAPABILITIES/ALL_VALID_TRANSPORTS; ATL keeps the values as they are. A specifier that
	 * is not Parcelable is written as null. */
	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(android.os.Parcel dest, int flags) {
		dest.writeLong(mNetworkCapabilities);
		dest.writeLong(0L); // mForbiddenNetworkCapabilities
		dest.writeLong(mTransportTypes);
		dest.writeInt(mLinkUpBandwidthKbps);
		dest.writeInt(mLinkDownBandwidthKbps);
		dest.writeParcelable(mNetworkSpecifier instanceof android.os.Parcelable ? (android.os.Parcelable)mNetworkSpecifier : null, flags);
		dest.writeParcelable(null, flags); // mTransportInfo
		dest.writeInt(mSignalStrength);
		dest.writeInt(-1); // mUids: null
		dest.writeIntArray(new int[0]); // mAllowedUids
		dest.writeString(null); // mSSID
		dest.writeBoolean(false); // mPrivateDnsBroken
		dest.writeIntArray(new int[0]); // administrator uids
		dest.writeInt(-1); // mOwnerUid: Process.INVALID_UID
		dest.writeInt(-1); // mRequestorUid: Process.INVALID_UID
		dest.writeString(null); // mRequestorPackageName
		dest.writeIntArray(new int[0]); // mSubIds
		dest.writeTypedList((java.util.List<Network>)null); // mUnderlyingNetworks
		dest.writeInt(0); // mEnterpriseId
		dest.writeInt(0); // mReservationId: RES_ID_UNSET
		dest.writeBoolean(false); // mMatchNonThreadLocalNetworks
	}

	public static final android.os.Parcelable.Creator<NetworkCapabilities> CREATOR = new android.os.Parcelable.Creator<NetworkCapabilities>() {
		@Override
		public NetworkCapabilities createFromParcel(android.os.Parcel in) {
			NetworkCapabilities netCap = new NetworkCapabilities();
			netCap.mNetworkCapabilities = in.readLong();
			in.readLong(); // forbidden capabilities
			netCap.mTransportTypes = in.readLong();
			netCap.mLinkUpBandwidthKbps = in.readInt();
			netCap.mLinkDownBandwidthKbps = in.readInt();
			android.os.Parcelable specifier = in.readParcelable(null);
			netCap.mNetworkSpecifier = specifier instanceof NetworkSpecifier ? (NetworkSpecifier)specifier : null;
			in.readParcelable(null); // transport info
			netCap.mSignalStrength = in.readInt();
			final int uidCount = in.readInt(); // uid ranges
			for (int i = 0; i < uidCount; i++) {
				in.readParcelable(null);
			}
			in.createIntArray(); // allowed uids
			in.readString(); // SSID
			in.readBoolean(); // private DNS broken
			in.createIntArray(); // administrator uids
			in.readInt(); // owner uid
			in.readInt(); // requestor uid
			in.readString(); // requestor package name
			in.createIntArray(); // subscription ids
			in.createTypedArrayList(Network.CREATOR); // underlying networks
			in.readInt(); // enterprise id
			in.readInt(); // reservation id
			in.readBoolean(); // match non-thread-local networks
			return netCap;
		}

		@Override
		public NetworkCapabilities[] newArray(int size) {
			return new NetworkCapabilities[size];
		}
	};
}
