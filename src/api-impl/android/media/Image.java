package android.media;

import android.graphics.Rect;
import android.hardware.HardwareBuffer;
import android.hardware.SyncFence;
import java.io.IOException;
import java.nio.ByteBuffer;

/* AOSP android.media.Image without the DataSpace part (getHardwareBuffer/getFence/setFence: android-16 Image.java).
 * ATL has no ImageReader or ImageWriter that would hand out images; the class is needed because the Flutter
 * engine looks it up (and its close()) when it starts and aborts if it is missing. */
public abstract class Image implements AutoCloseable {

	protected Image() {}

	protected boolean mIsImageValid = false;

	protected void throwISEIfImageIsInvalid() {
		if (!mIsImageValid) {
			throw new IllegalStateException("Image is already closed");
		}
	}

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

	public SyncFence getFence() throws IOException {
		return SyncFence.createEmpty();
	}

	public HardwareBuffer getHardwareBuffer() {
		throwISEIfImageIsInvalid();
		return null;
	}

	public void setFence(SyncFence fence) throws IOException {
		throwISEIfImageIsInvalid();
	}

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
