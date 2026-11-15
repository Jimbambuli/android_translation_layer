
package android.media.audiofx;
import android.util.Log;
import java.util.StringTokenizer;
/* AOSP media/java/android/media/audiofx/Equalizer.java. The constructor is an upstream ATL stub (Fossify Music Player) and
 * does not fail, although ATL has no equalizer engine; such an instance is an uninitialized AudioEffect, so the
 * members below, which are AOSP's, end in AudioEffect's checkState() answer (IllegalStateException). */
public class Equalizer extends AudioEffect {
	private final static String TAG = "Equalizer";
	public static final int PARAM_NUM_BANDS = 0;
	public static final int PARAM_LEVEL_RANGE = 1;
	public static final int PARAM_BAND_LEVEL = 2;
	public static final int PARAM_CENTER_FREQ = 3;
	public static final int PARAM_BAND_FREQ_RANGE = 4;
	public static final int PARAM_GET_BAND = 5;
	public static final int PARAM_CURRENT_PRESET = 6;
	public static final int PARAM_GET_NUM_OF_PRESETS = 7;
	public static final int PARAM_GET_PRESET_NAME = 8;
	private static final int PARAM_PROPERTIES = 9;
	public static final int PARAM_STRING_SIZE_MAX = 32;
	private short mNumBands = 0;
	private int mNumPresets;
	private String[] mPresetNames;
	private OnParameterChangeListener mParamListener = null;
	private BaseParameterListener mBaseParamListener = null;
	private final Object mParamListenerLock = new Object();
	/* upstream ATL stub: see the class comment */
	public Equalizer(int priority, int audioSession) {}
	public short getNumberOfBands()
	throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
		if (mNumBands != 0) {
			return mNumBands;
		}
		int[] param = new int[1];
		param[0] = PARAM_NUM_BANDS;
		short[] result = new short[1];
		checkStatus(getParameter(param, result));
		mNumBands = result[0];
		return mNumBands;
	}
	public short[] getBandLevelRange()
	throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
		short[] result = new short[2];
		checkStatus(getParameter(PARAM_LEVEL_RANGE, result));
		return result;
	}
	public void setBandLevel(short band, short level)
	throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
		int[] param = new int[2];
		short[] value = new short[1];
		param[0] = PARAM_BAND_LEVEL;
		param[1] = (int)band;
		value[0] = level;
		checkStatus(setParameter(param, value));
	}
	public short getBandLevel(short band)
	throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
		int[] param = new int[2];
		short[] result = new short[1];
		param[0] = PARAM_BAND_LEVEL;
		param[1] = (int)band;
		checkStatus(getParameter(param, result));
		return result[0];
	}
	public int getCenterFreq(short band)
	throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
		int[] param = new int[2];
		int[] result = new int[1];
		param[0] = PARAM_CENTER_FREQ;
		param[1] = (int)band;
		checkStatus(getParameter(param, result));
		return result[0];
	}
	public int[] getBandFreqRange(short band)
	throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
		int[] param = new int[2];
		int[] result = new int[2];
		param[0] = PARAM_BAND_FREQ_RANGE;
		param[1] = (int)band;
		checkStatus(getParameter(param, result));
		return result;
	}
	public short getBand(int frequency)
	throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
		int[] param = new int[2];
		short[] result = new short[1];
		param[0] = PARAM_GET_BAND;
		param[1] = frequency;
		checkStatus(getParameter(param, result));
		return result[0];
	}
	public short getCurrentPreset()
	throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
		short[] result = new short[1];
		checkStatus(getParameter(PARAM_CURRENT_PRESET, result));
		return result[0];
	}
	public void usePreset(short preset)
	throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
		checkStatus(setParameter(PARAM_CURRENT_PRESET, preset));
	}
	public short getNumberOfPresets()
	throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
		short[] result = new short[1];
		checkStatus(getParameter(PARAM_GET_NUM_OF_PRESETS, result));
		return result[0];
	}
	public String getPresetName(short preset)
	{
		if (preset >= 0 && preset < mNumPresets) {
			return mPresetNames[preset];
		} else {
			return "";
		}
	}
	public interface OnParameterChangeListener  {
		void onParameterChange(Equalizer effect, int status, int param1, int param2, int value);
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
				int p1 = -1;
				int p2 = -1;
				int v = -1;
				if (param.length >= 4) {
					p1 = byteArrayToInt(param, 0);
					if (param.length >= 8) {
						p2 = byteArrayToInt(param, 4);
					}
				}
				if (value.length == 2) {
					v = (int)byteArrayToShort(value, 0);;
				} else if (value.length == 4) {
					v = byteArrayToInt(value, 0);
				}
				if (p1 != -1 && v != -1) {
					l.onParameterChange(Equalizer.this, status, p1, p2, v);
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
		public short curPreset;
		public short numBands = 0;
		public short[] bandLevels = null;
		public Settings() {
		}
		public Settings(String settings) {
			StringTokenizer st = new StringTokenizer(settings, "=;");
			int tokens = st.countTokens();
			if (st.countTokens() < 5) {
				throw new IllegalArgumentException("settings: " + settings);
			}
			String key = st.nextToken();
			if (!key.equals("Equalizer")) {
				throw new IllegalArgumentException(
						"invalid settings for Equalizer: " + key);
			}
			try {
				key = st.nextToken();
				if (!key.equals("curPreset")) {
					throw new IllegalArgumentException("invalid key name: " + key);
				}
				curPreset = Short.parseShort(st.nextToken());
				key = st.nextToken();
				if (!key.equals("numBands")) {
					throw new IllegalArgumentException("invalid key name: " + key);
				}
				numBands = Short.parseShort(st.nextToken());
				if (st.countTokens() != numBands*2) {
					throw new IllegalArgumentException("settings: " + settings);
				}
				bandLevels = new short[numBands];
				for (int i = 0; i < numBands; i++) {
					key = st.nextToken();
					if (!key.equals("band"+(i+1)+"Level")) {
						throw new IllegalArgumentException("invalid key name: " + key);
					}
					bandLevels[i] = Short.parseShort(st.nextToken());
				}
			 } catch (NumberFormatException nfe) {
				throw new IllegalArgumentException("invalid value for key: " + key);
			}
		}
		@Override
		public String toString() {
			String str = new String (
					"Equalizer"+
					";curPreset="+Short.toString(curPreset)+
					";numBands="+Short.toString(numBands)
					);
			for (int i = 0; i < numBands; i++) {
				str = str.concat(";band"+(i+1)+"Level="+Short.toString(bandLevels[i]));
			}
			return str;
		}
	};
	public Equalizer.Settings getProperties()
	throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
		byte[] param = new byte[4 + mNumBands * 2];
		checkStatus(getParameter(PARAM_PROPERTIES, param));
		Settings settings = new Settings();
		settings.curPreset = byteArrayToShort(param, 0);
		settings.numBands = byteArrayToShort(param, 2);
		settings.bandLevels = new short[mNumBands];
		for (int i = 0; i < mNumBands; i++) {
			settings.bandLevels[i] = byteArrayToShort(param, 4 + 2*i);
		}
		return settings;
	}
	public void setProperties(Equalizer.Settings settings)
	throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
		if (settings.numBands != settings.bandLevels.length ||
			settings.numBands != mNumBands) {
			throw new IllegalArgumentException("settings invalid band count: " +settings.numBands);
		}
		byte[] param = concatArrays(shortToByteArray(settings.curPreset),
									shortToByteArray(mNumBands));
		for (int i = 0; i < mNumBands; i++) {
			param = concatArrays(param,
								 shortToByteArray(settings.bandLevels[i]));
		}
		checkStatus(setParameter(PARAM_PROPERTIES, param));
	}
}
