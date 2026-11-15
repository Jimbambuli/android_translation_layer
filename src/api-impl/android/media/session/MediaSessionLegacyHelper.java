package android.media.session;

import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.ArrayMap;
import android.util.Log;
import android.view.KeyEvent;

/* AOSP media/java/android/media/session/MediaSessionLegacyHelper.java, the part behind
 * AudioManager.registerMediaButtonEventReceiver()/unregisterMediaButtonEventReceiver(): one active MediaSession
 * per PendingIntent whose callback sends the media button intents to that PendingIntent. The
 * RemoteControlClient part (addRccListener() and the metadata/playback state forwarding) is not taken over. */
public class MediaSessionLegacyHelper {
	private static final String TAG = "MediaSessionHelper";

	private static final Object sLock = new Object();
	private static MediaSessionLegacyHelper sInstance;

	private Context mContext;
	private ArrayMap<PendingIntent, SessionHolder> mSessions = new ArrayMap<PendingIntent, SessionHolder>();

	private MediaSessionLegacyHelper(Context context) {
		mContext = context;
	}

	public static MediaSessionLegacyHelper getHelper(Context context) {
		synchronized (sLock) {
			if (sInstance == null) {
				sInstance = new MediaSessionLegacyHelper(context.getApplicationContext());
			}
		}
		return sInstance;
	}

	public void addMediaButtonListener(PendingIntent pi, ComponentName mbrComponent, Context context) {
		if (pi == null) {
			Log.w(TAG, "Pending intent was null, can't addMediaButtonListener.");
			return;
		}
		SessionHolder holder = getHolder(pi, true);
		if (holder == null) {
			return;
		}
		holder.mMediaButtonListener = new MediaButtonListener(pi, context);
		holder.mFlags |= MediaSession.FLAG_HANDLES_MEDIA_BUTTONS;
		holder.mSession.setFlags(holder.mFlags);
		holder.mSession.setMediaButtonReceiver(pi);
		holder.update();
	}

	public void removeMediaButtonListener(PendingIntent pi) {
		if (pi == null) {
			return;
		}
		SessionHolder holder = getHolder(pi, false);
		if (holder != null && holder.mMediaButtonListener != null) {
			holder.mFlags &= ~MediaSession.FLAG_HANDLES_MEDIA_BUTTONS;
			holder.mSession.setFlags(holder.mFlags);
			holder.mMediaButtonListener = null;
			holder.update();
		}
	}

	private SessionHolder getHolder(PendingIntent pi, boolean createIfMissing) {
		SessionHolder holder = mSessions.get(pi);
		if (holder == null && createIfMissing) {
			MediaSession session;
			session = new MediaSession(mContext, TAG + "-" + pi.getCreatorPackage());
			session.setActive(true);
			holder = new SessionHolder(session, pi);
			mSessions.put(pi, holder);
		}
		return holder;
	}

	private static void sendKeyEvent(PendingIntent pi, Context context, Intent intent) {
		try {
			pi.send(context, 0, intent);
		} catch (CanceledException e) {
			Log.e(TAG, "Error sending media key down event:", e);
			return;
		}
	}

	private static final class MediaButtonListener extends MediaSession.Callback {
		private final PendingIntent mPendingIntent;
		private final Context mContext;

		public MediaButtonListener(PendingIntent pi, Context context) {
			mPendingIntent = pi;
			mContext = context;
		}

		@Override
		public boolean onMediaButtonEvent(Intent mediaButtonIntent) {
			MediaSessionLegacyHelper.sendKeyEvent(mPendingIntent, mContext, mediaButtonIntent);
			return true;
		}

		@Override
		public void onPlay() {
			sendKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY);
		}

		@Override
		public void onPause() {
			sendKeyEvent(KeyEvent.KEYCODE_MEDIA_PAUSE);
		}

		@Override
		public void onSkipToNext() {
			sendKeyEvent(KeyEvent.KEYCODE_MEDIA_NEXT);
		}

		@Override
		public void onSkipToPrevious() {
			sendKeyEvent(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
		}

		@Override
		public void onFastForward() {
			sendKeyEvent(KeyEvent.KEYCODE_MEDIA_FAST_FORWARD);
		}

		@Override
		public void onRewind() {
			sendKeyEvent(KeyEvent.KEYCODE_MEDIA_REWIND);
		}

		@Override
		public void onStop() {
			sendKeyEvent(KeyEvent.KEYCODE_MEDIA_STOP);
		}

		private void sendKeyEvent(int keyCode) {
			KeyEvent ke = new KeyEvent(KeyEvent.ACTION_DOWN, keyCode);
			Intent intent = new Intent(Intent.ACTION_MEDIA_BUTTON);
			intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);

			intent.putExtra(Intent.EXTRA_KEY_EVENT, ke);
			MediaSessionLegacyHelper.sendKeyEvent(mPendingIntent, mContext, intent);

			ke = new KeyEvent(KeyEvent.ACTION_UP, keyCode);
			intent.putExtra(Intent.EXTRA_KEY_EVENT, ke);
			MediaSessionLegacyHelper.sendKeyEvent(mPendingIntent, mContext, intent);
		}
	}

	private class SessionHolder {
		public final MediaSession mSession;
		public final PendingIntent mPi;
		public MediaButtonListener mMediaButtonListener;
		public int mFlags;

		public SessionCallback mCb;

		public SessionHolder(MediaSession session, PendingIntent pi) {
			mSession = session;
			mPi = pi;
		}

		public void update() {
			if (mMediaButtonListener == null) {
				mSession.setCallback(null);
				mSession.release();
				mCb = null;
				mSessions.remove(mPi);
			} else if (mCb == null) {
				mCb = new SessionCallback();
				Handler handler = new Handler(Looper.getMainLooper());
				mSession.setCallback(mCb, handler);
			}
		}

		private class SessionCallback extends MediaSession.Callback {

			@Override
			public boolean onMediaButtonEvent(Intent mediaButtonIntent) {
				if (mMediaButtonListener != null) {
					mMediaButtonListener.onMediaButtonEvent(mediaButtonIntent);
				}
				return true;
			}

			@Override
			public void onPlay() {
				if (mMediaButtonListener != null) {
					mMediaButtonListener.onPlay();
				}
			}

			@Override
			public void onPause() {
				if (mMediaButtonListener != null) {
					mMediaButtonListener.onPause();
				}
			}

			@Override
			public void onSkipToNext() {
				if (mMediaButtonListener != null) {
					mMediaButtonListener.onSkipToNext();
				}
			}

			@Override
			public void onSkipToPrevious() {
				if (mMediaButtonListener != null) {
					mMediaButtonListener.onSkipToPrevious();
				}
			}

			@Override
			public void onFastForward() {
				if (mMediaButtonListener != null) {
					mMediaButtonListener.onFastForward();
				}
			}

			@Override
			public void onRewind() {
				if (mMediaButtonListener != null) {
					mMediaButtonListener.onRewind();
				}
			}

			@Override
			public void onStop() {
				if (mMediaButtonListener != null) {
					mMediaButtonListener.onStop();
				}
			}
		}
	}
}
