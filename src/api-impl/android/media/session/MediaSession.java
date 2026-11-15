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
import android.media.Rating;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.ResultReceiver;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.ViewConfiguration;
import android.media.session.MediaSessionManager.RemoteUserInfo;
import java.util.ArrayList;
import java.util.List;

public class MediaSession {
	/* AOSP MediaSession.FLAG_HANDLES_MEDIA_BUTTONS/FLAG_HANDLES_TRANSPORT_CONTROLS (deprecated, always handled) */
	@Deprecated
	public static final int FLAG_HANDLES_MEDIA_BUTTONS = 1 << 0;
	@Deprecated
	public static final int FLAG_HANDLES_TRANSPORT_CONTROLS = 1 << 1;

	private List<QueueItem> queue;
	private CharSequence queue_title;
	private MediaMetadata metadata;
	private PlaybackState playback_state;
	private CallbackMessageHandler mCallback;
	private final Token token = new Token(this);
	private boolean mActive;
	/* AOSP MediaSession fields; the values MediaSessionRecord keeps for the controllers */
	private Context mContext;
	private Bundle mSessionInfo;
	private MediaController mController;
	private int mFlags;
	private int mRatingType;
	private PendingIntent mSessionActivity;
	private PendingIntent mMediaButtonReceiver;
	private Bundle mExtras;
	private AudioAttributes mAudioAttrs;
	private android.media.VolumeProvider mVolumeProvider;

	/* the controller behind the transport controls and media keys: the desktop's media controls (MPRIS)
	 * and keys stand in for SystemUI and the system (MediaSessionService) */
	private static final RemoteUserInfo SYSTEM_CONTROLLER = new RemoteUserInfo("android", 0, 1000 /*Process.SYSTEM_UID*/);

	/* MediaSessionStack's media button session: the session that played last gets the media keys */
	private static MediaSession sMediaButtonSession;

	static synchronized MediaSession getMediaButtonSession() {
		return sMediaButtonSession;
	}

	/* only used within the process, so it refers to the session directly */
	public static final class Token {
		final MediaSession session;

		Token(MediaSession session) {
			this.session = session;
		}
	
    /* AOSP 16 API; no backing hardware/service in ATL */
    public int describeContents() { return 0; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public void writeToParcel(android.os.Parcel arg0, int arg1) {  }
}

	public static class QueueItem implements android.os.Parcelable {
		public static final int UNKNOWN_ID = -1;
		long id;
		MediaDescription description;

		public QueueItem(MediaDescription description, long id) {
			this.description = description;
			this.id = id;
		}

		/* AOSP MediaSession.QueueItem(Parcel)/writeToParcel()/describeContents()/CREATOR */
		private QueueItem(android.os.Parcel in) {
			description = MediaDescription.CREATOR.createFromParcel(in);
			id = in.readLong();
		}

		@Override
		public void writeToParcel(android.os.Parcel dest, int flags) {
			description.writeToParcel(dest, flags);
			dest.writeLong(id);
		}

		@Override
		public int describeContents() {
			return 0;
		}

		public static final android.os.Parcelable.Creator<QueueItem> CREATOR = new android.os.Parcelable.Creator<QueueItem>() {
			@Override
			public QueueItem createFromParcel(android.os.Parcel p) {
				return new QueueItem(p);
			}

			@Override
			public QueueItem[] newArray(int size) {
				return new QueueItem[size];
			}
		};

		public MediaDescription getDescription() {
			return description;
		}

		public long getQueueId() {
			return id;
		}
	
}

	public MediaSession(Context context, String tag) {
		this(context, tag, null);
	}

	/* API 29 */
	/* AOSP MediaSession(Context, String, Bundle): the session and its own controller (MediaSessionManager.createSession
	 * and the binder are the session itself here) */
	public MediaSession(Context context, String tag, android.os.Bundle sessionInfo) {
		if (context == null) {
			throw new IllegalArgumentException("context cannot be null.");
		}
		if (android.text.TextUtils.isEmpty(tag)) {
			throw new IllegalArgumentException("tag cannot be null or empty");
		}
		mContext = context;
		mSessionInfo = sessionInfo;
		mController = new MediaController(context, token);
	}

	/* AOSP MediaSession.getController() */
	public @NonNull MediaController getController() {
		return mController;
	}

	public Token getSessionToken() {
		return token;
	}

