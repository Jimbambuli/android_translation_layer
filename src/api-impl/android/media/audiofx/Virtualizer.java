
package android.media.audiofx;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.util.Log;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.StringTokenizer;
/* AOSP media/java/android/media/audiofx/Virtualizer.java. ATL has no effect engines, so the constructor fails like
 * AudioEffect's and no instance exists; the members are AOSP's, ending in AudioEffect's checkState() answer for an
 * uninitialized effect. */
public class Virtualizer extends AudioEffect {
	private final static String TAG = "Virtualizer";
	private final static boolean DEBUG = false;
	public static final int PARAM_STRENGTH_SUPPORTED = 0;
	public static final int PARAM_STRENGTH = 1;
	static final int PARAM_VIRTUAL_SPEAKER_ANGLES = 2;
	static final int PARAM_FORCE_VIRTUALIZATION_MODE = 3;
	static final int PARAM_VIRTUALIZATION_MODE = 4;
	private boolean mStrengthSupported = false;
	private OnParameterChangeListener mParamListener = null;
	private BaseParameterListener mBaseParamListener = null;
	private final Object mParamListenerLock = new Object();
	public Virtualizer(int priority, int audioSession)
	throws IllegalStateException, IllegalArgumentException,
		   UnsupportedOperationException, RuntimeException {
		super(EFFECT_TYPE_VIRTUALIZER, EFFECT_TYPE_NULL, priority, audioSession);
		if (audioSession == 0) {
			Log.w(TAG, "WARNING: attaching a Virtualizer to global output mix is deprecated!");
		}
		int[] value = new int[1];
		checkStatus(getParameter(PARAM_STRENGTH_SUPPORTED, value));
		mStrengthSupported = (value[0] != 0);
	}
	public boolean getStrengthSupported() {
	   return mStrengthSupported;
	}
	public void setStrength(short strength)
	throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
		checkStatus(setParameter(PARAM_STRENGTH, strength));
	}
	public short getRoundedStrength()
	throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
		short[] value = new short[1];
		checkStatus(getParameter(PARAM_STRENGTH, value));
		return value[0];
	}
	/* AOSP AudioFormat CHANNEL_OUT_DEFAULT / CHANNEL_OUT_STEREO (CHANNEL_OUT_FRONT_LEFT 0x4 | CHANNEL_OUT_FRONT_RIGHT 0x8) */
	private static final int CHANNEL_OUT_DEFAULT = 1;
	private static final int CHANNEL_OUT_STEREO = 0x4 | 0x8;

	/* AOSP AudioDeviceInfo.convertDeviceTypeToInternalDevice(): EXT_TO_INT_DEVICE_MAPPING.get(type, DEVICE_NONE) with
	 * the AudioSystem.DEVICE_OUT_* values of AOSP media/java/android/media/AudioSystem.java */
	private static int convertDeviceTypeToInternalDevice(int deviceType) {
		switch (deviceType) {
			case AudioDeviceInfo.TYPE_BUILTIN_EARPIECE: return 0x1;
			case AudioDeviceInfo.TYPE_BUILTIN_SPEAKER: return 0x2;
			case AudioDeviceInfo.TYPE_WIRED_HEADSET: return 0x4;
			case AudioDeviceInfo.TYPE_WIRED_HEADPHONES: return 0x8;
			case AudioDeviceInfo.TYPE_LINE_ANALOG: return 0x20000;
			case AudioDeviceInfo.TYPE_LINE_DIGITAL: return 0x80000;
			case AudioDeviceInfo.TYPE_BLUETOOTH_SCO: return 0x10;
			case AudioDeviceInfo.TYPE_BLUETOOTH_A2DP: return 0x80;
			case AudioDeviceInfo.TYPE_HDMI: return 0x400;
			case AudioDeviceInfo.TYPE_HDMI_ARC: return 0x40000;
			case AudioDeviceInfo.TYPE_HDMI_EARC: return 0x40001;
			case AudioDeviceInfo.TYPE_USB_DEVICE: return 0x4000;
			case AudioDeviceInfo.TYPE_USB_HEADSET: return 0x4000000;
			case AudioDeviceInfo.TYPE_USB_ACCESSORY: return 0x2000;
			case AudioDeviceInfo.TYPE_DOCK: return 0x1000;
			case AudioDeviceInfo.TYPE_DOCK_ANALOG: return 0x800;
			case AudioDeviceInfo.TYPE_FM: return 0x100000;
			case AudioDeviceInfo.TYPE_TELEPHONY: return 0x10000;
			case AudioDeviceInfo.TYPE_AUX_LINE: return 0x200000;
			case AudioDeviceInfo.TYPE_IP: return 0x800000;
			case AudioDeviceInfo.TYPE_BUS: return 0x1000000;
			case AudioDeviceInfo.TYPE_HEARING_AID: return 0x8000000;
			case AudioDeviceInfo.TYPE_BUILTIN_SPEAKER_SAFE: return 0x400000;
			case AudioDeviceInfo.TYPE_REMOTE_SUBMIX: return 0x8000;
			case AudioDeviceInfo.TYPE_BLE_HEADSET: return 0x20000000;
			case AudioDeviceInfo.TYPE_BLE_SPEAKER: return 0x20000001;
			case AudioDeviceInfo.TYPE_BLE_BROADCAST: return 0x20000002;
			default: return 0;
		}
	}

	private boolean getAnglesInt(int inputChannelMask, int deviceType, int[] angles)
			throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
		if (inputChannelMask == AudioFormat.CHANNEL_INVALID) {
			throw (new IllegalArgumentException(
					"Virtualizer: illegal CHANNEL_INVALID channel mask"));
		}
		int channelMask = inputChannelMask == CHANNEL_OUT_DEFAULT ?
				CHANNEL_OUT_STEREO : inputChannelMask;
		int nbChannels = Integer.bitCount(channelMask);
		if ((angles != null) && (angles.length < (nbChannels * 3))) {
			Log.e(TAG, "Size of array for angles cannot accomodate number of channels in mask ("
					+ nbChannels + ")");
			throw (new IllegalArgumentException(
					"Virtualizer: array for channel / angle pairs is too small: is " + angles.length
					+ ", should be " + (nbChannels * 3)));
		}
		ByteBuffer paramsConverter = ByteBuffer.allocate(3  * 4);
		paramsConverter.order(ByteOrder.nativeOrder());
		paramsConverter.putInt(PARAM_VIRTUAL_SPEAKER_ANGLES);
		paramsConverter.putInt((channelMask >> 2));
		paramsConverter.putInt(convertDeviceTypeToInternalDevice(deviceType));
		byte[] result = new byte[nbChannels * 4 * 3];
		int status = getParameter(paramsConverter.array(), result);
		if (DEBUG) {
			Log.v(TAG, "getAngles(0x" + Integer.toHexString(inputChannelMask) + ", 0x"
					+ Integer.toHexString(deviceType) + ") returns " + status);
		}
		if (status >= 0) {
			if (angles != null) {
				ByteBuffer resultConverter = ByteBuffer.wrap(result);
				resultConverter.order(ByteOrder.nativeOrder());
				for (int i = 0 ; i < nbChannels ; i++) {
					angles[3 * i] = (
							resultConverter.getInt((i * 4 * 3 << 2)));
					angles[3 * i + 1] = resultConverter.getInt(i * 4 * 3 + 4);
					angles[3 * i + 2] = resultConverter.getInt(i * 4 * 3 + 8);
					if (DEBUG) {
						Log.v(TAG, "channel 0x" + Integer.toHexString(angles[3*i]).toUpperCase()
								+ " at az=" + angles[3*i+1] + "deg"
								+ " elev="  + angles[3*i+2] + "deg");
					}
				}
			}
			return true;
		} else if (status == AudioEffect.ERROR_BAD_VALUE) {
			return false;
		} else {
			checkStatus(status);
		}
		Log.e(TAG, "unexpected status code " + status
				+ " after getParameter(PARAM_VIRTUAL_SPEAKER_ANGLES)");
		return false;
	}
	public static final int VIRTUALIZATION_MODE_OFF = 0;
	public static final int VIRTUALIZATION_MODE_AUTO = 1;
	public final static int VIRTUALIZATION_MODE_BINAURAL = 2;
	public final static int VIRTUALIZATION_MODE_TRANSAURAL = 3;
	private static int getDeviceForModeQuery(int virtualizationMode)
			throws IllegalArgumentException {
		switch (virtualizationMode) {
			case VIRTUALIZATION_MODE_BINAURAL:
				return AudioDeviceInfo.TYPE_WIRED_HEADPHONES;
			case VIRTUALIZATION_MODE_TRANSAURAL:
				return AudioDeviceInfo.TYPE_BUILTIN_SPEAKER;
			default:
				throw (new IllegalArgumentException(
						"Virtualizer: illegal virtualization mode " + virtualizationMode));
		}
	}
	private static int getDeviceForModeForce(int virtualizationMode)
			throws IllegalArgumentException {
		if (virtualizationMode == VIRTUALIZATION_MODE_AUTO) {
			return AudioDeviceInfo.TYPE_UNKNOWN;
		} else {
			return getDeviceForModeQuery(virtualizationMode);
		}
	}
	private static int deviceToMode(int deviceType) {
		switch (deviceType) {
			case AudioDeviceInfo.TYPE_WIRED_HEADSET:
			case AudioDeviceInfo.TYPE_WIRED_HEADPHONES:
			case AudioDeviceInfo.TYPE_BLUETOOTH_SCO:
			case AudioDeviceInfo.TYPE_BUILTIN_EARPIECE:
			case AudioDeviceInfo.TYPE_USB_HEADSET:
				return VIRTUALIZATION_MODE_BINAURAL;
			case AudioDeviceInfo.TYPE_BUILTIN_SPEAKER:
			case AudioDeviceInfo.TYPE_LINE_ANALOG:
			case AudioDeviceInfo.TYPE_LINE_DIGITAL:
			case AudioDeviceInfo.TYPE_BLUETOOTH_A2DP:
			case AudioDeviceInfo.TYPE_HDMI:
			case AudioDeviceInfo.TYPE_HDMI_ARC:
			case AudioDeviceInfo.TYPE_USB_DEVICE:
			case AudioDeviceInfo.TYPE_USB_ACCESSORY:
			case AudioDeviceInfo.TYPE_DOCK:
			case AudioDeviceInfo.TYPE_FM:
			case AudioDeviceInfo.TYPE_AUX_LINE:
				return VIRTUALIZATION_MODE_TRANSAURAL;
			case AudioDeviceInfo.TYPE_UNKNOWN:
			default:
				return VIRTUALIZATION_MODE_OFF;
		}
	}
	public boolean canVirtualize(int inputChannelMask, int virtualizationMode)
			throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
		return getAnglesInt(inputChannelMask, getDeviceForModeQuery(virtualizationMode), null);
	}
	public boolean getSpeakerAngles(int inputChannelMask,
			int virtualizationMode, int[] angles)
			throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
		if (angles == null) {
			throw (new IllegalArgumentException(
					"Virtualizer: illegal null channel / angle array"));
		}
		return getAnglesInt(inputChannelMask, getDeviceForModeQuery(virtualizationMode), angles);
	}
	public boolean forceVirtualizationMode(int virtualizationMode)
			throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
		int deviceType = getDeviceForModeForce(virtualizationMode);
		int internalDevice = convertDeviceTypeToInternalDevice(deviceType);
		int status = setParameter(PARAM_FORCE_VIRTUALIZATION_MODE, internalDevice);
		if (status >= 0) {
			return true;
		} else if (status == AudioEffect.ERROR_BAD_VALUE) {
			return false;
		} else {
			checkStatus(status);
		}
		Log.e(TAG, "unexpected status code " + status
				+ " after setParameter(PARAM_FORCE_VIRTUALIZATION_MODE)");
		return false;
	}
	public int getVirtualizationMode()
			throws IllegalStateException, UnsupportedOperationException {
		int[] value = new int[1];
		int status = getParameter(PARAM_VIRTUALIZATION_MODE, value);
		if (status >= 0) {
			return deviceToMode(AudioDeviceInfo.convertInternalDeviceToDeviceType(value[0]));
		} else if (status == AudioEffect.ERROR_BAD_VALUE) {
			return VIRTUALIZATION_MODE_OFF;
		} else {
			checkStatus(status);
		}
		Log.e(TAG, "unexpected status code " + status
				+ " after getParameter(PARAM_VIRTUALIZATION_MODE)");
		return VIRTUALIZATION_MODE_OFF;
	}
	public interface OnParameterChangeListener  {
		void onParameterChange(Virtualizer effect, int status, int param, short value);
	}
	private class BaseParameterListener implements AudioEffect.OnParameterChangeListener {
		private BaseParameterListener() {
		}
		public void onParameterChange(AudioEffect effect, int status, byte[] param, byte[] value) {
			OnParameterChangeListener l = null;
			synchronized (mParamListenerLock) {
				if (mParamListener != null) {
					l = mParamListener;
				}
			}
			if (l != null) {
				int p = -1;
				short v = -1;
				if (param.length == 4) {
					p = byteArrayToInt(param, 0);
				}
				if (value.length == 2) {
					v = byteArrayToShort(value, 0);
				}
				if (p != -1 && v != -1) {
					l.onParameterChange(Virtualizer.this, status, p, v);
				}
			}
		}
	}
	public void setParameterListener(OnParameterChangeListener listener) {
		synchronized (mParamListenerLock) {
			if (mParamListener == null) {
				mParamListener = listener;
				mBaseParamListener = new BaseParameterListener();
				super.setParameterListener(mBaseParamListener);
			}
		}
	}
	public static class Settings {
		public short strength;
		public Settings() {
		}
		public Settings(String settings) {
			StringTokenizer st = new StringTokenizer(settings, "=;");
			int tokens = st.countTokens();
			if (st.countTokens() != 3) {
				throw new IllegalArgumentException("settings: " + settings);
			}
			String key = st.nextToken();
			if (!key.equals("Virtualizer")) {
				throw new IllegalArgumentException(
						"invalid settings for Virtualizer: " + key);
			}
			try {
				key = st.nextToken();
				if (!key.equals("strength")) {
					throw new IllegalArgumentException("invalid key name: " + key);
				}
				strength = Short.parseShort(st.nextToken());
			 } catch (NumberFormatException nfe) {
				throw new IllegalArgumentException("invalid value for key: " + key);
			}
		}
		@Override
		public String toString() {
			String str = new String (
					"Virtualizer"+
					";strength="+Short.toString(strength)
					);
			return str;
		}
	};
	public Virtualizer.Settings getProperties()
	throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
		Settings settings = new Settings();
		short[] value = new short[1];
		checkStatus(getParameter(PARAM_STRENGTH, value));
		settings.strength = value[0];
		return settings;
	}
	public void setProperties(Virtualizer.Settings settings)
	throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
		checkStatus(setParameter(PARAM_STRENGTH, settings.strength));
	}
}
