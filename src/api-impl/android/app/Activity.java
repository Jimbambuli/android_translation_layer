package android.app;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.ActionBar;
import android.atl.ATLLoadedApp;
import android.atl.annotation.Export;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.LocusId;
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
import android.view.SearchEvent;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManagerImpl;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Transformation;
import android.webkit.MimeTypeMap;
import android.window.SizeConfigurationBuckets;
import android.app.assist.AssistContent;
import android.os.PersistableBundle;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.Executor;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import android.util.ArrayMap;
import android.os.Parcelable;
import java.util.List;
import java.util.Map;

public class Activity extends ContextThemeWrapper implements Window.Callback, KeyEvent.Callback, LayoutInflater.Factory2, android.content.ComponentCallbacks2 {
	private final static String TAG = "Activity";

	public static final int RESULT_CANCELED = 0;
	public static final int RESULT_OK = -1;

	Window window;
	int requested_orientation = -1 /*ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED*/; // dummy
	public Intent intent;
	public ComponentName component;
	private Activity resultActivity; // the activity that started this one with startActivityForResult()
	private android.atl.ATLProcess.RemoteCaller remoteCaller; // the starter in another process (android:process)
	private int coveringOtherProcesses; // the activities of other processes on top of this one (startInOtherProcess())
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
	final Handler mHandler = new Handler(Looper.getMainLooper());
	final FragmentController mFragments = FragmentController.createController(new HostCallbacks());
	static final String FRAGMENTS_TAG = "android:fragments";
	private final HashMap<Integer, String> mFragmentResultWho = new HashMap<>();
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
		/* a local file: VIEW with its type, else SEND, as from a file manager on Android */
		if (className == null && uri != null && ("file".equals(uri.getScheme()) || "data".equals(uri.getScheme()))) {
			Intent file = desktopFileIntent(uri);
			if (file != null) {
				intent = file;
				className = file.getComponent().getClassName();
			}
		}
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
						score = filter.match(intent.getAction(), intent.getType(), uri.getScheme(), uri, intent.getCategories(), TAG);
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
		ATLLoadedApp.setTaskBaseIntent(intent);
		return internalCreateActivity(className, native_window, intent);
	}

	/**
	 * A file the desktop hands to the app («Open with»), like a file manager on Android: VIEW with the file's type
	 * (Intent.setDataAndType(); players decide by it whether to show a video) for an activity that takes it, else
	 * what the share sheet sends: SEND (or SEND_MULTIPLE with one item) with a content URI in EXTRA_STREAM and
	 * ClipData, readable (FLAG_GRANT_READ_URI_PERMISSION). Returns the intent with its activity, or null.
	 */
	public static Intent desktopFileIntent(Uri file) {
		if ("data".equals(file.getScheme()))
			return sharedTextIntent(file);
		String type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(file.toString()));
		Intent view = new Intent(Intent.ACTION_VIEW);
		if (type != null)
			view.setDataAndType(file, type);
		else
			view.setData(file);
		ComponentName viewer = ownActivityFor(view, file);
		if (viewer != null)
			return view.setComponent(viewer);
		Uri content = android.content.ContentResolver.atlUriForFile(file.getPath());
		for (String action : new String[] {Intent.ACTION_SEND, Intent.ACTION_SEND_MULTIPLE}) {
			Intent send = new Intent(action).setType(type != null ? type : "application/octet-stream");
			ComponentName target = ownActivityFor(send, content);
			if (target == null)
				continue;
			if (Intent.ACTION_SEND.equals(action)) {
				send.putExtra(Intent.EXTRA_STREAM, content);
			} else {
				java.util.ArrayList<Uri> uris = new java.util.ArrayList<>();
				uris.add(content);
				send.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
			}
			send.setClipData(android.content.ClipData.newRawUri(null, content));
			send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
			return send.setComponent(target);
		}
		return null;
	}

	/**
	 * Text another installed app shared with this one (ContextImpl.share()): it comes as a data: URI
	 * ("data:text/plain;charset=utf-8,TEXT", percent-encoded) through --uri, and becomes what Android's
	 * chooser sends: SEND with EXTRA_TEXT for the activity that takes text/plain. Null if none does.
	 */
	private static Intent sharedTextIntent(Uri data) {
		String raw = data.getEncodedSchemeSpecificPart();
		int comma = raw != null ? raw.indexOf(',') : -1;
		if (comma < 0 || !raw.substring(0, comma).toLowerCase(java.util.Locale.ROOT).startsWith("text/plain"))
			return null;
		Intent send = new Intent(Intent.ACTION_SEND).setType("text/plain");
		ComponentName target = ownActivityFor(send, null);
		if (target == null)
			return null;
		send.putExtra(Intent.EXTRA_TEXT, Uri.decode(raw.substring(comma + 1)));
		return send.setComponent(target);
	}

	/* the app's activity whose filter in CATEGORY_DEFAULT matches best (MATCH_DEFAULT_ONLY), with `data` as the URI */
	private static ComponentName ownActivityFor(Intent intent, Uri data) {
		PackageParser.Package pkg = ATLLoadedApp.getPrimaryApplication().pkg;
		String className = null;
		int best = 0;
		for (PackageParser.Activity activity : pkg.activities) {
			if (!activity.info.enabled)
				continue;
			for (PackageParser.IntentInfo filter : activity.intents) {
				if (!filter.hasCategory(Intent.CATEGORY_DEFAULT))
					continue;
				int score = filter.match(intent.getAction(), intent.getType(), data != null ? data.getScheme() : null, data, intent.getCategories(), TAG);
				if (score > best) {
					className = activity.info.targetActivity != null ? activity.info.targetActivity : activity.className;
					best = score;
				}
			}
		}
		return className != null ? new ComponentName(pkg.packageName, className) : null;
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

	/* AOSP Activity.getVoiceInteractor(): the interactor of the voice interaction session the activity runs in,
	 * null for an activity that is not part of one. ATL has no voice interaction service, so no activity is. */
	public VoiceInteractor getVoiceInteractor() {
		return null;
	}

	/* AOSP: the package of the activity that started this one with startActivityForResult(); null if that one
	 * expects no result (it passed no request code, or a negative one) */
	public String getCallingPackage() {
		if (remoteCaller != null && remoteCaller.wantsResult)
			return getPackageName();
		return resultActivity == null ? null : resultActivity.getPackageName();
	}

	public ComponentName getComponentName() {
		return component;
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

	/* AOSP: getWindow().requestFeature(), which throws once content is set */
	public final boolean requestWindowFeature(int featureId) {
		return getWindow().requestFeature(featureId);
	}

	/* AOSP Activity.setVolumeControlStream()/getVolumeControlStream(): the window's */
	public final void setVolumeControlStream(int streamType) {
		getWindow().setVolumeControlStream(streamType);
	}

	public final int getVolumeControlStream() {
		return getWindow().getVolumeControlStream();
	}

	/* AOSP Activity.onActivityReenter(): empty hook (called when a started activity finished with a shared
	 * element transition) */
	public void onActivityReenter(int resultCode, Intent data) {}

	/* AOSP Activity.startIntentSenderFromChild() (deprecated): startIntentSenderForResult() of this activity */
	@Deprecated
	public void startIntentSenderFromChild(Activity child, IntentSender intent, int requestCode, Intent fillInIntent, int flagsMask, int flagsValues, int extraFlags) throws IntentSender.SendIntentException {
		startIntentSenderForResult(intent, requestCode, fillInIntent, flagsMask, flagsValues, extraFlags, null);
	}

	protected void onCreate(Bundle savedInstanceState) {
		Slog.i(TAG, "- onCreate - yay!");

		if (mLastNonConfigurationInstances != null) {
			mFragments.restoreLoaderNonConfig(mLastNonConfigurationInstances.loaders);
		}
		if (savedInstanceState != null) {
			Parcelable p = savedInstanceState.getParcelable(FRAGMENTS_TAG);
			mFragments.restoreAllState(p, mLastNonConfigurationInstances != null ? mLastNonConfigurationInstances.fragments : null);
		}
		mFragments.dispatchCreate();
		dispatchActivityCreated(savedInstanceState);

		return;
	}

	protected void onPostCreate(Bundle savedInstanceState) {
		Slog.i(TAG, "- onPostCreate - yay!");
		/* AOSP Activity.onPostCreate(): from now on a changed title reaches the window */
		mTitleReady = true;
		onTitleChanged(getTitle(), getTitleColor());
		return;
	}

	protected void onStart() {
		Slog.i(TAG, "- onStart - yay!");
		window.setTitle(title);
		if (!mMultiWindowKnown) {
			mIsInMultiWindowMode = isInMultiWindowMode();
			mMultiWindowKnown = true;
		}

		mFragments.doLoaderStart();
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

		dispatchActivityPaused();

		paused = true;
		return;
	}

	protected void onStop() {
		Slog.i(TAG, "- onStop - yay!");

		dispatchActivityStopped();

		return;
	}

	protected void onDestroy() {
		Slog.i(TAG, "- onDestroy - yay!");

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
		Parcelable p = mFragments.saveAllState();
		if (p != null) {
			outState.putParcelable(FRAGMENTS_TAG, p);
		}
		dispatchActivitySaveInstanceState(outState);
	}

	public void onConfigurationChanged(Configuration newConfig) {
		mFragments.dispatchConfigurationChanged(newConfig);
	}

	public void onLowMemory() {
		mFragments.dispatchLowMemory();
	}

	public void onTrimMemory(int level) {
		mFragments.dispatchTrimMemory(level);
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

	/** Same as {@link #onActivityResult(int, int, Intent)}, with the caller that sent the result. */
	public void onActivityResult(int requestCode, int resultCode, @NonNull Intent data, @NonNull ComponentCaller caller) {
		onActivityResult(requestCode, resultCode, data);
	}

	/* Activity.dispatchActivityResult(). ATL: only the results of activities (who == null); there are no
	 * results for fragments, permission requests, views or autofill */
	void dispatchActivityResult(String who, int requestCode, int resultCode, Intent data, String reason) {
		mFragments.noteStateNotSaved();
		if (who == null)
			who = mFragmentResultWho.remove(requestCode);
		Fragment frag = who != null ? mFragments.findFragmentByWho(who) : null;
		if (frag != null)
			frag.onActivityResult(requestCode, resultCode, data);
		else
			deliverActivityResult(requestCode, resultCode, data);
	}

	/* onActivityResult() with the ComponentCaller of Android 15: the result comes from an activity of this app */
	private void deliverActivityResult(int requestCode, int resultCode, Intent data) {
		ComponentCaller caller = new ComponentCaller(null, null, getPackageName(), android.system.Os.getuid());
		mCurrentCaller = caller;
		try {
			onActivityResult(requestCode, resultCode, data, caller);
		} finally {
			mCurrentCaller = null;
		}
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
		Uri result = uri != null ? Uri.parse(uri) : null;
		/* AOSP's DocumentsUI answers OPEN_DOCUMENT/GET_CONTENT and CREATE_DOCUMENT with a document's content URI
		 * (apps check the scheme, use DocumentFile); ATL's names the file the dialog chose (in a Flatpak the file
		 * chooser portal's document, writable for a file to save) like DownloadsProvider's "raw:" documents */
		if (result != null && action != 2 && "file".equals(result.getScheme()))
			result = android.content.ContentResolver.atlUriForFile(result.getPath());
		Intent data = new Intent(FILE_CHOOSER_ACTIONS.get(action), result);
		if (result != null)
			data.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | (action == 1 ? Intent.FLAG_GRANT_WRITE_URI_PERMISSION : 0));
		dispatchActivityResult(null, requestCode, resultCode, data, "fileChooserResult");
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
		/* package: (uninstall, app info) and market: intents never go to the desktop */
		if (android.content.pm.PackageInstaller.handlePackageIntent(this, intent, requestCode < 0 ? null : new java.util.function.IntConsumer() {
			    @Override
			    public void accept(int resultCode) {
				    deliverActivityResult(requestCode, resultCode, new Intent());
			    }
		    }))
			return;
		String process;
		if (intent.getComponent() != null && android.atl.ATLLoadedAppManager.startInstalledApp(getPackageName(), intent.getComponent().getPackageName())) {
			return;
		} else if (intent.getComponent() != null && (process = android.atl.ATLProcess.activityProcess(intent.getComponent().getClassName())) != null) {
			startInOtherProcess(this, intent, requestCode, process);
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
						deliverActivityResult(requestCode, 0 /*RESULT_CANCELED*/, new Intent());
						return;
					}
					activity.mLaunchedFromPackage = getPackageName();
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
					deliverActivityResult(requestCode, -1 /*RESULT_OK*/, new Intent());
				} else {
					deliverActivityResult(requestCode, 0 /*RESULT_CANCELED*/, new Intent());
				}
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
				deliverActivityResult(requestCode, 0 /*RESULT_CANCELED*/, new Intent());
			}
		} else {
			Slog.i(TAG, "startActivityForResult: intent was not handled. Calling onActivityResult(RESULT_CANCELED).");
			deliverActivityResult(requestCode, 0 /*RESULT_CANCELED*/, new Intent());
		}
	}
	public void startActivityForResult(Intent intent, int requestCode) {
		startActivityForResult(intent, requestCode, null);
	}

	/** @hide ATL: an activity of the app with android:process runs in that process, in a window of its own over
	 * this one (ATLProcess.startActivity()). Like the activity below it in the task on Android, the shown one is
	 * paused meanwhile and gets the result (`caller` with a request code >= 0) before it is resumed. */
	public static void startInOtherProcess(final Activity caller, final Intent intent, final int requestCode, final String process) {
		new Handler(Looper.getMainLooper()).post(new Runnable() {
			@Override
			public void run() {
				android.atl.ATLProcess.startActivity(intent, process, caller != null && requestCode >= 0, new android.atl.ATLProcess.ActivityFinished() {
					/* the activity of this process the started one is on top of, while it is */
					private Activity covered = cover(intent);

					private Activity cover(Intent intent) {
						Activity activity = nativeCoverCurrentActivity(intent);
						if (activity != null)
							activity.coveringOtherProcesses++;
						return activity;
					}

					private void uncover() {
						if (covered != null)
							covered.coveringOtherProcesses--;
						covered = null;
						nativeResumeCurrentActivity();
					}

					@Override
					public void onActivityFinished(int resultCode, Intent data) {
						if (caller != null && requestCode >= 0)
							caller.addPendingResult(requestCode, resultCode, data);
						uncover();
					}

					/* like the task below the pinned task on Android: resumed while the started activity is in
					 * picture-in-picture, paused again when it comes back */
					@Override
					public void onCovering(boolean covers) {
						if (!covers)
							uncover();
						else if (covered == null)
							covered = cover(null);
					}
				});
			}
		});
	}

	/** @hide ATL: the activity another process of the app started (ATLProcess) comes up in this process */
	public static void startFromOtherProcess(Intent intent, android.atl.ATLProcess.RemoteCaller caller) throws ReflectiveOperationException {
		Activity activity = internalCreateActivity(intent.getComponent().getClassName(),
		                                           android.atl.ATLLoadedApp.getPrimaryApplication().getNativeWindow(), intent);
		activity.remoteCaller = caller;
		nativeStartActivity(activity);
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
		return null;
	}

	/* AOSP: the dialog of a showDialog(id, args) is created with the arguments */
	protected Dialog onCreateDialog(int id, Bundle args) {
		return onCreateDialog(id);
	}

	protected void onPrepareDialog(int id, Dialog dialog) {
		dialog.setOwnerActivity(this);
	}

	protected void onPrepareDialog(int id, Dialog dialog, Bundle args) {
		onPrepareDialog(id, dialog);
	}

	/* AOSP: the dialogs of showDialog() are managed: created once per id, shown again by the next showDialog(),
	 * dismissed by dismissDialog() and discarded by removeDialog(). ATL does not save and restore them with
	 * the activity's state. */
	private final HashMap<Integer, Dialog> dialogs = new HashMap<Integer, Dialog>();

	public final void showDialog(int id) {
		showDialog(id, null);
	}

	public final boolean showDialog(int id, Bundle args) {
		Dialog dialog = dialogs.get(id);
		if (dialog == null) {
			dialog = onCreateDialog(id, args);
			if (dialog == null)
				return false;
			dialog.dispatchOnCreate(null);
			dialogs.put(id, dialog);
		}
		onPrepareDialog(id, dialog, args);
		dialog.show();
		return true;
	}

	public final void dismissDialog(int id) {
		Dialog dialog = dialogs.get(id);
		if (dialog == null)
			throw missingDialog(id);
		dialog.dismiss();
	}

	private IllegalArgumentException missingDialog(int id) {
		return new IllegalArgumentException("no dialog with id " + id + " was ever "
						    + "shown via Activity#showDialog");
	}

	public final void removeDialog(int id) {
		Dialog dialog = dialogs.remove(id);
		if (dialog != null) {
			dialog.dismiss();
		}
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
		if (!wasFinishing && remoteCaller != null)
			remoteCaller.finished(resultCode, resultData);
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
		FragmentManagerNonConfig fragments;
		ArrayMap<String, LoaderManager> loaders;
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
		FragmentManagerNonConfig fragments = mFragments.retainNestedNonConfig();

		// we are already stopped but asked to retain: mark the loaders for retention
		mFragments.doLoaderStart();
		mFragments.doLoaderStop(true);
		ArrayMap<String, LoaderManager> loaders = mFragments.retainLoaderNonConfig();

		if (activity == null && children == null && fragments == null && loaders == null) {
			return null;
		}

		NonConfigurationInstances nci = new NonConfigurationInstances();
		nci.activity = activity;
		nci.children = children;
		nci.fragments = fragments;
		nci.loaders = loaders;
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
		return mFragments.getFragmentManager();
	}

	/* AOSP Activity.onAttachFragment() */
	public void onAttachFragment(Fragment fragment) {
	}

	void startActivityFromFragment(Fragment fragment, Intent intent, int requestCode, Bundle options) {
		if (requestCode >= 0)
			mFragmentResultWho.put(requestCode, fragment.mWho);
		startActivityForResult(intent, requestCode, options);
	}

	/* AOSP Activity.HostCallbacks */
	class HostCallbacks extends FragmentHostCallback<Activity> {
		public HostCallbacks() {
			super(Activity.this /*activity*/);
		}

		@Override
		public void onDump(String prefix, java.io.FileDescriptor fd, java.io.PrintWriter writer, String[] args) {
		}

		@Override
		public boolean onShouldSaveFragmentState(Fragment fragment) {
			return !isFinishing();
		}

		@Override
		public LayoutInflater onGetLayoutInflater() {
			final LayoutInflater result = Activity.this.getLayoutInflater();
			if (onUseFragmentManagerInflaterFactory()) {
				return result.cloneInContext(Activity.this);
			}
			return result;
		}

		@Override
		public boolean onUseFragmentManagerInflaterFactory() {
			return getApplicationInfo().targetSdkVersion >= Build.VERSION_CODES.LOLLIPOP;
		}

		@Override
		public Activity onGetHost() {
			return Activity.this;
		}

		@Override
		public void onInvalidateOptionsMenu() {
			Activity.this.invalidateOptionsMenu();
		}

		@Override
		public void onStartActivityFromFragment(Fragment fragment, Intent intent, int requestCode, Bundle options) {
			Activity.this.startActivityFromFragment(fragment, intent, requestCode, options);
		}

		@Override
		public void onRequestPermissionsFromFragment(Fragment fragment, String[] permissions, int requestCode) {
			Activity.this.requestPermissions(permissions, requestCode);
		}

		@Override
		public boolean onHasWindowAnimations() {
			return getWindow() != null;
		}

		@Override
		public int onGetWindowAnimations() {
			final Window w = getWindow();
			return (w == null) ? 0 : w.getAttributes().windowAnimations;
		}

		@Override
		public void onAttachFragment(Fragment fragment) {
			Activity.this.onAttachFragment(fragment);
		}

		@Override
		public <T extends View> T onFindViewById(int id) {
			return Activity.this.findViewById(id);
		}

		@Override
		public boolean onHasView() {
			final Window w = getWindow();
			return (w != null && w.peekDecorView() != null);
		}
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
				boolean show = onCreateOptionsMenu(menu);
				show |= mFragments.dispatchCreateOptionsMenu(menu, getMenuInflater());
				return show;
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
			boolean goforit = onPrepareOptionsMenu(menu);
			goforit |= mFragments.dispatchPrepareOptionsMenu(menu);
			return goforit;
		}
		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		if (featureId == Window.FEATURE_OPTIONS_PANEL) {
			return onOptionsItemSelected(item) || mFragments.dispatchOptionsItemSelected(item);
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
			mFragments.dispatchOptionsMenuClosed(menu);
		}
	}

	/* AOSP Activity.setTitle(): a title change goes to onTitleChanged(), once onPostCreate() has run */
	public void setTitle(CharSequence title) {
		this.title = title;
		onTitleChanged(title, mTitleColor);
	}

	public void setTitle(int titleId) {
		setTitle(getText(titleId));
	}

	private boolean mTitleReady = false;
	private int mTitleColor = 0;

	/* AOSP: the color of the title; the window's title bar is the desktop's, which has no color to set */
	@Deprecated
	public void setTitleColor(int textColor) {
		mTitleColor = textColor;
		onTitleChanged(title, textColor);
	}

	public final int getTitleColor() {
		return mTitleColor;
	}

	/* AOSP Activity.onTitleChanged(): the window gets the title (and the action bar, which keeps it) */
	protected void onTitleChanged(CharSequence title, int color) {
		if (mActionBar != null)
			mActionBar.setWindowTitle(title);
		if (mTitleReady) {
			final Window win = getWindow();
			if (win != null) {
				/* the title of an action bar that was given one stays the title of the window */
				win.setTitle(mActionBar != null ? mActionBar.getTitle() : title);
			}
		}
	}

	public CharSequence getTitle() {
		return title;
	}

	/* AOSP 12+ (Activity.onBackInvoked(), ActivityClientController.onBackPressed()): Back that nobody took
	 * finishes the activity, but the root activity of the task moves the task to the back instead */
	@Export
	public void onBackPressed() {
		System.out.println("onBackPressed() called");
		if (isTaskRoot())
			moveTaskToBack(false);
		else
			finish();
	}

	/* AOSP calls the exit listener only when a splash screen was shown for this launch; ATL shows none,
	 * so the listener is never called and the theme is not used */
	public android.window.SplashScreen getSplashScreen() {
		return new android.window.SplashScreen() {
			public void setOnExitAnimationListener(android.window.SplashScreen.OnExitAnimationListener listener) {}
			public void clearOnExitAnimationListener() {}
			public void setSplashScreenTheme(int themeId) {}
		};
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
		internalSetIntent(newIntent, null);
	}

	/* Android 15 (AOSP android-14 QPR source): the app that launched this activity, that sent it a new
	 * intent, or that sent it a result. ATL knows only activities of the same app as callers. */
	private ComponentCaller mInitialCaller;
	private ComponentCaller mCaller;
	private ComponentCaller mCurrentCaller;
	private boolean mCallerSet;

	private ComponentCaller callerOfStarter() {
		Activity starter = resultActivity;
		return new ComponentCaller(null, null, starter != null ? starter.getPackageName() : null,
		                           starter != null ? android.system.Os.getuid() : -1);
	}

	/** The caller of the intent returned by {@link #getIntent()}. */
	public @Nullable ComponentCaller getCaller() {
		return mCallerSet ? mCaller : getInitialCaller();
	}

	/** Changes the intent returned by getIntent() and the caller returned by getCaller(). */
	public void setIntent(@Nullable Intent newIntent, @Nullable ComponentCaller newCaller) {
		internalSetIntent(newIntent, newCaller);
	}

	private void internalSetIntent(Intent newIntent, ComponentCaller newCaller) {
		this.intent = newIntent;
		mCaller = newCaller;
		mCallerSet = true;
	}

	/** The caller of the intent that launched this activity. */
	public @NonNull ComponentCaller getInitialCaller() {
		if (mInitialCaller == null)
			mInitialCaller = callerOfStarter();
		return mInitialCaller;
	}

	/** The caller of the new intent or result being delivered; only valid in onNewIntent()/onActivityResult(). */
	public @NonNull ComponentCaller getCurrentCaller() {
		if (mCurrentCaller == null) {
			throw new IllegalStateException("The caller is null because #getCurrentCaller should be"
			                                + " called within #onNewIntent method");
		}
		return mCurrentCaller;
	}

	/* AOSP Activity.performNewIntent(): ATL's native side delivers the new intent here */
	@Export
	private void performNewIntent(Intent intent) {
		mCurrentCaller = callerOfStarter();
		try {
			onNewIntent(intent, mCurrentCaller);
		} finally {
			mCurrentCaller = null;
		}
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
		/* ActivityThread.handleMultiWindowModeChanged(): ATL's multi-window mode is a window that is not
		 * maximized (isInMultiWindowMode()); a change comes with a change of the size */
		if (mMultiWindowKnown) {
			boolean multiWindow = isInMultiWindowMode();
			if (multiWindow != mIsInMultiWindowMode) {
				mIsInMultiWindowMode = multiWindow;
				onMultiWindowModeChanged(multiWindow, new Configuration(newConfig));
			}
		}
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
		else if (shouldReportChange(oldConfig, newConfig, mSizeConfigurations, handled)) {
			onConfigurationChanged(new Configuration(newConfig));
			/* ViewRootImpl.updateConfiguration(): the views of the window hear it as well */
			if (window.peekDecorView() != null)
				window.peekDecorView().dispatchConfigurationChanged(newConfig);
		}
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

	protected void onNewIntent(Intent intent) {}

	/** Same as {@link #onNewIntent(Intent)}, with the caller that sent the intent. */
	public void onNewIntent(@NonNull Intent intent, @NonNull ComponentCaller caller) {
		onNewIntent(intent);
	}

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

	/* AOSP: mParent != null */
	public boolean isChild() {
		return getParent() != null;
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
	/** @hide ATL: the window of this process for another process of the app, see ATLProcess.startActivity():
	 * export its handle (xdg-foreign) on the main thread, wait for it on another; show it for an activity that
	 * another process started, over that process's window (parentHandle) */
	public static native void nativeExportWindow();
	public static native String nativeWaitWindowHandle(int timeoutMs);
	public static native void nativeShowWindowFor(String parentHandle, String appId);
	/** @hide ATL: the number of the activities of this process */
	public static native int nativeActivityCount();
	private native void nativeRecreate();
	static native void nativePauseCurrentActivity(Intent intent);
	private static native Activity nativeCoverCurrentActivity(Intent intent);
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
	/* AOSP Activity.getReferrer(): EXTRA_REFERRER or EXTRA_REFERRER_NAME of the intent, else the
	 * package of the caller as android-app:// URI. ATL does not track the caller of a start, so
	 * without the extras it is null (as AOSP for a start with no known caller). */
	public Uri getReferrer() {
		Intent intent = getIntent();
		if (intent != null) {
			Uri referrer = intent.getParcelableExtra(Intent.EXTRA_REFERRER, Uri.class);
			if (referrer != null) {
				return referrer;
			}
			String referrerName = intent.getStringExtra(Intent.EXTRA_REFERRER_NAME);
			if (referrerName != null) {
				return Uri.parse(referrerName);
			}
		}
		return null;
	}
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

	/* AOSP Activity.unregisterForContextMenu(): view.setOnCreateContextMenuListener(null) */
	public void unregisterForContextMenu(View view) {
		view.setOnCreateContextMenuListener(null);
	}

	public void onCreateContextMenu(android.view.ContextMenu menu, View v, android.view.ContextMenu.ContextMenuInfo menuInfo) {}
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
		mFragments.dispatchPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);
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
		mFragments.attachHost(null /*parent*/);
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
		mFragments.dispatchActivityCreated();
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

	/* AOSP Activity.getLoaderManager(): the activity's loaders, started and stopped with it */
	public LoaderManager getLoaderManager() {
		return mFragments.getLoaderManager();
	}

	@Export
	private void performStart() {
		dispatchActivityPreStarted();
		mFragments.noteStateNotSaved();
		mFragments.execPendingActions();
		onStart();
		mFragments.dispatchStart();
		mFragments.reportLoaderStart();
		dispatchActivityPostStarted();
		started = true;
		performPostStart();
	}

	/* like AOSP: a stopped activity that comes back gets onRestart() before onStart() */
	@Export
	private void performRestart() {
		mCanEnterPictureInPicture = true;
		stopped = false;
		mFragments.noteStateNotSaved();
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
		mFragments.execPendingActions();
		onResume();
		mFragments.dispatchResume();
		mFragments.execPendingActions();
		dispatchActivityPostResumed();
	}

	@Export
	private void performPause() {
		dispatchActivityPrePaused();
		mFragments.dispatchPause();
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
		mFragments.doLoaderStop(mChangingConfigurations);
		dispatchActivityPreStopped();
		mFragments.dispatchStop();
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

	/* native code: this activity of a window that another process's activity opened entered picture-in-picture
	 * as the only one there (false), or came back from it (true) */
	@Export
	private void coversStarter(boolean covers) {
		if (remoteCaller != null)
			remoteCaller.covers(covers);
	}

	@Export
	private void performDestroy() {
		/* destroyed without finish() (its window closed): the starter in another process gets RESULT_CANCELED */
		if (remoteCaller != null && !finishing)
			remoteCaller.finished(RESULT_CANCELED, null);
		dispatchActivityPreDestroyed();
		mFragments.dispatchDestroy();
		onDestroy();
		mFragments.doLoaderDestroy();
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
		return mFragments.onCreateView(parent, name, context, attrs);
	}

	public View onCreateView(String name, Context context, AttributeSet attrs) {
		return null;
	}

	/* AOSP Activity.onSearchRequested(): the search dialog for the activity's searchable (startSearch()) */
	public boolean onSearchRequested() {
		startSearch(null, false, null, false);
		return true;
	}

	private SearchEvent mSearchEvent;

	/* AOSP Activity.onSearchRequested(SearchEvent): the event is kept while the plain callback runs (getSearchEvent()) */
	@Override
	public boolean onSearchRequested(SearchEvent searchEvent) {
		mSearchEvent = searchEvent;
		boolean result = onSearchRequested();
		mSearchEvent = null;
		return result;
	}

	public final SearchEvent getSearchEvent() {
		return mSearchEvent;
	}

	/* AOSP Activity.dispatchPopulateAccessibilityEvent(): the window's event gets the activity's class, package,
	 * whether it fills the screen and its title */
	@Override
	public boolean dispatchPopulateAccessibilityEvent(android.view.accessibility.AccessibilityEvent event) {
		event.setClassName(getClass().getName());
		event.setPackageName(getPackageName());
		WindowManager.LayoutParams params = getWindow().getAttributes();
		boolean isFullScreen = (params.width == WindowManager.LayoutParams.MATCH_PARENT) && (params.height == WindowManager.LayoutParams.MATCH_PARENT);
		event.setFullScreen(isFullScreen);
		CharSequence title = getTitle();
		if (!TextUtils.isEmpty(title)) {
			event.getText().add(title);
		}
		return true;
	}

	public View getCurrentFocus() {
		return window != null ? window.getCurrentFocus() : null;
	}

	public void setProgressBarIndeterminateVisibility(boolean indeterminate) {}

	public int getChangingConfigurations() {
		return mConfigChangeFlags;
	}

	public int getTaskId() {
		/* we don't support multiple activity stacks, so this is probably fine? */
		return System.identityHashCode(this.getApplicationContext());
	}

	/* AOSP: the task goes behind the others, the app keeps running; only the root activity may do it unless
	 * nonroot. ATL has one task per app window: the window is minimized, which stops the shown activity as on
	 * Android (see window_set_hidden() in android_app_Activity.c) */
	public boolean moveTaskToBack(boolean nonroot) {
		if (!nonroot && !isTaskRoot())
			return false;
		nativeMoveTaskToBack();
		return true;
	}

	private static native void nativeMoveTaskToBack();

	public void setFinishOnTouchOutside(boolean finish) {
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
	 * did for the back button so far: the OnBackInvokedDispatcher's callbacks first, then onBackPressed() */
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
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			onUserInteraction();
		}
		if (window.superDispatchTouchEvent(event)) {
			return true;
		}
		return onTouchEvent(event);
	}

	@Override
	public boolean dispatchTrackballEvent(MotionEvent event) {
		onUserInteraction();
		if (window.superDispatchTrackballEvent(event)) {
			return true;
		}
		return onTrackballEvent(event);
	}

	@Override
	public boolean dispatchGenericMotionEvent(MotionEvent event) {
		onUserInteraction();
		if (window.superDispatchGenericMotionEvent(event)) {
			return true;
		}
		return onGenericMotionEvent(event);
	}

	/* AOSP Activity.onGenericMotionEvent(): joystick and mouse events no view handled */
	public boolean onGenericMotionEvent(MotionEvent event) {
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

	/**
	 * AOSP asks the task whether it has the task affinity of the target: ATL's activities of an app are one task,
	 * whose affinity is that of this activity, so a target with another affinity belongs to another task.
	 */
	public boolean shouldUpRecreateTask(Intent targetIntent) {
		try {
			PackageManager pm = getPackageManager();
			ComponentName cn = targetIntent.getComponent();
			if (cn == null) {
				cn = targetIntent.resolveActivity(pm);
			}
			ActivityInfo info = pm.getActivityInfo(cn, 0);
			if (info.taskAffinity == null) {
				return false;
			}
			ActivityInfo own = pm.getActivityInfo(getComponentName(), 0);
			return !info.taskAffinity.equals(own.taskAffinity);
		} catch (PackageManager.NameNotFoundException e) {
			return false;
		}
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

	/* AOSP Activity.getActionBar()/initWindowDecorActionBar(): there is an action bar when the window has the feature
	 * (its theme has windowActionBar and not windowNoTitle, or the app asked for it) and the activity is no child;
	 * with a theme without action bar (AppCompat's, dialogs, NoActionBar) it is null as in AOSP.
	 * ATL's window has no decor bar widget: ActivityActionBar keeps the state and runs the callbacks. */
	private ActionBar mActionBar;

	public ActionBar getActionBar() {
		window.getDecorView(); // initializing the decor can change the window's features
		if (!isChild() && mActionBar == null && window.hasFeature(Window.FEATURE_ACTION_BAR))
			mActionBar = new ActivityActionBar(this);
		return mActionBar;
	}

	/* ---- AOSP Activity members that the apps call and that had no counterpart here ---- */

	/* AOSP Activity.addContentView(): the view goes into the content next to what is there */
	public void addContentView(View view, ViewGroup.LayoutParams params) {
		window.addContentView(view, params);
		onContentChanged();
	}

	/* AOSP: Activity.onCreate() for an activity that is persistable and was started with persisted state */
	public void onCreate(Bundle savedInstanceState, PersistableBundle persistentState) {
		onCreate(savedInstanceState);
	}

	/* ATL has no context menus (View.showContextMenu() shows none), so openContextMenu() opens none and the
	 * window has no FEATURE_CONTEXT_MENU panel for closeContextMenu() to close, as AOSP does when the window
	 * lacks the feature; onContextItemSelected() and onContextMenuClosed() are only AOSP's defaults */
	public void openContextMenu(View view) {
		view.showContextMenu();
	}

	public void closeContextMenu() {
		if (window.hasFeature(Window.FEATURE_CONTEXT_MENU)) {
			/* not reachable: Window.hasFeature() says no */
		}
	}

	public boolean onContextItemSelected(@NonNull MenuItem item) {
		if (getParent() != null) {
			return getParent().onContextItemSelected(item);
		}
		return false;
	}

	public void onContextMenuClosed(@NonNull Menu menu) {
		if (getParent() != null) {
			getParent().onContextMenuClosed(menu);
		}
	}

	/* AOSP Activity.openOptionsMenu(): the options menu in the window's panel. ATL's options menu is the desktop
	 * header bar's, which the user opens; nothing in the window can be opened from here, so this changes
	 * nothing (AOSP does the same when the window has no options panel) */
	public void openOptionsMenu() {
	}

	/* AOSP Activity.createPendingResult(): a PendingIntent whose sending delivers onActivityResult() to this
	 * activity (resultCode is the code of send(), data the given intent filled in with the sent one); null
	 * if the flags ask for a PendingIntent that does not exist (FLAG_NO_CREATE) */
	public PendingIntent createPendingResult(int requestCode, @NonNull Intent data, int flags) {
		if ((flags & PendingIntent.FLAG_NO_CREATE) != 0)
			return null;
		return PendingIntent.getActivityResult(this, requestCode, data);
	}

	/* what PendingIntent.send() of createPendingResult() does (ActivityThread.handleSendResult()): the result
	 * goes to onActivityResult() at once when the activity is running, else when it comes back */
	void deliverPendingResult(int requestCode, int resultCode, Intent data) throws PendingIntent.CanceledException {
		if (finishing || destroyed)
			throw new PendingIntent.CanceledException();
		if (paused || stopped) {
			addPendingResult(requestCode, resultCode, data);
			return;
		}
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				if (!finishing && !destroyed)
					dispatchActivityResult(null, requestCode, resultCode, data, "ACTIVITY_RESULT");
			}
		});
	}

	/* AOSP: activities are in no bubble; ATL has no bubbles */
	public boolean isLaunchedFromBubble() {
		return false;
	}

	/* AOSP: the package that started this activity (ActivityClient, from the ActivityRecord); only
	 * activities that another activity of the app started have one, the first one comes from the desktop */
	String mLaunchedFromPackage;
	/* AOSP: Activity.mIsInMultiWindowMode, kept to tell onMultiWindowModeChanged() when it changes */
	private boolean mIsInMultiWindowMode;
	private boolean mMultiWindowKnown;

	/** @hide AOSP Activity.getLaunchedFromUid(): the uid of the one that started the activity, which here is the app itself (or the launcher it was started from, which has no uid of its own to give) */
	public int getLaunchedFromUid() {
		return android.system.Os.getuid();
	}

	public String getLaunchedFromPackage() {
		return mLaunchedFromPackage;
	}

	/* AOSP: every activity has this Window.Callback method, which does nothing */
	public void onEnterAnimationComplete() {
	}

	/* AOSP Activity.onMultiWindowModeChanged(): the two variants call each other the way AOSP's do */
	public void onMultiWindowModeChanged(boolean isInMultiWindowMode, Configuration newConfig) {
		onMultiWindowModeChanged(isInMultiWindowMode);
	}

	@Deprecated
	public void onMultiWindowModeChanged(boolean isInMultiWindowMode) {
	}

	public void onProvideAssistContent(AssistContent outContent) {
	}

	/* AOSP Activity.onTouchEvent(): a touch outside of the window closes a window that asked for it
	 * (setFinishOnTouchOutside()) */
	public boolean onTouchEvent(MotionEvent event) {
		if (window.shouldCloseOnTouch(this, event)) {
			finish();
			return true;
		}
		return false;
	}

	public boolean onTrackballEvent(MotionEvent event) {
		return false;
	}

	/* AOSP: only activities that are translucent behind (visible behind) have this and it is gone since
	 * Android 5; requestVisibleBehind() says no and nothing is ever cancelled */
	public boolean requestVisibleBehind(boolean visible) {
		return false;
	}

	public void onVisibleBehindCanceled() {
	}

	/* AOSP Activity.overrideActivityTransition(): the animation of opening or closing this activity. ATL does
	 * not animate windows, so nothing shows the override; the check of the type is AOSP's. */
	public static final int OVERRIDE_TRANSITION_OPEN = 0;
	public static final int OVERRIDE_TRANSITION_CLOSE = 1;

	public void overrideActivityTransition(int overrideType, int enterAnim, int exitAnim) {
		overrideActivityTransition(overrideType, enterAnim, exitAnim, 0);
	}

	public void overrideActivityTransition(int overrideType, int enterAnim, int exitAnim, int backgroundColor) {
		if (overrideType != OVERRIDE_TRANSITION_OPEN && overrideType != OVERRIDE_TRANSITION_CLOSE) {
			throw new IllegalArgumentException("Override type must be either "
							   + "OVERRIDE_TRANSITION_OPEN or OVERRIDE_TRANSITION_CLOSE, but was " + overrideType);
		}
	}

	public void clearOverrideActivityTransition(int overrideType) {
		if (overrideType != OVERRIDE_TRANSITION_OPEN && overrideType != OVERRIDE_TRANSITION_CLOSE) {
			throw new IllegalArgumentException("Override type must be either "
							   + "OVERRIDE_TRANSITION_OPEN or OVERRIDE_TRANSITION_CLOSE, but was " + overrideType);
		}
	}

	/* AOSP Activity.ScreenCaptureCallback: told when the user takes a screenshot (permission
	 * DETECT_SCREEN_CAPTURE). The desktop does not tell ATL, so the callbacks are kept and never called. */
	public interface ScreenCaptureCallback {
		void onScreenCaptured();
	}

	private final ArrayList<ScreenCaptureCallback> mScreenCaptureCallbacks = new ArrayList<ScreenCaptureCallback>();

	public void registerScreenCaptureCallback(@NonNull Executor executor, @NonNull ScreenCaptureCallback callback) {
		if (executor == null || callback == null)
			throw new NullPointerException();
		synchronized (mScreenCaptureCallbacks) {
			if (!mScreenCaptureCallbacks.contains(callback))
				mScreenCaptureCallbacks.add(callback);
		}
	}

	public void unregisterScreenCaptureCallback(@NonNull ScreenCaptureCallback callback) {
		synchronized (mScreenCaptureCallbacks) {
			mScreenCaptureCallbacks.remove(callback);
		}
	}

	/* AOSP: where the activity shows when the screen is locked or turned off by the system (keyguard); the
	 * desktop session decides, so the flags are only kept (getWindow() has them as FLAG_SHOW_WHEN_LOCKED
	 * and FLAG_TURN_SCREEN_ON, as in AOSP) */
	public void setShowWhenLocked(boolean showWhenLocked) {
		if (showWhenLocked)
			window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
		else
			window.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
	}

	public void setTurnScreenOn(boolean turnScreenOn) {
		if (turnScreenOn)
			window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
		else
			window.clearFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
	}

	/* AOSP Activity.setTranslucent(): makes the activity's window see through to what is behind it, true if the
	 * conversion happened. A GTK window on the desktop is composited by the desktop, never converted by
	 * ATL, so the answer is false for both directions (AOSP answers false when the system refuses) */
	public boolean setTranslucent(boolean translucent) {
		return false;
	}

	/* AOSP Activity.setVrModeEnabled(): the system's VR service; a device without the VR feature
	 * (FEATURE_VR_MODE) has none, which AOSP reports with a PackageManager.NameNotFoundException when the
	 * component is not a VR listener service; here no component is one */
	public void setVrModeEnabled(boolean enabled, @NonNull ComponentName requestedComponent) throws PackageManager.NameNotFoundException {
		throw new PackageManager.NameNotFoundException(requestedComponent.flattenToString() + " is not a VR listener service");
	}

	/* AOSP Activity.setLocusContext(): tells the system's shortcut, content capture and usage services what the
	 * activity shows; ATL has none of them to tell */
	public void setLocusContext(@Nullable LocusId locusId, @Nullable Bundle bundle) {
	}

	/* AOSP Activity.startActionMode(): the decor view starts it; ATL has no action mode (View.startActionMode()
	 * answers null) */
	public ActionMode startActionMode(ActionMode.Callback callback) {
		return window.getDecorView().startActionMode(callback);
	}

	public ActionMode startActionMode(ActionMode.Callback callback, int type) {
		return window.getDecorView().startActionMode(callback, type);
	}

	/* AOSP Activity.startSearch() / SearchDialog: a dialog with a text field for the searchable of this activity
	 * (its "android.app.searchable", else the "android.app.default_searchable" of the activity or the application);
	 * the query goes as ACTION_SEARCH with SearchManager.QUERY to the searchable activity - to this one through
	 * onNewIntent() when it is single top, as on Android. Without a searchable nothing happens (AOSP: no dialog;
	 * there is no global search). Suggestions are not shown. */
	public void startSearch(@Nullable String initialQuery, boolean selectInitialQuery, @Nullable final Bundle appSearchData, boolean globalSearch) {
		SearchManager searchManager = (SearchManager)getSystemService(Context.SEARCH_SERVICE);
		final SearchableInfo searchable = searchableFor(searchManager);
		if (searchable == null) {
			Slog.i(TAG, "startSearch: no searchable for " + getComponentName());
			return;
		}
		final android.widget.EditText field = new android.widget.EditText(this);
		field.setSingleLine(true);
		if (searchable.getHintId() != 0)
			field.setHint(searchable.getHintId());
		if (initialQuery != null) {
			field.setText(initialQuery);
			if (selectInitialQuery)
				field.selectAll();
		}
		final AlertDialog[] dialog = new AlertDialog[1];
		final Runnable submit = new Runnable() {
			@Override
			public void run() {
				String query = field.getText().toString();
				if (dialog[0] != null)
					dialog[0].dismiss();
				if (query.trim().isEmpty())
					return;
				Intent search = new Intent(Intent.ACTION_SEARCH).setComponent(searchable.getSearchActivity());
				search.putExtra(SearchManager.QUERY, query);
				search.putExtra(SearchManager.USER_QUERY, query);
				if (appSearchData != null)
					search.putExtra(SearchManager.APP_DATA, appSearchData);
				deliverSearch(search);
			}
		};
		field.setOnEditorActionListener(new android.widget.TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(android.widget.TextView v, int actionId, KeyEvent event) {
				submit.run();
				return true;
			}
		});
		dialog[0] = new AlertDialog.Builder(this)
		                .setTitle(searchable.getLabelId() != 0 ? getText(searchable.getLabelId()) : getText(com.android.internal.R.string.search_go))
		                .setView(field)
		                .setPositiveButton(com.android.internal.R.string.search_go, new DialogInterface.OnClickListener() {
			                @Override
			                public void onClick(DialogInterface d, int which) {
				                submit.run();
			                }
		                })
		                .setNegativeButton(android.R.string.cancel, null)
		                .create();
		dialog[0].show();
		field.requestFocus();
	}

	private SearchableInfo searchableFor(SearchManager searchManager) {
		SearchableInfo own = searchManager.getSearchableInfo(getComponentName());
		if (own != null)
			return own;
		String name = null;
		ActivityInfo info = getActivityInfo();
		if (info != null && info.metaData != null)
			name = info.metaData.getString("android.app.default_searchable");
		if (name == null && getApplicationInfo().metaData != null)
			name = getApplicationInfo().metaData.getString("android.app.default_searchable");
		if (name == null || name.equals("*"))
			return null;
		if (name.startsWith("."))
			name = getPackageName() + name;
		return searchManager.getSearchableInfo(new ComponentName(getPackageName(), name));
	}

	private void deliverSearch(Intent search) {
		ActivityInfo info = getActivityInfo();
		boolean singleTop = info != null && (info.launchMode == ActivityInfo.LAUNCH_SINGLE_TOP || info.launchMode == ActivityInfo.LAUNCH_SINGLE_TASK
		                                     || info.launchMode == ActivityInfo.LAUNCH_SINGLE_INSTANCE);
		if (singleTop && getComponentName().equals(search.getComponent()))
			performNewIntent(search);
		else
			startActivity(search);
	}

	/* AOSP Activity.startActivityIfNeeded(): like startActivityForResult(), except that nothing starts when the
	 * activity for the intent is this one and it is single top (the flag, launchMode singleTop, singleTask or
	 * singleInstance) - then false is returned and the intent is not delivered, as for START_RETURN_INTENT_TO_CALLER */
	public boolean startActivityIfNeeded(@NonNull Intent intent, int requestCode) {
		return startActivityIfNeeded(intent, requestCode, null);
	}

	public boolean startActivityIfNeeded(@NonNull Intent intent, int requestCode, @Nullable Bundle options) {
		ComponentName target = intent.getComponent();
		if (target != null && target.equals(getComponentName())) {
			boolean singleTop = (intent.getFlags() & Intent.FLAG_ACTIVITY_SINGLE_TOP) != 0;
			ActivityInfo info = getActivityInfo();
			if (info != null) {
				singleTop |= info.launchMode == ActivityInfo.LAUNCH_SINGLE_TOP || info.launchMode == ActivityInfo.LAUNCH_SINGLE_TASK
				    || info.launchMode == ActivityInfo.LAUNCH_SINGLE_INSTANCE;
			}
			if (singleTop)
				return false;
		}
		startActivityForResult(intent, requestCode, options);
		return true;
	}

	/* AOSP Activity.startIntentSender() and startIntentSenderForResult(): what the IntentSender stands for is
	 * started in the name of the activity. For a PendingIntent of an activity this is startActivityForResult()
	 * of its intent with the fill-in intent merged in and the flags set as flagsValues says for the bits of
	 * flagsMask; a service or a broadcast is sent. */
	public void startIntentSender(IntentSender intent, @Nullable Intent fillInIntent, int flagsMask, int flagsValues, int extraFlags) throws IntentSender.SendIntentException {
		startIntentSender(intent, fillInIntent, flagsMask, flagsValues, extraFlags, null);
	}

	public void startIntentSender(IntentSender intent, @Nullable Intent fillInIntent, int flagsMask, int flagsValues, int extraFlags, @Nullable Bundle options) throws IntentSender.SendIntentException {
		startIntentSenderForResult(intent, -1, fillInIntent, flagsMask, flagsValues, extraFlags, options);
	}

	public void startIntentSenderForResult(IntentSender intent, int requestCode, @Nullable Intent fillInIntent, int flagsMask, int flagsValues, int extraFlags) throws IntentSender.SendIntentException {
		startIntentSenderForResult(intent, requestCode, fillInIntent, flagsMask, flagsValues, extraFlags, null);
	}

	public void startIntentSenderForResult(IntentSender intent, int requestCode, @Nullable Intent fillInIntent, int flagsMask, int flagsValues, int extraFlags, @Nullable Bundle options) throws IntentSender.SendIntentException {
		PendingIntent target = intent.getPendingIntent();
		if (target == null)
			throw new IntentSender.SendIntentException("no PendingIntent");
		Intent filled = new Intent(target.getIntent());
		if (fillInIntent != null)
			filled.putExtras(fillInIntent);
		filled.setFlags((filled.getFlags() & ~flagsMask) | (flagsValues & flagsMask));
		if (target.type == 0) {
			startActivityForResult(filled, requestCode, options);
		} else {
			try {
				target.send(this, 0, fillInIntent);
			} catch (PendingIntent.CanceledException e) {
				throw new IntentSender.SendIntentException(e);
			}
		}
	}

	/* AOSP Activity.dump(): what dumpsys prints about the activity; the part AOSP prints about the
	 * fragments and loaders is the fragment controller's */
	public void dump(@NonNull String prefix, @Nullable FileDescriptor fd, @NonNull PrintWriter writer, @Nullable String[] args) {
		String innerPrefix = prefix + "  ";
		writer.print(prefix);
		writer.print("Local Activity ");
		writer.print(Integer.toHexString(System.identityHashCode(this)));
		writer.println(" State:");
		writer.print(innerPrefix);
		writer.print("mResumed=");
		writer.print(!paused && started);
		writer.print(" mStopped=");
		writer.print(stopped);
		writer.print(" mFinished=");
		writer.println(finishing);
		writer.print(innerPrefix);
		writer.print("mIsInMultiWindowMode=");
		writer.print(mIsInMultiWindowMode);
		writer.print(" mIsInPictureInPictureMode=");
		writer.println(mIsInPictureInPictureMode);
		writer.print(innerPrefix);
		writer.print("mChangingConfigurations=");
		writer.println(mChangingConfigurations);
		writer.print(innerPrefix);
		writer.print("mCurrentConfig=");
		writer.println(mCurrentConfig);
		mFragments.dumpLoaders(innerPrefix, fd, writer, args);
		mFragments.getFragmentManager().dump(innerPrefix, fd, writer, args);
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
