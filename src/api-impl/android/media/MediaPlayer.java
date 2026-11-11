package android.media;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.system.ErrnoException;
import android.system.Os;
import java.io.FileDescriptor;
import java.io.IOException;
import java.util.Map;

/* Plays the audio of the data source, like AOSP's NuPlayer: demuxer and decoder (libavformat,
 * libavcodec) feed the audio device on a playback thread of the player. There is no video output.
 * The listeners are called on the looper of the thread that created the player, like AOSP's. */
public class MediaPlayer {
	public static final int MEDIA_ERROR_UNKNOWN = 1;
	public static final int MEDIA_ERROR_SERVER_DIED = 100;
	public static final int MEDIA_ERROR_IO = -1004;
	public static final int MEDIA_ERROR_MALFORMED = -1007;
	public static final int MEDIA_ERROR_UNSUPPORTED = -1010;
	public static final int MEDIA_ERROR_TIMED_OUT = -110;

	/* native_step() */
	private static final int STEP_END = -1;

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

	private final Handler eventHandler;
	private OnPreparedListener onPreparedListener;
	private OnCompletionListener onCompletionListener;
	private OnErrorListener onErrorListener;
	private OnSeekCompleteListener onSeekCompleteListener;

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
		postEvent(() -> {
			if (onPreparedListener != null)
				onPreparedListener.onPrepared(this);
		});
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
				if (onPreparedListener != null)
					onPreparedListener.onPrepared(this);
			});
		}, "MediaPlayer prepare").start();
	}

	private void prepared(long player) {
		nativePlayer = player;
		duration = native_getDuration(player);
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

	private void ensureThread() {
		if (thread == null) {
			long player = nativePlayer;
			thread = new Thread(() -> playbackLoop(player), "MediaPlayer");
			thread.start();
		}
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
				native_seek(player, seek);
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
					native_seek(player, resumeAt);
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
			}
			playing = true;
			ensureThread();
			lock.notifyAll();
		}
	}

	public void pause() {
		synchronized (lock) {
			playing = false;
			lock.notifyAll();
		}
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
		Thread t;
		synchronized (lock) {
			t = thread;
			thread = null;
			playing = false;
			seekRequest = -1;
			lock.notifyAll();
		}
		if (t != null) {
			try {
				t.join();
			} catch (InterruptedException e) {
			}
		}
		if (nativePlayer != 0)
			native_release(nativePlayer);
		nativePlayer = 0;
		position = 0;
	}

	public void reset() {
		stop();
		generation++;
		eventHandler.removeCallbacksAndMessages(null);
		closeSource();
		synchronized (lock) {
			looping = false;
		}
	}

	public void release() {
		reset();
		onPreparedListener = null;
		onCompletionListener = null;
		onErrorListener = null;
		onSeekCompleteListener = null;
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

	public void setOnBufferingUpdateListener(OnBufferingUpdateListener dummy) {}
	public void setOnInfoListener(OnInfoListener dummy) {}
	public void setOnVideoSizeChangedListener(OnVideoSizeChangedListener dummy) {}
	public void setAudioAttributes(AudioAttributes attributes) {}
	public void setAudioStreamType(int dummy) {}

	public int getAudioSessionId() { return 0; }

	public void setWakeMode(Context context, int mode) {}

	private static native long native_open(String path, int fd, long offset, long length);
	private static native int native_getDuration(long player);
	private static native int native_step(long player, float left, float right, boolean looping);
	private static native void native_pause(long player);
	private static native void native_seek(long player, int msec);
	private static native void native_release(long player);
}
