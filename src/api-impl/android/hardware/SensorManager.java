package android.hardware;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class SensorManager {

	public static float GRAVITY_EARTH = 9.81f;

	public Sensor getDefaultSensor(int type) {
		return new Sensor(type);
	}

	/* AOSP getDefaultSensor(int, boolean): ATL's sensors are not wake-up sensors, so only wakeUp == false matches */
	public Sensor getDefaultSensor(int type, boolean wakeUp) {
		return wakeUp ? null : getDefaultSensor(type);
	}

	/* AOSP legacy registerListener(SensorListener, int[, int]) (LegacySensorManager): the legacy
	 * sensors are not delivered, so there is nothing to register and the result is false (as for a
	 * device without the requested sensors) */
	@Deprecated
	public boolean registerListener(SensorListener listener, int sensors) {
		return false;
	}

	@Deprecated
	public boolean registerListener(SensorListener listener, int sensors, int rate) {
		return false;
	}

	@Deprecated
	public void unregisterListener(SensorListener listener) {
	}

	@Deprecated
	public void unregisterListener(SensorListener listener, int sensors) {
	}

	public boolean registerListener(SensorEventListener listener, Sensor sensor, int samplingPeriodUs, Handler handler) {
		return true; // we could try saying that the sensor doesn't exist and hope the app just doesn't use it then, but as long as we never call the handler the app should leave this alone
	}

	/* the sensor events are not batched, maxReportLatencyUs is only a hint (as in AOSP for a
	 * sensor without a FIFO) */
	public boolean registerListener(SensorEventListener listener, Sensor sensor, int samplingPeriodUs, int maxReportLatencyUs, Handler handler) {
		return registerListener(listener, sensor, samplingPeriodUs, handler);
	}

	public boolean registerListener(SensorEventListener listener, Sensor sensor, int samplingPeriodUs, int maxReportLatencyUs) {
		return registerListener(listener, sensor, samplingPeriodUs);
	}

	/* AOSP SystemSensorManager.mSensorListeners: the sensors each listener is registered for (its
	 * SensorEventQueue). ATL has one sensor per type; per type the source of the events: the
	 * LocationListener (TYPE_ORIENTATION) or the handle of the native accelerometer timer (0: none found) */
	private final HashMap<SensorEventListener, HashMap<Integer, Object>> mSensorListeners = new HashMap<>();

	public boolean registerListener(final SensorEventListener listener, final Sensor sensor, int samplingPeriodUs) {
		/* AOSP SystemSensorManager.registerListenerImpl(): null arguments and trigger sensors fail */
		if (listener == null || sensor == null || sensor.getReportingMode() == Sensor.REPORTING_MODE_ONE_SHOT)
			return false;
		synchronized (mSensorListeners) {
			HashMap<Integer, Object> sensors = mSensorListeners.get(listener);
			if (sensors != null && sensors.containsKey(sensor.getType()))
				return false; // AOSP SensorEventQueue.addSensor(): already registered
			Object source;
			switch (sensor.getType()) {
				case Sensor.TYPE_ORIENTATION:
					LocationListener locationListener = new LocationListener() {
						@Override
						public void onLocationChanged(Location location) {
							listener.onSensorChanged(new SensorEvent(new float[] {location.getBearing()}, sensor));
						}
					};
					new LocationManager().requestLocationUpdates(null, 0, 0, locationListener);
					source = locationListener;
					break;
				case Sensor.TYPE_ACCELEROMETER:
					source = register_accelerometer_listener_native(listener, sensor, samplingPeriodUs);
					break;
				default:
					return false;
			}
			if (sensors == null) {
				sensors = new HashMap<>();
				mSensorListeners.put(listener, sensors);
			}
			sensors.put(sensor.getType(), source);
			return true;
		}
	}

	/* AOSP SensorManager.unregisterListener(SensorEventListener): from all sensors */
	public void unregisterListener(final SensorEventListener listener) {
		if (listener == null)
			return;
		unregisterListenerImpl(listener, null);
	}

	/* AOSP SensorManager.unregisterListener(SensorEventListener, Sensor) */
	public void unregisterListener(final SensorEventListener listener, Sensor sensor) {
		if (listener == null || sensor == null)
			return;
		unregisterListenerImpl(listener, sensor);
	}

	/* AOSP SystemSensorManager.unregisterListenerImpl(): trigger sensors are cancelled with
	 * cancelTriggerSensor(); else the listener's registration for the sensor (null: all) ends */
	private void unregisterListenerImpl(SensorEventListener listener, Sensor sensor) {
		if (sensor != null && sensor.getReportingMode() == Sensor.REPORTING_MODE_ONE_SHOT)
			return;
		synchronized (mSensorListeners) {
			HashMap<Integer, Object> sensors = mSensorListeners.get(listener);
			if (sensors == null)
				return;
			for (Object source : sensor == null ? sensors.values() : Arrays.asList(sensors.get(sensor.getType()))) {
				if (source instanceof LocationListener)
					new LocationManager().removeUpdates((LocationListener)source);
				else if (source instanceof Long)
					unregister_accelerometer_listener_native((Long)source);
			}
			if (sensor == null)
				sensors.clear();
			else
				sensors.remove(sensor.getType());
			if (sensors.isEmpty())
				mSensorListeners.remove(listener);
		}
	}

	native long register_accelerometer_listener_native(SensorEventListener listener, Sensor sensor, int sampling_period);
	native void unregister_accelerometer_listener_native(long handle);

	public List<Sensor> getSensorList(int type) {
		return Arrays.asList(getDefaultSensor(type));
	}

	/* AOSP SensorManager.AXIS_* and remapCoordinateSystem() */
	public static final int AXIS_X = 1;
	public static final int AXIS_Y = 2;
	public static final int AXIS_Z = 3;
	public static final int AXIS_MINUS_X = AXIS_X | 0x80;
	public static final int AXIS_MINUS_Y = AXIS_Y | 0x80;
	public static final int AXIS_MINUS_Z = AXIS_Z | 0x80;

	private static final float[] sTempMatrix = new float[16];

	public static boolean remapCoordinateSystem(float[] inR, int X, int Y, float[] outR) {
		if (inR == outR) {
			final float[] temp = sTempMatrix;
			synchronized (temp) {
				// we don't expect to have a lot of contention
				if (remapCoordinateSystemImpl(inR, X, Y, temp)) {
					final int size = outR.length;
					for (int i = 0; i < size; i++) {
						outR[i] = temp[i];
					}
					return true;
				}
			}
		}
		return remapCoordinateSystemImpl(inR, X, Y, outR);
	}

	private static boolean remapCoordinateSystemImpl(float[] inR, int X, int Y, float[] outR) {
		/*
		 * X and Y define a rotation matrix 'r':
		 *
		 *  (X==1)?((X&0x80)?-1:1):0    (X==2)?((X&0x80)?-1:1):0    (X==3)?((X&0x80)?-1:1):0
		 *  (Y==1)?((Y&0x80)?-1:1):0    (Y==2)?((Y&0x80)?-1:1):0    (Y==3)?((X&0x80)?-1:1):0
		 *                              r[0] ^ r[1]
		 *
		 * where the 3rd line is the vector product of the first 2 lines
		 *
		 */

		final int length = outR.length;
		if (inR.length != length) {
			return false; // invalid parameter
		}
		if ((X & 0x7C) != 0 || (Y & 0x7C) != 0) {
			return false; // invalid parameter
		}
		if (((X & 0x3) == 0) || ((Y & 0x3) == 0)) {
			return false; // no axis specified
		}
		if ((X & 0x3) == (Y & 0x3)) {
			return false; // same axis specified
		}

		// Z is "the other" axis, its sign is either +/- sign(X)*sign(Y)
		// this can be calculated by exclusive-or'ing X and Y; except for
		// the sign inversion (+/-) which is calculated below.
		int Z = X ^ Y;

		// extract the axis (remove the sign), offset in the range 0 to 2.
		final int x = (X & 0x3) - 1;
		final int y = (Y & 0x3) - 1;
		final int z = (Z & 0x3) - 1;

		// compute the sign of Z (whether it needs to be inverted)
		final int axis_y = (z + 1) % 3;
		final int axis_z = (z + 2) % 3;
		if (((x ^ axis_y) | (y ^ axis_z)) != 0) {
			Z ^= 0x80;
		}

		final boolean sx = (X >= 0x80);
		final boolean sy = (Y >= 0x80);
		final boolean sz = (Z >= 0x80);

		// Perform R * r, in avoiding actual muls and adds.
		final int rowLength = ((length == 16) ? 4 : 3);
		for (int j = 0; j < 3; j++) {
			final int offset = j * rowLength;
			for (int i = 0; i < 3; i++) {
				if (x == i)
					outR[offset + i] = sx ? -inR[offset + 0] : inR[offset + 0];
				if (y == i)
					outR[offset + i] = sy ? -inR[offset + 1] : inR[offset + 1];
				if (z == i)
					outR[offset + i] = sz ? -inR[offset + 2] : inR[offset + 2];
			}
		}
		if (length == 16) {
			outR[3] = outR[7] = outR[11] = outR[12] = outR[13] = outR[14] = 0;
			outR[15] = 1;
		}
		return true;
	}

	/* AOSP SensorManager.PRESSURE_STANDARD_ATMOSPHERE and getAltitude() */
	public static final float PRESSURE_STANDARD_ATMOSPHERE = 1013.25f;

	public static float getAltitude(float p0, float p) {
		final float coef = 1.0f / 5.255f;
		return 44330.0f * (1.0f - (float) Math.pow(p / p0, coef));
	}

	/* AOSP SensorManager.getQuaternionFromVector() */
	public static void getQuaternionFromVector(float[] Q, float[] rv) {
		if (rv.length >= 4) {
			Q[0] = rv[3];
		} else {
			Q[0] = 1 - rv[0] * rv[0] - rv[1] * rv[1] - rv[2] * rv[2];
			Q[0] = (Q[0] > 0) ? (float) Math.sqrt(Q[0]) : 0;
		}
		Q[1] = rv[0];
		Q[2] = rv[1];
		Q[3] = rv[2];
	}

	/* AOSP SensorManager.getRotationMatrix() */
	public static boolean getRotationMatrix(float[] R, float[] I,
			float[] gravity, float[] geomagnetic) {
		float Ax = gravity[0];
		float Ay = gravity[1];
		float Az = gravity[2];

		final float normsqA = (Ax * Ax + Ay * Ay + Az * Az);
		final float g = 9.81f;
		final float freeFallGravitySquared = 0.01f * g * g;
		if (normsqA < freeFallGravitySquared) {
			return false;
		}

		final float Ex = geomagnetic[0];
		final float Ey = geomagnetic[1];
		final float Ez = geomagnetic[2];
		float Hx = Ey * Az - Ez * Ay;
		float Hy = Ez * Ax - Ex * Az;
		float Hz = Ex * Ay - Ey * Ax;
		final float normH = (float) Math.sqrt(Hx * Hx + Hy * Hy + Hz * Hz);

		if (normH < 0.1f) {
			return false;
		}
		final float invH = 1.0f / normH;
		Hx *= invH;
		Hy *= invH;
		Hz *= invH;
		final float invA = 1.0f / (float) Math.sqrt(Ax * Ax + Ay * Ay + Az * Az);
		Ax *= invA;
		Ay *= invA;
		Az *= invA;
		final float Mx = Ay * Hz - Az * Hy;
		final float My = Az * Hx - Ax * Hz;
		final float Mz = Ax * Hy - Ay * Hx;
		if (R != null) {
			if (R.length == 9) {
				R[0] = Hx;	 R[1] = Hy;	 R[2] = Hz;
				R[3] = Mx;	 R[4] = My;	 R[5] = Mz;
				R[6] = Ax;	 R[7] = Ay;	 R[8] = Az;
			} else if (R.length == 16) {
				R[0]  = Hx;	R[1]  = Hy;	R[2]  = Hz;   R[3]  = 0;
				R[4]  = Mx;	R[5]  = My;	R[6]  = Mz;   R[7]  = 0;
				R[8]  = Ax;	R[9]  = Ay;	R[10] = Az;   R[11] = 0;
				R[12] = 0;	 R[13] = 0;	 R[14] = 0;	R[15] = 1;
			}
		}
		if (I != null) {
			final float invE = 1.0f / (float) Math.sqrt(Ex * Ex + Ey * Ey + Ez * Ez);
			final float c = (Ex * Mx + Ey * My + Ez * Mz) * invE;
			final float s = (Ex * Ax + Ey * Ay + Ez * Az) * invE;
			if (I.length == 9) {
				I[0] = 1;	 I[1] = 0;	 I[2] = 0;
				I[3] = 0;	 I[4] = c;	 I[5] = s;
				I[6] = 0;	 I[7] = -s;	 I[8] = c;
			} else if (I.length == 16) {
				I[0] = 1;	 I[1] = 0;	 I[2] = 0;
				I[4] = 0;	 I[5] = c;	 I[6] = s;
				I[8] = 0;	 I[9] = -s;	 I[10] = c;
				I[3] = I[7] = I[11] = I[12] = I[13] = I[14] = 0;
				I[15] = 1;
			}
		}
		return true;
	}

	public static float[] getOrientation(float[] R, float[] values) {
		if (R.length == 9) {
			values[0] = (float) Math.atan2(R[1], R[4]);
			values[1] = (float) Math.asin(-R[7]);
			values[2] = (float) Math.atan2(-R[6], R[8]);
		} else {
			values[0] = (float) Math.atan2(R[1], R[5]);
			values[1] = (float) Math.asin(-R[9]);
			values[2] = (float) Math.atan2(-R[8], R[10]);
		}

		return values;
	}

	public static void getRotationMatrixFromVector(float[] R, float[] rotationVector) {

		float q0;
		float q1 = rotationVector[0];
		float q2 = rotationVector[1];
		float q3 = rotationVector[2];

		if (rotationVector.length >= 4) {
			q0 = rotationVector[3];
		} else {
			q0 = 1 - q1 * q1 - q2 * q2 - q3 * q3;
			q0 = (q0 > 0) ? (float) Math.sqrt(q0) : 0;
		}

		float sq_q1 = 2 * q1 * q1;
		float sq_q2 = 2 * q2 * q2;
		float sq_q3 = 2 * q3 * q3;
		float q1_q2 = 2 * q1 * q2;
		float q3_q0 = 2 * q3 * q0;
		float q1_q3 = 2 * q1 * q3;
		float q2_q0 = 2 * q2 * q0;
		float q2_q3 = 2 * q2 * q3;
		float q1_q0 = 2 * q1 * q0;

		if (R.length == 9) {
			R[0] = 1 - sq_q2 - sq_q3;
			R[1] = q1_q2 - q3_q0;
			R[2] = q1_q3 + q2_q0;

			R[3] = q1_q2 + q3_q0;
			R[4] = 1 - sq_q1 - sq_q3;
			R[5] = q2_q3 - q1_q0;

			R[6] = q1_q3 - q2_q0;
			R[7] = q2_q3 + q1_q0;
			R[8] = 1 - sq_q1 - sq_q2;
		} else if (R.length == 16) {
			R[0] = 1 - sq_q2 - sq_q3;
			R[1] = q1_q2 - q3_q0;
			R[2] = q1_q3 + q2_q0;
			R[3] = 0.0f;

			R[4] = q1_q2 + q3_q0;
			R[5] = 1 - sq_q1 - sq_q3;
			R[6] = q2_q3 - q1_q0;
			R[7] = 0.0f;

			R[8] = q1_q3 - q2_q0;
			R[9] = q2_q3 + q1_q0;
			R[10] = 1 - sq_q1 - sq_q2;
			R[11] = 0.0f;

			R[12] = R[13] = R[14] = 0.0f;
			R[15] = 1.0f;
		}
	}

	/* AOSP SensorManager.requestTriggerSensor() -> SystemSensorManager.requestTriggerSensorImpl(): only one-shot
	 * sensors; enabling the sensor (TriggerEventQueue.addSensor()) fails because ATL has no one-shot sensor
	 * source, which returns false as in AOSP when the sensor cannot be enabled */
	public boolean requestTriggerSensor(TriggerEventListener listener, Sensor sensor) {
		if (sensor == null) throw new IllegalArgumentException("sensor cannot be null");
		if (listener == null) throw new IllegalArgumentException("listener cannot be null");
		if (Sensor.getReportingMode(sensor) != Sensor.REPORTING_MODE_ONE_SHOT) return false;
		return false;
	}

	/* AOSP cancelTriggerSensorImpl(): no listener was ever registered (requestTriggerSensor()), so false */
	public boolean cancelTriggerSensor(TriggerEventListener listener, Sensor sensor) {
		if (sensor != null && Sensor.getReportingMode(sensor) != Sensor.REPORTING_MODE_ONE_SHOT) {
			return false;
		}
		return false;
	}
}
