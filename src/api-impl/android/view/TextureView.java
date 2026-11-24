package android.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;

/* AOSP android.view.TextureView: on its first draw it creates its SurfaceTexture and reports it with
 * onSurfaceTextureAvailable(); each frame a producer queues (e.g. MediaPlayer through a Surface(SurfaceTexture))
 * invalidates the view, which latches the newest frame, reports onSurfaceTextureUpdated() and draws it over its
 * bounds through the transform. AOSP draws the frame as a hardware layer; here it is the texture of the frame. */
public class TextureView extends View {

	private SurfaceTextureListener surfaceTextureListener;
	private SurfaceTexture mSurface;
	private boolean mUpdateLayer;
	private boolean mOpaque = true;

	private final SurfaceTexture.OnFrameAvailableListener mUpdateListener = surfaceTexture -> {
		mUpdateLayer = true;
		invalidate();
	};

	public TextureView(Context context) {
		super(context);
	}

	public TextureView(Context context, AttributeSet attributeSet) {
		super(context, attributeSet);
	}

	public TextureView(Context context, AttributeSet attributeSet, int defStyleAttr) {
		super(context, attributeSet, defStyleAttr);
	}

	public TextureView(Context context, AttributeSet attributeSet, int defStyleAttr, int defStyleRes) {
		super(context, attributeSet, defStyleAttr, defStyleRes);
	}

	public interface SurfaceTextureListener {
		void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height);
		void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height);
		boolean onSurfaceTextureDestroyed(SurfaceTexture surface);
		void onSurfaceTextureUpdated(SurfaceTexture surface);
	}

	public void setSurfaceTextureListener(SurfaceTextureListener surfaceTextureListener) {
		this.surfaceTextureListener = surfaceTextureListener;
	}

	public SurfaceTextureListener getSurfaceTextureListener() {
		return surfaceTextureListener;
	}

	public void setOpaque(boolean opaque) {
		mOpaque = opaque;
	}

	@Override
	public boolean isOpaque() {
		return mOpaque;
	}

	/* AOSP TextureView.isAvailable(): the SurfaceTexture exists once the view was drawn the first time */
	public boolean isAvailable() {
		return mSurface != null;
	}

	public SurfaceTexture getSurfaceTexture() {
		return mSurface;
	}

	/* AOSP: the view shows the frames of this SurfaceTexture from now on; the old one is released */
	public void setSurfaceTexture(SurfaceTexture surfaceTexture) {
		if (surfaceTexture == null)
			throw new NullPointerException("surfaceTexture must not be null");
		if (surfaceTexture == mSurface)
			throw new IllegalArgumentException("Trying to setSurfaceTexture to the same SurfaceTexture that's already set.");
		if (surfaceTexture.isReleased())
			throw new IllegalArgumentException("Cannot setSurfaceTexture to a released SurfaceTexture");
		if (mSurface != null)
			mSurface.release();
		mSurface = surfaceTexture;
		mSurface.setOnFrameAvailableListener(mUpdateListener, new Handler(Looper.getMainLooper()));
		mUpdateLayer = true;
		invalidate();
	}

	/* AOSP TextureView.getTextureLayer() + applyUpdate() */
	@Override
	public final void draw(Canvas canvas) {
		if (mSurface == null) {
			mSurface = new SurfaceTexture(false);
			mSurface.setDefaultBufferSize(getWidth(), getHeight());
			mSurface.setOnFrameAvailableListener(mUpdateListener, new Handler(Looper.getMainLooper()));
			if (surfaceTextureListener != null)
				surfaceTextureListener.onSurfaceTextureAvailable(mSurface, getWidth(), getHeight());
		}
		if (mUpdateLayer) {
			mUpdateLayer = false;
			if (mSurface.latchFrame() && surfaceTextureListener != null)
				surfaceTextureListener.onSurfaceTextureUpdated(mSurface);
		}
		drawFrame(canvas, getWidth(), getHeight());
	}

	private void drawFrame(Canvas canvas, int width, int height) {
		Bitmap frame = mSurface != null ? mSurface.getLatchedFrame() : null;
		if (frame == null)
			return;
		canvas.save();
		if (mTransform != null)
			canvas.concat(mTransform);
		/* AOSP: the layer is drawn through the SurfaceTexture's transform matrix, which undoes a frame's FLIP_V */
		if (mSurface.isLatchedFrameFlippedV())
			canvas.scale(1, -1, 0, height / 2f);
		canvas.drawBitmap(frame, null, new Rect(0, 0, width, height), null);
		canvas.restore();
	}

	/* AOSP 16 TextureView.onVisibilityChanged(): no frame updates while invisible. ATL creates the
	 * SurfaceTexture together with what AOSP calls the layer (in draw()), so mSurface != null stands
	 * for mLayer != null */
	@Override
	protected void onVisibilityChanged(View changedView, int visibility) {
		super.onVisibilityChanged(changedView, visibility);

		if (mSurface != null) {
			if (visibility == VISIBLE) {
				mSurface.setOnFrameAvailableListener(mUpdateListener, new Handler(Looper.getMainLooper()));
				mUpdateLayer = true;
				invalidate();
			} else {
				mSurface.setOnFrameAvailableListener(null);
			}
		}
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		if (mSurface != null) {
			mSurface.setDefaultBufferSize(w, h);
			if (surfaceTextureListener != null)
				surfaceTextureListener.onSurfaceTextureSizeChanged(mSurface, w, h);
		}
	}

	/* AOSP releaseSurfaceTexture(): the listener decides whether the SurfaceTexture is released */
	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		if (mSurface != null) {
			boolean release = surfaceTextureListener == null || surfaceTextureListener.onSurfaceTextureDestroyed(mSurface);
			if (release)
				mSurface.release();
			mSurface = null;
		}
	}

	/* AOSP TextureView.getBitmap(): the content of the view, scaled to the bitmap */
	public Bitmap getBitmap() {
		return getBitmap(getWidth(), getHeight());
	}

	public Bitmap getBitmap(int width, int height) {
		if (isAvailable() && width > 0 && height > 0)
			return getBitmap(Bitmap.createBitmap(getResources().getDisplayMetrics(), width, height, Bitmap.Config.ARGB_8888));
		return null;
	}

	public Bitmap getBitmap(Bitmap bitmap) {
		if (bitmap != null && isAvailable()) {
			if (mUpdateLayer) {
				mUpdateLayer = false;
				if (mSurface.latchFrame() && surfaceTextureListener != null)
					surfaceTextureListener.onSurfaceTextureUpdated(mSurface);
			}
			drawFrame(new Canvas(bitmap), bitmap.getWidth(), bitmap.getHeight());
		}
		return bitmap;
	}

	private android.graphics.Matrix mTransform;

	/* AOSP TextureView.setTransform(): the matrix that maps the texture onto the view, kept and returned by
	 * getTransform() */
	public void setTransform(android.graphics.Matrix transform) {
		mTransform = transform == null ? null : new android.graphics.Matrix(transform);
		invalidate();
	}

	public android.graphics.Matrix getTransform(android.graphics.Matrix transform) {
		if (transform == null) {
			transform = new android.graphics.Matrix();
		}
		if (mTransform != null) {
			transform.set(mTransform);
		} else {
			transform.reset();
		}
		return transform;
	}
}
