package android.media.session;

import android.content.Context;
import android.media.MediaMetadata;
import android.os.Bundle;
import android.os.ResultReceiver;
import java.util.List;

public class MediaController {

	private final MediaSession session;

	public MediaController(Context context, MediaSession.Token token) {
		session = token != null ? token.session : null;
	}

	public MediaMetadata getMetadata() {
		return session != null ? session.getMetadata() : null;
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
