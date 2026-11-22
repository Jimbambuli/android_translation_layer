package android.webkit;

import android.atl.annotation.Export;
import android.content.Context;
import android.content.res.AssetManager;
import android.os.Looper;
import android.text.TextUtils;
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
	// AOSP WebView: the renderer priorities of setRendererPriorityPolicy()
	public static final int RENDERER_PRIORITY_WAIVED = 0;
	public static final int RENDERER_PRIORITY_BOUND = 1;
	public static final int RENDERER_PRIORITY_IMPORTANT = 2;

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
	// WebViewFactory.sWebViewDisabled, see disableWebView()
	private static boolean webViewDisabled;
	// AwContents.mRendererPriority (as the WebView constant) and mRendererPriorityWaivedWhenNotVisible
	private int rendererRequestedPriority = RENDERER_PRIORITY_IMPORTANT;
	private boolean rendererPriorityWaivedWhenNotVisible;
	// the TextClassifier set by setTextClassifier(), Chromium's SmartSelectionProvider.mTextClassifier
	private android.view.textclassifier.TextClassifier textClassifier;
	// WebViewContentsClientAdapter.mPictureListener and mPictureListenerInvalidateOnly
	private PictureListener pictureListener;
	private boolean pictureListenerInvalidateOnly;
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
		this(context, attrs, defStyleAttr, 0, null, privateBrowsing);
	}

	protected WebView(Context context, AttributeSet attrs, int defStyleAttr, Map<String, Object> javaScriptInterfaces,
	                  boolean privateBrowsing) {
		this(context, attrs, defStyleAttr, 0, javaScriptInterfaces, privateBrowsing);
	}

	public WebView(Context context, AttributeSet attrs, int defStyleAttr) {
		this(context, attrs, defStyleAttr, 0);
	}

	public WebView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		this(context, attrs, defStyleAttr, defStyleRes, null, false);
	}

	/* AOSP: the constructor all others end in; WebViewChromium.init() adds the javaScriptInterfaces and handles privateBrowsing */
	protected WebView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes,
	                  Map<String, Object> javaScriptInterfaces, boolean privateBrowsing) {
		super(context, attrs, defStyleAttr, defStyleRes);
		if (context == null)
			throw new IllegalArgumentException("Invalid context argument");
		// AOSP WebView constructor: ensureProviderCreated(), then the provider's init()
		getFactory();
		if (privateBrowsing) {
			final String msg = "Private browsing is not supported in WebView.";
			if (context.getApplicationInfo().targetSdkVersion >= 19)
				throw new IllegalArgumentException(msg);
			android.util.Log.w("WebView", msg);
		}
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
		if (javaScriptInterfaces != null) {
			for (Map.Entry<String, Object> entry : javaScriptInterfaces.entrySet())
				addJavascriptInterface(entry.getValue(), entry.getKey());
		}
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

	private DownloadListener downloadListener;

	/* AOSP WebView.setDownloadListener(): a response the WebView can't show goes to the listener instead
	 * (internalDownloadStart()); without one it is dropped */
	public void setDownloadListener(DownloadListener downloadListener) {
		this.downloadListener = downloadListener;
	}

	public void setScrollBarStyle(int scrollBarStyle) {}

	public void setWebViewClient(WebViewClient webViewClient) {
		this.webViewClient = webViewClient;
	}

	/* SharedWebViewChromium: a WebView without client has a default WebViewClient (sNullWebViewClient) */
	private static final WebViewClient nullWebViewClient = new WebViewClient();

	public WebViewClient getWebViewClient() {
		return webViewClient != null ? webViewClient : nullWebViewClient;
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

	/* AOSP WebView.copyBackForwardList() as Chromium's WebBackForwardListChromium: never null, a copy of this
	 * WebView's history (empty, index -1, before the first page); the items as WebHistoryItemChromium: URL and
	 * original URL of the entry, its title, no favicon (ATL receives none, see getFavicon()) */
	public WebBackForwardList copyBackForwardList() {
		int[] currentIndex = {-1};
		String[] entries = isDestroyed() ? new String[0] : native_getHistory(widget, currentIndex);
		List<WebHistoryItem> items = new ArrayList<>();
		for (int i = 0; i + 2 < entries.length; i += 3)
			items.add(new HistoryItem(internalToAppUrl(entries[i]), internalToAppUrl(entries[i + 1]), entries[i + 2]));
		return new BackForwardList(items, items.isEmpty() ? -1 : currentIndex[0]);
	}

	private static class BackForwardList extends WebBackForwardList {
		private final List<WebHistoryItem> items;
		private final int currentIndex;

		BackForwardList(List<WebHistoryItem> items, int currentIndex) {
			this.items = items;
			this.currentIndex = currentIndex;
		}

		@Override
		public synchronized WebHistoryItem getCurrentItem() {
			return getSize() == 0 ? null : getItemAtIndex(getCurrentIndex());
		}

		@Override
		public synchronized int getCurrentIndex() {
			return currentIndex;
		}

		@Override
		public synchronized WebHistoryItem getItemAtIndex(int index) {
			return index < 0 || index >= getSize() ? null : items.get(index);
		}

		@Override
		public synchronized int getSize() {
			return items.size();
		}

		@Override
		protected synchronized WebBackForwardList clone() {
			List<WebHistoryItem> copy = new ArrayList<>();
			for (WebHistoryItem item : items)
				copy.add(((HistoryItem)item).clone());
			return new BackForwardList(copy, currentIndex);
		}
	}

	private static class HistoryItem extends WebHistoryItem {
		private final String url;
		private final String originalUrl;
		private final String title;

		HistoryItem(String url, String originalUrl, String title) {
			this.url = url;
			this.originalUrl = originalUrl;
			this.title = title;
		}

		@Override
		public int getId() {
			return -1;
		}

		@Override
		public String getUrl() {
			return url;
		}

		@Override
		public String getOriginalUrl() {
			return originalUrl;
		}

		@Override
		public String getTitle() {
			return title;
		}

		@Override
		public android.graphics.Bitmap getFavicon() {
			return null;
		}

		@Override
		protected synchronized HistoryItem clone() {
			return new HistoryItem(url, originalUrl, title);
		}
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

	public static void enableSlowWholeDocumentDraw() {
		getFactory();
	}

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

	/* AOSP WebView.canZoomIn()/canZoomOut() (deprecated): AwContents compares the page scale with its minimum and
	 * maximum; ATL's page scale stays at its only value (see zoomIn()), so neither way is open */
	@Deprecated
	public boolean canZoomIn() {
		return false;
	}

	@Deprecated
	public boolean canZoomOut() {
		return false;
	}

	/* AOSP WebView.invokeZoomPicker(): AwZoomControls shows the zoom buttons; ATL has no page zoom to offer
	 * (see zoomIn()) and no zoom buttons, so nothing is shown */
	public void invokeZoomPicker() {}

	/* AOSP WebView.getFavicon() is the provider's page icon, null until one was received; ATL does not deliver
	 * page icons. */
	public android.graphics.Bitmap getFavicon() {
		return null;
	}

	/* AOSP WebView.setRendererPriorityPolicy(): WebViewChromium maps the priority to AwContents' (an unknown one
	 * counts as RENDERER_PRIORITY_IMPORTANT), which stores it for its renderer process; ATL has no renderer process
	 * to rank, so it is only stored, and read back as Chromium does */
	public void setRendererPriorityPolicy(int rendererRequestedPriority, boolean waivedWhenNotVisible) {
		switch (rendererRequestedPriority) {
			case RENDERER_PRIORITY_WAIVED:
			case RENDERER_PRIORITY_BOUND:
				this.rendererRequestedPriority = rendererRequestedPriority;
				break;
			default:
				this.rendererRequestedPriority = RENDERER_PRIORITY_IMPORTANT;
		}
		rendererPriorityWaivedWhenNotVisible = waivedWhenNotVisible;
	}

	public int getRendererRequestedPriority() {
		return rendererRequestedPriority;
	}

	public boolean getRendererPriorityWaivedWhenNotVisible() {
		return rendererPriorityWaivedWhenNotVisible;
	}

	/* AOSP WebView.clearClientCertPreferences(): clears the remembered client certificate choices, then runs the
	 * callback on the UI thread. ATL keeps none (no ClientCertRequest persistence), so only the callback. */
	public static void clearClientCertPreferences(Runnable onCleared) {
		getFactory();
		if (onCleared != null)
			new android.os.Handler(Looper.getMainLooper()).post(onCleared);
	}

	/* AOSP WebView.startSafeBrowsing()/setSafeBrowsingWhitelist() hand over to the provider's statics. WebKit has
	 * no Safe Browsing: a provider without it reports false (not enabled / list not applied). */
	public static void startSafeBrowsing(Context context, ValueCallback<Boolean> callback) {
		getFactory();
		if (callback != null)
			new android.os.Handler(Looper.getMainLooper()).post(() -> callback.onReceiveValue(false));
	}

	public static void setSafeBrowsingWhitelist(List<String> hosts, ValueCallback<Boolean> callback) {
		getFactory();
		if (callback != null)
			new android.os.Handler(Looper.getMainLooper()).post(() -> callback.onReceiveValue(false));
	}

	/* AOSP WebView.getSafeBrowsingPrivacyPolicyUrl(): Chromium's AwContentsStatics answers with the Safe Browsing
	 * privacy policy and the app's language as "hl" (google_util::AppendGoogleLocaleParam(), "nb" as "no") */
	public static android.net.Uri getSafeBrowsingPrivacyPolicyUrl() {
		getFactory();
		String locale = java.util.Locale.getDefault().toLanguageTag();
		if (locale.equals("nb"))
			locale = "no";
		return android.net.Uri.parse("https://www.google.com/chrome/browser/privacy/?hl=" + android.net.Uri.encode(locale) + "#safe-browsing-policies");
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
		dataDirectorySuffix = suffix;
	}

	private static String dataDirectorySuffix;

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
		// AwContents.findAllAsync()
		if (find == null)
			throw new IllegalArgumentException("Search string shouldn't be null");
		boolean empty = find.isEmpty();
		findActiveMatch = 0;
		findMatchCount = 0;
		native_findAll(widget, find);
		if (empty && findListener != null)
			findListener.onFindResultReceived(0, 0, true);
	}

	/* AOSP WebView.findAll() (deprecated): WebViewChromium starts findAllAsync() and returns 0, the FindListener
	 * hears of the matches */
	@Deprecated
	public int findAll(String find) {
		android.os.StrictMode.noteSlowCall("findAll blocks UI: prefer findAllAsync");
		findAllAsync(find);
		return 0;
	}

	/* AOSP WebView.showFindDialog() (deprecated), WebViewChromium: false without a parent; otherwise it starts the
	 * find action mode (FindActionModeCallback), puts the text in and searches it as the action mode's findAll()
	 * does, and returns true. ATL's windows start no action modes (Activity.onWindowStartingActionMode() returns
	 * null), so the find bar itself does not appear; the search runs. */
	@Deprecated
	public boolean showFindDialog(String text, boolean showIme) {
		if (getParent() == null)
			return false;
		if (text != null) {
			if (text.isEmpty()) {
				clearMatches();
				findAll(null);
			} else {
				findAllAsync(text);
			}
		}
		return true;
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
			newPicture();
		}
	}

	/* as Chromium's AwContentsClientBridge.newDownload(): url, the user agent, Content-Disposition, MIME type and
	 * length of the response that is a download */
	@Export
	void internalDownloadStart(String url, String contentDisposition, String mimeType, long contentLength) {
		if (downloadListener != null)
			downloadListener.onDownloadStart(internalToAppUrl(url), getSettings().getUserAgentString(), contentDisposition, mimeType, contentLength);
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
		if (contentHeightCss != oldHeightCss || contentWidthCss != oldWidthCss)
			newPicture();
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

	/* AOSP WebView (deprecated): the scrollbar overlay setters do nothing, the getters answer what the old
	 * implementation defaulted to */
	@Deprecated
	public void setHorizontalScrollbarOverlay(boolean overlay) {}

	@Deprecated
	public boolean overlayHorizontalScrollbar() {
		return true;
	}

	@Deprecated
	public boolean overlayVerticalScrollbar() {
		return false;
	}

	/* AwScrollOffsetManager.pageUp()/pageDown(): a page (the view's height less PAGE_SCROLL_OVERLAP, half of it
	 * for a low view) up or down, or to the top or bottom; true when the page is to move. It moves with a smooth
	 * scroll, as the renderer's SmoothScroll() does in Chromium. */
	private static final int PAGE_SCROLL_OVERLAP = 24;

	public boolean pageUp(boolean top) {
		if (isDestroyed())
			return false;
		if (top)
			return animateScrollTo(getScrollX(), 0);
		int height = getHeight();
		int dy = height > 2 * PAGE_SCROLL_OVERLAP ? -height + PAGE_SCROLL_OVERLAP : -height / 2;
		return animateScrollTo(getScrollX(), getScrollY() + dy);
	}

	public boolean pageDown(boolean bottom) {
		if (isDestroyed())
			return false;
		if (bottom)
			return animateScrollTo(getScrollX(), computeVerticalScrollRange());
		int height = getHeight();
		int dy = height > 2 * PAGE_SCROLL_OVERLAP ? height - PAGE_SCROLL_OVERLAP : height / 2;
		return animateScrollTo(getScrollX(), getScrollY() + dy);
	}

	/* AwScrollOffsetManager.animateScrollTo(): clamped to the scrollable range, false if that is where the page is */
	private boolean animateScrollTo(int x, int y) {
		x = Math.max(0, Math.min(x, computeHorizontalScrollRange() - getWidth()));
		y = Math.max(0, Math.min(y, computeVerticalScrollRange() - getHeight()));
		if (x == getScrollX() && y == getScrollY())
			return false;
		smoothScrollTo(x, y);
		return true;
	}

	// the page scrolls smoothly to (x, y) in View pixels; it reports its new offset (internalScrollChanged())
	private void smoothScrollTo(int x, int y) {
		float scale = getScale();
		native_evaluateJavascript(widget, "scrollTo({left:" + (x / scale) + ",top:" + (y / scale) + ",behavior:'smooth'})", null);
	}

	/* AOSP WebView.flingScroll(): AwContents starts a fling of the page with this velocity (pixels per second, a
	 * positive one towards larger offsets). WebKit has no fling to start: the page moves smoothly to where an
	 * Android fling (OverScroller) with this velocity ends. */
	public void flingScroll(int vx, int vy) {
		if (isDestroyed())
			return;
		android.widget.OverScroller scroller = new android.widget.OverScroller(getContext());
		scroller.fling(getScrollX(), getScrollY(), vx, vy, 0, Math.max(0, computeHorizontalScrollRange() - getWidth()),
		               0, Math.max(0, computeVerticalScrollRange() - getHeight()));
		if (scroller.getFinalX() != getScrollX() || scroller.getFinalY() != getScrollY())
			smoothScrollTo(scroller.getFinalX(), scroller.getFinalY());
	}

	/* AOSP WebView.shouldDelayChildPressedState() (deprecated): WebViewChromium answers true */
	@Deprecated
	@Override
	public boolean shouldDelayChildPressedState() {
		return true;
	}

	/* AOSP WebView.clearView() (deprecated): AwContents shows only the background until the next page draws.
	 * WebKitGTK has no call to blank its drawing, so the page stays visible. */
	@Deprecated
	public void clearView() {}

	/* AOSP WebView.freeMemory() (deprecated): WebViewChromium does nothing, the engine manages its memory */
	@Deprecated
	public void freeMemory() {}

	/* AOSP WebView.setCertificate() and setMapTrackballToArrowKeys() (deprecated): WebViewChromium does nothing */
	@Deprecated
	public void setCertificate(android.net.http.SslCertificate certificate) {}

	@Deprecated
	public void setMapTrackballToArrowKeys(boolean setMap) {}

	/* AOSP WebView.get/setHttpAuthUsernamePassword() (deprecated): WebViewChromium keeps them in its
	 * WebViewDatabase */
	@Deprecated
	public String[] getHttpAuthUsernamePassword(String host, String realm) {
		return WebViewDatabase.getInstance(getContext()).getHttpAuthUsernamePassword(host, realm);
	}

	@Deprecated
	public void setHttpAuthUsernamePassword(String host, String realm, String username, String password) {
		WebViewDatabase.getInstance(getContext()).setHttpAuthUsernamePassword(host, realm, username, password);
	}

	/* AOSP WebView.get/setTextClassifier(): Chromium's selection keeps the classifier set, else it takes the one
	 * of the TextClassificationManager (SmartSelectionProvider.getTextClassifier()) */
	public android.view.textclassifier.TextClassifier getTextClassifier() {
		if (textClassifier != null)
			return textClassifier;
		android.view.textclassifier.TextClassificationManager manager = (android.view.textclassifier.TextClassificationManager)getContext().getSystemService(Context.TEXT_CLASSIFICATION_SERVICE);
		return manager != null ? manager.getTextClassifier() : android.view.textclassifier.TextClassifier.NO_OP;
	}

	public void setTextClassifier(android.view.textclassifier.TextClassifier textClassifier) {
		this.textClassifier = textClassifier;
	}

	/* AOSP WebView.documentHasImages(): AwContents asks the page whether it has an <img> element
	 * (GetImgChild() of aw_render_frame_ext.cc) and sends response with arg1 1 or 0 */
	public void documentHasImages(android.os.Message response) {
		if (isDestroyed())
			return;
		native_evaluateJavascript(widget, "document.getElementsByTagName('img').length>0", new ValueCallback<String>() {
			@Override
			public void onReceiveValue(String value) {
				response.arg1 = "true".equals(value) ? 1 : 0;
				response.sendToTarget();
			}
		});
	}

	/* AOSP WebView.capturePicture() (deprecated): AwContents records the page in its whole scroll range, null
	 * once destroyed. WebKitGTK only has the drawing of the visible part at hand (its widget), so the picture
	 * has the page's size and holds that part where it is on the page; the rest stays transparent. */
	@Deprecated
	public android.graphics.Picture capturePicture() {
		if (isDestroyed())
			return null;
		android.graphics.Picture picture = new android.graphics.Picture();
		android.graphics.Canvas canvas = picture.beginRecording(computeHorizontalScrollRange(), computeVerticalScrollRange());
		android.graphics.Bitmap visible = android.graphics.Bitmap.createBitmap(Math.max(getWidth(), 1), Math.max(getHeight(), 1), android.graphics.Bitmap.Config.ARGB_8888);
		if (native_capturePicture(widget, visible))
			canvas.drawBitmap(visible, getScrollX(), getScrollY(), null);
		picture.endRecording();
		return picture;
	}

	/* AOSP WebView.PictureListener (deprecated) */
	@Deprecated
	public interface PictureListener {
		@Deprecated
		void onNewPicture(WebView view, android.graphics.Picture picture);
	}

	/* AOSP WebView.setPictureListener() (deprecated): WebViewChromium tells the listener of each new picture of
	 * the page, without the picture (null) for apps targeting API 18 or later. Chromium's renderer reports every
	 * content update; WebKitGTK has no such event, so here a new picture is a finished load or a new page size. */
	@Deprecated
	public void setPictureListener(PictureListener listener) {
		pictureListener = listener;
		pictureListenerInvalidateOnly = getContext().getApplicationInfo().targetSdkVersion >= 18;
	}

	// WebViewContentsClientAdapter.onNewPicture()
	private void newPicture() {
		if (pictureListener != null)
			pictureListener.onNewPicture(this, pictureListenerInvalidateOnly ? null : capturePicture());
	}
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

	/* ---- WebChromeClient requests that WebKit raises (Chromium: WebViewContentsClientAdapter, AwContents) ---- */

	/* AwWebContentsDelegate.runFileChooser() -> WebViewContentsClientAdapter.showFileChooser(): the app's
	 * onShowFileChooser() decides; not handled (or no WebChromeClient) is no file, the legacy openFileChooser() is
	 * only for apps below Lollipop. The URIs the app returns are files for WebKit: file: URIs as they are,
	 * content: URIs (what a document picker returns) read through the ContentResolver into the WebView's cache. */
	@Export
	void internalShowFileChooser(final long request, boolean multiple, String[] mimeTypes) {
		if (webChromeClient == null) {
			native_finishFileChooser(request, null);
			return;
		}
		ValueCallback<android.net.Uri[]> callback = new ValueCallback<android.net.Uri[]>() {
			private boolean mCompleted;

			@Override
			public void onReceiveValue(final android.net.Uri[] uriList) {
				if (mCompleted)
					throw new IllegalStateException("showFileChooser result was already called");
				mCompleted = true;
				if (uriList == null) {
					native_finishFileChooser(request, null);
					return;
				}
				new Thread("WebView file chooser") {
					@Override
					public void run() {
						native_finishFileChooser(request, filesForUpload(uriList));
					}
				}.start();
			}
		};
		String acceptTypes = mimeTypes.length > 0 ? String.join(",", mimeTypes) : null;
		WebChromeClient.FileChooserParams params = new AtlFileChooserParams(multiple ? WebChromeClient.FileChooserParams.MODE_OPEN_MULTIPLE : WebChromeClient.FileChooserParams.MODE_OPEN, acceptTypes);
		if (webChromeClient.onShowFileChooser(this, callback, params))
			return;
		if (getContext().getApplicationInfo().targetSdkVersion >= android.os.Build.VERSION_CODES.LOLLIPOP) {
			callback.onReceiveValue(null);
			return;
		}
		native_finishFileChooser(request, null);
	}

	private static int uploadNumber;

	private String[] filesForUpload(android.net.Uri[] uris) {
		List<String> paths = new ArrayList<>();
		for (android.net.Uri uri : uris) {
			if (uri == null)
				continue;
			if ("file".equals(uri.getScheme()) || uri.getScheme() == null) {
				if (uri.getPath() != null)
					paths.add(uri.getPath());
				continue;
			}
			String name = null; // OpenableColumns.DISPLAY_NAME ("_display_name"; the class is not in ATL)
			try (android.database.Cursor cursor = getContext().getContentResolver().query(uri, new String[] {"_display_name"}, null, null, null)) {
				if (cursor != null && cursor.moveToFirst())
					name = cursor.getString(0);
			} catch (Exception e) {
			}
			if (name == null || name.isEmpty() || name.indexOf('/') >= 0)
				name = uri.getLastPathSegment() != null && uri.getLastPathSegment().indexOf('/') < 0 ? uri.getLastPathSegment() : "upload";
			int number;
			synchronized (WebView.class) {
				number = ++uploadNumber;
			}
			java.io.File dir = new java.io.File(getContext().getCacheDir(), "WebView/upload/" + number);
			dir.mkdirs();
			java.io.File file = new java.io.File(dir, name);
			try (InputStream in = getContext().getContentResolver().openInputStream(uri);
			     java.io.OutputStream out = new java.io.FileOutputStream(file)) {
				byte[] buffer = new byte[65536];
				int count;
				while ((count = in.read(buffer)) > 0)
					out.write(buffer, 0, count);
				paths.add(file.getPath());
			} catch (Exception e) {
				Log.w(TAG, "file chooser: can't read " + uri + ": " + e);
			}
		}
		return paths.toArray(new String[0]);
	}

	/* AwContents.onGeolocationPermissionsShowPrompt(): denied while WebSettings.setGeolocationEnabled(false), a
	 * retained decision (GeolocationPermissions) answers at once, otherwise the WebChromeClient's
	 * onGeolocationPermissionsShowPrompt(); without a WebChromeClient it is denied
	 * (WebViewContentsClientAdapter). An app that never invokes the callback leaves the page waiting, as there. */
	@Export
	void internalGeolocationPrompt(final long request, final String origin) {
		if (!getSettings().getGeolocationEnabled()) {
			native_finishPermission(request, false);
			return;
		}
		Boolean retained = GeolocationPermissions.getInstance().retained(origin);
		if (retained != null) {
			native_finishPermission(request, retained);
			return;
		}
		if (webChromeClient == null) {
			native_finishPermission(request, false);
			return;
		}
		webChromeClient.onGeolocationPermissionsShowPrompt(origin, new GeolocationPermissions.Callback() {
			private boolean answered;

			@Override
			public void invoke(String callbackOrigin, boolean allow, boolean retain) {
				synchronized (this) {
					if (answered)
						return;
					answered = true;
				}
				if (retain) {
					if (allow)
						GeolocationPermissions.getInstance().allow(callbackOrigin);
					else
						GeolocationPermissions.getInstance().deny(callbackOrigin);
				}
				native_finishPermission(request, allow);
			}
		});
	}

	/* AwPermissionRequest through WebViewContentsClientAdapter.onPermissionRequest() (PermissionRequestAdapter):
	 * the WebChromeClient grants or denies; WebChromeClient's own onPermissionRequest() denies */
	@Export
	void internalPermissionRequest(final long request, final String origin, boolean video, boolean audio) {
		List<String> resources = new ArrayList<>();
		if (video)
			resources.add(PermissionRequest.RESOURCE_VIDEO_CAPTURE);
		if (audio)
			resources.add(PermissionRequest.RESOURCE_AUDIO_CAPTURE);
		final String[] requested = resources.toArray(new String[0]);
		PermissionRequest permissionRequest = new PermissionRequest() {
			private boolean answered;

			@Override
			public android.net.Uri getOrigin() {
				return android.net.Uri.parse(origin);
			}

			@Override
			public String[] getResources() {
				return requested.clone();
			}

			/* PermissionRequestAdapter.grant(): allowed only if every requested resource is granted */
			@Override
			public void grant(String[] granted) {
				List<String> grantedList = granted != null ? Arrays.asList(granted) : new ArrayList<String>();
				answer(grantedList.containsAll(Arrays.asList(requested)));
			}

			@Override
			public void deny() {
				answer(false);
			}

			private void answer(boolean allow) {
				synchronized (this) {
					if (answered)
						return;
					answered = true;
				}
				native_finishPermission(request, allow);
			}
		};
		if (webChromeClient != null)
			webChromeClient.onPermissionRequest(permissionRequest);
		else
			permissionRequest.deny();
	}

	/* ---- androidx.webkit (org.chromium.support_lib_glue): WebMessageListener, document-start scripts, muting ---- */

	private static final class WebMessageListenerEntry {
		final String name;
		final AtlOriginMatcher matcher;
		final java.lang.reflect.InvocationHandler listener;

		WebMessageListenerEntry(String name, AtlOriginMatcher matcher, java.lang.reflect.InvocationHandler listener) {
			this.name = name;
			this.matcher = matcher;
			this.listener = listener;
		}
	}

	private static final class DocumentStartScript {
		final String script;
		final AtlOriginMatcher matcher;

		DocumentStartScript(String script, AtlOriginMatcher matcher) {
			this.script = script;
			this.matcher = matcher;
		}
	}

	/* read on the socket thread of the web process extension (internalFrameScripts()) */
	private final List<WebMessageListenerEntry> webMessageListeners = new java.util.concurrent.CopyOnWriteArrayList<>();
	private final List<DocumentStartScript> documentStartScripts = new java.util.concurrent.CopyOnWriteArrayList<>();

	/* AwContents.addWebMessageListener(): the name must be a JavaScript identifier, not used twice; invalid rules
	 * throw IllegalArgumentException. Frames get the object from their next document on. */
	public void addWebMessageListener(String jsObjectName, String[] allowedOriginRules, java.lang.reflect.InvocationHandler listener) {
		if (jsObjectName == null || !jsObjectName.matches("[A-Za-z_$][A-Za-z0-9_$]*"))
			throw new IllegalArgumentException("jsObjectName " + jsObjectName + " is not a valid JavaScript identifier");
		AtlOriginMatcher matcher = new AtlOriginMatcher(allowedOriginRules);
		for (WebMessageListenerEntry entry : webMessageListeners) {
			if (entry.name.equals(jsObjectName))
				throw new IllegalArgumentException("jsObjectName " + jsObjectName + " was already added.");
		}
		webMessageListeners.add(new WebMessageListenerEntry(jsObjectName, matcher, listener));
	}

	public void removeWebMessageListener(String jsObjectName) {
		for (WebMessageListenerEntry entry : webMessageListeners) {
			if (entry.name.equals(jsObjectName))
				webMessageListeners.remove(entry);
		}
	}

	/* AwContents.addDocumentStartJavaScript(): runs as a script of its own in each matching frame before the
	 * page's scripts, from the next document on; the handle's remove() takes it out again */
	public Runnable addDocumentStartJavaScript(String script, String[] allowedOriginRules) {
		if (script == null)
			throw new IllegalArgumentException("script shouldn't be null.");
		final DocumentStartScript entry = new DocumentStartScript(script, new AtlOriginMatcher(allowedOriginRules));
		documentStartScripts.add(entry);
		return new Runnable() {
			@Override
			public void run() {
				documentStartScripts.remove(entry);
			}
		};
	}

	/* the web process extension asks, on its socket thread, what a new document of a frame gets (see
	 * atl_webext.c): [0] installs the matching listener objects (Chromium's JsBinding: postMessage(), onmessage,
	 * add/removeEventListener("message")), then the matching document-start scripts */
	@Export
	String[] internalFrameScripts(String origin, boolean mainFrame) {
		List<String> scripts = new ArrayList<>();
		StringBuilder names = new StringBuilder();
		for (WebMessageListenerEntry entry : webMessageListeners) {
			if (entry.matcher.matches(origin))
				names.append(names.length() > 0 ? "," : "").append(jsString(entry.name));
		}
		scripts.add(names.length() == 0 ? "" : "(function(post){var d={};[" + names + "].forEach(function(name){"
		    + "var o={},ls=[];o.onmessage=null;"
		    + "o.postMessage=function(m){if(typeof m!=='string')throw new TypeError('postMessage: only strings are supported');post(name,m);};"
		    + "o.addEventListener=function(t,f){if(t==='message'&&typeof f==='function'&&ls.indexOf(f)<0)ls.push(f);};"
		    + "o.removeEventListener=function(t,f){var i=ls.indexOf(f);if(t==='message'&&i>=0)ls.splice(i,1);};"
		    + "d[name]=function(data){var e=new MessageEvent('message',{data:data});if(typeof o.onmessage==='function')o.onmessage(e);"
		    + "ls.slice().forEach(function(f){f(e);});};"
		    + "Object.defineProperty(window,name,{value:o,writable:true,configurable:true});});"
		    + "return function(name,data){if(d[name])d[name](data);};})");
		for (DocumentStartScript entry : documentStartScripts) {
			if (entry.matcher.matches(origin))
				scripts.add(entry.script);
		}
		return scripts.size() == 1 && scripts.get(0).isEmpty() ? null : scripts.toArray(new String[0]);
	}

	/* a listener object's postMessage(): WebMessageListener.onPostMessage(view, message, sourceOrigin,
	 * isMainFrame, replyProxy) through the app's boundary interfaces (SupportLibWebMessageListenerAdapter) */
	@Export
	void internalWebMessage(String name, String data, String origin, boolean mainFrame, final long frame, final long document) {
		for (WebMessageListenerEntry entry : webMessageListeners) {
			if (!entry.name.equals(name))
				continue;
			// the rules as the listener has them now (it may have been replaced since the frame got its object)
			if (!entry.matcher.matches(origin))
				return;
			final String jsObjectName = name;
			org.chromium.support_lib_glue.SupportLibReflectionUtil.postWebMessage(this, entry.listener, data, android.net.Uri.parse(origin), mainFrame, new org.chromium.support_lib_glue.SupportLibReflectionUtil.ReplyTarget() {
				@Override
				public void postMessage(String message) {
					if (!destroyed)
						native_postToFrame(widget, frame, document, jsObjectName, message);
				}
			});
			return;
		}
	}

	/* androidx WebViewCompat.setAudioMuted() / isAudioMuted() (AwContents): WebKit's mute of the page */
	public void setAudioMuted(boolean muted) {
		native_setAudioMuted(widget, muted);
	}

	public boolean isAudioMuted() {
		return native_isAudioMuted(widget);
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

	/* AOSP WebView.loadDataWithBaseURL() as Chromium's AwContents.loadDataWithBaseURL(): empty arguments become
	 * text/html and about:blank. A data: base URL makes it loadData(). Otherwise the data is the page as a plain
	 * string (encoding is not applied, the String is the content in UTF-8), the document has the base URL; the
	 * entry's URL is "data:<mimeType>;charset=utf-8;base64," (getOriginalUrl()), its virtual URL historyUrl
	 * (getUrl()). WebKit's reload of loaded data loads the base URL instead, so the native side keeps the data for
	 * reload() (struct data_load) */
	public void loadDataWithBaseURL(String baseUrl, String data, String mimeType, String encoding, String historyUrl) {
		if (isDestroyed())
			return;
		data = TextUtils.isEmpty(data) ? "" : data;
		mimeType = TextUtils.isEmpty(mimeType) ? "text/html" : mimeType;
		baseUrl = TextUtils.isEmpty(baseUrl) ? "about:blank" : baseUrl;
		historyUrl = TextUtils.isEmpty(historyUrl) ? "about:blank" : historyUrl;
		if (baseUrl.startsWith("data:")) {
			boolean isBase64 = "base64".equals(encoding);
			loadDataUrl(buildDataUri(data, mimeType, isBase64, isBase64 ? null : encoding));
			return;
		}
		String entryUrl = buildDataUri("", mimeType, true, "utf-8");
		if (mimeType.contains(";")) {
			mimeType = mimeType.substring(0, mimeType.indexOf(";"));
		}
		// webkit doesn't allow overwriting the file:// uri scheme. So we replace it with the android-asset:// scheme
		data = data.replace("file:///android_asset/", "android-asset:///assets/");
		// relative URLs in the document are resolved against the base URL, so it needs the same treatment
		baseUrl = baseUrl.replace("file:///android_asset/", "android-asset:///assets/");
		native_loadDataWithBaseURL(widget, baseUrl, data.getBytes(java.nio.charset.StandardCharsets.UTF_8), mimeType, historyUrl, entryUrl);
	}

	/* Chromium's LoadUrlParams.buildDataUri() */
	private static String buildDataUri(String data, String mimeType, boolean isBase64Encoded, String charset) {
		StringBuilder dataUrl = new StringBuilder("data:");
		dataUrl.append(mimeType);
		if (charset != null && !charset.isEmpty())
			dataUrl.append(";charset=" + charset);
		if (isBase64Encoded)
			dataUrl.append(";base64");
		dataUrl.append(",");
		dataUrl.append(data);
		return dataUrl.toString();
	}

	// a data: URL of loadData(): an ordinary URL load for WebKit, as for Chromium (LoadURLType.DATA)
	private void loadDataUrl(String dataUrl) {
		native_loadUrl(widget, dataUrl);
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
	 * Chromium (AwContents.postUrl(), LoadUrlParams.createLoadHttpPostParams()) sends postData as
	 * application/x-www-form-urlencoded. WebKitGTK's API has no request method or body: the POST is the submission
	 * of an HTML form with the fields of postData (decoded as that type; WebKit encodes them again), which WebKit
	 * submits in place of the page that holds it (native_postUrl()) */
	public void postUrl(String url, byte[] postData) {
		if (!android.webkit.URLUtil.isNetworkUrl(url)) {
			loadUrl(url);
			return;
		}
		if (isDestroyed())
			return;
		String formId = "atl-post-" + Long.toHexString(new java.security.SecureRandom().nextLong());
		StringBuilder form = new StringBuilder("<!DOCTYPE html><meta charset=\"utf-8\"><form id=\"").append(formId)
			.append("\" method=\"post\" enctype=\"application/x-www-form-urlencoded\" accept-charset=\"utf-8\" action=\"")
			.append(attributeEscape(url)).append("\">");
		String data = postData != null ? new String(postData, java.nio.charset.StandardCharsets.UTF_8) : "";
		for (String field : data.split("&")) {
			if (field.isEmpty())
				continue;
			int equals = field.indexOf('=');
			form.append("<input type=\"hidden\" name=\"").append(attributeEscape(formDecode(equals < 0 ? field : field.substring(0, equals))))
				.append("\" value=\"").append(attributeEscape(equals < 0 ? "" : formDecode(field.substring(equals + 1)))).append("\">");
		}
		form.append("</form>");
		native_postUrl(widget, url, form.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8), formId);
	}

	private static String attributeEscape(String s) {
		return s.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;");
	}

	/* application/x-www-form-urlencoded parsing of one name or value: '+' is a space, %XX a byte (UTF-8); a '%'
	 * without two hex digits stays as it is */
	private static String formDecode(String s) {
		java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
		byte[] in = s.replace('+', ' ').getBytes(java.nio.charset.StandardCharsets.UTF_8);
		for (int i = 0; i < in.length; i++) {
			if (in[i] == '%' && i + 2 < in.length && Character.digit(in[i + 1], 16) >= 0 && Character.digit(in[i + 2], 16) >= 0) {
				bytes.write(Character.digit(in[i + 1], 16) * 16 + Character.digit(in[i + 2], 16));
				i += 2;
			} else
				bytes.write(in[i]);
		}
		return new String(bytes.toByteArray(), java.nio.charset.StandardCharsets.UTF_8);
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

	/* Chromium (AwContents.getUrl()): the visible URL, for a page of loadDataWithBaseURL() its history URL */
	public String getUrl() {
		if (isDestroyed())
			return null;
		String historyUrl = native_getDataHistoryUrl(widget);
		return historyUrl != null ? historyUrl : url;
	}

	/* AOSP WebView.getOriginalUrl(): the URL asked for the current page, before redirects */
	public String getOriginalUrl() {
		if (isDestroyed())
			return null;
		String originalUrl = native_getOriginalUrl(widget);
		return originalUrl != null ? internalToAppUrl(originalUrl) : null;
	}

	/* AOSP WebView.getTitle() with Chromium as the provider: null only for a destroyed WebView (native_getTitle()) */
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
		if (isDestroyed())
			return;
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

	/* AOSP WebView.loadData() as Chromium's AwContents.loadData(): a data: URL of the data (LoadUrlParams.
	 * createLoadDataParams(), no charset), base64 only for encoding "base64"; for apps targeting below Q unencoded
	 * '#' are fixed up (CompatQuirks FIXUP_OCTOTHORPES_IN_LOAD_DATA) */
	public void loadData(String data, String mimeType, String encoding) {
		if (isDestroyed())
			return;
		boolean isBase64 = "base64".equals(encoding);
		if (data != null && data.contains("#") && !isBase64 && getContext().getApplicationInfo().targetSdkVersion < android.os.Build.VERSION_CODES.Q)
			data = fixupOctothorpesInLoadDataContent(data);
		loadDataUrl(buildDataUri(TextUtils.isEmpty(data) ? "" : data, TextUtils.isEmpty(mimeType) ? "text/html" : mimeType, isBase64, null));
	}

	private static final java.util.regex.Pattern dataUrlWithSelectorPattern = java.util.regex.Pattern.compile("^[^#]*(#[A-Za-z][A-Za-z0-9\\-_:.]*)$");

	// Chromium's AwContents.fixupOctothorpesInLoadDataContent()
	private static String fixupOctothorpesInLoadDataContent(String data) {
		java.util.regex.Matcher matcher = dataUrlWithSelectorPattern.matcher(data);
		String suffix = matcher.matches() ? matcher.group(1) : "";
		return data.replace("#", "%23") + suffix;
	}

	public void evaluateJavascript(String script, ValueCallback<String> resultCallback) {
		if (isDestroyed())
			return;
		native_evaluateJavascript(widget, script, resultCallback);
	}

	public static void setWebContentsDebuggingEnabled(boolean enabled) {
		getFactory();
	}

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
	 * implementation is WebView itself, so it is the loader of this class; androidx.webkit finds the support-library
	 * glue (org.chromium.support_lib_glue.SupportLibReflectionUtil) in it. */
	public static ClassLoader getWebViewClassLoader() {
		getFactory();
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

	/* AOSP WebView.getFactory() = WebViewFactory.getProvider(): fails once disableWebView() was called, otherwise the
	 * provider exists from then on. Also directly accessed by androidx WebViewGlueCommunicator to get ClassLoader */
	static synchronized Object getFactory() {
		if (webViewDisabled)
			throw new IllegalStateException("WebView.disableWebView() was called: WebView is disabled");
		if (!providerInitialized) {
			providerInitialized = true;
			/* AwBrowserProcess / PathUtils: the WebView's data in <data dir>/app_webview[_<suffix>] */
			Context app = android.app.ActivityThread.currentApplication();
			if (app != null) {
				native_setDataDirectory(app.getDataDir() + "/app_webview" + (dataDirectorySuffix != null ? "_" + dataDirectorySuffix : ""));
				native_setGeolocationProvider(new AtlGeolocationProvider(app));
			}
		}
		return new Object();
	}

	/* AOSP WebView.disableWebView() = WebViewFactory.disableWebView(): no WebView can be used in this process
	 * from now on, which is too late once one was */
	public static void disableWebView() {
		synchronized (WebView.class) {
			if (providerInitialized)
				throw new IllegalStateException("Can't disable WebView: WebView already initialized");
			webViewDisabled = true;
		}
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
	private native void native_loadDataWithBaseURL(long widget, String baseUrl, byte[] data, String mimeType, String historyUrl, String entryUrl);
	private native String native_getDataHistoryUrl(long widget);
	private native String[] native_getHistory(long widget, int[] currentIndex);
	private native void native_loadUrl(long widget, String url);
	private static native void native_finishFileChooser(long request, String[] paths);
	private static native void native_finishPermission(long request, boolean allow);
	static native void native_releaseRequest(long request);
	private native void native_postToFrame(long widget, long frame, long document, String name, String data);
	private native void native_setAudioMuted(long widget, boolean muted);
	private native boolean native_isAudioMuted(long widget);
	private static native void native_setDataDirectory(String directory);
	static native void native_setGeolocationProvider(Object provider);
	static native void native_geolocationUpdate(long manager, long timeMs, double latitude, double longitude, double accuracy,
	                                            double altitude, double altitudeAccuracy, double heading, double speed);
	static native void native_geolocationFailed(long manager, String message);
	private native void native_postUrl(long widget, String url, byte[] form, String formId);
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
	private native boolean native_capturePicture(long widget, android.graphics.Bitmap bitmap);
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
