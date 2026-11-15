package android.media.midi;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/* AOSP media/java/android/media/midi/MidiUmpDeviceService.java (flag virtual_ump). ATL has no MidiService to ask for
 * the device info, so onCreate() takes AOSP's "MidiDeviceInfo not found" path: no MidiDeviceServer is created,
 * getDeviceInfo() answers null, getOutputPortReceivers() an empty list and onBind() returns null. */
public abstract class MidiUmpDeviceService extends Service {
	private static final String TAG = "MidiUmpDeviceService";

	public static final String SERVICE_INTERFACE = "android.media.midi.MidiUmpDeviceService";

	private MidiDeviceInfo mDeviceInfo;

	@Override
	public void onCreate() {
		Log.e(TAG, "Could not find MidiDeviceInfo for MidiUmpDeviceService " + this);
	}

	public abstract List<MidiReceiver> onGetInputPortReceivers();

	public final List<MidiReceiver> getOutputPortReceivers() {
		return new ArrayList<MidiReceiver>();
	}

	public final MidiDeviceInfo getDeviceInfo() {
		return mDeviceInfo;
	}

	public void onDeviceStatusChanged(MidiDeviceStatus status) {
	}

	public void onClose() {
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}
