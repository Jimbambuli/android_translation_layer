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
import android.os.Looper;
import android.os.Message;
import android.os.ResultReceiver;
import java.util.List;

public class MediaSession {

	private List<QueueItem> queue;
	private CharSequence queue_title;
	private MediaMetadata metadata;
	private PlaybackState playback_state;
	private CallbackMessageHandler mCallback;
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

	public void setCallback(Callback callback) {
		setCallback(callback, null);
	}

	/* AOSP MediaSession.setCallback: transport controls are posted to the callback's looper */
	public void setCallback(Callback callback, Handler handler) {
		synchronized (this) {
			if (mCallback != null) {
				mCallback.removeCallbacksAndMessages(null);
			}
			if (callback == null) {
				mCallback = null;
				nativeSetCallback(null);
				return;
			}
			Looper looper = handler != null ? handler.getLooper() : Looper.myLooper();
			mCallback = new CallbackMessageHandler(looper, callback);
			nativeSetCallback(this);
		}
	}

	/* called by the MPRIS bridge, as AOSP's CallbackStub calls them for media controllers */
	@Export
	void dispatchPlay() {
		postToCallback(CallbackMessageHandler.MSG_PLAY, null);
	}

	@Export
	void dispatchPause() {
		postToCallback(CallbackMessageHandler.MSG_PAUSE, null);
	}

	@Export
	void dispatchStop() {
		postToCallback(CallbackMessageHandler.MSG_STOP, null);
	}

	@Export
	void dispatchNext() {
		postToCallback(CallbackMessageHandler.MSG_NEXT, null);
	}

	@Export
	void dispatchPrevious() {
		postToCallback(CallbackMessageHandler.MSG_PREVIOUS, null);
	}

	@Export
	void dispatchSeekTo(long pos) {
		postToCallback(CallbackMessageHandler.MSG_SEEK_TO, pos);
	}

	void postToCallback(int what, Object obj) {
		synchronized (this) {
			if (mCallback != null) {
				mCallback.post(what, obj);
			}
		}
	}

	public void setMediaButtonReceiver(PendingIntent pendingIntent) {}

	public void setActive(boolean active) {}

	public void setPlaybackState(PlaybackState state) {
		playback_state = state;
		updateNativeState();
	}

	public void setMetadata(MediaMetadata metadata) {
		this.metadata = metadata;
		updateNativeState();
	}

	/* Hands the state to the MPRIS bridge. The fields are the ones Android's SystemUI media
	 * controls show (MediaDataManager): display title or title, artist, album, duration and the
	 * art URI. Without metadata, the active queue item describes the track. */
	private void updateNativeState() {
		PlaybackState state = playback_state;
		if (state == null)
			return;
		String title = null;
		String artist = null;
		String album = null;
		String artUrl = null;
		long duration = 0;
		if (metadata != null) {
			title = metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE);
			if (title == null)
				title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE);
			artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST);
			if (artist == null)
				artist = metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE);
			album = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM);
			duration = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION);
			for (String key : new String[] {MediaMetadata.METADATA_KEY_ART_URI, MediaMetadata.METADATA_KEY_ALBUM_ART_URI, MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI}) {
				artUrl = metadata.getString(key);
				if (artUrl != null)
					break;
			}
		} else if (queue != null) {
			for (QueueItem item : queue) {
				if (item.id == state.getActiveQueueItemId()) {
					CharSequence t = item.description.getTitle();
					title = t == null ? null : t.toString();
					CharSequence st = item.description.getSubtitle();
					artist = st == null ? null : st.toString();
					artUrl = item.description.getIconUri() == null ? null : item.description.getIconUri().toString();
					break;
				}
			}
		}
		/* MPRIS knows Playing, Paused and Stopped; buffering and skipping count as playing (PlaybackState.isActive) */
		int status = state.getState() == PlaybackState.STATE_PAUSED ? MPRIS_PAUSED : state.isActive() ? MPRIS_PLAYING : MPRIS_STOPPED;
		nativeSetState(status, state.getActions(), state.getPosition(), state.getLastPositionUpdateTime(), state.getPlaybackSpeed(),
		               title, artist, album, duration, artUrl);
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
	private static final int MPRIS_STOPPED = 0;
	private static final int MPRIS_PAUSED = 1;
	private static final int MPRIS_PLAYING = 2;

	protected native void nativeSetState(int status, long actions, long position, long updateTime, float speed,
	                                     String title, String artist, String album, long duration, String artUrl);
	protected native void nativeSetCallback(MediaSession session);

	/* AOSP MediaSession.CallbackMessageHandler, with the transport controls the MPRIS bridge sends */
	private static class CallbackMessageHandler extends Handler {
		private static final int MSG_PLAY = 7;
		private static final int MSG_PAUSE = 12;
		private static final int MSG_STOP = 13;
		private static final int MSG_NEXT = 14;
		private static final int MSG_PREVIOUS = 15;
		private static final int MSG_SEEK_TO = 18;

		private MediaSession.Callback mCallback;

		CallbackMessageHandler(Looper looper, MediaSession.Callback callback) {
			super(looper);
			mCallback = callback;
		}

		void post(int what, Object obj) {
			Message msg = obtainMessage(what, obj);
			msg.setAsynchronous(true);
			sendMessage(msg);
		}

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case MSG_PLAY:
					mCallback.onPlay();
					break;
				case MSG_PAUSE:
					mCallback.onPause();
					break;
				case MSG_STOP:
					mCallback.onStop();
					break;
				case MSG_NEXT:
					mCallback.onSkipToNext();
					break;
				case MSG_PREVIOUS:
					mCallback.onSkipToPrevious();
					break;
				case MSG_SEEK_TO:
					mCallback.onSeekTo((Long)msg.obj);
					break;
			}
		}
	}

	public abstract static class Callback {
		public Callback() {}

		public void onCommand(@NonNull String command, @Nullable Bundle args, @Nullable ResultReceiver cb) {}

		public boolean onMediaButtonEvent(@NonNull Intent mediaButtonIntent) { return false; }

		public void onPrepare() {}

		public void onPrepareFromMediaId(String mediaId, Bundle extras) {}

		public void onPrepareFromSearch(String query, Bundle extras) {}

		public void onPrepareFromUri(Uri uri, Bundle extras) {}

		public void onPlay() {}

		public void onPlayFromSearch(String query, Bundle extras) {}

		public void onPlayFromMediaId(String mediaId, Bundle extras) {}

		public void onPlayFromUri(Uri uri, Bundle extras) {}

		public void onSkipToQueueItem(long id) {}

		public void onPause() {}

		public void onSkipToNext() {}

		public void onSkipToPrevious() {}

		public void onFastForward() {}

		public void onRewind() {}

		public void onStop() {}

		public void onSeekTo(long pos) {}

		//public void onSetRating(@NonNull Rating rating) {}

		public void onSetPlaybackSpeed(float speed) {}

		public void onCustomAction(@NonNull String action, @Nullable Bundle extras) {}
	}
}
