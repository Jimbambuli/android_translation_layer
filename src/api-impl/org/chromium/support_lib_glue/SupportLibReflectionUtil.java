package org.chromium.support_lib_glue;

import android.net.Uri;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/* The entry point of the WebView implementation for androidx.webkit: the support library looks this class up in
 * WebView.getWebViewClassLoader() and calls createWebViewProviderFactory() (Chromium android_webview/glue
 * SupportLibReflectionUtil). ATL's WebView is the implementation, so it answers here for the support-library
 * features it has. As Chromium's BoundaryInterfaceReflectionUtil.createInvocationHandlerFor(), a call on the app's
 * boundary interface goes to the delegate's public method of the same name and parameter types. */
public class SupportLibReflectionUtil {
	public static InvocationHandler createWebViewProviderFactory() {
		return createInvocationHandlerFor(new WebViewProviderFactory());
	}

	static InvocationHandler createInvocationHandlerFor(final Object delegate) {
		return new InvocationHandler() {
			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				try {
					return delegate.getClass().getMethod(method.getName(), method.getParameterTypes()).invoke(delegate, args);
				} catch (InvocationTargetException e) {
					throw e.getTargetException();
				}
			}
		};
	}

	/* WebViewProviderFactoryBoundaryInterface (Chromium SupportLibWebViewChromiumFactory) */
	public static class WebViewProviderFactory {
		/* the support-library features this implementation provides; androidx.webkit's WebViewFeature asks for
		 * these where the framework API has no method of its own */
		/* Features (support_lib_boundary util/Features.java) of what ATL's WebView really does; the framework API
		 * features (VISUAL_STATE_CALLBACK, POST_WEB_MESSAGE, SAFE_BROWSING_*, FORCE_DARK, ...) androidx decides by
		 * the SDK level and calls the framework methods. Not here, because WebKitGTK has no counterpart in ATL yet:
		 * WEB_MESSAGE_ARRAY_BUFFER (strings only), PROXY_OVERRIDE, MULTI_PROFILE, FORCE_DARK_STRATEGY,
		 * GET_VARIATIONS_HEADER and the other Chromium-only ones. */
		public String[] getSupportedFeatures() {
			return new String[] {"ALGORITHMIC_DARKENING", "WEB_MESSAGE_LISTENER", "DOCUMENT_START_SCRIPT:1", "MUTE_AUDIO",
			                     "VISUAL_STATE_CALLBACK", "GET_WEB_VIEW_CLIENT", "GET_WEB_CHROME_CLIENT"};
		}

		/* WebViewProviderBoundaryInterface for a WebView (Chromium SupportLibWebViewChromium) */
		public InvocationHandler createWebView(WebView webView) {
			return createInvocationHandlerFor(new WebViewProviderAdapter(webView));
		}

		public InvocationHandler getWebkitToCompatConverter() {
			return createInvocationHandlerFor(new WebkitToCompatConverter());
		}
	}

	/* WebkitToCompatConverterBoundaryInterface (Chromium SupportLibWebkitToCompatConverterAdapter) */
	public static class WebkitToCompatConverter {
		public InvocationHandler convertSettings(WebSettings settings) {
			return createInvocationHandlerFor(new WebSettingsAdapter(settings));
		}
	}

	/* WebSettingsBoundaryInterface (Chromium SupportLibWebSettingsAdapter, on the AwSettings of the WebSettings) */
	public static class WebSettingsAdapter {
		private final WebSettings settings;

		WebSettingsAdapter(WebSettings settings) {
			this.settings = settings;
		}

		public void setAlgorithmicDarkeningAllowed(boolean allow) {
			settings.setAlgorithmicDarkeningAllowed(allow);
		}

		public boolean isAlgorithmicDarkeningAllowed() {
			return settings.isAlgorithmicDarkeningAllowed();
		}
	}

	/* a method of a boundary interface as the app's androidx has it: androidx's InvocationHandlerWithDelegateGetter
	 * looks the method up by its declaring interface's name in the app's class loader (the interfaces are the
	 * app's, Chromium's glue carries its own copy in the WebView class loader; ATL's classes are the app's
	 * parents, so a copy here would replace the app's) */
	private static Object callBoundary(InvocationHandler handler, String iface, String name, Class<?>[] types, Object... args) {
		try {
			Class<?> boundary = Class.forName("org.chromium.support_lib_boundary." + iface, false, handler.getClass().getClassLoader());
			return handler.invoke(null, boundary.getMethod(name, types), args);
		} catch (RuntimeException e) {
			throw e;
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	private static boolean hasFeature(InvocationHandler handler, String iface, String feature) {
		try {
			String[] features = (String[])callBoundary(handler, iface, "getSupportedFeatures", new Class<?>[0]);
			return features != null && java.util.Arrays.asList(features).contains(feature);
		} catch (RuntimeException e) {
			return false;
		}
	}

	/* where JavaScriptReplyProxy.postMessage() goes (the frame and document whose object posted) */
	public interface ReplyTarget {
		void postMessage(String message);
	}

	/* SupportLibWebMessageListenerAdapter.onPostMessage(): string messages, no ports */
	public static void postWebMessage(WebView webView, InvocationHandler listener, String data, Uri sourceOrigin, boolean isMainFrame, ReplyTarget reply) {
		if (!hasFeature(listener, "WebMessageListenerBoundaryInterface", "WEB_MESSAGE_LISTENER")) {
			android.util.Log.e("WebMessageListenerAdapter", "The AndroidX doesn't have feature: WEB_MESSAGE_LISTENER");
			return;
		}
		callBoundary(listener, "WebMessageListenerBoundaryInterface", "onPostMessage",
		             new Class<?>[] {WebView.class, InvocationHandler.class, Uri.class, boolean.class, InvocationHandler.class},
		             webView, createInvocationHandlerFor(new WebMessageAdapter(data)), sourceOrigin, isMainFrame,
		             createInvocationHandlerFor(new JsReplyProxyAdapter(reply)));
	}

	/* WebMessageBoundaryInterface (SupportLibWebMessageAdapter) */
	public static class WebMessageAdapter {
		private final String data;

		WebMessageAdapter(String data) {
			this.data = data;
		}

		public String getData() {
			return data;
		}

		public InvocationHandler[] getPorts() {
			return null;
		}

		public String[] getSupportedFeatures() {
			return new String[0];
		}
	}

	/* JsReplyProxyBoundaryInterface (SupportLibJsReplyProxyAdapter), an IsomorphicObjectBoundaryInterface: the
	 * androidx peer is made once per proxy */
	public static class JsReplyProxyAdapter {
		private final ReplyTarget reply;
		private Object peer;

		JsReplyProxyAdapter(ReplyTarget reply) {
			this.reply = reply;
		}

		public void postMessage(String message) {
			reply.postMessage(message);
		}

		public synchronized Object getOrCreatePeer(java.util.concurrent.Callable<Object> creationCallable) {
			if (peer == null) {
				try {
					peer = creationCallable.call();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
			return peer;
		}
	}

	/* ScriptHandlerBoundaryInterface (SupportLibScriptHandlerAdapter) */
	public static class ScriptHandlerAdapter {
		private final Runnable remove;

		ScriptHandlerAdapter(Runnable remove) {
			this.remove = remove;
		}

		public void remove() {
			remove.run();
		}
	}

	/* WebViewProviderBoundaryInterface (Chromium SupportLibWebViewChromium), the methods of the features above */
	public static class WebViewProviderAdapter {
		private final java.lang.ref.WeakReference<WebView> webViewRef;

		WebViewProviderAdapter(WebView webView) {
			webViewRef = new java.lang.ref.WeakReference<>(webView);
		}

		private WebView webView() {
			WebView webView = webViewRef.get();
			if (webView == null)
				throw new IllegalStateException("Support lib method called on WebView that no longer exists.");
			return webView;
		}

		public void insertVisualStateCallback(long requestId, final InvocationHandler callback) {
			webView().postVisualStateCallback(requestId, new WebView.VisualStateCallback() {
				@Override
				public void onComplete(long completedRequestId) {
					callBoundary(callback, "VisualStateCallbackBoundaryInterface", "onComplete", new Class<?>[] {long.class}, completedRequestId);
				}
			});
		}

		public void addWebMessageListener(String jsObjectName, String[] allowedOriginRules, InvocationHandler listener) {
			webView().addWebMessageListener(jsObjectName, allowedOriginRules, listener);
		}

		public void removeWebMessageListener(String jsObjectName) {
			webView().removeWebMessageListener(jsObjectName);
		}

		public InvocationHandler addDocumentStartJavaScript(String script, String[] allowedOriginRules) {
			return createInvocationHandlerFor(new ScriptHandlerAdapter(webView().addDocumentStartJavaScript(script, allowedOriginRules)));
		}

		public WebViewClient getWebViewClient() {
			return webView().getWebViewClient();
		}

		public WebChromeClient getWebChromeClient() {
			return webView().getWebChromeClient();
		}

		public void setAudioMuted(boolean muted) {
			webView().setAudioMuted(muted);
		}

		public boolean isAudioMuted() {
			return webView().isAudioMuted();
		}
	}
}
