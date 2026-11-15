
package android.media.midi;
import java.io.IOException;
/* AOSP media/java/android/media/midi/MidiReceiver.java */
abstract public class MidiReceiver {
	private final int mMaxMessageSize;
	public MidiReceiver() {
		mMaxMessageSize = Integer.MAX_VALUE;
	}
	public MidiReceiver(int maxMessageSize) {
		mMaxMessageSize = maxMessageSize;
	}
	abstract public void onSend(byte[] msg, int offset, int count, long timestamp)
			throws IOException;
	public void flush() throws IOException {
		onFlush();
	}
	public void onFlush() throws IOException {
	}
	public final int getMaxMessageSize() {
		return mMaxMessageSize;
	}
	public void send(byte[] msg, int offset, int count) throws IOException {
		send(msg, offset, count, 0L);
	}
	public void send(byte[] msg, int offset, int count, long timestamp)
			throws IOException {
		int messageSize = getMaxMessageSize();
		while (count > 0) {
			int length = (count > messageSize ? messageSize : count);
			onSend(msg, offset, length, timestamp);
			offset += length;
			count -= length;
		}
	}
}
