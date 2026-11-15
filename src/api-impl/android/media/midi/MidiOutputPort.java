package android.media.midi;

import android.util.Log;

import com.android.internal.midi.MidiDispatcher;

import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/* AOSP media/java/android/media/midi/MidiOutputPort.java. ATL has no IMidiDeviceServer: the instance is built over
 * the pipe file descriptor only (AOSP constructor MidiOutputPort(FileDescriptor, int)), without server/token/
 * CloseGuard; no MidiService exists in ATL that would hand out such a descriptor. */
public final class MidiOutputPort extends MidiSender implements Closeable {
	private static final String TAG = "MidiOutputPort";

	private final int mPortNumber;
	private final FileInputStream mInputStream;
	private final MidiDispatcher mDispatcher = new MidiDispatcher();
	private boolean mIsClosed;
	private AtomicInteger mTotalBytes = new AtomicInteger();

	private final Thread mThread = new Thread() {
		@Override
		public void run() {
			byte[] buffer = new byte[MidiPortImpl.MAX_PACKET_SIZE];
			try {
				while (true) {
					int count = mInputStream.read(buffer);
					if (count < 0) {
						break;
					}
					int packetType = MidiPortImpl.getPacketType(buffer, count);
					switch (packetType) {
						case MidiPortImpl.PACKET_TYPE_DATA: {
							int offset = MidiPortImpl.getDataOffset(buffer, count);
							int size = MidiPortImpl.getDataSize(buffer, count);
							long timestamp = MidiPortImpl.getPacketTimestamp(buffer, count);
							mDispatcher.send(buffer, offset, size, timestamp);
							break;
						}
						case MidiPortImpl.PACKET_TYPE_FLUSH:
							mDispatcher.flush();
							break;
						default:
							Log.e(TAG, "Unknown packet type " + packetType);
							break;
					}
					mTotalBytes.addAndGet(count);
				}
			} catch (IOException e) {
				// FIXME report I/O failure?
			} finally {
				try {
					mInputStream.close();
				} catch (IOException e) {
				}
			}
		}
	};

	MidiOutputPort(FileDescriptor fd, int portNumber) {
		mPortNumber = portNumber;
		mInputStream = new FileInputStream(fd);
		mThread.start();
	}

	public final int getPortNumber() {
		return mPortNumber;
	}

	@Override
	public void onConnect(MidiReceiver receiver) {
		mDispatcher.getSender().connect(receiver);
	}

	@Override
	public void onDisconnect(MidiReceiver receiver) {
		mDispatcher.getSender().disconnect(receiver);
	}

	@Override
	public void close() throws IOException {
		synchronized (this) {
			if (mIsClosed) return;
			mInputStream.close();
			mIsClosed = true;
		}
	}

	public int pullTotalBytesCount() {
		return mTotalBytes.getAndSet(0);
	}
}
