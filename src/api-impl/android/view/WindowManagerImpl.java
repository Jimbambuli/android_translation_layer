package android.view;

import android.graphics.Rect;
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

	@Override
	public void addView(View view, android.view.ViewGroup.LayoutParams params) {
		Slog.v(TAG, "addView(" + view + ", " + params + ") called");
		view.setLayoutParams(params);
		view.parent = new WindowViewParent();
		view.onAttachedToWindow();
		Rect displayFrame = new Rect();
		view.getWindowVisibleDisplayFrame(displayFrame);
		view.internalSetDefaultMeasureSpec(View.MeasureSpec.AT_MOST | displayFrame.width(), View.MeasureSpec.AT_MOST | displayFrame.height());
		WindowManager.LayoutParams windowParams = (WindowManager.LayoutParams)params;
		native_addView(view.widget, windowParams.type, windowParams.x, windowParams.y, params.width, params.height);
	}

	@Override
	public void updateViewLayout(View view, android.view.ViewGroup.LayoutParams params) {
		Slog.v(TAG, "updateViewLayout(" + view + ", " + params + ") called");
		Rect displayFrame = new Rect();
		view.getWindowVisibleDisplayFrame(displayFrame);
		view.internalSetDefaultMeasureSpec(View.MeasureSpec.AT_MOST | displayFrame.width(), View.MeasureSpec.AT_MOST | displayFrame.height());
		WindowManager.LayoutParams windowParams = (WindowManager.LayoutParams)params;
		view.setLayoutParams(params);
		native_updateViewLayout(view.widget, windowParams.x, windowParams.y, params.width, params.height);
	}

	@Override
	public void removeView(View view) {
		native_removeView(view.widget);
		view.parent = null;
	}

	@Override
	public void removeViewImmediate(View view) {
		removeView(view);
	}

	private static native void native_addView(long widget, int type, int x, int y, int width, int height);
	private static native void native_updateViewLayout(long widget, int x, int y, int width, int height);
	private static native void native_removeView(long widget);
}
