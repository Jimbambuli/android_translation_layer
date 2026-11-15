
package android.media.audiofx;
import android.util.Log;
import java.util.StringTokenizer;
/* AOSP media/java/android/media/audiofx/BassBoost.java. ATL has no effect engines, so the constructor fails like
 * AudioEffect's and no instance exists; the members are AOSP's, ending in AudioEffect's checkState() answer for an
 * uninitialized effect. */
public class BassBoost extends AudioEffect {
	private final static String TAG = "BassBoost";
	public static final int PARAM_STRENGTH_SUPPORTED = 0;
	public static final int PARAM_STRENGTH = 1;
	private boolean mStrengthSupported = false;
	private OnParameterChangeListener mParamListener = null;
	private BaseParameterListener mBaseParamListener = null;
	private final Object mParamListenerLock = new Object();
	public BassBoost(int priority, int audioSession)
	throws IllegalStateException, IllegalArgumentException,
		   UnsupportedOperationException, RuntimeException {
		super(EFFECT_TYPE_BASS_BOOST, EFFECT_TYPE_NULL, priority, audioSession);
		if (audioSession == 0) {
			Log.w(TAG, "WARNING: attaching a BassBoost to global output mix is deprecated!");
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
	public interface OnParameterChangeListener  {
		void onParameterChange(BassBoost effect, int status, int param, short value);
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
					l.onParameterChange(BassBoost.this, status, p, v);
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
			if (!key.equals("BassBoost")) {
				throw new IllegalArgumentException(
						"invalid settings for BassBoost: " + key);
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
					"BassBoost"+
					";strength="+Short.toString(strength)
					);
			return str;
		}
	};
	public BassBoost.Settings getProperties()
	throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
		Settings settings = new Settings();
		short[] value = new short[1];
		checkStatus(getParameter(PARAM_STRENGTH, value));
		settings.strength = value[0];
		return settings;
	}
	public void setProperties(BassBoost.Settings settings)
	throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
		checkStatus(setParameter(PARAM_STRENGTH, settings.strength));
	}
}
