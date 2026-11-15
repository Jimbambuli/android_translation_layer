package android.media.audiofx;

/* AOSP media/java/android/media/audiofx/Visualizer.java. ATL has no effect engines, so native_setup() finds no
 * visualizer effect and the constructor throws as AOSP's does for a failed setup (error ERROR_BAD_VALUE, the
 * code AudioEffect reports for an unknown type); no instance exists. */
public class Visualizer {
	public static final int STATE_UNINITIALIZED = 0;
	public static final int STATE_INITIALIZED = 1;
	public static final int STATE_ENABLED = 2;

	public static final int SCALING_MODE_NORMALIZED = 0;
	public static final int SCALING_MODE_AS_PLAYED = 1;

	public static final int MEASUREMENT_MODE_NONE = 0;
	public static final int MEASUREMENT_MODE_PEAK_RMS = 1 << 0;

	public static final int SUCCESS = 0;
	public static final int ERROR = -1;
	public static final int ALREADY_EXISTS = -2;
	public static final int ERROR_NO_INIT = -3;
	public static final int ERROR_BAD_VALUE = -4;
	public static final int ERROR_INVALID_OPERATION = -5;
	public static final int ERROR_NO_MEMORY = -6;
	public static final int ERROR_DEAD_OBJECT = -7;

	/* AOSP Visualizer(int): result != SUCCESS and != ALREADY_EXISTS ends in the default branch of the switch */
	public Visualizer(int audioSession) throws UnsupportedOperationException, RuntimeException {
		throw new RuntimeException("Cannot initialize Visualizer engine, error: " + ERROR_BAD_VALUE);
	}

	/* AOSP getCaptureSizeRange() is native (effect library constants VISUALIZER_CAPTURE_SIZE_MIN/MAX). The
	 * native source is not part of the checked-out AOSP tree; the values are those of
	 * hardware/libhardware/include/.../effect_visualizer.h as remembered (128..1024), unverified here */
	public static int[] getCaptureSizeRange() {
		return new int[] {128, 1024};
	}

	/* AOSP getMaxCaptureRate() is native (android_media_Visualizer.cpp CAPTURE_RATE_MAX, milliHertz); value
	 * 20000 as remembered, not in the checked-out tree, unverified here */
	public static int getMaxCaptureRate() {
		return 20000;
	}

	/* AOSP release(): frees the native engine; none exists */
	public void release() {}

	/* AOSP setEnabled(): state is STATE_UNINITIALIZED */
	public int setEnabled(boolean enabled) throws IllegalStateException {
		throw new IllegalStateException("setEnabled() called in wrong state: " + STATE_UNINITIALIZED);
	}

	/* AOSP setCaptureSize(): state is STATE_UNINITIALIZED */
	public int setCaptureSize(int size) throws IllegalStateException {
		throw new IllegalStateException("setCaptureSize() called in wrong state: " + STATE_UNINITIALIZED);
	}

	/* AOSP setDataCaptureListener(): native_setPeriodicCapture() on an uninitialized effect fails with ERROR_NO_INIT */
	public int setDataCaptureListener(OnDataCaptureListener listener, int rate, boolean waveform, boolean fft) {
		return ERROR_NO_INIT;
	}

	public interface OnDataCaptureListener {
		void onWaveFormDataCapture(Visualizer visualizer, byte[] waveform, int samplingRate);

		void onFftDataCapture(Visualizer visualizer, byte[] fft, int samplingRate);
	}
}
