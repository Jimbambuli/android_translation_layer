package android.graphics;

/* AOSP android.graphics.SurfaceTexture, the consumer side: updateTexImage() latches the newest frame into the
 * external texture of the GL context the SurfaceTexture is used with (GLConsumer). ATL has no producer for it
 * yet (Surface(SurfaceTexture) gets no frames), so as in AOSP with an empty queue updateTexImage() only binds
 * the texture and getTimestamp() stays 0; onFrameAvailable() is never called. */
public class SurfaceTexture {

	public static interface OnFrameAvailableListener {
		void onFrameAvailable(SurfaceTexture surfaceTexture);
	}

	private final int texName;
	/* GLConsumer's mEglDisplay/mEglContext: the EGL context of the first updateTexImage() */
	private long eglDisplay;
	private long eglContext;
	private long timestamp;
	private boolean released;
	private OnFrameAvailableListener listener;

	public SurfaceTexture(int texName) {
		this.texName = texName;
	}

	public SurfaceTexture(int texName, boolean singleBufferMode) {
		this(texName);
	}

	public void setOnFrameAvailableListener(OnFrameAvailableListener listener) {
		this.listener = listener;
	}

	public void setOnFrameAvailableListener(OnFrameAvailableListener listener, android.os.Handler handler) {
		this.listener = listener;
	}

	/* AOSP SurfaceTexture.updateTexImage() (GLConsumer::updateTexImage()) */
	public void updateTexImage() {
		if (released)
			throw new RuntimeException("Error during updateTexImage (see logcat for details)"); // NO_INIT: abandoned
		if (!nativeUpdateTexImage(texName))
			throw new IllegalStateException("Unable to update texture contents (see logcat for details)");
	}

	/* the presentation time of the latched frame in nanoseconds (no frame: 0) */
	public long getTimestamp() {
		return timestamp;
	}

	public void release() {
		released = true;
	}

	public boolean isReleased() {
		return released;
	}

	private native boolean nativeUpdateTexImage(int texName);
}
