/* ATL: stand-in for the aconfig-generated com.android.internal.telephony.flags.Flags (no aconfig in ATL).
 * Only the flags the ported telephony code reads. Values are those of the release config bp4a of android-16.0.0_r4
 * (BP4A.251205.006, build/release/aconfig/bp4a and the inherited bp3a..ap3a); a flag without a value there is off. */
package com.android.internal.telephony.flags;

public final class Flags {
	public static final String FLAG_REDACT_OTP_SMS_API = "com.android.internal.telephony.flags.redact_otp_sms_api";
	public static final String FLAG_REDACT_OTP_SMS = "com.android.internal.telephony.flags.redact_otp_sms";
	public static final String FLAG_LIMIT_RAW_TABLE_VISIBILITY = "com.android.internal.telephony.flags.limit_raw_table_visibility";
	public static final String FLAG_WORK_PROFILE_API_SPLIT = "com.android.internal.telephony.flags.work_profile_api_split";

	public static final String FLAG_ENABLE_WPS_CHECK_API_FLAG = "com.android.internal.telephony.flags.enable_wps_check_api_flag";

	private Flags() {}

	public static boolean redactOtpSmsApi() { return true; }
	public static boolean redactOtpSms() { return true; }
	public static boolean limitRawTableVisibility() { return true; }
	public static boolean workProfileApiSplit() { return false; }
	// ENABLED in the release config:
	public static boolean nationalCountryCodeFormattingForLocalCalls() { return true; }
	public static boolean removeCountryCodeFromLocalSingaporeCalls() { return true; }
	public static boolean starlinkDataBugfix() { return true; }
}
