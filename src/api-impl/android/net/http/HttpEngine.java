/* AOSP external/cronet android/java/src/android/net/http/HttpEngine.java (android-16.0.0_r4). AOSP builds the engine
 * on Cronet (org.chromium.net, native library); ATL has none, so build() returns an engine on the platform's
 * HttpURLConnection (HttpUrlConnectionEngine): no QUIC, no HTTP/2 control, no bidirectional streams. */
package android.net.http;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Network;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandlerFactory;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.Executor;

@SuppressLint("NotCloseable")
public abstract class HttpEngine {
	protected HttpEngine() {}

	@NonNull
	public static Builder builder(@NonNull Context context) {
		return new Builder(context);
	}

	@SuppressLint("StaticFinalBuilder")
	public static class Builder {
		public static final int HTTP_CACHE_DISABLED = 0;

		public static final int HTTP_CACHE_IN_MEMORY = 1;

		public static final int HTTP_CACHE_DISK_NO_HTTP = 2;

		public static final int HTTP_CACHE_DISK = 3;

		private final Context mContext;
		private String mUserAgent;
		private String mStoragePath;
		private boolean mEnableQuic;
		private boolean mEnableHttp2 = true;
		private boolean mEnableBrotli;
		private int mHttpCacheMode = HTTP_CACHE_DISABLED;
		private long mHttpCacheMaxSize;

		public Builder(@NonNull Context context) {
			mContext = context;
		}

		/* AOSP: UserAgent.from(context), the app name and version plus system and Cronet build; ATL has the
		 * platform's own agent string */
		@NonNull
		public String getDefaultUserAgent() {
			String agent = System.getProperty("http.agent");
			return agent != null ? agent : mContext.getPackageName();
		}

		@NonNull
		public Builder setUserAgent(@NonNull String userAgent) {
			mUserAgent = userAgent;
			return this;
		}

		@NonNull
		public Builder setStoragePath(@NonNull String value) {
			if (!new java.io.File(value).isDirectory()) {
				throw new IllegalArgumentException("Storage path must be set to existing directory");
			}
			mStoragePath = value;
			return this;
		}

		@NonNull
		public Builder setEnableQuic(boolean value) {
			mEnableQuic = value;
			return this;
		}

		@NonNull
		public Builder setEnableHttp2(boolean value) {
			mEnableHttp2 = value;
			return this;
		}

		@NonNull
		public Builder setEnableBrotli(boolean value) {
			mEnableBrotli = value;
			return this;
		}

		@NonNull
		public Builder setEnableHttpCache(int cacheMode, long maxSize) {
			if (cacheMode == HTTP_CACHE_DISK || cacheMode == HTTP_CACHE_DISK_NO_HTTP) {
				if (mStoragePath == null) {
					throw new IllegalArgumentException("Storage path must be set");
				}
			}
			mHttpCacheMode = cacheMode;
			mHttpCacheMaxSize = maxSize;
			return this;
		}

		@NonNull
		public Builder addQuicHint(@NonNull String host, int port, int alternatePort) {
			if (host.contains("/")) {
				throw new IllegalArgumentException("Illegal QUIC Hint Host: " + host);
			}
			return this;
		}

		@NonNull
		public Builder addPublicKeyPins(@NonNull String hostName, @NonNull Set<byte[]> pinsSha256,
				boolean includeSubdomains, @NonNull Instant expirationInstant) {
			if (hostName == null) {
				throw new NullPointerException("The hostname cannot be null");
			}
			if (pinsSha256 == null) {
				throw new NullPointerException("The set of SHA256 pins cannot be null");
			}
			if (expirationInstant == null) {
				throw new NullPointerException("The pin expiration date cannot be null");
			}
			return this;
		}

		@NonNull
		public Builder setEnablePublicKeyPinningBypassForLocalTrustAnchors(boolean value) {
			return this;
		}

		@NonNull
		public Builder setQuicOptions(@NonNull QuicOptions quicOptions) {
			return this;
		}

		@NonNull
		public Builder setDnsOptions(@NonNull DnsOptions dnsOptions) {
			return this;
		}

		@NonNull
		public Builder setConnectionMigrationOptions(
				@NonNull ConnectionMigrationOptions connectionMigrationOptions) {
			return this;
		}

		@NonNull
		public HttpEngine build() {
			return new HttpUrlConnectionEngine(mUserAgent != null ? mUserAgent : getDefaultUserAgent(),
			                                   mHttpCacheMode != HTTP_CACHE_DISABLED);
		}
	}

	/* AOSP: ApiVersion.getCronetVersion(), the Cronet build; there is none on ATL */
	@NonNull
	public static String getVersionString() {
		return "ATL-HttpURLConnection";
	}

	public abstract void shutdown();

	public void bindToNetwork(@Nullable Network network) {}

	@SuppressLint("AndroidUri") @NonNull
	public abstract URLConnection openConnection(
			@SuppressLint("AndroidUri") @NonNull URL url) throws IOException;

	@SuppressLint("AndroidUri") @NonNull
	public abstract URLStreamHandlerFactory createUrlStreamHandlerFactory();

	@NonNull
	public abstract UrlRequest.Builder newUrlRequestBuilder(
			@NonNull String url, @NonNull Executor executor, @NonNull UrlRequest.Callback callback);

	@NonNull
	public abstract BidirectionalStream.Builder newBidirectionalStreamBuilder(
			@NonNull String url, @NonNull Executor executor,
			@NonNull BidirectionalStream.Callback callback);
}
