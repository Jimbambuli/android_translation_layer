package android.net.wifi.rtt;

/* AOSP frameworks/base/net/wifi/rtt/ResponderLocation.java (public API surface; no backing network service in ATL, bodies follow the AOSP no-service path) */
public final class ResponderLocation implements android.os.Parcelable {
    public int describeContents() { return 0; }
    public double getAltitude() { return 0.0; }
    public int getAltitudeType() { return 0; }
    public double getAltitudeUncertainty() { return 0.0; }
    public java.util.List<android.net.MacAddress> getColocatedBssids() { return null; }
    public int getDatum() { return 0; }
    public int getExpectedToMove() { return 0; }
    public double getFloorNumber() { return 0.0; }
    public double getHeightAboveFloorMeters() { return 0.0; }
    public double getHeightAboveFloorUncertaintyMeters() { return 0.0; }
    public double getLatitude() { return 0.0; }
    public double getLatitudeUncertainty() { return 0.0; }
    public int getLciVersion() { return 0; }
    public double getLongitude() { return 0.0; }
    public double getLongitudeUncertainty() { return 0.0; }
    public String getMapImageMimeType() { return null; }
    public android.net.Uri getMapImageUri() { return null; }
    public boolean getRegisteredLocationAgreementIndication() { return false; }
    public boolean isLciSubelementValid() { return false; }
    public boolean isZaxisSubelementValid() { return false; }
    public android.location.Address toCivicLocationAddress() { return null; }
    public android.util.SparseArray<java.lang.String> toCivicLocationSparseArray() { return null; }
    public android.location.Location toLocation() { return null; }
    public void writeToParcel(android.os.Parcel arg0, int arg1) {  }
    public static final int ALTITUDE_FLOORS = 2; // 0x2;
    public static final int ALTITUDE_METERS = 1; // 0x1;
    public static final int ALTITUDE_UNDEFINED = 0; // 0x0;
    public static final int DATUM_NAD83_MLLW = 3; // 0x3;
    public static final int DATUM_NAD83_NAV88 = 2; // 0x2;
    public static final int DATUM_UNDEFINED = 0; // 0x0;
    public static final int DATUM_WGS84 = 1; // 0x1;
    public static final int LCI_VERSION_1 = 1; // 0x1;
    public static final int LOCATION_FIXED = 0; // 0x0;
    public static final int LOCATION_MOVEMENT_UNKNOWN = 2; // 0x2;
    public static final int LOCATION_RESERVED = 3; // 0x3;
    public static final int LOCATION_VARIABLE = 1; // 0x1;
    /* AOSP Parcelable.Creator; no service hands ATL a parcel of this type */
    public static final android.os.Parcelable.Creator<android.net.wifi.rtt.ResponderLocation> CREATOR = new android.os.Parcelable.Creator<android.net.wifi.rtt.ResponderLocation>() {
        public android.net.wifi.rtt.ResponderLocation createFromParcel(android.os.Parcel source) { return null; }
        public android.net.wifi.rtt.ResponderLocation[] newArray(int size) { return new android.net.wifi.rtt.ResponderLocation[size]; }
    };
}
