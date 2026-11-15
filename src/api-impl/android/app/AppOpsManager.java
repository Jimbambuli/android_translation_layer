package android.app;

public class AppOpsManager {

	/* AOSP's AppOpsManager modes. ATL has no AppOpsService and grants what an app asks for, as it
	 * grants the permissions: every op is in mode MODE_ALLOWED. */
	/* AOSP @hide: the process states that decide an op's mode (App Manager reads the fields) */
	public static final int UID_STATE_PERSISTENT = 100;
	public static final int UID_STATE_TOP = 200;
	public static final int UID_STATE_FOREGROUND_SERVICE_LOCATION = 300;
	public static final int UID_STATE_FOREGROUND_SERVICE = 400;
	public static final int UID_STATE_FOREGROUND = 500;
	public static final int UID_STATE_MAX_LAST_NON_RESTRICTED = UID_STATE_FOREGROUND;
	public static final int UID_STATE_BACKGROUND = 600;
	public static final int UID_STATE_CACHED = 700;
	public static final int MAX_PRIORITY_UID_STATE = UID_STATE_PERSISTENT;
	public static final int MIN_PRIORITY_UID_STATE = UID_STATE_CACHED;

	public static final int MODE_ALLOWED = 0;
	public static final int MODE_IGNORED = 1;
	public static final int MODE_ERRORED = 2;
	public static final int MODE_DEFAULT = 3;
	public static final int MODE_FOREGROUND = 4;


	/* AOSP AppOpsManager.permissionToOp(): the public name of the op that the permission belongs to, else null */
	public static String permissionToOp(String permission) {
		int op = permissionToOpCode(permission);
		return op == OP_NONE ? null : sOpNames[op];
	}

	@Deprecated
	public int noteProxyOpNoThrow(String op, String proxiedPackageName) {
		return noteProxyOpNoThrow(op, proxiedPackageName, android.os.Binder.getCallingUid(), null, null);
	}

	@Deprecated
	public int noteProxyOpNoThrow(String op, String proxiedPackageName, int proxiedUid) {
		return noteProxyOpNoThrow(op, proxiedPackageName, proxiedUid, null, null);
	}

	/* the op is noted and the mode answered: ATL has no AppOpsService to note it with, every op is allowed */
	public int noteProxyOpNoThrow(String op, String proxiedPackageName, int proxiedUid, String proxiedAttributionTag, String message) {
		return MODE_ALLOWED;
	}

	/* AOSP AppOpsManager.noteOp/noteOpNoThrow/noteProxyOp (string forms): the op is noted with the AppOpsService and
	 * its mode answered, noteOp/noteProxyOp throw SecurityException on MODE_ERRORED. Every op is MODE_ALLOWED in ATL,
	 * so they answer that and never throw. */
	@Deprecated
	public int noteOp(String op, int uid, String packageName) {
		return noteOp(op, uid, packageName, null, null);
	}

	public int noteOp(String op, int uid, String packageName, String attributionTag, String message) {
		return MODE_ALLOWED;
	}

	@Deprecated
	public int noteOpNoThrow(String op, int uid, String packageName) {
		return noteOpNoThrow(op, uid, packageName, null, null);
	}

	public int noteOpNoThrow(String op, int uid, String packageName, String attributionTag, String message) {
		return MODE_ALLOWED;
	}

	@Deprecated
	public int noteProxyOp(String op, String proxiedPackageName) {
		return noteProxyOp(op, proxiedPackageName, android.os.Binder.getCallingUid(), null, null);
	}

	public int noteProxyOp(String op, String proxiedPackageName, int proxiedUid, String proxiedAttributionTag, String message) {
		return MODE_ALLOWED;
	}

	@Deprecated
	public int checkOpNoThrow(String op, int uid, String packageName) {
		return unsafeCheckOpNoThrow(op, uid, packageName);
	}

	public int unsafeCheckOpNoThrow(String op, int uid, String packageName) {
		return MODE_ALLOWED;
	}

	@Deprecated
	public int checkOp(String op, int uid, String packageName) {
		return unsafeCheckOp(op, uid, packageName);
	}

	public int unsafeCheckOp(String op, int uid, String packageName) {
		return MODE_ALLOWED;
	}

