package android.webkit;

public class WebSettings {

	/* the WebView whose WebKitSettings the setters change (null: a WebSettings without view) */
	private final WebView webView;
	private Boolean javaScriptEnabled;
	private boolean allowFileAccessFromFileURLs;
	private boolean allowUniversalAccessFromFileURLs;
	private String serifFontFamily = "serif";
	private String userAgent; /* null: the default */
	/* values the app set; null: not set, WebKit's default is in effect (see setJavaScriptEnabled()) */
	private Boolean domStorageEnabled;
	private Boolean loadsImagesAutomatically;
	private Boolean javaScriptCanOpenWindowsAutomatically;
	private Boolean mediaPlaybackRequiresUserGesture;
	private String defaultTextEncodingName;
	private String standardFontFamily = "sans-serif";
	private String sansSerifFontFamily = "sans-serif";
	private String fixedFontFamily = "monospace";
	private String cursiveFontFamily = "cursive";
	private String fantasyFontFamily = "fantasy";
	/* stored only, as in AwSettings (defaults from there): nothing in ATL's WebKit acts on them */
	private boolean supportMultipleWindows;
	private boolean safeBrowsingEnabled = true;
	private int forceDark = FORCE_DARK_AUTO;
	private int disabledActionModeMenuItems = MENU_ITEM_NONE;
	private boolean offscreenPreRaster;

	public static final int FORCE_DARK_OFF = 0;
	public static final int FORCE_DARK_AUTO = 1;
	public static final int FORCE_DARK_ON = 2;
	public static final int MENU_ITEM_NONE = 0;
	public static final int MENU_ITEM_SHARE = 1 << 0;
	public static final int MENU_ITEM_WEB_SEARCH = 1 << 1;
	public static final int MENU_ITEM_PROCESS_TEXT = 1 << 2;

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

	/* the string of an Android System WebView, built from what ATL reports as android.os.Build. Apps read the Chrome
	 * major version from it (Jellyfin refuses below 80); 120 is the last Chrome whose reduced user agent still names
	 * the model. WebKitGTK sends it too (WebView sets it on its WebKitSettings), so the app's pages see the user
	 * agent the app was told about */
	static String defaultUserAgent() {
		return "Mozilla/5.0 (Linux; Android " + android.os.Build.VERSION.RELEASE + "; " + android.os.Build.MODEL + " Build/" +
		       android.os.Build.ID + "; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/120.0.0.0 Mobile Safari/537.36";
	}

	public static String getDefaultUserAgent(android.content.Context context) {
		return defaultUserAgent();
	}

	public String getUserAgentString() {
		return userAgent != null ? userAgent : defaultUserAgent();
	}

	/* AOSP: null or an empty string restores the default */
	public void setUserAgentString(String userAgentString) {
		userAgent = (userAgentString == null || userAgentString.isEmpty()) ? null : userAgentString;
		if (webView != null)
			webView.setWebKitSetting("user-agent", getUserAgentString());
	}

	public void setSupportMultipleWindows(boolean supportMultipleWindows) {
		this.supportMultipleWindows = supportMultipleWindows;
	}

	public boolean supportMultipleWindows() {
		return supportMultipleWindows;
	}

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

	/* AOSP WebSettings.setLoadsImagesAutomatically(): WebKit auto-load-images */
	public void setLoadsImagesAutomatically(boolean loadsImagesAutomatically) {
		loadsImagesAutomatically = loadsImagesAutomatically;
		if (webView != null)
			webView.setWebKitSetting("auto-load-images", loadsImagesAutomatically);
	}

	public boolean getLoadsImagesAutomatically() {
		return loadsImagesAutomatically != null ? loadsImagesAutomatically : true; // WebKit's default until set
	}

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

	/* AOSP WebSettings.setDomStorageEnabled(): WebKit enable-html5-local-storage */
	public void setDomStorageEnabled(boolean flag) {
		domStorageEnabled = flag;
		if (webView != null)
			webView.setWebKitSetting("enable-html5-local-storage", flag);
	}

	public boolean getDomStorageEnabled() {
		return domStorageEnabled != null ? domStorageEnabled : true; // WebKit's default until set
	}

	/* AOSP WebSettings.setMediaPlaybackRequiresUserGesture(): WebKit media-playback-requires-user-gesture */
	public void setMediaPlaybackRequiresUserGesture(boolean require) {
		mediaPlaybackRequiresUserGesture = require;
		if (webView != null)
			webView.setWebKitSetting("media-playback-requires-user-gesture", require);
	}

