package android.net.wifi;

import android.annotation.IntDef;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/* AOSP WifiScanner.java (@hide class): only the band constants and annotations other public classes refer to; ATL has no scan service */
public class WifiScanner {
    private WifiScanner() {}

    public static final int WIFI_BAND_INDEX_24_GHZ = 0;
    public static final int WIFI_BAND_INDEX_5_GHZ = 1;
    public static final int WIFI_BAND_INDEX_5_GHZ_DFS_ONLY = 2;
    public static final int WIFI_BAND_INDEX_6_GHZ = 3;
    public static final int WIFI_BAND_INDEX_60_GHZ = 4;
    public static final int WIFI_BAND_COUNT = 5;

    public static final int WIFI_BAND_INDEX_5_GHZ_LOW = 29;

    public static final int WIFI_BAND_INDEX_5_GHZ_HIGH = 30;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(prefix = {"WIFI_BAND_INDEX_"}, value = {
            WIFI_BAND_INDEX_24_GHZ,
            WIFI_BAND_INDEX_5_GHZ,
            WIFI_BAND_INDEX_5_GHZ_DFS_ONLY,
            WIFI_BAND_INDEX_6_GHZ,
            WIFI_BAND_INDEX_60_GHZ})
    public @interface WifiBandIndex {}

    public static final int WIFI_BAND_UNSPECIFIED = 0;
    public static final int WIFI_BAND_24_GHZ = 1 << WIFI_BAND_INDEX_24_GHZ;
    public static final int WIFI_BAND_5_GHZ = 1 << WIFI_BAND_INDEX_5_GHZ;
    public static final int WIFI_BAND_5_GHZ_DFS_ONLY  = 1 << WIFI_BAND_INDEX_5_GHZ_DFS_ONLY;
    public static final int WIFI_BAND_6_GHZ = 1 << WIFI_BAND_INDEX_6_GHZ;
    public static final int WIFI_BAND_60_GHZ = 1 << WIFI_BAND_INDEX_60_GHZ;

    public static final int WIFI_BAND_5_GHZ_LOW = 1 << WIFI_BAND_INDEX_5_GHZ_LOW;
    public static final int WIFI_BAND_5_GHZ_HIGH = 1 << WIFI_BAND_INDEX_5_GHZ_HIGH;

    public static final int WIFI_BAND_BOTH = WIFI_BAND_24_GHZ | WIFI_BAND_5_GHZ;
    public static final int WIFI_BAND_24_GHZ_WITH_5GHZ_DFS  =
            WIFI_BAND_24_GHZ | WIFI_BAND_5_GHZ_DFS_ONLY;
    public static final int WIFI_BAND_5_GHZ_WITH_DFS  = WIFI_BAND_5_GHZ | WIFI_BAND_5_GHZ_DFS_ONLY;
    public static final int WIFI_BAND_BOTH_WITH_DFS =
            WIFI_BAND_24_GHZ | WIFI_BAND_5_GHZ | WIFI_BAND_5_GHZ_DFS_ONLY;
    public static final int WIFI_BAND_24_5_6_GHZ = WIFI_BAND_BOTH | WIFI_BAND_6_GHZ;
    public static final int WIFI_BAND_24_5_WITH_DFS_6_GHZ =
            WIFI_BAND_BOTH_WITH_DFS | WIFI_BAND_6_GHZ;
    public static final int WIFI_BAND_24_5_6_60_GHZ =
            WIFI_BAND_24_5_6_GHZ | WIFI_BAND_60_GHZ;
    public static final int WIFI_BAND_24_5_WITH_DFS_6_60_GHZ =
            WIFI_BAND_24_5_6_60_GHZ | WIFI_BAND_5_GHZ_DFS_ONLY;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(prefix = {"WIFI_BAND_"}, value = {
            WIFI_BAND_UNSPECIFIED,
            WIFI_BAND_24_GHZ,
            WIFI_BAND_5_GHZ,
            WIFI_BAND_BOTH,
            WIFI_BAND_5_GHZ_DFS_ONLY,
            WIFI_BAND_24_GHZ_WITH_5GHZ_DFS,
            WIFI_BAND_5_GHZ_WITH_DFS,
            WIFI_BAND_BOTH_WITH_DFS,
            WIFI_BAND_6_GHZ,
            WIFI_BAND_24_5_6_GHZ,
            WIFI_BAND_24_5_WITH_DFS_6_GHZ,
            WIFI_BAND_60_GHZ,
            WIFI_BAND_24_5_6_60_GHZ,
            WIFI_BAND_24_5_WITH_DFS_6_60_GHZ})
    public @interface WifiBand {}
}