	/* AOSP AppOpsManager: op codes, names and the static lookups. Op code = row in AOSP's sAppOpInfos
	 * (android-16.0.0_r4, AppOpsManager.java). Behind the hidden constants: MIUI_OP_START/MIUI_OP_END are Xiaomi's
	 * and not AOSP, they stay absent. */
	public static final int OP_COARSE_LOCATION = 0;
	public static final int OP_FINE_LOCATION = 1;
	public static final int OP_GPS = 2;
	public static final int OP_VIBRATE = 3;
	public static final int OP_READ_CONTACTS = 4;
	public static final int OP_WRITE_CONTACTS = 5;
	public static final int OP_READ_CALL_LOG = 6;
	public static final int OP_WRITE_CALL_LOG = 7;
	public static final int OP_READ_CALENDAR = 8;
	public static final int OP_WRITE_CALENDAR = 9;
	public static final int OP_WIFI_SCAN = 10;
	public static final int OP_POST_NOTIFICATION = 11;
	public static final int OP_NEIGHBORING_CELLS = 12;
	public static final int OP_CALL_PHONE = 13;
	public static final int OP_READ_SMS = 14;
	public static final int OP_WRITE_SMS = 15;
	public static final int OP_RECEIVE_SMS = 16;
	public static final int OP_RECEIVE_EMERGECY_SMS = 17;
	public static final int OP_RECEIVE_MMS = 18;
	public static final int OP_RECEIVE_WAP_PUSH = 19;
	public static final int OP_SEND_SMS = 20;
	public static final int OP_READ_ICC_SMS = 21;
	public static final int OP_WRITE_ICC_SMS = 22;
	public static final int OP_WRITE_SETTINGS = 23;
	public static final int OP_SYSTEM_ALERT_WINDOW = 24;
	public static final int OP_ACCESS_NOTIFICATIONS = 25;
	public static final int OP_CAMERA = 26;
	public static final int OP_RECORD_AUDIO = 27;
	public static final int OP_PLAY_AUDIO = 28;
	public static final int OP_READ_CLIPBOARD = 29;
	public static final int OP_WRITE_CLIPBOARD = 30;
	public static final int OP_TAKE_MEDIA_BUTTONS = 31;
	public static final int OP_TAKE_AUDIO_FOCUS = 32;
	public static final int OP_AUDIO_MASTER_VOLUME = 33;
	public static final int OP_AUDIO_VOICE_VOLUME = 34;
	public static final int OP_AUDIO_RING_VOLUME = 35;
	public static final int OP_AUDIO_MEDIA_VOLUME = 36;
	public static final int OP_AUDIO_ALARM_VOLUME = 37;
	public static final int OP_AUDIO_NOTIFICATION_VOLUME = 38;
	public static final int OP_AUDIO_BLUETOOTH_VOLUME = 39;
	public static final int OP_WAKE_LOCK = 40;
	public static final int OP_MONITOR_LOCATION = 41;
	public static final int OP_MONITOR_HIGH_POWER_LOCATION = 42;
	public static final int OP_GET_USAGE_STATS = 43;
	public static final int OP_MUTE_MICROPHONE = 44;
	public static final int OP_TOAST_WINDOW = 45;
	public static final int OP_PROJECT_MEDIA = 46;
	public static final int OP_ACTIVATE_VPN = 47;
	public static final int OP_WRITE_WALLPAPER = 48;
	public static final int OP_ASSIST_STRUCTURE = 49;
	public static final int OP_ASSIST_SCREENSHOT = 50;
	public static final int OP_READ_PHONE_STATE = 51;
	public static final int OP_ADD_VOICEMAIL = 52;
	public static final int OP_USE_SIP = 53;
	public static final int OP_PROCESS_OUTGOING_CALLS = 54;
	public static final int OP_USE_FINGERPRINT = 55;
	public static final int OP_BODY_SENSORS = 56;
	public static final int OP_READ_CELL_BROADCASTS = 57;
	public static final int OP_MOCK_LOCATION = 58;
	public static final int OP_READ_EXTERNAL_STORAGE = 59;
	public static final int OP_WRITE_EXTERNAL_STORAGE = 60;
	public static final int OP_TURN_SCREEN_ON = 61;
	public static final int OP_GET_ACCOUNTS = 62;
	public static final int OP_RUN_IN_BACKGROUND = 63;
	public static final int OP_AUDIO_ACCESSIBILITY_VOLUME = 64;
	public static final int OP_READ_PHONE_NUMBERS = 65;
	public static final int OP_REQUEST_INSTALL_PACKAGES = 66;
	public static final int OP_PICTURE_IN_PICTURE = 67;
	public static final int OP_INSTANT_APP_START_FOREGROUND = 68;
	public static final int OP_ANSWER_PHONE_CALLS = 69;
	public static final int OP_RUN_ANY_IN_BACKGROUND = 70;
	public static final int OP_CHANGE_WIFI_STATE = 71;
	public static final int OP_REQUEST_DELETE_PACKAGES = 72;
	public static final int OP_BIND_ACCESSIBILITY_SERVICE = 73;
	public static final int OP_ACCEPT_HANDOVER = 74;
	public static final int OP_MANAGE_IPSEC_TUNNELS = 75;
	public static final int OP_START_FOREGROUND = 76;
	public static final int OP_BLUETOOTH_SCAN = 77;
	public static final int OP_USE_BIOMETRIC = 78;
	public static final int OP_ACTIVITY_RECOGNITION = 79;
	public static final int OP_SMS_FINANCIAL_TRANSACTIONS = 80;
	public static final int OP_READ_MEDIA_AUDIO = 81;
	public static final int OP_WRITE_MEDIA_AUDIO = 82;
	public static final int OP_READ_MEDIA_VIDEO = 83;
	public static final int OP_WRITE_MEDIA_VIDEO = 84;
	public static final int OP_READ_MEDIA_IMAGES = 85;
	public static final int OP_WRITE_MEDIA_IMAGES = 86;
	public static final int OP_LEGACY_STORAGE = 87;
	public static final int OP_ACCESS_ACCESSIBILITY = 88;
	public static final int OP_READ_DEVICE_IDENTIFIERS = 89;
	public static final int OP_ACCESS_MEDIA_LOCATION = 90;
	public static final int OP_QUERY_ALL_PACKAGES = 91;
	public static final int OP_MANAGE_EXTERNAL_STORAGE = 92;
	public static final int OP_INTERACT_ACROSS_PROFILES = 93;
	public static final int OP_ACTIVATE_PLATFORM_VPN = 94;
	public static final int OP_LOADER_USAGE_STATS = 95;
	public static final int OP_AUTO_REVOKE_PERMISSIONS_IF_UNUSED = 97;
	public static final int OP_AUTO_REVOKE_MANAGED_BY_INSTALLER = 98;
	public static final int OP_NO_ISOLATED_STORAGE = 99;
	public static final int OP_PHONE_CALL_MICROPHONE = 100;
	public static final int OP_PHONE_CALL_CAMERA = 101;
	public static final int OP_RECORD_AUDIO_HOTWORD = 102;
	public static final int OP_MANAGE_ONGOING_CALLS = 103;
	public static final int OP_MANAGE_CREDENTIALS = 104;
	public static final int OP_USE_ICC_AUTH_WITH_DEVICE_IDENTIFIER = 105;
	public static final int OP_RECORD_AUDIO_OUTPUT = 106;
	public static final int OP_SCHEDULE_EXACT_ALARM = 107;
	public static final int OP_FINE_LOCATION_SOURCE = 108;
	public static final int OP_COARSE_LOCATION_SOURCE = 109;
	public static final int OP_MANAGE_MEDIA = 110;
	public static final int OP_BLUETOOTH_CONNECT = 111;
	public static final int OP_UWB_RANGING = 112;
	public static final int OP_ACTIVITY_RECOGNITION_SOURCE = 113;
	public static final int OP_BLUETOOTH_ADVERTISE = 114;
	public static final int OP_RECORD_INCOMING_PHONE_AUDIO = 115;
	public static final int OP_NEARBY_WIFI_DEVICES = 116;
	public static final int OP_ESTABLISH_VPN_SERVICE = 117;
	public static final int OP_ESTABLISH_VPN_MANAGER = 118;
	public static final int OP_ACCESS_RESTRICTED_SETTINGS = 119;
	public static final int OP_RECEIVE_AMBIENT_TRIGGER_AUDIO = 120;
	public static final int OP_RECEIVE_EXPLICIT_USER_INTERACTION_AUDIO = 121;
	public static final int OP_RUN_USER_INITIATED_JOBS = 122;
	public static final int OP_READ_MEDIA_VISUAL_USER_SELECTED = 123;
	public static final int OP_SYSTEM_EXEMPT_FROM_SUSPENSION = 124;
	public static final int OP_SYSTEM_EXEMPT_FROM_DISMISSIBLE_NOTIFICATIONS = 125;
	public static final int OP_READ_WRITE_HEALTH_DATA = 126;
	public static final int OP_FOREGROUND_SERVICE_SPECIAL_USE = 127;
	public static final int OP_SYSTEM_EXEMPT_FROM_POWER_RESTRICTIONS = 128;
	public static final int OP_SYSTEM_EXEMPT_FROM_HIBERNATION = 129;
	public static final int OP_SYSTEM_EXEMPT_FROM_ACTIVITY_BG_START_RESTRICTION = 130;
	public static final int OP_CAPTURE_CONSENTLESS_BUGREPORT_ON_USERDEBUG_BUILD = 131;
	public static final int OP_DEPRECATED_2 = 132;
	public static final int OP_USE_FULL_SCREEN_INTENT = 133;
	public static final int OP_CAMERA_SANDBOXED = 134;
	public static final int OP_RECORD_AUDIO_SANDBOXED = 135;
	public static final int OP_RECEIVE_SANDBOX_TRIGGER_AUDIO = 136;
	public static final int OP_DEPRECATED_3 = 137;
	public static final int OP_CREATE_ACCESSIBILITY_OVERLAY = 138;
	public static final int OP_MEDIA_ROUTING_CONTROL = 139;
	public static final int OP_ENABLE_MOBILE_DATA_BY_USER = 140;
	public static final int OP_RESERVED_FOR_TESTING = 141;
	public static final int OP_RAPID_CLEAR_NOTIFICATIONS_BY_LISTENER = 142;
	public static final int OP_READ_SYSTEM_GRAMMATICAL_GENDER = 143;
	public static final int OP_DEPRECATED_4 = 144;
	public static final int OP_ARCHIVE_ICON_OVERLAY = 145;
	public static final int OP_UNARCHIVAL_CONFIRMATION = 146;
	public static final int OP_EMERGENCY_LOCATION = 147;
	public static final int OP_RECEIVE_SENSITIVE_NOTIFICATIONS = 148;
	public static final int OP_READ_HEART_RATE = 149;
	public static final int OP_READ_SKIN_TEMPERATURE = 150;
	public static final int OP_RANGING = 151;
	public static final int OP_READ_OXYGEN_SATURATION = 152;
	public static final int OP_WRITE_SYSTEM_PREFERENCES = 153;
	public static final int OP_CONTROL_AUDIO = 154;
	public static final int OP_CONTROL_AUDIO_PARTIAL = 155;
	public static final int OP_EYE_TRACKING_COARSE = 156;
	public static final int OP_EYE_TRACKING_FINE = 157;
	public static final int OP_FACE_TRACKING = 158;
	public static final int OP_HAND_TRACKING = 159;
	public static final int OP_HEAD_TRACKING = 160;
	public static final int OP_SCENE_UNDERSTANDING_COARSE = 161;
	public static final int OP_SCENE_UNDERSTANDING_FINE = 162;
	public static final int OP_POST_PROMOTED_NOTIFICATIONS = 163;
	public static final int OP_SYSTEM_APPLICATION_OVERLAY = 164;
	public static final int OP_READ_CELL_IDENTITY = 165;
	public static final int OP_READ_CELL_INFO = 166;
	public static final int OP_COMPUTER_CONTROL = 167;
	public static final int OP_READ_OTP_SMS = 168;
	public static final int OP_ACCESS_LOCAL_NETWORK = 169;
	public static final int OP_NONE = -1;
	public static final int _NUM_OP = 170;
	public static final String OPSTR_COARSE_LOCATION = "android:coarse_location";
	public static final String OPSTR_FINE_LOCATION = "android:fine_location";
	public static final String OPSTR_MONITOR_LOCATION = "android:monitor_location";
	public static final String OPSTR_MONITOR_HIGH_POWER_LOCATION = "android:monitor_location_high_power";
	public static final String OPSTR_GET_USAGE_STATS = "android:get_usage_stats";
	public static final String OPSTR_ACTIVATE_VPN = "android:activate_vpn";
	public static final String OPSTR_READ_CONTACTS = "android:read_contacts";
	public static final String OPSTR_WRITE_CONTACTS = "android:write_contacts";
	public static final String OPSTR_READ_CALL_LOG = "android:read_call_log";
	public static final String OPSTR_WRITE_CALL_LOG = "android:write_call_log";
	public static final String OPSTR_READ_CALENDAR = "android:read_calendar";
	public static final String OPSTR_WRITE_CALENDAR = "android:write_calendar";
	public static final String OPSTR_CALL_PHONE = "android:call_phone";
	public static final String OPSTR_READ_SMS = "android:read_sms";
	public static final String OPSTR_RECEIVE_SMS = "android:receive_sms";
	public static final String OPSTR_RECEIVE_MMS = "android:receive_mms";
	public static final String OPSTR_RECEIVE_WAP_PUSH = "android:receive_wap_push";
	public static final String OPSTR_SEND_SMS = "android:send_sms";
	public static final String OPSTR_CAMERA = "android:camera";
	public static final String OPSTR_RECORD_AUDIO = "android:record_audio";
	public static final String OPSTR_READ_PHONE_STATE = "android:read_phone_state";
	public static final String OPSTR_ADD_VOICEMAIL = "android:add_voicemail";
	public static final String OPSTR_USE_SIP = "android:use_sip";
	public static final String OPSTR_PROCESS_OUTGOING_CALLS = "android:process_outgoing_calls";
	public static final String OPSTR_USE_FINGERPRINT = "android:use_fingerprint";
	public static final String OPSTR_BODY_SENSORS = "android:body_sensors";
	public static final String OPSTR_READ_CELL_BROADCASTS = "android:read_cell_broadcasts";
	public static final String OPSTR_MOCK_LOCATION = "android:mock_location";
	public static final String OPSTR_READ_EXTERNAL_STORAGE = "android:read_external_storage";
	public static final String OPSTR_WRITE_EXTERNAL_STORAGE = "android:write_external_storage";
	public static final String OPSTR_SYSTEM_ALERT_WINDOW = "android:system_alert_window";
	public static final String OPSTR_WRITE_SETTINGS = "android:write_settings";
	public static final String OPSTR_GET_ACCOUNTS = "android:get_accounts";
	public static final String OPSTR_READ_PHONE_NUMBERS = "android:read_phone_numbers";
	public static final String OPSTR_PICTURE_IN_PICTURE = "android:picture_in_picture";
	public static final String OPSTR_INSTANT_APP_START_FOREGROUND = "android:instant_app_start_foreground";
	public static final String OPSTR_ANSWER_PHONE_CALLS = "android:answer_phone_calls";
	public static final String OPSTR_ACCEPT_HANDOVER = "android:accept_handover";
	public static final String OPSTR_GPS = "android:gps";
	public static final String OPSTR_VIBRATE = "android:vibrate";
	public static final String OPSTR_WIFI_SCAN = "android:wifi_scan";
	public static final String OPSTR_POST_NOTIFICATION = "android:post_notification";
	public static final String OPSTR_NEIGHBORING_CELLS = "android:neighboring_cells";
	public static final String OPSTR_WRITE_SMS = "android:write_sms";
	public static final String OPSTR_RECEIVE_EMERGENCY_BROADCAST = "android:receive_emergency_broadcast";
	public static final String OPSTR_READ_ICC_SMS = "android:read_icc_sms";
	public static final String OPSTR_WRITE_ICC_SMS = "android:write_icc_sms";
	public static final String OPSTR_ACCESS_NOTIFICATIONS = "android:access_notifications";
	public static final String OPSTR_PLAY_AUDIO = "android:play_audio";
	public static final String OPSTR_READ_CLIPBOARD = "android:read_clipboard";
	public static final String OPSTR_WRITE_CLIPBOARD = "android:write_clipboard";
	public static final String OPSTR_TAKE_MEDIA_BUTTONS = "android:take_media_buttons";
	public static final String OPSTR_TAKE_AUDIO_FOCUS = "android:take_audio_focus";
	public static final String OPSTR_AUDIO_MASTER_VOLUME = "android:audio_master_volume";
	public static final String OPSTR_AUDIO_VOICE_VOLUME = "android:audio_voice_volume";
	public static final String OPSTR_AUDIO_RING_VOLUME = "android:audio_ring_volume";
	public static final String OPSTR_AUDIO_MEDIA_VOLUME = "android:audio_media_volume";
	public static final String OPSTR_AUDIO_ALARM_VOLUME = "android:audio_alarm_volume";
	public static final String OPSTR_AUDIO_NOTIFICATION_VOLUME = "android:audio_notification_volume";
	public static final String OPSTR_AUDIO_BLUETOOTH_VOLUME = "android:audio_bluetooth_volume";
	public static final String OPSTR_WAKE_LOCK = "android:wake_lock";
	public static final String OPSTR_MUTE_MICROPHONE = "android:mute_microphone";
	public static final String OPSTR_TOAST_WINDOW = "android:toast_window";
	public static final String OPSTR_PROJECT_MEDIA = "android:project_media";
	public static final String OPSTR_WRITE_WALLPAPER = "android:write_wallpaper";
	public static final String OPSTR_ASSIST_STRUCTURE = "android:assist_structure";
	public static final String OPSTR_ASSIST_SCREENSHOT = "android:assist_screenshot";
	public static final String OPSTR_TURN_SCREEN_ON = "android:turn_screen_on";
	public static final String OPSTR_RUN_IN_BACKGROUND = "android:run_in_background";
	public static final String OPSTR_AUDIO_ACCESSIBILITY_VOLUME = "android:audio_accessibility_volume";
	public static final String OPSTR_REQUEST_INSTALL_PACKAGES = "android:request_install_packages";
	public static final String OPSTR_RUN_ANY_IN_BACKGROUND = "android:run_any_in_background";
	public static final String OPSTR_CHANGE_WIFI_STATE = "android:change_wifi_state";
	public static final String OPSTR_REQUEST_DELETE_PACKAGES = "android:request_delete_packages";
	public static final String OPSTR_BIND_ACCESSIBILITY_SERVICE = "android:bind_accessibility_service";
	public static final String OPSTR_MANAGE_IPSEC_TUNNELS = "android:manage_ipsec_tunnels";
	public static final String OPSTR_START_FOREGROUND = "android:start_foreground";
	public static final String OPSTR_BLUETOOTH_SCAN = "android:bluetooth_scan";
	public static final String OPSTR_BLUETOOTH_CONNECT = "android:bluetooth_connect";
	public static final String OPSTR_BLUETOOTH_ADVERTISE = "android:bluetooth_advertise";
	public static final String OPSTR_USE_BIOMETRIC = "android:use_biometric";
	public static final String OPSTR_ACTIVITY_RECOGNITION = "android:activity_recognition";
	public static final String OPSTR_SMS_FINANCIAL_TRANSACTIONS = "android:sms_financial_transactions";
	public static final String OPSTR_READ_MEDIA_AUDIO = "android:read_media_audio";
	public static final String OPSTR_WRITE_MEDIA_AUDIO = "android:write_media_audio";
	public static final String OPSTR_READ_MEDIA_VIDEO = "android:read_media_video";
	public static final String OPSTR_WRITE_MEDIA_VIDEO = "android:write_media_video";
	public static final String OPSTR_READ_MEDIA_IMAGES = "android:read_media_images";
	public static final String OPSTR_WRITE_MEDIA_IMAGES = "android:write_media_images";
	public static final String OPSTR_LEGACY_STORAGE = "android:legacy_storage";
	public static final String OPSTR_ACCESS_MEDIA_LOCATION = "android:access_media_location";
	public static final String OPSTR_ACCESS_ACCESSIBILITY = "android:access_accessibility";
	public static final String OPSTR_READ_DEVICE_IDENTIFIERS = "android:read_device_identifiers";
	public static final String OPSTR_QUERY_ALL_PACKAGES = "android:query_all_packages";
	public static final String OPSTR_MANAGE_EXTERNAL_STORAGE = "android:manage_external_storage";
	public static final String OPSTR_AUTO_REVOKE_PERMISSIONS_IF_UNUSED = "android:auto_revoke_permissions_if_unused";
	public static final String OPSTR_AUTO_REVOKE_MANAGED_BY_INSTALLER = "android:auto_revoke_managed_by_installer";
	public static final String OPSTR_INTERACT_ACROSS_PROFILES = "android:interact_across_profiles";
	public static final String OPSTR_ACTIVATE_PLATFORM_VPN = "android:activate_platform_vpn";
	public static final String OPSTR_LOADER_USAGE_STATS = "android:loader_usage_stats";
	public static final String OPSTR_MANAGE_ONGOING_CALLS = "android:manage_ongoing_calls";
	public static final String OPSTR_MEDIA_ROUTING_CONTROL = "android:media_routing_control";
	public static final String OPSTR_ARCHIVE_ICON_OVERLAY = "android:archive_icon_overlay";
	public static final String OPSTR_UNARCHIVAL_CONFIRMATION = "android:unarchival_support";
	public static final String OPSTR_NO_ISOLATED_STORAGE = "android:no_isolated_storage";
	public static final String OPSTR_PHONE_CALL_MICROPHONE = "android:phone_call_microphone";
	public static final String OPSTR_PHONE_CALL_CAMERA = "android:phone_call_camera";
	public static final String OPSTR_RECORD_AUDIO_HOTWORD = "android:record_audio_hotword";
	public static final String OPSTR_MANAGE_CREDENTIALS = "android:manage_credentials";
	public static final String OPSTR_USE_ICC_AUTH_WITH_DEVICE_IDENTIFIER = "android:use_icc_auth_with_device_identifier";
	public static final String OPSTR_RECORD_AUDIO_OUTPUT = "android:record_audio_output";
	public static final String OPSTR_SCHEDULE_EXACT_ALARM = "android:schedule_exact_alarm";
	public static final String OPSTR_FINE_LOCATION_SOURCE = "android:fine_location_source";
	public static final String OPSTR_COARSE_LOCATION_SOURCE = "android:coarse_location_source";
	public static final String OPSTR_CAMERA_SANDBOXED = "android:camera_sandboxed";
	public static final String OPSTR_RECORD_AUDIO_SANDBOXED = "android:record_audio_sandboxed";
	public static final String OPSTR_MANAGE_MEDIA = "android:manage_media";
	public static final String OPSTR_UWB_RANGING = "android:uwb_ranging";
	public static final String OPSTR_NEARBY_WIFI_DEVICES = "android:nearby_wifi_devices";
	public static final String OPSTR_ACTIVITY_RECOGNITION_SOURCE = "android:activity_recognition_source";
	public static final String OPSTR_RECORD_INCOMING_PHONE_AUDIO = "android:record_incoming_phone_audio";
	public static final String OPSTR_ESTABLISH_VPN_SERVICE = "android:establish_vpn_service";
	public static final String OPSTR_ESTABLISH_VPN_MANAGER = "android:establish_vpn_manager";
	public static final String OPSTR_ACCESS_RESTRICTED_SETTINGS = "android:access_restricted_settings";
	public static final String OPSTR_RECEIVE_AMBIENT_TRIGGER_AUDIO = "android:receive_ambient_trigger_audio";
	public static final String OPSTR_READ_MEDIA_VISUAL_USER_SELECTED = "android:read_media_visual_user_selected";
	public static final String OPSTR_READ_WRITE_HEALTH_DATA = "android:read_write_health_data";
	public static final String OPSTR_RECEIVE_EXPLICIT_USER_INTERACTION_AUDIO = "android:receive_explicit_user_interaction_audio";
	public static final String OPSTR_RUN_USER_INITIATED_JOBS = "android:run_user_initiated_jobs";
	public static final String OPSTR_SYSTEM_EXEMPT_FROM_SUSPENSION = "android:system_exempt_from_suspension";
	public static final String OPSTR_SYSTEM_EXEMPT_FROM_DISMISSIBLE_NOTIFICATIONS = "android:system_exempt_from_dismissible_notifications";
	public static final String OPSTR_FOREGROUND_SERVICE_SPECIAL_USE = "android:foreground_service_special_use";
	public static final String OPSTR_SYSTEM_EXEMPT_FROM_POWER_RESTRICTIONS = "android:system_exempt_from_power_restrictions";
	public static final String OPSTR_SYSTEM_EXEMPT_FROM_HIBERNATION = "android:system_exempt_from_hibernation";
	public static final String OPSTR_SYSTEM_EXEMPT_FROM_ACTIVITY_BG_START_RESTRICTION = "android:system_exempt_from_activity_bg_start_restriction";
	public static final String OPSTR_CAPTURE_CONSENTLESS_BUGREPORT_ON_USERDEBUG_BUILD = "android:capture_consentless_bugreport_on_userdebug_build";
	public static final String OPSTR_DEPRECATED_2 = "android:deprecated_2";
	public static final String OPSTR_USE_FULL_SCREEN_INTENT = "android:use_full_screen_intent";
	public static final String OPSTR_RECEIVE_SANDBOX_TRIGGER_AUDIO = "android:receive_sandbox_trigger_audio";
	public static final String OPSTR_DEPRECATED_3 = "android:deprecated_3";
	public static final String OPSTR_CREATE_ACCESSIBILITY_OVERLAY = "android:create_accessibility_overlay";
	public static final String OPSTR_ENABLE_MOBILE_DATA_BY_USER = "android:enable_mobile_data_by_user";
	public static final String OPSTR_RESERVED_FOR_TESTING = "android:reserved_for_testing";
	public static final String OPSTR_RAPID_CLEAR_NOTIFICATIONS_BY_LISTENER = "android:rapid_clear_notifications_by_listener";
	public static final String OPSTR_READ_SYSTEM_GRAMMATICAL_GENDER = "android:read_system_grammatical_gender";
	public static final String OPSTR_DEPRECATED_4 = "android:deprecated_4";
	public static final String OPSTR_EMERGENCY_LOCATION = "android:emergency_location";
	public static final String OPSTR_RECEIVE_SENSITIVE_NOTIFICATIONS = "android:receive_sensitive_notifications";
	public static final String OPSTR_READ_HEART_RATE = "android:read_heart_rate";
	public static final String OPSTR_READ_OXYGEN_SATURATION = "android:read_oxygen_saturation";
	public static final String OPSTR_READ_SKIN_TEMPERATURE = "android:read_skin_temperature";
	public static final String OPSTR_RANGING = "android:ranging";
	public static final String OPSTR_WRITE_SYSTEM_PREFERENCES = "android:write_system_preferences";
	public static final String OPSTR_CONTROL_AUDIO = "android:control_audio";
	public static final String OPSTR_CONTROL_AUDIO_PARTIAL = "android:control_audio_partial";
	public static final String OPSTR_EYE_TRACKING_COARSE = "android:eye_tracking_coarse";
	public static final String OPSTR_EYE_TRACKING_FINE = "android:eye_tracking_fine";
	public static final String OPSTR_FACE_TRACKING = "android:face_tracking";
	public static final String OPSTR_HAND_TRACKING = "android:hand_tracking";
	public static final String OPSTR_HEAD_TRACKING = "android:head_tracking";
	public static final String OPSTR_SCENE_UNDERSTANDING_COARSE = "android:scene_understanding_coarse";
	public static final String OPSTR_SCENE_UNDERSTANDING_FINE = "android:scene_understanding_fine";
	public static final String OPSTR_POST_PROMOTED_NOTIFICATIONS = "android:post_promoted_notifications";
	public static final String OPSTR_SYSTEM_APPLICATION_OVERLAY = "android:system_application_overlay";
	public static final String OPSTR_READ_CELL_IDENTITY = "android:read_cell_identity";
	public static final String OPSTR_READ_CELL_INFO = "android:read_cell_info";
	public static final String OPSTR_COMPUTER_CONTROL = "android:computer_control";
	public static final String OPSTR_READ_OTP_SMS = "android:read_otp_sms";
	public static final String OPSTR_ACCESS_LOCAL_NETWORK = "android:access_local_network";