	public boolean getMediaPlaybackRequiresUserGesture() {
		return mediaPlaybackRequiresUserGesture != null ? mediaPlaybackRequiresUserGesture : false; // WebKit's default until set
	}

	/* AOSP WebSettings.setJavaScriptCanOpenWindowsAutomatically(): WebKit javascript-can-open-windows-automatically */
	public void setJavaScriptCanOpenWindowsAutomatically(boolean flag) {
		javaScriptCanOpenWindowsAutomatically = flag;
		if (webView != null)
			webView.setWebKitSetting("javascript-can-open-windows-automatically", flag);
	}

	public boolean getJavaScriptCanOpenWindowsAutomatically() {
		return javaScriptCanOpenWindowsAutomatically != null ? javaScriptCanOpenWindowsAutomatically : false; // WebKit's default until set
	}

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

	/* AOSP WebSettings.setStandardFontFamily(): WebKit default-font-family */
	public void setStandardFontFamily(String font) {
		standardFontFamily = font;
		if (webView != null && font != null)
			webView.setWebKitSetting("default-font-family", font);
	}

	public String getStandardFontFamily() {
		return standardFontFamily;
	}

	/* AOSP WebSettings.setSerifFontFamily(): WebKit serif-font-family */
	public void setSerifFontFamily(String font) {
		serifFontFamily = font;
		if (webView != null)
			webView.setWebKitSetting("serif-font-family", font);
	}

	public String getSerifFontFamily() {
		return serifFontFamily;
	}

	/* AOSP WebSettings.setSansSerifFontFamily(): WebKit sans-serif-font-family */
	public void setSansSerifFontFamily(String font) {
		sansSerifFontFamily = font;
		if (webView != null && font != null)
			webView.setWebKitSetting("sans-serif-font-family", font);
	}

	public String getSansSerifFontFamily() {
		return sansSerifFontFamily;
	}

	/* AOSP WebSettings.setFixedFontFamily(): WebKit monospace-font-family */
	public void setFixedFontFamily(String font) {
		fixedFontFamily = font;
		if (webView != null && font != null)
			webView.setWebKitSetting("monospace-font-family", font);
	}

	public String getFixedFontFamily() {
		return fixedFontFamily;
	}

	/* AOSP WebSettings.setCursiveFontFamily(): WebKit cursive-font-family */
	public void setCursiveFontFamily(String font) {
		cursiveFontFamily = font;
		if (webView != null && font != null)
			webView.setWebKitSetting("cursive-font-family", font);
	}

	public String getCursiveFontFamily() {
		return cursiveFontFamily;
	}

	/* AOSP WebSettings.setFantasyFontFamily(): WebKit fantasy-font-family */
	public void setFantasyFontFamily(String font) {
		fantasyFontFamily = font;
		if (webView != null && font != null)
			webView.setWebKitSetting("fantasy-font-family", font);
	}

	public String getFantasyFontFamily() {
		return fantasyFontFamily;
	}

	public void setDefaultFontSize(int size) {}

	public void setDefaultFixedFontSize(int size) {}

	public void setMinimumFontSize(int size) {}

	public void setOffscreenPreRaster(boolean enabled) {
		offscreenPreRaster = enabled;
	}

	public boolean getOffscreenPreRaster() {
		return offscreenPreRaster;
	}

	public void setSafeBrowsingEnabled(boolean enabled) {
		safeBrowsingEnabled = enabled;
	}

	public boolean getSafeBrowsingEnabled() {
		return safeBrowsingEnabled;
	}

	public void setForceDark(int forceDark) {
		this.forceDark = forceDark;
	}

	public int getForceDark() {
		return forceDark;
	}

	public void setDisabledActionModeMenuItems(int menuItems) {
		disabledActionModeMenuItems = menuItems;
	}

	public int getDisabledActionModeMenuItems() {
		return disabledActionModeMenuItems;
	}

	/* AOSP WebSettings.setDefaultTextEncodingName(): WebKit default-charset */
	public void setDefaultTextEncodingName(String encoding) {
		defaultTextEncodingName = encoding;
		if (webView != null && encoding != null)
			webView.setWebKitSetting("default-charset", encoding);
	}

	public String getDefaultTextEncodingName() {
		return defaultTextEncodingName != null ? defaultTextEncodingName : "iso-8859-1"; // WebKit's default until set
	}

	public void setAlgorithmicDarkeningAllowed(boolean allow) {}

	public int getTextZoom() {
		return 100;
	}

	public boolean getJavaScriptEnabled() {
		return javaScriptEnabled != null ? javaScriptEnabled : true; // WebKit's default until set, see setJavaScriptEnabled()
	}
}
