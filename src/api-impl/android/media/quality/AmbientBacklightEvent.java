package android.media.quality;

/* AOSP frameworks/base/media/java/android/media/quality/AmbientBacklightEvent.java (public API surface; no TV service in ATL, bodies follow the AOSP no-service path) */
public final class AmbientBacklightEvent implements android.os.Parcelable {
    public AmbientBacklightEvent(int arg0, android.media.quality.AmbientBacklightMetadata arg1)  {}
    public int describeContents() { return 0; }
    public int getEventType() { return 0; }
    public android.media.quality.AmbientBacklightMetadata getMetadata() { return null; }
    public void writeToParcel(android.os.Parcel arg0, int arg1) {  }
    public static final int AMBIENT_BACKLIGHT_EVENT_DISABLED = 2; // 0x2;
    public static final int AMBIENT_BACKLIGHT_EVENT_ENABLED = 1; // 0x1;
    public static final int AMBIENT_BACKLIGHT_EVENT_INTERRUPTED = 4; // 0x4;
    public static final int AMBIENT_BACKLIGHT_EVENT_METADATA_AVAILABLE = 3; // 0x3;
}
