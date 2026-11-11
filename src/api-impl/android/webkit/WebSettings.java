package android.webkit;

public class WebSettings {

	/* the WebView whose WebKitSettings the setters change (null: a WebSettings without view) */
	private final WebView webView;
	private Boolean javaScriptEnabled;
	private boolean allowFileAccessFromFileURLs;
	private boolean allowUniversalAccessFromFileURLs;
	private String serifFontFamily = "serif";

	WebSettings(WebView webView) {
		this.webView = webView;
	}

	public WebSettings() {
		this(null);
	}

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

	/* AOSP WebSettings.setJavaScriptEnabled(): the page's scripts (WebKit enable-javascript-markup; ATL's own
	 * user scripts keep running). Until the app sets it, WebKit's default (on) stays, unlike AOSP's off */
	public void setJavaScriptEnabled(boolean javaScriptEnabled) {
		this.javaScriptEnabled = javaScriptEnabled;
		if (webView != null)
			webView.setWebKitSetting("enable-javascript-markup", javaScriptEnabled);
	}

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

	/* AOSP WebSettings.setAllowUniversalAccessFromFileURLs()/setAllowFileAccessFromFileURLs(): what scripts of
	 * file: pages may load (WebKit allow-universal-access-from-file-urls/allow-file-access-from-file-urls) */
	public void setAllowUniversalAccessFromFileURLs(boolean allow) {
		allowUniversalAccessFromFileURLs = allow;
		if (webView != null)
			webView.setWebKitSetting("allow-universal-access-from-file-urls", allow);
	}

	public boolean getAllowUniversalAccessFromFileURLs() {
		return allowUniversalAccessFromFileURLs;
	}

	public void setGeolocationDatabasePath(String path) {}

	public void setSaveFormData(boolean save) {}

	public void setAllowContentAccess(boolean allow) {}

	public void setAllowFileAccessFromFileURLs(boolean allow) {
		allowFileAccessFromFileURLs = allow;
		if (webView != null)
			webView.setWebKitSetting("allow-file-access-from-file-urls", allow);
	}

	public boolean getAllowFileAccessFromFileURLs() {
		return allowFileAccessFromFileURLs;
	}

	public void setStandardFontFamily(String font) {}

	/* AOSP WebSettings.setSerifFontFamily(): WebKit serif-font-family */
	public void setSerifFontFamily(String font) {
		serifFontFamily = font;
		if (webView != null)
			webView.setWebKitSetting("serif-font-family", font);
	}

	public String getSerifFontFamily() {
		return serifFontFamily;
	}

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
		return javaScriptEnabled != null ? javaScriptEnabled : true; // WebKit's default until set, see setJavaScriptEnabled()
	}
}
