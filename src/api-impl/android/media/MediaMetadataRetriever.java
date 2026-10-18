package android.media;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import java.io.FileDescriptor;
import java.util.Map;

public class MediaMetadataRetriever {

	private MediaPlayer mediaPlayer;

	public void release() {
		if (mediaPlayer != null)
			mediaPlayer.release();
	}

	public void setDataSource(Context context, Uri uri) {
		mediaPlayer = new MediaPlayer();
		mediaPlayer.setDataSource(uri.getPath());
	}

	public void setDataSource(String path) {
		mediaPlayer = new MediaPlayer();
		mediaPlayer.setDataSource(path);
	}

	/* request headers are not supported by the media backend */
	public void setDataSource(String uri, Map<String, String> headers) {
		setDataSource(uri);
	}

	public void setDataSource(FileDescriptor fd, long offset, long length) {
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

	/* Frames are not extracted: null means "no frame", which AOSP returns as well when it cannot
	 * decode one, and callers (e.g. Glide) then show their placeholder. */

	public Bitmap getFrameAtTime(long timeUs, int option) {
		return null;
	}

	public Bitmap getFrameAtTime(long timeUs) {
		return getFrameAtTime(timeUs, OPTION_CLOSEST_SYNC);
	}

	public Bitmap getFrameAtTime() {
		return getFrameAtTime(-1, OPTION_CLOSEST_SYNC);
	}

	public Bitmap getScaledFrameAtTime(long timeUs, int option, int dstWidth, int dstHeight) {
		return null;
	}

	public static final int OPTION_PREVIOUS_SYNC = 0x00;
	public static final int OPTION_NEXT_SYNC = 0x01;
	public static final int OPTION_CLOSEST_SYNC = 0x02;
	public static final int OPTION_CLOSEST = 0x03;
}
