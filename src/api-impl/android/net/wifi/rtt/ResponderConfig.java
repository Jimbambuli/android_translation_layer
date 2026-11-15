package android.net.wifi.rtt;

/* AOSP android/net/wifi/rtt/ResponderConfig.java */
import static android.net.wifi.ScanResult.InformationElement.EID_EXTENSION_PRESENT;
import static android.net.wifi.ScanResult.InformationElement.EID_EXT_EHT_CAPABILITIES;
import static android.net.wifi.ScanResult.InformationElement.EID_EXT_HE_CAPABILITIES;
import static android.net.wifi.ScanResult.InformationElement.EID_HT_CAPABILITIES;
import static android.net.wifi.ScanResult.InformationElement.EID_VHT_CAPABILITIES;

import android.annotation.IntDef;
import android.annotation.IntRange;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.net.MacAddress;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiAnnotations;
import android.net.wifi.aware.PeerHandle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;


import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

/**
 * Defines the configuration of an IEEE 802.11mc Responder. The Responder may be an Access Point
 * (AP), a Wi-Fi Aware device, or a manually configured Responder.
 * <p>
 * A Responder configuration may be constructed from a {@link ScanResult} or manually (with the
 * data obtained out-of-band from a peer).
 */
public final class ResponderConfig implements Parcelable {
    private static final String TAG = "ResponderConfig";
    private static final int AWARE_BAND_2_DISCOVERY_CHANNEL = 2437;

    
    /**
     * Responder is an access point(AP).
     */

    public static final int RESPONDER_AP = 0;

    /**
     * Responder is a client device(STA).
     */
    public static final int RESPONDER_STA = 1;

    
    
    
    

    
    
    
    
    
    
    
    
    
    
    
    
    private static final long DEFAULT_NTB_MIN_TIME_BETWEEN_MEASUREMENTS_MICROS = 250000;
    private static final long DEFAULT_NTB_MAX_TIME_BETWEEN_MEASUREMENTS_MICROS = 15000000;

    
    
    
    
    
    
    
    
    
    
    private long mNtbMinMeasurementTime = DEFAULT_NTB_MIN_TIME_BETWEEN_MEASUREMENTS_MICROS;
    private long mNtbMaxMeasurementTime = DEFAULT_NTB_MAX_TIME_BETWEEN_MEASUREMENTS_MICROS;
    private final SecureRangingConfig mSecureRangingConfig;

    
    
    
    
