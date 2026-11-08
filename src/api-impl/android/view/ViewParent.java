package android.view;

public interface ViewParent {
	public abstract ViewParent getParent();

	public boolean isLayoutRequested();

	public void requestDisallowInterceptTouchEvent(boolean disallowIntercept);

	public abstract boolean onStartNestedScroll(View child, View target, int nestedScrollAxes);

	public boolean onNestedPreFling(View target, float velocityX, float velocityY);

	public boolean onNestedFling(View target, float velocityX, float velocityY, boolean consumed);

	public void onNestedScrollAccepted(View child, View target, int nestedScrollAxes);

	public void onNestedPreScroll(View target, int dx, int dy, int[] consumed);

	public void onNestedScroll(View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed);

	public void onStopNestedScroll(View target);

	public void onDescendantInvalidated(View child, View target);

	public void requestLayout();

	/** AOSP ViewParent.childHasTransientStateChanged(): a child's setHasTransientState() changed its state */
	public void childHasTransientStateChanged(View child, boolean hasTransientState);

	public View focusSearch(View v, int direction);

	public void requestChildFocus(View child, View focused);

	public void clearChildFocus(View child);

	public boolean requestSendAccessibilityEvent(View child, android.view.accessibility.AccessibilityEvent event);

	public void notifySubtreeAccessibilityStateChanged(View child, View source, int changeType);

	public boolean requestChildRectangleOnScreen(View child, android.graphics.Rect rectangle, boolean immediate);

	public boolean showContextMenuForChild(View originalView);

	public ActionMode startActionModeForChild(View originalView, ActionMode.Callback callback, int type);
}
