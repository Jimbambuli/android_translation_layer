package android.media.tv;

/* AOSP media/java/android/android/media/tv/TvInputManager.java (public API surface; no TV service in ATL, bodies follow the AOSP no-service path) */
public final class TvInputManager {
    public TvInputManager() {}

    public java.util.List<android.media.tv.TvContentRating> getBlockedRatings() { return new java.util.ArrayList<>(); }
    public int getInputState(String arg0) { return INPUT_STATE_DISCONNECTED; }
    public android.media.tv.TvInputInfo getTvInputInfo(String arg0) { return null; }
    public java.util.List<android.media.tv.TvInputInfo> getTvInputList() { return new java.util.ArrayList<>(); }
    public boolean isParentalControlsEnabled() { return false; }
    public boolean isRatingBlocked(android.media.tv.TvContentRating arg0) { return false; }
    public void registerCallback(android.media.tv.TvInputManager.TvInputCallback arg0, android.os.Handler arg1) {  }
    public void unregisterCallback(android.media.tv.TvInputManager.TvInputCallback arg0) {  }
    public void updateTvInputInfo(android.media.tv.TvInputInfo arg0) {  }
    public static final String ACTION_BLOCKED_RATINGS_CHANGED = "android.media.tv.action.BLOCKED_RATINGS_CHANGED";
    public static final String ACTION_PARENTAL_CONTROLS_ENABLED_CHANGED = "android.media.tv.action.PARENTAL_CONTROLS_ENABLED_CHANGED";
    public static final String ACTION_QUERY_CONTENT_RATING_SYSTEMS = "android.media.tv.action.QUERY_CONTENT_RATING_SYSTEMS";
    public static final String ACTION_SETUP_INPUTS = "android.media.tv.action.SETUP_INPUTS";
    public static final String ACTION_VIEW_RECORDING_SCHEDULES = "android.media.tv.action.VIEW_RECORDING_SCHEDULES";
    public static final int BROADCAST_INFO_STREAM_EVENT = 5; // 0x5;
    public static final int BROADCAST_INFO_TYPE_COMMAND = 7; // 0x7;
    public static final int BROADCAST_INFO_TYPE_DSMCC = 6; // 0x6;
    public static final int BROADCAST_INFO_TYPE_PES = 4; // 0x4;
    public static final int BROADCAST_INFO_TYPE_SECTION = 3; // 0x3;
    public static final int BROADCAST_INFO_TYPE_TABLE = 2; // 0x2;
    public static final int BROADCAST_INFO_TYPE_TIMELINE = 8; // 0x8;
    public static final int BROADCAST_INFO_TYPE_TS = 1; // 0x1;
    public static final int INPUT_STATE_CONNECTED = 0; // 0x0;
    public static final int INPUT_STATE_CONNECTED_STANDBY = 1; // 0x1;
    public static final int INPUT_STATE_DISCONNECTED = 2; // 0x2;
    public static final String META_DATA_CONTENT_RATING_SYSTEMS = "android.media.tv.metadata.CONTENT_RATING_SYSTEMS";
    public static final int RECORDING_ERROR_INSUFFICIENT_SPACE = 1; // 0x1;
    public static final int RECORDING_ERROR_RESOURCE_BUSY = 2; // 0x2;
    public static final int RECORDING_ERROR_UNKNOWN = 0; // 0x0;
    public static final String SESSION_DATA_KEY_AD_BUFFER = "ad_buffer";
    public static final String SESSION_DATA_KEY_AD_RESPONSE = "ad_response";
    public static final String SESSION_DATA_KEY_BROADCAST_INFO_RESPONSE = "broadcast_info_response";
    public static final String SESSION_DATA_KEY_CHANNEL_URI = "channel_uri";
    public static final String SESSION_DATA_KEY_TRACKS = "tracks";
    public static final String SESSION_DATA_KEY_TRACK_ID = "track_id";
    public static final String SESSION_DATA_KEY_TRACK_TYPE = "track_type";
    public static final String SESSION_DATA_KEY_TV_MESSAGE_TYPE = "tv_message_type";
    public static final String SESSION_DATA_KEY_VIDEO_UNAVAILABLE_REASON = "video_unavailable_reason";
    public static final String SESSION_DATA_TYPE_AD_BUFFER_CONSUMED = "ad_buffer_consumed";
    public static final String SESSION_DATA_TYPE_AD_RESPONSE = "ad_response";
    public static final String SESSION_DATA_TYPE_BROADCAST_INFO_RESPONSE = "broadcast_info_response";
    public static final String SESSION_DATA_TYPE_TRACKS_CHANGED = "tracks_changed";
    public static final String SESSION_DATA_TYPE_TRACK_SELECTED = "track_selected";
    public static final String SESSION_DATA_TYPE_TUNED = "tuned";
    public static final String SESSION_DATA_TYPE_TV_MESSAGE = "tv_message";
    public static final String SESSION_DATA_TYPE_VIDEO_AVAILABLE = "video_available";
    public static final String SESSION_DATA_TYPE_VIDEO_UNAVAILABLE = "video_unavailable";
    public static final int SIGNAL_STRENGTH_LOST = 1; // 0x1;
    public static final int SIGNAL_STRENGTH_STRONG = 3; // 0x3;
    public static final int SIGNAL_STRENGTH_WEAK = 2; // 0x2;
    public static final long TIME_SHIFT_INVALID_TIME = -9223372036854775808L; // 0x8000000000000000L;
    public static final int TIME_SHIFT_MODE_AUTO = 4; // 0x4;
    public static final int TIME_SHIFT_MODE_LOCAL = 2; // 0x2;
    public static final int TIME_SHIFT_MODE_NETWORK = 3; // 0x3;
    public static final int TIME_SHIFT_MODE_OFF = 1; // 0x1;
    public static final int TIME_SHIFT_STATUS_AVAILABLE = 3; // 0x3;
    public static final int TIME_SHIFT_STATUS_UNAVAILABLE = 2; // 0x2;
    public static final int TIME_SHIFT_STATUS_UNKNOWN = 0; // 0x0;
    public static final int TIME_SHIFT_STATUS_UNSUPPORTED = 1; // 0x1;
    public static final long TV_MESSAGE_GROUP_ID_NONE = -1L; // 0xffffffffffffffffL;
    public static final String TV_MESSAGE_KEY_GROUP_ID = "android.media.tv.TvInputManager.group_id";
    public static final String TV_MESSAGE_KEY_RAW_DATA = "android.media.tv.TvInputManager.raw_data";
    public static final String TV_MESSAGE_KEY_STREAM_ID = "android.media.tv.TvInputManager.stream_id";
    public static final String TV_MESSAGE_KEY_SUBTYPE = "android.media.tv.TvInputManager.subtype";
    public static final String TV_MESSAGE_SUBTYPE_CC_608E = "CTA 608-E";
    public static final String TV_MESSAGE_SUBTYPE_WATERMARKING_A335 = "ATSC A/335";
    public static final int TV_MESSAGE_TYPE_CLOSED_CAPTION = 2; // 0x2;
    public static final int TV_MESSAGE_TYPE_OTHER = 1000; // 0x3e8;
    public static final int TV_MESSAGE_TYPE_WATERMARK = 1; // 0x1;
    public static final int VIDEO_UNAVAILABLE_REASON_AUDIO_ONLY = 4; // 0x4;
    public static final int VIDEO_UNAVAILABLE_REASON_BUFFERING = 3; // 0x3;
    public static final int VIDEO_UNAVAILABLE_REASON_CAS_BLACKOUT = 16; // 0x10;
    public static final int VIDEO_UNAVAILABLE_REASON_CAS_CARD_INVALID = 15; // 0xf;
    public static final int VIDEO_UNAVAILABLE_REASON_CAS_CARD_MUTE = 14; // 0xe;
    public static final int VIDEO_UNAVAILABLE_REASON_CAS_INSUFFICIENT_OUTPUT_PROTECTION = 7; // 0x7;
    public static final int VIDEO_UNAVAILABLE_REASON_CAS_LICENSE_EXPIRED = 10; // 0xa;
    public static final int VIDEO_UNAVAILABLE_REASON_CAS_NEED_ACTIVATION = 11; // 0xb;
    public static final int VIDEO_UNAVAILABLE_REASON_CAS_NEED_PAIRING = 12; // 0xc;
    public static final int VIDEO_UNAVAILABLE_REASON_CAS_NO_CARD = 13; // 0xd;
    public static final int VIDEO_UNAVAILABLE_REASON_CAS_PVR_RECORDING_NOT_ALLOWED = 8; // 0x8;
    public static final int VIDEO_UNAVAILABLE_REASON_CAS_REBOOTING = 17; // 0x11;
    public static final int VIDEO_UNAVAILABLE_REASON_CAS_UNKNOWN = 18; // 0x12;
    public static final int VIDEO_UNAVAILABLE_REASON_INSUFFICIENT_RESOURCE = 6; // 0x6;
    public static final int VIDEO_UNAVAILABLE_REASON_NOT_CONNECTED = 5; // 0x5;
    public static final int VIDEO_UNAVAILABLE_REASON_STOPPED = 19; // 0x13;
    public static final int VIDEO_UNAVAILABLE_REASON_TUNING = 1; // 0x1;
    public static final int VIDEO_UNAVAILABLE_REASON_UNKNOWN = 0; // 0x0;
    public static final int VIDEO_UNAVAILABLE_REASON_WEAK_SIGNAL = 2; // 0x2;

    public abstract static class TvInputCallback {
        public TvInputCallback()  {}
        public void onInputAdded(String arg0) {  }
        public void onInputRemoved(String arg0) {  }
        public void onInputStateChanged(String arg0, int arg1) {  }
        public void onInputUpdated(String arg0) {  }
        public void onTvInputInfoUpdated(android.media.tv.TvInputInfo arg0) {  }
    }
}
