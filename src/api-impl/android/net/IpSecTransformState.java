package android.net;

/* AOSP frameworks/base/net/IpSecTransformState.java (public API surface; no backing network service in ATL, bodies follow the AOSP no-service path) */
public final class IpSecTransformState implements android.os.Parcelable {
    public int describeContents() { return 0; }
    public long getByteCount() { return 0L; }
    public long getPacketCount() { return 0L; }
    public byte[] getReplayBitmap() { return null; }
    public long getRxHighestSequenceNumber() { return 0L; }
    public long getTimestampMillis() { return 0L; }
    public long getTxHighestSequenceNumber() { return 0L; }
    public void writeToParcel(android.os.Parcel arg0, int arg1) {  }

    public static final class Builder {
        public Builder()  {}
        public android.net.IpSecTransformState build() { return null; }
        public android.net.IpSecTransformState.Builder setByteCount(long arg0) { return this; }
        public android.net.IpSecTransformState.Builder setPacketCount(long arg0) { return this; }
        public android.net.IpSecTransformState.Builder setReplayBitmap(byte[] arg0) { return this; }
        public android.net.IpSecTransformState.Builder setRxHighestSequenceNumber(long arg0) { return this; }
        public android.net.IpSecTransformState.Builder setTimestampMillis(long arg0) { return this; }
        public android.net.IpSecTransformState.Builder setTxHighestSequenceNumber(long arg0) { return this; }
    }
}
