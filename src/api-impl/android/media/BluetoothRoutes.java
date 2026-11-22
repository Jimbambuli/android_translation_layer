package android.media;

import android.content.Intent;
import android.os.Handler;
import com.android.bluetooth.btservice.AdapterService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/* ATL: the Bluetooth part of AOSP's AudioDeviceInventory/AudioDeviceBroker for the app's AudioManager. The
 * connected Bluetooth audio devices come from ATL's AdapterService (BlueZ): A2DP sinks (MediaTransport1) and
 * headsets. When an A2DP sink goes away, ACTION_AUDIO_BECOMING_NOISY is sent as
 * AudioDeviceInventory.checkSendBecomingNoisyIntentInt() does: only if a media player of the app holds audio focus
 * (no playback otherwise: "not a becoming noisy situation"). The device is taken as the music device, as AOSP's
 * audio policy routes media to a connected A2DP sink; which output PipeWire/PulseAudio really uses is not looked at.
 * @hide */
public final class BluetoothRoutes implements AdapterService.AudioDeviceListener {
	private static BluetoothRoutes instance;
	private static final HashMap<AudioDeviceCallback, Handler> callbacks = new HashMap<>();

	private BluetoothRoutes() {}

	/* from AudioManager's constructor: AOSP's AudioService always watches the devices */
	static synchronized void start() {
		if (instance != null || !AdapterService.isAvailable())
			return;
		instance = new BluetoothRoutes();
		AdapterService.get().addAudioDeviceListener(instance);
	}

	private static List<String[]> devices(List<String> addresses) {
		ArrayList<String[]> list = new ArrayList<>();
		for (String address : addresses) {
			Map<String, Object> p = AdapterService.get().deviceProperties(address);
			Object alias = p == null ? null : p.get("Alias");
			list.add(new String[] {address, alias instanceof String ? (String)alias : address});
		}
		return list;
	}

	/* {address, name} */
	static List<String[]> a2dpSinks() {
		if (!AdapterService.isAvailable())
			return new ArrayList<>();
		return devices(AdapterService.get().a2dpSinks());
	}

	static List<String[]> headsets() {
		if (!AdapterService.isAvailable())
			return new ArrayList<>();
		return devices(AdapterService.get().headsets());
	}

	/* a stable id per device and type (AOSP: the audio port handle) */
	static int id(int type, String address) {
		return 100 + ((type * 31 + address.hashCode()) & 0x3fffffff) * 2;
	}

	static synchronized void addCallback(AudioDeviceCallback callback, Handler handler) {
		start();
		callbacks.put(callback, handler);
	}

	static synchronized void removeCallback(AudioDeviceCallback callback) {
		callbacks.remove(callback);
	}

	@Override
	public void onBluetoothAudioChanged(int profile, String address, boolean connected) {
		String name = address;
		Map<String, Object> p = AdapterService.get().deviceProperties(address);
		if (p != null && p.get("Alias") instanceof String)
			name = (String)p.get("Alias");
		final ArrayList<AudioDeviceInfo> changed = new ArrayList<>();
		if (profile == android.bluetooth.BluetoothProfile.A2DP) {
			changed.add(new AudioDeviceInfo(id(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, address), AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, name, true, address));
			if (!connected && !AudioManager.sFocusHolders.isEmpty()) {
				Intent intent = new Intent(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
				AdapterService.context().sendBroadcast(intent);
			}
		} else {
			int sco = id(AudioDeviceInfo.TYPE_BLUETOOTH_SCO, address);
			changed.add(new AudioDeviceInfo(sco, AudioDeviceInfo.TYPE_BLUETOOTH_SCO, name, true, address));
			changed.add(new AudioDeviceInfo(sco + 1, AudioDeviceInfo.TYPE_BLUETOOTH_SCO, name, false, address));
		}
		final AudioDeviceInfo[] array = changed.toArray(new AudioDeviceInfo[0]);
		final boolean added = connected;
		HashMap<AudioDeviceCallback, Handler> copy;
		synchronized (BluetoothRoutes.class) {
			copy = new HashMap<>(callbacks);
		}
		for (final Map.Entry<AudioDeviceCallback, Handler> e : copy.entrySet()) {
			e.getValue().post(new Runnable() {
				public void run() {
					if (added)
						e.getKey().onAudioDevicesAdded(array);
					else
						e.getKey().onAudioDevicesRemoved(array);
				}
			});
		}
	}
}
