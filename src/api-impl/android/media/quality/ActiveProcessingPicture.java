package android.media.quality;

/* AOSP frameworks/base/media/java/android/media/quality/ActiveProcessingPicture.java (public API surface; no TV service in ATL, bodies follow the AOSP no-service path) */
public final class ActiveProcessingPicture implements android.os.Parcelable {
    /* AOSP Parcelable.Creator; no service ever hands ATL a parcel of this type */
    public static final android.os.Parcelable.Creator<android.media.quality.ActiveProcessingPicture> CREATOR = new android.os.Parcelable.Creator<android.media.quality.ActiveProcessingPicture>() {
        public android.media.quality.ActiveProcessingPicture createFromParcel(android.os.Parcel source) { return null; }
        public android.media.quality.ActiveProcessingPicture[] newArray(int size) { return new android.media.quality.ActiveProcessingPicture[size]; }
    };
    public ActiveProcessingPicture(int arg0, String arg1)  {}
    public int describeContents() { return 0; }
    public int getId() { return 0; }
    public String getProfileId() { return null; }
    public void writeToParcel(android.os.Parcel arg0, int arg1) {  }
}
