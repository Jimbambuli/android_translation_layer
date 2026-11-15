package android.provider;

import android.net.Uri;

/* AOSP frameworks/base/core/java/android/provider/CallLog.java: contract constants only. ATL has no call log
 * provider, a query on these URIs finds no provider as on a device without one. */
public class CallLog {
	public static final String AUTHORITY = "call_log";
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);

	public static class Calls implements BaseColumns {
		public static final Uri CONTENT_URI = Uri.parse("content://call_log/calls");
		public static final Uri SHADOW_CONTENT_URI = Uri.parse("content://call_log_shadow/calls");
		public static final Uri CONTENT_FILTER_URI = Uri.parse("content://call_log/calls/filter");
		public static final Uri CONTENT_VOIP_URI = Uri.parse(CONTENT_URI + "/voip");
		public static final String LIMIT_PARAM_KEY = "limit";
		public static final String OFFSET_PARAM_KEY = "offset";
		public static final String ALLOW_VOICEMAILS_PARAM_KEY = "allow_voicemails";
		public static final String INCLUDE_VOIP_CALLS_PARAM_KEY = "include_voip_calls";
		public static final String EXTRA_CALL_TYPE_FILTER = "android.provider.extra.CALL_TYPE_FILTER";
		public static final Uri CONTENT_URI_WITH_VOICEMAIL = CONTENT_URI.buildUpon() .appendQueryParameter(ALLOW_VOICEMAILS_PARAM_KEY, "true") .build();
		public static final Uri CONTENT_URI_WITH_VOIP_CALLS = CONTENT_URI.buildUpon() .appendQueryParameter(INCLUDE_VOIP_CALLS_PARAM_KEY, "true") .build();
		public static final String DEFAULT_SORT_ORDER = "date DESC";
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/calls";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/calls";
		public static final String TYPE = "type";
		public static final int INCOMING_TYPE = 1;
		public static final int OUTGOING_TYPE = 2;
		public static final int MISSED_TYPE = 3;
		public static final int VOICEMAIL_TYPE = 4;
		public static final int REJECTED_TYPE = 5;
		public static final int BLOCKED_TYPE = 6;
		public static final int ANSWERED_EXTERNALLY_TYPE = 7;
		public static final String FEATURES = "features";
		public static final int FEATURES_VIDEO = 1 << 0;
		public static final int FEATURES_PULLED_EXTERNALLY = 1 << 1;
		public static final int FEATURES_HD_CALL = 1 << 2;
		public static final int FEATURES_WIFI = 1 << 3;
		public static final int FEATURES_ASSISTED_DIALING_USED = 1 << 4;
		public static final int FEATURES_RTT = 1 << 5;
		public static final int FEATURES_VOLTE = 1 << 6;
		public static final String NUMBER = "number";
		public static final String IS_BUSINESS_CALL = "is_business_call";
		public static final String ASSERTED_DISPLAY_NAME = "asserted_display_name";
		public static final String NUMBER_PRESENTATION = "presentation";
		public static final int PRESENTATION_ALLOWED = 1;
		public static final int PRESENTATION_RESTRICTED = 2;
		public static final int PRESENTATION_UNKNOWN = 3;
		public static final int PRESENTATION_PAYPHONE = 4;
		public static final int PRESENTATION_UNAVAILABLE = 5;
		public static final String COUNTRY_ISO = "countryiso";
		public static final String DATE = "date";
		public static final String DURATION = "duration";
		public static final String DATA_USAGE = "data_usage";
		public static final String NEW = "new";
		public static final String CACHED_NAME = "name";
		public static final String PREFERRED_DISPLAY_NAME = "preferred_display_name";
		public static final String CACHED_NUMBER_TYPE = "numbertype";
		public static final String CACHED_NUMBER_LABEL = "numberlabel";
		public static final String VOICEMAIL_URI = "voicemail_uri";
		public static final String TRANSCRIPTION = "transcription";
		public static final String TRANSCRIPTION_STATE = "transcription_state";
		public static final String IS_READ = "is_read";
		public static final String GEOCODED_LOCATION = "geocoded_location";
		public static final String CACHED_LOOKUP_URI = "lookup_uri";
		public static final String CACHED_MATCHED_NUMBER = "matched_number";
		public static final String CACHED_NORMALIZED_NUMBER = "normalized_number";
		public static final String CACHED_PHOTO_ID = "photo_id";
		public static final String CACHED_PHOTO_URI = "photo_uri";
		public static final String CACHED_FORMATTED_NUMBER = "formatted_number";
		public static final String PHONE_ACCOUNT_COMPONENT_NAME = "subscription_component_name";
		public static final String PHONE_ACCOUNT_ID = "subscription_id";
		public static final String PHONE_ACCOUNT_ADDRESS = "phone_account_address";
		public static final String PHONE_ACCOUNT_HIDDEN = "phone_account_hidden";
		public static final String SUB_ID = "sub_id";
		public static final String POST_DIAL_DIGITS = "post_dial_digits";
		public static final String VIA_NUMBER = "via_number";
		public static final String ADD_FOR_ALL_USERS = "add_for_all_users";
		public static final String LAST_MODIFIED = "last_modified";
		public static final int BLOCK_REASON_NOT_BLOCKED = 0;
		public static final int BLOCK_REASON_CALL_SCREENING_SERVICE = 1;
		public static final int BLOCK_REASON_DIRECT_TO_VOICEMAIL = 2;
		public static final int BLOCK_REASON_BLOCKED_NUMBER = 3;
		public static final int BLOCK_REASON_UNKNOWN_NUMBER = 4;
		public static final int BLOCK_REASON_RESTRICTED_NUMBER = 5;
		public static final int BLOCK_REASON_PAY_PHONE = 6;
		public static final int BLOCK_REASON_NOT_IN_CONTACTS = 7;
		public static final String CALL_SCREENING_COMPONENT_NAME = "call_screening_component_name";
		public static final String CALL_SCREENING_APP_NAME = "call_screening_app_name";
		public static final String BLOCK_REASON = "block_reason";
	}
}
