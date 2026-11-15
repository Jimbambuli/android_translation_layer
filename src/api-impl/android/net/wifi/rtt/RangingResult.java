package android.net.wifi.rtt;

/* AOSP frameworks/base/net/wifi/rtt/RangingResult.java (public API surface; no backing network service in ATL, bodies follow the AOSP no-service path) */
public final class RangingResult implements android.os.Parcelable {
    public int describeContents() { return 0; }
    public int get80211azInitiatorTxLtfRepetitionsCount() { return 0; }
    public int get80211azNumberOfRxSpatialStreams() { return 0; }
    public int get80211azNumberOfTxSpatialStreams() { return 0; }
    public int get80211azResponderTxLtfRepetitionsCount() { return 0; }
    public int getDistanceMm() { return 0; }
    public int getDistanceStdDevMm() { return 0; }
    public byte[] getLci() { return null; }
    public byte[] getLcr() { return null; }
    public android.net.MacAddress getMacAddress() { return null; }
    public long getMaxTimeBetweenNtbMeasurementsMicros() { return 0L; }
    public int getMeasurementBandwidth() { return 0; }
    public int getMeasurementChannelFrequencyMHz() { return 0; }
    public long getMinTimeBetweenNtbMeasurementsMicros() { return 0L; }
    public int getNumAttemptedMeasurements() { return 0; }
    public int getNumSuccessfulMeasurements() { return 0; }
    public long getPasnComebackAfterMillis() { return 0L; }
    public byte[] getPasnComebackCookie() { return null; }
    public android.net.wifi.aware.PeerHandle getPeerHandle() { return null; }
    public long getRangingTimestampMillis() { return 0L; }
    public int getRssi() { return 0; }
    public int getSecureHeLtfProtocolVersion() { return 0; }
    public int getStatus() { return 0; }
    public android.net.wifi.rtt.ResponderLocation getUnverifiedResponderLocation() { return null; }
    public boolean is80211azNtbMeasurement() { return false; }
    public boolean is80211mcMeasurement() { return false; }
    public boolean isRangingAuthenticated() { return false; }
    public boolean isRangingFrameProtected() { return false; }
    public boolean isSecureHeLtfEnabled() { return false; }
    public void writeToParcel(android.os.Parcel arg0, int arg1) {  }
    public static final int STATUS_FAIL = 1; // 0x1;
    public static final int STATUS_RESPONDER_DOES_NOT_SUPPORT_IEEE80211MC = 2; // 0x2;
    public static final int STATUS_SUCCESS = 0; // 0x0;
    public static final int UNSPECIFIED = -1; // 0xffffffff;

    public static final class Builder {
        public Builder()  {}
        public android.net.wifi.rtt.RangingResult build() { return null; }
        public android.net.wifi.rtt.RangingResult.Builder set80211azInitiatorTxLtfRepetitionsCount(int arg0) { return null; }
        public android.net.wifi.rtt.RangingResult.Builder set80211azNtbMeasurement(boolean arg0) { return null; }
        public android.net.wifi.rtt.RangingResult.Builder set80211azNumberOfRxSpatialStreams(int arg0) { return null; }
        public android.net.wifi.rtt.RangingResult.Builder set80211azNumberOfTxSpatialStreams(int arg0) { return null; }
        public android.net.wifi.rtt.RangingResult.Builder set80211azResponderTxLtfRepetitionsCount(int arg0) { return null; }
        public android.net.wifi.rtt.RangingResult.Builder set80211mcMeasurement(boolean arg0) { return null; }
        public android.net.wifi.rtt.RangingResult.Builder setDistanceMm(int arg0) { return this; }
        public android.net.wifi.rtt.RangingResult.Builder setDistanceStdDevMm(int arg0) { return this; }
        public android.net.wifi.rtt.RangingResult.Builder setLci(byte[] arg0) { return this; }
        public android.net.wifi.rtt.RangingResult.Builder setLcr(byte[] arg0) { return this; }
        public android.net.wifi.rtt.RangingResult.Builder setMacAddress(android.net.MacAddress arg0) { return this; }
        public android.net.wifi.rtt.RangingResult.Builder setMaxTimeBetweenNtbMeasurementsMicros(long arg0) { return this; }
        public android.net.wifi.rtt.RangingResult.Builder setMeasurementBandwidth(int arg0) { return this; }
        public android.net.wifi.rtt.RangingResult.Builder setMeasurementChannelFrequencyMHz(int arg0) { return this; }
        public android.net.wifi.rtt.RangingResult.Builder setMinTimeBetweenNtbMeasurementsMicros(long arg0) { return this; }
        public android.net.wifi.rtt.RangingResult.Builder setNumAttemptedMeasurements(int arg0) { return this; }
        public android.net.wifi.rtt.RangingResult.Builder setNumSuccessfulMeasurements(int arg0) { return this; }
        public android.net.wifi.rtt.RangingResult.Builder setPasnComebackAfterMillis(long arg0) { return this; }
        public android.net.wifi.rtt.RangingResult.Builder setPasnComebackCookie(byte[] arg0) { return this; }
        public android.net.wifi.rtt.RangingResult.Builder setPeerHandle(android.net.wifi.aware.PeerHandle arg0) { return this; }
        public android.net.wifi.rtt.RangingResult.Builder setRangingAuthenticated(boolean arg0) { return this; }
        public android.net.wifi.rtt.RangingResult.Builder setRangingFrameProtected(boolean arg0) { return this; }
        public android.net.wifi.rtt.RangingResult.Builder setRangingTimestampMillis(long arg0) { return this; }
        public android.net.wifi.rtt.RangingResult.Builder setRssi(int arg0) { return this; }
        public android.net.wifi.rtt.RangingResult.Builder setSecureHeLtfEnabled(boolean arg0) { return this; }
        public android.net.wifi.rtt.RangingResult.Builder setSecureHeLtfProtocolVersion(int arg0) { return this; }
        public android.net.wifi.rtt.RangingResult.Builder setStatus(int arg0) { return this; }
        public android.net.wifi.rtt.RangingResult.Builder setUnverifiedResponderLocation(android.net.wifi.rtt.ResponderLocation arg0) { return this; }
    }
}
