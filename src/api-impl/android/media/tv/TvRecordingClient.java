package android.media.tv;

/* AOSP media/java/android/android/media/tv/TvRecordingClient.java (public API surface; no TV service in ATL, bodies follow the AOSP no-service path) */
public class TvRecordingClient {
    private final RecordingCallback mCallback;
    private final android.os.Handler mHandler;

    public TvRecordingClient(android.content.Context arg0, String arg1, android.media.tv.TvRecordingClient.RecordingCallback arg2, android.os.Handler arg3)  {
        mCallback = arg2;
        mHandler = arg3 == null ? new android.os.Handler(android.os.Looper.getMainLooper()) : arg3;
    }

    /* AOSP TvRecordingClient.tune(): TvInputManager.createRecordingSession() yields no session without a
     * TvInputManagerService, so the callback gets onConnectionFailed(inputId). */
    private void connectionFailed(final String inputId) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mCallback != null) {
                    mCallback.onConnectionFailed(inputId);
                }
            }
        });
    }
    public void pauseRecording() {  }
    public void pauseRecording(android.os.Bundle arg0) {  }
    public void release() {  }
    public void resumeRecording() {  }
    public void resumeRecording(android.os.Bundle arg0) {  }
    public void sendAppPrivateCommand(String arg0, android.os.Bundle arg1) {  }
    public void setTvInteractiveAppView(android.media.tv.interactive.TvInteractiveAppView arg0, String arg1) {  }
    public void startRecording(android.net.Uri arg0) {  }
    public void startRecording(android.net.Uri arg0, android.os.Bundle arg1) {  }
    public void stopRecording() {  }
    public void tune(String arg0, android.net.Uri arg1) { tune(arg0, arg1, null); }
    public void tune(String arg0, android.net.Uri arg1, android.os.Bundle arg2) {
        if (arg0 == null) {
            throw new NullPointerException();
        }
        if (arg0.isEmpty()) {
            throw new IllegalArgumentException("inputId cannot be empty");
        }
        connectionFailed(arg0);
    }

    public abstract static class RecordingCallback {
        public RecordingCallback()  {}
        public void onConnectionFailed(String arg0) {  }
        public void onDisconnected(String arg0) {  }
        public void onError(int arg0) {  }
        public void onRecordingStopped(android.net.Uri arg0) {  }
        public void onTuned(android.net.Uri arg0) {  }
    }
}
