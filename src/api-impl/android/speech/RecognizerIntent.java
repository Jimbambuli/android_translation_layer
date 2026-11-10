package android.speech;

/* AOSP RecognizerIntent: the intent actions and extras of speech recognition (the ones SearchView's voice
 * search uses; SearchView offers it only if an activity resolves the intent) */
public class RecognizerIntent {
	public static final String ACTION_RECOGNIZE_SPEECH = "android.speech.action.RECOGNIZE_SPEECH";
	public static final String ACTION_WEB_SEARCH = "android.speech.action.WEB_SEARCH";
	public static final String EXTRA_CALLING_PACKAGE = "calling_package";
	public static final String EXTRA_LANGUAGE_MODEL = "android.speech.extra.LANGUAGE_MODEL";
	public static final String LANGUAGE_MODEL_FREE_FORM = "free_form";
	public static final String LANGUAGE_MODEL_WEB_SEARCH = "web_search";
	public static final String EXTRA_PROMPT = "android.speech.extra.PROMPT";
	public static final String EXTRA_LANGUAGE = "android.speech.extra.LANGUAGE";
	public static final String EXTRA_MAX_RESULTS = "android.speech.extra.MAX_RESULTS";
	public static final String EXTRA_RESULTS_PENDINGINTENT = "android.speech.extra.RESULTS_PENDINGINTENT";
	public static final String EXTRA_RESULTS_PENDINGINTENT_BUNDLE = "android.speech.extra.RESULTS_PENDINGINTENT_BUNDLE";
}
