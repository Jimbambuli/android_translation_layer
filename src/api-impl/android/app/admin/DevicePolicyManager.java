package android.app.admin;

import android.content.ComponentName;
import java.util.List;

/**
 * Device policy manager of a device without any device administrator, device owner or profile
 * owner (what an AOSP DevicePolicyManagerService reports for an unmanaged user). The constants
 * are copied from AOSP; the methods answer like the AOSP client when the service has no active
 * admin: lists are null, queries for a given admin give the policy default, and everything that
 * needs an active admin throws SecurityException as documented in AOSP.
 */
public class DevicePolicyManager {

    public static final String DEPRECATE_USERMANAGERINTERNAL_DEVICEPOLICY_FLAG =
            "deprecate_usermanagerinternal_devicepolicy";
    public static final boolean DEPRECATE_USERMANAGERINTERNAL_DEVICEPOLICY_DEFAULT = true;
    public static final String ADD_ISFINANCED_DEVICE_FLAG =
            "add-isfinanced-device";
    public static final boolean ADD_ISFINANCED_FEVICE_DEFAULT = true;
    public static final String ACTION_PROVISION_MANAGED_PROFILE
        = "android.app.action.PROVISION_MANAGED_PROFILE";
    public static final String ACTION_PROVISION_MANAGED_USER
        = "android.app.action.PROVISION_MANAGED_USER";
    public static final String ACTION_PROVISION_MANAGED_DEVICE
        = "android.app.action.PROVISION_MANAGED_DEVICE";
    public static final String ACTION_STATE_USER_SETUP_COMPLETE =
            "android.app.action.STATE_USER_SETUP_COMPLETE";
    public static final String ACTION_PROVISION_MANAGED_DEVICE_FROM_TRUSTED_SOURCE =
            "android.app.action.PROVISION_MANAGED_DEVICE_FROM_TRUSTED_SOURCE";
    public static final String ACTION_PROVISION_FINANCED_DEVICE =
            "android.app.action.PROVISION_FINANCED_DEVICE";
    public static final String ACTION_PROVISION_FINALIZATION
            = "android.app.action.PROVISION_FINALIZATION";
    public static final String ACTION_ROLE_HOLDER_PROVISION_MANAGED_PROFILE =
            "android.app.action.ROLE_HOLDER_PROVISION_MANAGED_PROFILE";
    public static final int RESULT_WORK_PROFILE_CREATED = 122;
    public static final int RESULT_DEVICE_OWNER_SET = 123;
    public static final String ACTION_ROLE_HOLDER_PROVISION_MANAGED_DEVICE_FROM_TRUSTED_SOURCE =
            "android.app.action.ROLE_HOLDER_PROVISION_MANAGED_DEVICE_FROM_TRUSTED_SOURCE";
    public static final String ACTION_ROLE_HOLDER_PROVISION_FINALIZATION =
            "android.app.action.ROLE_HOLDER_PROVISION_FINALIZATION";
    public static final int RESULT_UPDATE_ROLE_HOLDER = 2;
    public static final String EXTRA_ROLE_HOLDER_STATE = "android.app.extra.ROLE_HOLDER_STATE";
    public static final String EXTRA_FORCE_UPDATE_ROLE_HOLDER =
            "android.app.extra.FORCE_UPDATE_ROLE_HOLDER";
    public static final String EXTRA_PROVISIONING_ALLOW_OFFLINE =
            "android.app.extra.PROVISIONING_ALLOW_OFFLINE";
    public static final String EXTRA_PROVISIONING_ROLE_HOLDER_PACKAGE_DOWNLOAD_LOCATION =
            "android.app.extra.PROVISIONING_ROLE_HOLDER_PACKAGE_DOWNLOAD_LOCATION";
    public static final String EXTRA_PROVISIONING_ROLE_HOLDER_SIGNATURE_CHECKSUM =
            "android.app.extra.PROVISIONING_ROLE_HOLDER_SIGNATURE_CHECKSUM";
    public static final String EXTRA_PROVISIONING_ROLE_HOLDER_PACKAGE_DOWNLOAD_COOKIE_HEADER =
            "android.app.extra.PROVISIONING_ROLE_HOLDER_PACKAGE_DOWNLOAD_COOKIE_HEADER";
    public static final String EXTRA_PROVISIONING_ROLE_HOLDER_EXTRAS_BUNDLE =
            "android.app.extra.PROVISIONING_ROLE_HOLDER_EXTRAS_BUNDLE";
    public static final String EXTRA_ROLE_HOLDER_PROVISIONING_INITIATOR_PACKAGE =
            "android.app.extra.ROLE_HOLDER_PROVISIONING_INITIATOR_PACKAGE";
    public static final String EXTRA_RESULT_LAUNCH_INTENT =
            "android.app.extra.RESULT_LAUNCH_INTENT";
    public static final String EXTRA_PROVISIONING_SHOULD_LAUNCH_RESULT_INTENT =
            "android.app.extra.PROVISIONING_SHOULD_LAUNCH_RESULT_INTENT";
    public static final String ACTION_BUGREPORT_SHARING_ACCEPTED =
            "com.android.server.action.REMOTE_BUGREPORT_SHARING_ACCEPTED";
    public static final String ACTION_BUGREPORT_SHARING_DECLINED =
            "com.android.server.action.REMOTE_BUGREPORT_SHARING_DECLINED";
    public static final String ACTION_REMOTE_BUGREPORT_DISPATCH =
            "android.intent.action.REMOTE_BUGREPORT_DISPATCH";
    public static final String EXTRA_REMOTE_BUGREPORT_HASH =
            "android.intent.extra.REMOTE_BUGREPORT_HASH";
    public static final String EXTRA_REMOTE_BUGREPORT_NONCE =
            "android.intent.extra.REMOTE_BUGREPORT_NONCE";
    public static final String EXTRA_BUGREPORT_NOTIFICATION_TYPE =
            "android.app.extra.bugreport_notification_type";
    public static final boolean PREFERENTIAL_NETWORK_SERVICE_ENABLED_DEFAULT = false;
    public static final int NOTIFICATION_BUGREPORT_STARTED = 1;
    public static final int NOTIFICATION_BUGREPORT_ACCEPTED_NOT_FINISHED = 2;
    public static final int NOTIFICATION_BUGREPORT_FINISHED_NOT_ACCEPTED = 3;
    public static final long DEFAULT_STRONG_AUTH_TIMEOUT_MS = 72 * 60 * 60 * 1000; // 72h

