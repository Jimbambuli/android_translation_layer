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

	public WebView(Context context) {
		this(context, null);
	}

	public WebView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public WebView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
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

	public WebSettings getSettings() {
		return new WebSettings();
	}

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

	public void setVerticalScrollBarEnabled(boolean enabled) {}
	public void setVerticalScrollbarOverlay(boolean overlay) {}
	public void setInitialScale(int scaleInPercent) {}

	private final Map<String, Object> javascriptInterfaces = new HashMap<>();
	// like on Android, interface methods are called on a background thread
	private static ExecutorService javaBridgeExecutor;

	/* The object becomes available to JavaScript as window.<name> on the next page load. Calls are
	 * asynchronous (WebKitGTK script message handlers), so return values are not passed back. */
	public void addJavascriptInterface(Object object, String name) {
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

	// to be used by native code; message is "<interface>\u0001<method>\u0001<type>:<value>..."
	// or "\u0001console\u0001<level>\u0001<message>"
	@Export
	void internalJsMessage(String message) {
		final String[] parts = message.split("\u0001", -1);
		if (parts.length < 2)
			return;
		if (parts[0].isEmpty() && parts[1].equals("console")) {
			internalConsoleMessage(parts.length > 2 ? parts[2] : "log", parts.length > 3 ? parts[3] : "");
			return;
		}
		final Object object = javascriptInterfaces.get(parts[0]);
		if (object == null)
			return;
		if (javaBridgeExecutor == null)
			javaBridgeExecutor = Executors.newSingleThreadExecutor();
		javaBridgeExecutor.execute(new Runnable() {
			@Override
			public void run() {
				invokeJavascriptInterface(object, parts[1], Arrays.copyOfRange(parts, 2, parts.length));
			}
		});
	}

	private void invokeJavascriptInterface(Object object, String methodName, String[] jsArgs) {
		for (Method method : getJavascriptInterfaceMethods(object)) {
			Class<?>[] types = method.getParameterTypes();
			if (!method.getName().equals(methodName) || types.length != jsArgs.length)
				continue;
			Object[] args = new Object[jsArgs.length];
			for (int i = 0; i < jsArgs.length; i++)
				args[i] = convertJavascriptArgument(jsArgs[i], types[i]);
			try {
				method.invoke(object, args);
			} catch (ReflectiveOperationException e) {
				e.printStackTrace();
			}
			return;
		}
		Log.w(TAG, "no JavaScript interface method " + methodName + " with " + jsArgs.length + " arguments");
	}

	private static Object convertJavascriptArgument(String typedValue, Class<?> type) {
		int colon = typedValue.indexOf(':');
		String jsType = typedValue.substring(0, colon);
		String value = typedValue.substring(colon + 1);
		if (jsType.equals("undefined") || (jsType.equals("object") && value.equals("null")))
			return type.isPrimitive() ? convertJavascriptArgument("number:0", type) : null;
		if (type == int.class || type == Integer.class)
			return (int)Double.parseDouble(value);
		if (type == long.class || type == Long.class)
			return (long)Double.parseDouble(value);
		if (type == float.class || type == Float.class)
			return (float)Double.parseDouble(value);
		if (type == double.class || type == Double.class)
			return Double.parseDouble(value);
		if (type == boolean.class || type == Boolean.class)
			return Boolean.parseBoolean(value);
		return value;
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

	public void destroy() {}

	public void loadDataWithBaseURL(String baseUrl, String data, String mimeType, String encoding, String historyUrl) {
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
		native_evaluateJavascript(widget, script, resultCallback);
	}

	public static void setWebContentsDebuggingEnabled(boolean enabled) {}

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

	// TODO: return the height of the web content
	public int getContentHeight() {
		return getHeight();
	}

	public float getScale() {
		return 1.0f;
	}

	@Override
	protected native long native_constructor(Context context, AttributeSet attrs);
	private native void native_loadDataWithBaseURL(long widget, String baseUrl, String data, String mimeType, String encoding);
	private native void native_loadUrl(long widget, String url);
	private native void native_reload(long widget);
	private native void native_addJavascriptInterface(long widget, String name, String methodNames);
	private native void native_evaluateJavascript(long widget, String script, ValueCallback<String> callback);
	private native void native_firePointerCancel(long widget);
	/* the page is drawn by WebKit, a CSS background of the widget would stay hidden behind it */
	@Override
	protected native void native_setBackgroundColor(long widget, int color);
}
