package android.media.audiofx;

/* AOSP media/java/android/media/audiofx/PresetReverb.java. ATL has no effect engines, so the constructor
 * fails like AudioEffect's and no instance exists; the members are AOSP's checkState() answer for an
 * uninitialized effect. */
public class PresetReverb extends AudioEffect {
	public static final int PARAM_PRESET = 0;
	public static final short PRESET_NONE = 0;
	public static final short PRESET_SMALLROOM = 1;
	public static final short PRESET_MEDIUMROOM = 2;
	public static final short PRESET_LARGEROOM = 3;
	public static final short PRESET_MEDIUMHALL = 4;
	public static final short PRESET_LARGEHALL = 5;
	public static final short PRESET_PLATE = 6;

	public PresetReverb(int priority, int audioSession) {
		super(EFFECT_TYPE_PRESET_REVERB, EFFECT_TYPE_NULL, priority, audioSession);
	}

	public void setPreset(short preset) {
		throw new IllegalStateException("setParameter() called on uninitialized AudioEffect.");
	}

	public short getPreset() {
		throw new IllegalStateException("getParameter() called on uninitialized AudioEffect.");
	}
}
