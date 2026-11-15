package android.provider;

import android.net.Uri;

/* AOSP provider/CallLog.java, the content:// URIs only (the columns and the Calls helpers are not ported).
 * ATL has no call log provider, so a query on them gets what a device without telephony answers. */
public class CallLog {
	public static final String AUTHORITY = "call_log";
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);

	public static class Calls {
		public static final Uri CONTENT_URI = Uri.parse("content://call_log/calls");
	}
}
