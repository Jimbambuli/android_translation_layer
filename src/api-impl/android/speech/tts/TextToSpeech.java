package android.speech.tts;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

public class TextToSpeech {
	public static final int ERROR = -1;

	private final Context mContext;
	private final java.util.Map<String, android.net.Uri> mEarcons = new java.util.HashMap<>();

	public TextToSpeech(Context context, TextToSpeech.OnInitListener listener) {
		mContext = context;
		new Handler(Looper.getMainLooper()).post(new Runnable() {
			@Override
			public void run() {
				listener.onInit(ERROR);
			}
		});
	}

	/* AOSP TextToSpeech(Context, OnInitListener, String engine): the engine package is looked up, which fails as
	 * without a name, as ATL has no engine installed */
	public TextToSpeech(Context context, TextToSpeech.OnInitListener listener, String engine) {
		this(context, listener);
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

	/* AOSP setPitch(): kept in the request parameters (Engine.KEY_PARAM_PITCH), needs no engine */
	public int setPitch(float pitch) {
		if (pitch > 0.0f) {
			int intPitch = (int)(pitch * 100);
			if (intPitch > 0) {
				synchronized (mParams) {
					mParams.putInt("pitch", intPitch);
				}
				return SUCCESS;
			}
		}
		return ERROR;
	}

	/* AOSP setAudioAttributes(): kept in the request parameters (Engine.KEY_PARAM_AUDIO_ATTRIBUTES) */
	public int setAudioAttributes(android.media.AudioAttributes audioAttributes) {
		if (audioAttributes != null) {
			synchronized (mParams) {
				mParams.putParcelable("audioAttributes", audioAttributes);
			}
			return SUCCESS;
		}
		return ERROR;
	}

	/* AOSP addEarcon(): the earcon map is kept locally, needs no engine */
	public int addEarcon(String earcon, String packagename, int resourceId) {
		return addEarcon(earcon, new android.net.Uri.Builder().scheme("android.resource")
		                             .encodedAuthority(packagename).appendEncodedPath(String.valueOf(resourceId)).build());
	}

	public int addEarcon(String earcon, String filename) {
		return addEarcon(earcon, android.net.Uri.parse(filename));
	}

	public int addEarcon(String earcon, java.io.File file) {
		return addEarcon(earcon, android.net.Uri.fromFile(file));
	}

	public int addEarcon(String earcon, android.net.Uri uri) {
		synchronized (mEarcons) {
			mEarcons.put(earcon, uri);
			return SUCCESS;
		}
	}

	/* AOSP: the methods below run on the engine (runAction()) and give its error result unbound */
	public int playEarcon(final String earcon, final int queueMode, final android.os.Bundle params, final String utteranceId) {
		return notBound(ERROR, "playEarcon");
	}

	@Deprecated
	public int playEarcon(final String earcon, final int queueMode, final java.util.HashMap<String, String> params) {
		return notBound(ERROR, "playEarcon");
	}

	public int playSilentUtterance(final long durationInMs, final int queueMode, final String utteranceId) {
		return notBound(ERROR, "playSilentUtterance");
	}

	@Deprecated
	public int playSilence(final long durationInMs, final int queueMode, final java.util.HashMap<String, String> params) {
		return notBound(ERROR, "playSilentUtterance");
	}

	public int synthesizeToFile(final CharSequence text, final android.os.Bundle params, final java.io.File file, final String utteranceId) {
		return notBound(ERROR, "synthesizeToFile");
	}

	public int synthesizeToFile(final CharSequence text, final android.os.Bundle params, final android.os.ParcelFileDescriptor fileDescriptor, final String utteranceId) {
		return notBound(ERROR, "synthesizeToFile");
	}

	@Deprecated
	public int synthesizeToFile(final String text, final java.util.HashMap<String, String> params, final String filename) {
		return notBound(ERROR, "synthesizeToFile");
	}

	public java.util.Set<java.util.Locale> getAvailableLanguages() {
		return notBound(null, "getAvailableLanguages");
	}

	public java.util.Set<Voice> getVoices() {
		return notBound(null, "getVoices");
	}

	public int setVoice(final Voice voice) {
		return notBound(LANG_NOT_SUPPORTED, "setVoice");
	}

	public Voice getVoice() {
		return notBound(null, "getVoice");
	}

	@Deprecated
	public java.util.Locale getLanguage() {
		return notBound(null, "getLanguage");
	}

	@Deprecated
	public java.util.Locale getDefaultLanguage() {
		return notBound(null, "getDefaultLanguage");
	}

	@Deprecated
	public java.util.Set<String> getFeatures(final java.util.Locale locale) {
		return notBound(null, "getFeatures");
	}

	/* AOSP getMaxSpeechInputLength() */
	public static int getMaxSpeechInputLength() {
		return 4000;
	}

	/* AOSP TtsEngines.getEngines(): the installed services for Engine.INTENT_ACTION_TTS_SERVICE */
	public java.util.List<EngineInfo> getEngines() {
		android.content.pm.PackageManager pm = mContext.getPackageManager();
		java.util.List<android.content.pm.ResolveInfo> resolveInfos = pm.queryIntentServices(
		    new android.content.Intent("android.intent.action.TTS_SERVICE"), android.content.pm.PackageManager.MATCH_DEFAULT_ONLY);
		if (resolveInfos == null)
			return java.util.Collections.emptyList();
		java.util.List<EngineInfo> engines = new java.util.ArrayList<EngineInfo>(resolveInfos.size());
		for (android.content.pm.ResolveInfo resolveInfo : resolveInfos) {
			android.content.pm.ServiceInfo service = resolveInfo.serviceInfo;
			if (service == null)
				continue;
			EngineInfo engine = new EngineInfo();
			engine.name = service.packageName;
			CharSequence label = service.loadLabel(pm);
			engine.label = label == null ? engine.name : label.toString();
			engine.icon = service.getIconResource();
			engine.system = service.applicationInfo != null
			    && (service.applicationInfo.flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0;
			engine.priority = engine.system ? Integer.MAX_VALUE : 0;
			engines.add(engine);
		}
		return engines;
	}

	/* AOSP TtsEngines.getDefaultEngine(): the TTS_DEFAULT_SYNTH setting (unset in ATL), else the highest
	 * ranked engine if it is a system engine */
	public String getDefaultEngine() {
		for (EngineInfo engine : getEngines()) {
			if (engine.system)
				return engine.name;
		}
		return null;
	}

	/* AOSP TextToSpeech.EngineInfo */
	public static class EngineInfo {
		public String name;
		public String label;
		public int icon;
		public boolean system;
		public int priority;

		@Override
		public String toString() {
			return "EngineInfo{name=" + name + "}";
		}
	}

	public static interface OnInitListener {
		abstract void onInit(int status);
	}

	public static interface OnUtteranceCompletedListener {
		public abstract void onUtteranceCompleted(String utteranceId);
	}
}
