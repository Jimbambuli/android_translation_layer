package android.webkit;

import android.atl.annotation.Export;
import android.content.Context;
import android.content.res.AssetManager;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Base64;
import android.view.MotionEvent;
import android.util.Log;
import android.view.ViewGroup;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/* AOSP frameworks/base core/java/android/webkit/WebView.java: WebView extends AbsoluteLayout (a ViewGroup); the
 * WebKitGTK widget still comes from WebView.native_constructor(), AbsoluteLayout has no native part */
public class WebView extends android.widget.AbsoluteLayout implements ViewGroup.OnHierarchyChangeListener, android.view.ViewTreeObserver.OnGlobalFocusChangeListener {
	/* AOSP WebView.onChildViewAdded()/onChildViewRemoved()/onGlobalFocusChanged(): deprecated, empty */
	@Deprecated
	public void onChildViewAdded(android.view.View parent, android.view.View child) {}

	@Deprecated
	public void onChildViewRemoved(android.view.View p, android.view.View child) {}

	@Deprecated
	public void onGlobalFocusChanged(android.view.View oldFocus, android.view.View newFocus) {}

	private static final String TAG = "WebView";
	// AOSP WebView: URI scheme for telephone number, email address and map address
	public static final String SCHEME_TEL = "tel:";
	public static final String SCHEME_MAILTO = "mailto:";
	public static final String SCHEME_GEO = "geo:0,0?q=";

	private WebViewClient webViewClient;
	private String url;
	private WebChromeClient webChromeClient;
	private boolean destroyed;
	// the page's scroll offset in View pixels as last reported or set, AwScrollOffsetManager's mNativeScroll*
	private int nativeScrollX;
	private int nativeScrollY;
	// the page's size in CSS pixels, as in AwContents
	private int contentWidthCss;
	private int contentHeightCss;
	// AwLayoutSizer: whether the last measure took the size from the parent rather than from the page
	private boolean widthMeasurementIsFixed;
	private boolean heightMeasurementIsFixed;
	private boolean verticalScrollBarEnabled = true;
	// AOSP WebView.mWebViewThread: the thread the WebView was made on
	private final Looper webViewThread = Looper.myLooper();
	// the progress last reported to WebChromeClient.onProgressChanged(), AwContents.getMostRecentProgress()
	private int progress = 100;
	// whether a WebView was made in this process (WebViewFactory.sProviderInstance != null)
	private static boolean providerInitialized;
	private boolean horizontalScrollBarEnabled = true;
	// AwContents.mPossiblyStaleHitTestData: what was under the pointer last, see internalHitTest()
	private int hitTestType = HitTestResult.UNKNOWN_TYPE;
	private String hitTestExtra;
	private String hitTestHref;
	private String hitTestAnchorText;
	private String hitTestImgSrc;
	private FindListener findListener;
	// the zero-based ordinal of the selected match of the last findAllAsync(), moved by findNext()
	private int findActiveMatch;
	private int findMatchCount;

	public WebView(Context context) {
		this(context, null);
	}

	public WebView(Context context, AttributeSet attrs) {
		this(context, attrs, com.android.internal.R.attr.webViewStyle);
	}

	/* AOSP WebView(Context, AttributeSet, int, boolean) (deprecated): the provider's init(privateBrowsing);
	 * Chromium's WebViewChromium.init() (not in the local sources) rejects private browsing for apps targeting
	 * KitKat or later and only logs it for older ones */
	public WebView(Context context, AttributeSet attrs, int defStyleAttr, boolean privateBrowsing) {
		this(context, attrs, defStyleAttr);
		if (privateBrowsing) {
			final String msg = "Private browsing is not supported in WebView.";
			if (context.getApplicationInfo().targetSdkVersion >= 19)
				throw new IllegalArgumentException(msg);
			android.util.Log.w("WebView", msg);
		}
	}

	public WebView(Context context, AttributeSet attrs, int defStyleAttr) {
		this(context, attrs, defStyleAttr, 0);
	}

