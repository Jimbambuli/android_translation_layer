package android.view;

import android.R;
import android.animation.LayoutTransition;
import android.annotation.UnsupportedAppUsage;
import android.atl.GskCanvas;
import android.atl.annotation.Export;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.util.Slog;
import android.content.pm.PackageManager;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Objects;

public class ViewGroup extends View implements ViewParent, ViewManager {
	/* clang-format off */
	static final int FLAG_CLIP_CHILDREN        = (1 << 0);
	static final int FLAG_INVALIDATE_REQUIRED  = (1 << 2);
	static final int FLAG_ANIMATION_DONE       = (1 << 4);
	static final int FLAG_OPTIMIZE_INVALIDATE  = (1 << 7);
	static final int FLAG_CLEAR_TRANSFORMATION = (1 << 8);
	@UnsupportedAppUsage
	protected static final int FLAG_USE_CHILD_DRAWING_ORDER = (1 << 10);
	@UnsupportedAppUsage
	protected static final int FLAG_SUPPORT_STATIC_TRANSFORMATIONS = (1 << 11);
	private static final int FLAG_ADD_STATES_FROM_CHILDREN = 0x2000;
	private static final int FLAG_MASK_FOCUSABILITY  = (3 << 17);
	public static final int FOCUS_BEFORE_DESCENDANTS = (1 << 17);
	public static final int FOCUS_AFTER_DESCENDANTS  = (2 << 17);
	public static final int FOCUS_BLOCK_DESCENDANTS  = (3 << 17);
	@UnsupportedAppUsage
	protected static final int FLAG_DISALLOW_INTERCEPT = (1 << 19);
	static final int FLAG_IS_TRANSITION_GROUP          = (1 << 24);
	static final int FLAG_IS_TRANSITION_GROUP_SET      = (1 << 25);
	static final int FLAG_TOUCHSCREEN_BLOCKS_FOCUS     = (1 << 26);
	/* clang-format on */

	protected int mGroupFlags;

	private static final int[] DESCENDANT_FOCUSABILITY_FLAGS =
			{FOCUS_BEFORE_DESCENDANTS, FOCUS_AFTER_DESCENDANTS,
					FOCUS_BLOCK_DESCENDANTS};

	public ArrayList<View> children;
	private ArrayList<View> detachedChildren;
	private boolean pendingHideDetachedChildren = false;
	private OnHierarchyChangeListener onHierarchyChangeListener;
	private LayoutTransition transition;
	private ViewGroupOverlay viewGroupOverlay;

	public ViewGroup(Context context) {
		this(context, null);
	}

	public ViewGroup(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public ViewGroup(Context context, AttributeSet attrs, int defStyleAttr) {
		this(context, attrs, defStyleAttr, 0);
	}

	public ViewGroup(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr);

		children = new ArrayList<View>();
		detachedChildren = new ArrayList<View>();

		setDescendantFocusability(FOCUS_BEFORE_DESCENDANTS);

		TypedArray a = context.obtainStyledAttributes(attrs, com.android.internal.R.styleable.ViewGroup, defStyleAttr, defStyleRes);
		if (a.hasValue(com.android.internal.R.styleable.ViewGroup_descendantFocusability))
			setDescendantFocusability(DESCENDANT_FOCUSABILITY_FLAGS[a.getInt(com.android.internal.R.styleable.ViewGroup_descendantFocusability, 0)]);
		if (a.getBoolean(com.android.internal.R.styleable.ViewGroup_touchscreenBlocksFocus, false))
			setTouchscreenBlocksFocus(true);
		setClipChildren(a.getBoolean(com.android.internal.R.styleable.ViewGroup_clipChildren, true));
		a.recycle();
	}

	public void addView(View child) {
		addView(child, -1);
	}

	public void addView(View child, int index) {
		if (child == null) {
			throw new IllegalArgumentException("Cannot add a null child view to a ViewGroup");
		}
		LayoutParams params = child.getLayoutParams();
		if (params == null) {
			params = generateDefaultLayoutParams();
			if (params == null) {
				throw new IllegalArgumentException("generateDefaultLayoutParams() cannot return null");
			}
		}
		addView(child, index, params);
	}

	public void addView(View child, LayoutParams params) {
		addView(child, -1, params);
	}

	public void addView(View child, int width, int height) {
		final LayoutParams params = generateDefaultLayoutParams();
		params.width = width;
		params.height = height;
		addView(child, -1, params);
	}

	/* AOSP's ViewGroup.addView(): the layout is requested before the child is added (invalidate(true)
	 * has no counterpart, GTK redraws what it lays out) */
	public void addView(View child, int index, LayoutParams params) {
		if (child == null) {
			throw new IllegalArgumentException("Cannot add a null child view to a ViewGroup");
		}
		requestLayout();
		addViewInternal(child, index, params, true);
	}

	/* like AOSP: the children that inherit the layout direction follow this view group */
	@Override
	public boolean resolveRtlPropertiesIfNeeded() {
		final boolean result = super.resolveRtlPropertiesIfNeeded();
		if (result) {
			for (int i = 0; i < children.size(); i++) {
				View child = children.get(i);
				if (child.isLayoutDirectionInherited())
					child.resolveRtlPropertiesIfNeeded();
			}
		}
		return result;
	}

	@Override
	public void resetRtlProperties() {
		super.resetRtlProperties();
		for (int i = 0; i < children.size(); i++) {
			View child = children.get(i);
			if (child.isLayoutDirectionInherited())
				child.resetRtlProperties();
		}
	}

	/* like AOSP: the children first */
	@Override
	void dispatchDetachedFromWindow() {
		for (int i = 0; i < children.size(); i++)
			children.get(i).dispatchDetachedFromWindow();
		super.dispatchDetachedFromWindow();
	}

	/* like AOSP: this view group first, then its children */
	@Override
	public void dispatchWindowFocusChanged(boolean hasFocus) {
		super.dispatchWindowFocusChanged(hasFocus);
		for (int i = 0; i < children.size(); i++)
			children.get(i).dispatchWindowFocusChanged(hasFocus);
	}

	public void onViewAdded(View view) {}
	public void onViewRemoved(View view) {}

	protected void addViewInternal(View child, int index, LayoutParams params, boolean callOnViewAdded) {
		if (child.parent == this)
			return;
		if (!checkLayoutParams(params))
			params = generateLayoutParams(params);

		child.parent = this;
		if (child.hasTransientState()) {
			childHasTransientStateChanged(child, true);
		}
		/* like AOSP: an inherited layout direction is resolved again in the new parent */
		if (child.isLayoutDirectionInherited())
			child.resetRtlProperties();
		child.applyLayoutParams(params);
		if (index < 0)
			index = children.size();
		children.add(index, child);
		int sortedIndex = index;
		for (View v : children) {
			if (v.getZ() != 0.0f) {
				// sort children by z-order
				ArrayList<View> sortedChildren = new ArrayList<View>(children);
				sortedChildren.sort(new Comparator<View>() {
					@Override
					public int compare(View o1, View o2) {
						return (int)(o1.getZ() - o2.getZ());
					}
				});
				sortedIndex = sortedChildren.indexOf(child);
				break;
			}
		}
		native_addView(widget, child.widget, sortedIndex, params);
		requestLayout();
		if (child.isDuplicateParentStateEnabled())
			child.refreshDrawableState();
		if (callOnViewAdded) {
			onViewAdded(child);
			if (onHierarchyChangeListener != null)
				onHierarchyChangeListener.onChildViewAdded(this, child);
		}
	}

