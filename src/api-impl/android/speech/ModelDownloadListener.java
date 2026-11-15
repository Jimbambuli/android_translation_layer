package android.speech;

/* AOSP android.speech.ModelDownloadListener */
public interface ModelDownloadListener {
	void onProgress(int completedPercent);

	void onSuccess();

	void onScheduled();

	void onError(int error);
}
