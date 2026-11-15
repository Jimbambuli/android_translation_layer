
package android.media.midi;
/* AOSP media/java/android/media/midi/MidiSender.java */
abstract public class MidiSender {
	public void connect(MidiReceiver receiver) {
		if (receiver == null) {
			throw new NullPointerException("receiver null in MidiSender.connect");
		}
		onConnect(receiver);
	}
	public void disconnect(MidiReceiver receiver) {
		if (receiver == null) {
			throw new NullPointerException("receiver null in MidiSender.disconnect");
		}
		onDisconnect(receiver);
	}
	abstract public void onConnect(MidiReceiver receiver);
	abstract public void onDisconnect(MidiReceiver receiver);
}
