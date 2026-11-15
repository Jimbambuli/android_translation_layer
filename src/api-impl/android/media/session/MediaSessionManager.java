package android.media.session;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.ComponentName;
import android.os.Handler;
import android.text.TextUtils;
import android.view.KeyEvent;
import java.util.List;
import java.util.Objects;

/* AOSP MediaSessionManager: the media keys go to the app's media button session. ATL has no
 * MediaSessionService: the sessions of the app are the only ones, and the media button session is the
 * one that played last (MediaSessionStack.updateMediaButtonSessionIfNeeded()), see MediaSession. */
public class MediaSessionManager {

	/* MediaSessionManager.dispatchMediaKeyEvent(): like MediaSessionService.dispatchMediaKeyEventLocked(),
	 * to the media button session, from the system */
	public void dispatchMediaKeyEvent(@NonNull KeyEvent keyEvent) {
		dispatchMediaKeyEventInternal(keyEvent, /*asSystemService=*/false, /*needWakeLock=*/false);
	}

	public void dispatchMediaKeyEvent(@NonNull KeyEvent keyEvent, boolean needWakeLock) {
		dispatchMediaKeyEventInternal(keyEvent, /*asSystemService=*/false, needWakeLock);
	}

	public void dispatchMediaKeyEventAsSystemService(@NonNull KeyEvent keyEvent) {
		dispatchMediaKeyEventInternal(keyEvent, /*asSystemService=*/true, /*needWakeLock=*/true);
	}

	/* MediaSessionManager.dispatchMediaKeyEventToSessionAsSystemService(): to the given session */
	public boolean dispatchMediaKeyEventToSessionAsSystemService(@NonNull KeyEvent keyEvent,
								      @NonNull MediaSession.Token sessionToken) {
		Objects.requireNonNull(sessionToken, "sessionToken shouldn't be null");
		Objects.requireNonNull(keyEvent, "keyEvent shouldn't be null");
		if (!KeyEvent.isMediaSessionKey(keyEvent.getKeyCode())) {
			return false;
		}
		return sessionToken.session.dispatchMediaKeyEvent(keyEvent);
	}

	/* AOSP getActiveSessions()/addOnActiveSessionsChangedListener()/getMediaKeyEventSession(): they need
	 * MEDIA_CONTENT_CONTROL (a privileged permission) or an enabled notification listener and throw a
	 * SecurityException otherwise (AOSP javadoc). ATL grants neither, so they throw. */
	public interface OnActiveSessionsChangedListener {
		void onActiveSessionsChanged(@Nullable List<MediaController> controllers);
	}

	private static SecurityException noMediaControl() {
		return new SecurityException("requires MEDIA_CONTENT_CONTROL or an enabled notification listener");
	}

	public @NonNull List<MediaController> getActiveSessions(@Nullable ComponentName notificationListener) {
		throw noMediaControl();
	}

	public void addOnActiveSessionsChangedListener(@NonNull OnActiveSessionsChangedListener sessionListener,
						       @Nullable ComponentName notificationListener) {
		addOnActiveSessionsChangedListener(sessionListener, notificationListener, null);
	}

	public void addOnActiveSessionsChangedListener(@NonNull OnActiveSessionsChangedListener sessionListener,
						       @Nullable ComponentName notificationListener, @Nullable Handler handler) {
		Objects.requireNonNull(sessionListener, "sessionListener shouldn't be null");
		throw noMediaControl();
	}

	/* nothing was added, so there is nothing to remove */
	public void removeOnActiveSessionsChangedListener(@NonNull OnActiveSessionsChangedListener sessionListener) {
		Objects.requireNonNull(sessionListener, "sessionListener shouldn't be null");
	}

	public @Nullable MediaSession.Token getMediaKeyEventSession() {
		throw noMediaControl();
	}

	/* AOSP isTrustedForMediaControl(): the trust decision is MediaSessionService.isTrusted() (not in the
	 * local sources); without that service the call ends in AOSP's RemoteException path, false */
	public boolean isTrustedForMediaControl(@NonNull RemoteUserInfo userInfo) {
		Objects.requireNonNull(userInfo, "userInfo shouldn't be null");
		return false;
	}

	private void dispatchMediaKeyEventInternal(KeyEvent keyEvent, boolean asSystemService, boolean needWakeLock) {
		Objects.requireNonNull(keyEvent, "keyEvent shouldn't be null");
		MediaSession session = MediaSession.getMediaButtonSession();
		if (session != null)
			session.dispatchMediaKeyEvent(keyEvent);
	}

	/**
	 * Information of a remote user of {@link MediaSession} or {@link MediaBrowserService}.
	 * This can be used to decide whether the remote user is trusted app, and also differentiate
	 * caller of {@link MediaSession} and {@link MediaBrowserService} callbacks.
	 */
	public static final class RemoteUserInfo {
		private final String mPackageName;
		private final int mPid;
		private final int mUid;

		public RemoteUserInfo(@NonNull String packageName, int pid, int uid) {
			mPackageName = packageName;
			mPid = pid;
			mUid = uid;
		}

		public String getPackageName() {
			return mPackageName;
		}

		public int getPid() {
			return mPid;
		}

		public int getUid() {
			return mUid;
		}

		@Override
		public boolean equals(@Nullable Object obj) {
			if (!(obj instanceof RemoteUserInfo)) {
				return false;
			}
			if (this == obj) {
				return true;
			}
			RemoteUserInfo otherUserInfo = (RemoteUserInfo)obj;
			return TextUtils.equals(mPackageName, otherUserInfo.mPackageName)
			    && mPid == otherUserInfo.mPid
			    && mUid == otherUserInfo.mUid;
		}

		@Override
		public int hashCode() {
			return Objects.hash(mPackageName, mPid, mUid);
		}
	}
    /* AOSP 16 API; no backing hardware/service in ATL */
    public void addOnMediaKeyEventSessionChangedListener(java.util.concurrent.Executor arg0, android.media.session.MediaSessionManager.OnMediaKeyEventSessionChangedListener arg1) {  }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public void addOnSession2TokensChangedListener(android.media.session.MediaSessionManager.OnSession2TokensChangedListener arg0) {  }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public void addOnSession2TokensChangedListener(android.media.session.MediaSessionManager.OnSession2TokensChangedListener arg0, android.os.Handler arg1) {  }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public String getMediaKeyEventSessionPackageName() { return null; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public java.util.List<android.media.Session2Token> getSession2Tokens() { return new java.util.ArrayList<>(); }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public void notifySession2Created(android.media.Session2Token arg0) {  }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public void removeOnMediaKeyEventSessionChangedListener(android.media.session.MediaSessionManager.OnMediaKeyEventSessionChangedListener arg0) {  }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public void removeOnSession2TokensChangedListener(android.media.session.MediaSessionManager.OnSession2TokensChangedListener arg0) {  }
}
