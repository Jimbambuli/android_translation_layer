package android.media.quality;

/* AOSP android/media/quality/AmbientBacklightMetadata.java */
import android.annotation.IntDef;
import android.annotation.IntRange;
import android.graphics.PixelFormat;
import android.os.Parcel;
import android.os.Parcelable;

import android.annotation.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;

/**
 * Metadata of ambient backlight.
 *
 * <p>A metadata instance is sent from ambient backlight hardware in a {@link AmbientBacklightEvent}
 * with {@link AmbientBacklightEvent#AMBIENT_BACKLIGHT_EVENT_METADATA_AVAILABLE}.
 */
public final class AmbientBacklightMetadata implements Parcelable {

    
    /**
     * The compress algorithm is disabled.
     */
    public static final int ALGORITHM_NONE = 0;

    /**
     * The compress algorithm is run length encoding (RLE).
     */
    public static final int ALGORITHM_RLE = 1;

    @NonNull
    private final String mPackageName;
    private final int mCompressAlgorithm;
    private final int mSource;
    private final int mColorFormat;
    private final int mHorizontalZonesNumber;
    private final int mVerticalZonesNumber;
    @NonNull
    private final int[] mZonesColors;

    /**
     * Constructs AmbientBacklightMetadata.
     */
    public AmbientBacklightMetadata(
            @NonNull String packageName,
            int compressAlgorithm,
            int source,
            int colorFormat,
            int horizontalZonesNumber,
            int verticalZonesNumber,
            @NonNull int[] zonesColors) {
        mPackageName = packageName;
        mCompressAlgorithm = compressAlgorithm;
        mSource = source;
        mColorFormat = colorFormat;
        mHorizontalZonesNumber = horizontalZonesNumber;
        mVerticalZonesNumber = verticalZonesNumber;
        mZonesColors = zonesColors;
    }

    private AmbientBacklightMetadata(Parcel in) {
        mPackageName = in.readString();
        mCompressAlgorithm = in.readInt();
        mSource = in.readInt();
        mColorFormat = in.readInt();
        mHorizontalZonesNumber = in.readInt();
        mVerticalZonesNumber = in.readInt();
        mZonesColors = in.createIntArray();
    }

    /**
     * Gets package name of the metadata.
     */
    @NonNull
    public String getPackageName() {
        return mPackageName;
    }

    /**
     * Gets compress algorithm.
     */
    
    public int getCompressionAlgorithm() {
        return mCompressAlgorithm;
    }

    /**
     * Gets source of ambient backlight detection.
     */
    
    public int getSource() {
        return mSource;
    }

    /**
     * Gets color format.
     */
    
    public int getColorFormat() {
        return mColorFormat;
    }

    /**
     * Gets the number of horizontal color zones.
     *
     * <p>A color zone is represented by one single aggregated color. The number should not be
     * larger than 128.
     */
    @IntRange(from = 0, to = 128)
    public int getHorizontalZonesCount() {
        return mHorizontalZonesNumber;
    }

    /**
     * Gets the number of vertical color zones.
     *
     * <p>A color zone is represented by one single aggregated color. The number should not be
     * larger than 80.
     */
    @IntRange(from = 0, to = 80)
    public int getVerticalZonesCount() {
        return mVerticalZonesNumber;
    }

    /**
     * Gets color data of all available color zones.
     *
     * <p>The format of the color data can be found at {@link #getColorFormat()}.
     *
     * @return an array of color data, in row by row (left-to-right then top-to-bottom) order of the
     * color zones.
     *
     * @see #getHorizontalZonesCount()
     * @see #getVerticalZonesCount()
     */
    @NonNull
    public int[] getZoneColors() {
        return mZonesColors;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(mPackageName);
        dest.writeInt(mCompressAlgorithm);
        dest.writeInt(mSource);
        dest.writeInt(mColorFormat);
        dest.writeInt(mHorizontalZonesNumber);
        dest.writeInt(mVerticalZonesNumber);
        dest.writeIntArray(mZonesColors);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final @NonNull Parcelable.Creator<AmbientBacklightMetadata> CREATOR =
            new Parcelable.Creator<AmbientBacklightMetadata>() {
                public AmbientBacklightMetadata createFromParcel(Parcel in) {
                    return new AmbientBacklightMetadata(in);
                }

                public AmbientBacklightMetadata[] newArray(int size) {
                    return new AmbientBacklightMetadata[size];
                }
            };

    @Override
    public String toString() {
        return "AmbientBacklightMetadata{packageName=" + mPackageName
                + ", compressAlgorithm=" + mCompressAlgorithm + ", source=" + mSource
                + ", colorFormat=" + mColorFormat + ", horizontalZonesNumber="
                + mHorizontalZonesNumber + ", verticalZonesNumber=" + mVerticalZonesNumber
                + ", zonesColors=" + Arrays.toString(mZonesColors) + "}";
    }
}
