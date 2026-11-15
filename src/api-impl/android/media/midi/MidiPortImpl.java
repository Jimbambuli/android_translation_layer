package android.media.midi;

/* AOSP media/java/android/media/midi/MidiPortImpl.java: packet format of the MIDI port pipes */
final class MidiPortImpl {
	public static final int PACKET_TYPE_DATA = 1;
	public static final int PACKET_TYPE_FLUSH = 2;
	public static final int MAX_PACKET_SIZE = 1024;
	private static final int TIMESTAMP_SIZE = 8;
	private static final int DATA_PACKET_OVERHEAD = TIMESTAMP_SIZE + 1;
	public static final int MAX_PACKET_DATA_SIZE = MAX_PACKET_SIZE - DATA_PACKET_OVERHEAD;

	public static int packData(byte[] message, int offset, int size, long timestamp, byte[] dest) {
		if (size > MAX_PACKET_DATA_SIZE) {
			size = MAX_PACKET_DATA_SIZE;
		}
		int length = 0;
		dest[length++] = PACKET_TYPE_DATA;
		System.arraycopy(message, offset, dest, length, size);
		length += size;
		for (int i = 0; i < TIMESTAMP_SIZE; i++) {
			dest[length++] = (byte)timestamp;
			timestamp >>= 8;
		}
		return length;
	}

	public static int packFlush(byte[] dest) {
		dest[0] = PACKET_TYPE_FLUSH;
		return 1;
	}

	public static int getPacketType(byte[] buffer, int bufferLength) {
		return buffer[0];
	}

	public static int getDataOffset(byte[] buffer, int bufferLength) {
		return 1;
	}

	public static int getDataSize(byte[] buffer, int bufferLength) {
		return bufferLength - DATA_PACKET_OVERHEAD;
	}

	public static long getPacketTimestamp(byte[] buffer, int bufferLength) {
		int offset = bufferLength;
		long timestamp = 0;
		for (int i = 0; i < TIMESTAMP_SIZE; i++) {
			int b = (int)buffer[--offset] & 0xFF;
			timestamp = (timestamp << 8) | b;
		}
		return timestamp;
	}
}
