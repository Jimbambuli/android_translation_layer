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
	public static void setThreadStatsUid(int uid) {}
	public static void clearThreadStatsUid() {}
	public static void incrementOperationCount(int operationCount) {}
	public static void incrementOperationCount(int tag, int operationCount) {}
	public static void tagSocket(java.net.Socket socket) throws java.net.SocketException {}
	public static void untagSocket(java.net.Socket socket) throws java.net.SocketException {}
}
