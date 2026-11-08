package android.view;

import android.content.Context;
import android.util.AttributeSet;

public class TextureView extends View {

	private SurfaceTextureListener surfaceTextureListener;

	public TextureView(Context context) {
		super(context);
	}

	public TextureView(Context context, AttributeSet attributeSet) {
		super(context, attributeSet);
	}

	public interface SurfaceTextureListener {}

	public void setSurfaceTextureListener(SurfaceTextureListener surfaceTextureListener) {
		this.surfaceTextureListener = surfaceTextureListener;
	}

	public void setOpaque(boolean opaque) {}

	public SurfaceTextureListener getSurfaceTextureListener() {
		return surfaceTextureListener;
	}

	/* AOSP TextureView.isAvailable(): the SurfaceTexture exists once the view is attached to a hardware accelerated
	 * window; ATL's TextureView has no SurfaceTexture, so it never does, and getSurfaceTexture() is null as AOSP's
	 * is before it is available */
	public boolean isAvailable() {
		return false;
	}

	public android.graphics.SurfaceTexture getSurfaceTexture() {
		return null;
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
