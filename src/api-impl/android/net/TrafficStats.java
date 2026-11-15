package android.net;

public class TrafficStats {
	public static void setThreadStatsTag(int dummy) {}

	public static int getThreadStatsTag() {
		return 0;
	}

	public static void clearThreadStatsTag() {}

	public static long getUidRxBytes(int uid) {
		return -1;
	}

	public static long getUidTxBytes(int uid) {
		return -1;
	}

	/* AOSP TrafficStats: no per-uid or interface counters here, UNSUPPORTED (-1) like a device without them */
	public static final int UNSUPPORTED = -1;
	public static final int TAG_SYSTEM_DOWNLOAD = 0xFFFFFF01;
	public static long getUidTxPackets(int uid) { return UNSUPPORTED; }
	public static long getUidRxPackets(int uid) { return UNSUPPORTED; }
	public static long getUidTcpTxBytes(int uid) { return UNSUPPORTED; }
	public static long getUidTcpRxBytes(int uid) { return UNSUPPORTED; }
	public static long getUidUdpTxBytes(int uid) { return UNSUPPORTED; }
	public static long getUidUdpRxBytes(int uid) { return UNSUPPORTED; }
	public static long getTotalTxBytes() { return UNSUPPORTED; }
	public static long getTotalRxBytes() { return UNSUPPORTED; }
	public static long getTotalTxPackets() { return UNSUPPORTED; }
	public static long getTotalRxPackets() { return UNSUPPORTED; }
	public static long getMobileTxBytes() { return UNSUPPORTED; }
	public static long getMobileRxBytes() { return UNSUPPORTED; }
	public static long getMobileTxPackets() { return UNSUPPORTED; }
	public static long getMobileRxPackets() { return UNSUPPORTED; }
	/* AOSP: per-thread uid for accounting, -1 (unset) by default; ATL does not account, the value is only kept */
	private static final ThreadLocal<Integer> sThreadUid = new ThreadLocal<Integer>() {
		@Override
		protected Integer initialValue() {
			return -1;
		}
	};
	public static void setThreadStatsUid(int uid) { sThreadUid.set(uid); }
	public static int getThreadStatsUid() { return sThreadUid.get(); }
	public static void clearThreadStatsUid() { sThreadUid.set(-1); }
	public static void incrementOperationCount(int operationCount) {}
	public static void incrementOperationCount(int tag, int operationCount) {}
	public static void tagSocket(java.net.Socket socket) throws java.net.SocketException {}
	public static void untagSocket(java.net.Socket socket) throws java.net.SocketException {}
	/* SocketTagger.get().tag()/untag(): libcore's default tagger does nothing */
	public static void tagDatagramSocket(java.net.DatagramSocket socket) throws java.net.SocketException {}
	public static void untagDatagramSocket(java.net.DatagramSocket socket) throws java.net.SocketException {}
    /* AOSP 16 API; no backing hardware/service in ATL */
    public static int getAndSetThreadStatsTag(int arg0) { return 0; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public static long getRxBytes(String arg0) { return 0L; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public static long getRxPackets(String arg0) { return 0L; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public static long getTxBytes(String arg0) { return 0L; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public static long getTxPackets(String arg0) { return 0L; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public static long getUidTcpRxSegments(int arg0) { return 0L; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public static long getUidTcpTxSegments(int arg0) { return 0L; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public static long getUidUdpRxPackets(int arg0) { return 0L; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public static long getUidUdpTxPackets(int arg0) { return 0L; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public static void tagFileDescriptor(java.io.FileDescriptor arg0) throws java.io.IOException {  }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public static void untagFileDescriptor(java.io.FileDescriptor arg0) throws java.io.IOException {  }
}
