package android.content;

import android.content.res.Configuration;

/* AOSP android.content.ComponentCallbacks */
public interface ComponentCallbacks {
	void onConfigurationChanged(Configuration newConfig);

	void onLowMemory();
}
