package android.speech;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Looper;
import java.util.List;

/* AOSP core/java/android/speech/SpeechRecognizer.java on a device without a speech recognition service.
 * AOSP queues every command (putMessage()) until the connection to the service exists; without the
 * system's SPEECH_RECOGNITION_SERVICE the connection never comes, so the listener change and the commands
 * stay queued and the listener hears nothing (the ERROR_CLIENT of maybeInitializeManagerService() goes
 * to the internal listener, which is still unset). */
public class SpeechRecognizer {
	public static final String RESULTS_RECOGNITION = "results_recognition";
	public static final String CONFIDENCE_SCORES = "confidence_scores";

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

	private final Context mContext;
	private final ComponentName mServiceComponent;

	private SpeechRecognizer(Context context, ComponentName serviceComponent) {
		mContext = context;
		mServiceComponent = serviceComponent;
	}

	/* AOSP: a service for RecognitionService.SERVICE_INTERFACE must be installed */
	public static boolean isRecognitionAvailable(Context context) {
		List<ResolveInfo> list = context.getPackageManager().queryIntentServices(
		    new Intent("android.speech.RecognitionService"), 0);
		return list != null && list.size() != 0;
	}

	public static SpeechRecognizer createSpeechRecognizer(final Context context) {
		return createSpeechRecognizer(context, null);
	}

	public static SpeechRecognizer createSpeechRecognizer(final Context context, final ComponentName serviceComponent) {
		if (context == null)
			throw new IllegalArgumentException("Context cannot be null");
		checkIsCalledFromMainThread();
		return new SpeechRecognizer(context, serviceComponent);
	}

	private static void checkIsCalledFromMainThread() {
		if (Looper.myLooper() != Looper.getMainLooper())
			throw new RuntimeException("SpeechRecognizer should be used only from the application's main thread");
	}

	/* queued until a service connection exists, i.e. never */
	public void setRecognitionListener(RecognitionListener listener) {
		checkIsCalledFromMainThread();
	}

	public void startListening(final Intent recognizerIntent) {
		if (recognizerIntent == null)
			throw new IllegalArgumentException("intent must not be null");
		checkIsCalledFromMainThread();
	}

	public void stopListening() {
		checkIsCalledFromMainThread();
	}

	public void cancel() {
		checkIsCalledFromMainThread();
	}

	public void destroy() {
	}

	/* AOSP isOnDeviceRecognitionAvailable(): config_defaultOnDeviceSpeechRecognitionService names a component;
	 * it is empty in AOSP's config.xml (and ATL's framework-res), so unflattenFromString() gives null */
	public static boolean isOnDeviceRecognitionAvailable(final Context context) {
		ComponentName componentName = ComponentName.unflattenFromString(
		    context.getString(com.android.internal.R.string.config_defaultOnDeviceSpeechRecognitionService));
		return componentName != null;
	}

	public static SpeechRecognizer createOnDeviceSpeechRecognizer(final Context context) {
		if (!isOnDeviceRecognitionAvailable(context)) {
			throw new UnsupportedOperationException("On-device recognition is not available");
		}
		return createSpeechRecognizer(context);
	}

	/* AOSP checkRecognitionSupport(): queued (putMessage()) until the service connection exists, i.e. never */
	public void checkRecognitionSupport(Intent recognizerIntent, java.util.concurrent.Executor executor,
	                                    RecognitionSupportCallback supportListener) {
		java.util.Objects.requireNonNull(recognizerIntent, "intent must not be null");
		java.util.Objects.requireNonNull(supportListener, "listener must not be null");
	}
}
