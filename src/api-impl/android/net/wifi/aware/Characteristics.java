package android.net.wifi.aware;

/* AOSP android/net/wifi/aware/Characteristics.java */

import android.annotation.IntDef;
import android.annotation.IntRange;
import android.net.wifi.WifiAnnotations;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;



import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * The characteristics of the Wi-Fi Aware implementation.
 */
public final class Characteristics implements Parcelable {
                                                            
    private final Bundle mCharacteristics;

    
    /**
     * Returns the maximum string length that can be used to specify a Aware service name. Restricts
     * the parameters of the {@link PublishConfig.Builder#setServiceName(String)} and
     * {@link SubscribeConfig.Builder#setServiceName(String)}.
     *
     * @return A positive integer, maximum string length of Aware service name.
     */
    public int getMaxServiceNameLength() {
        return mCharacteristics.getInt(KEY_MAX_SERVICE_NAME_LENGTH);
    }

    /**
     * Returns the maximum length of byte array that can be used to specify a Aware service specific
     * information field: the arbitrary load used in discovery or the message length of Aware
     * message exchange. Restricts the parameters of the
     * {@link PublishConfig.Builder#setServiceSpecificInfo(byte[])},
     * {@link SubscribeConfig.Builder#setServiceSpecificInfo(byte[])}, and
     * {@link DiscoverySession#sendMessage(PeerHandle, int, byte[])}
     * variants.
     *
     * @return A positive integer, maximum length of byte array for Aware messaging.
     */
    public int getMaxServiceSpecificInfoLength() {
        return mCharacteristics.getInt(KEY_MAX_SERVICE_SPECIFIC_INFO_LENGTH);
    }

    /**
     * Returns the maximum length of byte array that can be used to specify a Aware match filter.
     * Restricts the parameters of the
     * {@link PublishConfig.Builder#setMatchFilter(java.util.List)} and
     * {@link SubscribeConfig.Builder#setMatchFilter(java.util.List)}.
     *
     * @return A positive integer, maximum length of byte array for Aware discovery match filter.
     */
    public int getMaxMatchFilterLength() {
        return mCharacteristics.getInt(KEY_MAX_MATCH_FILTER_LENGTH);
    }

    /**
     * Returns the maximum number of Aware data interfaces supported by the device.
     *
     * @return A positive integer, maximum number of Aware data interfaces supported by the device.
     */
    @IntRange(from = 1)
    public int getNumberOfSupportedDataInterfaces() {
        return mCharacteristics.getInt(KEY_MAX_NDI_NUMBER);
    }

    /**
     * Returns the maximum number of Aware publish sessions supported by the device.
     * Use {@link AwareResources#getAvailablePublishSessionsCount()} to get the number of available
     * publish sessions which are not currently used by any app.
     *
     * @return A positive integer, maximum number of publish sessions supported by the device.
     */
    @IntRange(from = 1)
    public int getNumberOfSupportedPublishSessions() {
        return mCharacteristics.getInt(KEY_MAX_PUBLISH_NUMBER);
    }

    /**
     * Returns the maximum number of Aware subscribe session supported by the device.
     * Use {@link AwareResources#getAvailableSubscribeSessionsCount()} to get the number of
     * available subscribe sessions which are not currently used by any app.
     *
     * @return A positive integer, maximum number of subscribe sessions supported by the device.
     */
    @IntRange(from = 1)
    public int getNumberOfSupportedSubscribeSessions() {
        return mCharacteristics.getInt(KEY_MAX_SUBSCRIBE_NUMBER);
    }

    /**
     * Returns the maximum number of Aware data paths(also known as NDPs - NAN Data Paths) supported
     * by the device.
     * Use {@link AwareResources#getAvailableDataPathsCount()} to get the number of available Aware
     * data paths which are not currently used by any app.
     *
     * @return A positive integer, maximum number of Aware data paths supported by the device.
     */
    @IntRange(from = 1)
    public int getNumberOfSupportedDataPaths() {
        return mCharacteristics.getInt(KEY_MAX_NDP_NUMBER);
    }

    /**
     * Check if instant communication mode is supported by device. The instant communication mode is
     * defined as per Wi-Fi Alliance (WFA) Wi-Fi Aware specifications version 3.1 Section 12.3.
     * @return True if supported, false otherwise.
     */
    public boolean isInstantCommunicationModeSupported() {
        if (!true) {
            throw new UnsupportedOperationException();
        }
        return mCharacteristics.getBoolean(KEY_IS_INSTANT_COMMUNICATION_MODE_SUPPORTED);
    }

