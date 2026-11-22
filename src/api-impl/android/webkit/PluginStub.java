package android.webkit;

import android.content.Context;
import android.view.View;

/* AOSP frameworks/base core/java/android/webkit/PluginStub.java (Android 16): the interface of the long gone NPAPI
 * plugins; no WebView calls it */
public interface PluginStub {
	public abstract View getEmbeddedView(int NPP, Context context);

	public abstract View getFullScreenView(int NPP, Context context);
}
