package android.media.midi;

import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.util.ArraySet;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

/* AOSP media/java/android/media/midi/MidiManager.java. ATL has no MidiService (IMidiManager), so no MIDI device
 * ever exists: registered callbacks are kept (AOSP keeps them in mDeviceListeners) but never fire, the device
 * lists are empty (AOSP: the service reports no devices), and a device cannot be opened (AOSP: the open callback
 * gets a null server and the listener receives a null MidiDevice). */
public final class MidiManager {
	public static final int TRANSPORT_MIDI_BYTE_STREAM = 1;
	public static final int TRANSPORT_UNIVERSAL_MIDI_PACKETS = 2;

	public static final String BLUETOOTH_MIDI_SERVICE_INTENT = "android.media.midi.BluetoothMidiService";
	public static final String BLUETOOTH_MIDI_SERVICE_PACKAGE = "com.android.bluetoothmidiservice";
	public static final String BLUETOOTH_MIDI_SERVICE_CLASS = "com.android.bluetoothmidiservice.BluetoothMidiService";

	private ConcurrentHashMap<DeviceCallback, Object> mDeviceListeners = new ConcurrentHashMap<DeviceCallback, Object>();

	public static class DeviceCallback {
		public void onDeviceAdded(MidiDeviceInfo device) {
		}

		public void onDeviceRemoved(MidiDeviceInfo device) {
		}

		public void onDeviceStatusChanged(MidiDeviceStatus status) {
		}
	}

	public interface OnDeviceOpenedListener {
		abstract public void onDeviceOpened(MidiDevice device);
	}

	MidiManager() {
	}

	@Deprecated
	public void registerDeviceCallback(DeviceCallback callback, Handler handler) {
		mDeviceListeners.put(callback, callback);
	}

	public void registerDeviceCallback(int transport, Executor executor, DeviceCallback callback) {
		Objects.requireNonNull(executor);
		mDeviceListeners.put(callback, callback);
	}

	public void unregisterDeviceCallback(DeviceCallback callback) {
		mDeviceListeners.remove(callback);
	}

	@Deprecated
	public MidiDeviceInfo[] getDevices() {
		return new MidiDeviceInfo[0];
	}

	public Set<MidiDeviceInfo> getDevicesForTransport(int transport) {
		return Collections.emptySet();
	}

	private void sendOpenDeviceResponse(final MidiDevice device, final OnDeviceOpenedListener listener, Handler handler) {
		if (handler != null) {
			handler.post(new Runnable() {
				@Override
				public void run() {
					listener.onDeviceOpened(device);
				}
			});
		} else {
			listener.onDeviceOpened(device);
		}
	}

	public void openDevice(MidiDeviceInfo deviceInfo, OnDeviceOpenedListener listener, Handler handler) {
		sendOpenDeviceResponse(null, listener, handler);
	}

	public void openBluetoothDevice(BluetoothDevice bluetoothDevice, OnDeviceOpenedListener listener, Handler handler) {
		sendOpenDeviceResponse(null, listener, handler);
	}

	/* AOSP closeBluetoothDevice(): midiDevice.close(), IOException is logged */
	public void closeBluetoothDevice(MidiDevice midiDevice) {
		try {
			midiDevice.close();
		} catch (java.io.IOException ex) {
			android.util.Log.e("MidiManager", "Exception closing BLE-MIDI device" + ex);
		}
	}
}
