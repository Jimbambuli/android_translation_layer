package android.media.quality;

/* AOSP android/media/quality/AmbientBacklightSettings.java */
import android.annotation.IntDef;
import android.annotation.IntRange;
import android.graphics.PixelFormat;
import android.os.Parcel;
import android.os.Parcelable;

import android.annotation.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Settings to configure ambient backlight hardware.
 */
public final class AmbientBacklightSettings implements Parcelable {
    
    /**
     * The detection is disabled.
     */
    public static final int SOURCE_NONE = 0;

    /**
     * The detection is enabled for audio.
     */
    public static final int SOURCE_AUDIO = 1;

    /**
     * The detection is enabled for video.
     */
    public static final int SOURCE_VIDEO = 2;

    /**
     * The detection is enabled for audio and video.
     */
    public static final int SOURCE_AUDIO_VIDEO = 3;

    /**
     * The source of the ambient backlight.
     */
    private final int mSource;

    /**
     * The maximum framerate for the ambient backlight.
     */
    private final int mMaxFps;

    /**
     * The color format for the ambient backlight.
     */
    private final int mColorFormat;

    /**
     * The number of zones in horizontal direction.
     */
    private final int mHorizontalZonesNumber;

    /**
     * The number of zones in vertical direction.
     */
    private final int mVerticalZonesNumber;

    /**
     * The flag to indicate whether the letterbox is omitted.
     */
    private final boolean mIsLetterboxOmitted;

    /**
     * The color threshold for the ambient backlight.
     */
    private final int mThreshold;

    /**
     * Constructs AmbientBacklightSettings.
     */
    public AmbientBacklightSettings(
            @Source int source,
            int maxFps,
            @PixelFormat.Format int colorFormat,
            int horizontalZonesNumber,
            int verticalZonesNumber,
            boolean isLetterboxOmitted,
            int threshold) {
        mSource = source;
        mMaxFps = maxFps;
        mColorFormat = colorFormat;
        mHorizontalZonesNumber = horizontalZonesNumber;
        mVerticalZonesNumber = verticalZonesNumber;
        mIsLetterboxOmitted = isLetterboxOmitted;
        mThreshold = threshold;
    }

    private AmbientBacklightSettings(Parcel in) {
        mSource = in.readInt();
        mMaxFps = in.readInt();
        mColorFormat = in.readInt();
        mHorizontalZonesNumber = in.readInt();
        mVerticalZonesNumber = in.readInt();
        mIsLetterboxOmitted = in.readBoolean();
        mThreshold = in.readInt();
    }

    /**
     * Gets source of ambient backlight detection.
     */
    @Source
    public int getSource() {
        return mSource;
    }

    /**
     * Gets max frames per second.
     */
    @IntRange(from = 1)
    public int getMaxFps() {
        return mMaxFps;
    }

    /**
     * Gets color format.
     */
    @PixelFormat.Format
    public int getColorFormat() {
        return mColorFormat;
    }

    /**
     * Gets the number of horizontal color zones.
     *
     * <p>A color zone is a group of lights that always display the same color.
     */
    @IntRange(from = 0)
    public int getHorizontalZonesCount() {
        return mHorizontalZonesNumber;
    }

    /**
     * Gets the number of vertical color zones.
     *
     * <p>A color zone is a group of lights that always display the same color.
     */
    @IntRange(from = 0)
    public int getVerticalZonesCount() {
        return mVerticalZonesNumber;
    }

    /**
     * Returns {@code true} if the black portion of the screen in letter box mode is omitted;
     * {@code false} otherwise.
     *
     * <p>Letter-box is a technique to keep the original aspect ratio when displayed on a screen
     * with different aspect ratio. Black bars are added to the top and bottom.
     */
    public boolean isLetterboxOmitted() {
        return mIsLetterboxOmitted;
    }

    /**
     * Gets the detection threshold of the ambient light.
     *
     * <p>If the color of a color zone is changed but the difference is smaller than the threshold,
     * the change is ignored.
     */
    public int getThreshold() {
        return mThreshold;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mSource);
        dest.writeInt(mMaxFps);
        dest.writeInt(mColorFormat);
        dest.writeInt(mHorizontalZonesNumber);
        dest.writeInt(mVerticalZonesNumber);
        dest.writeBoolean(mIsLetterboxOmitted);
        dest.writeInt(mThreshold);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final @NonNull Parcelable.Creator<AmbientBacklightSettings> CREATOR =
            new Parcelable.Creator<AmbientBacklightSettings>() {
                public AmbientBacklightSettings createFromParcel(Parcel in) {
                    return new AmbientBacklightSettings(in);
                }

                public AmbientBacklightSettings[] newArray(int size) {
                    return new AmbientBacklightSettings[size];
                }
            };

    @Override
    public String toString() {
        return "AmbientBacklightSettings{Source=" + mSource + ", MaxFps=" + mMaxFps
                + ", ColorFormat=" + mColorFormat + ", HorizontalZonesNumber="
                + mHorizontalZonesNumber + ", VerticalZonesNumber=" + mVerticalZonesNumber
                + ", IsLetterboxOmitted=" + mIsLetterboxOmitted + ", Threshold=" + mThreshold + "}";
    }
}
