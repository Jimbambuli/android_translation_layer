package android.os;

import java.util.Arrays;
import java.util.Comparator;

/* AOSP android.os.PowerMonitorReadings (API 36): consumed energy per PowerMonitor. ATL's PowerManager has no
 * power monitors to read, so only the data class exists */
public final class PowerMonitorReadings {
	public static final int ENERGY_UNAVAILABLE = -1;

	/** @hide */
	public static final int GRANULARITY_UNSPECIFIED = 0;
	/** @hide */
	public static final int GRANULARITY_FINE = 1;

	private final PowerMonitor[] mPowerMonitors;
	private final long[] mEnergyUws;
	private final long[] mTimestampsMs;
	private final int mGranularity;

	private static final Comparator<PowerMonitor> POWER_MONITOR_COMPARATOR = Comparator.comparingInt(pm -> pm.index);

	/** @hide */
	public PowerMonitorReadings(PowerMonitor[] powerMonitors, long[] energyUws, long[] timestampsMs, int granularity) {
		mPowerMonitors = powerMonitors;
		mEnergyUws = energyUws;
		mTimestampsMs = timestampsMs;
		mGranularity = granularity;
	}

	public long getConsumedEnergy(PowerMonitor powerMonitor) {
		int offset = Arrays.binarySearch(mPowerMonitors, powerMonitor, POWER_MONITOR_COMPARATOR);
		if (offset >= 0) {
			return mEnergyUws[offset];
		}
		return ENERGY_UNAVAILABLE;
	}

	public long getTimestampMillis(PowerMonitor powerMonitor) {
		int offset = Arrays.binarySearch(mPowerMonitors, powerMonitor, POWER_MONITOR_COMPARATOR);
		if (offset >= 0) {
			return mTimestampsMs[offset];
		}
		return 0;
	}

	/** @hide */
	public int getGranularity() {
		return mGranularity;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(" monitors: [");
		for (int i = 0; i < mPowerMonitors.length; i++) {
			if (i != 0) {
				sb.append(", ");
			}
			sb.append(mPowerMonitors[i].getName()).append(" = ").append(mEnergyUws[i]).append(" (").append(mTimestampsMs[i]).append(')');
		}
		sb.append("]");
		return sb.toString();
	}
}
