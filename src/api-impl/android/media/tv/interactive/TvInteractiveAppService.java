package android.media.tv.interactive;

/* AOSP frameworks/base/media/java/android/media/tv/interactive/TvInteractiveAppService.java (public API surface; no backing service in ATL, bodies follow the AOSP no-service path) */
public abstract class TvInteractiveAppService extends android.app.Service {
    public TvInteractiveAppService()  {}
    public final void notifyStateChanged(int arg0, int arg1, int arg2) {  }
    public void onAppLinkCommand(android.os.Bundle arg0) {  }
    public final android.os.IBinder onBind(android.content.Intent arg0) { return null; }
    public abstract android.media.tv.interactive.TvInteractiveAppService.Session onCreateSession(String arg0, int arg1);
    public void onRegisterAppLinkInfo(android.media.tv.interactive.AppLinkInfo arg0) {  }
    public void onUnregisterAppLinkInfo(android.media.tv.interactive.AppLinkInfo arg0) {  }
    public static final String COMMAND_PARAMETER_KEY_CHANGE_CHANNEL_QUIETLY = "command_change_channel_quietly";
    public static final String COMMAND_PARAMETER_KEY_CHANNEL_URI = "command_channel_uri";
    public static final String COMMAND_PARAMETER_KEY_INPUT_ID = "command_input_id";
    public static final String COMMAND_PARAMETER_KEY_PLAYBACK_PARAMS = "command_playback_params";
    public static final String COMMAND_PARAMETER_KEY_PROGRAM_URI = "command_program_uri";
    public static final String COMMAND_PARAMETER_KEY_STOP_MODE = "command_stop_mode";
    public static final String COMMAND_PARAMETER_KEY_TIME_POSITION = "command_time_position";
    public static final String COMMAND_PARAMETER_KEY_TIME_SHIFT_MODE = "command_time_shift_mode";
    public static final String COMMAND_PARAMETER_KEY_TRACK_ID = "command_track_id";
    public static final String COMMAND_PARAMETER_KEY_TRACK_TYPE = "command_track_type";
    public static final String COMMAND_PARAMETER_KEY_VOLUME = "command_volume";
    public static final int COMMAND_PARAMETER_VALUE_STOP_MODE_BLANK = 1; // 0x1;
    public static final int COMMAND_PARAMETER_VALUE_STOP_MODE_FREEZE = 2; // 0x2;
    public static final String PLAYBACK_COMMAND_TYPE_SELECT_TRACK = "select_track";
    public static final String PLAYBACK_COMMAND_TYPE_SET_STREAM_VOLUME = "set_stream_volume";
    public static final String PLAYBACK_COMMAND_TYPE_STOP = "stop";
    public static final String PLAYBACK_COMMAND_TYPE_TUNE = "tune";
    public static final String PLAYBACK_COMMAND_TYPE_TUNE_NEXT = "tune_next";
    public static final String PLAYBACK_COMMAND_TYPE_TUNE_PREV = "tune_previous";
    public static final String SERVICE_INTERFACE = "android.media.tv.interactive.TvInteractiveAppService";
    public static final String SERVICE_META_DATA = "android.media.tv.interactive.app";
    public static final String TIME_SHIFT_COMMAND_TYPE_PAUSE = "pause";
    public static final String TIME_SHIFT_COMMAND_TYPE_PLAY = "play";
    public static final String TIME_SHIFT_COMMAND_TYPE_RESUME = "resume";
    public static final String TIME_SHIFT_COMMAND_TYPE_SEEK_TO = "seek_to";
    public static final String TIME_SHIFT_COMMAND_TYPE_SET_MODE = "set_mode";
    public static final String TIME_SHIFT_COMMAND_TYPE_SET_PLAYBACK_PARAMS = "set_playback_params";

