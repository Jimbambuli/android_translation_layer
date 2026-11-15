package android.media.tv;

/* AOSP media/java/android/android/media/tv/TvView.java (public API surface; no TV service in ATL, bodies follow the AOSP no-service path) */
public class TvView extends android.view.ViewGroup {
    private TvInputCallback mCallback;
    private TimeShiftPositionCallback mTimeShiftPositionCallback;
    private OnUnhandledInputEventListener mOnUnhandledInputEventListener;

    private void connectionFailed(final String inputId) {
        /* AOSP TvView.tune(): TvInputManager.createSession() yields no session without a TvInputManagerService;
         * TvView.MySessionCallback.onSessionCreated(null) then reports onConnectionFailed(inputId). */
        post(new Runnable() {
            @Override
            public void run() {
                if (mCallback != null) {
                    mCallback.onConnectionFailed(inputId);
                }
            }
        });
    }

    public TvView(android.content.Context arg0)  { super(arg0);}
    public TvView(android.content.Context arg0, android.util.AttributeSet arg1)  { super(arg0, arg1);}
    public TvView(android.content.Context arg0, android.util.AttributeSet arg1, int arg2)  { super(arg0, arg1, arg2);}
    public boolean dispatchUnhandledInputEvent(android.view.InputEvent arg0) {
        if (mOnUnhandledInputEventListener != null) {
            if (mOnUnhandledInputEventListener.onUnhandledInputEvent(arg0)) {
                return true;
            }
        }
        return onUnhandledInputEvent(arg0);
    }
    public java.util.List<android.media.AudioPresentation> getAudioPresentations() { return new java.util.ArrayList<android.media.AudioPresentation>(); }
    public String getSelectedTrack(int arg0) { return null; }
    public java.util.List<android.media.tv.TvTrackInfo> getTracks(int arg0) { return null; }
    protected void onLayout(boolean arg0, int arg1, int arg2, int arg3, int arg4) {  }
    public boolean onUnhandledInputEvent(android.view.InputEvent arg0) { return false; }
    public void overrideTvAppAttributionSource(android.content.AttributionSource arg0) {  }
    public void reset() {  }
    public void resumePlayback() {  }
    public void selectAudioPresentation(int arg0, int arg1) {  }
    public void selectTrack(int arg0, String arg1) {  }
    public void sendAppPrivateCommand(String arg0, android.os.Bundle arg1) {  }
    public void setCallback(android.media.tv.TvView.TvInputCallback arg0) { mCallback = arg0; }
    public void setCaptionEnabled(boolean arg0) {  }
    public void setInteractiveAppNotificationEnabled(boolean arg0) {  }
    public void setOnUnhandledInputEventListener(android.media.tv.TvView.OnUnhandledInputEventListener arg0) { mOnUnhandledInputEventListener = arg0; }
    public void setStreamVolume(float arg0) {  }
    public void setTimeShiftPositionCallback(android.media.tv.TvView.TimeShiftPositionCallback arg0) { mTimeShiftPositionCallback = arg0; }
    public void setTvMessageEnabled(int arg0, boolean arg1) {  }
    public void setVideoFrozen(boolean arg0) {  }
    public void setZOrderMediaOverlay(boolean arg0) {  }
    public void setZOrderOnTop(boolean arg0) {  }
    public void stopPlayback(int arg0) {  }
    public void timeShiftPause() {  }
    public void timeShiftPlay(String arg0, android.net.Uri arg1) {  }
    public void timeShiftResume() {  }
    public void timeShiftSeekTo(long arg0) {  }
    public void timeShiftSetMode(int arg0) {  }
    public void timeShiftSetPlaybackParams(android.media.PlaybackParams arg0) {  }
    public void tune(String arg0, android.net.Uri arg1) { tune(arg0, arg1, null); }
    public void tune(String arg0, android.net.Uri arg1, android.os.Bundle arg2) {
        if (arg0 == null) {
            throw new NullPointerException();
        }
        if (arg0.isEmpty()) {
            throw new IllegalArgumentException("inputId cannot be empty");
        }
        connectionFailed(arg0);
    }

    public static interface OnUnhandledInputEventListener {
        boolean onUnhandledInputEvent(android.view.InputEvent arg0);
    }

    public abstract static class TimeShiftPositionCallback {
        public TimeShiftPositionCallback()  {}
        public void onTimeShiftCurrentPositionChanged(String arg0, long arg1) {  }
        public void onTimeShiftStartPositionChanged(String arg0, long arg1) {  }
    }

    public abstract static class TvInputCallback {
        public TvInputCallback()  {}
        public void onAitInfoUpdated(String arg0, android.media.tv.AitInfo arg1) {  }
        public void onAudioPresentationSelected(String arg0, int arg1, int arg2) {  }
        public void onAudioPresentationsChanged(String arg0, java.util.List<android.media.AudioPresentation> arg1) {  }
        public void onAvailableSpeeds(String arg0, float[] arg1) {  }
        public void onChannelRetuned(String arg0, android.net.Uri arg1) {  }
        public void onConnectionFailed(String arg0) {  }
        public void onContentAllowed(String arg0) {  }
        public void onContentBlocked(String arg0, android.media.tv.TvContentRating arg1) {  }
        public void onCueingMessageAvailability(String arg0, boolean arg1) {  }
        public void onDisconnected(String arg0) {  }
        public void onSignalStrengthUpdated(String arg0, int arg1) {  }
        public void onTimeShiftMode(String arg0, int arg1) {  }
        public void onTimeShiftStatusChanged(String arg0, int arg1) {  }
        public void onTrackSelected(String arg0, int arg1, String arg2) {  }
        public void onTracksChanged(String arg0, java.util.List<android.media.tv.TvTrackInfo> arg1) {  }
        public void onTuned(String arg0, android.net.Uri arg1) {  }
        public void onTvMessage(String arg0, int arg1, android.os.Bundle arg2) {  }
        public void onVideoAvailable(String arg0) {  }
        public void onVideoFreezeUpdated(String arg0, boolean arg1) {  }
        public void onVideoSizeChanged(String arg0, int arg1, int arg2) {  }
        public void onVideoUnavailable(String arg0, int arg1) {  }
    }
}
