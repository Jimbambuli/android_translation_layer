package android.view;

import android.atl.GskCanvas;
import android.atl.annotation.Export;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import java.util.ArrayList;

public class SurfaceView extends View {

	final ArrayList<SurfaceHolder.Callback> mCallbacks = new ArrayList<SurfaceHolder.Callback>();

	public SurfaceView(Context context) {
		this(context, null);
	}

	public SurfaceView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public SurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
		this(context, attrs, defStyleAttr, 0);
	}

	public SurfaceView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		this(context, attrs, defStyleAttr, defStyleRes, false);
	}

	/** @hide AOSP: disableBackgroundLayer only controls the background layer of the window compositor, which ATL does not have */
	public SurfaceView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes, boolean disableBackgroundLayer) {
		super(context, attrs, defStyleAttr, defStyleRes);
		setWillNotDraw(true);

		mSurface.widget = this.widget;
	}

	@Export
	private void surfaceChanged(int format, int width, int height) {
		for (SurfaceHolder.Callback c : mCallbacks) {
			c.surfaceChanged(mSurfaceHolder, format, width, height);
		}
	}

	@Export
	private void surfaceCreated() {
		for (SurfaceHolder.Callback c : mCallbacks) {
			c.surfaceCreated(mSurfaceHolder);
		}
	}

	@Override
	protected native long native_constructor(Context context, AttributeSet attrs);

	protected static native long native_createSnapshot();
	protected static native void native_postSnapshot(long surfaceView, long snapshot);
	/* for an EGL window on this view's surface in another process (Surface.Endpoint): the socket for its buffers */
	static native int native_connectRemote(long surfaceView);
	/* the same for a SurfaceTexture: its frames are queued to it (SurfaceTexture.queueBuffer()) */
	static native int native_connectRemoteTexture(android.graphics.SurfaceTexture surfaceTexture);

	public SurfaceHolder getHolder() {
		return mSurfaceHolder;
	}

	private SurfaceControl mSurfaceControl;

	/* AOSP SurfaceView.getSurfaceControl() (API 29): the layer of this view's surface */
	public SurfaceControl getSurfaceControl() {
		if (mSurfaceControl == null)
			mSurfaceControl = new SurfaceControl(this, null, "SurfaceView");
		return mSurfaceControl;
	}

	final Surface mSurface = new Surface();

	private final SurfaceHolder mSurfaceHolder = new SurfaceHolder() {
		private static final String LOG_TAG = "SurfaceHolder";

		@Override
		public boolean isCreating() {
			//		return mIsCreating;
			return false;
		}

		@Override
		public void addCallback(Callback callback) {
			synchronized (mCallbacks) {
				if (mCallbacks.contains(callback) == false) {
					mCallbacks.add(callback);
				}
			}
		}

		@Override
		public void removeCallback(Callback callback) {
			/*		synchronized (mCallbacks) {
					mCallbacks.remove(callback);
					}*/
		}

		@Override
		public void setFixedSize(int width, int height) {
			/*		if (mRequestedWidth != width || mRequestedHeight != height) {
					mRequestedWidth = width;
					mRequestedHeight = height;
					requestLayout();
					}*/
		}

		@Override
		public void setSizeFromLayout() {
			/*		if (mRequestedWidth != -1 || mRequestedHeight != -1) {
					mRequestedWidth = mRequestedHeight = -1;
					requestLayout();
					}*/
		}

		@Override
		public void setFormat(int format) {
			/*
					// for backward compatibility reason, OPAQUE always
					// means 565 for SurfaceView
					if (format == PixelFormat.OPAQUE)
					format = PixelFormat.RGB_565;

					mRequestedFormat = format;
					if (mWindow != null) {
					updateWindow(false, false);
					}*/
		}

		/**
		 * @deprecated setType is now ignored.
		 */
		@Override
		@Deprecated
		public void setType(int type) {}

		@Override
		public void setKeepScreenOn(boolean screenOn) {
			//		Message msg = mHandler.obtainMessage(KEEP_SCREEN_ON_MSG);
			//		msg.arg1 = screenOn ? 1 : 0;
			//		mHandler.sendMessage(msg);
		}

		/**
		 * Gets a {@link Canvas} for drawing into the SurfaceView's Surface
		 *
		 * After drawing into the provided {@link Canvas}, the caller must
		 * invoke {@link #unlockCanvasAndPost} to post the new contents to the surface.
		 *
		 * The caller must redraw the entire surface.
		 * @return A canvas for drawing into the surface.
		 */
		@Override
		public Canvas lockCanvas() {
			return internalLockCanvas(null);
		}

		/**
		 * Gets a {@link Canvas} for drawing into the SurfaceView's Surface
		 *
		 * After drawing into the provided {@link Canvas}, the caller must
		 * invoke {@link #unlockCanvasAndPost} to post the new contents to the surface.
		 *
		 * @param inOutDirty A rectangle that represents the dirty region that the caller wants
		 * to redraw.  This function may choose to expand the dirty rectangle if for example
		 * the surface has been resized or if the previous contents of the surface were
		 * not available.  The caller must redraw the entire dirty region as represented
		 * by the contents of the inOutDirty rectangle upon return from this function.
		 * The caller may also pass <code>null</code> instead, in the case where the
		 * entire surface should be redrawn.
		 * @return A canvas for drawing into the surface.
		 */
		@Override
		public Canvas lockCanvas(Rect inOutDirty) {
			return internalLockCanvas(inOutDirty);
		}

		private final Canvas internalLockCanvas(Rect dirty) {
			/*		mSurfaceLock.lock();

					if (DEBUG) Log.i(TAG, "Locking canvas... stopped="
						+ mDrawingStopped + ", win=" + mWindow);

					Canvas c = null;
					if (!mDrawingStopped && mWindow != null) {
					try {
						c = mSurface.lockCanvas(dirty);
					} catch (Exception e) {
						Log.e(LOG_TAG, "Exception locking surface", e);
					}
					}

					if (DEBUG) Log.i(TAG, "Returned canvas: " + c);
					if (c != null) {
					mLastLockTime = SystemClock.uptimeMillis();
					return c;
					}

					// If the Surface is not ready to be drawn, then return null,
					// but throttle calls to this function so it isn't called more
					// than every 100ms.
					long now = SystemClock.uptimeMillis();
					long nextTime = mLastLockTime + 100;
					if (nextTime > now) {
					try {
						Thread.sleep(nextTime-now);
					} catch (InterruptedException e) {
					}
					now = SystemClock.uptimeMillis();
					}
					mLastLockTime = now;
					mSurfaceLock.unlock();
			*/
			if (getWidth() == 0 || getHeight() == 0)
				return null;

			return new GskCanvas(native_createSnapshot());
		}

		/**
		 * Posts the new contents of the {@link Canvas} to the surface and
		 * releases the {@link Canvas}.
		 *
		 * @param canvas The canvas previously obtained from {@link #lockCanvas}.
		 */
		@Override
		public void unlockCanvasAndPost(Canvas canvas) {
			native_postSnapshot(widget, ((GskCanvas)canvas).snapshot);
			//		mSurface.unlockCanvasAndPost(canvas);
			//		mSurfaceLock.unlock();
		}

		@Override
		public Surface getSurface() {
			return mSurface;
		}

		@Override
		public Rect getSurfaceFrame() {
			/* AOSP's mSurfaceFrame: the surface has the view's size (ATL ignores setFixedSize()) */
			return new Rect(0, 0, getWidth(), getHeight());
		}
	};

	/* AOSP SurfaceView.SURFACE_LIFECYCLE_* and setSurfaceLifecycle(): AOSP applies the requested
	 * strategy at the next updateSurface(). ATL's surface is the GTK widget's and follows its
	 * realization, so the request is stored only. */
	public static final int SURFACE_LIFECYCLE_DEFAULT = 0;
	public static final int SURFACE_LIFECYCLE_FOLLOWS_VISIBILITY = 1;
	public static final int SURFACE_LIFECYCLE_FOLLOWS_ATTACHMENT = 2;

	private int mRequestedSurfaceLifecycleStrategy = SURFACE_LIFECYCLE_DEFAULT;

	public void setSurfaceLifecycle(int lifecycleStrategy) {
		mRequestedSurfaceLifecycleStrategy = lifecycleStrategy;
	}

	/* AOSP SurfaceView sublayer (WindowManagerPolicyConstants): below the window by default, above it on top.
	 * ATL draws the surface as a GTK widget in view order, so the requested sublayer is kept only */
	private static final int APPLICATION_MEDIA_SUBLAYER = -2;
	private static final int APPLICATION_MEDIA_OVERLAY_SUBLAYER = -1;
	private static final int APPLICATION_PANEL_SUBLAYER = 1;

	private int mRequestedSubLayer = APPLICATION_MEDIA_SUBLAYER;

	public void setZOrderOnTop(boolean onTop) {
		mRequestedSubLayer = onTop ? APPLICATION_PANEL_SUBLAYER : APPLICATION_MEDIA_SUBLAYER;
	}

	public boolean isZOrderedOnTop() {
		return mRequestedSubLayer > 0;
	}

	public void setZOrderMediaOverlay(boolean mediaOverlay) {
		mRequestedSubLayer = mediaOverlay ? APPLICATION_MEDIA_OVERLAY_SUBLAYER : APPLICATION_MEDIA_SUBLAYER;
	}

	/* AOSP: keeps the surface out of screenshots and non-secure displays; there are none to keep it from here */
	public void setSecure(boolean isSecure) {
	}
}
