package android.net;

public class NetworkInfo implements android.os.Parcelable {
	public enum State {
		CONNECTED,
		CONNECTING,
		DISCONNECTED,
		DISCONNECTING,
		SUSPENDED,
		UNKNOWN
	}

	/* AOSP NetworkInfo.DetailedState (packages/modules/Connectivity, android16-release) */
	public enum DetailedState {
		IDLE,
		SCANNING,
		CONNECTING,
		AUTHENTICATING,
		OBTAINING_IPADDR,
		CONNECTED,
		SUSPENDED,
		DISCONNECTING,
		DISCONNECTED,
		FAILED,
		BLOCKED,
		VERIFYING_POOR_LINK,
		CAPTIVE_PORTAL_CHECK
	}

	private State state = State.DISCONNECTED;

	public NetworkInfo(boolean available) {
		state = available ? State.CONNECTED : State.DISCONNECTED;
	}

	public NetworkInfo.State getState() {
		return state;
	}

	public int getType() {
		return state == State.CONNECTED ? /*TYPE_WIFI*/ 0x1 : 0x0;
	}

	public boolean isConnected() {
		return state == State.CONNECTED;
	}

	public boolean isConnectedOrConnecting() {
		return state == State.CONNECTED;
	}

	public boolean isFailover() {
		return false;
	}

	public int getSubtype() {
		return 0; // NETWORK_TYPE_UNKNOWN
	}

	public boolean isRoaming() {
		return false;
	}

	public String getTypeName() {
		return state == State.CONNECTED ? "WIFI" : "UNKNOWN";
	}

	public String getSubtypeName() {
		return "UNKNOWN";
	}

	public boolean isAvailable() {
		return state == State.CONNECTED;
	}

	/* AOSP keeps the detailed state and derives getState() from it (stateMap); ATL's NetworkInfo is either
	 * connected or disconnected, whose detailed states are CONNECTED and DISCONNECTED */
	public DetailedState getDetailedState() {
		return state == State.CONNECTED ? DetailedState.CONNECTED : DetailedState.DISCONNECTED;
	}

	public String getExtraInfo() {
		return null;
	}

	public String getReason() {
		return null;
	}

	public int describeContents() {
		return 0;
	}

	public void writeToParcel(android.os.Parcel dest, int flags) {
		dest.writeInt(state == State.CONNECTED ? 1 : 0);
	}

	public static final Creator<NetworkInfo> CREATOR = new Creator<NetworkInfo>() {
		public NetworkInfo createFromParcel(android.os.Parcel in) {
			return new NetworkInfo(in.readInt() != 0);
		}

		public NetworkInfo[] newArray(int size) {
			return new NetworkInfo[size];
		}
	};
}