	/* AOSP MediaSession.setFlags() -> MediaSessionRecord.setFlags(): kept for MediaController.getFlags() */
	public void setFlags(int flags) {
		mFlags = flags;
	}

	public void setCallback(Callback callback) {
		setCallback(callback, null);
	}

	/* AOSP MediaSession.setCallback: transport controls are posted to the callback's looper */
	public void setCallback(Callback callback, Handler handler) {
		synchronized (this) {
			if (mCallback != null) {
				// We're updating the callback, clear the session from the old one.
				mCallback.mCallback.mSession = null;
				mCallback.removeCallbacksAndMessages(null);
			}
			if (callback == null) {
				mCallback = null;
				nativeSetCallback(null);
				return;
			}
			Looper looper = handler != null ? handler.getLooper() : Looper.myLooper();
			callback.mSession = this;
			mCallback = new CallbackMessageHandler(looper, callback);
			nativeSetCallback(this);
		}
	}

	/* AOSP MediaSession.getCurrentControllerInfo() */
	public final @NonNull RemoteUserInfo getCurrentControllerInfo() {
		if (mCallback == null || mCallback.mCurrentControllerInfo == null) {
			throw new IllegalStateException(
			    "This should be called inside of MediaSession.Callback methods");
		}
		return mCallback.mCurrentControllerInfo;
	}

	void dispatchMediaButton(RemoteUserInfo caller, Intent mediaButtonIntent) {
		postToCallback(caller, CallbackMessageHandler.MSG_MEDIA_BUTTON, mediaButtonIntent, 0);
	}

	void dispatchMediaButtonDelayed(RemoteUserInfo info, Intent mediaButtonIntent, long delay) {
		postToCallback(info, CallbackMessageHandler.MSG_PLAY_PAUSE_KEY_DOUBLE_TAP_TIMEOUT, mediaButtonIntent, delay);
	}

	/* MediaSessionRecord.sendMediaButton(): a media key reaches the session as an ACTION_MEDIA_BUTTON intent
	 * with the key event, which Callback.onMediaButtonEvent() handles */
	boolean dispatchMediaKeyEvent(KeyEvent keyEvent) {
		synchronized (this) {
			if (mCallback == null)
				return false;
		}
		Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
		mediaButtonIntent.putExtra(Intent.EXTRA_KEY_EVENT, keyEvent);
		dispatchMediaButton(SYSTEM_CONTROLLER, mediaButtonIntent);
		return true;
	}

	/* called by the MPRIS bridge, as AOSP's CallbackStub calls them for media controllers */
	@Export
	void dispatchPlay() {
		postToCallback(SYSTEM_CONTROLLER, CallbackMessageHandler.MSG_PLAY, null, 0);
	}

	@Export
	void dispatchPause() {
		postToCallback(SYSTEM_CONTROLLER, CallbackMessageHandler.MSG_PAUSE, null, 0);
	}

	@Export
	void dispatchStop() {
		postToCallback(SYSTEM_CONTROLLER, CallbackMessageHandler.MSG_STOP, null, 0);
	}

	@Export
	void dispatchNext() {
		postToCallback(SYSTEM_CONTROLLER, CallbackMessageHandler.MSG_NEXT, null, 0);
	}

	@Export
	void dispatchPrevious() {
		postToCallback(SYSTEM_CONTROLLER, CallbackMessageHandler.MSG_PREVIOUS, null, 0);
	}

	@Export
	void dispatchSeekTo(long pos) {
		postToCallback(SYSTEM_CONTROLLER, CallbackMessageHandler.MSG_SEEK_TO, pos, 0);
	}

	void postToCallback(RemoteUserInfo caller, int what, Object obj, long delayMs) {
		postToCallback(caller, what, obj, null, delayMs);
	}

	/* AOSP MediaSession.postToCallback(): MediaController.TransportControls of this process call it directly,
	 * where AOSP goes through MediaSessionRecord and the session's CallbackStub */
	void postToCallback(RemoteUserInfo caller, int what, Object obj, Bundle data, long delayMs) {
		synchronized (this) {
			if (mCallback != null) {
				mCallback.post(caller, what, obj, data, delayMs);
			}
		}
	}

