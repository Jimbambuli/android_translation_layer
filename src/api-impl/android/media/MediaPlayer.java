package android.media;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.system.ErrnoException;
import android.system.Os;
import android.view.Surface;
import android.view.SurfaceHolder;
import java.io.FileDescriptor;
import java.io.IOException;
import java.util.Map;

/* Plays the data source like AOSP's NuPlayer: demuxer and decoders (libavformat, libavcodec) feed the
 * audio device on a playback thread of the player, and the video frames go to the Surface on a video
 * thread, each when the audio clock (what the device has played) reaches its time; a frame that is
 * more than 40 ms late is dropped, like NuPlayer's renderer does. The listeners are called on the
 * looper of the thread that created the player, like AOSP's. */
public class MediaPlayer {
	public static final int MEDIA_ERROR_UNKNOWN = 1;
	public static final int MEDIA_ERROR_SERVER_DIED = 100;
	public static final int MEDIA_ERROR_IO = -1004;
	public static final int MEDIA_ERROR_MALFORMED = -1007;
	public static final int MEDIA_ERROR_UNSUPPORTED = -1010;
	public static final int MEDIA_ERROR_TIMED_OUT = -110;

	/* native_step() */
	private static final int STEP_END = -1;
	/* native_videoDecode() */
	private static final int VIDEO_END = -1;
	/* NuPlayer's renderer drops a video frame that is later than this (ms) */
	private static final int VIDEO_MAX_LATE_MS = 40;

	/* AOSP's limits (NuPlayer and the audio sink's time stretch) */
	private static final float SPEED_MIN = 0.01f;
	private static final float SPEED_MAX = 20.0f;
	private static final float PITCH_MIN = 0.25f;
	private static final float PITCH_MAX = 4.0f;

	public static final int VIDEO_SCALING_MODE_SCALE_TO_FIT = 1;
	public static final int VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING = 2;

	public interface OnCompletionListener {
		void onCompletion(MediaPlayer mp);
	}
	public interface OnErrorListener {
		boolean onError(MediaPlayer mp, int what, int extra);
	}
	public interface OnPreparedListener {
		void onPrepared(MediaPlayer mp);
	}
	public interface OnBufferingUpdateListener {
		void onBufferingUpdate(MediaPlayer mp, int percent);
	}
	public interface OnInfoListener {
		boolean onInfo(MediaPlayer mp, int what, int extra);
	}
	public interface OnSeekCompleteListener {
		void onSeekComplete(MediaPlayer mp);
	}
	public interface OnVideoSizeChangedListener {
		void onVideoSizeChanged(MediaPlayer mp, int width, int height);
	}
	public interface MediaPlayerControl {
	}

	/* the data source: a path or URL, or a region of a file descriptor (a dup, like AOSP's) */
	private String path;
	private FileDescriptor fd;
	private long fdOffset;
	private long fdLength;

	private long nativePlayer; // 0 unless prepared
	private int duration;
	private int videoWidth;
	private volatile int videoScalingMode = VIDEO_SCALING_MODE_SCALE_TO_FIT;
	private int videoHeight;
	private volatile Surface surface;
	private volatile int position;
	private int generation; // a reset/release makes pending events and asynchronous prepares stale

	private final Object lock = new Object();
	/* guarded by lock */
	private Thread thread;
	private boolean playing;
	private boolean completed;
	private boolean looping;
	private int seekRequest = -1;
	private float leftVolume = 1.f;
	private float rightVolume = 1.f;
	private Thread videoThread;
	private int videoSeekGeneration; // a seek of the video is due when it differs from the video thread's
	private int videoSeekTarget;
	private boolean paused; // pause() after start(): getPlaybackParams() tells speed 0, like NuPlayer
	/* setPlaybackParams(): the speed (not 0) and pitch */
	private float speed = 1.f;
	private float pitch = 1.f;

	private final Handler eventHandler;
	private OnPreparedListener onPreparedListener;
	private OnCompletionListener onCompletionListener;
	private OnErrorListener onErrorListener;
	private OnSeekCompleteListener onSeekCompleteListener;
	private OnVideoSizeChangedListener onVideoSizeChangedListener;

	public MediaPlayer() {
		Looper looper = Looper.myLooper();
		eventHandler = new Handler(looper != null ? looper : Looper.getMainLooper());
	}

