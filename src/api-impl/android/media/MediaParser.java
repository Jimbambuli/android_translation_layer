package android.media;

/* AOSP frameworks/base/media/java/android/media/MediaParser.java (public API surface; no backing service in ATL, bodies follow the AOSP no-service path) */
public final class MediaParser {
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

    public static final String PARAMETER_IN_BAND_CRYPTO_INFO = "android.media.mediaparser.inBandCryptoInfo";
    public static final String PARAMETER_INCLUDE_SUPPLEMENTAL_DATA = "android.media.mediaparser.includeSupplementalData";
    public static final String PARAMETER_IGNORE_TIMESTAMP_OFFSET = "android.media.mediaparser.ignoreTimestampOffset";
    public static final String PARAMETER_EAGERLY_EXPOSE_TRACKTYPE = "android.media.mediaparser.eagerlyExposeTrackType";
    public static final String PARAMETER_EXPOSE_DUMMY_SEEKMAP = "android.media.mediaparser.exposeDummySeekMap";
    public static final String PARAMETER_EXPOSE_CHUNK_INDEX_AS_MEDIA_FORMAT = "android.media.mediaParser.exposeChunkIndexAsMediaFormat";
    public static final String PARAMETER_EXPOSE_CAPTION_FORMATS = "android.media.mediaParser.exposeCaptionFormats";
    public static final String PARAMETER_OVERRIDE_IN_BAND_CAPTION_DECLARATIONS = "android.media.mediaParser.overrideInBandCaptionDeclarations";
    public static final String PARAMETER_EXPOSE_EMSG_TRACK = "android.media.mediaParser.exposeEmsgTrack";

    private static final String TS_MODE_SINGLE_PMT = "single_pmt";
    private static final String TS_MODE_MULTI_PMT = "multi_pmt";
    private static final String TS_MODE_HLS = "hls";
    /* Same order as AOSP EXTRACTOR_FACTORIES_BY_NAME (ExoPlayer DefaultExtractorsFactory order). */
    private static final java.util.LinkedHashSet<String> PARSER_NAMES = new java.util.LinkedHashSet<>(java.util.Arrays.asList(
            PARSER_NAME_MATROSKA, PARSER_NAME_FMP4, PARSER_NAME_MP4, PARSER_NAME_MP3, PARSER_NAME_ADTS, PARSER_NAME_AC3,
            PARSER_NAME_TS, PARSER_NAME_FLV, PARSER_NAME_OGG, PARSER_NAME_PS, PARSER_NAME_WAV, PARSER_NAME_AMR,
            PARSER_NAME_AC4, PARSER_NAME_FLAC));
    private static final java.util.HashMap<String, Class> EXPECTED_TYPE_BY_PARAMETER_NAME = new java.util.HashMap<>();
    static {
        for (String name : new String[] {PARAMETER_ADTS_ENABLE_CBR_SEEKING, PARAMETER_AMR_ENABLE_CBR_SEEKING,
                PARAMETER_FLAC_DISABLE_ID3, PARAMETER_MP4_IGNORE_EDIT_LISTS, PARAMETER_MP4_IGNORE_TFDT_BOX,
                PARAMETER_MP4_TREAT_VIDEO_FRAMES_AS_KEYFRAMES, PARAMETER_MATROSKA_DISABLE_CUES_SEEKING,
                PARAMETER_MP3_DISABLE_ID3, PARAMETER_MP3_ENABLE_CBR_SEEKING, PARAMETER_MP3_ENABLE_INDEX_SEEKING,
                PARAMETER_TS_ALLOW_NON_IDR_AVC_KEYFRAMES, PARAMETER_TS_IGNORE_AAC_STREAM, PARAMETER_TS_IGNORE_AVC_STREAM,
                PARAMETER_TS_IGNORE_SPLICE_INFO_STREAM, PARAMETER_TS_DETECT_ACCESS_UNITS,
                PARAMETER_TS_ENABLE_HDMV_DTS_AUDIO_STREAMS, PARAMETER_IN_BAND_CRYPTO_INFO,
                PARAMETER_INCLUDE_SUPPLEMENTAL_DATA, PARAMETER_IGNORE_TIMESTAMP_OFFSET, PARAMETER_EAGERLY_EXPOSE_TRACKTYPE,
                PARAMETER_EXPOSE_DUMMY_SEEKMAP, PARAMETER_EXPOSE_CHUNK_INDEX_AS_MEDIA_FORMAT,
                PARAMETER_OVERRIDE_IN_BAND_CAPTION_DECLARATIONS, PARAMETER_EXPOSE_EMSG_TRACK}) {
            EXPECTED_TYPE_BY_PARAMETER_NAME.put(name, Boolean.class);
        }
        EXPECTED_TYPE_BY_PARAMETER_NAME.put(PARAMETER_TS_MODE, String.class);
    }

