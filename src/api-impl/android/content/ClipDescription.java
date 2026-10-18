package android.content;

public class ClipDescription /*implements Parcelable*/ {
	public static final String MIMETYPE_TEXT_PLAIN = "text/plain";
	public static final String MIMETYPE_TEXT_HTML = "text/html";
	public static final String MIMETYPE_TEXT_URILIST = "text/uri-list";
	public static final String MIMETYPE_TEXT_INTENT = "text/vnd.android.intent";
	public static final String MIMETYPE_APPLICATION_ACTIVITY = "application/vnd.android.activity";
	public static final String MIMETYPE_APPLICATION_SHORTCUT = "application/vnd.android.shortcut";
	public static final String MIMETYPE_APPLICATION_TASK = "application/vnd.android.task";
	public static final String MIMETYPE_UNKNOWN = "application/octet-stream";
	public static final String EXTRA_PENDING_INTENT = "android.intent.extra.PENDING_INTENT";
	public static final String EXTRA_ACTIVITY_OPTIONS = "android.intent.extra.ACTIVITY_OPTIONS";
	public static final String EXTRA_LOGGING_INSTANCE_ID = "android.intent.extra.LOGGING_INSTANCE_ID";
	public static final String EXTRA_HIDE_DRAG_SOURCE_TASK_ID = "android.intent.extra.HIDE_DRAG_SOURCE_TASK_ID";
	public static final String EXTRA_IS_SENSITIVE = "android.content.extra.IS_SENSITIVE";
	public static final String EXTRA_IS_REMOTE_DEVICE = "android.content.extra.IS_REMOTE_DEVICE";
	public static final int CLASSIFICATION_NOT_COMPLETE = 1;
	public static final int CLASSIFICATION_NOT_PERFORMED = 2;
	public static final int CLASSIFICATION_COMPLETE = 3;

	private final CharSequence label;
	private final String[] mimeTypes;

	public ClipDescription(CharSequence label, String[] mimeTypes) {
		if (mimeTypes == null)
			throw new NullPointerException("mimeTypes is null");
		this.label = label;
		this.mimeTypes = mimeTypes;
	}

	public ClipDescription(ClipDescription o) {
		this(o.label, o.mimeTypes);
	}

	/**
	 * Compares a MIME type with a filter that may contain wildcards ("text/*", "*&#47;*").
	 */
	public static boolean compareMimeTypes(String concreteType, String desiredType) {
		if (desiredType.equals("*/*") || desiredType.equals(concreteType))
			return true;
		if (desiredType.endsWith("/*")) {
			String prefix = desiredType.substring(0, desiredType.length() - 1);
			return concreteType.startsWith(prefix);
		}
		return false;
	}

	public CharSequence getLabel() {
		return label;
	}

	public boolean hasMimeType(String mimeType) {
		for (String type : mimeTypes)
			if (compareMimeTypes(type, mimeType))
				return true;
		return false;
	}

	public String[] filterMimeTypes(String mimeType) {
		java.util.ArrayList<String> result = new java.util.ArrayList<>();
		for (String type : mimeTypes)
			if (compareMimeTypes(type, mimeType))
				result.add(type);
		return result.isEmpty() ? null : result.toArray(new String[0]);
	}

	public int getMimeTypeCount() {
		return mimeTypes.length;
	}

	public String getMimeType(int index) {
		return mimeTypes[index];
	}
}
