package android.media.projection;

/* AOSP frameworks/base/media/java/android/media/projection/MediaProjectionAppContent.java (public API surface; no backing service in ATL, bodies follow the AOSP no-service path) */
public final class MediaProjectionAppContent implements android.os.Parcelable {
    /* AOSP Parcelable.Creator; no service ever hands ATL a parcel of this type */
    public static final android.os.Parcelable.Creator<android.media.projection.MediaProjectionAppContent> CREATOR = new android.os.Parcelable.Creator<android.media.projection.MediaProjectionAppContent>() {
        public android.media.projection.MediaProjectionAppContent createFromParcel(android.os.Parcel source) { return null; }
        public android.media.projection.MediaProjectionAppContent[] newArray(int size) { return new android.media.projection.MediaProjectionAppContent[size]; }
    };
    public MediaProjectionAppContent(android.graphics.Bitmap arg0, CharSequence arg1, int arg2)  {}
    public int describeContents() { return 0; }
    public void writeToParcel(android.os.Parcel arg0, int arg1) {  }
}
