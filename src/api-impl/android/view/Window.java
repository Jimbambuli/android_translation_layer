package android.view;

import android.R;
import android.atl.annotation.Export;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.transition.Transition;
import android.util.SparseArray;
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

			/* like AOSP's DecorView: a window that wraps its content is at least as wide as
			 * windowMinWidthMajor/Minor of its theme say */
			@Override
			protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
				super.onMeasure(widthMeasureSpec, heightMeasureSpec);
				if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.AT_MOST && getMeasuredWidth() < minWidth)
					super.onMeasure(MeasureSpec.makeMeasureSpec(minWidth, MeasureSpec.EXACTLY), heightMeasureSpec);
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

	private int minWidth;

	/** @hide the minimum width of a window measured AT_MOST, from windowMinWidthMajor/Minor */
	public void setMinWidth(int minWidth) {
		this.minWidth = minWidth;
	}

	private static final String VIEWS_TAG = "android:views";

	/* the state of the window's views (see View.saveHierarchyState()), as AOSP's PhoneWindow keeps it */
	public Bundle saveHierarchyState() {
		Bundle outState = new Bundle();
		SparseArray<Parcelable> states = new SparseArray<>();
		decorView.saveHierarchyState(states);
		outState.putSparseParcelableArray(VIEWS_TAG, states);
		return outState;
	}

	public void restoreHierarchyState(Bundle savedInstanceState) {
		SparseArray<Parcelable> states = savedInstanceState.getSparseParcelableArray(VIEWS_TAG);
		if (states != null)
			decorView.restoreHierarchyState(states);
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
	@Export
	public void dispatchWindowFocusChanged(boolean hasFocus) {
		decorView.dispatchWindowFocusChanged(hasFocus);
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

	public void setContentView(View view) {
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
		return decorView;
	}

	/* like PhoneWindow: the decor view gets the key first; AppCompat's KeyEventDispatcher calls this
	 * before it hands the key to the Window.Callback (a dialog's Back, for instance) */
	public boolean superDispatchKeyEvent(KeyEvent event) {
		return decorView.dispatchKeyEvent(event);
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
		return null;
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

	public void setFormat(int format) {}

	public void setLayout(int width, int height) {
		params.width = width;
		params.height = height;
		set_layout(native_window, width, height);
		dispatchWindowAttributesChanged(params);
	}

	public WindowManager getWindowManager() {
		return new WindowManagerImpl();
	}

	public void setSoftInputMode(int dummy) {}

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

	public void setGravity(int gravity) {}

	public void setDecorFitsSystemWindows(boolean fits) {}

	public WindowInsetsController getInsetsController() {
		if (insetsController == null)
			insetsController = new InsetsController(this);
		return insetsController;
	}

	/** @hide */
	public WindowInsets getWindowInsets() {
		return new WindowInsets(visibleInsetTypes);
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
