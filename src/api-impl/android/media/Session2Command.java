package android.media;

/* AOSP frameworks/base/media/java/android/media/Session2Command.java (public API surface; no backing service in ATL, bodies follow the AOSP no-service path) */
public final class Session2Command implements android.os.Parcelable {
    @Deprecated public Session2Command(int arg0)  {}
    @Deprecated public Session2Command(String arg0, android.os.Bundle arg1)  {}
    @Deprecated public int describeContents() { return 0; }
    @Deprecated public int getCommandCode() { return 0; }
    @Deprecated public String getCustomAction() { return null; }
    @Deprecated public android.os.Bundle getCustomExtras() { return null; }
    @Deprecated public void writeToParcel(android.os.Parcel arg0, int arg1) {  }
    @Deprecated public static final int COMMAND_CODE_CUSTOM = 0; // 0x0;

    public static final class Result {
        @Deprecated public Result(int arg0, android.os.Bundle arg1)  {}
        @Deprecated public int getResultCode() { return 0; }
        @Deprecated public android.os.Bundle getResultData() { return null; }
        @Deprecated public static final int RESULT_ERROR_UNKNOWN_ERROR = -1; // 0xffffffff;
        @Deprecated public static final int RESULT_INFO_SKIPPED = 1; // 0x1;
        @Deprecated public static final int RESULT_SUCCESS = 0; // 0x0;
    }
}
