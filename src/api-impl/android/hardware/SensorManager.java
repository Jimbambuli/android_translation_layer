package android.hardware;

import android.atl.annotation.Export;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.SparseArray;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class SensorManager {

	public static float GRAVITY_EARTH = 9.81f;

	/* AOSP SensorManager.SENSOR_DELAY_* */
	public static final int SENSOR_DELAY_FASTEST = 0;
	public static final int SENSOR_DELAY_GAME = 1;
	public static final int SENSOR_DELAY_UI = 2;
	public static final int SENSOR_DELAY_NORMAL = 3;

	/* AOSP SensorManager.getDefaultSensor(int): the first sensor of the type that is a wake-up sensor for the
	 * types that are wake-up sensors by default, a non-wake-up one for the others */
	public Sensor getDefaultSensor(int type) {
		List<Sensor> l = getSensorList(type);
		boolean wakeUpSensor = false;
		if (type == Sensor.TYPE_PROXIMITY || type == Sensor.TYPE_SIGNIFICANT_MOTION
		    || type == Sensor.TYPE_TILT_DETECTOR || type == Sensor.TYPE_WAKE_GESTURE
		    || type == Sensor.TYPE_GLANCE_GESTURE || type == Sensor.TYPE_PICK_UP_GESTURE
		    || type == Sensor.TYPE_LOW_LATENCY_OFFBODY_DETECT
		    || type == Sensor.TYPE_WRIST_TILT_GESTURE
		    || type == Sensor.TYPE_DYNAMIC_SENSOR_META || type == Sensor.TYPE_HINGE_ANGLE) {
			wakeUpSensor = true;
		}
		for (Sensor sensor : l) {
			if (sensor.isWakeUpSensor() == wakeUpSensor)
				return sensor;
		}
		return null;
	}

	/* AOSP SensorManager.getDefaultSensor(int, boolean) */
	public Sensor getDefaultSensor(int type, boolean wakeUp) {
		List<Sensor> l = getSensorList(type);
		for (Sensor sensor : l) {
			if (sensor.isWakeUpSensor() == wakeUp)
				return sensor;
		}
		return null;
	}

	/* AOSP SystemSensorManager.mFullSensorsList: what the sensors HAL of this machine has. ATL's sensors HAL
	 * are the kernel's IIO devices (sysfs, polled like a HAL for IIO does; accelerometer, gyroscope,
	 * magnetometer, pressure, ambient light) and iio-sensor-proxy (net.hadess.SensorProxy, which also serves
	 * the sensors that are not IIO devices, like the hub sensors of Qualcomm phones): ambient light and
	 * proximity come from there when it has them. SensorService's virtual sensors (SensorFusion: rotation vectors,
	 * orientation, gravity, linear acceleration) are not there. */
	private static List<Sensor> sFullSensorsList;
	private static final SparseArray<Source> sHandleToSource = new SparseArray<>();

	private static synchronized List<Sensor> getFullSensorList() {
		if (sFullSensorsList != null)
			return sFullSensorsList;
		ArrayList<Sensor> list = new ArrayList<>();
		int proxy = nativeProxySensors();
		boolean proxyLight = (proxy & PROXY_LIGHT) != 0;
		boolean accel = false, magn = false;
		File[] devices = new File("/sys/bus/iio/devices").listFiles();
		if (devices != null) {
			Arrays.sort(devices);
			for (File dev : devices) {
				String name = readLine(new File(dev, "name"));
				for (IioSource source : IioSource.probe(dev)) {
					if (source.type == Sensor.TYPE_LIGHT && proxyLight)
						continue; // iio-sensor-proxy has it (and holds the device)
					/* one sensor per type, as the IIO HALs have it */
					if (source.type == Sensor.TYPE_ACCELEROMETER && accel || source.type == Sensor.TYPE_MAGNETIC_FIELD && magn)
						continue;
					accel |= source.type == Sensor.TYPE_ACCELEROMETER;
					magn |= source.type == Sensor.TYPE_MAGNETIC_FIELD;
					addSensor(list, source, name != null ? name : dev.getName(), "Linux IIO");
				}
			}
		}
		if (proxyLight)
			addSensor(list, new ProxySource(Sensor.TYPE_LIGHT, PROXY_LIGHT), "iio-sensor-proxy light", "iio-sensor-proxy");
		if ((proxy & PROXY_PROXIMITY) != 0)
			addSensor(list, new ProxySource(Sensor.TYPE_PROXIMITY, PROXY_PROXIMITY), "iio-sensor-proxy proximity", "iio-sensor-proxy");
		sFullSensorsList = Collections.unmodifiableList(list);
		return sFullSensorsList;
	}

	private static void addSensor(List<Sensor> list, Source source, String name, String vendor) {
		int handle = list.size() + 1;
		Sensor sensor = new Sensor(name, vendor, handle, source.type, source.maxRange, source.resolution,
		                           source.minDelay, source.maxDelay, source.type == Sensor.TYPE_PROXIMITY);
		source.sensor = sensor;
		sHandleToSource.put(handle, source);
		list.add(sensor);
	}

	/* AOSP SensorManager.getSensorList() */
	private final SparseArray<List<Sensor>> mSensorListByType = new SparseArray<>();

	public List<Sensor> getSensorList(int type) {
		List<Sensor> list;
		final List<Sensor> fullList = getFullSensorList();
		synchronized (mSensorListByType) {
			list = mSensorListByType.get(type);
			if (list == null) {
				if (type == Sensor.TYPE_ALL) {
					list = fullList;
				} else {
					list = new ArrayList<Sensor>();
					for (Sensor i : fullList) {
						if (i.getType() == type)
							list.add(i);
					}
				}
				list = Collections.unmodifiableList(list);
				mSensorListByType.append(type, list);
			}
		}
		return list;
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

	/* AOSP SensorManager.registerListener() variants: the rate goes through getDelay(); the events are not
	 * batched (no sensor has a FIFO), so maxReportLatencyUs only has to be valid */
	public boolean registerListener(SensorEventListener listener, Sensor sensor, int samplingPeriodUs) {
		return registerListener(listener, sensor, samplingPeriodUs, null);
	}

	public boolean registerListener(SensorEventListener listener, Sensor sensor, int samplingPeriodUs, Handler handler) {
		return registerListenerImpl(listener, sensor, getDelay(samplingPeriodUs), handler, 0);
	}

	public boolean registerListener(SensorEventListener listener, Sensor sensor, int samplingPeriodUs, int maxReportLatencyUs) {
		return registerListenerImpl(listener, sensor, getDelay(samplingPeriodUs), null, maxReportLatencyUs);
	}

	public boolean registerListener(SensorEventListener listener, Sensor sensor, int samplingPeriodUs, int maxReportLatencyUs, Handler handler) {
		return registerListenerImpl(listener, sensor, getDelay(samplingPeriodUs), handler, maxReportLatencyUs);
	}

	/* AOSP SensorManager.getDelay() */
	private static int getDelay(int rate) {
		int delay = -1;
		switch (rate) {
			case SENSOR_DELAY_FASTEST:
				delay = 0;
				break;
			case SENSOR_DELAY_GAME:
				delay = 20000;
				break;
			case SENSOR_DELAY_UI:
				delay = 66667;
				break;
			case SENSOR_DELAY_NORMAL:
				delay = 200000;
				break;
			default:
				delay = rate;
				break;
		}
		return delay;
	}

	/* AOSP SystemSensorManager.mSensorListeners: the SensorEventQueue of each listener, with the looper of the
	 * first registration (one looper per listener) */
	private final HashMap<SensorEventListener, Queue> mSensorListeners = new HashMap<>();
	private final Looper mMainLooper = Looper.getMainLooper();

	/* AOSP SystemSensorManager.registerListenerImpl() and SensorEventQueue.addSensor() */
	private boolean registerListenerImpl(SensorEventListener listener, Sensor sensor, int delayUs, Handler handler, int maxBatchReportLatencyUs) {
		if (listener == null || sensor == null)
			return false;
		if (sensor.getReportingMode() == Sensor.REPORTING_MODE_ONE_SHOT)
			return false; // trigger sensors use requestTriggerSensor()
		if (maxBatchReportLatencyUs < 0 || delayUs < 0)
			return false;
		Source source = sHandleToSource.get(sensor.getHandle());
		if (source == null || source.sensor != sensor)
			return false;
		synchronized (mSensorListeners) {
			Queue queue = mSensorListeners.get(listener);
			if (queue == null) {
				queue = new Queue(listener, new Handler(handler != null ? handler.getLooper() : mMainLooper));
				if (!queue.addSensor(source, delayUs))
					return false;
				mSensorListeners.put(listener, queue);
			} else if (!queue.addSensor(source, delayUs)) {
				return false;
			}
		}
		return true;
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
			Queue queue = mSensorListeners.get(listener);
			if (queue == null)
				return;
			boolean empty = sensor == null ? queue.removeAllSensors() : queue.removeSensor(sensor);
			if (empty)
				mSensorListeners.remove(listener);
		}
	}

	/* AOSP SystemSensorManager.SensorEventQueue: the sensors of one listener; the events are dispatched on its
	 * looper (dispatchSensorEvent(), onAccuracyChanged() when the accuracy changes) */
	private static final class Queue {
		final SensorEventListener mListener;
		final Handler mHandler;
		final SparseArray<Registration> mActiveSensors = new SparseArray<>();
		final SparseArray<Integer> mSensorAccuracies = new SparseArray<>();

		Queue(SensorEventListener listener, Handler handler) {
			mListener = listener;
			mHandler = handler;
		}

		boolean addSensor(Source source, int delayUs) {
			int handle = source.sensor.getHandle();
			if (mActiveSensors.get(handle) != null)
				return false;
			/* SensorService: the rate is limited to what the sensor can do */
			int period = source.minDelay > 0 ? Math.max(delayUs, source.minDelay) : delayUs;
			if (source.maxDelay > 0)
				period = Math.min(period, source.maxDelay);
			Registration registration = new Registration(this, source, period);
			if (!source.enable(registration))
				return false;
			mActiveSensors.put(handle, registration);
			return true;
		}

		boolean removeSensor(Sensor sensor) {
			Registration registration = mActiveSensors.get(sensor.getHandle());
			if (registration != null) {
				registration.source.disable(registration);
				mActiveSensors.remove(sensor.getHandle());
			}
			return mActiveSensors.size() == 0;
		}

		boolean removeAllSensors() {
			for (int i = 0; i < mActiveSensors.size(); i++)
				mActiveSensors.valueAt(i).source.disable(mActiveSensors.valueAt(i));
			mActiveSensors.clear();
			return true;
		}

		/* AOSP SensorEventQueue.dispatchSensorEvent(), on the queue's looper */
		void dispatchSensorEvent(final Registration registration, final float[] values, final int accuracy, final long timestamp) {
			if (Looper.myLooper() != mHandler.getLooper()) {
				mHandler.post(new Runnable() {
					@Override
					public void run() {
						dispatchSensorEvent(registration, values, accuracy, timestamp);
					}
				});
				return;
			}
			if (!registration.active)
				return; // unregistered while the event was pending
			SensorEvent t = registration.event;
			System.arraycopy(values, 0, t.values, 0, Math.min(values.length, t.values.length));
			t.timestamp = timestamp;
			t.accuracy = accuracy;
			int handle = t.sensor.getHandle();
			Integer old = mSensorAccuracies.get(handle);
			if (t.accuracy >= 0 && (old == null ? 0 : old) != t.accuracy) {
				mSensorAccuracies.put(handle, t.accuracy);
				mListener.onAccuracyChanged(t.sensor, t.accuracy);
			}
			mListener.onSensorChanged(t);
		}
	}

	/* one sensor of one queue */
	private static final class Registration {
		final Queue queue;
		final Source source;
		final int periodUs;
		final SensorEvent event;
		boolean active = true;

		Registration(Queue queue, Source source, int periodUs) {
			this.queue = queue;
			this.source = source;
			this.periodUs = periodUs;
			/* AOSP SensorEventQueue.addSensorEvent(): one event per sensor, reused for every event */
			this.event = new SensorEvent(new float[Sensor.getMaxLengthValuesArray(source.sensor, android.os.Build.VERSION.SDK_INT)], source.sensor);
		}

		void deliver(float[] values, long timestamp) {
			queue.dispatchSensorEvent(this, values, /*SENSOR_STATUS_ACCURACY_HIGH*/ 3, timestamp);
		}
	}

	/* where the events of a sensor come from */
	private static abstract class Source {
		final int type;
		float maxRange;
		float resolution;
		int minDelay;
		int maxDelay;
		Sensor sensor;

		Source(int type) {
			this.type = type;
		}

		abstract boolean enable(Registration registration);
		abstract void disable(Registration registration);
	}

	/* A continuous IIO sensor, read from sysfs at the period of each registration (the
	 * in_<channel>_<axis>_raw values with scale, offset and mount matrix, as iio-sensor-proxy reads them).
	 * Units as in the IIO ABI: m/s² (accel), rad/s (anglvel), Gauss (magn, Android: µT), kPa (pressure,
	 * Android: hPa), lux (illuminance). */
	private static final class IioSource extends Source {
		static final int MIN_DELAY_US = 10000; // polling sysfs faster than 100 Hz only costs time
		final File[] files;
		final float[] scale;
		final float[] offset;
		final float[] matrix; // the mount matrix, row-major 3x3, or null
		final float factor;

		IioSource(int type, File[] files, float[] scale, float[] offset, float[] matrix, float factor) {
			super(type);
			this.files = files;
			this.scale = scale;
			this.offset = offset;
			this.matrix = matrix;
			this.factor = factor;
			this.minDelay = type == Sensor.TYPE_LIGHT ? 0 : MIN_DELAY_US;
			this.maxDelay = 1000000;
			this.resolution = Math.abs(scale[0] * factor);
		}

		static List<IioSource> probe(File dev) {
			ArrayList<IioSource> sources = new ArrayList<>();
			/* accelerometer: Android reports the force that holds the device (+g upwards when it lies flat),
			 * the IIO acceleration is negated for that, as ATL did before */
			IioSource s = probe3(dev, "accel", Sensor.TYPE_ACCELEROMETER, -1);
			if (s != null) {
				s.maxRange = 4 * GRAVITY_EARTH;
				sources.add(s);
			}
			s = probe3(dev, "anglvel", Sensor.TYPE_GYROSCOPE, 1);
			if (s != null) {
				s.maxRange = 35;
				sources.add(s);
			}
			s = probe3(dev, "magn", Sensor.TYPE_MAGNETIC_FIELD, 100);
			if (s != null) {
				s.maxRange = 4900;
				sources.add(s);
			}
			s = probe1(dev, "pressure", Sensor.TYPE_PRESSURE, 10);
			if (s != null) {
				s.maxRange = 1100;
				sources.add(s);
			}
			s = probe1(dev, "illuminance", Sensor.TYPE_LIGHT, 1);
			if (s != null) {
				s.maxRange = 100000;
				sources.add(s);
			}
			return sources;
		}

		private static File channel(File dev, String name) {
			File f = new File(dev, name + "_input");
			if (f.isFile())
				return f;
			f = new File(dev, name + "_raw");
			return f.isFile() ? f : null;
		}

		private static float attr(File dev, String channel, String prefix, String attr, float def) {
			for (String n : new String[] {channel + "_" + attr, prefix + "_" + attr}) {
				String v = readLine(new File(dev, n));
				if (v != null) {
					try {
						return Float.parseFloat(v);
					} catch (NumberFormatException e) {
					}
				}
			}
			return def;
		}

		private static IioSource probe3(File dev, String prefix, int type, float factor) {
			File[] files = new File[3];
			float[] scale = new float[3];
			float[] offset = new float[3];
			String[] axes = {"x", "y", "z"};
			for (int i = 0; i < 3; i++) {
				String ch = "in_" + prefix + "_" + axes[i];
				files[i] = channel(dev, ch);
				if (files[i] == null)
					return null;
				boolean input = files[i].getName().endsWith("_input");
				scale[i] = input ? 1 : attr(dev, ch, "in_" + prefix, "scale", 1);
				offset[i] = input ? 0 : attr(dev, ch, "in_" + prefix, "offset", 0);
			}
			float[] matrix = null;
			for (String n : new String[] {"in_" + prefix + "_mount_matrix", "in_mount_matrix", "mount_matrix"}) {
				String v = readLine(new File(dev, n));
				if (v == null)
					continue;
				String[] parts = v.split("[,;]");
				if (parts.length != 9)
					continue;
				matrix = new float[9];
				try {
					for (int i = 0; i < 9; i++)
						matrix[i] = Float.parseFloat(parts[i].trim());
				} catch (NumberFormatException e) {
					matrix = null;
				}
				break;
			}
			return new IioSource(type, files, scale, offset, matrix, factor);
		}

		private static IioSource probe1(File dev, String prefix, int type, float factor) {
			String ch = "in_" + prefix;
			File f = channel(dev, ch);
			if (f == null)
				return null;
			boolean input = f.getName().endsWith("_input");
			return new IioSource(type, new File[] {f}, new float[] {input ? 1 : attr(dev, ch, ch, "scale", 1)},
			                     new float[] {input ? 0 : attr(dev, ch, ch, "offset", 0)}, null, factor);
		}

		/* the current values, or null if the device cannot be read */
		float[] read() {
			float[] v = new float[files.length];
			for (int i = 0; i < files.length; i++) {
				String s = readLine(files[i]);
				if (s == null)
					return null;
				try {
					v[i] = (Float.parseFloat(s) + offset[i]) * scale[i] * factor;
				} catch (NumberFormatException e) {
					return null;
				}
			}
			if (matrix != null) {
				float[] m = new float[3];
				for (int r = 0; r < 3; r++)
					m[r] = matrix[3 * r] * v[0] + matrix[3 * r + 1] * v[1] + matrix[3 * r + 2] * v[2];
				v = m;
			}
			return v;
		}

		private final HashMap<Registration, Runnable> mPollers = new HashMap<>();

		@Override
		boolean enable(final Registration registration) {
			if (read() == null)
				return false; // like a HAL that cannot activate the sensor
			Runnable poll = new Runnable() {
				@Override
				public void run() {
					if (!registration.active)
						return;
					float[] v = read();
					if (v != null)
						registration.deliver(v, SystemClock.elapsedRealtimeNanos());
					registration.queue.mHandler.postDelayed(this, Math.max(1, registration.periodUs / 1000));
				}
			};
			synchronized (mPollers) {
				mPollers.put(registration, poll);
			}
			registration.queue.mHandler.post(poll);
			return true;
		}

		@Override
		void disable(Registration registration) {
			registration.active = false;
			synchronized (mPollers) {
				Runnable poll = mPollers.remove(registration);
				if (poll != null)
					registration.queue.mHandler.removeCallbacks(poll);
			}
		}
	}

	/* An on-change sensor of iio-sensor-proxy: claimed while a listener is registered (ClaimLight/
	 * ClaimProximity), its property changes are the events; as for an on-change sensor of a HAL, the first
	 * event is the value at the time it is enabled. Proximity is reported like a binary proximity sensor
	 * (AOSP: values[0] is maxRange when far, 0 when near). */
	static final int PROXY_LIGHT = 1;
	static final int PROXY_PROXIMITY = 2;

	private static final class ProxySource extends Source {
		final int which;
		final ArrayList<Registration> registrations = new ArrayList<>();

		ProxySource(int type, int which) {
			super(type);
			this.which = which;
			this.minDelay = 0; // on-change
			this.maxDelay = 0;
			if (type == Sensor.TYPE_PROXIMITY) {
				this.maxRange = 5;
				this.resolution = 5;
			} else {
				this.maxRange = 100000;
				this.resolution = 1;
			}
		}

		@Override
		boolean enable(Registration registration) {
			boolean first;
			synchronized (registrations) {
				first = registrations.isEmpty();
				registrations.add(registration);
			}
			if (first && !nativeProxyClaim(which, true)) {
				synchronized (registrations) {
					registrations.remove(registration);
				}
				return false;
			}
			float value = nativeProxyValue(which);
			if (!Float.isNaN(value))
				registration.deliver(values(value), SystemClock.elapsedRealtimeNanos());
			return true;
		}

		@Override
		void disable(Registration registration) {
			registration.active = false;
			boolean last;
			synchronized (registrations) {
				last = registrations.remove(registration) && registrations.isEmpty();
			}
			if (last)
				nativeProxyClaim(which, false);
		}

		float[] values(float value) {
			if (type == Sensor.TYPE_PROXIMITY)
				return new float[] {value != 0 ? 0 : maxRange};
			return new float[] {value};
		}

		void changed(float value) {
			Registration[] regs;
			synchronized (registrations) {
				regs = registrations.toArray(new Registration[0]);
			}
			long now = SystemClock.elapsedRealtimeNanos();
			for (Registration r : regs)
				r.deliver(values(value), now);
		}
	}

	/* called by the native code when a claimed property of iio-sensor-proxy changed (main thread) */
	@Export
	static void onProxyValue(int which, float value) {
		synchronized (SensorManager.class) {
			for (int i = 0; i < sHandleToSource.size(); i++) {
				Source source = sHandleToSource.valueAt(i);
				if (source instanceof ProxySource && ((ProxySource)source).which == which)
					((ProxySource)source).changed(value);
			}
		}
	}

	private static String readLine(File f) {
		try (FileInputStream in = new FileInputStream(f)) {
			byte[] buf = new byte[256];
			int n = in.read(buf);
			if (n <= 0)
				return null;
			return new String(buf, 0, n).trim();
		} catch (IOException | SecurityException e) {
			return null;
		}
	}

	/* the sensors iio-sensor-proxy has (PROXY_* bits), 0 if it does not run */
	private static native int nativeProxySensors();
	private static native boolean nativeProxyClaim(int which, boolean claim);
	/* the current value (light: lux, proximity: 1 near, 0 far), NaN if unknown */
	private static native float nativeProxyValue(int which);

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