	public WebView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		providerInitialized = true;
		// AOSP WebView constructor: a WebView allows CookieSyncManager.getInstance()
		CookieSyncManager.setGetInstanceIsAllowed();
		// like Chromium's AwContents: the page shows the focus itself (focus rings, the caret)
		setDefaultFocusHighlightEnabled(false);
		// the pages load with the user agent that WebSettings reports, not WebKitGTK's own
		setWebKitSetting("user-agent", WebSettings.defaultUserAgent());
		/* navigator.language and Accept-Language follow the app's locale, as on Android; WebKit would take the
		 * C locale that ATL sets for the process (LC_ALL=C.UTF-8, navigator.language "c") */
		String language = java.util.Locale.getDefault().toLanguageTag();
		if (!"und".equals(language))
			native_setPreferredLanguage(language);
	}

	private static native void native_setPreferredLanguage(String language);

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		/* an ancestor took over the touch sequence: the page gets pointercancel, like Chromium's
		 * WebView (AwContents.onTouchEvent() forwards ACTION_CANCEL to the renderer) */
		if (event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
			native_firePointerCancel(widget);
			return true;
		}
		return native_gtkEventPropagation();
	}

	/* like Chromium's WebView (AwContents.onGenericMotionEvent()), the page consumes ACTION_SCROLL;
	 * WebKitGTK scrolls it with the GdkEvent */
	@Override
	public boolean onGenericMotionEvent(MotionEvent event) {
		if (event.getActionMasked() == MotionEvent.ACTION_SCROLL)
			return native_gtkScrollTarget(widget);
		return super.onGenericMotionEvent(event);
	}

	/* AOSP WebView.getSettings(): one WebSettings per WebView, whose setters reach WebKit's settings */
	private WebSettings settings;

	public WebSettings getSettings() {
		if (settings == null)
			settings = new WebSettings(this);
		return settings;
	}

	void setWebKitSetting(String name, boolean value) {
		if (!isDestroyed())
			native_setBooleanSetting(widget, name, value);
	}

	void setWebKitSetting(String name, String value) {
		if (!isDestroyed())
			native_setStringSetting(widget, name, value);
	}

	private native void native_setBooleanSetting(long widget, String name, boolean value);
	private native void native_setStringSetting(long widget, String name, String value);

	public void setDownloadListener(DownloadListener downloadListener) {}

	public void setScrollBarStyle(int scrollBarStyle) {}

	public void setWebViewClient(WebViewClient webViewClient) {
		this.webViewClient = webViewClient;
	}

	public WebViewClient getWebViewClient() {
		return webViewClient;
	}

	public WebChromeClient getWebChromeClient() {
		return webChromeClient;
	}

	public Looper getWebViewLooper() {
		return webViewThread;
	}

	public int getProgress() {
		return progress;
	}

	/* AOSP WebView.set/getWebViewRenderProcessClient(): the client is kept per WebView. ATL's WebKit raises no
	 * unresponsive/terminated events for it (nothing calls it), as for a renderer that never hangs */
	private WebViewRenderProcessClient webViewRenderProcessClient;

	public void setWebViewRenderProcessClient(java.util.concurrent.Executor executor, WebViewRenderProcessClient client) {
		webViewRenderProcessClient = client;
	}

	public void setWebViewRenderProcessClient(WebViewRenderProcessClient client) {
		webViewRenderProcessClient = client;
	}

	public WebViewRenderProcessClient getWebViewRenderProcessClient() {
		return webViewRenderProcessClient;
	}

	/* AOSP WebView.saveState(): null when the state cannot be saved, restoreState(): null when nothing is
	 * restored. ATL's WebKit session state is not reachable from Java, so both report "failed" and leave the
	 * Bundle untouched (as for a WebView without history). */
	public WebBackForwardList saveState(android.os.Bundle outState) {
		return null;
	}

	public WebBackForwardList restoreState(android.os.Bundle inState) {
		return null;
	}

	/* AOSP WebView.copyBackForwardList(): never null; an empty list (size 0, index -1, no items) as for a
	 * WebView without history. The WebKit history is not read here: approximation. */
	public WebBackForwardList copyBackForwardList() {
		return new WebBackForwardList() {
			@Override
			public WebHistoryItem getCurrentItem() {
				return null;
			}

			@Override
			public int getCurrentIndex() {
				return -1;
			}

			@Override
			public WebHistoryItem getItemAtIndex(int index) {
				return null;
			}

			@Override
			public int getSize() {
				return 0;
			}

			@Override
			protected WebBackForwardList clone() {
				return this;
			}
		};
	}

	/* AOSP WebView.getWebViewRenderProcess(): null when the WebView does not run its renderer in a separate
	 * process; ATL's WebKit exposes no render process handle. */
	public WebViewRenderProcess getWebViewRenderProcess() {
		return null;
	}

	/* AOSP WebView.zoomBy(): range check, then the provider zooms the page. ATL's WebKit page zoom is not
	 * driven from here: checked, not applied. */
	public void zoomBy(float zoomFactor) {
		if (zoomFactor < 0.01)
			throw new IllegalArgumentException("zoomFactor must be greater than 0.01.");
		if (zoomFactor > 100.0)
			throw new IllegalArgumentException("zoomFactor must be less than 100.");
	}

	/* AOSP: sets the page's online state (navigator.onLine); WebKit follows the host's network state, so nothing
	 * to set. enableSlowWholeDocumentDraw(): Chromium-only drawing mode, WebKit always paints the whole page. */
	public void setNetworkAvailable(boolean networkUp) {}

	public static void enableSlowWholeDocumentDraw() {}

	/* AOSP WebView.postVisualStateCallback(): onComplete(requestId) once the content as of this call is drawn.
	 * Approximated by the next main-looper turn: WebKit gives no draw-completion event to wait for */
	public void postVisualStateCallback(long requestId, VisualStateCallback callback) {
		new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> callback.onComplete(requestId));
	}

	/* AOSP WebView.zoomIn()/zoomOut(): the provider returns whether the page could be zoomed (AwContents.canZoomIn/
	 * canZoomOut). ATL's WebKit page zoom is not driven from here (see zoomBy()), so no zoom is possible. */
	public boolean zoomIn() {
		return false;
	}

	public boolean zoomOut() {
		return false;
	}

	/* AOSP WebView.getFavicon() is the provider's page icon, null until one was received; ATL does not deliver
	 * page icons. */
	public android.graphics.Bitmap getFavicon() {
		return null;
	}

	/* AOSP WebView.setRendererPriorityPolicy(): only stored by the provider; ATL has no renderer process to
	 * rank, getRendererRequestedPriority()/getRendererPriorityWaivedWhenNotVisible() are not implemented. */
	public void setRendererPriorityPolicy(int rendererRequestedPriority, boolean waivedWhenNotVisible) {}

	/* AOSP WebView.clearClientCertPreferences(): clears the remembered client certificate choices, then runs the
	 * callback on the UI thread. ATL keeps none (no ClientCertRequest persistence), so only the callback. */
	public static void clearClientCertPreferences(Runnable onCleared) {
		if (onCleared != null)
			new android.os.Handler(Looper.getMainLooper()).post(onCleared);
	}

	/* AOSP WebView.startSafeBrowsing()/setSafeBrowsingWhitelist() hand over to the provider's statics. WebKit has
	 * no Safe Browsing: a provider without it reports false (not enabled / list not applied). */
	public static void startSafeBrowsing(Context context, ValueCallback<Boolean> callback) {
		if (callback != null)
			new android.os.Handler(Looper.getMainLooper()).post(() -> callback.onReceiveValue(false));
	}

	public static void setSafeBrowsingWhitelist(List<String> hosts, ValueCallback<Boolean> callback) {
		if (callback != null)
			new android.os.Handler(Looper.getMainLooper()).post(() -> callback.onReceiveValue(false));
	}

	/* AOSP WebView.createPrintDocumentAdapter(): always an adapter (the provider's AwPrintDocumentAdapter). WebKitGTK prints only
	 * through GtkPrintOperation, which cannot produce the PDF stream onWrite() has to write: layout and write report a failure,
	 * as the provider's adapter does when it cannot print. (PrintManager.print() has no print spooler here anyway.) */
	@Deprecated
	public android.print.PrintDocumentAdapter createPrintDocumentAdapter() {
		return createPrintDocumentAdapter("default");
	}

	public android.print.PrintDocumentAdapter createPrintDocumentAdapter(String documentName) {
		return new android.print.PrintDocumentAdapter() {
			@Override
			public void onLayout(android.print.PrintAttributes oldAttributes, android.print.PrintAttributes newAttributes,
			                     android.os.CancellationSignal cancellationSignal, LayoutResultCallback callback,
			                     android.os.Bundle extras) {
				callback.onLayoutFailed(null);
			}

			@Override
			public void onWrite(android.print.PageRange[] pages, android.os.ParcelFileDescriptor destination,
			                    android.os.CancellationSignal cancellationSignal, WriteResultCallback callback) {
				callback.onWriteFailed(null);
			}
		};
	}

	/* ---- WebMessage channels (see AtlWebMessagePort): text messages through evaluateJavascript() and a JavascriptInterface ---- */
	private final Map<Integer, AtlWebMessagePort> messagePorts = new HashMap<>();
	private int nextMessagePortId = 1;
	private boolean messageBridgeAdded;

	public class AtlWebMessageBridge {
		@JavascriptInterface
		public void onMessage(int pageEndId, String data) {
			AtlWebMessagePort pageEnd;
			synchronized (messagePorts) {
				pageEnd = messagePorts.get(pageEndId);
			}
			if (pageEnd != null && pageEnd.mPeer != null)
				pageEnd.mPeer.fromPage(data);
		}
	}

	private static String jsString(String s) {
		StringBuilder sb = new StringBuilder("\"");
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			switch (c) {
			case '\\': sb.append("\\\\"); break;
			case '"': sb.append("\\\""); break;
			case '\n': sb.append("\\n"); break;
			case '\r': sb.append("\\r"); break;
			case '\u2028': sb.append("\\u2028"); break;
			case '\u2029': sb.append("\\u2029"); break;
			default:
				if (c < 0x20)
					sb.append(String.format("\\u%04x", (int) c));
				else
					sb.append(c);
			}
		}
		return sb.append('"').toString();
	}

	/* AOSP WebView.createWebMessageChannel(): the two ends of a new channel */
	public WebMessagePort[] createWebMessageChannel() {
		if (!messageBridgeAdded) {
			messageBridgeAdded = true;
			addJavascriptInterface(new AtlWebMessageBridge(), "__atlWebMessage");
		}
		AtlWebMessagePort a, b;
		synchronized (messagePorts) {
			a = new AtlWebMessagePort(this, nextMessagePortId++);
			b = new AtlWebMessagePort(this, nextMessagePortId++);
			a.mPeer = b;
			b.mPeer = a;
			messagePorts.put(a.mId, a);
			messagePorts.put(b.mId, b);
		}
		return new WebMessagePort[] {a, b};
	}

	/* AOSP WebView.postWebMessage(): a MessageEvent in the main frame, delivered if targetOrigin is "*" or the page's origin */
	public void postWebMessage(WebMessage message, android.net.Uri targetOrigin) {
		if (isDestroyed())
			return;
		WebMessagePort[] ports = message.getPorts();
		List<AtlWebMessagePort> handed = new ArrayList<>();
		if (ports != null) {
			for (WebMessagePort p : ports) {
				if (!(p instanceof AtlWebMessagePort) || ((AtlWebMessagePort) p).mClosed || ((AtlWebMessagePort) p).mTransferred
				    || ((AtlWebMessagePort) p).mPeer == null)
					throw new IllegalStateException("Port is already started, closed or transferred");
				handed.add((AtlWebMessagePort) p);
			}
		}
		StringBuilder js = new StringBuilder();
		js.append("(function(){var t=").append(jsString(targetOrigin.toString())).append(";");
		js.append("if(t!=='*'&&t!==window.location.origin)return;");
		js.append("window.__atlPorts=window.__atlPorts||{};var ports=[];");
		for (AtlWebMessagePort p : handed) {
			js.append("(function(id){var mc=new MessageChannel();window.__atlPorts[id]=mc.port2;")
			    .append("mc.port2.onmessage=function(e){__atlWebMessage.onMessage(id,String(e.data));};ports.push(mc.port1);})(")
			    .append(p.mId).append(");");
		}
		js.append("window.dispatchEvent(new MessageEvent('message',{data:").append(jsString(message.getData()))
		    .append(",origin:'',ports:ports}));})();");
		evaluateJavascript(js.toString(), null);
		for (AtlWebMessagePort p : handed)
			p.handedToPage();
	}

	void postToPagePort(int pageEndId, String data) {
		evaluateJavascript("(function(){var p=window.__atlPorts&&window.__atlPorts[" + pageEndId + "];if(p)p.postMessage("
		                       + jsString(data) + ");})();",
		                   null);
	}

	void closePagePort(int pageEndId) {
		evaluateJavascript("(function(){var p=window.__atlPorts&&window.__atlPorts[" + pageEndId
		                       + "];if(p){p.close();delete window.__atlPorts[" + pageEndId + "];}})();",
		                   null);
	}

	/* ViewGroup members (deprecated drawing caches), not present on ATL's ViewGroup: no effect, as on Android 16 */
	public void setAlwaysDrawnWithCacheEnabled(boolean enabled) {}

	public void setAnimationCacheEnabled(boolean enabled) {}

	/* AOSP WebViewFactory.setDataDirectorySuffix() */
	public static void setDataDirectorySuffix(String suffix) {
		if (providerInitialized)
			throw new IllegalStateException("Can't set data directory suffix: WebView already initialized");
		if (suffix.indexOf(java.io.File.separatorChar) >= 0)
			throw new IllegalArgumentException("Suffix " + suffix + " contains a path separator");
	}

	/* AwContents.getLastHitTestResult(): the data of the last hit test, the element under the pointer */
	public HitTestResult getHitTestResult() {
		HitTestResult result = new HitTestResult();
		result.setType(hitTestType);
		result.setExtra(hitTestExtra);
		return result;
	}

	/* AOSP WebView.requestFocusNodeHref(): the data of the last hit test go to the message as "url" (the
	 * anchor's href), "title" (its text) and "src" (the image's src) */
	public void requestFocusNodeHref(android.os.Message hrefMsg) {
		if (hrefMsg == null || isDestroyed())
			return;
		android.os.Bundle data = hrefMsg.getData();
		data.putString("url", hitTestHref);
		data.putString("title", hitTestAnchorText);
		data.putString("src", hitTestImgSrc);
		hrefMsg.setData(data);
		hrefMsg.sendToTarget();
	}

	/* AOSP WebView.requestImageRef(): "url" is the image's src */
	public void requestImageRef(android.os.Message msg) {
		if (msg == null || isDestroyed())
			return;
		android.os.Bundle data = msg.getData();
		data.putString("url", hitTestImgSrc);
		msg.setData(data);
		msg.sendToTarget();
	}

	/* to be used by native code, for each new element under the pointer. The types as documented for
	 * AOSP WebView.getHitTestResult(): an anchor with a non-JavaScript URL is SRC_ANCHOR_TYPE (PHONE_TYPE,
	 * EMAIL_TYPE, GEO_TYPE for tel:, mailto: and SCHEME_GEO URLs), with an image as child SRC_IMAGE_ANCHOR_TYPE,
	 * an image IMAGE_TYPE, a text field EDIT_TEXT_TYPE */
	@Export
	void internalHitTest(String link, String linkLabel, String image, boolean editable) {
		if (link != null && link.startsWith("javascript:"))
			link = null;
		link = link != null ? internalToAppUrl(link) : null;
		image = image != null ? internalToAppUrl(image) : null;
		hitTestHref = link;
		hitTestAnchorText = linkLabel;
		hitTestImgSrc = image;
		int type = HitTestResult.UNKNOWN_TYPE;
		String extra = null;
		if (editable) {
			type = HitTestResult.EDIT_TEXT_TYPE;
		} else if (link != null && image != null) {
			type = HitTestResult.SRC_IMAGE_ANCHOR_TYPE;
			extra = image;
		} else if (link != null && link.startsWith(SCHEME_TEL)) {
			type = HitTestResult.PHONE_TYPE;
			extra = android.net.Uri.decode(link.substring(SCHEME_TEL.length()));
		} else if (link != null && link.startsWith(SCHEME_MAILTO)) {
			type = HitTestResult.EMAIL_TYPE;
			extra = android.net.Uri.decode(link.substring(SCHEME_MAILTO.length()));
		} else if (link != null && link.startsWith(SCHEME_GEO)) {
			type = HitTestResult.GEO_TYPE;
			extra = android.net.Uri.decode(link.substring(SCHEME_GEO.length()));
		} else if (link != null) {
			type = HitTestResult.SRC_ANCHOR_TYPE;
			extra = link;
		} else if (image != null) {
			type = HitTestResult.IMAGE_TYPE;
			extra = image;
		}
		hitTestType = type;
		hitTestExtra = extra;
	}

	/* AOSP WebView.FindListener */
	public interface FindListener {
		public void onFindResultReceived(int activeMatchOrdinal, int numberOfMatches, boolean isDoneCounting);
	}

	public void setFindListener(FindListener listener) {
		findListener = listener;
	}

	/* AOSP WebView.findAllAsync(): finds and highlights all matches, the FindListener hears of the result */
	public void findAllAsync(String find) {
		if (isDestroyed())
			return;
		boolean empty = find.isEmpty();
		findActiveMatch = 0;
		findMatchCount = 0;
		native_findAll(widget, find);
		if (empty && findListener != null)
			findListener.onFindResultReceived(0, 0, true);
	}

	/* AOSP WebView.findNext(): selects the next match, wrapping around; nothing before findAllAsync() */
	public void findNext(boolean forward) {
		if (isDestroyed() || findMatchCount == 0)
			return;
		findActiveMatch = (findActiveMatch + (forward ? 1 : findMatchCount - 1)) % findMatchCount;
		native_findNext(widget, forward);
	}

	public void clearMatches() {
		if (isDestroyed())
			return;
		findMatchCount = 0;
		native_clearMatches(widget);
	}

	// to be used by native code: the number of matches of the last search
	@Export
	void internalFindResult(int matchCount) {
		findMatchCount = matchCount;
		if (findActiveMatch >= matchCount)
			findActiveMatch = 0;
		if (findListener != null)
			findListener.onFindResultReceived(findActiveMatch, matchCount, true);
	}

	public class WebViewTransport {
		private WebView mWebview;

		public synchronized void setWebView(WebView webview) {
			mWebview = webview;
		}

		public synchronized WebView getWebView() {
			return mWebview;
		}
	}

	public static abstract class VisualStateCallback {
		public abstract void onComplete(long requestId);
	}

	public static class HitTestResult {
		public static final int UNKNOWN_TYPE = 0;
		@Deprecated
		public static final int ANCHOR_TYPE = 1;
		public static final int PHONE_TYPE = 2;
		public static final int GEO_TYPE = 3;
		public static final int EMAIL_TYPE = 4;
		public static final int IMAGE_TYPE = 5;
		@Deprecated
		public static final int IMAGE_ANCHOR_TYPE = 6;
		public static final int SRC_ANCHOR_TYPE = 7;
		public static final int SRC_IMAGE_ANCHOR_TYPE = 8;
		public static final int EDIT_TEXT_TYPE = 9;

		private int mType;
		private String mExtra;

		public HitTestResult() {
			mType = UNKNOWN_TYPE;
		}

		public void setType(int type) {
			mType = type;
		}

		public void setExtra(String extra) {
			mExtra = extra;
		}

		public int getType() {
			return mType;
		}

		public String getExtra() {
			return mExtra;
		}
	}

	// undo the android-asset:// rewrite (see loadUrl()), apps expect the URL they loaded
	private static String internalToAppUrl(String url) {
		return url != null ? url.replace("android-asset:///assets/", "file:///android_asset/") : null;
	}

	/* to be used by native code, on a thread of its own (as on Android): asked by the web process before each
	 * subresource request, see intercept_connection_cb(). Returns the app's response for the native side (status
	 * code, reason phrase, content type and header lines, an empty line, the data), or null to load from the network */
	@Export
	byte[] internalShouldInterceptRequest(String url, String method, String headers) throws java.io.IOException {
		WebViewClient client = webViewClient;
		if (client == null)
			return null;
		final Map<String, String> requestHeaders = new HashMap<>();
		for (String header : headers.split("\u001f")) {
			int colon = header.indexOf(": ");
			if (colon > 0)
				requestHeaders.put(header.substring(0, colon), header.substring(colon + 2));
		}
		final android.net.Uri uri = android.net.Uri.parse(url);
		WebResourceResponse response = client.shouldInterceptRequest(this, new WebResourceRequest() {
			public android.net.Uri getUrl() { return uri; }
			public boolean isForMainFrame() { return false; } /* the main frame's document is not asked */
			public boolean isRedirect() { return false; }
			public boolean hasGesture() { return false; }
			public String getMethod() { return method; }
			public Map<String, String> getRequestHeaders() { return requestHeaders; }
		});
		if (response == null)
			return null;
		StringBuilder head = new StringBuilder();
		String type = response.getMimeType() == null ? "" : response.getMimeType();
		if (response.getMimeType() != null && response.getEncoding() != null)
			type += "; charset=" + response.getEncoding();
		head.append(response.getStatusCode()).append('\n').append(response.getReasonPhrase()).append('\n').append(type).append('\n');
		if (response.getResponseHeaders() != null)
			for (Map.Entry<String, String> header : response.getResponseHeaders().entrySet())
				head.append(header.getKey()).append(": ").append(header.getValue()).append('\n');
		head.append('\n');
		java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
		out.write(head.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
		InputStream data = response.getData();
		if (data != null) {
			byte[] buffer = new byte[16384];
			for (int n; (n = data.read(buffer)) > 0;)
				out.write(buffer, 0, n);
			data.close();
		}
		return out.toByteArray();
	}

	// to be used by native code
	@Export
	void internalLoadChanged(int loadState, String url) {
		url = internalToAppUrl(url);
		this.url = url;
		if (loadState == /*WEBKIT_LOAD_STARTED*/ 0) {
			if (webViewClient != null)
				webViewClient.onPageStarted(this, url, null);
			progress = 10;
			if (webChromeClient != null)
				webChromeClient.onProgressChanged(this, 10);
		} else if (loadState == /*WEBKIT_LOAD_FINISHED*/ 3) {
			progress = 100;
			if (webChromeClient != null)
				webChromeClient.onProgressChanged(this, 100);
			if (webViewClient != null)
				webViewClient.onPageFinished(this, url);
		}
	}

	@Export
	boolean internalShouldOverrideUrlLoading(String url) {
		url = internalToAppUrl(url);
		if (webViewClient == null)
			return false;
		/* as WebViewContentsClientAdapter.shouldOverrideUrlLoading(): the client gets the WebResourceRequest
		 * form, whose default calls the String form; asked for navigations of the main frame only */
		final android.net.Uri uri = android.net.Uri.parse(url);
		return webViewClient.shouldOverrideUrlLoading(this, new WebResourceRequest() {
			public android.net.Uri getUrl() { return uri; }
			public boolean isForMainFrame() { return true; }
			public boolean isRedirect() { return false; }
			public boolean hasGesture() { return false; }
			public String getMethod() { return "GET"; }
			public Map<String, String> getRequestHeaders() { return new HashMap<>(); }
		});
	}

	/* Android's WebView draws no vertical scrollbar when the app turns it off; WebKit draws its own, which
	 * native_setVerticalScrollBarEnabled() hides */
	@Override
	public void setVerticalScrollBarEnabled(boolean enabled) {
		if (verticalScrollBarEnabled == enabled || isDestroyed())
			return;
		verticalScrollBarEnabled = enabled;
		native_setVerticalScrollBarEnabled(widget, enabled);
	}

	// WebKit can only hide both scrollbars of a page together, so this one stays
	@Override
	public void setHorizontalScrollBarEnabled(boolean enabled) {
		horizontalScrollBarEnabled = enabled;
	}

	public boolean isVerticalScrollBarEnabled() {
		return verticalScrollBarEnabled;
	}

	public boolean isHorizontalScrollBarEnabled() {
		return horizontalScrollBarEnabled;
	}

	/* The page scrolled (message "\u0001scroll\u0001<x>\u0001<y>\u0001<width>\u0001<height>" in CSS pixels):
	 * like Chromium's AwScrollOffsetManager.scrollContainerViewTo(), the View's scroll offset follows the
	 * page, so getScrollY() and onScrollChanged() tell the app where the page is. */
	private void internalScrollChanged(String[] parts) {
		float scale = getScale();
		int oldWidthCss = contentWidthCss;
		int oldHeightCss = contentHeightCss;
		contentWidthCss = (int)Math.ceil(Double.parseDouble(parts[4]));
		contentHeightCss = (int)Math.ceil(Double.parseDouble(parts[5]));
		nativeScrollX = Math.round(Float.parseFloat(parts[2]) * scale);
		nativeScrollY = Math.round(Float.parseFloat(parts[3]) * scale);
		scrollTo(nativeScrollX, nativeScrollY);
		// AwLayoutSizer.onContentSizeChanged(): a size that follows the page is measured again
		if ((contentHeightCss != oldHeightCss && !heightMeasurementIsFixed) || (contentWidthCss != oldWidthCss && !widthMeasurementIsFixed))
			requestLayout();
	}

	/* AwLayoutSizer.onMeasure(): where the parent leaves the size open, the WebView takes the page's size,
	 * as a wrap_content WebView in a scrolling list (UNSPECIFIED height) or one with an AT_MOST limit */
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		int heightMode = MeasureSpec.getMode(heightMeasureSpec);
		int widthSize = MeasureSpec.getSize(widthMeasureSpec);
		int heightSize = MeasureSpec.getSize(heightMeasureSpec);
		int contentWidthPix = (int)(contentWidthCss * getScale());
		int contentHeightPix = (int)(contentHeightCss * getScale());
		int measuredWidth = contentWidthPix;
		int measuredHeight = contentHeightPix;
		widthMeasurementIsFixed = widthMode != MeasureSpec.UNSPECIFIED;
		heightMeasurementIsFixed = heightMode == MeasureSpec.EXACTLY;
		if (heightMeasurementIsFixed || (heightMode == MeasureSpec.AT_MOST && contentHeightPix > heightSize))
			measuredHeight = heightSize;
		if (widthMeasurementIsFixed)
			measuredWidth = widthSize;
		if (measuredHeight < contentHeightPix)
			measuredHeight |= MEASURED_STATE_TOO_SMALL;
		if (measuredWidth < contentWidthPix)
			measuredWidth |= MEASURED_STATE_TOO_SMALL;
		setMeasuredDimension(measuredWidth, measuredHeight);
	}

	/* like WebViewChromium.onScrollChanged() -> AwScrollOffsetManager.onContainerViewScrollChanged(): an
	 * app's scrollTo()/scrollBy() scrolls the page */
	@Override
	protected void onScrollChanged(int l, int t, int oldl, int oldt) {
		super.onScrollChanged(l, t, oldl, oldt);
		// the page's own offset, from internalScrollChanged()
		if (destroyed || (l == nativeScrollX && t == nativeScrollY))
			return;
		// AwScrollOffsetManager.scrollNativeTo()
		int x = Math.max(0, Math.min(l, computeHorizontalScrollRange() - getWidth()));
		int y = Math.max(0, Math.min(t, computeVerticalScrollRange() - getHeight()));
		if (x == nativeScrollX && y == nativeScrollY)
			return;
		nativeScrollX = x;
		nativeScrollY = y;
		native_scrollTo(widget, x / getScale(), y / getScale());
	}

	// AwScrollOffsetManager: the View's size plus the maximum scroll offset
	@Override
	protected int computeHorizontalScrollRange() {
		return Math.max(getWidth(), (int)Math.ceil(contentWidthCss * getScale()));
	}

	@Override
	protected int computeVerticalScrollRange() {
		return Math.max(getHeight(), (int)Math.ceil(contentHeightCss * getScale()));
	}
	public void setVerticalScrollbarOverlay(boolean overlay) {}
	public void setInitialScale(int scaleInPercent) {}

	private final Map<String, Object> javascriptInterfaces = new HashMap<>();
	// like on Android, interface methods are called on a background thread
	private static ExecutorService javaBridgeExecutor;

	/* The object becomes available to JavaScript as window.<name> on the next page load. Its methods return
	 * their values to the page, which waits for them, as in Chromium. */
	public void addJavascriptInterface(Object object, String name) {
		if (isDestroyed())
			return;
		javascriptInterfaces.put(name, object);
		Set<String> methodNames = new LinkedHashSet<>();
		for (Method method : getJavascriptInterfaceMethods(object))
			methodNames.add(method.getName());
		native_addJavascriptInterface(widget, name, String.join(",", methodNames));
	}

	private List<Method> getJavascriptInterfaceMethods(Object object) {
		// since API level 17, only methods annotated with @JavascriptInterface are exposed
		boolean annotationRequired = getContext().getApplicationInfo().targetSdkVersion >= 17;
		List<Method> methods = new ArrayList<>();
		for (Method method : object.getClass().getMethods()) {
			if (method.getDeclaringClass() == Object.class)
				continue;
			if (annotationRequired && !method.isAnnotationPresent(JavascriptInterface.class))
				continue;
			methods.add(method);
		}
		return methods;
	}

	// to be used by native code; message is "\u0001console\u0001<level>\u0001<message>"
	@Export
	void internalJsMessage(String message) {
		final String[] parts = message.split("\u0001", -1);
		if (parts.length >= 2 && parts[0].isEmpty() && parts[1].equals("console"))
			internalConsoleMessage(parts.length > 2 ? parts[2] : "log", parts.length > 3 ? parts[3] : "");
		else if (parts.length == 6 && parts[0].isEmpty() && parts[1].equals("scroll"))
			internalScrollChanged(parts);
	}

	/* A call of an interface method from the page: the page waits for the answer (a synchronous
	 * XMLHttpRequest, see native_addJavascriptInterface()), the method runs on a background thread as in
	 * Chromium's Java bridge, and native_finishJavaBridgeCall() hands the answer to the page.
	 * message is "<interface>\u0001<method>\u0001<type>:<value>..." */
	@Export
	void internalJavaBridgeCall(final long request, String message) {
		final String[] parts = message.split("\u0001", -1);
		if (javaBridgeExecutor == null)
			javaBridgeExecutor = Executors.newSingleThreadExecutor();
		javaBridgeExecutor.execute(new Runnable() {
			@Override
			public void run() {
				native_finishJavaBridgeCall(request, invokeJavascriptInterface(parts));
			}
		});
	}

	/* GinJavaMethodInvocationHelper::Invoke(): the first method with this name and number of parameters,
	 * the arguments coerced to the parameter types, the result (or the error) as the answer for the
	 * page: "u" undefined, "n" null, "b<true|false>", "d<number>", "s<string>" or "e<error message>" */
	private String invokeJavascriptInterface(String[] parts) {
		Object object = javascriptInterfaces.get(parts[0]);
		if (object == null)
			return "eJava object is gone";
		for (Method method : getJavascriptInterfaceMethods(object)) {
			Class<?>[] types = method.getParameterTypes();
			if (!method.getName().equals(parts[1]) || types.length != parts.length - 2)
				continue;
			Object[] args = new Object[types.length];
			for (int i = 0; i < types.length; i++)
				args[i] = coerceJavascriptArgument(parts[i + 2], types[i]);
			try {
				return javascriptResult(method.getReturnType(), method.invoke(object, args));
			} catch (ReflectiveOperationException e) {
				e.printStackTrace();
				return "eJava exception was raised during method invocation";
			}
		}
		return "eMethod not found";
	}

	private static String javascriptResult(Class<?> type, Object result) {
		if (type == void.class || type.isArray())
			return "u";
		if (type == boolean.class)
			return "b" + result;
		if (type == char.class)
			return "d" + (int)(Character)result;
		if (type == long.class)
			return "d" + (double)(Long)result;
		if (type.isPrimitive())
			return "d" + result;
		if (type == String.class)
			return result == null ? "u" : "s" + result;
		/* LIVECONNECT_COMPLIANCE as in Chromium's bridge: an object that is not null would be exposed
		 * to the page with its own interface methods; here it is undefined */
		return result == null ? "n" : "u";
	}

	/* CoerceJavaScriptValueToJavaValue() of Chromium's gin_java_script_to_java_types_coercion.cc, with the
	 * value as "<typeof>:<value>" ("null:" for null): numbers are truncated to the parameter's type,
	 * everything that does not fit becomes 0, false or null; String parameters get numbers and booleans as
	 * text and undefined (or an object) as "undefined"; wrapper and other object types get null. */
	private static Object coerceJavascriptArgument(String typedValue, Class<?> type) {
		int colon = typedValue.indexOf(':');
		String jsType = typedValue.substring(0, colon);
		String value = typedValue.substring(colon + 1);
		boolean isNumber = jsType.equals("number");
		double number = isNumber ? Double.parseDouble(value) : 0;
		if (type == String.class) {
			if (jsType.equals("string"))
				return value;
			if (isNumber)
				return number == (int)number ? Integer.toString((int)number) : javascriptNumberToString(number);
			if (jsType.equals("boolean"))
				return value;
			return jsType.equals("null") ? null : "undefined";
		}
		if (type == boolean.class)
			return jsType.equals("boolean") && value.equals("true");
		if (type == char.class)
			return isNumber && number == (int)number ? (char)(int)number : (char)0;
		if (type == byte.class)
			return (byte)(int)number;
		if (type == short.class)
			return (short)(int)number;
		if (type == int.class)
			return (int)number;
		if (type == long.class)
			return (long)number;
		if (type == float.class)
			return (float)number;
		if (type == double.class)
			return number;
		return null;
	}

	// printf's "%.6lg", as Chromium formats a number for a String parameter
	private static String javascriptNumberToString(double number) {
		if (Double.isNaN(number) || Double.isInfinite(number))
			return Double.isNaN(number) ? "nan" : number > 0 ? "inf" : "-inf";
		BigDecimal rounded = new BigDecimal(number).round(new MathContext(6)).stripTrailingZeros();
		int exponent = rounded.precision() - rounded.scale() - 1;
		if (rounded.signum() == 0 || (exponent >= -4 && exponent < 6))
			return rounded.toPlainString();
		return rounded.unscaledValue() + "e" + (exponent < 0 ? "-" : "+") + (Math.abs(exponent) < 10 ? "0" : "") + Math.abs(exponent);
	}

	private void internalConsoleMessage(String level, String message) {
		ConsoleMessage.MessageLevel messageLevel;
		switch (level) {
			case "error":
				messageLevel = ConsoleMessage.MessageLevel.ERROR;
				break;
			case "warn":
				messageLevel = ConsoleMessage.MessageLevel.WARNING;
				break;
			case "debug":
				messageLevel = ConsoleMessage.MessageLevel.DEBUG;
				break;
			default:
				messageLevel = ConsoleMessage.MessageLevel.LOG;
		}
		ConsoleMessage consoleMessage = new ConsoleMessage(message, "", 0, messageLevel);
		if (webChromeClient == null || !webChromeClient.onConsoleMessage(consoleMessage))
			Log.i(TAG, "console: " + message);
	}

	// to be used by native code
	@Export
	void internalJsResult(ValueCallback<String> callback, String result) {
		callback.onReceiveValue(result);
	}

	public void setWebChromeClient(WebChromeClient webChromeClient) {
		this.webChromeClient = webChromeClient;
	}

	public void removeAllViews() {}

	/* Like Android: frees the page and the web content's resources, the WebView must not be used
	 * afterwards; calls to it are logged and ignored. */
	public void destroy() {
		if (destroyed)
			return;
		destroyed = true;
		javascriptInterfaces.clear();
		widget = native_destroy(widget);
	}

	private boolean isDestroyed() {
		if (destroyed)
			Log.w(TAG, "Application attempted to call on a destroyed WebView");
		return destroyed;
	}

	public void loadDataWithBaseURL(String baseUrl, String data, String mimeType, String encoding, String historyUrl) {
		if (isDestroyed())
			return;
		if ("base64".equals(encoding)) {
			data = new String(Base64.decode(data, 0));
		}
		if (mimeType != null && mimeType.contains(";")) {
			mimeType = mimeType.substring(0, mimeType.indexOf(";"));
		}
		// webkit doesn't allow overwriting the file:// uri scheme. So we replace it with the android-asset:// scheme
		data = data.replace("file:///android_asset/", "android-asset:///assets/");
		// relative URLs in the document are resolved against the base URL, so it needs the same treatment
		if (baseUrl != null)
			baseUrl = baseUrl.replace("file:///android_asset/", "android-asset:///assets/");
		native_loadDataWithBaseURL(widget, baseUrl, data, mimeType, encoding);
	}

	public void loadUrl(String url) {
		if (isDestroyed())
			return;
		if (url.startsWith("javascript:")) {
			native_evaluateJavascript(widget, url.substring("javascript:".length()), null);
			return;
		}
		// webkit doesn't allow overwriting the file:// uri scheme. So we replace it with the android-asset:// scheme
		url = url.replace("file:///android_asset/", "android-asset:///assets/");
		native_loadUrl(widget, url);
	}

	/* AOSP WebView.postUrl(): a network URL is loaded with a POST of postData, any other URL with loadUrl().
	 * WebKitGTK's public API has no request method or body, so a POST is not provided. */
	public void postUrl(String url, byte[] postData) {
		if (android.webkit.URLUtil.isNetworkUrl(url))
			throw new UnsupportedOperationException("WebView.postUrl(): POST is not provided by ATL's WebKitGTK backend");
		loadUrl(url);
	}

	/* AOSP WebView.loadUrl(String, Map): as loadUrl(String), the headers go with the request */
	public void loadUrl(String url, Map<String, String> additionalHttpHeaders) {
		if (additionalHttpHeaders == null || additionalHttpHeaders.isEmpty() || url.startsWith("javascript:")) {
			loadUrl(url);
			return;
		}
		if (isDestroyed())
			return;
		url = url.replace("file:///android_asset/", "android-asset:///assets/");
		String[] headers = new String[additionalHttpHeaders.size() * 2];
		int i = 0;
		for (Map.Entry<String, String> header : additionalHttpHeaders.entrySet()) {
			headers[i++] = header.getKey();
			headers[i++] = header.getValue();
		}
		native_loadUrlWithHeaders(widget, url, headers);
	}

	public String getUrl() {
		return url;
	}

	/* AOSP WebView.getOriginalUrl(): the URL asked for the current page, before redirects */
	public String getOriginalUrl() {
		if (isDestroyed())
			return null;
		String originalUrl = native_getOriginalUrl(widget);
		return originalUrl != null ? internalToAppUrl(originalUrl) : null;
	}

	/* AOSP WebView.getTitle(): null if no page has been loaded */
	public String getTitle() {
		if (isDestroyed())
			return null;
		return native_getTitle(widget);
	}

	/* AOSP WebView.getCertificate(): the SSL certificate of the main top-level page, null if there is none */
	public android.net.http.SslCertificate getCertificate() {
		if (isDestroyed())
			return null;
		byte[] der = native_getCertificate(widget);
		if (der == null)
			return null;
		try {
			java.security.cert.CertificateFactory factory = java.security.cert.CertificateFactory.getInstance("X.509");
			return new android.net.http.SslCertificate((java.security.cert.X509Certificate)factory.generateCertificate(new java.io.ByteArrayInputStream(der)));
		} catch (java.security.cert.CertificateException e) {
			return null;
		}
	}

	public boolean canGoBack() {
		return canGoBackOrForward(-1);
	}

	public void goBack() {
		goBackOrForward(-1);
	}

	public boolean canGoForward() {
		return canGoBackOrForward(1);
	}

	public void goForward() {
		goBackOrForward(1);
	}

	/* AOSP WebView.canGoBackOrForward(): whether the history has an entry steps away from the current one */
	public boolean canGoBackOrForward(int steps) {
		if (isDestroyed())
			return false;
		return native_canGoBackOrForward(widget, steps);
	}

	/* AOSP WebView.goBackOrForward(): nothing if the entry is not there */
	public void goBackOrForward(int steps) {
		if (isDestroyed())
			return;
		native_goBackOrForward(widget, steps);
	}

	public void stopLoading() {
		if (isDestroyed())
			return;
		native_stopLoading(widget);
	}

	public void reload() {
		native_reload(widget);
	}

	// to be used by native code
	@Export
	AssetManager internalGetAssetManager() {
		return getContext().getResources().getAssets();
	}

	public void onPause() {}

	public void onResume() {}

	public boolean isPaused() {
		return false;
	}

	/* AOSP WebView.pauseTimers()/resumeTimers(): pause JavaScript timers of all WebViews; WebKitGTK has no
	 * such switch, the timers keep running */
	public void pauseTimers() {}

	public void resumeTimers() {}

	public void loadData(String data, String mimeType, String encoding) {
		loadDataWithBaseURL("about:blank", data, mimeType, encoding, "about:blank");
	}

	public void evaluateJavascript(String script, ValueCallback<String> resultCallback) {
		if (isDestroyed())
			return;
		native_evaluateJavascript(widget, script, resultCallback);
	}

	public static void setWebContentsDebuggingEnabled(boolean enabled) {}

	/* AOSP WebView.findAddress(): the address finder is plain Java (android.webkit.FindAddress) */
	@Deprecated
	public static String findAddress(String addr) {
		if (addr == null) {
			throw new NullPointerException("addr is null");
		}
		return FindAddress.findAddress(addr);
	}

	/* AOSP WebView.getCurrentWebViewPackage(): the package of the WebView implementation; ATL's is WebKitGTK,
	 * which is part of the runtime and not a package, so it is described as one */
	public static android.content.pm.PackageInfo getCurrentWebViewPackage() {
		android.content.pm.PackageInfo info = new android.content.pm.PackageInfo();
		info.packageName = "org.webkit.webkitgtk";
		info.versionName = "2.0";
		info.versionCode = 1;
		return info;
	}

	/* AOSP WebView.getWebViewClassLoader() (@hide): the ClassLoader of the WebView implementation package. ATL's
	 * implementation is WebView itself, so it is the loader of this class; the implementation's own classes
	 * (e.g. the chromium support-lib boundary) are not in it, a lookup of them fails with ClassNotFoundException
	 * just as with a provider that does not have them. */
	public static ClassLoader getWebViewClassLoader() {
		return WebView.class.getClassLoader();
	}

	public void clearFormData() {}

	/* AOSP WebView.clearHistory(): the back/forward list keeps only the current entry */
	public void clearHistory() {
		if (isDestroyed())
			return;
		native_clearHistory(widget);
	}

	public void clearSslPreferences() {}

	/* AOSP WebView.clearCache(): the resource cache, which all WebViews of the app share; the disk cache only
	 * with includeDiskFiles */
	public void clearCache(boolean includeDiskFiles) {
		if (isDestroyed())
			return;
		native_clearCache(widget, includeDiskFiles);
	}

	// directly accessed by androidx WebViewGlueCommunicator to get ClassLoader
	private static Object getFactory() {
		return new Object();
	}

	public void removeJavascriptInterface(String name) {
		// like on Android, the object stays visible to JavaScript until the next page load
		javascriptInterfaces.remove(name);
	}

	// AwContents.getContentHeightCss(): the page's height in CSS pixels
	public int getContentHeight() {
		return contentHeightCss;
	}

	/* AwContents.getScale(): page scale times the display density; a CSS pixel is a dp (see
	 * undo_font_dpi_zoom() in android_webkit_WebView.c), and the page is not zoomed */
	public float getScale() {
		return getContext().getResources().getDisplayMetrics().density;
	}

	@Override
	protected native long native_constructor(Context context, AttributeSet attrs);
	private native void native_loadDataWithBaseURL(long widget, String baseUrl, String data, String mimeType, String encoding);
	private native void native_loadUrl(long widget, String url);
	private native void native_reload(long widget);
	private native void native_addJavascriptInterface(long widget, String name, String methodNames);
	private native void native_evaluateJavascript(long widget, String script, ValueCallback<String> callback);
	private native void native_finishJavaBridgeCall(long request, String response);
	private native void native_firePointerCancel(long widget);
	private native void native_scrollTo(long widget, float x, float y);
	private native void native_setVerticalScrollBarEnabled(long widget, boolean enabled);
	private native long native_destroy(long widget);
	private native void native_loadUrlWithHeaders(long widget, String url, String[] headers);
	private native void native_stopLoading(long widget);
	private native boolean native_canGoBackOrForward(long widget, int steps);
	private native void native_goBackOrForward(long widget, int steps);
	private native void native_clearHistory(long widget);
	private native String native_getTitle(long widget);
	private native String native_getOriginalUrl(long widget);
	private native byte[] native_getCertificate(long widget);
	private native void native_findAll(long widget, String find);
	private native void native_findNext(long widget, boolean forward);
	private native void native_clearMatches(long widget);
	private native void native_clearCache(long widget, boolean includeDiskFiles);
	/* the page is drawn by WebKit, a CSS background of the widget would stay hidden behind it */
	@Override
	protected native void native_setBackgroundColor(long widget, int color);

	/* Chromium's WebView provider (not part of the AOSP tree): saveWebArchive() reports failure with a null result
	 * when the page cannot be saved as MHTML (ATL's engine does not write web archives); no passwords are saved */
	public void saveWebArchive(String filename) {
		saveWebArchive(filename, false, null);
	}

	public void saveWebArchive(String basename, boolean autoname, ValueCallback<String> callback) {
		if (callback != null)
			new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> callback.onReceiveValue(null));
	}

	@Deprecated
	public boolean isPrivateBrowsingEnabled() {
		return false;
	}

	@Deprecated
	public void savePassword(String host, String username, String password) {}
}
