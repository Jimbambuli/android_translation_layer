package android.view;

import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Slog;

public class WindowManagerImpl implements WindowManager, ViewManager {

	private static final String TAG = "WindowManagerImpl";

	/* The parent of a view added with addView(): the root of a window, like AOSP's ViewRootImpl.
	 * GTK lays out and invalidates the window itself, and there is nothing above to intercept
	 * touch events or to take part in nested scrolling. */
	private static class WindowViewParent implements ViewParent {

		/* AOSP ViewRootImpl.getChildVisibleRect(): the window clips, without its scroll offset; the window is
		 * as large as its one child */
		@Override
		public boolean getChildVisibleRect(View child, android.graphics.Rect r, android.graphics.Point offset) {
			return r.intersect(0, 0, child.getWidth(), child.getHeight());
		}

		/* AOSP ViewRootImpl.bringChildToFront(): the root has one child */
		@Override
		public void bringChildToFront(View child) {}

		/* like AOSP's ViewRootImpl.mTraversalScheduled: a layout of the window is pending */
		boolean traversalScheduled;

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

		/* like ViewRootImpl: the root view of the window; the focus search of a window is the focus search in it */
		View root;

		@Override
		public View focusSearch(View v, int direction) {
			if (!(root instanceof ViewGroup))
				return null;
			return FocusFinder.getInstance().findNextFocus((ViewGroup)root, v, direction);
		}

		/* ViewRootImpl.requestChildFocus()/clearChildFocus(): the window's focus is GTK's, which follows the view
		 * that took the focus; there is nothing more to record */
		@Override
		public void requestChildFocus(View child, View focused) {}

		@Override
		public void clearChildFocus(View child) {}

		/* ViewRootImpl.requestSendAccessibilityEvent(): hands the event on to the AccessibilityManager, which has
		 * no service to deliver it to; the root accepts it */
		@Override
		public boolean requestSendAccessibilityEvent(View child, android.view.accessibility.AccessibilityEvent event) {
			return true;
		}

		@Override
		public void notifySubtreeAccessibilityStateChanged(View child, View source, int changeType) {}

		/* ViewRootImpl.requestChildRectangleOnScreen(): scrolls the window's surface, which does not scroll */
		@Override
		public boolean requestChildRectangleOnScreen(View child, Rect rectangle, boolean immediate) {
			return false;
		}

		/* ATL has no context menus and no action mode */
		@Override
		public boolean showContextMenuForChild(View originalView) {
			return false;
		}

		@Override
		public boolean showContextMenuForChild(View originalView, float x, float y) {
			return false;
		}

		@Override
		public ActionMode startActionModeForChild(View originalView, ActionMode.Callback callback) {
			return null;
		}

		@Override
		public ActionMode startActionModeForChild(View originalView, ActionMode.Callback callback, int type) {
			return null;
		}
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
		WindowViewParent windowParent = new WindowViewParent();
		windowParent.root = view;
		view.parent = windowParent;
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
		view.setLayoutParams(params);
		/* as on AOSP (ViewRootImpl.setLayoutParams() -> scheduleTraversals()), the window is measured
		 * and placed later, not within this call: Compose's popups call it from their own layout pass,
		 * which a measure here re-enters ("performMeasureAndLayout called during measure layout") */
		if (!(view.parent instanceof WindowViewParent))
			return;
		WindowViewParent parent = (WindowViewParent)view.parent;
		if (parent.traversalScheduled)
			return;
		parent.traversalScheduled = true;
		new Handler(Looper.getMainLooper()).post(new Runnable() {
			@Override
			public void run() {
				parent.traversalScheduled = false;
				if (view.parent == parent) // not removed meanwhile
					performUpdateViewLayout(view);
			}
		});
	}

	private static void performUpdateViewLayout(View view) {
		Rect displayFrame = new Rect();
		view.getWindowVisibleDisplayFrame(displayFrame);
		view.internalSetDefaultMeasureSpec(View.MeasureSpec.AT_MOST | displayFrame.width(), View.MeasureSpec.AT_MOST | displayFrame.height());
		WindowManager.LayoutParams windowParams = (WindowManager.LayoutParams)view.getLayoutParams();
		native_updateViewLayout(view.widget, absoluteGravity(view, windowParams), windowParams.x, windowParams.y, windowParams.width, windowParams.height);
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

	/* AOSP WindowManagerImpl -> CrossWindowBlurListeners: the compositor's blur state, and each
	 * new listener is told the current state once on its executor (notifyListener()). ATL's
	 * windows are plain toplevels without cross-window blur, so the state is always false and
	 * never changes; no listener needs to be kept. */
	@Override
	public boolean isCrossWindowBlurEnabled() {
		return false;
	}

	@Override
	public void addCrossWindowBlurEnabledListener(java.util.function.Consumer<Boolean> listener) {
		final Handler handler = new Handler(Looper.getMainLooper());
		addCrossWindowBlurEnabledListener(handler::post, listener);
	}

	@Override
	public void addCrossWindowBlurEnabledListener(java.util.concurrent.Executor executor,
			java.util.function.Consumer<Boolean> listener) {
		java.util.Objects.requireNonNull(listener, "listener cannot be null");
		java.util.Objects.requireNonNull(executor, "executor cannot be null");
		executor.execute(() -> listener.accept(false));
	}

	@Override
	public void removeCrossWindowBlurEnabledListener(java.util.function.Consumer<Boolean> listener) {
		java.util.Objects.requireNonNull(listener, "listener cannot be null");
	}

	/* AOSP 16 WindowManagerImpl.addScreenRecordingCallback() -> ScreenRecordingCallbacks: the state
	 * comes from WindowManagerService (is a window of this uid visible in an active screen
	 * recording). ATL has no window manager service that tracks recordings, so the state stays
	 * SCREEN_RECORDING_STATE_NOT_VISIBLE and never changes; no callback needs to be kept. */
	@Override
	public int addScreenRecordingCallback(java.util.concurrent.Executor executor,
			java.util.function.Consumer<Integer> callback) {
		java.util.Objects.requireNonNull(executor, "executor must not be null");
		java.util.Objects.requireNonNull(callback, "callback must not be null");
		return SCREEN_RECORDING_STATE_NOT_VISIBLE;
	}

	@Override
	public void removeScreenRecordingCallback(java.util.function.Consumer<Integer> callback) {
		java.util.Objects.requireNonNull(callback, "callback must not be null");
	}

	private static native void native_addView(long widget, View view, int type, boolean touchModal, int gravity, int x, int y, int width, int height);
	private static native void native_updateViewLayout(long widget, int gravity, int x, int y, int width, int height);
	private static native void native_removeView(long widget);
}
