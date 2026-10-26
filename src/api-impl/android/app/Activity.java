package android.app;

import android.app.ActionBar;
import android.atl.ATLLoadedApp;
import android.atl.annotation.Export;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageParser;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.animation.ValueAnimator;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Slog;
import android.view.ContextThemeWrapper;
import android.view.Display;
import android.view.KeyEvent;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManagerImpl;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Transformation;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Activity extends ContextThemeWrapper implements Window.Callback, LayoutInflater.Factory2 {
	private final static String TAG = "Activity";

	public static final int RESULT_CANCELED = 0;
	public static final int RESULT_OK = -1;

	Window window;
	int requested_orientation = -1 /*ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED*/; // dummy
	public Intent intent;
	private Activity resultActivity;
	private int resultRequestCode;
	private boolean paused = false;
	private boolean started = false; // between performStart() and performStop()
	private boolean stopped = false; // stopped and not started again: the next start is a restart
	private Bundle savedState; // what it saved when it was last stopped, recreate() creates the new instance from it
	private CharSequence title = null;
	List<Fragment> fragments = new ArrayList<>();
	boolean destroyed = false;
	private boolean finishing = false;

	public static Activity internalCreateActivity(String className, long native_window, Intent intent) throws ReflectiveOperationException {
		Activity activity = ATLLoadedApp.getPrimaryApplication().createActivity(className, intent);
		activity.window.set_native_window(native_window);
		return activity;
	}

	/**
	 * Helper function to be called from native code to construct main activity
	 *
	 * @param className  class name of activity or null
	 * @return  instance of main activity class
	 * @throws Exception
	 */
	@Export
	private static Activity createMainActivity(String className, long native_window, String uriString) throws ReflectiveOperationException {
		Uri uri = uriString != null ? Uri.parse(uriString) : null;
		if (className == null) {
			for (PackageParser.Activity activity : ATLLoadedApp.getPrimaryApplication().pkg.activities) {
				if (!activity.info.enabled)
					continue;
				boolean done = false;
				for (PackageParser.IntentInfo intent : activity.intents) {
					Slog.i(TAG, intent.toString());
					if ((uri == null && intent.hasCategory("android.intent.category.LAUNCHER") && intent.hasAction("android.intent.action.MAIN")) ||        // NOLINT
					    (uri != null && intent.hasDataScheme(uri.getScheme())                  && intent.hasCategory("android.intent.category.DEFAULT"))) { // NOLINT
						className = activity.info.targetActivity != null ? activity.info.targetActivity : activity.className;
						done = true;
						break;
					}
				}
				if (done)
					break;
			}
		} else {
			className = className.replace('/', '.');
		}
		if (className == null) {
			if (uri != null)
				System.err.println("Failed to find Activity to launch URI: " + uri);
			else
				System.err.println("Failed to find main Activity");
			System.exit(1);
		}
		return internalCreateActivity(className, native_window, uri != null ? new Intent("android.intent.action.VIEW", uri) : new Intent());
	}

	public Activity() {
		super(null);
	}

	public View root_view;

	public final Application getApplication() {
		return (Application)getApplicationContext();
	}

	public WindowManager getWindowManager() {
		return new WindowManagerImpl();
	}

	public String getCallingPackage() {
		return null; // [from api reference] Note: if the calling activity is not expecting a result (that is it did not use the startActivityForResult(Intent, int) form that includes a request code), then the calling package will be null.
	}

	public ComponentName getComponentName() {
		return intent.getComponent();
	}

	public Intent getIntent() {
		return intent;
	}

	public int getRequestedOrientation() {
		return requested_orientation;
	}

	public void setRequestedOrientation(int orientation) {
		requested_orientation = orientation;
	}

	public boolean isFinishing() {
		return finishing;
	}

	public final boolean requestWindowFeature(int featureId) {
		return false; // whatever feature it is, it's probably not supported
	}

	public final void setVolumeControlStream(int streamType) {}

	protected void onCreate(Bundle savedInstanceState) {
		Slog.i(TAG, "- onCreate - yay!");

		for (Fragment fragment : fragments) {
			fragment.onCreate(savedInstanceState);
		}
		dispatchActivityLifecycle(Application.CREATED, Application.MAIN, savedInstanceState);

		return;
	}

	@Export
	protected void onPostCreate(Bundle savedInstanceState) {
		Slog.i(TAG, "- onPostCreate - yay!");
		return;
	}

	protected void onStart() {
		Slog.i(TAG, "- onStart - yay!");
		window.setTitle(title);

		for (Fragment fragment : fragments) {
			fragment.onStart();
		}
		dispatchActivityLifecycle(Application.STARTED, Application.MAIN, null);

		return;
	}

	protected void onRestart() {
		Slog.i(TAG, "- onRestart - yay!");

		return;
	}

	protected void onResume() {
		Slog.i(TAG, "- onResume - yay!");

		for (Fragment fragment : fragments) {
			fragment.onResume();
		}
		dispatchActivityLifecycle(Application.RESUMED, Application.MAIN, null);

		paused = false;
		return;
	}

	@Export
	protected void onPostResume() {
		Slog.i(TAG, "- onPostResume - yay!");
		return;
	}

	protected void onPause() {
		Slog.i(TAG, "- onPause - yay!");

		for (Fragment fragment : fragments) {
			fragment.onPause();
		}
		dispatchActivityLifecycle(Application.PAUSED, Application.MAIN, null);

		paused = true;
		return;
	}

	protected void onStop() {
		Slog.i(TAG, "- onStop - yay!");

		for (Fragment fragment : fragments) {
			fragment.onStop();
		}
		dispatchActivityLifecycle(Application.STOPPED, Application.MAIN, null);

		return;
	}

	protected void onDestroy() {
		Slog.i(TAG, "- onDestroy - yay!");

		for (Fragment fragment : fragments) {
			fragment.onDestroy();
		}
		dispatchActivityLifecycle(Application.DESTROYED, Application.MAIN, null);

		destroyed = true;
		return;
	}

	public void onWindowFocusChanged(boolean hasFocus) {
		Slog.i(TAG, "- onWindowFocusChanged - yay! (hasFocus: " + hasFocus + ")");

		return;
	}

	@Export
	public void onTopResumedActivityChanged(boolean isTopResumedActivity) {}

	public void onUserInteraction() {}

	protected void onUserLeaveHint() {}

	private static final String WINDOW_HIERARCHY_TAG = "android:viewHierarchyState";

	protected void onSaveInstanceState(Bundle outState) {
		outState.putBundle(WINDOW_HIERARCHY_TAG, getWindow().saveHierarchyState());
		dispatchActivityLifecycle(Application.SAVE_INSTANCE_STATE, Application.MAIN, outState);
	}

	public void onConfigurationChanged(Configuration newConfig) {
	}

	public void onLowMemory() {
	}

	/* --- */

	public void setContentView(int layoutResID) throws Exception {
		Slog.i(TAG, "- setContentView - yay!");

		root_view = getLayoutInflater().inflate(layoutResID, null, false);

		window.setContentView(root_view);
		onContentChanged();
	}

	public void setContentView(View view, ViewGroup.LayoutParams layoutParams) {
		setContentView(view);
	}

	public void setContentView(View view) {
		window.setContentView(view);
		onContentChanged();
	}

	public <T extends android.view.View> T findViewById(int id) {
		View view = window.findViewById(id);

		return (T)view;
	}

	public void invalidateOptionsMenu() {
		Slog.i(TAG, "invalidateOptionsMenu() called, should we do something?");
	}

	public Window getWindow() {
		return this.window;
	}

	public Display getDisplay() {
		return new Display();
	}

	public final void runOnUiThread(Runnable action) {
		if (Looper.myLooper() == Looper.getMainLooper()) {
			action.run();
		} else {
			new Handler(Looper.getMainLooper()).post(action);
		}
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {}

	// the order must match GtkFileChooserAction enum
	private static final List<String> FILE_CHOOSER_ACTIONS = Arrays.asList(
	    "android.intent.action.OPEN_DOCUMENT",     // (0) GTK_FILE_CHOOSER_ACTION_OPEN
	    "android.intent.action.CREATE_DOCUMENT",   // (1) GTK_FILE_CHOOSER_ACTION_SAVE
	    "android.intent.action.OPEN_DOCUMENT_TREE" // (2) GTK_FILE_CHOOSER_ACTION_SELECT_FOLDER
	);

	// callback from native code
	@Export
	protected void fileChooserResultCallback(int requestCode, int resultCode, int action, String uri) {
		onActivityResult(requestCode, resultCode, new Intent(FILE_CHOOSER_ACTIONS.get(action), uri != null ? Uri.parse(uri) : null));
	}

	public void startActivityForResult(Intent intent, int requestCode, Bundle options) {
		Slog.i(TAG, "startActivityForResult(" + intent + ", " + requestCode + "," + options + ") called");
		if (intent.getComponent() != null) {
			try {
				final Activity activity = internalCreateActivity(intent.getComponent().getClassName(), getWindow().native_window, intent);
				activity.resultRequestCode = requestCode;
				activity.resultActivity = this;
				/* like on Android (and like Context.startActivity), the new activity starts after the
				 * caller has returned: started from onCreate(), it must come up after the caller's
				 * onResume(), not be paused by it */
				new Handler(Looper.getMainLooper()).post(new Runnable() {
					@Override
					public void run() {
						nativeStartActivity(activity);
					}
				});
			} catch (ReflectiveOperationException e) {
				onActivityResult(requestCode, 0 /*RESULT_CANCELED*/, new Intent());
			}
		} else if (FILE_CHOOSER_ACTIONS.contains(intent.getAction())) {
			nativeFileChooser(FILE_CHOOSER_ACTIONS.indexOf(intent.getAction()), intent.getType(), intent.getStringExtra("android.intent.extra.TITLE"), requestCode);
		} else if (Intent.ACTION_GET_CONTENT.equals(intent.getAction())) {
			nativeFileChooser(0 /*GTK_FILE_CHOOSER_ACTION_OPEN*/, intent.getType(), intent.getStringExtra(Intent.EXTRA_TITLE), requestCode);
		} else if ("android.intent.action.INSTALL_PACKAGE".equals(intent.getAction())) {
			try {
				Process p = new ProcessBuilder("/usr/bin/env", "android-translation-layer", "--install", intent.getData().getPath()).start();
				int exitValue = p.waitFor();
				if (exitValue == 0) {
					onActivityResult(requestCode, -1 /*RESULT_OK*/, new Intent());
				} else {
					onActivityResult(requestCode, 0 /*RESULT_CANCELED*/, new Intent());
				}
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
				onActivityResult(requestCode, 0 /*RESULT_CANCELED*/, new Intent());
			}
		} else {
			Slog.i(TAG, "startActivityForResult: intent was not handled. Calling onActivityResult(RESULT_CANCELED).");
			onActivityResult(requestCode, 0 /*RESULT_CANCELED*/, new Intent());
		}
	}
	public void startActivityForResult(Intent intent, int requestCode) {
		startActivityForResult(intent, requestCode, null);
	}

	public void setResult(int resultCode, Intent data) {
		if (resultActivity != null) {
			resultActivity.onActivityResult(resultRequestCode, resultCode, data);
		}
	}

	public void setResult(int resultCode) {
		setResult(resultCode, null);
	}

	protected Dialog onCreateDialog(int id) {
		Slog.i(TAG, "Activity.onCreateDialog(" + id + ") called");
		return null;
	}

	protected void onPrepareDialog(int id, Dialog dialog) {
		Slog.i(TAG, "Activity.onPrepareDialog(" + id + ") called");
	}

	private Map<Integer, Dialog> dialogs = new HashMap<Integer, Dialog>();

	public final void showDialog(int id) {
		Slog.i(TAG, "Activity.showDialog(" + id + ") called");
		Dialog dialog = dialogs.get(id);
		if (dialog == null)
			dialogs.put(id, dialog = onCreateDialog(id));
		if (dialog == null) {
			Slog.w(TAG, "Dialog " + id + " was not created");
			return;
		}
		onPrepareDialog(id, dialog);
		dialog.show();
	}

	public boolean showDialog(int id, Bundle args) {
		return false;
	}

	public void removeDialog(int id) {
		Dialog dialog = dialogs.remove(id);
		if (dialog != null)
			dialog.dismiss();
	}

	public void finish() {
		finishing = true;
		new Handler(Looper.getMainLooper()).post(new Runnable() {
			@Override
			public void run() {
				if (window != null && window.native_window != 0) {
					nativeFinish(getWindow().native_window);
					window.native_window = 0;
				}
			}
		});
	}

	/* like on AOSP, what the previous instance returned from onRetainNonConfigurationInstance() when it was
	 * recreated; AndroidX keeps the ViewModels of the activity and its fragments in it */
	private Object lastNonConfigurationInstance;

	public Object getLastNonConfigurationInstance() {
		return lastNonConfigurationInstance;
	}

	public Object onRetainNonConfigurationInstance() {
		return null;
	}

	/* called from native code by recreate(), after the state was saved and before this instance is destroyed */
	@Export
	private void retainNonConfigurationInstanceFor(Activity next) {
		next.lastNonConfigurationInstance = onRetainNonConfigurationInstance();
	}

	public FragmentManager getFragmentManager() {
		return new FragmentManager(this);
	}

	public LayoutInflater getLayoutInflater() {
		return (LayoutInflater)getSystemService("layout_inflater");
	}

	private boolean changingConfigurations;

	/* true while recreate() replaces this instance: AndroidX keeps the ViewModels then */
	public boolean isChangingConfigurations() { return changingConfigurations; }

	@Override
	public void onContentChanged() {
		Slog.i(TAG, "- onContentChanged - yay!");
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		return true;
	}

	@Override
	public boolean onCreatePanelMenu(int featureId, Menu menu) {
		if (featureId == Window.FEATURE_OPTIONS_PANEL) {
			// HACK: catch non critical error occuring in Open Sudoku app
			try {
				return onCreateOptionsMenu(menu);
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		}
		return false;
	}

	@Override
	public View onCreatePanelView(int featureId) {
		return null;
	}

	public MenuInflater getMenuInflater() {
		return new MenuInflater(this);
	}

	public boolean onPrepareOptionsMenu(Menu menu) {
		return true;
	}

	@Override
	public boolean onPreparePanel(int featureId, View view, Menu menu) {
		if (featureId == Window.FEATURE_OPTIONS_PANEL && menu != null) {
			return onPrepareOptionsMenu(menu);
		}
		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		if (featureId == Window.FEATURE_OPTIONS_PANEL) {
			return onOptionsItemSelected(item);
		}
		return false;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		return false;
	}

	public void onOptionsMenuClosed(Menu menu) {}

	@Override
	public void onPanelClosed(int featureId, Menu menu) {
		if (featureId == Window.FEATURE_OPTIONS_PANEL) {
			onOptionsMenuClosed(menu);
		}
	}

	public void setTitle(CharSequence title) {
		this.title = title;
	}

	public void setTitle(int titleId) {
		this.title = getText(titleId);
	}

	public CharSequence getTitle() {
		return title;
	}

	@Export
	public void onBackPressed() {
		System.out.println("onBackPressed() called");
		finish();
	}

	public android.window.OnBackInvokedDispatcher getOnBackInvokedDispatcher() {
		return getWindow().getOnBackInvokedDispatcher();
	}

	/* called from native code for the back button before onBackPressed(): callbacks
	 * registered with the OnBackInvokedDispatcher come first, as on Android 13+ */
	@Export
	private boolean dispatchOnBackInvoked() {
		return ((android.window.WindowOnBackInvokedDispatcher)getOnBackInvokedDispatcher()).dispatchOnBackInvoked();
	}

	public void setIntent(Intent newIntent) {
		this.intent = newIntent;
	}

	public Intent getParentActivityIntent() {
		return null;
	}

	@Override
	public boolean onMenuOpened(int featureId, Menu menu) {
		Slog.i(TAG, "onMenuOpened(" + featureId + ", " + menu + ") called");
		return false;
	}

	/* like AOSP: the activity is stopped, saves its state and is destroyed, a new instance of
	 * it is created from that state in its place in the back stack */
	public void recreate() {
		changingConfigurations = true;
		try {
			Activity activity = internalCreateActivity(this.getClass().getName(), getWindow().native_window, intent);
			activity.resultRequestCode = resultRequestCode;
			activity.resultActivity = resultActivity;
			new Handler(Looper.getMainLooper()).post(new Runnable() {
				@Override
				public void run() {
					nativeRecreate(activity);
				}
			});
		} catch (ReflectiveOperationException e) {
			Slog.i(TAG, "exception in Activity.recreate, this is kinda sus");
			e.printStackTrace();
		}
	}

	/* Called by the decor view when the window was resized or (un)maximized. Like Android, the screen
	 * size of the configuration follows the window. An activity that handles the change itself (its
	 * configChanges) gets onConfigurationChanged(); otherwise it is relaunched if the new size crosses
	 * a size that the app has resources for (w<N>dp, h<N>dp), else only the resources follow. */
	public void internalWindowSizeChanged() {
		Resources resources = getResources();
		Configuration oldConfig = new Configuration(resources.getConfiguration());
		native_updateConfigFromWindow(getWindow().native_window, sys_config);
		Configuration newConfig = new Configuration(oldConfig);
		newConfig.screenWidthDp = sys_config.screenWidthDp;
		newConfig.screenHeightDp = sys_config.screenHeightDp;
		int diff = oldConfig.diff(newConfig);
		if (diff == 0)
			return;
		DisplayMetrics metrics = resources.getDisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		resources.updateConfiguration(newConfig, metrics);
		Resources appResources = getApplicationContext().getResources();
		if (appResources != resources)
			appResources.updateConfiguration(newConfig, metrics);

		int handled = 0;
		try {
			handled = getPackageManager().getActivityInfo(getComponentName(), 0).getRealConfigChanged();
		} catch (PackageManager.NameNotFoundException e) {
		}
		if ((diff & ~handled) == 0)
			onConfigurationChanged(new Configuration(newConfig));
		else if (crossesSizeThreshold(oldConfig, newConfig))
			recreate();
	}

	private boolean crossesSizeThreshold(Configuration a, Configuration b) {
		for (Configuration size : getAssets().getSizeConfigurations()) {
			if (crosses(size.screenWidthDp, a.screenWidthDp, b.screenWidthDp) || crosses(size.screenHeightDp, a.screenHeightDp, b.screenHeightDp))
				return true;
		}
		return false;
	}

	private static boolean crosses(int threshold, int a, int b) {
		return threshold != Configuration.SCREEN_WIDTH_DP_UNDEFINED && (a >= threshold) != (b >= threshold);
	}

	private Bundle performSaveInstanceState() {
		Bundle outState = new Bundle();
		dispatchActivityLifecycle(Application.SAVE_INSTANCE_STATE, Application.PRE, outState);
		onSaveInstanceState(outState);
		dispatchActivityLifecycle(Application.SAVE_INSTANCE_STATE, Application.POST, outState);
		return outState;
	}

	@Export
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		Bundle windowState = savedInstanceState.getBundle(WINDOW_HIERARCHY_TAG);
		if (windowState != null)
			getWindow().restoreHierarchyState(windowState);
	}

	public String getLocalClassName() {
		final String pkg = getPackageName();
		final String cls = this.getClass().getName();
		int packageLen = pkg.length();
		if (!cls.startsWith(pkg) || cls.length() <= packageLen || cls.charAt(packageLen) != '.') {
			return cls;
		}
		return cls.substring(packageLen + 1);
	}

	public SharedPreferences getPreferences(int mode) {
		return getSharedPreferences(getLocalClassName(), mode);
	}

	@Export
	protected void onNewIntent(Intent intent) {}

	public final Activity getParent() {
		return null;
	}

	public boolean hasWindowFocus() {
		return true; // FIXME?
	}

	public boolean isDestroyed() {
		return destroyed;
	}

	public void finishAffinity() {
		finish();
	}

	/* animations of the next activity change, set by overridePendingTransition() */
	private static boolean pendingTransitionSet;
	private static int pendingEnterAnim;
	private static int pendingExitAnim;

	public void overridePendingTransition(int enterAnim, int exitAnim) {
		pendingTransitionSet = true;
		pendingEnterAnim = enterAnim;
		pendingExitAnim = exitAnim;
	}

	// GtkStackTransitionType
	private static final int GTK_STACK_TRANSITION_NONE = 0;
	private static final int GTK_STACK_TRANSITION_CROSSFADE = 1;
	private static final int GTK_STACK_TRANSITION_OVER_UP = 8;
	private static final int GTK_STACK_TRANSITION_OVER_DOWN = 9;
	private static final int GTK_STACK_TRANSITION_OVER_LEFT = 10;
	private static final int GTK_STACK_TRANSITION_OVER_RIGHT = 11;
	private static final int GTK_STACK_TRANSITION_UNDER_UP = 12;
	private static final int GTK_STACK_TRANSITION_UNDER_DOWN = 13;
	private static final int GTK_STACK_TRANSITION_UNDER_LEFT = 14;
	private static final int GTK_STACK_TRANSITION_UNDER_RIGHT = 15;

	/*
	 * ATL shows the activities as pages of a GtkStack. Called from native code when the stack
	 * changes to another activity: returns the GtkStack transition (type | duration << 8) that
	 * resembles the window animations of this change. The animations come from
	 * overridePendingTransition() or else from the windowAnimationStyle of the theme (AOSP:
	 * activityOpenEnterAnimation and the like). An activity sliding in or out over at least a quarter
	 * of the window becomes a GTK slide, a change of alpha a cross-fade; smaller movements (the 96dp
	 * of the Android 14 default) are left out.
	 */
	@Export
	private static int getStackTransition(Activity entering, Activity leaving, boolean opening, int width, int height) {
		boolean set = pendingTransitionSet;
		pendingTransitionSet = false;
		Activity themed = opening ? entering : leaving;
		if (themed == null || entering == null || leaving == null || ValueAnimator.getDurationScale() == 0)
			return GTK_STACK_TRANSITION_NONE;
		int enterAnim = pendingEnterAnim;
		int exitAnim = pendingExitAnim;
		if (!set) {
			TypedArray a = themed.getTheme().obtainStyledAttributes(com.android.internal.R.styleable.Window);
			int style = a.getResourceId(com.android.internal.R.styleable.Window_windowAnimationStyle, 0);
			a.recycle();
			if (style == 0)
				return GTK_STACK_TRANSITION_NONE;
			a = themed.getTheme().obtainStyledAttributes(style, com.android.internal.R.styleable.WindowAnimation);
			enterAnim = a.getResourceId(opening ? com.android.internal.R.styleable.WindowAnimation_activityOpenEnterAnimation
			                                    : com.android.internal.R.styleable.WindowAnimation_activityCloseEnterAnimation, 0);
			exitAnim = a.getResourceId(opening ? com.android.internal.R.styleable.WindowAnimation_activityOpenExitAnimation
			                                   : com.android.internal.R.styleable.WindowAnimation_activityCloseExitAnimation, 0);
			a.recycle();
		}
		Animation enter = loadWindowAnimation(themed, enterAnim, width, height);
		Animation exit = loadWindowAnimation(themed, exitAnim, width, height);
		// {dx, dy, alpha} where the entering activity starts and where the leaving one ends
		float[] enterFrom = enter != null ? sampleWindowAnimation(enter, 0) : null;
		float[] exitTo = exit != null ? sampleWindowAnimation(exit, exit.computeDurationHint()) : null;
		int type = GTK_STACK_TRANSITION_NONE;
		Animation moving = null;
		if (enterFrom != null && Math.abs(enterFrom[1]) >= height / 4) {
			type = enterFrom[1] > 0 ? GTK_STACK_TRANSITION_OVER_UP : GTK_STACK_TRANSITION_OVER_DOWN;
			moving = enter;
		} else if (enterFrom != null && Math.abs(enterFrom[0]) >= width / 4) {
			type = enterFrom[0] > 0 ? GTK_STACK_TRANSITION_OVER_LEFT : GTK_STACK_TRANSITION_OVER_RIGHT;
			moving = enter;
		} else if (exitTo != null && Math.abs(exitTo[1]) >= height / 4) {
			type = exitTo[1] > 0 ? GTK_STACK_TRANSITION_UNDER_DOWN : GTK_STACK_TRANSITION_UNDER_UP;
			moving = exit;
		} else if (exitTo != null && Math.abs(exitTo[0]) >= width / 4) {
			type = exitTo[0] > 0 ? GTK_STACK_TRANSITION_UNDER_RIGHT : GTK_STACK_TRANSITION_UNDER_LEFT;
			moving = exit;
		}
		long duration;
		stackScrim = 0;
		if (moving != null) {
			duration = moving.computeDurationHint();
			/* the activity that stays in place fades over the black window background (the
			 * fade_out under a sliding-in activity, the fade_in under a sliding-out one): GtkStack
			 * can't fade it, the native side draws the matching black scrim */
			Animation still = moving == enter ? exit : enter;
			if (still != null) {
				float from = sampleWindowAnimation(still, 0)[2];
				float to = sampleWindowAnimation(still, duration)[2];
				if (from < 1 || to < 1)
					stackScrim = 1 << 17 | (moving == exit ? 1 << 16 : 0) | Math.round((1 - from) * 255) | Math.round((1 - to) * 255) << 8;
			}
		} else {
			// cross-fade for as long as the alpha of either animation changes
			duration = Math.max(enter != null ? alphaDuration(enter) : 0, exit != null ? alphaDuration(exit) : 0);
			if (duration > 0)
				type = GTK_STACK_TRANSITION_CROSSFADE;
		}
		duration = (long)(duration * ValueAnimator.getDurationScale());
		return type | (int)Math.min(duration, 5000) << 8;
	}

	/* for the native side, set by getStackTransition(): bit 17 set if there is a scrim, bit 16 if
	 * it dims the entering activity (else the leaving one), bits 0-7/8-15 its alpha at the start/end */
	private static int stackScrim;

	private static Animation loadWindowAnimation(Context context, int id, int width, int height) {
		if (id == 0)
			return null;
		try {
			Animation animation = AnimationUtils.loadAnimation(context, id);
			animation.initialize(width, height, width, height);
			return animation;
		} catch (RuntimeException e) {
			Slog.w(TAG, "cannot load window animation 0x" + Integer.toHexString(id) + ": " + e);
			return null;
		}
	}

	private static float[] sampleWindowAnimation(Animation animation, long time) {
		Transformation t = new Transformation();
		animation.setStartTime(0);
		animation.getTransformation(time, t);
		float[] values = new float[9];
		t.getMatrix().getValues(values);
		return new float[] {values[Matrix.MTRANS_X], values[Matrix.MTRANS_Y], t.getAlpha()};
	}

	private static long alphaDuration(Animation animation) {
		long end = animation.computeDurationHint();
		float last = sampleWindowAnimation(animation, end)[2];
		long time = end;
		while (time > 0 && Math.abs(sampleWindowAnimation(animation, time - 10)[2] - last) < 0.01f)
			time -= 10;
		return sampleWindowAnimation(animation, 0)[2] == last && time == 0 ? 0 : time;
	}

	public native boolean isTaskRoot();

	public void postponeEnterTransition() {}

	public void startPostponedEnterTransition() {}

	public boolean isChild() {
		return false;
	}

	public void setTaskDescription(ActivityManager.TaskDescription description) {}

	private native void nativeFinish(long native_window);
	public static native void nativeStartActivity(Activity activity);
	private native void nativeRecreate(Activity activity);
	public static native boolean nativeResumeActivity(Class<? extends Activity> activityClass, Intent intent);
	public static native void nativeOpenURI(String uri);
	public native void nativeFileChooser(int action, String type, String title, int requestCode);
	public void reportFullyDrawn() {}
	public void setVisible(boolean visible) {}
	public Uri getReferrer() { return null; }
	public void setDefaultKeyMode(int flag) {}
	public void registerForContextMenu(View view) {}
	public native boolean isInMultiWindowMode();

	// we never put an activity into picture-in-picture, so this is always false
	public boolean isInPictureInPictureMode() { return false; }

	private final ArrayList<Application.ActivityLifecycleCallbacks> activityLifecycleCallbacks = new ArrayList<>();

	/* API 29: callbacks for this activity only, on top of those of the Application */
	public void registerActivityLifecycleCallbacks(Application.ActivityLifecycleCallbacks callback) {
		synchronized (activityLifecycleCallbacks) {
			activityLifecycleCallbacks.add(callback);
		}
	}

	public void unregisterActivityLifecycleCallbacks(Application.ActivityLifecycleCallbacks callback) {
		synchronized (activityLifecycleCallbacks) {
			activityLifecycleCallbacks.remove(callback);
		}
	}

	/* like AOSP: while the activity comes up, the Application's callbacks go first (except for the
	 * post events), while it goes down the activity's own go first and in reverse order */
	private void dispatchActivityLifecycle(int event, int phase, Bundle bundle) {
		Object[] callbacks;
		synchronized (activityLifecycleCallbacks) {
			callbacks = activityLifecycleCallbacks.toArray();
		}
		Application app = getApplication();
		boolean down = event >= Application.PAUSED;
		boolean appFirst = !down && phase != Application.POST;
		if (appFirst && app != null)
			app.dispatchActivityLifecycle(event, phase, this, bundle);
		for (int i = 0; i < callbacks.length; i++)
			Application.callActivityLifecycle((Application.ActivityLifecycleCallbacks)callbacks[down ? callbacks.length - 1 - i : i], event, phase, this, bundle);
		if (!appFirst && app != null)
			app.dispatchActivityLifecycle(event, phase, this, bundle);
	}

	/* called from native code instead of onCreate() etc., with the pre and post callbacks around them */
	@Export
	private void performCreate(Bundle savedInstanceState) {
		dispatchActivityLifecycle(Application.CREATED, Application.PRE, savedInstanceState);
		onCreate(savedInstanceState);
		dispatchActivityLifecycle(Application.CREATED, Application.POST, savedInstanceState);
	}

	@Export
	private void performStart() {
		dispatchActivityLifecycle(Application.STARTED, Application.PRE, null);
		onStart();
		dispatchActivityLifecycle(Application.STARTED, Application.POST, null);
		started = true;
	}

	/* like AOSP: a stopped activity that comes back gets onRestart() before onStart() */
	@Export
	private void performRestart() {
		stopped = false;
		onRestart();
		performStart();
	}

	@Export
	private void performResume() {
		dispatchActivityLifecycle(Application.RESUMED, Application.PRE, null);
		onResume();
		dispatchActivityLifecycle(Application.RESUMED, Application.POST, null);
	}

	@Export
	private void performPause() {
		dispatchActivityLifecycle(Application.PAUSED, Application.PRE, null);
		onPause();
		dispatchActivityLifecycle(Application.PAUSED, Application.POST, null);
	}

	/* like AOSP's callActivityOnStop(): unless it is finishing, the activity saves its state when it is
	 * stopped, after onStop() for apps targeting API 28 and later, before it for older ones */
	@Export
	private void performStop(boolean saveState) {
		saveState = saveState && !finishing;
		boolean preP = getApplicationInfo().targetSdkVersion < 28;
		if (saveState && preP)
			savedState = performSaveInstanceState();
		dispatchActivityLifecycle(Application.STOPPED, Application.PRE, null);
		onStop();
		dispatchActivityLifecycle(Application.STOPPED, Application.POST, null);
		started = false;
		stopped = true;
		if (saveState && !preP)
			savedState = performSaveInstanceState();
	}

	/* like AOSP: when another activity is started by the user, the one it covers is told before it is paused */
	@Export
	private void performUserLeaving() {
		onUserInteraction();
		onUserLeaveHint();
	}

	@Export
	private void performDestroy() {
		dispatchActivityLifecycle(Application.DESTROYED, Application.PRE, null);
		onDestroy();
		dispatchActivityLifecycle(Application.DESTROYED, Application.POST, null);
		Dialog.closeWindowsOf(this);
		if (window != null)
			window.killViewTreeObserver();
	}

	public void setDisablePreviewScreenshots(boolean disable) {}
	public final View requireViewById(int id) {
		View view = findViewById(id);
		if (view == null)
			throw new IllegalArgumentException("ID does not reference a View inside this View");
		return view;
	}

	public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
		return null;
	}

	public boolean onSearchRequested() {
		return false;
	}

	public View getCurrentFocus() {
		return null;
	}

	public void setProgressBarIndeterminateVisibility(boolean indeterminate) {}

	public int getChangingConfigurations() {
		return 0;
	}

	public int getTaskId() {
		/* we don't support multiple activity stacks, so this is probably fine? */
		return System.identityHashCode(this.getApplicationContext());
	}

	boolean moveTaskToBack(boolean nonroot) {
		return true;
	}

	void setFinishOnTouchOutside(boolean finish) {
	}

	public void closeOptionsMenu() {
	}

	public void finishActivity(int requestCode) {
		/* TODO: track started activities so we can finish the right one here */
		Slog.w(TAG, "finishActivity: stub");
	}

	public void finishAfterTransition() {
		finish();
	}

	public boolean dispatchKeyEvent(KeyEvent event) {
		return false;
	}

	/* the remaining Window.Callback methods; input reaches the views through GTK, so there is
	 * nothing to dispatch here, and there is no action bar to start an action mode in */

	@Override
	public boolean dispatchKeyShortcutEvent(KeyEvent event) {
		return false;
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent event) {
		return false;
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
	public void onWindowAttributesChanged(WindowManager.LayoutParams params) {}

	@Override
	public void onAttachedToWindow() {}

	@Override
	public void onDetachedFromWindow() {}

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

	/**
	 * Navigates up to the activity of upIntent if it is in the back stack: the activities above it
	 * are finished and it receives upIntent in onNewIntent(). There is only one task, so
	 * shouldUpRecreateTask() is always false.
	 */
	public boolean navigateUpTo(Intent upIntent) {
		ComponentName target = upIntent.getComponent();
		if (target == null)
			return false;
		try {
			return nativeResumeActivity(getClassLoader().loadClass(target.getClassName()).asSubclass(Activity.class), upIntent);
		} catch (ClassNotFoundException e) {
			return false;
		}
	}

	public boolean shouldUpRecreateTask(Intent targetIntent) {
		return false;
	}

	public void requestPermissions(String[] permissions, int requestCode) {
		Slog.w(TAG, "requestPermissions(" + Arrays.toString(permissions) + "): not handled");
	}

	public boolean shouldShowRequestPermissionRationale(String permission) {
		return true;
	}

	public ActionBar getActionBar() {
		return null;
	}

	@Override
	public void atl_attach_base_context(Context baseContext) {
		super.atl_attach_base_context(baseContext);
		this.window = new Window(this, this);
	}

	public void applyOverrideConfiguration(Configuration overrideConfiguration) {}

	public ComponentName getCallingActivity() {
		return resultActivity == null ? null : resultActivity.getComponentName();
	}

	public boolean onNavigateUp() {
		return false;
	}

	public void setExitSharedElementCallback(SharedElementCallback callback) {}

	public void setEnterSharedElementCallback(SharedElementCallback callback) {}
}
