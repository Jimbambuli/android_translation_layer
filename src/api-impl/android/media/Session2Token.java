package android.media;

/* AOSP frameworks/base/media/java/android/media/Session2Token.java (public API surface; no backing service in ATL, bodies follow the AOSP no-service path) */
public final class Session2Token implements android.os.Parcelable {
    /* AOSP Parcelable.Creator; no service ever hands ATL a parcel of this type */
    public static final android.os.Parcelable.Creator<android.media.Session2Token> CREATOR = new android.os.Parcelable.Creator<android.media.Session2Token>() {
        public android.media.Session2Token createFromParcel(android.os.Parcel source) { return null; }
        public android.media.Session2Token[] newArray(int size) { return new android.media.Session2Token[size]; }
    };
    @Deprecated public Session2Token(android.content.Context arg0, android.content.ComponentName arg1)  {}
    @Deprecated public int describeContents() { return 0; }
    @Deprecated public android.os.Bundle getExtras() { return null; }
    @Deprecated public String getPackageName() { return null; }
    @Deprecated public String getServiceName() { return null; }
    @Deprecated public int getType() { return 0; }
    @Deprecated public int getUid() { return 0; }
    @Deprecated public void writeToParcel(android.os.Parcel arg0, int arg1) {  }
    @Deprecated public static final int TYPE_SESSION = 0; // 0x0;
    @Deprecated public static final int TYPE_SESSION_SERVICE = 1; // 0x1;
}
