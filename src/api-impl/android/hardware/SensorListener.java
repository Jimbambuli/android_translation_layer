package android.hardware;

/* AOSP core/java/android/hardware/SensorListener.java (deprecated) */
@Deprecated
public interface SensorListener {
	public void onSensorChanged(int sensor, float[] values);

	public void onAccuracyChanged(int sensor, int accuracy);
}