	private View touchTarget;
	private boolean intercepting_touch = false;

	/* AOSP ViewGroup's drag state: the child under the drag point, the children that are interested in the
	 * drag, the ACTION_DRAG_STARTED event for them and whether the group itself is interested */
	private View mCurrentDragChild;
	private DragEvent mCurrentDragStartEvent;
	private boolean mIsInterestedInDrag;
	private java.util.HashSet<View> mChildrenInterestedInDrag;

	/* ATL: the point (x, y) in the child's coordinates, the same mapping as isTouchPointInView() */
	private void transformPointToChild(float x, float y, View child, float[] out) {
		out[0] = x + getScrollX() - child.getLeft() - child.getTranslationX();
		out[1] = y + getScrollY() - child.getTop() - child.getTranslationY();
	}

	@Override
	public boolean dispatchDragEvent(DragEvent event) {
		boolean retval = false;
		final float tx = event.mX;
		final float ty = event.mY;

		// Dispatch down the view hierarchy
		final float[] localPoint = new float[2];

		switch (event.mAction) {
		case DragEvent.ACTION_DRAG_STARTED: {
			// Clear the state to recalculate which views we drag over.
			mCurrentDragChild = null;

			// Set up our tracking of drag-started notifications
			mCurrentDragStartEvent = DragEvent.obtain(event);
			if (mChildrenInterestedInDrag == null) {
				mChildrenInterestedInDrag = new java.util.HashSet<View>();
			} else {
				mChildrenInterestedInDrag.clear();
			}

			// Now dispatch down to our children, caching the responses
			final int count = children.size();
			for (int i = 0; i < count; i++) {
				final View child = children.get(i);
				child.mPrivateFlags2 &= ~View.DRAG_MASK;
				if (child.getVisibility() == VISIBLE) {
					if (notifyChildOfDragStart(child)) {
						retval = true;
					}
				}
			}

			// Notify itself of the drag start.
			mIsInterestedInDrag = super.dispatchDragEvent(event);
			if (mIsInterestedInDrag) {
				retval = true;
			}

			if (!retval) {
				// Neither us nor any of our children are interested in this drag, so stop tracking
				// the current drag event.
				mCurrentDragStartEvent.recycle();
				mCurrentDragStartEvent = null;
			}
		} break;

		case DragEvent.ACTION_DRAG_ENDED: {
			// Release the bookkeeping now that the drag lifecycle has ended
			final java.util.HashSet<View> childrenInterestedInDrag = mChildrenInterestedInDrag;
			if (childrenInterestedInDrag != null) {
				for (View child : childrenInterestedInDrag) {
					// If a child was interested in the ongoing drag, it's told that it's over
					if (child.dispatchDragEvent(event)) {
						retval = true;
					}
				}
				childrenInterestedInDrag.clear();
			}
			if (mCurrentDragStartEvent != null) {
				mCurrentDragStartEvent.recycle();
				mCurrentDragStartEvent = null;
			}

			if (mIsInterestedInDrag) {
				if (super.dispatchDragEvent(event)) {
					retval = true;
				}
				mIsInterestedInDrag = false;
			}
		} break;

		case DragEvent.ACTION_DRAG_LOCATION:
		case DragEvent.ACTION_DROP: {
			// Find the [possibly new] drag target
			View target = findFrontmostDroppableChildAt(event.mX, event.mY, localPoint);
			mCurrentDragChild = target;

			if (target == null && mIsInterestedInDrag) {
				target = this;
			}

			// Dispatch the actual drag notice, localized into the target coordinates.
			if (target != null) {
				if (target != this) {
					event.mX = localPoint[0];
					event.mY = localPoint[1];

					retval = target.dispatchDragEvent(event);

					event.mX = tx;
					event.mY = ty;

					if (mIsInterestedInDrag && !event.mEventHandlerWasCalled) {
						retval = super.dispatchDragEvent(event);
					}
				} else {
					retval = super.dispatchDragEvent(event);
				}
			}
		} break;
		}

		return retval;
	}

	// Find the frontmost child view that lies under the given point, and calculate
	// the position within its own local coordinate system.
	View findFrontmostDroppableChildAt(float x, float y, float[] outLocalPoint) {
		for (int i = children.size() - 1; i >= 0; i--) {
			final View child = children.get(i);
			if (!child.canAcceptDrag()) {
				continue;
			}

			if (isTouchPointInView(x, y, child)) {
				transformPointToChild(x, y, child, outLocalPoint);
				return child;
			}
		}
		return null;
	}

	boolean notifyChildOfDragStart(View child) {
		// The caller guarantees that the child is not in mChildrenInterestedInDrag yet.

		final float tx = mCurrentDragStartEvent.mX;
		final float ty = mCurrentDragStartEvent.mY;

		final float[] point = new float[2];
		transformPointToChild(tx, ty, child, point);

		mCurrentDragStartEvent.mX = point[0];
		mCurrentDragStartEvent.mY = point[1];
		final boolean canAccept = child.dispatchDragEvent(mCurrentDragStartEvent);
		mCurrentDragStartEvent.mX = tx;
		mCurrentDragStartEvent.mY = ty;
		mCurrentDragStartEvent.mEventHandlerWasCalled = false;
		if (canAccept) {
			mChildrenInterestedInDrag.add(child);
			if (!child.canAcceptDrag()) {
				child.mPrivateFlags2 |= View.PFLAG2_DRAG_CAN_ACCEPT;
				child.refreshDrawableState();
			}
		}
		return canAccept;
	}

	private boolean isTouchPointInView(float x, float y, View child) {
		if (child.getVisibility() != View.VISIBLE)
			return false;
		float localX = x + getScrollX() - child.getLeft() - child.getTranslationX();
		float localY = y + getScrollY() - child.getTop() - child.getTranslationY();
		return localX >= 0 && localX <= child.getWidth() && localY >= 0 && localY <= child.getHeight();
	}

	private boolean dispatchTransformedTouchEvent(MotionEvent event, boolean cancel, View child) {
		int oldAction = event.getAction();
		if (cancel)
			event.setAction(MotionEvent.ACTION_CANCEL);

		float offsetX = getScrollX() - child.getLeft() - child.getTranslationX();
		float offsetY = getScrollY() - child.getTop() - child.getTranslationY();
		event.offsetLocation(offsetX, offsetY);
		boolean handled = child.dispatchTouchEvent(event);
		event.offsetLocation(-offsetX, -offsetY);

		if (cancel)
			event.setAction(oldAction);
		return handled;
	}

	/* AOSP's ViewGroup.dispatchGenericPointerEvent(): the child under the pointer, front to back */
	@Override
	protected boolean dispatchGenericPointerEvent(MotionEvent event) {
		for (int i = children.size() - 1; i >= 0; i--) {
			View child = children.get(i);
			if (!isTouchPointInView(event.getX(), event.getY(), child))
				continue;
			if (dispatchTransformedGenericPointerEvent(event, child))
				return true;
		}
		// No child handled the event.  Send it to this view group.
		return super.dispatchGenericPointerEvent(event);
	}

