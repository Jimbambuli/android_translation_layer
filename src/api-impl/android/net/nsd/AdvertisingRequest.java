package android.net.nsd;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.Objects;

/* AOSP packages/modules/Connectivity/framework-t/src/android/net/nsd/AdvertisingRequest.java, public API only */
public final class AdvertisingRequest implements Parcelable {
	/* AOSP: NSD_ADVERTISING_UPDATE_ONLY (hidden) marks an update of an already registered service */
	static final long NSD_ADVERTISING_UPDATE_ONLY = 1;

	public static final long FLAG_SKIP_PROBING = 1 << 1;

	public static final Creator<AdvertisingRequest> CREATOR = new Creator<AdvertisingRequest>() {
		@Override
		public AdvertisingRequest createFromParcel(Parcel in) {
			NsdServiceInfo serviceInfo = (NsdServiceInfo)in.readParcelable(NsdServiceInfo.class.getClassLoader());
			int protocolType = in.readInt();
			long advertiseConfig = in.readLong();
			return new AdvertisingRequest(serviceInfo, protocolType, advertiseConfig);
		}

		@Override
		public AdvertisingRequest[] newArray(int size) {
			return new AdvertisingRequest[size];
		}
	};

	private final NsdServiceInfo mServiceInfo;
	private final int mProtocolType;
	private final long mAdvertisingConfig;

	private AdvertisingRequest(NsdServiceInfo serviceInfo, int protocolType, long advertisingConfig) {
		mServiceInfo = serviceInfo;
		mProtocolType = protocolType;
		mAdvertisingConfig = advertisingConfig;
	}

	public NsdServiceInfo getServiceInfo() {
		return mServiceInfo;
	}

	public int getProtocolType() {
		return mProtocolType;
	}

	public long getFlags() {
		return mAdvertisingConfig;
	}

	@Override
	public String toString() {
		return "serviceInfo: " + mServiceInfo + ", protocolType: " + mProtocolType + ", advertisingConfig: " + mAdvertisingConfig;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		} else if (!(other instanceof AdvertisingRequest)) {
			return false;
		} else {
			AdvertisingRequest otherRequest = (AdvertisingRequest)other;
			return mServiceInfo.equals(otherRequest.mServiceInfo)
				&& mProtocolType == otherRequest.mProtocolType
				&& mAdvertisingConfig == otherRequest.mAdvertisingConfig;
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(mServiceInfo, mProtocolType, mAdvertisingConfig);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeParcelable(mServiceInfo, flags);
		dest.writeInt(mProtocolType);
		dest.writeLong(mAdvertisingConfig);
	}

	public static final class Builder {
		private final NsdServiceInfo mServiceInfo;
		private int mProtocolType;
		private long mAdvertisingConfig;

		public Builder(NsdServiceInfo serviceInfo) {
			mServiceInfo = serviceInfo;
			mProtocolType = NsdManager.PROTOCOL_DNS_SD;
		}

		public Builder setFlags(long flags) {
			mAdvertisingConfig = flags;
			return this;
		}

		public Builder setProtocolType(int protocolType) {
			mProtocolType = protocolType;
			return this;
		}

		public AdvertisingRequest build() {
			return new AdvertisingRequest(mServiceInfo, mProtocolType, mAdvertisingConfig);
		}
	}
}
