package android.media;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import java.io.FileDescriptor;
import java.util.Map;

public class MediaMetadataRetriever {

	private MediaPlayer mediaPlayer;
	/* the data source for frame extraction: a path, or a region of a file descriptor */
	private String path;
	private FileDescriptor fd;
	private long fdOffset;
	private long fdLength;

	public void release() {
		if (mediaPlayer != null)
			mediaPlayer.release();
	}

	public void setDataSource(Context context, Uri uri) {
		setDataSource(uri.getPath());
	}

	public void setDataSource(String path) {
		this.path = path;
		mediaPlayer = new MediaPlayer();
		mediaPlayer.setDataSource(path);
	}

	/* request headers are not supported by the media backend */
	public void setDataSource(String uri, Map<String, String> headers) {
		setDataSource(uri);
	}

	public void setDataSource(FileDescriptor fd, long offset, long length) {
		this.fd = fd;
		fdOffset = offset;
		fdLength = length;
		mediaPlayer = new MediaPlayer();
		mediaPlayer.setDataSource(fd, offset, length);
	}

	public void setDataSource(FileDescriptor fd) {
		setDataSource(fd, 0, 0x7ffffffffffffffL);
	}

	public byte[] getEmbeddedPicture() {
		return null;
	}

	public String extractMetadata(int key) {
		switch (key) {
			case 9 /*METADATA_KEY_DURATION*/:
				return String.valueOf(mediaPlayer.getDuration());
			default:
				return null;
		}
	}

	public Bitmap getFrameAtTime(long timeUs, int option) {
		return getScaledFrameAtTime(timeUs, option, 0, 0);
	}

	public Bitmap getFrameAtTime(long timeUs) {
		return getFrameAtTime(timeUs, OPTION_CLOSEST_SYNC);
	}

	public Bitmap getFrameAtTime() {
		return getFrameAtTime(-1, OPTION_CLOSEST_SYNC);
	}

	/* null if there is no video frame, as in AOSP; callers (e.g. Glide) then show their placeholder */
	public Bitmap getScaledFrameAtTime(long timeUs, int option, int dstWidth, int dstHeight) {
		if (path == null && fd == null)
			throw new IllegalStateException("no data source");
		int[] size = new int[2];
		int[] pixels = native_getFrameAtTime(path, fd != null ? fd.getInt$() : -1, fdOffset, fdLength,
		                                     timeUs, option, dstWidth, dstHeight, size);
		if (pixels == null)
			return null;
		return Bitmap.createBitmap(pixels, size[0], size[1], Bitmap.Config.ARGB_8888);
	}

	private static native int[] native_getFrameAtTime(String path, int fd, long offset, long length, long timeUs,
	                                                  int option, int dstWidth, int dstHeight, int[] size);

	public static final int OPTION_PREVIOUS_SYNC = 0x00;
	public static final int OPTION_NEXT_SYNC = 0x01;
	public static final int OPTION_CLOSEST_SYNC = 0x02;
	public static final int OPTION_CLOSEST = 0x03;
}