	/* AOSP MediaSession.setMediaButtonReceiver(): kept like MediaSessionRecord.setMediaButtonReceiver(); ATL routes
	 * no media buttons to inactive sessions (MPRIS handles the keys) */
	public void setMediaButtonReceiver(PendingIntent pendingIntent) {
		mMediaButtonReceiver = pendingIntent;
	}

	/* API 31: the receiver that gets the media buttons while the session is inactive; ATL has no media button routing
	 * to apps (MPRIS handles the keys), so it is only kept */
	private android.content.ComponentName mMediaButtonBroadcastReceiver;

	public void setMediaButtonBroadcastReceiver(android.content.ComponentName broadcastReceiver) {
		mMediaButtonBroadcastReceiver = broadcastReceiver;
	}

	/* AOSP MediaSession.setActive(): only an active session gets the media keys */
	public void setActive(boolean active) {
		if (mActive == active)
			return;
		mActive = active;
		updateMediaButtonSession();
	}

	public void setPlaybackState(PlaybackState state) {
		playback_state = state;
		updateNativeState();
		updateMediaButtonSession();
		notifyControllers(MSG_CONTROLLER_PLAYBACK_STATE, state);
	}

	/* the callbacks of this session's MediaControllers, which AOSP's MediaSessionRecord calls over
	 * binder (pushPlaybackStateUpdate() and so on) */
	private final ArrayList<Pair<MediaController.Callback, Handler>> controllerCallbacks = new ArrayList<>();
	private static final int MSG_CONTROLLER_PLAYBACK_STATE = 1;
	private static final int MSG_CONTROLLER_METADATA = 2;
	private static final int MSG_CONTROLLER_QUEUE = 3;
	private static final int MSG_CONTROLLER_QUEUE_TITLE = 4;
	private static final int MSG_CONTROLLER_EXTRAS = 5;
	private static final int MSG_CONTROLLER_DESTROYED = 6;
	private static final int MSG_CONTROLLER_EVENT = 7;
	private static final int MSG_CONTROLLER_AUDIO_INFO = 8;

	void addControllerCallback(MediaController.Callback callback, Handler handler) {
		synchronized (controllerCallbacks) {
			for (Pair<MediaController.Callback, Handler> p : controllerCallbacks)
				if (p.first == callback)
					return;
			controllerCallbacks.add(new Pair<>(callback, handler));
		}
	}

