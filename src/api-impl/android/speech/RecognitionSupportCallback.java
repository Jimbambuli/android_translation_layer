package android.speech;


/* AOSP core/java/android/speech/RecognitionSupportCallback.java */
public interface RecognitionSupportCallback {

	void onSupportResult(RecognitionSupport recognitionSupport);

	void onError(int error);
}
