package android.os;

public final class PowerManager {
	public final class WakeLock {
		private boolean held;
		private boolean refCounted = true;
		private int count;

		public void setReferenceCounted(boolean referenceCounted) {
			refCounted = referenceCounted;
		}

		public void acquire() {
			if (refCounted) {
				count++;
			}
			held = true;
		}

		public void release() {
			if (refCounted) {
				count--;
				if (count > 0)
					return;
			}
			held = false;
		}

		public boolean isHeld() {
			return held;
		}

		public void acquire(long timeout) {
			acquire();
			new Thread(new Runnable() {
				public void run() {
					try {
						Thread.sleep(timeout);
					} catch (InterruptedException e) {
					}
					release();
				}
			}).start();
		}

		public void release(int flags) {
			release();
		}

		/* AOSP WakeLock.setWorkSource(): an empty source counts as none; the lock keeps a copy, which a held lock
		 * reports to the PowerManagerService (updateWakeLockWorkSource). ATL has no such service; whether it
		 * would check a permission there is not in the local sources (services/) and is not checked. */
		private WorkSource mWorkSource;

		public void setWorkSource(WorkSource ws) {
			synchronized (this) {
				if (ws != null && ws.isEmpty()) {
					ws = null;
				}
				mWorkSource = ws == null ? null : new WorkSource(ws);
			}
		}
	}

	public WakeLock newWakeLock(int levelAndFlags, String tag) {
		return new WakeLock();
	}

	public void goToSleep(long time) {
		// This requires DEVICE_POWER permission which is only possible for system processes. Any normal application gets a SecurityException.
		throw new SecurityException();
	}

	public void userActivity(long dummy, boolean dummy2) {}

	public static final int FULL_WAKE_LOCK = 0x1a;

	/* AOSP PowerManager thermal API (THERMAL_STATUS_* = Temperature.THROTTLING_*). ATL has no ThermalManagerService:
	 * the status is NONE (a device that is not throttled), the headroom is NaN as documented for a device without
	 * the HAL ("NaN if not supported"), listeners are kept but never called, and the sustained performance mode
	 * is off (config_sustainedPerformanceModeSupported is false in AOSP's config.xml) */
	public static final int THERMAL_STATUS_NONE = 0;
	public static final int THERMAL_STATUS_LIGHT = 1;
	public static final int THERMAL_STATUS_MODERATE = 2;
	public static final int THERMAL_STATUS_SEVERE = 3;
	public static final int THERMAL_STATUS_CRITICAL = 4;
	public static final int THERMAL_STATUS_EMERGENCY = 5;
	public static final int THERMAL_STATUS_SHUTDOWN = 6;

	public interface OnThermalStatusChangedListener {
		void onThermalStatusChanged(int status);
	}

	private final java.util.Map<OnThermalStatusChangedListener, java.util.concurrent.Executor> mThermalListeners = new java.util.HashMap<>();

	public int getCurrentThermalStatus() {
		return THERMAL_STATUS_NONE;
	}

	public void addThermalStatusListener(OnThermalStatusChangedListener listener) {
		java.util.Objects.requireNonNull(listener, "listener cannot be null");
		addThermalStatusListener(Runnable::run, listener);
	}

	public void addThermalStatusListener(java.util.concurrent.Executor executor, OnThermalStatusChangedListener listener) {
		java.util.Objects.requireNonNull(listener, "listener cannot be null");
		java.util.Objects.requireNonNull(executor, "executor cannot be null");
		synchronized (mThermalListeners) {
			if (mThermalListeners.containsKey(listener))
				throw new IllegalArgumentException("Listener already registered: " + listener);
			mThermalListeners.put(listener, executor);
		}
	}

	public void removeThermalStatusListener(OnThermalStatusChangedListener listener) {
		java.util.Objects.requireNonNull(listener, "listener cannot be null");
		synchronized (mThermalListeners) {
			if (mThermalListeners.remove(listener) == null)
				throw new IllegalArgumentException("Listener was not added");
		}
	}

	public float getThermalHeadroom(int forecastSeconds) {
		return Float.NaN;
	}

	public boolean isSustainedPerformanceModeSupported() {
		return false;
	}

	public static final int LOCATION_MODE_NO_CHANGE = 0;
	public static final int LOCATION_MODE_GPS_DISABLED_WHEN_SCREEN_OFF = 1;
	public static final int LOCATION_MODE_ALL_DISABLED_WHEN_SCREEN_OFF = 2;
	public static final int LOCATION_MODE_FOREGROUND_ONLY = 3;
	public static final int LOCATION_MODE_THROTTLE_REQUESTS_WHEN_SCREEN_OFF = 4;

	/* AOSP getLocationPowerSaveMode(): without battery saver (isPowerSaveMode() is false) nothing changes */
	public int getLocationPowerSaveMode() {
		return LOCATION_MODE_NO_CHANGE;
	}

	public boolean isPowerSaveMode() {
		return false;
	}

	public boolean isScreenOn() {
		return true;
	}

	public boolean isIgnoringBatteryOptimizations(String packageName) {
		return true;
	}

	public boolean isDeviceIdleMode() {
		return false;
	}

	/* AOSP PowerManager.isDeviceLightIdleMode asks the power manager service; ATL has none and
	 * never enters doze, which is what the service reports for an active device. */
	public boolean isDeviceLightIdleMode() {
		return false;
	}

	/* AOSP PowerManager.isLowPowerStandbyEnabled: low power standby is off by default on Android. */
	public boolean isLowPowerStandbyEnabled() {
		return false;
	}

	public boolean isInteractive() {
		return true;
	}

	public boolean isWakeLockLevelSupported(int level) {
		return false;
	}
}
