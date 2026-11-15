package android.media.tv.interactive;

/* AOSP frameworks/base/media/java/android/media/tv/interactive/TvInteractiveAppManager.java (public API surface; no backing service in ATL, bodies follow the AOSP no-service path) */
public final class TvInteractiveAppManager {
    public java.util.List<android.media.tv.interactive.AppLinkInfo> getAppLinkInfoList() { return null; }
    public java.util.List<android.media.tv.interactive.TvInteractiveAppServiceInfo> getTvInteractiveAppServiceList() { return null; }
    public void registerAppLinkInfo(String arg0, android.media.tv.interactive.AppLinkInfo arg1) {  }
    public void registerCallback(java.util.concurrent.Executor arg0, android.media.tv.interactive.TvInteractiveAppManager.TvInteractiveAppCallback arg1) {  }
    public void sendAppLinkCommand(String arg0, android.os.Bundle arg1) {  }
    public void unregisterAppLinkInfo(String arg0, android.media.tv.interactive.AppLinkInfo arg1) {  }
    public void unregisterCallback(android.media.tv.interactive.TvInteractiveAppManager.TvInteractiveAppCallback arg0) {  }
    public static final String ACTION_APP_LINK_COMMAND = "android.media.tv.interactive.action.APP_LINK_COMMAND";
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
    public static final String INTENT_KEY_BI_INTERACTIVE_APP_TYPE = "bi_interactive_app_type";
    public static final String INTENT_KEY_BI_INTERACTIVE_APP_URI = "bi_interactive_app_uri";
    public static final String INTENT_KEY_CHANNEL_URI = "channel_uri";
    public static final String INTENT_KEY_COMMAND_TYPE = "command_type";
    public static final String INTENT_KEY_INTERACTIVE_APP_SERVICE_ID = "interactive_app_id";
    public static final String INTENT_KEY_TV_INPUT_ID = "tv_input_id";
    public static final int INTERACTIVE_APP_STATE_ERROR = 3; // 0x3;
    public static final int INTERACTIVE_APP_STATE_RUNNING = 2; // 0x2;
    public static final int INTERACTIVE_APP_STATE_STOPPED = 1; // 0x1;
    public static final int SERVICE_STATE_ERROR = 4; // 0x4;
    public static final int SERVICE_STATE_PREPARING = 2; // 0x2;
    public static final int SERVICE_STATE_READY = 3; // 0x3;
    public static final int SERVICE_STATE_UNREALIZED = 1; // 0x1;
    public static final int TELETEXT_APP_STATE_ERROR = 3; // 0x3;
    public static final int TELETEXT_APP_STATE_HIDE = 2; // 0x2;
    public static final int TELETEXT_APP_STATE_SHOW = 1; // 0x1;

    public abstract static class TvInteractiveAppCallback {
        public TvInteractiveAppCallback()  {}
        public void onInteractiveAppServiceAdded(String arg0) {  }
        public void onInteractiveAppServiceRemoved(String arg0) {  }
        public void onInteractiveAppServiceUpdated(String arg0) {  }
        public void onTvInteractiveAppServiceStateChanged(String arg0, int arg1, int arg2, int arg3) {  }
    }
}
