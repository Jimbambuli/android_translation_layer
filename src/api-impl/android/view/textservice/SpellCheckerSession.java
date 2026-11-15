package android.view.textservice;

import java.util.concurrent.Executor;

/**
 * AOSP frameworks/base core/java/android/view/textservice/SpellCheckerSession.java, reduced.
 *
 * A session with a spell checker service. TextServicesManager.newSpellCheckerSession()
 * returns null in ATL (no spell checker service), so apps never get one. A session that
 * is never connected to a service, as here, queues requests and delivers no results
 * (AOSP SpellCheckerSessionListenerImpl before onServiceConnected()).
 */
public class SpellCheckerSession {

	public static final String SERVICE_META_DATA = "android.view.textservice.scs";

	private final SpellCheckerInfo mSpellCheckerInfo;
	private final TextServicesManager mTextServicesManager;
	private final SpellCheckerSessionListener mSpellCheckerSessionListener;
	private final Executor mExecutor;

	/* AOSP SpellCheckerSession(SpellCheckerInfo, TextServicesManager, SpellCheckerSessionListener, Executor) */
	public SpellCheckerSession(SpellCheckerInfo info, TextServicesManager tsm,
	                           SpellCheckerSessionListener listener, Executor executor) {
		if (info == null || listener == null || tsm == null) {
			throw new NullPointerException();
		}
		mSpellCheckerInfo = info;
		mTextServicesManager = tsm;
		mSpellCheckerSessionListener = listener;
		mExecutor = executor;
	}

	/* AOSP SpellCheckerSessionListenerImpl.isDisconnected(): mState != STATE_CONNECTED;
	 * never connected here */
	public boolean isSessionDisconnected() {
		return true;
	}

	public SpellCheckerInfo getSpellChecker() {
		return mSpellCheckerInfo;
	}

	public void cancel() {
	}

	public void close() {
	}

	public void getSentenceSuggestions(TextInfo[] textInfos, int suggestionsLimit) {
	}

	@Deprecated
	public void getSuggestions(TextInfo textInfo, int suggestionsLimit) {
		getSuggestions(new TextInfo[] {textInfo}, suggestionsLimit, false);
	}

	@Deprecated
	public void getSuggestions(TextInfo[] textInfos, int suggestionsLimit, boolean sequentialWords) {
	}

	/* AOSP SpellCheckerSession.SpellCheckerSessionListener */
	public interface SpellCheckerSessionListener {
		public void onGetSuggestions(SuggestionsInfo[] results);

		public void onGetSentenceSuggestions(SentenceSuggestionsInfo[] results);
	}
}
