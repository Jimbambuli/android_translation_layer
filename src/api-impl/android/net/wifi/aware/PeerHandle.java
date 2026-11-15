package android.net.wifi.aware;

/* AOSP packages/modules/Wifi/framework/java/android/net/wifi/aware/PeerHandle.java */
public class PeerHandle {
	public PeerHandle(int peerId) {
		this.peerId = peerId;
	}

	public int peerId;

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof PeerHandle)) {
			return false;
		}
		return peerId == ((PeerHandle) o).peerId;
	}

	@Override
	public int hashCode() {
		return peerId;
	}
}
