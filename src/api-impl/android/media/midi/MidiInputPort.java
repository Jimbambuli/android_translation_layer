package android.media.midi;

import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/* AOSP media/java/android/media/midi/MidiInputPort.java. ATL has no IMidiDeviceServer: the instance is built over the
 * pipe file descriptor only (AOSP constructor MidiInputPort(FileDescriptor, int)), without server/token/CloseGuard;
 * no MidiService exists in ATL that would hand out such a descriptor. */
public final class MidiInputPort extends MidiReceiver implements Closeable {
	private final int mPortNumber;
	private FileDescriptor mFileDescriptor;
	private FileOutputStream mOutputStream;
	private boolean mIsClosed;
	private AtomicInteger mTotalBytes = new AtomicInteger();
	private final byte[] mBuffer = new byte[MidiPortImpl.MAX_PACKET_SIZE];

	MidiInputPort(FileDescriptor fd, int portNumber) {
		super(MidiPortImpl.MAX_PACKET_DATA_SIZE);
		mFileDescriptor = fd;
		mPortNumber = portNumber;
		mOutputStream = new FileOutputStream(fd);
	}

	public final int getPortNumber() {
		return mPortNumber;
	}

	@Override
	public void onSend(byte[] msg, int offset, int count, long timestamp) throws IOException {
		if (offset < 0 || count < 0 || offset + count > msg.length) {
			throw new IllegalArgumentException("offset or count out of range");
		}
		if (count > MidiPortImpl.MAX_PACKET_DATA_SIZE) {
			throw new IllegalArgumentException("count exceeds max message size");
		}
		synchronized (mBuffer) {
			if (mOutputStream == null) {
				throw new IOException("MidiInputPort is closed");
			}
			int length = MidiPortImpl.packData(msg, offset, count, timestamp, mBuffer);
			mOutputStream.write(mBuffer, 0, length);
			mTotalBytes.addAndGet(length);
		}
	}

	@Override
	public void onFlush() throws IOException {
		synchronized (mBuffer) {
			if (mOutputStream == null) {
				throw new IOException("MidiInputPort is closed");
			}
			int length = MidiPortImpl.packFlush(mBuffer);
			mOutputStream.write(mBuffer, 0, length);
		}
	}

	FileDescriptor claimFileDescriptor() {
		synchronized (this) {
			FileDescriptor fd;
			synchronized (mBuffer) {
				fd = mFileDescriptor;
				if (fd == null) return null;
				try {
					mOutputStream.close();
				} catch (IOException e) {
				}
				mFileDescriptor = null;
				mOutputStream = null;
			}
			mIsClosed = true;
			return fd;
		}
	}

	@Override
	public void close() throws IOException {
		synchronized (this) {
			if (mIsClosed) return;
			synchronized (mBuffer) {
				mFileDescriptor = null;
				if (mOutputStream != null) {
					mOutputStream.close();
					mOutputStream = null;
				}
			}
			mIsClosed = true;
		}
	}

	public int pullTotalBytesCount() {
		return mTotalBytes.getAndSet(0);
	}
}