    /**
     * Creates a Responder configuration from a {@link ScanResult} corresponding to an Access
     * Point (AP), which can be obtained from {@link android.net.wifi.WifiManager#getScanResults()}.
     */
    @NonNull
    public static ResponderConfig fromScanResult(@NonNull ScanResult scanResult) {
        MacAddress macAddress = MacAddress.fromString(scanResult.BSSID);
        int responderType = RESPONDER_AP;
        boolean supports80211mc = scanResult.is80211mcResponder();
        boolean supports80211azNtbRanging = scanResult.is80211azNtbResponder();
        int channelWidth = scanResult.channelWidth;
        int frequency = scanResult.frequency;
        int centerFreq0 = scanResult.centerFreq0;
        int centerFreq1 = scanResult.centerFreq1;

        int preamble;
        // The IEEE 802.11mc is only compatible with HE and EHT when using the 6 GHz band.
        // However, the IEEE 802.11az supports HE and EHT across all Wi-Fi bands (2.4GHz, 5 GHz,
        // and 6 GHz).
        boolean isHeOrEhtAllowed = supports80211azNtbRanging || ScanResult.is6GHz(frequency);
        if (scanResult.informationElements != null && scanResult.informationElements.length != 0) {
            boolean htCapabilitiesPresent = false;
            boolean vhtCapabilitiesPresent = false;
            boolean heCapabilitiesPresent = false;
            boolean ehtCapabilitiesPresent = false;

            for (ScanResult.InformationElement ie : scanResult.informationElements) {
                if (ie.id == EID_HT_CAPABILITIES) {
                    htCapabilitiesPresent = true;
                } else if (ie.id == EID_VHT_CAPABILITIES) {
                    vhtCapabilitiesPresent = true;
                } else if (ie.id == EID_EXTENSION_PRESENT && ie.idExt == EID_EXT_HE_CAPABILITIES) {
                    heCapabilitiesPresent = true;
                } else if (ie.id == EID_EXTENSION_PRESENT && ie.idExt == EID_EXT_EHT_CAPABILITIES) {
                    ehtCapabilitiesPresent = true;
                }
            }

            if (ehtCapabilitiesPresent && isHeOrEhtAllowed) {
                preamble = ScanResult.PREAMBLE_EHT;
            } else if (supports80211azNtbRanging || (heCapabilitiesPresent && isHeOrEhtAllowed)) {
                // In IEEE 802.11az, ranging uses the HE preamble. An AP advertising 11az support
                // must therefore implement HE preamble handling for ranging, even if it is not
                // advertising HE capabilities for normal network association. Also EHT support
                // implies HE support.
                preamble = ScanResult.PREAMBLE_HE;
            } else if (vhtCapabilitiesPresent) {
                preamble = ScanResult.PREAMBLE_VHT;
            } else if (htCapabilitiesPresent) {
                preamble = ScanResult.PREAMBLE_HT;
            } else {
                preamble = ScanResult.PREAMBLE_LEGACY;
            }
        } else {
            Log.e(TAG, "Scan Results do not contain IEs - using backup method to select preamble");
            if (channelWidth == ScanResult.CHANNEL_WIDTH_320MHZ && isHeOrEhtAllowed) {
                preamble = ScanResult.PREAMBLE_EHT;
            } else if (channelWidth == ScanResult.CHANNEL_WIDTH_320MHZ
                    || channelWidth == ScanResult.CHANNEL_WIDTH_80MHZ
                    || channelWidth == ScanResult.CHANNEL_WIDTH_160MHZ) {
                preamble = ScanResult.PREAMBLE_VHT;
            } else {
                preamble = ScanResult.PREAMBLE_HT;
            }
        }

        Builder builder = new Builder()
                .setMacAddress(macAddress)
                .setResponderType(responderType)
                .set80211mcSupported(supports80211mc)
                .set80211azNtbSupported(supports80211azNtbRanging)
                .setChannelWidth(channelWidth)
                .setFrequencyMhz(frequency)
                .setCenterFreq0Mhz(centerFreq0)
                .setCenterFreq1Mhz(centerFreq1)
                .setPreamble(preamble);

        if (isSecureRangingResponder(scanResult)) {
            builder.setSecureRangingConfig(getSecureRangingConfig(scanResult));
        }

        return builder.build();
    }

    private static boolean isSecureRangingResponder(ScanResult scanResult) {
        return ((scanResult.capabilities != null) && (scanResult.capabilities.contains("PASN")));
    }

    private static SecureRangingConfig getSecureRangingConfig(ScanResult scanResult) {
        PasnConfig.Builder pasnConfigBuilder = new PasnConfig.Builder(
                PasnConfig.getBaseAkmsFromCapabilities(scanResult.capabilities),
                PasnConfig.getCiphersFromCapabilities(scanResult.capabilities));
        if (scanResult.getWifiSsid() != null) {
            pasnConfigBuilder.setWifiSsid(scanResult.getWifiSsid());
        }
        // If the responder is capable of PASN, always enable frame protection for secure ranging
        // irrespective of responder mandates or not.
        return new SecureRangingConfig.Builder(pasnConfigBuilder.build())
                .setSecureHeLtfEnabled(scanResult.isSecureHeLtfSupported())
                .setRangingFrameProtectionEnabled(true)
                .build();
    }

    
    
    private static boolean isResponderTypeSupported(int responderType) {
        switch (responderType) {
            case RESPONDER_AP:
            case RESPONDER_STA:
            case RESPONDER_AWARE:
                break;
            case RESPONDER_P2P_GO:
            case RESPONDER_P2P_CLIENT:
            default:
                return false;
        }
        return true;
    }

    
    /**
     * @return the MAC address of the responder
     */
    @Nullable
    public MacAddress getMacAddress() {
        return macAddress;
    }

