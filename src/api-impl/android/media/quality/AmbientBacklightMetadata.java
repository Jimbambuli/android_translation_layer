package android.media.quality;

/* AOSP frameworks/base/media/java/android/media/quality/AmbientBacklightMetadata.java (public API surface; no TV service in ATL, bodies follow the AOSP no-service path) */
public final class AmbientBacklightMetadata implements android.os.Parcelable {
    /* AOSP Parcelable.Creator; no service ever hands ATL a parcel of this type */
    public static final android.os.Parcelable.Creator<android.media.quality.AmbientBacklightMetadata> CREATOR = new android.os.Parcelable.Creator<android.media.quality.AmbientBacklightMetadata>() {
        public android.media.quality.AmbientBacklightMetadata createFromParcel(android.os.Parcel source) { return null; }
        public android.media.quality.AmbientBacklightMetadata[] newArray(int size) { return new android.media.quality.AmbientBacklightMetadata[size]; }
    };
    public AmbientBacklightMetadata(String arg0, int arg1, int arg2, int arg3, int arg4, int arg5, int[] arg6)  {}
    public int describeContents() { return 0; }
    public int getColorFormat() { return 0; }
    public int getCompressionAlgorithm() { return 0; }
    public int getHorizontalZonesCount() { return 0; }
    public String getPackageName() { return null; }
    public int getSource() { return 0; }
    public int getVerticalZonesCount() { return 0; }
    public int[] getZoneColors() { return null; }
    public void writeToParcel(android.os.Parcel arg0, int arg1) {  }
    public static final int ALGORITHM_NONE = 0; // 0x0;
    public static final int ALGORITHM_RLE = 1; // 0x1;
}
