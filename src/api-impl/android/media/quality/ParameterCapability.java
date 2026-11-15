package android.media.quality;

/* AOSP frameworks/base/media/java/android/media/quality/ParameterCapability.java (public API surface; no TV service in ATL, bodies follow the AOSP no-service path) */
public final class ParameterCapability implements android.os.Parcelable {
    /* AOSP Parcelable.Creator; no service ever hands ATL a parcel of this type */
    public static final android.os.Parcelable.Creator<android.media.quality.ParameterCapability> CREATOR = new android.os.Parcelable.Creator<android.media.quality.ParameterCapability>() {
        public android.media.quality.ParameterCapability createFromParcel(android.os.Parcel source) { return null; }
        public android.media.quality.ParameterCapability[] newArray(int size) { return new android.media.quality.ParameterCapability[size]; }
    };
    public int describeContents() { return 0; }
    public android.os.Bundle getCapabilities() { return null; }
    public String getParameterName() { return null; }
    public int getParameterType() { return 0; }
    public boolean isSupported() { return false; }
    public void writeToParcel(android.os.Parcel arg0, int arg1) {  }
    public static final String CAPABILITY_DEFAULT = "default";
    public static final String CAPABILITY_ENUM = "enum";
    public static final String CAPABILITY_MAX = "max";
    public static final String CAPABILITY_MIN = "min";
    public static final int TYPE_DOUBLE = 3; // 0x3;
    public static final int TYPE_INT = 1; // 0x1;
    public static final int TYPE_LONG = 2; // 0x2;
    public static final int TYPE_NONE = 0; // 0x0;
    public static final int TYPE_STRING = 4; // 0x4;
}
