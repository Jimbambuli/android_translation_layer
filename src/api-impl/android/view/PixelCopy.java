package android.view;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Handler;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/* AOSP PixelCopy copies from a Surface's buffer on the render thread. ATL has no readable buffer behind a
 * Surface (lockCanvas()/unlockCanvasAndPost() hand a snapshot to the GTK widget), so:
 * - a Window (or a View) is copied by drawing its view hierarchy into a Bitmap canvas, like ChangeBounds does;
 *   the content of SurfaceViews/TextureViews inside it is separate GTK widgets and is not in the copy
 * - a Surface/SurfaceView has no data to copy: ERROR_SOURCE_NO_DATA, AOSP's result for a surface that never
 *   got a buffer
 * The copy runs on the calling thread (the drawing needs the UI thread); the result is delivered later. */
public final class PixelCopy {

	public static final int SUCCESS = 0;
	public static final int ERROR_UNKNOWN = 1;
	public static final int ERROR_TIMEOUT = 2;
	public static final int ERROR_SOURCE_NO_DATA = 3;
	public static final int ERROR_SOURCE_INVALID = 4;
	public static final int ERROR_DESTINATION_INVALID = 5;

	public interface OnPixelCopyFinishedListener {
		void onPixelCopyFinished(int copyResult);
	}

	public static void request(SurfaceView source, Bitmap dest, OnPixelCopyFinishedListener listener, Handler listenerThread) {
		request(source.getHolder().getSurface(), dest, listener, listenerThread);
	}

	public static void request(SurfaceView source, Rect srcRect, Bitmap dest, OnPixelCopyFinishedListener listener, Handler listenerThread) {
		request(source.getHolder().getSurface(), srcRect, dest, listener, listenerThread);
	}

	public static void request(Surface source, Bitmap dest, OnPixelCopyFinishedListener listener, Handler listenerThread) {
		request(source, null, dest, listener, listenerThread);
	}

	public static void request(Surface source, Rect srcRect, Bitmap dest, OnPixelCopyFinishedListener listener, Handler listenerThread) {
		validateBitmapDest(dest);
		if (!source.isValid()) {
			throw new IllegalArgumentException("Surface isn't valid, source.isValid() == false");
		}
		if (srcRect != null && srcRect.isEmpty()) {
			throw new IllegalArgumentException("sourceRect is empty");
		}
		// no buffer of the surface can be read back in ATL
		listenerThread.post(() -> listener.onPixelCopyFinished(ERROR_SOURCE_NO_DATA));
	}

	public static void request(Window source, Bitmap dest, OnPixelCopyFinishedListener listener, Handler listenerThread) {
		request(source, null, dest, listener, listenerThread);
	}

	public static void request(Window source, Rect srcRect, Bitmap dest, OnPixelCopyFinishedListener listener, Handler listenerThread) {
		validateBitmapDest(dest);
		View root = rootForWindow(source);
		int result = copyView(root, srcRect, dest);
		listenerThread.post(() -> listener.onPixelCopyFinished(result));
	}

	private static void validateBitmapDest(Bitmap bitmap) {
		if (bitmap == null) {
			throw new IllegalArgumentException("Bitmap cannot be null");
		}
		if (bitmap.isRecycled()) {
			throw new IllegalArgumentException("Bitmap is recycled");
		}
		if (!bitmap.isMutable()) {
			throw new IllegalArgumentException("Bitmap is immutable");
		}
	}

	/* AOSP sourceForWindow(): the window needs a decor view and a backing surface; here the backing
	 * surface is the laid-out decor view (AOSP's check is the ViewRootImpl's Surface) */
	private static View rootForWindow(Window source) {
		if (source == null) {
			throw new IllegalArgumentException("source is null");
		}
		View decor = source.peekDecorView();
		if (decor == null) {
			throw new IllegalArgumentException("Only able to copy windows with decor views");
		}
		if (decor.getWidth() <= 0 || decor.getHeight() <= 0) {
			throw new IllegalArgumentException("Window doesn't have a backing surface!");
		}
		return decor;
	}

