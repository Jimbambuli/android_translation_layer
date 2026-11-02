package android.view;

import android.content.res.Resources;
import android.graphics.Rect;
import android.os.SystemClock;
import android.util.Slog;

public class WindowManagerImpl implements WindowManager, ViewManager {

	private static final String TAG = "WindowManagerImpl";

	/* The parent of a view added with addView(): the root of a window, like AOSP's ViewRootImpl.
	 * GTK lays out and invalidates the window itself, and there is nothing above to intercept
	 * touch events or to take part in nested scrolling. */
	private static class WindowViewParent implements ViewParent {

		@Override
		public android.view.ViewParent getParent() {
			return null;
		}

		@Override
		public boolean isLayoutRequested() {
			return false;
		}

		@Override
		public void childHasTransientStateChanged(View child, boolean hasTransientState) {}

		@Override
		public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {}

		@Override
		public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes) {
			return false;
		}

		@Override
		public boolean onNestedPreFling(View target, float velocityX, float velocityY) {
			return false;
		}

		@Override
		public boolean onNestedFling(View target, float velocityX, float velocityY, boolean consumed) {
			return false;
		}

		@Override
		public void onNestedScrollAccepted(View child, View target, int nestedScrollAxes) {}

		@Override
		public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {}

		@Override
		public void onNestedScroll(View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed) {}

		@Override
		public void onStopNestedScroll(View target) {}

		@Override
		public void onDescendantInvalidated(View child, View target) {}

		@Override
		public void requestLayout() {}
	}

	public android.view.Display getDefaultDisplay() {
		return new android.view.Display();
	}

	/* the window has the size of the display (see Display) and, as a desktop window, no system
	 * bars, cutout or rounded corners, so its insets are empty */
	@Override
	public WindowMetrics getCurrentWindowMetrics() {
		return new WindowMetrics(new Rect(0, 0, Display.window_width, Display.window_height),
		                         new WindowInsets.Builder().build(),
		                         Resources.getSystem().getDisplayMetrics().density);
	}

	@Override
	public WindowMetrics getMaximumWindowMetrics() {
		return getCurrentWindowMetrics();
	}

	@Override
	public void addView(View view, android.view.ViewGroup.LayoutParams params) {
		Slog.v(TAG, "addView(" + view + ", " + params + ") called");
		view.setLayoutParams(params);
		view.parent = new WindowViewParent();
		view.dispatchAttachedToWindow();
		android.atl.ATLViewDump.addRoot(view); // debugging aid (atl-doctor)
		Rect displayFrame = new Rect();
		view.getWindowVisibleDisplayFrame(displayFrame);
		view.internalSetDefaultMeasureSpec(View.MeasureSpec.AT_MOST | displayFrame.width(), View.MeasureSpec.AT_MOST | displayFrame.height());
		WindowManager.LayoutParams windowParams = (WindowManager.LayoutParams)params;
		/* as on AOSP, a focusable window that is not FLAG_NOT_TOUCH_MODAL takes all touches, also
		 * those outside of it (Compose's popups dismiss themselves on these) */
		boolean touchModal = (windowParams.flags & (WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL)) == 0;
		native_addView(view.widget, view, windowParams.type, touchModal, absoluteGravity(view, windowParams), windowParams.x, windowParams.y, params.width, params.height);
	}

	/* the gravity by which x and y place the window; ATL's LayoutParams have -1 for none set */
	private static int absoluteGravity(View view, WindowManager.LayoutParams params) {
		return Gravity.getAbsoluteGravity(params.gravity == -1 ? Gravity.TOP | Gravity.LEFT : params.gravity, view.getLayoutDirection());
	}

	/* called when GTK closed a touch-modal window on a press outside of it (or Escape): on
	 * AOSP the window gets such presses, here it gets ACTION_OUTSIDE, on which Compose's popups
	 * dismiss themselves alike */
	private static void dispatchOutsideTouch(View view) {
		long now = SystemClock.uptimeMillis();
		MotionEvent event = MotionEvent.obtain(now, now, MotionEvent.ACTION_OUTSIDE, 0, 0, 0);
		view.onTouchEvent(event);
		event.recycle();
	}

	@Override
	public void updateViewLayout(View view, android.view.ViewGroup.LayoutParams params) {
		Slog.v(TAG, "updateViewLayout(" + view + ", " + params + ") called");
		Rect displayFrame = new Rect();
		view.getWindowVisibleDisplayFrame(displayFrame);
		view.internalSetDefaultMeasureSpec(View.MeasureSpec.AT_MOST | displayFrame.width(), View.MeasureSpec.AT_MOST | displayFrame.height());
		WindowManager.LayoutParams windowParams = (WindowManager.LayoutParams)params;
		view.setLayoutParams(params);
		native_updateViewLayout(view.widget, absoluteGravity(view, windowParams), windowParams.x, windowParams.y, params.width, params.height);
	}

	@Override
	public void removeView(View view) {
		android.atl.ATLViewDump.removeRoot(view); // debugging aid (atl-doctor)
		view.dispatchDetachedFromWindow();
		native_removeView(view.widget);
		view.parent = null;
	}

	@Override
	public void removeViewImmediate(View view) {
		removeView(view);
	}

	private static native void native_addView(long widget, View view, int type, boolean touchModal, int gravity, int x, int y, int width, int height);
	private static native void native_updateViewLayout(long widget, int gravity, int x, int y, int width, int height);
	private static native void native_removeView(long widget);
}
