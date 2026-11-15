package android.widget;

import android.animation.ValueAnimator;
import android.atl.annotation.Export;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.animation.Animator;
import android.transition.Fade;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.transition.TransitionSet;
import android.transition.Visibility;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import java.lang.ref.WeakReference;

public class PopupWindow {

	int input_method_mode = 0;
	private Transition enter_transition;
	private Transition exit_transition;
	private static final int ANIMATION_STYLE_DEFAULT = -1;
	private int animation_style = ANIMATION_STYLE_DEFAULT;
	private ValueAnimator fade;
	private boolean fading_out;

	/*
	 * AOSP plays the popupEnterTransition and popupExitTransition of the theme (inflated as in
	 * PopupWindow.getTransition()) on the popup's window, and the windowEnterAnimation and
	 * windowExitAnimation of an animation style that the app set. ATL has no transition framework
	 * (TransitionManager): the Fade of a transition runs as it does in AOSP's Fade (from alpha 0 to 1
	 * when it appears, from 1 to 0 when it disappears) with its duration, start delay and interpolator,
	 * on the popover; the clip reveal of Android 14's popup_window_enter.xml is left out.
	 */
	private Transition getTransition(Context context, int resId) {
		if (resId != 0 && resId != com.android.internal.R.transition.no_transition) {
			try {
				final Transition transition = TransitionInflater.from(context).inflateTransition(resId);
				if (transition != null) {
					final boolean isEmpty = transition instanceof TransitionSet && ((TransitionSet)transition).getTransitionCount() == 0;
					if (!isEmpty)
						return transition;
				}
			} catch (RuntimeException e) {
				Log.w("PopupWindow", "cannot inflate transition 0x" + Integer.toHexString(resId) + ": " + e);
			}
		}
		return null;
	}

	/* the first Fade of the transition that fades in (Visibility.MODE_IN) or out (MODE_OUT) */
	private static Fade findFade(Transition transition, int mode) {
		if (transition instanceof Fade)
			return (((Fade)transition).getMode() & mode) != 0 ? (Fade)transition : null;
		if (transition instanceof TransitionSet) {
			TransitionSet set = (TransitionSet)transition;
			for (int i = 0; i < set.getTransitionCount(); i++) {
				Fade fade = findFade(set.getTransitionAt(i), mode);
				if (fade != null)
					return fade;
			}
		}
		return null;
	}

	/* the windowEnterAnimation/windowExitAnimation of an animation style that was set (the default
	 * style, and Animation.PopupWindow, which the transitions replace, have none here) */
	private Animation styleAnimation(boolean enter) {
		if (animation_style == ANIMATION_STYLE_DEFAULT || animation_style == 0 || contentView == null)
			return null;
		TypedArray a = contentView.getContext().obtainStyledAttributes(animation_style, com.android.internal.R.styleable.WindowAnimation);
		int id = a.getResourceId(enter ? com.android.internal.R.styleable.WindowAnimation_windowEnterAnimation
		                               : com.android.internal.R.styleable.WindowAnimation_windowExitAnimation, 0);
		a.recycle();
		if (id == 0)
			return null;
		try {
			return AnimationUtils.loadAnimation(contentView.getContext(), id);
		} catch (RuntimeException e) {
			Log.w("PopupWindow", "cannot load animation 0x" + Integer.toHexString(id) + ": " + e);
			return null;
		}
	}

	private void resetFade() {
		if (fade != null) {
			fade.cancel();
			fade = null;
		}
		if (contentView != null)
			contentView.clearAnimation();
		fading_out = false;
		native_resetFade(popover);
	}

