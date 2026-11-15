package android.media;

import android.content.Context;
import android.net.Uri;
import java.io.IOException;

/* AOSP media/java/android/media/Ringtone.java: the ringtone is played by a local MediaPlayer opened on the
 * URI. ATL has no remote ringtone player (the system's IRingtonePlayer); a URI that cannot be opened
 * locally stays silent, as AOSP's play() does when neither player is available. */
public class Ringtone {
	private final Context mContext;
	private Uri mUri;
	private MediaPlayer mLocalPlayer;
	private AudioAttributes mAudioAttributes = new AudioAttributes.Builder()
	    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
	    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
	    .build();
	private boolean mIsLooping;
	private float mVolume = 1.0f;

	public Ringtone(Context context, boolean allowRemote) {
		mContext = context;
	}

	/* AOSP setStreamType(): the stream becomes the legacy stream type of the attributes */
	public void setStreamType(int streamType) {
		setAudioAttributes(new AudioAttributes.Builder().setInternalLegacyStreamType(streamType).build());
	}

	public int getStreamType() {
		return AudioAttributes.toLegacyStreamType(mAudioAttributes);
	}

	/* AOSP setAudioAttributes(): a prepared local player is reopened with the new attributes */
	public void setAudioAttributes(AudioAttributes attributes) throws IllegalArgumentException {
		if (attributes == null)
			throw new IllegalArgumentException("Invalid null AudioAttributes for Ringtone");
		mAudioAttributes = attributes;
		if (mLocalPlayer != null)
			createLocalMediaPlayer();
	}

	public AudioAttributes getAudioAttributes() {
		return mAudioAttributes;
	}

	public void setLooping(boolean looping) {
		mIsLooping = looping;
		if (mLocalPlayer != null)
			mLocalPlayer.setLooping(looping);
	}

	public boolean isLooping() {
		return mIsLooping;
	}

	public void setVolume(float volume) {
		mVolume = Math.max(0.0f, Math.min(volume, 1.0f));
		if (mLocalPlayer != null)
			mLocalPlayer.setVolume(mVolume, mVolume);
	}

	public float getVolume() {
		return mVolume;
	}

	public String getTitle(Context context) {
		return "FIXME: Ringtone.getTitle()";
	}

	public void setUri(Uri uri) {
		mUri = uri;
		if (mUri == null)
			destroyLocalPlayer();
	}

	public Uri getUri() {
		return mUri;
	}

	/* AOSP createLocalMediaPlayer(): open and prepare the URI; on failure there is no local player */
	boolean createLocalMediaPlayer() {
		if (mUri == null)
			return false;
		destroyLocalPlayer();
		mLocalPlayer = new MediaPlayer();
		try {
			mLocalPlayer.setDataSource(mContext, mUri);
			mLocalPlayer.setAudioAttributes(mAudioAttributes);
			mLocalPlayer.setLooping(mIsLooping);
			mLocalPlayer.setVolume(mVolume, mVolume);
			mLocalPlayer.prepare();
		} catch (IOException | RuntimeException e) {
			/* AOSP catches SecurityException and IOException; ATL's MediaPlayer also fails some URIs it
			 * cannot resolve with runtime exceptions */
			destroyLocalPlayer();
		}
		return mLocalPlayer != null;
	}

	/* AOSP play(): starts the local player if the stream volume is not 0; without one there is nothing
	 * to play */
	public void play() {
		if (mLocalPlayer != null) {
			AudioManager am = (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);
			if (am == null || am.getStreamVolume(AudioAttributes.toLegacyStreamType(mAudioAttributes)) != 0)
				mLocalPlayer.start();
		} else {
			android.util.Log.w("Ringtone", "Neither local nor remote playback available");
		}
	}

	public void stop() {
		destroyLocalPlayer();
	}

	public boolean isPlaying() {
		if (mLocalPlayer != null)
			return mLocalPlayer.isPlaying();
		android.util.Log.w("Ringtone", "Neither local nor remote playback available");
		return false;
	}

	private void destroyLocalPlayer() {
		if (mLocalPlayer != null) {
			mLocalPlayer.setOnCompletionListener(null);
			mLocalPlayer.reset();
			mLocalPlayer.release();
			mLocalPlayer = null;
		}
	}
    /* AOSP 16 API; no backing hardware/service in ATL */
    protected void finalize() {  }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public boolean isHapticGeneratorEnabled() { return false; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public boolean setHapticGeneratorEnabled(boolean arg0) { return false; }
}
