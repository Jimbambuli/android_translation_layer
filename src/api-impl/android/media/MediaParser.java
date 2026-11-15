package android.media;

/* AOSP frameworks/base/media/java/android/media/MediaParser.java (public API surface; no backing service in ATL, bodies follow the AOSP no-service path) */
public final class MediaParser {
    public boolean advance(android.media.MediaParser.SeekableInputReader arg0) throws java.io.IOException { return false; }
    public static android.media.MediaParser create(android.media.MediaParser.OutputConsumer arg0, java.lang.String... arg1) { return null; }
    public static android.media.MediaParser createByName(String arg0, android.media.MediaParser.OutputConsumer arg1) { return null; }
    public android.media.metrics.LogSessionId getLogSessionId() { return null; }
    public String getParserName() { return null; }
    public static java.util.List<java.lang.String> getParserNames(android.media.MediaFormat arg0) { return null; }
    public void release() {  }
    public void seek(android.media.MediaParser.SeekPoint arg0) {  }
    public void setLogSessionId(android.media.metrics.LogSessionId arg0) {  }
    public android.media.MediaParser setParameter(String arg0, Object arg1) { return null; }
    public boolean supportsParameter(String arg0) { return false; }
    public static final String PARAMETER_ADTS_ENABLE_CBR_SEEKING = "android.media.mediaparser.adts.enableCbrSeeking";
    public static final String PARAMETER_AMR_ENABLE_CBR_SEEKING = "android.media.mediaparser.amr.enableCbrSeeking";
    public static final String PARAMETER_FLAC_DISABLE_ID3 = "android.media.mediaparser.flac.disableId3";
    public static final String PARAMETER_MATROSKA_DISABLE_CUES_SEEKING = "android.media.mediaparser.matroska.disableCuesSeeking";
    public static final String PARAMETER_MP3_DISABLE_ID3 = "android.media.mediaparser.mp3.disableId3";
    public static final String PARAMETER_MP3_ENABLE_CBR_SEEKING = "android.media.mediaparser.mp3.enableCbrSeeking";
    public static final String PARAMETER_MP3_ENABLE_INDEX_SEEKING = "android.media.mediaparser.mp3.enableIndexSeeking";
    public static final String PARAMETER_MP4_IGNORE_EDIT_LISTS = "android.media.mediaparser.mp4.ignoreEditLists";
    public static final String PARAMETER_MP4_IGNORE_TFDT_BOX = "android.media.mediaparser.mp4.ignoreTfdtBox";
    public static final String PARAMETER_MP4_TREAT_VIDEO_FRAMES_AS_KEYFRAMES = "android.media.mediaparser.mp4.treatVideoFramesAsKeyframes";
    public static final String PARAMETER_TS_ALLOW_NON_IDR_AVC_KEYFRAMES = "android.media.mediaparser.ts.allowNonIdrAvcKeyframes";
    public static final String PARAMETER_TS_DETECT_ACCESS_UNITS = "android.media.mediaparser.ts.ignoreDetectAccessUnits";
    public static final String PARAMETER_TS_ENABLE_HDMV_DTS_AUDIO_STREAMS = "android.media.mediaparser.ts.enableHdmvDtsAudioStreams";
    public static final String PARAMETER_TS_IGNORE_AAC_STREAM = "android.media.mediaparser.ts.ignoreAacStream";
    public static final String PARAMETER_TS_IGNORE_AVC_STREAM = "android.media.mediaparser.ts.ignoreAvcStream";
    public static final String PARAMETER_TS_IGNORE_SPLICE_INFO_STREAM = "android.media.mediaparser.ts.ignoreSpliceInfoStream";
    public static final String PARAMETER_TS_MODE = "android.media.mediaparser.ts.mode";
    public static final String PARSER_NAME_AC3 = "android.media.mediaparser.Ac3Parser";
    public static final String PARSER_NAME_AC4 = "android.media.mediaparser.Ac4Parser";
    public static final String PARSER_NAME_ADTS = "android.media.mediaparser.AdtsParser";
    public static final String PARSER_NAME_AMR = "android.media.mediaparser.AmrParser";
    public static final String PARSER_NAME_FLAC = "android.media.mediaparser.FlacParser";
    public static final String PARSER_NAME_FLV = "android.media.mediaparser.FlvParser";
    public static final String PARSER_NAME_FMP4 = "android.media.mediaparser.FragmentedMp4Parser";
    public static final String PARSER_NAME_MATROSKA = "android.media.mediaparser.MatroskaParser";
    public static final String PARSER_NAME_MP3 = "android.media.mediaparser.Mp3Parser";
    public static final String PARSER_NAME_MP4 = "android.media.mediaparser.Mp4Parser";
    public static final String PARSER_NAME_OGG = "android.media.mediaparser.OggParser";
    public static final String PARSER_NAME_PS = "android.media.mediaparser.PsParser";
    public static final String PARSER_NAME_TS = "android.media.mediaparser.TsParser";
    public static final String PARSER_NAME_UNKNOWN = "android.media.mediaparser.UNKNOWN";
    public static final String PARSER_NAME_WAV = "android.media.mediaparser.WavParser";
    public static final int SAMPLE_FLAG_DECODE_ONLY = -2147483648; // 0x80000000;
    public static final int SAMPLE_FLAG_ENCRYPTED = 1073741824; // 0x40000000;
    public static final int SAMPLE_FLAG_HAS_SUPPLEMENTAL_DATA = 268435456; // 0x10000000;
    public static final int SAMPLE_FLAG_KEY_FRAME = 1; // 0x1;
    public static final int SAMPLE_FLAG_LAST_SAMPLE = 536870912; // 0x20000000;