	private void playFade(final Fade fadeTransition, final boolean appear, final Runnable done) {
		fade = ValueAnimator.ofFloat(0, 1);
		fade.setDuration(fadeTransition.getDuration() >= 0 ? fadeTransition.getDuration() : 300);
		if (fadeTransition.getStartDelay() > 0)
			fade.setStartDelay(fadeTransition.getStartDelay());
		if (fadeTransition.getInterpolator() != null)
			fade.setInterpolator(fadeTransition.getInterpolator());
		native_setOpacity(popover, appear ? 0 : 1);
		fade.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator animation) {
				float f = (Float)animation.getAnimatedValue();
						native_setOpacity(popover, appear ? f : 1 - f);
			}
		});
		fade.addListener(new android.animation.AnimatorListenerAdapter() {
			private boolean canceled;

			@Override
			public void onAnimationCancel(Animator animation) {
				canceled = true;
			}

			@Override
			public void onAnimationEnd(Animator animation) {
				if (!canceled && done != null)
					done.run();
			}
		});
		fade.start();
	}

	/* after the popover was shown */
	private void startEnterAnimation() {
		resetFade();
		if (ValueAnimator.getDurationScale() == 0)
			return;
		Animation animation = styleAnimation(true);
		if (animation != null)
			contentView.startAnimation(animation);
		Fade enterFade = findFade(enter_transition, Visibility.MODE_IN);
		if (enterFade != null)
			playFade(enterFade, true, null);
	}

	private void finishDismiss() {
		resetFade();
		native_dismiss(popover);
	}

	public PopupWindow(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		popover = native_constructor();
		TypedArray a = context.obtainStyledAttributes(attrs, com.android.internal.R.styleable.PopupWindow, defStyleAttr, defStyleRes);
		overlap_anchor = a.getBoolean(com.android.internal.R.styleable.PopupWindow_overlapAnchor, false);
		if (a.hasValueOrEmpty(com.android.internal.R.styleable.PopupWindow_popupAnimationStyle)) {
			int animStyle = a.getResourceId(com.android.internal.R.styleable.PopupWindow_popupAnimationStyle, 0);
			animation_style = animStyle == com.android.internal.R.style.Animation_PopupWindow ? ANIMATION_STYLE_DEFAULT : animStyle;
		}
		enter_transition = getTransition(context, a.getResourceId(com.android.internal.R.styleable.PopupWindow_popupEnterTransition, 0));
		if (a.hasValueOrEmpty(com.android.internal.R.styleable.PopupWindow_popupExitTransition))
			exit_transition = getTransition(context, a.getResourceId(com.android.internal.R.styleable.PopupWindow_popupExitTransition, 0));
		else
			exit_transition = enter_transition == null ? null : enter_transition.clone();
		a.recycle();
		int[] colors = themeColors(context);
		native_setColors(popover, colors[0], colors[1]);
	}

	/* background and text color of Material popups: colorBackgroundFloating and textColorPrimary */
	static int[] themeColors(Context context) {
		/* sorted by id, as obtainStyledAttributes() expects */
		TypedArray a = context.obtainStyledAttributes(new int[] {com.android.internal.R.attr.textColorPrimary,
		                                                         com.android.internal.R.attr.colorBackgroundFloating});
		ColorStateList text = a.getColorStateList(0);
		int[] colors = {a.getColor(1, 0xffffffff), text != null ? text.getDefaultColor() : 0xff000000};
		a.recycle();
		return colors;
	}

	public PopupWindow(Context context, AttributeSet attrs, int defStyleAttr) {
		this(context, attrs, defStyleAttr, 0);
	}

	public PopupWindow(Context context) {
		this(context, null);
	}

	public PopupWindow() {
		popover = native_constructor();
	}

	public PopupWindow(View contentView, int width, int height, boolean focusable) {
		popover = native_constructor();
		setContentView(contentView);
		setWidth(width);
		setHeight(height);
		setFocusable(focusable);
	}

	public PopupWindow(View contentView, int width, int height) {
		this(contentView, width, height, false);
	}

	/* AOSP PopupWindow(int, int) -> this(null, width, height) */
	public PopupWindow(int width, int height) {
		popover = native_constructor();
		setWidth(width);
		setHeight(height);
		setFocusable(false);
	}

	public PopupWindow(Context context, AttributeSet attrs) {
		this(context, attrs, com.android.internal.R.attr.popupWindowStyle);
	}

	/* AOSP PopupWindow(View) -> this(contentView, 0, 0) */
	public PopupWindow(View contentView) {
		this(contentView, 0, 0);
	}

	private View contentView;
	private Drawable background;
	private long popover; // native pointer to GtkPopover
	private int anchored_gravity = DEFAULT_ANCHORED_GRAVITY;
	private boolean overlap_anchor;

	private static final int DEFAULT_ANCHORED_GRAVITY = Gravity.TOP | Gravity.START;

	public interface OnDismissListener {
		public void onDismiss();
	}

	public void setBackgroundDrawable(Drawable background) {
		this.background = background;
		if (contentView != null) {
			updateBackgroundView();
		}
	}

	/* the view that is handed to the popover: the content view, or - with a background - a FrameLayout
	 * around it that carries the background, as AOSP's PopupWindow.createBackgroundView() does, so that
	 * the padding of the background (a nine-patch, an InsetDrawable) is not applied to the content view
	 * itself, where ListPopupWindow would count it a second time */
	private void updateBackgroundView() {
		if (contentView == null) {
			native_setContentView(popover, 0);
			return;
		}
		if (contentView.getParent() instanceof ViewGroup)
			((ViewGroup)contentView.getParent()).removeView(contentView);
		View backgroundView = contentView;
		if (background != null) {
			final ViewGroup.LayoutParams layoutParams = contentView.getLayoutParams();
			final int height = layoutParams != null && layoutParams.height == ViewGroup.LayoutParams.WRAP_CONTENT
			                       ? ViewGroup.LayoutParams.WRAP_CONTENT
			                       : ViewGroup.LayoutParams.MATCH_PARENT;
			final FrameLayout wrapper = new FrameLayout(contentView.getContext());
			wrapper.addView(contentView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height));
			wrapper.setBackgroundDrawable(background);
			backgroundView = wrapper;
		}
		native_setContentView(popover, backgroundView.widget);
	}

	public void setInputMethodMode(int mode) {
		input_method_mode = mode;
	}

	public int getInputMethodMode() {
		return input_method_mode;
	}

	/** AOSP isAboveAnchor(): whether the last showAsDropDown()/update() placed the popup above its anchor */
	public boolean isAboveAnchor() {
		return native_isAboveAnchor(popover);
	}

	public boolean isShowing() {
		return native_isShowing(popover);
	}

	private boolean focusable;

	/* like AOSP, takes effect when the popup is shown (or updated) next */
	public void setFocusable(boolean focusable) {
		this.focusable = focusable;
		native_setFocusable(popover, focusable);
		applyTouchModal();
	}

	public boolean isFocusable() {
		return focusable;
	}

	public Drawable getBackground() {
		return background;
	}

	public void setContentView(View view) {
		contentView = view;
		updateBackgroundView();
	}

	private final int[] mTmpDrawingLocation = new int[2];
	private final Rect mTempRect = new Rect();

	/* AOSP's PopupWindow.getMaxAvailableHeight() (android-14). getAppRootView() is the anchor's root
	 * view (ATL has no WindowManagerGlobal); ATL has no system bars to ignore, so the display frame
	 * for ignoreBottomDecorations is the visible one. */
	public int getMaxAvailableHeight(View anchor) {
		return getMaxAvailableHeight(anchor, 0);
	}

	public int getMaxAvailableHeight(View anchor, int yOffset) {
		return getMaxAvailableHeight(anchor, yOffset, false);
	}

	public int getMaxAvailableHeight(View anchor, int yOffset, boolean ignoreBottomDecorations) {
		final Rect displayFrame = new Rect();

		final View appView = anchor.getRootView();
		appView.getWindowVisibleDisplayFrame(displayFrame);

		final int[] anchorPos = mTmpDrawingLocation;
		anchor.getLocationOnScreen(anchorPos);

		final int bottomEdge = displayFrame.bottom;

		final int distanceToBottom;
		if (overlap_anchor) {
			distanceToBottom = bottomEdge - anchorPos[1] - yOffset;
		} else {
			distanceToBottom = bottomEdge - (anchorPos[1] + anchor.getHeight()) - yOffset;
		}
		final int distanceToTop = anchorPos[1] - displayFrame.top + yOffset;

		// anchorPos[1] is distance from anchor to top of screen
		int returnedHeight = Math.max(distanceToBottom, distanceToTop);
		if (background != null) {
			background.getPadding(mTempRect);
			returnedHeight -= mTempRect.top + mTempRect.bottom;
		}

		return returnedHeight;
	}

	private boolean mOutsideTouchable = false;

	public boolean isOutsideTouchable() {
		return mOutsideTouchable;
	}

	public void setOutsideTouchable(boolean touchable) {
		mOutsideTouchable = touchable;
		applyTouchModal();
	}

	/* AOSP's PopupWindow is touch-modal unless told otherwise (mTouchModal): a focusable popup takes the presses
	 * outside of it and PopupDecorView dismisses it on such a press, and it has the keys (Back and Escape dismiss
	 * it). A popup that is not focusable is never touch-modal; it gets the outside presses as ACTION_OUTSIDE
	 * only if it is outside-touchable. The native touch modality stands for both. */
	private boolean mTouchModal = true;

	private void applyTouchModal() {
		native_setTouchModal(popover, focusable ? mTouchModal || mOutsideTouchable : mOutsideTouchable);
	}

	/* the state AOSP's PopupWindow keeps for the window it would add; ATL's popover does not use it */
	private View.OnTouchListener mTouchInterceptor;
	private boolean mClippingEnabled = true;
	private boolean mClipToScreen;
	private boolean mAllowScrollingAnchorParent = true;
	private boolean mLayoutInsetDecor = false;
	private boolean mAttachedInDecor = true;
	private boolean mAttachedInDecorSet = false;
	private int mWindowLayoutType = 1000; // WindowManager.LayoutParams.TYPE_APPLICATION_PANEL (FIRST_SUB_WINDOW), not declared in ATL
	private int mSoftInputMode = 1; // SOFT_INPUT_STATE_UNCHANGED, not declared in ATL
	private float mElevation;
	private Rect mEpicenterBounds;

	public void setTouchInterceptor(View.OnTouchListener l) {
		mTouchInterceptor = l;
	}

	/* AOSP's PopupWindow.hasContentView() */
	protected final boolean hasContentView() {
		return contentView != null;
	}

	public void showAsDropDown(View anchor, int xoff, int yoff, int gravity) {
		if (isShowing() || !hasContentView()) {
			return;
		}

		attachToAnchor(anchor, xoff, yoff, gravity);
		anchored_gravity = gravity;
		android.atl.ATLViewDump.removeRoot(this); // debugging aid (atl-doctor)
		android.atl.ATLViewDump.addRoot(this);
		native_showAsDropDown(popover, anchor.widget, anchor.getWidth(), anchor.getHeight(), xoff, yoff,
		                      Gravity.getAbsoluteGravity(gravity, anchor.getLayoutDirection()), overlap_anchor);
		startEnterAnimation();
		takeWindowFocus(anchor);
	}

	/* AOSP's PopupWindow.attachToAnchor()/detachFromAnchor()/alignToAnchor(): the popup follows its anchor
	 * whenever the anchor's root view is laid out, so it also lands at the anchor when that was added
	 * right before show() and had no place yet. ATL has no decor view; isShowing() stands for
	 * hasDecorView() and update() places the popup at the anchor (findDropDownPosition() is native). */
	private WeakReference<View> mAnchor;
	private WeakReference<View> mAnchorRoot;
	private int mAnchorXoff;
	private int mAnchorYoff;

	/* posted: the root is laid out from inside GTK's allocation, where the popover cannot be placed */
	private final Runnable mAlignToAnchor = new Runnable() {
		@Override
		public void run() {
			alignToAnchor();
		}
	};

	private final View.OnLayoutChangeListener mOnLayoutChangeListener = new View.OnLayoutChangeListener() {
		@Override
		public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
			v.removeCallbacks(mAlignToAnchor);
			v.post(mAlignToAnchor);
		}
	};

	protected void detachFromAnchor() {
		final View anchorRoot = mAnchorRoot != null ? mAnchorRoot.get() : null;
		if (anchorRoot != null) {
			anchorRoot.removeOnLayoutChangeListener(mOnLayoutChangeListener);
		}

		mAnchor = null;
		mAnchorRoot = null;
	}

	protected void attachToAnchor(View anchor, int xoff, int yoff, int gravity) {
		detachFromAnchor();

		final View anchorRoot = anchor.getRootView();
		anchorRoot.addOnLayoutChangeListener(mOnLayoutChangeListener);

		mAnchor = new WeakReference<>(anchor);
		mAnchorRoot = new WeakReference<>(anchorRoot);

		mAnchorXoff = xoff;
		mAnchorYoff = yoff;
	}

	private void alignToAnchor() {
		final View anchor = mAnchor != null ? mAnchor.get() : null;
		if (anchor != null && anchor.isAttachedToWindow() && isShowing()) {
			update(anchor, mAnchorXoff, mAnchorYoff, -1, -1);
		}
	}

	/* the root view of the window whose focus this popup has taken */
	private View focusTakenFrom;

	/* like AOSP: a focusable popup is a window of its own that gets the window focus, which the
	 * window it is shown over loses (a popup that is not focusable leaves the focus there) */
	private void takeWindowFocus(View anchor) {
		if (!focusable || contentView == null || !isShowing())
			return;
		if (focusTakenFrom == null) {
			focusTakenFrom = anchor.getRootView();
			native_setOnDismissListener(popover); // to give the focus back on every dismissal
			focusTakenFrom.dispatchRootWindowFocusChanged(false);
		}
		contentView.dispatchRootWindowFocusChanged(true);
	}

	/* the window below gets the focus back once the popup is gone; like the window manager, after
	 * the popup's window is removed, so a popup that is shown again right away keeps it */
	private void returnWindowFocus() {
		if (focusTakenFrom == null)
			return;
		new Handler(Looper.getMainLooper()).post(new Runnable() {
			@Override
			public void run() {
				if (focusTakenFrom == null || isShowing())
					return;
				View root = focusTakenFrom;
				focusTakenFrom = null;
				root.dispatchRootWindowFocusChanged(true);
			}
		});
	}

	public View getContentView() {
		return contentView;
	}

	public void setTouchable(boolean touchable) {
		native_setTouchable(popover, touchable);
	}

	public void showAsDropDown(View anchor) {
		showAsDropDown(anchor, 0, 0);
	}

	public void showAsDropDown(View anchor, int xoff, int yoff) {
		if (!anchor.isAttachedToWindow()) {
			Log.e("PopupWindow", "anchor is not attached to window");
			return;
		}
		showAsDropDown(anchor, xoff, yoff, DEFAULT_ANCHORED_GRAVITY);
	}

	public void showAtLocation(View parent, int gravity, int x, int y) {
		if (isShowing() || !hasContentView()) {
			return;
		}

		if (gravity == Gravity.NO_GRAVITY)
			gravity = Gravity.TOP | Gravity.START;
		android.atl.ATLViewDump.removeRoot(this); // debugging aid (atl-doctor)
		android.atl.ATLViewDump.addRoot(this);
		mLocationGravity = Gravity.getAbsoluteGravity(gravity, parent.getLayoutDirection());
		native_showAtLocation(popover, parent.widget, mLocationGravity, x, y);
		startEnterAnimation();
		takeWindowFocus(parent);
	}

	public void dismiss() {
		if (fading_out)
			return;
		detachFromAnchor();
		resetFade();
		Fade exitFade = ValueAnimator.getDurationScale() == 0 ? null : findFade(exit_transition, Visibility.MODE_OUT);
		Animation animation = ValueAnimator.getDurationScale() == 0 ? null : styleAnimation(false);
		if ((exitFade == null && animation == null) || !native_isMapped(popover)) {
			native_dismiss(popover);
			return;
		}
		/* as on AOSP, the dismiss listener is called at once, the popup is no longer showing and takes no
		 * input; it is popped down when the animations end */
		fading_out = true;
		native_startFadeOut(popover);
		final int[] running = {0};
		final Runnable onEnd = new Runnable() {
			@Override
			public void run() {
				if (--running[0] == 0 && fading_out)
					finishDismiss();
			}
		};
		if (animation != null) {
			running[0]++;
			animation.setFillAfter(true);
			animation.setAnimationListener(new Animation.AnimationListener() {
				@Override
				public void onAnimationStart(Animation animation) {}

				@Override
				public void onAnimationRepeat(Animation animation) {}

				@Override
				public void onAnimationEnd(Animation animation) {
					onEnd.run();
				}
			});
		}
		if (exitFade != null)
			running[0]++;
		if (animation != null)
			contentView.startAnimation(animation);
		if (exitFade != null)
			playFade(exitFade, false, onEnd);
	}

	/* as on AOSP, 0 means no window animation (the transitions are set apart); the style's window enter
	 * and exit animation are played on the content */
	public void setAnimationStyle(int animationStyle) {
		animation_style = animationStyle;
	}

	public int getAnimationStyle() {
		return animation_style;
	}

	public void setTouchModal(boolean touchModal) {
		mTouchModal = touchModal;
		applyTouchModal();
	}

	public float getElevation() {
		return mElevation;
	}

	public void setElevation(float elevation) {
		mElevation = elevation;
	}

	public void update(View anchor, int xoff, int yoff, int width, int height) {
		native_update(popover, anchor.widget, anchor.getWidth(), anchor.getHeight(), xoff, yoff, width, height,
		              Gravity.getAbsoluteGravity(anchored_gravity, anchor.getLayoutDirection()), overlap_anchor);
	}

	public void setWindowLayoutType(int layoutType) {
		mWindowLayoutType = layoutType;
	}

	public int getWindowLayoutType() {
		return mWindowLayoutType;
	}

	public void setIsClippedToScreen(boolean enabled) {
		mClipToScreen = enabled;
	}

	public boolean isClippedToScreen() {
		return mClipToScreen;
	}

	public void setEpicenterBounds(Rect bounds) {
		mEpicenterBounds = bounds != null ? new Rect(bounds) : null;
	}

	public Rect getEpicenterBounds() {
		return mEpicenterBounds != null ? new Rect(mEpicenterBounds) : null;
	}

	public void setAttachedInDecor(boolean enabled) {
		mAttachedInDecor = enabled;
		mAttachedInDecorSet = true;
	}

	public boolean isAttachedInDecor() {
		return mAttachedInDecor;
	}

	protected final void setLayoutInsetDecor(boolean enabled) {
		mLayoutInsetDecor = enabled;
	}

	protected final boolean isLayoutInsetDecor() {
		return mLayoutInsetDecor;
	}

	public boolean isClippingEnabled() {
		return mClippingEnabled;
	}

	/* a null transition turns the animation off, as on AOSP */
	public void setEnterTransition(Transition enterTransition) {
		enter_transition = enterTransition;
	}

	public Transition getEnterTransition() {
		return enter_transition;
	}

	public void setExitTransition(Transition exitTransition) {
		exit_transition = exitTransition;
	}

	public Transition getExitTransition() {
		return exit_transition;
	}

	public void setClippingEnabled(boolean enabled) {
		mClippingEnabled = enabled;
	}

	/* TODO: handle LayoutParams.WRAP_CONTENT and LayoutParams.MATCH_PARENT */
	public void setWidth(int width) {
		if (width < 0)
			return;

		native_setWidth(popover, width);
	}

	public void setHeight(int height) {
		if (height < 0)
			return;

		native_setHeight(popover, height);
	}

	public int getWidth() {
		return native_getWidth(popover);
	}

	public int getHeight() {
		return native_getHeight(popover);
	}

	/* AOSP computeGravity(): the gravity of the last showAtLocation(), START | TOP before any (here made absolute
	 * when showing) */
	private int mLocationGravity = Gravity.TOP | Gravity.LEFT;

	/** AOSP PopupWindow.update(int, int, int, int): the size as with setWidth()/setHeight() (-1 keeps it), and a
	 * shown popup moves to (x, y) in the window with the gravity of computeGravity(), drop-downs as well */
	public void update(int x, int y, int width, int height) {
		if (width >= 0)
			setWidth(width);
		if (height >= 0)
			setHeight(height);
		if (!isShowing() || !hasContentView())
			return;
		native_moveTo(popover, mLocationGravity, x, y);
	}
	private native void native_moveTo(long popover, int gravity, int x, int y);

	/** AOSP PopupWindow.update(int, int, int, int, boolean): force only makes AOSP re-apply unchanged values, which
	 * update(int, int, int, int) does here anyway */
	public void update(int x, int y, int width, int height, boolean force) {
		update(x, y, width, height);
	}

	/** AOSP PopupWindow.update(View, int, int): the size changes, the popup stays at the offsets of its last anchor */
	public void update(View anchor, int width, int height) {
		if (isShowing() && hasContentView())
			update(anchor, mAnchorXoff, mAnchorYoff, width, height);
		else {
			if (width >= 0)
				setWidth(width);
			if (height >= 0)
				setHeight(height);
		}
	}

	/** AOSP PopupWindow.update(): a shown popup takes over changed animation, flags and gravity. ATL's setters
	 * (setFocusable(), setTouchable(), setOutsideTouchable()) change the GtkPopover right away, so nothing is left
	 * to apply */
	public void update() {}

	/** AOSP PopupWindow.update(int, int): update(p.x, p.y, width, height, false), the size at the current
	 * position (-1 keeps a dimension) */
	public void update(int width, int height) {
		if (width >= 0)
			setWidth(width);
		if (height >= 0)
			setHeight(height);
	}

	public void setWindowLayoutMode(int widthSpec, int heightSpec) {}

	public boolean isTouchable() {
		return native_isTouchable(popover);
	}

	public void setOverlapAnchor(boolean overlap) {
		overlap_anchor = overlap;
	}

	public boolean getOverlapAnchor() {
		return overlap_anchor;
	}

	public void setSoftInputMode(int mode) {
		mSoftInputMode = mode;
	}

	public int getSoftInputMode() {
		return mSoftInputMode;
	}

	protected native long native_constructor();
	protected native boolean native_isAboveAnchor(long widget);
	protected native void native_setColors(long widget, int background, int foreground);
	protected native void native_setContentView(long widget, long contentView);
	protected native void native_showAsDropDown(long widget, long anchor, int anchorWidth, int anchorHeight, int xoff, int yoff, int gravity, boolean overlapAnchor);
	protected native void native_showAtLocation(long widget, long parent, int gravity, int x, int y);
	protected native boolean native_isShowing(long widget);
	protected native void native_setTouchable(long widget, boolean touchable);
	protected native void native_setTouchModal(long widget, boolean touchable);
	protected native void native_setFocusable(long widget, boolean focusable);
	protected native void native_dismiss(long widget);
	protected native boolean native_isMapped(long widget);
	protected native void native_startFadeOut(long widget);
	protected native void native_fadeOutDone(long widget);
	protected native void native_resetFade(long widget);
	protected native void native_setOpacity(long widget, float opacity);
	protected native void native_update(long widget, long anchor, int anchorWidth, int anchorHeight, int xoff, int yoff, int width, int height, int gravity, boolean overlapAnchor);
	private native void native_setOnDismissListener(long popover);

	private OnDismissListener on_dismiss_listener;

	/* like AOSP, a new listener replaces the old one */
	public void setOnDismissListener(OnDismissListener listener) {
		on_dismiss_listener = listener;
		native_setOnDismissListener(popover);
	}

	/* AOSP's PopupWindow.PopupDecorView.dispatchKeyEvent(): Back and Escape dismiss the popup. ATL
	 * has no decor view; the popup hands this the keys that reach it before its content gets them,
	 * and false is super.dispatchKeyEvent(), the content then gets the key. */
	@Export
	private boolean dispatchDecorKeyEvent(KeyEvent event) {
		if (event.getKeyCode() == KeyEvent.KEYCODE_BACK || event.getKeyCode() == KeyEvent.KEYCODE_ESCAPE) {
			final KeyEvent.DispatcherState state = contentView != null ? contentView.getKeyDispatcherState() : null;
			if (state == null) {
				return false;
			}

			if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
				state.startTracking(event, this);
				return true;
			} else if (event.getAction() == KeyEvent.ACTION_UP) {
				if (state.isTracking(event) && !event.isCanceled()) {
					dismiss();
					return true;
				}
			}
		}
		return false;
	}

	/* called from native code when the popup is dismissed */
	@Export
	private void dispatchDismiss() {
		if (on_dismiss_listener != null)
			on_dismiss_listener.onDismiss();
		returnWindowFocus();
	}
	public native void native_setWidth(long widget, int width);
	public native void native_setHeight(long widget, int height);
	public native int native_getWidth(long widget);
	public native int native_getHeight(long widget);
	public native boolean native_isTouchable(long widget);

	/* AOSP mSplitTouchEnabled: -1 = unset (then targetSdkVersion >= HONEYCOMB decides), 0 = off, 1 = on */
	private int mSplitTouchEnabled = -1;

	public boolean isSplitTouchEnabled() {
		if (mSplitTouchEnabled < 0 && contentView != null) {
			/* AOSP asks the Context the PopupWindow was made with; ATL keeps only the content view */
			return contentView.getContext().getApplicationInfo().targetSdkVersion >= android.os.Build.VERSION_CODES.HONEYCOMB;
		}
		return mSplitTouchEnabled == 1;
	}

	public void setSplitTouchEnabled(boolean enabled) {
		mSplitTouchEnabled = enabled ? 1 : 0;
	}
}
