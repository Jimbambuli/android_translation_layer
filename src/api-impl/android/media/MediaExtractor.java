package android.media;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.metrics.LogSessionId;
import android.net.Uri;
import android.os.PersistableBundle;

import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.UUID;

/* API 16. Demuxer on FFmpeg's libavformat (native part: api-impl-jni/media/android_media_MediaExtractor.c) with the
 * behaviour of AOSP's NuMediaExtractor and its JNI (media/jni/android_media_MediaExtractor.cpp): a source that does
 * not open throws IOException("Failed to instantiate extractor."), as does a second setDataSource(); an extractor
 * without a source has no tracks; a wrong track index or a too small buffer is an IllegalArgumentException; at the
 * end of the samples the getters report -1 and advance() false. After release() every native call throws
 * IllegalStateException as AOSP does once the context is cleared.
 * Java wrappers are taken from AOSP media/java/android/media/MediaExtractor.java. */
public final class MediaExtractor {

	public static final int SEEK_TO_PREVIOUS_SYNC = 0;
	public static final int SEEK_TO_NEXT_SYNC = 1;
	public static final int SEEK_TO_CLOSEST_SYNC = 2;

	public static final int SAMPLE_FLAG_SYNC = 1;
	public static final int SAMPLE_FLAG_ENCRYPTED = 2;
	public static final int SAMPLE_FLAG_PARTIAL_FRAME = 4;

	public static final String METRICS_KEY_FORMAT = "android.media.mediaextractor.fmt";
	public static final String METRICS_KEY_MIME_TYPE = "android.media.mediaextractor.mime";
	public static final String METRICS_KEY_TRACKS = "android.media.mediaextractor.ntrk";

	private boolean released;
	private long nativePtr; /* 0: no source */
	private LogSessionId logSessionId = LogSessionId.LOG_SESSION_ID_NONE;

	public MediaExtractor() {
	}

	private void checkNotReleased() {
		if (released)
			throw new IllegalStateException();
	}

	/* NuMediaExtractor::setDataSource(): -EINVAL when there is a source already, else the source's extractor */
	private synchronized void open(String path, String[] keys, String[] values, FileDescriptor fd, long offset, long length, MediaDataSource dataSource) throws IOException {
		checkNotReleased();
		long ptr = nativePtr == 0 ? native_open(path, keys, values, fd != null ? fd.getInt$() : -1, offset, length, dataSource) : 0;
		if (ptr == 0)
			throw new IOException("Failed to instantiate extractor.");
		nativePtr = ptr;
	}

	public final void setDataSource(MediaDataSource dataSource) throws IOException {
		checkNotReleased();
		if (dataSource == null)
			throw new IllegalArgumentException();
		open(null, null, null, null, 0, 0, dataSource);
	}

	public final void setDataSource(Context context, Uri uri, Map<String, String> headers) throws IOException {
		String scheme = uri.getScheme();
		if (scheme == null || scheme.equals("file")) {
			setDataSource(uri.getPath());
			return;
		}

		AssetFileDescriptor fd = null;
		try {
			ContentResolver resolver = context.getContentResolver();
			fd = resolver.openAssetFileDescriptor(uri, "r");
			if (fd == null) {
				return;
			}
			if (fd.getDeclaredLength() < 0) {
				setDataSource(fd.getFileDescriptor());
			} else {
				setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getDeclaredLength());
			}
			return;
		} catch (SecurityException ex) {
		} catch (IOException ex) {
		} finally {
			if (fd != null) {
				fd.close();
			}
		}

