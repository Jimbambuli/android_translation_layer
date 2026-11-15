package android.media;

/* AOSP frameworks/base/media/java/android/media/MediaSession2Service.java (public API surface; no backing service in ATL, bodies follow the AOSP no-service path) */
public abstract class MediaSession2Service extends android.app.Service {
    @Deprecated public MediaSession2Service()  {}
    @Deprecated public final void addSession(android.media.MediaSession2 arg0) {  }
    @Deprecated public final java.util.List<android.media.MediaSession2> getSessions() { return new java.util.ArrayList<>(); }
    @Deprecated public android.os.IBinder onBind(android.content.Intent arg0) { return null; }
    @Deprecated public abstract android.media.MediaSession2 onGetSession(android.media.MediaSession2.ControllerInfo arg0);
    @Deprecated public abstract android.media.MediaSession2Service.MediaNotification onUpdateNotification(android.media.MediaSession2 arg0);
    @Deprecated public final void removeSession(android.media.MediaSession2 arg0) {  }
    @Deprecated public static final String SERVICE_INTERFACE = "android.media.MediaSession2Service";

    public static class MediaNotification {
        @Deprecated public MediaNotification(int arg0, android.app.Notification arg1)  {}
        @Deprecated public android.app.Notification getNotification() { return null; }
        @Deprecated public int getNotificationId() { return 0; }
    }
}
