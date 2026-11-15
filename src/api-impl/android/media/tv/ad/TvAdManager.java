package android.media.tv.ad;

/* AOSP frameworks/base/media/java/android/media/tv/ad/TvAdManager.java (public API surface; no backing service in ATL, bodies follow the AOSP no-service path) */
public final class TvAdManager {
    public java.util.List<android.media.tv.ad.TvAdServiceInfo> getTvAdServiceList() { return null; }
    public void registerCallback(java.util.concurrent.Executor arg0, android.media.tv.ad.TvAdManager.TvAdServiceCallback arg1) {  }
    public void sendAppLinkCommand(String arg0, android.os.Bundle arg1) {  }
    public void unregisterCallback(android.media.tv.ad.TvAdManager.TvAdServiceCallback arg0) {  }
    public static final String ACTION_APP_LINK_COMMAND = "android.media.tv.ad.action.APP_LINK_COMMAND";
    public static final String APP_LINK_KEY_BACK_URI = "back_uri";
    public static final String APP_LINK_KEY_CLASS_NAME = "class_name";
    public static final String APP_LINK_KEY_COMMAND_TYPE = "command_type";
    public static final String APP_LINK_KEY_PACKAGE_NAME = "package_name";
    public static final String APP_LINK_KEY_SERVICE_ID = "service_id";
    public static final int ERROR_BLOCKED = 5; // 0x5;
    public static final int ERROR_ENCRYPTED = 6; // 0x6;
    public static final int ERROR_NONE = 0; // 0x0;
    public static final int ERROR_NOT_SUPPORTED = 2; // 0x2;
    public static final int ERROR_RESOURCE_UNAVAILABLE = 4; // 0x4;
    public static final int ERROR_UNKNOWN = 1; // 0x1;
    public static final int ERROR_UNKNOWN_CHANNEL = 7; // 0x7;
    public static final int ERROR_WEAK_SIGNAL = 3; // 0x3;
    public static final String INTENT_KEY_AD_SERVICE_ID = "ad_service_id";
    public static final String INTENT_KEY_CHANNEL_URI = "channel_uri";
    public static final String INTENT_KEY_COMMAND_TYPE = "command_type";
    public static final String INTENT_KEY_TV_INPUT_ID = "tv_input_id";
    public static final String SESSION_DATA_KEY_AD_BUFFER = "ad_buffer";
    public static final String SESSION_DATA_KEY_AD_REQUEST = "ad_request";
    public static final String SESSION_DATA_KEY_BROADCAST_INFO_REQUEST = "broadcast_info_request";
    public static final String SESSION_DATA_KEY_REQUEST_ID = "request_id";
    public static final String SESSION_DATA_TYPE_AD_BUFFER_READY = "ad_buffer_ready";
    public static final String SESSION_DATA_TYPE_AD_REQUEST = "ad_request";
    public static final String SESSION_DATA_TYPE_BROADCAST_INFO_REQUEST = "broadcast_info_request";
    public static final String SESSION_DATA_TYPE_REMOVE_BROADCAST_INFO_REQUEST = "remove_broadcast_info_request";
    public static final int SESSION_STATE_ERROR = 3; // 0x3;
    public static final int SESSION_STATE_RUNNING = 2; // 0x2;
    public static final int SESSION_STATE_STOPPED = 1; // 0x1;

    public abstract static class TvAdServiceCallback {
        public TvAdServiceCallback()  {}
        public void onAdServiceAdded(String arg0) {  }
        public void onAdServiceRemoved(String arg0) {  }
        public void onAdServiceUpdated(String arg0) {  }
    }
}
