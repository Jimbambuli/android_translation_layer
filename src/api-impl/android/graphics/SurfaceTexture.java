package android.graphics;

import android.os.Handler;
import android.os.Looper;

/* AOSP android.graphics.SurfaceTexture. The producers here (MediaPlayer, MediaCodec, Surface.lockCanvas() of a
 * Surface(SurfaceTexture)) queue frames as textures; onFrameAvailable() follows on the listener's handler,
 * as in AOSP. A consumer latches the newest frame: TextureView draws it, updateTexImage() of a GL consumer
 * puts it into the external GL texture (through an EGLImage of a GL_TEXTURE_2D the pixels are uploaded to).
 * getTimestamp() is the time of the frame latched last. */
public class SurfaceTexture {

	public static interface OnFrameAvailableListener {
		void onFrameAvailable(SurfaceTexture surfaceTexture);
	}

	private final int texName;
	/* GLConsumer's mEglDisplay/mEglContext: the EGL context of the first updateTexImage() */
	private long eglDisplay;
	private long eglContext;
	/* the GL objects of updateTexImage() (native), and whether it has put a frame into the texture */
	private long glImage;
	private boolean glHasFrame;
	private volatile long timestamp;
	private volatile boolean released;
	private volatile OnFrameAvailableListener listener;
	private volatile Handler handler;
	private final Looper creatorLooper = Looper.myLooper();
	private int defaultWidth = 1;
	private int defaultHeight = 1;
	/* guarded by this: the frame queued last (not yet latched), and the one latched */
	private Bitmap queued;
	private long queuedTimestamp;
	private Bitmap latched;

	public SurfaceTexture(int texName) {
		this.texName = texName;
	}

	public SurfaceTexture(int texName, boolean singleBufferMode) {
		this(texName);
	}

	/* API 26: a SurfaceTexture not attached to a GL texture (TextureView's) */
	public SurfaceTexture(boolean singleBufferMode) {
		this(0);
	}

	public void setOnFrameAvailableListener(OnFrameAvailableListener listener) {
		setOnFrameAvailableListener(listener, null);
	}

	/* AOSP: without a handler, the listener runs on the looper of the thread that created the SurfaceTexture,
	 * or else on the main looper */
	public void setOnFrameAvailableListener(OnFrameAvailableListener listener, Handler handler) {
		if (listener != null && handler == null)
			handler = new Handler(creatorLooper != null ? creatorLooper : Looper.getMainLooper());
		this.handler = handler;
		this.listener = listener;
	}

	public void setDefaultBufferSize(int width, int height) {
		defaultWidth = width;
		defaultHeight = height;
	}

	/** @hide the size Surface.lockCanvas() draws in */
	public int getDefaultBufferWidth() {
		return defaultWidth;
	}

	/** @hide */
	public int getDefaultBufferHeight() {
		return defaultHeight;
	}

	/** @hide ATL: the producer side (AOSP's BufferQueueProducer::queueBuffer()): a frame as a GdkTexture,
	 * which the SurfaceTexture takes over; an older frame not latched yet is dropped */
	public void queueBuffer(long texture, long timestampNs) {
		if (texture != 0)
			queueBitmap(new Bitmap(texture), timestampNs);
	}

	/** @hide the frame drawn into a Surface(SurfaceTexture) with lockCanvas() */
	public void queueBitmap(Bitmap frame, long timestampNs) {
		synchronized (this) {
			if (released) {
				frame.recycle();
				return;
			}
			if (queued != null)
				queued.recycle();
			queued = frame;
			queuedTimestamp = timestampNs;
		}
		Handler h = handler;
		OnFrameAvailableListener l = listener;
		if (h != null && l != null)
			h.post(() -> {
				if (listener == l)
					l.onFrameAvailable(this);
			});
	}

	/** @hide TextureView's consumer: makes the frame queued last the current one; false if there is none */
	public boolean latchFrame() {
		synchronized (this) {
			if (queued == null)
				return false;
			if (latched != null)
				latched.recycle();
			latched = queued;
			queued = null;
			timestamp = queuedTimestamp;
			return true;
		}
	}

	/** @hide the frame latched last, or null */
	public synchronized Bitmap getLatchedFrame() {
		return latched;
	}

	/* AOSP SurfaceTexture.updateTexImage() (GLConsumer::updateTexImage()) */
	public void updateTexImage() {
		if (released)
			throw new RuntimeException("Error during updateTexImage (see logcat for details)"); // NO_INIT: abandoned
		synchronized (this) {
			boolean newFrame = latchFrame();
			if (!nativeUpdateTexImage(texName, newFrame ? latched.getTexture() : 0))
				throw new IllegalStateException("Unable to update texture contents (see logcat for details)");
			glHasFrame |= newFrame;
		}
	}

	/* AOSP GLConsumer: identity before the first frame; then the buffers here are uncropped and their first row is
	 * the top of the image, as AOSP's, so the matrix flips the texture coordinates vertically */
	public void getTransformMatrix(float[] mtx) {
		if (mtx.length != 16)
			throw new IllegalArgumentException();
		java.util.Arrays.fill(mtx, 0);
		mtx[0] = mtx[10] = mtx[15] = 1;
		mtx[5] = glHasFrame ? -1 : 1;
		mtx[13] = glHasFrame ? 1 : 0;
	}

	/* the presentation time of the latched frame in nanoseconds (no frame: 0) */
	public long getTimestamp() {
		return timestamp;
	}

	public void release() {
		synchronized (this) {
			released = true;
			nativeRelease();
			if (queued != null)
				queued.recycle();
			if (latched != null)
				latched.recycle();
			queued = null;
			latched = null;
		}
	}

	public boolean isReleased() {
		return released;
	}

	private native boolean nativeUpdateTexImage(int texName, long texture);
	private native void nativeRelease();
}
