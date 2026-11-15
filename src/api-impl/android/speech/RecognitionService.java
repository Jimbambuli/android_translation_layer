package android.speech;

import android.app.Service;
import android.content.AttributionSource;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

/* AOSP frameworks/base core/java/android/speech/RecognitionService.java (Android 16): extends Service.
 * The public API and default bodies are taken over. ATL has no recognition system service and no binder
 * IPC to SpeechRecognizer clients (SpeechRecognizer.isRecognitionAvailable() finds no service), so like a
 * device without a recognizer nobody binds to it: onBind() returns no IRecognitionService binder, and
 * Callback/SupportCallback wrap the in-process listener interfaces instead of IRecognitionListener and
 * IRecognitionSupportCallback. */
public abstract class RecognitionService extends Service {
	public static final String SERVICE_INTERFACE = "android.speech.RecognitionService";
	public static final String SERVICE_META_DATA = "android.speech";

	private static final int DEFAULT_MAX_CONCURRENT_SESSIONS_COUNT = 1;

	public RecognitionService() {
	}

	protected abstract void onStartListening(Intent recognizerIntent, Callback listener);

	protected abstract void onCancel(Callback listener);

	protected abstract void onStopListening(Callback listener);

	public void onCheckRecognitionSupport(Intent recognizerIntent, SupportCallback supportCallback) {
		supportCallback.onError(SpeechRecognizer.ERROR_CANNOT_CHECK_SUPPORT);
	}

	public void onCheckRecognitionSupport(Intent recognizerIntent, AttributionSource attributionSource,
	                                      SupportCallback supportCallback) {
		onCheckRecognitionSupport(recognizerIntent, supportCallback);
	}

	public void onTriggerModelDownload(Intent recognizerIntent) {
	}

	public void onTriggerModelDownload(Intent recognizerIntent, AttributionSource attributionSource) {
		onTriggerModelDownload(recognizerIntent);
	}

	public void onTriggerModelDownload(Intent recognizerIntent, AttributionSource attributionSource,
	                                   ModelDownloadListener listener) {
		listener.onError(SpeechRecognizer.ERROR_CANNOT_LISTEN_TO_DOWNLOAD_EVENTS);
	}

	/* AOSP returns its RecognitionServiceBinder; ATL has no IRecognitionService to hand out */
	@Override
	public final IBinder onBind(final Intent intent) {
		return null;
	}

	public int getMaxConcurrentSessionsCount() {
		return DEFAULT_MAX_CONCURRENT_SESSIONS_COUNT;
	}

	public class Callback {
		private final RecognitionListener mListener;
		private final AttributionSource mCallingAttributionSource;

		private Callback(RecognitionListener listener, AttributionSource attributionSource) {
			mListener = listener;
			mCallingAttributionSource = attributionSource;
		}

		public void beginningOfSpeech() throws RemoteException {
			mListener.onBeginningOfSpeech();
		}

		public void bufferReceived(byte[] buffer) throws RemoteException {
			mListener.onBufferReceived(buffer);
		}

		public void endOfSpeech() throws RemoteException {
			mListener.onEndOfSpeech();
		}

		public void error(int error) throws RemoteException {
			mListener.onError(error);
		}

		public void partialResults(Bundle partialResults) throws RemoteException {
			mListener.onPartialResults(partialResults);
		}

		public void readyForSpeech(Bundle params) throws RemoteException {
			mListener.onReadyForSpeech(params);
		}

		public void results(Bundle results) throws RemoteException {
			mListener.onResults(results);
		}

		public void rmsChanged(float rmsdB) throws RemoteException {
			mListener.onRmsChanged(rmsdB);
		}

		public void segmentResults(Bundle results) throws RemoteException {
			mListener.onSegmentResults(results);
		}

		public void endOfSegmentedSession() throws RemoteException {
			mListener.onEndOfSegmentedSession();
		}

		public void languageDetection(Bundle results) {
			mListener.onLanguageDetection(results);
		}

		public int getCallingUid() {
			return mCallingAttributionSource.getUid();
		}

		public AttributionSource getCallingAttributionSource() {
			return mCallingAttributionSource;
		}
	}

	public static class SupportCallback {
		private final RecognitionSupportCallback mCallback;

		private SupportCallback(RecognitionSupportCallback callback) {
			this.mCallback = callback;
		}

		public void onSupportResult(RecognitionSupport recognitionSupport) {
			mCallback.onSupportResult(recognitionSupport);
		}

		public void onError(int errorCode) {
			mCallback.onError(errorCode);
		}
	}
}
