package android.app;

import android.R;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.atl.ATLLoadedApp;
import android.atl.annotation.Export;
import android.content.ComponentCallbacks;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.PackageParser;
import android.content.res.Configuration;
import android.os.Bundle;
import java.util.ArrayList;

public class Application extends ContextWrapper {
	public long native_window;

	@Export
	private String get_app_icon_path() {
		String icon_path = null;
		int icon = this.get_atl_loaded_app().pkg.applicationInfo.icon;
		try {
			if (icon != 0) // no android:icon: the default icon below
				icon_path = getString(icon);
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

	private ArrayList<ActivityLifecycleCallbacks> mActivityLifecycleCallbacks =
			new ArrayList<ActivityLifecycleCallbacks>();

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
	/* AOSP Application: the ComponentCallbacks registered with any context of the app hear of configuration
	 * changes and memory pressure */
	private final ArrayList<ComponentCallbacks> mComponentCallbacks = new ArrayList<ComponentCallbacks>();

	private Object[] collectComponentCallbacks() {
		synchronized (mComponentCallbacks) {
			return mComponentCallbacks.isEmpty() ? null : mComponentCallbacks.toArray();
		}
	}

	public void onConfigurationChanged(Configuration newConfig) {
		Object[] callbacks = collectComponentCallbacks();
		if (callbacks != null) {
			for (Object c : callbacks)
				((ComponentCallbacks)c).onConfigurationChanged(newConfig);
		}
	}
	public void onLowMemory() {
		Object[] callbacks = collectComponentCallbacks();
		if (callbacks != null) {
			for (Object c : callbacks)
				((ComponentCallbacks)c).onLowMemory();
		}
	}
	public void onTrimMemory(int level) {
		Object[] callbacks = collectComponentCallbacks();
		if (callbacks != null) {
			for (Object c : callbacks) {
				if (c instanceof android.content.ComponentCallbacks2)
					((android.content.ComponentCallbacks2)c).onTrimMemory(level);
			}
		}
	}
	@Override
	public void registerComponentCallbacks(ComponentCallbacks callback) {
		synchronized (mComponentCallbacks) {
			mComponentCallbacks.add(callback);
		}
	}
	@Override
	public void unregisterComponentCallbacks(ComponentCallbacks callback) {
		synchronized (mComponentCallbacks) {
			mComponentCallbacks.remove(callback);
		}
	}
	public void registerActivityLifecycleCallbacks(ActivityLifecycleCallbacks callback) {
		synchronized (mActivityLifecycleCallbacks) {
			mActivityLifecycleCallbacks.add(callback);
		}
	}
	public void unregisterActivityLifecycleCallbacks(ActivityLifecycleCallbacks callback) {
		synchronized (mActivityLifecycleCallbacks) {
			mActivityLifecycleCallbacks.remove(callback);
		}
	}

	/* package */ void dispatchActivityPreCreated(@NonNull Activity activity,
			@Nullable Bundle savedInstanceState) {
		Object[] callbacks = collectActivityLifecycleCallbacks();
		if (callbacks != null) {
			for (int i = 0; i < callbacks.length; i++) {
				((ActivityLifecycleCallbacks) callbacks[i]).onActivityPreCreated(activity,
						savedInstanceState);
			}
		}
	}

	/* package */ void dispatchActivityCreated(@NonNull Activity activity,
			@Nullable Bundle savedInstanceState) {
		Object[] callbacks = collectActivityLifecycleCallbacks();
		if (callbacks != null) {
			for (int i=0; i<callbacks.length; i++) {
				((ActivityLifecycleCallbacks)callbacks[i]).onActivityCreated(activity,
						savedInstanceState);
			}
		}
	}

	/* package */ void dispatchActivityPostCreated(@NonNull Activity activity,
			@Nullable Bundle savedInstanceState) {
		Object[] callbacks = collectActivityLifecycleCallbacks();
		if (callbacks != null) {
			for (int i = 0; i < callbacks.length; i++) {
				((ActivityLifecycleCallbacks) callbacks[i]).onActivityPostCreated(activity,
						savedInstanceState);
			}
		}
	}

	/* package */ void dispatchActivityPreStarted(@NonNull Activity activity) {
		Object[] callbacks = collectActivityLifecycleCallbacks();
		if (callbacks != null) {
			for (int i = 0; i < callbacks.length; i++) {
				((ActivityLifecycleCallbacks) callbacks[i]).onActivityPreStarted(activity);
			}
		}
	}

	/* package */ void dispatchActivityStarted(@NonNull Activity activity) {
		Object[] callbacks = collectActivityLifecycleCallbacks();
		if (callbacks != null) {
			for (int i=0; i<callbacks.length; i++) {
				((ActivityLifecycleCallbacks)callbacks[i]).onActivityStarted(activity);
			}
		}
	}

	/* package */ void dispatchActivityPostStarted(@NonNull Activity activity) {
		Object[] callbacks = collectActivityLifecycleCallbacks();
		if (callbacks != null) {
			for (int i = 0; i < callbacks.length; i++) {
				((ActivityLifecycleCallbacks) callbacks[i]).onActivityPostStarted(activity);
			}
		}
	}

	/* package */ void dispatchActivityPreResumed(@NonNull Activity activity) {
		Object[] callbacks = collectActivityLifecycleCallbacks();
		if (callbacks != null) {
			for (int i = 0; i < callbacks.length; i++) {
				((ActivityLifecycleCallbacks) callbacks[i]).onActivityPreResumed(activity);
			}
		}
	}

	/* package */ void dispatchActivityResumed(@NonNull Activity activity) {
		Object[] callbacks = collectActivityLifecycleCallbacks();
		if (callbacks != null) {
			for (int i=0; i<callbacks.length; i++) {
				((ActivityLifecycleCallbacks)callbacks[i]).onActivityResumed(activity);
			}
		}
	}

	/* package */ void dispatchActivityPostResumed(@NonNull Activity activity) {
		Object[] callbacks = collectActivityLifecycleCallbacks();
		if (callbacks != null) {
			for (int i = 0; i < callbacks.length; i++) {
				((ActivityLifecycleCallbacks) callbacks[i]).onActivityPostResumed(activity);
			}
		}
	}

	/* package */ void dispatchActivityPrePaused(@NonNull Activity activity) {
		Object[] callbacks = collectActivityLifecycleCallbacks();
		if (callbacks != null) {
			for (int i = 0; i < callbacks.length; i++) {
				((ActivityLifecycleCallbacks) callbacks[i]).onActivityPrePaused(activity);
			}
		}
	}

	/* package */ void dispatchActivityPaused(@NonNull Activity activity) {
		Object[] callbacks = collectActivityLifecycleCallbacks();
		if (callbacks != null) {
			for (int i=0; i<callbacks.length; i++) {
				((ActivityLifecycleCallbacks)callbacks[i]).onActivityPaused(activity);
			}
		}
	}

	/* package */ void dispatchActivityPostPaused(@NonNull Activity activity) {
		Object[] callbacks = collectActivityLifecycleCallbacks();
		if (callbacks != null) {
			for (int i = 0; i < callbacks.length; i++) {
				((ActivityLifecycleCallbacks) callbacks[i]).onActivityPostPaused(activity);
			}
		}
	}

	/* package */ void dispatchActivityPreStopped(@NonNull Activity activity) {
		Object[] callbacks = collectActivityLifecycleCallbacks();
		if (callbacks != null) {
			for (int i = 0; i < callbacks.length; i++) {
				((ActivityLifecycleCallbacks) callbacks[i]).onActivityPreStopped(activity);
			}
		}
	}

	/* package */ void dispatchActivityStopped(@NonNull Activity activity) {
		Object[] callbacks = collectActivityLifecycleCallbacks();
		if (callbacks != null) {
			for (int i=0; i<callbacks.length; i++) {
				((ActivityLifecycleCallbacks)callbacks[i]).onActivityStopped(activity);
			}
		}
	}

	/* package */ void dispatchActivityPostStopped(@NonNull Activity activity) {
		Object[] callbacks = collectActivityLifecycleCallbacks();
		if (callbacks != null) {
			for (int i = 0; i < callbacks.length; i++) {
				((ActivityLifecycleCallbacks) callbacks[i]).onActivityPostStopped(activity);
			}
		}
	}

	/* package */ void dispatchActivityPreSaveInstanceState(@NonNull Activity activity,
			@NonNull Bundle outState) {
		Object[] callbacks = collectActivityLifecycleCallbacks();
		if (callbacks != null) {
			for (int i = 0; i < callbacks.length; i++) {
				((ActivityLifecycleCallbacks) callbacks[i]).onActivityPreSaveInstanceState(
						activity, outState);
			}
		}
	}

	/* package */ void dispatchActivitySaveInstanceState(@NonNull Activity activity,
			@NonNull Bundle outState) {
		Object[] callbacks = collectActivityLifecycleCallbacks();
		if (callbacks != null) {
			for (int i=0; i<callbacks.length; i++) {
				((ActivityLifecycleCallbacks)callbacks[i]).onActivitySaveInstanceState(activity,
						outState);
			}
		}
	}

	/* package */ void dispatchActivityPostSaveInstanceState(@NonNull Activity activity,
			@NonNull Bundle outState) {
		Object[] callbacks = collectActivityLifecycleCallbacks();
		if (callbacks != null) {
			for (int i = 0; i < callbacks.length; i++) {
				((ActivityLifecycleCallbacks) callbacks[i]).onActivityPostSaveInstanceState(
						activity, outState);
			}
		}
	}

	/* package */ void dispatchActivityPreDestroyed(@NonNull Activity activity) {
		Object[] callbacks = collectActivityLifecycleCallbacks();
		if (callbacks != null) {
			for (int i = 0; i < callbacks.length; i++) {
				((ActivityLifecycleCallbacks) callbacks[i]).onActivityPreDestroyed(activity);
			}
		}
	}

	/* package */ void dispatchActivityDestroyed(@NonNull Activity activity) {
		Object[] callbacks = collectActivityLifecycleCallbacks();
		if (callbacks != null) {
			for (int i=0; i<callbacks.length; i++) {
				((ActivityLifecycleCallbacks)callbacks[i]).onActivityDestroyed(activity);
			}
		}
	}

	/* package */ void dispatchActivityPostDestroyed(@NonNull Activity activity) {
		Object[] callbacks = collectActivityLifecycleCallbacks();
		if (callbacks != null) {
			for (int i = 0; i < callbacks.length; i++) {
				((ActivityLifecycleCallbacks) callbacks[i]).onActivityPostDestroyed(activity);
			}
		}
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

	public void registerOnProvideAssistDataListener(OnProvideAssistDataListener callback) {
	}
	public void unregisterOnProvideAssistDataListener(OnProvideAssistDataListener callback) {
	}
	public static String getProcessName() {
		return android.atl.ATLProcess.myProcessName();
	}
}
