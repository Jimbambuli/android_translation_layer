package android.media;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.os.Handler;
import android.os.Looper;
import android.system.ErrnoException;
import android.system.Os;
import android.util.AndroidRuntimeException;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayDeque;

/* AOSP android.media.SoundPool. As in AOSP the samples are decoded completely in the background when they are
 * loaded (OnLoadCompleteListener on the looper of the thread that set it), then played by up to maxStreams
 * streams with priority, loop, rate and volume. The native side (libavformat/libavcodec like MediaPlayer)
 * mixes the streams of a pool into one stream of the audio device. */
public class SoundPool {

	private long nativePool;
	private int nextSampleId;
	private volatile Handler eventHandler;
	private volatile OnLoadCompleteListener onLoadCompleteListener;

	/* the samples still to decode, each with its own dup of the fd (AOSP's Sample dups it in load()) */
	private static class Load {
		int sampleId;
		FileDescriptor fd;
		long offset;
		long length;
	}
	private final ArrayDeque<Load> loads = new ArrayDeque<>();
	private Thread loader;

	public interface OnLoadCompleteListener {
		/**
		 * Called when a sound has completed loading.
		 *
		 * @param soundPool SoundPool object from the load() method
		 * @param sampleId the sample ID of the sound loaded.
		 * @param status the status of the load operation (0 = success)
		 */
		public void onLoadComplete(SoundPool soundPool, int sampleId, int status);
	}

	public SoundPool(int maxStreams, int streamType, int srcQuality) {
		nativePool = native_setup(maxStreams);
	}

	public final void release() {
		Thread t;
		synchronized (loads) {
			for (Load load : loads)
				close(load.fd);
			loads.clear();
			t = loader;
			loader = null;
			loads.notifyAll();
		}
		if (t != null) {
			try {
				t.join();
			} catch (InterruptedException e) {
			}
		}
		synchronized (this) {
			if (nativePool != 0)
				native_release(nativePool);
			nativePool = 0;
		}
	}

	protected void finalize() {
		release();
	}

	public int load(String path, int priority) {
		File f = new File(path);
		try (FileInputStream in = new FileInputStream(f)) {
			return load(in.getFD(), 0, f.length(), priority);
		} catch (IOException e) {
			System.out.println("SoundPool: error loading " + path);
			return 0;
		}
	}

	public int load(Context context, int resId, int priority) {
		AssetFileDescriptor afd = context.getResources().openRawResourceFd(resId);
		int id = 0;
		if (afd != null) {
			id = load(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength(), priority);
			try {
				afd.close();
			} catch (IOException ex) {
			}
		}
		return id;
	}

	public int load(AssetFileDescriptor afd, int priority) {
		if (afd == null)
			return 0;
		long len = afd.getLength();
		if (len < 0)
			throw new AndroidRuntimeException("no length for fd");
		return load(afd.getFileDescriptor(), afd.getStartOffset(), len, priority);
	}

	public int load(FileDescriptor fd, long offset, long length, int priority) {
		Load load = new Load();
		try {
			load.fd = Os.dup(fd);
		} catch (ErrnoException e) {
			return 0;
		}
		load.offset = offset;
		load.length = length;
		synchronized (loads) {
			if (nativePool == 0) {
				close(load.fd);
				return 0;
			}
			load.sampleId = ++nextSampleId;
			loads.add(load);
			if (loader == null) {
				loader = new Thread(this::loaderLoop, "SoundPool loader");
				loader.setDaemon(true);
				loader.start();
			}
			loads.notifyAll();
		}
		return load.sampleId;
	}

	private void loaderLoop() {
		while (true) {
			Load load;
			synchronized (loads) {
				while (loader == Thread.currentThread() && loads.isEmpty()) {
					try {
						loads.wait();
					} catch (InterruptedException e) {
					}
				}
				if (loader != Thread.currentThread())
					return;
				load = loads.poll();
			}
			int status = native_decode(nativePool, load.sampleId, load.fd.getInt$(), load.offset, load.length);
			close(load.fd);
			if (status != 0)
				System.out.println("SoundPool: sample " + load.sampleId + " could not be decoded");
			Handler handler = eventHandler;
			if (handler != null) {
				handler.post(() -> {
					OnLoadCompleteListener listener = onLoadCompleteListener;
					if (listener != null && handler == eventHandler)
						listener.onLoadComplete(this, load.sampleId, status);
				});
			}
		}
	}

	private static void close(FileDescriptor fd) {
		try {
			Os.close(fd);
		} catch (ErrnoException e) {
		}
	}

	public final boolean unload(int soundID) {
		return nativePool != 0 && native_unload(nativePool, soundID);
	}

