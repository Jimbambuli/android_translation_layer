package android.media.tv;

/* AOSP media/java/android/android/media/tv/TvInputService.java (public API surface; no TV service in ATL, bodies follow the AOSP no-service path) */
public abstract class TvInputService extends android.app.Service {
    public TvInputService()  {}
    public final android.os.IBinder onBind(android.content.Intent arg0) { return null; }
    public android.media.tv.TvInputService.RecordingSession onCreateRecordingSession(String arg0) { return null; }
    public android.media.tv.TvInputService.RecordingSession onCreateRecordingSession(String arg0, String arg1) { return onCreateRecordingSession(arg0); }
    public abstract android.media.tv.TvInputService.Session onCreateSession(String arg0);
    public android.media.tv.TvInputService.Session onCreateSession(String arg0, String arg1) { return onCreateSession(arg0); }
    public android.media.tv.TvInputService.Session onCreateSession(String arg0, String arg1, android.content.AttributionSource arg2) { return onCreateSession(arg0, arg1); }
    public static final int PRIORITY_HINT_USE_CASE_TYPE_BACKGROUND = 100; // 0x64;
    public static final int PRIORITY_HINT_USE_CASE_TYPE_LIVE = 400; // 0x190;
    public static final int PRIORITY_HINT_USE_CASE_TYPE_PLAYBACK = 300; // 0x12c;
    public static final int PRIORITY_HINT_USE_CASE_TYPE_RECORD = 500; // 0x1f4;
    public static final int PRIORITY_HINT_USE_CASE_TYPE_SCAN = 200; // 0xc8;
    public static final String SERVICE_INTERFACE = "android.media.tv.TvInputService";
    public static final String SERVICE_META_DATA = "android.media.tv.input";

    public abstract static class HardwareSession extends android.media.tv.TvInputService.Session {
        public HardwareSession(android.content.Context arg0)  { super(arg0);}
        public abstract String getHardwareInputId();
        public void onHardwareVideoAvailable() {  }
        public void onHardwareVideoUnavailable(int arg0) {  }
        public final boolean onSetSurface(android.view.Surface surface) {
            android.util.Log.e("TvInputService", "onSetSurface() should not be called in HardwareProxySession.");
            return false;
        }
    }

    public abstract static class RecordingSession {
        public RecordingSession(android.content.Context arg0)  {}
        public void notifyError(int arg0) {  }
        public void notifyRecordingStopped(android.net.Uri arg0) {  }
        public void notifyTuned(android.net.Uri arg0) {  }
        public void onAppPrivateCommand(String arg0, android.os.Bundle arg1) {  }
        public void onPauseRecording(android.os.Bundle arg0) {  }
        public abstract void onRelease();
        public void onResumeRecording(android.os.Bundle arg0) {  }
        public abstract void onStartRecording(android.net.Uri arg0);
        public void onStartRecording(android.net.Uri programUri, android.os.Bundle params) { onStartRecording(programUri); }
        public abstract void onStopRecording();
        public abstract void onTune(android.net.Uri arg0);
        public void onTune(android.net.Uri channelUri, android.os.Bundle params) { onTune(channelUri); }
    }

