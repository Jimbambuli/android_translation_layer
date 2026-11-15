package android.media.quality;

/* AOSP frameworks/base/media/java/android/media/quality/SoundProfile.java (public API surface; no TV service in ATL, bodies follow the AOSP no-service path) */
public final class SoundProfile implements android.os.Parcelable {
    /* AOSP Parcelable.Creator; no service ever hands ATL a parcel of this type */
    public static final android.os.Parcelable.Creator<android.media.quality.SoundProfile> CREATOR = new android.os.Parcelable.Creator<android.media.quality.SoundProfile>() {
        public android.media.quality.SoundProfile createFromParcel(android.os.Parcel source) { return null; }
        public android.media.quality.SoundProfile[] newArray(int size) { return new android.media.quality.SoundProfile[size]; }
    };
    public int describeContents() { return 0; }
    public String getInputId() { return null; }
    public String getName() { return null; }
    public String getPackageName() { return null; }
    public android.os.PersistableBundle getParameters() { return null; }
    public String getProfileId() { return null; }
    public int getProfileType() { return 0; }
    public void writeToParcel(android.os.Parcel arg0, int arg1) {  }
    public static final int ERROR_DUPLICATE = 2; // 0x2;
    public static final int ERROR_INVALID_ARGUMENT = 3; // 0x3;
    public static final int ERROR_NOT_ALLOWLISTED = 4; // 0x4;
    public static final int ERROR_NO_PERMISSION = 1; // 0x1;
    public static final int ERROR_UNKNOWN = 0; // 0x0;
    public static final int TYPE_APPLICATION = 2; // 0x2;
    public static final int TYPE_SYSTEM = 1; // 0x1;

    public static final class Builder {
        public Builder(String arg0)  {}
        public Builder(android.media.quality.SoundProfile arg0)  {}
        public android.media.quality.SoundProfile build() { return null; }
        public android.media.quality.SoundProfile.Builder setParameters(android.os.PersistableBundle arg0) { return this; }
    }
}