    /**
     * Returns the Wi-Fi Aware peer identifier of the responder.
     * <p>
     * This value is non-null if the responder is a Wi-Fi Aware peer and was configured
     * using a {@link PeerHandle} (via {@link ResponderConfig.Builder#setPeerHandle(PeerHandle)}).
     * The {@link PeerHandle} is an opaque identifier for a specific Aware peer discovered
     * during Wi-Fi Aware operations, such as service discovery.
     * <p>
     * If the responder is not an Aware peer, or if it was configured using its
     * MAC address (via {@link ResponderConfig.Builder#setMacAddress(MacAddress)}),
     * this method will return {@code null}.
     * <p>
     * Only one of {@link #getPeerHandle()} or {@link #getMacAddress()} will return a
     * non-null value for a valid {@link ResponderConfig}.
     *
     * @return The {@link PeerHandle} of the Wi-Fi Aware responder, or {@code null}
     *         if the responder is not an Aware peer or is identified by its MAC address.
     */
    @Nullable
    public PeerHandle getPeerHandle() {
        return peerHandle;
    }

    /**
     * @return true if the Responder supports the 802.11mc protocol, false otherwise.
     */
    public boolean is80211mcSupported() {
        return supports80211mc;
    }

    /**
     * @return true if the Responder supports the 802.11az non-trigger based ranging protocol,
     * false otherwise.
     */
    public boolean is80211azNtbSupported() {
        return supports80211azNtb;
    }

    /**
     * AP Channel bandwidth; one of {@link ScanResult#CHANNEL_WIDTH_20MHZ},
     * {@link ScanResult#CHANNEL_WIDTH_40MHZ},
     * {@link ScanResult#CHANNEL_WIDTH_80MHZ}, {@link ScanResult#CHANNEL_WIDTH_160MHZ},
     * {@link ScanResult #CHANNEL_WIDTH_80MHZ_PLUS_MHZ} or {@link ScanResult#CHANNEL_WIDTH_320MHZ}.
     *
     * @return the bandwidth repsentation of the Wi-Fi channel
     */
    public @WifiAnnotations.ChannelWidth int getChannelWidth() {
        return translateFromLocalToScanResultChannelWidth(channelWidth);
    }

    /**
     * @return the frequency in MHz of the Wi-Fi channel
     */
    @IntRange(from = 0)
    public int getFrequencyMhz() {
        return frequency;
    }

    /**
     * If the Access Point (AP) bandwidth is 20 MHz, 0 MHz is returned.
     * If the AP use 40, 80 or 160 MHz, this is the center frequency (in MHz).
     * if the AP uses 80 + 80 MHz, this is the center frequency of the first segment (in MHz).
     *
     * @return the center frequency in MHz of the first channel segment
     */
    @IntRange(from = 0)
    public int getCenterFreq0Mhz() {
        return centerFreq0;
    }

    /**
     * If the Access Point (AP) bandwidth is 80 + 80 MHz, this param is not used and returns 0.
     * If the AP uses 80 + 80 MHz, this is the center frequency of the second segment in MHz.
     *
     * @return the center frequency in MHz of the second channel segment (if used)
     */
    @IntRange(from = 0)
    public int getCenterFreq1Mhz() {
        return centerFreq1;
    }

    /**
     * Get the preamble type of the channel.
     *
     * @return the preamble used for this channel
     */
    public @WifiAnnotations.PreambleType int getPreamble() {
        return translateFromLocalToScanResultPreamble(preamble);
    }

    /**
     * Get responder type.
     * @see Builder#setResponderType(int)
     * @return The type of this responder
     */
    public int getResponderType() {
        return responderType;
    }

    
    
    
    