	/* AOSP's ViewGroup.dispatchTransformedGenericPointerEvent(), in the child's coordinates as
	 * dispatchTransformedTouchEvent() */
	private boolean dispatchTransformedGenericPointerEvent(MotionEvent event, View child) {
		float offsetX = getScrollX() - child.getLeft() - child.getTranslationX();
		float offsetY = getScrollY() - child.getTop() - child.getTranslationY();
		event.offsetLocation(offsetX, offsetY);
		boolean handled = child.dispatchGenericMotionEvent(event);
		event.offsetLocation(-offsetX, -offsetY);
		return handled;
	}

	/* AOSP's ViewGroup.onInterceptHoverEvent(), without the scroll bar thumb (not implemented) */
	@Export
	public boolean onInterceptHoverEvent(MotionEvent event) {
		return false;
	}

	/* AOSP's ViewGroup.dispatchHoverEvent() asks onInterceptHoverEvent() first and then hands the event to the
	 * children. Here every view with a hover handler has its own GTK motion controller (see
	 * View.native_constructor()), so the children get their events there and only the interception is kept. */
	@Export
	@Override
	protected boolean dispatchHoverEvent(MotionEvent event) {
		onInterceptHoverEvent(event);
		return super.dispatchHoverEvent(event);
	}

	@Export
	@Override
	public boolean dispatchTouchEvent(MotionEvent event) {
		int action = event.getActionMasked();
		if (action == MotionEvent.ACTION_DOWN) {
			if (touchTarget != null) {
				dispatchTransformedTouchEvent(event, true, touchTarget);
				touchTarget = null;
			}
			disallowIntercept = false;
			intercepting_touch = false;
		}

		if (!intercepting_touch && !disallowIntercept) {
			intercepting_touch = onInterceptTouchEvent(event);
			if ((intercepting_touch) && touchTarget != null) {
				dispatchTransformedTouchEvent(event, true, touchTarget);
				touchTarget = null;
			}
		}

		boolean handled = false;

		if (!intercepting_touch && touchTarget != null) {
			handled = dispatchTransformedTouchEvent(event, false, touchTarget);
		} else if (!intercepting_touch && action == MotionEvent.ACTION_DOWN) {
			// Find a child that handles the gesture, trying siblings front to back.
			for (int i = children.size() - 1; i >= 0; i--) {
				View child = children.get(i);
				if (!isTouchPointInView(event.getX(), event.getY(), child))
					continue;
				handled = dispatchTransformedTouchEvent(event, false, child);
				if (handled) {
					touchTarget = child;
					break;
				}
			}
		}

		if (touchTarget == null)
			handled = super.dispatchTouchEvent(event);

		if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
			touchTarget = null;
			intercepting_touch = false;
			disallowIntercept = false;
		}