    /**
     * A {@link android.os.Parcelable} extra of type {@link android.os.PersistableBundle} that
     * allows a mobile device management application or NFC programmer application which starts
     * managed provisioning to pass data to the management application instance after provisioning.
     * <p>
     * If used with {@link #ACTION_PROVISION_MANAGED_PROFILE} it can be used by the application that
     * sends the intent to pass data to itself on the newly created profile.
     * If used with {@link #ACTION_PROVISION_MANAGED_DEVICE} it allows passing data to the same
     * instance of the app on the primary user.
     * Starting from {@link android.os.Build.VERSION_CODES#M}, if used with
     * {@link #MIME_TYPE_PROVISIONING_NFC} as part of NFC managed device provisioning, the NFC
     * message should contain a stringified {@link java.util.Properties} instance, whose string
     * properties will be converted into a {@link android.os.PersistableBundle} and passed to the
     * management application after provisioning.
     *
     * <p>Admin apps will receive this extra in their {@link #ACTION_GET_PROVISIONING_MODE} and
     * {@link #ACTION_ADMIN_POLICY_COMPLIANCE} intent handlers. Additionally, {@link
     * #ACTION_GET_PROVISIONING_MODE} may also return this extra which will then be sent over to
     * {@link #ACTION_ADMIN_POLICY_COMPLIANCE}, alongside the original values that were passed to
     * {@link #ACTION_GET_PROVISIONING_MODE}.
     *
     * <p>
     * In both cases the application receives the data in
     * {@link DeviceAdminReceiver#onProfileProvisioningComplete} via an intent with the action
     * {@link DeviceAdminReceiver#ACTION_PROFILE_PROVISIONING_COMPLETE}. The bundle is not changed
     * during the managed provisioning.
     */
    public static final String EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE =
            "android.app.extra.PROVISIONING_ADMIN_EXTRAS_BUNDLE";
    public static final String EXTRA_PROVISIONING_DEVICE_ADMIN_PACKAGE_NAME
        = "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_NAME";
    public static final String EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME
        = "android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME";
    public static final String EXTRA_PROVISIONING_ACCOUNT_TO_MIGRATE
        = "android.app.extra.PROVISIONING_ACCOUNT_TO_MIGRATE";
    public static final String EXTRA_PROVISIONING_KEEP_ACCOUNT_ON_MIGRATION
            = "android.app.extra.PROVISIONING_KEEP_ACCOUNT_ON_MIGRATION";
    public static final String EXTRA_PROVISIONING_EMAIL_ADDRESS
        = "android.app.extra.PROVISIONING_EMAIL_ADDRESS";
    public static final String EXTRA_PROVISIONING_MAIN_COLOR =
             "android.app.extra.PROVISIONING_MAIN_COLOR";
    public static final String EXTRA_PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED =
            "android.app.extra.PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED";
    public static final String EXTRA_PROVISIONING_TIME_ZONE
        = "android.app.extra.PROVISIONING_TIME_ZONE";
    public static final String EXTRA_PROVISIONING_LOCAL_TIME
        = "android.app.extra.PROVISIONING_LOCAL_TIME";
    public static final String EXTRA_PROVISIONING_LOCALE
        = "android.app.extra.PROVISIONING_LOCALE";
    public static final String EXTRA_PROVISIONING_WIFI_SSID
        = "android.app.extra.PROVISIONING_WIFI_SSID";
    public static final String EXTRA_PROVISIONING_WIFI_HIDDEN
        = "android.app.extra.PROVISIONING_WIFI_HIDDEN";
    public static final String EXTRA_PROVISIONING_WIFI_SECURITY_TYPE
        = "android.app.extra.PROVISIONING_WIFI_SECURITY_TYPE";
    public static final String EXTRA_PROVISIONING_WIFI_PASSWORD =
            "android.app.extra.PROVISIONING_WIFI_PASSWORD";
    public static final String EXTRA_PROVISIONING_WIFI_EAP_METHOD =
            "android.app.extra.PROVISIONING_WIFI_EAP_METHOD";
    public static final String EXTRA_PROVISIONING_WIFI_PHASE2_AUTH =
            "android.app.extra.PROVISIONING_WIFI_PHASE2_AUTH";
    public static final String EXTRA_PROVISIONING_WIFI_CA_CERTIFICATE =
            "android.app.extra.PROVISIONING_WIFI_CA_CERTIFICATE";
    public static final String EXTRA_PROVISIONING_WIFI_USER_CERTIFICATE =
            "android.app.extra.PROVISIONING_WIFI_USER_CERTIFICATE";
    public static final String EXTRA_PROVISIONING_WIFI_IDENTITY =
            "android.app.extra.PROVISIONING_WIFI_IDENTITY";
    public static final String EXTRA_PROVISIONING_WIFI_ANONYMOUS_IDENTITY =
            "android.app.extra.PROVISIONING_WIFI_ANONYMOUS_IDENTITY";
    public static final String EXTRA_PROVISIONING_WIFI_DOMAIN =
            "android.app.extra.PROVISIONING_WIFI_DOMAIN";
    public static final String EXTRA_PROVISIONING_WIFI_PROXY_HOST
        = "android.app.extra.PROVISIONING_WIFI_PROXY_HOST";
    public static final String EXTRA_PROVISIONING_WIFI_PROXY_PORT
        = "android.app.extra.PROVISIONING_WIFI_PROXY_PORT";
    public static final String EXTRA_PROVISIONING_WIFI_PROXY_BYPASS
        = "android.app.extra.PROVISIONING_WIFI_PROXY_BYPASS";
    public static final String EXTRA_PROVISIONING_WIFI_PAC_URL
        = "android.app.extra.PROVISIONING_WIFI_PAC_URL";
    public static final String EXTRA_PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION
        = "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION";
    public static final String EXTRA_PROVISIONING_ORGANIZATION_NAME =
            "android.app.extra.PROVISIONING_ORGANIZATION_NAME";
    public static final String EXTRA_PROVISIONING_SUPPORT_URL =
            "android.app.extra.PROVISIONING_SUPPORT_URL";
    public static final String EXTRA_PROVISIONING_DEVICE_ADMIN_PACKAGE_LABEL =
            "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_LABEL";
    public static final String EXTRA_PROVISIONING_DEVICE_ADMIN_PACKAGE_ICON_URI =
            "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_ICON_URI";
    public static final String EXTRA_PROVISIONING_DEVICE_ADMIN_MINIMUM_VERSION_CODE
        = "android.app.extra.PROVISIONING_DEVICE_ADMIN_MINIMUM_VERSION_CODE";
    public static final String EXTRA_PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_COOKIE_HEADER
        = "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_COOKIE_HEADER";
    public static final String EXTRA_PROVISIONING_DEVICE_ADMIN_PACKAGE_CHECKSUM
        = "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_CHECKSUM";
    public static final String EXTRA_PROVISIONING_SENSORS_PERMISSION_GRANT_OPT_OUT =
            "android.app.extra.PROVISIONING_SENSORS_PERMISSION_GRANT_OPT_OUT";
    public static final String EXTRA_PROVISIONING_DEVICE_ADMIN_SIGNATURE_CHECKSUM
        = "android.app.extra.PROVISIONING_DEVICE_ADMIN_SIGNATURE_CHECKSUM";
    public static final String ACTION_MANAGED_PROFILE_PROVISIONED
        = "android.app.action.MANAGED_PROFILE_PROVISIONED";
    public static final String ACTION_PROVISIONING_SUCCESSFUL =
            "android.app.action.PROVISIONING_SUCCESSFUL";
    public static final String EXTRA_PROVISIONING_SKIP_ENCRYPTION =
             "android.app.extra.PROVISIONING_SKIP_ENCRYPTION";
    public static final String EXTRA_PROVISIONING_LOGO_URI =
            "android.app.extra.PROVISIONING_LOGO_URI";
    public static final String EXTRA_PROVISIONING_DISCLAIMERS =
            "android.app.extra.PROVISIONING_DISCLAIMERS";
    public static final String EXTRA_PROVISIONING_DISCLAIMER_HEADER =
            "android.app.extra.PROVISIONING_DISCLAIMER_HEADER";
    public static final String EXTRA_PROVISIONING_DISCLAIMER_CONTENT =
            "android.app.extra.PROVISIONING_DISCLAIMER_CONTENT";
    public static final String EXTRA_PROVISIONING_SKIP_USER_CONSENT =
            "android.app.extra.PROVISIONING_SKIP_USER_CONSENT";
    public static final String EXTRA_PROVISIONING_SKIP_EDUCATION_SCREENS =
            "android.app.extra.PROVISIONING_SKIP_EDUCATION_SCREENS";
    public static final String EXTRA_PROVISIONING_USE_MOBILE_DATA =
            "android.app.extra.PROVISIONING_USE_MOBILE_DATA";
    public static final String EXTRA_PROVISIONING_TRIGGER =
            "android.app.extra.PROVISIONING_TRIGGER";
    public static final int PROVISIONING_TRIGGER_UNSPECIFIED = 0;
    public static final int PROVISIONING_TRIGGER_CLOUD_ENROLLMENT = 1;
    public static final int PROVISIONING_TRIGGER_QR_CODE = 2;
    public static final int PROVISIONING_TRIGGER_PERSISTENT_DEVICE_OWNER = 3;
    public static final int PROVISIONING_TRIGGER_MANAGED_ACCOUNT = 4;
    public static final int PROVISIONING_TRIGGER_NFC = 5;
    public static final int FLAG_SUPPORTED_MODES_ORGANIZATION_OWNED = 1;
    public static final int FLAG_SUPPORTED_MODES_PERSONALLY_OWNED = 1 << 1;
    public static final int FLAG_SUPPORTED_MODES_DEVICE_OWNER = 1 << 2;
    public static final int WIFI_SECURITY_OPEN = 0;
    public static final int WIFI_SECURITY_PERSONAL = 1;
    public static final int WIFI_SECURITY_ENTERPRISE_EAP = 2;
    public static final int WIFI_SECURITY_ENTERPRISE_192 = 3;
    public static final String MIME_TYPE_PROVISIONING_NFC
        = "application/com.android.managedprovisioning";
    public static final String ACTION_ADD_DEVICE_ADMIN
            = "android.app.action.ADD_DEVICE_ADMIN";
    public static final String ACTION_SET_PROFILE_OWNER
            = "android.app.action.SET_PROFILE_OWNER";
    public static final String EXTRA_PROFILE_OWNER_NAME
            = "android.app.extra.PROFILE_OWNER_NAME";
    public static final String ACTION_DEVICE_POLICY_MANAGER_STATE_CHANGED
            = "android.app.action.DEVICE_POLICY_MANAGER_STATE_CHANGED";
    public static final String ACTION_DEVICE_OWNER_CHANGED
            = "android.app.action.DEVICE_OWNER_CHANGED";
    public static final String ACTION_RESET_PROTECTION_POLICY_CHANGED =
            "android.app.action.RESET_PROTECTION_POLICY_CHANGED";
    public static final String ACTION_LOST_MODE_LOCATION_UPDATE =
            "android.app.action.LOST_MODE_LOCATION_UPDATE";
    public static final String EXTRA_LOST_MODE_LOCATION =
            "android.app.extra.LOST_MODE_LOCATION";
    public static final String EXTRA_DEVICE_ADMIN = "android.app.extra.DEVICE_ADMIN";
    public static final String EXTRA_ADD_EXPLANATION = "android.app.extra.ADD_EXPLANATION";
    public static final String POLICY_DISABLE_CAMERA = "policy_disable_camera";
    public static final String POLICY_DISABLE_SCREEN_CAPTURE = "policy_disable_screen_capture";
    public static final String POLICY_SUSPEND_PACKAGES = "policy_suspend_packages";
    public static final String EXTRA_RESTRICTION = "android.app.extra.RESTRICTION";
    public static final String ACTION_SET_NEW_PASSWORD
            = "android.app.action.SET_NEW_PASSWORD";
    public static final String EXTRA_PASSWORD_COMPLEXITY =
            "android.app.extra.PASSWORD_COMPLEXITY";
    public static final int PASSWORD_COMPLEXITY_NONE = 0;
    public static final int PASSWORD_COMPLEXITY_LOW = 0x10000;
    public static final int PASSWORD_COMPLEXITY_MEDIUM = 0x30000;
    public static final int PASSWORD_COMPLEXITY_HIGH = 0x50000;
    public static final String EXTRA_DEVICE_PASSWORD_REQUIREMENT_ONLY =
            "android.app.extra.DEVICE_PASSWORD_REQUIREMENT_ONLY";
    public static final int NEARBY_STREAMING_NOT_CONTROLLED_BY_POLICY = 0;
    public static final int NEARBY_STREAMING_DISABLED = 1;
    public static final int NEARBY_STREAMING_ENABLED = 2;
    public static final int NEARBY_STREAMING_SAME_MANAGED_ACCOUNT_ONLY = 3;
    public static final String ACTION_SET_NEW_PARENT_PROFILE_PASSWORD
            = "android.app.action.SET_NEW_PARENT_PROFILE_PASSWORD";
    public static final String ACTION_SHOW_DEVICE_MONITORING_DIALOG
            = "android.app.action.SHOW_DEVICE_MONITORING_DIALOG";
    public static final String ACTION_APPLICATION_DELEGATION_SCOPES_CHANGED =
            "android.app.action.APPLICATION_DELEGATION_SCOPES_CHANGED";
    public static final String EXTRA_DELEGATION_SCOPES = "android.app.extra.DELEGATION_SCOPES";
    public static final int FLAG_PARENT_CAN_ACCESS_MANAGED = 0x0001;
    public static final int FLAG_MANAGED_CAN_ACCESS_PARENT = 0x0002;
    public static final String ACTION_SYSTEM_UPDATE_POLICY_CHANGED
            = "android.app.action.SYSTEM_UPDATE_POLICY_CHANGED";
    public static final String ACTION_DATA_SHARING_RESTRICTION_APPLIED =
            "android.app.action.DATA_SHARING_RESTRICTION_APPLIED";
    public static final String ACTION_DEVICE_POLICY_CONSTANTS_CHANGED =
            "android.app.action.DEVICE_POLICY_CONSTANTS_CHANGED";
    public static final int PERMISSION_POLICY_PROMPT = 0;
    public static final int PERMISSION_POLICY_AUTO_GRANT = 1;
    public static final int PERMISSION_POLICY_AUTO_DENY = 2;
    public static final int PERMISSION_GRANT_STATE_DEFAULT = 0;
    public static final int PERMISSION_GRANT_STATE_GRANTED = 1;
    public static final int PERMISSION_GRANT_STATE_DENIED = 2;
    public static final String DELEGATION_CERT_INSTALL = "delegation-cert-install";
    public static final String DELEGATION_APP_RESTRICTIONS = "delegation-app-restrictions";
    public static final String DELEGATION_BLOCK_UNINSTALL = "delegation-block-uninstall";
    public static final String DELEGATION_PERMISSION_GRANT = "delegation-permission-grant";
    public static final String DELEGATION_PACKAGE_ACCESS = "delegation-package-access";
    public static final String DELEGATION_ENABLE_SYSTEM_APP = "delegation-enable-system-app";
    public static final String DELEGATION_INSTALL_EXISTING_PACKAGE =
            "delegation-install-existing-package";
    public static final String DELEGATION_KEEP_UNINSTALLED_PACKAGES =
            "delegation-keep-uninstalled-packages";
    public static final String DELEGATION_NETWORK_LOGGING = "delegation-network-logging";
    public static final String DELEGATION_CERT_SELECTION = "delegation-cert-selection";
    public static final String DELEGATION_SECURITY_LOGGING = "delegation-security-logging";
    public static final int STATE_USER_UNMANAGED = 0;
    public static final int STATE_USER_SETUP_INCOMPLETE = 1;
    public static final int STATE_USER_SETUP_COMPLETE = 2;
    public static final int STATE_USER_SETUP_FINALIZED = 3;
    public static final int STATE_USER_PROFILE_COMPLETE = 4;
    public static final int STATE_USER_PROFILE_FINALIZED = 5;
    public static final int STATUS_UNKNOWN_ERROR = -1;
    public static final int STATUS_OK = 0;
    public static final int STATUS_HAS_DEVICE_OWNER = 1;
    public static final int STATUS_USER_HAS_PROFILE_OWNER = 2;
    public static final int STATUS_USER_NOT_RUNNING = 3;
    public static final int STATUS_USER_SETUP_COMPLETED = 4;
    public static final int STATUS_NONSYSTEM_USER_EXISTS = 5;
    public static final int STATUS_ACCOUNTS_NOT_EMPTY = 6;
    public static final int STATUS_NOT_SYSTEM_USER = 7;
    public static final int STATUS_HAS_PAIRED = 8;
    public static final int STATUS_MANAGED_USERS_NOT_SUPPORTED = 9;
    public static final int STATUS_SYSTEM_USER = 10;
    public static final int STATUS_CANNOT_ADD_MANAGED_PROFILE = 11;
    public static final int STATUS_DEVICE_ADMIN_NOT_SUPPORTED = 13;
    public static final int STATUS_SPLIT_SYSTEM_USER_DEVICE_SYSTEM_USER = 14;
    public static final int STATUS_PROVISIONING_NOT_ALLOWED_FOR_NON_DEVELOPER_USERS = 15;
    public static final int STATUS_HEADLESS_SYSTEM_USER_MODE_NOT_SUPPORTED = 16;
    public static final int LOCK_TASK_FEATURE_NONE = 0;
    public static final int LOCK_TASK_FEATURE_SYSTEM_INFO = 1;
    public static final int LOCK_TASK_FEATURE_NOTIFICATIONS = 1 << 1;
    public static final int LOCK_TASK_FEATURE_HOME = 1 << 2;
    public static final int LOCK_TASK_FEATURE_OVERVIEW = 1 << 3;
    public static final int LOCK_TASK_FEATURE_GLOBAL_ACTIONS = 1 << 4;
    public static final int LOCK_TASK_FEATURE_KEYGUARD = 1 << 5;
    public static final int LOCK_TASK_FEATURE_BLOCK_ACTIVITY_START_IN_TASK = 1 << 6;
    public static final String ACTION_DEVICE_ADMIN_SERVICE
            = "android.app.action.DEVICE_ADMIN_SERVICE";
    public static final int ID_TYPE_BASE_INFO = 1;
    public static final int ID_TYPE_SERIAL = 2;
    public static final int ID_TYPE_IMEI = 4;
    public static final int ID_TYPE_MEID = 8;
    public static final int ID_TYPE_INDIVIDUAL_ATTESTATION = 16;
    public static final int KEY_GEN_STRONGBOX_UNAVAILABLE = 1;
    public static final int INSTALLKEY_REQUEST_CREDENTIALS_ACCESS = 1;
    public static final int INSTALLKEY_SET_USER_SELECTABLE = 2;
    public static final String ACTION_PROFILE_OWNER_CHANGED =
            "android.app.action.PROFILE_OWNER_CHANGED";
    public static final int PRIVATE_DNS_MODE_UNKNOWN = 0;
    public static final int PRIVATE_DNS_MODE_OFF = 1;
    public static final int PRIVATE_DNS_MODE_OPPORTUNISTIC = 2;
    public static final int PRIVATE_DNS_MODE_PROVIDER_HOSTNAME = 3;
    public static final int PRIVATE_DNS_SET_NO_ERROR = 0;
    public static final int PRIVATE_DNS_SET_ERROR_HOST_NOT_SERVING = 1;
    public static final int PRIVATE_DNS_SET_ERROR_FAILURE_SETTING = 2;
    public static final String ACTION_GET_PROVISIONING_MODE =
            "android.app.action.GET_PROVISIONING_MODE";
    public static final String EXTRA_PROVISIONING_IMEI = "android.app.extra.PROVISIONING_IMEI";
    public static final String EXTRA_PROVISIONING_SERIAL_NUMBER =
            "android.app.extra.PROVISIONING_SERIAL_NUMBER";
    public static final String EXTRA_PROVISIONING_MODE =
            "android.app.extra.PROVISIONING_MODE";
    public static final String EXTRA_PROVISIONING_SUPPORTED_MODES =
            "android.app.extra.PROVISIONING_SUPPORTED_MODES";
    public static final String EXTRA_PROVISIONING_SKIP_OWNERSHIP_DISCLAIMER =
            "android.app.extra.PROVISIONING_SKIP_OWNERSHIP_DISCLAIMER";
    public static final String EXTRA_PROVISIONING_ALLOWED_PROVISIONING_MODES =
            "android.app.extra.PROVISIONING_ALLOWED_PROVISIONING_MODES";
    public static final int PROVISIONING_MODE_FULLY_MANAGED_DEVICE = 1;
    public static final int PROVISIONING_MODE_MANAGED_PROFILE = 2;
    public static final int PROVISIONING_MODE_MANAGED_PROFILE_ON_PERSONAL_DEVICE = 3;
    public static final String EXTRA_PROVISIONING_RETURN_BEFORE_POLICY_COMPLIANCE =
            "android.app.extra.PROVISIONING_RETURN_BEFORE_POLICY_COMPLIANCE";
    public static final String EXTRA_PROVISIONING_KEEP_SCREEN_ON =
            "android.app.extra.PROVISIONING_KEEP_SCREEN_ON";
    public static final String ACTION_ADMIN_POLICY_COMPLIANCE =
            "android.app.action.ADMIN_POLICY_COMPLIANCE";
    public static final String ACTION_UPDATE_DEVICE_POLICY_MANAGEMENT_ROLE_HOLDER =
            "android.app.action.UPDATE_DEVICE_POLICY_MANAGEMENT_ROLE_HOLDER";
    public static final int RESULT_UPDATE_DEVICE_POLICY_MANAGEMENT_ROLE_HOLDER_RECOVERABLE_ERROR =
            1;
    public static final int RESULT_UPDATE_DEVICE_POLICY_MANAGEMENT_ROLE_HOLDER_UNRECOVERABLE_ERROR =
            2;
    public static final String EXTRA_ROLE_HOLDER_UPDATE_FAILURE_STRATEGY =
            "android.app.extra.ROLE_HOLDER_UPDATE_FAILURE_STRATEGY";
    public static final int ROLE_HOLDER_UPDATE_FAILURE_STRATEGY_FAIL_PROVISIONING = 1;
    public static final int ROLE_HOLDER_UPDATE_FAILURE_STRATEGY_FALLBACK_TO_PLATFORM_PROVISIONING =
            2;
    public static final String EXTRA_ROLE_HOLDER_UPDATE_RESULT_CODE =
            "android.app.extra.ROLE_HOLDER_UPDATE_RESULT_CODE";
    public static final String EXTRA_PROVISIONING_ROLE_HOLDER_CUSTOM_USER_CONSENT_INTENT =
            "android.app.extra.PROVISIONING_ROLE_HOLDER_CUSTOM_USER_CONSENT_INTENT";
    public static final String ACTION_ESTABLISH_NETWORK_CONNECTION =
            "android.app.action.ESTABLISH_NETWORK_CONNECTION";
    public static final int MAX_PASSWORD_LENGTH = 16;
    public static final String ACTION_BIND_SECONDARY_LOCKSCREEN_SERVICE =
            "android.app.action.BIND_SECONDARY_LOCKSCREEN_SERVICE";
    public static final int PERSONAL_APPS_NOT_SUSPENDED = 0;
    public static final int PERSONAL_APPS_SUSPENDED_EXPLICITLY = 1 << 0;
    public static final int PERSONAL_APPS_SUSPENDED_PROFILE_TIMEOUT = 1 << 1;
    public static final int DEVICE_OWNER_TYPE_DEFAULT = 0;
    public static final int DEVICE_OWNER_TYPE_FINANCED = 1;
    public static final int OPERATION_LOCK_NOW = 1;
    public static final int OPERATION_SWITCH_USER = 2;
    public static final int OPERATION_START_USER_IN_BACKGROUND = 3;
    public static final int OPERATION_STOP_USER = 4;
    public static final int OPERATION_CREATE_AND_MANAGE_USER = 5;
    public static final int OPERATION_REMOVE_USER = 6;
    public static final int OPERATION_REBOOT = 7;
    public static final int OPERATION_WIPE_DATA = 8;
    public static final int OPERATION_LOGOUT_USER = 9;
    public static final int OPERATION_SET_USER_RESTRICTION = 10;
    public static final int OPERATION_SET_SYSTEM_SETTING = 11;
    public static final int OPERATION_SET_KEYGUARD_DISABLED = 12;
    public static final int OPERATION_SET_STATUS_BAR_DISABLED = 13;
    public static final int OPERATION_SET_SYSTEM_UPDATE_POLICY = 14;
    public static final int OPERATION_SET_APPLICATION_HIDDEN = 15;
    public static final int OPERATION_SET_APPLICATION_RESTRICTIONS = 16;
    public static final int OPERATION_SET_KEEP_UNINSTALLED_PACKAGES = 17;
    public static final int OPERATION_SET_LOCK_TASK_FEATURES = 18;
    public static final int OPERATION_SET_LOCK_TASK_PACKAGES = 19;
    public static final int OPERATION_SET_PACKAGES_SUSPENDED = 20;
    public static final int OPERATION_SET_TRUST_AGENT_CONFIGURATION = 21;
    public static final int OPERATION_SET_USER_CONTROL_DISABLED_PACKAGES = 22;
    public static final int OPERATION_CLEAR_APPLICATION_USER_DATA = 23;
    public static final int OPERATION_INSTALL_CA_CERT = 24;
    public static final int OPERATION_INSTALL_KEY_PAIR = 25;
    public static final int OPERATION_INSTALL_SYSTEM_UPDATE = 26;
    public static final int OPERATION_REMOVE_ACTIVE_ADMIN = 27;
    public static final int OPERATION_REMOVE_KEY_PAIR = 28;
    public static final int OPERATION_REQUEST_BUGREPORT = 29;
    public static final int OPERATION_SET_ALWAYS_ON_VPN_PACKAGE = 30;
    public static final int OPERATION_SET_CAMERA_DISABLED = 31;
    public static final int OPERATION_SET_FACTORY_RESET_PROTECTION_POLICY = 32;
    public static final int OPERATION_SET_GLOBAL_PRIVATE_DNS = 33;
    public static final int OPERATION_SET_LOGOUT_ENABLED = 34;
    public static final int OPERATION_SET_MASTER_VOLUME_MUTED = 35;
    public static final int OPERATION_SET_OVERRIDE_APNS_ENABLED = 36;
    public static final int OPERATION_SET_PERMISSION_GRANT_STATE = 37;
    public static final int OPERATION_SET_PERMISSION_POLICY = 38;
    public static final int OPERATION_SET_RESTRICTIONS_PROVIDER = 39;
    public static final int OPERATION_UNINSTALL_CA_CERT = 40;
    public static final int OPERATION_SAFETY_REASON_NONE = -1;
    public static final int OPERATION_SAFETY_REASON_DRIVING_DISTRACTION = 1;
    public static final int EXEMPT_FROM_SUSPENSION =  0;
    public static final int EXEMPT_FROM_DISMISSIBLE_NOTIFICATIONS =  1;
    public static final int EXEMPT_FROM_ACTIVITY_BG_START_RESTRICTION = 2;
    public static final int EXEMPT_FROM_HIBERNATION =  3;
    public static final int EXEMPT_FROM_POWER_RESTRICTIONS =  4;
    public static final String ACTION_DEVICE_POLICY_RESOURCE_UPDATED =
            "android.app.action.DEVICE_POLICY_RESOURCE_UPDATED";
    public static final String EXTRA_RESOURCE_TYPE =
            "android.app.extra.RESOURCE_TYPE";
    public static final int EXTRA_RESOURCE_TYPE_DRAWABLE = 1;
    public static final int EXTRA_RESOURCE_TYPE_STRING = 2;
    public static final String EXTRA_RESOURCE_IDS =
            "android.app.extra.RESOURCE_IDS";
    public static final String ACTION_DEVICE_FINANCING_STATE_CHANGED =
            "android.app.admin.action.DEVICE_FINANCING_STATE_CHANGED";
    public static final int MTE_NOT_CONTROLLED_BY_POLICY = 0;
    public static final int MTE_ENABLED = 1;
    public static final int MTE_DISABLED = 2;
    public static final int PASSWORD_QUALITY_UNSPECIFIED = 0;
    public static final int PASSWORD_QUALITY_BIOMETRIC_WEAK = 0x8000;
    public static final int PASSWORD_QUALITY_SOMETHING = 0x10000;
    public static final int PASSWORD_QUALITY_NUMERIC = 0x20000;
    public static final int PASSWORD_QUALITY_NUMERIC_COMPLEX = 0x30000;
    public static final int PASSWORD_QUALITY_ALPHABETIC = 0x40000;
    public static final int PASSWORD_QUALITY_ALPHANUMERIC = 0x50000;
    public static final int PASSWORD_QUALITY_COMPLEX = 0x60000;
    public static final int PASSWORD_QUALITY_MANAGED = 0x80000;
    public static final String ACCOUNT_FEATURE_DEVICE_OR_PROFILE_OWNER_ALLOWED =
            "android.account.DEVICE_OR_PROFILE_OWNER_ALLOWED";
    public static final String ACCOUNT_FEATURE_DEVICE_OR_PROFILE_OWNER_DISALLOWED =
            "android.account.DEVICE_OR_PROFILE_OWNER_DISALLOWED";
    public static final String REQUIRED_APP_MANAGED_USER = "android.app.REQUIRED_APP_MANAGED_USER";
    public static final String REQUIRED_APP_MANAGED_DEVICE =
            "android.app.REQUIRED_APP_MANAGED_DEVICE";
    public static final String REQUIRED_APP_MANAGED_PROFILE =
            "android.app.REQUIRED_APP_MANAGED_PROFILE";
    public static final int RESET_PASSWORD_REQUIRE_ENTRY = 0x0001;
    public static final int RESET_PASSWORD_DO_NOT_ASK_CREDENTIALS_ON_BOOT = 0x0002;
    public static final int FLAG_EVICT_CREDENTIAL_ENCRYPTION_KEY = 1;
    public static final int WIPE_EXTERNAL_STORAGE = 0x0001;
    public static final int WIPE_RESET_PROTECTION_DATA = 0x0002;
    public static final int WIPE_EUICC = 0x0004;
    public static final int WIPE_SILENTLY = 0x0008;
    public static final int ENCRYPTION_STATUS_UNSUPPORTED = 0;
    public static final int ENCRYPTION_STATUS_INACTIVE = 1;
    public static final int ENCRYPTION_STATUS_ACTIVATING = 2;
    public static final int ENCRYPTION_STATUS_ACTIVE = 3;
    public static final int ENCRYPTION_STATUS_ACTIVE_DEFAULT_KEY = 4;
    public static final int ENCRYPTION_STATUS_ACTIVE_PER_USER = 5;
    public static final String ACTION_START_ENCRYPTION
            = "android.app.action.START_ENCRYPTION";
    public static final String ACTION_CHECK_POLICY_COMPLIANCE =
            "android.app.action.CHECK_POLICY_COMPLIANCE";
    public static final String ACTION_PROVISIONING_COMPLETED =
            "android.app.action.PROVISIONING_COMPLETED";
    public static final String EXTRA_PROVISIONING_ACTION =
            "android.app.extra.PROVISIONING_ACTION";
    public static final String ACTION_SHOW_NEW_USER_DISCLAIMER =
            "android.app.action.SHOW_NEW_USER_DISCLAIMER";
    public static final int KEYGUARD_DISABLE_FEATURES_NONE = 0;
    public static final int KEYGUARD_DISABLE_WIDGETS_ALL = 1 << 0;
    public static final int KEYGUARD_DISABLE_SECURE_CAMERA = 1 << 1;
    public static final int KEYGUARD_DISABLE_SECURE_NOTIFICATIONS = 1 << 2;
    public static final int KEYGUARD_DISABLE_UNREDACTED_NOTIFICATIONS = 1 << 3;
    public static final int KEYGUARD_DISABLE_TRUST_AGENTS = 1 << 4;
    public static final int KEYGUARD_DISABLE_FINGERPRINT = 1 << 5;
    public static final int KEYGUARD_DISABLE_REMOTE_INPUT = 1 << 6;
    public static final int KEYGUARD_DISABLE_FACE = 1 << 7;
    public static final int KEYGUARD_DISABLE_IRIS = 1 << 8;
    public static final int KEYGUARD_DISABLE_SHORTCUTS_ALL = 1 << 9;
    public static final int KEYGUARD_DISABLE_BIOMETRICS =
            DevicePolicyManager.KEYGUARD_DISABLE_FACE
            | DevicePolicyManager.KEYGUARD_DISABLE_IRIS
            | DevicePolicyManager.KEYGUARD_DISABLE_FINGERPRINT;
    public static final int KEYGUARD_DISABLE_FEATURES_ALL = 0x7fffffff;
    public static final int NON_ORG_OWNED_PROFILE_KEYGUARD_FEATURES_AFFECT_OWNER =
            DevicePolicyManager.KEYGUARD_DISABLE_TRUST_AGENTS
            | DevicePolicyManager.KEYGUARD_DISABLE_BIOMETRICS;
    public static final int ORG_OWNED_PROFILE_KEYGUARD_FEATURES_PARENT_ONLY =
            DevicePolicyManager.KEYGUARD_DISABLE_SECURE_CAMERA
                    | DevicePolicyManager.KEYGUARD_DISABLE_SECURE_NOTIFICATIONS
                    | DevicePolicyManager.KEYGUARD_DISABLE_SHORTCUTS_ALL;
    public static final int PROFILE_KEYGUARD_FEATURES_AFFECT_OWNER =
            DevicePolicyManager.NON_ORG_OWNED_PROFILE_KEYGUARD_FEATURES_AFFECT_OWNER
                    | DevicePolicyManager.ORG_OWNED_PROFILE_KEYGUARD_FEATURES_PARENT_ONLY;
    public static final int ERROR_VPN_PACKAGE_NOT_FOUND = 1;
    public static final int SKIP_SETUP_WIZARD = 0x0001;
    public static final int MAKE_USER_EPHEMERAL = 0x0002;
    public static final int MAKE_USER_DEMO = 0x0004;
    public static final int LEAVE_ALL_SYSTEM_APPS_ENABLED = 0x0010;
    public static final int ERROR_PACKAGE_NAME_NOT_FOUND = 1;

    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.SOURCE)
    public static @interface OperationSafetyReason {
    }

    public static String operationSafetyReasonToString(int reason) {
        switch (reason) {
        case OPERATION_SAFETY_REASON_NONE: return "OPERATION_SAFETY_REASON_NONE";
        case OPERATION_SAFETY_REASON_DRIVING_DISTRACTION: return "OPERATION_SAFETY_REASON_DRIVING_DISTRACTION";
        default: return Integer.toString(reason);
        }
    }

    public static boolean isValidOperationSafetyReason(int reason) {
        return reason == OPERATION_SAFETY_REASON_DRIVING_DISTRACTION;
    }

    private static SecurityException noAdmin(ComponentName admin) {
        return new SecurityException("No active admin " + admin);
    }

    public boolean isAdminActive(ComponentName admin) { return false; }
    public boolean isAdminActiveAsUser(ComponentName admin, int userId) { return false; }
    public boolean isRemovingAdmin(ComponentName admin, int userId) { return false; }
    public List<ComponentName> getActiveAdmins() { return null; }
    public List<ComponentName> getActiveAdminsAsUser(int userId) { return null; }
    public void removeActiveAdmin(ComponentName admin) {}
    public boolean hasGrantedPolicy(ComponentName admin, int usesPolicy) { throw noAdmin(admin); }
    public boolean isDeviceOwnerApp(String packageName) { return false; }
    public boolean isProfileOwnerApp(String packageName) { return false; }
    public boolean isActivePasswordSufficient() { return true; }
    public int getPasswordComplexity() { return PASSWORD_COMPLEXITY_NONE; }
    public int getStorageEncryptionStatus() { return ENCRYPTION_STATUS_UNSUPPORTED; }
    public int getStorageEncryptionStatus(int userHandle) { return ENCRYPTION_STATUS_UNSUPPORTED; }
    public int setStorageEncryption(ComponentName admin, boolean encrypt) { throw noAdmin(admin); }
    public void wipeData(int flags) { throw new SecurityException("No active admin owned by uid"); }
    /* AOSP bindDeviceAdminServiceAsUser(): only a device or profile owner may bind (DevicePolicyManagerService);
     * there is none, so as for the other owner-only calls */
    public boolean bindDeviceAdminServiceAsUser(ComponentName admin, android.content.Intent serviceIntent, android.content.ServiceConnection conn, int flags, android.os.UserHandle targetUser) { throw noAdmin(admin); }

    public boolean getCameraDisabled(ComponentName admin) { return false; }
    /* AOSP DevicePolicyManager with mService == null (no device policy service): these calls do nothing */
    public void lockNow() { lockNow(0); }
    public void lockNow(int flags) {}
    public void clearDeviceOwnerApp(String packageName) {}
    public String[] getAccountTypesWithManagementDisabled() { return null; }
    public String[] getAccountTypesWithManagementDisabledAsUser(int userId) { return null; }
    public void setCameraDisabled(ComponentName admin, boolean disabled) { throw noAdmin(admin); }
    public int getKeyguardDisabledFeatures(ComponentName admin) { return KEYGUARD_DISABLE_FEATURES_NONE; }
    public void setKeyguardDisabledFeatures(ComponentName admin, int which) { throw noAdmin(admin); }
    public int getMaximumFailedPasswordsForWipe(ComponentName admin) { return 0; }
    public void setMaximumFailedPasswordsForWipe(ComponentName admin, int num) { throw noAdmin(admin); }
    public long getMaximumTimeToLock(ComponentName admin) { return 0L; }
    public void setMaximumTimeToLock(ComponentName admin, long timeMs) { throw noAdmin(admin); }
    public long getPasswordExpiration(ComponentName admin) { return 0L; }
    public long getPasswordExpirationTimeout(ComponentName admin) { return 0L; }
    public void setPasswordExpirationTimeout(ComponentName admin, long timeout) { throw noAdmin(admin); }
    public int getPasswordHistoryLength(ComponentName admin) { return 0; }
    public void setPasswordHistoryLength(ComponentName admin, int length) { throw noAdmin(admin); }
    public int getPasswordQuality(ComponentName admin) { return PASSWORD_QUALITY_UNSPECIFIED; }
    public void setPasswordQuality(ComponentName admin, int quality) { throw noAdmin(admin); }
    public int getPasswordMinimumLength(ComponentName admin) { return 0; }
    public int getPasswordMinimumLetters(ComponentName admin) { return 0; }
    public int getPasswordMinimumLowerCase(ComponentName admin) { return 0; }
    public int getPasswordMinimumNonLetter(ComponentName admin) { return 0; }
    public int getPasswordMinimumNumeric(ComponentName admin) { return 0; }
    public int getPasswordMinimumSymbols(ComponentName admin) { return 0; }
    public int getPasswordMinimumUpperCase(ComponentName admin) { return 0; }
    public void setPasswordMinimumLength(ComponentName admin, int length) { throw noAdmin(admin); }
    public void setPasswordMinimumLetters(ComponentName admin, int length) { throw noAdmin(admin); }
    public void setPasswordMinimumLowerCase(ComponentName admin, int length) { throw noAdmin(admin); }
    public void setPasswordMinimumNonLetter(ComponentName admin, int length) { throw noAdmin(admin); }
    public void setPasswordMinimumNumeric(ComponentName admin, int length) { throw noAdmin(admin); }
    public void setPasswordMinimumSymbols(ComponentName admin, int length) { throw noAdmin(admin); }
    public void setPasswordMinimumUpperCase(ComponentName admin, int length) { throw noAdmin(admin); }

	/* AOSP DevicePolicyManager with mService == null (no device policy service on the desktop): the defaults of the mService == null paths */
	public void acknowledgeDeviceCompliant() { }
	public void addCrossProfileIntentFilter(android.content.ComponentName p0, android.content.IntentFilter p1, int p2) { }
	public boolean addCrossProfileWidgetProvider(android.content.ComponentName p0, String p1) { return false; }
	public void addPersistentPreferredActivity(android.content.ComponentName p0, android.content.IntentFilter p1, android.content.ComponentName p2) { }
	public void addUserRestriction(android.content.ComponentName p0, String p1) { }
	public void addUserRestrictionGlobally(String p0) { }
	public boolean canAdminGrantSensorsPermissions() { return false; }
	public boolean canUsbDataSignalingBeDisabled() { return false; }
	public void clearCrossProfileIntentFilters(android.content.ComponentName p0) { }
	public void clearPackagePersistentPreferredActivities(android.content.ComponentName p0, String p1) { }
	public void clearProfileOwner(android.content.ComponentName p0) { }
	public boolean clearResetPasswordToken(android.content.ComponentName p0) { return false; }
	public void clearUserRestriction(android.content.ComponentName p0, String p1) { }
	public android.content.Intent createAdminSupportIntent(String p0) { return null; }
	public android.os.UserHandle createAndManageUser(android.content.ComponentName p0, String p1, android.content.ComponentName p2, android.os.PersistableBundle p3, int p4) { return null; }
	public int enableSystemApp(android.content.ComponentName p0, android.content.Intent p1) { return 0; }
	public void enableSystemApp(android.content.ComponentName p0, String p1) { }
	public java.util.Set<java.lang.String> getAffiliationIds(android.content.ComponentName p0) { return java.util.Collections.emptySet(); }
	public java.util.Set<java.lang.String> getAlwaysOnVpnLockdownWhitelist(android.content.ComponentName p0) { return null; }
	public String getAlwaysOnVpnPackage(android.content.ComponentName p0) { return null; }
	public int getAppFunctionsPolicy() { return 0; }
	public android.os.Bundle getApplicationRestrictions(android.content.ComponentName p0, String p1) { return null; }
	public String getApplicationRestrictionsManagingPackage(android.content.ComponentName p0) { return null; }
	public boolean getAutoTimeEnabled(android.content.ComponentName p0) { return false; }
	public int getAutoTimePolicy() { return 0; }
	public boolean getAutoTimeRequired() { return false; }
	public boolean getAutoTimeZoneEnabled(android.content.ComponentName p0) { return false; }
	public int getAutoTimeZonePolicy() { return 0; }
	public java.util.List<android.os.UserHandle> getBindDeviceAdminTargetUsers(android.content.ComponentName p0) { return java.util.Collections.emptyList(); }
	public boolean getBluetoothContactSharingDisabled(android.content.ComponentName p0) { return true; }
	public String getCertInstallerPackage(android.content.ComponentName p0) throws java.lang.SecurityException { return null; }
	public int getContentProtectionPolicy(android.content.ComponentName p0) { return 0; }
	public java.util.Set<java.lang.String> getCrossProfileCalendarPackages(android.content.ComponentName p0) { return java.util.Collections.emptySet(); }
	public boolean getCrossProfileCallerIdDisabled(android.content.ComponentName p0) { return false; }
	public boolean getCrossProfileContactsSearchDisabled(android.content.ComponentName p0) { return false; }
	public java.util.Set<java.lang.String> getCrossProfilePackages(android.content.ComponentName p0) { return java.util.Collections.emptySet(); }
	public java.util.List<java.lang.String> getCrossProfileWidgetProviders(android.content.ComponentName p0) { return java.util.Collections.emptyList(); }
	public int getCurrentFailedPasswordAttempts() { return 0; }
	public java.util.List<java.lang.String> getDelegatePackages(android.content.ComponentName p0, String p1) { return java.util.Collections.emptyList(); }
	public java.util.List<java.lang.String> getDelegatedScopes(android.content.ComponentName p0, String p1) { return java.util.Collections.emptyList(); }
	public CharSequence getDeviceOwnerLockScreenInfo() { return null; }
	public String getDevicePolicyManagementRoleHolderPackage() { return null; }
	public CharSequence getEndUserSessionMessage(android.content.ComponentName p0) { return null; }
	public String getEnrollmentSpecificId() { return null; }
	public String getGlobalPrivateDnsHost(android.content.ComponentName p0) { return null; }
	public int getGlobalPrivateDnsMode(android.content.ComponentName p0) { return 0; }
	public java.util.List<byte[]> getInstalledCaCerts(android.content.ComponentName p0) { return java.util.Collections.emptyList(); }
	public java.util.List<java.lang.String> getKeepUninstalledPackages(android.content.ComponentName p0) { return null; }
	public java.util.Map<java.lang.Integer,java.util.Set<java.lang.String>> getKeyPairGrants(String p0) { return null; }
	public int getLockTaskFeatures(android.content.ComponentName p0) { return 0; }
	public String[] getLockTaskPackages(android.content.ComponentName p0) { return new String[0]; }
	public CharSequence getLongSupportMessage(android.content.ComponentName p0) { return null; }
	public long getManagedProfileMaximumTimeOff(android.content.ComponentName p0) { return 0; }
	public java.util.List<java.lang.String> getMeteredDataDisabledPackages(android.content.ComponentName p0) { return new java.util.ArrayList<>(); }
	public int getMinimumRequiredWifiSecurityLevel() { return 0; }
	public int getMtePolicy() { return MTE_NOT_CONTROLLED_BY_POLICY; }
	public int getNearbyAppStreamingPolicy() { return 0; }
	public int getNearbyNotificationStreamingPolicy() { return 0; }
	public int getOrganizationColor(android.content.ComponentName p0) { return 0; }
	public CharSequence getOrganizationName(android.content.ComponentName p0) { return null; }
	public android.app.admin.DevicePolicyManager getParentProfileInstance(android.content.ComponentName p0) { return null; }
	public int getPasswordMaximumLength(int p0) { return MAX_PASSWORD_LENGTH; }
	public int getPermissionGrantState(android.content.ComponentName p0, String p1, String p2) { return 0; }
	public int getPermissionPolicy(android.content.ComponentName p0) { return 0; }
	public java.util.List<java.lang.String> getPermittedAccessibilityServices(android.content.ComponentName p0) { return null; }
	public java.util.List<java.lang.String> getPermittedCrossProfileNotificationListeners(android.content.ComponentName p0) { return null; }
	public java.util.List<java.lang.String> getPermittedInputMethods(android.content.ComponentName p0) { return null; }
	public int getPersonalAppsSuspendedReasons(android.content.ComponentName p0) { return 0; }
	public int getRequiredPasswordComplexity() { return 0; }
	public long getRequiredStrongAuthTimeout(android.content.ComponentName p0) { return 0; }
	public boolean getScreenCaptureDisabled(android.content.ComponentName p0) { return false; }
	public java.util.List<android.os.UserHandle> getSecondaryUsers(android.content.ComponentName p0) { return java.util.Collections.emptyList(); }
	public CharSequence getShortSupportMessage(android.content.ComponentName p0) { return null; }
	public CharSequence getStartUserSessionMessage(android.content.ComponentName p0) { return null; }
	public boolean getStorageEncryption(android.content.ComponentName p0) { return false; }
	public java.util.Set<java.lang.Integer> getSubscriptionIds() { return new java.util.HashSet<>(); }
	public android.os.PersistableBundle getTransferOwnershipBundle() { return null; }
	public java.util.List<android.os.PersistableBundle> getTrustAgentConfiguration(android.content.ComponentName p0, android.content.ComponentName p1) { return java.util.Collections.emptyList(); }
	public java.util.List<java.lang.String> getUserControlDisabledPackages(android.content.ComponentName p0) { return java.util.Collections.emptyList(); }
	public android.os.Bundle getUserRestrictions(android.content.ComponentName p0) { return null; }
	public android.os.Bundle getUserRestrictionsGlobally() { return null; }
	public String getWifiMacAddress(android.content.ComponentName p0) { return null; }
	public boolean grantKeyPairToApp(android.content.ComponentName p0, String p1, String p2) { return false; }
	public boolean grantKeyPairToWifiAuth(String p0) { return false; }
	public boolean hasCaCertInstalled(android.content.ComponentName p0, byte[] p1) { return false; }
	public boolean hasKeyPair(String p0) { return false; }
	public boolean hasLockdownAdminConfiguredNetworks(android.content.ComponentName p0) { return false; }
	public boolean installCaCert(android.content.ComponentName p0, byte[] p1) { return false; }
	public boolean installExistingPackage(android.content.ComponentName p0, String p1) { return false; }
	public boolean installKeyPair(android.content.ComponentName p0, java.security.PrivateKey p1, java.security.cert.Certificate p2, String p3) { return false; }
	public boolean installKeyPair(android.content.ComponentName p0, java.security.PrivateKey p1, java.security.cert.Certificate[] p2, String p3, int p4) { return false; }
	public boolean installKeyPair(android.content.ComponentName p0, java.security.PrivateKey p1, java.security.cert.Certificate[] p2, String p3, boolean p4) { return false; }
	public boolean isActivePasswordSufficientForDeviceRequirement() { return false; }
	public boolean isAffiliatedUser() { return false; }
	public boolean isAlwaysOnVpnLockdownEnabled(android.content.ComponentName p0) { return false; }
	public boolean isApplicationHidden(android.content.ComponentName p0, String p1) { return false; }
	public boolean isBackupServiceEnabled(android.content.ComponentName p0) { return false; }
	public boolean isCallerApplicationRestrictionsManagingPackage() { return false; }
	public boolean isCommonCriteriaModeEnabled(android.content.ComponentName p0) { return false; }
	public boolean isComplianceAcknowledgementRequired() { return false; }
	public boolean isDeviceFinanced() { return false; }
	public boolean isDeviceIdAttestationSupported() { return false; }
	public boolean isEphemeralUser(android.content.ComponentName p0) { return false; }
	public boolean isKeyPairGrantedToWifiAuth(String p0) { return false; }
	public boolean isLockTaskPermitted(String p0) { return false; }
	public boolean isLogoutEnabled() { return false; }
	public boolean isManagedProfile(android.content.ComponentName p0) { return false; }
	public boolean isMasterVolumeMuted(android.content.ComponentName p0) { return false; }
	public static boolean isMtePolicyEnforced() { return false; }
	public boolean isNetworkLoggingEnabled(android.content.ComponentName p0) { return false; }
	public boolean isOrganizationOwnedDeviceWithManagedProfile() { return false; }
	public boolean isOverrideApnEnabled(android.content.ComponentName p0) { return false; }
	public boolean isPackageSuspended(android.content.ComponentName p0, String p1) throws android.content.pm.PackageManager.NameNotFoundException { return false; }
	public boolean isPreferentialNetworkServiceEnabled() { return false; }
	public boolean isProvisioningAllowed(String p0) { return false; }
	public boolean isResetPasswordTokenActive(android.content.ComponentName p0) { return false; }
	public boolean isSafeOperation(int p0) { return false; }
	public boolean isSecurityLoggingEnabled(android.content.ComponentName p0) { return false; }
	public boolean isStatusBarDisabled() { return false; }
	public boolean isUninstallBlocked(android.content.ComponentName p0, String p1) { return false; }
	public boolean isUniqueDeviceAttestationSupported() { return false; }
	public boolean isUsbDataSignalingEnabled() { return true; }
	public boolean isUsingUnifiedPassword(android.content.ComponentName p0) { return true; }
	public java.util.List<android.os.UserHandle> listForegroundAffiliatedUsers() { return java.util.Collections.emptyList(); }
	public int logoutUser(android.content.ComponentName p0) { return 0; }
	public void reboot(android.content.ComponentName p0) { }
	public boolean removeCrossProfileWidgetProvider(android.content.ComponentName p0, String p1) { return false; }
	public boolean removeKeyPair(android.content.ComponentName p0, String p1) { return false; }
	public boolean removeOverrideApn(android.content.ComponentName p0, int p1) { return false; }
	public boolean removeUser(android.content.ComponentName p0, android.os.UserHandle p1) { return false; }
	public boolean requestBugreport(android.content.ComponentName p0) { return false; }
	public boolean resetPassword(String p0, int p1) { return false; }
	public boolean resetPasswordWithToken(android.content.ComponentName p0, String p1, byte[] p2, int p3) { return false; }
	public boolean revokeKeyPairFromApp(android.content.ComponentName p0, String p1, String p2) { return false; }
	public boolean revokeKeyPairFromWifiAuth(String p0) { return false; }
	public void setAccountManagementDisabled(android.content.ComponentName p0, String p1, boolean p2) { }
	public void setAffiliationIds(android.content.ComponentName p0, java.util.Set<java.lang.String> p1) { }
	public void setAlwaysOnVpnPackage(android.content.ComponentName p0, String p1, boolean p2) throws android.content.pm.PackageManager.NameNotFoundException { }
	public void setAlwaysOnVpnPackage(android.content.ComponentName p0, String p1, boolean p2, java.util.Set<java.lang.String> p3) throws android.content.pm.PackageManager.NameNotFoundException { }
	public void setAppFunctionsPolicy(int p0) { }
	public boolean setApplicationHidden(android.content.ComponentName p0, String p1, boolean p2) { return false; }
	public void setApplicationRestrictions(android.content.ComponentName p0, String p1, android.os.Bundle p2) { }
	public void setApplicationRestrictionsManagingPackage(android.content.ComponentName p0, String p1) throws android.content.pm.PackageManager.NameNotFoundException { }
	public void setAutoTimeEnabled(android.content.ComponentName p0, boolean p1) { }
	public void setAutoTimePolicy(int p0) { }
	public void setAutoTimeRequired(android.content.ComponentName p0, boolean p1) { }
	public void setAutoTimeZoneEnabled(android.content.ComponentName p0, boolean p1) { }
	public void setAutoTimeZonePolicy(int p0) { }
	public void setBackupServiceEnabled(android.content.ComponentName p0, boolean p1) { }
	public void setBluetoothContactSharingDisabled(android.content.ComponentName p0, boolean p1) { }
	public void setCertInstallerPackage(android.content.ComponentName p0, String p1) throws java.lang.SecurityException { }
	public void setCommonCriteriaModeEnabled(android.content.ComponentName p0, boolean p1) { }
	public void setConfiguredNetworksLockdownState(android.content.ComponentName p0, boolean p1) { }
	public void setContentProtectionPolicy(android.content.ComponentName p0, int p1) { }
	public void setCrossProfileCalendarPackages(android.content.ComponentName p0, java.util.Set<java.lang.String> p1) { }
	public void setCrossProfileCallerIdDisabled(android.content.ComponentName p0, boolean p1) { }
	public void setCrossProfileContactsSearchDisabled(android.content.ComponentName p0, boolean p1) { }
	public void setCrossProfilePackages(android.content.ComponentName p0, java.util.Set<java.lang.String> p1) { }
	public void setCrossProfileWidgetProviders(java.util.Set<java.lang.String> p0) { }
	public void setDefaultDialerApplication(String p0) { }
	public void setDefaultSmsApplication(android.content.ComponentName p0, String p1) { }
	public void setDelegatedScopes(android.content.ComponentName p0, String p1, java.util.List<java.lang.String> p2) { }
	public void setDeviceOwnerLockScreenInfo(android.content.ComponentName p0, CharSequence p1) { }
	public void setEndUserSessionMessage(android.content.ComponentName p0, CharSequence p1) { }
	public int setGlobalPrivateDnsModeOpportunistic(android.content.ComponentName p0) { return 0; }
	public int setGlobalPrivateDnsModeSpecifiedHost(android.content.ComponentName p0, String p1) { return 0; }
	public void setGlobalSetting(android.content.ComponentName p0, String p1, String p2) { }
	public void setKeepUninstalledPackages(android.content.ComponentName p0, java.util.List<java.lang.String> p1) { }
	public boolean setKeyPairCertificate(android.content.ComponentName p0, String p1, java.util.List<java.security.cert.Certificate> p2, boolean p3) { return false; }
	public boolean setKeyguardDisabled(android.content.ComponentName p0, boolean p1) { return false; }
	public void setLocationEnabled(android.content.ComponentName p0, boolean p1) { }
	public void setLockTaskFeatures(android.content.ComponentName p0, int p1) { }
	public void setLockTaskPackages(android.content.ComponentName p0, String[] p1) throws java.lang.SecurityException { }
	public void setLogoutEnabled(android.content.ComponentName p0, boolean p1) { }
	public void setLongSupportMessage(android.content.ComponentName p0, CharSequence p1) { }
	public void setManagedProfileMaximumTimeOff(android.content.ComponentName p0, long p1) { }
	public void setMasterVolumeMuted(android.content.ComponentName p0, boolean p1) { }
	public java.util.List<java.lang.String> setMeteredDataDisabledPackages(android.content.ComponentName p0, java.util.List<java.lang.String> p1) { return java.util.Collections.emptyList(); }
	public void setMinimumRequiredWifiSecurityLevel(int p0) { }
	public void setMtePolicy(int p0) { }
	public void setNearbyAppStreamingPolicy(int p0) { }
	public void setNearbyNotificationStreamingPolicy(int p0) { }
	public void setNetworkLoggingEnabled(android.content.ComponentName p0, boolean p1) { }
	public void setOrganizationColor(android.content.ComponentName p0, int p1) { }
	public void setOrganizationId(String p0) { }
	public void setOrganizationName(android.content.ComponentName p0, CharSequence p1) { }
	public void setOverrideApnsEnabled(android.content.ComponentName p0, boolean p1) { }
	public String[] setPackagesSuspended(android.content.ComponentName p0, String[] p1, boolean p2) { return null; }
	public boolean setPermissionGrantState(android.content.ComponentName p0, String p1, String p2, int p3) { return false; }
	public void setPermissionPolicy(android.content.ComponentName p0, int p1) { }
	public boolean setPermittedAccessibilityServices(android.content.ComponentName p0, java.util.List<java.lang.String> p1) { return false; }
	public boolean setPermittedCrossProfileNotificationListeners(android.content.ComponentName p0, java.util.List<java.lang.String> p1) { return false; }
	public boolean setPermittedInputMethods(android.content.ComponentName p0, java.util.List<java.lang.String> p1) { return false; }
	public void setPersonalAppsSuspended(android.content.ComponentName p0, boolean p1) { }
	public void setPreferentialNetworkServiceEnabled(boolean p0) { }
	public void setProfileEnabled(android.content.ComponentName p0) { }
	public void setProfileName(android.content.ComponentName p0, String p1) { }
	public void setRecommendedGlobalProxy(android.content.ComponentName p0, android.net.ProxyInfo p1) { }
	public void setRequiredPasswordComplexity(int p0) { }
	public void setRequiredStrongAuthTimeout(android.content.ComponentName p0, long p1) { }
	public boolean setResetPasswordToken(android.content.ComponentName p0, byte[] p1) { return false; }
	public void setRestrictionsProvider(android.content.ComponentName p0, android.content.ComponentName p1) { }
	public void setScreenCaptureDisabled(android.content.ComponentName p0, boolean p1) { }
	public void setSecureSetting(android.content.ComponentName p0, String p1, String p2) { }
	public void setSecurityLoggingEnabled(android.content.ComponentName p0, boolean p1) { }
	public void setShortSupportMessage(android.content.ComponentName p0, CharSequence p1) { }
	public void setStartUserSessionMessage(android.content.ComponentName p0, CharSequence p1) { }
	public boolean setStatusBarDisabled(android.content.ComponentName p0, boolean p1) { return false; }
	public void setSystemSetting(android.content.ComponentName p0, String p1, String p2) { }
	public boolean setTime(android.content.ComponentName p0, long p1) { return false; }
	public boolean setTimeZone(android.content.ComponentName p0, String p1) { return false; }
	public void setTrustAgentConfiguration(android.content.ComponentName p0, android.content.ComponentName p1, android.os.PersistableBundle p2) { }
	public void setUninstallBlocked(android.content.ComponentName p0, String p1, boolean p2) { }
	public void setUsbDataSignalingEnabled(boolean p0) { }
	public void setUserControlDisabledPackages(android.content.ComponentName p0, java.util.List<java.lang.String> p1) { }
	public void setUserIcon(android.content.ComponentName p0, android.graphics.Bitmap p1) { }
	public int startUserInBackground(android.content.ComponentName p0, android.os.UserHandle p1) { return 0; }
	public int stopUser(android.content.ComponentName p0, android.os.UserHandle p1) { return 0; }
	public boolean switchUser(android.content.ComponentName p0, android.os.UserHandle p1) { return false; }
	public void transferOwnership(android.content.ComponentName p0, android.content.ComponentName p1, android.os.PersistableBundle p2) { }
	public void uninstallAllUserCaCerts(android.content.ComponentName p0) { }
	public void uninstallCaCert(android.content.ComponentName p0, byte[] p1) { }
	public void wipeData(int p0, CharSequence p1) { }
	public void wipeDevice(int p0) { }
}
