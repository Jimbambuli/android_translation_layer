package android.view;

import android.R;
import android.atl.annotation.Export;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Insets;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.transition.Transition;
import android.util.DisplayMetrics;
import android.util.SparseArray;
import android.util.TypedValue;
import android.widget.FrameLayout;
import android.widget.Toolbar;
import android.window.OnBackInvokedDispatcher;
import android.window.WindowOnBackInvokedDispatcher;

public class Window {
	public static final int FEATURE_OPTIONS_PANEL = 0;
	public static final int FEATURE_NO_TITLE = 1;

	public ViewTreeObserver view_tree_observer = null;
	private WindowManager.LayoutParams params = new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, 0, 0, 0);

	public static interface Callback {
		public void onContentChanged();

		public abstract boolean onCreatePanelMenu(int featureId, Menu menu);

		public View onCreatePanelView(int featureId);

		public boolean onPreparePanel(int featureId, View view, Menu menu);

		public boolean onMenuItemSelected(int featureId, MenuItem item);

		public void onPanelClosed(int featureId, Menu menu);

		public boolean onMenuOpened(int featureId, Menu menu);

		public boolean dispatchKeyEvent(KeyEvent event);

		public boolean dispatchKeyShortcutEvent(KeyEvent event);

		public boolean dispatchTouchEvent(MotionEvent event);

		public boolean dispatchTrackballEvent(MotionEvent event);

		public boolean dispatchGenericMotionEvent(MotionEvent event);

		public void onWindowAttributesChanged(WindowManager.LayoutParams attrs);

		public void onWindowFocusChanged(boolean hasFocus);

		public void onAttachedToWindow();

		public void onDetachedFromWindow();

		public boolean onSearchRequested();

		public ActionMode onWindowStartingActionMode(ActionMode.Callback callback);

		public ActionMode onWindowStartingActionMode(ActionMode.Callback callback, int type);

		public void onActionModeStarted(ActionMode mode);

		public void onActionModeFinished(ActionMode mode);

		default public void onProvideKeyboardShortcuts(java.util.List<?> data, Menu menu, int deviceId) {}

		default public void onPointerCaptureChanged(boolean hasCapture) {}
	}

	public static interface OnFrameMetricsAvailableListener {}

	public long native_window;
	private ViewGroup decorView;

	private Window.Callback callback;
	private Context context;
	private final WindowOnBackInvokedDispatcher onBackInvokedDispatcher = new WindowOnBackInvokedDispatcher();
	private InsetsController insetsController;
	private int visibleInsetTypes = WindowInsets.Type.defaultVisible();
	private boolean applyInsetsPending;

	public Window(Context context, Window.Callback callback) {
		this.callback = callback;
		this.context = context;
		decorView = new FrameLayout(context) {
			@Override
			public OnBackInvokedDispatcher findOnBackInvokedDispatcher() {
				return onBackInvokedDispatcher;
			}

			/* like AOSP's DecorView: the window's Window.Callback hears when the window gains or loses
			 * the focus and when it is attached or detached */
			@Override
			public void onWindowFocusChanged(boolean hasWindowFocus) {
				super.onWindowFocusChanged(hasWindowFocus);
				if (Window.this.callback != null)
					Window.this.callback.onWindowFocusChanged(hasWindowFocus);
			}

			@Override
			protected void onAttachedToWindow() {
				super.onAttachedToWindow();
				if (Window.this.callback != null)
					Window.this.callback.onAttachedToWindow();
			}

			@Override
			protected void onDetachedFromWindow() {
				super.onDetachedFromWindow();
				if (Window.this.callback != null)
					Window.this.callback.onDetachedFromWindow();
			}

			/* AOSP's DecorView.onMeasure(), the minimum width of a window that wraps its content
			 * (windowMinWidthMajor/Minor); ATL has no fixed sizes (windowFixedWidth*) and no floating
			 * insets. ATL does not set Configuration.orientation and screenWidthDp keeps the start
			 * size, so the orientation and the available width come from the app's window, the
			 * frame ViewRootImpl measures a dialog in. */
			@Override
			protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
				final Resources res = getContext().getResources();
				final DisplayMetrics metrics = res.getDisplayMetrics();
				final Rect frame = new Rect();
				getWindowVisibleDisplayFrame(frame);
				final boolean isPortrait = frame.width() <= frame.height();

				final int widthMode = MeasureSpec.getMode(widthMeasureSpec);

				boolean fixedWidth = false;

				super.onMeasure(widthMeasureSpec, heightMeasureSpec);

				int width = getMeasuredWidth();
				boolean measure = false;

				widthMeasureSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);

				if (!fixedWidth && widthMode == MeasureSpec.AT_MOST) {
					final TypedValue tv = isPortrait ? mMinWidthMinor : mMinWidthMajor;
					final float availableWidth = frame.width();
					if (tv.type != TypedValue.TYPE_NULL) {
						final int min;
						if (tv.type == TypedValue.TYPE_DIMENSION) {
							min = (int) tv.getDimension(metrics);
						} else if (tv.type == TypedValue.TYPE_FRACTION) {
							min = (int) tv.getFraction(availableWidth, availableWidth);
						} else {
							min = 0;
						}

						if (width < min) {
							widthMeasureSpec = MeasureSpec.makeMeasureSpec(min, MeasureSpec.EXACTLY);
							measure = true;
						}
					}
				}

				// TODO: Support height?

				if (measure) {
					super.onMeasure(widthMeasureSpec, heightMeasureSpec);
				}
			}

			private final Runnable configurationFollowsSize = new Runnable() {
				@Override
				public void run() {
					((android.app.Activity)callback).internalWindowSizeChanged();
				}
			};

			/* the window was resized (not the first layout): the configuration follows, like on Android.
			 * GTK reports every step of an interactive resize, so it follows once the size has settled
			 * for a moment: an activity that is relaunched for the new size is relaunched once per
			 * resize, not at every size bucket the drag passes (apps that leak their activity, like
			 * JW Library, would leak one each time) */
			@Override
			protected void onSizeChanged(int w, int h, int oldw, int oldh) {
				super.onSizeChanged(w, h, oldw, oldh);
				if ((oldw != 0 || oldh != 0) && callback instanceof android.app.Activity) {
					removeCallbacks(configurationFollowsSize);
					postDelayed(configurationFollowsSize, 400);
				}
			}
		};
		// DecorView grabs all touch events and propagates them in Java
		decorView.native_registerTouchEventPropagation(decorView.widget);
		decorView.setId(android.R.id.content);
	}

	/** @hide like View.AttachInfo.mKeyDispatchState, one for the views of a window */
	public final KeyEvent.DispatcherState mKeyDispatchState = new KeyEvent.DispatcherState();

	// like PhoneWindow, read in installDecor() (PhoneWindow.generateLayout())
	private final TypedValue mMinWidthMajor = new TypedValue();
	private final TypedValue mMinWidthMinor = new TypedValue();

	private static final String VIEWS_TAG = "android:views";

	/* AOSP PhoneWindow.saveHierarchyState()/restoreHierarchyState() with the decor view as the content
	 * parent; ATL: no panels, no action bar views, and the focused view is not saved (findFocus() is a stub) */
	public Bundle saveHierarchyState() {
		Bundle outState = new Bundle();
		if (decorView == null) {
			return outState;
		}

		SparseArray<Parcelable> states = new SparseArray<Parcelable>();
		decorView.saveHierarchyState(states);
		outState.putSparseParcelableArray(VIEWS_TAG, states);

		return outState;
	}

	public void restoreHierarchyState(Bundle savedInstanceState) {
		if (decorView == null) {
			return;
		}

		SparseArray<Parcelable> savedStates
				= savedInstanceState.getSparseParcelableArray(VIEWS_TAG);
		if (savedStates != null) {
			decorView.restoreHierarchyState(savedStates);
		}
	}

	/* called when the activity is destroyed: like the observer of a window removed on AOSP, the
	 * observer dies, which disconnects it from the frame clock of the GtkWindow (that outlives the
	 * activity) and lets its listeners, and the views they hold, go */
	public void killViewTreeObserver() {
		if (view_tree_observer != null) {
			view_tree_observer.kill();
			view_tree_observer = null;
		}
	}

	public void set_native_window(long native_window) {
		this.native_window = native_window;
		set_jobject(native_window, this);
	}

	public void addFlags(int flags) {
		setFlags(flags, flags);
	}

	public void setFlags(int flags, int mask) {
		forcedWindowFlags |= mask;
		params.flags = (params.flags & ~mask) | (flags & mask);
		dispatchWindowAttributesChanged(params);
	}

	/* like AOSP: the Window.Callback hears about every change of the window attributes */
	protected void dispatchWindowAttributesChanged(WindowManager.LayoutParams attrs) {
		if (callback != null)
			callback.onWindowAttributesChanged(attrs);
	}

	/** @hide called from native code when the window gains or loses the focus: like ViewRootImpl,
	 * the views of the window get it, the decor view hands it to the Window.Callback */
	/*
	 * Tab and Shift+Tab that no focused widget used, as ViewRootImpl.ViewPostImeInputStage.processKeyEvent()
	 * handles KEYCODE_TAB: the focus search of the focused view, or of the root if nothing is focused,
	 * with FocusFinder, and the found view takes the focus. root is the decor view of the window.
	 */
	@Export
	static boolean moveFocusOnTab(View root, boolean forward) {
		int direction = forward ? View.FOCUS_FORWARD : View.FOCUS_BACKWARD;
		View focused = root.findFocus();
		if (focused != null) {
			View v = focused.focusSearch(direction);
			if (v != null && v != focused) {
				Rect rect = new Rect();
				focused.getFocusedRect(rect);
				return v.requestFocus(direction, rect);
			}
			return false;
		}
		// no view has the focus: the best view to start with
		View v = root instanceof ViewGroup ? ((ViewGroup)root).focusSearch(null, direction) : null;
		return v != null && v.requestFocus(direction);
	}

	@Export
	public void dispatchWindowFocusChanged(boolean hasFocus) {
		decorView.dispatchRootWindowFocusChanged(hasFocus);
	}

	public void clearFlags(int flags) {
		setFlags(0, flags);
	}

	private int forcedWindowFlags;
	private boolean haveDimAmount;

	/** @hide Like PhoneWindow.generateLayout(): the window attributes that come from the theme,
	 * unless the app has set them (dim behind a floating window, the window animations). */
	public void applyThemeAttributes() {
		TypedArray a = context.obtainStyledAttributes(com.android.internal.R.styleable.Window);
		boolean floating = a.getBoolean(com.android.internal.R.styleable.Window_windowIsFloating, false);
		if (a.getBoolean(com.android.internal.R.styleable.Window_backgroundDimEnabled, floating)) {
			if ((forcedWindowFlags & WindowManager.LayoutParams.FLAG_DIM_BEHIND) == 0)
				params.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND;
			if (!haveDimAmount)
				params.dimAmount = a.getFloat(com.android.internal.R.styleable.Window_backgroundDimAmount, 0.5f);
		}
		if (params.windowAnimations == 0)
			params.windowAnimations = a.getResourceId(com.android.internal.R.styleable.Window_windowAnimationStyle, 0);
		a.recycle();
	}

	public final Callback getCallback() {
		return this.callback;
	}
	public void setCallback(Window.Callback callback) {
		this.callback = callback;
	}

	private boolean decorInstalled;
	private boolean hasSoftInputMode;

	/* like PhoneWindow.installDecor()/generateLayout(), once, when the decor view is first asked
	 * for: the window flags from the theme, each set with setFlags(), so the Window.Callback hears
	 * of every one; the attributes ATL implements are applied elsewhere (applyThemeAttributes()) */
	private void installDecor() {
		if (decorInstalled)
			return;
		decorInstalled = true;
		TypedArray a = context.obtainStyledAttributes(com.android.internal.R.styleable.Window);
		final boolean floating = a.getBoolean(com.android.internal.R.styleable.Window_windowIsFloating, false);
		final int flagsToUpdate = (WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR) & ~forcedWindowFlags;
		if (floating)
			setFlags(0, flagsToUpdate); // AOSP sets the size to WRAP_CONTENT as well; ATL's dialogs size themselves
		else
			setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR, flagsToUpdate);
		setThemeFlag(a, com.android.internal.R.styleable.Window_windowFullscreen, false, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setThemeFlag(a, com.android.internal.R.styleable.Window_windowTranslucentStatus, false, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
		setThemeFlag(a, com.android.internal.R.styleable.Window_windowTranslucentNavigation, false, WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
		setThemeFlag(a, com.android.internal.R.styleable.Window_windowShowWallpaper, false, WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER);
		setThemeFlag(a, com.android.internal.R.styleable.Window_windowEnableSplitTouch, context.getApplicationInfo().targetSdkVersion >= 11 /* HONEYCOMB */, WindowManager.LayoutParams.FLAG_SPLIT_TOUCH);
		if (!floating)
			setThemeFlag(a, com.android.internal.R.styleable.Window_windowDrawsSystemBarBackgrounds, false, WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
		if (a.getBoolean(com.android.internal.R.styleable.Window_windowCloseOnTouchOutside, false))
			setCloseOnTouchOutsideIfNotSet(true);
		if (!hasSoftInputMode)
			params.softInputMode = a.getInt(com.android.internal.R.styleable.Window_windowSoftInputMode, params.softInputMode);
		a.getValue(com.android.internal.R.styleable.Window_windowMinWidthMajor, mMinWidthMajor);
		a.getValue(com.android.internal.R.styleable.Window_windowMinWidthMinor, mMinWidthMinor);
		a.recycle();
	}

	private void setThemeFlag(TypedArray a, int attr, boolean defValue, int flag) {
		if (a.getBoolean(attr, defValue))
			setFlags(flag, flag & ~forcedWindowFlags);
	}

	public void setContentView(View view) {
		installDecor();
		if (decorView.getBackground() == null) {
			TypedArray ta = context.obtainStyledAttributes(new int[] {R.attr.windowBackground});
			if (ta.hasValue(0))
				setBackgroundDrawable(ta.getDrawable(0));
			ta.recycle();
		}
		decorView.removeAllViews();
		decorView.addView(view);
	}

	public OnBackInvokedDispatcher getOnBackInvokedDispatcher() {
		return onBackInvokedDispatcher;
	}

	public View getDecorView() {
		installDecor();
		return decorView;
	}

	/* like PhoneWindow: the decor view gets the key first; AppCompat's KeyEventDispatcher calls this
	 * before it hands the key to the Window.Callback (a dialog's Back, for instance) */
	public boolean superDispatchKeyEvent(KeyEvent event) {
		return decorView.dispatchKeyEvent(event);
	}

	private boolean mCloseOnTouchOutside = false;
	private boolean mSetCloseOnTouchOutside = false;

	/* AOSP's Window.setCloseOnTouchOutside() and setCloseOnTouchOutsideIfNotSet() */
	public void setCloseOnTouchOutside(boolean close) {
		mCloseOnTouchOutside = close;
		mSetCloseOnTouchOutside = true;
	}

	public void setCloseOnTouchOutsideIfNotSet(boolean close) {
		if (!mSetCloseOnTouchOutside) {
			mCloseOnTouchOutside = close;
			mSetCloseOnTouchOutside = true;
		}
	}

	/* AOSP's Window.shouldCloseOnTouch() and isOutOfBounds() (android-14) */
	public boolean shouldCloseOnTouch(Context context, MotionEvent event) {
		final boolean isOutside =
		    event.getAction() == MotionEvent.ACTION_UP && isOutOfBounds(context, event)
		    || event.getAction() == MotionEvent.ACTION_OUTSIDE;
		if (mCloseOnTouchOutside && peekDecorView() != null && isOutside) {
			return true;
		}
		return false;
	}

	private boolean isOutOfBounds(Context context, MotionEvent event) {
		final int x = (int)event.getX();
		final int y = (int)event.getY();
		final int slop = ViewConfiguration.get(context).getScaledWindowTouchSlop();
		final View decorView = getDecorView();
		return (x < -slop) || (y < -slop)
		    || (x > (decorView.getWidth() + slop))
		    || (y > (decorView.getHeight() + slop));
	}

	public void takeInputQueue(InputQueue.Callback callback) {
		take_input_queue(native_window, callback, new InputQueue());
	}

	public boolean requestFeature(int featureId) {
		return false;
	}

	public View findViewById(int id) {
		if (id == com.android.internal.R.id.action_bar)
			return new Toolbar(context);
		return decorView.findViewById(id);
	}

	public View peekDecorView() {
		return decorView;
	}

	public WindowManager.LayoutParams getAttributes() {
		return params;
	}

	public void setBackgroundDrawable(Drawable drawable) {
		// HACK: disable transparent background for WhatsApp dialogs
		// For some unknown reason, the language picker in WhatsApp doesn't render the BottomSheet background currently.
		if (!"com.whatsapp".equals(context.getPackageName()))
			remove_gtk_background(native_window);
		decorView.setBackgroundDrawable(drawable);
	}

	public void setAttributes(WindowManager.LayoutParams params) {
		if (params.screenBrightness != -1)
			set_screen_brightness(params.screenBrightness);
		this.params = params;
		set_layout(native_window, params.width, params.height);
		dispatchWindowAttributesChanged(params);
	}

	public void takeSurface(SurfaceHolder.Callback2 callback) {}

	public void setStatusBarColor(int color) {}

	public void setNavigationBarColor(int color) {}

	public void setFormat(int format) {
		params.format = format;
		dispatchWindowAttributesChanged(params);
	}

	public void setType(int type) {
		params.type = type;
		dispatchWindowAttributesChanged(params);
	}

	public void setColorMode(int colorMode) {
		params.setColorMode(colorMode);
		dispatchWindowAttributesChanged(params);
	}

	public void setPreferMinimalPostProcessing(boolean isPreferred) {
		params.preferMinimalPostProcessing = isPreferred;
		dispatchWindowAttributesChanged(params);
	}

	public void setLayout(int width, int height) {
		params.width = width;
		params.height = height;
		set_layout(native_window, width, height);
		dispatchWindowAttributesChanged(params);
	}

	public WindowManager getWindowManager() {
		return new WindowManagerImpl();
	}

	public void setSoftInputMode(int mode) {
		if (mode != WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED) {
			params.softInputMode = mode;
			hasSoftInputMode = true;
		} else {
			hasSoftInputMode = false;
		}
		dispatchWindowAttributesChanged(params);
	}

	public int getNavigationBarColor() {
		return 0xFF888888; // gray
	}

	public void setBackgroundDrawableResource(int resId) {
		setBackgroundDrawable(context.getDrawable(resId));
	}

	public int getStatusBarColor() { return 0xFFFF0000; }

	public Context getContext() {
		return context;
	}

	public boolean hasFeature(int featureId) {
		return false;
	}

	public void setTitle(CharSequence title) {
		set_title(native_window, title != null ? title.toString() : context.getPackageName());
	}

	public Transition getSharedElementEnterTransition() {
		return new Transition();
	}

	public void setSharedElementExitTransition(Transition transition) {}

	public void setSharedElementReenterTransition(Transition transition) {}

	public void setSharedElementReturnTransition(Transition transition) {}

	public Transition getSharedElementExitTransition() {
		return new Transition();
	}

	public Transition getSharedElementReenterTransition() {
		return new Transition();
	}

	public void setReturnTransition(Transition transition) {}

	public void setEnterTransition(Transition transition) {}

	public void setGravity(int gravity) {
		params.gravity = gravity;
		dispatchWindowAttributesChanged(params);
	}

	public void setDecorFitsSystemWindows(boolean fits) {}

	public WindowInsetsController getInsetsController() {
		if (insetsController == null)
			insetsController = new InsetsController(this);
		return insetsController;
	}

	/** @hide */
	public WindowInsets getWindowInsets() {
		/* like InsetsState.calculateInsets() for a window without any insets source: a desktop window has no
		 * system bars, cutout or rounded corners, so every inset is empty; only the visibility of the bars,
		 * set by WindowInsetsController, is kept */
		boolean[] typeVisibilityMap = new boolean[WindowInsets.Type.SIZE];
		for (int type = WindowInsets.Type.FIRST; type <= WindowInsets.Type.LAST; type <<= 1)
			typeVisibilityMap[WindowInsets.Type.indexOf(type)] = (visibleInsetTypes & type) != 0;
		return new WindowInsets(new Insets[WindowInsets.Type.SIZE], new Insets[WindowInsets.Type.SIZE], typeVisibilityMap,
		                        false, false, 0, DisplayCutout.NO_CUTOUT, RoundedCorners.NO_ROUNDED_CORNERS,
		                        new PrivacyIndicatorBounds(), DisplayShape.NONE,
		                        WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout(), false);
	}

	/** @hide called by the InsetsController */
	void setInsetsVisible(int types, boolean visible) {
		int visibleTypes = visible ? (visibleInsetTypes | types) : (visibleInsetTypes & ~types);
		if (visibleTypes != visibleInsetTypes) {
			visibleInsetTypes = visibleTypes;
			requestApplyInsets();
		}
	}

	/** @hide dispatch the insets to the whole view hierarchy, once per main loop iteration */
	public void requestApplyInsets() {
		if (applyInsetsPending)
			return;
		applyInsetsPending = true;
		new Handler(Looper.getMainLooper()).post(new Runnable() {
			@Override
			public void run() {
				applyInsetsPending = false;
				decorView.dispatchApplyWindowInsets(getWindowInsets());
			}
		});
	}

	public void setStatusBarContrastEnforced(boolean enforced) {}

	public void setNavigationBarContrastEnforced(boolean enforced) {}

	public void setSharedElementEnterTransition(Transition transition) {}

	public Transition getEnterTransition() {
		return null;
	}

	public void setWindowAnimations(int resId) {
		params.windowAnimations = resId;
		dispatchWindowAttributesChanged(params);
	}

	public void setDimAmount(float dimAmount) {
		params.dimAmount = dimAmount;
		haveDimAmount = true;
		dispatchWindowAttributesChanged(params);
	}

	private native void set_title(long native_window, String title);
	public native void take_input_queue(long native_window, InputQueue.Callback callback, InputQueue queue);
	public native void set_layout(long native_window, int width, int height);
	private static native void set_jobject(long ptr, Window obj);
	private native void remove_gtk_background(long native_window);
	private native void set_screen_brightness(float brightness);
}