    private final java.util.Map<String, Object> mParserParameters = new java.util.HashMap<>();
    private final OutputConsumer mOutputConsumer;
    private final String[] mParserNamesPool;
    private String mParserName;
    private boolean mStarted;
    private boolean mExposeDummySeekMap;
    private boolean mReleased;
    private android.media.metrics.LogSessionId mLogSessionId;

    /* AOSP creates the extractors from the ExoPlayer library bundled in the media module; ATL has none, so no parser can read any stream. */
    private MediaParser(OutputConsumer outputConsumer, boolean createdByName, String... parserNamesPool) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.R) {
            throw new UnsupportedOperationException("Android version must be R or greater.");
        }
        mOutputConsumer = outputConsumer;
        mParserNamesPool = parserNamesPool;
        mParserName = createdByName ? parserNamesPool[0] : PARSER_NAME_UNKNOWN;
    }

    private static void assertValidNames(String[] names) {
        for (String name : names) {
            if (!PARSER_NAMES.contains(name)) {
                throw new IllegalArgumentException("Invalid extractor name: " + name + ". Supported parsers are: "
                        + android.text.TextUtils.join(", ", PARSER_NAMES) + ".");
            }
        }
    }

    public static android.media.MediaParser createByName(String name, android.media.MediaParser.OutputConsumer outputConsumer) {
        assertValidNames(new String[] {name});
        return new MediaParser(outputConsumer, true, name);
    }

    public static android.media.MediaParser create(android.media.MediaParser.OutputConsumer outputConsumer, java.lang.String... parserNames) {
        assertValidNames(parserNames);
        if (parserNames.length == 0) {
            parserNames = PARSER_NAMES.toArray(new String[0]);
        }
        return new MediaParser(outputConsumer, false, parserNames);
    }

    public static java.util.List<java.lang.String> getParserNames(android.media.MediaFormat mediaFormat) {
        String mimeType = mediaFormat.getString(MediaFormat.KEY_MIME);
        mimeType = mimeType == null ? null : mimeType.toLowerCase(java.util.Locale.ROOT);
        if (android.text.TextUtils.isEmpty(mimeType)) {
            return java.util.Collections.unmodifiableList(new java.util.ArrayList<>(PARSER_NAMES));
        }
        java.util.ArrayList<String> result = new java.util.ArrayList<>();
        switch (mimeType) {
            case "video/x-matroska":
            case "audio/x-matroska":
            case "video/x-webm":
            case "audio/x-webm":
                result.add(PARSER_NAME_MATROSKA);
                break;
            case "video/mp4":
            case "audio/mp4":
            case "application/mp4":
                result.add(PARSER_NAME_MP4);
                result.add(PARSER_NAME_FMP4);
                break;
            case "audio/mpeg":
                result.add(PARSER_NAME_MP3);
                break;
            case "audio/aac":
                result.add(PARSER_NAME_ADTS);
                break;
            case "audio/ac3":
                result.add(PARSER_NAME_AC3);
                break;
            case "video/mp2t":
            case "audio/mp2t":
                result.add(PARSER_NAME_TS);
                break;
            case "video/x-flv":
                result.add(PARSER_NAME_FLV);
                break;
            case "video/ogg":
            case "audio/ogg":
            case "application/ogg":
                result.add(PARSER_NAME_OGG);
                break;
            case "video/mp2p":
            case "video/mp1s":
                result.add(PARSER_NAME_PS);
                break;
            case "audio/vnd.wave":
            case "audio/wav":
            case "audio/wave":
            case "audio/x-wav":
                result.add(PARSER_NAME_WAV);
                break;
            case "audio/amr":
                result.add(PARSER_NAME_AMR);
                break;
            case "audio/ac4":
                result.add(PARSER_NAME_AC4);
                break;
            case "audio/flac":
            case "audio/x-flac":
                result.add(PARSER_NAME_FLAC);
                break;
            default:
                break;
        }
        return java.util.Collections.unmodifiableList(result);
    }

    public android.media.MediaParser setParameter(String parameterName, Object value) {
        if (mStarted) {
            throw new IllegalStateException("setParameters() must be called before the first advance() call.");
        }
        Class expectedType = EXPECTED_TYPE_BY_PARAMETER_NAME.get(parameterName);
        if (expectedType != null && !expectedType.isInstance(value)) {
            throw new IllegalArgumentException(parameterName + " expects a " + expectedType.getSimpleName() + " but a "
                    + value.getClass().getSimpleName() + " was passed.");
        }
        if (PARAMETER_TS_MODE.equals(parameterName) && !TS_MODE_SINGLE_PMT.equals(value) && !TS_MODE_HLS.equals(value)
                && !TS_MODE_MULTI_PMT.equals(value)) {
            throw new IllegalArgumentException(PARAMETER_TS_MODE + " does not accept: " + value);
        }
        if (PARAMETER_EXPOSE_DUMMY_SEEKMAP.equals(parameterName)) {
            mExposeDummySeekMap = (boolean) value;
        }
        mParserParameters.put(parameterName, value);
        return this;
    }

    public boolean supportsParameter(String parameterName) {
        return EXPECTED_TYPE_BY_PARAMETER_NAME.containsKey(parameterName)
                || PARAMETER_EXPOSE_CAPTION_FORMATS.equals(parameterName);
    }

    public String getParserName() {
        return mParserName;
    }

    /* No extractor library: the AOSP outcome when no parser of the pool recognizes the stream (sniffing fails for all). */
    public boolean advance(android.media.MediaParser.SeekableInputReader seekableInputReader) throws java.io.IOException {
        mStarted = true;
        if (mExposeDummySeekMap && !mParserName.equals(PARSER_NAME_UNKNOWN)) {
            mOutputConsumer.onSeekMapFound(SeekMap.DUMMY);
        }
        throw UnrecognizedInputFormatException.createForExtractors(mParserNamesPool);
    }

    public void seek(android.media.MediaParser.SeekPoint seekPoint) {
    }

    public void release() {
        mReleased = true;
    }

    public void setLogSessionId(android.media.metrics.LogSessionId logSessionId) {
        this.mLogSessionId = java.util.Objects.requireNonNull(logSessionId);
    }

    public android.media.metrics.LogSessionId getLogSessionId() {
        return mLogSessionId != null ? mLogSessionId : android.media.metrics.LogSessionId.LOG_SESSION_ID_NONE;
    }

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
        private ParsingException(Throwable cause) {
            super(cause);
        }
    }

    public static final class SeekMap {
        public static final int UNKNOWN_DURATION = -2147483648; // 0x80000000;
        /* AOSP @hide: one SeekPoint per request, at the requested time and position 0 */
        static final SeekMap DUMMY = new SeekMap(true);
        private final boolean mDummy;

        private SeekMap(boolean dummy) {
            mDummy = dummy;
        }

        public boolean isSeekable() { return mDummy; }
        public long getDurationMicros() { return UNKNOWN_DURATION; }
        public android.util.Pair<android.media.MediaParser.SeekPoint,android.media.MediaParser.SeekPoint> getSeekPoints(long timeMicros) {
            SeekPoint seekPoint = new SeekPoint(timeMicros, 0);
            return new android.util.Pair<>(seekPoint, seekPoint);
        }
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
        private static UnrecognizedInputFormatException createForExtractors(String... extractorNames) {
            StringBuilder builder = new StringBuilder();
            builder.append("None of the available parsers ( ");
            builder.append(extractorNames[0]);
            for (int i = 1; i < extractorNames.length; i++) {
                builder.append(", ");
                builder.append(extractorNames[i]);
            }
            builder.append(") could read the stream.");
            return new UnrecognizedInputFormatException(builder.toString());
        }

        private UnrecognizedInputFormatException(String extractorNames) {
            super(extractorNames);
        }
    }
}
