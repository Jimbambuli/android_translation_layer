package android.media;

import android.media.SoundPool;
import android.util.Log;

/* AOSP media/java/android/media/MediaActionSound.java. The sounds are loaded from /product/media/audio/ui/ and
 * /system/media/audio/ui/ as in AOSP; where these files do not exist SoundPool.load() fails and play() only logs
 * the error, as AOSP does. */
public class MediaActionSound {
	private static final int NUM_MEDIA_SOUND_STREAMS = 1;

	private SoundPool mSoundPool;
	private SoundState[] mSounds;

	private static final String[] SOUND_DIRS = {
		"/product/media/audio/ui/",
		"/system/media/audio/ui/",
	};

	private static final String[] SOUND_FILES = {
		"camera_click.ogg",
		"camera_focus.ogg",
		"VideoRecord.ogg",
		"VideoStop.ogg"
	};

	private static final String TAG = "MediaActionSound";
	
	public static final int SHUTTER_CLICK         = 0;

	public static final int FOCUS_COMPLETE        = 1;

	public static final int START_VIDEO_RECORDING = 2;

	public static final int STOP_VIDEO_RECORDING  = 3;

	private static final int STATE_NOT_LOADED             = 0;
	private static final int STATE_LOADING                = 1;
	private static final int STATE_LOADING_PLAY_REQUESTED = 2;
	private static final int STATE_LOADED                 = 3;

	/* AOSP mustPlayShutterSound() asks AudioService.isCameraSoundForced(). ATL has no audio service; the answer of
	 * an AOSP device with the default configuration is config_camera_sound_forced, false in core/res config.xml
	 * (that AudioService reads this resource is not checked here: services/ is not in the local AOSP tree) */
	public static boolean mustPlayShutterSound() {
		return false;
	}

	private class SoundState {
		public final int name;
		public int id;
		public int state;

		public SoundState(int name) {
			this.name = name;
			id = 0; // 0 is an invalid sample ID.
			state = STATE_NOT_LOADED;
		}
	}
	
	public MediaActionSound() {
		mSoundPool = new SoundPool.Builder()
				.setMaxStreams(NUM_MEDIA_SOUND_STREAMS)
				.setAudioAttributes(new AudioAttributes.Builder()
					.setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
					.setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
					.setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
					.build())
				.build();
		mSoundPool.setOnLoadCompleteListener(mLoadCompleteListener);
		mSounds = new SoundState[SOUND_FILES.length];
		for (int i = 0; i < mSounds.length; i++) {
			mSounds[i] = new SoundState(i);
		}
	}

	private int loadSound(SoundState sound) {
		final String soundFileName = SOUND_FILES[sound.name];
		for (String soundDir : SOUND_DIRS) {
			int id = mSoundPool.load(soundDir + soundFileName, 1);
			if (id > 0) {
				sound.state = STATE_LOADING;
				sound.id = id;
				return id;
			}
		}
		return 0;
	}

	public void load(int soundName) {
		if (soundName < 0 || soundName >= SOUND_FILES.length) {
			throw new RuntimeException("Unknown sound requested: " + soundName);
		}
		SoundState sound = mSounds[soundName];
		synchronized (sound) {
			switch (sound.state) {
			case STATE_NOT_LOADED:
				if (loadSound(sound) <= 0) {
					Log.e(TAG, "load() error loading sound: " + soundName);
				}
				break;
			default:
				Log.e(TAG, "load() called in wrong state: " + sound + " for sound: "+ soundName);
				break;
			}
		}
	}

	public void play(int soundName) {
		if (soundName < 0 || soundName >= SOUND_FILES.length) {
			throw new RuntimeException("Unknown sound requested: " + soundName);
		}
		SoundState sound = mSounds[soundName];
		synchronized (sound) {
			switch (sound.state) {
			case STATE_NOT_LOADED:
				loadSound(sound);
				if (loadSound(sound) <= 0) {
					Log.e(TAG, "play() error loading sound: " + soundName);
					break;
				}

			case STATE_LOADING:
				sound.state = STATE_LOADING_PLAY_REQUESTED;
				break;
			case STATE_LOADED:
				mSoundPool.play(sound.id, 1.0f, 1.0f, 0, 0, 1.0f);
				break;
			default:
				Log.e(TAG, "play() called in wrong state: " + sound.state + " for sound: "+ soundName);
				break;
			}
		}
	}

	private SoundPool.OnLoadCompleteListener mLoadCompleteListener =
			new SoundPool.OnLoadCompleteListener() {
		public void onLoadComplete(SoundPool soundPool,
				int sampleId, int status) {
			for (SoundState sound : mSounds) {
				if (sound.id != sampleId) {
					continue;
				}
				int playSoundId = 0;
				synchronized (sound) {
					if (status != 0) {
						sound.state = STATE_NOT_LOADED;
						sound.id = 0;
						Log.e(TAG, "OnLoadCompleteListener() error: " + status +
								" loading sound: "+ sound.name);
						return;
					}
					switch (sound.state) {
					case STATE_LOADING:
						sound.state = STATE_LOADED;
						break;
					case STATE_LOADING_PLAY_REQUESTED:
						playSoundId = sound.id;
						sound.state = STATE_LOADED;
						break;
					default:
						Log.e(TAG, "OnLoadCompleteListener() called in wrong state: "
								+ sound.state + " for sound: "+ sound.name);
						break;
					}
				}
				if (playSoundId != 0) {
					soundPool.play(playSoundId, 1.0f, 1.0f, 0, 0, 1.0f);
				}
				break;
			}
		}
	};

	public void release() {
		if (mSoundPool != null) {
			for (SoundState sound : mSounds) {
				synchronized (sound) {
					sound.state = STATE_NOT_LOADED;
					sound.id = 0;
				}
			}
			mSoundPool.release();
			mSoundPool = null;
		}
	}
}
