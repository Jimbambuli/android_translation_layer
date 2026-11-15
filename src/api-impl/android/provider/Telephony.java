package android.provider;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
		/** AOSP Mms.NAME_ADDR_EMAIL_PATTERN, extractAddrSpec(), isEmailAddress(), isPhoneNumber() */
		public static final Pattern NAME_ADDR_EMAIL_PATTERN = Pattern.compile("\\s*(\"[^\"]*\"|[^<>\"]+)\\s*<([^<>]+)>\\s*");

		public static String extractAddrSpec(String address) {
			Matcher match = NAME_ADDR_EMAIL_PATTERN.matcher(address);
			if (match.matches())
				return match.group(2);
			return address;
		}

		public static boolean isEmailAddress(String address) {
			if (TextUtils.isEmpty(address))
				return false;
			String s = extractAddrSpec(address);
			Matcher match = Patterns.EMAIL_ADDRESS.matcher(s);
			return match.matches();
		}

		public static boolean isPhoneNumber(String number) {
			if (TextUtils.isEmpty(number))
				return false;
			Matcher match = Patterns.PHONE.matcher(number);
			return match.matches();
		}

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

		private static final String[] ID_PROJECTION = {BaseColumns._ID};
		private static final Uri THREAD_ID_CONTENT_URI = Uri.parse("content://mms-sms/threadID");

		/** AOSP Threads.getOrCreateThreadId(Context, String) */
		public static long getOrCreateThreadId(Context context, String recipient) {
			Set<String> recipients = new HashSet<String>();
			recipients.add(recipient);
			return getOrCreateThreadId(context, recipients);
		}

		/* AOSP: queries the telephony provider's threadID URI (through SqliteWrapper.query, which only adds a toast for
		 * a SQLiteException). ATL has no telephony provider, so the query returns no cursor and the call fails like on a
		 * device without one: IllegalArgumentException "Unable to find or allocate a thread ID." */
		public static long getOrCreateThreadId(Context context, Set<String> recipients) {
			Uri.Builder uriBuilder = THREAD_ID_CONTENT_URI.buildUpon();
			for (String recipient : recipients) {
				if (Mms.isEmailAddress(recipient))
					recipient = Mms.extractAddrSpec(recipient);
				uriBuilder.appendQueryParameter("recipient", recipient);
			}
			Uri uri = uriBuilder.build();
			Cursor cursor = context.getContentResolver().query(uri, ID_PROJECTION, null, null, null);
			if (cursor != null) {
				try {
					if (cursor.moveToFirst())
						return cursor.getLong(0);
					else
						Log.e("Telephony", "getOrCreateThreadId returned no rows!");
				} finally {
					cursor.close();
				}
			}
			Log.e("Telephony", "getOrCreateThreadId failed with " + recipients.size() + " recipients");
			throw new IllegalArgumentException("Unable to find or allocate a thread ID.");
		}
	}

	public static final class Carriers {
		public static final Uri CONTENT_URI = Uri.parse("content://telephony/carriers");
	}
}
