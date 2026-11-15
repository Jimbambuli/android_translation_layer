package android.net;

/* AOSP frameworks/base/net/TelephonyNetworkSpecifier.java (public API surface; no backing network service in ATL, bodies follow the AOSP no-service path) */
public final class TelephonyNetworkSpecifier extends android.net.NetworkSpecifier implements android.os.Parcelable {
    public int describeContents() { return 0; }
    public int getSubscriptionId() { return 0; }
    public void writeToParcel(android.os.Parcel arg0, int arg1) {  }

    public static final class Builder {
        public Builder()  {}
        public android.net.TelephonyNetworkSpecifier build() { return null; }
        public android.net.TelephonyNetworkSpecifier.Builder setSubscriptionId(int arg0) { return this; }
    }
    /* AOSP Parcelable.Creator; no service hands ATL a parcel of this type */
    public static final android.os.Parcelable.Creator<android.net.TelephonyNetworkSpecifier> CREATOR = new android.os.Parcelable.Creator<android.net.TelephonyNetworkSpecifier>() {
        public android.net.TelephonyNetworkSpecifier createFromParcel(android.os.Parcel source) { return null; }
        public android.net.TelephonyNetworkSpecifier[] newArray(int size) { return new android.net.TelephonyNetworkSpecifier[size]; }
    };
}
