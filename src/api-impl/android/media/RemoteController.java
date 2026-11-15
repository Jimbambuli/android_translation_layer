package android.media;

/* AOSP frameworks/base/media/java/android/media/RemoteController.java (public API surface; no backing service in ATL, bodies follow the AOSP no-service path) */
public final class RemoteController {
    @Deprecated public RemoteController(android.content.Context arg0, android.media.RemoteController.OnClientUpdateListener arg1) throws java.lang.IllegalArgumentException {}
    @Deprecated public RemoteController(android.content.Context arg0, android.media.RemoteController.OnClientUpdateListener arg1, android.os.Looper arg2) throws java.lang.IllegalArgumentException {}
    @Deprecated public boolean clearArtworkConfiguration() { return false; }
    @Deprecated public android.media.RemoteController.MetadataEditor editMetadata() { return null; }
    @Deprecated public long getEstimatedMediaPosition() { return 0L; }
    @Deprecated public boolean seekTo(long arg0) throws java.lang.IllegalArgumentException { return false; }
    @Deprecated public boolean sendMediaKeyEvent(android.view.KeyEvent arg0) throws java.lang.IllegalArgumentException { return false; }
    @Deprecated public boolean setArtworkConfiguration(int arg0, int arg1) throws java.lang.IllegalArgumentException { return false; }
    @Deprecated public boolean setSynchronizationMode(int arg0) throws java.lang.IllegalArgumentException { return false; }
    @Deprecated public static final int POSITION_SYNCHRONIZATION_CHECK = 1; // 0x1;
    @Deprecated public static final int POSITION_SYNCHRONIZATION_NONE = 0; // 0x0;

    public class MetadataEditor extends android.media.MediaMetadataEditor {
        @Deprecated public void apply() {  }
    }

    public static interface OnClientUpdateListener {
        void onClientChange(boolean arg0);
        void onClientMetadataUpdate(android.media.RemoteController.MetadataEditor arg0);
        void onClientPlaybackStateUpdate(int arg0);
        void onClientPlaybackStateUpdate(int arg0, long arg1, long arg2, float arg3);
        void onClientTransportControlUpdate(int arg0);
    }
}
