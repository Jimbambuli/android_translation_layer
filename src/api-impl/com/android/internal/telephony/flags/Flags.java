/* ATL: stand-in for the aconfig-generated com.android.internal.telephony.flags.Flags (no aconfig in ATL).
 * Only the flags the ported telephony code reads. Values checked against build/release/aconfig/bp4a of
 * android-16.0.0_r4 (BP4A.251205.006): the last three below are ENABLED there. Not yet matched: redactOtpSms and
 * redactOtpSmsApi are ENABLED in bp4a (here false), workProfileApiSplit has no value in any release config, so
 * AOSP's default (disabled) applies (here true); limitRawTableVisibility is ENABLED (matches). */
package com.android.internal.telephony.flags;

public final class Flags {
	public static final String FLAG_REDACT_OTP_SMS_API = "com.android.internal.telephony.flags.redact_otp_sms_api";
	public static final String FLAG_REDACT_OTP_SMS = "com.android.internal.telephony.flags.redact_otp_sms";
	public static final String FLAG_LIMIT_RAW_TABLE_VISIBILITY = "com.android.internal.telephony.flags.limit_raw_table_visibility";
	public static final String FLAG_WORK_PROFILE_API_SPLIT = "com.android.internal.telephony.flags.work_profile_api_split";

	public static final String FLAG_ENABLE_WPS_CHECK_API_FLAG = "com.android.internal.telephony.flags.enable_wps_check_api_flag";

	private Flags() {}

	public static boolean redactOtpSmsApi() { return false; }
	public static boolean redactOtpSms() { return false; }
	public static boolean limitRawTableVisibility() { return true; }
	public static boolean workProfileApiSplit() { return true; }
	// ENABLED in build/release/aconfig/bp4a (the release config of android-16.0.0_r4, BP4A.251205.006)
	public static boolean nationalCountryCodeFormattingForLocalCalls() { return true; }
	public static boolean removeCountryCodeFromLocalSingaporeCalls() { return true; }
	public static boolean starlinkDataBugfix() { return true; }
}
