package android.app;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.ActionBar;
import android.atl.ATLLoadedApp;
import android.atl.annotation.Export;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageParser;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.animation.ValueAnimator;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.text.TextUtils;
import android.util.Log;
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
import android.window.SizeConfigurationBuckets;
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
	private Activity resultActivity; // the activity that started this one with startActivityForResult()
	private int resultRequestCode;
	/* like AOSP: what setResult() set, handed over by finish() */
	private int mResultCode = RESULT_CANCELED;
	private Intent mResultData;
	/* the results of the activities this one started that finished, delivered before it comes back
	 * (AOSP: ActivityResultItem, ActivityThread.handleSendResult()) */
	private ArrayList<ResultInfo> mPendingResults;

	private static final class ResultInfo {
		final int requestCode;
		final int resultCode;
		final Intent data;
		ResultInfo(int requestCode, int resultCode, Intent data) {
			this.requestCode = requestCode;
			this.resultCode = resultCode;
			this.data = data;
		}
	}
	private boolean paused = false;
	private boolean started = false; // between performStart() and performStop()
	private boolean stopped = false; // stopped and not started again: the next start is a restart
	private Bundle savedState; // what it saved when it was last stopped, recreate() creates the new instance from it
	private CharSequence title = null;
	List<Fragment> fragments = new ArrayList<>();
	boolean destroyed = false;
	private boolean finishing = false;
	private SizeConfigurationBuckets mSizeConfigurations;

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
		super();
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
		dispatchActivityCreated(savedInstanceState);

		return;
	}

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
		dispatchActivityStarted();

		return;
	}

	protected void onRestart() {
		Slog.i(TAG, "- onRestart - yay!");

		return;
	}

	protected void onResume() {
		android.atl.ATLViewDump.resumed(this); // debugging aid (atl-doctor)
		Slog.i(TAG, "- onResume - yay!");

		for (Fragment fragment : fragments) {
			fragment.onResume();
		}
		dispatchActivityResumed();

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
		dispatchActivityPaused();

		paused = true;
		return;
	}

	protected void onStop() {
		Slog.i(TAG, "- onStop - yay!");

		for (Fragment fragment : fragments) {
			fragment.onStop();
		}
		dispatchActivityStopped();

		return;
	}

	protected void onDestroy() {
		Slog.i(TAG, "- onDestroy - yay!");

		for (Fragment fragment : fragments) {
			fragment.onDestroy();
		}
		dispatchActivityDestroyed();

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
		dispatchActivitySaveInstanceState(outState);
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

	/* Activity.dispatchActivityResult(). ATL: only the results of activities (who == null); there are no
	 * results for fragments, permission requests, views or autofill */
	void dispatchActivityResult(String who, int requestCode, int resultCode, Intent data, String reason) {
		onActivityResult(requestCode, resultCode, data);
	}

	/* what finish() of the started activity does in ActivityRecord.finishIfPossible(): queue the result for
	 * the activity that started it */
	private void addPendingResult(int requestCode, int resultCode, Intent data) {
		synchronized (this) {
			if (mPendingResults == null)
				mPendingResults = new ArrayList<ResultInfo>();
			mPendingResults.add(new ResultInfo(requestCode, resultCode, data));
		}
	}

	/* called from native code when this activity comes back (before onRestart()/onStart()/onResume()):
	 * ActivityThread.deliverResults() */
	@Export
	private void performDeliverResults() {
		ArrayList<ResultInfo> results;
		synchronized (this) {
			results = mPendingResults;
			mPendingResults = null;
		}
		if (results == null)
			return;
		for (ResultInfo ri : results) {
			try {
				dispatchActivityResult(null, ri.requestCode, ri.resultCode, ri.data, "ACTIVITY_RESULT");
			} catch (Exception e) {
				throw new RuntimeException("Failure delivering result " + ri.resultCode + " (request " + ri.requestCode
				                           + ") to activity " + getComponentName().toShortString() + ": " + e.toString(), e);
			}
		}
	}

	// the order must match GtkFileChooserAction enum
	private static final List<String> FILE_CHOOSER_ACTIONS = Arrays.asList(
	    "android.intent.action.OPEN_DOCUMENT",     // (0) GTK_FILE_CHOOSER_ACTION_OPEN
	    "android.intent.action.CREATE_DOCUMENT",   // (1) GTK_FILE_CHOOSER_ACTION_SAVE
	    "android.intent.action.OPEN_DOCUMENT_TREE" // (2) GTK_FILE_CHOOSER_ACTION_SELECT_FOLDER
	);

	// callback from native code
	@Export
	protected void fileChooserResultCallback(int requestCode, int resultCode, int action, String uri) {
		dispatchActivityResult(null, requestCode, resultCode, new Intent(FILE_CHOOSER_ACTIONS.get(action), uri != null ? Uri.parse(uri) : null), "fileChooserResult");
	}

	// callback from native code; no uris: the user cancelled
	@Export
	protected void pickImagesResultCallback(int requestCode, int maxItems, String[] uris) {
		if (uris == null || uris.length == 0)
			dispatchActivityResult(null, requestCode, RESULT_CANCELED, null, "pickImagesResult");
		else
			dispatchActivityResult(null, requestCode, RESULT_OK, PhotoPicker.getPickerResponseIntent(maxItems, uris), "pickImagesResult");
	}

	public void startActivityForResult(Intent intent, int requestCode, Bundle options) {
		Slog.i(TAG, "startActivityForResult(" + intent + ", " + requestCode + "," + options + ") called");
		if (intent.getComponent() != null) {
			/* like on Android (and like Context.startActivity), the new activity starts after the
			 * caller has returned: started from onCreate(), it must come up after the caller's
			 * onResume(), not be paused by it; and like AOSP, the caller is paused before the new
			 * activity is created */
			new Handler(Looper.getMainLooper()).post(new Runnable() {
				@Override
				public void run() {
					nativePauseCurrentActivity(intent);
					final Activity activity;
					try {
						activity = internalCreateActivity(intent.getComponent().getClassName(), getWindow().native_window, intent);
					} catch (ReflectiveOperationException e) {
						nativeResumeCurrentActivity();
						onActivityResult(requestCode, 0 /*RESULT_CANCELED*/, new Intent());
						return;
					}
					/* like AOSP, a negative request code asks for no result */
					if (requestCode >= 0) {
						activity.resultRequestCode = requestCode;
						activity.resultActivity = Activity.this;
					}
					nativeStartActivity(activity);
				}
			});
		} else if (FILE_CHOOSER_ACTIONS.contains(intent.getAction())) {
			nativeFileChooser(FILE_CHOOSER_ACTIONS.indexOf(intent.getAction()), intent.getType(), intent.getStringExtra("android.intent.extra.TITLE"), requestCode);
		} else if (MediaStore.ACTION_PICK_IMAGES.equals(intent.getAction())) {
			/* on Android, MediaProvider's photo picker (PhotoPickerActivity) answers it; ATL shows the file dialog */
			String[] mimeTypes;
			int maxItems;
			try {
				mimeTypes = PhotoPicker.getMimeTypeFilters(intent);
				maxItems = PhotoPicker.getMaxSelectionLimit(intent);
			} catch (IllegalArgumentException e) {
				// PhotoPickerActivity.onCreate(): finish with RESULT_CANCELED
				Slog.e(TAG, "Finish activity due to an exception while parsing extras: " + e);
				dispatchActivityResult(null, requestCode, RESULT_CANCELED, null, "pickImagesResult");
				return;
			}
			nativePickImages(mimeTypes, maxItems, requestCode);
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

	public final void setResult(int resultCode) {
		synchronized (this) {
			mResultCode = resultCode;
			mResultData = null;
		}
	}

	public final void setResult(int resultCode, Intent data) {
		synchronized (this) {
			mResultCode = resultCode;
			mResultData = data;
		}
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

	/* Activity.finish(): the result of setResult() goes to the activity that started this one, which gets it
	 * in onActivityResult() when it comes back (ActivityClient.finishActivity()); a second finish() changes
	 * nothing */
	public void finish() {
		int resultCode;
		Intent resultData;
		synchronized (this) {
			resultCode = mResultCode;
			resultData = mResultData;
		}
		final boolean wasFinishing = finishing;
		finishing = true;
		if (!wasFinishing && resultActivity != null)
			resultActivity.addPendingResult(resultRequestCode, resultCode, resultData);
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

	/* Activity.NonConfigurationInstances: what the previous instance retained when it was recreated.
	 * ATL has no loaders and no fragment manager state to retain, and no voice interactor */
	static final class NonConfigurationInstances {
		Object activity;
		HashMap<String, Object> children;
	}
	NonConfigurationInstances mLastNonConfigurationInstances;

	public Object getLastNonConfigurationInstance() {
		return mLastNonConfigurationInstances != null
		    ? mLastNonConfigurationInstances.activity : null;
	}

	public Object onRetainNonConfigurationInstance() {
		return null;
	}

	HashMap<String, Object> getLastNonConfigurationChildInstances() {
		return mLastNonConfigurationInstances != null
		    ? mLastNonConfigurationInstances.children : null;
	}

	HashMap<String, Object> onRetainNonConfigurationChildInstances() {
		return null;
	}

	/* called from native code by recreate() before the instance is destroyed */
	@Export
	NonConfigurationInstances retainNonConfigurationInstances() {
		Object activity = onRetainNonConfigurationInstance();
		HashMap<String, Object> children = onRetainNonConfigurationChildInstances();

		if (activity == null && children == null) {
			return null;
		}

		NonConfigurationInstances nci = new NonConfigurationInstances();
		nci.activity = activity;
		nci.children = children;
		return nci;
	}

	/* called from native code by recreate() once this instance is destroyed: like AOSP's
	 * handleRelaunchActivity(), the new instance is created after the old one is gone, and gets what
	 * the old one retained (retainNonConfigurationInstances(), before it was destroyed): it is the
	 * lastNonConfigurationInstances argument of Activity.attach() */
	@Export
	private Activity createRelaunched(Object lastNonConfigurationInstances) {
		try {
			Activity activity = internalCreateActivity(this.getClass().getName(), getWindow().native_window, intent);
			activity.resultRequestCode = resultRequestCode;
			activity.resultActivity = resultActivity;
			activity.mLastNonConfigurationInstances = (NonConfigurationInstances)lastNonConfigurationInstances;
			return activity;
		} catch (ReflectiveOperationException e) {
			Slog.i(TAG, "exception in Activity.recreate, this is kinda sus");
			e.printStackTrace();
			return null;
		}
	}

	public FragmentManager getFragmentManager() {
		return new FragmentManager(this);
	}

	public LayoutInflater getLayoutInflater() {
		return (LayoutInflater)getSystemService("layout_inflater");
	}

	/* true if the activity is being destroyed in order to recreate it with a new configuration (AndroidX
	 * keeps the ViewModels then) */
	boolean mChangingConfigurations = false;
	/* the configuration changes that made recreate() relaunch the activity */
	int mConfigChangeFlags;

	public boolean isChangingConfigurations() {
		return mChangingConfigurations;
	}

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

	@Override
	public boolean onMenuOpened(int featureId, Menu menu) {
		Slog.i(TAG, "onMenuOpened(" + featureId + ", " + menu + ") called");
		return false;
	}

	/* like AOSP: the activity is stopped, saves its state and is destroyed, a new instance of
	 * it is created from that state in its place in the back stack */
	public void recreate() {
		recreate(0);
	}

	/* the relaunch of the activity for a configuration change (ActivityThread.handleRelaunchActivity()
	 * with configChanges) */
	private void recreate(int configChanges) {
		mConfigChangeFlags |= configChanges;
		mChangingConfigurations = true;
		new Handler(Looper.getMainLooper()).post(new Runnable() {
			@Override
			public void run() {
				nativeRecreate();
			}
		});
	}

	/* Called by the decor view when the window was resized or (un)maximized. Like Android, the screen
	 * size of the configuration follows the window. As in ActivityRecord.getConfigurationChanges() and
	 * shouldRelaunchLocked(), the activity is relaunched if the change minus the size changes that cross
	 * no size bucket of the app (SizeConfigurationBuckets) is not handled by its configChanges; otherwise
	 * the resources follow and ActivityThread.shouldReportChange() decides on onConfigurationChanged(). */
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
		/* ActivityThread.reportSizeConfigurations() */
		if (mSizeConfigurations == null)
			mSizeConfigurations = new SizeConfigurationBuckets(resources.getSizeConfigurations());
		int changes = SizeConfigurationBuckets.filterDiff(diff, oldConfig, newConfig, mSizeConfigurations);
		if ((changes & ~handled) != 0)
			recreate(changes & ~handled);
		else if (shouldReportChange(oldConfig, newConfig, mSizeConfigurations, handled))
			onConfigurationChanged(new Configuration(newConfig));
		mCurrentConfig = new Configuration(newConfig);
	}

	/* Activity.mCurrentConfig: the configuration the activity was created with or last told about */
	private Configuration mCurrentConfig;

	/* ActivityRecord.ensureActivityConfiguration() with shouldRelaunchLocked(), after a change of the global
	 * configuration (ActivityThread.handleConfigurationChanged()): for the shown activity right away, for
	 * the others when they come back. The activity is relaunched if its configChanges do not handle the
	 * change, otherwise ActivityThread.shouldReportChange() decides on onConfigurationChanged(). */
	@Export
	void ensureActivityConfiguration() {
		if (mCurrentConfig == null || finishing || mChangingConfigurations)
			return;
		Resources resources = getResources();
		Configuration newConfig = new Configuration(resources.getConfiguration());
		int diff = mCurrentConfig.diff(newConfig);
		if (diff == 0)
			return;
		Configuration oldConfig = mCurrentConfig;
		mCurrentConfig = newConfig;
		int handled = 0;
		try {
			handled = getPackageManager().getActivityInfo(getComponentName(), 0).getRealConfigChanged();
		} catch (PackageManager.NameNotFoundException e) {
		}
		if (mSizeConfigurations == null)
			mSizeConfigurations = new SizeConfigurationBuckets(resources.getSizeConfigurations());
		int changes = SizeConfigurationBuckets.filterDiff(diff, oldConfig, newConfig, mSizeConfigurations);
		if ((changes & ~handled) != 0)
			recreate(changes & ~handled);
		else if (shouldReportChange(oldConfig, newConfig, mSizeConfigurations, handled))
			onConfigurationChanged(new Configuration(newConfig));
	}

	/* ActivityThread.shouldReportChange(), without the window bounds and alwaysReportChange */
	private static boolean shouldReportChange(Configuration currentConfig, Configuration newConfig,
						  SizeConfigurationBuckets sizeBuckets, int handledConfigChanges) {
		final int publicDiff = currentConfig.diff(newConfig);
		// Don't report the change if there's no public diff between current and new config.
		if (publicDiff == 0) {
			return false;
		}

		final int diffWithBucket = SizeConfigurationBuckets.filterDiff(publicDiff, currentConfig,
				newConfig, sizeBuckets);
		// Compare to the diff which filter the change without crossing size buckets with
		// {@code handledConfigChanges}. The small changes should not block Activity to receive
		// its handled config updates. Also, if Activity handles all small changes, we should
		// dispatch the updated config to it.
		final int diff = diffWithBucket != 0 ? diffWithBucket : publicDiff;
		// If this activity doesn't handle any of the config changes, then don't bother
		// calling onConfigurationChanged. Otherwise, report to the activity for the
		// changes.
		return (~handledConfigChanges & diff) == 0;
	}

	private Bundle performSaveInstanceState() {
		Bundle outState = new Bundle();
		dispatchActivityPreSaveInstanceState(outState);
		onSaveInstanceState(outState);
		dispatchActivityPostSaveInstanceState(outState);
		return outState;
	}

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

	/*
	 * The window animations of an activity change, as the window manager plays them: the entering
	 * activity runs its activityOpenEnterAnimation (or ...CloseEnterAnimation) and the one it replaces
	 * its ...ExitAnimation, each from its own windowAnimationStyle, or both the animations of
	 * overridePendingTransition(). The entering window animates as a view (like a dialog's window), the
	 * leaving one is no longer part of the stack and is shown by a snapshot, which the native code
	 * draws with the transformation sampled here. Returns how long the snapshot is needed, in ms
	 * (0: none). An opening change keeps the old window below the new one until the animation of the
	 * new window ends, a closing one animates the old window out.
	 */
	private static Animation leavingAnimation;
	private static long leavingDuration;
	private static final Transformation leavingTransformation = new Transformation();

	@Export
	private static int startWindowTransition(Activity entering, Activity leaving, boolean opening, int width, int height) {
		boolean set = pendingTransitionSet;
		pendingTransitionSet = false;
		leavingAnimation = null;
		if (entering == null || leaving == null || entering == leaving || ValueAnimator.getDurationScale() == 0)
			return 0;
		Animation enter = null;
		Animation exit = null;
		if (set) {
			enter = loadWindowAnimation(entering, pendingEnterAnim, width, height);
			exit = loadWindowAnimation(leaving, pendingExitAnim, width, height);
		} else {
			enter = loadWindowAnimation(entering, windowAnimationAttr(entering, opening ? com.android.internal.R.styleable.WindowAnimation_activityOpenEnterAnimation
			                                                                           : com.android.internal.R.styleable.WindowAnimation_activityCloseEnterAnimation), width, height);
			exit = loadWindowAnimation(leaving, windowAnimationAttr(leaving, opening ? com.android.internal.R.styleable.WindowAnimation_activityOpenExitAnimation
			                                                                         : com.android.internal.R.styleable.WindowAnimation_activityCloseExitAnimation), width, height);
		}
		if (enter != null)
			entering.getWindow().getDecorView().startAnimation(enter);
		leavingAnimation = exit;
		leavingDuration = Math.max(exit != null ? exit.computeDurationHint() : 0, opening && enter != null ? enter.computeDurationHint() : 0);
		return (int)leavingDuration;
	}

	/* the next frame of the leaving window: out = {alpha, the 3x3 matrix}; false when it is gone */
	@Export
	private static boolean sampleWindowTransition(long elapsed, float[] out) {
		if (elapsed >= leavingDuration)
			return false;
		leavingTransformation.clear();
		if (leavingAnimation != null) {
			leavingAnimation.setStartTime(0);
			leavingAnimation.getTransformation(elapsed, leavingTransformation);
		}
		out[0] = leavingTransformation.getAlpha();
		float[] values = new float[9];
		leavingTransformation.getMatrix().getValues(values);
		System.arraycopy(values, 0, out, 1, 9);
		return true;
	}

	private static int windowAnimationAttr(Activity activity, int attr) {
		TypedArray a = activity.getTheme().obtainStyledAttributes(com.android.internal.R.styleable.Window);
		int style = a.getResourceId(com.android.internal.R.styleable.Window_windowAnimationStyle, 0);
		a.recycle();
		if (style == 0)
			return 0;
		a = activity.getTheme().obtainStyledAttributes(style, com.android.internal.R.styleable.WindowAnimation);
		int id = a.getResourceId(attr, 0);
		a.recycle();
		return id;
	}

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

	public native boolean isTaskRoot();

	public void postponeEnterTransition() {}

	public void startPostponedEnterTransition() {}

	public boolean isChild() {
		return false;
	}

	public void setTaskDescription(ActivityManager.TaskDescription description) {}

	private native void nativeFinish(long native_window);
	public static native void nativeStartActivity(Activity activity);
	private native void nativeRecreate();
	static native void nativePauseCurrentActivity(Intent intent);
	static native void nativeResumeCurrentActivity();
	public static native boolean nativeResumeActivity(Class<? extends Activity> activityClass, Intent intent);
	public static native void nativeOpenURI(String uri);
	public native void nativeFileChooser(int action, String type, String title, int requestCode);
	public native void nativePickImages(String[] mimeTypes, int maxItems, int requestCode);
	public void reportFullyDrawn() {}
	public void setVisible(boolean visible) {}
	public Uri getReferrer() { return null; }
	public void setDefaultKeyMode(int flag) {}
	public void registerForContextMenu(View view) {}
	public native boolean isInMultiWindowMode();

	// we never put an activity into picture-in-picture, so this is always false
	public boolean isInPictureInPictureMode() { return false; }

	private final ArrayList<Application.ActivityLifecycleCallbacks> mActivityLifecycleCallbacks =
			new ArrayList<Application.ActivityLifecycleCallbacks>();

	public void registerActivityLifecycleCallbacks(
			@NonNull Application.ActivityLifecycleCallbacks callback) {
		synchronized (mActivityLifecycleCallbacks) {
			mActivityLifecycleCallbacks.add(callback);
		}
	}

	public void unregisterActivityLifecycleCallbacks(
			@NonNull Application.ActivityLifecycleCallbacks callback) {
		synchronized (mActivityLifecycleCallbacks) {
			mActivityLifecycleCallbacks.remove(callback);
		}
	}

	private void dispatchActivityPreCreated(@Nullable Bundle savedInstanceState) {
		getApplication().dispatchActivityPreCreated(this, savedInstanceState);
		Object[] callbacks = collectActivityLifecycleCallbacks();
		if (callbacks != null) {
			for (int i = 0; i < callbacks.length; i++) {
				((Application.ActivityLifecycleCallbacks) callbacks[i]).onActivityPreCreated(this,
						savedInstanceState);
			}
		}
	}

	private void dispatchActivityCreated(@Nullable Bundle savedInstanceState) {
		getApplication().dispatchActivityCreated(this, savedInstanceState);
		Object[] callbacks = collectActivityLifecycleCallbacks();
		if (callbacks != null) {
			for (int i = 0; i < callbacks.length; i++) {
				((Application.ActivityLifecycleCallbacks) callbacks[i]).onActivityCreated(this,
						savedInstanceState);
			}
		}
	}

	private void dispatchActivityPostCreated(@Nullable Bundle savedInstanceState) {
		Object[] callbacks = collectActivityLifecycleCallbacks();
		if (callbacks != null) {
			for (int i = 0; i < callbacks.length; i++) {
				((Application.ActivityLifecycleCallbacks) callbacks[i]).onActivityPostCreated(this,
						savedInstanceState);
			}
		}
		getApplication().dispatchActivityPostCreated(this, savedInstanceState);
	}

	private void dispatchActivityPreStarted() {
		getApplication().dispatchActivityPreStarted(this);
		Object[] callbacks = collectActivityLifecycleCallbacks();
		if (callbacks != null) {
			for (int i = 0; i < callbacks.length; i++) {
				((Application.ActivityLifecycleCallbacks) callbacks[i]).onActivityPreStarted(this);
			}
		}
	}

	private void dispatchActivityStarted() {
		getApplication().dispatchActivityStarted(this);
		Object[] callbacks = collectActivityLifecycleCallbacks();
		if (callbacks != null) {
			for (int i = 0; i < callbacks.length; i++) {
				((Application.ActivityLifecycleCallbacks) callbacks[i]).onActivityStarted(this);
			}
		}
	}

	private void dispatchActivityPostStarted() {
		Object[] callbacks = collectActivityLifecycleCallbacks();
		if (callbacks != null) {
			for (int i = 0; i < callbacks.length; i++) {
				((Application.ActivityLifecycleCallbacks) callbacks[i])
						.onActivityPostStarted(this);
			}
		}
		getApplication().dispatchActivityPostStarted(this);
	}

	private void dispatchActivityPreResumed() {
		getApplication().dispatchActivityPreResumed(this);
		Object[] callbacks = collectActivityLifecycleCallbacks();
		if (callbacks != null) {
			for (int i = 0; i < callbacks.length; i++) {
				((Application.ActivityLifecycleCallbacks) callbacks[i]).onActivityPreResumed(this);
			}
		}
	}

	private void dispatchActivityResumed() {
		getApplication().dispatchActivityResumed(this);
		Object[] callbacks = collectActivityLifecycleCallbacks();
		if (callbacks != null) {
			for (int i = 0; i < callbacks.length; i++) {
				((Application.ActivityLifecycleCallbacks) callbacks[i]).onActivityResumed(this);
			}
		}
	}

	private void dispatchActivityPostResumed() {
		Object[] callbacks = collectActivityLifecycleCallbacks();
		if (callbacks != null) {
			for (int i = 0; i < callbacks.length; i++) {
				((Application.ActivityLifecycleCallbacks) callbacks[i]).onActivityPostResumed(this);
			}
		}
		getApplication().dispatchActivityPostResumed(this);
	}

	private void dispatchActivityPrePaused() {
		getApplication().dispatchActivityPrePaused(this);
		Object[] callbacks = collectActivityLifecycleCallbacks();
		if (callbacks != null) {
			for (int i = callbacks.length - 1; i >= 0; i--) {
				((Application.ActivityLifecycleCallbacks) callbacks[i]).onActivityPrePaused(this);
			}
		}
	}

	private void dispatchActivityPaused() {
		Object[] callbacks = collectActivityLifecycleCallbacks();
		if (callbacks != null) {
			for (int i = callbacks.length - 1; i >= 0; i--) {
				((Application.ActivityLifecycleCallbacks) callbacks[i]).onActivityPaused(this);
			}
		}
		getApplication().dispatchActivityPaused(this);
	}

	private void dispatchActivityPostPaused() {
		Object[] callbacks = collectActivityLifecycleCallbacks();
		if (callbacks != null) {
			for (int i = callbacks.length - 1; i >= 0; i--) {
				((Application.ActivityLifecycleCallbacks) callbacks[i]).onActivityPostPaused(this);
			}
		}
		getApplication().dispatchActivityPostPaused(this);
	}

	private void dispatchActivityPreStopped() {
		getApplication().dispatchActivityPreStopped(this);
		Object[] callbacks = collectActivityLifecycleCallbacks();
		if (callbacks != null) {
			for (int i = callbacks.length - 1; i >= 0; i--) {
				((Application.ActivityLifecycleCallbacks) callbacks[i]).onActivityPreStopped(this);
			}
		}
	}

	private void dispatchActivityStopped() {
		Object[] callbacks = collectActivityLifecycleCallbacks();
		if (callbacks != null) {
			for (int i = callbacks.length - 1; i >= 0; i--) {
				((Application.ActivityLifecycleCallbacks) callbacks[i]).onActivityStopped(this);
			}
		}
		getApplication().dispatchActivityStopped(this);
	}

	private void dispatchActivityPostStopped() {
		Object[] callbacks = collectActivityLifecycleCallbacks();
		if (callbacks != null) {
			for (int i = callbacks.length - 1; i >= 0; i--) {
				((Application.ActivityLifecycleCallbacks) callbacks[i])
						.onActivityPostStopped(this);
			}
		}
		getApplication().dispatchActivityPostStopped(this);
	}

	private void dispatchActivityPreSaveInstanceState(@NonNull Bundle outState) {
		getApplication().dispatchActivityPreSaveInstanceState(this, outState);
		Object[] callbacks = collectActivityLifecycleCallbacks();
		if (callbacks != null) {
			for (int i = callbacks.length - 1; i >= 0; i--) {
				((Application.ActivityLifecycleCallbacks) callbacks[i])
						.onActivityPreSaveInstanceState(this, outState);
			}
		}
	}

	private void dispatchActivitySaveInstanceState(@NonNull Bundle outState) {
		Object[] callbacks = collectActivityLifecycleCallbacks();
		if (callbacks != null) {
			for (int i = callbacks.length - 1; i >= 0; i--) {
				((Application.ActivityLifecycleCallbacks) callbacks[i])
						.onActivitySaveInstanceState(this, outState);
			}
		}
		getApplication().dispatchActivitySaveInstanceState(this, outState);
	}

	private void dispatchActivityPostSaveInstanceState(@NonNull Bundle outState) {
		Object[] callbacks = collectActivityLifecycleCallbacks();
		if (callbacks != null) {
			for (int i = callbacks.length - 1; i >= 0; i--) {
				((Application.ActivityLifecycleCallbacks) callbacks[i])
						.onActivityPostSaveInstanceState(this, outState);
			}
		}
		getApplication().dispatchActivityPostSaveInstanceState(this, outState);
	}

	private void dispatchActivityPreDestroyed() {
		getApplication().dispatchActivityPreDestroyed(this);
		Object[] callbacks = collectActivityLifecycleCallbacks();
		if (callbacks != null) {
			for (int i = callbacks.length - 1; i >= 0; i--) {
				((Application.ActivityLifecycleCallbacks) callbacks[i])
						.onActivityPreDestroyed(this);
			}
		}
	}

	private void dispatchActivityDestroyed() {
		Object[] callbacks = collectActivityLifecycleCallbacks();
		if (callbacks != null) {
			for (int i = callbacks.length - 1; i >= 0; i--) {
				((Application.ActivityLifecycleCallbacks) callbacks[i]).onActivityDestroyed(this);
			}
		}
		getApplication().dispatchActivityDestroyed(this);
	}

	private void dispatchActivityPostDestroyed() {
		Object[] callbacks = collectActivityLifecycleCallbacks();
		if (callbacks != null) {
			for (int i = callbacks.length - 1; i >= 0; i--) {
				((Application.ActivityLifecycleCallbacks) callbacks[i])
						.onActivityPostDestroyed(this);
			}
		}
		getApplication().dispatchActivityPostDestroyed(this);
	}

	private Object[] collectActivityLifecycleCallbacks() {
		Object[] callbacks = null;
		synchronized (mActivityLifecycleCallbacks) {
			if (mActivityLifecycleCallbacks.size() > 0) {
				callbacks = mActivityLifecycleCallbacks.toArray();
			}
		}
		return callbacks;
	}

	/* called from native code instead of onCreate() etc., with the pre and post callbacks around them */
	@Export
	private void performCreate(Bundle savedInstanceState) {
		mCurrentConfig = new Configuration(getResources().getConfiguration());
		dispatchActivityPreCreated(savedInstanceState);
		onCreate(savedInstanceState);
		dispatchActivityPostCreated(savedInstanceState);
		postCreatePending = true;
		createdFromState = savedInstanceState;
	}

	/* like AOSP's ActivityThread.handleStartActivity(): after its first onStart(), a new instance
	 * restores the state it was created from and gets onPostCreate() */
	private boolean postCreatePending;
	private Bundle createdFromState;

	private void performPostStart() {
		if (!postCreatePending)
			return;
		postCreatePending = false;
		Bundle state = createdFromState;
		createdFromState = null;
		if (finishing)
			return;
		if (state != null)
			onRestoreInstanceState(state);
		onPostCreate(state);
	}

	@Export
	private void performStart() {
		dispatchActivityPreStarted();
		onStart();
		dispatchActivityPostStarted();
		started = true;
		performPostStart();
	}

	/* like AOSP: a stopped activity that comes back gets onRestart() before onStart() */
	@Export
	private void performRestart() {
		stopped = false;
		ensureActivityConfiguration();
		onRestart();
		performStart();
	}

	@Export
	private void performResume() {
		/* the activities of an app share one GtkWindow, which knows only the Window of the activity that
		 * registered last (weakly, and that one is gone once its activity has finished): the lookup of a
		 * view's Window (View.getRootWindowInsets(), requestApplyInsets()) must find the one of the
		 * activity that comes to the front. Without it the insets of the main activity were lost after
		 * the settings had been open, and JW Library kept the old bottom margin of the content */
		if (window != null && window.native_window != 0)
			window.set_native_window(window.native_window);
		dispatchActivityPreResumed();
		onResume();
		dispatchActivityPostResumed();
	}

	@Export
	private void performPause() {
		dispatchActivityPrePaused();
		onPause();
		dispatchActivityPostPaused();
	}

	/* like AOSP's callActivityOnStop(): unless it is finishing, the activity saves its state when it is
	 * stopped, after onStop() for apps targeting API 28 and later, before it for older ones */
	@Export
	private void performStop(boolean saveState) {
		saveState = saveState && !finishing;
		boolean preP = getApplicationInfo().targetSdkVersion < 28;
		if (saveState && preP)
			savedState = performSaveInstanceState();
		dispatchActivityPreStopped();
		onStop();
		dispatchActivityPostStopped();
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
		dispatchActivityPreDestroyed();
		onDestroy();
		dispatchActivityPostDestroyed();
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

	public View onCreateView(String name, Context context, AttributeSet attrs) {
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
		return mConfigChangeFlags;
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
	 * Activity.navigateUpTo(). If the activity of upIntent is in the back stack, the activities above it
	 * are finished and it receives upIntent in onNewIntent(); if not, this activity is finished and the
	 * activity is started from upIntent (ActivityTaskManager's navigateUpTo(), which is not available
	 * here: there is only one task, so shouldUpRecreateTask() is always false).
	 */
	public boolean navigateUpTo(Intent upIntent) {
		ComponentName destInfo = upIntent.getComponent();
		if (destInfo == null) {
			destInfo = upIntent.resolveActivity(getPackageManager());
			if (destInfo == null) {
				return false;
			}
			upIntent = new Intent(upIntent);
			upIntent.setComponent(destInfo);
		}
		try {
			if (nativeResumeActivity(getClassLoader().loadClass(destInfo.getClassName()).asSubclass(Activity.class), upIntent))
				return true;
		} catch (ClassNotFoundException e) {
			return false;
		}
		startActivity(upIntent);
		finish();
		return true;
	}

	/* Activity.getParentActivityIntent() */
	public Intent getParentActivityIntent() {
		final ActivityInfo info = getActivityInfo();
		final String parentName = info != null ? info.parentActivityName : null;
		if (TextUtils.isEmpty(parentName)) {
			return null;
		}

		// If the parent itself has no parent, generate a main activity intent.
		final ComponentName target = new ComponentName(this, parentName);
		try {
			final ActivityInfo parentInfo = getPackageManager().getActivityInfo(target, 0);
			final String parentActivity = parentInfo.parentActivityName;
			final Intent parentIntent = parentActivity == null
			    ? Intent.makeMainActivity(target)
			    : new Intent().setComponent(target);
			return parentIntent;
		} catch (PackageManager.NameNotFoundException e) {
			Log.e(TAG, "getParentActivityIntent: bad parentActivityName '" + parentName +
			               "' in manifest");
			return null;
		}
	}

	/* AOSP's mActivityInfo, from the manifest */
	private ActivityInfo getActivityInfo() {
		try {
			return getPackageManager().getActivityInfo(getComponentName(), 0);
		} catch (PackageManager.NameNotFoundException e) {
			return null;
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
		getLayoutInflater().setPrivateFactory(this); /* Activity.attach() */
	}

	public void applyOverrideConfiguration(Configuration overrideConfiguration) {}

	public ComponentName getCallingActivity() {
		return resultActivity == null ? null : resultActivity.getComponentName();
	}

	/* Activity.onNavigateUp(), without the task stack that shouldUpRecreateTask() would need */
	public boolean onNavigateUp() {
		// Automatically handle hierarchical Up navigation if the proper
		// metadata is available.
		Intent upIntent = getParentActivityIntent();
		if (upIntent != null) {
			final ActivityInfo info = getActivityInfo();
			if (info != null && info.taskAffinity == null) {
				// Activities with a null affinity are special; they really shouldn't
				// specify a parent activity intent in the first place. Just finish
				// the current activity and call it a day.
				finish();
			} else {
				navigateUpTo(upIntent);
			}
			return true;
		}
		return false;
	}

	@Deprecated
	public boolean onNavigateUpFromChild(Activity child) {
		return onNavigateUp();
	}

	public void setExitSharedElementCallback(SharedElementCallback callback) {}

	public void setEnterSharedElementCallback(SharedElementCallback callback) {}
}
