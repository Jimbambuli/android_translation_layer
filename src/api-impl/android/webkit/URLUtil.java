package android.webkit;

public class URLUtil {

	public static String guessFileName(String url, String contentDisposition, String mimeType) {
		String filename = url.substring(url.lastIndexOf('/') + 1);
		if (filename.contains("?"))
			filename = filename.substring(0, filename.indexOf('?'));
		return filename;
	}

	/**
	 * @return {@code true} if the url is an https: url.
	 */
	public static boolean isHttpsUrl(String url) {
		return (null != url)
		    && (url.length() > 7)
		    && url.substring(0, 8).equalsIgnoreCase("https://");
	}

	public static boolean isHttpUrl(String url) {
		return (null != url)
		    && (url.length() > 6)
		    && url.substring(0, 7).equalsIgnoreCase("http://");
	}

	public static boolean isContentUrl(String url) {
		return url.startsWith("content://");
	}

	public static boolean isValidUrl(String url) {
		if (url == null)
			return false;
		return isHttpUrl(url) || isHttpsUrl(url) || url.startsWith("file://") || url.startsWith("about:")
		    || url.startsWith("data:") || url.startsWith("javascript:");
	}

	/* the methods below are the bodies of AOSP frameworks/base core/java/android/webkit/URLUtil.java */
	static final String ASSET_BASE = "file:///android_asset/";
	static final String RESOURCE_BASE = "file:///android_res/";
	static final String FILE_BASE = "file:";
	static final String PROXY_BASE = "file:///cookieless_proxy/";

	public static String composeSearchUrl(String inQuery, String template, String queryPlaceHolder) {
		int placeHolderIndex = template.indexOf(queryPlaceHolder);
		if (placeHolderIndex < 0) {
			return null;
		}
		StringBuilder buffer = new StringBuilder();
		buffer.append(template.substring(0, placeHolderIndex));
		try {
			buffer.append(java.net.URLEncoder.encode(inQuery, "utf-8"));
		} catch (java.io.UnsupportedEncodingException ex) {
			return null;
		}
		buffer.append(template.substring(placeHolderIndex + queryPlaceHolder.length()));
		return buffer.toString();
	}

	public static boolean isAssetUrl(String url) {
		return (null != url) && url.startsWith(ASSET_BASE);
	}

	public static boolean isResourceUrl(String url) {
		return (null != url) && url.startsWith(RESOURCE_BASE);
	}

	public static boolean isFileUrl(String url) {
		return (null != url) && (url.startsWith(FILE_BASE) && !url.startsWith(ASSET_BASE) && !url.startsWith(PROXY_BASE));
	}

	public static boolean isAboutUrl(String url) {
		return (null != url) && url.startsWith("about:");
	}

	public static boolean isDataUrl(String url) {
		return (null != url) && url.startsWith("data:");
	}

	public static boolean isJavaScriptUrl(String url) {
		return (null != url) && url.startsWith("javascript:");
	}

	public static boolean isNetworkUrl(String url) {
		if (url == null || url.length() == 0) {
			return false;
		}
		return isHttpUrl(url) || isHttpsUrl(url);
	}

	public static String guessUrl(String inUrl) {
		String retVal = inUrl;
		android.net.WebAddress webAddress;

		if (inUrl.length() == 0) return inUrl;
		if (inUrl.startsWith("about:")) return inUrl;
		// Do not try to interpret data scheme URLs
		if (inUrl.startsWith("data:")) return inUrl;
		// Do not try to interpret file scheme URLs
		if (inUrl.startsWith("file:")) return inUrl;
		// Do not try to interpret javascript scheme URLs
		if (inUrl.startsWith("javascript:")) return inUrl;

		// bug 762454: strip period off end of url
		if (inUrl.endsWith(".") == true) {
			inUrl = inUrl.substring(0, inUrl.length() - 1);
		}

		try {
			webAddress = new android.net.WebAddress(inUrl);
		} catch (android.net.ParseException ex) {
			return retVal;
		}

		// Check host
		if (webAddress.getHost().indexOf('.') == -1) {
			// no dot: user probably entered a bare domain.  try .com
			webAddress.setHost("www." + webAddress.getHost() + ".com");
		}
		return webAddress.toString();
	}
}
