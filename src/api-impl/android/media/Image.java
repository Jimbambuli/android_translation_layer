package android.media;

import android.graphics.Rect;
import java.nio.ByteBuffer;

/* AOSP android.media.Image without the parts that need HardwareBuffer, SyncFence or DataSpace.
 * ATL has no ImageReader or ImageWriter that would hand out images; the class is needed because the Flutter
 * engine looks it up (and its close()) when it starts and aborts if it is missing. */
public abstract class Image implements AutoCloseable {

	protected Image() {}

	public abstract int getFormat();

	public abstract int getWidth();

	public abstract int getHeight();

	public abstract long getTimestamp();

	public abstract Plane[] getPlanes();

	@Override
	public abstract void close();

	public int getPlaneCount() {
		return getPlanes().length;
	}

	public void setTimestamp(long timestamp) {}

	private Rect mCropRect;

	public Rect getCropRect() {
		if (mCropRect == null)
			return new Rect(0, 0, getWidth(), getHeight());
		return new Rect(mCropRect);
	}

	public void setCropRect(Rect cropRect) {
		if (cropRect != null) {
			cropRect = new Rect(cropRect);
			if (!cropRect.intersect(0, 0, getWidth(), getHeight()))
				cropRect.setEmpty();
		}
		mCropRect = cropRect;
	}

	public static abstract class Plane {
		protected Plane() {}

		public abstract int getRowStride();

		public abstract int getPixelStride();

		public abstract ByteBuffer getBuffer();
	}
}
