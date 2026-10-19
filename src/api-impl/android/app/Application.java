package android.app;

import android.R;
import android.atl.ATLLoadedApp;
import android.atl.annotation.Export;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.PackageParser;
import android.content.res.Configuration;
import android.os.Bundle;

public class Application extends ContextWrapper {
	public long native_window;

	@Export
	private String get_app_icon_path() {
		String icon_path = null;
		try {
			icon_path = getString(this.get_atl_loaded_app().pkg.applicationInfo.icon);
		} catch (android.content.res.Resources.NotFoundException e) {
			e.printStackTrace();
		}
		if (icon_path == null) {
			icon_path = getString(R.mipmap.sym_def_app_icon);
		} else if (icon_path.endsWith(".xml")) {
			icon_path = null;
		}
		return icon_path;
	}

	@Export
	private long get_app_icon_paintable() {
		return getPackageManager().getApplicationIcon(this.get_atl_loaded_app().pkg.applicationInfo).paintable;
	}

	@Export
	String get_app_label() {
		return getString(this.get_atl_loaded_app().pkg.applicationInfo.labelRes);
	}

	@Export
	String get_supported_mime_types() {
		StringBuilder mimeTypes = new StringBuilder();
		for (PackageParser.Activity activity : this.get_atl_loaded_app().pkg.activities) {
			if (activity.intents == null)
				continue;
			for (PackageParser.IntentInfo intent : activity.intents) {
				for (int i = 0; i < intent.countDataSchemes(); i++) {
					String scheme = intent.getDataScheme(i);
					// ignore http and https, as there is no way to only handle specific hosts in a .desktop file
					if (!"http".equals(scheme) && !"https".equals(scheme))
						mimeTypes.append("x-scheme-handler/").append(intent.getDataScheme(i)).append(";");
				}
			}
		}
		return (mimeTypes.length() == 0) ? null : mimeTypes.toString();
	}

	public interface ActivityLifecycleCallbacks {
		default void onActivityPreCreated(Activity activity, Bundle savedInstanceState) {}
		void onActivityCreated(Activity activity, Bundle savedInstanceState);
		default void onActivityPostCreated(Activity activity, Bundle savedInstanceState) {}
		default void onActivityPreStarted(Activity activity) {}
		void onActivityStarted(Activity activity);
		default void onActivityPostStarted(Activity activity) {}
		default void onActivityPreResumed(Activity activity) {}
		void onActivityResumed(Activity activity);
		default void onActivityPostResumed(Activity activity) {}
		default void onActivityPrePaused(Activity activity) {}
		void onActivityPaused(Activity activity);
		default void onActivityPostPaused(Activity activity) {}
		default void onActivityPreStopped(Activity activity) {}
		void onActivityStopped(Activity activity);
		default void onActivityPostStopped(Activity activity) {}
		default void onActivityPreSaveInstanceState(Activity activity, Bundle outState) {}
		void onActivitySaveInstanceState(Activity activity, Bundle outState);
		default void onActivityPostSaveInstanceState(Activity activity, Bundle outState) {}
		default void onActivityPreDestroyed(Activity activity) {}
		void onActivityDestroyed(Activity activity);
		default void onActivityPostDestroyed(Activity activity) {}
	}

	/* lifecycle events of Activity.dispatchActivityLifecycle(): event * 3 + phase */
	static final int CREATED = 0, STARTED = 1, RESUMED = 2, PAUSED = 3, STOPPED = 4, SAVE_INSTANCE_STATE = 5, DESTROYED = 6;
	static final int PRE = 0, MAIN = 1, POST = 2;

