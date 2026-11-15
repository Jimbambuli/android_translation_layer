package android.media.tv.interactive;

/* AOSP frameworks/base/media/java/android/media/tv/interactive/TvInteractiveAppView.java (public API surface; no backing service in ATL, bodies follow the AOSP no-service path) */
public class TvInteractiveAppView extends android.view.ViewGroup {
    public TvInteractiveAppView(android.content.Context arg0)  { super(arg0);}
    public TvInteractiveAppView(android.content.Context arg0, android.util.AttributeSet arg1)  { super(arg0, arg1);}
    public TvInteractiveAppView(android.content.Context arg0, android.util.AttributeSet arg1, int arg2)  { super(arg0, arg1, arg2);}
    public void clearCallback() {  }
    public void clearOnUnhandledInputEventListener() {  }
    public void createBiInteractiveApp(android.net.Uri arg0, android.os.Bundle arg1) {  }
    public void destroyBiInteractiveApp(String arg0) {  }
    public boolean dispatchUnhandledInputEvent(android.view.InputEvent arg0) { return false; }
    public android.media.tv.interactive.TvInteractiveAppView.OnUnhandledInputEventListener getOnUnhandledInputEventListener() { return null; }
    public void notifyError(String arg0, android.os.Bundle arg1) {  }
    public void notifyRecordingScheduled(String arg0, String arg1) {  }
    public void notifyRecordingStarted(String arg0, String arg1) {  }
    public void notifyRecordingStopped(String arg0) {  }
    public void notifyTimeShiftCurrentPositionChanged(String arg0, long arg1) {  }
    public void notifyTimeShiftPlaybackParams(android.media.PlaybackParams arg0) {  }
    public void notifyTimeShiftStartPositionChanged(String arg0, long arg1) {  }
    public void notifyTimeShiftStatusChanged(String arg0, int arg1) {  }
    public void notifyTvMessage(int arg0, android.os.Bundle arg1) {  }
    public void notifyVideoFreezeUpdated(boolean arg0) {  }
    public void onAttachedToWindow() {  }
    public void onDetachedFromWindow() {  }
    public void onLayout(boolean arg0, int arg1, int arg2, int arg3, int arg4) {  }
    public void onMeasure(int arg0, int arg1) {  }
    public boolean onUnhandledInputEvent(android.view.InputEvent arg0) { return false; }
    public void onVisibilityChanged(android.view.View arg0, int arg1) {  }
    public void prepareInteractiveApp(String arg0, int arg1) {  }
    public void reset() {  }
    public void resetInteractiveApp() {  }
    public void sendAvailableSpeeds(float[] arg0) {  }
    public void sendCertificate(String arg0, int arg1, android.net.http.SslCertificate arg2) {  }
    public void sendCurrentChannelLcn(int arg0) {  }
    public void sendCurrentChannelUri(android.net.Uri arg0) {  }
    public void sendCurrentTvInputId(String arg0) {  }
    public void sendCurrentVideoBounds(android.graphics.Rect arg0) {  }
    public void sendSelectedTrackInfo(java.util.List<android.media.tv.TvTrackInfo> arg0) {  }
    public void sendSigningResult(String arg0, byte[] arg1) {  }
    public void sendStreamVolume(float arg0) {  }
    public void sendTimeShiftMode(int arg0) {  }
    public void sendTrackInfoList(java.util.List<android.media.tv.TvTrackInfo> arg0) {  }
    public void sendTvRecordingInfo(android.media.tv.TvRecordingInfo arg0) {  }
    public void sendTvRecordingInfoList(java.util.List<android.media.tv.TvRecordingInfo> arg0) {  }
    public void setCallback(java.util.concurrent.Executor arg0, android.media.tv.interactive.TvInteractiveAppView.TvInteractiveAppCallback arg1) {  }
    public void setOnUnhandledInputEventListener(java.util.concurrent.Executor arg0, android.media.tv.interactive.TvInteractiveAppView.OnUnhandledInputEventListener arg1) {  }
    public void setTeletextAppEnabled(boolean arg0) {  }
    public int setTvView(android.media.tv.TvView arg0) { return 0; }
    public void setZOrderMediaOverlay(boolean arg0) {  }
    public void setZOrderOnTop(boolean arg0) {  }
    public void startInteractiveApp() {  }
    public void stopInteractiveApp() {  }
    public static final String BI_INTERACTIVE_APP_KEY_ALIAS = "alias";
    public static final String BI_INTERACTIVE_APP_KEY_CERTIFICATE = "certificate";
    public static final String BI_INTERACTIVE_APP_KEY_HTTP_ADDITIONAL_HEADERS = "http_additional_headers";
    public static final String BI_INTERACTIVE_APP_KEY_HTTP_USER_AGENT = "http_user_agent";
    public static final String BI_INTERACTIVE_APP_KEY_PRIVATE_KEY = "private_key";
    public static final String ERROR_KEY_METHOD_NAME = "method_name";

    public static interface OnUnhandledInputEventListener {
        boolean onUnhandledInputEvent(android.view.InputEvent arg0);
    }

    public abstract static class TvInteractiveAppCallback {
        public TvInteractiveAppCallback()  {}
        public void onBiInteractiveAppCreated(String arg0, android.net.Uri arg1, String arg2) {  }
        public void onPlaybackCommandRequest(String arg0, String arg1, android.os.Bundle arg2) {  }
        public void onRequestAvailableSpeeds(String arg0) {  }
        public void onRequestCertificate(String arg0, String arg1, int arg2) {  }
        public void onRequestCurrentChannelLcn(String arg0) {  }
        public void onRequestCurrentChannelUri(String arg0) {  }
        public void onRequestCurrentTvInputId(String arg0) {  }
        public void onRequestCurrentVideoBounds(String arg0) {  }
        public void onRequestScheduleRecording(String arg0, String arg1, String arg2, android.net.Uri arg3, android.net.Uri arg4, android.os.Bundle arg5) {  }
        public void onRequestScheduleRecording(String arg0, String arg1, String arg2, android.net.Uri arg3, long arg4, long arg5, int arg6, android.os.Bundle arg7) {  }
        public void onRequestSelectedTrackInfo(String arg0) {  }
        public void onRequestSigning(String arg0, String arg1, String arg2, String arg3, byte[] arg4) {  }
        public void onRequestSigning(String arg0, String arg1, String arg2, String arg3, int arg4, byte[] arg5) {  }
        public void onRequestStartRecording(String arg0, String arg1, android.net.Uri arg2) {  }
        public void onRequestStopRecording(String arg0, String arg1) {  }
        public void onRequestStreamVolume(String arg0) {  }
        public void onRequestTimeShiftMode(String arg0) {  }
        public void onRequestTrackInfoList(String arg0) {  }
        public void onRequestTvRecordingInfo(String arg0, String arg1) {  }
        public void onRequestTvRecordingInfoList(String arg0, int arg1) {  }
        public void onSetTvRecordingInfo(String arg0, String arg1, android.media.tv.TvRecordingInfo arg2) {  }
        public void onSetVideoBounds(String arg0, android.graphics.Rect arg1) {  }
        public void onStateChanged(String arg0, int arg1, int arg2) {  }
        public void onTeletextAppStateChanged(String arg0, int arg1) {  }
        public void onTimeShiftCommandRequest(String arg0, String arg1, android.os.Bundle arg2) {  }
    }
}
