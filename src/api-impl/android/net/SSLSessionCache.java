package android.net;

import java.io.File;

public class SSLSessionCache {

	public SSLSessionCache(File cacheDir) {}

	/* AOSP SSLSessionCache(Context): a cache in the app's private "sslcache" directory */
	public SSLSessionCache(android.content.Context context) {
		this(context.getDir("sslcache", android.content.Context.MODE_PRIVATE));
	}
}
