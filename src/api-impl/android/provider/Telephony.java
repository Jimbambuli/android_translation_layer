package android.provider;

import android.content.Context;
import android.net.Uri;

/* AOSP provider/Telephony.java, the content:// URIs of the SMS/MMS/carrier tables only (the column interfaces are
 * not ported). ATL has no telephony provider, so a query on them gets what a device without it answers. */
public class Telephony {

	public static final class Sms {
		public static final Uri CONTENT_URI = Uri.parse("content://sms");

		public static final class Inbox {
			public static final Uri CONTENT_URI = Uri.parse("content://sms/inbox");
		}

		public static final class Sent {
			public static final Uri CONTENT_URI = Uri.parse("content://sms/sent");
		}

		public static final class Draft {
			public static final Uri CONTENT_URI = Uri.parse("content://sms/draft");
		}

		public static final class Outbox {
			public static final Uri CONTENT_URI = Uri.parse("content://sms/outbox");
		}

		public static final class Conversations {
			public static final Uri CONTENT_URI = Uri.parse("content://sms/conversations");
		}

		/* AOSP: SmsApplication.getDefaultSmsApplication() - ATL has no SMS application, so there is no default */
		public static String getDefaultSmsPackage(Context context) {
			return null;
		}
	}

	public static final class Mms {
		public static final Uri CONTENT_URI = Uri.parse("content://mms");
		public static final Uri REPORT_REQUEST_URI = Uri.withAppendedPath(CONTENT_URI, "report-request");
		public static final Uri REPORT_STATUS_URI = Uri.withAppendedPath(CONTENT_URI, "report-status");

		public static final class Inbox {
			public static final Uri CONTENT_URI = Uri.parse("content://mms/inbox");
		}

		public static final class Sent {
			public static final Uri CONTENT_URI = Uri.parse("content://mms/sent");
		}

		public static final class Draft {
			public static final Uri CONTENT_URI = Uri.parse("content://mms/drafts");
		}

		public static final class Outbox {
			public static final Uri CONTENT_URI = Uri.parse("content://mms/outbox");
		}

		public static final class Addr {
			public static Uri getAddrUriForMessage(String messageId) {
				return Mms.CONTENT_URI.buildUpon().appendPath(String.valueOf(messageId)).appendPath("addr").build();
			}
		}

		public static final class Part {
			private static final String TABLE_PART = "part";
			public static final Uri CONTENT_URI = Uri.withAppendedPath(Mms.CONTENT_URI, TABLE_PART);

			public static Uri getPartUriForMessage(String messageId) {
				return Mms.CONTENT_URI.buildUpon().appendPath(String.valueOf(messageId)).appendPath(TABLE_PART).build();
			}
		}

		public static final class Rate {
			public static final Uri CONTENT_URI = Uri.withAppendedPath(Mms.CONTENT_URI, "rate");
		}
	}

	public static final class MmsSms {
		public static final Uri CONTENT_URI = Uri.parse("content://mms-sms/");
		public static final Uri CONTENT_CONVERSATIONS_URI = Uri.parse("content://mms-sms/conversations");
		public static final Uri CONTENT_FILTER_BYPHONE_URI = Uri.parse("content://mms-sms/messages/byphone");

		public static final class PendingMessages {
			public static final Uri CONTENT_URI = Uri.withAppendedPath(MmsSms.CONTENT_URI, "pending");
		}
	}

	public static final class Threads {
		public static final Uri CONTENT_URI = Uri.withAppendedPath(MmsSms.CONTENT_URI, "conversations");
		public static final Uri OBSOLETE_THREADS_URI = Uri.withAppendedPath(CONTENT_URI, "obsolete");
		public static final int COMMON_THREAD = 0;
		public static final int BROADCAST_THREAD = 1;
	}

	public static final class Carriers {
		public static final Uri CONTENT_URI = Uri.parse("content://telephony/carriers");
	}
}
