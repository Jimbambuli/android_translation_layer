package android.app;

import android.R;
import android.animation.ValueAnimator;
import android.atl.annotation.Export;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.Choreographer;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import java.util.ArrayList;

public class Dialog implements Window.Callback, DialogInterface, KeyEvent.Callback {
	protected long nativePtr;

	protected native long nativeInit(long decor_view);
	private native void nativeSetTitle(long ptr, String title);
	private native void nativeSetContentView(long ptr, long widget);
	private native void nativeShow(long ptr);
	private native void nativeClose(long ptr);
	private native boolean nativeIsShowing(long ptr);
	private static native void nativeDimBehind(float amount, int duration);

	private Context context;
	private Window window;
	private OnDismissListener onDismissListener;
	private OnShowListener onShowListener;
	private OnCancelListener onCancelListener;
	private OnKeyListener onKeyListener;
	private boolean cancelable = true;
	private boolean closeOnTouchOutside;
	private boolean closeOnTouchOutsideSet;
	private boolean created;
	private boolean started; // between onStart() and onStop()

	/* as in AOSP, the dialog's context carries the dialog theme (the given one, or the
	 * dialogTheme of the context's theme) */
	public Dialog(Context context, int themeResId) {
		if (themeResId == 0) {
			TypedValue outValue = new TypedValue();
			context.getTheme().resolveAttribute(R.attr.dialogTheme, outValue, true);
			themeResId = outValue.resourceId;
		}
		// like AOSP, the dialog's window belongs to the activity of the context
		for (Context c = context; c instanceof ContextWrapper; c = ((ContextWrapper)c).getBaseContext()) {
			if (c instanceof Activity) {
				ownerActivity = (Activity)c;
				break;
			}
		}
		this.context = new ContextThemeWrapper(context, themeResId);
		window = new Window(this.context, this);
		nativePtr = nativeInit(window.getDecorView().widget);

		window.set_native_window(nativePtr);
	}

	public Dialog(Context context) {
		this(context, 0);
	}

	public final boolean requestWindowFeature(int featureId) {
		return false;
	}

	public LayoutInflater getLayoutInflater() {
		return LayoutInflater.from(context);
	}

	public Context getContext() {
		return context;
	}

	public void setContentView(View view) {
		getWindow().setContentView(view);
	}

	public void setContentView(int layoutResId) {
		setContentView(LayoutInflater.from(context).inflate(layoutResId, null));
	}

	public void setContentView(View view, ViewGroup.LayoutParams lp) {
		view.setLayoutParams(lp);
		setContentView(view);
	}

	public void setTitle(CharSequence title) {
		nativeSetTitle(nativePtr, String.valueOf(title));
	}

	public void setTitle(int titleId) {
		nativeSetTitle(nativePtr, context.getString(titleId));
	}

	public void setOwnerActivity(Activity activity) {
		ownerActivity = activity;
	}

	public void setCancelable(boolean cancelable) {
		this.cancelable = cancelable;
	}

	public void setOnCancelListener(OnCancelListener onCancelListener) {
		this.onCancelListener = onCancelListener;
	}

	public void setOnDismissListener(OnDismissListener onDismissListener) {
		this.onDismissListener = onDismissListener;
	}

	public View findViewById(int id) {
		return window.findViewById(id);
	}

