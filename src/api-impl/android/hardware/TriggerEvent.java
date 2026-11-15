package android.hardware;

/* AOSP core/java/android/hardware/TriggerEvent.java */
public final class TriggerEvent {
	
	public final float[] values;

	public Sensor sensor;

	public long timestamp;

	TriggerEvent(int size) {
		values = new float[size];
	}
}
