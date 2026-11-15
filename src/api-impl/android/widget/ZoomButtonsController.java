package android.widget;

import android.view.View;

public class ZoomButtonsController {
	/* AOSP ZoomButtonsController shows a ZoomControls container in its own window over the owner view
	 * (android-14 core/java/android/widget/ZoomButtonsController.java). ATL has no such window, so
	 * visibility is only kept as state, like a device that never gets to show it. */
	private final View mOwnerView;
	private boolean mIsVisible;
	private OnZoomListener mCallback;

	public ZoomButtonsController(View ownerView) {
		mOwnerView = ownerView;
	}

	public void setOnZoomListener(OnZoomListener listener) {
		mCallback = listener;
	}

	public boolean isVisible() {
		return mIsVisible;
	}

	public void setVisible(boolean visible) {
		if (visible != mIsVisible) {
			mIsVisible = visible;
			if (mCallback != null) {
				mCallback.onVisibilityChanged(visible);
			}
		}
	}

	public boolean onTouch(View v, android.view.MotionEvent event) {
		return false;
	}

	public interface OnZoomListener {
		void onVisibilityChanged(boolean visible);
		void onZoom(boolean zoomIn);
	}
}
