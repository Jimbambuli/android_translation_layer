package android.media;

/* AOSP frameworks/base/media/java/android/media/JetPlayer.java (public API surface; no backing service in ATL, bodies follow the AOSP no-service path) */
public class JetPlayer {
    public boolean clearQueue() { return false; }
    public Object clone() throws java.lang.CloneNotSupportedException { return null; }
    public boolean closeJetFile() { return false; }
    protected void finalize() {  }
    public static android.media.JetPlayer getJetPlayer() { return null; }
    public static int getMaxTracks() { return 0; }
    public boolean loadJetFile(String arg0) { return false; }
    public boolean loadJetFile(android.content.res.AssetFileDescriptor arg0) { return false; }
    public boolean pause() { return false; }
    public boolean play() { return false; }
    public boolean queueJetSegment(int arg0, int arg1, int arg2, int arg3, int arg4, byte arg5) { return false; }
    public boolean queueJetSegmentMuteArray(int arg0, int arg1, int arg2, int arg3, boolean[] arg4, byte arg5) { return false; }
    public void release() {  }
    public void setEventListener(android.media.JetPlayer.OnJetEventListener arg0) {  }
    public void setEventListener(android.media.JetPlayer.OnJetEventListener arg0, android.os.Handler arg1) {  }
    public boolean setMuteArray(boolean[] arg0, boolean arg1) { return false; }
    public boolean setMuteFlag(int arg0, boolean arg1, boolean arg2) { return false; }
    public boolean setMuteFlags(int arg0, boolean arg1) { return false; }
    public boolean triggerClip(int arg0) { return false; }

    public static interface OnJetEventListener {
        void onJetEvent(android.media.JetPlayer arg0, short arg1, byte arg2, byte arg3, byte arg4, byte arg5);
        void onJetNumQueuedSegmentUpdate(android.media.JetPlayer arg0, int arg1);
        void onJetPauseUpdate(android.media.JetPlayer arg0, int arg1);
        void onJetUserIdUpdate(android.media.JetPlayer arg0, int arg1, int arg2);
    }
}
