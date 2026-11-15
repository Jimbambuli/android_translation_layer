package android.media.tv.ad;

/* AOSP frameworks/base/media/java/android/media/tv/ad/TvAdView.java (public API surface; no backing service in ATL, bodies follow the AOSP no-service path) */
public class TvAdView extends android.view.ViewGroup {
    public TvAdView(android.content.Context arg0)  { super(arg0);}
    public TvAdView(android.content.Context arg0, android.util.AttributeSet arg1)  { super(arg0, arg1);}
    public TvAdView(android.content.Context arg0, android.util.AttributeSet arg1, int arg2)  { super(arg0, arg1, arg2);}
    public void clearCallback() {  }
    public void clearOnUnhandledInputEventListener() {  }
    public boolean dispatchUnhandledInputEvent(android.view.InputEvent arg0) { return false; }
    public android.media.tv.ad.TvAdView.OnUnhandledInputEventListener getOnUnhandledInputEventListener() { return null; }
    public void notifyError(String arg0, android.os.Bundle arg1) {  }
    public void notifyTvMessage(int arg0, android.os.Bundle arg1) {  }
    public void onAttachedToWindow() {  }
    public void onDetachedFromWindow() {  }
    public void onLayout(boolean arg0, int arg1, int arg2, int arg3, int arg4) {  }
    public void onMeasure(int arg0, int arg1) {  }
    public boolean onUnhandledInputEvent(android.view.InputEvent arg0) { return false; }
    public void onVisibilityChanged(android.view.View arg0, int arg1) {  }
    public void prepareAdService(String arg0, String arg1) {  }
    public void reset() {  }
    public void resetAdService() {  }
    public void sendCurrentChannelUri(android.net.Uri arg0) {  }
    public void sendCurrentTvInputId(String arg0) {  }
    public void sendCurrentVideoBounds(android.graphics.Rect arg0) {  }
    public void sendSigningResult(String arg0, byte[] arg1) {  }
    public void sendTrackInfoList(java.util.List<android.media.tv.TvTrackInfo> arg0) {  }
    public void setCallback(java.util.concurrent.Executor arg0, android.media.tv.ad.TvAdView.TvAdCallback arg1) {  }
    public void setOnUnhandledInputEventListener(android.media.tv.ad.TvAdView.OnUnhandledInputEventListener arg0) {  }
    public boolean setTvView(android.media.tv.TvView arg0) { return false; }
    public void setZOrderMediaOverlay(boolean arg0) {  }
    public void setZOrderOnTop(boolean arg0) {  }
    public void startAdService() {  }
    public void stopAdService() {  }
    public static final String ERROR_KEY_ERROR_CODE = "error_code";
    public static final String ERROR_KEY_METHOD_NAME = "method_name";

    public static interface OnUnhandledInputEventListener {
        boolean onUnhandledInputEvent(android.view.InputEvent arg0);
    }

    public abstract static class TvAdCallback {
        public TvAdCallback()  {}
        public void onRequestCurrentChannelUri(String arg0) {  }
        public void onRequestCurrentTvInputId(String arg0) {  }
        public void onRequestCurrentVideoBounds(String arg0) {  }
        public void onRequestSigning(String arg0, String arg1, String arg2, String arg3, byte[] arg4) {  }
        public void onRequestTrackInfoList(String arg0) {  }
        public void onStateChanged(String arg0, int arg1, int arg2) {  }
    }
}
