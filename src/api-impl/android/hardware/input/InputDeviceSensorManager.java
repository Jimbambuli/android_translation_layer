package android.hardware.input;

import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.TriggerEventListener;
import android.os.Handler;
import android.util.Slog;
import android.view.InputDevice;
import java.util.ArrayList;
import java.util.List;

/* AOSP InputDeviceSensorManager (@hide, core/java/android/hardware/input/InputDeviceSensorManager.java): the
 * SensorManager of an input device (InputDevice.getSensorManager()). Its sensors are those the input manager
 * service lists for a device with hasSensor() (InputManagerGlobal.getSensorList()); InputReader's game
 * controllers report none, so the list is empty, getDefaultSensor() is null and a registered listener gets no
 * events, as on an Android device whose input device has no sensors. */
public class InputDeviceSensorManager {
	private static final String TAG = "InputDeviceSensorManager";

	private final InputManagerGlobal mGlobal;
	/* AOSP mInputSensorEventListeners: the listeners registered on any device's manager */
	private final ArrayList<SensorEventListener> mInputSensorEventListeners = new ArrayList<>();
	private final Object mInputSensorLock = new Object();

	public InputDeviceSensorManager(InputManagerGlobal inputManagerGlobal) {
		mGlobal = inputManagerGlobal;
	}

	SensorManager getSensorManager(int deviceId) {
		return new InputSensorManager(deviceId);
	}

	/* updateInputDeviceSensorInfoLocked(): only a device with hasSensor() has sensors */
	private List<Sensor> getFullSensorListForDevice(int deviceId) {
		InputDevice inputDevice = mGlobal.getInputDevice(deviceId);
		if (inputDevice != null && inputDevice.hasSensor())
			return mGlobal.getSensorList(deviceId);
		return new ArrayList<>();
	}

	private Sensor getSensorForInputDevice(int deviceId, int type) {
		for (Sensor sensor : getFullSensorListForDevice(deviceId)) {
			if (sensor.getType() == type)
				return sensor;
		}
		return null;
	}

	/* registerListenerInternal(): the argument checks; the listener is kept, a sensor of no input device is
	 * not enabled */
	private boolean registerListenerInternal(SensorEventListener listener, Sensor sensor, int delayUs,
	                                         int maxBatchReportLatencyUs, Handler handler) {
		if (listener == null) {
			Slog.e(TAG, "listener is null");
			return false;
		}
		if (sensor == null) {
			Slog.e(TAG, "sensor is null");
			return false;
		}
		if (sensor.getReportingMode() == 3 /* Sensor.REPORTING_MODE_ONE_SHOT */) {
			Slog.e(TAG, "Trigger Sensors should use the requestTriggerSensor.");
			return false;
		}
		if (maxBatchReportLatencyUs < 0 || delayUs < 0) {
			Slog.e(TAG, "maxBatchReportLatencyUs and delayUs should be non-negative");
			return false;
		}
		synchronized (mInputSensorLock) {
			if (!mInputSensorEventListeners.contains(listener))
				mInputSensorEventListeners.add(listener);
		}
		return true;
	}

	private void unregisterListenerInternal(SensorEventListener listener, Sensor sensor) {
		if (listener == null)
			throw new IllegalArgumentException("listener must not be null");
		/* the listener has no device sensor enabled (no input device has sensors), so it goes with any sensor */
		synchronized (mInputSensorLock) {
			mInputSensorEventListeners.remove(listener);
		}
	}

	/* AOSP SensorManager.getDelay(): the SENSOR_DELAY_* constants as microseconds, other values unchanged */
	private static int getDelay(int rate) {
		switch (rate) {
			case 0: return 0;
			case 1: return 20000;
			case 2: return 66667;
			case 3: return 200000;
			default: return rate;
		}
	}

	/* AOSP InputSensorManager: SensorManager's public methods with the *Impl() of this class (ATL's SensorManager
	 * has no *Impl() layer, so the public methods are overridden) */
	public class InputSensorManager extends SensorManager {
		final int mId;

		InputSensorManager(int deviceId) {
			mId = deviceId;
		}

		@Override
		public Sensor getDefaultSensor(int type) {
			return getSensorForInputDevice(mId, type);
		}

		@Override
		public List<Sensor> getSensorList(int type) {
			List<Sensor> list = new ArrayList<>();
			for (Sensor sensor : getFullSensorListForDevice(mId)) {
				if (type == Sensor.TYPE_ALL || sensor.getType() == type)
					list.add(sensor);
			}
			return java.util.Collections.unmodifiableList(list);
		}

		@Override
		public boolean registerListener(SensorEventListener listener, Sensor sensor, int samplingPeriodUs) {
			return registerListener(listener, sensor, samplingPeriodUs, (Handler)null);
		}

		@Override
		public boolean registerListener(SensorEventListener listener, Sensor sensor, int samplingPeriodUs, int maxReportLatencyUs) {
			return registerListenerInternal(listener, sensor, getDelay(samplingPeriodUs), maxReportLatencyUs, null);
		}

		@Override
		public boolean registerListener(SensorEventListener listener, Sensor sensor, int samplingPeriodUs, Handler handler) {
			return registerListenerInternal(listener, sensor, getDelay(samplingPeriodUs), 0, handler);
		}

		@Override
		public boolean registerListener(SensorEventListener listener, Sensor sensor, int samplingPeriodUs, int maxReportLatencyUs, Handler handler) {
			return registerListenerInternal(listener, sensor, getDelay(samplingPeriodUs), maxReportLatencyUs, handler);
		}

		@Override
		public void unregisterListener(SensorEventListener listener) {
			if (listener == null)
				return;
			unregisterListenerInternal(listener, null);
		}

		@Override
		public void unregisterListener(SensorEventListener listener, Sensor sensor) {
			if (listener == null || sensor == null)
				return;
			unregisterListenerInternal(listener, sensor);
		}

		/* requestTriggerSensorImpl() / cancelTriggerSensorImpl() of InputSensorManager */
		@Override
		public boolean requestTriggerSensor(TriggerEventListener listener, Sensor sensor) {
			return true;
		}

		@Override
		public boolean cancelTriggerSensor(TriggerEventListener listener, Sensor sensor) {
			return true;
		}
	}
}
