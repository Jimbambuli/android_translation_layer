package android.media;

/* AOSP frameworks/base/media/java/android/media/MediaSession2.java (public API surface; no backing service in ATL, bodies follow the AOSP no-service path) */
public class MediaSession2 implements java.lang.AutoCloseable {
    @Deprecated public void broadcastSessionCommand(android.media.Session2Command arg0, android.os.Bundle arg1) {  }
    @Deprecated public void cancelSessionCommand(android.media.MediaSession2.ControllerInfo arg0, Object arg1) {  }
    @Deprecated public void close() {  }
    @Deprecated public java.util.List<android.media.MediaSession2.ControllerInfo> getConnectedControllers() { return null; }
    @Deprecated public String getId() { return null; }
    @Deprecated public android.media.Session2Token getToken() { return null; }
    @Deprecated public boolean isPlaybackActive() { return false; }
    @Deprecated public Object sendSessionCommand(android.media.MediaSession2.ControllerInfo arg0, android.media.Session2Command arg1, android.os.Bundle arg2) { return null; }
    @Deprecated public void setPlaybackActive(boolean arg0) {  }

    public static final class Builder {
        @Deprecated public Builder(android.content.Context arg0)  {}
        @Deprecated public android.media.MediaSession2 build() { return null; }
        @Deprecated public android.media.MediaSession2.Builder setExtras(android.os.Bundle arg0) { return this; }
        @Deprecated public android.media.MediaSession2.Builder setId(String arg0) { return this; }
        @Deprecated public android.media.MediaSession2.Builder setSessionActivity(android.app.PendingIntent arg0) { return this; }
        @Deprecated public android.media.MediaSession2.Builder setSessionCallback(java.util.concurrent.Executor arg0, android.media.MediaSession2.SessionCallback arg1) { return this; }
    }

    public static final class ControllerInfo {
        @Deprecated public android.os.Bundle getConnectionHints() { return null; }
        @Deprecated public String getPackageName() { return null; }
        @Deprecated public android.media.session.MediaSessionManager.RemoteUserInfo getRemoteUserInfo() { return null; }
        @Deprecated public int getUid() { return 0; }
    }

    public abstract static class SessionCallback {
        @Deprecated public SessionCallback()  {}
        @Deprecated public void onCommandResult(android.media.MediaSession2 arg0, android.media.MediaSession2.ControllerInfo arg1, Object arg2, android.media.Session2Command arg3, android.media.Session2Command.Result arg4) {  }
        @Deprecated public android.media.Session2CommandGroup onConnect(android.media.MediaSession2 arg0, android.media.MediaSession2.ControllerInfo arg1) { return null; }
        @Deprecated public void onDisconnected(android.media.MediaSession2 arg0, android.media.MediaSession2.ControllerInfo arg1) {  }
        @Deprecated public void onPostConnect(android.media.MediaSession2 arg0, android.media.MediaSession2.ControllerInfo arg1) {  }
        @Deprecated public android.media.Session2Command.Result onSessionCommand(android.media.MediaSession2 arg0, android.media.MediaSession2.ControllerInfo arg1, android.media.Session2Command arg2, android.os.Bundle arg3) { return null; }
    }
}
