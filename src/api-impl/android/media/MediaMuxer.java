package android.media;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.media.MediaCodec.BufferInfo;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.ByteBuffer;

/* AOSP frameworks/base/media/java/android/media/MediaMuxer.java: the argument checks and the states. ATL has no muxer
 * that writes the container (libstagefright), so creating one fails with an IOException, like a muxer AOSP cannot create. */
public final class MediaMuxer {
    public static final class OutputFormat {
        private OutputFormat() {}
        public static final int MUXER_OUTPUT_FIRST   = 0;
        public static final int MUXER_OUTPUT_MPEG_4 = MUXER_OUTPUT_FIRST;
        public static final int MUXER_OUTPUT_WEBM   = MUXER_OUTPUT_FIRST + 1;
        public static final int MUXER_OUTPUT_3GPP   = MUXER_OUTPUT_FIRST + 2;
        public static final int MUXER_OUTPUT_HEIF   = MUXER_OUTPUT_FIRST + 3;
        public static final int MUXER_OUTPUT_OGG   = MUXER_OUTPUT_FIRST + 4;
        public static final int MUXER_OUTPUT_LAST   = MUXER_OUTPUT_OGG;
    };
    @IntDef({
        OutputFormat.MUXER_OUTPUT_MPEG_4,
        OutputFormat.MUXER_OUTPUT_WEBM,
        OutputFormat.MUXER_OUTPUT_3GPP,
        OutputFormat.MUXER_OUTPUT_HEIF,
        OutputFormat.MUXER_OUTPUT_OGG,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface Format {}
    /* AOSP nativeSetup() creates libstagefright's MediaMuxer on the descriptor; ATL has no muxer, so AOSP's failure of a
     * muxer that cannot be created: an IOException */
    private static long nativeSetup(@NonNull FileDescriptor fd, int format) throws IllegalArgumentException, IOException {
        throw new IOException("Failed to instantiate the muxer: ATL has no media muxer");
    }
    private static void nativeRelease(long nativeObject) {}
    private static void nativeStart(long nativeObject) {}
    private static void nativeStop(long nativeObject) {}
    private static int nativeAddTrack(long nativeObject, String[] keys, Object[] values) {
        throw new IllegalStateException("Muxer has been released!"); // no native muxer exists, see nativeSetup()
    }
    private static void nativeSetOrientationHint(long nativeObject, int degrees) {}
    private static void nativeSetLocation(long nativeObject, int latitude, int longitude) {}
    private static void nativeWriteSampleData(long nativeObject, int trackIndex, @NonNull ByteBuffer byteBuf, int offset, int size, long presentationTimeUs, int flags) {}
    private static final int MUXER_STATE_UNINITIALIZED  = -1;
    private static final int MUXER_STATE_INITIALIZED    = 0;
    private static final int MUXER_STATE_STARTED        = 1;
    private static final int MUXER_STATE_STOPPED        = 2;
    private int mState = MUXER_STATE_UNINITIALIZED;
        private int mLastTrackIndex = -1;
    private long mNativeObject;
    private String convertMuxerStateCodeToString(int aState) {
        switch (aState) {
            case MUXER_STATE_UNINITIALIZED:
                return "UNINITIALIZED";
            case MUXER_STATE_INITIALIZED:
                return "INITIALIZED";
            case MUXER_STATE_STARTED:
                return "STARTED";
            case MUXER_STATE_STOPPED:
                return "STOPPED";
            default:
                return "UNKNOWN";
        }
    }
    public MediaMuxer(@NonNull String path, @Format int format) throws IOException {
        if (path == null) {
            throw new IllegalArgumentException("path must not be null");
        }
        RandomAccessFile file = null;
        try {
            file = new RandomAccessFile(path, "rws");
            file.setLength(0);
            FileDescriptor fd = file.getFD();
            setUpMediaMuxer(fd, format);
        } finally {
            if (file != null) {
                file.close();
            }
        }
    }
    public MediaMuxer(@NonNull FileDescriptor fd, @Format int format) throws IOException {
        setUpMediaMuxer(fd, format);
    }
    private void setUpMediaMuxer(@NonNull FileDescriptor fd, @Format int format) throws IOException {
        if (format < OutputFormat.MUXER_OUTPUT_FIRST || format > OutputFormat.MUXER_OUTPUT_LAST) {
            throw new IllegalArgumentException("format: " + format + " is invalid");
        }
        mNativeObject = nativeSetup(fd, format);
        mState = MUXER_STATE_INITIALIZED;
    }
    public void setOrientationHint(int degrees) {
        if (degrees != 0 && degrees != 90  && degrees != 180 && degrees != 270) {
            throw new IllegalArgumentException("Unsupported angle: " + degrees);
        }
        if (mState == MUXER_STATE_INITIALIZED) {
            nativeSetOrientationHint(mNativeObject, degrees);
        } else {
            throw new IllegalStateException("Can't set rotation degrees due" +
                    " to wrong state(" + convertMuxerStateCodeToString(mState) + ")");
        }
    }
    public void setLocation(float latitude, float longitude) {
        int latitudex10000  = Math.round(latitude * 10000);
        int longitudex10000 = Math.round(longitude * 10000);
        if (latitudex10000 > 900000 || latitudex10000 < -900000) {
            String msg = "Latitude: " + latitude + " out of range.";
            throw new IllegalArgumentException(msg);
        }
        if (longitudex10000 > 1800000 || longitudex10000 < -1800000) {
            String msg = "Longitude: " + longitude + " out of range";
            throw new IllegalArgumentException(msg);
        }
        if (mState == MUXER_STATE_INITIALIZED && mNativeObject != 0) {
            nativeSetLocation(mNativeObject, latitudex10000, longitudex10000);
        } else {
            throw new IllegalStateException("Can't set location due to wrong state("
                                             + convertMuxerStateCodeToString(mState) + ")");
        }
    }
    public void start() {
        if (mNativeObject == 0) {
            throw new IllegalStateException("Muxer has been released!");
        }
        if (mState == MUXER_STATE_INITIALIZED) {
            nativeStart(mNativeObject);
            mState = MUXER_STATE_STARTED;
        } else {
            throw new IllegalStateException("Can't start due to wrong state("
                                             + convertMuxerStateCodeToString(mState) + ")");
        }
    }
    public void stop() {
        if (mState == MUXER_STATE_STARTED) {
            try {
                nativeStop(mNativeObject);
            } catch (Exception e) {
                throw e;
            } finally {
                mState = MUXER_STATE_STOPPED;
            }
        } else {
            throw new IllegalStateException("Can't stop due to wrong state("
                                             + convertMuxerStateCodeToString(mState) + ")");
        }
    }
    @Override
    protected void finalize() throws Throwable {
        try {
            if (mNativeObject != 0) {
                nativeRelease(mNativeObject);
                mNativeObject = 0;
            }
        } finally {
            super.finalize();
        }
    }
    public int addTrack(@NonNull MediaFormat format) {
        if (format == null) {
            throw new IllegalArgumentException("format must not be null.");
        }
        if (mState != MUXER_STATE_INITIALIZED) {
            throw new IllegalStateException("Muxer is not initialized.");
        }
        if (mNativeObject == 0) {
            throw new IllegalStateException("Muxer has been released!");
        }
        int trackIndex = -1;
        int mapSize = format.getKeys().size();
        if (mapSize > 0) {
            trackIndex = nativeAddTrack(mNativeObject, null, null);
        } else {
            throw new IllegalArgumentException("format must not be empty.");
        }
        if (mLastTrackIndex >= trackIndex) {
            throw new IllegalArgumentException("Invalid format.");
        }
        mLastTrackIndex = trackIndex;
        return trackIndex;
    }
    public void writeSampleData(int trackIndex, @NonNull ByteBuffer byteBuf,
            @NonNull BufferInfo bufferInfo) {
        if (trackIndex < 0 || trackIndex > mLastTrackIndex) {
            throw new IllegalArgumentException("trackIndex is invalid");
        }
        if (byteBuf == null) {
            throw new IllegalArgumentException("byteBuffer must not be null");
        }
        if (bufferInfo == null) {
            throw new IllegalArgumentException("bufferInfo must not be null");
        }
        if (bufferInfo.size < 0 || bufferInfo.offset < 0
                || (bufferInfo.offset + bufferInfo.size) > byteBuf.capacity()) {
            throw new IllegalArgumentException("bufferInfo must specify a" +
                    " valid buffer offset and size");
        }
        if (mNativeObject == 0) {
            throw new IllegalStateException("Muxer has been released!");
        }
        if (mState != MUXER_STATE_STARTED) {
            throw new IllegalStateException("Can't write, muxer is not started");
        }
        nativeWriteSampleData(mNativeObject, trackIndex, byteBuf,
                bufferInfo.offset, bufferInfo.size,
                bufferInfo.presentationTimeUs, bufferInfo.flags);
    }
    public void release() {
        if (mState == MUXER_STATE_STARTED) {
            stop();
        }
        if (mNativeObject != 0) {
            nativeRelease(mNativeObject);
            mNativeObject = 0;
        }
        mState = MUXER_STATE_UNINITIALIZED;
    }
}
