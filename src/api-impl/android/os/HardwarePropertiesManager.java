package android.os;

import android.content.Context;
import android.util.Log;

/* AOSP HardwarePropertiesManager. The calls go to HardwarePropertiesManagerService, which only answers the device
 * owner and the current VR listener (enforceHardwarePropertiesRetrievalAllowed(): "The caller is neither a device
 * owner nor the current VrListener"); ATL's app is neither, so after the argument checks every call is a
 * SecurityException. */
public class HardwarePropertiesManager {
	private static final String TAG = HardwarePropertiesManager.class.getSimpleName();

	public static final int DEVICE_TEMPERATURE_CPU = 0;
	public static final int DEVICE_TEMPERATURE_GPU = 1;
	public static final int DEVICE_TEMPERATURE_BATTERY = 2;
	public static final int DEVICE_TEMPERATURE_SKIN = 3;

	public static final int TEMPERATURE_CURRENT = 0;
	public static final int TEMPERATURE_THROTTLING = 1;
	public static final int TEMPERATURE_SHUTDOWN = 2;
	public static final int TEMPERATURE_THROTTLING_BELOW_VR_MIN = 3;

	public static final float UNDEFINED_TEMPERATURE = -Float.MAX_VALUE;

	private final Context mContext;

	public HardwarePropertiesManager(Context context) {
		mContext = context;
	}

	public float[] getDeviceTemperatures(int type, int source) {
		switch (type) {
		case DEVICE_TEMPERATURE_CPU:
		case DEVICE_TEMPERATURE_GPU:
		case DEVICE_TEMPERATURE_BATTERY:
		case DEVICE_TEMPERATURE_SKIN:
			switch (source) {
			case TEMPERATURE_CURRENT:
			case TEMPERATURE_THROTTLING:
			case TEMPERATURE_SHUTDOWN:
			case TEMPERATURE_THROTTLING_BELOW_VR_MIN:
				throw new SecurityException("The caller is neither a device owner nor the current VrListener");
			default:
				Log.w(TAG, "Unknown device temperature source.");
				return new float[0];
			}
		default:
			Log.w(TAG, "Unknown device temperature type.");
			return new float[0];
		}
	}

	public CpuUsageInfo[] getCpuUsages() {
		throw new SecurityException("The caller is neither a device owner nor the current VrListener");
	}

	public float[] getFanSpeeds() {
		throw new SecurityException("The caller is neither a device owner nor the current VrListener");
	}
}
