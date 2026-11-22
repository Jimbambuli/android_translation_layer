/* ATL: stand-in for the aconfig-generated com.android.internal.telephony.flags.Flags (no aconfig in ATL).
 * Only the flags the ported telephony code reads. Values: flag defaults of Android 16 could not be looked up
 * in the sources (flag value not checked); chosen: the flags that are security/behaviour fixes on, OTP redaction off. */
package com.android.internal.telephony.flags;

public final class Flags {
	public static final String FLAG_REDACT_OTP_SMS_API = "com.android.internal.telephony.flags.redact_otp_sms_api";
	public static final String FLAG_REDACT_OTP_SMS = "com.android.internal.telephony.flags.redact_otp_sms";
	public static final String FLAG_LIMIT_RAW_TABLE_VISIBILITY = "com.android.internal.telephony.flags.limit_raw_table_visibility";
	public static final String FLAG_WORK_PROFILE_API_SPLIT = "com.android.internal.telephony.flags.work_profile_api_split";

	private Flags() {}

	public static boolean redactOtpSmsApi() { return false; }
	public static boolean redactOtpSms() { return false; }
	public static boolean limitRawTableVisibility() { return true; }
	public static boolean workProfileApiSplit() { return true; }
}