	/* AOSP AppOpsManager.sAppOpInfos, one row per op code: public name, debug name, permission, switch op, default mode.
	 * Permissions that AOSP hides behind a feature flag (ranging, xr, write_system_preferences) are null here. */
	private static final String[] sOpNames = {
		"android:coarse_location",
		"android:fine_location",
		"android:gps",
		"android:vibrate",
		"android:read_contacts",
		"android:write_contacts",
		"android:read_call_log",
		"android:write_call_log",
		"android:read_calendar",
		"android:write_calendar",
		"android:wifi_scan",
		"android:post_notification",
		"android:neighboring_cells",
		"android:call_phone",
		"android:read_sms",
		"android:write_sms",
		"android:receive_sms",
		"android:receive_emergency_broadcast",
		"android:receive_mms",
		"android:receive_wap_push",
		"android:send_sms",
		"android:read_icc_sms",
		"android:write_icc_sms",
		"android:write_settings",
		"android:system_alert_window",
		"android:access_notifications",
		"android:camera",
		"android:record_audio",
		"android:play_audio",
		"android:read_clipboard",
		"android:write_clipboard",
		"android:take_media_buttons",
		"android:take_audio_focus",
		"android:audio_master_volume",
		"android:audio_voice_volume",
		"android:audio_ring_volume",
		"android:audio_media_volume",
		"android:audio_alarm_volume",
		"android:audio_notification_volume",
		"android:audio_bluetooth_volume",
		"android:wake_lock",
		"android:monitor_location",
		"android:monitor_location_high_power",
		"android:get_usage_stats",
		"android:mute_microphone",
		"android:toast_window",
		"android:project_media",
		"android:activate_vpn",
		"android:write_wallpaper",
		"android:assist_structure",
		"android:assist_screenshot",
		"android:read_phone_state",
		"android:add_voicemail",
		"android:use_sip",
		"android:process_outgoing_calls",
		"android:use_fingerprint",
		"android:body_sensors",
		"android:read_cell_broadcasts",
		"android:mock_location",
		"android:read_external_storage",
		"android:write_external_storage",
		"android:turn_screen_on",
		"android:get_accounts",
		"android:run_in_background",
		"android:audio_accessibility_volume",
		"android:read_phone_numbers",
		"android:request_install_packages",
		"android:picture_in_picture",
		"android:instant_app_start_foreground",
		"android:answer_phone_calls",
		"android:run_any_in_background",
		"android:change_wifi_state",
		"android:request_delete_packages",
		"android:bind_accessibility_service",
		"android:accept_handover",
		"android:manage_ipsec_tunnels",
		"android:start_foreground",
		"android:bluetooth_scan",
		"android:use_biometric",
		"android:activity_recognition",
		"android:sms_financial_transactions",
		"android:read_media_audio",
		"android:write_media_audio",
		"android:read_media_video",
		"android:write_media_video",
		"android:read_media_images",
		"android:write_media_images",
		"android:legacy_storage",
		"android:access_accessibility",
		"android:read_device_identifiers",
		"android:access_media_location",
		"android:query_all_packages",
		"android:manage_external_storage",
		"android:interact_across_profiles",
		"android:activate_platform_vpn",
		"android:loader_usage_stats",
		"",
		"android:auto_revoke_permissions_if_unused",
		"android:auto_revoke_managed_by_installer",
		"android:no_isolated_storage",
		"android:phone_call_microphone",
		"android:phone_call_camera",
		"android:record_audio_hotword",
		"android:manage_ongoing_calls",
		"android:manage_credentials",
		"android:use_icc_auth_with_device_identifier",
		"android:record_audio_output",
		"android:schedule_exact_alarm",
		"android:fine_location_source",
		"android:coarse_location_source",
		"android:manage_media",
		"android:bluetooth_connect",
		"android:uwb_ranging",
		"android:activity_recognition_source",
		"android:bluetooth_advertise",
		"android:record_incoming_phone_audio",
		"android:nearby_wifi_devices",
		"android:establish_vpn_service",
		"android:establish_vpn_manager",
		"android:access_restricted_settings",
		"android:receive_ambient_trigger_audio",
		"android:receive_explicit_user_interaction_audio",
		"android:run_user_initiated_jobs",
		"android:read_media_visual_user_selected",
		"android:system_exempt_from_suspension",
		"android:system_exempt_from_dismissible_notifications",
		"android:read_write_health_data",
		"android:foreground_service_special_use",
		"android:system_exempt_from_power_restrictions",
		"android:system_exempt_from_hibernation",
		"android:system_exempt_from_activity_bg_start_restriction",
		"android:capture_consentless_bugreport_on_userdebug_build",
		"android:deprecated_2",
		"android:use_full_screen_intent",
		"android:camera_sandboxed",
		"android:record_audio_sandboxed",
		"android:receive_sandbox_trigger_audio",
		"android:deprecated_3",
		"android:create_accessibility_overlay",
		"android:media_routing_control",
		"android:enable_mobile_data_by_user",
		"android:reserved_for_testing",
		"android:rapid_clear_notifications_by_listener",
		"android:read_system_grammatical_gender",
		"android:deprecated_4",
		"android:archive_icon_overlay",
		"android:unarchival_support",
		"android:emergency_location",
		"android:receive_sensitive_notifications",
		"android:read_heart_rate",
		"android:read_skin_temperature",
		"android:ranging",
		"android:read_oxygen_saturation",
		"android:write_system_preferences",
		"android:control_audio",
		"android:control_audio_partial",
		"android:eye_tracking_coarse",
		"android:eye_tracking_fine",
		"android:face_tracking",
		"android:hand_tracking",
		"android:head_tracking",
		"android:scene_understanding_coarse",
		"android:scene_understanding_fine",
		"android:post_promoted_notifications",
		"android:system_application_overlay",
		"android:read_cell_identity",
		"android:read_cell_info",
		"android:computer_control",
		"android:read_otp_sms",
		"android:access_local_network"};
	private static final String[] sOpSimpleNames = {
		"COARSE_LOCATION",
		"FINE_LOCATION",
		"GPS",
		"VIBRATE",
		"READ_CONTACTS",
		"WRITE_CONTACTS",
		"READ_CALL_LOG",
		"WRITE_CALL_LOG",
		"READ_CALENDAR",
		"WRITE_CALENDAR",
		"WIFI_SCAN",
		"POST_NOTIFICATION",
		"NEIGHBORING_CELLS",
		"CALL_PHONE",
		"READ_SMS",
		"WRITE_SMS",
		"RECEIVE_SMS",
		"RECEIVE_EMERGENCY_BROADCAST",
		"RECEIVE_MMS",
		"RECEIVE_WAP_PUSH",
		"SEND_SMS",
		"READ_ICC_SMS",
		"WRITE_ICC_SMS",
		"WRITE_SETTINGS",
		"SYSTEM_ALERT_WINDOW",
		"ACCESS_NOTIFICATIONS",
		"CAMERA",
		"RECORD_AUDIO",
		"PLAY_AUDIO",
		"READ_CLIPBOARD",
		"WRITE_CLIPBOARD",
		"TAKE_MEDIA_BUTTONS",
		"TAKE_AUDIO_FOCUS",
		"AUDIO_MASTER_VOLUME",
		"AUDIO_VOICE_VOLUME",
		"AUDIO_RING_VOLUME",
		"AUDIO_MEDIA_VOLUME",
		"AUDIO_ALARM_VOLUME",
		"AUDIO_NOTIFICATION_VOLUME",
		"AUDIO_BLUETOOTH_VOLUME",
		"WAKE_LOCK",
		"MONITOR_LOCATION",
		"MONITOR_HIGH_POWER_LOCATION",
		"GET_USAGE_STATS",
		"MUTE_MICROPHONE",
		"TOAST_WINDOW",
		"PROJECT_MEDIA",
		"ACTIVATE_VPN",
		"WRITE_WALLPAPER",
		"ASSIST_STRUCTURE",
		"ASSIST_SCREENSHOT",
		"READ_PHONE_STATE",
		"ADD_VOICEMAIL",
		"USE_SIP",
		"PROCESS_OUTGOING_CALLS",
		"USE_FINGERPRINT",
		"BODY_SENSORS",
		"READ_CELL_BROADCASTS",
		"MOCK_LOCATION",
		"READ_EXTERNAL_STORAGE",
		"WRITE_EXTERNAL_STORAGE",
		"TURN_SCREEN_ON",
		"GET_ACCOUNTS",
		"RUN_IN_BACKGROUND",
		"AUDIO_ACCESSIBILITY_VOLUME",
		"READ_PHONE_NUMBERS",
		"REQUEST_INSTALL_PACKAGES",
		"PICTURE_IN_PICTURE",
		"INSTANT_APP_START_FOREGROUND",
		"ANSWER_PHONE_CALLS",
		"RUN_ANY_IN_BACKGROUND",
		"CHANGE_WIFI_STATE",
		"REQUEST_DELETE_PACKAGES",
		"BIND_ACCESSIBILITY_SERVICE",
		"ACCEPT_HANDOVER",
		"MANAGE_IPSEC_TUNNELS",
		"START_FOREGROUND",
		"BLUETOOTH_SCAN",
		"USE_BIOMETRIC",
		"ACTIVITY_RECOGNITION",
		"SMS_FINANCIAL_TRANSACTIONS",
		"READ_MEDIA_AUDIO",
		"WRITE_MEDIA_AUDIO",
		"READ_MEDIA_VIDEO",
		"WRITE_MEDIA_VIDEO",
		"READ_MEDIA_IMAGES",
		"WRITE_MEDIA_IMAGES",
		"LEGACY_STORAGE",
		"ACCESS_ACCESSIBILITY",
		"READ_DEVICE_IDENTIFIERS",
		"ACCESS_MEDIA_LOCATION",
		"QUERY_ALL_PACKAGES",
		"MANAGE_EXTERNAL_STORAGE",
		"INTERACT_ACROSS_PROFILES",
		"ACTIVATE_PLATFORM_VPN",
		"LOADER_USAGE_STATS",
		"",
		"AUTO_REVOKE_PERMISSIONS_IF_UNUSED",
		"AUTO_REVOKE_MANAGED_BY_INSTALLER",
		"NO_ISOLATED_STORAGE",
		"PHONE_CALL_MICROPHONE",
		"PHONE_CALL_CAMERA",
		"RECORD_AUDIO_HOTWORD",
		"MANAGE_ONGOING_CALLS",
		"MANAGE_CREDENTIALS",
		"USE_ICC_AUTH_WITH_DEVICE_IDENTIFIER",
		"RECORD_AUDIO_OUTPUT",
		"SCHEDULE_EXACT_ALARM",
		"FINE_LOCATION_SOURCE",
		"COARSE_LOCATION_SOURCE",
		"MANAGE_MEDIA",
		"BLUETOOTH_CONNECT",
		"UWB_RANGING",
		"ACTIVITY_RECOGNITION_SOURCE",
		"BLUETOOTH_ADVERTISE",
		"RECORD_INCOMING_PHONE_AUDIO",
		"NEARBY_WIFI_DEVICES",
		"ESTABLISH_VPN_SERVICE",
		"ESTABLISH_VPN_MANAGER",
		"ACCESS_RESTRICTED_SETTINGS",
		"RECEIVE_SOUNDTRIGGER_AUDIO",
		"RECEIVE_EXPLICIT_USER_INTERACTION_AUDIO",
		"RUN_USER_INITIATED_JOBS",
		"READ_MEDIA_VISUAL_USER_SELECTED",
		"SYSTEM_EXEMPT_FROM_SUSPENSION",
		"SYSTEM_EXEMPT_FROM_DISMISSIBLE_NOTIFICATIONS",
		"READ_WRITE_HEALTH_DATA",
		"FOREGROUND_SERVICE_SPECIAL_USE",
		"SYSTEM_EXEMPT_FROM_POWER_RESTRICTIONS",
		"SYSTEM_EXEMPT_FROM_HIBERNATION",
		"SYSTEM_EXEMPT_FROM_ACTIVITY_BG_START_RESTRICTION",
		"CAPTURE_CONSENTLESS_BUGREPORT_ON_USERDEBUG_BUILD",
		"DEPRECATED_2",
		"USE_FULL_SCREEN_INTENT",
		"CAMERA_SANDBOXED",
		"RECORD_AUDIO_SANDBOXED",
		"RECEIVE_SANDBOX_TRIGGER_AUDIO",
		"DEPRECATED_3",
		"CREATE_ACCESSIBILITY_OVERLAY",
		"MEDIA_ROUTING_CONTROL",
		"ENABLE_MOBILE_DATA_BY_USER",
		"OP_RESERVED_FOR_TESTING",
		"RAPID_CLEAR_NOTIFICATIONS_BY_LISTENER",
		"READ_SYSTEM_GRAMMATICAL_GENDER",
		"DEPRECATED_4",
		"ARCHIVE_ICON_OVERLAY",
		"UNARCHIVAL_CONFIRMATION",
		"EMERGENCY_LOCATION",
		"RECEIVE_SENSITIVE_NOTIFICATIONS",
		"READ_HEART_RATE",
		"READ_SKIN_TEMPERATURE",
		"RANGING",
		"READ_OXYGEN_SATURATION",
		"WRITE_SYSTEM_PREFERENCES",
		"CONTROL_AUDIO",
		"CONTROL_AUDIO_PARTIAL",
		"EYE_TRACKING_COARSE",
		"EYE_TRACKING_FINE",
		"FACE_TRACKING",
		"HAND_TRACKING",
		"HEAD_TRACKING",
		"SCENE_UNDERSTANDING_COARSE",
		"SCENE_UNDERSTANDING_FINE",
		"POST_PROMOTED_NOTIFICATIONS",
		"SYSTEM_APPLICATION_OVERLAY",
		"READ_CELL_IDENTITY",
		"READ_CELL_INFO",
		"COMPUTER_CONTROL",
		"READ_OTP_SMS",
		"ACCESS_LOCAL_NETWORK"};
	private static final String[] sOpPerms = {
		"android.permission.ACCESS_COARSE_LOCATION",
		"android.permission.ACCESS_FINE_LOCATION",
		null,
		"android.permission.VIBRATE",
		"android.permission.READ_CONTACTS",
		"android.permission.WRITE_CONTACTS",
		"android.permission.READ_CALL_LOG",
		"android.permission.WRITE_CALL_LOG",
		"android.permission.READ_CALENDAR",
		"android.permission.WRITE_CALENDAR",
		"android.permission.ACCESS_WIFI_STATE",
		"android.permission.POST_NOTIFICATIONS",
		null,
		"android.permission.CALL_PHONE",
		"android.permission.READ_SMS",
		null,
		"android.permission.RECEIVE_SMS",
		"android.permission.RECEIVE_EMERGENCY_BROADCAST",
		"android.permission.RECEIVE_MMS",
		"android.permission.RECEIVE_WAP_PUSH",
		"android.permission.SEND_SMS",
		"android.permission.READ_SMS",
		null,
		"android.permission.WRITE_SETTINGS",
		"android.permission.SYSTEM_ALERT_WINDOW",
		"android.permission.ACCESS_NOTIFICATIONS",
		"android.permission.CAMERA",
		"android.permission.RECORD_AUDIO",
		null,
		null,
		null,
		null,
		null,
		null,
		null,
		null,
		null,
		null,
		null,
		null,
		"android.permission.WAKE_LOCK",
		null,
		null,
		"android.permission.PACKAGE_USAGE_STATS",
		null,
		null,
		null,
		null,
		null,
		null,
		null,
		"android.permission.READ_PHONE_STATE",
		"com.android.voicemail.permission.ADD_VOICEMAIL",
		"android.permission.USE_SIP",
		"android.permission.PROCESS_OUTGOING_CALLS",
		"android.permission.USE_FINGERPRINT",
		"android.permission.BODY_SENSORS",
		"android.permission.READ_CELL_BROADCASTS",
		null,
		"android.permission.READ_EXTERNAL_STORAGE",
		"android.permission.WRITE_EXTERNAL_STORAGE",
		"android.permission.TURN_SCREEN_ON",
		"android.permission.GET_ACCOUNTS",
		null,
		null,
		"android.permission.READ_PHONE_NUMBERS",
		"android.permission.REQUEST_INSTALL_PACKAGES",
		null,
		"android.permission.INSTANT_APP_FOREGROUND_SERVICE",
		"android.permission.ANSWER_PHONE_CALLS",
		null,
		"android.permission.CHANGE_WIFI_STATE",
		"android.permission.REQUEST_DELETE_PACKAGES",
		"android.permission.BIND_ACCESSIBILITY_SERVICE",
		"android.permission.ACCEPT_HANDOVER",
		"android.permission.MANAGE_IPSEC_TUNNELS",
		"android.permission.FOREGROUND_SERVICE",
		"android.permission.BLUETOOTH_SCAN",
		"android.permission.USE_BIOMETRIC",
		"android.permission.ACTIVITY_RECOGNITION",
		"android.permission.SMS_FINANCIAL_TRANSACTIONS",
		"android.permission.READ_MEDIA_AUDIO",
		null,
		"android.permission.READ_MEDIA_VIDEO",
		null,
		"android.permission.READ_MEDIA_IMAGES",
		null,
		null,
		null,
		null,
		"android.permission.ACCESS_MEDIA_LOCATION",
		null,
		"android.permission.MANAGE_EXTERNAL_STORAGE",
		"android.permission.INTERACT_ACROSS_PROFILES",
		null,
		"android.permission.LOADER_USAGE_STATS",
		null,
		null,
		null,
		null,
		null,
		null,
		null,
		"android.permission.MANAGE_ONGOING_CALLS",
		null,
		"android.permission.USE_ICC_AUTH_WITH_DEVICE_IDENTIFIER",
		null,
		"android.permission.SCHEDULE_EXACT_ALARM",
		null,
		null,
		"android.permission.MANAGE_MEDIA",
		"android.permission.BLUETOOTH_CONNECT",
		"android.permission.UWB_RANGING",
		null,
		"android.permission.BLUETOOTH_ADVERTISE",
		null,
		"android.permission.NEARBY_WIFI_DEVICES",
		null,
		null,
		null,
		null,
		null,
		null,
		"android.permission.READ_MEDIA_VISUAL_USER_SELECTED",
		null,
		null,
		null,
		"android.permission.FOREGROUND_SERVICE_SPECIAL_USE",
		null,
		null,
		null,
		"android.permission.CAPTURE_CONSENTLESS_BUGREPORT_ON_USERDEBUG_BUILD",
		null,
		"android.permission.USE_FULL_SCREEN_INTENT",
		null,
		null,
		"android.permission.RECEIVE_SANDBOX_TRIGGER_AUDIO",
		null,
		null,
		"android.permission.MEDIA_ROUTING_CONTROL",
		null,
		null,
		null,
		"android.permission.READ_SYSTEM_GRAMMATICAL_GENDER",
		null,
		null,
		null,
		"android.permission.LOCATION_BYPASS",
		null,
		"android.permission.health.READ_HEART_RATE",
		"android.permission.health.READ_SKIN_TEMPERATURE",
		null,
		"android.permission.health.READ_OXYGEN_SATURATION",
		null,
		null,
		null,
		null,
		null,
		null,
		null,
		null,
		null,
		null,
		null,
		null,
		null,
		null,
		null,
		null,
		null};
	private static final int[] sOpSwitch = {OP_COARSE_LOCATION, OP_FINE_LOCATION, OP_COARSE_LOCATION, OP_VIBRATE, OP_READ_CONTACTS, OP_WRITE_CONTACTS, OP_READ_CALL_LOG, OP_WRITE_CALL_LOG, OP_READ_CALENDAR, OP_WRITE_CALENDAR, OP_COARSE_LOCATION, OP_POST_NOTIFICATION, OP_COARSE_LOCATION, OP_CALL_PHONE, OP_READ_SMS, OP_WRITE_SMS, OP_RECEIVE_SMS, OP_RECEIVE_SMS, OP_RECEIVE_MMS, OP_RECEIVE_WAP_PUSH, OP_SEND_SMS, OP_READ_SMS, OP_WRITE_SMS, OP_WRITE_SETTINGS, OP_SYSTEM_ALERT_WINDOW, OP_ACCESS_NOTIFICATIONS, OP_CAMERA, OP_RECORD_AUDIO, OP_PLAY_AUDIO, OP_READ_CLIPBOARD, OP_WRITE_CLIPBOARD, OP_TAKE_MEDIA_BUTTONS, OP_TAKE_AUDIO_FOCUS, OP_AUDIO_MASTER_VOLUME, OP_AUDIO_VOICE_VOLUME, OP_AUDIO_RING_VOLUME, OP_AUDIO_MEDIA_VOLUME, OP_AUDIO_ALARM_VOLUME, OP_AUDIO_NOTIFICATION_VOLUME, OP_AUDIO_BLUETOOTH_VOLUME, OP_WAKE_LOCK, OP_COARSE_LOCATION, OP_COARSE_LOCATION, OP_GET_USAGE_STATS, OP_MUTE_MICROPHONE, OP_TOAST_WINDOW, OP_PROJECT_MEDIA, OP_ACTIVATE_VPN, OP_WRITE_WALLPAPER, OP_ASSIST_STRUCTURE, OP_ASSIST_SCREENSHOT, OP_READ_PHONE_STATE, OP_ADD_VOICEMAIL, OP_USE_SIP, OP_PROCESS_OUTGOING_CALLS, OP_USE_FINGERPRINT, OP_BODY_SENSORS, OP_READ_CELL_BROADCASTS, OP_MOCK_LOCATION, OP_READ_EXTERNAL_STORAGE, OP_WRITE_EXTERNAL_STORAGE, OP_TURN_SCREEN_ON, OP_GET_ACCOUNTS, OP_RUN_IN_BACKGROUND, OP_AUDIO_ACCESSIBILITY_VOLUME, OP_READ_PHONE_NUMBERS, OP_REQUEST_INSTALL_PACKAGES, OP_PICTURE_IN_PICTURE, OP_INSTANT_APP_START_FOREGROUND, OP_ANSWER_PHONE_CALLS, OP_RUN_ANY_IN_BACKGROUND, OP_CHANGE_WIFI_STATE, OP_REQUEST_DELETE_PACKAGES, OP_BIND_ACCESSIBILITY_SERVICE, OP_ACCEPT_HANDOVER, OP_MANAGE_IPSEC_TUNNELS, OP_START_FOREGROUND, OP_BLUETOOTH_SCAN, OP_USE_BIOMETRIC, OP_ACTIVITY_RECOGNITION, OP_SMS_FINANCIAL_TRANSACTIONS, OP_READ_MEDIA_AUDIO, OP_WRITE_MEDIA_AUDIO, OP_READ_MEDIA_VIDEO, OP_WRITE_MEDIA_VIDEO, OP_READ_MEDIA_IMAGES, OP_WRITE_MEDIA_IMAGES, OP_LEGACY_STORAGE, OP_ACCESS_ACCESSIBILITY, OP_READ_DEVICE_IDENTIFIERS, OP_ACCESS_MEDIA_LOCATION, OP_QUERY_ALL_PACKAGES, OP_MANAGE_EXTERNAL_STORAGE, OP_INTERACT_ACROSS_PROFILES, OP_ACTIVATE_PLATFORM_VPN, OP_LOADER_USAGE_STATS, -1, OP_AUTO_REVOKE_PERMISSIONS_IF_UNUSED, OP_AUTO_REVOKE_MANAGED_BY_INSTALLER, OP_NO_ISOLATED_STORAGE, OP_PHONE_CALL_MICROPHONE, OP_PHONE_CALL_CAMERA, OP_RECORD_AUDIO_HOTWORD, OP_MANAGE_ONGOING_CALLS, OP_MANAGE_CREDENTIALS, OP_USE_ICC_AUTH_WITH_DEVICE_IDENTIFIER, OP_RECORD_AUDIO_OUTPUT, OP_SCHEDULE_EXACT_ALARM, OP_FINE_LOCATION, OP_COARSE_LOCATION, OP_MANAGE_MEDIA, OP_BLUETOOTH_CONNECT, OP_UWB_RANGING, OP_ACTIVITY_RECOGNITION, OP_BLUETOOTH_ADVERTISE, OP_RECORD_INCOMING_PHONE_AUDIO, OP_NEARBY_WIFI_DEVICES, OP_ESTABLISH_VPN_SERVICE, OP_ESTABLISH_VPN_MANAGER, OP_ACCESS_RESTRICTED_SETTINGS, OP_RECEIVE_AMBIENT_TRIGGER_AUDIO, OP_RECEIVE_EXPLICIT_USER_INTERACTION_AUDIO, OP_RUN_USER_INITIATED_JOBS, OP_READ_MEDIA_VISUAL_USER_SELECTED, OP_SYSTEM_EXEMPT_FROM_SUSPENSION, OP_SYSTEM_EXEMPT_FROM_DISMISSIBLE_NOTIFICATIONS, OP_READ_WRITE_HEALTH_DATA, OP_FOREGROUND_SERVICE_SPECIAL_USE, OP_SYSTEM_EXEMPT_FROM_POWER_RESTRICTIONS, OP_SYSTEM_EXEMPT_FROM_HIBERNATION, OP_SYSTEM_EXEMPT_FROM_ACTIVITY_BG_START_RESTRICTION, OP_CAPTURE_CONSENTLESS_BUGREPORT_ON_USERDEBUG_BUILD, OP_DEPRECATED_2, OP_USE_FULL_SCREEN_INTENT, OP_CAMERA_SANDBOXED, OP_RECORD_AUDIO_SANDBOXED, OP_RECEIVE_SANDBOX_TRIGGER_AUDIO, OP_DEPRECATED_3, OP_CREATE_ACCESSIBILITY_OVERLAY, OP_MEDIA_ROUTING_CONTROL, OP_ENABLE_MOBILE_DATA_BY_USER, OP_RESERVED_FOR_TESTING, OP_RAPID_CLEAR_NOTIFICATIONS_BY_LISTENER, OP_READ_SYSTEM_GRAMMATICAL_GENDER, OP_DEPRECATED_4, OP_ARCHIVE_ICON_OVERLAY, OP_UNARCHIVAL_CONFIRMATION, OP_EMERGENCY_LOCATION, OP_RECEIVE_SENSITIVE_NOTIFICATIONS, OP_READ_HEART_RATE, OP_READ_SKIN_TEMPERATURE, OP_RANGING, OP_READ_OXYGEN_SATURATION, OP_WRITE_SYSTEM_PREFERENCES, OP_CONTROL_AUDIO, OP_CONTROL_AUDIO_PARTIAL, OP_EYE_TRACKING_COARSE, OP_EYE_TRACKING_FINE, OP_FACE_TRACKING, OP_HAND_TRACKING, OP_HEAD_TRACKING, OP_SCENE_UNDERSTANDING_COARSE, OP_SCENE_UNDERSTANDING_FINE, OP_POST_PROMOTED_NOTIFICATIONS, OP_SYSTEM_APPLICATION_OVERLAY, OP_READ_CELL_IDENTITY, OP_READ_CELL_INFO, OP_COMPUTER_CONTROL, OP_READ_OTP_SMS, OP_ACCESS_LOCAL_NETWORK};
	private static final int[] sOpDefaultMode = {MODE_ALLOWED, MODE_ALLOWED, MODE_ALLOWED, MODE_ALLOWED, MODE_ALLOWED, MODE_ALLOWED, MODE_ALLOWED, MODE_ALLOWED, MODE_ALLOWED, MODE_ALLOWED, MODE_ALLOWED, MODE_ALLOWED, MODE_ALLOWED, MODE_ALLOWED, MODE_ALLOWED, MODE_IGNORED, MODE_ALLOWED, MODE_ALLOWED, MODE_ALLOWED, MODE_ALLOWED, MODE_ALLOWED, MODE_ALLOWED, MODE_ALLOWED, MODE_DEFAULT, MODE_DEFAULT, MODE_DEFAULT, MODE_ALLOWED, MODE_ALLOWED, MODE_ALLOWED, MODE_ALLOWED, MODE_ALLOWED, MODE_ALLOWED, MODE_FOREGROUND, MODE_ALLOWED, MODE_ALLOWED, MODE_ALLOWED, MODE_ALLOWED, MODE_ALLOWED, MODE_ALLOWED, MODE_ALLOWED, MODE_ALLOWED, MODE_ALLOWED, MODE_ALLOWED, MODE_DEFAULT, MODE_ALLOWED, MODE_ALLOWED, MODE_IGNORED, MODE_IGNORED, MODE_ALLOWED, MODE_ALLOWED, MODE_ALLOWED, MODE_ALLOWED, MODE_ALLOWED, MODE_ALLOWED, MODE_ALLOWED, MODE_ALLOWED, MODE_ALLOWED, MODE_ALLOWED, MODE_ERRORED, MODE_ALLOWED, MODE_ALLOWED, MODE_DEFAULT, MODE_ALLOWED, MODE_ALLOWED, MODE_ALLOWED, MODE_ALLOWED, MODE_DEFAULT, MODE_ALLOWED, MODE_DEFAULT, MODE_ALLOWED, MODE_ALLOWED, MODE_ALLOWED, MODE_ALLOWED, MODE_ALLOWED, MODE_ALLOWED, MODE_ERRORED, MODE_ALLOWED, MODE_ALLOWED, MODE_ALLOWED, MODE_ALLOWED, MODE_DEFAULT, MODE_ALLOWED, MODE_ERRORED, MODE_ALLOWED, MODE_ERRORED, MODE_ALLOWED, MODE_ERRORED, MODE_DEFAULT, MODE_ALLOWED, MODE_ERRORED, MODE_ALLOWED, MODE_DEFAULT, MODE_DEFAULT, MODE_DEFAULT, MODE_IGNORED, MODE_DEFAULT, MODE_IGNORED, MODE_DEFAULT, MODE_ALLOWED, MODE_ERRORED, MODE_ALLOWED, MODE_ALLOWED, MODE_ALLOWED, MODE_DEFAULT, MODE_DEFAULT, MODE_DEFAULT, MODE_ALLOWED, MODE_DEFAULT, MODE_ALLOWED, MODE_ALLOWED, MODE_DEFAULT, MODE_ALLOWED, MODE_ALLOWED, MODE_ALLOWED, MODE_ALLOWED, MODE_ALLOWED, MODE_ALLOWED, MODE_ALLOWED, MODE_ALLOWED, MODE_DEFAULT, MODE_ALLOWED, MODE_DEFAULT, MODE_ALLOWED, MODE_ALLOWED, MODE_DEFAULT, MODE_DEFAULT, MODE_ALLOWED, MODE_DEFAULT, MODE_DEFAULT, MODE_DEFAULT, MODE_DEFAULT, MODE_DEFAULT, MODE_IGNORED, MODE_DEFAULT, MODE_ALLOWED, MODE_ALLOWED, MODE_DEFAULT, MODE_IGNORED, MODE_ALLOWED, MODE_DEFAULT, MODE_ALLOWED, MODE_ALLOWED, MODE_ALLOWED, MODE_DEFAULT, MODE_IGNORED, MODE_DEFAULT, MODE_DEFAULT, MODE_DEFAULT, MODE_DEFAULT, MODE_ALLOWED, MODE_ALLOWED, MODE_ALLOWED, MODE_ALLOWED, MODE_DEFAULT, MODE_FOREGROUND, MODE_FOREGROUND, MODE_DEFAULT, MODE_DEFAULT, MODE_DEFAULT, MODE_DEFAULT, MODE_DEFAULT, MODE_DEFAULT, MODE_DEFAULT, MODE_DEFAULT, MODE_DEFAULT, MODE_ALLOWED, MODE_ALLOWED, MODE_IGNORED, MODE_DEFAULT, MODE_ALLOWED};

