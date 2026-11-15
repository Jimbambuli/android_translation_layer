package android.media.midi;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

/* AOSP media/java/android/media/midi/MidiDeviceService.java. ATL has no MidiService to ask for the device info, so
 * onCreate() takes AOSP's "MidiDeviceInfo not found" path: no MidiDeviceServer is created, getDeviceInfo() and
 * getOutputPortReceivers() answer null and onBind() returns null. */
abstract public class MidiDeviceService extends Service {
	private static final String TAG = "MidiDeviceService";

	public static final String SERVICE_INTERFACE = "android.media.midi.MidiDeviceService";

	private MidiDeviceInfo mDeviceInfo;

	@Override
	public void onCreate() {
		Log.e(TAG, "Could not find MidiDeviceInfo for MidiDeviceService " + this);
	}

	abstract public MidiReceiver[] onGetInputPortReceivers();

	public final MidiReceiver[] getOutputPortReceivers() {
		return null;
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
