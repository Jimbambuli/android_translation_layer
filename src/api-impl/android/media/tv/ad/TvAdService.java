package android.media.tv.ad;

/* AOSP frameworks/base/media/java/android/media/tv/ad/TvAdService.java (public API surface; no backing service in ATL, bodies follow the AOSP no-service path) */
public abstract class TvAdService extends android.app.Service {
    public TvAdService()  {}
    public void onAppLinkCommand(android.os.Bundle arg0) {  }
    public final android.os.IBinder onBind(android.content.Intent arg0) { return null; }
    public abstract android.media.tv.ad.TvAdService.Session onCreateSession(String arg0, String arg1);
    public static final String SERVICE_INTERFACE = "android.media.tv.ad.TvAdService";
    public static final String SERVICE_META_DATA = "android.media.tv.ad.service";

    public abstract static class Session implements android.view.KeyEvent.Callback {
        public Session(android.content.Context arg0)  {}
        public boolean isMediaViewEnabled() { return false; }
        public void layoutSurface(int arg0, int arg1, int arg2, int arg3) {  }
        public void notifySessionStateChanged(int arg0, int arg1) {  }
        public android.view.View onCreateMediaView() { return null; }
        public void onCurrentChannelUri(android.net.Uri arg0) {  }
        public void onCurrentTvInputId(String arg0) {  }
        public void onCurrentVideoBounds(android.graphics.Rect arg0) {  }
        public void onError(String arg0, android.os.Bundle arg1) {  }
        public boolean onGenericMotionEvent(android.view.MotionEvent arg0) { return false; }
        public boolean onKeyDown(int arg0, android.view.KeyEvent arg1) { return false; }
        public boolean onKeyLongPress(int arg0, android.view.KeyEvent arg1) { return false; }
        public boolean onKeyMultiple(int arg0, int arg1, android.view.KeyEvent arg2) { return false; }
        public boolean onKeyUp(int arg0, android.view.KeyEvent arg1) { return false; }
        public void onMediaViewSizeChanged(int arg0, int arg1) {  }
        public abstract void onRelease();
        public void onResetAdService() {  }
        public abstract boolean onSetSurface(android.view.Surface arg0);
        public void onSigningResult(String arg0, byte[] arg1) {  }
        public void onStartAdService() {  }
        public void onStopAdService() {  }
        public void onSurfaceChanged(int arg0, int arg1, int arg2) {  }
        public boolean onTouchEvent(android.view.MotionEvent arg0) { return false; }
        public void onTrackInfoList(java.util.List<android.media.tv.TvTrackInfo> arg0) {  }
        public boolean onTrackballEvent(android.view.MotionEvent arg0) { return false; }
        public void onTvInputSessionData(String arg0, android.os.Bundle arg1) {  }
        public void onTvMessage(int arg0, android.os.Bundle arg1) {  }
        public void requestCurrentChannelUri() {  }
        public void requestCurrentTvInputId() {  }
        public void requestCurrentVideoBounds() {  }
        public void requestSigning(String arg0, String arg1, String arg2, byte[] arg3) {  }
        public void requestTrackInfoList() {  }
        public void sendTvAdSessionData(String arg0, android.os.Bundle arg1) {  }
        public void setMediaViewEnabled(boolean arg0) {  }
    }
}
