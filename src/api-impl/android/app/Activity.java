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
import android.graphics.drawable.Drawable;
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

public class Activity extends ContextThemeWrapper implements Window.Callback, KeyEvent.Callback, LayoutInflater.Factory2 {
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
		/* a link from the desktop, like a browser starts it on Android: VIEW, BROWSABLE, for an activity with
		 * a matching filter in CATEGORY_DEFAULT (MATCH_DEFAULT_ONLY) */
		Intent intent = uri != null ? new Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE) : new Intent();
		if (className == null) {
			int best = 0;
			for (PackageParser.Activity activity : ATLLoadedApp.getPrimaryApplication().pkg.activities) {
				if (!activity.info.enabled)
					continue;
				for (PackageParser.IntentInfo filter : activity.intents) {
					Slog.i(TAG, filter.toString());
					int score;
					if (uri == null)
						score = filter.hasCategory("android.intent.category.LAUNCHER") && filter.hasAction("android.intent.action.MAIN") ? 1 : 0;
					else if (filter.hasCategory(Intent.CATEGORY_DEFAULT))
						score = filter.match(intent.getAction(), null, uri.getScheme(), uri, intent.getCategories(), TAG);
					else
						score = 0;
					if (score > best) {
						className = activity.info.targetActivity != null ? activity.info.targetActivity : activity.className;
						best = score;
					}
				}
				if (uri == null && className != null)
					break;
			}
		} else {
			className = className.replace('/', '.');
		}
		if (className == null && uri != null) {
			/* «Open with» offers the app for every link of a scheme; one it has no filter for starts the app */
			System.err.println("Failed to find Activity to launch URI: " + uri);
			return createMainActivity(null, native_window, null);
		}
		if (className == null) {
			System.err.println("Failed to find main Activity");
			System.exit(1);
		}
		return internalCreateActivity(className, native_window, intent);
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
		/* AOSP: the system's PackageInstallerActivity confirms a PackageInstaller session */
		if (android.content.pm.PackageInstaller.handleConfirmIntent(intent))
			return;
		if (intent.getComponent() != null && android.atl.ATLLoadedAppManager.startInstalledApp(getPackageName(), intent.getComponent().getPackageName())) {
			return;
		} else if (intent.getComponent() != null) {
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
	@Export
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
		newConfig.orientation = sys_config.orientation;
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
		ensureActivityConfiguration(getResources().getConfiguration());
	}

	private void ensureActivityConfiguration(Configuration globalConfig) {
		if (mCurrentConfig == null || finishing || mChangingConfigurations)
			return;
		Resources resources = getResources();
		Configuration newConfig = new Configuration(globalConfig);
		applyPictureInPictureBounds(newConfig);
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

	/* AOSP Activity.finishAndRemoveTask(): finish(FINISH_TASK_WITH_ACTIVITY), the task is removed with all its
	 * activities (ActivityClientController.finishActivity() -> ActivityTaskSupervisor.removeTask()). ATL has the
	 * app's one task (the back stack) and the pinned task of picture-in-picture (nativeFinishTask()) */
	public void finishAndRemoveTask() {
		nativeFinishTask();
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

	/**
	 * If set to false, this indicates to the system that it should never take a
	 * screenshot of the activity to be used as a representation in recents screen.
	 * ATL shows no recents screen.
	 */
	public void setRecentsScreenshotEnabled(boolean enabled) {}

	private native void nativeFinish(long native_window);
	public static native void nativeStartActivity(Activity activity);
	private native void nativeRecreate();
	static native void nativePauseCurrentActivity(Intent intent);
	static native void nativeResumeCurrentActivity();
	public static native boolean nativeResumeActivity(Class<? extends Activity> activityClass, Intent intent);
	static native void nativeClearTaskMark();
	static native void nativeClearTaskFinish(boolean finish);
	private native void nativeFinishTask();
	public static native void nativeOpenURI(String uri);
	public native void nativeFileChooser(int action, String type, String title, int requestCode);
	public native void nativePickImages(String[] mimeTypes, int maxItems, int requestCode);
	public void reportFullyDrawn() {}
	public void setVisible(boolean visible) {}
	public Uri getReferrer() { return null; }
	/* AOSP Activity.setDefaultKeyMode(); of the modes, ATL has DEFAULT_KEYS_DISABLE (keys the activity does not
	 * handle are dropped) and DEFAULT_KEYS_SHORTCUT, the others type nowhere */
	public static final int DEFAULT_KEYS_DISABLE = 0;
	public static final int DEFAULT_KEYS_DIALER = 1;
	public static final int DEFAULT_KEYS_SHORTCUT = 2;
	public static final int DEFAULT_KEYS_SEARCH_LOCAL = 3;
	public static final int DEFAULT_KEYS_SEARCH_GLOBAL = 4;
	private int mDefaultKeyMode = DEFAULT_KEYS_DISABLE;

	public final void setDefaultKeyMode(int mode) {
		mDefaultKeyMode = mode;
	}
	public void registerForContextMenu(View view) {}
	public native boolean isInMultiWindowMode();

	/* AOSP Activity, picture-in-picture. The part of ActivityTaskManagerService (ATMS: the pinned task, which
	 * is a small window of its own here, its menu with the actions) is native, see android_app_Activity.c */
	boolean mIsInPictureInPictureMode;
	private boolean mCanEnterPictureInPicture = false;
	/* ActivityRecord.pictureInPictureArgs */
	private final PictureInPictureParams mPictureInPictureArgs = new PictureInPictureParams.Builder().build();
	/* the size of the pinned task's window (the bounds of WINDOWING_MODE_PINNED), 0 when not pinned */
	private int mPinnedWidth;
	private int mPinnedHeight;

	public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, Configuration newConfig) {
		// Left deliberately empty. There should be no side effects if a direct
		// subclass of Activity does not call super.
		onPictureInPictureModeChanged(isInPictureInPictureMode);
	}

	public void onPictureInPictureUiStateChanged(@NonNull PictureInPictureUiState pipState) {
		// Left deliberately empty. There should be no side effects if a direct
		// subclass of Activity does not call super.
	}

	@Deprecated
	public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode) {
		// Left deliberately empty. There should be no side effects if a direct
		// subclass of Activity does not call super.
	}

	public boolean isInPictureInPictureMode() {
		return mIsInPictureInPictureMode;
	}

	@Deprecated
	public void enterPictureInPictureMode() {
		enterPictureInPictureMode(new PictureInPictureParams.Builder().build());
	}

	public boolean enterPictureInPictureMode(@NonNull PictureInPictureParams params) {
		if (!deviceSupportsPictureInPictureMode()) {
			return false;
		}
		if (params == null) {
			throw new IllegalArgumentException("Expected non-null picture-in-picture params");
		}
		if (!mCanEnterPictureInPicture) {
			throw new IllegalStateException("Activity must be resumed to enter"
			    + " picture-in-picture");
		}
		// Set mIsInPictureInPictureMode earlier and don't wait for
		// onPictureInPictureModeChanged callback here. This is to ensure that
		// isInPictureInPictureMode returns true in the following onPause callback.
		// See https://developer.android.com/guide/topics/ui/picture-in-picture for guidance.
		mIsInPictureInPictureMode = enterPictureInPictureModeInternal("enterPictureInPictureMode", params);
		return mIsInPictureInPictureMode;
	}

	public void setPictureInPictureParams(@NonNull PictureInPictureParams params) {
		if (!deviceSupportsPictureInPictureMode()) {
			return;
		}
		if (params == null) {
			throw new IllegalArgumentException("Expected non-null picture-in-picture params");
		}
		/* ATMS.setPictureInPictureParams(): the pinned task takes the new aspect ratio and actions */
		ensureValidPictureInPictureActivityParams("setPictureInPictureParams", params);
		mPictureInPictureArgs.copyOnlySet(params);
		if (mIsInPictureInPictureMode)
			nativeUpdatePictureInPicture(pictureInPictureAspectRatio());
	}

	public int getMaxNumPictureInPictureActions() {
		return MAX_NUM_PICTURE_IN_PICTURE_ACTIONS;
	}

	private boolean deviceSupportsPictureInPictureMode() {
		return getPackageManager().hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE);
	}

	public boolean onPictureInPictureRequested() {
		return false;
	}

	final void dispatchPictureInPictureModeChanged(boolean isInPictureInPictureMode,
						       Configuration newConfig) {
		for (Fragment fragment : fragments) {
			fragment.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);
		}
		mIsInPictureInPictureMode = isInPictureInPictureMode;
		onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);
	}

	/* config_pictureInPictureMaxNumberOfActions and the aspect ratio limits of AOSP's config.xml */
	private static final int MAX_NUM_PICTURE_IN_PICTURE_ACTIONS = 3;
	private static final float PICTURE_IN_PICTURE_MIN_ASPECT_RATIO = 0.41841004184f;
	private static final float PICTURE_IN_PICTURE_MAX_ASPECT_RATIO = 2.39f;

	/* ATMS.ensureValidPictureInPictureActivityParams() */
	private void ensureValidPictureInPictureActivityParams(String caller, PictureInPictureParams params) {
		ActivityInfo info = null;
		try {
			info = getPackageManager().getActivityInfo(getComponentName(), 0);
		} catch (PackageManager.NameNotFoundException e) {
		}
		if (info == null || !info.supportsPictureInPicture()) {
			throw new IllegalStateException(caller
			    + ": Current activity does not support picture-in-picture.");
		}
		if (params.hasSetAspectRatio()
		    && (params.getAspectRatioFloat() < PICTURE_IN_PICTURE_MIN_ASPECT_RATIO
			|| params.getAspectRatioFloat() > PICTURE_IN_PICTURE_MAX_ASPECT_RATIO)) {
			throw new IllegalArgumentException(String.format(caller
			    + ": Aspect ratio is too extreme (must be between %f and %f).",
			    PICTURE_IN_PICTURE_MIN_ASPECT_RATIO, PICTURE_IN_PICTURE_MAX_ASPECT_RATIO));
		}
		// Truncate the number of actions if necessary
		params.truncateActions(MAX_NUM_PICTURE_IN_PICTURE_ACTIONS);
	}

	/* ATMS.enterPictureInPictureMode(): the params become the activity's, then the activity moves to
	 * the pinned task (native: its page moves into a window of its own, the activity below comes back) */
	private boolean enterPictureInPictureModeInternal(String caller, PictureInPictureParams params) {
		ensureValidPictureInPictureActivityParams(caller, params);
		if (finishing)
			return false;
		mPictureInPictureArgs.copyOnlySet(params);
		return nativeEnterPictureInPicture(pictureInPictureAspectRatio());
	}

	/* TaskFragment.startPausing() with shouldAutoPip: when the user leaves the app (its window is minimized), the
	 * resumed activity whose params allow it (setAutoEnterEnabled()) enters picture-in-picture instead of being
	 * paused; ActivityThread sets mIsInPictureInPictureMode before the activity is paused */
	@Export
	private boolean autoEnterPictureInPicture() {
		if (paused || !mCanEnterPictureInPicture || !mPictureInPictureArgs.isAutoEnterEnabled()
		    || !deviceSupportsPictureInPictureMode())
			return false;
		try {
			mIsInPictureInPictureMode = enterPictureInPictureModeInternal("autoEnterPictureInPicture", mPictureInPictureArgs);
		} catch (RuntimeException e) {
			Slog.w(TAG, "autoEnterPictureInPicture: " + e);
			return false;
		}
		return mIsInPictureInPictureMode;
	}

	/* the aspect ratio of the pinned task: the app's, else config_pictureInPictureDefaultAspectRatio */
	private float pictureInPictureAspectRatio() {
		return mPictureInPictureArgs.hasSetAspectRatio() ? mPictureInPictureArgs.getAspectRatioFloat() : 1.777778f;
	}

	/* the configuration of an activity in the pinned task: screen size and orientation of its bounds */
	private void applyPictureInPictureBounds(Configuration config) {
		if (mPinnedWidth <= 0 || mPinnedHeight <= 0)
			return;
		config.screenWidthDp = mPinnedWidth;
		config.screenHeightDp = mPinnedHeight;
		config.smallestScreenWidthDp = Math.min(mPinnedWidth, mPinnedHeight);
		config.orientation = mPinnedWidth > mPinnedHeight ? Configuration.ORIENTATION_LANDSCAPE : Configuration.ORIENTATION_PORTRAIT;
	}

	/* called from native code when the activity moved into or out of the pinned task (width and height of its
	 * window, 0 when it left it): like ActivityThread.performActivityConfigurationChanged(), the picture-in-picture
	 * mode change (handleWindowingModeChangeIfNeeded()) comes before the configuration change */
	@Export
	private void pinnedTaskChanged(int width, int height) {
		boolean pinned = width > 0 && height > 0;
		mPinnedWidth = pinned ? width : 0;
		mPinnedHeight = pinned ? height : 0;
		Configuration newConfig = new Configuration(getResources().getConfiguration());
		applyPictureInPictureBounds(newConfig);
		if (pinned != mIsInPictureInPictureMode || !pinned)
			dispatchPictureInPictureModeChanged(pinned, newConfig);
		ensureActivityConfiguration(getResources().getConfiguration());
	}

	/* the actions of the pinned task's menu (PipMenuView.updateActionViews()), for the native code; the icons
	 * stay loaded while the menu shows them */
	private ArrayList<Drawable> mPictureInPictureActionIcons = new ArrayList<>();

	@Export
	private String[] getPictureInPictureActionTitles() {
		List<RemoteAction> actions = mPictureInPictureArgs.getActions();
		String[] titles = new String[actions == null ? 0 : actions.size()];
		for (int i = 0; i < titles.length; i++) {
			CharSequence title = actions.get(i).getTitle();
			titles[i] = title == null ? null : title.toString();
		}
		return titles;
	}

	@Export
	private long[] getPictureInPictureActionIcons() {
		List<RemoteAction> actions = mPictureInPictureArgs.getActions();
		long[] icons = new long[actions == null ? 0 : actions.size()];
		mPictureInPictureActionIcons.clear();
		for (int i = 0; i < icons.length; i++) {
			RemoteAction action = actions.get(i);
			Drawable icon = action.shouldShowIcon() && action.getIcon() != null ? action.getIcon().loadDrawable(this) : null;
			if (icon != null) {
				mPictureInPictureActionIcons.add(icon);
				icons[i] = icon.paintable;
			}
		}
		return icons;
	}

	@Export
	private boolean[] getPictureInPictureActionsEnabled() {
		List<RemoteAction> actions = mPictureInPictureArgs.getActions();
		boolean[] enabled = new boolean[actions == null ? 0 : actions.size()];
		for (int i = 0; i < enabled.length; i++)
			enabled[i] = actions.get(i).isEnabled();
		return enabled;
	}

	/* called from native code when the user clicked an action of the pinned task's menu (PipMenuView) */
	@Export
	private void performPictureInPictureAction(int index) {
		List<RemoteAction> actions = mPictureInPictureArgs.getActions();
		if (actions == null || index >= actions.size())
			return;
		try {
			actions.get(index).getActionIntent().send();
		} catch (Exception e) {
			Slog.w(TAG, "Failed to send action " + actions.get(index), e);
		}
	}

	private native boolean nativeEnterPictureInPicture(float aspectRatio);
	private native void nativeUpdatePictureInPicture(float aspectRatio);

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
		mCanEnterPictureInPicture = true;
		mCurrentConfig = new Configuration(getResources().getConfiguration());
		dispatchActivityPreCreated(savedInstanceState);
		Activity outer = creating;
		creating = this;
		try {
			onCreate(savedInstanceState);
		} finally {
			creating = outer;
		}
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
		mCanEnterPictureInPicture = true;
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
		// Disallow entering picture-in-picture after the activity has been stopped
		mCanEnterPictureInPicture = false;
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

	/* AOSP Activity.dispatchKeyEvent(): the window's views (Window.superDispatchKeyEvent()), then the activity
	 * itself (KeyEvent.dispatch() to onKeyDown() etc.). Called for the keys the views did not consume on their
	 * way through GTK (WrapperWidget.c), see Window.dispatchKeyEventFromNative(). ATL has no action bar that
	 * would take the menu key first. */
	public boolean dispatchKeyEvent(KeyEvent event) {
		onUserInteraction();

		Window win = getWindow();
		if (win.superDispatchKeyEvent(event)) {
			return true;
		}
		View decor = win.getDecorView();
		return event.dispatch(this, decor != null
		    ? decor.getKeyDispatcherState() : null, this);
	}

	/* AOSP Activity.dispatchKeyShortcutEvent() */
	@Override
	public boolean dispatchKeyShortcutEvent(KeyEvent event) {
		onUserInteraction();
		if (getWindow().superDispatchKeyShortcutEvent(event)) {
			return true;
		}
		return onKeyShortcut(event.getKeyCode(), event);
	}

	/* AOSP Activity.onKeyDown(): Back starts tracking (it goes back when released, see onKeyUp()), the other keys
	 * go by the default key mode */
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (getApplicationInfo().targetSdkVersion
			    >= Build.VERSION_CODES.ECLAIR) {
				event.startTracking();
			} else {
				onBackPressed();
			}
			return true;
		}

		if (mDefaultKeyMode == DEFAULT_KEYS_DISABLE) {
			return false;
		} else if (mDefaultKeyMode == DEFAULT_KEYS_SHORTCUT) {
			return false;
		} else if (keyCode == KeyEvent.KEYCODE_TAB) {
			// Don't consume TAB here since it's used for navigation. Arrow keys
			// aren't considered "typing keys" so they already won't get consumed.
			return false;
		} else {
			/* DEFAULT_KEYS_DIALER/SEARCH_*: ATL has no dialer or search to type into */
			return false;
		}
	}

	public boolean onKeyLongPress(int keyCode, KeyEvent event) {
		return false;
	}

	/* AOSP Activity.onKeyUp(): a tracked Back that was not canceled goes back. AOSP calls onBackPressed() here
	 * (or the system hands Back to the OnBackInvokedDispatcher); ATL's back (nativeBackInvoked()) does what it
	 * did for the back button so far: the OnBackInvokedDispatcher's callbacks first, then onBackPressed(),
	 * unless the activity is the only one and has Activity's own onBackPressed() (its task would just move
	 * to the back on Android 12+) */
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		int sdkVersion = getApplicationInfo().targetSdkVersion;
		if (sdkVersion >= Build.VERSION_CODES.ECLAIR) {
			if (keyCode == KeyEvent.KEYCODE_BACK
			    && event.isTracking()
			    && !event.isCanceled()) {
				nativeBackInvoked();
				return true;
			}
		}
		return false;
	}

	public boolean onKeyMultiple(int keyCode, int repeatCount, KeyEvent event) {
		return false;
	}

	/* AOSP Activity.onKeyShortcut(): ATL has no action bar to give the shortcut to */
	public boolean onKeyShortcut(int keyCode, KeyEvent event) {
		return false;
	}

	private native void nativeBackInvoked();

	/* ○ of ATL's navigation bar (navigation_bar.c): Home inside the app, the task back to its root: the launch
	 * intent with FLAG_ACTIVITY_NEW_TASK and FLAG_ACTIVITY_CLEAR_TASK starts the start activity anew and the open
	 * ones finish (ContextImpl.startActivity()). FLAG_ACTIVITY_CLEAR_TOP is not enough: a launcher activity that
	 * is a trampoline (JW Library's MainActivity starts its singleTask SiloContainer and finishes) is not in the
	 * back stack, and the singleTask activity it starts only gets onNewIntent() and stays on its page. */
	@Export
	private void navigateToStart() {
		Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
		if (intent != null)
			startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
	}

	/* AOSP Activity.setMediaController(): the media keys the activity does not handle go to this session */
	public final void setMediaController(android.media.session.MediaController controller) {
		getWindow().setMediaController(controller);
	}

	public final android.media.session.MediaController getMediaController() {
		return getWindow().getMediaController();
	}

	/* the remaining Window.Callback methods; touches reach the views through GTK, so there is
	 * nothing to dispatch here, and there is no action bar to start an action mode in */

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

	/* the activity whose onCreate() runs */
	private static Activity creating;

	/** @hide ATL: the activity being created belongs to another process than the application
	 * (android:process); ATL runs all of them in one */
	public static boolean isCreatingInOtherProcess() {
		Activity a = creating;
		ActivityInfo info = a != null ? a.getActivityInfo() : null;
		if (info == null || info.processName == null)
			return false;
		/* as with AOSP's PackageParser, the application's process is named after the package by default */
		String app = a.getApplicationInfo().processName;
		return !info.processName.equals(app != null ? app : a.getPackageName());
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

	/**
	 * Create {@link android.view.DragAndDropPermissions} object bound to this activity and controlling the
	 * access permissions for content URIs associated with the {@link android.view.DragEvent}.
	 * @return The {@link android.view.DragAndDropPermissions} object used to control access to the content URIs.
	 * Null if no content URIs are associated with the event or if permissions could not be granted.
	 */
	public android.view.DragAndDropPermissions requestDragAndDropPermissions(android.view.DragEvent event) {
		android.view.DragAndDropPermissions dragAndDropPermissions = android.view.DragAndDropPermissions.obtain(event);
		if (dragAndDropPermissions != null && dragAndDropPermissions.take(null)) {
			return dragAndDropPermissions;
		}
		return null;
	}

	public void requestPermissions(String[] permissions, final int requestCode) {
		if (requestCode < 0)
			throw new IllegalArgumentException("requestCode should be >= 0");
		// ATL has no permission dialog: like AOSP, the answer always arrives later on the main thread,
		// here with the current grant state of each permission
		final String[] requested = permissions.clone();
		final int[] results = new int[requested.length];
		for (int i = 0; i < requested.length; i++)
			results[i] = checkSelfPermission(requested[i]);
		new Handler(Looper.getMainLooper()).post(new Runnable() {
			public void run() {
				onRequestPermissionsResult(requestCode, requested, results);
			}
		});
	}

	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {}

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