	/* the part of AOSP's native copy (CanvasContext::copySurfaceInto): the source rect is clipped to the source,
	 * and scaled to the destination's size; a source rect outside the source gives ERROR_SOURCE_INVALID (assumed, not checked against hwui).
	 * Returns the status */
	private static int copyView(View root, Rect srcRect, Bitmap dest) {
		int width = root.getWidth();
		int height = root.getHeight();
		Rect src = new Rect(0, 0, width, height);
		if (srcRect != null && !srcRect.isEmpty() && !src.intersect(srcRect)) {
			return ERROR_SOURCE_INVALID;
		}
		try {
			Bitmap full = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
			root.draw(new Canvas(full));
			new Canvas(dest).drawBitmap(full, src, new Rect(0, 0, dest.getWidth(), dest.getHeight()), null);
			full.recycle();
			return SUCCESS;
		} catch (RuntimeException e) {
			android.util.Log.w("PixelCopy", "drawing the window failed: " + e);
			return ERROR_UNKNOWN;
		}
	}

	public static final class Result {
		private final int mStatus;
		private final Bitmap mBitmap;

		private Result(int status, Bitmap bitmap) {
			mStatus = status;
			mBitmap = bitmap;
		}

		public int getStatus() {
			return mStatus;
		}

		public Bitmap getBitmap() {
			if (mStatus != SUCCESS) {
				throw new IllegalStateException("Copy request didn't succeed, status = " + mStatus);
			}
			return mBitmap;
		}
	}

	public static final class Request {
		private final Surface mSource; // null for a window
		private final View mSourceView; // the decor view of a window
		private Rect mSrcRect;
		private Bitmap mDest;

		private Request(Surface source, View sourceView) {
			mSource = source;
			mSourceView = sourceView;
		}

		public static final class Builder {
			private Request mRequest;

			private Builder(Request request) {
				mRequest = request;
			}

			public static Builder ofWindow(Window source) {
				return new Builder(new Request(null, rootForWindow(source)));
			}

			public static Builder ofWindow(View source) {
				if (source == null || !source.isAttachedToWindow()) {
					throw new IllegalArgumentException("View must not be null & must be attached to window");
				}
				// AOSP copies the window of the view; its root view is what ATL draws
				return new Builder(new Request(null, source.getRootView()));
			}

			public static Builder ofSurface(Surface source) {
				if (source == null || !source.isValid()) {
					throw new IllegalArgumentException("Source must not be null & must be valid");
				}
				return new Builder(new Request(source, null));
			}

			public static Builder ofSurface(SurfaceView source) {
				return ofSurface(source.getHolder().getSurface());
			}

			private void requireNotBuilt() {
				if (mRequest == null) {
					throw new IllegalStateException("build() already called on this builder");
				}
			}

			public Builder setSourceRect(Rect srcRect) {
				requireNotBuilt();
				mRequest.mSrcRect = srcRect;
				return this;
			}

			public Builder setDestinationBitmap(Bitmap destination) {
				requireNotBuilt();
				if (destination != null) {
					validateBitmapDest(destination);
				}
				mRequest.mDest = destination;
				return this;
			}

			public Request build() {
				requireNotBuilt();
				Request ret = mRequest;
				mRequest = null;
				return ret;
			}
		}

		public Bitmap getDestinationBitmap() {
			return mDest;
		}

		public Rect getSourceRect() {
			return mSrcRect;
		}

		public void request(Executor callbackExecutor, Consumer<Result> listener) {
			if (mSource != null) {
				// AOSP: ERROR_SOURCE_INVALID for a released surface, else the copy; ATL has no data to copy
				int status = mSource.isValid() ? ERROR_SOURCE_NO_DATA : ERROR_SOURCE_INVALID;
				callbackExecutor.execute(() -> listener.accept(new Result(status, null)));
				return;
			}
			// AOSP CopyRequest.getDestinationBitmap(): without a destination one of the source rect's size
			// (the whole source for none) is made
			Bitmap dest = mDest;
			int status;
			if (dest == null) {
				Rect all = new Rect(0, 0, mSourceView.getWidth(), mSourceView.getHeight());
				if (mSrcRect != null && !mSrcRect.isEmpty())
					all.intersect(mSrcRect);
				if (all.isEmpty()) {
					status = ERROR_SOURCE_INVALID;
				} else {
					dest = Bitmap.createBitmap(all.width(), all.height(), Bitmap.Config.ARGB_8888);
					status = copyView(mSourceView, mSrcRect, dest);
				}
			} else {
				status = copyView(mSourceView, mSrcRect, dest);
			}
			Bitmap result = dest;
			callbackExecutor.execute(() -> listener.accept(new Result(status, result)));
		}
	}

	public static void request(Request request, Executor callbackExecutor, Consumer<Result> listener) {
		request.request(callbackExecutor, listener);
	}

	private PixelCopy() {}
}
