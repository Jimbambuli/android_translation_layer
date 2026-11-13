package android.media;

import android.app.PendingIntent;
import android.graphics.Bitmap;

/* AOSP android.media.RemoteControlClient (deprecated since API 21): the lock screen's and the media routes' view of a
 * player. ATL has no lock screen; the player is shown by its MediaSession (MPRIS), so the client keeps nothing. */
@Deprecated
public class RemoteControlClient {
	public static final int PLAYSTATE_STOPPED = 1;
	public static final int PLAYSTATE_PAUSED = 2;
	public static final int PLAYSTATE_PLAYING = 3;
	public static final int PLAYSTATE_FAST_FORWARDING = 4;
	public static final int PLAYSTATE_REWINDING = 5;
	public static final int PLAYSTATE_SKIPPING_FORWARDS = 6;
	public static final int PLAYSTATE_SKIPPING_BACKWARDS = 7;
	public static final int PLAYSTATE_BUFFERING = 8;
	public static final int PLAYSTATE_ERROR = 9;

	public RemoteControlClient(PendingIntent mediaButtonIntent) {}

	public MetadataEditor editMetadata(boolean startEmpty) {
		return new MetadataEditor();
	}

	public void setPlaybackState(int state) {}

	public void setPlaybackState(int state, long timeInMs, float playbackSpeed) {}

	public void setTransportControlFlags(int transportControlFlags) {}

	public void setMetadataUpdateListener(OnMetadataUpdateListener l) {}

	public void setPlaybackPositionUpdateListener(OnPlaybackPositionUpdateListener l) {}

	public void setOnGetPlaybackPositionListener(OnGetPlaybackPositionListener l) {}

	public class MetadataEditor {
		public MetadataEditor putString(int key, String value) {
			return this;
		}

		public MetadataEditor putLong(int key, long value) {
			return this;
		}

		public MetadataEditor putBitmap(int key, Bitmap bitmap) {
			return this;
		}

		public MetadataEditor putObject(int key, Object object) {
			return this;
		}

		public void addEditableKey(int key) {}

		public void removeEditableKeyBit(int key) {}

		public void clear() {}

		public void apply() {}
	}

	public interface OnMetadataUpdateListener {
		void onMetadataUpdate(int key, Object newValue);
	}

	public interface OnPlaybackPositionUpdateListener {
		void onPlaybackPositionUpdate(long newPositionMs);
	}

	public interface OnGetPlaybackPositionListener {
		long onGetPlaybackPosition();
	}
}
