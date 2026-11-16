package org.chromium.support_lib_glue;

import android.webkit.WebSettings;
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
		public String[] getSupportedFeatures() {
			return new String[] {"ALGORITHMIC_DARKENING"};
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
}
