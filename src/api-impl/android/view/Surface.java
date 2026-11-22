package android.view;

import android.annotation.IntDef;
import android.graphics.SurfaceTexture;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class Surface implements android.os.Parcelable {

	/** @hide */
	@IntDef(prefix = {"ROTATION_"}, value = {ROTATION_0, ROTATION_90, ROTATION_180, ROTATION_270})
	@Retention(RetentionPolicy.SOURCE)
	public @interface Rotation {}

	/** Rotation constant: 0 degree rotation (natural orientation) */
	public static final int ROTATION_0 = 0;

	/** Rotation constant: 90 degree rotation. */
	public static final int ROTATION_90 = 1;

	/** Rotation constant: 180 degree rotation. */
	public static final int ROTATION_180 = 2;

	/** Rotation constant: 270 degree rotation. */
	public static final int ROTATION_270 = 3;

	public long widget;
	/* the SurfaceTexture of Surface(SurfaceTexture): its frames go to that consumer (e.g. a TextureView) */
	public SurfaceTexture surfaceTexture;
	private android.graphics.Bitmap lockedBitmap;

	/* a Surface read in another process of the app (Surface.CREATOR there): the endpoint in the process that
	 * shows it, through which that process gets the buffers drawn here (atl_connect()) */
	public android.os.IBinder atl_endpoint;
	/* the PixelFormat of the buffers (AOSP: the format of the surface's BufferQueue); a SurfaceView sets its own,
	 * the native ANativeWindow_getFormat() reads it */
	int atl_format = android.graphics.PixelFormat.RGBA_8888;
	private Endpoint endpoint;
	/* the Surface of an ImageReader (AOSP: the producer side of the reader's BufferQueue): the camera queues its
	 * images there */
	public android.media.ImageReader atl_imageReader;

	/* AOSP Surface.writeToParcel()/readFromParcel()/CREATOR: a Surface travels as the binder of its buffer queue
	 * (IGraphicBufferProducer). Here it is the binder of an Endpoint for the widget or SurfaceTexture it draws
	 * into: read in the same process, it gives a Surface on those again; read in another process, a Surface
	 * whose EGL window sends its buffers to that widget (ANativeWindow_fromSurface()). */
	public static final android.os.Parcelable.Creator<Surface> CREATOR = new android.os.Parcelable.Creator<Surface>() {
		@Override
		public Surface createFromParcel(android.os.Parcel source) {
			try {
				Surface s = new Surface();
				s.readFromParcel(source);
				return s;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public Surface[] newArray(int size) {
			return new Surface[size];
		}
	};

	public void readFromParcel(android.os.Parcel source) {
		if (source == null) {
			throw new IllegalArgumentException("source must not be null");
		}
		source.readString(); // the name AOSP gives a surface of a view, none here
		android.os.IBinder binder = source.readStrongBinder();
		widget = 0;
		surfaceTexture = null;
		atl_endpoint = null;
		if (binder instanceof Endpoint) {
			widget = ((Endpoint)binder).widget;
			surfaceTexture = ((Endpoint)binder).surfaceTexture;
		} else {
			atl_endpoint = binder;
		}
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(android.os.Parcel dest, int flags) {
		if (dest == null) {
			throw new IllegalArgumentException("dest must not be null");
		}
		dest.writeString(null);
		if (atl_endpoint != null) {
			dest.writeStrongBinder(atl_endpoint);
		} else if (widget != 0 || surfaceTexture != null) {
			if (endpoint == null || endpoint.widget != widget || endpoint.surfaceTexture != surfaceTexture)
				endpoint = new Endpoint(widget, surfaceTexture);
			dest.writeStrongBinder(endpoint);
		} else {
			dest.writeStrongBinder(null);
		}
	}

	/* the binder a Surface is parceled as: in another process, CONNECT gives the socket for the buffers of an EGL
	 * window on the Surface (SurfaceView.native_connectRemote()) */
	static class Endpoint extends android.os.Binder {
		static final int CONNECT = android.os.IBinder.FIRST_CALL_TRANSACTION;
		final long widget;
		final SurfaceTexture surfaceTexture;

		Endpoint(long widget, SurfaceTexture surfaceTexture) {
			this.widget = widget;
			this.surfaceTexture = surfaceTexture;
		}

		@Override
		protected boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException {
			if (code != CONNECT)
				return super.onTransact(code, data, reply, flags);
			int fd = widget != 0 ? SurfaceView.native_connectRemote(widget)
			       : surfaceTexture != null ? SurfaceView.native_connectRemoteTexture(surfaceTexture) : -1;
			reply.writeInt(fd >= 0 ? 0 : -1);
			if (fd >= 0)
				reply.atl_writeOwnedFileDescriptor(android.os.ParcelFileDescriptor.adoptFd(fd).getFileDescriptor());
			return true;
		}
	}

	/* called from ANativeWindow_fromSurface() for a Surface without a widget of this process: the socket to the widget
	 * of another process or to the SurfaceTexture (its consumer), or -1 (a released Surface) */
	private int atl_connect() {
		if (atl_endpoint == null)
			return surfaceTexture != null && !surfaceTexture.isReleased() ? SurfaceView.native_connectRemoteTexture(surfaceTexture) : -1;
		android.os.Parcel data = android.os.Parcel.obtain();
		android.os.Parcel reply = android.os.Parcel.obtain();
		try {
			if (atl_endpoint == null || !atl_endpoint.transact(Endpoint.CONNECT, data, reply, 0) || reply.readInt() != 0)
				return -1;
			android.os.ParcelFileDescriptor pfd = reply.readFileDescriptor();
			return pfd != null ? pfd.detachFd() : -1;
		} catch (android.os.RemoteException e) {
			android.util.Log.w("Surface", "the process showing the surface is gone: " + e);
			return -1;
		} finally {
			reply.recycle();
			data.recycle();
		}
	}

	public Surface() {}

	/* AOSP Surface(SurfaceControl) (API 29): draws into the widget of the SurfaceView the layer belongs to */
	public Surface(SurfaceControl from) {
		widget = from.atl_widget();
		atl_endpoint = from.atl_remoteEndpoint();
	}

	public Surface(SurfaceTexture surfaceTexture) {
		this.surfaceTexture = surfaceTexture;
	}

	public boolean isValid() {
		return widget != 0 || (surfaceTexture != null && !surfaceTexture.isReleased()) || (atl_endpoint != null && atl_endpoint.isBinderAlive()) || (atl_imageReader != null && atl_imageReader.isValidReader());
	}

	public void release() {
		surfaceTexture = null;
		atl_endpoint = null;
		atl_imageReader = null;
	}

	/* AOSP Surface.lockCanvas()/lockHardwareCanvas()/unlockCanvasAndPost(): the canvas draws into a snapshot, which
	 * unlockCanvasAndPost() hands to the SurfaceView the surface belongs to (as SurfaceHolder.lockCanvas() does).
	 * AOSP locks the buffer for software drawing; there is only the one kind of canvas here. */
	public android.graphics.Canvas lockCanvas(android.graphics.Rect inOutDirty) {
		if (!isValid())
			throw new IllegalStateException("Surface has no native object.");
		if (atl_endpoint != null)
			throw new UnsupportedOperationException("software drawing into a surface of another process is not implemented");
		if (widget == 0) { // a SurfaceTexture's: a buffer of its default size
			lockedBitmap = android.graphics.Bitmap.createBitmap(surfaceTexture.getDefaultBufferWidth(), surfaceTexture.getDefaultBufferHeight(), android.graphics.Bitmap.Config.ARGB_8888);
			return new android.graphics.Canvas(lockedBitmap);
		}
		return new android.atl.GskCanvas(SurfaceView.native_createSnapshot());
	}

	public android.graphics.Canvas lockHardwareCanvas() {
		return lockCanvas(null);
	}

	public void unlockCanvasAndPost(android.graphics.Canvas canvas) {
		if (canvas == null)
			throw new IllegalArgumentException("canvas must not be null");
		if (widget == 0 && lockedBitmap != null) {
			if (!isValid())
				throw new IllegalStateException("Surface has no native object.");
			surfaceTexture.queueBitmap(lockedBitmap, System.nanoTime());
			lockedBitmap = null;
			return;
		}
		if (!(canvas instanceof android.atl.GskCanvas))
			throw new IllegalArgumentException("canvas object must be the same instance that was previously returned by lockCanvas");
		if (!isValid())
			throw new IllegalStateException("Surface has no native object.");
		SurfaceView.native_postSnapshot(widget, ((android.atl.GskCanvas)canvas).snapshot);
	}

	/* API 30/31: only a hint for the display's refresh rate, which ATL does not adjust */
	public static final int FRAME_RATE_COMPATIBILITY_DEFAULT = 0;
	public static final int FRAME_RATE_COMPATIBILITY_FIXED_SOURCE = 1;
	public static final int CHANGE_FRAME_RATE_ONLY_IF_SEAMLESS = 0;
	public static final int CHANGE_FRAME_RATE_ALWAYS = 1;

	public void setFrameRate(float frameRate, int compatibility) {}

	public void setFrameRate(float frameRate, int compatibility, int changeFrameRateStrategy) {}

	/* AOSP Surface.FrameRateParams: the values only; ATL has no display refresh rate control, so setFrameRate does nothing */
	public static class FrameRateParams {
		private FrameRateParams() {}

		public static final FrameRateParams IGNORE = new FrameRateParams.Builder().setDesiredRateRange(0f, Float.MAX_VALUE).build();

		public static final class Builder {
			private float mDesiredMinRate;
			private float mDesiredMaxRate;
			private float mFixedSourceRate;
			private int mChangeFrameRateStrategy;

			public Builder() {}

			public Builder setDesiredRateRange(float desiredMinRate, float desiredMaxRate) {
				if (desiredMaxRate < desiredMinRate) {
					System.err.println("Surface: Failed to set desired frame rate range. desiredMaxRate should be greater than or equal to desiredMinRate");
					return this;
				}
				mDesiredMinRate = desiredMinRate;
				mDesiredMaxRate = desiredMaxRate;
				return this;
			}

			public Builder setFixedSourceRate(float fixedSourceRate) {
				mFixedSourceRate = fixedSourceRate;
				return this;
			}

			public Builder setChangeFrameRateStrategy(int changeFrameRateStrategy) {
				mChangeFrameRateStrategy = changeFrameRateStrategy;
				return this;
			}

			public FrameRateParams build() {
				FrameRateParams frameRate = new FrameRateParams();
				frameRate.mDesiredMinRate = this.mDesiredMinRate;
				frameRate.mDesiredMaxRate = this.mDesiredMaxRate;
				frameRate.mFixedSourceRate = this.mFixedSourceRate;
				frameRate.mChangeFrameRateStrategy = this.mChangeFrameRateStrategy;
				return frameRate;
			}
		}

		public float getDesiredMinRate() {
			return mDesiredMinRate;
		}

		public float getDesiredMaxRate() {
			return mDesiredMaxRate;
		}

		public float getFixedSourceRate() {
			return mFixedSourceRate;
		}

		public int getChangeFrameRateStrategy() {
			return mChangeFrameRateStrategy;
		}

		float mDesiredMinRate;
		float mDesiredMaxRate;
		float mFixedSourceRate;
		int mChangeFrameRateStrategy;
	}

	public void setFrameRate(FrameRateParams frameRateParams) {}
}
