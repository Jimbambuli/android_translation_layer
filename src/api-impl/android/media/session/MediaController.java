package android.media.session;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaMetadata;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