	public static final String[] MODE_NAMES = {"allow", "ignore", "deny", "default", "foreground"};

	public static int strOpToOp(String op) {
		for (int i = 0; i < _NUM_OP; i++) {
			if (sOpNames[i] != null && sOpNames[i].equals(op)) {
				return i;
			}
		}
		throw new IllegalArgumentException("Unknown operation string: " + op);
	}

	public static boolean isValidOp(int op) {
		return op >= 0 && op < _NUM_OP;
	}

	public static int opToSwitch(int op) {
		return sOpSwitch[op];
	}

	public static String opToName(int op) {
		if (op == OP_NONE) return "NONE";
		return op < _NUM_OP ? sOpSimpleNames[op] : ("Unknown(" + op + ")");
	}

	public static String opToPublicName(int op) {
		return sOpNames[op];
	}

	public static String opToPermission(int op) {
		return sOpPerms[op];
	}

	public static String opToPermission(String op) {
		return opToPermission(strOpToOp(op));
	}

	/* AOSP also maps health permissions to OP_READ_WRITE_HEALTH_DATA (HealthConnectManager); not ported */
	public static int permissionToOpCode(String permission) {
		for (int op = 0; op < _NUM_OP; op++) {
			if (sOpPerms[op] != null && sOpPerms[op].equals(permission)) {
				return op;
			}
		}
		return OP_NONE;
	}

