package android.media.session;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaMetadata;
import android.media.Rating;
import android.media.session.MediaSessionManager.RemoteUserInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.os.ResultReceiver;
import android.text.TextUtils;
import android.view.KeyEvent;
import java.util.List;

public class MediaController {

	private final MediaSession session;
	private final MediaSession.Token mToken;
	private final Context mContext;
	private final TransportControls mTransportControls = new TransportControls();

	public MediaController(Context context, MediaSession.Token token) {
		mToken = token;
		mContext = context;
		session = token != null ? token.session : null;
	}

	/* AOSP MediaController.getTransportControls() */
	public TransportControls getTransportControls() {
		return mTransportControls;
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

	/* AOSP MediaController.getPlaybackInfo(): a session of this process plays locally */
	public PlaybackInfo getPlaybackInfo() {
		return new PlaybackInfo(PlaybackInfo.PLAYBACK_TYPE_LOCAL, 2 /* VOLUME_CONTROL_ABSOLUTE */, 100, 100, new AudioAttributes.Builder().build(), null);
	}

	/* AOSP MediaController.registerCallback(): the session tells the callback about its changes on the
	 * handler's thread (AOSP over binder, here the session is in the same process) */
	public void registerCallback(Callback callback) {
		registerCallback(callback, null);
	}

	public void registerCallback(Callback callback, Handler handler) {
		if (callback == null) {
			throw new IllegalArgumentException("callback must not be null");
		}
		if (handler == null) {
			Looper looper = Looper.myLooper();
			handler = new Handler(looper != null ? looper : Looper.getMainLooper());
		}
		if (session != null)
			session.addControllerCallback(callback, handler);
	}

	public void unregisterCallback(Callback callback) {
		if (callback == null) {
			throw new IllegalArgumentException("callback must not be null");
		}
		if (session != null)
			session.removeControllerCallback(callback);
	}

	/* AOSP MediaController.TransportControls: the session is in this process, so the actions go to its callback
	 * handler directly (AOSP: over binder through MediaSessionRecord), with this app as the controller */
	public final class TransportControls {
		private TransportControls() {}

		private void send(int what, Object obj, Bundle extras) {
			if (session != null)
				session.postToCallback(new RemoteUserInfo(mContext.getPackageName(), Process.myPid(), Process.myUid()), what, obj, extras, 0);
		}

		public void prepare() {
			send(MediaSession.CallbackMessageHandler.MSG_PREPARE, null, null);
		}

		public void prepareFromMediaId(String mediaId, Bundle extras) {
			if (TextUtils.isEmpty(mediaId)) {
				throw new IllegalArgumentException("You must specify a non-empty String for prepareFromMediaId.");
			}
			send(MediaSession.CallbackMessageHandler.MSG_PREPARE_MEDIA_ID, mediaId, extras);
		}

		public void prepareFromSearch(String query, Bundle extras) {
			if (query == null) {
				// This is to remain compatible with
				// INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH
				query = "";
			}
			send(MediaSession.CallbackMessageHandler.MSG_PREPARE_SEARCH, query, extras);
		}

		public void prepareFromUri(Uri uri, Bundle extras) {
			if (uri == null || Uri.EMPTY.equals(uri)) {
				throw new IllegalArgumentException("You must specify a non-empty Uri for prepareFromUri.");
			}
			send(MediaSession.CallbackMessageHandler.MSG_PREPARE_URI, uri, extras);
		}

		public void play() {
			send(MediaSession.CallbackMessageHandler.MSG_PLAY, null, null);
		}

		public void playFromMediaId(String mediaId, Bundle extras) {
			if (TextUtils.isEmpty(mediaId)) {
				throw new IllegalArgumentException("You must specify a non-empty String for playFromMediaId.");
			}
			send(MediaSession.CallbackMessageHandler.MSG_PLAY_MEDIA_ID, mediaId, extras);
		}

		public void playFromSearch(String query, Bundle extras) {
			if (query == null) {
				// This is to remain compatible with
				// INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH
				query = "";
			}
			send(MediaSession.CallbackMessageHandler.MSG_PLAY_SEARCH, query, extras);
		}

		public void playFromUri(Uri uri, Bundle extras) {
			if (uri == null || Uri.EMPTY.equals(uri)) {
				throw new IllegalArgumentException("You must specify a non-empty Uri for playFromUri.");
			}
			send(MediaSession.CallbackMessageHandler.MSG_PLAY_URI, uri, extras);
		}

		public void skipToQueueItem(long id) {
			send(MediaSession.CallbackMessageHandler.MSG_SKIP_TO_ITEM, id, null);
		}

		public void pause() {
			send(MediaSession.CallbackMessageHandler.MSG_PAUSE, null, null);
		}

		public void stop() {
			send(MediaSession.CallbackMessageHandler.MSG_STOP, null, null);
		}

		public void seekTo(long pos) {
			send(MediaSession.CallbackMessageHandler.MSG_SEEK_TO, pos, null);
		}

		public void fastForward() {
			send(MediaSession.CallbackMessageHandler.MSG_FAST_FORWARD, null, null);
		}

		public void skipToNext() {
			send(MediaSession.CallbackMessageHandler.MSG_NEXT, null, null);
		}

		public void rewind() {
			send(MediaSession.CallbackMessageHandler.MSG_REWIND, null, null);
		}

		public void skipToPrevious() {
			send(MediaSession.CallbackMessageHandler.MSG_PREVIOUS, null, null);
		}

		public void setRating(Rating rating) {
			send(MediaSession.CallbackMessageHandler.MSG_RATE, rating, null);
		}

		public void setPlaybackSpeed(float speed) {
			if (speed == 0.0f) {
				throw new IllegalArgumentException("speed must not be zero");
			}
			send(MediaSession.CallbackMessageHandler.MSG_SET_PLAYBACK_SPEED, speed, null);
		}

		public void sendCustomAction(PlaybackState.CustomAction customAction, Bundle args) {
			if (customAction == null) {
				throw new IllegalArgumentException("CustomAction cannot be null.");
			}
			sendCustomAction(customAction.getAction(), args);
		}

		public void sendCustomAction(String action, Bundle args) {
			if (TextUtils.isEmpty(action)) {
				throw new IllegalArgumentException("CustomAction cannot be null.");
			}
			send(MediaSession.CallbackMessageHandler.MSG_CUSTOM_ACTION, action, args);
		}
	}

	/* AOSP MediaController.Callback */
	public abstract static class Callback {
		public void onSessionDestroyed() {}

		public void onSessionEvent(String event, Bundle extras) {}

		public void onPlaybackStateChanged(PlaybackState state) {}

		public void onMetadataChanged(MediaMetadata metadata) {}

		public void onQueueChanged(List<MediaSession.QueueItem> queue) {}

		public void onQueueTitleChanged(CharSequence title) {}

		public void onExtrasChanged(Bundle extras) {}

		public void onAudioInfoChanged(PlaybackInfo info) {}
	}

	/* AOSP MediaController.PlaybackInfo, without the Parcelable part */
	public static final class PlaybackInfo {
		public static final int PLAYBACK_TYPE_LOCAL = 1;
		public static final int PLAYBACK_TYPE_REMOTE = 2;

		private final int mPlaybackType;
		private final int mVolumeControl;
		private final int mMaxVolume;
		private final int mCurrentVolume;
		private final AudioAttributes mAudioAttrs;
		private final String mVolumeControlId;

		public PlaybackInfo(int type, int control, int max, int current, AudioAttributes attrs, String volumeControlId) {
			mPlaybackType = type;
			mVolumeControl = control;
			mMaxVolume = max;
			mCurrentVolume = current;
			mAudioAttrs = attrs;
			mVolumeControlId = volumeControlId;
		}

		public int getPlaybackType() {
			return mPlaybackType;
		}

		public int getVolumeControl() {
			return mVolumeControl;
		}

		public int getMaxVolume() {
			return mMaxVolume;
		}

		public int getCurrentVolume() {
			return mCurrentVolume;
		}

		public AudioAttributes getAudioAttributes() {
			return mAudioAttrs;
		}

		public String getVolumeControlId() {
			return mVolumeControlId;
		}
	}
}