    /**
     * Check if the Aware Pairing and all associated security features as defined in Wi-Fi Alliance
     * (WFA) Wi-Fi Aware Specification version 4.0 are supported.
     * This includes:
     * <ol>
     * <li>NAN Pairing (as in Wi-Fi Aware Specification Version 4.0 section 7.6) with NIK caching
     * <li>NDP unicast data frame encryption (as in Wi-Fi Aware Specification Version 4.0 section
     * 7.3.1)
     * <li>Group addressed data frame encryption (as in Wi-Fi Aware Specification Version 4.0
     * section 7.3.3)
     * <li>Management frame protection (as in Wi-Fi Aware Specification Version 4.0 section 7.3.2
     * for both unicast and multicast frames)
     * <li>Beacon integrity protection (as in Wi-Fi Aware Specification Version 4.0 section 7.3.4)
     * </ol>
     *
     * @return True if supported, false otherwise.
     */
    public boolean isAwarePairingSupported() {
        return mCharacteristics.getBoolean(KEY_SUPPORT_NAN_PAIRING);
    }

    /**
     * Check if Aware Suspension is supported. Aware Suspension is a mechanism of putting an Aware
     * connection in and out of a low-power mode while preserving the discovery sessions and data
     * paths.
     * @return True if supported, false otherwise.
     */
    public boolean isSuspensionSupported() {
        return mCharacteristics.getBoolean(KEY_SUPPORT_SUSPENSION);
    }

    
    
                    
    
    
    
    /**
     * Wi-Fi Aware supported open (unencrypted) data-path.
     */
    public static final int WIFI_AWARE_CIPHER_SUITE_NONE = 0;
    /**
     * Wi-Fi Aware supported cipher suite representing NCS SK 128: 128 bit shared-key.
     */
    public static final int WIFI_AWARE_CIPHER_SUITE_NCS_SK_128 = 1 << 0;

    /**
     * Wi-Fi Aware supported cipher suite representing NCS SK 256: 256 bit shared-key.
     */
    public static final int WIFI_AWARE_CIPHER_SUITE_NCS_SK_256 = 1 << 1;

    /**
     * Wi-Fi Aware supported cipher suite representing NCS PK 2WDH 128: 128 bit public-key.
     */
    public static final int WIFI_AWARE_CIPHER_SUITE_NCS_PK_128 = 1 << 2;

    /**
     * Wi-Fi Aware supported cipher suite representing NCS 2WDH 256: 256 bit public-key.
     */
    public static final int WIFI_AWARE_CIPHER_SUITE_NCS_PK_256 = 1 << 3;

    /**
     * Wi-Fi Aware supported cipher suite representing NCS PASN 128: 128 bit public-key.
     */
    public static final int WIFI_AWARE_CIPHER_SUITE_NCS_PK_PASN_128 = 1 << 4;

    /**
     * Wi-Fi Aware supported cipher suite representing NCS PASN 256: 256 bit public-key.
     */
    public static final int WIFI_AWARE_CIPHER_SUITE_NCS_PK_PASN_256 = 1 << 5;

    
    /**
     * Returns the set of cipher suites supported by the device for use in Wi-Fi Aware data-paths.
     * The device automatically picks the strongest cipher suite when initiating a data-path setup.
     *
     * @return A set of flags from {@link #WIFI_AWARE_CIPHER_SUITE_NCS_SK_128},
     * {@link #WIFI_AWARE_CIPHER_SUITE_NCS_SK_256}, {@link #WIFI_AWARE_CIPHER_SUITE_NCS_PK_128},
     * or {@link #WIFI_AWARE_CIPHER_SUITE_NCS_PK_256}
     */
    public @WifiAwareDataPathCipherSuites int getSupportedCipherSuites() {
        return mCharacteristics.getInt(KEY_SUPPORTED_DATA_PATH_CIPHER_SUITES);
    }

    /**
     * Returns the set of cipher suites supported by the device for use in Wi-Fi Aware pairing.
     *
     * @return A set of flags from {@link #WIFI_AWARE_CIPHER_SUITE_NCS_PK_PASN_256},
     * or {@link #WIFI_AWARE_CIPHER_SUITE_NCS_PK_PASN_256}
     */
    public @WifiAwarePairingCipherSuites int getSupportedPairingCipherSuites() {
        return mCharacteristics.getInt(KEY_SUPPORTED_PAIRING_CIPHER_SUITES);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeBundle(mCharacteristics);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final @android.annotation.NonNull Creator<Characteristics> CREATOR =
            new Creator<Characteristics>() {
                @Override
                public Characteristics createFromParcel(Parcel in) {
                    Characteristics c = new Characteristics(in.readBundle());
                    return c;
                }

                @Override
                public Characteristics[] newArray(int size) {
                    return new Characteristics[size];
                }
            };
}