	public static int opToDefaultMode(int op) {
		return sOpDefaultMode[op];
	}

	public static int opToDefaultMode(String appOp) {
		return opToDefaultMode(strOpToOp(appOp));
	}

	public static String modeToName(int mode) {
		if (mode >= 0 && mode < MODE_NAMES.length) {
			return MODE_NAMES[mode];
		}
		return "mode=" + mode;
	}

	/* AOSP checkOpRawNoThrow/unsafeCheckOpRawNoThrow: the mode set for the app, without resolving MODE_DEFAULT.
	 * ATL has no AppOpsService; as in the other check calls every op is MODE_ALLOWED. */
	public int checkOpRawNoThrow(String op, int uid, String packageName, String attributionTag) {
		return MODE_ALLOWED;
	}

	public int unsafeCheckOpRawNoThrow(String op, int uid, String packageName) {
		return MODE_ALLOWED;
	}

	public interface OnOpChangedListener {
		void onOpChanged(String op, String packageName);
	}

	/* AOSP registers the listener with AppOpsService, which calls it when the mode changes; ATL has none, the
	 * modes never change and the listener is never called. The op string is checked like in AOSP. */
	public void startWatchingMode(String op, String packageName, OnOpChangedListener callback) {
		strOpToOp(op);
	}

	public void stopWatchingMode(OnOpChangedListener callback) {}