		setDataSource(uri.toString(), headers);
	}

	public final void setDataSource(String path, Map<String, String> headers) throws IOException {
		String[] keys = null;
		String[] values = null;
		if (headers != null) {
			keys = new String[headers.size()];
			values = new String[headers.size()];
			int i = 0;
			for (Map.Entry<String, String> entry : headers.entrySet()) {
				keys[i] = entry.getKey();
				values[i] = entry.getValue();
				++i;
			}
		}
		nativeSetDataSource(path, keys, values);
	}

	public final void setDataSource(String path) throws IOException {
		nativeSetDataSource(path, null, null);
	}

	private void nativeSetDataSource(String path, String[] keys, String[] values) throws IOException {
		checkNotReleased();
		if (path == null)
			throw new IllegalArgumentException();
		open(path, keys, values, null, 0, 0, null);
	}

	public final void setDataSource(AssetFileDescriptor afd) throws IOException, IllegalArgumentException, IllegalStateException {
		if (afd == null)
			throw new NullPointerException();
		if (afd.getDeclaredLength() < 0) {
			setDataSource(afd.getFileDescriptor());
		} else {
			setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getDeclaredLength());
		}
	}

	public final void setDataSource(FileDescriptor fd) throws IOException {
		setDataSource(fd, 0, 0x7ffffffffffffffL);
	}

	/* the descriptor stays the caller's: the native part reads a dup of it, as NuMediaExtractor */
	public final void setDataSource(FileDescriptor fd, long offset, long length) throws IOException {
		checkNotReleased();
		if (fd == null)
			throw new IllegalArgumentException();
		open(null, null, null, fd, offset, length, null);
	}

	public final synchronized void release() {
		if (nativePtr != 0)
			native_release(nativePtr);
		nativePtr = 0;
		released = true;
	}

	public final synchronized int getTrackCount() {
		checkNotReleased();
		return nativePtr == 0 ? 0 : native_getTrackCount(nativePtr);
	}

	public DrmInitData getDrmInitData() {
		checkNotReleased();
		return null;
	}

	public Map<UUID, byte[]> getPsshInfo() {
		checkNotReleased();
		return null;
	}

	private synchronized Map<String, Object> getTrackFormatNative(int index) {
		checkNotReleased();
		Map<String, Object> format = nativePtr == 0 ? null : native_getTrackFormat(nativePtr, index);
		if (format == null)
			throw new IllegalArgumentException();
		return format;
	}

	public MediaFormat getTrackFormat(int index) {
		return new MediaFormat(getTrackFormatNative(index));
	}

	/* AOSP getFileFormatNative(): the container's "mime"; for AMediaExtractor_getFileFormat() */
	private synchronized MediaFormat getFileFormat() {
		checkNotReleased();
		Map<String, Object> format = nativePtr == 0 ? null : native_getFileFormat(nativePtr);
		if (format == null)
			throw new IllegalArgumentException();
		return new MediaFormat(format);
	}

	public synchronized void selectTrack(int index) {
		checkNotReleased();
		if (nativePtr == 0 || !native_selectTrack(nativePtr, index, true))
			throw new IllegalArgumentException();
	}

	public synchronized void unselectTrack(int index) {
		checkNotReleased();
		if (nativePtr == 0 || !native_selectTrack(nativePtr, index, false))
			throw new IllegalArgumentException();
	}

	public synchronized void seekTo(long timeUs, int mode) {
		checkNotReleased();
		if (mode < SEEK_TO_PREVIOUS_SYNC || mode > SEEK_TO_CLOSEST_SYNC)
			throw new IllegalArgumentException();
		if (nativePtr != 0)
			native_seekTo(nativePtr, timeUs, mode);
	}

	public synchronized boolean advance() {
		checkNotReleased();
		return nativePtr != 0 && native_advance(nativePtr);
	}

	/* the JNI sets limit and position around the sample, as AOSP's JMediaExtractor::readSampleData() */
	public synchronized int readSampleData(ByteBuffer byteBuf, int offset) {
		checkNotReleased();
		int size = nativePtr == 0 ? -1 : native_readSampleData(nativePtr, byteBuf, offset);
		if (size == -2)
			throw new IllegalArgumentException();
		if (size >= 0) {
			byteBuf.limit(offset + size);
			byteBuf.position(offset);
		}
		return size;
	}

	private long sample(int what) {
		checkNotReleased();
		long value = nativePtr == 0 ? Long.MIN_VALUE : native_getSample(nativePtr, what);
		return value == Long.MIN_VALUE ? -1 : value;
	}

	public synchronized int getSampleTrackIndex() {
		return (int)sample(0);
	}

	public synchronized long getSampleTime() {
		return sample(1);
	}

	public synchronized long getSampleSize() {
		return sample(2);
	}

	public synchronized int getSampleFlags() {
		return (int)sample(3);
	}

	/* no sample is encrypted: getSampleMeta() has no kKeyEncryptedSizes */
	public synchronized boolean getSampleCryptoInfo(MediaCodec.CryptoInfo info) {
		checkNotReleased();
		return false;
	}

	/* NuMediaExtractor::getCachedDuration(): no caching data source (NuCachedSource2) */
	public long getCachedDuration() {
		checkNotReleased();
		return -1;
	}

	public boolean hasCacheReachedEndOfStream() {
		checkNotReleased();
		return true;
	}

	public void setLogSessionId(LogSessionId logSessionId) {
		this.logSessionId = logSessionId;
	}

	public LogSessionId getLogSessionId() {
		return logSessionId;
	}

	public PersistableBundle getMetrics() {
		return new PersistableBundle();
	}

	protected void finalize() {
		release();
	}

	/* no track carries an audio presentation (NuMediaExtractor: ERROR_UNSUPPORTED, an empty list) */
	public java.util.List<android.media.AudioPresentation> getAudioPresentations(int trackIndex) {
		checkNotReleased();
		return new java.util.ArrayList<>();
	}
    /* AOSP 16 API; no backing hardware/service in ATL */
    public android.media.MediaExtractor.CasInfo getCasInfo(int arg0) { return null; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public void setMediaCas(android.media.MediaCas arg0) {  }

    /* AOSP 16 API; no backing hardware/service in ATL */
    public static final class CasInfo {
        public byte[] getPrivateData() { return null; }
        public android.media.MediaCas.Session getSession() { return null; }
        public int getSystemId() { return 0; }
    }

    /* AOSP 16 API; no backing hardware/service in ATL */
    public static final class MetricsConstants {
        public static final String FORMAT = "android.media.mediaextractor.fmt";
        public static final String MIME_TYPE = "android.media.mediaextractor.mime";
        public static final String TRACKS = "android.media.mediaextractor.ntrk";
    }

	private static native long native_open(String path, String[] keys, String[] values, int fd, long offset, long length, MediaDataSource dataSource);
	private static native void native_release(long ptr);
	private static native int native_getTrackCount(long ptr);
	private static native Map<String, Object> native_getTrackFormat(long ptr, int index);
	private static native Map<String, Object> native_getFileFormat(long ptr);
	private static native boolean native_selectTrack(long ptr, int index, boolean select);
	private static native void native_seekTo(long ptr, long timeUs, int mode);
	private static native boolean native_advance(long ptr);
	private static native int native_readSampleData(long ptr, ByteBuffer buffer, int offset);
	private static native long native_getSample(long ptr, int what);
}