    public abstract static class Session implements android.view.KeyEvent.Callback {
        public Session(android.content.Context arg0)  {}
        public boolean isMediaViewEnabled() { return false; }
        public void layoutSurface(int arg0, int arg1, int arg2, int arg3) {  }
        public void notifyAdBufferReady(android.media.tv.AdBuffer arg0) {  }
        public final void notifyBiInteractiveAppCreated(android.net.Uri arg0, String arg1) {  }
        public void notifySessionStateChanged(int arg0, int arg1) {  }
        public final void notifyTeletextAppStateChanged(int arg0) {  }
        public void onAdBufferConsumed(android.media.tv.AdBuffer arg0) {  }
        public void onAdResponse(android.media.tv.AdResponse arg0) {  }
        public void onAvailableSpeeds(float[] arg0) {  }
        public void onBroadcastInfoResponse(android.media.tv.BroadcastInfoResponse arg0) {  }
        public void onCertificate(String arg0, int arg1, android.net.http.SslCertificate arg2) {  }
        public void onContentAllowed() {  }
        public void onContentBlocked(android.media.tv.TvContentRating arg0) {  }
        public void onCreateBiInteractiveAppRequest(android.net.Uri arg0, android.os.Bundle arg1) {  }
        public android.view.View onCreateMediaView() { return null; }
        public void onCurrentChannelLcn(int arg0) {  }
        public void onCurrentChannelUri(android.net.Uri arg0) {  }
        public void onCurrentTvInputId(String arg0) {  }
        public void onCurrentVideoBounds(android.graphics.Rect arg0) {  }
        public void onDestroyBiInteractiveAppRequest(String arg0) {  }
        public void onError(String arg0, android.os.Bundle arg1) {  }
        public boolean onGenericMotionEvent(android.view.MotionEvent arg0) { return false; }
        public boolean onKeyDown(int arg0, android.view.KeyEvent arg1) { return false; }
        public boolean onKeyLongPress(int arg0, android.view.KeyEvent arg1) { return false; }
        public boolean onKeyMultiple(int arg0, int arg1, android.view.KeyEvent arg2) { return false; }
        public boolean onKeyUp(int arg0, android.view.KeyEvent arg1) { return false; }
        public void onMediaViewSizeChanged(int arg0, int arg1) {  }
        public void onRecordingConnectionFailed(String arg0, String arg1) {  }
        public void onRecordingDisconnected(String arg0, String arg1) {  }
        public void onRecordingError(String arg0, int arg1) {  }
        public void onRecordingScheduled(String arg0, String arg1) {  }
        public void onRecordingStarted(String arg0, String arg1) {  }
        public void onRecordingStopped(String arg0) {  }
        public void onRecordingTuned(String arg0, android.net.Uri arg1) {  }
        public abstract void onRelease();
        public void onResetInteractiveApp() {  }
        public void onSelectedTrackInfo(java.util.List<android.media.tv.TvTrackInfo> arg0) {  }
        public abstract boolean onSetSurface(android.view.Surface arg0);
        public void onSetTeletextAppEnabled(boolean arg0) {  }
        public void onSignalStrength(int arg0) {  }
        public void onSigningResult(String arg0, byte[] arg1) {  }
        public void onStartInteractiveApp() {  }
        public void onStopInteractiveApp() {  }
        public void onStreamVolume(float arg0) {  }
        public void onSurfaceChanged(int arg0, int arg1, int arg2) {  }
        public void onTimeShiftCurrentPositionChanged(String arg0, long arg1) {  }
        public void onTimeShiftMode(int arg0) {  }
        public void onTimeShiftPlaybackParams(android.media.PlaybackParams arg0) {  }
        public void onTimeShiftStartPositionChanged(String arg0, long arg1) {  }
        public void onTimeShiftStatusChanged(String arg0, int arg1) {  }
        public boolean onTouchEvent(android.view.MotionEvent arg0) { return false; }
        public void onTrackInfoList(java.util.List<android.media.tv.TvTrackInfo> arg0) {  }
        public void onTrackSelected(int arg0, String arg1) {  }
        public boolean onTrackballEvent(android.view.MotionEvent arg0) { return false; }
        public void onTracksChanged(java.util.List<android.media.tv.TvTrackInfo> arg0) {  }
        public void onTuned(android.net.Uri arg0) {  }
        public void onTvMessage(int arg0, android.os.Bundle arg1) {  }
        public void onTvRecordingInfo(android.media.tv.TvRecordingInfo arg0) {  }
        public void onTvRecordingInfoList(java.util.List<android.media.tv.TvRecordingInfo> arg0) {  }
        public void onVideoAvailable() {  }
        public void onVideoFreezeUpdated(boolean arg0) {  }
        public void onVideoUnavailable(int arg0) {  }
        public void removeBroadcastInfo(int arg0) {  }
        public void requestAd(android.media.tv.AdRequest arg0) {  }
        public void requestAvailableSpeeds() {  }
        public void requestBroadcastInfo(android.media.tv.BroadcastInfoRequest arg0) {  }
        public void requestCertificate(String arg0, int arg1) {  }
        public void requestCurrentChannelLcn() {  }
        public void requestCurrentChannelUri() {  }
        public void requestCurrentTvInputId() {  }
        public void requestCurrentVideoBounds() {  }
        public void requestScheduleRecording(String arg0, String arg1, android.net.Uri arg2, android.net.Uri arg3, android.os.Bundle arg4) {  }
        public void requestScheduleRecording(String arg0, String arg1, android.net.Uri arg2, long arg3, long arg4, int arg5, android.os.Bundle arg6) {  }
        public void requestSelectedTrackInfo() {  }
        public void requestSigning(String arg0, String arg1, String arg2, byte[] arg3) {  }
        public void requestSigning(String arg0, String arg1, String arg2, int arg3, byte[] arg4) {  }
        public void requestStartRecording(String arg0, android.net.Uri arg1) {  }
        public void requestStopRecording(String arg0) {  }
        public void requestStreamVolume() {  }
        public void requestTimeShiftMode() {  }
        public void requestTrackInfoList() {  }
        public void requestTvRecordingInfo(String arg0) {  }
        public void requestTvRecordingInfoList(int arg0) {  }
        public void sendPlaybackCommandRequest(String arg0, android.os.Bundle arg1) {  }
        public void sendTimeShiftCommandRequest(String arg0, android.os.Bundle arg1) {  }
        public void setMediaViewEnabled(boolean arg0) {  }
        public void setTvRecordingInfo(String arg0, android.media.tv.TvRecordingInfo arg1) {  }
        public void setVideoBounds(android.graphics.Rect arg0) {  }
    }
}
