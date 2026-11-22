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
	public static final int FEATURE_PROGRESS = 2;
	public static final int FEATURE_LEFT_ICON = 3;
	public static final int FEATURE_RIGHT_ICON = 4;
	public static final int FEATURE_INDETERMINATE_PROGRESS = 5;
	public static final int FEATURE_CONTEXT_MENU = 6;
	public static final int FEATURE_CUSTOM_TITLE = 7;
	public static final int FEATURE_ACTION_BAR = 8;
	public static final int FEATURE_ACTION_BAR_OVERLAY = 9;
	public static final int FEATURE_ACTION_MODE_OVERLAY = 10;
	public static final int FEATURE_SWIPE_TO_DISMISS = 11;
	public static final int FEATURE_CONTENT_TRANSITIONS = 12;
	public static final int FEATURE_ACTIVITY_TRANSITIONS = 13;
	public static final int FEATURE_MAX = FEATURE_ACTIVITY_TRANSITIONS;
	public static final int DEFAULT_FEATURES = (1 << FEATURE_OPTIONS_PANEL) | (1 << FEATURE_CONTEXT_MENU);

	/* AOSP Window.mFeatures: the features the window has, from its start the default ones; the theme adds its own in
	 * installDecor() and requestFeature() the ones the app asks for before it sets the content */
	private int mFeatures = DEFAULT_FEATURES;
	private boolean mContentParentExplicitlySet = false;

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

		/** AOSP Window.Callback.onSearchRequested(SearchEvent): the same, with the event that asked */
		public boolean onSearchRequested(SearchEvent searchEvent);

		/** AOSP Window.Callback.dispatchPopulateAccessibilityEvent(): lets the callback add the window's description */
		public boolean dispatchPopulateAccessibilityEvent(android.view.accessibility.AccessibilityEvent event);

		public ActionMode onWindowStartingActionMode(ActionMode.Callback callback);

		public ActionMode onWindowStartingActionMode(ActionMode.Callback callback, int type);

		public void onActionModeStarted(ActionMode mode);

		public void onActionModeFinished(ActionMode mode);

		default public void onProvideKeyboardShortcuts(java.util.List<KeyboardShortcutGroup> data, Menu menu, int deviceId) {}

		default public void onPointerCaptureChanged(boolean hasCapture) {}
	}

	/* AOSP Window.OnFrameMetricsAvailableListener */
	public static interface OnFrameMetricsAvailableListener {
		void onFrameMetricsAvailable(Window window, FrameMetrics frameMetrics, int dropCountSinceLastInvocation);
	}

	public long native_window;
	private ViewGroup decorView;

	private Window.Callback callback;
	private Context context;
	private final WindowOnBackInvokedDispatcher onBackInvokedDispatcher = new WindowOnBackInvokedDispatcher();
	private InsetsController insetsController;
	private int visibleInsetTypes = WindowInsets.Type.defaultVisible();
	private boolean applyInsetsPending;

	/* AOSP Window(Context): a window without a Window.Callback, which an Activity or Dialog sets with setCallback() */
	public Window(Context context) {
		this(context, null);
	}

	public Window(Context context, Window.Callback callback) {
		this.callback = callback;
		this.context = context;
		decorView = new FrameLayout(context) {
			@Override
			public OnBackInvokedDispatcher findOnBackInvokedDispatcher() {
				return onBackInvokedDispatcher;
			}

			/* like AOSP's DecorView: until the window is shown, the controller of the window
			 * (AOSP's PendingInsetsController) is handed out */
			@Override
			public WindowInsetsController getWindowInsetsController() {
				if (isAttachedToWindow())
					return super.getWindowInsetsController();
				return Window.this.getInsetsController();
			}

			/* like AOSP's DecorView: the window's Window.Callback hears when the window gains or loses
			 * the focus and when it is attached or detached */
			@Override
			public void onWindowFocusChanged(boolean hasWindowFocus) {
				super.onWindowFocusChanged(hasWindowFocus);
				if (Window.this.callback != null)
					Window.this.callback.onWindowFocusChanged(hasWindowFocus);
			}

			/* ViewRootImpl dispatches the window insets on its first traversal (dispatchApplyInsets()) */
			@Override
			protected void onAttachedToWindow() {
				super.onAttachedToWindow();
				if (Window.this.callback != null)
					Window.this.callback.onAttachedToWindow();
				Window.this.requestApplyInsets();
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
					if (updateImeOverlap())
						return;
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

			/* AOSP DecorView: the action modes the views start end here (startActionModeForChild()) */
			@Override
			public ActionMode startActionModeForChild(View originalView, ActionMode.Callback callback) {
				return startActionModeForChild(originalView, callback, ActionMode.TYPE_PRIMARY);
			}

			@Override
			public ActionMode startActionModeForChild(View child, ActionMode.Callback callback, int type) {
				return startDecorActionMode(child, callback, type);
			}

			@Override
			public ActionMode startActionMode(ActionMode.Callback callback) {
				return startActionMode(callback, ActionMode.TYPE_PRIMARY);
			}

			@Override
			public ActionMode startActionMode(ActionMode.Callback callback, int type) {
				return startDecorActionMode(this, callback, type);
			}
		};
		// DecorView grabs all touch events and propagates them in Java
		decorView.native_registerTouchEventPropagation(decorView.widget);
		decorView.setId(android.R.id.content);
		mContentParent = decorView;
		// files dragged from the desktop come as drag events (handleDragEvent())
		native_set_drop_target(decorView.widget);
	}

	/* AOSP ViewRootImpl.handleDragEvent() for files dragged from the desktop onto the window (a GtkDropTarget on
	 * the decor view, android_view_Window.c), which plays the window manager: ACTION_DRAG_STARTED with the
	 * files' types, ACTION_DRAG_LOCATION, ACTION_DROP with their content URIs (ContentResolver.atlUriForFile(),
	 * like a drag from AOSP's Files app), ACTION_DRAG_EXITED and ACTION_DRAG_ENDED after the drop or when the
	 * drag leaves the window (GTK does not tell where a drag outside ends). Returns what the views returned. */
	private android.content.ClipDescription dragDescription;

	private boolean handleDragEvent(int action, float x, float y, String[] paths, boolean dropResult) {
		android.content.ClipData data = null;
		if (paths != null) {
			android.content.ContentResolver resolver = getContext().getContentResolver();
			java.util.LinkedHashSet<String> types = new java.util.LinkedHashSet<>();
			android.content.ClipData.Item[] items = new android.content.ClipData.Item[paths.length];
			for (int i = 0; i < paths.length; i++) {
				android.net.Uri uri = android.content.ContentResolver.atlUriForFile(paths[i]);
				String type = resolver.getType(uri);
				types.add(type != null ? type : android.content.ClipDescription.MIMETYPE_UNKNOWN);
				items[i] = new android.content.ClipData.Item(uri);
			}
			if (items.length == 0)
				return false;
			data = new android.content.ClipData(new android.content.ClipDescription(null, types.toArray(new String[0])), items[0]);
			for (int i = 1; i < items.length; i++)
				data.addItem(items[i]);
		}
		if (action == DragEvent.ACTION_DRAG_STARTED) {
			View.sCurrentDragView = null; // Start the current-recipient tracking
			dragDescription = data != null ? data.getDescription() : null;
		}
		DragEvent event = DragEvent.obtain(action, x, y, 0, 0, null, dragDescription,
		                                   action == DragEvent.ACTION_DROP ? data : null, null,
		                                   action == DragEvent.ACTION_DROP ? Boolean.TRUE : null, dropResult);
		if (action == DragEvent.ACTION_DRAG_ENDED)
			dragDescription = null;
		boolean result = false;
		if (action == DragEvent.ACTION_DRAG_EXITED) {
			View.setDragFocus(null, event);
		} else {
			result = decorView.dispatchDragEvent(event);
			// If we've been sent a LOCATION and no view has accepted it, the drag focus is cleared
			if (action == DragEvent.ACTION_DRAG_LOCATION && !event.mEventHandlerWasCalled)
				View.setDragFocus(null, event);
		}
		event.recycle();
		return result;
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

	/* --- action modes, AOSP DecorView.startActionMode() and what it calls ---
	 * The Window.Callback may start the mode (Activity: a primary mode in its action bar, AppCompat: its own),
	 * otherwise the decor makes it: a primary mode is a StandaloneActionMode in an ActionBarContextView, a floating
	 * mode a FloatingActionMode with a FloatingToolbar. ATL's decor has no decor layout with action_mode_bar_stub
	 * (AOSP's screen_simple), so a primary mode always shows its bar in a popup at the top of the window, as AOSP
	 * does for a floating window. AOSP's Window.isDestroyed() checks are left out (ATL's window does not track it). */
	private ActionMode mPrimaryActionMode;
	private ActionMode mFloatingActionMode;
	private com.android.internal.widget.ActionBarContextView mPrimaryActionModeView;
	private android.widget.PopupWindow mPrimaryActionModePopup;
	private Runnable mShowPrimaryActionModePopup;
	private ViewTreeObserver.OnPreDrawListener mFloatingToolbarPreDrawListener;
	private View mFloatingActionModeOriginatingView;
	private com.android.internal.widget.floatingtoolbar.FloatingToolbar mFloatingToolbar;
	private android.animation.ObjectAnimator mFadeAnim;

	private ActionMode startDecorActionMode(View originatingView, ActionMode.Callback callback, int type) {
		ActionMode.Callback2 wrappedCallback = new ActionModeCallback2Wrapper(callback);
		ActionMode mode = null;
		if (getCallback() != null) {
			try {
				mode = getCallback().onWindowStartingActionMode(wrappedCallback, type);
			} catch (AbstractMethodError ame) {
				// Older apps might not implement the typed version of this method.
				if (type == ActionMode.TYPE_PRIMARY) {
					try {
						mode = getCallback().onWindowStartingActionMode(wrappedCallback);
					} catch (AbstractMethodError ame2) {
						// Older apps might not implement this callback method at all.
					}
				}
			}
		}
		if (mode != null) {
			if (mode.getType() == ActionMode.TYPE_PRIMARY) {
				cleanupPrimaryActionMode();
				mPrimaryActionMode = mode;
			} else if (mode.getType() == ActionMode.TYPE_FLOATING) {
				if (mFloatingActionMode != null) {
					mFloatingActionMode.finish();
				}
				mFloatingActionMode = mode;
			}
		} else {
			mode = createActionMode(type, wrappedCallback, originatingView);
			if (mode != null && wrappedCallback.onCreateActionMode(mode, mode.getMenu())) {
				setHandledActionMode(mode);
			} else {
				mode = null;
			}
		}
		if (mode != null && getCallback() != null) {
			try {
				getCallback().onActionModeStarted(mode);
			} catch (AbstractMethodError ame) {
				// Older apps might not implement this callback method.
			}
		}
		return mode;
	}

	private void cleanupPrimaryActionMode() {
		if (mPrimaryActionMode != null) {
			mPrimaryActionMode.finish();
			mPrimaryActionMode = null;
		}
		if (mPrimaryActionModeView != null) {
			mPrimaryActionModeView.killMode();
		}
	}

	private void cleanupFloatingActionModeViews() {
		if (mFloatingToolbar != null) {
			mFloatingToolbar.dismiss();
			mFloatingToolbar = null;
		}
		if (mFloatingActionModeOriginatingView != null) {
			if (mFloatingToolbarPreDrawListener != null) {
				mFloatingActionModeOriginatingView.getViewTreeObserver()
				    .removeOnPreDrawListener(mFloatingToolbarPreDrawListener);
				mFloatingToolbarPreDrawListener = null;
			}
			mFloatingActionModeOriginatingView = null;
		}
	}

	private ActionMode createActionMode(int type, ActionMode.Callback2 callback, View originatingView) {
		switch (type) {
			case ActionMode.TYPE_PRIMARY:
			default:
				return createStandaloneActionMode(callback);
			case ActionMode.TYPE_FLOATING:
				return createFloatingActionMode(originatingView, callback);
		}
	}

	private void setHandledActionMode(ActionMode mode) {
		if (mode.getType() == ActionMode.TYPE_PRIMARY) {
			setHandledPrimaryActionMode(mode);
		} else if (mode.getType() == ActionMode.TYPE_FLOATING) {
			setHandledFloatingActionMode(mode);
		}
	}

	/* AOSP DecorView.createStandaloneActionMode(), the branch of a floating window (see above) */
	private ActionMode createStandaloneActionMode(ActionMode.Callback callback) {
		endOnGoingFadeAnimation();
		cleanupPrimaryActionMode();
		if (mPrimaryActionModeView == null || !mPrimaryActionModeView.isAttachedToWindow()) {
			// Use the action bar theme.
			final TypedValue outValue = new TypedValue();
			final Resources.Theme baseTheme = context.getTheme();
			baseTheme.resolveAttribute(com.android.internal.R.attr.actionBarTheme, outValue, true);

			final Context actionBarContext;
			if (outValue.resourceId != 0) {
				final Resources.Theme actionBarTheme = context.getResources().newTheme();
				actionBarTheme.setTo(baseTheme);
				actionBarTheme.applyStyle(outValue.resourceId, true);

				actionBarContext = new ContextThemeWrapper(context, 0);
				actionBarContext.getTheme().setTo(actionBarTheme);
			} else {
				actionBarContext = context;
			}

			mPrimaryActionModeView = new com.android.internal.widget.ActionBarContextView(actionBarContext);
			mPrimaryActionModePopup = new android.widget.PopupWindow(actionBarContext, null,
			    com.android.internal.R.attr.actionModePopupWindowStyle);
			mPrimaryActionModePopup.setWindowLayoutType(WindowManager.LayoutParams.TYPE_APPLICATION);
			mPrimaryActionModePopup.setContentView(mPrimaryActionModeView);
			mPrimaryActionModePopup.setWidth(ViewGroup.LayoutParams.MATCH_PARENT);

			actionBarContext.getTheme().resolveAttribute(com.android.internal.R.attr.actionBarSize, outValue, true);
			final int height = TypedValue.complexToDimensionPixelSize(outValue.data,
			    actionBarContext.getResources().getDisplayMetrics());
			mPrimaryActionModeView.setContentHeight(height);
			mPrimaryActionModePopup.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
			mShowPrimaryActionModePopup = new Runnable() {
				public void run() {
					mPrimaryActionModePopup.showAtLocation(decorView, Gravity.TOP | Gravity.FILL_HORIZONTAL, 0, 0);
					endOnGoingFadeAnimation();

					if (decorView.isLaidOut()) {
						mFadeAnim = android.animation.ObjectAnimator.ofFloat(mPrimaryActionModeView, View.ALPHA, 0f, 1f);
						mFadeAnim.addListener(new android.animation.AnimatorListenerAdapter() {
							@Override
							public void onAnimationStart(android.animation.Animator animation) {
								mPrimaryActionModeView.setVisibility(View.VISIBLE);
							}

							@Override
							public void onAnimationEnd(android.animation.Animator animation) {
								mPrimaryActionModeView.setAlpha(1f);
								mFadeAnim = null;
							}
						});
						mFadeAnim.start();
					} else {
						mPrimaryActionModeView.setAlpha(1f);
						mPrimaryActionModeView.setVisibility(View.VISIBLE);
					}
				}
			};
		}
		if (mPrimaryActionModeView != null) {
			mPrimaryActionModeView.killMode();
			ActionMode mode = new com.android.internal.view.StandaloneActionMode(
			    mPrimaryActionModeView.getContext(), mPrimaryActionModeView,
			    callback, mPrimaryActionModePopup == null);
			return mode;
		}
		return null;
	}

	private void endOnGoingFadeAnimation() {
		if (mFadeAnim != null) {
			mFadeAnim.end();
		}
	}

	private void setHandledPrimaryActionMode(ActionMode mode) {
		endOnGoingFadeAnimation();
		mPrimaryActionMode = mode;
		mPrimaryActionMode.invalidate();
		mPrimaryActionModeView.initForMode(mPrimaryActionMode);
		decorView.post(mShowPrimaryActionModePopup);
	}

	private ActionMode createFloatingActionMode(View originatingView, ActionMode.Callback2 callback) {
		if (mFloatingActionMode != null) {
			mFloatingActionMode.finish();
		}
		cleanupFloatingActionModeViews();
		mFloatingToolbar = new com.android.internal.widget.floatingtoolbar.FloatingToolbar(this);
		final com.android.internal.view.FloatingActionMode mode =
		    new com.android.internal.view.FloatingActionMode(context, callback, originatingView, mFloatingToolbar);
		mFloatingActionModeOriginatingView = originatingView;
		mFloatingToolbarPreDrawListener = new ViewTreeObserver.OnPreDrawListener() {
			@Override
			public boolean onPreDraw() {
				mode.updateViewLocationInWindow();
				return true;
			}
		};
		return mode;
	}

	private void setHandledFloatingActionMode(ActionMode mode) {
		mFloatingActionMode = mode;
		mFloatingActionMode.invalidate(); // Will show the floating toolbar if necessary.
		mFloatingActionModeOriginatingView.getViewTreeObserver()
		    .addOnPreDrawListener(mFloatingToolbarPreDrawListener);
	}

	/* AOSP DecorView.ActionModeCallback2Wrapper: clears out the references when the action mode is destroyed */
	private class ActionModeCallback2Wrapper extends ActionMode.Callback2 {
		private final ActionMode.Callback mWrapped;

		public ActionModeCallback2Wrapper(ActionMode.Callback wrapped) {
			mWrapped = wrapped;
		}

		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			return mWrapped.onCreateActionMode(mode, menu);
		}

		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			decorView.requestFitSystemWindows();
			return mWrapped.onPrepareActionMode(mode, menu);
		}

		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			return mWrapped.onActionItemClicked(mode, item);
		}

		public void onDestroyActionMode(ActionMode mode) {
			mWrapped.onDestroyActionMode(mode);
			final boolean isMncApp = context.getApplicationInfo().targetSdkVersion >= android.os.Build.VERSION_CODES.M;
			final boolean isPrimary;
			final boolean isFloating;
			if (isMncApp) {
				isPrimary = mode == mPrimaryActionMode;
				isFloating = mode == mFloatingActionMode;
				if (!isPrimary && mode.getType() == ActionMode.TYPE_PRIMARY) {
					android.util.Log.e("DecorView", "Destroying unexpected ActionMode instance of TYPE_PRIMARY; "
					    + mode + " was not the current primary action mode! Expected " + mPrimaryActionMode);
				}
				if (!isFloating && mode.getType() == ActionMode.TYPE_FLOATING) {
					android.util.Log.e("DecorView", "Destroying unexpected ActionMode instance of TYPE_FLOATING; "
					    + mode + " was not the current floating action mode! Expected " + mFloatingActionMode);
				}
			} else {
				isPrimary = mode.getType() == ActionMode.TYPE_PRIMARY;
				isFloating = mode.getType() == ActionMode.TYPE_FLOATING;
			}
			if (isPrimary) {
				if (mPrimaryActionModePopup != null) {
					decorView.removeCallbacks(mShowPrimaryActionModePopup);
				}
				if (mPrimaryActionModeView != null) {
					endOnGoingFadeAnimation();
					// Store action mode view reference, so we can access it safely when animation ends.
					final com.android.internal.widget.ActionBarContextView lastActionModeView = mPrimaryActionModeView;
					mFadeAnim = android.animation.ObjectAnimator.ofFloat(mPrimaryActionModeView, View.ALPHA, 1f, 0f);
					mFadeAnim.addListener(new android.animation.AnimatorListenerAdapter() {
						@Override
						public void onAnimationEnd(android.animation.Animator animation) {
							if (lastActionModeView == mPrimaryActionModeView) {
								lastActionModeView.setVisibility(View.GONE);
								if (mPrimaryActionModePopup != null) {
									mPrimaryActionModePopup.dismiss();
								}
								lastActionModeView.killMode();
								mFadeAnim = null;
								decorView.requestApplyInsets();
							}
						}
					});
					mFadeAnim.start();
				}

				mPrimaryActionMode = null;
			} else if (isFloating) {
				cleanupFloatingActionModeViews();
				mFloatingActionMode = null;
			}
			if (getCallback() != null) {
				try {
					getCallback().onActionModeFinished(mode);
				} catch (AbstractMethodError ame) {
					// Older apps might not implement this callback method.
				}
			}
			decorView.requestFitSystemWindows();
		}

		@Override
		public void onGetContentRect(ActionMode mode, View view, Rect outRect) {
			if (mWrapped instanceof ActionMode.Callback2) {
				((ActionMode.Callback2)mWrapped).onGetContentRect(mode, view, outRect);
			} else {
				super.onGetContentRect(mode, view, outRect);
			}
		}
	}

	public final Callback getCallback() {
		return this.callback;
	}
	public void setCallback(Window.Callback callback) {
		this.callback = callback;
	}

	private boolean decorInstalled;
	private boolean hasSoftInputMode;
	private boolean isFloating;
	/* windowIsTranslucent of the theme, read when the decor is installed; native code shows what is behind
	 * such an activity's page */
	private boolean isTranslucent;

	/* PhoneWindow.isFloating(): windowIsFloating of the theme, read when the decor is installed */
	public boolean isFloating() {
		return isFloating;
	}

	/* like PhoneWindow.installDecor()/generateLayout(), once, when the decor view is first asked
	 * for: the window flags from the theme, each set with setFlags(), so the Window.Callback hears
	 * of every one; the attributes ATL implements are applied elsewhere (applyThemeAttributes()) */
	private void installDecor() {
		if (decorInstalled)
			return;
		decorInstalled = true;
		TypedArray a = context.obtainStyledAttributes(com.android.internal.R.styleable.Window);
		final boolean floating = a.getBoolean(com.android.internal.R.styleable.Window_windowIsFloating, false);
		isFloating = floating;
		isTranslucent = a.getBoolean(com.android.internal.R.styleable.Window_windowIsTranslucent, false);
		final int flagsToUpdate = (WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR) & ~forcedWindowFlags;
		if (floating)
			setFlags(0, flagsToUpdate); // AOSP sets the size to WRAP_CONTENT as well; ATL's dialogs size themselves
		else
			setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR, flagsToUpdate);
		/* the features of the theme, as PhoneWindow.generateLayout() asks for them */
		if (a.getBoolean(com.android.internal.R.styleable.Window_windowNoTitle, false))
			requestFeatureInternal(FEATURE_NO_TITLE);
		else if (a.getBoolean(com.android.internal.R.styleable.Window_windowActionBar, false))
			requestFeatureInternal(FEATURE_ACTION_BAR);
		if (a.getBoolean(com.android.internal.R.styleable.Window_windowActionBarOverlay, false))
			requestFeatureInternal(FEATURE_ACTION_BAR_OVERLAY);
		if (a.getBoolean(com.android.internal.R.styleable.Window_windowActionModeOverlay, false))
			requestFeatureInternal(FEATURE_ACTION_MODE_OVERLAY);
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
		/* AOSP PhoneWindow.generateLayout() */
		mEnsureStatusBarContrastWhenTransparent = a.getBoolean(com.android.internal.R.styleable.Window_enforceStatusBarContrast, false);
		mEnsureNavigationBarContrastWhenTransparent = a.getBoolean(com.android.internal.R.styleable.Window_enforceNavigationBarContrast, true);
		/* AOSP PhoneWindow.generateLayout(): a window that is not floating and has the action bar (and none of the
		 * title features that come first) gets the theme's decor layout (screen_toolbar, screen_action_bar), and its
		 * content goes into that layout's content view (DecorView.onResourcesLoaded()). ATL leaves out the other
		 * title layouts; the title of such a window is the desktop's (setTitle()). */
		if (!floating && (mFeatures & (1 << FEATURE_ACTION_BAR)) != 0
		    && (mFeatures & ((1 << FEATURE_LEFT_ICON) | (1 << FEATURE_RIGHT_ICON) | (1 << FEATURE_CUSTOM_TITLE))) == 0) {
			final int layoutResource = a.getResourceId(com.android.internal.R.styleable.Window_windowActionBarFullscreenDecorLayout,
			                                           com.android.internal.R.layout.screen_action_bar);
			decorView.setId(View.NO_ID);
			final View root = LayoutInflater.from(context).inflate(layoutResource, null);
			decorView.addView(root, 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
			mContentParent = decorView.findViewById(android.R.id.content);
			installDecorContentParent();
		}
		a.recycle();
	}

	/* AOSP PhoneWindow.installDecor(): the decor layout's action bar gets the window's callback, title, features
	 * and icons; the options menu is made after the activity's onCreate() (invalidatePanelMenu()) */
	private void installDecorContentParent() {
		mDecorContentParent = decorView.findViewById(com.android.internal.R.id.decor_content_parent);
		if (mDecorContentParent == null)
			return;
		mDecorContentParent.setWindowCallback(getCallback());
		if (mDecorContentParent.getTitle() == null)
			mDecorContentParent.setWindowTitle(mTitle);
		final int localFeatures = getLocalFeatures();
		for (int i = 0; i < FEATURE_MAX; i++) {
			if ((localFeatures & (1 << i)) != 0)
				mDecorContentParent.initFeature(i);
		}
		mDecorContentParent.setUiOptions(mUiOptions);
		if ((mResourcesSetFlags & FLAG_RESOURCE_SET_ICON) != 0 || (mIconRes != 0 && !mDecorContentParent.hasIcon())) {
			mDecorContentParent.setIcon(mIconRes);
		} else if ((mResourcesSetFlags & FLAG_RESOURCE_SET_ICON) == 0 && mIconRes == 0 && !mDecorContentParent.hasIcon()) {
			mDecorContentParent.setIcon(getContext().getPackageManager().getDefaultActivityIcon());
			mResourcesSetFlags |= FLAG_RESOURCE_SET_ICON_FALLBACK;
		}
		if ((mResourcesSetFlags & FLAG_RESOURCE_SET_LOGO) != 0 || (mLogoRes != 0 && !mDecorContentParent.hasLogo()))
			mDecorContentParent.setLogo(mLogoRes);
		/* deferred so that the app's onCreateOptionsMenu() does not run in the middle of its onCreate() */
		PanelFeatureState st = getPanelState(FEATURE_OPTIONS_PANEL, false);
		if (st == null || st.menu == null)
			invalidatePanelMenu(FEATURE_ACTION_BAR);
	}

	private void setThemeFlag(TypedArray a, int attr, boolean defValue, int flag) {
		if (a.getBoolean(attr, defValue))
			setFlags(flag, flag & ~forcedWindowFlags);
	}

	/* AOSP PhoneWindow.setContentView(int): inflates the layout into the content, as Activity.setContentView(int) does here */
	public void setContentView(int layoutResID) {
		setContentView(LayoutInflater.from(context).inflate(layoutResID, null, false));
	}

	/* AOSP PhoneWindow.setContentView(View, LayoutParams): the content is the view with the given layout parameters */
	public void setContentView(View view, ViewGroup.LayoutParams params) {
		view.setLayoutParams(params);
		setContentView(view);
	}

	public void setContentView(View view) {
		installDecor();
		mContentParentExplicitlySet = true;
		if (decorView.getBackground() == null) {
			TypedArray ta = context.obtainStyledAttributes(new int[] {R.attr.windowBackground});
			if (ta.hasValue(0))
				setBackgroundDrawable(ta.getDrawable(0));
			ta.recycle();
		}
		mContentParent.removeAllViews();
		mContentParent.addView(view);
	}

	/* AOSP PhoneWindow.addContentView(): the view is added to the content next to what is there, with the given
	 * layout parameters (setContentView() replaces the content) */
	public void addContentView(View view, ViewGroup.LayoutParams params) {
		installDecor();
		mContentParentExplicitlySet = true;
		mContentParent.addView(view, params);
	}

	public OnBackInvokedDispatcher getOnBackInvokedDispatcher() {
		return onBackInvokedDispatcher;
	}

	/* AOSP PhoneWindow.getRootSurfaceControl(): the ViewRootImpl of the decor view, null while it is not attached */
	public AttachedSurfaceControl getRootSurfaceControl() {
		return decorView != null ? decorView.getRootSurfaceControl() : null;
	}

	public View getDecorView() {
		installDecor();
		return decorView;
	}

	/* like PhoneWindow: the decor view gets the key first; AppCompat's KeyEventDispatcher calls this
	 * before it hands the key to the Window.Callback (a dialog's Back, for instance) */
	/* AOSP PhoneWindow.superDispatchTouchEvent(), superDispatchTrackballEvent(), superDispatchGenericMotionEvent():
	 * the decor view dispatches the event to the views */
	public boolean superDispatchTouchEvent(MotionEvent event) {
		return getDecorView().dispatchTouchEvent(event);
	}

	public boolean superDispatchTrackballEvent(MotionEvent event) {
		return getDecorView().dispatchTrackballEvent(event);
	}

	public boolean superDispatchGenericMotionEvent(MotionEvent event) {
		return getDecorView().dispatchGenericMotionEvent(event);
	}

	/* AOSP Window.getCurrentFocus(): the view of the window that has the focus */
	public View getCurrentFocus() {
		return peekDecorView() != null ? peekDecorView().findFocus() : null;
	}

	public boolean superDispatchKeyEvent(KeyEvent event) {
		return decorView.dispatchKeyEvent(event);
	}

	/* like PhoneWindow.superDispatchKeyShortcutEvent() */
	public boolean superDispatchKeyShortcutEvent(KeyEvent event) {
		return decorView.dispatchKeyShortcutEvent(event);
	}

	/* PhoneWindow's media controller (Activity.setMediaController()) */
	private android.media.session.MediaController mMediaController;

	public void setMediaController(android.media.session.MediaController controller) {
		mMediaController = controller;
	}

	public android.media.session.MediaController getMediaController() {
		return mMediaController;
	}

	/* A key that no view consumed on its way through GTK (WrapperWidget.c) reaches the window, see
	 * android_app_Activity.c. Like ViewRootImpl.ViewPostImeInputStage.processKeyEvent() it goes to the decor
	 * view (AOSP DecorView.dispatchKeyEvent(): the Window.Callback, then PhoneWindow.onKeyDown()/onKeyUp()),
	 * a Ctrl shortcut to dispatchKeyShortcutEvent(), and what nobody handled to the fallback event handler
	 * (PhoneFallbackEventHandler: the media keys go to the media button session). */
	@Export
	public boolean dispatchKeyEventFromNative(KeyEvent event) {
		final int keyCode = event.getKeyCode();
		final boolean isDown = event.getAction() == KeyEvent.ACTION_DOWN;
		final Window.Callback cb = getCallback();
		final boolean handled = cb != null ? cb.dispatchKeyEvent(event) : superDispatchKeyEvent(event);
		if (handled)
			return true;
		if (isDown ? onKeyDown(-1, keyCode, event) : onKeyUp(-1, keyCode, event))
			return true;

		/* ViewRootImpl.dispatchUnhandledKeyEvent(): the views' OnUnhandledKeyEventListeners are asked about a key
		 * nothing handled */
		if (peekDecorView() != null && peekDecorView().dispatchUnhandledKeyEvent(event))
			return true;

		// If the Control modifier is held, try to interpret the key as a shortcut.
		if (event.getAction() == KeyEvent.ACTION_DOWN
		    && event.isCtrlPressed()
		    && event.getRepeatCount() == 0
		    && !KeyEvent.isModifierKey(event.getKeyCode())) {
			if (cb != null ? cb.dispatchKeyShortcutEvent(event) : superDispatchKeyShortcutEvent(event))
				return true;
		}

		/* PhoneFallbackEventHandler.onKeyDown()/onKeyUp(): handleMediaKeyEvent() */
		if (KeyEvent.isMediaSessionKey(keyCode) || keyCode == KeyEvent.KEYCODE_MUTE) {
			((android.media.session.MediaSessionManager)context.getSystemService(Context.MEDIA_SESSION_SERVICE))
			    .dispatchMediaKeyEventAsSystemService(event);
			return true;
		}

		/* ViewRootImpl.ViewPostImeInputStage.processKeyEvent(): Meta+Tab and Meta+Shift+Tab move the focus to the
		 * next or previous keyboard navigation cluster (Tab alone is GTK's move-focus, see moveFocusOnTab()) */
		if (isDown && keyCode == KeyEvent.KEYCODE_TAB && peekDecorView() != null) {
			int groupNavigationDirection = 0;
			if (KeyEvent.metaStateHasModifiers(event.getMetaState(), KeyEvent.META_META_ON)) {
				groupNavigationDirection = View.FOCUS_FORWARD;
			} else if (KeyEvent.metaStateHasModifiers(event.getMetaState(), KeyEvent.META_META_ON | KeyEvent.META_SHIFT_ON)) {
				groupNavigationDirection = View.FOCUS_BACKWARD;
			}
			if (groupNavigationDirection != 0 && performKeyboardGroupNavigation(peekDecorView(), groupNavigationDirection))
				return true;
		}
		return false;
	}

	/* ViewRootImpl.performKeyboardGroupNavigation(), without the sound effect; root is the window's mView */
	private static boolean performKeyboardGroupNavigation(View root, int direction) {
		final View focused = root.findFocus();
		if (focused == null && root.restoreDefaultFocus()) {
			return true;
		}
		View cluster = focused == null ? root.keyboardNavigationClusterSearch(null, direction)
		                               : focused.keyboardNavigationClusterSearch(null, direction);

		// Since requestFocus only takes "real" focus directions (and therefore also
		// restoreFocusInCluster), convert forward/backward focus into FOCUS_DOWN.
		int realDirection = direction;
		if (direction == View.FOCUS_FORWARD || direction == View.FOCUS_BACKWARD) {
			realDirection = View.FOCUS_DOWN;
		}

		if (cluster != null && cluster == root) {
			// the default cluster. Try to find a non-clustered view to focus.
			if (cluster.restoreFocusNotInCluster()) {
				return true;
			}
			// otherwise skip to next actual cluster
			cluster = root.keyboardNavigationClusterSearch(null, direction);
		}

		return cluster != null && cluster.restoreFocusInCluster(realDirection);
	}

	/* PhoneWindow.onKeyDown() for the window's own keys: the media keys go to the window's media controller
	 * (if the activity set one); Back is tracked by the activity, ATL has no panels for the menu key and
	 * leaves the volume keys to the desktop */
	protected boolean onKeyDown(int featureId, int keyCode, KeyEvent event) {
		switch (keyCode) {
			// These are all the recognized media key codes in
			// KeyEvent.isMediaSessionKey()
			case KeyEvent.KEYCODE_MEDIA_PLAY:
			case KeyEvent.KEYCODE_MEDIA_PAUSE:
			case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
			case KeyEvent.KEYCODE_MUTE:
			case KeyEvent.KEYCODE_HEADSETHOOK:
			case KeyEvent.KEYCODE_MEDIA_STOP:
			case KeyEvent.KEYCODE_MEDIA_NEXT:
			case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
			case KeyEvent.KEYCODE_MEDIA_REWIND:
			case KeyEvent.KEYCODE_MEDIA_RECORD:
			case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD: {
				if (mMediaController != null) {
					if (((android.media.session.MediaSessionManager)context.getSystemService(Context.MEDIA_SESSION_SERVICE))
						.dispatchMediaKeyEventToSessionAsSystemService(event, mMediaController.getSessionToken())) {
						return true;
					}
				}
				return false;
			}
		}
		return false;
	}

	/* PhoneWindow.onKeyUp(): the key's tracking ends (KeyEvent.DispatcherState.handleUpEvent()) */
	protected boolean onKeyUp(int featureId, int keyCode, KeyEvent event) {
		final KeyEvent.DispatcherState dispatcher =
		    decorView != null ? decorView.getKeyDispatcherState() : null;
		if (dispatcher != null) {
			dispatcher.handleUpEvent(event);
		}
		// the media keys go to the media controller as on the way down
		if (mMediaController != null && KeyEvent.isMediaSessionKey(keyCode)) {
			return ((android.media.session.MediaSessionManager)context.getSystemService(Context.MEDIA_SESSION_SERVICE))
			    .dispatchMediaKeyEventToSessionAsSystemService(event, mMediaController.getSessionToken());
		}
		return false;
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

	/* AOSP PhoneWindow.requestFeature() */
	public boolean requestFeature(int featureId) {
		if (mContentParentExplicitlySet)
			throw new android.util.AndroidRuntimeException("requestFeature() must be called before adding content");
		return requestFeatureInternal(featureId);
	}

	private boolean requestFeatureInternal(int featureId) {
		final int features = mFeatures;
		final int newFeatures = features | (1 << featureId);
		if ((newFeatures & (1 << FEATURE_CUSTOM_TITLE)) != 0 && (newFeatures & ~CUSTOM_TITLE_COMPATIBLE_FEATURES) != 0)
			throw new android.util.AndroidRuntimeException("You cannot combine custom titles with other title features");
		if ((features & (1 << FEATURE_NO_TITLE)) != 0 && featureId == FEATURE_ACTION_BAR)
			return false; // Ignore. No title dominates.
		if ((features & (1 << FEATURE_ACTION_BAR)) != 0 && featureId == FEATURE_NO_TITLE)
			mFeatures &= ~(1 << FEATURE_ACTION_BAR); // Remove the action bar feature if we have no title. No title dominates.
		mFeatures |= 1 << featureId;
		return (mFeatures & (1 << featureId)) != 0;
	}

	private static final int CUSTOM_TITLE_COMPATIBLE_FEATURES = DEFAULT_FEATURES | (1 << FEATURE_CUSTOM_TITLE) | (1 << FEATURE_CONTENT_TRANSITIONS) | (1 << FEATURE_ACTIVITY_TRANSITIONS) | (1 << FEATURE_ACTION_MODE_OVERLAY);

	public final int getFeatures() {
		return mFeatures;
	}

	/* AOSP PhoneWindow.setFeatureInt() -> onIntChanged() and setFeatureDrawableResource() -> onDrawableChanged()
	 * change the progress bars, custom title container and icons of the title bar layout (screen_title*.xml);
	 * ATL's decor has no such title bar, so there is no view to update */
	public void setFeatureInt(int featureId, int value) {}

	public void setFeatureDrawableResource(int featureId, int resId) {}

	/* AOSP 16 Window.setDesiredHdrHeadroom()/getDesiredHdrHeadroom(): stored in the LayoutParams */
	public void setDesiredHdrHeadroom(float desiredHeadroom) {
		final WindowManager.LayoutParams attrs = getAttributes();
		attrs.setDesiredHdrHeadroom(desiredHeadroom);
		dispatchWindowAttributesChanged(attrs);
	}

	public float getDesiredHdrHeadroom() {
		return getAttributes().getDesiredHdrHeadroom();
	}

	/* AOSP */
	@SuppressWarnings("unchecked")
	public final <T extends View> T requireViewById(int id) {
		T view = (T)findViewById(id);
		if (view == null) {
			throw new IllegalArgumentException("ID does not reference a View inside this Window");
		}
		return view;
	}

	/* AOSP hands the listener to the window's hardware renderer, which reports each frame; GSK reports no frame
	 * metrics, so the listener is kept and never called (as in AOSP for a window without hardware rendering) */
	private final java.util.ArrayList<OnFrameMetricsAvailableListener> frameMetricsListeners = new java.util.ArrayList<>();

	public final void addOnFrameMetricsAvailableListener(OnFrameMetricsAvailableListener listener, android.os.Handler handler) {
		if (getDecorView() == null) {
			throw new IllegalStateException("can't observe a Window without an attached view");
		}
		if (listener == null) {
			throw new NullPointerException("listener cannot be null");
		}
		frameMetricsListeners.add(listener);
	}

	public final void removeOnFrameMetricsAvailableListener(OnFrameMetricsAvailableListener listener) {
		if (!frameMetricsListeners.remove(listener)) {
			throw new IllegalArgumentException("attempt to remove OnFrameMetricsAvailableListener that was never added");
		}
	}

	public View findViewById(int id) {
		View view = decorView.findViewById(id);
		if (view == null && id == com.android.internal.R.id.action_bar)
			return new Toolbar(context);
		return view;
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

	/* AOSP PhoneWindow.setStatusBarColor()/setNavigationBarColor(): ATL's window has no system bars to color, the
	 * colors are kept for the getters (initially the theme's, as PhoneWindow.generateLayout() reads them) */
	private Integer mStatusBarColor;
	private Integer mNavigationBarColor;

	public void setStatusBarColor(int color) {
		mStatusBarColor = color;
	}

	public void setNavigationBarColor(int color) {
		mNavigationBarColor = color;
	}

	private int themeColor(int index, int fallback) {
		android.content.res.TypedArray a = context.obtainStyledAttributes(com.android.internal.R.styleable.Window);
		int color = a.getColor(index, fallback);
		a.recycle();
		return color;
	}

	/* API 28: the divider between the app and the navigation bar, which ATL's window has none of; the color is kept */
	private int mNavigationBarDividerColor;

	public void setNavigationBarDividerColor(int dividerColor) {
		mNavigationBarDividerColor = dividerColor;
	}

	public int getNavigationBarDividerColor() {
		return mNavigationBarDividerColor;
	}

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
		if (mNavigationBarColor == null)
			mNavigationBarColor = themeColor(com.android.internal.R.styleable.Window_navigationBarColor, 0xFF000000);
		return mNavigationBarColor;
	}

	public void setBackgroundDrawableResource(int resId) {
		setBackgroundDrawable(context.getDrawable(resId));
	}

	public int getStatusBarColor() {
		if (mStatusBarColor == null)
			mStatusBarColor = themeColor(com.android.internal.R.styleable.Window_statusBarColor, 0xFF000000);
		return mStatusBarColor;
	}

	public Context getContext() {
		return context;
	}

	public boolean hasFeature(int featureId) {
		return (getFeatures() & (1 << featureId)) != 0;
	}

	/* the desktop's window title. Without a title (Activity.setTitle(null)) it is the application's label, like the
	 * caption of a desktop window in AOSP (WindowDecorTaskResourceLoader: pm.getApplicationLabel()) */
	public void setTitle(CharSequence title) {
		/* AOSP PhoneWindow.setTitle(): the decor layout's action bar shows it too */
		if (mDecorContentParent != null)
			mDecorContentParent.setWindowTitle(title);
		mTitle = title;
		if (title == null)
			title = context.getPackageManager().getApplicationLabel(context.getApplicationInfo());
		set_title(native_window, title.toString());
	}

	/* AOSP's Transition is abstract; an empty TransitionSet is the transition that does nothing (no shared elements) */
	public Transition getSharedElementEnterTransition() {
		return new android.transition.TransitionSet();
	}

	public void setSharedElementExitTransition(Transition transition) {}

	/* AOSP Window.setSharedElementsUseOverlay(): a no-op in AOSP's Window too (PhoneWindow handles it) */
	public void setSharedElementsUseOverlay(boolean sharedElementsUseOverlay) {}

	public void setSharedElementReenterTransition(Transition transition) {}

	/* AOSP PhoneWindow: the return transition defaults to USE_DEFAULT_TRANSITION, which
	 * getSharedElementReturnTransition() resolves to the enter transition */
	private Transition mSharedElementReturnTransition;
	private boolean mSharedElementReturnTransitionSet;

	public void setSharedElementReturnTransition(Transition transition) {
		mSharedElementReturnTransition = transition;
		mSharedElementReturnTransitionSet = true;
	}

	public Transition getSharedElementReturnTransition() {
		return mSharedElementReturnTransitionSet ? mSharedElementReturnTransition : getSharedElementEnterTransition();
	}

	/* AOSP PhoneWindow.setTransitionBackgroundFadeDuration()/getTransitionBackgroundFadeDuration();
	 * stored only, ATL plays no window transitions */
	private long mBackgroundFadeDurationMillis = -1;

	public void setTransitionBackgroundFadeDuration(long fadeDurationMillis) {
		if (fadeDurationMillis < 0) {
			throw new IllegalArgumentException("negative durations are not allowed");
		}
		mBackgroundFadeDurationMillis = fadeDurationMillis;
	}

	public long getTransitionBackgroundFadeDuration() {
		return (mBackgroundFadeDurationMillis < 0) ? 300 /* DEFAULT_BACKGROUND_FADE_DURATION_MS */ : mBackgroundFadeDurationMillis;
	}

	/* AOSP PhoneWindow.setVolumeControlStream()/getVolumeControlStream(): stored; ATL's volume
	 * keys do not pick a stream */
	private int mVolumeControlStreamType = Integer.MIN_VALUE; /* AudioManager.USE_DEFAULT_STREAM_TYPE */

	public void setVolumeControlStream(int streamType) {
		mVolumeControlStreamType = streamType;
	}

	public int getVolumeControlStream() {
		return mVolumeControlStreamType;
	}

	/* AOSP's Transition is abstract; an empty TransitionSet is the transition that does nothing (no shared elements) */
	public Transition getSharedElementExitTransition() {
		return new android.transition.TransitionSet();
	}

	/* AOSP's Transition is abstract; an empty TransitionSet is the transition that does nothing (no shared elements) */
	public Transition getSharedElementReenterTransition() {
		return new android.transition.TransitionSet();
	}

	/* AOSP stores the transition in the Window; ATL plays no window transitions */
	private Transition mExitTransition;

	public void setExitTransition(Transition transition) {
		mExitTransition = transition;
	}

	public Transition getExitTransition() {
		return mExitTransition;
	}

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

	/* The shell's on-screen keyboard is shown and the compositor made the window lower to keep it free (Phosh):
	 * that is the IME, not a new window size. As for a window that fits the IME (AOSP ViewRootImpl.
	 * adjustLayoutParamsForCompatibility(): fitInsetsTypes with ime() for SOFT_INPUT_ADJUST_RESIZE), the
	 * window frame ends above the IME; the configuration stays and WindowInsets has the IME visible, with no
	 * overlap of the window. The size of the window without the keyboard is kept to tell the two apart. */
	private int mFullWidth = -1;
	private int mFullHeight = -1;
	private boolean mImeShown;

	private static final java.util.ArrayList<java.lang.ref.WeakReference<Window>> sWindows = new java.util.ArrayList<>();

	{
		synchronized (sWindows) {
			sWindows.add(new java.lang.ref.WeakReference<>(this));
		}
	}

	/** @hide the on-screen keyboard was shown or hidden (InputMethodManager.oskVisibleChanged()) */
	public static void imeVisibilityChanged() {
		java.util.ArrayList<Window> windows = new java.util.ArrayList<>();
		synchronized (sWindows) {
			for (java.util.Iterator<java.lang.ref.WeakReference<Window>> it = sWindows.iterator(); it.hasNext();) {
				Window w = it.next().get();
				if (w == null)
					it.remove();
				else
					windows.add(w);
			}
		}
		for (Window w : windows)
			w.updateImeOverlap();
	}

	/* TRUE if the window is lower only because of the on-screen keyboard */
	private boolean updateImeOverlap() {
		int w = decorView.getWidth();
		int h = decorView.getHeight();
		boolean osk = android.view.inputmethod.InputMethodManager.isOskVisible();
		boolean byIme = osk && mFullWidth == w && h < mFullHeight;
		if (!byIme && w > 0 && h > 0 && (!osk || mFullWidth != w)) {
			mFullWidth = w;
			mFullHeight = h;
		}
		boolean shown = osk && (byIme || mFullWidth == w);
		if (shown != mImeShown) {
			mImeShown = shown;
			requestApplyInsets();
		}
		return byIme;
	}

	/** @hide */
	public WindowInsets getWindowInsets() {
		/* like InsetsState.calculateInsets() for a window without any insets source: a desktop window has no
		 * system bars, cutout or rounded corners, so every inset is empty; only the visibility of the bars,
		 * set by WindowInsetsController, is kept */
		boolean[] typeVisibilityMap = new boolean[WindowInsets.Type.SIZE];
		for (int type = WindowInsets.Type.FIRST; type <= WindowInsets.Type.LAST; type <<= 1)
			typeVisibilityMap[WindowInsets.Type.indexOf(type)] = (visibleInsetTypes & type) != 0;
		/* the IME source is visible while the on-screen keyboard is shown, it does not overlap the window */
		typeVisibilityMap[WindowInsets.Type.indexOf(WindowInsets.Type.ime())] = mImeShown;
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

	/* AOSP PhoneWindow.set/isStatusBarContrastEnforced(), set/isNavigationBarContrastEnforced(): defaults from the
	 * theme (generateLayout()); a desktop window has no system bars to scrim, so they are only kept */
	private boolean mEnsureStatusBarContrastWhenTransparent;
	private boolean mEnsureNavigationBarContrastWhenTransparent = true;

	public void setStatusBarContrastEnforced(boolean enforced) {
		mEnsureStatusBarContrastWhenTransparent = enforced;
	}

	public boolean isStatusBarContrastEnforced() {
		return mEnsureStatusBarContrastWhenTransparent;
	}

	public void setNavigationBarContrastEnforced(boolean enforced) {
		mEnsureNavigationBarContrastWhenTransparent = enforced;
	}

	public boolean isNavigationBarContrastEnforced() {
		return mEnsureNavigationBarContrastWhenTransparent;
	}

	public void setSharedElementEnterTransition(Transition transition) {}

	/* AOSP PhoneWindow: enter/return/reenter transitions and the overlap flags; return and reenter default
	 * (USE_DEFAULT_TRANSITION) to the enter and exit transition. Stored only, ATL plays no window transitions */
	private Transition mEnterTransition;
	private Transition mReturnTransition;
	private boolean mReturnTransitionSet;
	private Transition mReenterTransition;
	private boolean mReenterTransitionSet;
	private Boolean mAllowEnterTransitionOverlap;
	private Boolean mAllowReturnTransitionOverlap;

	public void setEnterTransition(Transition transition) {
		mEnterTransition = transition;
	}

	public Transition getEnterTransition() {
		return mEnterTransition;
	}

	public void setReturnTransition(Transition transition) {
		mReturnTransition = transition;
		mReturnTransitionSet = true;
	}

	public Transition getReturnTransition() {
		return mReturnTransitionSet ? mReturnTransition : getEnterTransition();
	}

	public void setReenterTransition(Transition transition) {
		mReenterTransition = transition;
		mReenterTransitionSet = true;
	}

	public Transition getReenterTransition() {
		return mReenterTransitionSet ? mReenterTransition : getExitTransition();
	}

	public void setAllowEnterTransitionOverlap(boolean allow) {
		mAllowEnterTransitionOverlap = allow;
	}

	public boolean getAllowEnterTransitionOverlap() {
		return (mAllowEnterTransitionOverlap == null) ? true : mAllowEnterTransitionOverlap;
	}

	public void setAllowReturnTransitionOverlap(boolean allowExitTransitionOverlap) {
		mAllowReturnTransitionOverlap = allowExitTransitionOverlap;
	}

	public boolean getAllowReturnTransitionOverlap() {
		return (mAllowReturnTransitionOverlap == null) ? true : mAllowReturnTransitionOverlap;
	}

	/* AOSP PhoneWindow.setElevation()/getElevation(): the decor's elevation casts the window's shadow; ATL's
	 * window frame is GTK's, so it is only kept */
	private float mElevation;

	public void setElevation(float elevation) {
		mElevation = elevation;
	}

	public float getElevation() {
		return mElevation;
	}

	/* AOSP PhoneWindow.setBackgroundBlurRadius(): the decor blurs behind it only with cross-window blur, which
	 * WindowManager.isCrossWindowBlurEnabled() reports off here */
	public void setBackgroundBlurRadius(int blurRadius) {}

	/* AOSP Window.getColorMode() */
	public int getColorMode() {
		return getAttributes().getColorMode();
	}

	/* AOSP Window.setHideOverlayWindows(): needs HIDE_(NON_SYSTEM_)OVERLAY_WINDOWS, then a private flag */
	public final void setHideOverlayWindows(boolean hide) {
		if (context.checkSelfPermission("android.permission.HIDE_NON_SYSTEM_OVERLAY_WINDOWS") != android.content.pm.PackageManager.PERMISSION_GRANTED
		    && context.checkSelfPermission("android.permission.HIDE_OVERLAY_WINDOWS") != android.content.pm.PackageManager.PERMISSION_GRANTED) {
			throw new SecurityException("Permission denial: setHideOverlayWindows: HIDE_OVERLAY_WINDOWS");
		}
		final WindowManager.LayoutParams attrs = getAttributes();
		final int mask = WindowManager.LayoutParams.SYSTEM_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS;
		attrs.privateFlags = (attrs.privateFlags & ~mask) | ((hide ? mask : 0) & mask);
		dispatchWindowAttributesChanged(attrs);
	}

	/* AOSP Window.setSustainedPerformanceMode() (setPrivateFlags()) */
	public void setSustainedPerformanceMode(boolean enable) {
		final WindowManager.LayoutParams attrs = getAttributes();
		final int mask = WindowManager.LayoutParams.PRIVATE_FLAG_SUSTAINED_PERFORMANCE_MODE;
		attrs.privateFlags = (attrs.privateFlags & ~mask) | ((enable ? mask : 0) & mask);
		dispatchWindowAttributesChanged(attrs);
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
	private native void native_set_drop_target(long decor_widget);

	/* ---- AOSP PhoneWindow: the options menu of the decor layout's action bar ---- */

	/* AOSP PhoneWindow.mContentParent: the view the content goes into, the decor view itself unless the window
	 * has the decor layout of the action bar */
	private ViewGroup mContentParent;
	private com.android.internal.widget.DecorContentParent mDecorContentParent;
	private CharSequence mTitle;
	private int mUiOptions;

	private static final int FLAG_RESOURCE_SET_ICON = 1 << 0;
	private static final int FLAG_RESOURCE_SET_LOGO = 1 << 1;
	private static final int FLAG_RESOURCE_SET_ICON_FALLBACK = 1 << 2;
	private int mResourcesSetFlags;
	private int mIconRes;
	private int mLogoRes;

	/* AOSP Window.getLocalFeatures(): the features of this window; ATL's windows have no container window, so
	 * they are all of its features */
	protected final int getLocalFeatures() {
		return mFeatures;
	}

	/* AOSP PhoneWindow.setUiOptions() */
	public void setUiOptions(int uiOptions) {
		mUiOptions = uiOptions;
	}

	public void setUiOptions(int uiOptions, int mask) {
		mUiOptions = (mUiOptions & ~mask) | (uiOptions & mask);
	}

	/* AOSP PhoneWindow.setIcon()/setDefaultIcon()/setLogo()/setDefaultLogo(): the icon and logo of the decor
	 * layout's action bar */
	public void setIcon(int resId) {
		mIconRes = resId;
		mResourcesSetFlags |= FLAG_RESOURCE_SET_ICON;
		mResourcesSetFlags &= ~FLAG_RESOURCE_SET_ICON_FALLBACK;
		if (mDecorContentParent != null)
			mDecorContentParent.setIcon(resId);
	}

	/** @hide */
	public void setDefaultIcon(int resId) {
		if ((mResourcesSetFlags & FLAG_RESOURCE_SET_ICON) != 0)
			return;
		mIconRes = resId;
		if (mDecorContentParent != null && (!mDecorContentParent.hasIcon() || (mResourcesSetFlags & FLAG_RESOURCE_SET_ICON_FALLBACK) != 0)) {
			if (resId != 0) {
				mDecorContentParent.setIcon(resId);
				mResourcesSetFlags &= ~FLAG_RESOURCE_SET_ICON_FALLBACK;
			} else {
				mDecorContentParent.setIcon(getContext().getPackageManager().getDefaultActivityIcon());
				mResourcesSetFlags |= FLAG_RESOURCE_SET_ICON_FALLBACK;
			}
		}
	}

	public void setLogo(int resId) {
		mLogoRes = resId;
		mResourcesSetFlags |= FLAG_RESOURCE_SET_LOGO;
		if (mDecorContentParent != null)
			mDecorContentParent.setLogo(resId);
	}

	/** @hide */
	public void setDefaultLogo(int resId) {
		if ((mResourcesSetFlags & FLAG_RESOURCE_SET_LOGO) != 0)
			return;
		mLogoRes = resId;
		if (mDecorContentParent != null && !mDecorContentParent.hasLogo())
			mDecorContentParent.setLogo(resId);
	}

	/* AOSP PhoneWindow.PanelFeatureState, the part the action bar's menu uses (ATL has no panel windows) */
	private static final class PanelFeatureState {
		final int featureId;
		com.android.internal.view.menu.MenuBuilder menu;
		View createdPanelView;
		boolean isPrepared;
		boolean isHandled;
		boolean refreshMenuContent;
		boolean refreshDecorView;
		boolean qwertyMode;
		Bundle frozenActionViewState;

		PanelFeatureState(int featureId) {
			this.featureId = featureId;
			refreshDecorView = false;
		}

		void setMenu(com.android.internal.view.menu.MenuBuilder menu) {
			this.menu = menu;
		}
	}

	private PanelFeatureState[] mPanels;
	private PanelFeatureState mPreparedPanel;
	private int mInvalidatePanelMenuFeatures;
	private boolean mInvalidatePanelMenuPosted;
	private boolean mClosingActionMenu;
	private ActionMenuPresenterCallback mActionMenuPresenterCallback;

	/* AOSP PhoneWindow.getPanelState() */
	private PanelFeatureState getPanelState(int featureId, boolean required) {
		if ((getFeatures() & (1 << featureId)) == 0) {
			if (!required)
				return null;
			throw new RuntimeException("The feature has not been requested");
		}
		PanelFeatureState[] ar;
		if ((ar = mPanels) == null || ar.length <= featureId) {
			PanelFeatureState[] nar = new PanelFeatureState[featureId + 1];
			if (ar != null)
				System.arraycopy(ar, 0, nar, 0, ar.length);
			mPanels = ar = nar;
		}
		PanelFeatureState st = ar[featureId];
		if (st == null)
			ar[featureId] = st = new PanelFeatureState(featureId);
		return st;
	}

	/* AOSP PhoneWindow.findMenuPanel() */
	private PanelFeatureState findMenuPanel(Menu menu) {
		final PanelFeatureState[] panels = mPanels;
		final int N = panels != null ? panels.length : 0;
		for (int i = 0; i < N; i++) {
			final PanelFeatureState panel = panels[i];
			if (panel != null && panel.menu == menu)
				return panel;
		}
		return null;
	}

	/* AOSP PhoneWindow.openPanel(int, KeyEvent): the options menu of the decor action bar is its overflow menu.
	 * ATL has no panel windows (the options menu at the bottom of a window without action bar), so there is
	 * nothing else to open. */
	public void openPanel(int featureId, KeyEvent event) {
		if (featureId == FEATURE_OPTIONS_PANEL && mDecorContentParent != null && mDecorContentParent.canShowOverflowMenu()
		    && !ViewConfiguration.get(getContext()).hasPermanentMenuKey()) {
			mDecorContentParent.showOverflowMenu();
		}
	}

	/* AOSP PhoneWindow.closePanel(int), with the same limit as openPanel() */
	public void closePanel(int featureId) {
		if (featureId == FEATURE_OPTIONS_PANEL && mDecorContentParent != null && mDecorContentParent.canShowOverflowMenu()
		    && !ViewConfiguration.get(getContext()).hasPermanentMenuKey()) {
			mDecorContentParent.hideOverflowMenu();
		}
	}

	/* AOSP PhoneWindow.invalidatePanelMenu(): the menu is made again on the next animation frame */
	public void invalidatePanelMenu(int featureId) {
		mInvalidatePanelMenuFeatures |= 1 << featureId;
		if (!mInvalidatePanelMenuPosted && decorView != null) {
			decorView.postOnAnimation(mInvalidatePanelMenuRunnable);
			mInvalidatePanelMenuPosted = true;
		}
	}

	private final Runnable mInvalidatePanelMenuRunnable = new Runnable() {
		@Override
		public void run() {
			for (int i = 0; i <= FEATURE_MAX; i++) {
				if ((mInvalidatePanelMenuFeatures & 1 << i) != 0)
					doInvalidatePanelMenu(i);
			}
			mInvalidatePanelMenuPosted = false;
			mInvalidatePanelMenuFeatures = 0;
		}
	};

	/* AOSP PhoneWindow.doInvalidatePanelMenu() */
	private void doInvalidatePanelMenu(int featureId) {
		PanelFeatureState st = getPanelState(featureId, false);
		if (st == null)
			return;
		Bundle savedActionViewStates = null;
		if (st.menu != null) {
			savedActionViewStates = new Bundle();
			st.menu.saveActionViewStates(savedActionViewStates);
			if (savedActionViewStates.size() > 0)
				st.frozenActionViewState = savedActionViewStates;
			st.menu.stopDispatchingItemsChanged();
			st.menu.clear();
		}
		st.refreshMenuContent = true;
		st.refreshDecorView = true;
		if ((featureId == FEATURE_ACTION_BAR || featureId == FEATURE_OPTIONS_PANEL) && mDecorContentParent != null) {
			st = getPanelState(FEATURE_OPTIONS_PANEL, false);
			if (st != null) {
				st.isPrepared = false;
				preparePanel(st, null);
			}
		}
	}

	/* AOSP PhoneWindow.preparePanel(): the Window.Callback creates (onCreatePanelMenu()) and prepares
	 * (onPreparePanel()) the menu, which the decor action bar shows */
	private boolean preparePanel(PanelFeatureState st, KeyEvent event) {
		if (st.isPrepared)
			return true;
		if ((mPreparedPanel != null) && (mPreparedPanel != st))
			mPreparedPanel.isPrepared = false; // AOSP closePanel(mPreparedPanel, false); ATL has no panel to close
		final Callback cb = getCallback();
		if (cb != null)
			st.createdPanelView = cb.onCreatePanelView(st.featureId);
		final boolean isActionBarMenu = (st.featureId == FEATURE_OPTIONS_PANEL || st.featureId == FEATURE_ACTION_BAR);
		if (isActionBarMenu && mDecorContentParent != null)
			mDecorContentParent.setMenuPrepared();
		if (st.createdPanelView == null) {
			if (st.menu == null || st.refreshMenuContent) {
				if (st.menu == null) {
					if (!initializePanelMenu(st) || (st.menu == null))
						return false;
				}
				if (isActionBarMenu && mDecorContentParent != null) {
					if (mActionMenuPresenterCallback == null)
						mActionMenuPresenterCallback = new ActionMenuPresenterCallback();
					mDecorContentParent.setMenu(st.menu, mActionMenuPresenterCallback);
				}
				st.menu.stopDispatchingItemsChanged();
				if ((cb == null) || !cb.onCreatePanelMenu(st.featureId, st.menu)) {
					st.setMenu(null);
					if (isActionBarMenu && mDecorContentParent != null)
						mDecorContentParent.setMenu(null, mActionMenuPresenterCallback);
					return false;
				}
				st.refreshMenuContent = false;
			}
			st.menu.stopDispatchingItemsChanged();
			if (st.frozenActionViewState != null) {
				st.menu.restoreActionViewStates(st.frozenActionViewState);
				st.frozenActionViewState = null;
			}
			if (!cb.onPreparePanel(st.featureId, st.createdPanelView, st.menu)) {
				if (isActionBarMenu && mDecorContentParent != null)
					mDecorContentParent.setMenu(null, mActionMenuPresenterCallback);
				st.menu.startDispatchingItemsChanged();
				return false;
			}
			KeyCharacterMap kmap = KeyCharacterMap.load(event != null ? event.getDeviceId() : KeyCharacterMap.VIRTUAL_KEYBOARD);
			st.qwertyMode = kmap.getKeyboardType() != KeyCharacterMap.NUMERIC;
			st.menu.setQwertyMode(st.qwertyMode);
			st.menu.startDispatchingItemsChanged();
		}
		st.isPrepared = true;
		st.isHandled = false;
		mPreparedPanel = st;
		return true;
	}

	/* AOSP PhoneWindow.initializePanelMenu(): the menu of the action bar is themed with actionBarTheme and
	 * actionBarWidgetTheme */
	private boolean initializePanelMenu(final PanelFeatureState st) {
		Context context = getContext();
		if ((st.featureId == FEATURE_OPTIONS_PANEL || st.featureId == FEATURE_ACTION_BAR) && mDecorContentParent != null) {
			final TypedValue outValue = new TypedValue();
			final Resources.Theme baseTheme = context.getTheme();
			baseTheme.resolveAttribute(com.android.internal.R.attr.actionBarTheme, outValue, true);
			Resources.Theme widgetTheme = null;
			if (outValue.resourceId != 0) {
				widgetTheme = context.getResources().newTheme();
				widgetTheme.setTo(baseTheme);
				widgetTheme.applyStyle(outValue.resourceId, true);
				widgetTheme.resolveAttribute(com.android.internal.R.attr.actionBarWidgetTheme, outValue, true);
			} else {
				baseTheme.resolveAttribute(com.android.internal.R.attr.actionBarWidgetTheme, outValue, true);
			}
			if (outValue.resourceId != 0) {
				if (widgetTheme == null) {
					widgetTheme = context.getResources().newTheme();
					widgetTheme.setTo(baseTheme);
				}
				widgetTheme.applyStyle(outValue.resourceId, true);
			}
			if (widgetTheme != null) {
				context = new ContextThemeWrapper(context, 0);
				context.getTheme().setTo(widgetTheme);
			}
		}
		final com.android.internal.view.menu.MenuBuilder menu = new com.android.internal.view.menu.MenuBuilder(context);
		menu.setCallback(mMenuBuilderCallback);
		st.setMenu(menu);
		return true;
	}

	/* the MenuBuilder.Callback that AOSP's PhoneWindow is itself */
	private final com.android.internal.view.menu.MenuBuilder.Callback mMenuBuilderCallback = new com.android.internal.view.menu.MenuBuilder.Callback() {
		/* AOSP PhoneWindow.onMenuItemSelected(MenuBuilder, MenuItem) */
		@Override
		public boolean onMenuItemSelected(com.android.internal.view.menu.MenuBuilder menu, MenuItem item) {
			final Callback cb = getCallback();
			if (cb != null) {
				final PanelFeatureState panel = findMenuPanel(menu.getRootMenu());
				if (panel != null)
					return cb.onMenuItemSelected(panel.featureId, item);
			}
			return false;
		}

		/* AOSP PhoneWindow.onMenuModeChange(): reopenMenu(true), the part for the decor action bar */
		@Override
		public void onMenuModeChange(com.android.internal.view.menu.MenuBuilder menu) {
			if (mDecorContentParent == null || !mDecorContentParent.canShowOverflowMenu()
			    || (ViewConfiguration.get(getContext()).hasPermanentMenuKey() && !mDecorContentParent.isOverflowMenuShowPending()))
				return;
			final Callback cb = getCallback();
			if (!mDecorContentParent.isOverflowMenuShowing()) {
				if (cb != null) {
					if (mInvalidatePanelMenuPosted && (mInvalidatePanelMenuFeatures & (1 << FEATURE_OPTIONS_PANEL)) != 0) {
						decorView.removeCallbacks(mInvalidatePanelMenuRunnable);
						mInvalidatePanelMenuRunnable.run();
					}
					final PanelFeatureState st = getPanelState(FEATURE_OPTIONS_PANEL, false);
					if (st != null && st.menu != null && !st.refreshMenuContent
					    && cb.onPreparePanel(FEATURE_OPTIONS_PANEL, st.createdPanelView, st.menu)) {
						cb.onMenuOpened(FEATURE_ACTION_BAR, st.menu);
						mDecorContentParent.showOverflowMenu();
					}
				}
			} else {
				mDecorContentParent.hideOverflowMenu();
				final PanelFeatureState st = getPanelState(FEATURE_OPTIONS_PANEL, false);
				if (st != null && cb != null)
					cb.onPanelClosed(FEATURE_ACTION_BAR, st.menu);
			}
		}
	};

	/* AOSP PhoneWindow.ActionMenuPresenterCallback */
	private final class ActionMenuPresenterCallback implements com.android.internal.view.menu.MenuPresenter.Callback {
		@Override
		public boolean onOpenSubMenu(com.android.internal.view.menu.MenuBuilder subMenu) {
			Callback cb = getCallback();
			if (cb != null) {
				cb.onMenuOpened(FEATURE_ACTION_BAR, subMenu);
				return true;
			}
			return false;
		}

		@Override
		public void onCloseMenu(com.android.internal.view.menu.MenuBuilder menu, boolean allMenusAreClosing) {
			checkCloseActionMenu(menu);
		}
	}

	/* AOSP PhoneWindow.checkCloseActionMenu() */
	private void checkCloseActionMenu(Menu menu) {
		if (mClosingActionMenu)
			return;
		mClosingActionMenu = true;
		mDecorContentParent.dismissPopups();
		Callback cb = getCallback();
		if (cb != null)
			cb.onPanelClosed(FEATURE_ACTION_BAR, menu);
		mClosingActionMenu = false;
	}
}
