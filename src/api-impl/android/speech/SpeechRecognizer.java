package android.speech;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import java.util.List;

/* AOSP SpeechRecognizer (core/java/android/speech/SpeechRecognizer.java) as on a device without a
 * recognition service: ServiceManager has no speech_recognition service, so connectToSystemService() ->
 * maybeInitializeManagerService() reports ERROR_CLIENT and the queued tasks (start/stop/cancel/listener
 * change) are never run. Not ported: the binder session (IRecognitionService), on-device recognition,
 * checkRecognitionSupport()/triggerModelDownload() (they need RecognitionSupport). */
public class SpeechRecognizer {
	private static final String TAG = "SpeechRecognizer";

	public static final String RESULTS_RECOGNITION = "results_recognition";
	public static final String CONFIDENCE_SCORES = "confidence_scores";
	public static final String RESULTS_ALTERNATIVES = "results_alternatives";
	public static final String RECOGNITION_PARTS = "recognition_parts";
	public static final String DETECTED_LANGUAGE = "detected_language";
	public static final String LANGUAGE_DETECTION_CONFIDENCE_LEVEL = "language_detection_confidence_level";
	public static final int LANGUAGE_DETECTION_CONFIDENCE_LEVEL_UNKNOWN = 0;
	public static final int LANGUAGE_DETECTION_CONFIDENCE_LEVEL_NOT_CONFIDENT = 1;
	public static final int LANGUAGE_DETECTION_CONFIDENCE_LEVEL_CONFIDENT = 2;
	public static final int LANGUAGE_DETECTION_CONFIDENCE_LEVEL_HIGHLY_CONFIDENT = 3;
	public static final String TOP_LOCALE_ALTERNATIVES = "top_locale_alternatives";
	public static final String LANGUAGE_SWITCH_RESULT = "language_switch_result";
	public static final int LANGUAGE_SWITCH_RESULT_NOT_ATTEMPTED = 0;
	public static final int LANGUAGE_SWITCH_RESULT_SUCCEEDED = 1;
	public static final int LANGUAGE_SWITCH_RESULT_FAILED = 2;
	public static final int LANGUAGE_SWITCH_RESULT_SKIPPED_NO_MODEL = 3;

	public static final int ERROR_NETWORK_TIMEOUT = 1;
	public static final int ERROR_NETWORK = 2;
	public static final int ERROR_AUDIO = 3;
	public static final int ERROR_SERVER = 4;
	public static final int ERROR_CLIENT = 5;
	public static final int ERROR_SPEECH_TIMEOUT = 6;
	public static final int ERROR_NO_MATCH = 7;
	public static final int ERROR_RECOGNIZER_BUSY = 8;
	public static final int ERROR_INSUFFICIENT_PERMISSIONS = 9;
	public static final int ERROR_TOO_MANY_REQUESTS = 10;
	public static final int ERROR_SERVER_DISCONNECTED = 11;
	public static final int ERROR_LANGUAGE_NOT_SUPPORTED = 12;
	public static final int ERROR_LANGUAGE_UNAVAILABLE = 13;
	public static final int ERROR_CANNOT_CHECK_SUPPORT = 14;
	public static final int ERROR_CANNOT_LISTEN_TO_DOWNLOAD_EVENTS = 15;

	private static final int MSG_ERROR = 4; // InternalRecognitionListener.MSG_ERROR

	/* AOSP queues setRecognitionListener()/startListening()/stopListening()/cancel() in mPendingTasks until
	 * the service connects (putMessage()); without a service that never happens, so they are no-ops here.
	 * Consequence as in AOSP: the listener is never set, so the ERROR_CLIENT reported by
	 * connectToSystemService() reaches nobody. */
	private final Handler mListenerHandler = new Handler(Looper.getMainLooper()) {
		@Override
		public void handleMessage(Message msg) {
			// InternalRecognitionListener's handler: nothing is delivered while no listener is set
			if (mInternalListener != null && msg.what == MSG_ERROR) {
				mInternalListener.onError((Integer)msg.obj);
			}
		}
	};
	private RecognitionListener mInternalListener; // set by handleChangeListener(), which never runs here

	private SpeechRecognizer() {}

	private void reportError(int error) {
		Message.obtain(mListenerHandler, MSG_ERROR, error).sendToTarget();
	}

	public static boolean isRecognitionAvailable(Context context) {
		List<ResolveInfo> list = context.getPackageManager().queryIntentServices(new Intent(RecognitionService.SERVICE_INTERFACE), 0);
		return list != null && list.size() != 0;
	}

	public static boolean isOnDeviceRecognitionAvailable(Context context) {
		// config_defaultOnDeviceSpeechRecognitionService is empty without a service
		return false;
	}

	public static SpeechRecognizer createSpeechRecognizer(Context context) {
		return createSpeechRecognizer(context, null);
	}

	public static SpeechRecognizer createSpeechRecognizer(Context context, ComponentName serviceComponent) {
		if (context == null) {
			throw new IllegalArgumentException("Context cannot be null");
		}
		checkIsCalledFromMainThread();
		return new SpeechRecognizer();
	}

	public static SpeechRecognizer createOnDeviceSpeechRecognizer(Context context) {
		if (!isOnDeviceRecognitionAvailable(context)) {
			throw new UnsupportedOperationException("On-device recognition is not available");
		}
		return new SpeechRecognizer();
	}

	public void setRecognitionListener(RecognitionListener listener) {
		checkIsCalledFromMainThread();
	}

	public void startListening(Intent recognizerIntent) {
		if (recognizerIntent == null) {
			throw new IllegalArgumentException("intent must not be null");
		}
		checkIsCalledFromMainThread();
		connectToSystemService();
	}

	public void stopListening() {
		checkIsCalledFromMainThread();
	}

	public void cancel() {
		checkIsCalledFromMainThread();
	}

	public void destroy() {
		mInternalListener = null;
	}

	/* AOSP connectToSystemService() -> maybeInitializeManagerService(): no manager service */
	private void connectToSystemService() {
		Log.e(TAG, "no speech recognition manager service");
		reportError(ERROR_CLIENT);
	}

	private static void checkIsCalledFromMainThread() {
		if (Looper.myLooper() != Looper.getMainLooper()) {
			throw new RuntimeException("SpeechRecognizer should be used only from the application's main thread");
		}
	}
}
