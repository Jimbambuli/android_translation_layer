package android.media;

/* AOSP frameworks/base/media/java/android/media/MediaController2.java (public API surface; no backing service in ATL, bodies follow the AOSP no-service path) */
public class MediaController2 implements java.lang.AutoCloseable {
    @Deprecated public void cancelSessionCommand(Object arg0) {  }
    @Deprecated public void close() {  }
    @Deprecated public android.media.Session2Token getConnectedToken() { return null; }
    @Deprecated public boolean isPlaybackActive() { return false; }
    @Deprecated public Object sendSessionCommand(android.media.Session2Command arg0, android.os.Bundle arg1) { return null; }

    public static final class Builder {
        @Deprecated public Builder(android.content.Context arg0, android.media.Session2Token arg1)  {}
        @Deprecated public android.media.MediaController2 build() { return null; }
        @Deprecated public android.media.MediaController2.Builder setConnectionHints(android.os.Bundle arg0) { return this; }
        @Deprecated public android.media.MediaController2.Builder setControllerCallback(java.util.concurrent.Executor arg0, android.media.MediaController2.ControllerCallback arg1) { return this; }
    }

    public abstract static class ControllerCallback {
        @Deprecated public ControllerCallback()  {}
        @Deprecated public void onCommandResult(android.media.MediaController2 arg0, Object arg1, android.media.Session2Command arg2, android.media.Session2Command.Result arg3) {  }
        @Deprecated public void onConnected(android.media.MediaController2 arg0, android.media.Session2CommandGroup arg1) {  }
        @Deprecated public void onDisconnected(android.media.MediaController2 arg0) {  }
        @Deprecated public void onPlaybackActiveChanged(android.media.MediaController2 arg0, boolean arg1) {  }
        @Deprecated public android.media.Session2Command.Result onSessionCommand(android.media.MediaController2 arg0, android.media.Session2Command arg1, android.os.Bundle arg2) { return null; }
    }
}
