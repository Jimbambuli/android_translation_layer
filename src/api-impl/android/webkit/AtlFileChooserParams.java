package android.webkit;

import android.content.Intent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/* The parameters of a file chooser as Chromium's AwContentsClient.FileChooserParamsImpl (android_webview), handed to
 * WebChromeClient.onShowFileChooser() as WebViewContentsClientAdapter.fromAwFileChooserParams() does. WebKitGTK
 * reports of the <input> only the MIME types of its accept attribute (no file extensions, no capture attribute,
 * no title or default name), so acceptTypes is those MIME types, comma separated, and capture is false. */
final class AtlFileChooserParams extends WebChromeClient.FileChooserParams {
	private final int mMode;
	private final String mAcceptTypes;
	private static final Map<String, String> sAcceptTypesMapping;

	static {
		// Mapping for converting file extensions to MIME types (AwContentsClient.FileChooserParamsImpl)
		String[] tuples = new String[] {
			"application/*","application/*",
			"audio/*","audio/*",
			"font/*","font/*",
			"image/*","image/*",
			"text/*","text/*",
			"video/*","video/*",
			".aac","audio/aac",
			".abw","application/x-abiword",
			".arc","application/x-freearc",
			".avif","image/avif",
			".avi","video/x-msvideo",
			".azw","application/vnd.amazon.ebook",
			".bin","application/octet-stream",
			".bmp","image/bmp",
			".bz","application/x-bzip",
			".bz2","application/x-bzip2",
			".cda","application/x-cdf",
			".csh","application/x-csh",
			".css","text/css",
			".csv","text/csv",
			".doc","application/msword",
			".docx","application/vnd.openxmlformats-officedocument.wordprocessingml.document",
			".eot","application/vnd.ms-fontobject",
			".epub","application/epub+zip",
			".gz","application/gzip",
			".gif","image/gif",
			".htm","text/html",
			".html","text/html",
			".ico","image/vnd.microsoft.icon",
			".ics","text/calendar",
			".jar","application/java-archive",
			".jpeg","image/jpeg",
			".jpg","image/jpeg",
			".js","text/javascript",
			".json","application/json",
			".jsonld","application/ld+json",
			".mid","audio/midi",
			".midi","audio/midi",
			".mjs","text/javascript",
			".mp3","audio/mpeg",
			".mp4","video/mp4",
			".mpeg","video/mpeg",
			".mpkg","application/vnd.apple.installer+xml",
			".odp","application/vnd.oasis.opendocument.presentation",
			".ods","application/vnd.oasis.opendocument.spreadsheet",
			".odt","application/vnd.oasis.opendocument.text",
			".oga","audio/ogg",
			".ogv","video/ogg",
			".ogx","application/ogg",
			".opus","audio/opus",
			".otf","font/otf",
			".png","image/png",
			".pdf","application/pdf",
			".php","application/x-httpd-php",
			".ppt","application/vnd.ms-powerpoint",
			".pptx","application/vnd.openxmlformats-officedocument.presentationml.presentation",
			".rar","application/vnd.rar",
			".rtf","application/rtf",
			".sh","application/x-sh",
			".svg","image/svg+xml",
			".swf","application/x-shockwave-flash",
			".tar","application/x-tar",
			".tif","image/tiff",
			".tiff","image/tiff",
			".ts","video/mp2t",
			".ttf","font/ttf",
			".txt","text/plain",
			".vsd","application/vnd.visio",
			".wav","audio/wav",
			".weba","audio/webm",
			".webm","video/webm",
			".webp","image/webp",
			".woff","font/woff",
			".woff2","font/woff2",
			".xhtml","application/xhtml+xml",
			".xls","application/vnd.ms-excel",
			".xlsx","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
			".xml","application/xml",
			".xul","application/vnd.mozilla.xul+xml",
			".zip","application/zip",
			".3gp","video/3gpp",
			".3g2","video/3gpp2",
			".7z","application/x-7z-compressed",
		};
		Map<String, String> map = new HashMap<String, String>(tuples.length / 2);
		for (int i = 0; i < tuples.length; i += 2) {
			map.put(tuples[i], tuples[i + 1]);
		}
		sAcceptTypesMapping = map;
	}

	AtlFileChooserParams(int mode, String acceptTypes) {
		mMode = mode;
		mAcceptTypes = acceptTypes;
	}

	@Override
	public int getMode() {
		return mMode;
	}

	@Override
	public String[] getAcceptTypes() {
		if (mAcceptTypes == null) {
			return new String[0];
		}
		return mAcceptTypes.split(",");
	}

	@Override
	public boolean isCaptureEnabled() {
		return false;
	}

	@Override
	public CharSequence getTitle() {
		return null;
	}

	@Override
	public String getFilenameHint() {
		return null;
	}

	@Override
	public int getPermissionMode() {
		return PERMISSION_MODE_READ;
	}

	/* FileChooserParamsImpl.createIntent() for the modes WebKit asks for (open one / several files, readable) */
	@Override
	public Intent createIntent() {
		String mimeType = "*/*";
		Intent i = new Intent(Intent.ACTION_GET_CONTENT);
		i.addCategory(Intent.CATEGORY_OPENABLE);
		if (getMode() == MODE_OPEN_MULTIPLE) {
			i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
		}
		if (mAcceptTypes != null && !mAcceptTypes.trim().isEmpty()) {
			String[] acceptTypesArray = getAcceptTypes();
			if (acceptTypesArray.length > 0) {
				String[] mimeTypesToAccept = getMimeTypesToAccept(acceptTypesArray);
				if (mimeTypesToAccept.length > 0) {
					if (!mimeTypesToAccept[0].trim().isEmpty()) {
						mimeType = mimeTypesToAccept[0];
					}
					i.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypesToAccept);
				}
			}
		}
		i.setType(mimeType);
		return i;
	}

	private String[] getMimeTypesToAccept(String[] acceptTypesList) {
		ArrayList<String> acceptTypesArray = new ArrayList<String>();
		for (int i = 0; i < acceptTypesList.length; i++) {
			if (sAcceptTypesMapping.containsKey(acceptTypesList[i])) {
				acceptTypesArray.add(sAcceptTypesMapping.get(acceptTypesList[i]));
			} else if (sAcceptTypesMapping.containsValue(acceptTypesList[i])) {
				acceptTypesArray.add(acceptTypesList[i]);
			}
		}
		return acceptTypesArray.toArray(new String[acceptTypesArray.size()]);
	}
}
