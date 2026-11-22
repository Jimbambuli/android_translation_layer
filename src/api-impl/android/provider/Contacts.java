package android.provider;

import android.net.Uri;

public class Contacts {
	/* AOSP Contacts.AUTHORITY (deprecated) */
	public static final String AUTHORITY = "contacts";

	public static class People {
		public static final Uri CONTENT_URI = Uri.parse("content://com.android.contacts/people");

		public static final Uri CONTENT_FILTER_URI = Uri.parse("content://com.android.contacts/people_filter");

		/* AOSP Contacts.People.Phones, PhonesColumns.NUMBER */
		public static final class Phones {
			public static final String NUMBER = "number";
		}
	}

	/* ATL: AOSP legacy Contacts.Phones, only the two members the telephony provider uses */
	public static final class Phones {
		public static final Uri CONTENT_FILTER_URL = Uri.parse("content://contacts/phones/filter");

		public static final String PERSON_ID = "person";
	}
}
