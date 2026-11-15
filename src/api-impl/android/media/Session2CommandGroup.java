package android.media;

/* AOSP frameworks/base/media/java/android/media/Session2CommandGroup.java (public API surface; no backing service in ATL, bodies follow the AOSP no-service path) */
public final class Session2CommandGroup implements android.os.Parcelable {
    /* AOSP Parcelable.Creator; no service ever hands ATL a parcel of this type */
    public static final android.os.Parcelable.Creator<android.media.Session2CommandGroup> CREATOR = new android.os.Parcelable.Creator<android.media.Session2CommandGroup>() {
        public android.media.Session2CommandGroup createFromParcel(android.os.Parcel source) { return null; }
        public android.media.Session2CommandGroup[] newArray(int size) { return new android.media.Session2CommandGroup[size]; }
    };
    @Deprecated public int describeContents() { return 0; }
    @Deprecated public java.util.Set<android.media.Session2Command> getCommands() { return null; }
    @Deprecated public boolean hasCommand(android.media.Session2Command arg0) { return false; }
    @Deprecated public boolean hasCommand(int arg0) { return false; }
    @Deprecated public void writeToParcel(android.os.Parcel arg0, int arg1) {  }

    public static final class Builder {
        @Deprecated public Builder()  {}
        @Deprecated public Builder(android.media.Session2CommandGroup arg0)  {}
        @Deprecated public android.media.Session2CommandGroup.Builder addCommand(android.media.Session2Command arg0) { return this; }
        @Deprecated public android.media.Session2CommandGroup build() { return null; }
        @Deprecated public android.media.Session2CommandGroup.Builder removeCommand(android.media.Session2Command arg0) { return this; }
    }
}