    /**
     * Get secure ranging configuration.
     * @return Secure ranging configuration. Returns null for non-secure ranging configuration.
     */
    @Nullable
    public SecureRangingConfig getSecureRangingConfig() {
        return mSecureRangingConfig;
    }
    /**
     * Builder class used to construct {@link ResponderConfig} objects.
     */
    public static final class Builder {
        private MacAddress mMacAddress;
        private PeerHandle mPeerHandle;
        private int mResponderType = RESPONDER_AP;
        private boolean mSupports80211Mc = true;
        private boolean mSupports80211azNtb = false;
        private int mChannelWidth = CHANNEL_WIDTH_20MHZ;
        private int mFrequency = 0;
        private int mCenterFreq0 = 0;
        private int mCenterFreq1 = 0;
        private int mPreamble = PREAMBLE_LEGACY;
        private long mNtbMinMeasurementTime = DEFAULT_NTB_MIN_TIME_BETWEEN_MEASUREMENTS_MICROS;
        private long mNtbMaxMeasurementTime = DEFAULT_NTB_MAX_TIME_BETWEEN_MEASUREMENTS_MICROS;
        private SecureRangingConfig mSecureRangingConfig = null;

        /**
         * Default constructor for the Builder.
         */
        public Builder() {
        }

        /**
         * Creates a new Builder instance, initializing it with the values from an
         * existing {@link ResponderConfig} object. This allows for easy modification
         * of an existing configuration.
         *
         * @param config The ResponderConfig instance to copy values from.
         */
        public Builder(@NonNull ResponderConfig config) {
            Objects.requireNonNull(config);
            this.mMacAddress = config.macAddress;
            this.mPeerHandle = config.peerHandle;
            this.mResponderType = config.responderType;
            this.mSupports80211Mc = config.supports80211mc;
            this.mSupports80211azNtb = config.supports80211azNtb;
            this.mChannelWidth = config.channelWidth;
            this.mFrequency = config.frequency;
            this.mCenterFreq0 = config.centerFreq0;
            this.mCenterFreq1 = config.centerFreq1;
            this.mPreamble = config.preamble;
            this.mNtbMinMeasurementTime = config.mNtbMinMeasurementTime;
            this.mNtbMaxMeasurementTime = config.mNtbMaxMeasurementTime;
            this.mSecureRangingConfig = config.mSecureRangingConfig;
        }

        /**
         * Sets the Responder MAC Address.
         * <p>
         * A responder must be identified by <em>either</em> a {@link MacAddress}
         * <em>or</em> a {@link PeerHandle} (for Aware peers, set via
         * {@link #setPeerHandle(PeerHandle)}), but not both.
         * <p>
         * Note: This method sets the MAC address. If a {@link PeerHandle} was previously
         * set using {@link #setPeerHandle(PeerHandle)}, both identifiers might be temporarily
         * present within the builder. The final validation ensuring that only one of
         * these identifiers is exclusively set occurs during the {@link #build()} operation.
         * If {@link #build()} is called when both a MAC address and a PeerHandle are configured
         * or when neither is set, it will result in an {@link IllegalArgumentException}.
         *
         * @param macAddress the physical address of the responder
         * @return the builder to facilitate chaining
         *         {@code builder.setXXX(..).setXXX(..)}.
         */
        @NonNull
        public Builder setMacAddress(@Nullable MacAddress macAddress) {
            this.mMacAddress = macAddress;
            return this;
        }

        /**
         * Sets the Responder Aware Peer handle.
         * <p>
         * Use this method to identify the responder when it is a Wi-Fi Aware peer.
         * The {@link PeerHandle} is a unique identifier for an Aware peer obtained
         * through Wi-Fi Aware discovery processes.
         * <p>
         * A responder must be identified by <em>either</em> a {@link PeerHandle}
         * (for Aware peers) <em>or</em> a {@link MacAddress} (for non-Aware peers like
         * Access Points or Wi-Fi Direct devices), but not both.
         * <p>
         * Note: This method sets the peer handle. If a MAC address was previously set
         * using {@link #setMacAddress(MacAddress)}, both identifiers might be temporarily
         * present within the builder. The final validation ensuring that only one of
         * these identifiers is exclusively set occurs during the {@link #build()} operation.
         * If {@link #build()} is called when both a MAC address and a PeerHandle are configured
         * or when neither is set, it will result in an {@link IllegalArgumentException}.
         *
         * @param peerHandle Peer handle of the responder
         * @return the builder to facilitate chaining
         *         {@code builder.setXXX(..).setXXX(..)}.
         */
        @NonNull
        public Builder setPeerHandle(@Nullable PeerHandle peerHandle) {
            this.mPeerHandle = peerHandle;
            return this;
        }