	public void show() {
		System.out.println("showing the Dialog " + this);
		Runnable action = new Runnable() {
			@Override
			public void run() {
				if (!created) {
					window.applyThemeAttributes();
					onCreate(null);
					created = true;
				}
				if (!started) {
					onStart();
					started = true;
				}
				// Read the size of the main window. Floating dialogs should be smaller as specified in the windowMinWidth* attributes.
				// Non-floating are typically constructed with MATCH_PARENT layout params and thus get the exact size of the main window.
				// Most non-floating dialogs are technically dialogs, but are expected to behave more like full size activities.
				Rect displayFrame = new Rect();
				getWindow().getDecorView().getWindowVisibleDisplayFrame(displayFrame);

				TypedArray a = context.obtainStyledAttributes(R.styleable.Window);
				// like PhoneWindow.setCloseOnTouchOutsideIfNotSet()
				if (!closeOnTouchOutsideSet)
					closeOnTouchOutside = a.getBoolean(R.styleable.Window_windowCloseOnTouchOutside, false);
				float windowWidthFraction = 1;
				float minWidthFraction = 0; // like AOSP, no minimum width without the attribute
				if (a.getBoolean(R.styleable.Window_windowIsFloating, false)) {
					int minWidthAttr = displayFrame.width() > displayFrame.height() ? R.styleable.Window_windowMinWidthMajor : R.styleable.Window_windowMinWidthMinor;
					windowWidthFraction = a.getFraction(minWidthAttr, 1, 1, 1);
					if (a.hasValue(minWidthAttr))
						minWidthFraction = windowWidthFraction;
				}
				a.recycle();

				LayoutParams lp = getWindow().getAttributes();
				int widthMeasureSpec;
				if (lp.width == LayoutParams.WRAP_CONTENT) {
					// like AOSP: start at the preferred dialog width (wider if the content does not
					// fit, see fitWindowToContent), the fraction is the minimum width
					displayWidth = displayFrame.width();
					baseWidth = Math.min(displayWidth, context.getResources().getDimensionPixelSize(com.android.internal.R.dimen.config_prefDialogWidth));
					widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(baseWidth, View.MeasureSpec.AT_MOST);
					getWindow().setMinWidth((int)(displayWidth * minWidthFraction));
				} else {
					widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(lp.width >= 0 ? lp.width : (int)(displayFrame.width() * windowWidthFraction), View.MeasureSpec.EXACTLY);
				}

				int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(lp.height >= 0 ? lp.height : (int)(displayFrame.height()),
				                                                         lp.height == LayoutParams.WRAP_CONTENT ? View.MeasureSpec.AT_MOST : View.MeasureSpec.EXACTLY);

				getWindow().getDecorView().internalSetDefaultMeasureSpec(widthMeasureSpec, heightMeasureSpec);

				wrapWidthMeasureSpec = widthMeasureSpec;
				wrapHeightMeasureSpec = heightMeasureSpec;
				if (!fitsWindowToContent) {
					fitsWindowToContent = true;
					getWindow().getDecorView().getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
						@Override
						public void onGlobalLayout() {
							fitWindowToContent();
						}
					});
				}

				shown = true;
				showPending = true;
				presentWhenLaidOut(3);
			}
		};
		if (Looper.myLooper() == Looper.getMainLooper()) {
			action.run();
		} else {
			new Handler(Looper.getMainLooper()).post(action);
		}
	}

	private int wrapWidthMeasureSpec;
	private int displayWidth;
	private int baseWidth;
	private int wrapHeightMeasureSpec;
	private boolean fitsWindowToContent;
	private int fittedWidth = -1;
	private int fittedHeight = -1;

	/* Like ViewRootImpl for a WRAP_CONTENT window: measure the content again after each layout and
	 * resize the window when its size changed. GTK sizes the window only once, when it is shown,
	 * and content that is created later (Compose composes when the view gets attached) stayed in a
	 * window of the size measured before. */
	private void fitWindowToContent() {
		LayoutParams lp = getWindow().getAttributes();
		if (lp.width != LayoutParams.WRAP_CONTENT && lp.height != LayoutParams.WRAP_CONTENT)
			return;
		View decor = getWindow().getDecorView();
		decor.measure(wrapWidthMeasureSpec, wrapHeightMeasureSpec);
		/* like ViewRootImpl.measureHierarchy: wider when the content does not fit */
		int widthSize = View.MeasureSpec.getSize(wrapWidthMeasureSpec);
		if (lp.width == LayoutParams.WRAP_CONTENT && (decor.getMeasuredWidthAndState() & View.MEASURED_STATE_TOO_SMALL) != 0 && widthSize < displayWidth) {
			widthSize = widthSize == baseWidth ? (baseWidth + displayWidth) / 2 : displayWidth;
			wrapWidthMeasureSpec = View.MeasureSpec.makeMeasureSpec(widthSize, View.MeasureSpec.AT_MOST);
			decor.internalSetDefaultMeasureSpec(wrapWidthMeasureSpec, wrapHeightMeasureSpec);
			decor.measure(wrapWidthMeasureSpec, wrapHeightMeasureSpec);
		}
		int width = lp.width == LayoutParams.WRAP_CONTENT ? decor.getMeasuredWidth() : lp.width;
		int height = lp.height == LayoutParams.WRAP_CONTENT ? decor.getMeasuredHeight() : lp.height;
		if (width == fittedWidth && height == fittedHeight)
			return;
		fittedWidth = width;
		fittedHeight = height;
		getWindow().set_layout(getWindow().native_window, width, height);
	}

	/* Like AOSP, which shows the window after its first traversal: content that is created when it
	 * gets attached (Compose composes in the next frame) is laid out first, so that the window is
	 * mapped, and placed by the compositor, with its final size. */
	private boolean showPending;
	private boolean shown; // from show() until the dialog is dismissed
	private void presentWhenLaidOut(final int framesLeft) {
		if (!showPending)
			return;
		fitWindowToContent();
		if ((fittedWidth <= 0 || fittedHeight <= 0) && framesLeft > 0) {
			Choreographer.getInstance().postFrameCallback(new Choreographer.FrameCallback() {
				@Override
				public void doFrame(long frameTimeNanos) {
					presentWhenLaidOut(framesLeft - 1);
				}
			});
			return;
		}
		showPending = false;
		nativeShow(nativePtr);
		if (!openWindows.contains(this))
			openWindows.add(this);
		animateWindow(true);
		if (onShowListener != null)
			onShowListener.onShow(this);
	}

	public boolean isShowing() {
		return showPending || nativeIsShowing(nativePtr);
	}

	@Export
	public void dismiss() {
		System.out.println("dismissing the Dialog " + Dialog.this);
		// HACK: dismissing the Dialog takes some time in AOSP, as the request goes back and forth between the application
		// and the system server. We replicate this behavior by adding 10 ms delay.
		// This Hack is required for NewPipe RouterActivity which has a race condition. It subscribes an rxJava observable
		// and immediately calls Dialog.dismiss(). The OnDismissListener would unsubscribes the observable again.
		new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
			@Override
			public void run() {
				// like AOSP, a dialog that is not shown (any more) is not dismissed again
				if (!shown)
					return;
				shown = false;
				showPending = false;
				animateWindow(false);
				if (started) {
					started = false;
					onStop();
				}
				if (onDismissListener != null)
					onDismissListener.onDismiss(Dialog.this);
			}
		}, 10);
	}

	public Window getWindow() {
		return window;
	}

	private static int dimmingDialogs;
	private boolean dimming;
	private Activity ownerActivity;
	private static final ArrayList<Dialog> openWindows = new ArrayList<>(); // dialogs whose window is shown

	/* The dim (FLAG_DIM_BEHIND) belongs to the dialog's window, as on AOSP: it comes with the window
	 * and goes with it, whichever way the window goes (dismiss(), hide(), the activity destroyed). */
	private void setDimmed(float amount, int duration) {
		boolean dim = amount > 0;
		if (dim == dimming)
			return;
		dimming = dim;
		dimmingDialogs += dim ? 1 : -1;
		if (dim)
			nativeDimBehind(amount, duration);
		else if (dimmingDialogs == 0)
			nativeDimBehind(0, duration);
	}

	private void closeWindow() {
		openWindows.remove(this);
		nativeClose(nativePtr);
	}

	/* like WindowManagerGlobal.closeAll() after an activity was destroyed: the windows of its dialogs
	 * that are still shown (leaked) are removed at once, and their dim with them */
	static void closeWindowsOf(Activity activity) {
		for (Dialog dialog : new ArrayList<>(openWindows)) {
			if (dialog.ownerActivity != activity)
				continue;
			dialog.showPending = false;
			dialog.window.getDecorView().clearAnimation();
			dialog.setDimmed(0, 0);
			dialog.closeWindow();
		}
	}

	/* Like WindowManager: the window enter/exit animation of the window's windowAnimations style
	 * (Animation.Material.Dialog and the like) runs on the content, the dim behind it (FLAG_DIM_BEHIND)
	 * fades in and out alongside, and a dismissed window goes away when its exit animation ends. */
	private void animateWindow(final boolean enter) {
		View decor = window.getDecorView();
		LayoutParams lp = window.getAttributes();
		Animation animation = null;
		if (ValueAnimator.getDurationScale() != 0 && lp.windowAnimations != 0) {
			TypedArray a = context.obtainStyledAttributes(lp.windowAnimations, com.android.internal.R.styleable.WindowAnimation);
			int id = a.getResourceId(enter ? com.android.internal.R.styleable.WindowAnimation_windowEnterAnimation
			                               : com.android.internal.R.styleable.WindowAnimation_windowExitAnimation, 0);
			a.recycle();
			if (id != 0) {
				try {
					animation = AnimationUtils.loadAnimation(context, id);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		// AOSP's dim layer follows the window's animation, 200 ms without one
		int duration = ValueAnimator.getDurationScale() == 0 ? 0 : animation != null ? (int)animation.computeDurationHint() : 200;
		setDimmed(enter && (lp.flags & LayoutParams.FLAG_DIM_BEHIND) != 0 ? lp.dimAmount : 0, duration);
		if (enter) {
			if (animation != null)
				decor.startAnimation(animation);
			else
				decor.clearAnimation();
			return;
		}
		if (animation == null) {
			decor.clearAnimation();
			closeWindow();
			return;
		}
		animation.setFillAfter(true); // stays hidden until the window is gone
		animation.setAnimationListener(new Animation.AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {}

			@Override
			public void onAnimationRepeat(Animation animation) {}

			@Override
			public void onAnimationEnd(Animation animation) {
				if (!shown && !showPending)
					closeWindow();
			}
		});
		decor.startAnimation(animation);
	}

	public void setCanceledOnTouchOutside(boolean cancel) {
		if (cancel && !cancelable)
			cancelable = true;
		closeOnTouchOutside = cancel;
		closeOnTouchOutsideSet = true;
	}

	/* a press outside of the shown dialog, see Dialog.onTouchEvent() and Window.shouldCloseOnTouch() */
	private void onTouchOutside() {
		if (cancelable && closeOnTouchOutside && isShowing())
			cancel();
	}

	public class Builder {
		public Builder(Context context) {
			System.out.println("making a Dialog$Builder");
		}
	}

	@Override
	public void onContentChanged() {
	}
	// the Window.Callback and option menu methods with the defaults of AOSP's Dialog

	@Override
	public boolean onCreatePanelMenu(int featureId, Menu menu) {
		if (featureId == Window.FEATURE_OPTIONS_PANEL)
			return onCreateOptionsMenu(menu);
		return false;
	}

	@Override
	public View onCreatePanelView(int featureId) {
		return null;
	}

	@Override
	public boolean onPreparePanel(int featureId, View view, Menu menu) {
		if (featureId == Window.FEATURE_OPTIONS_PANEL && menu != null)
			return onPrepareOptionsMenu(menu) && menu.hasVisibleItems();
		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		return false;
	}

	@Override
	public void onPanelClosed(int featureId, Menu menu) {}

	@Override
	public boolean onMenuOpened(int featureId, Menu menu) {
		return true;
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		return true;
	}

	public boolean onPrepareOptionsMenu(Menu menu) {
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		return false;
	}

	/* AOSP's Dialog.dispatchKeyEvent() */
	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		if ((onKeyListener != null) && (onKeyListener.onKey(this, event.getKeyCode(), event))) {
			return true;
		}
		if (window.superDispatchKeyEvent(event)) {
			return true;
		}
		View decor = window.getDecorView();
		return event.dispatch(this, decor != null
		    ? decor.getKeyDispatcherState() : null, this);
	}

	/* AOSP's Dialog.onKeyDown() */
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE) {
			event.startTracking();
			return true;
		}

		return false;
	}

	/* AOSP's Dialog.onKeyUp(). ATL has no ViewRootImpl that hands Back to the window's
	 * OnBackInvokedDispatcher, so the dialog does it here and calls onBackPressed() only when no
	 * callback is registered. */
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if ((keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE)
		    && event.isTracking()
		    && !event.isCanceled()) {
			if (!((android.window.WindowOnBackInvokedDispatcher)getOnBackInvokedDispatcher()).dispatchOnBackInvoked())
				onBackPressed();
			return true;
		}
		return false;
	}

	public boolean onKeyLongPress(int keyCode, KeyEvent event) {
		return false;
	}

	public boolean onKeyMultiple(int keyCode, int repeatCount, KeyEvent event) {
		return false;
	}

	public void onBackPressed() {
		if (cancelable)
			cancel();
	}

	public android.window.OnBackInvokedDispatcher getOnBackInvokedDispatcher() {
		return getWindow().getOnBackInvokedDispatcher();
	}

	// input reaches the views through GTK, so there is nothing to dispatch here

	@Override
	public boolean dispatchKeyShortcutEvent(KeyEvent event) {
		return false;
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent event) {
		return onTouchEvent(event);
	}

	@Override
	public boolean dispatchTrackballEvent(MotionEvent event) {
		return false;
	}

	@Override
	public boolean dispatchGenericMotionEvent(MotionEvent event) {
		return false;
	}

	@Override
	public void onWindowAttributesChanged(LayoutParams params) {}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {}

	@Override
	public void onAttachedToWindow() {}

	@Override
	public void onDetachedFromWindow() {}

	@Override
	public boolean onSearchRequested() {
		return false;
	}

	@Override
	public ActionMode onWindowStartingActionMode(ActionMode.Callback callback) {
		return null;
	}

	@Override
	public ActionMode onWindowStartingActionMode(ActionMode.Callback callback, int type) {
		return null;
	}

	@Override
	public void onActionModeStarted(ActionMode mode) {}

	@Override
	public void onActionModeFinished(ActionMode mode) {}

	protected void onCreate(Bundle savedInstanceState) {
		System.out.println("- onCreate - Dialog!");
	}

	protected void onStart() {}

	protected void onStop() {}

	private static final String DIALOG_SHOWING_TAG = "android:dialogShowing";

	public Bundle onSaveInstanceState() {
		Bundle bundle = new Bundle();
		bundle.putBoolean(DIALOG_SHOWING_TAG, isShowing());
		return bundle;
	}

	public void onRestoreInstanceState(Bundle savedInstanceState) {
		if (savedInstanceState.getBoolean(DIALOG_SHOWING_TAG))
			show();
	}

	public void hide() {
		System.out.println("hiding the Dialog " + this);
		showPending = false;
		window.getDecorView().clearAnimation();
		setDimmed(0, 0); // the window goes at once, its dim with it; show() brings both back
		closeWindow();
	}

	@Override
	public void cancel() {
		if (onCancelListener != null)
			onCancelListener.onCancel(this);
		dismiss();
	}

	public void setOnShowListener(OnShowListener onShowListener) {
		this.onShowListener = onShowListener;
	}

	public void setCancelMessage(Message msg) {}

	public void setDismissMessage(Message msg) {}

	public boolean onTouchEvent(MotionEvent event) {
		return false;
	}

	public void setOnKeyListener(OnKeyListener onKeyListener) {
		this.onKeyListener = onKeyListener;
	}

	public View getCurrentFocus() {
		return null;
	}
}
