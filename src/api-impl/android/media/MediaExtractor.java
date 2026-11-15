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

/* API 16. Demuxer. ATL ships no extractor plugin (AOSP NuMediaExtractor/MediaExtractorFactory), so no
 * data source can be opened: every setDataSource() throws IOException("Failed to instantiate extractor."),
 * which is what AOSP's JNI (media/jni/android_media_MediaExtractor.cpp) throws when no extractor sniffs
 * the content. The remaining methods follow that JNI for an extractor without data: getTrackCount() is 0
 * (NuMediaExtractor::countTracks with mImpl == NULL), getTrackFormat/selectTrack/unselectTrack throw
 * IllegalArgumentException, the sample getters report -1 and advance() false. After release() every
 * native call throws IllegalStateException as AOSP does once the context is cleared.
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
	private LogSessionId logSessionId = LogSessionId.LOG_SESSION_ID_NONE;

	public MediaExtractor() {
	}

	private void checkNotReleased() {
		if (released)
			throw new IllegalStateException();
	}

	private void noExtractor() throws IOException {
		throw new IOException("Failed to instantiate extractor.");
	}

	public final void setDataSource(MediaDataSource dataSource) throws IOException {
		checkNotReleased();
		if (dataSource == null)
			throw new IllegalArgumentException();
		noExtractor();
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
		nativeSetDataSource(path);
	}

	public final void setDataSource(String path) throws IOException {
		nativeSetDataSource(path);
	}

	private void nativeSetDataSource(String path) throws IOException {
		checkNotReleased();
		if (path == null)
			throw new IllegalArgumentException();
		noExtractor();
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

	public final void setDataSource(FileDescriptor fd, long offset, long length) throws IOException {
		checkNotReleased();
		if (fd == null)
			throw new IllegalArgumentException();
		noExtractor();
	}

	public final void release() {
		released = true;
	}

	public final int getTrackCount() {
		checkNotReleased();
		return 0;
	}

	public DrmInitData getDrmInitData() {
		checkNotReleased();
		return null;
	}

	public Map<UUID, byte[]> getPsshInfo() {
		checkNotReleased();
		return null;
	}

	public MediaFormat getTrackFormat(int index) {
		checkNotReleased();
		throw new IllegalArgumentException();
	}

	public void selectTrack(int index) {
		checkNotReleased();
		throw new IllegalArgumentException();
	}

	public void unselectTrack(int index) {
		checkNotReleased();
		throw new IllegalArgumentException();
	}

	/* NuMediaExtractor::seekTo without selected tracks reaches the end of stream, which the JNI ignores */
	public void seekTo(long timeUs, int mode) {
		checkNotReleased();
	}

	public boolean advance() {
		checkNotReleased();
		return false;
	}

	public int readSampleData(ByteBuffer byteBuf, int offset) {
		checkNotReleased();
		return -1;
	}

	public int getSampleTrackIndex() {
		checkNotReleased();
		return -1;
	}

	public long getSampleTime() {
		checkNotReleased();
		return -1;
	}

	public long getSampleSize() {
		checkNotReleased();
		return -1;
	}

	public int getSampleFlags() {
		checkNotReleased();
		return -1;
	}

	public boolean getSampleCryptoInfo(MediaCodec.CryptoInfo info) {
		checkNotReleased();
		return false;
	}

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
    /* AOSP 16 API; no backing hardware/service in ATL */
    protected void finalize() {  }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public java.util.List<android.media.AudioPresentation> getAudioPresentations(int arg0) { return new java.util.ArrayList<>(); }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public android.media.MediaExtractor.CasInfo getCasInfo(int arg0) { return null; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public void setMediaCas(android.media.MediaCas arg0) {  }
}
