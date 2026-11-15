/* HttpEngine on the platform's HttpURLConnection, in place of AOSP's Cronet engine (external/cronet
 * stable/components/cronet/android/java/src/org/chromium/net/impl: CronetUrlRequestContext, CronetUrlRequest,
 * UrlRequestBuilderImpl). The callback protocol of CronetUrlRequest is kept: onRedirectReceived waits for
 * followRedirect(), onResponseStarted and onReadCompleted wait for read(), onSucceeded follows the read that hits the
 * end of the body, a throwing non-final callback fails the request with a CallbackException. Not available over
 * HttpURLConnection: QUIC, HTTP/2 control, BidirectionalStream, public key pins, traffic stats tagging. */
package android.net.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.NoRouteToHostException;
import java.net.Proxy;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;

final class HttpUrlConnectionEngine extends HttpEngine {
	/* net::URLRequest::kMaxRedirects */
	private static final int MAX_REDIRECTS = 20;
	private static final int BUFFER_SIZE = 8192;

	private final String mUserAgent;
	private final boolean mUseCaches;
	private final ExecutorService mIo;
	private int mRunningRequests;

	HttpUrlConnectionEngine(String userAgent, boolean useCaches) {
		mUserAgent = userAgent;
		mUseCaches = useCaches;
		mIo = Executors.newCachedThreadPool(new ThreadFactory() {
			@Override
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r, "HttpEngine-io");
				t.setDaemon(true);
				return t;
			}
		});
	}

	/* CronetUrlRequestContext.shutdown() */
	@Override
	public void shutdown() {
		synchronized (this) {
			if (mRunningRequests != 0) {
				throw new IllegalStateException("Cannot shutdown with running requests.");
			}
		}
		mIo.shutdown();
	}

	/* CronetUrlRequestContext.openConnection(URL) */
	@Override
	public URLConnection openConnection(URL url) throws IOException {
		String protocol = url.getProtocol();
		if ("http".equals(protocol) || "https".equals(protocol)) {
			return url.openConnection(Proxy.NO_PROXY);
		}
		throw new UnsupportedOperationException("Unexpected protocol:" + protocol);
	}

	/* AOSP hands out handlers that run on the engine; this engine is the platform's own stack, so a factory that
	 * creates no handler (URL then uses the platform's) is the same thing */
	@Override
	public URLStreamHandlerFactory createUrlStreamHandlerFactory() {
		return new URLStreamHandlerFactory() {
			@Override
			public URLStreamHandler createURLStreamHandler(String protocol) {
				return null;
			}
		};
	}

	@Override
	public UrlRequest.Builder newUrlRequestBuilder(String url, Executor executor, UrlRequest.Callback callback) {
		return new RequestBuilder(this, url, executor, callback);
	}

	/* the platform has no HTTP/2 stream API */
	@Override
	public BidirectionalStream.Builder newBidirectionalStreamBuilder(String url, Executor executor,
	                                                                 BidirectionalStream.Callback callback) {
		throw new UnsupportedOperationException("BidirectionalStream is not supported on this HttpEngine");
	}

	private synchronized void requestStarted() {
		mRunningRequests++;
	}

	private synchronized void requestFinished() {
		mRunningRequests--;
	}

	private static final class CallbackExceptionImpl extends CallbackException {
		CallbackExceptionImpl(String message, Throwable cause) {
			super(message, cause);
		}
	}

	private static final class NetworkExceptionImpl extends NetworkException {
		private final int mErrorCode;

		NetworkExceptionImpl(String message, Throwable cause, int errorCode) {
			super(message, cause);
			mErrorCode = errorCode;
		}

		@Override
		public int getErrorCode() {
			return mErrorCode;
		}

		@Override
		public boolean isImmediatelyRetryable() {
			return mErrorCode == ERROR_NETWORK_CHANGED;
		}
	}

	private static final class ResponseInfo extends UrlResponseInfo {
		private final List<String> mUrlChain;
		private final int mStatusCode;
		private final String mStatusText;
		private final HeaderBlock mHeaders;
		private final long mReceivedByteCount;

		ResponseInfo(List<String> urlChain, int statusCode, String statusText, HeaderBlock headers, long received) {
			mUrlChain = Collections.unmodifiableList(new ArrayList<String>(urlChain));
			mStatusCode = statusCode;
			mStatusText = statusText;
			mHeaders = headers;
			mReceivedByteCount = received;
		}

		@Override
		public String getUrl() {
			return mUrlChain.get(mUrlChain.size() - 1);
		}

		@Override
		public List<String> getUrlChain() {
			return mUrlChain;
		}

		@Override
		public int getHttpStatusCode() {
			return mStatusCode;
		}

		@Override
		public String getHttpStatusText() {
			return mStatusText;
		}

		@Override
		public HeaderBlock getHeaders() {
			return mHeaders;
		}

		@Override
		public boolean wasCached() {
			return false;
		}

		/* HttpURLConnection does not say which protocol it spoke; it speaks HTTP/1.1 itself */
		@Override
		public String getNegotiatedProtocol() {
			return "http/1.1";
		}

		/* bytes of body read; Cronet counts the bytes received on the wire */
		@Override
		public long getReceivedByteCount() {
			return mReceivedByteCount;
		}
	}

	private static final class RequestBuilder extends UrlRequest.Builder {
		private final HttpUrlConnectionEngine mEngine;
		private final String mUrl;
		private final UrlRequest.Callback mCallback;
		private final Executor mExecutor;
		private String mMethod;
		private final ArrayList<Map.Entry<String, String>> mHeaders = new ArrayList<Map.Entry<String, String>>();
		private boolean mDisableCache;
		private int mPriority = UrlRequest.REQUEST_PRIORITY_MEDIUM;
		private UploadDataProvider mUploadDataProvider;
		private Executor mUploadDataProviderExecutor;
		private boolean mAllowDirectExecutor;
		private boolean mTrafficStatsTagSet;
		private int mTrafficStatsTag;
		private boolean mTrafficStatsUidSet;
		private int mTrafficStatsUid;

		RequestBuilder(HttpUrlConnectionEngine engine, String url, Executor executor, UrlRequest.Callback callback) {
			mEngine = engine;
			mUrl = Objects.requireNonNull(url, "URL is required.");
			mCallback = Objects.requireNonNull(callback, "Callback is required.");
			mExecutor = Objects.requireNonNull(executor, "Executor is required.");
		}

		@Override
		public UrlRequest.Builder setHttpMethod(String method) {
			mMethod = Objects.requireNonNull(method, "Method is required.");
			return this;
		}

		/* UrlRequestBuilderImpl.addHeader(): Accept-Encoding is dropped, the stack sets it itself */
		@Override
		public UrlRequest.Builder addHeader(String header, String value) {
			Objects.requireNonNull(header, "Invalid header name.");
			Objects.requireNonNull(value, "Invalid header value.");
			if ("Accept-Encoding".equalsIgnoreCase(header)) {
				return this;
			}
			mHeaders.add(new AbstractMap.SimpleEntry<String, String>(header, value));
			return this;
		}

		@Override
		public UrlRequest.Builder setCacheDisabled(boolean disableCache) {
			mDisableCache = disableCache;
			return this;
		}

		@Override
		public UrlRequest.Builder setPriority(int priority) {
			mPriority = priority;
			return this;
		}

		/* UrlRequestBuilderImpl.setUploadDataProvider(): the method defaults to POST */
		@Override
		public UrlRequest.Builder setUploadDataProvider(UploadDataProvider uploadDataProvider, Executor executor) {
			mUploadDataProvider = Objects.requireNonNull(uploadDataProvider, "Invalid UploadDataProvider.");
			mUploadDataProviderExecutor = Objects.requireNonNull(executor, "Invalid UploadDataProvider Executor.");
			if (mMethod == null) {
				mMethod = "POST";
			}
			return this;
		}

		@Override
		public UrlRequest.Builder setDirectExecutorAllowed(boolean allowDirectExecutor) {
			mAllowDirectExecutor = allowDirectExecutor;
			return this;
		}

		/* ATL has one network */
		@Override
		public UrlRequest.Builder bindToNetwork(android.net.Network network) {
			return this;
		}

		@Override
		public UrlRequest.Builder setTrafficStatsTag(int tag) {
			mTrafficStatsTagSet = true;
			mTrafficStatsTag = tag;
			return this;
		}

		@Override
		public UrlRequest.Builder setTrafficStatsUid(int uid) {
			mTrafficStatsUidSet = true;
			mTrafficStatsUid = uid;
			return this;
		}

		/* UrlRequestBuilderImpl.build(): "default to get if not set" */
		@Override
		public UrlRequest build() {
			return new Request(this, mMethod == null ? "GET" : mMethod);
		}
	}

	private static final class Request extends UrlRequest {
		private final HttpUrlConnectionEngine mEngine;
		private final String mInitialUrl;
		private final UrlRequest.Callback mCallback;
		private final Executor mExecutor;
		private final String mMethod;
		private final List<Map.Entry<String, String>> mRequestHeaders;
		private final boolean mCacheDisabled;
		private final int mPriority;
		private final UploadDataProvider mUploadDataProvider;
		private final Executor mUploadExecutor;
		private final boolean mAllowDirectExecutor;
		private final boolean mTrafficStatsTagSet;
		private final int mTrafficStatsTag;
		private final boolean mTrafficStatsUidSet;
		private final int mTrafficStatsUid;
		private final Sink mSink = new Sink();

		/* all below guarded by mLock */
		private final Object mLock = new Object();
		private boolean mStarted;
		private boolean mDone;
		private boolean mWaitingOnRedirect;
		private boolean mWaitingOnRead;
		private ByteBuffer mReadBuffer;
		private int mStatus = Status.IDLE;
		private HttpURLConnection mConnection;
		private ResponseInfo mInfo;
		private long mReceived;
		private final ArrayList<String> mUrlChain = new ArrayList<String>();
		private Thread mWorker;

		Request(RequestBuilder b, String method) {
			mEngine = b.mEngine;
			mInitialUrl = b.mUrl;
			mCallback = b.mCallback;
			mExecutor = b.mExecutor;
			mMethod = method;
			mRequestHeaders = new ArrayList<Map.Entry<String, String>>(b.mHeaders);
			mCacheDisabled = b.mDisableCache;
			mPriority = b.mPriority;
			mUploadDataProvider = b.mUploadDataProvider;
			mUploadExecutor = b.mUploadDataProviderExecutor;
			mAllowDirectExecutor = b.mAllowDirectExecutor;
			mTrafficStatsTagSet = b.mTrafficStatsTagSet;
			mTrafficStatsTag = b.mTrafficStatsTag;
			mTrafficStatsUidSet = b.mTrafficStatsUidSet;
			mTrafficStatsUid = b.mTrafficStatsUid;
		}

		private interface Action {
			void run() throws Exception;
		}

		/* the upload provider answers through this sink, the worker thread waits for the answer */
		private final class Sink extends UploadDataSink {
			private boolean mArmed;
			private boolean mResult;
			private boolean mFinal;
			private Exception mError;

			void arm() {
				synchronized (mLock) {
					mArmed = true;
					mResult = false;
					mFinal = false;
					mError = null;
				}
			}

			private void complete(boolean finalChunk, Exception error) {
				synchronized (mLock) {
					if (!mArmed) {
						throw new IllegalStateException("Unexpected upload result invocation");
					}
					mArmed = false;
					mResult = true;
					mFinal = finalChunk;
					mError = error;
					mLock.notifyAll();
				}
			}

			@Override
			public void onReadSucceeded(boolean finalChunk) {
				complete(finalChunk, null);
			}

			@Override
			public void onReadError(Exception exception) {
				complete(false, exception);
			}

			@Override
			public void onRewindSucceeded() {
				complete(false, null);
			}

			@Override
			public void onRewindError(Exception exception) {
				complete(false, exception);
			}
		}

		@Override
		public String getHttpMethod() {
			return mMethod;
		}

		@Override
		public HeaderBlock getHeaders() {
			return new HeaderBlockImpl(new ArrayList<Map.Entry<String, String>>(mRequestHeaders));
		}

		@Override
		public boolean isCacheDisabled() {
			return mCacheDisabled;
		}

		@Override
		public boolean isDirectExecutorAllowed() {
			return mAllowDirectExecutor;
		}

		@Override
		public int getPriority() {
			return mPriority;
		}

		@Override
		public boolean hasTrafficStatsTag() {
			return mTrafficStatsTagSet;
		}

		@Override
		public int getTrafficStatsTag() {
			if (!mTrafficStatsTagSet) {
				throw new IllegalStateException("TrafficStatsTag is not set");
			}
			return mTrafficStatsTag;
		}

		@Override
		public boolean hasTrafficStatsUid() {
			return mTrafficStatsUidSet;
		}

		@Override
		public int getTrafficStatsUid() {
			if (!mTrafficStatsUidSet) {
				throw new IllegalStateException("TrafficStatsUid is not set");
			}
			return mTrafficStatsUid;
		}

		/* CronetUrlRequest.start() */
		@Override
		public void start() {
			synchronized (mLock) {
				if (mStarted || mDone) {
					throw new IllegalStateException("Request is already started.");
				}
				if (mUploadDataProvider != null) {
					boolean hasContentType = false;
					for (Map.Entry<String, String> header : mRequestHeaders) {
						if (header.getKey().equalsIgnoreCase("Content-Type") && !header.getValue().isEmpty()) {
							hasContentType = true;
						}
					}
					if (!hasContentType) {
						throw new IllegalArgumentException("Requests with upload data must have a Content-Type.");
					}
				}
				mStarted = true;
			}
			mEngine.requestStarted();
			try {
				mEngine.mIo.execute(new Runnable() {
					@Override
					public void run() {
						work();
					}
				});
			} catch (RejectedExecutionException e) {
				fail(new HttpException("Exception posting task to executor", e));
			}
		}

		/* CronetUrlRequest.followRedirect() */
		@Override
		public void followRedirect() {
			synchronized (mLock) {
				if (!mWaitingOnRedirect) {
					throw new IllegalStateException("No redirect to follow.");
				}
				mWaitingOnRedirect = false;
				mLock.notifyAll();
			}
		}

		/* CronetUrlRequest.read() with Preconditions.checkHasRemaining() and checkDirect() */
		@Override
		public void read(ByteBuffer buffer) {
			if (!buffer.hasRemaining()) {
				throw new IllegalArgumentException("ByteBuffer is already full.");
			}
			if (!buffer.isDirect()) {
				throw new IllegalArgumentException("byteBuffer must be a direct ByteBuffer.");
			}
			synchronized (mLock) {
				if (!mWaitingOnRead) {
					throw new IllegalStateException("Unexpected read attempt.");
				}
				mWaitingOnRead = false;
				if (mDone) {
					return;
				}
				mReadBuffer = buffer;
				mLock.notifyAll();
			}
		}

		/* CronetUrlRequest.cancel(): nothing before start() or after the end */
		@Override
		public void cancel() {
			ResponseInfo info;
			synchronized (mLock) {
				if (mDone || !mStarted) {
					return;
				}
				info = mInfo;
			}
			if (!markDone()) {
				return;
			}
			cleanup();
			final ResponseInfo finalInfo = info;
			postFinal(new Runnable() {
				@Override
				public void run() {
					mCallback.onCanceled(Request.this, finalInfo);
				}
			});
		}

		@Override
		public boolean isDone() {
			synchronized (mLock) {
				return mDone;
			}
		}

		/* CronetUrlRequest.getStatus(): INVALID once the request is not running. Status is what the worker last
		 * recorded; HttpURLConnection does not report finer steps (DNS, SSL handshake, ...) */
		@Override
		public void getStatus(final StatusListener listener) {
			final int status;
			synchronized (mLock) {
				status = mDone || !mStarted ? Status.INVALID : mStatus;
			}
			try {
				mExecutor.execute(new Runnable() {
					@Override
					public void run() {
						listener.onStatus(status);
					}
				});
			} catch (RejectedExecutionException e) {
			}
		}

		private void setStatus(int status) {
			synchronized (mLock) {
				mStatus = status;
			}
		}

		private boolean markDone() {
			synchronized (mLock) {
				if (mDone) {
					return false;
				}
				mDone = true;
				mStatus = Status.INVALID;
				mLock.notifyAll();
				return true;
			}
		}

		/* disconnects, closes the upload provider, lets the engine shut down; before the final callback, so that the
		 * callback may call shutdown() */
		private void cleanup() {
			HttpURLConnection c;
			synchronized (mLock) {
				c = mConnection;
				mConnection = null;
			}
			if (c != null) {
				c.disconnect();
			}
			if (mUploadDataProvider != null) {
				try {
					mUploadExecutor.execute(new Runnable() {
						@Override
						public void run() {
							try {
								mUploadDataProvider.close();
							} catch (IOException e) {
							}
						}
					});
				} catch (RejectedExecutionException e) {
				}
			}
			mEngine.requestFinished();
		}

		private void fail(final HttpException error) {
			ResponseInfo info;
			synchronized (mLock) {
				info = mInfo;
			}
			if (!markDone()) {
				return;
			}
			cleanup();
			final ResponseInfo finalInfo = info;
			postFinal(new Runnable() {
				@Override
				public void run() {
					mCallback.onFailed(Request.this, finalInfo, error);
				}
			});
		}

		/* CronetUrlRequest.postTaskToExecutor() and the callback wrappers: nothing runs after the end; inline
		 * execution on the worker thread is prohibited unless allowed (checkCallingThread()); a throwing
		 * callback fails the request */
		private void postNonFinal(final Action action) {
			try {
				mExecutor.execute(new Runnable() {
					@Override
					public void run() {
						if (!mAllowDirectExecutor && Thread.currentThread() == mWorker) {
							throw new InlineExecutionProhibitedException();
						}
						if (isDone()) {
							return;
						}
						try {
							action.run();
						} catch (Exception e) {
							fail(new CallbackExceptionImpl("Exception received from UrlRequest.Callback", e));
						}
					}
				});
			} catch (RejectedExecutionException e) {
				fail(new HttpException("Exception posting task to executor", e));
			}
		}

		/* an exception in a final callback is not reported anywhere (CronetUrlRequest.onFinalCallbackException logs) */
		private void postFinal(final Runnable callback) {
			try {
				mExecutor.execute(new Runnable() {
					@Override
					public void run() {
						try {
							callback.run();
						} catch (RuntimeException e) {
						}
					}
				});
			} catch (RejectedExecutionException e) {
			}
		}

		private ResponseInfo newInfo(int code, String text, HeaderBlock headers) {
			synchronized (mLock) {
				mInfo = new ResponseInfo(mUrlChain, code, text, headers, mReceived);
				return mInfo;
			}
		}

		/* AOSP maps Chromium's net error codes; HttpURLConnection only has exception types */
		private static NetworkException translate(IOException e) {
			String m = String.valueOf(e.getMessage());
			if (e instanceof UnknownHostException) {
				return new NetworkExceptionImpl(m, e, NetworkException.ERROR_HOSTNAME_NOT_RESOLVED);
			} else if (e instanceof SocketTimeoutException) {
				return new NetworkExceptionImpl(m, e, NetworkException.ERROR_TIMED_OUT);
			} else if (e instanceof NoRouteToHostException) {
				return new NetworkExceptionImpl(m, e, NetworkException.ERROR_ADDRESS_UNREACHABLE);
			} else if (e instanceof ConnectException) {
				return new NetworkExceptionImpl(m, e, NetworkException.ERROR_CONNECTION_REFUSED);
			} else if (e instanceof SocketException && m.toLowerCase().contains("reset")) {
				return new NetworkExceptionImpl(m, e, NetworkException.ERROR_CONNECTION_RESET);
			}
			return new NetworkExceptionImpl(m, e, NetworkException.ERROR_OTHER);
		}

		private static boolean isRedirect(int code) {
			return code == 301 || code == 302 || code == 303 || code == 307 || code == 308;
		}

		private HeaderBlock responseHeaders(HttpURLConnection c) {
			List<Map.Entry<String, String>> list = new ArrayList<Map.Entry<String, String>>();
			for (int i = 0;; i++) {
				String key = c.getHeaderFieldKey(i);
				String value = c.getHeaderField(i);
				if (key == null && value == null) {
					break;
				}
				if (key != null) {
					list.add(new AbstractMap.SimpleImmutableEntry<String, String>(key, value));
				}
			}
			return new HeaderBlockImpl(list);
		}

		private HttpURLConnection open(String url, String method, boolean upload) throws IOException {
			HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection(Proxy.NO_PROXY);
			c.setInstanceFollowRedirects(false);
			c.setUseCaches(mEngine.mUseCaches && !mCacheDisabled);
			c.setRequestMethod(method);
			boolean hasUserAgent = false;
			for (Map.Entry<String, String> header : mRequestHeaders) {
				if (header.getKey().equalsIgnoreCase("User-Agent")) {
					hasUserAgent = true;
				}
				c.addRequestProperty(header.getKey(), header.getValue());
			}
			if (!hasUserAgent) {
				c.setRequestProperty("User-Agent", mEngine.mUserAgent);
			}
			return c;
		}

		/* waits for the sink's answer; the Cronet equivalent is UploadDataStream */
		private boolean awaitSink() throws IOException, InterruptedException {
			synchronized (mLock) {
				while (!mSink.mResult && !mDone) {
					mLock.wait();
				}
				if (mDone) {
					throw new IOException("Request is done");
				}
				if (mSink.mError != null) {
					throw new CallbackExceptionImpl("Exception received from UploadDataProvider", mSink.mError);
				}
				return mSink.mFinal;
			}
		}

		private void sendBody(HttpURLConnection c) throws IOException, InterruptedException {
			long length;
			try {
				length = mUploadDataProvider.getLength();
			} catch (IOException e) {
				throw new CallbackExceptionImpl("Exception received from UploadDataProvider", e);
			}
			if (length >= 0) {
				c.setFixedLengthStreamingMode(length);
			} else {
				c.setChunkedStreamingMode(0);
			}
			c.setDoOutput(true);
			setStatus(Status.SENDING_REQUEST);
			OutputStream out = c.getOutputStream();
			final ByteBuffer buffer = ByteBuffer.allocateDirect(2 * BUFFER_SIZE);
			long sent = 0;
			boolean last = false;
			while (length >= 0 ? sent < length : !last) {
				buffer.clear();
				mSink.arm();
				mUploadExecutor.execute(new Runnable() {
					@Override
					public void run() {
						try {
							mUploadDataProvider.read(mSink, buffer);
						} catch (Exception e) {
							try {
								mSink.onReadError(e);
							} catch (IllegalStateException ignored) {
							}
						}
					}
				});
				last = awaitSink();
				buffer.flip();
				byte[] chunk = new byte[buffer.remaining()];
				buffer.get(chunk);
				out.write(chunk);
				sent += chunk.length;
			}
			out.close();
		}

		private void rewindBody() throws IOException, InterruptedException {
			mSink.arm();
			mUploadExecutor.execute(new Runnable() {
				@Override
				public void run() {
					try {
						mUploadDataProvider.rewind(mSink);
					} catch (Exception e) {
						try {
							mSink.onRewindError(e);
						} catch (IllegalStateException ignored) {
						}
					}
				}
			});
			awaitSink();
		}

		/* the worker thread: request, redirects, response, body */
		private void work() {
			synchronized (mLock) {
				mWorker = Thread.currentThread();
			}
			try {
				String url = mInitialUrl;
				String method = mMethod;
				boolean upload = mUploadDataProvider != null;
				mUrlChain.add(url);
				HttpURLConnection c;
				int code;
				for (int redirects = 0;; redirects++) {
					setStatus(Status.CONNECTING);
					c = open(url, method, upload);
					synchronized (mLock) {
						if (mDone) {
							c.disconnect();
							return;
						}
						mConnection = c;
					}
					if (upload) {
						sendBody(c);
					}
					setStatus(Status.WAITING_FOR_RESPONSE);
					code = c.getResponseCode();
					String location = c.getHeaderField("Location");
					if (!isRedirect(code) || location == null) {
						break;
					}
					if (redirects >= MAX_REDIRECTS) {
						fail(new NetworkExceptionImpl("net::ERR_TOO_MANY_REDIRECTS", null,
						                              NetworkException.ERROR_OTHER));
						return;
					}
					String next = new URL(new URL(url), location).toString();
					mUrlChain.add(next);
					final ResponseInfo redirectInfo =
					    newInfo(code, String.valueOf(c.getResponseMessage()), responseHeaders(c));
					final String nextUrl = next;
					synchronized (mLock) {
						mWaitingOnRedirect = true;
						mStatus = Status.WAITING_FOR_DELEGATE;
					}
					postNonFinal(new Action() {
						@Override
						public void run() throws Exception {
							mCallback.onRedirectReceived(Request.this, redirectInfo, nextUrl);
						}
					});
					synchronized (mLock) {
						while (mWaitingOnRedirect && !mDone) {
							mLock.wait();
						}
						if (mDone) {
							return;
						}
					}
					/* net::URLRequest::Redirect: 303, and 301 and 302 for POST, become GET without a body */
					if (code == 303 || ((code == 301 || code == 302) && "POST".equals(method))) {
						method = "GET";
						upload = false;
					} else if (upload) {
						rewindBody();
					}
					c.disconnect();
					url = next;
				}
				final ResponseInfo info = newInfo(code, String.valueOf(c.getResponseMessage()), responseHeaders(c));
				InputStream in = code >= 400 ? c.getErrorStream() : c.getInputStream();
				synchronized (mLock) {
					mWaitingOnRead = true;
					mStatus = Status.WAITING_FOR_DELEGATE;
				}
				postNonFinal(new Action() {
					@Override
					public void run() throws Exception {
						mCallback.onResponseStarted(Request.this, info);
					}
				});
				while (true) {
					ByteBuffer buffer;
					synchronized (mLock) {
						while (mReadBuffer == null && !mDone) {
							mLock.wait();
						}
						if (mDone) {
							return;
						}
						buffer = mReadBuffer;
						mReadBuffer = null;
						mStatus = Status.READING_RESPONSE;
					}
					int count = -1;
					byte[] chunk = null;
					if (in != null) {
						chunk = new byte[Math.min(buffer.remaining(), BUFFER_SIZE)];
						count = in.read(chunk);
					}
					if (count < 0) {
						if (in != null) {
							in.close();
						}
						if (markDone()) {
							cleanup();
							postFinal(new Runnable() {
								@Override
								public void run() {
									mCallback.onSucceeded(Request.this, newInfoSnapshot(info));
								}
							});
						}
						return;
					}
					buffer.put(chunk, 0, count);
					final ResponseInfo readInfo;
					final ByteBuffer readBuffer = buffer;
					synchronized (mLock) {
						mReceived += count;
						mWaitingOnRead = true;
						mStatus = Status.WAITING_FOR_DELEGATE;
						readInfo = mInfo = new ResponseInfo(mUrlChain, info.getHttpStatusCode(),
						                                    info.getHttpStatusText(), info.getHeaders(), mReceived);
					}
					postNonFinal(new Action() {
						@Override
						public void run() throws Exception {
							mCallback.onReadCompleted(Request.this, readInfo, readBuffer);
						}
					});
				}
			} catch (IOException e) {
				fail(e instanceof HttpException ? (HttpException) e : translate(e));
			} catch (InterruptedException e) {
				fail(new HttpException("Interrupted", e));
			} catch (RuntimeException e) {
				fail(new HttpException("Exception in request", e));
			}
		}

		private ResponseInfo newInfoSnapshot(ResponseInfo base) {
			synchronized (mLock) {
				return new ResponseInfo(mUrlChain, base.getHttpStatusCode(), base.getHttpStatusText(),
				                        base.getHeaders(), mReceived);
			}
		}
	}
}
