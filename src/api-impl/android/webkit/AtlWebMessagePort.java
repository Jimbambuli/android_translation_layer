package android.webkit;

import android.os.Handler;
import android.os.Looper;
import java.util.ArrayList;
import java.util.List;

/* One end of the pipe made by WebView.createWebMessageChannel(). Chromium's ports are mojo message pipes; here
 * the end that is handed to the page (WebView.postWebMessage(message, origin) with the port in message.getPorts())
 * becomes the other half of a MessageChannel created in the page by evaluateJavascript(), and the messages that
 * the page posts on it come back through the JavascriptInterface "__atlWebMessage" (see WebView). Approximation:
 * only text messages; a message posted to the page before the port is handed over is held back until then. */
final class AtlWebMessagePort extends WebMessagePort {
	private final WebView mView;
	final int mId;
	AtlWebMessagePort mPeer;
	boolean mTransferred;
	boolean mClosed;
	private boolean mStarted;
	private WebMessageCallback mCallback;
	private Handler mHandler;
	private final List<String> mOutgoing = new ArrayList<>(); // posted to the peer's page end while it is not handed over
	private final List<String> mIncoming = new ArrayList<>(); // from the page while no callback is set

	AtlWebMessagePort(WebView view, int id) {
		mView = view;
		mId = id;
	}

	@Override
	public synchronized void postMessage(WebMessage message) {
		if (mClosed || mTransferred)
			throw new IllegalStateException("Port is already closed or transferred");
		if (message.getPorts() != null && message.getPorts().length > 0)
			throw new IllegalStateException("Ports can only be transferred with WebView.postWebMessage()");
		mStarted = true;
		// the message goes to the end of the pipe the page holds, which is the peer's
		if (mPeer.mTransferred)
			mView.postToPagePort(mPeer.mId, message.getData());
		else
			mPeer.mOutgoing.add(message.getData());
	}

	@Override
	public synchronized void close() {
		if (mClosed)
			return;
		mClosed = true;
		mPeer.mView.closePagePort(mPeer.mId);
	}

	@Override
	public synchronized void setWebMessageCallback(WebMessageCallback callback) {
		setWebMessageCallback(callback, null);
	}

	@Override
	public synchronized void setWebMessageCallback(WebMessageCallback callback, Handler handler) {
		if (mClosed || mTransferred)
			throw new IllegalStateException("Port is already closed or transferred");
		mStarted = true;
		mCallback = callback;
		mHandler = handler;
		List<String> queued = new ArrayList<>(mIncoming);
		mIncoming.clear();
		for (String data : queued)
			deliver(data);
	}

	/* the messages held back until this end was handed to the page */
	synchronized void handedToPage() {
		mTransferred = true;
		List<String> queued = new ArrayList<>(mPeer.mOutgoing);
		mPeer.mOutgoing.clear();
		for (String data : queued)
			mView.postToPagePort(mId, data);
	}

	/* a message from the page for this (app side) end */
	synchronized void fromPage(String data) {
		if (mClosed)
			return;
		if (mCallback == null)
			mIncoming.add(data);
		else
			deliver(data);
	}

	private void deliver(final String data) {
		final WebMessageCallback callback = mCallback;
		Handler handler = mHandler != null ? mHandler : new Handler(Looper.getMainLooper());
		final WebMessagePort port = this;
		handler.post(new Runnable() {
			@Override
			public void run() {
				callback.onMessage(port, new WebMessage(data));
			}
		});
	}
}
