package android.net.nsd;

import android.net.Network;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import java.util.Objects;

/* AOSP packages/modules/Connectivity/framework-t/src/android/net/nsd/DiscoveryRequest.java, public API only.
 * ATL's Network is not Parcelable, so a Network travels through a Parcel as its network handle. */
public final class DiscoveryRequest implements Parcelable {
	private final int mProtocolType;
	private final String mServiceType;
	private final String mSubtype;
	private final Network mNetwork;

	public static final Creator<DiscoveryRequest> CREATOR = new Creator<DiscoveryRequest>() {
		@Override
		public DiscoveryRequest createFromParcel(Parcel in) {
			int protocolType = in.readInt();
			String serviceType = in.readString();
			String subtype = in.readString();
			Network network = in.readBoolean() ? Network.fromNetworkHandle(in.readLong()) : null;
			return new DiscoveryRequest(protocolType, serviceType, subtype, network);
		}

		@Override
		public DiscoveryRequest[] newArray(int size) {
			return new DiscoveryRequest[size];
		}
	};

	private DiscoveryRequest(int protocolType, String serviceType, String subtype, Network network) {
		mProtocolType = protocolType;
		mServiceType = serviceType;
		mSubtype = subtype;
		mNetwork = network;
	}

	public String getServiceType() {
		return mServiceType;
	}

	public String getSubtype() {
		return mSubtype;
	}

	/* AOSP: hidden getProtocolType(), used by NsdManager */
	int getProtocolType() {
		return mProtocolType;
	}

	public Network getNetwork() {
		return mNetwork;
	}

	@Override
	public String toString() {
		return ", protocolType: " + mProtocolType + ", serviceType: " + mServiceType + ", subtype: " + mSubtype + ", network: " + mNetwork;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		} else if (!(other instanceof DiscoveryRequest)) {
			return false;
		} else {
			DiscoveryRequest otherRequest = (DiscoveryRequest)other;
			return mProtocolType == otherRequest.mProtocolType
				&& Objects.equals(mServiceType, otherRequest.mServiceType)
				&& Objects.equals(mSubtype, otherRequest.mSubtype)
				&& Objects.equals(mNetwork, otherRequest.mNetwork);
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(mProtocolType, mServiceType, mSubtype, mNetwork);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(mProtocolType);
		dest.writeString(mServiceType);
		dest.writeString(mSubtype);
		dest.writeBoolean(mNetwork != null);
		if (mNetwork != null)
			dest.writeLong(mNetwork.getNetworkHandle());
	}

	public static final class Builder {
		private final int mProtocolType;
		private String mServiceType;
		private String mSubtype;
		private Network mNetwork;

		public Builder(String serviceType) {
			mProtocolType = NsdManager.PROTOCOL_DNS_SD;
			setServiceType(serviceType);
		}

		/* AOSP: hidden Builder(int, String), used by NsdManager */
		Builder(int protocolType, String serviceType) {
			mProtocolType = protocolType;
			setServiceType(serviceType);
		}

		/* AOSP: hidden setServiceType() */
		Builder setServiceType(String serviceType) {
			if (TextUtils.isEmpty(serviceType)) {
				throw new IllegalArgumentException("Service type cannot be empty");
			}
			mServiceType = serviceType;
			return this;
		}

		public Builder setSubtype(String subtype) {
			mSubtype = subtype;
			return this;
		}

		public Builder setNetwork(Network network) {
			mNetwork = network;
			return this;
		}

		public DiscoveryRequest build() {
			return new DiscoveryRequest(mProtocolType, mServiceType, mSubtype, mNetwork);
		}
	}
}