	/* AOSP: the service lists the packages with a mode set for the ops; ATL has none */
	public java.util.List<PackageOps> getPackagesForOps(String[] ops) {
		if (ops != null) {
			for (String op : ops) {
				strOpToOp(op);
			}
		}
		return new java.util.ArrayList<>();
	}

	public static final class PackageOps {
		private final String mPackageName;
		private final int mUid;
		private final java.util.List<OpEntry> mEntries;

		public PackageOps(String packageName, int uid, java.util.List<OpEntry> entries) {
			mPackageName = packageName;
			mUid = uid;
			mEntries = entries;
		}

		public String getPackageName() {
			return mPackageName;
		}

		public int getUid() {
			return mUid;
		}

		public java.util.List<OpEntry> getOps() {
			return mEntries;
		}
	}

	/* AOSP OpEntry: op, mode and per-attribution access events. ATL has no AppOpsService that records events, so
	 * an entry has none: AOSP's getters over zero events answer -1 (times, durations, proxy uid), null (proxy
	 * package) and false (isRunning). */
	public static final class OpEntry {
		private final int mOp;
		private final int mMode;

		public OpEntry(int op, int mode) {
			mOp = op;
			mMode = mode;
		}

		public int getOp() {
			return mOp;
		}

		public String getOpStr() {
			return sOpNames[mOp];
		}