	/* returns 0 if the sound is not (yet) loaded or no stream is free for its priority; the arguments are
	 * checked like AOSP's SoundPool::play() */
	public final int play(int soundID, float leftVolume, float rightVolume, int priority, int loop, float rate) {
		if (nativePool == 0)
			return 0;
		if (!validVolume(leftVolume, rightVolume))
			leftVolume = rightVolume = 1.f;
		return native_play(nativePool, soundID, leftVolume, rightVolume, priority, Math.max(loop, -1), rate(rate));
	}

	/* AOSP checkVolume(): a volume outside 0.0 to 1.0 makes both 1.0 (for backward compatibility) */
	private static boolean validVolume(float left, float right) {
		return left >= 0.f && left <= 1.f && right >= 0.f && right <= 1.f;
	}

	/* AOSP checkRate(): clamped to 0.125 to 8.0 */
	private static float rate(float r) {
		return Math.max(0.125f, Math.min(8.f, r));
	}

	private static final int CMD_PAUSE = 0;
	private static final int CMD_RESUME = 1;
	private static final int CMD_STOP = 2;
	private static final int CMD_VOLUME = 3;
	private static final int CMD_PRIORITY = 4;
	private static final int CMD_LOOP = 5;
	private static final int CMD_RATE = 6;

	private void command(int streamID, int cmd, int i, float a, float b) {
		if (nativePool != 0)
			native_command(nativePool, streamID, cmd, i, a, b);
	}

	public final void pause(int streamID) {
		command(streamID, CMD_PAUSE, 0, 0, 0);
	}

	public final void resume(int streamID) {
		command(streamID, CMD_RESUME, 0, 0, 0);
	}

	public final void autoPause() {
		if (nativePool != 0)
			native_autoPause(nativePool, true);
	}

	public final void autoResume() {
		if (nativePool != 0)
			native_autoPause(nativePool, false);
	}

	public final void stop(int streamID) {
		command(streamID, CMD_STOP, 0, 0, 0);
	}

	public final void setVolume(int streamID, float leftVolume, float rightVolume) {
		if (!validVolume(leftVolume, rightVolume))
			leftVolume = rightVolume = 1.f;
		command(streamID, CMD_VOLUME, 0, leftVolume, rightVolume);
	}

	public void setVolume(int streamID, float volume) {
		setVolume(streamID, volume, volume);
	}

	public final void setPriority(int streamID, int priority) {
		command(streamID, CMD_PRIORITY, priority, 0, 0);
	}

	public final void setLoop(int streamID, int loop) {
		command(streamID, CMD_LOOP, Math.max(loop, -1), 0, 0);
	}

	public final void setRate(int streamID, float rate) {
		command(streamID, CMD_RATE, 0, rate(rate), 0);
	}

	/* AOSP: the listener is called on the looper of the thread that sets it, else on the main looper */
	public void setOnLoadCompleteListener(OnLoadCompleteListener listener) {
		if (listener == null) {
			eventHandler = null;
			onLoadCompleteListener = null;
			return;
		}
		Looper looper = Looper.myLooper();
		if (looper == null)
			looper = Looper.getMainLooper();
		onLoadCompleteListener = listener;
		eventHandler = looper != null ? new Handler(looper) : null;
	}

	public static class Builder {
		private int mMaxStreams = 1;
		private AudioAttributes mAudioAttributes;

		public Builder() {}

		public Builder setMaxStreams(int maxStreams) throws IllegalArgumentException {
			if (maxStreams <= 0)
				throw new IllegalArgumentException("Strictly positive value required for the maximum number of streams");
			mMaxStreams = maxStreams;
			return this;
		}

		public Builder setAudioAttributes(AudioAttributes attributes) throws IllegalArgumentException {
			if (attributes == null)
				throw new IllegalArgumentException("Invalid null AudioAttributes");
			mAudioAttributes = attributes;
			return this;
		}

		public Builder setAudioSessionId(int sessionId) {
			if (sessionId != 0 /* AudioManager.AUDIO_SESSION_ID_GENERATE */ && sessionId < 1)
				throw new IllegalArgumentException("Invalid audio session ID " + sessionId);
			return this;
		}

		public Builder setContext(Context context) {
			if (context == null)
				throw new NullPointerException();
			return this;
		}

		public SoundPool build() {
			return new SoundPool(mMaxStreams, AudioManager.STREAM_MUSIC, 0);
		}
	}

	private static native long native_setup(int maxStreams);
	private static native int native_decode(long pool, int sampleId, int fd, long offset, long length);
	private static native boolean native_unload(long pool, int sampleId);
	private static native int native_play(long pool, int sampleId, float left, float right, int priority, int loop, float rate);
	private static native void native_command(long pool, int streamID, int cmd, int i, float a, float b);
	private static native void native_autoPause(long pool, boolean pause);
	private static native void native_release(long pool);
}
