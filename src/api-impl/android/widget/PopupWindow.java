package android.widget;

import android.animation.ValueAnimator;
import android.atl.annotation.Export;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.transition.Transition;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;

public class PopupWindow {

	int input_method_mode = 0;
	private Transition enter_transition;
	private Transition exit_transition;
	/* AOSP plays the popupEnterTransition/popupExitTransition of the theme; ATL can't inflate
	 * transitions and plays the fade of those of Android 14 (transition/popup_window_enter.xml:
	 * fade from 0.1 in 100 ms, together with a 250 ms clip reveal that is left out;
	 * popup_window_exit.xml: fade out in 300 ms) */
	private static final int ENTER_FADE_MS = 100;
	private static final int EXIT_FADE_MS = 300;
	private boolean enter_animated;
	private boolean exit_animated;
	private int animation_style = -1;

	private int fadeDuration(boolean enter) {
		if (animation_style == 0 || ValueAnimator.getDurationScale() == 0 || !(enter ? enter_animated : exit_animated))
			return 0;
		return enter ? ENTER_FADE_MS : EXIT_FADE_MS;
	}

	public PopupWindow(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		popover = native_constructor();
		TypedArray a = context.obtainStyledAttributes(attrs, com.android.internal.R.styleable.PopupWindow, defStyleAttr, defStyleRes);
		overlap_anchor = a.getBoolean(com.android.internal.R.styleable.PopupWindow_overlapAnchor, false);
		enter_animated = a.getResourceId(com.android.internal.R.styleable.PopupWindow_popupEnterTransition, 0) != 0;
		exit_animated = a.getResourceId(com.android.internal.R.styleable.PopupWindow_popupExitTransition, 0) != 0;
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

	public PopupWindow(Context context) {
		this(context, null, 0, 0);
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
		this(contentView, width, height, true);
	}

	private View contentView;
	private Drawable background;
	private long popover; // native pointer to GtkPopover
	private int anchored_gravity = DEFAULT_ANCHORED_GRAVITY;
	private boolean overlap_anchor;

	private static final int DEFAULT_ANCHORED_GRAVITY = Gravity.TOP | Gravity.START;

	public interface OnDismissListener {
		@Export
		public void onDismiss();
	}

	public void setBackgroundDrawable(Drawable background) {
		this.background = background;
		/* FIXME: use a decorview? */
		if (contentView != null) {
			contentView.setBackgroundDrawable(background);
		}
	}

	public void setInputMethodMode(int mode) {
		input_method_mode = mode;
	}

	public int getInputMethodMode() {
		return input_method_mode;
	}

	public boolean isShowing() {
		return native_isShowing(popover);
	}

	public void setFocusable(boolean focusable) {}

	public Drawable getBackground() {
		return background;
	}

	public void setContentView(View view) {
		contentView = view;
		if (contentView != null) {
			contentView.setBackground(getBackground());
		}
		native_setContentView(popover, view == null ? 0 : view.widget);
	}

	public int getMaxAvailableHeight(View anchor, int yOffset) { return 500; }

	public int getMaxAvailableHeight(View anchor, int yOffset, boolean ignoreKeyboard) { return 500; }

	public void setOutsideTouchable(boolean touchable) {
		/* FIXME: the semantics are different, this seems to specifically exist for cases
		 * where the popup is *not* modal, so that in addition to the window behind getting
		 * the real event, the popup gets a special MotionEvent.ACTION_OUTSIDE event */
		native_setTouchModal(popover, touchable);
	}

	public void setTouchInterceptor(View.OnTouchListener listener) {}

	public void showAsDropDown(View anchor, int xoff, int yoff, int gravity) {
		anchored_gravity = gravity;
		native_showAsDropDown(popover, anchor.widget, anchor.getWidth(), anchor.getHeight(), xoff, yoff,
		                      Gravity.getAbsoluteGravity(gravity, anchor.getLayoutDirection()), overlap_anchor);
		native_fadeIn(popover, fadeDuration(true));
	}

	public View getContentView() {
		return contentView;
	}

	public void setTouchable(boolean touchable) {
		native_setTouchable(popover, touchable);
	}

	public void showAsDropDown(View anchor, int xoff, int yoff) {
		if (!anchor.isAttachedToWindow()) {
			Log.e("PopupWindow", "anchor is not attached to window");
			return;
		}
		showAsDropDown(anchor, xoff, yoff, DEFAULT_ANCHORED_GRAVITY);
	}

	public void showAtLocation(View parent, int gravity, int x, int y) {
		if (gravity == Gravity.NO_GRAVITY)
			gravity = Gravity.TOP | Gravity.START;
		native_showAtLocation(popover, parent.widget, Gravity.getAbsoluteGravity(gravity, parent.getLayoutDirection()), x, y);
		native_fadeIn(popover, fadeDuration(true));
	}

	public void dismiss() {
		native_dismiss(popover, fadeDuration(false));
	}

	/* 0 turns the animations off, as on AOSP; other styles are not played */
	public void setAnimationStyle(int animationStyle) {
		animation_style = animationStyle;
	}

	public void setTouchModal(boolean touchModal) {
		native_setTouchModal(popover, touchModal);
	}

	public void setElevation(float elevation) {}

	public void update(View anchor, int xoff, int yoff, int width, int height) {
		native_update(popover, anchor.widget, anchor.getWidth(), anchor.getHeight(), xoff, yoff, width, height,
		              Gravity.getAbsoluteGravity(anchored_gravity, anchor.getLayoutDirection()), overlap_anchor);
	}

	public void setWindowLayoutType(int type) {}

	public void setIsClippedToScreen(boolean isClippedToScreen) {}

	public void setEpicenterBounds(Rect bounds) {}

	/* transitions are stored but not played; a null one turns the fade off (see fadeDuration) */
	public void setEnterTransition(Transition enterTransition) {
		enter_transition = enterTransition;
		enter_animated = enterTransition != null;
	}

	public Transition getEnterTransition() {
		return enter_transition;
	}

	public void setExitTransition(Transition exitTransition) {
		exit_transition = exitTransition;
		exit_animated = exitTransition != null;
	}

	public Transition getExitTransition() {
		return exit_transition;
	}

	public void setClippingEnabled(boolean enabled) {}

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

	public void update(int x, int y, int width, int height) {}

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

	public void setSoftInputMode(int mode) {}

	protected native long native_constructor();
	protected native void native_setColors(long widget, int background, int foreground);
	protected native void native_setContentView(long widget, long contentView);
	protected native void native_showAsDropDown(long widget, long anchor, int anchorWidth, int anchorHeight, int xoff, int yoff, int gravity, boolean overlapAnchor);
	protected native void native_showAtLocation(long widget, long parent, int gravity, int x, int y);
	protected native boolean native_isShowing(long widget);
	protected native void native_setTouchable(long widget, boolean touchable);
	protected native void native_setTouchModal(long widget, boolean touchable);
	protected native void native_dismiss(long widget, int fadeMs);
	protected native void native_fadeIn(long widget, int fadeMs);
	protected native void native_update(long widget, long anchor, int anchorWidth, int anchorHeight, int xoff, int yoff, int width, int height, int gravity, boolean overlapAnchor);
	public native void setOnDismissListener(OnDismissListener listener);
	public native void native_setWidth(long widget, int width);
	public native void native_setHeight(long widget, int height);
	public native int native_getWidth(long widget);
	public native int native_getHeight(long widget);
	public native boolean native_isTouchable(long widget);
}
