package android.hardware;

/* AOSP SensorEventListener2: a SensorEventListener that also hears when SensorManager.flush() is done */
public interface SensorEventListener2 extends SensorEventListener {
	public void onFlushCompleted(Sensor sensor);
}
