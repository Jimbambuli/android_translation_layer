package android.webkit;

import android.atl.annotation.Export;
import android.content.Context;
import android.content.res.AssetManager;
import android.util.AttributeSet;
import android.util.Base64;
import android.view.MotionEvent;
import android.util.Log;
import android.view.ViewGroup;
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

public class WebView extends ViewGroup {
	private static final String TAG = "WebView";

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
	private boolean verticalScrollBarEnabled = true;
	private boolean horizontalScrollBarEnabled = true;

	public WebView(Context context) {
		this(context, null);
	}

	public WebView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public WebView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		// like Chromium's AwContents: the page shows the focus itself (focus rings, the caret)
		setDefaultFocusHighlightEnabled(false);
	}

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

	// undo the android-asset:// rewrite (see loadUrl()), apps expect the URL they loaded
	private static String internalToAppUrl(String url) {
		return url != null ? url.replace("android-asset:///assets/", "file:///android_asset/") : null;
	}

	// to be used by native code
	@Export
	void internalLoadChanged(int loadState, String url) {
		url = internalToAppUrl(url);
		this.url = url;
		if (loadState == /*WEBKIT_LOAD_STARTED*/ 0) {
			if (webViewClient != null)
				webViewClient.onPageStarted(this, url, null);
			if (webChromeClient != null)
				webChromeClient.onProgressChanged(this, 10);
		} else if (loadState == /*WEBKIT_LOAD_FINISHED*/ 3) {
			if (webChromeClient != null)
				webChromeClient.onProgressChanged(this, 100);
			if (webViewClient != null)
				webViewClient.onPageFinished(this, url);
		}
	}

	@Export
	boolean internalShouldOverrideUrlLoading(String url) {
		url = internalToAppUrl(url);
		if (webViewClient != null)
			return webViewClient.shouldOverrideUrlLoading(this, url);
		else
			return false;
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
		contentWidthCss = (int)Math.ceil(Double.parseDouble(parts[4]));
		contentHeightCss = (int)Math.ceil(Double.parseDouble(parts[5]));
		nativeScrollX = Math.round(Float.parseFloat(parts[2]) * scale);
		nativeScrollY = Math.round(Float.parseFloat(parts[3]) * scale);
		scrollTo(nativeScrollX, nativeScrollY);
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

	public String getUrl() {
		return url;
	}

	public void stopLoading() {}

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

	public void clearHistory() {}

	public void clearMatches() {}

	public void clearSslPreferences() {}

	public void clearCache(boolean includeDiskFiles) {}

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
	/* the page is drawn by WebKit, a CSS background of the widget would stay hidden behind it */
	@Override
	protected native void native_setBackgroundColor(long widget, int color);
}