    public static interface InputReader {
        long getLength();
        long getPosition();
        int read(byte[] arg0, int arg1, int arg2) throws java.io.IOException;
    }

    public static interface OutputConsumer {
        void onSampleCompleted(int arg0, long arg1, int arg2, int arg3, int arg4, android.media.MediaCodec.CryptoInfo arg5);
        void onSampleDataFound(int arg0, android.media.MediaParser.InputReader arg1) throws java.io.IOException;
        void onSeekMapFound(android.media.MediaParser.SeekMap arg0);
        void onTrackCountFound(int arg0);
        void onTrackDataFound(int arg0, android.media.MediaParser.TrackData arg1);
    }

    public static final class ParsingException extends java.io.IOException {
    }

    public static final class SeekMap {
        public long getDurationMicros() { return 0L; }
        public android.util.Pair<android.media.MediaParser.SeekPoint,android.media.MediaParser.SeekPoint> getSeekPoints(long arg0) { return null; }
        public boolean isSeekable() { return false; }
        public static final int UNKNOWN_DURATION = -2147483648; // 0x80000000;
    }

    /* AOSP MediaParser.SeekPoint */
    public static final class SeekPoint {
        public static final android.media.MediaParser.SeekPoint START = new SeekPoint(0, 0);
        public final long timeMicros;
        public final long position;

        private SeekPoint(long timeMicros, long position) {
            this.timeMicros = timeMicros;
            this.position = position;
        }

        public String toString() {
            return "[timeMicros=" + timeMicros + ", position=" + position + "]";
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            SeekPoint other = (SeekPoint) obj;
            return timeMicros == other.timeMicros && position == other.position;
        }

        public int hashCode() {
            int result = (int) timeMicros;
            result = 31 * result + (int) position;
            return result;
        }
    }

    public static interface SeekableInputReader extends android.media.MediaParser.InputReader {
        void seekToPosition(long arg0);
    }

    /* AOSP MediaParser.TrackData */
    public static final class TrackData {
        public final android.media.MediaFormat mediaFormat;
        public final android.media.DrmInitData drmInitData;

        private TrackData(android.media.MediaFormat mediaFormat, android.media.DrmInitData drmInitData) {
            this.mediaFormat = mediaFormat;
            this.drmInitData = drmInitData;
        }
    }

    public static final class UnrecognizedInputFormatException extends java.io.IOException {
    }
}
