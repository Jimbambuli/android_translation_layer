package android.media;

/* AOSP frameworks/base/media/java/android/media/MediaSync.java (public API surface; no backing service in ATL, bodies follow the AOSP no-service path) */
public final class MediaSync {
    public MediaSync()  {}
    public android.view.Surface createInputSurface() { return null; }
    protected void finalize() {  }
    public void flush() {  }
    public android.media.PlaybackParams getPlaybackParams() { return null; }
    public android.media.SyncParams getSyncParams() { return null; }
    public android.media.MediaTimestamp getTimestamp() { return null; }
    public void queueAudio(java.nio.ByteBuffer arg0, int arg1, long arg2) {  }
    public void release() {  }
    public void setAudioTrack(android.media.AudioTrack arg0) {  }
    public void setCallback(android.media.MediaSync.Callback arg0, android.os.Handler arg1) {  }
    public void setOnErrorListener(android.media.MediaSync.OnErrorListener arg0, android.os.Handler arg1) {  }
    public void setPlaybackParams(android.media.PlaybackParams arg0) {  }
    public void setSurface(android.view.Surface arg0) {  }
    public void setSyncParams(android.media.SyncParams arg0) {  }
    public static final int MEDIASYNC_ERROR_AUDIOTRACK_FAIL = 1; // 0x1;
    public static final int MEDIASYNC_ERROR_SURFACE_FAIL = 2; // 0x2;

    public abstract static class Callback {
        public Callback()  {}
        public abstract void onAudioBufferConsumed(android.media.MediaSync arg0, java.nio.ByteBuffer arg1, int arg2);
    }

    public static interface OnErrorListener {
        void onError(android.media.MediaSync arg0, int arg1, int arg2);
    }
}