        /**
         * Sets an indication the access point can to respond to the two-sided Wi-Fi RTT protocol,
         * but, if false, indicates only one-sided Wi-Fi RTT is possible.
         *
         * @param supports80211mc the ability to support the Wi-Fi RTT protocol
         * @return the builder to facilitate chaining
         *         {@code builder.setXXX(..).setXXX(..)}.
         */
        @NonNull
        public Builder set80211mcSupported(boolean supports80211mc) {
            this.mSupports80211Mc = supports80211mc;
            return this;
        }

        /**
         * Sets an indication the access point can to respond to the IEEE 802.11az non-trigger
         * based ranging protocol, but, if false, indicates only IEEE 802.11mc or one-sided Wi-Fi
         * RTT is possible.
         *
         * @param supports80211azNtb the ability to support the IEEE 802.11az non-trigger based
         *                           ranging protocol
         * @return the builder to facilitate chaining
         *         {@code builder.setXXX(..).setXXX(..)}.
         */
        @NonNull
        public Builder set80211azNtbSupported(boolean supports80211azNtb) {
            this.mSupports80211azNtb = supports80211azNtb;
            return this;
        }

        /**
         * Sets the channel bandwidth in MHz.
         *
         * @param channelWidth the bandwidth of the channel in MHz
         * @return the builder to facilitate chaining
         *         {@code builder.setXXX(..).setXXX(..)}.
         */
        @NonNull
        public Builder setChannelWidth(@WifiAnnotations.ChannelWidth int channelWidth) {
            this.mChannelWidth = translateFromScanResultToLocalChannelWidth(channelWidth);
            return this;
        }

        /**
         * Sets the frequency of the channel in MHz.
         * <p>
         * Note: The frequency is used as a hint, and the underlying WiFi subsystem may use it, or
         * select an alternate if its own connectivity scans have determined the frequency of the
         * access point has changed.
         * </p>
         *
         * @param frequency the frequency of the channel in MHz
         * @return the builder to facilitate chaining
         *         {@code builder.setXXX(..).setXXX(..)}.
         */
        @NonNull
        public Builder setFrequencyMhz(@IntRange(from = 0) int frequency) {
            this.mFrequency = frequency;
            return this;
        }

        /**
         * Sets the center frequency in MHz of the first segment of the channel.
         * <p>
         * Note: The frequency is used as a hint, and the underlying WiFi subsystem may use it, or
         * select an alternate if its own connectivity scans have determined the frequency of the
         * access point has changed.
         * </p>
         *
         * @param centerFreq0 the center frequency in MHz of first channel segment
         * @return the builder to facilitate chaining
         *         {@code builder.setXXX(..).setXXX(..)}.
         */
        @NonNull
        public Builder setCenterFreq0Mhz(@IntRange(from = 0) int centerFreq0) {
            this.mCenterFreq0 = centerFreq0;
            return this;
        }

        /**
         * Sets the center frequency in MHz of the second segment of the channel, if used.
         * <p>
         * Note: The frequency is used as a hint, and the underlying WiFi subsystem may use it, or
         * select an alternate if its own connectivity scans have determined the frequency of the
         * access point has changed.
         * </p>
         *
         * @param centerFreq1 the center frequency in MHz of second channel segment
         * @return the builder to facilitate chaining
         *         {@code builder.setXXX(..).setXXX(..)}.
         */
        @NonNull
        public Builder setCenterFreq1Mhz(@IntRange(from = 0) int centerFreq1) {
            this.mCenterFreq1 = centerFreq1;
            return this;
        }

        /**
         * Sets the preamble encoding for the protocol.
         *
         * @param preamble the preamble encoding
         * @return the builder to facilitate chaining
         *         {@code builder.setXXX(..).setXXX(..)}.
         */
        @NonNull
        public Builder setPreamble(@WifiAnnotations.PreambleType int preamble) {
            this.mPreamble = translateFromScanResultToLocalPreamble(preamble);
            return this;
        }

