package android.media.midi;

import android.util.Log;

import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.IOException;

/* AOSP media/java/android/media/midi/MidiDevice.java. ATL has no MidiService, so MidiManager never opens a device and
 * no instance is handed out; the members follow AOSP with an IMidiDeviceServer that has no such port: the open*Port()
 * calls return null (AOSP: the server returns no file descriptor), connectPorts() claims nothing further. */
public final class MidiDevice implements Closeable {
	private static final String TAG = "MidiDevice";

	private final MidiDeviceInfo mDeviceInfo;
	private boolean mIsDeviceClosed;

	public class MidiConnection implements Closeable {
		private boolean mIsClosed;

		MidiConnection() {
		}

		@Override
		public void close() throws IOException {
			synchronized (this) {
				if (mIsClosed) return;
				mIsClosed = true;
			}
		}
	}

	MidiDevice(MidiDeviceInfo deviceInfo) {
		mDeviceInfo = deviceInfo;
	}

	public MidiDeviceInfo getInfo() {
		return mDeviceInfo;
	}

	public MidiInputPort openInputPort(int portNumber) {
		if (mIsDeviceClosed) {
			return null;
		}
		return null;
	}

	public MidiOutputPort openOutputPort(int portNumber) {
		if (mIsDeviceClosed) {
			return null;
		}
		return null;
	}

	public MidiConnection connectPorts(MidiInputPort inputPort, int outputPortNumber) {
		if (outputPortNumber < 0 || outputPortNumber >= mDeviceInfo.getOutputPortCount()) {
			throw new IllegalArgumentException("outputPortNumber out of range");
		}
		if (mIsDeviceClosed) {
			return null;
		}
		FileDescriptor fd = inputPort.claimFileDescriptor();
		if (fd == null) {
			return null;
		}
		Log.e(TAG, "connectPorts: no device server");
		return null;
	}

	@Override
	public void close() throws IOException {
		synchronized (this) {
			mIsDeviceClosed = true;
		}
	}

	@Override
	public String toString() {
		return ("MidiDevice: " + mDeviceInfo.toString());
	}
}
