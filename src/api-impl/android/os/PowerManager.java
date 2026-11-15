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

		/* AOSP WakeLock.setStateListener(): the listener is told when the system disables or enables the
		 * lock (power allowlist, quota, cached app); ATL has no such service, a lock is never disabled, so the
		 * listener is kept and never called */
		private WakeLockStateListener mListener;

		public void setStateListener(java.util.concurrent.Executor executor, WakeLockStateListener listener) {
			java.util.Objects.requireNonNull(executor, "executor cannot be null");
			synchronized (this) {
				mListener = listener;
			}
		}
	}

	/* AOSP PowerManager.WakeLockStateListener */
	public interface WakeLockStateListener {
		void onStateChanged(boolean enabled);
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

	/* AOSP PowerManager.OnThermalHeadroomChangedListener; like the thermal status listeners the listeners are kept
	 * and never called (no ThermalManagerService, never throttled) */
	public interface OnThermalHeadroomChangedListener {
		void onThermalHeadroomChanged(float headroom, float forecastHeadroom, int forecastSeconds,
		                              java.util.Map<Integer, Float> thresholds);
	}

	private final java.util.Map<OnThermalHeadroomChangedListener, java.util.concurrent.Executor> mThermalHeadroomListeners =
	    new java.util.HashMap<>();

	public void addThermalHeadroomListener(OnThermalHeadroomChangedListener listener) {
		java.util.Objects.requireNonNull(listener, "Thermal headroom listener cannot be null");
		addThermalHeadroomListener(Runnable::run, listener);
	}

	public void addThermalHeadroomListener(java.util.concurrent.Executor executor, OnThermalHeadroomChangedListener listener) {
		java.util.Objects.requireNonNull(listener, "Thermal headroom listener cannot be null");
		java.util.Objects.requireNonNull(executor, "Executor cannot be null");
		synchronized (mThermalHeadroomListeners) {
			if (mThermalHeadroomListeners.containsKey(listener))
				throw new IllegalArgumentException("Thermal headroom listener already registered: " + listener);
			mThermalHeadroomListeners.put(listener, executor);
		}
	}

	public void removeThermalHeadroomListener(OnThermalHeadroomChangedListener listener) {
		java.util.Objects.requireNonNull(listener, "Thermal headroom listener cannot be null");
		synchronized (mThermalHeadroomListeners) {
			if (mThermalHeadroomListeners.remove(listener) == null)
				throw new IllegalArgumentException("Thermal headroom listener was not added");
		}
	}

	/* AOSP getThermalHeadroomThresholds(): the map holds a status only if the manufacturer defined a threshold
	 * for it (convertThresholdsToMap() drops NaN); without sensors there is none */
	public java.util.Map<Integer, Float> getThermalHeadroomThresholds() {
		return new android.util.ArrayMap<>();
	}

	/* AOSP getBatteryDischargePrediction(): null "if the device is powered, charging, or an error was
	 * encountered"; a device without battery is powered */
	public java.time.Duration getBatteryDischargePrediction() {
		return null;
	}

	public boolean isBatteryDischargePredictionPersonalized() {
		return false;
	}

	/* AOSP isExemptFromLowPowerStandby()/isAllowedInLowPowerStandby(): true "if Low Power Standby is disabled",
	 * which is the default (see isLowPowerStandbyEnabled()) */
	public boolean isExemptFromLowPowerStandby() {
		return true;
	}

	public boolean isAllowedInLowPowerStandby(int reason) {
		return true;
	}

	public boolean isAllowedInLowPowerStandby(String feature) {
		return true;
	}

	/* AOSP PowerManager.isRebootingUserspaceSupportedImpl() is false */
	public static boolean isRebootingUserspaceSupportedImpl() {
		return false;
	}

	public boolean isRebootingUserspaceSupported() {
		return isRebootingUserspaceSupportedImpl();
	}

	public static final String REBOOT_USERSPACE = "userspace";

	/* AOSP reboot(): the userspace check, then PowerManagerService.reboot() needs the REBOOT permission, which a
	 * normal application does not hold (SecurityException, as for goToSleep() above) */
	public void reboot(String reason) {
		if (REBOOT_USERSPACE.equals(reason) && !isRebootingUserspaceSupported()) {
			throw new UnsupportedOperationException("Attempted userspace reboot on a device that doesn't support it");
		}
		throw new SecurityException();
	}
}
