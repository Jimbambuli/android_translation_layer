package android.net.http;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.CacheRequest;
import java.net.CacheResponse;
import java.net.ResponseCache;
import java.net.URI;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;

/**
 * Stub: the AOSP implementation is backed by com.android.okhttp, which is not
 * part of our boot classpath. We accept install() and simply never cache.
 */
public final class HttpResponseCache extends ResponseCache implements Closeable {
	private static HttpResponseCache installed;
	private final File directory;
	private final long maxSize;

	private HttpResponseCache(File directory, long maxSize) {
		this.directory = directory;
		this.maxSize = maxSize;
	}

	public static HttpResponseCache getInstalled() {
		return installed;
	}

	public static synchronized HttpResponseCache install(File directory, long maxSize) throws IOException {
		if (installed != null && installed.directory.equals(directory)) {
			return installed;
		}
		installed = new HttpResponseCache(directory, maxSize);
		directory.mkdirs();
		ResponseCache.setDefault(installed);
		return installed;
	}

	@Override
	public CacheResponse get(URI uri, String requestMethod, Map<String, List<String>> requestHeaders) throws IOException {
		return null;
	}

	@Override
	public CacheRequest put(URI uri, URLConnection urlConnection) throws IOException {
		return null;
	}

	public long size() {
		return 0;
	}

	public long maxSize() {
		return maxSize;
	}

	public void flush() {}

	public int getNetworkCount() {
		return 0;
	}

	public int getHitCount() {
		return 0;
	}

	public int getRequestCount() {
		return 0;
	}

	@Override
	public void close() throws IOException {
		if (ResponseCache.getDefault() == this) {
			ResponseCache.setDefault(null);
			installed = null;
		}
	}

	public void delete() throws IOException {
		close();
	}
}