        /**
         * Sets the responder type, can be {@link #RESPONDER_AP} or {@link #RESPONDER_STA} or
         * {@link #RESPONDER_AWARE}
         *
         * @param responderType the type of the responder, if not set defaults to
         * {@link #RESPONDER_AP}
         * @return the builder to facilitate chaining {@code builder.setXXX(..).setXXX(..)}.
         */
        @NonNull
        public Builder setResponderType(int responderType) {
            if (!isResponderTypeSupported(responderType)) {
                throw new IllegalArgumentException("invalid responder type " + responderType);
            }
            mResponderType = responderType;
            return this;
        }

        
        
        /**
         * Set secure ranging configuration. See {@link SecureRangingConfig} for more details.
         * <p>
         * Note: Secure ranging will get enabled only if the device and responder support. For
         * device support see {@link WifiRttManager#getRttCharacteristics()}. Following capabilities
         * needs to be enabled,
         * <ul>
         * <li>{@link WifiRttManager#CHARACTERISTICS_KEY_BOOLEAN_NTB_INITIATOR}
         * <li>{@link WifiRttManager#CHARACTERISTICS_KEY_BOOLEAN_SECURE_HE_LTF_SUPPORTED} and/or
         * <li>{@link WifiRttManager#CHARACTERISTICS_KEY_BOOLEAN_RANGING_FRAME_PROTECTION_SUPPORTED}
         * </ul>
         * For the responder support (from scan result),
         * <ul>
         * <li> {@link ScanResult#capabilities} string contains PASN and optionally a base AKM
         * <li> {@link ScanResult#isSecureHeLtfSupported()}
         * <li> {@link ScanResult#isRangingFrameProtectionRequired()}
         * </ul>
         * @param secureRangingConfig Secure ranging configuration
         * @return the builder to facilitate chaining {@code builder.setXXX(..).setXXX(..)}.
         */
        @NonNull
        public Builder setSecureRangingConfig(@NonNull SecureRangingConfig secureRangingConfig) {
            Objects.requireNonNull(secureRangingConfig, "secureRangingConfig cannot be null");
            mSecureRangingConfig = secureRangingConfig;
            return this;
        }

        /**
         * Build {@link ResponderConfig} given the current configurations made on the builder.
         * @return an instance of {@link ResponderConfig}
         */
        @NonNull
        public ResponderConfig build() {
            if ((mMacAddress == null && mPeerHandle == null)
                    || (mMacAddress != null && mPeerHandle != null)) {
                throw new IllegalArgumentException(
                        "Invalid ResponderConfig - must specify a MAC address or peer handle but "
                                + "not both");
            }
            if (mSupports80211azNtb && !isHeSupported(mPreamble)) {
                throw new IllegalArgumentException(
                        "IEEE 802.11az responder must support HE preamble");
            }
            // For Aware, use supported default values
            if (mResponderType == RESPONDER_AWARE) {
                mSupports80211Mc = true;
                mFrequency = AWARE_BAND_2_DISCOVERY_CHANNEL;
                mChannelWidth = CHANNEL_WIDTH_20MHZ;
                mPreamble = PREAMBLE_HT;
            }
            return new ResponderConfig(this);
        }
    }

