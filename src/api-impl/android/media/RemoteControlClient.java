package android.media;

import android.media.session.PlaybackState;

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

	public RemoteControlClient(PendingIntent mediaButtonIntent, android.os.Looper looper) {}

	public MetadataEditor editMetadata(boolean startEmpty) {
		return new MetadataEditor();
	}

	public void setPlaybackState(int state) {}

	public void setPlaybackState(int state, long timeInMs, float playbackSpeed) {}

	public void setTransportControlFlags(int transportControlFlags) {}

	public void setMetadataUpdateListener(OnMetadataUpdateListener l) {}

	public void setPlaybackPositionUpdateListener(OnPlaybackPositionUpdateListener l) {}

	public void setOnGetPlaybackPositionListener(OnGetPlaybackPositionListener l) {}

	/* AOSP RemoteControlClient.MetadataEditor extends MediaMetadataEditor */
	public class MetadataEditor extends MediaMetadataEditor {
		public final static int BITMAP_KEY_ARTWORK = 100;

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
	
    /* AOSP 16 API; no backing hardware/service in ATL */
    public Object clone() throws java.lang.CloneNotSupportedException { return null; }
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
    /* AOSP 16 API; no backing hardware/service in ATL */
    public android.media.session.MediaSession getMediaSession() { return null; }

    /* AOSP android/media/RemoteControlClient.java: public final static int PLAYSTATE_NONE */
    public final static int PLAYSTATE_NONE               = 0;

    /* AOSP android/media/RemoteControlClient.java: public final static int FLAG_KEY_MEDIA_PREVIOUS */
    public final static int FLAG_KEY_MEDIA_PREVIOUS = 1 << 0;

    /* AOSP android/media/RemoteControlClient.java: public final static int FLAG_KEY_MEDIA_REWIND */
    public final static int FLAG_KEY_MEDIA_REWIND = 1 << 1;

    /* AOSP android/media/RemoteControlClient.java: public final static int FLAG_KEY_MEDIA_PLAY */
    public final static int FLAG_KEY_MEDIA_PLAY = 1 << 2;

    /* AOSP android/media/RemoteControlClient.java: public final static int FLAG_KEY_MEDIA_PLAY_PAUSE */
    public final static int FLAG_KEY_MEDIA_PLAY_PAUSE = 1 << 3;

    /* AOSP android/media/RemoteControlClient.java: public final static int FLAG_KEY_MEDIA_PAUSE */
    public final static int FLAG_KEY_MEDIA_PAUSE = 1 << 4;

    /* AOSP android/media/RemoteControlClient.java: public final static int FLAG_KEY_MEDIA_STOP */
    public final static int FLAG_KEY_MEDIA_STOP = 1 << 5;

    /* AOSP android/media/RemoteControlClient.java: public final static int FLAG_KEY_MEDIA_FAST_FORWARD */
    public final static int FLAG_KEY_MEDIA_FAST_FORWARD = 1 << 6;

    /* AOSP android/media/RemoteControlClient.java: public final static int FLAG_KEY_MEDIA_NEXT */
    public final static int FLAG_KEY_MEDIA_NEXT = 1 << 7;

    /* AOSP android/media/RemoteControlClient.java: public final static int FLAG_KEY_MEDIA_POSITION_UPDATE */
    public final static int FLAG_KEY_MEDIA_POSITION_UPDATE = 1 << 8;

    /* AOSP android/media/RemoteControlClient.java: public final static int FLAG_KEY_MEDIA_RATING */
    public final static int FLAG_KEY_MEDIA_RATING = 1 << 9;

    /* AOSP android/media/RemoteControlClient.java: static int getRccStateFromState */
    static int getRccStateFromState(int state) {
        switch (state) {
            case PlaybackState.STATE_BUFFERING:
                return PLAYSTATE_BUFFERING;
            case PlaybackState.STATE_ERROR:
                return PLAYSTATE_ERROR;
            case PlaybackState.STATE_FAST_FORWARDING:
                return PLAYSTATE_FAST_FORWARDING;
            case PlaybackState.STATE_NONE:
                return PLAYSTATE_NONE;
            case PlaybackState.STATE_PAUSED:
                return PLAYSTATE_PAUSED;
            case PlaybackState.STATE_PLAYING:
                return PLAYSTATE_PLAYING;
            case PlaybackState.STATE_REWINDING:
                return PLAYSTATE_REWINDING;
            case PlaybackState.STATE_SKIPPING_TO_PREVIOUS:
                return PLAYSTATE_SKIPPING_BACKWARDS;
            case PlaybackState.STATE_SKIPPING_TO_NEXT:
                return PLAYSTATE_SKIPPING_FORWARDS;
            case PlaybackState.STATE_STOPPED:
                return PLAYSTATE_STOPPED;
            default:
                return -1;
        }
    }

    /* AOSP android/media/RemoteControlClient.java: static int getRccControlFlagsFromActions */
    static int getRccControlFlagsFromActions(long actions) {
        int rccFlags = 0;
        long action = 1;
        while (action <= actions && action < Integer.MAX_VALUE) {
            if ((action & actions) != 0) {
                rccFlags |= getRccFlagForAction(action);
            }
            action = action << 1;
        }
        return rccFlags;
    }

    /* AOSP android/media/RemoteControlClient.java: private static int getRccFlagForAction */
    private static int getRccFlagForAction(long action) {
        // We only care about the lower set of actions that can map to rcc
        // flags.
        int testAction = action < Integer.MAX_VALUE ? (int) action : 0;
        switch (testAction) {
            case (int) PlaybackState.ACTION_SKIP_TO_PREVIOUS:
                return FLAG_KEY_MEDIA_PREVIOUS;
            case (int) PlaybackState.ACTION_REWIND:
                return FLAG_KEY_MEDIA_REWIND;
            case (int) PlaybackState.ACTION_PLAY:
                return FLAG_KEY_MEDIA_PLAY;
            case (int) PlaybackState.ACTION_PLAY_PAUSE:
                return FLAG_KEY_MEDIA_PLAY_PAUSE;
            case (int) PlaybackState.ACTION_PAUSE:
                return FLAG_KEY_MEDIA_PAUSE;
            case (int) PlaybackState.ACTION_STOP:
                return FLAG_KEY_MEDIA_STOP;
            case (int) PlaybackState.ACTION_FAST_FORWARD:
                return FLAG_KEY_MEDIA_FAST_FORWARD;
            case (int) PlaybackState.ACTION_SKIP_TO_NEXT:
                return FLAG_KEY_MEDIA_NEXT;
            case (int) PlaybackState.ACTION_SEEK_TO:
                return FLAG_KEY_MEDIA_POSITION_UPDATE;
            case (int) PlaybackState.ACTION_SET_RATING:
                return FLAG_KEY_MEDIA_RATING;
        }
        return 0;
    }
}
