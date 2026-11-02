package android.media.session;

import android.content.Context;
import android.media.MediaMetadata;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.view.KeyEvent;
import java.util.List;

public class MediaController {

	private final MediaSession session;
	private final MediaSession.Token mToken;

	public MediaController(Context context, MediaSession.Token token) {
		mToken = token;
		session = token != null ? token.session : null;
	}

	public MediaSession.Token getSessionToken() {
		return mToken;
	}

	/* AOSP MediaController.dispatchMediaButtonEvent(): a media key for the session (onMediaButtonEvent()) */
	public boolean dispatchMediaButtonEvent(KeyEvent keyEvent) {
		if (keyEvent == null) {
			throw new IllegalArgumentException("KeyEvent may not be null");
		}
		if (!KeyEvent.isMediaSessionKey(keyEvent.getKeyCode())) {
			return false;
		}
		return session != null && session.dispatchMediaKeyEvent(keyEvent);
	}

	public MediaMetadata getMetadata() {
		return session != null ? session.getMetadata() : null;
	}

	public PlaybackState getPlaybackState() {
		return session != null ? session.getPlaybackState() : null;
	}

	public CharSequence getQueueTitle() {
		return session != null ? session.getQueueTitle() : null;
	}

	public List<MediaSession.QueueItem> getQueue() {
		return session != null ? session.getQueue() : null;
	}

	/* MediaSession.Callback has no onCommand() yet, so commands are dropped */
	public void sendCommand(String command, Bundle args, ResultReceiver cb) {}
}
