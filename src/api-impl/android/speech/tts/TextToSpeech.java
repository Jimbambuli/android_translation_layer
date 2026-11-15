package android.speech.tts;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

public class TextToSpeech {
	public static final int ERROR = -1;

	public TextToSpeech(Context context, TextToSpeech.OnInitListener listener) {
		new Handler(Looper.getMainLooper()).post(new Runnable() {
			@Override
			public void run() {
				listener.onInit(ERROR);
			}
		});
	}

	public int setOnUtteranceCompletedListener(TextToSpeech.OnUtteranceCompletedListener listener) {
		return ERROR;
	}

	public int setOnUtteranceProgressListener(UtteranceProgressListener listener) {
		return ERROR;
	}

	/* AOSP TextToSpeech.shutdown(): without a bound engine there is nothing to disconnect */
	public void shutdown() {
	}

	/* AOSP TextToSpeech results and queue modes */
	public static final int SUCCESS = 0;
	public static final int QUEUE_FLUSH = 0;
	public static final int QUEUE_ADD = 1;
	public static final int LANG_COUNTRY_VAR_AVAILABLE = 2;
	public static final int LANG_COUNTRY_AVAILABLE = 1;
	public static final int LANG_AVAILABLE = 0;
	public static final int LANG_MISSING_DATA = -1;
	public static final int LANG_NOT_SUPPORTED = -2;

	/* ATL has no TTS engine, so the constructor reports ERROR and TextToSpeech is never bound to one: the methods
	 * that need the engine return the error result of AOSP runAction() ("not bound to TTS engine") */
	private <R> R notBound(R errorResult, String method) {
		android.util.Log.w("TextToSpeech", method + " failed: not bound to TTS engine");
		return errorResult;
	}

	/* AOSP TextToSpeech.setLanguage() */
	public int setLanguage(final java.util.Locale loc) {
		return notBound(LANG_NOT_SUPPORTED, "setLanguage");
	}

	/* AOSP TextToSpeech.isLanguageAvailable() */
	public int isLanguageAvailable(final java.util.Locale loc) {
		return notBound(LANG_NOT_SUPPORTED, "isLanguageAvailable");
	}

	/* AOSP TextToSpeech.isSpeaking() */
	public boolean isSpeaking() {
		return notBound(false, "isSpeaking");
	}

	/* AOSP TextToSpeech.speak(CharSequence, int, Bundle, String) */
	public int speak(final CharSequence text, final int queueMode, final android.os.Bundle params, final String utteranceId) {
		return notBound(ERROR, "speak");
	}

	/* AOSP TextToSpeech.speak(String, int, HashMap) */
	@Deprecated
	public int speak(final String text, final int queueMode, final java.util.HashMap<String, String> params) {
		return speak(text, queueMode, (android.os.Bundle)null, params == null ? null : params.get("utteranceId"));
	}

	/* AOSP TextToSpeech.setSpeechRate(): kept in the request parameters (Engine.KEY_PARAM_RATE), needs no engine */
	private final android.os.Bundle mParams = new android.os.Bundle();

	public int setSpeechRate(float speechRate) {
		if (speechRate > 0.0f) {
			int intRate = (int)(speechRate * 100);
			if (intRate > 0) {
				synchronized (mParams) {
					mParams.putInt("rate", intRate);
				}
				return SUCCESS;
			}
		}
		return ERROR;
	}

	public int stop() {
		return ERROR;
	}

	public static interface OnInitListener {
		abstract void onInit(int status);
	}

	public static interface OnUtteranceCompletedListener {
		public abstract void onUtteranceCompleted(String utteranceId);
	}
}
