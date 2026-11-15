/* AOSP external/cronet android/java/src/android/net/http/UrlRequest.java (android-16.0.0_r4) */
package android.net.http;

import android.annotation.IntDef;
import android.annotation.SuppressLint;
import android.net.Network;

import android.annotation.NonNull;
import android.annotation.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

public abstract class UrlRequest {

	UrlRequest() {}

	public static final int REQUEST_PRIORITY_IDLE = 0;

	public static final int REQUEST_PRIORITY_LOWEST = 1;

	public static final int REQUEST_PRIORITY_LOW = 2;

	public static final int REQUEST_PRIORITY_MEDIUM = 3;

	public static final int REQUEST_PRIORITY_HIGHEST = 4;

	@SuppressLint("StaticFinalBuilder")
	public abstract static class Builder {

		Builder() {}

		@NonNull
		public abstract Builder setHttpMethod(@NonNull String method);

		@NonNull
		public abstract Builder addHeader(@NonNull String header, @NonNull String value);

		@NonNull
		public abstract Builder setCacheDisabled(boolean disableCache);

		@NonNull
		public abstract Builder setPriority(int priority);

		@NonNull @SuppressLint("MissingGetterMatchingBuilder")
		public abstract Builder setUploadDataProvider(
				@NonNull UploadDataProvider uploadDataProvider, @NonNull Executor executor);

		@NonNull
		public abstract Builder setDirectExecutorAllowed(boolean allowDirectExecutor);

		@NonNull @SuppressLint("BuilderSetStyle")
		public abstract Builder bindToNetwork(@Nullable Network network);

		@NonNull
		public abstract Builder setTrafficStatsTag(int tag);

		@NonNull
		public abstract Builder setTrafficStatsUid(int uid);

		@NonNull
		public abstract UrlRequest build();
	}

	public interface Callback {

		@SuppressLint("GenericException")
		void onRedirectReceived(@NonNull UrlRequest request,
				@NonNull UrlResponseInfo info, @NonNull String newLocationUrl) throws Exception;

		@SuppressLint("GenericException")
		void onResponseStarted(@NonNull UrlRequest request,
				@NonNull UrlResponseInfo info) throws Exception;

		@SuppressLint("GenericException")
		void onReadCompleted(@NonNull UrlRequest request,
				@NonNull UrlResponseInfo info, @NonNull ByteBuffer byteBuffer) throws Exception;

		 void onSucceeded(
				@NonNull UrlRequest request, @NonNull UrlResponseInfo info);

		void onFailed(@NonNull UrlRequest request,
				@Nullable UrlResponseInfo info, @NonNull HttpException error);

		void onCanceled(@NonNull UrlRequest request, @Nullable UrlResponseInfo info);
	}

	@Retention(RetentionPolicy.SOURCE)
	@IntDef({
			Status.INVALID,
			Status.IDLE,
			Status.WAITING_FOR_STALLED_SOCKET_POOL,
			Status.WAITING_FOR_AVAILABLE_SOCKET,
			Status.WAITING_FOR_DELEGATE,
			Status.WAITING_FOR_CACHE,
			Status.DOWNLOADING_PAC_FILE,
			Status.RESOLVING_PROXY_FOR_URL,
			Status.RESOLVING_PROXY_FOR_URL,
			Status.RESOLVING_HOST_IN_PAC_FILE,
			Status.ESTABLISHING_PROXY_TUNNEL,
			Status.RESOLVING_HOST,
			Status.CONNECTING,
			Status.SSL_HANDSHAKE,
			Status.SENDING_REQUEST,
			Status.WAITING_FOR_RESPONSE,
			Status.READING_RESPONSE})
	public @interface UrlRequestStatus {}

	public static class Status {

		public static final int INVALID = -1;

		public static final int IDLE = 0;

		public static final int WAITING_FOR_STALLED_SOCKET_POOL = 1;

		public static final int WAITING_FOR_AVAILABLE_SOCKET = 2;

		public static final int WAITING_FOR_DELEGATE = 3;

		public static final int WAITING_FOR_CACHE = 4;

		public static final int DOWNLOADING_PAC_FILE = 5;

		public static final int RESOLVING_PROXY_FOR_URL = 6;

		public static final int RESOLVING_HOST_IN_PAC_FILE = 7;

		public static final int ESTABLISHING_PROXY_TUNNEL = 8;

		public static final int RESOLVING_HOST = 9;

		public static final int CONNECTING = 10;

		public static final int SSL_HANDSHAKE = 11;

		public static final int SENDING_REQUEST = 12;

		public static final int WAITING_FOR_RESPONSE = 13;

		public static final int READING_RESPONSE = 14;

		private Status() {}
	}

	public interface StatusListener {

		void onStatus(@UrlRequestStatus int status);
	}

	@Nullable
	public abstract String getHttpMethod();

	@NonNull
	public abstract HeaderBlock getHeaders();

	public abstract boolean isCacheDisabled();

	public abstract boolean isDirectExecutorAllowed();

	public abstract int getPriority();

	public abstract boolean hasTrafficStatsTag();

	public abstract int getTrafficStatsTag();

	public abstract boolean hasTrafficStatsUid();

	public abstract int getTrafficStatsUid();

	public abstract void start();

	public abstract void followRedirect();

	public abstract void read(@NonNull ByteBuffer buffer);

	public abstract void cancel();

	public abstract boolean isDone();

	@SuppressLint("ExecutorRegistration")
	public abstract void getStatus(@NonNull final StatusListener listener);

}
