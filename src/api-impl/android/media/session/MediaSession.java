package android.media.session;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.PendingIntent;
import android.atl.annotation.Export;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaDescription;
import android.media.MediaMetadata;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import java.util.List;

public class MediaSession {

	private List<QueueItem> queue;
	private CharSequence queue_title;
	private MediaMetadata metadata;
	private PlaybackState playback_state;
	private final Token token = new Token(this);

	/* only used within the process, so it refers to the session directly */
	public static final class Token {
		final MediaSession session;

		Token(MediaSession session) {
			this.session = session;
		}
	}

	public static class QueueItem {
		public static final int UNKNOWN_ID = -1;
		long id;
		MediaDescription description;

		public QueueItem(MediaDescription description, long id) {
			this.description = description;
			this.id = id;
		}

		public MediaDescription getDescription() {
			return description;
		}

		public long getQueueId() {
			return id;
		}
	}

	public MediaSession(Context context, String tag) {}

	/* API 29 */
	public MediaSession(Context context, String tag, android.os.Bundle sessionInfo) {
		this(context, tag);
	}

	public Token getSessionToken() {
		return token;
	}

	public void setFlags(int flags) {}

	public void setCallback(Callback callback, Handler handler) {
		nativeSetCallback(callback);
	}

	public void setCallback(Callback callback) {
		nativeSetCallback(callback);
	}

	public void setMediaButtonReceiver(PendingIntent pendingIntent) {}

	public void setActive(boolean active) {}

	public void setPlaybackState(PlaybackState state) {
		playback_state = state;
		String title = null;
		String subTitle = null;
		String artUrl = null;
		if (queue != null)
			for (QueueItem item : queue) {
				if (item.id == state.getActiveQueueItemId()) {
					CharSequence t = item.description.getTitle();
					title = t == null ? null : t.toString();
					CharSequence st = item.description.getSubtitle();
					subTitle = st == null ? null : st.toString();
					artUrl = item.description.getIconUri() == null ? null : item.description.getIconUri().toString();
					break;
				}
			}
		nativeSetState(state.getState(), state.getActions(), state.getPosition(), state.getLastPositionUpdateTime(), title, subTitle, artUrl);
	}

	public void setMetadata(MediaMetadata metadata) {
		this.metadata = metadata;
	}

	public void setQueue(List<QueueItem> queue) {
		this.queue = queue;
	}

	public void release() {}

	public void setPlaybackToLocal(AudioAttributes audioAttributes) {}

	public void setExtras(Bundle extras) {}

	public void setSessionActivity(PendingIntent pendingIntent) {}

	public boolean isActive() {
		return true;
	}

	public void setRatingType(int ratingType) {}

	public void setQueueTitle(CharSequence title) {
		queue_title = title;
	}

	List<QueueItem> getQueue() {
		return queue;
	}

	CharSequence getQueueTitle() {
		return queue_title;
	}

	MediaMetadata getMetadata() {
		return metadata;
	}

	PlaybackState getPlaybackState() {
		return playback_state;
	}
	protected native void nativeSetState(int state, long actions, long position, long updateTime, String title, String subTitle, String artUrl);
	protected native void nativeSetCallback(Callback callback);

	public abstract static class Callback {
		public Callback() {}

		public void onCommand(@NonNull String command, @Nullable Bundle args, @Nullable ResultReceiver cb) {}

		public boolean onMediaButtonEvent(@NonNull Intent mediaButtonIntent) { return false; }

		public void onPrepare() {}

		public void onPrepareFromMediaId(String mediaId, Bundle extras) {}

		public void onPrepareFromSearch(String query, Bundle extras) {}

		public void onPrepareFromUri(Uri uri, Bundle extras) {}

		@Export
		public void onPlay() {}

		public void onPlayFromSearch(String query, Bundle extras) {}

		public void onPlayFromMediaId(String mediaId, Bundle extras) {}

		public void onPlayFromUri(Uri uri, Bundle extras) {}

		public void onSkipToQueueItem(long id) {}

		@Export
		public void onPause() {}

		public void onSkipToNext() {}

		public void onSkipToPrevious() {}

		public void onFastForward() {}

		public void onRewind() {}

		public void onStop() {}

		@Export
		public void onSeekTo(long pos) {}

		//public void onSetRating(@NonNull Rating rating) {}

		public void onSetPlaybackSpeed(float speed) {}

		public void onCustomAction(@NonNull String action, @Nullable Bundle extras) {}
	}
}
