package android.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;

/**
 * AOSP ZoomControls (android16 core/java/android/widget/ZoomControls.java). AOSP inflates R.layout.zoom_controls, which is
 * not part of the AOSP sources (it is generated into framework-res with the zoom_plus/zoom_minus bitmaps); the two
 * ZoomButtons are created in code instead, without images.
 */
public class ZoomControls extends LinearLayout {
	private final ZoomButton mZoomIn;
	private final ZoomButton mZoomOut;

	public ZoomControls(Context context) {
		this(context, null);
	}

	public ZoomControls(Context context, AttributeSet attrs) {
		super(context, attrs);
		setFocusable(false);
		setOrientation(HORIZONTAL);
		mZoomIn = new ZoomButton(context);
		mZoomOut = new ZoomButton(context);
		addView(mZoomIn, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		addView(mZoomOut, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
	}

	public void setOnZoomInClickListener(OnClickListener listener) {
		mZoomIn.setOnClickListener(listener);
	}

	public void setOnZoomOutClickListener(OnClickListener listener) {
		mZoomOut.setOnClickListener(listener);
	}

	/** Sets the delay between calls to the click listeners while a button is held, in milliseconds */
	public void setZoomSpeed(long speed) {
		mZoomIn.setZoomSpeed(speed);
		mZoomOut.setZoomSpeed(speed);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		/* Consume all touch events so they don't get dispatched to the view beneath this view. */
		return true;
	}

	public void show() {
		fade(View.VISIBLE, 0.0f, 1.0f);
	}

	public void hide() {
		fade(View.GONE, 1.0f, 0.0f);
	}

	private void fade(int visibility, float startAlpha, float endAlpha) {
		AlphaAnimation anim = new AlphaAnimation(startAlpha, endAlpha);
		anim.setDuration(500);
		startAnimation(anim);
		setVisibility(visibility);
	}

	public void setIsZoomInEnabled(boolean isEnabled) {
		mZoomIn.setEnabled(isEnabled);
	}

	public void setIsZoomOutEnabled(boolean isEnabled) {
		mZoomOut.setEnabled(isEnabled);
	}

	@Override
	public boolean hasFocus() {
		return mZoomIn.hasFocus() || mZoomOut.hasFocus();
	}

	@Override
	public CharSequence getAccessibilityClassName() {
		return ZoomControls.class.getName();
	}
}
