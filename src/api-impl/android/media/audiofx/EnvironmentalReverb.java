
package android.media.audiofx;
import java.util.StringTokenizer;
/* AOSP media/java/android/media/audiofx/EnvironmentalReverb.java. ATL has no effect engines, so the constructor fails like
 * AudioEffect's and no instance exists; the members are AOSP's, ending in AudioEffect's checkState() answer for an
 * uninitialized effect. */
public class EnvironmentalReverb extends AudioEffect {
	private final static String TAG = "EnvironmentalReverb";
	public static final int PARAM_ROOM_LEVEL = 0;
	public static final int PARAM_ROOM_HF_LEVEL = 1;
	public static final int PARAM_DECAY_TIME = 2;
	public static final int PARAM_DECAY_HF_RATIO = 3;
	public static final int PARAM_REFLECTIONS_LEVEL = 4;
	public static final int PARAM_REFLECTIONS_DELAY = 5;
	public static final int PARAM_REVERB_LEVEL = 6;
	public static final int PARAM_REVERB_DELAY = 7;
	public static final int PARAM_DIFFUSION = 8;
	public static final int PARAM_DENSITY = 9;
	private static final int PARAM_PROPERTIES = 10;
	private OnParameterChangeListener mParamListener = null;
	private BaseParameterListener mBaseParamListener = null;
	private final Object mParamListenerLock = new Object();
	public EnvironmentalReverb(int priority, int audioSession)
	throws IllegalArgumentException, UnsupportedOperationException, RuntimeException {
		super(EFFECT_TYPE_ENV_REVERB, EFFECT_TYPE_NULL, priority, audioSession);
	}
	public void setRoomLevel(short room)
	throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
		byte[] param = shortToByteArray(room);
		checkStatus(setParameter(PARAM_ROOM_LEVEL, param));
	}
	public short getRoomLevel()
	throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
		byte[] param = new byte[2];
		checkStatus(getParameter(PARAM_ROOM_LEVEL, param));
		return byteArrayToShort(param);
	}
	public void setRoomHFLevel(short roomHF)
	throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
		byte[] param = shortToByteArray(roomHF);
		checkStatus(setParameter(PARAM_ROOM_HF_LEVEL, param));
	}
	public short getRoomHFLevel()
	throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
		byte[] param = new byte[2];
		checkStatus(getParameter(PARAM_ROOM_HF_LEVEL, param));
		return byteArrayToShort(param);
	}
	public void setDecayTime(int decayTime)
	throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
		byte[] param = intToByteArray(decayTime);
		checkStatus(setParameter(PARAM_DECAY_TIME, param));
	}
	public int getDecayTime()
	throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
		byte[] param = new byte[4];
		checkStatus(getParameter(PARAM_DECAY_TIME, param));
		return byteArrayToInt(param);
	}
	public void setDecayHFRatio(short decayHFRatio)
	throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
		byte[] param = shortToByteArray(decayHFRatio);
		checkStatus(setParameter(PARAM_DECAY_HF_RATIO, param));
	}
	public short getDecayHFRatio()
	throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
		byte[] param = new byte[2];
		checkStatus(getParameter(PARAM_DECAY_HF_RATIO, param));
		return byteArrayToShort(param);
	}
	public void setReflectionsLevel(short reflectionsLevel)
	throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
		byte[] param = shortToByteArray(reflectionsLevel);
		checkStatus(setParameter(PARAM_REFLECTIONS_LEVEL, param));
	}
	public short getReflectionsLevel()
	throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
		byte[] param = new byte[2];
		checkStatus(getParameter(PARAM_REFLECTIONS_LEVEL, param));
		return byteArrayToShort(param);
	}
	public void setReflectionsDelay(int reflectionsDelay)
	throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
		byte[] param = intToByteArray(reflectionsDelay);
		checkStatus(setParameter(PARAM_REFLECTIONS_DELAY, param));
	}
	public int getReflectionsDelay()
	throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
		byte[] param = new byte[4];
		checkStatus(getParameter(PARAM_REFLECTIONS_DELAY, param));
		return byteArrayToInt(param);
	}
	public void setReverbLevel(short reverbLevel)
	throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
		byte[] param = shortToByteArray(reverbLevel);
		checkStatus(setParameter(PARAM_REVERB_LEVEL, param));
	}
	public short getReverbLevel()
	throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
		byte[] param = new byte[2];
		checkStatus(getParameter(PARAM_REVERB_LEVEL, param));
		return byteArrayToShort(param);
	}
	public void setReverbDelay(int reverbDelay)
	throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
		byte[] param = intToByteArray(reverbDelay);
		checkStatus(setParameter(PARAM_REVERB_DELAY, param));
	}
	public int getReverbDelay()
	throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
		byte[] param = new byte[4];
		checkStatus(getParameter(PARAM_REVERB_DELAY, param));
		return byteArrayToInt(param);
	}
	public void setDiffusion(short diffusion)
	throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
		byte[] param = shortToByteArray(diffusion);
		checkStatus(setParameter(PARAM_DIFFUSION, param));
	}
	public short getDiffusion()
	throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
		byte[] param = new byte[2];
		checkStatus(getParameter(PARAM_DIFFUSION, param));
		return byteArrayToShort(param);
	}
	public void setDensity(short density)
	throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
		byte[] param = shortToByteArray(density);
		checkStatus(setParameter(PARAM_DENSITY, param));
	}
	public short getDensity()
	throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
		byte[] param = new byte[2];
		checkStatus(getParameter(PARAM_DENSITY, param));
		return byteArrayToShort(param);
	}
	public interface OnParameterChangeListener  {
		void onParameterChange(EnvironmentalReverb effect, int status, int param, int value);
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
				int v = -1;
				if (param.length == 4) {
					p = byteArrayToInt(param, 0);
				}
				if (value.length == 2) {
					v = (int)byteArrayToShort(value, 0);
				} else if (value.length == 4) {
					v = byteArrayToInt(value, 0);
				}
				if (p != -1 && v != -1) {
					l.onParameterChange(EnvironmentalReverb.this, status, p, v);
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
		public short roomLevel;
		public short roomHFLevel;
		public int decayTime;
		public short decayHFRatio;
		public short reflectionsLevel;
		public int reflectionsDelay;
		public short reverbLevel;
		public int reverbDelay;
		public short diffusion;
		public short density;
		public Settings() {
		}
		public Settings(String settings) {
			StringTokenizer st = new StringTokenizer(settings, "=;");
			int tokens = st.countTokens();
			if (st.countTokens() != 21) {
				throw new IllegalArgumentException("settings: " + settings);
			}
			String key = st.nextToken();
			if (!key.equals("EnvironmentalReverb")) {
				throw new IllegalArgumentException(
						"invalid settings for EnvironmentalReverb: " + key);
			}
			try {
				key = st.nextToken();
				if (!key.equals("roomLevel")) {
					throw new IllegalArgumentException("invalid key name: " + key);
				}
				roomLevel = Short.parseShort(st.nextToken());
				key = st.nextToken();
				if (!key.equals("roomHFLevel")) {
					throw new IllegalArgumentException("invalid key name: " + key);
				}
				roomHFLevel = Short.parseShort(st.nextToken());
				key = st.nextToken();
				if (!key.equals("decayTime")) {
					throw new IllegalArgumentException("invalid key name: " + key);
				}
				decayTime = Integer.parseInt(st.nextToken());
				key = st.nextToken();
				if (!key.equals("decayHFRatio")) {
					throw new IllegalArgumentException("invalid key name: " + key);
				}
				decayHFRatio = Short.parseShort(st.nextToken());
				key = st.nextToken();
				if (!key.equals("reflectionsLevel")) {
					throw new IllegalArgumentException("invalid key name: " + key);
				}
				reflectionsLevel = Short.parseShort(st.nextToken());
				key = st.nextToken();
				if (!key.equals("reflectionsDelay")) {
					throw new IllegalArgumentException("invalid key name: " + key);
				}
				reflectionsDelay = Integer.parseInt(st.nextToken());
				key = st.nextToken();
				if (!key.equals("reverbLevel")) {
					throw new IllegalArgumentException("invalid key name: " + key);
				}
				reverbLevel = Short.parseShort(st.nextToken());
				key = st.nextToken();
				if (!key.equals("reverbDelay")) {
					throw new IllegalArgumentException("invalid key name: " + key);
				}
				reverbDelay = Integer.parseInt(st.nextToken());
				key = st.nextToken();
				if (!key.equals("diffusion")) {
					throw new IllegalArgumentException("invalid key name: " + key);
				}
				diffusion = Short.parseShort(st.nextToken());
				key = st.nextToken();
				if (!key.equals("density")) {
					throw new IllegalArgumentException("invalid key name: " + key);
				}
				density = Short.parseShort(st.nextToken());
			 } catch (NumberFormatException nfe) {
				throw new IllegalArgumentException("invalid value for key: " + key);
			}
		}
		@Override
		public String toString() {
			return new String (
					"EnvironmentalReverb"+
					";roomLevel="+Short.toString(roomLevel)+
					";roomHFLevel="+Short.toString(roomHFLevel)+
					";decayTime="+Integer.toString(decayTime)+
					";decayHFRatio="+Short.toString(decayHFRatio)+
					";reflectionsLevel="+Short.toString(reflectionsLevel)+
					";reflectionsDelay="+Integer.toString(reflectionsDelay)+
					";reverbLevel="+Short.toString(reverbLevel)+
					";reverbDelay="+Integer.toString(reverbDelay)+
					";diffusion="+Short.toString(diffusion)+
					";density="+Short.toString(density)
					);
		}
	};
	static private int PROPERTY_SIZE = 26;
	public EnvironmentalReverb.Settings getProperties()
	throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
		byte[] param = new byte[PROPERTY_SIZE];
		checkStatus(getParameter(PARAM_PROPERTIES, param));
		Settings settings = new Settings();
		settings.roomLevel = byteArrayToShort(param, 0);
		settings.roomHFLevel = byteArrayToShort(param, 2);
		settings.decayTime = byteArrayToInt(param, 4);
		settings.decayHFRatio = byteArrayToShort(param, 8);
		settings.reflectionsLevel = byteArrayToShort(param, 10);
		settings.reflectionsDelay = byteArrayToInt(param, 12);
		settings.reverbLevel = byteArrayToShort(param, 16);
		settings.reverbDelay = byteArrayToInt(param, 18);
		settings.diffusion = byteArrayToShort(param, 22);
		settings.density = byteArrayToShort(param, 24);
		return settings;
	}
	public void setProperties(EnvironmentalReverb.Settings settings)
	throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
		byte[] param = concatArrays(shortToByteArray(settings.roomLevel),
									shortToByteArray(settings.roomHFLevel),
									intToByteArray(settings.decayTime),
									shortToByteArray(settings.decayHFRatio),
									shortToByteArray(settings.reflectionsLevel),
									intToByteArray(settings.reflectionsDelay),
									shortToByteArray(settings.reverbLevel),
									intToByteArray(settings.reverbDelay),
									shortToByteArray(settings.diffusion),
									shortToByteArray(settings.density));
		checkStatus(setParameter(PARAM_PROPERTIES, param));
	}
}
