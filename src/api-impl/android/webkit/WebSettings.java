package android.webkit;

public class WebSettings {

	public static enum LayoutAlgorithm {
		NORMAL,
		NARROW_COLUMNS,
	}

	public static enum RenderPriority {
		HIGH,
	}

	public static String getDefaultUserAgent(android.content.Context context) {
		return "GDPR VIOLATION";
	}

	public String getUserAgentString() {
		return "GDPR VIOLATION";
	}

	public void setUserAgentString(String userAgentString) {}

	public void setSupportMultipleWindows(boolean supportMultipleWindows) {}

	public void setJavaScriptEnabled(boolean javaScriptEnabled) {}

	public void setSavePassword(boolean savePassword) {}

	public void setGeolocationEnabled(boolean enabled) {}

	public void setCacheMode(int dummy) {}

	public void setLayoutAlgorithm(LayoutAlgorithm layoutAlgorithm) {}

	public void setAllowFileAccess(boolean allowFileAccess) {}

	public void setBuiltInZoomControls(boolean builtInZoomControls) {}

	public void setDisplayZoomControls(boolean displayZoomControls) {}

	public void setLoadsImagesAutomatically(boolean loadsImagesAutomatically) {}

	public void setSupportZoom(boolean supportZoom) {}

	public void setUseWideViewPort(boolean useWideViewPort) {}

	public void setTextZoom(int textZoom) {}

	public void setAppCacheEnabled(boolean enabled) {}

	public void setAppCachePath(String path) {}

	private boolean loadWithOverviewMode;

	public void setLoadWithOverviewMode(boolean overview) {
		loadWithOverviewMode = overview;
	}

	public boolean getLoadWithOverviewMode() {
		return loadWithOverviewMode;
	}

	public void setNeedInitialFocus(boolean flag) {}
	public void setRenderPriority(RenderPriority priority) {}

	public void setBlockNetworkLoads(boolean block) {}

	public void setMixedContentMode(int mode) {}

	public void setBlockNetworkImage(boolean block) {}

	public void setDomStorageEnabled(boolean flag) {}

	public void setMediaPlaybackRequiresUserGesture(boolean require) {}

	public void setJavaScriptCanOpenWindowsAutomatically(boolean flag) {}

	public void setAppCacheMaxSize(long size) {}

	public void setDatabaseEnabled(boolean enabled) {}

	public void setDatabasePath(String path) {}

	public void setAllowUniversalAccessFromFileURLs(boolean allow) {}

	public void setGeolocationDatabasePath(String path) {}

	public void setSaveFormData(boolean save) {}

	public void setAllowContentAccess(boolean allow) {}

	public void setAllowFileAccessFromFileURLs(boolean allow) {}

	public void setStandardFontFamily(String font) {}

	public void setSerifFontFamily(String font) {}

	public void setSansSerifFontFamily(String font) {}

	public void setFixedFontFamily(String font) {}

	public void setCursiveFontFamily(String font) {}

	public void setFantasyFontFamily(String font) {}

	public void setDefaultFontSize(int size) {}

	public void setDefaultFixedFontSize(int size) {}

	public void setMinimumFontSize(int size) {}

	public void setOffscreenPreRaster(boolean enabled) {}

	public void setSafeBrowsingEnabled(boolean enabled) {}

	public void setForceDark(int forceDark) {}

	public void setAlgorithmicDarkeningAllowed(boolean allow) {}

	public int getTextZoom() {
		return 100;
	}

	public boolean getJavaScriptEnabled() {
		return true; // JavaScript is always enabled in our WebKitWebView
	}
}