		return handled;
	}

	protected boolean addViewInLayout(View child, int index, LayoutParams params) {
		addViewInternal(child, index, params, true);
		return true;
	}

	protected boolean addViewInLayout(View child, int index, LayoutParams params, boolean preventRequestLayout) {
		addViewInternal(child, index, params, true);
		if (!preventRequestLayout)
			requestLayout();
		return true;
	}

	void dispatchViewRemoved(View child) {
		onViewRemoved(child);
		if (onHierarchyChangeListener != null) {
			onHierarchyChangeListener.onChildViewRemoved(this, child);
		}
	}

	/* AOSP removeFromArray(); ATL also takes the child's widget out of the GTK parent */
	private void removeFromArray(int index) {
		View child = children.get(index);
		if (child.hasTransientState()) {
			childHasTransientStateChanged(child, false);
		}
		child.parent = null;
		children.remove(index);
		native_removeView(widget, child.widget);
	}

	/* AOSP ViewGroup.removeView() ... removeAllViewsInLayout(); without what ATL has no counterpart for (the
	 * LayoutTransition, the focus bookkeeping of mFocused/mDefaultFocus, touch and hover targets, disappearing
	 * views and transient state, accessibility) */
	public void removeView(View view) {
		if (removeViewInternal(view)) {
			requestLayout();
			invalidate();
		}
	}

	public void removeViewInLayout(View view) {
		removeViewInternal(view);
	}

	public void removeViewsInLayout(int start, int count) {
		removeViewsInternal(start, count);
	}

	public void removeViewAt(int index) {
		removeViewInternal(index, getChildAt(index));
		requestLayout();
		invalidate();
	}

	public void removeViews(int start, int count) {
		removeViewsInternal(start, count);
		requestLayout();
		invalidate();
	}

	private boolean removeViewInternal(View view) {
		final int index = indexOfChild(view);
		if (index >= 0) {
			removeViewInternal(index, view);
			return true;
		}
		return false;
	}

	private void removeViewInternal(int index, View view) {
		// like AOSP's mAttachInfo != null: dispatchDetachedFromWindow() does nothing for a detached view
		view.dispatchDetachedFromWindow();

		removeFromArray(index);

		dispatchViewRemoved(view);
	}

	private void removeViewsInternal(int start, int count) {
		final int end = start + count;

		if (start < 0 || count < 0 || end > children.size()) {
			throw new IndexOutOfBoundsException();
		}

		for (int i = start; i < end; i++) {
			final View view = children.get(i);

			view.dispatchDetachedFromWindow();

			dispatchViewRemoved(view);
		}

		for (int i = end - 1; i >= start; i--) {
			removeFromArray(i);
		}
	}

	public void removeAllViews() {
		removeAllViewsInLayout();
		requestLayout();
		invalidate();
	}

	public void removeAllViewsInLayout() {
		final int count = children.size();
		if (count <= 0) {
			return;
		}

		for (int i = count - 1; i >= 0; i--) {
			final View view = children.get(i);

			view.dispatchDetachedFromWindow();

			dispatchViewRemoved(view);

			removeFromArray(i);
		}
	}

	public void detachViewFromParent(int index) {
		detachViewFromParent(getChildAt(index));
	}

	public void attachViewToParent(View view, int index, LayoutParams params) {
		final boolean wasDetached = detachedChildren.remove(view);
		if (!wasDetached) {
			/* a view that was never added (ViewPager2 attaches its RecyclerView so): addViewInternal()
			 * puts it into the children once, as AOSP's addInArray() does */
			addViewInternal(view, index, params, false);
		} else {
			if (!checkLayoutParams(params))
				params = generateLayoutParams(params);

			view.parent = this;
			view.applyLayoutParams(params);
			if (index < 0)
				index = children.size();
			children.add(index, view);
			if (view.hasTransientState()) {
				childHasTransientStateChanged(view, true);
			}
		}
		view.native_setVisibility(view.widget, view.getVisibility(), view.getAlpha() * view.getTransitionAlpha(), view.isEnabled());
	}

	protected void removeDetachedView(View child, boolean animate) {
		if (!detachedChildren.remove(child))
			return;
		child.dispatchDetachedFromWindow();
		child.parent = null;
		native_removeView(widget, child.widget);
		dispatchViewRemoved(child);
	}

	@Override
	public WindowInsets dispatchApplyWindowInsets(WindowInsets insets) {
		insets = super.dispatchApplyWindowInsets(insets);
		if (insets.isConsumed()) {
			return insets;
		}
		if (View.sBrokenInsetsDispatch) {
			return brokenDispatchApplyWindowInsets(insets);
		} else {
			return newDispatchApplyWindowInsets(insets);
		}
	}

	private WindowInsets brokenDispatchApplyWindowInsets(WindowInsets insets) {
		final int count = getChildCount();
		for (int i = 0; i < count; i++) {
			insets = getChildAt(i).dispatchApplyWindowInsets(insets);
			if (insets.isConsumed()) {
				break;
			}
		}
		return insets;
	}

	private WindowInsets newDispatchApplyWindowInsets(WindowInsets insets) {
		final int count = getChildCount();
		for (int i = 0; i < count; i++) {
			getChildAt(i).dispatchApplyWindowInsets(insets);
		}
		return insets;
	}

	@Override
	protected void dispatchVisibilityChanged(View changedView, int visibility) {
		if (children == null) // happens if this gets called during super constructor
			return;

		for (View child : children) {
			child.dispatchVisibilityChanged(changedView, visibility);
		}
	}

	protected native void native_addView(long widget, long child, int index, LayoutParams params);
	protected native void native_removeView(long widget, long child);
	@Override
	protected native void native_drawChildren(long widget, long snapshot);
	protected native void native_drawChild(long widget, long child, long snapshot);
	@Override
	protected void native_drawContent(long widget, long snapshot) {}

	public View getChildAt(int index) {
		if (index < 0 || index >= children.size())
			return null;
		return children.get(index);
	}

	public int indexOfChild(View child) {
		return children.indexOf(child);
	}

	public void updateViewLayout(View view, ViewGroup.LayoutParams params) {
		view.setLayoutParams(params);
		view.requestLayout();
	}

	public LayoutParams generateLayoutParams(AttributeSet attrs) {
		return new LayoutParams(getContext(), attrs);
	}
	protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
		return p;
	}

	public void bringChildToFront(View child) {
		// TODO: actually implement this (might make sense to implement it in the subclasses instead), when applicable
	}

	/**
	 * Returns the number of children in the group.
	 *
	 * @return a positive integer representing the number of children in
	 *         the group
	 */
	@Override
	protected void dispatchSaveInstanceState(SparseArray<Parcelable> container) {
		super.dispatchSaveInstanceState(container);
		final View[] children = this.children.toArray(new View[0]);
		final int count = children.length;
		for (int i = 0; i < count; i++) {
			View c = children[i];
			if (c.isSaveFromParentEnabled()) {
				c.dispatchSaveInstanceState(container);
			}
		}
	}

	protected void dispatchFreezeSelfOnly(SparseArray<Parcelable> container) {
		super.dispatchSaveInstanceState(container);
	}

	@Override
	protected void dispatchRestoreInstanceState(SparseArray<Parcelable> container) {
		super.dispatchRestoreInstanceState(container);
		final View[] children = this.children.toArray(new View[0]);
		final int count = children.length;
		for (int i = 0; i < count; i++) {
			View c = children[i];
			if (c.isSaveFromParentEnabled()) {
				c.dispatchRestoreInstanceState(container);
			}
		}
	}

	protected void dispatchThawSelfOnly(SparseArray<Parcelable> container) {
		super.dispatchRestoreInstanceState(container);
	}

	public int getChildCount() {
		return children.size();
	}

	public void setMotionEventSplittingEnabled(boolean enabled) {}

	public void setOnHierarchyChangeListener(OnHierarchyChangeListener listener) {
		this.onHierarchyChangeListener = listener;
	}

	/** @hide AOSP ViewGroup.onSetLayoutParams(): a child's layout parameters were set */
	protected void onSetLayoutParams(View child, LayoutParams layoutParams) {
		requestLayout();
	}

	protected boolean checkLayoutParams(LayoutParams params) {
		return true;
	}

	public LayoutTransition getLayoutTransition() {
		if (transition == null)
			transition = new LayoutTransition();
		return transition;
	}

	public static int getChildMeasureSpec(int spec, int padding, int childDimension) {
		int specMode = MeasureSpec.getMode(spec);
		int specSize = MeasureSpec.getSize(spec);
		int size = Math.max(0, specSize - padding);
		int resultSize = 0;
		int resultMode = 0;
		switch (specMode) {
			// Parent has imposed an exact size on us
			case MeasureSpec.EXACTLY:
				if (childDimension >= 0) {
					resultSize = childDimension;
					resultMode = MeasureSpec.EXACTLY;
				} else if (childDimension == LayoutParams.MATCH_PARENT) {
					// Child wants to be our size. So be it.
					resultSize = size;
					resultMode = MeasureSpec.EXACTLY;
				} else if (childDimension == LayoutParams.WRAP_CONTENT) {
					// Child wants to determine its own size. It can't be
					// bigger than us.
					resultSize = size;
					resultMode = MeasureSpec.AT_MOST;
				}
				break;
			// Parent has imposed a maximum size on us
			case MeasureSpec.AT_MOST:
				if (childDimension >= 0) {
					// Child wants a specific size... so be it
					resultSize = childDimension;
					resultMode = MeasureSpec.EXACTLY;
				} else if (childDimension == LayoutParams.MATCH_PARENT) {
					// Child wants to be our size, but our size is not fixed.
					// Constrain child to not be bigger than us.
					resultSize = size;
					resultMode = MeasureSpec.AT_MOST;
				} else if (childDimension == LayoutParams.WRAP_CONTENT) {
					// Child wants to determine its own size. It can't be
					// bigger than us.
					resultSize = size;
					resultMode = MeasureSpec.AT_MOST;
				}
				break;
			// Parent asked to see how big we want to be
			case MeasureSpec.UNSPECIFIED:
				if (childDimension >= 0) {
					// Child wants a specific size... let them have it
					resultSize = childDimension;
					resultMode = MeasureSpec.EXACTLY;
				} else if (childDimension == LayoutParams.MATCH_PARENT) {
					// Child wants to be our size... find out how big it should
					// be
					resultSize = 0; // View.sUseZeroUnspecifiedMeasureSpec ? 0 : size;
					resultMode = MeasureSpec.UNSPECIFIED;
				} else if (childDimension == LayoutParams.WRAP_CONTENT) {
					// Child wants to determine its own size.... find out how
					// big it should be
					resultSize = 0; // View.sUseZeroUnspecifiedMeasureSpec ? 0 : size;
					resultMode = MeasureSpec.UNSPECIFIED;
				}
				break;
		}
		// noinspection ResourceType
		return MeasureSpec.makeMeasureSpec(resultSize, resultMode);
	}

	protected void measureChildWithMargins(View child,
	                                       int parentWidthMeasureSpec, int widthUsed,
	                                       int parentHeightMeasureSpec, int heightUsed) {
		final MarginLayoutParams lp = (MarginLayoutParams)child.getLayoutParams();
		final int childWidthMeasureSpec = getChildMeasureSpec(parentWidthMeasureSpec, paddingLeft + paddingRight + lp.leftMargin + lp.rightMargin + widthUsed, lp.width);
		final int childHeightMeasureSpec = getChildMeasureSpec(parentHeightMeasureSpec, paddingTop + paddingBottom + lp.topMargin + lp.bottomMargin + heightUsed, lp.height);
		child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
	}

	protected void measureChild(View child, int parentWidthMeasureSpec,
	                            int parentHeightMeasureSpec) {
		final LayoutParams lp = child.getLayoutParams();
		final int childWidthMeasureSpec = getChildMeasureSpec(parentWidthMeasureSpec, paddingLeft + paddingRight, lp.width);
		final int childHeightMeasureSpec = getChildMeasureSpec(parentHeightMeasureSpec, paddingTop + paddingBottom, lp.height);
		child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
	}

	protected void measureChildren(int widthMeasureSpec, int heightMeasureSpec) {
		for (View child : children) {
			measureChild(child, widthMeasureSpec, heightMeasureSpec);
		}
	}

	/* AOSP ViewGroup.onCreateDrawableState(), setAddStatesFromChildren(), addStatesFromChildren()
	 * and childDrawableStateChanged() */
	@Override
	protected int[] onCreateDrawableState(int extraSpace) {
		if ((mGroupFlags & FLAG_ADD_STATES_FROM_CHILDREN) == 0) {
			return super.onCreateDrawableState(extraSpace);
		}

		int need = 0;
		int n = getChildCount();
		for (int i = 0; i < n; i++) {
			int[] childState = getChildAt(i).getDrawableState();

			if (childState != null) {
				need += childState.length;
			}
		}

		int[] state = super.onCreateDrawableState(extraSpace + need);

		for (int i = 0; i < n; i++) {
			int[] childState = getChildAt(i).getDrawableState();

			if (childState != null) {
				state = mergeDrawableStates(state, childState);
			}
		}

		return state;
	}

	public void setAddStatesFromChildren(boolean addsStates) {
		if (addsStates) {
			mGroupFlags |= FLAG_ADD_STATES_FROM_CHILDREN;
		} else {
			mGroupFlags &= ~FLAG_ADD_STATES_FROM_CHILDREN;
		}

		refreshDrawableState();
	}

	public boolean addStatesFromChildren() {
		return (mGroupFlags & FLAG_ADD_STATES_FROM_CHILDREN) != 0;
	}

	public void childDrawableStateChanged(View child) {
		if ((mGroupFlags & FLAG_ADD_STATES_FROM_CHILDREN) != 0) {
			refreshDrawableState();
		}
	}

	@Override
	protected void drawableStateChanged() {
		super.drawableStateChanged();
		for (View child : new ArrayList<>(children)) {
			if (child.isDuplicateParentStateEnabled())
				child.refreshDrawableState();
		}
	}

	@Override
	public void dispatchSetSelected(boolean selected) {
		final View[] children = this.children.toArray(new View[0]);
		final int count = children.length;
		for (int i = 0; i < count; i++) {
			children[i].setSelected(selected);
		}
	}

	@Override
	public void dispatchSetActivated(boolean activated) {
		final View[] children = this.children.toArray(new View[0]);
		final int count = children.length;
		for (int i = 0; i < count; i++) {
			children[i].setActivated(activated);
		}
	}

	/* as AOSP: this group is focused or a descendant is (then getFocusedChild() is that branch) */
	@Override
	public boolean hasFocus() {
		return isFocused() || getFocusedChild() != null;
	}

	/* as AOSP: the direct child that has or contains the focus */
	public View getFocusedChild() {
		if (!super.hasFocus()) // nothing in this tree is focused
			return null;
		for (View child : children) {
			if (child.hasFocus())
				return child;
		}
		return null;
	}

	/* AOSP ViewGroup.hasFocusable(boolean, boolean) and hasFocusableChild(): this group or, unless it blocks them,
	 * one of its descendants is focusable */
	@Override
	boolean hasFocusable(boolean allowAutoFocus, boolean dispatchExplicit) {
		// Invisible and gone views are never focusable.
		if (getVisibility() != VISIBLE) {
			return false;
		}

		// Only use effective focusable value when allowed.
		if (super.hasFocusable(allowAutoFocus, dispatchExplicit)) {
			return true;
		}

		// Determine whether we have a focused descendant.
		final int descendantFocusability = getDescendantFocusability();
		if (descendantFocusability != FOCUS_BLOCK_DESCENDANTS) {
			return hasFocusableChild(dispatchExplicit);
		}

		return false;
	}

	boolean hasFocusableChild(boolean dispatchExplicit) {
		// Determine whether we have a focusable descendant.
		for (View child : children) {
			// In case the subclass has overridden has[Explicit]Focusable, dispatch
			// to the expected one for each child even though we share logic here.
			if ((dispatchExplicit && child.hasExplicitFocusable())
			    || (!dispatchExplicit && child.hasFocusable())) {
				return true;
			}
		}

		return false;
	}

	/* AOSP ViewGroup.findFocus(), focusSearch(), addFocusables(), shouldBlockFocusForTouchscreen() */
	@Override
	public View findFocus() {
		if (isFocused())
			return this;
		View child = getFocusedChild();
		return child != null ? child.findFocus() : null;
	}

	/* the root of a window (the decor view, which has no parent view) searches; the others ask it */
	public View focusSearch(View focused, int direction) {
		if (!(parent instanceof ViewGroup))
			return FocusFinder.getInstance().findNextFocus(this, focused, direction);
		return ((ViewGroup)parent).focusSearch(focused, direction);
	}

	boolean shouldBlockFocusForTouchscreen() {
		return getTouchscreenBlocksFocus() && getContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN);
	}

	@Override
	public void addFocusables(ArrayList<View> views, int direction, int focusableMode) {
		final int focusableCount = views.size();

		final int descendantFocusability = getDescendantFocusability();
		final boolean blockFocusForTouchscreen = shouldBlockFocusForTouchscreen();
		final boolean focusSelf = (isFocusableInTouchMode() || !blockFocusForTouchscreen);

		if (descendantFocusability == FOCUS_BLOCK_DESCENDANTS) {
			if (focusSelf) {
				super.addFocusables(views, direction, focusableMode);
			}
			return;
		}

		if (blockFocusForTouchscreen) {
			focusableMode |= FOCUSABLES_TOUCH_MODE;
		}

		if ((descendantFocusability == FOCUS_BEFORE_DESCENDANTS) && focusSelf) {
			super.addFocusables(views, direction, focusableMode);
		}

		int count = 0;
		final View[] visibleChildren = new View[children.size()];
		for (View child : children) {
			if (child.getVisibility() == VISIBLE) {
				visibleChildren[count++] = child;
			}
		}

		FocusFinder.sort(visibleChildren, 0, count, this, isLayoutRtl());

		for (int i = 0; i < count; i++) {
			visibleChildren[i].addFocusables(views, direction, focusableMode);
		}

		// When set to FOCUS_AFTER_DESCENDANTS, we only add ourselves if
		// there aren't any focusable descendants.  this is
		// to avoid the focus search finding layouts when a more precise search
		// among the focusable children would be more interesting.
		if ((descendantFocusability == FOCUS_AFTER_DESCENDANTS) && focusSelf) {
			if (focusableCount == views.size()) {
				super.addFocusables(views, direction, focusableMode);
			}
		}
	}

	@Override
	public void addKeyboardNavigationClusters(java.util.Collection<View> views, int direction) {
		final int focusableCount = views.size();

		super.addKeyboardNavigationClusters(views, direction);

		if (focusableCount != views.size()) {
			// No need to look for clusters inside a cluster.
			return;
		}

		if (getDescendantFocusability() == FOCUS_BLOCK_DESCENDANTS) {
			return;
		}

		int count = 0;
		final View[] visibleChildren = new View[children.size()];
		for (View child : children) {
			if (child.getVisibility() == VISIBLE) {
				visibleChildren[count++] = child;
			}
		}

		FocusFinder.sort(visibleChildren, 0, count, this, isLayoutRtl());

		for (int i = 0; i < count; i++) {
			visibleChildren[i].addKeyboardNavigationClusters(views, direction);
		}
	}

	@Override
	public void addTouchables(ArrayList<View> views) {
		super.addTouchables(views);

		for (View child : children) {
			if (child.getVisibility() == VISIBLE) {
				child.addTouchables(views);
			}
		}
	}

	/* AOSP ViewGroup.offsetRectBetweenParentAndChild() */
	void offsetRectBetweenParentAndChild(View descendant, Rect rect, boolean offsetFromChildToParent, boolean clipToBounds) {
		// already in the same coord system :)
		if (descendant == this) {
			return;
		}

		ViewParent theParent = descendant.getParent();

		// search and offset up to the parent
		while ((theParent != null) && (theParent instanceof View) && (theParent != this)) {
			if (offsetFromChildToParent) {
				rect.offset(descendant.getLeft() - descendant.getScrollX(), descendant.getTop() - descendant.getScrollY());
				if (clipToBounds) {
					View p = (View)theParent;
					boolean intersected = rect.intersect(0, 0, p.getWidth(), p.getHeight());
					if (!intersected) {
						rect.setEmpty();
					}
				}
			} else {
				if (clipToBounds) {
					View p = (View)theParent;
					boolean intersected = rect.intersect(0, 0, p.getWidth(), p.getHeight());
					if (!intersected) {
						rect.setEmpty();
					}
				}
				rect.offset(descendant.getScrollX() - descendant.getLeft(), descendant.getScrollY() - descendant.getTop());
			}

			descendant = (View)theParent;
			theParent = descendant.getParent();
		}

		// now that we are up to this view, need to offset one more time
		// to get into our coordinate space
		if (theParent == this) {
			if (offsetFromChildToParent) {
				rect.offset(descendant.getLeft() - descendant.getScrollX(), descendant.getTop() - descendant.getScrollY());
			} else {
				rect.offset(descendant.getScrollX() - descendant.getLeft(), descendant.getScrollY() - descendant.getTop());
			}
		} else {
			throw new IllegalArgumentException("parameter must be a descendant of this view");
		}
	}

	public int getDescendantFocusability() {
		return mGroupFlags & FLAG_MASK_FOCUSABILITY;
	}

	/* AOSP; ATL: GTK does not let the focus into the children of a group that blocks them */
	public void setDescendantFocusability(int focusability) {
		switch (focusability) {
			case FOCUS_BEFORE_DESCENDANTS:
			case FOCUS_AFTER_DESCENDANTS:
			case FOCUS_BLOCK_DESCENDANTS:
				break;
			default:
				throw new IllegalArgumentException("must be one of FOCUS_BEFORE_DESCENDANTS, "
						+ "FOCUS_AFTER_DESCENDANTS, FOCUS_BLOCK_DESCENDANTS");
		}
		mGroupFlags &= ~FLAG_MASK_FOCUSABILITY;
		mGroupFlags |= (focusability & FLAG_MASK_FOCUSABILITY);
		native_setBlockDescendantsFocus(widget, focusability == FOCUS_BLOCK_DESCENDANTS);
	}
	private native void native_setBlockDescendantsFocus(long widget, boolean block);

	/* AOSP, without PFLAG_WANTS_FOCUS (GTK keeps the focus of a view that is not laid out yet) */
	@Override
	public boolean requestFocus(int direction, Rect previouslyFocusedRect) {
		int descendantFocusability = getDescendantFocusability();

		boolean result;
		switch (descendantFocusability) {
			case FOCUS_BLOCK_DESCENDANTS:
				result = super.requestFocus(direction, previouslyFocusedRect);
				break;
			case FOCUS_BEFORE_DESCENDANTS: {
				final boolean took = super.requestFocus(direction, previouslyFocusedRect);
				result = took ? took : onRequestFocusInDescendants(direction,
						previouslyFocusedRect);
				break;
			}
			case FOCUS_AFTER_DESCENDANTS: {
				final boolean took = onRequestFocusInDescendants(direction, previouslyFocusedRect);
				result = took ? took : super.requestFocus(direction, previouslyFocusedRect);
				break;
			}
			default:
				throw new IllegalStateException("descendant focusability must be "
						+ "one of FOCUS_BEFORE_DESCENDANTS, FOCUS_AFTER_DESCENDANTS, FOCUS_BLOCK_DESCENDANTS "
						+ "but is " + descendantFocusability);
		}
		return result;
	}

	@SuppressWarnings({"ConstantConditions"})
	protected boolean onRequestFocusInDescendants(int direction,
			Rect previouslyFocusedRect) {
		int index;
		int increment;
		int end;
		int count = getChildCount();
		if ((direction & FOCUS_FORWARD) != 0) {
			index = 0;
			increment = 1;
			end = count;
		} else {
			index = count - 1;
			increment = -1;
			end = -1;
		}
		final View[] children = this.children.toArray(new View[0]);
		for (int i = index; i != end; i += increment) {
			View child = children[i];
			if (child.getVisibility() == VISIBLE) {
				if (child.requestFocus(direction, previouslyFocusedRect)) {
					return true;
				}
			}
		}
		return false;
	}

	public void startViewTransition(View view) {}
	public void endViewTransition(View view) {}

	protected LayoutParams generateDefaultLayoutParams() {
		return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
	}

	public void focusableViewAvailable(View v) {}

	@Override
	public void setGravity(int gravity) {
		super.setGravity(gravity);
		// update children as necessary
		for (View child : children) {
			LayoutParams params = child.getLayoutParams();
			if (params.gravity == -1)
				child.setLayoutParams(params);
		}
	}

	protected void setChildrenDrawingOrderEnabled(boolean enabled) {}

	public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
		for (ViewGroup iter = this; iter != null; iter = iter.parent instanceof ViewGroup ? (ViewGroup)iter.parent : null)
			iter.disallowIntercept = disallowIntercept;
	}

	protected boolean isChildrenDrawingOrderEnabled() { return false; }

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

	public void setClipToPadding(boolean clipToPadding) {}

	public View findViewById(int id) {
		if (id == NO_ID) // as in AOSP: views without an id are not found by it
			return null;
		if (Log.isLoggable(TAG, Log.VERBOSE))
			Slog.v(TAG, "findViewById: looking for id: " + String.format("%x", id) + "(" + getResources().getAssets().getResourceName(id) + ")"
			            + " | checking: " + this + ",id: " + String.format("%x", this.getId()) + ", id_str: " + this.getIdName());
		if (this.id == id) {
			if (Log.isLoggable(TAG, Log.VERBOSE))
				Slog.v(TAG, "findViewById: found: " + this + " | id: " + String.format("%x", this.getId()) + ", id_str: " + this.getIdName());
			return this;
		}
		for (View child : children) {
			View result = child.findViewById(id);
			if (result != null)
				return result;
		}
		return null;
	}

	public void detachViewFromParent(View view) {
		if (children.remove(view) && view.hasTransientState()) {
			childHasTransientStateChanged(view, false);
		}
		view.parent = null;
		detachedChildren.add(view);
		if (!pendingHideDetachedChildren) {
			pendingHideDetachedChildren = true;
			post(new Runnable() {
				@Override
				public void run() {
					pendingHideDetachedChildren = false;
					for (View child : detachedChildren)
						child.native_setVisibility(child.widget, GONE, 0, false);
				}
			});
		}
	}

	private boolean touchscreenBlocksFocus;

	/* AOSP: on a device with a touchscreen, the focus search (Tab) skips the descendants that are not
	 * focusable in touch mode (AppCompat's Toolbar sets this, so Tab goes through the content) */
	public void setTouchscreenBlocksFocus(boolean touchscreenBlocksFocus) {
		this.touchscreenBlocksFocus = touchscreenBlocksFocus;
	}

	/* as AOSP: by default each child is clipped to its bounds */
	private boolean clipChildren = true;
	public void setClipChildren(boolean clipChildren) {
		if (this.clipChildren == clipChildren)
			return;
		this.clipChildren = clipChildren;
		native_setClipChildren(widget, clipChildren);
	}
	private native void native_setClipChildren(long widget, boolean clipChildren);

	@Override
	protected void dispatchSetPressed(boolean pressed) {
		final View[] children = this.children.toArray(new View[0]);
		final int count = children.length;
		for (int i = 0; i < count; i++) {
			final View child = children[i];
			// Children that are clickable on their own should not
			// show a pressed state when their parent view does.
			// Clearing a pressed state always propagates.
			if (!pressed || (!child.isClickable() && !child.isLongClickable())) {
				child.setPressed(pressed);
			}
		}
	}

	@Override
	public View findViewWithTag(Object tag) {
		if (Objects.equals(tag, getTag()))
			return this;
		for (View child : children) {
			View result = child.findViewWithTag(tag);
			if (result != null)
				return result;
		}
		return null;
	}

	public void setLayoutTransition(LayoutTransition transition) {
		this.transition = transition;
	}

	public boolean drawChild(Canvas canvas, View child, long drawingTime) {
		if (canvas instanceof GskCanvas)
			native_drawChild(widget, child.widget, ((GskCanvas)canvas).snapshot);
		return false;
	}

	protected void cleanupLayoutState(View child) {}

	/**
	 * AOSP ViewGroup.shouldDelayChildPressedState(): true unless the container never scrolls
	 * (FrameLayout, LinearLayout, RelativeLayout and AbsoluteLayout say so), so that a pressed
	 * child inside a scrolling container shows its pressed state only after the tap timeout.
	 */
	public boolean shouldDelayChildPressedState() {
		return true;
	}

	/* AOSP ViewGroup.mChildCountWithTransientState, childHasTransientStateChanged() and hasTransientState() */
	private int mChildCountWithTransientState = 0;

	@Override
	public void childHasTransientStateChanged(View child, boolean childHasTransientState) {
		final boolean oldHasTransientState = hasTransientState();
		if (childHasTransientState) {
			mChildCountWithTransientState++;
		} else {
			mChildCountWithTransientState--;
		}

		final boolean newHasTransientState = hasTransientState();
		if (parent != null && oldHasTransientState != newHasTransientState) {
			parent.childHasTransientStateChanged(this, newHasTransientState);
		}
	}

	@Override
	public boolean hasTransientState() {
		return mChildCountWithTransientState > 0 || super.hasTransientState();
	}

	public void offsetChildrenTopAndBottom(int offset) {
		// FIXME
	}

	private native void native_offsetRect(long from_widget, long to_widget, Rect rect);

	public final void offsetDescendantRectToMyCoords(View descendant, Rect rect) {
		native_offsetRect(descendant.widget, widget, rect);
	}

	public final void offsetRectIntoDescendantCoords(View descendant, Rect rect) {
		native_offsetRect(widget, descendant.widget, rect);
	}

	public boolean getClipToPadding() { return false; }

	public boolean isTransitionGroup() { return false; }

	public static class LayoutParams {
		public static final int FILL_PARENT = -1;
		public static final int MATCH_PARENT = -1;
		public static final int WRAP_CONTENT = -2;

		public int width = 0;
		public int height = 0;
		public float weight = 0;
		public int gravity = -1;

		public LayoutParams() {
			// FIXME
		}

		public LayoutParams(int width, int height) {
			this.width = width;
			this.height = height;
		}

		public LayoutParams(int width, int height, float weight) {
			this.width = width;
			this.height = height;
			this.weight = weight;
		}

		public LayoutParams(Context context, AttributeSet attrs) {
			TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ViewGroup_Layout);
			setBaseAttributes(a, R.styleable.ViewGroup_Layout_layout_width, R.styleable.ViewGroup_Layout_layout_height);
			a.recycle();
			a = context.obtainStyledAttributes(attrs, new int[] {android.R.attr.layout_gravity});
			gravity = a.getInt(0, -1);
			a.recycle();
		}

		protected void setBaseAttributes(TypedArray a, int widthAttr, int heightAttr) {
			width = a.getLayoutDimension(widthAttr, "layout_width");
			height = a.getLayoutDimension(heightAttr, "layout_height");
		}

		public void resolveLayoutDirection(int layoutDirection) {}

		/**
		 * Used to animate layouts.
		 */
		//public LayoutAnimationController.AnimationParameters layoutAnimationParameters;
	}

	public static class MarginLayoutParams extends ViewGroup.LayoutParams {
		public int leftMargin;
		public int topMargin;
		public int rightMargin;
		public int bottomMargin;

		public MarginLayoutParams() {
			super();
		}

		public MarginLayoutParams(LayoutParams params) {
			super();
			width = params.width;
			height = params.height;
		}

		public MarginLayoutParams(int width, int height) {
			super(width, height);
		}

		public MarginLayoutParams(int width, int height, float weight) {
			super(width, height, weight);
		}

		public MarginLayoutParams(Context context, AttributeSet attributeSet) {
			super(context, attributeSet);
			TypedArray a = context.obtainStyledAttributes(attributeSet, com.android.internal.R.styleable.ViewGroup_MarginLayout);

			int margin = a.getDimensionPixelSize(com.android.internal.R.styleable.ViewGroup_MarginLayout_layout_margin, -1);

			int marginVertical = a.getDimensionPixelSize(com.android.internal.R.styleable.ViewGroup_MarginLayout_layout_marginVertical, -1);
			int marginHorizontal = a.getDimensionPixelSize(com.android.internal.R.styleable.ViewGroup_MarginLayout_layout_marginHorizontal, -1);

			/* like AOSP, relative margins may be negative (AppCompat's menu item icon) */
			int marginStart = a.getDimensionPixelSize(com.android.internal.R.styleable.ViewGroup_MarginLayout_layout_marginStart, DEFAULT_MARGIN_RELATIVE);
			int marginEnd = a.getDimensionPixelSize(com.android.internal.R.styleable.ViewGroup_MarginLayout_layout_marginEnd, DEFAULT_MARGIN_RELATIVE);

			leftMargin = 0;
			topMargin = 0;
			rightMargin = 0;
			bottomMargin = 0;

			if (margin >= 0) {
				leftMargin = margin;
				topMargin = margin;
				rightMargin = margin;
				bottomMargin = margin;
			} else {
				if (marginVertical >= 0) {
					topMargin = marginVertical;
					bottomMargin = marginVertical;
				} else {
					topMargin = a.getDimensionPixelSize(com.android.internal.R.styleable.ViewGroup_MarginLayout_layout_marginTop, 0);
					bottomMargin = a.getDimensionPixelSize(com.android.internal.R.styleable.ViewGroup_MarginLayout_layout_marginBottom, 0);
				}

				if (marginHorizontal >= 0) {
					leftMargin = marginHorizontal;
					rightMargin = marginHorizontal;
				} else {
					leftMargin = a.getDimensionPixelSize(com.android.internal.R.styleable.ViewGroup_MarginLayout_layout_marginLeft, 0);
					rightMargin = a.getDimensionPixelSize(com.android.internal.R.styleable.ViewGroup_MarginLayout_layout_marginRight, 0);

					/* like AOSP, start/end override left/right once resolved */
					startMargin = marginStart;
					endMargin = marginEnd;
				}
			}
			needResolution = isMarginRelative();

			a.recycle();
		}

		public MarginLayoutParams(ViewGroup.MarginLayoutParams source) {
			super();
			width = source.width;
			height = source.height;
			leftMargin = source.leftMargin;
			topMargin = source.topMargin;
			rightMargin = source.rightMargin;
			bottomMargin = source.bottomMargin;
			startMargin = source.startMargin;
			endMargin = source.endMargin;
			layoutDirection = source.layoutDirection;
			needResolution = source.needResolution;
		}

		/* like AOSP: the relative margins are kept apart and, if one is set, resolved into
		 * leftMargin/rightMargin with the layout direction of the view (an unset one gives 0) */
		private static final int DEFAULT_MARGIN_RELATIVE = Integer.MIN_VALUE;
		private int startMargin = DEFAULT_MARGIN_RELATIVE;
		private int endMargin = DEFAULT_MARGIN_RELATIVE;
		private int layoutDirection = LAYOUT_DIRECTION_LTR;
		private boolean needResolution;

		public void setMargins(int left, int top, int right, int bottom) {
			leftMargin = left;
			topMargin = top;
			rightMargin = right;
			bottomMargin = bottom;
			needResolution = isMarginRelative();
		}

		/** @hide */
		public void setMarginsRelative(int start, int top, int end, int bottom) {
			startMargin = start;
			topMargin = top;
			endMargin = end;
			bottomMargin = bottom;
			needResolution = true;
		}

		public int getMarginStart() {
			if (startMargin != DEFAULT_MARGIN_RELATIVE)
				return startMargin;
			if (needResolution)
				doResolveMargins();
			return isLayoutRtl() ? rightMargin : leftMargin;
		}

		public int getMarginEnd() {
			if (endMargin != DEFAULT_MARGIN_RELATIVE)
				return endMargin;
			if (needResolution)
				doResolveMargins();
			return isLayoutRtl() ? leftMargin : rightMargin;
		}

		public void setMarginStart(int marginStart) {
			startMargin = marginStart;
			needResolution = true;
		}
		public void setMarginEnd(int marginEnd) {
			endMargin = marginEnd;
			needResolution = true;
		}

		public void setLayoutDirection(int layoutDirection) {
			if (layoutDirection != LAYOUT_DIRECTION_LTR && layoutDirection != LAYOUT_DIRECTION_RTL)
				return;
			if (layoutDirection != this.layoutDirection) {
				this.layoutDirection = layoutDirection;
				needResolution = isMarginRelative();
			}
		}

		public int getLayoutDirection() { return layoutDirection; }

		/** @hide */
		public boolean isLayoutRtl() { return layoutDirection == LAYOUT_DIRECTION_RTL; }

		public boolean isMarginRelative() {
			return startMargin != DEFAULT_MARGIN_RELATIVE || endMargin != DEFAULT_MARGIN_RELATIVE;
		}

		@Override
		public void resolveLayoutDirection(int layoutDirection) {
			setLayoutDirection(layoutDirection);
			if (!isMarginRelative() || !needResolution)
				return;
			doResolveMargins();
		}

		private void doResolveMargins() {
			if (isLayoutRtl()) {
				leftMargin = (endMargin > DEFAULT_MARGIN_RELATIVE) ? endMargin : 0;
				rightMargin = (startMargin > DEFAULT_MARGIN_RELATIVE) ? startMargin : 0;
			} else {
				leftMargin = (startMargin > DEFAULT_MARGIN_RELATIVE) ? startMargin : 0;
				rightMargin = (endMargin > DEFAULT_MARGIN_RELATIVE) ? endMargin : 0;
			}
			needResolution = false;
		}
	}

	public interface OnHierarchyChangeListener {
		public void onChildViewAdded(View parent, View child);
		public void onChildViewRemoved(View parent, View child);
	}

	/* onStartNestedScroll() never accepts, so like in AOSP there is nothing to do for the
	 * other NestedScrollingParent callbacks; subclasses that take part override them */

	@Override
	public void onNestedScrollAccepted(View child, View target, int nestedScrollAxes) {}

	@Override
	public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {}

	@Override
	public void onNestedScroll(View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed) {}

	@Override
	public void onStopNestedScroll(View target) {}

	public void requestChildFocus(View child, View focused) {}

	public boolean getClipChildren() {
		return clipChildren;
	}

	@Override
	public void onDescendantInvalidated(View child, View target) {}

	public boolean getTouchscreenBlocksFocus() { return touchscreenBlocksFocus; }

	public ViewGroupOverlay getOverlay() {
		if (viewGroupOverlay == null)
			viewGroupOverlay = new ViewGroupOverlay();
		return viewGroupOverlay;
	}

	public void scheduleLayoutAnimation() {}

	private boolean layoutSuppressed;

	/* API 29: transitions hold back layout passes while they run; layout is not
	 * actually held back, but a layout pass follows the release as in AOSP */
	public void suppressLayout(boolean suppress) {
		boolean release = layoutSuppressed && !suppress;
		layoutSuppressed = suppress;
		if (release)
			requestLayout();
	}

	public boolean isLayoutSuppressed() {
		return layoutSuppressed;
	}

	/* API 29; children are drawn in the order of their index */
	public final int getChildDrawingOrder(int drawingPosition) {
		return drawingPosition;
	}
}
