
package android.media.audiofx;
import java.util.StringTokenizer;
/* AOSP media/java/android/media/audiofx/PresetReverb.java. ATL has no effect engines, so the constructor fails like
 * AudioEffect's and no instance exists; the members are AOSP's, ending in AudioEffect's checkState() answer for an
 * uninitialized effect. */
public class PresetReverb extends AudioEffect {
	private final static String TAG = "PresetReverb";
	public static final int PARAM_PRESET = 0;
	public static final short PRESET_NONE        = 0;
	public static final short PRESET_SMALLROOM   = 1;
	public static final short PRESET_MEDIUMROOM  = 2;
	public static final short PRESET_LARGEROOM   = 3;
	public static final short PRESET_MEDIUMHALL  = 4;
	public static final short PRESET_LARGEHALL   = 5;
	public static final short PRESET_PLATE       = 6;
	private OnParameterChangeListener mParamListener = null;
	private BaseParameterListener mBaseParamListener = null;
	private final Object mParamListenerLock = new Object();
	public PresetReverb(int priority, int audioSession)
	throws IllegalArgumentException, UnsupportedOperationException, RuntimeException {
		super(EFFECT_TYPE_PRESET_REVERB, EFFECT_TYPE_NULL, priority, audioSession);
	}
	public void setPreset(short preset)
	throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
		checkStatus(setParameter(PARAM_PRESET, preset));
	}
	public short getPreset()
	throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
		short[] value = new short[1];
		checkStatus(getParameter(PARAM_PRESET, value));
		return value[0];
	}
	public interface OnParameterChangeListener  {
		void onParameterChange(PresetReverb effect, int status, int param, short value);
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
					l.onParameterChange(PresetReverb.this, status, p, v);
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
		public short preset;
		public Settings() {
		}
		public Settings(String settings) {
			StringTokenizer st = new StringTokenizer(settings, "=;");
			int tokens = st.countTokens();
			if (st.countTokens() != 3) {
				throw new IllegalArgumentException("settings: " + settings);
			}
			String key = st.nextToken();
			if (!key.equals("PresetReverb")) {
				throw new IllegalArgumentException(
						"invalid settings for PresetReverb: " + key);
			}
			try {
				key = st.nextToken();
				if (!key.equals("preset")) {
					throw new IllegalArgumentException("invalid key name: " + key);
				}
				preset = Short.parseShort(st.nextToken());
			 } catch (NumberFormatException nfe) {
				throw new IllegalArgumentException("invalid value for key: " + key);
			}
		}
		@Override
		public String toString() {
			String str = new String (
					"PresetReverb"+
					";preset="+Short.toString(preset)
					);
			return str;
		}
	};
	public PresetReverb.Settings getProperties()
	throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
		Settings settings = new Settings();
		short[] value = new short[1];
		checkStatus(getParameter(PARAM_PRESET, value));
		settings.preset = value[0];
		return settings;
	}
	public void setProperties(PresetReverb.Settings settings)
	throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
		checkStatus(setParameter(PARAM_PRESET, settings.preset));
	}
}
