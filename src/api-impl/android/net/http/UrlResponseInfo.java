/* AOSP external/cronet android/java/src/android/net/http/UrlResponseInfo.java (android-16.0.0_r4) */
package android.net.http;

import android.annotation.NonNull;
import android.annotation.Nullable;

import java.util.List;
import java.util.Map;

public abstract class UrlResponseInfo {

	@NonNull
	public abstract String getUrl();

	@NonNull
	public abstract List<String> getUrlChain();

	public abstract int getHttpStatusCode();

	@NonNull
	public abstract String getHttpStatusText();

	@NonNull
	public abstract HeaderBlock getHeaders();

	public abstract boolean wasCached();

	@NonNull
	public abstract String getNegotiatedProtocol();

	public abstract long getReceivedByteCount();
}