    public abstract static class Session implements android.view.KeyEvent.Callback {
        public Session(android.content.Context arg0)  {}
        public void layoutSurface(int left, int top, int right, int bottom) {
            if (left > right || top > bottom) {
                throw new IllegalArgumentException("Invalid parameter");
            }
        }
        public void notifyAdBufferConsumed(android.media.tv.AdBuffer arg0) {  }
        public void notifyAdResponse(android.media.tv.AdResponse arg0) {  }
        public void notifyAitInfoUpdated(android.media.tv.AitInfo arg0) {  }
        public void notifyAudioPresentationChanged(java.util.List<android.media.AudioPresentation> arg0) {  }
        public void notifyAudioPresentationSelected(int arg0, int arg1) {  }
        public void notifyAvailableSpeeds(float[] arg0) {  }
        public void notifyBroadcastInfoResponse(android.media.tv.BroadcastInfoResponse arg0) {  }
        public void notifyChannelRetuned(android.net.Uri arg0) {  }
        public void notifyContentAllowed() {  }
        public void notifyContentBlocked(android.media.tv.TvContentRating rating) {
            java.util.Objects.requireNonNull(rating);
        }
        public void notifyCueingMessageAvailability(boolean arg0) {  }
        public void notifySignalStrength(int arg0) {  }
        public void notifyTimeShiftMode(int arg0) {  }
        public void notifyTimeShiftStatusChanged(int arg0) {  }
        public void notifyTrackSelected(int arg0, String arg1) {  }
        public void notifyTracksChanged(java.util.List<android.media.tv.TvTrackInfo> arg0) {  }
        public void notifyTuned(android.net.Uri arg0) {  }
        public void notifyTvMessage(int arg0, android.os.Bundle arg1) {  }
        public void notifyVideoAvailable() {  }
        public void notifyVideoFreezeUpdated(boolean arg0) {  }
        public void notifyVideoUnavailable(int arg0) {  }
        public void onAdBufferReady(android.media.tv.AdBuffer arg0) {  }
        public void onAppPrivateCommand(String arg0, android.os.Bundle arg1) {  }
        public android.view.View onCreateOverlayView() { return null; }
        public boolean onGenericMotionEvent(android.view.MotionEvent arg0) { return false; }
        public boolean onKeyDown(int arg0, android.view.KeyEvent arg1) { return false; }
        public boolean onKeyLongPress(int arg0, android.view.KeyEvent arg1) { return false; }
        public boolean onKeyMultiple(int arg0, int arg1, android.view.KeyEvent arg2) { return false; }
        public boolean onKeyUp(int arg0, android.view.KeyEvent arg1) { return false; }
        public void onOverlayViewSizeChanged(int arg0, int arg1) {  }
        public abstract void onRelease();
        public void onRemoveBroadcastInfo(int arg0) {  }
        public void onRequestAd(android.media.tv.AdRequest arg0) {  }
        public void onRequestBroadcastInfo(android.media.tv.BroadcastInfoRequest arg0) {  }
        public void onResumePlayback() {  }
        public boolean onSelectAudioPresentation(int arg0, int arg1) { return false; }
        public boolean onSelectTrack(int arg0, String arg1) { return false; }
        public abstract void onSetCaptionEnabled(boolean arg0);
        public void onSetInteractiveAppNotificationEnabled(boolean arg0) {  }
        public abstract void onSetStreamVolume(float arg0);
        public abstract boolean onSetSurface(android.view.Surface arg0);
        public void onSetTvMessageEnabled(int arg0, boolean arg1) {  }
        public void onStopPlayback(int arg0) {  }
        public void onSurfaceChanged(int arg0, int arg1, int arg2) {  }
        public long onTimeShiftGetCurrentPosition() { return Long.MIN_VALUE; }
        public long onTimeShiftGetStartPosition() { return Long.MIN_VALUE; }
        public void onTimeShiftPause() {  }
        public void onTimeShiftPlay(android.net.Uri arg0) {  }
        public void onTimeShiftResume() {  }
        public void onTimeShiftSeekTo(long arg0) {  }
        public void onTimeShiftSetMode(int arg0) {  }
        public void onTimeShiftSetPlaybackParams(android.media.PlaybackParams arg0) {  }
        public boolean onTouchEvent(android.view.MotionEvent arg0) { return false; }
        public boolean onTrackballEvent(android.view.MotionEvent arg0) { return false; }
        public abstract boolean onTune(android.net.Uri arg0);
        public boolean onTune(android.net.Uri arg0, android.os.Bundle arg1) { return onTune(arg0); }
        public void onTvAdSessionData(String arg0, android.os.Bundle arg1) {  }
        public void onTvMessage(int arg0, android.os.Bundle arg1) {  }
        public void onUnblockContent(android.media.tv.TvContentRating arg0) {  }
        public void sendTvInputSessionData(String arg0, android.os.Bundle arg1) {  }
        public void setOverlayViewEnabled(boolean arg0) {  }
    }
}