		public int getMode() {
			return mMode;
		}

		@Deprecated
		public long getTime() {
			return -1;
		}

		@Deprecated
		public long getRejectTime() {
			return -1;
		}

		@Deprecated
		public long getLastAccessTime(int flags) {
			return -1;
		}

		@Deprecated
		public long getLastAccessForegroundTime(int flags) {
			return -1;
		}

		@Deprecated
		public long getLastAccessBackgroundTime(int flags) {
			return -1;
		}

		@Deprecated
		public long getLastAccessTime(int fromUidState, int toUidState, int flags) {
			return -1;
		}

		@Deprecated
		public long getLastRejectTime(int flags) {
			return -1;
		}

		@Deprecated
		public long getLastRejectForegroundTime(int flags) {
			return -1;
		}

		@Deprecated
		public long getLastRejectBackgroundTime(int flags) {
			return -1;
		}

		@Deprecated
		public long getLastRejectTime(int fromUidState, int toUidState, int flags) {
			return -1;
		}

		@Deprecated
		public long getLastAccessTime() {
			return -1;
		}

		@Deprecated
		public long getLastAccessForegroundTime() {
			return -1;
		}

		@Deprecated
		public long getLastAccessBackgroundTime() {
			return -1;
		}

		@Deprecated
		public long getLastRejectTime() {
			return -1;
		}

		@Deprecated
		public long getLastRejectForegroundTime() {
			return -1;
		}

		@Deprecated
		public long getLastRejectBackgroundTime() {
			return -1;
		}

		public boolean isRunning() {
			return false;
		}

		@Deprecated
		public long getDuration() {
			return -1;
		}

		@Deprecated
		public long getLastDuration(int flags) {
			return -1;
		}

		@Deprecated
		public long getLastForegroundDuration(int flags) {
			return -1;
		}

		@Deprecated
		public long getLastBackgroundDuration(int flags) {
			return -1;
		}

		@Deprecated
		public long getLastDuration(int fromUidState, int toUidState, int flags) {
			return -1;
		}

		@Deprecated
		public int getProxyUid() {
			return -1;
		}

		@Deprecated
		public String getProxyPackageName() {
			return null;
		}

		public int getProxyUid(int uidState, int flags) {
			return -1;
		}

		public String getProxyPackageName(int uidState, int flags) {
			return null;
		}
	}
}
