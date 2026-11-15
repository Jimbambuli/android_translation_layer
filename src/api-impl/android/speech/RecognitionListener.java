package android.speech;

import android.os.Bundle;

/* AOSP core/java/android/speech/RecognitionListener.java */
public interface RecognitionListener {
	void onReadyForSpeech(Bundle params);

	void onBeginningOfSpeech();

	void onRmsChanged(float rmsdB);

	void onBufferReceived(byte[] buffer);

	void onEndOfSpeech();

	void onError(int error);

	void onResults(Bundle results);

	void onPartialResults(Bundle partialResults);

	default void onSegmentResults(Bundle segmentResults) {}

	default void onEndOfSegmentedSession() {}

	default void onLanguageDetection(Bundle results) {}

	void onEvent(int eventType, Bundle params);
}
