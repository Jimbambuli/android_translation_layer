package android.hardware;

import android.atl.annotation.Export;

public class SensorEvent {

	public final float[] values;

	public Sensor sensor;

	/* AOSP SensorEvent.accuracy: ATL's sensor sources give no status; the events are reported as
	 * SENSOR_STATUS_ACCURACY_HIGH */
	public int accuracy = /*SensorManager.SENSOR_STATUS_ACCURACY_HIGH*/ 3;

	/* AOSP SensorEvent.timestamp: nanoseconds in the SystemClock.elapsedRealtimeNanos() base */
	public long timestamp;

	/* AOSP SensorEvent.firstEventAfterDiscontinuity: set only for head tracker sensors, which ATL does not have */
	public boolean firstEventAfterDiscontinuity;

	@Export
	public SensorEvent(float[] values, Sensor sensor) {
		this.values = values;
		this.sensor = sensor;
		this.timestamp = android.os.SystemClock.elapsedRealtimeNanos();
	}
}