	public static MediaPlayer create(Context context, int resid) {
		try {
			AssetFileDescriptor afd = context.getResources().openRawResourceFd(resid);
			if (afd == null)
				return null;
			MediaPlayer mp = new MediaPlayer();
			mp.setDataSource(afd);
			afd.close();
			mp.prepare();
			return mp;
		} catch (IOException | RuntimeException e) {
			System.out.println("MediaPlayer.create(" + resid + ") failed: " + e);
			return null;
		}
	}

	public static MediaPlayer create(Context context, Uri uri) {
		try {
			MediaPlayer mp = new MediaPlayer();
			mp.setDataSource(context, uri);
			mp.prepare();
			return mp;
		} catch (IOException | RuntimeException e) {
			System.out.println("MediaPlayer.create(" + uri + ") failed: " + e);
			return null;
		}
	}

	public void setDataSource(String path) throws IOException {
		closeSource();
		this.path = path.startsWith("file://") ? Uri.parse(path).getPath() : path;
	}

	public void setDataSource(Context context, Uri uri) throws IOException {
		String scheme = uri.getScheme();
		if (scheme == null || "file".equals(scheme)) {
			setDataSource(uri.getPath());
			return;
		}
		if ("http".equals(scheme) || "https".equals(scheme)) {
			setDataSource(uri.toString());
			return;
		}
		AssetFileDescriptor afd = context.getContentResolver().openAssetFileDescriptor(uri, "r");
		if (afd == null)
			throw new IOException("no data for " + uri);
		try {
			setDataSource(afd);
		} finally {
			afd.close();
		}
	}

	/* request headers are not supported */
	public void setDataSource(Context context, Uri uri, Map<String, String> headers) throws IOException {
		setDataSource(context, uri);
	}