    private static boolean isHeSupported(int preamble) {
        // EHT support implies HE support.
        return preamble != PREAMBLE_LEGACY && preamble != PREAMBLE_HT && preamble != PREAMBLE_VHT;
    }
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        if (macAddress == null) {
            dest.writeBoolean(false);
        } else {
            dest.writeBoolean(true);
            macAddress.writeToParcel(dest, flags);
        }
        if (peerHandle == null) {
            dest.writeBoolean(false);
        } else {
            dest.writeBoolean(true);
            dest.writeInt(peerHandle.peerId);
        }
        dest.writeInt(responderType);
        dest.writeBoolean(supports80211mc);
        dest.writeBoolean(supports80211azNtb);
        dest.writeInt(channelWidth);
        dest.writeInt(frequency);
        dest.writeInt(centerFreq0);
        dest.writeInt(centerFreq1);
        dest.writeInt(preamble);
        dest.writeLong(mNtbMinMeasurementTime);
        dest.writeLong(mNtbMaxMeasurementTime);
        dest.writeParcelable(mSecureRangingConfig, flags);
    }

    public static final @android.annotation.NonNull Creator<ResponderConfig> CREATOR = new Creator<ResponderConfig>() {
        @Override
        public ResponderConfig[] newArray(int size) {
            return new ResponderConfig[size];
        }

        @Override
        public ResponderConfig createFromParcel(Parcel in) {
            boolean macAddressPresent = in.readBoolean();
            MacAddress macAddress = null;
            if (macAddressPresent) {
                macAddress = MacAddress.CREATOR.createFromParcel(in);
            }
            boolean peerHandlePresent = in.readBoolean();
            PeerHandle peerHandle = null;
            if (peerHandlePresent) {
                peerHandle = new PeerHandle(in.readInt());
            }

            ResponderConfig.Builder builder = new Builder()
                    .setMacAddress(macAddress)
                    .setPeerHandle(peerHandle)
                    .setResponderType(in.readInt())
                    .set80211mcSupported(in.readBoolean())
                    .set80211azNtbSupported(in.readBoolean())
                    .setChannelWidth(in.readInt())
                    .setFrequencyMhz(in.readInt())
                    .setCenterFreq0Mhz(in.readInt())
                    .setCenterFreq1Mhz(in.readInt())
                    .setPreamble(in.readInt())
                    .setNtbMinTimeBetweenMeasurementsMicros(in.readLong())
                    .setNtbMaxTimeBetweenMeasurementsMicros(in.readLong());
            SecureRangingConfig secureRangingConfig = in.readParcelable(
                    SecureRangingConfig.class.getClassLoader());

            if (secureRangingConfig != null) {
                builder.setSecureRangingConfig(secureRangingConfig);
            }
            return new ResponderConfig(builder);

        }
    };

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof ResponderConfig)) {
            return false;
        }

        ResponderConfig lhs = (ResponderConfig) o;

        return Objects.equals(macAddress, lhs.macAddress) && Objects.equals(peerHandle,
                lhs.peerHandle) && responderType == lhs.responderType
                && supports80211mc == lhs.supports80211mc && channelWidth == lhs.channelWidth
                && frequency == lhs.frequency && centerFreq0 == lhs.centerFreq0
                && centerFreq1 == lhs.centerFreq1 && preamble == lhs.preamble
                && supports80211azNtb == lhs.supports80211azNtb
                && mNtbMinMeasurementTime == lhs.mNtbMinMeasurementTime
                && mNtbMaxMeasurementTime == lhs.mNtbMaxMeasurementTime
                && Objects.equals(mSecureRangingConfig, lhs.mSecureRangingConfig);
    }

    @Override
    public int hashCode() {
        return Objects.hash(macAddress, peerHandle, responderType, supports80211mc, channelWidth,
                frequency, centerFreq0, centerFreq1, preamble, supports80211azNtb,
                mNtbMinMeasurementTime, mNtbMaxMeasurementTime, mSecureRangingConfig);
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer("ResponderConfig: macAddress=").append(macAddress)
                .append(", peerHandle=").append(peerHandle == null ? "<null>" : peerHandle.peerId)
                .append(", responderType=").append(responderType)
                .append(", supports80211mc=").append(supports80211mc)
                .append(", channelWidth=").append(channelWidth)
                .append(", frequency=").append(frequency)
                .append(", centerFreq0=").append(centerFreq0)
                .append(", centerFreq1=").append(centerFreq1)
                .append(", preamble=").append(preamble)
                .append(", supports80211azNtb=").append(supports80211azNtb)
                .append(", mNtbMinMeasurementTime=").append(mNtbMinMeasurementTime)
                .append(", mNtbMaxMeasurementTime=").append(mNtbMaxMeasurementTime);

        if (mSecureRangingConfig != null) {
            sb.append(", mSecureRangingConfig=").append(mSecureRangingConfig);
        }

        return sb.toString();

    }

    
    
    
    }
