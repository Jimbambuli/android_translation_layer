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

	/* AOSP Surface.writeToParcel()/readFromParcel()/CREATOR: a Surface travels as the handle of the native surface it
	 * holds. ATL's Surface is the pointer of the GTK widget it draws into, which is valid in this process only: a
	 * parcel read in this process (what an app does with it, e.g. an in-process Bundle) gives a Surface on the same
	 * widget; there is no other process to read it in. */
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
		widget = source.readLong();
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
		dest.writeLong(widget);
	}

	public Surface() {}

	public Surface(SurfaceTexture surfaceTexture) {
		this.surfaceTexture = surfaceTexture;
	}

	public boolean isValid() {
		return widget != 0 || (surfaceTexture != null && !surfaceTexture.isReleased());
	}

	public void release() {
		surfaceTexture = null;
	}

	/* AOSP Surface.lockCanvas()/lockHardwareCanvas()/unlockCanvasAndPost(): the canvas draws into a snapshot, which
	 * unlockCanvasAndPost() hands to the SurfaceView the surface belongs to (as SurfaceHolder.lockCanvas() does).
	 * AOSP locks the buffer for software drawing; there is only the one kind of canvas here. */
	public android.graphics.Canvas lockCanvas(android.graphics.Rect inOutDirty) {
		if (!isValid())
			throw new IllegalStateException("Surface has no native object.");
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
}