	static void callActivityLifecycle(ActivityLifecycleCallbacks cb, int event, int phase, Activity a, Bundle b) {
		switch (event * 3 + phase) {
			case CREATED * 3 + PRE: cb.onActivityPreCreated(a, b); break;
			case CREATED * 3 + MAIN: cb.onActivityCreated(a, b); break;
			case CREATED * 3 + POST: cb.onActivityPostCreated(a, b); break;
			case STARTED * 3 + PRE: cb.onActivityPreStarted(a); break;
			case STARTED * 3 + MAIN: cb.onActivityStarted(a); break;
			case STARTED * 3 + POST: cb.onActivityPostStarted(a); break;
			case RESUMED * 3 + PRE: cb.onActivityPreResumed(a); break;
			case RESUMED * 3 + MAIN: cb.onActivityResumed(a); break;
			case RESUMED * 3 + POST: cb.onActivityPostResumed(a); break;
			case PAUSED * 3 + PRE: cb.onActivityPrePaused(a); break;
			case PAUSED * 3 + MAIN: cb.onActivityPaused(a); break;
			case PAUSED * 3 + POST: cb.onActivityPostPaused(a); break;
			case STOPPED * 3 + PRE: cb.onActivityPreStopped(a); break;
			case STOPPED * 3 + MAIN: cb.onActivityStopped(a); break;
			case STOPPED * 3 + POST: cb.onActivityPostStopped(a); break;
			case SAVE_INSTANCE_STATE * 3 + PRE: cb.onActivityPreSaveInstanceState(a, b); break;
			case SAVE_INSTANCE_STATE * 3 + MAIN: cb.onActivitySaveInstanceState(a, b); break;
			case SAVE_INSTANCE_STATE * 3 + POST: cb.onActivityPostSaveInstanceState(a, b); break;
			case DESTROYED * 3 + PRE: cb.onActivityPreDestroyed(a); break;
			case DESTROYED * 3 + MAIN: cb.onActivityDestroyed(a); break;
			case DESTROYED * 3 + POST: cb.onActivityPostDestroyed(a); break;
		}
	}

	private final java.util.ArrayList<ActivityLifecycleCallbacks> activityLifecycleCallbacks = new java.util.ArrayList<>();

	void dispatchActivityLifecycle(int event, int phase, Activity activity, Bundle bundle) {
		Object[] callbacks;
		synchronized (activityLifecycleCallbacks) {
			callbacks = activityLifecycleCallbacks.toArray();
		}
		for (Object cb : callbacks)
			callActivityLifecycle((ActivityLifecycleCallbacks)cb, event, phase, activity, bundle);
	}
	/**
	 * Callback interface for use with {@link Application#registerOnProvideAssistDataListener}
	 * and {@link Application#unregisterOnProvideAssistDataListener}.
	 */
	public interface OnProvideAssistDataListener {
		/**
		 * This is called when the user is requesting an assist, to build a full
		 * {@link Intent#ACTION_ASSIST} Intent with all of the context of the current
		 * application.  You can override this method to place into the bundle anything
		 * you would like to appear in the {@link Intent#EXTRA_ASSIST_CONTEXT} part
		 * of the assist Intent.
		 */
		public void onProvideAssistData(Activity activity, Bundle data);
	}

	public Application() {
		super(null);
	}

	/**
	 * Called when the application is starting, before any activity, service,
	 * or receiver objects (excluding content providers) have been created.
	 * Implementations should be as quick as possible (for example using
	 * lazy initialization of state) since the time spent in this function
	 * directly impacts the performance of starting the first activity,
	 * service, or receiver in a process.
	 * If you override this method, be sure to call super.onCreate().
	 */
	@Export
	public void onCreate() {
	}
	/**
	 * This method is for use in emulated process environments.  It will
	 * never be called on a production Android device, where processes are
	 * removed by simply killing them; no user code (including this callback)
	 * is executed when doing so.
	 */
	public void onTerminate() {
	}
	public void onConfigurationChanged(Configuration newConfig) {
	}
	public void onLowMemory() {
	}
	public void onTrimMemory(int level) {
	}
	/*public void registerComponentCallbacks(ComponentCallbacks callback) {
	}
	public void unregisterComponentCallbacks(ComponentCallbacks callback) {
	}*/
	public void registerActivityLifecycleCallbacks(ActivityLifecycleCallbacks callback) {
		synchronized (activityLifecycleCallbacks) {
			activityLifecycleCallbacks.add(callback);
		}
	}
	public void unregisterActivityLifecycleCallbacks(ActivityLifecycleCallbacks callback) {
		synchronized (activityLifecycleCallbacks) {
			activityLifecycleCallbacks.remove(callback);
		}
	}
	public void registerOnProvideAssistDataListener(OnProvideAssistDataListener callback) {
	}
	public void unregisterOnProvideAssistDataListener(OnProvideAssistDataListener callback) {
	}
	public static String getProcessName() {
		// note: we currently don't set the process name
		return ATLLoadedApp.getPrimaryApplication().pkg.packageName;
	}
}
