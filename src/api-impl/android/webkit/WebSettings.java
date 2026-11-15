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
	private int cacheMode = -1; // LOAD_DEFAULT, the documented default
	private boolean useWideViewPort; // documented default false
	private boolean blockNetworkImage; // documented default false

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

	/* AOSP WebSettings.LayoutAlgorithm */
	public static enum LayoutAlgorithm {
		NORMAL,
		@Deprecated
		SINGLE_COLUMN,
		@Deprecated
		NARROW_COLUMNS,
		TEXT_AUTOSIZING
	}

	public enum TextSize {
		SMALLEST(50),
		SMALLER(75),
		NORMAL(100),
		LARGER(150),
		LARGEST(200);

		TextSize(int size) {
			value = size;
		}

		int value;
	}

	public enum PluginState {
		ON,
		ON_DEMAND,
		OFF
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

	public void setSavePassword(boolean savePassword) { mSavePassword = savePassword; }

	public void setGeolocationEnabled(boolean enabled) {}

	public void setCacheMode(int mode) {
		cacheMode = mode;
	}

	public int getCacheMode() {
		return cacheMode;
	}

	public void setLayoutAlgorithm(LayoutAlgorithm layoutAlgorithm) { mLayoutAlgorithm = layoutAlgorithm; }

	public void setAllowFileAccess(boolean allowFileAccess) { mAllowFileAccess = allowFileAccess; }

	public void setBuiltInZoomControls(boolean builtInZoomControls) { mBuiltInZoomControls = builtInZoomControls; }

	public void setDisplayZoomControls(boolean displayZoomControls) { mDisplayZoomControls = displayZoomControls; }

	/* AOSP WebSettings.setLoadsImagesAutomatically(): WebKit auto-load-images */
	public void setLoadsImagesAutomatically(boolean loadsImagesAutomatically) {
		this.loadsImagesAutomatically = loadsImagesAutomatically;
		if (webView != null)
			webView.setWebKitSetting("auto-load-images", loadsImagesAutomatically);
	}

	public boolean getLoadsImagesAutomatically() {
		return loadsImagesAutomatically != null ? loadsImagesAutomatically : true; // WebKit's default until set
	}

	public void setSupportZoom(boolean supportZoom) { mSupportZoom = supportZoom; }

	public void setUseWideViewPort(boolean useWideViewPort) {
		this.useWideViewPort = useWideViewPort;
	}

	public boolean getUseWideViewPort() {
		return useWideViewPort;
	}

	public void setTextZoom(int textZoom) { mTextZoom = textZoom; }

	/* ContentSettingsAdapter: the text size is a text zoom of the enum's percentage */
	@Deprecated
	public void setTextSize(TextSize t) {
		setTextZoom(t.value);
	}

	/* plugins no longer exist; the Chromium adapter ignores the call */
	@Deprecated
	public void setPluginState(PluginState state) { mPluginState = state; }

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

	public void setBlockNetworkLoads(boolean block) { mBlockNetworkLoads = block; }

	public void setMixedContentMode(int mode) { mMixedContentMode = mode; }

	public void setBlockNetworkImage(boolean block) {
		blockNetworkImage = block;
	}

	public boolean getBlockNetworkImage() {
		return blockNetworkImage;
	}

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

	public void setDatabaseEnabled(boolean enabled) { mDatabaseEnabled = enabled; }

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

	public void setSaveFormData(boolean save) { mSaveFormData = save; }

	public void setAllowContentAccess(boolean allow) { mAllowContentAccess = allow; }

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

	public void setDefaultFontSize(int size) { mDefaultFontSize = Math.max(1, Math.min(72, size)); }

	public void setDefaultFixedFontSize(int size) { mDefaultFixedFontSize = Math.max(1, Math.min(72, size)); }

	public void setMinimumFontSize(int size) { mMinimumFontSize = Math.max(1, Math.min(72, size)); }

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

	public void setAlgorithmicDarkeningAllowed(boolean allow) { mAlgorithmicDarkeningAllowed = allow; }

	private int mTextZoom = 100;

	public int getTextZoom() {
		return mTextZoom;
	}

	public boolean getJavaScriptEnabled() {
		return javaScriptEnabled != null ? javaScriptEnabled : true; // WebKit's default until set, see setJavaScriptEnabled()
	}

	/* Chromium's AwSettings defaults (the WebView of Android 16) for what the setters keep */
	private boolean mSavePassword = true;
	private boolean mSaveFormData = true;
	private LayoutAlgorithm mLayoutAlgorithm = LayoutAlgorithm.NARROW_COLUMNS;
	private boolean mAllowFileAccess = true;
	private boolean mAllowContentAccess = true;
	private boolean mBuiltInZoomControls = false;
	private boolean mDisplayZoomControls = true;
	private boolean mSupportZoom = true;
	private PluginState mPluginState = PluginState.OFF;
	private boolean mBlockNetworkLoads = false;
	private int mMixedContentMode = 1;
	private boolean mDatabaseEnabled = false;
	private int mDefaultFontSize = 16;
	private int mDefaultFixedFontSize = 13;
	private int mMinimumFontSize = 8;
	private int mMinimumLogicalFontSize = 8;
	private boolean mAlgorithmicDarkeningAllowed = false;
	private boolean mLightTouchEnabled = false;
	private boolean mEnableSmoothTransition = false;
	private ZoomDensity mDefaultZoom = ZoomDensity.MEDIUM;

	public enum ZoomDensity {
		FAR(150), MEDIUM(100), CLOSE(75);
		ZoomDensity(int size) { value = size; }
		final int value;
	}

	public boolean getSavePassword() { return mSavePassword; }
	public boolean getSaveFormData() { return mSaveFormData; }
	public LayoutAlgorithm getLayoutAlgorithm() { return mLayoutAlgorithm; }
	public boolean getAllowFileAccess() { return mAllowFileAccess; }
	public boolean getAllowContentAccess() { return mAllowContentAccess; }
	public boolean getBuiltInZoomControls() { return mBuiltInZoomControls; }
	public boolean getDisplayZoomControls() { return mDisplayZoomControls; }
	public boolean supportZoom() { return mSupportZoom; }
	public PluginState getPluginState() { return mPluginState; }
	public boolean getBlockNetworkLoads() { return mBlockNetworkLoads; }
	public int getMixedContentMode() { return mMixedContentMode; }
	public boolean getDatabaseEnabled() { return mDatabaseEnabled; }
	public String getDatabasePath() { return ""; }
	public int getDefaultFontSize() { return mDefaultFontSize; }
	public int getDefaultFixedFontSize() { return mDefaultFixedFontSize; }
	public int getMinimumFontSize() { return mMinimumFontSize; }
	public int getMinimumLogicalFontSize() { return mMinimumLogicalFontSize; }
	public void setMinimumLogicalFontSize(int size) { mMinimumLogicalFontSize = Math.max(1, Math.min(72, size)); }
	public boolean isAlgorithmicDarkeningAllowed() { return mAlgorithmicDarkeningAllowed; }
	public boolean getLightTouchEnabled() { return mLightTouchEnabled; }
	public void setLightTouchEnabled(boolean enabled) { mLightTouchEnabled = enabled; }
	public void setEnableSmoothTransition(boolean enable) { mEnableSmoothTransition = enable; }
	public boolean enableSmoothTransition() { return mEnableSmoothTransition; }
	public ZoomDensity getDefaultZoom() { return mDefaultZoom; }
	public void setDefaultZoom(ZoomDensity zoom) { mDefaultZoom = zoom; }
	public TextSize getTextSize() {
		int closestValue = Integer.MAX_VALUE;
		TextSize closestSize = null;
		for (TextSize ts : TextSize.values()) {
			int diff = Math.abs(ts.value - getTextZoom());
			if (diff == 0)
				return ts;
			if (diff < closestValue) {
				closestValue = diff;
				closestSize = ts;
			}
		}
		return closestSize;
	}
}
