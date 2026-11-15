/* AOSP external/cronet android/java/src/android/net/http/BidirectionalStream.java (android-16.0.0_r4) */
package android.net.http;

import android.annotation.IntDef;
import android.annotation.SuppressLint;

import android.annotation.NonNull;
import android.annotation.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

public abstract class BidirectionalStream {

	public static final int STREAM_PRIORITY_IDLE = 0;

	public static final int STREAM_PRIORITY_LOWEST = 1;

	public static final int STREAM_PRIORITY_LOW = 2;

	public static final int STREAM_PRIORITY_MEDIUM = 3;

	public static final int STREAM_PRIORITY_HIGHEST = 4;

	@Retention(RetentionPolicy.SOURCE)
	@IntDef({
			BidirectionalStream.STREAM_PRIORITY_IDLE,
			BidirectionalStream.STREAM_PRIORITY_LOWEST,
			BidirectionalStream.STREAM_PRIORITY_LOW,
			BidirectionalStream.STREAM_PRIORITY_MEDIUM,
			BidirectionalStream.STREAM_PRIORITY_HIGHEST})
	public @interface BidirectionalStreamPriority {}

	@SuppressLint("StaticFinalBuilder")
	public abstract static class Builder {

		@NonNull
		public abstract Builder setHttpMethod(@NonNull String method);

		@NonNull
		public abstract Builder addHeader(@NonNull String header, @NonNull String value);

		@NonNull
		public abstract Builder setPriority(@BidirectionalStreamPriority int priority);

		@NonNull
		public abstract Builder setDelayRequestHeadersUntilFirstFlushEnabled(
				boolean delayRequestHeadersUntilFirstFlush);

		@NonNull
		public abstract Builder setTrafficStatsTag(int tag);

		@NonNull
		public abstract Builder setTrafficStatsUid(int uid);

		@NonNull
		public abstract BidirectionalStream build();
	
}

	public interface Callback {

		void onStreamReady(@NonNull BidirectionalStream stream);

		void onResponseHeadersReceived(@NonNull BidirectionalStream stream,
				@NonNull UrlResponseInfo info);

		void onReadCompleted(@NonNull BidirectionalStream stream, @NonNull UrlResponseInfo info,
				@NonNull ByteBuffer buffer, boolean endOfStream);

		void onWriteCompleted(@NonNull BidirectionalStream stream, @NonNull UrlResponseInfo info,
				@NonNull ByteBuffer buffer, boolean endOfStream);

		void onResponseTrailersReceived(@NonNull BidirectionalStream stream,
				@NonNull UrlResponseInfo info, @NonNull HeaderBlock trailers);

		void onSucceeded(@NonNull BidirectionalStream stream, @NonNull UrlResponseInfo info);

		void onFailed(@NonNull BidirectionalStream stream, @Nullable UrlResponseInfo info,
				@NonNull HttpException error);

		void onCanceled(@NonNull BidirectionalStream stream, @Nullable UrlResponseInfo info);
	}

	@NonNull
	public abstract String getHttpMethod();

	public abstract boolean hasTrafficStatsTag();

	public abstract int getTrafficStatsTag();

	public abstract boolean hasTrafficStatsUid();

	public abstract int getTrafficStatsUid();

	@NonNull
	public abstract HeaderBlock getHeaders();

	public abstract @BidirectionalStreamPriority int getPriority();

	public abstract boolean isDelayRequestHeadersUntilFirstFlushEnabled();

	public abstract void start();

	public abstract void read(@NonNull ByteBuffer buffer);

	public abstract void write(@NonNull ByteBuffer buffer, boolean endOfStream);

	public abstract void flush();

	public abstract void cancel();

	public abstract boolean isDone();
    /* AOSP 16 API; no backing hardware/service in ATL */
    public BidirectionalStream() {}
}