	void removeControllerCallback(MediaController.Callback callback) {
		synchronized (controllerCallbacks) {
			for (int i = 0; i < controllerCallbacks.size(); i++) {
				if (controllerCallbacks.get(i).first == callback) {
					controllerCallbacks.remove(i);
					return;
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void notifyControllers(final int what, final Object arg) {
		ArrayList<Pair<MediaController.Callback, Handler>> callbacks;
		synchronized (controllerCallbacks) {
			callbacks = new ArrayList<>(controllerCallbacks);
		}
		for (final Pair<MediaController.Callback, Handler> p : callbacks) {
			p.second.post(new Runnable() {
				@Override
				public void run() {
					switch (what) {
						case MSG_CONTROLLER_PLAYBACK_STATE: p.first.onPlaybackStateChanged((PlaybackState)arg); break;
						case MSG_CONTROLLER_METADATA: p.first.onMetadataChanged((MediaMetadata)arg); break;
						case MSG_CONTROLLER_QUEUE: p.first.onQueueChanged((List<QueueItem>)arg); break;
						case MSG_CONTROLLER_QUEUE_TITLE: p.first.onQueueTitleChanged((CharSequence)arg); break;
						case MSG_CONTROLLER_EXTRAS: p.first.onExtrasChanged((Bundle)arg); break;
						case MSG_CONTROLLER_DESTROYED: p.first.onSessionDestroyed(); break;
						case MSG_CONTROLLER_EVENT: p.first.onSessionEvent(((Pair<String, Bundle>)arg).first, ((Pair<String, Bundle>)arg).second); break;
						case MSG_CONTROLLER_AUDIO_INFO: p.first.onAudioInfoChanged((MediaController.PlaybackInfo)arg); break;
					}
				}
			});
		}
	}

	/* MediaSessionStack.updateMediaButtonSessionIfNeeded(): the active session that plays becomes the
	 * media button session and stays it until another one plays or it is gone */
	private void updateMediaButtonSession() {
		synchronized (MediaSession.class) {
			if (mActive && playback_state != null && playback_state.isActive())
				sMediaButtonSession = this;
			else if (!mActive && sMediaButtonSession == this)
				sMediaButtonSession = null;
		}
	}

	public void setMetadata(MediaMetadata metadata) {
		this.metadata = metadata;
		updateNativeState();
		notifyControllers(MSG_CONTROLLER_METADATA, metadata);
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
		notifyControllers(MSG_CONTROLLER_QUEUE, queue);
	}

	/* AOSP MediaSession.release() */
	public void release() {
		setCallback(null);
		mActive = false;
		updateMediaButtonSession();
		notifyControllers(MSG_CONTROLLER_DESTROYED, null);
		synchronized (controllerCallbacks) {
			controllerCallbacks.clear();
		}
	}

	/* AOSP MediaSession.setPlaybackToLocal() -> MediaSessionRecord.setPlaybackToLocal(): local volume handling with
	 * these attributes; the controllers hear onAudioInfoChanged() */
	public void setPlaybackToLocal(AudioAttributes audioAttributes) {
		if (audioAttributes == null) {
			throw new IllegalArgumentException("Attributes cannot be null for local playback.");
		}
		synchronized (this) {
			mAudioAttrs = audioAttributes;
			mVolumeProvider = null;
		}
		notifyControllers(MSG_CONTROLLER_AUDIO_INFO, getPlaybackInfo());
	}

	/* AOSP MediaSession.setPlaybackToRemote() -> MediaSessionRecord.setPlaybackToRemote(): the volume belongs to
	 * the provider; volume changes from controllers go to it (onSetVolumeTo/onAdjustVolume) */
	public void setPlaybackToRemote(@NonNull android.media.VolumeProvider volumeProvider) {
		if (volumeProvider == null) {
			throw new IllegalArgumentException("volumeProvider may not be null!");
		}
		synchronized (this) {
			mVolumeProvider = volumeProvider;
		}
		notifyControllers(MSG_CONTROLLER_AUDIO_INFO, getPlaybackInfo());
	}

	/* AOSP MediaSession.sendSessionEvent() -> MediaSessionRecord.pushEvent(): to all controllers' onSessionEvent() */
	public void sendSessionEvent(@NonNull String event, @Nullable Bundle extras) {
		if (android.text.TextUtils.isEmpty(event)) {
			throw new IllegalArgumentException("event cannot be null or empty");
		}
		notifyControllers(MSG_CONTROLLER_EVENT, new Pair<String, Bundle>(event, extras));
	}

	public void setExtras(Bundle extras) {
		mExtras = extras;
		notifyControllers(MSG_CONTROLLER_EXTRAS, extras);
	}

	/* AOSP MediaSession.setSessionActivity(): kept for MediaController.getSessionActivity() */
	public void setSessionActivity(PendingIntent pendingIntent) {
		mSessionActivity = pendingIntent;
	}

	public boolean isActive() {
		return mActive;
	}

	/* AOSP MediaSession.setRatingType(): kept for MediaController.getRatingType() */
	public void setRatingType(int ratingType) {
		mRatingType = ratingType;
	}

	public void setQueueTitle(CharSequence title) {
		queue_title = title;
		notifyControllers(MSG_CONTROLLER_QUEUE_TITLE, title);
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

	/* the session's side of AOSP ISessionController (MediaSessionRecord.ControllerStub) */
	String getPackageName() {
		return mContext != null ? mContext.getPackageName() : null;
	}

	Bundle getSessionInfo() {
		return mSessionInfo;
	}

	Bundle getExtras() {
		return mExtras;
	}

	long getFlags() {
		return mFlags;
	}

	int getRatingType() {
		return mRatingType;
	}

	PendingIntent getSessionActivity() {
		return mSessionActivity;
	}

	/* MediaSessionRecord.getVolumeAttributes(): remote playback reports the provider, local playback the stream
	 * volume of the attributes (AudioManager.getStreamVolume/getStreamMaxVolume) */
	MediaController.PlaybackInfo getPlaybackInfo() {
		android.media.VolumeProvider vp;
		AudioAttributes attrs;
		synchronized (this) {
			vp = mVolumeProvider;
			attrs = mAudioAttrs != null ? mAudioAttrs : new AudioAttributes.Builder().build();
		}
		if (vp != null) {
			return new MediaController.PlaybackInfo(MediaController.PlaybackInfo.PLAYBACK_TYPE_REMOTE, vp.getVolumeControl(),
			                                        vp.getMaxVolume(), vp.getCurrentVolume(), attrs, vp.getVolumeControlId());
		}
		int stream = AudioAttributes.toLegacyStreamType(attrs);
		android.media.AudioManager am = getAudioManager();
		int max = am != null ? am.getStreamMaxVolume(stream) : 100;
		int current = am != null ? am.getStreamVolume(stream) : 100;
		return new MediaController.PlaybackInfo(MediaController.PlaybackInfo.PLAYBACK_TYPE_LOCAL,
		                                        2 /* VolumeProvider.VOLUME_CONTROL_ABSOLUTE */, max, current, attrs, null);
	}

	private android.media.AudioManager getAudioManager() {
		return mContext != null ? (android.media.AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE) : null;
	}

	/* MediaSessionRecord.setVolumeTo(): remote -> VolumeProvider.onSetVolumeTo() (unless fixed), local -> the stream */
	void setVolumeTo(int value, int flags) {
		android.media.VolumeProvider vp;
		AudioAttributes attrs;
		synchronized (this) {
			vp = mVolumeProvider;
			attrs = mAudioAttrs != null ? mAudioAttrs : new AudioAttributes.Builder().build();
		}
		if (vp != null) {
			if (vp.getVolumeControl() != android.media.VolumeProvider.VOLUME_CONTROL_ABSOLUTE)
				return;
			value = Math.max(0, Math.min(value, vp.getMaxVolume()));
			vp.onSetVolumeTo(value);
			return;
		}
		android.media.AudioManager am = getAudioManager();
		if (am != null)
			am.setStreamVolume(AudioAttributes.toLegacyStreamType(attrs), value, flags);
	}

	/* MediaSessionRecord.adjustVolume(): remote -> VolumeProvider.onAdjustVolume() (unless fixed), local -> the stream */
	void adjustVolume(int direction, int flags) {
		android.media.VolumeProvider vp;
		AudioAttributes attrs;
		synchronized (this) {
			vp = mVolumeProvider;
			attrs = mAudioAttrs != null ? mAudioAttrs : new AudioAttributes.Builder().build();
		}
		if (vp != null) {
			if (vp.getVolumeControl() == android.media.VolumeProvider.VOLUME_CONTROL_FIXED)
				return;
			vp.onAdjustVolume(direction);
			return;
		}
		android.media.AudioManager am = getAudioManager();
		if (am != null)
			am.adjustStreamVolume(AudioAttributes.toLegacyStreamType(attrs), direction, flags);
	}
	private static final int MPRIS_STOPPED = 0;
	private static final int MPRIS_PAUSED = 1;
	private static final int MPRIS_PLAYING = 2;

	protected native void nativeSetState(int status, long actions, long position, long updateTime, float speed,
	                                     String title, String artist, String album, long duration, String artUrl);
	protected native void nativeSetCallback(MediaSession session);

	/* AOSP MediaSession.CallbackMessageHandler, with the transport controls the MPRIS bridge sends and
	 * the media buttons */
	class CallbackMessageHandler extends Handler {
		static final int MSG_COMMAND = 1;
		static final int MSG_MEDIA_BUTTON = 2;
		static final int MSG_PREPARE = 3;
		static final int MSG_PREPARE_MEDIA_ID = 4;
		static final int MSG_PREPARE_SEARCH = 5;
		static final int MSG_PREPARE_URI = 6;
		static final int MSG_PLAY = 7;
		static final int MSG_PLAY_MEDIA_ID = 8;
		static final int MSG_PLAY_SEARCH = 9;
		static final int MSG_PLAY_URI = 10;
		static final int MSG_SKIP_TO_ITEM = 11;
		static final int MSG_PAUSE = 12;
		static final int MSG_STOP = 13;
		static final int MSG_NEXT = 14;
		static final int MSG_PREVIOUS = 15;
		static final int MSG_FAST_FORWARD = 16;
		static final int MSG_REWIND = 17;
		static final int MSG_SEEK_TO = 18;
		static final int MSG_RATE = 19;
		static final int MSG_SET_PLAYBACK_SPEED = 20;
		static final int MSG_CUSTOM_ACTION = 21;
		static final int MSG_PLAY_PAUSE_KEY_DOUBLE_TAP_TIMEOUT = 24;

		private MediaSession.Callback mCallback;
		private RemoteUserInfo mCurrentControllerInfo;

		CallbackMessageHandler(Looper looper, MediaSession.Callback callback) {
			super(looper);
			mCallback = callback;
			mCallback.mHandler = this;
		}

		void post(RemoteUserInfo caller, int what, Object obj, Bundle data, long delayMs) {
			Pair<RemoteUserInfo, Object> objWithCaller = Pair.create(caller, obj);
			Message msg = obtainMessage(what, objWithCaller);
			msg.setAsynchronous(true);
			msg.setData(data);
			if (delayMs > 0) {
				sendMessageDelayed(msg, delayMs);
			} else {
				sendMessage(msg);
			}
		}

		@Override
		@SuppressWarnings("unchecked")
		public void handleMessage(Message msg) {
			mCurrentControllerInfo = ((Pair<RemoteUserInfo, Object>)msg.obj).first;

			Object obj = ((Pair<RemoteUserInfo, Object>)msg.obj).second;

			switch (msg.what) {
				case MSG_COMMAND:
					Command cmd = (Command)obj;
					mCallback.onCommand(cmd.command, cmd.extras, cmd.stub);
					break;
				case MSG_MEDIA_BUTTON:
					mCallback.onMediaButtonEvent((Intent)obj);
					break;
				case MSG_PREPARE:
					mCallback.onPrepare();
					break;
				case MSG_PREPARE_MEDIA_ID:
					mCallback.onPrepareFromMediaId((String)obj, msg.getData());
					break;
				case MSG_PREPARE_SEARCH:
					mCallback.onPrepareFromSearch((String)obj, msg.getData());
					break;
				case MSG_PREPARE_URI:
					mCallback.onPrepareFromUri((Uri)obj, msg.getData());
					break;
				case MSG_PLAY:
					mCallback.onPlay();
					break;
				case MSG_PLAY_MEDIA_ID:
					mCallback.onPlayFromMediaId((String)obj, msg.getData());
					break;
				case MSG_PLAY_SEARCH:
					mCallback.onPlayFromSearch((String)obj, msg.getData());
					break;
				case MSG_PLAY_URI:
					mCallback.onPlayFromUri((Uri)obj, msg.getData());
					break;
				case MSG_SKIP_TO_ITEM:
					mCallback.onSkipToQueueItem((Long)obj);
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
				case MSG_FAST_FORWARD:
					mCallback.onFastForward();
					break;
				case MSG_REWIND:
					mCallback.onRewind();
					break;
				case MSG_SEEK_TO:
					mCallback.onSeekTo((Long)obj);
					break;
				case MSG_RATE:
					mCallback.onSetRating((Rating)obj);
					break;
				case MSG_SET_PLAYBACK_SPEED:
					mCallback.onSetPlaybackSpeed((Float)obj);
					break;
				case MSG_CUSTOM_ACTION:
					mCallback.onCustomAction((String)obj, msg.getData());
					break;
				case MSG_PLAY_PAUSE_KEY_DOUBLE_TAP_TIMEOUT:
					mCallback.handleMediaPlayPauseKeySingleTapIfPending();
					break;
			}
			mCurrentControllerInfo = null;
		}
	}

	/* AOSP MediaSession.Command */
	static final class Command {
		final String command;
		final Bundle extras;
		final ResultReceiver stub;

		Command(String command, Bundle extras, ResultReceiver stub) {
			this.command = command;
			this.extras = extras;
			this.stub = stub;
		}
	}

	/* AOSP MediaSession.dispatchCommand() (CallbackStub.onCommand) */
	void dispatchCommand(RemoteUserInfo caller, String command, Bundle args, ResultReceiver resultCb) {
		postToCallback(caller, CallbackMessageHandler.MSG_COMMAND, new Command(command, args, resultCb), 0);
	}

	public abstract static class Callback {
		private MediaSession mSession;
		private CallbackMessageHandler mHandler;
		private boolean mMediaPlayPauseKeyPending;

		public Callback() {}

		public void onCommand(@NonNull String command, @Nullable Bundle args, @Nullable ResultReceiver cb) {}

		/* AOSP MediaSession.Callback.onMediaButtonEvent(): the media keys become transport controls, two
		 * taps of play/pause within the double tap timeout skip to the next item */
		public boolean onMediaButtonEvent(@NonNull Intent mediaButtonIntent) {
			if (mSession != null && mHandler != null
			    && Intent.ACTION_MEDIA_BUTTON.equals(mediaButtonIntent.getAction())) {
				KeyEvent ke = mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, android.view.KeyEvent.class);
				if (ke != null && ke.getAction() == KeyEvent.ACTION_DOWN) {
					PlaybackState state = mSession.playback_state;
					long validActions = state == null ? 0 : state.getActions();
					switch (ke.getKeyCode()) {
						case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
						case KeyEvent.KEYCODE_HEADSETHOOK:
							if (ke.getRepeatCount() > 0) {
								// Consider long-press as a single tap.
								handleMediaPlayPauseKeySingleTapIfPending();
							} else if (mMediaPlayPauseKeyPending) {
								// Consider double tap as the next.
								mHandler.removeMessages(CallbackMessageHandler
											    .MSG_PLAY_PAUSE_KEY_DOUBLE_TAP_TIMEOUT);
								mMediaPlayPauseKeyPending = false;
								if ((validActions & PlaybackState.ACTION_SKIP_TO_NEXT) != 0) {
									onSkipToNext();
								}
							} else {
								mMediaPlayPauseKeyPending = true;
								mSession.dispatchMediaButtonDelayed(
								    mSession.getCurrentControllerInfo(),
								    mediaButtonIntent, ViewConfiguration.getDoubleTapTimeout());
							}
							return true;
						default:
							// If another key is pressed within double tap timeout, consider the
							// pending play/pause as a single tap to handle media keys in order.
							handleMediaPlayPauseKeySingleTapIfPending();
							break;
					}

					switch (ke.getKeyCode()) {
						case KeyEvent.KEYCODE_MEDIA_PLAY:
							if ((validActions & PlaybackState.ACTION_PLAY) != 0) {
								onPlay();
								return true;
							}
							break;
						case KeyEvent.KEYCODE_MEDIA_PAUSE:
							if ((validActions & PlaybackState.ACTION_PAUSE) != 0) {
								onPause();
								return true;
							}
							break;
						case KeyEvent.KEYCODE_MEDIA_NEXT:
							if ((validActions & PlaybackState.ACTION_SKIP_TO_NEXT) != 0) {
								onSkipToNext();
								return true;
							}
							break;
						case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
							if ((validActions & PlaybackState.ACTION_SKIP_TO_PREVIOUS) != 0) {
								onSkipToPrevious();
								return true;
							}
							break;
						case KeyEvent.KEYCODE_MEDIA_STOP:
							if ((validActions & PlaybackState.ACTION_STOP) != 0) {
								onStop();
								return true;
							}
							break;
						case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
							if ((validActions & PlaybackState.ACTION_FAST_FORWARD) != 0) {
								onFastForward();
								return true;
							}
							break;
						case KeyEvent.KEYCODE_MEDIA_REWIND:
							if ((validActions & PlaybackState.ACTION_REWIND) != 0) {
								onRewind();
								return true;
							}
							break;
					}
				}
			}
			return false;
		}

		private void handleMediaPlayPauseKeySingleTapIfPending() {
			if (!mMediaPlayPauseKeyPending) {
				return;
			}
			mMediaPlayPauseKeyPending = false;
			mHandler.removeMessages(CallbackMessageHandler.MSG_PLAY_PAUSE_KEY_DOUBLE_TAP_TIMEOUT);
			PlaybackState state = mSession.playback_state;
			long validActions = state == null ? 0 : state.getActions();
			boolean isPlaying = state != null
			    && state.getState() == PlaybackState.STATE_PLAYING;
			boolean canPlay = (validActions & (PlaybackState.ACTION_PLAY_PAUSE
							   | PlaybackState.ACTION_PLAY)) != 0;
			boolean canPause = (validActions & (PlaybackState.ACTION_PLAY_PAUSE
							    | PlaybackState.ACTION_PAUSE)) != 0;
			if (isPlaying && canPause) {
				onPause();
			} else if (!isPlaying && canPlay) {
				onPlay();
			}
		}

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

		public void onSetRating(@NonNull Rating rating) {}

		public void onSetPlaybackSpeed(float speed) {}

		public void onCustomAction(@NonNull String action, @Nullable Bundle extras) {}
	}
}
