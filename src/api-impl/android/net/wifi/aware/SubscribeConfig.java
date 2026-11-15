package android.net.wifi.aware;

/* AOSP android/net/wifi/aware/SubscribeConfig.java */


import android.annotation.IntDef;
import android.annotation.IntRange;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.net.wifi.OuiKeyedData;
import android.net.wifi.ParcelUtil;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiAnnotations;
import android.net.wifi.WifiScanner;
import android.net.wifi.rtt.RangingRequest;
import android.net.wifi.rtt.ResponderConfig;
import android.net.wifi.util.HexEncoding;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;



import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Defines the configuration of an Aware subscribe session. Built using
 * {@link SubscribeConfig.Builder}. Subscribe is done using
 * {@link WifiAwareSession#subscribe(SubscribeConfig, DiscoverySessionCallback,
 * android.os.Handler)} or
 * {@link SubscribeDiscoverySession#updateSubscribe(SubscribeConfig)}.
 */
public final class SubscribeConfig implements Parcelable {
    
    /**
     * Defines a passive subscribe session - a subscribe session where
     * subscribe packets are not transmitted over-the-air and the device listens
     * and matches to transmitted publish packets. Configuration is done using
     * {@link SubscribeConfig.Builder#setSubscribeType(int)}.
     */
    public static final int SUBSCRIBE_TYPE_PASSIVE = 0;

    /**
     * Defines an active subscribe session - a subscribe session where
     * subscribe packets are transmitted over-the-air. Configuration is done
     * using {@link SubscribeConfig.Builder#setSubscribeType(int)}.
     */
    public static final int SUBSCRIBE_TYPE_ACTIVE = 1;

    private static final int AWARE_BAND_2_DISCOVERY_CHANNEL = 2437;
    private static final int MIN_RTT_BURST_SIZE = RangingRequest.getMinRttBurstSize();
    private static final int MAX_RTT_BURST_SIZE = RangingRequest.getMaxRttBurstSize();

    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    private final boolean mEnableInstantMode;

    private final int mBand;

    private final AwarePairingConfig mPairingConfig;

    private final boolean mIsSuspendable;
    private final List<OuiKeyedData> mVendorData;

    
    
    
    
    
    
    
    
    
    @Override
    public String toString() {
        return "SubscribeConfig [mServiceName='" + (mServiceName == null ? "<null>"
                : String.valueOf(HexEncoding.encode(mServiceName))) + ", mServiceName.length=" + (
                mServiceName == null ? 0 : mServiceName.length) + ", mServiceSpecificInfo='" + (
                (mServiceSpecificInfo == null) ? "<null>" : String.valueOf(
                        HexEncoding.encode(mServiceSpecificInfo)))
                + ", mServiceSpecificInfo.length=" + (mServiceSpecificInfo == null ? 0
                : mServiceSpecificInfo.length) + ", mMatchFilter="
                + (new TlvBufferUtils.TlvIterable(0, 1, mMatchFilter)).toString()
                + ", mMatchFilter.length=" + (mMatchFilter == null ? 0 : mMatchFilter.length)
                + ", mSubscribeType=" + mSubscribeType + ", mTtlSec=" + mTtlSec
                + ", mEnableTerminateNotification=" + mEnableTerminateNotification
                + ", mMinDistanceMm=" + mMinDistanceMm
                + ", mMinDistanceMmSet=" + mMinDistanceMmSet
                + ", mMaxDistanceMm=" + mMaxDistanceMm
                + ", mMaxDistanceMmSet=" + mMaxDistanceMmSet + "]"
                + ", mEnableInstantMode=" + mEnableInstantMode
                + ", mBand=" + mBand
                + ", mPairingConfig" + mPairingConfig
                + ", mIsSuspendable=" + mIsSuspendable
                + ", mVendorData=" + mVendorData + "]"
                + ", mPeriodicRangingInterval" + mPeriodicRangingInterval
                + ", mPeriodicRangingEnabled" + mPeriodicRangingEnabled
                + ", mRttBurstSize" + mRttBurstSize
                + ", mFrequencyMhz" + mFrequencyMhz
                + ", mCenterFrequency0Mhz" + mCenterFrequency0Mhz
                + ", mCenterFrequency1Mhz" + mCenterFrequency1Mhz
                + ", mPreamble" + mPreamble
                + ", mChannelWidth" + mChannelWidth;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByteArray(mServiceName);
        dest.writeByteArray(mServiceSpecificInfo);
        dest.writeByteArray(mMatchFilter);
        dest.writeInt(mSubscribeType);
        dest.writeInt(mTtlSec);
        dest.writeInt(mEnableTerminateNotification ? 1 : 0);
        dest.writeInt(mMinDistanceMm);
        dest.writeInt(mMinDistanceMmSet ? 1 : 0);
        dest.writeInt(mMaxDistanceMm);
        dest.writeInt(mMaxDistanceMmSet ? 1 : 0);
        dest.writeBoolean(mEnableInstantMode);
        dest.writeInt(mBand);
        dest.writeParcelable(mPairingConfig, flags);
        dest.writeBoolean(mIsSuspendable);
        dest.writeList(mVendorData);
        dest.writeInt(mPeriodicRangingInterval);
        dest.writeBoolean(mPeriodicRangingEnabled);
        dest.writeInt(mRttBurstSize);
        dest.writeInt(mFrequencyMhz);
        dest.writeInt(mCenterFrequency0Mhz);
        dest.writeInt(mCenterFrequency1Mhz);
        dest.writeInt(mPreamble);
        dest.writeInt(mChannelWidth);
    }

    @NonNull
    public static final Creator<SubscribeConfig> CREATOR = new Creator<SubscribeConfig>() {
        @Override
        public SubscribeConfig[] newArray(int size) {
            return new SubscribeConfig[size];
        }

        @Override
        public SubscribeConfig createFromParcel(Parcel in) {
            byte[] serviceName = in.createByteArray();
            byte[] ssi = in.createByteArray();
            byte[] matchFilter = in.createByteArray();
            int subscribeType = in.readInt();
            int ttlSec = in.readInt();
            boolean enableTerminateNotification = in.readInt() != 0;
            int minDistanceMm = in.readInt();
            boolean minDistanceMmSet = in.readInt() != 0;
            int maxDistanceMm = in.readInt();
            boolean maxDistanceMmSet = in.readInt() != 0;
            boolean enableInstantMode = in.readBoolean();
            int band = in.readInt();
            AwarePairingConfig pairingConfig = in.readParcelable(
                    AwarePairingConfig.class.getClassLoader());
            boolean isSuspendable = in.readBoolean();
            List<OuiKeyedData> vendorData = ParcelUtil.readOuiKeyedDataList(in);
            int rangingInterval = in.readInt();
            boolean enablePeriodicRanging = in.readBoolean();
            int burstSize = in.readInt();
            int frequencyMhz = in.readInt();
            int centerFrequency0Mhz = in.readInt();
            int centerFrequency1Mhz = in.readInt();
            int preamble = in.readInt();
            int channelWidth = in.readInt();

            return new SubscribeConfig(serviceName, ssi, matchFilter, subscribeType, ttlSec,
                    enableTerminateNotification, minDistanceMmSet, minDistanceMm, maxDistanceMmSet,
                    maxDistanceMm, enableInstantMode, band, pairingConfig, isSuspendable,
                    vendorData, rangingInterval, enablePeriodicRanging, burstSize,
                    frequencyMhz, centerFrequency0Mhz, centerFrequency1Mhz, preamble,
                    channelWidth);
        }
    };

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof SubscribeConfig)) {
            return false;
        }

        SubscribeConfig lhs = (SubscribeConfig) o;

        if (!(Arrays.equals(mServiceName, lhs.mServiceName) && Arrays.equals(
                mServiceSpecificInfo, lhs.mServiceSpecificInfo) && Arrays.equals(mMatchFilter,
                lhs.mMatchFilter) && mSubscribeType == lhs.mSubscribeType && mTtlSec == lhs.mTtlSec
                && mEnableTerminateNotification == lhs.mEnableTerminateNotification
                && mMinDistanceMmSet == lhs.mMinDistanceMmSet
                && mMaxDistanceMmSet == lhs.mMaxDistanceMmSet
                && mEnableInstantMode == lhs.mEnableInstantMode
                && mBand == lhs.mBand
                && mIsSuspendable == lhs.mIsSuspendable
                && Objects.equals(mVendorData, lhs.mVendorData)
                && mPeriodicRangingEnabled == lhs.mPeriodicRangingEnabled
                && mRttBurstSize == lhs.mRttBurstSize
                && mFrequencyMhz == lhs.mFrequencyMhz
                && mCenterFrequency0Mhz == lhs.mCenterFrequency0Mhz
                && mCenterFrequency1Mhz == lhs.mCenterFrequency1Mhz
                && mPreamble == lhs.mPreamble
                && mChannelWidth == lhs.mChannelWidth)) {
            return false;
        }

        if (mMinDistanceMmSet && mMinDistanceMm != lhs.mMinDistanceMm) {
            return false;
        }

        if (mMaxDistanceMmSet && mMaxDistanceMm != lhs.mMaxDistanceMm) {
            return false;
        }

        if (mPeriodicRangingEnabled && mPeriodicRangingInterval != lhs.mPeriodicRangingInterval) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(Arrays.hashCode(mServiceName),
                Arrays.hashCode(mServiceSpecificInfo), Arrays.hashCode(mMatchFilter),
                mSubscribeType, mTtlSec, mEnableTerminateNotification, mMinDistanceMmSet,
                mMaxDistanceMmSet, mEnableInstantMode, mBand,  mIsSuspendable, mVendorData,
                mPeriodicRangingEnabled, mRttBurstSize, mFrequencyMhz, mCenterFrequency0Mhz,
                mCenterFrequency1Mhz, mPreamble, mChannelWidth);

        if (mMinDistanceMmSet) {
            result = Objects.hash(result, mMinDistanceMm);
        }
        if (mMaxDistanceMmSet) {
            result = Objects.hash(result, mMaxDistanceMm);
        }
        if (mPeriodicRangingEnabled) {
            result = Objects.hash(result, mPeriodicRangingInterval);
        }

        return result;
    }

    
    /**
     * Check if instant mode is enabled for this subscribe session.
     * @see Builder#setInstantCommunicationModeEnabled(boolean, int)
     * @return true for enabled, false otherwise.
     */
    public boolean isInstantCommunicationModeEnabled() {
        return mEnableInstantMode;
    }

    /**
     * Check if enable instant mode on 5G for this subscribe session
     *
     * @see Builder#setInstantCommunicationModeEnabled(boolean, int)
     * @return If instant communication mode is not enabled will return {@link
     *     ScanResult#WIFI_BAND_24_GHZ} as default.
     */
    @WifiAwareManager.InstantModeBand
    public int getInstantCommunicationBand() {
        return mBand;
    }

    /**
     * Get the Aware Pairing config for this subscribe session
     * @see Builder#setPairingConfig(AwarePairingConfig)
     * @return A {@link AwarePairingConfig} specified in this config.
     */
    @Nullable
    public AwarePairingConfig getPairingConfig() {
        return mPairingConfig;
    }

    
    
    
    
    
    
    
    
    
    
    /**
     * Builder used to build {@link SubscribeConfig} objects.
     */
    public static final class Builder {
        private byte[] mServiceName;
        private byte[] mServiceSpecificInfo;
        private byte[] mMatchFilter;
        private int mSubscribeType = SUBSCRIBE_TYPE_PASSIVE;
        private int mTtlSec = 0;
        private boolean mEnableTerminateNotification = true;
        private boolean mMinDistanceMmSet = false;
        private int mMinDistanceMm;
        private boolean mMaxDistanceMmSet = false;
        private int mMaxDistanceMm;
        private boolean mEnableInstantMode;
        private int mBand = WifiScanner.WIFI_BAND_24_GHZ;
        private AwarePairingConfig mPairingConfig;
        private boolean mIsSuspendable = false;
        private @NonNull List<OuiKeyedData> mVendorData = Collections.emptyList();
        private boolean mPeriodicRangingEnabled = false;
        private int mPeriodicRangingInterval = PERIODIC_RANGING_INTERVAL_512TU;
        private int mRttBurstSize = RangingRequest.getDefaultRttBurstSize();
        private int mFrequencyMhz = AWARE_BAND_2_DISCOVERY_CHANNEL;
        private int mCenterFrequency0Mhz = 0;
        private int mCenterFrequency1Mhz = 0;
        private int mPreamble = ResponderConfig.PREAMBLE_HT;
        private int mChannelWidth = ResponderConfig.CHANNEL_WIDTH_20MHZ;

        /**
         * Specify the service name of the subscribe session. The actual on-air
         * value is a 6 byte hashed representation of this string.
         * <p>
         * The Service Name is a UTF-8 encoded string from 1 to 255 bytes in length.
         * The only acceptable single-byte UTF-8 symbols for a Service Name are alphanumeric
         * values (A-Z, a-z, 0-9), the hyphen ('-'), the period ('.') and the underscore ('_'). All
         * valid multi-byte UTF-8 characters are acceptable in a Service Name.
         * <p>
         * Must be called - an empty ServiceName is not valid.
         *
         * @param serviceName The service name for the subscribe session.
         *
         * @return The builder to facilitate chaining
         *         {@code builder.setXXX(..).setXXX(..)}.
         */
        public Builder setServiceName(@NonNull String serviceName) {
            if (serviceName == null) {
                throw new IllegalArgumentException("Invalid service name - must be non-null");
            }
            mServiceName = serviceName.getBytes(StandardCharsets.UTF_8);
            return this;
        }

        /**
         * Specify service specific information for the subscribe session. This is
         * a free-form byte array available to the application to send
         * additional information as part of the discovery operation - i.e. it
         * will not be used to determine whether a publish/subscribe match
         * occurs.
         * <p>
         *     Optional. Empty by default.
         *
         * @param serviceSpecificInfo A byte-array for the service-specific
         *            information field.
         *
         * @return The builder to facilitate chaining
         *         {@code builder.setXXX(..).setXXX(..)}.
         */
        public Builder setServiceSpecificInfo(@Nullable byte[] serviceSpecificInfo) {
            mServiceSpecificInfo = serviceSpecificInfo;
            return this;
        }

        /**
         * The match filter for a subscribe session. Used to determine whether a service
         * discovery occurred - in addition to relying on the service name.
         * <p>
         *     Optional. Empty by default.
         *
         * @param matchFilter A list of match filter entries (each of which is an arbitrary byte
         *                    array).
         *
         * @return The builder to facilitate chaining
         *         {@code builder.setXXX(..).setXXX(..)}.
         */
        public Builder setMatchFilter(@Nullable List<byte[]> matchFilter) {
            mMatchFilter = new TlvBufferUtils.TlvConstructor(0, 1).allocateAndPut(
                    matchFilter).getArray();
            return this;
        }

        /**
         * Sets the type of the subscribe session: active (subscribe packets are
         * transmitted over-the-air), or passive (no subscribe packets are
         * transmitted, a match is made against a solicited/active publish
         * session whose packets are transmitted over-the-air).
         *
         * @param subscribeType Subscribe session type:
         *            {@link SubscribeConfig#SUBSCRIBE_TYPE_ACTIVE} or
         *            {@link SubscribeConfig#SUBSCRIBE_TYPE_PASSIVE}.
         *
         * @return The builder to facilitate chaining
         *         {@code builder.setXXX(..).setXXX(..)}.
         */
        public Builder setSubscribeType(@SubscribeTypes int subscribeType) {
            if (subscribeType < SUBSCRIBE_TYPE_PASSIVE || subscribeType > SUBSCRIBE_TYPE_ACTIVE) {
                throw new IllegalArgumentException("Invalid subscribeType - " + subscribeType);
            }
            mSubscribeType = subscribeType;
            return this;
        }

        /**
         * Sets the time interval (in seconds) an active (
         * {@link SubscribeConfig.Builder#setSubscribeType(int)}) subscribe session
         * will be alive - i.e. broadcasting a packet. When the TTL is reached
         * an event will be generated for
         * {@link DiscoverySessionCallback#onSessionTerminated()}.
         * <p>
         *     Optional. 0 by default - indicating the session doesn't terminate on its own.
         *     Session will be terminated when {@link DiscoverySession#close()} is
         *     called.
         *
         * @param ttlSec Lifetime of a subscribe session in seconds.
         *
         * @return The builder to facilitate chaining
         *         {@code builder.setXXX(..).setXXX(..)}.
         */
        public Builder setTtlSec(int ttlSec) {
            if (ttlSec < 0) {
                throw new IllegalArgumentException("Invalid ttlSec - must be non-negative");
            }
            mTtlSec = ttlSec;
            return this;
        }

        /**
         * Configure whether a subscribe terminate notification
         * {@link DiscoverySessionCallback#onSessionTerminated()} is reported
         * back to the callback.
         *
         * @param enable If true the terminate callback will be called when the
         *            subscribe is terminated. Otherwise it will not be called.
         *
         * @return The builder to facilitate chaining
         *         {@code builder.setXXX(..).setXXX(..)}.
         */
        public Builder setTerminateNotificationEnabled(boolean enable) {
            mEnableTerminateNotification = enable;
            return this;
        }

        /**
         * Configure the minimum distance to a discovered publisher at which to trigger a discovery
         * notification. I.e. discovery will be triggered if we've found a matching publisher
         * (based on the other criteria in this configuration) <b>and</b> the distance to the
         * publisher is larger than the value specified in this API. Can be used in conjunction with
         * {@link #setMaxDistanceMm(int)} to specify a geofence, i.e. discovery with min <=
         * distance <= max.
         * <p>
         * For ranging to be used in discovery it must also be enabled on the publisher using
         * {@link PublishConfig.Builder#setRangingEnabled(boolean)}. However, ranging may
         * not be available or enabled on the publisher or may be temporarily disabled on either
         * subscriber or publisher - in such cases discovery will proceed without ranging.
         * <p>
         * When ranging is enabled and available on both publisher and subscriber and a service
         * is discovered based on geofence constraints the
         * {@link DiscoverySessionCallback#onServiceDiscoveredWithinRange(PeerHandle, byte[], List, int)}
         * is called, otherwise the
         * {@link DiscoverySessionCallback#onServiceDiscovered(PeerHandle, byte[], List)}
         * is called.
         * <p>
         * The device must support Wi-Fi RTT for this feature to be used. Feature support is checked
         * as described in {@link android.net.wifi.rtt}.
         * <p>
         * Note: The minimum distance is same as outer threshold of egress geofence. This can be
         * more than {@link #setMaxDistanceMm(int)} which is same as inner threshold of ingress
         * geofence i.e. discovery with  distance <= max or distance >= min.
         *
         * @param minDistanceMm Minimum distance, in mm, to the publisher above which to trigger
         *                      discovery.
         *
         * @return The builder to facilitate chaining
         *         {@code builder.setXXX(..).setXXX(..)}.
         */
        public Builder setMinDistanceMm(int minDistanceMm) {
            mMinDistanceMm = minDistanceMm;
            mMinDistanceMmSet = true;
            return this;
        }

        /**
         * Configure the maximum distance to a discovered publisher at which to trigger a discovery
         * notification. I.e. discovery will be triggered if we've found a matching publisher
         * (based on the other criteria in this configuration) <b>and</b> the distance to the
         * publisher is smaller than the value specified in this API. Can be used in conjunction
         * with {@link #setMinDistanceMm(int)} to specify a geofence, i.e. discovery with min <=
         * distance <= max.
         * <p>
         * For ranging to be used in discovery it must also be enabled on the publisher using
         * {@link PublishConfig.Builder#setRangingEnabled(boolean)}. However, ranging may
         * not be available or enabled on the publisher or may be temporarily disabled on either
         * subscriber or publisher - in such cases discovery will proceed without ranging.
         * <p>
         * When ranging is enabled and available on both publisher and subscriber and a service
         * is discovered based on geofence constraints the
         * {@link DiscoverySessionCallback#onServiceDiscoveredWithinRange(PeerHandle, byte[], List, int)}
         * is called, otherwise the
         * {@link DiscoverySessionCallback#onServiceDiscovered(PeerHandle, byte[], List)}
         * is called.
         * <p>
         * The device must support Wi-Fi RTT for this feature to be used. Feature support is checked
         * as described in {@link android.net.wifi.rtt}.
         * <p>
         * Note: The maximum distance is same as inner threshold of ingress geofence. This can be
         * less than {@link #setMinDistanceMm(int)} which is same as outer threshold of egress
         * geofence i.e. discovery with  distance <= max or distance >= min.
         *
         * @param maxDistanceMm Maximum distance, in mm, to the publisher below which to trigger
         *                      discovery.
         *
         * @return The builder to facilitate chaining
         *         {@code builder.setXXX(..).setXXX(..)}.
         */
        public Builder setMaxDistanceMm(int maxDistanceMm) {
            mMaxDistanceMm = maxDistanceMm;
            mMaxDistanceMmSet = true;
            return this;
        }

        /**
         * Configure whether to enable and use instant communication for this subscribe session.
         * Instant communication will speed up service discovery and any data-path set up as part of
         * this session. Use {@link Characteristics#isInstantCommunicationModeSupported()} to check
         * if the device supports this feature.
         *
         * <p>Note: due to increased power requirements of this mode - it will only remain enabled
         * for 30 seconds from the time the discovery session is started.
         *
         * @param enabled true for enable instant communication mode, default is false.
         * @param band When setting to {@link ScanResult#WIFI_BAND_5_GHZ}, device will try to enable
         *     instant communication mode on 5Ghz, but may fall back to 2.4Ghz due to regulatory
         *     requirements.
         * @return the current {@link Builder} builder, enabling chaining of builder methods.
         */
        @NonNull
        public Builder setInstantCommunicationModeEnabled(
                boolean enabled, @WifiAwareManager.InstantModeBand int band) {
            if (!true) {
                throw new UnsupportedOperationException();
            }
            if (band != ScanResult.WIFI_BAND_24_GHZ && band != ScanResult.WIFI_BAND_5_GHZ) {
                throw new IllegalArgumentException();
            }
            mBand = band;
            mEnableInstantMode = enabled;
            return this;
        }

        /**
         * Set the {@link AwarePairingConfig} for this subscribe session, the peer can use this info
         * to determine the config of the following bootstrapping, pairing setup/verification
         * request.
         * @see AwarePairingConfig
         * @param config The pairing config set to the peer. Only valid when
         * {@link Characteristics#isAwarePairingSupported()} is true.
         * @return the current {@link Builder} builder, enabling chaining of builder methods.
         */
        @NonNull public Builder setPairingConfig(@Nullable AwarePairingConfig config) {
            if (!true) {
                throw new UnsupportedOperationException();
            }
            mPairingConfig = config;
            return this;
        }

        
        
        
        
        
        
        
        
        
        
        /**
         * Build {@link SubscribeConfig} given the current requests made on the
         * builder.
         */
        public SubscribeConfig build() {
            return new SubscribeConfig(mServiceName, mServiceSpecificInfo, mMatchFilter,
                    mSubscribeType, mTtlSec, mEnableTerminateNotification,
                    mMinDistanceMmSet, mMinDistanceMm, mMaxDistanceMmSet, mMaxDistanceMm,
                    mEnableInstantMode, mBand, mPairingConfig, mIsSuspendable, mVendorData,
                    mPeriodicRangingInterval, mPeriodicRangingEnabled, mRttBurstSize,
                    mFrequencyMhz, mCenterFrequency0Mhz, mCenterFrequency1Mhz, mPreamble,
                    mChannelWidth);

        }
    }
}
