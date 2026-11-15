package android.media.quality;

/* AOSP frameworks/base/media/java/android/media/quality/AmbientBacklightSettings.java (public API surface; no TV service in ATL, bodies follow the AOSP no-service path) */
public final class AmbientBacklightSettings implements android.os.Parcelable {
    public AmbientBacklightSettings(int arg0, int arg1, int arg2, int arg3, int arg4, boolean arg5, int arg6)  {}
    public int describeContents() { return 0; }
    public int getColorFormat() { return 0; }
    public int getHorizontalZonesCount() { return 0; }
    public int getMaxFps() { return 0; }
    public int getSource() { return 0; }
    public int getThreshold() { return 0; }
    public int getVerticalZonesCount() { return 0; }
    public boolean isLetterboxOmitted() { return false; }
    public void writeToParcel(android.os.Parcel arg0, int arg1) {  }
    public static final int SOURCE_AUDIO = 1; // 0x1;
    public static final int SOURCE_AUDIO_VIDEO = 3; // 0x3;
    public static final int SOURCE_NONE = 0; // 0x0;
    public static final int SOURCE_VIDEO = 2; // 0x2;
}
