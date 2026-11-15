package android.media;

/* AOSP frameworks/base/media/java/android/media/MediaMuxer.java (public API surface; no backing service in ATL, bodies follow the AOSP no-service path) */
public final class MediaMuxer {
    public MediaMuxer(String arg0, int arg1) throws java.io.IOException {}
    public MediaMuxer(java.io.FileDescriptor arg0, int arg1) throws java.io.IOException {}
    public int addTrack(android.media.MediaFormat arg0) { return 0; }
    public void release() {  }
    public void setLocation(float arg0, float arg1) {  }
    public void setOrientationHint(int arg0) {  }
    public void start() {  }
    public void stop() {  }
    public void writeSampleData(int arg0, java.nio.ByteBuffer arg1, android.media.MediaCodec.BufferInfo arg2) {  }

    public static final class OutputFormat {
        public static final int MUXER_OUTPUT_3GPP = 2; // 0x2;
        public static final int MUXER_OUTPUT_HEIF = 3; // 0x3;
        public static final int MUXER_OUTPUT_MPEG_4 = 0; // 0x0;
        public static final int MUXER_OUTPUT_OGG = 4; // 0x4;
        public static final int MUXER_OUTPUT_WEBM = 1; // 0x1;
    }
}
