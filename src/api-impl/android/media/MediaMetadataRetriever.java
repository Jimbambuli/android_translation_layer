package android.media;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MediaMetadataRetriever implements AutoCloseable {

	/* the data source for frame extraction: a path, or a region of a file descriptor */
	private String path;
	private FileDescriptor fd;
	private long fdOffset;
	private long fdLength;
	private File tempFile;
	private String[] metadata;

	public void release() {
		if (tempFile != null)
			tempFile.delete();
	}

	public void setDataSource(Context context, Uri uri) {
		setDataSource(uri.getPath());
	}

	public void setDataSource(String path) {
		this.path = path;
		metadata = null;
	}

	/* request headers are not supported by the media backend */
	public void setDataSource(String uri, Map<String, String> headers) {
		setDataSource(uri);
	}

	public void setDataSource(FileDescriptor fd, long offset, long length) {
		this.fd = fd;
		fdOffset = offset;
		fdLength = length;
		metadata = null;
	}

	/* the media backend reads files, so the data source is copied into a temporary file that
	 * goes away with release() */
	public void setDataSource(MediaDataSource dataSource) throws IllegalArgumentException {
		try {
			File file = File.createTempFile("MediaDataSource", null);
			file.deleteOnExit();
			try (FileOutputStream out = new FileOutputStream(file)) {
				byte[] buffer = new byte[65536];
				long position = 0;
				int n;
				while ((n = dataSource.readAt(position, buffer, 0, buffer.length)) > 0) {
					out.write(buffer, 0, n);
					position += n;
				}
			}
			tempFile = file;
			setDataSource(file.getPath());
		} catch (IOException e) {
			throw new IllegalArgumentException(e);
		}
	}

	public void setDataSource(FileDescriptor fd) {
		setDataSource(fd, 0, 0x7ffffffffffffffL);
	}

	/* AOSP MediaMetadataRetriever.close() */
	@Override
	public void close() throws IOException {
		release();
	}

	/* the source as passed to the native_* calls, as getScaledFrameAtTime() does */
	private int nativeFd() {
		if (path == null && fd == null)
			throw new IllegalStateException("no data source");
		return fd != null ? fd.getInt$() : -1;
	}

	public byte[] getEmbeddedPicture() {
		return native_getEmbeddedPicture(path, nativeFd(), fdOffset, fdLength);
	}

	/* AOSP's retriever reads the metadata once with the data source; here on the first call */
	public String extractMetadata(int key) {
		if (metadata == null)
			metadata = native_extractMetadata(path, nativeFd(), fdOffset, fdLength, METADATA_KEY_XMP_LENGTH + 1);
		return key >= 0 && key < metadata.length ? metadata[key] : null;
	}

	public Bitmap getFrameAtTime(long timeUs, int option) {
		return getScaledFrameAtTime(timeUs, option, 0, 0);
	}

	/* frames are always ARGB_8888, which is also what BitmapParams asks for by default */
	public Bitmap getFrameAtTime(long timeUs, int option, BitmapParams params) {
		return getFrameAtTime(timeUs, option);
	}

	public Bitmap getScaledFrameAtTime(long timeUs, int option, int dstWidth, int dstHeight, BitmapParams params) {
		return getScaledFrameAtTime(timeUs, option, dstWidth, dstHeight);
	}

	/* AOSP MediaMetadataRetriever.getFrameAtIndex(int[, BitmapParams]) -> getFramesAtIndex(frameIndex, 1, params).get(0) */
	public Bitmap getFrameAtIndex(int frameIndex, BitmapParams params) {
		return getFramesAtIndex(frameIndex, 1, params).get(0);
	}

	public Bitmap getFrameAtIndex(int frameIndex) {
		return getFramesAtIndex(frameIndex, 1).get(0);
	}

	public List<Bitmap> getFramesAtIndex(int frameIndex, int numFrames, BitmapParams params) {
		return getFramesAtIndexInternal(frameIndex, numFrames, params);
	}

	public List<Bitmap> getFramesAtIndex(int frameIndex, int numFrames) {
		return getFramesAtIndexInternal(frameIndex, numFrames, null);
	}

	/* AOSP MediaMetadataRetriever.getFramesAtIndexInternal(): the checks on METADATA_KEY_HAS_VIDEO and
	 * METADATA_KEY_VIDEO_FRAME_COUNT as in AOSP (ATL's media backend does not report the frame count, so
	 * Integer.parseInt(null) fails as in AOSP for such a source). _getFrameAtIndex() is replaced by seeking by
	 * time to the index's share of the duration, as the backend has no frame index; like the native one the list
	 * is shorter than requested if a frame cannot be retrieved */
	private List<Bitmap> getFramesAtIndexInternal(int frameIndex, int numFrames, BitmapParams params) {
		if (!"yes".equals(extractMetadata(METADATA_KEY_HAS_VIDEO))) {
			throw new IllegalStateException("Does not contain video or image sequences");
		}
		int frameCount = Integer.parseInt(extractMetadata(METADATA_KEY_VIDEO_FRAME_COUNT));
		if (frameIndex < 0 || numFrames < 1 || frameIndex >= frameCount || frameIndex > frameCount - numFrames) {
			throw new IllegalArgumentException("Invalid frameIndex or numFrames: " + frameIndex + ", " + numFrames);
		}
		long durationUs = Long.parseLong(extractMetadata(METADATA_KEY_DURATION)) * 1000;
		List<Bitmap> frames = new ArrayList<>();
		for (int i = frameIndex; i < frameIndex + numFrames; i++) {
			Bitmap frame = getFrameAtTime(durationUs * i / frameCount, OPTION_CLOSEST);
			if (frame == null)
				break;
			frames.add(frame);
		}
		return frames;
	}

	/* AOSP MediaMetadataRetriever.getImageAtIndex()/getPrimaryImage() -> getImageAtIndexInternal() */
	public Bitmap getImageAtIndex(int imageIndex, BitmapParams params) {
		return getImageAtIndexInternal(imageIndex, params);
	}

	public Bitmap getImageAtIndex(int imageIndex) {
		return getImageAtIndexInternal(imageIndex, null);
	}

	public Bitmap getPrimaryImage(BitmapParams params) {
		return getImageAtIndexInternal(-1, params);
	}

	public Bitmap getPrimaryImage() {
		return getImageAtIndexInternal(-1, null);
	}

	/* AOSP getImageAtIndexInternal(): the checks as in AOSP; _getImageAtIndex() is null as the media backend does
	 * not read still image tracks (HEIF/AVIF), the same as getThumbnailImageAtIndex() */
	private Bitmap getImageAtIndexInternal(int imageIndex, BitmapParams params) {
		if (!"yes".equals(extractMetadata(METADATA_KEY_HAS_IMAGE))) {
			throw new IllegalStateException("Does not contain still images");
		}
		String imageCount = extractMetadata(METADATA_KEY_IMAGE_COUNT);
		if (imageIndex >= Integer.parseInt(imageCount)) {
			throw new IllegalArgumentException("Invalid image index: " + imageCount);
		}
		return null;
	}

	/* null as in AOSP when the source has no still image track (HEIF/AVIF), which the media
	 * backend does not read */
	public Bitmap getThumbnailImageAtIndex(int imageIndex, BitmapParams params, int targetSize, int maxPixels) {
		return null;
	}

	public Bitmap getFrameAtTime(long timeUs) {
		return getFrameAtTime(timeUs, OPTION_CLOSEST_SYNC);
	}

	public Bitmap getFrameAtTime() {
		return getFrameAtTime(-1, OPTION_CLOSEST_SYNC);
	}

	/* null if there is no video frame, as in AOSP; callers (e.g. Glide) then show their placeholder */
	public Bitmap getScaledFrameAtTime(long timeUs, int option, int dstWidth, int dstHeight) {
		int[] size = new int[2];
		int[] pixels = native_getFrameAtTime(path, nativeFd(), fdOffset, fdLength,
		                                     timeUs, option, dstWidth, dstHeight, size);
		if (pixels == null)
			return null;
		return Bitmap.createBitmap(pixels, size[0], size[1], Bitmap.Config.ARGB_8888);
	}

	private static native int[] native_getFrameAtTime(String path, int fd, long offset, long length, long timeUs,
	                                                  int option, int dstWidth, int dstHeight, int[] size);
	private static native byte[] native_getEmbeddedPicture(String path, int fd, long offset, long length);
	private static native String[] native_extractMetadata(String path, int fd, long offset, long length, int count);

	public static final class BitmapParams {
		private Bitmap.Config inPreferredConfig = Bitmap.Config.ARGB_8888;
		private Bitmap.Config outActualConfig = Bitmap.Config.ARGB_8888;

		public BitmapParams() {}

		public void setPreferredConfig(Bitmap.Config config) {
			if (config == null) {
				throw new IllegalArgumentException("preferred config can't be null");
			}
			inPreferredConfig = config;
		}

		public Bitmap.Config getPreferredConfig() {
			return inPreferredConfig;
		}

		public Bitmap.Config getActualConfig() {
			return outActualConfig;
		}
	}

	public static final int OPTION_PREVIOUS_SYNC = 0x00;
	public static final int OPTION_NEXT_SYNC = 0x01;
	public static final int OPTION_CLOSEST_SYNC = 0x02;
	public static final int OPTION_CLOSEST = 0x03;
	public static final int METADATA_KEY_CD_TRACK_NUMBER = 0;
	public static final int METADATA_KEY_ALBUM           = 1;
	public static final int METADATA_KEY_ARTIST          = 2;
	public static final int METADATA_KEY_AUTHOR          = 3;
	public static final int METADATA_KEY_COMPOSER        = 4;
	public static final int METADATA_KEY_DATE            = 5;
	public static final int METADATA_KEY_GENRE           = 6;
	public static final int METADATA_KEY_TITLE           = 7;
	public static final int METADATA_KEY_YEAR            = 8;
	public static final int METADATA_KEY_DURATION        = 9;
	public static final int METADATA_KEY_NUM_TRACKS      = 10;
	public static final int METADATA_KEY_WRITER          = 11;
	public static final int METADATA_KEY_MIMETYPE        = 12;
	public static final int METADATA_KEY_ALBUMARTIST     = 13;
	public static final int METADATA_KEY_DISC_NUMBER     = 14;
	public static final int METADATA_KEY_COMPILATION     = 15;
	public static final int METADATA_KEY_HAS_AUDIO       = 16;
	public static final int METADATA_KEY_HAS_VIDEO       = 17;
	public static final int METADATA_KEY_VIDEO_WIDTH     = 18;
	public static final int METADATA_KEY_VIDEO_HEIGHT    = 19;
	public static final int METADATA_KEY_BITRATE         = 20;
	public static final int METADATA_KEY_TIMED_TEXT_LANGUAGES      = 21;
	public static final int METADATA_KEY_IS_DRM          = 22;
	public static final int METADATA_KEY_LOCATION        = 23;
	public static final int METADATA_KEY_VIDEO_ROTATION = 24;
	public static final int METADATA_KEY_CAPTURE_FRAMERATE = 25;
	public static final int METADATA_KEY_HAS_IMAGE       = 26;
	public static final int METADATA_KEY_IMAGE_COUNT     = 27;
	public static final int METADATA_KEY_IMAGE_PRIMARY   = 28;
	public static final int METADATA_KEY_IMAGE_WIDTH     = 29;
	public static final int METADATA_KEY_IMAGE_HEIGHT    = 30;
	public static final int METADATA_KEY_IMAGE_ROTATION  = 31;
	public static final int METADATA_KEY_VIDEO_FRAME_COUNT = 32;
	public static final int METADATA_KEY_EXIF_OFFSET = 33;
	public static final int METADATA_KEY_EXIF_LENGTH = 34;
	public static final int METADATA_KEY_COLOR_STANDARD = 35;
	public static final int METADATA_KEY_COLOR_TRANSFER = 36;
	public static final int METADATA_KEY_COLOR_RANGE    = 37;
	public static final int METADATA_KEY_SAMPLERATE      = 38;
	public static final int METADATA_KEY_BITS_PER_SAMPLE = 39;
	public static final int METADATA_KEY_VIDEO_CODEC_MIME_TYPE = 40;
	public static final int METADATA_KEY_XMP_OFFSET = 41;
	public static final int METADATA_KEY_XMP_LENGTH = 42;
}
