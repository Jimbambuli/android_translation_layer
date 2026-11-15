package android.net;

/* AOSP packages/modules/Connectivity/framework/src/android/net/NetworkRequest.java (android14-release): the
 * capabilities and transports a request asks for (WorkManager builds its network constraint from one).
 * Without legacy type, uid ranges and string specifiers. */
public class NetworkRequest {
	public final NetworkCapabilities networkCapabilities;

	private NetworkRequest(NetworkCapabilities nc) {
		networkCapabilities = nc;
	}

	public static class Builder {
		private final NetworkCapabilities mNetworkCapabilities;

		/* AOSP: a new request asks for NOT_RESTRICTED, TRUSTED and NOT_VPN, like new NetworkCapabilities() */
		public Builder() {
			mNetworkCapabilities = new NetworkCapabilities();
		}

		public Builder(NetworkRequest request) {
			mNetworkCapabilities = new NetworkCapabilities(request.networkCapabilities);
		}

		public NetworkRequest build() {
			return new NetworkRequest(new NetworkCapabilities(mNetworkCapabilities));
		}

		public Builder addCapability(int capability) {
			mNetworkCapabilities.addCapability(capability);
			return this;
		}

		public Builder removeCapability(int capability) {
			mNetworkCapabilities.removeCapability(capability);
			return this;
		}

		public Builder clearCapabilities() {
			mNetworkCapabilities.clearAll();
			return this;
		}

		public Builder addTransportType(int transportType) {
			mNetworkCapabilities.addTransportType(transportType);
			return this;
		}

		public Builder removeTransportType(int transportType) {
			mNetworkCapabilities.removeTransportType(transportType);
			return this;
		}

		/* AOSP NetworkRequest.Builder.setNetworkSpecifier(NetworkSpecifier) */
		public Builder setNetworkSpecifier(NetworkSpecifier networkSpecifier) {
			mNetworkCapabilities.setNetworkSpecifier(networkSpecifier);
			return this;
		}

		public Builder setIncludeOtherUidNetworks(boolean include) {
			return this;
		}
	}

	public NetworkSpecifier getNetworkSpecifier() {
		return networkCapabilities.getNetworkSpecifier();
	}

	public boolean hasCapability(int capability) {
		return networkCapabilities.hasCapability(capability);
	}

	public boolean hasTransport(int transportType) {
		return networkCapabilities.hasTransport(transportType);
	}

	public int[] getCapabilities() {
		return networkCapabilities.getCapabilities();
	}

	public int[] getTransportTypes() {
		return networkCapabilities.getTransportTypes();
	}

	public boolean canBeSatisfiedBy(NetworkCapabilities nc) {
		return networkCapabilities.satisfiedByNetworkCapabilities(nc);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof NetworkRequest))
			return false;
		return networkCapabilities.equals(((NetworkRequest)obj).networkCapabilities);
	}

	@Override
	public int hashCode() {
		return networkCapabilities.hashCode();
	}

	@Override
	public String toString() {
		return "NetworkRequest [ " + networkCapabilities + " ]";
	}
}
