package android.media.projection;

/* AOSP frameworks/base/media/java/android/media/projection/AppContentProjectionService.java (public API surface; no backing service in ATL, bodies follow the AOSP no-service path) */
public abstract class AppContentProjectionService extends android.app.Service {
    public AppContentProjectionService()  {}
    public final android.os.IBinder onBind(android.content.Intent arg0) { return null; }
    public abstract void onContentRequest(android.media.projection.AppContentRequest arg0);
    public abstract void onContentRequestCanceled();
    public abstract boolean onLoopbackProjectionStarted(android.media.projection.AppContentProjectionSession arg0, int arg1);
    public abstract void onSessionStopped(android.media.projection.AppContentProjectionSession arg0);
    public static final String SERVICE_INTERFACE = "android.media.projection.AppContentProjectionService";
}