	public void setDataSource(AssetFileDescriptor afd) throws IOException {
		if (afd.getDeclaredLength() < 0)
			setDataSource(afd.getFileDescriptor());
		else
			setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getDeclaredLength());
	}

	public void setDataSource(FileDescriptor fd) throws IOException {
		setDataSource(fd, 0, 0x7ffffffffffffffL);
	}

	public void setDataSource(FileDescriptor fd, long offset, long length) throws IOException {
		closeSource();
		try {
			this.fd = Os.dup(fd);
		} catch (ErrnoException e) {
			throw new IOException(e);
		}
		fdOffset = offset;
		fdLength = length;
	}

	private long open() {
		return native_open(path, fd != null ? fd.getInt$() : -1, fdOffset, fdLength);
	}

	public void prepare() throws IOException {
		long player = open();
		if (player == 0)
			throw new IOException("Prepare failed.: status=0x1");
		prepared(player);
		postEvent(this::videoSizeChanged);
		postEvent(() -> {
			if (onPreparedListener != null)
				onPreparedListener.onPrepared(this);
		});
	}

	/* NuPlayer reports the video size (0x0 without video) before the prepared event */
	private void videoSizeChanged() {
		if (onVideoSizeChangedListener != null)
			onVideoSizeChangedListener.onVideoSizeChanged(this, videoWidth, videoHeight);
	}

	public void prepareAsync() {
		int gen = generation;
		new Thread(() -> {
			long player = open();
			eventHandler.post(() -> {
				if (gen != generation) {
					if (player != 0)
						native_release(player);
					return;
				}
				if (player == 0) {
					error(MEDIA_ERROR_UNKNOWN, MEDIA_ERROR_UNSUPPORTED);
					return;
				}
				prepared(player);
				videoSizeChanged();
				if (onPreparedListener != null)
					onPreparedListener.onPrepared(this);
			});
		}, "MediaPlayer prepare").start();
	}

	private void prepared(long player) {
		nativePlayer = player;
		native_setPlaybackRate(player, speed, pitch);
		duration = native_getDuration(player);
		int size = native_getVideoSize(player);
		videoWidth = size >>> 16;
		videoHeight = size & 0xffff;
		position = 0;
		synchronized (lock) {
			completed = false;
		}
	}

	/* the listeners of a stale event (reset/release in between) are not called */
	private void postEvent(Runnable event) {
		int gen = generation;
		eventHandler.post(() -> {
			if (gen == generation)
				event.run();
		});
	}

	/* AOSP: an error not handled by the OnErrorListener completes the playback */
	private void error(int what, int extra) {
		System.out.println("MediaPlayer error (" + what + ", " + extra + ")");
		boolean handled = onErrorListener != null && onErrorListener.onError(this, what, extra);
		if (!handled && onCompletionListener != null)
			onCompletionListener.onCompletion(this);
	}

	/* guarded by lock */
	private void ensureThread() {
		if (thread == null) {
			long player = nativePlayer;
			thread = new Thread(() -> playbackLoop(player), "MediaPlayer");
			thread.start();
		}
		if (videoThread == null && videoWidth > 0 && surface != null) {
			long player = nativePlayer;
			videoSeekTarget = position; // the video starts where the audio is
			videoSeekGeneration++;
			videoThread = new Thread(() -> videoLoop(player), "MediaPlayer video");
			videoThread.start();
		}
	}

	/* guarded by lock */
	private void seekVideo(int msec) {
		videoSeekTarget = msec;
		videoSeekGeneration++;
	}

	/* NuPlayer's video renderer: each frame waits until the audio clock reaches its time; after a seek
	 * the frame at the new position is shown at once, also while paused */
	private void videoLoop(long player) {
		Thread me = Thread.currentThread();
		int generation = videoSeekGeneration - 1;
		int loops = 0;      // the passes of setLooping() of the video, compared to the audio clock's
		int pts = -1;       // the decoded frame that waits for its time
		boolean ended = false;
		while (true) {
			int target = -1;
			synchronized (lock) {
				if (videoThread != me)
					return;
				if (generation != videoSeekGeneration) {
					generation = videoSeekGeneration;
					target = videoSeekTarget;
				} else if (!playing || ended) {
					try {
						lock.wait();
					} catch (InterruptedException e) {
					}
					continue;
				}
			}
			if (target >= 0) {
				native_videoSeek(player, target);
				loops = 0;
				pts = native_videoDecode(player);
				ended = pts < 0;
				if (pts >= 0)
					render(player, pts);
				pts = -1;
				continue;
			}
			if (pts < 0) {
				pts = native_videoDecode(player);
				if (pts < 0) {
					if (pts == VIDEO_END && isLooping()) {
						native_videoSeek(player, 0);
						loops++;
					} else {
						ended = true;
					}
					pts = -1;
					continue;
				}
			}
			long clock = native_clock(player);
			int clockLoops = (int)(clock >> 32);
			long wait = loops > clockLoops ? 10 : loops < clockLoops ? -VIDEO_MAX_LATE_MS - 1 : pts - (int)clock;
			if (wait > 0) {
				synchronized (lock) {
					if (videoThread == me && generation == videoSeekGeneration && playing) {
						try {
							lock.wait(Math.min(wait, 10));
						} catch (InterruptedException e) {
						}
					}
				}
				continue;
			}
			if (wait >= -VIDEO_MAX_LATE_MS)
				render(player, pts);
			pts = -1;
		}
	}

	/* the frame decoded last into the Surface: the widget of a SurfaceView, or a SurfaceTexture */
	private void render(long player, int pts) {
		Surface s = surface;
		if (s == null)
			return;
		if (s.widget != 0)
			native_showTexture(s.widget, native_videoTexture(player), videoScalingMode == VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
		else if (s.surfaceTexture != null)
			s.surfaceTexture.queueBuffer(native_videoTexture(player), pts * 1000000L);
	}

	private void playbackLoop(long player) {
		boolean running = false; // the device plays (not paused)
		int resumeAt = -1;       // a pause dropped the device's samples, resume from here
		while (true) {
			int seek;
			float left, right;
			boolean loop;
			synchronized (lock) {
				while (thread == Thread.currentThread() && !playing && seekRequest < 0 && !running) {
					try {
						lock.wait();
					} catch (InterruptedException e) {
					}
				}
				if (thread != Thread.currentThread())
					return;
				seek = seekRequest;
				seekRequest = -1;
				if (!playing && running) {
					native_pause(player);
					running = false;
					resumeAt = position;
				}
				if (seek < 0 && !playing)
					continue;
				left = leftVolume;
				right = rightVolume;
				loop = looping;
			}
			if (seek >= 0) {
				native_seek(player, seek, true);
				position = seek;
				resumeAt = -1;
				postEvent(() -> {
					if (onSeekCompleteListener != null)
						onSeekCompleteListener.onSeekComplete(this);
				});
				continue;
			}
			if (!running) {
				if (resumeAt >= 0)
					native_seek(player, resumeAt, false);
				resumeAt = -1;
				running = true;
			}
			int ret = native_step(player, left, right, loop);
			if (ret >= 0) {
				position = ret;
				continue;
			}
			synchronized (lock) {
				playing = false;
				completed = true;
			}
			running = false;
			if (ret == STEP_END) {
				if (duration > 0)
					position = duration;
				postEvent(() -> {
					if (onCompletionListener != null)
						onCompletionListener.onCompletion(this);
				});
			} else {
				postEvent(() -> error(MEDIA_ERROR_UNKNOWN, MEDIA_ERROR_IO));
			}
		}
	}

	public void start() {
		if (nativePlayer == 0) {
			System.out.println("MediaPlayer.start() called in state without data (not prepared)");
			return;
		}
		synchronized (lock) {
			if (completed) {
				completed = false;
				seekRequest = 0;
				seekVideo(0);
			}
			playing = true;
			paused = false;
			ensureThread();
			lock.notifyAll();
		}
	}

	public void pause() {
		synchronized (lock) {
			if (playing)
				paused = true;
			playing = false;
			lock.notifyAll();
		}
	}

	/* AOSP: before prepare only kept; once prepared speed 0 pauses (keeping the previous speed for start()), another
	 * speed starts the playback. Speed 0.01 to 20 (or 0), pitch 0.25 to 4, else IllegalArgumentException. The time
	 * stretch is libavfilter's atempo (AOSP's audio sink: Sonic). */
	public void setPlaybackParams(PlaybackParams params) {
		if (params == null)
			throw new IllegalArgumentException("params must not be null");
		if (nativePlayer == 0 && path == null && fd == null)
			throw new IllegalStateException();
		float newSpeed = speed, newPitch = pitch;
		boolean speedSet = false;
		try {
			newSpeed = params.getSpeed();
			speedSet = true;
		} catch (IllegalStateException e) { // not set: unchanged
		}
		try {
			newPitch = params.getPitch();
		} catch (IllegalStateException e) {
		}
		if ((newSpeed != 0.f && newSpeed < SPEED_MIN) || newSpeed > SPEED_MAX || newPitch < PITCH_MIN || newPitch > PITCH_MAX)
			throw new IllegalArgumentException("Playback params are not supported: speed " + newSpeed + ", pitch " + newPitch);
		pitch = newPitch;
		if (newSpeed != 0.f)
			speed = newSpeed;
		if (nativePlayer == 0)
			return;
		native_setPlaybackRate(nativePlayer, speed, pitch);
		if (speedSet && newSpeed == 0.f)
			pause();
		else if (speedSet)
			start();
	}

	/* AOSP: speed 0 while paused */
	public PlaybackParams getPlaybackParams() {
		if (nativePlayer == 0 && path == null && fd == null)
			throw new IllegalStateException();
		boolean isPaused;
		synchronized (lock) {
			isPaused = paused;
		}
		return new PlaybackParams().setSpeed(isPaused ? 0.f : speed).setPitch(pitch).setAudioFallbackMode(PlaybackParams.AUDIO_FALLBACK_MODE_DEFAULT);
	}

	public boolean isPlaying() {
		synchronized (lock) {
			return playing;
		}
	}

	public void seekTo(int msec) {
		if (nativePlayer == 0)
			return;
		synchronized (lock) {
			seekRequest = Math.max(0, msec);
			seekVideo(seekRequest);
			completed = false;
			ensureThread();
			lock.notifyAll();
		}
		position = Math.max(0, msec);
	}

	public void seekTo(long msec, int mode) {
		seekTo((int)msec);
	}

	public int getCurrentPosition() {
		return position;
	}

	public int getDuration() {
		return nativePlayer != 0 ? duration : -1;
	}

	public void setLooping(boolean looping) {
		synchronized (lock) {
			this.looping = looping;
		}
	}

	public boolean isLooping() {
		synchronized (lock) {
			return looping;
		}
	}

	public void setVolume(float leftVolume, float rightVolume) {
		synchronized (lock) {
			this.leftVolume = leftVolume;
			this.rightVolume = rightVolume;
		}
	}

	/* the playback thread ends, the decoder goes away; prepare() again to play */
	public void stop() {
		Thread t, v;
		synchronized (lock) {
			t = thread;
			thread = null;
			v = videoThread;
			videoThread = null;
			playing = false;
			seekRequest = -1;
			lock.notifyAll();
		}
		for (Thread join : new Thread[] {t, v}) {
			if (join == null)
				continue;
			try {
				join.join();
			} catch (InterruptedException e) {
			}
		}
		if (nativePlayer != 0)
			native_release(nativePlayer);
		nativePlayer = 0;
		position = 0;
		synchronized (lock) {
			paused = false;
		}
	}

	public void reset() {
		stop();
		generation++;
		eventHandler.removeCallbacksAndMessages(null);
		closeSource();
		synchronized (lock) {
			looping = false;
		}
		speed = pitch = 1.f;
	}

	public void release() {
		reset();
		onPreparedListener = null;
		onCompletionListener = null;
		onErrorListener = null;
		onSeekCompleteListener = null;
		onVideoSizeChangedListener = null;
		surface = null;
	}

	private void closeSource() {
		if (fd != null) {
			try {
				Os.close(fd);
			} catch (ErrnoException e) {
			}
		}
		fd = null;
		path = null;
	}

	public void setOnCompletionListener(OnCompletionListener listener) {
		onCompletionListener = listener;
	}

	public void setOnErrorListener(OnErrorListener listener) {
		onErrorListener = listener;
	}

	public void setOnPreparedListener(OnPreparedListener listener) {
		onPreparedListener = listener;
	}

	public void setOnSeekCompleteListener(OnSeekCompleteListener listener) {
		onSeekCompleteListener = listener;
	}

	public void setOnVideoSizeChangedListener(OnVideoSizeChangedListener listener) {
		onVideoSizeChangedListener = listener;
	}

	/* AOSP: the video goes to the holder's Surface (null: no video output) */
	public void setDisplay(SurfaceHolder sh) {
		setSurface(sh != null ? sh.getSurface() : null);
	}

	public void setSurface(Surface surface) {
		this.surface = surface;
		synchronized (lock) {
			if (thread != null && nativePlayer != 0)
				ensureThread(); // the video thread starts at the current position
		}
	}

	public int getVideoWidth() {
		return nativePlayer != 0 ? videoWidth : 0;
	}

	public int getVideoHeight() {
		return nativePlayer != 0 ? videoHeight : 0;
	}

	/* AOSP: SCALE_TO_FIT (the default) stretches the frames over the Surface, SCALE_TO_FIT_WITH_CROPPING covers it
	 * keeping their aspect ratio; like AOSP's compositor this applies to a SurfaceView, a TextureView shows the
	 * whole frame */
	public void setVideoScalingMode(int mode) {
		if (mode != VIDEO_SCALING_MODE_SCALE_TO_FIT && mode != VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
			throw new IllegalArgumentException("Scaling mode " + mode + " is not supported");
		videoScalingMode = mode;
	}

	public void setScreenOnWhilePlaying(boolean screenOn) {}

	public void setOnBufferingUpdateListener(OnBufferingUpdateListener dummy) {}
	public void setOnInfoListener(OnInfoListener dummy) {}
	public void setAudioAttributes(AudioAttributes attributes) {}
	public void setAudioStreamType(int dummy) {}

	public int getAudioSessionId() { return 0; }

	public void setWakeMode(Context context, int mode) {}

	private static native long native_open(String path, int fd, long offset, long length);
	private static native int native_getDuration(long player);
	private static native int native_step(long player, float left, float right, boolean looping);
	private static native void native_pause(long player);
	private static native void native_setPlaybackRate(long player, float speed, float pitch);
	private static native void native_seek(long player, int msec, boolean user);
	private static native long native_clock(long player);
	private static native int native_getVideoSize(long player);
	private static native int native_videoDecode(long player);
	private static native void native_videoSeek(long player, int msec);
	private static native long native_videoTexture(long player);
	private static native void native_showTexture(long widget, long texture, boolean crop);
	private static native void native_release(long player);
}
